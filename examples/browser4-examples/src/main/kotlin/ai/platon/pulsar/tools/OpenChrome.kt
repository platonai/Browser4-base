package ai.platon.pulsar.tools

import ai.platon.pulsar.protocol.browser.impl.DefaultBrowserFactory

suspend fun main() {
    val browser = DefaultBrowserFactory().launchDefaultBrowser()
    val driver = browser.newDriver()

    driver.navigateTo("about:blank")

    println("Chrome launched. Press Enter to close...")
    readlnOrNull()

    browser.close()
}
