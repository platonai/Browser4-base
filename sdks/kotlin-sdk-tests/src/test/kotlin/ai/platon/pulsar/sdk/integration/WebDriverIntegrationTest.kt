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

import ai.platon.pulsar.sdk.WebDriver
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
    fun setupDriver() {
        createSession()
        driver = WebDriver(client)
    }

    @Test
    fun `should navigate to URL`() {
        val url = TestUrls.SIMPLE_PAGE
        driver.navigateTo(url)
        
        val currentUrl = driver.currentUrl()
        assertNotNull(currentUrl, "Current URL should not be null")
        assertTrue(currentUrl.contains(url) || currentUrl == url, "Current URL should match navigated URL")
    }

    @Test
    fun `should get page title`() {
        driver.navigateTo(TestUrls.SIMPLE_PAGE)
        
        val title = driver.title()
        assertNotNull(title, "Title should not be null")
        assertTrue(title.isNotBlank(), "Title should not be blank")
    }

    @Test
    fun `should check element exists`() {
        driver.navigateTo(TestUrls.SIMPLE_PAGE)
        
        // Check common elements
        assertTrue(driver.exists("body"), "Body element should exist")
        assertTrue(driver.exists("html"), "HTML element should exist")
    }

    @Test
    fun `should extract text content`() {
        driver.navigateTo(TestUrls.PRODUCT_DETAIL)
        
        val title = driver.selectFirstTextOrNull("#productTitle")
        assertNotNull(title, "Product title should not be null")
        assertTrue(title.isNotBlank(), "Product title should not be blank")
    }

    @Test
    fun `should scroll page`() {
        driver.navigateTo(TestUrls.PRODUCT_LIST)
        
        // Scroll to bottom
        driver.scrollToBottom()
        
        // Scroll to top
        driver.scrollToTop()
    }

    @Test
    @Tag("Slow")
    fun `should capture screenshot`() {
        driver.navigateTo(TestUrls.SIMPLE_PAGE)
        
        val screenshot = driver.captureScreenshot()
        assertNotNull(screenshot, "Screenshot should not be null")
        assertTrue(screenshot.isNotEmpty(), "Screenshot should not be empty")
    }

    @Test
    fun `should execute script`() {
        driver.navigateTo(TestUrls.SIMPLE_PAGE)
        
        val result = driver.executeScript("return document.title")
        assertNotNull(result, "Script result should not be null")
    }

    @Test
    fun `should wait for selector`() {
        driver.navigateTo(TestUrls.SIMPLE_PAGE)
        
        // Wait for body element
        driver.waitForSelector("body", timeout = 5000)
        assertTrue(driver.exists("body"), "Body element should exist after wait")
    }

    @Test
    fun `should extract multiple fields`() {
        driver.navigateTo(TestUrls.PRODUCT_DETAIL)
        
        val fields = driver.extract(mapOf(
            "title" to "#productTitle",
            "price" to ".a-price-whole"
        ))
        
        assertNotNull(fields, "Extracted fields should not be null")
        assertTrue(fields.containsKey("title"), "Should have title field")
    }

    @Test
    fun `should get page source`() {
        driver.navigateTo(TestUrls.SIMPLE_PAGE)
        
        val source = driver.pageSource()
        assertNotNull(source, "Page source should not be null")
        assertTrue(source.contains("<html"), "Page source should contain HTML")
    }

    @Test
    fun `should navigate back and forward`() {
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
    fun `should reload page`() {
        driver.navigateTo(TestUrls.SIMPLE_PAGE)
        val url1 = driver.currentUrl()
        
        driver.reload()
        
        val url2 = driver.currentUrl()
        assertEquals(url1, url2, "URL should remain the same after reload")
    }
}
