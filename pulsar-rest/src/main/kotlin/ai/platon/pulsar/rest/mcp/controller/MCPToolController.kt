package ai.platon.pulsar.rest.mcp.controller

import ai.platon.pulsar.agentic.agents.BasicBrowserAgent
import ai.platon.pulsar.agentic.tools.AgentToolExecutor
import ai.platon.pulsar.rest.mcp.service.SessionManager
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

// ---------------------------------------------------------------------------
// DTOs
// ---------------------------------------------------------------------------

/**
 * Request body for calling an MCP tool.
 */
data class MCPToolCallRequest(
    @param:JsonProperty("tool") val tool: String,
    @param:JsonProperty("arguments") val arguments: Map<String, Any?>? = null
)

/**
 * Response from an MCP tool call.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class MCPToolCallResponse(
    @get:JsonProperty("content")
    @param:JsonProperty("content")
    val content: List<MCPContent>,
    @get:JsonProperty("isError")
    @param:JsonSetter(nulls = Nulls.SKIP)
    @param:JsonProperty("isError")
    val isError: Boolean = false
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class MCPContent(
    @param:JsonProperty("type") val type: String = "text",
    @param:JsonProperty("text") val text: String
)

// ---------------------------------------------------------------------------
// Controller
// ---------------------------------------------------------------------------

/**
 * REST controller that exposes Browser4 MCP tools over HTTP.
 *
 * This allows the browser4-cli (and any HTTP client) to invoke MCP tools
 * through a simple REST endpoint instead of STDIO.
 *
 * Session management tools (open_session, close_session, list_sessions, etc.)
 * are handled directly by this controller.
 */
