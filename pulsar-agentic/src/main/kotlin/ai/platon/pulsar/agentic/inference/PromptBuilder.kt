package ai.platon.pulsar.agentic.inference

import ai.platon.browser4.driver.chrome.dom.DOMSerializer
import ai.platon.browser4.driver.chrome.dom.model.TabState
import ai.platon.pulsar.agentic.inference.action.OBSERVE_RESPONSE_ELEMENT_SCHEMA
import ai.platon.pulsar.agentic.inference.action.TASK_COMPLETE_SCHEMA_PROMPT
import ai.platon.pulsar.agentic.model.AgentHistory
import ai.platon.pulsar.agentic.model.AgentState
import ai.platon.pulsar.agentic.model.ExecutionContext
import ai.platon.pulsar.agentic.prompts.buildMainSystemPromptV1
import ai.platon.pulsar.agentic.prompts.buildToolUseSections
import ai.platon.pulsar.common.KStrings
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.ai.llm.PromptTemplate
import ai.platon.pulsar.common.brief
import ai.platon.pulsar.common.serialize.json.Pson
import java.util.*

/**
 * Description:
 * Builder for language-localized prompt snippets used by agentic browser tasks.
 *
 * Prompt key points:
 * - Locale-aware (CN/EN) output
 * - Produces structured fragments for system/user roles
 * - Minimizes extra text to steer LLM behavior
 */
class PromptBuilder() {

