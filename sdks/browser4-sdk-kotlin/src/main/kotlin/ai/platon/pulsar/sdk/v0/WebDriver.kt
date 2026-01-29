@file:Suppress("UNUSED")
package ai.platon.pulsar.sdk.v0

import ai.platon.pulsar.sdk.v0.detail.PulsarClient
import java.util.*

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor
 * license agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership. The ASF licenses this file to
 * you under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required
 * by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
/**
 * WebDriver-compatible façade mapping to selector-first REST endpoints.
 *
 * This class provides methods for browser control and automation through
 * the Browser4 REST API. It supports:
 * - Navigation: navigate_to, current_url, go_back, go_forward, reload
 * - Element interaction: click, fill, type, press, hover, focus
 * - Scrolling: scrollDown, scrollUp, scrollTo, scrollToBottom, scrollToTop
 * - Selection: exists, waitForSelector, selectFirstText, selectTextAll
 * - Screenshots: captureScreenshot
 * - Script execution: evaluate, executeScript
 * - Control: delay, pause, stop
 *
 * Example usage:
 * ```kotlin
 * val client = PulsarClient()
 * client.createSession()
 * val driver = WebDriver(client)
 * driver.navigateTo("https://example.com")
 * println(driver.currentUrl())
 * driver.click("button.submit")
 * ```
 *
 * @param client PulsarClient instance for API communication
 */
