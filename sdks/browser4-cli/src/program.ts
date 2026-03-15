#!/usr/bin/env node
/**
 * Browser4 CLI — drive a Browser4 server from the command line.
 *
 * Implements every command described in `browser4-cli-basic.md`.
 *
 * All operations are routed through the Browser4 MCP Server tool interface
 * via `POST /mcp/call-tool`.
 *
 * Element selectors
 *   Browser4 CLI forwards selector-oriented WebDriver commands directly to
 *   the MCP server. Use CSS selectors (or backend node IDs already supported
 *   by the server) for element-targeting commands such as `click`, `type`,
 *   `fill`, `press`, and `upload`.
 *
 * State persistence
 *   The active session ID and server URL are kept in ~/.browser4/cli-state.json
 *   between invocations.
 */

import * as fs from 'fs';
import * as path from 'path';
import axios, {AxiosInstance} from 'axios';
import {clearState, CliState, readState, resolveRef, writeState} from './state';
import {ensureServerRunning} from './cli/daemon/daemon';
import {commands} from './cli/daemon/commands';
import {parseCommand} from './cli/daemon/command';
import {generateHelp, generateHelpJSON} from './cli/daemon/helpGenerator';
import {readManagedServerProcesses, shutdownManagedServerProcesses} from './cli/daemon/managedProcesses';

// ---------------------------------------------------------------------------
// MCP tool call helpers
// ---------------------------------------------------------------------------

function makeAxios(baseUrl: string): AxiosInstance {
    return axios.create({
        baseURL: baseUrl.replace(/\/$/, ''),
        timeout: 30_000,
        headers: {'Content-Type': 'application/json'},
    });
}

const LEGACY_TOOL_NAME_ALIASES: Readonly<Record<string, string>> = {
    browser_navigate: 'navigate',
    browser_snapshot: 'aria_snapshot',
    browser_navigate_back: 'go_back',
    browser_navigate_forward: 'go_forward',
    browser_reload: 'reload',
    browser_press_key: 'press',
    browser_press_sequentially: 'type',
    browser_drag: 'drag',
    browser_type: 'fill',
    browser_hover: 'hover',
    browser_select_option: 'select_option',
    browser_file_upload: 'upload',
    browser_check: 'check',
    browser_uncheck: 'uncheck',
    browser_evaluate: 'evaluate',
    browser_resize: 'resize',
    browser_take_screenshot: 'screenshot',
    browser_keydown: 'keydown',
    browser_keyup: 'keyup',
    browser_mouse_move_xy: 'mousemove',
    browser_mouse_down: 'mousedown',
    browser_mouse_up: 'mouseup',
    browser_mouse_wheel: 'mousewheel',
};

