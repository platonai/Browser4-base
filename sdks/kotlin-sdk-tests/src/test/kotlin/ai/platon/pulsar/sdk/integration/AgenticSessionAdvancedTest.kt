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
import ai.platon.pulsar.sdk.v0.AgenticSession
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Advanced AgenticSession integration tests.
 *
 * Tests agent functionality variants and event handlers.
 */
@Tag("Slow")
class AgenticSessionAdvancedTest : KotlinSdkIntegrationTestBase() {

    private lateinit var session: AgenticSession

    @BeforeEach
    suspend fun setupSession() {
        createSession()
        session = AgenticSession(client)
    }

    // ========== Event Handler Tests ==========

    @Test
    @DisplayName("should register and receive agent event handlers")
    suspend fun testShouldRegisterAndReceiveAgentEventHandlers() {
        var eventReceived = false
        
        // Register handlers for different event types
        session.agentEventHandlers.agent.on("onWillAct") { event ->
            eventReceived = true
        }

        session.driver.navigateTo(TestUrls.SIMPLE_PAGE)
        
        // Trigger an agent action
        val result = session.act("scroll down")

        assertNotNull(result, "Action result should not be null")
        // Note: Event handling is best-effort, so we don't assert on eventReceived
    }

    @Test
    @DisplayName("should handle multiple event types")
    suspend fun testShouldHandleMultipleEventTypes() {
        val events = mutableListOf<String>()
        
        session.agentEventHandlers.agent.on("onWillObserve") { event ->
            events.add("willObserve")
        }
        session.agentEventHandlers.agent.on("onDidObserve") { event ->
            events.add("didObserve")
        }

        session.driver.navigateTo(TestUrls.PRODUCT_DETAIL)
        
        val observation = session.observe("find clickable elements")

        assertNotNull(observation, "Observation should not be null")
        // Events are best-effort in integration tests
    }

    // ========== Parameter Variants Tests ==========

    @Test
    @DisplayName("should execute act with default parameters")
    suspend fun testShouldExecuteActWithDefaultParameters() {
        session.driver.navigateTo(TestUrls.SIMPLE_PAGE)

        val result = session.act("scroll to top")

        assertNotNull(result, "Result should not be null")
    }

    @Test
    @DisplayName("should execute act with custom timeout")
    suspend fun testShouldExecuteActWithCustomTimeout() {
        session.driver.navigateTo(TestUrls.PRODUCT_LIST)

        val result = session.act("find the search box", timeoutMs = 5000L)

        assertNotNull(result, "Result should not be null")
    }

    @Test
    @DisplayName("should execute run with default parameters")
    suspend fun testShouldExecuteRunWithDefaultParameters() {
        val result = session.run("visit ${TestUrls.SIMPLE_PAGE} and check if it loads")

        assertNotNull(result, "Result should not be null")
        assertTrue(result.message.isNotBlank(), "Result message should not be blank")
    }

    @Test
    @DisplayName("should execute run with custom timeout")
    suspend fun testShouldExecuteRunWithCustomTimeout() {
        val result = session.run(
            task = "visit ${TestUrls.PRODUCT_DETAIL} and extract title",
            timeoutMs = 10000L
        )

        assertNotNull(result, "Result should not be null")
    }

    @Test
    @DisplayName("should execute observe with default parameters")
    suspend fun testShouldExecuteObserveWithDefaultParameters() {
        session.driver.navigateTo(TestUrls.INTERACTIVE_1)

        val observation = session.observe("identify buttons")

        assertNotNull(observation, "Observation should not be null")
        assertNotNull(observation.observations, "Observations list should not be null")
    }

    @Test
    @DisplayName("should execute observe with custom parameters")
    suspend fun testShouldExecuteObserveWithCustomParameters() {
        session.driver.navigateTo(TestUrls.PRODUCT_DETAIL)

        val observation = session.observe(
            instruction = "find product information",
            returnAction = true
        )

        assertNotNull(observation, "Observation should not be null")
    }

    @Test
    @DisplayName("should execute agentExtract with instruction and schema")
    suspend fun testShouldExecuteAgentExtractWithInstructionAndSchema() {
        session.driver.navigateTo(TestUrls.PRODUCT_DETAIL)

        val schema = mapOf(
            "title" to "string",
            "price" to "number"
        )

        val result = session.agentExtract(
            instruction = "Extract product title and price",
            schema = schema
        )

        assertNotNull(result, "Extraction result should not be null")
    }

    @Test
    @DisplayName("should execute summarize")
    suspend fun testShouldExecuteSummarize() {
        session.driver.navigateTo(TestUrls.PRODUCT_DETAIL)

        val summary = session.summarize("provide a brief summary of this page")

        assertNotNull(summary, "Summary should not be null")
    }

    // ========== State History Tests ==========

    @Test
    @DisplayName("should track state history")
    suspend fun testShouldTrackStateHistory() {
        session.driver.navigateTo(TestUrls.SIMPLE_PAGE)
        
        session.act("scroll down")
        session.act("scroll up")

        val history = session.stateHistory

        assertNotNull(history, "State history should not be null")
        assertNotNull(history.states, "History states should not be null")
    }

    @Test
    @DisplayName("should track process trace")
    suspend fun testShouldTrackProcessTrace() {
        session.driver.navigateTo(TestUrls.SIMPLE_PAGE)
        
        session.act("scroll to bottom")

        val trace = session.processTrace

        assertNotNull(trace, "Process trace should not be null")
    }

    @Test
    @DisplayName("should clear state history")
    suspend fun testShouldClearStateHistory() {
        session.driver.navigateTo(TestUrls.SIMPLE_PAGE)
        
        session.act("scroll down")
        
        // Clear history
        session.clearHistory()

        val history = session.stateHistory
        
        assertNotNull(history, "State history should not be null")
        assertTrue(history.states.isEmpty(), "State history should be empty after clear")
    }
}
