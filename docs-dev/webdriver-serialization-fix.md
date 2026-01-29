# WebDriver Serialization Fix

**Date:** 2026-01-29  
**Branch:** copilot/fix-webdriver-parallel-operations  
**Issue:** WebDriver并行操作问题修复

## Problem Statement

The requirement stated: "ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver 的所有方法不得并行操作，只能串行操作" (All methods of WebDriver must not execute in parallel, only serially).

The REST API controllers in `pulsar-rest/src/main/kotlin/ai/platon/pulsar/rest/openapi` were allowing concurrent HTTP requests to call WebDriver methods in parallel on the same session, violating this constraint.

## Root Cause

When multiple concurrent HTTP requests arrived for the same session, Spring's controller suspend functions would call WebDriver methods directly without any synchronization. Since Spring handles each request in a separate coroutine, these could execute simultaneously, causing potential race conditions and violations of the serial execution requirement.

## Solution

Added a per-session `Mutex` to enforce serial execution of all WebDriver operations within a session:

1. **SessionManager.ManagedSession** - Added `driverMutex: Mutex = Mutex()` field
2. **All Controller Methods** - Wrapped driver method calls with `driverMutex.withLock { }`

### Architecture

```
HTTP Request 1 (Session A) → Controller → mutex.withLock { driver.method() } → WebDriver
HTTP Request 2 (Session A) → Controller → [WAITS for mutex] → WebDriver
HTTP Request 3 (Session B) → Controller → mutex.withLock { driver.method() } → WebDriver (parallel)
```

## Files Modified

### Core Implementation
- `pulsar-rest/src/main/kotlin/ai/platon/pulsar/rest/openapi/service/SessionManager.kt`
  - Added `driverMutex: Mutex` to ManagedSession

### Controllers (All WebDriver calls wrapped with mutex.withLock)
- `NavigationController.kt` - 6 operations
- `SelectorController.kt` - 18 operations  
- `ScrollController.kt` - 7 operations
- `ElementController.kt` - 4 operations
- `ScriptController.kt` - 2 operations

### Testing
- `WebDriverSerializationTest.kt` - 4 comprehensive unit tests

## Testing Strategy

Created unit tests to verify:
1. **Serial execution within session** - Max 1 concurrent operation per session
2. **Parallel execution across sessions** - Different sessions can run concurrently
3. **Race condition prevention** - Mutex prevents lost updates
4. **Proper queuing** - Operations execute in order

All tests pass successfully.

## Performance Impact

- **Within a session:** Operations are serialized (required by constraint)
- **Across sessions:** No impact - different sessions can still operate in parallel
- **Overhead:** Minimal - Kotlin's Mutex is non-blocking and uses coroutine suspension

## Example Usage Pattern

```kotlin
suspend fun someControllerMethod(sessionId: String): ResponseEntity<Any> {
    val session = sessionManager.getSession(sessionId) ?: return notFound()
    
    return try {
        // All WebDriver operations must be wrapped with withLock
        val result = session.driverMutex.withLock {
            val driver = session.pulsarSession.getOrCreateBoundDriver()
            driver.someMethod() // This executes serially within the session
        }
        ResponseEntity.ok(result)
    } catch (e: Exception) {
        handleError(e)
    }
}
```

## Code Review Feedback Addressed

1. ✅ Removed unnecessary synchronized blocks in tests (mutex already ensures serialization)
2. ✅ Replaced timing-based test assertions with concurrent execution counting
3. ✅ Made tests more robust and less flaky on slow CI systems

## Security Analysis

✅ CodeQL security check: No vulnerabilities detected

## Verification

- ✅ Build: Successful
- ✅ Tests: All 4 unit tests pass
- ✅ Code Review: All comments addressed
- ✅ Security: No vulnerabilities

## Key Takeaways

1. **Per-session mutex** is the correct granularity - allows parallelism across sessions while ensuring serial execution within a session
2. **Kotlin coroutine Mutex** (`kotlinx.coroutines.sync.Mutex`) is the appropriate synchronization primitive for suspend functions
3. **withLock** extension function provides exception-safe lock acquisition and release
4. **Testing concurrent code** requires careful design - avoid timing-based assertions, use execution counting instead

## Related Documentation

- WebDriver Interface: `pulsar-core/pulsar-skeleton/src/main/kotlin/ai/platon/pulsar/skeleton/crawl/fetch/driver/WebDriver.kt`
- Kotlin Coroutines Mutex: https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.sync/-mutex/
