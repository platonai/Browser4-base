package ai.platon.pulsar.common.browser.fingerprint

import ai.platon.pulsar.common.browser.BrowserType
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
    @DisplayName("test basic generator omits advanced parameters")
    fun testBasicGeneratorOmitsAdvancedParameters() {
        val fingerprint = generator.generate(
            BrowserType.PULSAR_CHROME,
            BasicFingerprintGenerator.DevicePreset.DESKTOP_WINDOWS
        )

        assertNull(fingerprint.geoTimeParameters)
        assertNull(fingerprint.hardwareParameters)
        assertNull(fingerprint.webGLParameters)
        assertNull(fingerprint.canvasParameters)
        assertNull(fingerprint.mediaParameters)
        assertNull(fingerprint.miscParameters)
    }
}
