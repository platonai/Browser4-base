# External Task Source

写一个ps1/sh脚本，完成以下功能：
1. 监控 github 任务
2. 监控指定 URL 任务

## 监控 github 任务

1. 从指定 github 仓库获取最新 20 个issues，如果该 issue 被指派到 @galaxyeye，则将该 issue 内容保存到一个文件中，文件名是当前时间戳，纯数字形式。
2. 将该文件移入到 coworker/tasks/2working 目录中，从而触发后续的工作流程。

## 监控指定 URL 任务

1. 每隔 1 分钟访问一次指定 URL，如果该 URL 返回的内容包含特定关键词（例如 "@galaxyeye"），则将该内容保存到一个文件中，文件名是当前时间戳，纯数字形式。
2. 将该文件移入到 coworker/tasks/2working 目录中，从而触发后续的工作流程。