    companion object {
        var locale: Locale = Locale.CHINESE

        val isZH = locale in listOf(Locale.CHINESE, Locale.SIMPLIFIED_CHINESE, Locale.TRADITIONAL_CHINESE)

        val language = if (isZH) "中文" else "English"

        const val MAX_ACTIONS = 1

        fun buildResponseSchema(): String {
            return buildObserveResultSchema(returnAction = true)
        }

        /**
         * Build the JSON schema for observing results.
         *
         * See [ai.platon.pulsar.agentic.inference.action.ModelObserveResponseElements]
         * */
        fun buildObserveResultSchema(returnAction: Boolean): String {
            // English is better for LLM to understand JSON
            val schema1 = OBSERVE_RESPONSE_ELEMENT_SCHEMA

            val schema2 = """
{
  "elements": [
    {
      "locator": string,
      "description": string
    }
  ]
}
""".let { Strings.compactWhitespaces(it) }

            return if (returnAction) schema1 else schema2
        }

        val TOOL_CALL_RULE_CONTENT = """
- domain: 工具域，如 driver, browser, skill.debug.scraping 等，用点号区分子域
- method: 方法名，如 click, fill, extract 等
- 输出结果中，定位节点时 `selector` 字段始终填入 `locator` 的值
- 确保 `locator` 与对应的可交互元素列表中的 `locator` 完全匹配，或者与无障碍树节点属性完全匹配，准确定位该节点
- JSON 格式输出时，禁止包含任何额外文本
- 从`## 浏览器状态`段落获得所有打开标签页的信息
- 如需检索信息，新建标签页而非复用当前页
- 使用 `click(selector, "Ctrl")` 新建标签页，在**新标签页**打开链接。系统若为 macOS，自动将 Ctrl 映射为 Meta
- 如果目标页面在**新标签页**打开，使用 `browser.switchTab(tabId: String)` 切换到目标页面，从`## 浏览器状态`段落获得 `tabId`
- 若预期元素缺失，尝试刷新页面、滚动或返回上一页
- 若向字段输入内容：1. 无需先滚动和聚焦（工具内部处理）2. 可能需1) 回车 2) 显式搜索按钮 3) 下拉选项以完成操作。
- 若填写输入框后操作序列中断，通常是因为页面发生了变化（例如输入框下方弹出了建议选项）
- 若出现验证码，尽可能尝试解决；若无法解决，则启用备用策略（例如换其他站点、回退上一步）
- 若页面因输入文本等操作发生变化，需判断是否要交互新出现的元素（例如从列表中选择正确选项）。
- 若上一步操作序列因页面变化而中断，需补全未执行的剩余操作。例如，若你尝试输入文本并点击搜索按钮，但点击未执行（因页面变化），应在下一步重试点击操作。
- 始终考虑最终目标：<user_request>包含的内容。若用户指定了明确步骤，这些步骤始终具有最高优先级。
- 若<user_request>中包含具体页面信息（如商品类型、评分、价格、地点等），尝试使用筛选功能以提高效率。
- 如无必要，不要登录页面。没有凭证时，绝对不要尝试登录。
- 始终先判断任务属于两类哪一种：
    1. 非常具体的逐步指令
       - 精确地遵循这些步骤，不要跳过，尽力完成每一项要求。
    2. 开放式任务：
       - 自行规划并有创造性地完成任务。
       - 如果你在开放式任务中被卡住（例如遇到登录或验证码），可以重新评估任务并尝试替代方案，例如有时即使出现登录弹窗，页面的某些部分仍可访问，或者可以通过网络搜索获得信息。

    """.trimIndent()

        val TOOL_CALL_RULE_CONTENT_EN = """
Browser use guidelines:

* **domain**: Method domain, such as `driver`, `browser`, `skill.debug.scraping`, etc. Subdomains may be separated by dots.
* **method**: Method name, such as `click`, `fill`, `extract`, etc.
* In output results, when locating a node, always set the `selector` field to the value of `locator`.
* Ensure the `locator` exactly matches the corresponding `locator` in the interactive element list, or fully matches the accessibility tree node attributes, to precisely identify the target node.
* When outputting JSON, do not include any additional text.
* Obtain information about all open tabs from the `## Browser State` section.
* When searching for information, open a new tab instead of reusing the current one.
* Use `click(selector, "Ctrl")` to open links in a **new tab**. On macOS, `Ctrl` is automatically mapped to `Meta`.
* If the target page opens in a **new tab**, use `browser.switchTab(tabId: String)` to switch to it. Retrieve `tabId` from the `## Browser State` section.
* If an expected element is missing, attempt to refresh the page, scroll, or navigate back.
* When entering text into a field:

  1. No need to scroll or focus first (handled internally).
  2. You may need to: (1) press Enter, (2) explicitly click a search button, or (3) select from a dropdown to complete the action.
* If the operation sequence is interrupted after filling an input field, it is typically due to page changes (e.g., suggestion options appearing below the field).
* If a CAPTCHA appears, attempt to resolve it whenever possible; if unsuccessful, apply fallback strategies (e.g., switch sites or go back).
* If the page changes due to text input or other actions, determine whether interaction with newly appeared elements is required (e.g., selecting the correct option from a list).
* If a previous operation sequence was interrupted due to page changes, complete the remaining steps. For example, if you attempted to input text and click the search button but the click was not executed (due to page changes), retry the click in the next step.
* Always keep the ultimate objective in mind: the content within `<user_request>`. If explicit steps are provided, they take highest priority.
* If `<user_request>` includes specific page criteria (e.g., product type, rating, price, location), use filtering features whenever possible to improve efficiency.
* Do not log in unless necessary. Never attempt to log in without credentials.
* Always first determine which of the following two categories the task belongs to:

  1. **Highly specific step-by-step instructions**

     * Follow the steps precisely. Do not skip any. Fulfill each requirement to the best of your ability.
  2. **Open-ended tasks**

     * Plan autonomously and complete the task creatively.
     * If blocked during an open-ended task (e.g., login prompt or CAPTCHA), reassess and attempt alternative approaches. For example, even if a login modal appears, parts of the page may still be accessible, or the required information may be obtainable via web search.

        """.trimIndent()

        /**
         * TODO: move to skill
         * */
        val EXTRACTION_TOOL_NOTE_CONTENT = """
使用 `agent.extract` 满足高级数据提取要求，仅当 `textContent`, `selectFirstTextOrNull` 不能满足要求时使用。

参数说明：

1. `instruction`: 准确描述 1. 数据提取目标 2. 数据提取要求
2. `schema`: 数据提取结果的 schema 要求，以 JSON 格式描述，并且遵循下面结构
3. instruction 负责『做什么』，schema 负责『输出形状』；出现冲突时以 schema 为准

Schema 参数结构：
```
class ExtractionField(
    val name: String,
    val type: String = "string",                 // JSON schema primitive or 'object' / 'array'
    val description: String,
    val required: Boolean = true,
    val objectMemberProperties: List<ExtractionField> = emptyList(), // define the schema of member properties if type == object
    val arrayElements: ExtractionField? = null                    // define the schema of elements if type == array
)
class ExtractionSchema(val fields: List<ExtractionField>)
```

例：
```
{
  "fields": [
    {
      "name": "product",
      "type": "object",
      "description": "Product info",
      "objectMemberProperties": [
        {
          "name": "name",
          "type": "string",
          "description": "Product name",
          "required": true
        },
        {
          "name": "variants",
          "type": "array",
          "required": false,
          "arrayElements": {
            "name": "variant",
            "type": "object",
            "required": false,
            "objectMemberProperties": [
              { "name": "sku", "type": "string", "required": false },
              { "name": "price", "type": "number", "required": false }
            ]
          }
        }
      ]
    }
  ]
}
```

"""

        val INTERACTIVE_ELEMENT_LIST_NOTE_CONTENT = """
(Interactive Elements)

可交互元素列表包含页面 DOM 可交互元素的主要信息，包括元素简化 HTML 表示，文本内容，前后文本，所在视口，坐标和大小等。

列表格式：
[locator]{viewport}(x,y,width,height)<slimNode>textContent</slimNode>Text-Before-This-Interactive-Element-And-After-Previous-Interactive-Element

- 默认列出当前焦点视口，第1，2视口和最后一视口元素。
- 节点唯一定位符 `locator` 由两个整数组成，不含括号，同无障碍树保持一致。
- `viewport` 为节点所在视口序号，1-based，不含括号。
- 注意：网页内容变化可能导致视口位置随时发生变化。
- `x,y,width,height` 为节点坐标和尺寸。

        """.trimIndent()

        val A11Y_TREE_NOTE_CONTENT = """
(Accessibility Tree)

无障碍树包含页面 DOM 关键节点的主要信息，包括节点文本内容，可见性，可交互性，坐标和尺寸等。

- 除非特别指定，无障碍树仅包含网页当前视口内的节点信息，并包含少量视口外节点，以保证信息充分。
- 节点唯一定位符 `locator`。
- 对所有节点：`invisible` 默认为 `false`，`scrollable` 默认为 `false`, `interactive` 默认为 `false`。
- 对于坐标和尺寸，若未显式赋值，则视为 `0`。涉及属性：`clientRects`, `scrollRects`, `bounds`。

        """.trimIndent()

        const val SINGLE_ACTION_GENERATION_PROMPT = """
根据动作描述和网页内容，选择最合适一个或多个工具。

## 动作描述

{{ACTION_DESCRIPTIONS}}

---

## 工具列表

```kotlin
{{TOOL_CALL_SPECIFICATION}}
```

---

## 网页内容

网页内容以无障碍树的形式呈现:

{{NANO_TREE_LAZY_JSON}}

---

## 输出要求

- 仅输出 JSON 内容，无多余文字
- domain 取值 driver
- method 和 arguments 遵循 `## 工具列表` 的函数表达式

输出格式：
{{OUTPUT_SCHEMA_ACT}}

---

        """

        val OBSERVE_GUIDE_OUTPUT_SCHEMA = """
{
  "elements": [
    {
      "locator": "Web page node locator",
      "description": "Description of the current locator and tool selection",
      "screenshotContentSummary": "Summary of the current screenshot content",
      "currentPageContentSummary": "Summary of the current web page text content, based on the accessibility tree or web content extraction results",
      "memory": "1–3 specific sentences describing this step and the overall progress. This should include information helpful for future progress tracking, such as the number of pages visited or items found.",
      "thinking": "A structured <think>-style reasoning block."
    }
  ]
}
        """.trimIndent()

        val OBSERVE_GUIDE_OUTPUT_SCHEMA_RETURN_ACTIONS = """
{
  "elements": [
    {
      "locator": "Web page node locator",
      "description": "Description of the current locator and tool selection",
      "domain": "Tool domain, such as `driver`",
      "method": "Method name, such as `click`",
      "arguments": [
        {
          "name": "Parameter name, such as `selector`",
          "value": "Parameter value, such as `0,4`"
        }
      ],
      "screenshotContentSummary": "Summary of the current screenshot content",
      "currentPageContentSummary": "Summary of the current web page text content, based on the accessibility tree or web content extraction results",
      "memory": "1–3 specific sentences describing this step and the overall progress. This should include information helpful for future progress tracking, such as the number of pages visited or items found.",
      "thinking": "A structured <think>-style reasoning block."
    }
  ]
}
        """.trimIndent()

        val OBSERVE_GUIDE_SYSTEM_MESSAGE = """
## 总体要求

你正在通过根据用户希望观察的页面内容来查找元素，帮助用户实现浏览器操作自动化。
你将获得：
- 一条关于待观察元素的指令
- 一个包含网页所有可交互元素信息的列表
- 一个展示页面语义结构的分层无障碍树（accessibility tree）。该树是DOM（文档对象模型）与无障碍树的混合体。

如果存在符合指令的元素，则返回这些元素的数组；否则返回空数组。

---

## 浏览器状态说明

浏览器状态包括：
- 当前 URL：你当前查看页面的 URL。
- 打开的标签页：带有 id 的打开标签页。

---

## 视觉信息说明

- 视觉信息如存在，作为首要事实依据（GROUND TRUTH）

---

## 可交互元素说明

$INTERACTIVE_ELEMENT_LIST_NOTE_CONTENT

---

## 无障碍树说明

$A11Y_TREE_NOTE_CONTENT

---

## 输出要求

- 输出严格使用下面 JSON 格式，仅输出 JSON 内容，无多余文字
- 最多一个元素，domain & method 字段不得为空

输出格式:
{{OUTPUT_SCHEMA_PLACEHOLDER}}

---

${buildToolUseSections()}

"""

        fun compactPrompt(prompt: String, maxWidth: Int = 200): String {
            val boundaries = """
你正在通过根据用户希望观察的页面内容来查找元素
否则返回空数组。

## 工具列表说明
---

## 无障碍树说明
---
            """.trimIndent()

            val boundaryPairs = boundaries.split("\n").filter { it.isNotBlank() }.chunked(2).map { it[0] to it[1] }

            val compacted = KStrings.replaceContentInSections(prompt, boundaryPairs, "\n...\n\n")

            return Strings.compactInline(compacted, maxWidth)
        }
    }

