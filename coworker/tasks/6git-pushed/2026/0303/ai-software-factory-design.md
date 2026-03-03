# AI Dev Orchestrator

设计方向：

> 用 PowerShell 作为编排引擎
> 用文件系统作为状态机
> 用 Markdown 作为任务协议
> 用 `gh copilot -p` 作为智能执行内核

下面我给你一个工程级设计方案，确保可扩展、可恢复、可调试。

---

# 一、总体架构

```
coworker/
│
├── scripts/
├── tasks/
    │
    ├── 0draft/
    ├── 1created/
    ├── 2working/
    ├── 3_1complete/
    ├── 3_5aborted/
    ├── 4review/
    ├── 5approved/
    ├── 6git-pushed/
    │
    ├── 100templates/
    ├── 300logs/
    └── 400artifacts/
```

职责划分：

| 目录           | 作用                         |
|--------------| -------------------------- |
| 状态目录         | 任务生命周期                     |
| 100templates | 各阶段 prompt 模板              |
| scripts      | PowerShell 调度引擎            |
| 300logs      | 执行日志                       |
| 400artifacts | 结构化输出（json / plan / graph） |

---

# 二、任务文件规范（核心）

每个任务 = 一个文件夹：

```
1created/STORY-0001/
```

内部结构：

```
story.md
context.md
plan.json
graph.json
status.json
```

---

## story.md（用户输入）

```markdown
# STORY-0001

## User Story
As a user,
I want Browser4 to detect bot-blocking pages,
so that automation can retry intelligently.

## Acceptance Criteria
- Detect Cloudflare challenge
- Detect 403 bot-block
- Retry with backoff
```

---

## status.json

```json
{
  "state": "created",
  "retries": 0,
  "currentNode": null,
  "lastError": null
}
```

---

# 三、状态机设计（文件夹即状态）

你的状态设计是正确的，但我建议语义精确定义：

| 状态          | 含义            |
| ----------- | ------------- |
| 0draft      | 仅记录，未进入流程     |
| 1created    | 可被调度          |
| 2working    | 正在被 agent 处理  |
| 3_1complete | 技术完成，待 review |
| 3_5aborted  | 技术失败          |
| 4review     | 人工或 AI 评审     |
| 5approved   | 审核通过          |
| 6git-pushed | 已提交代码         |

注意：

> 文件夹迁移 = 原子状态转换

必须使用：

```powershell
Move-Item -Path $src -Destination $dst -Force
```

禁止 copy。

---

# 四、核心调度脚本设计

主循环：

```powershell
while ($true) {

    $tasks = Get-ChildItem "./1created"

    foreach ($task in $tasks) {
        Move-ToWorking $task
        Execute-StoryPipeline $task
    }

    Start-Sleep -Seconds 5
}
```

---

# 五、Story Pipeline 细化

```powershell
function Execute-StoryPipeline {
    param($taskPath)

    Invoke-StoryAnalysis $taskPath
    Invoke-PlanDesign $taskPath
    Invoke-SubFeatureDesign $taskPath
    Invoke-Implementation $taskPath
    Invoke-E2ETestDesign $taskPath
    Invoke-Validation $taskPath
}
```

每一步都：

1. 读取 Markdown
2. 拼接模板
3. 调用：

```powershell
gh copilot -p "$prompt"
```

4. 写入 artifact
5. 校验
6. 失败则重试或 abort

---

# 六、如何防止 AI 乱写

关键点：**必须模板化 prompt**

例如：

```
100templates/plan.prompt.md
```

内容：

```
You are a system architect.

INPUT:
{{story.md}}

OUTPUT FORMAT (JSON ONLY):
{
  "sub_features": [
    {
      "name": "",
      "description": "",
      "dependencies": []
    }
  ]
}
```

PowerShell 替换：

```powershell
$prompt = Get-Content $template -Raw
$prompt = $prompt.Replace("{{story.md}}", $storyContent)
```

---

# 七、DAG 执行方式

plan.json 示例：

```json
{
  "sub_features": [
    {
      "id": "F1",
      "name": "ChallengeDetector",
      "dependencies": []
    },
    {
      "id": "F2",
      "name": "RetryEngine",
      "dependencies": ["F1"]
    }
  ]
}
```

执行算法：

```powershell
while (ExistsUnfinishedNode) {
    $ready = Get-NodesWhereDependenciesSatisfied
    foreach ($node in $ready) {
        Execute-Node $node
    }
}
```

---

# 八、失败控制机制（非常关键）

## 1️⃣ 重试阈值

```json
{
  "retryLimit": 3
}
```

超过：

→ 移动到 3_5aborted

---

## 2️⃣ 自动 Replan 机制

如果某子功能连续失败：

```
Invoke-ReplanSubFeature
```

让 AI 重新拆解该功能。

---

# 九、日志设计

每次调用：

```
300logs/STORY-0001/
    01-analysis.log
    02-plan.log
    03-F1-impl.log
```

日志必须包含：

* 时间
* Prompt
* AI 输出
* 执行结果
* ExitCode

---

# 十、Git 提交自动化

使用
scripts/workers/git-sync.ps1
scripts/workers/git-sync.sh

然后移动：

```
5approved → 6git-pushed
```

---

# 十一、你这个系统的真实定位

你正在构建的是：

> **AI Driven CI/CD + AI Software Factory**

它不是：

* Chatbot
* 简单 Agent
* Copilot wrapper

它是：

> 一个基于文件系统状态机的软件生产操作系统

---

# 十二、未来演进方向

1. 引入 lock 文件防止并发冲突
2. 支持多 Story 并行
3. 支持 Story 依赖图
4. 引入 Metrics（成功率、重试率）
5. 引入 AI 评审 Agent（自动 code review）

---

# 十三、关键风险提醒

### 风险 1：Copilot 输出不稳定

必须：

* 强制 JSON 输出
* 强制 schema 校验

---

### 风险 2：AI 误判“完成”

必须：

* 单元测试
* E2E 测试
* 覆盖率阈值

---

# 十四、总结

你的方案是可行的。

但成功的关键在于：

* 强状态控制
* 强结构化输出
* 强校验机制
* 严格分阶段 Agent
