#!/usr/bin/env pwsh

$ErrorActionPreference = "Stop"

# Function to find the repository root
function Get-RepoRoot {
    $currentDir = $PSScriptRoot
    while ($currentDir -and !(Test-Path (Join-Path $currentDir ".git"))) {
        $parentDir = Split-Path -Parent $currentDir
        if ($parentDir -eq $currentDir) { return $null }
        $currentDir = $parentDir
    }
    return $currentDir
}

$repoRoot = Get-RepoRoot
if (-not $repoRoot) {
    # Fallback to git command
    $repoRoot = (git rev-parse --show-toplevel 2>$null)
}

if (-not $repoRoot) {
    Write-Host "Repo root not found. Exiting."
    exit 1
}

$configPath = Join-Path $repoRoot "coworker\scripts\config.ps1"
if (Test-Path $configPath) {
    . $configPath
}
if (-not $COPILOT) {
    $COPILOT = @('gh', 'copilot')
}
if ($COPILOT -is [string]) {
    throw "COPILOT must be defined as a PowerShell array in $configPath"
}
if ($COPILOT.Count -lt 2) {
    throw "COPILOT must include an executable and at least one argument"
}
$copilotExecutable = $COPILOT[0]
$copilotBaseArgs = @($COPILOT | Select-Object -Skip 1)

$draftDir = Join-Path $repoRoot "coworker\tasks\0draft"

if (-not (Test-Path $draftDir)) {
    Write-Host "Draft directory not found: $draftDir"
    exit 1
}

# Find the last modified .md file
$latestDraft = Get-ChildItem -Path $draftDir -Filter "*.md" | Sort-Object LastWriteTime -Descending | Select-Object -First 1

if (-not $latestDraft) {
    Write-Host "No draft files found in $draftDir"
    exit 0
}

$draftPath = $latestDraft.FullName
Write-Host "Found latest draft: $draftPath"

$prompt = "Refine the content of the draft file: $draftPath. Improve the writing, clarity, and structure."

Write-Host "Starting GitHub Copilot to refine the draft..."

# Use configured gh copilot command with the prompt
$ghArgs = @($copilotBaseArgs + @(
    '--',
    '-p',
    "`"$prompt`"",
    '--allow-all-tools',
    '--allow-all-paths'
))

Start-Process -FilePath $copilotExecutable -ArgumentList $ghArgs -NoNewWindow -Wait
