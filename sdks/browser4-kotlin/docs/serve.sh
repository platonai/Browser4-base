#!/bin/bash
# Serve script for Browser4 Kotlin SDK documentation

set -e

echo "===== Browser4 Kotlin SDK Documentation Server ====="
echo ""

# Check if virtual environment exists
if [ ! -d "venv" ]; then
    echo "Virtual environment not found. Running build.sh first..."
    ./build.sh
fi

# Activate virtual environment
source venv/bin/activate

# Serve documentation
echo "Starting documentation server..."
echo "Documentation will be available at: http://127.0.0.1:8000"
echo ""
echo "Press Ctrl+C to stop the server."
echo ""

mkdocs serve
