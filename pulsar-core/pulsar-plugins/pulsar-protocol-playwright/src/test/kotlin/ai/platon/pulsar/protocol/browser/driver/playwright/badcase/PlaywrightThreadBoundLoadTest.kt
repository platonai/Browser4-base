package ai.platon.pulsar.protocol.browser.driver.playwright.badcase

import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.common.printlnPro
import ai.platon.pulsar.common.sleepSeconds
import ai.platon.pulsar.protocol.browser.driver.playwright.PlaywrightTestBase.Companion.BAD_PARALLELISM_WARNING
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.options.LoadState
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Tag
import java.nio.file.Paths
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Warning: this is a bad case to demonstrate the problem of Playwright's parallelism.
 * */
@Disabled("MustRunExplicitly")
@Tag("MustRunExplicitly")
class PlaywrightThreadBoundLoadTest {
    companion object {
        const val CONTEXT_COUNT = 5           // 模拟 5 个用户
        const val TABS_PER_CONTEXT = 3        // 每个用户开 3 个标签页
    }

    private val originURL = "https://www.amazon.com/"
    private val testUrls = ConcurrentLinkedQueue(LinkExtractors.fromResource("seeds/seeds.txt"))
    private val workers = mutableListOf<PageWorker>()
    private val isShutdown = AtomicBoolean(false)

    /**
     * PageWorker represents a dedicated thread for a Playwright Page instance.
     * It ensures all operations on the Page are executed in this thread.
     * */
    private inner class PageWorker(val page: Page) : Thread() {
        private val tasks = ArrayBlockingQueue<String>(1)
        val isProcessing = AtomicBoolean(false)

        fun addTask(url: String) {
            tasks.put(url)
        }

        override fun run() {
            while (!isShutdown.get()) {
                val url = tasks.poll()
                if (url != null) {
                    fetch(url)
                }
                sleepSeconds(1)
            }
        }

        private fun fetch(url: String) {
            try {
                isProcessing.set(true)
                fetch0(url)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isProcessing.set(false)
            }
        }

        private fun fetch0(url: String) {
            printlnPro("$name \t Fetching one in ${tasks.size.inc()} | $url")

            val asins = mutableListOf<String>()
            try {
                require(!page.isClosed) { "Page is closed" }

                val navigateOptions = Page.NavigateOptions()
                    .setTimeout(30_000.0)
                    .setReferer(originURL)
                page.navigate(url, navigateOptions)
                page.waitForLoadState(LoadState.LOAD)
                page.waitForSelector("div[data-asin]", Page.WaitForSelectorOptions().setTimeout(30_000.0))
                printlnPro("$name \t ✅ Opened in tab ${page.hashCode()} | $url")
                sleepSeconds(1)

                val elements = page.querySelectorAll("div[data-asin]")
                elements.mapTo(asins) { it.getAttribute("data-asin") }
                printlnPro(asins.joinToString())

                page.navigate("about:blank")
            } catch (e: Exception) {
                if (asins.isEmpty()) {
                    printlnPro("$name \t ❌ Failed to open $url: ${e.message}")
                } else {
                    printlnPro("$name \t ASINs: ${asins.joinToString()}, error: ${e.message}")
                }
            }
        }
    }

    fun run() {
        Playwright.create().use { playwright ->
            // Step 1: 创建contexts和pages，并为每个page创建专属worker
            repeat(CONTEXT_COUNT) { contextIndex ->
                val userDataDir = Paths.get("target/user-data-dir-$contextIndex")
                val context = playwright.chromium().launchPersistentContext(
                    userDataDir,
                    BrowserType.LaunchPersistentContextOptions().apply {
                        headless = false
                    }
                )

                repeat(TABS_PER_CONTEXT) { tabIndex ->
                    val page = context.newPage()
                    val worker = PageWorker(page)
                    worker.name = "$tabIndex-$tabIndex"
                    workers.add(worker)
                    worker.start()
                }
            }

            // Step 2: 分发任务给workers
            try {
                while (testUrls.isNotEmpty()) {
                    val url = testUrls.poll() ?: break
                    var worker = workers.firstOrNull { !it.isProcessing.get() }
                    while (!isShutdown.get() && worker == null) {
                        worker = workers.firstOrNull { !it.isProcessing.get() }
                    }

                    worker?.addTask(url)
                }

                // 等待所有任务完成
                isShutdown.set(true)
                workers.forEach {
                    it.join()
                }
            } catch (e: Exception) {
                printlnPro("❌ Error occurred: ${e.message}")
            } finally {
                // 关闭所有contexts
                workers.forEach { worker ->
                    try {
                        worker.page.context().close()
                    } catch (e: Exception) {
                        printlnPro("Error closing context: ${e.message}")
                    }
                }
            }

            printlnPro("🎉 All requests completed.")
        }
    }
}

fun main() {
    printlnPro(BAD_PARALLELISM_WARNING)

    PlaywrightThreadBoundLoadTest().run()
}

