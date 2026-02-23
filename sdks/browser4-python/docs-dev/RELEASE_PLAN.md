# browser4-python 发布计划

## 📋 概述

本文档描述 browser4-python 的完整发布流程，包括版本管理、打包、测试、发布和后续维护。

**当前状态：**
- 版本：0.1.0
- 状态：开发中，未正式发布
- Python 版本要求：>=3.9
- 依赖：requests>=2.31.0, beautifulsoup4>=4.12.0

---

## 1. 版本策略

### 1.1 版本命名规则

采用语义化版本 (Semantic Versioning 2.0.0)：`MAJOR.MINOR.PATCH[-SUFFIX]`

- **MAJOR (主版本号)**：不兼容的 API 变更
- **MINOR (次版本号)**：向后兼容的功能新增
- **PATCH (修订号)**：向后兼容的问题修复
- **SUFFIX (可选)**：预发布标识
  - `alpha.N`：内部测试版本
  - `beta.N`：公开测试版本
  - `rc.N`：候选发布版本

**示例版本序列：**
```
0.1.0-alpha.1  → 内部测试
0.1.0-beta.1   → 公开测试
0.1.0-rc.1     → 候选版本
0.1.0          → 正式版本
0.1.1          → Bug 修复
0.2.0          → 新功能
1.0.0          → 重大版本
```

### 1.2 版本号同步策略

browser4-python 遵循**独立版本号**策略，与主项目 Browser4 版本号**解耦**：

| 组件 | 当前版本 | 版本策略 |
|------|---------|---------|
| Browser4 (主项目) | 4.5.0 | Maven 语义化版本 |
| browser4-kotlin | 跟随主项目 | 与主项目同步 |
| browser4-python | 0.1.0 | 独立版本号 |
| browser4-sdk-nodejs | TBD | 独立版本号 |
| browser4-sdk-rust | TBD | 独立版本号 |

**原因：**
1. Python SDK 是独立的客户端库，生命周期与主项目不同
2. 可以根据 Python 生态系统的节奏独立迭代
3. 避免因主项目版本跳跃而造成混淆
4. 遵循 Python 社区惯例（独立包独立版本）

**兼容性标识：**
- 在 README 和文档中明确标注兼容的 Browser4 服务器版本范围
- 例如：`browser4-python 0.1.x` 兼容 `Browser4 4.5.x - 4.6.x`

### 1.3 版本文件管理

需要同步更新版本号的文件：

```
sdks/browser4-python/
├── pyproject.toml        # version = "X.Y.Z"
├── setup.cfg             # version = X.Y.Z
├── browser4/__init__.py  # __version__ = "X.Y.Z"
├── CHANGELOG.md          # 更新变更日志
└── README.md             # 更新版本示例
```

---

## 2. 发布前准备

### 2.1 功能完成清单

- [ ] 所有计划功能已实现并通过代码审查
- [ ] API 接口稳定，无计划中的破坏性变更
- [ ] 所有公共 API 有完整的文档字符串 (docstrings)
- [ ] 示例代码已更新并验证可用

### 2.2 测试清单

#### 单元测试
```bash
cd sdks/browser4-python
uv run pytest -m "not integration" --cov=browser4 --cov-report=term-missing
```

**要求：**
- [ ] 所有单元测试通过
- [ ] 代码覆盖率 ≥ 80%
- [ ] 无测试跳过 (除非有明确原因)

#### 集成测试
```bash
cd sdks/browser4-python
uv run pytest -m integration -v -s
```

**要求：**
- [ ] 所有集成测试通过
- [ ] 测试覆盖所有主要使用场景：
  - Browser4Driver 自动下载和启动
  - PulsarSession 基本操作
  - AgenticSession AI 功能
  - WebDriver 元素交互
  - 错误处理和边界条件

#### 兼容性测试

测试 Python 版本兼容性：
```bash
# 使用 tox 或手动测试
for version in 3.9 3.10 3.11 3.12; do
    echo "Testing Python $version"
    python$version -m pytest
done
```

**要求：**
- [ ] Python 3.9 测试通过
- [ ] Python 3.10 测试通过
- [ ] Python 3.11 测试通过
- [ ] Python 3.12 测试通过

#### 平台测试

**要求：**
- [ ] Linux (Ubuntu 20.04+) 测试通过
- [ ] macOS (11+) 测试通过
- [ ] Windows (10/11) 测试通过

