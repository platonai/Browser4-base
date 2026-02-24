优化 coworker

## 支持指定任务文件

```shell
coworker.ps1 -TaskFile "coworker/tasks/0prepare/1.md"

# 也可以直接指定文件路径
coworker.ps1 "coworker/tasks/0prepare/1.md"
```

- 执行命令时，将指定文件移入 1created 文件夹中, 然后按照原有逻辑执行。

## 优化重命名逻辑

用户会随意指定任务文件名，1.md, 2.md 等以数字开头的文件名都被视作随意文件名，这节约了用户的时间，coworker 需要根据文件内容将这些文件重命名为可读性强的文件名。

执行顺序：
1. 将指定文件移动到 1created 文件夹中。
2. 调用 `gh copilot` 生成新的文件名。
3. 将文件重命名为新的文件名。
4. 将文件移入 2working 文件夹中。
5. 执行原有逻辑（调用 `gh copilot` 执行任务）。

## 更新 coworker.sh

根据 coworker.ps1 更新 coworker.sh
