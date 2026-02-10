#!/usr/bin/env pwsh

# Find the first parent directory containing the VERSION file
$AppHome=(Get-Item -Path $MyInvocation.MyCommand.Path).Directory
while ($AppHome -ne $null -and !(Test-Path "$AppHome/VERSION")) {
  $AppHome = Split-Path -Parent $AppHome
}
Set-Location $AppHome

function Print-Usage {
  Write-Host "Usage: test.ps1 [test-type] [maven-args...]"
  Write-Host ""
  Write-Host "Test Types:"
  Write-Host "  fast        Run fast unit tests only (default)"
  Write-Host "  it          Run integration tests"
  Write-Host "  e2e         Run end-to-end tests"
  Write-Host "  sdk         Run SDK tests"
  Write-Host "  core        Run core module supplementary tests"
  Write-Host "  rest        Run REST module tests"
  Write-Host "  all         Run all tests (integration, e2e, sdk)"
  Write-Host ""
  Write-Host "Examples:"
  Write-Host "  test.ps1                    # Run fast unit tests"
  Write-Host "  test.ps1 fast               # Run fast unit tests"
  Write-Host "  test.ps1 it                 # Run integration tests"
  Write-Host "  test.ps1 e2e                # Run end-to-end tests"
  Write-Host "  test.ps1 sdk                # Run SDK tests"
  Write-Host "  test.ps1 all                # Run all tests"
  Write-Host '  test.ps1 it -pl pulsar-core # Run integration tests for pulsar-core only'
  exit 1
}

# Maven command
$MvnCmd = Join-Path $AppHome '.\mvnw.cmd'

# Validate Maven wrapper exists
if (!(Test-Path $MvnCmd)) {
    Write-Error "Maven wrapper not found at $MvnCmd"
    exit 1
}

# Default test type is fast
$TestType = "fast"
$AdditionalMvnArgs = @()

# Parse command-line arguments
if ($args.Count -gt 0) {
  $FirstArg = $args[0]
  switch ($FirstArg) {
    { $_ -in "fast", "it", "e2e", "sdk", "core", "rest", "all" } {
      $TestType = $FirstArg
      if ($args.Count -gt 1) {
        $AdditionalMvnArgs = $args[1..($args.Count - 1)]
      }
    }
    { $_ -in "-h", "-help", "--help" } {
      Print-Usage
    }
    Default {
      $AdditionalMvnArgs = $args
    }
  }
}

# Execute tests based on type
Write-Host "=========================================="
Write-Host "Running $TestType tests..."
Write-Host "=========================================="

$MvnArgs = @("test") + $AdditionalMvnArgs

try {
  switch ($TestType) {
    "fast" {
      Write-Host "Running fast unit tests (default behavior)..."
      & $MvnCmd @MvnArgs
    }
    "it" {
      Write-Host "Running integration tests..."
      & $MvnCmd @MvnArgs "-DrunITs=true"
    }
    "e2e" {
      Write-Host "Running end-to-end tests..."
      & $MvnCmd @MvnArgs "-DrunE2ETests=true"
    }
    "sdk" {
      Write-Host "Running SDK tests..."
      & $MvnCmd @MvnArgs "-DrunSDKTests=true"
    }
    "core" {
      Write-Host "Running core module supplementary tests..."
      & $MvnCmd @MvnArgs "-DrunCoreTests=true"
    }
    "rest" {
      Write-Host "Running REST module tests..."
      & $MvnCmd @MvnArgs "-DrunRestTests=true"
    }
    "all" {
      Write-Host "Running all tests (integration, e2e, sdk)..."
      & $MvnCmd @MvnArgs "-DrunITs=true" "-DrunE2ETests=true" "-DrunSDKTests=true"
    }
    Default {
      Write-Error "Unknown test type '$TestType'"
      Print-Usage
    }
  }

  $ExitCode = $LASTEXITCODE

  if ($ExitCode -eq 0) {
    Write-Host ""
    Write-Host "=========================================="
    Write-Host "✅ $TestType tests completed successfully"
    Write-Host "=========================================="
  } else {
    Write-Host ""
    Write-Host "=========================================="
    Write-Host "❌ $TestType tests failed with exit code $ExitCode"
    Write-Host "=========================================="
  }

  exit $ExitCode
}
catch {
  Write-Error "Failed to execute tests: $_"
  exit 1
}
