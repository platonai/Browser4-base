#!/bin/bash
# Serve the MkDocs documentation site locally

set -e

# Change to the docs directory
cd "$(dirname "$0")"

echo "Serving Browser4 Python SDK documentation..."

# Install dependencies if needed
if ! command -v mkdocs &> /dev/null; then
    echo "Installing mkdocs dependencies..."
    pip install -r requirements.txt
fi

# Serve the site
echo "Starting local server..."
echo "Documentation will be available at: http://127.0.0.1:8000"
echo "Press Ctrl+C to stop the server"
echo ""

mkdocs serve
