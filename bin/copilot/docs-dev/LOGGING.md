# Coworker 脚本日志系统文档

## 概述

Coworker 脚本 (`coworker.ps1` 和 `coworker.sh`) 提供了全面的日志记录功能，将所有屏幕输出和操作记录到文件，同时为外部程序 (Copilot) 的输出创建单独的日志。

## 日志目录结构

```
docs-dev/copilot/tasks/daily/
├── created/                      # 新建任务文件输入目录
├── working/                      # 执行中的任务临时目录
├── finished/                     # 已完成任务目录
└── logs/                         # 所有日志文件存储位置
    ├── coworker-20260214-103045.log            # 主脚本日志（每次运行新建）
    ├── task_MyTask_20260214-103045.log         # 任务执行日志（任务级别）
    ├── task_AnotherTask_20260214-103046.log    # 另一个任务的日志
    ├── copilot_MyTask_20260214-103045.log      # Copilot 工具输出（任务级别）
    └── copilot_AnotherTask_20260214-103046.log # 另一个任务的 Copilot 输出
```

## 日志类型说明

### 1. 主脚本日志 (coworker-*.log)

**文件位置**: `docs-dev/copilot/tasks/daily/logs/coworker-yyyyMMdd-HHmmss.log`

**包含内容**:
- 脚本启动和完成信息
- 每个任务的处理过程
- 任务移动（created → working → finished）的记录
- Copilot 执行的启动和完成信息
- Copilot 的退出码
- 脚本运行过程中的所有标准日志消息
- DEBUG 级别的详细信息（不会输出到控制台）

**示例内容**:
```
[2026-02-14 10:30:45] [INFO] ==========================================================================
[2026-02-14 10:30:45] [INFO] Coworker Task Runner - PowerShell Version
[2026-02-14 10:30:45] [INFO] Started at: 2026-02-14 10:30:45
[2026-02-14 10:30:45] [INFO] Script Log: D:\workspace\Browser4\Browser4-4.6\docs-dev\copilot\tasks\daily\logs\coworker-20260214-103045.log
[2026-02-14 10:30:45] [INFO] ===========================================================================
[2026-02-14 10:30:45] [INFO] Processing task.txt...
[2026-02-14 10:30:45] [INFO] Moved to working: D:\workspace\Browser4\Browser4-4.6\docs-dev\copilot\tasks\daily\working\MyTask.txt
[2026-02-14 10:30:45] [DEBUG] Task log will be written to: D:\workspace\Browser4\Browser4-4.6\docs-dev\copilot\tasks\daily\logs\task_MyTask_20260214-103045.log
[2026-02-14 10:30:46] [INFO] Executing Copilot for task: My Task
[2026-02-14 10:30:46] [DEBUG] Task Description: Task from task.txt
[2026-02-14 10:30:46] [DEBUG] Prompt length: 256 characters
[2026-02-14 10:31:20] [INFO] Copilot execution finished with exit code 0
[2026-02-14 10:31:20] [DEBUG] Copilot external tool log: D:\workspace\Browser4\Browser4-4.6\docs-dev\copilot\tasks\daily\logs\copilot_MyTask_20260214-103045.log
[2026-02-14 10:31:20] [INFO] Task moved to finished: D:\workspace\Browser4\Browser4-4.6\docs-dev\copilot\tasks\daily\finished\MyTask.txt
[2026-02-14 10:31:20] [INFO] ---
[2026-02-14 10:31:21] [INFO] ==========================================================================
[2026-02-14 10:31:21] [INFO] All tasks completed
[2026-02-14 10:31:21] [INFO] Ended at: 2026-02-14 10:31:21
[2026-02-14 10:31:21] [INFO] Script Log: D:\workspace\Browser4\Browser4-4.6\docs-dev\copilot\tasks\daily\logs\coworker-20260214-103045.log
[2026-02-14 10:31:21] [INFO] ==========================================================================
```

### 2. 任务日志 (task_*.log)

**文件位置**: `docs-dev/copilot/tasks/daily/logs/task_<TaskName>_yyyyMMdd-HHmmss.log`

**包含内容**:
- 任务元数据（标题、描述、开始时间）
- 完整的任务提示内容 (Prompt)
- Copilot 执行的结果（退出码和日志位置）

