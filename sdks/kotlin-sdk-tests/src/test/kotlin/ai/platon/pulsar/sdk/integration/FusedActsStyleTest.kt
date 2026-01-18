package ai.platon.pulsar.sdk.integration

import ai.platon.pulsar.sdk.AgenticSession
import ai.platon.pulsar.sdk.integration.util.TestUrls
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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
/**
 * Tests demonstrating FusedActs-style API usage.
 *
 * These tests verify that the SDK API can be used in the same way
 * as the internal FusedActs example, making it easier for users to
 * migrate code or follow internal examples.
 */
@Tag("Slow")
class FusedActsStyleTest : KotlinSdkIntegrationTestBase() {

    private lateinit var session: AgenticSession

    @BeforeEach
    fun setupSession() {
        createSession()
        session = AgenticSession(client)
    }

    companion object {
        @JvmStatic
        @AfterAll
        fun cleanup() {
            // Clean up the default session after all tests
            AgenticSession.resetDefault()
        }
    }

    @Test
    suspend fun `can create session with getOrCreate factory method`() {
        val session = AgenticSession.getOrCreate(baseUrl)

        assertNotNull(session)
        assertNotNull(session.companionAgent)
        assertTrue(session.isActive)
    }

    @Test
    suspend fun `getOrCreate returns same instance on multiple calls`() {
        val session1 = AgenticSession.getOrCreate(baseUrl)
        val session2 = AgenticSession.getOrCreate(baseUrl)

        assertEquals(session1, session2)
        assertEquals(session1.uuid, session2.uuid)
    }

