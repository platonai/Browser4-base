# Claude Skills Framework

The Claude Skills framework provides a plugin-style architecture for encapsulating reusable browser automation patterns. Skills are self-contained modules with metadata, dependencies, and lifecycle hooks that can be dynamically loaded and composed.

## Overview

The Skills framework consists of the following core components:

- **Skill**: Interface defining a self-contained automation module
- **SkillMetadata**: Describes a skill with id, version, dependencies, and tags
- **SkillContext**: Provides runtime context with session info and shared resources
- **SkillRegistry**: Central registry for managing registered skills
- **SkillLoader**: Handles dynamic loading of skills with dependency resolution
- **SkillComposer**: Creates composite skills from multiple simpler skills
- **SkillResult**: Represents the outcome of a skill execution

## Quick Start

### Creating a Simple Skill

```kotlin
class MyCustomSkill : AbstractSkill() {
    override val metadata = SkillMetadata(
        id = "my-custom-skill",
        name = "My Custom Skill",
        version = "1.0.0",
        description = "Does something useful",
        author = "Your Name",
        tags = setOf("automation", "custom")
    )

    override suspend fun execute(
        context: SkillContext,
        params: Map<String, Any>
    ): SkillResult {
        // Your implementation here
        val url = params["url"] as? String 
            ?: return SkillResult.failure("Missing url parameter")
        
        // Do something useful
        val result = performAutomation(url)
        
        return SkillResult.success(
            data = result,
            message = "Successfully completed automation"
        )
    }
}
```

### Registering and Using a Skill

```kotlin
// Create context
val context = SkillContext(
    sessionId = "session-123",
    config = mapOf("timeout" to 30000)
)

// Get registry and loader
val registry = SkillRegistry.instance
val loader = SkillLoader(registry)

// Load the skill
val skill = MyCustomSkill()
loader.load(skill, context)

// Execute the skill
val result = registry.execute(
    skillId = "my-custom-skill",
    context = context,
    params = mapOf("url" to "https://example.com")
)

if (result.success) {
    println("Result: ${result.data}")
} else {
    println("Error: ${result.message}")
}
```

## Core Concepts

### Skill Metadata

Every skill must provide metadata that describes it:

```kotlin
SkillMetadata(
    id = "unique-skill-id",              // Required: Unique identifier
    name = "Human Readable Name",        // Required: Display name
    version = "1.0.0",                   // Required: Semantic version
    description = "What it does",        // Optional: Description
    author = "Author Name",              // Optional: Creator
    dependencies = listOf("other-skill"), // Optional: Skill dependencies
    tags = setOf("tag1", "tag2")         // Optional: Categorization tags
)
```

### Skill Context

The SkillContext provides runtime information and shared resources:

```kotlin
val context = SkillContext(
    sessionId = "session-123",
    config = mapOf("setting" to "value"),
    sharedResources = mutableMapOf()
)

// Access configuration
val timeout = context.getConfig("timeout", 30000)

// Share resources between skills
context.setResource("scraped-data", data)
val data = context.getResource<DataType>("scraped-data")
```

### Skill Lifecycle

Skills have four lifecycle hooks:

1. **onLoad()**: Called when the skill is registered
2. **onBeforeExecute()**: Called before execution (can cancel execution)
3. **execute()**: Main execution logic
4. **onAfterExecute()**: Called after execution (for cleanup/logging)
5. **onUnload()**: Called when the skill is unregistered

```kotlin
class LifecycleAwareSkill : AbstractSkill() {
    override val metadata = SkillMetadata(
        id = "lifecycle-skill",
        name = "Lifecycle Aware Skill"
    )

    override suspend fun onLoad(context: SkillContext) {
        super.onLoad(context)
        // Initialize resources
        println("Skill loaded")
    }

    override suspend fun onBeforeExecute(
        context: SkillContext,
        params: Map<String, Any>
    ): Boolean {
        // Validate parameters
        return params.containsKey("required-param")
    }

    override suspend fun execute(
        context: SkillContext,
        params: Map<String, Any>
    ): SkillResult {
        // Main logic
        return SkillResult.success()
    }

    override suspend fun onAfterExecute(
        context: SkillContext,
        params: Map<String, Any>,
        result: SkillResult
    ) {
        // Log metrics, cleanup
        println("Execution completed: ${result.success}")
    }

    override suspend fun onUnload(context: SkillContext) {
        super.onUnload(context)
        // Clean up resources
        println("Skill unloaded")
    }
}
```

## Skill Dependencies

Skills can depend on other skills. Dependencies are automatically resolved during loading:

```kotlin
// Base skill
class DataFetcherSkill : AbstractSkill() {
    override val metadata = SkillMetadata(
        id = "data-fetcher",
        name = "Data Fetcher"
    )
    // ... implementation
}

// Dependent skill
class DataProcessorSkill : AbstractSkill() {
    override val metadata = SkillMetadata(
        id = "data-processor",
        name = "Data Processor",
        dependencies = listOf("data-fetcher") // Requires data-fetcher
    )
    // ... implementation
}

// Load with automatic dependency resolution
val loader = SkillLoader(registry)
val results = loader.loadAll(
    listOf(DataProcessorSkill(), DataFetcherSkill()), // Order doesn't matter
    context
)
```

## Composite Skills

Use the SkillComposer to create skills that combine multiple simpler skills:

### Sequential Composition

Execute skills one after another:

