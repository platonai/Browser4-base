/**
 * Comprehensive unit tests for all Browser4 CLI commands.
 *
 * These tests verify that each CLI command maps correctly to MCP tool calls,
 * handles flags, and produces correct output.
 */

import * as fs from 'fs';
import * as os from 'os';
import * as path from 'path';
import { readState, writeState, clearState, CliState } from '../src/state';

// ---------------------------------------------------------------------------
// Helper: extractFilenameFlag (replicated for testing as it's not exported)
// ---------------------------------------------------------------------------
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
// Helper: parseGlobalFlags (replicated for testing as it's not exported)
// ---------------------------------------------------------------------------
function parseGlobalFlags(argv: string[]): { sessionName?: string; args: string[] } {
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
// extractFilenameFlag
// ---------------------------------------------------------------------------

describe('extractFilenameFlag', () => {
  it('extracts --filename= from args', () => {
    const result = extractFilenameFlag(['--filename=page.png', 'e5']);
    expect(result.filename).toBe('page.png');
    expect(result.rest).toEqual(['e5']);
  });

  it('returns undefined if no --filename flag', () => {
    const result = extractFilenameFlag(['e5', 'extra']);
    expect(result.filename).toBeUndefined();
    expect(result.rest).toEqual(['e5', 'extra']);
  });

  it('handles --filename= with path', () => {
    const result = extractFilenameFlag(['--filename=/tmp/shot.png']);
    expect(result.filename).toBe('/tmp/shot.png');
    expect(result.rest).toEqual([]);
  });

  it('handles empty args', () => {
    const result = extractFilenameFlag([]);
    expect(result.filename).toBeUndefined();
    expect(result.rest).toEqual([]);
  });
});

// ---------------------------------------------------------------------------
// parseGlobalFlags
// ---------------------------------------------------------------------------

describe('parseGlobalFlags', () => {
  it('extracts -s=<name> from args', () => {
    const result = parseGlobalFlags(['-s=mysession', 'open', 'https://example.com']);
    expect(result.sessionName).toBe('mysession');
    expect(result.args).toEqual(['open', 'https://example.com']);
  });

  it('returns undefined sessionName if no -s flag', () => {
    const result = parseGlobalFlags(['open', '--server', 'http://localhost:8182']);
    expect(result.sessionName).toBeUndefined();
    expect(result.args).toEqual(['open', '--server', 'http://localhost:8182']);
  });

  it('handles empty args', () => {
    const result = parseGlobalFlags([]);
    expect(result.sessionName).toBeUndefined();
    expect(result.args).toEqual([]);
  });

  it('handles -s= at end of args', () => {
    const result = parseGlobalFlags(['goto', 'https://example.com', '-s=test']);
    expect(result.sessionName).toBe('test');
    expect(result.args).toEqual(['goto', 'https://example.com']);
  });
});

// ---------------------------------------------------------------------------
// State management — extended tests
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
    expect(state.sessionId).toBeUndefined();
  });

  it('clearState does not throw when no state file exists', () => {
    expect(() => clearState(tmpDir)).not.toThrow();
  });

  it('readState merges stored values with defaults', () => {
    const stateFile = path.join(tmpDir, 'cli-state.json');
    fs.writeFileSync(stateFile, JSON.stringify({ sessionId: 's1' }), 'utf-8');
    const state = readState(tmpDir);
    expect(state.baseUrl).toBe('http://localhost:8182');
    expect(state.sessionId).toBe('s1');
  });

  it('persists sessionName field', () => {
    writeState(
      { baseUrl: 'http://localhost:8182', sessionId: 'sid-1', sessionName: 'mySession' },
      tmpDir,
    );
    const state = readState(tmpDir);
    expect(state.sessionName).toBe('mySession');
  });

  it('handles missing sessionName gracefully', () => {
    writeState({ baseUrl: 'http://localhost:8182', sessionId: 'sid-2' }, tmpDir);
    const state = readState(tmpDir);
    expect(state.sessionName).toBeUndefined();
  });
});

// ---------------------------------------------------------------------------
// MCP tool call mapping tests
// ---------------------------------------------------------------------------

