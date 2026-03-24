package ai.platon.pulsar.agentic.inference

import ai.platon.browser4.driver.chrome.dom.model.BrowserUseState
import ai.platon.pulsar.agentic.agents.AgentConfig
import ai.platon.pulsar.agentic.agents.BasicBrowserAgent
import ai.platon.pulsar.agentic.inference.detail.PageStateTracker
import ai.platon.pulsar.agentic.model.*
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.time.Instant
import java.util.*
import kotlin.io.path.readText

class AgentStateManagerPersistenceTest {

    @Test
    fun addToHistoryAndUpdateAgentStateWriteTaskScopedArtifacts() {
        val agent = mockk<BasicBrowserAgent>(relaxed = true)
        every { agent.uuid } returns UUID.fromString("11111111-1111-1111-1111-111111111111")
        every { agent.startTime } returns Instant.parse("2026-03-11T00:00:00Z")
        every { agent.config } returns AgentConfig()

        val pageStateTracker = mockk<PageStateTracker>(relaxed = true)
        val stateManager = AgentStateManager(agent, pageStateTracker)

        val agentState = AgentState(
            step = 1,
            instruction = "direct command",
            browserUseState = BrowserUseState.DUMMY
        )
        val context = ExecutionContext(
            step = 1,
            instruction = "direct command",
            event = "act",
            agentState = agentState,
            stateHistory = AgentHistory(),
            config = AgentConfig(),
            sessionId = "task-abc"
        )
        stateManager.setActiveContext(context)

        val actionDescription = ActionDescription(
            instruction = "fs.writeString(\"hello.txt\", \"hello\")",
            observeElements = listOf(
                ObserveElement(
                    toolCall = ToolCall("fs", "writeString", mutableMapOf("filename" to "hello.txt", "content" to "hello"))
                )
            ),
            agentState = agentState,
            context = context
        )
        val detail = DetailedActResult(
            actionDescription = actionDescription,
            toolCallResult = ToolCallResult.NO_OP,
            description = "Direct command persisted"
        )

        stateManager.updateAgentState(context, detail)
        stateManager.addToHistory(agentState)
        val runLogDir = stateManager.resolveSessionLogDir(context.sessionId)

        assertTrue(Files.exists(runLogDir.resolve("state.jsonl")))
        assertTrue(Files.exists(runLogDir.resolve("result.jsonl")))
        assertTrue(Files.exists(runLogDir.resolve("history.jsonl")))
        assertTrue(runLogDir.resolve("history.jsonl").readText().contains("direct command"))
    }
}
