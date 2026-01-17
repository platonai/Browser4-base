package ai.platon.pulsar.agentic.mcp

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Tests for MCPConfig data class.
 */
class MCPConfigTest {

    @Test
    fun `test STDIO config requires command`() {
        assertThrows<IllegalArgumentException> {
            MCPConfig(
                serverName = "test-server",
                transportType = MCPTransportType.STDIO,
                command = null
            )
        }
    }

    @Test
    fun `test STDIO config with command succeeds`() {
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
    fun `test SSE config requires URL`() {
        assertThrows<IllegalArgumentException> {
            MCPConfig(
                serverName = "test-server",
                transportType = MCPTransportType.SSE,
                url = null
            )
        }
    }

    @Test
    fun `test SSE config with URL succeeds`() {
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
    fun `test WebSocket config requires URL`() {
        assertThrows<IllegalArgumentException> {
            MCPConfig(
                serverName = "test-server",
                transportType = MCPTransportType.WEBSOCKET,
                url = null
            )
        }
    }

    @Test
    fun `test WebSocket config with URL succeeds`() {
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
    fun `test config can be disabled`() {
        val config = MCPConfig(
            serverName = "test-server",
            transportType = MCPTransportType.STDIO,
            command = "node",
            enabled = false
        )
        
        assertFalse(config.enabled)
    }

    @Test
    fun `test config with empty command string fails`() {
        assertThrows<IllegalArgumentException> {
            MCPConfig(
                serverName = "test-server",
                transportType = MCPTransportType.STDIO,
                command = ""
            )
        }
    }

    @Test
    fun `test config with blank command string fails`() {
        assertThrows<IllegalArgumentException> {
            MCPConfig(
                serverName = "test-server",
                transportType = MCPTransportType.STDIO,
                command = "   "
            )
        }
    }

    @Test
    fun `test config with empty URL string fails`() {
        assertThrows<IllegalArgumentException> {
            MCPConfig(
                serverName = "test-server",
                transportType = MCPTransportType.SSE,
                url = ""
            )
        }
    }

    @Test
    fun `test all transport types are supported`() {
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
