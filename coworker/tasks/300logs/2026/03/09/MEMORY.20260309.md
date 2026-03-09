## Daily Memory - 2026-03-09

- **PulsarWebDriver click tests realigned:** Replaced disabled `PulsarWebDriverClickTests` that still used an obsolete dynamic page with active E2E coverage against `generated/interactive-screens.html`, updating selectors to current IDs like `#addButton` and `#toggleMessageButton`. Coverage now includes count-based clicks, modifier clicks, missing-element no-ops, disabled-button behavior, navigation, and sequential interactions. **Learning:** When generated mock pages drift, refresh tests against real HTML instead of preserving stale selectors; `E2ETest` coverage requires `-DrunE2ETests=true` or Maven may report zero tests.  
  **Validation:** `.\mvnw.cmd -f pulsar-tests\pom.xml -pl pulsar-it-tests -am -DrunE2ETests=true -Dtest=PulsarWebDriverClickTests -D"surefire.failIfNoSpecifiedTests=false" test` → **13 tests, 0 failures/errors**. Sequential assertions must match real page behavior; calculator validation messages can be expected when prerequisite fields are unset.

- **Coworker periodic-script rename cleanup:** Renamed legacy periodic entrypoints to queue-oriented names via `coworker/scripts/process-coworker-queue.ps1` and `process-draft-refinement-queue.ps1`, moved scheduler-backed implementations under `coworker/scripts/deprecated/`, kept old `run_*_periodically.ps1` files as compatibility shims, and updated scheduler/docs. **Learning:** Once `coworker-scheduler.ps1` owns recurrence, helper names should describe queue processing, not scheduling; shims preserve existing automation.  
  **Validation:** AST-parsed touched `.ps1` files and imported `coworker-scheduler.config.psd1` to confirm configured script paths still resolve. In shared environments, AST parsing plus config-path validation is a safe, side-effect-free regression check.

- **ARIA snapshot rendering alignment:** Implemented Playwright-style ARIA snapshot rendering through `DOMState.render()` and `PageHandler.ariaSnapshot()`, with a renderer emitting roles, names, state attrs, refs, pointer hints, inline text, and `/url` from DOM/AX data. Also added `href` to default included attributes so link URLs survive DOM-state construction. **Learning:** For Playwright-compatible accessibility YAML, render from the richer micro/unfiltered tree instead of generic object YAML so refs, cursor hints, and semantic properties are reconstructed deterministically.  
  **Validation:** `.\mvnw.cmd -pl pulsar-core\pulsar-browser -am -Dtest=DOMStateBuilderTest -D"surefire.failIfNoSpecifiedTests=false" test` → **11 tests, 0 failures/errors, 1 skipped**. Small renderer-focused unit tests are sufficient for formatting changes.

- **Scheduler empty-queue skip:** Updated `coworker/scripts/coworker-scheduler.ps1` so tasks can declare `PendingPaths`; the scheduler now checks those paths before spawning child PowerShell processes. Default `coworker` and `draft-refinement` entries now use queue paths, preventing empty runs and extra PowerShell window popups. **Learning:** Queue-awareness belongs in the unified scheduler because avoiding child-process launch is what removes empty-run UX noise.  
  **Validation:** Temporary-config harness ran `coworker-scheduler.ps1 -Once` with empty and non-empty queues, confirming `WaitingForWork`/`RunCount=0` with no launch in the first case and normal worker execution in the second.

- **MicroToNanoTreeHelper review:** Reviewed `MicroDOMTreeNodeHelper.kt` and found two correctness issues: `toNanoTreeInRangeRecursive()` can drop visible leaf nodes or in-range descendants by pre-pruning on child presence and wrapper Y-range, and `toInteractiveDOMTreeNodeList()` mixes `nodeId` with `paintOrder/backendNodeId` when computing `textBefore`, which can attach surrounding text to the wrong interactive node. **Outcome:** Delivered review guidance only. **Learning:** In DOM summarizers, recurse before pruning generated subtrees, and use one monotonic traversal order for any “text between nodes” feature.


Total usage est:        1 Premium request
API time spent:         16s
Total session time:     27s
Total code changes:     +0 -0
Breakdown by AI model:
 gpt-5.4                 21.5k in, 960 out, 1.5k cached (Est. 1 Premium request)


- **PromptBuilder English translation cleanup:** Translated all remaining Chinese prompt text in `PromptBuilder.kt` into concise English, including tool-use rules, extraction guidance, observe/summary prompts, browser-state messaging, and task metadata strings. Removed the redundant alternate English rules block by aliasing it to the canonical rule set and normalized `language` output to `Chinese`/`English`. **Learning:** Centralizing prompt guidance in one English source avoids drift between duplicated CN/EN constants and makes future prompt edits safer.  
  **Validation:** `python -c "from pathlib import Path; p = Path(r'D:\workspace\Browser4\Browser4-4.6\pulsar-agentic\src\main\kotlin\ai\platon\pulsar\agentic\inference\PromptBuilder.kt'); lines=p.read_text(encoding='utf-8').splitlines(); [print(f'{i+1}: {line}') for i,line in enumerate(lines) if any(ord(ch)>127 for ch in line)]"` → **no remaining non-ASCII prompt text**. ` .\mvnw.cmd -pl pulsar-agentic -am -DskipTests compile` → **build succeeded**.
