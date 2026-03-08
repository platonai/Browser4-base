# Builtin AI Coworker

The Builtin AI Coworker is an agent that assists you with various tasks in your repository. It processes task files that you create, executes them, and can commit changes back to your repository.

## How to Use

1. run `run_coworker_periodically.ps1` to start
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

## Periodic Runner

The periodic runner monitors `1created` and `5approved` folders and processes tasks automatically.

**Windows (PowerShell):**

```powershell
.\coworker\scripts\run_coworker_periodically.ps1
```

**Linux/macOS (Bash):**

```bash
./coworker/scripts/run_coworker_periodically.sh
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
.\coworker\scripts\run_draft_refinement_periodically.ps1 -Once
```

**Linux/macOS (Bash):**

```bash
./coworker/scripts/workers/refine-drafts.sh
./coworker/scripts/workers/refine-drafts.sh ./coworker/tasks/0draft/refine/1ready
./coworker/scripts/run_draft_refinement_periodically.sh --once
```
