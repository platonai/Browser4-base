# Data Extraction

Comprehensive guide to extracting structured data from web pages using Browser4 Kotlin SDK.

## Overview

Browser4 provides three main approaches for data extraction:
1. **CSS Selector Extraction** - Fast, deterministic
2. **WebDriver Extraction** - Direct browser DOM access
3. **AI-Powered Extraction** - Intelligent, adaptive

## CSS Selector Extraction

### Basic Extraction

```kotlin
val session = AgenticSession.getOrCreate()
val page = session.open("https://example.com")
val document = session.parse(page)

// Extract multiple fields
val fields = session.extract(document, mapOf(
    "title" to "h1",
    "description" to "p.description",
    "price" to ".price"
))

println("Title: ${"$"}{fields["title"]}")
session.close()
```

### One-Step Scraping

```kotlin
val fields = session.scrape(
    url = "https://example.com/product",
    args = "-expire 1d",
    selectors = mapOf(
        "title" to "h1.product-title",
        "price" to ".price-current",
        "features" to ".feature-list li"
    )
)
```

## WebDriver Extraction

### Extract Text

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com")

// Single element
val title = driver.selectFirstTextOrNull("h1")

// All matching elements
val paragraphs = driver.selectTextAll("p")
```

### Extract Attributes

```kotlin
// Single attribute
val imageUrl = driver.selectFirstAttributeOrNull("img", "src")

// All attributes
val links = driver.selectAttributeAll("a", "href")
```

### Batch Extraction

```kotlin
val fields = driver.extract(mapOf(
    "title" to "h1",
    "price" to ".price",
    "description" to ".description"
))
```

## AI-Powered Extraction

### Intelligent Extraction

```kotlin
val agent = session.companionAgent
session.driver.navigateTo("https://example.com/product")

val result = agent.extract(
    instruction = "Extract product name, price, and description"
)

if (result.success) {
    println("Data: ${"$"}{result.data}")
}
```

### Schema-Based Extraction

```kotlin
val schema = mapOf(
    "type" to "object",
    "properties" to mapOf(
        "name" to mapOf("type" to "string"),
        "price" to mapOf("type" to "number"),
        "features" to mapOf(
            "type" to "array",
            "items" to mapOf("type" to "string")
        )
    )
)

val result = agent.extract(
    instruction = "Extract product information",
    schema = schema
)
```

## Advanced Patterns

### Table Extraction

```kotlin
val tableData = driver.executeScript("""
    const table = document.querySelector('table');
    const headers = Array.from(table.querySelectorAll('th')).map(th => th.textContent.trim());
    const rows = Array.from(table.querySelectorAll('tbody tr')).map(row => {
        const cells = Array.from(row.querySelectorAll('td')).map(td => td.textContent.trim());
        return Object.fromEntries(headers.map((h, i) => [h, cells[i]]));
    });
    return rows;
""") as List<Map<String, Any>>
```

### Pagination Extraction

```kotlin
val allProducts = mutableListOf<Map<String, Any?>>()

while (true) {
    val products = driver.executeScript("""
        return Array.from(document.querySelectorAll('.product')).map(p => ({
            name: p.querySelector('.name')?.textContent?.trim(),
            price: p.querySelector('.price')?.textContent?.trim()
        }));
    """) as List<Map<String, Any?>>
    
    allProducts.addAll(products)
    
    if (!driver.exists(".next-page:not(.disabled)")) break
    
    driver.click(".next-page")
    driver.waitForNavigation()
}
```

## Best Practices

1. **Use specific selectors** - Prefer IDs and stable classes
2. **Handle missing data** - Use safe extraction methods with null checks
3. **Wait for content** - Ensure dynamic content loads before extraction
4. **Batch operations** - Extract multiple fields in one call
5. **Validate data** - Check required fields exist and have correct format
6. **Cache documents** - Reuse parsed documents when possible
7. **Clean data** - Trim whitespace and normalize text

## Next Steps

- [AI Automation](ai-automation.md) - Intelligent extraction
- [Screenshots](screenshots.md) - Visual verification
- [Examples](../examples/basic-usage.md) - More examples
