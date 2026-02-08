package ai.platon.pulsar.common.browser

import org.slf4j.LoggerFactory
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.ServiceLoader
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.isRegularFile

/**
 * Loads and manages fingerprint generator providers.
 *
 * This loader supports two modes:
 * 1. **Basic mode** (default): Uses the built-in [BasicFingerprintGenerator] which provides
 *    minimal fingerprints with core parameters only.
 * 2. **Professional mode** (opt-in): Loads the professional fingerprint generator from an
 *    external plugin JAR file via [URLClassLoader]. The JAR is discovered from the plugin
 *    directory and loaded using the Java [ServiceLoader] mechanism.
 *
 * To enable professional mode, set the system property `fingerprint.generator.pro.enabled`
 * to `true`. The loader searches for plugin JARs in the directory specified by the system
 * property `fingerprint.generator.pro.plugin.dir`, defaulting to `plugins/` relative to
 * the application's working directory.
 *
 * The loader supports hot-reloading: calling [reload] will re-scan the plugin directory
 * and load any newly available professional generator plugin.
 */
object FingerprintGeneratorLoader {

    private val logger = LoggerFactory.getLogger(FingerprintGeneratorLoader::class.java)

    /**
     * System property key to enable professional fingerprinting.
     */
    const val PRO_ENABLED_KEY = "fingerprint.generator.pro.enabled"

    /**
     * System property key for the plugin directory path.
     */
    const val PLUGIN_DIR_KEY = "fingerprint.generator.pro.plugin.dir"

    /**
     * Default plugin directory name.
     */
    const val DEFAULT_PLUGIN_DIR = "plugins"

    private val basicGenerator = BasicFingerprintGenerator()

    private val activeProvider = AtomicReference<FingerprintGeneratorProvider>(basicGenerator)

    private var pluginClassLoader: URLClassLoader? = null

    /**
     * Get the currently active fingerprint generator provider.
     *
     * If professional mode is enabled and a professional provider has been loaded,
     * returns the professional provider. Otherwise, returns the basic provider.
     *
     * @return The active [FingerprintGeneratorProvider]
     */
    fun getProvider(): FingerprintGeneratorProvider {
        if (isProEnabled() && activeProvider.get() === basicGenerator) {
            // Attempt to load the pro provider if it hasn't been loaded yet
            loadProProvider()
        }
        return activeProvider.get()
    }

    /**
     * Check whether professional fingerprinting is enabled.
     */
    fun isProEnabled(): Boolean {
        return System.getProperty(PRO_ENABLED_KEY, "false").equals("true", ignoreCase = true)
    }

    /**
     * Reload the professional fingerprint generator from the plugin directory.
     *
     * This method re-scans the plugin directory for JAR files containing a
     * [FingerprintGeneratorProvider] implementation and loads it.
     * If no professional provider is found, the basic provider remains active.
     */
    fun reload() {
        closePluginClassLoader()
        if (isProEnabled()) {
            loadProProvider()
        } else {
            activeProvider.set(basicGenerator)
        }
    }

    /**
     * Get the name of the currently active provider.
     */
    fun getActiveProviderName(): String = activeProvider.get().name

    private fun loadProProvider() {
        try {
            // First, try to find the provider on the current classpath (e.g., as a Maven dependency)
            val classpathProvider = loadFromClasspath()
            if (classpathProvider != null) {
                activeProvider.set(classpathProvider)
                logger.info("Loaded professional fingerprint generator '{}' from classpath", classpathProvider.name)
                return
            }

            // Then, try to load from plugin directory (JAR hot-loading)
            val pluginDirProvider = loadFromPluginDir()
            if (pluginDirProvider != null) {
                activeProvider.set(pluginDirProvider)
                logger.info("Loaded professional fingerprint generator '{}' from plugin directory", pluginDirProvider.name)
                return
            }

            logger.warn("Professional fingerprinting is enabled but no provider found, falling back to basic generator")
        } catch (e: Exception) {
            logger.error("Failed to load professional fingerprint generator, falling back to basic generator", e)
            activeProvider.set(basicGenerator)
        }
    }

    private fun loadFromClasspath(): FingerprintGeneratorProvider? {
        val loader = ServiceLoader.load(FingerprintGeneratorProvider::class.java)
        return loader.firstOrNull { it.name != "basic" }
    }

    private fun loadFromPluginDir(): FingerprintGeneratorProvider? {
        val pluginDir = getPluginDir()
        if (!Files.isDirectory(pluginDir)) {
            logger.debug("Plugin directory does not exist: {}", pluginDir)
            return null
        }

        val jarUrls = Files.list(pluginDir)
            .filter { it.isRegularFile() && it.toString().endsWith(".jar") }
            .map { it.toUri().toURL() }
            .toArray { size -> arrayOfNulls<URL>(size) }

        if (jarUrls.isEmpty()) {
            logger.debug("No JAR files found in plugin directory: {}", pluginDir)
            return null
        }

        logger.info("Loading fingerprint plugin JARs from {}: {}", pluginDir,
            jarUrls.joinToString { it.toString() })

        val classLoader = URLClassLoader(jarUrls, FingerprintGeneratorProvider::class.java.classLoader)
        pluginClassLoader = classLoader

        val loader = ServiceLoader.load(FingerprintGeneratorProvider::class.java, classLoader)
        return loader.firstOrNull { it.name != "basic" }
    }

    private fun getPluginDir(): Path {
        val configuredDir = System.getProperty(PLUGIN_DIR_KEY)
        return if (configuredDir != null) {
            Paths.get(configuredDir)
        } else {
            Paths.get(DEFAULT_PLUGIN_DIR)
        }
    }

    private fun closePluginClassLoader() {
        try {
            pluginClassLoader?.close()
            pluginClassLoader = null
        } catch (e: Exception) {
            logger.debug("Error closing plugin class loader", e)
        }
        activeProvider.set(basicGenerator)
    }
}
