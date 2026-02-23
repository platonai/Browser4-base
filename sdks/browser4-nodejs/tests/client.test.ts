/**
 * Unit tests for the Browser4 NodeJS SDK.
 * 
 * These tests use mock responses to verify the SDK behavior without
 * requiring a running Browser4 server.
 */

import {
  PulsarClient,
  PulsarSession,
  AgenticSession,
  WebDriver,
  PageEventHandlers,
  createWebPage,
  createNormURL,
  createAgentRunResult
} from '../src';

// Mock axios
jest.mock('axios');
import axios from 'axios';
const mockedAxios = axios as jest.Mocked<typeof axios>;

describe('PulsarClient', () => {
  let client: PulsarClient;
  let mockAxiosInstance: any;

  beforeEach(() => {
    mockAxiosInstance = {
      request: jest.fn()
    };
    mockedAxios.create.mockReturnValue(mockAxiosInstance);
    client = new PulsarClient({ baseUrl: 'http://localhost:8182' });
  });

  describe('createSession', () => {
    it('should create session and set session ID', async () => {
      mockAxiosInstance.request.mockResolvedValue({
        data: { value: { sessionId: 'test-session-123' } }
      });

      const sessionId = await client.createSession();

      expect(sessionId).toBe('test-session-123');
      expect(client.sessionId).toBe('test-session-123');
      expect(mockAxiosInstance.request).toHaveBeenCalledWith(
        expect.objectContaining({
          method: 'POST',
          url: '/session'
        })
      );
    });

    it('should create session with capabilities', async () => {
      mockAxiosInstance.request.mockResolvedValue({
        data: { value: { sessionId: 'test-session-123' } }
      });

      await client.createSession({ browserName: 'chrome' });

      expect(mockAxiosInstance.request).toHaveBeenCalledWith(
        expect.objectContaining({
          data: { capabilities: { browserName: 'chrome' } }
        })
      );
    });

    it('should throw error if sessionId missing', async () => {
      mockAxiosInstance.request.mockResolvedValue({
        data: { value: {} }
      });

      await expect(client.createSession()).rejects.toThrow('createSession response missing sessionId');
    });
  });

  describe('deleteSession', () => {
    it('should delete session', async () => {
      client.sessionId = 'test-session-123';
      mockAxiosInstance.request.mockResolvedValue({ data: {} });

      await client.deleteSession();

      expect(mockAxiosInstance.request).toHaveBeenCalledWith(
        expect.objectContaining({
          method: 'DELETE',
          url: '/session/test-session-123'
        })
      );
    });

    it('should throw error if no session ID', async () => {
      await expect(client.deleteSession()).rejects.toThrow('session_id is required');
    });
  });

  describe('post', () => {
    it('should make POST request', async () => {
      client.sessionId = 'test-session-123';
      mockAxiosInstance.request.mockResolvedValue({
        data: { value: { result: 'success' } }
      });

      const result = await client.post('/test', { data: 'test' });

      expect(result).toEqual({ result: 'success' });
    });
  });

  describe('get', () => {
    it('should make GET request', async () => {
      client.sessionId = 'test-session-123';
      mockAxiosInstance.request.mockResolvedValue({
        data: { value: 'test-result' }
      });

      const result = await client.get('/test');

      expect(result).toBe('test-result');
    });
  });
});

