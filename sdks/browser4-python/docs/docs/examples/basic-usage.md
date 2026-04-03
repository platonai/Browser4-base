# Basic Usage

This guide covers the fundamental operations of the Browser4 Python SDK, including session creation, page loading, and simple data extraction.

## Quick Start with Browser4Driver

The easiest way to get started is using `Browser4Driver`, which automatically downloads and starts the Browser4 server:

```python
from browser4 import Browser4Driver, PulsarClient, AgenticSession

# Browser4Driver automatically downloads and starts the server
with Browser4Driver() as driver:
    # Create client and session
    client = PulsarClient(base_url=driver.base_url)
    session_id = client.create_session()
    session = AgenticSession(client)
    
    # Your automation code here
    page = session.open("https://example.com")
    print(f"Opened: {page.url}")
    
    # Clean up
    session.close()
    client.close()

# Server stops automatically when exiting the context
```

## Connecting to an Existing Server

If you already have a Browser4 server running:

```python
from browser4 import PulsarClient, AgenticSession

# Connect to existing server
client = PulsarClient(base_url="http://localhost:8182")
session_id = client.create_session()
session = AgenticSession(client)

# Your automation code here
page = session.open("https://example.com")

# Clean up
session.close()
client.close()
```

## Creating Sessions

### Using PulsarSession

`PulsarSession` provides basic page loading and data extraction:

```python
from browser4 import PulsarClient, PulsarSession

client = PulsarClient(base_url="http://localhost:8182")
client.create_session()
session = PulsarSession(client)

# Session is ready to use
print(f"Session ID: {session.uuid}")
print(f"Session active: {session.is_active}")
```

### Using AgenticSession

`AgenticSession` extends `PulsarSession` with AI-powered capabilities:

```python
from browser4 import PulsarClient, AgenticSession

client = PulsarClient(base_url="http://localhost:8182")
client.create_session()
session = AgenticSession(client)

# All PulsarSession methods are available
# Plus AI-powered methods: act(), run(), observe(), etc.
```

## Loading Pages

### Open a Page Immediately

Use `open()` to load a page directly, bypassing cache:

```python
# Open a page (fresh load)
page = session.open("https://example.com")

print(f"URL: {page.url}")
print(f"Location: {page.location}")
print(f"Content type: {page.content_type}")
print(f"Content length: {page.content_length}")
print(f"Status: {page.protocol_status}")
```

### Load with Caching

Use `load()` to load from cache if available, or fetch from internet:

```python
# Load with 1-day expiry
page = session.load("https://example.com", args="-expire 1d")

# Refresh and parse
page = session.load("https://example.com", args="-refresh -parse")
```

### Common Load Arguments

Load arguments control page loading behavior:

```python
# Expire after 1 day
page = session.load(url, args="-expire 1d")

# Force refresh (ignore cache)
page = session.load(url, args="-refresh")

# Enable parsing subsystem
page = session.load(url, args="-parse")

# Multiple arguments
page = session.load(url, args="-expire 1d -refresh -parse")
```

## Normalizing URLs

Normalize URLs with load arguments:

```python
# Normalize URL with arguments
norm = session.normalize("https://example.com", args="-expire 1d -refresh")

print(f"Full spec: {norm.spec}")
print(f"URL: {norm.url}")
print(f"Args: {norm.args}")
print(f"Valid: {not norm.is_nil}")
```

## Submitting URLs for Async Processing

Submit URLs to the crawl pool for background processing:

```python
# Submit single URL
session.submit("https://example.com/page1")

# Submit with arguments
session.submit("https://example.com/page2", args="-expire 1h")

# Submit multiple URLs
urls = [
    "https://example.com/page1",
    "https://example.com/page2",
    "https://example.com/page3",
]
for url in urls:
    session.submit(url, args="-expire 1d")
```

## Extracting Data with CSS Selectors

### Parse a Page

Parse the page HTML into a document object:

```python
page = session.open("https://example.com")
document = session.parse(page)

# Document is a BeautifulSoup object (if available)
if document:
    print(f"Title: {document.title.string}")
```

### Extract Specific Fields

Extract data using CSS selectors:

```python
page = session.open("https://example.com")
document = session.parse(page)

# Extract fields
fields = session.extract(document, {
    "title": "h1",
    "description": "p.description",
    "links": "a[href]",
    "prices": ".price"
})

print(f"Title: {fields.get('title')}")
print(f"Description: {fields.get('description')}")
print(f"Links: {fields.get('links')}")
print(f"Prices: {fields.get('prices')}")
```

### Scrape in One Operation

Combine load, parse, and extract in a single call:

```python
# Scrape with selectors
data = session.scrape(
    "https://example.com",
    args="-expire 1d",
    fields={
        "title": "h1",
        "description": ".content",
        "author": ".author-name",
        "date": "time[datetime]"
    }
)

print(f"Scraped data: {data}")
```

## Using WebDriver for Element Interaction

