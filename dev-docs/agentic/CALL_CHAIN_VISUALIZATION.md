# BrowserPerceptiveAgent.run() 调用链可视化

## 完整调用链路图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     BrowserPerceptiveAgent.run(task: String)                │
│                              [入口方法 - 简化版]                               │
└────────────────────────────────────┬────────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                  BrowserPerceptiveAgent.run(action: ActionOptions)          │
│                           [入口方法 - 完整版]                                  │
│  - 检查 isClosed                                                             │
│  - 创建协程上下文                                                             │
│  - withContext(agentScope.coroutineContext.minusKey(Job))                   │
└────────────────────────────────────┬────────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          resolveInCoroutine(action)                         │
│                            [核心执行协调器]                                    │
│  1. buildBaseExecutionContext() - 创建基础上下文                              │
│  2. addTrace("resolveStart") - 记录开始                                      │
│  3. withTimeout(effectiveTimeout) - 设置总超时                               │
│  4. resolveProblemWithRetry() - 执行主逻辑                                   │
│  5. generateFinalSummary() - 生成最终总结                                    │
│  6. addTrace("resolveDone") - 记录完成                                       │
└────────────────────────────────────┬────────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                    resolveProblemWithRetry(action, context)                 │
│                              [重试控制器]                                      │
│  循环 0..maxRetries 次:                                                      │
│    try {                                                                    │
│      result = doResolveProblem()                                            │
│      return result                                                          │
│    } catch (e) {                                                            │
│      cleanupPartialState()                                                  │
│      重建 baseContext                                                        │
│    }                                                                        │
└────────────────────────────────────┬────────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                  doResolveProblem(action, context, attempt)                 │
│                            [问题解决主循环]                                    │
│  1. initializeResolution() - 初始化解决会话                                   │
│  2. while (!isClosed && step < maxSteps) {                                  │
│       context = prepareStep()                                               │
│       result = step()                                                       │
│       if (result.shouldStop) break                                          │
│     }                                                                       │
│  3. buildFinalActResult() - 构建最终结果                                      │
└────────────────────────────────────┬────────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                  step(action, context, consecutiveNoOps)                    │
│                          [单步执行 - 观察行动循环]                              │
│                                                                             │
│  ┌──────────────┐         ┌──────────────┐         ┌──────────────┐        │
│  │   观察阶段    │  ────▶  │   决策阶段    │  ────▶  │   执行阶段    │        │
│  │  (Observe)   │         │  (Decide)    │         │   (Execute)  │        │
│  └──────────────┘         └──────────────┘         └──────────────┘        │
│         │                        │                        │                │
│         ▼                        ▼                        ▼                │
│  generateActions()         检查是否完成            executeToolCall()         │
│         │                        │                        │                │
│         │                        │                        │                │
│         └────────────────────────┴────────────────────────┘                │
│                                  │                                         │
│                           StepProcessingResult                             │
│                    (context, consecutiveNoOps, shouldStop)                 │
└────────────────────────────────────┬────────────────────────────────────────┘
                                     │
                    ┌────────────────┴────────────────┐
                    │                                 │
                    ▼                                 ▼
    ┌───────────────────────────┐       ┌───────────────────────────┐
    │   generateActions()       │       │  executeToolCall()        │
    │   [观察与动作生成]          │       │  [工具调用执行]             │
    └───────────┬───────────────┘       └───────────┬───────────────┘
                │                                   │
                ▼                                   ▼
    详见"观察阶段"流程图                    详见"执行阶段"流程图
```

---

## 观察阶段 (Observe Phase) 详细流程

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          generateActions(context)                           │
│                         [生成可执行动作描述]                                    │
└────────────────────────────────────┬────────────────────────────────────────┘
                                     │
                ┌────────────────────┼────────────────────┐
                │                    │                    │
                ▼                    ▼                    ▼
    ┌─────────────────┐  ┌──────────────────┐  ┌──────────────────┐
    │ 截图捕获         │  │ 提示词构建        │  │ LLM 推理         │
    │ captureScreen   │  │ buildResolve     │  │ cta.generate     │
    │ shotWithRetry   │  │ MessageListAll   │  │                  │
    └────────┬────────┘  └────────┬─────────┘  └────────┬─────────┘
             │                    │                     │
             │ screenshotB64      │ messages            │
             └────────────────────┼─────────────────────┘
                                  ▼
                    ┌──────────────────────────────┐
                    │  ContextToAction.generate    │
                    │  [LLM 调用核心]               │
                    │                              │
                    │  1. generateResponseRaw()    │
                    │     ├─ 系统提示词             │
                    │     ├─ 用户消息               │
                    │     └─ 截图（可选）           │
                    │                              │
                    │  2. chatModel.call()         │
                    │     └─ 调用外部 LLM API       │
                    │                              │
                    │  3. tta.modelResponse        │
                    │     ToActionDescription()    │
                    │     └─ 解析 LLM 响应          │
                    └──────────┬───────────────────┘
                               │
                               ▼
                    ┌──────────────────────────────┐
                    │    ActionDescription         │
                    │    [结构化动作描述]            │
                    │                              │
                    │  - instruction               │
                    │  - toolCall                  │
                    │  - observeElement            │
                    │  - isComplete                │
                    │  - summary                   │
                    │  - keyFindings               │
                    │  - nextSuggestions           │
                    └──────────────────────────────┘
```

