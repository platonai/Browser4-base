package ai.platon.pulsar.common.browser

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

class BasicFingerprintGeneratorTest {

    private val generator = BasicFingerprintGenerator()

    @Test
    @DisplayName("test basic generator name is basic")
    fun testBasicGeneratorName() {
        assertEquals("basic", generator.name)
    }

    @Test
    @DisplayName("test basic generator produces fingerprint with core parameters")
    fun testBasicGeneratorProducesCoreParameters() {
        val fingerprint = generator.generate(
            BrowserType.PULSAR_CHROME,
            FingerprintGenerator.DevicePreset.DESKTOP_WINDOWS
        )

        assertNotNull(fingerprint.userAgent)
        assertTrue(fingerprint.userAgent!!.contains("Windows"))
        assertNotNull(fingerprint.screenParameters)
        assertNotNull(fingerprint.viewportParameters)
        assertEquals(1920, fingerprint.screenParameters?.width)
        assertEquals(1, fingerprint.version)
    }

    @Test
    @DisplayName("test basic generator omits advanced parameters")
    fun testBasicGeneratorOmitsAdvancedParameters() {
        val fingerprint = generator.generate(
            BrowserType.PULSAR_CHROME,
            FingerprintGenerator.DevicePreset.DESKTOP_WINDOWS
        )

        assertNull(fingerprint.geoTimeParameters)
        assertNull(fingerprint.hardwareParameters)
        assertNull(fingerprint.webGLParameters)
        assertNull(fingerprint.canvasParameters)
        assertNull(fingerprint.mediaParameters)
        assertNull(fingerprint.miscParameters)
    }

    @Test
    @DisplayName("test basic generator all presets produce valid fingerprints")
    fun testBasicGeneratorAllPresets() {
        FingerprintGenerator.DevicePreset.values().forEach { preset ->
            val fingerprint = generator.generate(BrowserType.PULSAR_CHROME, preset)
            assertNotNull(fingerprint.userAgent, "User agent should be set for preset $preset")
            assertNotNull(fingerprint.screenParameters, "Screen parameters should be set for preset $preset")
            assertNotNull(fingerprint.viewportParameters, "Viewport parameters should be set for preset $preset")
        }
    }

    @Test
    @DisplayName("test basic generator random generation for all platforms")
    fun testBasicGeneratorRandomGeneration() {
        FingerprintGenerator.Platform.values().forEach { platform ->
            val fingerprint = generator.generateRandom(BrowserType.PULSAR_CHROME, platform)
            assertNotNull(fingerprint.userAgent, "User agent should be set for platform $platform")
        }
    }

    @Test
    @DisplayName("test basic generator Mac preset has correct user agent")
    fun testBasicGeneratorMacPreset() {
        val fingerprint = generator.generate(
            BrowserType.PULSAR_CHROME,
            FingerprintGenerator.DevicePreset.MACBOOK_PRO_13
        )

        assertTrue(fingerprint.userAgent!!.contains("Macintosh"))
        assertEquals(2560, fingerprint.screenParameters?.width)
        assertEquals(2.0, fingerprint.viewportParameters?.deviceScaleFactor)
    }

    @Test
    @DisplayName("test basic generator Linux preset has correct user agent")
    fun testBasicGeneratorLinuxPreset() {
        val fingerprint = generator.generate(
            BrowserType.PULSAR_CHROME,
            FingerprintGenerator.DevicePreset.DESKTOP_LINUX
        )

        assertTrue(fingerprint.userAgent!!.contains("Linux"))
        assertEquals(1920, fingerprint.screenParameters?.width)
    }

    @Test
    @DisplayName("test basic generator implements FingerprintGeneratorProvider")
    fun testBasicGeneratorImplementsProvider() {
        assertTrue(generator is FingerprintGeneratorProvider)
    }
}
