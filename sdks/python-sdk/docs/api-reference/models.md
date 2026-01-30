# Data Models API Reference

This module contains data models (data classes) used throughout the Python SDK. These models correspond to the Kotlin data classes and provide a consistent interface for working with Browser4 API responses.

## Overview

All models in this module are Python dataclasses that provide:

- Type hints for all fields
- JSON serialization/deserialization
- Kotlin-style naming aliases for compatibility
- Clear documentation of each field

## Core Models

### WebPage

Represents a web page result from load/open operations.

```python
@dataclass
class WebPage:
    url: str
    location: Optional[str] = None
    content_type: Optional[str] = None
    content_length: int = 0
    protocol_status: Optional[str] = None
    is_nil: bool = False
    html: Optional[str] = None
```

**Fields:**

- `url` (str): The original URL requested
- `location` (Optional[str]): Final URL after redirects. Default: `None`
- `content_type` (Optional[str]): MIME type (e.g., "text/html"). Default: `None`
- `content_length` (int): Content length in bytes. Default: `0`
- `protocol_status` (Optional[str]): HTTP status code and message. Default: `None`
- `is_nil` (bool): True if page is invalid/not found. Default: `False`
- `html` (Optional[str]): Raw HTML content if available. Default: `None`

**Properties:**

- `contentType`: Alias for `content_type` (Kotlin-style)
- `contentLength`: Alias for `content_length` (Kotlin-style)
- `protocolStatus`: Alias for `protocol_status` (Kotlin-style)
- `isNil`: Alias for `is_nil` (Kotlin-style)

**Example:**

```python
from pulsar_sdk import PulsarSession

session = PulsarSession(client)
page = session.open("https://example.com")

print(f"URL: {page.url}")
print(f"Final location: {page.location}")
print(f"Content type: {page.content_type}")
print(f"Content length: {page.content_length} bytes")
print(f"Status: {page.protocol_status}")
print(f"Is valid: {not page.is_nil}")

if page.html:
    print(f"HTML length: {len(page.html)} characters")

# Using Kotlin-style properties
print(f"Content type (Kotlin): {page.contentType}")
print(f"Is nil (Kotlin): {page.isNil}")
```

**Class Method:**

```python
@classmethod
def from_dict(cls, data: Dict[str, Any]) -> WebPage
```

Creates a WebPage instance from an API response dictionary.

### NormURL

Normalized URL result with parsed arguments.

```python
@dataclass
class NormURL:
    spec: str
    url: str
    args: Optional[str] = None
    is_nil: bool = False
```

**Fields:**

- `spec` (str): Full normalized specification (URL + args)
- `url` (str): Normalized URL
- `args` (Optional[str]): Parsed arguments string. Default: `None`
- `is_nil` (bool): True if URL is invalid. Default: `False`

**Properties:**

- `isNil`: Alias for `is_nil` (Kotlin-style)

**Example:**

```python
from pulsar_sdk import PulsarSession

session = PulsarSession(client)
norm = session.normalize("https://example.com", args="-expire 1d")

print(f"Full spec: {norm.spec}")
print(f"URL: {norm.url}")
print(f"Args: {norm.args}")
print(f"Valid: {not norm.is_nil}")

# Check if URL is valid before using
if not norm.is_nil:
    page = session.load(norm.url, norm.args)
```

**Class Method:**

```python
@classmethod
def from_dict(cls, data: Dict[str, Any]) -> NormURL
```

Creates a NormURL instance from an API response dictionary.

### PageSnapshot

Snapshot of a web page, used for capture operations.

```python
@dataclass
class PageSnapshot:
    url: str
    html: Optional[str] = None
```

**Fields:**

- `url` (str): The page URL
- `html` (Optional[str]): HTML content snapshot. Default: `None`

**Example:**

```python
from pulsar_sdk import AgenticSession

session = AgenticSession(client)
session.open("https://example.com")

# Capture current page state
snapshot = session.capture()

print(f"Captured URL: {snapshot.url}")
if snapshot.html:
    print(f"HTML length: {len(snapshot.html)}")
```

