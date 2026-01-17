package ai.platon.pulsar.agentic.mcp

import ai.platon.pulsar.common.getLogger
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.SseClientTransport
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import io.modelcontextprotocol.kotlin.sdk.client.WebSocketClientTransport
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.Tool
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import java.io.Closeable
import java.util.concurrent.TimeUnit

/**
 * Manages MCP client connections and lifecycle.
 *
 * This class handles connecting to MCP servers, managing the client lifecycle,
 * and providing access to tools exposed by connected servers.
 *
 * @property config The configuration for the MCP server connection.
 * @property clientInfo Information about this MCP client.
 * @property processTerminationTimeoutSeconds Timeout in seconds for graceful process termination.
 */
class MCPClientManager(
    private val config: MCPConfig,
    private val clientInfo: Implementation = Implementation(
        name = "pulsar-agentic-mcp-client",
        version = "1.0.0"
    ),
    private val processTerminationTimeoutSeconds: Long = DEFAULT_PROCESS_TERMINATION_TIMEOUT_SECONDS
) : Closeable {

    private val logger = getLogger(this)
    private val mutex = Mutex()

    private var client: Client? = null
    private var process: Process? = null
    private var httpClient: HttpClient? = null
    private var isConnected = false

    /**
     * The list of tools available from the connected MCP server.
     */
    var availableTools: List<Tool> = emptyList()
        private set

    /**
     * Connects to the MCP server using the configured transport.
     * This is an idempotent operation - calling it multiple times will only connect once.
     *
     * @throws IllegalStateException if the configuration is invalid.
     * @throws Exception if connection fails.
     */
    suspend fun connect() = mutex.withLock {
        if (isConnected) {
            logger.info("MCP client for server '{}' is already connected", config.serverName)
            return@withLock
        }

        if (!config.enabled) {
            logger.info("MCP server '{}' is disabled, skipping connection", config.serverName)
            return@withLock
        }

        try {
            logger.info("Connecting to MCP server: {}", config.serverName)

            val mcpClient = Client(clientInfo = clientInfo)

            val transport = when (config.transportType) {
                MCPTransportType.STDIO -> createStdioTransport()
                MCPTransportType.SSE -> createSSETransport()
                MCPTransportType.WEBSOCKET -> createWebSocketTransport()
            }

            mcpClient.connect(transport)

            // Request available tools from the server
            val toolsResult = mcpClient.listTools()
            availableTools = toolsResult.tools

            client = mcpClient
            isConnected = true

            logger.info(
                "Successfully connected to MCP server '{}' with {} tools: {}",
                config.serverName,
                availableTools.size,
                availableTools.joinToString(", ") { it.name }
            )
        } catch (e: Exception) {
            logger.error("Failed to connect to MCP server '{}': {}", config.serverName, e.message, e)
            cleanup()
            throw e
        }
    }

    /**
     * Calls a tool on the connected MCP server.
     *
     * @param toolName The name of the tool to call.
     * @param arguments The arguments to pass to the tool.
     * @return The result from the tool execution.
     * @throws IllegalStateException if not connected to the server.
     */
    suspend fun callTool(toolName: String, arguments: Map<String, Any?>): Any? {
        val mcpClient = client ?: throw IllegalStateException(
            "MCP client for server '${config.serverName}' is not connected"
        )

        return try {
            logger.debug("Calling tool '{}' on server '{}' with args: {}", toolName, config.serverName, arguments)
            val result = mcpClient.callTool(name = toolName, arguments = arguments)
            logger.debug("Tool '{}' execution completed successfully", toolName)
            result
        } catch (e: Exception) {
            logger.error("Error calling tool '{}' on server '{}': {}", toolName, config.serverName, e.message, e)
            throw e
        }
    }

    /**
     * Checks if the client is connected to the server.
     *
     * @return true if connected, false otherwise.
     */
    fun isConnected(): Boolean = isConnected

    /**
     * Gets the server name from the configuration.
     *
     * @return The server name.
     */
    fun getServerName(): String = config.serverName

    override fun close() {
        cleanup()
    }

    private fun createStdioTransport(): StdioClientTransport {
        val command = checkNotNull(config.command) { "Command is required for STDIO transport" }

        val processCommand = buildList {
            add(command)
            addAll(config.args)
        }

        logger.debug("Starting MCP server process: {}", processCommand.joinToString(" "))

        val processBuilder = ProcessBuilder(processCommand)
        val serverProcess = processBuilder.start()
        process = serverProcess

        return StdioClientTransport(
            input = serverProcess.inputStream.asSource().buffered(),
            output = serverProcess.outputStream.asSink().buffered()
        )
    }

    private fun createSSETransport(): SseClientTransport {
        val url = checkNotNull(config.url) { "URL is required for SSE transport" }

        val client = HttpClient(CIO) {
            expectSuccess = true
        }
        httpClient = client

        return SseClientTransport(client, url)
    }

    private fun createWebSocketTransport(): WebSocketClientTransport {
        val url = checkNotNull(config.url) { "URL is required for WebSocket transport" }

        val client = HttpClient(CIO) {
            expectSuccess = true
        }
        httpClient = client

        return WebSocketClientTransport(client, url)
    }

    private fun cleanup() {
        isConnected = false

        process?.let {
            try {
                if (it.isAlive) {
                    it.destroy()
                    // Wait for graceful shutdown
                    if (!it.waitFor(processTerminationTimeoutSeconds, TimeUnit.SECONDS)) {
                        it.destroyForcibly()
                    }
                }
                logger.debug("MCP server process terminated")
            } catch (e: Exception) {
                logger.warn("Error terminating MCP server process: {}", e.message)
            }
            process = null
        }

        httpClient?.let {
            try {
                it.close()
                logger.debug("HTTP client closed")
            } catch (e: Exception) {
                logger.warn("Error closing HTTP client: {}", e.message)
            }
            httpClient = null
        }

        client = null
        availableTools = emptyList()
    }

    companion object {
        /**
         * Default timeout in seconds for graceful process termination.
         */
        const val DEFAULT_PROCESS_TERMINATION_TIMEOUT_SECONDS = 5L
    }
}
