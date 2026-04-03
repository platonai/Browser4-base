# Browser4 Python SDK

Welcome to the **Browser4 Python SDK** documentation! This SDK provides a powerful Python interface to the Browser4 browser automation platform, enabling advanced web scraping, data extraction, and AI-powered browser automation.

## What is Browser4?

Browser4 is a lightning-fast, coroutine-safe browser engine designed specifically for AI agents. It provides:

- **Browser Agents** — Fully autonomous browser agents that reason, plan, and execute end-to-end tasks
- **Browser Automation** — High-performance automation for workflows, navigation, and data extraction
- **Machine Learning** — Learn field structures across complex pages without consuming tokens
- **Extreme Performance** — Fully coroutine-safe; supports 100k ~ 200k complex page visits per machine per day
- **Hybrid Extraction** — Combines LLM, ML, and selectors for clean data across chaotic pages

## Features

The Python SDK provides access to all Browser4 capabilities:

### 🚀 Automatic Server Management
- **Browser4Driver** automatically downloads and starts the Browser4 server
- No manual setup required - just start coding!
- Automatic health checking and lifecycle management

### 🤖 AI-Powered Automation
- Execute natural language commands: `session.act("click the login button")`
- Run multi-step tasks: `session.run("search for 'python' and click the first result")`
- Intelligent page observation and action planning

### 🌐 WebDriver-Compatible API
- Full browser control with familiar WebDriver methods
- Element interaction: click, type, fill, hover, scroll
- Advanced selectors and wait conditions

### 📊 Powerful Data Extraction
- CSS selector-based extraction
- AI-powered structured data extraction
- BeautifulSoup integration for local parsing

### 📸 Screenshots & Scripting
- Full-page and element screenshots
- JavaScript execution and evaluation
- Custom script injection

## Quick Example

```python
from browser4 import Browser4Driver, PulsarClient, AgenticSession

# Browser4Driver automatically downloads and starts the server
with Browser4Driver() as driver:
    # Create client and session
    client = PulsarClient(base_url=driver.base_url)
    session_id = client.create_session()
    session = AgenticSession(client)

    # Navigate to a page
    page = session.open("https://example.com")
    print(f"Opened: {page.url}")

    # Use WebDriver for element interaction
    driver_wd = session.driver
    driver_wd.fill("input[name='search']", "browser automation")
    driver_wd.press("input[name='search']", "Enter")

    # Extract data using CSS selectors
    fields = session.extract(page, {
        "title": "h1",
        "description": ".description"
    })
    print(fields)

    # Use AI-powered actions
    result = session.act("click the login button")
    print(f"Action success: {result.success}")

    # Run multi-step tasks
    history = session.run("search for 'python' and click the first result")
    print(f"Task completed: {history.success}")

    # Clean up
    session.close()
    client.close()
```

## Getting Started

Ready to start? Check out these resources:

1. **[Installation](getting-started/installation.md)** - Install the SDK and dependencies
2. **[Quick Start](getting-started/quick-start.md)** - Your first Browser4 script
3. **[First Steps](getting-started/first-steps.md)** - Essential concepts and patterns
4. **[User Guide](guide/session-management.md)** - In-depth guides for all features
5. **[API Reference](api/overview.md)** - Complete API documentation

## Architecture

The SDK is organized into several key components:

- **Browser4Driver** - Manages the Browser4 server lifecycle
- **PulsarClient** - Low-level HTTP client for API communication
- **PulsarSession** - Session management for page loading and extraction
- **AgenticSession** - AI-powered automation (extends PulsarSession)
- **WebDriver** - Browser control and element interaction
- **Models** - Data models for pages, results, and agent state

## Community & Support

- **GitHub**: [platonai/Browser4](https://github.com/platonai/Browser4)
- **Docker**: [galaxyeye88/browser4](https://hub.docker.com/r/galaxyeye88/browser4)
- **Documentation**: [browser4.io](https://browser4.io)

## License

Copyright © 2025 Platon AI. All rights reserved.
