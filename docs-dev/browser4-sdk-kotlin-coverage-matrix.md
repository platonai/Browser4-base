# Browser4 Kotlin SDK Coverage Matrix (Integration Tests)

Scope: sdks/kotlin-sdk-tests/src/test/kotlin/ai/platon/pulsar/sdk/integration
Target SDK: sdks/browser4-sdk-kotlin/src/main/kotlin/ai/platon/pulsar/sdk/v0

Legend:
- Yes: covered by a test in the integration suite
- Partial: covered in limited or indirect paths
- No: no coverage found in the integration suite

## PulsarSession

Source: sdks/browser4-sdk-kotlin/src/main/kotlin/ai/platon/pulsar/sdk/v0/PulsarSession.kt

| Method | Coverage | Tests | Notes |
| --- | --- | --- | --- |
| normalize(url, args, toItemOption) | Partial | PulsarSessionIntegrationTest, ErrorHandlingAndEdgeCasesTest | default args and basic inputs only |
| normalizeOrNull(url, args, toItemOption) | Yes | ErrorHandlingAndEdgeCasesTest | null/blank inputs |
| open(url, args) | Yes | PulsarSessionIntegrationTest, FusedActsStyleTest | basic path |
| open(url, eventHandlers, args) | No | - | no event handler tests |
| load(url, args) | Yes | PulsarSessionIntegrationTest, ErrorHandlingAndEdgeCasesTest | basic path |
| loadAll(urls, args) | Yes | PulsarSessionIntegrationTest | basic path |
| submit(url, args) | Yes | PulsarSessionIntegrationTest | positive path only |
| submitAll(urls, args) | Yes | PulsarSessionIntegrationTest | positive path only |
| parse(page) | Yes | PulsarSessionIntegrationTest, FusedActsStyleTest | basic path |
| extract(document, map) | Yes | PulsarSessionIntegrationTest, FusedActsStyleTest | limited selectors |
| extract(document, selectors) | No | - | no direct test |
| scrape(url, args, selectors) | Yes | PulsarSessionIntegrationTest | basic path |
| chat(prompt) | No | - | no tests |
| chat(userMessage, systemMessage) | No | - | no tests |
| getOrCreateBoundDriver() | Partial | FusedActsStyleTest | existence only |
| driver/boundDriver/isActive/uuid | Yes | PulsarSessionIntegrationTest, FusedActsStyleTest, ErrorHandlingAndEdgeCasesTest | state and binding |

## WebDriver

Source: sdks/browser4-sdk-kotlin/src/main/kotlin/ai/platon/pulsar/sdk/v0/WebDriver.kt

| Method | Coverage | Tests | Notes |
| --- | --- | --- | --- |
| open(url) | Partial | WebDriverIntegrationTest | indirect via navigateTo |
| navigateTo(url) | Yes | WebDriverIntegrationTest | |
| currentUrl() | Yes | WebDriverIntegrationTest, ErrorHandlingAndEdgeCasesTest | |
| getCurrentUrl() | No | - | |
| url() | No | - | |
| documentUri() | No | - | |
| baseUri() | No | - | |
| title() | Yes | WebDriverIntegrationTest | |
| pageSource() | Yes | WebDriverIntegrationTest | |
| exists(selector, strategy) | Yes | Multiple | |
| isVisible(selector) | No | - | |
| isHidden(selector) | No | - | |
| isChecked(selector) | No | - | |
| waitForSelector(selector, strategy, timeout) | Yes | WebDriverIntegrationTest | |
| waitFor(selector, strategy, timeout) | No | - | |
| waitForNavigation(oldUrl, timeout) | Partial | WebDriverIntegrationTest | called without verification |
| findElementBySelector(selector, strategy) | No | - | |
| findElementsBySelector(selector, strategy) | No | - | |
| findElement(using, value) | No | - | |
| findElements(using, value) | No | - | |
| click(selector, count, strategy) | Yes | WebDriverClickAndAttributeTest | |
| clickElement(elementId) | No | - | |
| hover(selector) | Yes | WebDriverIntegrationTest | |
| focus(selector) | Yes | WebDriverKeyboardAndFocusTest | |
| blur(selector) | No | - | |
| fill(selector, value) | Yes | WebDriverIntegrationTest | |
| type(selector, value) | Yes | WebDriverIntegrationTest, WebDriverKeyboardAndFocusTest | |
| sendKeys(selector, value) | Yes | WebDriverKeyboardAndFocusTest | |
| press(selector, key) | Yes | WebDriverKeyboardAndFocusTest | |
| scrollToTop() | Yes | WebDriverIntegrationTest | |
| scrollToBottom() | Yes | WebDriverIntegrationTest | |
| scrollToMiddle(ratio) | Yes | WebDriverIntegrationTest | |
| scrollBy(x, y, smooth) | Yes | WebDriverIntegrationTest | |
| captureScreenshot() | Yes | WebDriverIntegrationTest | |
| executeScript(script) | Yes | WebDriverIntegrationTest | |
| selectFirstTextOrNull(selector) | Yes | Multiple | |
| selectTextAll(selector) | Partial | ErrorHandlingAndEdgeCasesTest | empty result only |
| selectFirstAttributeOrNull(selector, attr) | Yes | WebDriverClickAndAttributeTest | |
| selectAttributeAll(selector, attr) | Yes | WebDriverClickAndAttributeTest | |
| getAttribute(selector, attr) | No | - | |
| goBack() | Yes | WebDriverIntegrationTest | |
| goForward() | Yes | WebDriverIntegrationTest | |
| reload() | Yes | WebDriverIntegrationTest | |

## AgenticSession

Source: sdks/browser4-sdk-kotlin/src/main/kotlin/ai/platon/pulsar/sdk/v0/AgenticSession.kt

| Method/Capability | Coverage | Tests | Notes |
| --- | --- | --- | --- |
| getOrCreate(baseUrl, useLocalDriver) | Yes | FusedActsStyleTest | |
| create(baseUrl, useLocalDriver) | Yes | FusedActsStyleTest | |
| resetDefault() | Yes | FusedActsStyleTest | |
| act(...) / agentAct(...) | Yes | AgenticSessionIntegrationTest, FusedActsStyleTest | parameter variants not tested |
| run(...) / agentRun(...) | Yes | AgenticSessionIntegrationTest, FusedActsStyleTest | parameter variants not tested |
| observe(...) / agentObserve(...) | Yes | AgenticSessionIntegrationTest | parameter variants not tested |
| agentExtract(...) | Yes | AgenticSessionIntegrationTest | |
| summarize(...) | Yes | AgenticSessionIntegrationTest | |
| agentEventHandlers | No | - | |
| stateHistory/processTrace | Partial | FusedActsStyleTest | existence/clear only |

## AgenticContexts / PerceptiveAgent / Models

Sources:
- sdks/browser4-sdk-kotlin/src/main/kotlin/ai/platon/pulsar/sdk/v0/AgenticContexts.kt
- sdks/browser4-sdk-kotlin/src/main/kotlin/ai/platon/pulsar/sdk/v0/PerceptiveAgent.kt
- sdks/browser4-sdk-kotlin/src/main/kotlin/ai/platon/pulsar/sdk/v0/Models.kt

| Module | Coverage | Notes |
| --- | --- | --- |
| AgenticContexts | No | no direct tests |
| PerceptiveAgent interface | Partial | via AgenticSession implementations |
| Models DTOs | No | no direct parsing/edge tests |
