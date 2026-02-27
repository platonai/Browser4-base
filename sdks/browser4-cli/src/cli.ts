#!/usr/bin/env node
/**
 * Browser4 CLI — drive a Browser4 server from the command line.
 *
 * Implements every command described in `browser4-cli-basic.md`.
 *
 * All operations are routed through the Browser4 MCP Server tool interface
 * via `POST /mcp/call-tool`.
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
import * as os from 'os';
import { spawn, ChildProcess } from 'child_process';
import * as https from 'https';
import axios, { AxiosInstance } from 'axios';
import { readState, writeState, clearState, CliState } from './state';

// ---------------------------------------------------------------------------
// Server Management
// ---------------------------------------------------------------------------

async function ensureServerRunning(args: string[]): Promise<void> {
  const state = readState();
  let baseUrl = state.baseUrl || 'http://localhost:8182';
  
  // Check for --server override in args
  const serverIdx = args.indexOf('--server');
  if (serverIdx !== -1 && args[serverIdx + 1]) {
    baseUrl = args[serverIdx + 1];
  }
  
  // Only attempt to start if we're pointing to localhost
  if (!baseUrl.includes('localhost') && !baseUrl.includes('127.0.0.1')) {
    return;
  }

  // Check if already running
  try {
    const ax = makeAxios(baseUrl);
    await ax.get('/actuator/health');
    return;
  } catch (error) {
    // Not running, proceed to start
  }

  console.log('Browser4 server not running. Starting...');

  const jarPath = await findOrDownloadJar();
  const port = parseInt(new URL(baseUrl).port) || 8182;

  await startServer(jarPath, port);
}

async function findOrDownloadJar(): Promise<string> {
  // Check environment variable
  if (process.env.BROWSER4_JAR_PATH && fs.existsSync(process.env.BROWSER4_JAR_PATH)) {
    return process.env.BROWSER4_JAR_PATH;
  }

  // Check common locations
  const candidates = [
    path.join(os.homedir(), '.browser4', 'Browser4.jar'),
    path.resolve('Browser4.jar'),
    path.resolve('target', 'Browser4.jar'),
    path.resolve(__dirname, '..', '..', 'target', 'Browser4.jar'),
    path.resolve(__dirname, '..', '..', '..', 'browser4', 'browser4-agents', 'target', 'Browser4.jar'), // Monorepo location
  ];

  for (const candidate of candidates) {
    if (fs.existsSync(candidate)) {
      return candidate;
    }
  }

  // Download if not found
  const downloadPath = path.join(os.homedir(), '.browser4', 'Browser4.jar');
  await downloadJar(downloadPath);
  return downloadPath;
}

async function downloadJar(targetPath: string): Promise<void> {
  const dir = path.dirname(targetPath);
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir, { recursive: true });
  }

  const url = 'https://github.com/platonai/Browser4/releases/latest/download/Browser4.jar';
  console.log(`Downloading Browser4.jar from ${url}...`);

  return new Promise((resolve, reject) => {
    const file = fs.createWriteStream(targetPath);
    https.get(url, (response) => {
      if (response.statusCode === 301 || response.statusCode === 302) {
        if (!response.headers.location) {
            reject(new Error('Redirect without location header'));
            return;
        }
        https.get(response.headers.location, (redirectResponse) => {
          redirectResponse.pipe(file);
          file.on('finish', () => {
            file.close();
            console.log('Download complete');
            resolve();
          });
        }).on('error', (err) => {
            fs.unlinkSync(targetPath);
            reject(err);
        });
      } else {
        response.pipe(file);
        file.on('finish', () => {
          file.close();
          console.log('Download complete');
          resolve();
        });
      }
    }).on('error', (err) => {
      fs.unlinkSync(targetPath);
      reject(err);
    });
  });
}

async function startServer(jarPath: string, port: number): Promise<void> {
  console.log(`Starting server from ${jarPath} on port ${port}...`);
  
  const child = spawn('java', ['-jar', jarPath, `--server.port=${port}`], {
    detached: true,
    stdio: 'ignore' // or 'inherit' for debugging, but 'ignore' keeps it clean
  });

  child.unref(); // Allow the parent process to exit independently

  // Wait for health check
  const start = Date.now();
  const timeout = 60000;
  
  while (Date.now() - start < timeout) {
    try {
      await axios.get(`http://localhost:${port}/actuator/health`);
      console.log('Server is up and running.');
      return;
    } catch (e) {
      await new Promise(resolve => setTimeout(resolve, 1000));
    }
  }

  throw new Error(`Server failed to start within ${timeout}ms`);
}

// ---------------------------------------------------------------------------
// MCP tool call helpers
// ---------------------------------------------------------------------------

function makeAxios(baseUrl: string): AxiosInstance {
  return axios.create({
    baseURL: baseUrl.replace(/\/$/, ''),
    timeout: 30_000,
    headers: { 'Content-Type': 'application/json' },
  });
}

/**
 * Call an MCP tool on the server.
 *
 * @param ax       Axios instance
 * @param tool     MCP tool name (e.g. "navigate", "click", "aria_snapshot")
 * @param args     Tool arguments
 * @returns The text content from the first content block, or the full response.
 */
