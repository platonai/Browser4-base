Here’s a compressed version under 3000 characters:

## Daily Memory - 2026-03-14

- **DOM pipeline documented:** Added KDoc to `DOMStateBuilder.kt` clarifying `MergedDOMTree -> TinyDOMTreeNode -> MicroDOMTreeNode`. Key takeaway: explicitly document where fidelity is lost as rich internal DOM/AX state becomes compact prompt-facing nodes.

- **DOM serialization validation baseline:** Re-ran `DOMStateBuilderTest` before and after related work; it passed both times. Key takeaway: this is the smallest reliable safety check for DOM serialization and aria snapshot changes.

- **Nano-tree range pruning bug fixed:** Expanded `MicroToNanoTreeHelperTest` for overlapping leaves, open-start/closed-end ranges, merged seen-chunk behavior, invalid intervals, and full-range consistency. Root cause: `toNanoTreeInRangeRecursive()` filtered leaf children before recursion, which dropped valid prompt nodes. Fix: recurse first, then prune only empty placeholder nano nodes. Key takeaway: range tests must include true leaf descendants; pre-recursion filtering can remove the exact compact nodes prompts need.

- **Full-range behavior unified:** Reviewed `toNanoTree()`, `toNanoTreeInRange()`, and `toNanoTreeUnfiltered()`, then changed `toNanoTreeInRange()` so canonical full range (`0.0..1_000_000.0`) delegates to the unfiltered path. This removed drift where range traversal lost geometry-less nodes, zero-height nodes at `y == 0`, or nodes beyond the sentinel cutoff. Key takeaway: if “full range” is supposed to mean unfiltered, both must share one code path.

- **Best focused validation pair:** Ran `MicroToNanoTreeHelperTest` plus `DOMStateBuilderTest`. Key takeaway: this pair is the efficient regression suite for DOM serialization changes; in PowerShell, quote comma-separated Surefire class lists as one `-Dtest=...` value.

- **Aria snapshot rendering moved earlier:** Added `AriaSnapshotRenderer`, made `DOMState.ariaSnapshot` prefer `EnhancedDOMTreeNode`, extracted shared YAML formatting helpers, and deprecated `AriaSnapshotForNanoDOMTreeRenderer` for compatibility. This preserved AX metadata such as description and autocomplete-style/non-default properties that were lost during enhanced -> micro -> nano compaction. Key takeaway: fidelity-sensitive rendering should happen before lossy compaction, or keep direct access to the richer tree.

- **Tooling note:** Validation confirmed description/autocomplete-style metadata now survives. Also, when appending Markdown via PowerShell here-strings, avoid unescaped backticks or use escape-aware construction.

- **Timeout-skippable LLM proxy test:** Updated `UniversalProxyParserTest.kt` so `testParseuniversalproxyWithNonStandardProxy()` catches `ChatModelException` and converts known timeout messages into a JUnit assumption skip, while rethrowing all other failures. Key takeaway: guard only the known remote-timeout path; don’t weaken the whole test.

- **Surefire/E2E behavior confirmed:** A focused `UniversalProxyParserTest` run executed 0 tests until rerun with `-DrunE2ETests=true`, then ran and skipped locally due to missing LLM config. Key takeaway: `@Tag("RequiresServer")` tests require `-DrunE2ETests=true` even in focused Surefire runs.

- **Coworker memory policy preserved:** Appended daily memory without editing prior monthly history; confirmed `coworker\tasks\300logs\2026\03\MEMORY.202603.md` already contained the `2026-03-13` rollup. Key takeaway: verify first, then append; never rewrite earlier month entries.

- **Kotlin aria snapshot cursor formatting aligned with TypeScript:** Updated `AriaSnapshotFormatting.kt` so `[cursor=pointer]` is emitted only for the highest rendered ref in a pointer-bearing subtree, matching `ariaSnapshot.ts`. Added a regression in `DOMStateBuilderTest.kt`. Key takeaway: matching Playwright aria snapshot output requires recursive formatting state; ancestor-emitted cursor markers must suppress descendant duplicates.

- **Test wrappers updated for current modules:** Updated `bin/test.ps1`, `bin/test.sh`, and `bin/README.md` to remove deleted `kotlin-sdk`/`python-sdk` targets, keep `nodejs-sdk` mapped to `sdks/browser4-cli`, and fail clearly on removed SDK modes. Also hardened wrappers with `-P=-examples` so test runs avoid the removed `browser4/browser4-spa` module. Validation showed wrapper commands succeeded, `nodejs-sdk` reached the right package and failed only on an existing assertion, and `bash -n ./bin/test.sh` passed after restoring LF line endings. Key takeaway: wrapper validation should separate wrapper regressions from existing package-test failures and Windows Bash CRLF quirks.


- **Aria snapshot renderer E2E coverage added:** Added `AriaSnapshotRendererE2ETest` in `pulsar-tests\pulsar-it-tests` to validate Playwright-style aria snapshot behavior against a real Spring-served page plus the existing `assets/frames/nested-frames.html` fixture. The test injects focused DOM cases for generic collapsing, nested pointer cursors, presentational-wrapper descendants, textbox placeholder rendering, and titled generic nodes while keeping iframe coverage limited to renderer-relevant output. Key takeaway: for Browser4 aria snapshot parity work, use a real server-backed page and normalize dynamic refs in assertions instead of comparing brittle raw backend ids or full indentation-sensitive blocks.

- **AriaSnapshotRenderer parity fixes:** Updated `AriaSnapshotRenderer` to collapse unnamed generic wrappers with a single rendered child node, suppress high-noise default AX props (`focusable`, `focused`, `editable`, `settable`, and false-valued invalid/multiline/readonly/required), derive cursor-pointer hints from actual snapshot/inline-style pointer signals instead of generic interactability when snapshot data exists, and make generic titled nodes promote AX descriptions into the rendered name fallback when no real name is present. Key takeaway: Playwright-style aria snapshot fidelity depends on filtering Chrome AX noise and preserving semantically meaningful fallbacks (like title-as-name) while avoiding ref-preserving wrappers that distort the rendered tree.

- **Validation path confirmed again:** `DOMStateBuilderTest` passed after the renderer changes, then `pulsar-browser` had to be installed locally before the focused `AriaSnapshotRendererE2ETest` run in `pulsar-it-tests` could validate against the updated artifact. Key takeaway: for cross-module Browser4 DOM/aria work, the reliable loop is `pulsar-browser` unit test -> local install -> focused `pulsar-it-tests` E2E with `-DrunE2ETests=true`.
