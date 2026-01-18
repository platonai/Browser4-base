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
import ai.platon.pulsar.sdk.detail.PulsarClient
import ai.platon.pulsar.sdk.integration.server.PulsarRestServerApplication
import ai.platon.pulsar.sdk.integration.server.TestServerConfiguration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import java.time.Duration
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
@Import(PulsarContextConfiguration::class, TestServerConfiguration::class)
@Tag("IntegrationTest")
@Tag("RequiresServer")
@TestPropertySource(locations = ["classpath:application-sdk-integration-test.properties"])
abstract class KotlinSdkIntegrationTestBase {

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
        client = PulsarClient(baseUrl = baseUrl, timeout = Duration.ofSeconds(60))
    }

    /**
     * Cleanup after each test
     */
    @AfterEach
    fun cleanupClient() {
        try {
            // Try to delete session if exists
            if (client.sessionId != null) {
                client.deleteSession()
            }
        } catch (e: Exception) {
            // Ignore cleanup errors
        } finally {
            client.close()
        }
    }

    /**
     * Create new session and set it to client
     */
    protected fun createSession(): String {
        val sessionId = client.createSession()
        client.sessionId = sessionId
        return sessionId
    }

    /**
     * Wait until condition is met or timeout
     */
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
}