    fun buildOperatorSystemPrompt(): String {
        return """
${buildMainSystemPromptV1()}
        """.trimIndent()
    }

    fun buildMultistepAgentMessageListAll(context: ExecutionContext): AgentMessageList {
        // Prepare messages for model
        val messages = AgentMessageList()

        initObserveUserInstruction(context.instruction, messages)

        buildResolveMessageListStart(context, context.stateHistory, messages)

        // browser state, viewport info, interactive elements, DOM
        buildObserveUserMessageLast(messages, context)

        return messages
    }

    fun buildSingleObserveMessageListAll(params: ObserveParams, context: ExecutionContext): AgentMessageList {
        // Prepare messages for model
        val messages = AgentMessageList()

        // observe guide
        buildSingleObserveGuideSystemPrompt(messages, params)
        // browser state, viewport info, interactive elements, DOM
        buildObserveUserMessageLast(messages, context)

        return messages
    }

    fun buildResolveMessageListStart(
        context: ExecutionContext, stateHistory: AgentHistory,
        messages: AgentMessageList,
    ): AgentMessageList {
        val instruction = context.instruction

        val systemMsg = buildOperatorSystemPrompt()

        messages.addSystem(systemMsg)
        messages.addLastIfAbsent("user", buildUserRequestMessage(instruction), name = "user_request")
        messages.addUser(buildAgentStateHistoryMessage(stateHistory))
        if (context.screenshotB64 != null) {
            messages.addUser(buildBrowserVisionInfo())
        }

        val prevTCResult = context.agentState.prevState?.toolCallResult
        if (prevTCResult != null) {
            messages.addUser(buildPrevToolCallResultMessage(context))
        }

        return messages
    }

