# Installation

This guide will help you install the Browser4 Python SDK and its dependencies.

## Prerequisites

Before installing the Browser4 Python SDK, ensure you have:

- **Python 3.9 or higher**
- **Java 17 or higher** (required for Browser4 server)
- **Google Chrome** (latest version recommended)

### Check Your Python Version

```bash
python --version
# or
python3 --version
```

### Check Your Java Version

```bash
java -version
```

If you don't have Java 17+, download it from:
- [Oracle JDK](https://www.oracle.com/java/technologies/downloads/)
- [OpenJDK](https://openjdk.org/)
- [Amazon Corretto](https://aws.amazon.com/corretto/)

## Installation Methods

### Using uv (Recommended)

[uv](https://github.com/astral-sh/uv) is a fast, modern Python package manager with built-in virtual environment management.

```bash
# Install uv if you haven't already
curl -LsSf https://astral.sh/uv/install.sh | sh

# Clone or create your project
git clone https://github.com/platonai/Browser4.git
cd Browser4/sdks/browser4-sdk-python

# Install dependencies (creates/updates virtualenv automatically)
uv sync

# Run your scripts
uv run python your_script.py
```

**With dev dependencies:**
```bash
uv sync --extra dev
```

### Using pip

```bash
# Install from local source
cd Browser4/sdks/browser4-sdk-python
pip install -e .

# Or install with dev dependencies
pip install -e ".[dev]"
```

### Using pip from PyPI (Future)

```bash
# Once published to PyPI
pip install browser4-sdk
```

## Verify Installation

Test your installation by running:

```python
python -c "import browser4; print('Browser4 SDK version:', browser4.__version__)"
```

You should see output like:
```
Browser4 SDK version: 0.1.0
```

## First Run: Automatic Server Download

The first time you use `Browser4Driver`, it will automatically download the Browser4 server JAR file from GitHub releases. This is a one-time operation that may take a few minutes depending on your internet connection.

```python
from browser4 import Browser4Driver

# First run: downloads Browser4.jar automatically
with Browser4Driver() as driver:
    print(f"Server running at: {driver.base_url}")
```

**What happens during first run:**
1. Checks if Browser4.jar exists in the default location
2. If not found, downloads from GitHub releases
3. Verifies the downloaded file
4. Starts the server
5. Waits for health check to pass

**Default download location:**
- Linux/macOS: `~/.browser4/Browser4.jar`
- Windows: `%USERPROFILE%\.browser4\Browser4.jar`

## Manual Server Installation (Optional)

If you prefer to manage the Browser4 server manually or have connectivity issues:

### Download Manually

1. Download Browser4.jar from [GitHub Releases](https://github.com/platonai/Browser4/releases)
2. Place it in a known location, e.g., `/opt/browser4/Browser4.jar`
3. Specify the path when creating Browser4Driver:

```python
from browser4 import Browser4Driver

driver = Browser4Driver(jar_path="/opt/browser4/Browser4.jar")
driver.start()
# ... use the driver
driver.stop()
```

### Run as Standalone Service

You can also run Browser4 as a standalone service and connect to it:

```bash
# Start Browser4 server manually
java -jar Browser4.jar --server.port=8182
```

Then connect without Browser4Driver:

```python
from browser4 import PulsarClient, AgenticSession

# Connect to existing server
client = PulsarClient(base_url="http://localhost:8182")
session_id = client.create_session()
session = AgenticSession(client)

# ... use the session

session.close()
client.close()
```

## Installing Development Dependencies

For development work (running tests, building docs):

```bash
# Using uv
uv sync --extra dev

# Using pip
pip install -e ".[dev]"
```

Development dependencies include:
- pytest - Test framework
- pytest-cov - Coverage reporting
- mkdocs - Documentation generator
- mkdocs-material - Documentation theme

## Troubleshooting

### Import Error: No module named 'browser4'

Make sure you've installed the package:
```bash
pip install -e .
# or
uv sync
```

### Java Not Found

Ensure Java is installed and in your PATH:
```bash
java -version
```

On Linux/macOS, add to `~/.bashrc` or `~/.zshrc`:
```bash
export JAVA_HOME=/path/to/java
export PATH=$JAVA_HOME/bin:$PATH
```

On Windows, add to System Environment Variables.

### Download Failed

If automatic download fails:
1. Check your internet connection
2. Verify GitHub access (not blocked by firewall)
3. Use manual installation method
4. Check proxy settings if behind corporate proxy

### Permission Denied

On Linux/macOS, ensure the directory is writable:
```bash
chmod +w ~/.browser4
```

## Next Steps

Now that you have Browser4 installed:

1. **[Quick Start](quick-start.md)** - Write your first script
2. **[First Steps](first-steps.md)** - Learn essential concepts
3. **[Configuration](../configuration/browser4-driver.md)** - Customize your setup
