# WebDriver

::: browser4.webdriver.WebDriver

## Overview

`WebDriver` provides a WebDriver-compatible interface for browser control and element interaction. It maps to selector-first REST endpoints, offering a familiar API for browser automation.

## Key Features

- **Navigation**: Navigate to URLs, go back/forward, reload pages
- **Element Finding & Interaction**: Click, fill, type, press keys, hover
- **Scrolling**: Scroll up/down, to elements, to top/bottom
- **Content Extraction**: Get text, HTML, attributes from elements
- **Screenshots**: Capture full page or element screenshots
- **Script Execution**: Execute JavaScript synchronously or asynchronously
- **Waiting**: Wait for elements, navigation, custom conditions

## Constructor

```python
def __init__(self, client: PulsarClient)
```

**Example:**

```python
from browser4 import PulsarClient, WebDriver

client = PulsarClient()
client.create_session()
driver = WebDriver(client)
```

## Properties

- **`id`**: Driver ID (integer)
- **`navigate_history`**: List of visited URLs

## Navigation

### Core Methods

```python
driver.navigate_to(url: str) -> Any  # Navigate to URL
driver.open(url: str) -> None  # Convenience wrapper for navigate_to
driver.reload() -> Any  # Reload current page
driver.go_back() -> Any  # Navigate back
driver.go_forward() -> Any  # Navigate forward
```

### URL & Page Info

```python
driver.current_url() -> str  # Get current URL
driver.get_current_url() -> str  # Alias for current_url
driver.title() -> str  # Get page title
driver.page_source() -> Optional[str]  # Get HTML source
driver.document_uri() -> str  # Get document.documentURI
driver.base_uri() -> str  # Get document.baseURI
```

**Example:**

```python
driver.navigate_to("https://example.com")
print(f"Title: {driver.title()}")
print(f"URL: {driver.current_url()}")
```

## Element Finding

### Existence Checking

```python
driver.exists(selector: str, strategy: str = "css") -> bool
driver.is_visible(selector: str) -> bool
driver.is_hidden(selector: str) -> bool
driver.is_checked(selector: str) -> bool  # For checkboxes/radios
```

### Finding Elements

```python
# By selector
driver.find_element_by_selector(selector: str, strategy: str = "css") -> Dict
driver.find_elements_by_selector(selector: str, strategy: str = "css") -> List[Dict]

# WebDriver style
driver.find_element(using: str, value: str) -> Dict
driver.find_elements(using: str, value: str) -> List[Dict]
```

**Example:**

```python
if driver.exists("button.submit"):
    print("Submit button found")
    
element = driver.find_element_by_selector("button.submit")
elements = driver.find_elements_by_selector("a.product-link")
```

## Element Interaction

### Clicking & Hovering

```python
driver.click(selector: str, count: int = 1, strategy: str = "css") -> Any
driver.click_element(element_id: str) -> Any
driver.hover(selector: str) -> Any
driver.focus(selector: str) -> Any
```

### Text Input

```python
driver.fill(selector: str, text: str, strategy: str = "css") -> Any  # Clear then fill
driver.type(selector: str, text: str) -> Any  # Append text
driver.send_keys(selector: str, text: str, strategy: str = "css") -> Any  # Alias for fill
driver.press(selector: str, key: str, strategy: str = "css") -> Any  # Press key
```

### Checkbox/Radio

```python
driver.check(selector: str) -> Any  # Check checkbox
driver.uncheck(selector: str) -> Any  # Uncheck checkbox
```

**Example:**

```python
driver.fill("input[name='username']", "john_doe")
driver.fill("input[name='password']", "secret123")
driver.check("input#agree-terms")
driver.click("button[type='submit']")
```

## Waiting

```python
driver.wait_for_selector(selector: str, strategy: str = "css", timeout: int = 30000) -> bool
driver.wait_for(selector: str, ...) -> bool  # Alias
driver.wait_for_navigation(old_url: str = "", timeout: int = 30000) -> bool
```

**Example:**

```python
driver.click("a.next-page")
driver.wait_for_selector("div.content", timeout=10000)
```

## Scrolling

```python
driver.scroll_down(count: int = 1) -> float
driver.scroll_up(count: int = 1) -> float
driver.scroll_to(selector: str) -> float  # Scroll element into view
driver.scroll_to_top() -> float
driver.scroll_to_bottom() -> float
driver.scroll_to_middle(ratio: float = 0.5) -> float
driver.scroll_by(pixels: float = 200.0, smooth: bool = True) -> float
```

**Example:**

```python
driver.scroll_down(count=3)
driver.scroll_to("#contact-section")
driver.scroll_to_bottom()
```

## Content Extraction

### HTML & Text

```python
driver.outer_html(selector: Optional[str] = None, strategy: str = "css") -> Optional[str]
driver.text_content(selector: Optional[str] = None) -> Optional[str]
```

### Selecting Content

```python
# First match
driver.select_first_text_or_null(selector: str) -> Optional[str]
driver.select_first_attribute_or_null(selector: str, attr_name: str) -> Optional[str]

# All matches
driver.select_text_all(selector: str) -> List[str]
driver.select_attribute_all(selector: str, attr_name: str) -> List[str]
```

### Batch Extraction

```python
driver.extract(fields: Dict[str, str]) -> Dict[str, Optional[str]]
```

**Example:**

