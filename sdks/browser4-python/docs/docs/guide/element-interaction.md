# Element Interaction

Interact with page elements through clicking, typing, form filling, and more. Browser4's WebDriver provides comprehensive element interaction capabilities with CSS selector-based targeting.

## Basic Element Interaction

### Clicking Elements

```python
from browser4 import PulsarClient, WebDriver

client = PulsarClient(base_url="http://localhost:8182")
session_id = client.create_session()
driver = WebDriver(client)

# Navigate to page
driver.navigate_to("https://example.com")

# Click element by CSS selector
driver.click("button.submit")
driver.click("a.login-link")
driver.click("#accept-button")

# Click with complex selectors
driver.click("div.modal button[type='submit']")
driver.click("nav ul li:nth-child(3) a")
```

### Typing and Filling

**fill()** - Replace entire field content:
```python
# Fill replaces all text in the field
driver.fill("input[name='email']", "user@example.com")
driver.fill("input#username", "john_doe")
driver.fill("textarea.comment", "This is my comment")
```

**type()** - Type character by character:
```python
# Type simulates human typing (slower but more realistic)
driver.type("input[name='search']", "browser automation")

# Type is useful for triggering keystroke events
driver.type("input.autocomplete", "python")
```

### Pressing Keys

```python
# Press Enter key
driver.press("input[name='search']", "Enter")

# Press other special keys
driver.press("input.field", "Tab")
driver.press("input.field", "Escape")
driver.press("textarea", "Control+A")

# Common pattern: fill and submit
driver.fill("input[name='search']", "query text")
driver.press("input[name='search']", "Enter")
```

## Form Interaction

### Complete Form Example

```python
def fill_login_form(driver):
    """Fill and submit a login form."""
    
    # Fill username
    driver.fill("input#username", "john_doe")
    
    # Fill password
    driver.fill("input#password", "SecurePass123!")
    
    # Check "remember me"
    driver.check("input#remember")
    
    # Submit form
    driver.click("button[type='submit']")
    
    # Wait for navigation
    driver.wait_for_selector(".dashboard", timeout=10000)
    print("Login successful!")

# Usage
driver.navigate_to("https://example.com/login")
fill_login_form(driver)
```

### Checkbox and Radio Buttons

```python
# Check a checkbox
driver.check("input#terms-checkbox")

# Uncheck a checkbox
driver.uncheck("input#newsletter")

# Toggle checkbox
if driver.is_checked("input#newsletter"):
    driver.uncheck("input#newsletter")
else:
    driver.check("input#newsletter")

# Select radio button
driver.click("input[type='radio'][value='option1']")
```

### Select Dropdowns

```python
# For standard <select> elements, use click
driver.click("select#country")
driver.click("select#country option[value='US']")

# Or use custom JavaScript
driver.execute_script("""
    document.querySelector('select#country').value = 'US';
    document.querySelector('select#country').dispatchEvent(new Event('change'));
""")
```

## Element States

### Checking Element Existence

```python
# Check if element exists
if driver.exists("button.submit"):
    print("Submit button found")
    driver.click("button.submit")
else:
    print("Submit button not found")

# Check multiple elements
elements = ["#header", ".navbar", ".footer"]
for selector in elements:
    exists = driver.exists(selector)
    print(f"{selector}: {'Found' if exists else 'Not found'}")
```

### Checking Element Visibility

```python
# Check if element is visible
if driver.is_visible(".modal"):
    print("Modal is visible")
    driver.click(".modal button.close")
else:
    print("Modal is hidden")

# Wait for visibility
driver.wait_for_selector(".loading-spinner", timeout=5000)
if driver.is_visible(".loading-spinner"):
    print("Still loading...")
```

### Waiting for Elements

```python
# Wait for element to appear
found = driver.wait_for_selector("h1.title", timeout=10000)
if found:
    print("Title element appeared")
else:
    print("Timeout waiting for title")

# Wait for multiple elements
selectors = [".header", ".content", ".footer"]
for selector in selectors:
    found = driver.wait_for_selector(selector, timeout=5000)
    print(f"{selector}: {'Found' if found else 'Timeout'}")

# Wait and interact pattern
if driver.wait_for_selector("button.load-more", timeout=10000):
    driver.click("button.load-more")
```

## Advanced Interactions

### Hovering

```python
# Hover over element to reveal submenu
driver.hover(".menu-item")

# Wait for submenu to appear
driver.wait_for_selector(".submenu", timeout=2000)

# Click submenu item
driver.click(".submenu a.option")

# Hover pattern for tooltips
driver.hover(".info-icon")
driver.wait_for_selector(".tooltip", timeout=1000)
tooltip_text = driver.select_first_text_or_null(".tooltip")
print(f"Tooltip: {tooltip_text}")
```

### Focusing Elements

```python
# Focus on input field
driver.focus("input#search")

# Focus is useful for triggering focus events
driver.focus("input.autocomplete")

# Tab through form fields
fields = ["#field1", "#field2", "#field3"]
for field in fields:
    driver.focus(field)
    driver.fill(field, "value")
```

