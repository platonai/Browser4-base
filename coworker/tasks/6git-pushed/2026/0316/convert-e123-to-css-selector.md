# Convert e123 format selector to CSS selector in PulsarWebDriver

## Problem

WebDriver provides a way to locate elements using various strategies, including:

- backend node id
- CSS selectors

Backend node id can be in two formats:

- e123 format: `e123`
- backend node id format: `backend:123`

Some methods in PulsarWebDriver, such as `waitForScrollSettled` didn't handle the e123 format correctly, leading to
errors when trying to locate elements.

## Solution

Convert the e123 format selector to the CSS selector if the selector is used in JavaScript context, for example,
in `waitForScrollSettled` method. This involves checking if the selector starts with 'e' followed by digits, and if so,
converting it to the corresponding CSS selector format.

```shell
val backendNodeId = "e123".substring(1) // Extract the numeric part
val ref = ElementRefCriteria(backendNodeId = "123")
selector = snapshotService.findElement(ref).cssSelector()

// now selector can be used in JavaScript context to locate the element correctly
```
