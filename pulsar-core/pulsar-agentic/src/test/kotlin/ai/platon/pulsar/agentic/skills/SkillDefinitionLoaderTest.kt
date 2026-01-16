package ai.platon.pulsar.agentic.skills

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

/**
 * Tests for SkillDefinitionLoader.
 */
class SkillDefinitionLoaderTest {

    @Test
    fun `should load skill definitions from resources`() {
        val loader = SkillDefinitionLoader()
        
        val definitions = loader.loadFromResources("skills")
        
        assertNotNull(definitions)
        assertTrue(definitions.isNotEmpty(), "Should load at least one skill definition")
        
        // Verify expected skills are loaded
        val skillIds = definitions.map { it.skillId }.toSet()
        assertTrue(skillIds.contains("web-scraping"), "Should load web-scraping skill")
        assertTrue(skillIds.contains("form-filling"), "Should load form-filling skill")
        assertTrue(skillIds.contains("data-validation"), "Should load data-validation skill")
    }

    @Test
    fun `should parse web scraping skill metadata correctly`() {
        val loader = SkillDefinitionLoader()
        
        val definitions = loader.loadFromResources("skills")
        val webScrapingSkill = definitions.find { it.skillId == "web-scraping" }
        
        assertNotNull(webScrapingSkill, "Web scraping skill should be found")
        webScrapingSkill?.let {
            assertEquals("web-scraping", it.skillId)
            assertEquals("Web Scraping", it.name)
            assertEquals("1.0.0", it.version)
            assertEquals("Browser4", it.author)
            assertTrue(it.tags.contains("scraping"))
            assertTrue(it.tags.contains("extraction"))
            assertTrue(it.tags.contains("web"))
            assertTrue(it.description.isNotBlank())
            assertTrue(it.dependencies.isEmpty())
        }
    }

    @Test
    fun `should parse form filling skill with dependencies`() {
        val loader = SkillDefinitionLoader()
        
        val definitions = loader.loadFromResources("skills")
        val formFillingSkill = definitions.find { it.skillId == "form-filling" }
        
        assertNotNull(formFillingSkill, "Form filling skill should be found")
        formFillingSkill?.let {
            assertEquals("form-filling", it.skillId)
            assertEquals("Form Filling", it.name)
            assertTrue(it.dependencies.contains("web-scraping"), "Should have web-scraping dependency")
        }
    }

    @Test
    fun `should parse skill parameters correctly`() {
        val loader = SkillDefinitionLoader()
        
        val definitions = loader.loadFromResources("skills")
        val webScrapingSkill = definitions.find { it.skillId == "web-scraping" }
        
        assertNotNull(webScrapingSkill)
        webScrapingSkill?.let {
            assertTrue(it.parameters.isNotEmpty(), "Should have parameters defined")
            
            val urlParam = it.parameters["url"]
            assertNotNull(urlParam, "Should have url parameter")
            urlParam?.let { param ->
                assertEquals("String", param.type)
                assertTrue(param.required)
                assertEquals("-", param.defaultValue)
            }
            
            val selectorParam = it.parameters["selector"]
            assertNotNull(selectorParam, "Should have selector parameter")
            selectorParam?.let { param ->
                assertEquals("String", param.type)
                assertTrue(param.required)
            }
            
            val attributesParam = it.parameters["attributes"]
            assertNotNull(attributesParam, "Should have attributes parameter")
            attributesParam?.let { param ->
                assertEquals("List<String>", param.type)
                assertFalse(param.required)
            }
        }
    }

    @Test
    fun `should detect optional directories`() {
        val loader = SkillDefinitionLoader()
        
        val definitions = loader.loadFromResources("skills")
        val webScrapingSkill = definitions.find { it.skillId == "web-scraping" }
        
        assertNotNull(webScrapingSkill)
        webScrapingSkill?.let {
            assertNotNull(it.scriptsPath, "Should have scripts directory")
            assertNotNull(it.referencesPath, "Should have references directory")
            assertNotNull(it.assetsPath, "Should have assets directory")
        }
    }

    @Test
    fun `should get skill scripts`() {
        val loader = SkillDefinitionLoader()
        
        val definitions = loader.loadFromResources("skills")
        val webScrapingSkill = definitions.find { it.skillId == "web-scraping" }
        
        assertNotNull(webScrapingSkill)
        webScrapingSkill?.let {
            val scripts = loader.getSkillScripts(it)
            assertTrue(scripts.isNotEmpty(), "Should have at least one script")
            assertTrue(scripts.any { it.fileName.toString().contains("example-usage") })
        }
    }

    @Test
    fun `should get skill references`() {
        val loader = SkillDefinitionLoader()
        
        val definitions = loader.loadFromResources("skills")
        val webScrapingSkill = definitions.find { it.skillId == "web-scraping" }
        
        assertNotNull(webScrapingSkill)
        webScrapingSkill?.let {
            val references = loader.getSkillReferences(it)
            assertTrue(references.isNotEmpty(), "Should have at least one reference document")
            assertTrue(references.any { it.fileName.toString().contains("developer-guide") })
        }
    }

