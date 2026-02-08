package ai.platon.pulsar.common.browser

/**
 * Screen parameters for browser fingerprinting.
 *
 * These parameters define the screen characteristics that will be exposed via JavaScript's
 * screen object and window.devicePixelRatio.
 *
 * @property width Screen width in pixels
 * @property height Screen height in pixels
 * @property availWidth Available screen width (excluding system UI)
 * @property availHeight Available screen height (excluding system UI)
 * @property colorDepth Color depth in bits (typically 24 or 32)
 * @property pixelDepth Pixel depth in bits (typically same as colorDepth)
 * @property devicePixelRatio Device pixel ratio (1.0, 1.5, 2.0, etc.)
 * @property orientation Screen orientation ("landscape-primary" or "portrait-primary")
 */
data class ScreenParameters(
    val width: Int,
    val height: Int,
    val availWidth: Int,
    val availHeight: Int,
    val colorDepth: Int = 24,
    val pixelDepth: Int = 24,
    val devicePixelRatio: Double = 1.0,
    val orientation: String = "landscape-primary"
) {
    init {
        require(width > 0) { "Screen width must be positive" }
        require(height > 0) { "Screen height must be positive" }
        require(availWidth <= width) { "Available width cannot exceed screen width" }
        require(availHeight <= height) { "Available height cannot exceed screen height" }
        require(colorDepth in listOf(24, 30, 32, 48)) { "Color depth must be 24, 30, 32, or 48" }
        require(devicePixelRatio > 0) { "Device pixel ratio must be positive" }
    }

    companion object {
        /**
         * Common desktop screen configuration (1920x1080).
         */
        val DESKTOP_1920X1080 = ScreenParameters(
            width = 1920,
            height = 1080,
            availWidth = 1920,
            availHeight = 1040,
            colorDepth = 24,
            pixelDepth = 24,
            devicePixelRatio = 1.0
        )

        /**
         * Common laptop screen configuration (1366x768).
         */
        val LAPTOP_1366X768 = ScreenParameters(
            width = 1366,
            height = 768,
            availWidth = 1366,
            availHeight = 728,
            colorDepth = 24,
            pixelDepth = 24,
            devicePixelRatio = 1.0
        )

        /**
         * MacBook Pro 13" Retina (2560x1600).
         */
        val MACBOOK_PRO_13 = ScreenParameters(
            width = 2560,
            height = 1600,
            availWidth = 2560,
            availHeight = 1577,
            colorDepth = 24,
            pixelDepth = 24,
            devicePixelRatio = 2.0
        )
    }
}

/**
 * Viewport parameters for browser fingerprinting.
 *
 * These parameters define the browser viewport (visible area) and are used with
 * CDP's Emulation.setDeviceMetricsOverride.
 *
 * @property width Viewport width in pixels
 * @property height Viewport height in pixels
 * @property deviceScaleFactor Device scale factor (usually same as devicePixelRatio)
 * @property isMobile Whether this is a mobile device
 * @property hasTouch Whether the device supports touch
 * @property isLandscape Whether the device is in landscape orientation
 */
data class ViewportParameters(
    val width: Int,
    val height: Int,
    val deviceScaleFactor: Double = 1.0,
    val isMobile: Boolean = false,
    val hasTouch: Boolean = false,
    val isLandscape: Boolean = true
) {
    init {
        require(width > 0) { "Viewport width must be positive" }
        require(height > 0) { "Viewport height must be positive" }
        require(deviceScaleFactor > 0) { "Device scale factor must be positive" }
    }

    companion object {
        /**
         * Common desktop viewport (1920x1080).
         */
        val DESKTOP = ViewportParameters(
            width = 1920,
            height = 1080,
            deviceScaleFactor = 1.0,
            isMobile = false,
            hasTouch = false,
            isLandscape = true
        )

        /**
         * Common laptop viewport (1366x768).
         */
        val LAPTOP = ViewportParameters(
            width = 1366,
            height = 768,
            deviceScaleFactor = 1.0,
            isMobile = false,
            hasTouch = false,
            isLandscape = true
        )
    }
}

/**
 * Geographic and time-related parameters for browser fingerprinting.
 *
 * These parameters define timezone, language, and location settings that will be
 * exposed via JavaScript APIs.
 *
 * @property timezone IANA timezone identifier (e.g., "Asia/Shanghai", "America/New_York")
 * @property timezoneOffset Timezone offset in minutes from UTC
 * @property locale Locale identifier (e.g., "zh-CN", "en-US")
 * @property languages Ordered list of language preferences
 * @property latitude Geographic latitude (optional)
 * @property longitude Geographic longitude (optional)
 * @property accuracy Location accuracy in meters (optional)
 */
