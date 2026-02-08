package ai.platon.pulsar.skeleton.crawl.fetch.privacy

import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.browser.Fingerprint
import ai.platon.pulsar.common.serialize.json.pulsarObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class BrowserProfileTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun createUsesExistingFingerprintWhenPresent() {
        val profileDir = tempDir.resolve("existing-profile")
        Files.createDirectories(profileDir)
        val fingerprintFile = profileDir.resolve("fingerprint.json")
        val existingFingerprint = Fingerprint(BrowserType.PLAYWRIGHT_CHROME).apply {
            userAgent = "custom-agent"
        }
        pulsarObjectMapper().writeValue(fingerprintFile.toFile(), existingFingerprint)

        val profile = BrowserProfile.create(BrowserType.PULSAR_CHROME, profileDir)

        assertEquals(BrowserType.PULSAR_CHROME, profile.fingerprint.browserType)
        assertEquals("custom-agent", profile.fingerprint.userAgent)
        assertEquals(fingerprintFile.toString(), profile.fingerprint.source)
    }

    @Test
    fun createGeneratesFingerprintWhenMissing() {
        val profileDir = tempDir.resolve("generated-profile")

        val profile = BrowserProfile.create(BrowserType.PULSAR_CHROME, profileDir)

        val fingerprintFile = profileDir.resolve("fingerprint.json")
        assertTrue(Files.exists(fingerprintFile))
        val savedFingerprint = pulsarObjectMapper().readValue(fingerprintFile.toFile(), Fingerprint::class.java)

        assertEquals(BrowserType.PULSAR_CHROME, profile.fingerprint.browserType)
        assertEquals(savedFingerprint.userAgent, profile.fingerprint.userAgent)
        assertNotNull(savedFingerprint.userAgent)
    }
}

