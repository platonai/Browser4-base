#!/usr/bin/env bash

# ============================================================================
# Coworker Task Runner - Bash Shell Version
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
#   bash coworker.sh
#   ./coworker.sh
# ============================================================================


# This allows the script to be run from any location within the project
AppHome="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
while [[ ! -f "$repoRoot/ROOT.md" ]] && [[ "$repoRoot" != "/" ]]; do
    AppHome="$(dirname "$repoRoot")"
done

cd "$repoRoot"

# Define directory paths for task management workflow
baseDir="$repoRoot/coworker/tasks"
createdDir="$baseDir/1created"        # Input directory for new tasks
workingDir="$baseDir/2working"        # Processing directory for current tasks
finishedDir="$baseDir/3finished"      # Output directory for completed tasks
logsDir="$baseDir/logs"              # Directory for script and execution logs
repoRoot="$repoRoot"                  # Repository root for Copilot execution

# Ensure all required directories exist
# Create them if they don't already exist
mkdir -p "$createdDir"
mkdir -p "$workingDir"
mkdir -p "$finishedDir"
mkdir -p "$logsDir"

# Initialize script-level logging
# Main log file for all script output
scriptLogPath="$logsDir/coworker-$(date +%Y%m%d-%H%M%S).log"
scriptStartTime=$(date '+%Y-%m-%d %H:%M:%S')

# ============================================================================
# Logging Functions
# ============================================================================

# Function: Write message to console and main script log file
# Usage: log_message "message text" [LEVEL]
# Levels: INFO (default), WARN, ERROR
log_message() {
    local message="$1"
    local level="${2:-INFO}"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    local logEntry="[$timestamp] [$level] $message"

    # Write to console
    case "$level" in
        WARN)
            echo -e "\033[33m$logEntry\033[0m"  # Yellow for warnings
            ;;
        ERROR)
            echo -e "\033[31m$logEntry\033[0m" >&2  # Red for errors
            ;;
        *)
            echo "$logEntry"
            ;;
    esac

    # Append to script log file
    echo "$logEntry" >> "$scriptLogPath"
}

# Function: Write message only to log file (for verbose output)
# Usage: log_verbose "debug message"
log_verbose() {
    local message="$1"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    local logEntry="[$timestamp] [DEBUG] $message"

    # Append to script log file only (not console)
    echo "$logEntry" >> "$scriptLogPath"
}

# Log script startup
log_message "===========================================================================" INFO
log_message "Coworker Task Runner - Bash Shell Version" INFO
log_message "Started at: $scriptStartTime" INFO
log_message "Script Log: $scriptLogPath" INFO
log_message "==========================================================================" INFO

