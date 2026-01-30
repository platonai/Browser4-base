#!/bin/bash
# Build the MkDocs documentation site

set -e

# Change to the docs directory
cd "$(dirname "$0")"

echo "Building Browser4 Python SDK documentation..."

# Install dependencies if needed
if ! command -v mkdocs &> /dev/null; then
    echo "Installing mkdocs dependencies..."
    pip install -r requirements.txt
fi

# Build the site
echo "Building site..."
mkdocs build

echo "✓ Documentation built successfully!"
echo "Site generated in: site/"
echo ""
echo "To serve locally, run: ./serve.sh"
echo "To deploy, run: ./deploy.sh"