    fun buildObserveGuideSystemExtraPrompt(userProvidedInstructions: String?): SimpleMessage? {
        if (userProvidedInstructions.isNullOrBlank()) return null

        val contentCN = """
## 用户自定义指令

在执行操作时请牢记用户的指令。如果这些指令与当前任务无关，请忽略。

用户指令：
$userProvidedInstructions

---

""".trim()

        val contentEN = contentCN

        val content = if (isZH) contentCN else contentEN

        return SimpleMessage("system", content)
    }

    fun buildExtractSystemPrompt(userProvidedInstructions: String? = null): SimpleMessage {
        val userInstructions = buildObserveGuideSystemExtraPrompt(userProvidedInstructions)

        val content = """
# 系统指南

你正在代表用户提取内容。如果用户要求你提取“列表”信息或“全部”信息，你必须提取用户请求的所有信息。

你将获得：
1. 一条指令
2. 一个要从中提取内容的 DOM 元素列表

- 从 DOM 元素中原样打印精确文本，包含所有符号、字符和换行。
- 如果没有发现新的信息，打印 null 或空字符串。

<user_request>
$userInstructions
</user_request>

"""

        return SimpleMessage(role = "system", content = content)
    }

    fun buildAgentStateHistoryMessage(agentHistory: AgentHistory): String {
        val history = agentHistory.states
        if (history.isEmpty()) {
            return ""
        }

        val headingSize = 2
        val tailingSize = 8
        val totalSize = headingSize + tailingSize
        val stateHistory = when {
            history.size <= totalSize -> history
            else -> history.take(headingSize) + history.takeLast(tailingSize)
        }

        val historyJsonl = stateHistory.joinToString("\n") {
            Pson.toJson(
                mapOf(
                    "step" to it.step,
                    "toolCall" to it.actionDescription?.pseudoExpression,
                    "nextGoal" to it.nextGoal,
                    "thinking" to it.thinking,
                    "exception" to it.exception?.message,
                    "summary" to it.summary,
                    "keyFindings" to it.keyFindings
                )
            )
        }

        val msg = """
## 执行轨迹（按序）

(仅保留 $totalSize 步骤)

<agent_history>
$historyJsonl
</agent_history>

---

		""".trimIndent()

        return msg
    }

