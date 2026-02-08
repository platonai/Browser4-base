package ai.platon.pulsar.skeleton.crawl.fetch.privacy

import ai.platon.browser4.driver.common.BrowserSettings
import ai.platon.pulsar.common.browser.BrowserProfileMode
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.browser.Fingerprint
import ai.platon.pulsar.common.config.CapabilityTypes.BROWSER_CONTEXT_NUMBER
import ai.platon.pulsar.common.config.CapabilityTypes.PRIVACY_AGENT_GENERATOR_CLASS
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.logging.ThrottlingLogger
import ai.platon.pulsar.common.serialize.json.pulsarObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.nio.file.Files
import java.nio.file.Path

/**
 * A browser profile defines a unique agent to visit websites.
 *
 * Page visits through different privacy agents should not be detected
 * as the same person, even if the visits are from the same host.
 * */
data class BrowserProfile(
    val contextDir: Path,
    var fingerprint: Fingerprint
) : Comparable<BrowserProfile> {

    val id = ProfileId(contextDir, fingerprint.browserType)
    val ident get() = id.ident
    val display get() = id.display
    val browserType get() = fingerprint.browserType
    val isSystemDefault get() = id.isSystemDefault
    val isDefault get() = id.isDefault
    val isPrototype get() = id.isPrototype
    val isGroup get() = id.isGroup
    val isTemporary get() = id.isTemporary
    val isPermanent get() = id.isPermanent

    constructor(contextDir: Path) : this(contextDir, BrowserType.PULSAR_CHROME)

    constructor(contextDir: Path, browserType: BrowserType) : this(contextDir, Fingerprint(browserType))

    /**
     * The PrivacyAgent equality.
     * Note: do not use the default equality function
     * */
    override fun equals(other: Any?) = other is BrowserProfile && other.id == this.id

    override fun hashCode() = id.hashCode()

    override fun compareTo(other: BrowserProfile) = id.compareTo(other.id)

//    override fun toString() = /** AUTO GENERATED **/

    companion object {
        private val logger = getLogger(this)
        private val throttlingLogger = ThrottlingLogger(logger)
        
        // Lazy initialization of fingerprint generator
        private val fingerprintGenerator by lazy { 
            ai.platon.pulsar.common.browser.FingerprintGenerator() 
        }
        
        // Fingerprint validator for loaded fingerprints
        private val fingerprintValidator by lazy {
            ai.platon.pulsar.common.browser.FingerprintValidator()
        }

        /**
         * The random browser profile opens browser with a random data dir.
         * */
        val RANDOM_TEMP get() = createRandomTemp()

        private fun create(contextDir: Path) = create(BrowserType.PULSAR_CHROME, contextDir)

        private fun create(browserType: BrowserType, contextDir: Path): BrowserProfile {
            val path = contextDir.resolve("fingerprint.json")
            val fingerprint: Fingerprint = if (Files.exists(path)) {
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
            return BrowserProfile(contextDir, fingerprint)
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
                        System.getProperty("os.name").contains("Windows", ignoreCase = true) -> 
                            ai.platon.pulsar.common.browser.FingerprintGenerator.Platform.WINDOWS
                        System.getProperty("os.name").contains("Mac", ignoreCase = true) -> 
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

        fun createSystemDefault(): BrowserProfile {
            return createSystemDefault(BrowserType.DEFAULT)
        }

        fun createSystemDefault(browserType: BrowserType): BrowserProfile {
            throttlingLogger.info("You are creating a SYSTEM_DEFAULT browser context, force set max browser number to be 1")
            throttlingLogger.info("Chrome DevTools remote debugging requires a non-default data directory. Specify this using --user-data-dir.")

            BrowserSettings.withBrowserContextMode(BrowserProfileMode.SYSTEM_DEFAULT, browserType)
            require(System.getProperty(BROWSER_CONTEXT_NUMBER).toIntOrNull() == 1)
            require(System.getProperty(PRIVACY_AGENT_GENERATOR_CLASS).contains("SystemDefaultPrivacyAgentGenerator"))
            return create(browserType, PrivacyContext.SYSTEM_DEFAULT_BROWSER_CONTEXT_DIR_PLACEHOLDER)
        }

        fun createDefault(): BrowserProfile {
            return createDefault(BrowserType.DEFAULT)
        }

        fun createDefault(browserType: BrowserType): BrowserProfile {
            throttlingLogger.info("You are creating a DEFAULT browser context, force set max browser number to 1")

            BrowserSettings.withBrowserContextMode(BrowserProfileMode.DEFAULT, browserType)
            require(System.getProperty(BROWSER_CONTEXT_NUMBER).toIntOrNull() == 1)
            require(System.getProperty(PRIVACY_AGENT_GENERATOR_CLASS).contains("DefaultPrivacyAgentGenerator"))
            return create(browserType, PrivacyContext.DEFAULT_CONTEXT_DIR)
        }

        fun createPrototype(): BrowserProfile {
            return createPrototype(BrowserType.DEFAULT)
        }

        fun createPrototype(browserType: BrowserType): BrowserProfile {
            throttlingLogger.info("You are creating a PROTOTYPE browser context, force set max browser number to be 1")

            BrowserSettings.withBrowserContextMode(BrowserProfileMode.PROTOTYPE, browserType)
            require(System.getProperty(BROWSER_CONTEXT_NUMBER).toIntOrNull() == 1)
            require(System.getProperty(PRIVACY_AGENT_GENERATOR_CLASS).contains("PrototypePrivacyAgentGenerator"))
            return create(browserType, PrivacyContext.PROTOTYPE_CONTEXT_DIR)
        }

        fun createNextSequential() = createNextSequential(BrowserType.PULSAR_CHROME)

        fun createNextSequential(browserType: BrowserType): BrowserProfile {
            BrowserSettings.withBrowserContextMode(BrowserProfileMode.SEQUENTIAL, browserType)
            return create(browserType, PrivacyContext.NEXT_SEQUENTIAL_CONTEXT_DIR)
        }

        fun createRandomTemp() = createRandomTemp(BrowserType.PULSAR_CHROME)

        fun createRandomTemp(browserType: BrowserType): BrowserProfile {
            BrowserSettings.withBrowserContextMode(BrowserProfileMode.TEMPORARY, browserType)
            return create(browserType, PrivacyContext.createRandom(browserType))
        }
    }
}
