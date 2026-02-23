# Element Interaction

Complete guide to interacting with web page elements using Browser4 Kotlin SDK.

## Overview

Element interaction is core to browser automation. Browser4 provides comprehensive methods for clicking, filling forms, hovering, and manipulating page elements.

## Clicking Elements

### Basic Click

```kotlin
import ai.platon.pulsar.sdk.v0.*

fun main() {
    val session = AgenticSession.getOrCreate()
    val driver = session.driver
    
    driver.navigateTo("https://example.com")
    
    // Click using CSS selector
    driver.click("button.submit")
    
    // Click using XPath
    driver.click("//button[@type='submit']", strategy = "xpath")
    
    session.close()
}
```

### Click with Wait

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com")

// Wait for element before clicking
driver.waitForSelector("button.submit")
driver.click("button.submit")

// Or check if element exists first
if (driver.exists("button.submit")) {
    driver.click("button.submit")
}
```

### Click Multiple Elements

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com")

// Click all matching elements
val buttons = driver.selectAttributeAll("button.item", "id")
buttons.forEach { buttonId ->
    driver.click("#$buttonId")
    Thread.sleep(500) // Brief pause between clicks
}
```

## Form Interaction

### Fill Input Fields

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com/form")

// Fill text input
driver.fill("input[name='username']", "john_doe")
driver.fill("input[name='email']", "john@example.com")
driver.fill("input[type='password']", "secret123")

// Fill textarea
driver.fill("textarea[name='comment']", "This is a comment")
```

### Type Text (Keyboard Simulation)

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com/search")

// Type text (simulates keyboard input)
driver.type("input[name='search']", "kotlin programming")

// Type vs Fill:
// - type() simulates keyboard events (triggers JS listeners)
// - fill() directly sets value (faster but may not trigger events)
```

### Press Keys

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com/search")

// Fill search box
driver.fill("input[name='search']", "kotlin")

// Press Enter to submit
driver.press("input[name='search']", "Enter")

// Other keys
driver.press("input[name='field']", "Tab")
driver.press("input[name='field']", "Escape")
driver.press("input[name='field']", "ArrowDown")
```

## Checkboxes and Radio Buttons

### Check/Uncheck Checkboxes

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com/form")

// Check checkbox
driver.check("input[type='checkbox']#terms")
driver.check("input[type='checkbox']#newsletter")

// Uncheck checkbox
driver.uncheck("input[type='checkbox']#newsletter")

// Toggle based on condition
val shouldAcceptTerms = true
if (shouldAcceptTerms) {
    driver.check("input[type='checkbox']#terms")
}
```

### Select Radio Buttons

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com/form")

// Select radio button
driver.click("input[type='radio'][value='option1']")

// Select by label
driver.click("label[for='radio-option-2']")
```

## Dropdown Menus

### Select from Dropdown

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com/form")

// Select by clicking the dropdown and option
driver.click("select[name='country']")
driver.click("option[value='USA']")

// Or using JavaScript
driver.executeScript("""
    document.querySelector('select[name="country"]').value = 'USA';
    document.querySelector('select[name="country"]').dispatchEvent(new Event('change'));
""")
```

### Multi-Select

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com/form")

// Select multiple options
val options = listOf("option1", "option2", "option3")
for (option in options) {
    driver.click("option[value='$option']")
}
```

## Hover and Focus

### Hover Over Elements

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com")

// Hover to reveal dropdown
driver.hover(".menu-item")
Thread.sleep(500) // Wait for dropdown animation

// Click revealed element
driver.click(".dropdown-item")
```

### Focus Elements

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com/form")

// Focus on input field
driver.focus("input[name='email']")

// Type after focusing
driver.type("input[name='email']", "user@example.com")
```

## Advanced Interactions

### Drag and Drop

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com/drag-drop")

// Drag and drop using JavaScript
driver.executeScript("""
    const source = document.querySelector('.draggable');
    const target = document.querySelector('.drop-zone');
    
    const dragEvent = new DragEvent('dragstart', {
        dataTransfer: new DataTransfer()
    });
    source.dispatchEvent(dragEvent);
    
    const dropEvent = new DragEvent('drop', {
        dataTransfer: dragEvent.dataTransfer
    });
    target.dispatchEvent(dropEvent);
""")
```

