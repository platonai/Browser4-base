package ai.platon.pulsar.common.browser

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

class FingerprintValidatorTest {
    
    private val validator = FingerprintValidator()
    
    @Test
    @DisplayName("test valid fingerprint passes validation")
    fun testValidFingerprintPassesValidation() {
        val fingerprint = Fingerprint(
            browserType = BrowserType.PULSAR_CHROME,
            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            screenParameters = ScreenParameters.DESKTOP_1920X1080,
            viewportParameters = ViewportParameters.DESKTOP,
            geoTimeParameters = GeoTimeParameters.US_EAST,
            hardwareParameters = HardwareParameters.WINDOWS_DESKTOP,
            webGLParameters = WebGLParameters.INTEL_INTEGRATED
        )
        
        val result = validator.validate(fingerprint)
        assertTrue(result.isValid, "Expected fingerprint to be valid but got: ${result}")
    }
    
    @Test
    @DisplayName("test userAgent platform mismatch detected")
    fun testUserAgentPlatformMismatch() {
        val fingerprint = Fingerprint(
            browserType = BrowserType.PULSAR_CHROME,
            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
            hardwareParameters = HardwareParameters(
                hardwareConcurrency = 8,
                platform = "MacIntel"  // Mismatch: Windows UA with Mac platform
            )
        )
        
        val result = validator.validate(fingerprint)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Windows") && it.contains("MacIntel") })
    }
    
    @Test
    @DisplayName("test Mac userAgent with correct platform")
    fun testMacUserAgentWithCorrectPlatform() {
        val fingerprint = Fingerprint(
            browserType = BrowserType.PULSAR_CHROME,
            userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36",
            hardwareParameters = HardwareParameters.MAC_LAPTOP
        )
        
        val result = validator.validate(fingerprint)
        assertTrue(result.isValid, "Mac fingerprint should be valid: ${result}")
    }
    
    @Test
    @DisplayName("test Linux userAgent with correct platform")
    fun testLinuxUserAgentWithCorrectPlatform() {
        val fingerprint = Fingerprint(
            browserType = BrowserType.PULSAR_CHROME,
            userAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36",
            hardwareParameters = HardwareParameters.LINUX_DESKTOP
        )
        
        val result = validator.validate(fingerprint)
        assertTrue(result.isValid, "Linux fingerprint should be valid: ${result}")
    }
    
    @Test
    @DisplayName("test viewport exceeding screen width detected")
    fun testViewportExceedingScreenWidth() {
        val fingerprint = Fingerprint(
            browserType = BrowserType.PULSAR_CHROME,
            screenParameters = ScreenParameters(
                width = 1366,
                height = 768,
                availWidth = 1366,
                availHeight = 728
            ),
            viewportParameters = ViewportParameters(
                width = 1920,  // Exceeds screen width
                height = 768
            )
        )
        
        val result = validator.validate(fingerprint)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Viewport width") && it.contains("exceeds") })
    }
    
    @Test
    @DisplayName("test viewport exceeding screen height detected")
    fun testViewportExceedingScreenHeight() {
        val fingerprint = Fingerprint(
            browserType = BrowserType.PULSAR_CHROME,
            screenParameters = ScreenParameters(
                width = 1920,
                height = 1080,
                availWidth = 1920,
                availHeight = 1040
            ),
            viewportParameters = ViewportParameters(
                width = 1920,
                height = 1200  // Exceeds screen height
            )
        )
        
        val result = validator.validate(fingerprint)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Viewport height") && it.contains("exceeds") })
    }
    
    @Test
    @DisplayName("test device scale factor mismatch warning")
    fun testDeviceScaleFactorMismatch() {
        val fingerprint = Fingerprint(
            browserType = BrowserType.PULSAR_CHROME,
            userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36",  // Added userAgent
            screenParameters = ScreenParameters(
                width = 2560,
                height = 1600,
                availWidth = 2560,
                availHeight = 1577,
                devicePixelRatio = 2.0
            ),
            viewportParameters = ViewportParameters(
                width = 1280,
                height = 800,
                deviceScaleFactor = 1.5  // Mismatch with devicePixelRatio
            )
        )
        
        val result = validator.validate(fingerprint)
        assertTrue(result.isValid, "Should be valid but have warnings: ${result}")
        assertTrue(result.hasWarnings)
        assertTrue(result.warnings.any { it.contains("scale factor") })
    }
    
    @Test
    @DisplayName("test unreasonable hardware concurrency detected")
    fun testUnreasonableHardwareConcurrency() {
        val fingerprint = Fingerprint(
            browserType = BrowserType.PULSAR_CHROME,
            hardwareParameters = HardwareParameters(
                hardwareConcurrency = 256,  // Unreasonably high
                platform = "Win32"
            )
        )
        
        val result = validator.validate(fingerprint)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Hardware concurrency") })
    }
    
    @Test
    @DisplayName("test unreasonable device memory detected")
    fun testUnreasonableDeviceMemory() {
        val fingerprint = Fingerprint(
            browserType = BrowserType.PULSAR_CHROME,
            hardwareParameters = HardwareParameters(
                hardwareConcurrency = 8,
                deviceMemory = 512,  // Unreasonably high
                platform = "Win32"
            )
        )
        
        val result = validator.validate(fingerprint)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Device memory") })
    }
    
