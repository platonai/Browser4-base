# Browser4 NodeJS SDK

NodeJS SDK for Browser4 WebDriver-compatible and AgenticSession APIs.

This SDK provides a NodeJS interface to the Browser4 browser automation platform, enabling:
- **Automatic server management** with Browser4Driver (download & start Browser4.jar automatically)
- **Web scraping and data extraction** using CSS selectors
- **AI-powered browser automation** with natural language instructions
- **WebDriver-compatible API** for browser control

## Installation

```bash
npm install @platonai/browser4-sdk
```

For detailed installation instructions, troubleshooting, and system requirements, see [INSTALLATION.md](./INSTALLATION.md).

### Verify Installation

After installation, verify it works:

```bash
npm run verify
```

Or manually test:

```javascript
const sdk = require('@platonai/browser4-sdk');
console.log(sdk.PulsarClient ? '✅ Installed' : '❌ Failed');
```

## Quick Start

The easiest way to get started is using `Browser4Driver`, which automatically downloads and starts the Browser4 server:

```typescript
import { Browser4Driver, PulsarClient, AgenticSession } from '@platonai/browser4-sdk';

// Browser4Driver automatically downloads and starts the server
const driver = new Browser4Driver();
await driver.use(async (d) => {
  // Create client and session
  const client = new PulsarClient({ baseUrl: d.baseUrl });
  const sessionId = await client.createSession();
  const session = new AgenticSession(client);

  // Navigate to a page
  const page = await session.open('https://example.com');
  console.log(`Opened: ${page.url}`);

  // Use WebDriver for element interaction
  const webDriver = session.driver;
  await webDriver.fill('input[name="search"]', 'browser automation');
  await webDriver.press('input[name="search"]', 'Enter');

  // Extract data using CSS selectors (parse first, then extract)
  const document = await session.parse(page);
  const fields = await session.extract(document, {
    title: 'h1',
    description: '.description'
  });
  console.log(fields);

  // Use AI-powered actions
  const result = await session.act('click the login button');
  console.log(`Action success: ${result.success}`);

  // Run multi-step tasks
  const history = await session.run('search for "python" and click the first result');
  console.log(`Task completed: ${history.success}`);

  // Clean up
  await session.close();
  client.close();
});

// Server stops automatically when exiting the use block
```

If you already have a Browser4 server running, you can connect to it directly:

```typescript
import { PulsarClient, AgenticSession } from '@platonai/browser4-sdk';

// Connect to existing server
const client = new PulsarClient({ baseUrl: 'http://localhost:8182' });
const sessionId = await client.createSession();
const session = new AgenticSession(client);

// ... use the session ...

await session.close();
```

## API Overview

### Browser4Driver

Manages the lifecycle of a local Browser4.jar process, including automatic download, startup, and shutdown.

```typescript
import { Browser4Driver } from '@platonai/browser4-sdk';

// Using the use method (recommended)
const driver = new Browser4Driver();
await driver.use(async (d) => {
  console.log(`Server running at: ${d.baseUrl}`);
  // Use the server...
});

// Manual control
const driver = new Browser4Driver({ port: 8183 });
await driver.start();
console.log(`Server running at: ${driver.baseUrl}`);
// ... use the server ...
await driver.stop();
```

**Configuration options:**
- `homeDir`: Directory to store Browser4.jar (default: `~/.browser4`)
- `port`: Server port (default: 8182)
- `startupTimeout`: Startup timeout in ms (default: 60000)
- `downloadUrl`: Custom download URL for Browser4.jar
- `autoDownload`: Auto-download if jar not found (default: true)
- `javaPath`: Path to Java executable (default: 'java')

### PulsarClient

Low-level HTTP client for Browser4 API communication.

```typescript
import { PulsarClient } from '@platonai/browser4-sdk';

const client = new PulsarClient({
  baseUrl: 'http://localhost:8182',
  timeout: 30000
});

const sessionId = await client.createSession();
console.log(`Session ID: ${sessionId}`);

// Make API calls
const result = await client.post('/session/{sessionId}/open', {
  url: 'https://example.com'
});

await client.deleteSession();
```

### PulsarSession

High-level session management for page loading, parsing, and extraction.

