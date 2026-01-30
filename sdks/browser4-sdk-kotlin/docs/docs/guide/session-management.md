# Session Management

Complete guide to managing sessions in the Browser4 Kotlin SDK.

## Overview

A session represents an isolated browser context with its own cookies, storage, and state. Sessions are the foundation of all Browser4 operations.

## Session Lifecycle

```
Create Session → Use Session → Close Session
      ↓              ↓              ↓
  Initialize    Operations    Cleanup
```

## Creating Sessions

### Default Session (Recommended)

The simplest approach uses the static `getOrCreate()` method:

```kotlin
import ai.platon.pulsar.sdk.v0.*

fun main() {
    // Get or create default session
    val session = AgenticSession.getOrCreate()
    
    // Use session
    session.driver.navigateTo("https://example.com")
    
    // Close when done
    session.close()
}
```

**Characteristics:**
- Singleton pattern - same instance returned on subsequent calls
- Automatic local driver management
- Survives across multiple `getOrCreate()` calls
- Must call `session.close()` or `AgenticSession.resetDefault()` to clean up

### Fresh Session

Create a new session each time:

```kotlin
import ai.platon.pulsar.sdk.v0.*
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    // Always creates a new session
    val session = AgenticSession.create()
    
    // Use session
    session.driver.navigateTo("https://example.com")
    
    // Clean up
    session.close()
}
```

### Custom Configuration

For advanced control over the client and driver:

```kotlin
import ai.platon.pulsar.sdk.v0.*
import ai.platon.pulsar.sdk.v0.detail.PulsarClient
import ai.platon.pulsar.sdk.v0.detail.LocalDriverOptions

fun main() {
    // Configure local driver
    val options = LocalDriverOptions(
        port = 9000,
        jarPath = "/custom/path/Browser4.jar",
        javaOptions = mapOf(
            "OPENROUTER_API_KEY" to "your-api-key",
            "server.port" to "9000"
        )
    )
    
    // Create client with options
    val client = PulsarClient(
        useLocalDriver = true,
        localDriverOptions = options
    )
    
    // Create session
    client.createSession()
    val session = AgenticSession(client)
    
    // Use session
    session.driver.navigateTo("https://example.com")
    
    // Clean up
    session.close()
    client.close() // Also stops local driver
}
```

## Session Types

### AgenticSession

The most powerful session type with AI capabilities:

```kotlin
val session = AgenticSession.getOrCreate()

// All PulsarSession methods
val page = session.open("https://example.com")
val document = session.parse(page)

// Plus AI-powered methods
val agent = session.companionAgent
agent.act("click the login button")
agent.run("search for 'kotlin' and extract the top result")

// WebDriver access
val driver = session.driver
driver.navigateTo("https://google.com")

session.close()
```

**Use AgenticSession when:**
- You need AI-powered automation
- You want natural language control
- You need both extraction and interaction

### PulsarSession

Basic session for loading and extraction:

```kotlin
import ai.platon.pulsar.sdk.v0.*
import ai.platon.pulsar.sdk.v0.detail.PulsarClient

fun main() {
    val client = PulsarClient(useLocalDriver = true)
    client.createSession()
    val session = PulsarSession(client)
    
    // Load and extract
    val page = session.load("https://example.com")
    val document = session.parse(page)
    val fields = session.extract(document, mapOf("title" to "h1"))
    
    println("Title: ${fields["title"]}")
    
    session.close()
    client.close()
}
```

**Use PulsarSession when:**
- You only need page loading and extraction
- You don't need AI features
- You want minimal overhead

## Remote Server Connection

Connect to an existing Browser4 server:

```kotlin
import ai.platon.pulsar.sdk.v0.*
import ai.platon.pulsar.sdk.v0.detail.PulsarClient

fun main() {
    // Connect to remote server
    val client = PulsarClient(baseUrl = "http://remote-server:8182")
    client.createSession()
    val session = AgenticSession(client)
    
    // Use session normally
    session.driver.navigateTo("https://example.com")
    
    session.close()
    client.close()
}
```

Or with the convenience method:

```kotlin
// Connect to remote server
val session = AgenticSession.getOrCreate(
    baseUrl = "http://remote-server:8182",
    useLocalDriver = false
)

// Use session
session.driver.navigateTo("https://example.com")

session.close()
```

## Session Properties

Access session information:

