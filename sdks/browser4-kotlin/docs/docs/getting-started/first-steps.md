# First Steps

This tutorial walks you through the core concepts of the Browser4 Kotlin SDK step by step.

## Understanding the Architecture

The Browser4 SDK has three main layers:

1. **PulsarClient** - Low-level HTTP client for API communication
2. **PulsarSession** - Session management for page loading and extraction
3. **AgenticSession** - AI-powered automation extending PulsarSession
4. **WebDriver** - Browser control and element interaction

```
AgenticSession
    ↓ extends
PulsarSession
    ↓ uses
PulsarClient
    ↓ communicates with
Browser4 Server
```

## Step 1: Creating a Session

The session is your main interface to the Browser4 engine. There are two ways to create a session:

### Option A: Using the Default Session (Recommended)

The simplest approach uses `getOrCreate()`, which automatically handles driver setup:

```kotlin
import ai.platon.pulsar.sdk.v0.*

fun main() {
    // Automatically starts local driver and creates session
    val session = AgenticSession.getOrCreate()
    
    // Your automation code here
    
    // Clean up
    session.close()
}
```

**What happens:**
- Downloads Browser4.jar if not already present (stored in `~/.browser4/`)
- Starts the Browser4 server on port 8182
- Creates a session and returns it ready to use

### Option B: Explicit Client Creation

For more control over configuration:

```kotlin
import ai.platon.pulsar.sdk.v0.*
import ai.platon.pulsar.sdk.v0.detail.PulsarClient

fun main() {
    // Create client with custom options
    val client = PulsarClient(
        baseUrl = "http://localhost:8182",
        useLocalDriver = true
    )
    
    // Create session
    client.createSession()
    val session = AgenticSession(client)
    
    // Your automation code here
    
    // Clean up
    session.close()
    client.close()
}
```

## Step 2: Loading Pages

Browser4 provides three methods for loading pages, each optimized for different scenarios:

### 2.1 Open - Fresh Load

`open()` always fetches the page from the internet, bypassing any cached content:

```kotlin
val session = AgenticSession.getOrCreate()

// Fresh load, bypasses cache
val page = session.open("https://example.com")
println("URL: ${page.url}")
println("Content Type: ${page.contentType}")
println("Content Length: ${page.contentLength}")

session.close()
```

**Use `open()` when:**
- You need the absolute latest content
- You're testing dynamic content
- Cache freshness is critical

### 2.2 Load - Smart Caching

`load()` checks the cache first, only fetching from the internet if necessary:

```kotlin
val session = AgenticSession.getOrCreate()

// Load with caching (faster for repeated access)
val page = session.load("https://example.com", "-expire 1d")
println("Loaded from ${if (page.location != null) "cache" else "internet"}")

session.close()
```

**Load Arguments:**
- `-expire 1d` - Page expires after 1 day
- `-expire 1h` - Expires after 1 hour
- `-refresh` - Force refresh even if cached
- `-parse` - Activate parsing subsystem

**Use `load()` when:**
- Content doesn't change frequently
- Performance is important
- You're crawling many pages

### 2.3 Submit - Background Processing

`submit()` adds the URL to a crawl queue for asynchronous processing:

```kotlin
val session = AgenticSession.getOrCreate()

// Submit for background processing
val page = session.submit("https://example.com", "-expire 1d")
println("Submitted: ${page.url}")

// Later, retrieve the result
val loaded = session.load("https://example.com")

session.close()
```

**Use `submit()` when:**
- Processing many URLs in bulk
- You don't need immediate results
- You want to parallelize crawling

## Step 3: Navigating with WebDriver

Once you have a session, use WebDriver for browser control:

```kotlin
val session = AgenticSession.getOrCreate()
val driver = session.driver

// Navigate to a page
driver.navigateTo("https://example.com")
println("Current URL: ${driver.currentUrl()}")
println("Page title: ${driver.title()}")

// Navigate back and forward
driver.goBack()
driver.goForward()

// Reload the page
driver.reload()

session.close()
```

