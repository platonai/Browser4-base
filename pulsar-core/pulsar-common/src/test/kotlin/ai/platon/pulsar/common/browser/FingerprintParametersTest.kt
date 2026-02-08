package ai.platon.pulsar.common.browser

import ai.platon.pulsar.common.serialize.json.prettyPulsarObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import kotlin.test.assertFailsWith

class FingerprintParametersTest {
    
    private val mapper = prettyPulsarObjectMapper()
    
    @Test
    @DisplayName("test screen parameters creation and validation")
    fun testScreenParametersCreation() {
        val screen = ScreenParameters(
            width = 1920,
            height = 1080,
            availWidth = 1920,
            availHeight = 1040,
            colorDepth = 24,
            pixelDepth = 24,
            devicePixelRatio = 1.0
        )
        
        assertEquals(1920, screen.width)
        assertEquals(1080, screen.height)
        assertEquals(24, screen.colorDepth)
        assertEquals(1.0, screen.devicePixelRatio)
    }
    
    @Test
    @DisplayName("test screen parameters validation - invalid width")
    fun testScreenParametersInvalidWidth() {
        assertFailsWith<IllegalArgumentException> {
            ScreenParameters(
                width = -1,
                height = 1080,
                availWidth = 1920,
                availHeight = 1040
            )
        }
    }
    
    @Test
    @DisplayName("test screen parameters validation - available exceeds actual")
    fun testScreenParametersAvailableExceedsActual() {
        assertFailsWith<IllegalArgumentException> {
            ScreenParameters(
                width = 1920,
                height = 1080,
                availWidth = 2000,  // exceeds width
                availHeight = 1040
            )
        }
    }
    
    @Test
    @DisplayName("test screen parameters presets")
    fun testScreenParametersPresets() {
        val desktop = ScreenParameters.DESKTOP_1920X1080
        assertEquals(1920, desktop.width)
        assertEquals(1080, desktop.height)
        
        val laptop = ScreenParameters.LAPTOP_1366X768
        assertEquals(1366, laptop.width)
        assertEquals(768, laptop.height)
        
        val macbook = ScreenParameters.MACBOOK_PRO_13
        assertEquals(2560, macbook.width)
        assertEquals(2.0, macbook.devicePixelRatio)
    }
    
    @Test
    @DisplayName("test screen parameters serialization")
    fun testScreenParametersSerialization() {
        val screen = ScreenParameters.DESKTOP_1920X1080
        val json = mapper.writeValueAsString(screen)
        val deserialized = mapper.readValue(json, ScreenParameters::class.java)
        
        assertEquals(screen, deserialized)
    }
    
    @Test
    @DisplayName("test viewport parameters creation")
    fun testViewportParametersCreation() {
        val viewport = ViewportParameters(
            width = 1920,
            height = 1080,
            deviceScaleFactor = 1.0,
            isMobile = false,
            hasTouch = false
        )
        
        assertEquals(1920, viewport.width)
        assertEquals(1080, viewport.height)
        assertFalse(viewport.isMobile)
    }
    
    @Test
    @DisplayName("test viewport parameters presets")
    fun testViewportParametersPresets() {
        val desktop = ViewportParameters.DESKTOP
        assertFalse(desktop.isMobile)
        assertFalse(desktop.hasTouch)
        
        val laptop = ViewportParameters.LAPTOP
        assertEquals(1366, laptop.width)
    }
    
    @Test
    @DisplayName("test geo-time parameters creation")
    fun testGeoTimeParametersCreation() {
        val geoTime = GeoTimeParameters(
            timezone = "Asia/Shanghai",
            timezoneOffset = -480,
            locale = "zh-CN",
            languages = listOf("zh-CN", "zh", "en")
        )
        
        assertEquals("Asia/Shanghai", geoTime.timezone)
        assertEquals(-480, geoTime.timezoneOffset)
        assertEquals("zh-CN", geoTime.locale)
        assertEquals(3, geoTime.languages.size)
    }
    
    @Test
    @DisplayName("test geo-time parameters validation - blank timezone")
    fun testGeoTimeParametersBlankTimezone() {
        assertFailsWith<IllegalArgumentException> {
            GeoTimeParameters(
                timezone = "",
                timezoneOffset = 0,
                locale = "en-US",
                languages = listOf("en")
            )
        }
    }
    
    @Test
    @DisplayName("test geo-time parameters validation - empty languages")
    fun testGeoTimeParametersEmptyLanguages() {
        assertFailsWith<IllegalArgumentException> {
            GeoTimeParameters(
                timezone = "America/New_York",
                timezoneOffset = 0,
                locale = "en-US",
                languages = emptyList()
            )
        }
    }
    
    @Test
    @DisplayName("test geo-time parameters validation - invalid coordinates")
    fun testGeoTimeParametersInvalidCoordinates() {
        assertFailsWith<IllegalArgumentException> {
            GeoTimeParameters(
                timezone = "America/New_York",
                timezoneOffset = 0,
                locale = "en-US",
                languages = listOf("en"),
                latitude = 91.0  // out of range
            )
        }
    }
    
    @Test
    @DisplayName("test geo-time parameters presets")
    fun testGeoTimeParametersPresets() {
        val china = GeoTimeParameters.CHINA
        assertEquals("Asia/Shanghai", china.timezone)
        assertEquals("zh-CN", china.locale)
        
        val usEast = GeoTimeParameters.US_EAST
        assertEquals("America/New_York", usEast.timezone)
        
        val uk = GeoTimeParameters.UK
        assertEquals("Europe/London", uk.timezone)
    }
    
