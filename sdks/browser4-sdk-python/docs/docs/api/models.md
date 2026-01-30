# Data Models

::: browser4.models

## Overview

The Browser4 Python SDK includes comprehensive data models that represent API responses and domain objects. These models use Python's `@dataclass` decorator for clean, type-safe data structures.

All models provide `from_dict()` class methods for easy construction from API JSON responses.

## Core Models

### WebPage

::: browser4.models.WebPage

Represents a web page result from load/open operations.

**Fields:**

- `url` (`str`): Page URL
- `location` (`Optional[str]`): Final location after redirects
- `content_type` (`Optional[str]`): Content-Type header value
- `content_length` (`int`): Content length in bytes
- `protocol_status` (`Optional[str]`): HTTP protocol status
- `is_nil` (`bool`): Whether the page is nil/empty
- `html` (`Optional[str]`): Page HTML content

**Example:**

```python
page = session.load("https://example.com", "-parse")
print(f"URL: {page.url}")
print(f"Type: {page.content_type}")
print(f"Size: {page.content_length} bytes")
print(f"Has HTML: {page.html is not None}")
```

### NormURL

::: browser4.models.NormURL

Normalized URL with parsed load arguments.

**Fields:**

- `spec` (`str`): Original URL specification
- `url` (`str`): Normalized URL
- `args` (`Optional[str]`): Load arguments string
- `is_nil` (`bool`): Whether the URL is nil/invalid

**Example:**

```python
norm = session.normalize("https://example.com", args="-expire 1d")
print(f"Original: {norm.spec}")
print(f"Normalized: {norm.url}")
print(f"Arguments: {norm.args}")
```

### ElementRef

::: browser4.models.ElementRef

Reference to a DOM element (WebDriver element identifier).

**Fields:**

- `element_id` (`str`): WebDriver element ID

**Example:**

```python
ref = ElementRef(element_id="abc-123-def")
```

## Agent Result Models

### AgentRunResult

::: browser4.models.AgentRunResult

Result from `agent_run()` operation (autonomous task execution).

**Fields:**

- `success` (`bool`): Whether the task completed successfully
- `message` (`str`): Result message or error description
- `history_size` (`int`): Number of steps in execution history
- `process_trace_size` (`int`): Size of the process trace
- `final_result` (`Any`): Final result of the task
- `trace` (`Optional[List[str]]`): Execution trace lines

**Properties:**

- `finalResult`: Kotlin-style alias for `final_result`

**Example:**

```python
result = session.run("search for python and extract top 3 results")
if result.success:
    print(f"Task completed in {result.history_size} steps")
    print(f"Result: {result.final_result}")
else:
    print(f"Task failed: {result.message}")

# View trace
for line in result.trace or []:
    print(line)
```

### AgentActResult

::: browser4.models.AgentActResult

Result from `agent_act()` operation (single action execution).

**Fields:**

- `success` (`bool`): Whether the action succeeded
- `message` (`str`): Result message
- `action` (`Optional[str]`): The executed action description
- `is_complete` (`bool`): Whether the action completed
- `expression` (`Optional[str]`): JavaScript expression executed
- `result` (`Any`): Action result value
- `trace` (`Optional[List[str]]`): Execution trace

**Example:**

```python
result = session.act("click the login button")
if result.success:
    print(f"Action: {result.action}")
    print(f"Complete: {result.is_complete}")
    print(f"Result: {result.result}")
else:
    print(f"Action failed: {result.message}")
```

### AgentObservation

::: browser4.models.AgentObservation

Collection of observation results from `agent_observe()`.

**Fields:**

- `observations` (`List[ObserveResult]`): List of observation results

**Example:**

```python
observation = session.observe("find interactive elements")
for obs in observation.observations:
    print(f"Element: {obs.locator}")
    print(f"Description: {obs.description}")
    if obs.next_goal:
        print(f"Suggested: {obs.next_goal}")
```

### ObserveResult

::: browser4.models.ObserveResult

Single observation result describing a page element or state.

**Fields:**

- `locator` (`Optional[str]`): Element locator/selector
- `domain` (`Optional[str]`): Action domain
- `method` (`Optional[str]`): Suggested method
- `arguments` (`Optional[Dict[str, Any]]`): Method arguments
- `description` (`Optional[str]`): Element description
- `screenshot_content_summary` (`Optional[str]`): Screenshot content summary
- `current_page_content_summary` (`Optional[str]`): Page content summary
- `next_goal` (`Optional[str]`): Suggested next goal
- `thinking` (`Optional[str]`): Agent reasoning
- `summary` (`Optional[str]`): Observation summary
- `key_findings` (`Optional[str]`): Key findings from observation
- `next_suggestions` (`Optional[List[str]]`): List of next action suggestions

**Example:**

```python
observation = session.observe()
for obs in observation.observations:
    if obs.description:
        print(f"Found: {obs.description}")
    if obs.next_goal:
        print(f"Next: {obs.next_goal}")
```

### ExtractionResult

::: browser4.models.ExtractionResult

Result from AI-powered `agent_extract()` operation.

**Fields:**

- `success` (`bool`): Whether extraction succeeded
- `message` (`str`): Result message
- `data` (`Any`): Extracted data (structure depends on schema)

**Example:**

```python
result = session.agent_extract(
    "extract product details",
    schema={"type": "object", "properties": {"name": {"type": "string"}}}
)

if result.success:
    print(f"Extracted: {result.data}")
else:
    print(f"Extraction failed: {result.message}")
```

## State & History Models

### AgentState

::: browser4.models.AgentState

Single state in agent execution history.

**Fields:**

- `step` (`int`): Step number
- `action` (`Optional[str]`): Action description
- `result` (`Any`): Action result
- `success` (`bool`): Whether step succeeded
- `message` (`str`): Step message

