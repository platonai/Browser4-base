#!/usr/bin/env pwsh

# Find the first parent directory that contains a VERSION file
$AppHome = (Get-Item -Path $MyInvocation.MyCommand.Path).Directory
while ($AppHome -ne $null -and -not (Test-Path (Join-Path $AppHome "ROOT.md"))) {
    $AppHome = Split-Path -Parent $AppHome
}
Set-Location $AppHome

$taskFilePath = Join-Path $AppHome "docs-dev\copilot\tasks\daily\tasks.md"
$prompt = "Run tasks described in $taskFilePath"

gh copilot -p "$prompt" --allow-all-tools
