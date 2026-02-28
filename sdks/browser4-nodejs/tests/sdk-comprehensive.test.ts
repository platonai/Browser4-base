/**
 * Comprehensive tests for the Browser4 NodeJS SDK.
 *
 * Covers all SDK methods with various scenarios, edge cases, and error handling.
 * Uses mock responses so no running Browser4 server is required.
 */

import {
  PulsarClient,
  PulsarSession,
  AgenticSession,
  WebDriver,
  Browser4Driver,
  PageEventHandlers,
  createWebPage,
  createNormURL,
  createAgentRunResult,
  createAgentActResult,
  createObserveResult,
  createAgentObservation,
  createExtractionResult,
  createAgentHistory,
  createChatResponse,
  WebPage,
  ElementRef
} from '../src';

// ---------------------------------------------------------------------------
// Axios mock setup
// ---------------------------------------------------------------------------

jest.mock('axios');
import axios from 'axios';
const mockedAxios = axios as jest.Mocked<typeof axios>;

/** Build a fresh mock Axios instance and configure `axios.create` to return it. */
function buildMockAxios() {
  const instance = { request: jest.fn() };
  mockedAxios.create.mockReturnValue(instance as any);
  return instance;
}

/** Create a mock response whose `data.value` equals `value`. */
function valueResponse(value: any) {
  return { data: { value } };
}

/** Create a mock response whose `data` equals `raw` (no `.value` wrapper). */
function rawResponse(raw: any) {
  return { data: raw };
}

// ===========================================================================
// PulsarClient – thorough coverage
// ===========================================================================

describe('PulsarClient', () => {
  let mockAxios: ReturnType<typeof buildMockAxios>;
  let client: PulsarClient;

  beforeEach(() => {
    mockAxios = buildMockAxios();
    client = new PulsarClient({ baseUrl: 'http://localhost:8182' });
  });

  // -------------------------------------------------------------------------
  // Constructor / configuration
  // -------------------------------------------------------------------------

  describe('constructor', () => {
    it('should use default baseUrl when none provided', () => {
      const c = new PulsarClient();
      // Internal; verify through behaviour – no error on instantiation
      expect(c).toBeDefined();
    });

    it('should strip trailing slash from baseUrl', () => {
      expect(() => new PulsarClient({ baseUrl: 'http://localhost:8182/' })).not.toThrow();
    });

    it('should accept custom timeout and sessionId', () => {
      const c = new PulsarClient({ timeout: 5000, sessionId: 'pre-set' });
      expect(c.sessionId).toBe('pre-set');
    });

    it('should accept custom defaultHeaders', () => {
      expect(
        () => new PulsarClient({ defaultHeaders: { 'X-Custom': 'value' } })
      ).not.toThrow();
    });
  });

  // -------------------------------------------------------------------------
  // createSession
  // -------------------------------------------------------------------------

  describe('createSession', () => {
    it('should create a session and store the sessionId', async () => {
      mockAxios.request.mockResolvedValue(valueResponse({ sessionId: 'abc-123' }));

      const id = await client.createSession();

      expect(id).toBe('abc-123');
      expect(client.sessionId).toBe('abc-123');
    });

    it('should POST to /session', async () => {
      mockAxios.request.mockResolvedValue(valueResponse({ sessionId: 'sid' }));
      await client.createSession();

      expect(mockAxios.request).toHaveBeenCalledWith(
        expect.objectContaining({ method: 'POST', url: '/session' })
      );
    });

    it('should pass empty capabilities when none provided', async () => {
      mockAxios.request.mockResolvedValue(valueResponse({ sessionId: 'sid' }));
      await client.createSession();

      expect(mockAxios.request).toHaveBeenCalledWith(
        expect.objectContaining({ data: { capabilities: {} } })
      );
    });

    it('should pass provided capabilities', async () => {
      mockAxios.request.mockResolvedValue(valueResponse({ sessionId: 'sid' }));
      await client.createSession({ browserName: 'chrome', headless: true });

      expect(mockAxios.request).toHaveBeenCalledWith(
        expect.objectContaining({
          data: { capabilities: { browserName: 'chrome', headless: true } }
        })
      );
    });

    it('should throw when sessionId is missing in response', async () => {
      mockAxios.request.mockResolvedValue(valueResponse({}));
      await expect(client.createSession()).rejects.toThrow(
        'createSession response missing sessionId'
      );
    });

    it('should throw when response value is a non-object', async () => {
      mockAxios.request.mockResolvedValue(valueResponse(null));
      await expect(client.createSession()).rejects.toThrow(
        'createSession response missing sessionId'
      );
    });
  });

  // -------------------------------------------------------------------------
  // deleteSession
  // -------------------------------------------------------------------------

  describe('deleteSession', () => {
    it('should DELETE /session/{id}', async () => {
      client.sessionId = 'sid-1';
      mockAxios.request.mockResolvedValue(rawResponse({}));

      await client.deleteSession();

      expect(mockAxios.request).toHaveBeenCalledWith(
        expect.objectContaining({ method: 'DELETE', url: '/session/sid-1' })
      );
    });

    it('should accept an explicit sessionId override', async () => {
      mockAxios.request.mockResolvedValue(rawResponse({}));

      await client.deleteSession('explicit-id');

      expect(mockAxios.request).toHaveBeenCalledWith(
        expect.objectContaining({ url: '/session/explicit-id' })
      );
    });

    it('should throw when no session is set', async () => {
      await expect(client.deleteSession()).rejects.toThrow('session_id is required');
    });
  });

  // -------------------------------------------------------------------------
  // post / get / delete helpers
  // -------------------------------------------------------------------------

  describe('post', () => {
    it('should make a POST request and unwrap .value', async () => {
      client.sessionId = 'sid';
      mockAxios.request.mockResolvedValue(valueResponse({ ok: true }));

      const result = await client.post('/test', { key: 'val' });

      expect(result).toEqual({ ok: true });
      expect(mockAxios.request).toHaveBeenCalledWith(
        expect.objectContaining({ method: 'POST', url: '/test', data: { key: 'val' } })
      );
    });

    it('should return raw data when .value is absent', async () => {
      client.sessionId = 'sid';
      mockAxios.request.mockResolvedValue(rawResponse({ raw: 'data' }));

      const result = await client.post('/test', {});

      expect(result).toEqual({ raw: 'data' });
    });
  });

  describe('get', () => {
    it('should make a GET request and unwrap .value', async () => {
      client.sessionId = 'sid';
      mockAxios.request.mockResolvedValue(valueResponse('hello'));

      const result = await client.get('/test');

      expect(result).toBe('hello');
      expect(mockAxios.request).toHaveBeenCalledWith(
        expect.objectContaining({ method: 'GET', url: '/test' })
      );
    });

    it('should return raw data when .value is absent', async () => {
      client.sessionId = 'sid';
      mockAxios.request.mockResolvedValue(rawResponse('raw'));

      const result = await client.get('/test');

      expect(result).toBe('raw');
    });
  });

  describe('delete', () => {
    it('should make a DELETE request', async () => {
      client.sessionId = 'sid';
      mockAxios.request.mockResolvedValue(rawResponse({}));

      await client.delete('/test');

      expect(mockAxios.request).toHaveBeenCalledWith(
        expect.objectContaining({ method: 'DELETE', url: '/test' })
      );
    });
  });

  // -------------------------------------------------------------------------
  // HTTP error handling
  // -------------------------------------------------------------------------

  describe('error handling', () => {
    it('should wrap axios 4xx errors with status and URL', async () => {
      client.sessionId = 'sid';
      const axiosErr = Object.assign(new Error('Not Found'), {
        isAxiosError: true,
        response: {
          status: 404,
          headers: { 'content-type': 'application/json' },
          data: { error: 'not found' }
        },
        config: { url: '/session/sid/load' }
      });
      mockedAxios.isAxiosError.mockReturnValue(true);
      mockAxios.request.mockRejectedValue(axiosErr);

      await expect(client.get('/session/sid/load')).rejects.toThrow('HTTP 404');
    });

    it('should re-throw non-axios errors as-is', async () => {
      client.sessionId = 'sid';
      const err = new TypeError('network failure');
      mockedAxios.isAxiosError.mockReturnValue(false);
      mockAxios.request.mockRejectedValue(err);

      await expect(client.get('/test')).rejects.toThrow('network failure');
    });

    it('should handle axios error with plain-text response body', async () => {
      client.sessionId = 'sid';
      const axiosErr = Object.assign(new Error('Server Error'), {
        isAxiosError: true,
        response: {
          status: 500,
          headers: { 'content-type': 'text/plain' },
          data: 'Internal Server Error'
        },
        config: { url: '/test' }
      });
      mockedAxios.isAxiosError.mockReturnValue(true);
      mockAxios.request.mockRejectedValue(axiosErr);

      await expect(client.post('/test', {})).rejects.toThrow('HTTP 500');
    });
  });

  // -------------------------------------------------------------------------
  // close
  // -------------------------------------------------------------------------

  describe('close', () => {
    it('should be a no-op and not throw', () => {
      expect(() => client.close()).not.toThrow();
    });
  });
});

