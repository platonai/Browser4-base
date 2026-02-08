# Browser Profile API Reference

## Overview

Complete API reference for the Browser Profile Enhancement system.

**Package**: `ai.platon.pulsar.common.browser`

---

## Core Classes

### Fingerprint

```kotlin
data class Fingerprint(
    val browserType: BrowserType,
    val userAgent: String?,
    val proxyURI: URI? = null,
    val websiteAccounts: WebsiteAccounts? = null,
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

**Description**: Complete browser fingerprint containing all identification parameters.

**Properties**:
- `browserType`: Browser type (PULSAR_CHROME, etc.)
- `userAgent`: User agent string
- `proxyURI`: Optional proxy configuration
- `websiteAccounts`: Optional website login accounts
- `screenParameters`: Screen dimensions and properties
- `viewportParameters`: Browser viewport configuration
- `geoTimeParameters`: Timezone, locale, languages
- `hardwareParameters`: CPU, memory, platform info
- `webGLParameters`: GPU vendor and renderer
- `canvasParameters`: Canvas fingerprinting seed
- `mediaParameters`: Media device counts
- `miscParameters`: Additional navigator properties
- `version`: Schema version for migration

**Example**:
```kotlin
val fingerprint = Fingerprint(
    browserType = BrowserType.PULSAR_CHROME,
    userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)...",
    screenParameters = ScreenParameters(1920, 1080, 1.0, 24, "landscape"),
    hardwareParameters = HardwareParameters(8, 8, "Win32", "Google Inc.", 0),
    version = 1
)
```

---

## Parameter Classes

### ScreenParameters

```kotlin
data class ScreenParameters(
    val width: Int,
    val height: Int,
    val devicePixelRatio: Double,
    val colorDepth: Int,
    val orientation: String
)
```

**Description**: Physical screen properties.

**Properties**:
- `width`: Screen width in pixels (e.g., 1920)
- `height`: Screen height in pixels (e.g., 1080)
- `devicePixelRatio`: Pixel density ratio (typically 1.0 or 2.0 for Retina)
- `colorDepth`: Color depth in bits (24 or 30)
- `orientation`: Screen orientation ("landscape" or "portrait")

**Presets**:
```kotlin
ScreenParameters.DESKTOP_1920x1080    // Standard desktop
ScreenParameters.LAPTOP_1366x768      // Common laptop
ScreenParameters.MACBOOK_PRO_RETINA   // 2560×1600, 2.0 ratio
```

**Example**:
```kotlin
val screen = ScreenParameters(
    width = 1920,
    height = 1080,
    devicePixelRatio = 1.0,
    colorDepth = 24,
    orientation = "landscape"
)
```

---

### ViewportParameters

```kotlin
data class ViewportParameters(
    val width: Int,
    val height: Int,
    val deviceScaleFactor: Double,
    val isMobile: Boolean = false
)
```

**Description**: Browser window viewport configuration.

**Properties**:
- `width`: Viewport width in pixels
- `height`: Viewport height in pixels
- `deviceScaleFactor`: Scale factor (should match screen's devicePixelRatio)
- `isMobile`: Whether in mobile mode

**Constraints**:
- Viewport dimensions must be ≤ screen dimensions
- `deviceScaleFactor` should equal `ScreenParameters.devicePixelRatio`

**Presets**:
```kotlin
ViewportParameters.DESKTOP_1920x1080
ViewportParameters.LAPTOP_1366x768
ViewportParameters.MACBOOK_PRO_RETINA
```

**Example**:
```kotlin
val viewport = ViewportParameters(
    width = 1920,
    height = 1080,
    deviceScaleFactor = 1.0,
    isMobile = false
)
```

---

### GeoTimeParameters

```kotlin
data class GeoTimeParameters(
    val timezone: String,
    val locale: String,
    val languages: List<String>
)
```

**Description**: Geographic location and time settings.

**Properties**:
- `timezone`: IANA timezone (e.g., "America/New_York")
- `locale`: BCP 47 locale (e.g., "en-US")
- `languages`: Preferred languages list

**Constraints**:
- `languages[0]` should match `locale` prefix (e.g., "en-US" → "en")

**Presets**:
```kotlin
GeoTimeParameters.US_EASTERN      // America/New_York, en-US
GeoTimeParameters.CHINA_SHANGHAI  // Asia/Shanghai, zh-CN
GeoTimeParameters.UK_LONDON       // Europe/London, en-GB
```

**Example**:
```kotlin
val geoTime = GeoTimeParameters(
    timezone = "America/New_York",
    locale = "en-US",
    languages = listOf("en-US", "en")
)
```

---

### HardwareParameters

```kotlin
data class HardwareParameters(
    val hardwareConcurrency: Int,
    val deviceMemory: Int,
    val platform: String,
    val vendor: String,
    val maxTouchPoints: Int
)
```

**Description**: Hardware and system information.

**Properties**:
- `hardwareConcurrency`: Number of CPU cores (typically 2-16)
- `deviceMemory`: RAM in GB (typically 4-32)
- `platform`: OS platform string ("Win32", "MacIntel", "Linux x86_64")
- `vendor`: Browser vendor string (typically "Google Inc.")
- `maxTouchPoints`: Touch points (0 for non-touch, 5-10 for touch)

**Constraints**:
- `platform` must match userAgent OS
- `hardwareConcurrency` should be realistic (2, 4, 6, 8, 12, 16)
- Touch devices should have `maxTouchPoints > 0`

**Presets**:
```kotlin
HardwareParameters.WINDOWS_8_CORES   // Win32, 8 cores, 8GB
HardwareParameters.MAC_M1            // MacIntel, 8 cores, 8GB
HardwareParameters.LINUX_4_CORES     // Linux x86_64, 4 cores, 8GB
```

**Example**:
```kotlin
val hardware = HardwareParameters(
    hardwareConcurrency = 8,
    deviceMemory = 8,
    platform = "Win32",
    vendor = "Google Inc.",
    maxTouchPoints = 0
)
```

---

### WebGLParameters

```kotlin
data class WebGLParameters(
    val vendor: String,
    val renderer: String
)
```

**Description**: WebGL GPU information.

**Properties**:
- `vendor`: GPU vendor (e.g., "Google Inc. (Intel)")
- `renderer`: GPU renderer string (e.g., "ANGLE (Intel, Intel(R) UHD Graphics...)")

**Constraints**:
- Should match realistic GPU configurations
- Intel for most systems, NVIDIA/AMD for gaming systems
- Apple GPUs for Mac systems

**Presets**:
```kotlin
WebGLParameters.INTEL_HD            // Intel integrated graphics
WebGLParameters.NVIDIA_GTX          // NVIDIA discrete GPU
WebGLParameters.APPLE_M1            // Apple M1 GPU
```

**Example**:
```kotlin
val webgl = WebGLParameters(
    vendor = "Google Inc. (Intel)",
    renderer = "ANGLE (Intel, Intel(R) UHD Graphics 630 Direct3D11 vs_5_0 ps_5_0)"
)
```

---

### CanvasParameters

```kotlin
data class CanvasParameters(
    val seed: Long
)
```

**Description**: Canvas fingerprinting seed for consistent noise generation.

**Properties**:
- `seed`: Random seed for canvas fingerprinting defense

**Usage**:
Canvas fingerprinting adds subtle noise to canvas operations to prevent tracking. The seed ensures the same noise pattern is used consistently within a profile.

**Example**:
```kotlin
val canvas = CanvasParameters(
    seed = System.currentTimeMillis()  // Unique per profile
)
```

---

### MediaParameters

```kotlin
data class MediaParameters(
    val audioInputDevices: Int,
    val audioOutputDevices: Int,
    val videoInputDevices: Int
)
```

**Description**: Media device counts for navigator.mediaDevices enumeration.

**Properties**:
- `audioInputDevices`: Number of microphones (0-2)
- `audioOutputDevices`: Number of speakers (1-2)
- `videoInputDevices`: Number of cameras (0-2)

**Presets**:
```kotlin
MediaParameters.DESKTOP_TYPICAL  // 0 input, 1 output, 1 video
MediaParameters.LAPTOP_TYPICAL   // 1 input, 1 output, 1 video
```

**Example**:
```kotlin
val media = MediaParameters(
    audioInputDevices = 0,
    audioOutputDevices = 1,
    videoInputDevices = 1
)
```

---

### MiscParameters

```kotlin
data class MiscParameters(
    val doNotTrack: String?,
    val webdriver: Boolean,
    val cookieEnabled: Boolean
)
```

**Description**: Additional navigator properties.

**Properties**:
- `doNotTrack`: DNT header value ("1", "0", or null)
- `webdriver`: Whether navigator.webdriver is true
- `cookieEnabled`: Whether cookies are enabled

**Example**:
```kotlin
val misc = MiscParameters(
    doNotTrack = "1",
    webdriver = false,  // Should be false for stealth
    cookieEnabled = true
)
```

---

## Generator

### FingerprintGenerator

```kotlin
class FingerprintGenerator {
    fun generate(
        browserType: BrowserType,
        preset: DevicePreset
    ): Fingerprint
    
