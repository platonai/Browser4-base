#!/usr/bin/env pwsh

# Find the first parent directory containing the VERSION file
$AppHome=(Get-Item -Path $MyInvocation.MyCommand.Path).Directory
while ($AppHome -ne $null -and !(Test-Path "$AppHome/VERSION")) {
  $AppHome = Split-Path -Parent $AppHome
}
Set-Location $AppHome

function Print-Usage {
  Write-Host "Usage: test.ps1 [test-types...] [maven-args...]"
  Write-Host ""
  Write-Host "Test Types:"
  Write-Host "  fast        Run fast unit tests only"
  Write-Host "  it          Run integration tests"
  Write-Host "  e2e         Run end-to-end tests"
  Write-Host "  kotlin-sdk  Run Kotlin SDK tests"
  Write-Host "  python-sdk  Run Python SDK tests"
  Write-Host "  nodejs-sdk  Run NodeJS SDK tests"
  Write-Host "  core        Run core module supplementary tests"
  Write-Host "  rest        Run REST module tests"
  Write-Host "  all         Run all tests (fast, core, it, e2e, rest)"
  Write-Host ""
  Write-Host "Examples:"
  Write-Host "  test.ps1 fast                       # Run fast unit tests"
  Write-Host "  test.ps1 it                         # Run integration tests"
  Write-Host "  test.ps1 e2e                        # Run end-to-end tests"
  Write-Host "  test.ps1 kotlin-sdk                 # Run Kotlin SDK tests"
  Write-Host "  test.ps1 it kotlin-sdk              # Run integration and Kotlin SDK tests"
  Write-Host "  test.ps1 python-sdk                 # Run Python SDK tests"
  Write-Host "  test.ps1 python-sdk -m integration  # Run Python SDK integration tests only"
  Write-Host "  test.ps1 nodejs-sdk                 # Run NodeJS SDK tests"
  Write-Host "  test.ps1 nodejs-sdk --coverage      # Run NodeJS SDK tests with coverage"
  Write-Host "  test.ps1 all                        # Run all tests"
  Write-Host '  test.ps1 it -pl pulsar-core         # Run integration tests for pulsar-core only'
  exit 1
}

# Maven command
$MvnCmd = Join-Path $AppHome '.\mvnw.cmd'

# Validate Maven wrapper exists
if (!(Test-Path $MvnCmd)) {
    Write-Error "Maven wrapper not found at $MvnCmd"
    exit 1
}

# Default test type is fast if no args provided
$TestTypes = @()
$AdditionalMvnArgs = @()

# Parse command-line arguments
if ($args.Count -eq 0) {
  Print-Usage
}

$KnownTestTypes = @("fast", "it", "e2e", "kotlin-sdk", "python-sdk", "nodejs-sdk", "core", "rest", "all")
$ParsingTestTypes = $true

for ($i = 0; $i -lt $args.Count; $i++) {
  $arg = $args[$i]
  if ($ParsingTestTypes -and ($arg -in $KnownTestTypes)) {
    $TestTypes += $arg
  } elseif ($arg -in "-h", "-help", "--help") {
    Print-Usage
  } else {
    $ParsingTestTypes = $false
    $AdditionalMvnArgs += $arg
  }
}

if ($TestTypes.Count -eq 0) {
    if ($AdditionalMvnArgs.Count -eq 0) {
         # No args provided at all, covered by top check but good for safety
         $TestTypes += "fast"
    } else {
         # Only maven args provided, assume fast
         $TestTypes += "fast"
    }
}

# Expand 'all' to specific types
$ExpandedTestTypes = @()
foreach ($type in $TestTypes) {
    if ($type -eq "all") {
        $ExpandedTestTypes += "fast"
        $ExpandedTestTypes += "core"
        $ExpandedTestTypes += "it"
        $ExpandedTestTypes += "e2e"
        $ExpandedTestTypes += "rest"
    } else {
        $ExpandedTestTypes += $type
    }
}
$TestTypes = $ExpandedTestTypes

# Remove duplicates while preserving order
$UniqueTestTypes = @()
foreach ($type in $TestTypes) {
    if ($type -notin $UniqueTestTypes) {
        $UniqueTestTypes += $type
    }
}
$TestTypes = $UniqueTestTypes

