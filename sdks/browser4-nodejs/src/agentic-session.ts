/**
 * AgenticSession extends PulsarSession with AI-powered agent capabilities.
 */

import { PulsarSession } from './pulsar-session';
import {
  AgentRunResult,
  AgentActResult,
  AgentObservation,
  ExtractionResult,
  AgentHistory,
  AgentState,
  createAgentRunResult,
  createAgentActResult,
  createAgentObservation,
  createExtractionResult,
  createAgentHistory
} from './models';

/**
 * AgenticSession provides AI-powered browser automation capabilities.
 * 
 * Extends PulsarSession with agent methods for autonomous task execution,
 * mirroring the Kotlin AgenticSession interface.
 */
export class AgenticSession extends PulsarSession {
  private _processTrace: string[] = [];
  private _stateHistory: AgentState[] = [];

  /**
   * Get the process trace.
   */
  get processTrace(): string[] {
    return [...this._processTrace];
  }

  /**
   * Get the state history.
   */
  get stateHistory(): AgentHistory {
    return {
      states: [...this._stateHistory],
      hasErrors: this._stateHistory.some(s => !s.success),
      finalResult: this._stateHistory.length > 0 
        ? this._stateHistory[this._stateHistory.length - 1].result 
        : undefined
    };
  }

  /**
   * Get the companion agent (returns self).
   */
  get companionAgent(): AgenticSession {
    return this;
  }

  /**
   * Execute a single action.
   */
  async act(action: string): Promise<AgentActResult> {
    const result = await this.client.post('/session/{sessionId}/agent/act', { action });
    const actResult = createAgentActResult(result);
    
    // Update state history
    this._stateHistory.push({
      step: this._stateHistory.length + 1,
      action,
      result: actResult.result,
      success: actResult.success,
      message: actResult.message
    });

    // Update process trace
    if (actResult.trace) {
      this._processTrace.push(...actResult.trace);
    }

    return actResult;
  }

  /**
   * Run an autonomous task.
   */
  async run(instruction: string): Promise<AgentRunResult> {
    return this.agentRun(instruction);
  }

  /**
   * Run an autonomous task (explicit method name).
   */
  async agentRun(instruction: string): Promise<AgentRunResult> {
    const result = await this.client.post('/session/{sessionId}/agent/run', { instruction });
    const runResult = createAgentRunResult(result);

    // Update state history
    this._stateHistory.push({
      step: this._stateHistory.length + 1,
      action: `run: ${instruction}`,
      result: runResult.finalResult,
      success: runResult.success,
      message: runResult.message
    });

    // Update process trace
    if (runResult.trace) {
      this._processTrace.push(...runResult.trace);
    }

    return runResult;
  }

  /**
   * Observe the page and get suggestions.
   */
  async observe(instruction?: string): Promise<AgentObservation> {
    const result = await this.client.post('/session/{sessionId}/agent/observe', {
      instruction: instruction || ''
    });
    return createAgentObservation(result);
  }

  /**
   * Summarize the page content.
   */
  async summarize(instruction?: string): Promise<string> {
    return this.client.post('/session/{sessionId}/agent/summarize', {
      instruction: instruction || ''
    });
  }

  /**
   * Extract data using AI.
   */
  async agentExtract(instruction: string, schema?: any): Promise<ExtractionResult> {
    const result = await this.client.post('/session/{sessionId}/agent/extract', {
      instruction,
      schema: schema || {}
    });
    return createExtractionResult(result);
  }

  /**
   * Clear agent history.
   */
  async clearHistory(): Promise<boolean> {
    const result = await this.client.post('/session/{sessionId}/agent/clearHistory', {});
    this._processTrace = [];
    this._stateHistory = [];
    return result === true;
  }

  /**
   * Get agent history from server.
   */
  async getHistory(): Promise<AgentHistory> {
    const result = await this.client.get('/session/{sessionId}/agent/history');
    return createAgentHistory(result);
  }
}
