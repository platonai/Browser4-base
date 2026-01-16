package ai.platon.pulsar.sdk

/**
 * PulsarSession provides methods for loading pages from storage or internet,
 * parsing them, and extracting data.
 *
 * This class provides a consistent API for web scraping and data extraction tasks
 * through the Browser4 REST API.
 *
 * Key methods:
 * - [open]: Open a URL immediately (bypass cache)
 * - [load]: Load from cache or fetch from internet
 * - [submit]: Submit URL to crawl pool for async processing
 * - [normalize]: Normalize a URL with load arguments
 * - [parse]: Parse a page into a document
 * - [extract]: Extract fields from a document
 * - [scrape]: Load, parse, and extract in one operation
 *
 * Example usage:
 * ```kotlin
 * val client = PulsarClient()
 * client.createSession()
 * val session = PulsarSession(client)
 * val page = session.load("https://example.com", "-expire 1d")
 * val fields = session.extract(page, mapOf("title" to "h1"))
 * session.close()
 * ```
 *
 * @param client PulsarClient instance for API communication
 */
open class PulsarSession(
    val client: PulsarClient
) : AutoCloseable {

    private var _driver: WebDriver? = null
    private var _id: Int = 0

    /**
     * Gets the session ID (numeric).
     */
    val id: Int get() = _id

    /**
     * Gets the session UUID.
     */
    val uuid: String get() = client.sessionId ?: ""

    /**
     * Gets a short descriptive display text.
     */
    val display: String
        get() = if (uuid.isNotEmpty()) "PulsarSession(${uuid.take(8)}...)" else "PulsarSession(no-session)"

    /**
     * Checks if the session is active.
     */
    val isActive: Boolean get() = client.sessionId != null

    /**
     * Gets the bound WebDriver instance.
     */
    val driver: WebDriver
        get() {
            if (_driver == null) {
                _driver = WebDriver(client)
            }
            return _driver!!
        }

    /**
     * Gets the bound driver (or null if not bound).
     */
    val boundDriver: WebDriver? get() = _driver

    // ========== URL Normalization ==========

    /**
     * Normalizes a URL with optional load arguments.
     *
     * @param url The URL to normalize
     * @param args Optional load arguments (e.g., "-expire 1d")
     * @param toItemOption Whether to convert to item load options
     * @return [NormURL] with normalized URL and parsed arguments
     */
    @Suppress("UNCHECKED_CAST")
    fun normalize(url: String, args: String? = null, toItemOption: Boolean = false): NormURL {
        val payload = mutableMapOf<String, Any?>("url" to url, "toItemOption" to toItemOption)
        if (args != null) {
            payload["args"] = args
        }
        val value = client.post("/session/{sessionId}/normalize", payload)
        return if (value is Map<*, *>) {
            NormURL.fromMap(value as Map<String, Any?>)
        } else {
            NormURL(spec = url, url = url)
        }
    }

    /**
     * Normalizes a URL, returning null if invalid.
     *
     * @param url The URL to normalize (can be null)
     * @param args Optional load arguments
     * @param toItemOption Whether to convert to item load options
     * @return [NormURL] or null if URL is invalid
     */
    fun normalizeOrNull(url: String?, args: String? = null, toItemOption: Boolean = false): NormURL? {
        if (url.isNullOrBlank()) {
            return null
        }
        val result = normalize(url, args, toItemOption)
        return if (result.isNil) null else result
    }

    // ========== Page Loading ==========

    /**
     * Opens a URL immediately, bypassing local cache.
     *
     * This method opens the URL immediately, regardless of the previous
     * state of the page in local storage.
     *
     * @param url The URL to open
     * @param args Optional load arguments
     * @return [WebPage] with the loaded page information
     */
    @Suppress("UNCHECKED_CAST")
    fun open(url: String, args: String? = null): WebPage {
        val payload = mutableMapOf<String, Any?>("url" to url)
        if (args != null) {
            payload["args"] = args
        }
        val value = client.post("/session/{sessionId}/open", payload)
        return if (value is Map<*, *>) {
            WebPage.fromMap(value as Map<String, Any?>)
        } else {
            WebPage(url = url)
        }
    }

    /**
     * Opens a URL immediately with event handlers.
     *
     * This method is reserved for future support of page event handlers.
     * Event handlers allow customization of page loading behavior.
     *
     * @param url The URL to open
     * @param eventHandlers Page event handlers for customization
     * @param args Optional load arguments
     * @return [WebPage] with the loaded page information
     */
    @Suppress("UNCHECKED_CAST")
    fun open(url: String, eventHandlers: PageEventHandlers, args: String? = null): WebPage {
        // MVP implementation for REST/OpenAPI mode:
        // - create a subscription
        // - start an SSE listener using /events/stream
        // - run the actual open()

        val subscription = try {
            val requestedEventTypes = eventHandlers.subscribedEventTypesOrNull() ?: listOf("page")
            val value = driver.subscribeEvents(mapOf("eventTypes" to requestedEventTypes))

            val map = value as? Map<String, Any?>

            // OpenAPI endpoints typically return a WebDriver-shaped wrapper: {"value": {...}}
            val valueObj = map?.get("value")
            when (valueObj) {
                is Map<*, *> -> (valueObj as Map<String, Any?>)["subscriptionId"] as? String
                else -> map?.get("subscriptionId") as? String
            }
        } catch (_: Exception) {
            null
        }

        val stopFlag = java.util.concurrent.atomic.AtomicBoolean(false)

        // Start listener before open, so we can receive early events.
        val listener = Thread {
            try {
                val base = client.resolvedBaseUrl.trimEnd('/')
                val sessionId = client.sessionId ?: return@Thread
                val query = if (!subscription.isNullOrBlank()) {
                    "?subscriptionId=$subscription"
                } else {
                    ""
                }
                val uri = java.net.URI.create("$base/session/$sessionId/events/stream$query")

                val sse = SseClient(client.rawHttpClient, client.resolvedTimeout)
                sse.connect(
                    uri = uri,
                    headers = client.resolvedDefaultHeaders.filterKeys { it.lowercase() != "content-type" },
                    shouldStop = { stopFlag.get() }
                ) { evt ->
                    // Server sends json string as data (we serialize Event DTO as JSON).
                    val oe = OpenApiEvent.fromJson(evt.data)
                    if (oe != null) {
                        // Dispatch to SDK-side handlers.
                        eventHandlers.dispatch(oe)
                    }
                    // else: ignore malformed frames (best-effort)
                }
            } catch (_: Exception) {
                // Swallow listener errors to avoid breaking open(); users can still load the page.
            }
        }.apply {
            name = "pulsar-sdk-open-sse-listener"
            isDaemon = true
        }

        listener.start()

        return try {
            open(url, args)
        } finally {
            stopFlag.set(true)
        }
    }

    /**
     * Loads a URL from local storage or fetches from internet.
     *
     * This method first checks if the page exists in local storage and
     * meets the specified criteria. If so, it returns the cached version.
     * Otherwise, it fetches the page from the internet.
     *
     * @param url The URL to load
     * @param args Optional load arguments (e.g., "-expire 1d", "-refresh")
     * @return [WebPage] with the loaded page information
     */
    @Suppress("UNCHECKED_CAST")
    fun load(url: String, args: String? = null): WebPage {
        val payload = mutableMapOf<String, Any?>("url" to url)
        if (args != null) {
            payload["args"] = args
        }
        val value = client.post("/session/{sessionId}/load", payload)
        return if (value is Map<*, *>) {
            WebPage.fromMap(value as Map<String, Any?>)
        } else {
            WebPage(url = url)
        }
    }

    /**
     * Loads multiple URLs.
     *
     * @param urls URLs to load
     * @param args Optional load arguments applied to all URLs
     * @return List of loaded [WebPage]s
     */
    fun loadAll(urls: Iterable<String>, args: String? = null): List<WebPage> {
        return urls.map { load(it, args) }
    }

    /**
     * Submits a URL to the crawl pool for asynchronous processing.
     *
     * This is a non-blocking operation that returns immediately.
     * The URL will be processed later in the crawl loop.
     *
     * @param url The URL to submit
     * @param args Optional load arguments
     * @return True if the URL was submitted successfully
     */
    fun submit(url: String, args: String? = null): Boolean {
        val payload = mutableMapOf<String, Any?>("url" to url)
        if (args != null) {
            payload["args"] = args
        }
        val value = client.post("/session/{sessionId}/submit", payload)
        return if (value != null) value as? Boolean ?: true else true
    }

    /**
     * Submits multiple URLs to the crawl pool.
     *
     * @param urls URLs to submit
     * @param args Optional load arguments applied to all URLs
     * @return True if all URLs were submitted successfully
     */
    fun submitAll(urls: Iterable<String>, args: String? = null): Boolean {
        for (url in urls) {
            if (!submit(url, args)) {
                return false
            }
        }
        return true
    }

    // ========== Parsing and Extraction ==========

    /**
     * Parses a [WebPage] into a Jsoup document.
     *
     * This method parses the HTML content using Jsoup, providing a rich DOM
     * API for querying and manipulating the document structure.
     *
     * @param page The [WebPage] to parse
     * @return Jsoup Document object, or null if HTML is not available
     */
    fun parse(page: WebPage): org.jsoup.nodes.Document? {
        val html = page.html ?: return null
        val baseUrl = page.url
        return org.jsoup.Jsoup.parse(html, baseUrl)
    }

    /**
     * Extracts fields from a Jsoup document using CSS selectors.
     *
     * @param document The Jsoup document to extract from
     * @param fieldSelectors Map of field names to CSS selectors
     * @return Map of field names to extracted values (text content)
     */
    fun extract(document: org.jsoup.nodes.Document, fieldSelectors: Map<String, String>): Map<String, String?> {
        return fieldSelectors.mapValues { (_, selector) ->
            val elements = document.select(selector)
            if (elements.isEmpty()) null else elements.first()?.text()
        }
    }

    /**
     * Extracts fields from a document using CSS selectors.
     *
     * This is a legacy method that delegates to the WebDriver.
     * For Jsoup-based extraction, use extract(document: org.jsoup.nodes.Document, ...) instead.
     *
     * @param document The document (or page) to extract from
     * @param fieldSelectors Map of field names to selectors
     * @return Map of field names to extracted values
     */
    @Deprecated("Use extract(document: org.jsoup.nodes.Document, ...) for Jsoup documents")
    fun extract(document: Any, fieldSelectors: Map<String, String>): Map<String, String?> {
        return driver.extract(fieldSelectors)
    }

    /**
     * Extracts fields from a document using CSS selectors.
     *
     * @param document The document (or page) to extract from
     * @param selectors List of selectors (selector becomes field name)
     * @return Map of field names to extracted values
     */
    fun extract(document: org.jsoup.nodes.Document, selectors: Iterable<String>): Map<String, String?> {
        val fieldSelectors = selectors.associateWith { it }
        return extract(document, fieldSelectors)
    }

    /**
     * Extracts fields from a document using CSS selectors.
     *
     * This is a legacy method that delegates to the WebDriver.
     *
     * @param document The document (or page) to extract from
     * @param selectors List of selectors (selector becomes field name)
     * @return Map of field names to extracted values
     */
    @Deprecated("Use extract(document: org.jsoup.nodes.Document, ...) for Jsoup documents")
    fun extract(document: Any, selectors: Iterable<String>): Map<String, String?> {
        val fieldSelectors = selectors.associateWith { it }
        return driver.extract(fieldSelectors)
    }

    /**
     * Loads a page, parses it, and extracts fields in one operation.
     *
     * @param url The URL to scrape
     * @param args Load arguments
     * @param fieldSelectors Field selectors for extraction
     * @return Map of field names to extracted values
     */
    fun scrape(url: String, args: String, fieldSelectors: Map<String, String>): Map<String, String?> {
        val page = load(url, args)
        val document = parse(page) ?: return emptyMap()
        return extract(document, fieldSelectors)
    }

    // ========== Chat/LLM Operations ==========

    /**
     * Sends a prompt to the LLM and returns the response.
     *
     * This method provides direct access to chat/LLM capabilities for
     * natural language processing tasks.
     *
     * @param prompt The user prompt to send to the LLM
     * @return [ChatResponse] with the LLM's response
     */
    @Suppress("UNCHECKED_CAST")
    fun chat(prompt: String): ChatResponse {
        val payload = mapOf("prompt" to prompt)
        val value = client.post("/session/{sessionId}/chat", payload)
        return ChatResponse.fromAny(value)
    }

    /**
     * Sends a user message and system message to the LLM.
     *
     * The system message provides context or instructions for how the LLM
     * should respond to the user message.
     *
     * @param userMessage The user's message/question
     * @param systemMessage System instructions for the LLM
     * @return [ChatResponse] with the LLM's response
     */
    @Suppress("UNCHECKED_CAST")
    fun chat(userMessage: String, systemMessage: String): ChatResponse {
        val payload = mapOf(
            "userMessage" to userMessage,
            "systemMessage" to systemMessage
        )
        val value = client.post("/session/{sessionId}/chat", payload)
        return ChatResponse.fromAny(value)
    }

    // ========== Driver Management ==========

    /**
     * Gets or creates a bound WebDriver.
     *
     * @return The bound WebDriver instance
     */
    fun getOrCreateBoundDriver(): WebDriver {
        return driver
    }

    /**
     * Creates a new bound WebDriver.
     *
     * @return A new WebDriver instance
     */
    fun createBoundDriver(): WebDriver {
        _driver = WebDriver(client)
        return _driver!!
    }

    /**
     * Binds a WebDriver to this session.
     *
     * @param driver The WebDriver to bind
     */
    fun bindDriver(driver: WebDriver) {
        _driver = driver
    }

    /**
     * Unbinds a WebDriver from this session.
     *
     * @param driver The WebDriver to unbind
     */
    fun unbindDriver(driver: WebDriver) {
        if (_driver === driver) {
            _driver = null
        }
    }

    // ========== Capture Operations ==========

    /**
     * Captures the live page controlled by a WebDriver.
     *
     * This creates a static snapshot of the current page state.
     *
     * @param driver The WebDriver controlling the page (uses bound driver if null)
     * @param url Optional URL to identify the capture
     * @return [PageSnapshot] with the captured page
     */
    @Suppress("UNCHECKED_CAST")
    fun capture(driver: WebDriver? = null, url: String? = null): WebPage {
        val drv = driver ?: this.driver
        val currentUrl = url ?: drv.getCurrentUrl()
        val value = client.post("/session/{sessionId}/open", mapOf("url" to currentUrl))
        return WebPage(
            url = if (value is Map<*, *>) (value as Map<String, Any?>)["url"] as? String ?: currentUrl else currentUrl,
            html = if (value is Map<*, *>) (value as Map<String, Any?>)["html"] as? String else null
        )
    }

    // ========== Helper Methods (API Compatibility) ==========

    /**
     * Registers a closable object with the session.
     *
     * @param closable Object with a close() method
     */
    fun registerClosable(closable: Any) {
        // Placeholder for resource management
    }

    /**
     * Gets or sets session data.
     *
     * @param name Data key name
     * @param value Value to set (if provided)
     * @return Stored value for the name
     */
    fun data(name: String, value: Any? = null): Any? {
        // Placeholder for session data storage
        return null
    }

    /**
     * Gets or sets a session property.
     *
     * @param name Property name
     * @param value Value to set (if provided)
     * @return Property value
     */
    fun property(name: String, value: String? = null): String? {
        // Placeholder for session properties
        return null
    }

    /**
     * Creates load options from arguments string.
     *
     * @param args Load arguments string
     * @param eventHandlers Optional event handlers
     * @return Options map
     */
    fun options(args: String = "", eventHandlers: PageEventHandlers? = null): Map<String, Any?> {
        return mapOf("args" to args, "eventHandlers" to eventHandlers)
    }

    // ========== Utility Methods ==========

    /**
     * Checks if a page exists in storage.
     *
     * @param url The URL to check
     * @return True if the page exists in storage
     */
    fun exists(url: String): Boolean {
        // This would need a dedicated endpoint; using a workaround
        return false
    }

    /**
     * Flushes pending changes to storage.
     */
    fun flush() {
        // Placeholder
    }

    /**
     * Closes the session.
     */
    override fun close() {
        client.deleteSession()
    }
}
