# Browser4 Kotlin SDK Documentation

This directory contains the complete documentation for the Browser4 Kotlin SDK.

## Documentation Structure

The documentation is organized as follows:

- **Getting Started** - Installation, quick start, and first steps tutorials
- **User Guide** - Comprehensive guides for all SDK features
- **API Reference** - Complete API documentation for all classes
- **Examples** - Practical code examples for common use cases
- **Configuration** - Configuration options and environment setup
- **FAQ** - Frequently asked questions
- **中文文档** - Chinese language documentation

## Building the Documentation

### Prerequisites

- Python 3.8 or later
- pip (Python package manager)

### Quick Build

Run the build script:

```bash
./build.sh
```

This will:
1. Create a Python virtual environment
2. Install MkDocs and dependencies
3. Build the documentation to `site/`

### Preview Locally

To preview the documentation with live reloading:

```bash
./serve.sh
```

The documentation will be available at http://127.0.0.1:8000

### Deploy to GitHub Pages

To deploy the documentation to GitHub Pages:

```bash
./deploy.sh
```

## Manual Build

If you prefer to build manually:

```bash
# Create and activate virtual environment
python3 -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt

# Build documentation
mkdocs build

# Or serve locally
mkdocs serve
```

## Documentation Contents

### Getting Started
- **Introduction** - Overview of the SDK and its capabilities
- **Installation** - How to add the SDK to your project
- **Quick Start** - Your first script in minutes
- **First Steps** - Detailed tutorial on SDK basics

### User Guide
- **Session Management** - Creating and managing browser sessions
- **Navigation** - Navigating to URLs and controlling page loading
- **Element Interaction** - Clicking, typing, and interacting with elements
- **Data Extraction** - Extracting structured data from web pages
- **AI-Powered Automation** - Using natural language for automation
- **Screenshots** - Capturing screenshots of pages and elements
- **Script Execution** - Running JavaScript in the browser

### API Reference
- **Overview** - API architecture and design
- **PulsarClient** - Low-level HTTP client
- **PulsarSession** - Session management and data extraction
- **AgenticSession** - AI-powered automation
- **WebDriver** - Browser control and element interaction
- **Models** - Data models and types

### Examples
- **Basic Usage** - Simple examples to get started
- **Advanced Usage** - Complex automation patterns
- **AI Automation** - Natural language automation examples
- **Complete Workflow** - End-to-end workflow examples

### Configuration
- **Local Driver** - Configuring the local Browser4 driver
- **Remote Server** - Connecting to remote Browser4 servers
- **Environment Variables** - Environment configuration reference

## Contributing to Documentation

To contribute to the documentation:

1. Edit the Markdown files in `docs/`
2. Preview your changes with `./serve.sh`
3. Submit a pull request

## Documentation Style Guide

- Use code examples liberally
- Keep examples practical and runnable
- Include both simple and advanced examples
- Use admonitions (info, warning, tip) for important notes
- Cross-reference related topics
- Keep language clear and concise

## Technology Stack

- **MkDocs** - Static site generator
- **Material for MkDocs** - Documentation theme
- **Python Markdown** - Markdown processor
- **PyMdown Extensions** - Extended Markdown features

## Links

- [Main Repository](https://github.com/platonai/Browser4)
- [SDK Source Code](../src/main/kotlin)
- [Examples](../../../kotlin-sdk-examples)
- [Issue Tracker](https://github.com/platonai/Browser4/issues)

## License

The documentation is licensed under the same license as the Browser4 project: Apache License 2.0.
