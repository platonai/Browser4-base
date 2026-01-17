package ai.platon.pulsar.agentic.tools.executors

import ai.platon.pulsar.agentic.ToolCall
import ai.platon.pulsar.skeleton.crawl.fetch.driver.AbstractWebDriver
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class WebDriverExToolExecutorTest {

    private lateinit var driver: AbstractWebDriver
    private lateinit var executor: WebDriverExToolExecutor

    @BeforeEach
    fun setUp() {
        driver = mockk(relaxed = true)
        executor = WebDriverExToolExecutor()
    }

    @Test
    fun `help returns available methods`() {
        val help = executor.help()
        
        assertNotNull(help)
        assertTrue(help.isNotBlank())
        assertTrue(help.contains("Extract text content"))
    }

    @Test
    fun `help for extract method returns detailed help`() {
        val help = executor.help("extract")
        
        assertNotNull(help)
        assertTrue(help.contains("Extract text content"))
        assertTrue(help.contains("extract"))
    }

    @Test
    fun `help for unknown method returns empty string`() {
        val help = executor.help("unknownMethod")
        
        assertEquals("", help)
    }

    @Test
    fun `extract calls selectTextAll with union selector`() = runBlocking {
        coEvery { driver.selectTextAll(any()) } returns listOf("text1", "text2")
        
        val tc = ToolCall(
            domain = "driverEx",
            method = "extract",
            arguments = mutableMapOf("selectors" to ".class1,.class2,#id1")
        )
        
        executor.execute(tc, driver)
        
        coVerify { driver.selectTextAll(".class1,.class2,#id1") }
    }

    @Test
    fun `extract with comma-separated string selectors`() = runBlocking {
        coEvery { driver.selectTextAll(any()) } returns listOf("text")
        
        val tc = ToolCall(
            domain = "driverEx",
            method = "extract",
            arguments = mutableMapOf("selectors" to ".class1,.class2")
        )
        
        executor.execute(tc, driver)
        
        coVerify { driver.selectTextAll(".class1,.class2") }
    }

    @Test
    fun `unsupported method returns exception`() = runBlocking {
        val tc = ToolCall(
            domain = "driverEx",
            method = "unsupportedMethod",
            arguments = mutableMapOf()
        )
        
        val result = executor.execute(tc, driver)
        
        assertNotNull(result.exception)
        assertTrue(result.exception?.cause?.message?.contains("Unsupported") == true)
    }

    @Test
    fun `domain property is driverEx`() {
        assertEquals("driverEx", executor.domain)
    }
}
