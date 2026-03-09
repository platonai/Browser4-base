# Builtin AI Coworker

The Builtin AI Coworker is an agent that assists you with various tasks in your repository. It processes task files that you create, executes them, and can commit changes back to your repository.

## How to Use

1. run `coworker-scheduler.ps1` to start recurring automation
2. draft tasks in `0draft` (or anywhere)
3. copy ready tasks to `1created` for execution
4. once executed, you can find results in `3_1complete` and detailed logs in `300logs`
5. review results if needed
6. move task file from `3_1complete` to `5approved` to trigger git pushing

## How It Works

Task files flow through a pipeline of numbered folders inside `coworker/tasks/`:

| Stage | Folder | Description |
|-------|--------|-------------|
| Draft | `0draft` | Create and draft your task files here |
| Queue | `1created` | Move tasks here when ready for execution |
| Plan | `200plan` | Agent planning phase (managed automatically) |
| Work | `2working` | Agent is actively executing the task |
| Complete | `3_1complete` | Execution finished — review the changes |
| Review | `4review` | Optional manual review stage |
| Approved | `5approved` | Approved tasks awaiting commit/push |
| Pushed | `6git-pushed` | Successfully committed and pushed |
| Archive | `700archive` | Archived completed tasks |

## Quick Start

1. **Draft** — Create your task file in `coworker/tasks/0draft/`.
2. **Queue** — Move it to `coworker/tasks/1created/` when ready.
3. **Execute** — Run the coworker script to process the task:
   - Windows: `.\coworker\scripts\coworker.ps1`
   - Python: `python .\coworker\scripts\coworker.py`
   - Linux/macOS: `./coworker/scripts/coworker.sh`
   - Linux/macOS (Python): `python3 ./coworker/scripts/coworker.py`
4. **Review** — Task moves to `3_1complete` after execution. Review the changes.
5. **Approve** — Move the task to `5approved` to have it automatically committed and pushed by the periodic runner.

## Prerequisites

GitHub CLI (`gh`) must be installed and authenticated.

See https://github.com/cli/cli#installation for installation instructions.

## Tags

You can use tags in task files to provide additional context or control behavior.

Supported tags:

- `#auto-approve` — Automatically move the task to `5approved` after completion instead of `3_1complete`. Useful for trusted, low-risk tasks that can be committed without manual review.

## Mentions

> **Experimental**

Mention `@coworker` in a task file to notify the agent to process the task.

## Syncing with Git

After tasks are approved, push changes to your repository using the git-sync scripts.

**Windows (PowerShell):**

```powershell
.\coworker\scripts\workers\git-sync.ps1
```


**Linux/macOS (Bash):**

```bash
./coworker/scripts/workers/git-sync.sh
```

## Unified Scheduler (PowerShell)

Use the unified scheduler when you want a single Windows Task Scheduler trigger to manage all recurring coworker jobs. The scheduler launches each configured task in its own PowerShell process, records stdout/stderr logs, and continuously writes task status to `coworker/tasks/300logs/scheduler/scheduled-tasks.status.json`.

Task definitions live in `coworker/scripts/coworker-scheduler.config.psd1`. Each entry can be enabled or disabled independently and sets its own `IntervalSeconds`, script path, arguments, optional `DependsOn` task ordering, and optional `PendingPaths` input queues. When `PendingPaths` is configured, the scheduler checks those files/folders and skips spawning a PowerShell child process until work is actually present.

**Windows (PowerShell):**

```powershell
.\coworker\scripts\coworker-scheduler.ps1
.\coworker\scripts\coworker-scheduler.ps1 -Once
```

Default scheduled tasks:

- `coworker` — processes queued coworker tasks after task-source monitoring
- `draft-refinement` — processes the draft refinement queue
- `task-source-monitor` — polls configured task sources and dispatches new tasks when enabled

The scheduler invokes the legacy one-shot implementations from `coworker/scripts/deprecated/`. The clearer PowerShell entry points are `coworker/scripts/process-coworker-queue.ps1`, `coworker/scripts/process-draft-refinement-queue.ps1`, and `coworker/scripts/task-source-monitor.ps1`. The older `run_*_periodically.ps1` names remain as compatibility shims and print a deprecation warning before delegating.

## Legacy Queue Processors

For direct one-shot or looped execution, use the clearer legacy queue processors:

- `coworker/scripts/process-coworker-queue.ps1`
- `coworker/scripts/process-draft-refinement-queue.ps1`
- `coworker/scripts/task-source-monitor.ps1`

The scheduler-backed implementations live in:

- `coworker/scripts/deprecated/process-coworker-queue.ps1`
- `coworker/scripts/deprecated/process-draft-refinement-queue.ps1`
- `coworker/scripts/deprecated/task-source-monitor.ps1`

The older names remain available as deprecated aliases for backward compatibility:

- `coworker/scripts/run_coworker_periodically.ps1`
- `coworker/scripts/run_draft_refinement_periodically.ps1`

For recurring automation, prefer `coworker-scheduler.ps1`.

**Windows (PowerShell):**

```powershell
.\coworker\scripts\process-coworker-queue.ps1
.\coworker\scripts\process-coworker-queue.ps1 -Once
```

## Draft Refinement

Draft refinement uses a dedicated pipeline under `coworker/tasks/0draft/refine/`:

- `1ready` — drafts waiting to be refined
- `2working` — drafts currently being refined
- `3done` — refined drafts ready for review

You can refine a single file or every file in a folder. When a folder is provided, files are processed one by one.

**Windows (PowerShell):**

```powershell
.\coworker\scripts\workers\refine-drafts.ps1
.\coworker\scripts\workers\refine-drafts.ps1 -Path .\coworker\tasks\0draft\refine\1ready
.\coworker\scripts\coworker-scheduler.ps1
```

**Linux/macOS (Bash):**

```bash
./coworker/scripts/workers/refine-drafts.sh
./coworker/scripts/workers/refine-drafts.sh ./coworker/tasks/0draft/refine/1ready
pwsh ./coworker/scripts/process-draft-refinement-queue.ps1 -Once
```

