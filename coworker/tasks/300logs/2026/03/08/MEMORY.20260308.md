## Daily Memory - 2026-03-08

### coworker-script-config-loading / standardize-gh-copilot-calls
**Summary:** Unified all coworker/worker `gh copilot` invocations behind shared PowerShell/Bash helpers so main, delegated, streamed, captured, and direct worker runs use the same base flags and argument-building behavior.  
**Changes:** Updated `coworker.ps1`/`.sh` and worker entry points (`refine-last-draft`, memory generators, `git-sync`, `rename`) to source shared config, validate `COPILOT`, use shell-native command arrays, and avoid fragile string quoting. Added reusable `workers/gh-copilot.ps1` and `.sh`; fixed the bad PowerShell `GH_DEBUG` example.  
**Validation:** PowerShell parsing and Bash `bash -n` checks passed; smoke tests produced the expected `gh copilot --model gpt-5.4 --no-ask-user --log-level info --allow-all ...`.  
**Key Learnings:** Shared Copilot config must live in shell-native helpers/arrays, not strings. Every direct Copilot entry point should reuse the same base to prevent behavior drift. On Windows, LF-normalized temp copies and temp working dirs make Bash validation reliable.

### create-missing-draft-files
**Summary:** Added automatic placeholder maintenance so `coworker/tasks/0draft` always contains `1.md` through `5.md`.  
**Changes:** Added placeholder ensure functions to both PowerShell and Bash runners; ran them before queue listing and after finishing a task; restored missing `0draft/1.md`.  
**Validation:** PowerShell parser and Bash validation passed; confirmed `0draft` contains `1.md`-`5.md`.  
**Key Learnings:** Placeholder hygiene belongs in shared runners, and enforcing it both at startup and after task completion keeps PowerShell/Bash behavior aligned.

### webdriver-mcp-toolspec-update
**Summary:** Tightened MCP tool-spec generation to explicit `@MCP` annotations, added `WebDriver.drag(sourceSelector, targetSelector)`, and refreshed generated code-mirror artifacts so only annotated WebDriver/agent tools are exposed.  
**Changes:** Annotated executor-used WebDriver methods, implemented default drag via offsets + `dragAndDrop`, updated `ToolSpecGenerator` to emit only annotated methods and prefer case-insensitive `@mcp` KDoc sections, annotated MCP-visible `PerceptiveAgent` entry points, and regenerated artifacts/tests.  
**Validation:** Scripted coverage checks confirmed executor-called WebDriver methods map to `@MCP`; artifacts regenerated. Maven validation was blocked by a pre-existing Kotlin compile-daemon connection issue.  
**Key Learnings:** This tool-spec pipeline is source-text driven, so annotations and generated artifacts must stay synchronized. Restricting exposure to explicit `@MCP` prevents deprecated/helper overload leakage; when Maven is blocked, scripted consistency checks still provide useful evidence.

### webdriver-tool-alignment
**Summary:** Realigned browser4-cli WebDriver commands with actual MCP/backend behavior and added REST-side normalization for legacy callers.  
**Changes:** Updated CLI commands to real MCP names/args (`navigate`, `go_back`, `press`, `type`, `select_option`, `tab_*`, etc.), removed unsupported exports (`console`, `pdf`), updated snapshot/open/screenshot flows to use `open`, `aria_snapshot`, and `screenshot` with base64 file saving, and added snake_case + legacy payload normalization in `MCPToolController.kt`. Refreshed parser/controller/E2E expectations.  
**Validation:** `npx jest tests/commands.test.ts --runInBand` and `npx tsc --noEmit` passed in `sdks/browser4-cli`; targeted Maven controller testing hit the same pre-existing Kotlin daemon issue.  
**Key Learnings:** Safest CLI alignment is to match real MCP tool names and parameter shapes, not partial Playwright-style aliases. Small controller-side normalization preserves backward compatibility without reintroducing alias drift.

### add-draft-refinement-stage
**Summary:** Added a dedicated draft-refinement pipeline so drafts move from `0draft/refine/1ready` to `2working` to `3done`.  
**Changes:** Added `refine-drafts.ps1`/`.sh` to accept files/folders, resolve repo root from script location, move inputs into working, invoke shared `gh copilot` helpers to rewrite content, and move successful outputs to done while leaving failures visible in working. Added periodic runners (`run_draft_refinement_periodically.ps1`/`.sh`) and documented flow/commands in `coworker/README.md` and `README.zh.md`.  
**Validation:** Smoke tests in a temporary git repo with fake Copilot commands passed for PowerShell and Bash; sample drafts moved from `1ready` to `3done`; new shell scripts also passed `bash -n`.  
**Key Learnings:** Standalone coworker helpers should resolve repo root from script location, not caller cwd. Using `2working` as the in-progress artifact and only moving rewritten output to `3done` cleanly preserves failed inputs for inspection.


Total usage est:        1 Premium request
API time spent:         16s
Total session time:     24s
Total code changes:     +0 -0
Breakdown by AI model:
 gpt-5.4                 20.9k in, 1.1k out, 17.9k cached (Est. 1 Premium request)


### create-python-coworker
**Summary:** Added a Python implementation of the coworker task runner so the task pipeline can be driven without PowerShell or Bash.  
**Changes:** Added `coworker/scripts/coworker.py` mirroring repo-root discovery, task-folder lifecycle, placeholder maintenance, config-driven `gh copilot` invocation, task naming, memory init, logging, approved-task git-sync, and completion moves. Updated `coworker/README.md` and `coworker/README.zh.md` to document the Python entry point and correct the main coworker script paths.  
**Validation:** `python -m py_compile .\coworker\scripts\coworker.py`, `python .\coworker\scripts\coworker.py --help`, and a smoke test in a temporary repo with fake Copilot and memory helpers all passed; the smoke task was renamed, executed, logged, and moved into `3_1complete`.  
**Key Learnings:** A Python port can stay aligned with `coworker.ps1` by preserving the same folder contract and helper-script interfaces while loading `COPILOT` config directly. A temp repo with fake Copilot commands provides safe end-to-end coverage for task-runner changes without touching live coworker queues.
