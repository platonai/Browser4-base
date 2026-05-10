package ai.platon.pulsar.protocol.browser

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.skeleton.workflow.protocol.ProtocolFactory
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit test for new protocol plugin.
 */
class TestProtocolFactory {
    private val protocolFactory = ProtocolFactory(ImmutableConfig())

    @Test
    @Throws(Exception::class)
    fun testGetProtocol() {
        assertEquals(
            BrowserEmulatorProtocol::class.java.name,
            protocolFactory.getProtocol("browser:http://example.com")?.javaClass?.name
        )
    }
}
