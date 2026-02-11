# Browser4 依赖包升级方案 (Dependency Upgrade Plan)

## 文档信息
- **创建日期**: 2026-01-14
- **项目版本**: 4.5.0
- **目标**: 制定系统化的依赖包升级策略和流程

---

## 1. 项目依赖概况

### 1.1 项目结构
Browser4 是一个多模块 Maven 项目，包含以下核心模块：

**核心模块** (11个主要模块)
- `pulsar-parent`: 父POM，管理插件版本
- `pulsar-dependencies`: 集中管理所有外部依赖版本
- `pulsar-core`: 核心引擎模块
- `pulsar-rest`: REST API模块
- `pulsar-client`: 客户端SDK
- `pulsar-tests-common`: 测试公共模块
- `pulsar-tests`: 集成测试模块
- `pulsar-benchmarks`: 性能基准测试
- `pulsar-bom`: 依赖BOM
- `browser4-agents`: 浏览器智能体
- `browser4-spa`: SPA应用

**总计**: 41个POM文件

### 1.2 当前技术栈版本

#### 语言与运行时
- **Java**: 17
- **Kotlin**: 2.2.21
- **Kotlin Coroutines**: 1.10.2

#### 核心框架
- **Spring Boot**: 4.0.1
- **Ktor**: 3.3.1
- **LangChain4j**: 1.5.0

#### 主要依赖库
| 类别 | 库名 | 当前版本 |
|------|------|----------|
| 工具库 | Guava | 33.2.1-jre |
| 工具库 | Apache Commons IO | 2.16.1 |
| 工具库 | Apache Commons Text | 1.11.0 |
| 工具库 | Apache Commons Compress | 1.26.2 |
| Web | Apache HttpClient | 4.5.13 |
| 解析 | Apache Tika | 2.9.0 |
| 文档 | Apache POI | 5.3.0 |
| NLP | Apache Lucene | 7.3.1 |
| 图算法 | JGraphT | 1.0.1 |
| 监控 | Dropwizard Metrics | 4.2.12 |
| 系统监控 | OSHI | 6.6.6 |
| 测试 | JUnit 5 | 5.12.1 |
| 测试 | MockK | 1.14.6 |
| 测试 | Mockito Kotlin | 5.4.0 |

#### Maven 插件
- **Maven Surefire**: 3.5.4
- **Kotlin Maven Plugin**: 2.2.21
- **Dokka**: 1.9.10
- **OWASP Dependency Check**: 7.0.1 (当前已禁用)
- **Jacoco**: 0.8.11

---

## 2. 依赖升级策略

### 2.1 升级原则

#### P0 - 必须升级 (Critical)
1. **安全漏洞修复**: CVE已公开的高危/严重漏洞
2. **兼容性问题**: 与JDK/Kotlin新版本不兼容
3. **功能阻塞**: 影响核心功能的依赖bug

#### P1 - 应该升级 (High)
1. **性能改进**: 显著性能提升（>10%）
2. **重要功能**: 需要的新功能只在新版本提供
3. **长期支持**: 当前版本即将停止维护

#### P2 - 计划升级 (Medium)
1. **常规更新**: 小版本更新，包含bug修复
2. **依赖链优化**: 减少依赖冲突
3. **生态系统同步**: 与主流版本保持一致

#### P3 - 可选升级 (Low)
1. **文档改进**: 仅文档、示例更新
2. **内部重构**: 对外API无变化的内部优化
3. **实验性功能**: 非核心功能的实验性更新

### 2.2 升级顺序

```
第一阶段：基础设施
  └─> JDK (如需要) → Maven → Maven Plugins

第二阶段：核心框架
  └─> Kotlin → Kotlin Coroutines → Spring Boot

第三阶段：核心依赖
  └─> 按依赖树深度从深到浅

第四阶段：测试框架
  └─> JUnit → MockK → Mockito

第五阶段：其他依赖
  └─> 按优先级分批升级
```

### 2.3 风险评估矩阵

| 风险类型 | 可能性 | 影响 | 缓解措施 |
|---------|-------|------|---------|
| API不兼容 | 中 | 高 | 渐进式升级、充分测试 |
| 性能退化 | 低 | 高 | 性能基准测试对比 |
| 依赖冲突 | 中 | 中 | 使用依赖排除、BOM管理 |
| 安全漏洞 | 低 | 高 | 定期扫描、及时响应 |
| 构建失败 | 低 | 中 | CI/CD自动验证 |

