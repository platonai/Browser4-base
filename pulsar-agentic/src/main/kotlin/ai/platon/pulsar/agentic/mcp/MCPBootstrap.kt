package ai.platon.pulsar.agentic.mcp

import ai.platon.pulsar.agentic.tools.CustomToolRegistry
import kotlinx.coroutines.runBlocking

/**
 * Bootstrap helpers for wiring MCP (Model Context Protocol) tools into the agent.
 *
 * Contract:
 * - Call [register] once during app/library initialization.
 * - For STDIO servers, ensure [MCPConfig.command] is resolvable in PATH.
 * - Tools will be registered into [CustomToolRegistry] under domain `mcp.<serverName>`.
 */
object MCPBootstrap {

    /**
     * Register one MCP server and (by default) auto-register its tools into [CustomToolRegistry].
     */
    fun register(config: MCPConfig, autoRegisterTools: Boolean = true) {
        runBlocking {
            MCPPluginRegistry.instance.registerMCPServer(config, autoRegisterTools)
        }
    }

    /**
     * Register multiple MCP servers. Returns a map of serverName -> exception for failures.
     */
    fun registerAll(configs: List<MCPConfig>, autoRegisterTools: Boolean = true): Map<String, Exception> {
        return runBlocking {
            MCPPluginRegistry.instance.registerMCPServers(configs, autoRegisterTools)
        }
    }

    /**
     * Close all MCP connections and unregister their tool executors.
     */
    fun close() {
        MCPPluginRegistry.instance.close()
    }
}

