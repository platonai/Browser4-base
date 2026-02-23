# API Comparison: NodeJS SDK vs Python SDK vs Kotlin SDK

This document compares the Browser4 SDK APIs across different languages to demonstrate alignment and consistency.

## Client Creation

### NodeJS
```typescript
import { PulsarClient } from '@platonai/browser4-sdk';
const client = new PulsarClient({ baseUrl: 'http://localhost:8182' });
const sessionId = await client.createSession();
```

### Python
```python
from browser4 import PulsarClient
client = PulsarClient(base_url='http://localhost:8182')
session_id = client.create_session()
```

### Kotlin
```kotlin
val client = PulsarClient(baseUrl = "http://localhost:8182")
val sessionId = client.createSession()
```

## Session Management

### NodeJS
```typescript
import { PulsarSession } from '@platonai/browser4-sdk';
const session = new PulsarSession(client);

// Open URL
const page = await session.open('https://example.com');

// Load with options
const page2 = await session.load('https://example.com', '-expire 1d');

// Normalize URL
const normUrl = await session.normalize('https://example.com', '-expire 1d');
```

### Python
```python
from browser4 import PulsarSession
session = PulsarSession(client)

# Open URL
page = session.open('https://example.com')

# Load with options
page2 = session.load('https://example.com', args='-expire 1d')

# Normalize URL
norm_url = session.normalize('https://example.com', args='-expire 1d')
```

### Kotlin
```kotlin
val session = PulsarSession(client)

// Open URL
val page = session.open("https://example.com")

// Load with options
val page2 = session.load("https://example.com", args = "-expire 1d")

// Normalize URL
val normUrl = session.normalize("https://example.com", args = "-expire 1d")
```

## AI-Powered Agent Operations

### NodeJS
```typescript
import { AgenticSession } from '@platonai/browser4-sdk';
const session = new AgenticSession(client);

// Execute single action
const actResult = await session.act('click the login button');

// Run multi-step task
const runResult = await session.run('search for "nodejs"');

// Observe page
const observations = await session.observe('what can I do?');

// Summarize
const summary = await session.summarize('summarize this page');

// Extract with AI
const extraction = await session.agentExtract('extract product names');

// Get history
const history = session.stateHistory;

// Clear history
await session.clearHistory();
```

### Python
```python
from browser4 import AgenticSession
session = AgenticSession(client)

# Execute single action
act_result = session.act('click the login button')

# Run multi-step task
run_result = session.run('search for "nodejs"')

# Observe page
observations = session.observe('what can I do?')

# Summarize
summary = session.summarize('summarize this page')

# Extract with AI
extraction = session.agent_extract('extract product names')

# Get history
history = session.state_history

# Clear history
session.clear_history()
```

### Kotlin
```kotlin
val session = AgenticSession(client)

// Execute single action
val actResult = session.act("click the login button")

// Run multi-step task
val runResult = session.run("search for \"nodejs\"")

// Observe page
val observations = session.observe("what can I do?")

// Summarize
val summary = session.summarize("summarize this page")

// Extract with AI
val extraction = session.agentExtract("extract product names")

// Get history
val history = session.stateHistory

// Clear history
session.clearHistory()
```

## WebDriver Operations

### NodeJS
```typescript
const driver = session.driver;

// Navigation
await driver.navigateTo('https://example.com');
const url = await driver.currentUrl();
await driver.back();
await driver.forward();
await driver.refresh();

// Element interaction
await driver.click('button.submit');
await driver.fill('input[name="email"]', 'test@example.com');
await driver.type('textarea', 'text');
await driver.press('input', 'Enter');

// Element queries
const text = await driver.getText('h1');
const exists = await driver.exists('.modal');
await driver.waitForSelector('.loading', 5000);

// Actions
await driver.hover('.dropdown');
await driver.scrollTo('#footer');
await driver.executeScript('return document.title');
await driver.delay(1000);
```

### Python
```python
driver = session.driver

# Navigation
driver.navigate_to('https://example.com')
url = driver.current_url()
driver.back()
driver.forward()
driver.refresh()

# Element interaction
driver.click('button.submit')
driver.fill('input[name="email"]', 'test@example.com')
driver.type('textarea', 'text')
driver.press('input', 'Enter')

# Element queries
text = driver.get_text('h1')
exists = driver.exists('.modal')
driver.wait_for_selector('.loading', timeout=5000)

# Actions
driver.hover('.dropdown')
driver.scroll_to('#footer')
driver.execute_script('return document.title')
driver.delay(1000)
```

