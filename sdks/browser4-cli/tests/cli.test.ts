/**
 * Unit tests for Browser4 CLI helper logic.
 *
 * These tests cover the ref-resolution and state-management utilities without
 * requiring a running Browser4 server.
 */

import * as fs from 'fs';
import * as os from 'os';
import * as path from 'path';

import { resolveRef, readState, writeState, clearState } from '../src/state';

// ---------------------------------------------------------------------------
// resolveRef
// ---------------------------------------------------------------------------

describe('resolveRef', () => {
  it('converts e<N> refs to backend:<N>', () => {
    expect(resolveRef('e15')).toBe('backend:15');
    expect(resolveRef('e0')).toBe('backend:0');
    expect(resolveRef('e999')).toBe('backend:999');
  });

  it('passes plain CSS selectors through unchanged', () => {
    expect(resolveRef('.my-class')).toBe('.my-class');
    expect(resolveRef('#my-id')).toBe('#my-id');
    expect(resolveRef('button[type=submit]')).toBe('button[type=submit]');
  });

  it('passes already-resolved backend: refs through unchanged', () => {
    expect(resolveRef('backend:15')).toBe('backend:15');
  });

  it('does not convert strings that only partially match the pattern', () => {
    // "e15x" has trailing chars — should not be converted
    expect(resolveRef('e15x')).toBe('e15x');
    // "e" with no digits
    expect(resolveRef('e')).toBe('e');
  });
});

// ---------------------------------------------------------------------------
// State management — uses a real temp directory to avoid fs-mock issues.
// ---------------------------------------------------------------------------

describe('state management', () => {
  let tmpDir: string;

  beforeEach(() => {
    tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'browser4-cli-test-'));
  });

  afterEach(() => {
    try { fs.rmSync(tmpDir, { recursive: true }); } catch { /* ignore */ }
  });

  it('readState returns defaults when no state file exists', () => {
    const state = readState(tmpDir);
    expect(state.baseUrl).toBe('http://localhost:8182');
    expect(state.sessionId).toBeUndefined();
    expect(state.activeSelector).toBeUndefined();
  });

  it('writeState persists state and readState retrieves it', () => {
    writeState(
      { baseUrl: 'http://localhost:8182', sessionId: 'abc-123', activeSelector: 'backend:15' },
      tmpDir,
    );
    const state = readState(tmpDir);
    expect(state.sessionId).toBe('abc-123');
    expect(state.activeSelector).toBe('backend:15');
    expect(state.baseUrl).toBe('http://localhost:8182');
  });

  it('writeState creates the state directory if it does not exist', () => {
    const nested = path.join(tmpDir, 'deep', 'nested');
    writeState({ baseUrl: 'http://localhost:8182', sessionId: 'test-id' }, nested);
    expect(fs.existsSync(nested)).toBe(true);
    const state = readState(nested);
    expect(state.sessionId).toBe('test-id');
  });

  it('clearState removes the state file', () => {
    writeState({ baseUrl: 'http://localhost:8182', sessionId: 'xyz' }, tmpDir);
    clearState(tmpDir);
    const state = readState(tmpDir);
    // After clearing, state should revert to defaults (no sessionId).
    expect(state.sessionId).toBeUndefined();
  });

  it('clearState does not throw when no state file exists', () => {
    expect(() => clearState(tmpDir)).not.toThrow();
  });

  it('readState merges stored values with defaults', () => {
    // Write only a sessionId (no baseUrl).
    const stateFile = path.join(tmpDir, 'cli-state.json');
    fs.writeFileSync(stateFile, JSON.stringify({ sessionId: 's1' }), 'utf-8');
    const state = readState(tmpDir);
    // baseUrl should fall back to the default.
    expect(state.baseUrl).toBe('http://localhost:8182');
    expect(state.sessionId).toBe('s1');
  });
});

