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

import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit

/**
 * Browser4Driver manages the lifecycle of a local Browser4.jar process.
 *
 * This class handles:
 * - Downloading Browser4.jar from GitHub releases if not present
 * - Starting and stopping the Browser4 server process
 * - Health checking the server
 *
 * Example usage:
 * ```kotlin
 * val driver = Browser4Driver()
 * driver.start()
 * // Use driver.baseUrl with PulsarClient
 * val client = PulsarClient(baseUrl = driver.baseUrl)
 * // ... use client ...
 * driver.stop()
 * ```
 *
 * @param jarPath Path where Browser4.jar should be stored (default: ~/.browser4/Browser4.jar)
 * @param downloadUrl URL to download Browser4.jar from (default: GitHub release v4.4.0)
 * @param port Port for the Browser4 server (default: 8182)
 * @param javaOptions Additional Java options for the process
 */
class Browser4Driver(
    private val jarPath: String = defaultJarPath(),
    private val downloadUrl: String = DEFAULT_DOWNLOAD_URL,
    private val port: Int = DEFAULT_PORT,
    private val javaOptions: Map<String, String> = emptyMap()
) : AutoCloseable {

    companion object {
        private const val DEFAULT_DOWNLOAD_URL = 
            "https://github.com/platonai/Browser4/releases/download/v4.4.0/Browser4.jar"
        private const val DEFAULT_PORT = 8182
        private const val STARTUP_TIMEOUT_SECONDS = 120L
        private const val HEALTH_CHECK_INTERVAL_MS = 500L

        /**
         * Returns the default jar path in the user's home directory.
         */
        fun defaultJarPath(): String {
            val userHome = System.getProperty("user.home")
            val browser4Dir = File(userHome, ".browser4")
            browser4Dir.mkdirs()
            return File(browser4Dir, "Browser4.jar").absolutePath
        }
    }

    private var process: Process? = null

    /**
     * The base URL where the Browser4 server is accessible.
     */
    val baseUrl: String
        get() = "http://localhost:$port"

    /**
     * Checks if the Browser4.jar file exists.
     */
    val isJarPresent: Boolean
        get() = File(jarPath).exists()

    /**
     * Checks if the Browser4 server process is running.
     */
    val isRunning: Boolean
        get() = process?.isAlive == true

    /**
     * Downloads Browser4.jar from the configured URL if it doesn't exist.
     *
     * @throws RuntimeException if download fails
     */
    fun downloadIfNeeded() {
        if (isJarPresent) {
            return
        }

        println("Browser4.jar not found at $jarPath")
        println("Downloading from $downloadUrl...")

        val jarFile = File(jarPath)
        jarFile.parentFile?.mkdirs()

        try {
            val url = URL(downloadUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 30000
            connection.readTimeout = 60000

            connection.inputStream.use { input ->
                Files.copy(input, jarFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }

            println("Browser4.jar downloaded successfully to $jarPath")
        } catch (e: Exception) {
            throw RuntimeException("Failed to download Browser4.jar: ${e.message}", e)
        }
    }

    /**
     * Starts the Browser4 server process.
     *
     * This method:
     * 1. Downloads Browser4.jar if needed
     * 2. Starts the Java process
     * 3. Waits for the server to be ready
     *
     * @param waitForReady If true, waits for server to be ready before returning
     * @throws IllegalStateException if already running
     * @throws RuntimeException if startup fails
     */
    fun start(waitForReady: Boolean = true) {
        if (isRunning) {
            throw IllegalStateException("Browser4 server is already running")
        }

        downloadIfNeeded()

        println("Starting Browser4 server on port $port...")

        val commands = mutableListOf("java")

        // Add Java system properties
        javaOptions.forEach { (key, value) ->
            commands.add("-D$key=$value")
        }

        // Add environment variable properties
        System.getenv("OPENROUTER_API_KEY")?.let { apiKey ->
            if (!javaOptions.containsKey("OPENROUTER_API_KEY")) {
                commands.add("-DOPENROUTER_API_KEY=$apiKey")
            }
        }

        // Configure server port if not default
        if (port != DEFAULT_PORT) {
            commands.add("-Dserver.port=$port")
        }

        commands.add("-jar")
        commands.add(jarPath)

        val processBuilder = ProcessBuilder(commands)
        processBuilder.redirectErrorStream(true)
        processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT)

        process = processBuilder.start()

        if (waitForReady) {
            waitForServerReady()
        }

        println("Browser4 server started successfully")
    }

    /**
     * Waits for the Browser4 server to be ready by checking the health endpoint.
     *
     * @param timeoutSeconds Maximum time to wait in seconds
     * @throws RuntimeException if server doesn't become ready within timeout
     */
    fun waitForServerReady(timeoutSeconds: Long = STARTUP_TIMEOUT_SECONDS) {
        val startTime = System.currentTimeMillis()
        val timeoutMs = timeoutSeconds * 1000

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (!isRunning) {
                throw RuntimeException("Browser4 process terminated unexpectedly")
            }

            if (isServerHealthy()) {
                return
            }

            Thread.sleep(HEALTH_CHECK_INTERVAL_MS)
        }

        throw RuntimeException("Browser4 server failed to start within $timeoutSeconds seconds")
    }

    /**
     * Checks if the Browser4 server is healthy by attempting to connect to it.
     *
     * @return true if server responds to health check, false otherwise
     */
    fun isServerHealthy(): Boolean {
        return try {
            val url = URI.create("$baseUrl/status").toURL()
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 1000
            connection.readTimeout = 1000
            val responseCode = connection.responseCode
            connection.disconnect()
            responseCode in 200..299
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Stops the Browser4 server process.
     *
     * @param force If true, forcibly kills the process; otherwise attempts graceful shutdown
     */
    fun stop(force: Boolean = false) {
        process?.let { proc ->
            if (proc.isAlive) {
                println("Stopping Browser4 server...")
                
                if (force) {
                    proc.destroyForcibly()
                } else {
                    proc.destroy()
                    // Wait up to 10 seconds for graceful shutdown
                    if (!proc.waitFor(10, TimeUnit.SECONDS)) {
                        println("Browser4 server did not stop gracefully, forcing...")
                        proc.destroyForcibly()
                    }
                }
                
                println("Browser4 server stopped")
            }
        }
        process = null
    }

    /**
     * Closes the driver and stops the server process.
     */
    override fun close() {
        stop()
    }
}
