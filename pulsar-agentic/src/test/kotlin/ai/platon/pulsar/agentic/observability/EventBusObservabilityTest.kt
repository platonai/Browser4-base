package ai.platon.pulsar.agentic.observability

import ai.platon.pulsar.agentic.event.AgenticEvents
import ai.platon.pulsar.common.event.DangerousEventBus
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Tests for EventBus mechanism that provides observability and testability
 * for PerceptiveAgent and InferenceEngine methods.
 */
@Tag("observability")
class EventBusObservabilityTest {

    companion object {
        private val capturedEvents = ConcurrentHashMap<String, MutableList<Map<String, Any?>>>()

        @BeforeAll
        @JvmStatic
        fun setupEventHandlers() {
            // Register event handlers using centralized event definitions
            AgenticEvents.getAllEventTypes().forEach { eventType ->
                DangerousEventBus.register(eventType) { payload ->
                    val map = payload as? Map<String, Any?> ?: return@register null
                    capturedEvents.computeIfAbsent(eventType) { mutableListOf() }.add(map)
                    payload
                }
            }
        }

        @AfterAll
        @JvmStatic
        fun cleanup() {
            // Unregister all event handlers using centralized event definitions
            AgenticEvents.getAllEventTypes().forEach { eventType ->
                DangerousEventBus.unregister(eventType)
            }
        }
    }

    @BeforeEach
    fun clearCapturedEvents() {
        capturedEvents.clear()
    }

    @Test
    fun testEventBusRegistrationAndEmit() {
        val testEventType = "test.event"
        var handlerCalled = false
        var receivedPayload: Map<String, Any?>? = null

        DangerousEventBus.register(testEventType) { payload ->
            handlerCalled = true
            receivedPayload = payload as? Map<String, Any?>
            payload
        }

        val testPayload = mapOf("key" to "value", "number" to 42)
        DangerousEventBus.emit(testEventType, testPayload)

        // Give event bus time to process
        Thread.sleep(100)

        assertTrue(handlerCalled, "Event handler should be called")
        assertNotNull(receivedPayload, "Payload should be received")
        assertEquals("value", receivedPayload?.get("key"))
        assertEquals(42, receivedPayload?.get("number"))

        DangerousEventBus.unregister(testEventType)
    }

    @Test
    fun testPerceptiveAgentRunEvents() {
        val eventType = AgenticEvents.PerceptiveAgent.RUN_WILL_EXECUTE

        // Simulate emitting the event
        val testPayload = mapOf(
            "action" to "test action",
            "uuid" to "test-uuid"
        )
        DangerousEventBus.emit(eventType, testPayload)

        // Give event bus time to process
        Thread.sleep(100)

        val events = capturedEvents[eventType]
        assertNotNull(events, "Events should be captured")
        assertTrue(events!!.isNotEmpty(), "At least one event should be captured")
        assertEquals("test action", events[0]["action"])
        assertEquals("test-uuid", events[0]["uuid"])
    }

    @Test
    fun testPerceptiveAgentObserveEvents() {
        val willEventType = AgenticEvents.PerceptiveAgent.OBSERVE_WILL_EXECUTE
        val didEventType = AgenticEvents.PerceptiveAgent.OBSERVE_DID_EXECUTE

        // Simulate observing
        val options = mapOf("instruction" to "find button")
        DangerousEventBus.emit(willEventType, mapOf("options" to options, "uuid" to "test-uuid"))

        Thread.sleep(100)

        val results = listOf(mapOf("locator" to "1,123"))
        DangerousEventBus.emit(didEventType, mapOf(
            "options" to options,
            "uuid" to "test-uuid",
            "observeResults" to results
        ))

        Thread.sleep(100)

        val willEvents = capturedEvents[willEventType]
        val didEvents = capturedEvents[didEventType]

        assertTrue(willEvents != null, "Will events should be captured")
        assertTrue(didEvents != null, "Did events should be captured")
        assertTrue(willEvents!!.isNotEmpty())
        assertTrue(didEvents!!.isNotEmpty())
    }