---

## 3. 具体升级计划

### 3.1 安全漏洞修复（P0）

**当前已知问题**:
- Apache Lucene 7.3.1: 较旧版本，建议升级到 9.x
- Apache HttpClient 4.5.13: 建议迁移到 HttpClient 5.x
- Hadoop 2.7.2: 仅依赖配置类，考虑移除或隔离

**行动项**:
1. 启用 OWASP Dependency Check
2. 生成依赖漏洞报告
3. 按优先级修复高危漏洞

### 3.2 核心框架升级（P1）

#### Spring Boot
- **当前**: 4.0.1
- **目标**: 保持稳定，关注 4.0.x 小版本更新
- **注意事项**: Spring Boot 4.0 是新版本，需充分测试

#### Kotlin
- **当前**: 2.2.21
- **目标**: 跟进最新稳定版 2.2.x
- **注意事项**: 关注编译器优化和协程改进

#### Ktor
- **当前**: 3.3.1
- **目标**: 跟进 3.x 最新版本
- **依赖关系**: 与Kotlin版本保持兼容

### 3.3 工具库升级（P2）

#### Apache Commons 系列
所有 Apache Commons 库保持统一升级策略：
- 优先升级到最新稳定版
- 验证向后兼容性
- 关注性能改进

#### Guava
- **当前**: 33.2.1-jre
- **目标**: 保持最新，关注Java 17+优化

#### Apache Tika
- **当前**: 2.9.0
- **目标**: 升级到 2.x 最新版本
- **注意事项**: 测试HTML解析功能

### 3.4 测试框架升级（P2）

#### JUnit 5
- **当前**: 5.12.1
- **目标**: 保持最新 5.x 版本

#### MockK
- **当前**: 1.14.6
- **目标**: 升级到最新版本
- **注意事项**: 验证与Spring MockK集成

### 3.5 构建工具升级（P2）

#### Maven 插件
- Maven Surefire: 保持最新
- Maven Compiler: 跟进Java 17支持
- Dokka: 跟进Kotlin版本

---

## 4. 工具与自动化

### 4.1 Maven Versions Plugin

使用 Maven Versions Plugin 辅助依赖升级：

```bash
# 显示所有可更新的依赖
./mvnw versions:display-dependency-updates

# 显示所有可更新的插件
./mvnw versions:display-plugin-updates

# 显示所有可更新的属性
./mvnw versions:display-property-updates

# 更新某个依赖到指定版本
./mvnw versions:use-dep-version \
  -Dincludes=org.example:dependency-name \
  -DdepVersion=1.2.3

# 更新所有依赖到最新版本（慎用）
./mvnw versions:use-latest-versions
```

**推荐配置** (添加到 `pulsar-parent/pom.xml`):

```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>versions-maven-plugin</artifactId>
    <version>2.18.0</version>
    <configuration>
        <generateBackupPoms>false</generateBackupPoms>
        <rulesUri>file://${project.basedir}/maven-version-rules.xml</rulesUri>
    </configuration>
</plugin>
```

### 4.2 OWASP Dependency Check

启用并配置 OWASP Dependency Check：

**当前状态**: 已配置但禁用 (`<skip>true</skip>`)

**启用方法**:
1. 修改 `pom.xml`，设置 `<skip>false</skip>`
2. 配置检查级别和报告格式

```xml
<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <version>11.2.0</version>
    <configuration>
        <skip>false</skip>
        <failBuildOnCVSS>7</failBuildOnCVSS>
        <format>ALL</format>
        <outputDirectory>${project.build.directory}/dependency-check</outputDirectory>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>check</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

**运行方式**:
```bash
# 仅检查不构建
./mvnw dependency-check:check

# 生成聚合报告
./mvnw dependency-check:aggregate

# 清除本地CVE数据库
./mvnw dependency-check:purge
```

### 4.3 依赖树分析

```bash
# 查看完整依赖树
./mvnw dependency:tree

# 查看特定模块的依赖树
./mvnw -pl pulsar-core dependency:tree

# 查看依赖冲突
./mvnw dependency:tree -Dverbose

# 分析依赖关系
./mvnw dependency:analyze

