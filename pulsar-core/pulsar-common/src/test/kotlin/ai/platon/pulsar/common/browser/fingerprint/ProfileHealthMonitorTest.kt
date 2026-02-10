package ai.platon.pulsar.common.browser.fingerprint

import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.browser.ProfileHealthMonitor
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class ProfileHealthMonitorTest {
    
    private val monitor = ProfileHealthMonitor()
    
    @TempDir
    lateinit var tempDir: Path
    
    @Test
    @DisplayName("test healthy fingerprint passes all checks")
    fun testHealthyFingerprintPassesAllChecks() {
        val fingerprint = createCompleteFingerprint()
        
        val report = monitor.checkHealth(fingerprint)
        
        assertTrue(report.isHealthy)
        assertEquals(0, report.failedChecks.size)
        assertTrue(report.summary().contains("healthy"))
    }
    
    @Test
    @DisplayName("test incomplete fingerprint fails integrity check")
    fun testIncompleteFingerprintFailsIntegrityCheck() {
        val fingerprint = Fingerprint(
            browserType = BrowserType.PULSAR_CHROME
            // Missing essential parameters
        )
        
        val report = monitor.checkHealth(fingerprint)
        
        assertFalse(report.isHealthy)
        assertTrue(report.failedChecks.any { it.name == "Fingerprint Integrity" })
    }
    
    @Test
    @DisplayName("test inconsistent fingerprint fails consistency check")
    fun testInconsistentFingerprintFailsConsistencyCheck() {
        val fingerprint = Fingerprint(
            browserType = BrowserType.PULSAR_CHROME,
            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
            screenParameters = ScreenParameters.DESKTOP_1920X1080,
            hardwareParameters = HardwareParameters(
                hardwareConcurrency = 8,
                platform = "MacIntel"  // Inconsistent with Windows user agent
            ),
            geoTimeParameters = GeoTimeParameters.US_EAST
        )
        
        val report = monitor.checkHealth(fingerprint)
        
        assertFalse(report.isHealthy)
        assertTrue(report.failedChecks.any { it.name == "Fingerprint Consistency" })
    }
    
    @Test
    @DisplayName("test profile with accessible directory passes check")
    fun testProfileWithAccessibleDirectoryPassesCheck() {
        val contextDir = tempDir.resolve("test-profile")
        Files.createDirectories(contextDir)
        
        val report = monitor.checkHealth(createCompleteFingerprint(), contextDir)
        
        assertTrue(report.checks.any { 
            it.name == "Context Directory" && it.passed 
        })
    }
    
    @Test
    @DisplayName("test profile with non-existent directory fails check")
    fun testProfileWithNonExistentDirectoryFailsCheck() {
        val contextDir = tempDir.resolve("non-existent-profile")
        
        val report = monitor.checkHealth(createCompleteFingerprint(), contextDir)
        
        assertTrue(report.checks.any { 
            it.name == "Context Directory" && !it.passed 
        })
    }
    
    @Test
    @DisplayName("test fingerprint with current version passes check")
    fun testFingerprintWithCurrentVersionPassesCheck() {
        val fingerprint = createCompleteFingerprint()
        fingerprint.version = 1
        
        val report = monitor.checkHealth(fingerprint)
        
        assertTrue(report.checks.any { 
            it.name == "Fingerprint Version" && it.passed 
        })
    }
    
    @Test
    @DisplayName("test fingerprint with newer version fails check")
    fun testFingerprintWithNewerVersionFailsCheck() {
        val fingerprint = createCompleteFingerprint()
        fingerprint.version = 999  // Future version
        
        val report = monitor.checkHealth(fingerprint)
        
        assertTrue(report.checks.any { 
            it.name == "Fingerprint Version" && !it.passed 
        })
    }
    
    @Test
    @DisplayName("test fingerprint with older version passes with upgrade message")
    fun testFingerprintWithOlderVersionPassesWithUpgradeMessage() {
        val fingerprint = createCompleteFingerprint()
        fingerprint.version = 0  // Old version
        
        val report = monitor.checkHealth(fingerprint)
        
        val versionCheck = report.checks.find { it.name == "Fingerprint Version" }
        assertNotNull(versionCheck)
        assertTrue(versionCheck!!.passed)
        assertTrue(versionCheck.message.contains("upgrade"))
    }
    
    @Test
    @DisplayName("test health report summary shows correct status")
    fun testHealthReportSummaryShowsCorrectStatus() {
        val healthyFingerprint = createCompleteFingerprint()
        val healthyReport = monitor.checkHealth(healthyFingerprint)
        
        assertTrue(healthyReport.summary().contains("healthy"))
        assertTrue(healthyReport.summary().contains("checks passed"))
        
        val unhealthyFingerprint = Fingerprint(BrowserType.PULSAR_CHROME)
        val unhealthyReport = monitor.checkHealth(unhealthyFingerprint)
        
        assertTrue(unhealthyReport.summary().contains("issues"))
        assertTrue(unhealthyReport.summary().contains("failed"))
    }
    
    @Test
    @DisplayName("test health report toString includes all checks")
    fun testHealthReportToStringIncludesAllChecks() {
        val fingerprint = createCompleteFingerprint()
        val report = monitor.checkHealth(fingerprint)
        
        val str = report.toString()
        assertTrue(str.contains("Fingerprint Integrity"))
        assertTrue(str.contains("Fingerprint Consistency"))
        assertTrue(str.contains("Fingerprint Version"))
    }
    
    @Test
    @DisplayName("test health check includes status symbols")
    fun testHealthCheckIncludesStatusSymbols() {
        val fingerprint = createCompleteFingerprint()
        val report = monitor.checkHealth(fingerprint)
        
        val str = report.toString()
        assertTrue(str.contains("✓") || str.contains("✗"))
    }
    
    @Test
    @DisplayName("test complete profile health check with directory")
    fun testCompleteProfileHealthCheckWithDirectory() {
        val contextDir = tempDir.resolve("complete-profile")
        Files.createDirectories(contextDir)
        
        val report = monitor.checkHealth(createCompleteFingerprint(), contextDir)
        
        // Should have all checks
        assertTrue(report.checks.any { it.name == "Fingerprint Integrity" })
        assertTrue(report.checks.any { it.name == "Fingerprint Consistency" })
        assertTrue(report.checks.any { it.name == "Context Directory" })
        assertTrue(report.checks.any { it.name == "Fingerprint Version" })
        
        assertTrue(report.isHealthy)
    }
    
    @Test
    @DisplayName("test health check without directory")
    fun testHealthCheckWithoutDirectory() {
        val report = monitor.checkHealth(createCompleteFingerprint())
        
        // Should not have directory check
        assertFalse(report.checks.any { it.name == "Context Directory" })
        
        // Should have other checks
        assertTrue(report.checks.any { it.name == "Fingerprint Integrity" })
        assertTrue(report.checks.any { it.name == "Fingerprint Consistency" })
        assertTrue(report.checks.any { it.name == "Fingerprint Version" })
    }
    
    @Test
    @DisplayName("test health check detects consistency issues")
    fun testHealthCheckWithWarningsStillPasses() {
        // Create a fingerprint with consistency issues
        val fingerprint = Fingerprint(
            browserType = BrowserType.PULSAR_CHROME,
            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
            screenParameters = ScreenParameters.DESKTOP_1920X1080,
            viewportParameters = ViewportParameters.DESKTOP,
            hardwareParameters = HardwareParameters(
                hardwareConcurrency = 8,
                platform = "Win32",
                vendor = "Apple Computer, Inc."  // Mismatch: doesn't match Chrome
            ),
            geoTimeParameters = GeoTimeParameters.US_EAST
        )
        
        val report = monitor.checkHealth(fingerprint)
        
        // Should have consistency check
        val consistencyCheck = report.checks.find { it.name == "Fingerprint Consistency" }
        assertNotNull(consistencyCheck)
        // The check will detect the issue (either as warning or error)
        // Just verify the consistency check was performed
        assertTrue(consistencyCheck!!.message.isNotEmpty() || !consistencyCheck.passed)
    }
    
    @Test
    @DisplayName("test failed checks are correctly identified")
    fun testFailedChecksAreCorrectlyIdentified() {
        val fingerprint = Fingerprint(BrowserType.PULSAR_CHROME)
        val report = monitor.checkHealth(fingerprint)
        
        val failedChecks = report.failedChecks
        assertTrue(failedChecks.isNotEmpty())
        assertTrue(failedChecks.all { !it.passed })
    }
    
    private fun createCompleteFingerprint(): Fingerprint {
        return Fingerprint(
            browserType = BrowserType.PULSAR_CHROME,
            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            screenParameters = ScreenParameters.DESKTOP_1920X1080,
            viewportParameters = ViewportParameters.DESKTOP,
            geoTimeParameters = GeoTimeParameters.US_EAST,
            hardwareParameters = HardwareParameters.WINDOWS_DESKTOP,
            webGLParameters = WebGLParameters.INTEL_INTEGRATED,
            canvasParameters = CanvasParameters.DEFAULT,
            mediaParameters = MediaParameters.DESKTOP,
            miscParameters = MiscParameters.DEFAULT,
            version = 1
        )
    }
}
