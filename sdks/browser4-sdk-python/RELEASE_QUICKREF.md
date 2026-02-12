# browser4-sdk-python 发布快速参考

这是 [完整发布计划](RELEASE_PLAN.md) 的简化版本，供快速查阅。

## 🚀 快速发布流程

### 1. 准备阶段（15-30分钟）

```bash
cd sdks/browser4-sdk-python

# 1.1 运行测试
uv run pytest -m "not integration" --cov=browser4  # 单元测试
uv run pytest -m integration -v -s                 # 集成测试

# 1.2 更新版本号
vi pyproject.toml        # version = "X.Y.Z"
vi setup.cfg             # version = X.Y.Z
vi browser4/__init__.py  # __version__ = "X.Y.Z"
vi CHANGELOG.md          # 添加变更日志

# 1.3 提交变更
git add .
git commit -m "chore(python-sdk): Prepare release X.Y.Z"
```

### 2. 构建和测试（5-10分钟）

```bash
# 2.1 清理和构建
rm -rf dist/ build/ *.egg-info
uv run python -m build

# 2.2 检查包
twine check dist/*

# 2.3 本地测试
uv venv test-env
source test-env/bin/activate
pip install dist/*.whl
python -c "import browser4; print(browser4.__version__)"
deactivate
```

### 3. 测试发布到 TestPyPI（5分钟）

```bash
# 3.1 上传到 TestPyPI
twine upload --repository testpypi dist/*

# 3.2 从 TestPyPI 测试安装
uv venv test-testpypi
source test-testpypi/bin/activate
pip install --index-url https://test.pypi.org/simple/ \
    --extra-index-url https://pypi.org/simple/ \
    browser4-sdk==X.Y.Z
python -c "import browser4; print(browser4.__version__)"
deactivate
```

### 4. 正式发布（5-10分钟）

```bash
# 4.1 创建 Git 标签
git tag -a python-sdk-vX.Y.Z -m "Release browser4-sdk-python vX.Y.Z"
git push origin python-sdk-vX.Y.Z

# 4.2 上传到 PyPI
twine upload dist/*

# 4.3 创建 GitHub Release
gh release create python-sdk-vX.Y.Z \
  --title "browser4-sdk-python vX.Y.Z" \
  --notes-file CHANGELOG.md \
  dist/*
```

**总耗时：30-55分钟**

---

## 📋 版本号规则

| 类型 | 格式 | 何时使用 | 示例 |
|------|------|---------|------|
| 主版本 | X.0.0 | 不兼容的 API 变更 | 1.0.0, 2.0.0 |
| 次版本 | X.Y.0 | 向后兼容的新功能 | 0.2.0, 0.3.0 |
| 修订版 | X.Y.Z | 向后兼容的 bug 修复 | 0.1.1, 0.1.2 |
| 预发布 | X.Y.Z-suffix.N | 测试版本 | 0.1.0-rc.1 |

**当前版本：0.1.0**
**下一个版本建议：**
- Bug 修复 → 0.1.1
- 新功能 → 0.2.0
- 重大变更 → 1.0.0

---

## 🔧 版本文件更新清单

| 文件 | 修改内容 | 示例 |
|------|---------|------|
| `pyproject.toml` | `version = "X.Y.Z"` | `version = "0.1.0"` |
| `setup.cfg` | `version = X.Y.Z` | `version = 0.1.0` |
| `browser4/__init__.py` | `__version__ = "X.Y.Z"` | `__version__ = "0.1.0"` |
| `CHANGELOG.md` | 添加新版本条目 | `## [0.1.0] - 2026-02-11` |
| `README.md` | 更新安装示例（如需要） | `pip install browser4-sdk==0.1.0` |

---

## ✅ 发布前检查清单

### 必须完成（否则不要发布）

- [ ] 所有测试通过（单元测试 + 集成测试）
- [ ] 代码覆盖率 ≥ 80%
- [ ] 安全扫描无严重漏洞（`pip-audit`）
- [ ] CHANGELOG.md 已更新
- [ ] 版本号在所有文件中一致
- [ ] README.md 准确且可运行
- [ ] TestPyPI 测试通过

### 推荐完成（提升质量）

- [ ] 多 Python 版本测试（3.9-3.12）
- [ ] 多平台测试（Linux, macOS, Windows）
- [ ] 示例代码验证
- [ ] 文档审查
- [ ] 依赖项审查

---

## 🔑 关键命令速查

### 测试
```bash
# 单元测试
uv run pytest -m "not integration"

# 集成测试
uv run pytest -m integration -v -s

# 带覆盖率
uv run pytest --cov=browser4 --cov-report=term-missing

# 安全扫描
uv run pip-audit
```

### 构建
```bash
# 清理
rm -rf dist/ build/ *.egg-info

# 构建
uv run python -m build

# 检查
twine check dist/*
```

