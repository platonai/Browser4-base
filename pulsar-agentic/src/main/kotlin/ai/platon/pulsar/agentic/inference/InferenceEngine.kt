package ai.platon.pulsar.agentic.inference

import ai.platon.browser4.driver.chrome.dom.DomService
import ai.platon.pulsar.agentic.AgenticSession
import ai.platon.pulsar.agentic.inference.action.ContextToAction
import ai.platon.pulsar.agentic.inference.detail.ExecutionContext
import ai.platon.pulsar.agentic.model.ActionDescription
import ai.platon.pulsar.agentic.model.AgentState
import ai.platon.pulsar.agentic.model.ExtractionSchema
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.MultiSinkMessageWriter
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.event.DangerousEventBus
import ai.platon.pulsar.common.serialize.json.pulsarObjectMapper
import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.skeleton.crawl.fetch.driver.AbstractWebDriver
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import java.nio.file.Path
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap

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
    val multiStep: Boolean = false,
    val logInferenceToFile: Boolean = false,
    val fromAct: Boolean = false,
)

object InferenceMessageBuilder {

    private val promptBuilder = PromptBuilder()

    fun buildObserveMessages(
        params: ObserveParams
    ): AgentMessageList {
        return if (params.multiStep) {
            promptBuilder.buildMultiStepAgentMessageListAll(params.context)
        } else {
            promptBuilder.buildObserveMessageListAll(params, params.context)
        }
    }

    fun buildExtractPrompt(params: ExtractParams): AgentMessageList {
        val messages = AgentMessageList()

        messages.addLast(promptBuilder.buildExtractSystemPrompt(params.userProvidedInstructions))
        messages.addUser(promptBuilder.buildExtractUserRequestPrompt(params), "user_request")
        messages.addLast(promptBuilder.buildExtractUserPrompt(params))

        return messages
    }

    fun buildMetadataPrompt(
        params: ExtractParams,
        extractedNode: ObjectNode,
    ): AgentMessageList {
        val metadataMessages = AgentMessageList()
        val metadataSystem = promptBuilder.buildMetadataSystemPrompt()
        // For metadata, pass the extracted object directly
        val metadataUser = promptBuilder.buildMetadataUserPrompt(params.instruction, extractedNode, params.agentState)

        metadataMessages.addLast(metadataSystem)
        metadataMessages.addLast(metadataUser)

        return metadataMessages
    }

