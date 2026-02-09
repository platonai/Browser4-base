#!/usr/bin/env pwsh

[CmdletBinding(SupportsShouldProcess = $true, ConfirmImpact = "High")]
param(
    [int]$MinAgeDays = 7,
    [switch]$IncludeRemote,
    [string]$RemoteName = "origin"
)

if ($MinAgeDays -lt 0) {
    throw "MinAgeDays must be zero or greater."
}

function Get-RefLines([string]$refGlob, [string]$scope) {
    git for-each-ref --format="$scope %(refname:short) %(committerdate:unix)" $refGlob
}

if (-not $PSBoundParameters.ContainsKey("IncludeRemote")) {
    $IncludeRemote = $true
    Write-Verbose "IncludeRemote not specified; defaulting to include remote refs"
}

# Only target copilot branches; include remote refs only when requested.
$refLines = @()
$refLines += Get-RefLines "refs/heads/copilot/*" "local"
if ($IncludeRemote) {
    $refLines += Get-RefLines "refs/remotes/$RemoteName/copilot/*" "remote"
}

Write-Verbose "Loaded $($refLines.Count) candidate refs"

$currentBranch = (git rev-parse --abbrev-ref HEAD).Trim()
$now = [DateTimeOffset]::UtcNow
$ageSummaries = New-Object System.Collections.Generic.List[string]

foreach ($line in $refLines) {
    if ([string]::IsNullOrWhiteSpace($line)) {
        continue
    }

    $parts = $line -split " ", 3
    if ($parts.Count -lt 3) {
        continue
    }

    $scope = $parts[0].Trim()
    $refName = $parts[1].Trim()

    if ($refName -eq $currentBranch) {
        Write-Verbose "Skipping current branch $refName"
        continue
    }

    $unixSeconds = 0
    if (-not [long]::TryParse($parts[2], [ref]$unixSeconds)) {
        continue
    }

    $lastCommitDateTime = [DateTimeOffset]::FromUnixTimeSeconds($unixSeconds)
    $ageDays = ($now - $lastCommitDateTime).TotalDays
    $ageSummaries.Add("$refName -> $([math]::Round($ageDays, 2)) days")
    if ($ageDays -lt $MinAgeDays) {
        Write-Verbose "Skipping $refName (age $([math]::Round($ageDays, 2)) days < $MinAgeDays)"
        continue
    }

    if ($scope -eq "remote") {
        $remoteBranch = $refName -replace "^$RemoteName/", ""
        Write-Verbose "Deleting remote branch $RemoteName/$remoteBranch"
        if ($PSCmdlet.ShouldProcess("$RemoteName/$remoteBranch", "Delete remote branch")) {
            git push $RemoteName --delete $remoteBranch
        }
    } else {
        Write-Verbose "Deleting local branch $refName"
        if ($PSCmdlet.ShouldProcess($refName, "Delete local branch")) {
            git branch -D $refName
        }
    }
}

if ($ageSummaries.Count -gt 0) {
    Write-Verbose "Branch ages:"
    $ageSummaries | ForEach-Object { Write-Verbose $_ }
}
