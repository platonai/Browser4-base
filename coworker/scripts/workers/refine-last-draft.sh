#!/bin/bash
set -e

# Get the directory of the script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Find the repository root
REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || echo "$SCRIPT_DIR/../../..")"

CONFIG_SH="$REPO_ROOT/coworker/scripts/config.sh"
if [[ -f "$CONFIG_SH" ]]; then
    # shellcheck disable=SC1090
    source "$CONFIG_SH"
fi

if ! declare -p COPILOT >/dev/null 2>&1; then
    COPILOT=(gh copilot)
fi

if [[ "$(declare -p COPILOT 2>/dev/null)" != declare\ -a* ]]; then
    echo "Error: COPILOT must be defined as a bash array in $CONFIG_SH" >&2
    exit 1
fi

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

# Run configured gh copilot command
"${COPILOT[@]}" -- -p "$PROMPT" --allow-all-tools --allow-all-paths