    fun buildSummaryPrompt(instruction: String?, textContent: String): AgentMessageList {
        val messages = AgentMessageList()

        if (instruction.isNullOrBlank()) {
            messages.addUser("对下述文本给出一个总结。")
        } else {
            messages.addUser("根据用户指令，对下述文本给出一个总结。")
            messages.addUser("""<user_request>$instruction</user_request>""")
        }
        messages.addUser("\n\n$textContent\n\n".trimIndent())

        return messages
    }
}

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
        val messages = InferenceMessageBuilder.buildObserveMessages(params)

        val startTime = Instant.now()
        val prefix = if (params.fromAct) "act" else "observe"
        val timestamp = AppPaths.fromNow()

        val callFile: Path? = log(
            dirPrefix = "${prefix}-request-summary",
            requestId = context.uuid,
            timestamp = timestamp,
            modelCall = prefix,
            messages = messages.messages
        )

        DangerousEventBus.emit("ContextToAction.generate.willExecute", mapOf(
            "context" to context,
            "messages" to messages
        ))

        val actionDescription = cta.generate(messages, context)
        requireNotNull(context.agentState.actionDescription) {
            "Field should be set: context.agentState.actionDescription"
        }
        val modelResponse = requireNotNull(actionDescription.modelResponse) {
            "Field should be set: actionDescription.modelResponse"
        }

        DangerousEventBus.emit("ContextToAction.generate.didExecute", mapOf(
            "context" to context,
            "messages" to messages,
            "actionDescription" to actionDescription
        ))

        val tokenUsage = modelResponse.tokenUsage
        val responseContent = modelResponse.content

        val respFile = log(
            prefix = "${prefix}-response-summary",
            suffix = "$timestamp.json",
            payload = mapOf(
                "requestId" to context.uuid,
                "modelResponse" to prefix,
                "rawResponse" to safeJsonPreview(responseContent)
            )
        )

        logSummary(
            prefix = prefix,
            payload = mapOf(
                "${prefix}InferenceType" to prefix,
                "timestamp" to timestamp,
                "llmInputFile" to callFile,
                "llmOutputFile" to respFile,
                "inputTokenCount" to tokenUsage.inputTokenCount,
                "outputTokenCount" to tokenUsage.outputTokenCount,
                "totalTokenCount" to tokenUsage.totalTokenCount,
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
        DangerousEventBus.emit("InferenceEngine.extract.willExecute", mapOf(
            "params" to params
        ))

        val messages = InferenceMessageBuilder.buildExtractPrompt(params)

        // 1) Extraction call -----------------------------------------------------------------
        val timestamp = AppPaths.fromNow()
        val callFile: Path? = log(
            dirPrefix = "extract-request-summary",
            timestamp = timestamp,
            requestId = params.requestId,
            modelCall = "extract",
            messages = messages.messages
        )

        val extractStartTime = Instant.now()
        val extractResponse: ModelResponse = cta.generateResponseRaw(messages)

        val extractedNode: ObjectNode = runCatching {
            pulsarObjectMapper().readTree(extractResponse.content) as? ObjectNode
                ?: JsonNodeFactory.instance.objectNode()
        }.getOrElse { JsonNodeFactory.instance.objectNode() }

        val extractRespFile: Path = log(
            prefix = "extract-response-summary",
            suffix = "$timestamp.json",
            payload = mapOf(
                "requestId" to params.requestId,
                "modelCall" to "extract",
                "rawResponse" to safeJsonPreview(extractResponse.content),
            )
        )

        logSummary(
            prefix = "extract",
            payload = mapOf(
                "extractInferenceType" to "extract",
                "timestamp" to timestamp,
                "llmInputFile" to callFile,
                "llmOutputFile" to extractRespFile,
                "inputTokenCount" to extractResponse.tokenUsage.inputTokenCount,
                "outputTokenCount" to extractResponse.tokenUsage.outputTokenCount,
                "totalTokenCount" to extractResponse.tokenUsage.totalTokenCount,
                "inferenceTimeMillis" to DateTimes.elapsedTime(extractStartTime).toMillis()
            )
        )

        // 2) Metadata call -------------------------------------------------------------------
        val metadataMessages = InferenceMessageBuilder.buildMetadataPrompt(params, extractedNode)

        val metadataCallFile = log(
            dirPrefix = "extract-summary",
            timestamp = timestamp,
            requestId = params.requestId,
            modelCall = "metadata",
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

        val metadataRespFile: Path = log(
            prefix = "extract-metadata-summary",
            suffix = "$timestamp.json",
            payload = mapOf(
                "requestId" to params.requestId,
                "modelResponse" to "metadata",
                "completed" to completed,
                "progress" to progress,
            )
        )

        logSummary(
            prefix = "extract",
            payload = mapOf(
                "extractInferenceType" to "metadata",
                "timestamp" to timestamp,
                "llmInputFile" to metadataCallFile,
                "llmOutputFile" to metadataRespFile,
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

        DangerousEventBus.emit("InferenceEngine.extract.didExecute", mapOf(
            "params" to params,
            "result" to result,
            "extractedNode" to extractedNode,
            "metaNode" to metaNode
        ))

        return result
    }

    suspend fun summarize(instruction: String?, textContent: String): String {
        val messages = InferenceMessageBuilder.buildSummaryPrompt(instruction, textContent)

        DangerousEventBus.emit("InferenceEngine.summarize.willExecute", mapOf(
            "instruction" to instruction,
            "messages" to messages,
            "textContent" to textContent,
        ))

        val response = cta.generateResponseRaw(messages)

        DangerousEventBus.emit("InferenceEngine.summarize.didExecute", mapOf(
            "instruction" to instruction,
            "textContentLength" to textContent.length,
            "result" to response.content,
            "tokenUsage" to response.tokenUsage
        ))

        // TODO: count token usage

        return response.content
    }

    private fun safeJsonPreview(raw: String, limit: Int = 2000): String {
        return Strings.compactInline(raw, limit)
    }

    private fun log(prefix: String, suffix: String, payload: Any): Path {
        val path = auxLogDir.resolve(prefix).resolve(suffix)
        return auxLogger.writeTo(payload, path)
    }

    private fun logSummary(prefix: String, payload: Map<String, Any?>): Path {
        val path = auxLogDir.resolve("summary").resolve("${prefix}_summary.jsonl")
        return auxLogger.writeTo(payload, path)
    }

    // ------------------------------ Small utilities --------------------------------
    private fun log(
        dirPrefix: String, requestId: String, timestamp: String, modelCall: String,
        messages: List<Any>, enabled: Boolean = true
    ): Path? {
        if (!enabled) return null

        return log(
            prefix = dirPrefix, suffix = "$timestamp.json", payload = mapOf(
                "requestId" to requestId,
                "modelCall" to modelCall,
                "messages" to messages,
            )
        )
    }

    companion object {
        private val FILE_LOCKS: ConcurrentHashMap<String, Any> = ConcurrentHashMap()
    }
}
