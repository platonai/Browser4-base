# AgenticSession

::: browser4.agentic_session.AgenticSession

## Overview

`AgenticSession` extends `PulsarSession` with AI-powered browser automation capabilities. It combines the data extraction features of PulsarSession with intelligent browser interaction using natural language instructions.

## Key Features

- **All PulsarSession Methods**: Inherits page loading, parsing, and extraction
- **Natural Language Actions**: Execute actions described in plain English
- **Autonomous Tasks**: Run multi-step tasks with observe-act loops
- **Page Observation**: Analyze pages and suggest next actions
- **AI Extraction**: Extract structured data using AI assistance
- **Content Summarization**: Generate intelligent page summaries
- **State History**: Track executed actions for memory and debugging

## Class Definition

```python
class AgenticSession(PulsarSession):
    """
    AgenticSession extends PulsarSession with AI-powered browser automation.
    
    Provides methods for intelligent browser interaction using natural language
    instructions, combining data extraction with AI-powered agent functionality.
    
    Args:
        client: PulsarClient instance for API communication
    """
```

## Constructor

### `__init__`

```python
def __init__(self, client: PulsarClient)
```

**Parameters:**

- **client** (`PulsarClient`): PulsarClient instance

**Example:**

```python
from browser4 import PulsarClient, AgenticSession

client = PulsarClient()
client.create_session()
session = AgenticSession(client)
```

## Properties (Inherited)

AgenticSession inherits all PulsarSession properties:

- `id`, `uuid`, `display` - Session identification
- `is_active` - Session status
- `driver`, `bound_driver` - WebDriver access

## AgenticSession-Specific Properties

### `companion_agent`

```python
@property
def companion_agent(self) -> "AgenticSession"
```

Get the companion agent (returns self for API compatibility with Kotlin).

**Returns:** Self reference

### `state_history` / `stateHistory`

```python
@property
def state_history(self) -> AgentHistory

@property
def stateHistory(self) -> AgentHistory  # Kotlin-style naming
```

Get the agent state history tracking executed actions and results.

**Returns:** `AgentHistory` object with action states

**Example:**

```python
session = AgenticSession(client)
session.act("click the search button")
session.act("type 'python' in the search box")

history = session.state_history
print(f"History size: {history.size}")
for state in history.states:
    print(f"Step {state.step}: {state.action} - Success: {state.success}")
```

### `process_trace` / `processTrace`

```python
@property
def process_trace(self) -> List[str]

@property
def processTrace(self) -> List[str]  # Kotlin-style naming
```

Get the process trace showing detailed execution logs.

**Returns:** List of trace strings

**Example:**

```python
session = AgenticSession(client)
result = session.run("navigate to example.com and click login")

for trace_line in session.process_trace:
    print(trace_line)
```

### `context`

```python
@property
def context(self) -> "AgenticSession"
```

Get the context (returns self for API compatibility).

**Returns:** Self reference

## Agentic Operations

### `act` / `agent_act`

```python
def act(self, action: str, **kwargs: Any) -> AgentActResult

def agent_act(self, action: str, **kwargs: Any) -> AgentActResult
```

Execute a single action described in natural language.

**Parameters:**

- **action** (`str`): Natural language description of the action
- **kwargs**: Additional parameters:
  - `multiAct` (`bool`): Whether each act forms a new chained context
  - `modelName` (`str`): Optional LLM model name
  - `variables` (`Dict`): Extra variables for prompt/tool
  - `domSettleTimeoutMs` (`int`): Timeout for DOM settling
  - `timeoutMs` (`int`): Overall timeout

**Returns:** `AgentActResult` with action execution results

**Example:**

```python
session = AgenticSession(client)
session.driver.navigate_to("https://example.com")

# Simple actions
result = session.act("click the search button")
print(f"Success: {result.success}")
print(f"Message: {result.message}")

# With parameters
result = session.act(
    "fill the search box with 'python'",
    timeoutMs=10000
)

# Multi-step chained actions
result = session.act("click login", multiAct=True)
result = session.act("type username", multiAct=True)
result = session.act("type password", multiAct=True)
```

