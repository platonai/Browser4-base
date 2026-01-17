package ai.platon.pulsar.agentic.mcp

/**
 * Configuration for MCP server connection.
 *
 * @property serverName The name of the MCP server.
 * @property transportType The type of transport to use (stdio, sse, websocket).
 * @property command The command to start the server (for stdio transport).
 * @property args Arguments for the server command (for stdio transport).
 * @property url The URL for SSE or WebSocket transport.
 * @property enabled Whether this MCP server connection is enabled.
 */
data class MCPConfig(
    val serverName: String,
    val transportType: MCPTransportType = MCPTransportType.STDIO,
    val command: String? = null,
    val args: List<String> = emptyList(),
    val url: String? = null,
    val enabled: Boolean = true
) {
    init {
        when (transportType) {
            MCPTransportType.STDIO -> {
                require(!command.isNullOrBlank()) { "Command is required for STDIO transport" }
            }
            MCPTransportType.SSE, MCPTransportType.WEBSOCKET -> {
                require(!url.isNullOrBlank()) { "URL is required for ${transportType.name} transport" }
            }
        }
    }
}

/**
 * Supported MCP transport types.
 */
enum class MCPTransportType {
    /**
     * Standard input/output transport for local processes.
     */
    STDIO,
    
    /**
     * Server-Sent Events transport for HTTP streaming.
     */
    SSE,
    
    /**
     * WebSocket transport for bidirectional communication.
     */
    WEBSOCKET
}
