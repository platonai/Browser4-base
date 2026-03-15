/**
 * Tests for browser4-cli program.ts — argument parsing and command dispatch.
 */

// Mock axios and state before importing program
jest.mock('axios', () => ({
    __esModule: true,
    default: {
        create: jest.fn(() => ({
            post: jest.fn(),
            get: jest.fn(),
        })),
    },
}));

jest.mock('../src/state', () => ({
    readState: jest.fn(() => ({baseUrl: 'http://localhost:8182'})),
    writeState: jest.fn(),
    clearState: jest.fn(),
    resolveRef: jest.fn((ref: string) => /^e\d+$/i.test(ref) ? `backend:${ref.slice(1)}` : ref),
}));

jest.mock('../src/cli/daemon/daemon', () => ({
    ensureServerRunning: jest.fn(),
}));

import {normalizeToolCall, parseRawArgs, parseGlobalFlags} from '../src/program';
import {shouldEnsureServerRunning} from '../src/program';

describe('parseRawArgs', () => {
    it('should parse positional arguments', () => {
        const result = parseRawArgs(['goto', 'https://example.com']);
        expect(result._).toEqual(['goto', 'https://example.com']);
    });

    it('should parse --key=value options', () => {
        const result = parseRawArgs(['screenshot', '--filename=page.png']);
        expect(result._).toEqual(['screenshot']);
        expect(result.filename).toBe('page.png');
    });

    it('should parse --flag as boolean true', () => {
        const result = parseRawArgs(['type', 'hello', '--submit']);
        expect(result._).toEqual(['type', 'hello']);
        expect(result.submit).toBe(true);
    });

    it('should parse --flag=true as boolean true', () => {
        const result = parseRawArgs(['fill', 'e5', 'text', '--submit=true']);
        expect(result._).toEqual(['fill', 'e5', 'text']);
        expect(result.submit).toBe(true);
    });

    it('should parse --flag=false as boolean false', () => {
        const result = parseRawArgs(['fill', 'e5', 'text', '--submit=false']);
        expect(result._).toEqual(['fill', 'e5', 'text']);
        expect(result.submit).toBe(false);
    });

    it('should handle multiple options', () => {
        const result = parseRawArgs(['screenshot', '--filename=page.png', '--full-page']);
        expect(result._).toEqual(['screenshot']);
        expect(result.filename).toBe('page.png');
        expect(result['full-page']).toBe(true);
    });

    it('should handle command with no args', () => {
        const result = parseRawArgs(['close']);
        expect(result._).toEqual(['close']);
        expect(Object.keys(result).filter(k => k !== '_')).toHaveLength(0);
    });

    it('should handle empty args', () => {
        const result = parseRawArgs([]);
        expect(result._).toEqual([]);
    });

    it('should handle numeric-looking values as strings', () => {
        const result = parseRawArgs(['resize', '1920', '1080']);
        expect(result._).toEqual(['resize', '1920', '1080']);
    });
});

describe('parseGlobalFlags', () => {
    it('should extract -s=<name> session flag', () => {
        const result = parseGlobalFlags(['-s=mySession', 'goto', 'https://example.com']);
        expect(result.sessionName).toBe('mySession');
        expect(result.args).toEqual(['goto', 'https://example.com']);
    });

    it('should extract --server=<url> flag', () => {
        const result = parseGlobalFlags(['--server=http://remote:8182', 'goto', 'https://example.com']);
        expect(result.serverUrl).toBe('http://remote:8182');
        expect(result.args).toEqual(['goto', 'https://example.com']);
    });

    it('should extract --server <url> flag (space separated)', () => {
        const result = parseGlobalFlags(['--server', 'http://remote:8182', 'goto', 'https://example.com']);
        expect(result.serverUrl).toBe('http://remote:8182');
        expect(result.args).toEqual(['goto', 'https://example.com']);
    });

    it('should not treat --server followed by a flag as server URL', () => {
        const result = parseGlobalFlags(['--server', '--other', 'goto']);
        expect(result.serverUrl).toBeUndefined();
        expect(result.args).toEqual(['--server', '--other', 'goto']);
    });

    it('should pass through non-global flags', () => {
        const result = parseGlobalFlags(['goto', 'https://example.com', '--filename=test.yml']);
        expect(result.sessionName).toBeUndefined();
        expect(result.serverUrl).toBeUndefined();
        expect(result.args).toEqual(['goto', 'https://example.com', '--filename=test.yml']);
    });

    it('should handle both session and server flags', () => {
        const result = parseGlobalFlags(['-s=sess1', '--server=http://host:8182', 'open']);
        expect(result.sessionName).toBe('sess1');
        expect(result.serverUrl).toBe('http://host:8182');
        expect(result.args).toEqual(['open']);
    });

    it('should handle empty args', () => {
        const result = parseGlobalFlags([]);
        expect(result.sessionName).toBeUndefined();
        expect(result.serverUrl).toBeUndefined();
        expect(result.args).toEqual([]);
    });
});

describe('shouldEnsureServerRunning', () => {
    it('skips auto-start for close-all', () => {
        expect(shouldEnsureServerRunning('close-all')).toBe(false);
    });

    it('skips auto-start for kill-all', () => {
        expect(shouldEnsureServerRunning('kill-all')).toBe(false);
    });

    it('keeps auto-start for normal commands', () => {
        expect(shouldEnsureServerRunning('open')).toBe(true);
    });
});

describe('normalizeToolCall', () => {
    it('maps legacy navigate tool names to backend MCP names', () => {
        expect(normalizeToolCall('browser_navigate', {url: 'https://example.com'})).toEqual({
            tool: 'navigate',
            args: {url: 'https://example.com'},
        });
    });

    it('maps legacy click variants to click and dblclick', () => {
        expect(normalizeToolCall('browser_click', {ref: 'e1'})).toEqual({
            tool: 'click',
            args: {selector: 'backend:1'},
        });
        expect(normalizeToolCall('browser_click', {ref: 'e1', doubleClick: true})).toEqual({
            tool: 'dblclick',
            args: {selector: 'backend:1'},
        });
    });

    it('maps legacy tab action tool names to tab MCP names', () => {
        expect(normalizeToolCall('browser_tabs', {action: 'select', index: 2})).toEqual({
            tool: 'tab_select',
            args: {index: 2},
        });
    });

    it('maps legacy dialog tool names to dialog MCP names', () => {
        expect(normalizeToolCall('browser_handle_dialog', {accept: true, promptText: 'ok'})).toEqual({
            tool: 'dialog_accept',
            args: {promptText: 'ok'},
        });
        expect(normalizeToolCall('browser_handle_dialog', {accept: false})).toEqual({
            tool: 'dialog_dismiss',
            args: {},
        });
    });

    it('maps legacy selector-style command args to selector fields', () => {
        expect(normalizeToolCall('browser_type', {ref: 'e2271', text: 'hello'})).toEqual({
            tool: 'fill',
            args: {selector: 'backend:2271', text: 'hello'},
        });
        expect(normalizeToolCall('browser_hover', {ref: '.cta'})).toEqual({
            tool: 'hover',
            args: {selector: '.cta'},
        });
        expect(normalizeToolCall('browser_drag', {startRef: 'e1', endRef: 'e2'})).toEqual({
            tool: 'drag',
            args: {sourceSelector: 'backend:1', targetSelector: 'backend:2'},
        });
    });
});
