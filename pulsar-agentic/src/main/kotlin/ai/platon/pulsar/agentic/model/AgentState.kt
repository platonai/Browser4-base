package ai.platon.pulsar.agentic.model

import ai.platon.browser4.driver.chrome.dom.model.BrowserUseState
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.compactedBrief
import ai.platon.pulsar.common.serialize.json.Pson
import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.Instant

data class AgentState constructor(
    var step: Int,
    // The user instruction
    var instruction: String,
    // The current browser use state
    @JsonIgnore
    var browserUseState: BrowserUseState,
    // A simple and descriptive description
    var description: String? = null,
    // The last event
    var event: String? = null,
    // AI:
    var domain: String? = null,
    // AI:
    var method: String? = null,
    // AI: the summary of the screenshot provided in this step
    var screenshotContentSummary: String? = null,
    // AI: the summary of the page content provided in this step
    var currentPageContentSummary: String? = null,
    // AI: an evaluation for the previous goal: evaluation and state: [success, failed, partial success]
    var evaluationPreviousGoal: String? = null,
    // AI: the next goal to archive
    var nextGoal: String? = null,
    // AI: thinking
    var thinking: String? = null,
    // if is complete method
    var isComplete: Boolean? = null,
    // timestamp
    var timestamp: Instant = Instant.now(),
    // The last exception
    var exception: Exception? = null,

    // AI: completion summary
    var summary: String? = null,
    // AI: completion key findings
    var keyFindings: List<String>? = null,
    // AI: completion next suggestions
    var nextSuggestions: List<String>? = null,

    @JsonIgnore
    var actionDescription: ActionDescription? = null,
    @JsonIgnore
    var toolCallResult: ToolCallResult? = null,
    @JsonIgnore
    var prevState: AgentState? = null
) {
    // the url to handle in this step
    val url: String get() = browserUseState.browserState.url
    val isSuccess: Boolean get() = exception == null
    val isDone: Boolean get() = isComplete == true
    val hasErrors: Boolean get() = exception != null

    fun toJson(): String {
        return Pson.toJson(mapOf(
            "step" to step,
            "url" to url,
            "description" to description,
            "event" to event,
            "domain" to domain,
            "method" to method,
            "isComplete" to isComplete,
            "timestamp" to timestamp.toString(),
            "screenshotContentSummary" to screenshotContentSummary,
            "currentPageContentSummary" to currentPageContentSummary,
            "evaluationPreviousGoal" to evaluationPreviousGoal,
            "nextGoal" to nextGoal,
            "thinking" to thinking,
            "instruction" to instruction,
            "exception" to exception?.compactedBrief(),
        ))
    }

    override fun toString(): String {
        if (step == 0) {
            return "step=0, N/A"
        }

        val state = if (isSuccess) """✨OK""" else "💔FAIL"
        val event0 = event ?: method ?: ""

        if (isComplete == true) {
            val ident = "    - "
            return buildString {
                appendLine("""$state, isComplete=true 🎉""")
                appendLine("summary: \n$summary")
                if (keyFindings != null) {
                    appendLine("keyFindings: \n" + keyFindings?.joinToString("\n$ident", ident))
                }
                if (nextGoal != null) {
                    appendLine("nextSuggestions: \n" + nextSuggestions?.joinToString("\n$ident", ident))
                }
            }
        } else {
            val finalSummary = listOf(
                "description" to description,
                "pageContentSummary" to currentPageContentSummary,
                "screenshotContentSummary" to screenshotContentSummary,
                "evaluationPreviousGoal" to evaluationPreviousGoal,
                "nextGoal" to nextGoal,
                "exception" to exception?.compactedBrief(),
            )
                .filter { it.second != null }
                .joinToString("\n") { (k, s) -> "\t- $k: ${Strings.compactInline(s)}" }

            val pseudoExpression = actionDescription?.pseudoExpression
            val resultPreview = toolCallResult?.evaluate?.preview ?: "(absent)"
            val toolCallState = if (toolCallResult?.success == true) "✅OK" else "❌FAIL"
            return "$state, event=$event0, tool=`$pseudoExpression`, resultPreview=`$resultPreview`, $toolCallState\n$finalSummary"
        }
    }
}
