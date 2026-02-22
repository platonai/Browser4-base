package ai.platon.pulsar.agentic.mcp.server

import ai.platon.pulsar.agentic.common.AgentFileSystem
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
 * - All expected MCP tools are registered with correct names,
 *   matching [ai.platon.pulsar.agentic.tools.specs.ToolSpecification.TOOL_CALL_SPECIFICATION].
 * - Each tool returns a non-error result when the underlying WebDriver succeeds.
 * - Each tool returns an error result (isError = true) when the WebDriver throws.
 */
@DisplayName("Browser4MCPServer")
class Browser4MCPServerTest {

    private lateinit var driver: WebDriver
    private lateinit var fileSystem: AgentFileSystem
    private lateinit var mcpServer: Browser4MCPServer

    @BeforeEach
    fun setUp() {
        driver = mockk(relaxed = true)
        fileSystem = mockk(relaxed = true)
        mcpServer = Browser4MCPServer(
            driver = driver,
            fileSystem = fileSystem,
            serverInfo = Implementation(name = "browser4-test", version = "0.0.0")
        )
    }

    // -------------------------------------------------------------------------
    // Tool registration — domain: driver
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("registers all expected driver navigation tools")
    fun registersDriverNavigationTools() {
        val names = mcpServer.server.tools.keys
        assertTrue(names.contains("navigate_to"), "Expected navigate_to")
        assertTrue(names.contains("reload"), "Expected reload")
        assertTrue(names.contains("go_back"), "Expected go_back")
        assertTrue(names.contains("go_forward"), "Expected go_forward")
        assertTrue(names.contains("wait_for_selector"), "Expected wait_for_selector")
    }

    @Test
    @DisplayName("registers all expected driver status tools")
    fun registersDriverStatusTools() {
        val names = mcpServer.server.tools.keys
        assertTrue(names.contains("exists"), "Expected exists")
        assertTrue(names.contains("is_visible"), "Expected is_visible")
        assertTrue(names.contains("focus"), "Expected focus")
    }

    @Test
    @DisplayName("registers all expected driver interaction tools")
    fun registersDriverInteractionTools() {
        val names = mcpServer.server.tools.keys
        assertTrue(names.contains("hover"), "Expected hover")
        assertTrue(names.contains("click"), "Expected click")
        assertTrue(names.contains("fill"), "Expected fill")
        assertTrue(names.contains("type"), "Expected type")
        assertTrue(names.contains("press"), "Expected press")
        assertTrue(names.contains("check"), "Expected check")
        assertTrue(names.contains("uncheck"), "Expected uncheck")
        assertTrue(names.contains("scroll_to"), "Expected scroll_to")
    }

    @Test
    @DisplayName("registers all expected driver scrolling tools")
    fun registersDriverScrollingTools() {
        val names = mcpServer.server.tools.keys
        assertTrue(names.contains("scroll_to_top"), "Expected scroll_to_top")
        assertTrue(names.contains("scroll_to_bottom"), "Expected scroll_to_bottom")
        assertTrue(names.contains("scroll_to_middle"), "Expected scroll_to_middle")
        assertTrue(names.contains("scroll_by"), "Expected scroll_by")
    }

    @Test
    @DisplayName("registers all expected driver content tools")
    fun registersDriverContentTools() {
        val names = mcpServer.server.tools.keys
        assertTrue(names.contains("text_content"), "Expected text_content")
        assertTrue(names.contains("get_text"), "Expected get_text")
        assertTrue(names.contains("delay"), "Expected delay")
    }

    // -------------------------------------------------------------------------
    // Tool registration — domain: browser
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("registers all expected browser tab tools")
    fun registersBrowserTabTools() {
        val names = mcpServer.server.tools.keys
        assertTrue(names.contains("switch_tab"), "Expected switch_tab")
        assertTrue(names.contains("close_tab"), "Expected close_tab")
    }

    // -------------------------------------------------------------------------
    // Tool registration — domain: fs
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("registers all expected file-system tools when fileSystem is provided")
    fun registersFileSystemTools() {
        val names = mcpServer.server.tools.keys
        assertTrue(names.contains("write_string"), "Expected write_string")
        assertTrue(names.contains("read_string"), "Expected read_string")
        assertTrue(names.contains("append"), "Expected append")
        assertTrue(names.contains("replace_content"), "Expected replace_content")
        assertTrue(names.contains("file_exists"), "Expected file_exists")
        assertTrue(names.contains("get_file_info"), "Expected get_file_info")
        assertTrue(names.contains("delete_file"), "Expected delete_file")
        assertTrue(names.contains("copy_file"), "Expected copy_file")
        assertTrue(names.contains("move_file"), "Expected move_file")
        assertTrue(names.contains("list_files"), "Expected list_files")
    }

    @Test
    @DisplayName("does not register file-system tools when fileSystem is null")
    fun doesNotRegisterFileSystemToolsWhenNull() {
        val serverWithoutFs = Browser4MCPServer(
            driver = driver,
            fileSystem = null,
            serverInfo = Implementation(name = "browser4-test-nofs", version = "0.0.0")
        )
        val names = serverWithoutFs.server.tools.keys
        assertFalse(names.contains("write_string"), "write_string should not be registered")
        assertFalse(names.contains("read_string"), "read_string should not be registered")
        assertFalse(names.contains("list_files"), "list_files should not be registered")
    }

