package ai.platon.pulsar.rest.openapi.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Request to create a new WebDriver session.
 */
data class NewSessionRequest(
    @param:JsonProperty("capabilities") val capabilities: Map<String, Any?>? = null
)

/**
 * Response after creating a new session.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class NewSessionResponse(
    @param:JsonProperty("value") val value: SessionValue
) {
    data class SessionValue(
        @param:JsonProperty("sessionId") val sessionId: String,
        @param:JsonProperty("capabilities") val capabilities: Map<String, Any?>? = null
    )
}

/**
 * Response with session details.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class SessionDetails(
    @param:JsonProperty("value") val value: SessionDetailsValue
) {
    data class SessionDetailsValue(
        @param:JsonProperty("sessionId") val sessionId: String,
        @param:JsonProperty("url") val url: String? = null,
        @param:JsonProperty("status") val status: String = "active",
        @param:JsonProperty("capabilities") val capabilities: Map<String, Any?>? = null
    )
}

/**
 * Generic WebDriver response wrapper.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class WebDriverResponse<T>(
    @param:JsonProperty("value") val value: T? = null
)

/**
 * Error response following WebDriver spec.
 */
data class ErrorResponse(
    @param:JsonProperty("value") val value: ErrorValue
) {
    data class ErrorValue(
        @param:JsonProperty("error") val error: String,
        @param:JsonProperty("message") val message: String,
        @param:JsonProperty("stacktrace") val stacktrace: String? = null
    )
}

/**
 * Request to set/navigate to a URL.
 */
data class SetUrlRequest(
    @param:JsonProperty("url") val url: String
)

/**
 * Response containing a URL.
 */
data class UrlResponse(
    @param:JsonProperty("value") val value: String?
)

/**
 * Selector reference for selector-first operations.
 */
data class SelectorRef(
    @param:JsonProperty("selector") val selector: String,
    @param:JsonProperty("strategy") val strategy: String = "css"
)

/**
 * Request to wait for a selector.
 */
data class WaitForRequest(
    @param:JsonProperty("selector") val selector: String,
    @param:JsonProperty("strategy") val strategy: String = "css",
    @param:JsonProperty("timeout") val timeout: Int = 30000
)

/**
 * Request to find element using WebDriver strategy.
 */
data class FindElementRequest(
    @param:JsonProperty("using") val using: String,
    @param:JsonProperty("value") val value: String
)

/**
 * WebDriver element reference.
 * Uses the W3C WebDriver element identifier key.
 */
data class ElementRef(
    @param:JsonProperty("element-6066-11e4-a52e-4f735466cecf")
    val elementId: String
)

/**
 * Response containing a single element.
 */
data class ElementResponse(
    @param:JsonProperty("value") val value: ElementRef
)

/**
 * Response containing multiple elements.
 */
data class ElementsResponse(
    @param:JsonProperty("value") val value: List<ElementRef>
)

/**
 * Response for existence check.
 */
data class ExistsResponse(
    @param:JsonProperty("value") val value: ExistsValue
) {
    data class ExistsValue(
        @param:JsonProperty("exists") val exists: Boolean
    )
}

/**
 * Request to fill an input element.
 */
data class FillRequest(
    @param:JsonProperty("selector") val selector: String,
    @param:JsonProperty("strategy") val strategy: String = "css",
    @param:JsonProperty("value") val value: String
)

/**
 * Request to press a key on an element.
 */
data class PressRequest(
    @param:JsonProperty("selector") val selector: String,
    @param:JsonProperty("strategy") val strategy: String = "css",
    @param:JsonProperty("key") val key: String
)

/**
 * Response containing HTML content.
 */
data class HtmlResponse(
    @param:JsonProperty("value") val value: String?
)

/**
 * Response containing a screenshot.
 */
data class ScreenshotResponse(
    @param:JsonProperty("value") val value: String?
)

/**
 * Request to send keys to an element.
 */
data class SendKeysRequest(
    @param:JsonProperty("text") val text: String
)

/**
 * Response containing an attribute value.
 */
data class AttributeResponse(
    @param:JsonProperty("value") val value: String?
)

/**
 * Response containing text content.
 */
data class TextResponse(
    @param:JsonProperty("value") val value: String?
)

/**
 * Request to execute a script.
 */
data class ScriptRequest(
    @param:JsonProperty("script") val script: String,
    @param:JsonProperty("args") val args: List<Any?>? = null
)

/**
 * Response from script execution.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ScriptResponse(
    @param:JsonProperty("value") val value: Any? = null
)

/**
 * Request to delay execution.
 */