async function callTool(
  ax: AxiosInstance,
  tool: string,
  args: Record<string, unknown> = {},
): Promise<string> {
  const res = await ax.post('/mcp/call-tool', { tool, arguments: args });
  const data = res.data as { content?: Array<{ text?: string }>; isError?: boolean };

  if (data.isError) {
    const msg = data.content?.[0]?.text ?? 'Unknown MCP error';
    throw new Error(msg);
  }

  return data.content?.[0]?.text ?? '';
}

// ---------------------------------------------------------------------------
// Snapshot file helpers
// ---------------------------------------------------------------------------

const SNAPSHOT_DIR = '.browser4-cli/snapshot';
const SCREENSHOT_DIR = '.browser4-cli/screenshot';

/**
 * Ensure a directory exists (creates it recursively if needed).
 */
function ensureDir(dir: string): void {
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir, { recursive: true });
  }
}

/**
 * Generate a timestamped filename.
 */
function timestampedFilename(prefix: string, ext: string): string {
  const now = new Date().toISOString().replace(/[:.]/g, '-');
  return `${prefix}-${now}.${ext}`;
}

/**
 * Parse --filename=<value> from the args list.
 * Returns the filename value (or undefined) and the remaining args.
 */
function extractFilenameFlag(args: string[]): { filename?: string; rest: string[] } {
  const rest: string[] = [];
  let filename: string | undefined;
  for (const arg of args) {
    if (arg.startsWith('--filename=')) {
      filename = arg.slice('--filename='.length);
    } else {
      rest.push(arg);
    }
  }
  return { filename, rest };
}

// ---------------------------------------------------------------------------
// Post-command snapshot
// ---------------------------------------------------------------------------

/**
 * After each command, retrieve the current browser state via MCP tools
 * and save a snapshot.
 *
 * Output format:
 *   ### Page
 *   - Page URL: https://example.com/
 *   - Page Title: Example Domain
 *   ### Snapshot
 *   [Snapshot](.browser4-cli/snapshot/page-2026-02-14T19-22-42-679Z.yml)
 */
async function postCommandSnapshot(ax: AxiosInstance, sessionId: string): Promise<void> {
  try {
    const [pageUrl, pageTitle, snapshotContent] = await Promise.all([
      callTool(ax, 'page_url', { sessionId }),
      callTool(ax, 'page_title', { sessionId }),
      callTool(ax, 'aria_snapshot', { sessionId }),
    ]);

    const outName = timestampedFilename('page', 'yml');
    const outPath = path.resolve(SNAPSHOT_DIR, outName);
    ensureDir(path.dirname(outPath));
    fs.writeFileSync(outPath, snapshotContent, 'utf-8');

    console.log('### Page');
    console.log(`- Page URL: ${pageUrl}`);
    console.log(`- Page Title: ${pageTitle}`);
    console.log('### Snapshot');
    console.log(`[Snapshot](${outPath})`);
  } catch {
    // If snapshot fails (e.g. session just closed), silently ignore
  }
}

// ---------------------------------------------------------------------------
// Argument parsing helpers
// ---------------------------------------------------------------------------

/**
 * Parse global flags that appear before the command.
 * Currently supports: -s=<sessionName>, --server=<url>
 */
interface GlobalFlags {
  sessionName?: string;
  args: string[];
}

function parseGlobalFlags(argv: string[]): GlobalFlags {
  const args: string[] = [];
  let sessionName: string | undefined;

  for (const arg of argv) {
    if (arg.startsWith('-s=')) {
      sessionName = arg.slice('-s='.length);
    } else {
      args.push(arg);
    }
  }
  return { sessionName, args };
}

// ---------------------------------------------------------------------------
// Command implementations
// ---------------------------------------------------------------------------

/**
 * `open` — create a new session (and browser window) on the server.
 */
