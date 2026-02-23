# Quick Reference for @platonai/browser4-sdk

Quick reference for common tasks with the Browser4 NodeJS SDK.

## Installation

```bash
# Install from npm (when published)
npm install @platonai/browser4-sdk

# Install from local package
npm install ./platonai-browser4-sdk-0.1.0.tgz

# Install globally
npm install -g @platonai/browser4-sdk
```

## Development

```bash
# Install dependencies
npm install

# Build the SDK
npm run build

# Clean build artifacts
npm run clean

# Rebuild from scratch
npm run rebuild

# Watch for changes
npm run build -- --watch
```

## Testing

```bash
# Run all tests
npm test

# Run tests in watch mode
npm run test:watch

# Run tests with coverage
npm run test:coverage
```

## Linting & Formatting

```bash
# Lint code
npm run lint

# Format code
npm run format
```

## Package Management

```bash
# Check what will be published
npm run pack:check

# Create a package tarball
npm pack

# Verify local build
npm run verify:build

# Verify installation (after npm install)
npm run verify
```

## Version Management

```bash
# Bump patch version (0.1.0 -> 0.1.1)
npm run version:patch

# Bump minor version (0.1.0 -> 0.2.0)
npm run version:minor

# Bump major version (0.1.0 -> 1.0.0)
npm run version:major

# Manual version update
npm version 0.2.0
```

## Publishing

```bash
# Test publish without actually publishing
npm run publish:test

# Publish to npm registry
npm run publish:public

# Or manually
npm publish --access public
```

## Quick Start Examples

### JavaScript (CommonJS)

```javascript
const { Browser4Driver, PulsarClient, AgenticSession } = require('@platonai/browser4-sdk');

async function main() {
  const driver = new Browser4Driver();
  await driver.use(async (d) => {
    const client = new PulsarClient({ baseUrl: d.baseUrl });
    await client.createSession();
    const session = new AgenticSession(client);

    const page = await session.open('https://example.com');
    console.log(page.url);

    await session.close();
  });
}

main().catch(console.error);
```

### TypeScript (ES Modules)

```typescript
import { Browser4Driver, PulsarClient, AgenticSession } from '@platonai/browser4-sdk';

async function main(): Promise<void> {
  const driver = new Browser4Driver();
  await driver.use(async (d) => {
    const client = new PulsarClient({ baseUrl: d.baseUrl });
    await client.createSession();
    const session = new AgenticSession(client);

    const page = await session.open('https://example.com');
    console.log(page.url);

    await session.close();
  });
}

main().catch(console.error);
```

### Web Scraping

```javascript
const { Browser4Driver, PulsarClient, PulsarSession } = require('@platonai/browser4-sdk');

async function scrape() {
  const driver = new Browser4Driver();
  await driver.use(async (d) => {
    const client = new PulsarClient({ baseUrl: d.baseUrl });
    await client.createSession();
    const session = new PulsarSession(client);

    const data = await session.scrape('https://example.com', {
      title: 'h1',
      description: 'meta[name="description"]::attr(content)',
      links: 'a::attr(href)'
    });

    console.log(data);
    await session.close();
  });
}

scrape().catch(console.error);
```

### AI-Powered Automation

```javascript
const { Browser4Driver, PulsarClient, AgenticSession } = require('@platonai/browser4-sdk');

async function automate() {
  const driver = new Browser4Driver();
  await driver.use(async (d) => {
    const client = new PulsarClient({ baseUrl: d.baseUrl });
    await client.createSession();
    const session = new AgenticSession(client);

    await session.open('https://example.com');

    // Single action
    await session.act('click the login button');

    // Multi-step task
    await session.run('search for "nodejs" and click the first result');

    await session.close();
  });
}

automate().catch(console.error);
```

## Troubleshooting

### Build Issues

```bash
# Clear everything and rebuild
rm -rf node_modules package-lock.json dist
npm install
npm run build
```

### Installation Issues

```bash
# Clear npm cache
npm cache clean --force

# Reinstall
npm install --force
```

### Test Issues

```bash
# Update snapshots
npm test -- -u

# Run specific test
npm test -- test-name
```

## Environment Variables

```bash
# Set custom Java path
export JAVA_HOME=/path/to/java

# Set custom Browser4 home
export BROWSER4_HOME=/path/to/.browser4

# Set custom port
export BROWSER4_PORT=8183
```

## File Structure

```
browser4-nodejs/
├── dist/              # Compiled JavaScript and type definitions
├── src/               # TypeScript source code
├── tests/             # Test files
├── examples/          # Example usage
├── scripts/           # Utility scripts
├── package.json       # Package configuration
├── tsconfig.json      # TypeScript configuration
├── README.md          # Main documentation
├── INSTALLATION.md    # Installation guide
├── PUBLISHING.md      # Publishing guide
└── LICENSE            # Apache 2.0 license
```

## Common Scripts Explained

- `prepare`: Runs automatically before package is published or installed from git
- `prepublishOnly`: Runs before `npm publish` (lints, tests, builds)
- `prepack`: Runs before creating tarball (rebuilds from scratch)
- `pack:check`: Shows what will be included in the published package

## Links

- **Repository**: https://github.com/platonai/Browser4
- **SDK Directory**: https://github.com/platonai/Browser4/tree/main/sdks/browser4-nodejs
- **npm Package**: https://www.npmjs.com/package/@platonai/browser4-sdk (when published)
- **Issues**: https://github.com/platonai/Browser4/issues
- **Documentation**: https://github.com/platonai/Browser4/tree/main/docs

## Support

For help:
1. Check the [README.md](./README.md)
2. Read [INSTALLATION.md](./INSTALLATION.md)
3. Review [examples/](./examples/)
4. Open an [issue](https://github.com/platonai/Browser4/issues)
