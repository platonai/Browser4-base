# First Steps

Now that you've run your first Browser4 script, let's dive deeper into essential concepts and patterns you'll use in every project.

## Core Components

Browser4 Python SDK consists of five main components:

### 1. Browser4Driver
Manages the Browser4 server lifecycle.

```python
from browser4 import Browser4Driver

# Automatic management (recommended)
with Browser4Driver() as driver:
    print(f"Server at: {driver.base_url}")
    # Server automatically stops when exiting context

# Manual management
driver = Browser4Driver(port=8182)
driver.start()
print(f"Healthy: {driver.is_server_healthy()}")
# ... use server
driver.stop()
```

### 2. PulsarClient
Low-level HTTP client for API communication.

```python
from browser4 import PulsarClient

client = PulsarClient(
    base_url="http://localhost:8182",
    timeout=30.0,
    default_headers={"Authorization": "Bearer token"}
)

# Create session
session_id = client.create_session()
print(f"Session: {session_id}")

# Delete session when done
client.delete_session()
```

### 3. PulsarSession
Basic session with page loading and extraction.

```python
from browser4 import PulsarClient, PulsarSession

client = PulsarClient()
session_id = client.create_session()
session = PulsarSession(client)

# Load pages
page = session.open("https://example.com")  # Immediate load
cached = session.load("https://example.com", args="-expire 1d")

# Submit for background processing
session.submit("https://example.com/page1")
session.submit("https://example.com/page2")

# Extract data
document = session.parse(page)
data = session.extract(document, {"title": "h1"})
```

### 4. AgenticSession
AI-powered session extending PulsarSession.

```python
from browser4 import PulsarClient, AgenticSession

client = PulsarClient()
session_id = client.create_session()
session = AgenticSession(client)

# All PulsarSession methods available
page = session.open("https://example.com")

# Plus AI capabilities
session.act("click the search button")
session.run("find and fill the contact form")
```

### 5. WebDriver
Browser control and element interaction.

```python
# Access through session
driver = session.driver

# Navigation
driver.navigate_to("https://example.com")
driver.reload()
driver.go_back()

# Element interaction
driver.click("button.submit")
driver.fill("input[name='email']", "user@example.com")
driver.scroll_down(3)

# Data extraction
text = driver.select_first_text_or_null("h1")
texts = driver.select_text_all("li.item")
```

## Loading Pages

Browser4 provides multiple ways to load pages depending on your needs.

### open() - Immediate Load
Fetches the page immediately, bypassing cache:

```python
page = session.open("https://example.com")
print(page.url)
print(page.content_type)
```

**Use when:** You need the latest version of the page.

### load() - Cache-Aware Load
Uses cache if available and not expired:

```python
# Load with expiration time
page = session.load("https://example.com", args="-expire 1d")

# Force refresh
page = session.load("https://example.com", args="-refresh")

# Enable parsing
page = session.load("https://example.com", args="-parse")
```

**Use when:** You want efficient caching behavior.

### submit() - Background Processing
Submits URLs for asynchronous processing:

```python
# Submit single URL
session.submit("https://example.com")

# Submit with arguments
session.submit("https://example.com", args="-expire 1h -parse")

# Submit multiple
urls = ["https://example.com/1", "https://example.com/2"]
session.submit_all(urls)
```

**Use when:** Processing many URLs or don't need immediate results.

## Load Arguments

Control page loading behavior with arguments:

| Argument | Description | Example |
|----------|-------------|---------|
| `-expire <time>` | Set expiration time | `-expire 1d`, `-expire 2h` |
| `-refresh` | Force page refresh | `-refresh` |
| `-parse` | Enable parsing subsystem | `-parse` |
| `-outLink <selector>` | Extract outlinks | `-outLink a[href]` |
| `-scroll <count>` | Scroll count | `-scroll 5` |

```python
# Multiple arguments
page = session.load(
    "https://example.com",
    args="-expire 1d -refresh -parse -scroll 3"
)
```

## Extracting Data

Browser4 provides multiple extraction methods.

### CSS Selectors (PulsarSession)

```python
page = session.open("https://example.com")
document = session.parse(page)

# Extract fields
fields = session.extract(document, {
    "title": "h1",
    "description": "p.description",
    "links": "a[href]"
})

print(fields["title"])  # Single value
print(fields["links"])   # List of values
```

### WebDriver Extraction

```python
driver = session.driver

# Single text
title = driver.select_first_text_or_null("h1")

# Multiple texts
items = driver.select_text_all("li.item")

# Attribute
href = driver.select_first_attribute_or_null("a", "href")
hrefs = driver.select_attribute_all("a", "href")

# Multiple fields at once
fields = driver.extract({
    "title": "h1",
    "price": ".price",
    "stock": ".stock-info"
})
```

### scrape() - One-Step Operation

