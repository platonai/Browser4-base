# Web Scraping Skill

## Metadata

- **Skill ID**: `web-scraping`
- **Name**: Web Scraping
- **Version**: 1.0.0
- **Author**: Browser4
- **Tags**: `scraping`, `extraction`, `web`

## Description

Extract data from web pages using CSS selectors. This skill provides a powerful way to scrape content from websites by targeting specific elements using CSS selector patterns.

## Dependencies

None

## Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| url | String | Yes | - | The URL of the web page to scrape |
| selector | String | Yes | - | CSS selector to target specific elements |
| attributes | List<String> | No | ["text"] | List of attributes to extract (e.g., "text", "href", "src") |

## Return Value

Returns a `SkillResult` with the following data structure:

```json
{
  "url": "string",
  "selector": "string",
  "attributes": ["string"],
  "data": "extracted content"
}
```

## Usage Examples

### Basic Text Extraction

```kotlin
val result = registry.execute(
    skillId = "web-scraping",
    context = context,
    params = mapOf(
        "url" to "https://example.com",
        "selector" to ".content"
    )
)
```

### Extract Multiple Attributes

```kotlin
val result = registry.execute(
    skillId = "web-scraping",
    context = context,
    params = mapOf(
        "url" to "https://example.com/products",
        "selector" to "a.product-link",
        "attributes" to listOf("text", "href")
    )
)
```

## Tool Call Specification

```kotlin
ToolCallSpec(
    domain = "skill.scraping",
    method = "extract",
    arguments = [
        "url: String",
        "selector: String",
        "attributes: List<String> = listOf('text')"
    ],
    returnType = "Map<String, Any>",
    description = "Extract data from a web page using CSS selectors"
)
```

## Error Handling

The skill returns a failure result in the following cases:
- Missing required parameter `url`
- Missing required parameter `selector`
- Invalid URL format (must start with http:// or https://)

## Lifecycle Hooks

### onLoad
Initializes resources and loads configurations when the skill is registered.

### onBeforeExecute
Validates URL format before execution. Returns false if URL doesn't start with http:// or https://.

### onAfterExecute
Records the timestamp of successful scraping operations in shared resources.

### validate
Validates skill configuration and environment. Always returns true for this skill.

## Implementation Notes

- The current implementation provides simulated data for demonstration purposes
- In production, this skill would integrate with a real web scraping engine
- Supports both synchronous and asynchronous execution patterns
- Thread-safe and can be used in concurrent environments

## See Also

- [Form Filling Skill](../form-filling/SKILL.md)
- [Data Validation Skill](../data-validation/SKILL.md)
- [Skills Framework Documentation](/docs/skills-framework.md)