# Execute tests
foreach ($TestType in $TestTypes) {
    Write-Host "=========================================="
    Write-Host "Running $TestType tests..."
    Write-Host "=========================================="

    $MvnArgs = @("test") + $AdditionalMvnArgs
    $ExitCode = 0

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
          & $MvnCmd @MvnArgs "-DrunE2ETests=true" "-P" "all-modules" "-pl" "pulsar-tests,pulsar-tests/pulsar-e2e-tests" "-am"
        }
        "kotlin-sdk" {
          Write-Host "Running Kotlin SDK tests..."
          & $MvnCmd @MvnArgs "-DrunSDKTests=true" "-P" "all-modules" "-pl" "sdks/kotlin-sdk-tests" "-am"
        }
        "python-sdk" {
          Write-Host "Running Python SDK tests..."
          $PythonSdkDir = Join-Path $AppHome "sdks\browser4-sdk-python"

          if (!(Test-Path $PythonSdkDir)) {
            Write-Error "Python SDK directory not found at $PythonSdkDir"
            exit 1
          }

          # Check if Python is available
          $pythonCmd = Get-Command python -ErrorAction SilentlyContinue
          if (!$pythonCmd) {
            $pythonCmd = Get-Command python3 -ErrorAction SilentlyContinue
          }
          if (!$pythonCmd) {
            Write-Error "Python is not installed or not in PATH"
            exit 1
          }

          # Check if pytest is available
          $pytestCheck = & $pythonCmd.Source -m pytest --version 2>&1
          if ($LASTEXITCODE -ne 0) {
            Write-Error "pytest is not installed. Install it with: pip install pytest"
            Write-Host "Or install all dev dependencies with: pip install -e `".[dev]`" in $PythonSdkDir"
            exit 1
          }

          Push-Location $PythonSdkDir
          Write-Host "Working directory: $(Get-Location)"
          & $pythonCmd.Source -m pytest $AdditionalMvnArgs
          $ExitCode = $LASTEXITCODE
          Pop-Location
          if ($ExitCode -ne 0) { exit $ExitCode }
        }
        "nodejs-sdk" {
          Write-Host "Running NodeJS SDK tests..."
          $NodejsSdkDir = Join-Path $AppHome "sdks\browser4-sdk-nodejs"

          if (!(Test-Path $NodejsSdkDir)) {
            Write-Error "NodeJS SDK directory not found at $NodejsSdkDir"
            exit 1
          }

          # Check if Node.js is available
          $nodeCmd = Get-Command node -ErrorAction SilentlyContinue
          if (!$nodeCmd) {
            Write-Error "Node.js is not installed or not in PATH"
            exit 1
          }

          Push-Location $NodejsSdkDir
          Write-Host "Working directory: $(Get-Location)"

          # Check if node_modules exists
          if (!(Test-Path "$NodejsSdkDir\node_modules")) {
            Write-Host "Installing dependencies..."
            & npm install
            if ($LASTEXITCODE -ne 0) {
              Write-Error "Failed to install dependencies"
              Pop-Location
              exit 1
            }
          }

          # Check if jest is available
          if (!(Test-Path "$NodejsSdkDir\node_modules\.bin\jest.cmd")) {
            Write-Error "jest is not installed. Install it with: npm install"
            Pop-Location
            exit 1
          }

          & npm test -- $AdditionalMvnArgs
          $ExitCode = $LASTEXITCODE
          Pop-Location
          if ($ExitCode -ne 0) { exit $ExitCode }
        }
        "core" {
          Write-Host "Running core module supplementary tests..."
          & $MvnCmd @MvnArgs "-DrunCoreTests=true" "-Ppulsar-core-tests" "-pl" "pulsar-core,pulsar-core/pulsar-core-tests" "-am"
        }
        "rest" {
          Write-Host "Running REST module tests..."
          & $MvnCmd @MvnArgs "-DrunRestTests=true"
        }
        Default {
          Write-Error "Unknown test type '$TestType'"
          exit 1
        }
      }

      $LastExitCode = $LASTEXITCODE
      if ($LastExitCode -ne 0) {
         $ExitCode = $LastExitCode
      }
      
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
        exit $ExitCode
      }
    }
    catch {
      Write-Error "Failed to execute tests: $_"
      exit 1
    }
}
exit 0