// ===========================================================================
// PulsarSession
// ===========================================================================

describe('PulsarSession', () => {
  let mockAxios: ReturnType<typeof buildMockAxios>;
  let client: PulsarClient;
  let session: PulsarSession;

  beforeEach(() => {
    mockAxios = buildMockAxios();
    client = new PulsarClient();
    client.sessionId = 'sess-123';
    session = new PulsarSession(client);
  });

  // -------------------------------------------------------------------------
  // Properties
  // -------------------------------------------------------------------------

  describe('properties', () => {
    it('id defaults to 0', () => {
      expect(session.id).toBe(0);
    });

    it('uuid returns the client sessionId', () => {
      expect(session.uuid).toBe('sess-123');
    });

    it('uuid returns empty string when client has no session', () => {
      client.sessionId = undefined;
      expect(session.uuid).toBe('');
    });

    it('display contains PulsarSession and truncated uuid', () => {
      expect(session.display).toMatch(/PulsarSession/);
      expect(session.display).toMatch(/sess-12/);
    });

    it('display indicates no session when sessionId is absent', () => {
      client.sessionId = undefined;
      expect(session.display).toContain('no-session');
    });

    it('isActive is true when sessionId is set', () => {
      expect(session.isActive).toBe(true);
    });

    it('isActive is false when sessionId is absent', () => {
      client.sessionId = undefined;
      expect(session.isActive).toBe(false);
    });
  });

  // -------------------------------------------------------------------------
  // driver / getOrCreateBoundDriver
  // -------------------------------------------------------------------------

  describe('driver', () => {
    it('should create a WebDriver lazily', () => {
      expect(session.driver).toBeInstanceOf(WebDriver);
    });

    it('should return the same instance on repeated access', () => {
      expect(session.driver).toBe(session.driver);
    });

    it('getOrCreateBoundDriver returns same instance as driver getter', () => {
      expect(session.getOrCreateBoundDriver()).toBe(session.driver);
    });
  });

  // -------------------------------------------------------------------------
  // open
  // -------------------------------------------------------------------------

  describe('open', () => {
    it('should POST to /session/{sessionId}/open', async () => {
      mockAxios.request.mockResolvedValue(
        valueResponse({ url: 'https://example.com', isNil: false, contentLength: 500 })
      );

      const page = await session.open('https://example.com');

      expect(mockAxios.request).toHaveBeenCalledWith(
        expect.objectContaining({ url: '/session/sess-123/open' })
      );
      expect(page.url).toBe('https://example.com');
      expect(page.isNil).toBe(false);
    });

    it('should pass args to the request', async () => {
      mockAxios.request.mockResolvedValue(
        valueResponse({ url: 'https://example.com', isNil: false, contentLength: 0 })
      );

      await session.open('https://example.com', '-refresh');

      expect(mockAxios.request).toHaveBeenCalledWith(
        expect.objectContaining({ data: { url: 'https://example.com', args: '-refresh' } })
      );
    });

    it('should send empty string for args when not provided', async () => {
      mockAxios.request.mockResolvedValue(
        valueResponse({ url: 'https://x.com', isNil: false, contentLength: 0 })
      );

      await session.open('https://x.com');

      expect(mockAxios.request).toHaveBeenCalledWith(
        expect.objectContaining({ data: { url: 'https://x.com', args: '' } })
      );
    });
  });

  // -------------------------------------------------------------------------
  // load
  // -------------------------------------------------------------------------

  describe('load', () => {
    it('should POST to /session/{sessionId}/load', async () => {
      mockAxios.request.mockResolvedValue(
        valueResponse({ url: 'https://example.com', isNil: false, contentLength: 1024 })
      );

      const page = await session.load('https://example.com', '-expire 1d');

      expect(mockAxios.request).toHaveBeenCalledWith(
        expect.objectContaining({ url: '/session/sess-123/load' })
      );
      expect(page.contentLength).toBe(1024);
    });

    it('should return a WebPage with isNil true for missing pages', async () => {
      mockAxios.request.mockResolvedValue(
        valueResponse({ url: 'https://missing.com', isNil: true, contentLength: 0 })
      );

      const page = await session.load('https://missing.com');
      expect(page.isNil).toBe(true);
    });
  });

  // -------------------------------------------------------------------------
  // submit
  // -------------------------------------------------------------------------

  describe('submit', () => {
    it('should POST to /session/{sessionId}/submit and return true', async () => {
      mockAxios.request.mockResolvedValue(valueResponse(true));

      const result = await session.submit('https://example.com');

      expect(result).toBe(true);
      expect(mockAxios.request).toHaveBeenCalledWith(
        expect.objectContaining({ url: '/session/sess-123/submit' })
      );
    });

    it('should forward args when provided', async () => {
      mockAxios.request.mockResolvedValue(valueResponse(true));

      await session.submit('https://example.com', '-parse');

      expect(mockAxios.request).toHaveBeenCalledWith(
        expect.objectContaining({ data: { url: 'https://example.com', args: '-parse' } })
      );
    });
  });

  // -------------------------------------------------------------------------
  // normalize
  // -------------------------------------------------------------------------

  describe('normalize', () => {
    it('should POST to /session/{sessionId}/normalize', async () => {
      mockAxios.request.mockResolvedValue(
        valueResponse({ spec: 'https://example.com -expire 1d', url: 'https://example.com', args: '-expire 1d', isNil: false })
      );

      const norm = await session.normalize('https://example.com', '-expire 1d');

      expect(norm.url).toBe('https://example.com');
      expect(norm.args).toBe('-expire 1d');
      expect(norm.isNil).toBe(false);
    });

    it('should return a nil NormURL when server indicates nil', async () => {
      mockAxios.request.mockResolvedValue(
        valueResponse({ spec: '', url: '', isNil: true })
      );

      const norm = await session.normalize('bad-url');
      expect(norm.isNil).toBe(true);
    });
  });

  // -------------------------------------------------------------------------
  // parse
  // -------------------------------------------------------------------------

  describe('parse', () => {
    it('should POST to /session/{sessionId}/parse with url and html', async () => {
      const page: WebPage = {
        url: 'https://example.com',
        html: '<html><body>Hi</body></html>',
        isNil: false,
        contentLength: 30
      };
      mockAxios.request.mockResolvedValue(valueResponse({ docId: 'doc-1' }));

      const doc = await session.parse(page);

      expect(doc).toEqual({ docId: 'doc-1' });
      expect(mockAxios.request).toHaveBeenCalledWith(
        expect.objectContaining({
          url: '/session/sess-123/parse',
          data: { url: 'https://example.com', html: page.html }
        })
      );
    });
  });

  // -------------------------------------------------------------------------
  // extract
  // -------------------------------------------------------------------------

  describe('extract', () => {
    it('should POST to /session/{sessionId}/extract and wrap in FieldsExtraction', async () => {
      mockAxios.request.mockResolvedValue(valueResponse({ title: 'Hello', price: '9.99' }));

      const result = await session.extract({}, { title: 'h1', price: '.price' });

      expect(result.fields).toEqual({ title: 'Hello', price: '9.99' });
    });
  });

  // -------------------------------------------------------------------------
  // scrape (composite)
  // -------------------------------------------------------------------------

  describe('scrape', () => {
    it('should chain load → parse → extract', async () => {
      // load
      mockAxios.request.mockResolvedValueOnce(
        valueResponse({ url: 'https://example.com', isNil: false, contentLength: 100 })
      );
      // parse
      mockAxios.request.mockResolvedValueOnce(valueResponse({ docId: 'doc-1' }));
      // extract
      mockAxios.request.mockResolvedValueOnce(valueResponse({ name: 'Widget' }));

      const result = await session.scrape('https://example.com', { name: 'h1' });

      expect(result.fields).toEqual({ name: 'Widget' });
    });
  });

  // -------------------------------------------------------------------------
  // chat
  // -------------------------------------------------------------------------

  describe('chat', () => {
    it('should POST to /session/{sessionId}/chat', async () => {
      mockAxios.request.mockResolvedValue(
        valueResponse({ content: 'Sure!', role: 'assistant', model: 'gpt-4' })
      );

      const resp = await session.chat('Hello');

      expect(resp.content).toBe('Sure!');
      expect(resp.role).toBe('assistant');
      expect(resp.model).toBe('gpt-4');
    });

    it('should include systemMessage when provided', async () => {
      mockAxios.request.mockResolvedValue(
        valueResponse({ content: 'ok', role: 'assistant' })
      );

      await session.chat('user msg', 'system context');

      expect(mockAxios.request).toHaveBeenCalledWith(
        expect.objectContaining({
          data: { userMessage: 'user msg', systemMessage: 'system context' }
        })
      );
    });

    it('should handle a plain-string response from server', async () => {
      mockAxios.request.mockResolvedValue(valueResponse('plain string response'));

      const resp = await session.chat('hi');

      expect(resp.content).toBe('plain string response');
      expect(resp.role).toBe('assistant');
    });
  });

  // -------------------------------------------------------------------------
  // close
  // -------------------------------------------------------------------------

  describe('close', () => {
    it('should delete the session when sessionId exists', async () => {
      mockAxios.request.mockResolvedValue(rawResponse({}));

      await session.close();

      expect(mockAxios.request).toHaveBeenCalledWith(
        expect.objectContaining({ method: 'DELETE', url: '/session/sess-123' })
      );
    });

    it('should not call deleteSession when no sessionId', async () => {
      client.sessionId = undefined;
      await session.close();
      expect(mockAxios.request).not.toHaveBeenCalled();
    });
  });
});

