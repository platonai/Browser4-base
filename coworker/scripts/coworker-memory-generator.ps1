#!/usr/bin/env pwsh

<#
.SYNOPSIS
    Coworker Memory Generator
.DESCRIPTION
    Generates memory summaries (daily, monthly, yearly, all) based on logs or previous summaries.
.PARAMETER Type
    The type of memory to generate: "daily", "monthly", "yearly", "all". Defaults to "daily".
.PARAMETER Date
    The date to generate memory for (format: YYYY-MM-DD). Defaults to today.
.PARAMETER Force
    Force generation even if file exists (overwrites).
#>
param(
    [ValidateSet("daily", "monthly", "yearly", "all")]
    [string]$Type = "daily",
    
    [string]$Date = ((Get-Date).ToUniversalTime().ToString("yyyy-MM-dd")),

    [switch]$Force
)

$ErrorActionPreference = "Stop"

# 🔍 Find repo root
$repoRoot = git rev-parse --show-toplevel 2>$null
if (-not $repoRoot) {
    Write-Host "Repo root not found. Exiting."
    exit 1
}
Set-Location $repoRoot

$parsedDate = Get-Date $Date
$year = $parsedDate.ToString("yyyy")
$month = $parsedDate.ToString("MM")
$day = $parsedDate.ToString("dd")

$logsBaseDir = "coworker\tasks\300logs"

# Function to run gh copilot
function Invoke-GhCopilot {
    param($Prompt)
    
    # Truncate if too long (approx check, limit depends on OS/shell but 20k is safeish)
    if ($Prompt.Length -gt 25000) {
        Write-Warning "Prompt is too long ($($Prompt.Length) chars). Truncating..."
        $Prompt = $Prompt.Substring(0, 25000) + " ... [Truncated]"
    }

    Write-Host "Calling gh copilot..."
    
    # Escape double quotes in the prompt and wrap in quotes to ensure correct argument parsing
    $safePrompt = $Prompt.Replace('"', '\"')
    
    # Pass arguments as an array to avoid fragile manual escaping/quoting.
    $copilotArgList = @(
        'copilot'
        '--'
        '-p'
        "`"$safePrompt`""
        '--allow-all-tools'
    )

    # Use Start-Process to handle arguments safely
    Start-Process -FilePath 'gh' -ArgumentList $copilotArgList -NoNewWindow -Wait
}

if ($Type -eq "daily") {
    # Reuse existing logic or call the existing script?
    # Better to keep logic self-contained if we want this to be the main entry point.
    # For now, let's call the existing script to avoid duplication if it exists, or reimplement.
    # The existing script is specific to daily. Let's call it.
    
    $dailyScript = "coworker\scripts\coworker-daily-memory-generator.ps1"
    if (Test-Path $dailyScript) {
        & $dailyScript -Date $Date
    } else {
        Write-Error "Daily memory generator script not found at $dailyScript"
    }
}
elseif ($Type -eq "monthly") {
    $targetDir = "$logsBaseDir\$year\$month"
    $targetFile = "$targetDir\MEMORY.$year$month.md"
    
    if (-not (Test-Path $targetDir)) {
        Write-Error "Directory $targetDir does not exist. No daily memories to summarize."
        exit 1
    }

    # Gather all daily memories for the month
    $dailyMemories = Get-ChildItem -Path "$targetDir\*\MEMORY.*.md" -Recurse
    
    if ($dailyMemories.Count -eq 0) {
        Write-Warning "No daily memories found for $year-$month."
        exit 0
    }

    $combinedContent = ""
    foreach ($file in $dailyMemories) {
        $content = Get-Content $file.FullName -Raw
        $combinedContent += "`n`n=== DAILY MEMORY: $($file.Name) ===`n$content"
    }

    $prompt = @"
You are an AI assistant helping to generate a MONTHLY memory summary for a developer coworker.
Based on the following DAILY memories, generate the content for the MONTHLY memory file and save it to: $targetFile

SPECIFICATION:
# MEMORY.$year$month.md
## Monthly Memory - $year-$month

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
- Use the `create` tool to write the file directly.
- Overwrite if exists.

DAILY MEMORIES:
$combinedContent
"@
    
    Invoke-GhCopilot -Prompt $prompt
}
elseif ($Type -eq "yearly") {
    $targetDir = "$logsBaseDir\$year"
    $targetFile = "$targetDir\MEMORY.$year.md"
    
    # Gather all monthly memories for the year
    # Monthly memories are at logs/YYYY/MM/MEMORY.YYYYMM.md
    $monthlyMemories = Get-ChildItem -Path "$logsBaseDir\$year\*\MEMORY.$year*.md"
    
    if ($monthlyMemories.Count -eq 0) {
        Write-Warning "No monthly memories found for $year."
        exit 0
    }

    $combinedContent = ""
    foreach ($file in $monthlyMemories) {
        $content = Get-Content $file.FullName -Raw
        $combinedContent += "`n`n=== MONTHLY MEMORY: $($file.Name) ===`n$content"
    }

    $prompt = @"
You are an AI assistant helping to generate a YEARLY memory summary for a developer coworker.
Based on the following MONTHLY memories, generate the content for the YEARLY memory file and save it to: $targetFile

SPECIFICATION:
# MEMORY.$year.md
## Annual Strategic Review - $year

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
- Use the `create` tool to write the file directly.
- Overwrite if exists.

MONTHLY MEMORIES:
$combinedContent
"@

    Invoke-GhCopilot -Prompt $prompt
}
elseif ($Type -eq "all") {
    $targetFile = "$logsBaseDir\MEMORY.md"
    
    # Gather all yearly memories
    # Yearly memories are at logs/YYYY/MEMORY.YYYY.md
    $yearlyMemories = Get-ChildItem -Path "$logsBaseDir\*\MEMORY.*.md" | Where-Object { $_.Name -match "MEMORY\.\d{4}\.md" }
    
    if ($yearlyMemories.Count -eq 0) {
        # Fallback to monthly if no yearly? Or just warn?
        Write-Warning "No yearly memories found. Trying monthly..."
        $yearlyMemories = Get-ChildItem -Path "$logsBaseDir\*\*\MEMORY.*.md" | Where-Object { $_.Name -match "MEMORY\.\d{6}\.md" }
    }

    if ($yearlyMemories.Count -eq 0) {
        Write-Warning "No memories found to summarize."
        exit 0
    }

    $combinedContent = ""
    foreach ($file in $yearlyMemories) {
        $content = Get-Content $file.FullName -Raw
        $combinedContent += "`n`n=== MEMORY: $($file.Name) ===`n$content"
    }

    $prompt = @"
You are an AI assistant helping to generate a GLOBAL memory summary for a developer coworker.
Based on the following past memories, generate the content for the GLOBAL memory file and save it to: $targetFile

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
- Use the `create` tool to write the file directly.
- Overwrite if exists.

PAST MEMORIES:
$combinedContent
"@

    Invoke-GhCopilot -Prompt $prompt
}
