package ai.platon.pulsar.common.browser

import kotlin.random.Random

/**
 * Professional fingerprint generator that produces realistic and consistent browser
 * fingerprints with full anti-detection parameter coverage.
 *
 * The generator ensures that all parameters are logically coherent (e.g., userAgent matches
 * platform, screen resolution is reasonable for the device type, etc.) and uses common
 * device configurations to avoid detection.
 *
 * This class implements [FingerprintGeneratorProvider] and serves as the built-in
 * professional fingerprint generator. When the professional fingerprinting plugin is
 * enabled, this generator (or one loaded from an external JAR) provides full-coverage
 * fingerprints including WebGL, canvas, media devices, hardware, and geo-time parameters.
 *
 * @see BasicFingerprintGenerator for the default basic generator
 * @see FingerprintGeneratorLoader for the loading mechanism
 */
class FingerprintGenerator : FingerprintGeneratorProvider {
    
    override val name: String = "pro"
    
    private val validator = FingerprintValidator()
    
    /**
     * Generate a realistic fingerprint for the given browser type.
     *
     * @param browserType The target browser type
     * @param preset Optional device preset to use (Desktop, Laptop, MacBook, etc.)
     * @return A fully configured, validated fingerprint
     * @throws IllegalStateException if generated fingerprint fails validation
     */
    override fun generate(
        browserType: BrowserType,
        preset: DevicePreset
    ): Fingerprint {
        val fingerprint = when (preset) {
            DevicePreset.DESKTOP_WINDOWS -> generateDesktopWindows(browserType)
            DevicePreset.LAPTOP_WINDOWS -> generateLaptopWindows(browserType)
            DevicePreset.MACBOOK_PRO_13 -> generateMacBookPro13(browserType)
            DevicePreset.MACBOOK_AIR -> generateMacBookAir(browserType)
            DevicePreset.DESKTOP_LINUX -> generateDesktopLinux(browserType)
            DevicePreset.LAPTOP_LINUX -> generateLaptopLinux(browserType)
        }
        
        // Validate the generated fingerprint
        val result = validator.validate(fingerprint)
        if (!result.isValid) {
            throw IllegalStateException("Generated fingerprint failed validation: $result")
        }
        
        return fingerprint
    }
    
    /**
     * Generate a fingerprint with randomized but reasonable parameters.
     *
     * @param browserType The target browser type
     * @param platform Target platform (Windows, Mac, Linux)
     * @return A fully configured, validated fingerprint
     */
    override fun generateRandom(
        browserType: BrowserType,
        platform: Platform
    ): Fingerprint {
        val preset = when (platform) {
            Platform.WINDOWS -> listOf(
                DevicePreset.DESKTOP_WINDOWS,
                DevicePreset.LAPTOP_WINDOWS
            ).random()
            Platform.MAC -> listOf(
                DevicePreset.MACBOOK_PRO_13,
                DevicePreset.MACBOOK_AIR
            ).random()
            Platform.LINUX -> listOf(
                DevicePreset.DESKTOP_LINUX,
                DevicePreset.LAPTOP_LINUX
            ).random()
        }
        
        return generate(browserType, preset)
    }
    
    private fun generateDesktopWindows(browserType: BrowserType): Fingerprint {
        val userAgent = generateUserAgent(browserType, Platform.WINDOWS, "10.0")
        
        return Fingerprint(
            browserType = browserType,
            userAgent = userAgent,
            screenParameters = ScreenParameters.DESKTOP_1920X1080,
            viewportParameters = ViewportParameters.DESKTOP,
            geoTimeParameters = GeoTimeParameters.US_EAST,
            hardwareParameters = HardwareParameters.WINDOWS_DESKTOP,
            webGLParameters = WebGLParameters.INTEL_INTEGRATED,
            canvasParameters = CanvasParameters(
                fingerprintSeed = generateCanvasSeed()
            ),
            mediaParameters = MediaParameters.DESKTOP,
            miscParameters = MiscParameters.DEFAULT,
            version = 1
        )
    }
    
    private fun generateLaptopWindows(browserType: BrowserType): Fingerprint {
        val userAgent = generateUserAgent(browserType, Platform.WINDOWS, "10.0")
        
        return Fingerprint(
            browserType = browserType,
            userAgent = userAgent,
            screenParameters = ScreenParameters.LAPTOP_1366X768,
            viewportParameters = ViewportParameters.LAPTOP,
            geoTimeParameters = GeoTimeParameters.US_EAST,
            hardwareParameters = HardwareParameters(
                hardwareConcurrency = 4,
                deviceMemory = 8,
                maxTouchPoints = 0,
                platform = "Win32",
                vendor = "Google Inc."
            ),
            webGLParameters = WebGLParameters.INTEL_INTEGRATED,
            canvasParameters = CanvasParameters(
                fingerprintSeed = generateCanvasSeed()
            ),
            mediaParameters = MediaParameters.DESKTOP,
            miscParameters = MiscParameters.DEFAULT,
            version = 1
        )
    }
    
    private fun generateMacBookPro13(browserType: BrowserType): Fingerprint {
        val userAgent = generateUserAgent(browserType, Platform.MAC, "10_15_7")
        
        return Fingerprint(
            browserType = browserType,
            userAgent = userAgent,
            screenParameters = ScreenParameters.MACBOOK_PRO_13,
            viewportParameters = ViewportParameters(
                width = 1280,
                height = 800,
                deviceScaleFactor = 2.0,
                isMobile = false,
                hasTouch = false,
                isLandscape = true
            ),
            geoTimeParameters = GeoTimeParameters.US_EAST,
            hardwareParameters = HardwareParameters.MAC_LAPTOP,
            webGLParameters = WebGLParameters.APPLE_M1,
            canvasParameters = CanvasParameters(
                fingerprintSeed = generateCanvasSeed()
            ),
            mediaParameters = MediaParameters(
                audioInputDevices = listOf(
                    MediaDevice("default", "Built-in Microphone", "audioinput")
                ),
                audioOutputDevices = listOf(
                    MediaDevice("default", "Built-in Output", "audiooutput")
                ),
                videoInputDevices = listOf(
                    MediaDevice("default", "FaceTime HD Camera", "videoinput")
                )
            ),
            miscParameters = MiscParameters.DEFAULT,
            version = 1
        )
    }
    