#### 端到端测试

**要求：**
- [ ] 使用真实网站运行所有示例代码
- [ ] 验证 README 快速开始指南
- [ ] 验证所有 examples/ 目录下的示例

### 2.3 文档清单

#### 代码文档
- [ ] 所有公共类和函数有完整的 docstrings
- [ ] Docstrings 遵循 Google 或 NumPy 风格
- [ ] 类型注解完整且准确

#### 用户文档
- [ ] README.md 准确描述安装和使用
- [ ] 快速开始指南可以直接运行
- [ ] API 参考文档完整（或链接到生成的文档）
- [ ] 示例代码覆盖主要使用场景
- [ ] CHANGELOG.md 列出所有变更

#### 文档站点（可选，未来增强）
```bash
cd sdks/browser4-python/docs
mkdocs build
mkdocs serve  # 本地预览
```

### 2.4 依赖项审查

#### 安全扫描
```bash
# 使用 pip-audit 检查已知漏洞
uv pip install pip-audit
uv run pip-audit

# 或使用 safety
uv pip install safety
uv run safety check
```

**要求：**
- [ ] 无已知安全漏洞
- [ ] 所有依赖项版本明确指定
- [ ] 依赖项许可证兼容（MIT/Apache 2.0/BSD）

#### 依赖版本
- [ ] 确认最低版本要求准确（通过测试验证）
- [ ] 避免过于严格的版本限制（除非有兼容性问题）
- [ ] 核心依赖数量最小化

### 2.5 版本更新

#### 更新版本号
```bash
# 手动编辑或使用脚本
vi pyproject.toml    # version = "0.1.0"
vi setup.cfg         # version = 0.1.0
vi browser4/__init__.py  # __version__ = "0.1.0"
```

#### 更新 CHANGELOG.md

创建 `CHANGELOG.md`（如果不存在）：
```markdown
# Changelog

All notable changes to browser4-python will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0] - 2026-02-XX

### Added
- Initial release of browser4-python
- Browser4Driver for automatic server management
- PulsarClient for low-level HTTP API communication
- PulsarSession for page loading and data extraction
- AgenticSession for AI-powered browser automation
- WebDriver for browser control and element interaction
- Comprehensive test suite (unit + integration)
- Examples for common use cases

### Features
- Automatic Browser4.jar download and startup
- Session management (create, delete)
- Page loading with cache control
- CSS selector-based data extraction
- Natural language browser actions (act, run, observe)
- WebDriver-compatible API (click, fill, scroll, etc.)
- Screenshot capture
- JavaScript execution

### Documentation
- Complete README with quick start guide
- API reference for all public classes
- Multiple example scripts
- API comparison with Kotlin SDK

[0.1.0]: https://github.com/platonai/Browser4/releases/tag/python-sdk-v0.1.0
```

#### 提交版本变更
```bash
cd sdks/browser4-python
git add pyproject.toml setup.cfg browser4/__init__.py CHANGELOG.md README.md
git commit -m "chore(python-sdk): Prepare release 0.1.0"
```

---

## 3. 构建和打包

### 3.1 构建分发包

#### 清理旧构建
```bash
cd sdks/browser4-python
rm -rf dist/ build/ *.egg-info
```

#### 使用 build 工具
```bash
# 安装构建工具
uv pip install build

# 构建 wheel 和 sdist
uv run python -m build

# 输出：
# dist/
#   ├── browser4_sdk-0.1.0-py3-none-any.whl  # Wheel 包
#   └── browser4_sdk-0.1.0.tar.gz             # 源码分发包
```

#### 验证包内容
```bash
# 检查 wheel 包内容
unzip -l dist/browser4_sdk-0.1.0-py3-none-any.whl

# 检查 sdist 包内容
tar -tzf dist/browser4_sdk-0.1.0.tar.gz

# 验证元数据
uv pip install pkginfo
pkginfo dist/browser4_sdk-0.1.0-py3-none-any.whl
```

**确认包含：**
- [ ] 所有 Python 源代码文件
- [ ] LICENSE 文件
- [ ] README.md
- [ ] pyproject.toml / setup.cfg
- [ ] 无多余的测试文件或临时文件

### 3.2 本地测试安装

#### 创建干净的虚拟环境
```bash
# 使用 uv
uv venv test-env
source test-env/bin/activate  # Windows: test-env\Scripts\activate

# 或使用 venv
python -m venv test-env
source test-env/bin/activate
```

