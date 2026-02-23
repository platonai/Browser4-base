/**
 * Thin HTTP client over the Browser4 OpenAPI.
 */

import axios, { AxiosInstance, AxiosError } from 'axios';

export interface PulsarClientConfig {
  baseUrl?: string;
  timeout?: number;
  sessionId?: string;
  defaultHeaders?: Record<string, string>;
}

export class PulsarClient {
  private baseUrl: string;
  private timeout: number;
  private axiosInstance: AxiosInstance;
  public sessionId?: string;

  constructor(config: PulsarClientConfig = {}) {
    this.baseUrl = (config.baseUrl || 'http://localhost:8182').replace(/\/$/, '');
    this.timeout = config.timeout || 30000;
    this.sessionId = config.sessionId;

    const defaultHeaders = {
      'Content-Type': 'application/json',
      ...(config.defaultHeaders || {})
    };

    this.axiosInstance = axios.create({
      baseURL: this.baseUrl,
      timeout: this.timeout,
      headers: defaultHeaders
    });
  }

  private requireSession(sessionId?: string): string {
    const sid = sessionId || this.sessionId;
    if (!sid) {
      throw new Error('session_id is required; call createSession() first or pass session_id explicitly');
    }
    return sid;
  }

  private async request(
    method: string,
    path: string,
    options: {
      sessionId?: string;
      body?: any;
    } = {}
  ): Promise<any> {
    let finalPath = path;
    
    // Handle session ID in path
    if (path.includes('{sessionId}')) {
      const sid = this.requireSession(options.sessionId);
      finalPath = path.replace('{sessionId}', sid);
    }

    try {
      const response = await this.axiosInstance.request({
        method,
        url: finalPath,
        data: options.body
      });

      // WebDriver responses typically wrap in { value: ... }
      if (response.data && typeof response.data === 'object' && 'value' in response.data) {
        return response.data.value;
      }

      return response.data;
    } catch (error) {
      if (axios.isAxiosError(error)) {
        const axiosError = error as AxiosError;
        const response = axiosError.response;
        
        if (response) {
          const contentType = response.headers['content-type'] || '';
          let details = '';
          
          if (contentType.includes('application/json')) {
            try {
              details = JSON.stringify(response.data);
            } catch {
              details = String(response.data);
            }
          } else {
            details = String(response.data);
          }

          throw new Error(
            `HTTP ${response.status} (url=${axiosError.config?.url}, body=${details})`
          );
        }
      }
      throw error;
    }
  }

  async createSession(capabilities?: Record<string, any>): Promise<string> {
    const value = await this.request('POST', '/session', {
      body: { capabilities: capabilities || {} }
    });

    const sessionId = typeof value === 'object' && value.sessionId ? value.sessionId : null;
    if (!sessionId) {
      throw new Error('createSession response missing sessionId');
    }

    this.sessionId = sessionId;
    return sessionId;
  }

  async deleteSession(sessionId?: string): Promise<void> {
    const sid = this.requireSession(sessionId);
    await this.request('DELETE', `/session/${sid}`);
  }

  async post(path: string, body: any, sessionId?: string): Promise<any> {
    return this.request('POST', path, { sessionId, body });
  }

  async get(path: string, sessionId?: string): Promise<any> {
    return this.request('GET', path, { sessionId });
  }

  async delete(path: string, sessionId?: string): Promise<any> {
    return this.request('DELETE', path, { sessionId });
  }

  close(): void {
    // Axios doesn't need explicit cleanup, but we provide this for API consistency
  }
}
