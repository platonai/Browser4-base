package ai.platon.pulsar.agentic.tools.executors

import ai.platon.pulsar.agentic.ToolCall
import ai.platon.pulsar.agentic.tools.AgentToolManager
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SystemToolExecutorTest {

    private lateinit var agentToolManager: AgentToolManager
    private lateinit var executor: SystemToolExecutor

    @BeforeEach
    fun setUp() {
        agentToolManager = mockk(relaxed = true)
        executor = SystemToolExecutor(agentToolManager)
    }

    @Test
    fun `help returns available system tools`() {
        val help = executor.help()
        
        assertNotNull(help)
        assertTrue(help.isNotBlank())
        assertTrue(help.contains("System Tools"))
        assertTrue(help.contains("help"))
    }

    @Test
    fun `help for help method returns detailed help`() {
        val help = executor.help("help")
        
        assertNotNull(help)
        assertTrue(help.contains("Get help information"))
        assertTrue(help.contains("help"))
    }

    @Test
    fun `help for unknown method returns not found message`() {
        val help = executor.help("unknownMethod")
        
        assertNotNull(help)
        assertTrue(help.contains("not found"))
    }

    @Test
    fun `help with domain and method delegates to agent tool manager`() = runBlocking {
        every { agentToolManager.help("fs", "writeString") } returns "File system help"
        
        val result = executor.help("fs", "writeString")
        
        assertEquals("File system help", result)
    }

    @Test
    fun `system help method executes correctly`() = runBlocking {
        every { agentToolManager.help("driver", "click") } returns "Click help text"
        
        val tc = ToolCall(
            domain = "system",
            method = "help",
            arguments = mutableMapOf("domain" to "driver", "method" to "click")
        )
        
        val result = executor.execute(tc, executor)
        
        assertEquals("Click help text", result.value)
    }

    @Test
    fun `domain property is system`() {
        assertEquals("system", executor.domain)
    }
}
