# Builtin AI Coworker

## Coworker Workflow

1. **Draft**: Create your task files in `coworker/tasks/0prepare`.
2. **Queue**: Move the task files to `coworker/tasks/1created` when ready.
3. **Execute**: Run `coworker.ps1` (Windows) or `coworker.sh` (Linux/macOS) to process the task.
4. **Review**: The task moves to `coworker/tasks/3complete` after execution. Review the changes.
5. **Approve**: Move the task to `coworker/tasks/5approved` if you want it to be automatically committed/pushed by the periodic runner.

## Periodical Runner

The periodic runner monitors `1created` and `5approved` folders and executes tasks automatically.

**Windows (PowerShell):**

```powershell
.\coworker\scripts\run_coworker_periodically.ps1
```

**Linux/macOS (Bash):**

```bash
./coworker/scripts/run_coworker_periodically.sh
```

## Commit and Push

After tasks are completed, you can use the commit scripts to push changes to your repository.

**Windows (PowerShell):**

```powershell
.\coworker\scripts\commit.ps1
```

**Linux/macOS (Bash):**

```bash
./coworker/scripts/commit.sh
```
