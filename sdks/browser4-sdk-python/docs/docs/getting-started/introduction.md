# Introduction

Welcome to Browser4 Python SDK! This guide will help you understand what Browser4 is and how the Python SDK can help you build powerful browser automation solutions.

## What is Browser4?

Browser4 is a next-generation browser automation platform designed for AI agents and high-performance web scraping. Unlike traditional browser automation tools, Browser4 is built from the ground up to be:

- **AI-Native** - Execute natural language commands and multi-step autonomous tasks
- **Lightning Fast** - Handle 100k-200k complex page visits per machine per day
- **Coroutine-Safe** - Built for modern async/concurrent applications
- **Intelligent** - Combines LLM, ML, and traditional selectors for robust extraction

## What Can You Do With Browser4?

### Web Scraping at Scale
Extract data from complex websites with hybrid approaches:
- CSS selectors for structured data
- AI-powered extraction for dynamic content
- ML-based field detection across similar pages

### AI-Powered Automation
Let AI agents handle complex workflows:
- Natural language commands: "click the login button"
- Multi-step tasks: "search for X and filter by Y"
- Intelligent observation and action planning

### Traditional Browser Automation
Full WebDriver-compatible API for precise control:
- Element interaction (click, type, fill)
- Navigation and history management
- Screenshots and script execution

## Why Choose Browser4 Python SDK?

### Zero Configuration
The SDK includes **Browser4Driver** which automatically:
- Downloads the Browser4 server (one-time operation)
- Starts and manages the Java process
- Handles health checking and graceful shutdown
- No manual installation or setup required!

### Pythonic API
Clean, idiomatic Python interface:
```python
from browser4 import Browser4Driver, PulsarClient, AgenticSession

with Browser4Driver() as driver:
    client = PulsarClient(base_url=driver.base_url)
    session = AgenticSession(client)
    
    # Simple, readable API
    page = session.open("https://example.com")
    result = session.act("click the search button")
    session.close()
```

### Production Ready
Built for real-world use:
- Comprehensive error handling
- Type hints throughout
- Extensive documentation
- Battle-tested in production environments

## Key Concepts

### Sessions
Sessions manage browser state and provide the main API:
- **PulsarSession** - Basic session with loading and extraction
- **AgenticSession** - AI-powered session with autonomous capabilities

### WebDriver
Browser control interface for element interaction:
- Navigate, click, type, scroll
- Wait for elements and conditions
- Extract data with selectors

### Pages and Documents
- **WebPage** - Represents a loaded web page from the server
- **Document** - Parsed HTML for local extraction (via BeautifulSoup)

### AI Agents
Built-in AI capabilities for automation:
- **act()** - Execute single natural language actions
- **run()** - Run multi-step autonomous tasks
- **observe()** - Get AI observations about page state

## Next Steps

Ready to get started?

1. **[Install the SDK](installation.md)** - Set up your environment
2. **[Quick Start](quick-start.md)** - Write your first script
3. **[First Steps](first-steps.md)** - Learn essential patterns

Or dive into specific topics:
- [Session Management](../guide/session-management.md)
- [AI-Powered Automation](../guide/ai-automation.md)
- [Data Extraction](../guide/data-extraction.md)
