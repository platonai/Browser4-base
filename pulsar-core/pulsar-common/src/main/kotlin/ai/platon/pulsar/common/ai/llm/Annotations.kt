package ai.platon.pulsar.common.ai.llm

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class MCP(
    val description: String = "",
    val expression: String = "",
    val domain: String = "default"
)
