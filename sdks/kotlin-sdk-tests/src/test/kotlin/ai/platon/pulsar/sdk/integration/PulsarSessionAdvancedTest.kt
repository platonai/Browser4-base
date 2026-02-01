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
import ai.platon.pulsar.sdk.v0.PageEventHandlers
import ai.platon.pulsar.sdk.v0.PulsarSession
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Advanced PulsarSession integration tests.
 *
 * Tests advanced session functionality that is not covered in basic tests.
 */
@Tag("IntegrationTest")
class PulsarSessionAdvancedTest : KotlinSdkIntegrationTestBase() {

    private lateinit var session: PulsarSession

    @BeforeEach
    suspend fun setupSession() {
        createSession()
        session = PulsarSession(client)
    }

    @Test
    @DisplayName("should open page with event handlers")
    suspend fun testShouldOpenPageWithEventHandlers() {
        val url = TestUrls.SIMPLE_PAGE
        val eventHandlers = PageEventHandlers()
        
        var eventReceived = false
        eventHandlers.load.on("load") { event ->
            eventReceived = true
        }

        val page = session.open(url, eventHandlers)

        assertNotNull(page, "Page should not be null")
        assertTrue(page.url.isNotBlank(), "Page URL should not be blank")
        // Note: Event handling is best-effort, so we don't assert on eventReceived
    }

    @Test
    @DisplayName("should extract with selector list")
    suspend fun testShouldExtractWithSelectorList() {
        val url = TestUrls.PRODUCT_DETAIL
        val page = session.load(url)
        val document = session.parse(page)

        assertNotNull(document, "Document should not be null")

        val selectors = listOf("#productTitle", ".a-price-whole")
        val result = session.extract(document, selectors)

        assertNotNull(result, "Extracted result should not be null")
        assertTrue(result.containsKey("#productTitle"), "Should have #productTitle field")
        assertTrue(result.containsKey(".a-price-whole"), "Should have .a-price-whole field")
    }

    @Test
    @DisplayName("should chat with single prompt")
    suspend fun testShouldChatWithSinglePrompt() {
        val prompt = "What is 2 + 2?"

        val response = session.chat(prompt)

        assertNotNull(response, "Chat response should not be null")
        assertNotNull(response.content, "Response content should not be null")
    }

    @Test
    @DisplayName("should chat with user and system messages")
    suspend fun testShouldChatWithUserAndSystemMessages() {
        val userMessage = "Tell me a number between 1 and 10"
        val systemMessage = "You are a helpful assistant. Keep responses brief."

        val response = session.chat(userMessage, systemMessage)

        assertNotNull(response, "Chat response should not be null")
        assertNotNull(response.content, "Response content should not be null")
    }
}
