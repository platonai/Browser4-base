#!/usr/bin/env bash

# Find repo root
REPO_ROOT=$(git rev-parse --show-toplevel 2>/dev/null)
if [ -z "$REPO_ROOT" ]; then
    echo "Repo root not found. Exiting."
    exit 1
fi

cd "$REPO_ROOT" || exit 1

SCRIPT_PATH="$REPO_ROOT/coworker/scripts/coworker.sh"
SCRIPT_NAME="coworker.sh"
WRAPPER_NAME="run_coworker_periodically.sh"

echo "Monitoring $SCRIPT_NAME..."
echo "Script path: $SCRIPT_PATH"

while true; do
    # Check for tasks in 1created or 5approved
    HAS_TASKS=false
    
    # Check 1created
    if [ -d "coworker/tasks/1created" ]; then
        if [ "$(ls -A coworker/tasks/1created)" ]; then
            HAS_TASKS=true
        fi
    fi
    
    # Check 5approved if no tasks found yet
    if [ "$HAS_TASKS" = false ] && [ -d "coworker/tasks/5approved" ]; then
        if [ "$(ls -A coworker/tasks/5approved)" ]; then
            HAS_TASKS=true
        fi
    fi

    TIMESTAMP=$(date "+%Y-%m-%d %H:%M:%S")

    if [ "$HAS_TASKS" = false ]; then
        echo "$TIMESTAMP - No tasks found in 1created or 5approved. Skipping check."
        sleep 15
        continue
    fi

    # Check if script is running
    # We look for processes matching the script name, but exclude this wrapper script
    IS_RUNNING=false
    
    # Get PIDs of processes matching the script name
    PIDS=$(pgrep -f "$SCRIPT_NAME")
    
    if [ -n "$PIDS" ]; then
        for pid in $PIDS; do
            # Skip if it's the current process
            if [ "$pid" = "$$" ]; then
                continue
            fi
            
            # Get the command line for this PID
            CMDLINE=$(ps -p "$pid" -o args= 2>/dev/null)
            
            # Check if it contains SCRIPT_NAME and does NOT contain WRAPPER_NAME
            if [[ "$CMDLINE" == *"$SCRIPT_NAME"* ]] && [[ "$CMDLINE" != *"$WRAPPER_NAME"* ]]; then
                IS_RUNNING=true
                break
            fi
        done
    fi

    if [ "$IS_RUNNING" = true ]; then
        echo "$TIMESTAMP - $SCRIPT_NAME is already running."
    else
        echo "$TIMESTAMP - $SCRIPT_NAME is NOT running. Starting it..."
        
        if [ -f "$SCRIPT_PATH" ]; then
            # Make sure it's executable
            if [ ! -x "$SCRIPT_PATH" ]; then
                chmod +x "$SCRIPT_PATH"
            fi
            
            # Run the script
            # We run it directly and wait for it to complete. 
            # This ensures we don't start multiple instances concurrently from this loop.
            "$SCRIPT_PATH"
            
            echo "Finished $SCRIPT_NAME execution."
        else
            echo "Error: Script not found at $SCRIPT_PATH"
        fi
    fi

    sleep 15
done