    fun generateRandom(
        browserType: BrowserType,
        platform: Platform
    ): Fingerprint
}
```

**Description**: Generates realistic, validated fingerprints.

**Methods**:

#### generate()

Generates fingerprint for specific device preset.

**Parameters**:
- `browserType`: Browser type (typically PULSAR_CHROME)
- `preset`: Device configuration to use

**Returns**: Complete, validated `Fingerprint`

**Example**:
```kotlin
val generator = FingerprintGenerator()
val fingerprint = generator.generate(
    BrowserType.PULSAR_CHROME,
    FingerprintGenerator.DevicePreset.DESKTOP_WINDOWS
)
```

#### generateRandom()

Generates random fingerprint for platform.

**Parameters**:
- `browserType`: Browser type
- `platform`: OS platform (WINDOWS, MAC, or LINUX)

**Returns**: Random `Fingerprint` for platform

**Example**:
```kotlin
val generator = FingerprintGenerator()
val fingerprint = generator.generateRandom(
    BrowserType.PULSAR_CHROME,
    FingerprintGenerator.Platform.WINDOWS
)
// Randomly selects DESKTOP_WINDOWS or LAPTOP_WINDOWS
```

**Device Presets**:

```kotlin
enum class DevicePreset {
    DESKTOP_WINDOWS,   // 1920×1080, Intel GPU, 8 cores
    LAPTOP_WINDOWS,    // 1366×768, Intel GPU, 4 cores
    MACBOOK_PRO_13,    // 2560×1600 Retina, M1, 8 cores
    MACBOOK_AIR,       // 2560×1600 Retina, M1, 8 cores
    DESKTOP_LINUX,     // 1920×1080, Intel GPU, 4 cores
    LAPTOP_LINUX       // 1366×768, Intel GPU, 4 cores
}
```

**Platforms**:

```kotlin
enum class Platform {
    WINDOWS,  // Includes DESKTOP_WINDOWS, LAPTOP_WINDOWS
    MAC,      // Includes MACBOOK_PRO_13, MACBOOK_AIR
    LINUX     // Includes DESKTOP_LINUX, LAPTOP_LINUX
}
```

---

## Validator

### FingerprintValidator

```kotlin
class FingerprintValidator {
    fun validate(fingerprint: Fingerprint): ValidationResult
}
```

**Description**: Validates fingerprint consistency.

**Methods**:

#### validate()

Performs comprehensive validation checks.

**Parameters**:
- `fingerprint`: The fingerprint to validate

**Returns**: `ValidationResult` with status and error messages

**Validation Checks**:
1. **Browser Type**: Must not be null
2. **User Agent**: Must not be blank
3. **Platform Match**: UserAgent OS matches HardwareParameters.platform
4. **Screen/Viewport**: Viewport ≤ screen dimensions
5. **Device Pixel Ratio**: Matches viewport scale factor
6. **Language Match**: First language matches locale
7. **Touch Points**: Mobile devices should have > 0 touch points
8. **Hardware Ranges**: CPU cores, memory within realistic ranges

**Example**:
```kotlin
val validator = FingerprintValidator()
val result = validator.validate(fingerprint)

