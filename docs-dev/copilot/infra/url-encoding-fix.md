# URL Encoding Fix in WebDriver SDK Clients

## Problem

When calling `driver.sendKeys("#username", "Hello")` in the test `testShouldUseSendKeysWithSpecialKeys`, the generated URL was:

```
/session/0ee8ac05-5a70-4430-a2a3-22ef6c7ed011/element/#username/value
```

The `#` character in the URL path is problematic because:
1. In URLs, `#` is the fragment identifier and everything after it is not sent to the server
2. The selector `#username` was being treated as an elementId and inserted directly into the URL path
3. This caused the URL to be interpreted incorrectly

## Solution

We implemented two complementary fixes:

### 1. Changed `sendKeys()` API (Breaking Change)

**Before:**
```kotlin
suspend fun sendKeys(elementId: String, text: String): Any? {
    return client.post(
        "/session/{sessionId}/element/$elementId/value",
        mapOf("text" to text)
    )
}
```

**After:**
```kotlin
suspend fun sendKeys(selector: String, text: String, strategy: String = "css"): Any? {
    return fill(selector, text, strategy)
}
```

**Rationale:**
- The test was calling `sendKeys` with a CSS selector, not an elementId
- Other similar methods like `type()` and `fill()` already accept selectors
- Using the selector-based endpoint `/session/{sessionId}/selectors/fill` is more consistent
- Avoids the URL encoding issue entirely for this method

### 2. Added URL Encoding for Element-Based Methods

For methods that legitimately use `elementId` in the URL path, we added proper URL encoding:

```kotlin
private fun encodePathSegment(value: String): String {
    return URLEncoder.encode(value, StandardCharsets.UTF_8)
}
```

**Methods Fixed:**
- `clickElement(elementId)` - encodes elementId
- `getAttribute(elementId, name)` - encodes both elementId and attribute name
- `getText(elementId)` - encodes elementId

**Example:**
```kotlin
// Before (incorrect)
client.get("/session/{sessionId}/element/$elementId/text")

// After (correct)
client.get("/session/{sessionId}/element/${encodePathSegment(elementId)}/text")
```

### 3. Applied Same Fixes to Python SDK

The Python SDK had identical issues and received the same fixes:

```python
@staticmethod
def _encode_path_segment(value: str) -> str:
    """URL-encode a string for safe use in URL paths."""
    return quote(value, safe='')
```

## Verification

### URL Encoding Test Results

```
Original selector: #username
URL encoded:       %23username

✅ PASS: '#' character properly encoded as '%23'

Before fix (WRONG):
  /session/0ee8ac05-5a70-4430-a2a3-22ef6c7ed011/element/#username/value
  Problem: '#' in URL becomes a fragment identifier

After fix (CORRECT):
  /session/0ee8ac05-5a70-4430-a2a3-22ef6c7ed011/element/%23username/value
  Result: Properly encoded URL path
```

### Test Coverage

- ✅ SDK unit tests pass
- ✅ URL encoding verification tests added
- ✅ Verified no other similar issues in codebase

## Impact

### Breaking Changes

The `sendKeys()` method signature changed in both Kotlin and Python SDKs:

**Kotlin:**
- Old: `sendKeys(elementId: String, text: String)`
- New: `sendKeys(selector: String, text: String, strategy: String = "css")`

**Python:**
- Old: `send_keys(element_id: str, text: str)`
- New: `send_keys(selector: str, text: str, strategy: str = "css")`

### Migration Guide

If you were using `sendKeys` with elementIds from `findElement()`, you now have two options:

1. **Use selector-based approach (recommended):**
```kotlin
// Instead of:
val element = driver.findElement("css selector", "#username")
driver.sendKeys(element["elementId"], "text")

// Use:
driver.sendKeys("#username", "text")
```

2. **Use fill() with selector:**
```kotlin
driver.fill("#username", "text")  // Same as sendKeys now
```

### Other Fixed Methods

These methods continue to work with elementIds but now properly encode them:
- `clickElement(elementId)`
- `getAttribute(elementId, name)`
- `getText(elementId)`

## Files Changed

1. `sdks/browser4-kotlin/src/main/kotlin/ai/platon/pulsar/sdk/v0/WebDriver.kt`
   - Added `URLEncoder` import
   - Added `encodePathSegment()` helper method
   - Changed `sendKeys()` to accept selector instead of elementId
   - Fixed `clickElement()`, `getAttribute()`, `getText()` to use URL encoding

2. `sdks/python-sdk/browser4/webdriver.py`
   - Added `urllib.parse.quote` import
   - Added `_encode_path_segment()` helper method
   - Changed `send_keys()` to accept selector instead of element_id
   - Fixed `click_element()`, `get_attribute()`, `get_text()` to use URL encoding

3. `sdks/browser4-kotlin/src/test/kotlin/ai/platon/pulsar/sdk/UrlEncodingTest.kt` (new)
   - Comprehensive URL encoding verification tests

## Related Issues

This fix addresses the issue where special characters in CSS selectors (like `#`, `[`, `]`, etc.) were not properly URL-encoded when used in URL paths, causing incorrect behavior or API errors.

## Testing

To verify the fix works:

```bash
# Build the SDK
./mvnw -Psdk -pl sdks/browser4-kotlin clean install -DskipTests

# Run URL encoding tests
./mvnw -Psdk -pl sdks/browser4-kotlin test -Dtest=UrlEncodingTest
```
