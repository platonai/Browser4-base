# API Reference Overview

Complete API reference for the Browser4 Kotlin SDK.

## Core Classes

### [PulsarClient](pulsar-client.md)
Low-level HTTP client for Browser4 API communication.

**Key responsibilities:**
- Session management
- HTTP request/response handling  
- Local driver management

### [PulsarSession](pulsar-session.md)
High-level session for page loading and extraction.

**Key capabilities:**
- URL normalization
- Page loading (open, load, submit)
- HTML parsing
- CSS selector-based extraction

### [AgenticSession](agentic-session.md)  
AI-powered browser automation extending PulsarSession.

**AI capabilities:**
- Natural language actions (act, run)
- Page observation and suggestions
- Intelligent data extraction
- Content summarization

### [WebDriver](webdriver.md)
Browser control and element interaction.

**Core features:**
- Page navigation
- Element interaction (click, fill, type)
- Scrolling and waiting
- Screenshot capture
- JavaScript execution

### [Data Models](models.md)
Data structures for API responses and requests.

## Quick Reference

### Creating Sessions

```kotlin
// Default session (local driver)
val session = AgenticSession.getOrCreate()

// Custom configuration
val client = PulsarClient(
    baseUrl = "http://localhost:8182",
    useLocalDriver = true
)
client.createSession()
val session = AgenticSession(client)
```

### Loading Pages

```kotlin
// Fresh load (bypass cache)
val page = session.open("https://example.com")

// Cached load
val page = session.load("https://example.com", "-expire 1d")

// Background processing
val page = session.submit("https://example.com")
```

### Navigation

```kotlin
val driver = session.driver

driver.navigateTo("https://example.com")
driver.goBack()
driver.goForward()
driver.reload()
```

### Element Interaction

```kotlin
driver.click("button.submit")
driver.fill("input[name='search']", "kotlin")
driver.press("input[name='search']", "Enter")
driver.waitForSelector(".results")
```

### Data Extraction

```kotlin
// CSS selectors
val fields = session.extract(document, mapOf(
    "title" to "h1",
    "price" to ".price"
))

// WebDriver
val title = driver.selectFirstTextOrNull("h1")
val links = driver.selectAttributeAll("a", "href")

// AI-powered
val result = agent.extract(
    "extract product name and price"
)
```

### AI Automation

```kotlin
val agent = session.companionAgent

// Single action
agent.act("click the search button")

// Multi-step task
agent.run("search for kotlin and extract results")

// Observe
val obs = agent.observe("find interactive elements")
```

## Type Hierarchy

```
AutoCloseable
    ↓
PulsarSession
    ↓
AgenticSession (implements PerceptiveAgent)

PulsarClient (standalone)
WebDriver (standalone, uses PulsarClient)
```

## Thread Safety

- **PulsarClient**: Thread-safe
- **PulsarSession**: Not thread-safe, one per thread
- **AgenticSession**: Not thread-safe, one per thread
- **WebDriver**: Not thread-safe, bound to session

## Error Handling

All methods may throw exceptions. Wrap in try-catch:

```kotlin
try {
    val session = AgenticSession.getOrCreate()
    // Operations
    session.close()
} catch (e: Exception) {
    println("Error: ${e.message}")
}
```

## Next Steps

- [PulsarClient API](pulsar-client.md) - Client reference
- [PulsarSession API](pulsar-session.md) - Session reference
- [AgenticSession API](agentic-session.md) - AI session reference
- [WebDriver API](webdriver.md) - Driver reference
- [Data Models](models.md) - Model reference
