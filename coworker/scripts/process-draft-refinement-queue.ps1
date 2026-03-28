#!/usr/bin/env pwsh

param(
    [string]$Path,
    [int]$IntervalSeconds = 15,
    [switch]$Once
)

$ErrorActionPreference = 'Stop'

$configScriptPath = Join-Path $PSScriptRoot 'config.ps1'
. $configScriptPath

$repoRoot = Get-WorkspaceRoot
$refineScript = Join-Path $PSScriptRoot 'workers\refine-drafts.ps1'
$defaultReadyDir = Resolve-TasksPath '0draft\refine\1ready'
$scanPath = if ([string]::IsNullOrWhiteSpace($Path)) { $defaultReadyDir } else { $Path }

Write-Host "Monitoring draft refinement path: $scanPath"
Write-Host "Refine script: $refineScript"

while ($true) {
    Ensure-CoworkerDraftRefinementPlaceholders -DraftDirectory $defaultReadyDir

    $pendingFiles = @()
    if (Test-Path $scanPath) {
        $scanItem = Get-Item $scanPath
        if ($scanItem.PSIsContainer) {
            $pendingFiles = @(Get-ChildItem -Path $scanItem.FullName -File | Where-Object { Test-CoworkerActionableDraftRefinementFile -Item $_ })
        }
        elseif (Test-CoworkerActionableDraftRefinementFile -Item $scanItem) {
            $pendingFiles = @($scanItem)
        }
    }

    $timestamp = (Get-Date).ToUniversalTime().ToString('yyyy-MM-dd HH:mm:ss')
    if ($pendingFiles.Count -eq 0) {
        Write-Host "$timestamp - No actionable draft files found for refinement."
        if ($Once) {
            exit 0
        }

        Start-Sleep -Seconds $IntervalSeconds
        continue
    }

    Write-Host "$timestamp - Refining $($pendingFiles.Count) draft file(s)..."
    & $refineScript -Path $scanPath
    $exitCode = $LASTEXITCODE
    if ($exitCode -ne 0) {
        Write-Host "$timestamp - Draft refinement finished with exit code $exitCode." -ForegroundColor Yellow
    }

    if ($Once) {
        exit $exitCode
    }

    Start-Sleep -Seconds $IntervalSeconds
}
