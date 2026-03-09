#!/usr/bin/env pwsh

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
$target = Join-Path $repoRoot 'coworker\scripts\deprecated\process-coworker-queue.ps1'


& $target @args
exit $LASTEXITCODE
