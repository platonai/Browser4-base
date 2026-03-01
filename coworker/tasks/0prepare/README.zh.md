# Coworker Task Drafting Area

请在此处撰写你的任务草稿。本区用于准备中的任务，尚未准备好进入正式执行阶段。

一旦任务定义完整且可执行，请将其移动到 `1created` 目录，Coworker 会自动开始处理该目录下的任务。

运行 `run_coworker_periodically.ps1`，Coworker 将自动处理 `1created` 目录中的任务。

任务文件内容格式不限，推荐使用以下模板来规范任务描述：

```markdown
# 任务标题

## 问题描述
详细描述需要解决的问题或任务，尽量具体明确。

## 解决方案

简要说明你计划采用的解决思路，包括步骤、方法或任何具体的技术方案。

## 参考资料

列出有助于理解和解决任务的相关资源、文档或参考链接。
```

## 示例

- [简单示例 - refine-coworker-readme.md.md](../6git-pushed/2026/0228/refine-coworker-readme.md.md)
- [进阶示例 - implement-daily-memory-batching.md](../6git-pushed/2026/0228/implement-daily-memory-batching.md)

## 参考资料

- [coworker.ps1](../../scripts/coworker.ps1)
- [coworker.sh](../../scripts/coworker.sh)
- [run_coworker_periodically.ps1](../../scripts/run_coworker_periodically.ps1)
- [run_coworker_periodically.sh](../../scripts/run_coworker_periodically.sh)