### ElementRef

Reference to a DOM element, matching WebDriver element identifier.

```python
@dataclass
class ElementRef:
    element_id: str
```

**Fields:**

- `element_id` (str): WebDriver element identifier

**Example:**

```python
from pulsar_sdk.models import ElementRef

# Element reference from WebDriver
element_ref = ElementRef(element_id="abc123")
print(f"Element ID: {element_ref.element_id}")
```

## Agent Result Models

### AgentActResult

Result from agent act operation (single action execution).

```python
@dataclass
class AgentActResult:
    success: bool = False
    message: str = ""
    action: Optional[str] = None
    is_complete: bool = False
    expression: Optional[str] = None
    result: Any = None
    trace: Optional[List[str]] = None
```

**Fields:**

- `success` (bool): Whether the action succeeded. Default: `False`
- `message` (str): Result message or error description. Default: `""`
- `action` (Optional[str]): The action that was executed. Default: `None`
- `is_complete` (bool): Whether the action completed. Default: `False`
- `expression` (Optional[str]): JavaScript expression used. Default: `None`
- `result` (Any): Action result value. Default: `None`
- `trace` (Optional[List[str]]): Execution trace. Default: `None`

**Properties:**

- `isComplete`: Alias for `is_complete` (Kotlin-style)

**Example:**

```python
from pulsar_sdk import AgenticSession

session = AgenticSession(client)
result = session.act("click the submit button")

print(f"Success: {result.success}")
print(f"Message: {result.message}")
print(f"Action: {result.action}")
print(f"Complete: {result.is_complete}")

if result.result:
    print(f"Result: {result.result}")

if result.trace:
    print(f"Trace: {result.trace}")

# Using Kotlin-style property
print(f"Is complete (Kotlin): {result.isComplete}")
```

**Class Method:**

```python
@classmethod
def from_dict(cls, data: Dict[str, Any]) -> AgentActResult
```

### AgentRunResult

Result from agent run operation (multi-step task execution).

```python
@dataclass
class AgentRunResult:
    success: bool = False
    message: str = ""
    history_size: int = 0
    process_trace_size: int = 0
    final_result: Any = None
    trace: Optional[List[str]] = None
```

**Fields:**

- `success` (bool): Whether the task succeeded. Default: `False`
- `message` (str): Result message or error description. Default: `""`
- `history_size` (int): Number of history entries. Default: `0`
- `process_trace_size` (int): Number of process trace entries. Default: `0`
- `final_result` (Any): Final result value. Default: `None`
- `trace` (Optional[List[str]]): Execution trace. Default: `None`

**Properties:**

- `finalResult`: Alias for `final_result` (Kotlin-style)
- `historySize`: Alias for `history_size` (Kotlin-style)
- `processTraceSize`: Alias for `process_trace_size` (Kotlin-style)

**Example:**

```python
from pulsar_sdk import AgenticSession

session = AgenticSession(client)
result = session.run("search for python and click first result")

print(f"Success: {result.success}")
print(f"Message: {result.message}")
print(f"History size: {result.history_size}")
print(f"Process trace size: {result.process_trace_size}")

if result.final_result:
    print(f"Final result: {result.final_result}")

if result.trace:
    for entry in result.trace:
        print(f"Trace: {entry}")

# Using Kotlin-style properties
print(f"Final result (Kotlin): {result.finalResult}")
print(f"History size (Kotlin): {result.historySize}")
```

**Class Method:**

```python
@classmethod
def from_dict(cls, data: Dict[str, Any]) -> AgentRunResult
```

### AgentObservation

Result from agent observe operation.

```python
@dataclass
class AgentObservation:
    observations: List[ObserveResult] = field(default_factory=list)
```

**Fields:**

- `observations` (List[ObserveResult]): List of observation results

**Example:**

