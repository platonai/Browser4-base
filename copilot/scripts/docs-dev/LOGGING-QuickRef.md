# Coworker 脚本日志系统 - 快速参考

## 一句话总结
所有脚本输出都记录到 `logs/coworker-*.log`，外部程序输出记录到 `logs/copilot_*.log`，任务信息记录到 `logs/task_*.log`。

## 日志文件位置

| 类型 | 文件位置 | 用途 |
|-----|--------|------|
| 主脚本日志 | `logs/coworker-yyyyMMdd-HHmmss.log` | 脚本执行过程（每次运行新建） |
| 任务日志 | `logs/task_<name>_yyyyMMdd-HHmmss.log` | 任务的提示、描述、执行结果 |
| 工具日志 | `logs/copilot_<name>_yyyyMMdd-HHmmss.log` | Copilot 工具的完整输出 |

## 快速命令

### 查看最新脚本日志（最后 50 行）
```bash
# PowerShell
Get-Content (Get-ChildItem docs-dev\copilot\tasks\daily\logs\coworker-*.log | Sort LastWriteTime -Desc | Select -First 1).FullName -Tail 50

# Bash
tail -50 $(ls -1t docs-dev/copilot/tasks/daily/logs/coworker-*.log | head -1)
```

### 实时监看日志
```bash
# PowerShell
Get-Content docs-dev\copilot\tasks\daily\logs\coworker-*.log -Wait

# Bash
tail -f docs-dev/copilot/tasks/daily/logs/coworker-*.log
```

### 查看所有警告和错误
```bash
# PowerShell
Select-String '\[(WARN|ERROR)\]' docs-dev\copilot\tasks\daily\logs\coworker-*.log

# Bash
grep -E '\[(WARN|ERROR)\]' docs-dev/copilot/tasks/daily/logs/coworker-*.log
```

### 按任务名查看日志
```bash
# PowerShell
Get-ChildItem docs-dev\copilot\tasks\daily\logs\task_MyTask_*.log | Get-Content

# Bash
cat docs-dev/copilot/tasks/daily/logs/task_MyTask_*.log
```

### 查看特定任务的 Copilot 输出
```bash
# PowerShell
Get-ChildItem docs-dev\copilot\tasks\daily\logs\copilot_MyTask_*.log | Get-Content

# Bash
cat docs-dev/copilot/tasks/daily/logs/copilot_MyTask_*.log
```

## 日志级别

| 级别 | 控制台颜色 | 记录位置 | 用途 |
|-----|---------|--------|------|
| **INFO** | 白色（默认） | 主脚本日志 | 标准流程信息 |
| **WARN** | 黄色 | 主脚本日志 | 警告信息 |
| **ERROR** | 红色 | 主脚本日志 | 错误信息 |
| **DEBUG** | 不显示 | 仅主脚本日志 | 详细调试信息 |

## 目录结构

```
docs-dev/copilot/tasks/daily/
├── created/      # ← 放新任务文件在这里
├── working/      # ← 执行中的任务（通常为空）
├── finished/     # ← 完成的任务文件
└── logs/         # ← 所有日志文件
    ├── coworker-20260214-103045.log
    ├── task_MyTask_20260214-103045.log
    └── copilot_MyTask_20260214-103045.log
```

## 故障排查流程

1. **查看主脚本日志** → 找到失败的任务
2. **查看任务日志** → 了解任务内容和结果
3. **查看 Copilot 日志** → 获取工具的详细输出和错误信息
4. **检查 stderr 部分** → 查看程序是否有错误输出

## 常见日志消息

### 成功执行
```
[2026-02-14 10:30:45] [INFO] Processing task.txt...
[2026-02-14 10:30:45] [INFO] Moved to working: .../working/task.txt
[2026-02-14 10:30:46] [INFO] Executing Copilot for task: My Task
[2026-02-14 10:31:20] [INFO] Copilot execution finished with exit code 0
[2026-02-14 10:31:20] [INFO] Task moved to finished: .../finished/task.txt
```

### 非零退出码（警告）
```
[2026-02-14 10:31:20] [INFO] Copilot execution finished with exit code 1
[2026-02-14 10:31:20] [WARN] Warning: Copilot exited with non-zero code. Check log: ...
```

### 执行失败
```
[2026-02-14 10:30:46] [ERROR] Failed to execute copilot: ...
```

## 日志记录自动化

### 清理超过 30 天的日志
```bash
# PowerShell
Get-ChildItem docs-dev\copilot\tasks\daily\logs\*.log | Where {$_.LastWriteTime -lt (Get-Date).AddDays(-30)} | Remove-Item

# Bash
find docs-dev/copilot/tasks/daily/logs -name "*.log" -mtime +30 -delete
```

### 统计日志文件
```bash
# PowerShell
(Get-ChildItem docs-dev\copilot\tasks\daily\logs\*.log).Count

# Bash
ls -1 docs-dev/copilot/tasks/daily/logs/*.log | wc -l
```

## 日志文件大小管理

```bash
# PowerShell - 查看所有日志的总大小
(Get-ChildItem docs-dev\copilot\tasks\daily\logs\*.log | Measure -Sum Length).Sum / 1MB

# Bash - 查看所有日志的总大小
du -sh docs-dev/copilot/tasks/daily/logs/
```

## 运行脚本

### PowerShell
```powershell
powershell -ExecutionPolicy Bypass -File bin\copilot\coworker.ps1
```

### Bash
```bash
./bin/copilot/coworker.sh
```

---

**更多信息请参考**: `bin/copilot/LOGGING.md`

