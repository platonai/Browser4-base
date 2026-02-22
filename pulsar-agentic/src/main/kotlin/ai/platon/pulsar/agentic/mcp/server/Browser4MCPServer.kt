package ai.platon.pulsar.agentic.mcp.server

import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Browser4 MCP Server — exposes browser automation capabilities as MCP tools.
 *
 * This server allows external MCP clients (Claude Desktop, Cursor, Windsurf, etc.)
 * to drive a real browser through the Model Context Protocol.
 *
 * ## Tool Design Rationale
 *
 * Tools are grouped into five categories:
 *
 * ### 1. Navigation (navigate_to, go_back, go_forward, reload, current_url)
 * Every useful browser task starts with navigation. These tools give the LLM
 * full control over the URL bar and history, enabling multi-step workflows
 * such as login → menu → form → submit sequences.
 *
 * ### 2. Element Interaction (click, type, fill, hover, scroll_to, check, uncheck, press)
 * The most frequent actions an agent performs are clicking and typing.
 * Exposing granular interaction primitives instead of a single "do_everything" action
 * keeps tool calls predictable, auditable, and easy to retry on failure.
 * - `fill` clears then types (form reset pattern)
 * - `check`/`uncheck` handle boolean toggles (checkboxes, radio buttons)
 * - `press` sends keyboard keys for hotkeys (Enter, Tab, Escape)
 * - `hover` is required for revealing hover menus and tooltips
 *
 * ### 3. Page Content (get_text, get_html, get_attribute, page_source, screenshot)
 * Agents need to read the page to decide their next action.
 * - `get_text` / `get_html` read specific elements
 * - `get_attribute` reads metadata (href, src, value, data-* attributes)
 * - `page_source` returns the full DOM (useful when element structure is unknown)
 * - `screenshot` provides a visual snapshot for multimodal reasoning
 *
 * ### 4. Waiting & Synchronisation (wait_for_selector, wait_for_navigation)
 * Dynamic pages (SPAs, AJAX, lazy-loading) require explicit waits.
 * Omitting wait tools forces LLMs to insert ad-hoc delays, which are brittle.
 * Correct synchronisation dramatically reduces flakiness.
 *
 * ### 5. JavaScript Evaluation (evaluate)
 * A power-user escape hatch. When CSS selectors cannot reach a target
 * (e.g. shadow DOM, canvas overlays, hidden state), `evaluate` lets the
 * agent inject arbitrary JavaScript. Annotated as open-world / non-read-only
 * to signal its elevated risk to the client.
 *
 * @param driver The [WebDriver] instance that will execute browser actions.
 * @param serverInfo MCP server identification (name and version).
 */