data class GeoTimeParameters(
    val timezone: String,
    val timezoneOffset: Int,
    val locale: String,
    val languages: List<String>,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracy: Double? = null
) {
    init {
        require(timezone.isNotBlank()) { "Timezone cannot be blank" }
        require(locale.isNotBlank()) { "Locale cannot be blank" }
        require(languages.isNotEmpty()) { "Languages list cannot be empty" }
        if (latitude != null) {
            require(latitude in -90.0..90.0) { "Latitude must be between -90 and 90" }
        }
        if (longitude != null) {
            require(longitude in -180.0..180.0) { "Longitude must be between -180 and 180" }
        }
        if (accuracy != null) {
            require(accuracy > 0) { "Accuracy must be positive" }
        }
    }

    companion object {
        /**
         * China/Shanghai timezone with Chinese language.
         */
        val CHINA = GeoTimeParameters(
            timezone = "Asia/Shanghai",
            timezoneOffset = -480,
            locale = "zh-CN",
            languages = listOf("zh-CN", "zh", "en")
        )

        /**
         * US/New York timezone with English language.
         */
        val US_EAST = GeoTimeParameters(
            timezone = "America/New_York",
            timezoneOffset = 300,
            locale = "en-US",
            languages = listOf("en-US", "en")
        )

        /**
         * UK/London timezone with English language.
         */
        val UK = GeoTimeParameters(
            timezone = "Europe/London",
            timezoneOffset = 0,
            locale = "en-GB",
            languages = listOf("en-GB", "en")
        )
    }
}

/**
 * Hardware-related parameters for browser fingerprinting.
 *
 * These parameters define hardware characteristics exposed via navigator object.
 *
 * @property hardwareConcurrency Number of logical CPU cores
 * @property deviceMemory Amount of device memory in GB (optional, not all browsers expose this)
 * @property maxTouchPoints Maximum number of simultaneous touch points
 * @property platform Platform identifier (e.g., "Win32", "MacIntel", "Linux x86_64")
 * @property vendor Browser vendor string
 * @property vendorSub Browser vendor sub-string (usually empty)
 * @property productSub Browser product sub-string (usually "20030107")
 */
data class HardwareParameters(
    val hardwareConcurrency: Int,
    val deviceMemory: Int? = null,
    val maxTouchPoints: Int = 0,
    val platform: String,
    val vendor: String = "Google Inc.",
    val vendorSub: String = "",
    val productSub: String = "20030107"
) {
    init {
        require(hardwareConcurrency > 0) { "Hardware concurrency must be positive" }
        require(deviceMemory == null || deviceMemory > 0) { "Device memory must be positive if specified" }
        require(maxTouchPoints >= 0) { "Max touch points cannot be negative" }
        require(platform.isNotBlank()) { "Platform cannot be blank" }
    }

    companion object {
        /**
         * Typical Windows desktop (8 cores, 8GB RAM).
         */
        val WINDOWS_DESKTOP = HardwareParameters(
            hardwareConcurrency = 8,
            deviceMemory = 8,
            maxTouchPoints = 0,
            platform = "Win32",
            vendor = "Google Inc."
        )

        /**
         * Typical MacBook (8 cores, 16GB RAM).
         */
        val MAC_LAPTOP = HardwareParameters(
            hardwareConcurrency = 8,
            deviceMemory = 16,
            maxTouchPoints = 0,
            platform = "MacIntel",
            vendor = "Apple Computer, Inc."
        )

        /**
         * Typical Linux desktop (4 cores, 8GB RAM).
         */
        val LINUX_DESKTOP = HardwareParameters(
            hardwareConcurrency = 4,
            deviceMemory = 8,
            maxTouchPoints = 0,
            platform = "Linux x86_64",
            vendor = "Google Inc."
        )
    }
}

/**
 * WebGL-related parameters for browser fingerprinting.
 *
 * These parameters define WebGL capabilities and are used to generate consistent
 * WebGL fingerprints.
 *
 * @property vendor WebGL vendor string
 * @property renderer WebGL renderer string
 * @property unmaskedVendor Unmasked GPU vendor (may be null for privacy)
 * @property unmaskedRenderer Unmasked GPU renderer (may be null for privacy)
 * @property shadingLanguageVersion GLSL version string
 * @property maxTextureSize Maximum texture size
 * @property maxViewportDims Maximum viewport dimensions
 */
