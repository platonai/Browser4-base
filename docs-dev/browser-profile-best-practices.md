# Browser Profile Best Practices Guide

## Table of Contents
1. [Introduction](#introduction)
2. [Generation Best Practices](#generation-best-practices)
3. [Validation Best Practices](#validation-best-practices)
4. [Persistence Best Practices](#persistence-best-practices)
5. [Monitoring Best Practices](#monitoring-best-practices)
6. [Performance Best Practices](#performance-best-practices)
7. [Security Best Practices](#security-best-practices)
8. [Testing Best Practices](#testing-best-practices)
9. [Common Pitfalls](#common-pitfalls)
10. [Production Checklist](#production-checklist)

## Introduction

This guide provides best practices for using the Browser Profile Enhancement system effectively, securely, and performantly in production environments.

## Generation Best Practices

### ✅ DO: Use Device Presets

Device presets ensure consistency and realism.

```kotlin
// ✅ GOOD: Use preset
val fingerprint = generator.generate(
    BrowserType.PULSAR_CHROME,
    FingerprintGenerator.DevicePreset.DESKTOP_WINDOWS
)
```

```kotlin
// ❌ BAD: Manual construction
val fingerprint = Fingerprint(
    browserType = BrowserType.PULSAR_CHROME,
    userAgent = "Mozilla/5.0...",
    screenParameters = ScreenParameters(1920, 1080, 1.0, 24, "landscape"),
    // Easy to create inconsistent fingerprint
)
```

**Why**: Presets guarantee:
- All parameters are populated
- Parameters are logically consistent
- Realistic device configurations
- Automatic validation passes

### ✅ DO: Choose Appropriate Presets for Your Use Case

Different presets suit different scenarios:

```kotlin
// Web scraping: Use common configurations
val scraping = generator.generate(
    BrowserType.PULSAR_CHROME,
    DevicePreset.LAPTOP_WINDOWS  // Most common
)

// Testing: Use stable configuration
val testing = generator.generate(
    BrowserType.PULSAR_CHROME,
    DevicePreset.DESKTOP_WINDOWS  // Predictable
)

// High-res work: Use Retina display
val highRes = generator.generate(
    BrowserType.PULSAR_CHROME,
    DevicePreset.MACBOOK_PRO_13  // 2560×1600
)
```

**Guidelines**:
- **Scraping/Automation**: `LAPTOP_WINDOWS` or `LAPTOP_LINUX` (most common)
- **Testing**: `DESKTOP_WINDOWS` (consistent, predictable)
- **Mobile Simulation**: Customize with `isMobile=true`
- **High-DPI**: `MACBOOK_PRO_13` or `MACBOOK_AIR`

### ✅ DO: Generate Once, Reuse Many Times

Fingerprints should be stable across sessions.

```kotlin
// ✅ GOOD: Generate once and persist
val profile = BrowserProfile.create(contextDir)
// Reuses existing fingerprint or generates new one

// Use the same profile for multiple sessions
repeat(100) {
    session.use(profile) {
        // Same fingerprint every time
    }
}
```

```kotlin
// ❌ BAD: Regenerate every time
repeat(100) {
    val fingerprint = generator.generate(...)  // New fingerprint each time!
    // This defeats the purpose of stable identity
}
```

**Why**: Fingerprint stability is key to maintaining a consistent browser identity.

### ✅ DO: Validate Generated Fingerprints

Always validate, even when using presets (defensive programming).

```kotlin
val fingerprint = generator.generate(
    BrowserType.PULSAR_CHROME,
    DevicePreset.DESKTOP_WINDOWS
)

val validator = FingerprintValidator()
val result = validator.validate(fingerprint)

require(result.isValid) { 
    "Generated fingerprint is invalid: ${result.errors}" 
}
```

**Why**: Catches edge cases, future changes, or corruption.

### ❌ DON'T: Modify Fingerprints After Creation

Fingerprints should be immutable in practice.

```kotlin
// ❌ BAD: Modifying existing fingerprint
val modified = existingFingerprint.copy(
    screenParameters = newScreenParams  // Causes drift!
)
```

```kotlin
// ✅ GOOD: Generate new fingerprint
val newFingerprint = generator.generate(
    BrowserType.PULSAR_CHROME,
    DevicePreset.DESKTOP_WINDOWS
)
```

**Why**: Modifications can break consistency and cause drift.

## Validation Best Practices

### ✅ DO: Validate Before Saving

Always validate before persisting fingerprints.

```kotlin
val fingerprint = generator.generate(...)
val result = validator.validate(fingerprint)

if (result.isValid) {
    saveFingerprint(fingerprint, contextDir)
} else {
    logger.error("Invalid fingerprint: ${result.errors}")
    throw IllegalStateException("Cannot save invalid fingerprint")
}
```

### ✅ DO: Validate On Load

Verify fingerprints when loading from disk.

```kotlin
val fingerprint = loadFingerprint(contextDir)
val result = validator.validate(fingerprint)

if (!result.isValid) {
    logger.warn("Loaded fingerprint is invalid: ${result.errors}")
    
    // Decide: regenerate or fail
    if (strictMode) {
        throw IllegalStateException("Invalid fingerprint")
    } else {
        // Regenerate
        return generator.generate(
            fingerprint.browserType,
            DevicePreset.DESKTOP_WINDOWS
        )
    }
}
```

**Why**: Files can be corrupted, manually edited, or outdated.

### ✅ DO: Handle Warnings Appropriately

Warnings don't fail validation but may indicate issues.

```kotlin
val result = validator.validate(fingerprint)

if (result.warnings.isNotEmpty()) {
    result.warnings.forEach { warning ->
        logger.warn("Fingerprint warning: $warning")
    }
    
    // Decide based on severity
    if (result.warnings.any { it.contains("unusually high") }) {
        // May flag as suspicious in fingerprint detection
        logger.warn("Fingerprint may be detectable")
    }
}
```

### ❌ DON'T: Ignore Validation Errors

```kotlin
// ❌ BAD: Ignoring validation
val fingerprint = generator.generate(...)
// No validation - might be invalid!
```

```kotlin
// ✅ GOOD: Always validate
val fingerprint = generator.generate(...)
val result = validator.validate(fingerprint)
require(result.isValid)
```

## Persistence Best Practices

### ✅ DO: Use Atomic File Operations

Prevent corruption during save.

```kotlin
fun saveFingerprint(fingerprint: Fingerprint, contextDir: Path) {
    val fingerprintPath = contextDir.resolve("fingerprint.json")
    val tempPath = contextDir.resolve("fingerprint.json.tmp")
    
    try {
        // Write to temp file
        val json = pulsarObjectMapper()
            .writerWithDefaultPrettyPrinter()
            .writeValueAsString(fingerprint)
        Files.writeString(tempPath, json, StandardOpenOption.CREATE)
        
        // Atomic rename
        Files.move(tempPath, fingerprintPath, 
                   StandardCopyOption.ATOMIC_MOVE,
                   StandardCopyOption.REPLACE_EXISTING)
    } catch (e: Exception) {
        Files.deleteIfExists(tempPath)
        throw e
    }
}
```

**Why**: Prevents partial writes if process crashes.

### ✅ DO: Back Up Before Modifications

Create backups before any changes.

```kotlin
val fingerprintPath = contextDir.resolve("fingerprint.json")
val backupPath = contextDir.resolve("fingerprint.json.backup")

// Backup before modification
if (Files.exists(fingerprintPath)) {
    Files.copy(fingerprintPath, backupPath,
               StandardCopyOption.REPLACE_EXISTING)
}

// Now safe to modify
modifyFingerprint(fingerprintPath)

// Verify modification succeeded
val fingerprint = loadFingerprint(fingerprintPath)
val result = validator.validate(fingerprint)

if (!result.isValid) {
    // Restore backup
    Files.copy(backupPath, fingerprintPath,
               StandardCopyOption.REPLACE_EXISTING)
    throw IllegalStateException("Modification failed, restored backup")
}
```

### ✅ DO: Use Proper File Permissions

Protect fingerprints from unauthorized access.

```kotlin
val fingerprintPath = contextDir.resolve("fingerprint.json")

// Unix: Set to user read/write only
if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
    Files.setPosixFilePermissions(fingerprintPath,
        PosixFilePermissions.fromString("rw-------"))
}

// Windows: Use ACLs
if (System.getProperty("os.name").startsWith("Windows")) {
    val acl = Files.getFileAttributeView(fingerprintPath, 
                                          AclFileAttributeView::class.java)
    // Configure ACL for user only
}
```

**Why**: Fingerprints are sensitive - prevent tampering and unauthorized access.

### ✅ DO: Version Your Fingerprints

Include version for future migrations.

```kotlin
data class Fingerprint(
    // ... fields ...
    val version: Int = 1  // Always include version
)

fun loadFingerprint(path: Path): Fingerprint {
    val fingerprint = pulsarObjectMapper()
        .readValue(path.toFile(), Fingerprint::class.java)
    
    // Migrate if needed
    return when (fingerprint.version) {
        1 -> fingerprint  // Current version
        0 -> migrateV0ToV1(fingerprint)
        else -> throw IllegalStateException(
            "Unsupported fingerprint version: ${fingerprint.version}"
        )
    }
}
```

### ❌ DON'T: Store Fingerprints in VCS

```kotlin
// ❌ BAD: Committing fingerprints to git
// /profiles/user123/fingerprint.json

// ✅ GOOD: Add to .gitignore
// profiles/*/fingerprint.json
// profiles/*/*.json
```

**Why**: Fingerprints are per-installation, not per-codebase.

## Monitoring Best Practices

### ✅ DO: Implement Periodic Health Checks

Monitor profiles in production.

```kotlin
class ProfileMonitoringService {
    private val monitor = ProfileHealthMonitor()
    private val scheduler = Executors.newScheduledThreadPool(1)
    
    fun start() {
        scheduler.scheduleAtFixedRate({
            checkAllProfiles()
        }, 0, 1, TimeUnit.HOURS)
    }
    
    private fun checkAllProfiles() {
        profiles.forEach { profile ->
            try {
                val report = monitor.checkHealth(
                    profile.fingerprint,
                    profile.contextDir
                )
                
                if (!report.isHealthy) {
                    handleUnhealthyProfile(profile, report)
                }
            } catch (e: Exception) {
                logger.error("Health check failed for ${profile.id}", e)
            }
        }
    }
    
    private fun handleUnhealthyProfile(
        profile: Profile,
        report: HealthReport
    ) {
        logger.warn("Profile ${profile.id} unhealthy: ${report.failedChecks}")
        
        // Alert
        alerting.send("Profile unhealthy", report.summary())
        
        // Take action based on failed checks
        report.failedChecks.forEach { check ->
            when (check.name) {
                "Fingerprint Integrity" -> regenerateFingerprint(profile)
                "Context Directory" -> recreateDirectory(profile)
                // ... handle others
            }
        }
    }
}
```

### ✅ DO: Monitor Drift

Detect unexpected fingerprint changes.

```kotlin
class DriftMonitoringService {
    private val detector = FingerprintDriftDetector()
    
    fun checkDrift(profile: Profile) {
        // Load original (from backup or database)
        val original = loadOriginalFingerprint(profile)
        
        // Load current
        val current = loadFingerprint(profile.contextDir)
        
        // Detect drift
        val report = detector.detectDrift(original, current)
        
        if (report.hasDrift) {
            logger.error("Drift detected in profile ${profile.id}:")
            report.drifts.forEach { logger.error("  - $it") }
            
            // Alert
            alerting.send("Fingerprint drift", report.summary())
            
            // Decide: restore or accept
            if (shouldRestore(report)) {
                restoreOriginalFingerprint(profile, original)
            } else {
                // Update baseline
                updateOriginalFingerprint(profile, current)
            }
        }
    }
}
```

### ✅ DO: Track Metrics

Monitor fingerprint usage and health.

```kotlin
class FingerprintMetrics {
    private val registry = MetricRegistry()
    
    private val generationCounter = registry.counter("fingerprint.generation")
    private val validationCounter = registry.counter("fingerprint.validation")
    private val validationFailures = registry.counter("fingerprint.validation.failures")
    private val driftDetections = registry.counter("fingerprint.drift.detections")
    private val healthChecks = registry.counter("fingerprint.health.checks")
    private val healthFailures = registry.counter("fingerprint.health.failures")
    
    fun recordGeneration() {
        generationCounter.inc()
    }
    
    fun recordValidation(result: ValidationResult) {
        validationCounter.inc()
        if (!result.isValid) {
            validationFailures.inc()
        }
    }
    
    fun recordDrift(report: DriftReport) {
        if (report.hasDrift) {
            driftDetections.inc()
        }
    }
    
    fun recordHealthCheck(report: HealthReport) {
        healthChecks.inc()
        if (!report.isHealthy) {
            healthFailures.inc()
        }
    }
}
```

### ❌ DON'T: Ignore Failed Health Checks

```kotlin
// ❌ BAD: Ignoring health issues
val report = monitor.checkHealth(fingerprint, contextDir)
// No action taken

// ✅ GOOD: Take action
val report = monitor.checkHealth(fingerprint, contextDir)
if (!report.isHealthy) {
    handleHealthIssues(report)
}
```

## Performance Best Practices

### ✅ DO: Reuse Generator/Validator Instances

Instances are thread-safe and reusable.

```kotlin
// ✅ GOOD: Reuse instances
class FingerprintService {
    private val generator = FingerprintGenerator()
    private val validator = FingerprintValidator()
    
    fun createFingerprint(): Fingerprint {
        val fp = generator.generate(...)
        validator.validate(fp)
        return fp
    }
}
```

```kotlin
// ❌ BAD: Creating new instances
fun createFingerprint(): Fingerprint {
    val generator = FingerprintGenerator()  // Wasteful
    val validator = FingerprintValidator()  // Wasteful
    val fp = generator.generate(...)
    validator.validate(fp)
    return fp
}
```

### ✅ DO: Cache Loaded Fingerprints

Avoid repeated file I/O.

```kotlin
class FingerprintCache {
    private val cache = ConcurrentHashMap<Path, Fingerprint>()
    
    fun getFingerprint(contextDir: Path): Fingerprint {
        return cache.computeIfAbsent(contextDir) {
            loadFingerprint(it)
        }
    }
    
    fun invalidate(contextDir: Path) {
        cache.remove(contextDir)
    }
}
```

### ✅ DO: Batch Operations When Possible

```kotlin
// ✅ GOOD: Batch validation
val fingerprints = loadManyFingerprints()
val validator = FingerprintValidator()

val results = fingerprints.parallelStream()
    .map { fp -> fp to validator.validate(fp) }
    .collect(Collectors.toList())

results.forEach { (fp, result) ->
    if (!result.isValid) {
        handleInvalid(fp, result)
    }
}
```

### ❌ DON'T: Perform I/O in Hot Paths

```kotlin
// ❌ BAD: Loading on every request
fun handleRequest() {
    val fingerprint = loadFingerprint(contextDir)  // Slow I/O
    // ... use fingerprint
}

// ✅ GOOD: Load once, cache
val fingerprint = cache.getFingerprint(contextDir)
```

## Security Best Practices

### ✅ DO: Protect Fingerprint Files

```kotlin
// Set restrictive permissions
Files.setPosixFilePermissions(fingerprintPath,
    PosixFilePermissions.fromString("rw-------"))

// Verify permissions before loading
val permissions = Files.getPosixFilePermissions(fingerprintPath)
if (permissions != PosixFilePermissions.fromString("rw-------")) {
    logger.warn("Fingerprint has insecure permissions: $permissions")
    // Fix permissions
    Files.setPosixFilePermissions(fingerprintPath,
        PosixFilePermissions.fromString("rw-------"))
}
```

### ✅ DO: Validate Input from Untrusted Sources

```kotlin
fun loadFingerprintFromUser(json: String): Fingerprint {
    // Parse
    val fingerprint = try {
        pulsarObjectMapper().readValue(json, Fingerprint::class.java)
    } catch (e: JsonProcessingException) {
        throw IllegalArgumentException("Invalid JSON", e)
    }
    
    // Validate
    val result = validator.validate(fingerprint)
    if (!result.isValid) {
        throw IllegalArgumentException("Invalid fingerprint: ${result.errors}")
    }
    
    // Sanitize sensitive fields if needed
    return fingerprint.copy(
        proxyURI = null,  // Don't accept proxy from user
        websiteAccounts = null  // Don't accept accounts from user
    )
}
```

### ✅ DO: Use Checksums for Integrity

```kotlin
fun saveFingerprintWithChecksum(fingerprint: Fingerprint, contextDir: Path) {
    val json = pulsarObjectMapper()
        .writerWithDefaultPrettyPrinter()
        .writeValueAsString(fingerprint)
    
    // Calculate checksum
    val checksum = MessageDigest.getInstance("SHA-256")
        .digest(json.toByteArray())
        .joinToString("") { "%02x".format(it) }
    
    // Save fingerprint
    Files.writeString(contextDir.resolve("fingerprint.json"), json)
    
    // Save checksum
    Files.writeString(contextDir.resolve("fingerprint.json.sha256"), checksum)
}

fun loadFingerprintWithVerification(contextDir: Path): Fingerprint {
    val json = Files.readString(contextDir.resolve("fingerprint.json"))
    val storedChecksum = Files.readString(contextDir.resolve("fingerprint.json.sha256"))
    
    // Verify checksum
    val actualChecksum = MessageDigest.getInstance("SHA-256")
        .digest(json.toByteArray())
        .joinToString("") { "%02x".format(it) }
    
    if (actualChecksum != storedChecksum) {
        throw SecurityException("Fingerprint checksum mismatch - file may be corrupted or tampered")
    }
    
    return pulsarObjectMapper().readValue(json, Fingerprint::class.java)
}
```

### ❌ DON'T: Log Sensitive Fingerprint Data

```kotlin
// ❌ BAD: Logging full fingerprint
logger.info("Fingerprint: $fingerprint")  // May contain sensitive data

// ✅ GOOD: Log only necessary info
logger.info("Fingerprint loaded: type=${fingerprint.browserType}, version=${fingerprint.version}")
```

### ❌ DON'T: Use Predictable Canvas Seeds

```kotlin
// ❌ BAD: Predictable seed
val canvas = CanvasParameters(seed = 12345)  // Same for everyone

// ✅ GOOD: Random unique seed
val canvas = CanvasParameters(
    seed = SecureRandom().nextLong()  // Unique per profile
)
```

## Testing Best Practices

### ✅ DO: Test With Real Browsers

```kotlin
@Test
fun testFingerprintApplicationInBrowser() = runWebDriverTest(testUrl) { driver ->
    // Verify screen parameters applied
    val width = driver.executeScript("return screen.width") as Long
    assertEquals(1920, width)
    
    // Verify navigator properties
    val hardwareConcurrency = driver.executeScript(
        "return navigator.hardwareConcurrency"
    ) as Long
    assertEquals(8, hardwareConcurrency)
}
```

### ✅ DO: Test All Device Presets

```kotlin
@Test
fun testAllDevicePresets() {
    DevicePreset.values().forEach { preset ->
        val fingerprint = generator.generate(
            BrowserType.PULSAR_CHROME,
            preset
        )
        
        val result = validator.validate(fingerprint)
        assertTrue(result.isValid, 
                   "Preset $preset should generate valid fingerprint")
    }
}
```

### ✅ DO: Test Edge Cases

```kotlin
@Test
fun testHighResolutionScreen() {
    val fingerprint = generator.generate(
        BrowserType.PULSAR_CHROME,
        DevicePreset.MACBOOK_PRO_13
    )
    
    // Verify Retina display (2x pixel ratio)
    assertEquals(2.0, fingerprint.screenParameters?.devicePixelRatio)
    assertEquals(2.0, fingerprint.viewportParameters?.deviceScaleFactor)
}

@Test
fun testNonEnglishLocale() {
    val fingerprint = generator.generate(
        BrowserType.PULSAR_CHROME,
        DevicePreset.DESKTOP_WINDOWS
    ).copy(
        geoTimeParameters = GeoTimeParameters(
            timezone = "Asia/Shanghai",
            locale = "zh-CN",
            languages = listOf("zh-CN", "zh")
        )
    )
    
    val result = validator.validate(fingerprint)
    assertTrue(result.isValid)
}
```

### ✅ DO: Test Persistence

```kotlin
@Test
fun testFingerprintPersistence() {
    val original = generator.generate(
        BrowserType.PULSAR_CHROME,
        DevicePreset.DESKTOP_WINDOWS
    )
    
    // Save
    saveFingerprint(original, testDir)
    
    // Load
    val loaded = loadFingerprint(testDir)
    
    // Verify identical
    assertEquals(original, loaded)
    
    // Verify no drift
    val report = detector.detectDrift(original, loaded)
    assertFalse(report.hasDrift)
}
```

### ❌ DON'T: Skip Integration Tests

```kotlin
// ❌ BAD: Only unit tests
@Test
fun testFingerprintGeneration() {
    val fingerprint = generator.generate(...)
    assertNotNull(fingerprint)
}

// ✅ GOOD: Include integration tests
@IntegrationTest
@Test
fun testFingerprintInRealBrowser() = runWebDriverTest(...) { driver ->
    // Test with actual browser
}
```

## Common Pitfalls

### Pitfall 1: Modifying Fingerprints

**Problem**: Changing fingerprint causes drift.

```kotlin
// ❌ Wrong
val modified = fingerprint.copy(
    screenParameters = newScreen
)
```

**Solution**: Generate new fingerprint instead.

```kotlin
// ✅ Correct
val newFingerprint = generator.generate(...)
```

### Pitfall 2: Inconsistent Parameters

**Problem**: Manual construction with inconsistent values.

```kotlin
// ❌ Wrong
val fingerprint = Fingerprint(
    userAgent = "Mozilla/5.0 (Windows...)",
    hardwareParameters = HardwareParameters(
        platform = "MacIntel"  // Inconsistent!
    )
)
```

**Solution**: Use presets or validate.

```kotlin
// ✅ Correct
val fingerprint = generator.generate(
    BrowserType.PULSAR_CHROME,
    DevicePreset.DESKTOP_WINDOWS
)
val result = validator.validate(fingerprint)
require(result.isValid)
```

### Pitfall 3: Not Validating After Load

**Problem**: Loading corrupted or edited fingerprints.

```kotlin
// ❌ Wrong
val fingerprint = loadFingerprint(path)
// Use without validation
```

**Solution**: Always validate.

```kotlin
// ✅ Correct
val fingerprint = loadFingerprint(path)
val result = validator.validate(fingerprint)
if (!result.isValid) {
    // Handle invalid fingerprint
}
```

### Pitfall 4: Regenerating on Every Use

**Problem**: Defeating the purpose of stable identity.

```kotlin
// ❌ Wrong
fun getProfile(): Profile {
    return Profile(
        fingerprint = generator.generate(...)  // New every time!
    )
}
```

**Solution**: Generate once, persist, reuse.

```kotlin
// ✅ Correct
fun getProfile(contextDir: Path): Profile {
    return BrowserProfile.create(contextDir)  // Reuses existing
}
```

### Pitfall 5: Ignoring Warnings

**Problem**: Missing potential issues.

```kotlin
// ❌ Wrong
val result = validator.validate(fingerprint)
if (result.isValid) {
    // Proceed, ignoring warnings
}
```

**Solution**: Review and handle warnings.

```kotlin
// ✅ Correct
val result = validator.validate(fingerprint)
if (result.isValid) {
    if (result.warnings.isNotEmpty()) {
        logger.warn("Fingerprint warnings: ${result.warnings}")
        // Decide if acceptable
    }
}
```

## Production Checklist

Before deploying to production:

### Generation
- [ ] Using device presets (not manual construction)
- [ ] Appropriate preset for use case selected
- [ ] All generated fingerprints validated
- [ ] Canvas seeds are unique per profile

### Persistence
- [ ] Atomic file operations implemented
- [ ] Backup strategy in place
- [ ] File permissions secured (user-only access)
- [ ] Fingerprints excluded from version control
- [ ] Checksums used for integrity verification

### Validation
- [ ] Validation before save implemented
- [ ] Validation on load implemented
- [ ] Warning handling strategy defined
- [ ] Invalid fingerprint recovery process defined

### Monitoring
- [ ] Periodic health checks scheduled
- [ ] Drift detection implemented
- [ ] Metrics collection in place
- [ ] Alerting configured for failures
- [ ] Log levels appropriately set

### Security
- [ ] File permissions restrictive
- [ ] Checksums verify integrity
- [ ] Sensitive data not logged
- [ ] Input validation for untrusted sources
- [ ] Regular security audits scheduled

### Performance
- [ ] Generator/validator instances reused
- [ ] Fingerprints cached where appropriate
- [ ] Batch operations used for bulk tasks
- [ ] I/O minimized in hot paths

### Testing
- [ ] All device presets tested
- [ ] Integration tests with real browsers
- [ ] Edge cases covered
- [ ] Persistence tested
- [ ] Performance benchmarks established

### Documentation
- [ ] Team trained on best practices
- [ ] Runbooks created for common issues
- [ ] Monitoring dashboards configured
- [ ] Incident response procedures documented

---

## Summary

Key takeaways:

1. **Use Presets**: Always prefer device presets over manual construction
2. **Validate Always**: Validate on generate, save, and load
3. **Generate Once**: Create fingerprint once and reuse for stability
4. **Monitor Health**: Implement periodic health checks and drift detection
5. **Secure Files**: Protect fingerprints with proper permissions and checksums
6. **Test Thoroughly**: Include integration tests with real browsers
7. **Handle Errors**: Don't ignore validation warnings or health check failures
8. **Cache Smart**: Reuse instances and cache loaded fingerprints

Following these practices ensures stable, secure, and performant browser profiles in production.

---

**Last Updated**: 2026-02-08  
**Version**: 1.0  
**Status**: Production Ready
