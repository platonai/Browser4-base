# API Reference Overview

Welcome to the Browser4 Python SDK API reference. This SDK provides a comprehensive interface to the Browser4 browser automation platform, enabling web scraping, data extraction, and AI-powered browser interaction.

## Core Classes

The SDK is organized around several key classes that work together to provide a complete browser automation solution:

| Class | Purpose | Module |
|-------|---------|--------|
| [**Browser4Driver**](browser4-driver.md) | Lifecycle management for the Browser4 server | `browser4.driver` |
| [**PulsarClient**](pulsar-client.md) | Low-level HTTP client for API communication | `browser4.client` |
| [**PulsarSession**](pulsar-session.md) | Session management for page loading and extraction | `browser4.agentic_session` |
| [**AgenticSession**](agentic-session.md) | AI-powered browser automation (extends PulsarSession) | `browser4.agentic_session` |
| [**WebDriver**](webdriver.md) | Browser control and element interaction | `browser4.webdriver` |

## Data Models

The SDK includes comprehensive data models for working with API responses:

| Model | Purpose | Module |
|-------|---------|--------|
| [**WebPage**](models.md#webpage) | Represents a loaded web page | `browser4.models` |
| [**NormURL**](models.md#normUrl) | Normalized URL with load arguments | `browser4.models` |
| [**AgentRunResult**](models.md#agentRunResult) | Result from agent run operation | `browser4.models` |
| [**AgentActResult**](models.md#agentActResult) | Result from agent act operation | `browser4.models` |
| [**AgentObservation**](models.md#agentObservation) | Page observation results | `browser4.models` |
| [**ExtractionResult**](models.md#extractionResult) | AI-powered extraction results | `browser4.models` |

See the [Models Reference](models.md) for a complete list of all data models.

## Quick Import Guide

### Basic Imports

```python
from browser4 import (
    Browser4Driver,
    PulsarClient,
    PulsarSession,
    AgenticSession,
    WebDriver
)
```

### With Data Models

```python
from browser4 import (
    Browser4Driver,
    PulsarClient,
    AgenticSession,
    WebPage,
    NormURL,
    AgentRunResult,
    AgentActResult
)
```

### Import All

```python
import browser4

# Access classes
driver = browser4.Browser4Driver()
client = browser4.PulsarClient()
session = browser4.AgenticSession(client)
```

## Common Usage Patterns

### Pattern 1: Automatic Server Management

```python
from browser4 import Browser4Driver, PulsarClient, AgenticSession

# Browser4Driver automatically downloads and starts the server
with Browser4Driver() as driver:
    client = PulsarClient(base_url=driver.base_url)
    session_id = client.create_session()
    session = AgenticSession(client)

    # Use the session
    page = session.open("https://example.com")
    result = session.act("click the search button")

    # Cleanup
    session.close()
```

### Pattern 2: Manual Server Management

```python
from browser4 import Browser4Driver, PulsarClient, PulsarSession

# Start server manually
driver = Browser4Driver()
driver.start()

try:
    client = PulsarClient(base_url=driver.base_url)
    session_id = client.create_session()
    session = PulsarSession(client)

    # Load and parse pages
    page = session.load("https://example.com", "-expire 1d")
    document = session.parse(page)

finally:
    driver.stop()
```

### Pattern 3: Connect to Existing Server

```python
from browser4 import PulsarClient, AgenticSession

# Connect to an already-running Browser4 server
client = PulsarClient(base_url="http://localhost:8182")
session_id = client.create_session()
session = AgenticSession(client)

# Use AI-powered automation
result = session.run("search for 'python' and extract the top 5 results")
print(result.success)
print(result.final_result)

session.close()
```

### Pattern 4: Direct WebDriver Control

```python
from browser4 import PulsarClient, WebDriver

client = PulsarClient()
client.create_session()
driver = WebDriver(client)

# Low-level browser control
driver.navigate_to("https://example.com")
driver.click("button.submit")
driver.fill("input[name='search']", "query text")
title = driver.select_first_text_or_null("h1")
screenshot = driver.capture_screenshot()
```

## Class Hierarchy

```
Browser4Driver           # Server lifecycle management
    ↓
PulsarClient            # HTTP API communication
    ↓
PulsarSession           # Page loading & extraction
    ↓
AgenticSession          # AI-powered automation
    |
    +→ WebDriver        # Browser control (composition)
```

## Key Capabilities

### 🚀 Browser Automation
- Navigate to URLs and control browser state
- Click, fill forms, press keys, hover elements
- Scroll, capture screenshots, execute JavaScript
- Wait for elements and navigation events

### 📄 Page Loading & Caching
- Load pages with intelligent caching
- Support for expiration policies and refresh
- Parse HTML with BeautifulSoup integration
- Extract data using CSS selectors

### 🤖 AI-Powered Agents
- Execute actions via natural language instructions
- Run autonomous multi-step tasks
- Observe pages and suggest next actions
- Extract structured data with AI assistance
- Summarize page content

### 🔧 Server Management
- Automatic download of Browser4.jar
- Health checking and startup verification
- Configurable ports and Java options
- Context manager support for cleanup

## Next Steps

- [Browser4Driver Reference](browser4-driver.md) - Server lifecycle management
- [PulsarClient Reference](pulsar-client.md) - HTTP client API
- [PulsarSession Reference](pulsar-session.md) - Session management
- [AgenticSession Reference](agentic-session.md) - AI-powered automation
- [WebDriver Reference](webdriver.md) - Browser control
- [Models Reference](models.md) - Data models

## Version Information

Current SDK version: **0.1.0**

For more examples and guides, see the main [documentation](../index.md).
