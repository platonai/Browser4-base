package ai.platon.pulsar.agentic.inference

import ai.platon.browser4.driver.chrome.dom.DomService
import ai.platon.pulsar.agentic.AgenticSession
import ai.platon.pulsar.agentic.event.AgentEventBus
import ai.platon.pulsar.agentic.event.AgenticEvents
import ai.platon.pulsar.agentic.inference.action.ContextToAction
import ai.platon.pulsar.agentic.inference.detail.ExecutionContext
import ai.platon.pulsar.agentic.model.ActionDescription
import ai.platon.pulsar.agentic.model.AgentState
import ai.platon.pulsar.agentic.model.ExtractionSchema
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.MultiSinkMessageWriter
import ai.platon.pulsar.common.event.EventBus
import ai.platon.pulsar.common.serialize.json.Pson
import ai.platon.pulsar.common.serialize.json.pulsarObjectMapper
import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.skeleton.crawl.fetch.driver.AbstractWebDriver
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import java.nio.file.Path
import java.time.Instant
import java.util.*

data class ExtractParams(
    val instruction: String,
    val agentState: AgentState,
    val schema: ExtractionSchema,
    val requestId: String = UUID.randomUUID().toString(),
    val userProvidedInstructions: String? = null,
)

data class ObserveParams(
    val context: ExecutionContext,
    /**
     * User provided additional system instructions
     * */
    val userProvidedInstructions: String? = null,
    val returnAction: Boolean = false,
    val multistep: Boolean = false,
    val logInferenceToFile: Boolean = false,
    val fromAct: Boolean = false,
)

