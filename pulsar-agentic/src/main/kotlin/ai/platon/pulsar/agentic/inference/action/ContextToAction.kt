package ai.platon.pulsar.agentic.inference.action

import ai.platon.pulsar.agentic.inference.AgentMessageList
import ai.platon.pulsar.agentic.model.ExecutionContext
import ai.platon.pulsar.agentic.model.ActionDescription
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.ExperimentalApi
import ai.platon.pulsar.common.brief
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.external.BrowserChatModel
import ai.platon.pulsar.external.ChatModelFactory
import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.external.ResponseState
import java.nio.file.Files

open class ContextToAction(
    val conf: ImmutableConfig
) {
    private val logger = getLogger(this)

    val baseDir = AppPaths.get("tta")

    val chatModel: BrowserChatModel get() = ChatModelFactory.getOrCreate(conf)

    val tta = TextToAction(conf)

    init {
        Files.createDirectories(baseDir)
    }

    @ExperimentalApi
    open suspend fun generate(messages: AgentMessageList, context: ExecutionContext): ActionDescription {
        try {
            val instruction = context.instruction

            val response = generateResponseRaw(messages, context.screenshotB64)

            val actionDescription = tta.modelResponseToActionDescription(instruction, context.agentState, response)

            require(context.agentState.actionDescription == actionDescription) {
                "Required: context.agentState.actionDescription == actionDescription"
            }

            return actionDescription
        } catch (e: Exception) {
            val errorResponse = ModelResponse("Unknown exception" + e.brief(), ResponseState.OTHER)
            val actionDescription = ActionDescription(
                context.instruction,
                exception = e,
                modelResponse = errorResponse,
                context = context
            )
            context.agentState.actionDescription = actionDescription

            return actionDescription
        }
    }

    @ExperimentalApi
    open suspend fun generateResponseRaw(messages: AgentMessageList, screenshotB64: String? = null): ModelResponse {
        val systemMessage = messages.systemMessages().joinToString("\n")
        val userMessage = messages.userMessages().joinToString("\n")

        val category = "cta"
        val response = if (screenshotB64 != null) {
            chatModel.call(systemMessage,
                userMessage,
                imageUrl = null,
                b64Image = screenshotB64,
                mediaType = "image/jpeg", category = category)
        } else {
            chatModel.call(systemMessage, userMessage, category = category)
        }

        return response
    }
}