**示例内容**:
```
Task: My Task
Description: Task from task.txt
Started: 2026-02-14 10:30:46
Prompt:
Please analyze the following code and provide optimization suggestions.
The code is responsible for processing large data files...

---
Copilot Execution Output:

Copilot Exit Code: 0
Copilot Log: D:\workspace\Browser4\Browser4-4.6\docs-dev\copilot\tasks\daily\logs\copilot_MyTask_20260214-103045.log
```

### 3. Copilot 外部工具日志 (copilot_*.log)

**文件位置**: `docs-dev/copilot/tasks/daily/logs/copilot_<TaskName>_yyyyMMdd-HHmmss.log`

**包含内容**:
- Copilot 工具的完整标准输出 (STDOUT)
- Copilot 工具的完整标准错误输出 (STDERR)，如果存在的话

**示例内容**:
```
[Copilot 的完整输出内容...]
Here are the optimization suggestions for your code:

1. Replace the traditional loop with LINQ for better performance
2. Use async/await for I/O operations
3. Implement caching for frequently accessed data
...

=== COPILOT STDERR ===

Warning: Some dependencies might be outdated
```

## 日志级别说明

### INFO (信息)
- **控制台显示**: 白色/默认颜色
- **记录方式**: 记录到主脚本日志
- **用途**: 标准流程信息（任务开始、任务完成等）
- **示例**: `Processing task.txt...`

### WARN (警告)
- **控制台显示**: 黄色
- **记录方式**: 记录到主脚本日志
- **用途**: 警告信息（Copilot 非零退出码等）
- **示例**: `Warning: Copilot exited with non-zero code. Check log: ...`

### ERROR (错误)
- **控制台显示**: 红色，输出到标准错误流
- **记录方式**: 记录到主脚本日志
- **用途**: 错误信息（执行失败等）
- **示例**: `Failed to execute copilot: ...`

### DEBUG (调试)
- **控制台显示**: 不显示（仅记录到文件）
- **记录方式**: 仅记录到主脚本日志
- **用途**: 详细的调试信息
- **示例**: `Task log will be written to: ...`

## 使用示例

### 运行脚本

#### PowerShell
```powershell
cd D:\workspace\Browser4\Browser4-4.6
powershell -ExecutionPolicy Bypass -File bin\copilot\coworker.ps1
```

#### Bash/Shell
```bash
cd /path/to/Browser4-4.6
./bin/copilot/coworker.sh
# 或
bash bin/copilot/coworker.sh
```

### 查看日志

#### 实时监看主脚本日志
```bash
# PowerShell (Windows)
Get-Content docs-dev\copilot\tasks\daily\logs\coworker-*.log -Wait

# Bash (Linux/Mac/WSL)
tail -f docs-dev/copilot/tasks/daily/logs/coworker-*.log
```

#### 查看最新的脚本日志
```bash
# PowerShell (Windows)
Get-Content (Get-ChildItem docs-dev\copilot\tasks\daily\logs\coworker-*.log | Sort-Object LastWriteTime -Descending | Select-Object -First 1).FullName

# Bash (Linux/Mac/WSL)
tail -n 50 $(ls -1t docs-dev/copilot/tasks/daily/logs/coworker-*.log | head -1)
```

#### 查看特定任务的日志
```bash
# PowerShell (Windows)
Get-Content docs-dev\copilot\tasks\daily\logs\task_MyTask_*.log

# Bash (Linux/Mac/WSL)
cat docs-dev/copilot/tasks/daily/logs/task_MyTask_*.log
```

#### 查看 Copilot 的输出日志
```bash
# PowerShell (Windows)
Get-Content docs-dev\copilot\tasks\daily\logs\copilot_MyTask_*.log

# Bash (Linux/Mac/WSL)
cat docs-dev/copilot/tasks/daily/logs/copilot_MyTask_*.log
```

#### 过滤特定级别的日志
```bash
# PowerShell - 查看所有警告和错误
Select-String '\[(WARN|ERROR)\]' docs-dev\copilot\tasks\daily\logs\coworker-*.log

# Bash - 查看所有警告和错误
grep -E '\[(WARN|ERROR)\]' docs-dev/copilot/tasks/daily/logs/coworker-*.log
```

## 故障排查

### 1. 任务执行失败

**步骤**:
1. 查看主脚本日志找到失败的任务名称
2. 定位对应的任务日志 (`task_<name>_*.log`)
3. 查看 Copilot 外部工具日志 (`copilot_<name>_*.log`) 获取详细错误信息
4. 检查 stderr 部分（以 `=== COPILOT STDERR ===` 开头）