```kotlin
val composer = SkillComposer(registry)

// Register component skills
registry.register(FetchDataSkill(), context)
registry.register(ProcessDataSkill(), context)
registry.register(SaveDataSkill(), context)

// Create sequential composite
val pipeline = composer.sequential(
    "data-pipeline",
    listOf("fetch-data", "process-data", "save-data")
)
registry.register(pipeline, context)

// Execute the entire pipeline
val result = registry.execute("data-pipeline", context)
```

### Parallel Composition

Execute skills concurrently:

```kotlin
// Create parallel composite
val parallel = composer.parallel(
    "multi-scraper",
    listOf("scrape-site-a", "scrape-site-b", "scrape-site-c")
)
registry.register(parallel, context)

// Execute all skills in parallel
val result = registry.execute("multi-scraper", context)
```

## Tool Call Specifications

Skills can expose tool call specifications for agent integration:

```kotlin
class WebScrapingSkill : AbstractSkill() {
    override val metadata = SkillMetadata(
        id = "web-scraping",
        name = "Web Scraping"
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
            description = "Extract data from a web page"
        )
    )
    
    // ... implementation
}
```

## Skill Discovery

Find skills by tags or author:

```kotlin
// Find all scraping skills
val scrapingSkills = registry.findByTag("scraping")

// Find all skills by an author
val mySkills = registry.findByAuthor("My Organization")

// Get all registered skills
val allSkills = registry.getAll()

// Get skill metadata
val metadata = registry.getAllMetadata()
```

## Error Handling

Skills use SkillResult to communicate success or failure:

```kotlin
override suspend fun execute(
    context: SkillContext,
    params: Map<String, Any>
): SkillResult {
    return try {
        val data = performOperation(params)
        SkillResult.success(
            data = data,
            message = "Operation completed"
        )
    } catch (e: Exception) {
        SkillResult.failure(
            message = "Operation failed: ${e.message}",
            metadata = mapOf("exception" to e)
        )
    }
}
```

## Best Practices

1. **Keep skills focused**: Each skill should do one thing well
2. **Use meaningful IDs**: Choose descriptive, unique skill IDs
3. **Document dependencies**: Clearly specify all required skills
4. **Validate inputs**: Use onBeforeExecute() to validate parameters
5. **Handle errors gracefully**: Always return SkillResult with clear messages
6. **Clean up resources**: Use onUnload() to release resources
7. **Use semantic versioning**: Follow semver for version numbers
8. **Tag appropriately**: Use tags for skill discovery and categorization

## Example Skills

The framework includes several example skills:

- **WebScrapingSkill**: Extract data from web pages using CSS selectors
- **FormFillingSkill**: Automatically fill web forms with provided data
- **DataValidationSkill**: Validate data against specified rules

See the `examples` package for complete implementations.

## API Reference

### Skill Interface

```kotlin
interface Skill {
    val metadata: SkillMetadata
    val toolCallSpecs: List<ToolCallSpec>
    val targetClass: KClass<*>?
    
    suspend fun execute(context: SkillContext, params: Map<String, Any>): SkillResult
    suspend fun onLoad(context: SkillContext)
    suspend fun onUnload(context: SkillContext)
    suspend fun onBeforeExecute(context: SkillContext, params: Map<String, Any>): Boolean
    suspend fun onAfterExecute(context: SkillContext, params: Map<String, Any>, result: SkillResult)
    suspend fun validate(context: SkillContext): Boolean
}
```

### SkillRegistry

```kotlin
class SkillRegistry {
    companion object {
        val instance: SkillRegistry
    }
    
    suspend fun register(skill: Skill, context: SkillContext)
    suspend fun unregister(skillId: String, context: SkillContext): Boolean
    fun get(skillId: String): Skill?
    fun contains(skillId: String): Boolean
    fun getAll(): List<Skill>
    fun findByTag(tag: String): List<Skill>
    fun findByAuthor(author: String): List<Skill>
    suspend fun execute(skillId: String, context: SkillContext, params: Map<String, Any>): SkillResult
    suspend fun clear(context: SkillContext)
}
```

### SkillLoader

```kotlin
class SkillLoader(registry: SkillRegistry) {
    suspend fun load(skill: Skill, context: SkillContext): Boolean
    suspend fun loadAll(skills: List<Skill>, context: SkillContext): Map<String, Boolean>
    suspend fun reload(skill: Skill, context: SkillContext): Boolean
    suspend fun unload(skillId: String, context: SkillContext): Boolean
    suspend fun unloadAll(skillIds: List<String>, context: SkillContext): Map<String, Boolean>
}
```

### SkillComposer

```kotlin
class SkillComposer(registry: SkillRegistry) {
    fun sequential(compositeId: String, skillIds: List<String>, metadata: SkillMetadata?): Skill
    fun parallel(compositeId: String, skillIds: List<String>, metadata: SkillMetadata?): Skill
}
```

## Testing

The framework includes comprehensive tests:

- **SkillTest**: Tests for Skill interface and base classes
- **SkillRegistryTest**: Tests for skill registration and management
- **SkillLoaderTest**: Tests for loading and dependency resolution
- **SkillComposerTest**: Tests for composite skill creation
- **ExampleSkillsTest**: Integration tests with example skills

## Future Enhancements

Potential future additions to the framework:

- Skill versioning and compatibility checks
- Skill marketplace/repository support
- Skill hot-reloading
- Skill isolation/sandboxing
- Skill telemetry and monitoring
- Skill templates and scaffolding tools