### `run` / `agent_run`

```python
def run(self, task: str, **kwargs: Any) -> AgentRunResult

def agent_run(self, task: str, **kwargs: Any) -> AgentRunResult
```

Run an autonomous agent task with observe-act loop.

**Parameters:**

- **task** (`str`): Natural language description of the task
- **kwargs**: Additional parameters (same as `act()`)

**Returns:** `AgentRunResult` with task execution results

**Example:**

```python
session = AgenticSession(client)

# Autonomous multi-step task
result = session.run(
    "go to example.com, search for 'python', and click the first result"
)
print(f"Success: {result.success}")
print(f"Message: {result.message}")
print(f"Final result: {result.final_result}")
print(f"History size: {result.history_size}")

# Complex task with timeout
result = session.run(
    "search for 'machine learning' and extract top 5 article titles",
    timeoutMs=60000
)

if result.success:
    print(f"Extracted: {result.final_result}")
```

### `observe` / `agent_observe`

```python
def observe(self, instruction: Optional[str] = None, **kwargs: Any) -> AgentObservation

def agent_observe(self, instruction: Optional[str] = None, **kwargs: Any) -> AgentObservation
```

Observe the page and return potential actions.

**Parameters:**

- **instruction** (`Optional[str]`): Optional observation instruction
- **kwargs**: Additional parameters:
  - `modelName` (`str`): Optional LLM model name
  - `domSettleTimeoutMs` (`int`): Timeout for DOM settling
  - `returnAction` (`bool`): Whether to return actionable tool calls
  - `drawOverlay` (`bool`): Whether to highlight interactive elements

**Returns:** `AgentObservation` with observation results

**Example:**

```python
session = AgenticSession(client)
session.driver.navigate_to("https://example.com")

# General observation
observation = session.observe()
for obs in observation.observations:
    print(f"Found: {obs.description}")
    print(f"Locator: {obs.locator}")
    print(f"Next goal: {obs.next_goal}")

# Targeted observation
observation = session.observe(
    instruction="find all clickable buttons",
    drawOverlay=True
)
```

### `agent_extract`

```python
def agent_extract(
    self,
    instruction: str,
    schema: Optional[Dict[str, Any]] = None,
    selector: Optional[str] = None,
    **kwargs: Any
) -> ExtractionResult
```

Extract structured data from the page using AI.

**Parameters:**

- **instruction** (`str`): Extraction instruction
- **schema** (`Optional[Dict[str, Any]]`): Optional JSON schema for the result
- **selector** (`Optional[str]`): Optional CSS selector to scope extraction
- **kwargs**: Additional parameters

**Returns:** `ExtractionResult` with extracted data

**Example:**

```python
session = AgenticSession(client)
session.driver.navigate_to("https://example.com/products")

# Extract without schema
result = session.agent_extract(
    "extract all product names and prices"
)
print(result.data)

# Extract with schema
schema = {
    "type": "object",
    "properties": {
        "products": {
            "type": "array",
            "items": {
                "type": "object",
                "properties": {
                    "name": {"type": "string"},
                    "price": {"type": "number"}
                }
            }
        }
    }
}

result = session.agent_extract(
    "extract product information",
    schema=schema,
    selector="div.product-list"
)
print(result.data)
```

### `summarize` / `agent_summarize`

```python
def summarize(self, instruction: Optional[str] = None, selector: Optional[str] = None) -> str

def agent_summarize(self, instruction: Optional[str] = None, selector: Optional[str] = None) -> str
```

Summarize page content using AI.

**Parameters:**

- **instruction** (`Optional[str]`): Optional guidance for summarization
- **selector** (`Optional[str]`): Optional CSS selector to limit scope

**Returns:** Summary text string

**Example:**

```python
session = AgenticSession(client)
session.driver.navigate_to("https://example.com/article")

# General summary
summary = session.summarize()
print(summary)

# Targeted summary
summary = session.summarize(
    instruction="summarize the main points",
    selector="article.content"
)
print(summary)

# Custom instruction
summary = session.summarize(
    instruction="provide a technical summary for developers"
)
print(summary)
```

