# Maven Failsafe Plugin 评估 - 决策树

```
是否需要引入 maven-failsafe-plugin？
│
├─ 你的模块是什么？
│  │
│  ├─ 核心业务模块 (pulsar-core, pulsar-agentic)
│  │  └─ ❌ NO - 使用 JUnit 5 Tags 足够
│  │
│  ├─ 专用测试模块 (pulsar-it-tests, pulsar-e2e-tests)
│  │  └─ ❌ NO - 物理模块隔离已足够
│  │
│  ├─ SDK 测试模块 (kotlin-sdk-tests)
│  │  └─ ✅ MAYBE - 可选择性试点
│  │     ├─ 优点：用户可 mvn test 快速验证
│  │     └─ 缺点：增加配置复杂度
│  │
│  └─ 性能测试模块 (pulsar-benchmarks)
│     └─ ✅ MAYBE - 可独立执行
│        ├─ 优点：不影响快速反馈循环
│        └─ 缺点：当前架构已足够
│
├─ 你的测试需求是什么？
│  │
│  ├─ 需要多维度分类 (Level + Cost + Environment + Policy)
│  │  └─ ❌ NO - Failsafe 只支持单维度
│  │     改用：JUnit 5 Tags 组合
│  │
│  ├─ 需要在测试前启动外部服务
│  │  └─ ❌ NO - Spring Boot + GitHub Actions 更好
│  │     改用：@SpringBootTest + docker-compose
│  │
│  ├─ 需要分离单测和 IT 的统计
│  │  └─ ❌ NO - 模块隔离已实现
│  │     改用：独立模块 + 独立执行
│  │
│  ├─ 需要在 IT 失败后清理资源
│  │  └─ ❌ NO - JUnit 生命周期足够
│  │     改用：@AfterEach / @AfterAll
│  │
│  └─ 需要传统 Maven 生命周期集成
│     └─ ⚠️ MAYBE - 但要评估成本
│        考虑：是否与现有架构冲突？
│
└─ 你的团队情况如何？
   │
   ├─ 团队熟悉 JUnit 5 Tags
   │  └─ ✅ 保持现状 - 无需引入 Failsafe
   │
   ├─ 团队习惯传统 Maven 实践
   │  └─ ⚠️ 需要培训 - 说明 Tags 优势
   │
   └─ 团队强烈要求使用 Failsafe
      └─ ⚠️ 增量试点 - 仅在 SDK 模块
```

---

## 关键判断标准

### ✅ 适合使用 Failsafe 的信号

1. **测试类型单一**
   - ✅ 只需区分 "单测" vs "集成测试"（无其他维度）
   - ✅ 测试命名遵循严格约定（`*Test.kt` vs `*IT.kt`）

2. **生命周期需求明确**
   - ✅ 必须在 Maven 生命周期中启动/停止外部服务
   - ✅ 服务启动逻辑复杂，无法用 `@SpringBootTest` 处理

3. **组织要求**
   - ✅ 公司有统一的 Failsafe 标准
   - ✅ 团队成员都熟悉 Failsafe 使用

### ❌ 不适合使用 Failsafe 的信号

1. **测试分类复杂**
   - ❌ 需要按多个维度分类（如 Browser4 的四维度）
   - ❌ 需要动态组合条件（如 "Fast 且不需要 AI"）

2. **已有更好方案**
   - ❌ Spring Boot 测试框架已解决生命周期问题
   - ❌ 物理模块隔离已实现统计分离
   - ❌ CI 工具（GitHub Actions）已编排外部服务

3. **成本高于收益**
   - ❌ 需要修改大量 POM 配置
   - ❌ 与现有架构冲突（如 Tag 体系）
   - ❌ 团队学习成本高

---

## Browser4 项目的特殊性

### 为什么 Browser4 不需要 Failsafe？

1. **四维度测试分类**
   ```kotlin
   // Browser4 可以这样查询
   mvn test -Dgroups="Fast"              // 所有快速测试
   mvn test -Dgroups="Integration,Fast"  // 快速的集成测试
   mvn test -DexcludedGroups="RequiresAI" // 不需要 AI 的测试
   
   // Failsafe 做不到这些
   ```

2. **物理模块隔离**
   ```
   pulsar-it-tests/     # 已经是独立模块
   ├── pom.xml          # 独立配置
   └── target/
       └── surefire-reports/  # 独立统计
   
   # 不需要 Failsafe 来隔离
   ```

3. **Spring Boot 自动化**
   ```kotlin
   @SpringBootTest  // 自动启动/停止应用
   class RestAPIIT {
       // 无需 Maven 生命周期管理
   }
   ```

4. **CI 编排成熟**
   ```yaml
   # GitHub Actions 更灵活
   - run: docker-compose up -d    # 并行启动多个服务
   - run: mvn test -DrunITs=true  # 执行测试
   - run: docker-compose down     # 清理资源
   ```

---

## 快速自检表

**如果以下全部为 YES，考虑引入 Failsafe：**
- [ ] 测试只需单维度分类（单测 vs 集成测试）
- [ ] 没有使用 Spring Boot 测试框架
- [ ] 需要 Maven 插件启动/停止外部服务
- [ ] 团队熟悉且认可 Failsafe

**如果以下任一为 YES，不建议引入 Failsafe：**
- [x] 需要多维度测试分类（Browser4 四维度）
- [x] 已使用 Spring Boot `@SpringBootTest`
- [x] 已有专用测试模块实现物理隔离
- [x] CI 工具已编排外部服务
- [x] 团队对 JUnit 5 Tags 满意

**Browser4 结果：** 5/5 不建议引入 ❌

---

## 实施路径

### 路径 A：保持现状（推荐）✅

```bash
# 无需改动，继续使用当前架构
mvn test                      # 单元测试
mvn test -pl pulsar-it-tests  # 集成测试
mvn test -Pall-modules -DrunITs=true  # 全量测试
```

**优势：**
- ✅ 零成本
- ✅ 架构已经优秀
- ✅ 团队无学习负担

### 路径 B：SDK 模块试点（可选）⚠️

```xml
<!-- 仅在 sdks/kotlin-sdk-tests/pom.xml 中添加 -->
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

**试点标准：**
- ✅ 3 个月后评估：是否简化了 SDK 用户体验？
- ⚠️ 如果收益不明显，回滚到 Surefire

### 路径 C：全面引入（不推荐）❌

```bash
# 需要修改所有测试模块的 POM
# 需要协调 Surefire 和 Failsafe 规则
# 需要团队培训新的执行命令
# 与现有 Tag 体系冲突
```

**风险：**
- ❌ 破坏现有架构优势
- ❌ 配置维护负担重
- ❌ 收益不明显

---

## 延伸阅读

- **Browser4 测试哲学**：`TESTING.md`
- **完整评估报告**：`docs-dev/maven-failsafe-plugin-evaluation.md`
- **英文摘要**：`docs-dev/maven-failsafe-plugin-evaluation.en.md`
- **快速参考**：`docs-dev/maven-failsafe-plugin-evaluation.quick-ref.md`

---

**更新日期：** 2026-02-10  
**作者：** GitHub Copilot