async function cmdOpen(args: string[], sessionName?: string): Promise<void> {
  const state = readState();

  const serverIdx = args.indexOf('--server');
  if (serverIdx !== -1 && args[serverIdx + 1]) {
    state.baseUrl = args[serverIdx + 1];
  }

  const ax = makeAxios(state.baseUrl);

  // Filter out flags; any remaining positional arg is the URL to navigate to.
  const positional = args.filter(
    a => !a.startsWith('--') && (serverIdx === -1 || a !== args[serverIdx + 1]),
  );
  const initialUrl = positional[0];

  const result = await callTool(ax, 'open_session', {});
  const parsed = JSON.parse(result);
  const sessionId = parsed.sessionId;

  if (!sessionId) {
    throw new Error('Server did not return a sessionId');
  }

  state.sessionId = sessionId;
  if (sessionName) state.sessionName = sessionName;
  delete state.activeSelector;
  writeState(state);

  console.log(`Session opened: ${sessionId}`);

  // If a URL was provided, navigate to it right away.
  if (initialUrl) {
    await callTool(ax, 'navigate', { sessionId, url: initialUrl });
    console.log(`Navigated to ${initialUrl}`);
  }
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
  await callTool(ax, 'navigate', { sessionId: state.sessionId, url });
  console.log(`Navigated to ${url}`);
}

/**
 * `click <ref>` — click the element identified by `ref`.
 */
async function cmdClick(args: string[]): Promise<void> {
  const rawRef = args[0];
  if (!rawRef) {
    throw new Error('Usage: browser4-cli click <ref>');
  }

  const selector = rawRef;
  const state = requireSession();
  const ax = makeAxios(state.baseUrl);
  await callTool(ax, 'click', { sessionId: state.sessionId, selector });

  state.activeSelector = selector;
  writeState(state);

  console.log(`Clicked ${selector}`);
}

/**
 * `dblclick <ref>` — double-click the element identified by `ref`.
 */
async function cmdDblclick(args: string[]): Promise<void> {
  const rawRef = args[0];
  if (!rawRef) {
    throw new Error('Usage: browser4-cli dblclick <ref>');
  }

  const selector = rawRef;
  const state = requireSession();
  const ax = makeAxios(state.baseUrl);
  await callTool(ax, 'dblclick', { sessionId: state.sessionId, selector });

  state.activeSelector = selector;
  writeState(state);

  console.log(`Double-clicked ${selector}`);
}

/**
 * `fill <ref> <text>` — clear an input field and type text into it.
 */
async function cmdFill(args: string[]): Promise<void> {
  const rawRef = args[0];
  const text = args[1];
  if (!rawRef || text === undefined) {
    throw new Error('Usage: browser4-cli fill <ref> <text>');
  }

  const selector = rawRef;
  const state = requireSession();
  const ax = makeAxios(state.baseUrl);
  await callTool(ax, 'fill', { sessionId: state.sessionId, selector, text });

  state.activeSelector = selector;
  writeState(state);

  console.log(`Filled ${selector} with "${text}"`);
}

/**
 * `drag <sourceRef> <targetRef>` — drag from one element to another.
 */
async function cmdDrag(args: string[]): Promise<void> {
  const srcRef = args[0];
  const tgtRef = args[1];
  if (!srcRef || !tgtRef) {
    throw new Error('Usage: browser4-cli drag <sourceRef> <targetRef>');
  }

  const sourceSelector = srcRef;
  const targetSelector = tgtRef;
  const state = requireSession();
  const ax = makeAxios(state.baseUrl);
  await callTool(ax, 'drag', {
    sessionId: state.sessionId,
    source_selector: sourceSelector,
    target_selector: targetSelector,
  });

  console.log(`Dragged ${sourceSelector} to ${targetSelector}`);
}

/**
 * `hover <ref>` — hover over an element.
 */
async function cmdHover(args: string[]): Promise<void> {
  const rawRef = args[0];
  if (!rawRef) {
    throw new Error('Usage: browser4-cli hover <ref>');
  }

  const selector = rawRef;
  const state = requireSession();
  const ax = makeAxios(state.baseUrl);
  await callTool(ax, 'hover', { sessionId: state.sessionId, selector });

  console.log(`Hovered over ${selector}`);
}

/**
 * `select <ref> <value>` — select a dropdown option.
 */
