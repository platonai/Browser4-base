---
name: browser4-cli
description: Automates browser interactions for web testing, form filling, screenshots, and data extraction. Use when the user needs to navigate websites, interact with web pages, fill forms, take screenshots, test web applications, or extract information from web pages.
allowed-tools: Bash(browser4-cli:*)
---

# Browser Automation with browser4-cli

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
