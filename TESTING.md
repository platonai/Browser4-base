# Browser4 Testing Guide

Quick reference for running tests in the Browser4 project.

## Prerequisites

- Java 17+
- Maven (wrapper included)
- Chrome/Chromium (for E2E tests)

## Quick Start

### Run All Tests

```bash
# Linux/macOS
./bin/test.sh

# Windows
bin\test.ps1
```

### Run Tests for Specific Module

```bash
# Linux/macOS
./mvnw test -pl pulsar-core

# Windows
mvnw.cmd test -pl pulsar-core
```

### Skip Tests During Build

```bash
# Linux/macOS
./bin/build.sh

# Windows (default behavior - tests skipped)
bin\build.ps1
```

### Run Tests During Build

```bash
# Linux/macOS
./bin/build.sh -test

# Windows
bin\build.ps1 -test
```

## Test Categories

### Unit Tests (Default)

Fast, isolated tests that run by default:

```bash
# Linux/macOS
./mvnw test

# Windows
mvnw.cmd test
```

### Integration Tests

Include integration tests with the `IntegrationTest` tag:

```bash
# Linux/macOS
./mvnw test -DexcludedGroups=""

# Windows
mvnw.cmd test -D"excludedGroups="
```

### E2E Tests

Run end-to-end browser automation tests:

```bash
# Linux/macOS
./mvnw test -pl pulsar-tests -Dtest="*E2ETest*"

# Windows
mvnw.cmd test -pl pulsar-tests -Dtest="*E2ETest*"
```

### Specific Test Class

```bash
# Linux/macOS
./mvnw test -Dtest=MyTestClass

# Windows
mvnw.cmd test -Dtest=MyTestClass
```

### Specific Test Method

```bash
# Linux/macOS
./mvnw test -Dtest=MyTestClass#myTestMethod

# Windows
mvnw.cmd test -Dtest=MyTestClass#myTestMethod
```

## Test Scripts

The project provides convenient test scripts in the `bin/` directory:

### test.sh / test.ps1

Comprehensive test runner with multiple options:

```bash
# Run unit tests only (default)
./bin/test.sh

# Run with integration tests
./bin/test.sh -integration

# Run full suite including E2E
./bin/test.sh -e2e

# Run specific module
./bin/test.sh -module pulsar-core

# Run with coverage report
./bin/test.sh -coverage

# Clean before testing
./bin/test.sh -clean
```

Windows equivalent:
```powershell
bin\test.ps1 -integration
bin\test.ps1 -e2e
bin\test.ps1 -module pulsar-core
bin\test.ps1 -coverage
bin\test.ps1 -clean
```

## Common Scenarios

### Development Workflow

During active development, run tests for the module you're working on:

```bash
# Linux/macOS
./mvnw test -pl pulsar-core -am

# Windows (note: requires -D"surefire.failIfNoSpecifiedTests=false")
mvnw.cmd test -pl pulsar-core -am -D"surefire.failIfNoSpecifiedTests=false"
```

### Before Commit

Run full unit test suite:

```bash
./bin/test.sh
```

### Before Push

Run integration tests:

```bash
./bin/test.sh -integration
```

### Debug Single Test

```bash
# Linux/macOS with debug port
./mvnw test -Dtest=MyTest -Dmaven.surefire.debug

# Windows
mvnw.cmd test -Dtest=MyTest -D"maven.surefire.debug"
```

Then attach your debugger to port 5005.

## Test Tags

Tests can be tagged for selective execution:

- `IntegrationTest`: Multi-component tests
- `E2ETest`: Browser automation tests
- `ExternalServiceTest`: Requires external services
- `TimeConsumingTest`: Takes > 10 seconds
- `HeavyTest`: Resource intensive
- `SmokeTest`: Quick health checks
- `BenchmarkTest`: Performance tests

### Exclude Specific Tags

```bash
# Linux/macOS
./mvnw test -DexcludedGroups="TimeConsumingTest,ExternalServiceTest"

# Windows
mvnw.cmd test -D"excludedGroups=TimeConsumingTest,ExternalServiceTest"
```

### Include Only Specific Tags

```bash
# Linux/macOS
./mvnw test -Dgroups="SmokeTest"

# Windows
mvnw.cmd test -D"groups=SmokeTest"
```

