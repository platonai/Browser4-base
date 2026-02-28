#!/usr/bin/env pwsh

param(
    [Parameter(Position=0)]
    [string]$Task
)

$repoRoot = (git rev-parse --show-toplevel 2>$null)
if (-not $repoRoot) {
    Write-Host "Repo root not found. Exiting."
    exit 1
}
Set-Location $repoRoot

# Write the task description to a temporary file to avoid issues with newlines and quotes in the prompt
# The temporary file name to make it easier to identify and clean up later if needed.
# - located in $env:TEMP/browser4/coworker/
# - starts with "cw-prompt-"
$tempDir = Join-Path -Path $env:TEMP -ChildPath "browser4\coworker"
if (-not (Test-Path -Path $tempDir)) {
    New-Item -Path $tempDir -ItemType Directory | Out-Null
}
$tempFilePath = Join-Path -Path $tempDir -ChildPath "cw-prompt-$(Get-Date -Format 'yyyyMMdd-HHmmss').txt"
Set-Content -Path $tempFilePath -Value $taskDescription -Encoding UTF8

$prompt = "Finish the task described in file: $tempFilePath."
# Escape double quotes in the prompt and wrap in quotes to ensure correct argument parsing
$safePrompt = $prompt.Replace('"', '\"')

# Pass arguments as an array to avoid fragile manual escaping/quoting.
# This keeps quotes/newlines intact in the -p prompt.
$copilotArgList = @(
    'copilot'
    '--'
    '-p'
    "`"$safePrompt`""
    '--allow-all-tools'
    '--allow-all-paths'
)

$process = Start-Process -FilePath 'gh' -ArgumentList $copilotArgList
