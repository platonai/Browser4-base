package ai.platon.pulsar.agentic.inference.action

import ai.platon.browser4.driver.chrome.dom.model.BrowserUseState
import ai.platon.pulsar.agentic.inference.AgentMessageList
import ai.platon.pulsar.agentic.model.ActionDescription
import ai.platon.pulsar.agentic.model.AgentState
import ai.platon.pulsar.agentic.model.DetailedActResult

/**
 * A single tool/action request produced by an LLM.
 *
 * This is a lightweight, JSON-friendly representation that is typically returned by the model and then
 * converted into internal types (e.g. [ai.platon.pulsar.agentic.model.ToolCall]).
 *
 * Notes on compatibility:
 * - All properties are nullable and have defaults so that missing fields in model output can be tolerated.
 * - The exact semantics of [domain]/[method] are defined by the tool-call specification rendered in prompts.
 */
data class ToolCallElement(
    /**
     * Tool domain (aka namespace), e.g. "fs", "mcp.weather", "skill.blog", or a custom domain.
     *
     * When absent, the caller usually treats this action as invalid or falls back to a default domain.
     */
    val domain: String? = null,

    /**
     * Tool method (aka function name) inside [domain], e.g. "readString", "getWeather", "writeBlog".
     */
    val method: String? = null,

    /**
     * Natural language description for explainability/debugging.
     */
    val description: String? = null,

    /**
     * Arguments passed to the tool method.
     *
     * Historical note: this model uses `Map<String, String>` which implies values are strings.
     * Newer observe schemas use `Any` for values (see [ObserveResponseElement.arguments]).
     *
     * The list form is used because some prompts return arguments as an array of `{name,value}` objects.
     */
    val arguments: List<Map<String, Any>?>? = null,

    /**
     * Locator string for the target element, if applicable.
     * */
    val locator: String? = null,

    /**
     * Screenshot summary (if an image was provided).
     */
    val screenshotContentSummary: String? = null,

    /**
     * Current page summary as perceived by the model, if applicable.
     */
    val currentPageContentSummary: String? = null,



    /**
     * Long-term memory that the agent wants to persist.
     */
    val memory: String? = null,

    /**
     * Free-form chain-of-thought style reasoning.
     *
     * Consumers should treat this as best-effort debug information and must not rely on it for correctness.
     */
    val thinking: String? = null,

    /**
     * Model's evaluation of whether the previous goal was achieved.
     */
    val evaluationPreviousGoal: String? = null,

    /**
     * Model's suggested next goal.
     */
    val nextGoal: String? = null,
)

/**
 * A model response that contains one or more [ToolCallElement] plus optional reasoning/memory.
 *
 * This type is intended to be deserialized from LLM output. Keep it permissive: nullable fields and defaults
 * help tolerate partially malformed responses.
 */
data class ToolCallElements(
    /**
     * Actions suggested by the model.
     */
    val toolCalls: List<ToolCallElement>,
)

const val GENERAL_TOOL_CALL_RESULT_PROMPT = """
{
  "toolCalls": [
     {
      "domain": "Tool domain, such as `fs`, `mcp.weather`, `skill.blog`",
      "method": "Method name, such as `readString`",
      "description": "Description of the current tool selection",
      "arguments": [
        {
          "name": "Parameter name, such as `filename`",
          "value": "Parameter value, such as `test.txt`"
        }
      ],
      "locator": "Web page node locator, composed of two numbers, such as `0,4`, only if applicable",
      "memory": "1–3 specific sentences describing this step and the overall progress. This should include information helpful for future progress tracking, such as the number of pages visited or items found.",
      "thinking": "A structured <think>-style reasoning block that applies the `## 推理规则`.",
      "evaluationPreviousGoal": "A concise one-sentence analysis of the previous action, clearly stating success, failure, or uncertainty.",
      "nextGoal": "A clear one-sentence statement of the next direct goal and action to take."
    }
  ]
}
"""

/**
 * A terminal response indicating whether the overall task is complete.
 *
 * This structure is parsed in [TextToAction.modelResponseToActionDescription] when the model output contains
 * a `taskComplete` field.
 */
data class ObserveResponseComplete(
    /**
     * Whether the agent believes the whole task is complete.
     */
    val taskComplete: Boolean = false,

    /**
     * Whether the execution is considered successful when [taskComplete] is true.
     */
    val success: Boolean = false,

    /**
     * A short error cause when [success] is false.
     */
    val errorCause: String? = null,

    /**
     * High-level summary for the user.
     */
    val summary: String? = null,

    /**
     * Bullet-point findings extracted from the page/process.
     */
    val keyFindings: List<String>? = null,

    /**
     * Suggested next steps (follow-up queries, alternate actions, etc.).
     */
    val nextSuggestions: List<String>? = null,
)

const val TASK_COMPLETE_SCHEMA_PROMPT = """
{"taskComplete":bool,"success":bool,"errorCause":string?,"summary":string,"keyFindings":[string],"nextSuggestions":[string]}
"""

