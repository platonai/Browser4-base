# Browser4Driver

::: browser4.driver.Browser4Driver

## Overview

`Browser4Driver` manages the lifecycle of a local Browser4.jar process. It handles downloading the JAR file from GitHub releases, starting the Java process, health checking, and graceful shutdown.

## Key Features

- **Automatic Download**: Downloads Browser4.jar from GitHub releases if not present
- **Lifecycle Management**: Starts, monitors, and stops the Browser4 server process
- **Health Checking**: Waits for server readiness before returning control
- **Context Manager**: Supports Python `with` statement for automatic cleanup
- **Port Configuration**: Configurable server port (default: 8182)
- **Java Options**: Pass custom Java system properties

## Class Definition

```python
class Browser4Driver:
    """
    Browser4Driver manages the lifecycle of a local Browser4.jar process.
    
    Args:
        jar_path: Path where Browser4.jar should be stored
                  (default: ~/.browser4/lib/Browser4.jar)
        download_url: URL to download Browser4.jar from
                      (default: GitHub release v4.4.0)
        port: Port for the Browser4 server (default: 8182)
        java_options: Additional Java options for the process
    """
```

## Constructor

### `__init__`

```python
def __init__(
    self,
    jar_path: Optional[str] = None,
    download_url: Optional[str] = None,
    port: int = 8182,
    java_options: Optional[Dict[str, str]] = None
)
```

**Parameters:**

- **jar_path** (`Optional[str]`): Custom path for Browser4.jar. Default: `~/.browser4/lib/Browser4.jar`
- **download_url** (`Optional[str]`): Custom download URL. Default: Latest GitHub release
- **port** (`int`): Server port. Default: `8182`
- **java_options** (`Optional[Dict[str, str]]`): Java system properties (e.g., `{"heap.size": "2g"}`)

**Example:**

```python
# Default configuration
driver = Browser4Driver()

# Custom port
driver = Browser4Driver(port=9090)

# Custom JAR path and Java options
driver = Browser4Driver(
    jar_path="/opt/browser4/Browser4.jar",
    java_options={"spring.profiles.active": "rest,private"}
)
```

## Properties

### `base_url`

```python
@property
def base_url(self) -> str
```

The base URL where the Browser4 server is accessible.

**Returns:** URL string (e.g., `http://localhost:8182`)

**Example:**

```python
driver = Browser4Driver(port=9090)
print(driver.base_url)  # http://localhost:9090
```

### `is_jar_present`

```python
@property
def is_jar_present(self) -> bool
```

Checks if the Browser4.jar file exists at the configured path.

**Returns:** `True` if JAR file exists, `False` otherwise

**Example:**

```python
driver = Browser4Driver()
if not driver.is_jar_present:
    print("JAR will be downloaded on start()")
```

### `is_running`

```python
@property
def is_running(self) -> bool
```

Checks if the Browser4 server process is currently running.

**Returns:** `True` if the process is running, `False` otherwise

**Example:**

```python
driver = Browser4Driver()
print(driver.is_running)  # False

driver.start()
print(driver.is_running)  # True

driver.stop()
print(driver.is_running)  # False
```

## Methods

### `download_if_needed`

```python
def download_if_needed(self) -> None
```

Downloads Browser4.jar from the configured URL if it doesn't exist locally. Includes retry logic and integrity checking.

**Raises:**
- `RuntimeError`: If download fails after all retry attempts

**Example:**

```python
driver = Browser4Driver()
driver.download_if_needed()  # Downloads if not present
```

### `start`

```python
def start(self, wait_for_ready: bool = True) -> None
```

Starts the Browser4 server process. This method:

1. Downloads Browser4.jar if needed
2. Verifies Java is available
3. Checks if port is available
4. Starts the Java process
5. Waits for server to be ready (if `wait_for_ready=True`)

**Parameters:**

- **wait_for_ready** (`bool`): If `True`, waits for server health check. Default: `True`

**Raises:**

- `RuntimeError`: If server is already running, port is in use, Java is not available, or startup fails

**Example:**

```python
driver = Browser4Driver()
driver.start()
print(f"Server ready at {driver.base_url}")

# Start without waiting
driver = Browser4Driver()
driver.start(wait_for_ready=False)
driver.wait_for_server_ready(timeout_seconds=60)
```

### `wait_for_server_ready`

```python
def wait_for_server_ready(self, timeout_seconds: float = 120) -> None
```

Waits for the Browser4 server to be ready by polling the health endpoint.

**Parameters:**

- **timeout_seconds** (`float`): Maximum time to wait. Default: `120`

**Raises:**

- `RuntimeError`: If server doesn't become ready within timeout or process terminates

**Example:**

```python
driver = Browser4Driver()
driver.start(wait_for_ready=False)

# Custom timeout
driver.wait_for_server_ready(timeout_seconds=60)
```