#### 从构建包安装
```bash
# 安装 wheel
pip install dist/browser4_sdk-0.1.0-py3-none-any.whl

# 验证导入
python -c "import browser4; print(browser4.__version__)"

# 运行快速测试
python -c "
from browser4 import PulsarClient, AgenticSession
print('Import successful!')
"
```

#### 测试卸载和重新安装
```bash
pip uninstall -y browser4-python
pip install dist/browser4_sdk-0.1.0.tar.gz  # 测试 sdist
python -c "import browser4; print(browser4.__version__)"
```

---

## 4. 发布流程

### 4.1 测试发布到 TestPyPI

TestPyPI 是 PyPI 的测试环境，用于验证发布流程。

#### 配置 TestPyPI 凭据
```bash
# 方式 1：使用 .pypirc 文件
cat > ~/.pypirc << EOF
[distutils]
index-servers =
    testpypi
    pypi

[testpypi]
repository = https://test.pypi.org/legacy/
username = __token__
password = pypi-AgEIcHlwaS5vcmc...  # TestPyPI API Token

[pypi]
repository = https://upload.pypi.org/legacy/
username = __token__
password = pypi-AgEIcHlwaS5vcmc...  # PyPI API Token
EOF

chmod 600 ~/.pypirc

# 方式 2：使用环境变量
export TWINE_USERNAME=__token__
export TWINE_PASSWORD=pypi-AgEIcHlwaS5vcmc...
export TWINE_REPOSITORY=testpypi
```

#### 上传到 TestPyPI
```bash
# 安装 twine
uv pip install twine

# 检查包完整性
twine check dist/*

# 上传到 TestPyPI
twine upload --repository testpypi dist/*

# 或者直接指定 URL
twine upload --repository-url https://test.pypi.org/legacy/ dist/*
```

#### 从 TestPyPI 安装验证
```bash
# 创建新的测试环境
uv venv test-testpypi
source test-testpypi/bin/activate

# 从 TestPyPI 安装
pip install --index-url https://test.pypi.org/simple/ \
    --extra-index-url https://pypi.org/simple/ \
    browser4-python==0.1.0

# 验证
python -c "import browser4; print(browser4.__version__)"

# 运行简单测试
python examples/basic_usage.py
```

**验证清单：**
- [ ] 版本号正确
- [ ] 依赖项正确安装
- [ ] 导入无错误
- [ ] 基本功能可用
- [ ] 示例代码可运行

### 4.2 正式发布到 PyPI

⚠️ **警告：发布到 PyPI 后无法删除，只能撤回（yank）。请确保一切就绪。**

#### 最终检查清单
- [ ] TestPyPI 测试全部通过
- [ ] CHANGELOG.md 已更新
- [ ] 版本号已最终确定
- [ ] 代码已合并到主分支
- [ ] Git 标签已创建

#### 创建 Git 标签
```bash
cd /path/to/Browser4
git checkout main  # 或 master
git pull

# 创建带注释的标签
git tag -a python-sdk-v0.1.0 -m "Release browser4-python v0.1.0"

# 推送标签
git push origin python-sdk-v0.1.0
```

#### 上传到 PyPI
```bash
cd sdks/browser4-python

# 确保 dist/ 目录只包含当前版本
rm -rf dist/
uv run python -m build

# 最后一次检查
twine check dist/*

# 上传到 PyPI
twine upload dist/*

# 输出示例：
# Uploading distributions to https://upload.pypi.org/legacy/
# Uploading browser4_sdk-0.1.0-py3-none-any.whl
# Uploading browser4_sdk-0.1.0.tar.gz
# View at: https://pypi.org/project/browser4-python/0.1.0/
```

### 4.3 创建 GitHub Release

#### 通过 GitHub Web 界面
1. 访问 https://github.com/platonai/Browser4/releases/new
2. 选择标签：`python-sdk-v0.1.0`
3. Release 标题：`browser4-python v0.1.0`
4. 描述：从 CHANGELOG.md 复制内容
5. 附件：上传 `dist/` 目录下的文件（可选）
6. 勾选 "Set as the latest release" 或 "Set as a pre-release"
7. 点击 "Publish release"

