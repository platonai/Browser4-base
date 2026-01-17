package ai.platon.pulsar.agentic.skills

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

/**
 * Tests for SkillLoader and SkillComposer functionality.
 */
class SkillLoaderTest {

    private lateinit var registry: SkillRegistry
    private lateinit var loader: SkillLoader
    private lateinit var context: SkillContext

    @BeforeEach
    fun setup() = runBlocking {
        registry = SkillRegistry.instance
        loader = SkillLoader(registry)
        context = SkillContext(sessionId = "test-session-123")
        registry.clear(context)
    }

    @AfterEach
    fun cleanup() = runBlocking {
        registry.clear(context)
    }

    @Test
    fun `test load single skill`() = runBlocking {
        val skill = TestSkill("test-skill")

        val success = loader.load(skill, context)

        assertTrue(success)
        assertTrue(registry.contains("test-skill"))
    }

    @Test
    fun `test load skill with invalid metadata fails`() = runBlocking {
        val skill = object : AbstractSkill() {
            override val metadata = SkillMetadata(id = "test", name = "Test", version = "1.0.0")
            override suspend fun execute(context: SkillContext, params: Map<String, Any>) =
                SkillResult.success()
            override suspend fun validate(context: SkillContext) = false
        }

        val success = loader.load(skill, context)

        assertFalse(success)
        assertFalse(registry.contains("test"))
    }

    @Test
    fun `test load multiple skills with dependencies`() = runBlocking {
        val dep = TestSkill("dependency")
        val skill1 = TestSkill("skill-1", dependencies = listOf("dependency"))
        val skill2 = TestSkill("skill-2", dependencies = listOf("skill-1"))

        val results = loader.loadAll(listOf(skill2, skill1, dep), context)

        assertEquals(3, results.size)
        assertTrue(results["dependency"] == true)
        assertTrue(results["skill-1"] == true)
        assertTrue(results["skill-2"] == true)
        assertEquals(3, registry.size())
    }

    @Test
    fun `test load multiple skills with circular dependencies fails`() = runBlocking {
        val skill1 = TestSkill("skill-1", dependencies = listOf("skill-2"))
        val skill2 = TestSkill("skill-2", dependencies = listOf("skill-1"))

        val results = loader.loadAll(listOf(skill1, skill2), context)

        assertEquals(2, results.size)
        assertFalse(results["skill-1"]!!)
        assertFalse(results["skill-2"]!!)
        assertEquals(0, registry.size())
    }

    @Test
    fun `test load multiple skills with missing dependencies fails`() = runBlocking {
        val skill = TestSkill("test-skill", dependencies = listOf("missing"))

        val results = loader.loadAll(listOf(skill), context)

        assertEquals(1, results.size)
        assertFalse(results["test-skill"]!!)
        assertEquals(0, registry.size())
    }

    @Test
    fun `test reload skill`() = runBlocking {
        val skill = TestSkill("test-skill")
        loader.load(skill, context)

        val reloaded = loader.reload(skill, context)

        assertTrue(reloaded)
        assertTrue(registry.contains("test-skill"))
    }

    @Test
    fun `test unload skill`() = runBlocking {
        val skill = TestSkill("test-skill")
        loader.load(skill, context)

        val unloaded = loader.unload("test-skill", context)

        assertTrue(unloaded)
        assertFalse(registry.contains("test-skill"))
    }

    @Test
    fun `test unload non-existent skill returns false`() = runBlocking {
        val unloaded = loader.unload("nonexistent", context)

        assertFalse(unloaded)
    }

    @Test
    fun `test unload multiple skills`() = runBlocking {
        loader.load(TestSkill("skill-1"), context)
        loader.load(TestSkill("skill-2"), context)

        val results = loader.unloadAll(listOf("skill-1", "skill-2"), context)

        assertEquals(2, results.size)
        assertTrue(results["skill-1"] == true)
        assertTrue(results["skill-2"] == true)
        assertEquals(0, registry.size())
    }

    // Test helper class

    class TestSkill(
        id: String,
        dependencies: List<String> = emptyList()
    ) : AbstractSkill() {
        override val metadata = SkillMetadata(
            id = id,
            name = "Test Skill $id",
            version = "1.0.0",
            dependencies = dependencies
        )

        override suspend fun execute(context: SkillContext, params: Map<String, Any>): SkillResult {
            return SkillResult.success(message = "Executed")
        }
    }
}

/**
 * Tests for SkillComposer functionality.
 */
class SkillComposerTest {

    private lateinit var registry: SkillRegistry
    private lateinit var composer: SkillComposer
    private lateinit var context: SkillContext

    @BeforeEach
    fun setup() = runBlocking {
        registry = SkillRegistry.instance
        composer = SkillComposer(registry)
        context = SkillContext(sessionId = "test-session-123")
        registry.clear(context)
    }

