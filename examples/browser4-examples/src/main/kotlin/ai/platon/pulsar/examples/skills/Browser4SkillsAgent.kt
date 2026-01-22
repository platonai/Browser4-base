package ai.platon.pulsar.examples.skills

import ai.platon.pulsar.agentic.context.AgenticContexts
import ai.platon.pulsar.agentic.skills.SkillContext
import ai.platon.pulsar.agentic.skills.SkillRegistry
import ai.platon.pulsar.agentic.skills.examples.WebScrapingSkill

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
    val webScrapingSkill = WebScrapingSkill()
    registry.register(webScrapingSkill, context)

    // Use agent with a skill-oriented task
    val task = """
        Use web-scraping skill to scrape https://news.ycombinator.com/news
        """.trimIndent()

    val history = agent.run(task)
    println("Agent result: ${history.finalResult}")
}
