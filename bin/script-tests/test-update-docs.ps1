#!/usr/bin/env pwsh
$repoRoot = (git rev-parse --show-toplevel 2>$null)
Set-Location $repoRoot

function Set-TextPreserveNewline {
  param(
    [Parameter(Mandatory)] [string] $Path,
    [Parameter(Mandatory)] [string] $Text,
    [Parameter(Mandatory)] [string] $NewLine
  )

  $normalized = $Text -replace "\r\n|\r|\n", $NewLine
  [System.IO.File]::WriteAllText($Path, $normalized, [System.Text.UTF8Encoding]::new($false))
}

# Replace SNAPSHOT version with the release version
@('README.md', 'README.zh.md') | ForEach-Object {
  Get-ChildItem -Path "$repoRoot" -Depth 5 -Filter $_ -Recurse | ForEach-Object {
    $path = $_.FullName
    $original = Get-Content -Path $path -Raw
    $nl = if ($original -match "\r\n") { "`r`n" } else { "`n" }

    $updated = $original -replace "Browser4", "Browser4Test"

    if ($updated -ne $original) {
      Set-TextPreserveNewline -Path $path -Text $updated -NewLine $nl
    }
  }
}
