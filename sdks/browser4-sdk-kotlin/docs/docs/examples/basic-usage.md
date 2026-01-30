# Basic Usage Examples

Practical examples for common Browser4 Kotlin SDK usage patterns.

## Simple Page Loading

```kotlin
import ai.platon.pulsar.sdk.v0.*

fun main() {
    val session = AgenticSession.getOrCreate()
    
    val page = session.open("https://example.com")
    val document = session.parse(page)
    val title = document.title()
    
    println("Page title: $title")
    
    session.close()
}
```

## Extract Data with CSS Selectors

```kotlin
val session = AgenticSession.getOrCreate()

val fields = session.scrape(
    url = "https://example.com/product",
    args = "-expire 1d",
    selectors = mapOf(
        "title" to "h1.product-name",
        "price" to ".price",
        "description" to ".description",
        "availability" to ".stock-status"
    )
)

println("Product: ${fields.fields["title"]}")
println("Price: ${fields.fields["price"]}")

session.close()
```

## Navigate and Click

```kotlin
val session = AgenticSession.getOrCreate()
val driver = session.driver

driver.navigateTo("https://example.com")
driver.waitForSelector(".content")

driver.click("button.show-more")
driver.waitForSelector(".additional-content")

val content = driver.selectTextAll(".item")
content.forEach { println("Item: $it") }

session.close()
```

## Fill and Submit Form

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com/contact")

driver.fill("input[name='name']", "John Doe")
driver.fill("input[name='email']", "john@example.com")
driver.fill("textarea[name='message']", "Hello!")
driver.check("input[type='checkbox']#terms")
driver.click("button[type='submit']")

driver.waitForSelector(".success-message")
println("Form submitted!")

session.close()
```

## Screenshot Capture

```kotlin
import java.io.File
import java.util.Base64

val driver = session.driver
driver.navigateTo("https://example.com")

val screenshot = driver.captureScreenshot(fullPage = true)
if (screenshot != null) {
    val bytes = Base64.getDecoder().decode(screenshot)
    File("page.png").writeBytes(bytes)
    println("Screenshot saved")
}
```

## Extract from Table

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com/table")

val data = driver.executeScript("""
    const rows = Array.from(document.querySelectorAll('tbody tr'));
    return rows.map(row => ({
        name: row.cells[0]?.textContent?.trim(),
        value: row.cells[1]?.textContent?.trim()
    }));
""") as List<Map<String, Any>>

data.forEach { println("Row: $it") }
```

## Pagination

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com/products")

val allItems = mutableListOf<String>()
var page = 1

while (page <= 5) {
    val items = driver.selectTextAll(".product-name")
    allItems.addAll(items)
    
    if (!driver.exists(".next-page:not(.disabled)")) break
    
    driver.click(".next-page")
    driver.waitForNavigation()
    page++
}

println("Collected ${allItems.size} items")
```

See also:
- [Advanced Usage](advanced-usage.md)
- [AI Automation Examples](ai-automation.md)
- [Complete Workflow](complete-workflow.md)