    // -------------------------------------------------------------------------
    // Tool registration — domain: system
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("registers the help tool")
    fun registersHelpTool() {
        assertTrue(mcpServer.server.tools.keys.contains("help"), "Expected help")
    }

    // -------------------------------------------------------------------------
    // Tool count
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("exposes exactly 36 tools when fileSystem is provided and agent is null")
    fun registersCorrectToolCount() {
        // driver(23) + browser(2) + fs(10) + system(1) = 36
        assertEquals(36, mcpServer.server.tools.size,
            "Expected exactly 36 registered tools, got: ${mcpServer.server.tools.keys}")
    }

    @Test
    @DisplayName("exposes exactly 26 tools when fileSystem and agent are both null")
    fun registersCorrectToolCountWithoutOptionals() {
        val serverNoOptionals = Browser4MCPServer(
            driver = driver,
            fileSystem = null,
            agent = null,
            serverInfo = Implementation(name = "browser4-test-min", version = "0.0.0")
        )
        // driver(23) + browser(2) + system(1) = 26
        assertEquals(26, serverNoOptionals.server.tools.size,
            "Expected exactly 26 registered tools, got: ${serverNoOptionals.server.tools.keys}")
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
    @DisplayName("exists returns true when WebDriver reports element exists")
    fun existsReturnsTrueWhenElementFound() = runBlocking {
        coEvery { driver.exists("#btn") } returns true

        val tool = mcpServer.server.tools["exists"]!!
        val request = buildRequest("exists", mapOf("selector" to "#btn"))
        val result = tool.handler(request)

        assertFalse(result.isError == true)
        val text = (result.content.firstOrNull() as? io.modelcontextprotocol.kotlin.sdk.types.TextContent)?.text
        assertEquals("true", text)
    }

    @Test
    @DisplayName("click with modifier passes modifier to WebDriver")
    fun clickWithModifierPassesModifier() = runBlocking {
        coEvery { driver.click("#btn", "Shift") } returns Unit

        val tool = mcpServer.server.tools["click"]!!
        val request = buildRequest("click", mapOf("selector" to "#btn", "modifier" to "Shift"))
        val result = tool.handler(request)

        assertFalse(result.isError == true)
        val text = (result.content.firstOrNull() as? io.modelcontextprotocol.kotlin.sdk.types.TextContent)?.text
        assertTrue(text?.contains("Shift") == true)
    }

    @Test
    @DisplayName("scroll_to_top returns success when WebDriver succeeds")
    fun scrollToTopReturnsSuccess() = runBlocking {
        coEvery { driver.scrollToTop() } returns 0.0

        val tool = mcpServer.server.tools["scroll_to_top"]!!
        val result = tool.handler(buildRequest("scroll_to_top", emptyMap()))

        assertFalse(result.isError == true)
    }

    @Test
    @DisplayName("delay waits the specified number of milliseconds")
    fun delayWaitsSpecifiedMillis() = runBlocking {
        coEvery { driver.delay(any<Long>()) } returns Unit

        val tool = mcpServer.server.tools["delay"]!!
        val result = tool.handler(buildRequest("delay", mapOf("millis" to "100")))

        assertFalse(result.isError == true)
        val text = (result.content.firstOrNull() as? io.modelcontextprotocol.kotlin.sdk.types.TextContent)?.text
        assertTrue(text?.contains("100") == true)
    }

    @Test
    @DisplayName("help returns documentation for a known domain")
    fun helpReturnsDomainDocumentation() = runBlocking {
        val tool = mcpServer.server.tools["help"]!!
        val result = tool.handler(buildRequest("help", mapOf("domain" to "driver")))

        assertFalse(result.isError == true)
        val text = (result.content.firstOrNull() as? io.modelcontextprotocol.kotlin.sdk.types.TextContent)?.text
        assertTrue(text?.contains("driver.") == true, "Expected driver tool specs in help output")
    }

    @Test
    @DisplayName("help returns documentation for a specific method")
    fun helpReturnsMethodDocumentation() = runBlocking {
        val tool = mcpServer.server.tools["help"]!!
        val result = tool.handler(buildRequest("help", mapOf("domain" to "driver", "method" to "navigateTo")))

        assertFalse(result.isError == true)
        val text = (result.content.firstOrNull() as? io.modelcontextprotocol.kotlin.sdk.types.TextContent)?.text
        assertTrue(text?.contains("navigateTo") == true, "Expected navigateTo in help output")
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

    @Test
    @DisplayName("delay returns error when millis parameter is missing")
    fun delayMissingMillisReturnsError() = runBlocking {
        val tool = mcpServer.server.tools["delay"]!!
        val result = tool.handler(buildRequest("delay", emptyMap()))

        assertTrue(result.isError == true)
    }

    @Test
    @DisplayName("help returns error for unknown domain")
    fun helpReturnsErrorForUnknownDomain() = runBlocking {
        val tool = mcpServer.server.tools["help"]!!
        val result = tool.handler(buildRequest("help", mapOf("domain" to "nonexistent")))

        assertFalse(result.isError == true)
        val text = (result.content.firstOrNull() as? io.modelcontextprotocol.kotlin.sdk.types.TextContent)?.text
        assertTrue(text?.contains("Unknown domain") == true)
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
