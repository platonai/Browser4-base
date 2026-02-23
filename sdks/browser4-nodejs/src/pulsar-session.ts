/**
 * PulsarSession and AgenticSession classes for browser automation.
 * 
 * This module provides high-level session management for browser automation,
 * combining page loading, parsing, extraction, and AI-powered agent capabilities.
 */

import { PulsarClient } from './client';
import { WebDriver } from './webdriver';
import {
  WebPage,
  NormURL,
  FieldsExtraction,
  ChatResponse,
  createWebPage,
  createNormURL,
  createChatResponse
} from './models';

/**
 * PulsarSession provides methods for loading pages from storage or internet,
 * parsing them, and extracting data.
 * 
 * This class mirrors the Kotlin PulsarSession interface, providing a consistent
 * API across languages for web scraping and data extraction tasks.
 */
export class PulsarSession {
  protected client: PulsarClient;
  private _driver?: WebDriver;
  private _id: number = 0;

  constructor(client: PulsarClient) {
    this.client = client;
  }

  /**
   * Get the session ID (numeric).
   */
  get id(): number {
    return this._id;
  }

  /**
   * Get the session UUID.
   */
  get uuid(): string {
    return this.client.sessionId || '';
  }

  /**
   * Get a short descriptive display text.
   */
  get display(): string {
    return this.uuid 
      ? `PulsarSession(${this.uuid.substring(0, 8)}...)`
      : 'PulsarSession(no-session)';
  }

  /**
   * Check if the session is active.
   */
  get isActive(): boolean {
    return this.client.sessionId !== undefined;
  }

  /**
   * Get the bound WebDriver instance.
   */
  get driver(): WebDriver {
    if (!this._driver) {
      this._driver = new WebDriver(this.client);
    }
    return this._driver;
  }

  /**
   * Get or create the bound WebDriver instance.
   */
  getOrCreateBoundDriver(): WebDriver {
    return this.driver;
  }

  /**
   * Open a URL immediately (bypass cache).
   */
  async open(url: string, args?: string): Promise<WebPage> {
    const result = await this.client.post('/session/{sessionId}/open', {
      url,
      args: args || ''
    });
    return createWebPage(result);
  }

  /**
   * Load a URL from cache or fetch from internet.
   */
  async load(url: string, args?: string): Promise<WebPage> {
    const result = await this.client.post('/session/{sessionId}/load', {
      url,
      args: args || ''
    });
    return createWebPage(result);
  }

  /**
   * Submit URL to crawl pool for async processing.
   */
  async submit(url: string, args?: string): Promise<boolean> {
    return this.client.post('/session/{sessionId}/submit', {
      url,
      args: args || ''
    });
  }

  /**
   * Normalize a URL with load arguments.
   */
  async normalize(url: string, args?: string): Promise<NormURL> {
    const result = await this.client.post('/session/{sessionId}/normalize', {
      url,
      args: args || ''
    });
    return createNormURL(result);
  }

  /**
   * Parse a page into a document.
   */
  async parse(page: WebPage): Promise<any> {
    return this.client.post('/session/{sessionId}/parse', {
      url: page.url,
      html: page.html
    });
  }

  /**
   * Extract fields from a document using CSS selectors.
   */
  async extract(document: any, fields: Record<string, string>): Promise<FieldsExtraction> {
    const result = await this.client.post('/session/{sessionId}/extract', {
      document,
      fields
    });
    return { fields: result };
  }

  /**
   * Load, parse, and extract in one operation.
   */
  async scrape(url: string, fields: Record<string, string>, args?: string): Promise<FieldsExtraction> {
    const page = await this.load(url, args);
    const document = await this.parse(page);
    return this.extract(document, fields);
  }

  /**
   * Chat with the LLM.
   */
  async chat(userMessage: string, systemMessage?: string): Promise<ChatResponse> {
    const result = await this.client.post('/session/{sessionId}/chat', {
      userMessage,
      systemMessage
    });
    return createChatResponse(result);
  }

  /**
   * Close the session.
   */
  async close(): Promise<void> {
    if (this.client.sessionId) {
      await this.client.deleteSession();
    }
  }
}