class WebDriver(
    val client: PulsarClient
) {
    private var _id: Int = 0
    private val _navigateHistory: MutableList<String> = Collections.synchronizedList(ArrayList())

    /**
     * Gets the driver ID.
     */
    val id: Int get() = _id

    /**
     * Gets the navigation history.
     */
    val navigateHistory: List<String> get() = _navigateHistory.toList()

    // ========== Navigation ==========

    /**
     * Opens the specified URL and waits for navigation to complete.
     *
     * @param url The URL to navigate to
     */
    suspend fun open(url: String) {
        navigateTo(url)
    }

    /**
     * Navigates to a URL.
     *
     * @param url The URL to navigate to
     * @return Navigation result
     */
    suspend fun navigateTo(url: String): Any? {
        val result = client.post("/session/{sessionId}/url", mapOf("url" to url))
        _navigateHistory.add(url)
        return result
    }

    /**
     * Reloads the current page.
     *
     * @return Reload result
     */
    suspend fun reload(): Any? {
        return client.post("/session/{sessionId}/reload", emptyMap())
    }

    /**
     * Navigates back in browser history.
     *
     * @return Navigation result
     */
    suspend fun goBack(): Any? {
        return client.post("/session/{sessionId}/back", emptyMap())
    }

    /**
     * Navigates forward in browser history.
     *
     * @return Navigation result
     */
    suspend fun goForward(): Any? {
        return client.post("/session/{sessionId}/forward", emptyMap())
    }

    /**
     * Gets the current URL displayed in the address bar.
     *
     * @return The current URL as a string
     */
    suspend fun currentUrl(): String {
        return client.get("/session/{sessionId}/url") as? String ?: ""
    }

    /**
     * Alias for [currentUrl].
     */
    suspend fun getCurrentUrl(): String = currentUrl()

    /**
     * Gets the document URL property.
     *
     * @return The document URL
     */
    suspend fun url(): String = currentUrl()

    /**
     * Gets the document.documentURI property.
     *
     * @return The document URI
     */
    suspend fun documentUri(): String {
        return client.get("/session/{sessionId}/documentUri") as? String ?: ""
    }

    /**
     * Alias for [documentUri].
     */
    suspend fun getDocumentUri(): String = documentUri()

    /**
     * Gets the document.baseURI property.
     *
     * @return The base URI
     */
    suspend fun baseUri(): String {
        return client.get("/session/{sessionId}/baseUri") as? String ?: ""
    }

    /**
     * Alias for [baseUri].
     */
    suspend fun getBaseUri(): String = baseUri()

    /**
     * Gets the current page title.
     *
     * @return The page title
     */
    suspend fun title(): String {
        return client.get("/session/{sessionId}/title") as? String ?: ""
    }

    /**
     * Gets the source code of the current page.
     *
     * @return The page HTML source or null
     */
    suspend fun pageSource(): String? {
        return outerHtml()
    }

    // ========== Status Checking ==========

    /**
     * Checks if an element exists in the DOM.
     *
     * @param selector CSS selector or XPath expression
     * @param strategy Selector strategy ("css" or "xpath")
     * @return True if the element exists
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun exists(selector: String, strategy: String = "css"): Boolean {
        val value = client.post(
            "/session/{sessionId}/selectors/exists",
            mapOf("selector" to selector, "strategy" to strategy)
        )
        return when (value) {
            is Map<*, *> -> (value as Map<String, Any?>)["exists"] as? Boolean ?: false
            is Boolean -> value
            else -> false
        }
    }

    /**
     * Checks if an element is visible.
     *
     * @param selector CSS selector
     * @return True if the element is visible
     */
    suspend fun isVisible(selector: String): Boolean {
        val value = client.post(
            "/session/{sessionId}/selectors/isVisible",
            mapOf("selector" to selector, "strategy" to "css")
        )
        return when (value) {
            is Map<*, *> -> (value as? Map<String, Any?>)?.get("value") as? Boolean ?: false
            is Boolean -> value
            else -> false
        }
    }

    /**
     * Checks if an element is hidden.
     *
     * @param selector CSS selector
     * @return True if the element is hidden
     */
    suspend fun isHidden(selector: String): Boolean = !isVisible(selector)

    /**
     * Checks if a checkbox/radio element is checked.
     *
     * @param selector CSS selector
     * @return True if the element is checked
     */
    suspend fun isChecked(selector: String): Boolean {
        val value = client.post(
            "/session/{sessionId}/selectors/isChecked",
            mapOf("selector" to selector, "strategy" to "css")
        )
        return when (value) {
            is Map<*, *> -> (value as? Map<String, Any?>)?.get("value") as? Boolean ?: false
            is Boolean -> value
            else -> false
        }
    }

    // ========== Wait Operations ==========

    /**
     * Waits for an element to appear in the DOM.
     *
     * @param selector CSS selector or XPath expression
     * @param strategy Selector strategy ("css" or "xpath")
     * @param timeout Maximum wait time in milliseconds
     * @return True if the element was found before timeout
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun waitForSelector(selector: String, strategy: String = "css", timeout: Int = 30000): Boolean {
        val value = client.post(
            "/session/{sessionId}/selectors/waitFor",
            mapOf("selector" to selector, "strategy" to strategy, "timeout" to timeout)
        )
        return when (value) {
            null -> true
            is Map<*, *> -> (value as Map<String, Any?>)["exists"] as? Boolean ?: true
            is Boolean -> value
            else -> true
        }
    }

    /**
     * Alias for [waitForSelector].
     */
    suspend fun waitFor(selector: String, strategy: String = "css", timeout: Int = 30000): Boolean {
        return waitForSelector(selector, strategy, timeout)
    }

    /**
     * Waits for navigation to complete (URL change).
     *
     * @param oldUrl The previous URL to compare against
     * @param timeout Maximum wait time in milliseconds
     * @return True if navigation completed
     */
    suspend fun waitForNavigation(oldUrl: String = "", timeout: Int = 30000): Boolean {
        client.post("/session/{sessionId}/control/delay", mapOf("ms" to minOf(timeout, 1000)))
        return true
    }

    // ========== Element Finding ==========

    /**
     * Finds a single element by selector.
     *
     * @param selector CSS selector or XPath expression
     * @param strategy Selector strategy
     * @return Element reference map
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun findElementBySelector(selector: String, strategy: String = "css"): Map<String, Any?>? {
        return client.post(
            "/session/{sessionId}/selectors/element",
            mapOf("selector" to selector, "strategy" to strategy)
        ) as? Map<String, Any?>
    }

    /**
     * Finds all elements matching a selector.
     *
     * @param selector CSS selector or XPath expression
     * @param strategy Selector strategy
     * @return List of element reference maps
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun findElementsBySelector(selector: String, strategy: String = "css"): List<Map<String, Any?>> {
        val result = client.post(
            "/session/{sessionId}/selectors/elements",
            mapOf("selector" to selector, "strategy" to strategy)
        )
        return result as? List<Map<String, Any?>> ?: emptyList()
    }

    /**
     * Finds element using WebDriver locator strategy.
     *
     * @param using Locator strategy (e.g., "css selector", "xpath")
     * @param value Locator value
     * @return Element reference map
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun findElement(using: String, value: String): Map<String, Any?>? {
        return client.post(
            "/session/{sessionId}/element",
            mapOf("using" to using, "value" to value)
        ) as? Map<String, Any?>
    }

    /**
     * Finds elements using WebDriver locator strategy.
     *
     * @param using Locator strategy
     * @param value Locator value
     * @return List of element reference maps
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun findElements(using: String, value: String): List<Map<String, Any?>> {
        val result = client.post(
            "/session/{sessionId}/elements",
            mapOf("using" to using, "value" to value)
        )
        return result as? List<Map<String, Any?>> ?: emptyList()
    }

    // ========== Element Interaction ==========

    /**
     * Clicks an element identified by selector.
     *
     * @param selector CSS selector or XPath expression
     * @param count Number of clicks (for double-click, use 2)
     * @param strategy Selector strategy
     * @return Click result
     */
    suspend fun click(selector: String, count: Int = 1, strategy: String = "css"): Any? {
        return client.post(
            "/session/{sessionId}/selectors/click",
            mapOf("selector" to selector, "strategy" to strategy)
        )
    }

    /**
     * Clicks an element by its ID.
     *
     * @param elementId WebDriver element ID
     * @return Click result
     */
    suspend fun clickElement(elementId: String): Any? {
        return client.post("/session/{sessionId}/element/$elementId/click", emptyMap())
    }

    /**
     * Hovers over an element.
     *
     * @param selector CSS selector
     * @return Hover result
     */
    suspend fun hover(selector: String): Any? {
        return client.post(
            "/session/{sessionId}/selectors/hover",
            mapOf("selector" to selector, "strategy" to "css")
        )
    }

    /**
     * Focuses on an element.
     *
     * @param selector CSS selector
     * @return Focus result
     */
    suspend fun focus(selector: String): Any? {
        return client.post(
            "/session/{sessionId}/selectors/focus",
            mapOf("selector" to selector, "strategy" to "css")
        )
    }

    /**
     * Types text into an element (appending to existing content).
     *
     * @param selector CSS selector
     * @param text Text to type
     * @return Type result
     */
    suspend fun type(selector: String, text: String): Any? {
        return fill(selector, text)
    }

    /**
     * Fills an input element with text (clearing existing content first).
     *
     * @param selector CSS selector or XPath expression
     * @param text Text to fill
     * @param strategy Selector strategy
     * @return Fill result
     */
    suspend fun fill(selector: String, text: String, strategy: String = "css"): Any? {
        return client.post(
            "/session/{sessionId}/selectors/fill",
            mapOf("selector" to selector, "strategy" to strategy, "value" to text)
        )
    }

    /**
     * Presses a key on an element.
     *
     * @param selector CSS selector or XPath expression
     * @param key Key to press (e.g., "Enter", "Tab")
     * @param strategy Selector strategy
     * @return Press result
     */
    suspend fun press(selector: String, key: String, strategy: String = "css"): Any? {
        return client.post(
            "/session/{sessionId}/selectors/press",
            mapOf("selector" to selector, "strategy" to strategy, "key" to key)
        )
    }

    /**
     * Sends keys to an element.
     *
     * @param elementId WebDriver element ID
     * @param text Text to send
     * @return Send keys result
     */
    suspend fun sendKeys(elementId: String, text: String): Any? {
        return client.post(
            "/session/{sessionId}/element/$elementId/value",
            mapOf("text" to text)
        )
    }

    /**
     * Checks a checkbox element.
     *
     * @param selector CSS selector
     * @return Check result
     */
    suspend fun check(selector: String): Any? {
        return client.post(
            "/session/{sessionId}/selectors/check",
            mapOf("selector" to selector, "strategy" to "css")
        )
    }

    /**
     * Unchecks a checkbox element.
     *
     * @param selector CSS selector
     * @return Uncheck result
     */
    suspend fun uncheck(selector: String): Any? {
        return client.post(
            "/session/{sessionId}/selectors/uncheck",
            mapOf("selector" to selector, "strategy" to "css")
        )
    }

    // ========== Scrolling ==========

    /**
     * Scrolls down the page.
     *
     * @param count Number of scroll actions
     * @return Current scroll position
     */
    suspend fun scrollDown(count: Int = 1): Double {
        val value = client.post(
            "/session/{sessionId}/scroll/down",
            mapOf("count" to count)
        )
        return when (value) {
            is Map<*, *> -> ((value as? Map<String, Any?>)?.get("value") as? Number)?.toDouble() ?: 0.0
            is Number -> value.toDouble()
            else -> 0.0
        }
    }

    /**
     * Scrolls up the page.
     *
     * @param count Number of scroll actions
     * @return Current scroll position
     */
    suspend fun scrollUp(count: Int = 1): Double {
        val value = client.post(
            "/session/{sessionId}/scroll/up",
            mapOf("count" to count)
        )
        return when (value) {
            is Map<*, *> -> ((value as? Map<String, Any?>)?.get("value") as? Number)?.toDouble() ?: 0.0
            is Number -> value.toDouble()
            else -> 0.0
        }
    }

    /**
     * Scrolls an element into view.
     *
     * @param selector CSS selector of the element
     * @return Current scroll position
     */
    suspend fun scrollTo(selector: String): Double {
        val value = client.post(
            "/session/{sessionId}/scroll/to",
            mapOf("selector" to selector, "strategy" to "css")
        )
        return when (value) {
            is Map<*, *> -> ((value as? Map<String, Any?>)?.get("value") as? Number)?.toDouble() ?: 0.0
            is Number -> value.toDouble()
            else -> 0.0
        }
    }

    /**
     * Scrolls to the top of the page.
     *
     * @return Current scroll position (0)
     */
    suspend fun scrollToTop(): Double {
        val value = client.post("/session/{sessionId}/scroll/top", emptyMap())
        return when (value) {
            is Map<*, *> -> ((value as? Map<String, Any?>)?.get("value") as? Number)?.toDouble() ?: 0.0
            is Number -> value.toDouble()
            else -> 0.0
        }
    }

    /**
     * Scrolls to the bottom of the page.
     *
     * @return Current scroll position
     */
    suspend fun scrollToBottom(): Double {
        val value = client.post("/session/{sessionId}/scroll/bottom", emptyMap())
        return when (value) {
            is Map<*, *> -> ((value as? Map<String, Any?>)?.get("value") as? Number)?.toDouble() ?: 0.0
            is Number -> value.toDouble()
            else -> 0.0
        }
    }

    /**
     * Scrolls to a specific position on the page.
     *
     * @param ratio Scroll ratio (0.0 = top, 1.0 = bottom)
     * @return Current scroll position
     */
    suspend fun scrollToMiddle(ratio: Double = 0.5): Double {
        val value = client.post(
            "/session/{sessionId}/scroll/middle",
            mapOf("ratio" to ratio)
        )
        return when (value) {
            is Map<*, *> -> ((value as? Map<String, Any?>)?.get("value") as? Number)?.toDouble() ?: 0.0
            is Number -> value.toDouble()
            else -> 0.0
        }
    }

    /**
     * Scrolls by a specific number of pixels.
     *
     * @param pixels Pixels to scroll (positive = down, negative = up)
     * @param smooth Whether to use smooth scrolling
     * @return Current scroll position
     */
    suspend fun scrollBy(pixels: Double = 200.0, smooth: Boolean = true): Double {
        val value = client.post(
            "/session/{sessionId}/scroll/by",
            mapOf("pixels" to pixels, "smooth" to smooth)
        )
        return when (value) {
            is Map<*, *> -> ((value as? Map<String, Any?>)?.get("value") as? Number)?.toDouble() ?: 0.0
            is Number -> value.toDouble()
            else -> 0.0
        }
    }

    // ========== Content Extraction ==========

    /**
     * Gets the outer HTML of an element or the entire document.
     *
     * @param selector CSS selector (optional, if null returns document HTML)
     * @param strategy Selector strategy
     * @return HTML content or null
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun outerHtml(selector: String? = null, strategy: String = "css"): String? {
        return if (selector != null) {
            val value = client.post(
                "/session/{sessionId}/selectors/outerHtml",
                mapOf("selector" to selector, "strategy" to strategy)
            )
            when (value) {
                is Map<*, *> -> (value as Map<String, Any?>)["value"] as? String
                is String -> value
                else -> null
            }
        } else {
            // For full document HTML, request with "html" or "body" selector
            val value = client.post(
                "/session/{sessionId}/selectors/outerHtml",
                mapOf("selector" to "html", "strategy" to strategy)
            )
            when (value) {
                is Map<*, *> -> (value as Map<String, Any?>)["value"] as? String
                is String -> value
                else -> null
            }
        }
    }

    /**
     * Gets the text content of an element or document.
     *
     * @param selector CSS selector (optional)
     * @return Text content or null
     */
    suspend fun textContent(selector: String? = null): String? {
        return if (selector != null) {
            selectFirstTextOrNull(selector)
        } else {
            // For document text content, use outerHtml endpoint to get full page
            // and extract text, or use a new endpoint
            selectFirstTextOrNull("body")
        }
    }

    /**
     * Gets the text content of the first element matching the selector.
     *
     * @param selector CSS selector
     * @return Text content or null if not found
     */
    suspend fun selectFirstTextOrNull(selector: String): String? {
        val value = client.post(
            "/session/{sessionId}/selectors/textContent",
            mapOf("selector" to selector, "strategy" to "css")
        )
        return when (value) {
            is Map<*, *> -> (value as? Map<String, Any?>)?.get("value") as? String
            is String -> value
            else -> null
        }
    }

    /**
     * Gets text content of all elements matching the selector.
     *
     * @param selector CSS selector
     * @return List of text contents
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun selectTextAll(selector: String): List<String> {
        val value = client.post(
            "/session/{sessionId}/selectors/textContentAll",
            mapOf("selector" to selector, "strategy" to "css")
        )
        return when (value) {
            is Map<*, *> -> {
                val list = (value as? Map<String, Any?>)?.get("value") as? List<*>
                list?.mapNotNull { it as? String } ?: emptyList()
            }
            is List<*> -> value.mapNotNull { it as? String }
            else -> emptyList()
        }
    }

    /**
     * Gets an attribute value of the first element matching the selector.
     *
     * @param selector CSS selector
     * @param attrName Attribute name
     * @return Attribute value or null
     */
    suspend fun selectFirstAttributeOrNull(selector: String, attrName: String): String? {
        val value = client.post(
            "/session/{sessionId}/selectors/attribute",
            mapOf("selector" to selector, "strategy" to "css", "attrName" to attrName)
        )
        return when (value) {
            is Map<*, *> -> (value as? Map<String, Any?>)?.get("value") as? String
            is String -> value
            else -> null
        }
    }

    /**
     * Gets attribute values of all elements matching the selector.
     *
     * @param selector CSS selector
     * @param attrName Attribute name
     * @return List of attribute values
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun selectAttributeAll(selector: String, attrName: String): List<String> {
        val value = client.post(
            "/session/{sessionId}/selectors/attributeAll",
            mapOf("selector" to selector, "strategy" to "css", "attrName" to attrName)
        )
        return when (value) {
            is Map<*, *> -> {
                val list = (value as? Map<String, Any?>)?.get("value") as? List<*>
                list?.mapNotNull { it as? String } ?: emptyList()
            }
            is List<*> -> value.mapNotNull { it as? String }
            else -> emptyList()
        }
    }

    /**
     * Gets an attribute of an element by ID.
     *
     * @param elementId WebDriver element ID
     * @param name Attribute name
     * @return Attribute value
     */
    suspend fun getAttribute(elementId: String, name: String): Any? {
        return client.get("/session/{sessionId}/element/$elementId/attribute/$name")
    }

    /**
     * Gets the text content of an element by ID.
     *
     * @param elementId WebDriver element ID
     * @return Text content
     */
    suspend fun getText(elementId: String): String {
        return client.get("/session/{sessionId}/element/$elementId/text") as? String ?: ""
    }

    /**
     * Extracts multiple fields using CSS selectors.
     *
     * @param fields Map of field names to CSS selectors
     * @return Map of field names to extracted values
     */
    suspend fun extract(fields: Map<String, String>): Map<String, String?> {
        return fields.mapValues { (_, selector) -> selectFirstTextOrNull(selector) }
    }

    // ========== Screenshots ==========

    /**
     * Takes a screenshot.
     *
     * @param selector CSS selector for element screenshot (optional)
     * @param fullPage Whether to capture the full page
     * @return Base64-encoded screenshot or null
     */
    suspend fun captureScreenshot(selector: String? = null, fullPage: Boolean = false): String? {
        // Use "body" as default selector for full page screenshots
        val effectiveSelector = selector ?: "body"
        return screenshot(effectiveSelector)
    }

    /**
     * Takes a screenshot (alias for [captureScreenshot]).
     *
     * @param selector CSS selector (optional)
     * @param strategy Selector strategy
     * @return Base64-encoded screenshot or null
     */
    suspend fun screenshot(selector: String? = null, strategy: String = "css"): String? {
        val effectiveSelector = selector ?: "body"
        val payload = mapOf(
            "selector" to effectiveSelector,
            "strategy" to strategy
        )
        return client.post("/session/{sessionId}/selectors/screenshot", payload) as? String
    }

    // ========== Script Execution ==========

    /**
     * Executes JavaScript and returns the result.
     *
     * @param expression JavaScript expression to evaluate
     * @return Evaluation result
     */
    suspend fun evaluate(expression: String): Any? {
        return executeScript("return $expression")
    }

    /**
     * Executes synchronous JavaScript.
     *
     * @param script JavaScript code to execute
     * @param args Arguments to pass to the script
     * @return Script return value
     */
    suspend fun executeScript(script: String, args: List<Any?>? = null): Any? {
        return client.post(
            "/session/{sessionId}/execute/sync",
            mapOf("script" to script, "args" to (args ?: emptyList<Any?>()))
        )
    }

    /**
     * Executes asynchronous JavaScript.
     *
     * @param script JavaScript code to execute
     * @param args Arguments to pass to the script
     * @param timeout Execution timeout in milliseconds
     * @return Script return value
     */
    suspend fun executeAsyncScript(script: String, args: List<Any?>? = null, timeout: Int = 30000): Any? {
        return client.post(
            "/session/{sessionId}/execute/async",
            mapOf("script" to script, "args" to (args ?: emptyList<Any?>()), "timeout" to timeout)
        )
    }

    // ========== Control ==========

    /**
     * Delays execution for a specified time.
     *
     * @param millis Delay in milliseconds
     * @return Delay result
     */
    suspend fun delay(millis: Int): Any? {
        return client.post("/session/{sessionId}/control/delay", mapOf("ms" to millis))
    }

    /**
     * Pauses the session execution.
     *
     * @return Pause result
     */
    suspend fun pause(): Any? {
        return client.post("/session/{sessionId}/control/pause", emptyMap())
    }

    /**
     * Stops the session execution.
     *
     * @return Stop result
     */
    suspend fun stop(): Any? {
        return client.post("/session/{sessionId}/control/stop", emptyMap())
    }

    /**
     * Brings the browser window to the front.
     *
     * @return Result
     */
    suspend fun bringToFront(): Any? {
        return client.post("/session/{sessionId}/bringToFront", emptyMap())
    }

    // ========== Events (Placeholder) ==========

    /**
     * Creates an event configuration.
     *
     * @param config Event configuration map
     * @return Created config response
     */
    suspend fun createEventConfig(config: Map<String, Any>): Any? {
        return client.post("/session/{sessionId}/event-configs", config)
    }

    /**
     * Lists all event configurations.
     *
     * @return List of event configurations
     */
    suspend fun listEventConfigs(): Any? {
        return client.get("/session/{sessionId}/event-configs")
    }

    /**
     * Gets captured events.
     *
     * @return List of events
     */
    suspend fun getEvents(): Any? {
        return client.get("/session/{sessionId}/events")
    }

    /**
     * Subscribes to events.
     *
     * @param subscribeRequest Subscription request map
     * @return Subscription response
     */
    suspend fun subscribeEvents(subscribeRequest: Map<String, Any>): Any? {
        return client.post("/session/{sessionId}/events/subscribe", subscribeRequest)
    }

    // ========== Convenience Methods ==========

    /**
     * Closes the driver (cleanup).
     */
    fun close() {
        // No specific cleanup needed for REST-based driver
    }
}
