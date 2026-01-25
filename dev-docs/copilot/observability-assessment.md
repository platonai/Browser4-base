# Browser4 可观测性评估报告

本文档对Browser4项目的可观测性进行全面评估，涵盖日志(Logging)、指标(Metrics)和追踪(Tracing)三大支柱，以及健康检查、系统监控等方面。

---

## 📋 评估摘要

| 维度 | 成熟度 | 评分(1-5) | 说明 |
|------|--------|-----------|------|
| **日志系统** | 良好 | 4/5 | 完善的结构化日志，多通道分离 |
| **指标收集** | 良好 | 4/5 | Codahale Metrics + Graphite，覆盖广泛 |
| **分布式追踪** | 基础 | 2/5 | CDP Tracing支持，缺乏分布式追踪集成 |
| **健康检查** | 良好 | 4/5 | Spring Actuator + 自定义端点 |
| **系统监控** | 优秀 | 5/5 | OSHI深度集成，硬件资源全覆盖 |

**综合评分: 3.8/5** — 项目具备良好的可观测性基础，适合生产环境使用。

---

## 1. 📝 日志系统 (Logging)

### 1.1 技术栈

- **日志框架**: SLF4J + Logback
- **配置文件**: `pulsar-core/pulsar-resources/src/main/resources/logback.xml`

### 1.2 核心特性

#### 多通道日志分离

项目实现了细粒度的日志分离策略：

```
logs/pulsar.log     - 默认日志
logs/pulsar.pg.log  - 页面加载任务状态
logs/pulsar.m.log   - 指标日志
logs/pulsar.bs.log  - 浏览器日志
logs/pulsar.sql.log - SQL执行日志
logs/pulsar.api.log - API调用日志
logs/pulsar.c.log   - 计数器日志
logs/pulsar.hv.log  - 采集日志
logs/pulsar.dc.log  - 数据收集日志
```

#### 结构化日志支持

`StructuredLogger` 类支持JSON格式的结构化日志输出：

```kotlin
// pulsar-core/pulsar-common/src/main/kotlin/.../StructuredLogger.kt
class StructuredLogger(
    private val ownerLogger: Logger? = null,
    private val enableStructuredLogging: Boolean = false,
    private val target: Any? = null,
) {
    fun info(message: String, additionalData: Map<String, Any> = emptyMap()) {
        // 支持JSON格式和传统格式两种输出
    }
}
```

#### 智能代理日志

`StructuredAgentLogger` 专为AI Agent操作设计：

```kotlin
// 支持observe/extract操作的结构化日志
fun logObserve(instruction: String, requestId: String, resultCount: Int, success: Boolean)
fun logExtract(instruction: String, requestId: String, success: Boolean)
```

#### 日志节流

`ThrottlingLogger` 防止日志风暴：

```kotlin
// 同一消息在TTL时间内只记录一次
class ThrottlingLogger(
    private val logger: Logger,
    private val ttl: Duration = Duration.ofMinutes(30)
)
```

### 1.3 任务状态日志格式

项目设计了丰富的符号体系用于任务状态表示：

| 符号 | 含义 |
|------|------|
| 💯 | 任务成功 |
| 💔 | 任务失败 |
| 🗙 | 任务取消 |
| 🤺 | 任务重试 |
| ⚡ | 首次抓取 |
| 💿 | 从磁盘加载 |
| 🔃 | 更新抓取 |

### 1.4 优势

- ✅ 多通道分离，便于运维筛选
- ✅ 结构化日志支持JSON格式
- ✅ 丰富的上下文信息
- ✅ 日志节流防止风暴
- ✅ Unicode符号直观表示状态

### 1.5 改进建议

- ⚠️ 考虑集成日志聚合系统（如ELK Stack）的格式规范
- ⚠️ 添加MDC（Mapped Diagnostic Context）支持请求追踪
- ⚠️ 结构化日志默认关闭，建议生产环境默认开启

---

## 2. 📊 指标系统 (Metrics)

### 2.1 技术栈

- **核心库**: Dropwizard/Codahale Metrics
- **报告器**: Slf4jReporter, GraphiteReporter
- **存储后端**: Graphite (可选)