### Element Scrolling

```python
# Scroll to specific element
driver.scroll_to("h2.section-title")

# Scroll element into view before interacting
driver.scroll_to("button.far-down")
driver.click("button.far-down")

# Scroll to element with offset
driver.execute_script("""
    document.querySelector('.element').scrollIntoView({
        behavior: 'smooth',
        block: 'center'
    });
""")
```

## Scrolling

### Basic Scrolling

```python
# Scroll down (default: 1 viewport height)
driver.scroll_down()

# Scroll down multiple times
driver.scroll_down(count=3)

# Scroll up
driver.scroll_up()
driver.scroll_up(count=2)

# Get scroll position
position = driver.scroll_down()
print(f"Scrolled to position: {position}")
```

### Scroll to Positions

```python
# Scroll to top of page
driver.scroll_to_top()

# Scroll to bottom of page
driver.scroll_to_bottom()

# Scroll to middle
driver.scroll_to_middle(0.5)  # 50% of page height

# Scroll to specific percentage
driver.scroll_to_middle(0.25)  # 25% of page height
driver.scroll_to_middle(0.75)  # 75% of page height
```

### Infinite Scroll Pattern

```python
def scroll_to_load_all(driver, max_scrolls=10):
    """Scroll to load all content on infinite scroll page."""
    
    for i in range(max_scrolls):
        # Get current scroll position
        prev_position = driver.evaluate("window.scrollY")
        
        # Scroll down
        driver.scroll_down()
        
        # Wait for content to load
        driver.delay(1000)
        
        # Check if reached bottom
        current_position = driver.evaluate("window.scrollY")
        if current_position == prev_position:
            print(f"Reached bottom after {i+1} scrolls")
            break
    
    print("Finished loading all content")

# Usage
driver.navigate_to("https://example.com/infinite-scroll")
scroll_to_load_all(driver)
```

## Complete Form Example

```python
from browser4 import Browser4Driver, PulsarClient, AgenticSession

def complete_registration_form():
    """Complete a registration form with various input types."""
    
    with Browser4Driver() as driver_mgr:
        client = PulsarClient(base_url=driver_mgr.base_url)
        session_id = client.create_session()
        session = AgenticSession(client)
        driver = session.driver
        
        try:
            # Navigate to form
            driver.navigate_to("https://example.com/register")
            
            # Wait for form to load
            driver.wait_for_selector("form#registration", timeout=10000)
            
            # Fill text fields
            driver.fill("input#firstName", "John")
            driver.fill("input#lastName", "Doe")
            driver.fill("input#email", "john.doe@example.com")
            
            # Fill password
            driver.fill("input#password", "SecurePass123!")
            driver.fill("input#confirmPassword", "SecurePass123!")
            
            # Select radio button
            driver.click("input[name='gender'][value='male']")
            
            # Check checkboxes
            driver.check("input#terms")
            driver.check("input#newsletter")
            
            # Select dropdown
            driver.click("select#country")
            driver.click("select#country option[value='US']")
            
            # Fill textarea
            driver.fill("textarea#bio", "Software developer interested in automation.")
            
            # Scroll to submit button
            driver.scroll_to("button[type='submit']")
            
            # Submit form
            driver.click("button[type='submit']")
            
            # Wait for success message
            if driver.wait_for_selector(".success-message", timeout=10000):
                message = driver.select_first_text_or_null(".success-message")
                print(f"Registration successful: {message}")
            else:
                print("No success message found")
            
        finally:
            session.close()
            client.close()

if __name__ == "__main__":
    complete_registration_form()
```

## Element Interaction Patterns

### Conditional Interaction Pattern

```python
def interact_conditionally(driver):
    """Interact based on element state."""
    
    # Check and interact pattern
    if driver.exists("button.cookie-accept"):
        driver.click("button.cookie-accept")
        print("Accepted cookies")
    
    # Wait and interact pattern
    if driver.wait_for_selector(".modal", timeout=5000):
        if driver.is_visible(".modal"):
            driver.click(".modal button.close")
            print("Closed modal")
    
    # Verify before interact pattern
    if driver.exists("input#search"):
        driver.fill("input#search", "query")
        driver.press("input#search", "Enter")
        print("Searched")

interact_conditionally(driver)
```

### Retry Interaction Pattern

```python
def click_with_retry(driver, selector, max_attempts=3):
    """Click element with retry logic."""
    
    for attempt in range(max_attempts):
        try:
            # Wait for element
            if driver.wait_for_selector(selector, timeout=5000):
                # Scroll into view
                driver.scroll_to(selector)
                
                # Click
                driver.click(selector)
                print(f"Clicked {selector}")
                return True
                
        except Exception as e:
            print(f"Click attempt {attempt + 1} failed: {e}")
            if attempt < max_attempts - 1:
                driver.delay(1000)
    
    print(f"Failed to click {selector} after {max_attempts} attempts")
    return False

# Usage
success = click_with_retry(driver, "button.dynamic-element")
```

