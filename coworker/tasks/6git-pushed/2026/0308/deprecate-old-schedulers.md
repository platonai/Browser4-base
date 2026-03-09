# Deprecate old schedulers

Move the old schedulers to a `deprecated` directory and update the documentation to point to the new schedulers in `coworker/coworker-scheduler`.
This will allow us to maintain backward compatibility while encouraging users to switch to the new schedulers.

Move the following old schedulers to directory `coworker/deprecated`:

[run_coworker_periodically.ps1](../../scripts/run_coworker_periodically.ps1)
[run_coworker_periodically.sh](../../scripts/run_coworker_periodically.sh)
[run_draft_refinement_periodically.ps1](../../scripts/run_draft_refinement_periodically.ps1)
[run_draft_refinement_periodically.sh](../../scripts/run_draft_refinement_periodically.sh)
[monitor-task-source.ps1](../../scripts/monitor-task-source.ps1)
[monitor-task-source.sh](../../scripts/monitor-task-source.sh)

## References

[coworker-scheduler.ps1](../../scripts/coworker-scheduler.ps1)
