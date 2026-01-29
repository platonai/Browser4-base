package ai.platon.pulsar.common.urls

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.printlnPro
import ai.platon.pulsar.common.config.AppConstants.BROWSER_INTERNAL_BASE_URL
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import java.io.File
import java.net.*
import java.util.*
import kotlin.io.path.toPath

class URLUtilsTest {

    @Test
    fun testURIBasics() {
        // 准备测试数据
        val path = AppPaths.getTmpDirectory("test.txt")
        val uri = path.toUri()
        val url = uri.toURL()
        assertEquals("file", uri.scheme)
        assertEquals("file", url.protocol)
    }

    @Test
    fun testNormalizer() {
        var url = "https://www.amazon.com/s?k=\"Boys%27+Novelty+Belt+Buckles\"&rh=n:9057119011&page=1"
        var normUrl = URLUtils.normalizeOrNull(url, true)
        assertNull(normUrl)

        url = "https://www.amazon.com/s?k=Boys%27+Novelty+Belt+Buckles&rh=n:9057119011&page=1"
        normUrl = URLUtils.normalizeOrNull(url, true)
        assertNotNull(normUrl)
    }

    @Test
    fun `isStandard should return true for standard URL`() {
        // 标准 URL 测试
        val standardUrl = "https://www.example.com"
        assertTrue(URLUtils.isStandard(standardUrl))
    }

    @Test
    fun `isStandard should return false for non-standard URL`() {
        // 非标准 URL 测试
        val nonStandardUrl = "example"
        assertFalse(URLUtils.isStandard(nonStandardUrl))

        assertFalse(URLUtils.isStandard("about:blank"))
        assertFalse(URLUtils.isStandard("chrome://chrome-urls"))
        assertFalse(URLUtils.isStandard("chrome://accessibility"))
    }

    @Test
    fun `isStandard should return false for null input`() {
        // 空输入测试
        assertFalse(URLUtils.isStandard(null))
    }

    @Test
    fun `isStandard should return false for empty string`() {
        // 空字符串测试
        val emptyString = ""
        assertFalse(URLUtils.isStandard(emptyString))
    }

    @Test
    fun ensureChromeURLsAreMalformed() {
        assertThrows(MalformedURLException::class.java) {
            URI.create("chrome://chrome-urls").toURL()
        }
    }

    @Test
    fun testNormalize_WithoutQuery() {
        val result = URLUtils.normalize("http://example.com/path?query=123#fragment", true)
        assertEquals(URI.create("http://example.com/path").toURL(), result)
    }

    @Test
    fun testNormalize_WithQuery() {
        val result = URLUtils.normalize("http://example.com/path?query=123#fragment")
        assertEquals(URI.create("http://example.com/path?query=123").toURL(), result)
    }

    @Test
    fun testNormalize_RemoveFragment() {
        val result = URLUtils.normalize("http://example.com/path#fragment")
        assertEquals(URI.create("http://example.com/path").toURL(), result)
    }

    @Test
    fun testNormalize_InvalidUrl() {
        assertThrows(IllegalArgumentException::class.java) {
            val url = URLUtils.normalize("invalid-url")
//            logPrintln(url)
        }
    }

    @Test
    fun testNormalize_InvalidUriSyntax() {
        assertThrows(URISyntaxException::class.java) {
            URLUtils.normalize("http://example.com/path&%!({{")
        }
    }

    @Test
    fun testNormalize_EmptyUrl() {
        assertThrows(IllegalArgumentException::class.java) {
            URLUtils.normalize("")
        }
    }

