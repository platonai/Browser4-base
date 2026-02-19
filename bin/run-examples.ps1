#!/usr/bin/env pwsh

$repoRoot = (git rev-parse --show-toplevel 2>$null)
Set-Location $repoRoot

# Import common utility script
. $repoRoot\bin\common\Util.ps1

Fix-Encoding-UTF8

# 运行
./mvnw.cmd `
  -D"file.encoding=UTF-8" `
  -D"exec.mainClass=ai.platon.pulsar.examples.agent.Browser4AgentKt" `
  -pl examples/browser4-examples `
  exec:java
