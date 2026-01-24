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
import ai.platon.pulsar.common.MessageWriter
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.serialize.json.prettyPulsarObjectMapper
import ai.platon.pulsar.external.BrowserChatModel
import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.skeleton.crawl.fetch.driver.AbstractWebDriver
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import java.nio.file.*
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
    private val session: AgenticSession,
    private val chatModel: BrowserChatModel,
) {
    // Reuse a single ObjectMapper for JSON parsing within this class
    private val mapper = ObjectMapper()

    private val cta = ContextToAction(session.sessionConfig)
    private val auxLogDir: Path get() = AppPaths.detectAuxiliaryLogDir().resolve("agent")

    val domService: DomService
        get() = (session.getOrCreateBoundDriver() as? AbstractWebDriver)?.domService
            ?: throw IllegalStateException("Bound driver is not AbstractWebDriver")

    suspend fun observe(params: ObserveParams, context: ExecutionContext): ActionDescription {
        val messages = InferenceMessageBuilder.buildObserveMessages(params)

        val startTime = Instant.now()
        val prefix = if (params.fromAct) "act" else "observe"
        val timestamp = AppPaths.fromNow()

        val callFile: Path? = info(
            dirPrefix = "${prefix}-summary",
            kind = "${prefix}Call",
            requestId = context.uuid,
            timestamp = timestamp,
            modelCall = prefix,
            messages = messages.messages
        )

        val actionDescription = cta.generate(messages, context)
        requireNotNull(context.agentState.actionDescription) {
            "Field should be set: context.agentState.actionDescription"
        }

        val modelResponse = requireNotNull(actionDescription.modelResponse) {
            "Field should be set: actionDescription.modelResponse"
        }

        val tokenUsage = modelResponse.tokenUsage
        val responseContent = modelResponse.content

        val respFile = info(
            prefix = "${prefix}-summary",
            kind = "${prefix}Response",
            suffix = "$timestamp.json",
            payload = mapOf(
                "requestId" to context.uuid,
                "modelResponse" to prefix,
                "rawResponse" to safeJsonPreview(responseContent)
            )
        )

        appendSummaryToFile(
            prefix = prefix,
            entry = mapOf(
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
        val messages = InferenceMessageBuilder.buildExtractPrompt(params)

        // 1) Extraction call -----------------------------------------------------------------
        val timestamp = AppPaths.fromNow()
        val callFile: Path? = info(
            dirPrefix = "extract-summary",
            kind = "extractCall",
            timestamp = timestamp,
            requestId = params.requestId,
            modelCall = "extract",
            messages = messages.messages
        )

        val extractStartTime = Instant.now()
        val extractResponse: ModelResponse = cta.generateResponseRaw(messages)

        val extractedNode: ObjectNode = runCatching {
            mapper.readTree(extractResponse.content) as? ObjectNode ?: JsonNodeFactory.instance.objectNode()
        }.getOrElse { JsonNodeFactory.instance.objectNode() }

        val extractRespFile: Path = info(
            prefix = "extract-summary",
            kind = "extractResponse",
            suffix = "$timestamp.json",
            payload = mapOf(
                "requestId" to params.requestId,
                "modelCall" to "extract",
                "rawResponse" to safeJsonPreview(extractResponse.content),
            )
        )

        appendSummaryToFile(
            prefix = "extract",
            entry = mapOf(
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

        val metadataCallFile: Path? = info(
            dirPrefix = "extract-summary",
            kind = "metadataCall",
            timestamp = timestamp,
            requestId = params.requestId,
            modelCall = "metadata",
            messages = metadataMessages.messages
        )

        val metadataStartTime = Instant.now()
        val metadataResponse = cta.generateResponseRaw(metadataMessages)

        val metaNode: ObjectNode = runCatching {
            mapper.readTree(metadataResponse.content) as? ObjectNode ?: JsonNodeFactory.instance.objectNode()
        }.getOrElse { JsonNodeFactory.instance.objectNode() }
        val progress = metaNode.path("progress").asText("")
        val completed = metaNode.path("completed").asBoolean(false)

        val metadataRespFile: Path = info(
            prefix = "extract-summary",
            kind = "metadataResponse",
            suffix = "$timestamp.json",
            payload = mapOf(
                "requestId" to params.requestId,
                "modelResponse" to "metadata",
                "completed" to completed,
                "progress" to progress,
            )
        )

        appendSummaryToFile(
            prefix = "extract",
            entry = mapOf(
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

        return result
    }

    suspend fun summary(instruction: String?, textContent: String): String {
        val messages = InferenceMessageBuilder.buildSummaryPrompt(instruction, textContent)

        val response = cta.generateResponseRaw(messages)

        // TODO: count token usage

        return response.content
    }

    private fun safeJsonPreview(raw: String, limit: Int = 2000): String {
        return Strings.compactInline(raw, limit)
    }

    private fun info(prefix: String, kind: String, suffix: String, payload: Any): Path {
        val dir = auxLogDir.resolve(prefix)
        Files.createDirectories(dir)

        val path = dir.resolve("${kind}.$suffix")
        val content: Any =
            runCatching { prettyPulsarObjectMapper().writeValueAsString(payload) }.getOrNull() ?: payload.toString()
        MessageWriter.writeOnce(path, content)
        return path
    }

    private fun appendSummaryToFile(prefix: String, entry: Map<String, Any?>) {
        val summaryDir = auxLogDir.resolve("summary")
        Files.createDirectories(summaryDir)

        val file = summaryFile(summaryDir, prefix)

        val entryNode: JsonNode = mapper.valueToTree(normalizeSummaryEntry(entry))

        val current = readSummaryArrayOrEmpty(file)
        current.add(entryNode)
        writeSummaryArrayAtomically(summaryDir, prefix, file, current)
    }

    private fun summaryFile(summaryDir: Path, prefix: String): Path = summaryDir.resolve("${prefix}_summary.json")

    /**
     * Convert values to stable JSON-friendly shapes.
     *
     * Currently, we only normalize [Path] -> String. Other types are left as-is.
     */
    private fun normalizeSummaryEntry(entry: Map<String, Any?>): Map<String, Any?> {
        return entry.mapValues { (_, v) ->
            when (v) {
                is Path -> v.toString()
                else -> v
            }
        }
    }

    /**
     * Read an existing summary file as a JSON array.
     *
     * If the file doesn't exist, is empty, isn't an array, or is invalid JSON, an empty array is returned.
     */
    private fun readSummaryArrayOrEmpty(file: Path): ArrayNode {
        if (!Files.exists(file)) {
            return JsonNodeFactory.instance.arrayNode()
        }

        val bytes = runCatching { Files.readAllBytes(file) }.getOrNull() ?: return JsonNodeFactory.instance.arrayNode()
        if (bytes.isEmpty()) {
            return JsonNodeFactory.instance.arrayNode()
        }

        return runCatching { mapper.readTree(bytes) as? ArrayNode }
            .getOrNull()
            ?: JsonNodeFactory.instance.arrayNode()
    }

    private fun writeSummaryArrayAtomically(summaryDir: Path, prefix: String, file: Path, array: ArrayNode) {
        val tempFile = Files.createTempFile(summaryDir, "${prefix}_summary_", ".json")
        try {
            Files.write(
                tempFile,
                prettyPulsarObjectMapper().writeValueAsBytes(array),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            )

            try {
                Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            runCatching { Files.deleteIfExists(tempFile) }
        }
    }

    // ------------------------------ Small utilities --------------------------------
    private fun info(
        dirPrefix: String, kind: String, requestId: String, timestamp: String, modelCall: String,
        messages: List<Any>, enabled: Boolean = true
    ): Path? {
        if (!enabled) return null

        return info(
            prefix = dirPrefix, kind = kind, suffix = "$timestamp.json", payload = mapOf(
                "requestId" to requestId,
                "modelCall" to modelCall,
                "messages" to messages,
            )
        )
    }
}
