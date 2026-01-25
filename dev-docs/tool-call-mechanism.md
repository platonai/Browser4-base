# Agent Tool Call 机制（pulsar-agentic）

> 适用代码目录：`pulsar-agentic/src/main/kotlin/ai/platon/pulsar/agentic/tools`

本文档解释 Browser4/Platon Pulsar 的 Agent **Tool Call**（工具调用）机制：工具规范如何暴露给 LLM、LLM 产出的 `ToolCall` 如何被路由到目标对象、内置域（driver/browser/fs/agent/system）与自定义域（Custom Tool）如何扩展，以及常见的错误/超时处理。

---

## 1. 核心概念（数据流契约）

### 1.1 ToolCall 是什么

`ToolCall` 是一次“调用某个工具方法”的结构化描述，核心字段用于表达：

- `domain`: 工具域名（如 `driver`、`browser`）
- `method`: 方法名（如 `navigateTo`、`click`）
- `arguments`: 参数（键值对，或被转换后的 map）

在运行时，Agent 会把模型产出的 `ToolCall` 填充进 `ActionDescription.toolCall`，然后由 `AgentToolManager.execute(...)` 负责执行并返回 `ToolCallResult`。

### 1.2 TcEvaluate / ToolCallResult

- `TcEvaluate`：单次调用的执行结果容器（常见包含 value / exception / expression 等信息）。
- `ToolCallResult`：对外返回的“最终结构”，包括：
  - `success`：成功/失败
  - `evaluate`：`TcEvaluate?`
  - `message`：附加信息
  - `actionDescription`：原始动作描述（用于链路追踪）

> 约定：当执行中抛异常时，`ToolCallResult.success=false`，并把异常封装为 `TcEvaluate(expression, e)`。

---

## 2. 内置工具域（Built-in Domains）

内置工具域由 `AgentToolManager` 固定路由。当前内置域：

- `driver` → `WebDriver`（页面导航、元素交互等）
- `browser` → `driver.browser`（Tab 管理等）
- `fs` → `AgentFileSystem`（读写文件等）
- `agent` → `BrowserAgentActor`（更高层的 agent 能力）
- `system` → `SystemToolExecutor`（help/元能力）

对应的工具签名（给 LLM 看）定义在：

- `ToolSpecification.TOOL_CALL_SPECIFICATION`

其中还包含：

- `ToolSpecification.SUPPORTED_TOOL_CALLS`：过滤出可调用行
- `ToolSpecification.SUPPORTED_ACTIONS`：仅提取 `domain.method`
- `ToolSpecification.MAY_NAVIGATE_ACTIONS`：可能触发导航的 method 集合（用于后置等待导航）

---

## 3. 执行流程（AgentToolManager.execute）

入口：`AgentToolManager.execute(actionDescription: ActionDescription, message: String? = null)`

### 3.1 快速取消（用户中断优先）

执行第一件事是尊重协程取消：

- 通过 `currentCoroutineContext().isActive` 检测
- 若已取消，立即返回 `success=false`，message 为 `USER interrupted`

### 3.2 域路由（domain dispatch）

核心在：

- 取出 `tc = actionDescription.toolCall`
- 按 `tc.domain` 做路由

内置域由 `when (tc.domain)` 直接映射到目标对象：

- `driver` → `driver`
- `browser` → `driver.browser`
- `fs` → `fs`
- `agent` → `agent`
- `system` → `system`

### 3.3 反射执行（ToolExecutor / BasicToolCallExecutor）

路由确定 target 之后，真正执行由：

- `BasicToolCallExecutor.callFunctionOn(tc, target)`

它会在 `toolExecutors` 中找到第一个 `targetClass` 能匹配 `target::class` 的 executor，并调用 executor 的 `callFunctionOn`。

> 注意：`BasicToolCallExecutor` 里也提供了 `eval(...)`（使用 Kotlin Script 引擎）能力，但 `AgentToolManager` 的主路径是走 `callFunctionOn`。

### 3.4 后置副作用处理（post hooks）

执行成功后会根据 method 做一些补偿/状态同步：

- `switchTab` → `onDidSwitchTab`：把“切到前台”的 driver 重新绑定到 session
- `navigateTo` → `onDidNavigateTo`：等待导航完成、等待 `body` 出现，并额外 delay（当前固定 3s）

### 3.5 统一的导航等待（可能触发导航的动作）

为了减少“模型认为已经跳转但页面还没稳定”的情况，`AgentToolManager` 对 `MAY_NAVIGATE_ACTIONS` 做统一等待：

- 取 oldUrl：`actionDescription.agentState?.browserUseState?.browserState?.url`
- 如果 oldUrl != null 且 method 属于 `ToolSpecification.MAY_NAVIGATE_ACTIONS`
  - 调用 `driver.waitForNavigation(oldUrl, timeoutMs=3000)`
  - 若超时，log warn 并仍返回成功的 `ToolCallResult`（不强制失败）

这种策略让 Agent 具备“尽量稳定，但不因为偶发导航慢而硬失败”的行为特征。

### 3.6 异常处理

任何异常都会：

