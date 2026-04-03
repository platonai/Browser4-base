#!/usr/bin/env pwsh

$browser4JarPattern = '.+Browser4.jar.*'

$procs = Get-CimInstance Win32_Process | Where-Object {
    $_.Name -match '^(java|javaw)\.exe$' -and
    -not [string]::IsNullOrWhiteSpace($_.CommandLine) -and
    $_.CommandLine -match $browser4JarPattern
};

if (-not $procs)
{
    Write-Output 'NO_BROWSER4_PROCESSES'
}
else
{
    $procs |
            ForEach-Object {
                $p = Get-Process -Id $_.ProcessId -ErrorAction SilentlyContinue; [PSCustomObject]@{
                    ProcessId = $_.ProcessId; Name = $_.Name; WorkingSetMB = if ($p)
                    {
                        [math]::Round($p.WorkingSet64 / 1MB, 2)
                    }
                    else
                    {
                        $null
                    };

                    CPUSeconds = if ($p)
                    {
                        [math]::Round($p.CPU, 2)
                    }
                    else
                    {
                        $null
                    };

                    StartTime = if ($p)
                    {
                        $p.StartTime
                    }
                    else
                    {
                        $null
                    };

                    ExecutablePath = $_.ExecutablePath; CommandLine = $_.CommandLine
                }
            } | Sort-Object ProcessId | Format-List
}
