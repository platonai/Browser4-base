package ai.platon.pulsar.plugins.fingerprint.pro

import ai.platon.pulsar.common.browser.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

class ProFingerprintGeneratorTest {

    private val generator = ProFingerprintGenerator()
    private val validator = FingerprintValidator()

    @Test
    @DisplayName("test pro generator name is pro")
    fun testProGeneratorName() {
        assertEquals("pro", generator.name)
    }

    @Test
    @DisplayName("test pro generator implements FingerprintGeneratorProvider")
    fun testProGeneratorImplementsProvider() {
        assertTrue(generator is FingerprintGeneratorProvider)
    }

    @Test
    @DisplayName("test pro generator produces full fingerprint with all parameters")
    fun testProGeneratorProducesFullFingerprint() {
        val fingerprint = generator.generate(
            BrowserType.PULSAR_CHROME,
            FingerprintGenerator.DevicePreset.DESKTOP_WINDOWS
        )

        assertNotNull(fingerprint.userAgent, "User agent should be set")
        assertNotNull(fingerprint.screenParameters, "Screen parameters should be set")
        assertNotNull(fingerprint.viewportParameters, "Viewport parameters should be set")
        assertNotNull(fingerprint.geoTimeParameters, "Geo-time parameters should be set")
        assertNotNull(fingerprint.hardwareParameters, "Hardware parameters should be set")
        assertNotNull(fingerprint.webGLParameters, "WebGL parameters should be set")
        assertNotNull(fingerprint.canvasParameters, "Canvas parameters should be set")
        assertNotNull(fingerprint.mediaParameters, "Media parameters should be set")
        assertNotNull(fingerprint.miscParameters, "Misc parameters should be set")
    }

    @Test
    @DisplayName("test pro generator passes validation for all presets")
    fun testProGeneratorPassesValidation() {
        FingerprintGenerator.DevicePreset.values().forEach { preset ->
            val fingerprint = generator.generate(BrowserType.PULSAR_CHROME, preset)
            val result = validator.validate(fingerprint)
            assertTrue(result.isValid, "Fingerprint for preset $preset should be valid: $result")
        }
    }

    @Test
    @DisplayName("test pro generator unique canvas seeds")
    fun testProGeneratorUniqueCanvasSeeds() {
        val fingerprint1 = generator.generate(
            BrowserType.PULSAR_CHROME,
            FingerprintGenerator.DevicePreset.DESKTOP_WINDOWS
        )
        Thread.sleep(10)
        val fingerprint2 = generator.generate(
            BrowserType.PULSAR_CHROME,
            FingerprintGenerator.DevicePreset.DESKTOP_WINDOWS
        )

        assertNotEquals(
            fingerprint1.canvasParameters?.fingerprintSeed,
            fingerprint2.canvasParameters?.fingerprintSeed,
            "Canvas seeds should be unique"
        )
    }

    @Test
    @DisplayName("test pro generator Mac preset has Apple-specific settings")
    fun testProGeneratorMacAppleSettings() {
        val fingerprint = generator.generate(
            BrowserType.PULSAR_CHROME,
            FingerprintGenerator.DevicePreset.MACBOOK_PRO_13
        )

        assertEquals("MacIntel", fingerprint.hardwareParameters?.platform)
        assertTrue(fingerprint.webGLParameters?.vendor?.contains("Apple") ?: false)
        assertTrue(
            fingerprint.mediaParameters?.videoInputDevices?.any { it.label.contains("FaceTime") } ?: false
        )
    }
}
