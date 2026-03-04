package ai.platon.pulsar.agentic.model

import ai.platon.browser4.driver.chrome.dom.model.DOMTreeNodeEx
import ai.platon.pulsar.agentic.ActResult
import ai.platon.pulsar.agentic.ObserveResult
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.compactedBrief
import ai.platon.pulsar.common.serialize.json.Pson
import ai.platon.pulsar.external.ModelResponse
import com.fasterxml.jackson.annotation.JsonIgnore
import org.apache.commons.lang3.StringUtils
import java.time.Instant

data class DetailedActResult(
    val actionDescription: ActionDescription,
    val toolCallResult: ToolCallResult? = null,
    val success: Boolean = false,
    /**
     * A short description about the current state
     * */
    val description: String? = null,
    /**
     * The exception that not from tool call execution which is already in ToolCallResult
     * */
    val exception: Exception? = null,
) {
    fun toActResult(): ActResult {
        return ActResult(
            action = actionDescription.instruction,
            success = success,
            message = description ?: "",
            result = toolCallResult,
            detail = this
        )
    }

    fun toJson(): String {
        return Pson.toJson(this)
    }

    override fun toString(): String {
        return "DetailedActResult(success=$success, description=$description, exception=${exception?.compactedBrief()}, actionDescription=${actionDescription}, toolCallResult=${toolCallResult})"
    }

    companion object {
        fun failed(actionDescription: ActionDescription, exception: Exception? = null): DetailedActResult {
            return DetailedActResult(actionDescription, exception = exception)
        }
    }
}

data class ToolSpec constructor(
    val domain: String,
    val method: String,
    val arguments: List<Arg> = listOf(),
    val returnType: String = "Unit",
    val description: String? = null,
    val help: String? = null,
) {
    data class Arg(
        val name: String,
        val type: String,
        val defaultValue: String? = null,
    ) {
        @get:JsonIgnore
        val expression: String
            get() {
                return if (defaultValue != null) "$name: $type = $defaultValue" else "$name: $type"
            }
    }

    @get:JsonIgnore
    val expression: String
        get() {
            val args = arguments.joinToString(prefix = "(", postfix = ")")
            return "$domain.$method$args"
        }
}

data class ToolCall constructor(
    /**
     * Tool domain (aka namespace), e.g. "driver", "fs", "mcp.weather", "skill.blog", or a custom domain.
     */
    val domain: String,
    val method: String,
    val arguments: MutableMap<String, Any?> = mutableMapOf(),
    val description: String? = null,
) {
    @get:JsonIgnore
    val weakTypeNamedArguments
        get() = arguments.entries.joinToString { (k, v) -> "$k=" + Strings.doubleQuote(v?.toString()) }

    @get:JsonIgnore
    val pseudoNamedArguments
        get() = arguments.entries
            .joinToString { (k, v) -> "$k=" + Strings.doubleQuote(Strings.compactInline(v?.toString(), 20)) }

    @get:JsonIgnore
    val weakTypeExpression: String get() = "$domain.${method}($weakTypeNamedArguments)"

    @get:JsonIgnore
    val pseudoExpression: String get() = "$domain.${method}($pseudoNamedArguments)"

    override fun toString() = pseudoExpression
}

data class TcException(
    val expression: String = "",
    /**
     * Ignore the cause in preview, which may cause recursion when the cause contains a lot of data.
     * */
    @JsonIgnore
    val cause: Exception? = null,
    var help: String? = null,
) {
    val domain: String get() = expression.substringBefore('.')
    val method: String get() = StringUtils.substringBetween(expression, ".", ")")
    val message: String? get() = cause?.message
}

data class TcEvaluate constructor(
    /**
     * Ignore the value in preview, which may cause recursion when the value is complex and contains a lot of data.
     * The preview should be generated from the expression and description, which is more concise and informative.
     * */
    @JsonIgnore
    val value: Any? = null,
    val description: String? = null,
    val className: String? = null,
    val expression: String? = null,
    var exception: TcException? = null
) {
    val preview get() = doGetPreview()

    constructor(expression: String, cause: Exception, help: String? = null) :
            this(expression = expression, exception = TcException(expression, cause, help))

    private fun doGetPreview(): String {
        return when (value) {
            is Number -> "$value"
            is Boolean -> "$value"
            else -> Strings.compactInline("$value", 50)
        }
    }
}

