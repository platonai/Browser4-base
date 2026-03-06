#!/usr/bin/env bash

# 🔍 Find the repo root using git
repoRoot=$(git rev-parse --show-toplevel 2>/dev/null)
if [[ -z "$repoRoot" ]]; then
    echo "Repo root not found. Exiting."
    exit 1
fi
cd "$repoRoot"

# Call copilot to commit all changes with a message
prompt="Commit all changes in $repoRoot and push to the remote repository. Resolve conflicts if there is any."
echo "Running: gh copilot -- -p '$prompt' --allow-all-tools"
gh copilot -- -p "$prompt" --allow-all-tools
