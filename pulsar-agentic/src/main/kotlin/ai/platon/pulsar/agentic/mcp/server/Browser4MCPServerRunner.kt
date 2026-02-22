package ai.platon.pulsar.agentic.mcp.server

import ai.platon.pulsar.agentic.common.AgentFileSystem
import ai.platon.pulsar.agentic.context.AgenticContexts
import ai.platon.pulsar.common.getLogger
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered

/**
 * Starts the Browser4 MCP server using STDIO transport.
 *
 * STDIO transport is the standard integration method used by Claude Desktop,
 * Cursor, Windsurf, and other MCP-compatible AI clients that launch the server
 * as a local subprocess and communicate via stdin/stdout.
 *
 * ## Usage
 *
 * ### Direct invocation
 * ```bash
 * java -cp browser4-all.jar ai.platon.pulsar.agentic.mcp.server.Browser4MCPServerRunnerKt
 * ```
 *
 * ### Claude Desktop configuration (`claude_desktop_config.json`)
 * ```json
 * {
 *   "mcpServers": {
 *     "browser4": {
 *       "command": "java",
 *       "args": ["-cp", "/path/to/browser4-all.jar",
 *                "ai.platon.pulsar.agentic.mcp.server.Browser4MCPServerRunnerKt"]
 *     }
 *   }
 * }
 * ```
 *
 * ## Behaviour
 * 1. Creates an [AgenticSession] and acquires a [WebDriver] bound to a real Chrome browser.
 * 2. Creates an [AgentFileSystem] for file-system tool support.
 * 3. Wraps everything in a [Browser4MCPServer] to register all tools.
 * 4. Creates a [StdioServerTransport] that reads JSON-RPC messages from stdin
 *    and writes responses to stdout.
 * 5. Blocks until the MCP client closes the connection (i.e. EOF on stdin).
 * 6. Shuts down the Pulsar context and closes the browser.
 */
fun main() {
    val logger = getLogger("Browser4MCPServerRunner")
    logger.info("Starting Browser4 MCP Server (STDIO transport)")

    val session = AgenticContexts.createSession()
    val driver = session.getOrCreateBoundDriver()
    val fileSystem = AgentFileSystem()

    try {
        val mcpServer = Browser4MCPServer(driver = driver, fileSystem = fileSystem)

        val transport = StdioServerTransport(
            inputStream = System.`in`.asSource().buffered(),
            outputStream = System.out.asSink().buffered()
        )

        runBlocking {
            logger.info("Browser4 MCP Server connected — waiting for client requests")
            mcpServer.server.connect(transport)
            logger.info("Browser4 MCP Server STDIO session ended")
        }
    } finally {
        logger.info("Shutting down Browser4 MCP Server")
        AgenticContexts.shutdown()
    }
}