**Example:**

```python
history = session.state_history
for state in history.states:
    status = "✓" if state.success else "✗"
    print(f"{status} Step {state.step}: {state.action}")
```

### AgentHistory

::: browser4.models.AgentHistory

Complete agent execution history.

**Fields:**

- `states` (`List[AgentState]`): List of execution states
- `has_errors` (`bool`): Whether any errors occurred
- `final_result` (`Any`): Final result of execution

**Properties:**

- `size`: Number of states in history

**Example:**

```python
session.act("click button1")
session.act("click button2")

history = session.state_history
print(f"Executed {history.size} actions")
print(f"Has errors: {history.has_errors}")

for state in history.states:
    print(f"Step {state.step}: {state.action} - {state.success}")
```

### ChatResponse

::: browser4.models.ChatResponse

Response from LLM chat operations.

**Fields:**

- `content` (`str`): Response content text
- `role` (`str`): Message role (default: "assistant")
- `model` (`Optional[str]`): Model used for generation

**Example:**

```python
response = session.chat("What is web scraping?")
print(f"Role: {response.role}")
print(f"Content: {response.content}")
if response.model:
    print(f"Model: {response.model}")
```

## Extraction Models

### FieldsExtraction

::: browser4.models.FieldsExtraction

Result of field extraction with CSS selectors.

**Fields:**

- `fields` (`Dict[str, Any]`): Extracted field values

**Example:**

```python
extraction = FieldsExtraction(fields={
    "title": "Example Page",
    "price": "$19.99",
    "stock": "In Stock"
})
```

## Tool & Action Models

### ToolCallResult

::: browser4.models.ToolCallResult

Result of a tool call execution.

**Fields:**

- `success` (`bool`): Whether tool call succeeded
- `message` (`str`): Result message
- `data` (`Any`): Tool call result data

**Example:**

```python
result = ToolCallResult(
    success=True,
    message="Tool executed successfully",
    data={"result": 42}
)
```

### ActionDescription

::: browser4.models.ActionDescription

Description of an action to be performed.

**Fields:**

- `description` (`str`): Action description
- `parameters` (`Optional[Dict[str, Any]]`): Action parameters

**Example:**

```python
action = ActionDescription(
    description="click the submit button",
    parameters={"selector": "button.submit"}
)
```

## Event System

### PageEventHandlers

::: browser4.models.PageEventHandlers

Placeholder for page event handlers (future implementation).

This class will support event-driven page interactions similar to the Kotlin PageEventHandlers interface.

**Planned Features:**

- Browse event handlers (onWillNavigate, onDocumentSteady, etc.)
- Load event handlers (onLoaded, onHTMLDocumentParsed, etc.)
- Crawl event handlers

**Properties:**

- `browse_event_handlers` (`Dict[str, Any]`): Browse event handlers
- `load_event_handlers` (`Dict[str, Any]`): Load event handlers  
- `crawl_event_handlers` (`Dict[str, Any]`): Crawl event handlers

**Example:**

```python
handlers = PageEventHandlers()
# Future: handlers.browse_event_handlers['onDocumentSteady'] = callback
```

## Using Models

### Creating from API Responses

All models provide `from_dict()` class methods:

```python
# From API response dict
page_data = {"url": "https://example.com", "html": "<html>...</html>"}
page = WebPage.from_dict(page_data)

result_data = {"success": True, "message": "Done", "finalResult": {"key": "value"}}
result = AgentRunResult.from_dict(result_data)
```

### Type Checking

Models use Python type hints for IDE support:

```python
def process_page(page: WebPage) -> str:
    if page.html:
        return f"Page has {len(page.html)} bytes of HTML"
    return "No HTML content"

def handle_result(result: AgentRunResult) -> None:
    if result.success:
        print(f"Success: {result.final_result}")
    else:
        print(f"Failed: {result.message}")
```

### Pattern Matching (Python 3.10+)

```python
match result:
    case AgentRunResult(success=True, final_result=data):
        print(f"Task succeeded with: {data}")
    case AgentRunResult(success=False, message=error):
        print(f"Task failed: {error}")
```

## Model Import

```python
# Import specific models
from browser4 import WebPage, NormURL, AgentRunResult, AgentActResult

# Import all models
from browser4.models import (
    WebPage,
    NormURL,
    ElementRef,
    FieldsExtraction,
    AgentRunResult,
    AgentActResult,
    AgentObservation,
    ObserveResult,
    ExtractionResult,
    ToolCallResult,
    ActionDescription,
    AgentState,
    AgentHistory,
    ChatResponse,
    PageEventHandlers
)

# Or import entire module
import browser4.models
```

## Complete Example

```python
from browser4 import (
    PulsarClient,
    AgenticSession,
    WebPage,
    AgentRunResult,
    AgentHistory
)

client = PulsarClient()
client.create_session()
session = AgenticSession(client)

# Load page
page: WebPage = session.load("https://example.com", "-parse")
print(f"Loaded: {page.url}")
print(f"Content-Type: {page.content_type}")

# Execute agent task
result: AgentRunResult = session.run("find and click the search button")

if result.success:
    print(f"Task completed in {result.history_size} steps")
    
    # Review history
    history: AgentHistory = session.state_history
    for state in history.states:
        print(f"Step {state.step}: {state.action} - {'✓' if state.success else '✗'}")
else:
    print(f"Task failed: {result.message}")

session.close()
```

## See Also

- [AgenticSession](agentic-session.md) - Uses Agent* models
- [PulsarSession](pulsar-session.md) - Uses WebPage, NormURL
- [WebDriver](webdriver.md) - Uses ElementRef
- [API Overview](overview.md) - Complete API reference