data class ToolCallResult constructor(
    val success: Boolean = false,
    val evaluate: TcEvaluate? = null,
    val message: String? = null,
    @JsonIgnore
    val actionDescription: ActionDescription? = null
) {
    val expression: String get() = actionDescription?.expression ?: ""
    val modelResponse: String? get() = actionDescription?.modelResponse?.content
}

data class ObserveElement constructor(
    val locator: String? = null,

    val screenshotContentSummary: String? = null,
    val currentPageContentSummary: String? = null,
    val evaluationPreviousGoal: String? = null,
    val nextGoal: String? = null,
    val thinking: String? = null,

    val modelResponse: String? = null,

    // Revised fields
    val toolCall: ToolCall? = null,
    @JsonIgnore
    val node: DOMTreeNodeEx? = null,
    val backendNodeId: Int? = null,
    val xpath: String? = null,
    val cssSelector: String? = null,
    val cssFriendlyExpression: String? = null,
) {
    @get:JsonIgnore
    val description: String? get() = toolCall?.description

    @get:JsonIgnore
    val domain: String? get() = toolCall?.domain

    @get:JsonIgnore
    val method: String? get() = toolCall?.method

    @get:JsonIgnore
    val arguments: Map<String, Any?>? get() = toolCall?.arguments

    /**
     * Expression with weak parameter types
     * */
    @get:JsonIgnore
    val expression: String? get() = toolCall?.weakTypeExpression

    @get:JsonIgnore
    val pseudoExpression get() = toolCall?.pseudoExpression
}

data class AgentHistory(
    val states: MutableList<AgentState> = mutableListOf(),
) {
    val size get() = states.size

    val finalResult get() = states.lastOrNull()
    val isDone get() = finalResult?.isComplete == true
    val isSuccess get() = finalResult?.isSuccess == true
    val totalSteps get() = states.size
    val hasErrors get() = states.any { it.hasErrors }
    val actionHistory get() = states.map { it.actionDescription }
    val actionResults get() = states.map { it.toolCallResult }

    val urls get() = states.map { it.url }
    val modelOutputs get() = states.map { it.actionDescription?.modelResponse?.content }
    val modelThoughts get() = states.map { it.actionDescription?.observeElement?.thinking }

    fun isEmpty() = states.isEmpty()
    fun isNotEmpty() = states.isNotEmpty()
    fun first() = states.first()
    fun last() = states.last()
    fun firstOrNull() = states.firstOrNull()
    fun lastOrNull() = states.lastOrNull()

    fun toJson(): String {
        return Pson.toJson(mapOf(
            "size" to size,
            "isDone" to isDone,
            "isSuccess" to isSuccess,
            "totalSteps" to totalSteps,
            "hasErrors" to hasErrors,
            "urls" to urls,
            "modelOutputs" to modelOutputs,
            "modelThoughts" to modelThoughts,
        ))
    }

    override fun toString(): String {
        return states.joinToString("\n") { it.toString() }
    }
}

/**
 * The action description for the agent to go forward in the next step.
 * */
