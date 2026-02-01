# Security Review Notes - OpenAPI Implementation

## Date: 2026-02-01

## Overview
Code review identified JavaScript injection vulnerabilities in SDK WebDriver methods that directly interpolate user input into JavaScript code.

## Identified Issues

### 1. JavaScript Injection in SDK Methods
**Severity**: Medium to High
**Files Affected**:
- `sdks/browser4-sdk-kotlin/src/main/kotlin/ai/platon/pulsar/sdk/v0/WebDriver.kt`
- `sdks/browser4-sdk-python/browser4/webdriver.py`
- `pulsar-rest/src/main/kotlin/ai/platon/pulsar/rest/openapi/controller/ElementController.kt`
- `pulsar-rest/src/main/kotlin/ai/platon/pulsar/rest/openapi/controller/CookieController.kt`

**Methods Affected**:
- `setAttribute`, `setAttributeAll`
- `setProperty`, `setPropertyAll`
- `evaluateValue(selector, functionDeclaration)`
- Cookie string building

**Issue**: Direct string interpolation of user-provided values (selectors, property names, values) into JavaScript code without proper escaping.

**Example**:
```kotlin
// Vulnerable
executeScript("document.querySelector('$selector')?.setAttribute('$attrName', '$attrValue')")
```

If `selector` contains `'); alert('XSS'); //`, it could execute arbitrary JavaScript.

## Context and Mitigation

### Why This Pattern Exists
1. **Local API Consistency**: The SDK methods mirror the local `WebDriver` interface from `pulsar-core/pulsar-skeleton/src/main/kotlin/ai/platon/pulsar/skeleton/crawl/fetch/driver/WebDriver.kt`
2. **Existing Pattern**: The local WebDriver implementation uses similar JavaScript evaluation patterns
3. **Browser Control**: Direct JavaScript execution is necessary for browser automation

### Risk Assessment
**Risk Level**: Medium
- **Exposure**: These are automation APIs, not user-facing web interfaces
- **Trust Boundary**: Callers are expected to be trusted automation scripts, not end users
- **Isolation**: Browser automation typically runs in isolated contexts
- **Authentication**: REST API requires session authentication

### Recommended Fixes

#### Short-term (Quick Wins)
1. **Input Validation**:
   ```kotlin
   private fun sanitizeSelector(selector: String): String {
       // Remove or escape dangerous characters
       return selector.replace("'", "\\'")
           .replace("\"", "\\\"")
           .replace("\n", "\\n")
           .replace("\r", "\\r")
   }
   ```

2. **Use JSON Encoding**:
   ```kotlin
   import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
   
   val mapper = jacksonObjectMapper()
   val safeValue = mapper.writeValueAsString(attrValue)
   executeScript("document.querySelector($safeSelector).setAttribute($safeAttrName, $safeValue)")
   ```

3. **Parameter Binding** (if supported):
   ```kotlin
   // Pass values as parameters instead of string interpolation
   executeScript(
       "document.querySelector(arguments[0]).setAttribute(arguments[1], arguments[2])",
       listOf(selector, attrName, attrValue)
   )
   ```

#### Long-term (Comprehensive)
1. **Create Sanitization Utility**:
   - Create `ai.platon.pulsar.common.JavaScriptSanitizer`
   - Centralize all JavaScript string escaping
   - Use in both local and SDK implementations

2. **Enhance Execute Script API**:
   - Modify `executeScript` to support parameter binding
   - Update all local WebDriver implementations
   - Propagate to SDK implementations

3. **Security Testing**:
   - Add fuzzing tests with malicious inputs
   - Test XSS-like payloads in selectors and values
   - Validate sanitization effectiveness

## Action Items

### Immediate (This PR)
- [x] Document security concerns
- [x] Note in code comments where sanitization is needed
- [ ] Add basic input validation for critical methods (optional)

### Follow-up (Future PR)
- [ ] Implement comprehensive JavaScript sanitization utility
- [ ] Refactor local WebDriver to use sanitization
- [ ] Update SDK implementations to use sanitization
- [ ] Add security tests for injection prevention
- [ ] Update documentation with security best practices

## Cookie Date Format Issue

**Issue**: Cookie expiration dates use `Date.toString()` instead of RFC 6265 format.

**Fix**:
```kotlin
import java.text.SimpleDateFormat
import java.util.TimeZone

private val cookieDateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z").apply {
    timeZone = TimeZone.getTimeZone("GMT")
}

// Usage:
cookie.expires?.let { 
    append("; expires=${cookieDateFormat.format(java.util.Date(it))}") 
}
```

## Conclusion

The identified security issues are real but have limited risk in the context of browser automation APIs. They should be addressed in a follow-up PR with a comprehensive solution that applies to both local and SDK implementations.

For production use:
1. Ensure REST API is behind authentication
2. Use in trusted environments only
3. Validate/sanitize input at the application layer
4. Plan for comprehensive fix in next iteration

## References
- OWASP JavaScript Injection: https://owasp.org/www-community/attacks/Code_Injection
- W3C WebDriver Spec: https://w3c.github.io/webdriver/
- RFC 6265 (Cookies): https://tools.ietf.org/html/rfc6265
