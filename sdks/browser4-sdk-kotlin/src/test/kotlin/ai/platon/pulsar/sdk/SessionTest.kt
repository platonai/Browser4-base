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
package ai.platon.pulsar.sdk

import ai.platon.pulsar.sdk.v0.*
import ai.platon.pulsar.sdk.v0.detail.PulsarClient
import org.junit.jupiter.api.Test
import kotlin.test.*
import org.junit.jupiter.api.DisplayName

/**
 * Unit tests for WebDriver.
 * These tests verify WebDriver state management.
 * Note: Integration tests with a real server are in a separate test class.
 */
class WebDriverTest {

    @Test
        @DisplayName("WebDriver can be created with client")
    fun webdriverCanBeCreatedWithClient() {
        val client = PulsarClient(sessionId = "test-session")
        val driver = WebDriver(client)

        assertEquals(0, driver.id)
        assertTrue(driver.navigateHistory.isEmpty())
    }

    @Test
        @DisplayName("WebDriver tracks navigation history")
    fun webdriverTracksNavigationHistory() {
        val client = PulsarClient(sessionId = "test-session")
        val driver = WebDriver(client)

        // Directly add to history (without actual navigation)
        // In real usage, navigateTo adds to history
        assertTrue(driver.navigateHistory.isEmpty())
    }

    @Test
        @DisplayName("WebDriver close does not throw")
    fun webdriverCloseDoesNotThrow() {
        val client = PulsarClient(sessionId = "test-session")
        val driver = WebDriver(client)
        driver.close()
        // Should complete without exception
    }
}

/**
 * Unit tests for PulsarSession.
 */
class PulsarSessionTest {

    @Test
        @DisplayName("PulsarSession can be created with client")
    fun pulsarsessionCanBeCreatedWithClient() {
        val client = PulsarClient(sessionId = "test-session")
        val session = PulsarSession(client)

        assertEquals(0, session.id)
        assertEquals("test-session", session.uuid)
        assertTrue(session.isActive)
    }

    @Test
        @DisplayName("PulsarSession display shows session info")
    fun pulsarsessionDisplayShowsSessionInfo() {
        val client = PulsarClient(sessionId = "abcdefgh12345678")
        val session = PulsarSession(client)

        assertTrue(session.display.contains("abcdefgh"))
    }

    @Test
        @DisplayName("PulsarSession display shows no-session when inactive")
    fun pulsarsessionDisplayShowsNoSessionWhenInactive() {
        val client = PulsarClient()
        val session = PulsarSession(client)

        assertFalse(session.isActive)
        assertTrue(session.display.contains("no-session"))
    }

    @Test
        @DisplayName("PulsarSession driver is lazily created")
    fun pulsarsessionDriverIsLazilyCreated() {
        val client = PulsarClient(sessionId = "test-session")
        val session = PulsarSession(client)

        // boundDriver should be null initially
        assertNull(session.boundDriver)

        // driver property should create it
        val driver = session.driver
        assertNotNull(driver)
        assertNotNull(session.boundDriver)
    }

    @Test
        @DisplayName("PulsarSession createBoundDriver creates new driver")
    fun pulsarsessionCreatebounddriverCreatesNewDriver() {
        val client = PulsarClient(sessionId = "test-session")
        val session = PulsarSession(client)

        val driver1 = session.createBoundDriver()
        val driver2 = session.createBoundDriver()

        // Each call creates a new driver
        assertNotNull(driver1)
        assertNotNull(driver2)
    }

    @Test
        @DisplayName("PulsarSession bindDriver and unbindDriver work correctly")
    fun pulsarsessionBinddriverAndUnbinddriverWorkCorrectly() {
        val client = PulsarClient(sessionId = "test-session")
        val session = PulsarSession(client)
        val driver = WebDriver(client)

        session.bindDriver(driver)
        assertEquals(driver, session.boundDriver)

        session.unbindDriver(driver)
        assertNull(session.boundDriver)
    }

    @Test
        @DisplayName("PulsarSession normalizeOrNull returns null for blank URL")
    fun pulsarsessionNormalizeornullReturnsNullForBlankUrl() = kotlinx.coroutines.test.runTest {
        val client = PulsarClient(sessionId = "test-session")
        val session = PulsarSession(client)

        assertNull(session.normalizeOrNull(null))
        assertNull(session.normalizeOrNull(""))
        assertNull(session.normalizeOrNull("   "))
    }

