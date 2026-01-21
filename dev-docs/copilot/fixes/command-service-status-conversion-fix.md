# CommandService Status Conversion Fix

## Problem
The CommandService had issues with status conversion from PageVisitStatus and AgentTaskStatus to CommandStatus. This caused:
1. Clients not receiving proper status updates via SSE streaming
2. Intermediate states (created, in_progress) not being properly tracked
3. Agent tasks always marked as "done" regardless of actual state

## Root Causes

### Issue 1: AgentTaskStatus → CommandStatus
The conversion was missing several critical fields:
- `event` field not transferred
- `processState` not transferred - always called `status.done()` making all tasks appear finished
- `lastModifiedTime` not transferred - breaking SSE update detection
- `finishTime` not transferred - losing completion timestamp

### Issue 2: PageVisitStatus → CommandStatus
The conversion had similar issues plus logic problems:
- `event` field not transferred
- `processState` not properly preserved
- `lastModifiedTime` not transferred - breaking SSE update detection
- `finishTime` not transferred - losing completion timestamp
- Called `refresh()` after conversion which could override the `processState`
- Used `addInstructResult()` which had side effects instead of direct assignment

## Solution

### Fixed AgentTaskStatus.toCommandStatus()
Now properly transfers all status fields:
```kotlin
private fun AgentTaskStatus.toCommandStatus(): CommandStatus {
    val status = CommandStatus(this.id)
    // Transfer all status fields
    status.statusCode = this.statusCode
    status.event = this.event
    status.processState = this.processState  // Now preserves actual state
    status.message = this.message
    status.lastModifiedTime = this.lastModifiedTime  // Enables SSE update detection
    status.finishTime = this.finishTime
    
    // Transfer agent-specific data
    status.agentHistory = this.agentHistory
    if (this.agentHistory != null) {
        val summary = this.agentHistory?.lastOrNull()?.summary ?: ""
        if (summary.isNotBlank()) {
            status.ensureCommandResult().summary = summary
        }
    }
    
    return status  // No longer calls status.done()
}
```

### Fixed PageVisitStatus.toCommandStatus()
Now properly transfers all status fields without side effects:
```kotlin
private fun PageVisitStatus.toCommandStatus(): CommandStatus {
    val status = CommandStatus(this.id)
    
    // Transfer all basic status fields
    status.statusCode = this.statusCode
    status.event = this.event
    status.processState = this.processState  // Preserved correctly
    status.pageStatusCode = this.pageStatusCode
    status.pageContentBytes = this.pageContentBytes
    status.message = this.message
    status.lastModifiedTime = this.lastModifiedTime  // Enables SSE update detection
    status.finishTime = this.finishTime
    
    // Transfer request if present
    status.request = this.request

    // Direct assignment instead of addInstructResult
    val restResults = instructResults.map { it.toRestInstructResult() }
    status.instructResults = restResults.toMutableList()

    // Map result data
    val visitResult = pageVisitResult
    if (visitResult != null) {
        val commandResult = status.ensureCommandResult()
        commandResult.pageSummary = visitResult.pageSummary
        commandResult.fields = visitResult.fields
        commandResult.xsqlResultSet = visitResult.xsqlResultSet
    }

    return status  // No longer calls refresh() or failed()
}
```

## Impact

### SSE Streaming Now Works Correctly
The `commandStatusFlow` in CommandService relies on `status.refreshed(lastModifiedTime)` to detect updates:
```kotlin
fun commandStatusFlow(id: String): Flow<CommandStatus> = flow {
    var lastModifiedTime = Instant.EPOCH
    do {
        delay(FLOW_POLLING_INTERVAL)
        val status = getStatus(id) ?: CommandStatus.notFound(id)
        if (status.refreshed(lastModifiedTime)) {  // Now works!
            emit(status)
            lastModifiedTime = status.lastModifiedTime
        }
        if (status.isDone) {
            emit(status)
        }
    } while (!status.isDone)
}
```

With `lastModifiedTime` now properly transferred, the streaming correctly:
1. Detects when status has been updated
2. Emits updates to SSE clients
3. Continues until `isDone` (based on `processState == "done"`)

### State Tracking Now Accurate
Clients can now correctly observe:
- `processState: "created"` - Task just created
- `processState: "in_progress"` - Task is running
- `processState: "done"` - Task completed

This enables proper progress tracking in UI and clients.

## Testing
Added `CommandStatusConversionTest` to verify:
1. All fields are properly transferred from PageVisitStatus
2. All fields are properly transferred from AgentTaskStatus
3. `processState` is preserved correctly (not forced to "done")
4. Intermediate states work correctly

## Files Changed
- `pulsar-rest/src/main/kotlin/ai/platon/pulsar/rest/api/service/CommandService.kt`
- `pulsar-rest/src/test/kotlin/ai/platon/pulsar/rest/api/service/CommandStatusConversionTest.kt` (new)
