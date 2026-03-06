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
import axios, {AxiosInstance} from 'axios';
import {clearState, CliState, readState, writeState} from './state';
import {ensureServerRunning} from "./cli/daemon/daemon";

// ---------------------------------------------------------------------------
// Server Management
// ---------------------------------------------------------------------------

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
    const res = await ax.post('/mcp/call-tool', {tool, arguments: args});
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
    return {filename, rest};
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
    return {sessionName, args};
}

// ---------------------------------------------------------------------------
// Command implementations
// ---------------------------------------------------------------------------

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
    const {sessionName, args: remaining} = parseGlobalFlags(rawArgs);
    const [command, ...rest] = remaining;

    try {
        if (command !== 'help' && command !== '--help' && command !== '-h' && command !== undefined) {
            // Pass remaining args to check for --server flag
            await ensureServerRunning(rest);
        }

        // Use files in cli/daemon to implement the client commands, which will call MCP tools and manage state as needed.
        // For example, "open" will start a new session and save the session ID to state; "click" will read the session ID from state and call the click tool; etc.
        // Each command implementation should be in its own function for clarity.

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

main().then(r => "");
