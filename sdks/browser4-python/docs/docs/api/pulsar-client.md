# PulsarClient

::: browser4.client.PulsarClient

## Overview

`PulsarClient` is a thin HTTP client wrapper over the Browser4 REST API. It handles session management, request formatting, and response parsing, providing a low-level interface for communicating with the Browser4 server.

## Key Features

- **Session Management**: Create and delete WebDriver-compatible sessions
- **Request Handling**: Automatic header management and JSON encoding
- **Response Parsing**: Automatic JSON decoding and WebDriver value unwrapping
- **Timeout Control**: Configurable request timeouts
- **Path Templating**: Automatic session ID substitution in URL paths

## Class Definition

```python
class PulsarClient:
    """
    Thin HTTP client over the Browser4 OpenAPI.
    
    Args:
        base_url: Base URL of the Browser4 server (default: http://localhost:8182)
        timeout: Request timeout in seconds (default: 30.0)
        session_id: Initial session ID (default: None, call create_session())
        default_headers: Custom headers to include in all requests
    """
```

## Constructor

### `__init__`

```python
def __init__(
    self,
    base_url: str = "http://localhost:8182",
    timeout: float = 30.0,
    session_id: Optional[str] = None,
    default_headers: Optional[Dict[str, str]] = None
)
```

**Parameters:**

- **base_url** (`str`): Server base URL. Default: `http://localhost:8182`
- **timeout** (`float`): Request timeout in seconds. Default: `30.0`
- **session_id** (`Optional[str]`): Existing session ID. Default: `None`
- **default_headers** (`Optional[Dict[str, str]]`): Custom HTTP headers

**Example:**

```python
from browser4 import PulsarClient

# Default configuration
client = PulsarClient()

# Custom server and timeout
client = PulsarClient(
    base_url="http://localhost:9090",
    timeout=60.0
)

# With existing session ID
client = PulsarClient(session_id="abc-123-def")

# With custom headers
client = PulsarClient(
    default_headers={"X-Custom-Header": "value"}
)
```

## Properties

### `base_url`

```python
base_url: str
```

The base URL of the Browser4 server.

**Example:**

```python
client = PulsarClient(base_url="http://localhost:8182")
print(client.base_url)  # http://localhost:8182
```

### `timeout`

```python
timeout: float
```

Default timeout for HTTP requests in seconds.

**Example:**

```python
client = PulsarClient(timeout=60.0)
print(client.timeout)  # 60.0
```

### `session_id`

```python
session_id: Optional[str]
```

Current session ID. Set by `create_session()` or constructor.

**Example:**

```python
client = PulsarClient()
print(client.session_id)  # None

client.create_session()
print(client.session_id)  # e.g., "abc-123-def-456"
```

## Session Management

### `create_session`

```python
def create_session(self, capabilities: Optional[Dict[str, Any]] = None) -> str
```

Creates a new WebDriver-compatible session with the Browser4 server.

**Parameters:**

- **capabilities** (`Optional[Dict[str, Any]]`): WebDriver capabilities. Default: `{}`

**Returns:** Session ID string

**Raises:**

- `RuntimeError`: If server response doesn't contain sessionId

**Example:**

```python
from browser4 import PulsarClient

client = PulsarClient()
session_id = client.create_session()
print(f"Created session: {session_id}")

# With capabilities
session_id = client.create_session(
    capabilities={"browserName": "chrome"}
)
```

### `delete_session`

```python
def delete_session(self, session_id: Optional[str] = None) -> None
```

Deletes a session, releasing server resources.

**Parameters:**

- **session_id** (`Optional[str]`): Session ID to delete. If `None`, uses `self.session_id`

**Raises:**

- `ValueError`: If no session ID is available

**Example:**

```python
client = PulsarClient()
client.create_session()

# Use the session...

# Delete the current session
client.delete_session()

# Or delete a specific session
client.delete_session(session_id="abc-123")
```

## Generic Request Methods

### `post`

```python
def post(
    self, 
    path: str, 
    body: Dict[str, Any], 
    session_id: Optional[str] = None
) -> Any
```

Send a POST request to the server.

**Parameters:**

- **path** (`str`): API endpoint path (e.g., `/session/{sessionId}/url`)
- **body** (`Dict[str, Any]`): Request body (will be JSON-encoded)
- **session_id** (`Optional[str]`): Override session ID for this request

**Returns:** Response data (JSON decoded, WebDriver value unwrapped)

**Raises:**

- `requests.exceptions.HTTPError`: On HTTP error status
- `ValueError`: If session ID is required but not available

**Example:**

```python
client = PulsarClient()
client.create_session()

# Navigate to URL
result = client.post(
    "/session/{sessionId}/url",
    {"url": "https://example.com"}
)

# Click an element
result = client.post(
    "/session/{sessionId}/selectors/click",
    {"selector": "button.submit", "strategy": "css"}
)
```

### `get`

```python
def get(self, path: str, session_id: Optional[str] = None) -> Any
```

Send a GET request to the server.

**Parameters:**

- **path** (`str`): API endpoint path
- **session_id** (`Optional[str]`): Override session ID for this request

**Returns:** Response data

**Example:**

```python
client = PulsarClient()
client.create_session()

# Get current URL
current_url = client.get("/session/{sessionId}/url")
print(f"Current URL: {current_url}")

# Get page title
title = client.get("/session/{sessionId}/title")
```

