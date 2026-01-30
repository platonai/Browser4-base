# WebDriver API Reference

The `WebDriver` class provides browser automation capabilities through a WebDriver-compatible interface. It communicates with the Browser4 server via REST API for remote browser control.

## Class: WebDriver

```python
from pulsar_sdk import WebDriver
```

### Constructor

```python
WebDriver(client: PulsarClient)
```

**Parameters:**

- `client` (PulsarClient): An initialized PulsarClient instance with an active session

**Example:**

```python
from pulsar_sdk import PulsarClient, WebDriver

client = PulsarClient()
client.create_session()
driver = WebDriver(client)
```

## Navigation

### navigate_to()

Navigate to a URL.

```python
navigate_to(url: str) -> Any
```

**Parameters:**

- `url` (str): The URL to navigate to

**Returns:**

- Navigation result

**Example:**

```python
driver.navigate_to("https://example.com")
driver.navigate_to("https://github.com")
```

**Alternative Name:**

`open()` - Opens URL and waits for navigation to complete

### current_url()

Get the current URL displayed in the address bar.

```python
current_url() -> str
```

**Returns:**

- `str`: The current URL

**Example:**

```python
url = driver.current_url()
print(f"Current URL: {url}")
```

**Alternative Names:**

- `get_current_url()` - Alias for Kotlin compatibility
- `url()` - Get document.URL property

### title()

Get the current page title.

```python
title() -> str
```

**Returns:**

- `str`: The page title

**Example:**

```python
title = driver.title()
print(f"Page title: {title}")
```

### page_source()

Get the HTML source code of the current page.

```python
page_source() -> Optional[str]
```

**Returns:**

- `Optional[str]`: The page HTML source or None

**Example:**

```python
html = driver.page_source()
if html:
    print(f"Page size: {len(html)} characters")
```

### go_back()

Navigate back in browser history.

```python
go_back() -> Any
```

**Example:**

```python
driver.navigate_to("https://example.com/page1")
driver.navigate_to("https://example.com/page2")
driver.go_back()  # Returns to page1
```

### go_forward()

Navigate forward in browser history.

```python
go_forward() -> Any
```

**Example:**

```python
driver.go_back()
driver.go_forward()  # Returns to page2
```

### reload()

Reload the current page.

```python
reload() -> Any
```

**Example:**

```python
driver.reload()
```

### document_uri()

Get the document.documentURI property.

```python
document_uri() -> str
```

**Returns:**

- `str`: The document URI

**Alternative Name:**

`get_document_uri()` - Alias for snake_case consistency

### base_uri()

Get the document.baseURI property.

```python
base_uri() -> str
```

**Returns:**

- `str`: The base URI

**Alternative Name:**

`get_base_uri()` - Alias for snake_case consistency

## Element Interaction

### click()

Click an element identified by CSS selector.

```python
click(selector: str, count: int = 1, strategy: str = "css") -> Any
```

**Parameters:**

- `selector` (str): CSS selector or XPath expression
- `count` (int): Number of clicks (for double-click, use 2). Default: `1`
- `strategy` (str): Selector strategy ("css" or "xpath"). Default: `"css"`

**Returns:**

- Click result

**Example:**

```python
# Single click
driver.click("button.submit")

# Double click
driver.click("div.item", count=2)

# Using XPath
driver.click("//button[@id='submit']", strategy="xpath")
```

**Alternative Name:**

`click_element(element_id: str)` - Click an element by its WebDriver ID

### fill()

Fill an input element with text (clearing existing content first).

```python
fill(selector: str, text: str, strategy: str = "css") -> Any
```

**Parameters:**

- `selector` (str): CSS selector or XPath expression
- `text` (str): Text to fill
- `strategy` (str): Selector strategy. Default: `"css"`

**Returns:**

- Fill result

**Example:**

```python
# Fill text input
driver.fill("input[name='email']", "user@example.com")

# Fill textarea
driver.fill("textarea#message", "Hello, World!")

# Fill with XPath
driver.fill("//input[@id='username']", "john_doe", strategy="xpath")
```

### type()

Type text into an element (appending to existing content).

```python
type(selector: str, text: str) -> Any
```

**Parameters:**

- `selector` (str): CSS selector
- `text` (str): Text to type

**Returns:**

- Type result

**Example:**

```python
# Type additional text
driver.type("input.search", "python tutorial")
```