#### 通过 GitHub CLI
```bash
# 安装 gh CLI (如果需要)
# brew install gh  # macOS
# apt install gh   # Ubuntu

# 认证
gh auth login

# 创建 Release
gh release create python-sdk-v0.1.0 \
  --title "browser4-python v0.1.0" \
  --notes-file sdks/browser4-python/CHANGELOG.md \
  dist/browser4_sdk-0.1.0-py3-none-any.whl \
  dist/browser4_sdk-0.1.0.tar.gz

# 如果是预发布版本
gh release create python-sdk-v0.1.0-rc.1 \
  --title "browser4-python v0.1.0 Release Candidate 1" \
  --notes-file sdks/browser4-python/CHANGELOG.md \
  --prerelease \
  dist/*
```

### 4.4 验证发布

#### 验证 PyPI 页面
- [ ] 访问 https://pypi.org/project/browser4-python/
- [ ] 确认版本号正确
- [ ] 检查项目描述是否正确渲染（README.md）
- [ ] 验证元数据（作者、许可证、项目链接）
- [ ] 确认依赖项列表正确

#### 验证安装
```bash
# 在全新环境中测试
uv venv fresh-install
source fresh-install/bin/activate

# 安装发布的版本
pip install browser4-python==0.1.0

# 验证
python -c "
import browser4
print(f'Version: {browser4.__version__}')
print('✓ Installation successful')
"

# 运行示例
python -c "
from browser4 import PulsarClient
client = PulsarClient(base_url='http://localhost:8182')
print('✓ Import successful')
"
```

---

## 5. 自动化发布 (CI/CD)

### 5.1 GitHub Actions 工作流

创建 `.github/workflows/python-sdk-release.yml`：

```yaml
name: Release Python SDK

on:
  push:
    tags:
      - 'python-sdk-v[0-9]+.[0-9]+.[0-9]+'
      - 'python-sdk-v[0-9]+.[0-9]+.[0-9]+-*'
  workflow_dispatch:
    inputs:
      version:
        description: 'Version to release (e.g., 0.1.0)'
        required: true
        type: string
      dry_run:
        description: 'Dry run (TestPyPI only)'
        required: false
        default: false
        type: boolean

permissions:
  contents: write
  id-token: write  # For PyPI trusted publishing

jobs:
  build:
    name: Build Distribution
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Set up Python
        uses: actions/setup-python@v5
        with:
          python-version: '3.11'

      - name: Install uv
        run: |
          curl -LsSf https://astral.sh/uv/install.sh | sh
          echo "$HOME/.cargo/bin" >> $GITHUB_PATH

      - name: Install build tools
        run: uv pip install --system build twine

      - name: Build package
        run: |
          cd sdks/browser4-python
          python -m build

      - name: Check package
        run: |
          cd sdks/browser4-python
          twine check dist/*

      - name: Upload artifacts
        uses: actions/upload-artifact@v4
        with:
          name: python-package-distributions
          path: sdks/browser4-python/dist/

  test-package:
    name: Test Package Installation
    needs: build
    runs-on: ${{ matrix.os }}
    strategy:
      matrix:
        os: [ubuntu-latest, macos-latest, windows-latest]
        python-version: ['3.9', '3.10', '3.11', '3.12']

    steps:
      - uses: actions/checkout@v4

      - name: Set up Python ${{ matrix.python-version }}
        uses: actions/setup-python@v5
        with:
          python-version: ${{ matrix.python-version }}

      - name: Download artifacts
        uses: actions/download-artifact@v4
        with:
          name: python-package-distributions
          path: dist/

      - name: Test wheel installation
        run: |
          python -m pip install dist/*.whl
          python -c "import browser4; print(f'✓ browser4-python {browser4.__version__}')"

  publish-testpypi:
    name: Publish to TestPyPI
    needs: [build, test-package]
    runs-on: ubuntu-latest
    if: github.event.inputs.dry_run == 'true'

    environment:
      name: testpypi
      url: https://test.pypi.org/project/browser4-python/

    steps:
      - name: Download artifacts
        uses: actions/download-artifact@v4
        with:
          name: python-package-distributions
          path: dist/

      - name: Publish to TestPyPI
        uses: pypa/gh-action-pypi-publish@release/v1
        with:
          repository-url: https://test.pypi.org/legacy/
          packages-dir: dist/

  publish-pypi:
    name: Publish to PyPI
    needs: [build, test-package]
    runs-on: ubuntu-latest
    if: startsWith(github.ref, 'refs/tags/python-sdk-v') && github.event.inputs.dry_run != 'true'

    environment:
      name: pypi
      url: https://pypi.org/project/browser4-python/

    permissions:
      id-token: write  # IMPORTANT: mandatory for trusted publishing

    steps:
      - name: Download artifacts
        uses: actions/download-artifact@v4
        with:
          name: python-package-distributions
          path: dist/

      - name: Publish to PyPI
        uses: pypa/gh-action-pypi-publish@release/v1
        with:
          packages-dir: dist/

  create-release:
    name: Create GitHub Release
    needs: publish-pypi
    runs-on: ubuntu-latest

    permissions:
      contents: write

    steps:
      - uses: actions/checkout@v4

      - name: Download artifacts
        uses: actions/download-artifact@v4
        with:
          name: python-package-distributions
          path: dist/

      - name: Extract version from tag
        id: version
        run: |
          VERSION=${GITHUB_REF#refs/tags/python-sdk-v}
          echo "version=$VERSION" >> $GITHUB_OUTPUT

      - name: Create Release
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        run: |
          cd sdks/browser4-python
          gh release create ${{ github.ref_name }} \
            --title "browser4-python v${{ steps.version.outputs.version }}" \
            --notes-file CHANGELOG.md \
            dist/*
```

