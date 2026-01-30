# Remote Server Configuration

Connect to an existing Browser4 server.

## Overview

For production or shared environments, connect to a centrally hosted Browser4 server instead of running a local driver.

## Basic Connection

```kotlin
import ai.platon.pulsar.sdk.v0.*
import ai.platon.pulsar.sdk.v0.detail.PulsarClient

val client = PulsarClient(
    baseUrl = "http://remote-server:8182",
    useLocalDriver = false
)
client.createSession()
val session = AgenticSession(client)
```

## With AgenticSession

```kotlin
val session = AgenticSession.getOrCreate(
    baseUrl = "http://remote-server:8182",
    useLocalDriver = false
)
```

## Custom Headers

Add authentication or tracking headers:

```kotlin
val client = PulsarClient(
    baseUrl = "http://remote-server:8182",
    useLocalDriver = false,
    defaultHeaders = mapOf(
        "Authorization" to "Bearer your-token",
        "X-Client-ID" to "your-client-id"
    )
)
```

## Server Requirements

The remote server must be running Browser4 with:

- Port 8182 (default) accessible
- WebSocket support for events
- Chrome/Chromium installed

## Docker Deployment

Run Browser4 in Docker:

```bash
docker run -d \
  -p 8182:8182 \
  -e OPENROUTER_API_KEY=your-key \
  platonai/browser4:latest
```

## Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: browser4
spec:
  replicas: 3
  selector:
    matchLabels:
      app: browser4
  template:
    metadata:
      labels:
        app: browser4
    spec:
      containers:
      - name: browser4
        image: platonai/browser4:latest
        ports:
        - containerPort: 8182
        env:
        - name: OPENROUTER_API_KEY
          valueFrom:
            secretKeyRef:
              name: browser4-secrets
              key: openrouter-api-key
---
apiVersion: v1
kind: Service
metadata:
  name: browser4
spec:
  selector:
    app: browser4
  ports:
  - port: 8182
    targetPort: 8182
  type: LoadBalancer
```

## Health Checks

Check server health:

```kotlin
val client = PulsarClient(baseUrl = "http://remote-server:8182")

try {
    client.createSession()
    println("Server is healthy")
    client.deleteSession()
} catch (e: Exception) {
    println("Server is unavailable: ${e.message}")
}
```

## Load Balancing

Use multiple servers with a load balancer:

```kotlin
val servers = listOf(
    "http://server1:8182",
    "http://server2:8182",
    "http://server3:8182"
)

fun getClient(): PulsarClient {
    val server = servers.random()
    return PulsarClient(baseUrl = server, useLocalDriver = false)
}
```

## Security

### HTTPS

```kotlin
val client = PulsarClient(
    baseUrl = "https://secure-server:8182",
    useLocalDriver = false
)
```

### Authentication

```kotlin
val client = PulsarClient(
    baseUrl = "https://server:8182",
    useLocalDriver = false,
    defaultHeaders = mapOf(
        "Authorization" to "Bearer ${System.getenv("AUTH_TOKEN")}"
    )
)
```

## Best Practices

1. **Use HTTPS** - Encrypt traffic in production
2. **Load balance** - Distribute load across servers
3. **Health checks** - Monitor server availability
4. **Authentication** - Secure access to servers
5. **Retry logic** - Handle transient failures
6. **Connection pooling** - Reuse connections

## Next Steps

- [Local Driver Configuration](local-driver.md)
- [Environment Variables](environment-variables.md)
