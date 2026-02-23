# WebDriver API

Browser control and element interaction.

## Constructor

```kotlin
class WebDriver(val client: PulsarClient)
```

## Navigation

### navigateTo()

```kotlin
suspend fun navigateTo(url: String): Any?
```

Navigate to URL.

### currentUrl(), url()

```kotlin
suspend fun currentUrl(): String
suspend fun url(): String
```

Get current URL.

### title()

```kotlin
suspend fun title(): String
```

Get page title.

### reload(), goBack(), goForward()

```kotlin
suspend fun reload(): Any?
suspend fun goBack(): Any?
suspend fun goForward(): Any?
```

Navigation controls.

## Element Interaction

### click()

```kotlin
suspend fun click(
    selector: String,
    strategy: String = "css"
): Any?
```

Click element.

### fill()

```kotlin
suspend fun fill(
    selector: String,
    text: String,
    strategy: String = "css"
): Any?
```

Fill input field.

### type()

```kotlin
suspend fun type(
    selector: String,
    text: String,
    strategy: String = "css"
): Any?
```

Type text (simulates keyboard).

### press()

```kotlin
suspend fun press(
    selector: String,
    key: String,
    strategy: String = "css"
): Any?
```

Press key.

### check(), uncheck()

```kotlin
suspend fun check(selector: String, strategy: String = "css"): Any?
suspend fun uncheck(selector: String, strategy: String = "css"): Any?
```

Checkbox operations.

### hover(), focus()

```kotlin
suspend fun hover(selector: String, strategy: String = "css"): Any?
suspend fun focus(selector: String, strategy: String = "css"): Any?
```

## Waiting

### waitForSelector()

```kotlin
suspend fun waitForSelector(
    selector: String,
    timeout: Long = 30000,
    strategy: String = "css"
): Any?
```

Wait for element.

### waitForNavigation()

```kotlin
suspend fun waitForNavigation(timeout: Long = 30000): Any?
```

Wait for navigation to complete.

### exists(), isVisible(), isHidden()

```kotlin
suspend fun exists(selector: String, strategy: String = "css"): Boolean
suspend fun isVisible(selector: String, strategy: String = "css"): Boolean
suspend fun isHidden(selector: String, strategy: String = "css"): Boolean
```

Element state checks.

## Scrolling

### scrollDown(), scrollUp()

```kotlin
suspend fun scrollDown(count: Int = 1): Any?
suspend fun scrollUp(count: Int = 1): Any?
```

Scroll by pages.

### scrollTo()

```kotlin
suspend fun scrollTo(selector: String, strategy: String = "css"): Any?
```

Scroll to element.

### scrollToTop(), scrollToBottom(), scrollToMiddle()

```kotlin
suspend fun scrollToTop(): Any?
suspend fun scrollToBottom(): Any?
suspend fun scrollToMiddle(ratio: Double = 0.5): Any?
```

Scroll to positions.

## Content Extraction

### selectFirstTextOrNull()

```kotlin
suspend fun selectFirstTextOrNull(selector: String, strategy: String = "css"): String?
```

Extract first element text.

### selectTextAll()

```kotlin
suspend fun selectTextAll(selector: String, strategy: String = "css"): List<String>
```

Extract all element texts.

### selectFirstAttributeOrNull()

```kotlin
suspend fun selectFirstAttributeOrNull(
    selector: String,
    attr: String,
    strategy: String = "css"
): String?
```

Extract first attribute.

### selectAttributeAll()

```kotlin
suspend fun selectAttributeAll(
    selector: String,
    attr: String,
    strategy: String = "css"
): List<String>
```

Extract all attributes.

### outerHtml(), textContent()

```kotlin
suspend fun outerHtml(selector: String? = null, strategy: String = "css"): String?
suspend fun textContent(selector: String? = null, strategy: String = "css"): String?
```

### extract()

```kotlin
suspend fun extract(fields: Map<String, String>): Map<String, Any?>
```

Extract multiple fields.

## Screenshots

### captureScreenshot()

```kotlin
suspend fun captureScreenshot(
    selector: String? = null,
    fullPage: Boolean = false,
    strategy: String = "css"
): String?
```

Capture screenshot (Base64).

### screenshot()

```kotlin
suspend fun screenshot(selector: String? = null, strategy: String = "css"): String?
```

Convenience method.

## Script Execution

### executeScript()

```kotlin
suspend fun executeScript(script: String, args: List<Any> = emptyList()): Any?
```

Execute JavaScript.

### executeAsyncScript()

```kotlin
suspend fun executeAsyncScript(
    script: String,
    args: List<Any> = emptyList(),
    timeout: Long = 30000
): Any?
```

Execute async JavaScript.

### evaluate()

```kotlin
suspend fun evaluate(expression: String): Any?
```

Evaluate expression.

## Control

### delay()

```kotlin
suspend fun delay(millis: Long): Any?
```

Delay execution.

### pause(), stop()

```kotlin
suspend fun pause(): Any?
suspend fun stop(): Any?
```

## See Also

- [Element Interaction Guide](../guide/element-interaction.md)
- [Data Extraction Guide](../guide/data-extraction.md)