### Kotlin
```kotlin
val driver = session.driver

// Navigation
driver.navigateTo("https://example.com")
val url = driver.currentUrl()
driver.back()
driver.forward()
driver.refresh()

// Element interaction
driver.click("button.submit")
driver.fill("input[name='email']", "test@example.com")
driver.type("textarea", "text")
driver.press("input", "Enter")

// Element queries
val text = driver.getText("h1")
val exists = driver.exists(".modal")
driver.waitForSelector(".loading", timeout = 5000)

// Actions
driver.hover(".dropdown")
driver.scrollTo("#footer")
driver.executeScript("return document.title")
driver.delay(1000)
```

## Data Extraction

### NodeJS
```typescript
// Parse and extract
const document = await session.parse(page);
const fields = await session.extract(document, {
  title: 'h1',
  price: '.price'
});

// Scrape in one call
const data = await session.scrape('https://example.com', {
  title: 'h1',
  description: '.description'
});
```

### Python
```python
# Parse and extract
document = session.parse(page)
fields = session.extract(document, {
    'title': 'h1',
    'price': '.price'
})

# Scrape in one call
data = session.scrape('https://example.com', {
    'title': 'h1',
    'description': '.description'
})
```

### Kotlin
```kotlin
// Parse and extract
val document = session.parse(page)
val fields = session.extract(document, mapOf(
    "title" to "h1",
    "price" to ".price"
))

// Scrape in one call
val data = session.scrape("https://example.com", mapOf(
    "title" to "h1",
    "description" to ".description"
))
```

## Data Models

All three SDKs provide the same data models:

- **WebPage**: Page loading result
- **NormURL**: Normalized URL with arguments
- **AgentRunResult**: Multi-step task result
- **AgentActResult**: Single action result
- **AgentObservation**: Page observation result
- **ExtractionResult**: AI extraction result
- **AgentHistory**: Execution history
- **ChatResponse**: LLM chat response

## Naming Conventions

| Concept | NodeJS | Python | Kotlin |
|---------|--------|--------|--------|
| Class names | PascalCase | PascalCase | PascalCase |
| Method names | camelCase | snake_case | camelCase |
| Property names | camelCase | snake_case | camelCase |
| Constructor | `new Class()` | `Class()` | `Class()` |
| Async operations | `async/await` | sync by default | suspend functions |

## Key Differences

### Async/Await Pattern

**NodeJS**: All I/O operations are async and use `async/await`
```typescript
const page = await session.open('https://example.com');
```

**Python**: All operations are synchronous (blocking)
```python
page = session.open('https://example.com')
```

**Kotlin**: Uses coroutines with suspend functions
```kotlin
val page = session.open("https://example.com")
```

### Property Access

**NodeJS**: Uses getters for computed properties
```typescript
const uuid = session.uuid;
const history = session.stateHistory;
```

**Python**: Uses properties and methods
```python
uuid = session.uuid
history = session.state_history
```

**Kotlin**: Uses properties
```kotlin
val uuid = session.uuid
val history = session.stateHistory
```

### Error Handling

**NodeJS**: Uses try/catch with Promises
```typescript
try {
  await client.createSession();
} catch (error) {
  console.error(error);
}
```

**Python**: Uses try/except
```python
try:
    client.create_session()
except Exception as e:
    print(e)
```

**Kotlin**: Uses try/catch
```kotlin
try {
    client.createSession()
} catch (e: Exception) {
    println(e)
}
```

## Browser4Driver (Server Management)

### NodeJS
```typescript
import { Browser4Driver } from '@platonai/browser4-sdk';

const driver = new Browser4Driver({ port: 8182 });
await driver.use(async (d) => {
  console.log(`Server at: ${d.baseUrl}`);
  // Use the server...
});
// Server stops automatically
```

### Python
```python
from browser4 import Browser4Driver

with Browser4Driver(port=8182) as driver:
    print(f"Server at: {driver.base_url}")
    # Use the server...
# Server stops automatically
```

### Kotlin
```kotlin
Browser4Driver(port = 8182).use { driver ->
    println("Server at: ${driver.baseUrl}")
    // Use the server...
}
// Server stops automatically
```

## Summary

The Browser4 SDK maintains consistent APIs across all three languages while respecting each language's idioms and conventions:

- **NodeJS**: Modern TypeScript with async/await, strong typing
- **Python**: Pythonic naming with context managers, type hints
- **Kotlin**: Kotlin coroutines, extension functions, data classes

All SDKs provide:
- ✅ Same core functionality
- ✅ Aligned API methods
- ✅ Consistent data models
- ✅ Compatible workflows
- ✅ Language-appropriate conventions
