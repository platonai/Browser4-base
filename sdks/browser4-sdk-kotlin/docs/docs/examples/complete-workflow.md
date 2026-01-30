# Complete Workflow Example

End-to-end example of a complete web scraping workflow.

## E-commerce Product Scraper

```kotlin
import ai.platon.pulsar.sdk.v0.*
import java.io.File
import java.util.Base64
import kotlinx.coroutines.runBlocking

data class Product(
    val name: String,
    val price: Double,
    val rating: Double,
    val reviewCount: Int,
    val imageUrl: String,
    val availability: Boolean
)

fun main() = runBlocking {
    val session = AgenticSession.getOrCreate()
    val driver = session.driver
    
    try {
        println("Starting product scraper...")
        
        // Navigate to search page
        driver.navigateTo("https://example.com/products")
        driver.waitForSelector(".product-list")
        
        // Apply filters
        println("Applying filters...")
        driver.click(".filter-category")
        driver.click("option[value='electronics']")
        driver.click(".filter-price")
        driver.click("option[value='under-100']")
        driver.click("button.apply-filters")
        driver.waitForNavigation()
        
        // Scrape all pages
        val allProducts = mutableListOf<Product>()
        var currentPage = 1
        
        while (currentPage <= 5) {
            println("Scraping page $currentPage...")
            
            // Extract products from current page
            val pageProducts = extractProducts(driver)
            allProducts.addAll(pageProducts)
            
            println("  Found ${pageProducts.size} products")
            
            // Check for next page
            if (!driver.exists(".pagination .next:not(.disabled)")) {
                break
            }
            
            // Go to next page
            driver.click(".pagination .next")
            driver.waitForNavigation()
            driver.waitForSelector(".product-list")
            currentPage++
        }
        
        // Visit each product for detailed info
        println("\nGathering detailed information...")
        val detailedProducts = allProducts.take(10).map { product ->
            getProductDetails(driver, product)
        }
        
        // Save results
        saveResults(detailedProducts)
        
        // Generate report
        generateReport(detailedProducts)
        
        println("\nScraping complete!")
        println("Total products: ${detailedProducts.size}")
        
    } finally {
        session.close()
    }
}

fun extractProducts(driver: WebDriver): List<Product> {
    val products = driver.executeScript("""
        return Array.from(document.querySelectorAll('.product-item')).map(item => ({
            name: item.querySelector('.product-name')?.textContent?.trim() || '',
            price: parseFloat(item.querySelector('.price')?.textContent?.replace(/[^0-9.]/g, '') || '0'),
            rating: parseFloat(item.querySelector('.rating')?.textContent || '0'),
            reviewCount: parseInt(item.querySelector('.review-count')?.textContent?.match(/\d+/)?.[0] || '0'),
            imageUrl: item.querySelector('img')?.src || '',
            availability: item.querySelector('.in-stock') !== null,
            detailUrl: item.querySelector('a')?.href || ''
        }));
    """) as List<Map<String, Any>>
    
    return products.map { data ->
        Product(
            name = data["name"] as String,
            price = (data["price"] as? Number)?.toDouble() ?: 0.0,
            rating = (data["rating"] as? Number)?.toDouble() ?: 0.0,
            reviewCount = (data["reviewCount"] as? Number)?.toInt() ?: 0,
            imageUrl = data["imageUrl"] as String,
            availability = data["availability"] as Boolean
        )
    }
}

fun getProductDetails(driver: WebDriver, product: Product): Product {
    // Navigate to detail page
    driver.navigateTo("https://example.com/product/${product.name.hashCode()}")
    driver.waitForSelector(".product-detail")
    
    // Extract additional details
    val description = driver.selectFirstTextOrNull(".description") ?: ""
    val features = driver.selectTextAll(".features li")
    
    // Capture screenshot
    val screenshot = driver.captureScreenshot(selector = ".product-image")
    if (screenshot != null) {
        val bytes = Base64.getDecoder().decode(screenshot)
        File("screenshots/${product.name.hashCode()}.png").apply {
            parentFile.mkdirs()
            writeBytes(bytes)
        }
    }
    
    return product // Return with additional info if needed
}

fun saveResults(products: List<Product>) {
    val csv = buildString {
        appendLine("Name,Price,Rating,Reviews,Available")
        products.forEach { p ->
            appendLine("${p.name},${p.price},${p.rating},${p.reviewCount},${p.availability}")
        }
    }
    File("products.csv").writeText(csv)
    println("Results saved to products.csv")
}

fun generateReport(products: List<Product>) {
    val avgPrice = products.map { it.price }.average()
    val avgRating = products.map { it.rating }.average()
    val available = products.count { it.availability }
    
    println("\n=== Report ===")
    println("Total products: ${products.size}")
    println("Average price: $${"%.2f".format(avgPrice)}")
    println("Average rating: ${"%.1f".format(avgRating)}")
    println("Available: $available / ${products.size}")
}
```

## AI-Powered Workflow

```kotlin
fun runAiWorkflow() = runBlocking {
    val session = AgenticSession.getOrCreate()
    val agent = session.companionAgent
    val driver = session.driver
    
    try {
        // Let AI handle the workflow
        driver.navigateTo("https://example.com")
        
        val result = agent.run("""
            Navigate to the products page, 
            apply filters for electronics under $100,
            extract product names, prices, and ratings from the first 3 pages,
            and save the results
        """)
        
        if (result.success) {
            println("AI workflow completed!")
            println("Result: ${result.finalResult}")
        }
    } finally {
        session.close()
    }
}
```

See also:
- [Basic Usage](basic-usage.md)
- [Advanced Usage](advanced-usage.md)
- [AI Automation](ai-automation.md)
