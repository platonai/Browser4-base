/**
 * Browser4Driver manages the lifecycle of a local Browser4.jar process.
 * 
 * Provides automatic download, startup, and shutdown of the Browser4 server.
 */

import { spawn, ChildProcess } from 'child_process';
import * as fs from 'fs';
import * as path from 'path';
import * as https from 'https';
import * as os from 'os';
import axios from 'axios';

export interface Browser4DriverConfig {
  /**
   * Directory to store the Browser4.jar file.
   * Default: ~/.browser4
   */
  homeDir?: string;

  /**
   * Port for the Browser4 server.
   * Default: 8182
   */
  port?: number;

  /**
   * Startup timeout in milliseconds.
   * Default: 60000 (60 seconds)
   */
  startupTimeout?: number;

  /**
   * Download URL for Browser4.jar.
   */
  downloadUrl?: string;

  /**
   * Whether to auto-download if jar not found.
   * Default: true
   */
  autoDownload?: boolean;

  /**
   * Java executable path.
   * Default: 'java'
   */
  javaPath?: string;
}

export class Browser4Driver {
  private homeDir: string;
  private port: number;
  private startupTimeout: number;
  private downloadUrl: string;
  private autoDownload: boolean;
  private javaPath: string;
  private process?: ChildProcess;
  private isRunning: boolean = false;

  constructor(config: Browser4DriverConfig = {}) {
    this.homeDir = config.homeDir || path.join(os.homedir(), '.browser4');
    this.port = config.port || 8182;
    this.startupTimeout = config.startupTimeout || 60000;
    this.downloadUrl = config.downloadUrl || 
      'https://github.com/platonai/Browser4/releases/latest/download/Browser4.jar';
    this.autoDownload = config.autoDownload !== false;
    this.javaPath = config.javaPath || 'java';
  }

  /**
   * Get the base URL for the server.
   */
  get baseUrl(): string {
    return `http://localhost:${this.port}`;
  }

  /**
   * Get the jar file path.
   */
  private get jarPath(): string {
    return path.join(this.homeDir, 'Browser4.jar');
  }

  /**
   * Check if the server is running.
   */
  get running(): boolean {
    return this.isRunning;
  }

  /**
   * Start the Browser4 server.
   */
  async start(): Promise<void> {
    if (this.isRunning) {
      return;
    }

    // Ensure home directory exists
    if (!fs.existsSync(this.homeDir)) {
      fs.mkdirSync(this.homeDir, { recursive: true });
    }

    // Download jar if needed
    if (!fs.existsSync(this.jarPath)) {
      if (!this.autoDownload) {
        throw new Error(`Browser4.jar not found at ${this.jarPath} and autoDownload is disabled`);
      }
      await this.downloadJar();
    }

    // Start the server
    this.process = spawn(this.javaPath, [
      '-jar',
      this.jarPath,
      `--server.port=${this.port}`
    ]);

    // Handle process events
    this.process.on('error', (error) => {
      console.error('Browser4 process error:', error);
      this.isRunning = false;
    });

    this.process.on('exit', (code) => {
      console.log(`Browser4 process exited with code ${code}`);
      this.isRunning = false;
    });

    // Wait for server to be ready
    await this.waitForServer();
    this.isRunning = true;
  }

  /**
   * Stop the Browser4 server.
   */
  async stop(): Promise<void> {
    if (!this.isRunning || !this.process) {
      return;
    }

    return new Promise((resolve) => {
      if (this.process) {
        this.process.on('exit', () => {
          this.isRunning = false;
          resolve();
        });
        this.process.kill();
      } else {
        resolve();
      }
    });
  }

  /**
   * Download the Browser4.jar file.
   */
  private async downloadJar(): Promise<void> {
    console.log(`Downloading Browser4.jar from ${this.downloadUrl}...`);

    return new Promise((resolve, reject) => {
      const file = fs.createWriteStream(this.jarPath);
      
      https.get(this.downloadUrl, (response) => {
        // Handle redirects
        if (response.statusCode === 301 || response.statusCode === 302) {
          const redirectUrl = response.headers.location;
          if (redirectUrl) {
            https.get(redirectUrl, (redirectResponse) => {
              redirectResponse.pipe(file);
              file.on('finish', () => {
                file.close();
                console.log('Download complete');
                resolve();
              });
            }).on('error', (err) => {
              fs.unlinkSync(this.jarPath);
              reject(err);
            });
          } else {
            reject(new Error('Redirect without location header'));
          }
          return;
        }

        response.pipe(file);
        file.on('finish', () => {
          file.close();
          console.log('Download complete');
          resolve();
        });
      }).on('error', (err) => {
        fs.unlinkSync(this.jarPath);
        reject(err);
      });
    });
  }

  /**
   * Wait for the server to be ready.
   */
  private async waitForServer(): Promise<void> {
    const startTime = Date.now();
    const checkInterval = 1000;

    while (Date.now() - startTime < this.startupTimeout) {
      try {
        const response = await axios.get(`${this.baseUrl}/actuator/health`, {
          timeout: 2000
        });
        if (response.status === 200) {
          return;
        }
      } catch (error) {
        // Server not ready yet, continue waiting
      }

      await new Promise(resolve => setTimeout(resolve, checkInterval));
    }

    throw new Error(`Server failed to start within ${this.startupTimeout}ms`);
  }

  /**
   * Use with async/await pattern (similar to context manager).
   */
  async use<T>(callback: (driver: Browser4Driver) => Promise<T>): Promise<T> {
    try {
      await this.start();
      return await callback(this);
    } finally {
      await this.stop();
    }
  }
}