@RestController
@CrossOrigin
@RequestMapping(
    path = ["/mcp"],
    produces = [MediaType.APPLICATION_JSON_VALUE]
)
@ConditionalOnBean(SessionManager::class)
class MCPToolController(
    private val sessionManager: SessionManager
) {
    companion object {
        private val FRONTEND_TOOL_NAME_ALIASES: Map<String, String> = mapOf(
            "browser_navigate" to "navigate",
            "browser_snapshot" to "aria_snapshot",
            "browser_navigate_back" to "go_back",
            "browser_navigate_forward" to "go_forward",
            "browser_reload" to "reload",
            "browser_press_key" to "press",
            "browser_press_sequentially" to "type",
            "browser_keydown" to "keydown",
            "browser_keyup" to "keyup",
            "browser_mouse_move_xy" to "mousemove",
            "browser_mouse_down" to "mousedown",
            "browser_mouse_up" to "mouseup",
            "browser_mouse_wheel" to "mousewheel",
            "browser_drag" to "drag",
            "browser_type" to "fill",
            "browser_hover" to "hover",
            "browser_select_option" to "select_option",
            "browser_file_upload" to "upload",
            "browser_check" to "check",
            "browser_uncheck" to "uncheck",
            "browser_evaluate" to "evaluate",
            "browser_resize" to "resize",
            "browser_take_screenshot" to "screenshot",
        )

        private const val CLEAR_SESSION_STORAGE_SCRIPT = """
            (() => {
                const result = {
                    localStorageCleared: false,
                    sessionStorageCleared: false,
                    errors: []
                };
                try {
                    window.localStorage.clear();
                    result.localStorageCleared = true;
                } catch (error) {
                    result.errors.push("localStorage: " + error);
                }
                try {
                    window.sessionStorage.clear();
                    result.sessionStorageCleared = true;
                } catch (error) {
                    result.errors.push("sessionStorage: " + error);
                }
                return JSON.stringify(result);
            })()
        """
    }

    private val logger = LoggerFactory.getLogger(MCPToolController::class.java)

    private data class NormalizedToolCall(
        val tool: String,
        val arguments: Map<String, Any?>
    )

    // =========================================================================
    // Tool call endpoint
    // =========================================================================

    /**
     * Call an MCP tool.
     *
     * Session management tools (`open_session`, `close_session`, `list_sessions`,
     * `close_all_sessions`, `kill_all_sessions`, `delete_session_data`) do not
     * require a sessionId.
     *
     * All other tools require the `sessionId` to be provided in the request body
     * or via the path variable.
     */
    @PostMapping("/call-tool", consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun callTool(
        @RequestBody request: MCPToolCallRequest,
        response: HttpServletResponse
    ): ResponseEntity<MCPToolCallResponse> {
        addRequestId(response)

        return try {
            when (request.tool) {
                // Session management tools
                "open_session" -> handleOpenSession(request)
                "close_session" -> handleCloseSession(request)
                "list_sessions" -> handleListSessions()
                "close_all_sessions" -> handleCloseAllSessions()
                "kill_all_sessions" -> handleKillAllSessions()
                "delete_session_data" -> handleDeleteSessionData(request)
                // All other tools are dispatched to the session's agent
                else -> dispatchToAgentToolExecutor(request)
            }
        } catch (e: Exception) {
            logger.error("MCP tool call failed | tool={} | {}", request.tool, e.message, e)
            ResponseEntity.ok(errorResponse("${request.tool} failed: ${e.message}"))
        }
    }

    /**
     * List available MCP tools.
     */
    @GetMapping("/tools")
    fun listTools(
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        addRequestId(response)

        val tools = listOf(
            // Session management
            "open_session", "close_session", "list_sessions",
            "close_all_sessions", "kill_all_sessions", "delete_session_data",
            // Frontend-declared Browser4 CLI tools
            "browser_navigate", "browser_snapshot",
            "browser_navigate_back", "browser_navigate_forward", "browser_reload",
            "browser_press_key", "browser_press_sequentially",
            "browser_keydown", "browser_keyup",
            "browser_mouse_move_xy", "browser_mouse_down", "browser_mouse_up", "browser_mouse_wheel",
            "browser_click", "browser_drag", "browser_type", "browser_hover", "browser_select_option",
            "browser_file_upload", "browser_check", "browser_uncheck",
            "browser_evaluate", "browser_handle_dialog", "browser_resize",
            "browser_take_screenshot", "browser_tabs",
            // Internal helper tools still used by browser4-cli
            "page_url", "page_title"
        )

        return ResponseEntity.ok(mapOf("tools" to tools))
    }

    // =========================================================================
    // Session management handlers
    // =========================================================================

    private fun handleOpenSession(request: MCPToolCallRequest): ResponseEntity<MCPToolCallResponse> {
        val capabilities = request.arguments?.get("capabilities") as? Map<String, Any?>
        val session = sessionManager.createSession(capabilities)

        // Navigate to initial URL if provided
        val url = request.arguments?.get("url")?.toString()

        logger.info("MCP open_session: created session {}", session.sessionId)
        return ResponseEntity.ok(
            textResponse("""{"sessionId":"${session.sessionId}"}""")
        )
    }

    private fun handleCloseSession(request: MCPToolCallRequest): ResponseEntity<MCPToolCallResponse> {
        val sessionId = requireSessionId(request)
        val deleted = sessionManager.deleteSession(sessionId)
        return if (deleted) {
            ResponseEntity.ok(textResponse("Session closed"))
        } else {
            ResponseEntity.ok(errorResponse("Session not found: $sessionId"))
        }
    }

    private fun handleListSessions(): ResponseEntity<MCPToolCallResponse> {
        val sessions = sessionManager.getAllSessions().map { s ->
            """{"sessionId":"${s.sessionId}","url":"${s.url ?: ""}","status":"${s.status}"}"""
        }
        return ResponseEntity.ok(textResponse("[${sessions.joinToString(",")}]"))
    }

    private fun handleCloseAllSessions(): ResponseEntity<MCPToolCallResponse> {
        val count = sessionManager.deleteAllSessions()
        return ResponseEntity.ok(textResponse("Closed $count session(s)"))
    }

    private fun handleKillAllSessions(): ResponseEntity<MCPToolCallResponse> {
        val count = sessionManager.deleteAllSessions()
        return ResponseEntity.ok(textResponse("Killed $count session(s)"))
    }

    private suspend fun handleDeleteSessionData(request: MCPToolCallRequest): ResponseEntity<MCPToolCallResponse> {
        val sessionId = requireSessionId(request)
        val managed = sessionManager.getSession(sessionId)
            ?: return ResponseEntity.ok(errorResponse("Session not found: $sessionId"))

        managed.withLock {
            driver.clearBrowserCookies()
            val storageResult = driver.evaluate(CLEAR_SESSION_STORAGE_SCRIPT)?.toString().orEmpty()
            if (storageResult.isNotBlank() && !storageResult.contains("\"errors\":[]")) {
                logger.warn(
                    "delete_session_data completed with partial storage cleanup | sessionId={} | result={}",
                    sessionId,
                    storageResult
                )
            }
        }

        return ResponseEntity.ok(textResponse("User data deleted for session"))
    }

    // =========================================================================
    // Dispatch to per-session AgentToolManager
    // =========================================================================

    /**
     * Dispatch a tool call to the session's AgentToolManager.
     *
     * This replaces the manual tool implementation by delegating to the central
     * tool registry in [AgentToolExecutor].
     */
    private suspend fun dispatchToAgentToolExecutor(request: MCPToolCallRequest): ResponseEntity<MCPToolCallResponse> {
        val normalizedRequest = normalizeFrontendToolCall(request.tool, request.arguments ?: emptyMap())
        val sessionId = requireSessionId(normalizedRequest.arguments)
        val managed = sessionManager.getSession(sessionId)
            ?: return ResponseEntity.ok(errorResponse("Session not found: $sessionId"))

        val agent = managed.agenticSession.companionAgent as? BasicBrowserAgent
            ?: return ResponseEntity.ok(errorResponse("Session agent does not support tools"))

        val toolName = normalizedRequest.tool
        val args = normalizeToolArguments(toolName, normalizedRequest.arguments)

        // Find the matching tool in AgentToolManager
        val toolCall = agent.toolExtractor.resolveToolCall(toolName, args)
            ?: return ResponseEntity.ok(errorResponse("Unknown tool: ${request.tool}"))

        return try {
            val result = agent.toolExtractor.execute(toolCall)
            val evaluate = result.evaluate
            val exception = evaluate.exception
            if (exception != null) {
                ResponseEntity.ok(errorResponse("${request.tool} failed: ${exception.message} help: ${exception.help}"))
            } else {
                ResponseEntity.ok(textResponse(evaluate.value?.toString() ?: ""))
            }
        } catch (e: Exception) {
            logger.error("MCP tool execution failed | tool={} | normalizedTool={} | {}", request.tool, toolName, e.message, e)
            ResponseEntity.ok(errorResponse("${request.tool} failed: ${e.message}"))
        }
    }

    private fun normalizeFrontendToolCall(toolName: String, args: Map<String, Any?>): NormalizedToolCall {
        if (toolName == "browser_tabs") {
            val action = args["action"]?.toString()
            val resolvedTool = when (action) {
                "list" -> "tab_list"
                "new" -> "tab_new"
                "close" -> "tab_close"
                "select" -> "tab_select"
                else -> toolName
            }
            return NormalizedToolCall(
                tool = resolvedTool,
                arguments = args.toMutableMap().apply { remove("action") }
            )
        }

        if (toolName == "browser_handle_dialog") {
            val accept = args["accept"].toBooleanValue()
            return NormalizedToolCall(
                tool = if (accept == false) "dialog_dismiss" else "dialog_accept",
                arguments = args.toMutableMap().apply { remove("accept") }
            )
        }

        if (toolName == "browser_click") {
            val doubleClick = args["doubleClick"].toBooleanValue()
            return NormalizedToolCall(
                tool = if (doubleClick == true) "dblclick" else "click",
                arguments = args.toMutableMap().apply { remove("doubleClick") }
            )
        }

        return NormalizedToolCall(
            tool = FRONTEND_TOOL_NAME_ALIASES[toolName] ?: toolName,
            arguments = args
        )
    }

    private fun normalizeToolArguments(toolName: String, args: Map<String, Any?>): Map<String, Any?> {
        val normalized = args.mapKeys { (key, _) -> snakeToCamel(key) }.toMutableMap()
        normalized.remove("sessionId")

        val ref = normalized.remove("ref")
        if (!normalized.containsKey("selector") && ref != null) {
            normalized["selector"] = ref
        }

        val startRef = normalized.remove("startRef")
        if (!normalized.containsKey("sourceSelector") && startRef != null) {
            normalized["sourceSelector"] = startRef
        }

        val endRef = normalized.remove("endRef")
        if (!normalized.containsKey("targetSelector") && endRef != null) {
            normalized["targetSelector"] = endRef
        }

        val modifiers = normalized.remove("modifiers")
        if (!normalized.containsKey("modifier") && modifiers is List<*> && modifiers.isNotEmpty()) {
            normalized["modifier"] = modifiers.first()?.toString()
        }

        when (toolName) {
            "switch_tab", "tab_select", "close_tab", "tab_close" -> {
                val legacyTabId = normalized.remove("index") ?: normalized.remove("id")
                if (!normalized.containsKey("tabId") && legacyTabId != null) {
                    normalized["tabId"] = legacyTabId.toString()
                }
            }

            "select_option" -> {
                val legacyValue = normalized.remove("value")
                if (!normalized.containsKey("values") && legacyValue != null) {
                    normalized["values"] = listOf(legacyValue.toString())
                }
            }
        }

        return normalized
    }

    private fun Any?.toBooleanValue(): Boolean? = when (this) {
        is Boolean -> this
        is String -> this.toBooleanStrictOrNull()
        else -> null
    }

    private fun snakeToCamel(key: String): String {
        if (!key.contains("_")) {
            return key
        }

        val parts = key.split("_").filter { it.isNotEmpty() }
        if (parts.isEmpty()) {
            return key
        }

        return buildString {
            append(parts.first())
            parts.drop(1).forEach { append(it.replaceFirstChar { c -> c.uppercase() }) }
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private fun requireSessionId(request: MCPToolCallRequest): String {
        return request.arguments?.get("sessionId")?.toString()
            ?: throw IllegalArgumentException("Missing required parameter: sessionId")
    }

    private fun requireSessionId(arguments: Map<String, Any?>): String {
        return arguments["sessionId"]?.toString()
            ?: throw IllegalArgumentException("Missing required parameter: sessionId")
    }

    private fun requireArg(args: Map<String, Any?>, key: String): String {
        return args[key]?.toString()
            ?: throw IllegalArgumentException("Missing required parameter: $key")
    }

    private fun textResponse(text: String): MCPToolCallResponse =
        MCPToolCallResponse(content = listOf(MCPContent(text = text)))

    private fun errorResponse(message: String): MCPToolCallResponse =
        MCPToolCallResponse(content = listOf(MCPContent(text = "ERROR: $message")), isError = true)

    private fun addRequestId(response: HttpServletResponse) {
        response.addHeader("X-Request-Id", UUID.randomUUID().toString())
    }
}
