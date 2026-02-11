# Browser4 Kotlin SDK

**Version:** 4.5.0

Welcome to the Browser4 Kotlin SDK documentation! This SDK provides a powerful and intuitive interface for browser automation, web scraping, and AI-powered web interaction.

## What is Browser4 Kotlin SDK?

Browser4 Kotlin SDK is a Kotlin-based client library that provides a high-level interface to the Browser4 browser automation platform. It enables developers to:

- 🤖 **Automate Browser Interactions** - Control web browsers programmatically with intuitive APIs
- 🚀 **Extract Web Data** - Extract structured data from web pages using CSS selectors or AI
- 🧠 **AI-Powered Automation** - Use natural language to describe browser actions
- ⚡ **High Performance** - Built with Kotlin coroutines for efficient concurrent operations
- 🎯 **Simple & Powerful** - Easy to learn, yet powerful enough for complex automation tasks

## Key Features

### 🎮 Multiple API Styles

The SDK supports three distinct usage patterns to fit your needs:

- **PulsarSession** - For data extraction and page loading
- **WebDriver** - For traditional browser automation with element interaction
- **AgenticSession** - For AI-powered natural language automation

### 🔌 Flexible Deployment

- **Local Driver Mode** - Automatically downloads and runs Browser4 locally
- **Remote Server Mode** - Connect to existing Browser4 servers
- **Docker Support** - Run in containerized environments

### 📦 Rich Functionality

- Session management and lifecycle control
- Navigation and page interaction
- Element selection and manipulation
- Data extraction with CSS selectors or XPath
- Screenshot capture
- JavaScript execution
- AI-powered actions and observations
- Coroutine-safe async operations

## Quick Example

Here's a taste of what you can do with the SDK:

```kotlin
import ai.platon.pulsar.sdk.v0.*

suspend fun main() {
    // Create a session with automatic local driver
    val session = AgenticSession.getOrCreate()
    val driver = session.getOrCreateBoundDriver()
    val agent = session.companionAgent

    // Open a page and extract data
    val page = session.open("https://example.com")
    val document = session.parse(page)
    val fields = session.extract(document, mapOf(
        "title" to "h1",
        "description" to "p"
    ))

    println("Title: ${fields["title"]}")
    println("Description: ${fields["description"]}")

    // Use AI for natural language automation
    agent.act("click the search button")
    agent.run("search for 'kotlin' and click the first result")

    // Clean up
    session.context.close()
}
```

## Getting Started

Ready to dive in? Check out these resources:

- [Installation Guide](getting-started/installation.md) - Set up the SDK in your project
- [Quick Start](getting-started/quick-start.md) - Run your first automation script
- [API Reference](api/overview.md) - Explore the complete API
- [Examples](examples/basic-usage.md) - Learn from practical examples

## Architecture

The SDK is organized into several key components:

```
AgenticSession (AI-powered automation)
    ↓ extends
PulsarSession (Data extraction & page loading)
    ↓ uses
WebDriver (Browser control & element interaction)
    ↓ uses
PulsarClient (Low-level HTTP communication)
```

Each layer builds upon the previous one, giving you the flexibility to use the level of abstraction that best suits your needs.

## Community & Support

- **GitHub Repository**: [platonai/Browser4](https://github.com/platonai/Browser4)
- **Issues**: [Report bugs or request features](https://github.com/platonai/Browser4/issues)
- **Docker**: [Browser4 Docker Images](https://hub.docker.com/r/galaxyeye88/browser4)

## License

Browser4 Kotlin SDK is licensed under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0).

## Next Steps

- 📘 [Read the Getting Started Guide](getting-started/introduction.md)
- 🎓 [Explore the User Guide](guide/session-management.md)
- 💻 [Try the Examples](examples/basic-usage.md)
- 📚 [Browse the API Reference](api/overview.md)