### LLM 推理流程细节

```
chatModel.call(system, user, screenshot)
    │
    ├─ [提示词组成]
    │  ├─ 系统提示词: 角色定义、能力说明、工具列表
    │  ├─ 历史消息: 之前的观察和动作
    │  ├─ 当前状态: DOM 树、可交互元素、浏览器状态
    │  └─ 用户指令: 当前要完成的任务
    │
    ├─ [LLM 处理]
    │  └─ 返回 JSON 格式的工具调用或完成标志
    │
    └─ [响应解析]
       ├─ 成功: 提取 toolCall、locator、arguments
       ├─ 完成: isComplete=true, summary 总结
       └─ 失败: 记录错误，触发重试或断路器
```

---

## 执行阶段 (Execute Phase) 详细流程

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                executeToolCall(actionDescription, context)                  │
│                            [工具调用执行器]                                    │
└────────────────────────────────┬────────────────────────────────────────────┘
                                 │
                                 ▼
                    ┌─────────────────────┐
                    │  前置验证             │
                    │  (Pre-validation)   │
                    │                     │
                    │  if (enablePreAction│
                    │      Validation)    │
                    │    validate()       │
                    └─────────┬───────────┘
                              │
                    ┌─────────▼────────┐
                    │   验证通过?       │
                    └─────────┬────────┘
                              │
                    ┌─────────┴─────────┐
                    │ 否                │ 是
                    ▼                   ▼
        ┌───────────────────┐  ┌───────────────────────┐
        │ 记录验证失败       │  │ 执行工具调用           │
        │ 断路器计数+1       │  │ toolExecutor.execute() │
        │ return null       │  └───────────┬───────────┘
        └───────────────────┘              │
                                           ▼
                              ┌────────────────────────┐
                              │   工具执行             │
                              │   (Tool Execution)     │
                              │                        │
                              │   根据 toolCall.method  │
                              │   路由到对应执行器:      │
                              │                        │
                              │   - browser.*          │
                              │   - webdriver.*        │
                              │   - system.*           │
                              │   - fs.*               │
                              │   - agent.*            │
                              │   - skill.*            │
                              │   - mcp.*              │
                              └───────────┬────────────┘
                                          │
                              ┌───────────▼───────────┐
                              │  执行结果              │
                              │  (ToolCallResult)     │
                              │                       │
                              │  - success            │
                              │  - evaluate           │
                              │  - rawResult          │
                              └───────────┬───────────┘
                                          │
                              ┌───────────▼───────────┐
                              │  状态更新              │
                              │  (State Update)       │
                              │                       │
                              │  1. 更新 AgentState    │
                              │  2. 添加到 history     │
                              │  3. 更新 TODO          │
                              │  4. 记录 trace         │
                              │  5. 更新性能指标        │
                              └───────────┬───────────┘
                                          │
                                          ▼
                              ┌────────────────────────┐
                              │   DetailedActResult    │
                              │   [详细执行结果]         │
                              │                        │
                              │   - actionDescription  │
                              │   - toolCallResult     │
                              │   - success            │
                              │   - summary            │
                              └────────────────────────┘
```

### 工具执行路由表

```
toolCall.method          →    Executor                →    实际操作
─────────────────────────────────────────────────────────────────────
browser.navigate         →    BrowserToolExecutor     →    driver.navigateTo()
browser.click            →    BrowserToolExecutor     →    element.click()
browser.input            →    BrowserToolExecutor     →    element.sendKeys()
browser.scroll           →    BrowserToolExecutor     →    driver.executeScript()

webdriver.screenshot     →    WebDriverExToolExecutor →    driver.takeScreenshot()
webdriver.getText        →    WebDriverExToolExecutor →    element.getText()

