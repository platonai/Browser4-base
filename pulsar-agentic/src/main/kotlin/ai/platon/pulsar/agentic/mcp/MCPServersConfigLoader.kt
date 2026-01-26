package ai.platon.pulsar.agentic.mcp

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.RequiredDirectory
import ai.platon.pulsar.common.concurrent.GracefulScheduledExecutor
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.serialize.json.pulsarObjectMapper
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import java.io.Closeable
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import kotlin.io.path.exists
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.isRegularFile

/**
 * Configuration for an individual MCP server in Claude-compatible JSON format.
 *
 * Example:
 * ```json
 * {
 *   "command": "npx",
 *   "args": ["-y", "@modelcontextprotocol/server-filesystem", "/Users/yourname/Desktop"]
 * }
 * ```
 *
 * @property command The command to start the MCP server.
 * @property args The arguments for the server command.
 * @property env Optional environment variables for the server process.
 * @property enabled Whether this MCP server is enabled (defaults to true).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class MCPServerJsonConfig(
    val command: String,
    val args: List<String> = emptyList(),
    val env: Map<String, String>? = null,
    val enabled: Boolean = true
)

/**
 * Root structure for Claude-compatible MCP servers JSON configuration file.
 *
 * Example:
 * ```json
 * {
 *   "mcpServers": {
 *     "filesystem": {
 *       "command": "npx",
 *       "args": ["-y", "@modelcontextprotocol/server-filesystem", "/Users/yourname/Desktop"]
 *     },
 *     "puppeteer": {
 *       "command": "npx",
 *       "args": ["-y", "@modelcontextprotocol/server-puppeteer"]
 *     }
 *   }
 * }
 * ```
 *
 * @property mcpServers Map of server name to server configuration.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class MCPServersJsonConfigFile(
    val mcpServers: Map<String, MCPServerJsonConfig> = emptyMap()
)

/**
 * Loads and manages MCP server configurations from JSON files.
 *
 * This class handles:
 * - Parsing Claude-compatible JSON configuration files
 * - Converting JSON configs to MCPConfig objects
 * - Optional periodic scanning and auto-loading of configuration changes
 *
 * @property configDir The directory to scan for configuration files.
 * @property registry The MCP plugin registry to register servers with.
 */
