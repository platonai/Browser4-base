# Navigation

Comprehensive guide to navigating web pages with Browser4 Kotlin SDK.

## Overview

Navigation is the foundation of browser automation. Browser4 provides multiple approaches to loading and navigating web pages, each optimized for different scenarios.

## Navigation Methods

### WebDriver Navigation

The `WebDriver` class provides direct browser control methods:

```kotlin
import ai.platon.pulsar.sdk.v0.*

fun main() {
    val session = AgenticSession.getOrCreate()
    val driver = session.driver
    
    // Navigate to URL
    driver.navigateTo("https://example.com")
    println("Current URL: ${driver.currentUrl()}")
    
    // Get page title
    println("Title: ${driver.title()}")
    
    session.close()
}
```

### Session Loading

The `PulsarSession` class provides high-level page loading:

```kotlin
val session = AgenticSession.getOrCreate()

// Open (fresh load, bypasses cache)
val page1 = session.open("https://example.com")

// Load (uses cache when available)
val page2 = session.load("https://example.com", "-expire 1d")

// Submit (background processing)
val page3 = session.submit("https://example.com")

session.close()
```

## Basic Navigation

### Navigate to URL

```kotlin
val driver = session.driver

// Simple navigation
driver.navigateTo("https://example.com")

// Wait for navigation to complete
driver.waitForNavigation()

// Check current URL
val currentUrl = driver.currentUrl()
println("Now at: $currentUrl")
```

### Open vs Navigate

Use `open()` for navigation with waiting:

```kotlin
// Open and wait for page load
driver.open("https://example.com")

// Equivalent to:
driver.navigateTo("https://example.com")
driver.waitForNavigation()
```

### Navigation History

```kotlin
val driver = session.driver

// Navigate to first page
driver.navigateTo("https://example.com")

// Navigate to second page
driver.navigateTo("https://google.com")

// Go back
driver.goBack()
println("Back to: ${driver.currentUrl()}")

// Go forward
driver.goForward()
println("Forward to: ${driver.currentUrl()}")

// View navigation history
val history = driver.navigateHistory
println("History: $history")
```

### Reload Page

```kotlin
val driver = session.driver

driver.navigateTo("https://example.com")

// Reload current page
driver.reload()
driver.waitForNavigation()

println("Page reloaded")
```

## Page Information

### Get URL Information

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com/path?query=value")

// Current browser URL
val url = driver.currentUrl()
println("Current URL: $url")

// Document URL (may differ due to redirects)
val docUrl = driver.url()
println("Document URL: $docUrl")

// Document URI
val uri = driver.documentUri()
println("Document URI: $uri")

// Base URI (for relative links)
val baseUri = driver.baseUri()
println("Base URI: $baseUri")
```

### Get Page Title

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com")

val title = driver.title()
println("Page title: $title")
```

### Get Page Source

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com")

// Get full HTML source
val html = driver.pageSource()
println("HTML length: ${html?.length}")
```

## Wait Strategies

### Wait for Navigation

```kotlin
val driver = session.driver

// Click link and wait for navigation
driver.click("a.next-page")
driver.waitForNavigation()

println("Navigation complete")
```

### Wait for Element

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com")

// Wait for element to appear (default 30s timeout)
driver.waitForSelector(".dynamic-content")

// Wait with custom timeout (60 seconds)
driver.waitForSelector(".slow-content", timeout = 60000)

// Continue only after element appears
val content = driver.selectFirstTextOrNull(".dynamic-content")
println("Content: $content")
```

### Check Element Existence

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com")

// Check if element exists
if (driver.exists(".modal")) {
    println("Modal exists")
    driver.click(".modal .close-button")
}

// Check if element is visible
if (driver.isVisible(".notification")) {
    println("Notification is visible")
}

// Check if element is hidden
if (driver.isHidden(".menu")) {
    println("Menu is hidden")
}
```

## Load Arguments

Pulsar sessions support load arguments for fine-grained control:

### Common Arguments

```kotlin
val session = AgenticSession.getOrCreate()

// Cache expiration
val page1 = session.load("https://example.com", "-expire 1d")  // 1 day
val page2 = session.load("https://example.com", "-expire 1h")  // 1 hour
val page3 = session.load("https://example.com", "-expire 1m")  // 1 minute

// Force refresh
val page4 = session.load("https://example.com", "-refresh")

// Activate parsing
val page5 = session.load("https://example.com", "-parse")

// Extract outgoing links
val page6 = session.load("https://example.com", "-outLink a[href]")