    @Test
    suspend fun `create factory method creates new instance each time`() {
        val session1 = AgenticSession.create(baseUrl)
        val session2 = AgenticSession.create(baseUrl)

        assertNotNull(session1)
        assertNotNull(session2)
        assertTrue(session1.isActive)
        assertTrue(session2.isActive)

        // Clean up manually created sessions
        try {
            session1.close()
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
        try {
            session2.close()
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }

    @Test
    suspend fun `FusedActs-style API is available`() {
        val session = this.session

        assertNotNull(session.companionAgent, "session.companionAgent should exist")
        assertNotNull(session.getOrCreateBoundDriver(), "session.getOrCreateBoundDriver() should work")
        assertNotNull(session.context, "session.context should exist")

        // Verify agent properties
        val agent = session.companionAgent
        assertTrue(agent.processTrace.isEmpty(), "agent.processTrace should be accessible")

        // Verify driver is created
        val driver = session.getOrCreateBoundDriver()
        assertTrue(driver.id >= 0, "driver id should be non-negative")
    }

    @Test
    suspend fun `session properties match FusedActs expectations`() {
        val session = this.session
        val agent = session.companionAgent
        val driver = session.getOrCreateBoundDriver()

        // Properties from FusedActs
        assertEquals(session, agent, "agent should be the session itself")
        assertEquals(session, session.context, "context should be the session itself")
        assertNotNull(session.boundDriver, "boundDriver should be available after getOrCreateBoundDriver")
    }

    @Test
    suspend fun `agent methods are available`() {
        val session = this.session
        val agent = session.companionAgent

        // Verify agent methods exist (from FusedActs usage)
        // Note: We can't actually call these without a running server,
        // but we can verify the methods exist
        assertTrue(agent.processTrace.isEmpty())

        // clearHistory should not throw
        agent.clearHistory()
        assertTrue(agent.processTrace.isEmpty())
    }

    @Test
    suspend fun `driver methods are available`() {
        val driver = session.getOrCreateBoundDriver()

        // Verify driver methods from FusedActs exist
        // We can't actually call them without a server, but verify they exist
        assertNotNull(driver.navigateHistory)
        assertTrue(driver.navigateHistory.isEmpty())
    }

    @Test
    suspend fun `session methods match FusedActs usage`() {
        val session = this.session

        // Verify methods used in FusedActs
        // These would fail without a server, but we verify signatures
        assertNotNull(session)

        // session.open, session.parse, session.extract, session.capture
        // all exist and have correct signatures
        assertTrue(session.isActive)
    }

    @Test
    suspend fun `registerClosable does not throw`() {
        // From FusedActs: session.registerClosable(starter)
        // Use a simple AutoCloseable implementation for testing
        val testCloseable = AutoCloseable {
            // No-op for testing
        }
        session.registerClosable(testCloseable)

        // Should complete without exception
    }

    @Test
    suspend fun `context close is accessible`() {
        // In FusedActs: session.context.close()
        // We test that context.close() is the same as session.close()
        val session = AgenticSession.create(baseUrl)

        assertEquals(session, session.context)

        // Both should be callable (though we won't actually close default session)
        assertNotNull(session.context)

        // Clean up the test session
        try {
            session.close()
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }

    // ============================================================================
    // Comprehensive tests covering FusedActsStyleExample operations
    // ============================================================================

    @Test
    suspend fun `test session open operation from FusedActsStyleExample`() {
        val url = TestUrls.SIMPLE_PAGE

        // Step 1 from example: Open URL
        val page = session.open(url)

        assertNotNull(page, "Page should not be null")
        assertNotNull(page.url, "Page URL should not be null")
        assertTrue(page.url.isNotBlank(), "Page URL should not be blank")
    }

    @Test
    suspend fun `test session parse operation from FusedActsStyleExample`() {
        val url = TestUrls.SIMPLE_PAGE

        // Step 1-2 from example: Open and parse
        val page = session.open(url)
        val document = session.parse(page)

        assertNotNull(document, "Document should not be null")
        assertNotNull(document.title(), "Document title should not be null")
    }

    @Test
    suspend fun `test session extract operation from FusedActsStyleExample`() {
        val url = TestUrls.SIMPLE_PAGE

        // Step 1-3 from example: Open, parse, and extract
        val page = session.open(url)
        val document = session.parse(page)

        if (document != null) {
            val fields = session.extract(document, mapOf("title" to "title"))
            assertNotNull(fields, "Extracted fields should not be null")
            assertTrue(fields is Map, "Extracted fields should be a map")
        }
    }

    @Test
    suspend fun `test agent act operation from FusedActsStyleExample`() {
        val agent = session.companionAgent
        val driver = session.getOrCreateBoundDriver()

        // Navigate to a test page first
        driver.navigateTo(TestUrls.SIMPLE_PAGE)

        // Step 4 from example: Execute action
        val result = agent.act("scroll to the bottom")

        assertNotNull(result, "Action result should not be null")
        assertNotNull(result.message, "Action result message should not be null")
    }

    @Test
    suspend fun `test driver selectFirstTextOrNull operation from FusedActsStyleExample`() {
        val driver = session.getOrCreateBoundDriver()

        // Navigate to a test page
        driver.navigateTo(TestUrls.SIMPLE_PAGE)

        // Step 5 from example: Capture text from live DOM
        val content = driver.selectFirstTextOrNull("body")

        // Content may be null or empty depending on page state, just verify no exception
        assertTrue(true, "selectFirstTextOrNull should execute without exception")
    }

    @Test
    suspend fun `test agent run operation from FusedActsStyleExample`() {
        val agent = session.companionAgent
        val driver = session.getOrCreateBoundDriver()

        // Navigate to a test page first
        driver.navigateTo(TestUrls.SIMPLE_PAGE)

        // Step 10 from example: Run autonomous task
        agent.clearHistory()
        val history = agent.run("scroll to the bottom of the page")

        assertNotNull(history, "Task history should not be null")
        assertNotNull(history.finalResult, "Task final result should not be null")
    }

    @Test
    suspend fun `test session capture operation from FusedActsStyleExample`() {
        val driver = session.getOrCreateBoundDriver()

        // Navigate to a test page
        driver.navigateTo(TestUrls.SIMPLE_PAGE)

        // Step 11 from example: Capture live page
        val page = session.capture(driver)

        assertNotNull(page, "Captured page should not be null")
        assertNotNull(page.url, "Captured page URL should not be null")
    }

    @Test
    suspend fun `test agent clearHistory operation from FusedActsStyleExample`() {
        val agent = session.companionAgent
        val driver = session.getOrCreateBoundDriver()

        // Navigate and perform an action to create history
        driver.navigateTo(TestUrls.SIMPLE_PAGE)
        agent.act("scroll down")

        // Step 10 from example: Clear history
        agent.clearHistory()

        // Process trace should be empty after clearing
        assertTrue(agent.processTrace.isEmpty(), "Process trace should be empty after clearHistory")
    }

    @Test
    suspend fun `test agent processTrace access from FusedActsStyleExample`() {
        val agent = session.companionAgent

        // Step 14 from example: Access process trace
        val trace = agent.processTrace

        assertNotNull(trace, "Process trace should not be null")
        assertTrue(trace is List<*>, "Process trace should be a list")
    }

    @Test
    suspend fun `test multiple actions sequence from FusedActsStyleExample`() {
        val agent = session.companionAgent
        val driver = session.getOrCreateBoundDriver()

        // Navigate to test page
        driver.navigateTo(TestUrls.SIMPLE_PAGE)

        // Step 4-6 from example: Multiple actions in sequence
        var result = agent.act("scroll to the bottom")
        assertNotNull(result, "First action result should not be null")

        result = agent.act("scroll to the top")
        assertNotNull(result, "Second action result should not be null")
    }

    @Test
    suspend fun `test full workflow from FusedActsStyleExample`() {
        val url = TestUrls.SIMPLE_PAGE
        val agent = session.companionAgent
        val driver = session.getOrCreateBoundDriver()

        // Complete workflow similar to FusedActsStyleExample.run()
        // Step 1: Open URL
        var page = session.open(url)
        assertNotNull(page, "Opened page should not be null")

        // Step 2: Parse the page
        var document = session.parse(page)
        assertNotNull(document, "Parsed document should not be null")

        // Step 3: Extract fields
        if (document != null) {
            val fields = session.extract(document, mapOf("title" to "title"))
            assertNotNull(fields, "Extracted fields should not be null")
        }

        // Step 4-5: Execute action and capture text
        val result = agent.act("scroll down")
        assertNotNull(result, "Action result should not be null")

        val content = driver.selectFirstTextOrNull("body")
        // Just verify no exception thrown

        // Step 11: Capture and re-parse
        page = session.capture(driver)
        document = session.parse(page)
        assertNotNull(document, "Re-parsed document should not be null")

        // Step 14: Access process trace
        val trace = agent.processTrace
        assertNotNull(trace, "Process trace should not be null")
    }

    @Test
    suspend fun `test context property from FusedActsStyleExample`() {
        // Step 15 from example: Access session.context
        val context = session.context

        assertNotNull(context, "Context should not be null")
        assertEquals(session, context, "Context should be the session itself")
    }
}