```kotlin
val session = AgenticSession.getOrCreate()

// Session identifiers
println("Session ID: ${session.id}")
println("Session UUID: ${session.uuid}")
println("Display: ${session.display}")

// Session state
println("Is active: ${session.isActive}")

// Access client
val client = session.client
println("Base URL: ${client.resolvedBaseUrl}")
```

## Managing Multiple Sessions

### Sequential Sessions

Create and use sessions one at a time:

```kotlin
import ai.platon.pulsar.sdk.v0.*
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    // First session
    val session1 = AgenticSession.create()
    session1.driver.navigateTo("https://example.com")
    val data1 = session1.driver.selectFirstTextOrNull("h1")
    session1.close()
    
    // Second session (fresh state)
    val session2 = AgenticSession.create()
    session2.driver.navigateTo("https://example.org")
    val data2 = session2.driver.selectFirstTextOrNull("h1")
    session2.close()
    
    println("Data1: $data1")
    println("Data2: $data2")
}
```

### Parallel Sessions

Use multiple sessions simultaneously:

```kotlin
import ai.platon.pulsar.sdk.v0.*
import ai.platon.pulsar.sdk.v0.detail.PulsarClient
import kotlinx.coroutines.*

fun main() = runBlocking {
    // Create multiple clients and sessions
    val sessions = (1..3).map {
        val client = PulsarClient(useLocalDriver = false, baseUrl = "http://localhost:8182")
        client.createSession()
        AgenticSession(client)
    }
    
    // Use sessions in parallel
    val results = sessions.mapIndexed { index, session ->
        async {
            session.driver.navigateTo("https://example.com?id=$index")
            val data = session.driver.selectFirstTextOrNull("h1")
            session.close()
            data
        }
    }.awaitAll()
    
    println("Results: $results")
    
    // Clean up clients
    sessions.forEach { it.client.close() }
}
```

## Session Cleanup

### Manual Cleanup

Always close sessions when done:

```kotlin
val session = AgenticSession.getOrCreate()
try {
    // Use session
    session.driver.navigateTo("https://example.com")
} finally {
    session.close()
}
```

### Automatic Cleanup with `use`

Kotlin's `use` function ensures cleanup:

```kotlin
import ai.platon.pulsar.sdk.v0.*
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    AgenticSession.create().use { session ->
        session.driver.navigateTo("https://example.com")
        val title = session.driver.title()
        println("Title: $title")
        // session.close() called automatically
    }
}
```

### Reset Default Session

Clean up the singleton default session:

```kotlin
// Use default session
val session = AgenticSession.getOrCreate()
session.driver.navigateTo("https://example.com")

// Later, reset it (closes session and stops local driver)
AgenticSession.resetDefault()

// Next call creates a fresh session
val newSession = AgenticSession.getOrCreate()
```

## WebDriver Binding

Each session can have an associated WebDriver:

```kotlin
val session = AgenticSession.getOrCreate()

// Access driver (creates if needed)
val driver = session.driver

// Check if driver is bound
val boundDriver = session.boundDriver // null if not created yet

// Get or create bound driver
val driver2 = session.getOrCreateBoundDriver()

session.close()
```

## Session Configuration

### Timeouts

Configure request timeouts:

```kotlin
import ai.platon.pulsar.sdk.v0.detail.PulsarClient

val client = PulsarClient(
    baseUrl = "http://localhost:8182"
    // Timeout configuration is handled internally by the HTTP client
)
```

### Headers

Add custom headers to all requests:

```kotlin
import ai.platon.pulsar.sdk.v0.detail.PulsarClient

val client = PulsarClient(
    baseUrl = "http://localhost:8182",
    defaultHeaders = mapOf(
        "X-Custom-Header" to "custom-value",
        "User-Agent" to "MyApp/1.0"
    )
)
client.createSession()
val session = AgenticSession(client)
```

## Local Driver Options

Customize the local Browser4 driver:

```kotlin
import ai.platon.pulsar.sdk.v0.detail.LocalDriverOptions

val options = LocalDriverOptions(
    // Custom JAR path
    jarPath = "/opt/browser4/Browser4.jar",
    
    // Custom download URL (for automatic download)
    downloadUrl = "https://github.com/platonai/Browser4/releases/download/latest/Browser4.jar",
    
    // Custom port
    port = 9000,
    
    // Java system properties / environment variables
    javaOptions = mapOf(
        "OPENROUTER_API_KEY" to "your-api-key",
        "server.port" to "9000",
        "logging.level.root" to "INFO"
    )
)
```

