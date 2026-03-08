# Daily Memory - 2026-03-08

## Task: coworker-script-config-loading

### Summary
Standardized all coworker runners and worker scripts to load shared Copilot command config from `coworker/scripts/config.ps1` and `coworker/scripts/config.sh`, ensuring configured CLI flags are applied consistently across main, delegated, streamed, and captured invocations.

### Changes
- Reworked `config.ps1` and `config.sh` to define explicit native shell command arrays for the Copilot invocation instead of fragile plain strings.
- Updated main runners:
  - `coworker.ps1` now sources `config.ps1`, validates the `COPILOT` array, and reuses it for both task-name generation and main execution.
  - `coworker.sh` now sources `config.sh`, validates bash-array shape, and reuses it for naming and main execution.
- Extended the same pattern to direct worker entry points:
  - `refine-last-draft.ps1/.sh`
  - `coworker-memory-generator.ps1/.sh`
  - `coworker-daily-memory-generator.ps1/.sh`
- All direct Copilot invocations now resolve through the shared configured executable + arguments.

### Validation
- PowerShell:
  - Parsed updated scripts successfully.
  - Verified resolved `COPILOT` array expands to:
    `gh copilot --model gpt-5.4 --no-ask-user --log-level info --allow-all`
  - Confirmed worker invocations append expected prompt/tool flags on top of the shared base command.
- Bash:
  - Validated scripts with `bash -n` using LF-normalized temp copies.
  - Confirmed sourced `config.sh` exposes a valid `COPILOT` array beginning with `gh copilot`.

### Key Learnings
- Shared CLI configuration should be represented as native shell arrays, not strings, when scripts need to safely append arguments.
- Every script entry point that invokes Copilot directly must validate and reuse the shared command array; otherwise delegated flows can drift from configured flags.
- On Windows, Bash validation is more reliable against LF-normalized temp copies when repository files use CRLF line endings.

### Structural Insight
This repo’s coworker tooling now follows a stronger pattern:
1. shared shell-specific command-array config,
2. per-entrypoint validation,
3. reuse of the same configured Copilot base command across primary and delegated flows.


Total usage est:        1 Premium request
API time spent:         9s
Total session time:     16s
Total code changes:     +0 -0
Breakdown by AI model:
 gpt-5.4                 19.9k in, 573 out, 16.9k cached (Est. 1 Premium request)


## Task: create-missing-draft-files

### Summary
Implemented automatic placeholder maintenance for the coworker drafting area so `coworker/tasks/0draft` always contains `1.md` through `5.md` when the runner starts and after a task is completed.

### Changes
- Added `Ensure-DraftPlaceholders` to `coworker/scripts/coworker.ps1` and `ensure_draft_placeholders` to `coworker/scripts/coworker.sh`.
- Wired both runners to recreate any missing numbered draft files before listing the draft queue and immediately after moving a finished task to its destination.
- Restored the currently missing placeholder file `coworker/tasks/0draft/1.md`.

### Validation
- Parsed `coworker/scripts/coworker.ps1` successfully with the PowerShell parser.
- Checked the updated Bash runner with `bash -n` on an LF-normalized temp copy.
- Verified `coworker/tasks/0draft` now contains `1.md`, `2.md`, `3.md`, `4.md`, and `5.md`.

### Key Learnings
- Placeholder maintenance belongs in the shared coworker runners so both Windows and Bash flows stay aligned.
- Running the placeholder check at startup and after task completion covers both immediate repo hygiene and future automated runs.
- The monthly memory already contains the 2026-03-07 rollup, so no monthly append was required for this task.