class InferenceEngine(
    private val session: AgenticSession
) {
    private val cta = ContextToAction(session.sessionConfig)
    private val auxLogDir: Path get() = AppPaths.detectAuxiliaryLogDir().resolve("agent")
    private val auxLogger by lazy { MultiSinkMessageWriter(auxLogDir) }

    val domService: DomService
        get() = (session.getOrCreateBoundDriver() as? AbstractWebDriver)?.domService
            ?: throw IllegalStateException("Bound driver is not AbstractWebDriver")

    suspend fun observe(params: ObserveParams, context: ExecutionContext): ActionDescription {
        val messages = InferencePromptBuilder.buildObserveMessages(params)

        val startTime = Instant.now()
        val actionType = if (params.fromAct) "act" else "observe"
        val timestamp = AppPaths.fromNow()

        val llmInputFile = log(
            subdirectory = actionType,
            filename = "$timestamp.request.json",
            requestId = context.uuid,
            messages = messages.messages
        )

        onWillInfer(context, messages, actionType)

        val actionDescription = cta.generate(messages, context)
        requireNotNull(context.agentState.actionDescription) {
            "Field should be set: context.agentState.actionDescription"
        }
        val modelResponse = requireNotNull(actionDescription.modelResponse) {
            "Field should be set: actionDescription.modelResponse"
        }

        val inferenceTimeMillis = DateTimes.elapsedTime(startTime).toMillis()

        onDidInfer(context, messages, actionDescription, actionType, inferenceTimeMillis)

        val llmOutputFile = log(
            subdirectory = actionType,
            filename = "$timestamp.response.json",
            payload = mapOf(
                "requestId" to context.uuid,
                "modelResponse" to modelResponse
            )
        )

        logSummary(
            filename = "$actionType.jsonl",
            payload = mapOf(
                "${actionType}InferenceType" to actionType,
                "timestamp" to timestamp,
                "llmInputFile" to llmInputFile,
                "llmOutputFile" to llmOutputFile,
                "inputTokenCount" to modelResponse.tokenUsage.inputTokenCount,
                "outputTokenCount" to modelResponse.tokenUsage.outputTokenCount,
                "totalTokenCount" to modelResponse.tokenUsage.totalTokenCount,
                "inferenceTimeMillis" to DateTimes.elapsedTime(startTime).toMillis()
            )
        )

        return actionDescription
    }

    /**
     * Returns an ObjectNode with extracted fields expanded at top-level, plus:
     *   - metadata: { progress, completed }
     *   - inputTokenCount, outputTokenCount, totalTokenCount, inferenceTimeMillis
     */
    suspend fun extract(params: ExtractParams): ObjectNode {
        onWillExtractInfer(params)

        val messages = InferencePromptBuilder.buildExtractPrompt(params)

        // 1) Extraction call -----------------------------------------------------------------
        val timestamp = AppPaths.fromNow()
        val filename = "extract.jsonl"
        val llmInputFile: Path? = log(
            subdirectory = "extract",
            filename = filename,
            requestId = params.requestId,
            messages = messages.messages
        )

        val extractStartTime = Instant.now()
        val extractResponse: ModelResponse = cta.generateResponseRaw(messages)

        val extractedNode: ObjectNode = runCatching {
            pulsarObjectMapper().readTree(extractResponse.content) as? ObjectNode
                ?: JsonNodeFactory.instance.objectNode()
        }.getOrElse { JsonNodeFactory.instance.objectNode() }

        val extractOutputFile: Path = log(
            subdirectory = "extract",
            filename = filename,
            payload = mapOf(
                "requestId" to params.requestId,
                "response" to extractedNode
            )
        )

        logSummary(
            filename = filename,
            payload = mapOf(
                "extractInferenceType" to "extract",
                "timestamp" to timestamp,
                "llmInputFile" to llmInputFile,
                "llmOutputFile" to extractOutputFile,
                "inputTokenCount" to extractResponse.tokenUsage.inputTokenCount,
                "outputTokenCount" to extractResponse.tokenUsage.outputTokenCount,
                "totalTokenCount" to extractResponse.tokenUsage.totalTokenCount,
                "inferenceTimeMillis" to DateTimes.elapsedTime(extractStartTime).toMillis()
            )
        )

        // 2) Metadata call -------------------------------------------------------------------
        val metadataMessages = InferencePromptBuilder.buildMetadataPrompt(params, extractedNode)

        val metadataInputFile = log(
            subdirectory = "extract",
            filename = filename,
            requestId = params.requestId,
            messages = metadataMessages.messages
        )

        val metadataStartTime = Instant.now()
        val metadataResponse = cta.generateResponseRaw(metadataMessages)

        val metaNode: ObjectNode = runCatching {
            pulsarObjectMapper().readTree(metadataResponse.content) as? ObjectNode
                ?: JsonNodeFactory.instance.objectNode()
        }.getOrElse { JsonNodeFactory.instance.objectNode() }
        val progress = metaNode.path("progress").asText("")
        val completed = metaNode.path("completed").asBoolean(false)

        val metadataOutputFile: Path = log(
            subdirectory = "extract",
            filename = filename,
            payload = mapOf(
                "requestId" to params.requestId,
                "modelResponse" to Pson.toJsonOrNull(metadataResponse.content),
                "completed" to completed,
                "progress" to progress,
            )
        )

        logSummary(
            filename = "extract.jsonl",
            payload = mapOf(
                "extractInferenceType" to "metadata",
                "timestamp" to timestamp,
                "inputTokenCount" to metadataResponse.tokenUsage.inputTokenCount,
                "outputTokenCount" to metadataResponse.tokenUsage.outputTokenCount,
                "totalTokenCount" to metadataResponse.tokenUsage.totalTokenCount,
                "inferenceTimeMillis" to DateTimes.elapsedTime(metadataStartTime).toMillis()
            )
        )

        val usage1 = extractResponse.tokenUsage
        val usage2 = metadataResponse.tokenUsage
        val inputTokenCount = usage1.inputTokenCount + usage2.inputTokenCount
        val outputTokenCount = usage1.outputTokenCount + usage2.outputTokenCount
        val totalTokenCount = usage1.totalTokenCount + usage2.totalTokenCount

        val totalInferenceTimeMillis = DateTimes.elapsedTime(extractStartTime).toMillis()

        val result: ObjectNode = (extractedNode.deepCopy()).apply {
            set<ObjectNode>("metadata", JsonNodeFactory.instance.objectNode().apply {
                put("progress", progress)
                put("completed", completed)
            })
            put("inputTokenCount", inputTokenCount)
            put("outputTokenCount", outputTokenCount)
            put("totalTokenCount", totalTokenCount)
            put("inferenceTimeMillis", totalInferenceTimeMillis)
        }

        onDidExtractInfer(params, result, extractedNode, metaNode, completed, progress, totalInferenceTimeMillis,
            inputTokenCount, outputTokenCount, totalTokenCount)

        return result
    }

    suspend fun summarize(instruction: String?, textContent: String): String {
        val messages = InferencePromptBuilder.buildSummaryPrompt(instruction, textContent)
        
        val startTime = Instant.now()

        onWillSummarizeInfer(instruction, messages, textContent)

        val response = cta.generateResponseRaw(messages)
        
        val inferenceTimeMillis = DateTimes.elapsedTime(startTime).toMillis()

        onDidSummarizeInfer(instruction, textContent, response, inferenceTimeMillis)

        // TODO: count token usage

        return response.content
    }

    // ------------------------------ Event Handler Methods --------------------------------

    private fun onWillInfer(context: ExecutionContext, messages: AgentMessageList, actionType: String) {
        // Emit AgentEventBus inference event
        AgentEventBus.emitInferenceEvent(
            eventType = AgenticEvents.InferenceEventTypes.ON_WILL_INFER,
            agentId = context.uuid,
            message = "Starting LLM inference for $actionType",
            metadata = mapOf(
                "context" to context.sid,
                "step" to context.step,
                "actionType" to actionType
            )
        )

        EventBus.emit(
            AgenticEvents.ContextToAction.GENERATE_WILL_EXECUTE, mapOf(
                "context" to context,
                "messages" to messages
            )
        )
    }

    private fun onDidInfer(
        context: ExecutionContext,
        messages: AgentMessageList,
        actionDescription: ActionDescription,
        actionType: String,
        inferenceTimeMillis: Long
    ) {
        val modelResponse = actionDescription.modelResponse!!

        // Emit AgentEventBus inference event
        AgentEventBus.emitInferenceEvent(
            eventType = AgenticEvents.InferenceEventTypes.ON_DID_INFER,
            agentId = context.uuid,
            message = "LLM inference completed for $actionType",
            metadata = mapOf(
                "context" to context.sid,
                "step" to context.step,
                "actionType" to actionType,
                "duration" to inferenceTimeMillis,
                "inputTokenCount" to modelResponse.tokenUsage.inputTokenCount,
                "outputTokenCount" to modelResponse.tokenUsage.outputTokenCount,
                "totalTokenCount" to modelResponse.tokenUsage.totalTokenCount
            )
        )

        EventBus.emit(
            AgenticEvents.ContextToAction.GENERATE_DID_EXECUTE, mapOf(
                "context" to context,
                "messages" to messages,
                "actionDescription" to actionDescription
            )
        )
    }

    private fun onWillExtractInfer(params: ExtractParams) {
        // Emit AgentEventBus inference event
        AgentEventBus.emitInferenceEvent(
            eventType = AgenticEvents.InferenceEventTypes.ON_WILL_EXTRACT_INFER,
            agentId = params.requestId,
            message = "Starting extraction inference",
            metadata = mapOf(
                "instruction" to params.instruction.take(100),
                "requestId" to params.requestId
            )
        )

        EventBus.emit(
            AgenticEvents.InferenceEngine.EXTRACT_WILL_EXECUTE, mapOf(
                "params" to params
            )
        )
    }

    private fun onDidExtractInfer(
        params: ExtractParams,
        result: ObjectNode,
        extractedNode: ObjectNode,
        metaNode: ObjectNode,
        completed: Boolean,
        progress: String,
        totalInferenceTimeMillis: Long,
        inputTokenCount: Int,
        outputTokenCount: Int,
        totalTokenCount: Int
    ) {
        // Emit AgentEventBus inference event
        AgentEventBus.emitInferenceEvent(
            eventType = AgenticEvents.InferenceEventTypes.ON_DID_EXTRACT_INFER,
            agentId = params.requestId,
            message = "Extraction inference completed",
            metadata = mapOf(
                "requestId" to params.requestId,
                "completed" to completed,
                "progress" to progress,
                "duration" to totalInferenceTimeMillis,
                "inputTokenCount" to inputTokenCount,
                "outputTokenCount" to outputTokenCount,
                "totalTokenCount" to totalTokenCount
            )
        )

        EventBus.emit(
            AgenticEvents.InferenceEngine.EXTRACT_DID_EXECUTE, mapOf(
                "params" to params,
                "result" to result,
                "extractedNode" to extractedNode,
                "metaNode" to metaNode
            )
        )
    }

    private fun onWillSummarizeInfer(instruction: String?, messages: AgentMessageList, textContent: String) {
        // Emit AgentEventBus inference event
        AgentEventBus.emitInferenceEvent(
            eventType = AgenticEvents.InferenceEventTypes.ON_WILL_SUMMARIZE_INFER,
            agentId = null,
            message = "Starting summarization inference",
            metadata = mapOf(
                "instruction" to instruction,
                "textContentLength" to textContent.length
            )
        )

        EventBus.emit(
            AgenticEvents.InferenceEngine.SUMMARIZE_WILL_EXECUTE, mapOf(
                "instruction" to instruction,
                "messages" to messages,
                "textContent" to textContent,
            )
        )
    }

    private fun onDidSummarizeInfer(instruction: String?, textContent: String, response: ModelResponse, inferenceTimeMillis: Long) {
        // Emit AgentEventBus inference event
        AgentEventBus.emitInferenceEvent(
            eventType = AgenticEvents.InferenceEventTypes.ON_DID_SUMMARIZE_INFER,
            agentId = null,
            message = "Summarization inference completed",
            metadata = mapOf(
                "instruction" to instruction,
                "resultLength" to response.content.length,
                "duration" to inferenceTimeMillis,
                "inputTokenCount" to response.tokenUsage.inputTokenCount,
                "outputTokenCount" to response.tokenUsage.outputTokenCount,
                "totalTokenCount" to response.tokenUsage.totalTokenCount
            )
        )

        EventBus.emit(
            AgenticEvents.InferenceEngine.SUMMARIZE_DID_EXECUTE, mapOf(
                "instruction" to instruction,
                "textContentLength" to textContent.length,
                "result" to response.content,
                "tokenUsage" to response.tokenUsage
            )
        )
    }

    // ------------------------------ Small utilities --------------------------------

    private fun logSummary(filename: String, payload: Map<String, Any?>): Path {
        val path = auxLogDir.resolve("summary").resolve(filename)
        return auxLogger.writeTo(payload, path)
    }

    private fun log(subdirectory: String, filename: String, payload: Map<String, Any?>): Path {
        val path = auxLogDir.resolve(subdirectory).resolve(filename)
        return auxLogger.writeTo(payload, path)
    }

    private fun log(
        subdirectory: String, requestId: String, filename: String,
        messages: List<Any>, enabled: Boolean = true
    ): Path? {
        if (!enabled) return null

        val payload = mapOf("requestId" to requestId, "messages" to messages)
        val path = auxLogDir.resolve(subdirectory).resolve(filename)
        return auxLogger.writeTo(payload, path)
    }
}
