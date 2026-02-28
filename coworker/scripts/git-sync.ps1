#!/usr/bin/env pwsh

# 🔍 Find repo root
$repoRoot = git rev-parse --show-toplevel 2>$null
if (-not $repoRoot) {
    Write-Host "Repo root not found. Exiting."
    exit 1
}

Set-Location $repoRoot

# 显式为路径添加双引号（用于自然语言提示清晰化）
$quotedRoot = '"' + $repoRoot + '"'

$prompt = @"
Commit all changes in $quotedRoot.
Pull from remote.
Then push to remote.
If conflicts occur, resolve them automatically.
"@

# Invoke gh-copilot.ps1 with the prompt
# . coworker\scripts\gh-copilot.ps1 $prompt

# Escape double quotes in the prompt and wrap in quotes to ensure correct argument parsing
$safePrompt = $prompt.Replace('"', '\"')

Write-Host "Running:"
Write-Host "gh copilot -p '$safePrompt' --allow-all-tools"

# Pass arguments as an array to avoid fragile manual escaping/quoting.
$copilotArgList = @(
    'copilot'
    '--'
    '-p'
    "`"$safePrompt`""
    '--allow-all-tools'
)

Start-Process -FilePath 'gh' -ArgumentList $copilotArgList -NoNewWindow -Wait
