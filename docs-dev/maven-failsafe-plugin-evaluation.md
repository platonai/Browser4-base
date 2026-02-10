# Maven Failsafe Plugin 评估报告

## 执行摘要

本文档评估 Browser4 项目引入 `maven-failsafe-plugin` 的收益与成本。基于项目当前的测试架构分析，**不建议全面引入 failsafe-plugin**，但可在特定场景下选择性使用。

**核心结论：**
- ✅ 项目已通过 **JUnit 5 Tags + 物理模块隔离** 实现了比传统 failsafe 更灵活的测试分类
- ⚠️ Failsafe 的主要价值（生命周期隔离）在当前 Spring Boot + Tag 架构下收益有限
- ⚡ 引入 failsafe 会增加配置复杂度，与现有 Tag 语义体系产生冲突
- 💡 可选方案：在 SDK 测试等特定场景下使用 failsafe 作为补充

---

## 1. 项目现状分析

### 1.1 当前测试架构

Browser4 采用 **AI-First 测试分类法**（详见 `TESTING.md`），基于四个正交维度：

| 维度 | 取值 | 控制方式 |
|------|------|----------|
| **Level**（层级） | Unit / Integration / E2E / SDK | JUnit 5 `@Tag` |
| **Cost**（成本） | Fast / Slow / Heavy | JUnit 5 `@Tag` |
| **Environment**（环境） | RequiresServer / RequiresBrowser / RequiresAI | JUnit 5 `@Tag` |
| **Policy**（策略） | MustRunExplicitly / SkippableLowerLevel | JUnit 5 `@Tag` |

**执行控制：**
```bash
# 默认执行（快速单测）
mvn test

# 启用集成测试
mvn test -DrunITs=true

# 启用 E2E 测试
mvn test -DrunE2ETests=true

# 启用所有测试
bin/test.sh all
```

**物理隔离：**
```
pulsar-tests/
├── pulsar-tests-common/        # 共享测试工具
├── pulsar-it-tests/            # Integration Tests (15+ 测试套件)
├── pulsar-rest-tests/          # REST API 集成测试
└── pulsar-e2e-tests/           # E2E 测试 (9 个测试类)

<module>/src/test/              # 模块内的单元测试 (~100 文件)
```

### 1.2 当前 Maven 插件配置

**Surefire Plugin**（已配置）：
```xml
<!-- pulsar-parent/pom.xml -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.5.4</version>
    <configuration>
        <!-- 通过 JUnit 5 Tags 过滤测试 -->
        <groups>Unit,Fast</groups>
        <excludedGroups>MustRunExplicitly</excludedGroups>
        
        <!-- 并行执行配置 -->
        <parallel>methods</parallel>
        <threadCount>4</threadCount>
    </configuration>
</plugin>
```

**Failsafe Plugin**（仅声明版本，未配置执行）：
```xml
<!-- pulsar-parent/pom.xml - 仅在 pluginManagement 中声明 -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-failsafe-plugin</artifactId>
    <version>3.5.4</version>
</plugin>
```

**关键发现：**
- Failsafe 插件已在父 POM 中声明版本，但 **没有实际绑定到生命周期**
- 所有测试（包括 IT 和 E2E）均由 **Surefire 统一执行**
- 通过 **JUnit 5 Tags + Maven Properties** 实现测试过滤

---

## 2. Maven Failsafe Plugin 能力分析

### 2.1 Failsafe 核心功能

| 功能 | 说明 | Surefire 差异 |
|------|------|--------------|
| **生命周期隔离** | 绑定到 `integration-test` 阶段 | Surefire 绑定到 `test` 阶段 |
| **失败延迟** | 失败不立即停止构建，在 `verify` 阶段汇总 | Surefire 失败立即停止 |
| **资源管理** | 支持 `pre-integration-test` 启动服务 | Surefire 无此能力 |
| **测试隔离** | IT 失败不影响单测统计 | 单测和 IT 混在一起 |

