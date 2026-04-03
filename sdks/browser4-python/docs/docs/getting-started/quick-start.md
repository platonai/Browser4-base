# Quick Start

Get up and running with Browser4 Python SDK in minutes! This guide will walk you through creating your first browser automation script.

## Your First Script

Create a file called `first_script.py`:

```python
from browser4 import Browser4Driver, PulsarClient, AgenticSession

# Browser4Driver automatically downloads and starts the server
with Browser4Driver() as driver:
    print(f"✓ Server started at: {driver.base_url}")
    
    # Create client and session
    client = PulsarClient(base_url=driver.base_url)
    session_id = client.create_session()
    print(f"✓ Session created: {session_id}")
    
    session = AgenticSession(client)
    
    # Navigate to a page
    page = session.open("https://example.com")
    print(f"✓ Opened: {page.url}")
    print(f"  Content type: {page.content_type}")
    
    # Get page title
    title = session.driver.title()
    print(f"✓ Page title: {title}")
    
    # Extract data with CSS selectors
    document = session.parse(page)
    fields = session.extract(document, {
        "title": "h1",
        "description": "p"
    })
    print(f"✓ Extracted fields:")
    for key, value in fields.items():
        print(f"  {key}: {value[:80]}...")
    
    # Clean up
    session.close()
    client.close()
    print("✓ Session closed")

print("✓ All done!")
```

## Run Your Script

### Using uv
```bash
uv run python first_script.py
```

### Using Python directly
```bash
python first_script.py
```

## Expected Output

On first run (downloading Browser4.jar):
```
Downloading Browser4.jar...
Download complete: 150.2 MB
Starting Browser4 server...
✓ Server started at: http://localhost:8182
✓ Session created: 550e8400-e29b-41d4-a716-446655440000
✓ Opened: https://example.com
  Content type: text/html
✓ Page title: Example Domain
✓ Extracted fields:
  title: Example Domain...
  description: This domain is for use in illustrative examples in documents...
✓ Session closed
✓ All done!
```

On subsequent runs (server already downloaded):
```
✓ Server started at: http://localhost:8182
✓ Session created: 550e8400-e29b-41d4-a716-446655440000
✓ Opened: https://example.com
  Content type: text/html
✓ Page title: Example Domain
...
```

## Understanding the Code

Let's break down what each part does:

### 1. Import Required Classes

```python
from browser4 import Browser4Driver, PulsarClient, AgenticSession
```

- **Browser4Driver** - Manages the Browser4 server lifecycle
- **PulsarClient** - HTTP client for API communication
- **AgenticSession** - AI-powered session with full capabilities

### 2. Start the Server

```python
with Browser4Driver() as driver:
    # Server automatically starts and stops
```

Using a context manager ensures the server is properly started and stopped. Browser4Driver:
- Downloads Browser4.jar if not present (first run only)
- Starts the Java server process
- Waits for health check
- Automatically stops when done

### 3. Create a Session

```python
client = PulsarClient(base_url=driver.base_url)
session_id = client.create_session()
session = AgenticSession(client)
```

- **PulsarClient** handles HTTP communication
- **create_session()** establishes a new browser session
- **AgenticSession** provides the main API

### 4. Load and Parse a Page

```python
page = session.open("https://example.com")
document = session.parse(page)
```

- **open()** loads the page immediately
- **parse()** parses HTML for local extraction

### 5. Extract Data

```python
fields = session.extract(document, {
    "title": "h1",
    "description": "p"
})
```

Extract data using CSS selectors. Returns a dictionary with extracted values.

### 6. Clean Up

```python
session.close()
client.close()
```

Always close sessions and clients when done.

## Next Steps

### Try WebDriver Commands

Add browser interaction to your script:

```python
# Get current URL
url = session.driver.current_url()

# Click an element
session.driver.click("a.some-link")

# Fill a form
session.driver.fill("input[name='email']", "user@example.com")

# Scroll down
session.driver.scroll_down(3)

# Take a screenshot
screenshot = session.driver.capture_screenshot()
```

### Try AI-Powered Actions

Let AI handle complex tasks:

```python
# Single action
result = session.act("click the login button")
print(f"Action: {result.action}")
print(f"Success: {result.success}")

# Multi-step task
history = session.run("search for 'python' and click the first result")
print(f"Task completed: {history.success}")
```

### Extract Structured Data

Use selectors to extract specific fields:

```python
data = session.scrape(
    "https://news.ycombinator.com",
    "-expire 1h",
    {
        "stories": ".athing .titleline > a::text",
        "points": ".score::text",
        "authors": ".hnuser::text"
    }
)
```

## Common Patterns

### Connect to Existing Server

If you already have Browser4 running:

```python
from browser4 import PulsarClient, AgenticSession

client = PulsarClient(base_url="http://localhost:8182")
session_id = client.create_session()
session = AgenticSession(client)

# ... use the session

session.close()
client.close()
```

### Customize Server Port

```python
with Browser4Driver(port=8183) as driver:
    client = PulsarClient(base_url=driver.base_url)
    # ... rest of code
```

### Add Java Options

```python
driver = Browser4Driver(
    port=8182,
    java_options={
        "spring.profiles.active": "rest,private",
        "custom.setting": "value"
    }
)
```

## Troubleshooting

### Server Won't Start

Check Java version:
```bash
java -version  # Should be 17+
```

Check port availability:
```bash
# Linux/macOS
lsof -i :8182

# Windows
netstat -ano | findstr :8182
```

### Import Errors

Ensure Browser4 SDK is installed:
```bash
pip show browser4-sdk
# or
python -c "import browser4; print(browser4.__version__)"
```

### Timeout Errors

Increase timeout for slower connections:
```python
client = PulsarClient(base_url=driver.base_url, timeout=60.0)
```

## Learn More

- **[First Steps](first-steps.md)** - Essential concepts and patterns
- **[User Guide](../guide/session-management.md)** - In-depth feature guides
- **[Examples](../examples/basic-usage.md)** - More complete examples
- **[API Reference](../api/overview.md)** - Complete API documentation