data class WebGLParameters(
    val vendor: String,
    val renderer: String,
    val unmaskedVendor: String? = null,
    val unmaskedRenderer: String? = null,
    val shadingLanguageVersion: String = "WebGL GLSL ES 1.0",
    val maxTextureSize: Int = 16384,
    val maxViewportDims: List<Int> = listOf(16384, 16384)
) {
    init {
        require(vendor.isNotBlank()) { "WebGL vendor cannot be blank" }
        require(renderer.isNotBlank()) { "WebGL renderer cannot be blank" }
        require(maxTextureSize > 0) { "Max texture size must be positive" }
        require(maxViewportDims.size == 2) { "Max viewport dims must have exactly 2 elements" }
    }

    companion object {
        /**
         * Intel integrated graphics (common on laptops).
         */
        val INTEL_INTEGRATED = WebGLParameters(
            vendor = "Google Inc. (Intel)",
            renderer = "ANGLE (Intel, Intel(R) UHD Graphics Direct3D11 vs_5_0 ps_5_0)",
            unmaskedVendor = "Intel Inc.",
            unmaskedRenderer = "Intel(R) UHD Graphics"
        )

        /**
         * NVIDIA discrete graphics (common on gaming PCs).
         */
        val NVIDIA_DISCRETE = WebGLParameters(
            vendor = "Google Inc. (NVIDIA)",
            renderer = "ANGLE (NVIDIA, NVIDIA GeForce GTX 1650 Direct3D11 vs_5_0 ps_5_0)",
            unmaskedVendor = "NVIDIA Corporation",
            unmaskedRenderer = "NVIDIA GeForce GTX 1650"
        )

        /**
         * Apple M1 GPU (MacBooks).
         */
        val APPLE_M1 = WebGLParameters(
            vendor = "Apple Inc.",
            renderer = "Apple M1",
            unmaskedVendor = "Apple Inc.",
            unmaskedRenderer = "Apple M1"
        )
    }
}

/**
 * Canvas fingerprinting parameters.
 *
 * Canvas fingerprinting works by drawing text/shapes and reading pixel data.
 * Different hardware/drivers produce slightly different pixel values.
 *
 * @property fingerprintSeed Seed for generating deterministic canvas fingerprint noise.
 *                          If null, canvas fingerprinting is not modified.
 */
data class CanvasParameters(
    val fingerprintSeed: String? = null
) {
    companion object {
        /**
         * Default canvas parameters (no modification).
         */
        val DEFAULT = CanvasParameters(fingerprintSeed = null)
    }
}

/**
 * Media device information for enumerateDevices API.
 *
 * @property deviceId Unique device identifier
 * @property label Human-readable device label
 * @property kind Device kind ("audioinput", "audiooutput", "videoinput")
 */
data class MediaDevice(
    val deviceId: String,
    val label: String,
    val kind: String
) {
    init {
        require(deviceId.isNotBlank()) { "Device ID cannot be blank" }
        require(kind in listOf("audioinput", "audiooutput", "videoinput")) {
            "Kind must be audioinput, audiooutput, or videoinput"
        }
    }
}

/**
 * Media device parameters for browser fingerprinting.
 *
 * These parameters define the list of media devices that will be returned by
 * navigator.mediaDevices.enumerateDevices().
 *
 * @property audioInputDevices List of audio input devices
 * @property audioOutputDevices List of audio output devices
 * @property videoInputDevices List of video input devices
 */
data class MediaParameters(
    val audioInputDevices: List<MediaDevice> = emptyList(),
    val audioOutputDevices: List<MediaDevice> = emptyList(),
    val videoInputDevices: List<MediaDevice> = emptyList()
) {
    companion object {
        /**
         * Typical desktop media configuration.
         */
        val DESKTOP = MediaParameters(
            audioInputDevices = listOf(
                MediaDevice(
                    deviceId = "default",
                    label = "Default - Microphone (Realtek High Definition Audio)",
                    kind = "audioinput"
                )
            ),
            audioOutputDevices = listOf(
                MediaDevice(
                    deviceId = "default",
                    label = "Default - Speakers (Realtek High Definition Audio)",
                    kind = "audiooutput"
                )
            ),
            videoInputDevices = listOf(
                MediaDevice(
                    deviceId = "default",
                    label = "HD Webcam (0bda:58b0)",
                    kind = "videoinput"
                )
            )
        )
    }
}

/**
 * Plugin information (mostly deprecated in modern browsers).
 *
 * @property name Plugin name
 * @property description Plugin description
 * @property filename Plugin filename
 */
data class PluginInfo(
    val name: String,
    val description: String,
    val filename: String
)

/**
 * MIME type information (mostly deprecated in modern browsers).
 *
 * @property type MIME type string
 * @property description MIME type description
 * @property suffixes File suffixes for this MIME type
 */
data class MimeTypeInfo(
    val type: String,
    val description: String,
    val suffixes: String
)

/**
 * Miscellaneous browser parameters for fingerprinting.
 *
 * @property doNotTrack Do Not Track header value ("1", "unspecified", or null)
 * @property cookieEnabled Whether cookies are enabled
 * @property pdfViewerEnabled Whether PDF viewer is enabled
 * @property plugins List of browser plugins (usually empty in modern browsers)
 * @property mimeTypes List of supported MIME types (usually empty in modern browsers)
 */
data class MiscParameters(
    val doNotTrack: String? = null,
    val cookieEnabled: Boolean = true,
    val pdfViewerEnabled: Boolean = true,
    val plugins: List<PluginInfo> = emptyList(),
    val mimeTypes: List<MimeTypeInfo> = emptyList()
) {
    companion object {
        /**
         * Default miscellaneous parameters.
         */
        val DEFAULT = MiscParameters(
            doNotTrack = null,
            cookieEnabled = true,
            pdfViewerEnabled = true
        )
    }
}