    @Test
    @DisplayName("test hardware parameters creation")
    fun testHardwareParametersCreation() {
        val hardware = HardwareParameters(
            hardwareConcurrency = 8,
            deviceMemory = 16,
            maxTouchPoints = 0,
            platform = "Win32"
        )
        
        assertEquals(8, hardware.hardwareConcurrency)
        assertEquals(16, hardware.deviceMemory)
        assertEquals("Win32", hardware.platform)
    }
    
    @Test
    @DisplayName("test hardware parameters validation - invalid concurrency")
    fun testHardwareParametersInvalidConcurrency() {
        assertFailsWith<IllegalArgumentException> {
            HardwareParameters(
                hardwareConcurrency = 0,
                platform = "Win32"
            )
        }
    }
    
    @Test
    @DisplayName("test hardware parameters presets")
    fun testHardwareParametersPresets() {
        val windows = HardwareParameters.WINDOWS_DESKTOP
        assertEquals("Win32", windows.platform)
        assertEquals(8, windows.hardwareConcurrency)
        
        val mac = HardwareParameters.MAC_LAPTOP
        assertEquals("MacIntel", mac.platform)
        
        val linux = HardwareParameters.LINUX_DESKTOP
        assertEquals("Linux x86_64", linux.platform)
    }
    
    @Test
    @DisplayName("test WebGL parameters creation")
    fun testWebGLParametersCreation() {
        val webgl = WebGLParameters(
            vendor = "Google Inc. (Intel)",
            renderer = "ANGLE (Intel, Intel(R) UHD Graphics)"
        )
        
        assertEquals("Google Inc. (Intel)", webgl.vendor)
        assertTrue(webgl.renderer.contains("Intel"))
    }
    
    @Test
    @DisplayName("test WebGL parameters presets")
    fun testWebGLParametersPresets() {
        val intel = WebGLParameters.INTEL_INTEGRATED
        assertTrue(intel.vendor.contains("Intel"))
        
        val nvidia = WebGLParameters.NVIDIA_DISCRETE
        assertTrue(nvidia.vendor.contains("NVIDIA"))
        
        val apple = WebGLParameters.APPLE_M1
        assertTrue(apple.vendor.contains("Apple"))
    }
    
    @Test
    @DisplayName("test canvas parameters")
    fun testCanvasParameters() {
        val canvas1 = CanvasParameters(fingerprintSeed = "test-seed")
        assertEquals("test-seed", canvas1.fingerprintSeed)
        
        val canvas2 = CanvasParameters.DEFAULT
        assertNull(canvas2.fingerprintSeed)
    }
    
    @Test
    @DisplayName("test media device creation")
    fun testMediaDeviceCreation() {
        val device = MediaDevice(
            deviceId = "default",
            label = "Default Microphone",
            kind = "audioinput"
        )
        
        assertEquals("default", device.deviceId)
        assertEquals("audioinput", device.kind)
    }
    
    @Test
    @DisplayName("test media device validation - invalid kind")
    fun testMediaDeviceInvalidKind() {
        assertFailsWith<IllegalArgumentException> {
            MediaDevice(
                deviceId = "default",
                label = "Test",
                kind = "invalid"
            )
        }
    }
    
    @Test
    @DisplayName("test media parameters creation")
    fun testMediaParametersCreation() {
        val media = MediaParameters(
            audioInputDevices = listOf(
                MediaDevice("mic1", "Microphone", "audioinput")
            ),
            audioOutputDevices = listOf(
                MediaDevice("speaker1", "Speakers", "audiooutput")
            )
        )
        
        assertEquals(1, media.audioInputDevices.size)
        assertEquals(1, media.audioOutputDevices.size)
    }
    
    @Test
    @DisplayName("test media parameters preset")
    fun testMediaParametersPreset() {
        val desktop = MediaParameters.DESKTOP
        assertTrue(desktop.audioInputDevices.isNotEmpty())
        assertTrue(desktop.audioOutputDevices.isNotEmpty())
    }
    
    @Test
    @DisplayName("test misc parameters creation")
    fun testMiscParametersCreation() {
        val misc = MiscParameters(
            doNotTrack = "1",
            cookieEnabled = true,
            pdfViewerEnabled = true
        )
        
        assertEquals("1", misc.doNotTrack)
        assertTrue(misc.cookieEnabled)
    }
    
    @Test
    @DisplayName("test misc parameters default")
    fun testMiscParametersDefault() {
        val misc = MiscParameters.DEFAULT
        assertNull(misc.doNotTrack)
        assertTrue(misc.cookieEnabled)
    }
    
    @Test
    @DisplayName("test all parameters serialization")
    fun testAllParametersSerialization() {
        val screen = ScreenParameters.DESKTOP_1920X1080
        val viewport = ViewportParameters.DESKTOP
        val geoTime = GeoTimeParameters.CHINA
        val hardware = HardwareParameters.WINDOWS_DESKTOP
        val webgl = WebGLParameters.INTEL_INTEGRATED
        val canvas = CanvasParameters.DEFAULT
        val media = MediaParameters.DESKTOP
        val misc = MiscParameters.DEFAULT
        
        // Test each parameter type can be serialized and deserialized
        listOf(
            screen to ScreenParameters::class.java,
            viewport to ViewportParameters::class.java,
            geoTime to GeoTimeParameters::class.java,
            hardware to HardwareParameters::class.java,
            webgl to WebGLParameters::class.java,
            canvas to CanvasParameters::class.java,
            media to MediaParameters::class.java,
            misc to MiscParameters::class.java
        ).forEach { (obj, clazz) ->
            val json = mapper.writeValueAsString(obj)
            val deserialized = mapper.readValue(json, clazz)
            assertEquals(obj, deserialized)
        }
    }
}