### 2. 脚本异常中止

**步骤**:
1. 查看最新的主脚本日志中的最后几行
2. 寻找 ERROR 级别的日志条目
3. 检查是否有待处理的任务还在 `working` 目录中
4. 手动清理 `working` 目录中的任务文件后重新运行

### 3. 日志文件找不到

**验证**:
- 确保 `docs-dev/copilot/tasks/daily/logs` 目录存在
- 脚本会自动创建不存在的日志目录
- 检查文件系统权限，确保有写入权限

## 日志清理

### 自动清理（建议定期执行）

```bash
# PowerShell - 保留最近 30 天的日志
Get-ChildItem docs-dev\copilot\tasks\daily\logs\*.log | Where-Object {$_.LastWriteTime -lt (Get-Date).AddDays(-30)} | Remove-Item

# Bash - 保留最近 30 天的日志
find docs-dev/copilot/tasks/daily/logs -name "*.log" -mtime +30 -delete
```

### 完全清空日志

```bash
# PowerShell
Remove-Item docs-dev\copilot\tasks\daily\logs\*.log

# Bash
rm docs-dev/copilot/tasks/daily/logs/*.log
```

## 日志输出示例对比

### 控制台输出
```
[2026-02-14 10:30:45] [INFO] ==========================================================================
[2026-02-14 10:30:45] [INFO] Coworker Task Runner - PowerShell Version
[2026-02-14 10:30:45] [INFO] Started at: 2026-02-14 10:30:45
[2026-02-14 10:30:45] [INFO] Script Log: D:\workspace\Browser4\Browser4-4.6\docs-dev\copilot\tasks\daily\logs\coworker-20260214-103045.log
[2026-02-14 10:30:45] [INFO] ===========================================================================
[2026-02-14 10:30:45] [INFO] Processing task.txt...
[2026-02-14 10:30:45] [INFO] Moved to working: D:\workspace\Browser4\Browser4-4.6\docs-dev\copilot\tasks\daily\working\MyTask.txt
[2026-02-14 10:30:46] [INFO] Executing Copilot for task: My Task
[2026-02-14 10:31:20] [INFO] Copilot execution finished with exit code 0
[2026-02-14 10:31:20] [INFO] Task moved to finished: D:\workspace\Browser4\Browser4-4.6\docs-dev\copilot\tasks\daily\finished\MyTask.txt
[2026-02-14 10:31:20] [INFO] ---
[2026-02-14 10:31:21] [INFO] ==========================================================================
[2026-02-14 10:31:21] [INFO] All tasks completed
[2026-02-14 10:31:21] [INFO] Ended at: 2026-02-14 10:31:21
```

**说明**: 控制台仅显示 INFO、WARN 和 ERROR 级别的消息。DEBUG 消息不显示。

### 脚本日志文件内容

除了上面控制台显示的所有信息外，还包含 DEBUG 消息：

```
[2026-02-14 10:30:45] [DEBUG] Task log will be written to: D:\workspace\Browser4\Browser4-4.6\docs-dev\copilot\tasks\daily\logs\task_MyTask_20260214-103045.log
[2026-02-14 10:30:46] [DEBUG] Task Description: Task from task.txt
[2026-02-14 10:30:46] [DEBUG] Prompt length: 256 characters
[2026-02-14 10:31:20] [DEBUG] Copilot external tool log: D:\workspace\Browser4\Browser4-4.6\docs-dev\copilot\tasks\daily\logs\copilot_MyTask_20260214-103045.log
```

## 最佳实践

1. **定期查看日志**: 在脚本完成后查看主脚本日志，确保所有任务成功执行
2. **保存重要日志**: 对于重要的任务执行，备份相应的日志文件
3. **实时监控**: 使用 `tail -f` 或 `Get-Content -Wait` 命令实时监看脚本执行
4. **定期清理**: 定期清理旧日志文件，释放磁盘空间
5. **自动化处理**: 将脚本集成到计划任务或 CI/CD 流程中，并定期检查日志

## 相关文件

- `coworker.ps1`: PowerShell 版本的 Coworker 脚本
- `coworker.sh`: Bash/Shell 版本的 Coworker 脚本
- `SCHEDULER.md`: 关于计划任务和守护进程的文档