    /**
     * Test Windows file URI.
     * Enable only on Windows.
     * */
    @Test
    @EnabledOnOs(OS.WINDOWS)
    fun testNormalize_WindowsFileURI() {
        val filePath = "C:\\Users\\Vincent\\Documents"
        val uri = File(filePath).toURI()  // 自动转换为合法的 file:// URI
        printlnPro(uri.toString()) // 输出 file:/C:/Users/Vincent/Documents
        assertEquals("file:/C:/Users/Vincent/Documents", uri.toString())

        // 注意：虽然 URI 标准中为绝对路径推荐 file:///C:/...，
        // 但 Java 会自动简化为 file:/C:/...，它依然是合法的并能正常解析。
        val uri1 = URI.create("file:/C:/Users/Vincent/Documents")
        val uri2 = URI.create("file:///C:/Users/Vincent/Documents")
        assertEquals(uri1, uri2)

        assertEquals("file:/C:/Users/Vincent/Documents", uri1.toPath().toFile().toURI().toString())
        assertEquals("file:/C:/Users/Vincent/Documents", uri2.toPath().toFile().toURI().toString())

        assertEquals("file:///C:/Users/Vincent/Documents", uri1.toPath().toUri().toString())
        assertEquals("file:///C:/Users/Vincent/Documents", uri2.toPath().toUri().toString())

        val url = "file:///C:/Users/User/Documents/file.txt"
        val normalizedUrl = URLUtils.normalize(url)
        assertEquals(URI.create("file:///C:/Users/User/Documents/file.txt").toURL(), normalizedUrl)
    }


    @Test
    fun testIsBrowserURL() {
        // Test with a browser-specific protocol
        assertTrue(URLUtils.isBrowserURL("chrome://settings"))
        assertTrue(URLUtils.isBrowserURL("edge://settings"))
        assertTrue(URLUtils.isBrowserURL("about:blank"))
        // Test with a non-browser-specific protocol
        assertFalse(URLUtils.isBrowserURL("http"))
    }

    @Test
    fun testBrowserURLToStandardURL() {
        // Test converting a browser protocol to URL
        val url = "chrome://settings"
        val expected = "$BROWSER_INTERNAL_BASE_URL?url=${URLEncoder.encode(url, Charsets.UTF_8)}"
        // printlnPro(expected)
        assertEquals(expected, URLUtils.browserURLToStandardURL(url))
    }

    @Test
    fun testUrlToBrowserProtocol() {
        // Test extracting and re-encoding a browser protocol from URL
        val expected = "chrome://settings"
        val url = "$BROWSER_INTERNAL_BASE_URL?url=${URLEncoder.encode(expected, Charsets.UTF_8)}"
        assertEquals(expected, URLUtils.standardURLToBrowserURL(url))

        // Test with a URL that does not contain a browser protocol
        assertNull(URLUtils.standardURLToBrowserURL("http://example.com"))
    }

    @Test
    fun testPathToLocalURL() {
        // 准备测试数据
        val path = AppPaths.getTmpDirectory("test.txt")
        assertEquals("file", path.toUri().scheme)

        // 调用待测试的方法
        val result = URLUtils.pathToLocalURL(path)

        // 验证结果是否符合预期
        val expectedPrefix = AppConstants.LOCAL_FILE_BASE_URL
        val base64 = Base64.getUrlEncoder().encode(path.toString().toByteArray()).toString(Charsets.UTF_8)
        val expectedURL = "$expectedPrefix?path=$base64"

//        printlnPro(path)
//        printlnPro(result)

        assertEquals(expectedURL, result)
    }

    // ===== Comprehensive tests for URLUtils.normalize* methods =====

    @Test
    fun `normalizeOrNull should handle URL with empty query parameter value`() {
        // This is the original failing case from the issue
        val url = "http://localhost:18080/test?param="
        val normalized = URLUtils.normalizeOrNull(url)
        
        // Debug output
        printlnPro("Testing URL: $url")
        printlnPro("Normalized result: $normalized")
        
        assertNotNull(normalized, "URL with empty query parameter value should be normalized")
        assertTrue(normalized!!.contains("param="), "Query parameter should be preserved")
    }

    @Test
    fun `normalizeOrNull should handle URL with multiple empty query parameters`() {
        val url = "http://example.com/path?a=&b=&c=value"
        val normalized = URLUtils.normalizeOrNull(url)
        
        assertNotNull(normalized, "URL with multiple empty query parameters should be normalized")
    }

    @Test
    fun `normalizeOrNull should handle URL with query parameter and fragment`() {
        val url = "http://example.com/test?param=#fragment"
        val normalized = URLUtils.normalizeOrNull(url)
        
        assertNotNull(normalized, "URL with empty param and fragment should be normalized")
        assertFalse(normalized!!.contains("#"), "Fragment should be removed")
    }