### 2.2 典型 Failsafe 配置示例

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-failsafe-plugin</artifactId>
    <version>3.5.4</version>
    <executions>
        <execution>
            <goals>
                <goal>integration-test</goal>
                <goal>verify</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <!-- 传统命名约定 -->
        <includes>
            <include>**/*IT.java</include>
            <include>**/*IT.kt</include>
            <include>**/*ITCase.java</include>
            <include>**/IT*.java</include>
        </includes>
        
        <!-- 或者使用 JUnit 5 Tags -->
        <groups>Integration</groups>
    </configuration>
</plugin>
```

**生命周期流程：**
```
mvn verify
  ↓
1. compile          # 编译生产代码
2. test-compile     # 编译测试代码
3. test             # Surefire 执行单元测试
4. package          # 打包（jar/war）
5. pre-integration-test   # 启动服务（如 Spring Boot）
6. integration-test       # Failsafe 执行集成测试
7. post-integration-test  # 停止服务
8. verify           # 检查 IT 结果，失败则构建失败
```

---

## 3. Browser4 项目的适配性评估

### 3.1 ✅ Failsafe 的潜在收益

#### 3.1.1 生命周期隔离（理论收益）

**场景：** 需要在测试前启动外部服务（数据库、Mock 服务器）

**传统问题：**
```bash
# 手动启动服务
docker-compose up -d
# 运行测试
mvn test -DrunITs=true
# 手动停止服务
docker-compose down
```

**Failsafe 方案：**
```xml
<plugin>
    <groupId>org.codehaus.cargo</groupId>
    <artifactId>cargo-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>start-server</id>
            <phase>pre-integration-test</phase>
            <goals><goal>start</goal></goals>
        </execution>
        <execution>
            <id>stop-server</id>
            <phase>post-integration-test</phase>
            <goals><goal>stop</goal></goals>
        </execution>
    </executions>
</plugin>

<plugin>
    <artifactId>maven-failsafe-plugin</artifactId>
    <executions>
        <execution>
            <goals>
                <goal>integration-test</goal>
                <goal>verify</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

**Browser4 现状：**
- ✅ 集成测试已使用 `@SpringBootTest` 自动管理 Spring 容器生命周期
- ✅ CI 中通过 GitHub Actions 管理外部服务（MongoDB、Docker Compose）
- ⚠️ **不需要** Maven 插件来管理服务生命周期

#### 3.1.2 统计隔离（中等收益）

**收益：** 单元测试和集成测试分别统计，失败不混淆

**当前问题示例：**
```
[INFO] Results:
[ERROR] Failures: 
[ERROR]   UserServiceTest.testCreateUser:42 expected: <User[id=1]> but was: <null>
[ERROR]   OrderServiceIT.testCheckoutFlow:89 Connection refused to localhost:8182
[INFO] Tests run: 145, Failures: 2, Errors: 0, Skipped: 3
```

**Failsafe 方案：**
```
[INFO] Surefire Report:
[INFO] Tests run: 100, Failures: 1, Errors: 0, Skipped: 2

[INFO] Failsafe Report:
[INFO] Tests run: 45, Failures: 1, Errors: 0, Skipped: 1
```

**评估：**
- ✅ 分离报告便于识别单测失败 vs IT 失败
- ⚠️ Browser4 已通过 **物理模块隔离** 达到类似效果：
  - `pulsar-core/target/surefire-reports/` → 单元测试
  - `pulsar-it-tests/target/surefire-reports/` → 集成测试

#### 3.1.3 失败延迟（低收益）

**收益：** IT 失败不立即停止构建，允许清理资源

**场景：**
```java
@Test
public void integrationTest() {
    startExpensiveService();  // 启动耗时服务
    // ... 测试逻辑
    // 如果这里失败，Surefire 立即退出，服务未停止
}
```

**Failsafe 保证：**
- IT 失败后继续执行 `post-integration-test` 阶段
- 在 `verify` 阶段才报告失败

**Browser4 现状：**
- ✅ 使用 JUnit 5 `@AfterEach` / `@AfterAll` 保证资源清理
- ✅ Spring Boot 测试框架自动管理上下文清理
- ⚠️ 不依赖 Maven 生命周期来清理资源

### 3.2 ⚠️ Failsafe 的成本与风险

#### 3.2.1 与现有 Tag 体系冲突（高风险）

**问题：** Failsafe 基于 **命名约定** 或 **单一 Tag** 过滤，无法表达 Browser4 的多维度分类