## Session State Management

### Capture Page State

Capture current browser state:

```kotlin
val session = AgenticSession.getOrCreate()
val driver = session.driver

driver.navigateTo("https://example.com")

// Interact with page
driver.click("button.load-more")
driver.waitForSelector(".new-content")

// Capture current state
val snapshot = session.capture(driver)
println("Captured URL: ${snapshot.url}")
println("Captured HTML length: ${snapshot.html?.length}")

session.close()
```

### Parse Snapshot

Parse captured state:

```kotlin
val snapshot = session.capture(driver)
val document = session.parse(snapshot)

val fields = session.extract(document, mapOf(
    "title" to "h1",
    "items" to ".item"
))

println("Extracted: $fields")
```

## Error Handling

Handle session errors gracefully:

```kotlin
import ai.platon.pulsar.sdk.v0.*

fun main() {
    var session: AgenticSession? = null
    try {
        session = AgenticSession.getOrCreate()
        session.driver.navigateTo("https://example.com")
        
        // Operations that might fail
        val title = session.driver.selectFirstTextOrNull("h1")
            ?: throw Exception("Title not found")
        
        println("Title: $title")
        
    } catch (e: Exception) {
        println("Error: ${e.message}")
    } finally {
        session?.close()
    }
}
```

## Session Pooling

For high-throughput applications, implement session pooling:

```kotlin
import ai.platon.pulsar.sdk.v0.*
import ai.platon.pulsar.sdk.v0.detail.PulsarClient
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentLinkedQueue

class SessionPool(
    private val baseUrl: String = "http://localhost:8182",
    private val poolSize: Int = 5
) {
    private val pool = ConcurrentLinkedQueue<AgenticSession>()
    
    suspend fun init() {
        repeat(poolSize) {
            val client = PulsarClient(baseUrl = baseUrl, useLocalDriver = false)
            client.createSession()
            val session = AgenticSession(client)
            pool.offer(session)
        }
    }
    
    fun acquire(): AgenticSession? = pool.poll()
    
    fun release(session: AgenticSession) {
        pool.offer(session)
    }
    
    fun close() {
        while (pool.isNotEmpty()) {
            pool.poll()?.close()
        }
    }
}

// Usage
fun main() = runBlocking {
    val pool = SessionPool(poolSize = 3)
    pool.init()
    
    try {
        val session = pool.acquire()
        if (session != null) {
            try {
                session.driver.navigateTo("https://example.com")
                val title = session.driver.title()
                println("Title: $title")
            } finally {
                pool.release(session)
            }
        }
    } finally {
        pool.close()
    }
}
```

## Best Practices

1. **Always close sessions** - Use `try-finally` or `.use {}` to ensure cleanup
2. **Reuse sessions** - Create once, use many times within the same operation
3. **Avoid session leaks** - Track created sessions and close them
4. **Use appropriate session type** - AgenticSession for AI, PulsarSession for basic tasks
5. **Configure timeouts** - Set appropriate timeouts for your use case
6. **Handle connection errors** - Implement retry logic for network issues
7. **Monitor resources** - Sessions consume memory and browser resources
8. **Use session pooling** - For high-throughput applications

## Troubleshooting

### Session Creation Fails

```kotlin
try {
    val session = AgenticSession.getOrCreate()
} catch (e: Exception) {
    // Check if Browser4 server is running
    // Check port availability
    // Check network connectivity
    println("Failed to create session: ${e.message}")
}
```

### Session Becomes Inactive

```kotlin
val session = AgenticSession.getOrCreate()

if (!session.isActive) {
    println("Session is not active")
    // Recreate session
    AgenticSession.resetDefault()
    val newSession = AgenticSession.getOrCreate()
}
```

### Memory Leaks

Monitor and clean up sessions:

```kotlin
// Keep track of created sessions
val sessions = mutableListOf<AgenticSession>()

try {
    repeat(10) {
        val session = AgenticSession.create()
        sessions.add(session)
        // Use session
    }
} finally {
    // Clean up all sessions
    sessions.forEach { it.close() }
    sessions.clear()
}
```

## Next Steps

- **[Navigation Guide](navigation.md)** - Learn about page navigation
- **[Element Interaction](element-interaction.md)** - Interact with page elements
- **[Data Extraction](data-extraction.md)** - Extract structured data
- **[AI Automation](ai-automation.md)** - Use AI-powered features
