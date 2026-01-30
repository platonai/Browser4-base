package ai.platon.pulsar.heavy.ql

import ai.platon.pulsar.common.printlnPro
import ai.platon.pulsar.common.sql.SQLTemplate
import ai.platon.pulsar.ql.h2.DomToH2Queries
import ai.platon.pulsar.ql.h2.utils.ResultSetUtils
import ai.platon.pulsar.test.TestUrls
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestExtractCases : TestBase() {
    private val newsIndexUrl = TestUrls.NEWS_INDEX_URL
    private val newsDetailUrl = TestUrls.NEWS_DETAIL_URL

    @Test
    fun testSavePages() {
        execute("CALL ADMIN_SAVE('$productIndexUrl', 'product.index.html')")
        execute("CALL ADMIN_SAVE('$productDetailUrl', 'product.detail.html')")
        execute("CALL ADMIN_SAVE('$newsIndexUrl', 'news.index.html')")
        execute("CALL ADMIN_SAVE('$newsDetailUrl', 'news.detail.html')")
    }

    @Test
    fun testLoadAndGetFeatures() {
        execute("SELECT * FROM LOAD_AND_GET_FEATURES('$productIndexUrl -expires 1d') LIMIT 20")
    }

    @Test
    fun testLoadAndGetLinks() {
        // val expr = "div:expr(WIDTH>=210 && WIDTH<=230 && HEIGHT>=400 && HEIGHT<=420 && SIBLING>30 ) a[href~=item]"
        val expr = "a[href~=item]"
        execute("SELECT * FROM LOAD_AND_GET_LINKS('$productIndexUrl -expires 1s', '$expr')")
    }

    @Test
    fun testLoadAndGetAnchors() {
        // val expr = "div:expr(WIDTH>=210 && WIDTH<=230 && HEIGHT>=400 && HEIGHT<=420 && SIBLING>30 ) a[href~=item]"
        val expr = "a[href~=item]"
        execute("SELECT * FROM LOAD_AND_GET_ANCHORS('$productIndexUrl -expires 1d', '$expr')")
    }

    @Test
    fun testLoadOutPages() {
        val limit = 20
        val pages = DomToH2Queries.loadOutPages(ai.platon.pulsar.ql.TestBase.Companion.session, url, restrictCss, 1, limit)
        pages.map { it.url }.distinct().forEachIndexed { i, url -> printlnPro("$i.\t$url") }
        assertTrue("Page size: " + pages.size) { pages.size <= limit }
    }
}