### 5.2 PyPI Trusted Publishing 配置

推荐使用 PyPI 的 Trusted Publishing 功能，无需手动管理 API Token。

#### 配置步骤
1. 登录 PyPI: https://pypi.org/
2. 进入项目设置：https://pypi.org/manage/project/browser4-python/settings/
3. 点击 "Publishing" → "Add a new publisher"
4. 填写信息：
   - **PyPI Project Name**: `browser4-python`
   - **Owner**: `platonai`
   - **Repository**: `Browser4`
   - **Workflow name**: `python-sdk-release.yml`
   - **Environment name**: `pypi`
5. 保存

现在 GitHub Actions 可以无需 token 直接发布到 PyPI。

### 5.3 自动化发布触发方式

#### 方式 1：推送 Git 标签（推荐）
```bash
git tag python-sdk-v0.1.0
git push origin python-sdk-v0.1.0
```

#### 方式 2：手动触发工作流
```bash
gh workflow run python-sdk-release.yml \
  -f version=0.1.0 \
  -f dry_run=false
```

#### 方式 3：通过 GitHub Web 界面
1. 访问 Actions 页面
2. 选择 "Release Python SDK" 工作流
3. 点击 "Run workflow"
4. 填写版本号和选项
5. 点击 "Run workflow"

---

## 6. 发布后活动

### 6.1 通知和公告

#### 更新项目文档
- [ ] 在主 README.md 中更新 Python SDK 安装说明
- [ ] 更新 `docs/` 目录中的相关文档
- [ ] 在项目网站（如果有）添加发布公告

#### 社交媒体和社区
- [ ] 发布 Twitter/X 推文
- [ ] 在 Reddit (r/Python, r/webscraping) 分享
- [ ] 在 Hacker News 提交
- [ ] 在 Python Weekly / Awesome Python 等资源列表提交
- [ ] 在项目 Discord/Slack 频道公告

#### 通知用户
```markdown
📢 **browser4-python v0.1.0 发布！**

我们很高兴地宣布 browser4-python 首个正式版本发布！

🎉 **主要特性：**
- 自动 Browser4 服务器管理
- AI 驱动的浏览器自动化
- WebDriver 兼容的 API
- 丰富的数据提取功能

📦 **安装：**
```bash
pip install browser4-python
```

📖 **文档：**
https://github.com/platonai/Browser4/tree/main/sdks/browser4-python

🙏 感谢所有贡献者！
```

### 6.2 监控和反馈

#### PyPI 下载统计
- 监控 PyPI 下载量：https://pypistats.org/packages/browser4-python
- 使用 pypistats 工具：
  ```bash
  pip install pypistats
  pypistats recent browser4-python
  pypistats overall browser4-python --monthly
  ```

#### GitHub 活动监控
- [ ] 监控 Issues（bug 报告、功能请求）
- [ ] 监控 Pull Requests
- [ ] 监控 GitHub Stars 增长
- [ ] 设置 GitHub Watch 通知

#### 用户反馈渠道
- [ ] GitHub Issues：技术问题和 bug 报告
- [ ] GitHub Discussions：一般性讨论和问答
- [ ] Discord/Slack：实时社区支持
- [ ] Email：直接联系维护者

### 6.3 问题修复和补丁版本