- `logger.warn("Failed to execute tool call | $actionDescription", e)`
- 返回 `ToolCallResult(success=false, evaluate=TcEvaluate(expression, e), message=e.message)`

其中 `expression` 会择优使用：

- `pseudoExpression` → `cssFriendlyExpression` → `expression`

---

## 4. 工具帮助（help）机制

入口：`AgentToolManager.help(domain: String, method: String): String`

它会：

1) 先查内置 `concreteExecutors` 是否有对应 domain 的 executor，并调用 `help(method)`
2) 否则查 `CustomToolRegistry` 中是否注册了该 domain 的 executor，并调用 `help(method)`

其中 system 域也提供：

- `system.help(domain: String)`
- `system.help(domain: String, method: String)`

这些签名在 `ToolSpecification.TOOL_CALL_SPECIFICATION` 中对模型可见。

---

## 5. 自定义工具（Custom Tools）扩展

自定义工具有两个维度：

1) **执行器（Executor）注册**：告诉系统“domain=xxx 由谁执行”
2) **目标对象（Target）绑定**：告诉系统“domain=xxx 的方法调用，实际落到哪个对象实例上”

### 5.1 注册自定义 executor：CustomToolRegistry

- 单例：`CustomToolRegistry.instance`
- 注册：`register(executor)`
- 可选导出签名：
  - 如果 `executor` 实现 `ToolCallSpecificationProvider`，会自动把其 specs 缓存起来，用于 prompt 渲染
  - 或者使用 `register(executor, specs)` 直接提供 specs

### 5.2 绑定 domain 的 target：AgentToolManager.registerCustomTarget

`AgentToolManager` 内部维护：

- `customTargets: MutableMap<String, Any>`

当 `tc.domain` 不属于内置域时：

- 若 `CustomToolRegistry.instance.get(tc.domain)` 存在，则视为自定义域
- 但还需要 `customTargets[tc.domain]` 返回 target，否则会抛 `UnsupportedOperationException`

> 这意味着“注册 executor”与“绑定 target”缺一不可。

### 5.3 自定义工具签名如何暴露给模型

- 渲染入口：`ToolCallSpecificationRenderer.render(...)`
- 它会把 `ToolSpecification.TOOL_CALL_SPECIFICATION`（内置）原样输出
- 然后追加 `// CustomTool` 段，把 `CustomToolRegistry` 中缓存的 `ToolSpec` 渲染成 kotlin-like 签名

---

## 6. 目录结构索引（tools 包）

- `AgentToolManager.kt`
  - 总入口：执行、help、custom target 绑定、post hooks
- `ToolSpecification.kt`
  - 内置工具签名与 MAY_NAVIGATE_ACTIONS 列表
- `BasicToolCallExecutor.kt`
  - 基于 `ToolExecutor` 的反射/路由执行器
  - 也包含 Kotlin Script `eval`（慢且不安全，避免用于不可信输入）
- `CustomToolRegistry.kt`
  - 自定义域 executor 注册表 + prompt 可见 spec 缓存
- `ToolCallSpecificationProvider.kt`
  - executor 可实现的接口：提供 `List<ToolSpec>`
- `ToolCallSpecificationRenderer.kt`
  - 渲染规范字符串给 prompt
- `executors/*`
  - 内置执行器实现（driver/browser/fs/agent/system 等）
- `examples/*`
  - 工具机制使用示例（如果存在）

---

## 7. 推荐实践与注意事项

1) **domain 的职责边界**
   - `driver`：页面/DOM 交互
   - `browser`：多 tab、多窗口
   - `agent`：更高层语义能力（observe/act/extract/summarize）
   - `system`：元信息（help、工具能力描述）

2) **导航等待策略**
   - 如果新增一个“可能导致 URL 变化”的 driver 方法，记得把 method 名加入 `ToolSpecification.MAY_NAVIGATE_ACTIONS`。

3) **自定义工具上线最小 checklist**
   - 实现 `ToolExecutor`（建议继承 `AbstractToolExecutor`）并确保 `domain` 唯一
   - （可选）实现 `ToolCallSpecificationProvider` 或 `register(executor, specs)`
   - 在 agent 初始化时：
     - `CustomToolRegistry.instance.register(executor)`
     - `toolManager.registerCustomTarget(domain, targetInstance)`

4) **安全性**
   - `BasicToolCallExecutor.eval(...)` 使用 ScriptEngine 执行 Kotlin 表达式，避免对不可信输入开放。

---

## 8. FAQ

### Q1: 为什么自定义工具注册了 executor 还会报“no target object is available”？

因为执行需要 **两部分**：

- executor：负责把 `ToolCall` 翻译成实际方法调用
- target：实际承载业务方法/状态的对象实例

`AgentToolManager` 只会为内置域自动提供 target；自定义域必须手动 `registerCustomTarget(domain, target)`。

### Q2: 工具签名从哪里来？

内置：`ToolSpecification.TOOL_CALL_SPECIFICATION`。

自定义：来自 `CustomToolRegistry`（通过 `ToolCallSpecificationProvider` 自动采集，或手动 register 时注入）并由 `ToolCallSpecificationRenderer` 渲染。
