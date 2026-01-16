package ai.platon.pulsar.sdk

import com.google.gson.Gson
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * A minimal Server-Sent Events (SSE) client.
 *
 * This is intentionally lightweight and dependency-free (uses Java HttpClient).
 * It parses SSE frames (event/id/data lines) and emits [SseEvent] callbacks.
 */
internal class SseClient(
    private val httpClient: HttpClient,
    private val timeout: Duration
) {
    data class SseEvent(
        val event: String? = null,
        val id: String? = null,
        val data: String = ""
    )

    /**
     * Opens an SSE stream and invokes callbacks until the stream ends or [shouldStop] returns true.
     */
    fun connect(
        uri: URI,
        headers: Map<String, String> = emptyMap(),
        shouldStop: () -> Boolean = { false },
        onEvent: (SseEvent) -> Unit
    ) {
        val requestBuilder = HttpRequest.newBuilder()
            .uri(uri)
            .timeout(timeout)
            .GET()
            .header("Accept", "text/event-stream")
            .header("Cache-Control", "no-cache")

        headers.forEach { (k, v) -> requestBuilder.header(k, v) }

        val response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofInputStream())
        if (response.statusCode() >= 400) {
            throw RuntimeException("HTTP ${response.statusCode()}: ${response.body()}")
        }

        BufferedReader(InputStreamReader(response.body())).use { reader ->
            var eventName: String? = null
            var id: String? = null
            val data = StringBuilder()

            fun emit() {
                if (data.isNotEmpty()) {
                    // Remove trailing newline if present.
                    val payload = if (data.endsWith("\n")) data.substring(0, data.length - 1) else data.toString()
                    onEvent(SseEvent(event = eventName, id = id, data = payload))
                }
                eventName = null
                id = null
                data.setLength(0)
            }

            while (!shouldStop()) {
                val line = reader.readLine() ?: break

                if (line.isEmpty()) {
                    emit()
                    continue
                }

                // comment line
                if (line.startsWith(":")) {
                    continue
                }

                when {
                    line.startsWith("event:") -> eventName = line.substringAfter(':').trim()
                    line.startsWith("id:") -> id = line.substringAfter(':').trim()
                    line.startsWith("data:") -> {
                        data.append(line.substringAfter(':'))
                        data.append('\n')
                    }
                }
            }

            // flush last event if any
            emit()
        }
    }
}

/**
 * SDK-side representation of an OpenAPI event.
 */
data class OpenApiEvent(
    val eventId: String,
    val eventType: String,
    val timestamp: Long,
    val data: Map<String, Any?>? = null
) {
    companion object {
        private val gson = Gson()

        fun fromJson(json: String): OpenApiEvent? {
            return try {
                gson.fromJson(json, OpenApiEvent::class.java)
            } catch (_: Exception) {
                null
            }
        }
    }
}
