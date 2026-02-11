# Frequently Asked Questions

Common questions about Browser4 Kotlin SDK.

## Installation

### Q: What are the prerequisites?

A: You need:
- Java 17 or later
- Kotlin 1.9+
- Google Chrome (latest version)

### Q: How do I add the SDK to my project?

A: Add to your `pom.xml`:

```xml
<dependency>
    <groupId>ai.platon.pulsar</groupId>
    <artifactId>pulsar-sdk-kotlin</artifactId>
    <version>4.5.0</version>
</dependency>
```

## Local Driver

### Q: Where is Browser4.jar downloaded?

A: By default to `~/.browser4/Browser4.jar`

### Q: How do I use a specific version?

A: Specify the JAR path:

```kotlin
val options = LocalDriverOptions(
    jarPath = "/path/to/specific/Browser4.jar"
)
```

### Q: Port 8182 is in use, what do I do?

A: Use a different port:

```kotlin
val options = LocalDriverOptions(port = 9000)
```

## AI Features

### Q: Do I need an API key?

A: Yes, for AI features set `OPENROUTER_API_KEY`:

```bash
export OPENROUTER_API_KEY="your-key"
```

### Q: Which AI models are supported?

A: Any OpenRouter-compatible model (GPT-4, Claude, etc.)

### Q: How much do AI operations cost?

A: Costs depend on the LLM provider. Check OpenRouter pricing.

## Sessions

### Q: Should I close sessions?

A: Yes, always close sessions to free resources:

```kotlin
session.use {
    // operations
} // Auto-closed
```

### Q: Can I reuse sessions?

A: Yes, create once and reuse for multiple operations:

```kotlin
val session = AgenticSession.getOrCreate()
// Multiple operations
session.close()
```

### Q: How many sessions can I create?

A: Depends on system resources. Each session uses one browser instance.

## Performance

### Q: How can I speed up scraping?

A: Use:
- Caching with `-expire` parameter
- Parallel sessions
- CSS selectors instead of AI

### Q: Why is it slow?

A: Could be:
- Network latency
- Page loading time
- AI inference time
- Too many retries

## Errors

### Q: "Session not found" error?

A: Session expired or was closed. Create a new session.

### Q: "Element not found" error?

A: Use `waitForSelector()` before interacting:

```kotlin
driver.waitForSelector(".element")
driver.click(".element")
```

### Q: Chrome not found?

A: Install Chrome and ensure it's in PATH.

## Best Practices

### Q: When should I use AI vs selectors?

A: Use:
- **CSS selectors** - Fast, deterministic, structured pages
- **AI** - Complex layouts, dynamic content, natural language

### Q: How do I handle pagination?

A: See [examples/basic-usage.md](examples/basic-usage.md#pagination)

### Q: How do I extract tables?

A: See [guide/data-extraction.md](guide/data-extraction.md#table-extraction)

## Troubleshooting

### Q: Timeouts frequently?

A: Increase timeout:

```kotlin
driver.waitForSelector(".element", timeout = 60000)
```

### Q: Memory leaks?

A: Ensure sessions are closed:

```kotlin
try {
    // operations
} finally {
    session.close()
}
```

### Q: Screenshots are blank?

A: Wait for page to render:

```kotlin
driver.navigateTo(url)
Thread.sleep(1000)
val screenshot = driver.captureScreenshot()
```

## Advanced

### Q: Can I use multiple browsers?

A: Yes, create multiple sessions.

### Q: Can I run headless?

A: Yes, Chrome runs headless by default.

### Q: Can I use custom Chrome options?

A: Configure via environment or server config.

## Support

### Q: Where can I get help?

A:
- [GitHub Issues](https://github.com/platonai/Browser4/issues)
- [Documentation](index.md)
- [Examples](examples/basic-usage.md)

### Q: How do I report bugs?

A: Create an issue on GitHub with:
- SDK version
- Code example
- Error message
- Steps to reproduce

## Next Steps

- [Quick Start Guide](getting-started/quick-start.md)
- [API Reference](api/overview.md)
- [Examples](examples/basic-usage.md)
