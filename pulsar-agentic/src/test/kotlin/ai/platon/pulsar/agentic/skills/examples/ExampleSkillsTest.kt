package ai.platon.pulsar.agentic.skills.examples

import ai.platon.pulsar.agentic.skills.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

/**
 * Tests for example skills.
 */
class ExampleSkillsTest {

    private lateinit var registry: SkillRegistry
    private lateinit var context: SkillContext

    @BeforeEach
    fun setup() = runBlocking {
        registry = SkillRegistry.instance
        context = SkillContext(sessionId = "test-session-123")
        registry.clear(context)
    }

    @AfterEach
    fun cleanup() = runBlocking {
        registry.clear(context)
    }

    @Test
    fun `test WebScrapingSkill registration`() = runBlocking {
        val skill = WebScrapingSkill()

        registry.register(skill, context)

        assertTrue(registry.contains("web-scraping"))
        assertEquals("Web Scraping", skill.metadata.name)
        assertEquals("1.0.0", skill.metadata.version)
        assertTrue(skill.metadata.tags.contains("scraping"))
    }

    @Test
    fun `test WebScrapingSkill execution with valid parameters`() = runBlocking {
        val skill = WebScrapingSkill()
        registry.register(skill, context)

        val params = mapOf(
            "url" to "https://example.com",
            "selector" to ".content"
        )

        val result = registry.execute("web-scraping", context, params)

        assertTrue(result.success)
        Assertions.assertNotNull(result.data)
        assertTrue(result.message!!.contains("Successfully extracted"))
    }

    @Test
    fun `test WebScrapingSkill execution with missing url parameter`() = runBlocking {
        val skill = WebScrapingSkill()
        registry.register(skill, context)

        val params = mapOf("selector" to ".content")

        val result = registry.execute("web-scraping", context, params)

        assertFalse(result.success)
        assertTrue(result.message!!.contains("Missing required parameter: url"))
    }

    @Test
    fun `test WebScrapingSkill execution with missing selector parameter`() = runBlocking {
        val skill = WebScrapingSkill()
        registry.register(skill, context)

        val params = mapOf("url" to "https://example.com")

        val result = registry.execute("web-scraping", context, params)

        assertFalse(result.success)
        assertTrue(result.message!!.contains("Missing required parameter: selector"))
    }

    @Test
    fun `test WebScrapingSkill rejects invalid URL in onBeforeExecute`() = runBlocking {
        val skill = WebScrapingSkill()
        registry.register(skill, context)

        val params = mapOf(
            "url" to "invalid-url",
            "selector" to ".content"
        )

        val result = registry.execute("web-scraping", context, params)

        assertFalse(result.success)
        assertTrue(result.message!!.contains("cancelled"))
    }

    @Test
    fun `test WebScrapingSkill with custom attributes`() = runBlocking {
        val skill = WebScrapingSkill()
        registry.register(skill, context)

        val params = mapOf(
            "url" to "https://example.com",
            "selector" to ".content",
            "attributes" to listOf("text", "href", "title")
        )

        val result = registry.execute("web-scraping", context, params)

        assertTrue(result.success)
        @Suppress("UNCHECKED_CAST")
        val data = result.data as Map<String, Any>
        assertEquals(listOf("text", "href", "title"), data["attributes"])
    }

    @Test
    fun `test WebScrapingSkill sets shared resource on success`() = runBlocking {
        val skill = WebScrapingSkill()
        registry.register(skill, context)

        val params = mapOf(
            "url" to "https://example.com",
            "selector" to ".content"
        )

        registry.execute("web-scraping", context, params)

        Assertions.assertNotNull(context.getResource<Long>("last_scraping_success"))
    }

    @Test
    fun `test FormFillingSkill registration`() = runBlocking {
        val webScraping = WebScrapingSkill()
        val formFilling = FormFillingSkill()

        registry.register(webScraping, context)
        registry.register(formFilling, context)

        assertTrue(registry.contains("form-filling"))
        assertEquals("Form Filling", formFilling.metadata.name)
        assertTrue(formFilling.metadata.dependencies.contains("web-scraping"))
    }

    @Test
    fun `test FormFillingSkill cannot register without dependency`() = runBlocking {
        val skill = FormFillingSkill()

        val exception = assertThrows<IllegalStateException> {
            runBlocking { registry.register(skill, context) }
        }
        assertTrue(exception.message!!.contains("missing dependencies"))
    }

    @Test
    fun `test FormFillingSkill execution with valid parameters`() = runBlocking {
        registry.register(WebScrapingSkill(), context)
        registry.register(FormFillingSkill(), context)

        val params = mapOf(
            "url" to "https://example.com/form",
            "formData" to mapOf(
                "name" to "John Doe",
                "email" to "john@example.com"
            )
        )

        val result = registry.execute("form-filling", context, params)

        assertTrue(result.success)
        assertTrue(result.message!!.contains("Form filled successfully"))

        @Suppress("UNCHECKED_CAST")
        val data = result.data as Map<String, Any>
        @Suppress("UNCHECKED_CAST")
        val filledFields = data["filledFields"] as List<String>
        assertEquals(2, filledFields.size)
    }