```python
from pulsar_sdk import AgenticSession

session = AgenticSession(client)
observation = session.observe()

print(f"Found {len(observation.observations)} observations")

for obs in observation.observations:
    print(f"\nObservation:")
    print(f"  Locator: {obs.locator}")
    print(f"  Description: {obs.description}")
    print(f"  Method: {obs.method}")
    if obs.summary:
        print(f"  Summary: {obs.summary}")
```

**Class Method:**

```python
@classmethod
def from_dict(cls, data: Any) -> AgentObservation
```

### ObserveResult

Single observation result from agent observe operation.

```python
@dataclass
class ObserveResult:
    locator: Optional[str] = None
    domain: Optional[str] = None
    method: Optional[str] = None
    arguments: Optional[Dict[str, Any]] = None
    description: Optional[str] = None
    screenshot_content_summary: Optional[str] = None
    current_page_content_summary: Optional[str] = None
    next_goal: Optional[str] = None
    thinking: Optional[str] = None
    summary: Optional[str] = None
    key_findings: Optional[str] = None
    next_suggestions: Optional[List[str]] = None
```

**Fields:**

- `locator` (Optional[str]): Element locator/selector. Default: `None`
- `domain` (Optional[str]): Operation domain. Default: `None`
- `method` (Optional[str]): Suggested method to call. Default: `None`
- `arguments` (Optional[Dict[str, Any]]): Method arguments. Default: `None`
- `description` (Optional[str]): Element/action description. Default: `None`
- `screenshot_content_summary` (Optional[str]): Screenshot summary. Default: `None`
- `current_page_content_summary` (Optional[str]): Page content summary. Default: `None`
- `next_goal` (Optional[str]): Suggested next goal. Default: `None`
- `thinking` (Optional[str]): AI reasoning process. Default: `None`
- `summary` (Optional[str]): Overall summary. Default: `None`
- `key_findings` (Optional[str]): Key findings from observation. Default: `None`
- `next_suggestions` (Optional[List[str]]): List of next action suggestions. Default: `None`

**Example:**

```python
from pulsar_sdk import AgenticSession

session = AgenticSession(client)
observation = session.observe(instruction="find login elements")

for obs in observation.observations:
    if obs.locator:
        print(f"Element: {obs.locator}")
    if obs.description:
        print(f"Description: {obs.description}")
    if obs.method:
        print(f"Suggested action: {obs.method}")
    if obs.arguments:
        print(f"Arguments: {obs.arguments}")
    if obs.thinking:
        print(f"AI thinking: {obs.thinking}")
    if obs.next_suggestions:
        print(f"Suggestions: {', '.join(obs.next_suggestions)}")
```

**Class Method:**

```python
@classmethod
def from_dict(cls, data: Dict[str, Any]) -> ObserveResult
```

### ExtractionResult

Result from agent extract operation.

```python
@dataclass
class ExtractionResult:
    success: bool = False
    message: str = ""
    data: Any = None
```

**Fields:**

- `success` (bool): Whether extraction succeeded. Default: `False`
- `message` (str): Result message or error description. Default: `""`
- `data` (Any): Extracted data. Default: `None`

**Example:**

```python
from pulsar_sdk import AgenticSession

session = AgenticSession(client)
result = session.agent_extract(
    instruction="extract all product names and prices"
)

print(f"Success: {result.success}")
print(f"Message: {result.message}")

if result.success and result.data:
    # Data structure depends on extraction instruction and schema
    if isinstance(result.data, list):
        print(f"Extracted {len(result.data)} items")
        for item in result.data:
            print(f"  - {item}")
    elif isinstance(result.data, dict):
        for key, value in result.data.items():
            print(f"  {key}: {value}")
```

**Class Method:**

```python
@classmethod
def from_dict(cls, data: Dict[str, Any]) -> ExtractionResult
```

## Utility Models

### FieldsExtraction

Result of field extraction with CSS selectors.

```python
@dataclass
class FieldsExtraction:
    fields: Dict[str, Any] = field(default_factory=dict)
```

**Fields:**

- `fields` (Dict[str, Any]): Dictionary mapping field names to extracted values

**Example:**

