#!/usr/bin/env pwsh

param(
    [string]$Path,
    [int]$IntervalSeconds = 15,
    [switch]$Once
)

$ErrorActionPreference = 'Stop'

function Get-RepoRoot {
    $currentDirectory = $PSScriptRoot
    while ($currentDirectory) {
        if ((Test-Path (Join-Path $currentDirectory 'ROOT.md')) -or (Test-Path (Join-Path $currentDirectory '.git'))) {
            return (Resolve-Path $currentDirectory).Path
        }

        $parentDirectory = Split-Path -Parent $currentDirectory
        if ($parentDirectory -eq $currentDirectory) {
            break
        }
        $currentDirectory = $parentDirectory
    }

    throw 'Repo root not found.'
}

$repoRoot = Get-RepoRoot
$refineScript = Join-Path $repoRoot 'coworker\scripts\workers\refine-drafts.ps1'
$defaultReadyDir = Join-Path $repoRoot 'coworker\tasks\0draft\refine\1ready'
$scanPath = if ([string]::IsNullOrWhiteSpace($Path)) { $defaultReadyDir } else { $Path }

Write-Host "Monitoring draft refinement path: $scanPath"
Write-Host "Refine script: $refineScript"

while ($true) {
    $pendingFiles = @()
    if (Test-Path $scanPath) {
        $scanItem = Get-Item $scanPath
        if ($scanItem.PSIsContainer) {
            $pendingFiles = @(Get-ChildItem -Path $scanItem.FullName -File)
        }
        else {
            $pendingFiles = @($scanItem)
        }
    }

    $timestamp = (Get-Date).ToUniversalTime().ToString('yyyy-MM-dd HH:mm:ss')
    if ($pendingFiles.Count -eq 0) {
        Write-Host "$timestamp - No draft files found for refinement."
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