class MCPServersConfigLoader(
    private val configDir: Path = MCPPaths.MCP_CONFIG_DIR,
    private val registry: MCPPluginRegistry = MCPPluginRegistry.instance
) : Closeable {

    private val logger = getLogger(this)
    private val objectMapper = pulsarObjectMapper()

    private var configWatcher: ConfigWatcher? = null
    private val loadedFiles = mutableMapOf<Path, Long>()
    private val loadedServers = mutableSetOf<String>()

    /**
     * Loads MCP server configurations from a single JSON file.
     *
     * @param configFile Path to the JSON configuration file.
     * @param autoRegisterTools Whether to auto-register tools with CustomToolRegistry.
     * @return Map of server names to any exceptions that occurred during registration.
     */
    suspend fun loadFromFile(configFile: Path, autoRegisterTools: Boolean = true): Map<String, Exception> {
        if (!configFile.exists() || !configFile.isRegularFile()) {
            logger.warn("MCP config file does not exist or is not a regular file: {}", configFile)
            return emptyMap()
        }

        return try {
            logger.info("Loading MCP servers configuration from: {}", configFile)

            val jsonConfig = objectMapper.readValue<MCPServersJsonConfigFile>(configFile.toFile())
            val mcpConfigs = convertToMCPConfigs(jsonConfig)

            if (mcpConfigs.isEmpty()) {
                logger.info("No MCP servers found in config file: {}", configFile)
                return emptyMap()
            }

            val errors = registry.registerMCPServers(mcpConfigs, autoRegisterTools)

            // Track loaded servers
            mcpConfigs.forEach { config ->
                if (!errors.containsKey(config.serverName)) {
                    loadedServers.add(config.serverName)
                }
            }

            // Track loaded file
            loadedFiles[configFile] = configFile.getLastModifiedTime().toMillis()

            logger.info(
                "Loaded {} MCP servers from {}, {} succeeded, {} failed",
                mcpConfigs.size,
                configFile,
                mcpConfigs.size - errors.size,
                errors.size
            )

            errors
        } catch (e: Exception) {
            logger.error("Failed to load MCP config from {}: {}", configFile, e.message, e)
            mapOf("__file_error__" to e)
        }
    }

    /**
     * Loads MCP server configurations from a single JSON file (blocking version).
     *
     * @param configFile Path to the JSON configuration file.
     * @param autoRegisterTools Whether to auto-register tools with CustomToolRegistry.
     * @return Map of server names to any exceptions that occurred during registration.
     */
    fun loadFromFileBlocking(configFile: Path, autoRegisterTools: Boolean = true): Map<String, Exception> {
        return runBlocking { loadFromFile(configFile, autoRegisterTools) }
    }

    /**
     * Loads MCP server configurations from all JSON files in the config directory.
     *
     * @param autoRegisterTools Whether to auto-register tools with CustomToolRegistry.
     * @return Map of server names to any exceptions that occurred during registration.
     */
    suspend fun loadFromDirectory(autoRegisterTools: Boolean = true): Map<String, Exception> {
        if (!configDir.exists()) {
            logger.info("MCP config directory does not exist: {}", configDir)
            return emptyMap()
        }

        val allErrors = mutableMapOf<String, Exception>()

        val jsonFiles = Files.list(configDir)
            .filter { it.toString().endsWith(".json") && it.isRegularFile() }
            .toList()

        for (file in jsonFiles) {
            val errors = loadFromFile(file, autoRegisterTools)
            allErrors.putAll(errors)
        }

        return allErrors
    }

    /**
     * Loads MCP server configurations from all JSON files in the config directory (blocking version).
     *
     * @param autoRegisterTools Whether to auto-register tools with CustomToolRegistry.
     * @return Map of server names to any exceptions that occurred during registration.
     */
    fun loadFromDirectoryBlocking(autoRegisterTools: Boolean = true): Map<String, Exception> {
        return runBlocking { loadFromDirectory(autoRegisterTools) }
    }

    /**
     * Starts periodic scanning of the config directory for changes.
     *
     * @param interval The interval between scans.
     * @param initialDelay The delay before the first scan.
     * @param autoRegisterTools Whether to auto-register tools with CustomToolRegistry.
     */
    fun startPeriodicScan(
        interval: Duration = DEFAULT_SCAN_INTERVAL,
        initialDelay: Duration = DEFAULT_INITIAL_DELAY,
        autoRegisterTools: Boolean = true
    ) {
        if (configWatcher != null) {
            logger.warn("Periodic scan is already running")
            return
        }

        logger.info(
            "Starting periodic MCP config scan, interval: {}, initialDelay: {}, configDir: {}",
            interval, initialDelay, configDir
        )

        configWatcher = ConfigWatcher(interval, initialDelay, autoRegisterTools).also { it.start() }
    }

    /**
     * Stops the periodic scanning.
     */
    fun stopPeriodicScan() {
        configWatcher?.close()
        configWatcher = null
        logger.info("Stopped periodic MCP config scan")
    }

    /**
     * Checks for configuration changes and reloads modified files.
     *
     * @param autoRegisterTools Whether to auto-register tools with CustomToolRegistry.
     * @return Map of server names to any exceptions that occurred during registration.
     */
    suspend fun checkAndReload(autoRegisterTools: Boolean = true): Map<String, Exception> {
        if (!configDir.exists()) {
            return emptyMap()
        }

        val allErrors = mutableMapOf<String, Exception>()

        // Check for new or modified files
        val jsonFiles = Files.list(configDir)
            .filter { it.toString().endsWith(".json") && it.isRegularFile() }
            .toList()

        for (file in jsonFiles) {
            val lastModified = file.getLastModifiedTime().toMillis()
            val previousModified = loadedFiles[file]

            if (previousModified == null || lastModified > previousModified) {
                logger.info("Detected changes in MCP config file: {}", file)

                // Unregister previously loaded servers from this file before reloading
                if (previousModified != null) {
                    unloadServersFromFile(file)
                }

                val errors = loadFromFile(file, autoRegisterTools)
                allErrors.putAll(errors)
            }
        }

        // Check for deleted files
        val currentFiles = Files.list(configDir)
            .filter { it.toString().endsWith(".json") && it.isRegularFile() }
            .toList()
            .toSet()

        for (loadedFile in loadedFiles.keys.toList()) {
            if (loadedFile !in currentFiles) {
                logger.info("Detected deleted MCP config file: {}", loadedFile)
                unloadServersFromFile(loadedFile)
                loadedFiles.remove(loadedFile)
            }
        }

        return allErrors
    }

    /**
     * Gets the names of all loaded MCP servers.
     *
     * @return Set of server names.
     */
    fun getLoadedServers(): Set<String> = loadedServers.toSet()

    /**
     * Gets the paths of all loaded configuration files.
     *
     * @return Set of file paths.
     */
    fun getLoadedFiles(): Set<Path> = loadedFiles.keys.toSet()

    override fun close() {
        stopPeriodicScan()
        // Note: We don't unregister servers here as they may be used elsewhere
    }

    private fun convertToMCPConfigs(jsonConfig: MCPServersJsonConfigFile): List<MCPConfig> {
        return jsonConfig.mcpServers.map { (serverName, serverConfig) ->
            MCPConfig(
                serverName = serverName,
                transportType = MCPTransportType.STDIO,
                command = serverConfig.command,
                args = serverConfig.args,
                enabled = serverConfig.enabled
            )
        }
    }

    private fun unloadServersFromFile(configFile: Path) {
        try {
            val jsonConfig = objectMapper.readValue<MCPServersJsonConfigFile>(configFile.toFile())
            jsonConfig.mcpServers.keys.forEach { serverName ->
                if (registry.isRegistered(serverName)) {
                    registry.unregisterMCPServer(serverName)
                    loadedServers.remove(serverName)
                    logger.info("Unregistered MCP server: {}", serverName)
                }
            }
        } catch (e: Exception) {
            logger.warn("Failed to parse config file for unloading: {}", configFile, e)
        }
    }

    /**
     * Internal watcher class for periodic configuration scanning.
     */
    private inner class ConfigWatcher(
        interval: Duration,
        initialDelay: Duration,
        private val autoRegisterTools: Boolean
    ) : GracefulScheduledExecutor(initialDelay, interval) {

        override fun run() {
            try {
                runBlocking {
                    checkAndReload(autoRegisterTools)
                }
            } catch (e: Exception) {
                logger.error("Error during MCP config scan: {}", e.message, e)
            }
        }
    }

    companion object {
        /**
         * Default interval for periodic config scanning.
         */
        val DEFAULT_SCAN_INTERVAL: Duration = Duration.ofSeconds(30)

        /**
         * Default delay before starting the first scan.
         */
        val DEFAULT_INITIAL_DELAY: Duration = Duration.ofSeconds(5)

        /**
         * Default configuration file name.
         */
        const val DEFAULT_CONFIG_FILE_NAME = "mcp-servers.json"

        /**
         * Parses a JSON string into MCPServersJsonConfigFile.
         *
         * @param json The JSON string to parse.
         * @return The parsed configuration file.
         */
        fun parseJson(json: String): MCPServersJsonConfigFile {
            return pulsarObjectMapper().readValue(json)
        }

        /**
         * Converts MCPServersJsonConfigFile to a list of MCPConfig objects.
         *
         * @param jsonConfig The JSON configuration to convert.
         * @return List of MCPConfig objects.
         */
        fun toMCPConfigs(jsonConfig: MCPServersJsonConfigFile): List<MCPConfig> {
            return jsonConfig.mcpServers.map { (serverName, serverConfig) ->
                MCPConfig(
                    serverName = serverName,
                    transportType = MCPTransportType.STDIO,
                    command = serverConfig.command,
                    args = serverConfig.args,
                    enabled = serverConfig.enabled
                )
            }
        }
    }
}

/**
 * Paths specific to MCP configuration.
 */
object MCPPaths {

    /**
     * Directory for MCP server configuration files.
     */
    @RequiredDirectory
    val MCP_CONFIG_DIR: Path = AppPaths.CONFIG_DIR.resolve("mcp")

    /**
     * Default MCP servers configuration file path.
     */
    @RequiredDirectory
    val MCP_SERVERS_CONFIG_FILE: Path = MCP_CONFIG_DIR.resolve(MCPServersConfigLoader.DEFAULT_CONFIG_FILE_NAME)

    init {
        AppPaths.createRequiredResources(MCPPaths::class)
    }
}
