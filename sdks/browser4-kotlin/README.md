# Browser4 Kotlin SDK

Kotlin SDK for Browser4 WebDriver-Compatible API based on OpenAPI specification.

This SDK provides a Kotlin interface to the Browser4 browser automation platform,
enabling web scraping, data extraction, and AI-powered browser interaction.

## Features

- **Local Driver Mode**: Automatically downloads and starts Browser4.jar (no server required!)
- **Session Management**: Create, manage, and delete browser sessions
- **Navigation**: Navigate to URLs, go back/forward, reload pages
- **Element Interaction**: Click, fill, type, press keys, hover, focus
- **Scrolling**: Scroll down/up, scroll to elements, scroll to top/bottom
- **Content Extraction**: Extract text, attributes, and HTML content
- **Screenshots**: Capture screenshots of pages or elements
- **Script Execution**: Execute JavaScript in the browser
- **AI-Powered Automation**: Natural language commands for browser interaction

## Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>ai.platon.pulsar</groupId>
    <artifactId>pulsar-sdk-kotlin</artifactId>
    <version>4.6.0-SNAPSHOT</version>
</dependency>
```

Or for Gradle:

```kotlin
implementation("ai.platon.pulsar:pulsar-sdk-kotlin:4.6.0-SNAPSHOT")
```

## Quick Start

### Local Driver Mode (Recommended)

The SDK now automatically downloads and starts Browser4.jar when no server URL is specified:

```kotlin
import ai.platon.pulsar.sdk.v0.*

fun main() {
    // Automatically downloads and starts Browser4.jar
    val session = AgenticSession.getOrCreate()
    val agent = session.companionAgent
    val driver = session.getOrCreateBoundDriver()

    // Open and parse a page
    val url = "https://example.com"
    var page = session.open(url)
    var document = session.parse(page)

    // Extract data with CSS selectors
    var fields = session.extract(document, mapOf("title" to "h1"))
    println("Title: ${fields["title"]}")

    // Use natural language actions
    var result = agent.act("click the search button")
    println("Action: ${result.message}")

    // Run autonomous tasks
    var history = agent.run("search for 'kotlin' and extract results")
    println("Task: ${history.message}")

    // Capture live page state
    page = session.capture(driver)
    document = session.parse(page)

    // Get content from live DOM
    val content = driver.selectFirstTextOrNull("body")
    println("Content: ${content?.take(100)}")

    // Clean up (stops local driver automatically)
    session.context.close()
}
```

### Using Local Driver with Custom Configuration

```kotlin
import ai.platon.pulsar.sdk.v0.*

fun main() {
    // Configure local driver options
    val options = LocalDriverOptions(
        port = 9000,
        javaOptions = mapOf(
            "OPENROUTER_API_KEY" to "your-api-key"
        )
    )

    val client = PulsarClient(
        useLocalDriver = true,
        localDriverOptions = options
    )
    client.createSession()
    val session = AgenticSession(client)

    // Use session...

    session.close()
    client.close() // Stops local driver
}
```

### Basic Usage with PulsarSession

For more control over client configuration:

```kotlin
import ai.platon.pulsar.sdk.v0.*

fun main() {
    // Create client and session
    val client = PulsarClient(baseUrl = "http://localhost:8182")
    val sessionId = client.createSession()

    val session = PulsarSession(client)

    // Load a page
    val page = session.load("https://example.com", "-expire 1d")
    println("Loaded page: ${page.url}")

    // Navigate and interact using WebDriver
    session.driver.navigateTo("https://example.com")
    println("Current URL: ${session.driver.currentUrl()}")

    // Extract data
    val title = session.driver.selectFirstTextOrNull("h1")
    println("Title: $title")

    // Clean up
    session.close()
    client.close()
}
```

### AI-Powered Automation with AgenticSession

Using explicit client creation:

```kotlin
import ai.platon.pulsar.sdk.v0.*

