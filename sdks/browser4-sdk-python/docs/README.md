# Browser4 Python SDK Documentation

This directory contains the comprehensive MkDocs documentation site for the Browser4 Python SDK.

## Quick Start

### Prerequisites

- Python 3.9 or higher
- pip or uv package manager

### Install Dependencies

```bash
# Using pip
pip install -r requirements.txt

# Using uv
uv pip install -r requirements.txt
```

### Serve Locally

```bash
# Start development server
./serve.sh

# Or manually
mkdocs serve
```

The documentation will be available at http://127.0.0.1:8000

### Build Static Site

```bash
# Build the site
./build.sh

# Or manually
mkdocs build
```

The built site will be in the `site/` directory.

### Deploy to GitHub Pages

```bash
# Deploy to gh-pages branch
./deploy.sh

# Or manually
mkdocs gh-deploy --force
```

## Documentation Structure

```
docs/
├── mkdocs.yml                 # MkDocs configuration
├── requirements.txt           # Python dependencies
├── build.sh                   # Build script
├── serve.sh                   # Local server script
├── deploy.sh                  # Deployment script
└── docs/                      # Documentation content
    ├── index.md               # Homepage
    ├── getting-started/       # Getting started guides
    │   ├── introduction.md
    │   ├── installation.md
    │   ├── quick-start.md
    │   └── first-steps.md
    ├── guide/                 # User guides
    │   ├── session-management.md
    │   ├── navigation.md
    │   ├── element-interaction.md
    │   ├── data-extraction.md
    │   ├── ai-automation.md
    │   ├── screenshots.md
    │   └── script-execution.md
    ├── api/                   # API reference
    │   ├── overview.md
    │   ├── browser4-driver.md
    │   ├── pulsar-client.md
    │   ├── pulsar-session.md
    │   ├── agentic-session.md
    │   ├── webdriver.md
    │   └── models.md
    ├── examples/              # Examples
    │   ├── basic-usage.md
    │   ├── advanced-usage.md
    │   ├── ai-automation.md
    │   └── complete-workflow.md
    ├── configuration/         # Configuration guides
    │   ├── browser4-driver.md
    │   ├── remote-server.md
    │   └── environment-variables.md
    ├── faq.md                # FAQ
    └── changelog.md          # Changelog
```

## Documentation Content

### Getting Started
Complete guides for new users covering installation, quick start, and first steps.

### User Guide
In-depth guides for all Browser4 features:
- Session management
- Page navigation
- Element interaction
- Data extraction
- AI-powered automation
- Screenshots
- Script execution

### API Reference
Complete API documentation for all classes:
- Browser4Driver
- PulsarClient
- PulsarSession
- AgenticSession
- WebDriver
- Data Models

### Examples
Practical examples from basic to advanced:
- Basic usage patterns
- Advanced workflows
- AI automation
- Complete end-to-end workflows

### Configuration
Configuration guides for different scenarios:
- Browser4Driver setup
- Remote server connections
- Environment variables

## Building the Docs

The documentation uses:
- **MkDocs** - Static site generator
- **Material for MkDocs** - Material Design theme
- **mkdocstrings** - Auto-generate API docs from Python source
- **PyMdown Extensions** - Additional markdown features

### Theme Features

- Light/dark mode toggle
- Instant loading
- Navigation tabs
- Search with suggestions
- Code highlighting with copy button
- Responsive design

### MkDocs Commands

```bash
# Start development server
mkdocs serve

# Build static site
mkdocs build

# Deploy to GitHub Pages
mkdocs gh-deploy

# Show version
mkdocs --version
```

## Contributing

To contribute to the documentation:

1. Edit markdown files in `docs/`
2. Test locally with `mkdocs serve`
3. Ensure all links work
4. Build successfully with `mkdocs build`
5. Submit a pull request

### Writing Style

- Use clear, concise language
- Include working code examples
- Add troubleshooting sections
- Link to related documentation
- Follow existing structure

### Code Examples

All code examples should:
- Be complete and runnable
- Include necessary imports
- Show expected output
- Include error handling where relevant
- Follow Python best practices

## Deployment

The documentation can be deployed to:

- **GitHub Pages** - Using `mkdocs gh-deploy`
- **Read the Docs** - Via `.readthedocs.yaml` configuration
- **Any static hosting** - Upload the `site/` directory

## License

Copyright © 2025 Platon AI. All rights reserved.
