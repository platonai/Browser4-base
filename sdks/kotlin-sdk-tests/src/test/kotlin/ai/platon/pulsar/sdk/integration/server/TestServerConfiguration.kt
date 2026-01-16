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
package ai.platon.pulsar.sdk.integration.server

import ai.platon.pulsar.test.server.MockSiteApplication
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.SpringApplication
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.ConfigurableApplicationContext
import java.net.BindException
import java.net.ServerSocket

/**
 * Test configuration that automatically starts and stops the mock site server.
 * The mock server runs on port 18080 and provides test pages.
 */
@TestConfiguration
class TestServerConfiguration : InitializingBean, DisposableBean {

    private val log = LoggerFactory.getLogger(javaClass)
    private var mockServerContext: ConfigurableApplicationContext? = null
    private var isServerStarted = false

    companion object {
        const val MOCK_SERVER_PORT = 18080
        const val MOCK_SERVER_STARTUP_TIMEOUT_MS = 60000L // 60 seconds
    }

    private var serverThread: Thread? = null

    override fun afterPropertiesSet() {
        startMockServer()
    }

    override fun destroy() {
        stopMockServer()
    }

    private fun startMockServer() {
        if (isPortInUse(MOCK_SERVER_PORT)) {
            log.info("Port $MOCK_SERVER_PORT is already in use, assuming mock server is running")
            isServerStarted = true
            return
        }

        try {
            log.info("Starting embedded mock server on port $MOCK_SERVER_PORT...")

            val app = SpringApplication(MockSiteApplication::class.java)

            val properties = mapOf(
                "server.port" to MOCK_SERVER_PORT.toString(),
                "spring.main.banner-mode" to "off",
                "logging.level.root" to "WARN",
                "logging.level.ai.platon.pulsar.test.server" to "INFO",
                "spring.main.allow-bean-definition-overriding" to "true",
                "spring.main.web-application-type" to "servlet"
            )

            app.setDefaultProperties(properties)
            app.setAddCommandLineProperties(true)
            System.setProperty("server.port", MOCK_SERVER_PORT.toString())

            // Start in separate thread to avoid blocking
            serverThread = Thread {
                try {
                    log.info("Starting MockSiteApplication with properties: $properties")
                    mockServerContext = app.run()
                    log.info("Mock server application context created successfully")

                    val environment = mockServerContext?.environment
                    val actualPort = environment?.getProperty("server.port") ?: "unknown"
                    log.info("Mock server is running on port: $actualPort")
                } catch (e: Exception) {
                    log.error("Error starting mock server application", e)
                }
            }
            serverThread?.name = "mock-server"
            serverThread?.isDaemon = true
            serverThread?.start()

            // Wait for server to start
            val checkInterval = 1000L
            var attempts = 0
            val maxAttempts = 60

            while (attempts < maxAttempts) {
                if (isPortInUse(MOCK_SERVER_PORT)) {
                    log.info("Mock server started successfully on port $MOCK_SERVER_PORT")
                    isServerStarted = true
                    return
                }

                Thread.sleep(checkInterval)
                attempts++

                if (attempts % 10 == 0) {
                    log.info("Still waiting for mock server to start... (attempt $attempts/$maxAttempts)")
                }
            }

            log.error("Mock server failed to start within timeout")
            stopMockServer()
        } catch (e: Exception) {
            log.error("Failed to start mock server", e)
            stopMockServer()
        }
    }

    private fun stopMockServer() {
        mockServerContext?.let { context ->
            try {
                log.info("Stopping embedded mock server...")
                context.close()
                log.info("Embedded mock server stopped")
            } catch (e: Exception) {
                log.error("Error stopping embedded mock server", e)
            }
        }

        serverThread?.let { thread ->
            try {
                if (thread.isAlive) {
                    log.info("Interrupting mock server thread...")
                    thread.interrupt()
                }
            } catch (e: Exception) {
                log.error("Error interrupting mock server thread", e)
            }
        }

        mockServerContext = null
        serverThread = null
        isServerStarted = false
    }

    private fun isPortInUse(port: Int): Boolean {
        return try {
            ServerSocket(port).use { false }
        } catch (e: BindException) {
            true
        }
    }
}
