package ai.platon.pulsar.agentic.skills

import ai.platon.pulsar.agentic.skills.tools.SkillToolExecutor
import ai.platon.pulsar.agentic.skills.tools.SkillToolTarget
import ai.platon.pulsar.common.getLogger
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import java.nio.file.Files
import java.nio.file.Path

/**
 * End-to-end test for skill installation.
 *
 * This test validates the complete skill installation workflow by:
 * 1. Creating a self-improving-agent skill definition with SKILL.md
 * 2. Installing it via [SkillInstaller]
 * 3. Verifying the skill is registered in [SkillRegistry]
 * 4. Verifying the skill can be listed, activated, and its documentation read
 * 5. Uninstalling the skill and verifying cleanup
 *
 * **Pass criteria: self-improving-agent is successfully installed.**
 *
 * ## Running These Tests
 *
 * ```bash
 * ./mvnw test -P all-modules -pl :pulsar-e2e-tests -am -Dtest=SkillInstallE2ETest
 * ```
 */
@Tag("E2ETest")
@Tag("skills")
class SkillInstallE2ETest {

    private val logger = getLogger(this)

    private lateinit var registry: SkillRegistry
    private lateinit var context: SkillContext
    private lateinit var installer: SkillInstaller
    private lateinit var tempDir: Path

    @BeforeEach
    fun setup() = runBlocking {
        registry = SkillRegistry.instance
        context = SkillContext(sessionId = "test-skill-install-e2e")
        registry.clear(context)
        installer = SkillInstaller(registry)
        tempDir = Files.createTempDirectory("skill-install-e2e-test")
    }

    @AfterEach
    fun cleanup() = runBlocking {
        registry.clear(context)
        if (Files.exists(tempDir)) {
            Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .forEach { Files.deleteIfExists(it) }
        }
    }

    /**
     * Test that the self-improving-agent skill can be successfully installed.
     *
     * This is the primary test case. It validates the full installation pipeline:
     * 1. Create the skill directory with SKILL.md
     * 2. Install via SkillInstaller
     * 3. Verify the skill is registered and accessible
     */
    @Test
    @DisplayName("test self-improving-agent is successfully installed")
    fun testSelfImprovingAgentIsSuccessfullyInstalled() = runBlocking {
        val skillDir = createSelfImprovingAgentSkillDirectory()

        val result = installer.install(skillDir, context)

        assertTrue(result.success, "Installation should succeed: ${result.message}")
        assertEquals("self-improving-agent", result.skillId)
        assertTrue(registry.contains("self-improving-agent"),
            "self-improving-agent should be registered in SkillRegistry")
        assertNotNull(result.deployedPath, "Deployed path should be set")

        logger.info("✓ self-improving-agent installed successfully to {}", result.deployedPath)
    }

    /**
     * Test that the installed self-improving-agent skill metadata is correct.
     */
    @Test
    @DisplayName("test self-improving-agent metadata after installation")
    fun testSelfImprovingAgentMetadataAfterInstallation() = runBlocking {
        val skillDir = createSelfImprovingAgentSkillDirectory()
        val result = installer.install(skillDir, context)
        assertTrue(result.success, "Installation should succeed: ${result.message}")

        val skill = registry.get("self-improving-agent")
        assertNotNull(skill, "Skill should be retrievable from registry")
        assertEquals("Self Improving Agent", skill!!.metadata.name)
        assertEquals("1.0.0", skill.metadata.version)
        assertEquals("Browser4", skill.metadata.author)
        assertTrue(skill.metadata.tags.contains("agent"))
        assertTrue(skill.metadata.tags.contains("self-improving"))
    }

    /**
     * Test the full lifecycle: install, list, read docs, uninstall.
     */
    @Test
    @DisplayName("test self-improving-agent full lifecycle")
    fun testSelfImprovingAgentFullLifecycle() = runBlocking {
        val skillDir = createSelfImprovingAgentSkillDirectory()

        // Install
        val installResult = installer.install(skillDir, context)
        assertTrue(installResult.success, "Install should succeed: ${installResult.message}")
        assertTrue(registry.contains("self-improving-agent"))

        // List installed skills
        val installed = installer.listInstalled()
        assertTrue(installed.any { it["id"] == "self-improving-agent" },
            "self-improving-agent should appear in installed skills list")

        // Read documentation
        val doc = installer.readDocumentation("self-improving-agent")
        assertNotNull(doc, "Documentation should be readable")
        assertTrue(doc!!.contains("Self Improving Agent"))

        // Uninstall
        val uninstallResult = installer.uninstall("self-improving-agent", context)
        assertTrue(uninstallResult.success, "Uninstall should succeed: ${uninstallResult.message}")
        assertFalse(registry.contains("self-improving-agent"),
            "self-improving-agent should be removed from registry after uninstall")

        logger.info("✓ self-improving-agent full lifecycle completed successfully")
    }

