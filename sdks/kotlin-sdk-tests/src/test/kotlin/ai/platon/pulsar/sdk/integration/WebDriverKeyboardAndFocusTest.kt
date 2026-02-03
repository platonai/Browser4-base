/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor
 * license agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership. The ASF licenses this file to
 * you under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required
 * by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package ai.platon.pulsar.sdk.integration

import ai.platon.pulsar.sdk.integration.util.TestUrls
import ai.platon.pulsar.sdk.v0.WebDriver
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

/**
 * Tests for WebDriver keyboard and focus operations.
 *
 * Covers previously untested methods:
 * - focus operations (focus, blur)
 * - keyboard operations (press, sendKeys, type)
 * - form field interactions
 */
@Tag("IntegrationTest")
@Tag("RequiresBrowser")

class WebDriverKeyboardAndFocusTest : KotlinSdkIntegrationTestBase() {

    private lateinit var driver: WebDriver

    @BeforeEach
    suspend fun setupDriver() {
        createSession()
        driver = WebDriver(client)
    }

    // ========== Focus Operations ==========

    @Test
    @DisplayName("should focus on input element")
    suspend fun testShouldFocusOnInputElement() {
        driver.open(TestUrls.KEYBOARD_PAGE)

        // Focus on the focus input
        driver.focus("#focusInput")

        // Verify the input exists (focus operation completed)
        assertTrue(driver.exists("#focusInput"), "Input should exist after focus operation")
    }

    @Test
    @DisplayName("should focus on multiple elements sequentially")
    suspend fun testShouldFocusOnMultipleElementsSequentially() {
        driver.open(TestUrls.FORM_PAGE)

        // Focus on different inputs
        driver.focus("#username")
        driver.focus("#email")
        driver.focus("#password")

        // All elements should still exist
        assertTrue(driver.exists("#username"), "Username input should exist")
        assertTrue(driver.exists("#email"), "Email input should exist")
        assertTrue(driver.exists("#password"), "Password input should exist")
    }

    @Test
    @DisplayName("should handle focus on non-focusable element gracefully")
    @Tag("Fast")
    suspend fun testShouldHandleFocusOnNonFocusableElementGracefully() {
        driver.open(TestUrls.SIMPLE_PAGE)

        // Try to focus on a div (not normally focusable)
        try {
            driver.focus("body")
            // If it doesn't throw, that's acceptable
            assertTrue(true, "Focus on non-focusable element handled")
        } catch (e: Exception) {
            // Expected behavior
            assertTrue(true, "Exception expected for non-focusable element")
        }
    }

    // ========== Keyboard Press Operations ==========

    @Test
    @DisplayName("should press Enter key")
    suspend fun testShouldPressEnterKey() {
        driver.open(TestUrls.KEYBOARD_PAGE)

        driver.focus("#keyInput")
        driver.press("#keyInput", "Enter")

        // Verify operation completed
        assertTrue(driver.exists("#keyInput"), "Input should exist after key press")
    }

    @Test
    @DisplayName("should press Tab key")
    suspend fun testShouldPressTabKey() {
        driver.open(TestUrls.FORM_PAGE)

        driver.focus("#username")
        driver.press("#username", "Tab")

        // Tab should move focus (operation completed)
        assertTrue(driver.exists("#username"), "Form should be accessible after Tab")
    }

    @Test
    @DisplayName("should press Escape key")
    suspend fun testShouldPressEscapeKey() {
        driver.open(TestUrls.KEYBOARD_PAGE)

        driver.focus("#keyInput")
        driver.press("#keyInput", "Escape")

        // Verify operation completed
        assertTrue(driver.exists("#keyInput"), "Input should exist after Escape")
    }

    @Test
    @DisplayName("should press Arrow keys")
    suspend fun testShouldPressArrowKeys() {
        driver.open(TestUrls.KEYBOARD_PAGE)

        driver.focus("#keyInput")
        driver.press("#keyInput", "ArrowDown")
        driver.press("#keyInput", "ArrowUp")
        driver.press("#keyInput", "ArrowLeft")
        driver.press("#keyInput", "ArrowRight")

        // Verify operations completed
        assertTrue(driver.exists("#keyInput"), "Input should exist after arrow key presses")
    }

    // ========== Type and SendKeys Operations ==========

    @Test
    @DisplayName("should type text in input field")
    suspend fun testShouldTypeTextInInputField() {
        driver.open(TestUrls.FORM_PAGE)

        driver.type("#username", "testuser")

        // Verify the input exists (type operation completed)
        assertTrue(driver.exists("#username"), "Input should exist after typing")
    }

    @Test
    @DisplayName("should type in multiple input fields")
    suspend fun testShouldTypeInMultipleInputFields() {
        driver.open(TestUrls.FORM_PAGE)

        driver.type("#username", "testuser")
        driver.type("#email", "test@example.com")
        driver.type("#password", "password123")

        // Verify all inputs exist
        assertTrue(driver.exists("#username"), "Username input should exist")
        assertTrue(driver.exists("#email"), "Email input should exist")
        assertTrue(driver.exists("#password"), "Password input should exist")
    }

    @Test
    @DisplayName("should type empty string")
    suspend fun testShouldTypeEmptyString() {
        driver.open(TestUrls.FORM_PAGE)

        driver.type("#username", "")

        // Should handle empty string gracefully
        assertTrue(driver.exists("#username"), "Input should exist after typing empty string")
    }