function normalizeToolCall(
    tool: string,
    args: Record<string, unknown> = {},
): { tool: string; args: Record<string, unknown> } {
    const withSelector = (rawArgs: Record<string, unknown>, keys: string[]): Record<string, unknown> => {
        const normalizedArgs = {...rawArgs};
        for (const key of keys) {
            const value = normalizedArgs[key];
            if (typeof value === 'string') {
                normalizedArgs[key] = resolveRef(value);
            }
        }
        return normalizedArgs;
    };

    if (tool === 'browser_click') {
        const {doubleClick, ref, modifiers, ...rest} = args;
        const selectorArgs: Record<string, unknown> = {
            ...rest,
            ...(typeof ref === 'string' ? {selector: resolveRef(ref)} : {}),
            ...(Array.isArray(modifiers) && modifiers.length > 0 ? {modifier: modifiers[0]} : {}),
        };
        return {
            tool: doubleClick ? 'dblclick' : 'click',
            args: selectorArgs,
        };
    }

    if (tool === 'browser_handle_dialog') {
        const {accept, ...rest} = args;
        return {
            tool: accept === false ? 'dialog_dismiss' : 'dialog_accept',
            args: rest,
        };
    }

    if (tool === 'browser_tabs') {
        const {action, ...rest} = args;
        if (action === 'list') return {tool: 'tab_list', args: rest};
        if (action === 'new') return {tool: 'tab_new', args: rest};
        if (action === 'close') return {tool: 'tab_close', args: rest};
        if (action === 'select') return {tool: 'tab_select', args: rest};
    }

    if (tool === 'browser_type') {
        const {ref, ...rest} = args;
        return {
            tool: 'fill',
            args: {
                ...rest,
                ...(typeof ref === 'string' ? {selector: resolveRef(ref)} : {}),
            },
        };
    }

    if (tool === 'browser_hover' || tool === 'browser_select_option' || tool === 'browser_check' || tool === 'browser_uncheck') {
        const {ref, ...rest} = args;
        return {
            tool: LEGACY_TOOL_NAME_ALIASES[tool] ?? tool,
            args: {
                ...rest,
                ...(typeof ref === 'string' ? {selector: resolveRef(ref)} : {}),
            },
        };
    }

    if (tool === 'browser_drag') {
        const {startRef, endRef, ...rest} = args;
        return {
            tool: 'drag',
            args: {
                ...rest,
                ...(typeof startRef === 'string' ? {sourceSelector: resolveRef(startRef)} : {}),
                ...(typeof endRef === 'string' ? {targetSelector: resolveRef(endRef)} : {}),
            },
        };
    }

    if (tool === 'browser_take_screenshot') {
        const {ref, ...rest} = args;
        return {
            tool: 'screenshot',
            args: {
                ...rest,
                ...(typeof ref === 'string' ? {selector: resolveRef(ref)} : {}),
            },
        };
    }

    if (tool === 'browser_file_upload') {
        const rawArgs = withSelector(args, ['selector']);
        return {
            tool: 'upload',
            args: rawArgs,
        };
    }

    return {
        tool: LEGACY_TOOL_NAME_ALIASES[tool] ?? tool,
        args: withSelector(args, ['selector']),
    };
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
    const normalized = normalizeToolCall(tool, args);
    const res = await ax.post('/mcp/call-tool', {tool: normalized.tool, arguments: normalized.args});
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

/**
 * Ensure a directory exists (creates it recursively if needed).
 */
function ensureDir(dir: string): void {
    if (!fs.existsSync(dir)) {
        fs.mkdirSync(dir, {recursive: true});
    }
}

/**
 * Generate a timestamped filename.
 */
function timestampedFilename(prefix: string, ext: string): string {
    const now = new Date().toISOString().replace(/[:.]/g, '-');
    return `${prefix}-${now}.${ext}`;
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
            callTool(ax, 'page_url', {sessionId}),
            callTool(ax, 'page_title', {sessionId}),
            callTool(ax, 'aria_snapshot', {sessionId}),
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
 * Supports: -s=<sessionName>, --server=<url>
 */
interface GlobalFlags {
    sessionName?: string;
    serverUrl?: string;
    args: string[];
}

function parseGlobalFlags(argv: string[]): GlobalFlags {
    const args: string[] = [];
    let sessionName: string | undefined;
    let serverUrl: string | undefined;

    for (let i = 0; i < argv.length; i++) {
        const arg = argv[i];
        if (arg.startsWith('-s=')) {
            sessionName = arg.slice('-s='.length);
        } else if (arg.startsWith('--server=')) {
            serverUrl = arg.slice('--server='.length);
        } else if (arg === '--server' && i + 1 < argv.length && !argv[i + 1].startsWith('-')) {
            serverUrl = argv[++i];
        } else {
            args.push(arg);
        }
    }
    return {sessionName, serverUrl, args};
}

/**
 * Parse raw CLI arguments into a structure compatible with `parseCommand`.
 *
 * Positional arguments go into `_`.
 * `--key=value` is parsed as a named option.
 * `--flag` (no value) is parsed as boolean `true`.
 * Values `"true"` and `"false"` are coerced to booleans.
 */
function parseRawArgs(rawArgs: string[]): Record<string, unknown> & { _: string[] } {
    const result: Record<string, unknown> & { _: string[] } = {_: []};
    for (const arg of rawArgs) {
        if (arg.startsWith('--')) {
            const eqIdx = arg.indexOf('=');
            if (eqIdx !== -1) {
                const key = arg.slice(2, eqIdx);
                const val = arg.slice(eqIdx + 1);
                if (val === 'true') result[key] = true;
                else if (val === 'false') result[key] = false;
                else result[key] = val;
            } else {
                result[arg.slice(2)] = true;
            }
        } else {
            result._.push(arg);
        }
    }
    return result;
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
// Help
// ---------------------------------------------------------------------------

function printHelp(commandName?: string): void {
    if (commandName && commandName !== '--help') {
        const helpData = generateHelpJSON();
        const cmdHelp = helpData.commands[commandName];
        if (cmdHelp) {
            console.log(cmdHelp);
        } else {
            console.error(`Unknown command: ${commandName}`);
            console.log(helpData.global);
        }
    } else {
        console.log(generateHelp());
    }
}

// ---------------------------------------------------------------------------
// Command handlers
// ---------------------------------------------------------------------------

/** Handle the `open` command: create a session and optionally navigate. */
async function handleOpen(
    ax: AxiosInstance,
    baseUrl: string,
    toolParams: Record<string, unknown>,
    sessionName?: string,
): Promise<void> {
    const sessionResult = await callTool(ax, 'open_session', {});
    let sessionId: string;
    try {
        const parsed = JSON.parse(sessionResult);
        sessionId = parsed.sessionId;
    } catch {
        sessionId = sessionResult;
    }

    writeState({sessionId, baseUrl, sessionName});

    if (toolParams.url && toolParams.url !== 'about:blank') {
        const result = await callTool(ax, 'open', {
            ...toolParams,
            sessionId,
        });
        if (result) console.log(result);
    } else {
        console.log(`Session opened: ${sessionId}`);
    }
}

/** Handle the `close` command: close the active session. */
async function handleClose(ax: AxiosInstance): Promise<void> {
    const state = requireSession();
    try {
        await callTool(ax, 'close_session', {sessionId: state.sessionId});
    } catch {
        // Session might already be closed
    }
    clearState();
    console.log('Session closed.');
}

/** Handle the `close-all` command: close all sessions. */
async function handleCloseAll(ax: AxiosInstance): Promise<void> {
    const baseUrls = new Set([
        ax.defaults.baseURL?.replace(/\/$/, '') ?? '',
        ...readManagedServerProcesses().map(processInfo => processInfo.baseUrl.replace(/\/$/, '')),
    ]);
    const closeResults: string[] = [];
    const closeErrors: string[] = [];

    for (const baseUrl of baseUrls) {
        if (!baseUrl) {
            continue;
        }

        try {
            const result = await callTool(makeAxios(baseUrl), 'close_all_sessions', {});
            closeResults.push(baseUrl === ax.defaults.baseURL?.replace(/\/$/, '')
                ? result
                : `${baseUrl}: ${result}`);
        } catch (error) {
            const message = error instanceof Error ? error.message : String(error);
            closeErrors.push(`${baseUrl}: ${message}`);
        }
    }

    const shutdownResult = await shutdownManagedServerProcesses({force: false});
    clearState();
    if (closeResults.length > 0) {
        for (const result of closeResults) {
            console.log(result);
        }
    } else {
        console.log('No reachable Browser4 servers responded to close-all.');
    }
    logShutdownResult('Stopped', shutdownResult);
    if (closeErrors.length > 0) {
        console.error(`close-all warnings: ${closeErrors.join(' | ')}`);
    }
}

/** Handle the `kill-all` command: forcefully kill all sessions. */
async function handleKillAll(ax: AxiosInstance): Promise<void> {
    const shutdownResult = await shutdownManagedServerProcesses({force: true});
    clearState();
    logShutdownResult('Killed', shutdownResult);
}

function logShutdownResult(
    action: 'Stopped' | 'Killed',
    shutdownResult: Awaited<ReturnType<typeof shutdownManagedServerProcesses>>,
): void {
    if (shutdownResult.stoppedPids.length > 0) {
        console.log(`${action} Browser4 process(es): ${shutdownResult.stoppedPids.join(', ')}`);
    } else if (shutdownResult.missingPids.length === 0) {
        console.log('No tracked Browser4 processes found.');
    }

    if (shutdownResult.missingPids.length > 0) {
        console.log(`Already stopped Browser4 process(es): ${shutdownResult.missingPids.join(', ')}`);
    }

    if (shutdownResult.forcedPids.length > 0 && action === 'Stopped') {
        console.log(`Forced Browser4 process(es) after graceful timeout: ${shutdownResult.forcedPids.join(', ')}`);
    }

    if (shutdownResult.remainingPids.length > 0) {
        console.error(`Browser4 process(es) still running after ${action.toLowerCase()}: ${shutdownResult.remainingPids.join(', ')}`);
    }
}

function shouldEnsureServerRunning(command?: string): boolean {
    return command !== 'close-all' && command !== 'kill-all';
}

/** Handle the `list` command: list all active sessions. */
async function handleList(ax: AxiosInstance): Promise<void> {
    const result = await callTool(ax, 'list_sessions', {});
    console.log(result);
}

/** Handle the `delete-data` command: delete session data. */
async function handleDeleteData(ax: AxiosInstance): Promise<void> {
    const state = requireSession();
    const result = await callTool(ax, 'delete_session_data', {sessionId: state.sessionId});
    console.log(result || 'Session data deleted.');
}

/**
 * Handle the `snapshot` command: capture page snapshot and save to file.
 * Produces the same output format as `postCommandSnapshot`.
 */
async function handleSnapshot(
    ax: AxiosInstance,
    toolParams: Record<string, unknown>,
): Promise<void> {
    const state = requireSession();
    const sid = state.sessionId;
    const [pageUrl, pageTitle, snapshotContent] = await Promise.all([
        callTool(ax, 'page_url', {sessionId: sid}),
        callTool(ax, 'page_title', {sessionId: sid}),
        callTool(ax, 'aria_snapshot', {sessionId: sid}),
    ]);

    const outName = (toolParams.filename as string) || timestampedFilename('snapshot', 'yml');
    const outPath = path.resolve(SNAPSHOT_DIR, outName);
    ensureDir(path.dirname(outPath));
    fs.writeFileSync(outPath, snapshotContent, 'utf-8');

    console.log('### Page');
    console.log(`- Page URL: ${pageUrl}`);
    console.log(`- Page Title: ${pageTitle}`);
    console.log('### Snapshot');
    console.log(`[Snapshot](${outPath})`);
}

async function handleScreenshot(
    ax: AxiosInstance,
    toolParams: Record<string, unknown>,
): Promise<void> {
    const state = requireSession();
    const sid = state.sessionId;
    const {filename, ...captureArgs} = toolParams;
    const base64 = await callTool(ax, 'screenshot', {
        ...captureArgs,
        sessionId: sid,
    });

    const outName = (filename as string) || timestampedFilename('screenshot', 'png');
    const outPath = path.resolve(SNAPSHOT_DIR, outName);
    ensureDir(path.dirname(outPath));
    fs.writeFileSync(outPath, Buffer.from(base64, 'base64'));
    console.log(`[Screenshot](${outPath})`);
}

/**
 * Generic command handler: call an MCP tool with the session ID and print the result.
 */
async function handleToolCommand(
    ax: AxiosInstance,
    toolName: string,
    toolParams: Record<string, unknown>,
): Promise<void> {
    const state = requireSession();
    const result = await callTool(ax, toolName, {
        ...toolParams,
        sessionId: state.sessionId,
    });
    if (result) {
        console.log(result);
    }
}

// ---------------------------------------------------------------------------
// Entry point
// ---------------------------------------------------------------------------

async function main(): Promise<void> {
    const rawArgs = process.argv.slice(2);
    const {sessionName, serverUrl, args: remaining} = parseGlobalFlags(rawArgs);
    const [command] = remaining;

    try {
        // Handle help or no command
        if (!command || command === 'help' || command === '--help' || command === '-h') {
            printHelp(remaining[1]);
            return;
        }

        console.log("Starting command...");

        // Resolve base URL: --server flag > persisted state > default
        const currentState = readState();
        const baseUrl = serverUrl || currentState.baseUrl;
        if (serverUrl && serverUrl !== currentState.baseUrl) {
            writeState({...currentState, baseUrl: serverUrl});
        }

        // Ensure the Browser4 server is running
        if (shouldEnsureServerRunning(command)) {
            await ensureServerRunning(remaining.slice(1));
        }

        const ax = makeAxios(baseUrl);

        // Look up the command definition
        const cmdDef = commands[command];
        if (!cmdDef) {
            console.error(`Unknown command: ${command}. Run 'browser4-cli help' for usage.`);
            process.exit(1);
        }

        // Parse positional + named arguments using the command schema
        const parsed = parseRawArgs(remaining);
        // Type assertion: parseCommand uses Zod for runtime validation which
        // handles booleans/numbers even though the signature says string values.
        const {toolName, toolParams} = parseCommand(
            cmdDef,
            parsed as unknown as Record<string, string> & { _: string[] },
        );

        // Dispatch the command
        switch (command) {
            case 'open':
                await handleOpen(ax, baseUrl, toolParams, sessionName);
                break;
            case 'close':
                await handleClose(ax);
                break;
            case 'close-all':
                await handleCloseAll(ax);
                break;
            case 'kill-all':
                await handleKillAll(ax);
                break;
            case 'list':
                await handleList(ax);
                break;
            case 'delete-data':
                await handleDeleteData(ax);
                break;
            case 'snapshot':
                await handleSnapshot(ax, toolParams);
                break;
            case 'screenshot':
                await handleScreenshot(ax, toolParams);
                break;
            default:
                if (!toolName) {
                    console.log(`Command '${command}' is not yet implemented.`);
                    break;
                }
                await handleToolCommand(ax, toolName, toolParams);
                break;
        }

        // Post-command snapshot for commands that modify browser state
        if (!NO_SNAPSHOT_COMMANDS.has(command)) {
            const state = readState();
            if (state.sessionId) {
                await postCommandSnapshot(ax, state.sessionId);
            }
        }
    } catch (err) {
        console.error(".................");

        const msg = err instanceof Error ? err.message : String(err);
        console.error(`Error: ${msg}`);
        process.exit(1);
    }
}

// Only run when invoked directly (not when imported for testing)
if (require.main === module) {
    main().catch((err) => {
        console.error(err instanceof Error ? err.message : String(err));
        process.exit(1);
    });
}

export {parseRawArgs, parseGlobalFlags, shouldEnsureServerRunning, normalizeToolCall};
