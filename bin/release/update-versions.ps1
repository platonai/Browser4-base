#!/usr/bin/env pwsh

$repoRoot = (git rev-parse --show-toplevel 2>$null)
Set-Location $repoRoot

Write-Host "Deploy the project ..."
Write-Host "Changing version ..."

$SNAPSHOT_VERSION = Get-Content "$repoRoot\VERSION" -TotalCount 1
$VERSION =$SNAPSHOT_VERSION -replace "-SNAPSHOT", ""
$VERSION | Set-Content "$repoRoot\VERSION"

# Replace SNAPSHOT version with the release version
@('pom.xml', 'llm-config.md', 'README.md', 'README.zh.md') | ForEach-Object {
  Get-ChildItem -Path "$repoRoot" -Depth 2 -Filter $_ -Recurse | ForEach-Object {
    (Get-Content $_.FullName) -replace $SNAPSHOT_VERSION, $VERSION | Set-Content -Encoding utf8 $_.FullName
  }
}