system.sleep             →    SystemToolExecutor      →    Thread.sleep()
system.getenv            →    SystemToolExecutor      →    System.getenv()

fs.write                 →    FileSystemToolExecutor  →    Files.writeString()
fs.read                  →    FileSystemToolExecutor  →    Files.readString()

agent.done               →    AgentToolExecutor       →    标记完成
agent.extract            →    AgentToolExecutor       →    调用 extract()

skill.run                →    SkillToolExecutor       →    执行自定义技能

mcp.*                    →    MCPToolExecutor         →    转发到 MCP 服务器
```

---

## 状态管理流程

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        AgentStateManager                                    │
│                         [状态管理中心]                                         │
└────────────────────────────────────┬────────────────────────────────────────┘
                                     │
          ┌──────────────────────────┼──────────────────────────┐
          │                          │                          │
          ▼                          ▼                          ▼
    ┌──────────┐              ┌──────────┐              ┌──────────┐
    │ History  │              │ Context  │              │  Trace   │
    │ 操作历史  │              │ 执行上下文 │              │ 过程追踪  │
    └──────────┘              └──────────┘              └──────────┘
         │                          │                          │
         │                          │                          │
         ▼                          ▼                          ▼
    stateHistory              contexts 列表              processTrace
    ├─ states: List<          ├─ _baseContext          ├─ event
    │  AgentState>            ├─ _activeContext        ├─ timestamp
    │  └─ 每步的状态            └─ 上下文栈              ├─ sessionId
    │                                                  └─ metadata
    └─ 用于历史回顾                                      └─ 用于调试追踪

每个 AgentState 包含:
├─ step: Int                    // 步骤号
├─ instruction: String          // 用户指令
├─ actionDescription            // 本步动作描述
├─ toolCall                     // 执行的工具调用
├─ toolCallResult               // 工具执行结果
├─ browserUseState              // 浏览器状态快照
│  ├─ domState                  // DOM 树
│  ├─ browserState              // 浏览器元信息
│  └─ tabState                  // 标签页状态
├─ summary                      // 步骤总结
├─ isComplete                   // 是否完成任务
└─ prevState                    // 前一步状态引用
```

---

## 错误处理与恢复机制

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          错误处理层次结构                                       │
└─────────────────────────────────────────────────────────────────────────────┘

Level 1: 顶层超时控制 (resolveInCoroutine)
    │
    ├─ withTimeout(effectiveTimeout)
    │  └─ 捕获: TimeoutCancellationException
    │     └─ 返回: 超时的 ActResult
    │
    └─ 捕获: CancellationException
       └─ 日志记录 "Cancelled due to cancellation"

Level 2: 重试控制 (resolveProblemWithRetry)
    │
    ├─ for (0..maxRetries)
    │  ├─ 尝试: doResolveProblem()
    │  └─ 捕获: Exception
    │     ├─ 日志记录错误
    │     ├─ cleanupPartialState()
    │     └─ 重建 baseContext
    │
    └─ 所有重试失败
       └─ 返回: 包含错误信息的 ActResult

Level 3: 断路器保护 (CircuitBreaker)
    │
    ├─ recordFailure(type)
    │  ├─ LLM_FAILURE: 5 次后断开
    │  ├─ VALIDATION_FAILURE: 8 次后断开
    │  └─ EXECUTION_FAILURE: 3 次后断开
    │
    ├─ 达到阈值
    │  └─ throw CircuitBreakerTrippedException
    │
    └─ recordSuccess(type)
       └─ 重置对应计数器

Level 4: 单步错误处理 (step, executeToolCall)
    │
    ├─ generateActions()
    │  └─ 捕获: Exception
    │     ├─ handleObserveException()
    │     ├─ 断路器计数+1
    │     └─ 返回: 包含异常的 ActionDescription
    │
    └─ executeToolCall()
       └─ 捕获: Exception
          ├─ 断路器计数+1
          ├─ 日志记录
          └─ return null (表示执行失败)

Level 5: 工具执行错误 (toolExecutor.execute)
    │
    └─ 各工具执行器内部的错误处理
       ├─ 验证参数
       ├─ 执行操作
       └─ 返回 ToolCallResult(success, evaluate, error)
