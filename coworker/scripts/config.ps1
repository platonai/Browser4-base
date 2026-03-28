$configDataPath = Join-Path $PSScriptRoot 'config.psd1'
if (-not (Test-Path $configDataPath)) {
    throw "Config data file not found: $configDataPath"
}

$script:configData = Import-PowerShellDataFile -Path $configDataPath
if (-not $script:configData.ContainsKey('COPILOT')) {
    throw "COPILOT is not defined in $configDataPath"
}

function Get-CoworkerConfigValue {
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

function Resolve-CoworkerConfiguredPath {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,
        [string]$BaseDirectory = $PSScriptRoot
    )

    if ([string]::IsNullOrWhiteSpace($Path)) {
        throw 'Configured path cannot be empty.'
    }

    if ([System.IO.Path]::IsPathRooted($Path)) {
        return [System.IO.Path]::GetFullPath($Path)
    }

    return [System.IO.Path]::GetFullPath((Join-Path $BaseDirectory $Path))
}

function Get-CoworkerConfigData {
    return $script:configData
}

function Get-WorkspaceRoot {
    $pathsConfig = Get-CoworkerConfigValue -Map $script:configData -Key 'Paths' -DefaultValue @{}
    $path = [string](Get-CoworkerConfigValue -Map $pathsConfig -Key 'WorkspaceRoot' -DefaultValue '..\..')
    return Resolve-CoworkerConfiguredPath -Path $path
}

function Get-CoworkerRoot {
    $pathsConfig = Get-CoworkerConfigValue -Map $script:configData -Key 'Paths' -DefaultValue @{}
    $path = [string](Get-CoworkerConfigValue -Map $pathsConfig -Key 'CoworkerRoot' -DefaultValue '..')
    return Resolve-CoworkerConfiguredPath -Path $path
}

function Get-TasksRoot {
    $pathsConfig = Get-CoworkerConfigValue -Map $script:configData -Key 'Paths' -DefaultValue @{}
    $path = [string](Get-CoworkerConfigValue -Map $pathsConfig -Key 'TasksRoot' -DefaultValue '..\tasks')
    return Resolve-CoworkerConfiguredPath -Path $path
}

function Get-SchedulerWorkingDirectory {
    $schedulerConfig = Get-CoworkerConfigValue -Map $script:configData -Key 'Scheduler' -DefaultValue @{}
    $path = [string](Get-CoworkerConfigValue -Map $schedulerConfig -Key 'WorkingDirectory' -DefaultValue '..\..')
    return Resolve-CoworkerConfiguredPath -Path $path
}

function Resolve-WorkspacePath {
    param(
        [Parameter(Mandatory = $true)]
        [string]$RelativePath
    )

    return Resolve-CoworkerConfiguredPath -Path $RelativePath -BaseDirectory (Get-WorkspaceRoot)
}

function Resolve-CoworkerPath {
    param(
        [Parameter(Mandatory = $true)]
        [string]$RelativePath
    )

    return Resolve-CoworkerConfiguredPath -Path $RelativePath -BaseDirectory (Get-CoworkerRoot)
}

function Resolve-TasksPath {
    param(
        [Parameter(Mandatory = $true)]
        [string]$RelativePath
    )

    return Resolve-CoworkerConfiguredPath -Path $RelativePath -BaseDirectory (Get-TasksRoot)
}

function Test-CoworkerPlaceholderFile {
    param(
        [Parameter(Mandatory = $true)]
        [System.IO.FileSystemInfo]$Item
    )

    return $Item.Name -eq '.gitkeep'
}

function Test-CoworkerDotPath {
    param(
        [Parameter(Mandatory = $true)]
        [System.IO.FileSystemInfo]$Item
    )

    $currentItem = $Item
    while ($null -ne $currentItem) {
        if ($currentItem.Name.StartsWith('.')) {
            return $true
        }

        # FileInfo exposes Directory; DirectoryInfo exposes Parent.
        if ($currentItem.PSObject.Properties.Match('Directory').Count -gt 0) {
            $currentItem = $currentItem.Directory
            continue
        }

        if ($currentItem.PSObject.Properties.Match('Parent').Count -gt 0) {
            $currentItem = $currentItem.Parent
            continue
        }

        $currentItem = $null
    }

    return $false
}

function Test-CoworkerIgnoredFile {
    param(
        [Parameter(Mandatory = $true)]
        [System.IO.FileSystemInfo]$Item
    )

    return (Test-CoworkerDotPath -Item $Item) -or (Test-CoworkerPlaceholderFile -Item $Item)
}

function Test-CoworkerPendingFile {
    param(
        [Parameter(Mandatory = $true)]
        [System.IO.FileSystemInfo]$Item
    )

    return -not $Item.PSIsContainer -and -not (Test-CoworkerIgnoredFile -Item $Item)
}

function Test-CoworkerActionableDraftRefinementFile {
    param(
        [Parameter(Mandatory = $true)]
        [System.IO.FileSystemInfo]$Item
    )

    if (-not (Test-CoworkerPendingFile -Item $Item)) {
        return $false
    }

    $content = Get-Content -LiteralPath $Item.FullName -Raw -Encoding UTF8 -ErrorAction Stop
    return -not [string]::IsNullOrWhiteSpace($content)
}

function Ensure-CoworkerDraftRefinementPlaceholders {
    param(
        [Parameter(Mandatory = $true)]
        [string]$DraftDirectory,
        [ValidateRange(1, 100)]
        [int]$MaxCount = 5
    )

    if (-not (Test-Path -LiteralPath $DraftDirectory)) {
        New-Item -ItemType Directory -Path $DraftDirectory -Force | Out-Null
    }

    foreach ($draftNumber in 1..$MaxCount) {
        $draftPath = Join-Path $DraftDirectory "$draftNumber.md"
        if (-not (Test-Path -LiteralPath $draftPath)) {
            Set-Content -LiteralPath $draftPath -Value '' -Encoding UTF8
        }
    }
}

function Get-CoworkerQueueFiles {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,
        [switch]$Recurse
    )

    if (-not (Test-Path -LiteralPath $Path)) {
        return @()
    }

    $getChildItemParameters = @{
        LiteralPath  = $Path
        File         = $true
        ErrorAction  = 'SilentlyContinue'
    }

    if ($Recurse) {
        $getChildItemParameters.Recurse = $true
    }

    return @(Get-ChildItem @getChildItemParameters | Where-Object { -not (Test-CoworkerIgnoredFile -Item $_) })
}

$COPILOT = @($script:configData['COPILOT'])