```python
# Load and extract in one call
data = session.scrape(
    "https://example.com",
    "-expire 1d",
    {
        "title": "h1",
        "content": ".main-content",
        "author": ".author-name"
    }
)
```

## Working with WebDriver

The WebDriver provides precise browser control.

### Navigation

```python
driver = session.driver

# Go to URL
driver.navigate_to("https://example.com")

# Get current URL
url = driver.current_url()

# History
driver.go_back()
driver.go_forward()
driver.reload()
```

### Element Interaction

```python
# Click
driver.click("button.submit")

# Type text (character by character)
driver.type("input[name='search']", "browser automation")

# Fill instantly
driver.fill("input[name='email']", "user@example.com")

# Press key
driver.press("input[name='search']", "Enter")

# Hover
driver.hover(".dropdown-menu")

# Focus
driver.focus("input#username")
```

### Checkboxes and Forms

```python
# Check/uncheck
driver.check("input[type='checkbox']")
driver.uncheck("input[type='checkbox']")

# Multiple fields
driver.fill("input[name='username']", "john")
driver.fill("input[name='password']", "secret")
driver.click("button[type='submit']")
```

### Scrolling

```python
# Scroll down/up by pages
driver.scroll_down(3)  # 3 pages down
driver.scroll_up(2)    # 2 pages up

# Scroll to element
driver.scroll_to("h2.section-title")

# Scroll to positions
driver.scroll_to_top()
driver.scroll_to_bottom()
driver.scroll_to_middle(0.75)  # 75% down the page
```

### Waiting for Elements

```python
# Wait for selector
found = driver.wait_for_selector("h2.title", timeout=10000)

# Check existence
exists = driver.exists(".element")

# Check visibility
visible = driver.is_visible(".modal")
```

## Using AI Capabilities

AgenticSession provides AI-powered automation.

### Single Actions

```python
# Execute natural language action
result = session.act("click the login button")
print(f"Action: {result.action}")
print(f"Success: {result.success}")
print(f"Message: {result.message}")

# More examples
session.act("scroll to the bottom")
session.act("click the first search result")
session.act("fill in the email field with test@example.com")
```

### Multi-Step Tasks

```python
# Run autonomous task
history = session.run(
    "search for 'python programming' and click the first result"
)

print(f"Success: {history.success}")
print(f"Steps: {history.history_size}")
print(f"Final result: {history.final_result}")

# Check each step
for entry in history.history:
    print(f"- {entry}")
```

### Observing Page State

```python
# Get observations
result = session.observe("what actions can I take?")

for obs in result.observations:
    print(f"- {obs.description}")
    print(f"  Selector: {obs.selector}")
```

### AI Extraction

```python
# Structured extraction
extraction = session.agent_extract(
    instruction="Extract all product names and prices",
    schema={"type": "array", "items": {"type": "object"}}
)

print(extraction.data)
```

## Error Handling

Always handle errors gracefully:

```python
from browser4 import Browser4Driver, PulsarClient, AgenticSession

try:
    with Browser4Driver() as driver:
        client = PulsarClient(base_url=driver.base_url)
        
        try:
            session_id = client.create_session()
            session = AgenticSession(client)
            
            # Your automation code
            page = session.open("https://example.com")
            
        except Exception as e:
            print(f"Session error: {e}")
        finally:
            if 'session' in locals():
                session.close()
            client.close()
            
except Exception as e:
    print(f"Server error: {e}")
```

## Best Practices

### 1. Use Context Managers

```python
# Good - automatic cleanup
with Browser4Driver() as driver:
    # ... use driver

# Requires manual cleanup
driver = Browser4Driver()
driver.start()
# ... use driver
driver.stop()
```

### 2. Close Sessions

```python
# Always close when done
session = AgenticSession(client)
try:
    # ... use session
finally:
    session.close()
```

### 3. Reuse Clients

```python
# Create once, use multiple times
client = PulsarClient(base_url=driver.base_url)

session1 = AgenticSession(client)
# ... use session1
session1.close()

session2 = AgenticSession(client)
# ... use session2
session2.close()

client.close()
```

### 4. Use Appropriate Load Methods

```python
# Immediate, uncached
page = session.open(url)

# Cached with expiration
page = session.load(url, args="-expire 1d")

# Background processing
session.submit(url)
```

### 5. Set Reasonable Timeouts

```python
# For slow connections or complex pages
client = PulsarClient(base_url=driver.base_url, timeout=60.0)

# Wait for elements
driver.wait_for_selector(".slow-element", timeout=30000)
```

## Next Steps

Now that you understand the basics:

- **[Session Management](../guide/session-management.md)** - Advanced session patterns
- **[Data Extraction](../guide/data-extraction.md)** - Comprehensive extraction guide
- **[AI Automation](../guide/ai-automation.md)** - AI-powered workflows
- **[Examples](../examples/basic-usage.md)** - Complete working examples
