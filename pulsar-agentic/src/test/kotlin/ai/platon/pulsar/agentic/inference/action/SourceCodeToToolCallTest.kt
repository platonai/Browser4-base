package ai.platon.pulsar.agentic.inference.action

import ai.platon.pulsar.agentic.tools.specs.SourceCodeToToolCallSpec
import ai.platon.pulsar.skeleton.common.llm.LLMUtils
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

class SourceCodeToToolCallTest {
    @Test
        @DisplayName("extract methods from WebDriver resource")
    fun extractMethodsFromWebdriverResource() {
        val sourceCode = LLMUtils.readSourceFileFromResource("WebDriver.kt")
        val tools = SourceCodeToToolCallSpec.extractInterface("driver", sourceCode, "WebDriver")
        assertTrue(tools.isNotEmpty(), "Tool list should not be empty")
        val click = tools.firstOrNull { it.domain == "driver" && it.method == "click" }
        assertNotNull(click, "Should contain driver.click method")
        assertTrue(click!!.arguments.map { it.name }.contains("selector"), "click should have selector argument")
    }
}