// ===========================================================================
// AgenticSession
// ===========================================================================

describe('AgenticSession', () => {
  let mockAxios: ReturnType<typeof buildMockAxios>;
  let client: PulsarClient;
  let session: AgenticSession;

  beforeEach(() => {
    mockAxios = buildMockAxios();
    client = new PulsarClient();
    client.sessionId = 'agent-sess';
    session = new AgenticSession(client);
  });

  // -------------------------------------------------------------------------
  // Initial state
  // -------------------------------------------------------------------------

  describe('initial state', () => {
    it('processTrace is empty', () => {
      expect(session.processTrace).toEqual([]);
    });

    it('stateHistory has empty states and no errors', () => {
      expect(session.stateHistory.states).toEqual([]);
      expect(session.stateHistory.hasErrors).toBe(false);
      expect(session.stateHistory.finalResult).toBeUndefined();
    });

    it('companionAgent returns self', () => {
      expect(session.companionAgent).toBe(session);
    });
  });

  // -------------------------------------------------------------------------
  // act
  // -------------------------------------------------------------------------

  describe('act', () => {
    it('should POST to /session/{sessionId}/agent/act', async () => {
      mockAxios.request.mockResolvedValue(
        valueResponse({ success: true, message: 'done', isComplete: true })
      );

      await session.act('click button');

      expect(mockAxios.request).toHaveBeenCalledWith(
        expect.objectContaining({
          url: '/session/agent-sess/agent/act',
          data: { action: 'click button' }
        })
      );
    });

    it('should add a state to stateHistory on success', async () => {
      mockAxios.request.mockResolvedValue(
        valueResponse({ success: true, message: 'ok', isComplete: true })
      );

      await session.act('scroll down');

      expect(session.stateHistory.states).toHaveLength(1);
      expect(session.stateHistory.states[0].action).toBe('scroll down');
      expect(session.stateHistory.states[0].success).toBe(true);
      expect(session.stateHistory.states[0].step).toBe(1);
    });

    it('should accumulate states across multiple acts', async () => {
      mockAxios.request.mockResolvedValue(
        valueResponse({ success: true, message: 'ok', isComplete: true })
      );

      await session.act('action 1');
      await session.act('action 2');

      const history = session.stateHistory;
      expect(history.states).toHaveLength(2);
      expect(history.states[0].step).toBe(1);
      expect(history.states[1].step).toBe(2);
    });

    it('should mark stateHistory.hasErrors when an action fails', async () => {
      mockAxios.request.mockResolvedValue(
        valueResponse({ success: false, message: 'error', isComplete: false })
      );

      await session.act('bad action');

      expect(session.stateHistory.hasErrors).toBe(true);
    });

    it('should append trace items to processTrace', async () => {
      mockAxios.request.mockResolvedValue(
        valueResponse({
          success: true,
          message: 'ok',
          isComplete: true,
          trace: ['t1', 't2']
        })
      );

      await session.act('act with trace');

      expect(session.processTrace).toContain('t1');
      expect(session.processTrace).toContain('t2');
    });

    it('should not change processTrace when trace is absent', async () => {
      mockAxios.request.mockResolvedValue(
        valueResponse({ success: true, message: 'ok', isComplete: true })
      );

      await session.act('no trace');

      expect(session.processTrace).toHaveLength(0);
    });

    it('should return the full AgentActResult', async () => {
      mockAxios.request.mockResolvedValue(
        valueResponse({
          success: true,
          message: 'Clicked',
          action: 'click',
          isComplete: true,
          expression: 'btn.click()',
          result: { clicked: true }
        })
      );

      const result = await session.act('click btn');

      expect(result.action).toBe('click');
      expect(result.expression).toBe('btn.click()');
      expect(result.result).toEqual({ clicked: true });
    });
  });

  // -------------------------------------------------------------------------
  // run / agentRun
  // -------------------------------------------------------------------------

  describe('run / agentRun', () => {
    it('run should delegate to agentRun', async () => {
      mockAxios.request.mockResolvedValue(
        valueResponse({ success: true, message: 'done', historySize: 1, processTraceSize: 0 })
      );

      const result = await session.run('do task');

      expect(result.success).toBe(true);
    });

    it('agentRun should POST to /session/{sessionId}/agent/run', async () => {
      mockAxios.request.mockResolvedValue(
        valueResponse({ success: true, message: 'done', historySize: 2, processTraceSize: 1 })
      );

      await session.agentRun('do explicit task');

      expect(mockAxios.request).toHaveBeenCalledWith(
        expect.objectContaining({
          url: '/session/agent-sess/agent/run',
          data: { instruction: 'do explicit task' }
        })
      );
    });

    it('should add a state to stateHistory after run', async () => {
      mockAxios.request.mockResolvedValue(
        valueResponse({ success: true, message: 'finished', historySize: 1, processTraceSize: 0 })
      );

      await session.run('complete workflow');

      expect(session.stateHistory.states).toHaveLength(1);
      expect(session.stateHistory.states[0].action).toContain('complete workflow');
    });

    it('should populate processTrace from run trace', async () => {
      mockAxios.request.mockResolvedValue(
        valueResponse({
          success: true,
          message: 'done',
          historySize: 1,
          processTraceSize: 2,
          trace: ['step-a', 'step-b']
        })
      );

      await session.run('task');

      expect(session.processTrace).toContain('step-a');
      expect(session.processTrace).toContain('step-b');
    });

    it('should populate finalResult in stateHistory', async () => {
      mockAxios.request.mockResolvedValue(
        valueResponse({
          success: true,
          message: 'done',
          historySize: 1,
          processTraceSize: 0,
          finalResult: { answer: 42 }
        })
      );

      await session.run('task');

      expect(session.stateHistory.finalResult).toEqual({ answer: 42 });
    });
  });

  // -------------------------------------------------------------------------
  // observe
  // -------------------------------------------------------------------------

  describe('observe', () => {
    it('should POST to /session/{sessionId}/agent/observe', async () => {
      mockAxios.request.mockResolvedValue(
        valueResponse([{ locator: '0,1', method: 'click', description: 'Click link' }])
      );

      const obs = await session.observe('what can I do?');

      expect(mockAxios.request).toHaveBeenCalledWith(
        expect.objectContaining({
          url: '/session/agent-sess/agent/observe',
          data: { instruction: 'what can I do?' }
        })
      );
      expect(obs.observations).toHaveLength(1);
      expect(obs.observations[0].method).toBe('click');
    });

    it('should use empty string when no instruction provided', async () => {
      mockAxios.request.mockResolvedValue(valueResponse([]));

      await session.observe();

      expect(mockAxios.request).toHaveBeenCalledWith(
        expect.objectContaining({ data: { instruction: '' } })
      );
    });

    it('should return empty observations when server returns empty array', async () => {
      mockAxios.request.mockResolvedValue(valueResponse([]));

      const obs = await session.observe();
      expect(obs.observations).toHaveLength(0);
    });

    it('should return empty observations when server returns non-array', async () => {
      mockAxios.request.mockResolvedValue(valueResponse(null));

      const obs = await session.observe();
      expect(obs.observations).toHaveLength(0);
    });
  });

  // -------------------------------------------------------------------------
  // summarize
  // -------------------------------------------------------------------------

  describe('summarize', () => {
    it('should POST to /session/{sessionId}/agent/summarize', async () => {
      mockAxios.request.mockResolvedValue(valueResponse('Page contains products'));

      const summary = await session.summarize('Summarize products');

      expect(mockAxios.request).toHaveBeenCalledWith(
        expect.objectContaining({ url: '/session/agent-sess/agent/summarize' })
      );
      expect(summary).toBe('Page contains products');
    });

    it('should use empty string when no instruction provided', async () => {
      mockAxios.request.mockResolvedValue(valueResponse('Summary'));

      await session.summarize();

      expect(mockAxios.request).toHaveBeenCalledWith(
        expect.objectContaining({ data: { instruction: '' } })
      );
    });
  });

  // -------------------------------------------------------------------------
  // agentExtract
  // -------------------------------------------------------------------------

  describe('agentExtract', () => {
    it('should POST to /session/{sessionId}/agent/extract', async () => {
      mockAxios.request.mockResolvedValue(
        valueResponse({ success: true, message: 'Extracted', data: [{ name: 'Product A' }] })
      );

      const result = await session.agentExtract('List product names', { type: 'array' });

      expect(mockAxios.request).toHaveBeenCalledWith(
        expect.objectContaining({
          url: '/session/agent-sess/agent/extract',
          data: { instruction: 'List product names', schema: { type: 'array' } }
        })
      );
      expect(result.success).toBe(true);
      expect(result.data).toEqual([{ name: 'Product A' }]);
    });

    it('should use empty object schema when not provided', async () => {
      mockAxios.request.mockResolvedValue(
        valueResponse({ success: true, message: 'ok', data: {} })
      );

      await session.agentExtract('extract title');

      expect(mockAxios.request).toHaveBeenCalledWith(
        expect.objectContaining({ data: { instruction: 'extract title', schema: {} } })
      );
    });

    it('should return failure result on extraction error', async () => {
      mockAxios.request.mockResolvedValue(
        valueResponse({ success: false, message: 'Could not extract' })
      );

      const result = await session.agentExtract('extract nonexistent');
      expect(result.success).toBe(false);
      expect(result.message).toBe('Could not extract');
    });
  });

  // -------------------------------------------------------------------------
  // clearHistory
  // -------------------------------------------------------------------------

  describe('clearHistory', () => {
    it('should POST to /session/{sessionId}/agent/clearHistory', async () => {
      // prime some state
      mockAxios.request.mockResolvedValueOnce(
        valueResponse({ success: true, message: 'ok', isComplete: true, trace: ['x'] })
      );
      await session.act('something');

      mockAxios.request.mockResolvedValueOnce(valueResponse(true));

      const cleared = await session.clearHistory();

      expect(cleared).toBe(true);
      expect(session.processTrace).toHaveLength(0);
      expect(session.stateHistory.states).toHaveLength(0);
    });

    it('should return false when server returns false', async () => {
      mockAxios.request.mockResolvedValue(valueResponse(false));

      const result = await session.clearHistory();
      expect(result).toBe(false);
    });
  });

  // -------------------------------------------------------------------------
  // getHistory
  // -------------------------------------------------------------------------

  describe('getHistory', () => {
    it('should GET /session/{sessionId}/agent/history', async () => {
      mockAxios.request.mockResolvedValue(
        valueResponse({
          states: [{ step: 1, action: 'click', success: true, message: 'ok' }],
          hasErrors: false,
          finalResult: null
        })
      );

      const history = await session.getHistory();

      expect(mockAxios.request).toHaveBeenCalledWith(
        expect.objectContaining({
          method: 'GET',
          url: '/session/agent-sess/agent/history'
        })
      );
      expect(history.states).toHaveLength(1);
      expect(history.states[0].action).toBe('click');
    });

    it('should return empty history when server returns empty states', async () => {
      mockAxios.request.mockResolvedValue(valueResponse({ states: [], hasErrors: false }));

      const history = await session.getHistory();
      expect(history.states).toHaveLength(0);
      expect(history.hasErrors).toBe(false);
    });
  });

  // -------------------------------------------------------------------------
  // Inherited PulsarSession methods work from AgenticSession
  // -------------------------------------------------------------------------

  describe('inherits PulsarSession', () => {
    it('should be able to open a URL', async () => {
      mockAxios.request.mockResolvedValue(
        valueResponse({ url: 'https://a.com', isNil: false, contentLength: 10 })
      );

      const page = await session.open('https://a.com');
      expect(page.url).toBe('https://a.com');
    });

    it('should expose a WebDriver via .driver', () => {
      expect(session.driver).toBeInstanceOf(WebDriver);
    });
  });
});

