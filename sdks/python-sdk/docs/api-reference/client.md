# PulsarClient API Reference

The `PulsarClient` class is the low-level HTTP client that communicates with the Browser4 REST API. It handles session management and HTTP requests.

## Class: PulsarClient

```python
from pulsar_sdk import PulsarClient
```

### Constructor

```python
PulsarClient(
    base_url: str = "http://localhost:8182",
    timeout: float = 30.0,
    session_id: Optional[str] = None,
    default_headers: Optional[Dict[str, str]] = None
)
```

**Parameters:**

- `base_url` (str): Base URL of the Browser4 server. Default: `"http://localhost:8182"`
- `timeout` (float): Request timeout in seconds. Default: `30.0`
- `session_id` (Optional[str]): Initial session ID if reconnecting. Default: `None`
- `default_headers` (Optional[Dict[str, str]]): Custom HTTP headers. Default: `None`

**Example:**

```python
# Basic client
client = PulsarClient()

# Custom configuration
client = PulsarClient(
    base_url="http://remote-server:8182",
    timeout=60.0,
    default_headers={
        "Authorization": "Bearer token123",
        "X-API-Key": "your-api-key"
    }
)
```

## Session Management

### create_session()

Create a new browser session on the server.

```python
create_session(capabilities: Optional[Dict[str, Any]] = None) -> str
```

**Parameters:**

- `capabilities` (Optional[Dict[str, Any]]): Browser capabilities (e.g., `{"browserName": "chrome"}`). Default: `None`

**Returns:**

- `str`: The session ID

**Example:**

```python
client = PulsarClient()
session_id = client.create_session()
print(f"Created session: {session_id}")

# With capabilities
session_id = client.create_session(capabilities={
    "browserName": "chrome",
    "browserVersion": "latest"
})
```

### delete_session()

Delete the current session and free resources.

```python
delete_session(session_id: Optional[str] = None) -> None
```

**Parameters:**

- `session_id` (Optional[str]): Session ID to delete. If `None`, uses the client's session ID. Default: `None`

**Example:**

```python
# Delete current session
client.delete_session()

# Delete specific session
client.delete_session(session_id="abc123")
```

### get_session_status()

Get information about a session.

```python
get_session_status(session_id: Optional[str] = None) -> Dict[str, Any]
```

**Parameters:**

- `session_id` (Optional[str]): Session ID to query. If `None`, uses the client's session ID. Default: `None`

**Returns:**

- `Dict[str, Any]`: Session status information

**Example:**

```python
status = client.get_session_status()
print(f"Session ready: {status.get('ready')}")
```

## Page Operations

### open_url()

Open a URL immediately (bypass cache).

```python
open_url(url: str, session_id: Optional[str] = None) -> Dict[str, Any]
```

**Parameters:**

- `url` (str): The URL to open
- `session_id` (Optional[str]): Session ID. If `None`, uses the client's session ID. Default: `None`

**Returns:**

- `Dict[str, Any]`: Page data including URL, content, etc.

**Example:**

```python
page = client.open_url("https://example.com")
print(f"Opened: {page['url']}")
```

### load_url()

Load a URL (use cache if available).

```python
load_url(url: str, args: str = "", session_id: Optional[str] = None) -> Dict[str, Any]
```

**Parameters:**

- `url` (str): The URL to load
- `args` (str): Load arguments (e.g., `"-expire 1d"`). Default: `""`
- `session_id` (Optional[str]): Session ID. Default: `None`

**Returns:**

- `Dict[str, Any]`: Page data

**Example:**

```python
# Load with cache
page = client.load_url("https://example.com", args="-expire 1h")

# Force reload
page = client.load_url("https://example.com", args="-refresh")
```

### submit_url()

Submit a URL for asynchronous processing.

```python
submit_url(url: str, args: str = "", session_id: Optional[str] = None) -> Dict[str, Any]
```

**Parameters:**

- `url` (str): The URL to submit
- `args` (str): Load arguments. Default: `""`
- `session_id` (Optional[str]): Session ID. Default: `None`

**Returns:**

- `Dict[str, Any]`: Submission result

**Example:**

```python
# Submit for background processing
result = client.submit_url("https://example.com/page1")
result = client.submit_url("https://example.com/page2", args="-expire 1d")
```

### normalize_url()

Normalize a URL with arguments.

```python
normalize_url(url: str, args: str = "", session_id: Optional[str] = None) -> Dict[str, Any]
```

**Parameters:**

- `url` (str): The URL to normalize
- `args` (str): Load arguments. Default: `""`
- `session_id` (Optional[str]): Session ID. Default: `None`

