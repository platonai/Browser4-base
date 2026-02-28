# MEMORY.20260228.md
## Daily Memory - 2026-02-28

### Tasks Executed
- **MCP Documentation Optimization**:
    - Updated `WebDriver` KDoc to be more concise and suitable for LLM tool descriptions.
    - Implemented logic in `SourceCodeToToolCallSpec` to extract specific paragraphs tagged with `#mcp` (or `@mcp`) as the tool description.
    - Populated `ToolSpec.help` with the full KDoc content while keeping `ToolSpec.description` concise.
- **Coworker Documentation**: Refined `coworker/README.md` to accurately reflect the 9-stage task pipeline and script paths.
- **Coworker Automation**: Attempted to implement auto-approval logic for tasks tagged with `#auto-approve`.
- **Browser4 Node.js SDK**:
    - Built `browser4-nodejs` SDK and added 163 new tests (197 total), covering `PulsarClient`, `PulsarSession`, `AgenticSession`, and `WebDriver`.
    - Fixed a critical bug in `createSession` where `typeof null === 'object'` caused a crash.
- **Task Monitoring System**:
    - Created `bin/monitor.sh` and `bin/monitor.ps1` to monitor GitHub issues assigned to `@galaxyeye` and dispatch them to `coworker/tasks/2working`.
    - Implemented URL monitoring for keyword detection.
    - Optimized the monitor to prevent duplicate processing by filtering `is:open -label:processed` and applying a `processed` label after dispatch.
- **Daily Memory Generator Optimization**:
    - Updated `coworker-daily-memory-generator` scripts to use character-based truncation (2000 chars for prompts, 3000 for output) alongside line-based log truncation (Head 10 + Tail 300).
    - Enhanced script to target `*.task.log` and `*.copilot.log` specifically, extracting "clean prompts" to further save tokens.
    - Implemented a backup mechanism (`.bak`) before overwriting memory files.
    - Fixed `gh copilot` command invocation (using `-p` with `--` separator) and a `Rename-Item` bug in PowerShell.
- **Task Source Monitor Deduplication**:
    - Implemented content-based deduplication in `task-source-monitor` (ps1/sh) using MD5 hashing of fetched URL content.
    - Added logic to append the hash to the task file as a marker and skip dispatch if the hash already exists in `coworker/tasks`.
- **Coworker Memory System Implementation**:
    - Integrated memory management into `coworker.ps1` and `coworker.sh`.
    - Configured the agent to read context from Global (`MEMORY.md`), Yearly, Monthly, and Daily memory files in `coworker/tasks/300logs`.
    - Added prompt instructions for the agent to update the daily memory file after task completion.
- **Coworker Loop Detection**:
    - Implemented a safety mechanism in `run_coworker_periodically` (ps1/sh) to detect stuck agents.
    - Monitors the ratio of activity logs to total lines; kills the process if activity is < 5% for 3 consecutive minutes.
    - Automatically moves stuck tasks to `3_5aborted`.
- **Coworker Memory Generator Fixes**:
    - Resolved `error: unknown option '---#'` in `coworker-daily-memory-generator.ps1` by using `Start-Process` with `ArgumentList`.
    - Fixed `gh copilot` argument parsing across multiple scripts (`coworker.ps1`, `git-sync.ps1`, `coworker.sh`) to handle special characters in prompts correctly.
- **Daily Memory Batching**:
    - Implemented batch processing for `coworker-daily-memory-generator` to handle large logs (approx. 15k chars per batch) and avoid token limits.
- **Token Usage Analysis**:
    - Created `count-total-token-usage` script to aggregate token usage and estimate costs.
    - Reported total estimated cost for Feb 25-28, 2026 as ~$135.36 (Dominant model: `gemini-3-pro-preview`).
- **Daily Memory Compression**:
    - Implemented self-compression for daily memory files > 3000 characters.
    - Backs up original to `MEMORY.YYYYMMDD.long.md` and overwrites with compressed version using `gh copilot`.
- **Article Creation**:
    - Created "How I created a project builtin AI coworker" article based on project structure and documentation.
- **Timezone Handling Improvement**:
    - Updated all coworker scripts (`coworker.ps1`, `task-source-monitor.ps1`, etc.) to use UTC for all date/time operations.
    - Replaced `Get-Date` with `(Get-Date).ToUniversalTime()` in PowerShell and added `-u` flag to `date` in Bash.

