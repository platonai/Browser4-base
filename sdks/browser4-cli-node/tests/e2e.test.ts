import * as fs from 'fs';
import * as http from 'http';
import * as net from 'net';
import * as os from 'os';
import * as path from 'path';
import {spawn, ChildProcess, execFileSync} from 'child_process';

import {commands} from '../src/cli/daemon/commands';

interface CliRunResult {
    stdout: string;
    stderr: string;
    exitCode: number;
}

interface FixtureContext {
    fixtureBaseUrl: string;
    browser4BaseUrl: string;
    tempRoot: string;
    workspaceDir: string;
    stateDir: string;
    uploadFilePath: string;
}

const isE2EEnabled = process.env.BROWSER4_CLI_E2E === 'true';
const describeE2E = isE2EEnabled ? describe : describe.skip;

const repoRoot = path.resolve(__dirname, '..', '..', '..');
const cliProgramPath = path.join(__dirname, '..', 'dist', 'program.js');
const defaultJarPath = path.join(repoRoot, 'browser4', 'browser4-agents', 'target', 'Browser4.jar');

const interactivePath = '/interactive';
const otherPath = '/other';
const interactiveTitle = 'Browser4 CLI Interactive Fixture';
const otherTitle = 'Browser4 CLI Other Fixture';

let fixtureContext: FixtureContext;
let fixtureServer: http.Server;
let browser4Process: ChildProcess;
const coveredCommands = new Set<string>();
const cliCommandTimeoutMs = 90_000;

function interactiveHtml(): string {
    return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <title>${interactiveTitle}</title>
  <style>
    body { margin: 0; font-family: sans-serif; min-height: 2400px; }
    #mouse-area {
      position: fixed;
      top: 0;
      left: 0;
      width: 480px;
      height: 320px;
      background: rgba(0, 128, 255, 0.08);
      border: 1px solid #08f;
    }
    #drag-source, #drag-target {
      width: 160px;
      height: 48px;
      margin-top: 16px;
      border: 1px solid #555;
      display: flex;
      align-items: center;
      justify-content: center;
    }
  </style>
