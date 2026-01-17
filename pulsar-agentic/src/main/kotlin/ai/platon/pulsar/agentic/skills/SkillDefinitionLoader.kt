package ai.platon.pulsar.agentic.skills

import ai.platon.pulsar.common.getLogger
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Metadata parsed from a SKILL.md file.
 *
 * @property skillId Unique identifier for the skill
 * @property name Human-readable name
 * @property version Semantic version
 * @property author Skill author
 * @property tags Set of categorization tags
 * @property description Full description of the skill
 * @property dependencies List of skill IDs this skill depends on
 * @property parameters Map of parameter names to their descriptions
 * @property examples List of usage examples
 */
data class SkillDefinition(
    val skillId: String,
    val name: String,
    val version: String,
    val author: String,
    val tags: Set<String>,
    val description: String,
    val dependencies: List<String>,
    val parameters: Map<String, ParameterInfo>,
    val examples: List<String>,
    val scriptsPath: Path? = null,
    val referencesPath: Path? = null,
    val assetsPath: Path? = null
) {
    /**
     * Information about a skill parameter.
     */
    data class ParameterInfo(
        val name: String,
        val type: String,
        val required: Boolean,
        val defaultValue: String?,
        val description: String
    )
}

/**
 * Loader for skill definitions from directory structure.
 *
 * The SkillDefinitionLoader reads skill metadata from SKILL.md files
 * and provides access to associated resources (scripts, references, assets).
 *
 * Expected directory structure:
 * ```
 * skill-name/
 * ├── SKILL.md          # Required: Skill metadata and documentation
 * ├── scripts/          # Optional: Executable scripts
 * ├── references/       # Optional: Developer documentation
 * └── assets/           # Optional: Configuration and templates
 * ```
 *
 * ## Usage Example:
 * ```kotlin
 * val loader = SkillDefinitionLoader()
 *
 * // Load from resources
 * val definitions = loader.loadFromResources("skills")
 *
 * // Load from filesystem
 * val definitions = loader.loadFromDirectory(Paths.get("/path/to/skills"))
 *
 * // Access skill metadata
 * definitions.forEach { definition ->
 *     println("Skill: ${definition.name} v${definition.version}")
 *     println("Description: ${definition.description}")
 * }
 * ```
 */
class SkillDefinitionLoader {
    private val logger = getLogger(this)

    /**
     * Load skill definitions from a directory.
     *
     * @param skillsDirectory Path to directory containing skill subdirectories
     * @return List of skill definitions found
     */
    fun loadFromDirectory(skillsDirectory: Path): List<SkillDefinition> {
        if (!Files.exists(skillsDirectory) || !Files.isDirectory(skillsDirectory)) {
            logger.warn("Skills directory not found: $skillsDirectory")
            return emptyList()
        }

        val definitions = mutableListOf<SkillDefinition>()

        Files.list(skillsDirectory).use { paths ->
            paths.filter { Files.isDirectory(it) }.forEach { skillDir ->
                try {
                    val definition = loadSkillDefinition(skillDir)
                    if (definition != null) {
                        definitions.add(definition)
                        logger.info("✓ Loaded skill definition: ${definition.skillId}")
                    }
                } catch (e: Exception) {
                    logger.warn("Failed to load skill from ${skillDir.fileName}: ${e.message}")
                }
            }
        }

        return definitions
    }

    /**
     * Load skill definitions from classpath resources.
     *
     * @param resourcePath Resource path (e.g., "skills")
     * @return List of skill definitions found
     */
    fun loadFromResources(resourcePath: String): List<SkillDefinition> {
        val resourceUrl = javaClass.classLoader.getResource(resourcePath)
        if (resourceUrl == null) {
            logger.warn("Resource path not found: $resourcePath")
            return emptyList()
        }

        val skillsPath = Paths.get(resourceUrl.toURI())
        return loadFromDirectory(skillsPath)
    }