```typescript
import { PulsarClient, PulsarSession } from '@platonai/browser4-sdk';

const client = new PulsarClient();
await client.createSession();
const session = new PulsarSession(client);

// Open URL immediately (bypass cache)
const page = await session.open('https://example.com');

// Load from cache or fetch from internet
const page2 = await session.load('https://example.com', '-expire 1d');

// Submit URL to crawl pool
await session.submit('https://example.com');

// Normalize URL with args
const normUrl = await session.normalize('https://example.com', '-expire 1d');

// Parse and extract
const document = await session.parse(page);
const fields = await session.extract(document, {
  title: 'h1',
  price: '.price'
});

// Scrape in one call
const data = await session.scrape('https://example.com', {
  title: 'h1',
  description: '.description'
});

await session.close();
```

### AgenticSession

AI-powered browser automation extending PulsarSession.

```typescript
import { PulsarClient, AgenticSession } from '@platonai/browser4-sdk';

const client = new PulsarClient();
await client.createSession();
const session = new AgenticSession(client);

// Execute a single action
const actResult = await session.act('click the login button');
console.log(actResult.success);

// Run multi-step autonomous task
const runResult = await session.run('search for "nodejs" and click first result');
console.log(runResult.finalResult);

// Observe the page
const observations = await session.observe('what can I do on this page?');
observations.observations.forEach(obs => {
  console.log(`${obs.method}: ${obs.description}`);
});

// Summarize page content
const summary = await session.summarize('summarize this page');
console.log(summary);

// Extract data using AI
const extraction = await session.agentExtract(
  'extract all product names and prices',
  { type: 'array', items: { name: 'string', price: 'number' } }
);
console.log(extraction.data);

// Get execution history
const history = session.stateHistory;
console.log(`Executed ${history.states.length} actions`);

// Clear history
await session.clearHistory();

await session.close();
```

### WebDriver

Browser control and element interaction.

```typescript
import { PulsarClient, PulsarSession } from '@platonai/browser4-sdk';

const client = new PulsarClient();
await client.createSession();
const session = new PulsarSession(client);
const driver = session.driver;

// Navigation
await driver.navigateTo('https://example.com');
console.log(await driver.currentUrl());
await driver.back();
await driver.forward();
await driver.refresh();

// Element interaction
await driver.click('button.submit');
await driver.fill('input[name="email"]', 'test@example.com');
await driver.type('textarea', 'Some text');
await driver.press('input', 'Enter');

// Element queries
const text = await driver.getText('h1');
const attr = await driver.getAttribute('a', 'href');
const exists = await driver.exists('.modal');
await driver.waitForSelector('.loading', 5000);

// Actions
await driver.hover('.dropdown');
await driver.scrollTo('#footer');
await driver.select('select[name="country"]', 'USA');

// JavaScript execution
const result = await driver.executeScript('return document.title');

// Screenshot
const screenshot = await driver.screenshot();

// Delay
await driver.delay(1000);

// Navigation history
console.log(driver.navigateHistory);
```

## Data Models

The SDK provides TypeScript interfaces for all data models:

```typescript
import {
  WebPage,
  NormURL,
  AgentRunResult,
  AgentActResult,
  AgentObservation,
  ExtractionResult,
  AgentHistory,
  ChatResponse
} from '@platonai/browser4-sdk';
```

## Testing

```bash
# Run tests
npm test

# Run tests with coverage
npm run test:coverage

# Run tests in watch mode
npm run test:watch
```

## Building

```bash
# Build the SDK
npm run build

# Lint
npm run lint

# Format
npm run format
```

## Examples

See the `examples/` directory for more examples:
- Basic usage
- Web scraping
- AI-powered automation
- Advanced workflows

## Publishing

For maintainers who need to publish the package to npm, see [PUBLISHING.md](./PUBLISHING.md) for detailed instructions.

## Requirements

- Node.js >= 16
- Java 17+ (for Browser4 server)
- Latest Google Chrome (for browser automation)

## License

Apache-2.0

## Links

- [GitHub Repository](https://github.com/platonai/Browser4)
- [Documentation](https://github.com/platonai/Browser4/tree/main/docs)
- [Python SDK](https://github.com/platonai/Browser4/tree/main/sdks/browser4-python)
- [Kotlin SDK](https://github.com/platonai/Browser4/tree/main/sdks/browser4-kotlin)
