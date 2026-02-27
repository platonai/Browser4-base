# Auto-approve support for coworker

When coworker.ps1/sh works on a task, if the task has tag "#auto-approve", coworker will automatically approve the task when it is completed.

The current logic is as follows:

When a task is completed, the task file is moved to "3complete" folder.

New logic:

When a task is completed, if the task has tag "#auto-approve", the task file is moved to "4approved" folder.
Otherwise, the task file is moved to "3complete" folder.

#auto-approve
