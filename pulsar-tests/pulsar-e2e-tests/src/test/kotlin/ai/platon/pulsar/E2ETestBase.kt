package ai.platon.pulsar

import ai.platon.pulsar.agentic.AgenticSession
import ai.platon.pulsar.boot.autoconfigure.PulsarContextConfiguration
import ai.platon.pulsar.common.config.ImmutableConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.http.ResponseEntity
import org.springframework.test.web.servlet.client.RestTestClient
import org.springframework.test.web.servlet.client.expectBody

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PulsarContextConfiguration::class)
class E2ETestBase {

    @LocalServerPort
    val serverPort: Int = 0

    @Autowired
    lateinit var session: AgenticSession

    @Autowired
    lateinit var configuration: ImmutableConfig

    val hostname = "127.0.0.1"

    val baseUri get() = String.format("http://%s:%d", hostname, serverPort)

    // Build a RestTestClient bound to the running server on demand
    protected val client get() = RestTestClient.bindToServer().baseUrl(baseUri).build()

    protected fun getHtml(path: String): ResponseEntity<String> =
        client.get().uri(path)
            .exchange()
            .expectStatus().is2xxSuccessful
            .expectBody<String>()
            .returnResult()
            .let { result -> ResponseEntity(result.responseBody!!, result.responseHeaders, result.status) }
}
