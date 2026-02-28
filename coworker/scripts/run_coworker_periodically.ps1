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
        $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
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

    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"

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
            # Start the process synchronously to avoid spawning multiple checks
            & $ScriptPath
            Write-Host "Finished $ScriptName."
        } catch {
            Write-Error "Failed to start ${ScriptName}: $_"
        }
    }

    Start-Sleep -Seconds 15
}
