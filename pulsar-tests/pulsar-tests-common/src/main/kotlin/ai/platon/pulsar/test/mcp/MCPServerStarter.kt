package ai.platon.pulsar.test.mcp

import ai.platon.pulsar.common.getLogger
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.time.Duration
import java.time.Instant

/**
 * Reusable utility to wait for the MCP test server to become available.
 *
 * Features:
 *  - Tries a health endpoint first (/mcp/info) then falls back to root /mcp (optional)
 *  - Configurable timeouts & intervals
 *  - Returns true if any probe gets a 2xx/3xx response
 *
 * This logic follows the same pattern as DemoSiteStarter for consistency.
 */
class MCPServerStarter : AutoCloseable {
    private val logger = getLogger(this)

    data class Options(
        val timeout: Duration = Duration.ofSeconds(12),
        val interval: Duration = Duration.ofMillis(500),
        val healthPath: String = System.getProperty("mcp.server.healthPath", "/mcp/info"),
        val fallbackRoot: Boolean = true,
        val connectTimeoutMillis: Int = 1200,
        val readTimeoutMillis: Int = 1800,
        val verbose: Boolean = true,
    )

    /**
     * Ensure the MCP server serving the given url is started. Extracts the explicit port from the URL; if absent uses
     * system/env configured port or sensible fallbacks (18182, then 8182) instead of the protocol default (80).
     */
    fun start(url: String) {
        logger.info("Ensure MCP server is running (autoStart always enabled)")
        var ok = wait(url)

        if (!ok) {
            try {
                val u = URI(url).toURL()
                val configuredPort = System.getProperty("mcp.server.port")?.toIntOrNull()
                    ?: System.getenv("MCP_SERVER_PORT")?.toIntOrNull()
                val desiredPort = when {
                    u.port > 0 -> u.port
                    configuredPort != null -> configuredPort
                    else -> 18182 // primary fallback for MCP server
                }
                val fallbackPorts = listOfNotNull(configuredPort, 18182, 8182).distinct().filter { it != desiredPort }
                logger.info("Attempting to auto-start MCPTestApplication on port $desiredPort (fallbacks=$fallbackPorts) ...")
                MCPServerLauncher.start(port = desiredPort, enforcePort = true)
                val ready = MCPServerLauncher.awaitReady(Duration.ofSeconds(10))
                if (!ready && desiredPort != 0 && configuredPort == null && fallbackPorts.isNotEmpty()) {
                    // Try next fallback if first failed
                    for (p in fallbackPorts) {
                        logger.warn("Retry auto-start on fallback port $p ...")
                        MCPServerLauncher.start(port = p, enforcePort = true)
                        if (MCPServerLauncher.awaitReady(Duration.ofSeconds(6))) break
                    }
                }
                if (MCPServerLauncher.isRunning()) {
                    logger.info("Auto-start success: ${MCPServerLauncher.baseUrl()}")
                } else {
                    logger.warn("Auto-start attempted but server not ready within timeout")
                }
            } catch (e: Exception) {
                logger.error("Failed to auto-start MCP server: ${e.message}", e)
            }
        }

        ok = wait(url, Options(verbose = false))

        check(ok) { "Failed to start MCP server" }

        Runtime.getRuntime().addShutdownHook(Thread { this.close() })
    }

    /**
     * Wait for the MCP server referred to by a full URL (any path under host). Only host/port are probed.
     * @param pageUrl Any URL within the target host (ex: http://localhost:18182/mcp/info)
     */
    fun wait(pageUrl: String, options: Options = Options()): Boolean {
        val (healthURL, rootURL) = try {
            val u = URI.create(pageUrl).toURL()
            val effectivePort = if (u.port != -1) u.port else (System.getProperty("mcp.server.port")?.toIntOrNull()
                ?: System.getenv("MCP_SERVER_PORT")?.toIntOrNull() ?: 18182)
            val hostPort = URL(u.protocol, u.host, effectivePort, "/mcp")
            val health = URL(u.protocol, u.host, effectivePort, options.healthPath)
            health to hostPort
        } catch (e: Exception) {
            if (options.verbose) logger.error("[MCPServerStarter] Invalid URL: $pageUrl | ${e.message}")
            return false
        }

        val deadline = Instant.now().plus(options.timeout)
        while (Instant.now().isBefore(deadline)) {
            if (probe(healthURL, options) || (options.fallbackRoot && probe(rootURL, options))) {
                if (options.verbose) logger.info("[MCPServerStarter] Server is up: $healthURL")
                return true
            }
            Thread.sleep(options.interval.toMillis())
        }
        if (options.verbose) logger.warn("[MCPServerStarter] Server not reachable within ${options.timeout.toMillis()}ms: $pageUrl")
        return false
    }

    fun stop() {
        MCPServerLauncher.stop()
    }

    override fun close() {
        stop()
    }

    private fun probe(url: URL, options: Options): Boolean {
        return try {
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = options.connectTimeoutMillis
            conn.readTimeout = options.readTimeoutMillis
            conn.requestMethod = "GET"
            conn.inputStream.use { }
            val code = conn.responseCode
            val ok = code in 200..399
            if (ok && options.verbose) logger.info("[MCPServerStarter] Probe success $url -> $code")
            ok
        } catch (_: Exception) {
            false
        }
    }
}
