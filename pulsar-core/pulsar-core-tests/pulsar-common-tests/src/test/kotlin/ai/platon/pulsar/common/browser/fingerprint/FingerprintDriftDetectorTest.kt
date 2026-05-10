package ai.platon.pulsar.common.browser.fingerprint

import ai.platon.pulsar.common.browser.BrowserType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

class FingerprintDriftDetectorTest {

    private val detector = FingerprintDriftDetector()

    @Test
    @DisplayName("test no drift when fingerprints are identical")
    fun testNoDriftWhenIdentical() {
        val fingerprint = createTestFingerprint()

        val report = detector.detectDrift(fingerprint, fingerprint)

        assertFalse(report.hasDrift)
        assertEquals(0, report.drifts.size)
        assertTrue(report.summary().contains("No fingerprint drift"))
    }

    @Test
    @DisplayName("test drift detected when user agent changes")
    fun testDriftDetectedUserAgent() {
        val original = createTestFingerprint()
        val current = original.copy(userAgent = "Different User Agent")

        val report = detector.detectDrift(original, current)

        assertTrue(report.hasDrift)
        assertTrue(report.drifts.any { it.contains("User agent changed") })
    }

    @Test
    @DisplayName("test drift detected when screen resolution changes")
    fun testDriftDetectedScreenResolution() {
        val original = createTestFingerprint()
        val current = original.copy(
            screenParameters = ScreenParameters(
                width = 2560,  // Changed from 1920
                height = 1440,  // Changed from 1080
                availWidth = 2560,
                availHeight = 1400
            )
        )

        val report = detector.detectDrift(original, current)

        assertTrue(report.hasDrift)
        assertTrue(report.drifts.any { it.contains("Screen resolution changed") })
        assertTrue(report.drifts.any { it.contains("1920x1080") && it.contains("2560x1440") })
    }

    @Test
    @DisplayName("test drift detected when device pixel ratio changes")
    fun testDriftDetectedPixelRatio() {
        val original = createTestFingerprint()
        val current = original.copy(
            screenParameters = original.screenParameters?.copy(devicePixelRatio = 2.0)
        )

        val report = detector.detectDrift(original, current)

        assertTrue(report.hasDrift)
        assertTrue(report.drifts.any { it.contains("Device pixel ratio changed") })
    }

    @Test
    @DisplayName("test drift detected when timezone changes")
    fun testDriftDetectedTimezone() {
        val original = createTestFingerprint()
        val current = original.copy(
            geoTimeParameters = original.geoTimeParameters?.copy(timezone = "Asia/Tokyo")
        )

        val report = detector.detectDrift(original, current)

        assertTrue(report.hasDrift)
        assertTrue(report.drifts.any { it.contains("Timezone changed") })
        assertTrue(report.drifts.any { it.contains("America/New_York") && it.contains("Asia/Tokyo") })
    }

    @Test
    @DisplayName("test drift detected when hardware concurrency changes")
    fun testDriftDetectedHardwareConcurrency() {
        val original = createTestFingerprint()
        val current = original.copy(
            hardwareParameters = original.hardwareParameters?.copy(hardwareConcurrency = 16)
        )

        val report = detector.detectDrift(original, current)

        assertTrue(report.hasDrift)
        assertTrue(report.drifts.any { it.contains("Hardware concurrency changed") })
    }

    @Test
    @DisplayName("test drift detected when platform changes")
    fun testDriftDetectedPlatform() {
        val original = createTestFingerprint()
        val current = original.copy(
            hardwareParameters = original.hardwareParameters?.copy(platform = "MacIntel")
        )

        val report = detector.detectDrift(original, current)

        assertTrue(report.hasDrift)
        assertTrue(report.drifts.any { it.contains("Platform changed") })
    }

