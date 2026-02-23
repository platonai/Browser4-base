# Screenshots

Complete guide to capturing screenshots with Browser4 Kotlin SDK.

## Overview

Browser4 provides powerful screenshot capabilities for visual verification, debugging, and documentation. Screenshots can capture the entire page, visible viewport, or specific elements.

## Basic Screenshots

### Capture Viewport

```kotlin
import ai.platon.pulsar.sdk.v0.*
import java.io.File
import java.util.Base64

fun main() {
    val session = AgenticSession.getOrCreate()
    val driver = session.driver
    
    driver.navigateTo("https://example.com")
    
    // Capture visible viewport
    val screenshot = driver.captureScreenshot()
    
    if (screenshot != null) {
        val bytes = Base64.getDecoder().decode(screenshot)
        File("viewport.png").writeBytes(bytes)
        println("Screenshot saved")
    }
    
    session.close()
}
```

### Capture Full Page

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com")

// Capture entire page (scrolls automatically)
val screenshot = driver.captureScreenshot(fullPage = true)

if (screenshot != null) {
    val bytes = Base64.getDecoder().decode(screenshot)
    File("fullpage.png").writeBytes(bytes)
}
```

### Capture Element

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com")

// Capture specific element
val screenshot = driver.captureScreenshot(
    selector = ".header",
    fullPage = false
)

if (screenshot != null) {
    val bytes = Base64.getDecoder().decode(screenshot)
    File("header.png").writeBytes(bytes)
}
```

## Screenshot Method

### Using screenshot()

The `screenshot()` method is a convenient alternative:

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com")

// Same as captureScreenshot(selector, fullPage=false)
val screenshot = driver.screenshot(".product-image")
```

## Saving Screenshots

### Save to File

```kotlin
fun saveScreenshot(screenshot: String?, filename: String): Boolean {
    if (screenshot == null) return false
    
    try {
        val bytes = Base64.getDecoder().decode(screenshot)
        File(filename).writeBytes(bytes)
        return true
    } catch (e: Exception) {
        println("Failed to save screenshot: ${"$"}{e.message}")
        return false
    }
}

// Usage
val screenshot = driver.captureScreenshot(fullPage = true)
saveScreenshot(screenshot, "page.png")
```

### Save with Timestamp

```kotlin
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun saveScreenshotWithTimestamp(screenshot: String?, prefix: String = "screenshot"): String? {
    if (screenshot == null) return null
    
    val timestamp = LocalDateTime.now().format(
        DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
    )
    val filename = "${"$"}{prefix}_${"$"}{timestamp}.png"
    
    val bytes = Base64.getDecoder().decode(screenshot)
    File(filename).writeBytes(bytes)
    
    return filename
}

// Usage
val filename = saveScreenshotWithTimestamp(driver.captureScreenshot())
println("Saved: ${"$"}filename")
```

## Advanced Patterns

### Screenshot Before/After

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com")

// Before action
val before = driver.captureScreenshot()
saveScreenshot(before, "before.png")

// Perform action
driver.click("button.toggle-menu")
Thread.sleep(500) // Wait for animation

// After action
val after = driver.captureScreenshot()
saveScreenshot(after, "after.png")
```

### Screenshot Error States

```kotlin
fun performActionWithScreenshot(action: () -> Unit) {
    try {
        action()
    } catch (e: Exception) {
        // Capture screenshot on error
        val screenshot = driver.captureScreenshot(fullPage = true)
        val filename = saveScreenshotWithTimestamp(screenshot, "error")
        println("Error screenshot saved: ${"$"}filename")
        throw e
    }
}

// Usage
performActionWithScreenshot {
    driver.click(".non-existent-element")
}
```

### Screenshot Multiple Elements

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com/products")

// Get all product cards
val productCount = driver.executeScript(
    "return document.querySelectorAll('.product-card').length"
) as Int

// Screenshot each product
for (i in 0 until productCount) {
    val selector = ".product-card:nth-child(${"$"}{i + 1})"
    val screenshot = driver.captureScreenshot(selector = selector)
    saveScreenshot(screenshot, "product_${"$"}{i + 1}.png")
}
```

### Screenshot During Scroll

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com")

// Take screenshots while scrolling
val screenshots = mutableListOf<String>()

repeat(5) { index ->
    // Capture current view
    val screenshot = driver.captureScreenshot()
    if (screenshot != null) {
        screenshots.add(screenshot)
        saveScreenshot(screenshot, "scroll_${"$"}{index + 1}.png")
    }
    
    // Scroll down
    driver.scrollDown(count = 1)
    Thread.sleep(500)
}

println("Captured ${"$"}{screenshots.size} screenshots")
```

## Visual Verification

### Compare Screenshots

```kotlin
import java.security.MessageDigest

fun getScreenshotHash(screenshot: String): String {
    val bytes = Base64.getDecoder().decode(screenshot)
    val md = MessageDigest.getInstance("SHA-256")
    val hash = md.digest(bytes)
    return hash.joinToString("") { "%02x".format(it) }
}

val driver = session.driver
driver.navigateTo("https://example.com")

val screenshot1 = driver.captureScreenshot()
Thread.sleep(1000)
val screenshot2 = driver.captureScreenshot()

if (screenshot1 != null && screenshot2 != null) {
    val hash1 = getScreenshotHash(screenshot1)
    val hash2 = getScreenshotHash(screenshot2)
    
    if (hash1 == hash2) {
        println("Screenshots are identical")
    } else {
        println("Screenshots differ")
    }
}
```

### Verify Element Visibility

```kotlin
fun isElementVisuallyPresent(driver: WebDriver, selector: String): Boolean {
    val screenshot = driver.captureScreenshot(selector = selector)
    return screenshot != null && screenshot.length > 1000 // Min size check
}

// Usage
if (isElementVisuallyPresent(driver, ".cookie-banner")) {
    println("Cookie banner is visible")
    driver.click(".cookie-banner .accept")
}
```

## Best Practices

1. **Wait for rendering** - Add delay after navigation/interactions
2. **Full page for documentation** - Use fullPage for complete captures
3. **Elements for precision** - Use selector for specific components
4. **Timestamps for debugging** - Include timestamps in filenames
5. **Clean up old screenshots** - Remove screenshots after test runs
6. **Check for null** - Always handle null screenshot results
7. **Appropriate resolution** - Consider viewport size for screenshots
8. **Error screenshots** - Capture on failures for debugging
9. **Compression** - PNG is good balance of quality and size
10. **Storage** - Don't commit screenshots to version control

## Troubleshooting

### Screenshot is Blank

```kotlin
// Ensure page is fully loaded
driver.navigateTo("https://example.com")
driver.waitForSelector("body")
Thread.sleep(1000) // Wait for rendering

val screenshot = driver.captureScreenshot()
```

### Screenshot is Partial

```kotlin
// For full page, use fullPage parameter
val screenshot = driver.captureScreenshot(fullPage = true)

// Or scroll to ensure content is loaded
driver.scrollToBottom()
Thread.sleep(500)
val screenshot2 = driver.captureScreenshot(fullPage = true)
```

### Element Not in Screenshot

```kotlin
// Scroll element into view first
driver.scrollTo(".target-element")
Thread.sleep(300)

val screenshot = driver.captureScreenshot(selector = ".target-element")
```

## Next Steps

- [Script Execution](script-execution.md) - Custom screenshot logic
- [Examples](../examples/basic-usage.md) - More screenshot examples
- [API Reference](../api/webdriver.md) - Complete screenshot API
