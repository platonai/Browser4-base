# browser4-python Release Plan

## 📋 Overview

This document outlines the complete release process for browser4-python, including version management, packaging, testing, publishing, and ongoing maintenance.

**Current Status:**
- Version: 0.1.0
- Status: In development, not yet officially released
- Python Requirements: >=3.9
- Dependencies: requests>=2.31.0, beautifulsoup4>=4.12.0

---

## 1. Versioning Strategy

### 1.1 Version Naming Convention

Following Semantic Versioning 2.0.0: `MAJOR.MINOR.PATCH[-SUFFIX]`

- **MAJOR**: Incompatible API changes
- **MINOR**: Backwards-compatible new features
- **PATCH**: Backwards-compatible bug fixes
- **SUFFIX** (optional): Pre-release identifiers
  - `alpha.N`: Internal testing
  - `beta.N`: Public testing
  - `rc.N`: Release candidate

**Example Version Sequence:**
```
0.1.0-alpha.1  → Internal testing
0.1.0-beta.1   → Public testing
0.1.0-rc.1     → Release candidate
0.1.0          → Stable release
0.1.1          → Bug fixes
0.2.0          → New features
1.0.0          → Major version
```

### 1.2 Version Synchronization Strategy

browser4-python follows an **independent versioning** strategy, **decoupled** from the main Browser4 project:

| Component | Current Version | Version Strategy |
|-----------|-----------------|------------------|
| Browser4 (main project) | 4.5.0           | Maven semantic versioning |
| browser4-kotlin | Follows main    | Synced with main project |
| browser4-python | 0.1.0           | Independent versioning |
| browser4-nodejs | TBD             | Independent versioning |
| browser4-sdk-rust | TBD             | Independent versioning |

**Rationale:**
1. Python SDK is an independent client library with a different lifecycle
2. Can iterate independently following Python ecosystem rhythm
3. Avoids confusion from main project version jumps
4. Follows Python community conventions (independent packages have independent versions)

**Compatibility Indication:**
- Clearly document compatible Browser4 server version ranges in README and documentation
- Example: `browser4-python 0.1.x` compatible with `Browser4 4.5.x - 4.6.x`

### 1.3 Version File Management

Files requiring version synchronization:

```
sdks/browser4-python/
├── pyproject.toml        # version = "X.Y.Z"
├── setup.cfg             # version = X.Y.Z
├── browser4/__init__.py  # __version__ = "X.Y.Z"
├── CHANGELOG.md          # Update changelog
└── README.md             # Update version examples
```

---

## 2. Pre-Release Preparation

### 2.1 Feature Completion Checklist

- [ ] All planned features implemented and code-reviewed
- [ ] API interface stable, no planned breaking changes
- [ ] All public APIs have complete docstrings
- [ ] Example code updated and verified working

### 2.2 Testing Checklist

#### Unit Tests
```bash
cd sdks/browser4-python
uv run pytest -m "not integration" --cov=browser4 --cov-report=term-missing
```

**Requirements:**
- [ ] All unit tests pass
- [ ] Code coverage ≥ 80%
- [ ] No skipped tests (unless clearly justified)

#### Integration Tests
```bash
cd sdks/browser4-python
uv run pytest -m integration -v -s
```

**Requirements:**
- [ ] All integration tests pass
- [ ] Tests cover all major use cases:
  - Browser4Driver automatic download and startup
  - PulsarSession basic operations
  - AgenticSession AI capabilities
  - WebDriver element interactions
  - Error handling and edge cases

#### Compatibility Tests

Test Python version compatibility:
```bash
# Using tox or manual testing
for version in 3.9 3.10 3.11 3.12; do
    echo "Testing Python $version"
    python$version -m pytest
done
```

**Requirements:**
- [ ] Python 3.9 tests pass
- [ ] Python 3.10 tests pass
- [ ] Python 3.11 tests pass
- [ ] Python 3.12 tests pass

#### Platform Tests

**Requirements:**
- [ ] Linux (Ubuntu 20.04+) tests pass
- [ ] macOS (11+) tests pass
- [ ] Windows (10/11) tests pass

#### End-to-End Tests

**Requirements:**
- [ ] Run all example code with real websites
- [ ] Verify README quick start guide
- [ ] Verify all examples in examples/ directory