async function cmdSelect(args: string[]): Promise<void> {
  const rawRef = args[0];
  const value = args[1];
  if (!rawRef || value === undefined) {
    throw new Error('Usage: browser4-cli select <ref> <value>');
  }

  const selector = rawRef;
  const state = requireSession();
  const ax = makeAxios(state.baseUrl);
  await callTool(ax, 'select_option', { sessionId: state.sessionId, selector, value });

  console.log(`Selected "${value}" in ${selector}`);
}

/**
 * `upload <ref> <filePath>` — upload a file to a file input.
 */
async function cmdUpload(args: string[]): Promise<void> {
  let selector = 'input[type=file]';
  let filePaths: string[] = [];

  if (args.length === 1) {
    // browser4-cli upload <file>
    filePaths = [args[0]];
  } else if (args.length >= 2) {
    // browser4-cli upload <ref> <file> [file2...]
    const rawRef = args[0];
    selector = rawRef;
    filePaths = args.slice(1);
  } else {
    throw new Error('Usage: browser4-cli upload [<ref>] <filePath> [filePath2...]');
  }

  const state = requireSession();
  const ax = makeAxios(state.baseUrl);
  const absPaths = filePaths.map(p => path.resolve(p));

  await callTool(ax, 'upload', {
    sessionId: state.sessionId,
    selector,
    paths: absPaths,
  });

  console.log(`Uploaded ${absPaths.join(', ')} to ${selector}`);
}

/**
 * `check <ref>` — check a checkbox.
 */
async function cmdCheck(args: string[]): Promise<void> {
  const rawRef = args[0];
  if (!rawRef) {
    throw new Error('Usage: browser4-cli check <ref>');
  }

  const selector = rawRef;
  const state = requireSession();
  const ax = makeAxios(state.baseUrl);
  await callTool(ax, 'check', { sessionId: state.sessionId, selector });

  console.log(`Checked ${selector}`);
}

/**
 * `uncheck <ref>` — uncheck a checkbox.
 */
async function cmdUncheck(args: string[]): Promise<void> {
  const rawRef = args[0];
  if (!rawRef) {
    throw new Error('Usage: browser4-cli uncheck <ref>');
  }

  const selector = rawRef;
  const state = requireSession();
  const ax = makeAxios(state.baseUrl);
  await callTool(ax, 'uncheck', { sessionId: state.sessionId, selector });

  console.log(`Unchecked ${selector}`);
}

/**
 * `type <text>` — type text into the currently active element.
 */
async function cmdType(args: string[]): Promise<void> {
  const text = args[0];
  if (text === undefined) {
    throw new Error('Usage: browser4-cli type <text>');
  }

  const state = requireSession();
  const selector = state.activeSelector ?? 'body';
  const ax = makeAxios(state.baseUrl);
  await callTool(ax, 'fill', { sessionId: state.sessionId, selector, text });
  console.log(`Typed "${text}" into ${selector}`);
}

/**
 * `press <key>` — dispatch a keyboard key event on the active element.
 */
async function cmdPress(args: string[]): Promise<void> {
  const key = args[0];
  if (!key) {
    throw new Error('Usage: browser4-cli press <key>');
  }

  const state = requireSession();
  const selector = state.activeSelector ?? 'body';
  const ax = makeAxios(state.baseUrl);
  await callTool(ax, 'press', { sessionId: state.sessionId, selector, key });
  console.log(`Pressed ${key} on ${selector}`);
}

/**
 * `keydown <key>` — dispatch a keydown event.
 */
async function cmdKeydown(args: string[]): Promise<void> {
  const key = args[0];
  if (!key) {
    throw new Error('Usage: browser4-cli keydown <key>');
  }

  const state = requireSession();
  const ax = makeAxios(state.baseUrl);
  await callTool(ax, 'keydown', { sessionId: state.sessionId, key });
  console.log(`Key down: ${key}`);
}

/**
 * `keyup <key>` — dispatch a keyup event.
 */
async function cmdKeyup(args: string[]): Promise<void> {
  const key = args[0];
  if (!key) {
    throw new Error('Usage: browser4-cli keyup <key>');
  }

  const state = requireSession();
  const ax = makeAxios(state.baseUrl);
  await callTool(ax, 'keyup', { sessionId: state.sessionId, key });
  console.log(`Key up: ${key}`);
}

// ---------------------------------------------------------------------------
// Mouse commands
// ---------------------------------------------------------------------------

/**
 * `mousemove <x> <y>` — move the mouse to coordinates.
 */
