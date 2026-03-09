# Improve coworker-scheduler

## Problem

Coworker-scheduler start process periodically, even if there are no tasks to process.
Every time the process starts, a new PowerShell instance is pop up, which is disruptive and unnecessary.

## Solution

Only call the processor when there are any tasks to process in coworker-scheduler.ps1.

## References

[coworker-scheduler.ps1](../../scripts/coworker-scheduler.ps1)
