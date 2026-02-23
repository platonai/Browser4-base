/**
 * Data models for the NodeJS SDK.
 * 
 * These models correspond to the Kotlin data classes and provide a consistent
 * interface for working with Browser4 API responses.
 */

/**
 * Reference to a DOM element, matching WebDriver element identifier.
 */
export interface ElementRef {
  'element-6066-11e4-a52e-4f735466cecf': string;
}

/**
 * Represents a web page result from load/open operations.
 * Mirrors the Kotlin WebPage class.
 */
export interface WebPage {
  url: string;
  location?: string;
  contentType?: string;
  contentLength: number;
  protocolStatus?: string;
  isNil: boolean;
  html?: string;
}

/**
 * Normalized URL result.
 * Mirrors the Kotlin NormURL class.
 */
export interface NormURL {
  spec: string;
  url: string;
  args?: string;
  isNil: boolean;
}

/**
 * Result from agent run operation.
 */
export interface AgentRunResult {
  success: boolean;
  message: string;
  historySize: number;
  processTraceSize: number;
  finalResult?: any;
  trace?: string[];
}

/**
 * Result from agent act operation.
 */
export interface AgentActResult {
  success: boolean;
  message: string;
  action?: string;
  isComplete: boolean;
  expression?: string;
  result?: any;
  trace?: string[];
}

/**
 * Single observation result from agent observe operation.
 */
export interface ObserveResult {
  locator?: string;
  domain?: string;
  method?: string;
  arguments?: Record<string, any>;
  description?: string;
  screenshotContentSummary?: string;
  currentPageContentSummary?: string;
  nextGoal?: string;
  thinking?: string;
  summary?: string;
  keyFindings?: string;
  nextSuggestions?: string[];
}

/**
 * Result from agent observe operation.
 */
export interface AgentObservation {
  observations: ObserveResult[];
}

/**
 * Result from agent extract operation.
 */
export interface ExtractionResult {
  success: boolean;
  message: string;
  data?: any;
}

/**
 * Result of field extraction with CSS selectors.
 */
export interface FieldsExtraction {
  fields: Record<string, any>;
}

/**
 * Result of a tool call execution.
 * Mirrors the Kotlin ToolCallResult class.
 */
export interface ToolCallResult {
  success: boolean;
  message: string;
  data?: any;
}

/**
 * Description of an action to be performed.
 * Mirrors the Kotlin ActionDescription class.
 */
export interface ActionDescription {
  description: string;
  parameters?: Record<string, any>;
}

/**
 * Represents a single state in agent history.
 * Contains information about a step in the agent's execution.
 * Mirrors the Kotlin AgentState class.
 */
export interface AgentState {
  step: number;
  action?: string;
  result?: any;
  success: boolean;
  message: string;
}

/**
 * Agent history tracking execution states.
 * Provides memory of what actions have been performed.
 * Mirrors the Kotlin AgentHistory class.
 */
export interface AgentHistory {
  states: AgentState[];
  hasErrors: boolean;
  finalResult?: any;
}

/**
 * Chat response from the LLM.
 * Mirrors the Kotlin ChatResponse class.
 */
export interface ChatResponse {
  content: string;
  role: string;
  model?: string;
}

/**
 * Placeholder for page event handlers.
 * 
 * This class will be implemented in future tasks to support event-driven
 * page interactions similar to the Kotlin PageEventHandlers interface.
 */
export class PageEventHandlers {
  private _browseEventHandlers: Record<string, any> = {};
  private _loadEventHandlers: Record<string, any> = {};
  private _crawlEventHandlers: Record<string, any> = {};

  get browseEventHandlers(): Record<string, any> {
    return this._browseEventHandlers;
  }

  get loadEventHandlers(): Record<string, any> {
    return this._loadEventHandlers;
  }

  get crawlEventHandlers(): Record<string, any> {
    return this._crawlEventHandlers;
  }
}

// Helper functions for creating models from API responses

export function createWebPage(data: any): WebPage {
  return {
    url: data.url || '',
    location: data.location,
    contentType: data.contentType,
    contentLength: data.contentLength || 0,
    protocolStatus: data.protocolStatus,
    isNil: data.isNil || false,
    html: data.html
  };
}

export function createNormURL(data: any): NormURL {
  return {
    spec: data.spec || '',
    url: data.url || '',
    args: data.args,
    isNil: data.isNil || false
  };
}

export function createAgentRunResult(data: any): AgentRunResult {
  return {
    success: data.success || false,
    message: data.message || '',
    historySize: data.historySize || 0,
    processTraceSize: data.processTraceSize || 0,
    finalResult: data.finalResult,
    trace: data.trace
  };
}

export function createAgentActResult(data: any): AgentActResult {
  return {
    success: data.success || false,
    message: data.message || '',
    action: data.action,
    isComplete: data.isComplete || false,
    expression: data.expression,
    result: data.result,
    trace: data.trace
  };
}

export function createObserveResult(data: any): ObserveResult {
  return {
    locator: data.locator,
    domain: data.domain,
    method: data.method,
    arguments: data.arguments,
    description: data.description,
    screenshotContentSummary: data.screenshotContentSummary,
    currentPageContentSummary: data.currentPageContentSummary,
    nextGoal: data.nextGoal,
    thinking: data.thinking,
    summary: data.summary,
    keyFindings: data.keyFindings,
    nextSuggestions: data.nextSuggestions
  };
}

export function createAgentObservation(data: any): AgentObservation {
  if (Array.isArray(data)) {
    return {
      observations: data.map(item => typeof item === 'object' ? createObserveResult(item) : item)
    };
  }
  return { observations: [] };
}

export function createExtractionResult(data: any): ExtractionResult {
  return {
    success: data.success || false,
    message: data.message || '',
    data: data.data
  };
}

export function createAgentHistory(data: any): AgentHistory {
  const statesList = data.states || [];
  const states: AgentState[] = statesList.map((state: any) => ({
    step: state.step || 0,
    action: state.action,
    result: state.result,
    success: state.success || false,
    message: state.message || ''
  }));

  return {
    states,
    hasErrors: data.hasErrors || false,
    finalResult: data.finalResult
  };
}

export function createChatResponse(data: any): ChatResponse {
  if (typeof data === 'string') {
    return {
      content: data,
      role: 'assistant'
    };
  }
  
  if (typeof data === 'object' && data !== null) {
    return {
      content: data.content || '',
      role: data.role || 'assistant',
      model: data.model
    };
  }

  return {
    content: '',
    role: 'assistant'
  };
}
