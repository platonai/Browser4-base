# Local Driver Configuration

Configure the local Browser4 driver for development.

## Overview

The local driver mode automatically downloads and runs Browser4.jar on your machine, eliminating the need for a separate server.

## Basic Usage

```kotlin
import ai.platon.pulsar.sdk.v0.*

// Automatic (default)
val session = AgenticSession.getOrCreate()
// Local driver starts on port 8182

session.close()
```

## Custom Port

```kotlin
import ai.platon.pulsar.sdk.v0.detail.*

val options = LocalDriverOptions(
    port = 9000
)

val client = PulsarClient(
    useLocalDriver = true,
    localDriverOptions = options
)
client.createSession()
```

## Custom JAR Path

```kotlin
val options = LocalDriverOptions(
    jarPath = "/custom/path/Browser4.jar",
    port = 8182
)

val client = PulsarClient(
    useLocalDriver = true,
    localDriverOptions = options
)
```

## Java Options

Pass environment variables and system properties:

```kotlin
val options = LocalDriverOptions(
    port = 8182,
    javaOptions = mapOf(
        "OPENROUTER_API_KEY" to "your-api-key",
        "server.port" to "8182",
        "logging.level.root" to "INFO",
        "logging.level.ai.platon" to "DEBUG"
    )
)
```

## Download Configuration

Custom download URL:

```kotlin
val options = LocalDriverOptions(
    downloadUrl = "https://custom-server.com/Browser4.jar",
    jarPath = "~/.browser4/custom-Browser4.jar"
)
```

## Environment Variables

Set before starting:

```bash
export OPENROUTER_API_KEY="your-api-key"
export BROWSER4_PORT="8182"
```

The local driver automatically inherits environment variables.

## Default Locations

- **JAR file**: `~/.browser4/Browser4.jar`
- **Port**: 8182
- **Download**: Latest GitHub release

## Troubleshooting

### Port Already in Use

```kotlin
// Use different port
val options = LocalDriverOptions(port = 9000)
```

### Download Fails

```kotlin
// Use local JAR
val options = LocalDriverOptions(
    jarPath = "/path/to/Browser4.jar",
    downloadUrl = null  // Skip download
)
```

### Java Not Found

Ensure Java 17+ is in PATH:

```bash
java -version  # Should show 17 or higher
```

## Best Practices

1. **Dev environment** - Use local driver for development
2. **Production** - Use remote server for production
3. **Custom ports** - Avoid conflicts with other services
4. **Environment variables** - Store API keys in environment
5. **JAR updates** - Delete old JAR to force re-download

## Next Steps

- [Remote Server Configuration](remote-server.md)
- [Environment Variables](environment-variables.md)
