#!/bin/bash
set -e

# Get the directory of the script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Find the repository root
REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || echo "$SCRIPT_DIR/../../..")"

DRAFT_DIR="$REPO_ROOT/coworker/tasks/0draft"

if [ ! -d "$DRAFT_DIR" ]; then
    echo "Draft directory not found: $DRAFT_DIR"
    exit 1
fi

# Find the last modified .md file
LATEST_DRAFT=$(ls -t "$DRAFT_DIR"/*.md 2>/dev/null | head -n 1)

if [ -z "$LATEST_DRAFT" ]; then
    echo "No draft files found in $DRAFT_DIR"
    exit 0
fi

echo "Found latest draft: $LATEST_DRAFT"

PROMPT="Refine the content of the draft file: $LATEST_DRAFT. Improve the writing, clarity, and structure."

echo "Starting GitHub Copilot to refine the draft..."

# Run gh copilot
gh copilot -- -p "$PROMPT" --allow-all-tools --allow-all-paths
