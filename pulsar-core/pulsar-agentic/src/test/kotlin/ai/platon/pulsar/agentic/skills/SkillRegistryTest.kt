package ai.platon.pulsar.agentic.skills

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

/**
 * Tests for SkillRegistry functionality.
 */
class SkillRegistryTest {

    private lateinit var registry: SkillRegistry
    private lateinit var context: SkillContext

    @BeforeEach
    fun setup() = runBlocking {
        registry = SkillRegistry.instance
        context = SkillContext(sessionId = "test-session-123")
        registry.clear(context)
    }

    @AfterEach
    fun cleanup() = runBlocking {
        registry.clear(context)
    }

    @Test
    fun `test register skill`() = runBlocking {
        val skill = TestSkill("test-skill-1")

        registry.register(skill, context)

        assertTrue(registry.contains("test-skill-1"))
        assertEquals(1, registry.size())
        assertEquals(skill, registry.get("test-skill-1"))
    }

    @Test
    fun `test register duplicate skill throws exception`() = runBlocking {
        val skill1 = TestSkill("test-skill")
        val skill2 = TestSkill("test-skill")

        registry.register(skill1, context)

        val exception = assertThrows<IllegalArgumentException> {
            runBlocking { registry.register(skill2, context) }
        }
        assertTrue(exception.message!!.contains("already registered"))
    }

    @Test
    fun `test register skill with missing dependencies fails`() = runBlocking {
        val skill = TestSkill("test-skill", dependencies = listOf("missing-dep"))

        val exception = assertThrows<IllegalStateException> {
            runBlocking { registry.register(skill, context) }
        }
        assertTrue(exception.message!!.contains("missing dependencies"))
    }

    @Test
    fun `test register skill with satisfied dependencies succeeds`() = runBlocking {
        val dep = TestSkill("dependency")
        val skill = TestSkill("test-skill", dependencies = listOf("dependency"))

        registry.register(dep, context)
        registry.register(skill, context)

        assertTrue(registry.contains("test-skill"))
        assertEquals(2, registry.size())
    }

    @Test
    fun `test unregister skill`() = runBlocking {
        val skill = TestSkill("test-skill")
        registry.register(skill, context)

        val removed = registry.unregister("test-skill", context)

        assertTrue(removed)
        assertFalse(registry.contains("test-skill"))
        assertEquals(0, registry.size())
    }

    @Test
    fun `test unregister non-existent skill returns false`() = runBlocking {
        val removed = registry.unregister("nonexistent", context)

        assertFalse(removed)
    }

    @Test
    fun `test unregister skill with dependents fails`() = runBlocking {
        val dep = TestSkill("dependency")
        val skill = TestSkill("test-skill", dependencies = listOf("dependency"))

        registry.register(dep, context)
        registry.register(skill, context)

        val exception = assertThrows<IllegalStateException> {
            runBlocking { registry.unregister("dependency", context) }
        }
        assertTrue(exception.message!!.contains("required by"))
    }

    @Test
    fun `test get all skills`() = runBlocking {
        val skill1 = TestSkill("skill-1")
        val skill2 = TestSkill("skill-2")

        registry.register(skill1, context)
        registry.register(skill2, context)

        val all = registry.getAll()
        assertEquals(2, all.size)
        assertTrue(all.contains(skill1))
        assertTrue(all.contains(skill2))
    }

    @Test
    fun `test get all skill IDs`() = runBlocking {
        registry.register(TestSkill("skill-1"), context)
        registry.register(TestSkill("skill-2"), context)

        val ids = registry.getAllIds()
        assertEquals(2, ids.size)
        assertTrue(ids.contains("skill-1"))
        assertTrue(ids.contains("skill-2"))
    }

    @Test
    fun `test find skills by tag`() = runBlocking {
        val skill1 = TestSkill("skill-1", tags = setOf("tag1", "tag2"))
        val skill2 = TestSkill("skill-2", tags = setOf("tag2", "tag3"))
        val skill3 = TestSkill("skill-3", tags = setOf("tag3"))

        registry.register(skill1, context)
        registry.register(skill2, context)
        registry.register(skill3, context)

        val withTag1 = registry.findByTag("tag1")
        assertEquals(1, withTag1.size)
        assertTrue(withTag1.contains(skill1))

        val withTag2 = registry.findByTag("tag2")
        assertEquals(2, withTag2.size)
        assertTrue(withTag2.contains(skill1))
        assertTrue(withTag2.contains(skill2))

        val withTag3 = registry.findByTag("tag3")
        assertEquals(2, withTag3.size)
    }

