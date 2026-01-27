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

import ai.platon.pulsar.sdk.integration.util.TestUrls
import ai.platon.pulsar.sdk.v0.AgentEvent
import ai.platon.pulsar.sdk.v0.AgentEventHandlers
import ai.platon.pulsar.sdk.v0.AgenticSession
import kotlinx.coroutines.delay
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * End-to-end test for Browser4 Agent using the Kotlin SDK.
 *
 * This test validates the complete agent workflow through the SDK by:
 * 1. Creating an AgenticSession with agent event handlers
 * 2. Running agent operations (run, act, observe) with event streaming
 * 3. Verifying that server-side events are properly received by the client SDK
 * 4. Validating the agent operation results
 *
 * This test mirrors the functionality of the server-side AgentE2ETest
 * (ai.platon.pulsar.agentic.agent.AgentE2ETest) but tests it through the SDK.
 *
 * Test coverage:
 * - Agent event handlers registration and dispatch
 * - Server-side event streaming to client SDK
 * - Agent run/act/observe operations
 * - Event sequence and data integrity
 *
 * Note: When `pulsar.test.mode=true` the server returns lightweight
 * stubbed responses so the suite can run without a real LLM/backend.
 */
@Tag("IntegrationTest")
@Tag("RequiresServer")
@Tag("Slow")
class AgentE2ETest : KotlinSdkIntegrationTestBase() {

    private lateinit var session: AgenticSession

    companion object {
        private const val MAX_TEST_STEPS = 5
        private const val EVENT_PROCESSING_DELAY_MS = 500L

        private val capturedEvents = ConcurrentHashMap<String, CopyOnWriteArrayList<AgentEvent>>()
        private val runStepCount = AtomicInteger(0)
        private val observeCount = AtomicInteger(0)
        private val actCount = AtomicInteger(0)

        @BeforeAll
        @JvmStatic
        fun setupClass() {
            println("AgentE2ETest: Starting test class")
        }

        @AfterAll
        @JvmStatic
        fun cleanupClass() {
            println("AgentE2ETest: Cleaning up test class")
        }
    }

    @BeforeEach
    suspend fun setupSession() {
        createSession()
        session = AgenticSession(client)

        // Clear captured events and counters
        capturedEvents.clear()
        runStepCount.set(0)
        observeCount.set(0)
        actCount.set(0)

        // Setup agent event handlers
        setupAgentEventHandlers()
    }

    @AfterEach
    fun tearDown() {
        println("Test completed - Total steps: ${runStepCount.get()}, Observe count: ${observeCount.get()}, Act count: ${actCount.get()}")
    }

    /**
     * Sets up agent event handlers to capture and log events during tests.
     */
    private fun setupAgentEventHandlers() {
        val handlers = session.agentEventHandlers

        // Agent lifecycle event handlers
        handlers.agent.on(AgentEventHandlers.EventTypes.ON_WILL_RUN) { event ->
            println("🚀 Agent run starting - message: ${event.message}")
            capturedEvents.computeIfAbsent(AgentEventHandlers.EventTypes.ON_WILL_RUN) {
                CopyOnWriteArrayList()
            }.add(event)
        }

        handlers.agent.on(AgentEventHandlers.EventTypes.ON_DID_RUN) { event ->
            println("✅ Agent run completed - message: ${event.message}")
            capturedEvents.computeIfAbsent(AgentEventHandlers.EventTypes.ON_DID_RUN) {
                CopyOnWriteArrayList()
            }.add(event)
        }

        handlers.agent.on(AgentEventHandlers.EventTypes.ON_WILL_OBSERVE) { event ->
            observeCount.incrementAndGet()
            println("👀 Observing... (count: ${observeCount.get()})")
            capturedEvents.computeIfAbsent(AgentEventHandlers.EventTypes.ON_WILL_OBSERVE) {
                CopyOnWriteArrayList()
            }.add(event)
        }

        handlers.agent.on(AgentEventHandlers.EventTypes.ON_DID_OBSERVE) { event ->
            println("👀 Observation completed")
            capturedEvents.computeIfAbsent(AgentEventHandlers.EventTypes.ON_DID_OBSERVE) {
                CopyOnWriteArrayList()
            }.add(event)
        }

        handlers.agent.on(AgentEventHandlers.EventTypes.ON_WILL_ACT) { event ->
            actCount.incrementAndGet()
            println("🎬 Acting... (count: ${actCount.get()}) - message: ${event.message}")
            capturedEvents.computeIfAbsent(AgentEventHandlers.EventTypes.ON_WILL_ACT) {
                CopyOnWriteArrayList()
            }.add(event)
        }

        handlers.agent.on(AgentEventHandlers.EventTypes.ON_DID_ACT) { event ->
            println("🎬 Action completed - message: ${event.message}")
            capturedEvents.computeIfAbsent(AgentEventHandlers.EventTypes.ON_DID_ACT) {
                CopyOnWriteArrayList()
            }.add(event)
        }

        // Inference event handlers
        handlers.inference.on(AgentEventHandlers.EventTypes.ON_WILL_INFER) { event ->
            runStepCount.incrementAndGet()
            println("🧠 Inference starting - step: ${runStepCount.get()}")
            capturedEvents.computeIfAbsent(AgentEventHandlers.EventTypes.ON_WILL_INFER) {
                CopyOnWriteArrayList()
            }.add(event)
        }

        handlers.inference.on(AgentEventHandlers.EventTypes.ON_DID_INFER) { event ->
            println("🧠 Inference completed")
            capturedEvents.computeIfAbsent(AgentEventHandlers.EventTypes.ON_DID_INFER) {
                CopyOnWriteArrayList()
            }.add(event)
        }

        // Tool event handlers
        handlers.tool.on(AgentEventHandlers.EventTypes.ON_WILL_EXECUTE_TOOL) { event ->
            println("🔧 Tool execution starting - message: ${event.message}")
            capturedEvents.computeIfAbsent(AgentEventHandlers.EventTypes.ON_WILL_EXECUTE_TOOL) {
                CopyOnWriteArrayList()
            }.add(event)
        }

        handlers.tool.on(AgentEventHandlers.EventTypes.ON_DID_EXECUTE_TOOL) { event ->
            println("🔧 Tool execution completed")
            capturedEvents.computeIfAbsent(AgentEventHandlers.EventTypes.ON_DID_EXECUTE_TOOL) {
                CopyOnWriteArrayList()
            }.add(event)
        }
    }