async function cmdMousemove(args: string[]): Promise<void> {
  const x = parseFloat(args[0]);
  const y = parseFloat(args[1]);
  if (isNaN(x) || isNaN(y)) {
    throw new Error('Usage: browser4-cli mousemove <x> <y>');
  }

  const state = requireSession();
  const ax = makeAxios(state.baseUrl);
  await callTool(ax, 'mousemove', { sessionId: state.sessionId, x, y });
  console.log(`Mouse moved to (${x}, ${y})`);
}

/**
 * `mousedown [button]` — dispatch a mousedown event.
 */
async function cmdMousedown(args: string[]): Promise<void> {
  const button = args[0] ?? 'left';
  const state = requireSession();
  const ax = makeAxios(state.baseUrl);
  await callTool(ax, 'mousedown', { sessionId: state.sessionId, button });
  console.log(`Mouse down (${button})`);
}

/**
 * `mouseup [button]` — dispatch a mouseup event.
 */
async function cmdMouseup(args: string[]): Promise<void> {
  const button = args[0] ?? 'left';
  const state = requireSession();
  const ax = makeAxios(state.baseUrl);
  await callTool(ax, 'mouseup', { sessionId: state.sessionId, button });
  console.log(`Mouse up (${button})`);
}

/**
 * `mousewheel <deltaX> <deltaY>` — dispatch a mouse wheel event.
 */
async function cmdMousewheel(args: string[]): Promise<void> {
  const deltaX = parseFloat(args[0] ?? '0');
  const deltaY = parseFloat(args[1] ?? '100');
  if (isNaN(deltaX) || isNaN(deltaY)) {
    throw new Error('Usage: browser4-cli mousewheel <deltaX> <deltaY>');
  }

  const state = requireSession();
  const ax = makeAxios(state.baseUrl);
  await callTool(ax, 'mousewheel', { sessionId: state.sessionId, delta_x: deltaX, delta_y: deltaY });
  console.log(`Mouse wheel (${deltaX}, ${deltaY})`);
}

// ---------------------------------------------------------------------------
// Navigation commands
// ---------------------------------------------------------------------------

async function cmdGoBack(): Promise<void> {
  const state = requireSession();
  const ax = makeAxios(state.baseUrl);
  await callTool(ax, 'go_back', { sessionId: state.sessionId });
  console.log('Navigated back');
}

async function cmdGoForward(): Promise<void> {
  const state = requireSession();
  const ax = makeAxios(state.baseUrl);
  await callTool(ax, 'go_forward', { sessionId: state.sessionId });
  console.log('Navigated forward');
}

async function cmdReload(): Promise<void> {
  const state = requireSession();
  const ax = makeAxios(state.baseUrl);
  await callTool(ax, 'reload', { sessionId: state.sessionId });
  console.log('Page reloaded');
}

// ---------------------------------------------------------------------------
// Screenshot / PDF / Snapshot
// ---------------------------------------------------------------------------

/**
 * `screenshot [<ref>] [--filename=<file>]` — capture a screenshot.
 */
async function cmdScreenshot(args: string[]): Promise<void> {
  const { filename, rest } = extractFilenameFlag(args);

  const state = requireSession();
  const ax = makeAxios(state.baseUrl);

  const ref = rest[0];
  const toolArgs: Record<string, unknown> = { sessionId: state.sessionId };
  if (ref) {
    toolArgs.selector = ref;
  }

  const base64 = await callTool(ax, 'screenshot', toolArgs);

  if (!base64) {
    throw new Error('Server returned an empty screenshot');
  }

  const outName = filename ?? timestampedFilename('page', 'png');
  const outDir = path.dirname(outName) === '.' && !filename ? SCREENSHOT_DIR : path.dirname(outName);
  const outPath = filename ? path.resolve(outName) : path.resolve(outDir, outName);

  ensureDir(path.dirname(outPath));
  const buf = Buffer.from(base64, 'base64');
  fs.writeFileSync(outPath, buf);
  console.log(`Screenshot saved to ${outPath}`);
}

/**
 * `pdf [--filename=<file>]` — generate a PDF of the current page.
 */
async function cmdPdf(args: string[]): Promise<void> {
  const { filename } = extractFilenameFlag(args);

  const state = requireSession();
  const ax = makeAxios(state.baseUrl);

  // PDF generation via evaluate as fallback
  const base64 = await callTool(ax, 'evaluate', {
    sessionId: state.sessionId,
    expression: "'PDF generation not directly supported; use screenshot as alternative'",
  });

  const outName = filename ?? timestampedFilename('page', 'pdf');
  const outPath = path.resolve(outName);
  ensureDir(path.dirname(outPath));

  // Minimum expected base64 length for a valid PDF header
  const MIN_PDF_BASE64_LEN = 100;
  if (base64 && typeof base64 === 'string' && base64.length > MIN_PDF_BASE64_LEN) {
    const buf = Buffer.from(base64, 'base64');
    fs.writeFileSync(outPath, buf);
  } else {
    throw new Error('Server did not return valid PDF data');
  }
  console.log(`PDF saved to ${outPath}`);
}

