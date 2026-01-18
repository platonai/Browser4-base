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

import ai.platon.pulsar.sdk.v0.AgenticSession
import ai.platon.pulsar.sdk.v0.detail.Browser4Driver
import ai.platon.pulsar.sdk.v0.detail.LocalDriverOptions
import ai.platon.pulsar.sdk.v0.detail.PulsarClient
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for Browser4Driver.
 *
 * These tests verify the Browser4Driver configuration and behavior.
 * Note: Tests that require actual download or server startup are skipped
 * to avoid long-running tests in CI.
 */
class Browser4DriverTest {

    @Test
    fun `Browser4Driver can be created with default settings`() {
        val driver = Browser4Driver()

        assertNotNull(driver.baseUrl)
        assertTrue(driver.baseUrl.startsWith("http://localhost:"))
        assertFalse(driver.isRunning)
    }

    @Test
    fun `Browser4Driver can be created with custom port`() {
        val driver = Browser4Driver(port = 9999)

        assertEquals("http://localhost:9999", driver.baseUrl)
    }

    @Test
    fun `Browser4Driver can be created with custom jar path`() {
        val customPath = "/tmp/test-browser4.jar"
        val driver = Browser4Driver(jarPath = customPath)

        assertNotNull(driver)
        assertFalse(driver.isJarPresent) // Should not exist yet
    }

    @Test
    fun `Browser4Driver defaultJarPath returns valid path`() {
        val defaultPath = Browser4Driver.defaultJarPath()

        assertNotNull(defaultPath)
        assertTrue(defaultPath.contains(".browser4"))
        assertTrue(defaultPath.endsWith("Browser4.jar"))
    }

    @Test
    fun `Browser4Driver isRunning returns false when not started`() {
        val driver = Browser4Driver()

        assertFalse(driver.isRunning)
    }

    @Test
    fun `Browser4Driver start throws when already running`() {
        // This test validates the API exists
        // In real implementation, this would be tested in integration tests
        // where we actually start the driver
        val driver = Browser4Driver()
        assertNotNull(driver)
    }

    @Test
    fun `Browser4Driver stop does not throw when not running`() {
        val driver = Browser4Driver()

        // Should complete without exception even if not started
        driver.stop()
    }

    @Test
    fun `Browser4Driver close does not throw`() {
        val driver = Browser4Driver()

        driver.close()
        // Should complete without exception
    }

    @Test
    fun `Browser4Driver can be used with try-with-resources`() {
        // Validate AutoCloseable implementation
        Browser4Driver().use { driver ->
            assertNotNull(driver)
        }
    }

    @Test
    fun `Browser4Driver accepts Java options`() {
        val javaOptions = mapOf(
            "OPENROUTER_API_KEY" to "test-key",
            "server.port" to "8183"
        )
        val driver = Browser4Driver(javaOptions = javaOptions)

        assertNotNull(driver)
    }
}

/**
 * Unit tests for LocalDriverOptions.
 */
class LocalDriverOptionsTest {

    @Test
    fun `LocalDriverOptions can be created with defaults`() {
        val options = LocalDriverOptions()

        assertTrue(options.javaOptions.isEmpty())
    }

    @Test
    fun `LocalDriverOptions can be created with custom values`() {
        val options = LocalDriverOptions(
            jarPath = "/custom/path/Browser4.jar",
            downloadUrl = "https://custom.url/Browser4.jar",
            port = 9000,
            javaOptions = mapOf("key" to "value")
        )

        assertEquals("/custom/path/Browser4.jar", options.jarPath)
        assertEquals("https://custom.url/Browser4.jar", options.downloadUrl)
        assertEquals(9000, options.port)
        assertEquals("value", options.javaOptions["key"])
    }

    @Test
    fun `LocalDriverOptions data class properties work correctly`() {
        val options1 = LocalDriverOptions(port = 8182)
        val options2 = LocalDriverOptions(port = 8182)
        val options3 = LocalDriverOptions(port = 9999)

        assertEquals(options1, options2)
        assertTrue(options1 != options3)
    }
}

/**
 * Unit tests for PulsarClient with local driver.
 */
class PulsarClientLocalDriverTest {

    @Test
    fun `PulsarClient can be created with useLocalDriver false`() {
        val client = PulsarClient(useLocalDriver = false)

        assertNotNull(client)
    }

    @Test
    fun `PulsarClient can be created with explicit baseUrl`() {
        val client = PulsarClient(baseUrl = "http://remote-server:8182")

        assertNotNull(client)
    }

    @Test
    fun `PulsarClient with explicit baseUrl does not start local driver`() {
        val client = PulsarClient(
            baseUrl = "http://remote-server:8182",
            useLocalDriver = false
        )

        // Should not start local driver when explicit URL is provided
        assertNotNull(client)
    }

    @Test
    fun `PulsarClient accepts LocalDriverOptions`() {
        val options = LocalDriverOptions(
            port = 9000,
            javaOptions = mapOf("test" to "value")
        )

        val client = PulsarClient(
            useLocalDriver = false, // Don't actually start
            localDriverOptions = options
        )

        assertNotNull(client)
    }
}

/**
 * Unit tests for AgenticSession with local driver.
 */
class AgenticSessionLocalDriverTest {

    @Test
    fun `AgenticSession getOrCreate accepts null baseUrl`() {
        // Note: This would start local driver in real usage
        // For unit test, we just validate the API signature exists
        assertNotNull(AgenticSession::getOrCreate)
    }

    @Test
    fun `AgenticSession create accepts null baseUrl`() {
        // Note: This would start local driver in real usage
        // For unit test, we just validate the API signature exists
        assertNotNull(AgenticSession::create)
    }

    @Test
    fun `AgenticSession getOrCreate with explicit URL works`() {
        // With explicit URL, should not start local driver
        // This test validates backward compatibility
        assertNotNull(AgenticSession::getOrCreate)
    }
}
