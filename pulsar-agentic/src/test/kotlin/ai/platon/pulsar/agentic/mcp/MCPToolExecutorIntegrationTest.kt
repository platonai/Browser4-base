package ai.platon.pulsar.agentic.mcp

import ai.platon.pulsar.agentic.model.ToolCall
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Integration tests for MCPToolExecutor.
 * 
 * These tests validate MCPToolExecutor functionality without requiring
 * actual MCP server connections by using disabled configurations.
 */
@Tag("integration")
@Tag("mcp")
class MCPToolExecutorIntegrationTest {

    private val testServerName = "tool-executor-test"
    private val testDomain = "mcp.$testServerName"

    @AfterEach
    fun tearDown() {
        // Clean up any registrations
        if (MCPPluginRegistry.instance.isRegistered(testServerName)) {
            MCPPluginRegistry.instance.unregisterMCPServer(testServerName)
        }
    }

    @Test
    fun createToolExecutor() {
        val config = MCPConfig(
            serverName = testServerName,
            transportType = MCPTransportType.STDIO,
            command = "echo",
            enabled = false
        )

        val clientManager = MCPClientManager(config)
        val executor = MCPToolExecutor(clientManager)

        kotlin.test.assertNotNull(executor)
        assertEquals(testDomain, executor.domain)
        assertEquals(MCPClientManager::class, executor.targetClass)
    }

    @Test
    fun toolExecutorDomainIncludesServerName() {
        val config = MCPConfig(
            serverName = "my-custom-server",
            transportType = MCPTransportType.STDIO,
            command = "echo",
            enabled = false
        )

        val clientManager = MCPClientManager(config)
        val executor = MCPToolExecutor(clientManager)

        assertEquals("mcp.my-custom-server", executor.domain)
    }

    @Test
    fun callFunctionOnWhenNotConnectedReturnsError() = runBlocking {
        val config = MCPConfig(
            serverName = testServerName,
            transportType = MCPTransportType.STDIO,
            command = "echo",
            enabled = false
        )

        val clientManager = MCPClientManager(config)
        val executor = MCPToolExecutor(clientManager)

        val toolCall = ToolCall(
            domain = testDomain,
            method = "test_tool",
            arguments = mutableMapOf("arg" to "value")
        )

        val result = executor.callFunctionOn(toolCall, target = clientManager)

        // Should return evaluation with exception
        kotlin.test.assertNotNull(result.exception)
        val exMessage = result.exception?.cause?.message ?: result.exception?.toString()
        assertTrue(exMessage?.contains("not connected") == true)
    }

    @Test
    fun helpReturnsEmptyForNoTools() {
        val config = MCPConfig(
            serverName = testServerName,
            transportType = MCPTransportType.STDIO,
            command = "echo",
            enabled = false
        )

        val clientManager = MCPClientManager(config)
        val executor = MCPToolExecutor(clientManager)

        val help = executor.help()
        kotlin.test.assertNotNull(help)
    }

    @Test
    fun helpForSpecificMethodWhenToolNotFound() {
        val config = MCPConfig(
            serverName = testServerName,
            transportType = MCPTransportType.STDIO,
            command = "echo",
            enabled = false
        )

        val clientManager = MCPClientManager(config)
        val executor = MCPToolExecutor(clientManager)

        val help = executor.help("non_existent_tool")
        assertTrue(help.contains("not found"))
    }

    @Test
    fun getAvailableToolNamesReturnsEmptyWhenNotConnected() {
        val config = MCPConfig(
            serverName = testServerName,
            transportType = MCPTransportType.STDIO,
            command = "echo",
            enabled = false
        )

        val clientManager = MCPClientManager(config)
        val executor = MCPToolExecutor(clientManager)

        val toolNames = executor.getAvailableToolNames()
        assertTrue(toolNames.isEmpty())
    }

    @Test
    fun toolCallWithEmptyArguments() {
        runBlocking {
            val config = MCPConfig(
                serverName = testServerName,
                transportType = MCPTransportType.STDIO,
                command = "echo",
                enabled = false
            )

            val clientManager = MCPClientManager(config)
            val executor = MCPToolExecutor(clientManager)

            val toolCall = ToolCall(
                domain = testDomain,
                method = "test_tool",
                arguments = mutableMapOf() // Empty arguments
            )

            val result = executor.callFunctionOn(toolCall, target = clientManager)

            // Should still return evaluation (with error due to not connected)
            kotlin.test.assertNotNull(result)
            kotlin.test.assertNotNull(result.exception)
        }
    }

    @Test
    fun toolCallWithNullArguments() {
        runBlocking {
            val config = MCPConfig(
                serverName = testServerName,
                transportType = MCPTransportType.STDIO,
                command = "echo",
                enabled = false
            )

            val clientManager = MCPClientManager(config)
            val executor = MCPToolExecutor(clientManager)

            val toolCall = ToolCall(
                domain = testDomain,
                method = "test_tool",
                arguments = mutableMapOf("key" to null) // Null value
            )

            val result = executor.callFunctionOn(toolCall, target = clientManager)

            kotlin.test.assertNotNull(result)
        }
    }

    @Test
    fun multipleToolExecutorsCanExist() {
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

        val executor1 = MCPToolExecutor(manager1)
        val executor2 = MCPToolExecutor(manager2)

        assertNotEquals(executor1.domain, executor2.domain)
        assertEquals("mcp.server1", executor1.domain)
        assertEquals("mcp.server2", executor2.domain)
    }

    @Test
    fun toolExecutorUsesClientManagerServerName() {
        val serverName = "custom-test-server"
        val config = MCPConfig(
            serverName = serverName,
            transportType = MCPTransportType.STDIO,
            command = "echo",
            enabled = false
        )

        val clientManager = MCPClientManager(config)
        val executor = MCPToolExecutor(clientManager)

        assertTrue(executor.domain.endsWith(serverName))
    }
}