/**
 * `snapshot [--filename=<file>]` — retrieve the accessibility snapshot.
 */
async function cmdSnapshot(args: string[]): Promise<void> {
  const { filename } = extractFilenameFlag(args);

  const state = requireSession();
  const ax = makeAxios(state.baseUrl);
  const snapshot = await callTool(ax, 'aria_snapshot', { sessionId: state.sessionId });

  const content = typeof snapshot === 'string' ? snapshot : JSON.stringify(snapshot, null, 2);

  // Save to file
  const outName = filename ?? timestampedFilename('page', 'yml');
  const outDir = path.dirname(outName) === '.' && !filename ? SNAPSHOT_DIR : path.dirname(outName);
  const outPath = filename ? path.resolve(outName) : path.resolve(outDir, outName);

  ensureDir(path.dirname(outPath));
  fs.writeFileSync(outPath, content, 'utf-8');

  console.log('### Page');
  // Get page info via MCP tools
  try {
    const url = await callTool(ax, 'page_url', { sessionId: state.sessionId });
    const title = await callTool(ax, 'page_title', { sessionId: state.sessionId });
    console.log(`- Page URL: ${url}`);
    console.log(`- Page Title: ${title}`);
  } catch {
    // Ignore if page info is unavailable
  }
  console.log('### Snapshot');
  console.log(`[Snapshot](${outPath})`);
}

// ---------------------------------------------------------------------------
// Eval (JavaScript execution)
// ---------------------------------------------------------------------------

/**
 * `eval <expression> [<ref>]` — evaluate JavaScript.
 */
async function cmdEval(args: string[]): Promise<void> {
  const expression = args[0];
  if (!expression) {
    throw new Error('Usage: browser4-cli eval <expression> [<ref>]');
  }

  const state = requireSession();
  const ax = makeAxios(state.baseUrl);

  let script: string;
  const rawRef = args[1];
  if (rawRef) {
    const selector = rawRef;
    const safeSelector = JSON.stringify(selector);
    script = `(function() { var el = document.querySelector(${safeSelector}); return (${expression})(el); })()`;
  } else {
    script = expression;
  }

  const result = await callTool(ax, 'evaluate', { sessionId: state.sessionId, expression: script });
  console.log(result);
}

// ---------------------------------------------------------------------------
// Dialog handling
// ---------------------------------------------------------------------------

async function cmdDialogAccept(args: string[]): Promise<void> {
  const promptText = args[0] ?? undefined;
  const state = requireSession();
  const ax = makeAxios(state.baseUrl);
  await callTool(ax, 'dialog_accept', {
    sessionId: state.sessionId,
    ...(promptText !== undefined ? { prompt_text: promptText } : {}),
  });
  console.log('Dialog accepted');
}

async function cmdDialogDismiss(): Promise<void> {
  const state = requireSession();
  const ax = makeAxios(state.baseUrl);
  await callTool(ax, 'dialog_dismiss', { sessionId: state.sessionId });
  console.log('Dialog dismissed');
}

// ---------------------------------------------------------------------------
// Resize
// ---------------------------------------------------------------------------

async function cmdResize(args: string[]): Promise<void> {
  const width = parseInt(args[0], 10);
  const height = parseInt(args[1], 10);
  if (isNaN(width) || isNaN(height)) {
    throw new Error('Usage: browser4-cli resize <width> <height>');
  }

  const state = requireSession();
  const ax = makeAxios(state.baseUrl);
  await callTool(ax, 'resize', { sessionId: state.sessionId, width, height });
  console.log(`Resized to ${width}x${height}`);
}

// ---------------------------------------------------------------------------
// Tab management
// ---------------------------------------------------------------------------

async function cmdTabList(): Promise<void> {
  const state = requireSession();
  const ax = makeAxios(state.baseUrl);
  const tabs = await callTool(ax, 'tab_list', { sessionId: state.sessionId });
  console.log(tabs);
}

