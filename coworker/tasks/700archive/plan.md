# Plan for Daily Memory Batch Processing

## Objective
Update `coworker-daily-memory-generator.ps1` and `coworker-daily-memory-generator.sh` to process logs in batches to avoid token limits.

## Strategy
1.  **Collect all log data** first (as currently done), but keep it structured (e.g., list of task contents) instead of one big string.
2.  **Define a batch size** (e.g., approx 10k-15k chars or N tasks).
3.  **Loop through batches**:
    *   **First Batch**:
        *   Prompt: "Create the daily memory file... based on these logs."
    *   **Subsequent Batches**:
        *   Prompt: "Read the existing daily memory file ($memoryFile). UPDATE it to include the summary of THESE additional logs. Append new tasks to 'Tasks Executed'. Update 'Execution Quality Review' and other sections if these new logs provide new insights. Maintain the existing structure."

## Detailed Steps for PowerShell
1.  Read all task logs into an object array `@{ Name = "TaskName"; Content = "LogContent..." }`.
2.  Initialize `$currentBatchContent = ""`.
3.  Iterate through tasks.
4.  Add task content to `$currentBatchContent`.
5.  If `$currentBatchContent.Length` exceeds limit (e.g. 12000 chars) OR it's the last task:
    *   Construct Prompt.
    *   If it's the first batch, instructions = "CREATE ...".
    *   If it's not the first batch, instructions = "READ $memoryFile AND UPDATE ...".
    *   Call `gh copilot`.
    *   Clear `$currentBatchContent`.

## Detailed Steps for Bash
Similar logic, but using arrays or just accumulating string and checking length.

## Implementation Details
*   **Token Limit**: `gh copilot` likely uses GPT-4 or similar. 12k chars is roughly 3-4k tokens, which is safe for input + output.
*   **Prompt Adjustment**:
    *   Need to distinguish between "Create" mode and "Update" mode.
    *   Update mode needs to explicitly say "Read the file first".

## Directives
*   `#auto-approve` logic is mentioned in the task file but I should probably focus on the memory batching first as that's the main issue. The directives in the task file might be just instructions for *me* (the agent) or for the system.
*   Wait, the task file says:
    ```
    ## Directives
    #auto-approve
    ```
    This usually means *I* should auto-approve something? Or maybe it's a tag for the task itself. I will ignore it for the code logic unless it's relevant to the script.

## Refined Algorithm (PowerShell)

```powershell
$maxBatchSize = 15000
$tasks = @()
# ... collect all tasks into $tasks array ...

$batchBuffer = ""
$isFirstBatch = $true

foreach ($task in $tasks) {
    $taskContent = "=== TASK: $($task.Name) ===`n$($task.Content)`n"
    
    if (($batchBuffer.Length + $taskContent.Length) -gt $maxBatchSize) {
        # Process current batch
        Process-Batch -Content $batchBuffer -IsFirst $isFirstBatch -MemoryFile $memoryFile
        $batchBuffer = ""
        $isFirstBatch = $false
    }
    $batchBuffer += $taskContent
}

# Process remaining
if ($batchBuffer.Length -gt 0) {
    Process-Batch -Content $batchBuffer -IsFirst $isFirstBatch -MemoryFile $memoryFile
}
```

## Refined Algorithm (Bash)
Bash arrays are a bit tricky with newlines. I might use a temporary file or just careful string manipulation.
Actually, since I'm in a PowerShell environment mainly (Windows), I should prioritize the PS1 script, but I must update both to keep them in sync.

The `coworker-daily-memory-generator.sh` might be used in CI/CD or Linux environments.

## Action Item
1.  Modify `coworker-daily-memory-generator.ps1`.
2.  Modify `coworker-daily-memory-generator.sh`.
