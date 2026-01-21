# Kotlin SDK Integration Tests

Integration tests for the Kotlin SDK with real Browser4 REST server.

**Status**: ✅ Implementation Complete (4 test classes, 55+ tests)

## Overview

This module contains comprehensive integration tests that:
- Start a real pulsar-rest server on a random port
- Test the Kotlin SDK against the live server
- Include a mock EC server for test pages
- Cover all major SDK functionality (PulsarClient, WebDriver, PulsarSession, AgenticSession)

## Running Tests

### Prerequisites
- JDK 17+
- Chrome/Chromium installed (for browser tests)
- Maven Wrapper (recommended: `mvnw.cmd` from repo root)

### Run Integration Tests

> Notes
> - This module is configured via Surefire to run tests tagged `IntegrationTest` and to exclude `RequiresAI` by default.
> - Prefer running from the repo root so the Maven Wrapper and multi-module build work consistently.

```powershell
# From project root
.\mvnw.cmd -pl sdks/kotlin-sdk-tests -am test

# From this module directory
..\..\mvnw.cmd test
```

### Run Specific Test Classes

```powershell
# From project root
.\mvnw.cmd -pl sdks/kotlin-sdk-tests -am test -Dtest=PulsarClientIntegrationTest
.\mvnw.cmd -pl sdks/kotlin-sdk-tests -am test -Dtest=WebDriverIntegrationTest
.\mvnw.cmd -pl sdks/kotlin-sdk-tests -am test -Dtest=PulsarSessionIntegrationTest

# AgenticSession tests are disabled by default (and also excluded by tag).
# To enable them, remove @Disabled and include the tag RequiresAI (and configure AI/LLM).
.\mvnw.cmd -pl sdks/kotlin-sdk-tests -am test -Dtest=AgenticSessionIntegrationTest
```

### Run Tests with Specific Tags

```powershell
# Run only fast tests
.\mvnw.cmd -pl sdks/kotlin-sdk-tests -am test -Dgroups="IntegrationTest,Fast"

# Exclude slow tests
.\mvnw.cmd -pl sdks/kotlin-sdk-tests -am test -Dgroups="IntegrationTest" -DexcludedGroups="Slow"

# Exclude browser tests
.\mvnw.cmd -pl sdks/kotlin-sdk-tests -am test -Dgroups="IntegrationTest" -DexcludedGroups="RequiresBrowser"

# Include AI tests (requires AI/LLM config AND removing @Disabled)
.\mvnw.cmd -pl sdks/kotlin-sdk-tests -am test -Dgroups="IntegrationTest,RequiresAI" -DexcludedGroups=""
```

## Test Structure

```
src/test/kotlin/ai/platon/pulsar/sdk/integration/
├── KotlinSdkIntegrationTestBase.kt     # Base class for all tests
├── PulsarClientIntegrationTest.kt      # Client API tests
├── WebDriverIntegrationTest.kt         # Browser automation tests
├── server/
│   ├── PulsarRestServerApplication.kt  # Test server app
│   └── TestServerConfiguration.kt      # Mock server config
└── util/
    ├── TestUrls.kt                     # Test URL constants
    └── TestHelpers.kt                  # Test utilities
```

## Test Tags

Tests are organized using JUnit 5 tags:

- `@Tag("IntegrationTest")`: All integration tests
- `@Tag("RequiresServer")`: Needs REST server (all tests)
- `@Tag("RequiresBrowser")`: Needs browser (WebDriver tests)
- `@Tag("RequiresAI")`: Needs AI/LLM config (optional)
- `@Tag("Slow")`: Long-running tests (> 10 seconds)
- `@Tag("Fast")`: Quick tests (< 5 seconds)

## Configuration

Test configuration is in `src/test/resources/application-sdk-integration-test.properties`.

Key settings:
- Browser mode: TEMPORARY (auto-cleanup)
- Mock server port: 18080
- Test mode: enabled
- Reduced logging for cleaner output

## Test Coverage

### PulsarClient Tests (6 tests)
- Session creation and deletion
- Session with capabilities
- HTTP operations (GET, POST, DELETE)
- Error handling
- Session not found handling
- Multiple session support

### WebDriver Tests (15 tests)
- Page navigation (to URL, back, forward, reload)
- Element selection and existence checks
- Content extraction (text, multiple fields)
- Scrolling operations (to top, to bottom)
- Screenshot capture
- Script execution
- Wait mechanisms
- Page source retrieval

### PulsarSession Tests (18 tests)
- Session validation and activity checks
- URL normalization with/without arguments
- Page loading (load, open methods)
- Document parsing
- Field extraction with CSS selectors
- Scraping with combined operations
- Multiple page loading (loadAll)
- Async URL submission (submit, submitAll)
- Driver access through session
- Nil page handling

### AgenticSession Tests (16 tests, disabled by default)
- Single and parameterized AI actions
- Autonomous multi-step task execution
- Page observation with AI
- AI-powered data extraction
- Page summarization (default and custom)
- Complex multi-step workflows
- Combined manual and AI operations
- Error handling for AI operations
- Session state management

## Performance Targets

- Single test: < 10 seconds
- Full test suite: < 5 minutes
- Mock server startup: < 5 seconds

## Troubleshooting

### Tests are skipped
If you see no tests running, make sure you didn't accidentally exclude the `IntegrationTest` tag.

### Port already in use
The REST server uses a random port. The mock server uses port 18080. If 18080 is in use, tests may fail.

### Chrome not found
Browser tests require Chrome/Chromium. Install it or skip browser tests:
```powershell
.\mvnw.cmd -pl sdks/kotlin-sdk-tests -am test -Dgroups="IntegrationTest" -DexcludedGroups="RequiresBrowser"
```

### Tests are slow
Integration tests are slower than unit tests. Use:
```powershell
# Exclude slow tests
.\mvnw.cmd -pl sdks/kotlin-sdk-tests -am test -Dgroups="IntegrationTest" -DexcludedGroups="Slow"
```

## Related Documentation

- [Design Document](../browser4-sdk-kotlin/INTEGRATION-TEST-DESIGN.md)
- [Chinese Summary](../browser4-sdk-kotlin/INTEGRATION-TEST-DESIGN-SUMMARY.zh.md)
- [Architecture Diagram](../browser4-sdk-kotlin/INTEGRATION-TEST-ARCHITECTURE.txt)
- [Kotlin SDK README](../browser4-sdk-kotlin/README.md)
