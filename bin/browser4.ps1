#!/usr/bin/env pwsh

$ErrorActionPreference = "Stop"

$repoRoot = (git rev-parse --show-toplevel 2>$null)
Set-Location $repoRoot

# Import common utility script
. $repoRoot\bin\common\Util.ps1

Fix-Encoding-UTF8

$TARGET = Join-Path $PWD "target"
mkdir -f $TARGET
$UBERJAR = Join-Path $TARGET "Browser4.jar"
$SERVER_HOME = Join-Path $repoRoot "browser4\browser4-agents"
Copy-Item (Join-Path $SERVER_HOME "target\Browser4.jar") -Destination $UBERJAR

# Other Java options
$JAVA_OPTS = "$JAVA_OPTS -Dfile.encoding=UTF-8" # Use UTF-8

Write-Host "Using these JAVA_OPTS: $JAVA_OPTS"

Start-Process -NoNewWindow -Wait -FilePath "java" -ArgumentList ("$JAVA_OPTS", "-jar", "$UBERJAR")
