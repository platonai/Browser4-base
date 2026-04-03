#!/bin/bash
# Deploy the MkDocs documentation site to GitHub Pages

set -e

# Change to the docs directory
cd "$(dirname "$0")"

echo "Deploying Browser4 Python SDK documentation to GitHub Pages..."

# Install dependencies if needed
if ! command -v mkdocs &> /dev/null; then
    echo "Installing mkdocs dependencies..."
    pip install -r requirements.txt
fi

# Deploy to gh-pages branch
echo "Building and deploying..."
mkdocs gh-deploy --force

echo "✓ Documentation deployed successfully!"
echo "Site will be available at: https://platonai.github.io/Browser4/"