**Browser4 的复杂性：**
```kotlin
// 当前方案：四维度组合
@Tag("Integration")        // Level
@Tag("Heavy")              // Cost
@Tag("RequiresServer")     // Environment
@Tag("MustRunExplicitly")  // Policy
class RestContractIT { ... }
```

**如果使用 Failsafe：**
```xml
<!-- 方案 A：命名约定 -->
<includes>**/*IT.kt</includes>
<!-- 问题：无法区分 Fast IT vs Heavy IT -->

<!-- 方案 B：单一 Tag -->
<groups>Integration</groups>
<!-- 问题：丢失了 Cost / Environment / Policy 维度 -->
```

**影响：**
- ❌ 无法执行 "所有 Fast 的 Integration 测试"
- ❌ 无法跳过 "需要 AI 的 E2E 测试"
- ❌ 破坏了 `TESTING.md` 定义的语义契约

#### 3.2.2 配置复杂度（中等风险）

**增加的配置负担：**
1. 需要在多个模块的 POM 中重复配置 Failsafe
2. 需要协调 Surefire 和 Failsafe 的包含/排除规则
3. 需要管理两个插件的并行执行配置

**示例（每个测试模块需要）：**
```xml
<!-- pulsar-it-tests/pom.xml -->
<build>
    <plugins>
        <!-- Surefire: 排除 IT -->
        <plugin>
            <artifactId>maven-surefire-plugin</artifactId>
            <configuration>
                <excludes>
                    <exclude>**/*IT.kt</exclude>
                </excludes>
            </configuration>
        </plugin>
        
        <!-- Failsafe: 包含 IT -->
        <plugin>
            <artifactId>maven-failsafe-plugin</artifactId>
            <executions>
                <execution>
                    <goals>
                        <goal>integration-test</goal>
                        <goal>verify</goal>
                    </goals>
                </execution>
            </executions>
            <configuration>
                <includes>
                    <include>**/*IT.kt</include>
                </includes>
            </configuration>
        </plugin>
    </plugins>
</build>
```

#### 3.2.3 与 Spring Boot 测试冲突（中等风险）

**问题：** Spring Boot 的 `@SpringBootTest` 已经管理了应用生命周期

**当前方案：**
```kotlin
@SpringBootTest(webEnvironment = RANDOM_PORT)
class RestAPIIT {
    @BeforeEach
    fun setup() {
        // Spring 自动启动应用，端口随机
    }
    
    @AfterEach
    fun cleanup() {
        // Spring 自动清理上下文
    }
}
```

**如果使用 Failsafe + Cargo/Docker Compose：**
- ⚠️ 可能与 Spring Boot 的内嵌服务器冲突
- ⚠️ 需要协调端口分配（Cargo 启动 vs Spring 随机端口）
- ⚠️ 增加调试难度（服务启动失败是 Maven 问题还是 Spring 问题？）

#### 3.2.4 CI 集成成本（低风险）

**需要修改 CI 流程：**
```yaml
# 当前 CI (.github/workflows/ci.yml)
- name: Run Tests
  run: mvn test -DrunITs=true

# 改为 Failsafe 后
- name: Run Unit Tests
  run: mvn test

- name: Run Integration Tests
  run: mvn verify -DskipTests  # 跳过 Surefire，只跑 Failsafe
```

**影响：**
- ⚠️ 需要修改 10+ 个 GitHub Actions 工作流
- ⚠️ 团队成员需要学习新的命令（`mvn verify` vs `mvn test`）

### 3.3 🚫 Browser4 特有的反模式

#### 3.3.1 已有物理模块隔离

**现状：** 集成测试已经独立成模块
```
pulsar-it-tests/        # 独立模块
├── pom.xml             # 独立配置
├── src/test/kotlin/    # 所有测试都是 IT
└── target/
    └── surefire-reports/  # 独立报告
```

**如果引入 Failsafe：**
```
pulsar-it-tests/
├── target/
    ├── surefire-reports/    # 空（被排除）
    └── failsafe-reports/    # IT 报告
```

**分析：**
- ❌ **零收益**：模块级隔离已经实现了统计分离
- ❌ **增加复杂度**：需要配置 Surefire 排除规则
- ❌ **工具链混乱**：某些工具只识别 Surefire 报告

