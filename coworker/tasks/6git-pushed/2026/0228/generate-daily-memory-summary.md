# Improve Coworker's Memory

写一个脚本，分析 300logs 目录下每日的开发日志，来生成当天的记忆总结，并将其保存到 `coworker/tasks/300logs/YYYY/MM/DD/MEMORY.YYYYMMDD.md` 文件中。
脚本需要从当天的日志中提取关键信息，包括执行的任务、遇到的问题、解决方案和任何重要的见解。

譬如，从 coworker/tasks/300logs/2026/02/28 的日志中提取信息，生成 `MEMORY.20260228.md` 文件。

使用 `gh copilot` 作为 AI 助手来分析日志并生成记忆总结。确保脚本能够自动识别当天的日期，并正确地格式化输出文件。
你需要考虑如何节约 Token 消耗，同时确保生成的记忆总结内容详实且有用。

MEMORY 格式规范：

docs-dev/copilot/coworker/memory-specification.md