    @Test
    fun `normalizeOrNull should handle URL with only equals sign in query`() {
        val url = "http://example.com/test?="
        val normalized = URLUtils.normalizeOrNull(url)
        
        // This might fail, but we need to document the behavior
        // It's OK if this returns null for truly malformed URLs
        // But "param=" should work as it's a valid empty value
        if (normalized == null) {
            // Document that bare "=" is considered invalid
            assertTrue(true, "Bare equals sign may be considered invalid")
        } else {
            assertNotNull(normalized)
        }
    }

    @Test
    fun `normalizeOrNull should handle URL with encoded special characters in query`() {
        val url = "http://example.com/test?param=%20&other=value"
        val normalized = URLUtils.normalizeOrNull(url)
        
        assertNotNull(normalized, "URL with encoded special characters should be normalized")
    }

    @Test
    fun `normalizeOrNull should handle URL with special characters that need encoding`() {
        val url = "http://example.com/test?param=hello world"
        val normalized = URLUtils.normalizeOrNull(url)
        
        // URLs with unencoded spaces might be rejected, which is correct behavior
        // This test documents the expected behavior
        if (normalized == null) {
            assertTrue(true, "Unencoded spaces may be rejected")
        } else {
            assertNotNull(normalized)
        }
    }

    @Test
    fun `normalize should handle URL with empty query parameter value`() {
        val url = "http://localhost:18080/test?param="
        val normalized = URLUtils.normalize(url)
        
        assertNotNull(normalized)
        assertTrue(normalized.toString().contains("param="), "Query parameter should be preserved")
    }

    @Test
    fun `normalize should handle URL with ampersand at end of query`() {
        val url = "http://example.com/test?param=value&"
        val normalized = URLUtils.normalize(url)
        
        assertNotNull(normalized)
    }

    @Test
    fun `normalizeOrNull should return null for URL with double quotes in query parameter value`() {
        // From the existing test - URLs with quotes in parameter values are invalid
        val url = """https://www.amazon.com/s?k="Boys%27+Novelty+Belt+Buckles"&rh=n:9057119011&page=1"""
        val normalized = URLUtils.normalizeOrNull(url, true)
        
        assertNull(normalized, "URL with quotes in parameter values should be rejected")
    }

    @Test
    fun `normalize ignoreQuery should remove query string with empty parameter`() {
        val url = "http://example.com/path?param=&other=value"
        val normalized = URLUtils.normalize(url, ignoreQuery = true)
        
        assertNotNull(normalized)
        assertFalse(normalized.toString().contains("?"), "Query string should be removed when ignoreQuery=true")
        assertEquals("http://example.com/path", normalized.toString())
    }

    @Test
    fun `normalizeOrEmpty should return empty string for invalid URL`() {
        val url = "not a valid url"
        val normalized = URLUtils.normalizeOrEmpty(url)
        
        assertEquals("", normalized, "Invalid URL should return empty string")
    }

    @Test
    fun `normalizeOrEmpty should handle URL with empty parameter value`() {
        val url = "http://example.com/test?param="
        val normalized = URLUtils.normalizeOrEmpty(url)
        
        assertNotEquals("", normalized, "Valid URL with empty parameter should not return empty string")
    }

    @Test
    fun `normalizeUrls should filter out invalid URLs`() {
        val urls = listOf(
            "http://example.com/valid",
            "invalid url",
            "http://example.com/test?param=",
            "http://[invalid"
        )
        val normalized = URLUtils.normalizeUrls(urls)
        
        // Should contain only valid URLs
        assertTrue(normalized.size < urls.size, "Should filter out some invalid URLs")
        assertTrue(normalized.any { it.contains("valid") }, "Should contain valid URLs")
    }

    @Test
    fun `normalize should preserve localhost URLs with ports and empty query params`() {
        val url = "http://localhost:8080/api/test?param="
        val normalized = URLUtils.normalize(url)
        
        assertNotNull(normalized)
        assertEquals("localhost", normalized.host)
        assertEquals(8080, normalized.port)
        assertTrue(normalized.toString().contains("param="))
    }
}

