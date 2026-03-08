# WebDriver Tool Alignment

Align the WebDriver tools defined in the CLI specification with the current implementation. Every tool declared in the
CLI must be implemented, correctly wired through the backend call chain, and verified to work as expected.

## Goal

Ensure that each command in `supportedCommandsArray` is:

- declared in the CLI specification,
- implemented in the WebDriver layer,
- routed through the expected executor and controller path, and
- available end-to-end without mismatches in naming or behavior.

## Validation Checklist

All tools declared in `supportedCommandsArray` should map cleanly through the following call stack:

```text
WebDriver / PulsarWebDriver
-> WebDriverToolExecutor / BrowserToolExecutor
-> MCPToolController
-> commands.ts
```

Verify the following for every listed tool:

1. The command exists in `commands.ts`.
2. The corresponding WebDriver capability is implemented.
3. The executor layer exposes and dispatches the tool correctly.
4. The MCP controller can invoke it through the expected path.
5. Naming, parameters, and behavior stay consistent across all layers.

## Tools to Align

```ts
const supportedCommandsArray: AnyCommandSchema[] = [
    // core category
    open,
    close,
    goto,
    type,
    click,
    doubleClick,
    fill,
    drag,
    hover,
    select,
    fileUpload,
    check,
    uncheck,
    snapshot,
    evaluate,
    consoleList,
    dialogAccept,
    dialogDismiss,
    resize,
    deleteData,

    // navigation category
    goBack,
    goForward,
    reload,

    // keyboard category
    pressKey,
    keydown,
    keyup,

    // mouse category
    mouseMove,
    mouseDown,
    mouseUp,
    mouseWheel,

    // export category
    screenshot,
    pdfSave,

    // tabs category
    tabList,
    tabNew,
    tabClose,
    tabSelect
]
```

## References

- [commands.ts](../../../sdks/browser4-cli/src/cli/daemon/commands.ts)
- [WebDriver.kt](../../../pulsar-core/pulsar-skeleton/src/main/kotlin/ai/platon/pulsar/skeleton/crawl/fetch/driver/WebDriver.kt)
- [PulsarWebDriver.kt](../../../pulsar-core/pulsar-plugins/pulsar-protocol/src/main/kotlin/ai/platon/pulsar/protocol/browser/driver/cdt/PulsarWebDriver.kt)
- [WebDriverToolExecutor.kt](../../../pulsar-agentic/src/main/kotlin/ai/platon/pulsar/agentic/tools/builtin/WebDriverToolExecutor.kt)
- [BrowserToolExecutor.kt](../../../pulsar-agentic/src/main/kotlin/ai/platon/pulsar/agentic/tools/builtin/BrowserToolExecutor.kt)
