---
name: browser4-cli
description: Automates browser interactions for web testing, form filling, screenshots, and data extraction. Use when the user needs to navigate websites, interact with web pages, fill forms, take screenshots, test web applications, or extract information from web pages.
allowed-tools: Bash(browser4-cli:*)
---

# Browser Automation with browser4-cli

## Quick start

```bash
# open new browser
browser4-cli open
# navigate to a page
browser4-cli goto https://browser4.io/
# take a snapshot
browser4-cli snapshot
# interact with the page using refs from the snapshot
browser4-cli click e15
browser4-cli type "page.click"
browser4-cli press Enter
# take a screenshot
browser4-cli screenshot
# close the browser
browser4-cli close
```

## Commands

### Core

```bash
browser4-cli open
# open and navigate right away
browser4-cli open https://example.com/
browser4-cli goto https://browser4.io/
browser4-cli type "search query"
browser4-cli click e3
browser4-cli dblclick e7
browser4-cli fill e5 "user@example.com"
browser4-cli drag e2 e8
browser4-cli hover e4
browser4-cli select e9 "option-value"
browser4-cli upload ./document.pdf
browser4-cli check e12
browser4-cli uncheck e12
browser4-cli snapshot
browser4-cli snapshot --filename=after-click.yaml
browser4-cli eval "document.title"
browser4-cli eval "el => el.textContent" e5
browser4-cli dialog-accept
browser4-cli dialog-accept "confirmation text"
browser4-cli dialog-dismiss
browser4-cli resize 1920 1080
browser4-cli close
```

### Navigation

```bash
browser4-cli go-back
browser4-cli go-forward
browser4-cli reload
```

### Keyboard

```bash
browser4-cli press Enter
browser4-cli press ArrowDown
browser4-cli keydown Shift
browser4-cli keyup Shift
```

### Mouse

```bash
browser4-cli mousemove 150 300
browser4-cli mousedown
browser4-cli mousedown right
browser4-cli mouseup
browser4-cli mouseup right
browser4-cli mousewheel 0 100
```

### Save as

```bash
browser4-cli screenshot
browser4-cli screenshot e5
browser4-cli screenshot --filename=page.png
browser4-cli pdf --filename=page.pdf
```

### Tabs

```bash
browser4-cli tab-list
browser4-cli tab-new
browser4-cli tab-new https://example.com/page
browser4-cli tab-close
browser4-cli tab-close 2
browser4-cli tab-select 0
```

### Storage

```bash
browser4-cli state-save
browser4-cli state-save auth.json
browser4-cli state-load auth.json

# Cookies
browser4-cli cookie-list
browser4-cli cookie-list --domain=example.com
browser4-cli cookie-get session_id
browser4-cli cookie-set session_id abc123
browser4-cli cookie-set session_id abc123 --domain=example.com --httpOnly --secure
browser4-cli cookie-delete session_id
browser4-cli cookie-clear

# LocalStorage
browser4-cli localstorage-list
browser4-cli localstorage-get theme
browser4-cli localstorage-set theme dark
browser4-cli localstorage-delete theme
browser4-cli localstorage-clear

# SessionStorage
browser4-cli sessionstorage-list
browser4-cli sessionstorage-get step
browser4-cli sessionstorage-set step 3
browser4-cli sessionstorage-delete step
browser4-cli sessionstorage-clear
```

### Network

```bash
browser4-cli route "**/*.jpg" --status=404
browser4-cli route "https://api.example.com/**" --body='{"mock": true}'
browser4-cli route-list
browser4-cli unroute "**/*.jpg"
browser4-cli unroute
```

### DevTools

```bash
browser4-cli console
browser4-cli console warning
browser4-cli network
browser4-cli run-code "async page => await page.context().grantPermissions(['geolocation'])"
browser4-cli tracing-start
browser4-cli tracing-stop
browser4-cli video-start
browser4-cli video-stop video.webm
```

## Open parameters
```bash
# Use persistent profile with custom directory
browser4-cli open --profile=/path/to/profile

# Start with config file
browser4-cli open --config=my-config.json

# Close the browser
browser4-cli close
```

## Snapshots

After each command, browser4-cli provides a snapshot of the current browser state.

```bash
> browser4-cli goto https://example.com
### Page
- Page URL: https://example.com/
- Page Title: Example Domain
### Snapshot
[Snapshot](.browser4-cli/snapshot/page-2026-02-14T19-22-42-679Z.yml)
```

You can also take a snapshot on demand using `browser4-cli snapshot` command.

If `--filename` is not provided, a new snapshot file is created with a timestamp. Default to automatic file naming, use `--filename=` when artifact is a part of the workflow result.

## Browser Sessions

```bash
# create new browser session named "mysession" with persistent profile
browser4-cli -s=mysession open example.com --persistent
# same with manually specified profile directory (use when requested explicitly)
browser4-cli -s=mysession open example.com --profile=/path/to/profile
browser4-cli -s=mysession click e6
browser4-cli -s=mysession close  # stop a named browser
browser4-cli -s=mysession delete-data  # delete user data for persistent session

browser4-cli list
# Close all browsers
browser4-cli close-all
# Forcefully kill all browser processes
browser4-cli kill-all
```

## Local installation

In some cases user might want to install browser4-cli locally. If running globally available `browser4-cli` binary fails, use `npx browser4-cli` to run the commands. For example:

```bash
npx browser4-cli open https://example.com
npx browser4-cli click e1
```

## Example: Form submission

```bash
browser4-cli open https://example.com/form
browser4-cli snapshot

browser4-cli fill e1 "user@example.com"
browser4-cli fill e2 "password123"
browser4-cli click e3
browser4-cli snapshot
browser4-cli close
```

## Example: Multi-tab workflow

```bash
browser4-cli open https://example.com
browser4-cli tab-new https://example.com/other
browser4-cli tab-list
browser4-cli tab-select 0
browser4-cli snapshot
browser4-cli close
```

## Example: Debugging with DevTools

```bash
browser4-cli open https://example.com
browser4-cli click e4
browser4-cli fill e7 "test"
browser4-cli console
browser4-cli network
browser4-cli close
```

```bash
browser4-cli open https://example.com
browser4-cli tracing-start
browser4-cli click e4
browser4-cli fill e7 "test"
browser4-cli tracing-stop
browser4-cli close
```

## Specific tasks

* **Browser session management** [references/session-management.md](references/session-management.md)
* **Storage state (cookies, localStorage)** [references/storage-state.md](references/storage-state.md)
