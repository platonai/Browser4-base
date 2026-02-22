package ai.platon.pulsar.agentic.mcp.server

import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import io.mockk.coEvery
import io.mockk.mockk
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.PipedInputStream
import java.io.PipedOutputStream

/**
 * End-to-end tests for [Browser4MCPServer].
 *
 * These tests exercise the **full MCP protocol stack** by connecting a real MCP client
 * to a [Browser4MCPServer] over in-process STDIO pipes.  Unlike the unit tests in
 * [Browser4MCPServerTest] (which call tool handlers directly), these tests go through:
 *
 * 1. JSON-RPC initialization handshake (MCP `initialize` / `initialized`)
 * 2. `tools/list` request → response
 * 3. `tools/call` request → response (success and error cases)
 *
 * The [WebDriver] is mocked so no real browser is required.
 *
 * ## Transport
 * Two `PipedInputStream`/`PipedOutputStream` pairs create a bidirectional channel:
 * - **c2s** (client-to-server): client writes, server reads
 * - **s2c** (server-to-client): server writes, client reads
 *
 * The server runs in a background coroutine; the client connects synchronously before
 * each test.
 */
@Tag("E2ETest")
@Tag("mcp")
@DisplayName("Browser4MCPServer E2E (full MCP protocol)")
class Browser4MCPServerE2ETest {

    private lateinit var driver: WebDriver
    private lateinit var mcpServer: Browser4MCPServer
    private lateinit var client: Client
    private lateinit var serverJob: Job
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @BeforeEach
    fun setUp() = runBlocking {
        driver = mockk(relaxed = true)
        mcpServer = Browser4MCPServer(
            driver = driver,
            serverInfo = Implementation(name = "browser4-e2e-test", version = "1.0.0")
        )

        // ---- in-process bidirectional pipe ----
        // client → server direction
        val c2sIn = PipedInputStream()
        val c2sOut = PipedOutputStream(c2sIn)
        // server → client direction
        val s2cIn = PipedInputStream()
        val s2cOut = PipedOutputStream(s2cIn)

        // Start the server in a background coroutine
        val serverTransport = StdioServerTransport(
            inputStream = c2sIn.asSource().buffered(),
            outputStream = s2cOut.asSink().buffered()
        )
        serverJob = scope.launch {
            mcpServer.server.connect(serverTransport)
        }

        // Connect the MCP client
        val clientTransport = StdioClientTransport(
            input = s2cIn.asSource().buffered(),
            output = c2sOut.asSink().buffered()
        )
        client = Client(clientInfo = Implementation(name = "test-mcp-client", version = "1.0.0"))
        client.connect(clientTransport)
    }

    @AfterEach
    fun tearDown() = runBlocking {
        // Close the client first, then let the server drain cleanly before cancelling
        runCatching { client.close() }
        runCatching { mcpServer.server.close() }
        serverJob.cancel()
        scope.cancel()
    }

    // -------------------------------------------------------------------------
    // Tool listing tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("listTools returns all 21 registered tools")
    fun listToolsReturnsAll21Tools() = runBlocking {
        val result = client.listTools()
        assertNotNull(result)
        assertEquals(21, result.tools.size,
            "Expected 21 tools, got: ${result.tools.map { it.name }}")
    }

    @Test
    @DisplayName("listTools includes all navigation tools")
    fun listToolsIncludesNavigationTools() = runBlocking {
        val names = client.listTools().tools.map { it.name }.toSet()
        val expected = setOf("navigate_to", "go_back", "go_forward", "reload", "current_url")
        assertTrue(names.containsAll(expected),
            "Missing navigation tools: ${expected - names}")
    }

    @Test
    @DisplayName("listTools includes all element interaction tools")
    fun listToolsIncludesInteractionTools() = runBlocking {
        val names = client.listTools().tools.map { it.name }.toSet()
        val expected = setOf("click", "type", "fill", "hover", "scroll_to", "check", "uncheck", "press")
        assertTrue(names.containsAll(expected),
            "Missing interaction tools: ${expected - names}")
    }

    @Test
    @DisplayName("listTools includes all page content tools")
    fun listToolsIncludesContentTools() = runBlocking {
        val names = client.listTools().tools.map { it.name }.toSet()
        val expected = setOf("get_text", "get_html", "get_attribute", "page_source", "screenshot")
        assertTrue(names.containsAll(expected),
            "Missing content tools: ${expected - names}")
    }

