#!/bin/bash

# Coworker Memory Generator
# usage: ./coworker-memory-generator.sh [type] [date]
# type: daily (default), monthly, yearly, all
# date: YYYY-MM-DD (default: today)

TYPE="${1:-daily}"
DATE="${2:-$(date -u +%Y-%m-%d)}"

set -e

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

YEAR=$(date -d "$DATE" +%Y)
MONTH=$(date -d "$DATE" +%m)
DAY=$(date -d "$DATE" +%d)

LOGS_BASE_DIR="$REPO_ROOT/coworker/tasks/300logs"

invoke_gh_copilot() {
    local PROMPT="$1"
    local CAPTURE="$2"
    
    # Truncate if too long (approx check)
    if [ ${#PROMPT} -gt 25000 ]; then
        >&2 echo "Warning: Prompt is too long (${#PROMPT} chars). Truncating..."
        PROMPT="${PROMPT:0:25000} ... [Truncated]"
    fi

    if [ "$CAPTURE" != "true" ]; then
        >&2 echo "Calling gh copilot..."
    fi
    
    # We use eval or simple execution?
    # gh copilot -- -p "..."
    if [ "$CAPTURE" == "true" ]; then
        gh copilot -- -p "$PROMPT" --allow-all-tools
    else
        gh copilot -- -p "$PROMPT" --allow-all-tools
    fi
}


if [ "$TYPE" == "daily" ]; then
    DAILY_SCRIPT="$REPO_ROOT/coworker/scripts/workers/coworker-daily-memory-generator.sh"
    if [ -f "$DAILY_SCRIPT" ]; then
        "$DAILY_SCRIPT" "$DATE"
    else
        echo "Error: Daily memory generator script not found at $DAILY_SCRIPT"
        exit 1
    fi

elif [ "$TYPE" == "monthly" ]; then
    TARGET_DIR="$LOGS_BASE_DIR/$YEAR/$MONTH"
    TARGET_FILE="$TARGET_DIR/MEMORY.$YEAR$MONTH.md"
    
    if [ ! -d "$TARGET_DIR" ]; then
        echo "Error: Directory $TARGET_DIR does not exist. No daily memories to summarize."
        exit 1
    fi

    # Gather all daily memories for the month
    COMBINED_CONTENT=""
    # Use find to locate files recursively in the month directory
    while IFS= read -r file; do
        CONTENT=$(cat "$file")
        COMBINED_CONTENT+=$'\n\n=== DAILY MEMORY: '
        COMBINED_CONTENT+=$(basename "$file")
        COMBINED_CONTENT+=$' ===\n'
        COMBINED_CONTENT+="$CONTENT"
    done < <(find "$TARGET_DIR" -name "MEMORY.*.md")

    if [ -z "$COMBINED_CONTENT" ]; then
        echo "Warning: No daily memories found for $YEAR-$MONTH."
        exit 0
    fi

    PROMPT="
You are an AI assistant helping to generate a MONTHLY memory summary for a developer coworker.
Based on the following DAILY memories, generate the content for the MONTHLY memory file and save it to the ABSOLUTE path: $TARGET_FILE

SPECIFICATION:
# MEMORY.$YEAR$MONTH.md
## Monthly Memory - $YEAR-$MONTH

### Work Themes
- Major areas of focus this month

### Recurring Issues
- Problems that happened multiple times

### Structural Bottlenecks
- Process or technical limitations slowing progress

### Efficiency Trend
- Qualitative assessment of speed/quality over the month

### System Adjustments Proposed
- Changes to tools/workflow based on this month's experience

CONSTRAINTS:
- Use English only.
- Synthesize, don't just list.
- Use the \`create\` tool to write the file directly using the ABSOLUTE path: $TARGET_FILE
- Overwrite if exists.

DAILY MEMORIES:
$COMBINED_CONTENT
"
    invoke_gh_copilot "$PROMPT"

elif [ "$TYPE" == "yearly" ]; then
    TARGET_DIR="$LOGS_BASE_DIR/$YEAR"
    TARGET_FILE="$TARGET_DIR/MEMORY.$YEAR.md"
    
    # Gather all monthly memories for the year
    COMBINED_CONTENT=""
    # Monthly memories are at logs/YYYY/MM/MEMORY.YYYYMM.md
    while IFS= read -r file; do
        CONTENT=$(cat "$file")
        COMBINED_CONTENT+=$'\n\n=== MONTHLY MEMORY: '
        COMBINED_CONTENT+=$(basename "$file")
        COMBINED_CONTENT+=$' ===\n'
        COMBINED_CONTENT+="$CONTENT"
    done < <(find "$LOGS_BASE_DIR/$YEAR" -name "MEMORY.${YEAR}??.md") # Match YYYYMM pattern

    if [ -z "$COMBINED_CONTENT" ]; then
        echo "Warning: No monthly memories found for $YEAR."
        exit 0
    fi

    PROMPT="
You are an AI assistant helping to generate a YEARLY memory summary for a developer coworker.
Based on the following MONTHLY memories, generate the content for the YEARLY memory file and save it to the ABSOLUTE path: $TARGET_FILE

SPECIFICATION:
# MEMORY.$YEAR.md
## Annual Strategic Review - $YEAR

### Project State Evolution
- High-level changes in project scope/maturity

### Major Achievements
- Key milestones reached

### Major Failures
- Significant setbacks and lessons

### Structural Problems (Solved / Unsolved)
- Persistent issues

### Capability Upgrades
- New skills/tools acquired

### Strategic Risks
- Potential future threats

### Project Trajectory Forecast
- Where the project is heading

### Three Immediate Strategic Actions
- High-level next steps for next year

CONSTRAINTS:
- Use English only.
- Synthesize, don't just list.
- Use the \`create\` tool to write the file directly using the ABSOLUTE path: $TARGET_FILE
- Overwrite if exists.

MONTHLY MEMORIES:
$COMBINED_CONTENT
"
    invoke_gh_copilot "$PROMPT"

elif [ "$TYPE" == "all" ]; then
    TARGET_FILE="$LOGS_BASE_DIR/MEMORY.md"
    
    # Gather all yearly memories
    COMBINED_CONTENT=""
    FOUND_YEARLY=false
    
    while IFS= read -r file; do
        CONTENT=$(cat "$file")
        COMBINED_CONTENT+=$'\n\n=== MEMORY: '
        COMBINED_CONTENT+=$(basename "$file")
        COMBINED_CONTENT+=$' ===\n'
        COMBINED_CONTENT+="$CONTENT"
        FOUND_YEARLY=true
    done < <(find "$LOGS_BASE_DIR" -name "MEMORY.????.md" | grep -E "MEMORY\.[0-9]{4}\.md")

    if [ "$FOUND_YEARLY" = false ]; then
        echo "Warning: No yearly memories found. Trying monthly..."
        while IFS= read -r file; do
            CONTENT=$(cat "$file")
            COMBINED_CONTENT+=$'\n\n=== MEMORY: '
            COMBINED_CONTENT+=$(basename "$file")
            COMBINED_CONTENT+=$' ===\n'
            COMBINED_CONTENT+="$CONTENT"
        done < <(find "$LOGS_BASE_DIR" -name "MEMORY.??????.md" | grep -E "MEMORY\.[0-9]{6}\.md")
    fi

    if [ -z "$COMBINED_CONTENT" ]; then
        echo "Warning: No memories found to summarize."
        exit 0
    fi

    PROMPT="
You are an AI assistant helping to generate a GLOBAL memory summary for a developer coworker.
Based on the following past memories, generate the content for the GLOBAL memory file and save it to the ABSOLUTE path: $TARGET_FILE

SPECIFICATION:
# MEMORY.md

## Mission & Vision
- Long-term goals

## Core Principles
- Guiding philosophies derived from experience

## Evolution Phases
- History of project phases

## Major Turning Points
- Key decisions or events

## Long-Term Structural Challenges
- Deep-rooted issues

## Opportunity Landscape
- Potential areas for growth

## Three Strategic Priorities Now
- Current focus

CONSTRAINTS:
- Use English only.
- Synthesize, don't just list.
- Use the \`create\` tool to write the file directly using the ABSOLUTE path: $TARGET_FILE
- Overwrite if exists.

PAST MEMORIES:
$COMBINED_CONTENT
"
    invoke_gh_copilot "$PROMPT"

elif [ "$TYPE" == "init" ]; then
    # 1. Define paths
    MEMORY_DIR="$LOGS_BASE_DIR"
    MEMORY_YEAR_DIR="$MEMORY_DIR/$YEAR"
    MEMORY_MONTH_DIR="$MEMORY_YEAR_DIR/$MONTH"
    MEMORY_DAY_DIR="$MEMORY_MONTH_DIR/$DAY"

    mkdir -p "$MEMORY_YEAR_DIR"
    mkdir -p "$MEMORY_MONTH_DIR"
    mkdir -p "$MEMORY_DAY_DIR"

    MEMORY_YEAR_PATH="$MEMORY_YEAR_DIR/MEMORY.$YEAR.md"
    MEMORY_MONTH_PATH="$MEMORY_MONTH_DIR/MEMORY.$YEAR$MONTH.md"
    MEMORY_DAY_PATH="$MEMORY_DAY_DIR/MEMORY.$YEAR$MONTH$DAY.md"
    MEMORY_DAY_LONG_PATH="$MEMORY_DAY_DIR/MEMORY.$YEAR$MONTH$DAY.long.md"

    # 3. Check Daily Memory Size and Compress if needed
    if [ -f "$MEMORY_DAY_PATH" ]; then
        # Check size in bytes
        if [[ "$OSTYPE" == "darwin"* ]]; then
             SIZE=$(stat -f%z "$MEMORY_DAY_PATH")
        else
             SIZE=$(stat -c%s "$MEMORY_DAY_PATH")
        fi
        
        if [ "$SIZE" -gt 3000 ]; then
             >&2 echo "Daily memory exceeds 3000 chars ($SIZE). Initiating compression..."
             
             cp "$MEMORY_DAY_PATH" "$MEMORY_DAY_LONG_PATH"
             >&2 echo "Original memory backed up to: $MEMORY_DAY_LONG_PATH"
             
             DAILY_CONTENT=$(cat "$MEMORY_DAY_PATH")
             COMPRESS_PROMPT="Compress the following daily memory content to under 3000 characters. Preserve key insights and structural learnings. Content:\n$DAILY_CONTENT"
             
             # Capture output
             COMPRESSED_CONTENT=$(invoke_gh_copilot "$COMPRESS_PROMPT" "true")
             
             if [ -n "$COMPRESSED_CONTENT" ]; then
                 echo "$COMPRESSED_CONTENT" > "$MEMORY_DAY_PATH"
                 NEW_SIZE=${#COMPRESSED_CONTENT}
                 >&2 echo "Daily memory compressed to $NEW_SIZE chars."
             fi
        fi
    fi

    # 4. Construct Context String
    MEMORY_CONTEXT=""
    if [ -f "$MEMORY_MONTH_PATH" ]; then
        MONTH_CONTENT=$(cat "$MEMORY_MONTH_PATH")
        MEMORY_CONTEXT+=$'\n[Monthly Memory ('$YEAR'-'$MONTH')]:\n'$MONTH_CONTENT$'\n'
    fi
    
    if [ -f "$MEMORY_DAY_PATH" ]; then
        DAY_CONTENT=$(cat "$MEMORY_DAY_PATH")
        MEMORY_CONTEXT+=$'\n[Daily Memory ('$YEAR'-'$MONTH'-'$DAY')]:\n'$DAY_CONTENT$'\n'
    fi

    # 5. Construct Instructions String
    MEMORY_INSTRUCTIONS="
*** MEMORY UPDATE INSTRUCTIONS ***
You have a memory system to help you learn and improve.
Your memory files are located in: $LOGS_BASE_DIR

After completing the task, you MUST update your daily memory file: $MEMORY_DAY_PATH
1. Append a summary of this task, its outcome, and any lessons learned to $MEMORY_DAY_PATH.
2. Check if the Monthly Memory file ($MEMORY_MONTH_PATH) has been updated with the previous day's summary. If not, summarize all daily memories from this month (excluding today) into the Monthly Memory.
3. Ensure you do not overwrite existing content, always append.
"

    # 6. Output JSON using python for safe escaping
    export CTX="$MEMORY_CONTEXT"
    export INST="$MEMORY_INSTRUCTIONS"
    python3 -c "import json, os; print(json.dumps({'context': os.environ.get('CTX', ''), 'instructions': os.environ.get('INST', '')}))"

else
    echo "Unknown type: $TYPE. Use daily, monthly, yearly, or all."
    exit 1
fi
