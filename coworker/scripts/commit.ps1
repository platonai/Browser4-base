#!/usr/bin/env pwsh

# 🔍 Find the repo root using git
$AppHome = (git rev-parse --show-toplevel 2>$null)
if (-not $AppHome) {
    Write-Error "Could not determine repository root using git rev-parse --show-toplevel."
    exit 1
}
Set-Location $AppHome

# Call copilot to commit all changes with a message
$prompt = "Commit all changes in $AppHome and push to the remote repository. Resolve conflicts if there is any."
Write-Host "Running: gh copilot -p '$prompt' --allow-all-tools"
gh copilot -p "$prompt" --allow-all-tools
