package ai.platon.pulsar.agentic.skills.examples

import ai.platon.pulsar.agentic.ToolCallSpec
import ai.platon.pulsar.agentic.skills.*

/**
 * Example skill for web scraping operations.
 *
 * This skill demonstrates:
 * - Basic skill structure
 * - Metadata definition
 * - Tool call specifications
 * - Lifecycle hooks
 * - Parameter validation
 *
 * ## Usage Example:
 * ```kotlin
 * val registry = SkillRegistry.instance
 * val context = SkillContext(sessionId = "session-123")
 *
 * // Register the skill
 * val skill = WebScrapingSkill()
 * registry.register(skill, context)
 *
 * // Execute the skill
 * val result = registry.execute(
 *     skillId = "web-scraping",
 *     context = context,
 *     params = mapOf(
 *         "url" to "https://example.com",
 *         "selector" to ".content"
 *     )
 * )
 * ```
 */
class WebScrapingSkill : AbstractSkill() {
    override val metadata = SkillMetadata(
        id = "web-scraping",
        name = "Web Scraping",
        version = "1.0.0",
        description = "Extract data from web pages using CSS selectors",
        author = "Browser4",
        tags = setOf("scraping", "extraction", "web")
    )

    override val toolCallSpecs = listOf(
        ToolCallSpec(
            domain = "skill.scraping",
            method = "extract",
            arguments = listOf(
                ToolCallSpec.Arg("url", "String"),
                ToolCallSpec.Arg("selector", "String"),
                ToolCallSpec.Arg("attributes", "List<String>", "listOf(\"text\")")
            ),
            returnType = "Map<String, Any>",
            description = "Extract data from a web page using CSS selectors"
        )
    )

    override suspend fun onLoad(context: SkillContext) {
        super.onLoad(context)
        // Initialize resources, load configurations, etc.
    }

    override suspend fun execute(context: SkillContext, params: Map<String, Any>): SkillResult {
        val url = params["url"] as? String
            ?: return SkillResult.failure("Missing required parameter: url")

        val selector = params["selector"] as? String
            ?: return SkillResult.failure("Missing required parameter: selector")

        val attributes = params["attributes"] as? List<*> ?: listOf("text")

        // Simulate web scraping operation
        val extractedData = mapOf(
            "url" to url,
            "selector" to selector,
            "attributes" to attributes,
            "data" to "Simulated extracted content from $url"
        )

        return SkillResult.success(
            data = extractedData,
            message = "Successfully extracted data from $url"
        )
    }

    override suspend fun onBeforeExecute(context: SkillContext, params: Map<String, Any>): Boolean {
        // Validate URL format
        val url = params["url"] as? String ?: return false
        return url.startsWith("http://") || url.startsWith("https://")
    }

    override suspend fun onAfterExecute(context: SkillContext, params: Map<String, Any>, result: SkillResult) {
        // Log execution metrics
        if (result.success) {
            context.setResource("last_scraping_success", System.currentTimeMillis())
        }
    }

    override suspend fun validate(context: SkillContext): Boolean {
        // Validate skill configuration and environment
        return true
    }
}

/**
 * Example skill for form filling operations.
 *
 * This skill demonstrates:
 * - Skills with dependencies
 * - Complex parameter handling
 * - Inter-skill communication via shared resources
 *
 * ## Usage Example:
 * ```kotlin
 * val registry = SkillRegistry.instance
 * val context = SkillContext(sessionId = "session-123")
 *
 * // Register the skill (note: depends on "web-scraping")
 * val skill = FormFillingSkill()
 * registry.register(skill, context)
 *
 * // Execute the skill
 * val result = registry.execute(
 *     skillId = "form-filling",
 *     context = context,
 *     params = mapOf(
 *         "url" to "https://example.com/form",
 *         "formData" to mapOf(
 *             "name" to "John Doe",
 *             "email" to "john@example.com"
 *         )
 *     )
 * )
 * ```
 */
class FormFillingSkill : AbstractSkill() {
    override val metadata = SkillMetadata(
        id = "form-filling",
        name = "Form Filling",
        version = "1.0.0",
        description = "Automatically fill web forms with provided data",
        author = "Browser4",
        dependencies = listOf("web-scraping"),
        tags = setOf("forms", "automation", "input")
    )

