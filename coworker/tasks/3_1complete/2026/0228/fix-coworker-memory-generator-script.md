# Fix coworker-daily-memory-generator

(base) PS D:\workspace\Browser4\Browser4-4.6> D:/workspace/Browser4/Browser4-4.6/coworker/scripts/coworker-daily-memory-generator.ps1
Generating daily memory for 2026-02-28 from logs in coworker\tasks\300logs\2026\02\28...
警告: Logs are too long (91255 chars). Truncating...
警告: Memory file coworker\tasks\300logs\2026\02\28\MEMORY.20260228.md already exists.
Backed up existing memory file to coworker\tasks\300logs\2026\02\28\MEMORY.20260228.md.bak
Calling gh copilot...
error: unknown option '---
#'

Try 'copilot --help' for more information.
Memory generation task completed.

## Solution

Read [coworker.ps1](../../scripts/coworker.ps1) to learn how to use `gh copilot` in PowerShell.
The error message indicates that the script is passing an unknown option `---#` to the `gh copilot` command.
This likely means that there is a formatting issue in the way the script is constructing the command or handling the input logs.
