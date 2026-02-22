/**
 * Persistent state management for the Browser4 CLI.
 *
 * State is stored in ~/.browser4/cli-state.json and shared across
 * all browser4-cli invocations in the same terminal session.
 */

import * as fs from 'fs';
import * as path from 'path';
import * as os from 'os';

export interface CliState {
  /** Active session ID returned by the server on `open`. */
  sessionId?: string;
  /** Base URL of the Browser4 REST server. */
  baseUrl: string;
  /**
   * CSS selector (or `backend:<id>` ref) of the last element that was
   * clicked.  Used as the default target for subsequent `type` / `press`
   * commands.
   */
  activeSelector?: string;
}

/** Default directory for persisted state. Can be overridden for testing. */
export const DEFAULT_STATE_DIR = path.join(os.homedir(), '.browser4');

const DEFAULT_STATE: CliState = {
  baseUrl: 'http://localhost:8182',
};

function stateFile(stateDir: string): string {
  return path.join(stateDir, 'cli-state.json');
}

/** Read the persisted CLI state from disk, falling back to defaults. */
export function readState(stateDir: string = DEFAULT_STATE_DIR): CliState {
  try {
    const raw = fs.readFileSync(stateFile(stateDir), 'utf-8');
    const parsed = JSON.parse(raw) as Partial<CliState>;
    return { ...DEFAULT_STATE, ...parsed };
  } catch {
    return { ...DEFAULT_STATE };
  }
}

/** Write the CLI state to disk, creating the directory if necessary. */
export function writeState(state: CliState, stateDir: string = DEFAULT_STATE_DIR): void {
  if (!fs.existsSync(stateDir)) {
    fs.mkdirSync(stateDir, { recursive: true });
  }
  fs.writeFileSync(stateFile(stateDir), JSON.stringify(state, null, 2), 'utf-8');
}

/** Clear all persisted CLI state (called on `close`). */
export function clearState(stateDir: string = DEFAULT_STATE_DIR): void {
  try {
    fs.unlinkSync(stateFile(stateDir));
  } catch {
    // Nothing to clear.
  }
}

/**
 * Convert a user-facing element reference to a server selector string.
 *
 * The CLI accepts the compact notation `e<N>` (e.g. `e15`) that the
 * accessibility snapshot uses to label nodes.  These map directly to
 * browser backend node IDs and must be sent to the server as
 * `backend:<N>` (e.g. `backend:15`).
 *
 * Any other string is returned unchanged so that callers can also pass
 * plain CSS selectors, `backend:N` refs, or locator strings directly.
 *
 * @param ref - The element reference from the user (e.g. `e15`).
 * @returns The selector string understood by the Browser4 server.
 */
export function resolveRef(ref: string): string {
  const m = /^e(\d+)$/.exec(ref);
  if (m) {
    return `backend:${m[1]}`;
  }
  return ref;
}
