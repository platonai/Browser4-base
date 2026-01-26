package ai.platon.pulsar.agentic.mcp

import ai.platon.pulsar.agentic.tools.CustomToolRegistry
import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import java.time.Duration

/**
 * Bootstrap helpers for wiring MCP (Model Context Protocol) tools into the agent.
 *
 * Contract:
 * - Call [register] once during app/library initialization.
 * - For STDIO servers, ensure [MCPConfig.command] is resolvable in PATH.
 * - Tools will be registered into [CustomToolRegistry] under domain `mcp.<serverName>`.
 *
 * JSON Configuration Support:
 * - Use [registerFromJsonFile] to load MCP servers from a Claude-compatible JSON config file.
 * - Use [registerFromJsonDirectory] to load all JSON configs from a directory.
 * - Use [startPeriodicConfigScan] to enable automatic reloading of config changes.
 *
 * Example JSON format:
 * ```json
 * {
 *   "mcpServers": {
 *     "filesystem": {
 *       "command": "npx",
 *       "args": ["-y", "@modelcontextprotocol/server-filesystem", "/Users/yourname/Desktop"]
 *     }
 *   }
 * }
 * ```
 */
object MCPBootstrap {

    private var configLoader: MCPServersConfigLoader? = null

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
     * Register MCP servers from a JSON configuration file in Claude-compatible format.
     *
     * @param configFile Path to the JSON configuration file.
     * @param autoRegisterTools If true, automatically registers the MCP tools with CustomToolRegistry.
     * @return Map of server names to any exceptions that occurred during registration.
     */
    fun registerFromJsonFile(configFile: Path, autoRegisterTools: Boolean = true): Map<String, Exception> {
        return getOrCreateConfigLoader().loadFromFileBlocking(configFile, autoRegisterTools)
    }

    /**
     * Register MCP servers from all JSON files in the specified directory.
     *
     * @param configDir Directory containing JSON configuration files.
     * @param autoRegisterTools If true, automatically registers the MCP tools with CustomToolRegistry.
     * @return Map of server names to any exceptions that occurred during registration.
     */
    fun registerFromJsonDirectory(
        configDir: Path = MCPPaths.MCP_CONFIG_DIR,
        autoRegisterTools: Boolean = true
    ): Map<String, Exception> {
        configLoader = MCPServersConfigLoader(configDir)
        return configLoader!!.loadFromDirectoryBlocking(autoRegisterTools)
    }

    /**
     * Register MCP servers from the default JSON config file.
     *
     * The default config file is located at: `{APP_DATA_DIR}/config/mcp/mcp-servers.json`
     *
     * @param autoRegisterTools If true, automatically registers the MCP tools with CustomToolRegistry.
     * @return Map of server names to any exceptions that occurred during registration.
     */
    fun registerFromDefaultConfig(autoRegisterTools: Boolean = true): Map<String, Exception> {
        return registerFromJsonFile(MCPPaths.MCP_SERVERS_CONFIG_FILE, autoRegisterTools)
    }

    /**
     * Start periodic scanning of the MCP configuration directory.
     *
     * This enables automatic detection and loading of new or modified configuration files.
     *
     * @param configDir Directory to scan for configuration files.
     * @param interval Interval between scans.
     * @param initialDelay Delay before the first scan.
     * @param autoRegisterTools If true, automatically registers the MCP tools with CustomToolRegistry.
     */
    fun startPeriodicConfigScan(
        configDir: Path = MCPPaths.MCP_CONFIG_DIR,
        interval: Duration = MCPServersConfigLoader.DEFAULT_SCAN_INTERVAL,
        initialDelay: Duration = MCPServersConfigLoader.DEFAULT_INITIAL_DELAY,
        autoRegisterTools: Boolean = true
    ) {
        configLoader = MCPServersConfigLoader(configDir)
        configLoader!!.startPeriodicScan(interval, initialDelay, autoRegisterTools)
    }

    /**
     * Stop the periodic configuration scanning.
     */
    fun stopPeriodicConfigScan() {
        configLoader?.stopPeriodicScan()
    }

    /**
     * Get the names of all MCP servers loaded from JSON configuration files.
     *
     * @return Set of server names.
     */
    fun getLoadedServersFromJson(): Set<String> {
        return configLoader?.getLoadedServers() ?: emptySet()
    }

    /**
     * Close all MCP connections and unregister their tool executors.
     */
    fun close() {
        configLoader?.close()
        configLoader = null
        MCPPluginRegistry.instance.close()
    }

    private fun getOrCreateConfigLoader(): MCPServersConfigLoader {
        if (configLoader == null) {
            configLoader = MCPServersConfigLoader()
        }
        return configLoader!!
    }
}

