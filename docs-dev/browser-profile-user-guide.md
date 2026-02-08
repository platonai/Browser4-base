# Browser Profile User Guide

## Table of Contents
1. [Introduction](#introduction)
2. [Quick Start](#quick-start)
3. [Core Concepts](#core-concepts)
4. [Common Use Cases](#common-use-cases)
5. [Configuration](#configuration)
6. [Advanced Topics](#advanced-topics)
7. [Troubleshooting](#troubleshooting)
8. [API Reference](#api-reference)

## Introduction

The Browser Profile Enhancement system provides comprehensive fingerprinting capabilities to create stable, realistic browser identities. Each profile maintains consistency across:

- **Screen Parameters**: Resolution, color depth, pixel ratio
- **Viewport Parameters**: Window dimensions, scale factor
- **Geo/Time Parameters**: Timezone, locale, languages
- **Hardware Parameters**: CPU cores, memory, platform
- **WebGL Parameters**: GPU vendor, renderer
- **Canvas Parameters**: Unique seed for canvas fingerprinting
- **Media Parameters**: Audio/video device enumeration
- **Misc Parameters**: Navigator properties, plugins

### Key Benefits

✅ **Consistency**: Parameters logically align (e.g., Windows userAgent → Win32 platform)  
✅ **Stability**: Same fingerprint across sessions (no drift)  
✅ **Realism**: Based on actual device configurations  
✅ **Validation**: Automatic consistency checks  
✅ **Monitoring**: Drift detection and health checks  

## Quick Start

### Basic Usage

```kotlin
import ai.platon.pulsar.skeleton.crawl.fetch.privacy.BrowserProfile
import ai.platon.pulsar.common.browser.*

// 1. Create a profile (auto-generates fingerprint if missing)
val profile = BrowserProfile.create(contextDir)

// The profile now has a complete fingerprint saved to:
// contextDir/fingerprint.json

// 2. Use the profile with a browser
// The fingerprint is automatically applied when the browser starts
```

### Manual Fingerprint Generation

```kotlin
import ai.platon.pulsar.common.browser.*

// Create a generator
val generator = FingerprintGenerator()

// Generate a specific device
val desktop = generator.generate(
    BrowserType.PULSAR_CHROME,
    FingerprintGenerator.DevicePreset.DESKTOP_WINDOWS
)

// Generate random device for a platform
val randomMac = generator.generateRandom(
    BrowserType.PULSAR_CHROME,
    FingerprintGenerator.Platform.MAC
)

// Validate the fingerprint
val validator = FingerprintValidator()
val result = validator.validate(desktop)
if (result.isValid) {
    println("✓ Fingerprint is valid")
} else {
    println("✗ Validation errors:")
    result.errors.forEach { println("  - $it") }
}

// Save to JSON
val json = pulsarObjectMapper().writeValueAsString(desktop)
Files.writeString(Path.of("fingerprint.json"), json)
```

## Core Concepts

### 1. Fingerprint Model

A `Fingerprint` contains all parameters that identify a browser:

```kotlin
data class Fingerprint(
    val browserType: BrowserType,
    val userAgent: String?,
    val proxyURI: URI? = null,
    
    // Extended parameters (Phase 1)
    val screenParameters: ScreenParameters? = null,
    val viewportParameters: ViewportParameters? = null,
    val geoTimeParameters: GeoTimeParameters? = null,
    val hardwareParameters: HardwareParameters? = null,
    val webGLParameters: WebGLParameters? = null,
    val canvasParameters: CanvasParameters? = null,
    val mediaParameters: MediaParameters? = null,
    val miscParameters: MiscParameters? = null,
    val version: Int = 1
)
```

### 2. Device Presets

Six realistic device configurations:

```kotlin
enum class DevicePreset {
    DESKTOP_WINDOWS,   // 1920×1080, 8 cores, Intel GPU
    LAPTOP_WINDOWS,    // 1366×768, 4 cores, Intel GPU
    MACBOOK_PRO_13,    // 2560×1600 Retina, Apple M1
    MACBOOK_AIR,       // 2560×1600 Retina, Apple M1
    DESKTOP_LINUX,     // 1920×1080, 4 cores, Intel GPU
    LAPTOP_LINUX       // 1366×768, 4 cores, Intel GPU
}
```

### 3. Validation

`FingerprintValidator` ensures consistency:

- **UserAgent ↔ Platform**: "Windows NT" → "Win32"
- **Screen ≥ Viewport**: Screen must be larger than viewport
- **DevicePixelRatio**: Must match viewport scale factor
- **Hardware**: Realistic CPU core counts
- **Geo-Time**: Timezone aligns with locale

### 4. Drift Detection

`FingerprintDriftDetector` identifies changes:

```kotlin
val detector = FingerprintDriftDetector()
val original = loadOriginalFingerprint()
val current = loadCurrentFingerprint()

val report = detector.detectDrift(original, current)
if (report.hasDrift) {
    println("Drift detected at ${report.detectedAt}:")
    report.drifts.forEach { println("  - $it") }
    // Example output:
    // - Screen resolution changed: 1920x1080 → 2560x1440
    // - Platform changed: Win32 → MacIntel
}
```

### 5. Health Monitoring

`ProfileHealthMonitor` performs multi-level checks:

```kotlin
val monitor = ProfileHealthMonitor()
val report = monitor.checkHealth(fingerprint, contextDir)

println(report.summary())
// "Profile is healthy (4/4 checks passed)" or
// "Profile has issues (2/4 checks failed)"

if (!report.isHealthy) {
    report.failedChecks.forEach { println(it) }
}
```

## Common Use Cases

### Use Case 1: Auto-Generated Profile

**Scenario**: You want a profile with automatic fingerprint generation.

```kotlin
// Create profile - automatically generates fingerprint if missing
val profile = BrowserProfile.create(contextDir)

// Fingerprint is saved to contextDir/fingerprint.json
// It will be loaded and reused in future sessions
```

**When to use**: Most common case - let the system handle fingerprint generation and persistence.

### Use Case 2: Specific Device Type

**Scenario**: You need a MacBook Pro fingerprint.

```kotlin
val generator = FingerprintGenerator()
val fingerprint = generator.generate(
    BrowserType.PULSAR_CHROME,
    FingerprintGenerator.DevicePreset.MACBOOK_PRO_13
)

// Save to profile directory
val contextDir = Path.of("/path/to/profile")
Files.createDirectories(contextDir)
val json = pulsarObjectMapper().writerWithDefaultPrettyPrinter()
    .writeValueAsString(fingerprint)
Files.writeString(contextDir.resolve("fingerprint.json"), json)

// Create profile
val profile = BrowserProfile.create(contextDir)
```

**When to use**: When you need to simulate a specific device type.

### Use Case 3: Random Device for Platform

**Scenario**: You need a random Windows device.

```kotlin
val generator = FingerprintGenerator()
val fingerprint = generator.generateRandom(
    BrowserType.PULSAR_CHROME,
    FingerprintGenerator.Platform.WINDOWS
)

// Will randomly select DESKTOP_WINDOWS or LAPTOP_WINDOWS
```

**When to use**: When you want variety within a platform.

### Use Case 4: Custom Parameters

**Scenario**: You need specific screen resolution with preset template.

```kotlin
val generator = FingerprintGenerator()
val baseFingerprint = generator.generate(
    BrowserType.PULSAR_CHROME,
    FingerprintGenerator.DevicePreset.DESKTOP_WINDOWS
)

// Customize screen parameters
val customFingerprint = baseFingerprint.copy(
    screenParameters = ScreenParameters(
        width = 3840,
        height = 2160,
        devicePixelRatio = 1.0,
        colorDepth = 24,
        orientation = "landscape"
    ),
    viewportParameters = ViewportParameters(
        width = 3840,
        height = 2160,
        deviceScaleFactor = 1.0
    )
)

// Validate custom fingerprint
val validator = FingerprintValidator()
val result = validator.validate(customFingerprint)
require(result.isValid) { "Custom fingerprint is invalid: ${result.errors}" }
```

**When to use**: When you need fine-grained control over specific parameters.

### Use Case 5: Monitoring Profile Health

**Scenario**: Periodic health checks on profiles.

```kotlin
val monitor = ProfileHealthMonitor()

profiles.forEach { profile ->
    val fingerprint = loadFingerprint(profile.contextDir)
    val report = monitor.checkHealth(fingerprint, profile.contextDir)
    
    if (!report.isHealthy) {
        logger.warn("Profile ${profile.id} has issues:")
        report.failedChecks.forEach { logger.warn("  $it") }
        
        // Take action - regenerate fingerprint, etc.
        if (!report.checksMap["Fingerprint Integrity"]!!.passed) {
            // Regenerate fingerprint
            regenerateFingerprint(profile)
        }
    }
}
```

**When to use**: In production systems with long-running profiles.

### Use Case 6: Detecting Fingerprint Drift

**Scenario**: Verify fingerprint hasn't changed over time.

```kotlin
val detector = FingerprintDriftDetector()

// Load original fingerprint
val originalPath = contextDir.resolve("fingerprint.json.backup")
val original = loadFingerprint(originalPath)

// Load current fingerprint
val current = loadFingerprint(contextDir.resolve("fingerprint.json"))

// Detect drift
val report = detector.detectDrift(original, current)
if (report.hasDrift) {
    logger.error("Fingerprint drift detected!")
    logger.error("Changes:")
    report.drifts.forEach { logger.error("  - $it") }
    
    // Decide: restore original or accept changes?
    if (shouldRestore) {
        Files.copy(originalPath, contextDir.resolve("fingerprint.json"), 
                   StandardCopyOption.REPLACE_EXISTING)
    }
}
```

**When to use**: In security-sensitive applications where fingerprint stability is critical.

## Configuration

### Profile Directory Structure

```
profile-directory/
├── fingerprint.json          # Complete fingerprint
├── cookies/                  # Browser cookies
├── localStorage/             # Local storage
└── cache/                    # Browser cache
```

### fingerprint.json Format

```json
{
  "browserType": "PULSAR_CHROME",
  "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36...",
  "proxyURI": null,
  "screenParameters": {
    "width": 1920,
    "height": 1080,
    "devicePixelRatio": 1.0,
    "colorDepth": 24,
    "orientation": "landscape"
  },
  "viewportParameters": {
    "width": 1920,
    "height": 1080,
    "deviceScaleFactor": 1.0,
    "isMobile": false
  },
  "geoTimeParameters": {
    "timezone": "America/New_York",
    "locale": "en-US",
    "languages": ["en-US", "en"]
  },
  "hardwareParameters": {
    "hardwareConcurrency": 8,
    "deviceMemory": 8,
    "platform": "Win32",
    "vendor": "Google Inc.",
    "maxTouchPoints": 0
  },
  "webGLParameters": {
    "vendor": "Google Inc. (Intel)",
    "renderer": "ANGLE (Intel, Intel(R) UHD Graphics 630 Direct3D11 vs_5_0 ps_5_0)"
  },
  "canvasParameters": {
    "seed": 1234567890
  },
  "mediaParameters": {
    "audioInputDevices": 0,
    "audioOutputDevices": 1,
    "videoInputDevices": 1
  },
  "miscParameters": {
    "doNotTrack": "1",
    "webdriver": false,
    "cookieEnabled": true
  },
  "version": 1
}
```

### Environment Variables

Currently, fingerprint configuration is file-based. Future versions may support:

```bash
# Not yet implemented
export PULSAR_FINGERPRINT_PRESET=DESKTOP_WINDOWS
export PULSAR_FINGERPRINT_AUTO_REGENERATE=false
```

## Advanced Topics

### Custom Device Templates

You can create custom presets by extending the generator:

```kotlin
class CustomFingerprintGenerator : FingerprintGenerator() {
    fun generateHighEndGamingPC(browserType: BrowserType): Fingerprint {
        return generate(browserType, DevicePreset.DESKTOP_WINDOWS).copy(
            screenParameters = ScreenParameters(
                width = 3840,
                height = 2160,
                devicePixelRatio = 1.0,
                colorDepth = 30  // 10-bit color
            ),
            hardwareParameters = HardwareParameters(
                hardwareConcurrency = 16,  // High-end CPU
                deviceMemory = 32,
                platform = "Win32",
                vendor = "Google Inc.",
                maxTouchPoints = 0
            ),
            webGLParameters = WebGLParameters(
                vendor = "Google Inc. (NVIDIA Corporation)",
                renderer = "ANGLE (NVIDIA, NVIDIA GeForce RTX 3090 Direct3D11 vs_5_0 ps_5_0)"
            )
        )
    }
}
```

### Migration Between Fingerprint Versions

When the fingerprint schema changes (version increment):

```kotlin
fun migrateFingerprint(old: Fingerprint, newVersion: Int): Fingerprint {
    return when {
        old.version == 1 && newVersion == 2 -> {
            // Migration logic
            old.copy(
                version = 2,
                // Add new fields, transform old ones
            )
        }
        else -> old
    }
}
```

### Integration with Privacy Context

```kotlin
// In MultiPrivacyContextManager
class EnhancedPrivacyContextManager {
    private val healthMonitor = ProfileHealthMonitor()
    private val driftDetector = FingerprintDriftDetector()
    
    fun selectHealthyContext(): PrivacyContext {
        val contexts = getAllContexts()
        
        // Filter out unhealthy profiles
        val healthy = contexts.filter { context ->
            val fingerprint = loadFingerprint(context.contextDir)
            val health = healthMonitor.checkHealth(fingerprint, context.contextDir)
            health.isHealthy
        }
        
        // Select from healthy profiles
        return healthy.random()
    }
}
```

## Troubleshooting

### Issue: Fingerprint validation fails

**Symptoms**: `ValidationResult.isValid = false`

**Solution**:
```kotlin
val result = validator.validate(fingerprint)
if (!result.isValid) {
    println("Validation errors:")
    result.errors.forEach { println("  - $it") }
    
    // Common fixes:
    // 1. Regenerate with a preset
    val fixed = generator.generate(
        fingerprint.browserType,
        FingerprintGenerator.DevicePreset.DESKTOP_WINDOWS
    )
    
    // 2. Or fix specific issue
    if (result.errors.any { it.contains("viewport") }) {
        // Ensure viewport ≤ screen
        val screen = fingerprint.screenParameters!!
        val fixedFingerprint = fingerprint.copy(
            viewportParameters = ViewportParameters(
                width = screen.width,
                height = screen.height,
                deviceScaleFactor = screen.devicePixelRatio
            )
        )
    }
}
```

### Issue: Fingerprint drifts between sessions

**Symptoms**: Parameters change unexpectedly

**Solution**:
```kotlin
// 1. Check for external modification
val report = detector.detectDrift(backup, current)
if (report.hasDrift) {
    // Restore from backup
    Files.copy(backupPath, fingerprintPath, StandardCopyOption.REPLACE_EXISTING)
}

// 2. Ensure file permissions
Files.setPosixFilePermissions(fingerprintPath, 
    PosixFilePermissions.fromString("rw-------"))

// 3. Add integrity check on load
val checksum = calculateChecksum(fingerprintPath)
// Store and verify checksum
```

### Issue: Browser doesn't apply fingerprint

**Symptoms**: Browser shows different parameters than fingerprint

**Checklist**:
1. ✓ Fingerprint.json exists in profile directory
2. ✓ BrowserProfile.create() was called
3. ✓ Profile directory passed to browser
4. ✓ CDP methods available (check logs for reflection errors)
5. ✓ JavaScript injection executed (check browser console)

**Debug**:
```kotlin
// Add debug logging
logger.debug("Loading fingerprint from: ${contextDir}")
val fingerprint = loadFingerprint(contextDir)
logger.debug("Fingerprint: ${pulsarObjectMapper().writeValueAsString(fingerprint)}")

// In browser, check applied parameters:
// console.log(navigator.hardwareConcurrency)
// console.log(screen.width, screen.height)
// console.log(navigator.platform)
```

### Issue: "Context directory not accessible"

**Symptoms**: Health check fails with directory error

**Solution**:
```kotlin
// 1. Create directory if missing
Files.createDirectories(contextDir)

// 2. Check permissions
val writable = Files.isWritable(contextDir)
if (!writable) {
    // Fix permissions on Unix
    Files.setPosixFilePermissions(contextDir,
        PosixFilePermissions.fromString("rwx------"))
}

// 3. Check disk space
val usable = Files.getFileStore(contextDir).usableSpace
if (usable < 10 * 1024 * 1024) {  // < 10 MB
    logger.error("Low disk space: $usable bytes")
}
```

### Issue: Tests fail with "Browser not available"

**Symptoms**: Integration tests can't start browser

**Solution**:
```kotlin
// Check Chrome installation
val chromePath = System.getenv("CHROME_BIN") 
    ?: "/usr/bin/google-chrome"
    
require(Files.exists(Path.of(chromePath))) {
    "Chrome not found at $chromePath"
}

// Or skip tests if browser unavailable
@EnabledIf("isBrowserAvailable")
@Test
fun testBrowserFeature() { ... }

fun isBrowserAvailable(): Boolean {
    return System.getenv("SKIP_BROWSER_TESTS") != "true"
}
```

## API Reference

### FingerprintGenerator

```kotlin
class FingerprintGenerator {
    /**
     * Generate fingerprint for specific device preset
     */
    fun generate(
        browserType: BrowserType,
        preset: DevicePreset
    ): Fingerprint
    
    /**
     * Generate random device for platform
     */
    fun generateRandom(
        browserType: BrowserType,
        platform: Platform
    ): Fingerprint
    
    enum class DevicePreset {
        DESKTOP_WINDOWS, LAPTOP_WINDOWS,
        MACBOOK_PRO_13, MACBOOK_AIR,
        DESKTOP_LINUX, LAPTOP_LINUX
    }
    
    enum class Platform { WINDOWS, MAC, LINUX }
}
```

### FingerprintValidator

```kotlin
class FingerprintValidator {
    /**
     * Validate fingerprint consistency
     * 
     * @return ValidationResult with isValid and errors list
     */
    fun validate(fingerprint: Fingerprint): ValidationResult
}

data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String>,
    val warnings: List<String>
)
```

### FingerprintDriftDetector

```kotlin
class FingerprintDriftDetector {
    /**
     * Detect changes between fingerprints
     * 
     * @return DriftReport with hasDrift and list of changes
     */
    fun detectDrift(
        original: Fingerprint,
        current: Fingerprint
    ): DriftReport
}

data class DriftReport(
    val hasDrift: Boolean,
    val drifts: List<String>,
    val detectedAt: Instant
)
```

### ProfileHealthMonitor

```kotlin
class ProfileHealthMonitor {
    /**
     * Perform comprehensive health check
     * 
     * @param fingerprint The fingerprint to check
     * @param contextDir Optional profile directory for additional checks
     * @return HealthReport with status and details
     */
    fun checkHealth(
        fingerprint: Fingerprint,
        contextDir: Path? = null
    ): HealthReport
}

data class HealthReport(
    val isHealthy: Boolean,
    val totalChecks: Int,
    val passedChecks: Int,
    val failedChecks: List<HealthCheck>,
    val checksMap: Map<String, HealthCheck>
)
```

### Parameter Classes

```kotlin
data class ScreenParameters(
    val width: Int,
    val height: Int,
    val devicePixelRatio: Double,
    val colorDepth: Int,
    val orientation: String
)

data class ViewportParameters(
    val width: Int,
    val height: Int,
    val deviceScaleFactor: Double,
    val isMobile: Boolean = false
)

data class GeoTimeParameters(
    val timezone: String,
    val locale: String,
    val languages: List<String>
)

data class HardwareParameters(
    val hardwareConcurrency: Int,
    val deviceMemory: Int,
    val platform: String,
    val vendor: String,
    val maxTouchPoints: Int
)

data class WebGLParameters(
    val vendor: String,
    val renderer: String
)

data class CanvasParameters(
    val seed: Long
)

data class MediaParameters(
    val audioInputDevices: Int,
    val audioOutputDevices: Int,
    val videoInputDevices: Int
)

data class MiscParameters(
    val doNotTrack: String?,
    val webdriver: Boolean,
    val cookieEnabled: Boolean
)
```

## Best Practices

### 1. Always Validate Generated Fingerprints

```kotlin
val fingerprint = generator.generate(...)
val result = validator.validate(fingerprint)
require(result.isValid) { "Invalid fingerprint: ${result.errors}" }
```

### 2. Use Device Presets Instead of Manual Construction

❌ **Don't**:
```kotlin
val fingerprint = Fingerprint(
    browserType = BrowserType.PULSAR_CHROME,
    userAgent = "...",
    screenParameters = ScreenParameters(...),
    // Easy to make inconsistent
)
```

✅ **Do**:
```kotlin
val fingerprint = generator.generate(
    BrowserType.PULSAR_CHROME,
    DevicePreset.DESKTOP_WINDOWS
)
// Guaranteed consistent
```

### 3. Back Up Fingerprints Before Modification

```kotlin
// Backup before modifying
val backup = contextDir.resolve("fingerprint.json.backup")
Files.copy(fingerprintPath, backup, StandardCopyOption.REPLACE_EXISTING)

// Modify
val modified = fingerprint.copy(...)
saveFingerprint(modified, contextDir)
```

### 4. Monitor Profile Health in Production

```kotlin
// Periodic health check
scheduledExecutor.scheduleAtFixedRate({
    profiles.forEach { profile ->
        val health = monitor.checkHealth(profile.fingerprint, profile.contextDir)
        if (!health.isHealthy) {
            alerting.send("Profile ${profile.id} unhealthy: ${health.failedChecks}")
        }
    }
}, 0, 1, TimeUnit.HOURS)
```

### 5. Use Appropriate Presets for Use Case

- **Web scraping**: Use `LAPTOP_WINDOWS` or `LAPTOP_LINUX` (more common)
- **Testing**: Use `DESKTOP_WINDOWS` (consistent)
- **Mobile emulation**: Customize with `isMobile=true`
- **High-res workflows**: Use `MACBOOK_PRO_13` (Retina)

### 6. Don't Modify Fingerprints After Creation

Fingerprints should be stable. If you need different parameters, generate a new fingerprint.

❌ **Don't**:
```kotlin
fingerprint.screenParameters = newScreen  // Drift!
```

✅ **Do**:
```kotlin
val newFingerprint = generator.generate(...)  // New stable fingerprint
```

### 7. Handle Validation Errors Gracefully

```kotlin
val result = validator.validate(fingerprint)
if (!result.isValid) {
    // Log but don't fail
    logger.warn("Fingerprint validation failed: ${result.errors}")
    
    // Decide: regenerate or continue
    if (result.errors.any { it.contains("critical") }) {
        fingerprint = generator.generate(...)  // Regenerate
    }
    // Otherwise continue with warnings
}
```

### 8. Test Fingerprints Before Deployment

```kotlin
@Test
fun testFingerprintConsistency() {
    val fingerprint = loadProductionFingerprint()
    
    // Validate
    val validation = validator.validate(fingerprint)
    assertTrue(validation.isValid)
    
    // Check no drift from baseline
    val baseline = loadBaselineFingerprint()
    val drift = detector.detectDrift(baseline, fingerprint)
    assertFalse(drift.hasDrift, "Production fingerprint has drifted")
}
```

---

## Summary

The Browser Profile Enhancement system provides:

✅ **Comprehensive fingerprinting** across 9 parameter categories  
✅ **Automatic generation** with realistic device presets  
✅ **Validation** ensuring logical consistency  
✅ **Monitoring** with drift detection and health checks  
✅ **Easy integration** with existing browser automation  

For questions or issues, consult:
- Implementation summary: `docs-dev/browser-profile-implementation-summary.md`
- Analysis document: `docs-dev/browser-profile-enhancement-analysis.md`
- Source code: `pulsar-core/pulsar-common/src/main/kotlin/ai/platon/pulsar/common/browser/`

---

**Last Updated**: 2026-02-08  
**Version**: 1.0  
**Status**: Production Ready
