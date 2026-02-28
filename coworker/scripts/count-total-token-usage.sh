#!/bin/bash
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PYTHON_SCRIPT="$SCRIPT_DIR/count-total-token-usage.py"

if command -v python3 &>/dev/null; then
    python3 "$PYTHON_SCRIPT" "$SCRIPT_DIR/../tasks/300logs"
elif command -v python &>/dev/null; then
    python "$PYTHON_SCRIPT" "$SCRIPT_DIR/../tasks/300logs"
else
    echo "Python not found. Please install Python 3."
    exit 1
fi
