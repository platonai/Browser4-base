@file:Suppress("UNUSED")
package ai.platon.pulsar.sdk.v0

import ai.platon.pulsar.sdk.v0.detail.OpenApiEvent
import ai.platon.pulsar.sdk.v0.detail.PulsarClient
import ai.platon.pulsar.sdk.v0.detail.SseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

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
    private val closeableObjects = ConcurrentLinkedQueue<AutoCloseable>()

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
    suspend fun normalize(url: String, args: String? = null, toItemOption: Boolean = false): NormURL {
        val payload = mutableMapOf<String, Any?>("url" to url, "toItemOption" to toItemOption)
        if (args != null) {
            payload["args"] = args
        }
        val value = client.post("/session/{sessionId}/normalize", payload)
        return if (value is Map<*, *>) {
            NormURL.fromMap(value as Map<String, Any?>)
        } else {
            NormURL(url = url, args = args)
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
    suspend fun normalizeOrNull(url: String?, args: String? = null, toItemOption: Boolean = false): NormURL? {
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
    suspend fun open(url: String, args: String? = null): WebPage {
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
    suspend fun open(url: String, eventHandlers: PageEventHandlers, args: String? = null): WebPage {
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

        val stopFlag = AtomicBoolean(false)

        // Start listener using structured concurrency tied to the current coroutine
        return coroutineScope {
            val listenerJob = launch(Dispatchers.IO) {
                try {
                    val base = client.resolvedBaseUrl.trimEnd('/')
                    val sessionId = client.sessionId ?: return@launch
                    val query = if (!subscription.isNullOrBlank()) {
                        "?subscriptionId=$subscription"
                    } else {
                        ""
                    }
                    val url = "$base/session/$sessionId/events/stream$query"

                    val sse = SseClient(client.rawHttpClient)
                    sse.connect(
                        url = url,
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
            }

            try {
                open(url, args)
            } finally {
                stopFlag.set(true)
                listenerJob.cancel()
            }
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
    suspend fun load(url: String, args: String? = null): WebPage {
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
    suspend fun loadAll(urls: Iterable<String>, args: String? = null): List<WebPage> {
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
    suspend fun submit(url: String, args: String? = null): Boolean {
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
    suspend fun submitAll(urls: Iterable<String>, args: String? = null): Boolean {
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
    fun parse(page: WebPage): Document? {
        val html = page.html ?: return null
        val baseUrl = page.url
        return Jsoup.parse(html, baseUrl)
    }

    /**
     * Extracts fields from a Jsoup document using CSS selectors.
     *
     * @param document The Jsoup document to extract from
     * @param fieldSelectors Map of field names to CSS selectors
     * @return Map of field names to extracted values (text content)
     */
    fun extract(document: Document, fieldSelectors: Map<String, String>): Map<String, String?> {
        return fieldSelectors.mapValues { (_, selector) ->
            val elements = document.select(selector)
            if (elements.isEmpty()) null else elements.first()?.text()
        }
    }

    /**
     * Extracts fields from a document using CSS selectors.
     *
     * @param document The document (or page) to extract from
     * @param selectors List of selectors (selector becomes field name)
     * @return Map of field names to extracted values
     */
    fun extract(document: Document, selectors: Iterable<String>): Map<String, String?> {
        val fieldSelectors = selectors.associateWith { it }
        return extract(document, fieldSelectors)
    }

    /**
     * Loads a page, parses it, and extracts fields in one operation.
     *
     * @param url The URL to scrape
     * @param args Load arguments
     * @param fieldSelectors Field selectors for extraction
     * @return Map of field names to extracted values
     */
    suspend fun scrape(url: String, args: String, fieldSelectors: Map<String, String>): Map<String, String?> {
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
    suspend fun chat(prompt: String): ChatResponse {
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
    suspend fun chat(userMessage: String, systemMessage: String): ChatResponse {
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
    suspend fun capture(driver: WebDriver? = null, url: String? = null): WebPage {
        TODO()
    }

    // ========== Utility Methods ==========

    /**
     * Checks if a page exists in storage.
     *
     * @param url The URL to check
     * @return True if the page exists in storage
     */
    fun exists(url: String): Boolean {
        TODO()
    }

    fun registerClosable(closable: AutoCloseable) {
        closeableObjects.add(closable)
    }

    /**
     * Closes the session.
     *
     * Note: In coroutine-based SDK, proper cleanup should be done by calling deleteSession()
     * from a suspend context before closing. This method only closes resources that don't require suspension.
     */
    override fun close() {
        while (closeableObjects.isNotEmpty()) {
            closeableObjects.poll()?.also {
                try {
                    it.close()
                } catch (_: Exception) {
                    // Swallow close errors
                }
            }
        }
    }
}
