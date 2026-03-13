package ai.platon.pulsar.rest.mcp.controller

import ai.platon.pulsar.agentic.agents.BasicBrowserAgent
import ai.platon.pulsar.agentic.model.ToolCall
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
import java.util.UUID

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
    @param:JsonProperty("content") val content: List<MCPContent>,
    @param:JsonSetter(nulls = Nulls.SKIP)
    @param:JsonProperty("isError") val isError: Boolean = false
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
 * are handled directly by this controller.  All other tools are dispatched to
 * a per-session [Browser4MCPServer] instance that wraps the session's WebDriver.
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
    private val logger = LoggerFactory.getLogger(MCPToolController::class.java)

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
                else -> dispatchToAgent(request)
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
            // Driver tools
            "navigate", "reload", "go_back", "go_forward",
            "wait_for_selector", "exists", "is_visible", "focus",
            "hover", "click", "fill", "type", "upload", "press",
            "check", "uncheck",
            "scroll_to", "scroll_to_top", "scroll_to_bottom", "scroll_to_middle", "scroll_by",
            "text_content", "get_text", "delay",
            "aria_snapshot", "page_url", "page_title",
            "screenshot", "dblclick", "drag", "select_option",
            "evaluate", "evaluate_value", "dialog_accept", "dialog_dismiss", "resize",
            "keydown", "keyup",
            "mousemove", "mousedown", "mouseup", "mousewheel",
            // Browser tools
            "switch_tab", "close_tab", "tab_list", "tab_new", "tab_close", "tab_select"
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
            driver.evaluate("localStorage.clear(); sessionStorage.clear()")
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
    private suspend fun dispatchToAgent(request: MCPToolCallRequest): ResponseEntity<MCPToolCallResponse> {
        val sessionId = requireSessionId(request)
        val managed = sessionManager.getSession(sessionId)
            ?: return ResponseEntity.ok(errorResponse("Session not found: $sessionId"))

        val agent = managed.agenticSession.companionAgent as? BasicBrowserAgent
            ?: return ResponseEntity.ok(errorResponse("Session agent does not support tools"))

        val toolName = request.tool
        val args = normalizeToolArguments(toolName, request.arguments ?: emptyMap())

        // Find the matching tool in AgentToolManager
        val toolCall = resolveToolCall(toolName, args, agent)
            ?: return ResponseEntity.ok(errorResponse("Unknown tool: $toolName"))

        return try {
            val result = agent.toolExtractor.execute(toolCall)
            val evaluate = result.evaluate
            val exception = evaluate.exception
            if (exception != null) {
                ResponseEntity.ok(errorResponse("$toolName failed: ${exception.cause?.message} help: ${exception.help}"))
            } else {
                ResponseEntity.ok(textResponse(evaluate.value?.toString() ?: ""))
            }
        } catch (e: Exception) {
            logger.error("MCP tool execution failed | tool={} | {}", toolName, e.message, e)
            ResponseEntity.ok(errorResponse("$toolName failed: ${e.message}"))
        }
    }

    private fun resolveToolCall(toolName: String, args: Map<String, Any?>, agent: BasicBrowserAgent): ToolCall? {
        val args1 = args.toMutableMap()

        // 1. Explicit mapping for legacy/special names
        when (toolName) {
            "page_title" -> return ToolCall("tab", "title", args1)
            "page_url" -> return ToolCall("tab", "currentUrl", args1) // or just rely on pageUrl if it exists
            "switch_tab", "tab_select" -> return ToolCall("browser", "switchTab", args1)
            "tab_new" -> return ToolCall("browser", "newTab", args1)
            "tab_list" -> return ToolCall("browser", "listTabs", args1)
            "tab_close", "close_tab" -> return ToolCall("browser", "closeTab", args1)
            "keydown", "browser_keydown" -> return ToolCall("tab", "keyDown", args1)
            "keyup", "browser_keyup" -> return ToolCall("tab", "keyUp", args1)
            "mousemove", "browser_mouse_move_xy" -> return ToolCall("tab", "mouseMove", args1)
            "mousedown", "browser_mouse_down" -> return ToolCall("tab", "mouseDown", args1)
            "mouseup", "browser_mouse_up" -> return ToolCall("tab", "mouseUp", args1)
            "mousewheel", "browser_mouse_wheel" -> return ToolCall("tab", "mouseWheel", args1)
        }

        // 2. Generic mapping
        val specs = agent.toolExtractor.getAllToolSpecs()
        for ((domain, methods) in specs) {
            for ((method, _) in methods) {
                val mcpName = toMcpToolName(domain, method)
                if (mcpName == toolName) {
                    return ToolCall(domain, method, args1)
                }
            }
        }

        return null
    }

    /**
     * Convert domain+method to snake_case MCP tool name.
     * Must match logic in Browser4MCPServer.
     */
    private fun toMcpToolName(domain: String, method: String): String {
        val snake = method.replace(Regex("([A-Z])")) { "_${it.groupValues[1].lowercase()}" }
        return when (domain) {
            "tab", "system" -> snake
            else -> "${domain}_$snake"
        }
    }

    private fun normalizeToolArguments(toolName: String, args: Map<String, Any?>): Map<String, Any?> {
        val normalized = args.mapKeys { (key, _) -> snakeToCamel(key) }.toMutableMap()

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
