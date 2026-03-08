## Daily Memory - 2026-03-08

### coworker-script-config-loading / standardize-gh-copilot-calls
**Summary:** Standardized all coworker/worker `gh copilot` invocations around shared shell-specific config/helpers so main, delegated, streamed, captured, and direct worker runs all use the same base flags and arg-building behavior.

**Changes:** Updated `coworker.ps1`/`coworker.sh` plus worker entry points (`refine-last-draft`, `coworker-memory-generator`, `coworker-daily-memory-generator`, `git-sync`, `rename`) to source shared config/helpers, validate `COPILOT`, reuse shell-native command arrays, and avoid fragile string quoting. Added reusable `workers/gh-copilot.ps1` and `gh-copilot.sh` for config loading, arg formatting, invocation, and process launch; fixed the invalid PowerShell `GH_DEBUG` example.

**Validation:** PowerShell parsing passed; helper smoke tests emitted the expected `gh copilot --model gpt-5.4 --no-ask-user --log-level info --allow-all ...`; Bash checks passed via `bash -n` on LF-normalized temp copies and temp-dir smoke tests.

**Key Learnings:** Shared Copilot config should live in shell-native arrays/helpers, not strings. Every direct Copilot entry point must reuse the shared base or behavior drifts. On Windows, LF-normalized temp copies and temp working dirs make Bash validation reliable across CRLF/path-conversion issues.

### create-missing-draft-files
**Summary:** Added automatic placeholder maintenance so `coworker/tasks/0draft` always contains `1.md` through `5.md`.

**Changes:** Added placeholder ensure functions to both PowerShell and Bash runners; ran them before queue listing and after moving a finished task; restored missing `coworker/tasks/0draft/1.md`.

**Validation:** PowerShell parser passed; Bash validation passed on an LF-normalized temp copy; confirmed `0draft` contains `1.md`-`5.md`.

**Key Learnings:** Placeholder hygiene belongs in shared runners, and enforcing it both at startup and after task completion keeps PowerShell/Bash behavior aligned.

### webdriver-mcp-toolspec-update
**Summary:** Tightened MCP tool-spec generation to explicit `@MCP` annotations, added `WebDriver.drag(sourceSelector, targetSelector)`, and refreshed code-mirror artifacts so only annotated WebDriver/agent tools are exposed.

**Changes:** Added `@MCP` coverage for executor-used WebDriver methods; implemented default drag via offsets + `dragAndDrop`; updated `ToolSpecGenerator` to emit only annotated methods, prefer case-insensitive `@mcp` KDoc sections, and otherwise fall back to full KDoc/humanized names; annotated MCP-visible `PerceptiveAgent` entrypoints and refreshed generated artifacts/tests.

**Validation:** Scripted coverage checks confirmed executor-called WebDriver methods map to `@MCP` methods; code-mirror artifacts regenerated. Maven validation was blocked by a pre-existing Kotlin compile-daemon connection issue.

**Key Learnings:** The tool-spec pipeline is source-text driven, so annotations and generated code-mirror artifacts must stay synchronized. Restricting exposure to explicit `@MCP` prevents deprecated/helper overload leakage; when Maven is blocked, scripted consistency checks still provide useful evidence.

### webdriver-tool-alignment
**Summary:** Realigned browser4-cli’s WebDriver command surface with actual MCP/backend behavior and added REST-side normalization for legacy callers.

**Changes:** Updated CLI commands to real MCP names/args (`navigate`, `go_back`, `press`, `type`, `select_option`, `tab_*`, etc.); removed unsupported exports like `console`/`pdf`; updated `program.ts` snapshot/open/screenshot flows to call `open`, `aria_snapshot`, and `screenshot` with base64 file saving; added snake_case and legacy payload normalization in `MCPToolController.kt`; refreshed parser/controller/E2E expectations.

**Validation:** Passed `npx jest tests/commands.test.ts --runInBand` and `npx tsc --noEmit` in `sdks/browser4-cli`; targeted Maven controller testing was blocked by the same pre-existing Kotlin compile-daemon issue.

**Key Learnings:** Safest CLI alignment is to match real MCP tool names and parameter shapes, not partial Playwright-style aliases. Small controller-side normalization preserves backward compatibility without reintroducing alias drift.


Total usage est:        1 Premium request
API time spent:         13s
Total session time:     22s
Total code changes:     +0 -0
Breakdown by AI model:
 gpt-5.4                 20.6k in, 1.0k out, 17.9k cached (Est. 1 Premium request)

### add-draft-refinement-stage
**Summary:** Added a dedicated draft-refinement pipeline and runners so coworker drafts can be refined automatically from `coworker/tasks/0draft/refine/1ready` into `2working` and then `3done`.

**Changes:** Added `refine-drafts.ps1`/`.sh` workers that accept a file or folder, resolve the repo root from script location, move inputs into the refine working stage, invoke shared `gh copilot` helpers to rewrite content, and move successful outputs into the done stage while leaving failures visible in working. Added `run_draft_refinement_periodically.ps1`/`.sh` for on-demand (`-Once`/`--once`) or interval-based processing, and documented the new refinement flow plus commands in `coworker/README.md` and `coworker/README.zh.md`.

**Validation:** Ran smoke tests in a temporary git repo with fake Copilot commands for both PowerShell and Bash workers/runners, and verified each path moved sample drafts from `1ready` to `3done`. Also passed `bash -n` parsing for the new shell scripts.

**Key Learnings:** Repo-root discovery for standalone coworker helpers should be based on script location, not the caller's current working directory, otherwise validation and cross-repo invocation can resolve the wrong repository. Using the `2working` stage as the in-progress artifact and moving the rewritten file to `3done` cleanly matches the requested queue semantics while preserving failed inputs for inspection.