    /**
     * Load a single skill definition from a directory.
     *
     * @param skillDirectory Path to skill directory
     * @return Skill definition or null if SKILL.md not found
     */
    private fun loadSkillDefinition(skillDirectory: Path): SkillDefinition? {
        val skillMdPath = skillDirectory.resolve("SKILL.md")
        if (!Files.exists(skillMdPath)) {
            logger.warn("SKILL.md not found in ${skillDirectory.fileName}")
            return null
        }

        val content = Files.readString(skillMdPath)
        val metadata = parseSkillMetadata(content)

        // Check for optional directories
        val scriptsPath = skillDirectory.resolve("scripts")
        val referencesPath = skillDirectory.resolve("references")
        val assetsPath = skillDirectory.resolve("assets")

        return metadata.copy(
            scriptsPath = if (Files.exists(scriptsPath)) scriptsPath else null,
            referencesPath = if (Files.exists(referencesPath)) referencesPath else null,
            assetsPath = if (Files.exists(assetsPath)) assetsPath else null
        )
    }

    /**
     * Parse skill metadata from SKILL.md content.
     *
     * @param content Content of SKILL.md file
     * @return Parsed skill definition
     */
    private fun parseSkillMetadata(content: String): SkillDefinition {
        val lines = content.lines()

        // Extract metadata section
        var skillId = ""
        var name = ""
        var version = "1.0.0"
        var author = ""
        val tags = mutableSetOf<String>()
        var description = ""
        val dependencies = mutableListOf<String>()
        val parameters = mutableMapOf<String, SkillDefinition.ParameterInfo>()
        val examples = mutableListOf<String>()

        var inMetadataSection = false
        var inDescriptionSection = false
        var inDependenciesSection = false
        var inParametersSection = false
        var inExamplesSection = false

        var currentExample = StringBuilder()

        for (line in lines) {
            when {
                line.trim().startsWith("## Metadata") -> {
                    inMetadataSection = true
                    inDescriptionSection = false
                    inDependenciesSection = false
                    inParametersSection = false
                    inExamplesSection = false
                }
                line.trim().startsWith("## Description") -> {
                    inMetadataSection = false
                    inDescriptionSection = true
                    inDependenciesSection = false
                    inParametersSection = false
                    inExamplesSection = false
                }
                line.trim().startsWith("## Dependencies") -> {
                    inMetadataSection = false
                    inDescriptionSection = false
                    inDependenciesSection = true
                    inParametersSection = false
                    inExamplesSection = false
                }
                line.trim().startsWith("## Parameters") -> {
                    inMetadataSection = false
                    inDescriptionSection = false
                    inDependenciesSection = false
                    inParametersSection = true
                    inExamplesSection = false
                }
                line.trim().startsWith("## Usage Examples") || 
                line.trim().startsWith("## Examples") -> {
                    inMetadataSection = false
                    inDescriptionSection = false
                    inDependenciesSection = false
                    inParametersSection = false
                    inExamplesSection = true
                }
                line.trim().startsWith("##") -> {
                    // End of current section
                    if (inExamplesSection && currentExample.isNotEmpty()) {
                        examples.add(currentExample.toString().trim())
                        currentExample = StringBuilder()
                    }
                    inMetadataSection = false
                    inDescriptionSection = false
                    inDependenciesSection = false
                    inParametersSection = false
                    inExamplesSection = false
                }
                inMetadataSection -> {
                    when {
                        line.contains("**Skill ID**:") -> {
                            skillId = extractValue(line)
                        }
                        line.contains("**Name**:") -> {
                            name = extractValue(line)
                        }
                        line.contains("**Version**:") -> {
                            version = extractValue(line)
                        }
                        line.contains("**Author**:") -> {
                            author = extractValue(line)
                        }
                        line.contains("**Tags**:") -> {
                            val tagsStr = extractValue(line)
                            tags.addAll(tagsStr.split(",").map { it.trim().removePrefix("`").removeSuffix("`") })
                        }
                    }
                }
                inDescriptionSection -> {
                    if (line.isNotBlank() && !line.trim().startsWith("#")) {
                        description += line + "\n"
                    }
                }
                inDependenciesSection -> {
                    if (line.trim().startsWith("-") || line.trim().startsWith("*")) {
                        val depLine = line.trim().removePrefix("-").removePrefix("*").trim()
                        // Extract skill ID from backticks (e.g., "`web-scraping` - description" -> "web-scraping")
                        val dep = if (depLine.contains("`")) {
                            depLine.substringAfter("`").substringBefore("`")
                        } else {
                            depLine
                        }
                        if (dep.isNotBlank() && dep.lowercase() != "none") {
                            dependencies.add(dep)
                        }
                    }
                }
                inParametersSection -> {
                    // Parse parameter table rows
                    if (line.trim().startsWith("|") && 
                        !line.contains("Parameter") && 
                        !line.contains("---")) {
                        val parts = line.split("|").map { it.trim() }.filter { it.isNotEmpty() }
                        if (parts.size >= 4) {
                            val paramName = parts[0]
                            val paramType = parts[1]
                            val required = parts[2].lowercase() == "yes"
                            val defaultValue = parts[3]
                            val paramDesc = if (parts.size > 4) parts[4] else ""
                            
                            parameters[paramName] = SkillDefinition.ParameterInfo(
                                name = paramName,
                                type = paramType,
                                required = required,
                                defaultValue = defaultValue,
                                description = paramDesc
                            )
                        }
                    }
                }
                inExamplesSection -> {
                    if (line.trim().startsWith("```")) {
                        if (currentExample.isNotEmpty()) {
                            examples.add(currentExample.toString().trim())
                            currentExample = StringBuilder()
                        }
                    } else if (line.isNotBlank()) {
                        currentExample.append(line).append("\n")
                    }
                }
            }
        }

        // Add last example if exists
        if (currentExample.isNotEmpty()) {
            examples.add(currentExample.toString().trim())
        }

        // Validate required fields
        require(skillId.isNotBlank()) { "Skill ID is required in SKILL.md" }
        require(name.isNotBlank()) { "Skill name is required in SKILL.md" }

        return SkillDefinition(
            skillId = skillId,
            name = name,
            version = version,
            author = author,
            tags = tags,
            description = description.trim(),
            dependencies = dependencies,
            parameters = parameters,
            examples = examples
        )
    }

