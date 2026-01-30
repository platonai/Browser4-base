# Browser4Driver Configuration

This guide covers configuration options for Browser4Driver, which manages the Browser4 server process.

## Overview

`Browser4Driver` automatically downloads, starts, and manages a local Browser4.jar server process. It provides extensive configuration options to customize server behavior.

## Basic Configuration

### Default Configuration

The simplest usage with default settings:

```python
from browser4 import Browser4Driver

# Use defaults: port 8182, auto-download jar, standard Java options
driver = Browser4Driver()
driver.start()

print(f"Server running at: {driver.base_url}")

# Use the server...

driver.stop()
```

### Context Manager (Recommended)

Use a context manager for automatic cleanup:

```python
from browser4 import Browser4Driver

with Browser4Driver() as driver:
    print(f"Server at: {driver.base_url}")
    # Use the server...
# Server stops automatically
```

## Configuration Parameters

### Port Configuration

Specify a custom port for the server:

```python
from browser4 import Browser4Driver

# Use custom port
driver = Browser4Driver(port=8183)
driver.start()

print(f"Server URL: {driver.base_url}")  # http://localhost:8183
```

**Default**: `8182`

**Note**: Ensure the port is not already in use.

### JAR Path Configuration

Specify where Browser4.jar should be stored:

```python
from browser4 import Browser4Driver
from pathlib import Path

# Custom jar location
custom_path = Path.home() / "my-browser4" / "Browser4.jar"

driver = Browser4Driver(jar_path=str(custom_path))
driver.start()
```

**Default**: `~/.browser4/lib/Browser4.jar`

**Note**: The directory will be created automatically if it doesn't exist.

### Download URL Configuration

Specify a custom URL for downloading Browser4.jar:

```python
from browser4 import Browser4Driver

# Use custom download URL
driver = Browser4Driver(
    download_url="https://your-server.com/custom/Browser4.jar"
)
driver.start()
```

**Default**: Latest release from GitHub:
```
https://github.com/platonai/Browser4/releases/download/v4.4.0/Browser4.jar
```

### Java Options Configuration

Pass custom Java system properties to the server:

```python
from browser4 import Browser4Driver

# Configure Java options
driver = Browser4Driver(
    java_options={
        "server.port": "8183",
        "spring.profiles.active": "rest,private",
        "logging.level.ai.platon": "DEBUG",
        "browser4.driver.headless": "true",
    }
)
driver.start()
```

Common Java options:
- `server.port` - Server port
- `spring.profiles.active` - Spring profiles (e.g., "rest", "private")
- `logging.level.*` - Logging levels
- `browser4.*` - Browser4-specific settings

## Complete Configuration Example

```python
from browser4 import Browser4Driver, PulsarClient, AgenticSession
from pathlib import Path

# Full configuration
driver = Browser4Driver(
    # Server port
    port=8183,
    
    # JAR file location
    jar_path=str(Path.home() / "browser4-lib" / "Browser4.jar"),
    
    # Download URL
    download_url="https://github.com/platonai/Browser4/releases/download/v4.4.0/Browser4.jar",
    
    # Java system properties
    java_options={
        # Server configuration
        "server.port": "8183",
        "spring.profiles.active": "rest,private",
        
        # Logging
        "logging.level.ai.platon": "INFO",
        "logging.level.root": "WARN",
        
        # Browser settings
        "browser4.driver.headless": "true",
        "browser4.browser.chrome.options": "--disable-gpu",
        
        # Performance
        "browser4.fetch.queue.capacity": "1000",
        "browser4.storage.crawl.id": "my-crawl",
    }
)

try:
    # Start server
    driver.start()
    print(f"Server running at {driver.base_url}")
    
    # Create client
    client = PulsarClient(base_url=driver.base_url)
    session_id = client.create_session()
    session = AgenticSession(client)
    
    # Use the session...
    page = session.open("https://example.com")
    
    # Cleanup
    session.close()
    client.close()
    
finally:
    driver.stop()
```

## Startup Options

### Wait for Ready

Control whether to wait for server readiness:

```python
from browser4 import Browser4Driver

driver = Browser4Driver()

# Start without waiting (returns immediately)
driver.start(wait_for_ready=False)
print("Server is starting...")

# Wait manually when needed
driver.wait_for_server_ready(timeout_seconds=60)
print("Server is ready!")

driver.stop()
```

### Custom Startup Timeout

The default startup timeout is 120 seconds. This is usually sufficient, but can be adjusted if needed by waiting manually:

```python
from browser4 import Browser4Driver

driver = Browser4Driver()

# Start without automatic wait
driver.start(wait_for_ready=False)

# Wait with custom timeout
try:
    driver.wait_for_server_ready(timeout_seconds=180)  # 3 minutes
    print("Server ready!")
except TimeoutError:
    print("Server startup timed out")
    driver.stop()
```

## Server Management

### Checking Server Status

```python
from browser4 import Browser4Driver

driver = Browser4Driver()
driver.start()

# Check if server is running
if driver.is_running:
    print("Server process is running")

# Check if server is healthy (responds to requests)
if driver.is_server_healthy():
    print("Server is healthy and responding")

driver.stop()
```

### Getting Server Information