如果发现关键 bug，快速发布补丁版本：

```bash
# 1. 修复 bug
vi browser4/client.py

# 2. 更新版本号（PATCH +1）
vi pyproject.toml  # 0.1.0 → 0.1.1
vi setup.cfg
vi browser4/__init__.py

# 3. 更新 CHANGELOG
vi CHANGELOG.md

# 4. 提交和标签
git add .
git commit -m "fix(python-sdk): Fix critical bug in client connection"
git tag python-sdk-v0.1.1
git push origin main python-sdk-v0.1.1

# 5. 自动发布（通过 CI/CD）
# 或手动发布
python -m build
twine upload dist/*
```

### 6.4 下一个版本规划

#### 创建 Roadmap
在 GitHub Project 或 Issues 中规划下一个版本：

**v0.2.0 计划功能：**
- [ ] 完整的 PageEventHandlers 实现
- [ ] 更多示例和教程
- [ ] 性能优化
- [ ] 更好的错误处理
- [ ] CLI 工具支持

#### 更新开发版本
```bash
# 更新到下一个开发版本
vi pyproject.toml  # version = "0.2.0-dev"
vi setup.cfg       # version = 0.2.0.dev0
vi browser4/__init__.py  # __version__ = "0.2.0-dev"

git add .
git commit -m "chore(python-sdk): Bump version to 0.2.0-dev"
git push
```

---

## 7. 长期维护策略

### 7.1 版本支持政策

| 版本类型 | 支持期限 | 支持内容 |
|---------|---------|---------|
| 最新稳定版 | 持续 | 新功能、bug 修复、安全更新 |
| 上一个稳定版 | 6 个月 | 关键 bug 修复、安全更新 |
| 更早版本 | 最佳努力 | 仅安全更新 |

**示例：**
- v0.3.0 发布后
  - v0.3.x：完全支持
  - v0.2.x：6 个月内支持关键修复
  - v0.1.x：仅安全更新

### 7.2 发布节奏

建议的发布节奏：

- **PATCH 版本**：按需发布（bug 修复、安全更新）
- **MINOR 版本**：每 2-3 个月（新功能、改进）
- **MAJOR 版本**：每 6-12 个月（重大变更、API 重构）

### 7.3 向后兼容性

#### 兼容性承诺
- **MAJOR 版本**：允许不兼容的 API 变更
- **MINOR 版本**：必须向后兼容
- **PATCH 版本**：必须 100% 兼容

#### 废弃流程
当需要移除或更改 API 时：

1. **标记为废弃（Deprecation）**
   ```python
   import warnings

   def old_function():
       warnings.warn(
           "old_function is deprecated and will be removed in v2.0.0. "
           "Use new_function instead.",
           DeprecationWarning,
           stacklevel=2
       )
       # ... existing implementation
   ```

2. **文档更新**
   - 在 docstring 中标注 `.. deprecated:: 0.2.0`
   - 在 CHANGELOG 中列出
   - 在 README 中添加迁移指南

3. **保留至少一个 MINOR 版本**
   - v0.2.0：标记废弃，保留功能
   - v0.3.0：继续保留，发出警告
   - v1.0.0：可以移除

### 7.4 安全更新流程

#### 漏洞报告
- 创建 SECURITY.md 文件，说明如何报告安全问题
- 提供安全邮箱：security@example.com
- 不在公开 Issue 中讨论安全问题

#### 安全补丁发布
```bash
# 1. 在私有分支修复
git checkout -b security/CVE-2024-XXXX

# 2. 修复并测试
# ... fix code ...

# 3. 发布补丁版本
# v0.1.1, v0.2.1 (支持的版本)

# 4. 协调公开披露
# 在修复发布后公开漏洞细节
```

---

## 8. 工具和资源

### 8.1 必需工具

| 工具 | 用途 | 安装命令 |
|------|------|---------|
| uv | 包管理和环境管理 | `curl -LsSf https://astral.sh/uv/install.sh \| sh` |
| build | 构建分发包 | `uv pip install build` |
| twine | 上传到 PyPI | `uv pip install twine` |
| pytest | 测试框架 | `uv pip install pytest` |
| gh | GitHub CLI | `brew install gh` / `apt install gh` |

### 8.2 可选工具

