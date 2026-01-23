package ai.platon.pulsar.test.mcp

import ai.platon.pulsar.common.getLogger
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ConfigurableApplicationContext
import java.io.Closeable
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Utility launcher to programmatically start / stop the TestMCPServer via Spring Boot.
 *
 * Similar to MockSiteLauncher, this provides:
 *  - Programmatic start/stop of the MCP test server
 *  - Port configuration with enforcement
 *  - Health check and readiness probing
 */
object MCPServerLauncher : Closeable {
    private val logger = getLogger(this)

    @Volatile
    private var context: ConfigurableApplicationContext? = null

    @Volatile
    private var boundPort: Int? = null

    private val starting = AtomicBoolean(false)

    /** Whether the MCP server is currently running. */
    fun isRunning(): Boolean = context?.isActive == true

    /** The port the server actually bound to (null if not started). */
    fun port(): Int? = boundPort

    /** Base URL (null if not started). */
    fun baseUrl(): String? = boundPort?.let { "http://localhost:$it/mcp" }

    /**
     * Start the MCP server if not already started or restart if running on a different port and [enforcePort] is true.
     *
     * @param port Desired port (0 for random). Will override application*.properties via command-line arg.
     * @param enforcePort If true and already running on a different port, the server is restarted on the requested port.
     */
    @Synchronized
    fun start(
        port: Int = 18182,
        properties: Map<String, Any> = emptyMap(),
        profiles: Array<String> = emptyArray(),
        headless: Boolean = true,
        enforcePort: Boolean = true,
    ): ConfigurableApplicationContext {
        if (isRunning()) {
            val current = boundPort
            if (enforcePort && current != null && current != port) {
                logger.warn("MCPTestApplication already running on port $current, requested $port -> restarting to enforce port")
                stop()
            } else {
                logger.info("MCPTestApplication already running on port $current, reuse context")
                return context!!
            }
        }
        if (!starting.compareAndSet(false, true)) {
            // Another thread is starting; wait until available with timeout
            val deadline = Instant.now().plusSeconds(30)
            while (!isRunning() && Instant.now().isBefore(deadline)) {
                Thread.sleep(100)
            }
            if (!isRunning()) {
                throw IllegalStateException("Timeout waiting for MCPTestApplication to start")
            }
            return context!!
        }
        try {
            val defaultProps = (properties + mapOf(
                "logging.level.root" to (properties["logging.level.root"] ?: "INFO"),
                // Disable DevTools restart for programmatic launch
                "spring.devtools.restart.enabled" to "false",
                "spring.devtools.add-properties" to "false",
            )).toMutableMap()

            // Also set JVM system properties
            System.setProperty("spring.devtools.restart.enabled", "false")
            System.setProperty("spring.devtools.add-properties", "false")

            val builder = SpringApplicationBuilder(MCPTestApplication::class.java)
                .properties(defaultProps)
                .profiles(*profiles)
                .headless(headless)

            val args = mutableListOf<String>()
            // Always force desired port via command line arg for highest precedence
            args += "--server.port=$port"
            if (headless) args += "--java.awt.headless=true"

            logger.info("Starting MCPTestApplication | requestedPort=$port enforcePort=$enforcePort args=$args")
            val ctx = builder.run(*args.toTypedArray())
            context = ctx

            val resolvedPort = ctx.environment.getProperty("local.server.port")?.toInt()
                ?: ctx.environment.getProperty("server.port")?.toInt()
                ?: port
            boundPort = resolvedPort
            if (port != 0 && resolvedPort != port) {
                logger.warn("Requested port $port but server bound to $resolvedPort")
            } else {
                logger.info("MCPTestApplication started on port $resolvedPort")
            }
            return ctx
        } catch (e: Exception) {
            starting.set(false)
            throw IllegalStateException("Failed to start MCPTestApplication: ${e.message}", e)
        } finally {
            starting.set(false)
        }
    }

    /** Restart the server: stops if running, then starts again. */
    @Synchronized
    fun restart(port: Int = boundPort ?: 18182, properties: Map<String, Any> = emptyMap()): ConfigurableApplicationContext {
        stop()
        return start(port = port, properties = properties)
    }

    /**
     * Wait until the MCP info endpoint responds with a 2xx/3xx status or timeout reached.
     * @return true if ready, false if timed out.
     */
    fun awaitReady(
        timeout: Duration = Duration.ofSeconds(10),
        interval: Duration = Duration.ofMillis(300),
        healthPath: String = "/mcp/info"
    ): Boolean {
        val port = boundPort ?: return false
        val healthUrl = URI.create("http://localhost:$port$healthPath").toURL()
        val deadline = Instant.now().plus(timeout)
        while (Instant.now().isBefore(deadline)) {
            if (probe(healthUrl)) return true
            Thread.sleep(interval.toMillis())
        }
        return false
    }

    private fun probe(url: URL): Boolean {
        return try {
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 1000
                readTimeout = 1500
                requestMethod = "GET"
            }
            conn.inputStream.use { }
            val code = conn.responseCode
            code in 200..399
        } catch (_: Exception) {
            false
        }
    }

    /** Stop the server if running. */
    @Synchronized
    fun stop() {
        try {
            context?.close()
        } finally {
            context = null
            boundPort = null
        }
    }

    override fun close() = stop()
}