</head>
<body>
  <div id="mouse-area">mouse area</div>
  <main style="padding: 360px 24px 24px;">
    <input id="type-target" type="text" />
    <input id="fill-target" type="text" />
    <input id="file-input" type="file" />
    <input id="check-target" type="checkbox" />
    <select id="select-target">
      <option value="">-- choose --</option>
      <option value="green">Green</option>
      <option value="blue">Blue</option>
    </select>
    <button id="click-target" type="button">Click</button>
    <button id="dblclick-target" type="button">Double Click</button>
    <button id="hover-target" type="button">Hover</button>
    <button id="prompt-target" type="button">Prompt</button>
    <button id="confirm-target" type="button">Confirm</button>
    <div id="drag-source" draggable="true">drag source</div>
    <div id="drag-target">drag target</div>
    <pre id="state-log"></pre>
  </main>
  <script>
    window.__browser4State = {
      clickCount: 0,
      doubleClickCount: 0,
      hovered: false,
      dragStarted: false,
      dragDropped: '',
      promptResult: '',
      confirmResult: '',
      keyEvents: [],
      mouseDownCount: 0,
      mouseUpCount: 0,
      lastMouse: null,
      lastWheel: null,
      typeValue: '',
      fillValue: '',
      checkbox: false,
      selectValue: '',
      uploadCount: 0,
      uploadName: '',
      submitCount: 0
    };

    function syncState() {
      const state = window.__browser4State;
      state.typeValue = document.getElementById('type-target').value;
      state.fillValue = document.getElementById('fill-target').value;
      state.checkbox = document.getElementById('check-target').checked;
      state.selectValue = document.getElementById('select-target').value;
      const files = document.getElementById('file-input').files;
      state.uploadCount = files ? files.length : 0;
      state.uploadName = files && files[0] ? files[0].name : '';
      document.getElementById('state-log').textContent = JSON.stringify(state);
    }

    document.getElementById('click-target').addEventListener('click', () => {
      window.__browser4State.clickCount += 1;
      console.info('click-target clicked');
      syncState();
    });

    document.getElementById('dblclick-target').addEventListener('dblclick', () => {
      window.__browser4State.doubleClickCount += 1;
      syncState();
    });

    document.getElementById('hover-target').addEventListener('mouseenter', () => {
      window.__browser4State.hovered = true;
      syncState();
    });

    document.getElementById('drag-source').addEventListener('dragstart', (event) => {
      window.__browser4State.dragStarted = true;
      event.dataTransfer.setData('text/plain', 'drag-source');
      syncState();
    });

    document.getElementById('drag-target').addEventListener('dragover', (event) => {
      event.preventDefault();
    });

    document.getElementById('drag-target').addEventListener('drop', (event) => {
      event.preventDefault();
      window.__browser4State.dragDropped = event.dataTransfer.getData('text/plain');
      syncState();
    });

    document.getElementById('prompt-target').addEventListener('click', () => {
      setTimeout(() => {
        const value = window.prompt('Enter prompt text', 'seed');
        window.__browser4State.promptResult = value === null ? '__dismissed__' : value;
        syncState();
      }, 0);
    });

    document.getElementById('confirm-target').addEventListener('click', () => {
      setTimeout(() => {
        const accepted = window.confirm('Confirm action');
        window.__browser4State.confirmResult = accepted ? 'accepted' : 'dismissed';
        syncState();
      }, 0);
    });

    document.getElementById('fill-target').addEventListener('keydown', (event) => {
      if (event.key === 'Enter') {
        window.__browser4State.submitCount += 1;
        syncState();
      }
    });

    document.addEventListener('keydown', (event) => {
      window.__browser4State.keyEvents.push('down:' + event.key);
      syncState();
    });

    document.addEventListener('keyup', (event) => {
      window.__browser4State.keyEvents.push('up:' + event.key);
      syncState();
    });

    document.getElementById('type-target').addEventListener('input', syncState);
    document.getElementById('fill-target').addEventListener('input', syncState);
    document.getElementById('check-target').addEventListener('change', syncState);
    document.getElementById('select-target').addEventListener('change', syncState);
    document.getElementById('file-input').addEventListener('change', syncState);

    const mouseArea = document.getElementById('mouse-area');
    mouseArea.addEventListener('mousemove', (event) => {
      window.__browser4State.lastMouse = [Math.round(event.clientX), Math.round(event.clientY)];
      syncState();
    });
    mouseArea.addEventListener('mousedown', () => {
      window.__browser4State.mouseDownCount += 1;
      syncState();
    });
    mouseArea.addEventListener('mouseup', () => {
      window.__browser4State.mouseUpCount += 1;
      syncState();
    });
    mouseArea.addEventListener('wheel', (event) => {
      window.__browser4State.lastWheel = [Math.round(event.deltaX), Math.round(event.deltaY)];
      syncState();
    });

    console.info('interactive fixture ready');
    syncState();
  </script>
</body>
</html>`;
}

function otherHtml(): string {
    return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <title>${otherTitle}</title>
</head>
<body>
  <h1 id="page-marker">other page</h1>
  <script>console.info('other fixture ready');</script>
</body>
</html>`;
}

async function getFreePort(): Promise<number> {
    return await new Promise((resolve, reject) => {
        const server = net.createServer();
        server.on('error', reject);
        server.listen(0, '127.0.0.1', () => {
            const address = server.address();
            if (!address || typeof address === 'string') {
                reject(new Error('Failed to allocate a TCP port.'));
                return;
            }
            const {port} = address;
            server.close((error) => {
                if (error) {
                    reject(error);
                    return;
                }
                resolve(port);
            });
        });
    });
}

async function startFixtureServer(): Promise<http.Server> {
    const server = http.createServer((request, response) => {
        const url = request.url ?? '/';
        if (url === interactivePath || url === '/') {
            response.writeHead(200, {'Content-Type': 'text/html; charset=utf-8'});
            response.end(interactiveHtml());
            return;
        }
        if (url === otherPath) {
            response.writeHead(200, {'Content-Type': 'text/html; charset=utf-8'});
            response.end(otherHtml());
            return;
        }

        response.writeHead(404, {'Content-Type': 'text/plain; charset=utf-8'});
        response.end('not found');
    });

    const port = await getFreePort();
    await new Promise<void>((resolve, reject) => {
        server.once('error', reject);
        server.listen(port, '127.0.0.1', () => resolve());
    });
    return server;
}

