package ai.platon.pulsar.common

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Test process cleanup improvements
 */
class ProcessCleanupTest {

    @Test
    fun `test isProcessAlive with invalid PID`() {
        // Negative PID should always be false
        assertFalse(Runtimes.isProcessAlive(-1))
        assertFalse(Runtimes.isProcessAlive(0))
        
        // Use Int.MAX_VALUE which is guaranteed not to be a valid PID
        assertFalse(Runtimes.isProcessAlive(Int.MAX_VALUE))
    }
    
    @Test
    fun `test isProcessAlive with Int overload`() {
        // Test the Int overload we added
        val invalidPid: Int = -1
        assertFalse(Runtimes.isProcessAlive(invalidPid))
    }

    @Test
    @DisabledOnOs(OS.WINDOWS, disabledReason = "Process creation test may behave differently on Windows")
    fun `test process cleanup with short-lived process`() {
        // Create a short-lived process
        val processBuilder = ProcessBuilder("sh", "-c", "sleep 1")
        val process = processBuilder.start()
        val pid = process.pid()
        
        // Process should be alive
        assertTrue(process.isAlive)
        assertTrue(Runtimes.isProcessAlive(pid.toInt()))
        
        // Wait for process to complete
        process.waitFor()
        
        // Process should be dead
        assertFalse(process.isAlive)
        assertFalse(Runtimes.isProcessAlive(pid.toInt()))
    }
}
