# Environment Variables Reference

Environment variables for configuring Browser4 Kotlin SDK.

## API Keys

### OPENROUTER_API_KEY

OpenRouter API key for AI features.

```bash
export OPENROUTER_API_KEY="your-api-key-here"
```

**Usage:**
```kotlin
// Automatically used by local driver
val session = AgenticSession.getOrCreate()

// Or set explicitly
val options = LocalDriverOptions(
    javaOptions = mapOf(
        "OPENROUTER_API_KEY" to System.getenv("OPENROUTER_API_KEY")
    )
)
```

## Server Configuration

### BROWSER4_BASE_URL

Default server URL.

```bash
export BROWSER4_BASE_URL="http://localhost:8182"
```

### BROWSER4_PORT

Port for local driver.

```bash
export BROWSER4_PORT="8182"
```

### BROWSER4_JAR_PATH

Custom JAR file location.

```bash
export BROWSER4_JAR_PATH="/opt/browser4/Browser4.jar"
```

## Logging

### LOG_LEVEL

Set logging level.

```bash
export LOG_LEVEL="INFO"  # DEBUG, INFO, WARN, ERROR
```

## Chrome Configuration

### CHROME_BIN

Path to Chrome executable.

```bash
export CHROME_BIN="/usr/bin/google-chrome"
```

### CHROME_ARGS

Additional Chrome arguments.

```bash
export CHROME_ARGS="--headless --no-sandbox"
```

## Network

### HTTP_PROXY

HTTP proxy server.

```bash
export HTTP_PROXY="http://proxy:8080"
```

### HTTPS_PROXY

HTTPS proxy server.

```bash
export HTTPS_PROXY="https://proxy:8080"
```

## Development

### BROWSER4_DEV_MODE

Enable development mode.

```bash
export BROWSER4_DEV_MODE="true"
```

## Configuration File

Create `.env` file:

```bash
# .env
OPENROUTER_API_KEY=your-api-key
BROWSER4_PORT=8182
LOG_LEVEL=INFO
CHROME_ARGS=--headless
```

Load in application:

```kotlin
import java.io.File

fun loadEnv() {
    File(".env").forEachLine { line ->
        if (line.isNotBlank() && !line.startsWith("#")) {
            val (key, value) = line.split("=", limit = 2)
            System.setProperty(key.trim(), value.trim())
        }
    }
}
```

## Docker

Pass environment variables:

```bash
docker run -d \
  -e OPENROUTER_API_KEY=your-key \
  -e BROWSER4_PORT=8182 \
  -e LOG_LEVEL=INFO \
  -p 8182:8182 \
  platonai/browser4:latest
```

## Kubernetes

```yaml
env:
- name: OPENROUTER_API_KEY
  valueFrom:
    secretKeyRef:
      name: browser4-secrets
      key: openrouter-api-key
- name: LOG_LEVEL
  value: "INFO"
```

## Best Practices

1. **Never commit secrets** - Use environment variables
2. **Use .env files** - For local development
3. **Use secret management** - For production (Vault, AWS Secrets Manager)
4. **Validate on startup** - Check required variables exist
5. **Document required vars** - In README

## Next Steps

- [Local Driver Configuration](local-driver.md)
- [Remote Server Configuration](remote-server.md)
