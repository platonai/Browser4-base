# 测试体系优化方案（优化版）

## 一、测试分层（明确职责边界）

| 层级                     | 目的   | 默认是否执行 | 特点          |
| ---------------------- | ---- | ------ | ----------- |
| **Fast Unit Test**     | 快速反馈 | ✅ 默认   | <5s / 无外部依赖 |
| **Supplementary Test** | 补充信心 | ❌ 显式   | 稳定模块 / 慢    |
| **Integration Test**   | 模块协作 | ❌ 显式   | 需要服务        |
| **E2E Test**           | 用户路径 | ❌ 显式   | 高资源         |
| **SDK Test**           | 对外契约 | ❌ 显式   | 跨语言         |

> **核心原则**
> 👉 `mvn test` = 永远安全、永远快
> 👉 其他测试 = 必须显式 opt-in

---

## 二、测试类型与模块映射（明确“放哪”）

### 1️⃣ 快速单元测试（Fast Unit Test）

**定位**

* 高频运行
* 开发期默认执行
* 严禁依赖外部资源

**位置**

```
<module>/src/test
```

**命令**

```bash
mvn test
```

**约束**

* 默认只运行 `@Tag("Fast")`
* 禁止：

    * 网络
    * Docker
    * 浏览器
    * AI

---

### 2️⃣ 补充测试（Supplementary Test）

**定位**

* 慢 / 稳定模块
* 日常开发不跑

**模块**

```
pulsar-core/pulsar-core-tests
pulsar-tests/pulsar-rest-tests
```

**命令**

```bash
mvn test -DrunCoreTests=true
mvn test -DrunRestTests=true
```

---

### 3️⃣ 集成测试（Integration Test）

**定位**

* 多模块 / 服务协作
* 有真实基础设施

**模块**

```
pulsar-tests/pulsar-it-tests
```

**命令**

```bash
mvn test -DrunITs=true
```

---

### 4️⃣ 端到端测试（E2E Test）

**定位**

* 用户真实路径
* 最慢、最贵

**模块**

```
pulsar-tests/pulsar-e2e-tests
```

**命令**

```bash
mvn test -DrunE2ETests=true
```

---

### 5️⃣ SDK 测试

**模块**

```
sdks/kotlin-sdk-tests
sdks/browser4-sdk-python
```

**命令**

```bash
mvn test -DrunKotlinSDKTests=true
mvn test -DrunPythonSDKTests=true
mvn test -DrunSDKTests=true
```

---

## 三、JUnit 5 Tag 体系（统一、去歧义）

### ✅ 推荐 Tag 体系（正交、可组合）

#### 速度 / 成本

* `Fast`
* `Slow`
* `Heavy`

#### 依赖环境

* `RequiresServer`
* `RequiresBrowser`
* `RequiresAI`

#### 测试层级

* `Unit`
* `Integration`
* `E2E`
* `SDK`

#### 执行策略

* `MustRunExplicitly`
* `SkippableLowerLevel`
* `TestInfraCheck`

---

### 🚫 建议移除 / 合并的概念

| 原 Tag             | 建议              |
| ----------------- | --------------- |
| `E2ETest`         | 用 `E2E`         |
| `HeavyTest`       | 用 `Heavy`       |
| `IntegrationTest` | 用 `Integration` |

---

## 四、Maven 执行模型（Profile + Property）

### 统一设计原则

* **Property 控制“是否启用”**
* **Profile 控制“如何执行”**
* 默认 profile = 只跑 Fast Unit Test

---

### 示例：Surefire / Failsafe 约定

```xml
<properties>
  <runITs>false</runITs>
  <runE2ETests>false</runE2ETests>
  <runSDKTests>false</runSDKTests>
</properties>
```

```xml
<profile>
  <id>integration-tests</id>
  <activation>
    <property>
      <name>runITs</name>
      <value>true</value>
    </property>
  </activation>
  <properties>
    <groups>Integration</groups>
  </properties>
</profile>
```

---

### 推荐执行命令（规范化）

```bash
# 默认（本地开发）
mvn test

# 明确跑集成测试
mvn test -DrunITs=true

# 跑 SDK 全量
mvn test -DrunSDKTests=true

# 仅跑 Integration 且排除慢测试
mvn test -Dgroups="Integration,!Slow"

# CI 中跑全量（不推荐本地）
mvn test -DrunITs=true -DrunE2ETests=true -DrunSDKTests=true
```

> ⚠️ **注意**
> JUnit 5 实际参数是 `-Dgroups` → 内部映射到 `junit.jupiter.tags`

---

## 五、bin 脚本（开发者体验关键）

### 统一入口（强烈推荐）

```bash
bin/test.sh fast
bin/test.sh it
bin/test.sh e2e
bin/test.sh sdk
bin/test.sh all
```

内部映射：

```bash
fast → mvn test
it   → mvn test -DrunITs=true
e2e  → mvn test -DrunE2ETests=true
sdk  → mvn test -DrunSDKTests=true
all  → 显式全开
```

> 这是 **AI Agent / CI / 人类开发者** 的统一接口

---

## 六、CI / GitHub Actions 策略（降成本）

### 推荐拆分

| Workflow              | 触发           | 内容             |
| --------------------- | ------------ | -------------- |
| `unit.yml`            | PR           | Fast Unit Test |
| `integration.yml`     | 手动 / nightly | Integration    |
| `e2e.yml`             | 手动           | E2E            |
| `kotlin-sdk-test.yml` | SDK 变更       | Kotlin SDK     |
| `python-sdk-test.yml` | SDK 变更       | Python SDK     |

---

## 七、关键设计原则总结

> **一句话版本**

* `mvn test` 必须永远安全、快速、可预测
* 所有“贵测试”必须 **显式 opt-in**
* Tag 表达 **测试属性**
* Maven Property 表达 **是否执行**
* Script 是 **唯一入口**
* CI 是 **策略组合器**
