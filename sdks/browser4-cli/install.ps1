param(
    [switch]$WhatIf
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$script:RepoOwner = 'platonai'
$script:RepoName = 'Browser4'
$script:LatestReleaseApi = "https://api.github.com/repos/$($script:RepoOwner)/$($script:RepoName)/releases/latest"
$script:InstallRoot = $null
$script:LibDir = $null
$script:BinDir = $null
$script:TempDir = $null
$script:Architecture = $null
$script:PackageManager = $null

function Write-InstallerLog {
    param([string]$Message)

    Write-Host "[browser4-cli installer] $Message"
}

function Write-InstallerWarning {
    param([string]$Message)

    Write-Warning "[browser4-cli installer] $Message"
}

function Throw-InstallerError {
    param([string]$Message)

    throw "[browser4-cli installer] ERROR: $Message"
}

function Test-CommandAvailable {
    param([string]$Name)

    return [bool](Get-Command -Name $Name -ErrorAction SilentlyContinue)
}

function Resolve-InstallerPath {
    param([string]$PathValue)

    $expanded = [Environment]::ExpandEnvironmentVariables($PathValue)
    if ($expanded -eq '~') {
        $expanded = $HOME
    }
    elseif ($expanded.StartsWith('~/') -or $expanded.StartsWith('~\')) {
        $expanded = Join-Path -Path $HOME -ChildPath $expanded.Substring(2)
    }

    return [System.IO.Path]::GetFullPath($expanded)
}

function Initialize-Paths {
    $defaultInstallRoot = Join-Path -Path $HOME -ChildPath '.cargo'
    $defaultLibDir = Join-Path -Path $HOME -ChildPath '.browser4\lib'
    $installRootOverride = $env:BROWSER4_INSTALL_ROOT
    $libDirOverride = $env:BROWSER4_LIB_DIR

    if ([string]::IsNullOrWhiteSpace($installRootOverride)) {
        $installRootOverride = $defaultInstallRoot
    }

    if ([string]::IsNullOrWhiteSpace($libDirOverride)) {
        $libDirOverride = $defaultLibDir
    }

    $script:InstallRoot = Resolve-InstallerPath -PathValue $installRootOverride
    $script:LibDir = Resolve-InstallerPath -PathValue $libDirOverride
    $script:BinDir = Join-Path -Path $script:InstallRoot -ChildPath 'bin'
}

function Invoke-InstallerAction {
    param(
        [string]$Description,
        [scriptblock]$Action
    )

    if ($WhatIf) {
        Write-InstallerLog "What if: $Description"
        return $false
    }

    & $Action
    return $true
}

function New-InstallerTempDirectory {
    $path = Join-Path -Path ([System.IO.Path]::GetTempPath()) -ChildPath ("browser4-cli-installer-" + [Guid]::NewGuid().ToString('n'))
    New-Item -Path $path -ItemType Directory -Force | Out-Null
    return $path
}

function Cleanup {
    if ($script:TempDir -and (Test-Path -Path $script:TempDir)) {
        Remove-Item -Path $script:TempDir -Recurse -Force -ErrorAction SilentlyContinue
    }
}

function Assert-Windows {
    if ($env:OS -ne 'Windows_NT') {
        Throw-InstallerError 'This installer targets Windows PowerShell. Use install.sh on macOS or Linux.'
    }
}

function Get-Architecture {
    switch ($env:PROCESSOR_ARCHITECTURE) {
        'AMD64' { return 'x86_64' }
        'ARM64' { return 'arm64' }
        default { return $env:PROCESSOR_ARCHITECTURE.ToLowerInvariant() }
    }
}

function Test-Administrator {
    $identity = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = New-Object Security.Principal.WindowsPrincipal($identity)
    return $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

function Assert-Administrator {
    if (-not (Test-Administrator)) {
        Throw-InstallerError 'Installing prerequisites requires an elevated PowerShell session. Re-run this script as Administrator.'
    }
}

function Ensure-PackageManager {
    if ($script:PackageManager) {
        return $script:PackageManager
    }

    if (Test-CommandAvailable -Name 'winget') {
        $script:PackageManager = 'winget'
    }
    elseif (Test-CommandAvailable -Name 'choco') {
        $script:PackageManager = 'choco'
    }
    else {
        Throw-InstallerError 'No supported Windows package manager was found. Install winget (preferred) or Chocolatey and try again.'
    }

    Write-InstallerLog "Using package manager: $($script:PackageManager)"
    return $script:PackageManager
}

function Invoke-ExternalCommand {
    param(
        [string]$FilePath,
        [string[]]$Arguments,
        [string]$FailureMessage
    )

    & $FilePath @Arguments
    if ($LASTEXITCODE -ne 0) {
        Throw-InstallerError "$FailureMessage (exit code $LASTEXITCODE)."
    }
}

function Refresh-EnvironmentPath {
    $machinePath = [Environment]::GetEnvironmentVariable('Path', 'Machine')
    $userPath = [Environment]::GetEnvironmentVariable('Path', 'User')
    $joined = @($userPath, $machinePath) | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
    if ($joined.Count -gt 0) {
        $env:Path = ($joined -join ';')
    }
}

function Add-SessionPathEntry {
    param([string]$Directory)

    if ([string]::IsNullOrWhiteSpace($Directory) -or -not (Test-Path -Path $Directory)) {
        return
    }

    $entries = @($env:Path -split ';') | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
    if ($entries -notcontains $Directory) {
        $env:Path = "$Directory;$env:Path"
    }
}

function Ensure-UserPathEntry {
    param([string]$Directory)

    $current = [Environment]::GetEnvironmentVariable('Path', 'User')
    $entries = @()
    if (-not [string]::IsNullOrWhiteSpace($current)) {
        $entries = @($current -split ';') | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
    }

    if ($entries -contains $Directory) {
        Add-SessionPathEntry -Directory $Directory
        return
    }

    $updated = if ([string]::IsNullOrWhiteSpace($current)) { $Directory } else { "$current;$Directory" }
    if (-not (Invoke-InstallerAction -Description "Add $Directory to the user PATH" -Action {
        [Environment]::SetEnvironmentVariable('Path', $updated, 'User')
    })) {
        return
    }

    Add-SessionPathEntry -Directory $Directory
}

function Download-File {
    param(
        [string]$Url,
        [string]$Target
    )

    Write-InstallerLog "Downloading $(Split-Path -Path $Target -Leaf)..."
    for ($attempt = 1; $attempt -le 3; $attempt++) {
        try {
            Invoke-WebRequest -Uri $Url -OutFile $Target
            return
        }
        catch {
            if ($attempt -eq 3) {
                Throw-InstallerError "Failed to download $Url. $($_.Exception.Message)"
            }

            Start-Sleep -Seconds 2
        }
    }
}

function Install-WithWinget {
    param(
        [string]$Id,
        [string[]]$AdditionalArguments = @()
    )

    $arguments = @(
        'install',
        '--id', $Id,
        '--exact',
        '--silent',
        '--accept-package-agreements',
        '--accept-source-agreements',
        '--disable-interactivity'
    ) + $AdditionalArguments

    Invoke-ExternalCommand -FilePath 'winget' -Arguments $arguments -FailureMessage "winget failed while installing $Id"
}

function Install-WithChocolatey {
    param(
        [string]$Name,
        [string[]]$AdditionalArguments = @()
    )

    $arguments = @('install', $Name, '-y', '--no-progress') + $AdditionalArguments
    Invoke-ExternalCommand -FilePath 'choco' -Arguments $arguments -FailureMessage "Chocolatey failed while installing $Name"
}

function Install-Package {
    param(
        [string]$WingetId,
        [string]$ChocolateyName,
        [string]$DisplayName,
        [string[]]$WingetArguments = @(),
        [string[]]$ChocolateyArguments = @()
    )

    if (-not (Invoke-InstallerAction -Description "Install $DisplayName" -Action {
        Assert-Administrator
        switch (Ensure-PackageManager) {
            'winget' { Install-WithWinget -Id $WingetId -AdditionalArguments $WingetArguments }
            'choco' { Install-WithChocolatey -Name $ChocolateyName -AdditionalArguments $ChocolateyArguments }
            default { Throw-InstallerError "Unsupported package manager: $($script:PackageManager)" }
        }
    })) {
        return $false
    }

    Refresh-EnvironmentPath
    return $true
}

function Get-JavaMajorVersion {
    if (-not (Test-CommandAvailable -Name 'java')) {
        return 0
    }

    $output = (& java -version 2>&1 | Out-String)
    $match = [regex]::Match($output, 'version "([^"]+)"')
    if (-not $match.Success) {
        return 0
    }

    $version = $match.Groups[1].Value
    if ($version.StartsWith('1.')) {
        return [int]($version.Split('.')[1])
    }

    return [int]($version.Split('.')[0])
}

function Ensure-Java {
    $major = Get-JavaMajorVersion
    if ($major -ge 17) {
        Write-InstallerLog "Java $major is already installed."
        return
    }

    Write-InstallerLog 'Installing Java 17+...'
    $installed = Install-Package -WingetId 'EclipseAdoptium.Temurin.17.JDK' -ChocolateyName 'temurin17jdk' -DisplayName 'Java 17+'
    if (-not $installed) {
        return
    }

    $major = Get-JavaMajorVersion
    if ($major -lt 17) {
        Write-InstallerWarning 'Java installation completed, but java.exe is not available in the current session yet. Open a new shell after installation if needed.'
    }
}

function Test-ChromeInstalled {
    foreach ($candidate in @('chrome.exe', 'chrome', 'google-chrome', 'google-chrome-stable')) {
        if (Test-CommandAvailable -Name $candidate) {
            return $true
        }
    }

    foreach ($path in @(
        (Join-Path -Path $env:ProgramFiles -ChildPath 'Google\Chrome\Application\chrome.exe'),
        (Join-Path -Path ${env:ProgramFiles(x86)} -ChildPath 'Google\Chrome\Application\chrome.exe'),
        (Join-Path -Path $env:LOCALAPPDATA -ChildPath 'Google\Chrome\Application\chrome.exe')
    )) {
        if (Test-Path -Path $path) {
            return $true
        }
    }

    foreach ($registryPath in @(
        'HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion\App Paths\chrome.exe',
        'HKCU:\SOFTWARE\Microsoft\Windows\CurrentVersion\App Paths\chrome.exe'
    )) {
        if (Test-Path -Path $registryPath) {
            return $true
        }
    }

    return $false
}

function Ensure-Chrome {
    if (Test-ChromeInstalled) {
        Write-InstallerLog 'Google Chrome is already installed.'
        return
    }

    Write-InstallerLog 'Installing Google Chrome...'
    [void](Install-Package -WingetId 'Google.Chrome' -ChocolateyName 'googlechrome' -DisplayName 'Google Chrome')
}

function Test-BuildToolsInstalled {
    $vsWhere = Join-Path -Path ${env:ProgramFiles(x86)} -ChildPath 'Microsoft Visual Studio\Installer\vswhere.exe'
    if (Test-Path -Path $vsWhere) {
        $installationPath = & $vsWhere -latest -products * -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -property installationPath 2>$null | Select-Object -First 1
        if ($LASTEXITCODE -eq 0 -and -not [string]::IsNullOrWhiteSpace($installationPath)) {
            return $true
        }
    }

    foreach ($pattern in @(
        (Join-Path -Path ${env:ProgramFiles(x86)} -ChildPath 'Microsoft Visual Studio\*\*\VC\Tools\MSVC\*\bin\Hostx64\x64\cl.exe'),
        (Join-Path -Path $env:ProgramFiles -ChildPath 'Microsoft Visual Studio\*\*\VC\Tools\MSVC\*\bin\Hostx64\x64\cl.exe')
    )) {
        if (Get-ChildItem -Path $pattern -ErrorAction SilentlyContinue | Select-Object -First 1) {
            return $true
        }
    }

    return $false
}

function Ensure-BuildToolchain {
    if (Test-BuildToolsInstalled) {
        Write-InstallerLog 'Visual C++ Build Tools are already installed.'
        return
    }

    Write-InstallerLog 'Installing Visual C++ Build Tools...'
    $installed = Install-Package `
        -WingetId 'Microsoft.VisualStudio.2022.BuildTools' `
        -ChocolateyName 'visualstudio2022buildtools' `
        -DisplayName 'Visual C++ Build Tools' `
        -WingetArguments @('--override', '--wait --quiet --add Microsoft.VisualStudio.Workload.VCTools --includeRecommended --norestart') `
        -ChocolateyArguments @('--package-parameters', '"--add Microsoft.VisualStudio.Workload.VCTools --includeRecommended --passive --norestart"')
    if (-not $installed) {
        return
    }

    if (-not (Test-BuildToolsInstalled)) {
        Write-InstallerWarning 'Build Tools installation finished, but detection is not yet available in this session. Cargo may require a new shell or reboot to pick up the toolchain.'
    }
}

function Ensure-Rust {
    if ((Test-CommandAvailable -Name 'cargo') -and (Test-CommandAvailable -Name 'rustc')) {
        Write-InstallerLog 'Rust toolchain already installed.'
        return
    }

    Write-InstallerLog 'Installing Rust toolchain...'
    $installed = Install-Package -WingetId 'Rustlang.Rustup' -ChocolateyName 'rustup.install' -DisplayName 'Rust toolchain'
    if (-not $installed) {
        return
    }

    $cargoBin = Join-Path -Path $HOME -ChildPath '.cargo\bin'
    Add-SessionPathEntry -Directory $cargoBin

    if (-not ((Test-CommandAvailable -Name 'cargo') -and (Test-CommandAvailable -Name 'rustc'))) {
        Throw-InstallerError 'Rust installation completed, but cargo/rustc were not found on PATH. Open a new shell and re-run the installer.'
    }
}

function Resolve-LatestReleaseTag {
    $requestedTag = $env:BROWSER4_INSTALL_VERSION
    if ([string]::IsNullOrWhiteSpace($requestedTag)) {
        $release = Invoke-RestMethod -Headers @{ Accept = 'application/vnd.github+json' } -Uri $script:LatestReleaseApi
        $requestedTag = $release.tag_name
    }

    if ([string]::IsNullOrWhiteSpace($requestedTag)) {
        Throw-InstallerError 'Unable to determine the latest Browser4 release tag.'
    }

    if ($requestedTag.StartsWith('v')) {
        return $requestedTag
    }

    return "v$requestedTag"
}

function Install-Browser4Jar {
    param([string]$Tag)

    $targetPath = Join-Path -Path $script:LibDir -ChildPath 'Browser4.jar'
    $url = "https://github.com/$($script:RepoOwner)/$($script:RepoName)/releases/download/$Tag/Browser4.jar"

    if (-not (Invoke-InstallerAction -Description "Download Browser4.jar to $targetPath" -Action {
        New-Item -Path $script:LibDir -ItemType Directory -Force | Out-Null
        Download-File -Url $url -Target $targetPath
        if ((Get-Item -Path $targetPath).Length -le 0) {
            Throw-InstallerError 'Downloaded Browser4.jar is empty.'
        }
    })) {
        return
    }
}

function Install-Browser4CliFromSource {
    param([string]$Tag)

    $archivePath = Join-Path -Path $script:TempDir -ChildPath "Browser4-$Tag.zip"
    $archiveUrl = "https://github.com/$($script:RepoOwner)/$($script:RepoName)/archive/refs/tags/$Tag.zip"

    if (-not (Invoke-InstallerAction -Description "Build and install browser4-cli from Browser4 $Tag" -Action {
        Download-File -Url $archiveUrl -Target $archivePath
        Expand-Archive -Path $archivePath -DestinationPath $script:TempDir -Force

        $sourceRoot = Get-ChildItem -Path $script:TempDir -Directory | Where-Object { $_.Name -like 'Browser4*' } | Select-Object -First 1
        if (-not $sourceRoot) {
            Throw-InstallerError 'Unable to locate the extracted Browser4 source tree.'
        }

        $cliRoot = Join-Path -Path $sourceRoot.FullName -ChildPath 'sdks\browser4-cli'
        if (-not (Test-Path -Path $cliRoot)) {
            Throw-InstallerError 'Extracted release does not contain sdks/browser4-cli.'
        }

        New-Item -Path $script:InstallRoot -ItemType Directory -Force | Out-Null
        Push-Location -Path $cliRoot
        try {
            Invoke-ExternalCommand -FilePath 'cargo' -Arguments @('install', '--path', '.', '--root', $script:InstallRoot, '--locked', '--force') -FailureMessage 'Cargo failed while installing browser4-cli'
        }
        finally {
            Pop-Location
        }
    })) {
        return
    }

    $expectedBinary = Join-Path -Path $script:BinDir -ChildPath 'browser4-cli.exe'
    if (-not (Test-Path -Path $expectedBinary)) {
        Throw-InstallerError "browser4-cli.exe was not found at $expectedBinary after installation."
    }
}

function Print-Summary {
    param([string]$Tag)

    Write-InstallerLog 'Installation complete.'
    Write-InstallerLog "Release: $Tag"
    Write-InstallerLog "browser4-cli: $(Join-Path -Path $script:BinDir -ChildPath 'browser4-cli.exe')"
    Write-InstallerLog "Browser4.jar: $(Join-Path -Path $script:LibDir -ChildPath 'Browser4.jar')"
    Write-InstallerLog "Architecture: windows/$($script:Architecture)"

    $pathEntries = @($env:Path -split ';') | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
    if ($pathEntries -notcontains $script:BinDir) {
        Write-InstallerWarning "$($script:BinDir) is not on PATH in the current shell yet. Open a new shell or run: `$env:Path = '$($script:BinDir);' + `$env:Path"
    }
}

function Main {
    Assert-Windows
    Initialize-Paths

    $script:Architecture = Get-Architecture
    $script:TempDir = New-InstallerTempDirectory

    Write-InstallerLog "Detected architecture: $($script:Architecture)"

    Ensure-BuildToolchain
    Ensure-Java
    Ensure-Chrome
    Ensure-Rust

    $tag = Resolve-LatestReleaseTag
    Write-InstallerLog "Installing Browser4 $tag..."

    Install-Browser4Jar -Tag $tag
    Install-Browser4CliFromSource -Tag $tag
    Ensure-UserPathEntry -Directory $script:BinDir
    Print-Summary -Tag $tag
}

try {
    Main
}
finally {
    Cleanup
}

