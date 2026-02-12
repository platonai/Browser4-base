# Changelog

All notable changes to browser4-sdk-python will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0] - TBD

### Added
- Initial release of browser4-sdk-python
- Browser4Driver for automatic server management and lifecycle control
  - Automatic download of Browser4.jar from GitHub releases
  - Server startup, health checking, and graceful shutdown
  - Context manager support for resource management
- PulsarClient for low-level HTTP API communication
  - Session management (create, delete)
  - Request/response handling with configurable timeout
  - Custom headers support
- PulsarSession for page loading and data extraction
  - URL normalization with load arguments
  - Page loading with cache control
  - CSS selector-based data extraction
  - Async page submission for batch processing
- AgenticSession for AI-powered browser automation
  - Natural language action execution (`act`)
  - Autonomous task completion (`run`)
  - Page observation capabilities (`observe`)
  - AI-powered content extraction (`agent_extract`)
  - Content summarization
  - Process trace tracking
- WebDriver for browser control and element interaction
  - Navigation (navigate_to, reload, go_back, go_forward)
  - Element interaction (click, fill, type, press, hover, focus)
  - Checkbox operations (check, uncheck)
  - Scrolling (scroll_down, scroll_up, scroll_to, scroll_to_top/bottom/middle)
  - Wait operations (wait_for_selector, exists, is_visible)
  - Content extraction (select_first_text, select_text_all, select attributes)
  - Multi-field extraction with CSS selectors
  - Screenshot capture (full page or element)
  - JavaScript execution (execute_script, evaluate)
  - Control operations (delay, pause, stop)
- Comprehensive data models
  - WebPage: Represents loaded web pages
  - NormURL: Normalized URLs with parsed arguments
  - Agent result models (AgentRunResult, AgentActResult, etc.)
  - Event handlers (placeholder for future implementation)
- Complete test suite
  - Unit tests with mocked HTTP responses
  - Integration tests with real Browser4 server
  - Test infrastructure matching Kotlin SDK patterns
- Example scripts demonstrating common use cases
  - Basic usage example
  - WebDriver interaction example
  - Agentic session example
  - Fused acts style example
  - Driver usage example

### Features
- Python 3.9+ support
- Cross-platform compatibility (Linux, macOS, Windows)
- Type hints for better IDE support
- Comprehensive error handling
- Configurable timeouts and retry logic
- Proxy support via system environment variables

### Documentation
- Complete README with installation and usage guide
- Quick start examples
- API reference for all public classes and methods
- API comparison with Kotlin SDK
- Multiple working example scripts
- Release plan documentation (Chinese and English)
- Quick reference guide for releases

### Developer Tools
- pytest-based test suite
- Code coverage reporting
- Integration test support with Browser4 server
- Mock server for testing

[Unreleased]: https://github.com/platonai/Browser4/compare/python-sdk-v0.1.0...HEAD
[0.1.0]: https://github.com/platonai/Browser4/releases/tag/python-sdk-v0.1.0