**Returns:**

- `Dict[str, Any]`: Normalized URL data

**Example:**

```python
norm = client.normalize_url("https://example.com", args="-expire 1d")
print(f"Normalized: {norm['spec']}")
```

## WebDriver Operations

### navigate()

Navigate the browser to a URL.

```python
navigate(url: str, session_id: Optional[str] = None) -> None
```

**Parameters:**

- `url` (str): The URL to navigate to
- `session_id` (Optional[str]): Session ID. Default: `None`

**Example:**

```python
client.navigate("https://example.com")
```

### get_current_url()

Get the current browser URL.

```python
get_current_url(session_id: Optional[str] = None) -> str
```

**Parameters:**

- `session_id` (Optional[str]): Session ID. Default: `None`

**Returns:**

- `str`: Current URL

**Example:**

```python
url = client.get_current_url()
print(f"Current URL: {url}")
```

### click_element()

Click an element identified by CSS selector.

```python
click_element(selector: str, session_id: Optional[str] = None) -> None
```

**Parameters:**

- `selector` (str): CSS selector
- `session_id` (Optional[str]): Session ID. Default: `None`

**Example:**

```python
client.click_element("button.submit")
```

### fill_element()

Fill an input field with text.

```python
fill_element(selector: str, text: str, session_id: Optional[str] = None) -> None
```

**Parameters:**

- `selector` (str): CSS selector
- `text` (str): Text to fill
- `session_id` (Optional[str]): Session ID. Default: `None`

**Example:**

```python
client.fill_element("input[name='email']", "user@example.com")
```

## Agent Operations

### agent_act()

Execute a single AI-powered action.

```python
agent_act(instruction: str, session_id: Optional[str] = None) -> Dict[str, Any]
```

**Parameters:**

- `instruction` (str): Natural language instruction
- `session_id` (Optional[str]): Session ID. Default: `None`

**Returns:**

- `Dict[str, Any]`: Action result

**Example:**

```python
result = client.agent_act("click the search button")
print(f"Success: {result['success']}")
```

### agent_run()

Execute a multi-step AI-powered task.

```python
agent_run(instruction: str, session_id: Optional[str] = None) -> Dict[str, Any]
```

**Parameters:**

- `instruction` (str): Natural language task description
- `session_id` (Optional[str]): Session ID. Default: `None`

**Returns:**

- `Dict[str, Any]`: Task execution history

**Example:**

```python
history = client.agent_run("search for 'python' and click first result")
print(f"Completed: {history['success']}")
```

## Error Handling

The client raises standard Python exceptions:

- `requests.exceptions.ConnectionError`: Cannot connect to server
- `requests.exceptions.Timeout`: Request timed out
- `requests.exceptions.HTTPError`: HTTP error (4xx, 5xx)
- `ValueError`: Invalid parameters

**Example:**

```python
import requests
from pulsar_sdk import PulsarClient

client = PulsarClient()

try:
    session_id = client.create_session()
except requests.exceptions.ConnectionError:
    print("Cannot connect to Browser4 server")
except requests.exceptions.Timeout:
    print("Request timed out")
except requests.exceptions.HTTPError as e:
    print(f"HTTP error: {e.response.status_code}")
```

## Properties

### base_url

The base URL of the Browser4 server.

```python
client.base_url  # "http://localhost:8182"
```

### timeout

The request timeout in seconds.

```python
client.timeout  # 30.0
```

### session_id

The current session ID (may be `None`).

```python
client.session_id  # "abc123" or None
```

## Advanced Usage

### Custom Headers

Set custom headers for all requests:

```python
client = PulsarClient(default_headers={
    "Authorization": "Bearer token",
    "X-Custom-Header": "value"
})
```

### Session Reuse

Reconnect to an existing session:

```python
# Save session ID
session_id = client.create_session()
print(f"Session ID: {session_id}")

# Later, reconnect
client2 = PulsarClient(session_id=session_id)
status = client2.get_session_status()
```

### Timeout Configuration

Adjust timeout for long-running operations:

```python
# 60-second timeout
client = PulsarClient(timeout=60.0)

# For specific operations, handle at the requests level
# (currently not exposed, but can be added if needed)
```

## See Also

- [PulsarSession API](session.md) - High-level session management
- [AgenticSession API](agentic-session.md) - AI-powered automation
- [WebDriver API](webdriver.md) - Browser control

---

[← Back to API Reference](../api-reference/client.md) | [Next: PulsarSession →](session.md)
