package ai.platon.pulsar.agentic.skills.tools

import ai.platon.pulsar.agentic.ToolSpec
import ai.platon.pulsar.agentic.skills.Skill
import ai.platon.pulsar.agentic.skills.SkillRegistry
import ai.platon.pulsar.agentic.tools.ToolCallSpecificationProvider
import ai.platon.pulsar.agentic.tools.executors.AbstractToolExecutor
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
        // A stable entry point for the LLM.
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
        // Expose: (1) the stable skill.run entry point; (2) optional richer specs provided by skills.
        // Skills may use domains like "skill.scraping"; those should be registered separately if desired.
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

        return when (functionName) {
            "run" -> {
                validateArgs(args, allowed = setOf("id", "params"), required = setOf("id"), functionName)
                val id = paramString(args, "id", functionName)!!
                val params = args["params"]

                val paramsMap: Map<String, Any?> = when (params) {
                    null -> emptyMap()
                    is Map<*, *> -> params.entries.associate { (k, v) -> k.toString() to v }
                    else -> throw IllegalArgumentException("params must be Map<String, Any?> for $functionName | actual='${params::class.qualifiedName}'")
                }

                target.execute(id, paramsMap)
            }

            else -> throw IllegalArgumentException("Unsupported $domain method: $functionName(${args.keys})")
        }
    }
}

