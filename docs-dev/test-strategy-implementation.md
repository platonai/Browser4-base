# Test Strategy Implementation

## Overview

This document describes the implementation of the test strategy defined in `TESTING.md` and `docs-dev/copilot/tasks/daily/test-strategy.md`.

## Changes Summary

### 1. Maven Configuration (`pom.xml`)

#### Test Execution Properties

Added the following properties to control test execution:

```xml
<runITs>false</runITs>
<runE2ETests>false</runE2ETests>
<runSDKTests>false</runSDKTests>
<runCoreTests>false</runCoreTests>
<runRestTests>false</runRestTests>
```

#### Updated Default Excluded Groups

Updated `surefire.excludedGroups` to exclude all non-Fast tests by default:

```xml
<surefire.excludedGroups>
    Slow,Heavy,RequiresServer,RequiresBrowser,RequiresAI,RequiresDocker,
    Integration,E2E,SDK,MustRunExplicitly,
    IntegrationTest,E2ETest,HeavyTest,SkippableLowerLevelTest,TestInfraCheck,OptionalTest
</surefire.excludedGroups>
```

#### Maven Profiles

Added profiles for different test types:

- `integration-tests` - Activated by `-DrunITs=true`
- `e2e-tests` - Activated by `-DrunE2ETests=true`
- `sdk-tests` - Activated by `-DrunSDKTests=true`
- `core-tests` - Activated by `-DrunCoreTests=true`
- `rest-tests` - Activated by `-DrunRestTests=true`

### 2. Test Scripts

#### `bin/test.sh` (Linux/macOS)

Unified test execution script with the following commands:

```bash
./bin/test.sh fast   # Run fast unit tests (default)
./bin/test.sh it     # Run integration tests
./bin/test.sh e2e    # Run E2E tests
./bin/test.sh sdk    # Run SDK tests
./bin/test.sh core   # Run core module tests
./bin/test.sh rest   # Run REST module tests
./bin/test.sh all    # Run all tests (it, e2e, sdk)
```

Additional Maven arguments can be passed:

```bash
./bin/test.sh it -pl pulsar-core -am
```

#### `bin/test.ps1` (Windows)

PowerShell equivalent with the same interface:

```powershell
.\bin\test.ps1 fast
.\bin\test.ps1 it
.\bin\test.ps1 e2e
.\bin\test.ps1 sdk
.\bin\test.ps1 all
```

### 3. GitHub Actions Workflows

#### Updated Workflows

1. **`ci.yml`** - Updated to run only Fast unit tests by default
   - Uses `excluded_groups` to filter out Slow, Heavy, Integration, E2E, SDK tests
   - Aligns with test-strategy.md requirement: "mvn test must be fast and safe"

2. **`nightly.yml`** - Updated to run integration tests
   - Activates `integration-tests` profile
   - Excludes E2E and SDK tests
   - Runs on schedule (nightly)

3. **`kotlin-sdk-test.yml`** - Updated to use new test properties
   - Unit tests: Run fast tests only
   - Integration tests: Use `-DrunSDKTests=true`

#### New Workflows

1. **`integration-tests.yml`** - Manual and scheduled integration tests
   - Workflow dispatch (manual trigger)
   - Schedule: 02:00 UTC daily
   - Activates `integration-tests` profile
   - Excludes E2E and SDK tests

2. **`e2e-tests.yml`** - Manual E2E tests only
   - Workflow dispatch only (MustRunExplicitly)
   - Includes full infrastructure setup (Docker, MongoDB)
   - Runs E2E test scripts
   - Heavy resource usage

## Test Taxonomy

According to the test strategy, tests are classified using JUnit 5 tags:

### Test Levels (Required)
- `Unit` - Single module tests
- `Integration` - Multi-module / service collaboration
- `E2E` - End-to-end user paths
- `SDK` - External SDK contracts

### Cost (Required)
- `Fast` - < 5s
- `Slow` - 5-30s
- `Heavy` - > 30s / high resource usage

### Environment Dependencies (Optional)
- `RequiresServer`
- `RequiresBrowser`
- `RequiresAI`
- `RequiresDocker`

### Policies (Optional)
- `MustRunExplicitly` - Never run by default
- `SkippableLowerLevel` - Can be skipped if upper level passes
- `TestInfraCheck` - Infrastructure self-check (highest priority)

## Default Behavior

### `mvn test` (Default)

Runs only Fast unit tests:
- Level = Unit
- Cost = Fast
- NOT MustRunExplicitly

### Explicit Test Types

```bash
# Integration tests
mvn test -DrunITs=true

# E2E tests
mvn test -DrunE2ETests=true

# SDK tests
mvn test -DrunSDKTests=true

# All tests
mvn test -DrunITs=true -DrunE2ETests=true -DrunSDKTests=true
```

## Migration Guide

### For Developers

1. **Local development**: Use `./bin/test.sh fast` or `mvn test` for quick feedback
2. **Before PR**: Run `./bin/test.sh it` to ensure integration tests pass
3. **Full validation**: Use `./bin/test.sh all` (not recommended frequently)

### For CI/CD

1. **Pull requests**: Automatically run Fast unit tests via `ci.yml`
2. **Nightly builds**: Automatically run integration tests via `nightly.yml` or `integration-tests.yml`
3. **Manual E2E**: Trigger `e2e-tests.yml` workflow manually when needed

### For Test Authors

Update existing tests to use the new tag system:

```kotlin
@Tag("Unit")
@Tag("Fast")
class MyUnitTest { }

@Tag("Integration")
@Tag("Slow")
@Tag("RequiresServer")
@Tag("MustRunExplicitly")
class MyIntegrationTest { }

@Tag("E2E")
@Tag("Heavy")
@Tag("RequiresBrowser")
@Tag("RequiresAI")
@Tag("MustRunExplicitly")
class MyE2ETest { }
```

## Breaking Changes

### Default Test Exclusions

The default `surefire.excludedGroups` now excludes more test types. Tests that were previously run by `mvn test` may now be skipped unless they are tagged with appropriate tags.

**Action Required:**
- Tag all tests with appropriate Level and Cost tags
- Fast unit tests should have `@Tag("Unit")` and `@Tag("Fast")`
- Other tests need explicit opt-in via properties

### CI Behavior

- `ci.yml` now runs only Fast unit tests by default
- Integration and E2E tests require separate workflow triggers
- Nightly builds run integration tests automatically

## Validation

### Test the Scripts

```bash
# Test help
./bin/test.sh --help
./bin/test.ps1 -help

# Test fast tests (should complete quickly)
./bin/test.sh fast -DskipTests

# Test integration tests (will run if tests exist)
./bin/test.sh it -DskipTests
```

### Verify Maven Configuration

```bash
# Check active profiles
mvn help:active-profiles

# Check effective properties
mvn help:evaluate -Dexpression=surefire.excludedGroups

# Test with different properties
mvn test -DrunITs=true -DskipTests
```

### Verify GitHub Actions

1. Check workflow syntax: `gh workflow view ci.yml`
2. Manually trigger workflows: `gh workflow run integration-tests.yml`
3. Monitor workflow runs: `gh run list`

## References

- [TESTING.md](../TESTING.md) - Test taxonomy and guidelines
- [test-strategy.md](./copilot/tasks/daily/test-strategy.md) - Detailed test strategy
- [Maven Surefire Plugin](https://maven.apache.org/surefire/maven-surefire-plugin/) - Test execution configuration
- [JUnit 5 Tags](https://junit.org/junit5/docs/current/user-guide/#writing-tests-tagging-and-filtering) - Test tagging and filtering
