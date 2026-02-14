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
while [[ ! -f "$AppHome/VERSION" ]] && [[ "$AppHome" != "/" ]]; do
    AppHome="$(dirname "$AppHome")"
done

cd "$AppHome"

# Define directory paths for task management workflow
baseDir="$AppHome/docs-dev/copilot/tasks/daily"
createdDir="$baseDir/created"        # Input directory for new tasks
workingDir="$baseDir/working"        # Processing directory for current tasks
finishedDir="$baseDir/finished"      # Output directory for completed tasks
repoRoot="$AppHome"                  # Repository root for Copilot execution

# Ensure all required directories exist
# Create them if they don't already exist
mkdir -p "$createdDir"
mkdir -p "$workingDir"
mkdir -p "$finishedDir"

# Process each file in the created directory
for file in "$createdDir"/*; do
    # Skip if directory is empty
    [[ -e "$file" ]] || continue

    echo "Processing $(basename "$file")..."

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
    logPath="$finishedDir/${newFileName}.log"

    # Move task file from created directory to working directory
    # This marks the task as currently being processed
    mv "$file" "$workingPath"
    echo "Moved to working: $workingPath"

    # Change to repository root directory for execution
    # This ensures that Copilot runs in the correct context
    pushd "$repoRoot" > /dev/null || exit 1

    echo "Executing Copilot for task: $title"
    echo "Prompt: $prompt"

    # Execute Copilot and handle logging and error handling
    {
        # Define paths for temporary output and error logs
        stdOutLog="${logPath}.stdout"
        stdErrLog="${logPath}.stderr"

        # Execute copilot tool with the task prompt
        # Capture both standard output and standard error to separate files
        if copilot -p "$prompt" --allow-all-tools --allow-all-paths > "$stdOutLog" 2> "$stdErrLog"; then
            exitCode=$?
        else
            exitCode=$?
        fi

        # Combine stdout and stderr logs into a single log file
        # First append stdout if it exists
        if [[ -f "$stdOutLog" ]]; then
            cat "$stdOutLog" >> "$logPath"
        fi
        # Then append stderr if it exists and contains content
        if [[ -f "$stdErrLog" && -s "$stdErrLog" ]]; then
            {
                echo ""
                echo "=== STDERR ==="
                echo ""
                cat "$stdErrLog"
            } >> "$logPath"
        fi

        # Clean up temporary log files
        rm -f "$stdOutLog"
        rm -f "$stdErrLog"

        echo "Copilot execution finished with exit code $exitCode"

        # Warn if Copilot exited with an error code
        if [[ $exitCode -ne 0 ]]; then
            echo "Warning: Copilot exited with non-zero code. Check log: $logPath" >&2
        fi
    } || {
        # Handle any errors that occur during script execution
        echo "Failed to execute copilot: $?" >&2
        {
            echo ""
            echo "Error executing copilot"
        } >> "$logPath"
    }

    # Return to previous directory from pushd
    popd > /dev/null || exit 1

    # Move completed task from working directory to finished directory
    mv "$workingPath" "$finishedPath"
    echo "Task moved to finished: $finishedPath"
    echo "---------------------------------------------------"
done

