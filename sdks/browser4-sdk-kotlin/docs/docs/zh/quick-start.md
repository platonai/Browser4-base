# 快速开始

本指南将帮助您在几分钟内开始使用 Browser4 Kotlin SDK。

## 前提条件

- Java 17 或更高版本
- Maven 或 Gradle
- （可选）OpenRouter API 密钥（用于 AI 功能）

## 安装

### Maven

```xml
<dependency>
    <groupId>io.browser4</groupId>
    <artifactId>browser4-sdk-kotlin</artifactId>
    <version>4.6.0-SNAPSHOT</version>
</dependency>
```

### Gradle

```kotlin
implementation("io.browser4:browser4-sdk-kotlin:4.6.0-SNAPSHOT")
```

## 第一个示例

创建一个简单的 Kotlin 文件：

```kotlin
import ai.platon.pulsar.sdk.v0.*

suspend fun main() {
    // 创建会话（自动启动本地驱动）
    val session = AgenticSession.getOrCreate()

    // 打开网页
    val page = session.open("https://news.ycombinator.com")
    println("已打开: ${page.url}")

    // 解析页面
    val document = session.parse(page)
    println("标题: ${document?.title()}")

    // 提取数据
    val fields = session.extract(document!!, mapOf(
        "headline" to ".titleline > a:first-child"
    ))
    println("第一条新闻: ${fields["headline"]}")

    // 清理
    session.context.close()
}
```

## 运行程序

### 使用 Maven

```bash
mvn compile exec:java -Dexec.mainClass="MainKt"
```

### 使用 Gradle

```bash
gradle run
```

## 使用 WebDriver API

```kotlin
import ai.platon.pulsar.sdk.v0.*

suspend fun main() {
    val session = AgenticSession.getOrCreate()
    val driver = session.getOrCreateBoundDriver()

    // 导航
    driver.navigateTo("https://example.com")
    println("当前 URL: ${driver.currentUrl()}")

    // 点击元素
    driver.click("a.more-link")

    // 填充表单
    driver.fill("#search-input", "Kotlin SDK")
    driver.press("#search-input", "Enter")

    // 等待元素
    driver.waitForSelector(".results")

    // 提取文本
    val results = driver.selectTextAll(".result-item")
    results.forEach { println(it) }

    session.context.close()
}
```

## 使用 AI 自动化

```kotlin
import ai.platon.pulsar.sdk.v0.*

suspend fun main() {
    // 设置 API 密钥
    System.setProperty("OPENROUTER_API_KEY", "your-api-key")

    val session = AgenticSession.getOrCreate()
    val agent = session.companionAgent

    // 导航到页面
    session.open("https://news.ycombinator.com")

    // 使用自然语言执行操作
    val result = agent.act("点击第一条新闻")
    println("操作结果: ${result.message}")

    // 执行自主任务
    val task = agent.run("""
        找到搜索框，输入 'Kotlin'，
        然后提交表单
    """)
    println("任务结果: ${task.message}")

    session.context.close()
}
```

## 数据提取示例

```kotlin
import ai.platon.pulsar.sdk.v0.*

suspend fun main() {
    val session = AgenticSession.getOrCreate()

    // 加载页面
    val page = session.load("https://example.com/products")
    val document = session.parse(page)

    // 定义提取规则
    val selectors = mapOf(
        "name" to ".product-name",
        "price" to ".product-price",
        "rating" to ".product-rating",
        "availability" to ".product-stock"
    )

    // 提取数据
    val product = session.extract(document!!, selectors)

    println("产品信息:")
    product.forEach { (field, value) ->
        println("  $field: $value")
    }

    session.context.close()
}
```

## 本地驱动模式 vs 远程服务器

### 本地驱动（默认）

```kotlin
// 自动下载并启动 Browser4.jar
val session = AgenticSession.getOrCreate()
```

本地驱动会：
1. 检查 `~/.browser4/Browser4.jar` 是否存在
2. 如果不存在，从 GitHub 下载
3. 在端口 8182 上启动服务器
4. 自动连接

### 远程服务器

```kotlin
// 连接到现有服务器
val session = AgenticSession.getOrCreate(baseUrl = "http://your-server:8182")
```

## 配置 API 密钥

对于 AI 功能，设置您的 OpenRouter API 密钥：

### Linux/macOS

```bash
export OPENROUTER_API_KEY="your-api-key"
```

### Windows

```cmd
set OPENROUTER_API_KEY=your-api-key
```

或在代码中设置：

```kotlin
val options = LocalDriverOptions(
    javaOptions = mapOf("OPENROUTER_API_KEY" to "your-api-key")
)
val client = PulsarClient(useLocalDriver = true, localDriverOptions = options)
```

## 常见问题

### "端口 8182 已被占用"

使用不同的端口：

```kotlin
val options = LocalDriverOptions(port = 9000)
val client = PulsarClient(useLocalDriver = true, localDriverOptions = options)
client.createSession()
val session = AgenticSession(client)
```

### "找不到 Browser4.jar"

SDK 会自动下载。如果下载失败，手动指定路径：

```kotlin
val options = LocalDriverOptions(
    jarPath = "/path/to/Browser4.jar"
)
```

## 下一步

- 📖 [完整的用户指南](../guide/session-management.md)
- 🔍 [API 参考](api-reference.md)
- 💡 [更多示例](../examples/basic-usage.md)
- ⚙️ [配置选项](../configuration/local-driver.md)
