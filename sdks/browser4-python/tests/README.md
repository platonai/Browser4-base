# Browser4 Python SDK Tests

This directory contains tests for the Browser4 Python SDK, including both unit tests and integration tests.

## Test Structure

### Unit Tests
- `test_client.py` - Unit tests with stubbed responses
- `test_driver.py` - Unit tests for Browser4Driver
- `test_agentic_session_comprehensive.py` - Unit tests for AgenticSession
- `test_webdriver_comprehensive.py` - Unit tests for WebDriver

These tests use mocked HTTP responses and do not require running servers.

### Integration Tests
- `test_integration.py` - Integration tests with real Browser4 and Mock servers

Integration tests use the same infrastructure as the Kotlin SDK tests:
- **Browser4 REST Server** on port 8182 (started automatically)
- **Mock Site Server** on port 18080 (provides test pages)

## Running Tests

### Prerequisites
1. Java 17+ installed
2. Maven (or use the provided Maven wrapper)
3. Python 3.9+
4. Install dev dependencies:
   ```bash
   pip install -e ".[dev]"
   ```

### Run Unit Tests Only (Fast)
```bash
pytest -m "not integration"
```

### Run Integration Tests
```bash
pytest -m integration -v -s
```

**Note:** Integration tests will:
1. Build the Browser4 project if needed (first run takes ~5-10 minutes)
2. Start the Mock server on port 18080
3. Start the Browser4 REST server on port 8182
4. Run tests against real servers
5. Clean up servers after tests complete

### Run All Tests
```bash
pytest
```

### Run Specific Test File
```bash
pytest tests/test_client.py -v
pytest tests/test_integration.py -v -s
```

## Test Infrastructure

### Fixtures (conftest.py)

#### `maven_build`
- **Scope:** Session
- **Purpose:** Builds the Browser4 project if not already built
- **Used by:** All integration tests

#### `mock_server`
- **Scope:** Session
- **Purpose:** Starts Mock site server on port 18080
- **Provides:** Test pages matching Kotlin SDK tests
- **Reused:** Across all tests in the session

#### `browser4_server`
- **Scope:** Session
- **Purpose:** Starts Browser4 REST API server on port 8182
- **Dependencies:** Requires mock_server to be running
- **Reused:** Across all tests in the session

#### `integration_client`
- **Scope:** Function
- **Purpose:** Provides a fresh PulsarClient with session for each test
- **Cleanup:** Automatically deletes session after test

### Test URLs (test_urls.py)

Constants for test URLs, matching the Kotlin SDK `TestUrls.kt`:
- `SIMPLE_PAGE` - Simple static page
- `PRODUCT_LIST` - Product listing page
- `PRODUCT_DETAIL` - Product detail page
- `SIMPLE_DOM` - DOM manipulation test page
- And more...

## Consistency with Kotlin SDK Tests

The Python SDK integration tests are designed to match the Kotlin SDK tests:

1. **Same Servers:**
   - Uses `PulsarRestServerApplication` (Browser4 server)
   - Uses `MockSiteApplication` (Mock server on port 18080)

2. **Same Test Data:**
   - Test URLs match `TestUrls.kt`
   - Same test pages and endpoints

3. **Same Test Patterns:**
   - Session creation/deletion
   - Navigation and URL operations
   - WebDriver operations
   - AgenticSession workflows

## Troubleshooting

### Servers Don't Start
- Check Java version: `java -version` (need 17+)
- Check ports are free: `lsof -i :8182` and `lsof -i :18080`
- Check Maven build: Try manual build with `./mvnw clean install -DskipTests`

### Tests Hang
- Tests have 300s timeout by default
- First run builds the project (takes longer)
- Check logs for server startup issues

### Port Already in Use
- If ports 8182 or 18080 are in use, the fixtures will try to use existing servers
- Make sure no other Browser4 instances are running

### Build Failures
- Run manual build to see detailed errors:
  ```bash
  cd ../../..  # Go to Browser4 root
  ./mvnw clean install -DskipTests
  ```

## Adding New Tests

### Unit Test
Add to existing test files or create new `test_*.py` file:
```python
def test_my_feature(stub_client):
    client, stub = stub_client
    # Test with stubbed responses
    ...
```

### Integration Test
Add to `test_integration.py`:
```python
@pytest.mark.integration
class TestMyFeature:
    def test_with_real_server(self, integration_client):
        client = integration_client
        # Test with real server
        ...
```

## CI/CD Considerations

For CI environments:
1. Ensure Java 17+ is available
2. Cache Maven dependencies for faster builds
3. Consider running integration tests separately from unit tests
4. Integration tests may need longer timeout in CI

Example CI workflow:
```yaml
- name: Run unit tests
  run: pytest -m "not integration"
  
- name: Run integration tests
  run: pytest -m integration --timeout=600
  timeout-minutes: 15
```
