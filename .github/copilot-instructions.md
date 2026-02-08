# AI Copilot Usage and Authoring Guide (v2026-02-08)

> **Note**: This guide is specifically for GitHub Copilot. For Claude AI, see [CLAUDE.md](../CLAUDE.md) in the root directory.

---

<!-- TOC -->
**Table of Contents**
- [0) Quick Start (the 3 most common commands)](#0-quick-start-the-3-most-common-commands)
- [1) Overview](#1-overview)
- [2) Environment and Build](#2-environment-and-build)
- [3) Project Essentials](#3-project-essentials)
- [4) Run and Configuration](#4-run-and-configuration)
- [5) Logging and Performance](#5-logging-and-performance)
- [6) Code and Docs](#6-code-and-docs)
- [7) Testing Guidelines](#7-testing-guidelines)
- [8) Common Commands Cheat Sheet](#8-common-commands-cheat-sheet)
- [9) Definition of Done (DoD) for PR/Changes](#9-definition-of-done-dod-for-prchanges)
- [10) Failures and Troubleshooting](#10-failures-and-troubleshooting)
- [11) Minimal Test Policy](#11-minimal-test-policy)
- [12) AI Copilot Specific Guidance](#12-ai-copilot-specific-guidance)
<!-- /TOC -->

---

## 0) Quick Start (the 3 most common commands)

> Goal: finish build/tests as fast as possible **without guessing commands or hitting platform pitfalls**.

- First build (skip tests)
    - Windows (PowerShell):
      ```powershell
      .\mvnw.cmd -q -DskipTests
      ```
    - Windows (cmd):
      ```bat
      mvnw.cmd -q -DskipTests
      ```
    - Linux/macOS:
      ```bash
      chmod +x mvnw
      ./mvnw -q -DskipTests
      ```

- Verify only core module unit tests (recommended for fast local regression)
    - Windows (PowerShell):
      ```powershell
      .\mvnw.cmd -pl pulsar-core -am test -D"surefire.failIfNoSpecifiedTests=false"
      ```
    - Windows (cmd):
      ```bat
      mvnw.cmd -pl pulsar-core -am test -D"surefire.failIfNoSpecifiedTests=false"
      ```
    - Linux/macOS:
      ```bash
      ./mvnw -pl pulsar-core -am test -Dsurefire.failIfNoSpecifiedTests=false
      ```

- Recommended: use the repo scripts (auto-select platform and common parameter conventions)
    - Windows (PowerShell): `bin/build.ps1 [-test]`
    - Linux/macOS: `bin/build.sh [-test]`

### Cross-platform environment detection (optional)

- bash/zsh:
  ```bash
  if [[ "$OS" == "Windows_NT" ]]; then
    cmd /c mvnw.cmd -q -DskipTests
  else
    ./mvnw -q -DskipTests
  fi
  ```

- PowerShell:
  ```powershell
  if ($IsWindows) { .\mvnw.cmd -q -D"skipTests" } else { ./mvnw -q -DskipTests }
  ```

## 1) Overview

- Repo: multi-module Maven (**always use the root Maven Wrapper: `./mvnw` / `mvnw.cmd`**)
- Language: Kotlin-first, Java-compatible
- Principles: minimal changes, preserve style, clear logs, automatic validation and tests

## 2) Environment and Build

- Maven Wrapper (mandatory convention)
    - Windows: `mvnw.cmd ...` (in PowerShell, prefer `./mvnw.cmd ...`)
    - Linux/macOS: `./mvnw ...`

- Common build commands
    - Windows (cmd.exe):
        - `mvnw.cmd -q -DskipTests`
        - `mvnw.cmd -pl pulsar-core -am test -D"surefire.failIfNoSpecifiedTests=false"`
    - Windows (PowerShell):
        - `.\mvnw.cmd -q -D"skipTests"`
        - `.\mvnw.cmd -pl pulsar-core -am test -D"surefire.failIfNoSpecifiedTests=false"`
    - Linux/macOS:
        - `./mvnw -q -DskipTests`
        - `./mvnw -pl pulsar-core -am test -Dsurefire.failIfNoSpecifiedTests=false`

- Recommended scripts:
    - Windows: `bin/build.ps1 [-test]`
    - Linux/macOS: `bin/build.sh [-test]`

- Important note (Windows parameter escaping)
    - On Windows, it is **recommended to quote `-D` parameters**, e.g. `-D"dot.separated.parameter=quoted"`

## 3) Project Essentials

Browser4 is a lightning-fast, coroutine-safe browser engine for AI agents with:
- **Browser Agents**: Autonomous agents that reason, plan, and execute tasks
- **Browser Automation**: High-performance workflows, navigation, and extraction
- **Machine Learning**: Learns field structures without consuming tokens
- **Extreme Performance**: 100k-200k complex page visits per machine per day

### Module Overview

- Core API: `ai/platon/pulsar/core/api/API.kt` (key: `WebDriver`, `PulsarSession`)
- Agents: `PulsarSession` -> `AgenticSession` -> `BrowserPerceptiveAgent`
- Modules:
    - `pulsar-core`: core engine (sessions, scheduling, DOM, browser control)
    - `pulsar-agentic`: agent implementation, MCP, skill registration
    - `pulsar-rest`: Spring Boot REST / command entry
    - `sdks/*`: client SDKs
    - `browser4/*`: product aggregation (SPA & packaging)
    - Tests: `pulsar-tests` and `pulsar-tests-common`
- Sessions: `AgenticContexts.createSession()`
- Load parameters: use `LoadOptions` to parse CLI-style parameters embedded in the URL
- Browser automation: see `ai.platon.pulsar.browser`; API: `WebDriver`; implementation details: `PageHandler`, `ClickableDOM`
- MCP tools: see the `MCPTool` interface and `MCPToolExecutor`
- Skill registration: see `SkillRegistry`
- Event bus: `EventBus` (general, for decoupling monitoring and extensions) and `PulsarEventBus` (specialized, for full-page lifecycle notifications)
- Exception retries: for Chrome CDP RPC, reuse existing retry utilities to avoid log storms

## 4) Run and Configuration

- Application port: default 8182
- Config overrides: use layered `application*.properties`; avoid hard-coding defaults in code
- References: `docs/config.md` and `docs/rest-api-examples.md`

## 5) Logging and Performance

- Logging placeholders: `logger.info("Task {} finished in {} ms", taskId, cost)` (avoid string concatenation)

## 6) Code and Docs

- Kotlin: immutable `data class`, explicit return types, null-safety (`require`/`check`/`?:`)
- Public APIs must have KDoc: summary / params / returns / exceptions
- Store AI generated task docs in `docs-dev`

> KDoc template example:
```kotlin
/**
 * Loads a page and returns its parsed snapshot.
 *
 * @param url The target URL to load.
 * @param options Load options parsed from CLI-like URL params.
 * @return Parsed page snapshot.
 * @throws IllegalArgumentException if url is blank.
 */
fun load(url: String, options: LoadOptions): PageSnapshot {
    require(url.isNotBlank()) { "url must not be blank" }
    // ...existing code...
}
```

## 7) Testing Guidelines

### Test Location
- Module unit tests: `src/test/kotlin/...`
- Centralized integration/E2E: `pulsar-tests/`
- Shared utilities: `pulsar-tests-common/`

### Naming Conventions
- Unit tests: `<ClassName>Test.kt`
- Integration tests: `<ClassName>IT.kt`
- E2E tests: `<ClassName>E2ETest.kt`
- **Method names: use camelCase (NOT backtick naming)**
    - ✅ `testUserLoginWithValidCredentials()` + `@DisplayName("test user login with valid credentials")`
    - ❌ `` `test user login with valid credentials` ``

### Test Performance Targets
- Unit tests: <100ms
- Integration tests: <5s
- E2E tests: <30s

### Coverage Targets
- Global: ≥70%
- Core logic: ≥80%
- Utilities: ≥90%
- Controllers: ≥85%

### Test Tags (JUnit 5)
`IntegrationTest`, `E2ETest`, `ExternalServiceTest`, `Slow`, `HeavyTest`, `SmokeTest`, `BenchmarkTest`

## 8) Common Commands Cheat Sheet (copy by platform)

- Build (skip tests)
    - Windows (PowerShell): `.\mvnw.cmd -q -D"skipTests"`
    - Windows (cmd): `mvnw.cmd -q -DskipTests`
    - Linux/macOS: `./mvnw -q -DskipTests`

- Core module unit tests
    - Windows (PowerShell): `.\mvnw.cmd -pl pulsar-core -am test -D"surefire.failIfNoSpecifiedTests=false"`
    - Windows (cmd): `mvnw.cmd -pl pulsar-core -am test -D"surefire.failIfNoSpecifiedTests=false"`
    - Linux/macOS: `./mvnw -pl pulsar-core -am test -Dsurefire.failIfNoSpecifiedTests=false`

- SDKs module tests (skip dependencies)
    - Windows (PowerShell): `.\mvnw.cmd -Psdk -pl sdks\kotlin-sdk-tests test`
    - Windows (cmd): `mvnw.cmd -Psdk -pl sdks\kotlin-sdk-tests test`
    - Linux/macOS: `./mvnw -Psdk -pl sdks\kotlin-sdk-tests test`

- Recommended scripts
    - Windows: `bin/build.ps1 [-test]`
    - Linux/macOS: `bin/build.sh [-test]`

## 9) Definition of Done (DoD) for PR/Changes

- Build and relevant tests pass; no new noisy logs/warnings
- New/changed logic: main path + at least 1 boundary case
- Do not commit secrets/private endpoints; inputs are validated
- No arbitrary version drift (follow parent BOM)
- Public behavior/config changes are documented
- Potential performance impact (>≈5%) is evaluated or benchmarked

## 10) Failures and Troubleshooting

### Browser and CDP Issues

- **Browser/CDP**: prefer reproducing with the heavy suite in `pulsar-tests`
- **CDP Connection Failures**: 
  - Check Chrome version compatibility
  - Verify no other Chrome instances are interfering
  - Check system resources (memory, file descriptors)
- **Timeout Issues**: Increase timeout in LoadOptions: `-timeout 60s`
- **Headless Mode Problems**: Switch to GUI mode for debugging: `browser.display.mode=GUI`

### Agent and Session Issues

- **Agent/privacy context**: keep handlers idempotent and thread-safe
- **Session Leaks**: Always use `use` or try-finally to close sessions
- **Coroutine Issues**: Verify proper `CoroutineScope` and structured concurrency

### Build and Test Issues

- **Dependency Conflicts**: Run `./mvnw dependency:tree` to diagnose
- **Test Failures**: Isolate with `-Dtest=ClassName#methodName`
- **Flaky Tests**: Check for timing issues, add proper waits
- **Resource Cleanup**: Ensure test fixtures properly close resources

### Logging and Diagnostics

- **Log triage**: follow structured fields for easier filtering
- **Enable detailed logging**: `logger.setLevel(Level.TRACE)` for specific package
- **Page diagnostics**: Use `-diagnose` LoadOption for detailed page loading info
- **CDP tracing**: Set `browser.tracing=true` for Chrome DevTools Protocol details

### Common Quick Fixes
- Linux/macOS: `mvnw` not executable → `chmod +x mvnw`
- JDK version mismatch → ensure the required JDK version (prefer local `JAVA_HOME`)
- Windows parameter escaping → `-D"key.with.dots=value"` (quote dot-separated keys)
- Port in use (default 8182) → override `server.port` or use `application-local.properties`
- Log storms (CDP retries) → reuse existing retry utilities and lower log level; don’t build strings inside loops

---

## 11) Minimal Test Policy (default)

To keep iteration fast, **don’t run full test suites by default**.

- Default: **compile / validate dependencies (skip tests)**
- Then: run the **smallest relevant** test scope (single module/class) when logic changes
- Upgrade scope only when risk increases (cross-module, public API/DTO/serialization, Spring wiring, dependency bumps,
  concurrency/I/O, browser/CDP lifecycle)

Details and rationale: `docs-dev/copilot/minimal-test-policy.md`.

**Windows (PowerShell) quick commands**

```powershell
# Minimal compile (default)
.\mvnw.cmd -q -D"skipTests" test

# Fast regression (recommended)
.\mvnw.cmd -pl pulsar-core -am test -D"surefire.failIfNoSpecifiedTests=false"
```

---

## 12) AI Copilot Specific Guidance

### When to Use AI Suggestions

**Always Accept for:**
- Repetitive code patterns (getters/setters, builders)
- Standard boilerplate (test setup, logging statements)
- Common Kotlin idioms (data classes, extension functions)
- Known patterns from the codebase (LoadOptions parsing, session creation)

**Review Carefully for:**
- Complex business logic (browser automation, page extraction)
- Security-sensitive code (authentication, API keys)
- Performance-critical paths (coroutine handling, CDP operations)
- Public API contracts (breaking changes)

### Browser4-Specific Patterns

**Browser Automation:**
```kotlin
// Use WebDriver for browser control
val driver = session.driver()
driver.click("button.submit")
driver.waitFor("div.results")

// Use LoadOptions for page loading
val page = session.load(url, "-expires 1d -refresh -parse")
```

**Agent Tasks:**
```kotlin
// Create and use agents
val agent = AgenticContexts.getOrCreateAgent()
val result = agent.run("Navigate to example.com and extract all product prices")
```

**MCP Tools:**
```kotlin
// Register custom MCP tools
val tool = object : MCPTool {
    override val name = "custom_tool"
    override fun execute(params: Map<String, Any>): String { /* ... */ }
}
skillRegistry.register(tool)
```

### Common Refactoring Tasks

1. **Converting callbacks to coroutines:**
   ```kotlin
   // Before: callback-based
   fun loadPage(url: String, callback: (Page) -> Unit)
   
   // After: suspend function
   suspend fun loadPage(url: String): Page
   ```

2. **Adding null-safety:**
   ```kotlin
   // Add require/check for validation
   fun load(url: String, options: LoadOptions): Page {
       require(url.isNotBlank()) { "URL must not be blank" }
       // ...
   }
   ```

3. **Improving logging:**
   ```kotlin
   // Use placeholders, not string concatenation
   logger.info("Loading {} with options {}", url, options)
   ```

### Debugging Tips

**Chrome DevTools Protocol (CDP) Issues:**
- Enable detailed logging: `logger.setLevel(Level.TRACE)`
- Check CDP command failures in logs
- Verify Chrome version compatibility

**Coroutine Issues:**
- Use `runBlocking` for test contexts
- Ensure proper `CoroutineScope` usage
- Check for structured concurrency violations

**Page Loading Issues:**
- Verify LoadOptions syntax with `-help`
- Check page expiration with `-expires` option
- Use `-diagnose` for detailed diagnostics

---

*Last updated: 2026-02-08*
