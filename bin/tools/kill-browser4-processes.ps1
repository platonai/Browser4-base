#!/usr/bin/env pwsh

# Get all processes with the name "java" and filter those that have "Browser4.jar" in their command line
$procs = Get-CimInstance Win32_Process | Where-Object { $_.Name -match '^(java|javaw)\.exe$' -and $_.CommandLine -match 'Browser4\.jar' }

if (-not $procs)
{
    Write-Output 'NO_BROWSER4_PROCESSES'
}
else
{
    foreach ($proc in $procs)
    {
        try
        {
            # Attempt to kill the process
            Stop-Process -Id $proc.ProcessId -Force -ErrorAction Stop
            Write-Output "Killed process with ID: $( $proc.ProcessId )"
        }
        catch
        {
            Write-Output "Failed to kill process with ID: $( $proc.ProcessId ). Error: $_"
        }
    }
}
