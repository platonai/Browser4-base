# Changelog

All notable changes to the Browser4 NodeJS SDK will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Initial release preparation
- Publishing and installation infrastructure
- Comprehensive documentation (INSTALLATION.md, PUBLISHING.md, QUICKREF.md)
- Automated verification scripts
- GitHub Actions workflows for CI/CD

## [0.1.0] - TBD

### Added
- Initial release of Browser4 NodeJS SDK
- `Browser4Driver` - Automatic server lifecycle management
- `PulsarClient` - Low-level HTTP client for API communication
- `PulsarSession` - Session management for page loading and extraction
- `AgenticSession` - AI-powered browser automation
- `WebDriver` - Browser control and element interaction
- TypeScript type definitions
- Comprehensive test suite
- Documentation and examples

### Features
- Automatic Browser4.jar download and management
- WebDriver-compatible API
- AI-powered natural language browser control
- CSS selector-based data extraction
- Session management with cookie/storage support
- Navigation history tracking
- Element interaction (click, type, fill, etc.)
- JavaScript execution
- Screenshot capture

---

## How to Update This Changelog

### For Maintainers

When preparing a release:

1. Move items from `[Unreleased]` to a new version section
2. Add the release date
3. Follow the format below

### Version Format

```markdown
## [X.Y.Z] - YYYY-MM-DD

### Added
- New features

### Changed
- Changes in existing functionality

### Deprecated
- Soon-to-be removed features

### Removed
- Removed features

### Fixed
- Bug fixes

### Security
- Security fixes
```

### Examples

**Breaking Changes:**
```markdown
### Changed
- **BREAKING**: Renamed `WebDriver.type()` to `WebDriver.typeText()`
```

**New Features:**
```markdown
### Added
- Added support for file uploads via `WebDriver.uploadFile()`
```

**Bug Fixes:**
```markdown
### Fixed
- Fixed navigation timeout not being respected (#123)
```

---

## Links

- [Semantic Versioning](https://semver.org/)
- [Keep a Changelog](https://keepachangelog.com/)
- [GitHub Releases](https://github.com/platonai/Browser4/releases)
- [npm Package](https://www.npmjs.com/package/@platonai/browser4-sdk)
