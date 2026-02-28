#!/bin/bash

# Coworker Daily Memory Generator
# usage: ./coworker-daily-memory-generator.sh [YYYY-MM-DD]

set -e

# Determine date
if [ -n "$1" ]; then
    DATE="$1"
else
    DATE=$(date -u +%Y-%m-%d)
fi

YEAR=$(date -d "$DATE" +%Y)
MONTH=$(date -d "$DATE" +%m)
DAY=$(date -d "$DATE" +%d)

LOG_DIR="coworker/tasks/300logs/$YEAR/$MONTH/$DAY"
MEMORY_FILE="$LOG_DIR/MEMORY.$YEAR$MONTH$DAY.md"

if [ ! -d "$LOG_DIR" ]; then
    echo "Error: Log directory $LOG_DIR does not exist."
    exit 1
fi

echo "Generating daily memory for $DATE from logs in $LOG_DIR..."

# Collect logs
LOG_CONTENT=""

# Function to extract clean prompt from task log
extract_clean_prompt() {
    local task_log="$1"
    # Extract prompt part, stop before Memory Instructions
    # We use sed to print from "Prompt:" to "*** MEMORY UPDATE INSTRUCTIONS ***" or end of file
    sed -n '/^Prompt:/,/\*\*\* MEMORY UPDATE INSTRUCTIONS \*\*\*/p' "$task_log" | \
    grep -v "^Prompt:" | \
    grep -v "\*\*\* MEMORY UPDATE INSTRUCTIONS \*\*\*" | \
    head -c 2000 # Limit prompt length per task to 2000 chars to save tokens
}

# Function to extract copilot output
extract_copilot_output() {
    local copilot_log="$1"
    if [ -f "$copilot_log" ]; then
        # Head 10 lines
        head -n 10 "$copilot_log"
        echo ""
        echo "... [Intermediate logs skipped] ..."
        echo ""

        # Find line number of the LAST tool execution
        # We use grep with line numbers, filter for the pattern, take the last one, extract the number
        local last_tool_line=$(grep -nE "^● (Read|Edit|Run)" "$copilot_log" | tail -n 1 | cut -d: -f1)

        if [ -n "$last_tool_line" ]; then
             # Extract from that line to the end
             tail -n "+$last_tool_line" "$copilot_log"
        else
             # Fallback if no tool execution found (e.g. just chat), take last 100 lines
             tail -n 100 "$copilot_log"
        fi | head -c 20000
    else
        echo "[Copilot log not found]"
    fi
}

for task_log in "$LOG_DIR"/*.task.log; do
    if [ -f "$task_log" ]; then
        BASE_NAME=$(basename "$task_log" .task.log)
        COPILOT_LOG="$LOG_DIR/$BASE_NAME.copilot.log"
        
        LOG_CONTENT+=$'\n\n=== TASK: '
        LOG_CONTENT+="$BASE_NAME"
        LOG_CONTENT+=$' ===\n'
        
        # Extract metadata
        TITLE=$(grep "^Task:" "$task_log" | head -n 1 | cut -d: -f2- | xargs)
        LOG_CONTENT+="Title: $TITLE"$'\n'
        
        LOG_CONTENT+="--- PROMPT (Snippet) ---\n"
        LOG_CONTENT+=$(extract_clean_prompt "$task_log")
        LOG_CONTENT+=$'\n'
        
        LOG_CONTENT+="--- RESULT (Snippet) ---\n"
        LOG_CONTENT+=$(extract_copilot_output "$COPILOT_LOG")
        LOG_CONTENT+=$'\n'
    fi
done

# Check if we have content
if [ -z "$LOG_CONTENT" ]; then
    echo "No task logs found for $DATE."
    exit 0
fi

# Construct Prompt
PROMPT="
You are an AI assistant helping to generate a daily memory summary for a developer coworker.
Based on the following development logs, generate the content for the daily memory file and save it to: $MEMORY_FILE

SPECIFICATION:
# MEMORY.$YEAR$MONTH$DAY.md
## Daily Memory - $DATE

### Tasks Executed
- ...

### Execution Quality Review
- What worked well
- What was inefficient

### Issues Encountered
- ...

### Root Cause Analysis
- ...

### Process Improvement Insight
- At least one concrete improvement for future execution

CONSTRAINTS:
- Use English only.
- Be concise but insightful.
- Focus on structural issues and improvements.
- Do NOT just list logs, synthesize them.
- Use the \`create\` tool to write the file directly. If the file exists, overwrite it (I have already backed it up).

LOGS:
$LOG_CONTENT
"

# Call gh copilot to generate the content
# We use -p for prompt and --allow-all-tools to let it use create tool

# Ensure output directory exists (it should, since we checked LOG_DIR)
mkdir -p "$LOG_DIR"

# Check if memory file exists
if [ -f "$MEMORY_FILE" ]; then
    echo "Memory file $MEMORY_FILE already exists."
    mv "$MEMORY_FILE" "$MEMORY_FILE.bak"
    echo "Backed up existing memory file to $MEMORY_FILE.bak"
fi

echo "Calling gh copilot..."

# Warning: Command line length limit. If logs are huge, we need another approach.
# Truncate logs if necessary (e.g. last 1000 lines or by chars)
PROMPT_LENGTH=${#PROMPT}
if [ $PROMPT_LENGTH -gt 20000 ]; then
    echo "Warning: Logs are too long ($PROMPT_LENGTH chars). Truncating..."
    PROMPT="${PROMPT:0:20000} ... [Truncated]"
fi

# Use proper quoting for bash
gh copilot -p "$PROMPT" --allow-all-tools

echo "Memory generation task completed."
