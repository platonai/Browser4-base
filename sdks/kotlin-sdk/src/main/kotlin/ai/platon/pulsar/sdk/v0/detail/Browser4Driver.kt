package ai.platon.pulsar.sdk.v0.detail

import java.io.BufferedReader
import java.io.File
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.URI
import java.net.URL
import java.util.concurrent.TimeUnit

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
        const val DEFAULT_DOWNLOAD_URL =
            "https://github.com/platonai/Browser4/releases/download/v4.4.0/Browser4.jar"
        private const val LATEST_RELEASE_API =
            "https://api.github.com/repos/platonai/Browser4/releases/latest"
        private val DOWNLOAD_URL_REGEX =
            """"browser_download_url"\s*:\s*"([^"]*Browser4\.jar)"""".toRegex()
        private const val DEFAULT_PORT = 8182
        private const val STARTUP_TIMEOUT_SECONDS = 120L
        private const val HEALTH_CHECK_INTERVAL_MS = 500L

        /**
         * Returns the default jar path in the user's home directory.
         */
        fun defaultJarPath(): String {
            val userHome = System.getProperty("user.home")
            val browser4Dir = File(userHome, ".browser4").resolve("lib")
            browser4Dir.mkdirs()
            return File(browser4Dir, "Browser4.jar").absolutePath
        }
    }

    private var process: Process? = null
    @Volatile
    private var shutdownHook: Thread? = null

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
        val resolvedDownloadUrl = resolveDownloadUrl()
        println("Downloading from $resolvedDownloadUrl...")

        val jarFile = File(jarPath)
        jarFile.parentFile?.mkdirs()

        val attempts = 3
        val backoffMs = 3000L
        var lastError: Exception? = null

        repeat(attempts) { idx ->
            try {
                val url = URL(resolvedDownloadUrl)
                val connection = openHttpConnection(url)
                connection.instanceFollowRedirects = true
                connection.connectTimeout = 60000
                connection.readTimeout = 120000

                connection.inputStream.use { input ->
                    jarFile.outputStream().use { output ->
                        val total = connection.contentLengthLong
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var downloaded = 0L
                        var lastPercent = -1

                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            downloaded += read

                            if (total > 0) {
                                val percent = ((downloaded * 100) / total).toInt()
                                if (percent != lastPercent && percent % 5 == 0) {
                                    println("Downloading Browser4.jar... $percent%")
                                    lastPercent = percent
                                }
                            } else if (downloaded % (1024 * 1024) == 0L) {
                                println("Downloading Browser4.jar... ${downloaded / (1024 * 1024)} MB")
                            }
                        }

                        if (total > 0 && lastPercent != 100) {
                            println("Downloading Browser4.jar... 100%")
                        }
                    }
                }

                println("Browser4.jar downloaded successfully to $jarPath")
                return
            } catch (e: Exception) {
                lastError = e
                val attempt = idx + 1
                val retrying = attempt < attempts
                val msg = "Failed to download Browser4.jar (attempt $attempt/$attempts): ${e.message}"
                if (retrying) {
                    println("$msg; retrying in ${backoffMs / 1000}s...")
                    Thread.sleep(backoffMs)
                } else {
                    throw RuntimeException("$msg. Please check network/proxy or override downloadUrl.", e)
                }
            }
        }
    }

    /**
     * Resolves the download URL to the latest release asset when using the default URL.
     */
    private fun resolveDownloadUrl(): String {
        if (downloadUrl != DEFAULT_DOWNLOAD_URL) {
            return downloadUrl
        }
        val latestUrl = fetchLatestReleaseDownloadUrl()
        if (!latestUrl.isNullOrBlank()) {
            println("Resolved latest Browser4.jar download URL: $latestUrl")
            return latestUrl
        }
        println("Falling back to default Browser4.jar URL: $downloadUrl")
        return downloadUrl
    }

    private fun fetchLatestReleaseDownloadUrl(): String? {
        return runCatching {
            val url = URL(LATEST_RELEASE_API)
            val connection = openHttpConnection(url)
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "Browser4Driver")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.inputStream.bufferedReader().use(BufferedReader::readText)
        }.mapCatching { json ->
            parseLatestBrowserDownloadUrl(json)
        }.onFailure {
            println("Failed to resolve latest Browser4.jar URL: ${it.message}")
        }.getOrNull()
    }

    internal fun parseLatestBrowserDownloadUrl(json: String): String? {
        return DOWNLOAD_URL_REGEX.find(json)?.groupValues?.getOrNull(1)
    }

    /**
     * Open an HTTP connection honoring the system proxy settings.
     */
    private fun openHttpConnection(url: URL): HttpURLConnection {
        val proxies = runCatching { ProxySelector.getDefault()?.select(url.toURI()) }.getOrNull().orEmpty()
        val proxy = proxies.firstOrNull { it != Proxy.NO_PROXY && it.address() is InetSocketAddress }
        return if (proxy != null) {
            url.openConnection(proxy) as HttpURLConnection
        } else {
            url.openConnection() as HttpURLConnection
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

        // Ensure REST API profiles are active so /session endpoints are available by default
        val profilePropertyName = "spring.profiles.active"
        val hasProfileOption = javaOptions.keys.any { it.equals(profilePropertyName, ignoreCase = true) }
        val envProfile = System.getProperty(profilePropertyName) ?: System.getenv("SPRING_PROFILES_ACTIVE")
        if (!hasProfileOption && envProfile.isNullOrBlank()) {
            commands.add("-D$profilePropertyName=rest,private,advanced")
        }

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
        installShutdownHook()

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
        val candidates = listOf(
            "$baseUrl/health",
            "$baseUrl/actuator/health"
        )
        return candidates.any { endpoint ->
            try {
                val url = URI.create(endpoint).toURL()
                val connection = openHttpConnection(url)
                connection.requestMethod = "GET"
                connection.connectTimeout = 1000
                connection.readTimeout = 1000
                val code = connection.responseCode
                // Some Spring actuator endpoints return JSON with status: "UP"
                val ok = code in 200..299
                connection.disconnect()
                ok
            } catch (_: Exception) {
                false
            }
        }
    }

    /**
     * Stops the Browser4 server process.
     *
     * @param force If true, forcibly kills the process; otherwise attempts graceful shutdown
     */
    fun stop(force: Boolean = false) {
        removeShutdownHook()
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

    private fun installShutdownHook() {
        if (shutdownHook != null) return
        val hook = Thread {
            try {
                stop(force = true)
            } catch (_: Exception) {
                // best-effort during JVM shutdown
            }
        }
        try {
            Runtime.getRuntime().addShutdownHook(hook)
            shutdownHook = hook
        } catch (_: IllegalStateException) {
            // JVM is already shutting down; ignore
        }
    }

    private fun removeShutdownHook() {
        val hook = shutdownHook ?: return
        shutdownHook = null
        runCatching { Runtime.getRuntime().removeShutdownHook(hook) }
    }

    /**
     * Closes the driver and stops the server process.
     */
    override fun close() {
        stop()
    }
}
