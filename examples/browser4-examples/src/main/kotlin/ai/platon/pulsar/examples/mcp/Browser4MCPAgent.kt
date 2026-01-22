package ai.platon.pulsar.examples.mcp

import ai.platon.pulsar.agentic.context.AgenticContexts
import ai.platon.pulsar.agentic.mcp.MCPBootstrap
import ai.platon.pulsar.agentic.mcp.MCPConfig
import ai.platon.pulsar.agentic.mcp.MCPPluginRegistry
import ai.platon.pulsar.agentic.mcp.MCPTransportType

/**
 * # Browser4 MCP Agent Example
 *
 * This example demonstrates how to integrate MCP (Model Context Protocol) servers
 * with Browser4's agentic framework. MCP allows external tools and services to be
 * seamlessly integrated into the agent's capabilities.
 *
 * ## What is MCP?
 * MCP (Model Context Protocol) is a standard protocol for connecting AI agents
 * with external tools and data sources. It enables:
 * - Dynamic tool discovery
 * - Standard tool execution interface
 * - Multiple transport protocols (STDIO, SSE, WebSocket)
 * - Easy integration with third-party services
 *
 * ## Example Overview:
 * This example shows how to integrate MCP tools with the Browser4 agent,
 * demonstrating:
 *
 * 1. **MCP Server Configuration** - How to configure connections to MCP servers
 * 2. **Tool Registration** - How to register MCP tools with the agent
 * 3. **Agent Integration** - How the agent uses MCP tools in natural language tasks
 *
 * ## Prerequisites:
 * - Browser4 with pulsar-agentic module
 * - MCP server running locally or remotely
 * - LLM API key configured in application.properties
 *
 * ## Configuration Examples:
 *
 * ### STDIO Transport (Local Process)
 * For MCP servers that run as local processes:
 * ```kotlin
 * val config = MCPConfig(
 *     serverName = "my-mcp-server",
 *     transportType = MCPTransportType.STDIO,
 *     command = "node",
 *     args = listOf("path/to/server.js")
 * )
 * ```
 *
 * ### SSE Transport (HTTP Streaming)
 * For MCP servers accessible via HTTP:
 * ```kotlin
 * val config = MCPConfig(
 *     serverName = "remote-server",
 *     transportType = MCPTransportType.SSE,
 *     url = "http://localhost:8080/mcp"
 * )
 * ```
 *
 * ### WebSocket Transport
 * For MCP servers using WebSocket protocol:
 * ```kotlin
 * val config = MCPConfig(
 *     serverName = "ws-server",
 *     transportType = MCPTransportType.WEBSOCKET,
 *     url = "ws://localhost:8080/ws"
 * )
 * ```
 *
 * ## Real-World MCP Servers:
 *
 * You can connect to various MCP servers:
 * - **Weather Services** - Get weather information
 * - **Database Tools** - Query and manage databases
 * - **File System Tools** - File operations and management
 * - **API Integrations** - Connect to third-party APIs
 * - **Custom Business Logic** - Your own domain-specific tools
 *
 * @see MCPConfig Configuration for MCP server connections
 * @see MCPBootstrap Helper for registering MCP servers
 * @see MCPPluginRegistry Central registry for managing MCP servers
 */

/**
 * Example: Connecting to an MCP Server
 *
 * This example shows how to configure and register an MCP server.
 * The tools from the MCP server will be automatically discovered and
 * made available to the Browser4 agent.
 */
suspend fun connectToMCPServer() {
    println("\n=== Connecting to MCP Server ===\n")
    
    try {
        // Example 1: Connect to a local STDIO MCP server
        // This would start a Node.js process running an MCP server
        val stdioConfig = MCPConfig(
            serverName = "weather-server",
            transportType = MCPTransportType.STDIO,
            command = "node",
            args = listOf("path/to/weather-server.js"),
            enabled = true  // Set to false to disable this server
        )
        
        // Example 2: Connect to an HTTP-based MCP server
        val sseConfig = MCPConfig(
            serverName = "database-tools",
            transportType = MCPTransportType.SSE,
            url = "http://localhost:8080/mcp",
            enabled = true
        )
        
        // Register a single server with auto-registration of tools
        // autoRegisterTools = true means tools will be available to the agent
        println("Registering MCP server...")
        MCPBootstrap.register(stdioConfig, autoRegisterTools = true)
        
        println("✓ MCP server registered successfully!")
        println("  The agent can now use tools from '${stdioConfig.serverName}'")
        
    } catch (e: Exception) {
        println("✗ Failed to register MCP server: ${e.message}")
        println("\nCommon issues:")
        println("  - MCP server is not running or not reachable")
        println("  - Incorrect command or URL configuration")
        println("  - Missing dependencies for the transport type")
    }
}