    fun buildBrowserVisionInfo(): String {
        val visionInfo = """
## 视觉信息

[Current page screenshot provided as base64 image]

---

""".trimIndent()

        return visionInfo
    }

    fun buildPrevToolCallResultMessage(context: ExecutionContext): String {
        val agentState = requireNotNull(context.agentState)
        val toolCallResult = requireNotNull(context.agentState.prevState?.toolCallResult)
        val evaluate = toolCallResult.evaluate
        val evalResult = evaluate?.value?.toString()
        val exception = evaluate?.exception?.cause
        val evalMessage = when {
            exception != null -> "[执行异常]\n" + exception.brief()
            evalResult.isNullOrBlank() -> "[执行成功]"
            else -> "[执行成功] 输出结果：$evalResult"
        }.let { Strings.compactInline(it, 5000) }
        val help = evaluate?.exception?.help?.takeIf { it.isNotBlank() }
        val helpMessage = help?.let { "帮助信息：\n```\n$it\n```" } ?: ""
        val lastModelError = agentState.actionDescription?.modelResponse?.modelError
        val lastModelMessage = if (lastModelError != null) {
            """
上步模型错误：

$lastModelError

        """
        } else ""

        return """
## 上步输出

上步操作：${agentState.prevState?.method}
上步期望结果：${agentState.prevState?.nextGoal}

上步执行结果：
```
$evalMessage
```

$helpMessage
$lastModelMessage
---
        """.trimIndent()
    }

    fun buildUserRequestMessage(userRequest: String): String {
        val msg = """
# 当前任务

## 用户输入
<user_request>
$userRequest
</user_request>

---

                """.trimIndent()

        return msg
    }

    fun initExtractUserInstruction(instruction: String? = null): String {
        if (instruction.isNullOrBlank()) {
            return """
从网页中提取关键数据结构。

- 每次提供一个视口高度(viewport height)内的所有无障碍树 DOM 节点，你的数据来源是无障碍树

""".trimIndent()
        }

        return instruction
    }

    fun buildExtractUserRequestPrompt(params: ExtractParams): String {
        return """
## 用户指令
<user_request>
${params.instruction}
</user_request>
        """.trimIndent()
    }

