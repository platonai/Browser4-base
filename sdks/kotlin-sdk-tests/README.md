# Kotlin SDK Integration Tests

Integration tests for the Kotlin SDK with real Browser4 REST server.

## Overview

This module contains integration tests that:
- Start a real pulsar-rest server on a random port
- Test the Kotlin SDK against the live server
- Include a mock EC server for test pages
- Cover all major SDK functionality

## Running Tests

### Prerequisites
- JDK 17+
- Chrome/Chromium installed (for browser tests)
- Maven 3.6+

### Run Integration Tests

```bash
# From this directory
mvn test -DrunIntegrationTests=true

# Or use the profile
mvn test -Prun-integration-tests

# Run all tests including AI features
mvn test -DrunFullTests=true

# From project root
mvn test -pl sdks/kotlin-sdk-tests -DrunIntegrationTests=true
```

### Run Specific Test Classes

```bash
mvn test -Dtest=PulsarClientIntegrationTest -DrunIntegrationTests=true
mvn test -Dtest=WebDriverIntegrationTest -DrunIntegrationTests=true
```

### Run Tests with Specific Tags

```bash
# Run only fast tests
mvn test -Dgroups="IntegrationTest,Fast" -DrunIntegrationTests=true

# Exclude slow tests
mvn test -Dgroups="IntegrationTest,!Slow" -DrunIntegrationTests=true

# Exclude AI tests (default)
mvn test -Dgroups="IntegrationTest,!RequiresAI" -DrunIntegrationTests=true
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

### PulsarClient Tests
- Session creation and deletion
- HTTP operations (GET, POST, DELETE)
- Error handling
- Multiple session support

### WebDriver Tests
- Page navigation
- Element selection and interaction
- Content extraction
- Scrolling operations
- Screenshot capture
- Script execution
- Wait mechanisms

## Performance Targets

- Single test: < 10 seconds
- Full test suite: < 5 minutes
- Mock server startup: < 5 seconds

## Troubleshooting

### Tests are skipped
By default, tests are skipped. Use `-DrunIntegrationTests=true` to run them.

### Port already in use
The REST server uses a random port. The mock server uses port 18080. If 18080 is in use, tests may fail.

### Chrome not found
Browser tests require Chrome/Chromium. Install it or skip browser tests:
```bash
mvn test -Dgroups="IntegrationTest,!RequiresBrowser" -DrunIntegrationTests=true
```

### Tests are slow
Integration tests are slower than unit tests. Use:
```bash
# Run only fast tests
mvn test -Dgroups="IntegrationTest,!Slow" -DrunIntegrationTests=true
```

## Related Documentation

- [Design Document](../kotlin-sdk/INTEGRATION-TEST-DESIGN.md)
- [Chinese Summary](../kotlin-sdk/INTEGRATION-TEST-DESIGN-SUMMARY.zh.md)
- [Architecture Diagram](../kotlin-sdk/INTEGRATION-TEST-ARCHITECTURE.txt)
- [Kotlin SDK README](../kotlin-sdk/README.md)