### `delete`

```python
def delete(self, path: str, session_id: Optional[str] = None) -> Any
```

Send a DELETE request to the server.

**Parameters:**

- **path** (`str`): API endpoint path
- **session_id** (`Optional[str]`): Override session ID for this request

**Returns:** Response data

**Example:**

```python
client = PulsarClient()
client.create_session()

# Delete session using generic delete method
client.delete(f"/session/{client.session_id}")
```

## Resource Management

### `close`

```python
def close(self) -> None
```

Closes the underlying HTTP session. Call this when done with the client.

**Example:**

```python
client = PulsarClient()
try:
    client.create_session()
    # Use client...
finally:
    client.close()
```

## Complete Examples

### Basic Session Workflow

```python
from browser4 import PulsarClient

# Create client
client = PulsarClient(base_url="http://localhost:8182")

try:
    # Create session
    session_id = client.create_session()
    print(f"Session created: {session_id}")
    
    # Navigate to page
    client.post("/session/{sessionId}/url", {"url": "https://example.com"})
    
    # Get current URL
    url = client.get("/session/{sessionId}/url")
    print(f"Current URL: {url}")
    
    # Click element
    client.post(
        "/session/{sessionId}/selectors/click",
        {"selector": "button#submit", "strategy": "css"}
    )
    
finally:
    # Cleanup
    client.delete_session()
    client.close()
```

### With Browser4Driver

```python
from browser4 import Browser4Driver, PulsarClient

with Browser4Driver() as driver:
    client = PulsarClient(base_url=driver.base_url)
    
    try:
        session_id = client.create_session()
        
        # Use the client for low-level API access
        client.post("/session/{sessionId}/url", {"url": "https://example.com"})
        
        # Extract page title
        title = client.get("/session/{sessionId}/title")
        print(f"Title: {title}")
        
    finally:
        client.delete_session()
        client.close()
```

### Multiple Sessions

```python
from browser4 import PulsarClient

client = PulsarClient()

# Create first session
session1 = client.create_session()
print(f"Session 1: {session1}")

# Create second session (client tracks current session)
session2 = client.create_session()
print(f"Session 2: {session2}")

# Make requests to specific sessions
client.post(
    "/session/{sessionId}/url",
    {"url": "https://example.com"},
    session_id=session1
)

client.post(
    "/session/{sessionId}/url", 
    {"url": "https://google.com"},
    session_id=session2
)

# Cleanup
client.delete_session(session_id=session1)
client.delete_session(session_id=session2)
client.close()
```

### Error Handling

```python
from browser4 import PulsarClient
import requests

client = PulsarClient()

try:
    session_id = client.create_session()
    
    # Try to click non-existent element
    try:
        client.post(
            "/session/{sessionId}/selectors/click",
            {"selector": "button#nonexistent", "strategy": "css"}
        )
    except requests.exceptions.HTTPError as e:
        print(f"Click failed: {e}")
        
except RuntimeError as e:
    print(f"Failed to create session: {e}")
finally:
    if client.session_id:
        client.delete_session()
    client.close()
```

### Custom Timeout

```python
from browser4 import PulsarClient

# Short timeout for fast operations
client = PulsarClient(timeout=5.0)
session_id = client.create_session()

# Long-running operations may need higher timeout
client.timeout = 120.0
result = client.post(
    "/session/{sessionId}/agent/run",
    {"task": "complex multi-step task"}
)

client.delete_session()
client.close()
```

## Response Format

### WebDriver Value Unwrapping

PulsarClient automatically unwraps WebDriver-style responses:

```python
# Server returns: {"value": {"sessionId": "abc-123"}}
# Client returns: {"sessionId": "abc-123"}

# Server returns: {"value": "https://example.com"}
# Client returns: "https://example.com"
```

### Null Responses

If the server returns no content, methods return `None`:

```python
result = client.post("/session/{sessionId}/control/stop", {})
# result is None (204 No Content)
```

## Path Templating

PulsarClient automatically replaces `{sessionId}` placeholders in paths:

```python
# You write:
client.post("/session/{sessionId}/url", {"url": "https://example.com"})

# Client sends request to:
# POST /session/abc-123-def-456/url
```

## Integration with Higher-Level Classes

PulsarClient is typically used as the foundation for higher-level abstractions:

- **PulsarSession**: Uses PulsarClient for page loading and extraction
- **AgenticSession**: Uses PulsarClient for AI-powered operations
- **WebDriver**: Uses PulsarClient for browser control

**Example:**

```python
from browser4 import PulsarClient, AgenticSession, WebDriver

client = PulsarClient()
client.create_session()

# High-level session
session = AgenticSession(client)
session.open("https://example.com")

# Low-level driver
driver = WebDriver(client)
driver.click("button.submit")

# Direct client access
client.post("/session/{sessionId}/control/delay", {"ms": 1000})

client.delete_session()
client.close()
```

## See Also

- [Browser4Driver](browser4-driver.md) - Server lifecycle management
- [PulsarSession](pulsar-session.md) - High-level session operations
- [AgenticSession](agentic-session.md) - AI-powered automation
- [WebDriver](webdriver.md) - Browser control interface
- [API Overview](overview.md) - Complete API reference