/**
 * Example: Using Multiple MCP Servers
 *
 * This example demonstrates how to connect to multiple MCP servers
 * simultaneously, combining tools from different sources.
 */
suspend fun connectToMultipleMCPServers() {
    println("\n=== Connecting to Multiple MCP Servers ===\n")
    
    try {
        // Configure multiple MCP servers
        val configs = listOf(
            MCPConfig(
                serverName = "weather-service",
                transportType = MCPTransportType.STDIO,
                command = "python",
                args = listOf("weather_mcp_server.py"),
                enabled = true
            ),
            MCPConfig(
                serverName = "database-tools",
                transportType = MCPTransportType.SSE,
                url = "http://localhost:8080/mcp",
                enabled = true
            ),
            MCPConfig(
                serverName = "file-operations",
                transportType = MCPTransportType.WEBSOCKET,
                url = "ws://localhost:8081/ws",
                enabled = true
            ),
            MCPConfig(
                serverName = "optional-service",
                transportType = MCPTransportType.STDIO,
                command = "node",
                args = listOf("optional-server.js"),
                enabled = false  // This server won't be connected
            )
        )
        
        // Register all enabled servers at once
        println("Registering multiple MCP servers...")
        val errors = MCPBootstrap.registerAll(configs, autoRegisterTools = true)
        
        if (errors.isEmpty()) {
            println("✓ All enabled servers registered successfully!")
        } else {
            println("⚠ Some servers failed to register:")
            errors.forEach { (name, exception) ->
                println("  ✗ $name: ${exception.message}")
            }
        }
        
        // List successfully registered servers
        val registeredServers = MCPPluginRegistry.instance.getRegisteredServers()
        println("\n✓ Registered servers (${registeredServers.size}):")
        registeredServers.forEach { serverName ->
            println("  - $serverName")
        }
        
    } catch (e: Exception) {
        println("✗ Failed to register MCP servers: ${e.message}")
        e.printStackTrace()
    }
}

/**
 * Example: Agent Using MCP Tools
 *
 * This example demonstrates how the Browser4 agent can use MCP tools
 * in natural language tasks. Once MCP servers are registered, their
 * tools become available to the agent automatically.
 */
suspend fun agentWithMCPTools() {
    println("\n=== Agent Using MCP Tools ===\n")
    
    try {
        // First, register MCP servers (in a real app, this would be done at startup)
        val config = MCPConfig(
            serverName = "calculator",
            transportType = MCPTransportType.STDIO,
            command = "node",
            args = listOf("calculator-server.js"),
            enabled = true
        )
        
        println("Registering MCP server with agent...")
        MCPBootstrap.register(config, autoRegisterTools = true)
        
        // Create or get the Browser4 agent
        val agent = AgenticContexts.getOrCreateAgent()
        
        // The agent can now use MCP tools in natural language
        val task = """
            Using the calculator MCP tools, please:
            1. Add 123 and 456
            2. Multiply the result by 2
            3. Tell me the final answer
        """.trimIndent()
        
        println("\n--- Agent Task ---")
        println(task)
        println("\n--- Agent Execution ---")
        
        try {
            val history = agent.run(task)
            println("\n--- Agent Result ---")
            println(history.finalResult)
        } catch (e: Exception) {
            println("✗ Agent execution failed: ${e.message}")
            println("\nNote: This requires:")
            println("  1. LLM API key configured in application.properties")
            println("  2. MCP server running and accessible")
            println("\nConfigure your LLM provider:")
            println("  llm.provider=openai")
            println("  llm.apiKey=your-api-key-here")
        }
        
    } catch (e: Exception) {
        println("✗ Error: ${e.message}")
        e.printStackTrace()
    } finally {
        // Clean up: close all MCP connections
        MCPBootstrap.close()
        println("\n✓ MCP connections closed.")
    }
}

