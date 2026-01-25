# Agentic Event System

## Overview

The pulsar-agentic module uses a centralized event system based on `DangerousEventBus` to provide observability and extensibility for agent operations. All event types are defined in the `AgenticEvents` object.

## Event Types

### PerceptiveAgent Events

Events emitted by `PerceptiveAgent` implementations during their execution:

| Event | Timing | Payload |
|-------|--------|---------|
| `RUN_WILL_EXECUTE` | Before run method | `action` (ActionOptions), `uuid` (UUID) |
| `RUN_DID_EXECUTE` | After run method | `action` (ActionOptions), `uuid` (UUID), `result` (ActResult), `stateHistory` (AgentHistory) |
| `OBSERVE_WILL_EXECUTE` | Before observe method | `options` (ObserveOptions), `uuid` (UUID) |
| `OBSERVE_DID_EXECUTE` | After observe method | `options` (ObserveOptions), `uuid` (UUID), `observeResults` (List<ObserveResult>), `actionDescription` (ActionDescription) |
| `ACT_WILL_EXECUTE` | Before act method | `action` (ActionOptions), `uuid` (UUID) |
| `ACT_DID_EXECUTE` | After act method | `action` (ActionOptions), `uuid` (UUID), `result` (ActResult) |
| `EXTRACT_WILL_EXECUTE` | Before extract method | `options` (ExtractOptions), `uuid` (UUID) |
| `EXTRACT_DID_EXECUTE` | After extract method | `options` (ExtractOptions), `uuid` (UUID), `result` (ExtractResult) |
| `SUMMARIZE_WILL_EXECUTE` | Before summarize method | `instruction` (String?), `selector` (String?), `uuid` (UUID) |
| `SUMMARIZE_DID_EXECUTE` | After summarize method | `instruction` (String?), `selector` (String?), `uuid` (UUID), `result` (String) |

### InferenceEngine Events

Events emitted during inference operations:

| Event | Timing | Payload |
|-------|--------|---------|
| `OBSERVE_WILL_EXECUTE` | Before observe inference | `messages` (AgentMessageList) |
| `OBSERVE_DID_EXECUTE` | After observe inference | `actionDescription` (ActionDescription) |
| `EXTRACT_WILL_EXECUTE` | Before extract inference | `params` (ExtractParams) |
| `EXTRACT_DID_EXECUTE` | After extract inference | `params` (ExtractParams), `result` (ObjectNode), `extractedNode` (ObjectNode), `metaNode` (ObjectNode) |
| `SUMMARIZE_WILL_EXECUTE` | Before summarize inference | `instruction` (String?), `messages` (AgentMessageList), `textContent` (String) |
| `SUMMARIZE_DID_EXECUTE` | After summarize inference | `instruction` (String?), `textContentLength` (Int), `result` (String), `tokenUsage` (TokenUsage) |

### ContextToAction Events

Events emitted during action generation:

| Event | Timing | Payload |
|-------|--------|---------|
| `GENERATE_WILL_EXECUTE` | Before generating action | `context` (ExecutionContext), `messages` (AgentMessageList) |
| `GENERATE_DID_EXECUTE` | After generating action | `context` (ExecutionContext), `messages` (AgentMessageList), `actionDescription` (ActionDescription) |

## Usage Examples

### Registering Event Handlers

```kotlin
import ai.platon.pulsar.agentic.event.AgenticEvents
import ai.platon.pulsar.common.event.DangerousEventBus

// Register a handler for agent actions
DangerousEventBus.register(AgenticEvents.PerceptiveAgent.ACT_WILL_EXECUTE) { payload ->
    val map = payload as? Map<String, Any?> ?: return@register null
    val action = map["action"]
    println("Starting action: $action")
    payload
}

DangerousEventBus.register(AgenticEvents.PerceptiveAgent.ACT_DID_EXECUTE) { payload ->
    val map = payload as? Map<String, Any?> ?: return@register null
    val result = map["result"]
    println("Action completed: $result")
    payload
}
```

### Performance Monitoring

```kotlin
import ai.platon.pulsar.agentic.event.AgenticEvents
import ai.platon.pulsar.common.event.DangerousEventBus
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

val executionTimes = ConcurrentHashMap<String, Long>()

// Track execution time
DangerousEventBus.register(AgenticEvents.InferenceEngine.EXTRACT_WILL_EXECUTE) { payload ->
    val map = payload as? Map<String, Any?> ?: return@register null
    val params = map["params"] as? ExtractParams
    executionTimes[params?.requestId ?: "unknown"] = System.currentTimeMillis()
    payload
}

DangerousEventBus.register(AgenticEvents.InferenceEngine.EXTRACT_DID_EXECUTE) { payload ->
    val map = payload as? Map<String, Any?> ?: return@register null
    val params = map["params"] as? ExtractParams
    val requestId = params?.requestId ?: "unknown"
    val startTime = executionTimes[requestId] ?: return@register null
    val duration = System.currentTimeMillis() - startTime
    println("Extract completed in ${duration}ms")
    payload
}
```

### Iterating All Events

```kotlin
import ai.platon.pulsar.agentic.event.AgenticEvents
import ai.platon.pulsar.common.event.DangerousEventBus

// Register a common handler for all events
AgenticEvents.getAllEventTypes().forEach { eventType ->
    DangerousEventBus.register(eventType) { payload ->
        println("Event: $eventType")
        payload
    }
}

// Cleanup
AgenticEvents.getAllEventTypes().forEach { eventType ->
    DangerousEventBus.unregister(eventType)
}
```

## Best Practices

1. **Always use constants**: Use `AgenticEvents.*` constants instead of hardcoded strings
2. **Type safety**: Cast payloads safely and check for null
3. **Return payload**: Event handlers should return the payload (or modified version) to allow chaining
4. **Cleanup**: Unregister event handlers when no longer needed
5. **Non-blocking**: Event handlers run asynchronously via coroutines, so keep them lightweight

## Testing

All events are tested in:
- `EventBusObservabilityTest.kt` - Unit tests for all event types
- `EventBusObservabilityExample.kt` - Example usage patterns

## Migration from Hardcoded Strings

If you have existing code using hardcoded event strings, migrate as follows:

| Old String | New Constant |
|------------|--------------|
| `"PerceptiveAgent.act.willExecute"` | `AgenticEvents.PerceptiveAgent.ACT_WILL_EXECUTE` |
| `"InferenceEngine.extract.willExecute"` | `AgenticEvents.InferenceEngine.EXTRACT_WILL_EXECUTE` |
| `"ContextToAction.generate.willExecute"` | `AgenticEvents.ContextToAction.GENERATE_WILL_EXECUTE` |

See `AgenticEvents.kt` for the complete list of available constants.