// ===========================================================================
// WebDriver – complete method coverage
// ===========================================================================

describe('WebDriver', () => {
  let mockAxios: ReturnType<typeof buildMockAxios>;
  let client: PulsarClient;
  let driver: WebDriver;

  // Shared element ref for click/fill/press/getText/getAttribute tests
  const elementRef: ElementRef = {
    'element-6066-11e4-a52e-4f735466cecf': 'elem-id-abc'
  };

  beforeEach(() => {
    mockAxios = buildMockAxios();
    client = new PulsarClient();
    client.sessionId = 'wd-sess';
    driver = new WebDriver(client);
  });

  // -------------------------------------------------------------------------
  // navigateHistory
  // -------------------------------------------------------------------------

  describe('navigateHistory', () => {
    it('is empty initially', () => {
      expect(driver.navigateHistory).toEqual([]);
    });

    it('returns a copy, not the internal array', async () => {
      mockAxios.request.mockResolvedValue(rawResponse({}));
      await driver.navigate('https://a.com');
      const h = driver.navigateHistory;
      h.push('tamper');
      expect(driver.navigateHistory).toHaveLength(1);
    });
  });

  // -------------------------------------------------------------------------
  // navigate
  // -------------------------------------------------------------------------

  describe('navigate', () => {
    it('should POST to /session/{sessionId}/url', async () => {
      mockAxios.request.mockResolvedValue(rawResponse({}));

      await driver.navigate('https://example.com');

      expect(mockAxios.request).toHaveBeenCalledWith(
        expect.objectContaining({
          url: '/session/wd-sess/url',
          data: { url: 'https://example.com' }
        })
      );
    });

    it('should add URL to navigateHistory', async () => {
      mockAxios.request.mockResolvedValue(rawResponse({}));
      await driver.navigate('https://a.com');
      expect(driver.navigateHistory).toContain('https://a.com');
    });

    it('should track multiple navigations in order', async () => {
      mockAxios.request.mockResolvedValue(rawResponse({}));
      await driver.navigate('https://a.com');
      await driver.navigate('https://b.com');
      expect(driver.navigateHistory).toEqual(['https://a.com', 'https://b.com']);
    });
  });

  // -------------------------------------------------------------------------
  // currentUrl
  // -------------------------------------------------------------------------

  describe('currentUrl', () => {
    it('should GET /session/{sessionId}/url', async () => {
      mockAxios.request.mockResolvedValue(valueResponse('https://example.com'));

      const url = await driver.currentUrl();

      expect(url).toBe('https://example.com');
      expect(mockAxios.request).toHaveBeenCalledWith(
        expect.objectContaining({ method: 'GET', url: '/session/wd-sess/url' })
      );
    });
  });

  // -------------------------------------------------------------------------
  // back / forward / refresh
  // -------------------------------------------------------------------------

  describe('back', () => {
    it('should POST to /session/{sessionId}/back', async () => {
      mockAxios.request.mockResolvedValue(rawResponse({}));
      await driver.back();
      expect(mockAxios.request).toHaveBeenCalledWith(
        expect.objectContaining({ url: '/session/wd-sess/back' })
      );
    });
  });

  describe('forward', () => {
    it('should POST to /session/{sessionId}/forward', async () => {
      mockAxios.request.mockResolvedValue(rawResponse({}));
      await driver.forward();
      expect(mockAxios.request).toHaveBeenCalledWith(
        expect.objectContaining({ url: '/session/wd-sess/forward' })
      );
    });
  });

  describe('refresh', () => {
    it('should POST to /session/{sessionId}/refresh', async () => {
      mockAxios.request.mockResolvedValue(rawResponse({}));
      await driver.refresh();
      expect(mockAxios.request).toHaveBeenCalledWith(
        expect.objectContaining({ url: '/session/wd-sess/refresh' })
      );
    });
  });

  // -------------------------------------------------------------------------
  // title
  // -------------------------------------------------------------------------

  describe('title', () => {
    it('should GET /session/{sessionId}/title', async () => {
      mockAxios.request.mockResolvedValue(valueResponse('Page Title'));
      const t = await driver.title();
      expect(t).toBe('Page Title');
    });
  });

  // -------------------------------------------------------------------------
  // findElement / findElements
  // -------------------------------------------------------------------------

  describe('findElement', () => {
    it('should POST with css selector strategy', async () => {
      mockAxios.request.mockResolvedValue(valueResponse(elementRef));

      const el = await driver.findElement('h1.title');

      expect(mockAxios.request).toHaveBeenCalledWith(
        expect.objectContaining({
          url: '/session/wd-sess/element',
          data: { using: 'css selector', value: 'h1.title' }
        })
      );
      expect(el['element-6066-11e4-a52e-4f735466cecf']).toBe('elem-id-abc');
    });
  });

  describe('findElements', () => {
    it('should POST with css selector strategy and return array', async () => {
      mockAxios.request.mockResolvedValue(valueResponse([elementRef]));

      const els = await driver.findElements('li');

      expect(mockAxios.request).toHaveBeenCalledWith(
        expect.objectContaining({
          url: '/session/wd-sess/elements',
          data: { using: 'css selector', value: 'li' }
        })
      );
      expect(els).toHaveLength(1);
    });

    it('should return empty array when no elements found', async () => {
      mockAxios.request.mockResolvedValue(valueResponse([]));
      const els = await driver.findElements('.nonexistent');
      expect(els).toHaveLength(0);
    });
  });

  // -------------------------------------------------------------------------
  // click
  // -------------------------------------------------------------------------

  describe('click', () => {
    it('should find element then POST /element/click with elementId', async () => {
      // findElement response
      mockAxios.request.mockResolvedValueOnce(valueResponse(elementRef));
      // click response
      mockAxios.request.mockResolvedValueOnce(rawResponse({}));

      await driver.click('button.submit');

      expect(mockAxios.request).toHaveBeenNthCalledWith(
        2,
        expect.objectContaining({
          url: '/session/wd-sess/element/click',
          data: { elementId: 'elem-id-abc' }
        })
      );
    });
  });

  // -------------------------------------------------------------------------
  // fill / type
  // -------------------------------------------------------------------------

  describe('fill', () => {
    it('should find element then POST /element/value', async () => {
      mockAxios.request.mockResolvedValueOnce(valueResponse(elementRef));
      mockAxios.request.mockResolvedValueOnce(rawResponse({}));

      await driver.fill('input#name', 'Alice');

      expect(mockAxios.request).toHaveBeenNthCalledWith(
        2,
        expect.objectContaining({
          url: '/session/wd-sess/element/value',
          data: { elementId: 'elem-id-abc', text: 'Alice' }
        })
      );
    });
  });

  describe('type', () => {
    it('should behave the same as fill', async () => {
      mockAxios.request.mockResolvedValueOnce(valueResponse(elementRef));
      mockAxios.request.mockResolvedValueOnce(rawResponse({}));

      await driver.type('input#search', 'query');

      expect(mockAxios.request).toHaveBeenNthCalledWith(
        2,
        expect.objectContaining({ data: { elementId: 'elem-id-abc', text: 'query' } })
      );
    });
  });

  // -------------------------------------------------------------------------
  // press
  // -------------------------------------------------------------------------

  describe('press', () => {
    it('should find element then POST /element/press', async () => {
      mockAxios.request.mockResolvedValueOnce(valueResponse(elementRef));
      mockAxios.request.mockResolvedValueOnce(rawResponse({}));

      await driver.press('input#search', 'Enter');

      expect(mockAxios.request).toHaveBeenNthCalledWith(
        2,
        expect.objectContaining({
          url: '/session/wd-sess/element/press',
          data: { elementId: 'elem-id-abc', key: 'Enter' }
        })
      );
    });
  });

  // -------------------------------------------------------------------------
  // getText
  // -------------------------------------------------------------------------

  describe('getText', () => {
    it('should GET /element/{id}/text', async () => {
      mockAxios.request.mockResolvedValueOnce(valueResponse(elementRef));
      mockAxios.request.mockResolvedValueOnce(valueResponse('Hello World'));

      const text = await driver.getText('h1');

      expect(mockAxios.request).toHaveBeenNthCalledWith(
        2,
        expect.objectContaining({
          method: 'GET',
          url: '/session/wd-sess/element/elem-id-abc/text'
        })
      );
      expect(text).toBe('Hello World');
    });
  });

  // -------------------------------------------------------------------------
  // getAttribute
  // -------------------------------------------------------------------------

  describe('getAttribute', () => {
    it('should GET /element/{id}/attribute/{name}', async () => {
      mockAxios.request.mockResolvedValueOnce(valueResponse(elementRef));
      mockAxios.request.mockResolvedValueOnce(valueResponse('https://link.com'));

      const val = await driver.getAttribute('a.link', 'href');

      expect(mockAxios.request).toHaveBeenNthCalledWith(
        2,
        expect.objectContaining({
          method: 'GET',
          url: '/session/wd-sess/element/elem-id-abc/attribute/href'
        })
      );
      expect(val).toBe('https://link.com');
    });

    it('should return null when attribute is not present', async () => {
      mockAxios.request.mockResolvedValueOnce(valueResponse(elementRef));
      mockAxios.request.mockResolvedValueOnce(valueResponse(null));

      const val = await driver.getAttribute('div', 'data-missing');
      expect(val).toBeNull();
    });
  });

  // -------------------------------------------------------------------------
  // exists
  // -------------------------------------------------------------------------

  describe('exists', () => {
    it('should return true when element exists', async () => {
      mockAxios.request.mockResolvedValue(valueResponse({ exists: true }));
      expect(await driver.exists('h1')).toBe(true);
    });

    it('should return false when element does not exist', async () => {
      mockAxios.request.mockResolvedValue(valueResponse({ exists: false }));
      expect(await driver.exists('.missing')).toBe(false);
    });

    it('should return false when response is null', async () => {
      mockAxios.request.mockResolvedValue(valueResponse(null));
      expect(await driver.exists('.missing')).toBe(false);
    });

    it('should POST to /session/{sessionId}/selectors/exists', async () => {
      mockAxios.request.mockResolvedValue(valueResponse({ exists: true }));
      await driver.exists('#btn');
      expect(mockAxios.request).toHaveBeenCalledWith(
        expect.objectContaining({
          url: '/session/wd-sess/selectors/exists',
          data: { selector: '#btn' }
        })
      );
    });
  });

  // -------------------------------------------------------------------------
  // waitForSelector
  // -------------------------------------------------------------------------

  describe('waitForSelector', () => {
    it('should return true when selector appears', async () => {
      mockAxios.request.mockResolvedValue(valueResponse({ exists: true }));
      expect(await driver.waitForSelector('#content', 5000)).toBe(true);
    });

    it('should return false when selector times out', async () => {
      mockAxios.request.mockResolvedValue(valueResponse({ exists: false }));
      expect(await driver.waitForSelector('#ghost', 3000)).toBe(false);
    });

    it('should use default timeout of 30000', async () => {
      mockAxios.request.mockResolvedValue(valueResponse({ exists: true }));
      await driver.waitForSelector('#el');
      expect(mockAxios.request).toHaveBeenCalledWith(
        expect.objectContaining({ data: { selector: '#el', timeout: 30000 } })
      );
    });

    it('should forward custom timeout', async () => {
      mockAxios.request.mockResolvedValue(valueResponse({ exists: true }));
      await driver.waitForSelector('#el', 10000);
      expect(mockAxios.request).toHaveBeenCalledWith(
        expect.objectContaining({ data: { selector: '#el', timeout: 10000 } })
      );
    });
  });

  // -------------------------------------------------------------------------
  // executeScript
  // -------------------------------------------------------------------------

  describe('executeScript', () => {
    it('should POST to /session/{sessionId}/execute/sync', async () => {
      mockAxios.request.mockResolvedValue(valueResponse('title-value'));

      const result = await driver.executeScript('return document.title');

      expect(mockAxios.request).toHaveBeenCalledWith(
        expect.objectContaining({
          url: '/session/wd-sess/execute/sync',
          data: { script: 'return document.title', args: [] }
        })
      );
      expect(result).toBe('title-value');
    });

    it('should pass args to the script', async () => {
      mockAxios.request.mockResolvedValue(valueResponse(42));

      await driver.executeScript('return arguments[0] + arguments[1]', [10, 32]);

      expect(mockAxios.request).toHaveBeenCalledWith(
        expect.objectContaining({
          data: { script: 'return arguments[0] + arguments[1]', args: [10, 32] }
        })
      );
    });

    it('should use empty args array by default', async () => {
      mockAxios.request.mockResolvedValue(valueResponse(null));
      await driver.executeScript('void 0');
      expect(mockAxios.request).toHaveBeenCalledWith(
        expect.objectContaining({ data: { script: 'void 0', args: [] } })
      );
    });
  });

  // -------------------------------------------------------------------------
  // screenshot
  // -------------------------------------------------------------------------

  describe('screenshot', () => {
    it('should GET /session/{sessionId}/screenshot and return base64 string', async () => {
      mockAxios.request.mockResolvedValue(valueResponse('base64encodedimage=='));

      const img = await driver.screenshot();

      expect(mockAxios.request).toHaveBeenCalledWith(
        expect.objectContaining({ method: 'GET', url: '/session/wd-sess/screenshot' })
      );
      expect(img).toBe('base64encodedimage==');
    });
  });

  // -------------------------------------------------------------------------
  // scrollTo
  // -------------------------------------------------------------------------

  describe('scrollTo', () => {
    it('should POST to /session/{sessionId}/element/scrollTo', async () => {
      mockAxios.request.mockResolvedValue(rawResponse({}));

      await driver.scrollTo('.footer');

      expect(mockAxios.request).toHaveBeenCalledWith(
        expect.objectContaining({
          url: '/session/wd-sess/element/scrollTo',
          data: { selector: '.footer' }
        })
      );
    });
  });

  // -------------------------------------------------------------------------
  // hover
  // -------------------------------------------------------------------------

  describe('hover', () => {
    it('should POST to /session/{sessionId}/element/hover', async () => {
      mockAxios.request.mockResolvedValue(rawResponse({}));

      await driver.hover('.menu-item');

      expect(mockAxios.request).toHaveBeenCalledWith(
        expect.objectContaining({
          url: '/session/wd-sess/element/hover',
          data: { selector: '.menu-item' }
        })
      );
    });
  });

  // -------------------------------------------------------------------------
  // select
  // -------------------------------------------------------------------------

  describe('select', () => {
    it('should POST to /session/{sessionId}/element/select', async () => {
      mockAxios.request.mockResolvedValue(rawResponse({}));

      await driver.select('select#country', 'US');

      expect(mockAxios.request).toHaveBeenCalledWith(
        expect.objectContaining({
          url: '/session/wd-sess/element/select',
          data: { selector: 'select#country', value: 'US' }
        })
      );
    });
  });

  // -------------------------------------------------------------------------
  // delay
  // -------------------------------------------------------------------------

  describe('delay', () => {
    it('should POST to /session/{sessionId}/control/delay with ms', async () => {
      mockAxios.request.mockResolvedValue(rawResponse({}));

      await driver.delay(2000);

      expect(mockAxios.request).toHaveBeenCalledWith(
        expect.objectContaining({
          url: '/session/wd-sess/control/delay',
          data: { ms: 2000 }
        })
      );
    });

    it('should accept zero delay', async () => {
      mockAxios.request.mockResolvedValue(rawResponse({}));
      await driver.delay(0);
      expect(mockAxios.request).toHaveBeenCalledWith(
        expect.objectContaining({ data: { ms: 0 } })
      );
    });
  });
});

