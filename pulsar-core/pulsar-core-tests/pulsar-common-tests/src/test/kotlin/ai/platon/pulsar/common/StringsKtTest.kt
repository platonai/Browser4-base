package ai.platon.pulsar.common

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.IOException
import kotlin.test.assertEquals

class StringsKtTest {

    @Test
        @DisplayName("test readableClassName with KClass object")
    fun testReadableclassnameWithKclassObject() {
        val kclass = KStringsTest::class
        val result = readableClassName(kclass)
        assertEquals("a.p.p.c.KStringsTest", result)
    }

    @Test
        @DisplayName("test readableClassName with Companion object")
    fun testReadableclassnameWithCompanionObject() {
        val obj = TestCompanion
        val result = readableClassName(obj)
        assertTrue(result.contains("C"))
    }

    @Test
        @DisplayName("test readableClassName with nested class")
    fun testReadableclassnameWithNestedClass() {
        val obj = TestNestedClass()
        val result = readableClassName(obj)
        assertTrue(result.contains("_"))
    }

    @Test
        @DisplayName("test prependReadableClassName with whitespace ident")
    fun testPrependreadableclassnameWithWhitespaceIdent() {
        val obj = KStringsTest()
        val ident = "   "
        val name = "testName"
        val result = prependReadableClassName(obj, ident, name, ".")
        assertEquals("a.p.p.c.KStringsTest.testName", result)
    }

    @Test
        @DisplayName("test stringifyException with prefix")
    fun testStringifyexceptionWithPrefix() {
        val exception = RuntimeException("Test exception")
        val prefix = "ERROR: "
        val result = stringifyException(exception, prefix)
        assertTrue(result.startsWith("ERROR: "))
        assertTrue(result.contains("Test exception"))
    }

    @Test
        @DisplayName("test stringifyException with postfix")
    fun testStringifyexceptionWithPostfix() {
        val exception = RuntimeException("Test exception")
        val postfix = " [END]"
        val result = stringifyException(exception, postfix = postfix)
        assertTrue(result.contains("Test exception"))
        assertTrue(result.endsWith(" [END]"))
    }

    @Test
        @DisplayName("test stringifyException with both prefix and postfix")
    fun testStringifyexceptionWithBothPrefixAndPostfix() {
        val exception = RuntimeException("Test exception")
        val prefix = "ERROR: "
        val postfix = " [END]"
        val result = stringifyException(exception, prefix, postfix)
        assertTrue(result.startsWith("ERROR: "))
        assertTrue(result.contains("Test exception"))
        assertTrue(result.endsWith(" [END]"))
    }

    @Test
        @DisplayName("test stringifyException with nested exception")
    fun testStringifyexceptionWithNestedException() {
        val innerException = IllegalArgumentException("Inner exception")
        val outerException = RuntimeException("Outer exception", innerException)
        val result = stringifyException(outerException)
        assertTrue(result.contains("Outer exception"))
        assertTrue(result.contains("Inner exception"))
        assertTrue(result.contains("Caused by:"))
    }

    @Test
        @DisplayName("test simplifyException with simple message")
    fun testSimplifyexceptionWithSimpleMessage() {
        val exception = RuntimeException("Simple error message")
        val result = simplifyException(exception)
        assertEquals("Simple error message", result)
    }

    @Test
        @DisplayName("test simplifyException with multiline message")
    fun testSimplifyexceptionWithMultilineMessage() {
        val exception = RuntimeException("First line\nSecond line")
        val result = simplifyException(exception)
        assertEquals("First line\tSecond line", result)
    }

    @Test
        @DisplayName("test simplifyException with three line message")
    fun testSimplifyexceptionWithThreeLineMessage() {
        val exception = RuntimeException("First line\nSecond line\nThird line")
        val result = simplifyException(exception)
        assertEquals("First line\tSecond line ...", result)
    }

    @Test
        @DisplayName("test simplifyException with prefix")
    fun testSimplifyexceptionWithPrefix() {
        val exception = RuntimeException("Error message")
        val prefix = "ALERT: "
        val result = simplifyException(exception, prefix)
        assertEquals("ALERT: Error message", result)
    }

    @Test
        @DisplayName("test simplifyException with postfix")
    fun testSimplifyexceptionWithPostfix() {
        val exception = RuntimeException("Error message")
        val postfix = " (handled)"
        val result = simplifyException(exception, postfix = postfix)
        assertEquals("Error message (handled)", result)
    }

    @Test
        @DisplayName("test simplifyException with both prefix and postfix")
    fun testSimplifyexceptionWithBothPrefixAndPostfix() {
        val exception = RuntimeException("Error message")
        val prefix = "ALERT: "
        val postfix = " (handled)"
        val result = simplifyException(exception, prefix, postfix)
        assertEquals("ALERT: Error message (handled)", result)
    }

    @Test
        @DisplayName("test readableClassName with very long class name")
    fun testReadableclassnameWithVeryLongClassName() {
        val obj = TestVeryLongClassNameThatGoesOnAndOn()
        val result = readableClassName(obj, maxPartCount = 5)
        assertTrue(result.contains("TestVeryLongClassNameThatGoesOnAndOn"))
    }

    @ParameterizedTest
    @ValueSource(strings = [".", "-", "_", "/", "|", "::"])
        @DisplayName("test prependReadableClassName with various separators")
    fun testPrependreadableclassnameWithVariousSeparators(separator: String) {
        val obj = KStringsTest()
        val name = "testName"
        val result = prependReadableClassName(obj, name, separator)
        assertTrue(result.contains("KStringsTest")) { result }
        assertTrue(result.contains("testName")) { result }
    }

    @Test
        @DisplayName("test stringifyException with IOException")
    fun testStringifyexceptionWithIoexception() {
        val exception = IOException("File not found")
        val result = stringifyException(exception)
        assertTrue(result.contains("File not found"))
        assertTrue(result.contains("IOException"))
    }

    @Test
        @DisplayName("test simplifyException with IOException")
    fun testSimplifyexceptionWithIoexception() {
        val exception = IOException("File not found")
        val result = simplifyException(exception)
        assertEquals("File not found", result)
    }

    companion object TestCompanion

    class TestNestedClass

    class TestVeryLongClassNameThatGoesOnAndOn
}