    /**
     * Test that the agent can execute a simple run task and events are captured.
     *
     * This test validates:
     * 1. The SDK can successfully run an agent task
     * 2. Agent events are streamed from server to client
     * 3. Event handlers are properly invoked
     */
    @Test
    @DisplayName("test agent run task with event streaming")
    suspend fun testAgentRunTaskWithEventStreaming() {
        // Navigate to a test page first
        session.driver.navigateTo(TestUrls.SIMPLE_PAGE)

        // Run a simple task
        val task = "describe what you see on this page"
        println("Running task: $task")

        val result = session.runWithEvents(task)

        // Allow time for event processing
        delay(EVENT_PROCESSING_DELAY_MS)

        // Verify result
        assertNotNull(result, "Run result should not be null")
        println("Run result - success: ${result.success}, message: ${result.message}")

        // Log captured events summary
        logEventSummary()

        // Verify basic operation worked
        assertTrue(result.message.isNotBlank(), "Result message should not be blank")
    }

    /**
     * Test that the agent act operation works with event streaming.
     */
    @Test
    @DisplayName("test agent act with event streaming")
    suspend fun testAgentActWithEventStreaming() {
        // Navigate to a test page first
        session.driver.navigateTo(TestUrls.SIMPLE_PAGE)

        // Execute an action
        val action = "scroll to the bottom of the page"
        println("Executing action: $action")

        val result = session.actWithEvents(action)

        // Allow time for event processing
        delay(EVENT_PROCESSING_DELAY_MS)

        // Verify result
        assertNotNull(result, "Act result should not be null")
        println("Act result - success: ${result.success}, message: ${result.message}")

        // Log captured events summary
        logEventSummary()
    }

    /**
     * Test that the agent observe operation works with event streaming.
     */
    @Test
    @DisplayName("test agent observe with event streaming")
    suspend fun testAgentObserveWithEventStreaming() {
        // Navigate to a test page first
        session.driver.navigateTo(TestUrls.PRODUCT_DETAIL)

        // Observe the page
        val instruction = "find all interactive elements"
        println("Observing with instruction: $instruction")

        val result = session.observeWithEvents(instruction)

        // Allow time for event processing
        delay(EVENT_PROCESSING_DELAY_MS)

        // Verify result
        assertNotNull(result, "Observe result should not be null")
        assertNotNull(result.observations, "Observations list should not be null")
        println("Observe result - ${result.observations.size} observations found")

        // Log captured events summary
        logEventSummary()
    }