async function waitForHealth(baseUrl: string, timeoutMs: number): Promise<void> {
    const deadline = Date.now() + timeoutMs;
    let lastError = 'unknown';

    while (Date.now() < deadline) {
        try {
            const body = await httpGet(`${baseUrl}/actuator/health`);
            if (body.includes('"status":"UP"') || body.includes('"status":"UP"'.replace(/"/g, ''))) {
                return;
            }
            lastError = body;
        } catch (error) {
            lastError = error instanceof Error ? error.message : String(error);
        }
        await sleep(1000);
    }

    throw new Error(`Browser4 did not become healthy in time. Last response: ${lastError}`);
}

function httpGet(url: string): Promise<string> {
    return new Promise((resolve, reject) => {
        const request = http.get(url, (response) => {
            const chunks: Buffer[] = [];
            response.on('data', chunk => chunks.push(Buffer.from(chunk)));
            response.on('end', () => resolve(Buffer.concat(chunks).toString('utf-8')));
        });
        request.on('error', reject);
    });
}

async function startBrowser4(baseUrl: string, jarPath: string): Promise<ChildProcess> {
    const port = new URL(baseUrl).port;
    const child = spawn(
        'java',
        ['-jar', jarPath, `--server.port=${port}`],
        {
            cwd: path.dirname(jarPath),
            stdio: ['ignore', 'pipe', 'pipe'],
        },
    );

    let logs = '';
    child.stdout.on('data', chunk => {
        logs += chunk.toString();
    });
    child.stderr.on('data', chunk => {
        logs += chunk.toString();
    });

    try {
        await waitForHealth(baseUrl, 120_000);
        return child;
    } catch (error) {
        child.kill();
        throw new Error(`${error instanceof Error ? error.message : String(error)}\n${logs}`);
    }
}

async function stopProcess(child: ChildProcess | undefined): Promise<void> {
    if (!child || !child.pid) {
        return;
    }

    const waitForClose = (): Promise<boolean> => new Promise((resolve) => {
        const timer = setTimeout(() => resolve(false), 10_000);
        child.once('close', () => {
            clearTimeout(timer);
            resolve(true);
        });
    });

    try {
        child.kill();
    } catch {
        // Ignore and fall back to a forced stop below.
    }

    let closed = await waitForClose();
    if (!closed) {
        try {
            execFileSync(
                'powershell',
                [
                    '-NoProfile',
                    '-NonInteractive',
                    '-Command',
                    `Stop-Process -Id ${child.pid} -Force`,
                ],
                {stdio: 'ignore'},
            );
        } catch {
            // Ignore force-stop failures and continue teardown.
        }
        closed = await waitForClose();
    }

    child.stdout?.destroy();
    child.stderr?.destroy();
}

async function runCliProcess(args: string[]): Promise<CliRunResult> {
    return await new Promise((resolve, reject) => {
        const child = spawn(
            process.execPath,
            [cliProgramPath, `--server=${fixtureContext.browser4BaseUrl}`, ...args],
            {
                cwd: fixtureContext.workspaceDir,
                env: {
                    ...process.env,
                    BROWSER4_CLI_STATE_DIR: fixtureContext.stateDir,
                },
                stdio: ['ignore', 'pipe', 'pipe'],
            },
        );

        let stdout = '';
        let stderr = '';
        let settled = false;

        const timeout = setTimeout(() => {
            if (settled) {
                return;
            }
            settled = true;
            child.kill();
            resolve({
                stdout,
                stderr: `${stderr}\nTimed out after ${cliCommandTimeoutMs} ms.`,
                exitCode: -1,
            });
        }, cliCommandTimeoutMs);

        child.stdout.on('data', chunk => {
            stdout += chunk.toString();
        });
        child.stderr.on('data', chunk => {
            stderr += chunk.toString();
        });
        child.on('error', error => {
            if (settled) {
                return;
            }
            settled = true;
            clearTimeout(timeout);
            reject(error);
        });
        child.on('close', (exitCode) => {
            if (settled) {
                return;
            }
            settled = true;
            clearTimeout(timeout);
            resolve({
                stdout,
                stderr,
                exitCode: exitCode ?? 0,
            });
        });
    });
}

async function runCommand(command: string, ...args: string[]): Promise<CliRunResult> {
    coveredCommands.add(command);
    appendDebug(`START ${command} ${args.join(' ')}`.trim());
    const result = await runCliProcess([command, ...args]);
    appendDebug(`END ${command} exit=${result.exitCode}\nstdout:\n${result.stdout}\nstderr:\n${result.stderr}`);
    if (result.exitCode !== 0) {
        throw new Error([
            `Command failed: ${command} ${args.join(' ')}`.trim(),
            `exit=${result.exitCode}`,
            `stdout:\n${result.stdout}`,
            `stderr:\n${result.stderr}`,
        ].join('\n\n'));
    }
    return result;
}

async function runCommandExpectingFailure(
    command: string,
    args: string[],
    messagePattern: RegExp,
): Promise<CliRunResult> {
    coveredCommands.add(command);
    appendDebug(`START ${command} ${args.join(' ')} (expecting failure)`.trim());
    const result = await runCliProcess([command, ...args]);
    appendDebug(`END ${command} exit=${result.exitCode}\nstdout:\n${result.stdout}\nstderr:\n${result.stderr}`);
    expect(result.exitCode).not.toBe(0);
    expect(`${result.stdout}\n${result.stderr}`).toMatch(messagePattern);
    return result;
}

function stripSnapshotOutput(stdout: string): string {
    const marker = '\n### Page';
    const index = stdout.indexOf(marker);
    const withoutSnapshot = (index === -1 ? stdout : stdout.slice(0, index)).trim();
    return withoutSnapshot
        .split(/\r?\n/)
        .map(line => line.trim())
        .filter(line => line.length > 0 && line !== 'ensuring server...')
        .join('\n')
        .trim();
}

function interactiveUrl(): string {
    return `${fixtureContext.fixtureBaseUrl}${interactivePath}`;
}

function otherUrl(): string {
    return `${fixtureContext.fixtureBaseUrl}${otherPath}`;
}

async function evalText(expression: string): Promise<string> {
    const result = await runCommand('eval', expression);
    return stripSnapshotOutput(result.stdout);
}

async function readInteractiveState(): Promise<Record<string, unknown>> {
    const text = await evalText("document.getElementById('state-log').textContent");
    return JSON.parse(text) as Record<string, unknown>;
}

async function waitForState(
    predicate: (state: Record<string, unknown>) => boolean,
    timeoutMs: number = 15_000,
): Promise<Record<string, unknown>> {
    const deadline = Date.now() + timeoutMs;
    while (Date.now() < deadline) {
        const state = await readInteractiveState();
        if (predicate(state)) {
            return state;
        }
        await sleep(300);
    }
    throw new Error('Timed out waiting for interactive fixture state to match.');
}

async function readPersistedSessionId(): Promise<string> {
    const raw = await fs.promises.readFile(path.join(fixtureContext.stateDir, 'cli-state.json'), 'utf-8');
    const parsed = JSON.parse(raw) as {sessionId?: string};
    if (!parsed.sessionId) {
        throw new Error('No persisted session ID found.');
    }
    return parsed.sessionId;
}

async function resetCliArtifacts(): Promise<void> {
    await fs.promises.rm(fixtureContext.stateDir, {recursive: true, force: true});
    await fs.promises.rm(path.join(fixtureContext.workspaceDir, '.b4-playwright-cli'), {recursive: true, force: true});
}

function sleep(timeoutMs: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, timeoutMs));
}

