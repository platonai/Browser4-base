package ai.platon.pulsar.agentic.mcp

import ai.platon.pulsar.agentic.event.AgentEventBus
import ai.platon.pulsar.agentic.event.AgenticEvents
import ai.platon.pulsar.agentic.model.TcEvaluate
import ai.platon.pulsar.agentic.model.TcException
import ai.platon.pulsar.agentic.model.ToolCall
import ai.platon.pulsar.agentic.model.ToolSpec
import ai.platon.pulsar.agentic.tools.builtin.ToolExecutor
import ai.platon.pulsar.common.brief
import ai.platon.pulsar.common.getLogger
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.Tool
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.reflect.KClass

/**
 * Tool executor for MCP (Model Context Protocol) tools.
 *
 * This executor integrates MCP server tools into the Pulsar agentic tool execution framework.
 * It translates between Pulsar's ToolCall format and MCP's tool calling format.
 *
 * @property clientManager The MCP client manager that handles the server connection.
 */
class MCPToolExecutor(
    private val clientManager: MCPClientManager
) : ToolExecutor {

    private val logger = getLogger(this)

    override val domain: String
        get() = "mcp.${clientManager.getServerName()}"

    override val targetClass: KClass<*>
        get() = MCPClientManager::class

    private val toolSpecs: Map<String, ToolSpec> by lazy {
        buildToolSpecs()
    }

    /**
     * Executes a tool call on the MCP server.
     *
     * @param tc The tool call to execute.
     * @param target Ignored for MCP tools.
     * @return The evaluation result.
     */
    override suspend fun callFunctionOn(tc: ToolCall, target: Any): TcEvaluate {
        val toolName = tc.method
        val args = tc.arguments
        val pseudoExpression = tc.pseudoExpression
        val serverName = clientManager.getServerName()

        if (!clientManager.isConnected()) {
            val error = "MCP client for server '$serverName' is not connected"
            logger.warn(error)
            
            onMCPError(serverName, toolName, 0, error, "not_connected")
            
            return TcEvaluate(
                value = null,
                className = "null",
                expression = pseudoExpression,
                exception = TcException(pseudoExpression, IllegalStateException(error))
            )
        }

        onWillCallMCP(serverName, toolName, args)

        val startTime = System.currentTimeMillis()

        return try {
            // Convert arguments to the format expected by MCP
            val mcpArguments = convertArgumentsForMCP(args)

            // Call the tool on the MCP server
            val result = clientManager.callTool(toolName, mcpArguments)

            // Extract text content from the result
            val resultValue = extractResultValue(result)
            
            val duration = System.currentTimeMillis() - startTime

            onDidCallMCP(serverName, toolName, duration)

            TcEvaluate(
                value = resultValue,
                className = resultValue?.let { it::class.qualifiedName } ?: "null",
                expression = pseudoExpression
            )
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            logger.warn("Error executing MCP tool '{}': {}", toolName, e.brief())
            
            onMCPError(serverName, toolName, duration, "MCP tool call failed: ${e.message}", e.message)
            
            val helpText = help(toolName)
            TcEvaluate(
                value = null,
                className = "null",
                expression = pseudoExpression,
                exception = TcException(pseudoExpression, e, helpText)
            )
        }
    }

    // ------------------------------ Event Handler Methods --------------------------------

    private fun onWillCallMCP(serverName: String, toolName: String, args: Map<String, Any?>) {
        AgentEventBus.emitMCPEvent(
            eventType = AgenticEvents.MCPEventTypes.ON_WILL_CALL_MCP,
            agentId = null,
            message = "Calling MCP tool: $serverName.$toolName",
            metadata = mapOf(
                "serverName" to serverName,
                "toolName" to toolName,
                "argsKeys" to args.keys.toList()
            )
        )
    }

    private fun onDidCallMCP(serverName: String, toolName: String, duration: Long) {
        AgentEventBus.emitMCPEvent(
            eventType = AgenticEvents.MCPEventTypes.ON_DID_CALL_MCP,
            agentId = null,
            message = "MCP tool call completed: $serverName.$toolName",
            metadata = mapOf(
                "serverName" to serverName,
                "toolName" to toolName,
                "duration" to duration,
                "success" to true
            )
        )
    }

    private fun onMCPError(serverName: String, toolName: String, duration: Long, message: String, error: String?) {
        AgentEventBus.emitMCPEvent(
            eventType = AgenticEvents.MCPEventTypes.ON_MCP_ERROR,
            agentId = null,
            message = message,
            metadata = mapOf(
                "serverName" to serverName,
                "toolName" to toolName,
                "duration" to duration,
                "error" to error
            )
        )
    }

    override fun help(): String {
        return toolSpecs.values.mapNotNull { spec ->
            spec.description?.let { "${spec.expression}\n  $it" }
        }.joinToString("\n\n")
    }

    override fun help(method: String): String {
        val spec = toolSpecs[method] ?: return "Tool '$method' not found in MCP server '${clientManager.getServerName()}'"
        return buildString {
            spec.description?.let { appendLine(it) }
            appendLine(spec.expression)
        }.trim()
    }

    /**
     * Gets the list of available tool names.
     *
     * @return List of tool names.
     */
    fun getAvailableToolNames(): List<String> {
        return clientManager.availableTools.map { it.name }
    }

    private fun buildToolSpecs(): Map<String, ToolSpec> {
        return clientManager.availableTools.associate { tool ->
            val spec = convertMCPToolToSpec(tool)
            tool.name to spec
        }
    }

    private fun convertMCPToolToSpec(tool: Tool): ToolSpec {
        // Extract arguments from the tool's input schema
        val args = extractArgumentsFromSchema(tool.inputSchema)

        return ToolSpec(
            domain = domain,
            method = tool.name,
            arguments = args,
            returnType = "Any?",
            description = tool.description
        )
    }

    private fun extractArgumentsFromSchema(inputSchema: ToolSchema?): List<ToolSpec.Arg> {
        val schema = inputSchema ?: return emptyList()

        val properties = schema.properties ?: return emptyList()
        val required = schema.required?.toSet().orEmpty()

        return properties.entries.map { (name, element) ->
            val schemaObj = element as? JsonObject
            val type = schemaObj?.get("type")?.let { (it as? JsonPrimitive)?.content } ?: "Any"
            val isRequired = name in required

            ToolSpec.Arg(
                name = name,
                type = mapJsonTypeToKotlinType(type),
                defaultValue = if (isRequired) null else "null"
            )
        }
    }

    private fun mapJsonTypeToKotlinType(jsonType: String): String {
        return when (jsonType.lowercase()) {
            "string" -> "String"
            "number" -> "Double"
            "integer" -> "Int"
            "boolean" -> "Boolean"
            "array" -> "List<Any>"
            "object" -> "Map<String, Any>"
            else -> "Any"
        }
    }

    private fun convertArgumentsForMCP(args: Map<String, Any?>): Map<String, Any?> {
        // For now, we pass through arguments as-is
        // In the future, we might need more sophisticated type conversion
        return args
    }

    private fun extractResultValue(result: Any?): String? {
        if (result == null) return null

        // Try to extract text content if the result is a CallToolResult
        return try {
            if (result is io.modelcontextprotocol.kotlin.sdk.types.CallToolResult) {
                result.content
                    .filterIsInstance<TextContent>()
                    .joinToString("\n") { it.text }
                    .ifEmpty { result.toString() }
            } else {
                result.toString()
            }
        } catch (e: Exception) {
            logger.warn("Error extracting result value: {}", e.message)
            result.toString()
        }
    }
}
