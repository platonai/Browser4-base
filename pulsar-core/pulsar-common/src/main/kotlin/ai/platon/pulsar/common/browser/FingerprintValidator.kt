package ai.platon.pulsar.common.browser

/**
 * Result of fingerprint validation.
 *
 * @property isValid Whether the fingerprint passed all validation checks
 * @property errors List of validation error messages
 * @property warnings List of validation warning messages
 */
data class ValidationResult(
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
) {
    val isValid: Boolean get() = errors.isEmpty()
    
    val hasWarnings: Boolean get() = warnings.isNotEmpty()
    
    /**
     * Get a human-readable summary of the validation result.
     */
    fun summary(): String {
        return when {
            isValid && !hasWarnings -> "Validation passed"
            isValid && hasWarnings -> "Validation passed with ${warnings.size} warnings"
            else -> "Validation failed with ${errors.size} errors"
        }
    }
    
    override fun toString(): String {
        val sb = StringBuilder(summary())
        if (errors.isNotEmpty()) {
            sb.append("\nErrors:")
            errors.forEach { sb.append("\n  - $it") }
        }
        if (warnings.isNotEmpty()) {
            sb.append("\nWarnings:")
            warnings.forEach { sb.append("\n  - $it") }
        }
        return sb.toString()
    }
}

/**
 * Validates browser fingerprints for consistency and reasonability.
 *
 * A valid fingerprint should have:
 * - Consistent parameters (userAgent matches platform, screen matches viewport, etc.)
 * - Reasonable values (common screen sizes, realistic hardware specs, etc.)
 * - Coherent geography (timezone matches language/locale)
 *
 * This validator helps ensure that generated fingerprints will not be easily detected
 * as fake by anti-fingerprinting systems.
 */
class FingerprintValidator {
    
    /**
     * Validate a fingerprint for consistency and reasonability.
     *
     * @param fingerprint The fingerprint to validate
     * @return ValidationResult containing any errors or warnings
     */
    fun validate(fingerprint: Fingerprint): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        // Check basic parameters
        validateBasicParameters(fingerprint, errors)
        
        // Check userAgent consistency with platform and hardware
        validateUserAgentConsistency(fingerprint, errors, warnings)
        
        // Check screen and viewport consistency
        validateScreenViewportConsistency(fingerprint, errors, warnings)
        
        // Check hardware reasonability
        validateHardwareReasonability(fingerprint, errors, warnings)
        
        // Check geo-time consistency
        validateGeoTimeConsistency(fingerprint, errors, warnings)
        
        // Check WebGL consistency
        validateWebGLConsistency(fingerprint, errors, warnings)
        