    @Test
    fun `test FormFillingSkill execution with submit flag`() = runBlocking {
        registry.register(WebScrapingSkill(), context)
        registry.register(FormFillingSkill(), context)

        val params = mapOf(
            "url" to "https://example.com/form",
            "formData" to mapOf("name" to "John"),
            "submit" to true
        )

        val result = registry.execute("form-filling", context, params)

        assertTrue(result.success)

        @Suppress("UNCHECKED_CAST")
        val data = result.data as Map<String, Any>
        assertEquals(true, data["submitted"])
    }

    @Test
    fun `test FormFillingSkill rejects empty form data`() = runBlocking {
        registry.register(WebScrapingSkill(), context)
        registry.register(FormFillingSkill(), context)

        val params = mapOf(
            "url" to "https://example.com/form",
            "formData" to emptyMap<String, String>()
        )

        val result = registry.execute("form-filling", context, params)

        assertFalse(result.success)
        assertTrue(result.message!!.contains("cancelled"))
    }

    @Test
    fun `test DataValidationSkill registration`() = runBlocking {
        val skill = DataValidationSkill()

        registry.register(skill, context)

        assertTrue(registry.contains("data-validation"))
        assertEquals("Data Validation", skill.metadata.name)
        assertTrue(skill.metadata.tags.contains("validation"))
    }

    @Test
    fun `test DataValidationSkill validates email successfully`() = runBlocking {
        val skill = DataValidationSkill()
        registry.register(skill, context)

        val params = mapOf(
            "data" to mapOf("email" to "test@example.com"),
            "rules" to listOf("email")
        )

        val result = registry.execute("data-validation", context, params)

        assertTrue(result.success)
        assertTrue(result.message!!.contains("All validation rules passed"))
    }

    @Test
    fun `test DataValidationSkill rejects invalid email`() = runBlocking {
        val skill = DataValidationSkill()
        registry.register(skill, context)

        val params = mapOf(
            "data" to mapOf("email" to "invalid-email"),
            "rules" to listOf("email")
        )

        val result = registry.execute("data-validation", context, params)

        assertFalse(result.success)
        assertTrue(result.message!!.contains("Invalid email format"))
    }

    @Test
    fun `test DataValidationSkill validates required fields`() = runBlocking {
        val skill = DataValidationSkill()
        registry.register(skill, context)

        val params = mapOf(
            "data" to mapOf("field1" to "value1", "field2" to "value2"),
            "rules" to listOf("required")
        )

        val result = registry.execute("data-validation", context, params)

        assertTrue(result.success)
    }

    @Test
    fun `test DataValidationSkill rejects empty required fields`() = runBlocking {
        val skill = DataValidationSkill()
        registry.register(skill, context)

        val params = mapOf(
            "data" to mapOf("field1" to "", "field2" to "value2"),
            "rules" to listOf("required")
        )

        val result = registry.execute("data-validation", context, params)

        assertFalse(result.success)
        assertTrue(result.message!!.contains("Required fields are missing"))
    }

    @Test
    fun `test DataValidationSkill handles unknown rules`() = runBlocking {
        val skill = DataValidationSkill()
        registry.register(skill, context)

        val params = mapOf(
            "data" to mapOf("field" to "value"),
            "rules" to listOf("unknown-rule")
        )

        val result = registry.execute("data-validation", context, params)

        assertFalse(result.success)
        assertTrue(result.message!!.contains("Unknown validation rule"))
    }

    @Test
    fun `test DataValidationSkill with multiple rules`() = runBlocking {
        val skill = DataValidationSkill()
        registry.register(skill, context)

        val params = mapOf(
            "data" to mapOf(
                "email" to "test@example.com",
                "name" to "John"
            ),
            "rules" to listOf("email", "required")
        )

        val result = registry.execute("data-validation", context, params)

        assertTrue(result.success)
    }

    @Test
    fun `test skill tool call specifications`() {
        val webScraping = WebScrapingSkill()
        val formFilling = FormFillingSkill()
        val dataValidation = DataValidationSkill()

        assertTrue(webScraping.toolSpec.isNotEmpty())
        assertEquals("skill.scraping", webScraping.toolSpec[0].domain)
        assertEquals("extract", webScraping.toolSpec[0].method)

        assertTrue(formFilling.toolSpec.isNotEmpty())
        assertEquals("skill.form", formFilling.toolSpec[0].domain)
        assertEquals("fill", formFilling.toolSpec[0].method)

        assertTrue(dataValidation.toolSpec.isEmpty())
    }
}
