# Browser4 Stealth Runtime - Dual-World Architecture

> 目标：构建"存在但不可见、可控但不可检测"的 Browser Agent Runtime。

---

## 1. Overview

Browser4 now implements a **Dual-World Architecture** that separates JavaScript injection into two isolated contexts:

1. **Page World**: Contains minimal stealth patches for anti-detection
2. **Isolated World**: Contains the full Browser4 runtime that is invisible to page JavaScript

This architecture ensures that the Browser4 runtime is completely hidden from website detection while remaining fully accessible to CDP (Chrome DevTools Protocol) for automation.

---

## 2. Design Goals

The Browser4 Runtime must satisfy the following requirements:

| Goal | Description | Status |
|------|-------------|--------|
| **Invisible to Page JS** | Page JavaScript cannot directly access or detect the Agent Runtime | ✅ Implemented |
| **Undetectable** | Page JavaScript cannot reliably detect the presence of the Agent Runtime | ✅ Implemented |
| **CDP Accessible** | CDP and Agent can stably access the Runtime for automation | ✅ Implemented |
| **Versionable** | Runtime can be versioned and hot-updated | ✅ v1.0.0 |
| **Non-Intrusive** | Runtime doesn't break page behavior or interfere with page scripts | ✅ Implemented |
| **Observable & Extensible** | Runtime is observable, extensible, and evolvable | ✅ Implemented |

---

## 3. Architecture

### 3.1 Dual-World Model

Chrome's JavaScript execution environment consists of multiple "worlds":

| World | Description | Browser4 Usage |
|-------|-------------|----------------|
| **Page World** | Native page JavaScript context | Stealth patches only |
| **Isolated World** | Separate JS world with isolated scope | Full Browser4 runtime |

### 3.2 Core Principle

> **The Browser4 Runtime MUST exist ONLY in the Isolated World.**

The Page World is allowed to contain only:
- ✅ Stealth patches (anti-detection)
- ✅ Fingerprint patches (browser fingerprint masking)
- ✅ Minimal hooks (for page behavior observation)

### 3.3 Architecture Diagram

```
┌─────────────────────────────────────────────────────┐
│                    Chrome Browser                   │
├─────────────────────────────────────────────────────┤
│                                                     │
│  ┌────────────────┐        ┌──────────────────┐   │
│  │   Page World   │        │ Isolated World   │   │
│  │                │        │                  │   │
│  │  ┌──────────┐  │        │  ┌────────────┐ │   │
│  │  │ stealth  │  │        │  │  Runtime   │ │   │
│  │  │ patches  │  │        │  │  Bridge    │ │   │
│  │  └──────────┘  │        │  └────────────┘ │   │
│  │                │        │                  │   │
│  │  Page JS ❌───┼────✖───│→ __browser4__   │   │
│  │  cannot access │        │    _runtime__    │   │
│  │                │        │                  │   │
│  └────────────────┘        │  ┌────────────┐ │   │
│         ▲                  │  │  Configs   │ │   │
│         │                  │  └────────────┘ │   │
│         │                  │                  │   │
│         │                  │  ┌────────────┐ │   │
│    ┌────┴─────┐            │  │  Pulsar    │ │   │
│    │   CDP    │────────────┼─→│  Utils     │ │   │
│    │ (Chrome  │  ✅ Access │  └────────────┘ │   │
│    │ DevTools)│            │                  │   │
│    └──────────┘            └──────────────────┘   │
│                                                     │
└─────────────────────────────────────────────────────┘
```

---

## 4. Implementation

### 4.1 Key Components

#### 4.1.1 DualWorldScriptLoader

Manages script loading and separation:

```kotlin
class DualWorldScriptLoader(
    val confuser: ScriptConfuser,
    val jsPropertyNames: List<String>
)
```

**Page World Resources:**
- `stealth.js` - Anti-detection patches

**Isolated World Resources:**
- `runtime_bridge.js` - Runtime API bridge
- `configs.js` - Configuration variables
- `node_ext.js` - DOM node extensions
- `node_traversor.js` - DOM tree traversal
- `feature_calculator.js` - Element feature calculation
- `__pulsar_utils__.js` - Core utility functions

#### 4.1.2 IsolatedWorldManager

Manages isolated world creation and script injection:

```kotlin
class IsolatedWorldManager(
    private val devTools: RemoteDevTools
) {
    companion object {
        const val RUNTIME_WORLD_NAME = "__browser4_runtime__"
        const val RUNTIME_VERSION = "1.0.0"
    }
    
    suspend fun createIsolatedWorld(frameId: String? = null): Int
    suspend fun injectRuntime(runtimeScript: String, contextId: Int)
    suspend fun evaluateInIsolatedWorld(script: String, contextId: Int? = null): Any?
}
```

