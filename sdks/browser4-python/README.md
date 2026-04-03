# Browser4 Python SDK - For Testing and Development Only

Python SDK is developed for e2e tests only, do not use it in production.

You should use Browser4 by agents via SKILLs + CLI instead.

## Installation

```bash
# Recommended: use uv (fast, lockfile-aware)
# Creates/updates a virtualenv and installs this project + dev deps
uv sync --extra dev

# Run commands inside the managed environment
uv run python -c "import browser4; print(browser4.__version__ if hasattr(browser4, '__version__') else 'ok')"

# (Optional) pip-style workflow via uv's pip wrapper
# uv pip install -e ".[dev]"
```

## Quick Start

The easiest way to get started is using `Browser4Driver`, which automatically downloads and starts the Browser4 server:

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
    web_driver = session.driver
    web_driver.fill("input[name='search']", "browser automation")
    web_driver.press("input[name='search']", "Enter")

    # Extract data using CSS selectors (parse first, then extract)
    document = session.parse(page)
    fields = session.extract(document, {
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

# Server stops automatically when exiting the context
```

If you already have a Browser4 server running, you can connect to it directly:

```python
from browser4 import PulsarClient, AgenticSession

# Connect to existing server
client = PulsarClient(base_url="http://localhost:8182")
session_id = client.create_session()
session = AgenticSession(client)

# ... use the session ...

session.close()
```

## API Overview

### Browser4Driver

Manages the lifecycle of a local Browser4.jar process, including automatic download, startup, and shutdown.

```python
from browser4 import Browser4Driver

# Using context manager (recommended)
with Browser4Driver() as driver:
    print(f"Server running at: {driver.base_url}")
    # Use the server...

# Manual control
driver = Browser4Driver(
    port=8183,  # Custom port
    java_options={"custom.key": "value"}  # Java system properties
)
driver.start()
print(f"Server healthy: {driver.is_server_healthy()}")
# ... use the server ...
driver.stop()

# Configuration options
driver = Browser4Driver(
    jar_path="/custom/path/Browser4.jar",  # Custom jar location
    download_url="https://custom-url/Browser4.jar",  # Custom download URL
    port=8182,  # Server port
    java_options={"spring.profiles.active": "rest,private"}  # Java options
)
```

**Key features:**
- Automatically downloads Browser4.jar from GitHub releases if not present
- Starts and manages the Java process
- Health checking with automatic retry
- Graceful shutdown with cleanup
- Context manager support for automatic resource management
- Proxy support (uses system proxy settings)

### PulsarClient

Low-level HTTP client for API communication.

```python
from browser4 import PulsarClient

client = PulsarClient(
    base_url="http://localhost:8182",
    timeout=30.0,
    default_headers={"Authorization": "Bearer token"}
)

# Create a session
session_id = client.create_session(capabilities={"browserName": "chrome"})

# Delete session when done
client.delete_session()
```

### PulsarSession

Session management for page loading and data extraction.

```python
from browser4 import PulsarClient, PulsarSession

client = PulsarClient()
client.create_session()
session = PulsarSession(client)

# Normalize URLs with load arguments
norm_url = session.normalize("https://example.com", args="-expire 1d")

# Open URL immediately (bypass cache)
page = session.open("https://example.com")

# Load from cache or fetch
page = session.load("https://example.com", args="-expire 1d")

# Submit for async processing
session.submit("https://example.com/page1")
session.submit("https://example.com/page2", args="-expire 1h")

# Parse the page and extract fields
document = session.parse(page)
fields = session.extract(document, {
    "title": "h1",
    "links": "a[href]"
})

# Scrape in one operation
data = session.scrape(
    "https://example.com",
    "-expire 1d",
    {"title": ".title", "content": ".content"}
)
```

### AgenticSession

AI-powered browser automation extending PulsarSession.

```python
from browser4 import PulsarClient, AgenticSession

client = PulsarClient()
client.create_session()
session = AgenticSession(client)

# All PulsarSession methods are available
page = session.open("https://example.com")

# Execute single action
result = session.act("click the search button")
print(f"Success: {result.success}, Message: {result.message}")

# Run autonomous task
history = session.run("find the contact form and fill in name='John'")
print(f"Completed: {history.success}")

# Observe page state
observations = session.observe("what actions can I take?")
for obs in observations.observations:
    print(f"- {obs.description}")

# AI-powered extraction
extraction = session.agent_extract(
    instruction="Extract all product names and prices",
    schema={"type": "array", "items": {"type": "object"}}
)

# Summarize page content
summary = session.summarize("Summarize the main article")

# Clear history for new task
session.clear_history()

# Access process trace
print(session.process_trace)
```

### WebDriver

Browser control and element interaction.

```python
from browser4 import PulsarClient, WebDriver

client = PulsarClient()
client.create_session()
driver = WebDriver(client)

# Navigation
driver.navigate_to("https://example.com")
driver.reload()
driver.go_back()
driver.go_forward()

# Get page info
url = driver.current_url()
title = driver.title()
html = driver.page_source()

# Element interaction
driver.click("button.submit")
driver.fill("input[name='email']", "user@example.com")
driver.type("input[name='search']", "query")
driver.press("input[name='search']", "Enter")
driver.hover(".menu-item")
driver.focus("input#username")

# Checkboxes
driver.check("input[type='checkbox']")
driver.uncheck("input[type='checkbox']")

# Scrolling
driver.scroll_down(3)
driver.scroll_up(2)
driver.scroll_to("h2.section")
driver.scroll_to_top()
driver.scroll_to_bottom()
driver.scroll_to_middle(0.5)

# Wait for elements
found = driver.wait_for_selector("h2.title", timeout=10000)
exists = driver.exists(".element")
visible = driver.is_visible(".modal")

# Content extraction
text = driver.select_first_text_or_null("h1")
texts = driver.select_text_all("li.item")
attr = driver.select_first_attribute_or_null("a", "href")
attrs = driver.select_attribute_all("a", "href")

# Multiple field extraction
fields = driver.extract({
    "title": "h1",
    "price": ".price",
    "description": ".desc"
})

# Screenshots
screenshot = driver.capture_screenshot()
element_shot = driver.capture_screenshot(selector=".main-content")

# Script execution
result = driver.execute_script("return document.title")
driver.evaluate("window.scrollY")

# Control
driver.delay(1000)  # milliseconds
driver.pause()
driver.stop()
```

## Data Models

### WebPage

Represents a loaded web page.

```python
from browser4 import WebPage

page = session.load("https://example.com")
print(page.url)           # Page URL
print(page.location)      # Final location after redirects
print(page.content_type)  # e.g., "text/html"
print(page.is_nil)        # True if page is invalid/not found
```

### NormURL

Normalized URL with parsed arguments.

```python
from browser4 import NormURL

norm = session.normalize("https://example.com", args="-expire 1d")
print(norm.spec)    # Full normalized specification
print(norm.url)     # Normalized URL
print(norm.args)    # Parsed arguments
print(norm.is_nil)  # True if URL is invalid
```

### Agent Results

```python
from browser4 import AgentRunResult, AgentActResult

# Run result
run_result = session.run("complete the task")
print(run_result.success)
print(run_result.message)
print(run_result.history_size)

# Act result
act_result = session.act("click button")
print(act_result.success)
print(act_result.action)
print(act_result.is_complete)
```

## Event Handlers (Placeholder)

Event handlers are reserved for future implementation.

```python
from browser4 import PageEventHandlers

# Placeholder - will be implemented in future releases
handlers = PageEventHandlers()
# handlers.browse_event_handlers["onDocumentSteady"] = callback
```

## FusedActs Example

The SDK is designed to mirror the Kotlin FusedActs example:

```python
from browser4 import PulsarClient, AgenticSession

client = PulsarClient(base_url="http://localhost:8182")
client.create_session()
session = AgenticSession(client)

# Step 1: Open URL
url = "https://example.com"
page = session.open(url)
print(f"Opened: {page.url}")

# Step 2: Parse the page (local operation)
document = session.parse(page)

# Step 3: Extract fields with CSS selectors
fields = session.extract(document, {"title": "#title"})
print(f"Title: {fields.get('title')}")

# Step 4: AI-powered action
driver = session.driver
result = session.act("search for 'browser'")
print(f"Action result: {result.message}")

# Step 5: Capture body text
content = driver.select_first_text_or_null("body")
print(f"Body snippet: {content[:160] if content else ''}")

# Step 6: More actions
session.act("click the 3rd link")
session.act("go back")

# Step 7: Run multi-step task
history = session.run("find the search box, type 'web scraping' and submit")
print(f"Task result: {history.final_result}")

# Step 8: Capture and parse
page = session.capture(driver)
document = session.parse(page)
fields = session.extract(document, {"title": "#title"})

# Print process trace
for trace in session.process_trace:
    print(f"🚩 {trace}")

session.close()
```

## Testing

The SDK includes both unit tests and integration tests:

### Unit Tests (Fast)
Unit tests use mocked HTTP responses and don't require a running server:
```bash
# Run unit tests only
uv run pytest -m "not integration"

# Run with coverage
uv run pytest --cov=browser4 -m "not integration"
```

### Integration Tests (With Real Servers)
Integration tests start real Browser4 and Mock servers, matching the Kotlin SDK test infrastructure:
```bash
# Run integration tests
uv run pytest -m integration -v -s
```

Integration tests use:
- **Browser4 REST Server** on port 8182 (started automatically)
- **Mock Site Server** on port 18080 (provides test pages)
- Same test data and endpoints as Kotlin SDK tests

**Note:** First run may take 5-10 minutes to build the Browser4 project.

For more details, see [tests/README.md](tests/README.md).


## Configuration

### Custom Headers

```python
client = PulsarClient(
    base_url="http://localhost:8182",
    default_headers={
        "Authorization": "Bearer your-token",
        "X-Custom-Header": "value"
    }
)
```

### Timeout

```python
client = PulsarClient(
    base_url="http://localhost:8182",
    timeout=60.0  # 60 seconds
)
```

## Notes

- **Event handlers**: The `PageEventHandlers` class is a placeholder. Full event support will be implemented in future releases.
- **Capture**: The `capture` method currently re-opens the current URL as there's no dedicated REST endpoint yet.
- **Local parsing**: Use BeautifulSoup or similar libraries for local HTML parsing with the `parse` result.

## API Compatibility

This SDK aims to maintain API consistency with the Kotlin Browser4 interfaces:
- `PulsarSession` → Python `PulsarSession`
- `AgenticSession` → Python `AgenticSession`
- `WebDriver` → Python `WebDriver`
- `PageEventHandlers` → Python `PageEventHandlers` (placeholder)

Method names use Python naming conventions (snake_case) with Kotlin-style aliases available for compatibility.

## Release Information

For maintainers and contributors working on SDK releases:

- **[Release Plan (中文)](docs-dev/RELEASE_PLAN.md)** - Complete release process documentation in Chinese
- **[Release Plan (English)](docs-dev/RELEASE_PLAN.en.md)** - Complete release process documentation in English
- **[Quick Reference](docs-dev/RELEASE_QUICKREF.md)** - Quick reference guide for releases (Chinese)

These documents cover:
- Version management and semantic versioning strategy
- Pre-release testing and validation procedures
- Building and packaging for PyPI
- Automated CI/CD release workflows
- Post-release monitoring and maintenance
