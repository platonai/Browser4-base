package ai.platon.pulsar.common

import ai.platon.pulsar.common.PropertyNameStyle.kebabToCamelCase
import ai.platon.pulsar.common.PropertyNameStyle.toDotSeparatedKebabCase
import ai.platon.pulsar.common.PropertyNameStyle.toUpperUnderscoreCase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.Test

class PropertyNameStyleTest {

    @ParameterizedTest
    @CsvSource(
        "SPRING_PROFILES_ACTIVE, spring.profiles.active",
        "server.servlet.contextPath, server.servlet.context-path",
        "my.main-project.person.firstName, my.main-project.person.first-name",

        "OPENROUTER_API_KEY, openrouter.api.key",
        "llm.apiKey, llm.api-key",

        "MY_CUSTOM_SETTING, my.custom.setting",
        "DATABASE_URL, database.url",
        "REDIS_HOST, redis.host",
        "JWT_SECRET_KEY, jwt.secret.key",
        "SPRING_DATASOURCE_URL, spring.datasource.url",
        "SPRING_REDIS_HOST, spring.redis.host",
        "SPRING_MAIL_HOST, spring.mail.host",
        // Additional test cases
        "SINGLE_WORD, single.word",
        "MULTIPLE___UNDERSCORES, multiple.underscores",
        "LEADING_UNDERSCORE, leading.underscore",
        "TRAILING_UNDERSCORE_, trailing.underscore.",
        "MIXED_CASE_With_Some_Caps, mixed.case.with.some.caps",
        "NUMBER_123_IN_MIDDLE, number.123.in.middle",
        "NUMBER_AT_END_123, number.at.end.123",
        "EMPTY__MIDDLE, empty.middle",
        "CONSECUTIVE__UNDERSCORES, consecutive.underscores"
    )
        @DisplayName("test toKebabCase converts env vars to Spring Boot properties")
    fun testTokebabcaseConvertsEnvVarsToSpringBootProperties(input: String, expected: String) {
        assertEquals(expected, toDotSeparatedKebabCase(input))
    }

    @Test
        @DisplayName("test toKebabCase handles empty string")
    fun testTokebabcaseHandlesEmptyString() {
        assertEquals("", toDotSeparatedKebabCase(""))
    }

    @Test
        @DisplayName("test toKebabCase handles single word")
    fun testTokebabcaseHandlesSingleWord() {
        assertEquals("spring", toDotSeparatedKebabCase("SPRING"))
    }

    @Test
        @DisplayName("test toKebabCase handles already converted string")
    fun testTokebabcaseHandlesAlreadyConvertedString() {
        assertEquals("spring.profiles.active", toDotSeparatedKebabCase("spring.profiles.active"))
    }

    @Test
        @DisplayName("test toKebabCase handles only underscores")
    fun testTokebabcaseHandlesOnlyUnderscores() {
        assertEquals(".", toDotSeparatedKebabCase("___"))
    }

    @Test
        @DisplayName("test toKebabCase handles mixed separators")
    fun testTokebabcaseHandlesMixedSeparators() {
        assertEquals("spring.profiles.active", toDotSeparatedKebabCase("SPRING.PROFILES_ACTIVE"))
    }

    @Test
        @DisplayName("test toKebabCase handles unicode characters")
    fun testTokebabcaseHandlesUnicodeCharacters() {
        assertEquals("test.测试.テスト", toDotSeparatedKebabCase("TEST_测试_テスト"))
    }

    @Test
        @DisplayName("test toKebabCase handles special characters")
    fun testTokebabcaseHandlesSpecialCharacters() {
        assertEquals("test.special.chars!@#$%^&*()", toDotSeparatedKebabCase("TEST_SPECIAL_CHARS!@#$%^&*()"))
    }

    @Test
        @DisplayName("test toKebabCase handles whitespace")
    fun testTokebabcaseHandlesWhitespace() {
        assertEquals("test.with.whitespace", toDotSeparatedKebabCase("TEST WITH WHITESPACE"))
    }