# 查找未使用的依赖
./mvnw dependency:analyze -DignoreNonCompile=true
```

### 4.4 自动化脚本

创建辅助脚本 `bin/tools/check-dependencies.sh`:

```bash
#!/bin/bash
# 依赖检查脚本

echo "=== Checking for dependency updates ==="
./mvnw versions:display-dependency-updates -q

echo ""
echo "=== Checking for plugin updates ==="
./mvnw versions:display-plugin-updates -q

echo ""
echo "=== Checking for security vulnerabilities ==="
./mvnw dependency-check:check -q

echo ""
echo "=== Analyzing dependencies ==="
./mvnw dependency:analyze -q
```

---

## 5. 升级流程

### 5.1 标准升级流程

```
1. 调研阶段
   ├─ 查看变更日志 (CHANGELOG)
   ├─ 评估影响范围
   ├─ 识别破坏性变更
   └─ 确定升级路径

2. 准备阶段
   ├─ 创建升级分支
   ├─ 备份当前配置
   └─ 准备回滚方案

3. 实施阶段
   ├─ 更新版本号
   ├─ 解决编译错误
   ├─ 适配API变更
   └─ 更新配置文件

4. 测试阶段
   ├─ 单元测试
   ├─ 集成测试
   ├─ 性能测试
   └─ 回归测试

5. 验证阶段
   ├─ Code Review
   ├─ CI/CD验证
   ├─ 安全扫描
   └─ 文档更新

6. 发布阶段
   ├─ 合并主分支
   ├─ 更新版本记录
   ├─ 发布说明
   └─ 监控观察
```

### 5.2 Git 工作流

```bash
# 1. 创建升级分支
git checkout -b upgrade/dependency-name-version

# 2. 进行升级
# ... 修改 pom.xml 文件 ...

# 3. 测试验证
./mvnw clean test

# 4. 提交变更
git add .
git commit -m "deps: upgrade dependency-name from x.y.z to a.b.c"

# 5. 推送并创建PR
git push origin upgrade/dependency-name-version
```

### 5.3 提交信息规范

遵循 Conventional Commits:

```
deps: upgrade <package> from <old> to <new>

- 主要变更点1
- 主要变更点2
- 测试覆盖说明

Breaking Changes: (如有)
- 不兼容变更说明

Refs: #issue-number
```

---

## 6. 测试策略

### 6.1 测试层次

#### Level 1: 编译验证
```bash
./mvnw clean compile -DskipTests
```
**目标**: 确保代码能够成功编译

#### Level 2: 单元测试
```bash
./mvnw test
```
**目标**: 验证核心功能未受影响

#### Level 3: 集成测试
```bash
./mvnw verify
```
**目标**: 验证模块间交互正常

#### Level 4: 性能测试
```bash
./mvnw -pl pulsar-benchmarks exec:java
```
**目标**: 确保性能无显著退化（阈值：±5%）

#### Level 5: E2E测试
```bash
bin/tests/run-e2e-tests.sh
```
**目标**: 验证完整用户场景

### 6.2 测试覆盖率要求

- **新增代码**: ≥ 80%
- **修改代码**: 保持或提升现有覆盖率
- **核心模块**: ≥ 70%（当前要求）

### 6.3 性能基准

在 `pulsar-benchmarks` 模块运行基准测试：

```bash
# 运行所有基准测试
./mvnw -pl pulsar-benchmarks clean verify

# 运行特定基准
./mvnw -pl pulsar-benchmarks exec:java -Dexec.mainClass=BenchmarkRunner
```

**比较方法**:
1. 升级前运行基准，保存结果
2. 升级后运行基准，保存结果
3. 使用 JMH 工具比较结果
4. 性能退化 > 5% 需要说明原因

---

## 7. CI/CD 集成

### 7.1 GitHub Actions 工作流

创建 `.github/workflows/dependency-check.yml`:

```yaml
name: Dependency Security Check

on:
  schedule:
    # 每周一早上执行
    - cron: '0 1 * * 1'
  workflow_dispatch:

jobs:
  dependency-check:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v4

    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: 'maven'

    - name: Run OWASP Dependency Check
      run: ./mvnw dependency-check:check

    - name: Upload Report
      if: always()
      uses: actions/upload-artifact@v4
      with:
        name: dependency-check-report
        path: target/dependency-check-report.html

    - name: Check for High Severity Issues
      run: |
        if grep -q "severity=\"HIGH\"" target/dependency-check-report.xml; then
          echo "High severity vulnerabilities found!"
          exit 1
        fi