    @Test
    @DisplayName("listTools includes wait and JavaScript tools")
    fun listToolsIncludesWaitAndJsTools() = runBlocking {
        val names = client.listTools().tools.map { it.name }.toSet()
        val expected = setOf("wait_for_selector", "wait_for_navigation", "evaluate")
        assertTrue(names.containsAll(expected),
            "Missing wait/JS tools: ${expected - names}")
    }

    @Test
    @DisplayName("each tool has a non-blank description and an inputSchema")
    fun eachToolHasDescriptionAndSchema() = runBlocking {
        val tools = client.listTools().tools
        tools.forEach { tool ->
            assertFalse(tool.description.isNullOrBlank(),
                "Tool '${tool.name}' has a blank description")
            assertNotNull(tool.inputSchema,
                "Tool '${tool.name}' has no inputSchema")
        }
    }

    // -------------------------------------------------------------------------
    // Successful tool execution tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("navigate_to succeeds and returns confirmation over MCP")
    fun navigateToSucceedsViaMCP() = runBlocking {
        coEvery { driver.navigateTo(any<String>()) } returns Unit

        val result = client.callTool("navigate_to", mapOf("url" to "https://example.com"))
        assertFalse(result.isError == true)
        val text = (result.content.firstOrNull() as? TextContent)?.text
        assertTrue(text?.contains("https://example.com") == true,
            "Expected URL in success message, got: $text")
    }

    @Test
    @DisplayName("current_url returns the driver's URL over MCP")
    fun currentUrlReturnsDriverUrlViaMCP() = runBlocking {
        coEvery { driver.currentUrl() } returns "https://example.com/page"

        val result = client.callTool("current_url", emptyMap())
        assertFalse(result.isError == true)
        val text = (result.content.firstOrNull() as? TextContent)?.text
        assertEquals("https://example.com/page", text)
    }

    @Test
    @DisplayName("click succeeds and returns confirmation over MCP")
    fun clickSucceedsViaMCP() = runBlocking {
        coEvery { driver.click(any()) } returns Unit

        val result = client.callTool("click", mapOf("selector" to "#submit-btn"))
        assertFalse(result.isError == true)
        val text = (result.content.firstOrNull() as? TextContent)?.text
        assertTrue(text?.contains("#submit-btn") == true)
    }

    @Test
    @DisplayName("get_text returns element text over MCP")
    fun getTextReturnsElementTextViaMCP() = runBlocking {
        coEvery { driver.selectFirstTextOrNull("h1") } returns "Welcome"

        val result = client.callTool("get_text", mapOf("selector" to "h1"))
        assertFalse(result.isError == true)
        val text = (result.content.firstOrNull() as? TextContent)?.text
        assertEquals("Welcome", text)
    }

    @Test
    @DisplayName("get_attribute returns the attribute value over MCP")
    fun getAttributeReturnsValueViaMCP() = runBlocking {
        coEvery { driver.selectFirstAttributeOrNull("a.nav", "href") } returns "/home"

        val result = client.callTool("get_attribute",
            mapOf("selector" to "a.nav", "attribute" to "href"))
        assertFalse(result.isError == true)
        val text = (result.content.firstOrNull() as? TextContent)?.text
        assertEquals("/home", text)
    }

    @Test
    @DisplayName("evaluate returns JavaScript result over MCP")
    fun evaluateReturnsJsResultViaMCP() = runBlocking {
        coEvery { driver.evaluate("document.title") } returns "My Page Title"

        val result = client.callTool("evaluate", mapOf("expression" to "document.title"))
        assertFalse(result.isError == true)
        val text = (result.content.firstOrNull() as? TextContent)?.text
        assertEquals("My Page Title", text)
    }

    @Test
    @DisplayName("fill succeeds and returns confirmation over MCP")
    fun fillSucceedsViaMCP() = runBlocking {
        coEvery { driver.fill(any(), any()) } returns Unit

        val result = client.callTool("fill",
            mapOf("selector" to "input[name=email]", "text" to "user@example.com"))
        assertFalse(result.isError == true)
    }

