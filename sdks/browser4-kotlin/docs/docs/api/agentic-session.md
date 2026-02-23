# AgenticSession API

AI-powered browser automation extending PulsarSession.

## Inheritance

```kotlin
class AgenticSession(client: PulsarClient) : PulsarSession(client), PerceptiveAgent
```

Inherits all [PulsarSession](pulsar-session.md) methods plus AI capabilities.

## Static Methods

### getOrCreate()

```kotlin
fun getOrCreate(
    baseUrl: String? = null,
    useLocalDriver: Boolean = baseUrl == null
): AgenticSession
```

Get or create default session with local driver.

**Example:**
```kotlin
val session = AgenticSession.getOrCreate()
// Use session
session.close()
```

### create()

```kotlin
suspend fun create(
    baseUrl: String? = null,
    useLocalDriver: Boolean = baseUrl == null
): AgenticSession
```

Create new session.

### resetDefault()

```kotlin
fun resetDefault()
```

Close and reset default session.

## Properties

### companionAgent

```kotlin
val companionAgent: PerceptiveAgent
```

Access to agent capabilities (self reference).

### stateHistory

```kotlin
val stateHistory: AgentHistory
```

Agent execution history.

### agentEventHandlers

```kotlin
val agentEventHandlers: AgentEventHandlers
```

Event handlers for agent lifecycle events.

## AI Operations

### act()

```kotlin
suspend fun act(
    action: String,
    multiAct: Boolean = false,
    modelName: String? = null,
    variables: Map<String, String>? = null,
    domSettleTimeoutMs: Long? = null,
    timeoutMs: Long? = null
): AgentActResult
```

Execute single action in natural language.

**Example:**
```kotlin
val result = agent.act("click the search button")
if (result.success) {
    println(result.message)
}
```

### run()

```kotlin
suspend fun run(
    task: String,
    multiAct: Boolean = false,
    modelName: String? = null,
    variables: Map<String, String>? = null,
    domSettleTimeoutMs: Long? = null,
    timeoutMs: Long? = null
): AgentRunResult
```

Run autonomous multi-step task.

**Example:**
```kotlin
val result = agent.run(
    "search for kotlin and extract top results"
)
```

### observe()

```kotlin
suspend fun observe(
    instruction: String? = null,
    modelName: String? = null,
    domSettleTimeoutMs: Long? = null,
    returnAction: Boolean? = null,
    drawOverlay: Boolean = true
): AgentObservation
```

Observe page and suggest actions.

**Example:**
```kotlin
val obs = agent.observe("find interactive elements")
obs.observations.forEach {
    println("Element: ${it.locator}")
    println("Description: ${it.description}")
}
```

### extract()

```kotlin
suspend fun extract(
    instruction: String,
    schema: Map<String, Any?>? = null,
    selector: String? = null,
    modelName: String? = null,
    domSettleTimeoutMs: Long? = null
): ExtractionResult
```

AI-powered data extraction.

**Example:**
```kotlin
val result = agent.extract(
    "extract product name and price"
)
println("Data: ${result.data}")
```

### summarize()

```kotlin
suspend fun summarize(
    instruction: String? = null,
    selector: String? = null
): String
```

Generate page summary.

### clearHistory()

```kotlin
suspend fun clearHistory(): Boolean
```

Clear agent history.

## Event Streaming

### runWithEvents()

```kotlin
suspend fun runWithEvents(
    task: String,
    multiAct: Boolean = false,
    modelName: String? = null,
    variables: Map<String, String>? = null,
    domSettleTimeoutMs: Long? = null,
    timeoutMs: Long? = null
): AgentRunResult
```

Run task with event streaming.

### actWithEvents(), observeWithEvents()

Similar methods for act and observe with event streaming.

## See Also

- [PulsarSession API](pulsar-session.md) - Base session methods
- [AI Automation Guide](../guide/ai-automation.md) - Usage guide