```

### 7.2 依赖更新检查

创建 `.github/workflows/dependency-updates.yml`:

```yaml
name: Check Dependency Updates

on:
  schedule:
    # 每月1号执行
    - cron: '0 2 1 * *'
  workflow_dispatch:

jobs:
  check-updates:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v4

    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: 'maven'

    - name: Check for updates
      run: |
        ./mvnw versions:display-dependency-updates > dependency-updates.txt
        ./mvnw versions:display-plugin-updates >> dependency-updates.txt

    - name: Create Issue
      if: success()
      uses: actions/github-script@v7
      with:
        script: |
          const fs = require('fs');
          const updates = fs.readFileSync('dependency-updates.txt', 'utf8');

          await github.rest.issues.create({
            owner: context.repo.owner,
            repo: context.repo.repo,
            title: '📦 Monthly Dependency Update Report',
            body: '## Available Dependency Updates\n\n```\n' + updates + '\n```',
            labels: ['dependencies', 'maintenance']
          });
```

### 7.3 PR 自动检查

在现有 CI 工作流中添加依赖检查步骤：

```yaml
- name: Check Dependency Changes
  if: github.event_name == 'pull_request'
  run: |
    # 检查是否有依赖变更
    if git diff origin/main -- '**/pom.xml' | grep -q '<version>'; then
      echo "Dependency changes detected, running security check..."
      ./mvnw dependency-check:check
    fi
```

---

## 8. 特殊依赖处理

### 8.1 大版本升级

#### Apache Lucene (7.3.1 → 9.x)

**挑战**:
- API 大幅变更
- 索引格式不兼容
- 分析器重构

**策略**:
1. 创建兼容层抽象
2. 渐进式迁移
3. 保持向后兼容（如可能）

**步骤**:
```
阶段1: 评估
  - 识别所有使用点
  - 评估API变更影响
  - 估算工作量

阶段2: 设计
  - 设计适配器模式
  - 定义迁移路径
  - 准备测试用例

阶段3: 实施
  - 更新依赖版本
  - 实现适配器
  - 更新调用代码

阶段4: 验证
  - 功能测试
  - 性能对比
  - 兼容性验证
```

#### Apache HttpClient (4.5.x → 5.x)

**挑战**:
- 包名变更 (org.apache.http → org.apache.hc)
- API 重新设计
- 同步/异步API分离

**策略**:
1. 评估是否可以使用 Ktor 替代
2. 如必须升级，考虑同时保留4.x和5.x
3. 渐进式迁移各模块

### 8.2 移除不必要依赖

#### Hadoop 2.7.2

**当前用途**: 仅用于配置管理 (Configuration类)

**问题**:
- 版本过旧
- 依赖臃肿
- 安全风险

**方案**:
```
方案A: 完全移除
  - 使用 Apache Commons Configuration 替代
  - 或实现简单的配置管理类

方案B: 隔离依赖
  - 创建独立模块
  - 使用类加载器隔离
  - 排除传递依赖
```

**推荐**: 方案A，使用现有的配置框架

### 8.3 Spring Boot 生态依赖

由于使用 Spring Boot 4.0.1（较新版本），需要：

1. **跟随 Spring Boot BOM**:
   - 大部分依赖版本由 Spring Boot 管理
   - 避免手动指定版本
   - 仅在必要时覆盖

2. **关注兼容性**:
   - Spring Boot 4.0 需要 Java 17+
   - 某些第三方库可能尚未兼容

3. **测试覆盖**:
   - Spring Boot 集成测试
   - 自动配置验证
   - Actuator 端点检查

---

## 9. 文档与沟通

### 9.1 升级文档模板

为每次重要升级创建文档：

```markdown
# 升级记录：[依赖名称] [旧版本] → [新版本]

## 基本信息
- **升级日期**: YYYY-MM-DD
- **负责人**: @username
- **相关Issue**: #xxx
- **相关PR**: #xxx

## 升级原因
- [ ] 安全漏洞修复
- [ ] 功能需求
- [ ] 性能改进
- [ ] 版本维护

## 主要变更
1. 变更点1
2. 变更点2
3. ...

## 破坏性变更
- 无 / 列出具体变更

