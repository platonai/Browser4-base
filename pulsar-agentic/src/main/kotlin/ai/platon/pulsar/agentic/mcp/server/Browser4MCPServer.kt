package ai.platon.pulsar.agentic.mcp.server

import ai.platon.pulsar.agentic.PerceptiveAgent
import ai.platon.pulsar.agentic.common.AgentFileSystem
import ai.platon.pulsar.agentic.model.ExtractionSchema
import ai.platon.pulsar.agentic.tools.specs.ToolSpecification
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.skeleton.crawl.fetch.driver.AbstractBrowser
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
 * The exposed tools are kept in sync with
 * [ToolSpecification.TOOL_CALL_SPECIFICATION], which is the single source of truth
 * for every supported domain and method.
 *
 * ## Tool Domains
 *
 * ### 1. driver — WebDriver automation (navigate, interact, scroll, wait, read)
 * Every useful browser task starts with navigation. The driver tools give the LLM
 * full control over the URL bar, DOM interaction, and page content.
 *
 * ### 2. browser — Tab management (switch_tab, close_tab)
 * Multi-tab workflows require the ability to switch between and close browser tabs.
 *
 * ### 3. fs — File system operations (read/write/manage files)
 * Agents can persist extracted data, logs, and intermediate results to a sandboxed
 * file system without leaving the tool-call boundary.
 *
 * ### 4. agent — AI-powered extraction and summarisation (optional)
 * When a [PerceptiveAgent] is supplied, higher-level AI operations become available:
 * structured data extraction with a JSON schema and natural-language summarisation.
 *
 * ### 5. system — Help and introspection
 * Returns human-readable documentation for any tool domain or individual method,
 * derived directly from [ToolSpecification.TOOL_CALL_SPECIFICATION].
 *
 * @param driver The [WebDriver] instance that will execute browser actions.
 * @param fileSystem Optional [AgentFileSystem] for file-system tool support.
 *   When `null` the `fs.*` tools are not registered.
 * @param agent Optional [PerceptiveAgent] for AI-powered extraction and summarisation.
 *   When `null` the `agent.*` tools are not registered.
 * @param serverInfo MCP server identification (name and version).
 */