describe('PulsarSession', () => {
  let client: PulsarClient;
  let session: PulsarSession;
  let mockAxiosInstance: any;

  beforeEach(() => {
    mockAxiosInstance = {
      request: jest.fn()
    };
    mockedAxios.create.mockReturnValue(mockAxiosInstance);
    client = new PulsarClient();
    client.sessionId = 'test-session-123';
    session = new PulsarSession(client);
  });

  describe('properties', () => {
    it('should return session properties', () => {
      expect(session.uuid).toBe('test-session-123');
      expect(session.isActive).toBe(true);
      expect(session.display).toContain('PulsarSession');
    });
  });

  describe('normalize', () => {
    it('should normalize URL', async () => {
      mockAxiosInstance.request.mockResolvedValue({
        data: {
          value: {
            spec: 'https://example.com',
            url: 'https://example.com',
            args: '-expire 1d',
            isNil: false
          }
        }
      });

      const result = await session.normalize('https://example.com', '-expire 1d');

      expect(result.url).toBe('https://example.com');
      expect(result.args).toBe('-expire 1d');
      expect(result.isNil).toBe(false);
    });
  });

  describe('open', () => {
    it('should open URL immediately', async () => {
      mockAxiosInstance.request.mockResolvedValue({
        data: {
          value: {
            url: 'https://example.com',
            location: 'https://example.com',
            contentType: 'text/html',
            contentLength: 1024,
            protocolStatus: '200 OK',
            isNil: false
          }
        }
      });

      const page = await session.open('https://example.com');

      expect(page.url).toBe('https://example.com');
      expect(page.contentType).toBe('text/html');
      expect(page.isNil).toBe(false);
    });
  });

  describe('load', () => {
    it('should load URL from cache or internet', async () => {
      mockAxiosInstance.request.mockResolvedValue({
        data: {
          value: {
            url: 'https://example.com',
            contentType: 'text/html',
            contentLength: 2048,
            protocolStatus: '200 OK (cached)',
            isNil: false
          }
        }
      });

      const page = await session.load('https://example.com', '-expire 1d');

      expect(page.url).toBe('https://example.com');
      expect(mockAxiosInstance.request).toHaveBeenCalledWith(
        expect.objectContaining({
          data: expect.objectContaining({
            args: '-expire 1d'
          })
        })
      );
    });
  });

  describe('submit', () => {
    it('should submit URL to crawl pool', async () => {
      mockAxiosInstance.request.mockResolvedValue({
        data: { value: true }
      });

      const result = await session.submit('https://example.com');

      expect(result).toBe(true);
    });
  });

  describe('driver', () => {
    it('should return WebDriver instance', () => {
      const driver = session.driver;
      expect(driver).toBeInstanceOf(WebDriver);
    });

    it('should return same instance on multiple calls', () => {
      const driver1 = session.driver;
      const driver2 = session.driver;
      expect(driver1).toBe(driver2);
    });
  });

  describe('chat', () => {
    it('should send chat message', async () => {
      mockAxiosInstance.request.mockResolvedValue({
        data: {
          value: {
            content: 'Chat response',
            role: 'assistant',
            model: 'gpt-4'
          }
        }
      });

      const response = await session.chat('Hello');

      expect(response.content).toBe('Chat response');
      expect(response.role).toBe('assistant');
    });
  });
});

