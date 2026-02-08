package ai.platon.pulsar.common.browser

import ai.platon.pulsar.common.serialize.json.prettyPulsarObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

class FingerprintGeneratorTest {
    
    private val generator = FingerprintGenerator()
    private val validator = FingerprintValidator()
    private val mapper = prettyPulsarObjectMapper()
    
    @Test
    @DisplayName("test generate desktop Windows fingerprint")
    fun testGenerateDesktopWindows() {
        val fingerprint = generator.generate(
            BrowserType.PULSAR_CHROME,
            FingerprintGenerator.DevicePreset.DESKTOP_WINDOWS
        )
        
        assertNotNull(fingerprint.userAgent)
        assertTrue(fingerprint.userAgent!!.contains("Windows"))
        assertEquals("Win32", fingerprint.hardwareParameters?.platform)
        assertEquals(1920, fingerprint.screenParameters?.width)
        
        // Validate consistency
        val result = validator.validate(fingerprint)
        assertTrue(result.isValid, "Generated fingerprint should be valid: $result")
    }
    
    @Test
    @DisplayName("test generate laptop Windows fingerprint")
    fun testGenerateLaptopWindows() {
        val fingerprint = generator.generate(
            BrowserType.PULSAR_CHROME,
            FingerprintGenerator.DevicePreset.LAPTOP_WINDOWS
        )
        
        assertNotNull(fingerprint.userAgent)
        assertTrue(fingerprint.userAgent!!.contains("Windows"))
        assertEquals("Win32", fingerprint.hardwareParameters?.platform)
        assertEquals(1366, fingerprint.screenParameters?.width)
        
        // Validate consistency
        val result = validator.validate(fingerprint)
        assertTrue(result.isValid, "Generated fingerprint should be valid: $result")
    }
    
    @Test
    @DisplayName("test generate MacBook Pro 13 fingerprint")
    fun testGenerateMacBookPro13() {
        val fingerprint = generator.generate(
            BrowserType.PULSAR_CHROME,
            FingerprintGenerator.DevicePreset.MACBOOK_PRO_13
        )
        
        assertNotNull(fingerprint.userAgent)
        assertTrue(fingerprint.userAgent!!.contains("Macintosh"))
        assertEquals("MacIntel", fingerprint.hardwareParameters?.platform)
        assertEquals(2560, fingerprint.screenParameters?.width)
        assertEquals(2.0, fingerprint.screenParameters?.devicePixelRatio)
        
        // Check WebGL is Apple
        assertTrue(fingerprint.webGLParameters?.vendor?.contains("Apple") ?: false)
        
        // Validate consistency
        val result = validator.validate(fingerprint)
        assertTrue(result.isValid, "Generated fingerprint should be valid: $result")
    }
    
    @Test
    @DisplayName("test generate MacBook Air fingerprint")
    fun testGenerateMacBookAir() {
        val fingerprint = generator.generate(
            BrowserType.PULSAR_CHROME,
            FingerprintGenerator.DevicePreset.MACBOOK_AIR
        )
        
        assertNotNull(fingerprint.userAgent)
        assertTrue(fingerprint.userAgent!!.contains("Macintosh"))
        assertEquals("MacIntel", fingerprint.hardwareParameters?.platform)
        
        // Validate consistency
        val result = validator.validate(fingerprint)
        assertTrue(result.isValid, "Generated fingerprint should be valid: $result")
    }
    
    @Test
    @DisplayName("test generate desktop Linux fingerprint")
    fun testGenerateDesktopLinux() {
        val fingerprint = generator.generate(
            BrowserType.PULSAR_CHROME,
            FingerprintGenerator.DevicePreset.DESKTOP_LINUX
        )
        
        assertNotNull(fingerprint.userAgent)
        assertTrue(fingerprint.userAgent!!.contains("Linux"))
        assertEquals("Linux x86_64", fingerprint.hardwareParameters?.platform)
        
        // Validate consistency
        val result = validator.validate(fingerprint)
        assertTrue(result.isValid, "Generated fingerprint should be valid: $result")
    }
    
    @Test
    @DisplayName("test generate laptop Linux fingerprint")
    fun testGenerateLaptopLinux() {
        val fingerprint = generator.generate(
            BrowserType.PULSAR_CHROME,
            FingerprintGenerator.DevicePreset.LAPTOP_LINUX
        )
        
        assertNotNull(fingerprint.userAgent)
        assertTrue(fingerprint.userAgent!!.contains("Linux"))
        assertEquals("Linux x86_64", fingerprint.hardwareParameters?.platform)
        
        // Validate consistency
        val result = validator.validate(fingerprint)
        assertTrue(result.isValid, "Generated fingerprint should be valid: $result")
    }
    
    @Test
    @DisplayName("test generate random Windows fingerprint")
    fun testGenerateRandomWindows() {
        val fingerprint = generator.generateRandom(
            BrowserType.PULSAR_CHROME,
            FingerprintGenerator.Platform.WINDOWS
        )
        
        assertNotNull(fingerprint.userAgent)
        assertTrue(fingerprint.userAgent!!.contains("Windows"))
        assertEquals("Win32", fingerprint.hardwareParameters?.platform)
        
        // Validate consistency
        val result = validator.validate(fingerprint)
        assertTrue(result.isValid, "Generated fingerprint should be valid: $result")
    }
    
