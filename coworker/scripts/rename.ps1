param(
    [Parameter(Mandatory=$true)]
    [string]$FilePath
)

if (-not (Test-Path $FilePath)) {
    Write-Error "File not found: $FilePath"
    exit 1
}

$repoRoot = (git rev-parse --show-toplevel 2>$null)
if (-not $repoRoot) {
    Write-Host "Repo root not found. Exiting."
    exit 1
}
Set-Location $repoRoot

$content = Get-Content -Path $FilePath -Raw

# Initialize variables
$title = ""
$description = ""
$prompt = ""

# Parse structured content
if ($content -match "(?ms)^Title:\s*(?<title>.*?)(\r\n|\n)Description:\s*(?<desc>.*?)(\r\n|\n)Prompt:\s*(?<prompt>.*)$") {
    $title = $Matches['title'].Trim()
    $description = $Matches['desc'].Trim()
    $prompt = $Matches['prompt'].Trim()
} else {
    $fileItem = Get-Item $FilePath
    $title = $fileItem.BaseName
    $description = "Task from $($fileItem.Name)"
    $prompt = $content
}

$promptSample = $prompt
if ($promptSample.Length -gt 600) {
    $promptSample = $promptSample.Substring(0, 600)
}

$namingPrompt = @"
Create a short, descriptive task name in English kebab-case (3-6 words max). Output only the name.
Title: $title
Description: $description
Prompt: $promptSample
"@

$copilotNameTimeoutSeconds = 60
$Fallback = $title -replace '[\\/*?:"<>|]', '_'
if ([string]::IsNullOrWhiteSpace($Fallback)) {
    $Fallback = "task"
}

try {
    $promptEscaped = $namingPrompt -replace '"', '\"'
    $nameArgs = "-p `"$promptEscaped`" --allow-all-tools --allow-all-paths"

    # Use temporary files for redirecting output
    $nameStdOut = [System.IO.Path]::GetTempFileName()
    $nameStdErr = [System.IO.Path]::GetTempFileName()

    $nameProcess = Start-Process -FilePath "gh" -ArgumentList "copilot $nameArgs" -NoNewWindow -PassThru -RedirectStandardOutput $nameStdOut -RedirectStandardError $nameStdErr

    $waited = $false
    try {
        $null = Wait-Process -Id $nameProcess.Id -Timeout $copilotNameTimeoutSeconds -ErrorAction Stop
        $waited = $true
    } catch {
        $waited = $false
    }

    if (-not $waited -or -not $nameProcess.HasExited) {
        Stop-Process -Id $nameProcess.Id -Force -ErrorAction SilentlyContinue
        Remove-Item $nameStdOut -ErrorAction SilentlyContinue
        Remove-Item $nameStdErr -ErrorAction SilentlyContinue
        Write-Output $Fallback
        exit 0
    }

    $rawName = ""
    if (Test-Path $nameStdOut) {
        $rawName = (Get-Content -Path $nameStdOut | Where-Object { $_ -and $_.Trim() } | Select-Object -First 1)
    }

    Remove-Item $nameStdOut -ErrorAction SilentlyContinue
    Remove-Item $nameStdErr -ErrorAction SilentlyContinue

    if ($nameProcess.ExitCode -ne 0 -or [string]::IsNullOrWhiteSpace($rawName)) {
        Write-Output $Fallback
        exit 0
    }

    $normalized = $rawName.Trim()
    $normalized = $normalized -replace '\s+', '-'
    $normalized = $normalized -replace '[^A-Za-z0-9._-]', '-'
    $normalized = $normalized -replace '-+', '-'
    $normalized = $normalized.Trim(' ', '.', '-', '_')
    if ($normalized.Length -gt 60) {
        $normalized = $normalized.Substring(0, 60).Trim(' ', '.', '-', '_')
    }

    if ([string]::IsNullOrWhiteSpace($normalized)) {
        Write-Output $Fallback
    } else {
        Write-Output $normalized
    }
}
catch {
    Write-Output $Fallback
}
