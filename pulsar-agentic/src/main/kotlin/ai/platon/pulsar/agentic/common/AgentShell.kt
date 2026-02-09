package ai.platon.pulsar.agentic.common

import ai.platon.pulsar.common.getLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * Shell command execution result containing exit code, stdout, stderr, and metadata.
 */
data class ShellResult(
    val sessionId: String,
    val command: String,
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val durationMs: Long,
    val timedOut: Boolean = false,
) {
    val success: Boolean get() = exitCode == 0 && !timedOut

    override fun toString(): String {
        val status = if (success) "SUCCESS" else "FAILED(exitCode=$exitCode, timedOut=$timedOut)"
        return buildString {
            appendLine("[$status] command='$command' (${durationMs}ms)")
            if (stdout.isNotBlank()) appendLine("stdout:\n$stdout")
            if (stderr.isNotBlank()) appendLine("stderr:\n$stderr")
        }.trimEnd()
    }
}

/**
 * Secure shell command execution subsystem for AI agents.
 *
 * Provides controlled execution of shell commands with:
 * - Configurable timeout to prevent runaway processes
 * - Working directory management
 * - Output capture (stdout and stderr)
 * - Session-based result tracking for reading past outputs
 * - Command validation and security controls
 *
 * ## Usage Example:
 *
 * ```kotlin
 * val shell = AgentShell(baseDir = Paths.get("/tmp/agent-work"))
 * val result = shell.execute("echo hello")
 * println(result) // stdout: hello
 * ```
 *
 * @param baseDir The base working directory for command execution.
 * @param defaultTimeoutSeconds The default timeout for commands in seconds.
 * @author Browser4 Team
 */
