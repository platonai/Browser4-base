package ai.platon.pulsar.basic.session

import ai.platon.pulsar.basic.TestBase
import kotlin.test.*

/**
 * Test for URL normalization with special characters
 * Issue: URL with empty query parameter value is normalized to nil
 */
class URLNormalizationTest : TestBase() {
    
    @Test
    fun `session normalize should handle URL with empty query parameter value`() {
        val url = "http://localhost:18080/test?param="
        val normalized = session.normalize(url)

        assertNotNull(normalized, "URL with empty query parameter value should be normalized")
        assertFalse(normalized.isNil, "Should not result in nil page")
        assertTrue(normalized.spec.contains("param="), "Query parameter should be preserved")
    }
    
    @Test
    fun `session normalize should handle URL with multiple empty query parameters`() {
        val url = "http://example.com/path?a=&b=&c=value"
        val normalized = session.normalize(url)
        
        assertNotNull(normalized, "URL should be normalized")
        assertFalse(normalized.isNil, "Should not result in nil page")
    }
    
    @Test
    fun `session normalize should handle URL with query parameter and fragment`() {
        val url = "http://example.com/test?param=#fragment"
        val normalized = session.normalize(url)
        
        assertNotNull(normalized, "URL should be normalized")
        assertFalse(normalized.isNil, "Should not result in nil page")
        assertFalse(normalized.spec.contains("#"), "Fragment should be removed")
    }
}