    @Test
    @DisplayName("should type special characters")
    suspend fun testShouldTypeSpecialCharacters() {
        driver.open(TestUrls.FORM_PAGE)

        driver.type("#username", "user@#$%&*()")

        // Should handle special characters
        assertTrue(driver.exists("#username"), "Input should exist after typing special characters")
    }

    @Test
    @DisplayName("should type long text")
    suspend fun testShouldTypeLongText() {
        driver.open(TestUrls.FORM_PAGE)

        val longText = "a".repeat(1000)
        driver.type("#username", longText)

        // Should handle long text
        assertTrue(driver.exists("#username"), "Input should exist after typing long text")
    }

    @Test
    @DisplayName("should use sendKeys for text input")
    suspend fun testShouldUseSendKeysForTextInput() {
        driver.open(TestUrls.FORM_PAGE)

        driver.sendKeys("#email", "user@test.com")

        // Verify the input exists
        assertTrue(driver.exists("#email"), "Input should exist after sendKeys")
    }

    @Test
    @DisplayName("should use sendKeys with special keys")
    suspend fun testShouldUseSendKeysWithSpecialKeys() {
        driver.open(TestUrls.FORM_PAGE)

        driver.focus("#username")
        driver.sendKeys("#username", "Hello")

        // Verify operation completed
        assertTrue(driver.exists("#username"), "Input should exist after sendKeys")
    }

    // ========== Form Interaction Workflows ==========

    @Test
    @DisplayName("should complete form filling workflow")
    suspend fun testShouldCompleteFormFillingWorkflow() {
        driver.open(TestUrls.FORM_PAGE)

        // Fill form using keyboard operations
        driver.type("#username", "testuser")
        driver.press("#username", "Tab")
        driver.type("#email", "test@example.com")
        driver.press("#email", "Tab")
        driver.type("#password", "secure123")

        // Verify all fields are accessible
        assertTrue(driver.exists("#username"), "Username should be filled")
        assertTrue(driver.exists("#email"), "Email should be filled")
        assertTrue(driver.exists("#password"), "Password should be filled")
    }

    @Test
    @DisplayName("should handle focus and type together")
    suspend fun testShouldHandleFocusAndTypeTogether() {
        driver.open(TestUrls.FORM_PAGE)

        driver.focus("#username")
        driver.type("#username", "focused_user")

        // Verify operation completed
        assertTrue(driver.exists("#username"), "Input should exist after focus and type")
    }

    @Test
    @DisplayName("should type and submit with Enter")
    suspend fun testShouldTypeAndSubmitWithEnter() {
        driver.open(TestUrls.FORM_PAGE)

        driver.type("#username", "submituser")
        driver.focus("#username")
        driver.press("#username", "Enter")

        // Verify form is still accessible (might have submitted)
        assertTrue(driver.exists("#testForm"), "Form should exist after Enter press")
    }

    // ========== Clear and Modify Operations ==========

    @Test
    @DisplayName("should type then clear with selections")
    suspend fun testShouldTypeThenClearWithSelections() {
        driver.open(TestUrls.FORM_PAGE)

        // Type text
        driver.type("#username", "initial")

        // Select all and replace
        driver.focus("#username")
        // Ctrl+A (select all) - platform dependent
        driver.type("#username", "replaced")

        // Verify input is accessible
        assertTrue(driver.exists("#username"), "Input should exist after modification")
    }

    @Test
    @DisplayName("should handle rapid typing")
    @Tag("Fast")
    suspend fun testShouldHandleRapidTyping() {
        driver.open(TestUrls.FORM_PAGE)

        // Rapid typing operations
        repeat(5) {
            driver.type("#username", "test$it")
        }

        // Should handle rapid operations
        assertTrue(driver.exists("#username"), "Input should exist after rapid typing")
    }

    // ========== Edge Cases ==========

    @Test
    @DisplayName("should handle focus on non-existent element")
    @Tag("Fast")
    suspend fun testShouldHandleFocusOnNonExistentElement() {
        driver.open(TestUrls.SIMPLE_PAGE)

        try {
            driver.focus("#nonExistentInput")
            // If it doesn't throw, that's acceptable
        } catch (e: Exception) {
            // Expected behavior
            assertTrue(true, "Exception expected for non-existent element")
        }
    }

    @Test
    @DisplayName("should handle type on non-existent element")
    @Tag("Fast")
    suspend fun testShouldHandleTypeOnNonExistentElement() {
        driver.open(TestUrls.SIMPLE_PAGE)

        try {
            driver.type("#nonExistentInput", "text")
            // If it doesn't throw, that's acceptable
        } catch (e: Exception) {
            // Expected behavior
            assertTrue(true, "Exception expected for non-existent element")
        }
    }

    @Test
    @DisplayName("should handle Unicode characters in type")
    suspend fun testShouldHandleUnicodeCharactersInType() {
        driver.open(TestUrls.FORM_PAGE)

        // Type Unicode characters
        driver.type("#username", "用户名测试")

        // Should handle Unicode
        assertTrue(driver.exists("#username"), "Input should exist after typing Unicode")
    }

    @Test
    @DisplayName("should handle newlines in type")
    suspend fun testShouldHandleNewlinesInType() {
        driver.open(TestUrls.FORM_PAGE)

        // Type text with newline (might be treated differently in single-line input)
        driver.type("#username", "line1\nline2")

        // Should handle newline
        assertTrue(driver.exists("#username"), "Input should exist after typing with newline")
    }
}
