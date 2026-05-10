#!/usr/bin/env pwsh

<#
.SYNOPSIS
  清理稳定版 Git tag，仅保留每个 minor 版本中的第一个 patch 和最后一个 patch。

.DESCRIPTION
  仅处理形如 vMAJOR.MINOR.PATCH 的稳定版标签。
  例如：v1.7.0 ~ v1.7.5 只保留 v1.7.0 和 v1.7.5，删除中间标签。

  规则说明：
  1. 按 MAJOR.MINOR 分组；
  2. 每组保留最小 patch 对应的 tag 和最大 patch 对应的 tag；
  3. 如果某组只有 1 个 tag，则仅保留该 tag；
  4. 非稳定版标签（如 -rc、-ci）会被忽略。

.PARAMETER Remote
  远程仓库名称，默认 origin。

.PARAMETER DeleteRemote
  同时删除远程同名 tag。

.PARAMETER SkipFetch
  跳过执行 git fetch --tags。

.PARAMETER DryRun
  仅展示清理计划，不执行删除。

.EXAMPLE
  .\bin\git\clean-minor-tags.ps1 -DryRun

.EXAMPLE
  .\bin\git\clean-minor-tags.ps1 -DeleteRemote

.EXAMPLE
  .\bin\git\clean-minor-tags.ps1 -DeleteRemote -WhatIf
#>

