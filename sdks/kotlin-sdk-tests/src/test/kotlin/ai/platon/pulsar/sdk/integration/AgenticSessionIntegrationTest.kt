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

import ai.platon.pulsar.sdk.v0.AgenticSession
import ai.platon.pulsar.sdk.integration.util.TestUrls
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * AgenticSession integration tests.
 *
 * Tests AI-driven browser automation functionality.
 *
 * Note: When `pulsar.test.mode=true` the server returns lightweight
 * stubbed responses so the suite can run without a real LLM/backend.
 */
@Tag("Slow")
class AgenticSessionIntegrationTest : KotlinSdkIntegrationTestBase() {

    private lateinit var session: AgenticSession

    @BeforeEach
    suspend fun setupSession() {
        createSession()
        session = AgenticSession(client)
        // Assumptions.assumeTrue(ChatModelFactory.isModelConfigured(ImmutableConfig()))
    }

    @Test
    suspend fun `should execute single action`() {
        session.driver.navigateTo(TestUrls.SIMPLE_PAGE)

        val result = session.act("scroll to the bottom")

        assertNotNull(result, "Action result should not be null")
        // AI functionality may return different results, just verify no exception thrown
    }

    @Test
    suspend fun `should execute action with parameters`() {
        session.driver.navigateTo(TestUrls.PRODUCT_LIST)

        val result = session.act("click on the first product")

        assertNotNull(result, "Action result should not be null")
    }

    @Test
    suspend fun `should run autonomous task`() {
        val result = session.run("visit ${TestUrls.PRODUCT_DETAIL} and summarize the product")

        assertNotNull(result, "Task result should not be null")
        assertTrue(result.message.isNotBlank(), "Task result message should not be blank")
    }

    @Test
    suspend fun `should run task with multiple steps`() {
        val task = """
            1. Visit ${TestUrls.PRODUCT_LIST}
            2. Find the first product
            3. Get its name and price
        """.trimIndent()

        val result = session.run(task)

        assertNotNull(result, "Task result should not be null")
    }

    @Test
    suspend fun `should observe page`() {
        session.driver.navigateTo(TestUrls.PRODUCT_DETAIL)

        val observation = session.observe("find interactive elements")

        assertNotNull(observation, "Observation should not be null")
        assertNotNull(observation.observations, "Observations list should not be null")
    }

    @Test
    suspend fun `should observe page with specific focus`() {
        session.driver.navigateTo(TestUrls.PRODUCT_DETAIL)

        val observation = session.observe("find all buttons and links")

        assertNotNull(observation, "Observation should not be null")
    }

    @Test
    suspend fun `should extract data with AI`() {
        session.driver.navigateTo(TestUrls.PRODUCT_DETAIL)

        val extraction = session.agentExtract("extract product name, price, and description")

        assertNotNull(extraction, "Extraction should not be null")
        assertNotNull(extraction.data, "Extracted data should not be null")
    }

    @Test
    suspend fun `should extract structured data`() {
        session.driver.navigateTo(TestUrls.PRODUCT_DETAIL)

        val extraction = session.agentExtract("extract all product information as structured data")

        assertNotNull(extraction, "Extraction should not be null")
        assertNotNull(extraction.data, "Extracted data should not be null")
    }

    @Test
    suspend fun `should summarize page`() {
        session.driver.navigateTo(TestUrls.PRODUCT_DETAIL)

        val summary = session.summarize()

        assertNotNull(summary, "Summary should not be null")
        assertTrue(summary.isNotBlank(), "Summary should not be blank")
    }

    @Test
    suspend fun `should summarize with custom instruction`() {
        session.driver.navigateTo(TestUrls.PRODUCT_LIST)

        val summary = session.summarize("summarize the available products")

        assertNotNull(summary, "Summary should not be null")
        assertTrue(summary.isNotBlank(), "Summary should not be blank")
    }

    @Test
    suspend fun `should handle complex multi-step workflow`() {
        // Navigate to product list
        session.driver.navigateTo(TestUrls.PRODUCT_LIST)

        // Use AI to find and click first product
        session.act("click on the first product")

        // Wait a bit for navigation
        Thread.sleep(2000)

        // Extract product details
        val extraction = session.agentExtract("extract product name and price")

        assertNotNull(extraction, "Extraction should not be null")
        assertNotNull(extraction.data, "Extracted data should not be null")
    }

    @Test
    suspend fun `should combine manual and AI operations`() {
        // Manual navigation
        session.driver.navigateTo(TestUrls.PRODUCT_DETAIL)

        // Manual element check
        assertTrue(session.driver.exists("body"), "Body should exist")

        // AI observation
        val observation = session.observe("analyze the page structure")

        assertNotNull(observation, "Observation should not be null")

        // AI summarization
        val summary = session.summarize()

        assertNotNull(summary, "Summary should not be null")
    }

    @Test
    suspend fun `should handle errors gracefully`() {
        // Try to perform action without navigating first
        try {
            session.act("find a non-existent element")
            // Should either complete or throw exception
        } catch (e: Exception) {
            // Expected - AI may not be able to perform the action
            assertNotNull(e.message, "Error message should not be null")
        }
    }

    @Test
    suspend fun `should respect session state`() {
        // Verify session is from parent class
        assertTrue(session.isActive, "Session should be active")
        assertNotNull(session.uuid, "Session UUID should not be null")

        // Verify driver is accessible
        assertNotNull(session.driver, "Driver should be accessible")

        // Verify session can perform AI operations
        session.driver.navigateTo(TestUrls.SIMPLE_PAGE)
        val summary = session.summarize()
        assertNotNull(summary, "Summary should not be null")
    }
}
