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
cd "$REPO_ROOT"

YEAR=$(date -d "$DATE" +%Y)
MONTH=$(date -d "$DATE" +%m)
DAY=$(date -d "$DATE" +%d)

LOGS_BASE_DIR="coworker/tasks/300logs"

invoke_gh_copilot() {
    local PROMPT="$1"
    
    # Truncate if too long (approx check)
    if [ ${#PROMPT} -gt 25000 ]; then
        echo "Warning: Prompt is too long (${#PROMPT} chars). Truncating..."
        PROMPT="${PROMPT:0:25000} ... [Truncated]"
    fi

    echo "Calling gh copilot..."
    gh copilot -- -p "$PROMPT" --allow-all-tools
}

if [ "$TYPE" == "daily" ]; then
    DAILY_SCRIPT="coworker/scripts/coworker-daily-memory-generator.sh"
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
Based on the following DAILY memories, generate the content for the MONTHLY memory file and save it to: $TARGET_FILE

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
- Use the \`create\` tool to write the file directly.
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
Based on the following MONTHLY memories, generate the content for the YEARLY memory file and save it to: $TARGET_FILE

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
- Use the \`create\` tool to write the file directly.
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
Based on the following past memories, generate the content for the GLOBAL memory file and save it to: $TARGET_FILE

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
- Use the \`create\` tool to write the file directly.
- Overwrite if exists.

PAST MEMORIES:
$COMBINED_CONTENT
"
    invoke_gh_copilot "$PROMPT"

else
    echo "Unknown type: $TYPE. Use daily, monthly, yearly, or all."
    exit 1
fi
