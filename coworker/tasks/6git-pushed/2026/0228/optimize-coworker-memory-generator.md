# Improve coworker-daily-memory-generator

优化 coworker-daily-memory-generator，使其能够更高效地分析日志并生成记忆总结。

- 利用日志的结构，提取关键信息，仅将关键信息输入到 AI 模型中，以节约 Token 消耗。
- 读取 coworker.ps1 理解 coworker 的日志格式和内容，确保脚本能够正确解析日志文件。
- 按照 memory-specification.md 中的规范格式化输出文件，确保生成的记忆总结内容详实且有用。
- 详细解释你的优化思路和实现细节，包括如何提取关键信息、如何与 AI 模型交互以及如何格式化输出文件。

## References

- [coworker-daily-memory-generator.ps1](/coworker/scripts/coworker-daily-memory-generator.ps1)
- [coworker-daily-memory-generator.sh](/coworker/scripts/coworker-daily-memory-generator.sh)
- [coworker.ps1](../../scripts/coworker.ps1)
- [docs-dev/copilot/coworker/memory-specification.md](/docs-dev/copilot/coworker/memory-specification.md)

