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
        return when (preset) {
            FingerprintGenerator.DevicePreset.DESKTOP_WINDOWS -> generateDesktopWindows(browserType)
            FingerprintGenerator.DevicePreset.LAPTOP_WINDOWS -> generateLaptopWindows(browserType)
            FingerprintGenerator.DevicePreset.MACBOOK_PRO_13 -> generateMacBookPro13(browserType)
            FingerprintGenerator.DevicePreset.MACBOOK_AIR -> generateMacBookAir(browserType)
            FingerprintGenerator.DevicePreset.DESKTOP_LINUX -> generateDesktopLinux(browserType)
            FingerprintGenerator.DevicePreset.LAPTOP_LINUX -> generateLaptopLinux(browserType)
        }
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
            version = 1
        )
    }

    private fun generateLaptopWindows(browserType: BrowserType): Fingerprint {
        return Fingerprint(
            browserType = browserType,
            userAgent = generateUserAgent(FingerprintGenerator.Platform.WINDOWS, "10.0"),
            screenParameters = ScreenParameters.LAPTOP_1366X768,
            viewportParameters = ViewportParameters.LAPTOP,
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
            version = 1
        )
    }

    private fun generateDesktopLinux(browserType: BrowserType): Fingerprint {
        return Fingerprint(
            browserType = browserType,
            userAgent = generateUserAgent(FingerprintGenerator.Platform.LINUX, "x86_64"),
            screenParameters = ScreenParameters.DESKTOP_1920X1080,
            viewportParameters = ViewportParameters.DESKTOP,
            version = 1
        )
    }

    private fun generateLaptopLinux(browserType: BrowserType): Fingerprint {
        return Fingerprint(
            browserType = browserType,
            userAgent = generateUserAgent(FingerprintGenerator.Platform.LINUX, "x86_64"),
            screenParameters = ScreenParameters.LAPTOP_1366X768,
            viewportParameters = ViewportParameters.LAPTOP,
            version = 1
        )
    }

    private fun generateUserAgent(platform: FingerprintGenerator.Platform, platformVersion: String): String {
        val chromeVersion = "120.0.0.0"
        return when (platform) {
            FingerprintGenerator.Platform.WINDOWS ->
                "Mozilla/5.0 (Windows NT $platformVersion; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$chromeVersion Safari/537.36"
            FingerprintGenerator.Platform.MAC ->
                "Mozilla/5.0 (Macintosh; Intel Mac OS X $platformVersion) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$chromeVersion Safari/537.36"
            FingerprintGenerator.Platform.LINUX ->
                "Mozilla/5.0 (X11; Linux $platformVersion) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$chromeVersion Safari/537.36"
        }
    }
}
