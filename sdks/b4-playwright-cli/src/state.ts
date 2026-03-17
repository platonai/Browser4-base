/**
 * Persistent state management for the Browser4 CLI.
 *
 * State is stored in ~/.browser4/cli-state.json and shared across
 * all b4-playwright-cli invocations in the same terminal session.
 */

import * as fs from 'fs';
import * as path from 'path';
import * as os from 'os';

export interface CliState {
  /** Active session ID returned by the server on `open`. */
  sessionId?: string;
  /** Base URL of the Browser4 REST server. */
  baseUrl: string;
  /** Reserved selector slot for future CLI workflows. */
  activeSelector?: string;
  /** Named session label for the `-s=<name>` flag. */
  sessionName?: string;
}

function resolveDefaultStateDir(): string {
  const override = process.env.BROWSER4_CLI_STATE_DIR?.trim();
  if (override) {
    return path.resolve(override);
  }
  return path.join(os.homedir(), '.browser4');
}

/** Default directory for persisted state. Can be overridden for testing. */
export const DEFAULT_STATE_DIR = resolveDefaultStateDir();

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
 * Convert a CLI element ref into the selector format expected by Browser4.
 *
 * Supported forms:
 * - `e15` -> `backend:15`
 * - `backend:15` -> `backend:15`
 * - CSS/XPath selectors are passed through unchanged
 */
export function resolveRef(rawRef: string): string {
  const trimmed = rawRef.trim();
  const match = /^e(\d+)$/i.exec(trimmed);
  if (match) {
    return `backend:${match[1]}`;
  }

  return trimmed;
}
