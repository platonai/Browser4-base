#!/usr/bin/env pwsh

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$PowerShellExe = Join-Path $PSHOME $(if ($PSVersionTable.PSEdition -eq 'Core') { 'pwsh.exe' } else { 'powershell.exe' })
& $PowerShellExe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $ScriptDir 'bump-version.ps1') -Part patch
exit $LASTEXITCODE
