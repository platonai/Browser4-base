package ai.platon.pulsar.agentic.mcp.server

import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import io.mockk.coEvery
import io.mockk.mockk
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit tests for [Browser4MCPServer].
 *
 * These tests verify that:
 * - All expected MCP tools are registered with correct names.
 * - Each tool returns a non-error result when the underlying WebDriver succeeds.
 * - Each tool returns an error result (isError = true) when the WebDriver throws.
 */
@DisplayName("Browser4MCPServer")
class Browser4MCPServerTest {

    private lateinit var driver: WebDriver
    private lateinit var mcpServer: Browser4MCPServer

    @BeforeEach
    fun setUp() {
        driver = mockk(relaxed = true)
        mcpServer = Browser4MCPServer(
            driver = driver,
            serverInfo = Implementation(name = "browser4-test", version = "0.0.0")
        )
    }

    // -------------------------------------------------------------------------
    // Tool registration
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("registers all expected navigation tools")
    fun registersNavigationTools() {
        val names = mcpServer.server.tools.keys
        assertTrue(names.contains("navigate_to"), "Expected navigate_to")
        assertTrue(names.contains("go_back"), "Expected go_back")
        assertTrue(names.contains("go_forward"), "Expected go_forward")
        assertTrue(names.contains("reload"), "Expected reload")
        assertTrue(names.contains("current_url"), "Expected current_url")
    }

    @Test
    @DisplayName("registers all expected element interaction tools")
    fun registersInteractionTools() {
        val names = mcpServer.server.tools.keys
        assertTrue(names.contains("click"), "Expected click")
        assertTrue(names.contains("type"), "Expected type")
        assertTrue(names.contains("fill"), "Expected fill")
        assertTrue(names.contains("hover"), "Expected hover")
        assertTrue(names.contains("scroll_to"), "Expected scroll_to")
        assertTrue(names.contains("check"), "Expected check")
        assertTrue(names.contains("uncheck"), "Expected uncheck")
        assertTrue(names.contains("press"), "Expected press")
    }

    @Test
    @DisplayName("registers all expected page content tools")
    fun registersContentTools() {
        val names = mcpServer.server.tools.keys
        assertTrue(names.contains("get_text"), "Expected get_text")
        assertTrue(names.contains("get_html"), "Expected get_html")
        assertTrue(names.contains("get_attribute"), "Expected get_attribute")
        assertTrue(names.contains("page_source"), "Expected page_source")
        assertTrue(names.contains("screenshot"), "Expected screenshot")
    }

    @Test
    @DisplayName("registers all expected wait and JavaScript tools")
    fun registersWaitAndJsTools() {
        val names = mcpServer.server.tools.keys
        assertTrue(names.contains("wait_for_selector"), "Expected wait_for_selector")
        assertTrue(names.contains("wait_for_navigation"), "Expected wait_for_navigation")
        assertTrue(names.contains("evaluate"), "Expected evaluate")
    }

    @Test
    @DisplayName("exposes exactly 21 tools")
    fun registersCorrectToolCount() {
        assertEquals(21, mcpServer.server.tools.size,
            "Expected exactly 21 registered tools, got: ${mcpServer.server.tools.keys}")
    }

    // -------------------------------------------------------------------------
    // Happy path — successful tool invocations
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("navigate_to returns success message when WebDriver succeeds")
    fun navigateToReturnsSuccessMessage() = runBlocking {
        coEvery { driver.navigateTo(any<String>()) } returns Unit

        val tool = mcpServer.server.tools["navigate_to"]!!
        val request = buildRequest("navigate_to", mapOf("url" to "https://example.com"))
        val result = tool.handler(request)

        assertFalse(result.isError == true, "Expected isError=false")
        val text = (result.content.firstOrNull() as? io.modelcontextprotocol.kotlin.sdk.types.TextContent)?.text
        assertTrue(text?.contains("https://example.com") == true)
    }

    @Test
    @DisplayName("get_text returns element text when WebDriver succeeds")
    fun getTextReturnsElementText() = runBlocking {
        coEvery { driver.selectFirstTextOrNull("h1") } returns "Page Title"

        val tool = mcpServer.server.tools["get_text"]!!
        val request = buildRequest("get_text", mapOf("selector" to "h1"))
        val result = tool.handler(request)

        assertFalse(result.isError == true)
        val text = (result.content.firstOrNull() as? io.modelcontextprotocol.kotlin.sdk.types.TextContent)?.text
        assertEquals("Page Title", text)
    }

