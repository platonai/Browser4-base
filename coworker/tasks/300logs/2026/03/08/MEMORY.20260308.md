# Daily Memory - 2026-03-08

## Task: coworker-script-config-loading
**Summary:** Standardized coworker runners and worker scripts to load shared Copilot command config from `coworker/scripts/config.ps1` and `config.sh`, so the same CLI flags apply across main, delegated, streamed, and captured runs.

**Changes:**
- Replaced fragile string-based Copilot commands with native shell command arrays in both config files.
- Updated `coworker.ps1` and `coworker.sh` to source config, validate the `COPILOT` array, and reuse it for task naming and execution.
- Applied the same pattern to direct worker entry points:
  - `refine-last-draft.ps1/.sh`
  - `coworker-memory-generator.ps1/.sh`
  - `coworker-daily-memory-generator.ps1/.sh`
- All direct Copilot calls now route through the shared executable + argument array.

**Validation:**
- PowerShell scripts parsed successfully.
- Verified resolved PowerShell array expands to `gh copilot --model gpt-5.4 --no-ask-user --log-level info --allow-all`.
- Confirmed worker scripts append prompt/tool flags on top of the shared base command.
- Validated Bash scripts with `bash -n` on LF-normalized temp copies.
- Confirmed `config.sh` exposes a valid `COPILOT` bash array starting with `gh copilot`.

**Key Learnings / Structural Insight:**
- Shared CLI config should be stored as native shell arrays, not strings, when scripts need safe argument appends.
- Every script that invokes Copilot directly must validate and reuse the shared array or delegated flows will drift from configured flags.
- On Windows, Bash syntax validation is more reliable on LF-normalized temp copies when repo files use CRLF.
- Coworker tooling now follows a stronger pattern: shared shell-specific command-array config -> per-entrypoint validation -> reuse across primary and delegated flows.

## Task: create-missing-draft-files
**Summary:** Added automatic placeholder maintenance so `coworker/tasks/0draft` always contains `1.md` through `5.md` at runner startup and after task completion.

**Changes:**
- Added `Ensure-DraftPlaceholders` to `coworker/scripts/coworker.ps1`.
- Added `ensure_draft_placeholders` to `coworker/scripts/coworker.sh`.
- Wired both runners to recreate missing numbered draft files before queue listing and immediately after moving a finished task.
- Restored missing `coworker/tasks/0draft/1.md`.

**Validation:**
- PowerShell runner parsed successfully.
- Bash runner passed `bash -n` on an LF-normalized temp copy.
- Verified `coworker/tasks/0draft` contains `1.md` to `5.md`.

**Key Learnings:**
- Placeholder maintenance belongs in the shared runners so Windows and Bash behavior stays aligned.
- Checking at startup and after task completion preserves both immediate repo hygiene and future automation consistency.
- No monthly memory append was needed because the 2026-03-07 rollup already existed.


Total usage est:        1 Premium request
API time spent:         10s
Total session time:     18s
Total code changes:     +0 -0
Breakdown by AI model:
 gpt-5.4                 20.1k in, 737 out, 17.9k cached (Est. 1 Premium request)


## Task: webdriver-mcp-toolspec-update
**Summary:** Aligned MCP tool-spec generation with explicit `@MCP` annotations, added a real `WebDriver.drag(sourceSelector, targetSelector)` API, and refreshed code-mirror tool-spec resources to expose only annotated WebDriver/agent tools.

**Changes:**
- Added `@MCP` coverage across WebDriver methods exercised by `WebDriverToolExecutor`, and implemented a default `drag` helper in `WebDriver` that computes source/target offsets before delegating to `dragAndDrop`.
- Updated `ToolSpecGenerator` to emit specs only for `@MCP`-annotated methods, prefer `@mcp`-tagged KDoc paragraphs (case-insensitive), fall back to the full KDoc when no tag is present, and humanize method names when KDoc is absent.
- Simplified `WebDriverToolExecutor` to reuse the interface-level `drag` method/spec instead of maintaining a custom drag spec, annotated MCP-visible `PerceptiveAgent` entrypoints, added focused tests, and refreshed `code-mirror` JSON/text artifacts.

**Validation:**
- Verified via scripted coverage check that every `WebDriverToolExecutor.callFunctionOn` WebDriver method now maps to an `@MCP`-annotated WebDriver function.
- Regenerated `pulsar-core/pulsar-resources/src/main/resources/code-mirror/{WebDriver.kt.txt,PerceptiveAgent.kt.txt,driver-tool-call-specs.json,agent-tool-call-specs.json}` from the updated sources.
- Attempted Maven validation with `./mvnw -pl pulsar-agentic -am ...`, but the environment hit a pre-existing Kotlin compile-daemon connection failure in `pulsar-common` / `pulsar-tests-common` before reaching the changed module.

**Key Learnings:**
- Browser4's tool-spec pipeline is source-text driven, so `@MCP` annotations and code-mirror JSON/text artifacts must stay in sync whenever MCP-visible interfaces change.
- Constraining generated specs to explicit annotations removes deprecated/helper overloads (for example `navigateTo`) from MCP exposure and makes KDoc tagging rules matter for prompt-visible descriptions.
- Current local validation is vulnerable to Kotlin daemon startup failures outside the edited module; when that happens, scripted consistency checks are still useful to verify annotation/spec coverage while documenting the build blocker.