### 2.3 Documentation Checklist

#### Code Documentation
- [ ] All public classes and functions have complete docstrings
- [ ] Docstrings follow Google or NumPy style
- [ ] Type annotations complete and accurate

#### User Documentation
- [ ] README.md accurately describes installation and usage
- [ ] Quick start guide is directly runnable
- [ ] API reference documentation complete (or link to generated docs)
- [ ] Example code covers major use cases
- [ ] CHANGELOG.md lists all changes

#### Documentation Site (Optional, Future Enhancement)
```bash
cd sdks/browser4-python/docs
mkdocs build
mkdocs serve  # Local preview
```

### 2.4 Dependency Review

#### Security Scanning
```bash
# Use pip-audit to check for known vulnerabilities
uv pip install pip-audit
uv run pip-audit

# Or use safety
uv pip install safety
uv run safety check
```

**Requirements:**
- [ ] No known security vulnerabilities
- [ ] All dependency versions explicitly specified
- [ ] Dependency licenses compatible (MIT/Apache 2.0/BSD)

#### Dependency Versions
- [ ] Minimum version requirements accurate (verified through testing)
- [ ] Avoid overly strict version constraints (unless compatibility issues exist)
- [ ] Core dependencies minimized

### 2.5 Version Updates

#### Update Version Numbers
```bash
# Manual editing or using scripts
vi pyproject.toml    # version = "0.1.0"
vi setup.cfg         # version = 0.1.0
vi browser4/__init__.py  # __version__ = "0.1.0"
```

#### Update CHANGELOG.md

