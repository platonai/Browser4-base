import * as fs from 'fs';
import * as os from 'os';
import * as path from 'path';
import {execFileSync} from 'node:child_process';

import {DEFAULT_STATE_DIR} from '../../state';

export interface ManagedServerProcess {
    pid: number;
    baseUrl: string;
    port: number;
    jarPath: string;
    startedAt: string;
}

interface ManagedServerProcessRegistry {
    processes: ManagedServerProcess[];
}

export interface ShutdownOps {
    isRunning(pid: number): boolean;
    gracefulStop(pid: number): void;
    forceStop(pid: number): void;
    waitForExit(pid: number, timeoutMs: number, pollIntervalMs: number): Promise<boolean>;
}

export interface ShutdownManagedServerProcessesOptions {
    force?: boolean;
    registryPath?: string;
    timeoutMs?: number;
    pollIntervalMs?: number;
    ops?: ShutdownOps;
}

export interface ShutdownManagedServerProcessesResult {
    stoppedPids: number[];
    missingPids: number[];
    forcedPids: number[];
    remainingPids: number[];
}

const DEFAULT_REGISTRY_NAME = 'cli-managed-processes.json';
const DEFAULT_TIMEOUT_MS = 5_000;
const DEFAULT_POLL_INTERVAL_MS = 250;

export function managedServerRegistryPath(stateDir: string = DEFAULT_STATE_DIR): string {
    return path.join(stateDir, DEFAULT_REGISTRY_NAME);
}

export function readManagedServerProcesses(registryPath: string = managedServerRegistryPath()): ManagedServerProcess[] {
    try {
        const raw = fs.readFileSync(registryPath, 'utf-8');
        const parsed = JSON.parse(raw) as Partial<ManagedServerProcessRegistry>;
        return Array.isArray(parsed.processes)
            ? parsed.processes.filter(isManagedServerProcess)
            : [];
    } catch {
        return [];
    }
}

export function registerManagedServerProcess(
    processInfo: ManagedServerProcess,
    registryPath: string = managedServerRegistryPath(),
): void {
    const existing = readManagedServerProcesses(registryPath)
        .filter(entry => entry.pid !== processInfo.pid);
    existing.push(processInfo);
    writeManagedServerProcesses(existing, registryPath);
}

export function removeManagedServerProcess(
    pid: number,
    registryPath: string = managedServerRegistryPath(),
): void {
    const remaining = readManagedServerProcesses(registryPath)
        .filter(entry => entry.pid !== pid);
    writeManagedServerProcesses(remaining, registryPath);
}

export function clearManagedServerProcesses(
    registryPath: string = managedServerRegistryPath(),
): void {
    try {
        fs.unlinkSync(registryPath);
    } catch {
        // Nothing to clear.
    }
}

export async function shutdownManagedServerProcesses(
    options: ShutdownManagedServerProcessesOptions = {},
): Promise<ShutdownManagedServerProcessesResult> {
    const registryPath = options.registryPath ?? managedServerRegistryPath();
    const timeoutMs = options.timeoutMs ?? DEFAULT_TIMEOUT_MS;
    const pollIntervalMs = options.pollIntervalMs ?? DEFAULT_POLL_INTERVAL_MS;
    const ops = options.ops ?? createDefaultShutdownOps();
    const trackedProcesses = readManagedServerProcesses(registryPath);
    const remainingProcesses: ManagedServerProcess[] = [];
    const stoppedPids: number[] = [];
    const missingPids: number[] = [];
    const forcedPids: number[] = [];

    for (const trackedProcess of trackedProcesses) {
        const {pid} = trackedProcess;
        if (!ops.isRunning(pid)) {
            missingPids.push(pid);
            continue;
        }

        if (options.force) {
            ops.forceStop(pid);
            forcedPids.push(pid);
            const exited = await ops.waitForExit(pid, timeoutMs, pollIntervalMs);
            if (exited) {
                stoppedPids.push(pid);
            } else {
                remainingProcesses.push(trackedProcess);
            }
            continue;
        }

        ops.gracefulStop(pid);
        const exitedGracefully = await ops.waitForExit(pid, timeoutMs, pollIntervalMs);
        if (exitedGracefully) {
            stoppedPids.push(pid);
            continue;
        }

        ops.forceStop(pid);
        forcedPids.push(pid);
        const exitedAfterForce = await ops.waitForExit(pid, timeoutMs, pollIntervalMs);
        if (exitedAfterForce) {
            stoppedPids.push(pid);
        } else {
            remainingProcesses.push(trackedProcess);
        }
    }

    writeManagedServerProcesses(remainingProcesses, registryPath);

    return {
        stoppedPids,
        missingPids,
        forcedPids,
        remainingPids: remainingProcesses.map(entry => entry.pid),
    };
}

function writeManagedServerProcesses(
    processes: ManagedServerProcess[],
    registryPath: string = managedServerRegistryPath(),
): void {
    const dir = path.dirname(registryPath);
    if (!fs.existsSync(dir)) {
        fs.mkdirSync(dir, {recursive: true});
    }

    if (processes.length === 0) {
        clearManagedServerProcesses(registryPath);
        return;
    }

    const payload: ManagedServerProcessRegistry = {processes};
    fs.writeFileSync(registryPath, JSON.stringify(payload, null, 2), 'utf-8');
}

function isManagedServerProcess(value: unknown): value is ManagedServerProcess {
    if (!value || typeof value !== 'object') {
        return false;
    }

    const candidate = value as Partial<ManagedServerProcess>;
    return typeof candidate.pid === 'number'
        && Number.isInteger(candidate.pid)
        && typeof candidate.baseUrl === 'string'
        && typeof candidate.port === 'number'
        && Number.isInteger(candidate.port)
        && typeof candidate.jarPath === 'string'
        && typeof candidate.startedAt === 'string';
}

function createDefaultShutdownOps(): ShutdownOps {
    return {
        isRunning: (pid: number): boolean => {
            try {
                process.kill(pid, 0);
                return true;
            } catch {
                return false;
            }
        },
        gracefulStop: (pid: number): void => {
            if (os.platform() === 'win32') {
                try {
                    execFileSync('jcmd', [String(pid), 'VM.exit', '0'], {stdio: 'ignore'});
                    return;
                } catch {
                    execFileSync(
                        'powershell',
                        [
                            '-NoProfile',
                            '-NonInteractive',
                            '-Command',
                            `Stop-Process -Id ${pid}`,
                        ],
                        {stdio: 'ignore'},
                    );
                    return;
                }
            }

            process.kill(pid, 'SIGTERM');
        },
        forceStop: (pid: number): void => {
            if (os.platform() === 'win32') {
                execFileSync(
                    'powershell',
                    [
                        '-NoProfile',
                        '-NonInteractive',
                        '-Command',
                        `Stop-Process -Id ${pid} -Force`,
                    ],
                    {stdio: 'ignore'},
                );
                return;
            }

            process.kill(pid, 'SIGKILL');
        },
        waitForExit: async (pid: number, timeoutMs: number, pollIntervalMs: number): Promise<boolean> => {
            const start = Date.now();
            while (Date.now() - start < timeoutMs) {
                try {
                    process.kill(pid, 0);
                    await delay(pollIntervalMs);
                } catch {
                    return true;
                }
            }

            try {
                process.kill(pid, 0);
                return false;
            } catch {
                return true;
            }
        },
    };
}

function delay(timeoutMs: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, timeoutMs));
}
