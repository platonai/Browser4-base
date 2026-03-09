## Daily Memory - 2026-03-09

- **PulsarWebDriver click test realignment:** Replaced disabled `PulsarWebDriverClickTests` that still targeted an obsolete dynamic page with active E2E coverage against `generated/interactive-screens.html`, updating selectors to the live page (`#addButton`, `#toggleMessageButton`, etc.). The suite now covers count-based clicks, modifier clicks, missing-element no-ops, disabled-button handling, navigation, and sequential interaction flows. **Learning:** When generated mock pages change, refresh fixtures against real HTML instead of preserving stale selectors; Browser4 tests tagged `E2ETest` require `-DrunE2ETests=true` or Maven may report zero tests.

- **Validation:** Ran `.\mvnw.cmd -f pulsar-tests\pom.xml -pl pulsar-it-tests -am -DrunE2ETests=true -Dtest=PulsarWebDriverClickTests -D"surefire.failIfNoSpecifiedTests=false" test` → **13 tests, 0 failures, 0 errors**. **Learning:** Sequential click assertions must match actual page behavior; calculator validation messages may be expected if prerequisite fields remain unset.

- **Coworker periodic-script rename cleanup:** Renamed legacy periodic coworker entrypoints to queue-oriented names by adding `coworker/scripts/process-coworker-queue.ps1` and `coworker/scripts/process-draft-refinement-queue.ps1`, moving scheduler-backed implementations under `coworker/scripts/deprecated/`, and keeping old `run_*_periodically.ps1` files as compatibility shims while updating scheduler/docs. **Learning:** Once `coworker-scheduler.ps1` owns recurrence, helper names should describe queue processing, not scheduling; shims preserve automation compatibility during cleanup.

- **Validation:** Verified renamed scripts by AST-parsing touched `.ps1` files and importing `coworker-scheduler.config.psd1` to confirm configured script paths still resolve. **Learning:** In shared environments, AST parsing plus config-path validation is a safe way to catch syntax and rename regressions without side effects.

- **ARIA snapshot rendering alignment:** Implemented Playwright-style ARIA snapshot rendering through `DOMState.render()` and `PageHandler.ariaSnapshot()`, adding a renderer that emits roles, names, state attrs, refs, pointer hints, inline text, and `/url` from DOM/AX data. Also added `href` to default included attributes so link URLs survive DOM-state construction. **Learning:** For Playwright-compatible accessibility YAML, render from the richer micro/unfiltered tree rather than generic object YAML so refs, cursor hints, and semantic props are reconstructed deterministically.

- **Validation:** Ran `.\mvnw.cmd -pl pulsar-core\pulsar-browser -am -Dtest=DOMStateBuilderTest -D"surefire.failIfNoSpecifiedTests=false" test` → **11 tests, 0 failures, 0 errors, 1 skipped**. **Learning:** Small renderer-focused unit tests are enough to lock down Playwright-style formatting without full browser E2E coverage for every output tweak.

- **Scheduler empty-queue skip:** Updated `coworker/scripts/coworker-scheduler.ps1` so scheduled tasks can declare `PendingPaths`; the scheduler now checks those paths before spawning child PowerShell processes. Default `coworker` and `draft-refinement` entries now use queue paths, preventing disruptive empty runs and extra PowerShell window popups. **Learning:** Queue-awareness belongs in the unified scheduler, because avoiding child-process launch is what removes unnecessary empty-run UX noise.

- **Validation:** Verified with a temporary-config PowerShell harness running `coworker-scheduler.ps1 -Once` twice: once with an empty queue (confirmed `WaitingForWork`, `RunCount = 0`, no worker launch) and once with queued work (confirmed worker launch and successful exit). **Learning:** For scheduler scripts, a temp config plus marker script safely proves both skip and launch behavior without touching real queues in a shared environment.


Total usage est:        1 Premium request
API time spent:         15s
Total session time:     25s
Total code changes:     +0 -0
Breakdown by AI model:
 gpt-5.4                 21.3k in, 911 out, 18.9k cached (Est. 1 Premium request)


- **MicroToNanoTreeHelper review:** Reviewed `pulsar-core/pulsar-browser/src/main/kotlin/ai/platon/browser4/driver/chrome/dom/util/MicroDOMTreeNodeHelper.kt` and found two substantive correctness issues. First, `toNanoTreeInRangeRecursive()` pre-filters child micro nodes by `children` presence and by the wrapper node's own Y-range, so visible leaf nodes and in-range descendants under out-of-range containers can disappear from the nano tree/ARIA snapshot. Second, `toInteractiveDOMTreeNodeList()` mixes `nodeId` with `paintOrder/backendNodeId` when computing `textBefore`, so surrounding text can attach to the wrong interactive node. **Outcome:** Delivered review feedback with concrete fix directions (recurse before pruning; use one consistent traversal order/visit index for text accumulation) and made no code changes. **Learning:** For DOM summarizers, pruning should happen after recursion on generated subtrees, and any "text between nodes" feature must use a single monotonic ordering source end-to-end.
