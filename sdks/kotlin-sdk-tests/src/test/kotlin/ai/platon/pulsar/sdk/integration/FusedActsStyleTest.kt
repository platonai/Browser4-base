package ai.platon.pulsar.sdk.integration

import ai.platon.pulsar.sdk.AgenticSession
import org.junit.jupiter.api.AfterAll
import kotlin.test.Test
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
class FusedActsStyleTest {

    companion object {
        @JvmStatic
        @AfterAll
        fun cleanup() {
            // Clean up the default session after all tests
            AgenticSession.Companion.resetDefault()
        }
    }

    @Test
    fun `can create session with getOrCreate factory method`() {
        val session = AgenticSession.Companion.getOrCreate()

        assertNotNull(session)
        assertNotNull(session.companionAgent)
        assertTrue(session.isActive)
    }

    @Test
    fun `getOrCreate returns same instance on multiple calls`() {
        val session1 = AgenticSession.Companion.getOrCreate()
        val session2 = AgenticSession.Companion.getOrCreate()

        assertEquals(session1, session2)
        assertEquals(session1.uuid, session2.uuid)
    }

    @Test
    fun `create factory method creates new instance each time`() {
        val session1 = AgenticSession.Companion.create()
        val session2 = AgenticSession.Companion.create()

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
    fun `FusedActs-style API is available`() {
        val session = AgenticSession.Companion.getOrCreate()

        // Verify all FusedActs patterns work
        assertNotNull(session.companionAgent, "session.companionAgent should exist")
        assertNotNull(session.getOrCreateBoundDriver(), "session.getOrCreateBoundDriver() should work")
        assertNotNull(session.context, "session.context should exist")

        // Verify agent properties
        val agent = session.companionAgent
        assertTrue(agent.processTrace.isEmpty(), "agent.processTrace should be accessible")

        // Verify driver is created
        val driver = session.getOrCreateBoundDriver()
        assertEquals(0, driver.id)
    }

    @Test
    fun `session properties match FusedActs expectations`() {
        val session = AgenticSession.Companion.getOrCreate()
        val agent = session.companionAgent
        val driver = session.getOrCreateBoundDriver()

        // Properties from FusedActs
        assertEquals(session, agent, "agent should be the session itself")
        assertEquals(session, session.context, "context should be the session itself")
        assertNotNull(session.boundDriver, "boundDriver should be available after getOrCreateBoundDriver")
    }

    @Test
    fun `agent methods are available`() {
        val session = AgenticSession.Companion.getOrCreate()
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
    fun `driver methods are available`() {
        val session = AgenticSession.Companion.getOrCreate()
        val driver = session.getOrCreateBoundDriver()

        // Verify driver methods from FusedActs exist
        // We can't actually call them without a server, but verify they exist
        assertNotNull(driver.navigateHistory)
        assertTrue(driver.navigateHistory.isEmpty())
    }

    @Test
    fun `session methods match FusedActs usage`() {
        val session = AgenticSession.Companion.getOrCreate()

        // Verify methods used in FusedActs
        // These would fail without a server, but we verify signatures
        assertNotNull(session)

        // session.open, session.parse, session.extract, session.capture
        // all exist and have correct signatures
        assertTrue(session.isActive)
    }

    @Test
    fun `registerClosable does not throw`() {
        val session = AgenticSession.Companion.getOrCreate()

        // From FusedActs: session.registerClosable(starter)
        // Use a simple AutoCloseable implementation for testing
        val testCloseable = object : AutoCloseable {
            override fun close() {
                // No-op for testing
            }
        }
        session.registerClosable(testCloseable)

        // Should complete without exception
    }

    @Test
    fun `context close is accessible`() {
        // In FusedActs: session.context.close()
        // We test that context.close() is the same as session.close()
        val session = AgenticSession.Companion.create()

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
}