    fun buildExtractUserPrompt(params: ExtractParams): SimpleMessage {
        val browserState = params.agentState.browserUseState.browserState

        val scrollState = browserState.scrollState
        // Height in pixels of the page area above the current viewport. (被隐藏在视口上方的部分的高度)
        val hiddenTopHeight = scrollState.hiddenTopHeight
        val hiddenBottomHeight = scrollState.hiddenBottomHeight
        val viewportHeight = scrollState.viewportHeight
        val domState = params.agentState.browserUseState.domState

        // The 1-based viewport to see.
        val processingViewport = scrollState.processingViewport
        val viewportsTotal = scrollState.viewportsTotal
        val viewPortJson = Pson.toJson(
            "processingViewport" to processingViewport,
            "viewportHeight" to viewportHeight,
            "viewportsTotal" to viewportsTotal,
            "hiddenTopHeight" to hiddenTopHeight,
            "hiddenBottomHeight" to hiddenBottomHeight
        )
        val startY = scrollState.y.coerceAtLeast(0.0)
        val endY = (scrollState.y + viewportHeight).coerceAtLeast(0.0)
        val nanoTree = domState.microTree.toNanoTreeInRange(startY, endY)

        val schema = params.schema

        val content = """
## 视口信息

$viewPortJson

---

## 无障碍树
（仅当前视口范围内）
```yaml
${nanoTree.lazyYaml}
```

---

## 输出要求
你必须返回一个严格符合以下JSON Schema的有效JSON对象。不要包含任何额外说明。

${schema.toJsonSchema()}

        """.trimIndent()

        return SimpleMessage(role = "user", content = content)
    }

    fun buildMetadataSystemPrompt(): SimpleMessage {
        val metadataSystemPromptCN: String = """
你是一名 AI 助手，负责评估一次抽取任务的进展和完成状态。

- 每次提取当前视口范围内的数据
- 视口之上的数据已处理，视口之下的数据待处理

请分析抽取响应，判断任务是否已经完成或是否需要更多信息。
严格遵循以下标准：
1. 一旦当前抽取响应已经满足了指令，必须将完成状态设为 true 并停止处理，不论是否还有未查看视口。
2. 只有在以下两个条件同时成立时，才将完成状态设为 false：
   - 指令尚未被满足
   - 仍然有剩余视口数据未提取（viewportsTotal > processingViewport）

""".trimIndent()

        return SimpleMessage(
            role = "system",
            content = metadataSystemPromptCN,
        )
    }

    fun buildMetadataUserPrompt(
        instruction: String,
        extractionResponse: Any,
        agentState: AgentState,
    ): SimpleMessage {
        /**
         * The 1-based next chunk to see, each chunk is a viewport height.
         * */
        val browserUseState = agentState.browserUseState
        val scrollState = browserUseState.browserState.scrollState
        // Height in pixels of the page area above the current viewport. (被隐藏在视口上方的部分的高度)
        val hiddenTopHeight = scrollState.hiddenTopHeight
        val hiddenBottomHeight = scrollState.hiddenBottomHeight
        val viewportHeight = scrollState.viewportHeight

        // The 1-based viewport to see.
        val processingViewport = scrollState.processingViewport
        val viewportsTotal = scrollState.viewportsTotal

        val viewPortJson = Pson.toJson(
            "processingViewport" to processingViewport,
            "viewportHeight" to viewportHeight,
            "viewportsTotal" to viewportsTotal,
            "hiddenTopHeight" to hiddenTopHeight,
            "hiddenBottomHeight" to hiddenBottomHeight
        )

        val extractedJson = DOMSerializer.MAPPER.writeValueAsString(extractionResponse)

        val content =
            """
## 用户指令

<user_request>
$instruction
</user_request>

## 视口信息

$viewPortJson

---

## 提取结果

$extractedJson

---

""".trim()

        return SimpleMessage(role = "user", content = content)
    }

    private fun buildSingleObserveGuideSystemPrompt(messages: AgentMessageList, params: ObserveParams) {
        val schema =
            if (params.returnAction) OBSERVE_GUIDE_OUTPUT_SCHEMA_RETURN_ACTIONS else OBSERVE_GUIDE_OUTPUT_SCHEMA

        val observeSystemPrompt = PromptTemplate(OBSERVE_GUIDE_SYSTEM_MESSAGE).render(
            mapOf("OUTPUT_SCHEMA_PLACEHOLDER" to schema)
        )

        messages.addLast("system", observeSystemPrompt)

        val extra = buildObserveGuideSystemExtraPrompt(params.userProvidedInstructions)?.content
        if (extra != null) {
            messages.addLast("system", extra)
        }
    }