class Browser4MCPServer(
    private val driver: WebDriver,
    serverInfo: Implementation = Implementation(name = "browser4-mcp-server", version = "1.0.0"),
) {

    private val logger = getLogger(this)

    val server: Server = Server(
        serverInfo = serverInfo,
        options = ServerOptions(
            capabilities = ServerCapabilities(
                tools = ServerCapabilities.Tools(listChanged = false)
            )
        ),
        instructions = """
            Browser4 MCP Server gives you full control over a real Chrome browser.
            Use the tools in order: navigate first, then interact, then read content.
            Always call wait_for_selector or wait_for_navigation after actions that
            trigger page loads or dynamic updates.
        """.trimIndent()
    ) {
        registerNavigationTools()
        registerInteractionTools()
        registerContentTools()
        registerWaitTools()
        registerJavaScriptTools()
    }

    // -------------------------------------------------------------------------
    // Helpers — build ToolSchema property descriptors
    // -------------------------------------------------------------------------

    private fun stringProp(description: String): JsonObject =
        JsonObject(mapOf("type" to JsonPrimitive("string"), "description" to JsonPrimitive(description)))

    private fun intProp(description: String): JsonObject =
        JsonObject(mapOf("type" to JsonPrimitive("integer"), "description" to JsonPrimitive(description)))

    private fun schemaOf(vararg props: Pair<String, JsonObject>, required: List<String> = emptyList()): ToolSchema =
        ToolSchema(properties = JsonObject(props.toMap()), required = required)

    // -------------------------------------------------------------------------
    // Argument parsing
    // -------------------------------------------------------------------------

    private fun arg(arguments: JsonObject?, key: String): String? =
        arguments?.get(key)?.toString()?.trim('"')

    // -------------------------------------------------------------------------
    // Result helpers
    // -------------------------------------------------------------------------

    private fun textResult(text: String): CallToolResult =
        CallToolResult(content = listOf(TextContent(text = text)))

    private fun errorResult(message: String): CallToolResult {
        logger.warn("MCP tool error: {}", message)
        return CallToolResult(content = listOf(TextContent(text = "ERROR: $message")), isError = true)
    }

    // -------------------------------------------------------------------------
    // Navigation tools
    // -------------------------------------------------------------------------

    private fun Server.registerNavigationTools() {

        addTool(
            name = "navigate_to",
            description = "Navigate the browser to the given URL. " +
                "Call wait_for_navigation or wait_for_selector afterwards if the page is dynamic.",
            inputSchema = schemaOf(
                "url" to stringProp("The URL to navigate to, e.g. https://example.com"),
                required = listOf("url")
            )
        ) { request ->
            val url = arg(request.params.arguments, "url")
                ?: return@addTool errorResult("Missing required parameter: url")
            runCatching { driver.navigateTo(url) }
                .fold(
                    onSuccess = { textResult("Navigated to $url") },
                    onFailure = { errorResult("Navigation failed: ${it.message}") }
                )
        }

        addTool(
            name = "go_back",
            description = "Navigate back to the previous page in the browser history.",
            inputSchema = ToolSchema()
        ) { _ ->
            runCatching { driver.goBack() }
                .fold(
                    onSuccess = { textResult("Navigated back") },
                    onFailure = { errorResult("go_back failed: ${it.message}") }
                )
        }

        addTool(
            name = "go_forward",
            description = "Navigate forward in the browser history.",
            inputSchema = ToolSchema()
        ) { _ ->
            runCatching { driver.goForward() }
                .fold(
                    onSuccess = { textResult("Navigated forward") },
                    onFailure = { errorResult("go_forward failed: ${it.message}") }
                )
        }

        addTool(
            name = "reload",
            description = "Reload the current page.",
            inputSchema = ToolSchema()
        ) { _ ->
            runCatching { driver.reload() }
                .fold(
                    onSuccess = { textResult("Page reloaded") },
                    onFailure = { errorResult("reload failed: ${it.message}") }
                )
        }

        addTool(
            name = "current_url",
            description = "Return the URL currently loaded in the browser.",
            inputSchema = ToolSchema()
        ) { _ ->
            runCatching { driver.currentUrl() ?: "" }
                .fold(
                    onSuccess = { url -> textResult(url) },
                    onFailure = { errorResult("current_url failed: ${it.message}") }
                )
        }
    }

    // -------------------------------------------------------------------------
    // Element interaction tools
    // -------------------------------------------------------------------------

    private fun Server.registerInteractionTools() {

        addTool(
            name = "click",
            description = "Click the first element that matches the CSS selector.",
            inputSchema = schemaOf(
                "selector" to stringProp("CSS selector for the element to click"),
                required = listOf("selector")
            )
        ) { request ->
            val selector = arg(request.params.arguments, "selector")
                ?: return@addTool errorResult("Missing required parameter: selector")
            runCatching { driver.click(selector) }
                .fold(
                    onSuccess = { textResult("Clicked '$selector'") },
                    onFailure = { errorResult("click failed: ${it.message}") }
                )
        }

        addTool(
            name = "type",
            description = "Type text into the element matching the CSS selector, " +
                "appending to any existing value.",
            inputSchema = schemaOf(
                "selector" to stringProp("CSS selector for the input element"),
                "text" to stringProp("Text to type"),
                required = listOf("selector", "text")
            )
        ) { request ->
            val selector = arg(request.params.arguments, "selector")
                ?: return@addTool errorResult("Missing required parameter: selector")
            val text = arg(request.params.arguments, "text")
                ?: return@addTool errorResult("Missing required parameter: text")
            runCatching { driver.type(selector, text) }
                .fold(
                    onSuccess = { textResult("Typed into '$selector'") },
                    onFailure = { errorResult("type failed: ${it.message}") }
                )
        }

        addTool(
            name = "fill",
            description = "Clear the element matching the CSS selector and then type the given text. " +
                "Preferred over 'type' when you need to replace an existing value.",
            inputSchema = schemaOf(
                "selector" to stringProp("CSS selector for the input element"),
                "text" to stringProp("Text to fill"),
                required = listOf("selector", "text")
            )
        ) { request ->
            val selector = arg(request.params.arguments, "selector")
                ?: return@addTool errorResult("Missing required parameter: selector")
            val text = arg(request.params.arguments, "text")
                ?: return@addTool errorResult("Missing required parameter: text")
            runCatching { driver.fill(selector, text) }
                .fold(
                    onSuccess = { textResult("Filled '$selector'") },
                    onFailure = { errorResult("fill failed: ${it.message}") }
                )
        }

        addTool(
            name = "hover",
            description = "Move the mouse cursor over the element matching the CSS selector. " +
                "Use this to reveal dropdown menus, tooltips, and hover-activated UI components.",
            inputSchema = schemaOf(
                "selector" to stringProp("CSS selector for the element to hover over"),
                required = listOf("selector")
            )
        ) { request ->
            val selector = arg(request.params.arguments, "selector")
                ?: return@addTool errorResult("Missing required parameter: selector")
            runCatching { driver.hover(selector) }
                .fold(
                    onSuccess = { textResult("Hovered over '$selector'") },
                    onFailure = { errorResult("hover failed: ${it.message}") }
                )
        }

        addTool(
            name = "scroll_to",
            description = "Scroll the page until the element matching the CSS selector is visible in the viewport.",
            inputSchema = schemaOf(
                "selector" to stringProp("CSS selector of the element to scroll to"),
                required = listOf("selector")
            )
        ) { request ->
            val selector = arg(request.params.arguments, "selector")
                ?: return@addTool errorResult("Missing required parameter: selector")
            runCatching { driver.scrollTo(selector) }
                .fold(
                    onSuccess = { textResult("Scrolled to '$selector'") },
                    onFailure = { errorResult("scroll_to failed: ${it.message}") }
                )
        }

        addTool(
            name = "check",
            description = "Check the checkbox or radio button matching the CSS selector.",
            inputSchema = schemaOf(
                "selector" to stringProp("CSS selector for the checkbox or radio button"),
                required = listOf("selector")
            )
        ) { request ->
            val selector = arg(request.params.arguments, "selector")
                ?: return@addTool errorResult("Missing required parameter: selector")
            runCatching { driver.check(selector) }
                .fold(
                    onSuccess = { textResult("Checked '$selector'") },
                    onFailure = { errorResult("check failed: ${it.message}") }
                )
        }

        addTool(
            name = "uncheck",
            description = "Uncheck the checkbox matching the CSS selector.",
            inputSchema = schemaOf(
                "selector" to stringProp("CSS selector for the checkbox"),
                required = listOf("selector")
            )
        ) { request ->
            val selector = arg(request.params.arguments, "selector")
                ?: return@addTool errorResult("Missing required parameter: selector")
            runCatching { driver.uncheck(selector) }
                .fold(
                    onSuccess = { textResult("Unchecked '$selector'") },
                    onFailure = { errorResult("uncheck failed: ${it.message}") }
                )
        }

        addTool(
            name = "press",
            description = "Dispatch a keyboard key event on the element matching the CSS selector. " +
                "Common keys: Enter, Tab, Escape, ArrowDown, ArrowUp, Space.",
            inputSchema = schemaOf(
                "selector" to stringProp("CSS selector of the focused element"),
                "key" to stringProp("Key name, e.g. Enter, Tab, Escape, ArrowDown"),
                required = listOf("selector", "key")
            )
        ) { request ->
            val selector = arg(request.params.arguments, "selector")
                ?: return@addTool errorResult("Missing required parameter: selector")
            val key = arg(request.params.arguments, "key")
                ?: return@addTool errorResult("Missing required parameter: key")
            runCatching { driver.press(selector, key) }
                .fold(
                    onSuccess = { textResult("Pressed '$key' on '$selector'") },
                    onFailure = { errorResult("press failed: ${it.message}") }
                )
        }
    }

    // -------------------------------------------------------------------------
    // Page content tools
    // -------------------------------------------------------------------------

    private fun Server.registerContentTools() {

        addTool(
            name = "get_text",
            description = "Return the visible text content of the first element matching the CSS selector.",
            inputSchema = schemaOf(
                "selector" to stringProp("CSS selector of the target element"),
                required = listOf("selector")
            )
        ) { request ->
            val selector = arg(request.params.arguments, "selector")
                ?: return@addTool errorResult("Missing required parameter: selector")
            runCatching { driver.selectFirstTextOrNull(selector) ?: "" }
                .fold(
                    onSuccess = { textResult(it) },
                    onFailure = { errorResult("get_text failed: ${it.message}") }
                )
        }

        addTool(
            name = "get_html",
            description = "Return the outer HTML of the first element matching the CSS selector. " +
                "Use page_source to get the full page HTML.",
            inputSchema = schemaOf(
                "selector" to stringProp("CSS selector of the target element"),
                required = listOf("selector")
            )
        ) { request ->
            val selector = arg(request.params.arguments, "selector")
                ?: return@addTool errorResult("Missing required parameter: selector")
            runCatching { driver.outerHTML(selector) ?: "" }
                .fold(
                    onSuccess = { textResult(it) },
                    onFailure = { errorResult("get_html failed: ${it.message}") }
                )
        }

        addTool(
            name = "get_attribute",
            description = "Return the value of an attribute on the first element matching the CSS selector. " +
                "Useful for reading href, src, value, data-* attributes, etc.",
            inputSchema = schemaOf(
                "selector" to stringProp("CSS selector of the target element"),
                "attribute" to stringProp("Attribute name, e.g. href, src, value, data-id"),
                required = listOf("selector", "attribute")
            )
        ) { request ->
            val selector = arg(request.params.arguments, "selector")
                ?: return@addTool errorResult("Missing required parameter: selector")
            val attribute = arg(request.params.arguments, "attribute")
                ?: return@addTool errorResult("Missing required parameter: attribute")
            runCatching { driver.selectFirstAttributeOrNull(selector, attribute) ?: "" }
                .fold(
                    onSuccess = { textResult(it) },
                    onFailure = { errorResult("get_attribute failed: ${it.message}") }
                )
        }

        addTool(
            name = "page_source",
            description = "Return the full HTML source of the current page. " +
                "Use this when you need to understand the full DOM structure. " +
                "For individual elements prefer get_html to reduce token usage.",
            inputSchema = ToolSchema()
        ) { _ ->
            runCatching { driver.pageSource() ?: "" }
                .fold(
                    onSuccess = { textResult(it) },
                    onFailure = { errorResult("page_source failed: ${it.message}") }
                )
        }

        addTool(
            name = "screenshot",
            description = "Capture a screenshot of the current browser viewport and return it as a Base64-encoded PNG. " +
                "Use this for visual verification or when the page structure is unclear.",
            inputSchema = ToolSchema()
        ) { _ ->
            // captureScreenshot() already returns a Base64-encoded PNG string
            runCatching { driver.captureScreenshot() }
                .fold(
                    onSuccess = { base64 ->
                        if (base64 == null) {
                            errorResult("screenshot returned null")
                        } else {
                            CallToolResult(
                                content = listOf(
                                    io.modelcontextprotocol.kotlin.sdk.types.ImageContent(
                                        data = base64,
                                        mimeType = "image/png"
                                    )
                                )
                            )
                        }
                    },
                    onFailure = { errorResult("screenshot failed: ${it.message}") }
                )
        }
    }

    // -------------------------------------------------------------------------
    // Wait / synchronisation tools
    // -------------------------------------------------------------------------

    private fun Server.registerWaitTools() {

        addTool(
            name = "wait_for_selector",
            description = "Wait until an element matching the CSS selector appears in the DOM. " +
                "Call this after actions that trigger dynamic content (AJAX, SPA transitions).",
            inputSchema = schemaOf(
                "selector" to stringProp("CSS selector to wait for"),
                "timeout_ms" to intProp("Maximum time to wait in milliseconds (default: 30000)"),
                required = listOf("selector")
            )
        ) { request ->
            val selector = arg(request.params.arguments, "selector")
                ?: return@addTool errorResult("Missing required parameter: selector")
            val timeoutMs = arg(request.params.arguments, "timeout_ms")?.toLongOrNull() ?: 30_000L
            runCatching { driver.waitForSelector(selector, timeoutMs) }
                .fold(
                    onSuccess = { textResult("Element '$selector' found") },
                    onFailure = { errorResult("wait_for_selector failed: ${it.message}") }
                )
        }

        addTool(
            name = "wait_for_navigation",
            description = "Wait for a page navigation to complete. " +
                "Call this after clicking a link or submitting a form that navigates to a new URL.",
            inputSchema = schemaOf(
                "timeout_ms" to intProp("Maximum time to wait in milliseconds (default: 30000)")
            )
        ) { request ->
            val timeoutMs = arg(request.params.arguments, "timeout_ms")?.toLongOrNull() ?: 30_000L
            runCatching { driver.waitForNavigation(timeoutMillis = timeoutMs) }
                .fold(
                    onSuccess = { textResult("Navigation completed") },
                    onFailure = { errorResult("wait_for_navigation failed: ${it.message}") }
                )
        }
    }

    // -------------------------------------------------------------------------
    // JavaScript evaluation tools
    // -------------------------------------------------------------------------

    private fun Server.registerJavaScriptTools() {

        addTool(
            name = "evaluate",
            description = "Execute a JavaScript expression in the browser and return the result as a string. " +
                "Use this as an escape hatch when CSS selectors cannot reach the target " +
                "(shadow DOM, canvas, hidden state). " +
                "Warning: arbitrary code execution — use only when necessary.",
            inputSchema = schemaOf(
                "expression" to stringProp("JavaScript expression to evaluate, e.g. document.title"),
                required = listOf("expression")
            )
        ) { request ->
            val expression = arg(request.params.arguments, "expression")
                ?: return@addTool errorResult("Missing required parameter: expression")
            runCatching { driver.evaluate(expression) }
                .fold(
                    onSuccess = { result -> textResult(result?.toString() ?: "null") },
                    onFailure = { errorResult("evaluate failed: ${it.message}") }
                )
        }
    }
}