// ===========================================================================
// Models – helper functions
// ===========================================================================

describe('Models', () => {
  // -------------------------------------------------------------------------
  // createWebPage
  // -------------------------------------------------------------------------

  describe('createWebPage', () => {
    it('should map all provided fields', () => {
      const page = createWebPage({
        url: 'https://example.com',
        location: 'https://example.com/redirected',
        contentType: 'text/html; charset=utf-8',
        contentLength: 2048,
        protocolStatus: '200 OK',
        isNil: false,
        html: '<html/>'
      });

      expect(page.url).toBe('https://example.com');
      expect(page.location).toBe('https://example.com/redirected');
      expect(page.contentType).toBe('text/html; charset=utf-8');
      expect(page.contentLength).toBe(2048);
      expect(page.protocolStatus).toBe('200 OK');
      expect(page.isNil).toBe(false);
      expect(page.html).toBe('<html/>');
    });

    it('should default url to empty string', () => {
      const page = createWebPage({});
      expect(page.url).toBe('');
    });

    it('should default contentLength to 0', () => {
      const page = createWebPage({});
      expect(page.contentLength).toBe(0);
    });

    it('should default isNil to false', () => {
      const page = createWebPage({});
      expect(page.isNil).toBe(false);
    });

    it('should leave optional fields undefined when absent', () => {
      const page = createWebPage({ url: 'https://x.com', contentLength: 0, isNil: true });
      expect(page.location).toBeUndefined();
      expect(page.contentType).toBeUndefined();
      expect(page.html).toBeUndefined();
    });
  });

  // -------------------------------------------------------------------------
  // createNormURL
  // -------------------------------------------------------------------------

  describe('createNormURL', () => {
    it('should map all provided fields', () => {
      const norm = createNormURL({
        spec: 'https://x.com -expire 1d',
        url: 'https://x.com',
        args: '-expire 1d',
        isNil: false
      });

      expect(norm.spec).toBe('https://x.com -expire 1d');
      expect(norm.url).toBe('https://x.com');
      expect(norm.args).toBe('-expire 1d');
      expect(norm.isNil).toBe(false);
    });

    it('should default spec and url to empty string', () => {
      const norm = createNormURL({});
      expect(norm.spec).toBe('');
      expect(norm.url).toBe('');
    });

    it('should default isNil to false', () => {
      expect(createNormURL({}).isNil).toBe(false);
    });

    it('should leave args undefined when absent', () => {
      expect(createNormURL({ spec: '', url: '' }).args).toBeUndefined();
    });
  });

  // -------------------------------------------------------------------------
  // createAgentRunResult
  // -------------------------------------------------------------------------

  describe('createAgentRunResult', () => {
    it('should map all fields', () => {
      const r = createAgentRunResult({
        success: true,
        message: 'done',
        historySize: 3,
        processTraceSize: 2,
        finalResult: { x: 1 },
        trace: ['a', 'b']
      });

      expect(r.success).toBe(true);
      expect(r.message).toBe('done');
      expect(r.historySize).toBe(3);
      expect(r.processTraceSize).toBe(2);
      expect(r.finalResult).toEqual({ x: 1 });
      expect(r.trace).toEqual(['a', 'b']);
    });

    it('should default success to false', () => {
      expect(createAgentRunResult({}).success).toBe(false);
    });

    it('should default historySize and processTraceSize to 0', () => {
      const r = createAgentRunResult({});
      expect(r.historySize).toBe(0);
      expect(r.processTraceSize).toBe(0);
    });
  });

  // -------------------------------------------------------------------------
  // createAgentActResult
  // -------------------------------------------------------------------------

  describe('createAgentActResult', () => {
    it('should map all fields', () => {
      const r = createAgentActResult({
        success: true,
        message: 'clicked',
        action: 'click',
        isComplete: true,
        expression: 'el.click()',
        result: { clicked: true },
        trace: ['t1']
      });

      expect(r.success).toBe(true);
      expect(r.action).toBe('click');
      expect(r.isComplete).toBe(true);
      expect(r.expression).toBe('el.click()');
      expect(r.result).toEqual({ clicked: true });
      expect(r.trace).toEqual(['t1']);
    });

    it('should default success and isComplete to false', () => {
      const r = createAgentActResult({});
      expect(r.success).toBe(false);
      expect(r.isComplete).toBe(false);
    });
  });

  // -------------------------------------------------------------------------
  // createObserveResult
  // -------------------------------------------------------------------------

  describe('createObserveResult', () => {
    it('should map all optional fields', () => {
      const obs = createObserveResult({
        locator: '0,42',
        domain: 'example.com',
        method: 'click',
        arguments: { x: 1 },
        description: 'Click OK',
        screenshotContentSummary: 'button visible',
        currentPageContentSummary: 'page loaded',
        nextGoal: 'fill form',
        thinking: 'I should click',
        summary: 'Clicked OK',
        keyFindings: 'found button',
        nextSuggestions: ['fill name', 'submit']
      });

      expect(obs.locator).toBe('0,42');
      expect(obs.method).toBe('click');
      expect(obs.nextSuggestions).toEqual(['fill name', 'submit']);
    });

    it('should return empty object when no data', () => {
      const obs = createObserveResult({});
      expect(obs.locator).toBeUndefined();
      expect(obs.method).toBeUndefined();
    });
  });

  // -------------------------------------------------------------------------
  // createAgentObservation
  // -------------------------------------------------------------------------

  describe('createAgentObservation', () => {
    it('should map array of observation objects', () => {
      const obs = createAgentObservation([
        { locator: '0,1', method: 'click' },
        { locator: '0,2', method: 'fill' }
      ]);

      expect(obs.observations).toHaveLength(2);
      expect(obs.observations[0].method).toBe('click');
      expect(obs.observations[1].method).toBe('fill');
    });

    it('should return empty observations for non-array input', () => {
      expect(createAgentObservation(null).observations).toHaveLength(0);
      expect(createAgentObservation({}).observations).toHaveLength(0);
      expect(createAgentObservation(undefined).observations).toHaveLength(0);
    });

    it('should return empty observations for empty array', () => {
      expect(createAgentObservation([]).observations).toHaveLength(0);
    });
  });

  // -------------------------------------------------------------------------
  // createExtractionResult
  // -------------------------------------------------------------------------

  describe('createExtractionResult', () => {
    it('should map all fields', () => {
      const r = createExtractionResult({
        success: true,
        message: 'Extracted',
        data: [{ name: 'Widget' }]
      });

      expect(r.success).toBe(true);
      expect(r.message).toBe('Extracted');
      expect(r.data).toEqual([{ name: 'Widget' }]);
    });

    it('should default success to false', () => {
      expect(createExtractionResult({}).success).toBe(false);
    });

    it('should leave data undefined when absent', () => {
      expect(createExtractionResult({ success: true, message: 'ok' }).data).toBeUndefined();
    });
  });

  // -------------------------------------------------------------------------
  // createAgentHistory
  // -------------------------------------------------------------------------

  describe('createAgentHistory', () => {
    it('should build states array', () => {
      const h = createAgentHistory({
        states: [
          { step: 1, action: 'navigate', success: true, message: 'ok' },
          { step: 2, action: 'click', success: false, message: 'failed', result: 'err' }
        ],
        hasErrors: true,
        finalResult: 'done'
      });

      expect(h.states).toHaveLength(2);
      expect(h.states[0].step).toBe(1);
      expect(h.states[1].success).toBe(false);
      expect(h.hasErrors).toBe(true);
      expect(h.finalResult).toBe('done');
    });

    it('should return empty states when no states array', () => {
      const h = createAgentHistory({});
      expect(h.states).toHaveLength(0);
      expect(h.hasErrors).toBe(false);
    });

    it('should default step to 0 and success to false per state', () => {
      const h = createAgentHistory({ states: [{}] });
      expect(h.states[0].step).toBe(0);
      expect(h.states[0].success).toBe(false);
    });
  });

  // -------------------------------------------------------------------------
  // createChatResponse
  // -------------------------------------------------------------------------

  describe('createChatResponse', () => {
    it('should handle plain string input', () => {
      const r = createChatResponse('Hello from LLM');
      expect(r.content).toBe('Hello from LLM');
      expect(r.role).toBe('assistant');
      expect(r.model).toBeUndefined();
    });

    it('should handle full object input', () => {
      const r = createChatResponse({ content: 'Hi', role: 'assistant', model: 'gpt-4' });
      expect(r.content).toBe('Hi');
      expect(r.model).toBe('gpt-4');
    });

    it('should default content to empty string for object with no content', () => {
      const r = createChatResponse({});
      expect(r.content).toBe('');
    });

    it('should default role to assistant for object with no role', () => {
      const r = createChatResponse({ content: 'Hi' });
      expect(r.role).toBe('assistant');
    });

    it('should return empty content for null input', () => {
      const r = createChatResponse(null);
      expect(r.content).toBe('');
      expect(r.role).toBe('assistant');
    });

    it('should return empty content for undefined input', () => {
      const r = createChatResponse(undefined);
      expect(r.content).toBe('');
    });

    it('should return empty content for numeric input', () => {
      const r = createChatResponse(42);
      expect(r.content).toBe('');
    });
  });

  // -------------------------------------------------------------------------
  // PageEventHandlers
  // -------------------------------------------------------------------------

  describe('PageEventHandlers', () => {
    it('should instantiate without errors', () => {
      expect(() => new PageEventHandlers()).not.toThrow();
    });

    it('should expose browseEventHandlers as empty object', () => {
      const h = new PageEventHandlers();
      expect(h.browseEventHandlers).toBeDefined();
      expect(typeof h.browseEventHandlers).toBe('object');
    });

    it('should expose loadEventHandlers as empty object', () => {
      expect(new PageEventHandlers().loadEventHandlers).toBeDefined();
    });

    it('should expose crawlEventHandlers as empty object', () => {
      expect(new PageEventHandlers().crawlEventHandlers).toBeDefined();
    });
  });
});