    @Test
    fun testPerceptiveAgentActEvents() {
        val willEventType = AgenticEvents.PerceptiveAgent.ACT_WILL_EXECUTE
        val didEventType = AgenticEvents.PerceptiveAgent.ACT_DID_EXECUTE

        val action = mapOf("action" to "click button")
        DangerousEventBus.emit(willEventType, mapOf("action" to action, "uuid" to "test-uuid"))

        Thread.sleep(100)

        val result = mapOf("success" to true)
        DangerousEventBus.emit(didEventType, mapOf(
            "action" to action,
            "uuid" to "test-uuid",
            "result" to result
        ))

        Thread.sleep(100)

        val willEvents = capturedEvents[willEventType]
        val didEvents = capturedEvents[didEventType]

        assertTrue(willEvents != null)
        assertTrue(didEvents != null)
        assertTrue(willEvents!!.isNotEmpty())
        assertTrue(didEvents!!.isNotEmpty())
        @Suppress("UNCHECKED_CAST")
        assertEquals(true, (didEvents[0]["result"] as? Map<String, Any?>)?.get("success"))
    }

    @Test
    fun testPerceptiveAgentExtractEvents() {
        val willEventType = AgenticEvents.PerceptiveAgent.EXTRACT_WILL_EXECUTE
        val didEventType = AgenticEvents.PerceptiveAgent.EXTRACT_DID_EXECUTE

        val options = mapOf("instruction" to "extract data")
        DangerousEventBus.emit(willEventType, mapOf("options" to options, "uuid" to "test-uuid"))

        Thread.sleep(100)

        val result = mapOf("success" to true, "data" to mapOf("field" to "value"))
        DangerousEventBus.emit(didEventType, mapOf(
            "options" to options,
            "uuid" to "test-uuid",
            "result" to result
        ))

        Thread.sleep(100)

        val willEvents = capturedEvents[willEventType]
        val didEvents = capturedEvents[didEventType]

        assertTrue(willEvents != null)
        assertTrue(didEvents != null)
        assertTrue(willEvents!!.isNotEmpty())
        assertTrue(didEvents!!.isNotEmpty())
    }

    @Test
    fun testPerceptiveAgentSummarizeEvents() {
        val willEventType = AgenticEvents.PerceptiveAgent.SUMMARIZE_WILL_EXECUTE
        val didEventType = AgenticEvents.PerceptiveAgent.SUMMARIZE_DID_EXECUTE

        DangerousEventBus.emit(willEventType, mapOf(
            "instruction" to "summarize page",
            "selector" to null,
            "uuid" to "test-uuid"
        ))

        Thread.sleep(100)

        DangerousEventBus.emit(didEventType, mapOf(
            "instruction" to "summarize page",
            "selector" to null,
            "uuid" to "test-uuid",
            "result" to "Summary of the page"
        ))

        Thread.sleep(100)

        val willEvents = capturedEvents[willEventType]
        val didEvents = capturedEvents[didEventType]

        assertTrue(willEvents != null)
        assertTrue(didEvents != null)
        assertTrue(willEvents!!.isNotEmpty())
        assertTrue(didEvents!!.isNotEmpty())
        assertEquals("Summary of the page", didEvents[0]["result"])
    }

    @Test
    fun testInferenceEngineObserveEvents() {
        val willEventType = AgenticEvents.ContextToAction.GENERATE_WILL_EXECUTE
        val didEventType = AgenticEvents.ContextToAction.GENERATE_DID_EXECUTE

        val context = mapOf("step" to 1)
        val messages = mapOf("content" to "test message")

        DangerousEventBus.emit(willEventType, mapOf(
            "context" to context,
            "messages" to messages
        ))

        Thread.sleep(100)

        val actionDescription = mapOf("method" to "click")
        DangerousEventBus.emit(didEventType, mapOf(
            "context" to context,
            "messages" to messages,
            "actionDescription" to actionDescription
        ))

        Thread.sleep(100)

        val willEvents = capturedEvents[willEventType]
        val didEvents = capturedEvents[didEventType]

        assertTrue(willEvents != null)
        assertTrue(didEvents != null)
        assertTrue(willEvents!!.isNotEmpty())
        assertTrue(didEvents!!.isNotEmpty())
    }

