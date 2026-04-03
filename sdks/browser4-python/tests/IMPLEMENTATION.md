# Python SDK Test Infrastructure Implementation

## Summary

This document describes the implementation of integration test infrastructure for the Browser4 Python SDK that matches the Kotlin SDK test setup.

## Problem Statement (Chinese)

完善python sdk测试，你需要启动真实Browser4服务，并且启动一个Mock服务，确保测试服务同 Kotlin-sdk-test 模块中的测试使用一致的服务。

## Solution

The implementation adds integration test infrastructure that:

1. **Starts Real Browser4 Server**: Automatically launches the Browser4 REST API server on port 8182
2. **Starts Mock Server**: Launches the MockSiteApplication on port 18080 (same as Kotlin tests)
3. **Uses Consistent Test Data**: Test URLs and endpoints match the Kotlin SDK tests exactly

## Implementation Details

### Files Created/Modified

#### 1. `tests/conftest.py`
- **Purpose**: Pytest fixtures for test infrastructure
- **Key Fixtures**:
  - `maven_build`: Builds Browser4 project if needed (session-scoped)
  - `mock_server`: Starts Mock server on port 18080 (session-scoped)
  - `browser4_server`: Starts Browser4 REST server on port 8182 (session-scoped)
  - `integration_client`: Provides PulsarClient with session for each test (function-scoped)

#### 2. `tests/test_urls.py`
- **Purpose**: Test URL constants matching Kotlin SDK
- **Matches**: `TestUrls.kt` from kotlin-sdk-tests
- **Provides**: Constants like `SIMPLE_PAGE`, `PRODUCT_LIST`, etc.

#### 3. `tests/test_integration.py`
- **Purpose**: Integration tests using real servers
- **Test Classes**:
  - `TestPulsarClientIntegration`: Client session management tests
  - `TestPulsarSessionIntegration`: Session operations tests
  - `TestWebDriverIntegration`: WebDriver functionality tests
  - `TestAgenticSessionIntegration`: Agentic session workflow tests
  - `TestMockServerAccess`: Mock server accessibility tests

#### 4. `pytest.ini`
- **Purpose**: Pytest configuration
- **Features**:
  - Test markers (integration, unit, slow)
  - Timeout configuration (300s for integration tests)
  - Logging configuration

#### 5. `tests/README.md`
- **Purpose**: Documentation for test infrastructure
- **Content**: Running tests, fixtures, troubleshooting

#### 6. `tests/verify_test_setup.py`
- **Purpose**: Verification script for test setup
- **Checks**: Maven wrapper, ports, Java version, project structure

#### 7. Updated Files
- `pyproject.toml`: Added `pytest-timeout` dependency
- `README.md`: Added testing section with integration test instructions

## Consistency with Kotlin SDK Tests

### Same Servers
| Component | Port | Source |
|-----------|------|--------|
| Browser4 REST | 8182 | pulsar-rest (via Spring Boot) |
| Mock Site | 18080 | MockSiteApplication (pulsar-tests-common) |

### Same Test Infrastructure
- **Kotlin**: `KotlinSdkIntegrationTestBase` with Spring Boot test annotations
- **Python**: `conftest.py` fixtures that start the same servers

### Same Test Data
- **Kotlin**: `TestUrls.kt` constants
- **Python**: `test_urls.py` constants (exact match)

### Server Lifecycle
- **Session-scoped fixtures**: Servers start once per test session
- **Automatic cleanup**: Servers stop when tests complete
- **Port reuse**: If servers already running, tests use existing instances

## Usage

### Run Unit Tests Only (Fast)
```bash
pytest -m "not integration"
```

### Run Integration Tests (Requires Build)
```bash
pytest -m integration -v -s
```

### Verify Setup
```bash
python tests/verify_test_setup.py
```

### First Run Notes
- Integration tests will build the Browser4 project (5-10 minutes)
- Subsequent runs are much faster (servers start in ~30-60 seconds)
- Build artifacts are cached in `target/` directories

## Architecture

```
Python SDK Test Structure
├── Unit Tests (tests/test_*.py)
│   └── Use StubResponse for mocked HTTP
│
└── Integration Tests (tests/test_integration.py)
    ├── maven_build fixture (session)
    │   └── Builds Browser4 if needed
    │
    ├── mock_server fixture (session)
    │   └── Starts MockSiteApplication on 18080
    │
    ├── browser4_server fixture (session)
    │   └── Starts Browser4 REST on 8182
    │
    └── integration_client fixture (function)
        └── Provides fresh client with session
```

## Comparison with Kotlin Tests

### Kotlin (kotlin-sdk-tests)
```kotlin
@SpringBootTest(
    classes = [PulsarRestServerApplication::class],
    webEnvironment = RANDOM_PORT
)
@Import(MockServerConfiguration::class)
abstract class KotlinSdkIntegrationTestBase {
    @LocalServerPort
    protected var serverPort: Int = 0
}
```

### Python (tests/conftest.py)
```python
@pytest.fixture(scope="session")
def browser4_server(maven_build, mock_server):
    # Starts Browser4 REST server via Maven
    cmd = ["./mvnw", "-pl", "pulsar-rest", "spring-boot:run"]
    # ... start process ...
    yield base_url
    # ... cleanup ...
```

Both approaches:
- Start real servers
- Use same server implementations
- Provide clean test isolation
- Handle server lifecycle automatically

## Known Limitations

1. **First Run Speed**: Initial test run requires Maven build (5-10 minutes)
2. **Server Startup Time**: Integration tests need 30-60 seconds to start servers
3. **Port Conflicts**: Tests require ports 8182 and 18080 to be available
4. **Java Requirement**: Java 17+ must be installed
5. **Maven Requirement**: Maven wrapper must be present and executable

## Future Enhancements

Possible improvements:
1. Parallel test execution for integration tests
2. Docker-based server instances for faster startup
3. Test data generators matching Kotlin test helpers
4. More comprehensive integration test coverage
5. Performance benchmarking tests

## Testing the Implementation

All unit tests continue to work:
```bash
$ pytest -m "not integration" -v
# ... 37 tests pass ...
```

Integration test infrastructure validates correctly:
```bash
$ python tests/verify_test_setup.py
✓ Test infrastructure setup looks good!
```

Test collection works:
```bash
$ pytest tests/test_integration.py --collect-only
# ... 19 integration tests collected ...
```

## Conclusion

The Python SDK now has integration test infrastructure that:
- ✅ Starts real Browser4 service
- ✅ Starts Mock server on port 18080
- ✅ Uses consistent test services with Kotlin SDK tests
- ✅ Provides session-scoped fixtures for efficiency
- ✅ Includes comprehensive documentation
- ✅ Maintains backward compatibility with existing unit tests

The implementation follows pytest best practices and matches the Kotlin SDK test architecture while adapting to Python idioms.