    @Test
    @DisplayName("test generate random Mac fingerprint")
    fun testGenerateRandomMac() {
        val fingerprint = generator.generateRandom(
            BrowserType.PULSAR_CHROME,
            FingerprintGenerator.Platform.MAC
        )
        
        assertNotNull(fingerprint.userAgent)
        assertTrue(fingerprint.userAgent!!.contains("Macintosh"))
        assertEquals("MacIntel", fingerprint.hardwareParameters?.platform)
        
        // Validate consistency
        val result = validator.validate(fingerprint)
        assertTrue(result.isValid, "Generated fingerprint should be valid: $result")
    }
    
    @Test
    @DisplayName("test generate random Linux fingerprint")
    fun testGenerateRandomLinux() {
        val fingerprint = generator.generateRandom(
            BrowserType.PULSAR_CHROME,
            FingerprintGenerator.Platform.LINUX
        )
        
        assertNotNull(fingerprint.userAgent)
        assertTrue(fingerprint.userAgent!!.contains("Linux"))
        assertTrue(fingerprint.hardwareParameters?.platform?.startsWith("Linux") ?: false)
        
        // Validate consistency
        val result = validator.validate(fingerprint)
        assertTrue(result.isValid, "Generated fingerprint should be valid: $result")
    }
    
    @Test
    @DisplayName("test all generated fingerprints have version")
    fun testAllGeneratedFingerprintsHaveVersion() {
        val presets = FingerprintGenerator.DevicePreset.values()
        
        presets.forEach { preset ->
            val fingerprint = generator.generate(BrowserType.PULSAR_CHROME, preset)
            assertEquals(1, fingerprint.version, "Fingerprint should have version 1")
        }
    }
    
    @Test
    @DisplayName("test all generated fingerprints are serializable")
    fun testAllGeneratedFingerprintsAreSerializable() {
        val presets = FingerprintGenerator.DevicePreset.values()
        
        presets.forEach { preset ->
            val fingerprint = generator.generate(BrowserType.PULSAR_CHROME, preset)
            
            // Serialize and deserialize
            val json = mapper.writeValueAsString(fingerprint)
            val deserialized = mapper.readValue(json, Fingerprint::class.java)
            
            // Check key fields match
            assertEquals(fingerprint.browserType, deserialized.browserType)
            assertEquals(fingerprint.userAgent, deserialized.userAgent)
            assertEquals(fingerprint.version, deserialized.version)
        }
    }
    
    @Test
    @DisplayName("test generated fingerprints have unique canvas seeds")
    fun testGeneratedFingerprintsHaveUniqueCanvasSeeds() {
        val fingerprint1 = generator.generate(
            BrowserType.PULSAR_CHROME,
            FingerprintGenerator.DevicePreset.DESKTOP_WINDOWS
        )
        
        // Wait a tiny bit to ensure different timestamp
        Thread.sleep(10)
        
        val fingerprint2 = generator.generate(
            BrowserType.PULSAR_CHROME,
            FingerprintGenerator.DevicePreset.DESKTOP_WINDOWS
        )
        
        assertNotNull(fingerprint1.canvasParameters?.fingerprintSeed)
        assertNotNull(fingerprint2.canvasParameters?.fingerprintSeed)
        assertNotEquals(
            fingerprint1.canvasParameters?.fingerprintSeed,
            fingerprint2.canvasParameters?.fingerprintSeed,
            "Canvas seeds should be unique"
        )
    }
    
    @Test
    @DisplayName("test generated fingerprints have all required parameters")
    fun testGeneratedFingerprintsHaveAllRequiredParameters() {
        val fingerprint = generator.generate(
            BrowserType.PULSAR_CHROME,
            FingerprintGenerator.DevicePreset.DESKTOP_WINDOWS
        )
        
        assertNotNull(fingerprint.userAgent, "User agent should be set")
        assertNotNull(fingerprint.screenParameters, "Screen parameters should be set")
        assertNotNull(fingerprint.viewportParameters, "Viewport parameters should be set")
        assertNotNull(fingerprint.geoTimeParameters, "Geo-time parameters should be set")
        assertNotNull(fingerprint.hardwareParameters, "Hardware parameters should be set")
        assertNotNull(fingerprint.webGLParameters, "WebGL parameters should be set")
        assertNotNull(fingerprint.canvasParameters, "Canvas parameters should be set")
        assertNotNull(fingerprint.mediaParameters, "Media parameters should be set")
        assertNotNull(fingerprint.miscParameters, "Misc parameters should be set")
    }
    
    @Test
    @DisplayName("test Mac fingerprints have Apple-specific settings")
    fun testMacFingerprintsHaveAppleSpecificSettings() {
        val macPresets = listOf(
            FingerprintGenerator.DevicePreset.MACBOOK_PRO_13,
            FingerprintGenerator.DevicePreset.MACBOOK_AIR
        )
        
        macPresets.forEach { preset ->
            val fingerprint = generator.generate(BrowserType.PULSAR_CHROME, preset)
            
            assertEquals("MacIntel", fingerprint.hardwareParameters?.platform)
            assertTrue(
                fingerprint.webGLParameters?.vendor?.contains("Apple") ?: false,
                "Mac should have Apple GPU"
            )
            assertTrue(
                fingerprint.mediaParameters?.videoInputDevices?.any {
                    it.label.contains("FaceTime")
                } ?: false,
                "Mac should have FaceTime camera"
            )
        }
    }
}
