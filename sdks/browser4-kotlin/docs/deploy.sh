#!/bin/bash
# Deploy script for Browser4 Kotlin SDK documentation to GitHub Pages

set -e

echo "===== Browser4 Kotlin SDK Documentation Deployer ====="
echo ""

# Check if virtual environment exists
if [ ! -d "venv" ]; then
    echo "Virtual environment not found. Running build.sh first..."
    ./build.sh
fi

# Activate virtual environment
source venv/bin/activate

# Deploy to GitHub Pages
echo "Deploying to GitHub Pages..."
mkdocs gh-deploy --force

echo ""
echo "===== Deployment Complete ====="
echo "Documentation is now available on GitHub Pages."