data class DelayRequest(
    @param:JsonProperty("ms") val ms: Int
)

/**
 * Event configuration.
 */
data class EventConfig(
    @param:JsonProperty("eventType") val eventType: String,
    @param:JsonProperty("selector") val selector: String? = null,
    @param:JsonProperty("enabled") val enabled: Boolean = true
)

/**
 * Event configuration with ID.
 */
data class EventConfigWithId(
    @param:JsonProperty("configId") val configId: String,
    @param:JsonProperty("eventType") val eventType: String,
    @param:JsonProperty("enabled") val enabled: Boolean = true
)

/**
 * Response for event config creation.
 */
data class EventConfigResponse(
    @param:JsonProperty("value") val value: EventConfigWithId
)

/**
 * Response for listing event configs.
 */
data class EventConfigsResponse(
    @param:JsonProperty("value") val value: List<EventConfigWithId>
)

/**
 * Event data.
 */
data class Event(
    @param:JsonProperty("eventId") val eventId: String,
    @param:JsonProperty("eventType") val eventType: String,
    @param:JsonProperty("timestamp") val timestamp: Long,
    @param:JsonProperty("data") val data: Map<String, Any?>? = null
)

/**
 * Response for listing events.
 */
data class EventsResponse(
    @param:JsonProperty("value") val value: List<Event>
)

/**
 * Request to subscribe to events.
 */
data class SubscribeRequest(
    @param:JsonProperty("eventTypes") val eventTypes: List<String>
)

/**
 * Subscription data.
 */
data class SubscriptionData(
    @param:JsonProperty("subscriptionId") val subscriptionId: String,
    @param:JsonProperty("eventTypes") val eventTypes: List<String>
)

/**
 * Response for subscription.
 */
data class SubscriptionResponse(
    @param:JsonProperty("value") val value: SubscriptionData
)

// ========== PerceptiveAgent DTOs ==========

/**
 * Request to run an agent task.
 */
data class AgentRunRequest(
    @param:JsonProperty("task") val task: String,
    @param:JsonProperty("multiAct") val multiAct: Boolean = false,
    @param:JsonProperty("modelName") val modelName: String? = null,
    @param:JsonProperty("variables") val variables: Map<String, String>? = null,
    @param:JsonProperty("domSettleTimeoutMs") val domSettleTimeoutMs: Long? = null,
    @param:JsonProperty("timeoutMs") val timeoutMs: Long? = null
)

/**
 * Result of an agent run operation.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class AgentRunResult(
    @param:JsonProperty("success") val success: Boolean,
    @param:JsonProperty("message") val message: String = "",
    @param:JsonProperty("historySize") val historySize: Int = 0,
    @param:JsonProperty("processTraceSize") val processTraceSize: Int = 0
)

/**
 * Response for agent run.
 */
data class AgentRunResponse(
    @param:JsonProperty("value") val value: AgentRunResult
)

/**
 * Request to observe page.
 */
data class AgentObserveRequest(
    @param:JsonProperty("instruction") val instruction: String? = null,
    @param:JsonProperty("modelName") val modelName: String? = null,
    @param:JsonProperty("domSettleTimeoutMs") val domSettleTimeoutMs: Long? = null,
    @param:JsonProperty("returnAction") val returnAction: Boolean? = null,
    @param:JsonProperty("drawOverlay") val drawOverlay: Boolean = true
)

/**
 * Result of an observation.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ObserveResultDto(
    @param:JsonProperty("locator") val locator: String? = null,
    @param:JsonProperty("domain") val domain: String? = null,
    @param:JsonProperty("method") val method: String? = null,
    @param:JsonProperty("arguments") val arguments: Map<String, Any?>? = null,
    @param:JsonProperty("description") val description: String? = null,
    @param:JsonProperty("screenshotContentSummary") val screenshotContentSummary: String? = null,
    @param:JsonProperty("currentPageContentSummary") val currentPageContentSummary: String? = null,
    @param:JsonProperty("nextGoal") val nextGoal: String? = null,
    @param:JsonProperty("thinking") val thinking: String? = null,
    @param:JsonProperty("summary") val summary: String? = null,
    @param:JsonProperty("keyFindings") val keyFindings: String? = null,
    @param:JsonProperty("nextSuggestions") val nextSuggestions: List<String>? = null
)

/**
 * Response for agent observe.
 */
data class AgentObserveResponse(
    @param:JsonProperty("value") val value: List<ObserveResultDto>
)

/**
 * Request to execute an action.
 */
