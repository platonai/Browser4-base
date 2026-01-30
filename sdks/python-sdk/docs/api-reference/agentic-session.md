# AgenticSession API Reference

The `AgenticSession` class extends `PulsarSession` with AI-powered browser automation capabilities. It combines data extraction with intelligent natural language-driven browser interactions.

## Class: AgenticSession

```python
from pulsar_sdk import AgenticSession
```

### Constructor

```python
AgenticSession(client: PulsarClient)
```

**Parameters:**

- `client` (PulsarClient): An initialized PulsarClient instance with an active session

**Example:**

```python
from pulsar_sdk import PulsarClient, AgenticSession

client = PulsarClient()
client.create_session()
session = AgenticSession(client)
```

## Inheritance

`AgenticSession` inherits all methods from `PulsarSession`, including:

- `open()` - Open a URL immediately
- `load()` - Load a URL with caching
- `submit()` - Submit URLs for async processing
- `normalize()` - Normalize URLs
- `parse()` - Parse pages
- `extract()` - Extract data with CSS selectors
- `scrape()` - Load and extract in one call

See [PulsarSession API](session.md) for details on these inherited methods.

## AI-Powered Agent Methods

### act()

Execute a single action described in natural language.

```python
act(
    action: str,
    multi_act: bool = False,
    model_name: Optional[str] = None,
    variables: Optional[Dict[str, str]] = None,
    dom_settle_timeout_ms: Optional[int] = None,
    timeout_ms: Optional[int] = None
) -> AgentActResult
```

**Parameters:**

- `action` (str): Natural language description of the action to perform
- `multi_act` (bool): Whether each act forms a new chained context. Default: `False`
- `model_name` (Optional[str]): LLM model name to use. Default: `None` (uses server default)
- `variables` (Optional[Dict[str, str]]): Extra variables for prompt/tool. Default: `None`
- `dom_settle_timeout_ms` (Optional[int]): Timeout for DOM settling in milliseconds. Default: `None`
- `timeout_ms` (Optional[int]): Overall timeout in milliseconds. Default: `None`

**Returns:**

- `AgentActResult`: Action result with success status, message, and execution details

**Example:**

```python
# Simple action
result = session.act("click the login button")
print(f"Success: {result.success}")
print(f"Message: {result.message}")

# Action with custom model
result = session.act(
    "fill the search box with 'python tutorial'",
    model_name="gpt-4"
)

# Multiple sequential actions in context
result1 = session.act("click the menu", multi_act=True)
result2 = session.act("select profile settings", multi_act=True)
result3 = session.act("change language to English", multi_act=True)
```

**Alternative Name:**

`agent_act()` - Alias for `act()` method

### run()

Execute a multi-step autonomous task.

```python
run(
    task: str,
    multi_act: bool = False,
    model_name: Optional[str] = None,
    variables: Optional[Dict[str, str]] = None,
    dom_settle_timeout_ms: Optional[int] = None,
    timeout_ms: Optional[int] = None
) -> AgentRunResult
```

**Parameters:**

- `task` (str): Natural language description of the task to accomplish
- `multi_act` (bool): Whether each act forms a new chained context. Default: `False`
- `model_name` (Optional[str]): LLM model name to use. Default: `None`
- `variables` (Optional[Dict[str, str]]): Extra variables for prompt/tool. Default: `None`
- `dom_settle_timeout_ms` (Optional[int]): Timeout for DOM settling in milliseconds. Default: `None`
- `timeout_ms` (Optional[int]): Overall timeout in milliseconds. Default: `None`

**Returns:**

- `AgentRunResult`: Task result with success status, execution history, and trace information

**Example:**

```python
# Simple autonomous task
result = session.run("search for 'python tutorial' and click first result")
print(f"Success: {result.success}")
print(f"History size: {result.history_size}")
print(f"Process trace size: {result.process_trace_size}")

# Complex multi-step task
result = session.run(
    "login with username 'test@example.com', navigate to settings, and enable notifications"
)

# Task with timeout
result = session.run(
    "find all products under $50 and add them to cart",
    timeout_ms=60000  # 60 second timeout
)

# Access execution trace
for trace_entry in session.process_trace:
    print(f"Trace: {trace_entry}")
```

**Alternative Name:**

`agent_run()` - Alias for `run()` method

### observe()

Observe the current page and return potential actions.

```python
observe(
    instruction: Optional[str] = None,
    model_name: Optional[str] = None,
    dom_settle_timeout_ms: Optional[int] = None,
    return_action: Optional[bool] = None,
    draw_overlay: bool = True
) -> AgentObservation
```

**Parameters:**

