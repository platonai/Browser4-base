#!/usr/bin/env pwsh

# 🔍 Find the repo root using git
$repoRoot = (git rev-parse --show-toplevel 2>$null)
if (-not $repoRoot) {
    Write-Host "Repo root not found. Exiting."
    exit 1
}
Set-Location $repoRoot

# Call copilot to commit all changes with a message
$prompt = "Commit all changes in $repoRoot and push to the remote repository. Resolve conflicts if there is any."
Write-Host "Running: gh copilot -p '$prompt' --allow-all-tools"
gh copilot -p "$prompt" --allow-all-tools
