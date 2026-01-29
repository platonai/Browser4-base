package ai.platon.pulsar.rest.openapi.controller

import ai.platon.pulsar.rest.openapi.dto.*
import ai.platon.pulsar.rest.openapi.service.SessionManager
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriverException
import jakarta.servlet.http.HttpServletResponse
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Controller for scrolling operations.
 */
@RestController
@CrossOrigin
@RequestMapping(
    "/session/{sessionId}/scroll",
    produces = [MediaType.APPLICATION_JSON_VALUE]
)
@ConditionalOnBean(SessionManager::class)
class ScrollController(
    private val sessionManager: SessionManager
) {
    private val logger = LoggerFactory.getLogger(ScrollController::class.java)

    /**
     * Scrolls down on the page.
     */
    @PostMapping("/down", consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun scrollDown(
        @PathVariable sessionId: String,
        @RequestBody request: ScrollCountRequest,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} scrolling down {} times", sessionId, request.count)
        ControllerUtils.addRequestId(response)

        val managed = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        return try {
            val scrollY = managed.mutex.withLock {
                val driver = managed.driver
                driver.scrollDown(request.count)
            }
            ResponseEntity.ok(WebDriverResponse(value = scrollY))
        } catch (e: WebDriverException) {
            logger.error("Scroll down failed | sessionId={} | {}", sessionId, e.message)
            ControllerUtils.errorResponse("webdriver error", e.message ?: "WebDriver error")
        } catch (e: Exception) {
            logger.error("Scroll down failed | sessionId={} | {}", sessionId, e.message, e)
            ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
        }
    }

    /**
     * Scrolls up on the page.
     */
    @PostMapping("/up", consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun scrollUp(
        @PathVariable sessionId: String,
        @RequestBody request: ScrollCountRequest,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} scrolling up {} times", sessionId, request.count)
        ControllerUtils.addRequestId(response)

        val managed = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        return try {
            val scrollY = managed.mutex.withLock {
                val driver = managed.driver
                driver.scrollUp(request.count)
            }
            ResponseEntity.ok(WebDriverResponse(value = scrollY))
        } catch (e: WebDriverException) {
            logger.error("Scroll up failed | sessionId={} | {}", sessionId, e.message)
            ControllerUtils.errorResponse("webdriver error", e.message ?: "WebDriver error")
        } catch (e: Exception) {
            logger.error("Scroll up failed | sessionId={} | {}", sessionId, e.message, e)
            ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
        }
    }

    /**
     * Scrolls to an element.
     */
    @PostMapping("/to", consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun scrollTo(
        @PathVariable sessionId: String,
        @RequestBody request: SelectorRef,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} scrolling to selector: {}", sessionId, request.selector)
        ControllerUtils.addRequestId(response)

        val managed = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        return try {
            val scrollY = managed.mutex.withLock {
                val driver = managed.driver
                driver.scrollTo(request.selector)
            }
            ResponseEntity.ok(WebDriverResponse(value = scrollY))
        } catch (e: WebDriverException) {
            logger.error("Scroll to failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message)
            ControllerUtils.errorResponse("webdriver error", e.message ?: "WebDriver error")
        } catch (e: Exception) {
            logger.error("Scroll to failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message, e)
            ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
        }
    }

    /**
     * Scrolls to the top of the page.
     */
    @PostMapping("/top")
    suspend fun scrollToTop(
        @PathVariable sessionId: String,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} scrolling to top", sessionId)
        ControllerUtils.addRequestId(response)

        val managed = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        return try {
            val scrollY = managed.mutex.withLock {
                val driver = managed.driver
                driver.scrollToTop()
            }
            ResponseEntity.ok(WebDriverResponse(value = scrollY))
        } catch (e: WebDriverException) {
            logger.error("Scroll to top failed | sessionId={} | {}", sessionId, e.message)
            ControllerUtils.errorResponse("webdriver error", e.message ?: "WebDriver error")
        } catch (e: Exception) {
            logger.error("Scroll to top failed | sessionId={} | {}", sessionId, e.message, e)
            ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
        }
    }

    /**
     * Scrolls to the bottom of the page.
     */
    @PostMapping("/bottom")
    suspend fun scrollToBottom(
        @PathVariable sessionId: String,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} scrolling to bottom", sessionId)
        ControllerUtils.addRequestId(response)

        val managed = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        return try {
            val scrollY = managed.mutex.withLock {
                val driver = managed.driver
                driver.scrollToBottom()
            }
            ResponseEntity.ok(WebDriverResponse(value = scrollY))
        } catch (e: WebDriverException) {
            logger.error("Scroll to bottom failed | sessionId={} | {}", sessionId, e.message)
            ControllerUtils.errorResponse("webdriver error", e.message ?: "WebDriver error")
        } catch (e: Exception) {
            logger.error("Scroll to bottom failed | sessionId={} | {}", sessionId, e.message, e)
            ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
        }
    }

    /**
     * Scrolls to a position on the page.
     */
    @PostMapping("/middle", consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun scrollToMiddle(
        @PathVariable sessionId: String,
        @RequestBody request: ScrollRatioRequest,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} scrolling to middle with ratio: {}", sessionId, request.ratio)
        ControllerUtils.addRequestId(response)

        val managed = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        return try {
            val scrollY = managed.mutex.withLock {
                val driver = managed.driver
                driver.scrollToMiddle(request.ratio)
            }
            ResponseEntity.ok(WebDriverResponse(value = scrollY))
        } catch (e: WebDriverException) {
            logger.error("Scroll to middle failed | sessionId={} | {}", sessionId, e.message)
            ControllerUtils.errorResponse("webdriver error", e.message ?: "WebDriver error")
        } catch (e: Exception) {
            logger.error("Scroll to middle failed | sessionId={} | {}", sessionId, e.message, e)
            ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
        }
    }

    /**
     * Scrolls by a specific amount of pixels.
     */
    @PostMapping("/by", consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun scrollBy(
        @PathVariable sessionId: String,
        @RequestBody request: ScrollByRequest,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} scrolling by {} pixels", sessionId, request.pixels)
        ControllerUtils.addRequestId(response)

        val managed = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        return try {
            val scrollY = managed.mutex.withLock {
                val driver = managed.driver
                driver.scrollBy(request.pixels, request.smooth)
            }
            ResponseEntity.ok(WebDriverResponse(value = scrollY))
        } catch (e: WebDriverException) {
            logger.error("Scroll by failed | sessionId={} | {}", sessionId, e.message)
            ControllerUtils.errorResponse("webdriver error", e.message ?: "WebDriver error")
        } catch (e: Exception) {
            logger.error("Scroll by failed | sessionId={} | {}", sessionId, e.message, e)
            ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
        }
    }
}