Create `CHANGELOG.md` (if it doesn't exist):
```markdown
# Changelog

All notable changes to browser4-python will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0] - 2026-02-XX

### Added
- Initial release of browser4-python
- Browser4Driver for automatic server management
- PulsarClient for low-level HTTP API communication
- PulsarSession for page loading and data extraction
- AgenticSession for AI-powered browser automation
- WebDriver for browser control and element interaction
- Comprehensive test suite (unit + integration)
- Examples for common use cases

### Features
- Automatic Browser4.jar download and startup
- Session management (create, delete)
- Page loading with cache control
- CSS selector-based data extraction
- Natural language browser actions (act, run, observe)
- WebDriver-compatible API (click, fill, scroll, etc.)
- Screenshot capture
- JavaScript execution

### Documentation
- Complete README with quick start guide
- API reference for all public classes
- Multiple example scripts
- API comparison with Kotlin SDK

[0.1.0]: https://github.com/platonai/Browser4/releases/tag/python-sdk-v0.1.0
```

#### Commit Version Changes
```bash
cd sdks/browser4-python
git add pyproject.toml setup.cfg browser4/__init__.py CHANGELOG.md README.md
git commit -m "chore(python-sdk): Prepare release 0.1.0"
```

---

## 3. Building and Packaging

### 3.1 Build Distribution Packages

#### Clean Old Builds
```bash
cd sdks/browser4-python
rm -rf dist/ build/ *.egg-info
```

#### Use build Tool
```bash
# Install build tools
uv pip install build

# Build wheel and sdist
uv run python -m build

# Output:
# dist/
#   ├── browser4_sdk-0.1.0-py3-none-any.whl  # Wheel package
#   └── browser4_sdk-0.1.0.tar.gz             # Source distribution
```

#### Verify Package Contents
```bash
# Check wheel contents
unzip -l dist/browser4_sdk-0.1.0-py3-none-any.whl

# Check sdist contents
tar -tzf dist/browser4_sdk-0.1.0.tar.gz

# Verify metadata
uv pip install pkginfo
pkginfo dist/browser4_sdk-0.1.0-py3-none-any.whl
```

**Verify includes:**
- [ ] All Python source files
- [ ] LICENSE file
- [ ] README.md
- [ ] pyproject.toml / setup.cfg
- [ ] No extraneous test files or temporary files

### 3.2 Local Test Installation

#### Create Clean Virtual Environment
```bash
# Using uv
uv venv test-env
source test-env/bin/activate  # Windows: test-env\Scripts\activate

# Or using venv
python -m venv test-env
source test-env/bin/activate
```

#### Install from Built Package
```bash
# Install wheel
pip install dist/browser4_sdk-0.1.0-py3-none-any.whl

# Verify import
python -c "import browser4; print(browser4.__version__)"

# Run quick test
python -c "
from browser4 import PulsarClient, AgenticSession
print('Import successful!')
"
```

#### Test Uninstall and Reinstall
```bash
pip uninstall -y browser4-sdk
pip install dist/browser4_sdk-0.1.0.tar.gz  # Test sdist
python -c "import browser4; print(browser4.__version__)"
```

---

## 4. Release Process

### 4.1 Test Release to TestPyPI

TestPyPI is PyPI's testing environment for validating the release process.

#### Configure TestPyPI Credentials
```bash
# Method 1: Use .pypirc file
cat > ~/.pypirc << EOF
[distutils]
index-servers =
    testpypi
    pypi

[testpypi]
repository = https://test.pypi.org/legacy/
username = __token__
password = pypi-AgEIcHlwaS5vcmc...  # TestPyPI API Token

[pypi]
repository = https://upload.pypi.org/legacy/
username = __token__
password = pypi-AgEIcHlwaS5vcmc...  # PyPI API Token
EOF

chmod 600 ~/.pypirc

# Method 2: Use environment variables
export TWINE_USERNAME=__token__
export TWINE_PASSWORD=pypi-AgEIcHlwaS5vcmc...
export TWINE_REPOSITORY=testpypi
```

#### Upload to TestPyPI
```bash
# Install twine
uv pip install twine

# Check package integrity
twine check dist/*

# Upload to TestPyPI
twine upload --repository testpypi dist/*

# Or specify URL directly
twine upload --repository-url https://test.pypi.org/legacy/ dist/*
```

#### Install and Verify from TestPyPI
```bash
# Create new test environment
uv venv test-testpypi
source test-testpypi/bin/activate

# Install from TestPyPI
pip install --index-url https://test.pypi.org/simple/ \
    --extra-index-url https://pypi.org/simple/ \
    browser4-python==0.1.0

# Verify
python -c "import browser4; print(browser4.__version__)"

# Run simple test
python examples/basic_usage.py
```

**Verification Checklist:**
- [ ] Version number correct
- [ ] Dependencies installed correctly
- [ ] Import works without errors
- [ ] Basic functionality available
- [ ] Example code runs

### 4.2 Official Release to PyPI

⚠️ **Warning: Publishing to PyPI cannot be undone, only yanked. Ensure everything is ready.**

#### Final Checklist
- [ ] TestPyPI tests all passed
- [ ] CHANGELOG.md updated
- [ ] Version number finalized
- [ ] Code merged to main branch
- [ ] Git tag created

#### Create Git Tag
```bash
cd /path/to/Browser4
git checkout main  # or master
git pull

# Create annotated tag
git tag -a python-sdk-v0.1.0 -m "Release browser4-python v0.1.0"

# Push tag
git push origin python-sdk-v0.1.0
```

#### Upload to PyPI
```bash
cd sdks/browser4-python

# Ensure dist/ only contains current version
rm -rf dist/
uv run python -m build

# Final check
twine check dist/*

# Upload to PyPI
twine upload dist/*

# Example output:
# Uploading distributions to https://upload.pypi.org/legacy/
# Uploading browser4_sdk-0.1.0-py3-none-any.whl
# Uploading browser4_sdk-0.1.0.tar.gz
# View at: https://pypi.org/project/browser4-sdk/0.1.0/
```

### 4.3 Create GitHub Release

#### Via GitHub Web Interface
1. Visit https://github.com/platonai/Browser4/releases/new
2. Select tag: `python-sdk-v0.1.0`
3. Release title: `browser4-python v0.1.0`
4. Description: Copy content from CHANGELOG.md
5. Attachments: Upload files from `dist/` directory (optional)
6. Check "Set as the latest release" or "Set as a pre-release"
7. Click "Publish release"

#### Via GitHub CLI
```bash
# Install gh CLI (if needed)
# brew install gh  # macOS
# apt install gh   # Ubuntu

# Authenticate
gh auth login

# Create Release
gh release create python-sdk-v0.1.0 \
  --title "browser4-python v0.1.0" \
  --notes-file sdks/browser4-python/CHANGELOG.md \
  dist/browser4_sdk-0.1.0-py3-none-any.whl \
  dist/browser4_sdk-0.1.0.tar.gz

# If pre-release
gh release create python-sdk-v0.1.0-rc.1 \
  --title "browser4-python v0.1.0 Release Candidate 1" \
  --notes-file sdks/browser4-python/CHANGELOG.md \
  --prerelease \
  dist/*
```

### 4.4 Verify Release

#### Verify PyPI Page
- [ ] Visit https://pypi.org/project/browser4-python/
- [ ] Confirm version number correct
- [ ] Check project description renders correctly (README.md)
- [ ] Verify metadata (author, license, project links)
- [ ] Confirm dependency list correct

#### Verify Installation
```bash
# Test in fresh environment
uv venv fresh-install
source fresh-install/bin/activate

# Install released version
pip install browser4-python==0.1.0

# Verify
python -c "
import browser4
print(f'Version: {browser4.__version__}')
print('✓ Installation successful')
"

# Run example
python -c "
from browser4 import PulsarClient
client = PulsarClient(base_url='http://localhost:8182')
print('✓ Import successful')
"
```

---

## 5. Automated Release (CI/CD)

### 5.1 GitHub Actions Workflow

Create `.github/workflows/python-sdk-release.yml`:

```yaml
name: Release Python SDK

on:
  push:
    tags:
      - 'python-sdk-v[0-9]+.[0-9]+.[0-9]+'
      - 'python-sdk-v[0-9]+.[0-9]+.[0-9]+-*'
  workflow_dispatch:
    inputs:
      version:
        description: 'Version to release (e.g., 0.1.0)'
        required: true
        type: string
      dry_run:
        description: 'Dry run (TestPyPI only)'
        required: false
        default: false
        type: boolean

permissions:
  contents: write
  id-token: write  # For PyPI trusted publishing

jobs:
  build:
    name: Build Distribution
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Set up Python
        uses: actions/setup-python@v5
        with:
          python-version: '3.11'

      - name: Install uv
        run: |
          curl -LsSf https://astral.sh/uv/install.sh | sh
          echo "$HOME/.cargo/bin" >> $GITHUB_PATH

      - name: Install build tools
        run: uv pip install --system build twine

      - name: Build package
        run: |
          cd sdks/browser4-python
          python -m build

      - name: Check package
        run: |
          cd sdks/browser4-python
          twine check dist/*

      - name: Upload artifacts
        uses: actions/upload-artifact@v4
        with:
          name: python-package-distributions
          path: sdks/browser4-python/dist/

  test-package:
    name: Test Package Installation
    needs: build
    runs-on: ${{ matrix.os }}
    strategy:
      matrix:
        os: [ubuntu-latest, macos-latest, windows-latest]
        python-version: ['3.9', '3.10', '3.11', '3.12']

    steps:
      - uses: actions/checkout@v4

      - name: Set up Python ${{ matrix.python-version }}
        uses: actions/setup-python@v5
        with:
          python-version: ${{ matrix.python-version }}

      - name: Download artifacts
        uses: actions/download-artifact@v4
        with:
          name: python-package-distributions
          path: dist/

      - name: Test wheel installation
        run: |
          python -m pip install dist/*.whl
          python -c "import browser4; print(f'✓ browser4-sdk {browser4.__version__}')"

  publish-testpypi:
    name: Publish to TestPyPI
    needs: [build, test-package]
    runs-on: ubuntu-latest
    if: github.event.inputs.dry_run == 'true'

    environment:
      name: testpypi
      url: https://test.pypi.org/project/browser4-python/

    steps:
      - name: Download artifacts
        uses: actions/download-artifact@v4
        with:
          name: python-package-distributions
          path: dist/

      - name: Publish to TestPyPI
        uses: pypa/gh-action-pypi-publish@release/v1
        with:
          repository-url: https://test.pypi.org/legacy/
          packages-dir: dist/

  publish-pypi:
    name: Publish to PyPI
    needs: [build, test-package]
    runs-on: ubuntu-latest
    if: startsWith(github.ref, 'refs/tags/python-sdk-v') && github.event.inputs.dry_run != 'true'

    environment:
      name: pypi
      url: https://pypi.org/project/browser4-python/

    permissions:
      id-token: write  # IMPORTANT: mandatory for trusted publishing

    steps:
      - name: Download artifacts
        uses: actions/download-artifact@v4
        with:
          name: python-package-distributions
          path: dist/

      - name: Publish to PyPI
        uses: pypa/gh-action-pypi-publish@release/v1
        with:
          packages-dir: dist/

  create-release:
    name: Create GitHub Release
    needs: publish-pypi
    runs-on: ubuntu-latest

    permissions:
      contents: write

    steps:
      - uses: actions/checkout@v4

      - name: Download artifacts
        uses: actions/download-artifact@v4
        with:
          name: python-package-distributions
          path: dist/

      - name: Extract version from tag
        id: version
        run: |
          VERSION=${GITHUB_REF#refs/tags/python-sdk-v}
          echo "version=$VERSION" >> $GITHUB_OUTPUT

      - name: Create Release
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        run: |
          cd sdks/browser4-python
          gh release create ${{ github.ref_name }} \
            --title "browser4-python v${{ steps.version.outputs.version }}" \
            --notes-file CHANGELOG.md \
            dist/*
```

### 5.2 PyPI Trusted Publishing Configuration

Recommended: Use PyPI's Trusted Publishing feature for secure, token-free publishing.

#### Configuration Steps
1. Log in to PyPI: https://pypi.org/
2. Go to project settings: https://pypi.org/manage/project/browser4-python/settings/
3. Click "Publishing" → "Add a new publisher"
4. Fill in information:
   - **PyPI Project Name**: `browser4-python`
   - **Owner**: `platonai`
   - **Repository**: `Browser4`
   - **Workflow name**: `python-sdk-release.yml`
   - **Environment name**: `pypi`
5. Save

Now GitHub Actions can publish to PyPI without tokens.

### 5.3 Automated Release Triggers

#### Method 1: Push Git Tag (Recommended)
```bash
git tag python-sdk-v0.1.0
git push origin python-sdk-v0.1.0
```

#### Method 2: Manual Workflow Trigger
```bash
gh workflow run python-sdk-release.yml \
  -f version=0.1.0 \
  -f dry_run=false
```

#### Method 3: GitHub Web Interface
1. Visit Actions page
2. Select "Release Python SDK" workflow
3. Click "Run workflow"
4. Fill in version and options
5. Click "Run workflow"

---

## 6. Post-Release Activities

### 6.1 Announcements and Communication

#### Update Project Documentation
- [ ] Update Python SDK installation instructions in main README.md
- [ ] Update relevant documentation in `docs/` directory
- [ ] Add release announcement to project website (if applicable)

#### Social Media and Community
- [ ] Post on Twitter/X
- [ ] Share on Reddit (r/Python, r/webscraping)
- [ ] Submit to Hacker News
- [ ] Submit to Python Weekly / Awesome Python resource lists
- [ ] Announce in project Discord/Slack channels

#### User Notification
```markdown
📢 **browser4-python v0.1.0 Released!**

We're excited to announce the first official release of browser4-python!

🎉 **Key Features:**
- Automatic Browser4 server management
- AI-powered browser automation
- WebDriver-compatible API
- Rich data extraction capabilities

📦 **Installation:**
```bash
pip install browser4-python
```

📖 **Documentation:**
https://github.com/platonai/Browser4/tree/main/sdks/browser4-python

🙏 Thank you to all contributors!
```

### 6.2 Monitoring and Feedback

#### PyPI Download Statistics
- Monitor PyPI downloads: https://pypistats.org/packages/browser4-python
- Use pypistats tool:
  ```bash
  pip install pypistats
  pypistats recent browser4-python
  pypistats overall browser4-python --monthly
  ```

#### GitHub Activity Monitoring
- [ ] Monitor Issues (bug reports, feature requests)
- [ ] Monitor Pull Requests
- [ ] Monitor GitHub Stars growth
- [ ] Set up GitHub Watch notifications

#### User Feedback Channels
- [ ] GitHub Issues: Technical problems and bug reports
- [ ] GitHub Discussions: General discussion and Q&A
- [ ] Discord/Slack: Real-time community support
- [ ] Email: Direct contact with maintainers

### 6.3 Bug Fixes and Patch Releases

If critical bugs are discovered, quickly release a patch version:

```bash
# 1. Fix bug
vi browser4/client.py

# 2. Update version (PATCH +1)
vi pyproject.toml  # 0.1.0 → 0.1.1
vi setup.cfg
vi browser4/__init__.py

# 3. Update CHANGELOG
vi CHANGELOG.md

# 4. Commit and tag
git add .
git commit -m "fix(python-sdk): Fix critical bug in client connection"
git tag python-sdk-v0.1.1
git push origin main python-sdk-v0.1.1

# 5. Automatic release (via CI/CD)
# Or manual release
python -m build
twine upload dist/*
```

### 6.4 Next Version Planning

#### Create Roadmap
Plan next version in GitHub Project or Issues:

**v0.2.0 Planned Features:**
- [ ] Complete PageEventHandlers implementation
- [ ] More examples and tutorials
- [ ] Performance optimizations
- [ ] Better error handling
- [ ] CLI tool support

#### Update Development Version
```bash
# Update to next development version
vi pyproject.toml  # version = "0.2.0-dev"
vi setup.cfg       # version = 0.2.0.dev0
vi browser4/__init__.py  # __version__ = "0.2.0-dev"

git add .
git commit -m "chore(python-sdk): Bump version to 0.2.0-dev"
git push
```

---

## 7. Long-Term Maintenance Strategy

### 7.1 Version Support Policy

| Version Type | Support Duration | Support Content |
|--------------|-----------------|-----------------|
| Latest stable | Ongoing | New features, bug fixes, security updates |
| Previous stable | 6 months | Critical bug fixes, security updates |
| Older versions | Best effort | Security updates only |

**Example:**
- After v0.3.0 release:
  - v0.3.x: Full support
  - v0.2.x: Critical fixes for 6 months
  - v0.1.x: Security updates only

### 7.2 Release Cadence

Suggested release rhythm:

- **PATCH releases**: As needed (bug fixes, security updates)
- **MINOR releases**: Every 2-3 months (new features, improvements)
- **MAJOR releases**: Every 6-12 months (breaking changes, API overhauls)

### 7.3 Backwards Compatibility

#### Compatibility Promise
- **MAJOR versions**: Allow incompatible API changes
- **MINOR versions**: Must be backwards compatible
- **PATCH versions**: Must be 100% compatible

#### Deprecation Process
When API removal or changes are needed:

1. **Mark as Deprecated**
   ```python
   import warnings

   def old_function():
       warnings.warn(
           "old_function is deprecated and will be removed in v2.0.0. "
           "Use new_function instead.",
           DeprecationWarning,
           stacklevel=2
       )
       # ... existing implementation
   ```

2. **Update Documentation**
   - Mark in docstring with `.. deprecated:: 0.2.0`
   - List in CHANGELOG
   - Add migration guide to README

3. **Keep for at least one MINOR version**
   - v0.2.0: Mark deprecated, keep functionality
   - v0.3.0: Continue keeping, issue warnings
   - v1.0.0: Can remove

### 7.4 Security Update Process

#### Vulnerability Reporting
- Create SECURITY.md file explaining how to report security issues
- Provide security email: security@example.com
- Don't discuss security issues in public Issues

#### Security Patch Release
```bash
# 1. Fix in private branch
git checkout -b security/CVE-2024-XXXX

# 2. Fix and test
# ... fix code ...

# 3. Release patch versions
# v0.1.1, v0.2.1 (supported versions)

# 4. Coordinate public disclosure
# Publicly disclose vulnerability details after fix release
```

---

## 8. Tools and Resources

### 8.1 Required Tools

| Tool | Purpose | Install Command |
|------|---------|----------------|
| uv | Package and environment management | `curl -LsSf https://astral.sh/uv/install.sh \| sh` |
| build | Build distribution packages | `uv pip install build` |
| twine | Upload to PyPI | `uv pip install twine` |
| pytest | Testing framework | `uv pip install pytest` |
| gh | GitHub CLI | `brew install gh` / `apt install gh` |

### 8.2 Optional Tools

| Tool | Purpose | Install Command |
|------|---------|----------------|
| tox | Multi-version testing | `uv pip install tox` |
| mypy | Type checking | `uv pip install mypy` |
| ruff | Linting and formatting | `uv pip install ruff` |
| pip-audit | Security scanning | `uv pip install pip-audit` |
| mkdocs | Documentation generation | `uv pip install mkdocs mkdocs-material` |
| pypistats | PyPI statistics | `pip install pypistats` |

### 8.3 Reference Resources

#### Official PyPI Documentation
- Packaging Guide: https://packaging.python.org/
- PyPI User Guide: https://pypi.org/help/
- Trusted Publishing: https://docs.pypi.org/trusted-publishers/

#### Best Practices
- Python Packaging Authority (PyPA): https://www.pypa.io/
- Semantic Versioning: https://semver.org/
- Keep a Changelog: https://keepachangelog.com/

#### Community Resources
- Python Packaging Discord: https://discord.gg/pypa
- r/Python: https://reddit.com/r/Python
- Python Weekly: https://www.pythonweekly.com/

---

## 9. Checklist Summary

### Pre-Release Checklist

#### Code Quality
- [ ] All unit tests pass (coverage ≥ 80%)
- [ ] All integration tests pass
- [ ] Code review completed
- [ ] No known critical bugs
- [ ] Security scan passed

#### Documentation
- [ ] README.md complete and accurate
- [ ] API documentation complete (docstrings)
- [ ] CHANGELOG.md updated
- [ ] Example code runnable

#### Version Management
- [ ] Version number updated (pyproject.toml, setup.cfg, __init__.py)
- [ ] Git tag created
- [ ] Code merged to main branch

#### Build and Package
- [ ] `python -m build` successful
- [ ] `twine check dist/*` passed
- [ ] Local installation test passed

#### Test Release
- [ ] TestPyPI upload successful
- [ ] Installation from TestPyPI verified

### Release Checklist

- [ ] PyPI upload successful
- [ ] PyPI page displays correctly
- [ ] Installation from PyPI verified
- [ ] GitHub Release created
- [ ] Release announcement published

### Post-Release Checklist

- [ ] Monitor PyPI downloads
- [ ] Monitor GitHub Issues
- [ ] Respond to user feedback
- [ ] Plan next version
- [ ] Update project documentation

---

## 10. Troubleshooting

### 10.1 Common Issues

#### Build Failures
```bash
# Issue: Module not found
# Solution: Ensure __init__.py exists
find browser4 -name __init__.py

# Issue: Files not included
# Solution: Check MANIFEST.in or pyproject.toml [tool.setuptools]
```

#### Upload Failures
```bash
# Issue: 403 Forbidden
# Solution: Check API token permissions and project name

# Issue: 400 Bad Request - File already exists
# Solution: Version already exists, need to increment version

# Issue: Network timeout
# Solution: Retry or use proxy
twine upload --repository-url https://upload.pypi.org/legacy/ dist/* --verbose
```

#### Installation Failures
```bash
# Issue: Dependency conflicts
# Solution: Relax dependency version constraints
# pyproject.toml: requests>=2.31.0 not requests==2.31.0

# Issue: Python version incompatible
# Solution: Check requires-python setting
```

### 10.2 Rollback Strategy

#### PyPI Packages Cannot Be Deleted
- PyPI doesn't allow deleting published versions
- Can use `yank` to mark version as not recommended
  ```bash
  # Via PyPI web interface, or use API
  twine upload --skip-existing dist/*  # Skip existing
  ```

#### Released Wrong Version
1. Immediately release fix version (PATCH +1)
2. Yank wrong version on PyPI project page
3. Mark wrong version in GitHub Release
4. Notify users to upgrade to fix version

---

## Summary

This release plan provides a complete guide for browser4-python from development to release and maintenance. Key points:

1. **Semantic Versioning**: Follow SemVer 2.0.0, independent from main project
2. **Rigorous Testing**: Unit tests, integration tests, compatibility tests
3. **Automated Release**: Use GitHub Actions and PyPI Trusted Publishing
4. **Ongoing Maintenance**: Monitor feedback, quick fixes, regular iterations
5. **Security First**: Vulnerability scanning, security patches, responsible disclosure

Following this plan ensures high-quality, stable, and reliable Python SDK releases.

**Next Steps:**
1. Complete current feature development
2. Run complete test suite
3. Configure PyPI Trusted Publishing
4. Create automated workflow
5. Execute first release (v0.1.0)

Happy releasing! 🚀
