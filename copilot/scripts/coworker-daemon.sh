#!/usr/bin/env bash

# Daemon script for coworker.sh - runs every 30 seconds with lock-based concurrency control
# Usage: ./coworker-daemon.sh [start|stop|status|restart]

# Find the first parent directory that contains a VERSION file
AppHome="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
while [[ ! -f "$AppHome/ROOT.md" ]] && [[ "$AppHome" != "/" ]]; do
    AppHome="$(dirname "$AppHome")"
done

# Configuration
SCRIPT_DIR="$AppHome/bin/copilot"
COWORKER_SCRIPT="$SCRIPT_DIR/coworker.sh"
LOCK_DIR="${TMPDIR:-.}/coworker-locks"
LOCK_FILE="$LOCK_DIR/coworker.lock"
PID_FILE="$LOCK_DIR/coworker-daemon.pid"
LOG_DIR="$AppHome/logs/coworker"
LOG_FILE="$LOG_DIR/coworker-daemon.log"

# Interval in seconds
INTERVAL=30

# Initialize
mkdir -p "$LOCK_DIR"
mkdir -p "$LOG_DIR"

# Logging function
log_message() {
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo "[$timestamp] $1" | tee -a "$LOG_FILE"
}

# Acquire lock
acquire_lock() {
    # Check if another instance is running
    if [[ -f "$LOCK_FILE" ]]; then
        local pid=$(cat "$LOCK_FILE")
        # Check if the process is still running
        if ps -p "$pid" > /dev/null 2>&1; then
            return 1  # Another instance is running
        else
            # Process is dead, remove stale lock
            rm -f "$LOCK_FILE"
        fi
    fi

    echo $$ > "$LOCK_FILE"
    return 0
}

# Release lock
release_lock() {
    rm -f "$LOCK_FILE"
}

# Run coworker script with lock
run_coworker() {
    if ! acquire_lock; then
        log_message "[SKIP] coworker.sh is already running, skipping this cycle"
        return 0
    fi

    log_message "[START] Executing coworker.sh"

    if bash "$COWORKER_SCRIPT" >> "$LOG_FILE" 2>&1; then
        log_message "[SUCCESS] coworker.sh completed successfully"
    else
        local exit_code=$?
        log_message "[ERROR] coworker.sh failed with exit code $exit_code"
    fi

    release_lock
}

# Start daemon
start_daemon() {
    if [[ -f "$PID_FILE" ]]; then
        local pid=$(cat "$PID_FILE")
        if ps -p "$pid" > /dev/null 2>&1; then
            echo "Daemon is already running (PID: $pid)"
            return 0
        else
            rm -f "$PID_FILE"
        fi
    fi

    echo "Starting coworker daemon..."

    # Start daemon in background
    (
        echo $$ > "$PID_FILE"
        log_message "[DAEMON] Starting daemon with PID $$"

        while true; do
            run_coworker
            sleep "$INTERVAL"
        done
    ) &

    local daemon_pid=$!
    sleep 0.5

    if ps -p "$daemon_pid" > /dev/null 2>&1; then
        echo "Daemon started successfully (PID: $daemon_pid)"
        log_message "[DAEMON] Daemon started successfully with PID $daemon_pid"
    else
        echo "Failed to start daemon"
        rm -f "$PID_FILE"
        return 1
    fi
}

# Stop daemon
stop_daemon() {
    if [[ ! -f "$PID_FILE" ]]; then
        echo "Daemon is not running"
        return 0
    fi

    local pid=$(cat "$PID_FILE")

    if ! ps -p "$pid" > /dev/null 2>&1; then
        echo "Daemon is not running (stale PID file: $pid)"
        rm -f "$PID_FILE"
        return 0
    fi

    echo "Stopping daemon (PID: $pid)..."

    # Send SIGTERM to the daemon process group
    kill -TERM -$pid 2>/dev/null || kill -TERM "$pid" 2>/dev/null

    # Wait for graceful shutdown
    local count=0
    while ps -p "$pid" > /dev/null 2>&1 && [[ $count -lt 10 ]]; do
        sleep 0.5
        ((count++))
    done

    # Force kill if still running
    if ps -p "$pid" > /dev/null 2>&1; then
        echo "Force killing daemon (PID: $pid)..."
        kill -9 "$pid" 2>/dev/null
    fi

    rm -f "$PID_FILE"
    log_message "[DAEMON] Daemon stopped"
    echo "Daemon stopped"
}

# Check status
status_daemon() {
    if [[ ! -f "$PID_FILE" ]]; then
        echo "Daemon is not running"
        return 1
    fi

    local pid=$(cat "$PID_FILE")

    if ps -p "$pid" > /dev/null 2>&1; then
        echo "Daemon is running (PID: $pid)"
        return 0
    else
        echo "Daemon is not running (stale PID file)"
        rm -f "$PID_FILE"
        return 1
    fi
}

# Restart daemon
restart_daemon() {
    stop_daemon
    sleep 1
    start_daemon
}

# Main
case "${1:-start}" in
    start)
        start_daemon
        ;;
    stop)
        stop_daemon
        ;;
    status)
        status_daemon
        ;;
    restart)
        restart_daemon
        ;;
    *)
        echo "Usage: $0 {start|stop|status|restart}"
        echo ""
        echo "Commands:"
        echo "  start   - Start the daemon"
        echo "  stop    - Stop the daemon"
        echo "  status  - Check daemon status"
        echo "  restart - Restart the daemon"
        echo ""
        echo "The daemon will execute coworker.sh every $INTERVAL seconds."
        echo "If coworker.sh is already running, the cycle will be skipped."
        echo ""
        echo "Logs: $LOG_FILE"
        echo "Lock directory: $LOCK_DIR"
        exit 1
        ;;
esac

