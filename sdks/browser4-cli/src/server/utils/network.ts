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

import http from 'http';

import type net from 'net';
import {ManualPromise} from "../../utils/manualPromise";

export function createHttpServer(requestListener?: (req: http.IncomingMessage, res: http.ServerResponse) => void): http.Server;
export function createHttpServer(options: http.ServerOptions, requestListener?: (req: http.IncomingMessage, res: http.ServerResponse) => void): http.Server;
export function createHttpServer(...args: any[]): http.Server {
    const server = http.createServer(...args);
    decorateServer(server);
    return server;
}

export async function startHttpServer(server: http.Server, options: { host?: string, port?: number }) {
    const {host = 'localhost', port = 0} = options;
    const errorPromise = new ManualPromise();
    const errorListener = (error: Error) => errorPromise.reject(error);
    server.on('error', errorListener);
    try {
        server.listen(port, host);
        await Promise.race([
            new Promise(cb => server.once('listening', cb)),
            errorPromise,
        ]);
    } finally {
        server.removeListener('error', errorListener);
    }
}

export function decorateServer(server: net.Server) {
    const sockets = new Set<net.Socket>();
    server.on('connection', socket => {
        sockets.add(socket);
        socket.once('close', () => sockets.delete(socket));
    });

    const close = server.close;
    server.close = (callback?: (err?: Error) => void) => {
        for (const socket of sockets)
            socket.destroy();
        sockets.clear();
        return close.call(server, callback);
    };
}
