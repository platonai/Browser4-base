package ai.platon.pulsar.agentic.tools

import ai.platon.pulsar.agentic.model.ToolSpec
import ai.platon.pulsar.agentic.skills.*
import ai.platon.pulsar.agentic.skills.tools.SkillToolExecutor
import ai.platon.pulsar.agentic.tools.builtin.AbstractToolExecutor
import ai.platon.pulsar.agentic.tools.specs.ToolCallSpecificationRenderer
import ai.platon.pulsar.agentic.tools.specs.ToolSpecFormat
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass
import org.junit.jupiter.api.DisplayName

class ToolCallSpecificationRendererTest {

    private lateinit var registry: CustomToolRegistry
    private lateinit var skillRegistry: SkillRegistry
    private lateinit var skillContext: SkillContext

    @BeforeEach
    fun setUp() = runBlocking {
        registry = CustomToolRegistry.instance
        registry.clear()

        skillRegistry = SkillRegistry.instance
        skillContext = SkillContext(sessionId = "test-session")
        skillRegistry.clear(skillContext)
    }

    @AfterEach
    fun tearDown() = runBlocking {
        registry.clear()
        skillRegistry.clear(skillContext)
    }

    @Test
        @DisplayName("render should keep ToolSpecification verbatim and append custom tools")
    fun renderShouldKeepToolspecificationVerbatimAndAppendCustomTools() {
        val executor = DbToolExecutor()
        val specs = listOf(
            ToolSpec(
                domain = "db",
                method = "query",
                arguments = listOf(ToolSpec.Arg("sql", "String")),
                returnType = "String",
                description = "Run a SQL query"
            )
        )

        registry.register(executor, specs)

        val rendered = ToolCallSpecificationRenderer.render(includeCustomDomains = true)

        // Built-in specification should be present verbatim
        assertTrue(rendered.contains("// domain: driver"), rendered)
        assertTrue(rendered.contains("driver.reload()"), rendered)

        // Custom section appended
        assertTrue(rendered.contains("// CustomTool"), rendered)
        assertTrue(rendered.contains("db.query("), rendered)
        assertTrue(rendered.contains("sql:"), rendered)
    }

    @Test
        @DisplayName("render should include MCP service tools when registered")
    fun renderShouldIncludeMcpServiceToolsWhenRegistered() {
        // Simulate MCP tool registration
        val mcpExecutor = MockMCPToolExecutor()
        val mcpSpecs = listOf(
            ToolSpec(
                domain = "mcp.test-server",
                method = "fetch",
                arguments = listOf(
                    ToolSpec.Arg("url", "String"),
                    ToolSpec.Arg("headers", "Map<String, String>", "emptyMap()")
                ),
                returnType = "String",
                description = "Fetch content from a URL"
            ),
            ToolSpec(
                domain = "mcp.test-server",
                method = "store",
                arguments = listOf(
                    ToolSpec.Arg("key", "String"),
                    ToolSpec.Arg("value", "String")
                ),
                returnType = "Boolean",
                description = "Store a key-value pair"
            )
        )

        registry.register(mcpExecutor, mcpSpecs)

        val rendered = ToolCallSpecificationRenderer.render(includeCustomDomains = true)

        // Built-in tools should be present
        assertTrue(rendered.contains("// domain: driver"), "Should contain built-in driver domain")

        // MCP custom tools should be present
        assertTrue(rendered.contains("// CustomTool"), "Should contain CustomTool section")
        assertTrue(rendered.contains("mcp.test-server.fetch("), "Should contain MCP fetch method")
        assertTrue(rendered.contains("url: String"), "Should contain MCP fetch url parameter")
        assertTrue(rendered.contains("mcp.test-server.store("), "Should contain MCP store method")
        assertTrue(rendered.contains("key: String"), "Should contain MCP store key parameter")
    }

