#!/usr/bin/env pwsh

<#
.SYNOPSIS
    Coworker Daily Memory Generator
.DESCRIPTION
    Analyzes daily logs and generates a memory summary using gh copilot.
.PARAMETER Date
    The date to generate memory for (format: YYYY-MM-DD). Defaults to today.
#>
param(
    [string]$Date = ((Get-Date).ToUniversalTime().ToString("yyyy-MM-dd"))
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
$month = $parsedDate.ToString("mm") # Wait, mm is minutes? No, "MM"
$month = $parsedDate.ToString("MM")
$day = $parsedDate.ToString("dd")
$dateStr = $parsedDate.ToString("yyyy-MM-dd")
$compactDate = $parsedDate.ToString("yyyyMMdd")

$logDir = "coworker\tasks\300logs\$year\$month\$day"
$memoryFile = "$logDir\MEMORY.$compactDate.md"

if (-not (Test-Path $logDir)) {
    Write-Error "Log directory $logDir does not exist."
}

Write-Host "Generating daily memory for $dateStr from logs in $logDir..."

# Collect logs
$logContent = ""

# Function to extract clean prompt from task log
function Get-CleanPrompt {
    param($TaskLogPath)
    $content = Get-Content $TaskLogPath -Raw
    if ($content -match "(?s)Prompt:(.*?)\*\*\* MEMORY UPDATE INSTRUCTIONS \*\*\*") {
        return $Matches[1].Trim()
    } elseif ($content -match "(?s)Prompt:(.*)") {
        return $Matches[1].Trim()
    }
    return ""
}

Get-ChildItem -Path $logDir -Filter "*.task.log" | ForEach-Object {
    $taskLog = $_
    $baseName = $taskLog.Name -replace ".task.log$", ""
    $copilotLogPath = Join-Path $logDir "$baseName.copilot.log"

    $logContent += "`n`n=== TASK: $baseName ===`n"

    # Extract metadata
    $lines = Get-Content $taskLog.FullName -TotalCount 10
    $titleLine = $lines | Where-Object { $_ -match "^Task:" } | Select-Object -First 1
    if ($titleLine) { $logContent += "$titleLine`n" }

    $logContent += "--- PROMPT (Snippet) ---`n"
    $cleanPrompt = Get-CleanPrompt -TaskLogPath $taskLog.FullName
    # Truncate prompt to 2000 chars
    if ($cleanPrompt.Length -gt 2000) {
        $cleanPrompt = $cleanPrompt.Substring(0, 2000) + "... [Truncated]"
    }
    $logContent += "$cleanPrompt`n"

    $logContent += "--- RESULT (Snippet) ---`n"
    if (Test-Path $copilotLogPath) {
        $copilotContent = @(Get-Content $copilotLogPath)

        $lastToolIndex = -1
        for ($i = $copilotContent.Count - 1; $i -ge 0; $i--) {
            if ($copilotContent[$i] -match "^● (Read|Edit|Run)") {
                $lastToolIndex = $i
                break
            }
        }

        $head = $copilotContent | Select-Object -First 10
        $tailContent = ""

        if ($lastToolIndex -ge 0) {
             # Take from the last tool execution to the end
             $tailLines = $copilotContent | Select-Object -Skip $lastToolIndex
             $tailContent = $tailLines -join "`n"
        } else {
             # Fallback: take last 100 lines if no tool found
             $tailLines = $copilotContent | Select-Object -Last 100
             $tailContent = $tailLines -join "`n"
        }

        $copilotOutput = ($head -join "`n") + "`n... [Intermediate logs skipped] ...`n" + $tailContent

        # Truncate output to 20000 chars to avoid token limit if lines are very long
        if ($copilotOutput.Length -gt 20000) {
            $copilotOutput = $copilotOutput.Substring(0, 20000) + "... [Truncated]"
        }
        $logContent += "$copilotOutput`n"
    } else {
        $logContent += "[Copilot log not found]`n"
    }
}

if ([string]::IsNullOrWhiteSpace($logContent)) {
    Write-Host "No logs found for $dateStr."
    exit 0
}

# Construct Prompt
$prompt = @"
You are an AI assistant helping to generate a daily memory summary for a developer coworker.
Based on the following development logs, generate the content for the daily memory file and save it to: $memoryFile

SPECIFICATION:
# MEMORY.$compactDate.md
## Daily Memory - $dateStr

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
- Use the `create` tool to write the file directly. If the file exists, overwrite it (I have already backed it up).

LOGS:
$logContent
"@

# Truncate if too long (approx check, limit depends on OS/shell but 20k is safeish)
if ($prompt.Length -gt 20000) {
    Write-Warning "Logs are too long ($($prompt.Length) chars). Truncating..."
    $prompt = $prompt.Substring(0, 20000) + " ... [Truncated]"
}

# Check if memory file exists
if (Test-Path $memoryFile) {
    Write-Warning "Memory file $memoryFile already exists."
    # Backup existing
    $backupPath = "$memoryFile.bak"
    Move-Item -Path $memoryFile -Destination $backupPath -Force
    Write-Host "Backed up existing memory file to $backupPath"
}

Write-Host "Calling gh copilot..."

# Use Start-Process to handle arguments safely, similar to coworker.ps1
$safePrompt = $prompt.Replace('"', '\"')
$copilotArgList = @(
    'copilot',
    '--',
    '-p',
    "`"$safePrompt`"",
    '--allow-all-tools'
)

# Use direct invocation with -- to separate flags
# & gh copilot -- -p $prompt --allow-all-tools
Start-Process -FilePath 'gh' -ArgumentList $copilotArgList -NoNewWindow -Wait

Write-Host "Memory generation task completed."
