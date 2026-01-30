# PulsarSession API

Session management for page loading and extraction.

## Constructor

```kotlin
open class PulsarSession(
    val client: PulsarClient
) : AutoCloseable
```

## Properties

### id

```kotlin
val id: Int
```

Numeric session ID.

### uuid

```kotlin
val uuid: String
```

Session UUID string.

### isActive

```kotlin
val isActive: Boolean
```

Whether session is active.

### driver

```kotlin
val driver: WebDriver
```

Bound WebDriver instance (created on first access).

## URL Normalization

### normalize()

```kotlin
suspend fun normalize(
    url: String,
    args: String? = null,
    toItemOption: Boolean = false
): NormURL
```

Normalize URL with load arguments.

**Returns:** `NormURL` with normalized URL and args

**Example:**
```kotlin
val normUrl = session.normalize(
    "https://example.com",
    "-expire 1d"
)
println("URL: ${normUrl.url}")
println("Args: ${normUrl.args}")
```

### normalizeOrNull()

```kotlin
suspend fun normalizeOrNull(
    url: String?,
    args: String? = null,
    toItemOption: Boolean = false
): NormURL?
```

Normalize URL, return null if invalid.

## Page Loading

### open()

```kotlin
suspend fun open(
    url: String,
    args: String? = null
): WebPage
```

Open URL immediately (bypass cache).

**Returns:** `WebPage` result

**Example:**
```kotlin
val page = session.open("https://example.com")
println("Loaded: ${page.url}")
println("Content type: ${page.contentType}")
```

### load()

```kotlin
suspend fun load(
    url: String,
    args: String? = null
): WebPage
```

Load from cache or fetch.

**Example:**
```kotlin
val page = session.load(
    "https://example.com",
    "-expire 1d -parse"
)
```

### submit()

```kotlin
suspend fun submit(
    url: String,
    args: String? = null
): WebPage
```

Submit URL to crawl pool for async processing.

## HTML Parsing

### parse()

```kotlin
fun parse(page: WebPage): Document
fun parse(html: String, baseUri: String = ""): Document
fun parse(snapshot: PageSnapshot): Document
```

Parse HTML into Jsoup document.

**Example:**
```kotlin
val page = session.open("https://example.com")
val document = session.parse(page)
val title = document.title()
```

## Data Extraction

### extract()

```kotlin
fun extract(
    document: Document,
    selectors: Map<String, String>
): FieldsExtraction
```

Extract fields using CSS selectors.

**Parameters:**
- `document`: Parsed HTML document
- `selectors`: Map of field name to CSS selector

**Returns:** `FieldsExtraction` with extracted fields

**Example:**
```kotlin
val fields = session.extract(document, mapOf(
    "title" to "h1",
    "description" to "p.description",
    "price" to ".price",
    "links" to "a"  // Extracts all matching
))

println("Title: ${fields.fields["title"]}")
```

### scrape()

```kotlin
suspend fun scrape(
    url: String,
    args: String? = null,
    selectors: Map<String, String>
): FieldsExtraction
```

One-step load, parse, and extract.

**Example:**
```kotlin
val fields = session.scrape(
    url = "https://example.com/product",
    args = "-expire 1h",
    selectors = mapOf(
        "title" to "h1.product-name",
        "price" to ".price-current"
    )
)
```

## Page Capture

### capture()

```kotlin
suspend fun capture(driver: WebDriver): PageSnapshot
```

Capture current browser state.

**Example:**
```kotlin
driver.navigateTo("https://example.com")
driver.click("button.load-more")
val snapshot = session.capture(driver)
val document = session.parse(snapshot)
```

## Event Handling

### pageEventHandlers

```kotlin
val pageEventHandlers: PageEventHandlers
```

Register handlers for page lifecycle events.

**Example:**
```kotlin
session.pageEventHandlers.load.on("onLoaded") { event ->
    println("Page loaded: ${event.data}")
}
```

## Bound Driver Management

### getOrCreateBoundDriver()

```kotlin
fun getOrCreateBoundDriver(): WebDriver
```

Get or create bound WebDriver.

### boundDriver

```kotlin
val boundDriver: WebDriver?
```

Get bound driver (null if not created).

## Cleanup

### close()

```kotlin
override fun close()
```

Close session and release resources.

**Example:**
```kotlin
val session = PulsarSession(client)
try {
    // Use session
} finally {
    session.close()
}
```

## Best Practices

1. **Choose right method** - `open` for fresh, `load` for cached
2. **Parse once** - Reuse parsed documents
3. **Batch extraction** - Extract multiple fields at once
4. **Handle nil pages** - Check `page.isNil`
5. **Close sessions** - Always close when done

## Next Steps

- [AgenticSession API](agentic-session.md) - AI-powered session
- [WebDriver API](webdriver.md) - Browser control
- [Data Models](models.md) - Return types