    /**
     * Extract value from a metadata line.
     */
    private fun extractValue(line: String): String {
        return line.substringAfter(":")
            .trim()
            .removePrefix("`")
            .removeSuffix("`")
    }

    /**
     * Get list of scripts in a skill's scripts directory.
     *
     * @param definition Skill definition
     * @return List of script file paths
     */
    fun getSkillScripts(definition: SkillDefinition): List<Path> {
        val scriptsPath = definition.scriptsPath ?: return emptyList()
        if (!Files.exists(scriptsPath)) return emptyList()

        return Files.list(scriptsPath)
            .filter { Files.isRegularFile(it) }
            .toList()
    }

    /**
     * Get list of reference documents in a skill's references directory.
     *
     * @param definition Skill definition
     * @return List of reference file paths
     */
    fun getSkillReferences(definition: SkillDefinition): List<Path> {
        val referencesPath = definition.referencesPath ?: return emptyList()
        if (!Files.exists(referencesPath)) return emptyList()

        return Files.list(referencesPath)
            .filter { Files.isRegularFile(it) }
            .toList()
    }

    /**
     * Get list of assets in a skill's assets directory.
     *
     * @param definition Skill definition
     * @return List of asset file paths
     */
    fun getSkillAssets(definition: SkillDefinition): List<Path> {
        val assetsPath = definition.assetsPath ?: return emptyList()
        if (!Files.exists(assetsPath)) return emptyList()

        return Files.list(assetsPath)
            .filter { Files.isRegularFile(it) }
            .toList()
    }
}
