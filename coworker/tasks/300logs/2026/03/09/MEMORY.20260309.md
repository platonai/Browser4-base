## Daily Memory - 2026-03-09

- **PulsarWebDriver click test realignment:** Replaced disabled `PulsarWebDriverClickTests` that still targeted an obsolete dynamic page with active E2E coverage against `generated/interactive-screens.html`, updating selectors to the live page (`#addButton`, `#toggleMessageButton`, etc.). The new suite covers count-based clicks, modifier clicks, missing-element no-ops, disabled-button handling, navigation, and sequential interaction flows. **Learning:** When generated mock pages change, refresh fixtures against actual HTML instead of preserving stale selectors; Browser4 tests tagged `E2ETest` require `-DrunE2ETests=true` or Maven may report zero executed tests.

- **Validation:** Verified with `.\mvnw.cmd -f pulsar-tests\pom.xml -pl pulsar-it-tests -am -DrunE2ETests=true -Dtest=PulsarWebDriverClickTests -D"surefire.failIfNoSpecifiedTests=false" test` → **13 tests, 0 failures, 0 errors**. **Learning:** Sequential click assertions should match real page behavior; e.g. calculator validation messages may be expected if fields remain unset in the tested flow.

- **Coworker periodic-script rename cleanup:** Renamed legacy periodic coworker entrypoints to queue-oriented names by adding `coworker/scripts/process-coworker-queue.ps1` and `coworker/scripts/process-draft-refinement-queue.ps1`, moving scheduler-backed implementations under `coworker/scripts/deprecated/`, and updating scheduler/docs while keeping old `run_*_periodically.ps1` files as deprecation shims. **Learning:** Once `coworker-scheduler.ps1` owns recurrence, helper names should describe queue processing rather than imply they are the scheduler; compatibility wrappers allow clearer naming without breaking automation.

- **Validation:** Verified renamed scripts by PowerShell AST-parsing touched `.ps1` files and importing `coworker-scheduler.config.psd1` to confirm configured script paths still resolve. **Learning:** In shared environments, AST parsing plus config-path validation is a safe way to catch syntax and rename regressions without triggering side effects.

- **ARIA snapshot rendering alignment:** Implemented Playwright-style ARIA snapshot rendering through `DOMState.render()` and `PageHandler.ariaSnapshot()`, adding a dedicated renderer that emits roles, names, state attributes, refs, pointer hints, inline text, and `/url` from DOM/AX data. Also added `href` to default included attributes so link URLs survive standard DOM-state construction. **Learning:** For Playwright-compatible accessibility YAML, render from the richer micro/unfiltered tree rather than generic object YAML so refs, cursor hints, and semantic props are reconstructed deterministically.

- **Validation:** Verified with `.\mvnw.cmd -pl pulsar-core\pulsar-browser -am -Dtest=DOMStateBuilderTest -D"surefire.failIfNoSpecifiedTests=false" test` → **11 tests, 0 failures, 0 errors, 1 skipped**. **Learning:** Small renderer-focused unit tests are sufficient to lock down Playwright-style formatting without requiring full browser E2E coverage for every output tweak.


Total usage est:        1 Premium request
API time spent:         12s
Total session time:     21s
Total code changes:     +0 -0
Breakdown by AI model:
 gpt-5.4                 21.1k in, 788 out, 16.9k cached (Est. 1 Premium request)


- **Scheduler empty-queue skip:** Updated `coworker/scripts/coworker-scheduler.ps1` so scheduled tasks can declare `PendingPaths`, and the scheduler now checks those files/directories before spawning a child PowerShell process. The default `coworker` and `draft-refinement` entries now use queue paths, which prevents disruptive empty runs when there is nothing to process. **Learning:** Queue-awareness belongs in the unified scheduler rather than only in the legacy worker scripts, because avoiding the child-process launch is what eliminates the unnecessary PowerShell window popups.

- **Validation:** Verified with a targeted PowerShell harness that runs `coworker-scheduler.ps1 -Once` against a temporary config twice: first with an empty queue (confirmed `WaitingForWork`, `RunCount = 0`, and no worker launch), then with a queued file (confirmed the worker launched and exited successfully). **Learning:** For scheduler scripts, a temporary config plus marker script is a safe way to prove both skip and launch behavior without touching the real coworker queues in a shared environment.
