#!/usr/bin/env pwsh

param(
    [string]$ConfigPath,
    [switch]$Once
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Get-RepoRoot {
    $currentDirectory = $PSScriptRoot
    while ($currentDirectory) {
        if ((Test-Path (Join-Path $currentDirectory 'ROOT.md')) -or (Test-Path (Join-Path $currentDirectory '.git'))) {
            return (Resolve-Path $currentDirectory).Path
        }

        $parentDirectory = Split-Path -Parent $currentDirectory
        if ($parentDirectory -eq $currentDirectory) {
            break
        }
        $currentDirectory = $parentDirectory
    }

    throw 'Repo root not found.'
}

function Resolve-SchedulerPath {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,
        [Parameter(Mandatory = $true)]
        [string]$RepoRoot,
        [Parameter(Mandatory = $true)]
        [string]$ConfigDirectory
    )

    if ([System.IO.Path]::IsPathRooted($Path)) {
        return [System.IO.Path]::GetFullPath($Path)
    }

    $configRelativePath = Join-Path $ConfigDirectory $Path
    if (Test-Path $configRelativePath) {
        return (Resolve-Path $configRelativePath).Path
    }

    return [System.IO.Path]::GetFullPath((Join-Path $RepoRoot $Path))
}

function Get-ConfigValue {
    param(
        [Parameter(Mandatory = $true)]
        [object]$Map,
        [Parameter(Mandatory = $true)]
        [string]$Key,
        $DefaultValue = $null
    )

    if ($Map -is [System.Collections.IDictionary] -and $Map.Contains($Key)) {
        return $Map[$Key]
    }

    return $DefaultValue
}

function Ensure-Directory {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    if (-not (Test-Path $Path)) {
        New-Item -ItemType Directory -Path $Path -Force | Out-Null
    }
}

function Test-PathHasPendingFiles {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    if (-not (Test-Path -LiteralPath $Path)) {
        return $false
    }

    $item = Get-Item -LiteralPath $Path -ErrorAction SilentlyContinue
    if ($null -eq $item) {
        return $false
    }

    if (-not $item.PSIsContainer) {
        return $true
    }

    $pendingFile = Get-ChildItem -LiteralPath $item.FullName -File -Recurse -ErrorAction SilentlyContinue |
        Select-Object -First 1
    return $null -ne $pendingFile
}

function Get-TaskSnapshot {
    param(
        [Parameter(Mandatory = $true)]
        [hashtable]$TaskState
    )

    return [pscustomobject]@{
        Name                = $TaskState.Name
        Description         = $TaskState.Description
        Enabled             = $TaskState.Enabled
        IntervalSeconds     = $TaskState.IntervalSeconds
        DependsOn           = @($TaskState.DependsOn)
        PendingPaths        = @($TaskState.PendingPaths)
        ScriptPath          = $TaskState.ScriptPath
        Arguments           = @($TaskState.Arguments)
        Status              = $TaskState.Status
        LastStartedUtc      = $TaskState.LastStartedUtc
        LastFinishedUtc     = $TaskState.LastFinishedUtc
        LastExitCode        = $TaskState.LastExitCode
        LastDurationSeconds = $TaskState.LastDurationSeconds
        CurrentPid          = $TaskState.CurrentPid
        NextRunUtc          = $TaskState.NextRunUtc
        StdOutLogPath       = $TaskState.StdOutLogPath
        StdErrLogPath       = $TaskState.StdErrLogPath
        RunCount            = $TaskState.RunCount
    }
}

function Write-SchedulerStatus {
    param(
        [Parameter(Mandatory = $true)]
        [string]$StatusFile,
        [Parameter(Mandatory = $true)]
        [string]$ConfigPath,
        [Parameter(Mandatory = $true)]
        [int]$TickSeconds,
        [Parameter(Mandatory = $true)]
        [hashtable]$TaskStates
    )

    $statusDocument = [pscustomobject]@{
        GeneratedAtUtc = (Get-Date).ToUniversalTime().ToString('o')
        ConfigPath     = $ConfigPath
        TickSeconds    = $TickSeconds
        Tasks          = @($TaskStates.Values | Sort-Object Name | ForEach-Object { Get-TaskSnapshot -TaskState $_ })
    }

    $statusDocument | ConvertTo-Json -Depth 8 | Set-Content -Path $StatusFile -Encoding UTF8
}