### Form Validation Pattern

```python
def fill_and_validate(driver, field_selector, value, error_selector):
    """Fill field and check for validation errors."""
    
    # Fill field
    driver.fill(field_selector, value)
    
    # Trigger validation (blur)
    driver.focus(field_selector)
    driver.press(field_selector, "Tab")
    
    # Wait for validation
    driver.delay(500)
    
    # Check for error
    if driver.exists(error_selector) and driver.is_visible(error_selector):
        error = driver.select_first_text_or_null(error_selector)
        print(f"Validation error: {error}")
        return False
    
    return True

# Usage
valid = fill_and_validate(
    driver,
    "input#email",
    "invalid-email",
    ".email-error"
)
```

## AI-Powered Interaction

### Natural Language Actions

```python
from browser4 import AgenticSession

session = AgenticSession(client)
session.open("https://example.com")

# Use AI to interact with elements
session.act("click the login button")
session.act("fill in the email field with user@example.com")
session.act("check the remember me checkbox")
session.act("scroll down to the footer")

# Complex multi-step interaction
result = session.run("""
    1. Click on the search box
    2. Type 'python programming'
    3. Press Enter
    4. Wait for results to load
    5. Click on the first result
""")

print(f"Task completed: {result.success}")
```

## Troubleshooting

### Element Not Found

```python
# Check if element exists before interacting
if not driver.exists("button.submit"):
    print("Submit button not found")
    # Alternative approach
    driver.wait_for_selector("button.submit", timeout=10000)

# Check page loaded correctly
if driver.current_url() != expected_url:
    print("Unexpected page loaded")
```

### Element Not Clickable

```python
# Scroll element into view first
driver.scroll_to("button.far-down")
driver.click("button.far-down")

# Wait for element to be ready
driver.wait_for_selector("button.dynamic", timeout=10000)
driver.delay(500)  # Extra wait for animations
driver.click("button.dynamic")

# Check if element is visible
if driver.is_visible("button.submit"):
    driver.click("button.submit")
else:
    print("Button not visible")
```

### Timing Issues

```python
# Add delays between interactions
driver.fill("input#field1", "value1")
driver.delay(500)  # Wait 500ms
driver.fill("input#field2", "value2")

# Wait for page updates
driver.click("button.load-data")
driver.wait_for_selector(".data-loaded", timeout=10000)

# Use execute_script for immediate actions
driver.execute_script("""
    document.querySelector('button').click();
""")
```

### Dynamic Content

```python
# Wait for dynamic content to load
driver.navigate_to("https://example.com/dynamic")

# Wait for specific element
driver.wait_for_selector(".dynamic-content", timeout=15000)

# Verify content loaded
if driver.exists(".dynamic-content"):
    content = driver.select_first_text_or_null(".dynamic-content")
    if content:
        print(f"Content loaded: {content[:100]}")
```

## Best Practices

1. **Wait for elements** before interacting - Use `wait_for_selector()`
2. **Check existence first** - Use `exists()` before clicking
3. **Scroll into view** - Use `scroll_to()` for off-screen elements
4. **Use appropriate selectors** - Prefer IDs and stable classes
5. **Add delays when needed** - Use `delay()` for timing-sensitive operations
6. **Handle dynamic content** - Wait for elements to appear
7. **Verify actions** - Check results after interactions
8. **Use type() for realism** - When keystroke events matter
9. **Use fill() for speed** - When just setting values
10. **Clean up interactions** - Close modals, dismiss alerts

## Performance Tips

### Batch Interactions

```python
# Instead of multiple separate calls
driver.fill("input#field1", "value1")
driver.fill("input#field2", "value2")
driver.fill("input#field3", "value3")

# Consider using execute_script for batch operations
driver.execute_script("""
    document.querySelector('#field1').value = 'value1';
    document.querySelector('#field2').value = 'value2';
    document.querySelector('#field3').value = 'value3';
""")
```

### Minimize Waits

```python
# Avoid unnecessary waits
# driver.delay(5000)  # Bad: arbitrary wait

# Use conditional waits
driver.wait_for_selector(".element", timeout=5000)  # Good: wait only as needed
```

### Reuse Selectors

```python
# Define selectors once
SUBMIT_BUTTON = "button[type='submit']"
EMAIL_FIELD = "input[name='email']"
PASSWORD_FIELD = "input[name='password']"

# Reuse throughout code
driver.fill(EMAIL_FIELD, "user@example.com")
driver.fill(PASSWORD_FIELD, "password")
driver.click(SUBMIT_BUTTON)
```

## Next Steps

- **[Data Extraction](data-extraction.md)** - Extract data from pages
- **[Navigation](navigation.md)** - Navigate between pages
- **[Screenshots](screenshots.md)** - Capture page screenshots
- **[AI Automation](ai-automation.md)** - Use AI for interactions