/**
 * A non-terminal observe response that lists interactive/target elements.
 */
data class ObserveResponseElements(
    /**
     * Elements returned by the model.
     *
     * Null means "the model didn't return this field"; empty list means "returned but no elements".
     */
    val elements: List<ObserveResponseElement>? = null
)

/**
 * An element + tool-call suggestion inferred from the current page.
 *
 * This is the primary JSON schema produced by the observe/action-generation prompts.
 *
 * Conversion rules (see [TextToAction.toObserveElement]):
 * - [locator] may be wrapped with brackets, and will be normalized using `removeSurrounding("[", "]")`.
 * - [arguments] is expected to be a list of `{name: ..., value: ...}` maps; names become argument keys.
 *
 * Serialization notes:
 * - [arguments] uses `Any` to allow booleans/numbers/strings/objects. Keep values JSON-serializable.
 *
 * See also: [OBSERVE_RESPONSE_ELEMENT_SCHEMA_PROMPT]
 */
data class ObserveResponseElement(
    /**
     * Locator string for the target element.
     */
    val locator: String? = null,

    /**
     * Human-friendly description of the element and/or the intended action.
     */
    val description: String? = null,

    /**
     * Tool domain (namespace) for the suggested action.
     */
    val domain: String? = null,

    /**
     * Tool method (function name) for the suggested action.
     */
    val method: String? = null,

    /**
     * Tool arguments represented as a list of maps.
     *
     * Typical shape:
     * `[{"name": "selector", "value": "..."}, {"name": "text", "value": "..."}]`
     */
    val arguments: List<Map<String, Any>?>? = null,

    /**
     * Long-term memory to persist.
     */
    val memory: String? = null,

    /**
     * Model reasoning; optional debug information.
     */
    val thinking: String? = null,

    /**
     * Screenshot summary (if an image was provided).
     */
    val screenshotContentSummary: String? = null,

    /**
     * Current page summary as perceived by the model.
     */
    val currentPageContentSummary: String? = null,

    /**
     * Evaluation of the previous goal.
     */
    val evaluationPreviousGoal: String? = null,

    /**
     * Suggested next goal.
     */
    val nextGoal: String? = null,
)

const val OBSERVE_RESPONSE_ELEMENT_SCHEMA_PROMPT = """
{
  "elements": [
    {
      "locator": "Web page node locator, composed of two numbers, such as `0,4`",
      "domain": "Tool domain, such as `driver`",
      "method": "Method name, such as `click`",
      "description": "Description of the current locator and tool selection",
      "arguments": [
        {
          "name": "Parameter name, such as `selector`",
          "value": "Parameter value, such as `0,4`"
        }
      ],
      "screenshotContentSummary": "Summary of the current screenshot content",
      "currentPageContentSummary": "Summary of the current web page text content, based on the accessibility tree or web content extraction results",
      "memory": "1–3 specific sentences describing this step and the overall progress. This should include information helpful for future progress tracking, such as the number of pages visited or items found.",
      "thinking": "A structured <think>-style reasoning block that applies the `## 推理规则`.",
      "evaluationPreviousGoal": "A concise one-sentence analysis of the previous action, clearly stating success, failure, or uncertainty.",
      "nextGoal": "A clear one-sentence statement of the next direct goal and action to take."
    }
  ]
}
"""

/**
 * A runtime action node in the agent's internal execution graph.
 *
 * Think of this as "the full context for one step": user instruction + prompt/messages + agent/browser state
 * + parsed action + execution result.
 *
 * Graph fields:
 * - [prevAction]/[nextAction]/[parent]/[children] link nodes together for tracing.
 * - Be careful when serializing this type: it can contain cycles and very large object graphs.
 */
data class AgentAction(
    /**
     * Monotonic step number in the execution sequence.
     */
    val step: Int,

    /**
     * Original instruction or sub-goal for this step.
     */
    val userInstruction: String,

    /**
     * Prompt/response messages exchanged with the model.
     */
    val messages: AgentMessageList,

    /**
     * Mutable agent state at the time of this step.
     */
    val agentState: AgentState,

    /**
     * Browser-use state snapshot (DOM, viewport, etc.) captured for the step.
     */
    val browserUseState: BrowserUseState? = null,

    /**
     * Parsed action description computed from model output.
     */
    val actionDescription: ActionDescription? = null,

    /**
     * Detailed execution result after acting (timings, errors, side effects, etc.).
     */
    val detailedActResult: DetailedActResult? = null,

    /**
     * Previous action in the linear sequence, if any.
     */
    val prevAction: AgentAction? = null,

    /**
     * Next action in the linear sequence, if any.
     */
    val nextAction: AgentAction? = null,

    /**
     * Parent action in a hierarchical plan/tree.
     */
    val parent: AgentAction? = null,

    /**
     * Child actions created from this action (e.g. decomposition).
     */
    val children: List<AgentAction> = emptyList(),
)
