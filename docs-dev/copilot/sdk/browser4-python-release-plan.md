# browser4-python 发布计划文档

## 📚 文档概览

本目录包含 browser4-python 的完整发布计划文档，为 Python SDK 的首次发布和后续维护提供详细指南。

### 文档清单

| 文档 | 语言 | 描述 | 目标读者 |
|------|------|------|---------|
| [RELEASE_PLAN.md](../sdks/browser4-python/RELEASE_PLAN.md) | 中文 | 完整发布计划（35页） | 发布管理员、维护者 |
| [RELEASE_PLAN.en.md](../sdks/browser4-python/RELEASE_PLAN.en.md) | English | Complete release plan (35 pages) | Release managers, Maintainers |
| [RELEASE_QUICKREF.md](../sdks/browser4-python/RELEASE_QUICKREF.md) | 中文 | 快速参考指南（5页） | 快速查阅、日常发布 |

---

## 🎯 关键亮点

### 版本策略
- **独立版本号**：Python SDK 采用独立于主项目的版本号（当前 0.1.0）
- **语义化版本**：严格遵循 SemVer 2.0.0 规范
- **兼容性标识**：文档中明确标注与 Browser4 服务器的兼容性范围

### 发布流程
1. **准备阶段**：测试、版本更新、文档同步
2. **构建打包**：使用标准 Python 工具链（build + twine）
3. **测试发布**：先发布到 TestPyPI 验证
4. **正式发布**：发布到 PyPI + 创建 GitHub Release
5. **自动化**：完整的 GitHub Actions 工作流

### 自动化发布
- **Trusted Publishing**：无需手动管理 API Token
- **多平台测试**：自动测试 Linux、macOS、Windows × Python 3.9-3.12
- **一键发布**：推送 Git 标签即可触发完整发布流程

---

## 📖 文档内容概览

### [完整发布计划](../sdks/browser4-python/RELEASE_PLAN.md)

**章节结构：**

1. **版本策略** - 版本命名、同步策略、文件管理
2. **发布前准备** - 功能完成、测试清单、文档审查、依赖审查
3. **构建和打包** - 分发包构建、本地测试
4. **发布流程** - TestPyPI 测试、PyPI 正式发布、GitHub Release
5. **自动化发布** - CI/CD 工作流、Trusted Publishing 配置
6. **发布后活动** - 通知公告、监控反馈、问题修复、版本规划
7. **长期维护策略** - 支持政策、发布节奏、向后兼容、安全更新
8. **工具和资源** - 必需工具、可选工具、参考资源
9. **检查清单总结** - 发布前、发布、发布后完整检查清单
10. **故障排查** - 常见问题、回滚策略

**附录：**
- 自动化脚本（release.sh）
- 模板文件（SECURITY.md, CONTRIBUTING.md）

### [快速参考指南](../sdks/browser4-python/RELEASE_QUICKREF.md)

**快速索引：**

1. **快速发布流程** - 30-55分钟完整流程
2. **版本号规则** - 一目了然的版本类型表
3. **版本文件更新清单** - 需要修改的所有文件
4. **发布前检查清单** - 必须和推荐完成的项目
5. **关键命令速查** - 测试、构建、发布、Git 操作
6. **常见问题快速修复** - 故障排除方案
7. **自动化发布** - 三种触发方式
8. **发布后监控** - 验证和检查
9. **紧急回滚** - 问题版本的处理流程

---

## 🚀 首次发布检查清单

在执行首次发布（v0.1.0）前，确保完成以下准备工作：

### 基础设施准备
- [ ] 创建 PyPI 账号：https://pypi.org/account/register/
- [ ] 创建 TestPyPI 账号：https://test.pypi.org/account/register/
- [ ] 在 PyPI 创建项目占位：`browser4-sdk`
- [ ] 配置 PyPI Trusted Publishing（推荐）
- [ ] 安装必需工具：`uv`, `build`, `twine`, `gh`

### 代码准备
- [ ] 创建 `CHANGELOG.md` 文件
- [ ] 创建 `SECURITY.md` 文件（可选，但推荐）
- [ ] 创建 `CONTRIBUTING.md` 文件（可选）
- [ ] 完成所有单元测试和集成测试
- [ ] 代码覆盖率达到 ≥80%
- [ ] 所有示例代码验证可用

### CI/CD 准备
- [ ] 创建 `.github/workflows/python-sdk-release.yml`
- [ ] 配置 GitHub Environments（`pypi`, `testpypi`）
- [ ] 验证 GitHub Actions 工作流语法
- [ ] 执行一次 TestPyPI 测试发布

### 文档准备
- [ ] README.md 完整准确
- [ ] API 文档（docstrings）完整
- [ ] 示例代码可直接运行
- [ ] 兼容性说明清晰

### 社区准备（可选）
- [ ] 准备发布公告文案
- [ ] 准备社交媒体内容
- [ ] 联系相关社区和资源列表

---

## 📋 后续发布流程

首次发布后，后续版本的发布流程将简化为：

### 日常 Bug 修复（Patch 版本）
```bash
# 1. 修复 + 测试
# 2. 更新版本号（0.1.0 → 0.1.1）
# 3. 推送标签
git tag python-sdk-v0.1.1
git push origin python-sdk-v0.1.1
# 4. GitHub Actions 自动完成其余工作
```

### 新功能发布（Minor 版本）
```bash
# 1. 完成功能 + 测试
# 2. 更新文档和示例
# 3. 更新版本号（0.1.0 → 0.2.0）
# 4. 推送标签
git tag python-sdk-v0.2.0
git push origin python-sdk-v0.2.0
# 5. GitHub Actions 自动完成其余工作
```

---

## 🔗 相关资源

### 项目链接
- **GitHub 仓库**：https://github.com/platonai/Browser4
- **Python SDK 目录**：`sdks/browser4-python/`
- **PyPI 项目**：https://pypi.org/project/browser4-sdk/（首次发布后）

### 参考文档
- **主项目发布流程**：[bin/release/README.md](../../../bin/release/README.md)
- **Kotlin SDK**：`sdks/browser4-kotlin/`
- **CI/CD 工作流**：`.github/workflows/release.yml`

### 外部资源
- **Python Packaging Guide**：https://packaging.python.org/
- **PyPI Help**：https://pypi.org/help/
- **Semantic Versioning**：https://semver.org/
- **Keep a Changelog**：https://keepachangelog.com/

---

## 💡 最佳实践建议

### 版本发布节奏
- **Patch 版本**：按需发布（关键 bug 修复、安全更新）
- **Minor 版本**：每 2-3 个月（新功能、改进）
- **Major 版本**：每 6-12 个月（重大变更）

### 质量保证
1. 始终先在 TestPyPI 测试
2. 多 Python 版本兼容性测试
3. 多平台兼容性验证
4. 安全扫描必不可少（`pip-audit`）
5. 保持高代码覆盖率（≥80%）

### 用户体验
1. 保持向后兼容性（Minor 版本）
2. 提前标记废弃 API（至少提前一个 Minor 版本）
3. 提供清晰的迁移指南
4. 及时响应用户反馈
5. 维护详细的 CHANGELOG

---

## 📞 联系方式

- **Issues**：https://github.com/platonai/Browser4/issues
- **Discussions**：https://github.com/platonai/Browser4/discussions
- **Email**：devnull@example.com（更新为实际邮箱）

---

**创建日期**：2026-02-11
**维护者**：Browser4 Team
**状态**：✅ 计划完成，待首次发布