if (result.isValid) {
    println("✓ Fingerprint is valid")
} else {
    println("✗ Validation failed:")
    result.errors.forEach { println("  - $it") }
}

if (result.warnings.isNotEmpty()) {
    println("⚠ Warnings:")
    result.warnings.forEach { println("  - $it") }
}
```

### ValidationResult

```kotlin
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String>,
    val warnings: List<String>
)
```

**Description**: Result of fingerprint validation.

**Properties**:
- `isValid`: Whether fingerprint passed all checks
- `errors`: List of validation errors (makes isValid false)
- `warnings`: List of warnings (doesn't affect isValid)

**Example**:
```kotlin
val result = ValidationResult(
    isValid = false,
    errors = listOf(
        "Screen dimensions (1366x768) must be >= viewport (1920x1080)"
    ),
    warnings = listOf(
        "Hardware concurrency (16) is unusually high"
    )
)
```

---

## Drift Detection

### FingerprintDriftDetector

```kotlin
class FingerprintDriftDetector {
    fun detectDrift(
        original: Fingerprint,
        current: Fingerprint
    ): DriftReport
}
```

**Description**: Detects changes between fingerprints.

**Methods**:

#### detectDrift()

Compares two fingerprints and identifies differences.

**Parameters**:
- `original`: The baseline fingerprint
- `current`: The current fingerprint to compare

**Returns**: `DriftReport` with detected changes

**Compared Fields**:
- Browser type
- User agent
- Proxy URI
- All parameter objects (screen, viewport, geoTime, hardware, webGL, canvas, media, misc)

**Example**:
```kotlin
val detector = FingerprintDriftDetector()
val original = loadOriginalFingerprint()
val current = loadCurrentFingerprint()

