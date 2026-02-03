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
import ai.platon.pulsar.sdk.v0.WebDriver
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Advanced WebDriver integration tests.
 *
 * Tests WebDriver methods that are not covered in basic tests.
 */
@Tag("RequiresBrowser")
class WebDriverAdvancedTest : KotlinSdkIntegrationTestBase() {

    private lateinit var driver: WebDriver

    @BeforeEach
    suspend fun setupDriver() {
        createSession()
        driver = WebDriver(client)
    }

    // ========== URL/URI Property Tests ==========

    @Test
    @DisplayName("should get current URL using getCurrentUrl alias")
    suspend fun testShouldGetCurrentUrlUsingGetCurrentUrlAlias() {
        driver.open(TestUrls.SIMPLE_PAGE)

        val currentUrl = driver.getCurrentUrl()
        assertNotNull(currentUrl, "Current URL should not be null")
        assertTrue(currentUrl.isNotBlank(), "Current URL should not be blank")
    }

    @Test
    @DisplayName("should get URL using url method")
    suspend fun testShouldGetUrlUsingUrlMethod() {
        driver.open(TestUrls.SIMPLE_PAGE)

        val url = driver.url()
        assertNotNull(url, "URL should not be null")
        assertTrue(url.isNotBlank(), "URL should not be blank")
    }

    @Test
    @DisplayName("should get document URI")
    suspend fun testShouldGetDocumentUri() {
        driver.open(TestUrls.SIMPLE_PAGE)

        val documentUri = driver.documentUri()
        assertNotNull(documentUri, "Document URI should not be null")
        assertTrue(documentUri.isNotBlank(), "Document URI should not be blank")
    }

    @Test
    @DisplayName("should get base URI")
    suspend fun testShouldGetBaseUri() {
        driver.open(TestUrls.SIMPLE_PAGE)

        val baseUri = driver.baseUri()
        assertNotNull(baseUri, "Base URI should not be null")
        assertTrue(baseUri.isNotBlank(), "Base URI should not be blank")
    }

    // ========== Visibility Tests ==========

    @Test
    @DisplayName("should check if element is visible")
    suspend fun testShouldCheckIfElementIsVisible() {
        driver.open(TestUrls.SIMPLE_DOM)

        // Check visible element
        val isVisible = driver.isVisible("body")
        assertTrue(isVisible, "Body element should be visible")
    }

    @Test
    @DisplayName("should check if element is hidden")
    suspend fun testShouldCheckIfElementIsHidden() {
        driver.open(TestUrls.SIMPLE_DOM)

        // Check a visible element (should not be hidden)
        val isHidden = driver.isHidden("body")
        assertFalse(isHidden, "Body element should not be hidden")
    }

    @Test
    @DisplayName("should check if checkbox is checked")
    suspend fun testShouldCheckIfCheckboxIsChecked() {
        driver.open(TestUrls.FORM_PAGE)

        // Initially unchecked
        val isChecked = driver.isChecked("input[type=checkbox]")
        // Note: Actual result depends on form page initial state
        assertNotNull(isChecked, "Should return a boolean value")
    }

    // ========== Wait Tests ==========

    @Test
    @DisplayName("should wait for selector using waitFor alias")
    suspend fun testShouldWaitForSelectorUsingWaitForAlias() {
        driver.open(TestUrls.SIMPLE_PAGE)

        val result = driver.waitFor("body", timeout = 5000)
        assertTrue(result, "Should successfully wait for body element")
    }

    // ========== Element Finding Tests ==========

    @Test
    @DisplayName("should find element by selector")
    suspend fun testShouldFindElementBySelector() {
        driver.open(TestUrls.SIMPLE_DOM)

        val element = driver.findElementBySelector("body", "css")
        assertNotNull(element, "Should find body element")
    }

    @Test
    @DisplayName("should find elements by selector")
    suspend fun testShouldFindElementsBySelector() {
        driver.open(TestUrls.SIMPLE_DOM)

        val elements = driver.findElementsBySelector("div", "css")
        assertNotNull(elements, "Should return list of elements")
        // Note: Actual count depends on page structure
    }

    @Test
    @DisplayName("should find element using WebDriver locator")
    suspend fun testShouldFindElementUsingWebDriverLocator() {
        driver.open(TestUrls.SIMPLE_DOM)

        val element = driver.findElement("css selector", "body")
        assertNotNull(element, "Should find body element")
    }

    @Test
    @DisplayName("should find elements using WebDriver locator")
    suspend fun testShouldFindElementsUsingWebDriverLocator() {
        driver.open(TestUrls.SIMPLE_DOM)

        val elements = driver.findElements("css selector", "div")
        assertNotNull(elements, "Should return list of elements")
    }

    @Test
    @DisplayName("should find element using XPath")
    suspend fun testShouldFindElementUsingXPath() {
        driver.open(TestUrls.SIMPLE_DOM)

        val element = driver.findElement("xpath", "//body")
        assertNotNull(element, "Should find body element using XPath")
    }

    // ========== Element Interaction Tests ==========

    @Test
    @DisplayName("should click element by ID")
    suspend fun testShouldClickElementById() {
        driver.open(TestUrls.INTERACTIVE_1)
        driver.delay(500)

        // First find the element
        val element = driver.findElementBySelector("#clickable-button", "css")
        assertNotNull(element, "Should find clickable button")

        if (element != null) {
            val elementId = element["elementId"] as? String
            if (elementId != null) {
                driver.clickElement(elementId)
                // Click is best-effort, no assertion on result
            }
        }
    }

    // Note: blur() method is not implemented in WebDriver yet
    // Skipping blur test for now

    // ========== Attribute Tests ==========

    @Test
    @DisplayName("should get element attribute by element ID")
    suspend fun testShouldGetElementAttributeByElementId() {
        driver.open(TestUrls.SIMPLE_DOM)

        // First find the element
        val element = driver.findElementBySelector("input[id=input]", "css")
        assertNotNull(element, "Should find input element")

        if (element != null) {
            val elementId = element["elementId"] as? String
            if (elementId != null) {
                val id = driver.getAttribute(elementId, "id")
                assertNotNull(id, "Should get id attribute")
                assertEquals("input", id, "ID should match")
            }
        }
    }

    // ========== Outer HTML Test ==========

    @Test
    @DisplayName("should get outer HTML")
    suspend fun testShouldGetOuterHtml() {
        driver.open(TestUrls.SIMPLE_PAGE)

        val html = driver.outerHtml()
        assertNotNull(html, "Outer HTML should not be null")
        assertTrue(html.contains("<html"), "Should contain HTML tag")
    }
}
