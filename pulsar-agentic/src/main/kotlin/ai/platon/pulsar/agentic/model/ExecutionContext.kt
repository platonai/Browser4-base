package ai.platon.pulsar.agentic.model

import ai.platon.pulsar.agentic.ObserveOptions
import ai.platon.pulsar.agentic.agents.AgentConfig
import ai.platon.pulsar.agentic.inference.ExtractParams
import ai.platon.pulsar.agentic.inference.ObserveParams
import ai.platon.pulsar.common.serialize.json.Pson
import java.time.Instant
import java.util.UUID

/**
 * Structured logging context for better debugging
 */
data class ExecutionContext constructor(
    val step: Int,

    val instruction: String = "",
    val screenshotB64: String? = null,

    val event: String,
    val targetUrl: String? = null,

    val agentState: AgentState,
    val stepStartTime: Instant = Instant.now(),
    val stateHistory: AgentHistory,

    val config: AgentConfig,
    val sessionId: String,
    val uuid: String = UUID.randomUUID().toString(),
) {
    val sid get() = sessionId.take(8)

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
            multistep = resolve
        )
    }

    fun createObserveActParams(resolve: Boolean): ObserveParams {
        return ObserveParams(
            context = this,
            fromAct = true,
            returnAction = true,
            multistep = resolve,
            logInferenceToFile = config.logInferenceToFile,
        )
    }

    fun createExtractParams(schema: ExtractionSchema): ExtractParams {
        return ExtractParams(
            instruction = instruction,
            agentState = agentState,
            schema = schema,
            requestId = uuid,
        )
    }

    fun toJson(): String {
        return Pson.toJson(this)
    }

    override fun toString(): String {
        return "step: $step, event: $event, targetUrl: $targetUrl, sessionId: $sessionId, instruction: $instruction"
    }
}
