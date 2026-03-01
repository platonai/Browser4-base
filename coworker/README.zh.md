# 内置 AI 协作助手（Builtin AI Coworker）

内置 AI 协作助手是一个代理（agent），可协助你在仓库中完成各种任务。你只需创建任务文件，协作助手会处理这些文件、执行任务，并可将更改提交回仓库。

## 工作流程

任务文件会在 `coworker/tasks/` 目录下的编号文件夹中流转：

| 阶段   | 文件夹         | 说明                     |
|--------|----------------|--------------------------|
| 草稿   | `0draft`     | 在此处创建和编辑任务文件 |
| 队列   | `1created`     | 准备执行时移入此文件夹   |
| 规划   | `200plan`      | 代理规划阶段（自动管理） |
| 执行   | `2working`     | 代理正在执行任务         |
| 完成   | `3_1complete`    | 执行结束，可审查更改     |
| 审查   | `4review`      | 可选的人工审查阶段       |
| 已批准 | `5approved`    | 已批准任务，等待提交推送 |
| 已推送 | `6git-pushed`  | 已成功提交并推送         |
| 归档   | `700archive`   | 已归档的已完成任务       |

## 快速开始

1. **草稿** — 在 `coworker/tasks/0draft/` 创建任务文件。
2. **队列** — 准备好后将其移至 `coworker/tasks/1created/`。
3. **执行** — 运行协作助手脚本处理任务：
   - Windows: `./coworker/scripts/coworker.ps1`
   - Linux/macOS: `./coworker/scripts/coworker.sh`
4. **审查** — 任务执行后会进入 `3_1complete`，可审查更改。
5. **批准** — 将任务移至 `5approved`，定时任务会自动提交并推送。

## 前置条件

需安装并认证 GitHub CLI（`gh`）。

安装方法详见：https://github.com/cli/cli#installation

## 标签（Tags）

你可以在任务文件中使用标签，提供额外上下文或控制行为。

支持的标签：

- `#auto-approve` — 任务完成后自动移至 `5approved`，无需人工审查，适用于低风险、可信任务。

## 提及（Mentions）

> **实验性功能**

在任务文件中提及 `@coworker`，可通知代理处理该任务。

## 与 Git 同步

任务批准后，可使用 git-sync 脚本将更改推送到仓库。

**Windows (PowerShell)：**

```powershell
.\coworker\scripts\git-sync.ps1
```

**Linux/macOS (Bash)：**

```bash
./coworker/scripts/git-sync.sh
```

## 定时运行器

定时运行器会自动监控 `1created` 和 `5approved` 文件夹并处理任务。

**Windows (PowerShell)：**

```powershell
.\coworker\scripts\run_coworker_periodically.ps1
```

**Linux/macOS (Bash)：**

```bash
./coworker/scripts/run_coworker_periodically.sh
```

