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
package ai.platon.pulsar.sdk.v0.detail

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import kotlinx.coroutines.runBlocking
import java.time.Duration

/**
 * Thin HTTP client over the Browser4 OpenAPI.
 *
 * Provides low-level API communication with the Browser4 server,
 * handling session management and request/response serialization.
 *
 * When [useLocalDriver] is true and no explicit [baseUrl] is provided,
 * the client will automatically download and start a local Browser4.jar
 * instance.
 *
 * Example usage with local driver:
 * ```kotlin
 * val client = PulsarClient(useLocalDriver = true)
 * val sessionId = client.createSession()
 * // Use client for API calls
 * client.deleteSession()
 * client.close()
 * ```
 *
 * Example usage with remote server:
 * ```kotlin
 * val client = PulsarClient(baseUrl = "http://remote-server:8182")
 * val sessionId = client.createSession()
 * // Use client for API calls
 * client.deleteSession()
 * client.close()
 * ```
 *
 * @param baseUrl The base URL of the Browser4 server. If null and [useLocalDriver] is true,
 *                will use local driver. Otherwise defaults to http://localhost:8182
 * @param timeout Request timeout in seconds (default: 30.0)
 * @param sessionId Optional initial session ID
 * @param defaultHeaders Optional additional default headers
 * @param useLocalDriver If true, automatically starts a local Browser4 driver when no baseUrl is provided
 * @param localDriverOptions Options for the local Browser4 driver (jar path, download URL, etc.)
 */
