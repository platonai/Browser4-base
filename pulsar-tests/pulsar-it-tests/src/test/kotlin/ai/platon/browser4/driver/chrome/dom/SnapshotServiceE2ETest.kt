package ai.platon.browser4.driver.chrome.dom

import ai.platon.browser4.driver.chrome.RemoteDevTools
import ai.platon.browser4.driver.chrome.dom.model.*
import ai.platon.pulsar.WebDriverTestBase
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.serialize.json.prettyPulsarObjectMapper
import ai.platon.pulsar.protocol.browser.driver.cdt.PulsarWebDriver
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.time.Instant
import kotlin.io.path.createDirectories
import kotlin.test.assertIs

@Tag("E2ETest")
class SnapshotServiceE2ETest : WebDriverTestBase() {
    private val testURL get() = "$generatedAssetsBaseURL/interactive-dynamic.html"
    private val startTime = Instant.now()
    private val ident = AppPaths.md5Hex(testURL)
    private val reportDir = AppPaths.detectAuxiliaryLogDir().resolve("tests")

    private data class Metrics(
        val url: String,
        val timestamp: String,
        val case: String,
        val cdpTiming: Map<String, Long> = emptyMap(),
        val devicePixelRatio: Double? = null,
        val domNodeCount: Int? = null,
        val axNodeCount: Int? = null,
        val snapshotEntryCount: Int? = null,
        val renderedAriaSnapshotSize: Int? = null,
        val notes: String? = null,
        val differenceType: String = "meta"
    )

    @Test
    @DisplayName("Given interactive page When collecting all trees Then get DOM AX and Snapshot with timings")
    fun testGetDomAxAndSnapshot() = runEnhancedWebDriverTest(testURL) { driver ->
        assertIs<PulsarWebDriver>(driver)
        val devTools = driver.implementation as RemoteDevTools
        val service = CDPSnapshotService(devTools)

        val options = SnapshotOptions(
            maxDepth = 1000,
            includeAX = true,
            includeSnapshot = true,
            includeStyles = true,
            includePaintOrder = true,
            includeDOMRects = true,
            includeScrollAnalysis = true,
            includeVisibility = true,
            includeInteractivity = true
        )

        val trees = service.buildTargetTrees(target = PageTarget(), options = options)
        assertTrue(trees.devicePixelRatio > 0.1, "devicePixelRatio should be positive")
        assertTrue(trees.cdpTiming.isNotEmpty(), "cdpTiming should record phases")

        val enhancedRoot = service.buildEnhancedDomTree(trees)
        val simplified = service.buildTinyTree(enhancedRoot)
        val domState = service.buildDOMState(simplified)

        assertTrue { enhancedRoot.children.isNotEmpty() }
        kotlin.test.assertTrue { simplified.children.isNotEmpty() }

        assertTrue(domState.ariaSnapshot.length > 50, "Serialized Aria Snapshot should not be trivial")
        assertTrue(domState.selectorMap.isNotEmpty(), "Selector map should contain entries")

        // Probe a stable element
        val bodyNode = service.findElement(ElementRefCriteria(cssSelector = "body"))
        assertNotNull(bodyNode, "Should locate <body> element")
        val interacted = service.toInteractedElement(bodyNode!!)
        assertTrue(interacted.elementHash.isNotBlank(), "Interacted element hash should be non-empty")

        val domCount = countDomNodes(enhancedRoot)
        val metrics = Metrics(
            url = testURL,
            timestamp = Instant.now().toString(),
            case = "ChromeDomServiceE2E",
            cdpTiming = trees.cdpTiming,
            devicePixelRatio = trees.devicePixelRatio,
            domNodeCount = domCount,
            axNodeCount = trees.axTree.size,
            snapshotEntryCount = trees.snapshotByBackendId.size,
            renderedAriaSnapshotSize = domState.ariaSnapshot.length,
            notes = "End-to-end validation with LLM serialization"
        )

        writeMetrics(metrics)
        writeSnapshot(enhancedRoot)
        writeDOMState(domState)
    }

    private fun writeMetrics(metrics: Metrics) {
        val path = reportDir.resolve("snapshot-metrics-$ident.json")
        path.parent.createDirectories()
        Files.writeString(path, prettyPulsarObjectMapper().writeValueAsString(metrics))

        logger.info("Metrics written | {}", path.toUri())
    }

    private fun writeSnapshot(snapshot: DOMTreeNodeEx) {
        val path = reportDir.resolve("snapshot-$ident.yaml")
        path.parent.createDirectories()
        Files.writeString(path, snapshot.toYaml())

        logger.info("Snapshot written | {}", path.toUri())
    }

    private fun writeDOMState(domState: DOMState) {
        var path = reportDir.resolve("dom-state-micro-$ident.yaml")
        path.parent.createDirectories()
        Files.writeString(path, domState.microTree.toYaml())
        logger.info("Micro tree written | {}", path.toUri())

        path = reportDir.resolve("dom-state-nano-$ident.yaml")
        path.parent.createDirectories()
        Files.writeString(path, domState.ariaSnapshot)
        logger.info("Nano tree written | {}", path.toUri())
    }

    private fun countDomNodes(root: DOMTreeNodeEx?): Int {
        if (root == null) return 0
        var n = 1
        root.children.forEach { n += countDomNodes(it) }
        root.shadowRoots.forEach { n += countDomNodes(it) }
        root.contentDocument?.let { n += countDomNodes(it) }
        return n
    }
}