### press()

Press a key on an element.

```python
press(selector: str, key: str, strategy: str = "css") -> Any
```

**Parameters:**

- `selector` (str): CSS selector or XPath expression
- `key` (str): Key to press (e.g., "Enter", "Tab", "Escape", "ArrowDown")
- `strategy` (str): Selector strategy. Default: `"css"`

**Returns:**

- Press result

**Example:**

```python
# Press Enter in search box
driver.fill("input.search", "python")
driver.press("input.search", "Enter")

# Press Tab to move focus
driver.press("input.first-name", "Tab")

# Press Escape
driver.press("body", "Escape")
```

**Alternative Name:**

`send_keys(element_id: str, text: str)` - Send keys to an element by ID

### hover()

Hover over an element.

```python
hover(selector: str) -> Any
```

**Parameters:**

- `selector` (str): CSS selector

**Returns:**

- Hover result

**Example:**

```python
# Hover to reveal dropdown
driver.hover("button.menu")

# Hover over image to see tooltip
driver.hover("img.product-image")
```

### focus()

Focus on an element.

```python
focus(selector: str) -> Any
```

**Parameters:**

- `selector` (str): CSS selector

**Returns:**

- Focus result

**Example:**

```python
# Focus on input field
driver.focus("input#email")
```

### check()

Check a checkbox element.

```python
check(selector: str) -> Any
```

**Parameters:**

- `selector` (str): CSS selector

**Returns:**

- Check result

**Example:**

```python
# Check checkbox
driver.check("input[type='checkbox']#terms")

# Check if not already checked
driver.check("input#newsletter")
```

### uncheck()

Uncheck a checkbox element.

```python
uncheck(selector: str) -> Any
```

**Parameters:**

- `selector` (str): CSS selector

**Returns:**

- Uncheck result

**Example:**

```python
# Uncheck checkbox
driver.uncheck("input#notifications")
```

## Scrolling

### scroll_down()

Scroll down the page.

```python
scroll_down(count: int = 1) -> float
```

**Parameters:**

- `count` (int): Number of scroll actions. Default: `1`

**Returns:**

- `float`: Current scroll position

**Example:**

```python
# Scroll down once (200px)
position = driver.scroll_down()
print(f"Scrolled to: {position}px")

# Scroll down multiple times
position = driver.scroll_down(count=3)  # 600px
```

### scroll_up()

Scroll up the page.

```python
scroll_up(count: int = 1) -> float
```

**Parameters:**

- `count` (int): Number of scroll actions. Default: `1`

**Returns:**

- `float`: Current scroll position

**Example:**

```python
# Scroll up once
position = driver.scroll_up()

# Scroll up multiple times
position = driver.scroll_up(count=2)
```

### scroll_to()

Scroll an element into view.

```python
scroll_to(selector: str) -> float
```

**Parameters:**

- `selector` (str): CSS selector of the element

**Returns:**

- `float`: Current scroll position

**Example:**

```python
# Scroll to specific element
driver.scroll_to("div#content")

# Scroll to footer
driver.scroll_to("footer")
```

### scroll_to_top()

Scroll to the top of the page.

```python
scroll_to_top() -> float
```

**Returns:**

- `float`: Current scroll position (0)

**Example:**

```python
driver.scroll_to_top()
```

### scroll_to_bottom()

Scroll to the bottom of the page.

```python
scroll_to_bottom() -> float
```

**Returns:**

- `float`: Current scroll position

**Example:**

```python
# Scroll to bottom to load lazy content
driver.scroll_to_bottom()
```

### scroll_to_middle()

Scroll to a specific position on the page.

```python
scroll_to_middle(ratio: float = 0.5) -> float
```

**Parameters:**

- `ratio` (float): Scroll ratio (0.0 = top, 1.0 = bottom). Default: `0.5`

**Returns:**

- `float`: Current scroll position

**Example:**

```python
# Scroll to middle
driver.scroll_to_middle()

# Scroll to 25% of page
driver.scroll_to_middle(ratio=0.25)

# Scroll to 75% of page
driver.scroll_to_middle(ratio=0.75)
```

### scroll_by()

Scroll by a specific number of pixels.

```python
scroll_by(pixels: float = 200.0, smooth: bool = True) -> float
```

**Parameters:**

- `pixels` (float): Pixels to scroll (positive = down, negative = up). Default: `200.0`
- `smooth` (bool): Whether to use smooth scrolling. Default: `True`