fun main() {
    val client = PulsarClient()
    client.createSession()

    val session = AgenticSession(client)

    // Navigate to a page
    session.driver.navigateTo("https://example.com")

    // Use natural language to interact
    val actResult = session.act("click the search button")
    println("Action success: ${actResult.success}")

    // Run autonomous task
    val runResult = session.run("search for 'kotlin' and click first result")
    println("Task success: ${runResult.success}")

    // Observe page state
    val observation = session.observe("find all interactive elements")
    observation.observations.forEach { obs ->
        println("Found: ${obs.description}")
    }

    // AI-powered extraction
    val extraction = session.agentExtract("extract the main heading and first paragraph")
    println("Extracted data: ${extraction.data}")

    // Summarize page content
    val summary = session.summarize()
    println("Summary: $summary")

    session.close()
    client.close()
}
```

### WebDriver Usage

```kotlin
import ai.platon.pulsar.sdk.v0.*

fun main() {
    val client = PulsarClient()
    client.createSession()
    val driver = WebDriver(client)

    // Navigation
    driver.navigateTo("https://example.com")
    println("URL: ${driver.currentUrl()}")
    println("Title: ${driver.title()}")

    // Element interaction
    driver.click("button.submit")
    driver.fill("input[name='search']", "kotlin")
    driver.press("input[name='search']", "Enter")

    // Wait for elements
    driver.waitForSelector(".results")

    // Scrolling
    driver.scrollToBottom()
    driver.scrollTo(".footer")

    // Content extraction
    val texts = driver.selectTextAll(".result-item")
    texts.forEach { println(it) }

    // Extract multiple fields
    val fields = driver.extract(mapOf(
        "title" to "h1",
        "description" to ".description",
        "price" to ".price"
    ))
    println("Fields: $fields")

    // Screenshots
    val screenshot = driver.captureScreenshot()
    println("Screenshot length: ${screenshot?.length}")

    // Execute JavaScript
    val result = driver.executeScript("return document.querySelectorAll('a').length")
    println("Links count: $result")

    driver.close()
    client.deleteSession()
    client.close()
}
```

## API Reference

### PulsarClient

Low-level HTTP client for API communication.

| Method | Description |
|--------|-------------|
| `createSession(capabilities)` | Create a new browser session |
| `deleteSession(sessionId)` | Delete a session |
| `post(path, body, sessionId)` | Make a POST request |
| `get(path, sessionId)` | Make a GET request |
| `delete(path, sessionId)` | Make a DELETE request |

### PulsarSession

Session management for page loading and extraction.

| Method | Description |
|--------|-------------|
| `normalize(url, args)` | Normalize a URL with load arguments |
| `open(url, args)` | Open a URL immediately (bypass cache) |
| `load(url, args)` | Load from cache or fetch from internet |
| `submit(url, args)` | Submit URL to crawl pool |
| `extract(document, selectors)` | Extract fields using CSS selectors |
| `scrape(url, args, selectors)` | Load, parse, and extract in one operation |

### AgenticSession

AI-powered browser automation.

| Method | Description |
|--------|-------------|
| `act(action)` | Execute a single action in natural language |
| `run(task)` | Run an autonomous agent task |
| `observe(instruction)` | Observe page and suggest actions |
| `agentExtract(instruction, schema)` | AI-powered data extraction |
| `summarize(instruction)` | Generate page summary |
| `clearHistory()` | Clear agent history |

### WebDriver

Browser control and element interaction.

**Navigation:**
- `navigateTo(url)`, `currentUrl()`, `reload()`, `goBack()`, `goForward()`

**Element Interaction:**
- `click(selector)`, `fill(selector, text)`, `type(selector, text)`
- `press(selector, key)`, `hover(selector)`, `focus(selector)`
- `check(selector)`, `uncheck(selector)`

**Waiting:**
- `waitForSelector(selector, timeout)`, `waitForNavigation()`
- `exists(selector)`, `isVisible(selector)`, `isHidden(selector)`

**Scrolling:**
- `scrollDown(count)`, `scrollUp(count)`, `scrollTo(selector)`
- `scrollToTop()`, `scrollToBottom()`, `scrollToMiddle(ratio)`

**Content:**
- `selectFirstTextOrNull(selector)`, `selectTextAll(selector)`
- `selectFirstAttributeOrNull(selector, attr)`, `selectAttributeAll(selector, attr)`
- `outerHtml(selector)`, `textContent(selector)`
- `extract(fields)` - Extract multiple fields at once

**Screenshots:**
- `captureScreenshot(selector, fullPage)`, `screenshot(selector)`

**Scripts:**
- `executeScript(script, args)`, `executeAsyncScript(script, args, timeout)`
- `evaluate(expression)`

**Control:**
- `delay(millis)`, `pause()`, `stop()`

## Data Models

### WebPage
Represents a loaded web page.

```kotlin
data class WebPage(
    val url: String,
    val location: String?,
    val contentType: String?,
    val contentLength: Int,
    val protocolStatus: String?,
    val isNil: Boolean,
    val html: String?
)
```

### AgentRunResult
Result from agent run operation.

```kotlin
data class AgentRunResult(
    val success: Boolean,
    val message: String,
    val historySize: Int,
    val processTraceSize: Int,
    val finalResult: Any?,
    val trace: List<String>?
)
```

### AgentActResult
Result from agent act operation.

```kotlin
data class AgentActResult(
    val success: Boolean,
    val message: String,
    val action: String?,
    val isComplete: Boolean,
    val expression: String?,
    val result: Any?,
    val trace: List<String>?
)
```

### ObserveResult
Single observation result from agent observe operation.

```kotlin
data class ObserveResult(
    val locator: String?,
    val domain: String?,
    val method: String?,
    val arguments: Map<String, Any?>?,
    val description: String?,
    val screenshotContentSummary: String?,
    val currentPageContentSummary: String?,
    val nextGoal: String?,
    val thinking: String?,
    val summary: String?,
    val keyFindings: String?,
    val nextSuggestions: List<String>?
)
```

## Server Requirements

### Local Driver Mode (Default)

When using `AgenticSession.getOrCreate()` without parameters, the SDK automatically:
1. Downloads Browser4.jar from GitHub releases (if not already present in `~/.browser4/`)
2. Starts the Browser4 server on port 8182
3. Connects to the local server

The Browser4.jar is stored in `~/.browser4/Browser4.jar` by default.

**Environment Variables:**
- `OPENROUTER_API_KEY`: Your OpenRouter API key (automatically passed to the local driver)

### Remote Server Mode

To connect to an existing Browser4 server instead of using the local driver:

```kotlin
val session = AgenticSession.getOrCreate(baseUrl = "http://your-server:8182")
```

Or with explicit configuration:

```kotlin
val client = PulsarClient(baseUrl = "http://your-server:8182")
client.createSession()
val session = AgenticSession(client)
```

## Configuration

### Local Driver Options

```kotlin
data class LocalDriverOptions(
    val jarPath: String? = null,              // Custom path for Browser4.jar
    val downloadUrl: String? = null,          // Custom download URL
    val port: Int? = null,                    // Custom port (default: 8182)
    val javaOptions: Map<String, String> = emptyMap()  // Java system properties
)
```

Example:

```kotlin
val options = LocalDriverOptions(
    jarPath = "/custom/path/Browser4.jar",
    port = 9000,
    javaOptions = mapOf(
        "OPENROUTER_API_KEY" to "your-api-key",
        "server.port" to "9000"
    )
)

val client = PulsarClient(
    useLocalDriver = true,
    localDriverOptions = options
)
```

## Documentation

📚 **[Complete Documentation Site](docs/)** - Comprehensive guides, API reference, and examples

Quick links:
- [Getting Started Guide](docs/docs/getting-started/introduction.md)
- [Quick Start](docs/docs/getting-started/quick-start.md)
- [API Reference](docs/docs/api/overview.md)
- [Examples](docs/docs/examples/basic-usage.md)
- [中文文档](docs/docs/zh/index.md)

To build and view the documentation locally:

```bash
cd docs
./build.sh
./serve.sh
```

The documentation will be available at http://127.0.0.1:8000

## License

Apache License, Version 2.0

See [LICENSE](../../LICENSE) for details.
