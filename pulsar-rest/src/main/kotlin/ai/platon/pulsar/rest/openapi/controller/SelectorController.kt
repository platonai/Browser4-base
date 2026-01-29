package ai.platon.pulsar.rest.openapi.controller

import ai.platon.pulsar.external.ChatModelFactory
import ai.platon.pulsar.rest.openapi.dto.*
import ai.platon.pulsar.rest.openapi.service.SessionManager
import ai.platon.pulsar.rest.openapi.store.InMemoryStore
import ai.platon.pulsar.skeleton.context.PulsarContext
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriverException
import jakarta.servlet.http.HttpServletResponse
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Controller for selector-first element operations.
 */
@RestController
@CrossOrigin
@RequestMapping(
    "/session/{sessionId}/selectors",
    produces = [MediaType.APPLICATION_JSON_VALUE]
)
@ConditionalOnBean(SessionManager::class)
class SelectorController(
    private val sessionManager: SessionManager,
    private val pulsarContext: PulsarContext,
    private val store: InMemoryStore,
    @param:Value($$"${pulsar.stub.mode:false}")
    private val stubMode: Boolean = false
) {
    private val logger = LoggerFactory.getLogger(SelectorController::class.java)

    private fun shouldStub(): Boolean = stubMode || !ChatModelFactory.isModelConfigured(pulsarContext.configuration)

    /**
     * Checks if an element matching the selector exists.
     */
    @PostMapping("/exists", consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun selectorExists(
        @PathVariable sessionId: String,
        @RequestBody request: SelectorRef,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} checking selector exists: {}", sessionId, request.selector)
        ControllerUtils.addRequestId(response)

        val managed = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        if (shouldStub()) {
            return ResponseEntity.ok(ExistsResponse(value = ExistsResponse.ExistsValue(exists = true)))
        }

        return try {
            val exists = managed.mutex.withLock {
                val driver = managed.driver
                driver.exists(request.selector)
            }
            ResponseEntity.ok(ExistsResponse(value = ExistsResponse.ExistsValue(exists = exists)))
        } catch (e: WebDriverException) {
            logger.error("Selector exists check failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message)
            ControllerUtils.errorResponse("webdriver error", e.message ?: "WebDriver error")
        } catch (e: Exception) {
            logger.error("Selector exists check failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message, e)
            ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
        }
    }

    /**
     * Waits for an element matching the selector to appear.
     */
    @PostMapping("/waitFor", consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun waitForSelector(
        @PathVariable sessionId: String,
        @RequestBody request: WaitForRequest,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} waiting for selector: {} (timeout: {}ms)", sessionId, request.selector, request.timeout)
        ControllerUtils.addRequestId(response)

        val managed = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        val timeoutMillis = request.timeout.toLong().coerceAtLeast(0)

        if (shouldStub()) {
            return ResponseEntity.ok(WebDriverResponse<Any?>(value = null))
        }

        return try {
            val remainingMillis = managed.mutex.withLock {
                val driver = managed.driver
                driver.waitForSelector(request.selector, timeoutMillis)
            }

            if (remainingMillis <= 0L) {
                // OpenAPI defines 408 for waitFor timeout.
                return ResponseEntity.status(408).body(
                    ErrorResponse(
                        value = ErrorResponse.ErrorValue(
                            error = "timeout",
                            message = "Timeout waiting for selector '${request.selector}'"
                        )
                    )
                )
            }

            ResponseEntity.ok(WebDriverResponse<Any?>(value = null))
        } catch (e: WebDriverException) {
            logger.error("Wait for selector failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message)
            ControllerUtils.errorResponse("webdriver error", e.message ?: "WebDriver error")
        } catch (e: Exception) {
            logger.error("Wait for selector failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message, e)
            ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
        }
    }

    /**
     * Finds a single element by selector.
     */
    @PostMapping("/element", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun findElementBySelector(
        @PathVariable sessionId: String,
        @RequestBody request: SelectorRef,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} finding element by selector: {}", sessionId, request.selector)
        ControllerUtils.addRequestId(response)

        if (!sessionManager.sessionExists(sessionId)) {
            return ControllerUtils.notFound("session not found", "No active session with id $sessionId")
        }

        val element = store.getOrCreateElement(sessionId, request.selector, request.strategy)

        return ResponseEntity.ok(ElementResponse(value = ElementRef(elementId = element.elementId)))
    }

    /**
     * Finds all elements matching the selector.
     */
    @PostMapping("/elements", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun findElementsBySelector(
        @PathVariable sessionId: String,
        @RequestBody request: SelectorRef,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} finding elements by selector: {}", sessionId, request.selector)
        ControllerUtils.addRequestId(response)

        if (!sessionManager.sessionExists(sessionId)) {
            return ControllerUtils.notFound("session not found", "No active session with id $sessionId")
        }

        val element = store.getOrCreateElement(sessionId, request.selector, request.strategy)

        return ResponseEntity.ok(ElementsResponse(value = listOf(ElementRef(elementId = element.elementId))))
    }

    /**
     * Clicks an element by selector.
     */
    @PostMapping("/click", consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun clickBySelector(
        @PathVariable sessionId: String,
        @RequestBody request: SelectorRef,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} clicking selector: {}", sessionId, request.selector)
        ControllerUtils.addRequestId(response)

        val managed = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        return try {
            managed.mutex.withLock {
                val driver = managed.driver
                driver.click(request.selector)
            }
            ResponseEntity.ok(WebDriverResponse<Any?>(value = null))
        } catch (e: WebDriverException) {
            logger.error("Click failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message)
            ControllerUtils.errorResponse("webdriver error", e.message ?: "WebDriver error")
        } catch (e: Exception) {
            logger.error("Click failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message, e)
            ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
        }
    }

    /**
     * Fills an input element by selector.
     */
    @PostMapping("/fill", consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun fillBySelector(
        @PathVariable sessionId: String,
        @RequestBody request: FillRequest,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} filling selector: {} with value: {}", sessionId, request.selector, request.value)
        ControllerUtils.addRequestId(response)

        val managed = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        return try {
            managed.mutex.withLock {
                val driver = managed.driver
                driver.fill(request.selector, request.value)
            }
            ResponseEntity.ok(WebDriverResponse<Any?>(value = null))
        } catch (e: WebDriverException) {
            logger.error("Fill failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message)
            ControllerUtils.errorResponse("webdriver error", e.message ?: "WebDriver error")
        } catch (e: Exception) {
            logger.error("Fill failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message, e)
            ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
        }
    }

    /**
     * Presses a key on an element by selector.
     */
    @PostMapping("/press", consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun pressBySelector(
        @PathVariable sessionId: String,
        @RequestBody request: PressRequest,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} pressing key: {} on selector: {}", sessionId, request.key, request.selector)
        ControllerUtils.addRequestId(response)

        val managed = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        return try {
            managed.mutex.withLock {
                val driver = managed.driver
                driver.press(request.selector, request.key)
            }
            ResponseEntity.ok(WebDriverResponse<Any?>(value = null))
        } catch (e: WebDriverException) {
            logger.error("Press failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message)
            ControllerUtils.errorResponse("webdriver error", e.message ?: "WebDriver error")
        } catch (e: Exception) {
            logger.error("Press failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message, e)
            ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
        }
    }

    /**
     * Gets the outer HTML of an element by selector.
     */
    @PostMapping("/outerHtml", consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun getOuterHtmlBySelector(
        @PathVariable sessionId: String,
        @RequestBody request: SelectorRef,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} getting outerHtml for selector: {}", sessionId, request.selector)
        ControllerUtils.addRequestId(response)

        val managed = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        return try {
            val html = managed.mutex.withLock {
                val driver = managed.driver
                driver.outerHTML(request.selector)
            }
            ResponseEntity.ok(HtmlResponse(value = html))
        } catch (e: WebDriverException) {
            logger.error("Get outerHtml failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message)
            ControllerUtils.errorResponse("webdriver error", e.message ?: "WebDriver error")
        } catch (e: Exception) {
            logger.error("Get outerHtml failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message, e)
            ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
        }
    }

    /**
     * Takes a screenshot of an element by selector.
     */
    @PostMapping("/screenshot", consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun screenshotBySelector(
        @PathVariable sessionId: String,
        @RequestBody request: SelectorRef,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} taking screenshot of selector: {}", sessionId, request.selector)
        ControllerUtils.addRequestId(response)

        val managed = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        return try {
            val base64 = managed.mutex.withLock {
                val driver = managed.driver
                driver.captureScreenshot(request.selector)
            }
            ResponseEntity.ok(ScreenshotResponse(value = base64))
        } catch (e: WebDriverException) {
            logger.error("Screenshot failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message)
            ControllerUtils.errorResponse("webdriver error", e.message ?: "WebDriver error")
        } catch (e: Exception) {
            logger.error("Screenshot failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message, e)
            ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
        }
    }

    /**
     * Checks if an element is visible.
     */
    @PostMapping("/isVisible", consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun isVisible(
        @PathVariable sessionId: String,
        @RequestBody request: SelectorRef,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} checking if selector is visible: {}", sessionId, request.selector)
        ControllerUtils.addRequestId(response)

        val managed = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        return try {
            val visible = managed.mutex.withLock {
                val driver = managed.driver
                driver.isVisible(request.selector)
            }
            ResponseEntity.ok(WebDriverResponse(value = visible))
        } catch (e: WebDriverException) {
            logger.error("isVisible check failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message)
            ControllerUtils.errorResponse("webdriver error", e.message ?: "WebDriver error")
        } catch (e: Exception) {
            logger.error("isVisible check failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message, e)
            ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
        }
    }

    /**
     * Checks if an element is checked (checkbox/radio).
     */
    @PostMapping("/isChecked", consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun isChecked(
        @PathVariable sessionId: String,
        @RequestBody request: SelectorRef,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} checking if selector is checked: {}", sessionId, request.selector)
        ControllerUtils.addRequestId(response)

        val managed = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        return try {
            val checked = managed.mutex.withLock {
                val driver = managed.driver
                driver.isChecked(request.selector)
            }
            ResponseEntity.ok(WebDriverResponse(value = checked))
        } catch (e: WebDriverException) {
            logger.error("isChecked check failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message)
            ControllerUtils.errorResponse("webdriver error", e.message ?: "WebDriver error")
        } catch (e: Exception) {
            logger.error("isChecked check failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message, e)
            ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
        }
    }

    /**
     * Hovers over an element.
     */
    @PostMapping("/hover", consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun hover(
        @PathVariable sessionId: String,
        @RequestBody request: SelectorRef,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} hovering over selector: {}", sessionId, request.selector)
        ControllerUtils.addRequestId(response)

        val managed = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        return try {
            managed.mutex.withLock {
                val driver = managed.driver
                driver.hover(request.selector)
            }
            ResponseEntity.ok(WebDriverResponse<Any?>(value = null))
        } catch (e: WebDriverException) {
            logger.error("Hover failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message)
            ControllerUtils.errorResponse("webdriver error", e.message ?: "WebDriver error")
        } catch (e: Exception) {
            logger.error("Hover failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message, e)
            ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
        }
    }

    /**
     * Focuses on an element.
     */
    @PostMapping("/focus", consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun focus(
        @PathVariable sessionId: String,
        @RequestBody request: SelectorRef,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} focusing on selector: {}", sessionId, request.selector)
        ControllerUtils.addRequestId(response)

        val managed = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        return try {
            managed.mutex.withLock {
                val driver = managed.driver
                driver.focus(request.selector)
            }
            ResponseEntity.ok(WebDriverResponse<Any?>(value = null))
        } catch (e: WebDriverException) {
            logger.error("Focus failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message)
            ControllerUtils.errorResponse("webdriver error", e.message ?: "WebDriver error")
        } catch (e: Exception) {
            logger.error("Focus failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message, e)
            ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
        }
    }

    /**
     * Checks a checkbox element.
     */
    @PostMapping("/check", consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun check(
        @PathVariable sessionId: String,
        @RequestBody request: SelectorRef,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} checking selector: {}", sessionId, request.selector)
        ControllerUtils.addRequestId(response)

        val managed = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        return try {
            managed.mutex.withLock {
                val driver = managed.driver
                driver.check(request.selector)
            }
            ResponseEntity.ok(WebDriverResponse<Any?>(value = null))
        } catch (e: WebDriverException) {
            logger.error("Check failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message)
            ControllerUtils.errorResponse("webdriver error", e.message ?: "WebDriver error")
        } catch (e: Exception) {
            logger.error("Check failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message, e)
            ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
        }
    }

    /**
     * Unchecks a checkbox element.
     */
    @PostMapping("/uncheck", consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun uncheck(
        @PathVariable sessionId: String,
        @RequestBody request: SelectorRef,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} unchecking selector: {}", sessionId, request.selector)
        ControllerUtils.addRequestId(response)

        val managed = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        return try {
            managed.mutex.withLock {
                val driver = managed.driver
                driver.uncheck(request.selector)
            }
            ResponseEntity.ok(WebDriverResponse<Any?>(value = null))
        } catch (e: WebDriverException) {
            logger.error("Uncheck failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message)
            ControllerUtils.errorResponse("webdriver error", e.message ?: "WebDriver error")
        } catch (e: Exception) {
            logger.error("Uncheck failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message, e)
            ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
        }
    }

    /**
     * Gets text content of the first matching element.
     */
    @PostMapping("/textContent", consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun getTextContent(
        @PathVariable sessionId: String,
        @RequestBody request: SelectorRef,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} getting text content for selector: {}", sessionId, request.selector)
        ControllerUtils.addRequestId(response)

        val managed = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        return try {
            val text = managed.mutex.withLock {
                val driver = managed.driver
                driver.selectFirstTextOrNull(request.selector)
            }
            ResponseEntity.ok(TextResponse(value = text))
        } catch (e: WebDriverException) {
            logger.error("Get text content failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message)
            ControllerUtils.errorResponse("webdriver error", e.message ?: "WebDriver error")
        } catch (e: Exception) {
            logger.error("Get text content failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message, e)
            ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
        }
    }

    /**
     * Gets text content of all matching elements.
     */
    @PostMapping("/textContentAll", consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun getTextContentAll(
        @PathVariable sessionId: String,
        @RequestBody request: SelectorRef,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} getting text content for all selectors: {}", sessionId, request.selector)
        ControllerUtils.addRequestId(response)

        val managed = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        return try {
            val texts = managed.mutex.withLock {
                val driver = managed.driver
                driver.selectTextAll(request.selector)
            }
            ResponseEntity.ok(WebDriverResponse(value = texts))
        } catch (e: WebDriverException) {
            logger.error("Get text content all failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message)
            ControllerUtils.errorResponse("webdriver error", e.message ?: "WebDriver error")
        } catch (e: Exception) {
            logger.error("Get text content all failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message, e)
            ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
        }
    }

    /**
     * Gets an attribute value of the first matching element.
     */
    @PostMapping("/attribute", consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun getAttribute(
        @PathVariable sessionId: String,
        @RequestBody request: AttributeRequest,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} getting attribute {} for selector: {}", sessionId, request.attrName, request.selector)
        ControllerUtils.addRequestId(response)

        val managed = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        return try {
            val value = managed.mutex.withLock {
                val driver = managed.driver
                driver.selectFirstAttributeOrNull(request.selector, request.attrName)
            }
            ResponseEntity.ok(AttributeResponse(value = value))
        } catch (e: WebDriverException) {
            logger.error("Get attribute failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message)
            ControllerUtils.errorResponse("webdriver error", e.message ?: "WebDriver error")
        } catch (e: Exception) {
            logger.error("Get attribute failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message, e)
            ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
        }
    }

    /**
     * Gets attribute values of all matching elements.
     */
    @PostMapping("/attributeAll", consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun getAttributeAll(
        @PathVariable sessionId: String,
        @RequestBody request: AttributeRequest,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} getting attribute {} for all selectors: {}", sessionId, request.attrName, request.selector)
        ControllerUtils.addRequestId(response)

        val managed = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        return try {
            val values = managed.mutex.withLock {
                val driver = managed.driver
                driver.selectAttributeAll(request.selector, request.attrName)
            }
            ResponseEntity.ok(WebDriverResponse(value = values))
        } catch (e: WebDriverException) {
            logger.error("Get attribute all failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message)
            ControllerUtils.errorResponse("webdriver error", e.message ?: "WebDriver error")
        } catch (e: Exception) {
            logger.error("Get attribute all failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message, e)
            ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
        }
    }
}