**Returns:**

- `float`: Current scroll position

**Example:**

```python
# Scroll down 500px smoothly
driver.scroll_by(500)

# Scroll up 300px without smooth scrolling
driver.scroll_by(-300, smooth=False)

# Small scroll
driver.scroll_by(100)
```

## Element Selection and Extraction

### select_first_text_or_null()

Get the text content of the first element matching the selector.

```python
select_first_text_or_null(selector: str) -> Optional[str]
```

**Parameters:**

- `selector` (str): CSS selector

**Returns:**

- `Optional[str]`: Text content or None if not found

**Example:**

```python
# Get heading text
title = driver.select_first_text_or_null("h1")
print(f"Title: {title}")

# Get price
price = driver.select_first_text_or_null(".price")
if price:
    print(f"Price: {price}")
```

### select_text_all()

Get text content of all elements matching the selector.

```python
select_text_all(selector: str) -> List[str]
```

**Parameters:**

- `selector` (str): CSS selector

**Returns:**

- `List[str]`: List of text contents

**Example:**

```python
# Get all article titles
titles = driver.select_text_all("h2.article-title")
for title in titles:
    print(f"- {title}")

# Get all prices
prices = driver.select_text_all(".product .price")
print(f"Found {len(prices)} prices")
```

### select_first_attribute_or_null()

Get an attribute value of the first element matching the selector.

```python
select_first_attribute_or_null(selector: str, attr_name: str) -> Optional[str]
```

**Parameters:**

- `selector` (str): CSS selector
- `attr_name` (str): Attribute name

**Returns:**

- `Optional[str]`: Attribute value or None

**Example:**

```python
# Get link href
url = driver.select_first_attribute_or_null("a.download", "href")
print(f"Download URL: {url}")

# Get image src
img_url = driver.select_first_attribute_or_null("img.logo", "src")

# Get data attribute
product_id = driver.select_first_attribute_or_null("div.product", "data-id")
```

### select_attribute_all()

Get attribute values of all elements matching the selector.

```python
select_attribute_all(selector: str, attr_name: str) -> List[str]
```

**Parameters:**

- `selector` (str): CSS selector
- `attr_name` (str): Attribute name

**Returns:**

- `List[str]`: List of attribute values

**Example:**

```python
# Get all image URLs
image_urls = driver.select_attribute_all("img", "src")
print(f"Found {len(image_urls)} images")

# Get all links
links = driver.select_attribute_all("a", "href")
for link in links:
    print(f"Link: {link}")

# Get data attributes
product_ids = driver.select_attribute_all(".product", "data-id")
```

### extract()

Extract multiple fields using CSS selectors.

```python
extract(fields: Dict[str, str]) -> Dict[str, Optional[str]]
```

**Parameters:**

- `fields` (Dict[str, str]): Dictionary mapping field names to CSS selectors

**Returns:**

- `Dict[str, Optional[str]]`: Dictionary mapping field names to extracted values

**Example:**

```python
# Extract product details
data = driver.extract({
    "name": "h1.product-name",
    "price": ".price",
    "description": ".description",
    "rating": ".rating"
})

print(f"Product: {data['name']}")
print(f"Price: {data['price']}")
print(f"Rating: {data['rating']}")
```

### outer_html()

Get the outer HTML of an element or the entire document.

```python
outer_html(selector: Optional[str] = None, strategy: str = "css") -> Optional[str]
```

**Parameters:**

- `selector` (Optional[str]): CSS selector (optional, if None returns document HTML). Default: `None`
- `strategy` (str): Selector strategy. Default: `"css"`

**Returns:**

- `Optional[str]`: HTML content or None

**Example:**

```python
# Get entire document HTML
full_html = driver.outer_html()

# Get specific element HTML
element_html = driver.outer_html("div.content")
```

### text_content()

Get the text content of an element or document.

```python
text_content(selector: Optional[str] = None) -> Optional[str]
```

**Parameters:**

- `selector` (Optional[str]): CSS selector (optional). Default: `None`

**Returns:**

- `Optional[str]`: Text content or None

**Example:**

```python
# Get all page text
page_text = driver.text_content()

# Get specific element text
content = driver.text_content("article.main")
```

## Element Finding

### exists()

Check if an element exists in the DOM.

```python
exists(selector: str, strategy: str = "css") -> bool
```

