#
# PowerShell wrapper for link checker
# Ensures Python 3 is available and runs the link checker with proper error handling
#

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RootDir = (Get-Item (Join-Path $ScriptDir "..\..")).FullName
$PythonScript = Join-Path $ScriptDir "check-links.py"

# Check if Python 3 is available
$pythonCmd = $null
foreach ($cmd in @("python3", "python")) {
    if (Get-Command $cmd -ErrorAction SilentlyContinue) {
        $version = & $cmd --version 2>&1
        if ($version -match "Python 3\.") {
            $pythonCmd = $cmd
            break
        }
    }
}

if (-not $pythonCmd) {
    Write-Host "❌ Error: Python 3 is not installed or not in PATH" -ForegroundColor Red
    exit 1
}

# Check if requests library is available
$hasRequests = & $pythonCmd -c "import requests" 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "⚠️  Warning: 'requests' library not found." -ForegroundColor Yellow
    Write-Host "   This script requires the 'requests' library to function." -ForegroundColor Yellow
    Write-Host ""
    
    # Try to install if pip is available
    $pipCmd = $null
    foreach ($cmd in @("pip3", "pip")) {
        if (Get-Command $cmd -ErrorAction SilentlyContinue) {
            $pipCmd = $cmd
            break
        }
    }
    
    if ($pipCmd) {
        Write-Host "   Attempting automatic installation with $pipCmd..." -ForegroundColor Yellow
        Write-Host "   (This is safe - requests is a well-known HTTP library)" -ForegroundColor Yellow
        & $pipCmd install requests --user
        if ($LASTEXITCODE -ne 0) {
            Write-Host "❌ Failed to install requests. Please install manually:" -ForegroundColor Red
            Write-Host "   pip install requests" -ForegroundColor Red
            exit 1
        }
    } else {
        Write-Host "❌ pip not found. Please install manually:" -ForegroundColor Red
        Write-Host "   pip install requests" -ForegroundColor Red
        exit 1
    }
}

# Change to root directory
Set-Location $RootDir

# Run the Python script with all arguments passed through
& $pythonCmd $PythonScript $args
exit $LASTEXITCODE