    @Test
    fun `should get skill assets`() {
        val loader = SkillDefinitionLoader()
        
        val definitions = loader.loadFromResources("skills")
        val webScrapingSkill = definitions.find { it.skillId == "web-scraping" }
        
        assertNotNull(webScrapingSkill)
        webScrapingSkill?.let {
            val assets = loader.getSkillAssets(it)
            assertTrue(assets.isNotEmpty(), "Should have at least one asset")
            assertTrue(assets.any { it.fileName.toString().contains("config.json") })
        }
    }

    @Test
    fun `should load from custom directory`(@TempDir tempDir: Path) {
        // Create a test skill directory
        val skillDir = tempDir.resolve("test-skill")
        Files.createDirectories(skillDir)
        
        val skillMd = """
            # Test Skill
            
            ## Metadata
            
            - **Skill ID**: `test-skill`
            - **Name**: Test Skill
            - **Version**: 2.0.0
            - **Author**: Test Author
            - **Tags**: `test`, `example`
            
            ## Description
            
            This is a test skill for unit testing.
            
            ## Dependencies
            
            - `web-scraping`
            - `data-validation`
            
            ## Parameters
            
            | Parameter | Type | Required | Default | Description |
            |-----------|------|----------|---------|-------------|
            | testParam | String | Yes | - | Test parameter |
            | optionalParam | Boolean | No | false | Optional parameter |
            
            ## Usage Examples
            
            ```kotlin
            val result = execute(context, params)
            ```
        """.trimIndent()
        
        Files.writeString(skillDir.resolve("SKILL.md"), skillMd)
        
        // Create optional directories
        Files.createDirectories(skillDir.resolve("scripts"))
        Files.createDirectories(skillDir.resolve("references"))
        Files.createDirectories(skillDir.resolve("assets"))
        
        // Load from custom directory
        val loader = SkillDefinitionLoader()
        val definitions = loader.loadFromDirectory(tempDir)
        
        assertEquals(1, definitions.size, "Should load exactly one skill")
        
        val testSkill = definitions[0]
        assertEquals("test-skill", testSkill.skillId)
        assertEquals("Test Skill", testSkill.name)
        assertEquals("2.0.0", testSkill.version)
        assertEquals("Test Author", testSkill.author)
        assertTrue(testSkill.tags.contains("test"))
        assertTrue(testSkill.tags.contains("example"))
        assertEquals(2, testSkill.dependencies.size)
        assertTrue(testSkill.dependencies.contains("web-scraping"))
        assertTrue(testSkill.dependencies.contains("data-validation"))
        assertEquals(2, testSkill.parameters.size)
        assertTrue(testSkill.parameters.containsKey("testParam"))
        assertTrue(testSkill.parameters.containsKey("optionalParam"))
    }

    @Test
    fun `should handle missing SKILL_md file`(@TempDir tempDir: Path) {
        val skillDir = tempDir.resolve("invalid-skill")
        Files.createDirectories(skillDir)
        // Don't create SKILL.md
        
        val loader = SkillDefinitionLoader()
        val definitions = loader.loadFromDirectory(tempDir)
        
        assertTrue(definitions.isEmpty(), "Should not load skills without SKILL.md")
    }

    @Test
    fun `should handle empty skills directory`(@TempDir tempDir: Path) {
        val loader = SkillDefinitionLoader()
        val definitions = loader.loadFromDirectory(tempDir)
        
        assertTrue(definitions.isEmpty(), "Should return empty list for empty directory")
    }

    @Test
    fun `should parse tags correctly`() {
        val loader = SkillDefinitionLoader()
        
        val definitions = loader.loadFromResources("skills")
        
        definitions.forEach { definition ->
            assertNotNull(definition.tags)
            assertTrue(definition.tags.isNotEmpty(), "Skill ${definition.skillId} should have tags")
            
            // Tags should not contain backticks or quotes
            definition.tags.forEach { tag ->
                assertFalse(tag.contains("`"), "Tag should not contain backticks: $tag")
                assertFalse(tag.contains("\""), "Tag should not contain quotes: $tag")
            }
        }
    }

    @Test
    fun `should parse description correctly`() {
        val loader = SkillDefinitionLoader()
        
        val definitions = loader.loadFromResources("skills")
        
        definitions.forEach { definition ->
            assertNotNull(definition.description)
            assertTrue(definition.description.isNotBlank(), "Skill ${definition.skillId} should have description")
            assertFalse(definition.description.startsWith("#"), "Description should not start with #")
        }
    }

    @Test
    fun `should handle skills with no dependencies`() {
        val loader = SkillDefinitionLoader()
        
        val definitions = loader.loadFromResources("skills")
        val webScrapingSkill = definitions.find { it.skillId == "web-scraping" }
        
        assertNotNull(webScrapingSkill)
        webScrapingSkill?.let {
            assertTrue(it.dependencies.isEmpty(), "Web scraping skill should have no dependencies")
        }
    }

    @Test
    fun `should parse version in semver format`() {
        val loader = SkillDefinitionLoader()
        
        val definitions = loader.loadFromResources("skills")
        
        definitions.forEach { definition ->
            assertTrue(definition.version.matches(Regex("""\d+\.\d+\.\d+""")), 
                "Version should be in semver format: ${definition.version}")
        }
    }
}