val report = detector.detectDrift(original, current)
if (report.hasDrift) {
    println("Drift detected at ${report.detectedAt}:")
    report.drifts.forEach { println("  - $it") }
}
```

### DriftReport

```kotlin
data class DriftReport(
    val hasDrift: Boolean,
    val drifts: List<String>,
    val detectedAt: Instant
)
```

**Description**: Report of fingerprint drift.

**Properties**:
- `hasDrift`: Whether any drift was detected
- `drifts`: List of human-readable change messages
- `detectedAt`: Timestamp when drift was detected

**Methods**:

#### summary()

Returns a summary string.

**Returns**: Human-readable summary

**Example**:
```kotlin
val report = DriftReport(
    hasDrift = true,
    drifts = listOf(
        "Screen resolution changed: 1920x1080 → 2560x1440",
        "Hardware concurrency changed: 8 → 16"
    ),
    detectedAt = Instant.now()
)

println(report.summary())
// "Drift detected: 2 changes at 2026-02-08T14:30:00Z"
```

---

## Health Monitoring

### ProfileHealthMonitor

```kotlin
class ProfileHealthMonitor {
    fun checkHealth(
        fingerprint: Fingerprint,
        contextDir: Path? = null
    ): HealthReport
}
```

**Description**: Monitors fingerprint and profile health.

**Methods**:

#### checkHealth()

Performs comprehensive health checks.

**Parameters**:
- `fingerprint`: The fingerprint to check
- `contextDir`: Optional profile directory for additional checks

**Returns**: `HealthReport` with check results

**Health Checks**:
1. **Fingerprint Integrity**: Essential parameters present
2. **Fingerprint Consistency**: Parameters logically coherent
3. **Context Directory**: Directory accessible and writable (if provided)
4. **Fingerprint Version**: Version is supported

**Example**:
```kotlin
val monitor = ProfileHealthMonitor()
val report = monitor.checkHealth(fingerprint, contextDir)

println(report.summary())
// "Profile is healthy (4/4 checks passed)" or
// "Profile has issues (2/4 checks failed)"

if (!report.isHealthy) {
    report.failedChecks.forEach { check ->
        println("${check.symbol} ${check.name}: ${check.message}")
    }
}
```

### HealthReport

```kotlin
data class HealthReport(
    val isHealthy: Boolean,
    val totalChecks: Int,
    val passedChecks: Int,
    val failedChecks: List<HealthCheck>,
    val checksMap: Map<String, HealthCheck>
)
```

**Description**: Complete health check report.

**Properties**:
- `isHealthy`: Whether all checks passed
- `totalChecks`: Total number of checks performed
- `passedChecks`: Number of passed checks
- `failedChecks`: List of failed checks
- `checksMap`: Map of check name to check result

**Methods**:

#### summary()

Returns a summary string.

**Returns**: Human-readable summary

**Example**:
```kotlin
val report = HealthReport(
    isHealthy = false,
    totalChecks = 4,
    passedChecks = 2,
    failedChecks = listOf(
        HealthCheck("Fingerprint Integrity", false, 
                    "Missing parameters: userAgent, screenParameters")
    ),
    checksMap = mapOf(...)
)

println(report.summary())
// "Profile has issues (2/4 checks failed)"
```

### HealthCheck

```kotlin
data class HealthCheck(
    val name: String,
    val passed: Boolean,
    val message: String
)
```

**Description**: Individual health check result.

**Properties**:
- `name`: Check name (e.g., "Fingerprint Integrity")
- `passed`: Whether check passed
- `message`: Detailed message

**Computed Properties**:
- `symbol`: "✓" if passed, "✗" if failed

**Example**:
```kotlin
val check = HealthCheck(
    name = "Fingerprint Consistency",
    passed = false,
    message = "UserAgent platform 'Win32' doesn't match HardwareParameters platform 'MacIntel'"
)

