
## Task: fix-mcp-server-nosuchmethoderror (2026-03-06)

### Summary
Fixed `NoSuchMethodError` in `TestMCPServerMock` and `TestMCPServerForPluginMock` test classes.

### Root Cause
Test source files (`TestMCPServerMock.kt`, `TestMCPServerForPluginMock.kt`) were deleted in a prior commit (`68b5c0667`) but stale compiled `.class` files remained in `target/test-classes`. These stale classes called `MockMCPServer.callTool(String)` (the old legacy API), but the new `MockMCPServer` only had `callTool(Map<String, Any>)`. Maven ran the stale class files instead of recompiling, causing `NoSuchMethodError`.

### Fix
1. Restored `TestMCPServerMock.kt` and `TestMCPServerForPluginMock.kt` source files (adapted from git history at commit `05d83f34a`) with camelCase test method names and `@DisplayName` annotations per project conventions.
2. Added a `callTool(JsonNode)` overload to `MockMCPServer` that converts a Jackson `JsonNode` to `Map<String, Any>` and delegates to the existing `callTool(Map<String, Any>)`, enabling tests that use Jackson's ObjectMapper API.

### Result
All 27 tests pass (was: 3 failures, 14 errors).

### Lessons Learned
- **Stale class files are a common trap**: When source files are deleted without `mvn clean`, stale `.class` files in `target/` continue to be executed by Maven's test runner, causing confusing `NoSuchMethodError` failures.
- **Check git history for deleted files**: `git log --all --diff-filter=D -- "*FileName*"` quickly finds when and why a source file was deleted, and `git show <commit>:path` restores content.
- **API compatibility**: When refactoring public APIs of shared test utilities (e.g., MockMCPServer), add overloads to support call sites rather than forcing all callers to update simultaneously.

## Task: remove-jvm-webdriver-support (2026-03-06)

### Summary
Removed JVM interop WebDriver support and the `examples/java-compatible` module.

### Changes
- Deleted `JvmWebDriver.kt`, `AbstractJvmWebDriver.kt`, and `JvmWebPageWebDriverEventHandler`.
- Removed `jvm()` API from `WebDriver` and corresponding generated/code-mirror specs.
- Removed `examples/java-compatible` from Maven module declarations and deleted the module sources/resources.
- Updated `README.md` and `README.zh.md` to state Java-compatible examples are removed and SDK/CLI paths should be used.

### Validation
- `mvnw.cmd -q -pl pulsar-core -am -D"skipTests" compile` ✅
- `mvnw.cmd -q -pl examples/browser4-examples -am -D"skipTests" compile` ✅

### Lessons Learned
- Removing compatibility layers should include contract mirrors/spec resources to keep generated tool metadata consistent with runtime APIs.
- Maven module removal requires updating both parent profile modules and aggregator module lists to avoid reactor drift.
