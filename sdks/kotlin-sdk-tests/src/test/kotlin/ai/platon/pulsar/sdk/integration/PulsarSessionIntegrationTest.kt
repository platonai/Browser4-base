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

import ai.platon.pulsar.sdk.v0.PulsarSession
import ai.platon.pulsar.sdk.integration.util.TestUrls
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.*

/**
 * PulsarSession integration tests.
 *
 * Tests session functionality including page loading, data extraction,
 * and document parsing.
 */
@Tag("Fast")
class PulsarSessionIntegrationTest : KotlinSdkIntegrationTestBase() {

    private lateinit var session: PulsarSession

    @BeforeEach
    suspend fun setupSession() {
        createSession()
        session = PulsarSession(client)
    }

    @Test
    fun `should verify session is active`() {
        assertTrue(session.isActive, "Session should be active")
        assertNotNull(session.uuid, "Session UUID should not be null")
        assertTrue(session.uuid.isNotBlank(), "Session UUID should not be blank")
    }

    @Test
    suspend fun `should normalize URL`() {
        val url = "https://example.com"
        val normalized = session.normalize(url)

        assertNotNull(normalized, "Normalized URL should not be null")
        assertNotNull(normalized.url, "Normalized URL string should not be null")
        assertTrue(normalized.url.isNotBlank(), "Normalized URL should not be blank")
    }

    @Test
    suspend fun `should normalize URL with arguments`() {
        val url = "https://example.com"
        val args = "-expire 1d"
        val normalized = session.normalize(url, args)

        assertNotNull(normalized, "Normalized URL should not be null")
        assertNotNull(normalized.url, "Normalized URL string should not be null")
    }

    @Test
    suspend fun `should load page`() {
        val url = TestUrls.SIMPLE_PAGE
        val page = session.load(url)

        assertNotNull(page, "Page should not be null")
        assertFalse(page.isNil, "Page should not be nil")
        assertTrue(page.url.isNotBlank(), "Page URL should not be blank")
    }

    @Test
    suspend fun `should load page with arguments`() {
        val url = TestUrls.SIMPLE_PAGE
        val args = "-expire 1d"
        val page = session.load(url, args)

        assertNotNull(page, "Page should not be null")
        assertFalse(page.isNil, "Page should not be nil")
    }

    @Test
    suspend fun `should open page immediately`() {
        val url = TestUrls.SIMPLE_PAGE
        val page = session.open(url)

        assertNotNull(page, "Page should not be null")
        assertFalse(page.isNil, "Page should not be nil")
        assertTrue(page.url.isNotBlank(), "Page URL should not be blank")
    }

    @Test
    suspend fun `should parse page`() {
        val url = TestUrls.SIMPLE_PAGE
        val page = session.load(url)

        val document = session.parse(page)
        assertNotNull(document, "Parsed document should not be null")
    }

    @Test
    suspend fun `should extract fields from page with selectors`() {
        val url = TestUrls.PRODUCT_DETAIL
        val page = session.load(url)
        val document = session.parse(page)

        assertNotNull(document, "Document should not be null")

        val selectors = mapOf(
            "title" to "#productTitle",
            "price" to ".a-price-whole"
        )

        val result = session.extract(document, selectors)

        assertNotNull(result, "Extracted result should not be null")
        assertTrue(result.containsKey("title"), "Should have title field")
        assertTrue(result.containsKey("price"), "Should have price field")
    }

    @Test
    suspend fun `should scrape page with selectors`() {
        val url = TestUrls.PRODUCT_DETAIL
        val selectors = mapOf(
            "title" to "#productTitle",
            "price" to ".a-price-whole"
        )

        val result = session.scrape(url, "", selectors)

        assertNotNull(result, "Scrape result should not be null")
        assertTrue(result.containsKey("title"), "Should have title field")
        assertTrue(result.containsKey("price"), "Should have price field")
    }

    @Test
    suspend fun `should scrape page with arguments and selectors`() {
        val url = TestUrls.PRODUCT_DETAIL
        val args = "-expire 1d"
        val selectors = mapOf(
            "title" to "#productTitle"
        )

        val result = session.scrape(url, args, selectors)

        assertNotNull(result, "Scrape result should not be null")
        assertTrue(result.containsKey("title"), "Should have title field")
    }

    @Test
    suspend fun `should load multiple pages`() {
        val urls = listOf(
            TestUrls.SIMPLE_PAGE,
            TestUrls.PRODUCT_DETAIL
        )

        val pages = session.loadAll(urls)

        assertNotNull(pages, "Pages should not be null")
        assertEquals(urls.size, pages.size, "Should load all pages")
        pages.forEach { page ->
            assertNotNull(page, "Each page should not be null")
            assertFalse(page.isNil, "Each page should not be nil")
        }
    }

    @Test
    suspend fun `should load multiple pages with arguments`() {
        val urls = listOf(
            TestUrls.SIMPLE_PAGE,
            TestUrls.PRODUCT_LIST
        )
        val args = "-expire 1d"

        val pages = session.loadAll(urls, args)

        assertNotNull(pages, "Pages should not be null")
        assertEquals(urls.size, pages.size, "Should load all pages")
    }

    @Test
    suspend fun `should submit URL for async processing`() {
        val url = TestUrls.SIMPLE_PAGE

        val result = session.submit(url)

        // Submit returns boolean indicating if submission was successful
        assertNotNull(result, "Submit result should not be null")
    }

    @Test
    suspend fun `should submit multiple URLs`() {
        val urls = listOf(
            TestUrls.SIMPLE_PAGE,
            TestUrls.PRODUCT_LIST
        )

        val result = session.submitAll(urls)

        assertNotNull(result, "Submit result should not be null")
    }

    @Test
    suspend fun `should access bound driver`() {
        // Access driver through session
        val driver = session.driver

        assertNotNull(driver, "Driver should not be null")
        assertEquals(client, driver.client, "Driver should use same client")
    }

    @Test
    suspend fun `should handle page with nil status`() {
        // Try to load a page that might not exist
        val url = TestUrls.MOCK_SERVER_BASE + "/nonexistent"
        val page = session.load(url)

        assertNotNull(page, "Page object should not be null even if page doesn't exist")
        // Page may be nil or not, depending on server behavior
    }
}