### 2.2 核心组件

#### MetricsSystem

```kotlin
// pulsar-core/pulsar-skeleton/src/main/kotlin/.../MetricsSystem.kt
class MetricsSystem(conf: ImmutableConfig) : AutoCloseable {
    val initialDelay = conf.getDuration("metrics.report.initial.delay", Duration.ofMinutes(3))
    val slf4jReportInterval = conf.getDuration("metrics.slf4j.report.interval", Duration.ofMinutes(2))
    val graphiteReportInterval = conf.getDuration("metrics.graphite.report.interval", Duration.ofMinutes(2))
}
```

#### AppMetricRegistry

扩展的指标注册表，支持多种指标类型：

```kotlin
class AppMetricRegistry : MetricRegistry() {
    // 日计数器、小时计数器自动重置
    val dailyCounters = mutableSetOf<Counter>()
    val hourlyCounters = mutableSetOf<Counter>()

    // 多维度指标
    fun multiMetric(obj: Any, name: String): MultiMetric
}
```

### 2.3 采集的指标

#### 系统级指标

| 指标名称 | 类型 | 说明 |
|---------|------|------|
| `startTime` | Gauge | 应用启动时间 |
| `elapsedTime` | Gauge | 运行时长 |
| `availableMemory` | Gauge | 可用内存 |
| `freeSpace` | Gauge | 磁盘可用空间 |
| `runningChromeProcesses` | Gauge | Chrome进程数 |
| `usedMemory` | Gauge | 已用内存 |
| `cpuLoad` | Gauge | CPU负载 |

#### 核心业务指标 (CoreMetrics)

| 指标名称 | 类型 | 说明 |
|---------|------|------|
| `fetchTasks` | Meter | 抓取任务总数 |
| `successFetchTasks` | Meter | 成功任务数 |
| `finishedFetchTasks` | Meter | 完成任务数 |
| `contentBytes` | MultiMetric | 内容字节数 |
| `persists` | MultiMetric | 持久化操作数 |
| `proxies` | Meter | 代理使用数 |
| `pageImages` | Histogram | 页面图片数分布 |
| `pageAnchors` | Histogram | 页面链接数分布 |
| `pageHeights` | Histogram | 页面高度分布 |

#### 隐私上下文指标 (PrivacyContextMetrics)

| 指标名称 | 类型 | 说明 |
|---------|------|------|
| `tasks` | MultiMetric | 任务数 |
| `successes` | MultiMetric | 成功数 |
| `contextLeaks` | MultiMetric | 上下文泄漏数 |
| `leakWarnings` | Meter | 泄漏警告数 |

#### 数据库操作指标

| 指标名称 | 类型 | 说明 |
|---------|------|------|
| `dbGets` | Gauge | 数据库读取次数 |
| `dbGets/s` | Gauge | 数据库读取速率 |
| `dbGetAveMillis` | Gauge | 平均读取时长 |
| `dbPuts` | Gauge | 数据库写入次数 |
| `dbPutAveMillis` | Gauge | 平均写入时长 |

### 2.4 报告输出

#### Slf4j Reporter

定期输出到日志：
```
[GAUGE] CoreMetrics.availableMemory | value=12.5 GiB
[METER] CoreMetrics.fetchTasks | count=1234 m1_rate=15.2 m5_rate=14.8
[HISTOGRAM] CoreMetrics.contentBytes | count=1234 min=1024 max=524288 mean=65536
```

#### Graphite Reporter

支持推送到Graphite时序数据库：
```properties
graphite.server=crawl2
graphite.server.port=2004
graphite.pickled.batch.size=100
```

### 2.5 优势

- ✅ 指标覆盖全面（系统、业务、数据库）
- ✅ 支持多维度聚合（日、小时、总计）
- ✅ 支持Graphite集成
- ✅ 阴影指标机制避免污染显示

### 2.6 改进建议

- ⚠️ 建议添加Prometheus端点支持（Spring Boot Actuator已包含）
- ⚠️ 考虑添加Micrometer适配以支持更多后端
- ⚠️ 缺少告警阈值配置
- ⚠️ 建议添加SLI/SLO指标定义

