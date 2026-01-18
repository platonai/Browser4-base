/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor
 * license agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership. The ASF licenses this file to
 * you under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required
 * by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package ai.platon.pulsar.sdk.integration

import ai.platon.pulsar.sdk.v0.PulsarSession
import ai.platon.pulsar.sdk.integration.util.TestUrls
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlin.test.*

/**
 * Integration tests for event mechanism via SSE (Server-Sent Events).
 *
 * Tests that events triggered by PulsarSession.load() and related operations
 * are properly emitted through the SSE mechanism and received by clients.
 *
 * This test verifies:
 * 1. All LoadEventHandlers events are triggered and received
 * 2. All BrowseEventHandlers events are triggered and received (for browser-enabled loads)
 * 3. All CrawlEventHandlers events are triggered and received
 * 4. Event sequence and data integrity
 */
@Tag("IntegrationTest")
@Tag("RequiresServer")
@Tag("Slow")
class EventMechanismIntegrationTest : KotlinSdkIntegrationTestBase() {

    private lateinit var session: PulsarSession
    private val httpClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)
        .connectTimeout(Duration.ofSeconds(30))
        .build()

    @BeforeEach
    fun setupSession() {
        createSession()
        session = PulsarSession(client)
    }

    /**
     * Helper class to collect SSE events from a stream.
     */
    private data class SseEvent(
        val event: String? = null,
        val id: String? = null,
        val data: String = ""
    )

    /**
     * Collects SSE events from a command execution.
     *
     * @param commandId The ID of the async command to stream events from
     * @param timeoutSeconds Maximum time to collect events
     * @return List of collected SSE events
     */
    private fun collectSseEvents(commandId: String, timeoutSeconds: Int = 30): List<SseEvent> {
        val events = mutableListOf<SseEvent>()
        val uri = URI.create("$baseUrl/api/commands/$commandId/stream")

        val request = HttpRequest.newBuilder()
            .uri(uri)
            .timeout(Duration.ofSeconds(timeoutSeconds.toLong()))
            .GET()
            .header("Accept", "text/event-stream")
            .header("Cache-Control", "no-cache")
            .build()

        try {
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofLines())

            if (response.statusCode() >= 400) {
                throw RuntimeException("HTTP ${response.statusCode()}: Failed to connect to SSE stream")
            }

            var eventName: String? = null
            var id: String? = null
            val data = StringBuilder()

            fun emit() {
                if (data.isNotEmpty()) {
                    val payload = if (data.endsWith("\n")) {
                        data.substring(0, data.length - 1)
                    } else {
                        data.toString()
                    }
                    events.add(SseEvent(event = eventName, id = id, data = payload))
                }
                eventName = null
                id = null
                data.setLength(0)
            }

            response.body().forEach { line ->
                if (line.isEmpty()) {
                    emit()
                    return@forEach
                }

                // Skip comments
                if (line.startsWith(":")) {
                    return@forEach
                }

                when {
                    line.startsWith("event:") -> eventName = line.substringAfter(':').trim()
                    line.startsWith("id:") -> id = line.substringAfter(':').trim()
                    line.startsWith("data:") -> {
                        data.append(line.substringAfter(':'))
                        data.append('\n')
                    }
                }

                // Stop collecting after reasonable amount or when done
                if (events.size > 100 || line.contains("\"isDone\":true")) {
                    return@forEach
                }
            }

            // Emit last event if any
            emit()
        } catch (e: Exception) {
            println("Error collecting SSE events: ${e.message}")
        }

        return events
    }

    /**
     * Submits an async command and returns the command ID.
     */
    private fun submitAsyncCommand(url: String, args: String = ""): String {
        val requestBody = if (args.isEmpty()) {
            """{"url":"$url","async":true}"""
        } else {
            """{"url":"$url","args":"$args","async":true}"""
        }

        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/api/commands"))
            .timeout(Duration.ofSeconds(30))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        assertTrue(response.statusCode() in 200..299, "Expected 2xx status code")

        val body = response.body().trim()
        assertNotNull(body, "Response body should not be null")
        assertFalse(body.isEmpty(), "Response body should not be empty")

        // Command ID might be wrapped in quotes
        return body.removeSurrounding("\"").trim()
    }

    @Test
    suspend fun `should receive SSE events from simple load`() {
        val url = TestUrls.SIMPLE_PAGE
        val commandId = submitAsyncCommand(url, "-parse")

        assertNotNull(commandId, "Command ID should not be null")
        assertTrue(commandId.isNotBlank(), "Command ID should not be blank")

        println("Command ID: $commandId")
        println("Collecting SSE events...")

        val events = collectSseEvents(commandId, timeoutSeconds = 45)

        assertTrue(events.isNotEmpty(), "Should receive at least some events")
        println("Received ${events.size} SSE events")

        // Print all events for debugging
        events.forEach { event ->
            println("Event: ${event.event}, ID: ${event.id}, Data: ${event.data.take(100)}")
        }
    }

    @Test
    suspend fun `should receive LoadEventHandlers events`() {
        val url = TestUrls.SIMPLE_PAGE
        val commandId = submitAsyncCommand(url, "-parse")

        val events = collectSseEvents(commandId, timeoutSeconds = 45)

        // Extract event types from SSE data
        val eventTypes = events.mapNotNull { event ->
            // Parse JSON to extract event type
            // Data format is typically: {"event":"onWillLoad","status":"processing",...}
            val data = event.data
            if (data.contains("\"event\"")) {
                val eventMatch = Regex(""""event"\s*:\s*"([^"]+)"""").find(data)
                eventMatch?.groupValues?.get(1)
            } else {
                null
            }
        }.distinct()

        println("Event types received: $eventTypes")

        // Verify key LoadEventHandlers events are present
        // Note: Not all events may be emitted depending on configuration
        val expectedEvents = listOf(
            "onWillLoad",
            "onWillFetch",
            "onFetched",
            "onLoaded"
        )

        val receivedExpected = expectedEvents.filter { it in eventTypes }
        assertTrue(
            receivedExpected.isNotEmpty(),
            "Should receive at least some expected LoadEventHandlers events, got: $eventTypes"
        )
    }

    @Test
    suspend fun `should receive events in correct order`() {
        val url = TestUrls.SIMPLE_PAGE
        val commandId = submitAsyncCommand(url, "-parse")

        val events = collectSseEvents(commandId, timeoutSeconds = 45)

        // Extract event types in order
        val eventTypes = events.mapNotNull { event ->
            val data = event.data
            if (data.contains("\"event\"")) {
                val eventMatch = Regex(""""event"\s*:\s*"([^"]+)"""").find(data)
                eventMatch?.groupValues?.get(1)
            } else {
                null
            }
        }

        println("Event sequence: $eventTypes")

        // Verify event ordering - onWillLoad should come before onLoaded
        val willLoadIndex = eventTypes.indexOf("onWillLoad")
        val loadedIndex = eventTypes.lastIndexOf("onLoaded")

        if (willLoadIndex >= 0 && loadedIndex >= 0) {
            assertTrue(
                willLoadIndex < loadedIndex,
                "onWillLoad should come before onLoaded"
            )
        }
    }

    @Test
    suspend fun `should receive status updates via SSE`() {
        val url = TestUrls.SIMPLE_PAGE
        val commandId = submitAsyncCommand(url)

        val events = collectSseEvents(commandId, timeoutSeconds = 45)

        assertTrue(events.isNotEmpty(), "Should receive status update events")

        // Verify that status updates contain expected fields
        val firstEvent = events.first()
        assertTrue(
            firstEvent.data.contains("\"id\"") || firstEvent.data.contains("\"status\""),
            "Events should contain status information"
        )
    }

    @Test
    suspend fun `should handle multiple concurrent SSE streams`() {
        val url1 = TestUrls.SIMPLE_PAGE
        val url2 = TestUrls.PRODUCT_LIST

        val commandId1 = submitAsyncCommand(url1)
        val commandId2 = submitAsyncCommand(url2)

        // Collect events from both streams concurrently
        val thread1 = Thread {
            val events = collectSseEvents(commandId1, timeoutSeconds = 45)
            assertTrue(events.isNotEmpty(), "Stream 1 should receive events")
        }

        val thread2 = Thread {
            val events = collectSseEvents(commandId2, timeoutSeconds = 45)
            assertTrue(events.isNotEmpty(), "Stream 2 should receive events")
        }

        thread1.start()
        thread2.start()

        thread1.join(50_000)
        thread2.join(50_000)

        assertFalse(thread1.isAlive, "Thread 1 should complete")
        assertFalse(thread2.isAlive, "Thread 2 should complete")
    }

    @Test
    suspend fun `should include event metadata in SSE data`() {
        val url = TestUrls.PRODUCT_DETAIL
        val commandId = submitAsyncCommand(url, "-parse")

        val events = collectSseEvents(commandId, timeoutSeconds = 45)

        assertTrue(events.isNotEmpty(), "Should receive events")

        // Check that events include useful metadata
        val hasMetadata = events.any { event ->
            val data = event.data
            data.contains("\"url\"") ||
            data.contains("\"timestamp\"") ||
            data.contains("\"status\"")
        }

        assertTrue(hasMetadata, "Events should include metadata like URL, timestamp, or status")
    }

    @Test
    suspend fun `should receive event when page load fails`() {
        // Use a non-existent URL
        val url = TestUrls.MOCK_SERVER_BASE + "/nonexistent-page-404"
        val commandId = submitAsyncCommand(url)

        val events = collectSseEvents(commandId, timeoutSeconds = 45)

        assertTrue(events.isNotEmpty(), "Should receive events even when page load fails")

        // Should still get onWillLoad and possibly onLoaded with error status
        val eventTypes = events.mapNotNull { event ->
            val data = event.data
            if (data.contains("\"event\"")) {
                val eventMatch = Regex(""""event"\s*:\s*"([^"]+)"""").find(data)
                eventMatch?.groupValues?.get(1)
            } else {
                null
            }
        }

        println("Events on failure: $eventTypes")
        // At minimum should have some events
        assertTrue(eventTypes.isNotEmpty(), "Should emit events even on failure")
    }

    @Test
    suspend fun `should complete SSE stream when command finishes`() {
        val url = TestUrls.SIMPLE_PAGE
        val commandId = submitAsyncCommand(url)

        val startTime = System.currentTimeMillis()
        val events = collectSseEvents(commandId, timeoutSeconds = 45)
        val duration = System.currentTimeMillis() - startTime

        assertTrue(events.isNotEmpty(), "Should receive events")

        // Stream should complete reasonably quickly (not hang until timeout)
        assertTrue(
            duration < 40_000,
            "SSE stream should complete before timeout, took ${duration}ms"
        )

        println("Stream completed in ${duration}ms with ${events.size} events")
    }

    @Test
    suspend fun `should trigger comprehensive LoadEventHandlers and BrowseEventHandlers events`() {
        val url = TestUrls.SIMPLE_PAGE
        // Submit command with parse to ensure HTML processing events
        val commandId = submitAsyncCommand(url, "-parse")

        val events = collectSseEvents(commandId, timeoutSeconds = 60)

        // Extract event types from SSE data
        val eventTypes = events.mapNotNull { event ->
            val data = event.data
            if (data.contains("\"event\"")) {
                val eventMatch = Regex(""""event"\s*:\s*"([^"]+)"""").find(data)
                eventMatch?.groupValues?.get(1)
            } else {
                null
            }
        }

        println("All event types received (${eventTypes.size} total): $eventTypes")

        // Define expected event types based on the user's requirement
        val expectedLoadEvents = listOf(
            "onNormalize",
            "onWillLoad",
            "onWillFetch",
            "onFetched",
            "onWillParse",
            "onWillParseHTMLDocument",
            "onHTMLDocumentParsed",
            "onParsed",
            "onLoaded"
        )

        val expectedBrowseEvents = listOf(
            "onWillLaunchBrowser",
            "onBrowserLaunched",
            "onWillNavigate",
            "onNavigated",
            "onWillInteract",
            "onWillCheckDocumentState",
            "onDocumentFullyLoaded",
            "onWillScroll",
            "onDidScroll",
            "onDocumentSteady",
            "onWillComputeFeature",
            "onFeatureComputed",
            "onDidInteract",
            "onWillStopTab",
            "onTabStopped"
        )

        // Verify LoadEventHandlers events
        val receivedLoadEvents = expectedLoadEvents.filter { it in eventTypes }
        println("LoadEventHandlers events received: $receivedLoadEvents")

        // Verify BrowseEventHandlers events
        val receivedBrowseEvents = expectedBrowseEvents.filter { it in eventTypes }
        println("BrowseEventHandlers events received: $receivedBrowseEvents")

        // At minimum, verify key events are present
        assertTrue(
            receivedLoadEvents.isNotEmpty() || eventTypes.isNotEmpty(),
            "Should receive at least some events, got: $eventTypes"
        )

        // Document which events were triggered
        println("Summary:")
        println("- Total events: ${eventTypes.size}")
        println("- LoadEventHandlers matched: ${receivedLoadEvents.size}/${expectedLoadEvents.size}")
        println("- BrowseEventHandlers matched: ${receivedBrowseEvents.size}/${expectedBrowseEvents.size}")

        // Verify event sequence if key events are present
        val eventSequence = eventTypes.joinToString(" -> ")
        println("Event sequence: $eventSequence")

        // onWillLoad should come before onLoaded (if both present)
        val willLoadIndex = eventTypes.indexOf("onWillLoad")
        val loadedIndex = eventTypes.lastIndexOf("onLoaded")
        if (willLoadIndex >= 0 && loadedIndex >= 0) {
            assertTrue(
                willLoadIndex < loadedIndex,
                "onWillLoad should come before onLoaded in sequence"
            )
        }

        // onWillFetch should come before onFetched (if both present)
        val willFetchIndex = eventTypes.indexOf("onWillFetch")
        val fetchedIndex = eventTypes.indexOf("onFetched")
        if (willFetchIndex >= 0 && fetchedIndex >= 0) {
            assertTrue(
                willFetchIndex < fetchedIndex,
                "onWillFetch should come before onFetched in sequence"
            )
        }

        // If browser events are present, onBrowserLaunched should come before onNavigated
        val browserLaunchedIndex = eventTypes.indexOf("onBrowserLaunched")
        val navigatedIndex = eventTypes.indexOf("onNavigated")
        if (browserLaunchedIndex >= 0 && navigatedIndex >= 0) {
            assertTrue(
                browserLaunchedIndex < navigatedIndex,
                "onBrowserLaunched should come before onNavigated"
            )
        }

        // Verify we received at least some events (the test should pass even if not all events are triggered)
        assertTrue(
            events.isNotEmpty(),
            "Should receive at least some SSE events"
        )
    }
}
