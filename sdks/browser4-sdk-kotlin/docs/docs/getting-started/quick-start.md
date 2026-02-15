# Quick Start

Get up and running with Browser4 Kotlin SDK in minutes.

## Prerequisites

- **Java 17 or later** - Required to run the Browser4 driver
- **Kotlin 1.9+** - For SDK usage
- **Google Chrome** - Latest version installed on your system

## Installation

Add the Browser4 SDK dependency to your project:

### Maven

```xml
<dependency>
    <groupId>ai.platon.pulsar</groupId>
    <artifactId>pulsar-sdk-kotlin</artifactId>
    <version>4.6.0-SNAPSHOT</version>
</dependency>
```

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("ai.platon.pulsar:pulsar-sdk-kotlin:4.6.0-SNAPSHOT")
}
```

### Gradle (Groovy)

```groovy
dependencies {
    implementation 'ai.platon.pulsar:pulsar-sdk-kotlin:4.6.0-SNAPSHOT'
}
```

## Your First Program

Here's a simple example that loads a web page and extracts data:

```kotlin
import ai.platon.pulsar.sdk.v0.*

fun main() {
    // Create a session (automatically starts local driver)
    val session = AgenticSession.getOrCreate()

    // Load a web page
    val page = session.open("https://example.com")
    println("Loaded: ${page.url}")

    // Parse the page
    val document = session.parse(page)

    // Extract data using CSS selectors
    val fields = session.extract(document, mapOf(
        "title" to "h1",
        "description" to "p"
    ))

    println("Title: ${fields["title"]}")
    println("Description: ${fields["description"]}")

    // Clean up
    session.close()
}
```

Run this program, and you'll see:

```text
Loaded: https://example.com
Title: Example Domain
Description: This domain is for use in illustrative examples...
```

## What Just Happened?

1. **Automatic Driver Setup**: `AgenticSession.getOrCreate()` automatically downloaded and started the Browser4 driver in the background
2. **Page Loading**: `session.open()` opened the URL in a real Chrome browser
3. **Parsing**: `session.parse()` parsed the HTML into a structured document
4. **Data Extraction**: `session.extract()` used CSS selectors to extract specific fields

## Next Steps

Now that you have a working setup, explore these topics:

- **[First Steps](first-steps.md)** - Learn the fundamentals step by step
- **[Session Management](../guide/session-management.md)** - Understand session lifecycle
- **[Navigation Guide](../guide/navigation.md)** - Navigate between pages
- **[Data Extraction](../guide/data-extraction.md)** - Advanced extraction techniques
- **[AI Automation](../guide/ai-automation.md)** - Use natural language to automate browsing

## Quick Examples

### Using WebDriver for Interaction

```kotlin
import ai.platon.pulsar.sdk.v0.*

fun main() {
    val session = AgenticSession.getOrCreate()
    val driver = session.driver

    // Navigate to a page
    driver.navigateTo("https://example.com")

    // Click elements
    driver.click("button.submit")

    // Fill forms
    driver.fill("input[name='search']", "kotlin programming")
    driver.press("input[name='search']", "Enter")

    // Extract text
    val results = driver.selectTextAll(".result-item")
    results.forEach { println(it) }

    session.close()
}
```

### AI-Powered Automation

```kotlin
import ai.platon.pulsar.sdk.v0.*

fun main() {
    val session = AgenticSession.getOrCreate()
    val agent = session.companionAgent

    // Navigate to a page
    session.driver.navigateTo("https://example.com")

    // Use natural language to interact
    val result = agent.act("click the 'More information' link")
    println("Action result: ${result.message}")

    // Run autonomous tasks
    val taskResult = agent.run("find the email address on this page")
    println("Task result: ${taskResult.message}")

    session.close()
}
```

### Capturing Screenshots

```kotlin
import ai.platon.pulsar.sdk.v0.*
import java.io.File
import java.util.Base64

fun main() {
    val session = AgenticSession.getOrCreate()
    val driver = session.driver

    driver.navigateTo("https://example.com")

    // Capture full-page screenshot
    val screenshot = driver.captureScreenshot(fullPage = true)

    // Save to file
    if (screenshot != null) {
        val bytes = Base64.getDecoder().decode(screenshot)
        File("screenshot.png").writeBytes(bytes)
        println("Screenshot saved to screenshot.png")
    }

    session.close()
}
```

## Troubleshooting

### Port Already in Use

If you see an error about port 8182 already in use, you can specify a different port:

```kotlin
val options = LocalDriverOptions(port = 9000)
val client = PulsarClient(useLocalDriver = true, localDriverOptions = options)
client.createSession()
val session = AgenticSession(client)
```

### Chrome Not Found

Ensure Google Chrome is installed and accessible in your system PATH. The Browser4 driver uses Chrome for browser automation.

### Timeout Errors

For slow-loading pages, you can increase timeouts:

```kotlin
// Wait longer for navigation
driver.waitForNavigation(timeout = 30000) // 30 seconds
```

## Configuration

### Environment Variables

For AI-powered features, set your API key:

```bash
export OPENROUTER_API_KEY="your-api-key-here"
```

The local driver automatically picks up this environment variable.

### Custom Configuration

```kotlin
val options = LocalDriverOptions(
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

## Learn More

- **[API Reference](../api/overview.md)** - Complete API documentation
- **[Examples](../examples/basic-usage.md)** - More code examples
- **[FAQ](../faq.md)** - Frequently asked questions
