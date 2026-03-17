import * as fs from 'fs';
import * as os from 'os';
import * as path from 'path';

import {
    clearManagedServerProcesses,
    managedServerRegistryPath,
    readManagedServerProcesses,
    registerManagedServerProcess,
    shutdownManagedServerProcesses,
} from '../src/cli/daemon/managedProcesses';

describe('managedProcesses', () => {
    let tempStateDir: string;
    let registryPath: string;

    beforeEach(() => {
        tempStateDir = fs.mkdtempSync(path.join(os.tmpdir(), 'b4-playwright-cli-managed-'));
        registryPath = managedServerRegistryPath(tempStateDir);
    });

    afterEach(() => {
        clearManagedServerProcesses(registryPath);
        fs.rmSync(tempStateDir, {recursive: true, force: true});
    });

    it('registers and reads tracked Browser4 processes', () => {
        registerManagedServerProcess({
            pid: 101,
            baseUrl: 'http://localhost:8182',
            port: 8182,
            jarPath: 'C:\\Browser4.jar',
            startedAt: '2026-03-15T00:00:00.000Z',
        }, registryPath);

        expect(readManagedServerProcesses(registryPath)).toEqual([
            {
                pid: 101,
                baseUrl: 'http://localhost:8182',
                port: 8182,
                jarPath: 'C:\\Browser4.jar',
                startedAt: '2026-03-15T00:00:00.000Z',
            },
        ]);
    });

    it('gracefully stops tracked processes before forcing them', async () => {
        registerManagedServerProcess({
            pid: 202,
            baseUrl: 'http://localhost:8182',
            port: 8182,
            jarPath: 'C:\\Browser4.jar',
            startedAt: '2026-03-15T00:00:00.000Z',
        }, registryPath);

        const running = new Set([202]);
        const gracefulStop = jest.fn((pid: number) => {
            running.delete(pid);
        });
        const forceStop = jest.fn();

        const result = await shutdownManagedServerProcesses({
            registryPath,
            ops: {
                isRunning: (pid: number) => running.has(pid),
                gracefulStop,
                forceStop,
                waitForExit: async (pid: number) => !running.has(pid),
            },
        });

        expect(gracefulStop).toHaveBeenCalledWith(202);
        expect(forceStop).not.toHaveBeenCalled();
        expect(result).toEqual({
            stoppedPids: [202],
            missingPids: [],
            forcedPids: [],
            remainingPids: [],
        });
        expect(readManagedServerProcesses(registryPath)).toEqual([]);
    });

    it('forces a tracked process when graceful shutdown times out', async () => {
        registerManagedServerProcess({
            pid: 303,
            baseUrl: 'http://localhost:8282',
            port: 8282,
            jarPath: 'C:\\Browser4.jar',
            startedAt: '2026-03-15T00:00:00.000Z',
        }, registryPath);

        const running = new Set([303]);
        const gracefulStop = jest.fn();
        const forceStop = jest.fn((pid: number) => {
            running.delete(pid);
        });
        let waitCalls = 0;

        const result = await shutdownManagedServerProcesses({
            registryPath,
            ops: {
                isRunning: (pid: number) => running.has(pid),
                gracefulStop,
                forceStop,
                waitForExit: async (pid: number) => {
                    waitCalls += 1;
                    return waitCalls > 1 && !running.has(pid);
                },
            },
        });

        expect(gracefulStop).toHaveBeenCalledWith(303);
        expect(forceStop).toHaveBeenCalledWith(303);
        expect(result).toEqual({
            stoppedPids: [303],
            missingPids: [],
            forcedPids: [303],
            remainingPids: [],
        });
        expect(readManagedServerProcesses(registryPath)).toEqual([]);
    });

    it('force kills tracked processes when kill-all is used', async () => {
        registerManagedServerProcess({
            pid: 404,
            baseUrl: 'http://localhost:8383',
            port: 8383,
            jarPath: 'C:\\Browser4.jar',
            startedAt: '2026-03-15T00:00:00.000Z',
        }, registryPath);

        const running = new Set([404]);
        const gracefulStop = jest.fn();
        const forceStop = jest.fn((pid: number) => {
            running.delete(pid);
        });

        const result = await shutdownManagedServerProcesses({
            force: true,
            registryPath,
            ops: {
                isRunning: (pid: number) => running.has(pid),
                gracefulStop,
                forceStop,
                waitForExit: async (pid: number) => !running.has(pid),
            },
        });

        expect(gracefulStop).not.toHaveBeenCalled();
        expect(forceStop).toHaveBeenCalledWith(404);
        expect(result).toEqual({
            stoppedPids: [404],
            missingPids: [],
            forcedPids: [404],
            remainingPids: [],
        });
        expect(readManagedServerProcesses(registryPath)).toEqual([]);
    });
});
