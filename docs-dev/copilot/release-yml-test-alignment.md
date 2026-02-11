# Release.yml 测试配置对齐说明

## 修改目的

确保 `release.yml` 中的测试步骤与 `bin/test.sh` 脚本的行为完全一致：
1. "Run Unit Tests (Release)" 效果和 `bin/test.sh fast` 一致
2. "Run Integration Tests (Release)" 效果和 `bin/test.sh it` 一致

## 问题分析

### 原始配置问题

**Run Unit Tests (Release) - 原始配置**:
```yaml
maven_profiles: 'all-modules'
test_excludes: '**integration**,**e2e**'
excluded_groups: 'Slow,Heavy,HeavyTest,E2E,E2ETest,Integration,IntegrationTest,RequiresServer,BenchmarkTest'
run_pulsar_tests: 'false'
```
- 问题1: 使用 `all-modules` profile，包含了额外的模块（browser4, examples, pulsar-tests, sdks）
- 问题2: 使用文件级别的排除 (`test_excludes`)，与 pom.xml 的 JUnit tags 机制不一致
- 问题3: 手动指定 `excluded_groups`，与 pom.xml 默认配置重复且不一致
- 结果: 行为与 `bin/test.sh fast` 不一致

**Run Integration Tests (Release) - 原始配置**:
```yaml
maven_profiles: 'pulsar-tests'
test_excludes: ''
excluded_groups: 'BenchmarkTest,MustRunExplicitly'
run_pulsar_tests: 'true'
```
- 问题1: 使用 `pulsar-tests` profile，只运行 pulsar-tests 模块
- 问题2: `pulsar-tests` profile 覆盖 `excludedGroups` 为 "None"，导致所有测试都运行
- 问题3: 与 `integration-tests` profile 冲突
- 结果: 行为与 `bin/test.sh it` 完全不同

### bin/test.sh 实际行为

**bin/test.sh fast**:
```bash
./mvnw test
```
- 使用默认模块: `pulsar-core`, `pulsar-agentic`, `pulsar-tools`, `pulsar-rest`, `pulsar-bom`
- 使用 pom.xml 默认的 `surefire.excludedGroups`:
  ```
  Slow,Heavy,RequiresServer,RequiresBrowser,RequiresAI,RequiresDocker,
  Integration,E2E,SDK,MustRunExplicitly,
  SkippableLowerLevelTest,TestInfraCheck,OptionalTest,
  IntegrationTest,E2ETest,HeavyTest
  ```
- **不包含** `pulsar-tests` 模块

**bin/test.sh it**:
```bash
./mvnw test -DrunITs=true
```
- 使用默认模块（同上）
- 激活 `integration-tests` profile
- 覆盖 `surefire.excludedGroups` 为: `E2E,SDK,MustRunExplicitly,E2ETest`
- **允许** Integration, Slow, Heavy 标签的测试运行
- **不包含** `pulsar-tests` 模块

## 修改方案

### 修改后的配置

**Run Unit Tests (Release)**:
```yaml
maven_profiles: ''
test_excludes: ''
excluded_groups: ''
run_pulsar_tests: 'false'
timeout_minutes: '45'
```
- 实际命令: `./mvnw test -B`
- 行为: 完全等同于 `bin/test.sh fast`

**Run Integration Tests (Release)**:
```yaml
maven_profiles: ''
test_excludes: ''
excluded_groups: ''
run_pulsar_tests: 'true'
timeout_minutes: '90'
```
- 实际命令: `./mvnw test -DrunITs=true -B`
- 行为: 完全等同于 `bin/test.sh it`

### 核心原则

1. **依赖 pom.xml 配置**: 不在 CI 中重复配置排除规则，使用 pom.xml 的默认配置和 profile
2. **保持一致性**: CI 测试行为应该与开发者本地运行 `bin/test.sh` 的行为完全一致
3. **清晰的语义**: 
   - `run_pulsar_tests: 'false'` → 快速单元测试（默认行为）
   - `run_pulsar_tests: 'true'` → 集成测试（激活 integration-tests profile）

## pom.xml 测试配置说明

### 默认行为（无 profile）

```xml
<surefire.excludedGroups>
    Slow,Heavy,RequiresServer,RequiresBrowser,RequiresAI,RequiresDocker,
    Integration,E2E,SDK,MustRunExplicitly,
    SkippableLowerLevelTest,TestInfraCheck,OptionalTest,
    IntegrationTest,E2ETest,HeavyTest
</surefire.excludedGroups>
```

### integration-tests profile (-DrunITs=true)

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
        <!-- Integration tests can be Slow/Heavy - this is expected behavior
             Integration tests CAN require Browser/AI/Docker for service collaboration
             Only exclude E2E, SDK, and MustRunExplicitly tests -->
        <surefire.excludedGroups>E2E,SDK,MustRunExplicitly,E2ETest</surefire.excludedGroups>
    </properties>
</profile>
```

### 关键区别

| 特性 | 默认（fast） | Integration (it) |
|------|-------------|------------------|
| Slow 测试 | ❌ 排除 | ✅ 运行 |
| Heavy 测试 | ❌ 排除 | ✅ 运行 |
| Integration 测试 | ❌ 排除 | ✅ 运行 |
| RequiresXYZ 测试 | ❌ 排除 | ✅ 运行 |
| E2E 测试 | ❌ 排除 | ❌ 排除 |
| SDK 测试 | ❌ 排除 | ❌ 排除 |
| MustRunExplicitly | ❌ 排除 | ❌ 排除 |

## 验证方法

### 本地验证

```bash
# 测试快速单元测试
bin/test.sh fast

# 测试集成测试
bin/test.sh it

# 两者应该与 CI 行为一致
```

### CI 验证

1. 触发 release workflow
2. 检查 "Run Unit Tests (Release)" 日志，确认：
   - 命令为: `./mvnw test -B`
   - 使用默认模块
   - 使用默认 excludedGroups
3. 检查 "Run Integration Tests (Release)" 日志，确认：
   - 命令为: `./mvnw test -DrunITs=true -B`
   - 使用默认模块
   - 激活 integration-tests profile

## 参考文档

- [TESTING.md](../../TESTING.md) - 测试分类和标签说明
- [bin/test.sh](../../bin/test.sh) - 测试脚本实现
- [pom.xml](../../pom.xml) - Maven 配置和 profile 定义

## 变更历史

- 2026-02-11: 初始版本，修正 release.yml 测试配置
