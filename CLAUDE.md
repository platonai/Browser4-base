# Browser4 Project Guide for Claude

This guide provides project-specific context for Claude to assist with Browser4 development.

## Project Overview

**Browser4** is a lightning-fast, coroutine-safe browser engine for AI agents. It provides:

- **Browser Agents** — Fully autonomous browser agents that reason, plan, and execute end-to-end tasks
- **Browser Automation** — High-performance automation for workflows, navigation, and data extraction
- **Machine Learning Agent** — Learns field structures across complex pages without consuming tokens
- **Extreme Performance** — Fully coroutine-safe; supports 100k ~ 200k complex page visits per machine per day
- **Data Extraction** — Hybrid of LLM, ML, and selectors for clean data across chaotic pages

## Quick Start

### Prerequisites
- Java 17+
- Latest Google Chrome

### Build Commands

**Linux/macOS:**
```bash
chmod +x mvnw
./mvnw -q -DskipTests
```

**Windows (PowerShell):**
```powershell
.\mvnw.cmd -q -D"skipTests"
```

**Windows (cmd):**
```cmd
mvnw.cmd -q -DskipTests
```

### Run Tests

**Core module tests (Linux/macOS):**
```bash
./mvnw -pl pulsar-core -am test -Dsurefire.failIfNoSpecifiedTests=false
```

**Core module tests (Windows PowerShell):**
```powershell
.\mvnw.cmd -pl pulsar-core -am test -D"surefire.failIfNoSpecifiedTests=false"
```

### Recommended Build Scripts
- Windows: `bin/build.ps1 [-test]`
- Linux/macOS: `bin/build.sh [-test]`

## Project Structure

| Module | Description |
|--------|-------------|
| `pulsar-core` | Core engine: sessions, scheduling, DOM, browser control |
| `pulsar-agentic` | AI agents implementation, MCP, skills registration |
| `pulsar-rest` | Spring Boot REST layer & command endpoints |
| `sdks/*` | Client SDKs (Kotlin, Python) |
| `browser4/*` | Product aggregation (SPA & packaging) |
| `pulsar-tests` | E2E & heavy integration & scenario tests |
| `pulsar-tests-common` | Shared test base classes and utilities |
| `pulsar-benchmarks` | JMH benchmarks |

## Key APIs and Concepts

### Sessions
```kotlin
// Create a session
val session = AgenticContexts.createSession()

// Create an agent
val agent = AgenticContexts.getOrCreateAgent()
```

### Core API Classes
- `WebDriver` — Browser control interface with human-like behaviors
- `PulsarSession` → `AgenticSession` — Page loading, parsing, and extraction
- `LoadOptions` — CLI-style URL parameters for page loading
- `BrowserPerceptiveAgent` — AI agent implementation

### Load Options
URL parameters control page loading behavior:
```kotlin
val page = session.load(url, "-expires 1d -refresh -parse")
```

Key options:
- `-expires <duration>` — Page expiration time
- `-refresh` — Force page refresh
- `-parse` — Activate parsing subsystem
- `-outLink <selector>` — Extract links matching selector

## Code Style Guidelines

### Kotlin Conventions
- Prefer immutable `data class`
- Use explicit return types
- Apply null-safety patterns (`require`/`check`/`?:`)
- Public APIs require KDoc documentation

**KDoc Template:**
```kotlin
/**
 * Brief description of what the function does.
 *
 * @param paramName Description of the parameter.
 * @return Description of the return value.
 * @throws ExceptionType When this exception is thrown.
 */
fun functionName(paramName: Type): ReturnType {
    require(paramName.isValid) { "paramName must be valid" }
    // implementation
}
```

### Logging
Use placeholder-style logging (avoid string concatenation):
```kotlin
logger.info("Task {} finished in {} ms", taskId, cost)
```

## Testing Guidelines

### Test Location
- Module unit tests: `src/test/kotlin/...`
- Centralized integration/E2E: `pulsar-tests/`
- Shared utilities: `pulsar-tests-common/`

### Naming Conventions
- Unit tests: `<ClassName>Test.kt`
- Integration tests: `<ClassName>IT.kt`
- E2E tests: `<ClassName>E2ETest.kt`
- **Method names: Use camelCase (NOT backtick naming)**
  - ✅ `testUserLoginWithValidCredentials()`
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
`IntegrationTest`, `E2ETest`, `ExternalServiceTest`, `TimeConsumingTest`, `HeavyTest`, `SmokeTest`, `BenchmarkTest`

## Configuration

### Application Port
Default: 8182

### Configuration Files
- `application.properties` — Main configuration
- `application-*.properties` — Profile-specific overrides

### Key Configuration Properties
```properties
# LLM API Key
openrouter.api.key=your-api-key

# Browser context mode
browser.context.mode=DEFAULT  # DEFAULT | SYSTEM_DEFAULT | SEQUENTIAL | TEMPORARY

# Display mode
browser.display.mode=GUI  # GUI | HEADLESS | SUPERVISED
```

## Development Principles

1. **Minimal Changes** — Make the smallest possible modifications
2. **Preserve Style** — Match existing code patterns
3. **Clear Logging** — Use structured, placeholder-based logging
4. **Test Coverage** — Include tests for new/changed logic
5. **Documentation** — Update docs for public API changes

## Definition of Done (PR Checklist)

- [ ] Build and related tests pass
- [ ] No new high-noise logs or warnings
- [ ] New/changed logic has tests (main path + edge case)
- [ ] No secrets or private endpoints committed
- [ ] No arbitrary version changes (follow parent BOM)
- [ ] Documentation updated for public behavior changes
- [ ] Performance impact assessed if significant (>5%)

## Common Issues & Troubleshooting

| Issue | Solution |
|-------|----------|
| `mvnw` no execute permission | `chmod +x mvnw` |
| JDK version mismatch | Ensure JDK 17+ in `JAVA_HOME` |
| Windows parameter escaping | Use `-D"key.with.dots=value"` |
| Port 8182 in use | Override `server.port` or use `application-local.properties` |
| CDP retry log storms | Use existing retry utilities, lower log level |

## Documentation References

- [Configuration Guide](docs/config.md)
- [Build Guide](docs/build.md)
- [Advanced Guide](docs/advanced-guides.md)
- [REST API Examples](docs/rest-api-examples.md)
- [Concepts](docs/concepts.md)
- [X-SQL](docs/x-sql.md)
- [AI Products Guidance](docs/ai-products-guidance.md)

## Related AI Assistant Files

- GitHub Copilot: `.github/copilot-instructions.md`
- Cursor: `.cursorrules`
- Windsurf: `.windsurfrules`
- Aider: `.aider.conf.yml`

---

*Last updated: 2026-01-25*
