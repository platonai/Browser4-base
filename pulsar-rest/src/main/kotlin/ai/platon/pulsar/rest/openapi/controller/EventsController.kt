package ai.platon.pulsar.rest.openapi.controller

import ai.platon.pulsar.rest.openapi.dto.*
import ai.platon.pulsar.rest.openapi.service.SessionManager
import ai.platon.pulsar.rest.openapi.store.InMemoryStore
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException

/**
 * Controller for event configuration and subscription.
 */
@RestController
@CrossOrigin
@RequestMapping(
    "/session/{sessionId}",
    produces = [MediaType.APPLICATION_JSON_VALUE]
)
@ConditionalOnBean(SessionManager::class)
class EventsController(
    private val sessionManager: SessionManager,
    private val store: InMemoryStore,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(EventsController::class.java)

    /**
     * Creates an event configuration.
     */
    @PostMapping("/event-configs", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun createEventConfig(
        @PathVariable sessionId: String,
        @RequestBody request: EventConfig,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} creating event config for type: {}", sessionId, request.eventType)
        ControllerUtils.addRequestId(response)

        if (!sessionManager.sessionExists(sessionId)) {
            return ControllerUtils.notFound("session not found", "No active session with id $sessionId")
        }

        val config = store.addEventConfig(sessionId, request)

        return ResponseEntity.ok(EventConfigResponse(value = config))
    }

    /**
     * Gets all event configurations for a session.
     */
    @GetMapping("/event-configs")
    fun getEventConfigs(
        @PathVariable sessionId: String,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} getting event configs", sessionId)
        ControllerUtils.addRequestId(response)

        if (!sessionManager.sessionExists(sessionId)) {
            return ControllerUtils.notFound("session not found", "No active session with id $sessionId")
        }

        val configs = store.getEventConfigs(sessionId)

        return ResponseEntity.ok(EventConfigsResponse(value = configs))
    }

    /**
     * Gets all events for a session.
     */
    @GetMapping("/events")
    fun getEvents(
        @PathVariable sessionId: String,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} getting events", sessionId)
        ControllerUtils.addRequestId(response)

        if (!sessionManager.sessionExists(sessionId)) {
            return ControllerUtils.notFound("session not found", "No active session with id $sessionId")
        }

        val events = store.getEvents(sessionId)

        return ResponseEntity.ok(EventsResponse(value = events))
    }

    /**
     * Subscribes to events.
     */
    @PostMapping("/events/subscribe", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun subscribeToEvents(
        @PathVariable sessionId: String,
        @RequestBody request: SubscribeRequest,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} subscribing to events: {}", sessionId, request.eventTypes)
        ControllerUtils.addRequestId(response)

        if (!sessionManager.sessionExists(sessionId)) {
            return ControllerUtils.notFound("session not found", "No active session with id $sessionId")
        }

        val subscription = store.createSubscription(sessionId, request.eventTypes)

        return ResponseEntity.ok(SubscriptionResponse(value = subscription))
    }

    /**
     * Streams session events as Server-Sent Events (SSE).
     *
     * This endpoint provides a persistent connection for clients to receive events
     * in real time. It is intentionally simple and store-backed: it polls the in-memory
     * event list for new items and pushes them to the client.
     *
     * Filtering:
     * - If subscriptionId is provided, the server uses the subscription's eventTypes filter.
     * - Otherwise, clients can pass eventTypes (repeated query params) to filter events.
     */
    @GetMapping("/events/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamEvents(
        @PathVariable sessionId: String,
        @RequestParam(required = false) subscriptionId: String?,
        @RequestParam(required = false) eventTypes: List<String>?,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): SseEmitter {
        ControllerUtils.addRequestId(response)

        if (!sessionManager.sessionExists(sessionId)) {
            // With SSE, returning a normal error response is awkward; fail fast.
            throw IllegalArgumentException("No active session with id $sessionId")
        }

        val filter: Set<String>? = when {
            !subscriptionId.isNullOrBlank() -> {
                val sub = store.getSubscription(sessionId, subscriptionId)
                    ?: throw IllegalArgumentException("No subscription with id $subscriptionId")
                sub.eventTypes.toSet()
            }

            !eventTypes.isNullOrEmpty() -> eventTypes.toSet()
            else -> null
        }

        logger.debug(
            "Session {} streaming events (subscriptionId={}, filterSize={})",
            sessionId,
            subscriptionId,
            filter?.size ?: 0
        )

        // 30 minutes by default; clients may reconnect.
        val emitter = SseEmitter(30L * 60L * 1000L)

        val worker = Thread {
            var cursor = 0
            var lastKeepAliveNs = System.nanoTime()

            try {
                while (!Thread.currentThread().isInterrupted) {
                    if (request.isAsyncStarted.not() && request.isRequestedSessionIdValid.not()) {
                        // Best-effort exit if request looks invalid.
                        break
                    }

                    val (nextCursor, batch) = store.getEventsFrom(sessionId, cursor, filter)
                    cursor = nextCursor

                    // Send new events.
                    for (e in batch) {
                        val json = objectMapper.writeValueAsString(e)
                        emitter.send(
                            SseEmitter.event()
                                .name(e.eventType)
                                .id(e.eventId)
                                .data(json)
                        )
                    }

                    // Keep-alive every ~10 seconds to prevent proxies closing the connection.
                    val now = System.nanoTime()
                    if (now - lastKeepAliveNs > 10_000_000_000L) {
                        try {
                            emitter.send(SseEmitter.event().comment("keep-alive"))
                        } catch (_: IOException) {
                            break
                        }
                        lastKeepAliveNs = now
                    }

                    Thread.sleep(200)
                }

                emitter.complete()
            } catch (e: Exception) {
                emitter.completeWithError(e)
            }
        }.apply {
            name = "openapi-events-sse-$sessionId"
            isDaemon = true
        }

        emitter.onCompletion { worker.interrupt() }
        emitter.onTimeout {
            worker.interrupt()
            emitter.complete()
        }
        emitter.onError {
            worker.interrupt()
        }

        worker.start()
        return emitter
    }
}