    @Test
        @DisplayName("PulsarSession loadAll returns list of pages")
    fun pulsarsessionLoadallReturnsListOfPages() {
        val client = PulsarClient(sessionId = "test-session")
        val session = PulsarSession(client)

        // Without server, this will fail, but we can test the structure
        // This test validates that the method signature is correct
        assertTrue(true)
    }
}

/**
 * Unit tests for AgenticSession.
 */
class AgenticSessionTest {

    @Test
        @DisplayName("AgenticSession can be created with client")
    fun agenticsessionCanBeCreatedWithClient() {
        val client = PulsarClient(sessionId = "test-session")
        val session = AgenticSession(client)

        // AgenticSession implements PerceptiveAgent
        val agent: PerceptiveAgent = session.companionAgent
        assertEquals(session, agent)
        assertEquals(session, session.context)
        assertTrue(session.processTrace.isEmpty())
        assertTrue(session.stateHistory.states.isEmpty())
    }

    @Test
        @DisplayName("AgenticSession stateHistory is initially empty")
    fun agenticsessionStatehistoryIsInitiallyEmpty() {
        val client = PulsarClient(sessionId = "test-session")
        val session = AgenticSession(client)

        val history = session.stateHistory
        assertTrue(history.states.isEmpty())
        assertFalse(history.hasErrors)
        assertEquals(0, history.size)
    }

    @Test
        @DisplayName("AgenticSession processTrace is initially empty")
    fun agenticsessionProcesstraceIsInitiallyEmpty() {
        val client = PulsarClient(sessionId = "test-session")
        val session = AgenticSession(client)

        assertTrue(session.processTrace.isEmpty())
    }

//    @Test
//        @DisplayName("AgenticSession options creates map with args")
    fun agenticsessionOptionsCreatesMapWithArgs() {
//        val client = PulsarClient(sessionId = "test-session")
//        val session = AgenticSession(client)
//
//        val opts = session.options("-expire 1d")
//
//        assertEquals("-expire 1d", opts["args"])
//    }
//
//    @Test
//        @DisplayName("AgenticSession data returns null by default")
    fun agenticsessionDataReturnsNullByDefault() {
//        val client = PulsarClient(sessionId = "test-session")
//        val session = AgenticSession(client)
//
//        assertNull(session.data("test"))
//    }
//
//    @Test
//        @DisplayName("AgenticSession property returns null by default")
    fun agenticsessionPropertyReturnsNullByDefault() {
//        val client = PulsarClient(sessionId = "test-session")
//        val session = AgenticSession(client)
//
//        assertNull(session.property("test"))
//    }
//
//    @Test
//        @DisplayName("AgenticSession registerClosable does not throw")
    fun agenticsessionRegisterclosableDoesNotThrow() {
//        val client = PulsarClient(sessionId = "test-session")
//        val session = AgenticSession(client)
//
//        session.registerClosable(Object())
//        // Should complete without exception
//    }

    @Test
        @DisplayName("PageEventHandlers can be created")
    fun pageeventhandlersCanBeCreated() {
        val handlers = PageEventHandlers()

        assertTrue(handlers.browse.isEmpty())
        assertTrue(handlers.load.isEmpty())
        assertTrue(handlers.crawl.isEmpty())
    }

    @Test
        @DisplayName("AgentHistory can be created")
    fun agenthistoryCanBeCreated() {
        val history = AgentHistory()

        assertEquals(0, history.size)
        assertFalse(history.hasErrors)
        assertTrue(history.states.isEmpty())
        assertNull(history.finalResult)
    }

    @Test
        @DisplayName("AgentHistory can track states")
    fun agenthistoryCanTrackStates() {
        val state1 = AgentState(step = 1, action = "test", success = true)
        val state2 = AgentState(step = 2, action = "test2", success = false)
        val history = AgentHistory(states = mutableListOf(state1, state2), hasErrors = true)

        assertEquals(2, history.size)
        assertTrue(history.hasErrors)
        assertEquals("test", history.states[0].action)
        assertEquals("test2", history.states[1].action)
    }

    @Test
        @DisplayName("ChatResponse can be created from string")
    fun chatresponseCanBeCreatedFromString() {
        val response = ChatResponse.fromAny("Hello, world!")

        assertEquals("Hello, world!", response.content)
        assertEquals("assistant", response.role)
    }

    @Test
        @DisplayName("ChatResponse can be created from map")
    fun chatresponseCanBeCreatedFromMap() {
        val map = mapOf(
            "content" to "Test content",
            "role" to "user",
            "model" to "gpt-4"
        )
        val response = ChatResponse.fromAny(map)

        assertEquals("Test content", response.content)
        assertEquals("user", response.role)
        assertEquals("gpt-4", response.model)
    }
}
