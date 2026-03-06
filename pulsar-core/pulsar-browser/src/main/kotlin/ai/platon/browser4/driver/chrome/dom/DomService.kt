package ai.platon.browser4.driver.chrome.dom

import ai.platon.browser4.driver.chrome.dom.model.*

/**
 * Kotlin-native DOM service interface.
 */
interface DomService {

    suspend fun getBrowserUseState(target: PageTarget = PageTarget(), snapshotOptions: SnapshotOptions = SnapshotOptions()): BrowserUseState

    suspend fun getDOMState(target: PageTarget = PageTarget(), snapshotOptions: SnapshotOptions = SnapshotOptions()): DOMState

    /**
     * Find an element by various criteria (CSS selector, XPath, element hash).
     */
    fun findElement(ref: ElementRefCriteria): DOMTreeNodeEx?

    suspend fun buildBrowserState(domState: DOMState): BrowserUseState

    suspend fun addHighlights(elements: InteractiveDOMTreeNodeList)

    suspend fun removeHighlights(elements: InteractiveDOMTreeNodeList)

    suspend fun removeHighlights(force: Boolean = false)
}