---

## 3. 🔍 分布式追踪 (Tracing)

### 3.1 现状

项目目前**未集成**主流分布式追踪系统（OpenTelemetry/Jaeger/Zipkin）。

### 3.2 现有能力

#### CDP Tracing

支持Chrome DevTools Protocol的Tracing能力：

```kotlin
// pulsar-core/pulsar-tools/pulsar-browser/src/main/kotlin/.../Tracing.kt
interface Tracing {
    fun start(traceConfig: TraceConfig): Flow<Result<Unit>>
    fun end(): Flow<Result<Unit>>
    fun getCategories(): Flow<Result<List<String>>>
}
```

#### 请求追踪

`PrivacyContext` 中包含简单的请求追踪：
- 请求ID生成
- 任务关联

### 3.3 改进建议

- ⚠️ **高优先级**: 集成OpenTelemetry或类似分布式追踪系统
- ⚠️ 添加Span传播支持
- ⚠️ 实现跨服务调用追踪

---

## 4. 🏥 健康检查 (Health Checks)

### 4.1 技术栈

- **框架**: Spring Boot Actuator
- **自定义端点**: `/health`, `/health/ready`, `/health/live`

### 4.2 端点实现

#### HealthController

```kotlin
// pulsar-rest/src/main/kotlin/.../HealthController.kt
@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class HealthController(private val sessionManager: SessionManager) {

    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(mapOf(
            "status" to "UP",
            "activeSessions" to sessionManager.getActiveSessionCount()
        ))
    }

    @GetMapping("/health/ready")
    fun ready(): ResponseEntity<Map<String, Any>>

    @GetMapping("/health/live")
    fun live(): ResponseEntity<Map<String, Any>>
}
```

### 4.3 Spring Actuator配置

```properties
# browser4-spa/application-spa.properties
management.endpoints.web.exposure.include=*
```

### 4.4 可用端点

| 端点 | 说明 |
|------|------|
| `/health` | 基础健康状态 + 活跃会话数 |
| `/health/ready` | Kubernetes就绪探针 |
| `/health/live` | Kubernetes存活探针 |
| `/actuator/health` | Spring Actuator标准端点 |
| `/actuator/metrics` | 指标端点 |
| `/actuator/info` | 应用信息 |

### 4.5 优势

- ✅ 支持Kubernetes探针
- ✅ Spring Actuator完整集成
- ✅ 自定义业务健康指标

### 4.6 改进建议

- ⚠️ 添加更细粒度的健康检查（数据库、浏览器池、代理池）
- ⚠️ 考虑添加依赖服务健康检查

---

## 5. 🖥️ 系统监控 (System Monitoring)

### 5.1 技术栈

- **硬件监控**: OSHI (Operating System and Hardware Information)
- **核心类**: `AppSystemInfo`

### 5.2 监控能力

#### CPU监控

```kotlin
// 系统CPU负载 [0, 1]
val systemCpuLoad get() = computeSystemCpuLoad()

// 系统负载平均值 (1, 5, 15分钟)
val systemLoadAverage: DoubleArray?

// 临界阈值检查
val isCriticalCPULoad get() = systemCpuLoad > CRITICAL_CPU_THRESHOLD
```

#### 内存监控

```kotlin
val freeMemory get() = Runtime.getRuntime().freeMemory()
val availableMemory: Long? get() = memoryInfo?.available
val usedMemory: Long? get() = mi.total - mi.available
val isCriticalMemory: Boolean get() = am < memoryToReserve
```

#### 磁盘监控

```kotlin
val freeDiskSpaces get() = Runtimes.unallocatedDiskSpaces()
val isCriticalDiskSpace get() = checkIsOutOfDisk()
```

#### 网络监控

```kotlin
fun networkIFsReceivedBytes(): Long {
    return si.hardware.networkIFs.sumOf { it.bytesRecv }
}
```

### 5.3 临界状态检测

```kotlin
// 综合临界状态检测
val isSystemOverCriticalLoad get() =
    isCriticalMemory || isCriticalCPULoad || isCriticalDiskSpace
```

