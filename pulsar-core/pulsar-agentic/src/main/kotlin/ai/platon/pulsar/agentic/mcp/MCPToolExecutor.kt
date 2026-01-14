package ai.platon.pulsar.agentic.mcp

import ai.platon.pulsar.agentic.TcEvaluate
import ai.platon.pulsar.agentic.ToolCall
import ai.platon.pulsar.agentic.ToolCallSpec
import ai.platon.pulsar.agentic.tools.executors.ToolExecutor
import ai.platon.pulsar.common.brief
import ai.platon.pulsar.common.getLogger
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.Tool
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
    
    private val toolSpecs: Map<String, ToolCallSpec> by lazy {
        buildToolSpecs()
    }
    
    /**
     * Executes a tool call on the MCP server.
     *
     * @param tc The tool call to execute.
     * @param target Ignored for MCP tools.
     * @return The evaluation result.
     */
    override suspend fun execute(tc: ToolCall, target: Any): TcEvaluate {
        val toolName = tc.method
        val args = tc.arguments
        val pseudoExpression = tc.pseudoExpression
        
        if (!clientManager.isConnected()) {
            val error = "MCP client for server '${clientManager.getServerName()}' is not connected"
            logger.warn(error)
            return TcEvaluate(
                value = null,
                className = "null",
                expression = pseudoExpression,
                exception = IllegalStateException(error)
            )
        }
        
        return try {
            // Convert arguments to the format expected by MCP
            val mcpArguments = convertArgumentsForMCP(args)
            
            // Call the tool on the MCP server
            val result = clientManager.callTool(toolName, mcpArguments)
            
            // Extract text content from the result
            val resultValue = extractResultValue(result)
            
            TcEvaluate(
                value = resultValue,
                className = resultValue?.let { it::class.qualifiedName } ?: "null",
                expression = pseudoExpression
            )
        } catch (e: Exception) {
            logger.warn("Error executing MCP tool '{}': {}", toolName, e.brief())
            val helpText = help(toolName)
            TcEvaluate(
                value = null,
                className = "null",
                expression = pseudoExpression,
                exception = e,
                help = helpText
            )
        }
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
    
    private fun buildToolSpecs(): Map<String, ToolCallSpec> {
        return clientManager.availableTools.associate { tool ->
            val spec = convertMCPToolToSpec(tool)
            tool.name to spec
        }
    }
    
    private fun convertMCPToolToSpec(tool: Tool): ToolCallSpec {
        // Extract arguments from the tool's input schema
        val args = extractArgumentsFromSchema(tool.inputSchema)
        
        return ToolCallSpec(
            domain = domain,
            method = tool.name,
            arguments = args,
            returnType = "Any?",
            description = tool.description
        )
    }
    
    private fun extractArgumentsFromSchema(inputSchema: JsonObject?): List<ToolCallSpec.Arg> {
        if (inputSchema == null) return emptyList()
        
        val properties = inputSchema["properties"] as? JsonObject ?: return emptyList()
        val required = (inputSchema["required"] as? kotlinx.serialization.json.JsonArray)
            ?.mapNotNull { (it as? JsonPrimitive)?.content }
            ?.toSet() ?: emptySet()
        
        return properties.entries.map { (name, schema) ->
            val schemaObj = schema as? JsonObject
            val type = schemaObj?.get("type")?.let { (it as? JsonPrimitive)?.content } ?: "Any"
            val isRequired = name in required
            
            ToolCallSpec.Arg(
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