    @Test
    fun `test find skills by author`() = runBlocking {
        val skill1 = TestSkill("skill-1", author = "Author A")
        val skill2 = TestSkill("skill-2", author = "Author A")
        val skill3 = TestSkill("skill-3", author = "Author B")

        registry.register(skill1, context)
        registry.register(skill2, context)
        registry.register(skill3, context)

        val byAuthorA = registry.findByAuthor("Author A")
        assertEquals(2, byAuthorA.size)
        assertTrue(byAuthorA.contains(skill1))
        assertTrue(byAuthorA.contains(skill2))

        val byAuthorB = registry.findByAuthor("Author B")
        assertEquals(1, byAuthorB.size)
        assertTrue(byAuthorB.contains(skill3))
    }

    @Test
    fun `test execute skill by ID`() = runBlocking {
        val skill = TestSkill("test-skill")
        registry.register(skill, context)

        val result = registry.execute("test-skill", context, mapOf("key" to "value"))

        assertTrue(result.success)
        assertEquals("Executed", result.message)
    }

    @Test
    fun `test execute non-existent skill throws exception`() = runBlocking {
        val exception = assertThrows<IllegalArgumentException> {
            runBlocking { registry.execute("nonexistent", context) }
        }
        assertTrue(exception.message!!.contains("not registered"))
    }

    @Test
    fun `test execute calls lifecycle hooks`() = runBlocking {
        val skill = LifecycleTrackingSkill("test-skill")
        registry.register(skill, context)

        registry.execute("test-skill", context, mapOf("key" to "value"))

        assertTrue(skill.beforeExecuteCalled)
        assertTrue(skill.afterExecuteCalled)
    }

    @Test
    fun `test execute with onBeforeExecute returning false`() = runBlocking {
        val skill = object : AbstractSkill() {
            override val metadata = SkillMetadata(id = "test-skill", name = "Test", version = "1.0.0")
            override suspend fun execute(context: SkillContext, params: Map<String, Any>) =
                SkillResult.success()
            override suspend fun onBeforeExecute(context: SkillContext, params: Map<String, Any>) = false
        }

        registry.register(skill, context)
        val result = registry.execute("test-skill", context)

        assertFalse(result.success)
        assertTrue(result.message!!.contains("cancelled"))
    }

    @Test
    fun `test clear all skills`() = runBlocking {
        registry.register(TestSkill("skill-1"), context)
        registry.register(TestSkill("skill-2"), context)

        registry.clear(context)

        assertEquals(0, registry.size())
    }

    @Test
    fun `test get all metadata`() = runBlocking {
        val skill1 = TestSkill("skill-1")
        val skill2 = TestSkill("skill-2")

        registry.register(skill1, context)
        registry.register(skill2, context)

        val metadata = registry.getAllMetadata()
        assertEquals(2, metadata.size)
        assertTrue(metadata.any { it.id == "skill-1" })
        assertTrue(metadata.any { it.id == "skill-2" })
    }

    // Test helper classes

    class TestSkill(
        id: String,
        dependencies: List<String> = emptyList(),
        tags: Set<String> = emptySet(),
        author: String = ""
    ) : AbstractSkill() {
        override val metadata = SkillMetadata(
            id = id,
            name = "Test Skill $id",
            version = "1.0.0",
            dependencies = dependencies,
            tags = tags,
            author = author
        )

        override suspend fun execute(context: SkillContext, params: Map<String, Any>): SkillResult {
            return SkillResult.success(message = "Executed")
        }
    }

    class LifecycleTrackingSkill(id: String) : AbstractSkill() {
        var beforeExecuteCalled = false
        var afterExecuteCalled = false

        override val metadata = SkillMetadata(id = id, name = "Lifecycle Test", version = "1.0.0")

        override suspend fun onBeforeExecute(context: SkillContext, params: Map<String, Any>): Boolean {
            beforeExecuteCalled = true
            return true
        }

        override suspend fun onAfterExecute(context: SkillContext, params: Map<String, Any>, result: SkillResult) {
            afterExecuteCalled = true
        }

        override suspend fun execute(context: SkillContext, params: Map<String, Any>): SkillResult {
            return SkillResult.success(message = "Executed")
        }
    }
}
