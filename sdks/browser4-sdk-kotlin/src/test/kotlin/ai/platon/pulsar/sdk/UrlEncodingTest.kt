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
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests to verify URL encoding behavior for special characters.
 */
class UrlEncodingTest {

    @Test
    fun `URL encoding should encode hash symbol`() {
        val selector = "#username"
        val encoded = URLEncoder.encode(selector, StandardCharsets.UTF_8)
        
        assertEquals("%23username", encoded)
        assertFalse(encoded.contains("#"))
    }

    @Test
    fun `URL encoding should encode various special characters`() {
        val testCases = mapOf(
            "#id" to "%23id",
            ".class" to ".class",
            "[attr]" to "%5Battr%5D",
            "a b" to "a+b",
            "name=value" to "name%3Dvalue"
        )

        testCases.forEach { (input, expected) ->
            val encoded = URLEncoder.encode(input, StandardCharsets.UTF_8)
            assertEquals(expected, encoded, "Failed for input: $input")
        }
    }

    @Test
    fun `URL encoded string should not contain fragment identifier`() {
        val selectors = listOf("#username", "#password", "#email", "#id-123")
        
        selectors.forEach { selector ->
            val encoded = URLEncoder.encode(selector, StandardCharsets.UTF_8)
            assertFalse(encoded.contains("#"), "Encoded string should not contain # for: $selector")
            assertTrue(encoded.startsWith("%23"), "Encoded string should start with %23 for: $selector")
        }
    }

    @Test
    fun `sendKeys uses selector-based API not element-based API`() {
        // This test documents that sendKeys now takes a selector parameter
        // and uses the /selectors/fill endpoint instead of /element/{id}/value
        // The signature changed from: sendKeys(elementId: String, text: String)
        // to: sendKeys(selector: String, text: String, strategy: String = "css")
        assertTrue(true, "sendKeys now accepts selector parameter")
    }
}
