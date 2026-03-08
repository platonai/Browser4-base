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

# Find repo root
REPO_ROOT=$(git rev-parse --show-toplevel 2>/dev/null)
if [ -z "$REPO_ROOT" ]; then
    echo "Repo root not found. Exiting."
    exit 1
fi

# Ensure absolute path
if command -v realpath >/dev/null 2>&1; then
    REPO_ROOT=$(realpath "$REPO_ROOT")
else
    cd "$REPO_ROOT" && REPO_ROOT=$(pwd)
fi

cd "$REPO_ROOT"

CONFIG_SH="$REPO_ROOT/coworker/scripts/config.sh"
if [[ -f "$CONFIG_SH" ]]; then
    # shellcheck disable=SC1090
    source "$CONFIG_SH"
fi

if ! declare -p COPILOT >/dev/null 2>&1; then
    COPILOT=(gh copilot)
fi

if [[ "$(declare -p COPILOT 2>/dev/null)" != declare\ -a* ]]; then
    echo "Error: COPILOT must be defined as a bash array in $CONFIG_SH" >&2
    exit 1
fi

LOG_DIR="$REPO_ROOT/coworker/tasks/300logs/$YEAR/$MONTH/$DAY"
MEMORY_FILE="$LOG_DIR/MEMORY.$YEAR$MONTH$DAY.md"

if [ ! -d "$LOG_DIR" ]; then
  # Create the directory if it doesn't exist
  mkdir -p "$LOG_DIR"
fi

echo "Generating daily memory for $DATE from logs in $LOG_DIR..."

# Function to extract clean prompt from task log
extract_clean_prompt() {
    local task_log="$1"
    # Extract prompt part, stop before Memory Instructions
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

# Batch processing variables
BATCH_SIZE=15000
CURRENT_BATCH=""
FIRST_BATCH=true

# Function to process a batch of logs
process_batch() {
    local content="$1"
    local is_first="$2"
    local instruction=""

    if [ "$is_first" = "true" ]; then
        instruction="
You are an AI assistant helping to generate a daily memory summary for a developer coworker.
Based on the following development logs, generate the content for the daily memory file and save it to the ABSOLUTE path: $MEMORY_FILE

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
- Use the \`create\` tool to write the file directly using the ABSOLUTE path: $MEMORY_FILE. If the file exists, overwrite it (I have already backed it up).
"
    else
        instruction="
You are continuing to generate the daily memory summary for $DATE.
The memory file '$MEMORY_FILE' has already been created with the summary of previous tasks.

YOUR TASK:
1. READ the existing content of '$MEMORY_FILE' (using ABSOLUTE path).
2. ANALYZE the NEW logs provided below.
3. UPDATE '$MEMORY_FILE' to include the summary of these NEW logs:
    - Append the new tasks to 'Tasks Executed'.
    - Update 'Execution Quality Review', 'Issues Encountered', etc., if the new logs provide additional insights.
    - Consolidate similar points if possible.
4. Ensure the final file maintains the markdown structure.

CONSTRAINTS:
- Use the \`edit\` tool (or \`read\` then \`create\` if needed) to update the file using the ABSOLUTE path: $MEMORY_FILE.
- Do NOT overwrite the entire file with just the new logs; you must MERGE/APPEND.
- Keep the existing summary valid while adding new information.
"
    fi

    local prompt="$instruction

LOGS (Batch):
$content
"

    # Truncate prompt if it's insanely long (safety net)
    if [ ${#prompt} -gt 25000 ]; then
        echo "Warning: Batch prompt is too long (${#prompt} chars). Truncating..."
        prompt="${prompt:0:25000} ... [Truncated]"
    fi

    echo "Calling gh copilot for batch..."
    "${COPILOT[@]}" -- -p "$prompt" --allow-all-tools
}

# Check if memory file exists and backup before starting
if [ -f "$MEMORY_FILE" ]; then
    echo "Memory file $MEMORY_FILE already exists."
    mv "$MEMORY_FILE" "$MEMORY_FILE.bak"
    echo "Backed up existing memory file to $MEMORY_FILE.bak"
fi

for task_log in "$LOG_DIR"/*.task.log; do
    if [ -f "$task_log" ]; then
        BASE_NAME=$(basename "$task_log" .task.log)
        COPILOT_LOG="$LOG_DIR/$BASE_NAME.copilot.log"

        TASK_CONTENT=$'\n\n=== TASK: '
        TASK_CONTENT+="$BASE_NAME"
        TASK_CONTENT+=$' ===\n'

        # Extract metadata
        TITLE=$(grep "^Task:" "$task_log" | head -n 1 | cut -d: -f2- | xargs)
        TASK_CONTENT+="Title: $TITLE"$'\n'

        TASK_CONTENT+="--- PROMPT (Snippet) ---\n"
        TASK_CONTENT+=$(extract_clean_prompt "$task_log")
        TASK_CONTENT+=$'\n'

        TASK_CONTENT+="--- RESULT (Snippet) ---\n"
        TASK_CONTENT+=$(extract_copilot_output "$COPILOT_LOG")
        TASK_CONTENT+=$'\n'

        # Check if adding this task exceeds batch size
        CURRENT_LEN=${#CURRENT_BATCH}
        TASK_LEN=${#TASK_CONTENT}

        if [ $((CURRENT_LEN + TASK_LEN)) -gt $BATCH_SIZE ] && [ $CURRENT_LEN -gt 0 ]; then
            process_batch "$CURRENT_BATCH" "$FIRST_BATCH"
            CURRENT_BATCH=""
            FIRST_BATCH=false
        fi

        CURRENT_BATCH+="$TASK_CONTENT"
    fi
done

# Process remaining
if [ -n "$CURRENT_BATCH" ]; then
    process_batch "$CURRENT_BATCH" "$FIRST_BATCH"
else
    if [ "$FIRST_BATCH" = "true" ]; then
        echo "No logs found for $DATE."
        exit 0
    fi
fi

echo "Memory generation task completed."
exit 0
