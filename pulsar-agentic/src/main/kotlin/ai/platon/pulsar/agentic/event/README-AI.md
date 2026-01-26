# AgentEventBus 实现说明

基于 PulsarEventBus 实现了 AgentEventBus，用于处理 Agent 相关事件。

## 架构

- **AgentEventBus**: 全局单例对象，管理 Agent 事件处理器和服务端事件处理器
- **AgentEventHandlers**: 处理 Agent 内部事件，限定在 pulsar-agentic 模块内
- **ServerSideAgentEventHandlers**: 负责将 Agent 内部事件发送到客户端，使用与 PulsarEventBus.serverSideEventHandlers 相同的机制

## 主要组件

### ServerSideAgentEvent

Agent 服务端事件数据结构：

```kotlin
data class ServerSideAgentEvent(
    val eventType: String,      // 事件类型（如 "onWillObserve", "onDidAct"）
    val eventPhase: String,     // 事件阶段（agent, inference, tool, mcp, skill）
    val agentId: String? = null,
    val message: String? = null,
    val timestamp: Instant = Instant.now(),
    val metadata: Map<String, Any?> = emptyMap()
)
```

### AgentEventBus

提供以下功能：

- `emitAgentEvent()` - 发出 agent 阶段事件
- `emitInferenceEvent()` - 发出推理阶段事件
- `emitToolEvent()` - 发出工具调用事件
- `emitMCPEvent()` - 发出 MCP 事件
- `emitSkillEvent()` - 发出技能事件
- `emitEvent()` - 发出自定义阶段事件
- `withServerSideAgentEventHandlers()` - 提供协程级别的处理器隔离

### DefaultAgentFlowEventHandlers 示例

```kotlin
class DefaultAgentFlowEventHandlers: AgentFlowEventHandlers {
    override val onWillObserve: ObserveEventHandler = ObserveEventHandler()
    override val onDidObserve: ObserveEventHandler = ObserveEventHandler()

    override val onWillAct: ActEventHandler = ActEventHandler()
    override val onDidAct: ActEventHandler = ActEventHandler()

    override val onInferenceWillObserve: ExecutionContextAgentStateEventHandler = ExecutionContextAgentStateEventHandler()
    override val onInferenceDidObserve: ExecutionContextAgentStateEventHandler = ExecutionContextAgentStateEventHandler()
    
    override fun chain(other: AgentFlowEventHandlers): AgentFlowEventHandlers { ... }
}
```

## 使用示例

```kotlin
// 设置全局事件处理器
AgentEventBus.agentEventHandlers = DefaultAgentEventHandlers()
AgentEventBus.serverSideAgentEventHandlers = DefaultServerSideAgentEventHandlers()

// 发出事件
AgentEventBus.emitAgentEvent("onWillObserve", "agent-123", "开始观察")

// 使用协程级别处理器隔离
AgentEventBus.withServerSideAgentEventHandlers(customHandlers) {
    // 在此块中的事件使用 customHandlers
    agent.observe(options)
}
```

## 注意事项

- AgenticEvents 定义了需要处理的事件类型
- 原有的 EventBus 通用事件机制保留，AgentEventBus 是对 Agent 相关事件的特化支持
- 事件发送是非阻塞的，使用后台协程范围执行
