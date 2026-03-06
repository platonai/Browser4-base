/**
 * Copyright (c) Microsoft Corporation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * This file implements the daemon server that listens on a socket.
 * It handles client connections, processes incoming commands, and manages
 * the lifecycle of the browser context and backend server.
 */

import * as fs from 'fs';
import path from 'path';

import * as os from 'os';
import {spawn} from 'child_process';
import * as https from 'https';

import {commands} from './commands';
import {parseCommand} from './command';
import axios, {AxiosInstance} from "axios";
import {readState} from "../../state";

import type * as mcp from './../../mcp/exports';

function makeAxios(baseUrl: string): AxiosInstance {
    return axios.create({
        baseURL: baseUrl.replace(/\/$/, ''),
        timeout: 30_000,
        headers: {'Content-Type': 'application/json'},
    });
}

export async function ensureServerRunning(args: string[]): Promise<void> {
    const state = readState();
    let baseUrl = state.baseUrl || 'http://localhost:8182';

    // Check for --server override in args
    const serverIdx = args.indexOf('--server');
    if (serverIdx !== -1 && args[serverIdx + 1]) {
        baseUrl = args[serverIdx + 1];
    }

    // Only attempt to start if we're pointing to localhost
    if (!baseUrl.includes('localhost') && !baseUrl.includes('127.0.0.1')) {
        return;
    }

    // Check if already running
    try {
        const ax = makeAxios(baseUrl);
        await ax.get('/actuator/health');
        return;
    } catch (error) {
        // Not running, proceed to start
    }

    console.log('Browser4 server not running. Starting...');

    const jarPath = await findOrDownloadJar();
    const port = parseInt(new URL(baseUrl).port) || 8182;

    await startServer(jarPath, port);
}

async function findOrDownloadJar(): Promise<string> {
    // Check environment variable
    if (process.env.BROWSER4_JAR_PATH && fs.existsSync(process.env.BROWSER4_JAR_PATH)) {
        return process.env.BROWSER4_JAR_PATH;
    }

    // Check common locations
    const candidates = [
        path.join(os.homedir(), '.browser4', 'Browser4.jar'),
        path.resolve('Browser4.jar'),
        path.resolve('target', 'Browser4.jar'),
        path.resolve(__dirname, '..', '..', 'target', 'Browser4.jar'),
        path.resolve(__dirname, '..', '..', '..', 'browser4', 'browser4-agents', 'target', 'Browser4.jar'), // Monorepo location
    ];

    for (const candidate of candidates) {
        if (fs.existsSync(candidate)) {
            return candidate;
        }
    }

    // Download if not found
    const downloadPath = path.join(os.homedir(), '.browser4', 'Browser4.jar');
    await downloadJar(downloadPath);
    return downloadPath;
}

async function downloadJar(targetPath: string): Promise<void> {
    const dir = path.dirname(targetPath);
    if (!fs.existsSync(dir)) {
        fs.mkdirSync(dir, {recursive: true});
    }

    const url = 'https://github.com/platonai/Browser4/releases/latest/download/Browser4.jar';
    console.log(`Downloading Browser4.jar from ${url}...`);

    return new Promise((resolve, reject) => {
        const file = fs.createWriteStream(targetPath);
        https.get(url, (response) => {
            if (response.statusCode === 301 || response.statusCode === 302) {
                if (!response.headers.location) {
                    reject(new Error('Redirect without location header'));
                    return;
                }
                https.get(response.headers.location, (redirectResponse) => {
                    redirectResponse.pipe(file);
                    file.on('finish', () => {
                        file.close();
                        console.log('Download complete');
                        resolve();
                    });
                }).on('error', (err) => {
                    fs.unlinkSync(targetPath);
                    reject(err);
                });
            } else {
                response.pipe(file);
                file.on('finish', () => {
                    file.close();
                    console.log('Download complete');
                    resolve();
                });
            }
        }).on('error', (err) => {
            fs.unlinkSync(targetPath);
            reject(err);
        });
    });
}

async function startServer(jarPath: string, port: number): Promise<void> {
    console.log(`Starting server from ${jarPath} on port ${port}...`);

    const child = spawn('java', ['-jar', jarPath, `--server.port=${port}`], {
        detached: true,
        stdio: 'ignore' // or 'inherit' for debugging, but 'ignore' keeps it clean
    });

    child.unref(); // Allow the parent process to exit independently

    // Wait for health check
    const start = Date.now();
    const timeout = 60000;

    while (Date.now() - start < timeout) {
        try {
            await axios.get(`http://localhost:${port}/actuator/health`);
            console.log('Server is up and running.');
            return;
        } catch (e) {
            await new Promise(resolve => setTimeout(resolve, 1000));
        }
    }

    throw new Error(`Server failed to start within ${timeout}ms`);
}

function formatResult(result: mcp.CallToolResult) {
    // Formats the MCP tool execution result for the client response.
    const isError = result.isError;
    const text = result.content[0].type === 'text' ? result.content[0].text : undefined;
    return {isError, text};
}

function parseCliCommand(args: Record<string, string> & { _: string[] }): {
    toolName: string,
    toolParams: NonNullable<mcp.CallToolRequest['params']['arguments']>
} {
    // Parses CLI arguments to identify the command and its parameters.
    const command = commands[args._[0]];
    if (!command)
        throw new Error('Command is required');
    return parseCommand(command, args);
}
