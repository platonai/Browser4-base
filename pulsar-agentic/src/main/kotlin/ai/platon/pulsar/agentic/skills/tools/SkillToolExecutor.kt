package ai.platon.pulsar.agentic.skills.tools

import ai.platon.pulsar.agentic.event.AgentEventBus
import ai.platon.pulsar.agentic.event.AgenticEvents
import ai.platon.pulsar.agentic.model.ToolSpec
import ai.platon.pulsar.agentic.skills.Skill
import ai.platon.pulsar.agentic.skills.SkillRegistry
import ai.platon.pulsar.agentic.tools.builtin.AbstractToolExecutor
import ai.platon.pulsar.agentic.tools.specs.ToolCallSpecificationProvider
import ai.platon.pulsar.common.serialize.json.pulsarObjectMapper
import kotlin.reflect.KClass

/**
 * Expose registered [Skill]s as agent tools.
 *
 * Domain is fixed to [domain]. Each skill is invoked via method name == skillId.
 *
 * Example tool call (after wiring the target):
 * `skill.run(id: String, params: Map<String, Any?> = emptyMap())`
 */
class SkillToolExecutor(
    private val registry: SkillRegistry = SkillRegistry.instance,
) : AbstractToolExecutor(), ToolCallSpecificationProvider {

    override val domain: String = "skill"

    override val targetClass: KClass<*> = SkillToolTarget::class

    init {
        // 1) Discovery surface
        toolSpec["list"] = ToolSpec(
            domain = domain,
            method = "list",
            arguments = listOf(
                ToolSpec.Arg("maxDescriptionChars", "Int", "512")
            ),
            returnType = "List<SkillSummary>",
            description = "List registered skills (summary only, for discovery/matching)"
        )

        // 2) Activation surface
        toolSpec["activate"] = ToolSpec(
            domain = domain,
            method = "activate",
            arguments = listOf(
                ToolSpec.Arg("id", "String")
            ),
            returnType = "SkillActivation",
            description = "Load full SKILL.md (and resource pointers) for a skill (activation)"
        )

        // 3) Stable execution entry point
        toolSpec["run"] = ToolSpec(
            domain = domain,
            method = "run",
            arguments = listOf(
                ToolSpec.Arg("id", "String"),
                ToolSpec.Arg("params", "Map<String, Any?>", "emptyMap()")
            ),
            returnType = "SkillResult",
            description = "Run a registered skill by id"
        )
    }

    override fun getToolCallSpecifications(): List<ToolSpec> {
        // Expose: (1) stable skill.* entry points; (2) optional richer specs provided by skills.
        // Skills may use domains like "skill.debug.scraping"; those should be registered separately if desired.
        return buildList {
            addAll(toolSpec.values)
            registry.getAll().flatMapTo(this) { it.toolSpec }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    override suspend fun callFunctionOn(
        domain: String,
        functionName: String,
        args: Map<String, Any?>,
        target: Any
    ): Any? {
        require(domain == this.domain) { "Unsupported domain: $domain" }
        require(target is SkillToolTarget) { "Target must be a SkillToolTarget" }
        
        // Get agentId from SkillToolTarget context if available
        val agentId = target.context.sessionId

        return when (functionName) {
            "list" -> {
                val maxChars = (args["maxDescriptionChars"] as? Number)?.toInt() ?: 512
                val skills = registry.listSkillSummaries(maxDescriptionChars = maxChars)
                
                // Emit skill listed event
                AgentEventBus.emitSkillEvent(
                    eventType = AgenticEvents.SkillEventTypes.ON_SKILLS_LISTED,
                    agentId = agentId,
                    message = "Listed ${skills.size} skills",
                    metadata = mapOf(
                        "count" to skills.size,
                        "maxDescriptionChars" to maxChars
                    )
                )
                
                skills
            }

            "activate" -> {
                validateArgs(args, allowed = setOf("id"), required = setOf("id"), functionName)
                val id = paramString(args, "id", functionName)!!
                val activation = registry.activateSkill(id)
                
                // Emit skill activated event
                AgentEventBus.emitSkillEvent(
                    eventType = AgenticEvents.SkillEventTypes.ON_SKILL_ACTIVATED,
                    agentId = agentId,
                    message = "Skill activated: $id",
                    metadata = mapOf("skillId" to id)
                )
                
                activation
            }

            "run" -> {
                validateArgs(args, allowed = setOf("id", "params"), required = setOf("id"), functionName)
                val id = paramString(args, "id", functionName)!!
                val params = args["params"]

                val paramsMap: Map<String, Any?> = when (params) {
                    null -> emptyMap()
                    is Map<*, *> -> params.entries.associate { (k, v) -> k.toString() to v }
                    is String -> {
                        val trimmed = params.trim()
                        if (trimmed.isEmpty()) emptyMap()
                        else {
                            @Suppress("UNCHECKED_CAST")
                            pulsarObjectMapper().readValue(trimmed, Map::class.java) as Map<String, Any?>
                        }
                    }
                    else -> throw IllegalArgumentException("params must be Map<String, Any?> or JSON string for $functionName | actual='${params::class.qualifiedName}'")
                }

                // Emit skill will run event
                AgentEventBus.emitSkillEvent(
                    eventType = AgenticEvents.SkillEventTypes.ON_WILL_RUN_SKILL,
                    agentId = agentId,
                    message = "Running skill: $id",
                    metadata = mapOf(
                        "skillId" to id,
                        "paramsKeys" to paramsMap.keys.toList()
                    )
                )

                val startTime = System.currentTimeMillis()

                return try {
                    val result = target.execute(id, paramsMap)
                    val duration = System.currentTimeMillis() - startTime

                    // Emit skill did run event
                    AgentEventBus.emitSkillEvent(
                        eventType = AgenticEvents.SkillEventTypes.ON_DID_RUN_SKILL,
                        agentId = agentId,
                        message = "Skill completed: $id",
                        metadata = mapOf(
                            "skillId" to id,
                            "duration" to duration,
                            "success" to (result != null)
                        )
                    )

                    result
                } catch (e: Exception) {
                    val duration = System.currentTimeMillis() - startTime

                    // Emit skill error event
                    AgentEventBus.emitSkillEvent(
                        eventType = AgenticEvents.SkillEventTypes.ON_SKILL_ERROR,
                        agentId = agentId,
                        message = "Skill failed: $id - ${e.message}",
                        metadata = mapOf(
                            "skillId" to id,
                            "duration" to duration,
                            "error" to e.message
                        )
                    )

                    throw e
                }
            }

            else -> throw IllegalArgumentException("Unsupported $domain method: $functionName(${args.keys})")
        }
    }
}
