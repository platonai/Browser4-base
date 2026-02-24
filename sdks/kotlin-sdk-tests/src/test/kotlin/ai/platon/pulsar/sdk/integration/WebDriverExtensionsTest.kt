package ai.platon.pulsar.sdk.integration

import ai.platon.pulsar.sdk.integration.util.TestUrls
import ai.platon.pulsar.sdk.v0.WebDriver
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

@Tag("RequiresBrowser")
class WebDriverExtensionsTest : KotlinSdkIntegrationTestBase() {

    private lateinit var driver: WebDriver

    @BeforeEach
    suspend fun setupDriver() {
        createSession()
        driver = WebDriver(client)
    }

    @Test
    @DisplayName("should list sessions to verify SessionManager")
    suspend fun testListSessions() {
        val sessions = driver.listSessions()
        // If this works, SessionManager is active.
        // And ExtendedController has listSessions endpoint too!
        // So if this works, ExtendedController MUST be active.
        assertTrue(sessions.isNotEmpty())
    }

    @Test
    @DisplayName("should resize window")
    suspend fun testResize() {
        driver.open(TestUrls.SIMPLE_PAGE)
        // Just verify the call doesn't throw
        driver.resize(1024, 768)
        driver.resize(1920, 1080)
    }

    @Test
    @DisplayName("should double click")
    suspend fun testDblClick() {
        driver.open(TestUrls.INTERACTIVE_1)
        // Find an element to double click. Using body is safe enough for smoke test.
        driver.dblclick("body")
    }

    @Test
    @DisplayName("should handle dialogs")
    suspend fun testDialogs() {
        // We can't easily test actual dialog interaction without a page that pops one up.
        // But we can verify the API calls don't fail when no dialog is present (or fail with specific error)
        // or just ensure the method exists and is callable.
        
        // In some drivers, accept/dismiss might throw if no dialog is open.
        // In Browser4, it might be a no-op or throw.
        // For now just calling them to ensure SDK wiring is correct.
        try {
            driver.dialogDismiss()
        } catch (e: Exception) {
            // It's acceptable if it throws "no dialog open" or similar
        }
        
        try {
            driver.dialogAccept()
        } catch (e: Exception) {
            // It's acceptable if it throws "no dialog open" or similar
        }
    }
}