# Process each file in the created directory
for file in "$createdDir"/*; do
    # Skip if directory is empty
    [[ -e "$file" ]] || continue

    log_message "Processing $(basename "$file")..." INFO

    # 1. Move to working directory with a temporary name
    # This ensures we 'claim' the task immediately
    tempName="processing-$(basename "$file")"
    tempPath="$workingDir/$tempName"

    mv "$file" "$tempPath"
    log_message "Moved to working (processing): $tempPath" INFO

    # Read content for basic info
    content=$(cat "$tempPath")

    # Initialize variables
    title="$(basename "$file" | sed 's/\.[^.]*$//')"
    safeTitle=$(echo "$title" | sed 's/[\/\\*?:"<>|]/_/g')
    description="Task from $(basename "$file")"
    prompt="$content"

    # Try to parse structured content
    if [[ $content =~ ^Title:[[:space:]]*([^$'\n']+)$'\n'Description:[[:space:]]*([^$'\n']+)$'\n'Prompt:[[:space:]]*(.*)$ ]]; then
        title="${BASH_REMATCH[1]}"
        description="${BASH_REMATCH[2]}"
        prompt="${BASH_REMATCH[3]}"
    fi

    # 2. Call rename.sh on the file in working dir
    scriptDir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    renameScript="$scriptDir/rename.sh"
    descriptiveName=""

    chmod +x "$renameScript" 2>/dev/null

    if [[ -f "$renameScript" && -x "$renameScript" ]]; then
        generatedName=$("$renameScript" "$tempPath")
        if [[ -n "$generatedName" && "$generatedName" != *" "* && "$generatedName" != "Error"* ]]; then
            descriptiveName="$generatedName"
        fi
    fi

    if [[ -z "$descriptiveName" ]]; then
         descriptiveName="$safeTitle"
    fi

    # 3. Rename the file in working directory to the final descriptive name
    fileExt="${file##*.}"
    if [[ "$tempName" == *.* ]]; then
        newFileName="${descriptiveName}.${fileExt}"
    else
        newFileName="$descriptiveName"
    fi

    workingPath="$workingDir/$newFileName"

    # Handle filename collision in working dir
    if [[ -e "$workingPath" ]]; then
        counter=2
        while [[ -e "$workingDir/$descriptiveName.$counter.$fileExt" ]]; do
            ((counter++))
        done
        newFileName="$descriptiveName.$counter.$fileExt"
        workingPath="$workingDir/$newFileName"
    fi

    mv "$tempPath" "$workingPath"
    log_message "Renamed to: $workingPath" INFO
    finishedPath="$finishedDir/$newFileName"

    # Task log path
    taskLogPath="$logsDir/task_${newFileName}_$(date +%Y%m%d-%H%M%S).log"
    copilotLogPath="$logsDir/copilot_${newFileName}_$(date +%Y%m%d-%H%M%S).log"

    log_verbose "Task log will be written to: $taskLogPath"

    # Change to repository root directory for execution
    pushd "$repoRoot" > /dev/null || exit 1

    log_message "Executing Copilot for task: $descriptiveName" INFO
    log_verbose "Prompt length: ${#prompt} characters"

    # Record task execution details to task log
    {
        echo "Task: $descriptiveName"
        echo "Description: $description"
        echo "Original File: $(basename "$file")"
        echo "Started: $(date '+%Y-%m-%d %H:%M:%S')"
        echo "Prompt:"
        echo "$prompt"
        echo "---"
        echo "Copilot Execution Output:"
    } > "$taskLogPath"

    # Execute Copilot and handle logging and error handling
    {
        # Define paths for temporary output and error logs
        stdOutLog="${copilotLogPath}.stdout"
        stdErrLog="${copilotLogPath}.stderr"

        # Execute copilot tool with the task prompt
        # Capture both standard output and standard error to separate files
        if gh copilot -p "$prompt" --allow-all-tools --allow-all-paths > "$stdOutLog" 2> "$stdErrLog"; then
            exitCode=$?
        else
            exitCode=$?
        fi

        # Combine copilot stdout and stderr logs into the copilot-specific log
        # First append stdout if it exists
        if [[ -f "$stdOutLog" ]]; then
            cat "$stdOutLog" >> "$copilotLogPath"
        fi
        # Then append stderr if it exists and contains content
        if [[ -f "$stdErrLog" && -s "$stdErrLog" ]]; then
            {
                echo ""
                echo "=== COPILOT STDERR ==="
                echo ""
                cat "$stdErrLog"
            } >> "$copilotLogPath"
        fi

        # Clean up temporary log files
        rm -f "$stdOutLog"
        rm -f "$stdErrLog"

        log_message "Copilot execution finished with exit code $exitCode" INFO
        log_verbose "Copilot external tool log: $copilotLogPath"

        # Append copilot result to task log
        {
            echo ""
            echo "Copilot Exit Code: $exitCode"
            echo "Copilot Log: $copilotLogPath"
        } >> "$taskLogPath"

        # Warn if Copilot exited with an error code
        if [[ $exitCode -ne 0 ]]; then
            log_message "Warning: Copilot exited with non-zero code. Check log: $copilotLogPath" WARN
        fi
    } || {
        # Handle any errors that occur during script execution
        log_message "Failed to execute copilot: $?" ERROR
        {
            echo ""
            echo "Error executing copilot"
        } >> "$taskLogPath"
    }

    # Return to previous directory from pushd
    popd > /dev/null || exit 1

    # Move completed task from working directory to finished directory
    # Create date-based subdirectory: YYYY/MMDD
    currentYear=$(date +%Y)
    currentDate=$(date +%m%d)
    finishedSubDir="$finishedDir/$currentYear/$currentDate"
    mkdir -p "$finishedSubDir"

    finishedPath="$finishedSubDir/$newFileName"

    mv "$workingPath" "$finishedPath"
    log_message "Task moved to finished: $finishedPath" INFO
    log_message "---" INFO
done

# Log script completion
scriptEndTime=$(date '+%Y-%m-%d %H:%M:%S')
log_message "===========================================================================" INFO
log_message "All tasks completed" INFO
log_message "Ended at: $scriptEndTime" INFO
log_message "Script Log: $scriptLogPath" INFO
log_message "==========================================================================" INFO