**Parameters:**

- `selector` (str): CSS selector or XPath expression
- `strategy` (str): Selector strategy ("css" or "xpath"). Default: `"css"`

**Returns:**

- `bool`: True if the element exists, False otherwise

**Example:**

```python
# Check if element exists
if driver.exists("button.submit"):
    print("Submit button found")

# Check with XPath
if driver.exists("//button[@id='submit']", strategy="xpath"):
    print("Button exists")
```

### is_visible()

Check if an element is visible.

```python
is_visible(selector: str) -> bool
```

**Parameters:**

- `selector` (str): CSS selector

**Returns:**

- `bool`: True if the element is visible

**Example:**

```python
# Check visibility
if driver.is_visible("div.modal"):
    print("Modal is visible")

# Check before interacting
if driver.is_visible("button.submit"):
    driver.click("button.submit")
```

### is_hidden()

Check if an element is hidden.

```python
is_hidden(selector: str) -> bool
```

**Parameters:**

- `selector` (str): CSS selector

**Returns:**

- `bool`: True if the element is hidden

**Example:**

```python
if driver.is_hidden("div.error-message"):
    print("No errors")
```

### is_checked()

Check if a checkbox/radio element is checked.

```python
is_checked(selector: str) -> bool
```

**Parameters:**

- `selector` (str): CSS selector

**Returns:**

- `bool`: True if the element is checked

**Example:**

```python
# Check checkbox state
if driver.is_checked("input#remember-me"):
    print("Remember me is checked")

# Toggle based on state
if not driver.is_checked("input#newsletter"):
    driver.check("input#newsletter")
```

### wait_for_selector()

Wait for an element to appear in the DOM.

```python
wait_for_selector(selector: str, strategy: str = "css", timeout: int = 30000) -> bool
```

**Parameters:**

- `selector` (str): CSS selector or XPath expression
- `strategy` (str): Selector strategy ("css" or "xpath"). Default: `"css"`
- `timeout` (int): Maximum wait time in milliseconds. Default: `30000`

**Returns:**

- `bool`: True if the element was found before timeout

**Example:**

```python
# Wait for element to appear
if driver.wait_for_selector("div.results", timeout=10000):
    print("Results loaded")

# Wait for dynamic content
driver.wait_for_selector("div.lazy-loaded")
titles = driver.select_text_all("h2")

# Wait with XPath
driver.wait_for_selector("//div[@class='content']", strategy="xpath")
```

**Alternative Names:**

- `wait_for()` - Alias for wait_for_selector()

### wait_for_navigation()

Wait for navigation to complete (URL change).

```python
wait_for_navigation(old_url: str = "", timeout: int = 30000) -> bool
```

**Parameters:**

- `old_url` (str): The previous URL to compare against. Default: `""`
- `timeout` (int): Maximum wait time in milliseconds. Default: `30000`

**Returns:**

- `bool`: True if navigation completed

**Example:**

```python
old_url = driver.current_url()
driver.click("a.next-page")
driver.wait_for_navigation(old_url)
```

### find_element()

Find element using WebDriver locator strategy.

```python
find_element(using: str, value: str) -> Dict[str, Any]
```

**Parameters:**

- `using` (str): Locator strategy (e.g., "css selector", "xpath")
- `value` (str): Locator value

**Returns:**

- `Dict[str, Any]`: Element reference dictionary

**Example:**

```python
element = driver.find_element("css selector", "button.submit")
```

### find_elements()

Find elements using WebDriver locator strategy.

```python
find_elements(using: str, value: str) -> List[Dict[str, Any]]
```

**Parameters:**

- `using` (str): Locator strategy
- `value` (str): Locator value

**Returns:**

- `List[Dict[str, Any]]`: List of element reference dictionaries

**Example:**

```python
elements = driver.find_elements("css selector", "div.item")
print(f"Found {len(elements)} items")
```

## Screenshots

### capture_screenshot()

Take a screenshot.

```python
capture_screenshot(selector: Optional[str] = None, full_page: bool = False) -> Optional[str]
```

**Parameters:**

- `selector` (Optional[str]): CSS selector for element screenshot (optional). Default: `None`
- `full_page` (bool): Whether to capture the full page. Default: `False`

**Returns:**

- `Optional[str]`: Base64-encoded screenshot or None

**Example:**

