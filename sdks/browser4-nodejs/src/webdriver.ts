/**
 * WebDriver class for browser control and element interaction.
 * Provides a subset of W3C WebDriver API tailored for Browser4.
 */

import { PulsarClient } from './client';
import { ElementRef } from './models';

export class WebDriver {
  private client: PulsarClient;
  private _navigateHistory: string[] = [];

  constructor(client: PulsarClient) {
    this.client = client;
  }

  /**
   * Get navigation history.
   */
  get navigateHistory(): string[] {
    return [...this._navigateHistory];
  }

  /**
   * Navigate to a URL.
   */
  async navigateTo(url: string): Promise<void> {
    await this.client.post('/session/{sessionId}/url', { url });
    this._navigateHistory.push(url);
  }

  /**
   * Get the current URL.
   */
  async currentUrl(): Promise<string> {
    return this.client.get('/session/{sessionId}/url');
  }

  /**
   * Navigate back in browser history.
   */
  async back(): Promise<void> {
    await this.client.post('/session/{sessionId}/back', {});
  }

  /**
   * Navigate forward in browser history.
   */
  async forward(): Promise<void> {
    await this.client.post('/session/{sessionId}/forward', {});
  }

  /**
   * Refresh the current page.
   */
  async refresh(): Promise<void> {
    await this.client.post('/session/{sessionId}/refresh', {});
  }

  /**
   * Get the page title.
   */
  async title(): Promise<string> {
    return this.client.get('/session/{sessionId}/title');
  }

  /**
   * Find an element using a CSS selector.
   */
  async findElement(selector: string): Promise<ElementRef> {
    return this.client.post('/session/{sessionId}/element', {
      using: 'css selector',
      value: selector
    });
  }

  /**
   * Find multiple elements using a CSS selector.
   */
  async findElements(selector: string): Promise<ElementRef[]> {
    return this.client.post('/session/{sessionId}/elements', {
      using: 'css selector',
      value: selector
    });
  }

  /**
   * Click an element.
   */
  async click(selector: string): Promise<void> {
    const element = await this.findElement(selector);
    await this.client.post('/session/{sessionId}/element/click', {
      elementId: this.getElementId(element)
    });
  }

  /**
   * Fill an input element with text.
   */
  async fill(selector: string, text: string): Promise<void> {
    const element = await this.findElement(selector);
    await this.client.post('/session/{sessionId}/element/value', {
      elementId: this.getElementId(element),
      text
    });
  }

  /**
   * Press a key on an element.
   */
  async press(selector: string, key: string): Promise<void> {
    const element = await this.findElement(selector);
    await this.client.post('/session/{sessionId}/element/press', {
      elementId: this.getElementId(element),
      key
    });
  }

  /**
   * Type text into an element.
   */
  async type(selector: string, text: string): Promise<void> {
    await this.fill(selector, text);
  }

  /**
   * Get the text content of an element.
   */
  async getText(selector: string): Promise<string> {
    const element = await this.findElement(selector);
    return this.client.get(`/session/{sessionId}/element/${this.getElementId(element)}/text`);
  }

  /**
   * Get an attribute value from an element.
   */
  async getAttribute(selector: string, attributeName: string): Promise<string | null> {
    const element = await this.findElement(selector);
    return this.client.get(
      `/session/{sessionId}/element/${this.getElementId(element)}/attribute/${attributeName}`
    );
  }

  /**
   * Check if an element exists.
   */
  async exists(selector: string): Promise<boolean> {
    const result = await this.client.post('/session/{sessionId}/selectors/exists', { selector });
    return result?.exists || false;
  }

  /**
   * Wait for a selector to appear.
   */
  async waitForSelector(selector: string, timeout: number = 30000): Promise<boolean> {
    const result = await this.client.post('/session/{sessionId}/selectors/waitFor', {
      selector,
      timeout
    });
    return result?.exists || false;
  }

  /**
   * Execute JavaScript in the browser.
   */
  async executeScript(script: string, args: any[] = []): Promise<any> {
    return this.client.post('/session/{sessionId}/execute/sync', {
      script,
      args
    });
  }

  /**
   * Take a screenshot.
   */
  async screenshot(): Promise<string> {
    return this.client.get('/session/{sessionId}/screenshot');
  }

  /**
   * Scroll to an element.
   */
  async scrollTo(selector: string): Promise<void> {
    await this.client.post('/session/{sessionId}/element/scrollTo', { selector });
  }

  /**
   * Hover over an element.
   */
  async hover(selector: string): Promise<void> {
    await this.client.post('/session/{sessionId}/element/hover', { selector });
  }

  /**
   * Select an option in a select element.
   */
  async select(selector: string, value: string): Promise<void> {
    await this.client.post('/session/{sessionId}/element/select', {
      selector,
      value
    });
  }

  /**
   * Delay execution for a specified time.
   */
  async delay(ms: number): Promise<void> {
    await this.client.post('/session/{sessionId}/control/delay', { ms });
  }

  /**
   * Extract the element ID from an ElementRef.
   */
  private getElementId(element: ElementRef): string {
    return element['element-6066-11e4-a52e-4f735466cecf'];
  }
}
