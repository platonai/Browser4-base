package ai.platon.pulsar.common.js

import ai.platon.pulsar.common.js.JsUtils.toCDPCompatibleExpression
import ai.platon.pulsar.common.js.JsUtils.toIIFEOrNull
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import kotlin.test.assertNull
import org.junit.jupiter.api.DisplayName

class JsUtilsTest {

    @Test
        @DisplayName("test normal function expression")
    fun testNormalFunctionExpression() {
        val input = "function() { console.log('hello'); }"
        val expected = "(function() { console.log('hello'); })();"
        assertEquals(expected, toIIFEOrNull(input))
    }

    @Test
        @DisplayName("test function with leading and trailing spaces")
    fun testFunctionWithLeadingAndTrailingSpaces() {
        val input = "   function() {}   "
        val expected = "(function() {})();"
        assertEquals(expected, toIIFEOrNull(input))
    }

    @Test
        @DisplayName("test function with semicolons")
    fun testFunctionWithSemicolons() {
        val input = ";(function(){});"
        val expected = "((function(){}))();"
        assertEquals(expected, toIIFEOrNull(input))
    }

    @Test
        @DisplayName("test async function")
    fun testAsyncFunction() {
        val input = "async function() { await doSomething(); }"
        val expected = "(async function() { await doSomething(); })();"
        assertEquals(expected, toIIFEOrNull(input))
    }

    @Test
        @DisplayName("test arrow function")
    fun testArrowFunction() {
        val input = "() => { return 42; }"
        val expected = "(() => { return 42; })();"
        assertEquals(expected, toIIFEOrNull(input))
    }

    @Test
        @DisplayName("test arrow function with arguments")
    fun testArrowFunctionWithArguments() {
        val input = "x => x * 2"
        val args = "5"
        val expected = "(x => x * 2)(5);"
        assertEquals(expected, toIIFEOrNull(input, args))
    }

    @Test
        @DisplayName("test object literal should not be treated as function")
    fun testObjectLiteralShouldNotBeTreatedAsFunction() {
        val input = "{ key: 'value' }"
        val expected = "({ key: 'value' });"
        assertEquals(expected, toIIFEOrNull(input))
    }

    @Test
        @DisplayName("test invalid format returns error message")
    fun testInvalidFormatReturnsErrorMessage() {
        val input = "This is not a function"
        // ❌ Unsupported format: not a valid JS function
        assertNull(toIIFEOrNull(input))
    }

    @Test
        @DisplayName("test empty string returns error message")
    fun testEmptyStringReturnsErrorMessage() {
        val input = ""
        // ❌ Unsupported format: not a valid JS function
        assertNull(toIIFEOrNull(input))
    }

    @Test
        @DisplayName("test function with multi-line expressions")
    fun testFunctionWithMultiLineExpressions() {
        val input = """
            const a = 10;
            const b = 20;
            return a * b;
        """.trimIndent()

        // ❌ Unsupported format: not a valid JS function
        assertNull(toIIFEOrNull(input))
    }

    @Test
        @DisplayName("test function starting with x")
    fun testFunctionStartingWithX() {
        val input = "x => x + 1"
        val expected = "(x => x + 1)();"
        assertEquals(expected, toIIFEOrNull(input))
    }

    @Test
        @DisplayName("test function with custom arguments")
    fun testFunctionWithCustomArguments() {
        val input = "function(a, b) { return a + b; }"
        val args = "1, 2"
        val expected = "(function(a, b) { return a + b; })(1, 2);"
        assertEquals(expected, toIIFEOrNull(input, args))
    }

    @Test
        @DisplayName("test toCDPCompatibleExpression remove heading return")
    fun testTocdpcompatibleexpressionRemoveHeadingReturn() {
        var expected = "document.title"
        assertEquals(expected, toCDPCompatibleExpression("return   document.title  "))
        assertEquals(expected, toCDPCompatibleExpression("return   \ndocument.title  "))
        assertEquals(expected, toCDPCompatibleExpression("   return   document.title  "))
        assertEquals(expected, toCDPCompatibleExpression("\nreturn   document.title  "))
        assertEquals(expected, toCDPCompatibleExpression("\n\nreturn\ndocument.title  "))

        expected = "document.title; return 1;"
        assertEquals(expected, toCDPCompatibleExpression("document.title; return 1;  "))
        assertNotEquals(expected, toCDPCompatibleExpression("document.title;\n\nreturn 1;  "))
        assertEquals(expected, toCDPCompatibleExpression("return   document.title; return 1;  "))
        assertEquals(expected, toCDPCompatibleExpression("return   \ndocument.title; return 1;  "))
        assertEquals(expected, toCDPCompatibleExpression("   return   document.title; return 1;  "))
        assertEquals(expected, toCDPCompatibleExpression("\n\nreturn\ndocument.title; return 1;  "))
    }

    @Test
        @DisplayName("test toCDPCompatibleExpression wraps single line object literal")
    fun testTocdpcompatibleexpressionWrapsSingleLineObjectLiteral() {
        val input = "{ answer: 42 }"
        val expected = "({ answer: 42 });"
        assertEquals(expected, toCDPCompatibleExpression(input))
    }

    @Test
        @DisplayName("test toCDPCompatibleExpression converts single line function expression")
    fun testTocdpcompatibleexpressionConvertsSingleLineFunctionExpression() {
        val input = "function() { return 1 }"
        val expected = "(function() { return 1 })();"
        assertEquals(expected, toCDPCompatibleExpression(input))
    }

    @Test
        @DisplayName("test toCDPCompatibleExpression keeps normal expression")
    fun testTocdpcompatibleexpressionKeepsNormalExpression() {
        val input = "  document.title  "
        val expected = "document.title"
        assertEquals(expected, toCDPCompatibleExpression(input))
    }
}
