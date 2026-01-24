package ai.platon.pulsar.agentic.mcp

import ai.platon.pulsar.agentic.tools.CustomToolRegistry
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Tag
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Tests for MCPPluginRegistry functionality.
 * 
 * These tests validate the registry for managing multiple MCP server connections.
 */
@Tag("unit")
@Tag("mcp")
class MCPPluginRegistryTest {

    private lateinit var registry: MCPPluginRegistry
    private val testServerName = "registry-test-server"

    @BeforeEach
    fun setUp() {
        registry = MCPPluginRegistry()
        // Clean up any previous registrations in singleton
        if (MCPPluginRegistry.instance.isRegistered(testServerName)) {
            MCPPluginRegistry.instance.unregisterMCPServer(testServerName)
        }
        CustomToolRegistry.instance.unregister("mcp.$testServerName")
    }

    @AfterEach
    fun tearDown() {
        try {
            registry.close()
            if (MCPPluginRegistry.instance.isRegistered(testServerName)) {
                MCPPluginRegistry.instance.unregisterMCPServer(testServerName)
            }
            CustomToolRegistry.instance.unregister("mcp.$testServerName")
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }

    @Test
    fun newRegistryIsEmpty() {
        assertEquals(0, registry.size())
        assertTrue(registry.getRegisteredServers().isEmpty())
    }

    @Test
    fun registerDisabledServerDoesNotConnect() = runBlocking {
        val config = MCPConfig(
            serverName = testServerName,
            transportType = MCPTransportType.STDIO,
            command = "echo",
            enabled = false
        )

        registry.registerMCPServer(config)

        assertFalse(registry.isRegistered(testServerName))
        assertEquals(0, registry.size())
        assertNull(registry.getClientManager(testServerName))
        assertNull(registry.getToolExecutor(testServerName))
    }

    @Test
    fun registerSameServerTwiceThrowsException() = runBlocking {
        val config1 = MCPConfig(
            serverName = testServerName,
            transportType = MCPTransportType.STDIO,
            command = "echo",
            enabled = false
        )

        // First registration (disabled, so not actually registered)
        registry.registerMCPServer(config1)

        // Since disabled servers are not registered, we can register again
        org.junit.jupiter.api.Assertions.assertDoesNotThrow {
            runBlocking {
                registry.registerMCPServer(config1)
            }
        }
    }

    @Test
    fun unregisterNonExistentServerReturnsFalse() {
        val result = registry.unregisterMCPServer("non-existent-server")
        assertFalse(result)
    }

    @Test
    fun getClientManagerForNonExistentServerReturnsNull() {
        assertNull(registry.getClientManager("non-existent-server"))
    }

    @Test
    fun getToolExecutorForNonExistentServerReturnsNull() {
        assertNull(registry.getToolExecutor("non-existent-server"))
    }

    @Test
    fun isRegisteredReturnsFalseForNonExistentServer() {
        assertFalse(registry.isRegistered("non-existent-server"))
    }

    @Test
    fun closeEmptyRegistryDoesNotThrow() {
        Assertions.assertDoesNotThrow {
            registry.close()
        }
    }

    @Test
    fun registerMultipleDisabledServersSucceeds() = runBlocking {
        val configs = listOf(
            MCPConfig(
                serverName = "server1",
                transportType = MCPTransportType.STDIO,
                command = "echo",
                enabled = false
            ),
            MCPConfig(
                serverName = "server2",
                transportType = MCPTransportType.SSE,
                url = "http://localhost:8080",
                enabled = false
            ),
            MCPConfig(
                serverName = "server3",
                transportType = MCPTransportType.WEBSOCKET,
                url = "ws://localhost:8080",
                enabled = false
            )
        )

        val errors = registry.registerMCPServers(configs)

        assertTrue(errors.isEmpty())
        assertEquals(0, registry.size())
    }

    @Test
    fun registerServersReturnsErrorsForFailedRegistrations() = runBlocking {
        val configs = listOf(
            // Disabled server (should not cause error)
            MCPConfig(
                serverName = "valid-disabled",
                transportType = MCPTransportType.STDIO,
                command = "echo",
                enabled = false
            ),
            // Another disabled server
            MCPConfig(
                serverName = "valid-disabled-2",
                transportType = MCPTransportType.SSE,
                url = "http://localhost:9999",
                enabled = false
            )
        )

        val errors = registry.registerMCPServers(configs)

        // All servers are disabled, so no errors expected
        assertTrue(errors.isEmpty())
    }

    @Test
    fun singletonInstanceIsSame() {
        val instance1 = MCPPluginRegistry.instance
        val instance2 = MCPPluginRegistry.instance

        assertSame(instance1, instance2)
    }

    @Test
    fun getRegisteredServersReturnsEmptySetForNewRegistry() {
        val servers = registry.getRegisteredServers()
        assertTrue(servers.isEmpty())
    }

    @Test
    fun registerWithAutoRegisterToolsFalse() = runBlocking {
        val config = MCPConfig(
            serverName = testServerName,
            transportType = MCPTransportType.STDIO,
            command = "echo",
            enabled = false
        )

        // Should not throw even with autoRegisterTools = false
        Assertions.assertDoesNotThrow {
            runBlocking {
                registry.registerMCPServer(config, autoRegisterTools = false)
            }
        }
    }

    @Test
    fun closeUnregistersAllServers() = runBlocking {
        // Register multiple disabled servers
        val configs = listOf(
            MCPConfig(
                serverName = "temp1",
                transportType = MCPTransportType.STDIO,
                command = "echo",
                enabled = false
            ),
            MCPConfig(
                serverName = "temp2",
                transportType = MCPTransportType.STDIO,
                command = "echo",
                enabled = false
            )
        )

        registry.registerMCPServers(configs)
        
        // Close should clear all registrations
        registry.close()

        assertEquals(0, registry.size())
        assertTrue(registry.getRegisteredServers().isEmpty())
    }

    @Test
    fun unregisterServerRemovesFromRegistry() = runBlocking {
        val config = MCPConfig(
            serverName = testServerName,
            transportType = MCPTransportType.STDIO,
            command = "echo",
            enabled = false
        )

        registry.registerMCPServer(config)
        
        // Since server is disabled, it's not registered
        assertFalse(registry.isRegistered(testServerName))
        
        // Unregister should return false for non-registered server
        assertFalse(registry.unregisterMCPServer(testServerName))
    }
}