```python
# Capture full viewport
screenshot = driver.capture_screenshot()
with open("screenshot.png", "wb") as f:
    import base64
    f.write(base64.b64decode(screenshot))

# Capture full page
screenshot = driver.capture_screenshot(full_page=True)

# Capture specific element
element_screenshot = driver.capture_screenshot(selector="div.product")
```

**Alternative Name:**

`screenshot()` - Alias for capture_screenshot()

## Script Execution

### execute_script()

Execute synchronous JavaScript.

```python
execute_script(script: str, args: Optional[List[Any]] = None) -> Any
```

**Parameters:**

- `script` (str): JavaScript code to execute
- `args` (Optional[List[Any]]): Arguments to pass to the script. Default: `None`

**Returns:**

- Script return value

**Example:**

```python
# Execute simple script
title = driver.execute_script("return document.title")

# Execute with return value
height = driver.execute_script("return document.body.scrollHeight")

# Execute with arguments
result = driver.execute_script(
    "return arguments[0] + arguments[1]",
    args=[5, 10]
)  # Returns 15

# Modify DOM
driver.execute_script(
    "document.querySelector('h1').style.color = 'red'"
)

# Get computed style
color = driver.execute_script("""
    const el = document.querySelector('h1');
    return window.getComputedStyle(el).color;
""")
```

### evaluate()

Execute JavaScript and return the result.

```python
evaluate(expression: str) -> Any
```

**Parameters:**

- `expression` (str): JavaScript expression to evaluate

**Returns:**

- Evaluation result

**Example:**

```python
# Evaluate expression
page_title = driver.evaluate("document.title")

# Evaluate boolean
is_ready = driver.evaluate("document.readyState === 'complete'")

# Evaluate object
location = driver.evaluate("window.location.href")
```

### execute_async_script()

Execute asynchronous JavaScript.

```python
execute_async_script(
    script: str,
    args: Optional[List[Any]] = None,
    timeout: int = 30000
) -> Any
```

**Parameters:**

- `script` (str): JavaScript code to execute
- `args` (Optional[List[Any]]): Arguments to pass to the script. Default: `None`
- `timeout` (int): Execution timeout in milliseconds. Default: `30000`

**Returns:**

- Script return value

**Example:**

```python
# Async script with callback
result = driver.execute_async_script("""
    const callback = arguments[arguments.length - 1];
    setTimeout(() => callback('done'), 1000);
""")

# Fetch data asynchronously
data = driver.execute_async_script("""
    const callback = arguments[arguments.length - 1];
    fetch('/api/data')
        .then(res => res.json())
        .then(data => callback(data));
""")
```

## Control

### delay()

Delay execution for a specified time.

```python
delay(millis: int) -> Any
```

**Parameters:**

- `millis` (int): Delay in milliseconds

**Returns:**

- Delay result

**Example:**

```python
# Wait 1 second
driver.delay(1000)

# Wait for animation
driver.click("button.animate")
driver.delay(500)  # Wait for animation to complete
```

### pause()

Pause the session execution.

```python
pause() -> Any
```

**Returns:**

- Pause result

**Example:**

```python
driver.pause()
```

### stop()

Stop the session execution.

```python
stop() -> Any
```

**Returns:**

- Stop result

**Example:**

```python
driver.stop()
```

### bring_to_front()

Bring the browser window to the front.

```python
bring_to_front() -> Any
```

**Example:**

```python
driver.bring_to_front()
```

### close()

Close the driver (cleanup).

```python
close() -> None
```

**Note:** This is a no-op for the REST-based WebDriver. To properly release server resources, use `client.delete_session()` instead.

**Example:**

```python
# Driver close (no-op)
driver.close()

# Proper session cleanup
client.delete_session()
```

## Properties

### id

Get the driver ID.

```python
id: int
```

### navigate_history

Get the navigation history.

```python
navigate_history: List[str]
```

**Example:**

```python
driver.navigate_to("https://example.com")
driver.navigate_to("https://example.com/page1")
driver.navigate_to("https://example.com/page2")

print(f"Navigation history: {driver.navigate_history}")
# Output: ['https://example.com', 'https://example.com/page1', 'https://example.com/page2']
```

## Complete Example