data class AgentActRequest(
    @param:JsonProperty("action") val action: String,
    @param:JsonProperty("multiAct") val multiAct: Boolean = false,
    @param:JsonProperty("modelName") val modelName: String? = null,
    @param:JsonProperty("variables") val variables: Map<String, String>? = null,
    @param:JsonProperty("domSettleTimeoutMs") val domSettleTimeoutMs: Long? = null,
    @param:JsonProperty("timeoutMs") val timeoutMs: Long? = null
)

/**
 * Result of an action execution.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ActResultDto(
    @param:JsonProperty("success") val success: Boolean = false,
    @param:JsonProperty("message") val message: String = "",
    @param:JsonProperty("action") val action: String? = null,
    @param:JsonProperty("isComplete") val isComplete: Boolean = false,
    @param:JsonProperty("expression") val expression: String? = null
)

/**
 * Response for agent act.
 */
data class AgentActResponse(
    @param:JsonProperty("value") val value: ActResultDto
)

/**
 * Schema definition for extraction.
 */
data class ExtractionSchemaDto(
    @param:JsonProperty("type") val type: String = "object",
    @param:JsonProperty("properties") val properties: Map<String, Any>? = null,
    @param:JsonProperty("required") val required: List<String>? = null
)

/**
 * Request to extract data from page.
 */
data class AgentExtractRequest(
    @param:JsonProperty("instruction") val instruction: String,
    @param:JsonProperty("schema") val schema: ExtractionSchemaDto? = null,
    @param:JsonProperty("modelName") val modelName: String? = null,
    @param:JsonProperty("domSettleTimeoutMs") val domSettleTimeoutMs: Long? = null,
    @param:JsonProperty("selector") val selector: String? = null
)

/**
 * Result of an extraction.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ExtractResultDto(
    @param:JsonProperty("success") val success: Boolean,
    @param:JsonProperty("message") val message: String = "",
    @param:JsonProperty("data") val data: Any? = null
)

/**
 * Response for agent extract.
 */
data class AgentExtractResponse(
    @param:JsonProperty("value") val value: ExtractResultDto
)

/**
 * Request to summarize page content.
 */
data class AgentSummarizeRequest(
    @param:JsonProperty("instruction") val instruction: String? = null,
    @param:JsonProperty("selector") val selector: String? = null
)

/**
 * Response for agent summarize.
 */
data class AgentSummarizeResponse(
    @param:JsonProperty("value") val value: String
)

/**
 * Response for agent clear history.
 */
data class AgentClearHistoryResponse(
    @param:JsonProperty("value") val value: Boolean
)

// ========== PulsarSession DTOs ==========

/**
 * Request to normalize a URL.
 */
data class NormalizeRequest(
    @param:JsonProperty("url") val url: String,
    @param:JsonProperty("args") val args: String? = null,
    @param:JsonProperty("toItemOption") val toItemOption: Boolean = false
)

/**
 * Result of URL normalization.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class NormUrlResult(
    @param:JsonProperty("spec") val spec: String,
    @param:JsonProperty("url") val url: String,
    @param:JsonProperty("args") val args: String? = null,
    @param:JsonProperty("isNil") val isNil: Boolean = false
)

/**
 * Response for URL normalization.
 */
data class NormalizeResponse(
    @param:JsonProperty("value") val value: NormUrlResult
)

/**
 * Request to open a URL immediately.
 */
data class OpenRequest(
    @param:JsonProperty("url") val url: String
)

/**
 * Result of opening a URL.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class WebPageResult(
    @param:JsonProperty("url") val url: String,
    @param:JsonProperty("location") val location: String? = null,
    @param:JsonProperty("contentType") val contentType: String? = null,
    @param:JsonProperty("contentLength") val contentLength: Int = 0,
    @param:JsonProperty("protocolStatus") val protocolStatus: String? = null,
    @param:JsonProperty("isNil") val isNil: Boolean = false
)

/**
 * Response for opening a URL.
 */
data class OpenResponse(
    @param:JsonProperty("value") val value: WebPageResult
)

/**
 * Request to load a URL from storage or internet.
 */
data class LoadRequest(
    @param:JsonProperty("url") val url: String,
    @param:JsonProperty("args") val args: String? = null
)

/**
 * Response for loading a URL.
 */
data class LoadResponse(
    @param:JsonProperty("value") val value: WebPageResult
)

/**
 * Request to submit a URL to the crawl pool.
 */
data class SubmitRequest(
    @param:JsonProperty("url") val url: String,
    @param:JsonProperty("args") val args: String? = null
)

/**
 * Response for submitting a URL.
 */
data class SubmitResponse(
    @param:JsonProperty("value") val value: Boolean
)