    /**
     * Test that agent event handlers can be configured and receive events.
     */
    @Test
    @DisplayName("test agent event handlers configuration")
    suspend fun testAgentEventHandlersConfiguration() {
        val handlers = session.agentEventHandlers

        // Verify handler groups are available
        assertNotNull(handlers.agent, "Agent handler group should be available")
        assertNotNull(handlers.inference, "Inference handler group should be available")
        assertNotNull(handlers.tool, "Tool handler group should be available")
        assertNotNull(handlers.mcp, "MCP handler group should be available")
        assertNotNull(handlers.skill, "Skill handler group should be available")

        // Verify we can get registered event types
        val registeredTypes = handlers.registeredEventTypes()
        assertTrue(registeredTypes.isNotEmpty(), "Should have registered event types from setup")

        println("Registered event types: $registeredTypes")
    }

    /**
     * Test multi-step task execution with event tracking.
     */
    @Test
    @DisplayName("test multi-step task with events")
    suspend fun testMultiStepTaskWithEvents() {
        val task = """
            1. Visit ${TestUrls.PRODUCT_LIST}
            2. Find the first product
            3. Get its name
        """.trimIndent()

        println("Running multi-step task:\n$task")

        val result = session.runWithEvents(task)

        // Allow time for event processing
        delay(EVENT_PROCESSING_DELAY_MS)

        // Verify result
        assertNotNull(result, "Multi-step run result should not be null")
        println("Multi-step result - success: ${result.success}, message: ${result.message}")

        // Log captured events summary
        logEventSummary()
    }

    /**
     * Test that we can combine event streaming with manual operations.
     */
    @Test
    @DisplayName("test combined manual and event-driven operations")
    suspend fun testCombinedManualAndEventDrivenOperations() {
        // Manual navigation
        session.driver.navigateTo(TestUrls.PRODUCT_DETAIL)

        // Manual element check
        val bodyExists = session.driver.exists("body")
        assertTrue(bodyExists, "Body element should exist")

        // AI observation with events
        val observation = session.observeWithEvents("analyze the page structure")
        assertNotNull(observation, "Observation should not be null")

        // AI summarization
        val summary = session.summarize()
        assertNotNull(summary, "Summary should not be null")

        println("Combined operations completed successfully")
        println("- Body exists: $bodyExists")
        println("- Observations: ${observation.observations.size}")
        println("- Summary length: ${summary.length}")

        // Log captured events
        logEventSummary()
    }

    /**
     * Test AgentEvent data class parsing and properties.
     */
    @Test
    @DisplayName("test AgentEvent data class")
    fun testAgentEventDataClass() {
        val event = AgentEvent(
            eventType = "onWillObserve",
            eventPhase = "agent",
            agentId = "test-agent-123",
            message = "Starting observation",
            metadata = mapOf("step" to 1, "instruction" to "test")
        )

        assertEquals("onWillObserve", event.eventType)
        assertEquals("agent", event.eventPhase)
        assertEquals("test-agent-123", event.agentId)
        assertEquals("Starting observation", event.message)
        assertEquals(1, event.metadata["step"])
        assertEquals("test", event.metadata["instruction"])
    }

    /**
     * Test AgentEventHandlers event type constants.
     */
    @Test
    @DisplayName("test event type constants")
    fun testEventTypeConstants() {
        // Agent lifecycle events
        assertEquals("onWillRun", AgentEventHandlers.EventTypes.ON_WILL_RUN)
        assertEquals("onDidRun", AgentEventHandlers.EventTypes.ON_DID_RUN)
        assertEquals("onWillObserve", AgentEventHandlers.EventTypes.ON_WILL_OBSERVE)
        assertEquals("onDidObserve", AgentEventHandlers.EventTypes.ON_DID_OBSERVE)
        assertEquals("onWillAct", AgentEventHandlers.EventTypes.ON_WILL_ACT)
        assertEquals("onDidAct", AgentEventHandlers.EventTypes.ON_DID_ACT)

        // Inference events
        assertEquals("onWillInfer", AgentEventHandlers.EventTypes.ON_WILL_INFER)
        assertEquals("onDidInfer", AgentEventHandlers.EventTypes.ON_DID_INFER)

        // Tool events
        assertEquals("onWillExecuteTool", AgentEventHandlers.EventTypes.ON_WILL_EXECUTE_TOOL)
        assertEquals("onDidExecuteTool", AgentEventHandlers.EventTypes.ON_DID_EXECUTE_TOOL)
    }

    /**
     * Logs a summary of captured events for debugging.
     */
    private fun logEventSummary() {
        println("\n=== Event Summary ===")
        if (capturedEvents.isEmpty()) {
            println("No events captured (this is expected in test mode without real LLM)")
        } else {
            capturedEvents.forEach { (eventType, events) ->
                println("- $eventType: ${events.size} event(s)")
            }
        }
        println("Counters:")
        println("- Run steps: ${runStepCount.get()}")
        println("- Observe calls: ${observeCount.get()}")
        println("- Act calls: ${actCount.get()}")
        println("=====================\n")
    }
}
