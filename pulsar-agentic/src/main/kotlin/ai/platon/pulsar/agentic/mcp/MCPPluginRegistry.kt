package ai.platon.pulsar.agentic.mcp

import ai.platon.pulsar.agentic.tools.CustomToolRegistry
import ai.platon.pulsar.common.getLogger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.io.Closeable

/**
 * Registry for managing multiple MCP server connections.
 * 
 * This registry maintains a collection of MCP client managers, handles their
 * lifecycle, and integrates their tools with the CustomToolRegistry.
 */
class MCPPluginRegistry : Closeable {
    
    private val logger = getLogger(this)
    
    private val clientManagers = mutableMapOf<String, MCPClientManager>()
    private val toolExecutors = mutableMapOf<String, MCPToolExecutor>()
    
    /**
     * Registers and connects to an MCP server.
     *
     * @param config The configuration for the MCP server.
     * @param autoRegisterTools If true, automatically registers the MCP tools with CustomToolRegistry.
     * @throws IllegalArgumentException if a server with the same name is already registered.
     * @throws Exception if connection to the server fails.
     */
    suspend fun registerMCPServer(config: MCPConfig, autoRegisterTools: Boolean = true) {
        if (clientManagers.containsKey(config.serverName)) {
            throw IllegalArgumentException("MCP server '${config.serverName}' is already registered")
        }
        
        if (!config.enabled) {
            logger.info("MCP server '{}' is disabled, skipping registration", config.serverName)
            return
        }
        
        try {
            logger.info("Registering MCP server: {}", config.serverName)
            
            val clientManager = MCPClientManager(config)
            clientManager.connect()
            
            val toolExecutor = MCPToolExecutor(clientManager)
            
            clientManagers[config.serverName] = clientManager
            toolExecutors[config.serverName] = toolExecutor
            
            if (autoRegisterTools) {
                registerToolsWithCustomRegistry(toolExecutor)
            }
            
            logger.info(
                "Successfully registered MCP server '{}' with {} tools",
                config.serverName,
                toolExecutor.getAvailableToolNames().size
            )
        } catch (e: Exception) {
            logger.error("Failed to register MCP server '{}': {}", config.serverName, e.message, e)
            // Clean up if registration failed
            clientManagers.remove(config.serverName)
            toolExecutors.remove(config.serverName)
            throw e
        }
    }
    
    /**
     * Registers multiple MCP servers concurrently.
     *
     * @param configs The list of MCP server configurations.
     * @param autoRegisterTools If true, automatically registers tools with CustomToolRegistry.
     * @return A map of server names to any exceptions that occurred during registration.
     */
    suspend fun registerMCPServers(
        configs: List<MCPConfig>,
        autoRegisterTools: Boolean = true
    ): Map<String, Exception> = coroutineScope {
        val results = configs.map { config ->
            async {
                try {
                    registerMCPServer(config, autoRegisterTools)
                    config.serverName to null
                } catch (e: Exception) {
                    logger.warn("Failed to register MCP server '{}': {}", config.serverName, e.message)
                    config.serverName to e
                }
            }
        }.awaitAll()
        
        results.mapNotNull { (name, exception) ->
            exception?.let { name to it }
        }.toMap()
    }
    
    /**
     * Unregisters an MCP server and closes its connection.
     *
     * @param serverName The name of the server to unregister.
     * @return true if the server was unregistered, false if it wasn't registered.
     */
    fun unregisterMCPServer(serverName: String): Boolean {
        val clientManager = clientManagers.remove(serverName) ?: return false
        val toolExecutor = toolExecutors.remove(serverName)
        
        // Unregister tools from CustomToolRegistry
        toolExecutor?.let {
            val domain = it.domain
            CustomToolRegistry.instance.unregister(domain)
            logger.debug("Unregistered tools for MCP server '{}' from CustomToolRegistry", serverName)
        }
        
        // Close the client connection
        try {
            clientManager.close()
            logger.info("Unregistered MCP server: {}", serverName)
        } catch (e: Exception) {
            logger.warn("Error closing MCP client manager for '{}': {}", serverName, e.message)
        }
        
        return true
    }
    
    /**
     * Gets a client manager by server name.
     *
     * @param serverName The name of the server.
     * @return The client manager, or null if not found.
     */
    fun getClientManager(serverName: String): MCPClientManager? {
        return clientManagers[serverName]
    }
    
    /**
     * Gets a tool executor by server name.
     *
     * @param serverName The name of the server.
     * @return The tool executor, or null if not found.
     */
    fun getToolExecutor(serverName: String): MCPToolExecutor? {
        return toolExecutors[serverName]
    }
    
    /**
     * Gets all registered MCP server names.
     *
     * @return Set of server names.
     */
    fun getRegisteredServers(): Set<String> {
        return clientManagers.keys.toSet()
    }
    
    /**
     * Gets the total count of registered servers.
     *
     * @return Number of registered servers.
     */
    fun size(): Int = clientManagers.size
    
    /**
     * Checks if a server is registered.
     *
     * @param serverName The name of the server.
     * @return true if registered, false otherwise.
     */
    fun isRegistered(serverName: String): Boolean {
        return clientManagers.containsKey(serverName)
    }
    
    override fun close() {
        logger.info("Closing all MCP server connections")
        
        clientManagers.keys.toList().forEach { serverName ->
            try {
                unregisterMCPServer(serverName)
            } catch (e: Exception) {
                logger.warn("Error unregistering MCP server '{}': {}", serverName, e.message)
            }
        }
        
        clientManagers.clear()
        toolExecutors.clear()
    }
    
    private fun registerToolsWithCustomRegistry(toolExecutor: MCPToolExecutor) {
        try {
            CustomToolRegistry.instance.register(toolExecutor)
            logger.debug(
                "Registered {} tools from MCP server '{}' with CustomToolRegistry",
                toolExecutor.getAvailableToolNames().size,
                toolExecutor.domain
            )
        } catch (e: Exception) {
            logger.warn(
                "Failed to register MCP tools with CustomToolRegistry: {}",
                e.message
            )
            throw e
        }
    }
    
    companion object {
        /**
         * Singleton instance of the MCP plugin registry.
         */
        val instance: MCPPluginRegistry by lazy { MCPPluginRegistry() }
    }
}