```python
# Server URL
print(f"Base URL: {driver.base_url}")  # http://localhost:8182

# Port number
print(f"Port: {driver.port}")  # 8182

# JAR path
print(f"JAR path: {driver.jar_path}")

# Check if server process is running
print(f"Running: {driver.is_running}")
```

## Advanced Configuration

### Custom Health Check Interval

The driver checks server health during startup with configurable intervals. The default is 500ms between checks.

### Proxy Configuration

Browser4Driver automatically uses system proxy settings. Set environment variables:

```bash
# Linux/macOS
export HTTP_PROXY="http://proxy.example.com:8080"
export HTTPS_PROXY="http://proxy.example.com:8080"

# Windows (PowerShell)
$env:HTTP_PROXY="http://proxy.example.com:8080"
$env:HTTPS_PROXY="http://proxy.example.com:8080"
```

Then run your Python script - Browser4Driver will use the proxy for downloading the JAR.

### Memory Configuration

Configure Java heap memory:

```python
from browser4 import Browser4Driver

# Note: Memory settings must be passed via JAVA_OPTS or similar mechanisms
# The driver doesn't directly support JVM heap settings in java_options
# Instead, configure via environment:
import os
os.environ["JAVA_OPTS"] = "-Xmx4g -Xms2g"

driver = Browser4Driver()
driver.start()
```

### Headless Mode

Run Chrome in headless mode:

```python
from browser4 import Browser4Driver

driver = Browser4Driver(
    java_options={
        "browser4.driver.headless": "true"
    }
)
driver.start()
```

## Configuration Profiles

### Development Profile

For local development:

```python
driver = Browser4Driver(
    port=8182,
    java_options={
        "spring.profiles.active": "rest",
        "logging.level.ai.platon": "DEBUG",
        "browser4.driver.headless": "false",  # Show browser
    }
)
```

### Production Profile

For production use:

```python
driver = Browser4Driver(
    port=8182,
    java_options={
        "spring.profiles.active": "rest,private",
        "logging.level.ai.platon": "WARN",
        "browser4.driver.headless": "true",
        "browser4.fetch.queue.capacity": "10000",
    }
)
```

### Testing Profile

For automated testing:

```python
driver = Browser4Driver(
    port=8183,  # Different port for tests
    java_options={
        "spring.profiles.active": "rest,test",
        "logging.level.root": "ERROR",
        "browser4.driver.headless": "true",
    }
)
```

## Troubleshooting

### Port Already in Use

If the default port is in use:

```python
# Try a different port
driver = Browser4Driver(port=8183)

# Or find an available port automatically
import socket

def find_free_port():
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.bind(('', 0))
        return s.getsockname()[1]

port = find_free_port()
driver = Browser4Driver(port=port)
```

### JAR Download Fails

If automatic download fails:

1. **Check proxy settings**: Ensure HTTP_PROXY/HTTPS_PROXY are set correctly
2. **Download manually**: Download Browser4.jar and specify path:

```python
driver = Browser4Driver(
    jar_path="/path/to/manually/downloaded/Browser4.jar"
)
```

3. **Use custom URL**: If GitHub is blocked, host the JAR elsewhere:

```python
driver = Browser4Driver(
    download_url="https://your-mirror.com/Browser4.jar"
)
```

### Server Won't Start

Check Java installation:

```python
import subprocess

# Verify Java is installed
result = subprocess.run(
    ["java", "-version"],
    capture_output=True,
    text=True
)

print(result.stderr)  # Java version info
```

### Health Check Timeout

If server takes long to start:

```python
driver = Browser4Driver()

# Start without waiting
driver.start(wait_for_ready=False)

# Wait with longer timeout
try:
    driver.wait_for_server_ready(timeout_seconds=300)  # 5 minutes
except TimeoutError:
    print("Server failed to start")
    # Check logs or process status
    if driver.is_running:
        print("Process is running but not responding")
    else:
        print("Process failed to start")
```

## Environment Variables

Browser4Driver respects these environment variables:

- `HTTP_PROXY` - HTTP proxy for downloading JAR
- `HTTPS_PROXY` - HTTPS proxy for downloading JAR
- `JAVA_HOME` - Java installation path (if not in PATH)
- `JAVA_OPTS` - Additional JVM options

Example:

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export JAVA_OPTS="-Xmx4g -Xms2g"
export HTTP_PROXY=http://proxy.example.com:8080

python your_script.py
```

## Best Practices

1. **Use Context Manager**: Always use `with Browser4Driver()` for automatic cleanup

2. **Handle Exceptions**: Wrap driver operations in try-finally:
```python
driver = Browser4Driver()
try:
    driver.start()
    # Use server...
finally:
    driver.stop()
```

3. **Customize Port for Tests**: Use different ports for different test environments

4. **Configure Logging Appropriately**: Use DEBUG for development, WARN for production

5. **Cache JAR File**: Let Browser4Driver use the default path to cache downloads

6. **Check Health**: Always verify server health after startup:
```python
driver.start()
assert driver.is_server_healthy(), "Server is not healthy"
```

## Next Steps

- [Remote Server Configuration](remote-server.md) - Connect to remote Browser4 servers
- [Environment Variables](environment-variables.md) - Complete environment variable reference
- [Basic Usage](../examples/basic-usage.md) - Using Browser4Driver in practice
