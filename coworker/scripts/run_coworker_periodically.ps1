#!/usr/bin/env pwsh

param(
    [switch]$Monitor
)

$repoRoot = (git rev-parse --show-toplevel 2>$null)
if (-not $repoRoot) {
    Write-Host "Repo root not found. Exiting."
    exit 1
}
Set-Location $repoRoot

$ScriptPath = Resolve-Path ".\coworker\scripts\coworker.ps1"
$ScriptName = "coworker.ps1"
$MonitorScriptPath = Resolve-Path ".\coworker\scripts\task-source-monitor.ps1"

Write-Host "Monitoring $ScriptName..."
Write-Host "Script path: $ScriptPath"
if ($Monitor) {
    Write-Host "Task source monitoring enabled using: $MonitorScriptPath"
}

while ($true) {
    $createdTasks = Get-ChildItem -Path ".\coworker\tasks\1created" -File -ErrorAction SilentlyContinue
    $approvedTasks = Get-ChildItem -Path ".\coworker\tasks\5approved" -File -Recurse -ErrorAction SilentlyContinue

    if (-not ($createdTasks -or $approvedTasks)) {
        $timestamp = (Get-Date).ToUniversalTime().ToString("yyyy-MM-dd HH:mm:ss")
        Write-Host "$timestamp - No tasks found in 1created or 5approved. Skipping check."
        Start-Sleep -Seconds 15
        continue
    }

    $Running = Get-CimInstance Win32_Process | Where-Object {
        $_.Name -match 'pwsh|powershell' -and
        $_.CommandLine -match [regex]::Escape($ScriptName) -and
        $_.CommandLine -notmatch [regex]::Escape("run_coworker_periodically.ps1") -and
        $_.ProcessId -ne $PID
    }

    $timestamp = (Get-Date).ToUniversalTime().ToString("yyyy-MM-dd HH:mm:ss")

    if ($Monitor) {
        Write-Host "$timestamp - Running task source monitor..."
        try {
            & $MonitorScriptPath -Once
        } catch {
            Write-Error "Failed to run task source monitor: $_"
        }
    }

    if ($Running) {
        Write-Host "$timestamp - $ScriptName is already running."
    } else {
        Write-Host "$timestamp - $ScriptName is NOT running. Starting it..."
        try {
            # Start the process using Start-Process to get the process object
            $p = Start-Process -FilePath "pwsh" -ArgumentList "-File", $ScriptPath -PassThru
            Write-Host "Started $ScriptName with PID: $($p.Id)"

            # Monitor loop
            $loopCheckCounter = 0
            $consecutiveLowActivity = 0
            $maxConsecutiveLowActivity = 18 # 3 minutes / 10 seconds

            while (-not $p.HasExited) {
                Start-Sleep -Seconds 10

                # Find the latest copilot log file
                $currentYear = (Get-Date).ToUniversalTime().ToString("yyyy")
                $currentMonth = (Get-Date).ToUniversalTime().ToString("MM")
                $currentDay = (Get-Date).ToUniversalTime().ToString("dd")
                $logsSubDir = Join-Path ".\coworker\tasks\300logs" "$currentYear\$currentMonth\$currentDay"

                if (Test-Path $logsSubDir) {
                    $latestLog = Get-ChildItem -Path $logsSubDir -Filter "*.copilot.log.stdout" | Sort-Object LastWriteTime -Descending | Select-Object -First 1

                    if ($latestLog) {
                        try {
                            $lines = Get-Content -Path $latestLog.FullName -Tail 500 -ErrorAction SilentlyContinue
                            if ($lines) {
                                # PowerShell Get-Content returns an array of strings, but if only one line, it returns a string.
                                # Ensure it is an array.
                                $linesArray = @($lines)
                                $lineCount = $linesArray.Count
                                $actionCount = 0
                                foreach ($line in $linesArray) {
                                    if ($line -match "● ") {
                                        $actionCount++
                                    }
                                }

                                $ratio = 0
                                if ($lineCount -gt 0) {
                                    $ratio = $actionCount / $lineCount
                                }

                                # Only check ratio if we have enough lines to be statistically significant
                                # e.g. > 10 lines
                                if ($lineCount -gt 10) {
                                    if ($ratio -lt 0.05) {
                                        $consecutiveLowActivity++
                                        Write-Host "Warning: Low activity detected. Ratio: $ratio ($actionCount/$lineCount). Consecutive checks: $consecutiveLowActivity/$maxConsecutiveLowActivity" -ForegroundColor Yellow
                                    } else {
                                        $consecutiveLowActivity = 0
                                    }
                                } else {
                                     # Not enough lines yet, reset counter or ignore?
                                     # Better to ignore and wait for more logs.
                                     # But if it hangs with 5 lines forever?
                                     # The loop detection is for "outputting logs but no action".
                                     # So if it's outputting logs, lineCount will increase.
                                }

                                if ($consecutiveLowActivity -ge $maxConsecutiveLowActivity) {
                                    Write-Host "Error: Coworker loop detected! Killing process $($p.Id)..." -ForegroundColor Red
                                    Stop-Process -Id $p.Id -Force

                                    # Extract task base name from log filename
                                    # Format: HHmmss-TaskName.copilot.log.stdout
                                    $logName = $latestLog.Name
                                    $taskBaseName = $null

                                    if ($logName -match "^\d{6}-(.*)\.copilot\.log\.stdout$") {
                                        $taskBaseName = $Matches[1]
                                    }

                                    if ($taskBaseName) {
                                        Write-Host "Aborting task: $taskBaseName"

                                        $workingDir = ".\coworker\tasks\2working"
                                        $abortedDir = ".\coworker\tasks\3_5aborted"

                                        if (-not (Test-Path $abortedDir)) {
                                            New-Item -ItemType Directory -Path $abortedDir | Out-Null
                                        }

                                        # Use wildcard to match potential extensions or variations
                                        $taskFiles = Get-ChildItem -Path $workingDir -Filter "$taskBaseName*"
                                        foreach ($file in $taskFiles) {
                                            $destPath = Join-Path $abortedDir $file.Name
                                            Move-Item -Path $file.FullName -Destination $destPath -Force
                                            Write-Host "Moved task file to: $destPath" -ForegroundColor Red
                                        }
                                    } else {
                                        Write-Host "Could not determine task name from log file: $logName"
                                    }
                                    break
                                }
                            }
                        } catch {
                            Write-Host "Error reading/processing log file: $_"
                        }
                    }
                }
            }

            Write-Host "Finished $ScriptName."
        } catch {
            Write-Error "Failed to start ${ScriptName}: $_"
        }
    }

    Start-Sleep -Seconds 15
}