    @Test
    fun testInferenceEngineExtractEvents() {
        val willEventType = AgenticEvents.InferenceEngine.EXTRACT_WILL_EXECUTE
        val didEventType = AgenticEvents.InferenceEngine.EXTRACT_DID_EXECUTE

        val params = mapOf("instruction" to "extract data", "schema" to emptyMap<String, Any>())
        DangerousEventBus.emit(willEventType, mapOf("params" to params))

        Thread.sleep(100)

        val result = mapOf("field" to "value")
        DangerousEventBus.emit(didEventType, mapOf(
            "params" to params,
            "result" to result
        ))

        Thread.sleep(100)

        val willEvents = capturedEvents[willEventType]
        val didEvents = capturedEvents[didEventType]

        assertTrue(willEvents != null)
        assertTrue(didEvents != null)
        assertTrue(willEvents!!.isNotEmpty())
        assertTrue(didEvents!!.isNotEmpty())
    }

    @Test
    fun testInferenceEngineSummarizeEvents() {
        val willEventType = AgenticEvents.InferenceEngine.SUMMARIZE_WILL_EXECUTE
        val didEventType = AgenticEvents.InferenceEngine.SUMMARIZE_DID_EXECUTE

        DangerousEventBus.emit(willEventType, mapOf(
            "instruction" to "summarize",
            "textContentLength" to 1000
        ))

        Thread.sleep(100)

        DangerousEventBus.emit(didEventType, mapOf(
            "instruction" to "summarize",
            "textContentLength" to 1000,
            "result" to "Summary text",
            "tokenUsage" to mapOf("total" to 500)
        ))

        Thread.sleep(100)

        val willEvents = capturedEvents[willEventType]
        val didEvents = capturedEvents[didEventType]

        assertTrue(willEvents != null)
        assertTrue(didEvents != null)
        assertTrue(willEvents!!.isNotEmpty())
        assertTrue(didEvents!!.isNotEmpty())
        assertEquals("Summary text", didEvents[0]["result"])
    }

    @Test
    fun testEventHandlerCanModifyPayload() {
        val eventType = "test.modify.event"

        DangerousEventBus.register(eventType) { payload ->
            val map = payload as? Map<String, Any?> ?: return@register null
            // Handler can modify or enrich the payload
            val modified = map.toMutableMap()
            modified["modified"] = true
            modified
        }

        val testPayload = mapOf("original" to "data")
        DangerousEventBus.emit(eventType, testPayload)

        Thread.sleep(100)

        DangerousEventBus.unregister(eventType)
    }

    @Test
    fun testMultipleHandlersForSameEvent() {
        val eventType = "test.multiple.handlers"
        var handler1Called = false
        var handler2Called = false

        val handler1 = DangerousEventBus.register(eventType) { payload ->
            handler1Called = true
            payload
        }

        // Register a second handler by overwriting
        DangerousEventBus.register(eventType) { payload ->
            handler2Called = true
            payload
        }

        DangerousEventBus.emit(eventType, mapOf("test" to "data"))

        Thread.sleep(100)

        // Only the last registered handler should be called (overwrite behavior)
        assertFalse(handler1Called, "First handler should not be called after overwrite")
        assertTrue(handler2Called, "Second handler should be called")

        DangerousEventBus.unregister(eventType)
    }

    @Test
    fun testEventBusWithNullPayload() {
        val eventType = "test.null.payload"
        var handlerCalled = false

        DangerousEventBus.register(eventType) { payload ->
            handlerCalled = true
            null // Handler can return null
        }

        DangerousEventBus.emit(eventType, mapOf<String, Any?>())

        Thread.sleep(100)

        assertTrue(handlerCalled, "Handler should be called even with null return")

        DangerousEventBus.unregister(eventType)
    }

    @Test
    fun testUnregisterEventHandler() {
        val eventType = "test.unregister"
        var callCount = 0

        DangerousEventBus.register(eventType) { payload ->
            callCount++
            payload
        }

        DangerousEventBus.emit(eventType, mapOf("test" to "1"))
        Thread.sleep(100)
        assertEquals(1, callCount)

        DangerousEventBus.unregister(eventType)

        DangerousEventBus.emit(eventType, mapOf("test" to "2"))
        Thread.sleep(100)
        // Count should still be 1 since handler was unregistered
        assertEquals(1, callCount)
    }
}