describe('AgenticSession', () => {
  let client: PulsarClient;
  let session: AgenticSession;
  let mockAxiosInstance: any;

  beforeEach(() => {
    mockAxiosInstance = {
      request: jest.fn()
    };
    mockedAxios.create.mockReturnValue(mockAxiosInstance);
    client = new PulsarClient();
    client.sessionId = 'test-session-123';
    session = new AgenticSession(client);
  });

  describe('run', () => {
    it('should run autonomous task', async () => {
      mockAxiosInstance.request.mockResolvedValue({
        data: {
          value: {
            success: true,
            message: 'Task completed',
            historySize: 5,
            processTraceSize: 3,
            finalResult: 'done',
            trace: ['step1', 'step2']
          }
        }
      });

      const result = await session.run('complete the task');

      expect(result.success).toBe(true);
      expect(result.message).toBe('Task completed');
      expect(result.historySize).toBe(5);
      expect(session.processTrace).toContain('step1');
      expect(session.processTrace).toContain('step2');
    });
  });

  describe('act', () => {
    it('should execute single action', async () => {
      mockAxiosInstance.request.mockResolvedValue({
        data: {
          value: {
            success: true,
            message: 'Action executed',
            action: 'click button',
            isComplete: true,
            trace: ['action_trace']
          }
        }
      });

      const result = await session.act('click the button');

      expect(result.success).toBe(true);
      expect(result.isComplete).toBe(true);
      expect(session.stateHistory.states.length).toBe(1);
      expect(session.stateHistory.states[0].action).toBe('click the button');
    });
  });

  describe('observe', () => {
    it('should observe page', async () => {
      mockAxiosInstance.request.mockResolvedValue({
        data: {
          value: [
            {
              locator: '0,123',
              method: 'click',
              description: 'Click button'
            }
          ]
        }
      });

      const observations = await session.observe('what can I do?');

      expect(observations.observations.length).toBeGreaterThan(0);
      expect(observations.observations[0].method).toBe('click');
    });
  });

  describe('summarize', () => {
    it('should summarize page', async () => {
      mockAxiosInstance.request.mockResolvedValue({
        data: { value: 'Page summary text' }
      });

      const summary = await session.summarize('Summarize this page');

      expect(summary).toBe('Page summary text');
    });
  });

  describe('clearHistory', () => {
    it('should clear agent history', async () => {
      mockAxiosInstance.request.mockResolvedValue({
        data: { value: true }
      });

      // Add some trace
      mockAxiosInstance.request.mockResolvedValueOnce({
        data: {
          value: {
            success: true,
            message: 'ok',
            action: 'test',
            isComplete: true,
            trace: ['item1', 'item2']
          }
        }
      });

      await session.act('test action');
      expect(session.processTrace.length).toBeGreaterThan(0);

      mockAxiosInstance.request.mockResolvedValueOnce({
        data: { value: true }
      });

      const result = await session.clearHistory();

      expect(result).toBe(true);
      expect(session.processTrace.length).toBe(0);
      expect(session.stateHistory.states.length).toBe(0);
    });
  });

  describe('companionAgent', () => {
    it('should return self', () => {
      expect(session.companionAgent).toBe(session);
    });
  });

  describe('agentExtract', () => {
    it('should extract data using AI', async () => {
      mockAxiosInstance.request.mockResolvedValue({
        data: {
          value: {
            success: true,
            message: 'Extracted',
            data: { field1: 'value1' }
          }
        }
      });

      const result = await session.agentExtract('Extract product names', { type: 'array' });

      expect(result.success).toBe(true);
      expect(result.data).toEqual({ field1: 'value1' });
    });
  });
});

describe('WebDriver', () => {
  let client: PulsarClient;
  let driver: WebDriver;
  let mockAxiosInstance: any;

  beforeEach(() => {
    mockAxiosInstance = {
      request: jest.fn()
    };
    mockedAxios.create.mockReturnValue(mockAxiosInstance);
    client = new PulsarClient();
    client.sessionId = 'test-session-123';
    driver = new WebDriver(client);
  });

  describe('navigateTo', () => {
    it('should navigate to URL', async () => {
      mockAxiosInstance.request.mockResolvedValue({ data: {} });

      await driver.navigateTo('https://example.com');

      expect(mockAxiosInstance.request).toHaveBeenCalledWith(
        expect.objectContaining({
          data: { url: 'https://example.com' }
        })
      );
      expect(driver.navigateHistory).toContain('https://example.com');
    });
  });

  describe('currentUrl', () => {
    it('should get current URL', async () => {
      mockAxiosInstance.request.mockResolvedValue({
        data: { value: 'https://example.com' }
      });

      const url = await driver.currentUrl();

      expect(url).toBe('https://example.com');
    });
  });

  describe('exists', () => {
    it('should check element existence', async () => {
      mockAxiosInstance.request.mockResolvedValue({
        data: { value: { exists: true } }
      });

      const exists = await driver.exists('h1.title');

      expect(exists).toBe(true);
    });
  });

  describe('waitForSelector', () => {
    it('should wait for selector', async () => {
      mockAxiosInstance.request.mockResolvedValue({
        data: { value: { exists: true } }
      });

      const found = await driver.waitForSelector('h1.title', 5000);

      expect(found).toBe(true);
      expect(mockAxiosInstance.request).toHaveBeenCalledWith(
        expect.objectContaining({
          data: expect.objectContaining({
            timeout: 5000
          })
        })
      );
    });
  });

  describe('executeScript', () => {
    it('should execute script', async () => {
      mockAxiosInstance.request.mockResolvedValue({
        data: { value: 'script result' }
      });

      const result = await driver.executeScript('return document.title');

      expect(result).toBe('script result');
    });
  });

  describe('delay', () => {
    it('should delay execution', async () => {
      mockAxiosInstance.request.mockResolvedValue({ data: {} });

      await driver.delay(1000);

      expect(mockAxiosInstance.request).toHaveBeenCalledWith(
        expect.objectContaining({
          data: { ms: 1000 }
        })
      );
    });
  });

  describe('navigation history', () => {
    it('should track navigation history', async () => {
      mockAxiosInstance.request.mockResolvedValue({ data: {} });

      await driver.navigateTo('https://example.com');
      await driver.navigateTo('https://example.com/page2');

      const history = driver.navigateHistory;
      expect(history.length).toBe(2);
      expect(history[0]).toBe('https://example.com');
      expect(history[1]).toBe('https://example.com/page2');
    });
  });
});