[CmdletBinding(SupportsShouldProcess = $true, ConfirmImpact = 'High')]
param (
    [string]$Remote = "origin",
    [switch]$DeleteRemote,
    [switch]$SkipFetch,
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

$repoRoot = (& git rev-parse --show-toplevel 2>$null)
if ($LASTEXITCODE -ne 0 -or -not $repoRoot) {
    Write-Error "Not a git repository"
    exit 1
}

$repoRoot = ($repoRoot | Select-Object -First 1).ToString().Trim()
Set-Location $repoRoot
[Environment]::CurrentDirectory = $repoRoot

. "$repoRoot\bin\common\Util.ps1"
Fix-Encoding-UTF8

$gitExe = (Get-Command git -CommandType Application | Select-Object -First 1).Source

function Join-CommandArguments {
    param(
        [Parameter(Mandatory)]
        [string[]]$Arguments
    )

    return ($Arguments | ForEach-Object {
            if ($_ -match '[\s"]') {
                '"{0}"' -f ($_ -replace '"', '\"')
            } else {
                $_
            }
        }) -join ' '
}

if (-not (Test-Path ".git")) {
    Write-Error "Not a git repository: $repoRoot"
    exit 1
}

function Invoke-Git {
    param(
        [Parameter(Mandatory)]
        [string[]]$Arguments,
        [switch]$Quiet,
        [switch]$AllowFailure
    )

    $startInfo = New-Object System.Diagnostics.ProcessStartInfo
    $startInfo.FileName = $gitExe
    $startInfo.Arguments = Join-CommandArguments -Arguments $Arguments
    $startInfo.WorkingDirectory = $repoRoot
    $startInfo.UseShellExecute = $false
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    $startInfo.CreateNoWindow = $true

    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $startInfo

    [void]$process.Start()
    $stdoutText = $process.StandardOutput.ReadToEnd()
    $stderrText = $process.StandardError.ReadToEnd()
    $process.WaitForExit()
    $exitCode = $process.ExitCode

    $stdout = @($stdoutText -split "`r?`n" | Where-Object { $_ -ne '' })
    $stderr = @($stderrText -split "`r?`n" | Where-Object { $_ -ne '' })

    if (-not $AllowFailure -and $exitCode -ne 0) {
        $details = @($stdout + $stderr) | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
        if ($details) {
            throw "git $($Arguments -join ' ') failed with exit code ${exitCode}: $($details -join [Environment]::NewLine)"
        }

        throw "git $($Arguments -join ' ') failed with exit code $exitCode"
    }

    if (-not $Quiet) {
        return $stdout
    }
}

function Get-StableTagInfo {
    param(
        [Parameter(Mandatory)]
        [string]$Tag
    )

    if ($Tag -notmatch '^v(?<major>\d+)\.(?<minor>\d+)\.(?<patch>\d+)$') {
        return $null
    }

    [pscustomobject]@{
        Tag      = $Tag
        Major    = [int]$matches['major']
        Minor    = [int]$matches['minor']
        Patch    = [int]$matches['patch']
        MinorKey = "{0}.{1}" -f [int]$matches['major'], [int]$matches['minor']
    }
}

function Get-MinorCleanupPlan {
    param(
        [Parameter(Mandatory)]
        [object[]]$TagInfos
    )

    $groups = $TagInfos |
        Group-Object MinorKey |
        Sort-Object {
            ($_.Group | Select-Object -First 1).Major
        }, {
            ($_.Group | Select-Object -First 1).Minor
        }

    foreach ($group in $groups) {
        $ordered = $group.Group | Sort-Object Patch, Tag
        $minorLabel = "v{0}.{1}" -f $ordered[0].Major, $ordered[0].Minor

        $keepTags = [System.Collections.Generic.List[string]]::new()
        $keepTags.Add($ordered[0].Tag)

        if ($ordered.Count -gt 1) {
            $lastTag = $ordered[$ordered.Count - 1].Tag
            if ($lastTag -ne $ordered[0].Tag) {
                $keepTags.Add($lastTag)
            }
        }

        $removeTags = @(
            $ordered |
                Where-Object { $_.Tag -notin $keepTags } |
                Sort-Object Patch, Tag |
                Select-Object -ExpandProperty Tag
        )

        [pscustomobject]@{
            Minor     = $minorLabel
            AllTags   = @($ordered.Tag)
            KeepTags  = @($keepTags)
            RemoveTags = $removeTags
        }
    }
}

Write-Host "Working in: $repoRoot"

if (-not $SkipFetch) {
    Write-Host "Fetching tags from remote '$Remote'..."
    Invoke-Git -Arguments @('fetch', '--tags', $Remote) -Quiet
}

$allTags = @(Invoke-Git -Arguments @('tag', '--list'))
$stableTagInfos = @(
    $allTags |
        Where-Object { -not [string]::IsNullOrWhiteSpace($_) } |
        ForEach-Object { Get-StableTagInfo -Tag $_ } |
        Where-Object { $_ }
)

if (-not $stableTagInfos) {
    Write-Host "No stable tags matching vMAJOR.MINOR.PATCH were found."
    exit 0
}

$cleanupPlan = @(Get-MinorCleanupPlan -TagInfos $stableTagInfos)

Write-Host "`nCleanup plan by minor version:"
foreach ($entry in $cleanupPlan) {
    Write-Host "  [$($entry.Minor)]"
    Write-Host "    keep  : $($entry.KeepTags -join ', ')"
    if ($entry.RemoveTags.Count -gt 0) {
        Write-Host "    remove: $($entry.RemoveTags -join ', ')"
    } else {
        Write-Host "    remove: (none)"
    }
}

$tagsToDelete = @(
    $cleanupPlan |
        ForEach-Object { $_.RemoveTags } |
        Where-Object { $_ }
)

if (-not $tagsToDelete) {
    Write-Host "`nNo tags need to be deleted."
    exit 0
}

Write-Host "`nTags scheduled for deletion ($($tagsToDelete.Count)):"
$tagsToDelete | ForEach-Object { Write-Host "  $_" }

if ($DryRun) {
    Write-Host "`nDryRun mode enabled. No tags were deleted."
    exit 0
}

if (-not $WhatIfPreference) {
    $target = if ($DeleteRemote) { "local and remote '$Remote'" } else { "local" }
    $confirm = Read-Host "`nDelete these $($tagsToDelete.Count) tags from $target? (y/N)"
    if ($confirm -notmatch '^(y|yes)$') {
        Write-Host "Cancelled. No tags were deleted."
        exit 0
    }
}

$remoteTags = @()
if ($DeleteRemote) {
    $remoteTags = @(
        Invoke-Git -Arguments @('ls-remote', '--tags', $Remote) |
            ForEach-Object {
                $parts = ($_ -split "\s+", 2)
                if ($parts.Count -ge 2) {
                    $parts[1] -replace '^refs/tags/', '' -replace '\^\{\}$', ''
                }
            } |
            Where-Object { $_ } |
            Sort-Object -Unique
    )
}

foreach ($tag in $tagsToDelete) {
    if ($PSCmdlet.ShouldProcess($tag, 'Delete local tag')) {
        Invoke-Git -Arguments @('tag', '-d', $tag) -Quiet
        Write-Host "Deleted local tag: $tag"
    }

    if ($DeleteRemote) {
        if ($tag -in $remoteTags) {
            if ($PSCmdlet.ShouldProcess("$Remote/$tag", 'Delete remote tag')) {
                Invoke-Git -Arguments @('push', $Remote, '--delete', $tag) -Quiet
                Write-Host "Deleted remote tag: $tag"
            }
        } else {
            Write-Host "Remote tag not found, skipped: $tag"
        }
    }
}

Write-Host "`nDone."
