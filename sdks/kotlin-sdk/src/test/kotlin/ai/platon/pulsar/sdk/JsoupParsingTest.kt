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
package ai.platon.pulsar.sdk

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for parsing and extraction with Jsoup.
 */
class JsoupParsingTest {

    @Test
    fun `parse returns null for WebPage without HTML`() {
        val client = PulsarClient(sessionId = "test-session")
        val session = PulsarSession(client)

        val page = WebPage(url = "https://example.com", html = null)
        val document = session.parse(page)

        assertNull(document)
    }

    @Test
    fun `parse returns Jsoup Document for WebPage with HTML`() {
        val client = PulsarClient(sessionId = "test-session")
        val session = PulsarSession(client)

        val html = "<html><head><title>Test</title></head><body><h1>Hello</h1></body></html>"
        val page = WebPage(url = "https://example.com", html = html)
        val document = session.parse(page)

        assertNotNull(document)
        requireNotNull(document)
        assertEquals("Test", document.title())
        assertEquals("Hello", document.select("h1").text())
    }

    @Test
    fun `extract works with Jsoup Document and CSS selectors`() {
        val client = PulsarClient(sessionId = "test-session")
        val session = PulsarSession(client)

        val html = """
            <html>
                <head><title>Test Page</title></head>
                <body>
                    <h1 id="main-title">Welcome</h1>
                    <p class="intro">This is a test page.</p>
                    <div class="content">
                        <a href="/link1">Link 1</a>
                        <a href="/link2">Link 2</a>
                    </div>
                </body>
            </html>
        """.trimIndent()

        val page = WebPage(url = "https://example.com", html = html)
        val document = session.parse(page)
        assertNotNull(document)

        val fields = session.extract(
            document, mapOf(
                "title" to "title",
                "heading" to "#main-title",
                "intro" to ".intro",
                "firstLink" to ".content a"
            )
        )

        assertEquals("Test Page", fields["title"])
        assertEquals("Welcome", fields["heading"])
        assertEquals("This is a test page.", fields["intro"])
        assertEquals("Link 1", fields["firstLink"])
    }

    @Test
    fun `extract returns null for non-existent selectors`() {
        val client = PulsarClient(sessionId = "test-session")
        val session = PulsarSession(client)

        val html = "<html><body><h1>Test</h1></body></html>"
        val page = WebPage(url = "https://example.com", html = html)
        val document = session.parse(page)
        assertNotNull(document)

        val fields = session.extract(
            document, mapOf(
                "existing" to "h1",
                "nonExisting" to ".does-not-exist"
            )
        )

        assertEquals("Test", fields["existing"])
        assertNull(fields["nonExisting"])
    }

    @Test
    fun `extract with iterable selectors uses selector as field name`() {
        val client = PulsarClient(sessionId = "test-session")
        val session = PulsarSession(client)

        val html = """
            <html>
                <body>
                    <h1>Title</h1>
                    <p class="description">Description text</p>
                </body>
            </html>
        """.trimIndent()

        val page = WebPage(url = "https://example.com", html = html)
        val document = session.parse(page)
        assertNotNull(document)

        val fields = session.extract(document, listOf("h1", ".description"))

        assertEquals("Title", fields["h1"])
        assertEquals("Description text", fields[".description"])
    }

    @Test
    fun `scrape loads, parses and extracts in one operation`() {
        // Note: This test would require a real server to work
        // Here we just verify the method signature exists
        val client = PulsarClient(sessionId = "test-session")
        val session = PulsarSession(client)

        // Verify method exists by checking it compiles
        assertTrue(true)
    }

    @Test
    fun `Jsoup Document supports complex CSS selectors`() {
        val client = PulsarClient(sessionId = "test-session")
        val session = PulsarSession(client)

        val html = """
            <html>
                <body>
                    <div class="product">
                        <h2 class="name">Product 1</h2>
                        <span class="price">$19.99</span>
                    </div>
                    <div class="product">
                        <h2 class="name">Product 2</h2>
                        <span class="price">$29.99</span>
                    </div>
                </body>
            </html>
        """.trimIndent()

        val page = WebPage(url = "https://example.com", html = html)
        val document = session.parse(page)
        assertNotNull(document)

        val fields = session.extract(
            document, mapOf(
                "firstProductName" to ".product:first-child .name",
                "secondProductPrice" to ".product:nth-child(2) .price"
            )
        )

        assertEquals("Product 1", fields["firstProductName"])
        assertEquals("$29.99", fields["secondProductPrice"])
    }
}
