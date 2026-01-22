package ai.platon.pulsar.examples.agent

import ai.platon.pulsar.agentic.context.AgenticContexts
import ai.platon.pulsar.agentic.skills.SkillContext
import ai.platon.pulsar.agentic.skills.SkillRegistry
import ai.platon.pulsar.agentic.skills.examples.WebScrapingSkill
import ai.platon.pulsar.agentic.skills.examples.FormFillingSkill
import ai.platon.pulsar.agentic.skills.examples.DataValidationSkill

/**
 * Example demonstrating the Skills system with Browser4 agent.
 * 
 * This example shows how to:
 * 1. Register skills to a SkillRegistry
 * 2. Execute skills programmatically
 * 3. Use skills with an agent
 * 4. Discover skills by tags
 */
suspend fun main() {
    // Create the agent
    val agent = AgenticContexts.getOrCreateAgent()
    
    // Get or create the skill registry
    val registry = SkillRegistry.instance
    val context = SkillContext(sessionId = "browser4-skills-demo")
    
    // Register example skills
    println("=== Registering Skills ===")
    val webScrapingSkill = WebScrapingSkill()
    val formFillingSkill = FormFillingSkill()
    val dataValidationSkill = DataValidationSkill()
    
    registry.register(webScrapingSkill, context)
    registry.register(formFillingSkill, context)
    registry.register(dataValidationSkill, context)
    
    println("Registered ${registry.size()} skills: ${registry.getAllIds().joinToString(", ")}")
    println()
    
    // Example 1: Execute a skill directly
    println("=== Example 1: Direct Skill Execution ===")
    val scrapingResult = registry.execute(
        skillId = "web-scraping",
        context = context,
        params = mapOf(
            "url" to "https://news.ycombinator.com/news",
            "selector" to ".athing",
            "attributes" to listOf("text", "href")
        )
    )
    println("Web scraping result: ${scrapingResult.message}")
    println("Success: ${scrapingResult.success}")
    println()
    
    // Example 2: Execute form filling skill
    println("=== Example 2: Form Filling Skill ===")
    val formResult = registry.execute(
        skillId = "form-filling",
        context = context,
        params = mapOf(
            "url" to "https://example.com/contact",
            "formData" to mapOf(
                "name" to "John Doe",
                "email" to "john@example.com",
                "message" to "Hello from Browser4!"
            ),
            "submit" to false
        )
    )
    println("Form filling result: ${formResult.message}")
    println()
    
    // Example 3: Execute data validation skill
    println("=== Example 3: Data Validation Skill ===")
    val validationResult = registry.execute(
        skillId = "data-validation",
        context = context,
        params = mapOf(
            "data" to mapOf(
                "email" to "john@example.com",
                "name" to "John Doe"
            ),
            "rules" to listOf("email", "required")
        )
    )
    println("Validation result: ${validationResult.message}")
    println()
    
    // Example 4: Use agent with a skill-oriented task
    println("=== Example 4: Agent Task with Skills ===")
    val task = """
        1. Navigate to https://news.ycombinator.com/news
        2. Extract the titles and links of the top 5 articles
        3. Summarize the findings
        """.trimIndent()
    
    val history = agent.run(task)
    println("Agent result: ${history.finalResult}")
    println()
    
    // Example 5: Discover skills by tag
    println("=== Example 5: Skill Discovery ===")
    val scrapingSkills = registry.findByTag("scraping")
    println("Found ${scrapingSkills.size} scraping skills:")
    scrapingSkills.forEach { skill ->
        println("  - ${skill.metadata.name} (${skill.metadata.id})")
    }
    println()
    
    // Clean up
    println("=== Cleanup ===")
    registry.clear(context)
    println("Cleaned up all skills")
}