class PulsarClient
@JvmOverloads constructor(
    baseUrl: String? = null,
    private val timeout: Duration = Duration.ofSeconds(30),
    var sessionId: String? = null,
    private val defaultHeaders: Map<String, String> = emptyMap(),
    private val useLocalDriver: Boolean = false,
    private val localDriverOptions: LocalDriverOptions = LocalDriverOptions()
) : AutoCloseable {

    private var localDriver: Browser4Driver? = null
    private val baseUrl: String
    private val httpClient: HttpClient
    private val gson = Gson()

    init {
        this.baseUrl = when {
            baseUrl != null -> baseUrl
            useLocalDriver -> {
                val targetPort = localDriverOptions.port ?: 8182
                val targetBaseUrl = "http://localhost:$targetPort"

                // Reuse already running local service if healthy
                if (isServerHealthy(targetBaseUrl)) {
                    targetBaseUrl
                } else {
                    // Start local driver
                    val driver = Browser4Driver(
                        jarPath = localDriverOptions.jarPath ?: Browser4Driver.Companion.defaultJarPath(),
                        downloadUrl = localDriverOptions.downloadUrl ?: Browser4Driver.Companion.DEFAULT_DOWNLOAD_URL,
                        port = targetPort,
                        javaOptions = localDriverOptions.javaOptions
                    )
                    driver.start(waitForReady = true)
                    localDriver = driver
                    driver.baseUrl
                }
            }
            else -> "http://localhost:8182"
        }

        this.httpClient = HttpClient(CIO) {
            install(ContentNegotiation) {
                gson()
            }
            engine {
                requestTimeout = timeout.toMillis()
            }
        }
    }

    /**
     * Exposes the configured base URL for building absolute request URIs.
     */
    internal val resolvedBaseUrl: String get() = baseUrl

    /**
     * Exposes the configured request timeout.
     */
    internal val resolvedTimeout: Duration get() = timeout

    /**
     * Exposes the underlying HttpClient for streaming operations (e.g., SSE).
     */
    internal val rawHttpClient: HttpClient get() = httpClient

    /**
     * Exposes default headers (excluding per-request overrides).
     */
    internal val resolvedDefaultHeaders: Map<String, String> get() = defaultHeaders

    private fun isServerHealthy(base: String): Boolean {
        val tempClient = HttpClient(CIO) {
            engine {
                requestTimeout = 2000
            }
        }
        val endpoints = listOf("$base/health", "$base/actuator/health")
        return try {
            runBlocking {
                endpoints.any { endpoint ->
                    runCatching {
                        val response = tempClient.get(endpoint)
                        response.status.value in 200..299
                    }.getOrElse { false }
                }
            }
        } finally {
            runBlocking {
                tempClient.close()
            }
        }
    }

    private fun url(path: String): String {
        val normalizedBase = baseUrl.trimEnd('/')
        return "$normalizedBase$path"
    }

    private fun requireSession(sessionId: String? = null): String {
        val sid = sessionId ?: this.sessionId
        return sid ?: throw IllegalStateException("session_id is required; call createSession() first or pass sessionId explicitly")
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun request(
        method: String,
        path: String,
        sessionId: String? = null,
        body: Map<String, Any?>? = null
    ): Any? {
        var resolvedPath = path
        val sid = if ("{sessionId}" in path) {
            requireSession(sessionId)
        } else {
            sessionId ?: this.sessionId
        }

        if (sid != null) {
            resolvedPath = resolvedPath.replace("{sessionId}", sid)
        }

        val response = httpClient.request(url(resolvedPath)) {
            this.method = when (method.uppercase()) {
                "GET" -> HttpMethod.Get
                "DELETE" -> HttpMethod.Delete
                "POST" -> HttpMethod.Post
                "PUT" -> HttpMethod.Put
                else -> throw IllegalArgumentException("Unsupported HTTP method: $method")
            }

            contentType(ContentType.Application.Json)
            defaultHeaders.forEach { (key, value) ->
                header(key, value)
            }

            if (body != null && (method.uppercase() == "POST" || method.uppercase() == "PUT")) {
                setBody(body)
            }
        }

        if (response.status.value >= 400) {
            val errorBody = response.bodyAsText()
            throw RuntimeException("HTTP ${response.status.value}: $errorBody")
        }

        val responseBody = response.bodyAsText()
        if (responseBody.isBlank()) {
            return null
        }

        return try {
            val type = object : TypeToken<Map<String, Any?>>() {}.type
            val payload: Map<String, Any?> = gson.fromJson(responseBody, type)
            // WebDriver responses typically wrap in { value: ... }
            if (payload.containsKey("value")) payload["value"] else payload
        } catch (e: Exception) {
            responseBody
        }
    }

    /**
     * Creates a new browser session.
     *
     * @param capabilities Optional desired capabilities for the session
     * @return The created session ID
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun createSession(capabilities: Map<String, Any?>? = null): String {
        val response = request("POST", "/session", body = mapOf("capabilities" to (capabilities ?: emptyMap<String, Any?>())))
        val value = response as? Map<String, Any?>
        val newSessionId = value?.get("sessionId") as? String
            ?: throw RuntimeException("createSession response missing sessionId")
        this.sessionId = newSessionId
        return newSessionId
    }

    /**
     * Deletes the current or specified session.
     *
     * @param sessionId Optional session ID to delete (defaults to current session)
     */
    suspend fun deleteSession(sessionId: String? = null) {
        val sid = requireSession(sessionId)
        request("DELETE", "/session/$sid")
        if (sessionId == null || sessionId == this.sessionId) {
            this.sessionId = null
        }
    }

    /**
     * Performs a POST request to the API.
     *
     * @param path API endpoint path (may contain {sessionId} placeholder)
     * @param body Request body as a map
     * @param sessionId Optional session ID
     * @return Response value
     */
    suspend fun post(path: String, body: Map<String, Any?>, sessionId: String? = null): Any? {
        return request("POST", path, sessionId = sessionId, body = body)
    }

    /**
     * Performs a GET request to the API.
     *
     * @param path API endpoint path (may contain {sessionId} placeholder)
     * @param sessionId Optional session ID
     * @return Response value
     */
    suspend fun get(path: String, sessionId: String? = null): Any? {
        return request("GET", path, sessionId = sessionId)
    }

    /**
     * Performs a DELETE request to the API.
     *
     * @param path API endpoint path (may contain {sessionId} placeholder)
     * @param sessionId Optional session ID
     * @return Response value
     */
    suspend fun delete(path: String, sessionId: String? = null): Any? {
        return request("DELETE", path, sessionId = sessionId)
    }

    /**
     * Closes the HTTP client and local driver if started.
     */
    override fun close() {
        httpClient.close()
        localDriver?.close()
    }
}

/**
 * Configuration options for the local Browser4 driver.
 *
 * @param jarPath Path where Browser4.jar should be stored (default: ~/.browser4/Browser4.jar)
 * @param downloadUrl URL to download Browser4.jar from (default: GitHub release v4.4.0)
 * @param port Port for the Browser4 server (default: 8182)
 * @param javaOptions Additional Java system properties (e.g., "OPENROUTER_API_KEY" to "your-key")
 */
data class LocalDriverOptions(
    val jarPath: String? = null,
    val downloadUrl: String? = null,
    val port: Int? = null,
    val javaOptions: Map<String, String> = emptyMap()
)
