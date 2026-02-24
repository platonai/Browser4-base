# WebDriver Improvement

涉及类：

`ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver`

同步修改 OpenAPI 定义和后端实现, SDK 定义和测试。

## dblclick

Perform a double-click action on the element identified by the selector.

```kotlin
driver.dblclick("#button")
driver.dblclick("e123")
```

## dialog

Handle JavaScript dialogs (alert, confirm, prompt) by accepting or dismissing them, optionally providing input for prompt dialogs.

```kotlin
driver.dialogAccept()
driver.dialogDismiss()
driver.dialogAccept("confirmation text")
```

## resize

Resize the browser window to the specified width and height.

```kotlin
driver.resize(1920, 1080)
```

