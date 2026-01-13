package ai.platon.pulsar.skeleton.crawl

import ai.platon.pulsar.persist.WebPage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * The global event handlers.
 * */
object GlobalEventHandlers {
    /**
     * The page event handlers.
     *
     * The calling order rule:
     *
     * The more specific handlers has the opportunity to override the result of more general handlers.
     * */
    var pageEventHandlers: PageEventHandlers? = null

    /**
     * The server-side event handlers for broadcasting events to external listeners.
     *
     * When set, events from page event handlers will be forwarded to this handler,
     * which can broadcast them to clients via SSE or other mechanisms.
     * */
    var serverSideEventHandlers: ServerSideEventHandlers? = null

    /**
     * Background coroutine scope for non-blocking event emission.
     * Uses Dispatchers.Default for CPU-bound work and SupervisorJob to isolate failures.
     */
    private val eventScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * Emits a crawl event to server-side event handlers in a non-blocking manner.
     * This method can be called from any thread without blocking.
     */
    fun emitCrawlEvent(eventType: String, url: String? = null, message: String? = null) {
        serverSideEventHandlers?.let { handlers ->
            eventScope.launch {
                handlers.onCrawlEvent(eventType, url, message)
            }
        }
    }

    /**
     * Emits a load event to server-side event handlers in a non-blocking manner.
     * This method can be called from any thread without blocking.
     */
    fun emitLoadEvent(eventType: String, page: WebPage, message: String? = null, metadata: Map<String, Any?> = emptyMap()) {
        serverSideEventHandlers?.let { handlers ->
            eventScope.launch {
                handlers.onLoadEvent(eventType, page, message, metadata)
            }
        }
    }

    /**
     * Emits a browse event to server-side event handlers in a non-blocking manner.
     * This method can be called from any thread without blocking.
     */
    fun emitBrowseEvent(eventType: String, page: WebPage, message: String? = null, metadata: Map<String, Any?> = emptyMap()) {
        serverSideEventHandlers?.let { handlers ->
            eventScope.launch {
                handlers.onBrowseEvent(eventType, page, message, metadata)
            }
        }
    }
}
