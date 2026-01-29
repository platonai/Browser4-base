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

import ai.platon.pulsar.boot.autoconfigure.PulsarContextConfiguration
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.sdk.integration.server.MockServerConfiguration
import ai.platon.pulsar.sdk.integration.server.PulsarRestServerApplication
import ai.platon.pulsar.sdk.v0.detail.PulsarClient
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import kotlin.test.assertTrue

/**
 * Integration test base class.
 *
 * Features:
 * - Automatically starts complete Browser4 REST server
 * - Uses random port to avoid conflicts
 * - Auto-configures and cleans up SDK client
 * - Provides test utility methods
 */
@SpringBootTest(
    classes = [PulsarRestServerApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Import(PulsarContextConfiguration::class, MockServerConfiguration::class)
@Tag("IntegrationTest")
@Tag("RequiresServer")
@TestPropertySource(locations = ["classpath:application-sdk-integration-test.properties"])
abstract class KotlinSdkIntegrationTestBase {

    private val logger = getLogger(this)

    /**
     * Spring Boot injected server port
     */
    @LocalServerPort
    protected var serverPort: Int = 0

    /**
     * SDK client instance
     */
    protected lateinit var client: PulsarClient

    /**
     * Server base URL
     */
    protected val baseUrl: String
        get() = "http://localhost:$serverPort"

    /**
     * Setup before each test
     */
    @BeforeEach
    fun setupClient() {
        assertTrue(serverPort > 0, "Server port should be assigned")

        // Wait for server to be ready before issuing session/webdriver operations.
        // On cold starts the REST layer may accept connections while browser/CDP is still warming up.
        waitForServerReadiness()

        client = PulsarClient(baseUrl = baseUrl)
    }

    /**
     * Cleanup after each test
     */
    @AfterEach
    fun cleanupClient() = runBlocking {
        try {
            // Try to delete session if exists
            if (client.sessionId != null) {
                client.deleteSession()
            }
        } catch (_: Exception) {
            // Ignore cleanup errors
        } finally {
            client.close()
        }
    }

    /**
     * Create new session and set it to client
     */
    protected suspend fun createSession(): String {
        val sessionId = client.createSession()
        client.sessionId = sessionId
        return sessionId
    }

    /**
     * Wait until condition is met or timeout
     */
    @Suppress("SameParameterValue")
    protected fun waitUntil(
        timeoutSeconds: Int = 10,
        intervalMillis: Long = 500,
        condition: () -> Boolean
    ): Boolean {
        val endTime = System.currentTimeMillis() + timeoutSeconds * 1000
        while (System.currentTimeMillis() < endTime) {
            if (condition()) {
                return true
            }
            Thread.sleep(intervalMillis)
        }
        return false
    }

    /**
     * Waits until the REST server reports READY.
     */
    protected fun waitForServerReadiness(timeoutSeconds: Int = 30) {
        val ready = waitUntil(timeoutSeconds = timeoutSeconds, intervalMillis = 500) {
            isEndpointHealthy("/health") && isEndpointHealthy("/health/ready")
        }
        assertTrue(ready, "Server did not become ready in ${timeoutSeconds}s | baseUrl=$baseUrl")
        logger.info("Server is ready | baseUrl=$baseUrl")
    }

    private fun isEndpointHealthy(path: String): Boolean {
        val url = java.net.URL(baseUrl + path)
        return try {
            val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 1000
                readTimeout = 1000
            }
            conn.inputStream.use { /* drain */ }
            conn.responseCode in 200..299
        } catch (_: Exception) {
            false
        }
    }
}
