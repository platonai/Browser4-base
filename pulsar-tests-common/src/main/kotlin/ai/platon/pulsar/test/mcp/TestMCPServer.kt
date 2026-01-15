package ai.platon.pulsar.test.mcp

import ai.platon.pulsar.common.getLogger
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import java.io.Closeable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * A minimal MCP server implementation for testing purposes.
 * 
 * This server provides a simplified Model Context Protocol (MCP) implementation
 * suitable for testing MCP clients. It implements basic MCP protocol features:
 * - Tool listing (list_tools)
 * - Tool execution (call_tool)
 * 
 * The server exposes three simple tools:
 * - echo: Returns the input message
 * - add: Adds two numbers
 * - multiply: Multiplies two numbers
 * 
 * This implementation uses HTTP/JSON instead of the full MCP transport layer,
 * making it easier to set up and test without external dependencies.
 *
 * @property serverName The name of the MCP server.
 * @property serverVersion The version of the MCP server.
 */
@RestController
@RequestMapping("/mcp")
class TestMCPServer(
    private val serverName: String = "test-mcp-server",
    private val serverVersion: String = "1.0.0"
) : Closeable {
    
    private val logger = getLogger(this)
    private val objectMapper: ObjectMapper = jacksonObjectMapper()
    private val requestIdCounter = AtomicLong(0)
    private val isRunning = AtomicBoolean(false)
    
    // Store available tools
    private val tools = ConcurrentHashMap<String, ToolDefinition>()
    
    init {
        registerDefaultTools()
        isRunning.set(true)
        logger.info("TestMCPServer initialized with name: {}, version: {}", serverName, serverVersion)
    }
    
    /**
     * Returns server information.
     */
    @GetMapping("/info", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getInfo(): Map<String, Any> {
        return mapOf(
            "name" to serverName,
            "version" to serverVersion,
            "capabilities" to mapOf(
                "tools" to mapOf<String, Any>()
            )
        )
    }
    
    /**
     * Lists all available tools.
     * Corresponds to the MCP list_tools request.
     */
    @PostMapping("/list_tools", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun listTools(): Map<String, Any> {
        logger.debug("Received list_tools request")
        
        val toolsList = tools.values.map { tool ->
            mapOf(
                "name" to tool.name,
                "description" to tool.description,
                "inputSchema" to tool.inputSchema
            )
        }
        
        return mapOf(
            "tools" to toolsList
        )
    }
    
    /**
     * Executes a tool with the given arguments.
     * Corresponds to the MCP call_tool request.
     */
    @PostMapping("/call_tool", produces = [MediaType.APPLICATION_JSON_VALUE], consumes = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun callTool(@RequestBody request: JsonNode): Map<String, Any> {
        val toolName = request.get("name")?.asText() 
            ?: throw IllegalArgumentException("Tool name is required")
        val arguments = request.get("arguments") ?: objectMapper.createObjectNode()
        
        logger.debug("Received call_tool request for tool: {}", toolName)
        
        val tool = tools[toolName]
            ?: throw IllegalArgumentException("Tool not found: $toolName")
        
        return try {
            val result = tool.handler(arguments)
            mapOf(
                "content" to listOf(
                    mapOf(
                        "type" to "text",
                        "text" to result
                    )
                )
            )
        } catch (e: Exception) {
            logger.error("Error executing tool {}: {}", toolName, e.message, e)
            mapOf(
                "isError" to true,
                "content" to listOf(
                    mapOf(
                        "type" to "text",
                        "text" to "Error: ${e.message}"
                    )
                )
            )
        }
    }
    
    /**
     * Checks if the server is running.
     */
    fun isRunning(): Boolean = isRunning.get()
    
    /**
     * Registers the default set of tools.
     */
    private fun registerDefaultTools() {
        // Echo tool
        registerTool(
            ToolDefinition(
                name = "echo",
                description = "Echoes back the input message",
                inputSchema = mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "message" to mapOf(
                            "type" to "string",
                            "description" to "The message to echo back"
                        )
                    ),
                    "required" to listOf("message")
                )
            ) { args ->
                val message = args.get("message")?.asText()
                    ?: throw IllegalArgumentException("message argument is required")
                message
            }
        )
        
        // Add tool
        registerTool(
            ToolDefinition(
                name = "add",
                description = "Adds two numbers together",
                inputSchema = mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "a" to mapOf(
                            "type" to "number",
                            "description" to "First number"
                        ),
                        "b" to mapOf(
                            "type" to "number",
                            "description" to "Second number"
                        )
                    ),
                    "required" to listOf("a", "b")
                )
            ) { args ->
                val a = args.get("a")?.asDouble()
                    ?: throw IllegalArgumentException("a argument must be a number")
                val b = args.get("b")?.asDouble()
                    ?: throw IllegalArgumentException("b argument must be a number")
                (a + b).toString()
            }
        )
        
        // Multiply tool
        registerTool(
            ToolDefinition(
                name = "multiply",
                description = "Multiplies two numbers together",
                inputSchema = mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "a" to mapOf(
                            "type" to "number",
                            "description" to "First number"
                        ),
                        "b" to mapOf(
                            "type" to "number",
                            "description" to "Second number"
                        )
                    ),
                    "required" to listOf("a", "b")
                )
            ) { args ->
                val a = args.get("a")?.asDouble()
                    ?: throw IllegalArgumentException("a argument must be a number")
                val b = args.get("b")?.asDouble()
                    ?: throw IllegalArgumentException("b argument must be a number")
                (a * b).toString()
            }
        )
        
        logger.info("Registered {} default tools: {}", tools.size, tools.keys.joinToString(", "))
    }
    
    /**
     * Registers a tool with the server.
     */
    private fun registerTool(tool: ToolDefinition) {
        tools[tool.name] = tool
        logger.debug("Registered tool: {}", tool.name)
    }
    
    override fun close() {
        isRunning.set(false)
        tools.clear()
        logger.info("TestMCPServer closed")
    }
    
    /**
     * Definition of a tool that can be executed by the MCP server.
     */
    private data class ToolDefinition(
        val name: String,
        val description: String,
        val inputSchema: Map<String, Any>,
        val handler: (JsonNode) -> String
    )
}
