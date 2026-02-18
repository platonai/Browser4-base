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

# Find the first parent directory that contains a VERSION file
# This allows the script to be run from any location within the project
AppHome="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
while [[ ! -f "$AppHome/ROOT.md" ]] && [[ "$AppHome" != "/" ]]; do
    AppHome="$(dirname "$AppHome")"
done

cd "$AppHome"

# Define directory paths for task management workflow
baseDir="$AppHome/docs-dev/tasks/copilot"
createdDir="$baseDir/1created"        # Input directory for new tasks
workingDir="$baseDir/2working"        # Processing directory for current tasks
finishedDir="$baseDir/3finished"      # Output directory for completed tasks
logsDir="$baseDir/logs"              # Directory for script and execution logs
repoRoot="$AppHome"                  # Repository root for Copilot execution

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

    # Read file content
    content=$(cat "$file")

    # Initialize variables for task metadata
    title=""
    description=""
    prompt=""

    # Try to parse structured content with Title, Description, and Prompt sections
    # Format expected:
    # Title: ...
    # Description: ...
    # Prompt: ...
    # Uses bash regex matching to extract these fields if they follow the expected format
    if [[ $content =~ ^Title:[[:space:]]*([^$'\n']+)$'\n'Description:[[:space:]]*([^$'\n']+)$'\n'Prompt:[[:space:]]*(.*)$ ]]; then
        # Extract matched groups from structured format
        title="${BASH_REMATCH[1]}"
        description="${BASH_REMATCH[2]}"
        prompt="${BASH_REMATCH[3]}"
    else
        # Fallback: If file is not in structured format, treat entire content as prompt
        title="$(basename "$file" | sed 's/\.[^.]*$//')"
        description="Task from $(basename "$file")"
        prompt="$content"
    fi

    # Sanitize title to make it safe for use as a filename
    # Remove special characters that are not allowed in filenames across Unix systems
    safeTitle=$(echo "$title" | sed 's/[\/\\*?:"<>|]/_/g')

    # Extract file extension and construct new filename with sanitized title
    fileExt="${file##*.}"
    if [[ "$file" == *.* ]]; then
        newFileName="${safeTitle}.${fileExt}"
    else
        newFileName="$safeTitle"
    fi

    # Define full paths for the task file at each workflow stage
    workingPath="$workingDir/$newFileName"
    finishedPath="$finishedDir/$newFileName"

    # Task log path - combined log for task execution
    taskLogPath="$logsDir/task_${safeTitle}_$(date +%Y%m%d-%H%M%S).log"

    # Copilot-specific external tool log (separate from task log)
    copilotLogPath="$logsDir/copilot_${safeTitle}_$(date +%Y%m%d-%H%M%S).log"

    # Move task file from created directory to working directory
    # This marks the task as currently being processed
    mv "$file" "$workingPath"
    log_message "Moved to working: $workingPath" INFO
    log_verbose "Task log will be written to: $taskLogPath"

    # Change to repository root directory for execution
    # This ensures that Copilot runs in the correct context
    pushd "$repoRoot" > /dev/null || exit 1

    log_message "Executing Copilot for task: $title" INFO
    log_verbose "Task Description: $description"
    log_verbose "Prompt length: ${#prompt} characters"

    # Record task execution details to task log
    {
        echo "Task: $title"
        echo "Description: $description"
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