    override val toolCallSpecs = listOf(
        ToolCallSpec(
            domain = "skill.form",
            method = "fill",
            arguments = listOf(
                ToolCallSpec.Arg("url", "String"),
                ToolCallSpec.Arg("formData", "Map<String, String>"),
                ToolCallSpec.Arg("submit", "Boolean", "false")
            ),
            returnType = "SkillResult",
            description = "Fill a web form with the provided data"
        )
    )

    override suspend fun execute(context: SkillContext, params: Map<String, Any>): SkillResult {
        val url = params["url"] as? String
            ?: return SkillResult.failure("Missing required parameter: url")

        @Suppress("UNCHECKED_CAST")
        val formData = params["formData"] as? Map<String, String>
            ?: return SkillResult.failure("Missing required parameter: formData")

        val submit = params["submit"] as? Boolean ?: false

        // Simulate form filling operation
        val filledFields = formData.keys.toList()

        val resultData = mapOf(
            "url" to url,
            "filledFields" to filledFields,
            "submitted" to submit,
            "message" to "Successfully filled ${filledFields.size} fields"
        )

        return SkillResult.success(
            data = resultData,
            message = "Form filled successfully at $url"
        )
    }

    override suspend fun onBeforeExecute(context: SkillContext, params: Map<String, Any>): Boolean {
        // Validate form data
        @Suppress("UNCHECKED_CAST")
        val formData = params["formData"] as? Map<String, String> ?: return false
        return formData.isNotEmpty()
    }

    override suspend fun validate(context: SkillContext): Boolean {
        // Check if the required dependency skill is available
        val registry = SkillRegistry.instance
        return registry.contains("web-scraping")
    }
}

/**
 * Example skill for data validation.
 *
 * This skill demonstrates:
 * - Simple validation operations
 * - Working with different data types
 *
 * ## Usage Example:
 * ```kotlin
 * val registry = SkillRegistry.instance
 * val context = SkillContext(sessionId = "session-123")
 *
 * // Register the skill
 * val skill = DataValidationSkill()
 * registry.register(skill, context)
 *
 * // Execute the skill
 * val result = registry.execute(
 *     skillId = "data-validation",
 *     context = context,
 *     params = mapOf(
 *         "data" to mapOf("email" to "test@example.com"),
 *         "rules" to listOf("email")
 *     )
 * )
 * ```
 */
class DataValidationSkill : AbstractSkill() {
    override val metadata = SkillMetadata(
        id = "data-validation",
        name = "Data Validation",
        version = "1.0.0",
        description = "Validate data against specified rules",
        author = "Browser4",
        tags = setOf("validation", "data", "quality")
    )

    override suspend fun execute(context: SkillContext, params: Map<String, Any>): SkillResult {
        @Suppress("UNCHECKED_CAST")
        val data = params["data"] as? Map<String, Any>
            ?: return SkillResult.failure("Missing required parameter: data")

        @Suppress("UNCHECKED_CAST")
        val rules = params["rules"] as? List<String>
            ?: return SkillResult.failure("Missing required parameter: rules")

        val validationResults = mutableMapOf<String, Boolean>()
        val errors = mutableListOf<String>()

        for (rule in rules) {
            when (rule) {
                "email" -> {
                    val email = data["email"] as? String
                    val isValid = email?.matches(Regex("^[A-Za-z0-9+_.-]+@(.+)$")) ?: false
                    validationResults[rule] = isValid
                    if (!isValid) errors.add("Invalid email format")
                }
                "required" -> {
                    val isValid = data.values.all { it != null && it.toString().isNotBlank() }
                    validationResults[rule] = isValid
                    if (!isValid) errors.add("Required fields are missing")
                }
                else -> {
                    validationResults[rule] = false
                    errors.add("Unknown validation rule: $rule")
                }
            }
        }

        val allValid = errors.isEmpty()

        return if (allValid) {
            SkillResult.success(
                data = validationResults,
                message = "All validation rules passed"
            )
        } else {
            SkillResult.failure(
                message = "Validation failed: ${errors.joinToString(", ")}",
                metadata = mapOf("validationResults" to validationResults, "errors" to errors)
            )
        }
    }
}
