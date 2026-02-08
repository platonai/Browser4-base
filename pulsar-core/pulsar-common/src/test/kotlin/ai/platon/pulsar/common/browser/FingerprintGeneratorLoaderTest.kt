package ai.platon.pulsar.common.browser

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

class FingerprintGeneratorLoaderTest {

    @AfterEach
    fun tearDown() {
        System.clearProperty(FingerprintGeneratorLoader.PRO_ENABLED_KEY)
        System.clearProperty(FingerprintGeneratorLoader.PLUGIN_DIR_KEY)
        FingerprintGeneratorLoader.reload()
    }

    @Test
    @DisplayName("test default provider is basic generator")
    fun testDefaultProviderIsBasic() {
        val provider = FingerprintGeneratorLoader.getProvider()
        assertEquals("basic", provider.name)
    }

    @Test
    @DisplayName("test provider generates valid fingerprint")
    fun testProviderGeneratesValidFingerprint() {
        val provider = FingerprintGeneratorLoader.getProvider()
        val fingerprint = provider.generate(
            BrowserType.PULSAR_CHROME,
            FingerprintGenerator.DevicePreset.DESKTOP_WINDOWS
        )

        assertNotNull(fingerprint.userAgent)
        assertNotNull(fingerprint.screenParameters)
    }

    @Test
    @DisplayName("test pro enabled without plugin falls back to basic")
    fun testProEnabledWithoutPluginFallsToBasic() {
        System.setProperty(FingerprintGeneratorLoader.PRO_ENABLED_KEY, "true")
        System.setProperty(FingerprintGeneratorLoader.PLUGIN_DIR_KEY, "/tmp/nonexistent-plugin-dir")
        FingerprintGeneratorLoader.reload()

        val provider = FingerprintGeneratorLoader.getProvider()
        // Without the plugin JAR on classpath, should fall back to basic
        assertEquals("basic", provider.name)
    }

    @Test
    @DisplayName("test pro not enabled returns basic")
    fun testProNotEnabledReturnsBasic() {
        System.setProperty(FingerprintGeneratorLoader.PRO_ENABLED_KEY, "false")
        FingerprintGeneratorLoader.reload()

        assertEquals("basic", FingerprintGeneratorLoader.getActiveProviderName())
    }

    @Test
    @DisplayName("test reload resets provider")
    fun testReloadResetsProvider() {
        val before = FingerprintGeneratorLoader.getActiveProviderName()
        FingerprintGeneratorLoader.reload()
        val after = FingerprintGeneratorLoader.getActiveProviderName()

        assertEquals(before, after)
    }

    @Test
    @DisplayName("test isProEnabled returns false by default")
    fun testIsProEnabledDefault() {
        assertFalse(FingerprintGeneratorLoader.isProEnabled())
    }

    @Test
    @DisplayName("test isProEnabled returns true when configured")
    fun testIsProEnabledWhenConfigured() {
        System.setProperty(FingerprintGeneratorLoader.PRO_ENABLED_KEY, "true")
        assertTrue(FingerprintGeneratorLoader.isProEnabled())
    }
}