## Step 4: Parsing HTML

After loading a page, parse it into a structured document:

```kotlin
val session = AgenticSession.getOrCreate()

// Load and parse
val page = session.open("https://example.com")
val document = session.parse(page)

// Now you can query the document
println("Title: ${document.title()}")
println("Body text: ${document.body()?.text()}")

session.close()
```

You can also parse HTML strings directly:

```kotlin
val html = """
    <html>
        <body>
            <h1>Hello</h1>
            <p>World</p>
        </body>
    </html>
"""
val document = session.parse(html)
println(document.select("h1").text())
```

## Step 5: Extracting Data

Extract data from documents using CSS selectors:

### Simple Extraction

```kotlin
val session = AgenticSession.getOrCreate()
val page = session.open("https://example.com")
val document = session.parse(page)

// Extract multiple fields at once
val fields = session.extract(document, mapOf(
    "title" to "h1",
    "description" to "p",
    "links" to "a"
))

println("Title: ${fields["title"]}")
println("Description: ${fields["description"]}")
println("Links: ${fields["links"]}")

session.close()
```

### Direct WebDriver Extraction

```kotlin
val session = AgenticSession.getOrCreate()
val driver = session.driver

driver.navigateTo("https://example.com")

// Extract text from first matching element
val title = driver.selectFirstTextOrNull("h1")
println("Title: $title")

// Extract text from all matching elements
val paragraphs = driver.selectTextAll("p")
paragraphs.forEach { println("Paragraph: $it") }

// Extract attributes
val links = driver.selectAttributeAll("a", "href")
links.forEach { println("Link: $it") }

session.close()
```

### One-Step Scraping

Combine load, parse, and extract in a single call:

```kotlin
val session = AgenticSession.getOrCreate()

val fields = session.scrape(
    url = "https://example.com",
    args = "-expire 1d",
    selectors = mapOf(
        "title" to "h1",
        "content" to ".main-content"
    )
)

println("Scraped data: $fields")

session.close()
```

## Step 6: Interacting with Elements

Use WebDriver to interact with page elements:

### Clicking Elements

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com")

// Click using CSS selector
driver.click("button.submit")

// Click using XPath
driver.click("//button[@type='submit']", strategy = "xpath")
```

### Filling Forms

```kotlin
// Fill input field
driver.fill("input[name='username']", "john_doe")
driver.fill("input[name='password']", "secret123")

// Type text (simulates keyboard)
driver.type("textarea.comment", "This is a comment")

// Press keys
driver.press("input[name='search']", "Enter")
```

### Checking Boxes and Radio Buttons

```kotlin
// Check a checkbox
driver.check("input[type='checkbox']#terms")

// Uncheck a checkbox
driver.uncheck("input[type='checkbox']#newsletter")
```

### Hovering and Focusing

```kotlin
// Hover over element (shows tooltips, dropdowns, etc.)
driver.hover(".menu-item")

// Focus on element
driver.focus("input[name='email']")
```

## Step 7: Waiting for Elements

Always wait for elements to appear before interacting:

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com")

// Wait for element to appear (default 30s timeout)
driver.waitForSelector(".dynamic-content")

// Wait with custom timeout
driver.waitForSelector(".slow-loading", timeout = 60000) // 60 seconds

// Wait for navigation to complete
driver.waitForNavigation()

// Check if element exists
if (driver.exists(".error-message")) {
    println("Error occurred!")
}

// Check visibility
if (driver.isVisible(".modal")) {
    println("Modal is visible")
}
```

## Step 8: Scrolling

Control page scrolling for loading dynamic content:

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com")

// Scroll down multiple times (loads infinite scroll content)
driver.scrollDown(count = 3)

// Scroll to specific element
driver.scrollTo(".footer")

// Scroll to bottom of page
driver.scrollToBottom()

// Scroll to top
driver.scrollToTop()

// Scroll to middle
driver.scrollToMiddle(ratio = 0.5) // 50% down the page
```

## Step 9: Taking Screenshots

Capture visual state of the page:

```kotlin
import java.io.File
import java.util.Base64