    private fun generateMacBookAir(browserType: BrowserType): Fingerprint {
        val userAgent = generateUserAgent(browserType, Platform.MAC, "10_15_7")
        
        return Fingerprint(
            browserType = browserType,
            userAgent = userAgent,
            screenParameters = ScreenParameters(
                width = 2560,
                height = 1600,
                availWidth = 2560,
                availHeight = 1577,
                colorDepth = 24,
                pixelDepth = 24,
                devicePixelRatio = 2.0,
                orientation = "landscape-primary"
            ),
            viewportParameters = ViewportParameters(
                width = 1280,
                height = 800,
                deviceScaleFactor = 2.0,
                isMobile = false,
                hasTouch = false,
                isLandscape = true
            ),
            geoTimeParameters = GeoTimeParameters.US_EAST,
            hardwareParameters = HardwareParameters(
                hardwareConcurrency = 8,
                deviceMemory = 8,
                maxTouchPoints = 0,
                platform = "MacIntel",
                vendor = "Apple Computer, Inc."
            ),
            webGLParameters = WebGLParameters.APPLE_M1,
            canvasParameters = CanvasParameters(
                fingerprintSeed = generateCanvasSeed()
            ),
            mediaParameters = MediaParameters(
                audioInputDevices = listOf(
                    MediaDevice("default", "Built-in Microphone", "audioinput")
                ),
                audioOutputDevices = listOf(
                    MediaDevice("default", "Built-in Output", "audiooutput")
                ),
                videoInputDevices = listOf(
                    MediaDevice("default", "FaceTime HD Camera", "videoinput")
                )
            ),
            miscParameters = MiscParameters.DEFAULT,
            version = 1
        )
    }
    
    private fun generateDesktopLinux(browserType: BrowserType): Fingerprint {
        val userAgent = generateUserAgent(browserType, Platform.LINUX, "x86_64")
        
        return Fingerprint(
            browserType = browserType,
            userAgent = userAgent,
            screenParameters = ScreenParameters.DESKTOP_1920X1080,
            viewportParameters = ViewportParameters.DESKTOP,
            geoTimeParameters = GeoTimeParameters.US_EAST,
            hardwareParameters = HardwareParameters.LINUX_DESKTOP,
            webGLParameters = WebGLParameters.INTEL_INTEGRATED,
            canvasParameters = CanvasParameters(
                fingerprintSeed = generateCanvasSeed()
            ),
            mediaParameters = MediaParameters.DESKTOP,
            miscParameters = MiscParameters.DEFAULT,
            version = 1
        )
    }
    
    private fun generateLaptopLinux(browserType: BrowserType): Fingerprint {
        val userAgent = generateUserAgent(browserType, Platform.LINUX, "x86_64")
        
        return Fingerprint(
            browserType = browserType,
            userAgent = userAgent,
            screenParameters = ScreenParameters.LAPTOP_1366X768,
            viewportParameters = ViewportParameters.LAPTOP,
            geoTimeParameters = GeoTimeParameters.US_EAST,
            hardwareParameters = HardwareParameters(
                hardwareConcurrency = 4,
                deviceMemory = 8,
                maxTouchPoints = 0,
                platform = "Linux x86_64",
                vendor = "Google Inc."
            ),
            webGLParameters = WebGLParameters.INTEL_INTEGRATED,
            canvasParameters = CanvasParameters(
                fingerprintSeed = generateCanvasSeed()
            ),
            mediaParameters = MediaParameters.DESKTOP,
            miscParameters = MiscParameters.DEFAULT,
            version = 1
        )
    }
    
    private fun generateUserAgent(
        browserType: BrowserType,
        platform: Platform,
        platformVersion: String
    ): String {
        val chromeVersion = DEFAULT_CHROME_VERSION
        
        return when (platform) {
            Platform.WINDOWS -> {
                "Mozilla/5.0 (Windows NT $platformVersion; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$chromeVersion Safari/537.36"
            }
            Platform.MAC -> {
                "Mozilla/5.0 (Macintosh; Intel Mac OS X $platformVersion) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$chromeVersion Safari/537.36"
            }
            Platform.LINUX -> {
                "Mozilla/5.0 (X11; Linux $platformVersion) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$chromeVersion Safari/537.36"
            }
        }
    }
    
    private fun generateCanvasSeed(): String {
        // Generate a deterministic seed based on timestamp and random
        val timestamp = System.currentTimeMillis()
        val random = Random.nextInt(100000, 999999)
        return "canvas-seed-$timestamp-$random"
    }
    
    /**
     * Device preset types for fingerprint generation.
     */
    enum class DevicePreset {
        DESKTOP_WINDOWS,
        LAPTOP_WINDOWS,
        MACBOOK_PRO_13,
        MACBOOK_AIR,
        DESKTOP_LINUX,
        LAPTOP_LINUX
    }
    
    /**
     * Platform types.
     */
    enum class Platform {
        WINDOWS,
        MAC,
        LINUX
    }

    companion object {
        /**
         * Default Chrome version used for user agent generation.
         * Shared across all fingerprint generator implementations.
         */
        const val DEFAULT_CHROME_VERSION = "120.0.0.0"
    }
}
