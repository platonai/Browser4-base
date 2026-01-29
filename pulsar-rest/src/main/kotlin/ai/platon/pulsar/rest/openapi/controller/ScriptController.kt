package ai.platon.pulsar.rest.openapi.controller

import ai.platon.pulsar.rest.openapi.dto.ScriptRequest
import ai.platon.pulsar.rest.openapi.dto.ScriptResponse
import ai.platon.pulsar.rest.openapi.service.SessionManager
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriverException
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Controller for JavaScript execution.
 */
@RestController
@CrossOrigin
@RequestMapping(
    "/session/{sessionId}/execute",
    produces = [MediaType.APPLICATION_JSON_VALUE]
)
@ConditionalOnBean(SessionManager::class)
class ScriptController(
    private val sessionManager: SessionManager
) {
    private val logger = LoggerFactory.getLogger(ScriptController::class.java)

    /**
     * Executes a synchronous script.
     */
    @PostMapping("/sync", consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun executeSync(
        @PathVariable sessionId: String,
        @RequestBody request: ScriptRequest,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} executing sync script: {}", sessionId, request.script.take(100))
        ControllerUtils.addRequestId(response)

        val managed = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        return try {
            val result = managed.withLock {
                driver.evaluate(request.script)
            }
            ResponseEntity.ok(ScriptResponse(value = result))
        } catch (e: WebDriverException) {
            logger.error("Script execution failed | sessionId={} | {}", sessionId, e.message)
            ControllerUtils.errorResponse("webdriver error", e.message ?: "WebDriver error")
        } catch (e: Exception) {
            logger.error("Script execution failed | sessionId={} | {}", sessionId, e.message, e)
            ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
        }
    }

    /**
     * Executes an asynchronous script.
     */
    @PostMapping("/async", consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun executeAsync(
        @PathVariable sessionId: String,
        @RequestBody request: ScriptRequest,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} executing async script: {}", sessionId, request.script.take(100))
        ControllerUtils.addRequestId(response)

        val managed = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        return try {
            val result = managed.withLock {
                // For async scripts, we use the same evaluate method
                // The caller is responsible for proper async handling in the script
                driver.evaluate(request.script)
            }
            ResponseEntity.ok(ScriptResponse(value = result))
        } catch (e: WebDriverException) {
            logger.error("Async script execution failed | sessionId={} | {}", sessionId, e.message)
            ControllerUtils.errorResponse("webdriver error", e.message ?: "WebDriver error")
        } catch (e: Exception) {
            logger.error("Async script execution failed | sessionId={} | {}", sessionId, e.message, e)
            ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
        }
    }
}
