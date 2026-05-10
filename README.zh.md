# 🤖 Browser4

---

[English](README.md) | 简体中文

<!-- TOC -->
**目录**
- [🤖 Browser4](#-browser4)
    - [🌟 简介](#-简介)
        - [✨ 核心能力](#-核心能力)
    - [🎥 演示视频](#-演示视频)
    - [💡 使用示例](#-使用示例)
        - [工作流自动化](#工作流自动化)
        - [LLM + X-SQL](#llm--x-sql)
        - [高速并行处理](#高速并行处理)
        - [自动提取](#自动提取)
    - [📦 模块概览](#-模块概览)
    - [📜 文档](#-文档)
    - [🔧 代理 - 解除网站封锁](#-代理---解除网站封锁)
    - [✨ 特性](#-特性)
    - [🤝 支持与社区](#-支持与社区)
<!-- /TOC -->

## 🌟 简介

💖 **Browser4：为你的 AI 打造的闪电般快速、协程安全的浏览器引擎** 💖

### ✨ 核心能力

* 🤖 **浏览器自动化** — 高性能的工作流、导航和数据提取自动化。
* ⚡  **极致性能** — 完全协程安全；支持单机每天 10 万 ~ 20 万次复杂页面访问。
* 🧬 **数据提取** — LLM、ML 和选择器的混合方案，从混乱的页面中提取干净的数据。

## 🎥 演示视频

🎬 YouTube:
[![观看视频](https://img.youtube.com/vi/rJzXNXH3Gwk/0.jpg)](https://youtu.be/rJzXNXH3Gwk)

📺 Bilibili:
[https://www.bilibili.com/video/BV1fXUzBFE4L](https://www.bilibili.com/video/BV1fXUzBFE4L)

---

## 💡 使用示例

### 工作流自动化

底层浏览器自动化和数据提取，提供细粒度控制。

**特性：**
- 同时支持实时 DOM 访问和离线快照解析
- 直接且完整的 Chrome DevTools Protocol (CDP) 控制，协程安全
- 精确的元素交互（点击、滚动、输入）
- 使用 CSS 选择器/XPath 快速提取数据

```kotlin
val session = AgenticContexts.getOrCreateSession()
val agent = session.companionAgent
val driver = session.getOrCreateBoundDriver()

// 加载输入 URL 所引用的初始页面
var page = session.open(url)

// 使用自然语言指令驱动浏览器
agent.act("滚动到评论区")
// 直接从实时 DOM 中读取第一个匹配的评论节点
val content = driver.selectFirstTextOrNull("#comments")

// 将页面快照保存到内存文档中以供离线解析
var document = session.parse(page)
// 一次性将 CSS 选择器映射到结构化字段
var fields = session.extract(document, mapOf("title" to "#title"))

// 让伴随代理执行多步骤导航/搜索流程
val history = agent.run(
    "前往 amazon.com，搜索 'smart phone'，打开评分最高的商品页面"
)

// 将更新后的浏览器状态捕获回 PageSnapshot
page = session.capture(driver)
document = session.parse(page)
// 从捕获的快照中提取额外的属性
fields = session.extract(document, mapOf("ratings" to "#ratings"))
```

### LLM + X-SQL

非常适合高复杂度的数据提取流水线，涉及数十个实体、每个实体数百个字段。

**优势：**
- 相比传统方法，能够提取 10 倍以上的实体和 100 倍以上的字段
- 将 LLM 智能与精确的 CSS 选择器/XPath 相结合
- 类 SQL 语法，熟悉的查询方式

```kotlin
val context = AgenticContexts.create()
val sql = """
select
  llm_extract(dom, 'product name, price, ratings') as llm_extracted_data,
  dom_first_text(dom, '#productTitle') as title,
  dom_first_text(dom, '#bylineInfo') as brand,
  dom_first_text(dom, '#price tr td:matches(^Price) ~ td, #corePrice_desktop tr td:matches(^Price) ~ td') as price,
  dom_first_text(dom, '#acrCustomerReviewText') as ratings,
  str_first_float(dom_first_text(dom, '#reviewsMedley .AverageCustomerReviews span:contains(out of)'), 0.0) as score
from load_and_select('https://www.amazon.com/dp/B08PP5MSVB -i 1s -njr 3', 'body');
"""
val rs = context.executeQuery(sql)
println(ResultSetFormatter(rs, withHeader = true))
```

示例代码：

* [使用 X-SQL 从亚马逊商品页面抓取 100+ 字段](https://github.com/platonai/exotic-amazon/tree/main/src/main/resources/sites/amazon/crawl/parse/sql/crawl)
* [使用 X-SQL 抓取所有类型的亚马逊网页](https://github.com/platonai/exotic-amazon/tree/main/src/main/resources/sites/amazon/crawl/parse/sql/crawl)

### 高速并行处理

通过并行浏览器控制和智能资源优化实现极致吞吐量。

**性能：**
- 单机每天 1 万 ~ 2 万次复杂页面访问
- 并发会话管理
- 资源拦截以加快页面加载

```kotlin
val args = "-refresh -dropContent -interactLevel fastest"
val blockingUrls = listOf("*.png", "*.jpg")
val links = LinkExtractors.fromResource("urls.txt")
    .map { ListenableHyperlink(it, "", args = args) }
    .onEach {
        it.eventHandlers.browseEventHandlers.onWillNavigate.addLast { page, driver ->
            driver.addBlockedURLs(blockingUrls)
        }
    }

session.submitAll(links)
```

🎬 YouTube:
[![观看视频](https://img.youtube.com/vi/_BcryqWzVMI/0.jpg)](https://www.youtube.com/watch?v=_BcryqWzVMI)

📺 Bilibili:
[https://www.bilibili.com/video/BV1kM2rYrEFC](https://www.bilibili.com/video/BV1kM2rYrEFC)


---

### 自动提取

基于自监督/无监督机器学习的自动、大规模、高精度字段发现与提取 — 无需 LLM API 调用，零 Token，确定性且快速。

**它能做什么：**
- 高精度学习商品/详情页上的每一个可提取字段（通常数十到数百个）。
- 当 Browser4 在 GitHub 上达到 10K stars 时开源。

**为什么不仅仅使用 LLM？**
- LLM 提取会增加延迟、成本和 Token 限制。
- 基于 ML 的自动提取是本地化、可复现的，并可扩展到每天 10 万 ~ 20 万页面。
- 你也可以将两者结合：使用自动提取获取结构化基线数据 + LLM 进行语义增强。

**快速命令（PulsarRPAPro）：**
```bash
# 注意：需要 MongoDB
curl -L -o PulsarRPAPro.jar https://github.com/platonai/PulsarRPAPro/releases/download/v4.6.0/PulsarRPAPro.jar
```

**集成状态：**
- 当前可通过配套项目 [PulsarRPAPro](https://github.com/platonai/PulsarRPAPro) 使用。
- 原生的 Browser4 API 接入正在规划中；请关注发布动态。

**核心优势：**
- 高精度：>95% 的字段被发现；其中大多数准确率 >99%（在测试域名上的参考值）。
- 对选择器变化和 HTML 噪声具有韧性。
- 零外部依赖（无需 API Key）→ 大规模使用时成本效益高。
- 可解释：生成的选择器和 SQL 透明且可审计。

👽 使用机器学习代理提取数据：

![自动提取结果快照](docs/assets/images/amazon.png)

（即将推出：更丰富的仓库内示例和直接的 API 接口。）

---

---

## ✨ 特性

状态：[可用] 已包含在仓库中，[实验性] 正在积极迭代中，[计划中] 尚未加入仓库，[参考] 性能目标。

### 浏览器自动化与 RPA
- [可用] 基于工作流的浏览器操作
- [可用] 精确的协程安全控制（滚动、点击、提取）
- [可用] 灵活的事件处理器和生命周期管理

### 数据提取与查询
- [可用] 一行命令完成数据提取
- [可用] 面向 DOM/内容的 X-SQL 扩展查询语言
- [实验性] 结构化与非结构化混合提取（LLM & ML & 选择器）

### 性能与可扩展性
- [可用] 高效的并行页面渲染
- [可用] 反封锁设计和智能重试
- [参考] 在普通硬件上每天处理 10 万+ 复杂页面

### 隐匿与可靠性
- [实验性] 高级反机器人技术
- [可用] 通过 `PROXY_ROTATION_URL` 进行代理轮换
- [可用] 弹性调度与质量保证

### 开发者体验
- [可用] 简洁的 API 集成（REST、原生、文本命令）
- [可用] 丰富的配置分层
- [可用] 清晰的结构化日志和指标

### 存储与监控
- [可用] 支持本地文件系统和 MongoDB（可扩展）
- [可用] 全面的日志与透明度

---

## 🤝 支持与社区

加入我们的社区，获取支持、反馈和合作！

- **GitHub Discussions**：与开发者和其他用户交流。
- **Issue Tracker**：报告 Bug 或请求新功能。
- **社交媒体**：关注我们获取最新动态。

欢迎贡献！详情请参阅 [CONTRIBUTING.md](CONTRIBUTING.md)。

---

## 📜 文档

完整文档可在 `docs/` 目录和我们的 [GitHub Pages 站点](https://platonai.github.io/browser4/) 中查阅。

---

## 🔧 代理配置 - 解除网站封锁

<details>

设置环境变量 `PROXY_ROTATION_URL` 为你的代理服务商提供的轮换 URL：

```shell
export PROXY_ROTATION_URL=https://your-proxy-provider.com/rotation-endpoint
```

每次访问此轮换 URL 时，应返回一个或多个新的代理 IP。
如果你需要此类 URL，请联系你的代理服务商。

</details>

---

## 许可证

Apache 2.0 License。详情请参阅 [LICENSE](LICENSE)。
