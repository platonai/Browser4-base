#!/usr/bin/env node
/**
 * Browser4 CLI — drive a Browser4 server from the command line.
 *
 * Usage:
 *   browser4-cli open [--server <url>]
 *   browser4-cli goto <url>
 *   browser4-cli click <ref>
 *   browser4-cli type <text>
 *   browser4-cli press <key>
 *   browser4-cli screenshot [<output-file>]
 *   browser4-cli snapshot
 *   browser4-cli close
 *   browser4-cli help
 *
 * Element references
 *   The accessibility snapshot labels each interactive node with a short
 *   identifier such as `e15`.  Pass this directly to `click`, `type`, or
 *   `press`; the CLI will translate it to `backend:15` before contacting
 *   the server.  Plain CSS selectors are also accepted.
 *
 * State persistence
 *   The active session ID and the last-focused element ref are kept in
 *   ~/.browser4/cli-state.json between invocations.
 */

import * as fs from 'fs';
import * as path from 'path';
import axios, { AxiosInstance } from 'axios';
import { readState, writeState, clearState, resolveRef, CliState } from './state';

// ---------------------------------------------------------------------------
// HTTP helpers
// ---------------------------------------------------------------------------

function makeAxios(baseUrl: string): AxiosInstance {
  return axios.create({
    baseURL: baseUrl.replace(/\/$/, ''),
    timeout: 30_000,
    headers: { 'Content-Type': 'application/json' },
  });
}

/**
 * Unwrap a Browser4 REST response.
 * Successful responses have the shape `{ value: <payload> }`.
 */
function unwrap(data: unknown): unknown {
  if (data !== null && typeof data === 'object' && 'value' in (data as object)) {
    return (data as { value: unknown }).value;
  }
  return data;
}

async function apiPost(ax: AxiosInstance, url: string, body: unknown): Promise<unknown> {
  const res = await ax.post(url, body);
  return unwrap(res.data);
}

async function apiGet(ax: AxiosInstance, url: string): Promise<unknown> {
  const res = await ax.get(url);
  return unwrap(res.data);
}

async function apiDelete(ax: AxiosInstance, url: string): Promise<unknown> {
  const res = await ax.delete(url);
  return unwrap(res.data);
}

// ---------------------------------------------------------------------------
// Command implementations
// ---------------------------------------------------------------------------

/**
 * `open` — create a new session (and browser window) on the server.
 *
 * Options:
 *   --server <url>   Override the default server URL (http://localhost:8182).
 */
async function cmdOpen(args: string[]): Promise<void> {
  const state = readState();

  // Allow --server flag to override the base URL.
  const serverIdx = args.indexOf('--server');
  if (serverIdx !== -1 && args[serverIdx + 1]) {
    state.baseUrl = args[serverIdx + 1];
  }

  const ax = makeAxios(state.baseUrl);
  const value = await apiPost(ax, '/session', { capabilities: {} }) as { sessionId?: string } | null;

  const sessionId =
    value !== null && typeof value === 'object' && 'sessionId' in (value as object)
      ? (value as { sessionId?: string }).sessionId
      : undefined;

  if (!sessionId) {
    throw new Error('Server did not return a sessionId');
  }

  state.sessionId = sessionId;
  delete state.activeSelector;
  writeState(state);

  console.log(`Session opened: ${sessionId}`);
}

/**
 * `goto <url>` — navigate the browser to a URL.
 */
async function cmdGoto(args: string[]): Promise<void> {
  const url = args[0];
  if (!url) {
    throw new Error('Usage: browser4-cli goto <url>');
  }

  const state = requireSession();
  const ax = makeAxios(state.baseUrl);
  await apiPost(ax, `/session/${state.sessionId}/url`, { url });
  console.log(`Navigated to ${url}`);
}

/**
 * `click <ref>` — click the element identified by `ref`.
 *
 * Accepted ref formats:
 *   e15          → backend:15   (accessibility snapshot node id)
 *   backend:15   → backend:15   (already resolved)
 *   .my-class    → .my-class    (CSS selector — passed as-is)
 */
async function cmdClick(args: string[]): Promise<void> {
  const rawRef = args[0];
  if (!rawRef) {
    throw new Error('Usage: browser4-cli click <ref>');
  }

  const selector = resolveRef(rawRef);
  const state = requireSession();
  const ax = makeAxios(state.baseUrl);
  await apiPost(ax, `/session/${state.sessionId}/selectors/click`, { selector });

  // Remember the last-clicked element so that `type` / `press` can target it.
  state.activeSelector = selector;
  writeState(state);

  console.log(`Clicked ${selector}`);
}

/**
 * `type <text>` — type text into the currently active element.
 *
 * The active element is the one that was last clicked (its selector is
 * stored in state). If no element has been clicked yet, `body` is used
 * as a fallback so that the key events reach the page.
 */
async function cmdType(args: string[]): Promise<void> {
  const text = args[0];
  if (text === undefined) {
    throw new Error('Usage: browser4-cli type <text>');
  }

  const state = requireSession();
  const selector = state.activeSelector ?? 'body';
  const ax = makeAxios(state.baseUrl);
  await apiPost(ax, `/session/${state.sessionId}/selectors/fill`, {
    selector,
    value: text,
  });
  console.log(`Typed "${text}" into ${selector}`);
}

