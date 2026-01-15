package ai.platon.pulsar.agentic.skills

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

/**
 * Tests for the Skill interface and related classes.
 */
class SkillTest {

    private lateinit var context: SkillContext

    @BeforeEach
    fun setup() {
        context = SkillContext(sessionId = "test-session-123")
    }

    @Test
    fun `test SkillMetadata creation with valid data`() {
        val metadata = SkillMetadata(
            id = "test-skill",
            name = "Test Skill",
            version = "1.0.0",
            description = "A test skill",
            author = "Test Author"
        )

        assertEquals("test-skill", metadata.id)
        assertEquals("Test Skill", metadata.name)
        assertEquals("1.0.0", metadata.version)
        assertEquals("A test skill", metadata.description)
        assertEquals("Test Author", metadata.author)
    }

    @Test
    fun `test SkillMetadata fails with blank id`() {
        assertThrows<IllegalArgumentException> {
            SkillMetadata(
                id = "",
                name = "Test Skill"
            )
        }
    }

    @Test
    fun `test SkillMetadata fails with blank name`() {
        assertThrows<IllegalArgumentException> {
            SkillMetadata(
                id = "test-skill",
                name = ""
            )
        }
    }

    @Test
    fun `test SkillMetadata fails with invalid version`() {
        assertThrows<IllegalArgumentException> {
            SkillMetadata(
                id = "test-skill",
                name = "Test Skill",
                version = "1.0"
            )
        }
    }

    @Test
    fun `test SkillMetadata with dependencies and tags`() {
        val metadata = SkillMetadata(
            id = "test-skill",
            name = "Test Skill",
            dependencies = listOf("dep1", "dep2"),
            tags = setOf("tag1", "tag2")
        )

        assertEquals(2, metadata.dependencies.size)
        assertTrue(metadata.dependencies.contains("dep1"))
        assertEquals(2, metadata.tags.size)
        assertTrue(metadata.tags.contains("tag1"))
    }

    @Test
    fun `test SkillContext get and set config`() {
        val context = SkillContext(
            sessionId = "test-session",
            config = mapOf("key1" to "value1", "key2" to 42)
        )

        assertEquals("value1", context.getConfig("key1", "default"))
        assertEquals(42, context.getConfig("key2", 0))
        assertEquals("default", context.getConfig("key3", "default"))
    }

    @Test
    fun `test SkillContext get and set resource`() {
        val context = SkillContext(sessionId = "test-session")

        context.setResource("resource1", "value1")
        context.setResource("resource2", 123)

        assertEquals("value1", context.getResource<String>("resource1"))
        assertEquals(123, context.getResource<Int>("resource2"))
        Assertions.assertNull(context.getResource<String>("nonexistent"))
    }

    @Test
    fun `test SkillResult success creation`() {
        val result = SkillResult.success(data = "test data", message = "success")

        assertTrue(result.success)
        assertEquals("test data", result.data)
        assertEquals("success", result.message)
    }

    @Test
    fun `test SkillResult failure creation`() {
        val result = SkillResult.failure(message = "failure", metadata = mapOf("error" to "test"))

        assertFalse(result.success)
        assertEquals("failure", result.message)
        assertEquals("test", result.metadata["error"])
    }

    @Test
    fun `test AbstractSkill load state tracking`() = runBlocking {
        val skill = TestSkill()

        assertFalse(skill.isLoaded())

        skill.onLoad(context)
        assertTrue(skill.isLoaded())

        skill.onUnload(context)
        assertFalse(skill.isLoaded())
    }

    @Test
    fun `test AbstractSkill prevents double loading`() = runBlocking {
        val skill = TestSkill()

        skill.onLoad(context)

        assertThrows<IllegalStateException> {
            runBlocking { skill.onLoad(context) }
        }
    }

    @Test
    fun `test AbstractSkill prevents unload without load`() = runBlocking {
        val skill = TestSkill()

        assertThrows<IllegalStateException> {
            runBlocking { skill.onUnload(context) }
        }
    }

    @Test
    fun `test Skill lifecycle hooks are called`() = runBlocking {
        val skill = LifecycleTestSkill()
        val params = mapOf("key" to "value")

        skill.onLoad(context)
        assertTrue(skill.loadCalled)

        assertTrue(skill.onBeforeExecute(context, params))
        assertTrue(skill.beforeExecuteCalled)

        val result = skill.execute(context, params)
        assertTrue(skill.executeCalled)

        skill.onAfterExecute(context, params, result)
        assertTrue(skill.afterExecuteCalled)

        skill.onUnload(context)
        assertTrue(skill.unloadCalled)
    }

    @Test
    fun `test Skill validation`() = runBlocking {
        val skill = TestSkill()
        assertTrue(skill.validate(context))
    }

    // Test helper classes

    class TestSkill : AbstractSkill() {
        override val metadata = SkillMetadata(
            id = "test-skill",
            name = "Test Skill",
            version = "1.0.0"
        )

        override suspend fun execute(context: SkillContext, params: Map<String, Any>): SkillResult {
            return SkillResult.success(message = "Test executed")
        }
    }

    class LifecycleTestSkill : AbstractSkill() {
        var loadCalled = false
        var unloadCalled = false
        var beforeExecuteCalled = false
        var afterExecuteCalled = false
        var executeCalled = false

        override val metadata = SkillMetadata(
            id = "lifecycle-test",
            name = "Lifecycle Test",
            version = "1.0.0"
        )

        override suspend fun onLoad(context: SkillContext) {
            super.onLoad(context)
            loadCalled = true
        }

        override suspend fun onUnload(context: SkillContext) {
            super.onUnload(context)
            unloadCalled = true
        }

        override suspend fun onBeforeExecute(context: SkillContext, params: Map<String, Any>): Boolean {
            beforeExecuteCalled = true
            return true
        }

        override suspend fun onAfterExecute(context: SkillContext, params: Map<String, Any>, result: SkillResult) {
            afterExecuteCalled = true
        }

        override suspend fun execute(context: SkillContext, params: Map<String, Any>): SkillResult {
            executeCalled = true
            return SkillResult.success(message = "Executed")
        }
    }
}
