# Daily Memory - 2026-03-16

## 1. Unified Tool Naming (Frontend/Backend)
**Context:** The CLI wrapper previously aliased tool names.
**Change:** Removed aliasing from `sdks/browser4-cli`. Updated `pulsar-rest` `MCPToolController` to directly accept frontend-declared `browser_*` names (e.g., `browser_navigate`, `browser_click`).
**Validation:**
- CLI: `npm test` (program/commands) passed.
- Backend: `MCPToolControllerTest` passed.
- E2E: Focused `pulsar-rest-tests` passed after installing the modified `pulsar-rest` module locally.
**Key Insight:** API compatibility layers belong at the backend boundary, not in CLI wrappers.
**Testing Strategy:** For cross-module E2E, `mvn install` the changed module locally before running `pulsar-tests` with `-DrunE2ETests=true`.

## 2. Log Encoding Fix
**Context:** Windows default charset caused garbled localized logs (e.g., "使用 Web UI").
**Change:** Explicitly added `<charset>UTF-8</charset>` to all file appenders in `pulsar-core` logback configurations (`logback.xml`, `dev`, `prod`).
**Validation:** Verified `pulsar.log` output correctly encodes UTF-8 even when JVM runs with `-Dfile.encoding=GBK`.
**Key Insight:** Never rely on JVM default charset for file logging; explicit configuration is required for robust localization.

## 3. Real CLI E2E Coverage
**Context:** Unit tests missed contract mismatches between CLI and Backend.
**Change:** Added `npm run test:e2e` in `sdks/browser4-cli`. This runs a live `Browser4.jar`, isolates state via `BROWSER4_CLI_STATE_DIR`, and verifies all supported commands. Fixed `eval`, `upload`, `type`, and `press` contracts.
**Validation:** `npm run test:e2e` passed. Guard ensures tests stay aligned with `supportedCommandsArray`.
**Key Insight:**
- Live E2E tests are essential for detecting argument shape and identifier mismatches.
- Explicitly cover known backend gaps (e.g., aliases like `console`/`pdf`) to maintain visibility on feature parity.



## 4. DOMState Aria Snapshot Fallback
**Context:** `DOMStateBuilderTest` failures because `DOMState.ariaSnapshot` returned an empty string when `optimizedDOMTree` was null (default constructor behavior used in tests).
**Change:** Updated `DOMState.ariaSnapshot` in `DomModels.kt` to fallback to `serializableTree.toNanoTreeUnfiltered().ariaSnapshot` when `optimizedDOMTree` is null.
**Validation:** `DOMStateBuilderTest` passed (16 tests).
**Key Insight:** `DOMState` is sometimes constructed directly from `SerializableDOMTreeNode` in tests (and potentially elsewhere) where `OptimizedDOMTree` is not available. The fallback ensures `ariaSnapshot` still works via nano-tree conversion.

## 5. PulsarWebDriver Selector Conversion
**Context:** `PulsarWebDriver.waitForScrollSettled` failed with `e123` format selectors because `document.querySelector` doesn't support them.
**Change:** Implemented `convertSelectorIfNecessary` in `PulsarWebDriver.kt` to convert `e123` format to CSS selector using `SnapshotService`. Applied it in `waitForScrollSettled`.
**Validation:** Compiled `pulsar-protocol`.
**Key Insight:** Methods injecting raw JS with selectors must handle `e123` -> CSS conversion manually, as they bypass `PageHandler`'s resolution logic.
