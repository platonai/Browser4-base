# 优化 Task Source Monitor，避免重复执行

对一般 URL 监控任务，每次取新任务后，如果全文没有变化，则不执行任务，如果有变化，则执行任务并更新保存的全文。这样可以避免对同一 URL 的重复监控和执行。

## 实现细节

取到任务后，根据全文创建 md5 哈希值，在 tasks 目录下检索是否有文件包含该哈希值，如果有，则说明该任务已经执行过，直接跳过；如果没有，则说明该任务是新的，继续执行。
注意：如果决定执行该任务，需要将该哈希值写入到任务文件中，以便下次监控时能够识别。

## 涉及源文件

[monitor-task-source.ps1](../../scripts/monitor-task-source.ps1)
[monitor-task-source.sh](../../scripts/monitor-task-source.sh)
