# Changelog

All notable changes to the Browser4 Python SDK will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Added
- Complete MkDocs documentation site with comprehensive guides and examples
- Troubleshooting guide covering common issues and solutions
- Contributing guidelines for developers
- API reference documentation for all classes and methods

---

## [0.1.0] - 2024-01-30

### Added

#### Core Functionality
- `PulsarClient`: Low-level HTTP client for Browser4 REST API
- `PulsarSession`: Session management for page loading and data extraction
- `AgenticSession`: AI-powered browser automation extending PulsarSession
- `WebDriver`: Browser control and element interaction API

#### Session Management
- Session creation and deletion
- Session status checking
- Session reconnection support
- Automatic session ID tracking

#### Page Operations
- `open()`: Open URL immediately, bypass cache
- `load()`: Load URL with cache support
- `submit()`: Submit URL for async processing
- `normalize()`: Normalize URLs with load arguments
- Support for load arguments: `-expire`, `-refresh`, `-persist`, etc.

#### Data Extraction
- `extract()`: Extract data using CSS selectors
- `scrape()`: One-line load and extract
- `parse()`: Parse HTML into document structure
- Support for single and multiple element extraction
- Attribute extraction (`href`, `src`, etc.)

#### WebDriver Features
- Navigation: `navigate_to()`, `go_back()`, `go_forward()`, `reload()`
- Page info: `current_url()`, `title()`, `page_source()`
- Element interaction: `click()`, `fill()`, `type()`, `press()`, `hover()`, `focus()`
- Checkboxes: `check()`, `uncheck()`
- Scrolling: `scroll_down()`, `scroll_up()`, `scroll_to()`, `scroll_to_top()`, `scroll_to_bottom()`, `scroll_to_middle()`
- Element selection: `select_first_text_or_null()`, `select_text_all()`, `select_first_attribute_or_null()`, `select_attribute_all()`
- Element queries: `exists()`, `is_visible()`, `wait_for_selector()`
- Screenshots: `capture_screenshot()`
- Script execution: `execute_script()`, `evaluate()`
- Control: `delay()`, `pause()`, `stop()`

#### AI-Powered Features
- `act()`: Execute single action with natural language
- `run()`: Execute multi-step autonomous tasks
- `observe()`: Analyze and observe page state
- `agent_extract()`: AI-powered data extraction
- `summarize()`: Content summarization
- `clear_history()`: Reset agent context
- Process trace tracking

#### Data Models
- `WebPage`: Represents loaded web pages
- `NormURL`: Normalized URL with parsed arguments
- `PageSnapshot`: Parsed document structure
- `ElementRef`: DOM element reference
- `FieldsExtraction`: Extracted field values
- `AgentActResult`: Single action execution result
- `AgentRunResult`: Multi-step task execution result
- `AgentObservation`: Observation data
- `ObserveResult`: Page observation result
- `ExtractionResult`: Extraction result
- `ToolCallResult`: Tool call result
- `ActionDescription`: Action description
- `PageEventHandlers`: Event handler system (placeholder)

#### API Compatibility
- Kotlin-style property aliases (`contentType`, `isNil`, etc.)
- Method name aliases for Kotlin compatibility
- Consistent API design across languages

#### Configuration
- Configurable base URL
- Configurable timeout
- Custom HTTP headers support
- Environment variable support

### Documentation
- Comprehensive README with API overview
- Code examples for all major features
- FusedActs example demonstrating SDK capabilities
- Installation and setup instructions
- Testing guide

---

## [0.0.1] - 2024-01-01

### Added
- Initial project structure
- Basic client implementation
- Project configuration (pyproject.toml)
- Development dependencies setup

---

## Types of Changes

- **Added**: New features
- **Changed**: Changes in existing functionality
- **Deprecated**: Soon-to-be removed features
- **Removed**: Removed features
- **Fixed**: Bug fixes
- **Security**: Security vulnerability fixes

---

## Release Notes Format

Each release includes:
- **Version number**: Following semantic versioning
- **Release date**: ISO format (YYYY-MM-DD)
- **Changes**: Organized by type
- **Breaking changes**: Clearly marked if any
- **Migration guide**: For major version updates

---

## Upgrade Guide

### From 0.0.x to 0.1.0

**Breaking Changes:**
- Initial stable release, no breaking changes from pre-release

**New Features:**
All features listed in 0.1.0 are new. See the [Quick Start Guide](quickstart.md) for usage.

**Migration Steps:**
1. Install the latest version: `pip install -e .[dev]`
2. Update imports if needed
3. Review the [API Reference](api-reference/client.md) for new features

---

## Future Releases

### Planned for 0.2.0
- Enhanced event handler system
- Improved error handling and recovery
- Performance optimizations
- Additional selector strategies
- More browser control options

### Planned for 0.3.0
- WebSocket support for real-time communication
- Advanced authentication mechanisms
- Plugin system for extensibility
- Enhanced AI agent capabilities

### Under Consideration
- Async/await support
- Multi-session parallel execution
- Advanced caching strategies
- Browser profile management
- Proxy rotation support

---

## Contributing

See [CONTRIBUTING.md](contributing.md) for information on how to contribute to this project.

---

## Deprecated Features

None currently. Deprecated features will be listed here with:
- Version when deprecated
- Reason for deprecation
- Replacement recommendation
- Planned removal version

---

## Security Advisories

Security issues are taken seriously. See past security advisories here:

None currently.

To report a security vulnerability, please email the maintainers directly rather than opening a public issue.

---

## Links

- [Repository](https://github.com/platonai/Browser4)
- [Issues](https://github.com/platonai/Browser4/issues)
- [Documentation](index.md)
- [Contributing Guide](contributing.md)

---

*This changelog is maintained manually. For detailed commit history, see the [Git log](https://github.com/platonai/Browser4/commits/main/sdks/python-sdk).*
