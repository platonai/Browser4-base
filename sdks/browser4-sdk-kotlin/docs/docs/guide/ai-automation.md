# AI Automation

Complete guide to AI-powered browser automation with Browser4 Kotlin SDK.

## Overview

Browser4's AI automation capabilities allow you to control the browser using natural language instructions, making complex workflows accessible without writing detailed code.

## Core Concepts

The `AgenticSession` provides three main AI operations:

1. **Act** - Execute single actions
2. **Run** - Execute multi-step autonomous tasks  
3. **Observe** - Analyze page and suggest actions
4. **Extract** - AI-powered data extraction
5. **Summarize** - Generate page summaries

## Getting Started

### Initialize Agentic Session

```kotlin
import ai.platon.pulsar.sdk.v0.*

fun main() {
    val session = AgenticSession.getOrCreate()
    val agent = session.companionAgent
    val driver = session.driver
    
    driver.navigateTo("https://example.com")
    
    // Your AI automation code here
    
    session.close()
}
```

### Set API Key

For AI features, configure your API key:

```bash
export OPENROUTER_API_KEY="your-api-key-here"
```

Or in code:

```kotlin
val options = LocalDriverOptions(
    javaOptions = mapOf(
        "OPENROUTER_API_KEY" to "your-api-key"
    )
)
```

## Agent Act

Execute single actions described in natural language:

```kotlin
val agent = session.companionAgent
driver.navigateTo("https://example.com")

// Click an element
val result = agent.act("click the search button")
if (result.success) {
    println("Action: ${"$"}{result.message}")
}

// Fill a form
agent.act("fill the search box with 'kotlin programming'")
agent.act("press Enter in the search box")

// Navigate
agent.act("click the 'More information' link")
```

### Act with Options

```kotlin
val result = agent.act(
    action = "click the submit button",
    multiAct = false,              // Single action mode
    modelName = "gpt-4",           // Specific model
    domSettleTimeoutMs = 2000,     // Wait for DOM changes
    timeoutMs = 30000              // Overall timeout
)
```

## Agent Run

Run autonomous multi-step tasks:

```kotlin
val agent = session.companionAgent
driver.navigateTo("https://example.com")

// Complex task
val result = agent.run(
    "search for 'kotlin' and extract the top 3 results"
)

if (result.success) {
    println("Task completed: ${"$"}{result.message}")
    println("Result: ${"$"}{result.finalResult}")
    println("Steps: ${"$"}{result.historySize}")
}
```

### Run with Monitoring

```kotlin
val result = agent.run(
    task = "navigate to the products page, filter by price, and extract all items",
    multiAct = true,
    timeoutMs = 60000
)

// Check trace
result.trace?.forEach { println("Trace: ${"$"}it") }
```

## Agent Observe

Analyze the page and get action suggestions:

```kotlin
val observation = agent.observe(
    instruction = "what interactive elements are on this page?"
)

observation.observations.forEach { obs ->
    println("Element: ${"$"}{obs.locator}")
    println("Description: ${"$"}{obs.description}")
    println("Suggested action: ${"$"}{obs.method}")
    println("Next steps: ${"$"}{obs.nextSuggestions}")
}
```

### Observe with Options

```kotlin
val observation = agent.observe(
    instruction = "find all form fields",
    returnAction = true,           // Get actionable suggestions
    drawOverlay = true,            // Highlight elements visually
    domSettleTimeoutMs = 2000
)
```

## AI-Powered Data Extraction

### Natural Language Extraction

```kotlin
driver.navigateTo("https://example.com/product")

val result = agent.extract(
    instruction = "extract the product name, price, and availability"
)

if (result.success) {
    println("Extracted: ${"$"}{result.data}")
}
```

### Schema-Based Extraction

Define exact structure:

```kotlin
val schema = mapOf(
    "type" to "object",
    "properties" to mapOf(
        "name" to mapOf("type" to "string"),
        "price" to mapOf("type" to "number"),
        "currency" to mapOf("type" to "string"),
        "inStock" to mapOf("type" to "boolean"),
        "features" to mapOf(
            "type" to "array",
            "items" to mapOf("type" to "string")
        )
    ),
    "required" to listOf("name", "price")
)

val result = agent.extract(
    instruction = "extract product details",
    schema = schema
)
```

### Scoped Extraction

Extract from specific regions:

```kotlin
val result = agent.extract(
    instruction = "extract all review ratings and comments",
    selector = ".reviews-section"
)
```

## Page Summarization

Generate intelligent summaries:

