package ai.platon.pulsar.sdk

import ai.platon.pulsar.sdk.v0.PageEventHandlers
import ai.platon.pulsar.sdk.v0.detail.OpenApiEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.DisplayName

class OpenApiEventsTest {

    @Test
        @DisplayName("should parse OpenApiEvent from json")
    fun shouldParseOpenApiEventFromJson() {
        val json = """
            {"eventId":"e1","eventType":"onLoaded","timestamp":123,"data":{"url":"https://example.com"}}
        """.trimIndent()

        val evt = OpenApiEvent.fromJson(json)
        assertNotNull(evt)
        assertEquals("e1", evt.eventId)
        assertEquals("onLoaded", evt.eventType)
        assertEquals(123L, evt.timestamp)
        assertEquals("https://example.com", evt.data?.get("url"))
    }

    @Test
        @DisplayName("registeredEventTypes should be distinct across groups")
    fun registeredEventTypesShouldBeDistinctAcrossGroups() {
        val handlers = PageEventHandlers()
        handlers.load.on("onLoaded") { }
        handlers.browse.on("onWillFetch") { }
        handlers.crawl.on("onLoaded") { }

        assertEquals(setOf("onLoaded", "onWillFetch"), handlers.registeredEventTypes())
    }

    @Test
        @DisplayName("onAny should register in all groups")
    fun onAnyShouldRegisterInAllGroups() {
        val handlers = PageEventHandlers()
        handlers.onAny("onLoaded") { }

        // registeredEventTypes is a union of all groups.
        assertTrue(handlers.registeredEventTypes().contains("onLoaded"))
    }
}
