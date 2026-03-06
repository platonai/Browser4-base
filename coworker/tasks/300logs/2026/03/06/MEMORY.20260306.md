
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
