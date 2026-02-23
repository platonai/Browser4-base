package ai.platon.pulsar.sdk.v0.detail

import com.google.gson.Gson
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A minimal Server-Sent Events (SSE) client using Ktor.
 *
 * This is intentionally lightweight and parses SSE frames (event/id/data lines)
 * and emits [SseEvent] callbacks.
 */
internal class SseClient(
    private val httpClient: HttpClient
) {
    data class SseEvent(
        val event: String? = null,
        val id: String? = null,
        val data: String = ""
    )

    /**
     * Opens an SSE stream and invokes callbacks until the stream ends or [shouldStop] returns true.
     */
    suspend fun connect(
        url: String,
        headers: Map<String, String> = emptyMap(),
        shouldStop: () -> Boolean = { false },
        onEvent: (SseEvent) -> Unit
    ) {
        val response = httpClient.get(url) {
            header("Accept", "text/event-stream")
            header("Cache-Control", "no-cache")
            headers.forEach { (k, v) -> header(k, v) }
        }

        if (response.status.value >= 400) {
            throw RuntimeException("HTTP ${response.status.value}: ${response.bodyAsText()}")
        }

        val channel = response.bodyAsChannel()

        withContext(Dispatchers.IO) {
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

            try {
                while (!channel.isClosedForRead && !shouldStop()) {
                    // Read lines using readUTF8Line for efficiency
                    val line = channel.readUTF8Line() ?: break

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
            } catch (e: Exception) {
                // Handle channel closing or reading errors gracefully
            } finally {
                // flush last event if any
                emit()
            }
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