    @Test
        @DisplayName("test toKebabCase handles very long input")
    fun testTokebabcaseHandlesVeryLongInput() {
        val longInput = "A_VERY_LONG_ENVIRONMENT_VARIABLE_NAME_THAT_GOES_ON_AND_ON_AND_ON_AND_ON_AND_ON"
        val expected = "a.very.long.environment.variable.name.that.goes.on.and.on.and.on.and.on.and.on"
        assertEquals(expected, toDotSeparatedKebabCase(longInput))
    }













    /**
     *
     * */
    @Test
        @DisplayName("TC01 example(dot)property-name should convert to EXAMPLE_PROPERTYNAME")
    fun tc01ExampleDotPropertyNameShouldConvertToExamplePropertyname() {
        val result = toUpperUnderscoreCase("example.property-name")
        assertEquals("EXAMPLE_PROPERTYNAME", result)
    }

    @Test
        @DisplayName("TC02 hello(dot)world should convert to HELLO_WORLD")
    fun tc02HelloDotWorldShouldConvertToHelloWorld() {
        val result = toUpperUnderscoreCase("hello.world")
        assertEquals("HELLO_WORLD", result)
    }

    @Test
        @DisplayName("TC03 user name should convert to USER_NAME")
    fun tc03UserNameShouldConvertToUserName() {
        val result = toUpperUnderscoreCase("user name")
        assertEquals("USER_NAME", result)
    }

    @Test
        @DisplayName("TC04 some-random-test should convert to SOMERANDOMTEST")
    fun tc04SomeRandomTestShouldConvertToSomerandomtest() {
        val result = toUpperUnderscoreCase("some-random-test")
        assertEquals("SOMERANDOMTEST", result)
    }

    @Test
        @DisplayName("TC05 a(dot)b c-d should convert to A_B_C_D")
    fun tc05ADotBCDShouldConvertToABCD() {
        val result = toUpperUnderscoreCase("a.b c-d")
        assertEquals("A_B_CD", result)
    }

    @Test
        @DisplayName("TC06 empty string should return empty")
    fun tc06EmptyStringShouldReturnEmpty() {
        val result = toUpperUnderscoreCase("")
        assertEquals("", result)
    }

    @Test
        @DisplayName("TC07 UPPER(dot)CASE should convert to UPPER_CASE")
    fun tc07UpperDotCaseShouldConvertToUpperCase() {
        val result = toUpperUnderscoreCase("UPPER.CASE")
        assertEquals("UPPER_CASE", result)
    }

    @Test
        @DisplayName("TC08 lower-case(dot)test should convert to LOWERCASE_TEST")
    fun tc08LowerCaseDotTestShouldConvertToLowercaseTest() {
        val result = toUpperUnderscoreCase("lower-case.test")
        assertEquals("LOWERCASE_TEST", result)
    }

    // Tests for kebabToCamelCase function
    @ParameterizedTest
    @CsvSource(
        "spring, spring",
        "spring.profiles.active, springProfilesActive",
        "SPRING_PROFILES_ACTIVE, springProfilesActive",
        "spring-profiles-active, springProfilesActive",
        "my.main-project.person, myMainProjectPerson",
        "user-name, userName",
        "first-name, firstName",
        "api.key, apiKey",
        "database-url, databaseUrl",
        "jwt.secret-key, jwtSecretKey",
        "spring.datasource.url, springDatasourceUrl",
        "spring-redis-host, springRedisHost",
        "mixed.case_with-separators, mixedCaseWithSeparators"
    )
        @DisplayName("test kebabToCamelCase converts various formats to camelCase")
    fun testKebabtocamelcaseConvertsVariousFormatsToCamelcase(input: String, expected: String) {
        assertEquals(expected, kebabToCamelCase(input))
    }

    @Test
        @DisplayName("test kebabToCamelCase handles single word")
    fun testKebabtocamelcaseHandlesSingleWord() {
        assertEquals("spring", kebabToCamelCase("spring"))
        assertEquals("UPPERCASE", kebabToCamelCase("UPPERCASE"))
        assertEquals("MixedCase", kebabToCamelCase("MixedCase"))
    }

    @Test
        @DisplayName("test kebabToCamelCase handles empty string")
    fun testKebabtocamelcaseHandlesEmptyString() {
        assertEquals("", kebabToCamelCase(""))
    }

