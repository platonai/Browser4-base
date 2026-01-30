# Installation

This guide will help you install and set up the Browser4 Python SDK.

## Prerequisites

Before installing the SDK, ensure you have:

- **Python 3.9 or higher**
- **Browser4 server** running (see [Server Setup](#server-setup))
- **pip** package manager

## Quick Installation

### From Source (Development)

Clone the repository and install in editable mode:

```bash
# Clone the repository
git clone https://github.com/platonai/Browser4.git
cd Browser4/sdks/python-sdk

# Install in editable mode with dev dependencies
pip install -e .[dev]
```

### Manual Dependency Installation

If you prefer to install dependencies manually:

```bash
pip install requests>=2.31.0 beautifulsoup4>=4.12.0
```

For development and testing:

```bash
pip install pytest>=7.4.0
```

## Server Setup

The Python SDK requires a running Browser4 server to function. The server provides the browser automation backend.

### Using Docker (Recommended)

The easiest way to run the Browser4 server is using Docker:

```bash
# Pull the latest Browser4 image
docker pull platonai/browser4:latest

# Run the server
docker run -p 8182:8182 platonai/browser4:latest
```

The server will be available at `http://localhost:8182`.

### Building from Source

Alternatively, build and run the server from source:

```bash
# From the Browser4 repository root
cd Browser4

# Build the project (Unix/macOS)
./mvnw clean package -DskipTests

# Or on Windows
mvnw.cmd clean package -DskipTests

# Run the server
java -jar pulsar-rest/target/pulsar-rest-*.jar
```

### Verify Server is Running

Check if the server is running:

```bash
curl http://localhost:8182/api/v1/status
```

Or from Python:

```python
import requests
response = requests.get("http://localhost:8182/api/v1/status")
print(response.json())
```

## Verify Installation

Test your installation with a simple script:

```python
from pulsar_sdk import PulsarClient

# Create a client
client = PulsarClient(base_url="http://localhost:8182")

# Create a session
try:
    session_id = client.create_session()
    print(f"✓ Successfully created session: {session_id}")
    
    # Clean up
    client.delete_session()
    print("✓ Installation verified successfully!")
except Exception as e:
    print(f"✗ Installation verification failed: {e}")
```

If this script runs without errors, your installation is complete!

## Configuration

### Server URL

By default, the SDK connects to `http://localhost:8182`. To use a different server:

```python
from pulsar_sdk import PulsarClient

client = PulsarClient(base_url="http://your-server:8182")
```

### Timeout Settings

Adjust request timeouts as needed:

```python
client = PulsarClient(
    base_url="http://localhost:8182",
    timeout=60.0  # 60 seconds
)
```

### Custom Headers

Add custom headers for authentication or other purposes:

```python
client = PulsarClient(
    base_url="http://localhost:8182",
    default_headers={
        "Authorization": "Bearer your-token",
        "X-Custom-Header": "value"
    }
)
```

## Environment Variables

You can use environment variables to configure the SDK:

```bash
# Set the Browser4 server URL
export BROWSER4_URL=http://localhost:8182

# Set timeout (in seconds)
export BROWSER4_TIMEOUT=60
```

Then in your code:

```python
import os
from pulsar_sdk import PulsarClient

client = PulsarClient(
    base_url=os.getenv("BROWSER4_URL", "http://localhost:8182"),
    timeout=float(os.getenv("BROWSER4_TIMEOUT", "30"))
)
```

## Troubleshooting

### Connection Refused

If you get a "Connection refused" error:

1. Verify the Browser4 server is running
2. Check the server URL and port
3. Ensure no firewall is blocking the connection

```python
# Test connection
import requests
try:
    requests.get("http://localhost:8182/api/v1/status", timeout=5)
    print("Server is reachable")
except requests.exceptions.ConnectionError:
    print("Cannot connect to server. Is it running?")
```

### Import Errors

If you get import errors:

```bash
# Reinstall the package
pip uninstall pulsar-browser-sdk
pip install -e .[dev]

# Or verify Python path
python -c "import pulsar_sdk; print(pulsar_sdk.__file__)"
```

### Version Conflicts

If you have dependency conflicts:

```bash
# Create a virtual environment
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

# Install in the clean environment
pip install -e .[dev]
```

## Next Steps

Now that you have the SDK installed, you're ready to:

- Follow the [Quick Start Guide](quickstart.md) to write your first script
- Explore [Basic Usage](guides/basic-usage.md) patterns
- Check out [Examples](examples/simple-scraping.md) for common use cases

## Upgrading

To upgrade to the latest version:

```bash
cd Browser4/sdks/python-sdk
git pull origin main
pip install -e .[dev] --upgrade
```

## Uninstallation

To remove the SDK:

```bash
pip uninstall pulsar-browser-sdk
```

---

Need help? Check the [Troubleshooting Guide](troubleshooting.md) or [open an issue](https://github.com/platonai/Browser4/issues).