```python
# Extract single values
title = driver.select_first_text_or_null("h1")
href = driver.select_first_attribute_or_null("a.main", "href")

# Extract all
links = driver.select_text_all("a.product-name")
images = driver.select_attribute_all("img", "src")

# Extract multiple fields
data = driver.extract({
    "title": "h1.product-title",
    "price": "span.price",
    "description": "p.description"
})
```

### Element By ID

```python
driver.get_text(element_id: str) -> str
driver.get_attribute(element_id: str, name: str) -> Any
```

## Screenshots

```python
driver.capture_screenshot(selector: Optional[str] = None, full_page: bool = False) -> Optional[str]
driver.screenshot(selector: Optional[str] = None, strategy: str = "css") -> Optional[str]
```

**Returns:** Base64-encoded image

**Example:**

```python
# Viewport screenshot
viewport = driver.capture_screenshot()

# Full page
full = driver.capture_screenshot(full_page=True)

# Element
element = driver.capture_screenshot(selector="div.chart")
```

## Script Execution

```python
driver.evaluate(expression: str) -> Any  # Execute and return result
driver.execute_script(script: str, args: Optional[List[Any]] = None) -> Any
driver.execute_async_script(script: str, args: Optional[List[Any]] = None, timeout: int = 30000) -> Any
```

**Example:**

```python
# Evaluate expression
title = driver.evaluate("document.title")
width = driver.evaluate("window.innerWidth")

# Execute script
driver.execute_script("alert('Hello')")

# With return value
result = driver.execute_script("return document.querySelectorAll('a').length")

# With arguments
sum_result = driver.execute_script(
    "return arguments[0] + arguments[1]",
    args=[5, 10]
)
```

## Control

```python
driver.delay(millis: int) -> Any  # Delay execution
driver.pause() -> Any  # Pause session
driver.stop() -> Any  # Stop session
driver.bring_to_front() -> Any  # Bring window to front
driver.close() -> None  # Cleanup
```

**Example:**

```python
driver.click("button.submit")
driver.delay(2000)  # Wait 2 seconds
driver.click("button.next")
```

## Event Operations

```python
driver.create_event_config(config: Dict[str, Any]) -> Any
driver.list_event_configs() -> Any
driver.get_events() -> Any
driver.subscribe_events(subscribe_request: Dict[str, Any]) -> Any
```

## Complete Examples

### Form Automation

```python
from browser4 import PulsarClient, WebDriver

client = PulsarClient()
client.create_session()
driver = WebDriver(client)

# Navigate and fill form
driver.navigate_to("https://example.com/login")
driver.fill("input[name='username']", "user@example.com")
driver.fill("input[name='password']", "password123")
driver.check("input#remember-me")
driver.click("button[type='submit']")

# Wait for dashboard
driver.wait_for_selector("div.dashboard", timeout=5000)
print("Login successful")

driver.close()
```

### Data Scraping

```python
from browser4 import PulsarClient, WebDriver

client = PulsarClient()
client.create_session()
driver = WebDriver(client)

driver.navigate_to("https://example.com/products")

# Extract product data
products = driver.extract({
    "name": "h2.product-name",
    "price": "span.price",
    "rating": "div.rating",
    "stock": "span.stock-status"
})

# Get all product links
links = driver.select_attribute_all("a.product-link", "href")

print(f"Found {len(links)} products")
print(f"Sample: {products}")

driver.close()
```

### Advanced Interaction

```python
from browser4 import PulsarClient, WebDriver

client = PulsarClient()
client.create_session()
driver = WebDriver(client)

driver.navigate_to("https://example.com")

# Scroll to reveal content
driver.scroll_down(count=3)
driver.delay(1000)

# Hover to show menu
driver.hover("div.menu-trigger")
driver.delay(500)

# Click menu item
driver.click("a.menu-item")
driver.wait_for_navigation()

# Capture screenshot
screenshot = driver.capture_screenshot()

# Execute custom JavaScript
result = driver.execute_script("""
    return {
        title: document.title,
        links: document.querySelectorAll('a').length,
        images: document.querySelectorAll('img').length
    }
""")
print(result)

driver.close()
```

### Error Handling

```python
from browser4 import PulsarClient, WebDriver
import requests

client = PulsarClient()
client.create_session()
driver = WebDriver(client)

try:
    driver.navigate_to("https://example.com")
    
    # Wait for element with timeout
    if driver.wait_for_selector("div.content", timeout=5000):
        text = driver.select_first_text_or_null("div.content")
        print(f"Content: {text}")
    else:
        print("Content not found within timeout")
        
except requests.exceptions.HTTPError as e:
    print(f"HTTP Error: {e}")
except Exception as e:
    print(f"Error: {e}")
finally:
    driver.close()
```

## Selector Strategy

WebDriver supports two selector strategies:

- **`css`** (default): CSS selectors
- **`xpath`**: XPath expressions

**Example:**

```python
# CSS (default)
driver.click("button.submit")

# XPath
driver.click("//button[@id='submit']", strategy="xpath")

# In find methods
element = driver.find_element_by_selector("//h1", strategy="xpath")
```

## See Also

- [AgenticSession](agentic-session.md) - AI-powered automation
- [PulsarSession](pulsar-session.md) - Session with driver integration
- [PulsarClient](pulsar-client.md) - Underlying HTTP client
- [API Overview](overview.md) - Complete API reference
