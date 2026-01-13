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

import ai.platon.pulsar.sdk.PulsarClient
import org.junit.jupiter.api.Test
import kotlin.test.*

/**
 * PulsarClient integration tests.
 * 
 * Tests basic client functionality including session management,
 * HTTP operations, and error handling.
 */
class PulsarClientIntegrationTest : KotlinSdkIntegrationTestBase() {

    @Test
    fun `should create and delete session`() {
        // Create session
        val sessionId = client.createSession()
        assertNotNull(sessionId, "Session ID should not be null")
        assertTrue(sessionId.isNotBlank(), "Session ID should not be blank")
        
        // Verify session is set
        client.sessionId = sessionId
        assertEquals(sessionId, client.sessionId, "Session ID should match")
        
        // Delete session
        client.deleteSession()
        
        // Clear session ID
        client.sessionId = null
        assertNull(client.sessionId, "Session ID should be null after deletion")
    }

    @Test
    fun `should create session with capabilities`() {
        val capabilities = mapOf(
            "browserName" to "chrome",
            "pageLoadStrategy" to "normal"
        )
        
        val sessionId = client.createSession(capabilities)
        assertNotNull(sessionId, "Session ID should not be null")
        assertTrue(sessionId.isNotBlank(), "Session ID should not be blank")
        
        client.sessionId = sessionId
        client.deleteSession()
    }

    @Test
    fun `should make GET request`() {
        val sessionId = createSession()
        
        // GET current URL (should return empty or default value)
        val result = client.get("/session/$sessionId/url")
        assertNotNull(result, "GET result should not be null")
    }

    @Test
    fun `should make POST request`() {
        val sessionId = createSession()
        
        // POST navigate to URL
        val url = "http://localhost:18080/ec/"
        val result = client.post(
            "/session/$sessionId/url",
            mapOf("url" to url)
        )
        
        // Should return successfully
        assertNotNull(result, "POST result should not be null")
    }

    @Test
    fun `should handle errors gracefully`() {
        // Don't create session, try to access directly
        assertFailsWith<IllegalStateException> {
            client.post("/session/{sessionId}/url", mapOf("url" to "https://example.com"))
        }
    }

    @Test
    fun `should handle session not found`() {
        // Try to delete non-existent session
        client.sessionId = "non-existent-session-id"
        
        // Should throw or handle gracefully
        assertFailsWith<Exception> {
            client.deleteSession()
        }
    }

    @Test
    fun `should support multiple sessions`() {
        // Create first session
        val sessionId1 = client.createSession()
        assertNotNull(sessionId1)
        
        // Create second session (with new client instance)
        val client2 = PulsarClient(baseUrl = baseUrl)
        val sessionId2 = client2.createSession()
        assertNotNull(sessionId2)
        
        // Should be different
        assertNotEquals(sessionId1, sessionId2, "Session IDs should be different")
        
        // Clean up
        client.sessionId = sessionId1
        client.deleteSession()
        
        client2.sessionId = sessionId2
        client2.deleteSession()
        client2.close()
    }
}
