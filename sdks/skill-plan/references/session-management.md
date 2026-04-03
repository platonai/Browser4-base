# Browser Session Management

Run multiple isolated browser sessions concurrently with state persistence.

## Named Browser Sessions

Use `-s` flag to isolate browser contexts:

```bash
# Browser 1: Authentication flow
browser4-cli -s=auth open https://app.example.com/login

# Browser 2: Public browsing (separate cookies, storage)
browser4-cli -s=public open https://example.com

# Commands are isolated by browser session
browser4-cli -s=auth fill e1 "user@example.com"
browser4-cli -s=public snapshot
```

## Browser Session Isolation Properties

Each browser session has independent:
- Cookies
- LocalStorage / SessionStorage
- IndexedDB
- Cache
- Browsing history
- Open tabs

## Browser Session Commands

```bash
# List all browser sessions
browser4-cli list

# Stop a browser session (close the browser)
browser4-cli close                # stop the default browser
browser4-cli -s=mysession close   # stop a named browser

# Stop all browser sessions
browser4-cli close-all

# Forcefully kill all daemon processes (for stale/zombie processes)
browser4-cli kill-all

# Delete browser session user data (profile directory)
browser4-cli delete-data                # delete default browser data
browser4-cli -s=mysession delete-data   # delete named browser data
```

## Environment Variable

Set a default browser session name via environment variable:

```bash
export BROWSER4_CLI_SESSION="mysession"
browser4-cli open example.com  # Uses "mysession" automatically
```

## Common Patterns

### Concurrent Scraping

```bash
#!/bin/bash
# Scrape multiple sites concurrently

# Start all browsers
browser4-cli -s=site1 open https://site1.com &
browser4-cli -s=site2 open https://site2.com &
browser4-cli -s=site3 open https://site3.com &
wait

# Take snapshots from each
browser4-cli -s=site1 snapshot
browser4-cli -s=site2 snapshot
browser4-cli -s=site3 snapshot

# Cleanup
browser4-cli close-all
```

### A/B Testing Sessions

```bash
# Test different user experiences
browser4-cli -s=variant-a open "https://app.com?variant=a"
browser4-cli -s=variant-b open "https://app.com?variant=b"

# Compare
browser4-cli -s=variant-a screenshot
browser4-cli -s=variant-b screenshot
```

### Persistent Profile

By default, browser profile is kept in memory only. Use `--persistent` flag on `open` to persist the browser profile to disk:

```bash
# Use persistent profile (auto-generated location)
browser4-cli open https://example.com --persistent

# Use persistent profile with custom directory
browser4-cli open https://example.com --profile=/path/to/profile
```

## Default Browser Session

When `-s` is omitted, commands use the default browser session:

```bash
# These use the same default browser session
browser4-cli open https://example.com
browser4-cli snapshot
browser4-cli close  # Stops default browser
```

## Browser Session Configuration

Configure a browser session with specific settings when opening:

```bash
# Open with config file
browser4-cli open https://example.com --config=.playwright/my-cli.json

# Open with specific browser
browser4-cli open https://example.com --browser=firefox

# Open in headed mode
browser4-cli open https://example.com --headed

# Open with persistent profile
browser4-cli open https://example.com --persistent
```

## Best Practices

### 1. Name Browser Sessions Semantically

```bash
# GOOD: Clear purpose
browser4-cli -s=github-auth open https://github.com
browser4-cli -s=docs-scrape open https://docs.example.com

# AVOID: Generic names
browser4-cli -s=s1 open https://github.com
```

### 2. Always Clean Up

```bash
# Stop browsers when done
browser4-cli -s=auth close
browser4-cli -s=scrape close

# Or stop all at once
browser4-cli close-all

# If browsers become unresponsive or zombie processes remain
browser4-cli kill-all
```

### 3. Delete Stale Browser Data

```bash
# Remove old browser data to free disk space
browser4-cli -s=oldsession delete-data
```
