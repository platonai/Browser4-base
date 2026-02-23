# Browser4 Kotlin SDK 中文文档

**版本:** 4.6.0-SNAPSHOT

欢迎使用 Browser4 Kotlin SDK 中文文档！本 SDK 为浏览器自动化、网页抓取和 AI 驱动的网页交互提供了强大而直观的接口。

## 什么是 Browser4 Kotlin SDK？

Browser4 Kotlin SDK 是一个基于 Kotlin 的客户端库，为 Browser4 浏览器自动化平台提供高级接口。它使开发者能够：

- 🤖 **自动化浏览器交互** - 使用直观的 API 以编程方式控制网页浏览器
- 🚀 **提取网页数据** - 使用 CSS 选择器或 AI 从网页中提取结构化数据
- 🧠 **AI 驱动的自动化** - 使用自然语言描述浏览器操作
- ⚡ **高性能** - 基于 Kotlin 协程构建，实现高效的并发操作
- 🎯 **简单而强大** - 易于学习，但足够强大以处理复杂的自动化任务

## 核心特性

### 🎮 多种 API 风格

SDK 支持三种不同的使用模式以满足您的需求：

- **PulsarSession** - 用于数据提取和页面加载
- **WebDriver** - 用于传统的浏览器自动化和元素交互
- **AgenticSession** - 用于 AI 驱动的自然语言自动化

### 🔌 灵活的部署方式

- **本地驱动模式** - 自动下载并在本地运行 Browser4
- **远程服务器模式** - 连接到现有的 Browser4 服务器
- **Docker 支持** - 在容器化环境中运行

### 📦 丰富的功能

- 会话管理和生命周期控制
- 导航和页面交互
- 元素选择和操作
- 使用 CSS 选择器或 XPath 提取数据
- 截屏捕获
- JavaScript 执行
- AI 驱动的操作和观察
- 协程安全的异步操作

## 快速示例

以下是 SDK 的使用示例：

```kotlin
import ai.platon.pulsar.sdk.v0.*

suspend fun main() {
    // 创建带有自动本地驱动的会话
    val session = AgenticSession.getOrCreate()
    val driver = session.getOrCreateBoundDriver()
    val agent = session.companionAgent

    // 打开页面并提取数据
    val page = session.open("https://example.com")
    val document = session.parse(page)
    val fields = session.extract(document, mapOf(
        "title" to "h1",
        "description" to "p"
    ))

    println("标题: ${fields["title"]}")
    println("描述: ${fields["description"]}")

    // 使用 AI 进行自然语言自动化
    agent.act("点击搜索按钮")
    agent.run("搜索 'kotlin' 并点击第一个结果")

    // 清理资源
    session.context.close()
}
```

## 快速开始

准备好开始了吗？查看以下资源：

- [快速开始](quick-start.md) - 运行您的第一个自动化脚本
- [API 参考](api-reference.md) - 探索完整的 API
- [示例代码](../examples/basic-usage.md) - 从实际示例中学习

## 架构

SDK 组织为几个关键组件：

```
AgenticSession (AI 驱动的自动化)
    ↓ 继承自
PulsarSession (数据提取和页面加载)
    ↓ 使用
WebDriver (浏览器控制和元素交互)
    ↓ 使用
PulsarClient (底层 HTTP 通信)
```

每一层都建立在前一层之上，为您提供灵活性，以使用最适合您需求的抽象级别。

## 安装

### Maven

将以下依赖项添加到您的 `pom.xml`：

```xml
<dependency>
    <groupId>io.browser4</groupId>
    <artifactId>browser4-kotlin</artifactId>
    <version>4.6.0-SNAPSHOT</version>
</dependency>
```

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("io.browser4:browser4-kotlin:4.6.0-SNAPSHOT")
}
```

### Gradle (Groovy)

```groovy
dependencies {
    implementation 'io.browser4:browser4-kotlin:4.6.0-SNAPSHOT'
}
```

## 社区与支持

- **GitHub 仓库**: [platonai/Browser4](https://github.com/platonai/Browser4)
- **问题反馈**: [报告 Bug 或请求功能](https://github.com/platonai/Browser4/issues)
- **Docker**: [Browser4 Docker 镜像](https://hub.docker.com/r/galaxyeye88/browser4)

## 许可证

Browser4 Kotlin SDK 采用 [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0) 许可。

## 下一步

- 📘 [阅读快速开始指南](quick-start.md)
- 💻 [尝试示例代码](../examples/basic-usage.md)
- 📚 [浏览 API 参考](api-reference.md)