```python
from pulsar_sdk import PulsarSession

session = PulsarSession(client)
page = session.open("https://example.com")

# Extract fields
fields = session.extract(page, {
    "title": "h1",
    "description": "meta[name='description']",
    "author": ".author-name"
})

# fields is a dict, can be wrapped in FieldsExtraction if needed
from pulsar_sdk.models import FieldsExtraction

extraction = FieldsExtraction(fields=fields)
print(f"Title: {extraction.fields['title']}")
print(f"Author: {extraction.fields['author']}")
```

### ToolCallResult

Result of a tool call execution.

```python
@dataclass
class ToolCallResult:
    success: bool = False
    message: str = ""
    data: Any = None
```

**Fields:**

- `success` (bool): Whether the tool call succeeded. Default: `False`
- `message` (str): Result message or error description. Default: `""`
- `data` (Any): Tool call result data. Default: `None`

**Example:**

```python
from pulsar_sdk.models import ToolCallResult

# Tool call result
result = ToolCallResult(
    success=True,
    message="Element clicked successfully",
    data={"element": "button.submit"}
)

print(f"Success: {result.success}")
print(f"Message: {result.message}")
print(f"Data: {result.data}")
```

**Class Method:**

```python
@classmethod
def from_dict(cls, data: Dict[str, Any]) -> ToolCallResult
```

### ActionDescription

Description of an action to be performed.

```python
@dataclass
class ActionDescription:
    description: str
    parameters: Optional[Dict[str, Any]] = None
```

**Fields:**

- `description` (str): Human-readable action description
- `parameters` (Optional[Dict[str, Any]]): Optional action parameters. Default: `None`

**Example:**

```python
from pulsar_sdk.models import ActionDescription

# Define an action
action = ActionDescription(
    description="Click the submit button",
    parameters={
        "selector": "button.submit",
        "wait_for_navigation": True
    }
)

print(f"Action: {action.description}")
print(f"Parameters: {action.parameters}")
```

### PageEventHandlers

Placeholder for page event handlers (future implementation).

```python
class PageEventHandlers:
    _browse_event_handlers: Dict[str, Any]
    _load_event_handlers: Dict[str, Any]
    _crawl_event_handlers: Dict[str, Any]
```

**Properties:**

- `browse_event_handlers`: Browse event handlers dictionary
- `load_event_handlers`: Load event handlers dictionary
- `crawl_event_handlers`: Crawl event handlers dictionary

**Note:** This class is a placeholder for future event-driven functionality similar to the Kotlin PageEventHandlers interface.

**Example:**

```python
from pulsar_sdk.models import PageEventHandlers

# Create event handlers (placeholder functionality)
handlers = PageEventHandlers()

# Will be implemented in future:
# - onWillNavigate
# - onDocumentSteady
# - onLoaded
# - onHTMLDocumentParsed
# - etc.
```

## Usage Examples

### Working with WebPage

```python
from pulsar_sdk import PulsarClient, PulsarSession

client = PulsarClient()
client.create_session()
session = PulsarSession(client)

# Load a page
page = session.load("https://example.com")

# Check if page loaded successfully
if page.is_nil:
    print("Failed to load page")
else:
    print(f"Loaded: {page.url}")
    print(f"Final location: {page.location}")
    print(f"Content type: {page.content_type}")
    
    if page.html:
        # Process HTML
        from bs4 import BeautifulSoup
        soup = BeautifulSoup(page.html, 'html.parser')
        title = soup.find('title')
        print(f"Title: {title.text if title else 'N/A'}")
```

### Working with Agent Results

```python
from pulsar_sdk import PulsarClient, AgenticSession

client = PulsarClient()
client.create_session()
session = AgenticSession(client)

session.open("https://example.com")

# Single action
act_result = session.act("click the search button")
if act_result.success:
    print(f"Action completed: {act_result.message}")
    print(f"Is complete: {act_result.is_complete}")

# Multi-step task
run_result = session.run("search for python and extract results")
if run_result.success:
    print(f"Task completed: {run_result.message}")
    print(f"History entries: {run_result.history_size}")
    print(f"Trace entries: {run_result.process_trace_size}")
    
    if run_result.final_result:
        print(f"Final result: {run_result.final_result}")
```

