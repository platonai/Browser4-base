# Coroutines Migration Guide

The Kotlin SDK has been migrated to use Kotlin coroutines and Ktor HTTP client for better performance and modern async/await patterns.

## What Changed

### HTTP Client
- **Before**: Java `HttpClient` (blocking)
- **After**: Ktor `HttpClient` with CIO engine (non-blocking, coroutine-based)

### API Methods
- Methods that make HTTP calls are now `suspend` functions
- Pure computation methods (like `parse`, `extract`) remain regular functions

## Usage Examples

### Basic Usage with runBlocking

```kotlin
import ai.platon.pulsar.sdk.*
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    // Create client and session
    val client = PulsarClient(baseUrl = "http://localhost:8182")
    client.createSession()
    
    val session = PulsarSession(client)
    
    // Load a page (suspend function)
    val page = session.open("https://example.com")
    
    // Parse (regular function - no suspend needed)
    val document = session.parse(page)
    
    // Extract data (regular function)
    val fields = session.extract(document!!, mapOf(
        "title" to "h1",
        "description" to "p"
    ))
    
    println("Title: ${fields["title"]}")
    
    // Clean up
    client.deleteSession()
    client.close()
}
```

### Using AgenticSession with Coroutines

```kotlin
import ai.platon.pulsar.sdk.*
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    // Get or create a session (suspend function)
    val session = AgenticSession.getOrCreate()
    
    // All agent methods are now suspend functions
    val actResult = session.act("click the search button")
    val runResult = session.run("search for 'kotlin' and extract results")
    val observation = session.observe("What can I do on this page?")
    
    // Clean up
    session.close()
    AgenticSession.resetDefault()
}
```

### Using in Async Context

```kotlin
import ai.platon.pulsar.sdk.*
import kotlinx.coroutines.*

suspend fun loadMultiplePages(urls: List<String>) {
    val client = PulsarClient()
    client.createSession()
    val session = PulsarSession(client)
    
    // Load pages concurrently
    val pages = urls.map { url ->
        async {
            session.load(url)
        }
    }.awaitAll()
    
    // Process results
    pages.forEach { page ->
        println("Loaded: ${page.url}")
    }
    
    client.deleteSession()
    client.close()
}
```

### Testing with Coroutines

```kotlin
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

class MyTest {
    @Test
    fun `test session operations`() = runTest {
        val client = PulsarClient()
        client.createSession()
        val session = PulsarSession(client)
        
        val page = session.open("https://example.com")
        assertNotNull(page.html)
        
        client.deleteSession()
        client.close()
    }
}
```

## Migration Checklist

If you're migrating existing code:

1. **Add coroutine context**: Wrap your code in `runBlocking { }` or make your function `suspend`
2. **Update HTTP calls**: All methods that call the API are now `suspend`
3. **Keep local operations as-is**: `parse()`, `extract()` for Jsoup, and utility methods don't need `await`
4. **Update tests**: Use `runTest` from `kotlinx-coroutines-test` for tests with suspend functions
5. **Handle session cleanup**: Call `deleteSession()` explicitly in suspend context before closing

## Which Methods Are Suspend?

### Suspend Functions (make HTTP calls)
- `PulsarClient`: `createSession()`, `deleteSession()`, `get()`, `post()`, `delete()`
- `PulsarSession`: `open()`, `load()`, `submit()`, `normalize()`, `scrape()`, `chat()`
- `WebDriver`: All navigation and interaction methods
- `AgenticSession`: All agent methods (`act()`, `run()`, `observe()`, `extract()`, `summarize()`)

### Regular Functions (local operations)
- `PulsarSession`: `parse()`, `extract()` (Jsoup overloads), `data()`, `property()`, `options()`
- Helper methods: `bindDriver()`, `unbindDriver()`, `registerClosable()`, etc.

## Benefits

- **Non-blocking**: Better resource utilization with concurrent operations
- **Modern async/await**: Clean, readable asynchronous code
- **Ktor integration**: Modern, multiplatform HTTP client
- **Better performance**: Efficient coroutine-based I/O

## Dependencies Added

```xml
<!-- Kotlin Coroutines -->
<dependency>
    <groupId>org.jetbrains.kotlinx</groupId>
    <artifactId>kotlinx-coroutines-core</artifactId>
    <version>1.10.1</version>
</dependency>

<!-- Ktor HTTP Client -->
<dependency>
    <groupId>io.ktor</groupId>
    <artifactId>ktor-client-core-jvm</artifactId>
    <version>3.0.3</version>
</dependency>
<dependency>
    <groupId>io.ktor</groupId>
    <artifactId>ktor-client-cio-jvm</artifactId>
    <version>3.0.3</version>
</dependency>
```

## Need Help?

See the test files in `src/test/kotlin/ai/platon/pulsar/sdk/` for more examples.
