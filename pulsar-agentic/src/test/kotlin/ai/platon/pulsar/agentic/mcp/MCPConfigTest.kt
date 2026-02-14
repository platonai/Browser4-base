package ai.platon.pulsar.agentic.mcp

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.DisplayName

/**
 * Tests for MCPConfig data class.
 */
class MCPConfigTest {

    @Test
        @DisplayName("test STDIO config requires command")
    fun testStdioConfigRequiresCommand() {
        assertThrows<IllegalArgumentException> {
            MCPConfig(
                serverName = "test-server",
                transportType = MCPTransportType.STDIO,
                command = null
            )
        }
    }

    @Test
        @DisplayName("test STDIO config with command succeeds")
    fun testStdioConfigWithCommandSucceeds() {
        val config = MCPConfig(
            serverName = "test-server",
            transportType = MCPTransportType.STDIO,
            command = "node",
            args = listOf("server.js")
        )

        assertEquals("test-server", config.serverName)
        assertEquals(MCPTransportType.STDIO, config.transportType)
        assertEquals("node", config.command)
        assertEquals(listOf("server.js"), config.args)
        assertTrue(config.enabled)
    }

    @Test
        @DisplayName("test SSE config requires URL")
    fun testSseConfigRequiresUrl() {
        assertThrows<IllegalArgumentException> {
            MCPConfig(
                serverName = "test-server",
                transportType = MCPTransportType.SSE,
                url = null
            )
        }
    }

    @Test
        @DisplayName("test SSE config with URL succeeds")
    fun testSseConfigWithUrlSucceeds() {
        val config = MCPConfig(
            serverName = "test-server",
            transportType = MCPTransportType.SSE,
            url = "http://localhost:8080/sse"
        )

        assertEquals("test-server", config.serverName)
        assertEquals(MCPTransportType.SSE, config.transportType)
        assertEquals("http://localhost:8080/sse", config.url)
        assertTrue(config.enabled)
    }

    @Test
        @DisplayName("test WebSocket config requires URL")
    fun testWebsocketConfigRequiresUrl() {
        assertThrows<IllegalArgumentException> {
            MCPConfig(
                serverName = "test-server",
                transportType = MCPTransportType.WEBSOCKET,
                url = null
            )
        }
    }

    @Test
        @DisplayName("test WebSocket config with URL succeeds")
    fun testWebsocketConfigWithUrlSucceeds() {
        val config = MCPConfig(
            serverName = "test-server",
            transportType = MCPTransportType.WEBSOCKET,
            url = "ws://localhost:8080/ws"
        )

        assertEquals("test-server", config.serverName)
        assertEquals(MCPTransportType.WEBSOCKET, config.transportType)
        assertEquals("ws://localhost:8080/ws", config.url)
        assertTrue(config.enabled)
    }

    @Test
        @DisplayName("test config can be disabled")
    fun testConfigCanBeDisabled() {
        val config = MCPConfig(
            serverName = "test-server",
            transportType = MCPTransportType.STDIO,
            command = "node",
            enabled = false
        )

        assertFalse(config.enabled)
    }

    @Test
        @DisplayName("test config with empty command string fails")
    fun testConfigWithEmptyCommandStringFails() {
        assertThrows<IllegalArgumentException> {
            MCPConfig(
                serverName = "test-server",
                transportType = MCPTransportType.STDIO,
                command = ""
            )
        }
    }

    @Test
        @DisplayName("test config with blank command string fails")
    fun testConfigWithBlankCommandStringFails() {
        assertThrows<IllegalArgumentException> {
            MCPConfig(
                serverName = "test-server",
                transportType = MCPTransportType.STDIO,
                command = "   "
            )
        }
    }

    @Test
        @DisplayName("test config with empty URL string fails")
    fun testConfigWithEmptyUrlStringFails() {
        assertThrows<IllegalArgumentException> {
            MCPConfig(
                serverName = "test-server",
                transportType = MCPTransportType.SSE,
                url = ""
            )
        }
    }

    @Test
        @DisplayName("test all transport types are supported")
    fun testAllTransportTypesAreSupported() {
        val stdioConfig = MCPConfig(
            serverName = "stdio-server",
            transportType = MCPTransportType.STDIO,
            command = "node"
        )
        assertEquals(MCPTransportType.STDIO, stdioConfig.transportType)

        val sseConfig = MCPConfig(
            serverName = "sse-server",
            transportType = MCPTransportType.SSE,
            url = "http://localhost:8080/sse"
        )
        assertEquals(MCPTransportType.SSE, sseConfig.transportType)

        val wsConfig = MCPConfig(
            serverName = "ws-server",
            transportType = MCPTransportType.WEBSOCKET,
            url = "ws://localhost:8080/ws"
        )
        assertEquals(MCPTransportType.WEBSOCKET, wsConfig.transportType)
    }
}
