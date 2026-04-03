# Python SDK Test Architecture

This document provides a visual overview of the test architecture.

## Test Execution Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                    Run pytest Command                            │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │  pytest.ini      │
                    │  Configuration   │
                    └──────────────────┘
                              │
                              ▼
         ┌────────────────────┴────────────────────┐
         │                                          │
         ▼                                          ▼
┌─────────────────┐                      ┌──────────────────────┐
│   Unit Tests    │                      │  Integration Tests   │
│                 │                      │                      │
│ • test_client   │                      │ • test_integration   │
│ • test_driver   │                      │                      │
│ • test_webdriver│                      │ Uses fixtures:       │
│ • test_agentic  │                      │ • maven_build       │
│                 │                      │ • mock_server       │
│ Uses: Stubs     │                      │ • browser4_server   │
│ No servers      │                      │ • integration_client│
└─────────────────┘                      └──────────────────────┘
```

## Server Startup Sequence

```
Integration Test Execution
    │
    ▼
┌─────────────────────────────────────────────────────────────┐
│ 1. maven_build Fixture (Session Scope)                      │
│    • Check if project is built                              │
│    • Run: ./mvnw -q -DskipTests install                    │
│    • Cache build artifacts                                  │
└─────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. mock_server Fixture (Session Scope)                      │
│    • Check port 18080 availability                         │
│    • Start: MockSiteApplication via Maven                  │
│    • Wait for server ready (health check)                  │
│    • Provides: http://localhost:18080                      │
└─────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. browser4_server Fixture (Session Scope)                  │
│    • Check port 8182 availability                          │
│    • Start: Browser4 REST via Maven (pulsar-rest)         │
│    • Wait for server ready (health + /health/ready)        │
│    • Wait additional 5s for browser subsystem              │
│    • Provides: http://localhost:8182                       │
└─────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. integration_client Fixture (Function Scope)              │
│    • Create PulsarClient(base_url)                         │
│    • Create session via client.create_session()            │
│    • Yields client for test                                │
│    • Cleanup: delete session, close client                 │
└─────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────┐
│ Test Execution                                              │
│    • Use client to test functionality                       │
│    • Access Mock server pages                              │
│    • Verify Browser4 REST API operations                   │
└─────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────┐
│ Cleanup (Automatic)                                         │
│    • Function fixtures: teardown after each test            │
│    • Session fixtures: teardown once at end                 │
│    • Servers: graceful shutdown (terminate + kill)          │
└─────────────────────────────────────────────────────────────┘
```

## Component Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Browser4 System                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────────────┐         ┌──────────────────────┐     │
│  │  Browser4 REST API   │         │   Mock Site Server   │     │
│  │   (pulsar-rest)      │         │ (MockSiteApplication)│     │
│  │                      │         │                      │     │
│  │  Port: 8182          │         │  Port: 18080        │     │
│  │                      │         │                      │     │
│  │  Endpoints:          │         │  Test Pages:         │     │
│  │  • /session          │         │  • /ec/              │     │
│  │  • /url              │         │  • /ec/b?node=...    │     │
│  │  • /normalize        │         │  • /ec/dp/...        │     │
│  │  • /open             │         │  • /assets/...       │     │
│  │  • /load             │         │  • /generated/...    │     │
│  │  • /agent/*          │         │                      │     │
│  │  • /chat             │         │                      │     │
│  └──────────────────────┘         └──────────────────────┘     │
│           ▲                                    ▲                │
│           │                                    │                │
└───────────┼────────────────────────────────────┼────────────────┘
            │                                    │
            │                                    │
┌───────────┴────────────────────────────────────┴────────────────┐
│                    Python SDK Test Layer                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────────────┐         ┌──────────────────────┐     │
│  │   Integration Tests  │         │     Unit Tests       │     │
│  │                      │         │                      │     │
│  │  • PulsarClient      │         │  • Stubbed HTTP      │     │
│  │  • PulsarSession     │         │  • No servers        │     │
│  │  • WebDriver         │         │  • Fast execution    │     │
│  │  • AgenticSession    │         │  • Isolated tests    │     │
│  │                      │         │                      │     │
│  │  Uses: Real servers  │         │  Uses: StubResponse  │     │
│  └──────────────────────┘         └──────────────────────┘     │
│           ▲                                                      │
│           │                                                      │
│  ┌────────┴─────────────────────────────────────────┐          │
│  │            Test Infrastructure                     │          │
│  │                                                    │          │
│  │  • conftest.py (fixtures)                        │          │
│  │  • test_urls.py (constants)                      │          │
│  │  • pytest.ini (configuration)                    │          │
│  └────────────────────────────────────────────────────┘          │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

## Test Data Flow

```
┌────────────────────────────────────────────────────────────────┐
│                        Test URLs                                │
│  (from test_urls.py, matching Kotlin TestUrls.kt)             │
└────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌────────────────────────────────────────────────────────────────┐
│                    Integration Test                             │
│                                                                 │
│  test_navigate_to_url(integration_client):                     │
│      client.post(f"/session/{session_id}/url",                 │
│                  {"url": SIMPLE_PAGE})                         │
└────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌────────────────────────────────────────────────────────────────┐
│                   Browser4 REST API                             │
│                  (http://localhost:8182)                        │
│                                                                 │
│  POST /session/{sessionId}/url                                 │
│  Body: {"url": "http://localhost:18080/ec/"}                   │
└────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌────────────────────────────────────────────────────────────────┐
│                   Browser Subsystem                             │
│                                                                 │
│  • Chrome browser instance                                      │
│  • Page loading and rendering                                  │
│  • JavaScript execution                                         │
│  • Element interaction                                          │
└────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌────────────────────────────────────────────────────────────────┐
│                    Mock Site Server                             │
│                  (http://localhost:18080)                       │
│                                                                 │
│  GET /ec/                                                      │
│  Returns: HTML page content                                    │
└────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌────────────────────────────────────────────────────────────────┐
│                  Response Chain                                 │
│                                                                 │
│  Mock Server → Browser → Browser4 API → Test Client           │
└────────────────────────────────────────────────────────────────┘
```

## Comparison: Kotlin vs Python

```
┌─────────────────────────────────────────────────────────────────┐
│                    Kotlin SDK Tests                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  @SpringBootTest(                                               │
│      classes = [PulsarRestServerApplication::class],            │
│      webEnvironment = RANDOM_PORT                               │
│  )                                                              │
│  @Import(MockServerConfiguration::class)                        │
│  abstract class KotlinSdkIntegrationTestBase {                  │
│      @LocalServerPort                                           │
│      protected var serverPort: Int = 0                          │
│                                                                  │
│      @BeforeEach                                                │
│      fun setupClient() {                                        │
│          client = PulsarClient(baseUrl = "http://localhost:$port")│
│      }                                                           │
│  }                                                              │
│                                                                  │
│  • Spring Boot auto-starts servers                              │
│  • Uses @SpringBootTest annotations                             │
│  • MockServerConfiguration bean                                 │
│  • Per-test cleanup in @AfterEach                              │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ Equivalent Functionality
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Python SDK Tests                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  @pytest.fixture(scope="session")                              │
│  def browser4_server(maven_build, mock_server):                │
│      # Start Browser4 via Maven spring-boot:run                │
│      process = subprocess.Popen([                              │
│          "./mvnw", "-pl", "pulsar-rest", "spring-boot:run"    │
│      ])                                                         │
│      wait_for_server("http://localhost:8182")                  │
│      yield "http://localhost:8182"                             │
│      process.terminate()                                        │
│                                                                  │
│  @pytest.fixture(scope="function")                             │
│  def integration_client(browser4_server):                      │
│      client = PulsarClient(base_url=browser4_server)          │
│      client.session_id = client.create_session()              │
│      yield client                                               │
│      client.delete_session()                                    │
│                                                                  │
│  • pytest fixtures manage servers                               │
│  • Maven starts Spring Boot servers                             │
│  • Session-scoped for efficiency                                │
│  • Function-scoped client per test                              │
└─────────────────────────────────────────────────────────────────┘
```

## File Organization

```
sdks/browser4-python/
├── browser4/                    # SDK source code
│   ├── __init__.py
│   ├── client.py               # PulsarClient
│   ├── agentic_session.py      # AgenticSession
│   ├── webdriver.py            # WebDriver
│   └── models.py               # Data models
│
├── tests/                      # Test directory
│   │
│   ├── conftest.py            # ⭐ Test fixtures
│   ├── test_urls.py           # ⭐ Test URL constants
│   ├── test_integration.py    # ⭐ Integration tests
│   │
│   ├── test_client.py         # Unit tests (existing)
│   ├── test_driver.py         # Unit tests (existing)
│   ├── test_webdriver_comprehensive.py  # Unit tests
│   ├── test_agentic_session_comprehensive.py  # Unit tests
│   │
│   ├── verify_test_setup.py  # ⭐ Setup verification
│   ├── smoke_test.py          # ⭐ Quick smoke test
│   │
│   ├── README.md              # ⭐ Test documentation
│   ├── IMPLEMENTATION.md      # ⭐ Technical details
│   └── SUMMARY.md             # ⭐ Bilingual summary
│
├── pytest.ini                  # ⭐ Pytest configuration
├── pyproject.toml             # Project dependencies
└── README.md                  # Main SDK documentation

⭐ = New files created for integration testing
```

## Legend

- **Session Scope**: Fixture runs once per test session, shared across all tests
- **Function Scope**: Fixture runs once per test function, fresh for each test
- **Maven Wrapper**: `./mvnw` or `mvnw.cmd` for cross-platform Maven execution
- **Health Check**: HTTP GET to `/health` endpoint to verify server is ready
- **Mock Server**: Provides test HTML pages and assets for testing
- **Browser4 Server**: Full REST API server with browser automation capabilities