## 测试结果
- [ ] 编译通过
- [ ] 单元测试通过
- [ ] 集成测试通过
- [ ] 性能测试通过
- [ ] 安全扫描通过

## 回滚方案
如果出现问题，执行以下步骤回滚：
1. ...
2. ...

## 参考资料
- [官方发布说明](url)
- [迁移指南](url)
```

### 9.2 依赖版本记录

在 `docs/dependency-versions.md` 维护版本历史：

```markdown
# 依赖版本历史

## 当前版本 (4.5.0)

| 依赖 | 版本 | 更新日期 | 说明 |
|------|------|----------|------|
| Kotlin | 2.2.21 | 2026-01-xx | ... |
| Spring Boot | 4.0.1 | 2026-01-xx | ... |

## 版本变更历史

### 2026-01-xx
- Kotlin: 2.2.20 → 2.2.21
  - 修复编译器bug
  - 性能改进

### 2025-12-xx
- ...
```

### 9.3 定期报告

**月度依赖报告**:
- 可用更新汇总
- 安全漏洞报告
- 建议升级列表

**季度审查**:
- 依赖健康度评估
- 技术债务分析
- 升级计划调整

---

## 10. 最佳实践与注意事项

### 10.1 最佳实践

✅ **DO**:
1. **小步快跑**: 每次只升级少量依赖
2. **充分测试**: 每次升级都要运行完整测试套件
3. **阅读文档**: 仔细阅读 CHANGELOG 和 Migration Guide
4. **保持记录**: 记录升级原因和测试结果
5. **及时升级**: 不要让依赖版本落后太多
6. **自动化优先**: 尽可能使用工具自动化检查
7. **关注安全**: 优先处理安全漏洞
8. **版本锁定**: 明确指定版本，避免浮动版本

### 10.2 注意事项

❌ **DON'T**:
1. **避免盲目升级**: 不要不加测试就升级
2. **避免大批量**: 不要一次升级太多依赖
3. **避免跨大版本**: 尽量避免跨多个大版本升级
4. **避免快照版本**: 生产环境不要使用 SNAPSHOT 版本
5. **避免间接依赖**: 不要依赖传递依赖的内部类
6. **避免版本冲突**: 注意依赖树中的版本冲突
7. **避免遗漏测试**: 不要跳过任何测试阶段

### 10.3 常见问题

**Q1: 升级后构建失败怎么办？**
- 检查编译错误信息
- 查看依赖冲突 (`mvn dependency:tree`)
- 参考官方迁移指南
- 如无法快速解决，先回滚

**Q2: 如何处理依赖冲突？**
```xml
<!-- 方法1: 排除冲突依赖 -->
<dependency>
    <groupId>com.example</groupId>
    <artifactId>library</artifactId>
    <exclusions>
        <exclusion>
            <groupId>conflicting-group</groupId>
            <artifactId>conflicting-artifact</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<!-- 方法2: 使用 dependencyManagement 强制版本 -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>conflicting-group</groupId>
            <artifactId>conflicting-artifact</artifactId>
            <version>desired-version</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

**Q3: 如何验证升级后性能？**
```bash
# 升级前
./mvnw -pl pulsar-benchmarks verify
cp target/jmh-results.json baseline.json

# 升级后
./mvnw -pl pulsar-benchmarks verify
cp target/jmh-results.json upgraded.json

# 对比
jmh-visualizer compare baseline.json upgraded.json
```

---

## 11. 实施时间表（示例）

### 第一季度（Q1）

**Week 1-2: 准备阶段**
- [ ] 启用依赖检查工具
- [ ] 生成依赖报告
- [ ] 识别高优先级升级项

**Week 3-4: 安全修复（P0）**
- [ ] 升级有高危漏洞的依赖
- [ ] 完成测试和验证

**Week 5-8: 核心框架（P1）**
- [ ] Kotlin 小版本更新
- [ ] Spring Boot 补丁更新
- [ ] Ktor 更新

### 第二季度（Q2）

**Week 1-4: 工具库升级（P2）**
- [ ] Apache Commons 系列
- [ ] Guava
- [ ] Jackson

**Week 5-8: 测试框架（P2）**
- [ ] JUnit 5
- [ ] MockK
- [ ] Mockito

### 第三季度（Q3）

**Week 1-6: 大版本升级（P1）**
- [ ] Apache Lucene 7.x → 9.x
- [ ] 测试和验证

