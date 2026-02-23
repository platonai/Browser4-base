#!/bin/bash
# Build script for Browser4 Kotlin SDK documentation

set -e

echo "===== Browser4 Kotlin SDK Documentation Builder ====="
echo ""

# Check if Python is installed
if ! command -v python3 &> /dev/null; then
    echo "Error: Python 3 is not installed. Please install Python 3.8 or later."
    exit 1
fi

# Check if pip is installed
if ! command -v pip3 &> /dev/null; then
    echo "Error: pip3 is not installed. Please install pip."
    exit 1
fi

# Create virtual environment if it doesn't exist
if [ ! -d "venv" ]; then
    echo "Creating virtual environment..."
    python3 -m venv venv
fi

# Activate virtual environment
echo "Activating virtual environment..."
source venv/bin/activate

# Install dependencies
echo "Installing MkDocs and dependencies..."
pip install -r requirements.txt

# Build documentation
echo ""
echo "Building documentation..."
mkdocs build

echo ""
echo "===== Build Complete ====="
echo "Documentation is available in: site/"
echo ""
echo "To preview the documentation:"
echo "  ./serve.sh"
echo ""
echo "To deploy to GitHub Pages:"
echo "  ./deploy.sh"