### `clear_history` / `agent_clear_history`

```python
def clear_history(self) -> bool

def agent_clear_history(self) -> bool
```

Clear the agent's history so new tasks remain unaffected by previous ones.

**Returns:** `True` if history was cleared successfully

**Example:**

```python
session = AgenticSession(client)

# Execute some tasks
session.act("click button")
session.run("search for something")

# Check history
print(f"History size: {session.state_history.size}")

# Clear for fresh start
session.clear_history()
print(f"History size after clear: {session.state_history.size}")
```

## Capture Operations

### `capture`

```python
def capture(self, driver: Optional[WebDriver] = None, url: Optional[str] = None) -> WebPage
```

Capture the live page controlled by a WebDriver as a WebPage snapshot.

**Parameters:**

- **driver** (`Optional[WebDriver]`): WebDriver controlling the page (uses bound driver if None)
- **url** (`Optional[str]`): Optional URL to identify the capture

**Returns:** `WebPage` with captured page content

**Example:**

```python
session = AgenticSession(client)
session.driver.navigate_to("https://example.com")
session.act("scroll to bottom")

# Capture current state
page = session.capture()
print(f"Captured: {page.url}")
print(f"HTML length: {len(page.html)}")

# Parse and extract from capture
document = session.parse(page)
data = session.extract(document, {"title": "h1"})
```

## Helper Methods

### `register_closable`

```python
def register_closable(self, closable: Any) -> None
```

Register a closable object with the session for resource management.

**Parameters:**

- **closable** (`Any`): Object with a `close()` method

### `data`

```python
def data(self, name: str, value: Any = None) -> Any
```

Get or set session data.

**Parameters:**

- **name** (`str`): Data key
- **value** (`Any`): Value to set (if provided)

**Returns:** Stored value

### `property`

```python
def property(self, name: str, value: Optional[str] = None) -> Optional[str]
```

Get or set a session property.

**Parameters:**

- **name** (`str`): Property name
- **value** (`Optional[str]`): Value to set (if provided)

**Returns:** Property value

### `options`

```python
def options(self, args: str = "", event_handlers: Optional[PageEventHandlers] = None) -> Dict[str, Any]
```

Create load options from arguments string.

**Parameters:**

- **args** (`str`): Load arguments
- **event_handlers** (`Optional[PageEventHandlers]`): Event handlers

**Returns:** Options dictionary

### `close`

```python
def close(self) -> None
```

Close the session and release all resources.

**Example:**

```python
session = AgenticSession(client)
try:
    # Use session...
    pass
finally:
    session.close()
```

## Complete Examples

### Simple AI Action

```python
from browser4 import PulsarClient, AgenticSession

client = PulsarClient()
client.create_session()
session = AgenticSession(client)

# Navigate and act
session.driver.navigate_to("https://example.com")
result = session.act("click the login button")

if result.success:
    print("Login button clicked successfully")
else:
    print(f"Failed: {result.message}")

session.close()
```

### Autonomous Task Execution

```python
from browser4 import Browser4Driver, PulsarClient, AgenticSession

with Browser4Driver() as driver:
    client = PulsarClient(base_url=driver.base_url)
    client.create_session()
    session = AgenticSession(client)
    
    # Let agent handle the entire workflow
    result = session.run(
        "go to example.com, search for 'python tutorial', "
        "and extract the titles of the top 3 results"
    )
    
    if result.success:
        print(f"Results: {result.final_result}")
        print(f"Took {result.history_size} steps")
    
    # View execution trace
    for trace in session.process_trace:
        print(trace)
    
    session.close()
```

### AI-Powered Data Extraction

```python
from browser4 import PulsarClient, AgenticSession

client = PulsarClient()
client.create_session()
session = AgenticSession(client)

# Navigate to product page
session.driver.navigate_to("https://example.com/products")

# Extract with AI (no selectors needed)
result = session.agent_extract(
    "extract all products with their names, prices, and availability status"
)

if result.success:
    print("Extracted data:")
    print(result.data)
else:
    print(f"Extraction failed: {result.message}")

session.close()
```

