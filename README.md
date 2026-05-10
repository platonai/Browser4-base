# 🤖 Browser4
# Browser4-base

English | [简体中文](README-CN.md) | [中国镜像](https://gitee.com/platonai_galaxyeye/Browser4)

## 🥁 Introduce

💖 **Browser4: Your Ultimate AI-RPA Solution!** 💖

**Browser4** is a **high-performance**, **distributed**, and **open-source** Robotic Process Automation (RPA) framework.
Designed for **large-scale automation**, it excels in **browser automation**, **web content understanding**,
and **data extraction**. Browser4 tackles the challenges of modern web automation,
ensuring **accurate** and **comprehensive** data extraction even from the most **complex** and **dynamic** websites.

## Videos

YouTube:
[![Watch the video](https://img.youtube.com/vi/lQXSSQSNQ7I/0.jpg)](https://www.youtube.com/watch?v=lQXSSQSNQ7I)

Bilibili:
[https://www.bilibili.com/video/BV1kM2rYrEFC](https://www.bilibili.com/video/BV1kM2rYrEFC)




[![Docker Pulls](https://img.shields.io/docker/pulls/galaxyeye88/browser4?style=flat-square)](https://hub.docker.com/r/galaxyeye88/browser4)
[![License: APACHE2](https://img.shields.io/badge/license-APACHE2-green?style=flat-square)](https://github.com/platonai/browser4/blob/main/LICENSE)

---

English | [简体中文](README.zh.md) | [中国镜像](https://gitee.com/platonai_galaxyeye/Browser4)

<!-- TOC -->
**Table of Contents**
- [🤖 Browser4](#-browser4)
    - [🌟 Introduction](#-introduction)
        - [�?Key Capabilities](#-key-capabilities)
    - [🎥 Demo Videos](#-demo-videos)
    - [🚀 Quick Start](#-quick-start)
    - [💡 Usage Examples](#-usage-examples)
        - [Browser Agents](#browser-agents)
        - [Workflow Automation](#workflow-automation)
        - [LLM + X-SQL](#llm--x-sql)
        - [High-Speed Parallel Processing](#high-speed-parallel-processing)
        - [Auto Extraction](#auto-extraction)
    - [📦 Modules Overview](#-modules-overview)
    - [📜 Documentation](#-documentation)
    - [🔧 Proxies - Unblock Websites](#-proxies---unblock-websites)
    - [�?Features](#-features)
    - [🤝 Support & Community](#-support--community)
<!-- /TOC -->

## 🌟 Introduction

💖 **Browser4: a lightning-fast, coroutine-safe browser engine for your AI** 💖

### �?Key Capabilities

* 👽 **Browser Agents** �?Fully autonomous browser agents that reason, plan, and execute end-to-end tasks.
* 🤖 **Browser Automation** �?High-performance automation for workflows, navigation, and data extraction.
* ⚙️ **Machine Learning Agent** - Learns field structures across complex pages without consuming tokens.
* �? **Extreme Performance** �?Fully coroutine-safe; supports 100k ~ 200k complex page visits per machine per day.
* 🧬 **Data Extraction** �?Hybrid of LLM, ML, and selectors for clean data across chaotic pages.

## �?Quick Example: Agentic Workflow

## 🚀 Quick start

### Chat about a webpage:

```kotlin
// Give your Agent a mission, not just a script.
val agent = AgenticContexts.getOrCreateAgent()

// The Agent plans, navigates, and executes using Browser4 as its hands and eyes.
val result = agent.run("""
    1. Go to amazon.com
    2. Search for '4k monitors'
    3. Analyze the top 5 results for price/performance ratio
    4. Return the best option as JSON
""")
val document = session.loadDocument(url)
val response = session.chat("Tell me something about this webpage", document)
```

---

## 🎥 Demo Videos

🎬 YouTube:
[![Watch the video](https://img.youtube.com/vi/rJzXNXH3Gwk/0.jpg)](https://youtu.be/rJzXNXH3Gwk)

📺 Bilibili:
[https://www.bilibili.com/video/BV1fXUzBFE4L](https://www.bilibili.com/video/BV1fXUzBFE4L)

---

## 🚀 Quick Start

**Prerequisites**: Java 17+

1. **Clone the repository**
   ```shell
   git clone https://github.com/platonai/browser4.git
   cd browser4
   ```
Example code: [kotlin](/examples/browser4-examples/src/main/kotlin/ai/platon/pulsar/human/manual/llm/ChatAboutPage.kt).

2. **Configure your LLM API key**
### Tell the browser to get jobs done:

> Edit [application.properties](application.properties) and add your API key.
```kotlin
val prompts = """
move cursor to the element with id 'title' and click it
scroll to middle
scroll to top
get the text of the element with id 'title'
"""

3. **Build the project**
   ```shell
   ./mvnw -DskipTests
   ```
val eventHandlers = DefaultPageEventHandlers()
eventHandlers.browseEventHandlers.onDocumentActuallyReady.addLast { page, driver ->
val result = session.instruct(prompts, driver)
}
session.open(url, eventHandlers)
```

---

## 💡 Usage Examples
Example code: [kotlin](/examples/browser4-examples/src/main/kotlin/ai/platon/pulsar/human/manual/llm/TalkToActivePage.kt).

### Workflow Automation

Low-level browser automation & data extraction with fine-grained control.

**Features:**
- Both live DOM access and offline snapshot parsing
- Direct and full Chrome DevTools Protocol (CDP) control, coroutine safe
- Precise element interactions (click, scroll, input)
- Fast data extraction using CSS selectors/XPath
### One line of code to scrape:

```kotlin
val session = AgenticContexts.getOrCreateSession()
val agent = session.companionAgent
val driver = session.getOrCreateBoundDriver()

// Load the initial page referenced by your input URL
var page = session.open(url)

// Drive the browser with natural-language instructions
agent.act("scroll to the comment section")
// Read the first matching comment node directly from the live DOM
val content = driver.selectFirstTextOrNull("#comments")

// Snapshot the page to an in-memory document for offline parsing
var document = session.parse(page)
// Map CSS selectors to structured fields in one call
var fields = session.extract(document, mapOf("title" to "#title"))

// Let the companion agent execute a multi-step navigation/search flow
val history = agent.run(
    "Go to amazon.com, search for 'smart phone', open the product page with the highest ratings"
)

// Capture the updated browser state back into a PageSnapshot
page = session.capture(driver)
document = session.parse(page)
// Extract additional attributes from the captured snapshot
fields = session.extract(document, mapOf("ratings" to "#ratings"))
session.scrapeOutPages(
    "https://www.amazon.com/",  "-outLink a[href~=/dp/]", listOf("#title", "#acrCustomerReviewText"))
```

### LLM + X-SQL

Ideal for high-complexity data-extraction pipelines with multiple-dozen entities and several hundred fields per entity.

**Benefits:**
- Extract 10x more entities and 100x more fields compared to traditional methods
- Combine LLM intelligence with precise CSS selectors/XPath
- SQL-like syntax for familiar data queries
### Crawl with Robotic Process Automation (RPA):

```kotlin
val context = AgenticContexts.create()
val sql = """
val options = session.options(args)
val event = options.eventHandlers.browseEventHandlers
event.onBrowserLaunched.addLast { page, driver ->
    // warp up the browser to avoid being blocked by the website,
    // or choose the global settings, such as your location.
    warnUpBrowser(page, driver)
}
event.onWillFetch.addLast { page, driver ->
    // have to visit a referrer page before we can visit the desired page
    waitForReferrer(page, driver)
    // websites may prevent us from opening too many pages at a time, so we should open links one by one.
    waitForPreviousPage(page, driver)
}
event.onWillCheckDocumentState.addLast { page, driver ->
    // wait for a special fields to appear on the page
    driver.waitForSelector("body h1[itemprop=name]")
    // close the mask layer, it might be promotions, ads, or something else.
    driver.click(".mask-layer-close-button")
}
// visit the URL and trigger events
session.load(url, options)
```

Example code: [kotlin](/examples/browser4-examples/src/main/kotlin/ai/platon/pulsar/human/manual/sites/food/dianping/RestaurantCrawler.kt).

### Resolve *super complex* web data extraction problems using X-SQL:

```sql
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
    dom_first_text(dom, '#productTitle') as title,
    dom_first_text(dom, '#bylineInfo') as brand,
    dom_first_text(dom, '#price tr td:matches(^Price) ~ td, #corePrice_desktop tr td:matches(^Price) ~ td') as price,
    dom_first_text(dom, '#acrCustomerReviewText') as ratings,
    str_first_float(dom_first_text(dom, '#reviewsMedley .AverageCustomerReviews span:contains(out of)'), 0.0) as score
from load_and_select('https://www.amazon.com/dp/B0C1H26C46  -i 1s -njr 3', 'body');
```

Example code:

* [X-SQL to scrape 100+ fields from an Amazon's product page](https://github.com/platonai/exotic-amazon/tree/main/src/main/resources/sites/amazon/crawl/parse/sql/crawl)
* [X-SQLs to crawl all types of Amazon webpages](https://github.com/platonai/exotic-amazon/tree/main/src/main/resources/sites/amazon/crawl/parse/sql/crawl)

### High-Speed Parallel Processing
* [X-SQLs to scrape all types of Amazon webpages](https://github.com/platonai/exotic-amazon/tree/main/src/main/resources/sites/amazon/crawl/parse/sql/crawl)

Achieve extreme throughput with parallel browser control and smart resource optimization.

**Performance:**
- 10k ~ 20k complex page visits per machine per day
- Concurrent session management
- Resource blocking for faster page loads
### Continuous web crawling:

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
fun main() {
    val context = PulsarContexts.create()

    val parseHandler = { _: WebPage, document: FeaturedDocument ->
        // use the document
        // ...
        // and then extract further hyperlinks
        context.submitAll(document.selectHyperlinks("a[href~=/dp/]"))
    }
    val urls = LinkExtractors.fromResource("seeds10.txt")
        .map { ParsableHyperlink("$it -refresh", parseHandler) }
    context.submitAll(urls).await()
}
```

Example code: [kotlin](/examples/browser4-examples/src/main/kotlin/ai/platon/pulsar/human/manual/_5_ContinuousCrawler.kt), [java](/examples/browser4-examples/src/main/java/ai/platon/pulsar/human/manual/ContinuousCrawler.java).

session.submitAll(links)
```





🎬 YouTube:
[![Watch the video](https://img.youtube.com/vi/_BcryqWzVMI/0.jpg)](https://www.youtube.com/watch?v=_BcryqWzVMI)

📺 Bilibili:
[https://www.bilibili.com/video/BV1kM2rYrEFC](https://www.bilibili.com/video/BV1kM2rYrEFC)


---

### Auto Extraction

Automatic, large-scale, high-precision field discovery and extraction powered by self-/unsupervised machine learning �?no LLM API calls, no tokens, deterministic and fast.

**What it does:**
- Learns every extractable field on item/detail pages (often dozens to hundreds) with high precision.
- Open source when browser4 has 10K stars on GitHub.

**Why not just LLMs?**
- LLM extraction adds latency, cost, and token limits.
- ML-based auto extraction is local, reproducible, and scalable to 100k+ ~ 200k pages/day.
- You can still combine both: use Auto Extraction for structured baseline + LLM for semantic enrichment.

**Quick Commands (PulsarRPAPro):**
```bash
# NOTE: MongoDB required
curl -L -o PulsarRPAPro.jar https://github.com/platonai/PulsarRPAPro/releases/download/v4.7.2/PulsarRPAPro.jar
```

## 🚄 Features

- Web Spider: Scalable crawling, browser rendering, AJAX data extraction, and more.

- LLM Integration: Analyze and describe web content using natural, everyday language.

- Text-to-Action: Control browser actions through simple, intuitive language commands.

**Integration Status:**
- Available today via the companion project [PulsarRPAPro](https://github.com/platonai/PulsarRPAPro).
- Native Browser4 API exposure is planned; follow releases for updates.
- RPA (Robotic Process Automation): Automate human-like tasks, including Single Page Application (SPA) crawling and other high-value workflows.

**Key Advantages:**
- High precision: >95% fields discovered; majority with >99% accuracy (indicative on tested domains).
- Resilient to selector churn & HTML noise.
- Zero external dependency (no API key) �?cost-efficient at scale.
- Explainable: generated selectors & SQL are transparent and auditable.
- Simple API: Extract data with a single line of code or transform websites into structured tables with a single SQL query.

👽 Extract data with machine learning agents:
- X-SQL: Extended SQL for managing web data—crawling, scraping, content mining, and web-based business intelligence.

![Auto Extraction Result Snapshot](docs/assets/images/amazon.png)
- Bot Stealth: Advanced evasion techniques, including web driver stealth, IP rotation, and privacy context rotation to avoid detection and bans.

(Coming soon: richer in-repo examples and direct API hooks.)
- High Performance: Optimized for efficiency, capable of rendering hundreds of pages in parallel on a single machine without being blocked.

---
- Low Cost: Scrape 100,000+ browser-rendered e-commerce pages or process tens of millions of data points daily with minimal hardware requirements (8-core CPU, 32GB RAM).

## 📦 Modules Overview
- Data Quantity Assurance: Smart retry mechanisms, precise scheduling, and comprehensive web data lifecycle management.

| Module            | Description                                             |
|-------------------|---------------------------------------------------------|
| `pulsar-core`     | Core engine: sessions, scheduling, DOM, browser control |
| `pulsar-agentic`  | Agent implementation, MCP, and skill registration       |
| `pulsar-rest`     | Spring Boot REST layer & command endpoints              |
| `browser4-agents` | Agent & crawler orchestration with product packaging    |
| `sdks`            | CLI in Rust that supports SKILLS                        |
| `examples`        | Runnable examples and demos                             |
| `pulsar-tests`    | E2E & heavy integration & scenario tests                |
- Large-Scale Capability: Fully distributed architecture designed for massive-scale web crawling.

---
- Big Data Support: Flexible backend storage options, including Local File, MongoDB, HBase, and Gora.

## �?Features
- Logs & Metrics: Comprehensive monitoring and detailed event logging for full transparency.

Status: [Available] in repo, [Experimental] in active iteration, [Planned] not in repo, [Indicative] performance target.
- Auto Extraction: AI-powered pattern recognition to automatically and accurately extract all fields from webpages.

### AI & Agents
- [Available] Problem-solving autonomous browser agents
- [Available] Parallel agent sessions
- [Experimental] LLM-assisted page understanding & extraction
## 🧮 Browser4 as an executable jar

### Browser Automation & RPA
- [Available] Workflow-based browser actions
- [Available] Precise coroutine-safe control (scroll, click, extract)
- [Available] Flexible event handlers & lifecycle management
  We have released a standalone executable JAR based on Browser4, which includes:

### Data Extraction & Query
- [Available] One-line data extraction commands
- [Available] X-SQL extended query language for DOM/content
- [Experimental] Structured + unstructured hybrid extraction (LLM & ML & selectors)
- Data collection examples from top-tier websites.
- A mini-program for automatic information extraction based on self-supervised machine learning. The AI algorithm can identify all fields on detail pages with field accuracy exceeding 99%.
- A mini-program that automatically learns and outputs all collection rules based on self-supervised machine learning.
- The ability to execute web data collection tasks directly from the command line without writing any code.
- An upgraded Browser4 server that allows you to send SQL statements to collect web data.
- A Web UI where you can write SQL statements and send them to the server.

### Performance & Scalability
- [Available] High-efficiency parallel page rendering
- [Available] Block-resistant design & smart retries
- [Indicative] 100,000+ complex pages/day on modest hardware
  Download [Browser4Pro](https://github.com/platonai/Browser4Pro#download) and explore its capabilities with a single command line:

### Stealth & Reliability
- [Experimental] Advanced anti-bot techniques
- [Available] Proxy rotation via `PROXY_ROTATION_URL`
- [Available] Resilient scheduling & quality assurance
```shell
java -jar Browser4Pro.jar
```

### Developer Experience
- [Available] Simple API integration (REST, native, text commands)
- [Available] Rich configuration layering
- [Available] Clear structured logging & metrics
## 🎁 Browser4 as a java library

### Storage & Monitoring
- [Available] Local FS & MongoDB support (extensible)
- [Available] Comprehensive logs & transparency
  The simplest way to leverage the power of Browser4 is to add it to your project as a library.

---
Maven:

## 🤝 Support & Community
```xml
<dependency>
    <groupId>ai.platon.pulsar</groupId>
    <artifactId>pulsar-bom</artifactId>
    <version>VERSION</version>
</dependency>
```

Join our community for support, feedback, and collaboration!
Gradle:

- **GitHub Discussions**: Engage with developers and users.
- **Issue Tracker**: Report bugs or request features.
- **Social Media**: Follow us for updates and news.
```kotlin
implementation("ai.platon.pulsar:pulsar-bom:VERSION")
```

We welcome contributions! See [CONTRIBUTING.md](CONTRIBUTING.md) for details.
Clone the template project from github.com:
[kotlin](https://github.com/platonai/pulsar-kotlin-template),
[java-17](https://github.com/platonai/pulsar-java-17-template).

---
Start your own large-scale web crawling projects based on our commercial-grade open source projects: [Browser4Pro](https://github.com/platonai/Browser4Pro), [Exotic-amazon](https://github.com/platonai/exotic-amazon).

## 📜 Documentation
Check the [quick start](docs/get-started/2basic-usage.md) for more details.

Comprehensive documentation is available in the `docs/` directory and on our [GitHub Pages site](https://platonai.github.io/browser4/).
# 🌐 Browser4 as a REST Service

---
When Browser4 runs as a REST service, X-SQL can be used to scrape webpages or to query web data directly at any time, from anywhere, without opening an IDE.

## 🔧 Proxy Configuration - Unblock Website Access
## Build from Source

<details>
```
git clone https://github.com/platonai/Browser4.git
cd Browser4 && bin/build-run.sh
```

Set the environment variable `PROXY_ROTATION_URL` to the rotation URL provided by your proxy service provider:
For Chinese developers, we strongly suggest you to follow [this](/bin/tools/maven/maven-settings.md) instruction to accelerate the building process.

## Use X-SQL to Query the Web

Start the pulsar server if it is not started:

```shell
bin/pulsar
```

Scrape a webpage in another terminal window:

```shell
bin/scrape.sh
```

The bash script is straightforward. It merely uses curl to send a POST request with an X-SQL.

```shell
export PROXY_ROTATION_URL=https://your-proxy-provider.com/rotation-endpoint
curl -X POST --location "http://localhost:8182/api/x/e" -H "Content-Type: text/plain" -d "
  select
      dom_base_uri(dom) as url,
      dom_first_text(dom, '#productTitle') as title,
      dom_first_slim_html(dom, 'img:expr(width > 400)') as img
  from load_and_select('https://www.amazon.com/dp/B0C1H26C46', 'body');
"
```

Example code: [bash](bin/scrape.sh), [PowerShell](bin/scrape.ps1), [batch](bin/scrape.bat), [java](/pulsar-client/src/main/java/ai/platon/pulsar/client/Scraper.java), [kotlin](/pulsar-client/src/main/kotlin/ai/platon/pulsar/client/Scraper.kt), [php](/pulsar-client/src/main/php/Scraper.php).

Click [X-SQL](docs/x-sql.md) to see a detailed introduction and function descriptions about X-SQL.

# 📖 Step-by-Step Course

We have a step-by-step course by example:

- [Home](docs/get-started/1home.md)
- [Basic Usage](docs/get-started/2basic-usage.md)
- [Load Options](docs/get-started/3load-options.md)
- [Data Extraction](docs/get-started/4data-extraction.md)
- [URL](docs/get-started/5URL.md)
- [Java-style Async](docs/get-started/6Java-style-async.md)
- [Kotlin-style Async](docs/get-started/7Kotlin-style-async.md)
- [Continuous Crawling](docs/get-started/8continuous-crawling.md)
- [Event Handling](docs/get-started/9event-handling.md)
- [RPA](docs/get-started/10RPA.md)
- [WebDriver](docs/get-started/11WebDriver.md)
- [Massive Crawling](docs/get-started/12massive-crawling.md)
- [X-SQL](docs/get-started/13X-SQL.md)
- [AI Extraction](docs/get-started/14AI-extraction.md)
- [REST](docs/get-started/15REST.md)
- [Console](docs/get-started/16console.md)
- [Top Practice](docs/get-started/17top-practice.md)
- [Miscellaneous](docs/get-started/18miscellaneous.md)

# 📊 Logs & Metrics

Browser4 has carefully designed the logging and metrics subsystem to record every event that occurs in the system. Browser4 logs the status for every load execution, providing a clear and comprehensive overview of system performance. This detailed logging allows for quick assessment of the system’s health and efficiency. It answers key questions such as: Is the system operating smoothly? How many pages have been successfully retrieved? How many attempts were made to reload pages? And how many proxy IP addresses have been utilized? This information is invaluable for monitoring and troubleshooting purposes, ensuring that any issues can be promptly identified and addressed.

By focusing on a concise set of indicators, you can unlock a deeper understanding of the system’s overall condition: 💯 💔 🗙  ?💿 🔃 🤺.

Typical page loading logs are shown below. Check the [log-format](docs/log-format.md) to learn how to read the logs and gain insight into the state of the entire system at a glance.

```plaintext
2022-09-24 11:46:26.045  INFO [-worker-14] a.p.p.c.c.L.Task - 3313. 💯  ?U for N got 200 580.92 KiB in 1m14.277s, fc:1 | 75/284/96/277/6554 | 106.32.12.75 | 3xBpaR2 | https://www.walmart.com/ip/Restored-iPhone-7-32GB-Black-T-Mobile-Refurbished/329207863  -expires PT24H -ignoreFailure -itemExpires PT1M -outLinkSelector a[href~=/ip/] -parse -requireSize 300000
2022-09-24 11:46:09.190  INFO [-worker-32] a.p.p.c.c.L.Task - 3738. 💯 💿 U got 200 452.91 KiB in 55.286s, last fetched 9h32m50s ago, fc:1 | 49/171/82/238/6172 | 121.205.2.0.5 | https://www.walmart.com/ip/Boost-Mobile-Apple-iPhone-SE-2-Cell-Phone-Black-64GB-Prepaid-Smartphone/490934488  -expires PT24H -ignoreFailure -itemExpires PT1M -outLinkSelector a[href~=/ip/] -parse -requireSize 300000
2022-09-24 11:46:28.567  INFO [-worker-17] a.p.p.c.c.L.Task - 2269. 💯 🔃 U for SC got 200 565.07 KiB <- 543.41 KiB in 1m22.767s, last fetched 16m58s ago, fc:6 | 58/230/98/295/6272 | 27.158.125.76 | 9uwu602 | https://www.walmart.com/ip/Straight-Talk-Apple-iPhone-11-64GB-Purple-Prepaid-Smartphone/356345388?variantFieldId=actual_color  -expires PT24H -ignoreFailure -itemExpires PT1M -outLinkSelector a[href~=/ip/] -parse -requireSize 300000
2022-09-24 11:47:18.390  INFO [r-worker-8] a.p.p.c.c.L.Task - 3732. 💔  ?U for N got 1601 0 <- 0 in 32.201s, fc:1/1 Retry(1601) rsp: CRAWL, rrs: EMPTY_0B | 2zYxg52 | https://www.walmart.com/ip/Apple-iPhone-7-256GB-Jet-Black-AT-T-Locked-Smartphone-Grade-B-Used/182353175?variantFieldId=actual_color  -expires PT24H -ignoreFailure -itemExpires PT1M -outLinkSelector a[href~=/ip/] -parse -requireSize 300000
2022-09-24 11:47:13.860  INFO [-worker-60] a.p.p.c.c.L.Task - 2828. 🗙 🗙 U for SC got 200 0 <- 348.31 KiB <- 684.75 KiB in 0s, last fetched 18m55s ago, fc:2 | 34/130/52/181/5747 | 60.184.124.232 | 11zTa0r2 | https://www.walmart.com/ip/Walmart-Family-Mobile-Apple-iPhone-11-64GB-Black-Prepaid-Smartphone/209201965?athbdg=L1200  -expires PT24H -ignoreFailure -itemExpires PT1M -outLinkSelector a[href~=/ip/] -parse -requireSize 300000
```
# 💻 System Requirements

Each time you access this rotation URL, it should return a response containing one or more fresh proxy IPs.
If you need this type of URL, please contact your proxy service provider.
- Memory 4G+
- JDK 17+
- `java` on the PATH
- Latest Google Chrome
- [Optional] MongoDB started

</details>
Browser4 is tested on Ubuntu 18.04, Ubuntu 20.04, Windows 7, Windows 11, WSL, and any other operating system that meets the requirements should work as well.

---
# 🛸 Advanced Topics

## License
Check the [advanced topics](docs/faq/advanced-topics.md) to find out the answers for the following questions:

Apache 2.0 License. See [LICENSE](LICENSE) for details.
- What’s so difficult about scraping web data at scale?
- How to scrape a million product pages from an e-commerce website a day?
- How to scrape pages behind a login?
- How to download resources directly within a browser context?
- How to scrape a single page application (SPA)?
- Resource mode
- RPA mode
- How to make sure all fields are extracted correctly?
- How to crawl paginated links?
- How to crawl newly discovered links?
- How to crawl the entire website?
- How to simulate human behaviors?
- How to schedule priority tasks?
- How to start a task at a fixed time point?
- How to drop a scheduled task?
- How to know the status of a task?
- How to know what's going on in the system?
- How to automatically generate the CSS selectors for fields to scrape?
- How to extract content from websites using machine learning automatically with commercial accuracy?
- How to scrape amazon.com to match industrial needs?

# 🆚 Compare with Other Solutions

In general, the features mentioned in the Feature section are well-supported by Browser4, but other solutions do not.

Check the [solution comparison](docs/faq/solution-comparison.md) to see the detailed comparison to the other solutions:

- Browser4 vs selenium/puppeteer/playwright
- Browser4 vs nutch
- Browser4 vs scrapy+splash

# 🤓 Technical Details

Check the [technical details](docs/faq/technical-details.md) to see answers for the following questions:

- How to rotate my IP addresses?
- How to hide my bot from being detected?
- How & why to simulate human behaviors?
- How to render as many pages as possible on a single machine without being blocked?

# 🐦 Contact

- Wechat: galaxyeye
- Weibo: [galaxyeye](https://weibo.com/galaxyeye)
- Email: galaxyeye@live.cn, ivincent.zhang@gmail.com
- Twitter: galaxyeye8
- Website: [platon.ai](http://platon.ai)