- `instruction` (Optional[str]): Optional observation instruction to guide the AI. Default: `None`
- `model_name` (Optional[str]): LLM model name to use. Default: `None`
- `dom_settle_timeout_ms` (Optional[int]): Timeout for DOM settling in milliseconds. Default: `None`
- `return_action` (Optional[bool]): Whether to return actionable tool calls. Default: `None`
- `draw_overlay` (bool): Whether to highlight interactive elements. Default: `True`

**Returns:**

- `AgentObservation`: Observation results with page analysis and suggested actions

**Example:**

```python
# Basic observation
observation = session.observe()
for obs in observation.observations:
    print(f"Element: {obs.locator}")
    print(f"Description: {obs.description}")
    print(f"Action: {obs.method}")

# Observation with specific instruction
observation = session.observe(
    instruction="find all buttons related to checkout"
)

# Observation without overlay
observation = session.observe(
    instruction="analyze form fields",
    draw_overlay=False
)

# Get page summary from observation
for obs in observation.observations:
    if obs.summary:
        print(f"Summary: {obs.summary}")
    if obs.thinking:
        print(f"AI Thinking: {obs.thinking}")
    if obs.next_suggestions:
        print(f"Suggestions: {obs.next_suggestions}")
```

**Alternative Name:**

`agent_observe()` - Alias for `observe()` method

### agent_extract()

Extract structured data from the page using AI.

```python
agent_extract(
    instruction: str,
    schema: Optional[Dict[str, Any]] = None,
    selector: Optional[str] = None,
    model_name: Optional[str] = None,
    dom_settle_timeout_ms: Optional[int] = None
) -> ExtractionResult
```

**Parameters:**

- `instruction` (str): Extraction instruction describing what to extract
- `schema` (Optional[Dict[str, Any]]): JSON schema for the extraction result. Default: `None`
- `selector` (Optional[str]): CSS selector to scope extraction. Default: `None`
- `model_name` (Optional[str]): LLM model name to use. Default: `None`
- `dom_settle_timeout_ms` (Optional[int]): Timeout for DOM settling in milliseconds. Default: `None`

**Returns:**

- `ExtractionResult`: Extracted data with success status and message

**Example:**

```python
# Simple extraction
result = session.agent_extract("extract all product names and prices")
print(f"Success: {result.success}")
print(f"Data: {result.data}")

# Extraction with schema
schema = {
    "type": "object",
    "properties": {
        "products": {
            "type": "array",
            "items": {
                "type": "object",
                "properties": {
                    "name": {"type": "string"},
                    "price": {"type": "number"},
                    "rating": {"type": "number"}
                }
            }
        }
    }
}

result = session.agent_extract(
    instruction="extract all products with their details",
    schema=schema
)

# Scoped extraction
result = session.agent_extract(
    instruction="extract author name and publication date",
    selector="article.main-content"
)

# Process extracted data
if result.success:
    products = result.data.get('products', [])
    for product in products:
        print(f"{product['name']}: ${product['price']}")
```

### summarize()

Generate a summary of page content.

```python
summarize(
    instruction: Optional[str] = None,
    selector: Optional[str] = None
) -> str
```

**Parameters:**

- `instruction` (Optional[str]): Optional guidance for summarization. Default: `None`
- `selector` (Optional[str]): CSS selector to limit summarization scope. Default: `None`

**Returns:**

- `str`: Summary text

**Example:**

```python
# Summarize entire page
summary = session.summarize()
print(summary)

# Summarize with instruction
summary = session.summarize(
    instruction="focus on pricing and features"
)

# Summarize specific section
summary = session.summarize(
    selector="article.main-content"
)

# Summarize with both instruction and selector
summary = session.summarize(
    instruction="summarize the key technical specifications",
    selector="div.product-specs"
)
```

**Alternative Name:**

`agent_summarize()` - Alias for `summarize()` method

### clear_history()

Clear the agent's execution history.

```python
clear_history() -> bool
```

**Returns:**

- `bool`: True if history was cleared successfully

**Example:**

```python
# Execute some tasks
session.act("click the menu")
session.run("search for products")

# Clear history for fresh start
success = session.clear_history()
print(f"History cleared: {success}")

# New tasks are not affected by previous context
session.run("perform a new search")
```

**Alternative Name:**

`agent_clear_history()` - Alias for `clear_history()` method

## Properties

### driver

Access the underlying WebDriver instance.

```python
driver: WebDriver
```

**Example:**

```python
# Access WebDriver for low-level operations
driver = session.driver
driver.navigate_to("https://example.com")
title = driver.title()
```

### process_trace

Get the execution trace as a list of strings.

```python
process_trace: List[str]
```

**Example:**

```python
# Execute actions
session.act("click login")
session.run("fill form and submit")

# View execution trace
for entry in session.process_trace:
    print(f"Trace: {entry}")
```

