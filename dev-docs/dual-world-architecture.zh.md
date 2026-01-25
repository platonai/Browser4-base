# Browser4 隐形运行时 - 双世界架构

## 简介

Browser4 现在实现了**双世界架构**（Dual-World Architecture），将 JavaScript 注入分为两个隔离的上下文：

1. **页面世界（Page World）**：只包含最小化的隐形补丁
2. **隔离世界（Isolated World）**：包含完整的 Browser4 运行时，对页面 JavaScript 完全不可见

这种架构确保 Browser4 运行时完全隐藏，无法被网站检测，同时仍可通过 CDP 完全访问。

## 核心原则

> **Browser4 运行时必须只存在于隔离世界中。**

页面世界只允许包含：
- ✅ 隐形补丁（stealth patches）
- ✅ 指纹补丁（fingerprint patches）
- ✅ 最小化钩子（minimal hooks）

## 架构图

```
┌──────────────────────────────────┐
│        Chrome 浏览器              │
├──────────────────────────────────┤
│                                  │
│  ┌─────────┐      ┌──────────┐  │
│  │页面世界  │      │ 隔离世界  │  │
│  │         │      │           │  │
│  │stealth  │      │ Runtime   │  │
│  │patches  │      │ Bridge    │  │
│  │         │      │           │  │
│  │页面 JS  │──✖──→│__browser4 │  │
│  │无法访问  │      │_runtime__ │  │
│  └─────────┘      └──────────┘  │
│       ▲                 ▲        │
│       │                 │        │
│       └────── CDP ──────┘        │
│            ✅ 可访问              │
└──────────────────────────────────┘
```

## 主要组件

### 1. DualWorldScriptLoader

管理脚本加载和分离：

**页面世界资源：**
- `stealth.js` - 反检测补丁

**隔离世界资源：**
- `runtime_bridge.js` - 运行时 API 桥接
- `configs.js` - 配置变量
- `node_ext.js` - DOM 节点扩展
- `node_traversor.js` - DOM 树遍历
- `feature_calculator.js` - 元素特征计算
- `__pulsar_utils__.js` - 核心工具函数

### 2. IsolatedWorldManager

管理隔离世界的创建和脚本注入：

```kotlin
class IsolatedWorldManager {
    // 创建隔离世界
    suspend fun createIsolatedWorld(frameId: String? = null): Int

    // 注入运行时
    suspend fun injectRuntime(runtimeScript: String, contextId: Int)

    // 在隔离世界中执行脚本
    suspend fun evaluateInIsolatedWorld(script: String, contextId: Int?): Any?
}
```

## 使用方法

### 默认行为

Browser4 默认自动使用双世界架构：

```kotlin
val browser = BrowserFactory.createBrowser()
val driver = browser.newDriver()
driver.navigateTo("https://example.com")
// 脚本自动注入到两个世界中
```

### 从 CDP 访问运行时

```kotlin
// 在隔离世界中执行
val result = isolatedWorldManager.evaluateInIsolatedWorld(
    "__browser4_runtime__.getInfo()",
    contextId
)
```

### 页面 JavaScript 无法检测

```javascript
// 在页面世界中运行
console.log(typeof __browser4_runtime__);  // "undefined"
console.log(typeof __pulsar_utils__);      // "undefined"

// 即使用内省也找不到
Object.keys(window).filter(k => k.includes('browser4'));  // []
```

## 优势

### 1. 安全与隐形
- ✅ **无法检测**：网站无法检测 Browser4 自动化
- ✅ **隔离**：页面脚本无法篡改运行时
- ✅ **安全**：不会意外暴露自动化能力

### 2. 可维护性
- ✅ **关注点分离**：隐形与运行时逻辑分离
- ✅ **可版本化**：运行时可独立版本管理
- ✅ **热更新**：运行时可在不重载页面的情况下更新

### 3. 兼容性
- ✅ **向后兼容**：需要时可回退到传统模式
- ✅ **标准兼容**：使用标准 Chrome CDP API
- ✅ **跨平台**：支持 Chrome 的所有平台都可用

## 测试

### 单元测试

```bash
./mvnw test -Dtest=DualWorldScriptLoaderTest
```

测试覆盖：
- ✅ 页面世界只包含隐形补丁
- ✅ 隔离世界包含完整运行时
- ✅ 脚本正确分离
- ✅ 重载功能正常
- ✅ 资源列表正确

## 迁移指南

### 从传统（单世界）到双世界

**无需代码更改！** 双世界架构自动启用。

如果需要明确控制：

```kotlin
// 使用双世界（默认）
val settings = BrowserSettings(config)
val loader = settings.dualWorldScriptLoader

// 使用传统单世界（如需兼容性）
val legacyLoader = settings.scriptLoader  // 已弃用
```

## 自定义脚本

**页面世界脚本**（用于隐形/补丁）：
```
{BROWSER_DATA_DIR}/browser/js/preload/page-world/my-patch.js
```

**隔离世界脚本**（用于运行时扩展）：
```
{BROWSER_DATA_DIR}/browser/js/preload/isolated-world/my-extension.js
```

## 参考资料

- [完整文档](dual-world-architecture.md)
- [Chrome DevTools Protocol](https://chromedevtools.github.io/devtools-protocol/)
- [GitHub Issues](https://github.com/platonai/Browser4/issues)

---

**版本**: 1.0.0
**最后更新**: 2026-01-22
**状态**: 稳定
