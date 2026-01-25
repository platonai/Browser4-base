# AI Copilot 使用与编写指南（简版） (v2026-01-25)

—

## 0) 快速开始（最常用的 3 条命令）

> 目标：在**不猜命令、不踩平台坑**的前提下，最快完成构建/测试。

- 首次构建（不跑测试）
  - Windows (PowerShell)：
    ```powershell
    .\mvnw.cmd -q -DskipTests
    ```
  - Windows (cmd)：
    ```bat
    mvnw.cmd -q -DskipTests
    ```
  - Linux/macOS：
    ```bash
    chmod +x mvnw
    ./mvnw -q -DskipTests
    ```

- 仅验证核心模块单测（推荐本地快速回归）
  - Windows (PowerShell)：
    ```powershell
    .\mvnw.cmd -pl pulsar-core -am test -D"surefire.failIfNoSpecifiedTests=false"
    ```
  - Windows (cmd)：
    ```bat
    mvnw.cmd -pl pulsar-core -am test -D"surefire.failIfNoSpecifiedTests=false"
    ```
  - Linux/macOS：
    ```bash
    ./mvnw -pl pulsar-core -am test -Dsurefire.failIfNoSpecifiedTests=false
    ```

- 推荐：使用仓库脚本（会自动选择平台/参数习惯）
  - Windows (PowerShell)：`bin/build.ps1 [-test]`
  - Linux/macOS：`bin/build.sh [-test]`

### 跨平台环境探测（可选）

- bash/zsh:
  ```bash
  if [[ "$OS" == "Windows_NT" ]]; then
    cmd /c mvnw.cmd -q -DskipTests
  else
    ./mvnw -q -DskipTests
  fi
  ```

- PowerShell:
  ```powershell
  if ($IsWindows) { .\mvnw.cmd -q -D"skipTests" } else { ./mvnw -q -DskipTests }
  ```

## 1) 概览

- 仓库：多模块 Maven（**统一使用根目录 Maven Wrapper：`./mvnw` / `mvnw.cmd`**）
- 语言：Kotlin 优先，兼容 Java
- 原则：最小改动、保持风格、清晰日志、自动校验与测试

## 2) 环境与构建

- Maven Wrapper（强制约定）
  - Windows：`mvnw.cmd ...`（PowerShell 下建议写 `./mvnw.cmd ...`）
  - Linux/macOS：`./mvnw ...`

- 常用构建命令
  - Windows（cmd.exe）：
    - `mvnw.cmd -q -DskipTests`
    - `mvnw.cmd -pl pulsar-core -am test -D"surefire.failIfNoSpecifiedTests=false"`
  - Windows（PowerShell）：
    - `.\mvnw.cmd -q -D"skipTests"`
    - `.\mvnw.cmd -pl pulsar-core -am test -D"surefire.failIfNoSpecifiedTests=false"`
  - Linux/macOS：
    - `./mvnw -q -DskipTests`
    - `./mvnw -pl pulsar-core -am test -Dsurefire.failIfNoSpecifiedTests=false`

- 推荐脚本：
  - Windows：`bin/build.ps1 [-test]`
  - Linux/macOS：`bin/build.sh [-test]`

- 重要提示（Windows 参数转义）
  - Windows 下 `-D` 参数**建议加引号**，例如：`-D"dot.separated.parameter=quoted"`

## 3) 项目要点

- 核心 API：`ai/platon/pulsar/core/api/API.kt`，重点：`WebDriver`, `PulsarSession` -> `AgenticSession`
- 模块：
  - `pulsar-core`：核心引擎（会话、调度、DOM、浏览器控制）
  - `pulsar-agentic`：智能体实现，MCP，技能注册
  - `pulsar-rest`：Spring Boot REST/命令入口
  - `sdks/*`：客户端 SDK
  - `browser4/*`：产品聚合（SPA 与打包）
  - 测试：`pulsar-tests` 与 `pulsar-tests-common`
- 会话：`AgenticContexts.createSession()`
- 加载参数：使用 `LoadOptions` 解析 URL 中的 CLI 风格参数
- 浏览器自动化：查看 `ai.platon.pulsar.browser`，API 看 `WebDriver`；实现细节关注 `PageHandler`、`ClickableDOM`
- 智能体：接口看 `AgenticSession`，实现看 `BrowserPerceptiveAgent`
- MCP 工具：看 `MCPTool` 接口与 `MCPToolExecutor`
- SKILL 注册：看 `MCPPluginRegistry`
- 事件总线：`EventBus`，用于解耦监控与扩展，`PulsarEventBus`，用于网页全生命周期事件通知
- 异常重试：Chrome CDP RPC 相关使用现有重试工具，避免日志风暴