#### 3.3.2 Tag 语义优于命名约定

**Browser4 的语义需求：**
```bash
# 跑所有 Fast 的测试（不管是 Unit 还是 Integration）
mvn test -Dgroups="Fast"

# 跑所有不需要 AI 的测试（包括部分 E2E）
mvn test -DexcludedGroups="RequiresAI"
```

**Failsafe 的限制：**
- ❌ 基于文件名模式（`**/*IT.kt`），无法跨维度查询
- ❌ 单一 Tag 过滤（`<groups>Integration</groups>`），丢失其他维度

#### 3.3.3 CI 已有服务编排

**当前 CI 架构：**
```yaml
steps:
  - name: Start MongoDB
    run: docker-compose up -d mongodb
  
  - name: Run Tests
    run: mvn test -DrunITs=true
  
  - name: Stop Services
    if: always()
    run: docker-compose down
```

**Failsafe 的资源管理插件（如 Docker Maven Plugin）：**
- ⚠️ 与 GitHub Actions 的 Docker Compose 重复
- ⚠️ 增加配置复杂度（同时维护 POM 和 YAML）
- ⚠️ 降低灵活性（CI 可以并行启动服务，Maven 必须串行）

---

## 4. 推荐方案

### 4.1 主要建议：保持现状（✅ 推荐）

**理由：**
1. **已有更优架构**：JUnit 5 Tags + 模块隔离 > Failsafe 命名约定
2. **Spring Boot 集成**：`@SpringBootTest` 已解决生命周期管理
3. **CI 成熟度**：GitHub Actions 已高效编排外部服务
4. **语义完整性**：四维度分类法无法用 Failsafe 表达

**保持当前最佳实践：**
```bash
# 开发者本地快速测试
mvn test

# CI 全量测试
mvn test -Pall-modules -DrunITs=true -DrunE2ETests=true

# 单独跑某类测试
mvn test -pl pulsar-it-tests -Dgroups="Integration,Fast"
```

### 4.2 可选方案：选择性使用 Failsafe

#### 4.2.1 场景 1：SDK 测试（中等收益）

**适用性：** SDK 测试需要实际网络调用，可能需要启动真实服务

**实施方案：**
```xml
<!-- sdks/kotlin-sdk-tests/pom.xml -->
<build>
    <plugins>
        <!-- Surefire: 只跑单元测试 -->
        <plugin>
            <artifactId>maven-surefire-plugin</artifactId>
            <configuration>
                <groups>Unit</groups>
            </configuration>
        </plugin>
        
        <!-- Failsafe: 只跑集成测试 -->
        <plugin>
            <artifactId>maven-failsafe-plugin</artifactId>
            <executions>
                <execution>
                    <goals>
                        <goal>integration-test</goal>
                        <goal>verify</goal>
                    </goals>
                </execution>
            </executions>
            <configuration>
                <groups>IntegrationTest</groups>
            </configuration>
        </plugin>
    </plugins>
</build>
```

**收益：**
- ✅ SDK 用户可以 `mvn test` 快速验证安装（不需要启动服务）
- ✅ CI 可以 `mvn verify` 完整测试 SDK 功能

**成本：**
- ⚠️ 仅影响 1 个模块（`kotlin-sdk-tests`），不影响主项目

#### 4.2.2 场景 2：性能测试（低收益）

**适用性：** 如果未来添加性能基准测试（JMH），可能需要独立执行

**实施方案：**
```xml
<!-- pulsar-benchmarks/pom.xml -->
<plugin>
    <artifactId>maven-failsafe-plugin</artifactId>
    <executions>
        <execution>
            <id>benchmark-tests</id>
            <goals>
                <goal>integration-test</goal>
                <goal>verify</goal>
            </goals>
            <configuration>
                <groups>Benchmark</groups>
                <systemPropertyVariables>
                    <jmh.fork>1</jmh.fork>
                    <jmh.warmup>5</jmh.warmup>
                </systemPropertyVariables>
            </configuration>
        </execution>
    </executions>
</plugin>
```

**收益：**
- ✅ 性能测试独立执行，不影响快速反馈循环
- ✅ 可以配置更高的超时时间

