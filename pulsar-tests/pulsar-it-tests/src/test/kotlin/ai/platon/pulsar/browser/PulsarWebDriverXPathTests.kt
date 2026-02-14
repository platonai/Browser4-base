package ai.platon.pulsar.browser

import ai.platon.pulsar.WebDriverTestBase
import org.junit.jupiter.api.Disabled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.DisplayName

class PulsarWebDriverXPathTests : WebDriverTestBase() {

    override val webDriverService get() = FastWebDriverService(browserFactory)

    @Test
        @DisplayName("test selectFirstTextOrNull by xpath with id")
    fun testSelectfirsttextornullByXpathWithId() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        driver.waitForSelector("#preferencesSection")
        val text = driver.selectFirstTextOrNull("//*[@id='preferencesSection']/h2")
        assertEquals("""📊 Preferences""", text)
    }

    @Test
        @DisplayName("test selectFirstTextOrNull by xpath with data-testid")
    fun testSelectfirsttextornullByXpathWithDataTestid() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        val text = driver.selectFirstTextOrNull("//section[@data-testid='calculatorSection']/h2")
        assertEquals("🧮 Quick Calculator", text)
    }

    @Test
        @DisplayName("test selectFirstTextOrNull by xpath with contains")
    fun testSelectfirsttextornullByXpathWithContains() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        val text = driver.selectFirstTextOrNull("//section[contains(@class, 'section-toggle')]/h2")
        assertEquals("🎯 Dynamic Toggle", text)
    }

    @Test
        @DisplayName("test selectFirstTextOrNull by xpath with text content")
    fun testSelectfirsttextornullByXpathWithTextContent() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        val text = driver.selectFirstTextOrNull("//h2[contains(text(), 'Email Validation')]")
        assertEquals("✉️ Email Validation", text)
    }

    @Test
        @DisplayName("test selectFirstAttributeOrNull by xpath")
    fun testSelectfirstattributeornullByXpath() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        val placeholder = driver.selectFirstAttributeOrNull("//input[@id='name']", "placeholder")
        assertEquals("Type here...", placeholder)
    }

    @Test
        @DisplayName("test selectFirstAttributeOrNull by xpath with data attribute")
    fun testSelectfirstattributeornullByXpathWithDataAttribute() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        val dataComponent = driver.selectFirstAttributeOrNull("//input[@data-testid='nameInput']", "data-component")
        assertEquals("name-input", dataComponent)
    }

    @Test
        @DisplayName("test selectFirstAttributeOrNull by xpath for button")
    fun testSelectfirstattributeornullByXpathForButton() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        val ariaLabel = driver.selectFirstAttributeOrNull("//button[@id='addButton']", "aria-label")
        assertEquals("Add the two numbers", ariaLabel)
    }

    @Disabled("NOT IMPLEMENTED")
    @Test
        @DisplayName("test selectTextAll by xpath with multiple elements")
    fun testSelecttextallByXpathWithMultipleElements() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        val texts = driver.selectTextAll("//section/h2")
        assertTrue(texts.size >= 7)
        assertTrue(texts.contains("📋 User Information"))
        assertTrue(texts.contains("📊 Preferences"))
        assertTrue(texts.contains("🧮 Quick Calculator"))
    }

    @Test
        @DisplayName("test selectFirstTextOrNull by xpath with descendant")
    fun testSelectfirsttextornullByXpathWithDescendant() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        val text = driver.selectFirstTextOrNull("//section[@id='emailValidationSection']//h2")
        assertEquals("✉️ Email Validation", text)
    }

    @Disabled("MAY NOT SUPPORTED BY CDP")
    @Test
        @DisplayName("test selectFirstTextOrNull by xpath with parent")
    fun testSelectfirsttextornullByXpathWithParent() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        val text = driver.selectFirstTextOrNull("//input[@id='email2']/parent::section/h2")
        assertEquals("""🔒 Contact Us""", text)
    }

    @Disabled("MAY NOT SUPPORTED BY CDP")
    @Test
        @DisplayName("test selectFirstTextOrNull by xpath with following-sibling")
    fun testSelectfirsttextornullByXpathWithFollowingSibling() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        val text = driver.selectFirstTextOrNull("//h1/following-sibling::p")
        assertEquals(text?.contains("demonstrates various interactive elements"), true)
    }

    @Test
        @DisplayName("test selectFirstAttributeOrNull by xpath with multiple conditions")
    fun testSelectfirstattributeornullByXpathWithMultipleConditions() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        val dataRole = driver.selectFirstAttributeOrNull(
            "//button[@data-testid='toggleMessageButton' and @data-component='toggle-message-button']",
            "data-role"
        )
        assertEquals("button", dataRole)
    }

    @Disabled("NOT IMPLEMENTED")
    @Test
        @DisplayName("test selectAttributeAll by xpath for href attributes")
    fun testSelectattributeallByXpathForHrefAttributes() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        val hrefs = driver.selectAttributeAll("//a[contains(@href, 'example.com')]", "href")
        assertTrue(hrefs.isNotEmpty())
        assertTrue(hrefs.all { it.contains("example.com") })
    }

    @Disabled("ONLY XPATH START WITH // SUPPORTED")
    @Test
        @DisplayName("test selectFirstTextOrNull by xpath with position")
    fun testSelectfirsttextornullByXpathWithPosition() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        val text = driver.selectFirstTextOrNull("(//section/h2)[1]")
        assertEquals("\uD83C\uDFAF Dynamic Toggle", text)
    }

//    @Test
//        @DisplayName("test selectFirstAttributeOrNull by xpath with ancestor")
    fun testSelectfirstattributeornullByXpathWithAncestor() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
//        val sectionId = driver.selectFirstAttributeOrNull(
//            "//button[@id='toggleMessageButton']/ancestor::section",
//            "id"
//        )
//        assertEquals("toggleSection", sectionId)
//    }

    @Disabled("NOT IMPLEMENTED")
    @Test
        @DisplayName("test selectTextAll by xpath with specific class pattern")
    fun testSelecttextallByXpathWithSpecificClassPattern() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        val texts = driver.selectTextAll("//section[@class and contains(@class, 'section-')]/h2")
        assertTrue(texts.size >= 5)
    }

    @Test
        @DisplayName("test selectFirstAttributeOrNull by xpath for select element")
    fun testSelectfirstattributeornullByXpathForSelectElement() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        val ariaLabel = driver.selectFirstAttributeOrNull("//select[@id='colorSelect']", "aria-label")
        assertEquals("Favorite color selector", ariaLabel)
    }

    @Disabled("NOT IMPLEMENTED")
    @Test
        @DisplayName("test selectAttributeAll by xpath for all data-testid")
    fun testSelectattributeallByXpathForAllDataTestid() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        val testIds = driver.selectAttributeAll("//section[@data-testid]", "data-testid", start = 0, limit = 20)
        assertTrue(testIds.isNotEmpty())
        assertTrue(testIds.contains("userInformationSection"))
        assertTrue(testIds.contains("preferencesSection"))
    }

    @Test
        @DisplayName("test selectFirstTextOrNull by xpath with not condition")
    fun testSelectfirsttextornullByXpathWithNotCondition() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        val text = driver.selectFirstTextOrNull("//section[not(@class='hidden')]/h2[1]")
        assertNotNull(text)
        assertTrue(text.isNotEmpty())
    }
}