### Working with Observations

```python
from pulsar_sdk import AgenticSession

session = AgenticSession(client)
session.open("https://example.com/form")

# Observe page
observation = session.observe(instruction="find all form fields")

# Process observations
form_fields = []
for obs in observation.observations:
    if obs.locator and obs.description:
        form_fields.append({
            'selector': obs.locator,
            'description': obs.description,
            'action': obs.method
        })

print(f"Found {len(form_fields)} form fields:")
for field in form_fields:
    print(f"  {field['description']}: {field['selector']}")
```

### Structured Extraction

```python
from pulsar_sdk import AgenticSession

session = AgenticSession(client)
session.open("https://example.com/products")

# Define extraction schema
schema = {
    "type": "array",
    "items": {
        "type": "object",
        "properties": {
            "name": {"type": "string"},
            "price": {"type": "number"},
            "rating": {"type": "number"},
            "available": {"type": "boolean"}
        }
    }
}

# Extract with schema
result = session.agent_extract(
    instruction="extract all products",
    schema=schema
)

if result.success and result.data:
    products = result.data
    print(f"Extracted {len(products)} products")
    
    for product in products:
        print(f"{product['name']}: ${product['price']}")
        print(f"  Rating: {product['rating']}/5")
        print(f"  Available: {product['available']}")
```

## Type Checking

All models support type checking with mypy and other type checkers:

```python
from pulsar_sdk.models import WebPage, AgentActResult
from typing import Optional

def process_page(page: WebPage) -> Optional[str]:
    """Process a web page and return its title."""
    if page.is_nil:
        return None
    return page.html

def handle_action(result: AgentActResult) -> bool:
    """Handle an action result."""
    if not result.success:
        print(f"Action failed: {result.message}")
        return False
    
    print(f"Action succeeded: {result.action}")
    return True
```

## JSON Serialization

All models with `from_dict()` methods can be created from JSON responses:

```python
import json
from pulsar_sdk.models import WebPage, AgentActResult

# Parse JSON response
response_json = '{"url": "https://example.com", "contentType": "text/html"}'
data = json.loads(response_json)

# Create model from dict
page = WebPage.from_dict(data)
print(f"URL: {page.url}")
print(f"Content type: {page.content_type}")
```

## Kotlin-Style Compatibility

All models provide Kotlin-style property aliases for compatibility:

```python
from pulsar_sdk.models import WebPage, AgentRunResult

page = WebPage(url="https://example.com", content_type="text/html")

# Python-style (snake_case)
print(page.content_type)

# Kotlin-style (camelCase)
print(page.contentType)

result = AgentRunResult(history_size=5, final_result="done")

# Python-style
print(result.history_size)
print(result.final_result)

# Kotlin-style
print(result.historySize)
print(result.finalResult)
```

## Best Practices

1. **Check Success Status**: Always check the `success` field before using result data

```python
result = session.act("click button")
if result.success:
    # Use result.data
    pass
```

2. **Check for None Values**: Optional fields may be None

```python
page = session.load("https://example.com")
if page.html is not None:
    # Process HTML
    pass
```

3. **Use is_nil for Validation**: Check `is_nil` for WebPage and NormURL

```python
page = session.load(url)
if not page.is_nil:
    # Page is valid
    pass
```

4. **Type Annotations**: Use type hints for clarity

```python
from pulsar_sdk.models import WebPage
from typing import List

def get_page_titles(pages: List[WebPage]) -> List[str]:
    return [page.url for page in pages if not page.is_nil]
```

## See Also

- [PulsarClient API](client.md) - HTTP client
- [PulsarSession API](session.md) - Session management
- [AgenticSession API](agentic-session.md) - AI-powered automation
- [WebDriver API](webdriver.md) - Browser control

---

[← Back to WebDriver](webdriver.md) | [Back to API Reference](../api-reference/)