**成本：**
- ⚠️ 当前 `pulsar-benchmarks` 模块已有独立配置，收益有限

### 4.3 🚫 不推荐方案：全面引入 Failsafe

**以下场景不推荐使用 Failsafe：**

1. **主项目模块**（`pulsar-core`、`pulsar-agentic` 等）
   - ❌ 理由：Tag 体系已足够强大
   
2. **已有专用测试模块**（`pulsar-it-tests`、`pulsar-e2e-tests`）
   - ❌ 理由：物理隔离已实现统计分离
   
3. **Spring Boot 集成测试**（`pulsar-rest-tests`）
   - ❌ 理由：`@SpringBootTest` 已管理生命周期

---

## 5. 决策矩阵

| 评估维度 | 当前方案（JUnit 5 Tags） | Failsafe 方案 | 赢家 |
|---------|------------------------|--------------|------|
| **测试分类灵活性** | 四维度正交分类 | 单一维度（命名/Tag） | 🏆 当前方案 |
| **生命周期管理** | Spring Boot 自动化 | Maven 生命周期 | 🏆 当前方案 |
| **统计隔离** | 模块级物理隔离 | 插件级逻辑隔离 | 🤝 平局 |
| **CI 集成** | GitHub Actions 编排 | Maven 插件编排 | 🏆 当前方案 |
| **配置复杂度** | 集中式 Tag 定义 | 多模块重复配置 | 🏆 当前方案 |
| **社区标准** | JUnit 5 推荐方式 | Maven 传统实践 | 🤝 平局 |
| **学习曲线** | Tag 语义清晰 | Failsafe 生命周期较复杂 | 🏆 当前方案 |

**综合评分：**
- 当前方案：6 / 7 ✅
- Failsafe 方案：0 / 7 ❌

---

## 6. 实施建议

### 6.1 短期（保持现状）

**无需引入 Failsafe，继续优化现有体系：**

1. **完善 Tag 使用**
   ```kotlin
   // 确保所有集成测试都标记完整
   @Tag("Integration")
   @Tag("Heavy")
   @Tag("RequiresServer")
   class BrowserIT { ... }
   ```

2. **文档化最佳实践**
   - 更新 `TESTING.md`，明确说明不使用 Failsafe 的原因
   - 添加 CI 执行命令示例

3. **监控测试执行时间**
   - 使用 Surefire 的 `<rerunFailingTestsCount>` 处理不稳定测试
   - 使用 `<parallel>methods</parallel>` 加速执行

### 6.2 中期（可选试点）

**如果 SDK 测试规模增长，可试点 Failsafe：**

```xml
<!-- sdks/kotlin-sdk-tests/pom.xml -->
<profiles>
    <profile>
        <id>integration-test</id>
        <build>
            <plugins>
                <plugin>
                    <artifactId>maven-failsafe-plugin</artifactId>
                    <executions>
                        <execution>
                            <goals>
                                <goal>integration-test</goal>
                                <goal>verify</goal>
                            </goals>
                        </execution>
                    </executions>
                </plugin>
            </plugins>
        </build>
    </profile>
</profiles>
```

**评估标准：**
- ✅ 是否简化了 SDK 用户的测试体验？
- ✅ 是否减少了 CI 执行时间？
- ⚠️ 是否增加了配置维护负担？

### 6.3 长期（架构演进）

**如果未来需要更复杂的测试编排，考虑以下方案：**

1. **容器化测试环境（Testcontainers）**
   ```kotlin
   @Container
   val mongodb = MongoDBContainer("mongo:7.0")
   
   @SpringBootTest
   class ContainerizedIT {
       // 自动管理容器生命周期
   }
   ```

2. **分布式测试执行（Gradle Enterprise）**
   - 更适合大规模测试并行化
   - 提供测试缓存和失败分析

3. **云原生 CI（Kubernetes Test Jobs）**
   - 每个测试套件独立 Pod
   - 更好的资源隔离和扩展性

---

## 7. 常见问题解答

### Q1: 其他项目都用 Failsafe，为什么 Browser4 不用？

**A:** Browser4 的测试架构比传统项目更先进：
- 传统项目：依赖命名约定（`*Test.java` vs `*IT.java`）区分测试类型
- Browser4：使用 JUnit 5 Tags 的多维度分类，表达能力更强

