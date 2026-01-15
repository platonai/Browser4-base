package ai.platon.pulsar.agentic.skills

import ai.platon.pulsar.common.getLogger
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Registry for managing skills.
 *
 * The SkillRegistry is responsible for:
 * - Registering and unregistering skills
 * - Looking up skills by ID
 * - Managing skill lifecycle
 * - Resolving skill dependencies
 * - Providing skill discovery
 *
 * ## Example Usage:
 * ```kotlin
 * val registry = SkillRegistry.instance
 * val skill = WebScrapingSkill()
 *
 * // Register a skill
 * registry.register(skill, context)
 *
 * // Get a skill
 * val registeredSkill = registry.get("web-scraping")
 *
 * // Execute a skill
 * val result = registry.execute("web-scraping", context, params)
 *
 * // Unregister a skill
 * registry.unregister("web-scraping", context)
 * ```
 */
class SkillRegistry private constructor() {
    private val logger = getLogger(this)
    private val skills = ConcurrentHashMap<String, Skill>()
    private val mutex = Mutex()

    companion object {
        /**
         * Singleton instance of the registry.
         */
        val instance: SkillRegistry by lazy { SkillRegistry() }
    }

    /**
     * Register a skill.
     *
     * @param skill The skill to register
     * @param context Execution context for skill initialization
     * @throws IllegalArgumentException if a skill with the same ID is already registered
     * @throws IllegalStateException if skill dependencies are not satisfied
     */
    suspend fun register(skill: Skill, context: SkillContext) {
        mutex.withLock {
            val id = skill.metadata.id

            if (skills.containsKey(id)) {
                throw IllegalArgumentException(
                    "Skill '$id' is already registered. " +
                        "Use unregister() first if you want to replace it."
                )
            }

            // Validate dependencies
            val missingDeps = skill.metadata.dependencies.filterNot { skills.containsKey(it) }
            if (missingDeps.isNotEmpty()) {
                throw IllegalStateException(
                    "Cannot register skill '$id': missing dependencies: ${missingDeps.joinToString()}"
                )
            }

            // Validate the skill
            if (!skill.validate(context)) {
                throw IllegalStateException("Skill '$id' validation failed")
            }

            // Call lifecycle hook
            skill.onLoad(context)

            skills[id] = skill
            logger.info("✓ Registered skill: {} (version {})", skill.metadata.name, skill.metadata.version)
        }
    }

    /**
     * Unregister a skill.
     *
     * @param skillId The ID of the skill to unregister
     * @param context Execution context for skill cleanup
     * @return true if the skill was unregistered, false if not found
     */
    suspend fun unregister(skillId: String, context: SkillContext): Boolean {
        mutex.withLock {
            val skill = skills.remove(skillId) ?: return false

            // Check if any other skills depend on this one
            val dependents = skills.values.filter { skillId in it.metadata.dependencies }
            if (dependents.isNotEmpty()) {
                // Re-add the skill since we can't remove it
                skills[skillId] = skill
                throw IllegalStateException(
                    "Cannot unregister skill '$skillId': " +
                        "it is required by: ${dependents.joinToString { it.metadata.id }}"
                )
            }

            // Call lifecycle hook
            skill.onUnload(context)

            logger.info("✓ Unregistered skill: {}", skill.metadata.name)
            return true
        }
    }

    /**
     * Get a skill by ID.
     *
     * @param skillId The ID of the skill to retrieve
     * @return The skill if found, null otherwise
     */
    fun get(skillId: String): Skill? {
        return skills[skillId]
    }

    /**
     * Check if a skill is registered.
     *
     * @param skillId The ID of the skill to check
     * @return true if the skill is registered, false otherwise
     */
    fun contains(skillId: String): Boolean {
        return skills.containsKey(skillId)
    }

    /**
     * Get all registered skills.
     *
     * @return List of all registered skills
     */
    fun getAll(): List<Skill> {
        return skills.values.toList()
    }

    /**
     * Get all skill IDs.
     *
     * @return List of all registered skill IDs
     */
    fun getAllIds(): List<String> {
        return skills.keys.toList()
    }

    /**
     * Find skills by tag.
     *
     * @param tag Tag to search for
     * @return List of skills with the specified tag
     */
    fun findByTag(tag: String): List<Skill> {
        return skills.values.filter { tag in it.metadata.tags }
    }

    /**
     * Find skills by author.
     *
     * @param author Author to search for
     * @return List of skills by the specified author
     */
    fun findByAuthor(author: String): List<Skill> {
        return skills.values.filter { it.metadata.author == author }
    }

    /**
     * Execute a skill by ID.
     *
     * @param skillId The ID of the skill to execute
     * @param context Execution context
     * @param params Execution parameters
     * @return Result of the skill execution
     * @throws IllegalArgumentException if the skill is not found
     */
    suspend fun execute(
        skillId: String,
        context: SkillContext,
        params: Map<String, Any> = emptyMap()
    ): SkillResult {
        val skill = skills[skillId]
            ?: throw IllegalArgumentException("Skill '$skillId' is not registered")

        // Call before hook
        if (!skill.onBeforeExecute(context, params)) {
            return SkillResult.failure("Skill execution was cancelled by onBeforeExecute hook")
        }

        // Execute the skill
        val result = try {
            skill.execute(context, params)
        } catch (e: Exception) {
            logger.warn("Error executing skill '{}': {}", skillId, e.message)
            SkillResult.failure(
                message = "Skill execution failed: ${e.message}",
                metadata = mapOf("exception" to e)
            )
        }

        // Call after hook
        skill.onAfterExecute(context, params, result)

        return result
    }

    /**
     * Clear all registered skills.
     *
     * @param context Execution context for cleanup
     */
    suspend fun clear(context: SkillContext) {
        mutex.withLock {
            // Unload all skills in reverse dependency order
            val skillIds = skills.keys.toList()
            for (skillId in skillIds) {
                try {
                    unregister(skillId, context)
                } catch (e: Exception) {
                    logger.warn("Error unregistering skill '{}': {}", skillId, e.message)
                }
            }
            skills.clear()
            logger.info("✓ Cleared all skills")
        }
    }

    /**
     * Get the count of registered skills.
     *
     * @return Number of registered skills
     */
    fun size(): Int {
        return skills.size
    }

    /**
     * Get skill metadata for all registered skills.
     *
     * @return List of skill metadata
     */
    fun getAllMetadata(): List<SkillMetadata> {
        return skills.values.map { it.metadata }
    }
}