async function cmdTabNew(args: string[]): Promise<void> {
  const url = args[0] ?? undefined;
  const state = requireSession();
  const ax = makeAxios(state.baseUrl);
  await callTool(ax, 'tab_new', {
    sessionId: state.sessionId,
    ...(url !== undefined ? { url } : {}),
  });
  console.log(url ? `New tab opened: ${url}` : 'New tab opened');
}

async function cmdTabClose(args: string[]): Promise<void> {
  const state = requireSession();
  const ax = makeAxios(state.baseUrl);
  await callTool(ax, 'tab_close', { sessionId: state.sessionId });
  console.log('Tab closed');
}

async function cmdTabSelect(args: string[]): Promise<void> {
  const index = parseInt(args[0], 10);
  if (isNaN(index)) {
    throw new Error('Usage: browser4-cli tab-select <index>');
  }

  const state = requireSession();
  const ax = makeAxios(state.baseUrl);
  await callTool(ax, 'tab_select', { sessionId: state.sessionId, index });
  console.log(`Switched to tab ${index}`);
}

// ---------------------------------------------------------------------------
// Session management
// ---------------------------------------------------------------------------

async function cmdList(): Promise<void> {
  const state = readState();
  const ax = makeAxios(state.baseUrl);
  const sessions = await callTool(ax, 'list_sessions', {});
  console.log(sessions);
}

/**
 * `close` — close the active session and delete local state.
 */
async function cmdClose(): Promise<void> {
  let state: CliState;
  try {
    state = requireSession();
  } catch {
    clearState();
    console.log('No active session found; state cleared.');
    return;
  }

  const ax = makeAxios(state.baseUrl);
  try {
    await callTool(ax, 'close_session', { sessionId: state.sessionId });
  } catch (err) {
    const msg = err instanceof Error ? err.message : String(err);
    console.warn(`Warning: could not delete session on server: ${msg}`);
  }

  clearState();
  console.log('Session closed.');
}

async function cmdCloseAll(): Promise<void> {
  const state = readState();
  const ax = makeAxios(state.baseUrl);
  const result = await callTool(ax, 'close_all_sessions', {});
  clearState();
  console.log(result);
}

async function cmdKillAll(): Promise<void> {
  const state = readState();
  const ax = makeAxios(state.baseUrl);
  const result = await callTool(ax, 'kill_all_sessions', {});
  clearState();
  console.log(result);
}

async function cmdDeleteData(): Promise<void> {
  const state = requireSession();
  const ax = makeAxios(state.baseUrl);
  await callTool(ax, 'delete_session_data', { sessionId: state.sessionId });
  console.log('User data deleted for session.');
}

// ---------------------------------------------------------------------------
// Help
// ---------------------------------------------------------------------------

