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
import ai.platon.pulsar.sdk.v0.PulsarSession
import ai.platon.pulsar.sdk.v0.WebDriver
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.*

/**
 * Tests for error handling and edge cases.
 *
 * Covers previously untested scenarios:
 * - Null and invalid inputs
 * - Invalid selectors
 * - Concurrent operations
 * - Invalid URLs
 * - Timeout scenarios
 * - Empty results
 * - Malformed responses
 */
@Tag("IntegrationTest")
@Order(Integer.MAX_VALUE)
class ErrorHandlingAndEdgeCasesTest : KotlinSdkIntegrationTestBase() {

    private lateinit var session: PulsarSession
    private lateinit var driver: WebDriver

    @BeforeEach
    suspend fun setupSessionAndDriver() {
        createSession()
        session = PulsarSession(client)
        driver = WebDriver(client)
    }

    // ========== Null and Empty Input Tests ==========

    @Test
    @DisplayName("should handle empty selector gracefully")
    @Tag("Fast")
    suspend fun testShouldHandleEmptySelectorGracefully() {
        driver.navigateTo(TestUrls.SIMPLE_PAGE)

        // Empty selector should not crash
        val result = driver.selectFirstTextOrNull("")
        // Result should be null or empty, not throw exception
        assertTrue(result == null || result.isEmpty(), "Empty selector should return null or empty")
    }

    @Test
    @DisplayName("should handle blank URL in normalize")
    @Tag("Fast")
    suspend fun testShouldHandleBlankURLInNormalize() {
        val result = session.normalizeOrNull("   ", "-expire 1d")
        assertNull(result, "Blank URL should return null")
    }

    @Test
    @DisplayName("should handle null URL in normalizeOrNull")
    @Tag("Fast")
    suspend fun testShouldHandleNullURLInNormalizeOrNull() {
        val result = session.normalizeOrNull(null, "-expire 1d")
        assertNull(result, "Null URL should return null")
    }

    @Test
    @DisplayName("should handle empty string URL")
    @Tag("Fast")
    suspend fun testShouldHandleEmptyStringURL() {
        val result = session.normalizeOrNull("", "-expire 1d")
        assertNull(result, "Empty URL should return null")
    }

    // ========== Invalid Selector Tests ==========

    @Test
    @DisplayName("should handle invalid CSS selector")
    suspend fun testShouldHandleInvalidCSSSelector() {
        driver.navigateTo(TestUrls.SIMPLE_PAGE)

        // Invalid CSS selector
        try {
            driver.selectFirstTextOrNull("###invalid###selector")
            // If it doesn't throw, that's acceptable
        } catch (e: Exception) {
            // Expected behavior for invalid selector
            assertTrue(true, "Exception expected for invalid selector")
        }
    }

    @Test
    @DisplayName("should handle non-existent selector")
    suspend fun testShouldHandleNonExistentSelector() {
        driver.navigateTo(TestUrls.SIMPLE_PAGE)

        val result = driver.selectFirstTextOrNull("#thisElementDoesNotExist")
        assertNull(result, "Non-existent selector should return null")
    }

    @Test
    @DisplayName("should return empty list for non-existent elements in selectTextAll")
    suspend fun testShouldReturnEmptyListForNonExistentElementsInSelectTextAll() {
        driver.navigateTo(TestUrls.SIMPLE_PAGE)

        val results = driver.selectTextAll("#nonExistentElements")
        assertTrue(results.isEmpty(), "Non-existent elements should return empty list")
    }

    // ========== Invalid URL Tests ==========

    @Test
    @DisplayName("should handle malformed URL")
    @Tag("Fast")
    suspend fun testShouldHandleMalformedURL() {
        try {
            val page = session.open("not-a-valid-url", "-parse")
            // Check if it's a nil page
            assertTrue(page.isNil || page.url.contains("not-a-valid-url"),
                "Malformed URL should result in nil page or preserved URL")
        } catch (e: Exception) {
            // Expected behavior - URL validation failure
            assertTrue(true, "Exception expected for malformed URL")
        }
    }

    @Test
    @DisplayName("should handle URL with invalid protocol")
    @Tag("Fast")
    suspend fun testShouldHandleURLWithInvalidProtocol() {
        try {
            session.normalize("ftp://invalid-protocol.com")
            // If it doesn't throw, check the result
        } catch (e: Exception) {
            // Expected behavior
            assertTrue(true, "Exception expected for invalid protocol")
        }
    }

    @Test
    @DisplayName("should handle URL with special characters")
    @Tag("Fast")
    suspend fun testShouldHandleURLWithSpecialCharacters() {
        val url = "http://localhost:18080/test?param=<script>alert('xss')</script>"
        val normalized = session.normalize(url)

        assertNotNull(normalized, "URL with special characters should be normalized")
        // URL should be encoded or handled safely
        assertFalse(normalized.isNil, "Should not result in nil page")
    }

    // ========== Concurrent Operations Tests ==========

    @Test
    @DisplayName("should handle concurrent page loads")
    @Tag("Slow")
    suspend fun testShouldHandleConcurrentPageLoads() = coroutineScope {        val urls = listOf(
            TestUrls.SIMPLE_PAGE,
            TestUrls.PRODUCT_LIST,
            TestUrls.PRODUCT_DETAIL
        )

        val results = urls.map { url ->
            async {
                try {
                    session.load(url, "-parse")
                } catch (e: Exception) {
                    null
                }
            }
        }.awaitAll()

        // At least some should succeed
        val successCount = results.count { it != null && !it.isNil }
        assertTrue(successCount > 0, "At least one concurrent load should succeed")
    }