    @Test
        @DisplayName("test kebabToCamelCase handles whitespace")
    fun testKebabtocamelcaseHandlesWhitespace() {
        assertEquals("", kebabToCamelCase("   "))
        assertEquals("hello", kebabToCamelCase("  hello  "))
    }

    @Test
        @DisplayName("test kebabToCamelCase handles multiple consecutive separators")
    fun testKebabtocamelcaseHandlesMultipleConsecutiveSeparators() {
        assertEquals("springProfilesActive", kebabToCamelCase("spring...profiles___active---"))
        assertEquals("testMultipleSeparators", kebabToCamelCase("test...__--multiple--__..separators"))
    }

    @Test
        @DisplayName("test kebabToCamelCase handles leading and trailing separators")
    fun testKebabtocamelcaseHandlesLeadingAndTrailingSeparators() {
        assertEquals("springProfiles", kebabToCamelCase("___spring.profiles___"))
        assertEquals("apiKey", kebabToCamelCase("---api-key---"))
        assertEquals("testValue", kebabToCamelCase("...test.value..."))
    }

    @Test
        @DisplayName("test kebabToCamelCase handles mixed separators")
    fun testKebabtocamelcaseHandlesMixedSeparators() {
        assertEquals("springProfilesActive", kebabToCamelCase("spring.profiles_active"))
        assertEquals("mixedSeparatorTest", kebabToCamelCase("mixed_separator.test"))
    }

    @Test
        @DisplayName("test kebabToCamelCase handles numeric values")
    fun testKebabtocamelcaseHandlesNumericValues() {
        assertEquals("version2", kebabToCamelCase("version-2"))
        assertEquals("api2Key", kebabToCamelCase("api2.key"))
        assertEquals("test123Value", kebabToCamelCase("test123_value"))
    }

    @Test
        @DisplayName("test kebabToCamelCase handles special characters in segments")
    fun testKebabtocamelcaseHandlesSpecialCharactersInSegments() {
        assertEquals("test@#\$Special!%^&chars", kebabToCamelCase("test@#\$.special!%^&chars"))
        assertEquals("apiKeyValue", kebabToCamelCase("api-key-value"))
    }

    @Test
        @DisplayName("test kebabToCamelCase handles unicode characters")
    fun testKebabtocamelcaseHandlesUnicodeCharacters() {
        assertEquals("测试Value", kebabToCamelCase("测试.value"))
        assertEquals("testテスト", kebabToCamelCase("test_テスト"))
    }

    @Test
        @DisplayName("test kebabToCamelCase handles very long input")
    fun testKebabtocamelcaseHandlesVeryLongInput() {
        val input = "very.long.property.name.with.many.segments.that.goes.on.and.on"
        val expected = "veryLongPropertyNameWithManySegmentsThatGoesOnAndOn"
        assertEquals(expected, kebabToCamelCase(input))
    }

    @Test
        @DisplayName("test kebabToCamelCase handles edge cases")
    fun testKebabtocamelcaseHandlesEdgeCases() {
        // Single character segments
        assertEquals("aBC", kebabToCamelCase("a.b.c"))

        // Empty segments between separators
        assertEquals("startEnd", kebabToCamelCase("start..end"))
    }

    @Test
        @DisplayName("test kebabToCamelCase real world examples")
    fun testKebabtocamelcaseRealWorldExamples() {
        // Common Spring Boot properties
        assertEquals("springProfilesActive", kebabToCamelCase("spring.profiles.active"))
        assertEquals("serverPort", kebabToCamelCase("server.port"))
        assertEquals("springDatasourceUrl", kebabToCamelCase("spring.datasource.url"))
        assertEquals("springRedisHost", kebabToCamelCase("spring.redis.host"))

        // Environment variables
        assertEquals("springProfilesActive", kebabToCamelCase("SPRING_PROFILES_ACTIVE"))
        assertEquals("databaseUrl", kebabToCamelCase("DATABASE_URL"))
        assertEquals("jwtSecretKey", kebabToCamelCase("JWT_SECRET_KEY"))

        // Kebab case
        assertEquals("myPropertyName", kebabToCamelCase("my-property-name"))
        assertEquals("userName", kebabToCamelCase("user-name"))
        assertEquals("firstName", kebabToCamelCase("first-name"))
    }
}
