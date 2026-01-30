# PulsarClient API

Low-level HTTP client for Browser4 API communication.

## Constructor

```kotlin
class PulsarClient(
    baseUrl: String? = null,
    var sessionId: String? = null,
    private val defaultHeaders: Map<String, String> = emptyMap(),
    private val useLocalDriver: Boolean = false,
    private val localDriverOptions: LocalDriverOptions = LocalDriverOptions()
) : AutoCloseable
```

### Parameters

- `baseUrl`: Server URL (default: http://localhost:8182)
- `sessionId`: Initial session ID (optional)
- `defaultHeaders`: Default HTTP headers for all requests
- `useLocalDriver`: Auto-start local Browser4 driver
- `localDriverOptions`: Local driver configuration

## Session Management

### createSession()

```kotlin
suspend fun createSession(
    capabilities: Map<String, Any> = emptyMap()
): String?
```

Creates a new browser session.

**Returns:** Session ID

**Example:**
```kotlin
val client = PulsarClient()
val sessionId = client.createSession()
println("Session: $sessionId")
```

### deleteSession()

```kotlin
suspend fun deleteSession(sessionId: String? = this.sessionId): Boolean
```

Deletes a session.

**Returns:** True if successful

**Example:**
```kotlin
client.deleteSession()
```

## HTTP Methods

### post()

```kotlin
suspend fun post(
    path: String,
    body: Any? = null,
    sessionId: String? = this.sessionId
): Any?
```

Make a POST request.

**Parameters:**
- `path`: API endpoint path (use `{sessionId}` placeholder)
- `body`: Request body
- `sessionId`: Session ID

**Returns:** Response body

**Example:**
```kotlin
val response = client.post(
    "/session/{sessionId}/url",
    mapOf("url" to "https://example.com")
)
```

### get()

```kotlin
suspend fun get(
    path: String,
    sessionId: String? = this.sessionId
): Any?
```

Make a GET request.

**Example:**
```kotlin
val url = client.get("/session/{sessionId}/url") as String
```

### delete()

```kotlin
suspend fun delete(
    path: String,
    sessionId: String? = this.sessionId
): Any?
```

Make a DELETE request.

## Properties

### resolvedBaseUrl

```kotlin
internal val resolvedBaseUrl: String
```

The configured base URL.

### rawHttpClient

```kotlin
internal val rawHttpClient: HttpClient
```

Underlying HTTP client for advanced use (SSE, etc.)

### resolvedDefaultHeaders

```kotlin
internal val resolvedDefaultHeaders: Map<String, String>
```

Configured default headers.

## Cleanup

### close()

```kotlin
override fun close()
```

Close the client and stop local driver if running.

**Example:**
```kotlin
val client = PulsarClient(useLocalDriver = true)
try {
    // Use client
} finally {
    client.close() // Stops local driver
}
```

## LocalDriverOptions

Configuration for local Browser4 driver:

```kotlin
data class LocalDriverOptions(
    val jarPath: String? = null,
    val downloadUrl: String? = null,
    val port: Int? = null,
    val javaOptions: Map<String, String> = emptyMap()
)
```

**Fields:**
- `jarPath`: Custom JAR file path
- `downloadUrl`: Custom download URL
- `port`: Custom port (default: 8182)
- `javaOptions`: Java system properties/environment variables

**Example:**
```kotlin
val options = LocalDriverOptions(
    port = 9000,
    javaOptions = mapOf(
        "OPENROUTER_API_KEY" to "your-key",
        "server.port" to "9000"
    )
)

val client = PulsarClient(
    useLocalDriver = true,
    localDriverOptions = options
)
```

## Best Practices

1. **Use local driver for dev** - Simplifies setup
2. **Close clients** - Always close to free resources
3. **Reuse clients** - Create once, use many times
4. **Handle errors** - Wrap operations in try-catch
5. **Set custom headers** - For authentication, tracking

## Next Steps

- [PulsarSession API](pulsar-session.md) - Higher-level session
- [Configuration](../configuration/local-driver.md) - Driver configuration