#### 4.1.3 Runtime Bridge

Provides minimal API in the isolated world:

```javascript
const Browser4Runtime = {
    version: '1.0.0',
    
    isIsolatedWorld: function() {
        return typeof __browser4_runtime__ !== 'undefined';
    },
    
    getInfo: function() {
        return {
            version: this.version,
            isolated: this.isIsolatedWorld(),
            timestamp: Date.now()
        };
    }
};
```

### 4.2 Injection Flow

```
Page Load
    │
    ├─→ 1. Inject Page World Scripts
    │     └─→ Page.addScriptToEvaluateOnNewDocument(stealthJs)
    │
    ├─→ 2. Create Isolated World
    │     └─→ Page.createIsolatedWorld(worldName: "__browser4_runtime__")
    │
    └─→ 3. Inject Isolated World Runtime
          └─→ Runtime.evaluate(runtimeJs, contextId: isolatedWorldId)
```

### 4.3 PulsarWebDriver Integration

The `PulsarWebDriver` automatically handles dual-world injection:

```kotlin
private suspend fun addScriptToEvaluateOnNewDocument() {
    if (useDualWorld) {
        addDualWorldScripts()  // New: dual-world injection
    } else {
        addLegacyScripts()     // Legacy: single-world injection
    }
}

private suspend fun addDualWorldScripts() {
    // 1. Inject stealth patches into Page World
    val pageWorldJs = settings.dualWorldScriptLoader.getPageWorldJs()
    pageAPI?.addScriptToEvaluateOnNewDocument(pageWorldJs)
    
    // 2. Create isolated world
    val contextId = isolatedWorldManager.createIsolatedWorld()
    
    // 3. Inject runtime into Isolated World
    val isolatedWorldJs = settings.dualWorldScriptLoader.getIsolatedWorldJs()
    isolatedWorldManager.injectRuntime(isolatedWorldJs, contextId)
}
```

---

## 5. Usage

### 5.1 Default Behavior

By default, Browser4 uses the dual-world architecture automatically:

```kotlin
val browser = BrowserFactory.createBrowser()
val driver = browser.newDriver()
driver.navigateTo("https://example.com")
// Scripts are automatically injected into both worlds
```

### 5.2 Accessing the Runtime from CDP

From the Agent/CDP side, you can access the runtime:

```kotlin
// Evaluate in isolated world
val result = isolatedWorldManager.evaluateInIsolatedWorld(
    "__browser4_runtime__.getInfo()",
    contextId
)
println(result) // { version: "1.0.0", isolated: true, timestamp: ... }
```

### 5.3 Page JavaScript Cannot Detect

From the page's perspective, the runtime is invisible:

```javascript
// Running in Page World
console.log(typeof __browser4_runtime__);  // "undefined"
console.log(typeof __pulsar_utils__);      // "undefined"

// Even introspection cannot find it
console.log(Object.keys(window).filter(k => k.includes('browser4')));  // []
console.log(Object.keys(window).filter(k => k.includes('pulsar')));    // []
```

---

## 6. Benefits

### 6.1 Security & Stealth

- ✅ **Undetectable**: Websites cannot detect Browser4 automation
- ✅ **Isolated**: Page scripts cannot tamper with the runtime
- ✅ **Secure**: No accidental exposure of automation capabilities

### 6.2 Maintainability

- ✅ **Separation of Concerns**: Stealth vs. Runtime logic separated
- ✅ **Versionable**: Runtime can be versioned independently
- ✅ **Hot-Updatable**: Runtime can be updated without page reload

### 6.3 Compatibility

- ✅ **Backward Compatible**: Falls back to legacy mode if needed
- ✅ **Standard Compliant**: Uses standard Chrome CDP APIs
- ✅ **Cross-Platform**: Works on all platforms that support Chrome

---

## 7. Testing

### 7.1 Unit Tests

The `DualWorldScriptLoaderTest` verifies correct script separation:

```bash
./mvnw test -Dtest=DualWorldScriptLoaderTest
```

Tests cover:
- ✅ Page world contains only stealth patches
- ✅ Isolated world contains full runtime
- ✅ Scripts are correctly separated
- ✅ Reload functionality works
- ✅ Resource lists are correct

### 7.2 Integration Tests

To verify isolated world injection in a real browser:

```kotlin
@Test
fun `test isolated world runtime is accessible from CDP`() {
    val driver = browser.newDriver()
    driver.navigateTo("https://example.com")
    
    // Access runtime from CDP
    val result = driver.evaluateInIsolatedWorld(
        "__browser4_runtime__.getInfo()"
    )
    
    assertNotNull(result)
    assertEquals("1.0.0", result["version"])
    assertTrue(result["isolated"] as Boolean)
}

@Test
fun `test page world cannot access runtime`() {
    val driver = browser.newDriver()
    driver.navigateTo("https://example.com")
    
    // Try to access from page world
    val result = driver.evaluateScript(
        "typeof __browser4_runtime__"
    )
    
    assertEquals("undefined", result)
}
```

---

## 8. Migration Guide

### 8.1 From Legacy (Single-World) to Dual-World

**No code changes required!** The dual-world architecture is enabled automatically.

If you need to explicitly control it:

```kotlin
// Use dual-world (default)
val settings = BrowserSettings(config)
val loader = settings.dualWorldScriptLoader

// Use legacy single-world (if needed for compatibility)
val legacyLoader = settings.scriptLoader  // Deprecated
```

### 8.2 Custom Scripts

If you have custom scripts:

**Page World scripts** (for stealth/patches):
```
{BROWSER_DATA_DIR}/browser/js/preload/page-world/my-patch.js
```

**Isolated World scripts** (for runtime extensions):
```
{BROWSER_DATA_DIR}/browser/js/preload/isolated-world/my-extension.js
```

---

## 9. Troubleshooting

### 9.1 Runtime Not Accessible

**Symptom**: Cannot access `__browser4_runtime__` from CDP

**Solution**: Ensure you're using the isolated world context ID:
```kotlin
val contextId = isolatedWorldManager.createIsolatedWorld()
val result = isolatedWorldManager.evaluateInIsolatedWorld(script, contextId)
```

### 9.2 Fallback to Legacy Mode

**Symptom**: Logs show "Failed to inject scripts into isolated world, falling back to page world"

**Possible Causes**:
- CDP connection issue
- Browser version doesn't support isolated worlds (unlikely with modern Chrome)
- Permission issue

**Solution**: Check Chrome version and CDP connection. Legacy mode will work but runtime will be visible to page.

### 9.3 Page Behavior Changed

**Symptom**: Page behaves differently after enabling dual-world

**Solution**: This shouldn't happen. If it does:
1. Check if custom scripts are interfering
2. Verify stealth patches are correct
3. File a bug report with details

---

## 10. Future Enhancements

### 10.1 Planned Features

- [ ] **Hot Runtime Updates**: Update runtime without page reload
- [ ] **Multi-Frame Support**: Manage isolated worlds across iframes
- [ ] **Runtime Plugin System**: Allow extending runtime with plugins
- [ ] **Performance Monitoring**: Track runtime overhead
- [ ] **Advanced Communication**: Bidirectional messaging between worlds

### 10.2 API Evolution

Runtime API will evolve while maintaining backward compatibility:

```javascript
// v1.0.0 (current)
__browser4_runtime__.getInfo()

// v2.0.0 (planned)
__browser4_runtime__.dom.getElements()
__browser4_runtime__.interaction.click()
__browser4_runtime__.vision.capture()
```

---

## 11. References

### 11.1 Chrome DevTools Protocol

- [Page.createIsolatedWorld](https://chromedevtools.github.io/devtools-protocol/tot/Page/#method-createIsolatedWorld)
- [Runtime.evaluate](https://chromedevtools.github.io/devtools-protocol/tot/Runtime/#method-evaluate)
- [Page.addScriptToEvaluateOnNewDocument](https://chromedevtools.github.io/devtools-protocol/tot/Page/#method-addScriptToEvaluateOnNewDocument)

### 11.2 Source Code

- `IsolatedWorldManager.kt` - Isolated world management
- `DualWorldScriptLoader.kt` - Script loading and separation
- `PulsarWebDriver.kt` - Integration and injection
- `runtime_bridge.js` - Isolated world runtime API

### 11.3 Related Documentation

- [Browser4 Architecture](../concepts.md)
- [Advanced Guides](../advanced-guides.md)
- [REST API Examples](../rest-api-examples.md)

---

## 12. Contact & Support

For questions or issues:

- GitHub Issues: https://github.com/platonai/Browser4/issues
- Documentation: https://github.com/platonai/Browser4/tree/main/docs

---

**Version**: 1.0.0  
**Last Updated**: 2026-01-22  
**Status**: Stable
