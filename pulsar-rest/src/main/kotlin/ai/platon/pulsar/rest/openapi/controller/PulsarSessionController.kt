package ai.platon.pulsar.rest.openapi.controller

import ai.platon.pulsar.external.ChatModelFactory
import ai.platon.pulsar.rest.openapi.dto.*
import ai.platon.pulsar.rest.openapi.service.SessionManager
import ai.platon.pulsar.skeleton.context.PulsarContext
import jakarta.servlet.http.HttpServletResponse
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Controller for PulsarSession operations.
 * Provides URL normalization, page loading, and URL submission capabilities.
 */
@RestController
@CrossOrigin
@RequestMapping(
    "/session/{sessionId}",
    produces = [MediaType.APPLICATION_JSON_VALUE]
)
@ConditionalOnBean(SessionManager::class)
class PulsarSessionController(
    private val sessionManager: SessionManager,
    private val pulsarContext: PulsarContext,
    @param:Value("\${pulsar.stub.mode:false}")
    private val stubMode: Boolean = false
) {
    private val logger = LoggerFactory.getLogger(PulsarSessionController::class.java)

    private fun shouldStub(): Boolean = stubMode || !ChatModelFactory.isModelConfigured(pulsarContext.configuration)

    /**
     * Normalizes a URL with optional load arguments.
     */
    @PostMapping("/normalize", consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun normalize(
        @PathVariable sessionId: String,
        @RequestBody request: NormalizeRequest,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} normalizing URL: {}", sessionId, request.url)
        ControllerUtils.addRequestId(response)

        val session = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        // Use real PulsarSession.normalize, protected by mutex for serial execution
        val normUrl = session.mutex.withLock {
            session.pulsarSession.normalize(request.url, request.args ?: "")
        }
        val result = NormUrlResult(
            url = normUrl.url.toString(),
            args = normUrl.args,
        )

        return ResponseEntity.ok(NormalizeResponse(value = result))
    }

    /**
     * Opens a URL immediately, bypassing the local cache.
     */
    @PostMapping("/open", consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun open(
        @PathVariable sessionId: String,
        @RequestBody request: OpenRequest,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} opening URL: {}", sessionId, request.url)
        ControllerUtils.addRequestId(response)

        val session = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        // Use real PulsarSession.open (which fetches fresh from internet), protected by mutex for serial execution
        val page = session.mutex.withLock {
            session.pulsarSession.open(request.url)
        }

        sessionManager.setSessionUrl(sessionId, request.url)

        val result = WebPageResult(
            url = page.url,
            location = page.location,
            contentType = page.contentType,
            contentLength = page.contentLength.toInt(),
            protocolStatus = page.protocolStatus.toString(),
            html = page.contentAsString
        )

        return ResponseEntity.ok(OpenResponse(value = result))
    }

    /**
     * Loads a URL from local storage or fetches from the internet.
     * Checks local cache first; if page exists and meets criteria, returns cached version.
     * Otherwise, fetches from the internet.
     */
    @PostMapping("/load", consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun load(
        @PathVariable sessionId: String,
        @RequestBody request: LoadRequest,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} loading URL: {} with args: {}", sessionId, request.url, request.args)
        ControllerUtils.addRequestId(response)

        val session = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        // Use real PulsarSession.load (checks cache first), protected by mutex for serial execution
        val page = session.mutex.withLock {
            if (request.args != null) {
                session.pulsarSession.load(request.url, request.args)
            } else {
                session.pulsarSession.load(request.url)
            }
        }

        sessionManager.setSessionUrl(sessionId, request.url)

        val result = WebPageResult(
            url = page.url,
            location = page.location ?: page.url,
            contentType = page.contentType ?: "text/html",
            contentLength = page.contentLength.toInt(),
            protocolStatus = page.protocolStatus?.toString() ?: "200 OK",
            isNil = page.isNil,
            html = page.contentAsString
        )

        return ResponseEntity.ok(LoadResponse(value = result))
    }

    /**
     * Submits a URL to the crawl pool for asynchronous processing.
     * This is a non-blocking operation that returns immediately.
     */
    @PostMapping("/submit", consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun submit(
        @PathVariable sessionId: String,
        @RequestBody request: SubmitRequest,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} submitting URL: {} with args: {}", sessionId, request.url, request.args)
        ControllerUtils.addRequestId(response)

        val session = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        // Use real PulsarSession.submit, protected by mutex for serial execution
        session.mutex.withLock {
            if (request.args != null) {
                session.pulsarSession.submit(request.url, request.args)
            } else {
                session.pulsarSession.submit(request.url)
            }
        }

        return ResponseEntity.ok(SubmitResponse(value = true))
    }

    /**
     * Chat with LLM using a simple prompt or user/system messages.
     * Supports two modes:
     * 1. Single prompt: { "prompt": "What is 2 + 2?" }
     * 2. User + system messages: { "userMessage": "...", "systemMessage": "..." }
     */
    @PostMapping("/chat", consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun chat(
        @PathVariable sessionId: String,
        @RequestBody request: ChatRequest,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} chat request", sessionId)
        ControllerUtils.addRequestId(response)

        val session = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        if (shouldStub()) {
            val result = ChatResponse.ChatResponseValue(
                content = "Test mode response",
                role = "assistant",
                model = "stub"
            )
            return ResponseEntity.ok(ChatResponse(value = result))
        }

        val result = try {
            session.mutex.withLock {
                val chatModel = ChatModelFactory.getOrCreate(pulsarContext.configuration)

                val content = when {
                    request.prompt != null -> {
                        chatModel.call(request.prompt).content
                    }
                    request.userMessage != null -> {
                        val systemMsg = request.systemMessage ?: ""
                        chatModel.call(systemMsg, request.userMessage).content
                    }
                    else -> {
                        return@withLock ChatResponse.ChatResponseValue(
                            content = "Error: Either 'prompt' or 'userMessage' must be provided",
                            role = "assistant"
                        )
                    }
                }

                ChatResponse.ChatResponseValue(
                    content = content,
                    role = "assistant",
                    model = chatModel.javaClass.simpleName
                )
            }
        } catch (e: Exception) {
            logger.error("Error in chat: {}", e.message, e)
            ChatResponse.ChatResponseValue(
                content = "Error: ${e.message}",
                role = "assistant"
            )
        }

        return ResponseEntity.ok(ChatResponse(value = result))
    }
}