/**
 * `press <key>` — dispatch a keyboard key event on the active element.
 *
 * Common key names: Enter, Tab, Escape, ArrowDown, ArrowUp, Space, Backspace.
 */
async function cmdPress(args: string[]): Promise<void> {
  const key = args[0];
  if (!key) {
    throw new Error('Usage: browser4-cli press <key>');
  }

  const state = requireSession();
  const selector = state.activeSelector ?? 'body';
  const ax = makeAxios(state.baseUrl);
  await apiPost(ax, `/session/${state.sessionId}/selectors/press`, { selector, key });
  console.log(`Pressed ${key} on ${selector}`);
}

/**
 * `screenshot [<output-file>]` — capture a screenshot.
 *
 * If an output file is specified, the base64 PNG is decoded and written
 * there.  Otherwise the raw base64 string is printed to stdout.
 */
async function cmdScreenshot(args: string[]): Promise<void> {
  const outputFile = args[0];

  const state = requireSession();
  const ax = makeAxios(state.baseUrl);
  const base64 = (await apiGet(ax, `/session/${state.sessionId}/screenshot`)) as string;

  if (!base64) {
    throw new Error('Server returned an empty screenshot');
  }

  if (outputFile) {
    const buf = Buffer.from(base64, 'base64');
    const outPath = path.resolve(outputFile);
    fs.writeFileSync(outPath, buf);
    console.log(`Screenshot saved to ${outPath}`);
  } else {
    // Print base64 to stdout so callers can pipe / capture it.
    console.log(base64);
  }
}

/**
 * `snapshot` — retrieve the accessibility snapshot of the current page.
 *
 * The snapshot lists all interactive nodes with their `eN` identifiers,
 * roles, labels, and other accessibility attributes.  Agents use the
 * `eN` identifiers to target elements with `click`, `type`, and `press`.
 */
async function cmdSnapshot(): Promise<void> {
  const state = requireSession();
  const ax = makeAxios(state.baseUrl);
  const snapshot = await apiGet(ax, `/session/${state.sessionId}/snapshot`);
  console.log(typeof snapshot === 'string' ? snapshot : JSON.stringify(snapshot, null, 2));
}

/**
 * `close` — close the active session and delete local state.
 */
async function cmdClose(): Promise<void> {
  let state: CliState;
  try {
    state = requireSession();
  } catch {
    // No active session — still clean up the state file.
    clearState();
    console.log('No active session found; state cleared.');
    return;
  }

  const ax = makeAxios(state.baseUrl);
  try {
    await apiDelete(ax, `/session/${state.sessionId}`);
  } catch (err) {
    // Log but don't fail — the session may have already been closed.
    const msg = err instanceof Error ? err.message : String(err);
    console.warn(`Warning: could not delete session on server: ${msg}`);
  }

  clearState();
  console.log('Session closed.');
}

/** Print usage information to stdout. */
function cmdHelp(): void {
  console.log(`
Browser4 CLI — control a Browser4 server from the command line

USAGE
  browser4-cli <command> [options]

COMMANDS
  open [--server <url>]     Open a new browser session.
                            Defaults to http://localhost:8182.
  goto <url>                Navigate the browser to a URL.
  click <ref>               Click the element identified by <ref>.
  type <text>               Type text into the active element.
  press <key>               Press a keyboard key on the active element.
  screenshot [<file>]       Capture a screenshot.  Saves PNG to <file>
                            if given, otherwise prints base64 to stdout.
  snapshot                  Print the accessibility snapshot of the page.
  close                     Close the active session.
  help                      Print this help message.

ELEMENT REFERENCES
  Accessibility snapshots label each node with an identifier such as e15.
  Pass this directly to click/type/press; the CLI converts it to the
  backend:15 selector format expected by the server.

  Examples:
    browser4-cli click e15          # click backend node 15
    browser4-cli click .my-button   # click via CSS selector

EXAMPLES
  browser4-cli open
  browser4-cli goto https://playwright.dev
  browser4-cli snapshot
  browser4-cli click e15
  browser4-cli type "page.click"
  browser4-cli press Enter
  browser4-cli screenshot page.png
  browser4-cli close
`.trim());
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/** Return the current CLI state, throwing if there is no active session. */
function requireSession(): CliState {
  const state = readState();
  if (!state.sessionId) {
    throw new Error('No active session. Run "browser4-cli open" first.');
  }
  return state;
}

// ---------------------------------------------------------------------------
// Entry point
// ---------------------------------------------------------------------------

async function main(): Promise<void> {
  const [, , command, ...rest] = process.argv;

  try {
    switch (command) {
      case 'open':
        await cmdOpen(rest);
        break;
      case 'goto':
        await cmdGoto(rest);
        break;
      case 'click':
        await cmdClick(rest);
        break;
      case 'type':
        await cmdType(rest);
        break;
      case 'press':
        await cmdPress(rest);
        break;
      case 'screenshot':
        await cmdScreenshot(rest);
        break;
      case 'snapshot':
        await cmdSnapshot();
        break;
      case 'close':
        await cmdClose();
        break;
      case 'help':
      case '--help':
      case '-h':
      case undefined:
        cmdHelp();
        break;
      default:
        console.error(`Unknown command: ${command}`);
        console.error('Run "browser4-cli help" for usage.');
        process.exit(1);
    }
  } catch (err) {
    const msg = err instanceof Error ? err.message : String(err);
    console.error(`Error: ${msg}`);
    process.exit(1);
  }
}

main();