    @Test
        @DisplayName("render should include SKILL tools when registered")
    fun renderShouldIncludeSkillToolsWhenRegistered() = runBlocking {
        // Register a skill with toolSpec
        val skill = TestSkillWithToolSpec()
        skillRegistry.register(skill, skillContext)

        // Register SkillToolExecutor which exposes skill tools
        val skillExecutor = SkillToolExecutor(skillRegistry)
        registry.register(skillExecutor)

        val rendered = ToolCallSpecificationRenderer.render(includeCustomDomains = true)

        // Built-in tools should be present
        assertTrue(rendered.contains("// domain: driver"), "Should contain built-in driver domain")

        // Skill tools should be present
        assertTrue(rendered.contains("// CustomTool"), "Should contain CustomTool section")
        assertTrue(rendered.contains("skill.run("), "Should contain skill.run method")
        assertTrue(rendered.contains("id: String"), "Should contain skill.run id parameter")

        // Skill-specific toolSpec should be present
        assertTrue(rendered.contains("skill.test."), "Should contain skill-specific domain")
        assertTrue(rendered.contains("process("), "Should contain skill-specific method")
    }

    @Test
        @DisplayName("render should include both MCP and SKILL tools when both are registered")
    fun renderShouldIncludeBothMcpAndSkillToolsWhenBothAreRegistered() = runBlocking {
        // Register MCP tools
        val mcpExecutor = MockMCPToolExecutor()
        val mcpSpecs = listOf(
            ToolSpec(
                domain = "mcp.api-server",
                method = "call",
                arguments = listOf(ToolSpec.Arg("endpoint", "String")),
                returnType = "Any?",
                description = "Call an API endpoint"
            )
        )
        registry.register(mcpExecutor, mcpSpecs)

        // Register SKILL
        val skill = TestSkillWithToolSpec()
        skillRegistry.register(skill, skillContext)
        val skillExecutor = SkillToolExecutor(skillRegistry)
        registry.register(skillExecutor)

        val rendered = ToolCallSpecificationRenderer.render(includeCustomDomains = true)

        // Built-in tools should be present
        assertTrue(rendered.contains("// domain: driver"), "Should contain built-in driver domain")

        // Both MCP and SKILL tools should be present
        assertTrue(rendered.contains("// CustomTool"), "Should contain CustomTool section")

        // MCP tools
        assertTrue(rendered.contains("mcp.api-server.call("), "Should contain MCP call method")
        assertTrue(rendered.contains("endpoint: String"), "Should contain MCP endpoint parameter")

        // Skill tools
        assertTrue(rendered.contains("skill.run("), "Should contain skill.run method")
        assertTrue(rendered.contains("skill.test.process("), "Should contain skill-specific method")
    }

    @Test
        @DisplayName("render should properly format tool specifications with various parameter types")
    fun renderShouldProperlyFormatToolSpecificationsWithVariousParameterTypes() {
        val executor = ComplexToolExecutor()
        val specs = listOf(
            ToolSpec(
                domain = "complex",
                method = "process",
                arguments = listOf(
                    ToolSpec.Arg("requiredParam", "String"),
                    ToolSpec.Arg("optionalParam", "Int", "42"),
                    ToolSpec.Arg("listParam", "List<String>", "emptyList()"),
                    ToolSpec.Arg("mapParam", "Map<String, Any?>", "null")
                ),
                returnType = "Result",
                description = "Process complex data"
            )
        )

        registry.register(executor, specs)

        val rendered = ToolCallSpecificationRenderer.render(includeCustomDomains = true)

        // Verify the rendered format
        assertTrue(rendered.contains("complex.process("), "Should contain method signature")
        assertTrue(rendered.contains("requiredParam: String"), "Should contain required parameter")
        assertTrue(rendered.contains("optionalParam: Int = 42"), "Should contain optional parameter with default")
        assertTrue(rendered.contains("listParam: List<String> = emptyList()"), "Should contain list parameter")
        assertTrue(rendered.contains("mapParam: Map<String, Any?> = null"), "Should contain map parameter")
        assertTrue(rendered.contains("): Result"), "Should contain return type")
    }