    fun initObserveUserInstruction(
        instruction: String?,
        messages: AgentMessageList = AgentMessageList()
    ): AgentMessageList {
        val instruction2 = when {
            !instruction.isNullOrBlank() -> instruction
            isZH -> """
根据上下文和当前进展，选择最适合工具推进任务执行，最终目标是完成用户任务。
                """.trimIndent()

            else -> """
Based on the context and current progress, select the most appropriate tool to advance the task toward user completion.
                """.trimIndent()
        }

        messages.addUser(instruction2, name = "user_request")
        return messages
    }

    private fun buildObserveUserMessageLast(messages: AgentMessageList, context: ExecutionContext) {
        val prevBrowserState = context.agentState.prevState?.browserUseState?.browserState
        val browserState = context.agentState.browserUseState.browserState

        val prevTabs = prevBrowserState?.tabs ?: emptyList()
        val currentTabs = browserState.tabs
        val newTabs: List<TabState> = if (prevTabs.size != currentTabs.size) {
            currentTabs - prevTabs.toSet()
        } else emptyList()
        val newTabsJson = if (newTabs.isNotEmpty()) DOMSerializer.toJson(newTabs) else null
        val newTabsMessage = if (newTabs.isEmpty()) "" else {
            """
上一步新打开的标签页：

$newTabsJson

            """.trimIndent()
        }

        val scrollState = browserState.scrollState
        // Height in pixels of the page area above the current viewport. (被隐藏在视口上方的部分的高度)
        val hiddenTopHeight = scrollState.hiddenTopHeight
        val hiddenBottomHeight = scrollState.hiddenBottomHeight
        val viewportHeight = scrollState.viewportHeight
        val domState = context.agentState.browserUseState.domState

        // The 1-based viewport to see.
        val processingViewport = scrollState.processingViewport
        val viewportsTotal = scrollState.viewportsTotal

        val interactiveElements = context.agentState.browserUseState.getInteractiveElements()
        val viewPortJson = Pson.toJson(
            "processingViewport" to processingViewport,
            "viewportHeight" to viewportHeight,
            "viewportsTotal" to viewportsTotal,
            "hiddenTopHeight" to hiddenTopHeight,
            "hiddenBottomHeight" to hiddenBottomHeight
        )
        val delta = viewportHeight * 0.5
        val startY = (scrollState.y - delta).coerceAtLeast(0.0)
        val endY = (scrollState.y + viewportHeight + delta).coerceAtLeast(0.0)
        val nanoTree = domState.microTree.toNanoTreeInRange(startY, endY)

        fun contentCN() = """
## 浏览器状态

<browser_state>
${browserState.lazyJson}
</browser_state>

$newTabsMessage

---

## 视口信息

$viewPortJson

- 默认每次查看一个视口高度(viewport height)内的所有 DOM 节点
- 注意：网页内容变化可能导致视口位置和视口序号随时发生变化。
- 默认提供的无障碍树仅包含第`i`个视口内的 DOM 节点，并包含少量视口外邻近节点，以保证信息完整
- 如需查看下一视口，调用 `scrollBy(viewportHeight)` 向下滚动一屏获取更多信息

## 可交互元素

聚焦第${processingViewport}视口可交互元素。

${interactiveElements.lazyString}

## 无障碍树

聚焦第${processingViewport}视口节点。

```yaml
${nanoTree.lazyYaml}
```

---

"""

        fun contentEN() = contentCN()

        val content = when {
            isZH -> contentCN()
            else -> contentEN()
        }

        messages.addLast("user", content)
    }

    fun buildObserveActToolUsePrompt(action: String): String {
        val instruction =
            """
## 用户输入

根据以下动作选择一个工具来执行该动作：$action。查找动作、工具和目标最相关的页面元素。分析执行后的影响和预期结果。

---

"""

        return instruction
    }

    fun buildSummaryPrompt(goal: String, agentHistory: AgentHistory): Pair<String, String> {
        val system = "你是总结助理，请基于执行轨迹对原始目标进行总结，输出 JSON。"

        val history = buildAgentStateHistoryMessage(agentHistory)

        val user = """
## 原始目标
$goal

---

$history

## 输出要求

严格输出 JSON，无多余文字：

$TASK_COMPLETE_SCHEMA_PROMPT

---

        """.trimIndent()

        return system to user
    }

    fun tr(text: String) = translate(text)

    /**
     * Translate to another language, reserved
     * */
    fun translate(text: String): String {
        return text
    }
}