### Execution Quality Review
- **Success**: The tooling for MCP specification generation (SourceCodeToToolCallSpec) was significantly improved. It now supports flexible tag-based description extraction and separates short descriptions from detailed help text.
- **Success**: The Node.js SDK is now robust with high test coverage and a critical bug fix.
- **Success**: The task monitoring system is fully operational and optimized for idempotency, preventing duplicate task execution.
- **Success**: The Coworker agent now has a persistent memory system and self-healing capabilities (loop detection), significantly increasing its autonomy and reliability.
- **Success**: The memory generator scripts are now highly robust, handling special characters in prompts and large log files via batching and compression.
- **Success**: Standardized timezone handling to UTC across all scripts improves system reliability and consistency across different environments.
- **Efficiency**: The iterative updates to `WebDriver` KDoc (first adding `#mcp`, then refining to `@mcp`) showed good responsiveness to requirements, though doing it in one pass would have been better.
- **Efficiency**: The memory generator optimization significantly reduces token usage for future daily summaries by focusing on the most relevant log sections.
- **Inefficiency**: The task `035344-coworker-auto-approve-support` failed completely due to an argument parsing error, wasting a cycle.

### Issues Encountered
- **Script Failure**: Task `coworker-auto-approve-support` failed with "too many arguments" (Exit Code 1). This suggests a syntax error in the shell command construction or parameter passing within the agent's internal tool invocation.
- **Tool Noise**: Recurring "unknown option '--no-warnings'" errors in stderr suggest a configuration issue with the Node.js or runtime environment invoking the Copilot CLI or its underlying scripts.
- **SDK Bug**: `src/client.ts` failed when the server returned `null` because `typeof null` is `'object'`. Fixed by adding a truthiness guard.
- **Path Error**: Task `optimize-coworker-memory-generator` failed initially because it attempted to access `coworker\coworker.ps1` instead of the correct path `coworker\scripts\coworker.ps1`.
- **Argument Parsing**: `gh copilot` command failed with "unknown option" when prompts contained dashes or comments. Fixed by using `--` separator and proper argument escaping.
- **Platform Compatibility**: `chmod` command failed on Windows during script setup, requiring alternative handling or manual intervention for executable permissions.
- **File Operations**: `Rename-Item` in PowerShell failed to overwrite existing files, necessitating a switch to `Move-Item -Force`.

### Root Cause Analysis
- **Auto-approve Failure**: The "too many arguments" error usually indicates unquoted strings containing spaces being passed to a shell command or a tool that expects a fixed number of arguments.
- **Documentation Churn**: The switch from `#mcp` to `@mcp` indicates the initial specification for the tag wasn't fully settled or standard before implementation began.
- **Null Safety**: JavaScript's `typeof null` quirk is a common source of runtime errors; explicit null checks are necessary when dealing with external API responses.
- **Path Verification**: Scripts must verify file paths before execution. The `coworker.ps1` location assumption caused a failure in the optimization task.
- **Shell Interpretation**: The `gh copilot` errors were caused by the shell interpreting prompt content as flags. Strict argument separation with `--` is essential for CLI tools wrapping other commands.
- **Windows File System**: Windows handling of file permissions and overwrite operations differs significantly from Unix, requiring specific PowerShell commands (`Move-Item -Force` vs `mv`, ignoring `chmod`) to ensure cross-platform compatibility.

### Process Improvement Insight
- **Robust Argument Handling**: When modifying shell scripts (like `coworker.ps1/sh`), ensure strict quoting of file paths and tags to prevent argument parsing errors. Use `Start-Process -ArgumentList` in PowerShell for complex arguments.
- **Standardize Tags Early**: Define metadata tags (like `@mcp` vs `#mcp`) in a design spec before mass-updating code to avoid double-work.
- **Log Management**: Truncating large logs (preserving head/tail) is a crucial optimization for maintaining context without exceeding token limits.
- **Idempotency**: When monitoring external sources (like GitHub issues or URLs), always implement a state-tracking mechanism (like labels or content hashes) to prevent duplicate task creation.
- **Self-Correction**: The implementation of loop detection demonstrates a proactive approach to handling agent failures, moving from manual intervention to automated recovery.
- **Context Synthesis**: When historical memory logs are sparse, inferring context from codebase structure (as done for the article) is a viable fallback strategy.