Access the WebDriver for low-level browser control:

```python
# Get the WebDriver instance
driver = session.driver

# Get current page info
url = driver.current_url()
title = driver.title()
html = driver.page_source()

print(f"Current URL: {url}")
print(f"Page title: {title}")
```

### Extract Text and Attributes

```python
# Extract first matching text
title = driver.select_first_text_or_null("h1")
print(f"Title: {title}")

# Extract all matching texts
items = driver.select_text_all("li.item")
for item in items:
    print(f"- {item}")

# Extract attribute from first match
href = driver.select_first_attribute_or_null("a.main-link", "href")
print(f"Main link: {href}")

# Extract attributes from all matches
all_hrefs = driver.select_attribute_all("a", "href")
for href in all_hrefs:
    print(f"Link: {href}")
```

### Extract Multiple Fields

```python
# Extract multiple fields at once
fields = driver.extract({
    "title": "h1.title",
    "price": ".price",
    "description": ".description",
    "image": "img.main-image@src",  # @ for attributes
    "rating": ".rating@data-score"
})

print(f"Title: {fields.get('title')}")
print(f"Price: {fields.get('price')}")
print(f"Image: {fields.get('image')}")
```

## Complete Basic Example

Here's a complete example combining multiple operations:

```python
from browser4 import Browser4Driver, PulsarClient, AgenticSession

def scrape_example():
    """Complete example: load page and extract data."""
    
    with Browser4Driver() as driver:
        # Create session
        client = PulsarClient(base_url=driver.base_url)
        session_id = client.create_session()
        session = AgenticSession(client)
        
        try:
            # Load page
            print("Loading page...")
            page = session.open("https://example.com")
            
            # Parse content
            document = session.parse(page)
            
            # Extract structured data
            if document:
                fields = session.extract(document, {
                    "title": "h1",
                    "paragraphs": "p",
                    "links": "a[href]"
                })
                
                print(f"\nExtracted Data:")
                print(f"Title: {fields.get('title')}")
                print(f"Paragraphs: {len(fields.get('paragraphs', []))}")
                print(f"Links: {len(fields.get('links', []))}")
            
            # Use WebDriver for additional extraction
            wd = session.driver
            title = wd.title()
            url = wd.current_url()
            
            print(f"\nWebDriver Info:")
            print(f"Page Title: {title}")
            print(f"Current URL: {url}")
            
        finally:
            # Clean up
            session.close()
            client.close()

if __name__ == "__main__":
    scrape_example()
```

## Error Handling

Always use try-finally blocks to ensure cleanup:

```python
from browser4 import Browser4Driver, PulsarClient, AgenticSession

driver = Browser4Driver()

try:
    driver.start()
    client = PulsarClient(base_url=driver.base_url)
    
    try:
        session_id = client.create_session()
        session = AgenticSession(client)
        
        # Your automation code
        page = session.open("https://example.com")
        
        session.close()
    finally:
        client.close()
        
finally:
    driver.stop()
```

Or use context managers for automatic cleanup:

```python
with Browser4Driver() as driver:
    client = PulsarClient(base_url=driver.base_url)
    session_id = client.create_session()
    session = AgenticSession(client)
    
    try:
        # Your automation code
        page = session.open("https://example.com")
    finally:
        session.close()
        client.close()
```

## Session Properties

Check session state:

```python
# Session ID
print(f"Session UUID: {session.uuid}")
print(f"Session ID: {session.id}")

# Session status
print(f"Is active: {session.is_active}")

# Display name
print(f"Display: {session.display}")
```

## Checking Page Status

Verify page load success:

```python
page = session.open("https://example.com")

# Check if page loaded successfully
if page.is_nil:
    print("Failed to load page")
else:
    print(f"Page loaded: {page.url}")
    print(f"Status: {page.protocol_status}")
    print(f"Content type: {page.content_type}")
```

## Next Steps

- [Advanced Usage](advanced-usage.md) - Complex extraction, error handling, multiple sessions
- [AI Automation](ai-automation.md) - AI-powered browser automation
- [Complete Workflow](complete-workflow.md) - End-to-end scraping pipelines
- [WebDriver API Reference](../api/webdriver.md) - Complete WebDriver documentation
- [AgenticSession API Reference](../api/agentic-session.md) - Full AI capabilities

## Troubleshooting

### Server Won't Start

If Browser4Driver fails to start:

```python
# Check if port is available
driver = Browser4Driver(port=8183)  # Try different port

# Check if Java is installed
import subprocess
result = subprocess.run(["java", "-version"], capture_output=True)
print(result.stderr.decode())
```

### Connection Refused

If you get connection errors:

```python
# Check server health
driver = Browser4Driver()
driver.start()

if driver.is_server_healthy():
    print("Server is healthy")
else:
    print("Server is not responding")
```

### Import Errors

Ensure Browser4 SDK is installed:

```bash
pip install browser4
# or
uv pip install browser4
```
