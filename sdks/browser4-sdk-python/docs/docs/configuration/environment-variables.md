# Environment Variables

This guide documents all environment variables used by the Browser4 Python SDK for configuration.

## Overview

The Browser4 SDK respects various environment variables for configuration, allowing you to customize behavior without modifying code. This is especially useful for:

- Different deployment environments (dev/staging/production)
- CI/CD pipelines
- Docker containers
- Configuration management

## Browser4 Server Configuration

### BROWSER4_SERVER_URL

The URL of the Browser4 server to connect to.

**Default**: `http://localhost:8182`

**Example**:
```bash
export BROWSER4_SERVER_URL="http://remote-server.example.com:8182"
```

**Usage**:
```python
import os
from browser4 import PulsarClient

server_url = os.getenv("BROWSER4_SERVER_URL", "http://localhost:8182")
client = PulsarClient(base_url=server_url)
```

### BROWSER4_API_TOKEN

API token for authenticating with the Browser4 server.

**Default**: None

**Example**:
```bash
export BROWSER4_API_TOKEN="your-api-token-here"
```

**Usage**:
```python
import os
from browser4 import PulsarClient

api_token = os.getenv("BROWSER4_API_TOKEN")
headers = {}
if api_token:
    headers["Authorization"] = f"Bearer {api_token}"

client = PulsarClient(base_url="http://server:8182", default_headers=headers)
```

### BROWSER4_TIMEOUT

Default timeout in seconds for HTTP requests to the Browser4 server.

**Default**: `60.0`

**Example**:
```bash
export BROWSER4_TIMEOUT="120.0"
```

**Usage**:
```python
import os
from browser4 import PulsarClient

timeout = float(os.getenv("BROWSER4_TIMEOUT", "60.0"))
client = PulsarClient(base_url="http://server:8182", timeout=timeout)
```

## Browser4Driver Configuration

### BROWSER4_JAR_PATH

Path where Browser4.jar is stored or should be downloaded.

**Default**: `~/.browser4/lib/Browser4.jar`

**Example**:
```bash
export BROWSER4_JAR_PATH="/opt/browser4/Browser4.jar"
```

**Usage**:
```python
import os
from browser4 import Browser4Driver

jar_path = os.getenv("BROWSER4_JAR_PATH")
driver = Browser4Driver(jar_path=jar_path)
```

### BROWSER4_DOWNLOAD_URL

URL to download Browser4.jar from.

**Default**: `https://github.com/platonai/Browser4/releases/download/v4.5.0/Browser4.jar`

**Example**:
```bash
export BROWSER4_DOWNLOAD_URL="https://your-mirror.com/Browser4.jar"
```

**Usage**:
```python
import os
from browser4 import Browser4Driver

download_url = os.getenv("BROWSER4_DOWNLOAD_URL")
driver = Browser4Driver(download_url=download_url)
```

### BROWSER4_PORT

Port number for the local Browser4 server.

**Default**: `8182`

**Example**:
```bash
export BROWSER4_PORT="8183"
```

**Usage**:
```python
import os
from browser4 import Browser4Driver

port = int(os.getenv("BROWSER4_PORT", "8182"))
driver = Browser4Driver(port=port)
```

## Java Configuration

### JAVA_HOME

Path to Java installation directory.

**Default**: System default

**Example**:
```bash
export JAVA_HOME="/usr/lib/jvm/java-17-openjdk"
```

**Note**: Browser4Driver uses the `java` command from PATH. Set JAVA_HOME if you need a specific Java version.

### JAVA_OPTS

Additional JVM options for the Browser4 server process.

**Default**: None

**Example**:
```bash
export JAVA_OPTS="-Xmx4g -Xms2g -XX:+UseG1GC"
```

**Note**: These options affect the Java process running Browser4.jar. Memory settings are particularly useful for large-scale scraping.

## AI Configuration

### OPENROUTER_API_KEY

API key for OpenRouter (required for AI-powered features).

**Required for**: `act()`, `run()`, `observe()`, `agent_extract()`, `summarize()`

**Example**:
```bash
export OPENROUTER_API_KEY="sk-or-v1-..."
```

**Usage**:
```python
import os
from browser4 import PulsarClient, AgenticSession

# AI features require this to be set
if not os.getenv("OPENROUTER_API_KEY"):
    raise ValueError("OPENROUTER_API_KEY must be set for AI features")

client = PulsarClient(base_url="http://localhost:8182")
client.create_session()
session = AgenticSession(client)

# Use AI features
result = session.act("click the login button")
```