describe('Models', () => {
  describe('createWebPage', () => {
    it('should create WebPage from data', () => {
      const data = {
        url: 'https://example.com',
        location: 'https://example.com/final',
        contentType: 'text/html',
        contentLength: 1024,
        protocolStatus: '200 OK',
        isNil: false
      };

      const page = createWebPage(data);

      expect(page.url).toBe('https://example.com');
      expect(page.location).toBe('https://example.com/final');
      expect(page.contentType).toBe('text/html');
      expect(page.contentLength).toBe(1024);
      expect(page.isNil).toBe(false);
    });
  });

  describe('createNormURL', () => {
    it('should create NormURL from data', () => {
      const data = {
        spec: 'https://example.com -expire 1d',
        url: 'https://example.com',
        args: '-expire 1d',
        isNil: false
      };

      const norm = createNormURL(data);

      expect(norm.spec).toBe('https://example.com -expire 1d');
      expect(norm.url).toBe('https://example.com');
      expect(norm.args).toBe('-expire 1d');
    });
  });

  describe('createAgentRunResult', () => {
    it('should create AgentRunResult from data', () => {
      const data = {
        success: true,
        message: 'Completed',
        historySize: 5,
        processTraceSize: 3,
        finalResult: { key: 'value' }
      };

      const result = createAgentRunResult(data);

      expect(result.success).toBe(true);
      expect(result.message).toBe('Completed');
      expect(result.historySize).toBe(5);
      expect(result.finalResult).toEqual({ key: 'value' });
    });
  });

  describe('PageEventHandlers', () => {
    it('should create placeholder handlers', () => {
      const handlers = new PageEventHandlers();

      expect(handlers.browseEventHandlers).toBeDefined();
      expect(handlers.loadEventHandlers).toBeDefined();
      expect(handlers.crawlEventHandlers).toBeDefined();
    });
  });
});

describe('Integration-style Tests', () => {
  let client: PulsarClient;
  let session: AgenticSession;
  let mockAxiosInstance: any;

  beforeEach(() => {
    mockAxiosInstance = {
      request: jest.fn()
    };
    mockedAxios.create.mockReturnValue(mockAxiosInstance);
    client = new PulsarClient();
    client.sessionId = 'test-session-123';
    session = new AgenticSession(client);
  });

  it('should complete full workflow', async () => {
    // Mock open
    mockAxiosInstance.request.mockResolvedValueOnce({
      data: {
        value: {
          url: 'https://example.com',
          isNil: false,
          contentType: 'text/html',
          contentLength: 1024
        }
      }
    });

    const page = await session.open('https://example.com');
    expect(page.url).toBe('https://example.com');

    // Get driver
    const driver = session.getOrCreateBoundDriver();
    expect(driver).toBe(session.driver);

    // Mock act
    mockAxiosInstance.request.mockResolvedValueOnce({
      data: {
        value: {
          success: true,
          message: 'ok',
          action: 'click',
          isComplete: true
        }
      }
    });

    const actResult = await session.act('click the search button');
    expect(actResult.success).toBe(true);

    // Mock run
    mockAxiosInstance.request.mockResolvedValueOnce({
      data: {
        value: {
          success: true,
          message: 'ok',
          historySize: 1,
          processTraceSize: 1
        }
      }
    });

    const runResult = await session.run("search for 'test'");
    expect(runResult.success).toBe(true);

    // Check trace
    expect(session.stateHistory.states.length).toBeGreaterThan(0);

    // Mock clear
    mockAxiosInstance.request.mockResolvedValueOnce({
      data: { value: true }
    });

    await session.clearHistory();
    expect(session.processTrace.length).toBe(0);
  });
});