data class ActionDescription constructor(
    /**
     * The user's original instruction, should be only one instruction for the whole session, and should not be
     * changed in different steps. The reason is that we want to keep the original instruction as the main goal
     * of the session, and use the step and event to track the progress.
     * If we change the instruction in different steps, it may cause confusion and make it harder to track the progress.
     * */
    val instruction: String,

    /**
     * AI: observe elements
     * */
    val observeElements: List<ObserveElement>? = null,

    /**
     * AI: whether the task is complete
     * */
    var isComplete: Boolean = false,
    /**
     * AI: the error cause for the task
     * */
    var errorCause: String? = null,
    /**
     * AI: a summary about this task
     * */
    var summary: String? = null,
    /**
     * AI: a summary about this task
     * */
    val keyFindings: List<String>? = null,
    /**
     * AI: next suggestions
     * */
    val nextSuggestions: List<String>? = null,
    /**
     * AI: model response
     * */
    val modelResponse: ModelResponse? = null,
    /**
     * The exception if any
     * */
    val exception: Exception? = null,
    /**
     * The execution context
     * */
    val context: Any? = null,
    /**
     * The agent state
     * */
    val agentState: AgentState? = null,
) {
    val observeElement: ObserveElement? get() = observeElements?.firstOrNull()
    val toolCall: ToolCall? get() = observeElement?.toolCall
    val locator: String? get() = observeElement?.locator
    val xpath: String? get() = observeElement?.xpath

    /**
     * Expression with weak parameter types
     * */
    val expression: String? get() = observeElement?.expression
    val cssFriendlyExpression: String? get() = observeElement?.cssFriendlyExpression
    val pseudoExpression: String? get() = observeElement?.pseudoExpression

    val isReallyComplete get() = isComplete || expression?.contains("agent.done") == true


    fun complete(summary: String? = null) {
        isComplete = true
        if (summary != null) {
            this.summary = summary
        }
    }

    fun toActionDescriptions(): List<ActionDescription> {
        val elements = observeElements ?: return emptyList()
        return elements.map { this.copy(observeElements = listOf(it)) }
    }

    fun toObserveResults(agentState: AgentState): List<ObserveResult> {
        val results = observeElements?.map { ele ->
            ObserveResult(
                locator = ele.locator,
                domain = ele.domain?.ifBlank { null },
                method = ele.method?.ifBlank { null },
                arguments = ele.arguments?.takeIf { it.isNotEmpty() },
                description = ele.description ?: "(No comment ...)",

                screenshotContentSummary = ele.screenshotContentSummary,
                currentPageContentSummary = ele.currentPageContentSummary,
                evaluationPreviousGoal = ele.evaluationPreviousGoal,
                nextGoal = ele.nextGoal,
                thinking = ele.thinking,

                backendNodeId = ele.backendNodeId,
                observeElement = ele,
                agentState = agentState,
                actionDescription = this,
            )
        }

        return results ?: emptyList()
    }

    fun toJson(): String {
        return Pson.toJson(mapOf(
            "instruction" to instruction,
            "isComplete" to isComplete,
            "errorCause" to errorCause,
            "summary" to summary,
            "keyFindings" to keyFindings,
            "nextSuggestions" to nextSuggestions,
            "modelResponse" to modelResponse?.content,
            "exception" to exception?.compactedBrief(),
        ))
    }

    override fun toString(): String {
        return if (isComplete) "Completed. Summary: $summary"
        else (cssFriendlyExpression ?: modelResponse?.toString() ?: "")
    }
}

data class ProcessTrace constructor(
    val step: Int,
    val event: String? = null,
    val method: String? = null,
    val agentState: String? = null,
    val expression: String? = null,
    val isComplete: Boolean = false,
    val tcEvalResult: Any? = null,
    val message: String? = null,
    val items: Map<String, Any?> = emptyMap(),
    val timestamp: Instant = Instant.now(),
) {
    fun toJson(): String {
        return Pson.toJson(this)
    }

    override fun toString(): String {
        fun format(v: Any?): String? {
            return when (v) {
                null -> null
                is Throwable -> v.compactedBrief()
                else -> Strings.compactInline(v.toString(), 50)
            }
        }

        val itemStr = items.entries.joinToString { (k, v) -> "$k=" + format(v) }.takeIf { it.isNotBlank() }

        val str = buildString {
            append(timestamp)

            append(" | step=$step")
            event?.let { append(", event=$event") }
            method?.let { append(", method=$method") }

            if (!itemStr.isNullOrBlank()) {
                append("\n    $itemStr")
            }
            if (!message.isNullOrBlank()) {
                append("\n    $message")
            }
            if (!agentState.isNullOrBlank()) {
                append("\n    $agentState")
            }
        }

        return str
    }
}
