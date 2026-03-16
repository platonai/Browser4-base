package ai.platon.pulsar.basic.collect

import ai.platon.pulsar.basic.TestBase
import ai.platon.pulsar.common.printlnPro
import ai.platon.pulsar.common.urls.sites.amazon.AmazonUrls
import ai.platon.pulsar.common.urls.sites.amazon.AsinUrlNormalizer
import ai.platon.pulsar.skeleton.common.collect.HyperlinkExtractor
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TestHyperlinkExtractors : TestBase() {
    val portalUrl = "https://www.amazon.com/gp/most-wished-for/toys-and-games/251938011"

    @Test
    fun testHyperlinkExtractor() {
        val page = session.load(portalUrl)
        val document = session.parse(page)
        val normalizer = AsinUrlNormalizer()
        val extractor = HyperlinkExtractor(page, document, "a[href~=/dp/]", normalizer)
        val links = extractor.extract()

        links.forEach { printlnPro(it) }
        links.forEach {
            val asin = AmazonUrls.findAsin(it.url) ?: ""
            val href = it.href
            assertNotNull(href)
            assertTrue { asin in href }
            assertTrue { it.referrer == portalUrl }
        }
    }
}