class AgentShell constructor(
    private val baseDir: Path,
    private val defaultTimeoutSeconds: Long = DEFAULT_TIMEOUT_SECONDS,
) {
    companion object {
        const val DEFAULT_TIMEOUT_SECONDS = 30L
        const val MAX_TIMEOUT_SECONDS = 300L
        const val MAX_OUTPUT_CHARS = 100_000

        private val BLOCKED_PATTERNS = listOf(
            // Prevent destructive recursive operations
            "rm\\s+-[^\\s]*r[^\\s]*\\s+/\\s*$",
            "rm\\s+-[^\\s]*r[^\\s]*\\s+/\\*",
            // Prevent format/disk operations
            "mkfs\\.",
            "dd\\s+.*of=/dev/",
            // Prevent shutdown/reboot
            "shutdown",
            "reboot",
            "init\\s+[06]",
            // Prevent fork bombs
            ":\\(\\)\\{",
        ).map { Regex(it) }
    }

    private val logger = getLogger(this)
    private val sessionCounter = AtomicLong(0)
    private val results = ConcurrentHashMap<String, ShellResult>()

    /**
     * Execute a shell command with the specified timeout.
     *
     * @param command The shell command to execute.
     * @param timeoutSeconds Timeout in seconds (default: 30, max: 300).
     * @param workingDir Optional working directory override. Defaults to baseDir.
     * @return A formatted string describing the execution result.
     */
    suspend fun execute(
        command: String,
        timeoutSeconds: Long = defaultTimeoutSeconds,
        workingDir: String? = null,
    ): String {
        if (command.isBlank()) {
            return "Error: Command must not be blank."
        }

        val violation = validateCommand(command)
        if (violation != null) {
            return "Error: Command blocked for security reasons - $violation"
        }

        val effectiveTimeout = timeoutSeconds.coerceIn(1, MAX_TIMEOUT_SECONDS)
        val sessionId = "shell-${sessionCounter.incrementAndGet()}"
        val dir = if (workingDir != null) {
            val resolved = baseDir.resolve(workingDir).normalize()
            // Prevent path traversal outside baseDir
            if (!resolved.startsWith(baseDir.normalize())) {
                return "Error: Working directory must be within the base directory."
            }
            resolved.toFile()
        } else {
            baseDir.toFile()
        }

        if (!dir.exists()) {
            dir.mkdirs()
        }

        return try {
            val result = withContext(Dispatchers.IO) {
                runCommand(sessionId, command, effectiveTimeout, dir)
            }

            results[sessionId] = result
            formatResult(result)
        } catch (e: IOException) {
            val msg = "Error: Failed to execute command '$command'. ${e.message ?: ""}"
            logger.warn(msg, e)
            msg.trim()
        } catch (e: Exception) {
            val msg = "Error: Unexpected error executing command '$command'. ${e.message ?: ""}"
            logger.warn(msg, e)
            msg.trim()
        }
    }

    /**
     * Read the output of a previous command execution by session ID.
     *
     * @param sessionId The session ID returned from execute.
     * @return The formatted output of the previous execution.
     */
    fun readOutput(sessionId: String): String {
        val result = results[sessionId]
            ?: return "Error: No result found for session '$sessionId'."
        return formatResult(result)
    }

    /**
     * Get the status of a previous command execution by session ID.
     *
     * @param sessionId The session ID to query.
     * @return A status summary of the execution.
     */
    fun getStatus(sessionId: String): String {
        val result = results[sessionId]
            ?: return "Error: No session found with ID '$sessionId'."
        val status = if (result.success) "SUCCESS" else "FAILED"
        return "Session '$sessionId': status=$status, exitCode=${result.exitCode}, " +
                "timedOut=${result.timedOut}, duration=${result.durationMs}ms, " +
                "command='${result.command}'"
    }

    /**
     * List all tracked command sessions with their status.
     *
     * @return A formatted list of all sessions.
     */
    fun listSessions(): String {
        if (results.isEmpty()) {
            return "No shell sessions recorded."
        }

        val sb = StringBuilder()
        sb.appendLine("Shell sessions (${results.size} total):")
        for ((id, result) in results) {
            val status = if (result.success) "SUCCESS" else "FAILED"
            sb.appendLine("- $id: status=$status, command='${result.command}', duration=${result.durationMs}ms")
        }
        return sb.toString().trimEnd()
    }

    private fun runCommand(
        sessionId: String,
        command: String,
        timeoutSeconds: Long,
        workDir: java.io.File,
    ): ShellResult {
        val startTime = System.currentTimeMillis()
        val isWindows = System.getProperty("os.name").lowercase().contains("win")

        val processBuilder = if (isWindows) {
            ProcessBuilder("cmd.exe", "/c", command)
        } else {
            ProcessBuilder("sh", "-c", command)
        }

        processBuilder.directory(workDir)
        processBuilder.redirectErrorStream(false)

        val process = processBuilder.start()

        // Read streams in separate threads to avoid deadlock when output buffers fill up
        val stdoutFuture = java.util.concurrent.CompletableFuture.supplyAsync {
            process.inputStream.bufferedReader().use { it.readText() }
        }
        val stderrFuture = java.util.concurrent.CompletableFuture.supplyAsync {
            process.errorStream.bufferedReader().use { it.readText() }
        }

        val completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
        val durationMs = System.currentTimeMillis() - startTime

        if (!completed) {
            process.destroyForcibly()
            val stdout = stdoutFuture.getNow("")
            val stderr = stderrFuture.getNow("")
            return ShellResult(
                sessionId = sessionId,
                command = command,
                exitCode = -1,
                stdout = truncateOutput(stdout),
                stderr = truncateOutput(stderr),
                durationMs = durationMs,
                timedOut = true,
            )
        }

        val stdout = stdoutFuture.get()
        val stderr = stderrFuture.get()

        return ShellResult(
            sessionId = sessionId,
            command = command,
            exitCode = process.exitValue(),
            stdout = truncateOutput(stdout),
            stderr = truncateOutput(stderr),
            durationMs = durationMs,
        )
    }

    private fun validateCommand(command: String): String? {
        for (pattern in BLOCKED_PATTERNS) {
            if (pattern.containsMatchIn(command)) {
                return "matches blocked pattern: ${pattern.pattern}"
            }
        }
        return null
    }

    private fun truncateOutput(output: String): String {
        return if (output.length > MAX_OUTPUT_CHARS) {
            output.take(MAX_OUTPUT_CHARS) + "\n... (output truncated at $MAX_OUTPUT_CHARS chars)"
        } else {
            output
        }
    }

    private fun formatResult(result: ShellResult): String {
        val status = if (result.success) "SUCCESS" else "FAILED"
        return buildString {
            appendLine("Session: ${result.sessionId}")
            appendLine("Status: $status")
            appendLine("Exit Code: ${result.exitCode}")
            appendLine("Duration: ${result.durationMs}ms")
            if (result.timedOut) appendLine("⚠️ Command timed out")
            if (result.stdout.isNotBlank()) {
                appendLine("--- stdout ---")
                appendLine(result.stdout)
            }
            if (result.stderr.isNotBlank()) {
                appendLine("--- stderr ---")
                appendLine(result.stderr)
            }
        }.trimEnd()
    }
}
