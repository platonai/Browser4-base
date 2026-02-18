#!/usr/bin/env pwsh

# 🔍 Find the first parent directory containing the VERSION file
$AppHome=(Get-Item -Path $MyInvocation.MyCommand.Path).Directory
while ($AppHome -ne $null -and !(Test-Path "$AppHome/ROOT.md")) {
    $AppHome = Split-Path -Parent $AppHome
}
if ($AppHome -eq $null) {
    Write-Error "Could not find a parent directory containing a VERSION file."
    exit 1
}
Set-Location $AppHome

# Call copilot to commit all changes with a message
$prompt = "Commit all changes in $AppHome and push to the remote repository. Resolve conflicts if there is any."
Write-Host "Running: gh copilot -p '$prompt' --allow-all-tools"
gh copilot -p "$prompt" --allow-all-tools