function appendDebug(message: string): void {
    if (!fixtureContext?.tempRoot) {
        return;
    }
    fs.appendFileSync(
        path.join(fixtureContext.tempRoot, 'e2e-debug.log'),
        `[${new Date().toISOString()}] ${message}\n`,
        'utf-8',
    );
}

function escapeRegex(value: string): string {
    return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

function extractTabId(output: string, url: string): string {
    const pattern = new RegExp(`id[:=]"?([^",}\\s]+)"?.*?url[:=]"?${escapeRegex(url)}`, 's');
    const match = pattern.exec(output);
    if (!match?.[1]) {
        throw new Error(`Could not find tab id for ${url} in output:\n${output}`);
    }
    return match[1];
}

describeE2E('b4-playwright-cli real backend e2e', () => {
    jest.setTimeout(300_000);

    beforeAll(async () => {
        const jarPath = process.env.BROWSER4_E2E_JAR_PATH || defaultJarPath;
        appendDebug(`Using Browser4 jar: ${jarPath}`);
        if (!fs.existsSync(jarPath)) {
            throw new Error(`Browser4 jar not found at ${jarPath}. Build browser4/browser4-agents first or set BROWSER4_E2E_JAR_PATH.`);
        }
        if (!fs.existsSync(cliProgramPath)) {
            throw new Error(`CLI program not found at ${cliProgramPath}. Run npm run build first.`);
        }

        const tempRoot = await fs.promises.mkdtemp(path.join(os.tmpdir(), 'b4-playwright-cli-e2e-'));
        const workspaceDir = path.join(tempRoot, 'workspace');
        const stateDir = path.join(tempRoot, 'state');
        await fs.promises.mkdir(workspaceDir, {recursive: true});
        await fs.promises.mkdir(stateDir, {recursive: true});

        const uploadFilePath = path.join(tempRoot, 'upload.txt');
        await fs.promises.writeFile(uploadFilePath, 'b4-playwright-cli e2e upload payload', 'utf-8');

        fixtureServer = await startFixtureServer();
        const fixtureAddress = fixtureServer.address();
        if (!fixtureAddress || typeof fixtureAddress === 'string') {
            throw new Error('Fixture server failed to bind.');
        }

        const browser4Port = await getFreePort();
        const browser4BaseUrl = `http://127.0.0.1:${browser4Port}`;
        browser4Process = await startBrowser4(browser4BaseUrl, jarPath);
        appendDebug(`Browser4 started at ${browser4BaseUrl}`);

        fixtureContext = {
            fixtureBaseUrl: `http://127.0.0.1:${fixtureAddress.port}`,
            browser4BaseUrl,
            tempRoot,
            workspaceDir,
            stateDir,
            uploadFilePath,
        };
    });

    afterAll(async () => {
        await stopProcess(browser4Process);
        await new Promise<void>((resolve, reject) => {
            if (!fixtureServer || !fixtureServer.listening) {
                resolve();
                return;
            }
            fixtureServer.closeAllConnections?.();
            fixtureServer.closeIdleConnections?.();
            fixtureServer.close(error => error ? reject(error) : resolve());
        });
        if (fixtureContext?.tempRoot) {
            await fs.promises.rm(fixtureContext.tempRoot, {recursive: true, force: true});
        }
    });

    beforeEach(async () => {
        await resetCliArtifacts();
        appendDebug('Reset CLI artifacts');
    });

    it('covers session and navigation commands', async () => {
        const openResult = await runCommand('open');
        expect(openResult.stdout).toContain('Session opened:');

        const sessionId = await readPersistedSessionId();
        const listResult = await runCommand('list');
        expect(listResult.stdout).toContain(sessionId);

        await runCommand('goto', interactiveUrl());
        expect(await evalText('window.location.pathname')).toBe(interactivePath);

        await runCommand('goto', otherUrl());
        expect(await evalText('document.title')).toBe(otherTitle);

        await runCommand('go-back');
        expect(await evalText('window.location.pathname')).toBe(interactivePath);

        await runCommand('go-forward');
        expect(await evalText('window.location.pathname')).toBe(otherPath);

        await runCommand('reload');
        expect(await evalText('document.title')).toBe(otherTitle);

        const deleteDataResult = await runCommand('delete-data');
        expect(stripSnapshotOutput(deleteDataResult.stdout).toLowerCase()).toContain('deleted');

        const closeResult = await runCommand('close');
        expect(closeResult.stdout).toContain('Session closed.');
    });

    it('covers interaction, console, and export commands', async () => {
        await runCommand('open');
        await runCommand('goto', interactiveUrl());

        const resizeResult = await runCommand('resize', '1280', '900');
        expect(resizeResult.stdout).toContain('### Page');
        const viewportWidth = Number(await evalText('window.innerWidth.toString()'));
        expect(viewportWidth).toBeGreaterThanOrEqual(1000);

        await runCommand('type', '#type-target', 'hello world');
        await waitForState(state => state.typeValue === 'hello world');

        await runCommand('fill', '#fill-target', 'filled text');
        await waitForState(state => state.fillValue === 'filled text');

        await runCommand('press', '#fill-target', 'Enter');
        await waitForState(state => Number(state.submitCount) >= 1);

        await runCommand('click', '#type-target');
        await runCommand('keydown', 'Shift');
        await runCommand('keyup', 'Shift');
        const keyboardState = await waitForState(state =>
            Array.isArray(state.keyEvents)
            && state.keyEvents.includes('down:Shift')
            && state.keyEvents.includes('up:Shift'),
        );
        expect(keyboardState.keyEvents).toEqual(expect.arrayContaining(['down:Shift', 'up:Shift']));

        await runCommand('click', '#click-target');
        await waitForState(state => Number(state.clickCount) >= 1);

        await runCommand('dblclick', '#dblclick-target');
        await waitForState(state => Number(state.doubleClickCount) >= 1);

        await runCommand('hover', '#hover-target');
        await waitForState(state => state.hovered === true);

        await runCommand('drag', '#drag-source', '#drag-target');

        await runCommand('select', '#select-target', 'green');
        await waitForState(state => state.selectValue === 'green');

        await runCommand('check', '#check-target');
        await waitForState(state => state.checkbox === true);

        await runCommand('uncheck', '#check-target');
        await waitForState(state => state.checkbox === false);

        await runCommand('upload', '#file-input', fixtureContext.uploadFilePath);
        await waitForState(state => state.uploadName === 'upload.txt');

        await runCommandExpectingFailure('console', ['info'], /Unknown tool: browser_console_messages/);

        const snapshotResult = await runCommand('snapshot', '--filename=interactive.yml');
        expect(snapshotResult.stdout).toContain('[Snapshot](');
        const snapshotPath = path.join(fixtureContext.workspaceDir, '.b4-playwright-cli', 'snapshot', 'interactive.yml');
        expect(fs.existsSync(snapshotPath)).toBe(true);

        await runCommand('screenshot', '--filename=interactive.png');
        const screenshotPath = path.join(fixtureContext.workspaceDir, '.b4-playwright-cli', 'snapshot', 'interactive.png');
        expect(fs.statSync(screenshotPath).size).toBeGreaterThan(0);

        await runCommandExpectingFailure('pdf', ['--filename=interactive.pdf'], /Unknown tool: browser_pdf_save/);

        await runCommand('close');
    });

    it('covers mouse and dialog commands', async () => {
        await runCommand('open');
        await runCommand('goto', interactiveUrl());
        await runCommand('resize', '1280', '900');

        await runCommand('mousemove', '120', '120');
        await waitForState(state => Array.isArray(state.lastMouse));

        await runCommand('mousedown', 'left');
        await waitForState(state => Number(state.mouseDownCount) >= 1);

        await runCommand('mouseup', 'left');
        await waitForState(state => Number(state.mouseUpCount) >= 1);

        await runCommand('mousewheel', '0', '160');
        const wheelState = await waitForState(state => Array.isArray(state.lastWheel));
        expect(wheelState.lastWheel).not.toBeNull();

        await evalText("(() => { setTimeout(() => document.getElementById('prompt-target').click(), 100); return 'scheduled'; })()");
        await sleep(500);
        await runCommand('dialog-accept', 'accepted by cli');
        await waitForState(state => state.promptResult === 'accepted by cli');

        await evalText("(() => { setTimeout(() => document.getElementById('confirm-target').click(), 100); return 'scheduled'; })()");
        await sleep(500);
        await runCommand('dialog-dismiss');
        await waitForState(state => state.confirmResult === 'dismissed');

        await runCommand('close');
    });

    it('covers tab commands plus close-all and kill-all', async () => {
        await runCommand('open');
        await runCommand('goto', interactiveUrl());

        const initialTabs = await runCommand('tab-list');
        expect(stripSnapshotOutput(initialTabs.stdout)).toContain(interactiveUrl());

        await runCommand('tab-new', otherUrl());
        const updatedTabs = await runCommand('tab-list');
        const tabOutput = stripSnapshotOutput(updatedTabs.stdout);
        expect(tabOutput).toContain(interactiveUrl());
        expect(tabOutput).toContain(otherUrl());

        const otherTabId = extractTabId(tabOutput, otherUrl());
        await runCommand('tab-select', otherTabId);

        await runCommand('tab-close', otherTabId);

        const closeAllResult = await runCommand('close-all');
        expect(closeAllResult.stdout).toContain('No tracked Browser4 processes found.');

        const killAllResult = await runCommand('kill-all');
        expect(killAllResult.stdout).toContain('No tracked Browser4 processes found.');
    });

    it('keeps the e2e matrix aligned with every supported command', () => {
        expect(Array.from(coveredCommands).sort()).toEqual(Object.keys(commands).sort());
    });
});
