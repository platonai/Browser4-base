#!/usr/bin/env pwsh

$ErrorActionPreference = "Stop"


$repoRoot = (git rev-parse --show-toplevel 2>$null)

if (-not $repoRoot -or -not (Test-Path (Join-Path $repoRoot "ROOT.md"))) {
  throw "VERSION file not found when resolving project root."
}

Set-Location $repoRoot

& (Join-Path $repoRoot "bin/build/build.ps1") @args

& (Join-Path $repoRoot "bin/browser4.ps1") @args