    /**
     * Test installation via SkillToolExecutor (the tool call path agents use).
     */
    @Test
    @DisplayName("test self-improving-agent install via tool executor")
    fun testSelfImprovingAgentInstallViaToolExecutor() = runBlocking {
        val skillDir = createSelfImprovingAgentSkillDirectory()

        val executor = SkillToolExecutor(registry)
        val target = SkillToolTarget(context, registry)

        val result = executor.callFunctionOn(
            domain = "skill",
            functionName = "install",
            args = mapOf("sourceDir" to skillDir.toString(), "overwrite" to false),
            receiver = target
        )

        assertTrue(result is SkillInstaller.InstallResult)
        val installResult = result as SkillInstaller.InstallResult
        assertTrue(installResult.success,
            "Tool executor install should succeed: ${installResult.message}")
        assertEquals("self-improving-agent", installResult.skillId)
        assertTrue(registry.contains("self-improving-agent"),
            "self-improving-agent should be registered after tool executor install")

        logger.info("✓ self-improving-agent installed via tool executor")
    }

    /**
     * Test that installation steps are correctly parsed from the SKILL.md.
     */
    @Test
    @DisplayName("test self-improving-agent install steps are parsed")
    fun testSelfImprovingAgentInstallStepsAreParsed() = runBlocking {
        val skillDir = createSelfImprovingAgentSkillDirectory()

        val result = installer.install(skillDir, context)

        assertTrue(result.success, "Installation should succeed: ${result.message}")
        assertTrue(result.installSteps.isNotEmpty(), "Install steps should be parsed from SKILL.md")
        assertEquals(3, result.installSteps.size,
            "Should parse all 3 installation steps from SKILL.md")
    }

    /**
     * Create the self-improving-agent skill directory with a valid SKILL.md.
     */
    private fun createSelfImprovingAgentSkillDirectory(): Path {
        val skillDir = tempDir.resolve("self-improving-agent")
        Files.createDirectories(skillDir)

        val skillMd = skillDir.resolve("SKILL.md")
        Files.writeString(skillMd, """
---
name: self-improving-agent
description: An autonomous agent that iteratively improves its own performance by analyzing execution results, identifying failure patterns, and refining its strategies.
metadata:
  displayName: Self Improving Agent
  version: "1.0.0"
  author: Browser4
  tags: "agent, self-improving, autonomous, iteration"
  dependencies: ""
---

# Self Improving Agent Skill

## Description

The Self Improving Agent skill enables an AI agent to iteratively improve its own performance. It analyzes execution results from previous runs, identifies failure patterns and inefficiencies, and refines its strategies for subsequent tasks.

Key capabilities:
- Analyze execution history for failure patterns
- Generate improved action strategies based on past results
- Track performance metrics across iterations
- Adapt to new page structures and content patterns

## Installation

1. Copy skill files to the managed skills directory
2. Register the skill in the SkillRegistry
3. Verify the skill is available by listing installed skills

## Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| maxIterations | Int | No | 5 | Maximum number of improvement iterations |
| targetMetric | String | No | "success_rate" | The metric to optimize for |
| historyDepth | Int | No | 10 | Number of past executions to analyze |

## Return Value

Returns a `SkillResult` with improvement analysis and updated strategies.

## Usage Examples

### Basic Self-Improvement

```kotlin
val result = registry.execute(
    skillId = "self-improving-agent",
    context = context,
    params = mapOf(
        "maxIterations" to 3,
        "targetMetric" to "success_rate"
    )
)
```
        """.trimIndent())

        Files.createDirectories(skillDir.resolve("scripts"))
        Files.createDirectories(skillDir.resolve("references"))
        Files.createDirectories(skillDir.resolve("assets"))

        return skillDir
    }
}
