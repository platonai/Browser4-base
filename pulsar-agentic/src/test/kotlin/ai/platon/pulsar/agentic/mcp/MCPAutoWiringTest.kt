package ai.platon.pulsar.agentic.mcp

import ai.platon.pulsar.agentic.BasicAgenticSession
import ai.platon.pulsar.agentic.agents.AgentConfig
import ai.platon.pulsar.agentic.agents.BasicBrowserAgent
import ai.platon.pulsar.agentic.context.DefaultClassPathXmlAgenticContext
import ai.platon.pulsar.agentic.tools.CustomToolRegistry
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

/**
 * Contract test: if `mcp.<serverName>` is registered, a PerceptiveAgent should have the
 * corresponding [MCPClientManager] instance registered as the custom target in AgentToolManager.
 *
 * We avoid starting a real MCP server here; we only validate the domain->target wiring.
 */
class MCPAutoWiringTest {

    private val serverName = "auto-wiring-test"
    private val domain = "mcp.$serverName"

    @BeforeEach
    fun setUp() {
        CustomToolRegistry.instance.unregister(domain)
        MCPPluginRegistry.instance.unregisterMCPServer(serverName)
    }

    @AfterEach
    fun tearDown() {
        CustomToolRegistry.instance.unregister(domain)
        MCPPluginRegistry.instance.unregisterMCPServer(serverName)
    }

    @Test
        @DisplayName("BrowserAgentActor auto-wires MCPClientManager as target for mcp domain")
    fun browseragentactorAutoWiresMcpclientmanagerAsTargetForMcpDomain() {
        // Arrange: inject a dummy MCPClientManager into the registry (no connect required)
        val config = MCPConfig(
            serverName = serverName,
            enabled = true,
            transportType = MCPTransportType.SSE,
            url = "http://localhost"
        )
        val cm = MCPClientManager(config)

        val registry = MCPPluginRegistry.instance
        val clientManagersField = registry.javaClass.getDeclaredField("clientManagers").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val clientManagers = clientManagersField.get(registry) as MutableMap<String, MCPClientManager>
        clientManagers[serverName] = cm

        // Need the executor domain registered so AgentToolManager recognizes it as custom.
        CustomToolRegistry.instance.register(MCPToolExecutor(cm))

        // Act: create an actor and force tool manager init
        val context = DefaultClassPathXmlAgenticContext()
        val session = BasicAgenticSession(context, context.configuration.toVolatileConfig())
        val actor = object : BasicBrowserAgent(session, AgentConfig()) {}

        // Force tool manager init by invoking the protected getter via reflection
        val getter = actor.javaClass.superclass.getDeclaredMethod("getToolExecutor").apply { isAccessible = true }
        val tm = getter.invoke(actor)

        // Assert: customTargets contains domain -> cm
        val customTargetsField = tm.javaClass.getDeclaredField("customTargets").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val customTargets = customTargetsField.get(tm) as Map<String, Any>
        assertSame(cm, customTargets[domain])
    }
}
