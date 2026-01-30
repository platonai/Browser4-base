# API 参考

Browser4 Kotlin SDK 提供了多层 API，以满足不同的使用场景。

## 核心类

### PulsarClient

底层 HTTP 客户端，用于与 Browser4 API 通信。

**主要方法：**

- `createSession(capabilities)` - 创建新的浏览器会话
- `deleteSession(sessionId)` - 删除会话
- `post(path, body, sessionId)` - 发送 POST 请求
- `get(path, sessionId)` - 发送 GET 请求
- `close()` - 关闭客户端

**示例：**

```kotlin
val client = PulsarClient(baseUrl = "http://localhost:8182")
val sessionId = client.createSession()
// 使用客户端...
client.deleteSession(sessionId)
client.close()
```

### PulsarSession

会话管理类，用于页面加载和数据提取。

**主要方法：**

| 方法 | 说明 |
|------|------|
| `normalize(url, args)` | 规范化 URL 和加载参数 |
| `open(url, args)` | 立即打开 URL（绕过缓存） |
| `load(url, args)` | 从缓存加载或从互联网获取 |
| `submit(url, args)` | 提交 URL 到爬取池 |
| `parse(page)` | 将页面解析为 Jsoup 文档 |
| `extract(document, selectors)` | 使用 CSS 选择器提取字段 |
| `scrape(url, args, selectors)` | 一次操作完成加载、解析和提取 |

**示例：**

```kotlin
val session = PulsarSession(client)

// 加载页面
val page = session.load("https://example.com", "-expire 1d")

// 解析文档
val document = session.parse(page)

// 提取数据
val fields = session.extract(document!!, mapOf(
    "title" to "h1",
    "content" to ".main-content"
))

session.close()
```

### AgenticSession

AI 驱动的浏览器自动化会话，继承自 PulsarSession。

**主要方法：**

| 方法 | 说明 |
|------|------|
| `act(action)` | 执行自然语言描述的单个操作 |
| `run(task)` | 运行自主代理任务 |
| `observe(instruction)` | 观察页面并建议操作 |
| `agentExtract(instruction, schema)` | AI 驱动的数据提取 |
| `summarize(instruction)` | 生成页面摘要 |
| `clearHistory()` | 清除代理历史 |
| `getOrCreateBoundDriver()` | 获取或创建绑定的 WebDriver |

**示例：**

```kotlin
val session = AgenticSession.getOrCreate()
val agent = session.companionAgent

// 执行操作
agent.act("点击搜索按钮")

// 运行任务
agent.run("搜索 'Kotlin' 并提取前 5 个结果")

// 观察页面
val observation = agent.observe("找到所有交互元素")

session.context.close()
```

### WebDriver

浏览器控制和元素交互。

**导航方法：**

- `navigateTo(url)` - 导航到 URL
- `currentUrl()` - 获取当前 URL
- `reload()` - 重新加载页面
- `goBack()` - 后退
- `goForward()` - 前进
- `title()` - 获取页面标题

**元素交互方法：**

- `click(selector)` - 点击元素
- `fill(selector, text)` - 填充输入框
- `type(selector, text)` - 逐字符输入
- `press(selector, key)` - 按键
- `hover(selector)` - 悬停
- `focus(selector)` - 聚焦
- `check(selector)` - 选中复选框
- `uncheck(selector)` - 取消选中复选框

**等待方法：**

- `waitForSelector(selector, timeout)` - 等待选择器出现
- `waitForNavigation()` - 等待导航完成
- `exists(selector)` - 检查元素是否存在
- `isVisible(selector)` - 检查元素是否可见
- `isHidden(selector)` - 检查元素是否隐藏

**滚动方法：**

- `scrollDown(count)` - 向下滚动
- `scrollUp(count)` - 向上滚动
- `scrollTo(selector)` - 滚动到元素
- `scrollToTop()` - 滚动到顶部
- `scrollToBottom()` - 滚动到底部
- `scrollToMiddle(ratio)` - 滚动到中间

**内容提取方法：**

- `selectFirstTextOrNull(selector)` - 选择第一个文本
- `selectTextAll(selector)` - 选择所有文本
- `selectFirstAttributeOrNull(selector, attr)` - 选择第一个属性
- `selectAttributeAll(selector, attr)` - 选择所有属性
- `outerHtml(selector)` - 获取外部 HTML
- `textContent(selector)` - 获取文本内容
- `extract(fields)` - 一次提取多个字段

**截屏方法：**

- `captureScreenshot(selector, fullPage)` - 捕获截屏
- `screenshot(selector)` - 快捷截屏方法

**脚本执行方法：**

- `executeScript(script, args)` - 执行 JavaScript
- `executeAsyncScript(script, args, timeout)` - 执行异步脚本
- `evaluate(expression)` - 评估表达式

**控制方法：**

- `delay(millis)` - 延迟
- `pause()` - 暂停
- `stop()` - 停止

**示例：**

```kotlin
val driver = session.getOrCreateBoundDriver()

// 导航
driver.navigateTo("https://example.com")

// 交互
driver.click(".menu-button")
driver.fill("#search", "Kotlin")
driver.press("#search", "Enter")

// 提取
val results = driver.selectTextAll(".result-item")

// 截屏
val screenshot = driver.captureScreenshot()

// 脚本
val result = driver.executeScript("return document.title")
```

## 数据模型

### WebPage

表示加载的网页。

```kotlin
data class WebPage(
    val url: String,
    val location: String?,
    val contentType: String?,
    val contentLength: Int,
    val protocolStatus: String?,
    val html: String?
)
```

### NormURL

规范化的 URL 结果。

```kotlin
data class NormURL(
    val url: String,
    val args: String?
)
```

### AgentRunResult

代理运行操作的结果。

```kotlin
data class AgentRunResult(
    val success: Boolean,
    val message: String,
    val historySize: Int,
    val processTraceSize: Int,
    val finalResult: Any?,
    val trace: List<String>?
)
```

### AgentActResult

代理执行操作的结果。

```kotlin
data class AgentActResult(
    val success: Boolean,
    val message: String,
    val action: String?,
    val isComplete: Boolean,
    val expression: String?,
    val result: Any?,
    val trace: List<String>?
)
```

### ObserveResult

代理观察操作的单个观察结果。

```kotlin
data class ObserveResult(
    val locator: String?,
    val domain: String?,
    val method: String?,
    val arguments: Map<String, Any?>?,
    val description: String?,
    val screenshotContentSummary: String?,
    val currentPageContentSummary: String?,
    val nextGoal: String?,
    val thinking: String?,
    val summary: String?,
    val keyFindings: String?,
    val nextSuggestions: List<String>?
)
```

## 配置选项

### LocalDriverOptions

本地驱动配置选项。

```kotlin
data class LocalDriverOptions(
    val jarPath: String? = null,              // Browser4.jar 的自定义路径
    val downloadUrl: String? = null,          // 自定义下载 URL
    val port: Int? = null,                    // 自定义端口（默认：8182）
    val javaOptions: Map<String, String> = emptyMap()  // Java 系统属性
)
```

**示例：**

```kotlin
val options = LocalDriverOptions(
    port = 9000,
    javaOptions = mapOf(
        "OPENROUTER_API_KEY" to "your-api-key",
        "server.port" to "9000"
    )
)

val client = PulsarClient(
    useLocalDriver = true,
    localDriverOptions = options
)
```

## 更多资源

- [完整示例](../examples/basic-usage.md)
- [用户指南](../guide/session-management.md)
- [配置选项](../configuration/local-driver.md)
