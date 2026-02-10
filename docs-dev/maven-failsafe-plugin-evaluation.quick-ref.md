# Maven Failsafe Plugin 评估 - 快速参考

## TL;DR

**结论：不建议在 Browser4 项目中引入 maven-failsafe-plugin**

**原因：**
- ✅ 当前的 JUnit 5 Tags 体系比 Failsafe 更强大
- ⚠️ Failsafe 会增加配置复杂度，收益有限
- 🚀 现有架构已经足够好，无需改变

---

## 快速对比

| 需求 | 当前方案 | Failsafe 方案 | 推荐 |
|------|---------|--------------|------|
| 区分单测和集成测试 | `@Tag("Unit")` vs `@Tag("Integration")` | 文件命名 (`*Test.kt` vs `*IT.kt`) | ✅ 当前方案 |
| 选择性执行测试 | `mvn test -Dgroups="Fast"` | `mvn test` / `mvn verify` | ✅ 当前方案 |
| 管理测试生命周期 | `@SpringBootTest` + `@AfterEach` | Maven 生命周期 (`pre-integration-test`) | ✅ 当前方案 |
| 隔离测试统计 | 独立模块 (`pulsar-it-tests/`) | Surefire vs Failsafe 报告 | ✅ 当前方案 |
| CI 服务编排 | GitHub Actions Docker Compose | Maven Docker Plugin | ✅ 当前方案 |

---

## 当前最佳实践

### 开发者日常使用

```bash
# 快速单元测试（默认）
mvn test

# 运行特定模块的测试
mvn test -pl pulsar-core

# 运行集成测试
mvn test -pl pulsar-it-tests

# 运行 E2E 测试
mvn test -pl pulsar-e2e-tests
```

### CI 完整测试

```bash
# 方案 1：使用 Profile
mvn test -Pall-modules -DrunITs=true -DrunE2ETests=true

# 方案 2：逐模块执行
mvn test -pl pulsar-core,pulsar-agentic    # 单元测试
mvn test -pl pulsar-it-tests               # 集成测试
mvn test -pl pulsar-e2e-tests              # E2E 测试
```

### 按 Tag 过滤

```bash
# 运行所有 Fast 测试（包括单测和集成测试）
mvn test -Dgroups="Fast"

# 排除需要 AI 的测试
mvn test -DexcludedGroups="RequiresAI"

# 组合条件
mvn test -Dgroups="Integration,Fast" -DexcludedGroups="RequiresBrowser"
```

---

## Failsafe 的问题

### 1. 无法表达多维度分类

```kotlin
// Browser4 的需求：四个维度同时分类
@Tag("Integration")        // Level: 集成测试
@Tag("Heavy")              // Cost: 慢速
@Tag("RequiresServer")     // Environment: 需要服务器
@Tag("MustRunExplicitly")  // Policy: 必须显式运行

// Failsafe 只能做单维度过滤
<includes>**/*IT.kt</includes>  // 无法区分 Fast IT vs Heavy IT
<groups>Integration</groups>    // 丢失了其他三个维度
```

### 2. 与 Spring Boot 冲突

```kotlin
// 当前：Spring Boot 自动管理生命周期
@SpringBootTest(webEnvironment = RANDOM_PORT)
class RestAPIIT {
    // Spring 自动启动服务，随机端口避免冲突
}

// 如果用 Failsafe + Maven 插件启动服务
// - 端口冲突风险（Maven 启动 vs Spring 启动）
// - 调试困难（到底是哪个插件的问题？）
// - 配置重复（POM 和 @SpringBootTest 都要配置）
```

### 3. 增加配置复杂度

```xml
<!-- 每个测试模块都需要配置 -->
<plugins>
    <!-- Surefire: 排除 IT -->
    <plugin>
        <artifactId>maven-surefire-plugin</artifactId>
        <configuration>
            <excludes><exclude>**/*IT.kt</exclude></excludes>
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
            <includes><include>**/*IT.kt</include></includes>
        </configuration>
    </plugin>
</plugins>
```

---

## 可选场景：SDK 测试

**唯一推荐使用 Failsafe 的场景：SDK 测试模块**

```xml
<!-- sdks/kotlin-sdk-tests/pom.xml -->
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
```

**理由：**
- ✅ SDK 用户可以 `mvn test` 快速验证（不需要启动服务）
- ✅ CI 可以 `mvn verify` 完整测试 SDK 功能
- ⚠️ 仅影响 1 个模块，不影响主项目

---

## 常见问题

### Q: 其他项目都用 Failsafe，为什么我们不用？

**A:** Browser4 的测试架构比传统项目更先进：
- 传统项目：依赖文件名（`*Test.java` vs `*IT.java`）
- Browser4：使用 JUnit 5 Tags 的多维度分类，更灵活

### Q: 不用 Failsafe，如何保证 IT 失败后清理资源？

**A:** 使用 JUnit 5 生命周期钩子：
```kotlin
@AfterEach
fun cleanup() {
    // JUnit 保证即使测试失败也会执行
}
```

### Q: CI 中如何分别统计单测和 IT？

**A:** 通过模块隔离：
```yaml
- run: mvn test -pl '!pulsar-it-tests'  # 单元测试
- run: mvn test -pl pulsar-it-tests     # 集成测试（独立报告）
```

---

## 决策依据

| 评估维度 | 当前方案 | Failsafe | 赢家 |
|---------|---------|----------|------|
| 测试分类灵活性 | 四维度 | 单维度 | 🏆 当前 |
| 生命周期管理 | Spring Boot | Maven | 🏆 当前 |
| 配置复杂度 | 低 | 高 | 🏆 当前 |
| CI 集成 | GitHub Actions | Maven | 🏆 当前 |

**综合评分：** 当前方案 6/7 ✅ vs Failsafe 0/7 ❌

---

## 更多信息

详细评估报告：
- 中文版：`docs-dev/maven-failsafe-plugin-evaluation.md`
- 英文版：`docs-dev/maven-failsafe-plugin-evaluation.en.md`

测试最佳实践：
- `TESTING.md` - 测试分类法和执行策略
- `.github/workflows/ci.yml` - CI 测试执行配置

---

**最后更新：** 2026-02-10  
**作者：** GitHub Copilot
