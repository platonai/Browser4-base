# Advanced Usage Examples

Advanced patterns and techniques for Browser4 Kotlin SDK.

## Parallel Scraping

```kotlin
import kotlinx.coroutines.*
import ai.platon.pulsar.sdk.v0.*
import ai.platon.pulsar.sdk.v0.detail.PulsarClient

fun main() = runBlocking {
    val baseUrl = "http://localhost:8182"
    
    val urls = listOf(
        "https://example.com/product/1",
        "https://example.com/product/2",
        "https://example.com/product/3"
    )
    
    val results = urls.map { url ->
        async {
            val client = PulsarClient(baseUrl = baseUrl, useLocalDriver = false)
            client.createSession()
            val session = AgenticSession(client)
            
            try {
                val page = session.load(url)
                val doc = session.parse(page)
                val fields = session.extract(doc, mapOf("title" to "h1"))
                fields.fields["title"]
            } finally {
                session.close()
                client.close()
            }
        }
    }.awaitAll()
    
    results.forEach { println("Product: $it") }
}
```

## Custom Data Extraction

```kotlin
data class Product(
    val name: String,
    val price: Double,
    val rating: Double,
    val reviews: Int
)

fun extractProduct(driver: WebDriver): Product {
    val name = driver.selectFirstTextOrNull("h1.product-name") ?: ""
    
    val priceText = driver.selectFirstTextOrNull(".price") ?: "0"
    val price = priceText.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
    
    val ratingText = driver.selectFirstTextOrNull(".rating-value") ?: "0"
    val rating = ratingText.toDoubleOrNull() ?: 0.0
    
    val reviewText = driver.selectFirstTextOrNull(".review-count") ?: "0"
    val reviews = reviewText.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
    
    return Product(name, price, rating, reviews)
}

// Usage
val driver = session.driver
driver.navigateTo("https://example.com/product")
val product = extractProduct(driver)
println("Product: $product")
```

## Dynamic Content Handling

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com")

// Infinite scroll
var previousCount = 0
repeat(10) {
    driver.scrollToBottom()
    Thread.sleep(1000)
    
    val items = driver.selectTextAll(".item")
    if (items.size == previousCount) return@repeat
    previousCount = items.size
}

// AJAX loading
driver.click("button.load-more")
driver.waitForSelector(".new-content")
val newItems = driver.selectTextAll(".new-content .item")
```

## Error Recovery

```kotlin
fun safeNavigate(driver: WebDriver, url: String, maxRetries: Int = 3): Boolean {
    repeat(maxRetries) { attempt ->
        try {
            driver.navigateTo(url)
            driver.waitForNavigation()
            driver.waitForSelector("body", timeout = 5000)
            return true
        } catch (e: Exception) {
            println("Attempt ${attempt + 1} failed: ${e.message}")
            if (attempt < maxRetries - 1) {
                Thread.sleep(1000 * (attempt + 1))
            }
        }
    }
    return false
}
```

## Session Pool

```kotlin
class SessionPool(val size: Int = 3) {
    private val sessions = mutableListOf<AgenticSession>()
    private val available = java.util.concurrent.ConcurrentLinkedQueue<AgenticSession>()
    
    suspend fun init() {
        repeat(size) {
            val client = PulsarClient(baseUrl = "http://localhost:8182", useLocalDriver = false)
            client.createSession()
            val session = AgenticSession(client)
            sessions.add(session)
            available.offer(session)
        }
    }
    
    fun acquire(): AgenticSession? = available.poll()
    fun release(session: AgenticSession) { available.offer(session) }
    
    fun close() {
        sessions.forEach { it.close() }
    }
}
```

## Complex JavaScript Extraction

```kotlin
val productData = driver.executeScript("""
    function extractProduct(element) {
        return {
            id: element.dataset.id,
            name: element.querySelector('.name')?.textContent?.trim(),
            price: parseFloat(element.querySelector('.price')?.textContent?.replace(/[^0-9.]/g, '')),
            rating: {
                score: parseFloat(element.querySelector('.rating-score')?.textContent),
                count: parseInt(element.querySelector('.rating-count')?.textContent?.match(/\d+/)?.[0])
            },
            variants: Array.from(element.querySelectorAll('.variant')).map(v => ({
                size: v.dataset.size,
                color: v.dataset.color,
                stock: v.classList.contains('in-stock')
            }))
        };
    }
    
    return Array.from(document.querySelectorAll('.product-card')).map(extractProduct);
""") as List<Map<String, Any>>
```

See also:
- [Basic Usage](basic-usage.md)
- [AI Automation Examples](ai-automation.md)
