package ai.platon.pulsar.rest.api.controller

import ai.platon.pulsar.common.printlnPro
import ai.platon.pulsar.common.serialize.json.prettyPulsarObjectMapper
import ai.platon.pulsar.rest.api.TestHelper.MOCK_PRODUCT_DETAIL_URL
import ai.platon.pulsar.rest.api.entities.CommandRequest
import ai.platon.pulsar.rest.api.entities.CommandStatus
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.client.expectBody
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.DisplayName

@Tag("E2ETest")
class CommandControllerE2ETest : RestAPITestBase() {

    /**
     * Test [CommandController.submitCommand]
     * */
    @Test
        @DisplayName("Test submitCommand with pageSummaryPrompt + sync mode")
    fun testSubmitcommandWithPagesummarypromptSyncMode() {
        val pageType = "productDetailPage"
        val url = requireNotNull(urls[pageType])

        val request = CommandRequest(
            url,
            "",
            pageSummaryPrompt = "Summarize the product.",
            async = false
        )

        val status = client.post().uri("/api/commands")
            .body(request)
            .exchange()
            .expectStatus().is2xxSuccessful
            .expectBody<CommandStatus>()
            .returnResult()
            .responseBody
        assertNotNull(status)

        assertEquals(200, status.pageStatusCode)
        assertTrue(status.isDone)
        assertEquals(200, status.statusCode)

        assertNotNull(status.commandResult)
        assertNotNull(status.commandResult?.pageSummary)

        assertNull(status.commandResult?.fields)
        assertNull(status.commandResult?.links)
        assertNull(status.commandResult?.xsqlResultSet)
    }

    /**
     * Test [CommandController.submitCommand]
     * Test [CommandController.streamEvents]
     * */
    @Test
        @DisplayName("Test submitCommand with pageSummaryPrompt, dataExtractionRules + sync mode")
    fun testSubmitcommandWithPagesummarypromptDataextractionrulesSyncMode() {
        val pageType = "productDetailPage"
        val url = requireNotNull(urls[pageType])

        val request = CommandRequest(
            url,
            "",
            pageSummaryPrompt = "Summarize the product.",
            dataExtractionRules = "product name, ratings, price",
            async = false
        )

        val status = client.post().uri("/api/commands")
            .body(request)
            .exchange()
            .expectStatus().is2xxSuccessful
            .expectBody<CommandStatus>()
            .returnResult()
            .responseBody
        assertNotNull(status)

        assertEquals(200, status.pageStatusCode)
        assertTrue(status.isDone)
        assertEquals(200, status.statusCode)

        assertNotNull(status.commandResult)
        assertNotNull(status.commandResult?.pageSummary)
        assertNotNull(status.commandResult?.fields)

        assertNull(status.commandResult?.links)
        assertNull(status.commandResult?.xsqlResultSet)
    }

    @Test
        @DisplayName("test executeCommand with X-SQL + sync mode")
    fun testExecutecommandWithXSqlSyncMode() {
        val sqlTemplate = sqlTemplates["productDetailPage"]!!.template
        val request = CommandRequest(
            MOCK_PRODUCT_DETAIL_URL,
            xsql = sqlTemplate,
            async = false
        )

        val status = client.post().uri("/api/commands")
            .body(request)
            .exchange()
            .expectStatus().is2xxSuccessful
            .expectBody<CommandStatus>()
            .returnResult()
            .responseBody
        assertNotNull(status)

        printlnPro(prettyPulsarObjectMapper().writeValueAsString(status))
        val result = status.commandResult

        assertEquals(200, status.pageStatusCode)
        assertTrue(status.isDone)
        assertEquals(200, status.statusCode)

        assertNotNull(result)
        assertTrue { status.isDone }

        assertNull(result.pageSummary)
        assertNotNull(result.xsqlResultSet)
    }
}
