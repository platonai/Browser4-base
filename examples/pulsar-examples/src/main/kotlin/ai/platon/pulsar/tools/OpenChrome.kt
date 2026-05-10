package ai.platon.pulsar.tools

import ai.platon.pulsar.core.api.PulsarContexts

suspend fun main() {
    val browser = PulsarContexts.launchDefaultBrowser()
    val driver = browser.newDriver()

    driver.navigate("about:blank")

    println("Chrome launched. Press Enter to close...")
    readlnOrNull()

    browser.close()
}