## Coverage Reports

### Generate Coverage Report

```bash
# Linux/macOS
./mvnw test jacoco:report

# Windows
mvnw.cmd test jacoco:report
```

### View Coverage Report

Open `target/site/jacoco/index.html` in your browser.

### Aggregate Coverage (All Modules)

```bash
# Linux/macOS
./mvnw verify -Pci

# Windows
mvnw.cmd verify -P"ci"
```

View aggregate report at `target/site/jacoco-aggregate/index.html`.

## Parallel Testing

Enable parallel test execution for faster results:

```bash
# Linux/macOS
./mvnw test -Dsurefire.parallel=methods -Dsurefire.threadCount=4

# Windows
mvnw.cmd test -D"surefire.parallel=methods" -D"surefire.threadCount=4"
```

## Troubleshooting

### Tests Not Running

If tests are being skipped:

```bash
# Check if skipTests is set
./mvnw help:effective-pom | grep skipTests

# Force tests to run
./mvnw test -DskipTests=false
```

### Test Compilation Errors

Compile tests without running them:

```bash
# Linux/macOS
./mvnw test-compile

# Windows
mvnw.cmd test-compile
```

### Memory Issues

Increase memory for tests:

```bash
# Linux/macOS
export MAVEN_OPTS="-Xmx4g"
./mvnw test

# Windows
set MAVEN_OPTS=-Xmx4g
mvnw.cmd test
```

### Windows-Specific Issues

When using `-am` flag on Windows, you must add:
```bash
mvnw.cmd test -pl pulsar-core -am -D"surefire.failIfNoSpecifiedTests=false"
```

When using properties with dots, quote them:
```bash
mvnw.cmd test -D"my.dotted.property=value"
```

## CI/CD Integration

The project uses GitHub Actions for continuous testing:

- **Fast Feedback**: Unit tests on every commit
- **Integration**: Integration tests on PR
- **E2E**: Full suite on merge to main
- **Nightly**: Complete suite with benchmarks

See `.github/workflows/ci.yml` for configuration details.

## Performance Benchmarks

Run JMH benchmarks:

```bash
# Build benchmarks
./mvnw -pl pulsar-benchmarks -am package -DskipTests

# Run all benchmarks
java -jar pulsar-benchmarks/target/pulsar-benchmarks-*-shaded.jar

# Run specific benchmark
java -jar pulsar-benchmarks/target/pulsar-benchmarks-*-shaded.jar MyBenchmark
```

## Test Resources

### Mock Test Server

Tests requiring a web server can use the built-in mock server from `pulsar-tests-common`:

```kotlin
@ExtendWith(TestWebSiteAccess::class)
class MyTest {
    @Test
    fun testWithMockServer() {
        // Mock server available at http://localhost:<random-port>
    }
}
```

### Test Data

- **Static Resources**: `pulsar-tests-common/src/main/resources/static/`
- **Generated Data**: `pulsar-tests-common/src/main/resources/static/generated/`
- **Test Fixtures**: `src/test/resources/` in each module

## Best Practices

1. **Keep tests fast**: Unit tests < 100ms, Integration < 5s, E2E < 30s
2. **Use meaningful names**: `` `Given X When Y Then Z` ``
3. **One assertion per test**: Focus on single behavior
4. **Clean up resources**: Use `@AfterEach` or try-with-resources
5. **Avoid test interdependence**: Tests should run in any order
6. **Tag appropriately**: Use @Tag for categorization
7. **Mock external services**: Don't depend on external APIs in unit tests
8. **Test edge cases**: Not just happy paths

## Additional Resources

- [Test Strategy](docs/test-strategy.md) - Comprehensive testing strategy
- [Test Guide](devdocs/copilot/test-guide.md) - Detailed testing guide with examples
- [Maven Test Control](devdocs/development/mvn-test.md) - Maven Surefire configuration
- [Test Tags](devdocs/development/test/test-tags.md) - Tag usage and conventions

## Getting Help

- Check existing tests for examples
- Review test documentation in `docs/` and `devdocs/`
- Ask in project discussions or issues
- See [FAQ](docs/faq/) for common questions
