package ai.platon.pulsar.agentic.mcp.examples

import ai.platon.pulsar.agentic.mcp.MCPConfig
import ai.platon.pulsar.agentic.mcp.MCPPluginRegistry
import ai.platon.pulsar.agentic.mcp.MCPTransportType
import ai.platon.pulsar.agentic.ToolCall
import kotlinx.coroutines.runBlocking

/**
 * Example demonstrating how to use MCP plugin support.
 * 
 * This example shows:
 * 1. Configuring an MCP server connection
 * 2. Registering the server with the plugin registry
 * 3. Discovering available tools
 * 4. Executing tools through the standard interface
 * 
 * Note: This is a demonstration example. To run it, you'll need an actual
 * MCP server running (e.g., the weather server from the MCP SDK samples).
 */
object MCPPluginExample {
    
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        // Example 1: Connect to a local MCP server using STDIO
        connectToStdioServer()
        
        // Example 2: Connect to a remote MCP server using SSE
        // connectToSSEServer()
        
        // Example 3: Register multiple servers
        // connectToMultipleServers()
    }
    
    /**
     * Example: Connect to an MCP server using STDIO transport.
     */
    private suspend fun connectToStdioServer() {
        println("=== Example 1: STDIO Transport ===")
        
        // Configure the MCP server
        // This assumes you have a Node.js MCP server script
        val config = MCPConfig(
            serverName = "weather-server",
            transportType = MCPTransportType.STDIO,
            command = "node",
            args = listOf("path/to/weather-server.js")
        )
        
        try {
            // Register and connect to the server
            println("Connecting to weather-server...")
            MCPPluginRegistry.instance.registerMCPServer(config)
            
            // Get the tool executor for this server
            val toolExecutor = MCPPluginRegistry.instance.getToolExecutor("weather-server")
            
            if (toolExecutor != null) {
                // List available tools
                println("\nAvailable tools:")
                println(toolExecutor.help())
                
                // Execute a tool
                println("\nExecuting tool: get_forecast")
                val toolCall = ToolCall(
                    domain = "mcp.weather-server",
                    method = "get_forecast",
                    arguments = mutableMapOf(
                        "location" to "San Francisco, CA",
                        "days" to "3"
                    )
                )
                
                val result = toolExecutor.execute(toolCall)
                println("Result: ${result.value}")
            } else {
                println("Tool executor not found")
            }
        } catch (e: Exception) {
            println("Error: ${e.message}")
            e.printStackTrace()
        } finally {
            // Clean up
            MCPPluginRegistry.instance.close()
        }
    }
    
    /**
     * Example: Connect to an MCP server using SSE transport.
     */
    private suspend fun connectToSSEServer() {
        println("=== Example 2: SSE Transport ===")
        
        val config = MCPConfig(
            serverName = "remote-server",
            transportType = MCPTransportType.SSE,
            url = "http://localhost:8080/sse"
        )
        
        try {
            println("Connecting to remote-server...")
            MCPPluginRegistry.instance.registerMCPServer(config)
            
            val toolExecutor = MCPPluginRegistry.instance.getToolExecutor("remote-server")
            toolExecutor?.let {
                println("\nAvailable tools:")
                println(it.help())
            }
        } catch (e: Exception) {
            println("Error: ${e.message}")
        } finally {
            MCPPluginRegistry.instance.close()
        }
    }
    
    /**
     * Example: Register multiple MCP servers concurrently.
     */
    private suspend fun connectToMultipleServers() {
        println("=== Example 3: Multiple Servers ===")
        
        val configs = listOf(
            MCPConfig(
                serverName = "weather-server",
                transportType = MCPTransportType.STDIO,
                command = "node",
                args = listOf("weather-server.js")
            ),
            MCPConfig(
                serverName = "calendar-server",
                transportType = MCPTransportType.STDIO,
                command = "python",
                args = listOf("calendar-server.py")
            ),
            MCPConfig(
                serverName = "disabled-server",
                transportType = MCPTransportType.STDIO,
                command = "node",
                args = listOf("disabled.js"),
                enabled = false  // This server won't be connected
            )
        )
        
        try {
            println("Registering multiple servers...")
            val errors = MCPPluginRegistry.instance.registerMCPServers(configs)
            
            if (errors.isEmpty()) {
                println("All servers registered successfully")
            } else {
                println("Some servers failed to register:")
                errors.forEach { (name, exception) ->
                    println("  - $name: ${exception.message}")
                }
            }
            
            // List all registered servers
            val registeredServers = MCPPluginRegistry.instance.getRegisteredServers()
            println("\nRegistered servers: ${registeredServers.joinToString(", ")}")
            
            // Show tools from all servers
            registeredServers.forEach { serverName ->
                val toolExecutor = MCPPluginRegistry.instance.getToolExecutor(serverName)
                toolExecutor?.let {
                    println("\nTools from $serverName:")
                    println(it.help())
                }
            }
        } catch (e: Exception) {
            println("Error: ${e.message}")
        } finally {
            MCPPluginRegistry.instance.close()
        }
    }
}
