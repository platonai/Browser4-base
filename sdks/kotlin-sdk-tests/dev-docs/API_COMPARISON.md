# Kotlin SDK and FusedActs API Comparison

This document shows how the Kotlin SDK now aligns with the FusedActs example API patterns.

## Session Creation

### FusedActs (Internal)
```kotlin
import ai.platon.pulsar.agentic.context.AgenticContexts

private val session = AgenticContexts.getOrCreateSession()
```

### Kotlin SDK (Now)
```kotlin
import ai.platon.pulsar.sdk.AgenticSession

private val session = AgenticSession.getOrCreate()
```

## Getting Agent and Driver

### FusedActs
```kotlin
val agent = session.companionAgent
val driver = session.getOrCreateBoundDriver()
```

### Kotlin SDK
```kotlin
val agent = session.companionAgent  // ✓ Same
val driver = session.getOrCreateBoundDriver()  // ✓ Same
```

## Page Loading and Parsing

### FusedActs
```kotlin
var page = session.open(url)
var document = session.parse(page)
var fields = session.extract(document, mapOf("title" to "#title"))
```

### Kotlin SDK
```kotlin
var page = session.open(url)  // ✓ Same
var document = session.parse(page)  // ✓ Same
var fields = session.extract(document, mapOf("title" to "#title"))  // ✓ Same
```

## Agent Actions

### FusedActs
```kotlin
var result = agent.act("click the 3rd link")
var history = agent.run("search for 'kotlin' and extract results")
agent.clearHistory()
agent.processTrace.forEach { println("🚩$it") }
```

### Kotlin SDK
```kotlin
var result = agent.act("click the 3rd link")  // ✓ Same
var history = agent.run("search for 'kotlin' and extract results")  // ✓ Same
agent.clearHistory()  // ✓ Same
agent.processTrace.forEach { println("🚩$it") }  // ✓ Same
```

## Driver Operations

### FusedActs
```kotlin
var content = driver.selectFirstTextOrNull("body")
```

### Kotlin SDK
```kotlin
var content = driver.selectFirstTextOrNull("body")  // ✓ Same
```

## Page Capture

### FusedActs
```kotlin
page = session.capture(driver)
document = session.parse(page)
```

### Kotlin SDK
```kotlin
page = session.capture(driver)  // ✓ Same
document = session.parse(page)  // ✓ Same
```

## Session Cleanup

### FusedActs
```kotlin
session.context.close()
```

### Kotlin SDK
```kotlin
session.context.close()  // ✓ Same (context returns self)
// or simply:
session.close()  // Also works
```

## Complete Example Comparison

### FusedActs Example
```kotlin
class FusedActs {
    private val session = AgenticContexts.getOrCreateSession()
    
    suspend fun run() {
        val url = "https://example.com"
        val agent = session.companionAgent
        val driver = session.getOrCreateBoundDriver()
        
        var page = session.open(url)
        var document = session.parse(page)
        var fields = session.extract(document, mapOf("title" to "#title"))
        
        var result = agent.act("click the search button")
        var history = agent.run("search for 'kotlin'")
        
        page = session.capture(driver)
        var content = driver.selectFirstTextOrNull("body")
        
        agent.clearHistory()
        agent.processTrace.forEach { println("🚩$it") }
        
        session.context.close()
    }
}
```

### Kotlin SDK Example (Now)
```kotlin
class MyExample {
    private val session = AgenticSession.getOrCreate()
    
    suspend fun run() {
        val url = "https://example.com"
        val agent = session.companionAgent
        val driver = session.getOrCreateBoundDriver()
        
        var page = session.open(url)
        var document = session.parse(page)
        var fields = session.extract(document, mapOf("title" to "#title"))
        
        var result = agent.act("click the search button")
        var history = agent.run("search for 'kotlin'")
        
        page = session.capture(driver)
        var content = driver.selectFirstTextOrNull("body")
        
        agent.clearHistory()
        agent.processTrace.forEach { println("🚩$it") }
        
        session.context.close()
    }
}
```

## Summary

✅ **All FusedActs API patterns are now supported in the Kotlin SDK**

The only difference is the import statement:
- FusedActs: `import ai.platon.pulsar.agentic.context.AgenticContexts`
- SDK: `import ai.platon.pulsar.sdk.AgenticSession`

And the factory method:
- FusedActs: `AgenticContexts.getOrCreateSession()`
- SDK: `AgenticSession.getOrCreate()`

All other APIs are identical, making it easy to migrate code or follow internal examples.

## Backward Compatibility

The SDK still supports the explicit client approach for users who need more control:

```kotlin
val client = PulsarClient(baseUrl = "http://custom-server:8182")
client.createSession(capabilities = mapOf("browserName" to "chrome"))
val session = AgenticSession(client)
// ... use session
session.close()
client.close()
```

Both patterns are fully supported and tested.
