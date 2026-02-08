package ai.platon.pulsar.plugins.fingerprint.pro

import ai.platon.pulsar.common.browser.*
import kotlin.random.Random

/**
 * Professional fingerprint generator plugin that provides comprehensive, realistic
 * browser fingerprints with full anti-detection parameter coverage.
 *
 * This generator produces fingerprints with all parameter groups populated:
 * screen, viewport, geo-time, hardware, WebGL, canvas, media devices, and miscellaneous.
 * All parameters are internally consistent and validated.
 *
 * This plugin is loaded dynamically via Java ServiceLoader when professional fingerprinting
 * is enabled.
 */
class ProFingerprintGenerator : FingerprintGeneratorProvider {

    override val name: String = "pro"

    private val validator = FingerprintValidator()

    override fun generate(
        browserType: BrowserType,
        preset: FingerprintGenerator.DevicePreset
    ): Fingerprint {
        val fingerprint = when (preset) {
            FingerprintGenerator.DevicePreset.DESKTOP_WINDOWS -> generateDesktopWindows(browserType)
            FingerprintGenerator.DevicePreset.LAPTOP_WINDOWS -> generateLaptopWindows(browserType)
            FingerprintGenerator.DevicePreset.MACBOOK_PRO_13 -> generateMacBookPro13(browserType)
            FingerprintGenerator.DevicePreset.MACBOOK_AIR -> generateMacBookAir(browserType)
            FingerprintGenerator.DevicePreset.DESKTOP_LINUX -> generateDesktopLinux(browserType)
            FingerprintGenerator.DevicePreset.LAPTOP_LINUX -> generateLaptopLinux(browserType)
        }

        val result = validator.validate(fingerprint)
        if (!result.isValid) {
            throw IllegalStateException("Generated fingerprint failed validation: $result")
        }

        return fingerprint
    }

    override fun generateRandom(
        browserType: BrowserType,
        platform: FingerprintGenerator.Platform
    ): Fingerprint {
        val preset = when (platform) {
            FingerprintGenerator.Platform.WINDOWS -> listOf(
                FingerprintGenerator.DevicePreset.DESKTOP_WINDOWS,
                FingerprintGenerator.DevicePreset.LAPTOP_WINDOWS
            ).random()
            FingerprintGenerator.Platform.MAC -> listOf(
                FingerprintGenerator.DevicePreset.MACBOOK_PRO_13,
                FingerprintGenerator.DevicePreset.MACBOOK_AIR
            ).random()
            FingerprintGenerator.Platform.LINUX -> listOf(
                FingerprintGenerator.DevicePreset.DESKTOP_LINUX,
                FingerprintGenerator.DevicePreset.LAPTOP_LINUX
            ).random()
        }
        return generate(browserType, preset)
    }

    private fun generateDesktopWindows(browserType: BrowserType): Fingerprint {
        return Fingerprint(
            browserType = browserType,
            userAgent = generateUserAgent(FingerprintGenerator.Platform.WINDOWS, "10.0"),
            screenParameters = ScreenParameters.DESKTOP_1920X1080,
            viewportParameters = ViewportParameters.DESKTOP,
            geoTimeParameters = GeoTimeParameters.US_EAST,
            hardwareParameters = HardwareParameters.WINDOWS_DESKTOP,
            webGLParameters = WebGLParameters.INTEL_INTEGRATED,
            canvasParameters = CanvasParameters(fingerprintSeed = generateCanvasSeed()),
            mediaParameters = MediaParameters.DESKTOP,
            miscParameters = MiscParameters.DEFAULT,
            version = 1
        )
    }

    private fun generateLaptopWindows(browserType: BrowserType): Fingerprint {
        return Fingerprint(
            browserType = browserType,
            userAgent = generateUserAgent(FingerprintGenerator.Platform.WINDOWS, "10.0"),
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
            canvasParameters = CanvasParameters(fingerprintSeed = generateCanvasSeed()),
            mediaParameters = MediaParameters.DESKTOP,
            miscParameters = MiscParameters.DEFAULT,
            version = 1
        )
    }

    private fun generateMacBookPro13(browserType: BrowserType): Fingerprint {
        return Fingerprint(
            browserType = browserType,
            userAgent = generateUserAgent(FingerprintGenerator.Platform.MAC, "10_15_7"),
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
            canvasParameters = CanvasParameters(fingerprintSeed = generateCanvasSeed()),
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
        return Fingerprint(
            browserType = browserType,
            userAgent = generateUserAgent(FingerprintGenerator.Platform.MAC, "10_15_7"),
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
            canvasParameters = CanvasParameters(fingerprintSeed = generateCanvasSeed()),
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
        return Fingerprint(
            browserType = browserType,
            userAgent = generateUserAgent(FingerprintGenerator.Platform.LINUX, "x86_64"),
            screenParameters = ScreenParameters.DESKTOP_1920X1080,
            viewportParameters = ViewportParameters.DESKTOP,
            geoTimeParameters = GeoTimeParameters.US_EAST,
            hardwareParameters = HardwareParameters.LINUX_DESKTOP,
            webGLParameters = WebGLParameters.INTEL_INTEGRATED,
            canvasParameters = CanvasParameters(fingerprintSeed = generateCanvasSeed()),
            mediaParameters = MediaParameters.DESKTOP,
            miscParameters = MiscParameters.DEFAULT,
            version = 1
        )
    }

    private fun generateLaptopLinux(browserType: BrowserType): Fingerprint {
        return Fingerprint(
            browserType = browserType,
            userAgent = generateUserAgent(FingerprintGenerator.Platform.LINUX, "x86_64"),
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
            canvasParameters = CanvasParameters(fingerprintSeed = generateCanvasSeed()),
            mediaParameters = MediaParameters.DESKTOP,
            miscParameters = MiscParameters.DEFAULT,
            version = 1
        )
    }

    private fun generateUserAgent(platform: FingerprintGenerator.Platform, platformVersion: String): String {
        val chromeVersion = FingerprintGenerator.DEFAULT_CHROME_VERSION
        return when (platform) {
            FingerprintGenerator.Platform.WINDOWS ->
                "Mozilla/5.0 (Windows NT $platformVersion; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$chromeVersion Safari/537.36"
            FingerprintGenerator.Platform.MAC ->
                "Mozilla/5.0 (Macintosh; Intel Mac OS X $platformVersion) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$chromeVersion Safari/537.36"
            FingerprintGenerator.Platform.LINUX ->
                "Mozilla/5.0 (X11; Linux $platformVersion) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$chromeVersion Safari/537.36"
        }
    }

    private fun generateCanvasSeed(): String {
        val timestamp = System.currentTimeMillis()
        val random = Random.nextInt(100000, 999999)
        return "canvas-seed-$timestamp-$random"
    }
}
