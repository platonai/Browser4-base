package ai.platon.pulsar.common.browser

/**
 * Service provider interface for fingerprint generation.
 *
 * Implementations of this interface provide browser fingerprint generation capabilities.
 * The system uses Java ServiceLoader to discover available providers. When the professional
 * fingerprinting plugin is available and enabled via configuration, it will be used;
 * otherwise, the basic built-in generator is used.
 *
 * @see BasicFingerprintGenerator
 */
interface FingerprintGeneratorProvider {

    /**
     * The unique name of this provider (e.g., "basic", "pro").
     */
    val name: String

    /**
     * Generate a fingerprint for the given browser type and device preset.
     *
     * @param browserType The target browser type
     * @param preset Device preset to use
     * @return A fully configured fingerprint
     */
    fun generate(
        browserType: BrowserType,
        preset: FingerprintGenerator.DevicePreset = FingerprintGenerator.DevicePreset.DESKTOP_WINDOWS
    ): Fingerprint

    /**
     * Generate a fingerprint with a randomized device preset for the given platform.
     *
     * @param browserType The target browser type
     * @param platform Target platform (Windows, Mac, Linux)
     * @return A fully configured fingerprint
     */
    fun generateRandom(
        browserType: BrowserType,
        platform: FingerprintGenerator.Platform = FingerprintGenerator.Platform.WINDOWS
    ): Fingerprint
}