**Alternative Name:**

`processTrace` - Kotlin-style naming alias

### companion_agent

Get the companion agent (returns self for API compatibility).

```python
companion_agent: AgenticSession
```

### context

Get the session context (returns self for API compatibility).

```python
context: AgenticSession
```

## Complete Example

```python
from pulsar_sdk import PulsarClient, AgenticSession

# Setup
client = PulsarClient(base_url="http://localhost:8182")
client.create_session()
session = AgenticSession(client)

try:
    # Navigate to a website
    session.open("https://example.com")
    
    # Use AI to observe the page
    observation = session.observe()
    print(f"Found {len(observation.observations)} interactive elements")
    
    # Perform an action
    result = session.act("click the search button")
    if result.success:
        print("Search button clicked successfully")
    
    # Execute a complex task
    task_result = session.run(
        "search for 'python' and click the first result"
    )
    print(f"Task completed: {task_result.success}")
    
    # Extract data with AI
    extraction = session.agent_extract(
        "extract all article titles and authors"
    )
    print(f"Extracted data: {extraction.data}")
    
    # Get page summary
    summary = session.summarize()
    print(f"Page summary: {summary}")
    
    # View execution trace
    print(f"\nExecution trace ({len(session.process_trace)} entries):")
    for trace in session.process_trace:
        print(f"  - {trace}")
    
finally:
    session.close()
```

## Use Case Examples

### E-commerce Automation

```python
session = AgenticSession(client)
session.open("https://shop.example.com")

# Search for products
session.run("search for 'wireless headphones'")

# Extract product information
products = session.agent_extract(
    instruction="extract product names, prices, and ratings",
    schema={
        "type": "array",
        "items": {
            "type": "object",
            "properties": {
                "name": {"type": "string"},
                "price": {"type": "number"},
                "rating": {"type": "number"}
            }
        }
    }
)

# Filter and select best product
if products.success:
    for product in products.data:
        if product['rating'] >= 4.5 and product['price'] < 100:
            print(f"Found good deal: {product['name']}")
```

### Form Filling

```python
session = AgenticSession(client)
session.open("https://example.com/contact")

# Fill form using natural language
session.run("""
    fill the contact form with:
    - name: John Doe
    - email: john@example.com
    - message: I'm interested in your services
    and submit the form
""")

# Verify submission
summary = session.summarize("check if form was submitted successfully")
print(summary)
```

### Web Scraping with Context

```python
session = AgenticSession(client)
session.open("https://news.example.com")

# Extract news articles
articles = []
for i in range(3):
    # Navigate to article
    session.act(f"click article number {i+1}")
    
    # Extract article data
    article = session.agent_extract(
        instruction="extract title, author, date, and content"
    )
    
    if article.success:
        articles.append(article.data)
    
    # Go back
    session.driver.go_back()

print(f"Scraped {len(articles)} articles")
```

## Error Handling

```python
from pulsar_sdk import PulsarClient, AgenticSession

client = PulsarClient()
client.create_session()
session = AgenticSession(client)

try:
    # Execute action
    result = session.act("click the submit button")
    
    if not result.success:
        print(f"Action failed: {result.message}")
    else:
        print(f"Action successful: {result.message}")
        
except Exception as e:
    print(f"Error: {e}")
finally:
    session.close()
```

## Best Practices

1. **Clear History Between Tasks**: Clear history when switching to unrelated tasks to prevent context contamination

```python
session.clear_history()
session.run("new independent task")
```

2. **Use Timeouts**: Set appropriate timeouts for long-running operations

```python
result = session.run(
    "complex task",
    timeout_ms=120000  # 2 minutes
)
```

3. **Provide Clear Instructions**: Write specific, actionable instructions

```python
# Good
session.act("click the blue 'Sign In' button in the top right")

# Less specific
session.act("click button")
```

4. **Use Schemas for Extraction**: Define schemas for structured data extraction

```python
schema = {
    "type": "object",
    "properties": {
        "title": {"type": "string"},
        "price": {"type": "number"}
    }
}
result = session.agent_extract("extract product info", schema=schema)
```

5. **Check Results**: Always check success status before using results

```python
result = session.act("click menu")
if result.success:
    # Proceed with next action
    pass
else:
    # Handle error
    print(f"Failed: {result.message}")
```

## See Also

- [PulsarSession API](session.md) - Base session functionality
- [WebDriver API](webdriver.md) - Low-level browser control
- [Data Models](models.md) - Result types and data structures
- [PulsarClient API](client.md) - HTTP client

---

[← Back to PulsarSession](session.md) | [Next: WebDriver →](webdriver.md)