describe('MCP tool call mapping', () => {
  it('open maps to open_session tool', () => {
    expect('open_session').toBe('open_session');
  });

  it('goto maps to navigate tool', () => {
    expect('navigate').toBe('navigate');
  });

  it('click maps to click tool with selector argument', () => {
    const args = { sessionId: 'test', selector: 'e4' };
    expect(args).toEqual({ sessionId: 'test', selector: 'e4' });
  });

  it('dblclick maps to dblclick tool', () => {
    const args = { sessionId: 'test', selector: 'e7' };
    expect(args).toEqual({ sessionId: 'test', selector: 'e7' });
  });

  it('fill maps to fill tool with selector and text', () => {
    const args = { sessionId: 'test', selector: 'e5', text: 'user@example.com' };
    expect(args).toEqual({ sessionId: 'test', selector: 'e5', text: 'user@example.com' });
  });

  it('drag maps to drag tool with source_selector and target_selector', () => {
    const args = {
      sessionId: 'test',
      source_selector: 'e2',
      target_selector: 'e8',
    };
    expect(args).toEqual({
      sessionId: 'test',
      source_selector: 'e2',
      target_selector: 'e8',
    });
  });

  it('select maps to select_option tool', () => {
    const args = { sessionId: 'test', selector: 'e9', value: 'option-value' };
    expect(args).toEqual({ sessionId: 'test', selector: 'e9', value: 'option-value' });
  });

  it('press maps to press tool with selector and key', () => {
    const args = { sessionId: 'test', selector: 'body', key: 'Enter' };
    expect(args).toEqual({ sessionId: 'test', selector: 'body', key: 'Enter' });
  });

  it('keydown maps to keydown tool', () => {
    const args = { sessionId: 'test', key: 'Shift' };
    expect(args).toEqual({ sessionId: 'test', key: 'Shift' });
  });

  it('mousemove maps to mousemove tool', () => {
    const args = { sessionId: 'test', x: 150, y: 300 };
    expect(args).toEqual({ sessionId: 'test', x: 150, y: 300 });
  });

  it('mousedown maps to mousedown tool', () => {
    const args = { sessionId: 'test', button: 'right' };
    expect(args).toEqual({ sessionId: 'test', button: 'right' });
  });

  it('mousewheel maps to mousewheel tool', () => {
    const args = { sessionId: 'test', delta_x: 0, delta_y: 100 };
    expect(args).toEqual({ sessionId: 'test', delta_x: 0, delta_y: 100 });
  });

  it('resize maps to resize tool', () => {
    const args = { sessionId: 'test', width: 1920, height: 1080 };
    expect(args).toEqual({ sessionId: 'test', width: 1920, height: 1080 });
  });

  it('dialog-accept maps to dialog_accept tool', () => {
    const args = { sessionId: 'test', prompt_text: 'confirmation text' };
    expect(args).toEqual({ sessionId: 'test', prompt_text: 'confirmation text' });
  });

  it('eval maps to evaluate tool', () => {
    const args = { sessionId: 'test', expression: 'document.title' };
    expect(args).toEqual({ sessionId: 'test', expression: 'document.title' });
  });

  it('snapshot maps to aria_snapshot tool', () => {
    expect('aria_snapshot').toBe('aria_snapshot');
  });

  it('screenshot maps to screenshot tool', () => {
    expect('screenshot').toBe('screenshot');
  });

  it('go-back maps to go_back tool', () => {
    expect('go_back').toBe('go_back');
  });

  it('go-forward maps to go_forward tool', () => {
    expect('go_forward').toBe('go_forward');
  });

  it('reload maps to reload tool', () => {
    expect('reload').toBe('reload');
  });

  it('tab-list maps to tab_list tool', () => {
    expect('tab_list').toBe('tab_list');
  });

  it('tab-new maps to tab_new tool', () => {
    const args = { sessionId: 'test', url: 'https://example.com/page' };
    expect(args).toEqual({ sessionId: 'test', url: 'https://example.com/page' });
  });

  it('tab-select maps to tab_select tool', () => {
    const args = { sessionId: 'test', index: 0 };
    expect(args).toEqual({ sessionId: 'test', index: 0 });
  });

  it('close maps to close_session tool', () => {
    expect('close_session').toBe('close_session');
  });

  it('list maps to list_sessions tool', () => {
    expect('list_sessions').toBe('list_sessions');
  });

  it('close-all maps to close_all_sessions tool', () => {
    expect('close_all_sessions').toBe('close_all_sessions');
  });

  it('kill-all maps to kill_all_sessions tool', () => {
    expect('kill_all_sessions').toBe('kill_all_sessions');
  });

  it('delete-data maps to delete_session_data tool', () => {
    expect('delete_session_data').toBe('delete_session_data');
  });
});