## 4) 运行与配置

- 应用端口：默认 8182
- 配置覆盖：使用分层 `application*.properties`；避免在代码中硬编码默认值
- 参考：`docs/config.md` 与 `docs/rest-api-examples.md`

## 5) 日志与性能

- 日志占位符：`logger.info("Task {} finished in {} ms", taskId, cost)`（避免字符串拼接）
- 性能基准：`pulsar-benchmarks` 模块（JMH），按需运行与对比

## 6) Kotlin 风格

- Kotlin：不可变 `data class`、显式返回类型、空安全（`require/check`/`?:`）
- 公共 API 要有 KDoc：摘要/参数/返回/异常
- AI 做任务后，任务文档放到 dev-docs 目录，供后续参考

> KDoc 模板示例：
```kotlin
/**
 * Loads a page and returns its parsed snapshot.
 *
 * @param url The target URL to load.
 * @param options Load options parsed from CLI-like URL params.
 * @return Parsed page snapshot.
 * @throws IllegalArgumentException if url is blank.
 */
fun load(url: String, options: LoadOptions): PageSnapshot {
    require(url.isNotBlank()) { "url must not be blank" }
    // ...existing code...
}
```

## 7) 测试策略

- 位置：各模块 `src/test`，共享在 `pulsar-tests-common`，重型场景在 `pulsar-tests`
- 速度目标：单测 <100ms；集成 <5s；E2E <30s
- 覆盖率：CI 配置启用 Jacoco（全局至少约 70% 指令覆盖）
- 标签：按 `docs/copilot/templates/test-tag-usage.md`
- 命名规则：测试方法名必须使用驼峰命名法（camelCase），禁止使用 Kotlin 的反引号命名法（如 `` `some test name` ``）
- suspend 函数：使用 `suspend fun xxxTest() {}` 替代 `runBlocking { }` 包裹调用
- 默认接受改动：AI 编写的所有测试代码，直接接受，不询问是否接受

## 8) 常见命令速查（按平台复制即可）

- 构建（不跑测）
  - Windows (PowerShell)：`.\\mvnw.cmd -q -D\"skipTests\"`
  - Windows (cmd)：`mvnw.cmd -q -DskipTests`
  - Linux/macOS：`./mvnw -q -DskipTests`

- 核心模块单测
  - Windows (PowerShell)：`.\\mvnw.cmd -pl pulsar-core -am test -D\"surefire.failIfNoSpecifiedTests=false\"`
  - Windows (cmd)：`mvnw.cmd -pl pulsar-core -am test -D\"surefire.failIfNoSpecifiedTests=false\"`
  - Linux/macOS：`./mvnw -pl pulsar-core -am test -Dsurefire.failIfNoSpecifiedTests=false`

- 推荐脚本
  - Windows：`bin/build.ps1 [-test]`
  - Linux/macOS：`bin/build.sh [-test]`

## 9) PR/变更完成定义（DoD）

- 构建与相关测试通过，无新增高噪日志/告警
- 新/变更逻辑：主路径 + 至少 1 个边界用例
- 不提交密钥/私有端点；输入已校验
- 无随意版本漂移（遵守 parent BOM）
- 公共行为/配置变更同步更新文档
- 对潜在性能影响（>≈5%）给出评估或基准

## 10) 故障与排查

- 浏览器/CDP：优先使用 `pulsar-tests` 重型套件复现
- 代理与隐私上下文：保持处理程序幂等、线程安全
- 日志定位：遵循结构化字段，便于筛选

> 常见问题速查
- Linux/macOS：`mvnw` 无执行权限 → `chmod +x mvnw`
- JDK 版本不匹配 → 确保使用仓库要求版本（优先本地 `JAVA_HOME` 指向）
- Windows 参数转义 → `-D"key.with.dots=value"`（为点分参数加引号）
- 端口占用（默认 8182）→ 覆盖配置 `server.port` 或使用 `application-local.properties`
- 日志风暴（CDP 重试）→ 复用现有重试工具并调低日志级别，勿在循环内拼接字符串

—

附：更多细节请查阅
- 本文件同级 `README-AI.md`
- `docs/concepts.md`、`advanced-guides.md`、`rest-api-examples.md`