    @Test
    @DisplayName("test drift detected when WebGL vendor changes")
    fun testDriftDetectedWebGLVendor() {
        val original = createTestFingerprint()
        val current = original.copy(
            webGLParameters = original.webGLParameters?.copy(vendor = "NVIDIA Corporation")
        )

        val report = detector.detectDrift(original, current)

        assertTrue(report.hasDrift)
        assertTrue(report.drifts.any { it.contains("WebGL vendor changed") })
    }

    @Test
    @DisplayName("test drift detected when canvas seed changes")
    fun testDriftDetectedCanvasSeed() {
        val original = createTestFingerprint()
        val current = original.copy(
            canvasParameters = CanvasParameters(fingerprintSeed = "different-seed")
        )

        val report = detector.detectDrift(original, current)

        assertTrue(report.hasDrift)
        assertTrue(report.drifts.any { it.contains("Canvas fingerprint seed changed") })
    }

    @Test
    @DisplayName("test drift detected when parameters are added")
    fun testDriftDetectedParametersAdded() {
        val original = Fingerprint(
            browserType = BrowserType.PULSAR_CHROME,
            userAgent = "Test User Agent"
        )
        val current = createTestFingerprint()

        val report = detector.detectDrift(original, current)

        assertTrue(report.hasDrift)
        assertTrue(report.drifts.any { it.contains("added") })
    }

    @Test
    @DisplayName("test drift detected when parameters are removed")
    fun testDriftDetectedParametersRemoved() {
        val original = createTestFingerprint()
        val current = Fingerprint(
            browserType = BrowserType.PULSAR_CHROME,
            userAgent = original.userAgent
        )

        val report = detector.detectDrift(original, current)

        assertTrue(report.hasDrift)
        assertTrue(report.drifts.any { it.contains("removed") })
    }

    @Test
    @DisplayName("test multiple drifts detected")
    fun testMultipleDriftsDetected() {
        val original = createTestFingerprint()
        val current = original.copy(
            userAgent = "Different User Agent",
            screenParameters = original.screenParameters?.copy(width = 2560),
            hardwareParameters = original.hardwareParameters?.copy(hardwareConcurrency = 16)
        )

        val report = detector.detectDrift(original, current)

        assertTrue(report.hasDrift)
        assertTrue(report.drifts.size >= 3)
    }

    @Test
    @DisplayName("test drift report summary")
    fun testDriftReportSummary() {
        val original = createTestFingerprint()
        val current = original.copy(userAgent = "Different")

        val report = detector.detectDrift(original, current)

        assertTrue(report.summary().contains("drift detected"))
        assertTrue(report.summary().contains("1"))
    }

    @Test
    @DisplayName("test drift report toString")
    fun testDriftReportToString() {
        val original = createTestFingerprint()
        val current = original.copy(userAgent = "Different")

        val report = detector.detectDrift(original, current)

        val str = report.toString()
        assertTrue(str.contains("drift detected"))
        assertTrue(str.contains("User agent"))
    }

    private fun createTestFingerprint(): Fingerprint {
        return Fingerprint(
            browserType = BrowserType.PULSAR_CHROME,
            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
            screenParameters = ScreenParameters(
                width = 1920,
                height = 1080,
                availWidth = 1920,
                availHeight = 1040,
                devicePixelRatio = 1.0
            ),
            viewportParameters = ViewportParameters(
                width = 1920,
                height = 1080,
                deviceScaleFactor = 1.0
            ),
            geoTimeParameters = GeoTimeParameters(
                timezone = "America/New_York",
                timezoneOffset = 300,
                locale = "en-US",
                languages = listOf("en-US", "en")
            ),
            hardwareParameters = HardwareParameters(
                hardwareConcurrency = 8,
                platform = "Win32"
            ),
            webGLParameters = WebGLParameters(
                vendor = "Google Inc. (Intel)",
                renderer = "ANGLE (Intel, Intel(R) UHD Graphics)"
            ),
            canvasParameters = CanvasParameters(
                fingerprintSeed = "test-seed-123"
            )
        )
    }
}