    @Test
    @DisplayName("evaluate returns JS result when WebDriver succeeds")
    fun evaluateReturnsJsResult() = runBlocking {
        coEvery { driver.evaluate("document.title") } returns "My Page"

        val tool = mcpServer.server.tools["evaluate"]!!
        val request = buildRequest("evaluate", mapOf("expression" to "document.title"))
        val result = tool.handler(request)

        assertFalse(result.isError == true)
        val text = (result.content.firstOrNull() as? io.modelcontextprotocol.kotlin.sdk.types.TextContent)?.text
        assertEquals("My Page", text)
    }

    @Test
    @DisplayName("current_url returns the current URL from the driver")
    fun currentUrlReturnsDriverUrl() = runBlocking {
        coEvery { driver.currentUrl() } returns "https://example.com/page"

        val tool = mcpServer.server.tools["current_url"]!!
        val result = tool.handler(buildRequest("current_url", emptyMap()))

        assertFalse(result.isError == true)
        val text = (result.content.firstOrNull() as? io.modelcontextprotocol.kotlin.sdk.types.TextContent)?.text
        assertEquals("https://example.com/page", text)
    }

    // -------------------------------------------------------------------------
    // Error handling — WebDriver failures produce isError=true results
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("navigate_to returns error result when WebDriver throws")
    fun navigateToReturnsErrorOnFailure() = runBlocking {
        coEvery { driver.navigateTo(any<String>()) } throws RuntimeException("CDP connection lost")

        val tool = mcpServer.server.tools["navigate_to"]!!
        val request = buildRequest("navigate_to", mapOf("url" to "https://example.com"))
        val result = tool.handler(request)

        assertTrue(result.isError == true, "Expected isError=true")
        assertTrue(
            (result.content.firstOrNull() as? io.modelcontextprotocol.kotlin.sdk.types.TextContent)
                ?.text?.contains("CDP connection lost") == true,
            "Expected error message in content"
        )
    }

    @Test
    @DisplayName("click returns error result when WebDriver throws")
    fun clickReturnsErrorOnFailure() = runBlocking {
        coEvery { driver.click(any()) } throws RuntimeException("Element not found")

        val tool = mcpServer.server.tools["click"]!!
        val request = buildRequest("click", mapOf("selector" to "#btn"))
        val result = tool.handler(request)

        assertTrue(result.isError == true)
    }

    @Test
    @DisplayName("get_text returns error result when WebDriver throws")
    fun getTextReturnsErrorOnFailure() = runBlocking {
        coEvery { driver.selectFirstTextOrNull(any()) } throws RuntimeException("Timeout")

        val tool = mcpServer.server.tools["get_text"]!!
        val request = buildRequest("get_text", mapOf("selector" to "h1"))
        val result = tool.handler(request)

        assertTrue(result.isError == true)
    }

    @Test
    @DisplayName("evaluate returns error result when WebDriver throws")
    fun evaluateReturnsErrorOnFailure() = runBlocking {
        coEvery { driver.evaluate(any()) } throws RuntimeException("SyntaxError")

        val tool = mcpServer.server.tools["evaluate"]!!
        val request = buildRequest("evaluate", mapOf("expression" to "invalid js ("))
        val result = tool.handler(request)

        assertTrue(result.isError == true)
    }

    // -------------------------------------------------------------------------
    // Missing required parameters produce error results
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("navigate_to returns error when url parameter is missing")
    fun navigateToMissingUrlReturnsError() = runBlocking {
        val tool = mcpServer.server.tools["navigate_to"]!!
        val request = buildRequest("navigate_to", emptyMap())
        val result = tool.handler(request)

        assertTrue(result.isError == true)
        val text = (result.content.firstOrNull() as? io.modelcontextprotocol.kotlin.sdk.types.TextContent)?.text
        assertTrue(text?.contains("url") == true)
    }

    @Test
    @DisplayName("click returns error when selector parameter is missing")
    fun clickMissingSelectorReturnsError() = runBlocking {
        val tool = mcpServer.server.tools["click"]!!
        val request = buildRequest("click", emptyMap())
        val result = tool.handler(request)

        assertTrue(result.isError == true)
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Builds a [CallToolRequest] with the given tool name and string arguments.
     * Each value in [arguments] is wrapped in a [JsonPrimitive]; the server code
     * strips surrounding JSON quotes with `toString().trim('"')`.
     */
    private fun buildRequest(
        toolName: String,
        arguments: Map<String, String>
    ): io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest {
        val jsonArgs = arguments.entries.associate { (k, v) ->
            k to kotlinx.serialization.json.JsonPrimitive(v)
        }.let { kotlinx.serialization.json.JsonObject(it) }

        return io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest(
            params = io.modelcontextprotocol.kotlin.sdk.types.CallToolRequestParams(
                name = toolName,
                arguments = jsonArgs
            )
        )
    }
}
