#!/usr/bin/env pwsh

$repoRoot = (git rev-parse --show-toplevel 2>$null)
Set-Location $repoRoot

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
  Write-Host "  skills      Run skills module tests"
  Write-Host "  mcp         Run MCP module tests"
  Write-Host "  browser4    Run all Browser4 main tests (fast, core, rest, it, e2e)"
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
  Write-Host "  test.ps1 skills                     # Run skills module tests"
  Write-Host "  test.ps1 mcp                        # Run MCP module tests"
  Write-Host "  test.ps1 browser4                   # Run all Browser4 main tests"
  Write-Host '  test.ps1 it -pl pulsar-core         # Run integration tests for pulsar-core only'
  exit 1
}

# Maven command
$MvnCmd = Join-Path $repoRoot '.\mvnw.cmd'

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

$KnownTestTypes = @("fast", "it", "e2e", "kotlin-sdk", "python-sdk", "nodejs-sdk", "core", "rest", "skills", "mcp", "browser4")
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
         $TestTypes += "fast"
    } else {
         $TestTypes += "fast"
    }
}

# Separate Maven tests from SDK tests
$MavenTests = @()
$SDKTests = @()

foreach ($type in $TestTypes) {
    if ($type -eq "all-maven" -or $type -eq "browser4") {
        $MavenTests += "fast", "core", "it", "e2e", "rest"
        break
    } elseif ($type -in "python-sdk", "nodejs-sdk", "kotlin-sdk") {
        $SDKTests += $type
    } else {
        $MavenTests += $type
    }
}

# Remove duplicates from MavenTests while preserving order
$UniqueMavenTests = @()
foreach ($type in $MavenTests) {
    if ($type -notin $UniqueMavenTests) {
        $UniqueMavenTests += $type
    }
}
$MavenTests = $UniqueMavenTests

# If we have Maven tests, execute them as a single command
if ($MavenTests.Count -gt 0) {
    Write-Host "=========================================="
    Write-Host "Running Maven tests: $($MavenTests -join ', ')"
    Write-Host "=========================================="

    $MvnTestArgs = @("test")

    # Check which test types are requested
    $HasFast = $MavenTests -contains "fast"
    $HasIT = $MavenTests -contains "it"
    $HasE2E = $MavenTests -contains "e2e"
    $HasCore = $MavenTests -contains "core"
    $HasRest = $MavenTests -contains "rest"
    $HasSkills = $MavenTests -contains "skills"
    $HasMcp = $MavenTests -contains "mcp"

    $TestPatterns = @()
    if ($HasSkills) { $TestPatterns += "**/skills/**" }
    if ($HasMcp) { $TestPatterns += "**/mcp/**" }

    $Modules = @()

    # Add flags based on what's needed
    if ($HasIT) { $MvnTestArgs += "-DrunITs=true" }
    if ($HasE2E) { $MvnTestArgs += "-DrunE2ETests=true" }
    if ($HasCore) {
        $MvnTestArgs += "-DrunCoreTests=true"
        $MvnTestArgs += "-Ppulsar-core-tests"
        $Modules += "pulsar-core", "pulsar-core/pulsar-core-tests"
    }

    if ($HasSkills -or $HasMcp) {
        $Modules += "pulsar-agentic"

        # Only set -Dtest if we are NOT running other main test types
        if (-not ($HasFast -or $HasIT -or $HasE2E -or $HasCore -or $HasRest)) {
            if ($HasSkills) { $MvnTestArgs += "-DrunSkillsTests=true" }
            if ($HasMcp) { $MvnTestArgs += "-DrunMcpTests=true" }

            $JoinedPatterns = $TestPatterns -join ","
            $MvnTestArgs += "-Dtest=$JoinedPatterns"
        }
    }

    if ($HasFast -or $HasRest) {
        $Modules = @()
    }

    if ($Modules.Count -gt 0) {
        $JoinedModules = $Modules -join ","
        $MvnTestArgs += "-pl"
        $MvnTestArgs += $JoinedModules
        $MvnTestArgs += "-am"
    }

    # Add any additional Maven args
    $MvnTestArgs += $AdditionalMvnArgs

    # Execute Maven test command
    try {
        & $MvnCmd @MvnTestArgs
        $ExitCode = $LASTEXITCODE

        if ($ExitCode -ne 0) {
            Write-Host ""
            Write-Host "=========================================="
            Write-Host "❌ Maven tests failed with exit code $ExitCode"
            Write-Host "=========================================="
            exit $ExitCode
        }

        Write-Host ""
        Write-Host "=========================================="
        Write-Host "✅ Maven tests completed successfully"
        Write-Host "=========================================="
    }
    catch {
        Write-Error "Failed to execute Maven tests: $_"
        exit 1
    }
}

# Execute SDK tests
foreach ($TestType in $SDKTests) {
    Write-Host "=========================================="
    Write-Host "Running $TestType tests..."
    Write-Host "=========================================="

    $ExitCode = 0

    try {
      switch ($TestType) {
        "python-sdk" {
          Write-Host "Running Python SDK tests..."
          $PythonSdkDir = Join-Path $repoRoot "sdks\browser4-sdk-python"

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
        }
        "nodejs-sdk" {
          Write-Host "Running NodeJS SDK tests..."
          $NodejsSdkDir = Join-Path $repoRoot "sdks\browser4-sdk-nodejs"

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
        }
        "kotlin-sdk" {
          Write-Host "Running Kotlin SDK tests..."
          $KotlinTestArgs = @("test", "-DrunSDKTests=true", "-P", "all-modules", "-pl", "sdks/kotlin-sdk-tests", "-am") + $AdditionalMvnArgs
          & $MvnCmd @KotlinTestArgs
          $ExitCode = $LASTEXITCODE
        }
        Default {
          Write-Error "Unknown SDK test type '$TestType'"
          exit 1
        }
      }

      # Check if test failed and exit immediately
      if ($ExitCode -ne 0) {
        Write-Host ""
        Write-Host "=========================================="
        Write-Host "❌ $TestType tests failed with exit code $ExitCode"
        Write-Host "=========================================="
        exit $ExitCode
      }

      Write-Host ""
      Write-Host "=========================================="
      Write-Host "✅ $TestType tests completed successfully"
      Write-Host "=========================================="
    }
    catch {
      Write-Error "Failed to execute tests: $_"
      exit 1
    }
}
exit 0