### Double Click

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com")

// Double click using JavaScript
driver.executeScript("""
    const element = document.querySelector('.double-click-target');
    const event = new MouseEvent('dblclick', {
        bubbles: true,
        cancelable: true,
        view: window
    });
    element.dispatchEvent(event);
""")
```

### Right Click (Context Menu)

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com")

// Right click using JavaScript
driver.executeScript("""
    const element = document.querySelector('.context-menu-target');
    const event = new MouseEvent('contextmenu', {
        bubbles: true,
        cancelable: true,
        view: window,
        button: 2
    });
    element.dispatchEvent(event);
""")
```

## File Upload

### Upload Single File

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com/upload")

// Set file path using JavaScript
driver.executeScript("""
    const input = document.querySelector('input[type="file"]');
    const dataTransfer = new DataTransfer();
    dataTransfer.items.add(new File(['content'], 'filename.txt'));
    input.files = dataTransfer.files;
    input.dispatchEvent(new Event('change', { bubbles: true }));
""")
```

### Upload Multiple Files

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com/upload")

val files = listOf("file1.txt", "file2.txt", "file3.txt")

driver.executeScript("""
    const input = document.querySelector('input[type="file"][multiple]');
    const dataTransfer = new DataTransfer();
    ${files.joinToString("\n") { 
        "dataTransfer.items.add(new File(['content'], '$it'));" 
    }}
    input.files = dataTransfer.files;
    input.dispatchEvent(new Event('change', { bubbles: true }));
""")
```

## Waiting Strategies

### Wait for Element to Exist

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com")

// Wait for element (default 30s timeout)
driver.waitForSelector(".dynamic-content")

// Wait with custom timeout
driver.waitForSelector(".slow-content", timeout = 60000)
```

### Wait for Element to be Visible

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com")

// Check visibility
if (driver.isVisible(".modal")) {
    println("Modal is visible")
    driver.click(".modal .close-button")
}

// Wait until visible (custom implementation)
fun waitUntilVisible(selector: String, timeout: Long = 30000) {
    val start = System.currentTimeMillis()
    while (System.currentTimeMillis() - start < timeout) {
        if (driver.isVisible(selector)) {
            return
        }
        Thread.sleep(100)
    }
    throw Exception("Element not visible within timeout")
}

waitUntilVisible(".notification")
```

### Wait for Element to Disappear

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com")

// Click button that shows loading indicator
driver.click("button.load-data")

// Wait for loading indicator to disappear
fun waitUntilHidden(selector: String, timeout: Long = 30000) {
    val start = System.currentTimeMillis()
    while (System.currentTimeMillis() - start < timeout) {
        if (driver.isHidden(selector)) {
            return
        }
        Thread.sleep(100)
    }
}

waitUntilHidden(".loading-spinner")
```

## Element State Checking

### Check if Element Exists

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com")

if (driver.exists(".error-message")) {
    val error = driver.selectFirstTextOrNull(".error-message")
    println("Error: $error")
}

if (!driver.exists(".premium-feature")) {
    println("Premium feature not available")
}
```

### Check if Element is Visible

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com")

// Check visibility
val isVisible = driver.isVisible(".notification")
println("Notification visible: $isVisible")

// Check hidden
val isHidden = driver.isHidden(".secret-content")
println("Content hidden: $isHidden")
```

### Check Element State with JavaScript

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com")

// Check if element is enabled
val isEnabled = driver.executeScript("""
    return !document.querySelector('button.submit').disabled;
""") as Boolean

// Check if checkbox is checked
val isChecked = driver.executeScript("""
    return document.querySelector('input[type="checkbox"]').checked;
""") as Boolean

println("Button enabled: $isEnabled")
println("Checkbox checked: $isChecked")
```

## Complex Interaction Patterns

### Fill Multi-Step Form

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com/multi-step-form")

// Step 1
driver.fill("input[name='firstName']", "John")
driver.fill("input[name='lastName']", "Doe")
driver.click("button.next")
driver.waitForSelector(".step-2")

// Step 2
driver.fill("input[name='email']", "john@example.com")
driver.fill("input[name='phone']", "555-1234")
driver.click("button.next")
driver.waitForSelector(".step-3")

