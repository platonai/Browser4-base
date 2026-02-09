package ai.platon.pulsar.common.browser

import ai.platon.pulsar.common.serialize.json.pulsarObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.apache.commons.lang3.SystemUtils
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path

class FingerprintLoader(
    private val browserType: BrowserType,
    private val contextDir: Path
) {
    private val logger = LoggerFactory.getLogger(FingerprintLoader::class.java)

    // Lazy initialization of fingerprint generator via the loader
    private val fingerprintGenerator: FingerprintGeneratorProvider by lazy {
        FingerprintGeneratorLoader.getProvider()
    }

    // Fingerprint validator for loaded fingerprints
    private val fingerprintValidator by lazy {
        FingerprintValidator()
    }

    fun loadOrGenerateFingerprint(): Fingerprint {
        val path = contextDir.resolve("fingerprint.json")
        val fingerprint = if (Files.exists(path)) {
            // Load existing fingerprint
            try {
                val loaded = pulsarObjectMapper().readValue<Fingerprint>(path.toFile())
                    .also { it.source = path.toString() }

                // Validate loaded fingerprint
                val validationResult = fingerprintValidator.validate(loaded)
                if (!validationResult.isValid) {
                    logger.warn("Loaded fingerprint validation failed: ${validationResult.summary()}")
                    logger.warn("Validation errors: ${validationResult.errors.joinToString(", ")}")
                } else if (validationResult.hasWarnings) {
                    logger.debug("Loaded fingerprint has warnings: ${validationResult.warnings.joinToString(", ")}")
                }

                loaded
            } catch (e: Exception) {
                logger.warn("Failed to load fingerprint from $path, generating new one", e)
                generateAndSaveFingerprint(browserType, contextDir)
            }
        } else {
            // Generate new complete fingerprint
            generateAndSaveFingerprint(browserType, contextDir)
        }

        fingerprint.browserType = browserType

        return fingerprint
    }

    /**
     * Generate a complete fingerprint and save it to the context directory.
     *
     * This method generates a realistic fingerprint with all parameters populated
     * and saves it to fingerprint.json for future use.
     */
    private fun generateAndSaveFingerprint(browserType: BrowserType, contextDir: Path): Fingerprint {
        // Determine appropriate device preset based on context
        val preset = when {
            // For temporary contexts, use random generation
            contextDir.toString().contains("/tmp/") || contextDir.toString().contains("\\temp\\") -> {
                val platform = when {
                    SystemUtils.IS_OS_WINDOWS ->
                        ai.platon.pulsar.common.browser.FingerprintGenerator.Platform.WINDOWS

                    SystemUtils.IS_OS_MAC ->
                        ai.platon.pulsar.common.browser.FingerprintGenerator.Platform.MAC

                    else ->
                        ai.platon.pulsar.common.browser.FingerprintGenerator.Platform.LINUX
                }
                return fingerprintGenerator.generateRandom(browserType, platform).also { fingerprint ->
                    saveFingerprintToFile(fingerprint, contextDir)
                }
            }
            // For permanent contexts, use desktop preset
            else -> ai.platon.pulsar.common.browser.FingerprintGenerator.DevicePreset.DESKTOP_WINDOWS
        }

        val fingerprint = fingerprintGenerator.generate(browserType, preset)
        saveFingerprintToFile(fingerprint, contextDir)
        return fingerprint
    }

    /**
     * Save fingerprint to fingerprint.json file.
     */
    private fun saveFingerprintToFile(fingerprint: Fingerprint, contextDir: Path) {
        try {
            Files.createDirectories(contextDir)
            val path = contextDir.resolve("fingerprint.json")
            val json = ai.platon.pulsar.common.serialize.json.prettyPulsarObjectMapper()
                .writeValueAsString(fingerprint)
            Files.writeString(path, json)
            logger.info("Generated and saved fingerprint to $path")
        } catch (e: Exception) {
            logger.warn("Failed to save fingerprint to $contextDir", e)
        }
    }

}
