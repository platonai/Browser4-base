# Basic Usage Guide

This guide covers the fundamental patterns for using the Browser4 Python SDK to scrape and interact with web pages. You'll learn how to set up sessions, load pages, extract data using CSS selectors, and manage your scraping tasks effectively.

## Table of Contents

- [Installation and Setup](#installation-and-setup)
- [Creating a Client and Session](#creating-a-client-and-session)
- [Opening vs Loading Pages](#opening-vs-loading-pages)
- [CSS Selectors for Data Extraction](#css-selectors-for-data-extraction)
- [Working with WebPage Objects](#working-with-webpage-objects)
- [Session Management Best Practices](#session-management-best-practices)
- [Common Scraping Patterns](#common-scraping-patterns)
- [Error Handling](#error-handling)

---

## Installation and Setup

First, install the Browser4 Python SDK:

```bash
# Install from source (editable mode for development)
pip install -e .[dev]

# Or install dependencies manually
pip install requests beautifulsoup4
```

Make sure the Browser4 server is running:

```bash
# Default port is 8182
# Check if server is running
curl http://localhost:8182/health
```

---

## Creating a Client and Session

The `PulsarClient` is the foundation for all API communication. A session provides the context for browser operations.

### Basic Client Setup

```python
from pulsar_sdk import PulsarClient

# Connect to local server
client = PulsarClient(base_url="http://localhost:8182")

# Connect to remote server with authentication
client = PulsarClient(
    base_url="https://browser4.example.com",
    timeout=60.0,  # Increase timeout for slow networks
    default_headers={
        "Authorization": "Bearer your-api-token",
        "X-Custom-Header": "custom-value"
    }
)
```

### Creating and Managing Sessions

```python
from pulsar_sdk import PulsarClient, PulsarSession

# Create client
client = PulsarClient()

# Create a new session
session_id = client.create_session()
print(f"Created session: {session_id}")

# Initialize PulsarSession wrapper
session = PulsarSession(client)

# Check session status
print(f"Session active: {session.is_active}")
print(f"Session UUID: {session.uuid}")
print(f"Session display: {session.display}")

# When done, close the session
session.close()
```

### Session with Custom Capabilities

```python
# Create session with browser capabilities
session_id = client.create_session(capabilities={
    "browserName": "chrome",
    "browserVersion": "latest",
    "platformName": "linux"
})

session = PulsarSession(client)
```

---

## Opening vs Loading Pages

Browser4 provides two main methods for fetching pages: `open()` and `load()`. Understanding the difference is crucial for effective scraping.

### open() - Always Fetch Fresh

The `open()` method **always fetches** the page from the internet, bypassing any local cache:

```python
# Always get the latest version
page = session.open("https://news.example.com")
print(f"Fetched: {page.url}")
print(f"Status: Fresh from internet")
```

**Use `open()` when:**
- You need the most current data
- The page changes frequently (e.g., news, stock prices)
- You're testing or debugging and want to ensure fresh results

### load() - Smart Caching

The `load()` method checks the local cache first and respects cache policies:

```python
# Load from cache if available and valid
page = session.load("https://static.example.com/about")
print(f"Loaded: {page.url}")
```

**Use `load()` when:**
- The page doesn't change often
- You want to reduce network traffic and server load
- You're batch processing many URLs

### Load Arguments

Both methods accept an `args` parameter for fine-grained control:

```python
# Force refresh even if cached
page = session.load("https://example.com", args="-refresh")

# Use cache for 1 day
page = session.load("https://example.com", args="-expire 1d")

# Use cache for 1 hour
page = session.load("https://example.com", args="-expire 1h")

# Multiple arguments
page = session.load(
    "https://example.com",
    args="-expire 1d -ignoreFailure"
)
```

**Common load arguments:**
- `-refresh`: Force fetch from internet
- `-expire <duration>`: Cache expiration (e.g., `1h`, `1d`, `7d`)
- `-ignoreFailure`: Don't fail on page errors
- `-requireSize <bytes>`: Minimum page size requirement

---

## CSS Selectors for Data Extraction

CSS selectors are the primary way to extract data from pages. The SDK provides multiple methods for different extraction needs.

### Basic Extraction with extract()

```python
# Load a page
page = session.load("https://example.com")

# Extract using session.extract()
# Need to use WebDriver for actual extraction
fields = session.extract(page, {
    "title": "h1",
    "subtitle": "h2.subtitle",
    "author": ".author-name",
    "date": "time.published",
    "body": "article .content"
})

print(f"Title: {fields['title']}")
print(f"Author: {fields['author']}")
```

### Using WebDriver for Direct Extraction

For more control, use the WebDriver directly:

```python
# Get the session's WebDriver
driver = session.driver

# Navigate to page
driver.navigate_to("https://example.com")

# Extract single element text
title = driver.select_first_text_or_null("h1")
print(f"Title: {title}")

# Extract multiple elements
links = driver.select_text_all("a.nav-link")
for link in links:
    print(f"- {link}")

# Extract attributes
first_link = driver.select_first_attribute_or_null("a", "href")
all_links = driver.select_attribute_all("a", "href")

print(f"First link: {first_link}")
print(f"All links: {all_links}")
```

### Multi-Field Extraction

Extract multiple fields at once:

```python
driver = session.driver
driver.navigate_to("https://blog.example.com/post/123")

# Extract structured data
article = driver.extract({
    "title": "h1.post-title",
    "author": ".author-name",
    "date": "time[datetime]",
    "category": ".category",
    "content": ".post-content",
    "tags": ".tag-list .tag",  # Gets first match
    "comment_count": ".comment-count"
})

print(f"Article: {article['title']}")
print(f"By: {article['author']} on {article['date']}")
print(f"Category: {article['category']}")
```

### Attribute Extraction

```python
# Get all image sources
images = driver.select_attribute_all("img", "src")
for img_url in images:
    print(f"Image: {img_url}")

# Get all link URLs
links = driver.select_attribute_all("a", "href")

# Get data attributes
prices = driver.select_attribute_all(".product", "data-price")
```

---

## Working with WebPage Objects

The `WebPage` class represents a loaded page and provides metadata about the fetch operation.

### WebPage Properties

```python
page = session.load("https://example.com")

# Basic properties
print(f"URL: {page.url}")
print(f"Location: {page.location}")  # Final URL after redirects
print(f"Content Type: {page.content_type}")

# Check page validity
if page.is_nil:
    print("Page failed to load or is invalid")
else:
    print("Page loaded successfully")

# Access HTML content
html = page.html
if html:
    print(f"Page size: {len(html)} characters")
```

### Checking Page Status

```python
def load_page_safely(session, url):
    """Load a page and check if it's valid."""
    page = session.load(url)
    
    if page.is_nil:
        print(f"Failed to load: {url}")
        return None
    
    if not page.html:
        print(f"No content: {url}")
        return None
    
    print(f"✓ Loaded: {page.url}")
    return page

# Use the safe loader
page = load_page_safely(session, "https://example.com")
if page:
    # Process the page
    fields = session.extract(page, {"title": "h1"})
```

---

## Session Management Best Practices

Proper session management ensures efficient resource usage and prevents memory leaks.

### Pattern 1: Single Session for Simple Tasks

```python
from pulsar_sdk import PulsarClient, PulsarSession

def scrape_simple():
    """Simple scraping with single session."""
    client = PulsarClient()
    
    try:
        # Create session
        client.create_session()
        session = PulsarSession(client)
        
        # Do your work
        page = session.load("https://example.com")
        driver = session.driver
        driver.navigate_to(page.url)
        title = driver.select_first_text_or_null("h1")
        
        print(f"Title: {title}")
        
    finally:
        # Always close session
        session.close()
        client.close()

scrape_simple()
```

### Pattern 2: Context Manager (Recommended)

```python
from contextlib import contextmanager

@contextmanager
def pulsar_session():
    """Context manager for automatic session cleanup."""
    client = PulsarClient()
    client.create_session()
    session = PulsarSession(client)
    
    try:
        yield session
    finally:
        session.close()
        client.close()

# Use it
with pulsar_session() as session:
    page = session.load("https://example.com")
    driver = session.driver
    driver.navigate_to(page.url)
    title = driver.select_first_text_or_null("h1")
    print(f"Title: {title}")
```

### Pattern 3: Long-Running Session

```python
class ScraperSession:
    """Reusable scraper session."""
    
    def __init__(self, base_url="http://localhost:8182"):
        self.client = PulsarClient(base_url=base_url)
        self.session = None
    
    def __enter__(self):
        self.client.create_session()
        self.session = PulsarSession(self.client)
        return self
    
    def __exit__(self, exc_type, exc_val, exc_tb):
        if self.session:
            self.session.close()
        self.client.close()
    
    def scrape_page(self, url):
        """Scrape a single page."""
        page = self.session.load(url)
        driver = self.session.driver
        driver.navigate_to(page.url)
        
        return driver.extract({
            "title": "h1",
            "content": ".main-content"
        })

# Use the reusable session
with ScraperSession() as scraper:
    result1 = scraper.scrape_page("https://example.com/page1")
    result2 = scraper.scrape_page("https://example.com/page2")
    result3 = scraper.scrape_page("https://example.com/page3")
    
    print(f"Scraped 3 pages with one session")
```

---

## Common Scraping Patterns

Here are proven patterns for common scraping scenarios.

### Pattern 1: Simple Single-Page Scrape

```python
from pulsar_sdk import PulsarClient, PulsarSession

def scrape_article(url):
    """Scrape a single article."""
    client = PulsarClient()
    client.create_session()
    session = PulsarSession(client)
    
    try:
        # Load and extract
        page = session.load(url, args="-expire 1d")
        driver = session.driver
        driver.navigate_to(page.url)
        
        article = driver.extract({
            "title": "h1",
            "author": ".author",
            "date": "time",
            "content": ".article-body"
        })
        
        return article
        
    finally:
        session.close()
        client.close()

# Use it
article = scrape_article("https://blog.example.com/post/123")
print(f"Title: {article['title']}")
```

### Pattern 2: Scrape Multiple URLs

```python
def scrape_multiple_urls(urls):
    """Scrape multiple URLs efficiently."""
    client = PulsarClient()
    client.create_session()
    session = PulsarSession(client)
    results = []
    
    try:
        driver = session.driver
        
        for url in urls:
            try:
                # Load page
                page = session.load(url, args="-expire 1d")
                driver.navigate_to(page.url)
                
                # Extract data
                data = driver.extract({
                    "title": "h1",
                    "description": ".description"
                })
                
                data["url"] = url
                results.append(data)
                
                print(f"✓ Scraped: {url}")
                
            except Exception as e:
                print(f"✗ Failed: {url} - {e}")
                results.append({"url": url, "error": str(e)})
        
        return results
        
    finally:
        session.close()
        client.close()

# Scrape a list of URLs
urls = [
    "https://example.com/page1",
    "https://example.com/page2",
    "https://example.com/page3"
]

results = scrape_multiple_urls(urls)
print(f"Scraped {len(results)} pages")
```

### Pattern 3: Extract List Items

```python
def scrape_product_list(url):
    """Scrape a list of products."""
    client = PulsarClient()
    client.create_session()
    session = PulsarSession(client)
    
    try:
        driver = session.driver
        driver.navigate_to(url)
        
        # Get all product titles
        titles = driver.select_text_all(".product-card h3")
        
        # Get all prices
        prices = driver.select_text_all(".product-card .price")
        
        # Get all URLs
        urls = driver.select_attribute_all(".product-card a", "href")
        
        # Combine into product list
        products = []
        for i in range(len(titles)):
            products.append({
                "title": titles[i] if i < len(titles) else None,
                "price": prices[i] if i < len(prices) else None,
                "url": urls[i] if i < len(urls) else None
            })
        
        return products
        
    finally:
        session.close()
        client.close()

# Scrape products
products = scrape_product_list("https://shop.example.com/products")
for product in products:
    print(f"- {product['title']}: {product['price']}")
```

### Pattern 4: Navigate and Extract

```python
def scrape_with_navigation(start_url):
    """Navigate through pages and extract data."""
    client = PulsarClient()
    client.create_session()
    session = PulsarSession(client)
    
    try:
        driver = session.driver
        driver.navigate_to(start_url)
        
        # Extract from first page
        data = {
            "page1": driver.select_first_text_or_null("h1")
        }
        
        # Click to next page
        driver.click("a.next-page")
        driver.delay(2000)  # Wait for page load
        
        # Extract from second page
        data["page2"] = driver.select_first_text_or_null("h1")
        
        # Go back
        driver.go_back()
        driver.delay(1000)
        
        return data
        
    finally:
        session.close()
        client.close()

# Use navigation
result = scrape_with_navigation("https://example.com")
print(result)
```

---

## Error Handling

Robust error handling ensures your scraping scripts can handle failures gracefully.

### Basic Try-Except Pattern

```python
from pulsar_sdk import PulsarClient, PulsarSession

def scrape_with_error_handling(url):
    """Scrape with comprehensive error handling."""
    client = None
    session = None
    
    try:
        # Setup
        client = PulsarClient()
        client.create_session()
        session = PulsarSession(client)
        
        # Load page
        try:
            page = session.load(url, args="-expire 1d")
            
            if page.is_nil:
                print(f"Page is invalid: {url}")
                return None
            
        except Exception as e:
            print(f"Failed to load page: {e}")
            return None
        
        # Extract data
        try:
            driver = session.driver
            driver.navigate_to(page.url)
            
            data = driver.extract({
                "title": "h1",
                "content": ".content"
            })
            
            return data
            
        except Exception as e:
            print(f"Failed to extract data: {e}")
            return None
    
    except Exception as e:
        print(f"Unexpected error: {e}")
        return None
    
    finally:
        # Cleanup
        if session:
            try:
                session.close()
            except:
                pass
        if client:
            try:
                client.close()
            except:
                pass

# Use it
result = scrape_with_error_handling("https://example.com")
if result:
    print(f"Success: {result}")
```

### Retry Logic

```python
import time
from typing import Optional

def scrape_with_retry(url, max_retries=3, delay=2):
    """Scrape with automatic retries."""
    
    for attempt in range(max_retries):
        try:
            client = PulsarClient()
            client.create_session()
            session = PulsarSession(client)
            
            try:
                page = session.load(url, args="-expire 1d")
                driver = session.driver
                driver.navigate_to(page.url)
                
                data = driver.extract({
                    "title": "h1",
                    "content": ".content"
                })
                
                # Success!
                return data
                
            finally:
                session.close()
                client.close()
                
        except Exception as e:
            print(f"Attempt {attempt + 1} failed: {e}")
            
            if attempt < max_retries - 1:
                print(f"Retrying in {delay} seconds...")
                time.sleep(delay)
            else:
                print(f"All {max_retries} attempts failed")
                return None

# Use retry logic
result = scrape_with_retry("https://example.com", max_retries=3)
```

### Custom Exception Handling

```python
class ScrapingError(Exception):
    """Base exception for scraping errors."""
    pass

class PageLoadError(ScrapingError):
    """Failed to load page."""
    pass

class ExtractionError(ScrapingError):
    """Failed to extract data."""
    pass

def scrape_safe(url):
    """Scrape with custom exceptions."""
    client = PulsarClient()
    
    try:
        client.create_session()
        session = PulsarSession(client)
        
        # Load page
        page = session.load(url)
        if page.is_nil:
            raise PageLoadError(f"Page failed to load: {url}")
        
        # Extract
        driver = session.driver
        driver.navigate_to(page.url)
        
        title = driver.select_first_text_or_null("h1")
        if not title:
            raise ExtractionError("Could not find title element")
        
        return {"title": title, "url": url}
        
    except PageLoadError as e:
        print(f"Page load error: {e}")
        return None
        
    except ExtractionError as e:
        print(f"Extraction error: {e}")
        return None
        
    except Exception as e:
        print(f"Unexpected error: {e}")
        return None
        
    finally:
        session.close()
        client.close()

# Use it
result = scrape_safe("https://example.com")
```

---

## Next Steps

Now that you understand the basics, explore these advanced topics:

- **[Agent Automation Guide](agent-automation.md)** - Learn how to use AI-powered browser automation
- **[Data Extraction Guide](data-extraction.md)** - Advanced scraping techniques and patterns
- **[Advanced Guide](advanced.md)** - Complex workflows and production deployment

## Summary

Key takeaways:

1. **Client and Session**: Always create a client and session before scraping
2. **open() vs load()**: Use `open()` for fresh data, `load()` for cached data
3. **CSS Selectors**: Primary method for data extraction
4. **WebDriver**: Provides direct access to browser control and extraction
5. **Error Handling**: Always use try-finally to ensure cleanup
6. **Context Managers**: Recommended pattern for automatic resource management

Happy scraping! 🚀