        return ValidationResult(errors, warnings)
    }
    
    private fun validateBasicParameters(fingerprint: Fingerprint, errors: MutableList<String>) {
        // User agent should be present if other parameters are specified
        if (fingerprint.userAgent == null) {
            if (fingerprint.screenParameters != null || 
                fingerprint.hardwareParameters != null ||
                fingerprint.geoTimeParameters != null) {
                errors.add("User agent is required when other fingerprint parameters are specified")
            }
        }
    }
    
    private fun validateUserAgentConsistency(
        fingerprint: Fingerprint,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        val userAgent = fingerprint.userAgent ?: return
        val hardware = fingerprint.hardwareParameters ?: return
        
        // Check platform consistency
        when {
            "Windows" in userAgent && hardware.platform != "Win32" -> {
                errors.add("User agent indicates Windows but platform is ${hardware.platform}")
            }
            "Macintosh" in userAgent && hardware.platform != "MacIntel" -> {
                errors.add("User agent indicates Mac but platform is ${hardware.platform}")
            }
            "Linux" in userAgent && !hardware.platform.startsWith("Linux") -> {
                errors.add("User agent indicates Linux but platform is ${hardware.platform}")
            }
        }
        
        // Check vendor consistency
        when {
            "Chrome" in userAgent || "Chromium" in userAgent -> {
                if (hardware.vendor != "Google Inc.") {
                    warnings.add("Chrome user agent typically has vendor 'Google Inc.', got '${hardware.vendor}'")
                }
            }
            "Safari" in userAgent && "Chrome" !in userAgent -> {
                if (hardware.vendor != "Apple Computer, Inc.") {
                    warnings.add("Safari user agent typically has vendor 'Apple Computer, Inc.', got '${hardware.vendor}'")
                }
            }
        }
    }
    
    private fun validateScreenViewportConsistency(
        fingerprint: Fingerprint,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        val screen = fingerprint.screenParameters ?: return
        val viewport = fingerprint.viewportParameters ?: return
        
        // Viewport should not exceed screen size
        if (viewport.width > screen.width) {
            errors.add("Viewport width (${viewport.width}) exceeds screen width (${screen.width})")
        }
        if (viewport.height > screen.height) {
            errors.add("Viewport height (${viewport.height}) exceeds screen height (${screen.height})")
        }
        
        // Device scale factor should match device pixel ratio
        val scaleDiff = kotlin.math.abs(viewport.deviceScaleFactor - screen.devicePixelRatio)
        if (scaleDiff > 0.01) {
            warnings.add(
                "Device scale factor (${viewport.deviceScaleFactor}) differs from " +
                "device pixel ratio (${screen.devicePixelRatio})"
            )
        }
        
        // Check reasonable viewport size
        val viewportRatio = viewport.width.toDouble() / viewport.height
        if (viewportRatio < 0.5 || viewportRatio > 4.0) {
            warnings.add("Unusual viewport aspect ratio: ${String.format("%.2f", viewportRatio)}")
        }
    }
    
    private fun validateHardwareReasonability(
        fingerprint: Fingerprint,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        val hardware = fingerprint.hardwareParameters ?: return
        
        // Check reasonable CPU core count (1-128)
        if (hardware.hardwareConcurrency > 128) {
            errors.add("Hardware concurrency (${hardware.hardwareConcurrency}) exceeds reasonable limit")
        }
        
        // Check reasonable memory size
        hardware.deviceMemory?.let { memory ->
            if (memory > 256) {
                errors.add("Device memory (${memory}GB) exceeds reasonable limit")
            }
            if (memory < 1) {
                warnings.add("Device memory (${memory}GB) is unusually low")
            }
        }
        
        // Check touch points reasonability
        if (hardware.maxTouchPoints > 10) {
            warnings.add("Max touch points (${hardware.maxTouchPoints}) is unusually high")
        }
        
        // Mobile devices should have touch support
        fingerprint.viewportParameters?.let { viewport ->
            if (viewport.isMobile && hardware.maxTouchPoints == 0) {
                warnings.add("Mobile device should typically have maxTouchPoints > 0")
            }
            if (!viewport.isMobile && hardware.maxTouchPoints > 0) {
                warnings.add("Desktop device typically has maxTouchPoints = 0")
            }
        }
    }
    
    private fun validateGeoTimeConsistency(
        fingerprint: Fingerprint,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        val geoTime = fingerprint.geoTimeParameters ?: return
        
        // Check timezone format
        if (!geoTime.timezone.matches(Regex("^[A-Za-z]+/[A-Za-z_]+$"))) {
            warnings.add("Timezone '${geoTime.timezone}' may not be a valid IANA timezone")
        }
        
        // Check timezone offset reasonability (-12 hours to +14 hours)
        if (geoTime.timezoneOffset < -720 || geoTime.timezoneOffset > 840) {
            warnings.add("Timezone offset ${geoTime.timezoneOffset} is outside normal range")
        }
        
        // Check language consistency with locale
        if (geoTime.languages.isEmpty()) {
            errors.add("Languages list cannot be empty")
        } else {
            val firstLang = geoTime.languages[0]
            if (!firstLang.startsWith(geoTime.locale.substringBefore('-'))) {
                warnings.add(
                    "First language '$firstLang' does not match locale '${geoTime.locale}'"
                )
            }
        }
        
        // Check coordinates if provided
        if (geoTime.latitude != null && geoTime.longitude == null) {
            errors.add("Latitude provided without longitude")
        }
        if (geoTime.longitude != null && geoTime.latitude == null) {
            errors.add("Longitude provided without latitude")
        }
    }
    
    private fun validateWebGLConsistency(
        fingerprint: Fingerprint,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        val webgl = fingerprint.webGLParameters ?: return
        val hardware = fingerprint.hardwareParameters
        
        // Check vendor consistency
        if (hardware != null) {
            when {
                "Intel" in webgl.renderer && "MacIntel" == hardware.platform -> {
                    // Intel on Mac is common
                }
                "Apple" in webgl.renderer && "MacIntel" != hardware.platform -> {
                    warnings.add("Apple GPU typically appears on MacIntel platform")
                }
                "NVIDIA" in webgl.renderer || "AMD" in webgl.renderer -> {
                    // Discrete GPUs can appear on any platform
                }
            }
        }
        
        // Check reasonable texture sizes
        if (webgl.maxTextureSize < 2048) {
            warnings.add("Max texture size ${webgl.maxTextureSize} is unusually low")
        }
        if (webgl.maxTextureSize > 32768) {
            warnings.add("Max texture size ${webgl.maxTextureSize} is unusually high")
        }
    }
}