### Observe-Act Pattern

```python
from browser4 import PulsarClient, AgenticSession

client = PulsarClient()
client.create_session()
session = AgenticSession(client)

# Navigate to page
session.driver.navigate_to("https://example.com")

# Observe available actions
observation = session.observe("find interactive elements")

print("Available actions:")
for obs in observation.observations:
    print(f"- {obs.description}")
    if obs.next_goal:
        print(f"  Suggested next: {obs.next_goal}")

# Act on observation
if observation.observations:
    first_action = observation.observations[0].description
    result = session.act(first_action)
    print(f"Executed: {result.success}")

session.close()
```

### Multi-Step Workflow with History

```python
from browser4 import PulsarClient, AgenticSession

client = PulsarClient()
client.create_session()
session = AgenticSession(client)

# Step 1: Navigate
session.driver.navigate_to("https://example.com")

# Step 2: Execute actions
session.act("click the search icon")
session.act("type 'python' in the search box")
session.act("press Enter")

# Step 3: Wait and extract
session.driver.delay(2000)
result = session.agent_extract("extract search results")

# Review history
history = session.state_history
print(f"Executed {history.size} steps")

for state in history.states:
    status = "✓" if state.success else "✗"
    print(f"{status} Step {state.step}: {state.action}")

# Clear history for next task
session.clear_history()

session.close()
```

### Content Summarization

```python
from browser4 import PulsarClient, AgenticSession

client = PulsarClient()
client.create_session()
session = AgenticSession(client)

# Navigate to article
session.driver.navigate_to("https://example.com/article")

# Get summary
summary = session.summarize(
    instruction="provide a concise summary in 2-3 sentences"
)
print("Summary:")
print(summary)

# Targeted summary
technical_summary = session.summarize(
    instruction="summarize technical details only",
    selector="section.technical-details"
)
print("\nTechnical Summary:")
print(technical_summary)

session.close()
```

### Combining PulsarSession and AgenticSession

```python
from browser4 import PulsarClient, AgenticSession

client = PulsarClient()
client.create_session()
session = AgenticSession(client)

# Use PulsarSession methods
page = session.load("https://example.com", "-expire 1d -parse")
document = session.parse(page)

# Traditional extraction
basic_data = session.extract(document, {
    "title": "h1",
    "description": "p.description"
})

# AI-powered extraction for complex data
advanced_data = session.agent_extract(
    "extract product specifications and features"
)

# Combine results
print("Basic data:", basic_data)
print("Advanced data:", advanced_data.data)

session.close()
```

## Error Handling

```python
from browser4 import PulsarClient, AgenticSession

client = PulsarClient()
client.create_session()
session = AgenticSession(client)

try:
    # Attempt action
    result = session.act("click the non-existent button")
    
    if not result.success:
        print(f"Action failed: {result.message}")
        
        # Check history for context
        if session.state_history.has_errors:
            print("Previous errors detected in history")
        
        # Retry with different approach
        result = session.run(
            "find and click any visible button"
        )
        
except Exception as e:
    print(f"Unexpected error: {e}")
    
finally:
    session.close()
```

## Best Practices

1. **Clear History Between Tasks**: Use `clear_history()` to prevent context pollution
2. **Use `run()` for Complex Tasks**: Let the agent handle multi-step workflows autonomously
3. **Use `act()` for Specific Actions**: When you need precise control over individual steps
4. **Check `state_history`**: Review execution history for debugging and monitoring
5. **Combine with PulsarSession**: Use traditional extraction for simple cases, AI for complex
6. **Handle Errors Gracefully**: Check `result.success` and `result.message`

## See Also

- [PulsarSession](pulsar-session.md) - Base class with loading and extraction
- [WebDriver](webdriver.md) - Low-level browser control
- [AgentRunResult](models.md#agentrunresult) - Task execution result
- [AgentActResult](models.md#agentactresult) - Action execution result
- [AgentHistory](models.md#agenthistory) - Execution history tracking
- [API Overview](overview.md) - Complete API reference
