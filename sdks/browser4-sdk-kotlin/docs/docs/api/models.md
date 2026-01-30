# Data Models Reference

Data structures for API requests and responses.

## WebPage

Represents a loaded web page.

```kotlin
data class WebPage(
    val url: String,
    val location: String? = null,
    val contentType: String? = null,
    val contentLength: Int = 0,
    val protocolStatus: String? = null,
    val html: String? = null
)
```

**Properties:**
- `url`: Page URL
- `location`: Redirect location if any
- `contentType`: MIME type
- `contentLength`: Content size in bytes
- `protocolStatus`: HTTP status
- `html`: Page HTML content
- `isNil`: True if page is nil/invalid

## NormURL

Normalized URL with arguments.

```kotlin
data class NormURL(
    val url: String,
    val args: String? = null
)
```

**Properties:**
- `url`: Normalized URL
- `args`: Load arguments
- `isNil`: True if URL is invalid
- `urlSpec`: Full URL specification (url + args)

## AgentActResult

Result from agent act operation.

```kotlin
data class AgentActResult(
    val success: Boolean = false,
    val message: String = "",
    val action: String? = null,
    val isComplete: Boolean = false,
    val expression: String? = null,
    val result: Any? = null,
    val trace: List<String>? = null
)
```

## AgentRunResult

Result from agent run operation.

```kotlin
data class AgentRunResult(
    val success: Boolean = false,
    val message: String = "",
    val historySize: Int = 0,
    val processTraceSize: Int = 0,
    val finalResult: Any? = null,
    val trace: List<String>? = null
)
```

## ObserveResult

Single observation from agent.

```kotlin
data class ObserveResult(
    val locator: String? = null,
    val domain: String? = null,
    val method: String? = null,
    val arguments: Map<String, Any?>? = null,
    val description: String? = null,
    val screenshotContentSummary: String? = null,
    val currentPageContentSummary: String? = null,
    val nextGoal: String? = null,
    val thinking: String? = null,
    val summary: String? = null,
    val keyFindings: String? = null,
    val nextSuggestions: List<String>? = null
)
```

## AgentObservation

Collection of observations.

```kotlin
data class AgentObservation(
    val observations: List<ObserveResult> = emptyList()
)
```

## ExtractionResult

Result from AI extraction.

```kotlin
data class ExtractionResult(
    val success: Boolean = false,
    val message: String = "",
    val data: Any? = null
)
```

## PageSnapshot

Captured page state.

```kotlin
data class PageSnapshot(
    val url: String,
    val html: String? = null
)
```

## FieldsExtraction

CSS selector extraction result.

```kotlin
data class FieldsExtraction(
    val fields: Map<String, Any?> = emptyMap()
)
```

## AgentHistory

Agent execution history.

```kotlin
data class AgentHistory(
    val states: MutableList<AgentState> = mutableListOf(),
    val hasErrors: Boolean = false,
    val finalResult: Any? = null
)
```

## AgentState

Single state in agent history.

```kotlin
data class AgentState(
    val step: Int = 0,
    val action: String? = null,
    val result: Any? = null,
    val success: Boolean = false,
    val message: String = ""
)
```

## Event Models

### PageEventHandlers

Handlers for page lifecycle events.

```kotlin
class PageEventHandlers {
    val load: Group
    val browse: Group
    val crawl: Group
    
    fun onAny(eventType: String, handler: (OpenApiEvent) -> Unit)
}
```

### AgentEventHandlers

Handlers for agent events.

```kotlin
class AgentEventHandlers {
    val agent: Group
    val inference: Group
    val tool: Group
    val mcp: Group
    val skill: Group
    
    fun onAny(eventType: String, handler: (AgentEvent) -> Unit)
}
```

## See Also

- [API Overview](overview.md)
- [PulsarSession API](pulsar-session.md)
- [AgenticSession API](agentic-session.md)
