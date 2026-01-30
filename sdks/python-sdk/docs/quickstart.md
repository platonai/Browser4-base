# Quick Start

Get up and running with the Browser4 Python SDK in just a few minutes!

## Your First Script

Let's create a simple script that opens a web page and extracts data:

```python
from pulsar_sdk import PulsarClient, AgenticSession

# 1. Create a client
client = PulsarClient(base_url="http://localhost:8182")

# 2. Create a session
session_id = client.create_session()
print(f"Created session: {session_id}")

# 3. Create an AgenticSession for full capabilities
session = AgenticSession(client)

# 4. Open a web page
page = session.open("https://example.com")
print(f"Opened: {page.url}")

# 5. Extract data using CSS selectors
data = session.extract(page, {
    "title": "h1",
    "paragraphs": "p"
})
print(f"Title: {data.get('title')}")
print(f"Paragraphs found: {len(data.get('paragraphs', []))}")

# 6. Clean up
session.close()
```

Save this as `hello_browser4.py` and run it:

```bash
python hello_browser4.py
```

## Basic Concepts

### 1. PulsarClient

The `PulsarClient` is the low-level HTTP client that communicates with the Browser4 server:

```python
from pulsar_sdk import PulsarClient

client = PulsarClient(
    base_url="http://localhost:8182",  # Server URL
    timeout=30.0,                       # Request timeout
    default_headers={}                  # Optional headers
)
```

### 2. Sessions

Create a session to start browser automation:

```python
# Create a session
session_id = client.create_session()

# Delete when done
client.delete_session()
```

### 3. AgenticSession

`AgenticSession` provides high-level methods for browser automation:

```python
from pulsar_sdk import AgenticSession

session = AgenticSession(client)

# Open a page
page = session.open("https://example.com")

# Extract data
data = session.extract(page, {"title": "h1"})

# Use AI-powered actions
result = session.act("click the search button")
```

## Common Patterns

### Pattern 1: Web Scraping

Extract structured data from web pages:

```python
from pulsar_sdk import PulsarClient, PulsarSession

client = PulsarClient()
client.create_session()
session = PulsarSession(client)

# Load page with caching
page = session.load("https://news.ycombinator.com", args="-expire 1h")

# Extract multiple fields
articles = session.extract(page, {
    "title": ".titleline > a",
    "score": ".score",
    "author": ".hnuser"
})

print(f"Found {len(articles.get('title', []))} articles")
session.close()
```

### Pattern 2: Browser Automation

Control the browser with WebDriver API:

```python
from pulsar_sdk import PulsarClient, WebDriver

client = PulsarClient()
client.create_session()
driver = WebDriver(client)

# Navigate
driver.navigate_to("https://example.com")

# Interact with elements
driver.fill("input[name='search']", "browser automation")
driver.press("input[name='search']", "Enter")

# Wait for results
driver.wait_for_selector(".results", timeout=5000)

# Extract text
results = driver.select_text_all(".result-title")
print(f"Found {len(results)} results")

client.delete_session()
```

### Pattern 3: AI-Powered Automation

Let AI handle complex tasks:

```python
from pulsar_sdk import PulsarClient, AgenticSession

client = PulsarClient()
client.create_session()
session = AgenticSession(client)

# Open a page
session.open("https://example.com")

# Execute a single action
result = session.act("scroll to the bottom of the page")
print(f"Action result: {result.message}")

# Run a multi-step task
history = session.run(
    "find the contact form, fill in name='John Doe' and email='john@example.com'"
)
print(f"Task completed: {history.success}")
print(f"Steps taken: {history.history_size}")

session.close()
```

### Pattern 4: Combined Approach

Mix different APIs for maximum flexibility:

```python
from pulsar_sdk import PulsarClient, AgenticSession

client = PulsarClient()
client.create_session()
session = AgenticSession(client)

# Open page
page = session.open("https://example.com")

# Use WebDriver for precise control
driver = session.driver
driver.click("button#show-more")
driver.wait_for_selector(".expanded-content", timeout=3000)

# Extract data with selectors
data = session.extract(page, {
    "content": ".expanded-content"
})

# Use AI when logic gets complex
session.act("if there's a popup, close it")

# Run a task
session.run("scroll through all sections and collect headers")

session.close()
```

## Working with Data

### Extracting Single Values

```python
# Extract single text value
title = driver.select_first_text_or_null("h1")
print(f"Page title: {title}")

# Extract single attribute
link = driver.select_first_attribute_or_null("a.main-link", "href")
print(f"Link URL: {link}")
```

### Extracting Multiple Values

```python
# Extract all matching texts
links = driver.select_text_all("a")
print(f"Found {len(links)} links")

# Extract all attributes
urls = driver.select_attribute_all("a", "href")
print(f"URLs: {urls}")
```

### Extracting Multiple Fields

```python
# Extract structured data
fields = session.extract(page, {
    "title": "h1",
    "author": ".author-name",
    "date": ".publish-date",
    "content": ".article-body"
})

print(f"Article: {fields['title']}")
print(f"By: {fields['author']}")
```

## Error Handling

Always handle errors gracefully:

```python
from pulsar_sdk import PulsarClient, AgenticSession

client = PulsarClient()

try:
    # Create session
    session_id = client.create_session()
    session = AgenticSession(client)
    
    # Your automation code here
    page = session.open("https://example.com")
    data = session.extract(page, {"title": "h1"})
    
    print(f"Success! Title: {data.get('title')}")
    
except Exception as e:
    print(f"Error occurred: {e}")
    
finally:
    # Always clean up
    try:
        session.close()
    except:
        pass
```

## Context Manager Support

Use context managers for automatic cleanup:

```python
from pulsar_sdk import PulsarClient, AgenticSession

client = PulsarClient()
client.create_session()

# Session will be closed automatically
session = AgenticSession(client)
try:
    page = session.open("https://example.com")
    print(f"Opened: {page.url}")
finally:
    session.close()
```

## Next Steps

Now that you've mastered the basics:

- **[Basic Usage Guide](guides/basic-usage.md)**: Learn more patterns and best practices
- **[AI Automation Guide](guides/agent-automation.md)**: Deep dive into AI-powered features
- **[API Reference](api-reference/client.md)**: Explore all available methods
- **[Examples](examples/simple-scraping.md)**: See complete working examples

## Tips for Success

1. **Always create sessions**: The Browser4 server requires a session for operations
2. **Clean up resources**: Call `session.close()` when done
3. **Handle errors**: Network and browser errors can occur, use try/except
4. **Start simple**: Begin with basic scraping before using AI features
5. **Check the logs**: The Browser4 server logs can help debug issues

## Common Mistakes to Avoid

❌ **Forgetting to create a session**:
```python
client = PulsarClient()
# Missing: client.create_session()
session = AgenticSession(client)  # Will fail!
```

✅ **Always create a session first**:
```python
client = PulsarClient()
client.create_session()
session = AgenticSession(client)  # Works!
```

❌ **Not cleaning up**:
```python
session = AgenticSession(client)
page = session.open("https://example.com")
# Missing: session.close()
```

✅ **Clean up resources**:
```python
try:
    session = AgenticSession(client)
    page = session.open("https://example.com")
finally:
    session.close()
```

---

Ready for more? Continue to the [Basic Usage Guide](guides/basic-usage.md) →
