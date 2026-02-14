#!/usr/bin/env pwsh

# ============================================================================
# Coworker Task Runner - PowerShell Version
# ============================================================================
# Purpose:
#   Automatically processes task files in the 'created' directory
#   and executes them using the Copilot tool. Task files are moved through
#   a workflow: created -> working -> finished, with execution logs recorded.
#
# Task File Format (optional structured format):
#   Title: <task title>
#   Description: <task description>
#   Prompt: <task prompt content>
#
#   If not in structured format, the entire file content is treated as the prompt.
#
# Usage:
#   powershell -ExecutionPolicy Bypass -File coworker.ps1
# ============================================================================

# Find the first parent directory that contains a VERSION file
# This allows the script to be run from any location within the project
$AppHome = (Get-Item -Path $MyInvocation.MyCommand.Path).Directory
while ($AppHome -ne $null -and -not (Test-Path (Join-Path $AppHome "VERSION"))) {
    $AppHome = Split-Path -Parent $AppHome
}
Set-Location $AppHome

# Define directory paths for task management workflow
$baseDir = Join-Path $AppHome "docs-dev\copilot\tasks\daily"
$createdDir = Join-Path $baseDir "created"        # Input directory for new tasks
$workingDir = Join-Path $baseDir "working"        # Processing directory for current tasks
$finishedDir = Join-Path $baseDir "finished"      # Output directory for completed tasks
$repoRoot = $AppHome                              # Repository root for Copilot execution

# Ensure all required directories exist
# Create them if they don't already exist
if (!(Test-Path $createdDir)) { New-Item -ItemType Directory -Path $createdDir | Out-Null }
if (!(Test-Path $workingDir)) { New-Item -ItemType Directory -Path $workingDir | Out-Null }
if (!(Test-Path $finishedDir)) { New-Item -ItemType Directory -Path $finishedDir | Out-Null }

$files = Get-ChildItem -Path $createdDir

# Process each task file found in the created directory
foreach ($file in $files) {
    Write-Host "Processing $($file.Name)..."

    # Read the entire file content
    $content = Get-Content -Path $file.FullPath -Raw

    # Initialize variables for task metadata
    $title = ""
    $description = ""
    $prompt = ""

    # Try to parse structured content with Title, Description, and Prompt sections
    # Uses regex to extract these fields if they follow the expected format
    if ($content -match "(?ms)^Title:\s*(?<title>.*?)(\r\n|\n)Description:\s*(?<desc>.*?)(\r\n|\n)Prompt:\s*(?<prompt>.*)$") {
        $title = $Matches['title'].Trim()
        $description = $Matches['desc'].Trim()
        $prompt = $Matches['prompt'].Trim()
    } else {
        # Fallback: If file is not in structured format, use it as-is
        $title = $file.BaseName
        $description = "Task from $($file.Name)"
        $prompt = $content
    }

    # Sanitize the title to make it safe for use as a filename
    # Remove special characters that are not allowed in Windows filenames
    $safeTitle = $title -replace '[\\/*?:"<>|]', '_'
    $newFileName = "$safeTitle" + $file.Extension

    # Define full paths for the task file at each workflow stage
    $workingPath = Join-Path $workingDir $newFileName
    $finishedPath = Join-Path $finishedDir $newFileName
    $logPath = Join-Path $finishedDir ($newFileName + ".log")

    # Move task file from created directory to working directory
    # This marks the task as currently being processed
    Move-Item -Path $file.FullPath -Destination $workingPath -Force
    Write-Host "Moved to working: $workingPath"

    # Change working directory to repository root
    # This ensures that Copilot runs in the correct context
    Push-Location $repoRoot

    Write-Host "Executing Copilot for task: $title"
    Write-Host "Prompt: $prompt"

    try {
        # Escape double quotes in prompt for safe argument passing
        $promptEscaped = $prompt -replace '"', '\"'

        # Construct Copilot command arguments
        $copilotArgs = "-p `"$promptEscaped`" --allow-all-tools --allow-all-paths"

        # Define paths for temporary output and error logs
        $stdOutLog = $logPath + ".stdout"
        $stdErrLog = $logPath + ".stderr"

        # Execute Copilot tool with the task prompt
        # Capture both standard output and error output to separate files
        $process = Start-Process -FilePath "copilot" -ArgumentList $copilotArgs -NoNewWindow -PassThru -RedirectStandardOutput $stdOutLog -RedirectStandardError $stdErrLog -Wait

        # Combine stdout and stderr logs into a single log file
        # First append stdout if it exists
        if (Test-Path $stdOutLog) { Get-Content $stdOutLog | Out-File -FilePath $logPath -Append }
        # Then append stderr if it exists and contains content
        if (Test-Path $stdErrLog) {
            $errContent = Get-Content $stdErrLog
            if ($errContent) {
                "`r`n=== STDERR ===`r`n" | Out-File -FilePath $logPath -Append
                $errContent | Out-File -FilePath $logPath -Append
            }
        }

        # Clean up temporary log files
        Remove-Item $stdOutLog -ErrorAction SilentlyContinue
        Remove-Item $stdErrLog -ErrorAction SilentlyContinue

        Write-Host "Copilot execution finished with exit code $($process.ExitCode)"

        # Warn if Copilot exited with an error code
        if ($process.ExitCode -ne 0) {
            Write-Warning "Copilot exited with non-zero code. Check log: $logPath"
        }
    }
    catch {
        # Handle any errors that occur during Copilot execution
        Write-Error "Failed to execute copilot: $_"
        "Error executing copilot: $_" | Out-File -FilePath $logPath -Append
    }
    finally {
        # Always return to the original directory after execution
        Pop-Location
    }

    # Move completed task from working directory to finished directory
    Move-Item -Path $workingPath -Destination $finishedPath -Force
    Write-Host "Task moved to finished: $finishedPath"
    Write-Host "---------------------------------------------------"
}
