@{
    Scheduler = @{
        TickSeconds          = 5
        PowerShellExecutable = 'pwsh'
        LogDirectory         = 'coworker\tasks\300logs\scheduler'
        StatusFile           = 'logs\scheduled-tasks.status.json'
    }

    Tasks = @(
        @{
            Name            = 'coworker'
            Description     = 'Process queued coworker tasks.'
            Enabled         = $true
            IntervalSeconds = 15
            DependsOn       = @('monitor-task-source')
            PendingPaths    = @(
                'coworker\tasks\1created'
                'coworker\tasks\5approved'
            )
            ScriptPath      = 'coworker\scripts\process-coworker-queue.ps1'
            Arguments       = @('-Once')
        }
        @{
            Name            = 'draft-refinement'
            Description     = 'Process the draft refinement queue.'
            Enabled         = $true
            IntervalSeconds = 15
            PendingPaths    = @('coworker\tasks\0draft\refine\1ready')
            ScriptPath      = 'coworker\scripts\process-draft-refinement-queue.ps1'
            Arguments       = @('-Once')
        }
        @{
            Name            = 'monitor-task-source'
            Description     = 'Poll configured task sources and dispatch new tasks.'
            Enabled         = $false
            IntervalSeconds = 60
            ScriptPath      = 'coworker\scripts\monitor-task-source.ps1'
            Arguments       = @('-Once')
        }
    )
}

