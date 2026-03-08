# Daily Memory - 2026-03-08

## Task: load-configured-coworker-scripts (2026-03-08)

### Summary
Updated coworker PowerShell and Bash runners to load `coworker/scripts/config.ps1` and `coworker/scripts/config.sh` before invoking Copilot, so the configured CLI flags are applied consistently.

### Changes
- Converted `config.ps1` and `config.sh` to explicit command arrays representing the configured Copilot invocation.
- Updated `coworker.ps1` to source `config.ps1`, validate the configured command, and reuse it for both task-name generation and main task execution.
- Updated `coworker.sh` to source `config.sh`, validate the bash array shape, and reuse it for naming and main task execution paths.

### Validation
- Parsed `coworker/scripts/coworker.ps1` with the PowerShell parser and verified the loaded `COPILOT` array resolves to `gh copilot --model gpt-5.4 --no-ask-user --log-level info --allow-all`.
- Validated `coworker/scripts/coworker.sh` syntax using a normalized LF temp copy and verified the sourced `COPILOT` bash array starts with `gh copilot`.

### Lessons Learned
- Shared CLI configuration files should use native shell array forms rather than shell-fragile plain strings when multiple scripts need to compose extra arguments safely.
- On Windows, bash validation against repo files may require normalizing CRLF content to LF in a temporary copy before running `bash -n`.