println("${check.symbol} ${check.name}: ${check.message}")
// "✗ Fingerprint Consistency: UserAgent platform..."
```

---

## Integration

### BrowserProfile

```kotlin
class BrowserProfile {
    companion object {
        fun create(contextDir: Path): BrowserProfile
    }
    
    val fingerprint: Fingerprint
}
```

**Description**: Browser profile with automatic fingerprint management.

**Methods**:

#### create()

Creates or loads a browser profile.

**Parameters**:
- `contextDir`: Profile directory path

**Returns**: `BrowserProfile` with complete fingerprint

**Behavior**:
1. Checks for `contextDir/fingerprint.json`
2. If exists: loads and validates
3. If missing: generates, validates, and saves
4. Returns profile with fingerprint

**Example**:
```kotlin
val contextDir = Path.of("/profiles/user123")
val profile = BrowserProfile.create(contextDir)

// Fingerprint is now in profile.fingerprint
// And saved to contextDir/fingerprint.json
```

---

## Utility Functions

### JSON Serialization

```kotlin
import ai.platon.pulsar.common.pulsarObjectMapper

// Serialize
val json = pulsarObjectMapper()
    .writerWithDefaultPrettyPrinter()
    .writeValueAsString(fingerprint)

// Deserialize
val fingerprint = pulsarObjectMapper()
    .readValue(json, Fingerprint::class.java)
```

---

## Constants and Enums

### BrowserType

```kotlin
enum class BrowserType {
    PULSAR_CHROME,
    NATIVE_CHROME,
    // ... others
}
```

**Description**: Supported browser types.

---

## Error Handling

All classes use defensive programming:

```kotlin
try {
    val fingerprint = generator.generate(browserType, preset)
    val result = validator.validate(fingerprint)
    // Use fingerprint
} catch (e: IllegalArgumentException) {
    logger.error("Invalid argument: ${e.message}")
} catch (e: IOException) {
    logger.error("IO error: ${e.message}")
}
```

**Common Exceptions**:
- `IllegalArgumentException`: Invalid parameters
- `IOException`: File system errors
- `JsonProcessingException`: JSON parsing errors

---

## Thread Safety

All classes are thread-safe for read operations:

```kotlin
// Safe: Multiple threads generating fingerprints
val generator = FingerprintGenerator()
val fingerprints = (1..10).map {
    CompletableFuture.supplyAsync {
        generator.generate(BrowserType.PULSAR_CHROME, 
                          DevicePreset.DESKTOP_WINDOWS)
    }
}.map { it.get() }
```

**Note**: File operations (save/load) require external synchronization if accessing the same file from multiple threads.

---

## Performance

**Generation**: < 1ms per fingerprint  
**Validation**: < 1ms per fingerprint  
**Drift Detection**: < 1ms for comparison  
**Health Check**: < 10ms (includes optional file operations)

```kotlin
// Benchmark example
val start = System.nanoTime()
val fingerprint = generator.generate(
    BrowserType.PULSAR_CHROME,
    DevicePreset.DESKTOP_WINDOWS
)
val elapsed = (System.nanoTime() - start) / 1_000_000
println("Generated in ${elapsed}ms")  // Typically < 1ms
```

---

## Migration Guide

When upgrading fingerprint schema:

```kotlin
fun migrateFingerprint(fingerprint: Fingerprint): Fingerprint {
    return when (fingerprint.version) {
        1 -> fingerprint  // Current version
        0 -> {
            // Migrate from version 0 to 1
            fingerprint.copy(
                version = 1,
                // Add new fields, transform old ones
            )
        }
        else -> throw IllegalStateException(
            "Unsupported fingerprint version: ${fingerprint.version}"
        )
    }
}
```

---

## Related Documentation

- **User Guide**: `docs-dev/browser-profile-user-guide.md`
- **Implementation Summary**: `docs-dev/browser-profile-implementation-summary.md`
- **Analysis**: `docs-dev/browser-profile-enhancement-analysis.md`

---

**Last Updated**: 2026-02-08  
**API Version**: 1.0  
**Status**: Stable