function cmdHelp(): void {
  console.log(`
Browser4 CLI — control a Browser4 server from the command line

USAGE
  browser4-cli [options] <command> [arguments]

GLOBAL OPTIONS
  -s=<name>                 Use a named session.
  --server <url>            Override the default server URL.

CORE COMMANDS
  open [url] [--server <url>] [--persistent] [--profile=<dir>]
                            Open a new browser session, optionally navigate.
  goto <url>                Navigate the browser to a URL.
  type <text>               Type text into the active element.
  click <ref>               Click an element.
  dblclick <ref>            Double-click an element.
  fill <ref> <text>         Clear & type text into an input.
  drag <srcRef> <tgtRef>    Drag from one element to another.
  hover <ref>               Hover over an element.
  select <ref> <value>      Select a dropdown option.
  upload <filePath>         Upload a file.
  check <ref>               Check a checkbox.
  uncheck <ref>             Uncheck a checkbox.
  snapshot [--filename=<f>] Accessibility snapshot of the page.
  eval <expr> [<ref>]       Evaluate JavaScript expression.
  dialog-accept [text]      Accept the current dialog.
  dialog-dismiss            Dismiss the current dialog.
  resize <width> <height>   Resize the browser viewport.
  close                     Close the active session.

NAVIGATION
  go-back                   Navigate back.
  go-forward                Navigate forward.
  reload                    Reload the page.

KEYBOARD
  press <key>               Press a key (Enter, ArrowDown, etc.).
  keydown <key>             Dispatch keydown event.
  keyup <key>               Dispatch keyup event.

MOUSE
  mousemove <x> <y>         Move mouse to coordinates.
  mousedown [button]        Dispatch mousedown (left/right/middle).
  mouseup [button]          Dispatch mouseup.
  mousewheel <dX> <dY>      Dispatch mouse wheel event.

SAVE AS
  screenshot [<ref>] [--filename=<f>]
                            Capture a screenshot.
  pdf [--filename=<f>]      Generate a PDF.

TABS
  tab-list                  List open tabs.
  tab-new [url]             Open a new tab.
  tab-close [index]         Close a tab.
  tab-select <index>        Switch to a tab.

SESSION MANAGEMENT
  list                      List all active sessions.
  close-all                 Close all sessions.
  kill-all                  Kill all browser processes.
  delete-data               Delete user data for the session.

ELEMENT REFERENCES
  e15          →  backend:15   (accessibility snapshot id)
  .my-class    →  .my-class    (CSS selector)
  backend:15   →  backend:15   (already resolved)

EXAMPLES
  browser4-cli open
  browser4-cli goto https://browser4.io
  browser4-cli snapshot
  browser4-cli click e15
  browser4-cli type "page.click"
  browser4-cli press Enter
  browser4-cli screenshot --filename=page.png
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

/** Commands that should NOT trigger a post-command snapshot. */
const NO_SNAPSHOT_COMMANDS = new Set([
  'open', 'close', 'close-all', 'kill-all', 'list',
  'help', '--help', '-h', 'snapshot', 'screenshot', 'pdf',
]);

// ---------------------------------------------------------------------------
// Entry point
// ---------------------------------------------------------------------------

async function main(): Promise<void> {
  // Parse global flags from raw process.argv (skip node + script path)
  const rawArgs = process.argv.slice(2);
  const { sessionName, args: remaining } = parseGlobalFlags(rawArgs);
  const [command, ...rest] = remaining;

  try {
    if (command !== 'help' && command !== '--help' && command !== '-h' && command !== undefined) {
      // Pass remaining args to check for --server flag
      await ensureServerRunning(rest);
    }
    
    switch (command) {
      // Core
      case 'open':        await cmdOpen(rest, sessionName); break;
      case 'goto':        await cmdGoto(rest); break;
      case 'click':       await cmdClick(rest); break;
      case 'dblclick':    await cmdDblclick(rest); break;
      case 'fill':        await cmdFill(rest); break;
      case 'drag':        await cmdDrag(rest); break;
      case 'hover':       await cmdHover(rest); break;
      case 'select':      await cmdSelect(rest); break;
      case 'upload':      await cmdUpload(rest); break;
      case 'check':       await cmdCheck(rest); break;
      case 'uncheck':     await cmdUncheck(rest); break;
      case 'type':        await cmdType(rest); break;
      case 'snapshot':    await cmdSnapshot(rest); break;
      case 'eval':        await cmdEval(rest); break;
      case 'dialog-accept':  await cmdDialogAccept(rest); break;
      case 'dialog-dismiss': await cmdDialogDismiss(); break;
      case 'resize':      await cmdResize(rest); break;
      case 'close':       await cmdClose(); break;

      // Navigation
      case 'go-back':     await cmdGoBack(); break;
      case 'go-forward':  await cmdGoForward(); break;
      case 'reload':      await cmdReload(); break;

      // Keyboard
      case 'press':       await cmdPress(rest); break;
      case 'keydown':     await cmdKeydown(rest); break;
      case 'keyup':       await cmdKeyup(rest); break;

      // Mouse
      case 'mousemove':   await cmdMousemove(rest); break;
      case 'mousedown':   await cmdMousedown(rest); break;
      case 'mouseup':     await cmdMouseup(rest); break;
      case 'mousewheel':  await cmdMousewheel(rest); break;

      // Save as
      case 'screenshot':  await cmdScreenshot(rest); break;
      case 'pdf':         await cmdPdf(rest); break;

      // Tabs
      case 'tab-list':    await cmdTabList(); break;
      case 'tab-new':     await cmdTabNew(rest); break;
      case 'tab-close':   await cmdTabClose(rest); break;
      case 'tab-select':  await cmdTabSelect(rest); break;

      // Session management
      case 'list':        await cmdList(); break;
      case 'close-all':   await cmdCloseAll(); break;
      case 'kill-all':    await cmdKillAll(); break;
      case 'delete-data': await cmdDeleteData(); break;

      // Help
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

    // Post-command snapshot for commands that modify browser state
    if (command && !NO_SNAPSHOT_COMMANDS.has(command)) {
      const state = readState();
      if (state.sessionId) {
        const ax = makeAxios(state.baseUrl);
        await postCommandSnapshot(ax, state.sessionId);
      }
    }
  } catch (err) {
    const msg = err instanceof Error ? err.message : String(err);
    console.error(`Error: ${msg}`);
    process.exit(1);
  }
}

main();
