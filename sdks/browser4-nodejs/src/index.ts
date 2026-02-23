/**
 * NodeJS SDK for Browser4 AgenticSession and WebDriver-compatible API.
 * 
 * This SDK provides a NodeJS interface to the Browser4 browser automation platform,
 * enabling web scraping, data extraction, and AI-powered browser interaction.
 * 
 * Key classes:
 * - Browser4Driver: Automatic download and lifecycle management of Browser4 server
 * - PulsarClient: Low-level HTTP client for API communication
 * - PulsarSession: Session management for page loading and extraction
 * - AgenticSession: AI-powered browser automation (extends PulsarSession)
 * - WebDriver: Browser control and element interaction
 * 
 * Quick start:
 * ```typescript
 * import { Browser4Driver, PulsarClient, AgenticSession } from '@platonai/browser4-sdk';
 * 
 * // Start Browser4 server automatically
 * const driver = new Browser4Driver();
 * await driver.use(async (d) => {
 *   // Create client and session
 *   const client = new PulsarClient({ baseUrl: d.baseUrl });
 *   const sessionId = await client.createSession();
 *   const session = new AgenticSession(client);
 * 
 *   // Navigate and interact
 *   await session.driver.navigateTo('https://example.com');
 *   console.log(await session.driver.currentUrl());
 * 
 *   // Use AI-powered actions
 *   const result = await session.run('scroll to the bottom of the page');
 *   console.log(result.success);
 * 
 *   // Clean up
 *   await session.close();
 * });
 * ```
 */

// Export client
export { PulsarClient, PulsarClientConfig } from './client';

// Export driver
export { Browser4Driver, Browser4DriverConfig } from './driver';

// Export sessions
export { PulsarSession } from './pulsar-session';
export { AgenticSession } from './agentic-session';

// Export webdriver
export { WebDriver } from './webdriver';

// Export models
export {
  // Core models
  ElementRef,
  WebPage,
  NormURL,
  FieldsExtraction,
  
  // Agent models
  AgentRunResult,
  AgentActResult,
  ObserveResult,
  AgentObservation,
  ExtractionResult,
  ToolCallResult,
  ActionDescription,
  AgentState,
  AgentHistory,
  ChatResponse,
  
  // Events
  PageEventHandlers,
  
  // Helper functions
  createWebPage,
  createNormURL,
  createAgentRunResult,
  createAgentActResult,
  createObserveResult,
  createAgentObservation,
  createExtractionResult,
  createAgentHistory,
  createChatResponse
} from './models';