## Network Configuration

### HTTP_PROXY

HTTP proxy server for outgoing connections.

**Default**: None

**Example**:
```bash
export HTTP_PROXY="http://proxy.example.com:8080"
```

**Affects**:
- Browser4.jar download
- HTTP requests made by PulsarClient

### HTTPS_PROXY

HTTPS proxy server for outgoing connections.

**Default**: None

**Example**:
```bash
export HTTPS_PROXY="http://proxy.example.com:8080"
```

**Affects**:
- Browser4.jar download over HTTPS
- HTTPS requests made by PulsarClient

### NO_PROXY

Comma-separated list of hosts that should bypass proxy.

**Default**: None

**Example**:
```bash
export NO_PROXY="localhost,127.0.0.1,.internal.example.com"
```

## Logging Configuration

### BROWSER4_LOG_LEVEL

Log level for Browser4 SDK logging.

**Default**: `INFO`

**Values**: `DEBUG`, `INFO`, `WARNING`, `ERROR`, `CRITICAL`

**Example**:
```bash
export BROWSER4_LOG_LEVEL="DEBUG"
```

**Usage**:
```python
import os
import logging

log_level = os.getenv("BROWSER4_LOG_LEVEL", "INFO")
logging.basicConfig(level=getattr(logging, log_level))
```

## Complete Configuration Example

### Development Environment

```bash
# .env.development
BROWSER4_SERVER_URL=http://localhost:8182
BROWSER4_PORT=8182
BROWSER4_TIMEOUT=60.0
BROWSER4_LOG_LEVEL=DEBUG
OPENROUTER_API_KEY=sk-or-v1-your-dev-key
```

### Production Environment

```bash
# .env.production
BROWSER4_SERVER_URL=http://browser4-prod.internal:8182
BROWSER4_API_TOKEN=prod-api-token-xxx
BROWSER4_TIMEOUT=120.0
BROWSER4_LOG_LEVEL=WARNING
OPENROUTER_API_KEY=sk-or-v1-your-prod-key
HTTP_PROXY=http://proxy.internal:8080
HTTPS_PROXY=http://proxy.internal:8080
NO_PROXY=localhost,127.0.0.1,.internal
```

### Testing Environment

```bash
# .env.test
BROWSER4_PORT=8183
BROWSER4_TIMEOUT=30.0
BROWSER4_LOG_LEVEL=ERROR
BROWSER4_JAR_PATH=/tmp/test-Browser4.jar
```

## Using Environment Files

### With python-dotenv

Install python-dotenv:
```bash
pip install python-dotenv
```

Load environment variables:
```python
from dotenv import load_dotenv
from browser4 import Browser4Driver, PulsarClient, AgenticSession
import os

# Load environment variables from .env file
load_dotenv()

# Use environment variables
driver = Browser4Driver(
    port=int(os.getenv("BROWSER4_PORT", "8182"))
)
driver.start()

client = PulsarClient(
    base_url=os.getenv("BROWSER4_SERVER_URL", driver.base_url),
    timeout=float(os.getenv("BROWSER4_TIMEOUT", "60.0"))
)

client.create_session()
session = AgenticSession(client)

# Use session...
```

### Multiple Environment Files

```python
from dotenv import load_dotenv
import os

# Load base configuration
load_dotenv(".env")

# Override with environment-specific config
env = os.getenv("APP_ENV", "development")
load_dotenv(f".env.{env}", override=True)

# Now use environment variables
```

## Docker Configuration

### Dockerfile Example

```dockerfile
FROM python:3.11-slim

# Install Browser4 SDK
COPY requirements.txt .
RUN pip install -r requirements.txt

# Set default environment variables
ENV BROWSER4_SERVER_URL=http://browser4-server:8182
ENV BROWSER4_TIMEOUT=120.0
ENV BROWSER4_LOG_LEVEL=INFO

# Copy application
COPY . /app
WORKDIR /app

CMD ["python", "app.py"]
```

### Docker Compose Example

