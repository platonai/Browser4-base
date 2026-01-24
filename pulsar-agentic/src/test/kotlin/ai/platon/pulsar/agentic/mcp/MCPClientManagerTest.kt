package ai.platon.pulsar.agentic.mcp

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Unit tests for MCPClientManager.
 * 
 * These tests validate MCPClientManager functionality without requiring
 * actual server connections by using disabled configurations.
 */
@Tag("unit")
@Tag("mcp")
class MCPClientManagerTest {

    @Test
    fun createClientManagerWithValidConfig() {
        val config = MCPConfig(
            serverName = "test-client-manager",
            transportType = MCPTransportType.SSE,
            url = "http://localhost:8080/mcp",
            enabled = true
        )

        val manager = MCPClientManager(config)
        kotlin.test.assertNotNull(manager)
        assertEquals("test-client-manager", manager.getServerName())
        assertFalse(manager.isConnected())
    }

    @Test
    fun clientManagerReportsNotConnectedBeforeConnect() {
        val config = MCPConfig(
            serverName = "test-not-connected",
            transportType = MCPTransportType.SSE,
            url = "http://localhost:8080/mcp",
            enabled = true
        )

        val manager = MCPClientManager(config)
        assertFalse(manager.isConnected())
        assertTrue(manager.availableTools.isEmpty())
    }

    @Test
    fun closeClientManagerIsIdempotent() {
        val config = MCPConfig(
            serverName = "test-close-idempotent",
            transportType = MCPTransportType.SSE,
            url = "http://localhost:8080/mcp",
            enabled = true
        )

        val manager = MCPClientManager(config)
        
        // Close multiple times should not throw
        Assertions.assertDoesNotThrow {
            manager.close()
            manager.close()
            manager.close()
        }
    }

    @Test
    fun createClientManagerWithDisabledConfig() = runBlocking {
        val config = MCPConfig(
            serverName = "test-disabled",
            transportType = MCPTransportType.SSE,
            url = "http://localhost:8080/mcp",
            enabled = false
        )

        val manager = MCPClientManager(config)
        
        // Connect should return early for disabled config
        manager.connect()
        
        assertFalse(manager.isConnected())
        assertTrue(manager.availableTools.isEmpty())
    }

    @Test
    fun connectIsIdempotent() = runBlocking {
        val config = MCPConfig(
            serverName = "test-idempotent",
            transportType = MCPTransportType.SSE,
            url = "http://localhost:8080/mcp",
            enabled = false // Disabled to avoid actual connection
        )

        val manager = MCPClientManager(config)
        
        // Multiple connect calls should not throw
        Assertions.assertDoesNotThrow {
            runBlocking {
                manager.connect()
                manager.connect()
                manager.connect()
            }
        }
    }

    @Test
    fun callToolWhenNotConnectedThrowsException() = runBlocking {
        val config = MCPConfig(
            serverName = "test-not-connected-call",
            transportType = MCPTransportType.SSE,
            url = "http://localhost:8080/mcp",
            enabled = true
        )

        val manager = MCPClientManager(config)
        
        val exception = Assertions.assertThrows(IllegalStateException::class.java) {
            runBlocking {
                manager.callTool("echo", mapOf("message" to "test"))
            }
        }
        
        assertTrue(exception.message?.contains("not connected") == true)
    }

    @Test
    fun clientManagerWithSTDIOConfigRequiresCommand() {
        val exception = Assertions.assertThrows(IllegalArgumentException::class.java) {
            MCPConfig(
                serverName = "test-stdio-no-command",
                transportType = MCPTransportType.STDIO,
                command = null
            )
        }
        
        assertTrue(exception.message?.contains("Command", ignoreCase = true) == true)
    }

    @Test
    fun clientManagerWithSSEConfigRequiresUrl() {
        val exception = Assertions.assertThrows(IllegalArgumentException::class.java) {
            MCPConfig(
                serverName = "test-sse-no-url",
                transportType = MCPTransportType.SSE,
                url = null
            )
        }
        
        assertTrue(exception.message?.contains("URL", ignoreCase = true) == true)
    }

    @Test
    fun clientManagerWithWebSocketConfigRequiresUrl() {
        val exception = Assertions.assertThrows(IllegalArgumentException::class.java) {
            MCPConfig(
                serverName = "test-ws-no-url",
                transportType = MCPTransportType.WEBSOCKET,
                url = null
            )
        }
        
        assertTrue(exception.message?.contains("URL", ignoreCase = true) == true)
    }

    @Test
    fun clientManagerHandlesMultipleInstances() {
        val config1 = MCPConfig(
            serverName = "server1",
            transportType = MCPTransportType.STDIO,
            command = "echo",
            enabled = false
        )

        val config2 = MCPConfig(
            serverName = "server2",
            transportType = MCPTransportType.STDIO,
            command = "echo",
            enabled = false
        )

        val manager1 = MCPClientManager(config1)
        val manager2 = MCPClientManager(config2)

        assertNotEquals(manager1.getServerName(), manager2.getServerName())
        assertEquals("server1", manager1.getServerName())
        assertEquals("server2", manager2.getServerName())
    }

    @Test
    fun availableToolsIsEmptyWhenNotConnected() {
        val config = MCPConfig(
            serverName = "test-tools",
            transportType = MCPTransportType.STDIO,
            command = "echo",
            enabled = false
        )

        val manager = MCPClientManager(config)
        assertTrue(manager.availableTools.isEmpty())
    }
}
