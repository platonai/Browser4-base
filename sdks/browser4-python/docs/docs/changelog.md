# Changelog

All notable changes to the Browser4 Python SDK will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.0] - 2024-01-XX

### Added

#### Core Features
- **Browser4Driver** - Automatic lifecycle management for Browser4.jar
  - Automatic download from GitHub releases
  - Server startup and health checking
  - Context manager support for automatic cleanup
  - Configurable ports, JAR paths, and Java options
  - Proxy support for downloads

- **PulsarClient** - Low-level HTTP client for Browser4 API
  - Session management (create, delete, list)
  - Configurable timeouts and headers
  - Connection pooling via httpx

- **PulsarSession** - High-level session for web scraping
  - `open()` - Load pages immediately (bypass cache)
  - `load()` - Load with caching support
  - `submit()` - Submit URLs for async processing
  - `normalize()` - URL normalization with load arguments
  - `parse()` - Parse HTML into BeautifulSoup documents
  - `extract()` - Extract data with CSS selectors
  - `scrape()` - Combined load, parse, and extract

- **AgenticSession** - AI-powered browser automation (extends PulsarSession)
  - `act()` - Execute single actions with natural language
  - `run()` - Run autonomous multi-step tasks
  - `observe()` - Analyze page state and get suggestions
  - `agent_extract()` - AI-powered data extraction with schemas
  - `summarize()` - Generate content summaries
  - `clear_history()` - Clear agent state history
  - `state_history` - Access agent action history
  - `process_trace` - View detailed process trace

- **WebDriver** - Browser control and element interaction
  - Navigation: `navigate_to()`, `reload()`, `go_back()`, `go_forward()`
  - Page info: `current_url()`, `title()`, `page_source()`
  - Element interaction: `click()`, `fill()`, `type()`, `press()`, `hover()`, `focus()`
  - Checkboxes: `check()`, `uncheck()`
  - Scrolling: `scroll_down()`, `scroll_up()`, `scroll_to()`, `scroll_to_top()`, `scroll_to_bottom()`, `scroll_to_middle()`
  - Waiting: `wait_for_selector()`, `exists()`, `is_visible()`
  - Content extraction: `select_first_text_or_null()`, `select_text_all()`, `select_first_attribute_or_null()`, `select_attribute_all()`, `extract()`
  - Screenshots: `capture_screenshot()` (full page or element)
  - Script execution: `execute_script()`, `evaluate()`
  - Control: `delay()`, `pause()`, `stop()`

#### Data Models
- **WebPage** - Represents loaded web pages
  - URL, location, content type, status
  - Content length tracking
  - Nil page detection

- **NormURL** - Normalized URLs with parsed arguments
  - URL specification and normalization
  - Parsed load arguments
  - Validity checking

- **Agent Results**
  - `AgentRunResult` - Results from multi-step tasks
  - `AgentActResult` - Results from single actions
  - `ObserveResult` - Page observation results
  - `ExtractionResult` - AI extraction results
  - `AgentHistory` - Agent action history tracking
  - `AgentState` - Individual action state

- **Other Models**
  - `ElementRef` - Element references for WebDriver
  - `FieldsExtraction` - Field extraction results
  - `ToolCallResult` - Tool execution results
  - `ActionDescription` - Action metadata
  - `ChatResponse` - AI chat responses
  - `PageEventHandlers` - Event handler placeholder

#### Examples
- **basic_usage.py** - Fundamental SDK operations
  - Browser4Driver usage
  - Session creation and management
  - Page loading and parsing
  - CSS selector extraction
  - WebDriver navigation

- **agentic_session_example.py** - AI-powered features
  - AI actions with `act()`
  - Autonomous tasks with `run()`
  - Page observation
  - AI-powered extraction
  - Content summarization
  - State history tracking

- **driver_usage.py** - Server management patterns
  - Context manager usage
  - Manual lifecycle control
  - Custom configuration
  - Health checking

- **webdriver_example.py** - Browser control
  - Element interaction
  - Navigation and scrolling
  - Content extraction
  - Screenshots

#### Documentation
- Comprehensive README with quick start guide
- API comparison with Kotlin SDK
- Configuration examples
- Data model documentation
- Event handler placeholders
- FusedActs example port

#### Testing
- Basic test structure with pytest
- Example test files

#### Development Tools
- pyproject.toml with modern Python packaging
- setup.cfg for metadata
- uv.lock for dependency locking
- .gitignore for Python projects

### Technical Details

#### Dependencies
- **httpx** - Modern HTTP client with async support
- **beautifulsoup4** - HTML parsing (optional)
- **lxml** - Fast XML/HTML parser (optional)

#### Python Support
- Python 3.9+
- Type hints throughout
- Modern Python idioms

#### API Compatibility
- Mirrors Kotlin SDK API design
- Consistent method naming across languages
- Python naming conventions (snake_case) with Kotlin-style aliases

### Known Limitations

- **Event Handlers** - `PageEventHandlers` is a placeholder for future implementation
- **Capture Method** - Currently re-opens URL as there's no dedicated REST endpoint
- **Local Parsing** - Requires BeautifulSoup for HTML parsing

### Development Status

This is the initial release (0.1.0) of the Browser4 Python SDK. The API is considered stable for the included features, but may evolve based on user feedback and Browser4 platform updates.

### Breaking Changes

None - this is the first release.

### Upgrade Notes

This is the first release. No upgrade needed.

### Contributors

- Browser4 Core Team
- Python SDK Contributors

### Acknowledgments

- Kotlin Browser4 SDK for API design reference
- httpx library for excellent HTTP client
- BeautifulSoup for HTML parsing

## [Unreleased]

### Planned Features

- Full event handler implementation
- Additional AI model support
- Performance optimizations
- Enhanced error messages
- More comprehensive examples
- Additional data extraction patterns

### Under Consideration

- Async/await API
- Plugin system
- Custom AI provider integration
- Advanced caching strategies
- Metrics and monitoring

---

## Version History

- **0.1.0** (2024-01-XX) - Initial release

---

## How to Contribute

We welcome contributions! Please:

1. Check [existing issues](https://github.com/platonai/Browser4/issues)
2. Open a new issue for bugs or features
3. Submit pull requests with:
   - Clear description
   - Tests for new features
   - Documentation updates
   - Changelog entry

## Support

- **Documentation**: https://github.com/platonai/Browser4/tree/master/sdks/browser4-python/docs
- **Issues**: https://github.com/platonai/Browser4/issues
- **Discussions**: https://github.com/platonai/Browser4/discussions

## License

Licensed under the Apache License, Version 2.0. See LICENSE file for details.