// Combine arguments
val page7 = session.load(
    "https://example.com",
    "-expire 1d -refresh -parse -outLink a"
)

session.close()
```

### Page Load Options

```kotlin
val session = AgenticSession.getOrCreate()

// Load with detailed options
val page = session.load(
    url = "https://example.com",
    args = """
        -expire 1d
        -itemExpire 1h
        -refresh
        -parse
        -requireSize 200000
    """.trimIndent()
)

println("Loaded: ${page.url}")
println("Content length: ${page.contentLength}")

session.close()
```

## URL Normalization

Normalize URLs before loading:

```kotlin
val session = AgenticSession.getOrCreate()

// Normalize URL with arguments
val normUrl = session.normalize(
    "https://example.com",
    "-expire 1d -parse"
)

println("Normalized URL: ${normUrl.url}")
println("Arguments: ${normUrl.args}")
println("Full spec: ${normUrl.urlSpec}")

// Check if URL is valid
if (!normUrl.isNil) {
    // URL is valid, proceed
    val page = session.load(normUrl.url, normUrl.args)
}

session.close()
```

### Normalize or Null

Handle invalid URLs gracefully:

```kotlin
val session = AgenticSession.getOrCreate()

val url = "invalid://bad-url"
val normUrl = session.normalizeOrNull(url)

if (normUrl != null) {
    val page = session.load(normUrl.url, normUrl.args)
} else {
    println("Invalid URL")
}

session.close()
```

## Navigation Patterns

### Sequential Navigation

Navigate through multiple pages in sequence:

```kotlin
val session = AgenticSession.getOrCreate()
val driver = session.driver

val urls = listOf(
    "https://example.com/page1",
    "https://example.com/page2",
    "https://example.com/page3"
)

for (url in urls) {
    driver.navigateTo(url)
    driver.waitForSelector(".content")
    
    val title = driver.title()
    println("Page: $title")
    
    // Extract data
    val content = driver.selectFirstTextOrNull(".main-content")
    println("Content: $content")
}

session.close()
```

### Pagination

Navigate through paginated content:

```kotlin
val session = AgenticSession.getOrCreate()
val driver = session.driver

driver.navigateTo("https://example.com/products")

var page = 1
while (true) {
    println("Processing page $page")
    
    // Extract items from current page
    val items = driver.selectTextAll(".product-item")
    items.forEach { println("Item: $it") }
    
    // Check if next page exists
    if (!driver.exists(".next-page")) {
        break
    }
    
    // Navigate to next page
    driver.click(".next-page")
    driver.waitForNavigation()
    driver.waitForSelector(".product-item")
    
    page++
}

session.close()
```

### Link Following

Follow links from a page:

```kotlin
val session = AgenticSession.getOrCreate()
val driver = session.driver

driver.navigateTo("https://example.com")

// Extract all links
val links = driver.selectAttributeAll("a", "href")

// Visit each link
for (link in links.take(5)) { // Limit to first 5
    if (link.startsWith("http")) {
        println("Visiting: $link")
        driver.navigateTo(link)
        driver.waitForNavigation()
        
        val title = driver.title()
        println("Title: $title")
        
        // Go back to original page
        driver.goBack()
        driver.waitForNavigation()
    }
}

session.close()
```

### Tab Simulation

Simulate opening links in new tabs:

```kotlin
val session = AgenticSession.getOrCreate()
val driver = session.driver

driver.navigateTo("https://example.com")

// Get links to visit
val links = driver.selectAttributeAll("a.external", "href")

// Store original URL
val originalUrl = driver.currentUrl()

// Visit each link
for (link in links) {
    driver.navigateTo(link)
    driver.waitForNavigation()
    
    // Process page
    val title = driver.title()
    println("Link title: $title")
    
    // Return to original page
    driver.navigateTo(originalUrl)
    driver.waitForNavigation()
}

session.close()
```

## Handling Redirects

### Detect Redirects

```kotlin
val session = AgenticSession.getOrCreate()
val driver = session.driver

val startUrl = "https://example.com/redirect"
driver.navigateTo(startUrl)
driver.waitForNavigation()

val finalUrl = driver.currentUrl()

if (finalUrl != startUrl) {
    println("Redirected from $startUrl to $finalUrl")
}

session.close()
```

### Follow Redirect Chain

```kotlin
val session = AgenticSession.getOrCreate()

val page = session.open("https://example.com/redirect")

println("Start URL: https://example.com/redirect")
println("Final URL: ${page.url}")
println("Location: ${page.location}") // Redirect location if any