### 发布
```bash
# TestPyPI
twine upload --repository testpypi dist/*

# PyPI
twine upload dist/*

# 从 TestPyPI 安装
pip install --index-url https://test.pypi.org/simple/ \
    --extra-index-url https://pypi.org/simple/ \
    browser4-sdk==X.Y.Z

# 从 PyPI 安装
pip install browser4-sdk==X.Y.Z
```

### Git 操作
```bash
# 创建标签
git tag -a python-sdk-vX.Y.Z -m "Release message"

# 推送标签
git push origin python-sdk-vX.Y.Z

# 查看标签
git tag -l "python-sdk-v*"

# 删除本地标签
git tag -d python-sdk-vX.Y.Z

# 删除远程标签
git push origin --delete python-sdk-vX.Y.Z
```

### GitHub Release
```bash
# 创建 Release
gh release create python-sdk-vX.Y.Z \
  --title "browser4-sdk-python vX.Y.Z" \
  --notes-file CHANGELOG.md \
  dist/*

# 预发布版本
gh release create python-sdk-vX.Y.Z-rc.1 \
  --title "browser4-sdk-python vX.Y.Z RC1" \
  --notes "Release candidate" \
  --prerelease \
  dist/*
```

---

## 🚨 常见问题快速修复

### 问题：上传时提示 "File already exists"
```bash
# 原因：版本号已存在
# 解决：增加版本号（不能重复上传相同版本）

# 增加修订号
vi pyproject.toml  # 0.1.0 → 0.1.1
# ... 更新其他文件 ...
python -m build
twine upload dist/*
```

### 问题：TestPyPI 安装失败
```bash
# 确保指定两个索引
pip install --index-url https://test.pypi.org/simple/ \
    --extra-index-url https://pypi.org/simple/ \
    browser4-sdk==X.Y.Z

# TestPyPI 只有项目本身，依赖项从 PyPI 获取
```

### 问题：导入失败 "No module named 'browser4'"
```bash
# 检查包名称
pip list | grep browser4

# 应该看到：browser4-sdk
# 导入时使用：import browser4  # 不是 browser4-sdk
```

### 问题：版本号不一致
```bash
# 检查所有版本号
grep -r "0.1.0" pyproject.toml setup.cfg browser4/__init__.py

# 确保一致后重新构建
rm -rf dist/
python -m build
```

### 问题：构建包含了不需要的文件
```bash
# 检查包内容
unzip -l dist/*.whl
tar -tzf dist/*.tar.gz

# 如果有多余文件，添加到 .gitignore 或创建 MANIFEST.in
```

---

## 🔄 自动化发布（推荐）

### 方式 1：推送标签触发 CI/CD
```bash
git tag python-sdk-v0.1.0
git push origin python-sdk-v0.1.0
# GitHub Actions 自动执行：构建 → 测试 → 发布 → 创建 Release
```

### 方式 2：手动触发工作流
```bash
gh workflow run python-sdk-release.yml \
  -f version=0.1.0 \
  -f dry_run=false
```

### 方式 3：GitHub Web 界面
1. 访问 Actions 页面
2. 选择 "Release Python SDK"
3. 点击 "Run workflow"
4. 填写版本号和选项

---

## 📊 发布后监控

### 检查发布状态
```bash
# PyPI 页面
open https://pypi.org/project/browser4-sdk/

# GitHub Release
open https://github.com/platonai/Browser4/releases

# 下载统计
pypistats recent browser4-sdk
```

### 验证安装
```bash
# 新环境测试
uv venv verify
source verify/bin/activate
pip install browser4-sdk==X.Y.Z
python -c "import browser4; print(browser4.__version__)"
python examples/basic_usage.py
deactivate
rm -rf verify
```

---

## 🆘 紧急回滚

### 如果发现严重问题

1. **在 PyPI 上 yank 问题版本**
   - 访问 https://pypi.org/manage/project/browser4-sdk/releases/
   - 点击问题版本
   - 点击 "Yank"
   - 用户仍可指定版本安装，但不会自动选择

2. **立即发布修复版本**
   ```bash
   # 修复 bug
   vi browser4/client.py
   
   # 增加版本号
   vi pyproject.toml  # 0.1.0 → 0.1.1
   # ... 更新其他文件 ...
   
   # 快速发布
   python -m build
   twine upload dist/*
   ```

3. **通知用户**
   - 在 GitHub Release 中标注问题版本
   - 发布公告通知用户升级
   - 在项目 README 中添加警告

---

## 📞 获取帮助

- **完整文档**：[RELEASE_PLAN.md](RELEASE_PLAN.md)
- **英文版本**：[RELEASE_PLAN.en.md](RELEASE_PLAN.en.md)
- **PyPI 帮助**：https://pypi.org/help/
- **Packaging 指南**：https://packaging.python.org/
- **GitHub Issues**：报告问题和请求帮助

---

**最后更新：2026-02-11**
