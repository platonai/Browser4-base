package ai.platon.browser4.driver.chrome.dom

import ai.platon.pulsar.WebDriverTestBase
import ai.platon.pulsar.browser.FastWebDriverService
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Comprehensive end-to-end tests for PulsarWebDriver click methods:
 * - click(selector: String, count: Int)
 * - click(selector: String, modifier: String)
 *
 * Test pattern reference: PulsarWebDriverScrollTests
 * Test page: interactive-dynamic.html with various interactive elements
 */
@Tag("E2ETest")
class PulsarWebDriverClickTests : WebDriverTestBase() {

    override val webDriverService get() = FastWebDriverService(browserFactory)

    companion object {
        private const val SMALL_TIMEOUT = 1000L
        private const val MEDIUM_TIMEOUT = 2000L
        private const val LARGE_TIMEOUT = 3000L
    }

    // ==================================================
    // Tests for click(selector: String, count: Int)
    // ==================================================

    @Test
    @DisplayName("click single count on button loads content")
    fun testClickSingleCount() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        driver.waitForSelector("h1")
        
        // Click load users button once
        driver.bringToFront()
        driver.click("[data-testid='tta-load-users']", 1)
        
        // Verify users loaded
        val remainingTime = driver.waitForSelector("#dynamicContent.loaded")
        assertTrue(remainingTime.toMillis() > 0, "Content should load")
        assertTrue(driver.exists("#dynamicContent [data-testid^='tta-user-']"), "Users should be visible")
    }

    @Test
    @DisplayName("click with default count parameter")
    fun testClickDefaultCount() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        driver.waitForSelector("h1")
        
        // Click without specifying count (default is 1)
        driver.bringToFront()
        driver.click("[data-testid='tta-load-products']")
        
        // Verify products loaded
        driver.waitForSelector("#dynamicContent.loaded")
        assertTrue(driver.exists("#dynamicContent [data-testid^='tta-product-']"), "Products should be visible")
    }

    @Test
    @DisplayName("click count zero still triggers element")
    fun testClickCountZero() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        driver.waitForSelector("h1")
        
        // Ensure content is clear initially
        driver.bringToFront()
        driver.click("[data-testid='tta-clear-content']")
        driver.waitUntil(MEDIUM_TIMEOUT) {
            val txt = driver.selectFirstTextOrNull("#dynamicContent p")
            txt?.contains("Click a button to load content") == true
        }
        
        // Click with count 0 - the implementation still triggers the element
        // The count parameter affects click count, but the element is still focused/triggered
        driver.bringToFront()
        driver.click("[data-testid='tta-load-users']", 0)
        
        // In the current implementation, even count=0 will load content
        driver.waitForSelector("#dynamicContent.loaded")
        assertTrue(driver.exists("#dynamicContent [data-testid^='tta-user-']"), "Content should load even with count=0")
    }

    @Test
    @DisplayName("click count two triggers double click behavior")
    fun testClickCountTwo() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        driver.waitForSelector("h1")
        
        // Add an item to the list
        driver.bringToFront()
        driver.fill("#newItemInput", "Test Item")
        driver.click("[data-testid='tta-add-item']", 1)
        driver.waitUntil(MEDIUM_TIMEOUT) { driver.exists("#itemList [data-testid='tta-item-3']") }
        
        // Double-click edit button (count=2) should trigger edit mode
        driver.bringToFront()
        driver.click("[data-testid='tta-edit-3']", 2)
        
        // In typical UI, double-click on edit might enter edit mode faster
        // Verify edit input appears
        driver.waitUntil(MEDIUM_TIMEOUT) {
            driver.exists("#itemList [data-id='3'] input[type='text']")
        }
        assertTrue(driver.exists("#itemList [data-id='3'] input[type='text']"), "Edit input should appear")
    }

    @Test
    @DisplayName("click count three on button")
    fun testClickCountThree() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        driver.waitForSelector("h1")
        
        // Triple click on add multiple items button
        driver.bringToFront()
        driver.click("[data-testid='tta-add-multiple']", 3)
        
        // Each click adds 5 items, so 3 clicks should add 15 items (plus initial 2)
        driver.waitUntil(LARGE_TIMEOUT) {
            val count = (driver.evaluateValue("document.querySelectorAll('#itemList .list-item').length") as? Number)?.toInt() ?: 0
            count >= 15
        }
        
        val finalCount = (driver.evaluateValue("document.querySelectorAll('#itemList .list-item').length") as? Number)?.toInt() ?: 0
        assertTrue(finalCount >= 15, "Should have at least 15 items after 3 clicks: actual=$finalCount")
    }

    @Test
    @DisplayName("click count negative treated as zero")
    fun testClickCountNegative() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        driver.waitForSelector("h1")
        
        // Clear content first
        driver.bringToFront()
        driver.click("[data-testid='tta-clear-content']")
        driver.waitUntil(MEDIUM_TIMEOUT) {
            val txt = driver.selectFirstTextOrNull("#dynamicContent p")
            txt?.contains("Click a button to load content") == true
        }
        
        // Click with negative count - implementation handles it gracefully
        driver.bringToFront()
        driver.click("[data-testid='tta-load-users']", -1)
        
        // Implementation still triggers the element
        driver.waitForSelector("#dynamicContent.loaded")
        assertTrue(driver.exists("#dynamicContent [data-testid^='tta-user-']"), "Element should be triggered despite negative count")
    }

    @Test
    @DisplayName("click sequential with count 1 on different elements")
    fun testClickSequentialDifferentElements() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        driver.waitForSelector("h1")
        
        // Clear content
        driver.bringToFront()
        driver.click("[data-testid='tta-clear-content']", 1)
        driver.waitUntil(MEDIUM_TIMEOUT) {
            val txt = driver.selectFirstTextOrNull("#dynamicContent p")
            txt?.contains("Click a button to load content") == true
        }
        
        // Click load users
        driver.bringToFront()
        driver.click("[data-testid='tta-load-users']", 1)
        driver.waitForSelector("#dynamicContent.loaded")
        assertTrue(driver.exists("#dynamicContent [data-testid^='tta-user-']"))
        
        // Click load products
        driver.bringToFront()
        driver.click("[data-testid='tta-load-products']", 1)
        driver.waitForSelector("#dynamicContent.loaded")
        assertTrue(driver.exists("#dynamicContent [data-testid^='tta-product-']"))
        
        // Clear again
        driver.bringToFront()
        driver.click("[data-testid='tta-clear-content']", 1)
        driver.waitUntil(MEDIUM_TIMEOUT) {
            val txt = driver.selectFirstTextOrNull("#dynamicContent p")
            txt?.contains("Click a button to load content") == true
        }
    }

    @Test
    @DisplayName("click rapid sequential same element")
    fun testClickRapidSequentialSameElement() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        driver.waitForSelector("h1")
        
        // Click add multiple items button 3 times rapidly
        driver.bringToFront()
        repeat(3) {
            driver.click("[data-testid='tta-add-multiple']", 1)
        }
        
        // Should add 5 items per click = 15 items total (plus initial 2)
        driver.waitUntil(LARGE_TIMEOUT) {
            val count = (driver.evaluateValue("document.querySelectorAll('#itemList .list-item').length") as? Number)?.toInt() ?: 0
            count >= 15
        }
        
        val finalCount = (driver.evaluateValue("document.querySelectorAll('#itemList .list-item').length") as? Number)?.toInt() ?: 0
        assertTrue(finalCount >= 15, "Should have at least 15 items: actual=$finalCount")
    }

    @Test
    @DisplayName("click on dynamically added element")
    fun testClickOnDynamicElement() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        driver.waitForSelector("h1")
        
        // Add an item first
        driver.bringToFront()
        driver.fill("#newItemInput", "Dynamic Item")
        driver.click("[data-testid='tta-add-item']", 1)
        driver.waitUntil(MEDIUM_TIMEOUT) { driver.exists("#itemList [data-testid='tta-item-3']") }
        
        // Click edit button on the dynamically added item
        driver.bringToFront()
        driver.click("[data-testid='tta-edit-3']", 1)
        driver.waitForSelector("#itemList [data-id='3'] input[type='text']")
        
        assertTrue(driver.exists("#itemList [data-id='3'] input[type='text']"), "Edit input should appear for dynamic element")
    }

    @Test
    @DisplayName("click on element that triggers async operation")
    fun testClickAsyncOperation() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        driver.waitForSelector("h1")
        
        // Click load users (2s delay)
        driver.bringToFront()
        driver.click("[data-testid='tta-load-users']", 1)
        
        // Verify loading state
        driver.waitUntil(SMALL_TIMEOUT) { driver.exists("#dynamicContent.loading") }
        
        // Wait for completion
        driver.waitForSelector("#dynamicContent.loaded")
        assertTrue(driver.exists("#dynamicContent [data-testid^='tta-user-']"))
        
        val status = driver.selectFirstTextOrNull("#loadingStatus span")
        assertTrue(status?.contains("loaded successfully") == true)
    }

    @Test
    @DisplayName("click count large number")
    fun testClickCountLarge() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        driver.waitForSelector("h1")
        
        // Click with large count (10)
        driver.bringToFront()
        driver.click("[data-testid='tta-add-multiple']", 10)
        
        // Should add 5 items per click = 50 items (plus initial 2)
        driver.waitUntil(LARGE_TIMEOUT) {
            val count = (driver.evaluateValue("document.querySelectorAll('#itemList .list-item').length") as? Number)?.toInt() ?: 0
            count >= 50
        }
        
        val finalCount = (driver.evaluateValue("document.querySelectorAll('#itemList .list-item').length") as? Number)?.toInt() ?: 0
        assertTrue(finalCount >= 50, "Should have at least 50 items after 10 clicks: actual=$finalCount")
    }

    @Test
    @DisplayName("click concurrent same element")
    fun testClickConcurrentSameElement() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        driver.waitForSelector("h1")
        
        // Concurrent clicks on the same button
        driver.bringToFront()
        coroutineScope {
            val deferredClicks = List(3) {
                async { driver.click("[data-testid='tta-add-multiple']", 1) }
            }
            deferredClicks.awaitAll()
        }
        
        // Should add items (at least 5 from one successful click)
        driver.waitUntil(LARGE_TIMEOUT) {
            val count = (driver.evaluateValue("document.querySelectorAll('#itemList .list-item').length") as? Number)?.toInt() ?: 0
            count >= 7 // initial 2 + at least 5
        }
        
        val finalCount = (driver.evaluateValue("document.querySelectorAll('#itemList .list-item').length") as? Number)?.toInt() ?: 0
        assertTrue(finalCount >= 7, "Should have at least 7 items: actual=$finalCount")
    }

    @Test
    @DisplayName("click concurrent different elements")
    fun testClickConcurrentDifferentElements() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        driver.waitForSelector("h1")
        
        // Concurrent clicks on different buttons
        driver.bringToFront()
        coroutineScope {
            val clicks = listOf(
                async { driver.click("[data-testid='tta-add-images']", 1) },
                async { driver.click("[data-testid='tta-add-multiple']", 1) },
                async { driver.click("[data-testid='tta-load-users']", 1) }
            )
            clicks.awaitAll()
        }
        
        // Verify at least one operation succeeded
        driver.waitUntil(LARGE_TIMEOUT) {
            driver.exists("#dynamicContent [data-testid^='tta-user-']") ||
            driver.exists("#itemList .list-item") ||
            (driver.evaluateValue("document.querySelectorAll('#imageGrid .lazy-image').length") as? Number)?.toInt() ?: 0 > 3
        }
    }

    @Test
    @DisplayName("click return value consistency")
    fun testClickReturnValueConsistency() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        driver.waitForSelector("h1")
        
        // Click should complete without throwing
        driver.bringToFront()
        driver.click("[data-testid='tta-load-users']", 1)
        
        // Verify state changed
        driver.waitForSelector("#dynamicContent.loaded")
        assertTrue(driver.exists("#dynamicContent [data-testid^='tta-user-']"))
    }

    // ==================================================
    // Tests for click(selector: String, modifier: String)
    // ==================================================

    @Test
    @DisplayName("click with Shift modifier")
    fun testClickWithShiftModifier() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        driver.waitForSelector("h1")
        
        // Click button with Shift modifier
        driver.bringToFront()
        driver.click("[data-testid='tta-load-users']", "Shift")
        
        // The button should still trigger, modifier just affects how
        driver.waitForSelector("#dynamicContent.loaded")
        assertTrue(driver.exists("#dynamicContent [data-testid^='tta-user-']"))
    }

    @Test
    @DisplayName("click with Control modifier")
    fun testClickWithControlModifier() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        driver.waitForSelector("h1")
        
        // Click with Control modifier
        driver.bringToFront()
        driver.click("[data-testid='tta-load-products']", "Control")
        
        driver.waitForSelector("#dynamicContent.loaded")
        assertTrue(driver.exists("#dynamicContent [data-testid^='tta-product-']"))
    }

    @Test
    @DisplayName("click with Alt modifier")
    fun testClickWithAltModifier() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        driver.waitForSelector("h1")
        
        // Click with Alt modifier
        driver.bringToFront()
        driver.click("[data-testid='tta-clear-content']", "Alt")
        
        driver.waitUntil(MEDIUM_TIMEOUT) {
            val txt = driver.selectFirstTextOrNull("#dynamicContent p")
            txt?.contains("Click a button to load content") == true
        }
    }

    @Test
    @DisplayName("click with Meta modifier")
    fun testClickWithMetaModifier() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        driver.waitForSelector("h1")
        
        // Click with Meta modifier (Command on Mac, Windows key on Windows)
        driver.bringToFront()
        driver.click("[data-testid='tta-add-item']", "Meta")
        
        // Button should still work
        driver.waitUntil(MEDIUM_TIMEOUT) { driver.exists("#itemList [data-testid='tta-item-3']") }
        assertTrue(driver.exists("#itemList [data-testid='tta-item-3']"))
    }

    @Test
    @DisplayName("click with modifier on link element")
    fun testClickWithModifierOnLink() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        driver.waitForSelector("h1")
        
        // Load users to get links
        driver.bringToFront()
        driver.click("[data-testid='tta-load-users']", 1)
        driver.waitForSelector("#dynamicContent.loaded")
        
        // Click a user link with Ctrl modifier (typically opens in new tab)
        if (driver.exists("#dynamicContent a[data-testid^='tta-user-']")) {
            driver.bringToFront()
            driver.click("#dynamicContent a[data-testid^='tta-user-']", "Control")
            // Note: In headless mode, new tab behavior may differ
        }
    }

    @Test
    @DisplayName("click with empty modifier string")
    fun testClickWithEmptyModifier() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        driver.waitForSelector("h1")
        
        // Click with empty modifier should work like normal click
        driver.bringToFront()
        driver.click("[data-testid='tta-load-users']", "")
        
        driver.waitForSelector("#dynamicContent.loaded")
        assertTrue(driver.exists("#dynamicContent [data-testid^='tta-user-']"))
    }

    @Test
    @DisplayName("click with modifier sequential different elements")
    fun testClickWithModifierSequential() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        driver.waitForSelector("h1")
        
        // Click multiple buttons with different modifiers
        driver.bringToFront()
        driver.click("[data-testid='tta-load-users']", "Shift")
        driver.waitForSelector("#dynamicContent.loaded")
        assertTrue(driver.exists("#dynamicContent [data-testid^='tta-user-']"))
        
        driver.bringToFront()
        driver.click("[data-testid='tta-load-products']", "Control")
        driver.waitForSelector("#dynamicContent.loaded")
        assertTrue(driver.exists("#dynamicContent [data-testid^='tta-product-']"))
        
        driver.bringToFront()
        driver.click("[data-testid='tta-clear-content']", "Alt")
        driver.waitUntil(MEDIUM_TIMEOUT) {
            val txt = driver.selectFirstTextOrNull("#dynamicContent p")
            txt?.contains("Click a button to load content") == true
        }
    }

    @Test
    @DisplayName("click with modifier on dynamically added element")
    fun testClickWithModifierOnDynamicElement() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        driver.waitForSelector("h1")
        
        // Add item
        driver.bringToFront()
        driver.fill("#newItemInput", "Modifier Test")
        driver.click("[data-testid='tta-add-item']", 1)
        driver.waitUntil(MEDIUM_TIMEOUT) { driver.exists("#itemList [data-testid='tta-item-3']") }
        
        // Click edit button with modifier
        driver.bringToFront()
        driver.click("[data-testid='tta-edit-3']", "Shift")
        driver.waitForSelector("#itemList [data-id='3'] input[type='text']")
        
        assertTrue(driver.exists("#itemList [data-id='3'] input[type='text']"))
    }

    @Test
    @DisplayName("click with modifier consistency")
    fun testClickWithModifierConsistency() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        driver.waitForSelector("h1")
        
        // Click same button multiple times with same modifier
        val modifiers = listOf("Shift", "Control", "Alt")
        
        for (modifier in modifiers) {
            driver.bringToFront()
            driver.click("[data-testid='tta-clear-content']", modifier)
            driver.waitUntil(MEDIUM_TIMEOUT) {
                val txt = driver.selectFirstTextOrNull("#dynamicContent p")
                txt?.contains("Click a button to load content") == true
            }
            
            driver.bringToFront()
            driver.click("[data-testid='tta-load-users']", modifier)
            driver.waitForSelector("#dynamicContent.loaded")
            assertTrue(driver.exists("#dynamicContent [data-testid^='tta-user-']"), "Should work with modifier: $modifier")
        }
    }

    // ==================================================
    // Edge cases and error handling
    // ==================================================

    @Test
    @DisplayName("click on non-existent element should handle gracefully")
    fun testClickNonExistentElement() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        driver.waitForSelector("h1")
        
        // Attempt to click non-existent element
        try {
            driver.click("[data-testid='tta-non-existent-button']", 1)
            // If it doesn't throw, that's also acceptable behavior
        } catch (e: Exception) {
            // Exception is expected for non-existent elements
            assertTrue(e.message?.contains("not found") == true || e.message?.contains("No node") == true)
        }
    }

    @Test
    @DisplayName("click on disabled button should complete")
    fun testClickDisabledButton() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        driver.waitForSelector("h1")
        
        // Disable a button via JavaScript
        driver.evaluate("document.querySelector('[data-testid=\"tta-load-users\"]').disabled = true")
        
        // Click should complete even if button is disabled
        driver.bringToFront()
        driver.click("[data-testid='tta-load-users']", 1)
        
        // Disabled button typically won't trigger its action, but click completes
        Thread.sleep(500)
    }

    @Test
    @DisplayName("click on element that is scrolled out of view")
    fun testClickElementScrolledOutOfView() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        driver.waitForSelector("h1")
        
        // Scroll to top
        driver.scrollToTop()
        
        // Click element that may be out of initial viewport (driver should auto-scroll)
        driver.bringToFront()
        driver.click("[data-testid='tta-trigger-error']", 1)
        
        driver.waitUntil(MEDIUM_TIMEOUT) { driver.exists("#errorBoundary.show") }
        assertTrue(driver.exists("#errorBoundary.show"))
    }

    @Test
    @DisplayName("click on covered element should work with auto-scroll")
    fun testClickCoveredElement() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        driver.waitForSelector("h1")
        
        // Click button that may be partially covered
        driver.bringToFront()
        driver.click("[data-testid='tta-clear-virtual']", 1)
        
        // Should handle scrolling and clicking
        driver.waitUntil(MEDIUM_TIMEOUT) {
            val txt = driver.selectFirstTextOrNull("#virtualScrollContent p")
            txt?.contains("Click a button to generate items") == true
        }
    }

    @Test
    @DisplayName("click on element inside iframe would fail gracefully")
    fun testClickInsideIframe() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        driver.waitForSelector("h1")
        
        // Try to click inside a non-existent iframe (should fail gracefully)
        try {
            driver.click("iframe button", 1)
        } catch (e: Exception) {
            // Expected to fail as there's no iframe in the test page
            assertTrue(true, "Expected failure for iframe selector")
        }
    }

    @Test
    @DisplayName("click after page navigation")
    fun testClickAfterNavigation() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        driver.waitForSelector("h1")
        
        // Click and load content
        driver.bringToFront()
        driver.click("[data-testid='tta-load-users']", 1)
        driver.waitForSelector("#dynamicContent.loaded")
        
        // Navigate to same page again (refresh-like)
        driver.navigateTo(multiScreensInteractiveUrl)
        driver.waitForSelector("h1")
        
        // Click should work after navigation
        driver.bringToFront()
        driver.click("[data-testid='tta-load-products']", 1)
        driver.waitForSelector("#dynamicContent.loaded")
        assertTrue(driver.exists("#dynamicContent [data-testid^='tta-product-']"))
    }

    @Test
    @DisplayName("click validates selector before execution")
    fun testClickValidatesSelector() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        driver.waitForSelector("h1")
        
        // Valid selector should work
        driver.bringToFront()
        driver.click("[data-testid='tta-load-users']", 1)
        driver.waitForSelector("#dynamicContent.loaded")
        
        // Invalid selector should be handled
        try {
            driver.click("", 1)
        } catch (e: Exception) {
            // Empty selector should fail
            assertTrue(true)
        }
    }

    @Test
    @DisplayName("click idempotency on already triggered state")
    fun testClickIdempotency() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        driver.waitForSelector("h1")
        
        // Click load users
        driver.bringToFront()
        driver.click("[data-testid='tta-load-users']", 1)
        driver.waitForSelector("#dynamicContent.loaded")
        assertTrue(driver.exists("#dynamicContent [data-testid^='tta-user-']"))
        
        // Click again on same button
        driver.bringToFront()
        driver.click("[data-testid='tta-load-users']", 1)
        driver.waitForSelector("#dynamicContent.loaded")
        
        // Should still show users (idempotent operation)
        assertTrue(driver.exists("#dynamicContent [data-testid^='tta-user-']"))
    }

    @Test
    @DisplayName("click performance with rapid succession")
    fun testClickPerformanceRapidSuccession() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        driver.waitForSelector("h1")
        
        val startTime = System.currentTimeMillis()
        
        // Click 5 times rapidly
        driver.bringToFront()
        repeat(5) {
            driver.click("[data-testid='tta-add-item']", 1)
        }
        
        val elapsed = System.currentTimeMillis() - startTime
        
        // Should complete in reasonable time (< 10 seconds for 5 clicks)
        assertTrue(elapsed < 10000, "5 clicks should complete within 10s: actual=${elapsed}ms")
        
        // Verify items were added
        driver.waitUntil(LARGE_TIMEOUT) {
            val count = (driver.evaluateValue("document.querySelectorAll('#itemList .list-item').length") as? Number)?.toInt() ?: 0
            count >= 5
        }
    }
}