```

### 错误分类与处理策略

```
PerceptiveAgentError (sealed class)
    │
    ├─ TransientError [可重试]
    │  ├─ 网络连接失败
    │  ├─ 暂时性超时
    │  └─ 资源暂时不可用
    │  → 策略: 指数退避重试
    │
    ├─ TimeoutError [可重试]
    │  ├─ 操作超时
    │  └─ 网络超时
    │  → 策略: 增加超时时间重试
    │
    ├─ ValidationError [不可重试]
    │  ├─ 参数验证失败
    │  └─ 数据格式错误
    │  → 策略: 立即失败，记录日志
    │
    ├─ ResourceExhaustedError [不可重试]
    │  ├─ 内存耗尽
    │  └─ 配额用尽
    │  → 策略: 清理资源，可能需要人工介入
    │
    └─ PermanentError [不可重试]
       ├─ 断路器触发
       ├─ 状态错误
       └─ 配置错误
       → 策略: 立即失败，需要修复问题
```

---

## 性能关键路径分析

```
关键性能瓶颈点:

1. LLM 推理 [最耗时]
   └─ chatModel.call()
      └─ 通常 2-30 秒
      └─ 优化: 缓存、批处理、使用更快的模型

2. 页面截图 [次耗时]
   └─ driver.captureScreenshot()
      └─ 通常 100-500ms
      └─ 优化: 降低分辨率、按需截图、异步处理

3. DOM 树序列化 [中等]
   └─ domState.microTree.toJSON()
      └─ 通常 50-200ms
      └─ 优化: 增量更新、压缩、缓存

4. 状态哈希计算 [轻量]
   └─ calculatePageStateHash()
      └─ 通常 10-50ms
      └─ 优化: 缓存哈希、只哈希关键部分

5. 日志写入 [轻量]
   └─ logger.info() + 文件 I/O
      └─ 通常 5-20ms
      └─ 优化: 异步日志、批量写入
```

### 典型执行时间分布

```
单步执行 (step) 总时间: ~5-35 秒

├─ prepareStep()              ~100ms     (2%)
├─ generateActions()          ~5-30s     (90%)
│  ├─ captureScreenshot()     ~200ms     (4%)
│  ├─ buildMessages()         ~50ms      (1%)
│  └─ cta.generate()          ~5-30s     (85%)
│     └─ chatModel.call()     ~5-30s     (LLM 推理)
├─ executeToolCall()          ~100-500ms (5%)
│  ├─ validate()              ~10ms
│  ├─ toolExecutor.execute()  ~50-400ms
│  └─ updateState()           ~50ms
└─ updateTodo()               ~20ms      (1%)
└─ delay()                    ~50-200ms  (2%)

完整 resolve 会话时间: 数十秒到数分钟
└─ 取决于: maxSteps, LLM 响应时间, 任务复杂度
```

---

## 并发与资源管理

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          协程与作用域结构                                       │
└─────────────────────────────────────────────────────────────────────────────┘

外部调用者 Scope
    │
    └─> BrowserPerceptiveAgent
        │
        ├─ agentJob: SupervisorJob
        │  └─ 专用 Job，可独立取消
        │
        ├─ agentScope: CoroutineScope
        │  └─ Dispatchers.Default + agentJob
        │
        └─ 方法执行
           │
           ├─ run(action)
           │  └─ withContext(agentScope.ctx.minusKey(Job))
           │     └─ 继承调用者取消，使用 agent 调度器
           │
           ├─ act(action)
           │  └─ withContext(agentScope.ctx.minusKey(Job))
           │
           └─ observe(options)
              └─ withContext(agentScope.ctx.minusKey(Job))

资源清理流程:

close() 调用时:
    │
    ├─ closed.compareAndSet(false, true)
    │
    ├─ agentJob.cancel(CancellationException(...))
    │  └─ 取消所有在 agentScope 中运行的协程
    │
    └─ 所有方法入口检查 isClosed
       └─ 快速返回默认值

问题:
    └─ close() 后 addTrace() 可能在已取消的作用域执行
    └─ 建议: 先记录关闭事件，再取消 Job
```

### 内存管理与清理

```
内存占用主要来源:

1. stateHistory [增长]
   └─ 每步一个 AgentState
   └─ 清理: performMemoryCleanup() 每 50 步触发
   └─ 保留: maxHistorySize (默认 100)

2. stepExecutionTimes [泄漏风险 ⚠️]
   └─ ConcurrentHashMap<Int, Long>
   └─ 清理: 无 ❌
   └─ 建议: 添加到 performMemoryCleanup()

3. contexts [泄漏风险 ⚠️]
   └─ MutableList<ExecutionContext>
   └─ 清理: 无 ❌
   └─ 建议: 使用滑动窗口或限制大小

4. processTrace [泄漏风险 ⚠️]
   └─ MutableList<ProcessTrace>
   └─ 清理: 无 ❌
   └─ 建议: 限制大小或定期持久化并清理

5. screenshotB64 [大对象]
   └─ Base64 字符串，可能数 MB
   └─ 清理: 步骤结束后应清除
   └─ 建议: 使用后立即设为 null

清理触发条件:
├─ 定期: step % memoryCleanupIntervalSteps == 0
├─ 主动: close() 调用时
└─ 建议: 添加内存压力触发机制
```