    @Test
        @DisplayName("render without custom domains should only include built-in tools")
    fun renderWithoutCustomDomainsShouldOnlyIncludeBuiltInTools() {
        // Register some custom tools
        val executor = DbToolExecutor()
        val specs = listOf(
            ToolSpec(
                domain = "db",
                method = "query",
                arguments = listOf(ToolSpec.Arg("sql", "String")),
                returnType = "String"
            )
        )
        registry.register(executor, specs)

        val rendered = ToolCallSpecificationRenderer.render(includeCustomDomains = false)

        // Built-in tools should be present
        assertTrue(rendered.contains("// domain: driver"), "Should contain built-in driver domain")

        // Custom tools should NOT be present
        assertFalse(rendered.contains("// CustomTool"), "Should NOT contain CustomTool section")
        assertFalse(rendered.contains("db.query"), "Should NOT contain custom db.query method")
    }

    @Test
        @DisplayName("render should filter custom domains based on filter function")
    fun renderShouldFilterCustomDomainsBasedOnFilterFunction() {
        // Register multiple custom tools
        registry.register(DbToolExecutor(), listOf(
            ToolSpec(domain = "db", method = "query", arguments = emptyList(), returnType = "String")
        ))
        registry.register(MockMCPToolExecutor(), listOf(
            ToolSpec(domain = "mcp.test", method = "fetch", arguments = emptyList(), returnType = "String")
        ))

        // Filter to include only MCP domains
        val rendered = ToolCallSpecificationRenderer.render(
            includeCustomDomains = true,
            customDomainFilter = { it.startsWith("mcp.") }
        )

        // Built-in tools should be present
        assertTrue(rendered.contains("// domain: driver"), "Should contain built-in driver domain")

        // Only MCP tools should be in custom section
        assertTrue(rendered.contains("// CustomTool"), "Should contain CustomTool section")
        assertTrue(rendered.contains("mcp.test.fetch"), "Should contain MCP fetch method")
        assertFalse(rendered.contains("db.query"), "Should NOT contain filtered db.query method")
    }

    // ==================== JSON Format Tests ====================

    @Test
    fun testRenderJsonShouldProduceValidJsonStructure() {
        val rendered = ToolCallSpecificationRenderer.renderJson(includeCustomDomains = false)

        // Should be valid JSON structure
        assertTrue(rendered.contains(""""tools":"""), "Should contain tools array")
        assertTrue(rendered.contains(""""domain":"""), "Should contain domain field")
        assertTrue(rendered.contains(""""method":"""), "Should contain method field")
        assertTrue(rendered.contains(""""parameters":"""), "Should contain parameters array")
        assertTrue(rendered.contains(""""returns":"""), "Should contain returns field")
    }

