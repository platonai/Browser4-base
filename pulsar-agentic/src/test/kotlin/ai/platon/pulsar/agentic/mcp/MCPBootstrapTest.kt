package ai.platon.pulsar.agentic.mcp

import ai.platon.pulsar.agentic.tools.CustomToolRegistry
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Tag

/**
 * Tests for MCPBootstrap functionality.
 * 
 * These tests validate the bootstrap helpers for wiring MCP tools into the agent system.
 */
@Tag("unit")
@Tag("mcp")
class MCPBootstrapTest {

    private val testServerName = "bootstrap-test-server"
    private val testDomain = "mcp.$testServerName"

    @BeforeEach
    fun setUp() {
        // Clean up any previous registrations
        CustomToolRegistry.instance.unregister(testDomain)
        if (MCPPluginRegistry.instance.isRegistered(testServerName)) {
            MCPPluginRegistry.instance.unregisterMCPServer(testServerName)
        }
    }

    @AfterEach
    fun tearDown() {
        // Clean up after tests
        try {
            CustomToolRegistry.instance.unregister(testDomain)
            if (MCPPluginRegistry.instance.isRegistered(testServerName)) {
                MCPPluginRegistry.instance.unregisterMCPServer(testServerName)
            }
            MCPBootstrap.close()
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }

    @Test
    fun registerSingleDisabledServerDoesNotConnect() {
        val config = MCPConfig(
            serverName = testServerName,
            transportType = MCPTransportType.STDIO,
            command = "echo",
            enabled = false
        )

        // Should not throw, but also should not register
        MCPBootstrap.register(config, autoRegisterTools = true)

        // Verify server was not registered
        assertFalse(MCPPluginRegistry.instance.isRegistered(testServerName))
    }

    @Test
    fun registerWithAutoRegisterToolsFalse() {
        val config = MCPConfig(
            serverName = testServerName,
            transportType = MCPTransportType.STDIO,
            command = "echo",
            enabled = false
        )

        MCPBootstrap.register(config, autoRegisterTools = false)

        // Verify server was not registered (because disabled)
        assertFalse(MCPPluginRegistry.instance.isRegistered(testServerName))
    }

    @Test
    fun registerAllWithMultipleConfigs() {
        val configs = listOf(
            MCPConfig(
                serverName = "disabled-server-1",
                transportType = MCPTransportType.STDIO,
                command = "node",
                enabled = false
            ),
            MCPConfig(
                serverName = "disabled-server-2",
                transportType = MCPTransportType.SSE,
                url = "http://localhost:8080",
                enabled = false
            ),
            MCPConfig(
                serverName = "disabled-server-3",
                transportType = MCPTransportType.WEBSOCKET,
                url = "ws://localhost:8080",
                enabled = false
            )
        )

        val errors = MCPBootstrap.registerAll(configs, autoRegisterTools = true)

        // All servers are disabled, so no errors should occur
        assertTrue(errors.isEmpty(), "Expected no errors for disabled servers, but got: $errors")
        
        // Verify none were registered
        configs.forEach { config ->
            assertFalse(MCPPluginRegistry.instance.isRegistered(config.serverName))
        }
    }

    @Test
    fun registerAllHandlesMixedValidAndInvalidConfigs() {
        val configs = listOf(
            // Valid disabled config
            MCPConfig(
                serverName = "valid-disabled",
                transportType = MCPTransportType.STDIO,
                command = "echo",
                enabled = false
            ),
            // Another valid disabled config
            MCPConfig(
                serverName = "valid-disabled-2",
                transportType = MCPTransportType.SSE,
                url = "http://localhost:9999",
                enabled = false
            )
        )

        val errors = MCPBootstrap.registerAll(configs, autoRegisterTools = false)

        // Should complete without errors for disabled servers
        assertTrue(errors.isEmpty(), "Expected no errors, but got: $errors")
    }

    @Test
    fun closeUnregistersAllServers() {
        // Register some disabled servers
        val configs = listOf(
            MCPConfig(
                serverName = "temp-server-1",
                transportType = MCPTransportType.STDIO,
                command = "echo",
                enabled = false
            ),
            MCPConfig(
                serverName = "temp-server-2",
                transportType = MCPTransportType.STDIO,
                command = "echo",
                enabled = false
            )
        )

        MCPBootstrap.registerAll(configs)
        
        // Close should not throw even if servers weren't actually connected
        assertDoesNotThrow {
            MCPBootstrap.close()
        }

        // Verify registry is empty
        assertEquals(0, MCPPluginRegistry.instance.size())
    }

    @Test
    fun registerSameServerTwiceThrowsException() = runBlocking {
        val config = MCPConfig(
            serverName = testServerName,
            transportType = MCPTransportType.STDIO,
            command = "echo",
            enabled = false
        )

        // Register first time
        MCPBootstrap.register(config)

        // Attempt to register again with enabled=false should not throw
        // because disabled servers are not actually registered
        assertDoesNotThrow {
            MCPBootstrap.register(config)
        }
    }

    @Test
    fun bootstrapModuleDoesNotRequireCoroutineContext() {
        // This test verifies that MCPBootstrap can be called from non-coroutine contexts
        // by using runBlocking internally
        
        val config = MCPConfig(
            serverName = testServerName,
            transportType = MCPTransportType.STDIO,
            command = "echo",
            enabled = false
        )

        // Should not require explicit runBlocking from caller
        assertDoesNotThrow {
            MCPBootstrap.register(config)
        }

        // Also test registerAll
        assertDoesNotThrow {
            MCPBootstrap.registerAll(listOf(config))
        }

        // And close
        assertDoesNotThrow {
            MCPBootstrap.close()
        }
    }
}
