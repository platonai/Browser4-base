package ai.platon.pulsar.agentic.inference.detail

import ai.platon.pulsar.agentic.ActResult
import ai.platon.pulsar.agentic.ObserveOptions
import ai.platon.pulsar.agentic.agents.AgentConfig
import ai.platon.pulsar.agentic.inference.ExtractParams
import ai.platon.pulsar.agentic.inference.ObserveParams
import ai.platon.pulsar.agentic.model.*
import ai.platon.pulsar.common.Strings
import java.lang.ref.WeakReference
import java.time.Instant
import java.util.*

/**
 * A helper class to help ActResult keeping small.
 * */
object ActResultHelper {

    fun toString(actResult: ActResult): String {
        val eval = Strings.compactInline(actResult.tcEvalValue?.toString(), 50)
        return "[${actResult.action}] expr: ${actResult.expression} eval: $eval message: ${actResult.message}"
    }

    fun failed(message: String, action: String? = null) = ActResult(false, message, action)

    fun failed(message: String, detail: DetailedActResult) = ActResult(
        false,
        message,
        detail = detail,
    )

    fun complete(actionDescription: ActionDescription): ActResult {
        val detailedActResult = DetailedActResult(actionDescription, null, true, actionDescription.summary)
        // val toolCall = ToolCall("agent", "done")
        return ActResult(
            true,
            "completed",
            actionDescription.instruction,
            null,
            detailedActResult
        )
    }
}

/**
 * Enhanced error classification for better retry strategies
 */
sealed class PerceptiveAgentError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class TransientError(message: String, cause: Throwable? = null) : PerceptiveAgentError(message, cause)
    open class PermanentError(message: String, cause: Throwable? = null) : PerceptiveAgentError(message, cause)
    class TimeoutError(message: String, cause: Throwable? = null) : PerceptiveAgentError(message, cause)
    class ResourceExhaustedError(message: String, cause: Throwable? = null) : PerceptiveAgentError(message, cause)
    class ValidationError(message: String, cause: Throwable? = null) : PerceptiveAgentError(message, cause)
}

/**
 * Performance metrics for monitoring and optimization.
 * Thread-safe implementation using atomic fields for safe concurrent updates.
 * 
 * Note: Individual fields are updated atomically, but if you need to read
 * multiple fields consistently, external synchronization is required.
 */
data class PerformanceMetrics(
    @Volatile var totalSteps: Int = 0,
    @Volatile var successfulActions: Int = 0,
    @Volatile var failedActions: Int = 0,
    // Note: These fields are immutable (val) and should be replaced with new instances
    // if updates are needed, rather than mutating in place
    val averageActionTimeMs: Double = 0.0,
    val totalExecutionTimeMs: Long = 0,
    val memoryUsageMB: Double = 0.0,
    val retryCount: Int = 0,
    val consecutiveFailures: Int = 0
) {
    /**
     * Creates a new PerformanceMetrics with updated values.
     * Use this method to safely update metrics without race conditions.
     */
    fun withUpdates(
        totalSteps: Int = this.totalSteps,
        successfulActions: Int = this.successfulActions,
        failedActions: Int = this.failedActions,
        averageActionTimeMs: Double = this.averageActionTimeMs,
        totalExecutionTimeMs: Long = this.totalExecutionTimeMs,
        memoryUsageMB: Double = this.memoryUsageMB,
        retryCount: Int = this.retryCount,
        consecutiveFailures: Int = this.consecutiveFailures
    ): PerformanceMetrics = PerformanceMetrics(
        totalSteps, successfulActions, failedActions,
        averageActionTimeMs, totalExecutionTimeMs, memoryUsageMB,
        retryCount, consecutiveFailures
    )
}

/**
 * Structured logging context for better debugging
 */
data class ExecutionContext constructor(
    var step: Int,

    var instruction: String = "",
    var screenshotB64: String? = null,

    var event: String,
    var targetUrl: String? = null,

    val agentState: AgentState,
    val stateHistory: AgentHistory,

    val config: AgentConfig,

    val sessionId: String,
    val stepStartTime: Instant = Instant.now(),
    val additionalContext: Map<String, Any> = emptyMap(),

    var baseContext: WeakReference<ExecutionContext> = WeakReference<ExecutionContext>(null)
) {
    val sid get() = sessionId.take(8)

    val uuid = UUID.randomUUID().toString()

    val prevAgentState: AgentState? get() = agentState.prevState

    fun createObserveParams(
        options: ObserveOptions,
        fromAct: Boolean,
        resolve: Boolean
    ): ObserveParams {
        return ObserveParams(
            context = this,
            returnAction = options.returnAction ?: false,
            logInferenceToFile = config.logInferenceToFile,
            fromAct = fromAct,
            resolve = resolve
        )
    }

    fun createObserveActParams(resolve: Boolean): ObserveParams {
        return ObserveParams(
            context = this,
            fromAct = true,
            returnAction = true,
            resolve = resolve,
            logInferenceToFile = config.logInferenceToFile,
        )
    }

    fun createExtractParams(schema: ExtractionSchema): ExtractParams {
        return ExtractParams(
            instruction = instruction,
            agentState = agentState,
            schema = schema,
            requestId = uuid,
            logInferenceToFile = config.logInferenceToFile,
        )
    }
}