| 工具 | 用途 | 安装命令 |
|------|------|---------|
| tox | 多版本测试 | `uv pip install tox` |
| mypy | 类型检查 | `uv pip install mypy` |
| ruff | 代码检查和格式化 | `uv pip install ruff` |
| pip-audit | 安全扫描 | `uv pip install pip-audit` |
| mkdocs | 文档生成 | `uv pip install mkdocs mkdocs-material` |
| pypistats | PyPI 统计 | `pip install pypistats` |

### 8.3 参考资源

#### PyPI 官方文档
- 打包指南：https://packaging.python.org/
- PyPI 用户指南：https://pypi.org/help/
- Trusted Publishing：https://docs.pypi.org/trusted-publishers/

#### 最佳实践
- Python Packaging Authority (PyPA)：https://www.pypa.io/
- Semantic Versioning：https://semver.org/
- Keep a Changelog：https://keepachangelog.com/

#### 社区资源
- Python Packaging Discord：https://discord.gg/pypa
- r/Python：https://reddit.com/r/Python
- Python Weekly：https://www.pythonweekly.com/

---

## 9. 检查清单总结

### 发布前检查清单

#### 代码质量
- [ ] 所有单元测试通过（覆盖率 ≥ 80%）
- [ ] 所有集成测试通过
- [ ] 代码审查完成
- [ ] 无已知的严重 bug
- [ ] 安全扫描通过

#### 文档
- [ ] README.md 完整且准确
- [ ] API 文档完整（docstrings）
- [ ] CHANGELOG.md 更新
- [ ] 示例代码可运行

#### 版本管理
- [ ] 版本号已更新（pyproject.toml, setup.cfg, __init__.py）
- [ ] Git 标签已创建
- [ ] 代码已合并到主分支

#### 构建和打包
- [ ] `python -m build` 成功
- [ ] `twine check dist/*` 通过
- [ ] 本地安装测试通过

#### 测试发布
- [ ] TestPyPI 上传成功
- [ ] 从 TestPyPI 安装验证

### 发布检查清单

- [ ] PyPI 上传成功
- [ ] PyPI 页面显示正确
- [ ] 从 PyPI 安装验证
- [ ] GitHub Release 创建
- [ ] 发布公告发布

### 发布后检查清单

- [ ] 监控 PyPI 下载量
- [ ] 监控 GitHub Issues
- [ ] 回应用户反馈
- [ ] 规划下一个版本
- [ ] 更新项目文档

---

## 10. 故障排查

### 10.1 常见问题

#### 构建失败
```bash
# 问题：找不到模块
# 解决：确保 __init__.py 存在
find browser4 -name __init__.py

# 问题：文件未包含
# 解决：检查 MANIFEST.in 或 pyproject.toml [tool.setuptools]
```

#### 上传失败
```bash
# 问题：403 Forbidden
# 解决：检查 API token 权限和项目名称

# 问题：400 Bad Request - File already exists
# 解决：版本号已存在，需要增加版本号

# 问题：网络超时
# 解决：重试或使用代理
twine upload --repository-url https://upload.pypi.org/legacy/ dist/* --verbose
```

#### 安装失败
```bash
# 问题：依赖冲突
# 解决：放宽依赖版本限制
# pyproject.toml: requests>=2.31.0 而非 requests==2.31.0

# 问题：Python 版本不兼容
# 解决：检查 requires-python 设置
```

### 10.2 回滚策略

#### PyPI 包无法删除
- PyPI 不允许删除已发布的版本
- 可以使用 `yank` 标记版本为不推荐使用
  ```bash
  # 通过 PyPI Web 界面，或使用 API
  twine upload --skip-existing dist/*  # 跳过已存在
  ```

#### 发布错误版本
1. 立即发布修复版本（PATCH +1）
2. 在 PyPI 项目页面 yank 错误版本
3. 在 GitHub Release 中标注错误版本
4. 通知用户升级到修复版本

---

## 附录 A：自动化脚本

### 发布脚本 (release.sh)

创建 `sdks/browser4-python/scripts/release.sh`：

