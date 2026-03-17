/**
 * Tests for b4-playwright-cli program.ts — argument parsing and command dispatch.
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

import axios from 'axios';
import {main, normalizeToolCall, parseRawArgs, parseGlobalFlags, isStaleSessionErrorMessage} from '../src/program';
import {shouldEnsureServerRunning} from '../src/program';

const stateModule = jest.requireMock('../src/state') as {
    readState: jest.Mock;
    writeState: jest.Mock;
    clearState: jest.Mock;
};

const daemonModule = jest.requireMock('../src/cli/daemon/daemon') as {
    ensureServerRunning: jest.Mock;
};

const mockedAxios = axios as unknown as {
    create: jest.Mock;
};

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
    it('keeps frontend navigate tool names unchanged', () => {
        expect(normalizeToolCall('browser_navigate', {url: 'https://example.com'})).toEqual({
            tool: 'browser_navigate',
            args: {url: 'https://example.com'},
        });
    });

    it('keeps frontend click variants and resolves element refs', () => {
        expect(normalizeToolCall('browser_click', {ref: 'e1'})).toEqual({
            tool: 'browser_click',
            args: {ref: 'backend:1'},
        });
        expect(normalizeToolCall('browser_click', {ref: 'e1', doubleClick: true})).toEqual({
            tool: 'browser_click',
            args: {ref: 'backend:1', doubleClick: true},
        });
    });

    it('keeps frontend tab action tool names unchanged', () => {
        expect(normalizeToolCall('browser_tabs', {action: 'select', index: 2})).toEqual({
            tool: 'browser_tabs',
            args: {action: 'select', index: 2},
        });
    });

    it('keeps frontend dialog tool names unchanged', () => {
        expect(normalizeToolCall('browser_handle_dialog', {accept: true, promptText: 'ok'})).toEqual({
            tool: 'browser_handle_dialog',
            args: {accept: true, promptText: 'ok'},
        });
        expect(normalizeToolCall('browser_handle_dialog', {accept: false})).toEqual({
            tool: 'browser_handle_dialog',
            args: {accept: false},
        });
    });

    it('resolves selector-style frontend ref fields without renaming them', () => {
        expect(normalizeToolCall('browser_type', {ref: 'e2271', text: 'hello'})).toEqual({
            tool: 'browser_type',
            args: {ref: 'backend:2271', text: 'hello'},
        });
        expect(normalizeToolCall('browser_hover', {ref: '.cta'})).toEqual({
            tool: 'browser_hover',
            args: {ref: '.cta'},
        });
        expect(normalizeToolCall('browser_drag', {startRef: 'e1', endRef: 'e2'})).toEqual({
            tool: 'browser_drag',
            args: {startRef: 'backend:1', endRef: 'backend:2'},
        });
    });
});

describe('stale session handling', () => {
    const originalArgv = process.argv;
    const originalExit = process.exit;
    const originalConsoleLog = console.log;
    const originalConsoleError = console.error;

    beforeEach(() => {
        jest.clearAllMocks();
        process.argv = ['node', 'program.js'];
        process.exit = jest.fn(((code?: number) => {
            throw new Error(`process.exit:${code ?? 0}`);
        }) as typeof process.exit);
        console.log = jest.fn();
        console.error = jest.fn();
        daemonModule.ensureServerRunning.mockResolvedValue(undefined);
        stateModule.readState.mockReset();
        stateModule.writeState.mockReset();
        stateModule.clearState.mockReset();
        mockedAxios.create.mockReset();
    });

    afterEach(() => {
        process.argv = originalArgv;
        process.exit = originalExit;
        console.log = originalConsoleLog;
        console.error = originalConsoleError;
    });

    it('detects expired session errors from the backend', () => {
        expect(isStaleSessionErrorMessage('browser_navigate failed: Cannot find context with specified id')).toBe(true);
        expect(isStaleSessionErrorMessage('Saved session expired. Run "b4-playwright-cli open" first.')).toBe(false);
    });

    it('recreates the session and retries goto once when the saved session expired', async () => {
        stateModule.readState
            .mockReturnValueOnce({sessionId: 'stale-session', baseUrl: 'http://localhost:8182'})
            .mockReturnValueOnce({sessionId: 'stale-session', baseUrl: 'http://localhost:8182'})
            .mockReturnValue({sessionId: 'fresh-session', baseUrl: 'http://localhost:8182'});

        const post = jest.fn(async (_url: string, body: { tool: string; arguments: Record<string, unknown> }) => {
            switch (body.tool) {
                case 'browser_navigate':
                    if (body.arguments.sessionId === 'stale-session') {
                        return {
                            data: {
                                isError: true,
                                content: [{text: 'browser_navigate failed: Cannot find context with specified id'}],
                            },
                        };
                    }
                    return {
                        data: {
                            content: [{text: 'Navigated'}],
                        },
                    };
                case 'open_session':
                    return {
                        data: {
                            content: [{text: '{"sessionId":"fresh-session"}'}],
                        },
                    };
                case 'page_url':
                    return {data: {content: [{text: 'https://browser4.io'}]}};
                case 'page_title':
                    return {data: {content: [{text: 'Browser4'}]}};
                case 'browser_snapshot':
                    return {data: {content: [{text: 'snapshot: ok'}]}};
                default:
                    throw new Error(`Unexpected tool ${body.tool}`);
            }
        });

        mockedAxios.create.mockReturnValue({
            post,
            get: jest.fn(),
            defaults: {baseURL: 'http://localhost:8182'},
        });

        process.argv = ['node', 'program.js', 'goto', 'https://browser4.io'];

        await main();

        expect(stateModule.writeState).toHaveBeenNthCalledWith(1, {
            sessionId: undefined,
            baseUrl: 'http://localhost:8182',
        });
        expect(stateModule.writeState).toHaveBeenNthCalledWith(2, {
            sessionId: 'fresh-session',
            baseUrl: 'http://localhost:8182',
        });
        expect(post).toHaveBeenNthCalledWith(1, '/mcp/call-tool', {
            tool: 'browser_navigate',
            arguments: {url: 'https://browser4.io', sessionId: 'stale-session'},
        });
        expect(post).toHaveBeenNthCalledWith(2, '/mcp/call-tool', {
            tool: 'open_session',
            arguments: {},
        });
        expect(post).toHaveBeenNthCalledWith(3, '/mcp/call-tool', {
            tool: 'browser_navigate',
            arguments: {url: 'https://browser4.io', sessionId: 'fresh-session'},
        });
    });
});
