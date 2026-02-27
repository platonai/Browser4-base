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
  /** Named session label for the `-s=<name>` flag. */
  sessionName?: string;
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