```yaml
version: '3.8'

services:
  browser4-server:
    image: platonai/browser4:latest
    ports:
      - "8182:8182"
    environment:
      - JAVA_OPTS=-Xmx4g -Xms2g

  scraper:
    build: .
    depends_on:
      - browser4-server
    environment:
      - BROWSER4_SERVER_URL=http://browser4-server:8182
      - BROWSER4_TIMEOUT=120.0
      - OPENROUTER_API_KEY=${OPENROUTER_API_KEY}
      - BROWSER4_LOG_LEVEL=INFO
```

Run with environment file:
```bash
docker-compose --env-file .env.production up
```

## CI/CD Configuration

### GitHub Actions Example

```yaml
name: Browser4 Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest

    env:
      BROWSER4_PORT: 8183
      BROWSER4_TIMEOUT: 60.0
      BROWSER4_LOG_LEVEL: DEBUG

    steps:
      - uses: actions/checkout@v2

      - name: Set up Python
        uses: actions/setup-python@v2
        with:
          python-version: '3.11'

      - name: Install dependencies
        run: |
          pip install -r requirements.txt

      - name: Run tests
        env:
          OPENROUTER_API_KEY: ${{ secrets.OPENROUTER_API_KEY }}
        run: |
          pytest tests/
```

### GitLab CI Example

```yaml
test:
  image: python:3.11

  variables:
    BROWSER4_PORT: "8183"
    BROWSER4_TIMEOUT: "60.0"
    BROWSER4_LOG_LEVEL: "DEBUG"

  before_script:
    - pip install -r requirements.txt

  script:
    - pytest tests/
```

## Kubernetes Configuration

### ConfigMap Example

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: browser4-config
data:
  BROWSER4_SERVER_URL: "http://browser4-service:8182"
  BROWSER4_TIMEOUT: "120.0"
  BROWSER4_LOG_LEVEL: "INFO"
```

### Secret Example

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: browser4-secrets
type: Opaque
stringData:
  OPENROUTER_API_KEY: "sk-or-v1-your-key"
  BROWSER4_API_TOKEN: "your-api-token"
```

### Deployment Example

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: browser4-scraper
spec:
  replicas: 3
  template:
    spec:
      containers:
      - name: scraper
        image: your-registry/scraper:latest
        envFrom:
        - configMapRef:
            name: browser4-config
        - secretRef:
            name: browser4-secrets
```

## Best Practices

1. **Use .env files for local development**: Keep configuration out of code

2. **Use secrets management for production**: Store sensitive values in proper secret stores (AWS Secrets Manager, HashiCorp Vault, etc.)

3. **Document required variables**: List all required environment variables in your README

4. **Provide sensible defaults**: Use `os.getenv()` with default values where appropriate

5. **Validate configuration on startup**: Check that required variables are set

```python
import os

def validate_config():
    """Validate required environment variables."""
    required = ["OPENROUTER_API_KEY"]
    missing = [var for var in required if not os.getenv(var)]

    if missing:
        raise ValueError(f"Missing required environment variables: {', '.join(missing)}")

validate_config()
```

6. **Use different configs per environment**: Maintain separate .env files for dev/staging/prod

7. **Never commit secrets**: Add .env files to .gitignore

8. **Use type conversion**: Convert string env vars to appropriate types

```python
import os

port = int(os.getenv("BROWSER4_PORT", "8182"))
timeout = float(os.getenv("BROWSER4_TIMEOUT", "60.0"))
debug = os.getenv("DEBUG", "false").lower() == "true"
```

## Troubleshooting

### Variable Not Set

```python
import os

# Check if variable is set
if "OPENROUTER_API_KEY" not in os.environ:
    print("OPENROUTER_API_KEY is not set")
    print("Set it with: export OPENROUTER_API_KEY='your-key'")
```

### Variable Not Being Read

```bash
# Check current environment variables
env | grep BROWSER4

# Print from Python
python -c "import os; print(os.getenv('BROWSER4_SERVER_URL'))"
```

### .env File Not Loading

```python
from dotenv import load_dotenv
from pathlib import Path

# Check if .env exists
env_path = Path('.env')
if not env_path.exists():
    print(f".env file not found at {env_path.absolute()}")
else:
    # Load with verbose output
    load_dotenv(verbose=True)
```

## Next Steps

- [Browser4Driver Configuration](browser4-driver.md) - Local server configuration
- [Remote Server Configuration](remote-server.md) - Connect to remote servers
- [Basic Usage](../examples/basic-usage.md) - Using the configured SDK