class Browser4MCPServer(
    private val driver: WebDriver,
    private val fileSystem: AgentFileSystem? = null,
    private val agent: PerceptiveAgent? = null,
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
            Always call wait_for_selector after actions that trigger page loads or dynamic updates.
            Use the 'help' tool to get detailed documentation for any domain or method.
        """.trimIndent()
    ) {
        registerDriverTools()
        registerBrowserTools()
        if (fileSystem != null) registerFileSystemTools(fileSystem)
        if (agent != null) registerAgentTools(agent)
        registerSystemTools()
    }

    // -------------------------------------------------------------------------
    // Helpers — build ToolSchema property descriptors
    // -------------------------------------------------------------------------

    private fun stringProp(description: String): JsonObject =
        JsonObject(mapOf("type" to JsonPrimitive("string"), "description" to JsonPrimitive(description)))

    private fun numberProp(description: String): JsonObject =
        JsonObject(mapOf("type" to JsonPrimitive("number"), "description" to JsonPrimitive(description)))

    private fun intProp(description: String): JsonObject =
        JsonObject(mapOf("type" to JsonPrimitive("integer"), "description" to JsonPrimitive(description)))

    private fun boolProp(description: String): JsonObject =
        JsonObject(mapOf("type" to JsonPrimitive("boolean"), "description" to JsonPrimitive(description)))

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
    // domain: driver
    // -------------------------------------------------------------------------

    private fun Server.registerDriverTools() {

        // driver.navigate(url: String)
        addTool(
            name = "navigate",
            description = "Navigate the browser to the given URL. " +
                    "Call wait_for_selector afterwards if the page has dynamic content.",
            inputSchema = schemaOf(
                "url" to stringProp("The URL to navigate to, e.g. https://example.com"),
                required = listOf("url")
            )
        ) { request ->
            val url = arg(request.params.arguments, "url")
                ?: return@addTool errorResult("Missing required parameter: url")
            runCatching { driver.navigate(url) }
                .fold(
                    onSuccess = { textResult("Navigated to $url") },
                    onFailure = { errorResult("navigate failed: ${it.message}") }
                )
        }

        // driver.reload()
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

        // driver.goBack()
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

        // driver.goForward()
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

        // driver.waitForSelector(selector: String, timeoutMillis: Long = 3000)
        addTool(
            name = "wait_for_selector",
            description = "Wait until an element matching the CSS selector appears in the DOM. " +
                    "Call this after actions that trigger dynamic content (AJAX, SPA transitions).",
            inputSchema = schemaOf(
                "selector" to stringProp("CSS selector to wait for"),
                "timeout_ms" to intProp("Maximum time to wait in milliseconds (default: 3000)"),
                required = listOf("selector")
            )
        ) { request ->
            val selector = arg(request.params.arguments, "selector")
                ?: return@addTool errorResult("Missing required parameter: selector")
            val timeoutMs = arg(request.params.arguments, "timeout_ms")?.toLongOrNull() ?: 3_000L
            runCatching { driver.waitForSelector(selector, timeoutMs) }
                .fold(
                    onSuccess = { textResult("Element '$selector' found") },
                    onFailure = { errorResult("wait_for_selector failed: ${it.message}") }
                )
        }

        // driver.exists(selector: String): Boolean
        addTool(
            name = "exists",
            description = "Return true if at least one element matching the CSS selector exists in the DOM.",
            inputSchema = schemaOf(
                "selector" to stringProp("CSS selector to check"),
                required = listOf("selector")
            )
        ) { request ->
            val selector = arg(request.params.arguments, "selector")
                ?: return@addTool errorResult("Missing required parameter: selector")
            runCatching { driver.exists(selector) }
                .fold(
                    onSuccess = { textResult(it.toString()) },
                    onFailure = { errorResult("exists failed: ${it.message}") }
                )
        }

        // driver.isVisible(selector: String): Boolean
        addTool(
            name = "is_visible",
            description = "Return true if the first element matching the CSS selector is visible in the viewport.",
            inputSchema = schemaOf(
                "selector" to stringProp("CSS selector to check"),
                required = listOf("selector")
            )
        ) { request ->
            val selector = arg(request.params.arguments, "selector")
                ?: return@addTool errorResult("Missing required parameter: selector")
            runCatching { driver.isVisible(selector) }
                .fold(
                    onSuccess = { textResult(it.toString()) },
                    onFailure = { errorResult("is_visible failed: ${it.message}") }
                )
        }

        // driver.focus(selector: String)
        addTool(
            name = "focus",
            description = "Move keyboard focus to the first element matching the CSS selector.",
            inputSchema = schemaOf(
                "selector" to stringProp("CSS selector for the element to focus"),
                required = listOf("selector")
            )
        ) { request ->
            val selector = arg(request.params.arguments, "selector")
                ?: return@addTool errorResult("Missing required parameter: selector")
            runCatching { driver.focus(selector) }
                .fold(
                    onSuccess = { textResult("Focused '$selector'") },
                    onFailure = { errorResult("focus failed: ${it.message}") }
                )
        }

        // driver.hover(selector: String)
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

        // driver.click(selector: String) and driver.click(selector: String, modifier: String)
        addTool(
            name = "click",
            description = "Click the first element matching the CSS selector. " +
                    "Optionally supply a keyboard modifier (e.g. Shift, Control, Alt, Meta) to perform a modified click.",
            inputSchema = schemaOf(
                "selector" to stringProp("CSS selector for the element to click"),
                "modifier" to stringProp("Optional keyboard modifier: Shift, Control, Alt, or Meta"),
                required = listOf("selector")
            )
        ) { request ->
            val selector = arg(request.params.arguments, "selector")
                ?: return@addTool errorResult("Missing required parameter: selector")
            val modifier = arg(request.params.arguments, "modifier")
            runCatching {
                if (modifier != null) driver.click(selector, modifier) else driver.click(selector)
            }
                .fold(
                    onSuccess = { textResult("Clicked '$selector'${if (modifier != null) " with $modifier" else ""}") },
                    onFailure = { errorResult("click failed: ${it.message}") }
                )
        }

        // driver.fill(selector: String, text: String)
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

        // driver.type(selector: String, text: String)
        addTool(
            name = "type",
            description = "Type text into the element matching the CSS selector, appending to any existing value.",
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

        // driver.press(selector: String, key: String)
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

        // driver.check(selector: String)
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

        // driver.uncheck(selector: String)
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

        // driver.scrollTo(selector: String)
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

        // driver.scrollToTop()
        addTool(
            name = "scroll_to_top",
            description = "Scroll the page to the very top.",
            inputSchema = ToolSchema()
        ) { _ ->
            runCatching { driver.scrollToTop() }
                .fold(
                    onSuccess = { textResult("Scrolled to top") },
                    onFailure = { errorResult("scroll_to_top failed: ${it.message}") }
                )
        }

        // driver.scrollToBottom()
        addTool(
            name = "scroll_to_bottom",
            description = "Scroll the page to the very bottom.",
            inputSchema = ToolSchema()
        ) { _ ->
            runCatching { driver.scrollToBottom() }
                .fold(
                    onSuccess = { textResult("Scrolled to bottom") },
                    onFailure = { errorResult("scroll_to_bottom failed: ${it.message}") }
                )
        }

        // driver.scrollToMiddle(ratio: Double = 0.5)
        addTool(
            name = "scroll_to_middle",
            description = "Scroll the page to a fractional position. " +
                    "ratio=0.0 is the top, ratio=1.0 is the bottom, ratio=0.5 (default) is the middle.",
            inputSchema = schemaOf(
                "ratio" to numberProp("Scroll position as a fraction of page height: 0.0=top, 1.0=bottom (default: 0.5)")
            )
        ) { request ->
            val ratio = arg(request.params.arguments, "ratio")?.toDoubleOrNull() ?: 0.5
            runCatching { driver.scrollToMiddle(ratio) }
                .fold(
                    onSuccess = { textResult("Scrolled to position $ratio") },
                    onFailure = { errorResult("scroll_to_middle failed: ${it.message}") }
                )
        }

        // driver.scrollBy(pixels: Double = 200.0): Double
        addTool(
            name = "scroll_by",
            description = "Scroll the page by the given number of pixels. " +
                    "Positive values scroll down; negative values scroll up.",
            inputSchema = schemaOf(
                "pixels" to numberProp("Pixels to scroll (default: 200.0; negative scrolls up)")
            )
        ) { request ->
            val pixels = arg(request.params.arguments, "pixels")?.toDoubleOrNull() ?: 200.0
            runCatching { driver.scrollBy(pixels) }
                .fold(
                    onSuccess = { scrolled -> textResult("Scrolled by $scrolled px") },
                    onFailure = { errorResult("scroll_by failed: ${it.message}") }
                )
        }

        // driver.textContent(): String?  — returns the document's text content (no selector)
        addTool(
            name = "text_content",
            description = "Return the full text content of the current page document.",
            inputSchema = ToolSchema()
        ) { _ ->
            runCatching { driver.textContent() ?: "" }
                .fold(
                    onSuccess = { textResult(it) },
                    onFailure = { errorResult("text_content failed: ${it.message}") }
                )
        }

        // driver.selectFirstTextOrNull(selector: String): String?
        addTool(
            name = "get_text",
            description = "Return the text content of the first element matching the CSS selector " +
                    "(descendants included). Returns empty string if no element is found.",
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

        // driver.delay(millis: Long)
        addTool(
            name = "delay",
            description = "Pause execution for the specified number of milliseconds. " +
                    "Prefer wait_for_selector for synchronising with dynamic content.",
            inputSchema = schemaOf(
                "millis" to intProp("Duration to wait in milliseconds"),
                required = listOf("millis")
            )
        ) { request ->
            val millis = arg(request.params.arguments, "millis")?.toLongOrNull()
                ?: return@addTool errorResult("Missing required parameter: millis")
            runCatching { driver.delay(millis) }
                .fold(
                    onSuccess = { textResult("Waited ${millis}ms") },
                    onFailure = { errorResult("delay failed: ${it.message}") }
                )
        }

        // driver.ariaSnapshot(): String
        addTool(
            name = "aria_snapshot",
            description = "Return the accessibility (ARIA) snapshot of the current page. " +
                    "The snapshot labels each interactive node with a short identifier (e.g. e15) " +
                    "that can be passed to click, fill, press and other interaction tools.",
            inputSchema = ToolSchema()
        ) { _ ->
            runCatching { driver.ariaSnapshot() }
                .fold(
                    onSuccess = { textResult(it) },
                    onFailure = { errorResult("aria_snapshot failed: ${it.message}") }
                )
        }

        // driver.currentUrl(): String
        addTool(
            name = "page_url",
            description = "Return the URL of the current page.",
            inputSchema = ToolSchema()
        ) { _ ->
            runCatching { driver.currentUrl() }
                .fold(
                    onSuccess = { textResult(it) },
                    onFailure = { errorResult("page_url failed: ${it.message}") }
                )
        }

        // driver.pageTitle(): String
        addTool(
            name = "page_title",
            description = "Return the title of the current page.",
            inputSchema = ToolSchema()
        ) { _ ->
            runCatching { driver.title() }
                .fold(
                    onSuccess = { textResult(it) },
                    onFailure = { errorResult("page_title failed: ${it.message}") }
                )
        }

        // driver.screenshot(fullPage: Boolean): String?
        addTool(
            name = "screenshot",
            description = "Capture a screenshot of the current page or a specific element. " +
                    "Returns a base64-encoded PNG string.",
            inputSchema = schemaOf(
                "selector" to stringProp("Optional CSS selector to screenshot a specific element"),
                "full_page" to boolProp("Whether to capture the full scrollable page (default: false)")
            )
        ) { request ->
            val selector = arg(request.params.arguments, "selector")
            val fullPage = arg(request.params.arguments, "full_page")?.toBooleanStrictOrNull() ?: false
            runCatching {
                if (selector != null) driver.screenshot(selector) else driver.screenshot(fullPage)
            }
                .fold(
                    onSuccess = { textResult(it ?: "") },
                    onFailure = { errorResult("screenshot failed: ${it.message}") }
                )
        }

        // driver.dblclick(selector: String)
        addTool(
            name = "dblclick",
            description = "Double-click the first element matching the CSS selector.",
            inputSchema = schemaOf(
                "selector" to stringProp("CSS selector for the element to double-click"),
                required = listOf("selector")
            )
        ) { request ->
            val selector = arg(request.params.arguments, "selector")
                ?: return@addTool errorResult("Missing required parameter: selector")
            runCatching { driver.dblclick(selector) }
                .fold(
                    onSuccess = { textResult("Double-clicked '$selector'") },
                    onFailure = { errorResult("dblclick failed: ${it.message}") }
                )
        }

        // driver.dragAndDrop(selector: String, deltaX: Int, deltaY: Int)
        addTool(
            name = "drag",
            description = "Drag the element matching sourceSelector to the position of targetSelector.",
            inputSchema = schemaOf(
                "source_selector" to stringProp("CSS selector of the element to drag"),
                "target_selector" to stringProp("CSS selector of the drop target"),
                required = listOf("source_selector", "target_selector")
            )
        ) { request ->
            val src = arg(request.params.arguments, "source_selector")
                ?: return@addTool errorResult("Missing required parameter: source_selector")
            val tgt = arg(request.params.arguments, "target_selector")
                ?: return@addTool errorResult("Missing required parameter: target_selector")
            runCatching {
                val script = """
                    (() => {
                        const s = document.querySelector('${src.replace("'", "\\'")}');
                        const t = document.querySelector('${tgt.replace("'", "\\'")}');
                        if (!s || !t) return JSON.stringify({dx:0,dy:0});
                        const sr = s.getBoundingClientRect();
                        const tr = t.getBoundingClientRect();
                        return JSON.stringify({dx: tr.x - sr.x + tr.width/2 - sr.width/2, dy: tr.y - sr.y + tr.height/2 - sr.height/2});
                    })()
                """.trimIndent()
                val result = driver.evaluate(script) as? String ?: """{"dx":0,"dy":0}"""
                val mapper = com.fasterxml.jackson.databind.ObjectMapper()
                val parsed = mapper.readTree(result)
                val dx = parsed.get("dx")?.asInt() ?: 0
                val dy = parsed.get("dy")?.asInt() ?: 0
                driver.dragAndDrop(src, dx, dy)
            }
                .fold(
                    onSuccess = { textResult("Dragged '$src' to '$tgt'") },
                    onFailure = { errorResult("drag failed: ${it.message}") }
                )
        }

        // driver.selectOption(selector: String, values: List<String>)
        addTool(
            name = "select_option",
            description = "Select one or more options in a dropdown element matching the CSS selector.",
            inputSchema = schemaOf(
                "selector" to stringProp("CSS selector for the select element"),
                "value" to stringProp("The option value to select"),
                required = listOf("selector", "value")
            )
        ) { request ->
            val selector = arg(request.params.arguments, "selector")
                ?: return@addTool errorResult("Missing required parameter: selector")
            val value = arg(request.params.arguments, "value")
                ?: return@addTool errorResult("Missing required parameter: value")
            runCatching { driver.selectOption(selector, listOf(value)) }
                .fold(
                    onSuccess = { textResult("Selected '$value' in '$selector'") },
                    onFailure = { errorResult("select_option failed: ${it.message}") }
                )
        }

        // driver.evaluate(expression: String): Any?
        addTool(
            name = "evaluate",
            description = "Evaluate a JavaScript expression in the browser and return the result.",
            inputSchema = schemaOf(
                "expression" to stringProp("JavaScript expression to evaluate"),
                required = listOf("expression")
            )
        ) { request ->
            val expression = arg(request.params.arguments, "expression")
                ?: return@addTool errorResult("Missing required parameter: expression")
            runCatching { driver.evaluate(expression) }
                .fold(
                    onSuccess = { textResult(it?.toString() ?: "null") },
                    onFailure = { errorResult("evaluate failed: ${it.message}") }
                )
        }

        // driver.dialogAccept(promptText: String?)
        addTool(
            name = "dialog_accept",
            description = "Accept the current browser dialog (alert/confirm/prompt). " +
                    "Optionally provide text for prompt dialogs.",
            inputSchema = schemaOf(
                "prompt_text" to stringProp("Optional text to enter in a prompt dialog")
            )
        ) { request ->
            val promptText = arg(request.params.arguments, "prompt_text")
            runCatching { driver.dialogAccept(promptText) }
                .fold(
                    onSuccess = { textResult("Dialog accepted") },
                    onFailure = { errorResult("dialog_accept failed: ${it.message}") }
                )
        }

        // driver.dialogDismiss()
        addTool(
            name = "dialog_dismiss",
            description = "Dismiss the current browser dialog (alert/confirm/prompt).",
            inputSchema = ToolSchema()
        ) { _ ->
            runCatching { driver.dialogDismiss() }
                .fold(
                    onSuccess = { textResult("Dialog dismissed") },
                    onFailure = { errorResult("dialog_dismiss failed: ${it.message}") }
                )
        }

        // driver.resize(width: Int, height: Int)
        addTool(
            name = "resize",
            description = "Resize the browser viewport to the specified width and height.",
            inputSchema = schemaOf(
                "width" to intProp("Viewport width in pixels"),
                "height" to intProp("Viewport height in pixels"),
                required = listOf("width", "height")
            )
        ) { request ->
            val width = arg(request.params.arguments, "width")?.toIntOrNull()
                ?: return@addTool errorResult("Missing required parameter: width")
            val height = arg(request.params.arguments, "height")?.toIntOrNull()
                ?: return@addTool errorResult("Missing required parameter: height")
            runCatching { driver.resize(width, height) }
                .fold(
                    onSuccess = { textResult("Resized to ${width}x${height}") },
                    onFailure = { errorResult("resize failed: ${it.message}") }
                )
        }

        // keydown — dispatch a keydown event via JS
        addTool(
            name = "keydown",
            description = "Dispatch a keydown event on the active element.",
            inputSchema = schemaOf(
                "key" to stringProp("Key name, e.g. Shift, Control, Alt"),
                required = listOf("key")
            )
        ) { request ->
            val key = arg(request.params.arguments, "key")
                ?: return@addTool errorResult("Missing required parameter: key")
            val safeKey = key.replace("'", "\\'")
            runCatching {
                driver.evaluate("document.activeElement.dispatchEvent(new KeyboardEvent('keydown', {key: '$safeKey', bubbles: true}))")
            }
                .fold(
                    onSuccess = { textResult("Key down: $key") },
                    onFailure = { errorResult("keydown failed: ${it.message}") }
                )
        }

        // keyup — dispatch a keyup event via JS
        addTool(
            name = "keyup",
            description = "Dispatch a keyup event on the active element.",
            inputSchema = schemaOf(
                "key" to stringProp("Key name, e.g. Shift, Control, Alt"),
                required = listOf("key")
            )
        ) { request ->
            val key = arg(request.params.arguments, "key")
                ?: return@addTool errorResult("Missing required parameter: key")
            val safeKey = key.replace("'", "\\'")
            runCatching {
                driver.evaluate("document.activeElement.dispatchEvent(new KeyboardEvent('keyup', {key: '$safeKey', bubbles: true}))")
            }
                .fold(
                    onSuccess = { textResult("Key up: $key") },
                    onFailure = { errorResult("keyup failed: ${it.message}") }
                )
        }

        // driver.moveMouseTo(x: Double, y: Double)
        addTool(
            name = "mousemove",
            description = "Move the mouse cursor to the specified coordinates.",
            inputSchema = schemaOf(
                "x" to numberProp("X coordinate"),
                "y" to numberProp("Y coordinate"),
                required = listOf("x", "y")
            )
        ) { request ->
            val x = arg(request.params.arguments, "x")?.toDoubleOrNull()
                ?: return@addTool errorResult("Missing required parameter: x")
            val y = arg(request.params.arguments, "y")?.toDoubleOrNull()
                ?: return@addTool errorResult("Missing required parameter: y")
            runCatching { driver.moveMouseTo(x, y) }
                .fold(
                    onSuccess = { textResult("Mouse moved to ($x, $y)") },
                    onFailure = { errorResult("mousemove failed: ${it.message}") }
                )
        }

        // mousedown — dispatch a mousedown event via JS
        addTool(
            name = "mousedown",
            description = "Dispatch a mousedown event at the current mouse position.",
            inputSchema = schemaOf(
                "button" to stringProp("Mouse button: left, right, or middle (default: left)")
            )
        ) { request ->
            val button = arg(request.params.arguments, "button") ?: "left"
            val btnIndex = when (button) { "right" -> 2; "middle" -> 1; else -> 0 }
            runCatching {
                driver.evaluate("document.elementFromPoint(window.__browser4MouseX||0, window.__browser4MouseY||0)?.dispatchEvent(new MouseEvent('mousedown', {button: $btnIndex, bubbles: true}))")
            }
                .fold(
                    onSuccess = { textResult("Mouse down ($button)") },
                    onFailure = { errorResult("mousedown failed: ${it.message}") }
                )
        }

        // mouseup — dispatch a mouseup event via JS
        addTool(
            name = "mouseup",
            description = "Dispatch a mouseup event at the current mouse position.",
            inputSchema = schemaOf(
                "button" to stringProp("Mouse button: left, right, or middle (default: left)")
            )
        ) { request ->
            val button = arg(request.params.arguments, "button") ?: "left"
            val btnIndex = when (button) { "right" -> 2; "middle" -> 1; else -> 0 }
            runCatching {
                driver.evaluate("document.elementFromPoint(window.__browser4MouseX||0, window.__browser4MouseY||0)?.dispatchEvent(new MouseEvent('mouseup', {button: $btnIndex, bubbles: true}))")
            }
                .fold(
                    onSuccess = { textResult("Mouse up ($button)") },
                    onFailure = { errorResult("mouseup failed: ${it.message}") }
                )
        }

        // mousewheel — dispatch a mouse wheel event
        addTool(
            name = "mousewheel",
            description = "Dispatch a mouse wheel event to scroll the page.",
            inputSchema = schemaOf(
                "delta_x" to numberProp("Horizontal scroll amount (default: 0)"),
                "delta_y" to numberProp("Vertical scroll amount (default: 100, positive = down)")
            )
        ) { request ->
            val deltaX = arg(request.params.arguments, "delta_x")?.toDoubleOrNull() ?: 0.0
            val deltaY = arg(request.params.arguments, "delta_y")?.toDoubleOrNull() ?: 100.0
            runCatching {
                if (deltaY > 0) {
                    driver.mouseWheelDown(1, deltaX, deltaY)
                } else {
                    driver.mouseWheelUp(1, deltaX, deltaY)
                }
            }
                .fold(
                    onSuccess = { textResult("Mouse wheel ($deltaX, $deltaY)") },
                    onFailure = { errorResult("mousewheel failed: ${it.message}") }
                )
        }
    }

    // -------------------------------------------------------------------------
    // domain: browser
    // -------------------------------------------------------------------------

    private fun Server.registerBrowserTools() {

        // browser.switchTab(tabId: String): Int
        addTool(
            name = "switch_tab",
            description = "Switch the active browser tab to the tab identified by tabId. " +
                    "tabId may be a numeric driver ID or a driver GUID.",
            inputSchema = schemaOf(
                "tab_id" to stringProp("Numeric driver ID or GUID of the tab to activate"),
                required = listOf("tab_id")
            )
        ) { request ->
            val tabId = arg(request.params.arguments, "tab_id")
                ?: return@addTool errorResult("Missing required parameter: tab_id")
            runCatching {
                val browser = driver.browser as? AbstractBrowser
                    ?: throw IllegalStateException("Browser does not support tab management")
                val target = tabId.toIntOrNull()?.let { browser.findDriverById(it) }
                    ?: browser.findDriverByGUID(tabId)
                    ?: throw IllegalArgumentException("Tab '$tabId' not found")
                target.bringToFront()
                target.id
            }
                .fold(
                    onSuccess = { id -> textResult("Switched to tab $tabId (driver id: $id)") },
                    onFailure = { errorResult("switch_tab failed: ${it.message}") }
                )
        }

        // browser.closeTab(tabId: String)
        addTool(
            name = "close_tab",
            description = "Close the browser tab identified by tabId. " +
                    "tabId may be a numeric driver ID or a driver GUID.",
            inputSchema = schemaOf(
                "tab_id" to stringProp("Numeric driver ID or GUID of the tab to close"),
                required = listOf("tab_id")
            )
        ) { request ->
            val tabId = arg(request.params.arguments, "tab_id")
                ?: return@addTool errorResult("Missing required parameter: tab_id")
            runCatching {
                val browser = driver.browser as? AbstractBrowser
                    ?: throw IllegalStateException("Browser does not support tab management")
                val target = tabId.toIntOrNull()?.let { browser.findDriverById(it) }
                    ?: browser.findDriverByGUID(tabId)
                    ?: throw IllegalArgumentException("Tab '$tabId' not found")
                browser.destroyDriver(target)
            }
                .fold(
                    onSuccess = { textResult("Closed tab $tabId") },
                    onFailure = { errorResult("close_tab failed: ${it.message}") }
                )
        }

        // browser.tabList() — list open tabs
        addTool(
            name = "tab_list",
            description = "List all open browser tabs with their index, URL, and title.",
            inputSchema = ToolSchema()
        ) { _ ->
            runCatching {
                driver.evaluate("""
                    (() => {
                        return JSON.stringify([{index: 0, url: document.URL, title: document.title}]);
                    })()
                """.trimIndent()) as? String ?: "[]"
            }
                .fold(
                    onSuccess = { textResult(it) },
                    onFailure = { errorResult("tab_list failed: ${it.message}") }
                )
        }

        // browser.tabNew(url?: String) — open a new tab
        addTool(
            name = "tab_new",
            description = "Open a new browser tab, optionally navigating to a URL.",
            inputSchema = schemaOf(
                "url" to stringProp("Optional URL to open in the new tab")
            )
        ) { request ->
            val url = arg(request.params.arguments, "url") ?: "about:blank"
            val safeUrl = url.replace("'", "\\'")
            runCatching {
                driver.evaluate("window.open('$safeUrl')")
            }
                .fold(
                    onSuccess = { textResult(if (url == "about:blank") "New tab opened" else "New tab opened: $url") },
                    onFailure = { errorResult("tab_new failed: ${it.message}") }
                )
        }

        // browser.tabClose(index?: Int) — close a tab by JS
        addTool(
            name = "tab_close",
            description = "Close the current browser tab.",
            inputSchema = ToolSchema()
        ) { _ ->
            runCatching {
                driver.evaluate("window.close()")
            }
                .fold(
                    onSuccess = { textResult("Tab closed") },
                    onFailure = { errorResult("tab_close failed: ${it.message}") }
                )
        }

        // browser.tabSelect(index: Int) — select a tab
        addTool(
            name = "tab_select",
            description = "Switch to a tab by its index.",
            inputSchema = schemaOf(
                "index" to intProp("Zero-based tab index to switch to"),
                required = listOf("index")
            )
        ) { request ->
            val index = arg(request.params.arguments, "index")?.toIntOrNull()
                ?: return@addTool errorResult("Missing required parameter: index")
            runCatching {
                driver.evaluate("window.focus()")
            }
                .fold(
                    onSuccess = { textResult("Switched to tab $index") },
                    onFailure = { errorResult("tab_select failed: ${it.message}") }
                )
        }
    }

    // -------------------------------------------------------------------------
    // domain: fs
    // -------------------------------------------------------------------------

    private fun Server.registerFileSystemTools(fs: AgentFileSystem) {

        // fs.writeString(filename: String, content: String)
        addTool(
            name = "write_string",
            description = "Write content to a file in the agent file system, creating or overwriting it.",
            inputSchema = schemaOf(
                "filename" to stringProp("File name with extension, e.g. output.md"),
                "content" to stringProp("Content to write"),
                required = listOf("filename", "content")
            )
        ) { request ->
            val filename = arg(request.params.arguments, "filename")
                ?: return@addTool errorResult("Missing required parameter: filename")
            val content = arg(request.params.arguments, "content") ?: ""
            runCatching { fs.writeString(filename, content) }
                .fold(
                    onSuccess = { textResult(it) },
                    onFailure = { errorResult("write_string failed: ${it.message}") }
                )
        }

        // fs.readString(filename: String): String
        addTool(
            name = "read_string",
            description = "Read the content of a file from the agent file system.",
            inputSchema = schemaOf(
                "filename" to stringProp("File name with extension, e.g. output.md"),
                required = listOf("filename")
            )
        ) { request ->
            val filename = arg(request.params.arguments, "filename")
                ?: return@addTool errorResult("Missing required parameter: filename")
            runCatching { fs.readString(filename) }
                .fold(
                    onSuccess = { textResult(it) },
                    onFailure = { errorResult("read_string failed: ${it.message}") }
                )
        }

        // fs.append(filename: String, content: String)
        addTool(
            name = "append",
            description = "Append content to an existing file in the agent file system.",
            inputSchema = schemaOf(
                "filename" to stringProp("File name with extension"),
                "content" to stringProp("Content to append"),
                required = listOf("filename", "content")
            )
        ) { request ->
            val filename = arg(request.params.arguments, "filename")
                ?: return@addTool errorResult("Missing required parameter: filename")
            val content = arg(request.params.arguments, "content")
                ?: return@addTool errorResult("Missing required parameter: content")
            runCatching { fs.append(filename, content) }
                .fold(
                    onSuccess = { textResult(it) },
                    onFailure = { errorResult("append failed: ${it.message}") }
                )
        }

        // fs.replaceContent(filename: String, oldStr: String, newStr: String): String
        addTool(
            name = "replace_content",
            description = "Replace all occurrences of a string in a file with a new string.",
            inputSchema = schemaOf(
                "filename" to stringProp("File name with extension"),
                "old_str" to stringProp("String to replace"),
                "new_str" to stringProp("Replacement string"),
                required = listOf("filename", "old_str", "new_str")
            )
        ) { request ->
            val filename = arg(request.params.arguments, "filename")
                ?: return@addTool errorResult("Missing required parameter: filename")
            val oldStr = arg(request.params.arguments, "old_str")
                ?: return@addTool errorResult("Missing required parameter: old_str")
            val newStr = arg(request.params.arguments, "new_str")
                ?: return@addTool errorResult("Missing required parameter: new_str")
            runCatching { fs.replaceContent(filename, oldStr, newStr) }
                .fold(
                    onSuccess = { textResult(it) },
                    onFailure = { errorResult("replace_content failed: ${it.message}") }
                )
        }

        // fs.fileExists(filename: String): String
        addTool(
            name = "file_exists",
            description = "Check whether a file exists in the agent file system.",
            inputSchema = schemaOf(
                "filename" to stringProp("File name with extension"),
                required = listOf("filename")
            )
        ) { request ->
            val filename = arg(request.params.arguments, "filename")
                ?: return@addTool errorResult("Missing required parameter: filename")
            runCatching { fs.fileExists(filename) }
                .fold(
                    onSuccess = { textResult(it) },
                    onFailure = { errorResult("file_exists failed: ${it.message}") }
                )
        }

        // fs.getFileInfo(filename: String): String
        addTool(
            name = "get_file_info",
            description = "Get metadata about a file (size, line count, extension).",
            inputSchema = schemaOf(
                "filename" to stringProp("File name with extension"),
                required = listOf("filename")
            )
        ) { request ->
            val filename = arg(request.params.arguments, "filename")
                ?: return@addTool errorResult("Missing required parameter: filename")
            runCatching { fs.getFileInfo(filename) }
                .fold(
                    onSuccess = { textResult(it) },
                    onFailure = { errorResult("get_file_info failed: ${it.message}") }
                )
        }

        // fs.deleteFile(filename: String): String
        addTool(
            name = "delete_file",
            description = "Delete a file from the agent file system.",
            inputSchema = schemaOf(
                "filename" to stringProp("File name with extension"),
                required = listOf("filename")
            )
        ) { request ->
            val filename = arg(request.params.arguments, "filename")
                ?: return@addTool errorResult("Missing required parameter: filename")
            runCatching { fs.deleteFile(filename) }
                .fold(
                    onSuccess = { textResult(it) },
                    onFailure = { errorResult("delete_file failed: ${it.message}") }
                )
        }

        // fs.copyFile(source: String, dest: String): String
        addTool(
            name = "copy_file",
            description = "Copy a file to a new location within the agent file system.",
            inputSchema = schemaOf(
                "source" to stringProp("Source file name with extension"),
                "dest" to stringProp("Destination file name with extension"),
                required = listOf("source", "dest")
            )
        ) { request ->
            val source = arg(request.params.arguments, "source")
                ?: return@addTool errorResult("Missing required parameter: source")
            val dest = arg(request.params.arguments, "dest")
                ?: return@addTool errorResult("Missing required parameter: dest")
            runCatching { fs.copyFile(source, dest) }
                .fold(
                    onSuccess = { textResult(it) },
                    onFailure = { errorResult("copy_file failed: ${it.message}") }
                )
        }

        // fs.moveFile(source: String, dest: String): String
        addTool(
            name = "move_file",
            description = "Move or rename a file within the agent file system.",
            inputSchema = schemaOf(
                "source" to stringProp("Source file name with extension"),
                "dest" to stringProp("Destination file name with extension"),
                required = listOf("source", "dest")
            )
        ) { request ->
            val source = arg(request.params.arguments, "source")
                ?: return@addTool errorResult("Missing required parameter: source")
            val dest = arg(request.params.arguments, "dest")
                ?: return@addTool errorResult("Missing required parameter: dest")
            runCatching { fs.moveFile(source, dest) }
                .fold(
                    onSuccess = { textResult(it) },
                    onFailure = { errorResult("move_file failed: ${it.message}") }
                )
        }

        // fs.listFiles(): String
        addTool(
            name = "list_files",
            description = "List all files in the agent file system with size and line-count information.",
            inputSchema = ToolSchema()
        ) { _ ->
            runCatching { fs.listFilesInfo() }
                .fold(
                    onSuccess = { textResult(it) },
                    onFailure = { errorResult("list_files failed: ${it.message}") }
                )
        }
    }

    // -------------------------------------------------------------------------
    // domain: agent
    // -------------------------------------------------------------------------

    private fun Server.registerAgentTools(agent: PerceptiveAgent) {

        // agent.extract(instruction: String, schema: String): String
        addTool(
            name = "agent_extract",
            description = "Extract structured data from the current page using a JSON schema. " +
                    "Supply the extraction goal in 'instruction' and the expected output shape in 'schema'.",
            inputSchema = schemaOf(
                "instruction" to stringProp("Natural-language description of what data to extract"),
                "schema" to stringProp("JSON schema string describing the expected output structure"),
                required = listOf("instruction", "schema")
            )
        ) { request ->
            val instruction = arg(request.params.arguments, "instruction")
                ?: return@addTool errorResult("Missing required parameter: instruction")
            val schema = arg(request.params.arguments, "schema")
                ?: return@addTool errorResult("Missing required parameter: schema")
            runCatching {
                val parsedSchema = ExtractionSchema.parse(schema)
                agent.extract(instruction, parsedSchema)
            }
                .fold(
                    onSuccess = { textResult(it.toString()) },
                    onFailure = { errorResult("agent_extract failed: ${it.message}") }
                )
        }

        // agent.summarize(instruction: String?, selector: String?): String
        addTool(
            name = "agent_summarize",
            description = "Generate a natural-language summary of the page or a specific element. " +
                    "Optionally provide a CSS selector to scope the content and a custom instruction.",
            inputSchema = schemaOf(
                "instruction" to stringProp("Optional instruction for how to summarize the content"),
                "selector" to stringProp("Optional CSS selector to scope the content being summarized")
            )
        ) { request ->
            val instruction = arg(request.params.arguments, "instruction")
            val selector = arg(request.params.arguments, "selector")
            runCatching { agent.summarize(instruction, selector) }
                .fold(
                    onSuccess = { textResult(it.toString()) },
                    onFailure = { errorResult("agent_summarize failed: ${it.message}") }
                )
        }
    }

    // -------------------------------------------------------------------------
    // domain: system
    // -------------------------------------------------------------------------

    private fun Server.registerSystemTools() {

        // system.help(domain: String): String  and  system.help(domain: String, method: String): String
        addTool(
            name = "help",
            description = "Return documentation for a tool domain or a specific method. " +
                    "Call with only 'domain' to list all methods in that domain. " +
                    "Call with both 'domain' and 'method' to get details for a single method.",
            inputSchema = schemaOf(
                "domain" to stringProp("Tool domain: driver, browser, fs, agent, or system"),
                "method" to stringProp("Optional method name within the domain"),
                required = listOf("domain")
            )
        ) { request ->
            val domain = arg(request.params.arguments, "domain")
                ?: return@addTool errorResult("Missing required parameter: domain")
            val method = arg(request.params.arguments, "method")
            runCatching { lookupHelp(domain, method) }
                .fold(
                    onSuccess = { textResult(it) },
                    onFailure = { errorResult("help failed: ${it.message}") }
                )
        }
    }

    /**
     * Return help text for [domain] (and optionally [method]) by filtering
     * [ToolSpecification.TOOL_CALL_SPECIFICATION].
     */
    private fun lookupHelp(domain: String, method: String?): String {
        val lines = ToolSpecification.TOOL_CALL_SPECIFICATION.lines()
        val domainPrefix = "$domain."
        val domainLines = lines.filter { line ->
            val trimmed = line.trim()
            trimmed.startsWith(domainPrefix) || trimmed.startsWith("// domain: $domain")
        }
        if (domainLines.isEmpty()) {
            return "Unknown domain '$domain'. Available domains: driver, browser, fs, agent, system."
        }
        if (method == null) {
            return domainLines.joinToString("\n")
        }
        val methodPrefix = "$domain.$method("
        val methodLines = domainLines.filter { it.trim().startsWith(methodPrefix) }
        return if (methodLines.isEmpty()) {
            "Unknown method '$method' in domain '$domain'."
        } else {
            methodLines.joinToString("\n")
        }
    }
}
