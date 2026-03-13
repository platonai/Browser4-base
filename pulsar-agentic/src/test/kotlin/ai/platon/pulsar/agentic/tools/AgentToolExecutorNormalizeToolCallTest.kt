package ai.platon.pulsar.agentic.tools

import ai.platon.pulsar.agentic.AgenticSession
import ai.platon.pulsar.agentic.agents.BasicBrowserAgent
import ai.platon.pulsar.agentic.agents.AgentConfig
import ai.platon.pulsar.agentic.model.ToolCall
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.file.Files

class AgentToolExecutorNormalizeToolCallTest {

    private val session = mockk<AgenticSession>(relaxed = true)
    private val agent = mockk<BasicBrowserAgent>(relaxed = true)

    @Test
    fun normalizeToolCallMapsDriverPositionalArgsToNamedArgs() {
        val executor = AgentToolExecutor(Files.createTempDirectory("agent-tool-normalize"), agent)
        val toolCall = ToolCall("tab", "fill", mutableMapOf("0" to "#search", "1" to "Browser4"))

        val normalized = executor.normalizeToolCall(toolCall)

        assertEquals("tab", normalized.domain)
        assertEquals("#search", normalized.arguments["selector"])
        assertEquals("Browser4", normalized.arguments["text"])
    }

    @Test
    fun normalizeToolCallNormalizesBrowserAliasAndTabArgument() {
        val executor = AgentToolExecutor(Files.createTempDirectory("agent-tool-normalize"), agent)
        val toolCall = ToolCall("browser", "switchTab", mutableMapOf("0" to "tab-2"))

        val normalized = executor.normalizeToolCall(toolCall)

        assertEquals("browser", normalized.domain)
        assertEquals("tab-2", normalized.arguments["tabId"])
    }
}
