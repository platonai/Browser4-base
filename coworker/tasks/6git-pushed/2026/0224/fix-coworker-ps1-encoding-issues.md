# 修复乱码问题

coworker.ps1 生成的日志有乱码：

D:\workspace\Browser4\Browser4-4.6\coworker\tasks\logs\implement-webdriver-select-option.task.log

同时，控制台输出中也有乱码：

```
(base) PS D:\workspace\Browser4\Browser4-4.6> pwd

Path
----
D:\workspace\Browser4\Browser4-4.6


(base) PS D:\workspace\Browser4\Browser4-4.6> pwsh.exe --version
PowerShell 7.5.4

(base) PS D:\workspace\Browser4\Browser4-4.6> D:\workspace\Browser4\Browser4-4.6\coworker\scripts\coworker.ps1
[2026-02-22 19:56:13] [INFO] ===========================================================================
[2026-02-22 19:56:13] [INFO] Coworker Task Runner - PowerShell Version
[2026-02-22 19:56:13] [INFO] Started at: 02/22/2026 19:56:13
[2026-02-22 19:56:13] [INFO] Script Log: D:\workspace\Browser4\Browser4-4.6\coworker\tasks\logs\coworker-20260222-195613.log
[2026-02-22 19:56:13] [INFO] ==========================================================================
[2026-02-22 19:57:13] [INFO] Moved to working: D:\workspace\Browser4\Browser4-4.6\coworker\tasks\2working\2.md
[2026-02-22 19:57:13] [INFO] Executing Copilot for task: 2
[2026-02-22 19:57:13] [INFO] === Starting Copilot execution ===
鈼?Glob "**/coworker.ps1"
  鈹?1 file found
鈼?Glob "**/coworker.sh"
  鈹?1 file found
鈼?Read coworker\scripts\coworker.ps1
  鈹?490 lines read
鈼?Read coworker\scripts\coworker.sh
  鈹?359 lines read
鈼?Edit coworker\scripts\coworker.sh (+18 -7)
鈼?Edit coworker\scripts\coworker.sh (+20 -1)
```
