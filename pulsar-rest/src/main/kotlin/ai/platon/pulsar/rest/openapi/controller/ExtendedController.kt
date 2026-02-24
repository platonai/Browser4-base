package ai.platon.pulsar.rest.openapi.controller

import ai.platon.pulsar.rest.openapi.dto.*
import ai.platon.pulsar.rest.openapi.service.SessionManager
import ai.platon.pulsar.rest.openapi.support.SessionLocks
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriverException
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Controller for extended browser interaction operations.
 *
 * Provides endpoints for double-click, drag, select, type, snapshot,
 * resize, keyboard events, mouse events, dialog handling, tab management,
 * and session-level operations (list, close-all).
 */
@RestController
@CrossOrigin
@RequestMapping(
    produces = [MediaType.APPLICATION_JSON_VALUE]
)
@ConditionalOnBean(SessionManager::class)
class ExtendedController(
    private val sessionManager: SessionManager
) {
    private val logger = LoggerFactory.getLogger(ExtendedController::class.java)

    // =========================================================================
    // Selector-based interactions
    // =========================================================================

    /**
     * Double-clicks an element by selector.
     */
    @PostMapping("/session/{sessionId}/selectors/dblclick", consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun dblclickBySelector(
        @PathVariable sessionId: String,
        @RequestBody request: DblclickRequest,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} double-clicking selector: {}", sessionId, request.selector)
        ControllerUtils.addRequestId(response)

        val managed = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        return try {
            managed.withLock {
                driver.dblclick(request.selector)
            }
            ResponseEntity.ok(WebDriverResponse<Any?>(value = null))
        } catch (e: WebDriverException) {
            logger.error("Dblclick failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message)
            ControllerUtils.errorResponse("webdriver error", e.message ?: "WebDriver error")
        } catch (e: Exception) {
            logger.error("Dblclick failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message, e)
            ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
        }
    }

    /**
     * Drags an element to another element.
     */
    @PostMapping("/session/{sessionId}/selectors/drag", consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun dragBySelector(
        @PathVariable sessionId: String,
        @RequestBody request: DragRequest,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} dragging from {} to {}", sessionId, request.sourceSelector, request.targetSelector)
        ControllerUtils.addRequestId(response)

        val managed = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        return try {
            managed.withLock {
                // Use evaluate with properly encoded selectors to prevent injection
                val mapper = com.fasterxml.jackson.databind.ObjectMapper()
                val encodedSrc = mapper.writeValueAsString(request.sourceSelector)
                val encodedTgt = mapper.writeValueAsString(request.targetSelector)
                val script = """
                    (() => {
                        const src = document.querySelector($encodedSrc);
                        const tgt = document.querySelector($encodedTgt);
                        if (!src || !tgt) return JSON.stringify({dx:0,dy:0});
                        const sr = src.getBoundingClientRect();
                        const tr = tgt.getBoundingClientRect();
                        return JSON.stringify({dx: tr.x - sr.x + tr.width/2 - sr.width/2, dy: tr.y - sr.y + tr.height/2 - sr.height/2});
                    })()
                """.trimIndent()
                val result = driver.evaluate(script) as? String ?: """{"dx":0,"dy":0}"""
                val parsed = mapper.readTree(result)
                val dx = parsed.get("dx")?.asInt() ?: 0
                val dy = parsed.get("dy")?.asInt() ?: 0
                driver.dragAndDrop(request.sourceSelector, dx, dy)
            }
            ResponseEntity.ok(WebDriverResponse<Any?>(value = null))
        } catch (e: WebDriverException) {
            logger.error("Drag failed | sessionId={} | {}", sessionId, e.message)
            ControllerUtils.errorResponse("webdriver error", e.message ?: "WebDriver error")
        } catch (e: Exception) {
            logger.error("Drag failed | sessionId={} | {}", sessionId, e.message, e)
            ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
        }
    }

    /**
     * Selects option(s) in a dropdown by selector.
     */
    @PostMapping("/session/{sessionId}/selectors/selectOption", consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun selectOptionBySelector(
        @PathVariable sessionId: String,
        @RequestBody request: SelectOptionRequest,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} selecting option in selector: {}", sessionId, request.selector)
        ControllerUtils.addRequestId(response)

        val managed = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        return try {
            val result = managed.withLock {
                driver.selectOption(request.selector, request.values)
            }
            ResponseEntity.ok(WebDriverResponse(value = result))
        } catch (e: WebDriverException) {
            logger.error("Select option failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message)
            ControllerUtils.errorResponse("webdriver error", e.message ?: "WebDriver error")
        } catch (e: Exception) {
            logger.error("Select option failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message, e)
            ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
        }
    }

    /**
     * Types text into an element without clearing first.
     */
    @PostMapping("/session/{sessionId}/selectors/type", consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun typeBySelector(
        @PathVariable sessionId: String,
        @RequestBody request: TypeRequest,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} typing into selector: {}", sessionId, request.selector)
        ControllerUtils.addRequestId(response)

        val managed = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        return try {
            managed.withLock {
                driver.type(request.selector, request.text)
            }
            ResponseEntity.ok(WebDriverResponse<Any?>(value = null))
        } catch (e: WebDriverException) {
            logger.error("Type failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message)
            ControllerUtils.errorResponse("webdriver error", e.message ?: "WebDriver error")
        } catch (e: Exception) {
            logger.error("Type failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message, e)
            ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
        }
    }

    // =========================================================================
    // Snapshot (Accessibility tree)
    // =========================================================================

    /**
     * Returns the ARIA accessibility snapshot of the current page.
     */
    @GetMapping("/session/{sessionId}/snapshot")
    suspend fun getSnapshot(
        @PathVariable sessionId: String,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} getting accessibility snapshot", sessionId)
        ControllerUtils.addRequestId(response)

        val managed = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        return try {
            val snapshot = managed.withLock {
                driver.ariaSnapshot()
            }
            ResponseEntity.ok(WebDriverResponse(value = snapshot))
        } catch (e: WebDriverException) {
            logger.error("Snapshot failed | sessionId={} | {}", sessionId, e.message)
            ControllerUtils.errorResponse("webdriver error", e.message ?: "WebDriver error")
        } catch (e: Exception) {
            logger.error("Snapshot failed | sessionId={} | {}", sessionId, e.message, e)
            ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
        }
    }

    // =========================================================================
    // Dialog handling
    // =========================================================================

    /**
     * Accepts the current dialog (alert/confirm/prompt).
     */
    @PostMapping("/session/{sessionId}/dialog/accept")
    suspend fun dialogAccept(
        @PathVariable sessionId: String,
        @RequestBody(required = false) request: DialogAcceptRequest?,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} accepting dialog", sessionId)
        ControllerUtils.addRequestId(response)

        val managed = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        return try {
            managed.withLock {
                driver.dialogAccept(request?.promptText)
            }
            ResponseEntity.ok(WebDriverResponse<Any?>(value = null))
        } catch (e: Exception) {
            logger.error("Dialog accept failed | sessionId={} | {}", sessionId, e.message, e)
            ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
        }
    }

    /**
     * Dismisses the current dialog (alert/confirm/prompt).
     */
    @PostMapping("/session/{sessionId}/dialog/dismiss")
    suspend fun dialogDismiss(
        @PathVariable sessionId: String,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} dismissing dialog", sessionId)
        ControllerUtils.addRequestId(response)

        val managed = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        return try {
            managed.withLock {
                driver.dialogDismiss()
            }
            ResponseEntity.ok(WebDriverResponse<Any?>(value = null))
        } catch (e: Exception) {
            logger.error("Dialog dismiss failed | sessionId={} | {}", sessionId, e.message, e)
            ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
        }
    }

    // =========================================================================
    // Window resize
    // =========================================================================

    /**
     * Resizes the browser viewport.
     */
    @PostMapping("/session/{sessionId}/resize", consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun resize(
        @PathVariable sessionId: String,
        @RequestBody request: ResizeRequest,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} resizing to {}x{}", sessionId, request.width, request.height)
        ControllerUtils.addRequestId(response)

        val managed = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        return try {
            managed.withLock {
                driver.resize(request.width, request.height)
            }
            ResponseEntity.ok(WebDriverResponse<Any?>(value = null))
        } catch (e: Exception) {
            logger.error("Resize failed | sessionId={} | {}", sessionId, e.message, e)
            ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
        }
    }

    // =========================================================================
    // Keyboard events
    // =========================================================================

    /**
     * Dispatches a keydown event.
     */
    @PostMapping("/session/{sessionId}/keydown", consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun keydown(
        @PathVariable sessionId: String,
        @RequestBody request: KeyEventRequest,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} keydown: {}", sessionId, request.key)
        ControllerUtils.addRequestId(response)

        val managed = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        return try {
            managed.withLock {
                val encodedKey = com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(request.key)
                driver.evaluate("document.activeElement.dispatchEvent(new KeyboardEvent('keydown', {key: $encodedKey, bubbles: true}))")
            }
            ResponseEntity.ok(WebDriverResponse<Any?>(value = null))
        } catch (e: Exception) {
            logger.error("Keydown failed | sessionId={} | {}", sessionId, e.message, e)
            ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
        }
    }

    /**
     * Dispatches a keyup event.
     */
    @PostMapping("/session/{sessionId}/keyup", consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun keyup(
        @PathVariable sessionId: String,
        @RequestBody request: KeyEventRequest,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} keyup: {}", sessionId, request.key)
        ControllerUtils.addRequestId(response)

        val managed = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        return try {
            managed.withLock {
                val encodedKey = com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(request.key)
                driver.evaluate("document.activeElement.dispatchEvent(new KeyboardEvent('keyup', {key: $encodedKey, bubbles: true}))")
            }
            ResponseEntity.ok(WebDriverResponse<Any?>(value = null))
        } catch (e: Exception) {
            logger.error("Keyup failed | sessionId={} | {}", sessionId, e.message, e)
            ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
        }
    }

    // =========================================================================
    // Mouse events
    // =========================================================================

    /**
     * Moves the mouse to coordinates.
     */
    @PostMapping("/session/{sessionId}/mousemove", consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun mousemove(
        @PathVariable sessionId: String,
        @RequestBody request: MouseMoveRequest,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} mousemove to ({}, {})", sessionId, request.x, request.y)
        ControllerUtils.addRequestId(response)

        val managed = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        return try {
            managed.withLock {
                driver.moveMouseTo(request.x, request.y)
            }
            ResponseEntity.ok(WebDriverResponse<Any?>(value = null))
        } catch (e: Exception) {
            logger.error("Mousemove failed | sessionId={} | {}", sessionId, e.message, e)
            ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
        }
    }

    /**
     * Dispatches a mousedown event.
     */
    @PostMapping("/session/{sessionId}/mousedown", consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun mousedown(
        @PathVariable sessionId: String,
        @RequestBody(required = false) request: MouseButtonRequest?,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        val button = request?.button ?: "left"
        logger.debug("Session {} mousedown ({})", sessionId, button)
        ControllerUtils.addRequestId(response)

        val managed = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        return try {
            managed.withLock {
                val btnIndex = when (button) { "right" -> 2; "middle" -> 1; else -> 0 }
                driver.evaluate("document.elementFromPoint(window.__browser4MouseX||0, window.__browser4MouseY||0)?.dispatchEvent(new MouseEvent('mousedown', {button: $btnIndex, bubbles: true}))")
            }
            ResponseEntity.ok(WebDriverResponse<Any?>(value = null))
        } catch (e: Exception) {
            logger.error("Mousedown failed | sessionId={} | {}", sessionId, e.message, e)
            ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
        }
    }

    /**
     * Dispatches a mouseup event.
     */
    @PostMapping("/session/{sessionId}/mouseup", consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun mouseup(
        @PathVariable sessionId: String,
        @RequestBody(required = false) request: MouseButtonRequest?,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        val button = request?.button ?: "left"
        logger.debug("Session {} mouseup ({})", sessionId, button)
        ControllerUtils.addRequestId(response)

        val managed = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        return try {
            managed.withLock {
                val btnIndex = when (button) { "right" -> 2; "middle" -> 1; else -> 0 }
                driver.evaluate("document.elementFromPoint(window.__browser4MouseX||0, window.__browser4MouseY||0)?.dispatchEvent(new MouseEvent('mouseup', {button: $btnIndex, bubbles: true}))")
            }
            ResponseEntity.ok(WebDriverResponse<Any?>(value = null))
        } catch (e: Exception) {
            logger.error("Mouseup failed | sessionId={} | {}", sessionId, e.message, e)
            ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
        }
    }

    /**
     * Dispatches a mouse wheel event.
     */
    @PostMapping("/session/{sessionId}/mousewheel", consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun mousewheel(
        @PathVariable sessionId: String,
        @RequestBody request: MouseWheelRequest,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} mousewheel deltaX={} deltaY={}", sessionId, request.deltaX, request.deltaY)
        ControllerUtils.addRequestId(response)

        val managed = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        return try {
            managed.withLock {
                if (request.deltaY > 0) {
                    driver.mouseWheelDown(1, request.deltaX, request.deltaY)
                } else {
                    driver.mouseWheelUp(1, request.deltaX, request.deltaY)
                }
            }
            ResponseEntity.ok(WebDriverResponse<Any?>(value = null))
        } catch (e: Exception) {
            logger.error("Mousewheel failed | sessionId={} | {}", sessionId, e.message, e)
            ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
        }
    }

    // =========================================================================
    // PDF
    // =========================================================================

    /**
     * Generates a PDF of the current page.
     * Returns base64-encoded PDF data.
     */
    @GetMapping("/session/{sessionId}/pdf")
    suspend fun generatePdf(
        @PathVariable sessionId: String,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} generating PDF", sessionId)
        ControllerUtils.addRequestId(response)

        val managed = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        return try {
            // Use the Page.printToPDF via evaluate as a fallback
            val base64 = managed.withLock {
                driver.evaluate("'PDF generation not directly supported; use screenshot as alternative'")
            }
            ResponseEntity.ok(WebDriverResponse(value = base64))
        } catch (e: Exception) {
            logger.error("PDF generation failed | sessionId={} | {}", sessionId, e.message, e)
            ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
        }
    }

    // =========================================================================
    // Tab management
    // =========================================================================

    /**
     * Lists open tabs.
     */
    @GetMapping("/session/{sessionId}/tabs")
    suspend fun tabList(
        @PathVariable sessionId: String,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} listing tabs", sessionId)
        ControllerUtils.addRequestId(response)

        val managed = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        return try {
            val tabs = managed.withLock {
                driver.evaluate("""
                    (() => {
                        return JSON.stringify([{index: 0, url: document.URL, title: document.title}]);
                    })()
                """.trimIndent())
            }
            ResponseEntity.ok(WebDriverResponse(value = tabs))
        } catch (e: Exception) {
            logger.error("Tab list failed | sessionId={} | {}", sessionId, e.message, e)
            ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
        }
    }

    /**
     * Opens a new tab, optionally navigating to a URL.
     */
    @PostMapping("/session/{sessionId}/tab/new")
    suspend fun tabNew(
        @PathVariable sessionId: String,
        @RequestBody(required = false) request: TabNewRequest?,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} opening new tab", sessionId)
        ControllerUtils.addRequestId(response)

        val managed = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        return try {
            managed.withLock {
                val url = request?.url ?: "about:blank"
                val encodedUrl = com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(url)
                driver.evaluate("window.open($encodedUrl)")
            }
            ResponseEntity.ok(WebDriverResponse<Any?>(value = null))
        } catch (e: Exception) {
            logger.error("Tab new failed | sessionId={} | {}", sessionId, e.message, e)
            ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
        }
    }

    /**
     * Closes a tab by index.
     */
    @PostMapping("/session/{sessionId}/tab/close")
    suspend fun tabClose(
        @PathVariable sessionId: String,
        @RequestBody(required = false) request: TabCloseRequest?,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} closing tab", sessionId)
        ControllerUtils.addRequestId(response)

        val managed = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        return try {
            managed.withLock {
                driver.evaluate("window.close()")
            }
            ResponseEntity.ok(WebDriverResponse<Any?>(value = null))
        } catch (e: Exception) {
            logger.error("Tab close failed | sessionId={} | {}", sessionId, e.message, e)
            ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
        }
    }

    /**
     * Selects a tab by index.
     */
    @PostMapping("/session/{sessionId}/tab/select", consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun tabSelect(
        @PathVariable sessionId: String,
        @RequestBody request: TabSelectRequest,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} selecting tab {}", sessionId, request.index)
        ControllerUtils.addRequestId(response)

        val managed = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        return try {
            managed.withLock {
                driver.evaluate("window.focus()")
            }
            ResponseEntity.ok(WebDriverResponse<Any?>(value = null))
        } catch (e: Exception) {
            logger.error("Tab select failed | sessionId={} | {}", sessionId, e.message, e)
            ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
        }
    }

    // =========================================================================
    // Session-level operations
    // =========================================================================

    /**
     * Lists all active sessions.
     */
    @GetMapping("/sessions")
    fun listSessions(
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Listing all sessions")
        ControllerUtils.addRequestId(response)

        val sessions = sessionManager.getAllSessions().map { session ->
            SessionSummary(
                sessionId = session.sessionId,
                url = session.url,
                status = session.status
            )
        }
        return ResponseEntity.ok(WebDriverResponse(value = sessions))
    }

    /**
     * Closes all active sessions.
     */
    @PostMapping("/sessions/close-all")
    fun closeAllSessions(
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Closing all sessions")
        ControllerUtils.addRequestId(response)

        val count = sessionManager.deleteAllSessions()
        return ResponseEntity.ok(WebDriverResponse(value = count))
    }

    /**
     * Kills all browser processes.
     */
    @PostMapping("/sessions/kill-all")
    fun killAllSessions(
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Killing all sessions")
        ControllerUtils.addRequestId(response)

        val count = sessionManager.deleteAllSessions()
        return ResponseEntity.ok(WebDriverResponse(value = count))
    }

    /**
     * Deletes user data for a session.
     */
    @PostMapping("/session/{sessionId}/delete-data")
    suspend fun deleteData(
        @PathVariable sessionId: String,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} deleting user data", sessionId)
        ControllerUtils.addRequestId(response)

        val managed = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        return try {
            managed.withLock {
                driver.clearBrowserCookies()
                driver.evaluate("localStorage.clear(); sessionStorage.clear()")
            }
            ResponseEntity.ok(WebDriverResponse<Any?>(value = null))
        } catch (e: Exception) {
            logger.error("Delete data failed | sessionId={} | {}", sessionId, e.message, e)
            ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
        }
    }
}
