# Coworker Task Drafting Area

Draft your tasks here. This area is for tasks under preparation and not yet ready for execution.

Once a task is clearly defined and actionable, move it to the `1created` directory. The coworker will automatically process tasks in that directory.

To start processing, run `run_coworker_periodically.ps1`. The coworker will pick up tasks from the `1created` directory.

You may use any format for your task file, but the following template is recommended for clarity:

```markdown
# Task Title

## Problem
Clearly describe the issue or task. Be specific and concise.

## Solution
Outline your proposed approach, including steps, methods, or techniques.

## References
List relevant resources, documentation, or helpful links.
```

## Examples

- [Simple Example - refine-coworker-readme.md.md](../6git-pushed/2026/0228/refine-coworker-readme.md.md)
- [Advanced Example - implement-daily-memory-batching.md](../6git-pushed/2026/0228/implement-daily-memory-batching.md)

## References

- [coworker.ps1](../../scripts/coworker.ps1)
- [coworker.sh](../../scripts/coworker.sh)
- [run_coworker_periodically.ps1](../../scripts/run_coworker_periodically.ps1)
- [run_coworker_periodically.sh](../../scripts/run_coworker_periodically.sh)