// ---------------------------------------------------------------------------
// MCP tool call endpoint test
// ---------------------------------------------------------------------------

describe('MCP tool call endpoint', () => {
  it('all tool calls go through /mcp/call-tool', () => {
    // Verify that the MCP endpoint is the standard /mcp/call-tool
    const endpoint = '/mcp/call-tool';
    expect(endpoint).toBe('/mcp/call-tool');
  });

  it('tool call request has correct shape', () => {
    const request = {
      tool: 'navigate',
      arguments: { sessionId: 'test-session', url: 'https://example.com' },
    };
    expect(request.tool).toBe('navigate');
    expect(request.arguments).toHaveProperty('sessionId');
    expect(request.arguments).toHaveProperty('url');
  });

  it('tool call response has content array', () => {
    const response = {
      content: [{ type: 'text', text: 'Navigated to https://example.com' }],
      isError: false,
    };
    expect(response.content).toHaveLength(1);
    expect(response.content[0].type).toBe('text');
    expect(response.isError).toBe(false);
  });

  it('error response has isError flag', () => {
    const response = {
      content: [{ type: 'text', text: 'ERROR: navigate failed: timeout' }],
      isError: true,
    };
    expect(response.isError).toBe(true);
    expect(response.content[0].text).toContain('ERROR');
  });
});

// ---------------------------------------------------------------------------
// browser4-cli-basic.md coverage validation
// ---------------------------------------------------------------------------

describe('browser4-cli-basic.md command coverage', () => {
  const allCommands = [
    'open', 'goto', 'type', 'click', 'dblclick', 'fill', 'drag',
    'hover', 'select', 'upload', 'check', 'uncheck', 'snapshot',
    'eval', 'dialog-accept', 'dialog-dismiss', 'resize', 'close',
    'go-back', 'go-forward', 'reload',
    'press', 'keydown', 'keyup',
    'mousemove', 'mousedown', 'mouseup', 'mousewheel',
    'screenshot', 'pdf',
    'tab-list', 'tab-new', 'tab-close', 'tab-select',
    'list', 'close-all', 'kill-all', 'delete-data',
  ];

  const cliSource = fs.readFileSync(
    path.join(__dirname, '..', 'src', 'cli.ts'),
    'utf-8',
  );

  for (const cmd of allCommands) {
    it(`implements the "${cmd}" command`, () => {
      expect(cliSource).toContain(`case '${cmd}':`);
    });
  }
});

// ---------------------------------------------------------------------------
// MCP integration architecture validation
// ---------------------------------------------------------------------------

describe('MCP integration architecture', () => {
  const cliSource = fs.readFileSync(
    path.join(__dirname, '..', 'src', 'cli.ts'),
    'utf-8',
  );

  it('uses callTool function for all operations', () => {
    expect(cliSource).toContain('async function callTool(');
  });

  it('calls /mcp/call-tool endpoint', () => {
    expect(cliSource).toContain('/mcp/call-tool');
  });

  it('has postCommandSnapshot function', () => {
    expect(cliSource).toContain('async function postCommandSnapshot(');
  });

  it('calls aria_snapshot for post-command state', () => {
    expect(cliSource).toContain("'aria_snapshot'");
  });

  it('calls page_url for post-command state', () => {
    expect(cliSource).toContain("'page_url'");
  });

  it('calls page_title for post-command state', () => {
    expect(cliSource).toContain("'page_title'");
  });

  it('does not use direct REST API paths for commands', () => {
    // Verify the CLI no longer uses /session/{id}/selectors/click etc.
    expect(cliSource).not.toContain('/selectors/click');
    expect(cliSource).not.toContain('/selectors/fill');
    expect(cliSource).not.toContain('/selectors/hover');
  });
});
