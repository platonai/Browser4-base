# Introduction

## Welcome to Browser4 Kotlin SDK

Browser4 Kotlin SDK is a powerful client library that enables you to control web browsers, extract data, and automate web interactions using Kotlin. Whether you're building a web scraper, automating workflows, or creating AI-powered browser agents, this SDK provides everything you need.

## Why Browser4 Kotlin SDK?

### 🎯 Purpose-Built for Kotlin

The SDK is designed from the ground up for Kotlin, taking full advantage of:

- **Coroutines** - Efficient async/await patterns for concurrent operations
- **Type Safety** - Strong typing prevents errors at compile time
- **Null Safety** - Eliminates null pointer exceptions
- **Data Classes** - Clean, immutable models for API responses
- **Extension Functions** - Natural, fluent APIs

### 🚀 Three Levels of Abstraction

Choose the API level that fits your needs:

#### 1. PulsarSession - Data Extraction

Best for: Web scraping, data collection, content extraction

```kotlin
val session = PulsarSession.getOrCreate()
val page = session.load("https://example.com")
val document = session.parse(page)
val data = session.extract(document, mapOf("title" to "h1"))
```

#### 2. WebDriver - Browser Automation

Best for: UI testing, form filling, clicking buttons

```kotlin
val driver = session.getOrCreateBoundDriver()
driver.navigateTo("https://example.com")
driver.click("button#submit")
driver.fill("input[name='email']", "user@example.com")
```

#### 3. AgenticSession - AI Automation

Best for: Complex workflows, natural language commands

```kotlin
val agent = session.companionAgent
agent.act("click the login button")
agent.run("search for 'kotlin' and extract the top 5 results")
```

### ⚡ Performance & Scalability

- **Coroutine-Safe** - All operations are thread-safe and efficient
- **Connection Pooling** - Reuses HTTP connections for better performance
- **Lazy Loading** - Resources are loaded only when needed
- **Batch Operations** - Extract multiple fields in a single call

### 🔌 Flexible Deployment

#### Local Driver Mode (Default)

Perfect for development and small-scale use:

```kotlin
val session = AgenticSession.getOrCreate()
// Automatically downloads and starts Browser4.jar
```

#### Remote Server Mode

For production deployments and distributed systems:

```kotlin
val session = AgenticSession.getOrCreate(baseUrl = "http://your-server:8182")
```

## What Can You Build?

### 🕷️ Web Scrapers

Extract structured data from websites:

```kotlin
val products = session.load("https://shop.example.com/products")
val data = session.extract(products, mapOf(
    "name" to ".product-name",
    "price" to ".product-price",
    "rating" to ".product-rating"
))
```

### 🤖 Browser Agents

AI-powered autonomous agents:

```kotlin
agent.run("""
    1. Go to the shopping site
    2. Search for '4k monitors'
    3. Filter by price under $500
    4. Extract the top 5 results with ratings
""")
```

### 🧪 Testing Automation

UI and integration tests:

```kotlin
driver.navigateTo("https://app.example.com/login")
driver.fill("#username", "testuser")
driver.fill("#password", "testpass")
driver.click("#login-button")
driver.waitForSelector(".dashboard")
```

### 📊 Data Collection

Gather data for analysis:

```kotlin
val urls = listOf("url1", "url2", "url3")
val results = urls.map { url ->
    async {
        val page = session.load(url)
        session.extract(session.parse(page), selectors)
    }
}.awaitAll()
```

## Core Concepts

### Sessions

A session represents a browsing context with its own cookies, cache, and state. Sessions are isolated from each other.

### Drivers

A driver controls a specific browser instance within a session. It provides low-level commands for navigation and interaction.

### Agents

An agent is an AI-powered entity that can understand natural language instructions and execute complex tasks autonomously.

### Pages

A page represents a snapshot of a web page at a specific point in time, including its HTML content and metadata.

### Documents

A document is a parsed representation of a page's HTML, allowing you to query and extract data using CSS selectors.

## Prerequisites

Before you begin, ensure you have:

- **Java 17 or later** installed
- **Kotlin 1.9.0 or later** (if using Kotlin DSL)
- **Maven or Gradle** for dependency management
- Basic familiarity with Kotlin coroutines

## What's Next?

Now that you understand the basics, let's get the SDK installed and running:

1. [Installation](installation.md) - Add the SDK to your project
2. [Quick Start](quick-start.md) - Run your first script
3. [First Steps](first-steps.md) - Learn the fundamentals

Or jump directly to:

- [API Reference](../api/overview.md) - Complete API documentation
- [Examples](../examples/basic-usage.md) - Ready-to-run code examples
- [User Guide](../guide/session-management.md) - In-depth tutorials