val driver = session.driver
driver.navigateTo("https://example.com")

// Capture viewport screenshot
val screenshot = driver.captureScreenshot()

// Capture full-page screenshot
val fullScreenshot = driver.captureScreenshot(fullPage = true)

// Capture specific element
val elementScreenshot = driver.captureScreenshot(selector = ".header")

// Save to file
if (fullScreenshot != null) {
    val bytes = Base64.getDecoder().decode(fullScreenshot)
    File("page.png").writeBytes(bytes)
    println("Screenshot saved!")
}
```

## Step 10: Executing JavaScript

Run custom JavaScript in the browser context:

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com")

// Execute JavaScript and get result
val linkCount = driver.executeScript(
    "return document.querySelectorAll('a').length"
)
println("Number of links: $linkCount")

// Execute script with arguments
val result = driver.executeScript(
    "return arguments[0] + arguments[1]",
    listOf(10, 20)
)
println("Result: $result") // 30

// Evaluate expressions
val title = driver.evaluate("document.title")
println("Page title: $title")
```

## Step 11: AI-Powered Automation

Use natural language to automate browser tasks:

```kotlin
val session = AgenticSession.getOrCreate()
val agent = session.companionAgent
val driver = session.driver

driver.navigateTo("https://example.com")

// Execute single action
val actResult = agent.act("click the 'More information' link")
if (actResult.success) {
    println("Action completed: ${actResult.message}")
}

// Run autonomous task
val runResult = agent.run("find the contact email address and copy it")
if (runResult.success) {
    println("Task completed: ${runResult.message}")
    println("Result: ${runResult.finalResult}")
}

// Observe page and get suggestions
val observation = agent.observe("what actions can I take on this page?")
observation.observations.forEach { obs ->
    println("Found: ${obs.description}")
    println("Suggestion: ${obs.nextSuggestions?.joinToString()}")
}

session.close()
```

## Complete Example

Here's a complete example combining everything:

```kotlin
import ai.platon.pulsar.sdk.v0.*
import java.io.File
import java.util.Base64

fun main() {
    // Create session
    val session = AgenticSession.getOrCreate()
    val driver = session.driver
    val agent = session.companionAgent
    
    try {
        // Navigate to page
        println("Navigating to example.com...")
        driver.navigateTo("https://example.com")
        
        // Wait for content
        driver.waitForSelector("h1")
        
        // Extract data
        val title = driver.selectFirstTextOrNull("h1")
        val description = driver.selectFirstTextOrNull("p")
        println("Title: $title")
        println("Description: $description")
        
        // Use AI to interact
        val result = agent.act("click the 'More information' link")
        println("AI Action: ${result.message}")
        
        // Capture screenshot
        val screenshot = driver.captureScreenshot(fullPage = true)
        if (screenshot != null) {
            val bytes = Base64.getDecoder().decode(screenshot)
            File("example.png").writeBytes(bytes)
            println("Screenshot saved!")
        }
        
        println("Automation complete!")
        
    } finally {
        // Always clean up
        session.close()
    }
}
```

## Best Practices

1. **Always close sessions** - Use `try-finally` or `use` blocks to ensure cleanup
2. **Wait for elements** - Always use `waitForSelector()` before interacting with dynamic content
3. **Handle errors gracefully** - Wrap operations in try-catch blocks
4. **Reuse sessions** - Create one session and reuse it for multiple operations
5. **Use appropriate load methods** - Choose `open()`, `load()`, or `submit()` based on your needs

## Next Steps

Now that you understand the basics, dive deeper into specific topics:

- **[Session Management](../guide/session-management.md)** - Advanced session patterns
- **[Navigation](../guide/navigation.md)** - Complex navigation scenarios
- **[Element Interaction](../guide/element-interaction.md)** - Advanced interaction techniques
- **[Data Extraction](../guide/data-extraction.md)** - Complex extraction patterns
- **[AI Automation](../guide/ai-automation.md)** - Deep dive into AI features