    @Test
    @DisplayName("go_back and go_forward succeed over MCP")
    fun goBackAndForwardSucceedViaMCP() = runBlocking {
        coEvery { driver.goBack() } returns Unit
        coEvery { driver.goForward() } returns Unit

        val backResult = client.callTool("go_back", emptyMap())
        assertFalse(backResult.isError == true)

        val fwdResult = client.callTool("go_forward", emptyMap())
        assertFalse(fwdResult.isError == true)
    }

    @Test
    @DisplayName("reload succeeds over MCP")
    fun reloadSucceedsViaMCP() = runBlocking {
        coEvery { driver.reload() } returns Unit

        val result = client.callTool("reload", emptyMap())
        assertFalse(result.isError == true)
        val text = (result.content.firstOrNull() as? TextContent)?.text
        assertEquals("Page reloaded", text)
    }

    // -------------------------------------------------------------------------
    // Error handling tests — via the full MCP protocol stack
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("navigate_to with missing url parameter returns isError=true over MCP")
    fun navigateToMissingUrlReturnsErrorViaMCP() = runBlocking {
        val result = client.callTool("navigate_to", emptyMap())
        assertTrue(result.isError == true,
            "Expected isError=true for missing url parameter")
        val text = (result.content.firstOrNull() as? TextContent)?.text
        assertTrue(text?.contains("url") == true,
            "Expected 'url' mentioned in error message, got: $text")
    }

    @Test
    @DisplayName("click with missing selector parameter returns isError=true over MCP")
    fun clickMissingSelectorReturnsErrorViaMCP() = runBlocking {
        val result = client.callTool("click", emptyMap())
        assertTrue(result.isError == true)
        val text = (result.content.firstOrNull() as? TextContent)?.text
        assertTrue(text?.contains("selector") == true)
    }

    @Test
    @DisplayName("get_attribute with missing attribute parameter returns isError=true over MCP")
    fun getAttributeMissingParamReturnsErrorViaMCP() = runBlocking {
        val result = client.callTool("get_attribute", mapOf("selector" to "div"))
        assertTrue(result.isError == true)
    }

    @Test
    @DisplayName("WebDriver failure in navigate_to propagates as isError=true over MCP")
    fun webDriverFailurePropagatesAsErrorViaMCP() = runBlocking {
        coEvery { driver.navigateTo(any<String>()) } throws RuntimeException("CDP disconnected")

        val result = client.callTool("navigate_to", mapOf("url" to "https://example.com"))
        assertTrue(result.isError == true,
            "Expected isError=true when WebDriver throws")
        val text = (result.content.firstOrNull() as? TextContent)?.text
        assertTrue(text?.contains("CDP disconnected") == true,
            "Expected driver error in message, got: $text")
    }

    @Test
    @DisplayName("WebDriver failure in evaluate propagates as isError=true over MCP")
    fun evaluateDriverFailurePropagatesAsErrorViaMCP() = runBlocking {
        coEvery { driver.evaluate(any()) } throws RuntimeException("SyntaxError: Unexpected token")

        val result = client.callTool("evaluate", mapOf("expression" to "{{bad"))
        assertTrue(result.isError == true)
        val text = (result.content.firstOrNull() as? TextContent)?.text
        assertTrue(text?.contains("SyntaxError") == true)
    }

    // -------------------------------------------------------------------------
    // Multiple sequential calls test
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("multiple sequential tool calls succeed over a single MCP connection")
    fun multipleSequentialCallsSucceedViaMCP() = runBlocking {
        coEvery { driver.navigateTo(any<String>()) } returns Unit
        coEvery { driver.selectFirstTextOrNull(any()) } returns "headline"
        coEvery { driver.evaluate(any()) } returns "42"

        val nav = client.callTool("navigate_to", mapOf("url" to "https://example.com"))
        assertFalse(nav.isError == true, "navigate_to failed")

        val text = client.callTool("get_text", mapOf("selector" to "h1"))
        assertFalse(text.isError == true, "get_text failed")
        assertEquals("headline", (text.content.firstOrNull() as? TextContent)?.text)

        val js = client.callTool("evaluate", mapOf("expression" to "1 + 1"))
        assertFalse(js.isError == true, "evaluate failed")
        assertEquals("42", (js.content.firstOrNull() as? TextContent)?.text)
    }
}