### 5.4 配置项

```kotlin
var CRITICAL_CPU_THRESHOLD = System.getProperty("critical.cpu.threshold")?.toDoubleOrNull() ?: 0.85
var CRITICAL_MEMORY_THRESHOLD_MIB = System.getProperty("critical.memory.threshold.MiB")?.toDouble() ?: 0.0
```

### 5.5 优势

- ✅ 硬件级监控（CPU、内存、磁盘、网络）
- ✅ 自动临界状态检测
- ✅ 可配置阈值
- ✅ 跨平台支持（通过OSHI）

### 5.6 改进建议

- ⚠️ 考虑添加JVM GC监控
- ⚠️ 添加线程池监控

---

## 6. 🔧 配置与集成

### 6.1 指标配置

```properties
metrics.enabled=false  # 默认关闭
metrics.report.initial.delay=PT3M
metrics.csv.report.interval=PT5M
metrics.slf4j.report.interval=PT2M
metrics.graphite.report.interval=PT2M
metrics.counter.report.interval=PT30S
```

### 6.2 Graphite配置

```properties
graphite.server=crawl2
graphite.server.port=2004
graphite.pickled.batch.size=100
```

---

## 7. 📈 整体架构图

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Browser4 可观测性架构                          │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐     │
│  │    Logging      │  │    Metrics      │  │    Tracing      │     │
│  │   (SLF4J +      │  │  (Codahale +    │  │  (CDP Basic)    │     │
│  │    Logback)     │  │   Graphite)     │  │                 │     │
│  └────────┬────────┘  └────────┬────────┘  └────────┬────────┘     │
│           │                    │                    │               │
│  ┌────────▼────────────────────▼────────────────────▼────────┐     │
│  │                    Spring Boot Actuator                    │     │
│  │           /health  /metrics  /info  /env                   │     │
│  └────────────────────────────┬──────────────────────────────┘     │
│                               │                                     │
│  ┌────────────────────────────▼──────────────────────────────┐     │
│  │                   AppSystemInfo (OSHI)                     │     │
│  │        CPU | Memory | Disk | Network | Processes           │     │
│  └───────────────────────────────────────────────────────────┘     │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 8. ✅ 总结与建议

### 8.1 现有优势

1. **完善的日志体系** - 多通道分离、结构化支持、节流机制
2. **丰富的指标采集** - 系统、业务、数据库全覆盖
3. **深度系统监控** - OSHI集成，硬件级监控
4. **Kubernetes就绪** - 标准健康检查端点
5. **生产可用** - 支持Graphite时序数据库

### 8.2 改进优先级

| 优先级 | 改进项 | 预期收益 |
|--------|--------|----------|
| **P0** | 集成OpenTelemetry分布式追踪 | 跨服务调用可追踪 |
| **P1** | 添加Prometheus端点 | 标准化指标导出 |
| **P1** | 细化健康检查（DB/Browser/Proxy） | 故障快速定位 |
| **P2** | 添加告警规则配置 | 主动预警 |
| **P2** | 结构化日志默认开启 | 日志分析效率 |
| **P3** | JVM GC监控 | 性能调优 |
| **P3** | 添加SLI/SLO定义 | 服务级别目标 |

### 8.3 推荐监控栈

对于生产环境，建议采用以下监控栈：

```
日志:     Browser4 → Fluent Bit → Elasticsearch → Kibana
指标:     Browser4 → Prometheus → Grafana
追踪:     Browser4 → OpenTelemetry → Jaeger/Zipkin
告警:     Alertmanager + PagerDuty/Slack
```

---

## 9. 参考资料

- [日志格式说明](../../docs/page-load-log-format.md)
- [配置指南](../../docs/config.md)
- [Spring Boot Actuator文档](https://docs.spring.io/spring-boot/reference/actuator/)
- [Dropwizard Metrics文档](https://metrics.dropwizard.io/)
- [OSHI项目](https://github.com/oshi/oshi)

---

*评估日期: 2026-01-10*
*评估版本: 4.4.0-SNAPSHOT*
