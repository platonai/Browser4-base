# Improve Scheduled Coworker Tasks

The coworker system currently relies on multiple scheduled tasks without a unified management approach. As a result, task creation, execution, and maintenance are inconsistent across the system.

We need to design a complete, production-ready PowerShell-based scheduler with the following goals:

- Support multiple independent tasks
- Support concurrent execution
- Provide centralized scheduling
- Record logs and execution status
- Require only a single Task Scheduler trigger
- Allow task settings such as enablement and execution frequency to be configured through a configuration file
- Keep current implementation running, not breaking existing functionality

The scheduler must periodically run coworker, draft refinement, task source monitoring, and similar jobs in separate processes.

## Current Implementation

- [run_coworker_periodically.ps1](../../scripts/run_coworker_periodically.ps1)
- [run_coworker_periodically.sh](../../scripts/run_coworker_periodically.sh)
- [run_draft_refinement_periodically.ps1](../../scripts/run_draft_refinement_periodically.ps1)
- [run_draft_refinement_periodically.sh](../../scripts/run_draft_refinement_periodically.sh)
- [monitor-task-source.ps1](../../scripts/monitor-task-source.ps1)
- [monitor-task-source.sh](../../scripts/monitor-task-source.sh)

## Proposed Improvements

1. **Unified Scheduler**
   Create a single scheduler that manages all recurring tasks, instead of maintaining separate scripts and timers for each one.

2. **Configuration-Driven Tasks**
   Use a single configuration file to define task behavior, including whether a task is enabled, how often it runs, and other runtime parameters. This will simplify maintenance and make task management more consistent.