// Step 3
driver.check("input[type='checkbox']#terms")
driver.click("button.submit")
driver.waitForSelector(".success-message")

println("Form submitted successfully")
```

### Handle Modal Dialogs

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com")

// Click button that opens modal
driver.click("button.open-modal")
driver.waitForSelector(".modal")

// Wait for modal to be visible
Thread.sleep(500) // Wait for animation

// Interact with modal
driver.fill(".modal input[name='name']", "John Doe")
driver.click(".modal button.submit")

// Wait for modal to close
fun waitForModalClose() {
    val start = System.currentTimeMillis()
    while (System.currentTimeMillis() - start < 10000) {
        if (driver.isHidden(".modal")) {
            return
        }
        Thread.sleep(100)
    }
}

waitForModalClose()
println("Modal closed")
```

### Handle Autocomplete

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com/search")

// Type in autocomplete field
driver.type("input.autocomplete", "kotlin")

// Wait for suggestions
driver.waitForSelector(".autocomplete-suggestions")
Thread.sleep(500) // Wait for suggestions to load

// Click first suggestion
driver.click(".autocomplete-suggestions li:first-child")

println("Autocomplete selection made")
```

### Infinite Scroll Interaction

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com/infinite-scroll")

var itemCount = 0
var maxScrolls = 10

repeat(maxScrolls) { scrollNum ->
    // Get current item count
    val items = driver.selectTextAll(".item")
    val newCount = items.size
    
    if (newCount == itemCount) {
        // No new items loaded, we've reached the end
        println("Reached end of content")
        return@repeat
    }
    
    itemCount = newCount
    println("Scroll $scrollNum: Loaded $itemCount items")
    
    // Scroll to bottom
    driver.scrollToBottom()
    
    // Wait for new content
    Thread.sleep(1000)
}

println("Total items: $itemCount")
```

## Error Handling

### Handle Missing Elements

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com")

try {
    driver.click(".optional-button")
} catch (e: Exception) {
    println("Optional button not found, continuing...")
}

// Or use exists() check
if (driver.exists(".optional-button")) {
    driver.click(".optional-button")
} else {
    println("Optional button not present")
}
```

### Handle Click Failures

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com")

fun safeClick(selector: String, maxAttempts: Int = 3): Boolean {
    repeat(maxAttempts) { attempt ->
        try {
            driver.waitForSelector(selector, timeout = 5000)
            driver.click(selector)
            return true
        } catch (e: Exception) {
            println("Click attempt ${attempt + 1} failed: ${e.message}")
            if (attempt < maxAttempts - 1) {
                Thread.sleep(1000)
            }
        }
    }
    return false
}

val success = safeClick("button.submit")
if (!success) {
    println("Failed to click button after multiple attempts")
}
```

### Handle Stale Elements

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com")

fun clickWithRefresh(selector: String) {
    var attempts = 0
    while (attempts < 3) {
        try {
            driver.click(selector)
            return
        } catch (e: Exception) {
            // Element might be stale, wait and retry
            Thread.sleep(500)
            attempts++
        }
    }
    throw Exception("Element became stale: $selector")
}

clickWithRefresh("button.dynamic")
```

## Best Practices

1. **Always wait for elements** - Use `waitForSelector()` before interacting
2. **Check element existence** - Use `exists()` for optional elements
3. **Use appropriate selectors** - Prefer stable selectors (IDs, data attributes)
4. **Handle animations** - Add small delays after interactions that trigger animations
5. **Verify actions** - Check that interactions had the expected effect
6. **Use `type()` for JS listeners** - When form fields have JavaScript event listeners
7. **Implement retry logic** - Handle transient failures with retries
8. **Wait for state changes** - Wait for loading indicators to disappear
9. **Avoid hard-coded sleeps** - Use proper waiting mechanisms when possible
10. **Test in isolation** - Test complex interactions in isolation first

## Next Steps

- **[Data Extraction](data-extraction.md)** - Extract data after interaction
- **[Screenshots](screenshots.md)** - Capture interaction results visually
- **[Script Execution](script-execution.md)** - Advanced JavaScript interactions
- **[AI Automation](ai-automation.md)** - Let AI handle complex interactions
