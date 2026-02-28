# Fix copilot command argument parsing

## Problem

Multiple scripts in coworker/scripts/ call `gh copilot` with a prompt that includes newlines and double quotes.
This causes the command to fail with `error: unknown option '---`.

Error message:
```
Calling gh copilot...
error: unknown option '---
#'

Try 'copilot --help' for more information.
Memory generation task completed.
```

## Solution

- Escape double quotes in the prompt and wrap in quotes to ensure correct argument parsing
- Pass arguments as an array to avoid fragile manual escaping/quoting.

```shell
# Escape double quotes in the prompt and wrap in quotes to ensure correct argument parsing
$safePrompt = $prompt.Replace('"', '\"')

# Pass arguments as an array to avoid fragile manual escaping/quoting.
# This keeps quotes/newlines intact in the -p prompt.
$copilotArgList = @(
    'copilot'
    '-p'
    "`"$safePrompt`""
    '--allow-all-tools'
    '--allow-all-paths'
)

$process = Start-Process -FilePath 'gh' -ArgumentList $copilotArgList -NoNewWindow -PassThru -RedirectStandardOutput $stdOutLog -RedirectStandardError $stdErrLog
```

#auto-approve
