# Contributing to Browser4 Python SDK

Thank you for your interest in contributing to the Browser4 Python SDK! This document provides guidelines and instructions for contributing.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Making Changes](#making-changes)
- [Testing](#testing)
- [Documentation](#documentation)
- [Submitting Changes](#submitting-changes)
- [Code Style](#code-style)
- [Release Process](#release-process)

---

## Code of Conduct

By participating in this project, you agree to maintain a respectful and inclusive environment. Be kind, professional, and constructive in all interactions.

---

## Getting Started

### Prerequisites

- Python 3.9 or higher
- Git
- Browser4 server (for testing)
- Basic knowledge of web scraping and browser automation

### Finding Issues to Work On

1. Check the [issue tracker](https://github.com/platonai/Browser4/issues)
2. Look for issues labeled `good first issue` or `help wanted`
3. Comment on the issue to indicate you're working on it
4. For major changes, open an issue first to discuss

---

## Development Setup

### 1. Fork and Clone

```bash
# Fork the repository on GitHub
# Then clone your fork
git clone https://github.com/YOUR-USERNAME/Browser4.git
cd Browser4/sdks/python-sdk
```

### 2. Create Virtual Environment

```bash
# Create virtual environment
python -m venv venv

# Activate it
source venv/bin/activate  # On Windows: venv\Scripts\activate
```

### 3. Install in Editable Mode

```bash
# Install package with dev dependencies
pip install -e .[dev]

# Verify installation
python -c "import pulsar_sdk; print(pulsar_sdk.__version__)"
```

### 4. Start Browser4 Server

```bash
# Option 1: Using Docker
docker run -p 8182:8182 platonai/browser4:latest

# Option 2: From source (in repository root)
./mvnw clean package -DskipTests
java -jar pulsar-rest/target/pulsar-rest-*.jar
```

### 5. Verify Setup

```bash
# Run tests
pytest

# Check server connection
python -c "from pulsar_sdk import PulsarClient; print(PulsarClient().create_session())"
```

---

## Making Changes

### 1. Create a Branch

```bash
# Create a feature branch
git checkout -b feature/your-feature-name

# Or a bugfix branch
git checkout -b fix/issue-description
```

### 2. Make Your Changes

Follow these guidelines:

- **Keep changes focused**: One feature or fix per PR
- **Write clear code**: Use descriptive names and comments
- **Add tests**: Cover new functionality with tests
- **Update docs**: Document new features or changes
- **Follow style**: Maintain consistent code style

### 3. Commit Changes

```bash
# Stage your changes
git add .

# Commit with clear message
git commit -m "Add feature: description of what was added"

# Good commit messages:
# - "Fix: Handle None values in extract()"
# - "Add: Support for custom headers in WebDriver"
# - "Docs: Add examples for agent_extract()"
# - "Test: Add tests for session management"
```

---

## Testing

### Running Tests

```bash
# Run all tests
pytest

# Run specific test file
pytest tests/test_client.py

# Run with coverage
pytest --cov=pulsar_sdk --cov-report=html

# Run specific test
pytest tests/test_client.py::TestPulsarClient::test_create_session
```

### Writing Tests

Place tests in the `tests/` directory:

```python
# tests/test_new_feature.py
import pytest
from pulsar_sdk import PulsarClient, AgenticSession


class TestNewFeature:
    """Tests for new feature."""
    
    def test_basic_functionality(self):
        """Test basic functionality of new feature."""
        client = PulsarClient()
        client.create_session()
        session = AgenticSession(client)
        
        # Your test here
        result = session.new_feature()
        assert result is not None
        
        session.close()
    
    def test_error_handling(self):
        """Test error handling."""
        with pytest.raises(ValueError):
            # Test that should raise error
            pass
```

### Test Guidelines

- **Test edge cases**: Empty inputs, None values, errors
- **Use fixtures**: For common setup code
- **Mock external calls**: Don't depend on external services
- **Be specific**: Test one thing per test method
- **Use descriptive names**: `test_extract_returns_none_for_missing_field`

### Integration Tests

For tests requiring Browser4 server:

```python
import pytest
from pulsar_sdk import PulsarClient

@pytest.mark.integration
def test_integration_with_server():
    """Test that requires running Browser4 server."""
    client = PulsarClient()
    # Skip if server not available
    try:
        session_id = client.create_session()
    except:
        pytest.skip("Browser4 server not available")
    
    # Test code...
    client.delete_session()
```

Run integration tests:

```bash
# Run all tests including integration
pytest

# Run only integration tests
pytest -m integration

# Skip integration tests
pytest -m "not integration"
```

---

## Documentation

### Updating Documentation

When making changes:

1. **Update docstrings** for new/modified functions:
   ```python
   def new_function(param1: str, param2: int = 10) -> dict:
       """
       Short description of what function does.
       
       Longer description if needed, explaining behavior,
       edge cases, and usage patterns.
       
       Args:
           param1: Description of param1.
           param2: Description of param2. Default: 10.
           
       Returns:
           Dictionary containing result data.
           
       Raises:
           ValueError: If param1 is empty.
           
       Example:
           >>> result = new_function("test", 20)
           >>> print(result)
           {'status': 'success'}
       """
       # Implementation...
   ```

2. **Update README.md** if adding major features

3. **Update relevant docs/** pages:
   - Add examples to appropriate guides
   - Update API reference if needed
   - Add troubleshooting tips for common issues

### Building Documentation

```bash
# Install docs dependencies
pip install -r docs-requirements.txt

# Build docs
mkdocs build

# Serve docs locally
mkdocs serve
# Open http://127.0.0.1:8000 in browser

# Check for broken links
mkdocs build --strict
```

### Documentation Style

- Use clear, concise language
- Include working code examples
- Show expected output
- Document edge cases and gotchas
- Cross-reference related functionality

---

## Submitting Changes

### 1. Push Your Branch

```bash
# Push to your fork
git push origin feature/your-feature-name
```

### 2. Create Pull Request

1. Go to the [Browser4 repository](https://github.com/platonai/Browser4)
2. Click "New Pull Request"
3. Select your fork and branch
4. Fill out the PR template:

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
- [ ] Tests added/updated
- [ ] All tests passing
- [ ] Manual testing performed

## Checklist
- [ ] Code follows style guidelines
- [ ] Documentation updated
- [ ] No new warnings introduced
- [ ] Commit messages are clear
```

### 3. Code Review

- Address reviewer feedback promptly
- Push additional commits to your branch
- Discuss concerns or questions
- Be open to suggestions

### 4. After Merge

```bash
# Update your fork
git checkout main
git pull upstream main
git push origin main

# Delete feature branch
git branch -d feature/your-feature-name
git push origin --delete feature/your-feature-name
```

---

## Code Style

### Python Style Guide

Follow [PEP 8](https://pep8.org/) with these specifics:

#### Formatting

```python
# Use 4 spaces for indentation (no tabs)
def function_name(param1, param2):
    result = param1 + param2
    return result

# Maximum line length: 100 characters
long_string = (
    "This is a very long string that exceeds "
    "100 characters so we break it into multiple lines"
)

# Blank lines: 2 between top-level definitions
class MyClass:
    pass


def my_function():
    pass
```

#### Naming Conventions

```python
# Classes: PascalCase
class SessionManager:
    pass

# Functions/methods: snake_case
def load_page(url):
    pass

# Constants: UPPER_SNAKE_CASE
DEFAULT_TIMEOUT = 30.0
API_VERSION = "v1"

# Private attributes/methods: _leading_underscore
class MyClass:
    def __init__(self):
        self._private_attr = None
    
    def _private_method(self):
        pass
```

#### Type Hints

Always use type hints:

```python
from typing import Optional, List, Dict, Any

def extract_data(
    page: WebPage,
    selectors: Dict[str, str],
    timeout: Optional[int] = None
) -> Dict[str, Any]:
    """Extract data from page."""
    # Implementation...
```

#### Docstrings

Use Google-style docstrings:

```python
def function(arg1: str, arg2: int = 0) -> bool:
    """
    Short description (imperative mood: "Do something").
    
    Longer description with details, usage notes, and examples.
    
    Args:
        arg1: Description of arg1.
        arg2: Description of arg2. Default: 0.
        
    Returns:
        Description of return value.
        
    Raises:
        ValueError: When validation fails.
        
    Example:
        >>> result = function("test", 10)
        >>> print(result)
        True
    """
    pass
```

#### Imports

```python
# Standard library
import os
import sys
from typing import Optional, Dict

# Third-party
import requests
from bs4 import BeautifulSoup

# Local
from .client import PulsarClient
from .models import WebPage
```

### Code Linting

```bash
# Install linting tools
pip install flake8 black mypy

# Run linters
flake8 pulsar_sdk/
black pulsar_sdk/ --check
mypy pulsar_sdk/

# Auto-format code
black pulsar_sdk/
```

### Pre-commit Hooks (Optional)

```bash
# Install pre-commit
pip install pre-commit

# Set up hooks
pre-commit install

# Run manually
pre-commit run --all-files
```

---

## Release Process

(For maintainers)

### Version Numbering

Follow [Semantic Versioning](https://semver.org/):

- **MAJOR**: Breaking changes
- **MINOR**: New features (backwards compatible)
- **PATCH**: Bug fixes

### Creating a Release

1. **Update version**:
   ```python
   # pyproject.toml
   version = "0.2.0"
   
   # pulsar_sdk/__init__.py
   __version__ = "0.2.0"
   ```

2. **Update CHANGELOG.md**:
   ```markdown
   ## [0.2.0] - 2024-01-15
   
   ### Added
   - New feature X
   - Support for Y
   
   ### Changed
   - Improved performance of Z
   
   ### Fixed
   - Bug in feature A
   ```

3. **Commit and tag**:
   ```bash
   git add .
   git commit -m "Release version 0.2.0"
   git tag v0.2.0
   git push origin main --tags
   ```

4. **Create GitHub release**:
   - Go to Releases → New Release
   - Select tag
   - Copy changelog content
   - Publish release

---

## Questions?

- **General questions**: Open a discussion on GitHub
- **Bug reports**: Create an issue
- **Feature requests**: Create an issue with [Feature Request] label
- **Security issues**: Email maintainers privately

---

Thank you for contributing to Browser4 Python SDK! 🎉