    @AfterEach
    fun cleanup() = runBlocking {
        registry.clear(context)
    }

    @Test
    fun `test create sequential composite skill`() = runBlocking {
        // Register component skills
        registry.register(TestSkill("skill-1"), context)
        registry.register(TestSkill("skill-2"), context)
        registry.register(TestSkill("skill-3"), context)

        val composite = composer.sequential("composite", listOf("skill-1", "skill-2", "skill-3"))

        Assertions.assertNotNull(composite)
        assertEquals("composite", composite.metadata.id)
        assertEquals(3, composite.metadata.dependencies.size)
    }

    @Test
    fun `test sequential composite skill execution`() = runBlocking {
        // Register component skills
        val skill1 = CountingSkill("skill-1")
        val skill2 = CountingSkill("skill-2")
        registry.register(skill1, context)
        registry.register(skill2, context)

        val composite = composer.sequential("composite", listOf("skill-1", "skill-2"))
        registry.register(composite, context)

        val result = registry.execute("composite", context)

        assertTrue(result.success)
        assertEquals(1, skill1.executionCount)
        assertEquals(1, skill2.executionCount)

        @Suppress("UNCHECKED_CAST")
        val results = result.data as List<SkillResult>
        assertEquals(2, results.size)
        assertTrue(results.all { it.success })
    }

    @Test
    fun `test sequential composite fails on first failure`() = runBlocking {
        val skill1 = TestSkill("skill-1")
        val skill2 = FailingSkill("skill-2")
        val skill3 = TestSkill("skill-3")

        registry.register(skill1, context)
        registry.register(skill2, context)
        registry.register(skill3, context)

        val composite = composer.sequential("composite", listOf("skill-1", "skill-2", "skill-3"))
        registry.register(composite, context)

        val result = registry.execute("composite", context)

        assertFalse(result.success)
        assertTrue(result.message!!.contains("skill-2"))
    }

    @Test
    fun `test create parallel composite skill`() = runBlocking {
        registry.register(TestSkill("skill-1"), context)
        registry.register(TestSkill("skill-2"), context)

        val composite = composer.parallel("composite", listOf("skill-1", "skill-2"))

        Assertions.assertNotNull(composite)
        assertEquals("composite", composite.metadata.id)
        assertEquals(2, composite.metadata.dependencies.size)
    }

    @Test
    fun `test parallel composite skill execution`() = runBlocking {
        val skill1 = CountingSkill("skill-1")
        val skill2 = CountingSkill("skill-2")
        registry.register(skill1, context)
        registry.register(skill2, context)

        val composite = composer.parallel("composite", listOf("skill-1", "skill-2"))
        registry.register(composite, context)

        val result = registry.execute("composite", context)

        assertTrue(result.success)
        assertEquals(1, skill1.executionCount)
        assertEquals(1, skill2.executionCount)

        @Suppress("UNCHECKED_CAST")
        val results = result.data as Map<String, SkillResult>
        assertEquals(2, results.size)
        assertTrue(results.values.all { it.success })
    }

    @Test
    fun `test parallel composite reports all failures`() = runBlocking {
        val skill1 = FailingSkill("skill-1")
        val skill2 = TestSkill("skill-2")
        val skill3 = FailingSkill("skill-3")

        registry.register(skill1, context)
        registry.register(skill2, context)
        registry.register(skill3, context)

        val composite = composer.parallel("composite", listOf("skill-1", "skill-2", "skill-3"))
        registry.register(composite, context)

        val result = registry.execute("composite", context)

        assertFalse(result.success)
        assertTrue(result.message!!.contains("2 failures"))

        @Suppress("UNCHECKED_CAST")
        val failures = result.metadata["failures"] as List<String>
        assertEquals(2, failures.size)
        assertTrue(failures.contains("skill-1"))
        assertTrue(failures.contains("skill-3"))
    }

    // Test helper classes

    class TestSkill(private val skillId: String) : AbstractSkill() {
        override val metadata = SkillMetadata(id = skillId, name = "Test $skillId", version = "1.0.0")
        override suspend fun execute(context: SkillContext, params: Map<String, Any>) =
            SkillResult.success(message = "Executed $skillId")
    }

    class CountingSkill(private val skillId: String) : AbstractSkill() {
        var executionCount = 0
        override val metadata = SkillMetadata(id = skillId, name = "Counting $skillId", version = "1.0.0")
        override suspend fun execute(context: SkillContext, params: Map<String, Any>): SkillResult {
            executionCount++
            return SkillResult.success(message = "Executed $skillId")
        }
    }

    class FailingSkill(private val skillId: String) : AbstractSkill() {
        override val metadata = SkillMetadata(id = skillId, name = "Failing $skillId", version = "1.0.0")
        override suspend fun execute(context: SkillContext, params: Map<String, Any>) =
            SkillResult.failure(message = "Failed $skillId")
    }
}
