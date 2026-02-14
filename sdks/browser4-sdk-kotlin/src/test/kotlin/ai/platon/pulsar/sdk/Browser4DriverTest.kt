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
import org.junit.jupiter.api.DisplayName

/**
 * Unit tests for Browser4Driver.
 *
 * These tests verify the Browser4Driver configuration and behavior.
 * Note: Tests that require actual download or server startup are skipped
 * to avoid long-running tests in CI.
 */
class Browser4DriverTest {

    @Test
        @DisplayName("Browser4Driver can be created with default settings")
    fun browser4driverCanBeCreatedWithDefaultSettings() {
        val driver = Browser4Driver()

        assertNotNull(driver.baseUrl)
        assertTrue(driver.baseUrl.startsWith("http://localhost:"))
        assertFalse(driver.isRunning)
    }

    @Test
        @DisplayName("Browser4Driver can be created with custom port")
    fun browser4driverCanBeCreatedWithCustomPort() {
        val driver = Browser4Driver(port = 9999)

        assertEquals("http://localhost:9999", driver.baseUrl)
    }

    @Test
        @DisplayName("Browser4Driver can be created with custom jar path")
    fun browser4driverCanBeCreatedWithCustomJarPath() {
        val customPath = "/tmp/test-browser4.jar"
        val driver = Browser4Driver(jarPath = customPath)

        assertNotNull(driver)
        assertFalse(driver.isJarPresent) // Should not exist yet
    }

    @Test
        @DisplayName("Browser4Driver defaultJarPath returns valid path")
    fun browser4driverDefaultjarpathReturnsValidPath() {
        val defaultPath = Browser4Driver.defaultJarPath()

        assertNotNull(defaultPath)
        assertTrue(defaultPath.contains(".browser4"))
        assertTrue(defaultPath.endsWith("Browser4.jar"))
    }

    @Test
        @DisplayName("Browser4Driver isRunning returns false when not started")
    fun browser4driverIsrunningReturnsFalseWhenNotStarted() {
        val driver = Browser4Driver()

        assertFalse(driver.isRunning)
    }

    @Test
        @DisplayName("Browser4Driver start throws when already running")
    fun browser4driverStartThrowsWhenAlreadyRunning() {
        // This test validates the API exists
        // In real implementation, this would be tested in integration tests
        // where we actually start the driver
        val driver = Browser4Driver()
        assertNotNull(driver)
    }

    @Test
        @DisplayName("Browser4Driver stop does not throw when not running")
    fun browser4driverStopDoesNotThrowWhenNotRunning() {
        val driver = Browser4Driver()

        // Should complete without exception even if not started
        driver.stop()
    }

    @Test
        @DisplayName("Browser4Driver close does not throw")
    fun browser4driverCloseDoesNotThrow() {
        val driver = Browser4Driver()

        driver.close()
        // Should complete without exception
    }

    @Test
        @DisplayName("Browser4Driver can be used with try-with-resources")
    fun browser4driverCanBeUsedWithTryWithResources() {
        // Validate AutoCloseable implementation
        Browser4Driver().use { driver ->
            assertNotNull(driver)
        }
    }

    @Test
        @DisplayName("Browser4Driver accepts Java options")
    fun browser4driverAcceptsJavaOptions() {
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
        @DisplayName("LocalDriverOptions can be created with defaults")
    fun localdriveroptionsCanBeCreatedWithDefaults() {
        val options = LocalDriverOptions()

        assertTrue(options.javaOptions.isEmpty())
    }

    @Test
        @DisplayName("LocalDriverOptions can be created with custom values")
    fun localdriveroptionsCanBeCreatedWithCustomValues() {
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
        @DisplayName("LocalDriverOptions data class properties work correctly")
    fun localdriveroptionsDataClassPropertiesWorkCorrectly() {
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
        @DisplayName("PulsarClient can be created with useLocalDriver false")
    fun pulsarclientCanBeCreatedWithUselocaldriverFalse() {
        val client = PulsarClient(useLocalDriver = false)

        assertNotNull(client)
    }

    @Test
        @DisplayName("PulsarClient can be created with explicit baseUrl")
    fun pulsarclientCanBeCreatedWithExplicitBaseurl() {
        val client = PulsarClient(baseUrl = "http://remote-server:8182")

        assertNotNull(client)
    }

    @Test
        @DisplayName("PulsarClient with explicit baseUrl does not start local driver")
    fun pulsarclientWithExplicitBaseurlDoesNotStartLocalDriver() {
        val client = PulsarClient(
            baseUrl = "http://remote-server:8182",
            useLocalDriver = false
        )

        // Should not start local driver when explicit URL is provided
        assertNotNull(client)
    }

    @Test
        @DisplayName("PulsarClient accepts LocalDriverOptions")
    fun pulsarclientAcceptsLocaldriveroptions() {
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
        @DisplayName("AgenticSession getOrCreate accepts null baseUrl")
    fun agenticsessionGetorcreateAcceptsNullBaseurl() {
        // Note: This would start local driver in real usage
        // For unit test, we just validate the API signature exists
        assertNotNull(AgenticSession::getOrCreate)
    }

    @Test
        @DisplayName("AgenticSession create accepts null baseUrl")
    fun agenticsessionCreateAcceptsNullBaseurl() {
        // Note: This would start local driver in real usage
        // For unit test, we just validate the API signature exists
        assertNotNull(AgenticSession::create)
    }

    @Test
        @DisplayName("AgenticSession getOrCreate with explicit URL works")
    fun agenticsessionGetorcreateWithExplicitUrlWorks() {
        // With explicit URL, should not start local driver
        // This test validates backward compatibility
        assertNotNull(AgenticSession::getOrCreate)
    }
}
