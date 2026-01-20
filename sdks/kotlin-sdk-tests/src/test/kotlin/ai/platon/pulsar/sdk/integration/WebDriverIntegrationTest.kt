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

import ai.platon.pulsar.sdk.v0.WebDriver
import ai.platon.pulsar.sdk.integration.util.TestUrls
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.*

/**
 * WebDriver integration tests.
 *
 * Tests browser automation functionality including navigation,
 * element interaction, content extraction, and more.
 */
@Tag("RequiresBrowser")
class WebDriverIntegrationTest : KotlinSdkIntegrationTestBase() {

    private lateinit var driver: WebDriver

    @BeforeEach
    suspend fun setupDriver() {
        createSession()
        driver = WebDriver(client)
    }

    @Test
    suspend fun `should navigate to URL`() {
        val url = TestUrls.SIMPLE_PAGE
        driver.navigateTo(url)

        val currentUrl = driver.currentUrl()
        assertNotNull(currentUrl, "Current URL should not be null")
        assertTrue(currentUrl.contains(url) || currentUrl == url, "Current URL should match navigated URL")
    }

    @Test
    suspend fun `should get page title`() {
        driver.navigateTo(TestUrls.SIMPLE_PAGE)

        val title = driver.title()
        assertNotNull(title, "Title should not be null")
        assertTrue(title.isNotBlank(), "Title should not be blank")
    }

    @Test
    suspend fun `should check element exists`() {
        driver.navigateTo(TestUrls.SIMPLE_PAGE)

        // Check common elements
        assertTrue(driver.exists("body"), "Body element should exist")
        assertTrue(driver.exists("html"), "HTML element should exist")
    }

    @Test
    suspend fun `should extract text content`() {
        driver.navigateTo(TestUrls.PRODUCT_DETAIL)

        val title = driver.selectFirstTextOrNull("#productTitle")
        assertNotNull(title, "Product title should not be null")
        assertTrue(title.isNotBlank(), "Product title should not be blank")
    }

    @Test
    suspend fun `should scroll page`() {
        driver.navigateTo(TestUrls.PRODUCT_LIST)

        // Scroll to bottom
        driver.scrollToBottom()

        // Scroll to top
        driver.scrollToTop()
    }

    @Test
    @Tag("Slow")
    suspend fun `should capture screenshot`() {
        driver.navigateTo(TestUrls.SIMPLE_PAGE)

        val screenshot = driver.captureScreenshot()
        assertNotNull(screenshot, "Screenshot should not be null")
        assertTrue(screenshot.isNotEmpty(), "Screenshot should not be empty")
    }

    @Test
    suspend fun `should execute script`() {
        driver.navigateTo(TestUrls.SIMPLE_PAGE)

        val result = driver.executeScript("return document.title")
        assertNotNull(result, "Script result should not be null")
    }

    @Test
    suspend fun `should wait for selector`() {
        driver.navigateTo(TestUrls.SIMPLE_PAGE)

        // Wait for body element
        driver.waitForSelector("body", timeout = 5000)
        assertTrue(driver.exists("body"), "Body element should exist after wait")
    }

    @Test
    suspend fun `should extract multiple fields`() {
        driver.navigateTo(TestUrls.PRODUCT_DETAIL)

        val fields = driver.extract(mapOf(
            "title" to "#productTitle",
            "price" to ".a-price-whole"
        ))

        assertNotNull(fields, "Extracted fields should not be null")
        assertTrue(fields.containsKey("title"), "Should have title field")
    }

    @Test
    suspend fun `should get page source`() {
        driver.navigateTo(TestUrls.SIMPLE_PAGE)

        val source = driver.pageSource()
        assertNotNull(source, "Page source should not be null")
        assertTrue(source.contains("<html"), "Page source should contain HTML")
    }

    @Test
    suspend fun `should navigate back and forward`() {
        // Navigate to first page
        driver.navigateTo(TestUrls.SIMPLE_PAGE)
        val url1 = driver.currentUrl()

        // Navigate to second page
        driver.navigateTo(TestUrls.PRODUCT_LIST)
        val url2 = driver.currentUrl()

        assertNotEquals(url1, url2, "URLs should be different")

        // Go back
        driver.goBack()

        // Go forward
        driver.goForward()
    }

    @Test
    suspend fun `should reload page`() {
        driver.navigateTo(TestUrls.SIMPLE_PAGE)
        val url1 = driver.currentUrl()

        driver.reload()

        val url2 = driver.currentUrl()
        assertEquals(url1, url2, "URL should remain the same after reload")
    }

    // ========== Form Interaction Tests ==========

    @Test
    suspend fun `should fill input field`() {
        val testText = "awesome AI enabled Browser4!"
        driver.navigateTo(TestUrls.SIMPLE_DOM)

        // Fill input field
        driver.fill("input[id=input]", testText)

        // Verify the value was set
        val result = driver.executeScript("return document.querySelector('input[id=input]').value")
        assertEquals(testText, result, "Input value should match filled text")
    }

    @Test
    suspend fun `should type into input field`() {
        val testText = "Testing type functionality"
        driver.navigateTo(TestUrls.SIMPLE_DOM)

        // Type into input field
        driver.type("input[id=input]", testText)

        // Verify the value was set
        val result = driver.executeScript("return document.querySelector('input[id=input]').value")
        assertEquals(testText, result, "Input value should match typed text")
    }