    @Test
    fun testRenderJsonShouldIncludeBuiltInTools() {
        val rendered = ToolCallSpecificationRenderer.renderJson(includeCustomDomains = false)

        // Should include built-in driver tools
        assertTrue(rendered.contains(""""domain": "driver""""), "Should contain driver domain")
        assertTrue(rendered.contains(""""method": "navigateTo""""), "Should contain navigateTo method")
        assertTrue(rendered.contains(""""method": "click""""), "Should contain click method")

        // Should include built-in browser tools
        assertTrue(rendered.contains(""""domain": "browser""""), "Should contain browser domain")
        assertTrue(rendered.contains(""""method": "switchTab""""), "Should contain switchTab method")

        // Should include built-in fs tools
        assertTrue(rendered.contains(""""domain": "fs""""), "Should contain fs domain")
        assertTrue(rendered.contains(""""method": "writeString""""), "Should contain writeString method")
    }

    @Test
    fun testRenderJsonShouldIncludeParametersWithTypes() {
        val rendered = ToolCallSpecificationRenderer.renderJson(includeCustomDomains = false)

        // Should include parameter details
        assertTrue(rendered.contains(""""name": "url""""), "Should contain url parameter")
        assertTrue(rendered.contains(""""type": "String""""), "Should contain String type")
    }

    @Test
    fun testRenderJsonShouldIncludeParametersWithDefaults() {
        val rendered = ToolCallSpecificationRenderer.renderJson(includeCustomDomains = false)

        // Should include default values for parameters (e.g., waitForSelector has timeout default)
        assertTrue(rendered.contains(""""default":"""), "Should contain at least one default value")
    }

    @Test
    fun testRenderJsonShouldIncludeCustomTools() {
        val executor = DbToolExecutor()
        val specs = listOf(
            ToolSpec(
                domain = "db",
                method = "query",
                arguments = listOf(ToolSpec.Arg("sql", "String")),
                returnType = "String",
                description = "Run a SQL query"
            )
        )
        registry.register(executor, specs)

        val rendered = ToolCallSpecificationRenderer.renderJson(includeCustomDomains = true)

        // Should include custom db tool
        assertTrue(rendered.contains(""""domain": "db""""), "Should contain db domain")
        assertTrue(rendered.contains(""""method": "query""""), "Should contain query method")
        assertTrue(rendered.contains(""""description": "Run a SQL query""""), "Should contain description")
    }

    @Test
    fun testRenderWithFormatKotlinShouldMatchOriginalRender() {
        val kotlinFormat = ToolCallSpecificationRenderer.render(
            format = ToolSpecFormat.KOTLIN,
            includeCustomDomains = false
        )
        val originalRender = ToolCallSpecificationRenderer.render(includeCustomDomains = false)

        assertEquals(originalRender, kotlinFormat, "KOTLIN format should match original render")
    }

    @Test
    fun testRenderWithFormatJsonShouldMatchRenderJson() {
        val jsonFormat = ToolCallSpecificationRenderer.render(
            format = ToolSpecFormat.JSON,
            includeCustomDomains = false
        )
        val jsonRender = ToolCallSpecificationRenderer.renderJson(includeCustomDomains = false)

        assertEquals(jsonRender, jsonFormat, "JSON format should match renderJson")
    }

    @Test
    fun testParseBuiltInSpecificationsShouldParseAllTools() {
        val specs = ToolCallSpecificationRenderer.parseBuiltInSpecifications()

        // Should parse multiple tools
        assertTrue(specs.isNotEmpty(), "Should parse built-in specifications")

        // Should include driver domain tools
        val driverTools = specs.filter { it.domain == "driver" }
        assertTrue(driverTools.isNotEmpty(), "Should include driver tools")

        // Should include browser domain tools
        val browserTools = specs.filter { it.domain == "browser" }
        assertTrue(browserTools.isNotEmpty(), "Should include browser tools")

        // Should include fs domain tools
        val fsTools = specs.filter { it.domain == "fs" }
        assertTrue(fsTools.isNotEmpty(), "Should include fs tools")
    }

    @Test
    fun testParseBuiltInSpecificationsShouldParseArgumentsCorrectly() {
        val specs = ToolCallSpecificationRenderer.parseBuiltInSpecifications()

        // Find navigateTo method which has a url parameter
        val navigateTo = specs.find { it.domain == "driver" && it.method == "navigateTo" }
        assertNotNull(navigateTo, "Should find navigateTo method")
        assertEquals(1, navigateTo!!.arguments.size, "navigateTo should have 1 argument")
        assertEquals("url", navigateTo.arguments[0].name, "First argument should be url")
        assertEquals("String", navigateTo.arguments[0].type, "url should be String type")
    }

    @Test
    fun testParseBuiltInSpecificationsShouldParseDefaultValues() {
        val specs = ToolCallSpecificationRenderer.parseBuiltInSpecifications()

        // Find waitForSelector which has a default timeout
        val waitForSelector = specs.find { it.domain == "driver" && it.method == "waitForSelector" }
        assertNotNull(waitForSelector, "Should find waitForSelector method")

        val timeoutArg = waitForSelector!!.arguments.find { it.name == "timeoutMillis" }
        assertNotNull(timeoutArg, "Should have timeoutMillis argument")
        assertEquals("3000", timeoutArg!!.defaultValue, "timeoutMillis should have default 3000")
    }

    @Test
    fun testParseBuiltInSpecificationsShouldParseReturnTypes() {
        val specs = ToolCallSpecificationRenderer.parseBuiltInSpecifications()

        // Find exists method which returns Boolean
        val exists = specs.find { it.domain == "driver" && it.method == "exists" }
        assertNotNull(exists, "Should find exists method")
        assertEquals("Boolean", exists!!.returnType, "exists should return Boolean")

        // Find navigateTo which returns Unit (no return type specified)
        val navigateTo = specs.find { it.domain == "driver" && it.method == "navigateTo" }
        assertNotNull(navigateTo, "Should find navigateTo method")
        assertEquals("Unit", navigateTo!!.returnType, "navigateTo should return Unit")
    }

    @Test
    fun testRenderAsJsonShouldFormatSpecsCorrectly() {
        val specs = listOf(
            ToolSpec(
                domain = "test",
                method = "doSomething",
                arguments = listOf(
                    ToolSpec.Arg("param1", "String"),
                    ToolSpec.Arg("param2", "Int", "42")
                ),
                returnType = "Result",
                description = "Test method"
            )
        )

        val json = ToolCallSpecificationRenderer.renderAsJson(specs)

        assertTrue(json.contains(""""domain": "test""""), "Should contain domain")
        assertTrue(json.contains(""""method": "doSomething""""), "Should contain method")
        assertTrue(json.contains(""""name": "param1""""), "Should contain param1")
        assertTrue(json.contains(""""type": "Int""""), "Should contain Int type")
        assertTrue(json.contains(""""default": "42""""), "Should contain default value")
        assertTrue(json.contains(""""returns": "Result""""), "Should contain return type")
        assertTrue(json.contains(""""description": "Test method""""), "Should contain description")
    }

    // ==================== Helper test classes ====================

    private class DbToolExecutor : AbstractToolExecutor() {
        override val domain: String = "db"
        override val targetClass: KClass<*> = Any::class

        override suspend fun callFunctionOn(
            domain: String,
            functionName: String,
            args: Map<String, Any?>,
            target: Any
        ): Any? {
            return null
        }
    }

    private class MockMCPToolExecutor : AbstractToolExecutor() {
        override val domain: String = "mcp.test-server"
        override val targetClass: KClass<*> = Any::class

        override suspend fun callFunctionOn(
            domain: String,
            functionName: String,
            args: Map<String, Any?>,
            target: Any
        ): Any? {
            return null
        }
    }

    private class ComplexToolExecutor : AbstractToolExecutor() {
        override val domain: String = "complex"
        override val targetClass: KClass<*> = Any::class

        override suspend fun callFunctionOn(
            domain: String,
            functionName: String,
            args: Map<String, Any?>,
            target: Any
        ): Any? {
            return null
        }
    }

    private class TestSkillWithToolSpec : AbstractSkill() {
        override val metadata = SkillMetadata(
            id = "test-skill",
            name = "Test Skill",
            version = "1.0.0",
            description = "A test skill for demonstration"
        )

        override val toolSpec = listOf(
            ToolSpec(
                domain = "skill.test",
                method = "process",
                arguments = listOf(
                    ToolSpec.Arg("input", "String"),
                    ToolSpec.Arg("options", "Map<String, Any?>", "emptyMap()")
                ),
                returnType = "Any?",
                description = "Process input data with options"
            )
        )

        override suspend fun execute(context: SkillContext, params: Map<String, Any>): SkillResult {
            return SkillResult.success(data = "Test result")
        }
    }
}