```kotlin
driver.navigateTo("https://example.com/article")

// Basic summarization
val summary = agent.summarize()
println("Summary: ${"$"}summary")

// Guided summarization
val summary2 = agent.summarize(
    instruction = "summarize the main benefits of this product"
)

// Scoped summarization
val summary3 = agent.summarize(
    instruction = "summarize customer reviews",
    selector = ".reviews"
)
```

## Event Streaming

Monitor AI operations in real-time:

```kotlin
// Register event handlers
session.agentEventHandlers.agent.on("onWillAct") { event ->
    println("About to act: ${"$"}{event.message}")
}

session.agentEventHandlers.agent.on("onDidAct") { event ->
    println("Action completed: ${"$"}{event.metadata}")
}

session.agentEventHandlers.inference.on("onWillInfer") { event ->
    println("Calling LLM...")
}

// Run with event streaming
val result = session.runWithEvents(
    "search for kotlin and extract results"
)
```

### Available Event Types

```kotlin
// Agent lifecycle
AgentEventHandlers.EventTypes.ON_WILL_RUN
AgentEventHandlers.EventTypes.ON_DID_RUN
AgentEventHandlers.EventTypes.ON_WILL_ACT
AgentEventHandlers.EventTypes.ON_DID_ACT
AgentEventHandlers.EventTypes.ON_WILL_OBSERVE
AgentEventHandlers.EventTypes.ON_DID_OBSERVE

// Inference events
AgentEventHandlers.EventTypes.ON_WILL_INFER
AgentEventHandlers.EventTypes.ON_DID_INFER

// Tool events  
AgentEventHandlers.EventTypes.ON_WILL_EXECUTE_TOOL
AgentEventHandlers.EventTypes.ON_DID_EXECUTE_TOOL
```

## Agent History

Track agent execution:

```kotlin
val agent = session.companionAgent

agent.run("perform several actions")

// Check history
val history = agent.stateHistory
println("History size: ${"$"}{history.size}")

history.states.forEach { state ->
    println("Step ${"$"}{state.step}: ${"$"}{state.action}")
    println("  Success: ${"$"}{state.success}")
    println("  Result: ${"$"}{state.result}")
}

// Clear history for new task
agent.clearHistory()
```

## Advanced Patterns

### Multi-Step Workflow

```kotlin
val agent = session.companionAgent

// Step 1: Search
agent.act("navigate to the search page")
agent.act("fill search box with 'kotlin'")
agent.act("click search button")

// Step 2: Filter
agent.act("click the price filter")
agent.act("select price range $20-$50")
agent.act("apply filters")

// Step 3: Extract
val products = agent.extract(
    instruction = "extract all product names and prices"
)

println("Found products: ${"$"}{products.data}")
```

### Error Recovery

```kotlin
val agent = session.companionAgent

val result = agent.act("click the submit button")

if (!result.success) {
    println("Action failed: ${"$"}{result.message}")
    
    // Try alternative
    val alt = agent.act("click the send button")
    if (!alt.success) {
        // Observe and decide
        val obs = agent.observe("find submission options")
        // Handle based on observations
    }
}
```

### Conditional Automation

```kotlin
val observation = agent.observe("check if login is required")

val needsLogin = observation.observations.any { obs ->
    obs.description?.contains("login", ignoreCase = true) == true
}

if (needsLogin) {
    agent.act("click the login button")
    agent.act("fill username with 'user@example.com'")
    agent.act("fill password with 'password'")
    agent.act("click submit")
}

// Continue with main task
agent.run("extract product data")
```

## Best Practices

1. **Be specific** - Clear instructions get better results
2. **Break down complex tasks** - Use act() for steps, run() for full workflows
3. **Use schemas** - Define structure for consistent extraction
4. **Handle errors** - Check success and implement fallbacks
5. **Monitor events** - Use event handlers for debugging
6. **Clear history** - Reset between independent tasks
7. **Set timeouts** - Prevent hanging on difficult pages
8. **Scope operations** - Use selectors to focus AI attention
9. **Validate results** - Check extracted data format
10. **Use appropriate models** - Balance cost and capability

## Limitations

- **Requires API key** - OpenRouter or compatible LLM API
- **Costs per request** - LLM API calls incur costs
- **Slower than selectors** - AI reasoning takes time
- **Not deterministic** - Results may vary slightly
- **Best for complex cases** - Use CSS selectors when possible

## Next Steps

- [Data Extraction](data-extraction.md) - Compare with selector-based extraction
- [Examples](../examples/ai-automation.md) - More AI automation examples
- [API Reference](../api/agentic-session.md) - Complete API documentation