    @Test
    suspend fun `should fill textarea`() {
        val testText = "Multi-line\ntext content\nfor testing"
        driver.navigateTo(TestUrls.SIMPLE_DOM)

        // Fill textarea
        driver.fill("textarea[id=textarea]", testText)

        // Verify the value was set
        val result = driver.executeScript("return document.querySelector('textarea[id=textarea]').value")
        assertEquals(testText, result, "Textarea value should match filled text")
    }

    // ========== Scrolling Tests ==========

    @Test
    suspend fun `should scroll by pixels`() {
        driver.navigateTo(TestUrls.MULTI_SCREENS)

        // Scroll down by 200 pixels
        val scrollY = driver.scrollBy(200.0, smooth = true)

        // Give time for smooth scrolling to complete
        driver.delay(500)

        // Verify scroll position
        val actualY = driver.executeScript("return window.scrollY") as? Number
        assertNotNull(actualY, "Scroll position should not be null")
        assertTrue(actualY.toDouble() >= 180.0, "Should have scrolled at least 180 pixels (got ${actualY.toDouble()})")
    }

    @Test
    suspend fun `should scroll to top`() {
        driver.navigateTo(TestUrls.MULTI_SCREENS)

        // First scroll down
        driver.scrollToBottom()
        driver.delay(300)

        // Then scroll to top
        val topY = driver.scrollToTop()
        driver.delay(300)

        // Verify we're at the top
        val actualY = driver.executeScript("return window.scrollY") as? Number
        assertNotNull(actualY, "Scroll position should not be null")
        assertEquals(0.0, actualY.toDouble(), 5.0, "Should be at top of page")
        assertEquals(0.0, topY, 5.0, "Returned position should be 0")
    }

    @Test
    suspend fun `should scroll to bottom`() {
        driver.navigateTo(TestUrls.MULTI_SCREENS)

        // Scroll to bottom
        val bottomY = driver.scrollToBottom()
        driver.delay(300)

        // Calculate expected bottom position
        val viewportHeight = (driver.executeScript("return window.innerHeight") as? Number)?.toDouble() ?: 0.0
        val totalHeight = (driver.executeScript(
            "return Math.min(Math.max(document.documentElement.scrollHeight, document.body.scrollHeight), 15000)"
        ) as? Number)?.toDouble() ?: 0.0
        val expectedBottomY = (totalHeight - viewportHeight).coerceAtLeast(0.0)

        // Verify scroll position
        val actualY = (driver.executeScript("return window.scrollY") as? Number)?.toDouble() ?: 0.0
        assertEquals(expectedBottomY, bottomY, 10.0, "Returned position should match expected bottom")
        assertEquals(expectedBottomY, actualY, 10.0, "Actual position should match expected bottom")
    }

    @Test
    suspend fun `should scroll to middle`() {
        driver.navigateTo(TestUrls.MULTI_SCREENS)

        // Scroll to middle (50%)
        val ratio = 0.5
        val middleY = driver.scrollToMiddle(ratio)
        driver.delay(300)

        // Calculate expected middle position
        val viewportHeight = (driver.executeScript("return window.innerHeight") as? Number)?.toDouble() ?: 0.0
        val totalHeight = (driver.executeScript(
            "return Math.min(Math.max(document.documentElement.scrollHeight, document.body.scrollHeight), 15000)"
        ) as? Number)?.toDouble() ?: 0.0
        val maxScrollY = (totalHeight - viewportHeight).coerceAtLeast(0.0)
        val expectedMiddleY = maxScrollY * ratio

        // Verify scroll position
        val actualY = (driver.executeScript("return window.scrollY") as? Number)?.toDouble() ?: 0.0
        assertEquals(expectedMiddleY, middleY, 10.0, "Returned position should match expected middle")
        assertEquals(expectedMiddleY, actualY, 10.0, "Actual position should match expected middle")
    }

    // ========== Hover Tests ==========

    @Test
    @Tag("Slow")
    suspend fun `should hover over element`() {
        driver.navigateTo(TestUrls.INTERACTIVE_2)
        driver.delay(500)

        // Scroll to top to ensure page is in stable state
        driver.scrollToTop()
        driver.delay(300)

        // Get initial bounding rect
        val rect1 = driver.executeScript(
            "return JSON.stringify(document.querySelector('.hover-card').getBoundingClientRect())"
        )

        // Hover over the hover card
        driver.hover(".hover-card")
        driver.delay(700)

        // Get bounding rect after hover
        val rect2 = driver.executeScript(
            "return JSON.stringify(document.querySelector('.hover-card').getBoundingClientRect())"
        )

        // The rectangles should be different due to the transform on hover
        assertNotEquals(rect1, rect2, "Element should change on hover")
    }

    // ========== Property Tests ==========

    @Test
    suspend fun `should check element visibility`() {
        driver.navigateTo(TestUrls.SIMPLE_DOM)

        // Check visible element
        assertTrue(driver.exists("input[id=input]"), "Input element should exist")
    }

    @Test
    suspend fun `should extract text content`() {
        driver.navigateTo(TestUrls.SIMPLE_DOM)

        // Get text content
        val text = driver.selectFirstTextOrNull("#inner")
        assertNotNull(text, "Text content should not be null")
        assertTrue(text.contains("Text"), "Text should contain expected content")
    }
}