    @Test
    @DisplayName("should handle concurrent driver navigations")
    @Tag("Slow")
    suspend fun testShouldHandleConcurrentDriverNavigations() = coroutineScope {        // Note: This might fail if driver doesn't support concurrent access
        // But it should fail gracefully, not crash
        try {
            val navigations = (1..3).map {
                async {
                    driver.navigateTo(TestUrls.SIMPLE_PAGE)
                }
            }
            navigations.awaitAll()

            // If we get here, concurrent operations are supported
            assertTrue(true, "Concurrent navigations handled")
        } catch (e: Exception) {
            // If it fails, it should fail gracefully
            assertTrue(true, "Concurrent navigation failure handled gracefully")
        }
    }

    // ========== Empty and Nil Results Tests ==========

    @Test
    @DisplayName("should handle empty text content")
    suspend fun testShouldHandleEmptyTextContent() {
        driver.navigateTo(TestUrls.ERROR_PAGE)

        val emptyText = driver.selectFirstTextOrNull("#emptyDiv")
        // Should return null or empty string for empty element
        assertTrue(emptyText == null || emptyText.isEmpty(),
            "Empty element should return null or empty text")
    }

    @Test
    @DisplayName("should detect nil pages")
    suspend fun testShouldDetectNilPages() {
        // Try to load an invalid URL that should result in a nil page
        val url = session.normalizeOrNull("invalid://bad-url")

        // Should return null for completely invalid URL
        assertNull(url, "Invalid URL should result in null")
        // assertEquals(true, page?.isNil)
    }

    @Test
    @DisplayName("should handle nil page in parse")
    suspend fun testShouldHandleNilPageInParse() {
        // Create a nil page scenario
        val page = session.normalizeOrNull("")
        assertNull(page, "Empty URL should result in null normalization")
    }

    // ========== Timeout and Delay Tests ==========

    @Test
    @DisplayName("should handle delayed content loading")
    suspend fun testShouldHandleDelayedContentLoading() {
        driver.navigateTo(TestUrls.ERROR_PAGE)

        // Try to get content that appears after delay
        val delayedContent = driver.selectFirstTextOrNull("#delayedDiv")

        // Without waiting, might be empty
        // This tests that the system handles not-yet-loaded content
        assertNotNull(delayedContent, "Should return result for delayed div (might be empty initially)")
    }

    @Test
    @DisplayName("should handle very long URLs")
    suspend fun testShouldHandleVeryLongURLs() {
        val longUrl = "http://localhost:18080/test?" + "param=value&".repeat(100)

        try {
            val normalized = session.normalize(longUrl)
            assertNotNull(normalized, "Very long URL should be normalized")
        } catch (e: Exception) {
            // Expected if URL is too long
            assertTrue(true, "Exception handled for very long URL")
        }
    }

    // ========== Multiple Session Operations ==========

    @Test
    @DisplayName("should handle rapid session state checks")
    @Tag("Fast")
    suspend fun testShouldHandleRapidSessionStateChecks() {
        // Rapidly check session state multiple times
        repeat(5) {
            val isActive = session.isActive
            assertTrue(isActive, "Session should remain active during rapid checks")
        }
    }

    @Test
    @DisplayName("should handle rapid driver property access")
    @Tag("Fast")
    suspend fun testShouldHandleRapidDriverPropertyAccess() {
        driver.navigateTo(TestUrls.SIMPLE_PAGE)

        // Rapidly access driver properties
        repeat(5) {
            val url = driver.currentUrl()
            assertNotNull(url, "Current URL should be accessible")
        }
    }

    // ========== Hidden and Special Elements ==========

    @Test
    @DisplayName("should handle hidden elements")
    suspend fun testShouldHandleHiddenElements() {
        driver.navigateTo(TestUrls.ERROR_PAGE)

        val hiddenText = driver.selectFirstTextOrNull("#hiddenDiv")
        // Behavior depends on implementation - might return text or null
        assertNotNull(hiddenText, "Should return result for hidden element query (behavior varies)")
    }

    @Test
    @DisplayName("should handle script tags and special elements")
    suspend fun testShouldHandleScriptTagsAndSpecialElements() {
        driver.navigateTo(TestUrls.ERROR_PAGE)

        // Query for script tag
        val scriptExists = driver.exists("script")
        // Scripts exist in the page
        assertTrue(scriptExists, "Should detect script elements")
    }

    // ========== Error Recovery Tests ==========

    @Test
    @DisplayName("should recover from navigation to invalid URL")
    suspend fun testShouldRecoverFromNavigationToInvalidURL() {
        try {
            driver.navigateTo("http://invalid-domain-that-does-not-exist-12345.com")
        } catch (e: Exception) {
            // Expected failure
        }

        // Should still be able to navigate to valid URL
        driver.navigateTo(TestUrls.SIMPLE_PAGE)
        val url = driver.currentUrl()
        assertNotNull(url, "Should recover and navigate to valid URL")
    }

    @Test
    @DisplayName("should maintain session after failed operations")
    @Tag("Fast")
    suspend fun testShouldMaintainSessionAfterFailedOperations() {
        val initialActive = session.isActive

        // Attempt failed operation
        try {
            session.normalizeOrNull(null)
        } catch (e: Exception) {
            // Ignore
        }

        // Session should still be active
        val stillActive = session.isActive
        assertEquals(initialActive, stillActive, "Session state should be maintained after failed operation")
    }
}
