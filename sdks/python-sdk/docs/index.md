# Browser4 Python SDK

Welcome to the **Browser4 Python SDK** documentation! This SDK provides a powerful Python interface to the Browser4 browser automation platform, enabling web scraping, data extraction, and AI-powered browser interaction.

## What is Browser4?

Browser4 is an advanced browser automation platform that combines:

- **Traditional WebDriver API** for precise element control
- **AI-Powered Automation** with natural language instructions
- **Intelligent Web Scraping** with built-in data extraction
- **Session Management** with caching and persistence

## Key Features

### 🤖 AI-Powered Automation
Execute complex browser tasks using natural language:
```python
session.run("search for 'python tutorials' and click the first result")
```

### 🎯 WebDriver-Compatible API
Full control over browser elements:
```python
driver.fill("input[name='search']", "browser automation")
driver.click("button[type='submit']")
```

### 📊 Data Extraction
Extract structured data with CSS selectors:
```python
fields = session.extract(page, {
    "title": "h1",
    "price": ".price",
    "description": ".desc"
})
```

### 🔄 Session Management
Efficient page loading with caching:
```python
page = session.load("https://example.com", args="-expire 1d")
```

## Quick Example

```python
from pulsar_sdk import PulsarClient, AgenticSession

# Initialize
client = PulsarClient(base_url="http://localhost:8182")
client.create_session()
session = AgenticSession(client)

# Navigate and extract
page = session.open("https://example.com")
data = session.extract(page, {
    "title": "h1",
    "links": "a[href]"
})
print(data)

# AI-powered automation
result = session.run("scroll to bottom and click 'Contact Us'")
print(f"Task completed: {result.success}")

# Cleanup
session.close()
```

## Architecture

The SDK consists of four main components:

1. **PulsarClient**: Low-level HTTP client for API communication
2. **PulsarSession**: Session management for page loading and extraction
3. **AgenticSession**: AI-powered automation (extends PulsarSession)
4. **WebDriver**: Browser control and element interaction

```
┌─────────────────────────────────────┐
│        AgenticSession               │
│  (AI automation + all features)     │
├─────────────────────────────────────┤
│        PulsarSession                │
│  (Page loading + extraction)        │
├─────────────────────────────────────┤
│    WebDriver    │   PulsarClient    │
│ (Element control)│  (HTTP client)   │
└─────────────────────────────────────┘
```

## Getting Started

Ready to dive in? Start with these guides:

- **[Installation](installation.md)**: Install the SDK and set up your environment
- **[Quick Start](quickstart.md)**: Get up and running in minutes
- **[Basic Usage](guides/basic-usage.md)**: Learn the fundamentals
- **[API Reference](api-reference/client.md)**: Explore the complete API

## Use Cases

### Web Scraping
Extract data from websites efficiently with built-in parsing and CSS selectors.

### Automated Testing
Use WebDriver-compatible API for browser testing and QA automation.

### AI-Powered Tasks
Leverage natural language to perform complex browser interactions without writing detailed scripts.

### Data Collection
Build robust web scraping pipelines with session management and caching.

## Requirements

- Python 3.9 or higher
- Browser4 server running (default: `http://localhost:8182`)
- Dependencies: `requests`, `beautifulsoup4`

## Installation

```bash
pip install -e .[dev]
```

See the [Installation Guide](installation.md) for detailed instructions.

## Support

- **GitHub**: [platonai/Browser4](https://github.com/platonai/Browser4)
- **Issues**: [Report a bug or request a feature](https://github.com/platonai/Browser4/issues)
- **Documentation**: You're reading it!

## License

This SDK is part of the Browser4 project. See the repository for license information.

---

Ready to get started? Continue to the [Installation Guide](installation.md) →
