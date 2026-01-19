package ai.platon.pulsar.skeleton.crawl.protocol.http

import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.skeleton.crawl.protocol.Response
import org.apache.tika.mime.MimeTypes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TestAbstractHttpProtocolResolveMimeType {

    private class TestProtocol : AbstractHttpProtocol() {
        fun resolve(contentType: String?, url: String, data: ByteArray?): String? =
            resolveMimeType(contentType, url, data)

        override fun getResponse(page: WebPage, followRedirects: Boolean): Response? = null

        override suspend fun getResponseDeferred(page: WebPage, followRedirects: Boolean): Response? = null
    }

    private val protocol = TestProtocol()

    @Test
    fun `should strip content-type parameters`() {
        assertEquals("text/html", protocol.resolve("text/html; charset=UTF-8", "http://example.com", null))
    }

    @Test
    fun `should treat blank content-type as absent and fall back to url`() {
        // Extension-based detection (no body)
        assertEquals("text/html", protocol.resolve("  ", "http://example.com/a/b/index.html", null))
    }

    @Test
    fun `should use tika to detect from bytes when content-type missing`() {
        val html = "<html><body>ok</body></html>".toByteArray(Charsets.UTF_8)
        assertEquals("text/html", protocol.resolve(null, "http://example.com/", html))
    }

    @Test
    fun `should return deterministic value for empty body and no content-type`() {
        val resolved = protocol.resolve(null, "http://example.com/", ByteArray(0))
        assertNotNull(resolved)
        // With no extension and no body, octet-stream is the safest fallback.
        assertEquals(MimeTypes.OCTET_STREAM, resolved)
    }
}
