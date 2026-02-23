# Script Execution

Complete guide to executing JavaScript in the browser with Browser4 Kotlin SDK.

## Overview

Browser4 allows you to execute custom JavaScript in the browser context, enabling advanced automation, data extraction, and page manipulation beyond what CSS selectors can achieve.

## Basic Execution

### Execute Script

```kotlin
import ai.platon.pulsar.sdk.v0.*

fun main() {
    val session = AgenticSession.getOrCreate()
    val driver = session.driver
    
    driver.navigateTo("https://example.com")
    
    // Execute JavaScript
    val result = driver.executeScript("return document.title")
    println("Title: ${"$"}result")
    
    session.close()
}
```

### Execute with Return Value

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com")

// Return primitive values
val linkCount = driver.executeScript(
    "return document.querySelectorAll('a').length"
) as Int
println("Links: ${"$"}linkCount")

// Return objects
val pageInfo = driver.executeScript("""
    return {
        title: document.title,
        url: document.URL,
        links: document.querySelectorAll('a').length
    }
""") as Map<*, *>
println("Page info: ${"$"}pageInfo")
```

## Script with Arguments

### Pass Arguments

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com")

// Script with arguments
val result = driver.executeScript(
    "return arguments[0] + arguments[1]",
    listOf(10, 20)
)
println("Sum: ${"$"}result") // 30

// Multiple arguments
val greeting = driver.executeScript(
    "return 'Hello ' + arguments[0] + ', age ' + arguments[1]",
    listOf("John", 30)
)
println(greeting) // "Hello John, age 30"
```

### Element Manipulation

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com")

// Set element value
driver.executeScript(
    """
    const input = document.querySelector(arguments[0]);
    if (input) input.value = arguments[1];
    """,
    listOf("input[name='search']", "kotlin")
)

// Get element property
val value = driver.executeScript(
    """
    const input = document.querySelector(arguments[0]);
    return input ? input.value : null;
    """,
    listOf("input[name='search']")
)
```

## Evaluate Expressions

### Simple Evaluation

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com")

// Evaluate expression (simpler syntax)
val title = driver.evaluate("document.title") as String
val url = driver.evaluate("document.URL") as String
val height = driver.evaluate("document.body.scrollHeight") as Int

println("Title: ${"$"}title")
println("URL: ${"$"}url")
println("Height: ${"$"}height")
```

## Common Script Patterns

### DOM Manipulation

```kotlin
// Add class
driver.executeScript(
    "document.querySelector('.menu').classList.add('active')"
)

// Remove element
driver.executeScript(
    "document.querySelector('.ad-banner')?.remove()"
)

// Modify style
driver.executeScript(
    """
    const el = document.querySelector('.element');
    if (el) {
        el.style.display = 'block';
        el.style.backgroundColor = 'yellow';
    }
    """
)
```

### Extract Complex Data

```kotlin
// Extract nested data structures
val products = driver.executeScript("""
    return Array.from(document.querySelectorAll('.product')).map(p => ({
        id: p.dataset.id,
        name: p.querySelector('.name')?.textContent?.trim(),
        price: parseFloat(p.querySelector('.price')?.textContent?.replace(/[^0-9.]/g, '')),
        image: p.querySelector('img')?.src,
        inStock: p.querySelector('.stock')?.textContent?.includes('In Stock'),
        rating: {
            score: parseFloat(p.querySelector('.rating')?.textContent),
            count: parseInt(p.querySelector('.rating-count')?.textContent)
        },
        attributes: Array.from(p.querySelectorAll('.attribute')).reduce((acc, attr) => {
            acc[attr.dataset.name] = attr.textContent.trim();
            return acc;
        }, {})
    }));
""") as List<Map<String, Any>>

products.forEach { product ->
    println("Product: ${"$"}{product["name"]}")
    println("  Price: ${"$"}{product["price"]}")
    println("  Rating: ${"$"}{product["rating"]}")
}
```

### Scroll Control

```kotlin
// Scroll to specific position
driver.executeScript("window.scrollTo(0, 1000)")

// Scroll element into view
driver.executeScript(
    """
    const el = document.querySelector(arguments[0]);
    if (el) el.scrollIntoView({ behavior: 'smooth', block: 'center' });
    """,
    listOf(".target-element")
)

// Get scroll position
val scrollY = driver.executeScript("return window.scrollY") as Int
println("Scroll position: ${"$"}scrollY")
```

### Wait for Conditions

```kotlin
// Wait for element to appear
driver.executeScript("""
    return new Promise((resolve) => {
        const observer = new MutationObserver((mutations, obs) => {
            const el = document.querySelector(arguments[0]);
            if (el) {
                obs.disconnect();
                resolve(true);
            }
        });
        observer.observe(document.body, {
            childList: true,
            subtree: true
        });
        
        // Timeout after 10 seconds
        setTimeout(() => {
            observer.disconnect();
            resolve(false);
        }, 10000);
    });
""", listOf(".dynamic-content"))
```

