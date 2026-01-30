# AI Automation Examples

Examples of using AI-powered automation with Browser4 Kotlin SDK.

## Simple AI Actions

```kotlin
import ai.platon.pulsar.sdk.v0.*

fun main() {
    val session = AgenticSession.getOrCreate()
    val agent = session.companionAgent
    val driver = session.driver
    
    driver.navigateTo("https://example.com")
    
    // Click using natural language
    agent.act("click the search button")
    
    // Fill form
    agent.act("fill the search box with 'kotlin programming'")
    agent.act("press Enter in the search box")
    
    session.close()
}
```

## Autonomous Task Execution

```kotlin
val agent = session.companionAgent
driver.navigateTo("https://example.com")

val result = agent.run(
    "navigate to products page, apply price filter $20-$50, and extract product names"
)

if (result.success) {
    println("Task result: ${result.finalResult}")
}
```

## Intelligent Data Extraction

```kotlin
driver.navigateTo("https://example.com/product")

val result = agent.extract(
    instruction = "extract product name, price, and customer rating"
)

if (result.success) {
    println("Extracted: ${result.data}")
}
```

## Schema-Based Extraction

```kotlin
val schema = mapOf(
    "type" to "object",
    "properties" to mapOf(
        "title" to mapOf("type" to "string"),
        "price" to mapOf("type" to "number"),
        "currency" to mapOf("type" to "string"),
        "features" to mapOf(
            "type" to "array",
            "items" to mapOf("type" to "string")
        ),
        "inStock" to mapOf("type" to "boolean")
    )
)

val result = agent.extract(
    instruction = "extract product details",
    schema = schema
)
```

## Page Observation

```kotlin
val observation = agent.observe(
    instruction = "find all interactive elements"
)

observation.observations.forEach { obs ->
    println("Element: ${obs.locator}")
    println("Description: ${obs.description}")
    println("Suggestions: ${obs.nextSuggestions}")
}
```

## Event Monitoring

```kotlin
// Register handlers
session.agentEventHandlers.agent.on("onWillAct") { event ->
    println("About to perform action: ${event.message}")
}

session.agentEventHandlers.agent.on("onDidAct") { event ->
    println("Action completed: ${event.metadata}")
}

// Run with monitoring
val result = session.runWithEvents(
    "search for kotlin and extract top 5 results"
)
```

## Complex Workflow

```kotlin
val agent = session.companionAgent
driver.navigateTo("https://example.com")

// Check if login needed
val obs = agent.observe("check if login is required")
val needsLogin = obs.observations.any { 
    it.description?.contains("login", ignoreCase = true) == true
}

if (needsLogin) {
    agent.act("click login button")
    agent.act("fill username with user@example.com")
    agent.act("fill password with password123")
    agent.act("click submit button")
}

// Main task
val result = agent.run(
    "navigate to orders page and extract recent orders"
)

println("Orders: ${result.finalResult}")
```

See also:
- [AI Automation Guide](../guide/ai-automation.md)
- [Complete Workflow](complete-workflow.md)
