#!/usr/bin/env pwsh

# Browser4 Test Runner Script (PowerShell)
# Comprehensive test execution with multiple options

param(
    [switch]$Help,
    [switch]$Clean,
    [switch]$Integration,
    [switch]$E2E,
    [string]$Module = "",
    [string]$Test = "",
    [switch]$Coverage,
    [switch]$Parallel,
    [switch]$Verbose,
    [string]$Groups = "",
    [string]$ExcludeGroups = "TimeConsumingTest,ExternalServiceTest"
)

# Find the project root directory (contains VERSION file)
$AppHome = (Get-Item -Path $MyInvocation.MyCommand.Path).Directory.Parent.FullName
Set-Location $AppHome

function Show-Usage {
    Write-Host "Usage: test.ps1 [OPTIONS]"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -Help               Show this help message"
    Write-Host "  -Clean              Clean before testing"
    Write-Host "  -Integration        Include integration tests"
    Write-Host "  -E2E                Include E2E tests (implies -Integration)"
    Write-Host "  -Module MODULE      Test specific module (e.g., pulsar-core)"
    Write-Host "  -Test CLASS         Test specific class or pattern"
    Write-Host "  -Coverage           Generate coverage report"
    Write-Host "  -Parallel           Enable parallel test execution"
    Write-Host "  -Verbose            Enable verbose output"
    Write-Host "  -Groups TAGS        Include only specific test groups (comma-separated)"
    Write-Host "  -ExcludeGroups TAGS Exclude specific test groups (comma-separated)"
    Write-Host ""
    Write-Host "Examples:"
    Write-Host "  test.ps1                              # Run unit tests only"
    Write-Host "  test.ps1 -Integration                 # Run unit + integration tests"
    Write-Host "  test.ps1 -E2E                         # Run all tests including E2E"
    Write-Host "  test.ps1 -Module pulsar-core          # Test specific module"
    Write-Host "  test.ps1 -Test MyTest                 # Run specific test class"
    Write-Host "  test.ps1 -Coverage                    # Generate coverage report"
    Write-Host "  test.ps1 -Clean -Integration          # Clean and run integration tests"
    Write-Host "  test.ps1 -Parallel                    # Run tests in parallel"
    Write-Host "  test.ps1 -Groups SmokeTest            # Run only smoke tests"
    Write-Host ""
    exit 1
}

if ($Help) {
    Show-Usage
}

# Validate Maven wrapper exists
$MvnCmd = Join-Path $AppHome "mvnw.cmd"
if (!(Test-Path $MvnCmd)) {
    Write-Host "Error: Maven wrapper not found at $MvnCmd" -ForegroundColor Red
    exit 1
}

# Initialize Maven arguments
$MvnArgs = @()

# Add clean if requested
if ($Clean) {
    $MvnArgs += "clean"
}

# Add test goal
$MvnArgs += "test"

# Add module specification
if ($Module) {
    $MvnArgs += "-pl"
    $MvnArgs += $Module
    $MvnArgs += "-am"
    # When using -am, we need this flag to avoid "No tests were executed" error
    $MvnArgs += '-D"surefire.failIfNoSpecifiedTests=false"'
}

# Add specific test class
if ($Test) {
    $MvnArgs += "-Dtest=$Test"
}

# Handle test groups
if ($E2E) {
    # Include all tests
    $Integration = $true
    $ExcludeGroups = ""
}
elseif ($Integration) {
    # Exclude only E2E and time-consuming
    $ExcludeGroups = "E2ETest,TimeConsumingTest"
}

# Set excluded groups
if ($ExcludeGroups) {
    $MvnArgs += '-D"excludedGroups=' + $ExcludeGroups + '"'
}
else {
    # Empty string to include all
    $MvnArgs += '-D"excludedGroups="'
}

# Set included groups if specified
if ($Groups) {
    $MvnArgs += '-D"groups=' + $Groups + '"'
}

# Add parallel execution
if ($Parallel) {
    $MvnArgs += '-D"surefire.parallel=methods"'
    $MvnArgs += '-D"surefire.threadCount=4"'
    $MvnArgs += '-D"surefire.perCoreThreadCount=true"'
}

# Add coverage if requested
if ($Coverage) {
    $MvnArgs += "jacoco:report"
}

# Add verbose output
if ($Verbose) {
    $MvnArgs += "-X"
}
else {
    $MvnArgs += "-B"  # Batch mode
}

# Print configuration
Write-Host "╔═══════════════════════════════════════════════════════╗" -ForegroundColor Blue
Write-Host "║          Browser4 Test Execution                    ║" -ForegroundColor Blue
Write-Host "╚═══════════════════════════════════════════════════════╝" -ForegroundColor Blue
Write-Host ""
Write-Host "Configuration:" -ForegroundColor Yellow
Write-Host "  Clean: $Clean"
Write-Host "  Integration: $Integration"
Write-Host "  E2E: $E2E"
if ($Module) { Write-Host "  Module: $Module" }
if ($Test) { Write-Host "  Test: $Test" }
Write-Host "  Coverage: $Coverage"
Write-Host "  Parallel: $Parallel"
if ($ExcludeGroups) { Write-Host "  Excluded Groups: $ExcludeGroups" }
if ($Groups) { Write-Host "  Included Groups: $Groups" }
Write-Host ""
Write-Host "Executing: $MvnCmd $($MvnArgs -join ' ')" -ForegroundColor Yellow
Write-Host ""

# Record start time
$StartTime = Get-Date

# Execute Maven test command
& $MvnCmd @MvnArgs
$ExitCode = $LASTEXITCODE

# Record end time
$EndTime = Get-Date
$Duration = ($EndTime - $StartTime).TotalSeconds

# Print summary
Write-Host ""
Write-Host "╔═══════════════════════════════════════════════════════╗" -ForegroundColor Blue
Write-Host "║          Test Execution Summary                      ║" -ForegroundColor Blue
Write-Host "╚═══════════════════════════════════════════════════════╝" -ForegroundColor Blue
Write-Host ""

if ($ExitCode -eq 0) {
    Write-Host "✓ Tests passed successfully!" -ForegroundColor Green
}
else {
    Write-Host "✗ Tests failed!" -ForegroundColor Red
}

Write-Host ""
Write-Host "Duration: $([math]::Round($Duration, 2))s"

# Show coverage report location if generated
if ($Coverage) {
    Write-Host ""
    Write-Host "Coverage reports:" -ForegroundColor Yellow
    if ($Module) {
        Write-Host "  - $Module/target/site/jacoco/index.html"
    }
    else {
        Write-Host "  - target/site/jacoco-aggregate/index.html"
    }
}

# Show test reports location
Write-Host ""
Write-Host "Test reports:" -ForegroundColor Yellow
if ($Module) {
    Write-Host "  - $Module/target/surefire-reports/"
}
else {
    Write-Host "  - */target/surefire-reports/"
}

Write-Host ""

exit $ExitCode