## Async Script Execution

### Execute Async Script

```kotlin
val driver = session.driver
driver.navigateTo("https://example.com")

// Async script with callback
val result = driver.executeAsyncScript(
    """
    const callback = arguments[arguments.length - 1];
    
    setTimeout(() => {
        callback({ status: 'completed', time: Date.now() });
    }, 2000);
    """,
    emptyList(),
    timeout = 5000
)
println("Async result: ${"$"}result")
```

### Fetch API Calls

```kotlin
// Make HTTP request from browser
val data = driver.executeAsyncScript(
    """
    const callback = arguments[arguments.length - 1];
    const url = arguments[0];
    
    fetch(url)
        .then(res => res.json())
        .then(data => callback(data))
        .catch(err => callback({ error: err.message }));
    """,
    listOf("https://api.example.com/data"),
    timeout = 10000
)
```

## Event Handling

### Trigger Events

```kotlin
// Trigger click event
driver.executeScript(
    """
    const el = document.querySelector(arguments[0]);
    if (el) el.click();
    """,
    listOf("button.submit")
)

// Trigger custom event
driver.executeScript(
    """
    const event = new CustomEvent('myevent', { detail: arguments[1] });
    document.querySelector(arguments[0])?.dispatchEvent(event);
    """,
    listOf(".element", mapOf("key" to "value"))
)
```

### Listen to Events

```kotlin
// Set up event listener and wait
driver.executeAsyncScript(
    """
    const callback = arguments[arguments.length - 1];
    
    document.addEventListener('myevent', function(e) {
        callback({ received: true, detail: e.detail });
    }, { once: true });
    
    // Timeout
    setTimeout(() => callback({ received: false }), 5000);
    """,
    emptyList(),
    timeout = 6000
)
```

## Local Storage & Cookies

### Local Storage

```kotlin
// Set local storage
driver.executeScript(
    "localStorage.setItem(arguments[0], arguments[1])",
    listOf("key", "value")
)

// Get local storage
val value = driver.executeScript(
    "return localStorage.getItem(arguments[0])",
    listOf("key")
)

// Clear local storage
driver.executeScript("localStorage.clear()")
```

### Session Storage

```kotlin
// Set session storage
driver.executeScript(
    "sessionStorage.setItem(arguments[0], arguments[1])",
    listOf("key", "value")
)

// Get session storage
val value = driver.executeScript(
    "return sessionStorage.getItem(arguments[0])",
    listOf("key")
)
```

### Cookies

```kotlin
// Get all cookies
val cookies = driver.executeScript(
    "return document.cookie"
) as String

// Set cookie
driver.executeScript(
    "document.cookie = arguments[0]",
    listOf("name=value; path=/")
)
```

## Utility Functions

### Define Reusable Functions

```kotlin
// Define helper function
driver.executeScript("""
    window.extractProductData = function(selector) {
        return Array.from(document.querySelectorAll(selector)).map(el => ({
            name: el.querySelector('.name')?.textContent?.trim(),
            price: el.querySelector('.price')?.textContent?.trim()
        }));
    };
""")

// Use the function
val products = driver.executeScript(
    "return window.extractProductData('.product')"
) as List<Map<String, Any>>
```

## Best Practices

1. **Return values** - Always return values from scripts
2. **Error handling** - Wrap scripts in try-catch
3. **Null checks** - Check for element existence before manipulation
4. **Timeouts** - Set appropriate timeouts for async scripts
5. **Clean code** - Use template strings for readability
6. **Type safety** - Cast results to appropriate Kotlin types
7. **Avoid blocking** - Use async scripts for long operations
8. **Test in console** - Test scripts in browser console first
9. **Minimize DOM access** - Batch DOM operations when possible
10. **Use arguments** - Pass data via arguments instead of string concatenation

## Error Handling

### Handle Script Errors

```kotlin
try {
    val result = driver.executeScript("""
        // Script that might fail
        return document.querySelector('.non-existent').textContent;
    """)
} catch (e: Exception) {
    println("Script error: ${"$"}{e.message}")
}
```

### Safe Execution

```kotlin
fun safeExecuteScript(script: String, args: List<Any> = emptyList()): Any? {
    return try {
        driver.executeScript(script, args)
    } catch (e: Exception) {
        println("Script execution failed: ${"$"}{e.message}")
        null
    }
}
```

## Next Steps

- [Data Extraction](data-extraction.md) - Use scripts for extraction
- [Element Interaction](element-interaction.md) - Combine with interactions
- [Examples](../examples/advanced-usage.md) - Advanced script examples
- [API Reference](../api/webdriver.md) - Complete script execution API