/**
 * Example: Manual MCP Tool Discovery
 *
 * This example shows how to manually discover and inspect tools from
 * an MCP server without running them through the agent.
 */
suspend fun inspectMCPTools() {
    println("\n=== Inspecting MCP Tools ===\n")
    
    try {
        // Register an MCP server (without auto-registering tools to the agent)
        val config = MCPConfig(
            serverName = "example-server",
            transportType = MCPTransportType.STDIO,
            command = "node",
            args = listOf("example-server.js"),
            enabled = true
        )
        
        println("Connecting to MCP server...")
        MCPPluginRegistry.instance.registerMCPServer(config, autoRegisterTools = false)
        
        // Get the tool executor for this server
        val toolExecutor = MCPPluginRegistry.instance.getToolExecutor("example-server")
        
        if (toolExecutor != null) {
            // Display available tools and their descriptions
            println("\n--- Available Tools from '${config.serverName}' ---")
            println(toolExecutor.help())
            
            println("\n✓ Tools discovered and ready to use")
        } else {
            println("✗ Tool executor not found for '${config.serverName}'")
        }
        
    } catch (e: Exception) {
        println("✗ Failed to inspect MCP tools: ${e.message}")
    } finally {
        MCPPluginRegistry.instance.close()
    }
}

/**
 * Main entry point - demonstrates MCP integration patterns
 *
 * This example shows various ways to integrate MCP with Browser4.
 * Uncomment the examples you want to run.
 *
 * ## Running the Examples:
 *
 * 1. Ensure you have an MCP server running or available
 * 2. Update the configuration with your MCP server details
 * 3. Configure LLM API key in application.properties
 * 4. Run this file
 *
 * ## Learn More:
 *
 * - MCP Documentation: /pulsar-agentic/src/main/kotlin/ai/platon/pulsar/agentic/mcp/README.md
 * - Example MCP Plugin: /pulsar-agentic/src/test/kotlin/ai/platon/pulsar/agentic/mcp/examples/MCPPluginExample.kt
 * - Test MCP Server: /pulsar-tests/pulsar-tests-common/src/main/kotlin/ai/platon/pulsar/test/mcp/TestMCPServer.kt
 */
suspend fun main() {
    println("╔════════════════════════════════════════════════════════════════╗")
    println("║         Browser4 MCP (Model Context Protocol) Examples        ║")
    println("╚════════════════════════════════════════════════════════════════╝")
    
    println("""
        
        This example demonstrates MCP integration with Browser4.
        
        MCP (Model Context Protocol) allows you to extend the Browser4 agent
        with external tools and services. Tools from MCP servers become
        available to the agent automatically through natural language.
        
        Examples included:
        1. Connecting to a single MCP server
        2. Connecting to multiple MCP servers
        3. Using MCP tools with the Browser4 agent
        4. Inspecting available MCP tools
        
        Note: To run these examples, you need:
        - An MCP server (STDIO, SSE, or WebSocket)
        - LLM API key configured for agent integration
        
    """.trimIndent())
    
    println("\n" + "─".repeat(66))
    println("Select an example to run (uncomment in the code):")
    println("─".repeat(66))
    
    try {
        // Example 1: Connect to a single MCP server
        // Uncomment to run:
        // connectToMCPServer()
        
        // Example 2: Connect to multiple MCP servers
        // Uncomment to run:
        // connectToMultipleMCPServers()
        
        // Example 3: Use MCP tools with the agent (requires LLM configuration)
        // Uncomment to run:
        // agentWithMCPTools()
        
        // Example 4: Inspect MCP tools manually
        // Uncomment to run:
        // inspectMCPTools()
        
        println("\n💡 Tip: Uncomment the examples above to run them!")
        println("   Each example demonstrates a different MCP integration pattern.")
        
    } catch (e: Exception) {
        println("\n✗ Unexpected error: ${e.message}")
        e.printStackTrace()
    }
    
    println("\n╔════════════════════════════════════════════════════════════════╗")
    println("║                   MCP Examples Guide Completed                 ║")
    println("╚════════════════════════════════════════════════════════════════╝")
}
