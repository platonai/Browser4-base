package ai.platon.pulsar.common.browser

/**
 * Basic fingerprint generator that provides minimal browser fingerprints.
 *
 * This is the default generator used when the professional fingerprinting plugin is not
 * available or not enabled. It generates fingerprints with core parameters (browserType,
 * userAgent, screen, viewport) but does not include advanced parameters such as WebGL,
 * canvas, media devices, or hardware details.
 */
class BasicFingerprintGenerator : FingerprintGeneratorProvider {

    override val name: String = "basic"

    override fun generate(
        browserType: BrowserType,
        preset: FingerprintGenerator.DevicePreset
    ): Fingerprint {
        return Fingerprint(browserType)
    }

    override fun generateRandom(
        browserType: BrowserType, platform: FingerprintGenerator.Platform
    ): Fingerprint {
        return Fingerprint(browserType)
    }
}
