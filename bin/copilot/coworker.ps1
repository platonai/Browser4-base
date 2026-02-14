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
$logsDir = Join-Path $baseDir "logs"              # Directory for script and execution logs
$repoRoot = $AppHome                              # Repository root for Copilot execution

# Ensure all required directories exist
# Create them if they don't already exist
if (!(Test-Path $createdDir)) { New-Item -ItemType Directory -Path $createdDir | Out-Null }
if (!(Test-Path $workingDir)) { New-Item -ItemType Directory -Path $workingDir | Out-Null }
if (!(Test-Path $finishedDir)) { New-Item -ItemType Directory -Path $finishedDir | Out-Null }
if (!(Test-Path $logsDir)) { New-Item -ItemType Directory -Path $logsDir | Out-Null }

# Initialize script-level logging
# Main log file for all script output
$scriptLogPath = Join-Path $logsDir "coworker-$(Get-Date -Format 'yyyyMMdd-HHmmss').log"
$scriptStartTime = Get-Date

# ============================================================================
# Logging Functions
# ============================================================================

# Function: Write message to console and main script log file
function Write-LogMessage {
    param(
        [Parameter(Mandatory=$true)]
        [string]$Message,
        [ValidateSet('INFO', 'WARN', 'ERROR')]
        [string]$Level = 'INFO'
    )

    $timestamp = Get-Date -Format 'yyyy-MM-dd HH:mm:ss'
    $logEntry = "[$timestamp] [$Level] $Message"

    # Write to console
    switch ($Level) {
        'INFO' { Write-Host $logEntry }
        'WARN' { Write-Host $logEntry -ForegroundColor Yellow }
        'ERROR' { Write-Host $logEntry -ForegroundColor Red }
    }

    # Append to script log file
    $logEntry | Out-File -FilePath $scriptLogPath -Append
}

# Function: Write message only to log file (for verbose output)
function Write-LogVerbose {
    param(
        [Parameter(Mandatory=$true)]
        [string]$Message
    )

    $timestamp = Get-Date -Format 'yyyy-MM-dd HH:mm:ss'
    $logEntry = "[$timestamp] [DEBUG] $Message"

    # Append to script log file only (not console)
    $logEntry | Out-File -FilePath $scriptLogPath -Append
}

# Log script startup
Write-LogMessage "===========================================================================" INFO
Write-LogMessage "Coworker Task Runner - PowerShell Version" INFO
Write-LogMessage "Started at: $scriptStartTime" INFO
Write-LogMessage "Script Log: $scriptLogPath" INFO
Write-LogMessage "==========================================================================" INFO

$files = Get-ChildItem -Path $createdDir

# Process each task file found in the created directory
foreach ($file in $files) {
    Write-LogMessage "Processing $($file.Name)..." INFO

    # Read the entire file content
    $content = Get-Content -Path $file.FullName -Raw

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

    # Task log path - combined log for task execution
    $taskLogPath = Join-Path $logsDir "task_${safeTitle}_$(Get-Date -Format 'yyyyMMdd-HHmmss').log"

    # Copilot-specific external tool log (separate from task log)
    $copilotLogPath = Join-Path $logsDir "copilot_${safeTitle}_$(Get-Date -Format 'yyyyMMdd-HHmmss').log"

    # Move task file from created directory to working directory
    # This marks the task as currently being processed
    Move-Item -Path $file.FullName -Destination $workingPath -Force
    Write-LogMessage "Moved to working: $workingPath" INFO
    Write-LogVerbose "Task log will be written to: $taskLogPath"

    # Change working directory to repository root
    # This ensures that Copilot runs in the correct context
    Push-Location $repoRoot

    Write-LogMessage "Executing Copilot for task: $title" INFO
    Write-LogVerbose "Task Description: $description"
    Write-LogVerbose "Prompt length: $($prompt.Length) characters"

    # Record task execution details to task log
    @"
Task: $title
Description: $description
Started: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')
Prompt:
$prompt
---
Copilot Execution Output:
"@ | Out-File -FilePath $taskLogPath

    try {
        # Escape double quotes in prompt for safe argument passing
        $promptEscaped = $prompt -replace '"', '\"'

        # Construct Copilot command arguments
        $copilotArgs = "-p `"$promptEscaped`" --allow-all-tools --allow-all-paths"

        # Define paths for temporary output and error logs (for copilot external tool)
        $stdOutLog = $copilotLogPath + ".stdout"
        $stdErrLog = $copilotLogPath + ".stderr"

        # Execute Copilot tool with the task prompt
        # Capture both standard output and error output to separate files
        $process = Start-Process -FilePath "copilot" -ArgumentList $copilotArgs -NoNewWindow -PassThru -RedirectStandardOutput $stdOutLog -RedirectStandardError $stdErrLog -Wait

        # Combine copilot stdout and stderr logs into the copilot-specific log
        # First append stdout if it exists
        if (Test-Path $stdOutLog) { Get-Content $stdOutLog | Out-File -FilePath $copilotLogPath -Append }
        # Then append stderr if it exists and contains content
        if (Test-Path $stdErrLog) {
            $errContent = Get-Content $stdErrLog
            if ($errContent) {
                "`r`n=== COPILOT STDERR ===`r`n" | Out-File -FilePath $copilotLogPath -Append
                $errContent | Out-File -FilePath $copilotLogPath -Append
            }
        }

        # Clean up temporary log files
        Remove-Item $stdOutLog -ErrorAction SilentlyContinue
        Remove-Item $stdErrLog -ErrorAction SilentlyContinue

        Write-LogMessage "Copilot execution finished with exit code $($process.ExitCode)" INFO
        Write-LogVerbose "Copilot external tool log: $copilotLogPath"

        # Append copilot result to task log
        @"

Copilot Exit Code: $($process.ExitCode)
Copilot Log: $copilotLogPath
"@ | Out-File -FilePath $taskLogPath -Append

        # Warn if Copilot exited with an error code
        if ($process.ExitCode -ne 0) {
            Write-LogMessage "Warning: Copilot exited with non-zero code. Check log: $copilotLogPath" WARN
        }
    }
    catch {
        # Handle any errors that occur during Copilot execution
        Write-LogMessage "Failed to execute copilot: $_" ERROR
        "Error executing copilot: $_" | Out-File -FilePath $taskLogPath -Append
    }
    finally {
        # Always return to the original directory after execution
        Pop-Location
    }

    # Move completed task from working directory to finished directory
    Move-Item -Path $workingPath -Destination $finishedPath -Force
    Write-LogMessage "Task moved to finished: $finishedPath" INFO
    Write-LogMessage "---" INFO
}

# Log script completion
$scriptEndTime = Get-Date
Write-LogMessage "===========================================================================" INFO
Write-LogMessage "All tasks completed" INFO
Write-LogMessage "Ended at: $scriptEndTime" INFO
Write-LogMessage "Script Log: $scriptLogPath" INFO
Write-LogMessage "==========================================================================" INFO

