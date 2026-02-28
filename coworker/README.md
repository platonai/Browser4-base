# Builtin AI Coworker

The Builtin AI Coworker is an agent that assists you with various tasks in your repository. It processes task files that you create, executes them, and can commit changes back to your repository.

## How It Works

Task files flow through a pipeline of numbered folders inside `coworker/tasks/`:

| Stage | Folder | Description |
|-------|--------|-------------|
| Draft | `0prepare` | Create and draft your task files here |
| Queue | `1created` | Move tasks here when ready for execution |
| Plan | `200plan` | Agent planning phase (managed automatically) |
| Work | `2working` | Agent is actively executing the task |
| Complete | `3complete` | Execution finished — review the changes |
| Review | `4review` | Optional manual review stage |
| Approved | `5approved` | Approved tasks awaiting commit/push |
| Pushed | `6git-pushed` | Successfully committed and pushed |
| Archive | `700archive` | Archived completed tasks |

## Quick Start

1. **Draft** — Create your task file in `coworker/tasks/0prepare/`.
2. **Queue** — Move it to `coworker/tasks/1created/` when ready.
3. **Execute** — Run the coworker script to process the task:
   - Windows: `.\coworker\scripts\coworker.ps1`
   - Linux/macOS: `./coworker/scripts/coworker.sh`
4. **Review** — Task moves to `3complete` after execution. Review the changes.
5. **Approve** — Move the task to `5approved` to have it automatically committed and pushed by the periodic runner.

## Prerequisites

GitHub CLI (`gh`) must be installed and authenticated.

See https://github.com/cli/cli#installation for installation instructions.

## Tags

You can use tags in task files to provide additional context or control behavior.

Supported tags:

- `#auto-approve` — Automatically move the task to `5approved` after completion instead of `3complete`. Useful for trusted, low-risk tasks that can be committed without manual review.

## Mentions

> **Experimental**

Mention `@coworker` in a task file to notify the agent to process the task.

## Syncing with Git

After tasks are approved, push changes to your repository using the git-sync scripts.

**Windows (PowerShell):**

```powershell
.\coworker\scripts\git-sync.ps1
```

**Linux/macOS (Bash):**

```bash
./coworker/scripts/git-sync.sh
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