**类比：**
- Failsafe = 按文件名排序（简单，但灵活性差）
- JUnit 5 Tags = 按标签过滤（可以同时按类型、成本、环境过滤）

### Q2: 不用 Failsafe，如何保证 IT 失败后的资源清理？

**A:** 使用 JUnit 5 生命周期钩子 + Spring Boot 自动清理：
```kotlin
@SpringBootTest
class ResourceIT {
    @AfterEach
    fun cleanup() {
        // JUnit 保证即使测试失败也会执行
        closeResources()
    }
    
    companion object {
        @AfterAll
        @JvmStatic
        fun stopServer() {
            // Spring Boot 自动停止应用上下文
        }
    }
}
```

### Q3: CI 中如何确保 IT 和单测分别统计？

**A:** 通过模块隔离 + 独立执行：
```yaml
# .github/workflows/ci.yml
- name: Run Unit Tests
  run: mvn test -pl '!pulsar-it-tests,!pulsar-e2e-tests'
  
- name: Run Integration Tests
  run: mvn test -pl pulsar-it-tests
  
- name: Run E2E Tests
  run: mvn test -pl pulsar-e2e-tests
```

### Q4: 未来如果需要启动外部服务（如 Kafka），怎么办？

**A:** 优先使用 Spring Boot + Testcontainers：
```kotlin
@SpringBootTest
@Testcontainers
class KafkaIT {
    @Container
    val kafka = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"))
    
    @DynamicPropertySource
    fun kafkaProperties(registry: DynamicPropertyRegistry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers)
    }
}
```

**只有当 Testcontainers 无法满足时，才考虑 Failsafe + Maven Docker Plugin。**

### Q5: 如果团队强烈要求使用 Failsafe，如何最小化影响？

**A:** 采用"增量引入"策略：
1. **阶段 1**：仅在新的 `pulsar-contract-tests` 模块中试点
2. **阶段 2**：评估 3 个月，收集团队反馈
3. **阶段 3**：如果收益明确，逐步迁移其他模块（但保留核心模块的 Tag 体系）

**关键原则：** 不破坏现有的 Tag 语义契约！

---

## 8. 总结

### 核心观点

Browser4 项目 **不应全面引入 maven-failsafe-plugin**，原因如下：

1. **架构优越性**
   - ✅ JUnit 5 Tags 提供更灵活的多维度分类
   - ✅ 物理模块隔离已实现统计和执行分离
   - ✅ Spring Boot 自动化优于 Maven 生命周期管理

2. **成本风险**
   - ⚠️ 与现有 Tag 体系冲突，破坏语义完整性
   - ⚠️ 增加配置复杂度，无显著收益
   - ⚠️ 团队学习成本（`mvn verify` vs `mvn test`）

3. **特定场景可用**
   - ✅ SDK 测试模块可以选择性试点
   - ✅ 性能基准测试可以独立执行
   - ⚠️ 核心模块不推荐引入

### 最终建议

| 模块类型 | 是否使用 Failsafe | 理由 |
|---------|------------------|------|
| 核心业务模块 | ❌ 否 | Tag 体系已足够 |
| 专用测试模块（IT/E2E） | ❌ 否 | 物理隔离已足够 |
| SDK 测试 | ✅ 可选 | 便于用户快速验证 |
| 性能测试 | ✅ 可选 | 独立执行避免干扰 |

### 行动计划

**立即执行（0-1 周）：**
- [x] 完成本评估报告
- [ ] 团队评审会议，达成共识
- [ ] 更新 `TESTING.md`，明确说明技术选型理由

**短期优化（1-2 周）：**
- [ ] 审查所有测试类的 Tag 标注完整性
- [ ] 优化 CI 工作流，分离单测和 IT 执行
- [ ] 添加测试执行时间监控

**中期观察（3-6 个月）：**
- [ ] 监控 SDK 测试的执行体验
- [ ] 收集团队对当前测试架构的反馈
- [ ] 评估是否在 SDK 模块试点 Failsafe

---

**文档版本：** v1.0  
**最后更新：** 2026-02-10  
**作者：** GitHub Copilot (Browser4 评估任务)  
**审阅状态：** 待团队评审