function Start-ScheduledTaskRun {
    param(
        [Parameter(Mandatory = $true)]
        [hashtable]$TaskState,
        [Parameter(Mandatory = $true)]
        [string]$PowerShellExecutable,
        [Parameter(Mandatory = $true)]
        [string]$RepoRoot,
        [Parameter(Mandatory = $true)]
        [string]$LogDirectory
    )

    $startTime = (Get-Date).ToUniversalTime()
    $dateFolder = Join-Path $LogDirectory $startTime.ToString('yyyy\\MM\\dd')
    Ensure-Directory -Path $dateFolder

    $timestamp = $startTime.ToString('HHmmss')
    $stdOutPath = Join-Path $dateFolder "$timestamp-$($TaskState.Name).stdout.log"
    $stdErrPath = Join-Path $dateFolder "$timestamp-$($TaskState.Name).stderr.log"
    $argumentList = @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', $TaskState.ScriptPath) + @($TaskState.Arguments)

    $process = Start-Process -FilePath $PowerShellExecutable `
        -ArgumentList $argumentList `
        -WorkingDirectory $RepoRoot `
        -RedirectStandardOutput $stdOutPath `
        -RedirectStandardError $stdErrPath `
        -PassThru

    $TaskState.Process = $process
    $TaskState.Status = 'Running'
    $TaskState.CurrentPid = $process.Id
    $TaskState.LastStartedUtc = $startTime.ToString('o')
    $TaskState.NextRunUtc = $startTime.AddSeconds($TaskState.IntervalSeconds).ToString('o')
    $TaskState.StdOutLogPath = $stdOutPath
    $TaskState.StdErrLogPath = $stdErrPath
    $TaskState.RunCount = $TaskState.RunCount + 1

    Write-Host ("[{0}] Started {1} (PID {2})" -f $startTime.ToString('o'), $TaskState.Name, $process.Id)
}

function Update-ScheduledTaskRun {
    param(
        [Parameter(Mandatory = $true)]
        [hashtable]$TaskState
    )

    if ($null -eq $TaskState.Process) {
        return
    }

    $TaskState.Process.Refresh()
    if (-not $TaskState.Process.HasExited) {
        return
    }

    $finishedAt = (Get-Date).ToUniversalTime()
    $startedAt = [DateTimeOffset]::Parse($TaskState.LastStartedUtc)
    $TaskState.LastFinishedUtc = $finishedAt.ToString('o')
    $TaskState.LastExitCode = $TaskState.Process.ExitCode
    $TaskState.LastDurationSeconds = [Math]::Round(($finishedAt - $startedAt.UtcDateTime).TotalSeconds, 2)
    $TaskState.Status = if ($TaskState.Process.ExitCode -eq 0) { 'Idle' } else { 'Failed' }
    $TaskState.CurrentPid = $null
    $TaskState.Process = $null

    Write-Host ("[{0}] Finished {1} with exit code {2}" -f $finishedAt.ToString('o'), $TaskState.Name, $TaskState.LastExitCode)
}

function Test-ScheduledTaskHasPendingInputs {
    param(
        [Parameter(Mandatory = $true)]
        [hashtable]$TaskState
    )

    $pendingPaths = @($TaskState.PendingPaths)
    if ($pendingPaths.Count -eq 0) {
        return $true
    }

    foreach ($pendingPath in $pendingPaths) {
        if (Test-PathHasPendingFiles -Path $pendingPath) {
            return $true
        }
    }

    return $false
}

function Set-ScheduledTaskWaitingForWork {
    param(
        [Parameter(Mandatory = $true)]
        [hashtable]$TaskState,
        [Parameter(Mandatory = $true)]
        [datetime]$Now
    )

    $TaskState.Status = 'WaitingForWork'
    $TaskState.NextRunUtc = $Now.AddSeconds($TaskState.IntervalSeconds).ToString('o')
}

function Test-ScheduledTaskCanStart {
    param(
        [Parameter(Mandatory = $true)]
        [hashtable]$TaskState,
        [Parameter(Mandatory = $true)]
        [hashtable]$TaskStates,
        [Parameter(Mandatory = $true)]
        [datetime]$Now,
        [switch]$OnceMode
    )

    if (-not $TaskState.Enabled -or $null -ne $TaskState.Process) {
        return $false
    }

    if ($OnceMode -and $TaskState.RunCount -gt 0) {
        return $false
    }

    $nextRunUtc = $TaskState.NextRunUtc
    if (-not [string]::IsNullOrWhiteSpace($nextRunUtc)) {
        $nextRunAt = [DateTimeOffset]::Parse($nextRunUtc)
        if ($Now -lt $nextRunAt.UtcDateTime) {
            return $false
        }
    }

    foreach ($dependencyName in @($TaskState.DependsOn)) {
        if (-not $TaskStates.ContainsKey($dependencyName)) {
            throw "Scheduled task '$($TaskState.Name)' depends on unknown task '$dependencyName'."
        }

        $dependencyState = $TaskStates[$dependencyName]
        if ($dependencyState.Enabled -and $null -ne $dependencyState.Process) {
            return $false
        }

        $dependencyNextRunUtc = $dependencyState.NextRunUtc
        if (-not [string]::IsNullOrWhiteSpace($dependencyNextRunUtc)) {
            $dependencyNextRunAt = [DateTimeOffset]::Parse($dependencyNextRunUtc)
            if ($dependencyState.Enabled -and $Now -ge $dependencyNextRunAt.UtcDateTime) {
                return $false
            }
        }

        if ($OnceMode -and $dependencyState.Enabled -and $dependencyState.RunCount -eq 0) {
            return $false
        }
    }

    return $true
}

$repoRoot = Get-RepoRoot
if ([string]::IsNullOrWhiteSpace($ConfigPath)) {
    $ConfigPath = Join-Path $PSScriptRoot 'coworker-scheduler.config.psd1'
}

$resolvedConfigPath = Resolve-SchedulerPath -Path $ConfigPath -RepoRoot $repoRoot -ConfigDirectory $PSScriptRoot
if (-not (Test-Path $resolvedConfigPath)) {
    throw "Scheduler config not found: $resolvedConfigPath"
}

$config = Import-PowerShellDataFile -Path $resolvedConfigPath
if (-not $config.Tasks) {
    throw "Scheduler config must define a Tasks array: $resolvedConfigPath"
}

$schedulerConfig = Get-ConfigValue -Map $config -Key 'Scheduler' -DefaultValue @{}
$tickSeconds = [int](Get-ConfigValue -Map $schedulerConfig -Key 'TickSeconds' -DefaultValue 5)
$powerShellExecutable = [string](Get-ConfigValue -Map $schedulerConfig -Key 'PowerShellExecutable' -DefaultValue 'pwsh')
$logDirectory = Resolve-SchedulerPath -Path ([string](Get-ConfigValue -Map $schedulerConfig -Key 'LogDirectory' -DefaultValue 'coworker\tasks\300logs\scheduler')) -RepoRoot $repoRoot -ConfigDirectory $PSScriptRoot
$statusFile = Resolve-SchedulerPath -Path ([string](Get-ConfigValue -Map $schedulerConfig -Key 'StatusFile' -DefaultValue 'coworker\tasks\300logs\scheduler\scheduled-tasks.status.json')) -RepoRoot $repoRoot -ConfigDirectory $PSScriptRoot

Ensure-Directory -Path $logDirectory
Ensure-Directory -Path (Split-Path -Parent $statusFile)

$taskStates = @{}
foreach ($task in $config.Tasks) {
    $taskName = [string](Get-ConfigValue -Map $task -Key 'Name' -DefaultValue '')
    if ([string]::IsNullOrWhiteSpace($taskName)) {
        throw 'Each scheduled task must define Name.'
    }
    $intervalSeconds = [int](Get-ConfigValue -Map $task -Key 'IntervalSeconds' -DefaultValue 0)
    if ($intervalSeconds -le 0) {
        throw "Scheduled task '$taskName' must define IntervalSeconds."
    }
    $scriptPath = [string](Get-ConfigValue -Map $task -Key 'ScriptPath' -DefaultValue '')
    if ([string]::IsNullOrWhiteSpace($scriptPath)) {
        throw "Scheduled task '$taskName' must define ScriptPath."
    }

    $resolvedScriptPath = Resolve-SchedulerPath -Path $scriptPath -RepoRoot $repoRoot -ConfigDirectory $PSScriptRoot
    if (-not (Test-Path $resolvedScriptPath)) {
        throw "Scheduled task '$taskName' script not found: $resolvedScriptPath"
    }

    $enabled = [bool](Get-ConfigValue -Map $task -Key 'Enabled' -DefaultValue $true)
    $dependsOn = @()
    $rawDependsOn = Get-ConfigValue -Map $task -Key 'DependsOn' -DefaultValue @()
    if ($null -ne $rawDependsOn) {
        $dependsOn = @($rawDependsOn | ForEach-Object { [string]$_ } | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
    }

    $pendingPaths = @()
    $rawPendingPaths = Get-ConfigValue -Map $task -Key 'PendingPaths' -DefaultValue @()
    if ($null -ne $rawPendingPaths) {
        $pendingPaths = @(
            $rawPendingPaths |
                ForEach-Object { [string]$_ } |
                Where-Object { -not [string]::IsNullOrWhiteSpace($_) } |
                ForEach-Object { Resolve-SchedulerPath -Path $_ -RepoRoot $repoRoot -ConfigDirectory $PSScriptRoot }
        )
    }

    $taskStates[$taskName] = @{
        Name                = $taskName
        Description         = [string](Get-ConfigValue -Map $task -Key 'Description' -DefaultValue '')
        Enabled             = $enabled
        IntervalSeconds     = $intervalSeconds
        DependsOn           = $dependsOn
        PendingPaths        = $pendingPaths
        ScriptPath          = $resolvedScriptPath
        Arguments           = @((Get-ConfigValue -Map $task -Key 'Arguments' -DefaultValue @()))
        Status              = if ($enabled) { 'Idle' } else { 'Disabled' }
        LastStartedUtc      = $null
        LastFinishedUtc     = $null
        LastExitCode        = $null
        LastDurationSeconds = $null
        CurrentPid          = $null
        NextRunUtc          = (Get-Date).ToUniversalTime().ToString('o')
        StdOutLogPath       = $null
        StdErrLogPath       = $null
        RunCount            = 0
        Process             = $null
    }
}

Write-Host "Loaded scheduler config: $resolvedConfigPath"
Write-Host "Task status file: $statusFile"

if ($Once) {
    do {
        $now = (Get-Date).ToUniversalTime()
        $runningCount = 0
        foreach ($taskState in $taskStates.Values) {
            Update-ScheduledTaskRun -TaskState $taskState
            if ($null -ne $taskState.Process) {
                $runningCount++
            }
        }

        foreach ($taskState in $taskStates.Values | Sort-Object Name) {
            if (Test-ScheduledTaskCanStart -TaskState $taskState -TaskStates $taskStates -Now $now -OnceMode) {
                if (Test-ScheduledTaskHasPendingInputs -TaskState $taskState) {
                    Start-ScheduledTaskRun -TaskState $taskState -PowerShellExecutable $powerShellExecutable -RepoRoot $repoRoot -LogDirectory $logDirectory
                    $runningCount++
                }
                else {
                    Set-ScheduledTaskWaitingForWork -TaskState $taskState -Now $now
                }
            }
        }

        Write-SchedulerStatus -StatusFile $statusFile -ConfigPath $resolvedConfigPath -TickSeconds $tickSeconds -TaskStates $taskStates
        if ($runningCount -gt 0) {
            Start-Sleep -Seconds 1
        }
    } while ($runningCount -gt 0)

    $failed = $taskStates.Values | Where-Object { $_.Enabled -and $_.LastExitCode -ne 0 }
    exit $(if ($failed) { 1 } else { 0 })
}

while ($true) {
    $now = (Get-Date).ToUniversalTime()

    foreach ($taskState in $taskStates.Values) {
        if (-not $taskState.Enabled) {
            $taskState.Status = 'Disabled'
            continue
        }

        Update-ScheduledTaskRun -TaskState $taskState

        if ($null -ne $taskState.Process) {
            continue
        }

        if (Test-ScheduledTaskCanStart -TaskState $taskState -TaskStates $taskStates -Now $now) {
            if (Test-ScheduledTaskHasPendingInputs -TaskState $taskState) {
                Start-ScheduledTaskRun -TaskState $taskState -PowerShellExecutable $powerShellExecutable -RepoRoot $repoRoot -LogDirectory $logDirectory
            }
            else {
                Set-ScheduledTaskWaitingForWork -TaskState $taskState -Now $now
            }
        }
    }

    Write-SchedulerStatus -StatusFile $statusFile -ConfigPath $resolvedConfigPath -TickSeconds $tickSeconds -TaskStates $taskStates
    Start-Sleep -Seconds $tickSeconds
}
