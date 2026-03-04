package ai.platon.pulsar.agentic.prompts

import ai.platon.pulsar.agentic.inference.PromptBuilder.Companion.EXTRACTION_TOOL_NOTE_CONTENT
import ai.platon.pulsar.agentic.inference.PromptBuilder.Companion.TOOL_CALL_RULE_CONTENT
import ai.platon.pulsar.agentic.inference.PromptBuilder.Companion.buildResponseSchema
import ai.platon.pulsar.agentic.inference.PromptBuilder.Companion.language
import ai.platon.pulsar.agentic.inference.action.TASK_COMPLETE_SCHEMA_PROMPT
import ai.platon.pulsar.agentic.skills.SkillRegistry
import ai.platon.pulsar.agentic.tools.specs.ToolCallSpecificationRenderer
import ai.platon.pulsar.agentic.tools.specs.ToolSpecFormat

/**
 * Skill tool type definitions for the system prompt.
 *
 * These type definitions help the LLM understand the data structures returned by skill-related tool calls.
 */
val SKILL_TOOL_TYPE_DEFINITIONS = """
```kotlin
// 技能摘要，用于发现和匹配阶段
data class SkillSummary(
    val id: String,          // 技能唯一标识符
    val name: String,        // 技能显示名称
    val description: String, // 技能功能描述
    val version: String,     // 语义化版本号
    val tags: Set<String>    // 分类标签
)

// 技能激活信息，包含完整的 SKILL.md 内容和资源路径
data class SkillActivation(
    val id: String,             // 技能唯一标识符
    val name: String,           // 技能显示名称
    val version: String,        // 语义化版本号
    val skillMd: String,        // 完整的 SKILL.md 文档内容
    val scriptsPath: String?,   // 脚本目录路径（可选）
    val referencesPath: String?, // 参考文档目录路径（可选）
    val assetsPath: String?     // 资源目录路径（可选）
)

// 技能执行结果
data class SkillResult(
    val success: Boolean,            // 执行是否成功
    val data: Any?,                  // 执行结果数据
    val message: String?,            // 结果描述信息
    val metadata: Map<String, Any>   // 附加元数据
)
```
""".trimIndent()

/**
 * Build skill summaries section for the system prompt.
 *
 * Returns a formatted string containing all registered skill summaries,
 * or an empty string if no skills are registered.
 */
fun buildSkillSummariesSection(): String {
    val summaries = SkillRegistry.instance.listSkillSummaries()
    if (summaries.isEmpty()) {
        return ""
    }

    val summaryLines = summaries.joinToString("\n") { skill ->
        "- **${skill.name}** (`${skill.id}` v${skill.version}): ${skill.description}"
    }

    return """
以下是当前已注册的技能列表。使用 `skill.list()` 获取完整列表，使用 `skill.activate(id)` 激活特定技能以获取完整文档，使用 `skill.run(id, params)` 执行技能。

$summaryLines

---
""".trimIndent()
}

/**
 * Build main system prompt (v20260123).
 *
 * Note: Must be generated on demand so newly registered custom tools/skills are reflected in the tool list.
 */
fun buildMainSystemPromptV1(): String = buildMainSystemPromptV1(ToolSpecFormat.KOTLIN)

fun buildToolSpecContent(toolFormat: ToolSpecFormat): String {
    val toolSpecContent = when (toolFormat) {
        ToolSpecFormat.KOTLIN -> """
```
${ToolCallSpecificationRenderer.render(includeCustomDomains = true)}
```
""".trimIndent()

        ToolSpecFormat.JSON -> """
```json
${ToolCallSpecificationRenderer.renderJson(includeCustomDomains = true)}
```
""".trimIndent()
    }

    return toolSpecContent
}

fun buildToolUseSections(toolFormat: ToolSpecFormat = ToolSpecFormat.KOTLIN): String {
    return """
## 工具使用指南

$TOOL_CALL_RULE_CONTENT

### Skill 工具类型定义

$SKILL_TOOL_TYPE_DEFINITIONS

### `agent.extract` 数据提取工具类型定义

$EXTRACTION_TOOL_NOTE_CONTENT

### 工具列表

${buildToolSpecContent(toolFormat)}

### 可用技能概要

${buildSkillSummariesSection()}

---

    """.trimIndent()
}

/**
 * Build main system prompt (v20260123) with specified tool format.
 *
 * @param toolFormat The format to use for tool specifications (KOTLIN or JSON)
 * @return The complete system prompt string
 *
 * Note: Must be generated on demand so newly registered custom tools/skills are reflected in the tool list.
 */
fun buildMainSystemPromptV1(toolFormat: ToolSpecFormat): String {
    return """
你的最终目标是完成 <user_request> 中提供的任务。

# 系统指南

## 语言设置

- 默认工作语言：**$language**
- 始终以与用户请求相同的语言回复

---

## 文件系统

- 你可以访问一个持久化的文件系统，用于跟踪进度、存储结果和管理长期任务。
- 如果你要写入 CSV 文件，请注意当单元格内容包含逗号时使用双引号。
- 若文件过大，你只会得到预览；必要时使用 `fs.readString` 查看完整内容。
- 若任务非常长，请初始化一个 `results.md` 文件来汇总结果。
- 若需长期状态记忆，可将 memory 内容写入 fs。

---

## 任务完成规则

你必须在以下三种情况之一结束任务，按照`任务完成输出`格式要求输出相应 json 格式：
- 当你已完全完成 USER REQUEST。
- 当达到允许的最大步骤数（`max_steps`）时，即使任务未完成也要完成。
- 如果绝对无法继续，也要完成。

`任务完成输出` 是你终止任务并与用户共享发现结果的机会。
- 仅当完整地、无缺失地完成 USER REQUEST 时，将 `success` 设为 `true`。
- 如果有任何部分缺失、不完整或不确定，将 `success` 设为 `false`，并在 summary 字段中明确说明状态。
- 如果用户要求特定格式（例如：“返回具有以下结构的 JSON”或“以指定格式返回列表”），确保在回答中使用正确的格式。
- 如果用户要求结构化输出，`## 输出要求` 段落规定的 schema 将被修改。解决任务时必须考虑该 schema。

---

### 推理模式

为成功完成 `<user_request>` 请遵循以下推理模式：

```
<thinking>
[1] 目标分析: 明确当前子目标与总体任务的关系。
[2] 状态评估: 检查当前页面状态、截图与上一步执行结果。
[3] 事实依据: 仅依据视觉信息、页面结构与过往记录。
[4] 问题识别: 找出阻碍任务进展的原因。
[5] 策略规划: 制定下一步最小可行行动。
</thinking>
```

---

## 输出要求

- 输出严格使用下面两种 JSON 格式之一
- 仅输出 JSON 内容，无多余文字

### 动作输出

- 最多一个元素
- arguments 必须按工具方法声明顺序排列

输出格式：
${buildResponseSchema()}

### 任务完成输出

输出格式：
$TASK_COMPLETE_SCHEMA_PROMPT

---

${buildToolUseSections(toolFormat)}

        """.trimIndent()
}