```python
from pulsar_sdk import PulsarClient, WebDriver

# Setup
client = PulsarClient(base_url="http://localhost:8182")
client.create_session()
driver = WebDriver(client)

try:
    # Navigate
    driver.navigate_to("https://example.com")
    print(f"Title: {driver.title()}")
    
    # Check element existence
    if driver.exists("input.search"):
        # Fill and submit search
        driver.fill("input.search", "python tutorial")
        driver.press("input.search", "Enter")
        
        # Wait for results
        driver.wait_for_selector("div.results")
        
        # Scroll to load more
        driver.scroll_to_bottom()
        driver.delay(1000)
        
        # Extract data
        titles = driver.select_text_all("h2.result-title")
        links = driver.select_attribute_all("a.result-link", "href")
        
        print(f"Found {len(titles)} results")
        for title, link in zip(titles, links):
            print(f"- {title}: {link}")
        
        # Take screenshot
        screenshot = driver.capture_screenshot()
        
finally:
    # Close session to release server resources
    client.delete_session()
```

## Advanced Examples

### Form Handling

```python
driver.navigate_to("https://example.com/contact")

# Fill form fields
driver.fill("input[name='name']", "John Doe")
driver.fill("input[name='email']", "john@example.com")
driver.fill("textarea[name='message']", "Hello!")

# Check checkbox
driver.check("input#terms")

# Submit
driver.click("button[type='submit']")

# Wait for success message
driver.wait_for_selector("div.success", timeout=5000)
```

### Pagination

```python
page = 1
all_items = []

while True:
    # Extract items from current page
    items = driver.select_text_all("div.item")
    all_items.extend(items)
    
    # Check if next button exists
    if not driver.exists("button.next"):
        break
    
    # Go to next page
    driver.click("button.next")
    driver.wait_for_selector("div.item")
    page += 1

print(f"Collected {len(all_items)} items from {page} pages")
```

### Infinite Scroll

```python
last_count = 0
max_scrolls = 10

for _ in range(max_scrolls):
    # Scroll to bottom
    driver.scroll_to_bottom()
    driver.delay(1000)  # Wait for content to load
    
    # Count items
    items = driver.select_text_all("div.item")
    current_count = len(items)
    
    # Stop if no new items
    if current_count == last_count:
        break
    
    last_count = current_count

print(f"Loaded {last_count} items")
```

### Dynamic Content

```python
# Click to load dynamic content
driver.click("button.load-more")

# Wait for new content
driver.wait_for_selector("div.new-content", timeout=10000)

# Extract dynamically loaded data
data = driver.extract({
    "title": "h1.dynamic-title",
    "content": "div.dynamic-content"
})
```

## Error Handling

```python
try:
    driver.navigate_to("https://example.com")
    
    # Check if element exists before interacting
    if driver.exists("button.submit"):
        driver.click("button.submit")
    else:
        print("Submit button not found")
    
    # Wait with timeout
    if driver.wait_for_selector("div.results", timeout=5000):
        results = driver.select_text_all("div.results .item")
    else:
        print("Results did not load in time")
        
except Exception as e:
    print(f"Error: {e}")
finally:
    # Close session to release server resources
    client.delete_session()
```

## Best Practices

1. **Always Wait for Elements**: Use `wait_for_selector()` before interacting with dynamic content

```python
driver.wait_for_selector("button.submit")
driver.click("button.submit")
```

2. **Check Existence**: Verify elements exist before interacting

```python
if driver.exists("div.modal"):
    driver.click("button.modal-close")
```

3. **Use Delays Wisely**: Add delays for animations and dynamic content

```python
driver.click("button.animate")
driver.delay(500)  # Wait for animation
```

4. **Extract Data Efficiently**: Use batch extraction methods

```python
# Good: Extract multiple fields at once
data = driver.extract({
    "title": "h1",
    "price": ".price",
    "rating": ".rating"
})

# Less efficient: Multiple separate calls
title = driver.select_first_text_or_null("h1")
price = driver.select_first_text_or_null(".price")
rating = driver.select_first_text_or_null(".rating")
```

5. **Handle Navigation**: Wait for navigation to complete

```python
old_url = driver.current_url()
driver.click("a.next")
driver.wait_for_navigation(old_url)
```

## See Also

- [AgenticSession API](agentic-session.md) - AI-powered automation
- [PulsarSession API](session.md) - High-level session management
- [PulsarClient API](client.md) - HTTP client
- [Data Models](models.md) - Result types and data structures

---

[← Back to AgenticSession](agentic-session.md) | [Next: Models →](models.md)