// ===========================================================================
// Browser4Driver – configuration and properties
// ===========================================================================

describe('Browser4Driver', () => {
  it('should use default port 8182', () => {
    const d = new Browser4Driver();
    expect(d.baseUrl).toBe('http://localhost:8182');
  });

  it('should use custom port', () => {
    const d = new Browser4Driver({ port: 9000 });
    expect(d.baseUrl).toBe('http://localhost:9000');
  });

  it('running is false before start', () => {
    const d = new Browser4Driver();
    expect(d.running).toBe(false);
  });

  it('should accept custom homeDir config without error', () => {
    expect(() => new Browser4Driver({ homeDir: '/tmp/custom-browser4' })).not.toThrow();
  });

  it('should accept autoDownload: false', () => {
    expect(() => new Browser4Driver({ autoDownload: false })).not.toThrow();
  });

  it('should accept custom javaPath', () => {
    expect(() => new Browser4Driver({ javaPath: '/usr/lib/jvm/java-17/bin/java' })).not.toThrow();
  });

  it('should accept custom startupTimeout', () => {
    expect(() => new Browser4Driver({ startupTimeout: 120000 })).not.toThrow();
  });
});

// ===========================================================================
// End-to-end style workflow tests
// ===========================================================================

describe('End-to-end workflow scenarios', () => {
  let mockAxios: ReturnType<typeof buildMockAxios>;
  let client: PulsarClient;
  let session: AgenticSession;

  beforeEach(() => {
    mockAxios = buildMockAxios();
    client = new PulsarClient();
    client.sessionId = 'e2e-sess';
    session = new AgenticSession(client);
  });

  it('navigate → observe → act → summarize workflow', async () => {
    // navigate
    mockAxios.request.mockResolvedValueOnce(rawResponse({}));
    await session.driver.navigate('https://shop.example.com');
    expect(session.driver.navigateHistory).toContain('https://shop.example.com');

    // observe
    mockAxios.request.mockResolvedValueOnce(
      valueResponse([{ locator: '0,5', method: 'click', description: 'Add to cart' }])
    );
    const obs = await session.observe('what actions can I take?');
    expect(obs.observations[0].description).toBe('Add to cart');

    // act
    mockAxios.request.mockResolvedValueOnce(
      valueResponse({ success: true, message: 'added', isComplete: true })
    );
    const actResult = await session.act('click Add to cart button');
    expect(actResult.success).toBe(true);

    // summarize
    mockAxios.request.mockResolvedValueOnce(valueResponse('Cart now has 1 item'));
    const summary = await session.summarize('What is in the cart?');
    expect(summary).toBe('Cart now has 1 item');

    // Check accumulated state
    expect(session.stateHistory.states).toHaveLength(1);
  });

  it('run → extract → clearHistory lifecycle', async () => {
    // run
    mockAxios.request.mockResolvedValueOnce(
      valueResponse({
        success: true,
        message: 'Searched for laptops',
        historySize: 3,
        processTraceSize: 2,
        trace: ['init', 'search', 'scroll']
      })
    );
    const runResult = await session.run('search for laptops');
    expect(runResult.success).toBe(true);
    expect(session.processTrace).toContain('init');

    // agentExtract
    mockAxios.request.mockResolvedValueOnce(
      valueResponse({
        success: true,
        message: 'ok',
        data: [{ name: 'ThinkPad X1', price: '$1299' }]
      })
    );
    const extracted = await session.agentExtract('list laptop names and prices', {
      type: 'array',
      items: { name: 'string', price: 'string' }
    });
    expect(extracted.data[0].name).toBe('ThinkPad X1');

    // clearHistory
    mockAxios.request.mockResolvedValueOnce(valueResponse(true));
    await session.clearHistory();
    expect(session.processTrace).toHaveLength(0);
    expect(session.stateHistory.states).toHaveLength(0);
  });

  it('should handle a failing act gracefully without crashing', async () => {
    mockAxios.request.mockResolvedValue(
      valueResponse({ success: false, message: 'Element not found', isComplete: false })
    );

    const result = await session.act('click invisible element');
    expect(result.success).toBe(false);
    expect(session.stateHistory.hasErrors).toBe(true);
  });

  it('should handle server errors by propagating the exception', async () => {
    const axiosErr = Object.assign(new Error('Service Unavailable'), {
      isAxiosError: true,
      response: { status: 503, headers: { 'content-type': 'text/plain' }, data: 'down' },
      config: { url: '/session/e2e-sess/agent/run' }
    });
    mockedAxios.isAxiosError.mockReturnValue(true);
    mockAxios.request.mockRejectedValue(axiosErr);

    await expect(session.run('do something')).rejects.toThrow('HTTP 503');
  });
});