```bash
#!/bin/bash
set -e

VERSION=$1
DRY_RUN=${2:-false}

if [ -z "$VERSION" ]; then
    echo "Usage: ./release.sh <version> [dry_run]"
    echo "Example: ./release.sh 0.1.0"
    echo "         ./release.sh 0.1.0 true  # TestPyPI only"
    exit 1
fi

echo "🚀 Starting release process for version $VERSION"

# 1. 更新版本号
echo "📝 Updating version numbers..."
sed -i.bak "s/^version = .*/version = \"$VERSION\"/" pyproject.toml
sed -i.bak "s/^version = .*/version = $VERSION/" setup.cfg
sed -i.bak "s/__version__ = .*/__version__ = \"$VERSION\"/" browser4/__init__.py
rm -f pyproject.toml.bak setup.cfg.bak browser4/__init__.py.bak

# 2. 运行测试
echo "🧪 Running tests..."
uv run pytest -m "not integration"

# 3. 清理和构建
echo "📦 Building package..."
rm -rf dist/ build/ *.egg-info
uv run python -m build

# 4. 检查包
echo "🔍 Checking package..."
twine check dist/*

# 5. 测试安装
echo "🧪 Testing local installation..."
python -m venv /tmp/test-install-$$
source /tmp/test-install-$$/bin/activate
pip install dist/*.whl
python -c "import browser4; assert browser4.__version__ == '$VERSION'"
deactivate
rm -rf /tmp/test-install-$$

# 6. 上传
if [ "$DRY_RUN" = "true" ]; then
    echo "🧪 Uploading to TestPyPI..."
    twine upload --repository testpypi dist/*
    echo "✅ Test upload complete!"
    echo "📦 Install with: pip install --index-url https://test.pypi.org/simple/ browser4-python==$VERSION"
else
    read -p "⚠️  Upload to PyPI? (yes/no): " confirm
    if [ "$confirm" = "yes" ]; then
        echo "📤 Uploading to PyPI..."
        twine upload dist/*

        echo "🏷️  Creating git tag..."
        git add pyproject.toml setup.cfg browser4/__init__.py
        git commit -m "chore(python-sdk): Release v$VERSION"
        git tag "python-sdk-v$VERSION"
        git push origin main "python-sdk-v$VERSION"

        echo "✅ Release complete!"
        echo "📦 Install with: pip install browser4-python==$VERSION"
        echo "🔗 PyPI: https://pypi.org/project/browser4-python/$VERSION/"
    else
        echo "❌ Upload cancelled"
        exit 1
    fi
fi
```

使用：
```bash
cd sdks/browser4-python
chmod +x scripts/release.sh

# 测试发布
./scripts/release.sh 0.1.0 true

# 正式发布
./scripts/release.sh 0.1.0
```

---

## 附录 B：模板文件

### SECURITY.md
```markdown
# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 0.1.x   | :white_check_mark: |

## Reporting a Vulnerability

**Please do not report security vulnerabilities through public GitHub issues.**

Instead, please send an email to security@example.com with:
- Description of the vulnerability
- Steps to reproduce
- Potential impact
- Suggested fix (if any)

You should receive a response within 48 hours. If the issue is confirmed, we will:
1. Work on a fix
2. Release a security update
3. Credit you in the release notes (if desired)

Thank you for helping keep browser4-python secure!
```

### CONTRIBUTING.md
```markdown
# Contributing to browser4-python

Thank you for your interest in contributing!

## Development Setup

1. Clone the repository
2. Install dependencies: `uv sync --extra dev`
3. Run tests: `uv run pytest`

## Pull Request Process

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Make your changes
4. Add tests for new functionality
5. Ensure all tests pass: `uv run pytest`
6. Update documentation
7. Submit a pull request

## Code Style

- Follow PEP 8
- Use type hints
- Add docstrings for public APIs
- Keep functions focused and testable

## Commit Messages

Use conventional commits:
- `feat:` new feature
- `fix:` bug fix
- `docs:` documentation
- `test:` test changes
- `refactor:` code refactoring
- `chore:` maintenance tasks
```

---

## 结语

本发布计划提供了 browser4-python 从开发到发布、维护的完整指南。关键要点：

1. **语义化版本**：遵循 SemVer 2.0.0，独立于主项目
2. **严格测试**：单元测试、集成测试、兼容性测试
3. **自动化发布**：使用 GitHub Actions 和 PyPI Trusted Publishing
4. **持续维护**：监控反馈、快速修复、定期迭代
5. **安全第一**：漏洞扫描、安全补丁、负责任披露

遵循本计划，可以确保高质量、稳定、可靠的 Python SDK 发布。

**下一步行动：**
1. 完成当前功能开发
2. 运行完整测试套件
3. 配置 PyPI Trusted Publishing
4. 创建自动化工作流
5. 执行首次发布（v0.1.0）

祝发布顺利！🚀
