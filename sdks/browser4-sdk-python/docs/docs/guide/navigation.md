# Navigation

Navigate between pages, manage browser history, and control page loading in Browser4. This guide covers all navigation operations available through PulsarSession, AgenticSession, and WebDriver.

## Navigation Methods

Browser4 provides multiple ways to navigate:

### Open vs Navigate

**session.open()** - High-level page loading with caching:
```python
from browser4 import PulsarClient, PulsarSession

client = PulsarClient(base_url="http://localhost:8182")
session_id = client.create_session()
session = PulsarSession(client)

# Open bypasses cache, always fetches fresh
page = session.open("https://example.com")
print(f"Loaded: {page.url}")
```

**driver.navigate_to()** - Low-level browser navigation:
```python
from browser4 import PulsarClient, WebDriver

client = PulsarClient(base_url="http://localhost:8182")
session_id = client.create_session()
driver = WebDriver(client)

# Direct browser navigation
driver.navigate_to("https://example.com")
print(f"Current URL: {driver.current_url()}")
```

## Basic Navigation

### Navigate to URL

```python
from browser4 import PulsarClient, WebDriver

client = PulsarClient(base_url="http://localhost:8182")
session_id = client.create_session()
driver = WebDriver(client)

# Navigate to URL
driver.navigate_to("https://example.com")

# Or use open() alias
driver.open("https://example.com")

# Get current URL
url = driver.current_url()
print(f"Current: {url}")
```

### Get Page Information

```python
# Get current URL
current_url = driver.current_url()

# Get page title
title = driver.title()

# Get page source HTML
html = driver.page_source()

print(f"URL: {current_url}")
print(f"Title: {title}")
print(f"HTML length: {len(html)} characters")
```

## History Navigation

### Back and Forward

```python
driver = WebDriver(client)

# Navigate to multiple pages
driver.navigate_to("https://example.com")
driver.navigate_to("https://example.com/page1")
driver.navigate_to("https://example.com/page2")

# Go back
driver.go_back()
print(f"After back: {driver.current_url()}")

# Go back again
driver.go_back()
print(f"After second back: {driver.current_url()}")

# Go forward
driver.go_forward()
print(f"After forward: {driver.current_url()}")
```

### Navigation History Tracking

```python
driver = WebDriver(client)

# Navigate to several pages
driver.navigate_to("https://example.com")
driver.navigate_to("https://example.com/about")
driver.navigate_to("https://example.com/contact")

# Access navigation history
history = driver.navigate_history
print(f"Visited {len(history)} pages:")
for i, url in enumerate(history, 1):
    print(f"{i}. {url}")
```

## Reload and Refresh

### Reload Current Page

```python
driver = WebDriver(client)

# Navigate to a page
driver.navigate_to("https://example.com")

# Reload the page
driver.reload()
print("Page reloaded")

# Reload is equivalent to refresh
driver.refresh()
print("Page refreshed")
```

### Force Refresh with Session

```python
session = PulsarSession(client)

# Load with expiration (uses cache if fresh)
page = session.load("https://example.com", args="-expire 1d")

# Force fresh load (bypass cache)
page = session.open("https://example.com")
print(f"Fresh page loaded: {page.url}")
```

## Page Loading with Options

### Load Arguments

Control caching and parsing with load arguments:

```python
session = PulsarSession(client)

# Load with 1 day expiration
page = session.load("https://example.com", args="-expire 1d")

# Force refresh and parse
page = session.load("https://example.com", args="-expire 0s -parse")

# Load with multiple options
page = session.load(
    "https://example.com",
    args="-expire 1h -parse -outLink a[href]"
)
```

**Common load arguments:**
- `-expire <duration>` - Cache expiration (e.g., `1d`, `12h`, `30m`)
- `-refresh` - Force page refresh
- `-parse` - Activate parsing subsystem
- `-outLink <selector>` - Extract links matching selector
- `-requireSize <bytes>` - Minimum content size required

### Normalize URLs

Normalize URLs with load arguments:

```python
session = PulsarSession(client)

# Normalize URL with arguments
norm_url = session.normalize("https://example.com", args="-expire 1d")

print(f"Spec: {norm_url.spec}")
print(f"URL: {norm_url.url}")
print(f"Args: {norm_url.args}")
print(f"Valid: {not norm_url.is_nil}")
```

## Async Page Submission

### Submit for Background Loading

Submit URLs for asynchronous processing:

```python
session = PulsarSession(client)

# Submit single URL
session.submit("https://example.com/page1")

# Submit with load arguments
session.submit("https://example.com/page2", args="-expire 1h")

# Submit multiple URLs
urls = [
    "https://example.com/page1",
    "https://example.com/page2",
    "https://example.com/page3"
]

for url in urls:
    session.submit(url, args="-expire 1d")

print(f"Submitted {len(urls)} URLs for background processing")
```

## Complete Navigation Example

```python
from browser4 import Browser4Driver, PulsarClient, AgenticSession

def navigation_demo():
    """Comprehensive navigation demonstration."""
    
    with Browser4Driver() as driver_mgr:
        client = PulsarClient(base_url=driver_mgr.base_url)
        session_id = client.create_session()
        session = AgenticSession(client)
        
        try:
            # 1. Open initial page
            print("1. Opening example.com...")
            page = session.open("https://example.com")
            print(f"   Loaded: {page.url}")
            
            # 2. Navigate with WebDriver
            driver = session.driver
            print("\n2. Navigating to about page...")
            driver.navigate_to("https://example.com/about")
            print(f"   Current: {driver.current_url()}")
            
            # 3. Get page info
            print("\n3. Page information:")
            print(f"   Title: {driver.title()}")
            print(f"   URL: {driver.current_url()}")
            
            # 4. Navigate to another page
            print("\n4. Navigating to contact...")
            driver.navigate_to("https://example.com/contact")
            
            # 5. Use history navigation
            print("\n5. History navigation:")
            driver.go_back()
            print(f"   After back: {driver.current_url()}")
            driver.go_forward()
            print(f"   After forward: {driver.current_url()}")
            
            # 6. Reload page
            print("\n6. Reloading page...")
            driver.reload()
            print("   Page reloaded")
            
            # 7. View navigation history
            print("\n7. Navigation history:")
            history = driver.navigate_history
            for i, url in enumerate(history, 1):
                print(f"   {i}. {url}")
            
            # 8. Submit URLs for background processing
            print("\n8. Submitting URLs for background...")
            urls = [
                "https://example.com/page1",
                "https://example.com/page2"
            ]
            for url in urls:
                session.submit(url, args="-expire 1d")
            print(f"   Submitted {len(urls)} URLs")
            
        finally:
            session.close()
            client.close()

if __name__ == "__main__":
    navigation_demo()
```

## Navigation with AI

### AI-Powered Navigation

Use natural language for navigation:

```python
session = AgenticSession(client)

# Open starting page
session.open("https://example.com")

# AI-powered navigation actions
session.act("click the about link")
session.act("scroll to the bottom")
session.act("click the back button")

# Multi-step navigation task
result = session.run("""
    1. Click the products link
    2. Find the featured product
    3. Click to view details
""")

print(f"Navigation task: {result.success}")
```

## Navigation Patterns

### Sequential Navigation Pattern

Visit multiple pages in sequence:

```python
def visit_pages(session, urls):
    """Visit multiple pages sequentially."""
    pages = []
    
    for url in urls:
        print(f"Visiting: {url}")
        page = session.open(url)
        pages.append(page)
        
        # Optional: extract data from each page
        document = session.parse(page)
        title = session.extract(document, {"title": "h1"})
        print(f"  Title: {title.get('title')}")
    
    return pages

# Usage
urls = [
    "https://example.com/page1",
    "https://example.com/page2",
    "https://example.com/page3"
]
pages = visit_pages(session, urls)
```

### Conditional Navigation Pattern

Navigate based on page content:

```python
def navigate_conditionally(session):
    """Navigate based on page content."""
    driver = session.driver
    
    # Open starting page
    driver.navigate_to("https://example.com")
    
    # Check if login required
    if driver.exists("button.login"):
        print("Login required, navigating to login page")
        driver.click("button.login")
        driver.wait_for_selector("form#login", timeout=5000)
        print("On login page")
    else:
        print("Already logged in")
    
    # Navigate based on page state
    if driver.exists(".premium-content"):
        print("Premium content available")
    else:
        print("Standard content only")

navigate_conditionally(session)
```

### Navigation with Retry Pattern

Handle navigation failures gracefully:

```python
import time

def navigate_with_retry(driver, url, max_retries=3):
    """Navigate with retry on failure."""
    for attempt in range(max_retries):
        try:
            driver.navigate_to(url)
            
            # Verify navigation succeeded
            if driver.current_url() == url:
                print(f"Successfully navigated to {url}")
                return True
            
            print(f"URL mismatch, attempt {attempt + 1}")
            
        except Exception as e:
            print(f"Navigation failed (attempt {attempt + 1}): {e}")
            if attempt < max_retries - 1:
                time.sleep(2)  # Wait before retry
    
    print(f"Failed to navigate after {max_retries} attempts")
    return False

# Usage
success = navigate_with_retry(driver, "https://example.com")
```

## Troubleshooting

### Navigation Timeout

Increase timeout for slow pages:

```python
# Increase client timeout
client = PulsarClient(
    base_url="http://localhost:8182",
    timeout=120.0  # 2 minutes
)

# Or use load arguments
page = session.load("https://slow-site.com", args="-timeout 120s")
```

### Page Not Loading

Check page loading result:

```python
page = session.open("https://example.com")

# Check if page loaded successfully
if page.is_nil:
    print("Page failed to load")
else:
    print(f"Page loaded: {page.url}")
    print(f"Content type: {page.content_type}")
    print(f"Content length: {page.content_length}")
```

### Navigation History Issues

Clear history when needed:

```python
driver = WebDriver(client)

# Navigation history persists across operations
print(f"History size: {len(driver.navigate_history)}")

# To start fresh, create a new driver instance
old_driver.close()
driver = WebDriver(client)
```

### URL Encoding Issues

Handle special characters in URLs:

```python
from urllib.parse import quote

# Manually encode URL parameters
base_url = "https://example.com/search"
query = "python programming"
encoded_query = quote(query)
url = f"{base_url}?q={encoded_query}"

driver.navigate_to(url)
```

## Best Practices

1. **Use session.open() for fresh data** - Bypasses cache for current content
2. **Use session.load() for efficiency** - Leverages caching when appropriate
3. **Set appropriate expiration times** - Balance freshness vs performance
4. **Handle navigation failures** - Use try/except and verify results
5. **Monitor navigation history** - Track visited pages for debugging
6. **Clean up sessions** - Close sessions when done to free resources
7. **Use async submission for batch loading** - Submit multiple URLs for background processing

## Performance Tips

### Optimize Cache Usage

```python
# Good: Use cache for static content
page = session.load("https://example.com", args="-expire 1d")

# Good: Fresh data when needed
page = session.open("https://example.com/live-data")

# Avoid: Unnecessary refreshes
# page = session.open("https://example.com/static-page")  # No cache benefit
```

### Batch URL Submission

```python
# Efficient: Submit all URLs first
urls = ["https://example.com/page1", "https://example.com/page2"]
for url in urls:
    session.submit(url, args="-expire 1d")

# Then load them as needed
for url in urls:
    page = session.load(url, args="-expire 1d")  # Uses cache
```

### Minimize Page Reloads

```python
# Inefficient
driver.navigate_to("https://example.com")
data1 = driver.select_first_text_or_null("h1")
driver.reload()
data2 = driver.select_first_text_or_null("p")

# Better
driver.navigate_to("https://example.com")
data1 = driver.select_first_text_or_null("h1")
data2 = driver.select_first_text_or_null("p")
```

## Next Steps

- **[Element Interaction](element-interaction.md)** - Interact with page elements
- **[Data Extraction](data-extraction.md)** - Extract data from pages
- **[AI Automation](ai-automation.md)** - Use AI-powered navigation
- **[Session Management](session-management.md)** - Manage sessions effectively