**Week 7-8: 技术债务清理**
- [ ] 移除 Hadoop 依赖
- [ ] 清理未使用依赖

### 第四季度（Q4）

**Week 1-4: 优化和完善**
- [ ] 依赖树优化
- [ ] 性能基准测试
- [ ] 文档更新

**Week 5-8: 审查和总结**
- [ ] 年度依赖审查
- [ ] 制定下年度计划

---

## 12. 参考资源

### 官方文档
- [Maven Versions Plugin](https://www.mojohaus.org/versions-maven-plugin/)
- [OWASP Dependency Check](https://owasp.org/www-project-dependency-check/)
- [Spring Boot Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)
- [Kotlin Release Notes](https://kotlinlang.org/docs/releases.html)

### 工具
- [Maven Central Search](https://search.maven.org/)
- [mvnrepository.com](https://mvnrepository.com/)
- [Snyk Vulnerability Database](https://snyk.io/vuln/)
- [CVE Details](https://www.cvedetails.com/)

### 最佳实践
- [Semantic Versioning](https://semver.org/)
- [Keep a Changelog](https://keepachangelog.com/)
- [Conventional Commits](https://www.conventionalcommits.org/)

---

## 附录

### A. Maven Version Rules 示例

创建 `maven-version-rules.xml`:

```xml
<ruleset comparisonMethod="maven"
         xmlns="http://mojo.codehaus.org/versions-maven-plugin/rule/2.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://mojo.codehaus.org/versions-maven-plugin/rule/2.0.0
         http://mojo.codehaus.org/versions-maven-plugin/xsd/rule-2.0.0.xsd">

  <!-- 忽略所有 alpha, beta, rc 版本 -->
  <ignoreVersions>
    <ignoreVersion type="regex">.*[-_\.]alpha[-_\.]?[0-9]*</ignoreVersion>
    <ignoreVersion type="regex">.*[-_\.]beta[-_\.]?[0-9]*</ignoreVersion>
    <ignoreVersion type="regex">.*[-_\.]rc[-_\.]?[0-9]*</ignoreVersion>
    <ignoreVersion type="regex">.*[-_\.]M[0-9]+</ignoreVersion>
    <ignoreVersion type="regex">.*-SNAPSHOT</ignoreVersion>
  </ignoreVersions>

  <!-- 特定依赖规则 -->
  <rules>
    <rule groupId="org.apache.lucene" artifactId="*">
      <!-- 暂时限制在 7.x，等待迁移计划 -->
      <ignoreVersion type="regex">8\..*</ignoreVersion>
      <ignoreVersion type="regex">9\..*</ignoreVersion>
    </rule>
  </rules>
</ruleset>
```

### B. 快速参考命令

```bash
# 依赖管理
./mvnw versions:display-dependency-updates  # 查看可更新依赖
./mvnw versions:display-plugin-updates      # 查看可更新插件
./mvnw versions:use-latest-versions         # 更新到最新版本（慎用）
./mvnw versions:update-properties           # 更新版本属性

# 安全检查
./mvnw dependency-check:check               # 运行安全扫描
./mvnw dependency-check:aggregate           # 聚合报告

# 依赖分析
./mvnw dependency:tree                      # 查看依赖树
./mvnw dependency:analyze                   # 分析依赖使用
./mvnw dependency:list                      # 列出所有依赖

# 测试
./mvnw clean test                           # 单元测试
./mvnw clean verify                         # 包括集成测试
./mvnw -pl pulsar-core test                 # 测试单个模块
```

### C. 检查清单

升级前检查：
- [ ] 阅读 CHANGELOG
- [ ] 查看 Migration Guide
- [ ] 检查已知问题
- [ ] 评估影响范围
- [ ] 准备回滚方案

升级后验证：
- [ ] 编译成功
- [ ] 单元测试通过
- [ ] 集成测试通过
- [ ] 性能测试无退化
- [ ] 安全扫描通过
- [ ] 文档已更新
- [ ] CI/CD 通过

---

**文档维护**:
- 本文档应随着项目演进持续更新
- 每次重要依赖升级后更新相应章节
- 定期（季度）审查和修订

**反馈与改进**:
如有问题或建议，请创建 Issue 或直接联系维护团队。

---
*最后更新: 2026-01-14*