session.close()
```

## Handling Dynamic Content

### Wait for AJAX

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com")

// Click button that triggers AJAX
driver.click("button.load-data")

// Wait for content to appear
driver.waitForSelector(".ajax-content")

// Extract loaded content
val content = driver.selectFirstTextOrNull(".ajax-content")
println("AJAX content: $content")
```

### Infinite Scroll

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com/infinite-scroll")

var previousHeight = 0
var currentHeight = driver.executeScript("return document.body.scrollHeight") as Int

while (currentHeight > previousHeight) {
    // Scroll to bottom
    driver.scrollToBottom()
    
    // Wait for new content
    Thread.sleep(2000)
    
    // Check new height
    previousHeight = currentHeight
    currentHeight = driver.executeScript("return document.body.scrollHeight") as Int
    
    println("Loaded more content, height: $currentHeight")
}

println("Reached end of infinite scroll")
```

## Error Handling

### Handle Navigation Errors

```kotlin
val session = AgenticSession.getOrCreate()
val driver = session.driver

try {
    driver.navigateTo("https://invalid-url-does-not-exist.com")
    driver.waitForNavigation()
} catch (e: Exception) {
    println("Navigation failed: ${e.message}")
    // Handle error (retry, skip, etc.)
}

session.close()
```

### Timeout Handling

```kotlin
val driver = session.driver

try {
    driver.navigateTo("https://very-slow-website.com")
    
    // Wait with timeout
    driver.waitForSelector(".content", timeout = 10000) // 10 seconds
    
} catch (e: Exception) {
    println("Timeout waiting for content: ${e.message}")
    // Continue with alternative action
}
```

### Retry Logic

```kotlin
fun navigateWithRetry(
    driver: WebDriver,
    url: String,
    maxRetries: Int = 3
): Boolean {
    repeat(maxRetries) { attempt ->
        try {
            driver.navigateTo(url)
            driver.waitForNavigation()
            return true
        } catch (e: Exception) {
            println("Attempt ${attempt + 1} failed: ${e.message}")
            if (attempt == maxRetries - 1) {
                println("Max retries reached")
                return false
            }
            Thread.sleep(1000 * (attempt + 1)) // Exponential backoff
        }
    }
    return false
}

// Usage
val session = AgenticSession.getOrCreate()
val success = navigateWithRetry(session.driver, "https://example.com")
if (success) {
    println("Navigation successful")
}
session.close()
```

## Performance Optimization

### Preload Pages

```kotlin
val session = AgenticSession.getOrCreate()

// Submit URLs for background processing
val urls = listOf(
    "https://example.com/page1",
    "https://example.com/page2",
    "https://example.com/page3"
)

// Submit all URLs
urls.forEach { url ->
    session.submit(url, "-expire 1h")
}

// Later, load from cache (fast)
urls.forEach { url ->
    val page = session.load(url)
    println("Loaded from cache: ${page.url}")
}

session.close()
```

### Batch Loading

```kotlin
val session = AgenticSession.getOrCreate()

val urls = listOf(
    "https://example.com/product/1",
    "https://example.com/product/2",
    "https://example.com/product/3"
)

// Load all pages efficiently
val pages = urls.map { url ->
    session.load(url, "-expire 1d")
}

// Process loaded pages
pages.forEach { page ->
    val document = session.parse(page)
    val title = document.select("h1").text()
    println("Title: $title")
}

session.close()
```

## Best Practices

1. **Always wait for navigation** - Use `waitForNavigation()` after actions that trigger navigation
2. **Wait for key elements** - Use `waitForSelector()` to ensure content is loaded
3. **Handle timeouts gracefully** - Implement timeout and retry logic
4. **Use appropriate load methods** - Choose `open()`, `load()`, or `submit()` based on needs
5. **Check element existence** - Use `exists()` before interacting with optional elements
6. **Normalize URLs** - Use `normalize()` to validate and clean URLs
7. **Monitor navigation history** - Track navigation for debugging and validation
8. **Handle redirects** - Check if final URL matches expected URL
9. **Implement retry logic** - Handle transient network errors
10. **Optimize for performance** - Use caching and batch loading when appropriate

## Next Steps

- **[Element Interaction](element-interaction.md)** - Learn to interact with page elements
- **[Data Extraction](data-extraction.md)** - Extract structured data from pages
- **[Screenshots](screenshots.md)** - Capture visual state
- **[Script Execution](script-execution.md)** - Execute custom JavaScript