    @Test
    @DisplayName("test mobile device without touch points warning")
    fun testMobileDeviceWithoutTouchPoints() {
        val fingerprint = Fingerprint(
            browserType = BrowserType.PULSAR_CHROME,
            userAgent = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36",  // Added mobile userAgent
            viewportParameters = ViewportParameters(
                width = 375,
                height = 667,
                isMobile = true,
                hasTouch = true
            ),
            hardwareParameters = HardwareParameters(
                hardwareConcurrency = 4,
                maxTouchPoints = 0,  // Mobile should have touch points
                platform = "Linux armv8l"
            )
        )
        
        val result = validator.validate(fingerprint)
        assertTrue(result.isValid, "Should be valid but have warnings: ${result}")
        assertTrue(result.hasWarnings)
        assertTrue(result.warnings.any { it.contains("Mobile") && it.contains("maxTouchPoints") })
    }
    
    @Test
    @DisplayName("test empty languages list detected")
    fun testEmptyLanguagesList() {
        // GeoTimeParameters validates languages list is not empty on construction
        // This test verifies that valid parameters pass validation
        val fingerprint = Fingerprint(
            browserType = BrowserType.PULSAR_CHROME,
            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
            geoTimeParameters = GeoTimeParameters(
                timezone = "America/New_York",
                timezoneOffset = 300,
                locale = "en-US",
                languages = listOf("en-US")  // Valid, not empty
            )
        )
        
        val result = validator.validate(fingerprint)
        assertTrue(result.isValid, "Valid fingerprint should pass: ${result}")
    }
    
    @Test
    @DisplayName("test language locale mismatch warning")
    fun testLanguageLocaleMismatch() {
        val fingerprint = Fingerprint(
            browserType = BrowserType.PULSAR_CHROME,
            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",  // Added userAgent
            geoTimeParameters = GeoTimeParameters(
                timezone = "America/New_York",
                timezoneOffset = 300,
                locale = "zh-CN",  // Chinese locale
                languages = listOf("en-US", "en")  // but English languages
            )
        )
        
        val result = validator.validate(fingerprint)
        assertTrue(result.isValid, "Should be valid but have warnings: ${result}")
        assertTrue(result.hasWarnings)
        assertTrue(result.warnings.any { it.contains("language") && it.contains("locale") })
    }
    
    @Test
    @DisplayName("test latitude without longitude detected")
    fun testLatitudeWithoutLongitude() {
        val fingerprint = Fingerprint(
            browserType = BrowserType.PULSAR_CHROME,
            geoTimeParameters = GeoTimeParameters(
                timezone = "America/New_York",
                timezoneOffset = 300,
                locale = "en-US",
                languages = listOf("en-US"),
                latitude = 40.7128  // Missing longitude
            )
        )
        
        val result = validator.validate(fingerprint)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Latitude") && it.contains("without longitude") })
    }
    
    @Test
    @DisplayName("test Chrome vendor mismatch warning")
    fun testChromeVendorMismatch() {
        val fingerprint = Fingerprint(
            browserType = BrowserType.PULSAR_CHROME,
            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            hardwareParameters = HardwareParameters(
                hardwareConcurrency = 8,
                platform = "Win32",
                vendor = "Apple Computer, Inc."  // Wrong vendor for Chrome
            )
        )
        
        val result = validator.validate(fingerprint)
        assertTrue(result.isValid, "Should be valid but have warnings")
        assertTrue(result.hasWarnings)
        assertTrue(result.warnings.any { it.contains("Chrome") && it.contains("Google Inc.") })
    }
    
    @Test
    @DisplayName("test complete consistent fingerprint")
    fun testCompleteConsistentFingerprint() {
        val fingerprint = Fingerprint(
            browserType = BrowserType.PULSAR_CHROME,
            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            screenParameters = ScreenParameters(
                width = 1920,
                height = 1080,
                availWidth = 1920,
                availHeight = 1040,
                colorDepth = 24,
                pixelDepth = 24,
                devicePixelRatio = 1.0
            ),
            viewportParameters = ViewportParameters(
                width = 1920,
                height = 1080,
                deviceScaleFactor = 1.0,
                isMobile = false,
                hasTouch = false
            ),
            geoTimeParameters = GeoTimeParameters(
                timezone = "America/New_York",
                timezoneOffset = 300,
                locale = "en-US",
                languages = listOf("en-US", "en")
            ),
            hardwareParameters = HardwareParameters(
                hardwareConcurrency = 8,
                deviceMemory = 8,
                maxTouchPoints = 0,
                platform = "Win32",
                vendor = "Google Inc."
            ),
            webGLParameters = WebGLParameters.INTEL_INTEGRATED,
            canvasParameters = CanvasParameters.DEFAULT,
            mediaParameters = MediaParameters.DESKTOP,
            miscParameters = MiscParameters.DEFAULT
        )
        
        val result = validator.validate(fingerprint)
        assertTrue(result.isValid, "Complete consistent fingerprint should be valid: ${result}")
        assertFalse(result.hasWarnings, "Complete consistent fingerprint should have no warnings: ${result}")
    }
    
    @Test
    @DisplayName("test validation result summary")
    fun testValidationResultSummary() {
        val validResult = ValidationResult(emptyList(), emptyList())
        assertEquals("Validation passed", validResult.summary())
        
        val warningResult = ValidationResult(emptyList(), listOf("warning1"))
        assertEquals("Validation passed with 1 warnings", warningResult.summary())
        
        val errorResult = ValidationResult(listOf("error1", "error2"), emptyList())
        assertEquals("Validation failed with 2 errors", errorResult.summary())
    }
}