---

## 配置系统概览

```
AgentConfig (30+ 配置项)
    │
    ├─ 执行控制
    │  ├─ maxSteps: 100
    │  ├─ maxRetries: 3
    │  └─ consecutiveNoOpLimit: 5
    │
    ├─ 超时配置
    │  ├─ actTimeoutMs: 10分钟
    │  ├─ llmInferenceTimeoutMs: 10分钟
    │  ├─ resolveTimeoutMs: 24小时
    │  ├─ actionGenerationTimeoutMs: 30秒
    │  ├─ screenshotCaptureTimeoutMs: 5秒
    │  └─ domSettleTimeoutMs: 5秒
    │
    ├─ 重试配置
    │  ├─ baseRetryDelayMs: 1000
    │  └─ maxRetryDelayMs: 30000
    │
    ├─ 断路器配置
    │  ├─ maxConsecutiveLLMFailures: 5
    │  └─ maxConsecutiveValidationFailures: 8
    │
    ├─ 内存管理
    │  ├─ memoryCleanupIntervalSteps: 50
    │  └─ maxHistorySize: 100
    │
    ├─ 检查点配置
    │  ├─ enableCheckpointing: false
    │  ├─ checkpointIntervalSteps: 10
    │  └─ maxCheckpointsPerSession: 5
    │
    ├─ TODO 集成
    │  ├─ enableTodoWrites: true
    │  ├─ todoPlanWithLLM: true
    │  ├─ todoWriteProgressEveryStep: true
    │  └─ todoProgressWriteEveryNSteps: 1
    │
    ├─ 观测配置
    │  ├─ enableStructuredLogging: false
    │  ├─ logInferenceToFile: true
    │  ├─ enableDebugMode: false
    │  ├─ enablePerformanceMetrics: true
    │  └─ screenshotEveryNSteps: 1
    │
    ├─ 安全策略
    │  ├─ allowLocalhost: false
    │  ├─ allowedPorts: Set(80, 443, ...)
    │  ├─ maxSelectorLength: 1000
    │  └─ denyUnknownActions: false
    │
    └─ 优化开关
       ├─ enableAdaptiveDelays: true
       ├─ enablePreActionValidation: true
       └─ maxResultsToTry: 3

建议: 按功能域拆分为多个配置类
```

---

## 监控与可观测性

```
日志层次:
    │
    ├─ 结构化日志 (StructuredAgentLogger)
    │  └─ 格式: emoji + sid + 结构化字段
    │  └─ 示例: "🚀 agent.start sid=abc12345 step=1 url=https://..."
    │
    ├─ 过程追踪 (ProcessTrace)
    │  └─ 记录所有事件，包括元信息
    │  └─ writeProcessTrace() 持久化到文件
    │
    ├─ 性能指标 (PerformanceMetrics)
    │  └─ totalSteps, successfulActions, failedActions
    │  └─ 问题: 很多字段未更新 ⚠️
    │
    └─ 检查点 (AgentCheckpoint)
       └─ 保存会话状态，支持恢复
       └─ 默认关闭，需配置启用

关键监控点:
    │
    ├─ 步骤执行时间 (stepExecutionTimes)
    ├─ 连续失败计数 (consecutiveFailureCounter)
    ├─ 断路器状态 (circuitBreaker.getFailureCounts())
    ├─ 内存使用 (stateHistory.size, contexts.size)
    └─ LLM 调用统计 (需要添加)

建议增强:
    ├─ 集成 OpenTelemetry 分布式追踪
    ├─ 导出指标到 Prometheus
    ├─ 添加更多业务指标
    └─ 实时监控告警
```

---

## 总结

**调用链深度**: 5-6 层  
**关键性能瓶颈**: LLM 推理（90% 时间）  
**主要风险点**: 内存泄漏、并发安全、资源清理  
**架构优点**: 清晰的职责分离、完善的错误处理  
**改进方向**: 简化状态管理、增强测试、优化资源管理

详细问题和建议请参阅:
- [完整审查报告](./CODE_REVIEW_BrowserPerceptiveAgent.md)
- [中文摘要](./CODE_REVIEW_SUMMARY_zh.md)