### `is_server_healthy`

```python
def is_server_healthy(self) -> bool
```

Checks if the Browser4 server is healthy by attempting to connect to health endpoints.

**Returns:** `True` if server responds with HTTP 2xx status

**Example:**

```python
driver = Browser4Driver()
driver.start()

if driver.is_server_healthy():
    print("Server is ready to accept requests")
```

### `stop`

```python
def stop(self, force: bool = False) -> None
```

Stops the Browser4 server process.

**Parameters:**

- **force** (`bool`): If `True`, forcibly kills the process. If `False`, attempts graceful shutdown. Default: `False`

**Example:**

```python
driver = Browser4Driver()
driver.start()

# Graceful shutdown (waits up to 10 seconds)
driver.stop()

# Force kill
driver.stop(force=True)
```

### `close`

```python
def close(self) -> None
```

Alias for `stop()`. Closes the driver and stops the server process.

**Example:**

```python
driver = Browser4Driver()
driver.start()
driver.close()  # Same as driver.stop()
```

## Context Manager Support

Browser4Driver supports Python's context manager protocol (`with` statement) for automatic resource cleanup.

### `__enter__` and `__exit__`

**Example:**

```python
from browser4 import Browser4Driver, PulsarClient

# Automatic start and stop
with Browser4Driver() as driver:
    print(f"Server running at {driver.base_url}")
    client = PulsarClient(base_url=driver.base_url)
    # ... use the client ...
# Server automatically stopped here
```

This is the **recommended** way to use Browser4Driver as it ensures proper cleanup even if exceptions occur.

## Complete Examples

### Basic Usage

```python
from browser4 import Browser4Driver, PulsarClient, AgenticSession

# Start the server
with Browser4Driver() as driver:
    # Create client and session
    client = PulsarClient(base_url=driver.base_url)
    session_id = client.create_session()
    session = AgenticSession(client)
    
    # Use the session
    page = session.open("https://example.com")
    print(f"Loaded: {page.url}")
    
    # Cleanup
    session.close()
```

### Custom Configuration

```python
from browser4 import Browser4Driver

# Configure custom port and Java options
driver = Browser4Driver(
    port=9090,
    java_options={
        "spring.profiles.active": "rest,private,advanced",
        "server.compression.enabled": "true"
    }
)

try:
    driver.start()
    print(f"Server started at {driver.base_url}")
    
    # Use the server...
    
finally:
    driver.stop()
```

### Manual Download Control

```python
from browser4 import Browser4Driver

driver = Browser4Driver()

# Check and download manually
if not driver.is_jar_present:
    print("Downloading Browser4.jar...")
    driver.download_if_needed()
    print("Download complete")

# Start the server
driver.start()
print(f"Server ready at {driver.base_url}")

driver.stop()
```

### Error Handling

```python
from browser4 import Browser4Driver

driver = Browser4Driver(port=8182)

try:
    driver.start()
except RuntimeError as e:
    if "port" in str(e).lower():
        print("Port 8182 is already in use")
        # Try alternative port
        driver = Browser4Driver(port=8183)
        driver.start()
    elif "java" in str(e).lower():
        print("Java not found. Please install Java 17+")
    else:
        raise

# Use the driver...
driver.stop()
```

## Constants

- **DEFAULT_DOWNLOAD_URL**: `"https://github.com/platonai/Browser4/releases/download/v4.4.0/Browser4.jar"`
- **LATEST_RELEASE_API**: `"https://api.github.com/repos/platonai/Browser4/releases/latest"`
- **JAR_FILENAME**: `"Browser4.jar"`
- **DEFAULT_PORT**: `8182`
- **STARTUP_TIMEOUT_SECONDS**: `120`
- **HEALTH_CHECK_INTERVAL_MS**: `500`

## Environment Variables

The driver respects these environment variables:

- **SPRING_PROFILES_ACTIVE**: Overrides default Spring profiles (default: `rest,private,advanced`)
- **OPENROUTER_API_KEY**: Automatically passed to server if set

## Troubleshooting

### Port Already in Use

```python
RuntimeError: Port 8182 is already in use. Choose a different port...
```

**Solution:** Use a different port or stop the process using that port:

```python
driver = Browser4Driver(port=8183)
```

### Java Not Found

```python
RuntimeError: Java executable not found on PATH...
```

**Solution:** Install Java 17+ and ensure `java` is in your PATH.

### Download Failures

```python
RuntimeError: Failed to download Browser4.jar (attempt 3/3)...
```

**Solution:** Check network connection, proxy settings, or provide a custom download URL.

## See Also

- [PulsarClient](pulsar-client.md) - HTTP client to communicate with the server
- [AgenticSession](agentic-session.md) - High-level session management
- [API Overview](overview.md) - Complete API reference
