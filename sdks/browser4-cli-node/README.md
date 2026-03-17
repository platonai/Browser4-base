# Browser4 CLI

> Use rust implementation instead for faster performance and a self-contained binary,
> the node version is kept only for compatible with playwright.

A command-line interface for controlling a [Browser4](https://github.com/platonai/Browser4)
server. Designed for use by AI agents through SKILLS + CLI.

## Prerequisites

- Node.js ≥ 16
- A running Browser4 server (default port **8182**)

## Installation

```bash
npm install -g @platonai/b4-playwright-cli
```

Or build from source:

```bash
cd sdks/b4-playwright-cli
npm install
npm run build
npm link        # makes b4-playwright-cli available globally
```

For automatic recompilation while editing locally:

```bash
npm run build:watch
```

If you want the globally linked `b4-playwright-cli` command to pick up changes as you save,
run `npm link` once and keep the watch build running in a separate terminal.

## Usage

```
b4-playwright-cli <command> [options]
```

### Commands

| Command | Description |
|---|---|
| `open [--server <url>]` | Open a new browser session |
| `goto <url>` | Navigate to a URL |
| `click <ref>` | Click an element |
| `type <text>` | Type text into the active element |
| `press <key>` | Press a keyboard key |
| `keydown <key>` | Press and hold a keyboard key |
| `keyup <key>` | Release a keyboard key |
| `mousemove <x> <y>` | Move the mouse to viewport coordinates |
| `mousedown [button]` | Press a mouse button at the current cursor position |
| `mouseup [button]` | Release a mouse button at the current cursor position |
| `mousewheel <dx> <dy>` | Scroll the mouse wheel by deltas |
| `screenshot [<file>]` | Take a screenshot |
| `snapshot` | Print the accessibility snapshot |
| `close` | Close the active session |
| `help` | Show usage information |

### Element References

The `snapshot` command returns an accessibility tree where every interactive
node is labelled with a short identifier such as `e15`. Pass this identifier
directly to `click`, `type`, or `press`; the CLI automatically converts it to
the `backend:15` selector format required by the server.

You can also pass plain CSS selectors (e.g. `.my-button`, `#search-input`) or
fully-qualified `backend:<N>` refs directly.

## Examples

```shell
# Open a new browser window
b4-playwright-cli open

# Navigate to a page
b4-playwright-cli goto https://browser4.io

# Inspect the page — note the eN labels on interactive nodes
b4-playwright-cli snapshot

# Interact using refs from the snapshot
# e15 → backend:15 is handled automatically
b4-playwright-cli click e15
b4-playwright-cli type "page.click"
b4-playwright-cli press Enter
b4-playwright-cli keydown Shift
b4-playwright-cli mousemove 150 300
b4-playwright-cli mousewheel 0 100
b4-playwright-cli keyup Shift

# Take a screenshot and save it to disk
b4-playwright-cli screenshot output.png

# Use a custom server URL
b4-playwright-cli open --server http://localhost:9090

# Close the session when done
b4-playwright-cli close
```

## State

The CLI persists the active session ID and the last-clicked element
(for `type` / `press`) to `~/.browser4/cli-state.json`. This file is
created automatically on `open` and removed on `close`.
