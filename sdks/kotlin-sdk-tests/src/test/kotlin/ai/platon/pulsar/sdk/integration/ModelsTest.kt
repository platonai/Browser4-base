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

import ai.platon.pulsar.sdk.v0.AgentActResult
import ai.platon.pulsar.sdk.v0.AgentRunResult
import ai.platon.pulsar.sdk.v0.NormURL
import ai.platon.pulsar.sdk.v0.ObserveResult
import ai.platon.pulsar.sdk.v0.WebPage
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Models DTO tests.
 *
 * Tests parsing and edge cases for SDK model classes.
 */
@Tag("Fast")
@Tag("IntegrationTest")
class ModelsTest {

    @Test
    @DisplayName("should create WebPage from map")
    fun testShouldCreateWebPageFromMap() {
        val data = mapOf(
            "url" to "https://example.com",
            "location" to "https://example.com/page",
            "contentType" to "text/html",
            "contentLength" to 1024,
            "protocolStatus" to "200",
            "html" to "<html><body>Test</body></html>"
        )

        val page = WebPage.fromMap(data)

        assertEquals("https://example.com", page.url)
        assertEquals("https://example.com/page", page.location)
        assertEquals("text/html", page.contentType)
        assertEquals(1024, page.contentLength)
        assertEquals("200", page.protocolStatus)
        assertEquals("<html><body>Test</body></html>", page.html)
        assertFalse(page.isNil, "Page should not be nil")
    }

    @Test
    @DisplayName("should handle nil WebPage")
    fun testShouldHandleNilWebPage() {
        val data = mapOf(
            "url" to NormURL.NIL_URL
        )

        val page = WebPage.fromMap(data)

        assertTrue(page.isNil, "Page should be nil")
    }

    @Test
    @DisplayName("should create NormURL from map")
    fun testShouldCreateNormUrlFromMap() {
        val data = mapOf(
            "url" to "https://example.com",
            "args" to "-expire 1d -refresh"
        )

        val normUrl = NormURL.fromMap(data)

        assertEquals("https://example.com", normUrl.url)
        assertEquals("-expire 1d -refresh", normUrl.args)
        assertEquals("https://example.com -expire 1d -refresh", normUrl.urlSpec)
        assertFalse(normUrl.isNil, "NormURL should not be nil")
    }

    @Test
    @DisplayName("should handle nil NormURL")
    fun testShouldHandleNilNormUrl() {
        val data = mapOf(
            "url" to NormURL.NIL_URL
        )

        val normUrl = NormURL.fromMap(data)

        assertTrue(normUrl.isNil, "NormURL should be nil")
    }

    @Test
    @DisplayName("should create AgentRunResult from map")
    fun testShouldCreateAgentRunResultFromMap() {
        val data = mapOf(
            "success" to true,
            "message" to "Task completed successfully",
            "historySize" to 5,
            "processTraceSize" to 3,
            "finalResult" to "Result data",
            "trace" to listOf("Step 1", "Step 2", "Step 3")
        )

        val result = AgentRunResult.fromMap(data)

        assertTrue(result.success, "Result should be successful")
        assertEquals("Task completed successfully", result.message)
        assertEquals(5, result.historySize)
        assertEquals(3, result.processTraceSize)
        assertEquals("Result data", result.finalResult)
        assertNotNull(result.trace)
        assertEquals(3, result.trace?.size)
    }

    @Test
    @DisplayName("should create AgentActResult from map")
    fun testShouldCreateAgentActResultFromMap() {
        val data = mapOf(
            "success" to true,
            "message" to "Action completed",
            "action" to "click",
            "isComplete" to true,
            "expression" to "document.querySelector('button').click()",
            "result" to "clicked",
            "trace" to listOf("Action trace")
        )

        val result = AgentActResult.fromMap(data)

        assertTrue(result.success, "Result should be successful")
        assertEquals("Action completed", result.message)
        assertEquals("click", result.action)
        assertTrue(result.isComplete, "Action should be complete")
        assertEquals("document.querySelector('button').click()", result.expression)
        assertEquals("clicked", result.result)
        assertNotNull(result.trace)
    }

    @Test
    @DisplayName("should create ObserveResult from map")
    fun testShouldCreateObserveResultFromMap() {
        val data = mapOf(
            "locator" to "#button",
            "domain" to "example.com",
            "method" to "click",
            "arguments" to mapOf("count" to 1)
        )

        val result = ObserveResult.fromMap(data)

        assertEquals("#button", result.locator)
        assertEquals("example.com", result.domain)
        assertEquals("click", result.method)
        assertNotNull(result.arguments)
        assertEquals(1, result.arguments?.get("count"))
    }

    @Test
    @DisplayName("should handle empty map for WebPage")
    fun testShouldHandleEmptyMapForWebPage() {
        val data = emptyMap<String, Any?>()

        val page = WebPage.fromMap(data)

        assertEquals("", page.url)
        assertEquals(null, page.location)
        assertEquals(null, page.contentType)
        assertEquals(0, page.contentLength)
    }

    @Test
    @DisplayName("should handle partial data for AgentRunResult")
    fun testShouldHandlePartialDataForAgentRunResult() {
        val data = mapOf(
            "success" to false,
            "message" to "Failed"
        )

        val result = AgentRunResult.fromMap(data)

        assertFalse(result.success)
        assertEquals("Failed", result.message)
        assertEquals(0, result.historySize)
        assertEquals(0, result.processTraceSize)
        assertEquals(null, result.finalResult)
        assertEquals(null, result.trace)
    }

    @Test
    @DisplayName("should handle number types in map")
    fun testShouldHandleNumberTypesInMap() {
        val data = mapOf(
            "url" to "https://example.com",
            "contentLength" to 1024L // Long instead of Int
        )

        val page = WebPage.fromMap(data)

        assertEquals(1024, page.contentLength)
    }
}
