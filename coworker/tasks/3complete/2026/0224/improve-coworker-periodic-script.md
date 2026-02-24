# Coworker Improvement

Improve [run_coworker_periodically.ps1](../../scripts/run_coworker_periodically.ps1)

Only when there are new tasks in the following directories, the script will run coworker to process the tasks:

- 1created
- 5approved

If there are no new tasks, the script will skip running coworker and wait for the next scheduled time.
This will help save system resources and avoid unnecessary processing when there are no new tasks to handle.
