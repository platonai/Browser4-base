# Installation Guide for @platonai/browser4-sdk

This guide provides detailed instructions for installing and setting up the Browser4 NodeJS SDK.

## Table of Contents

- [System Requirements](#system-requirements)
- [Installation Methods](#installation-methods)
- [Verification](#verification)
- [Common Issues](#common-issues)
- [Uninstallation](#uninstallation)

## System Requirements

Before installing, ensure your system meets these requirements:

- **Node.js**: Version 16.0.0 or higher
- **npm**: Version 7.0.0 or higher
- **Java**: Version 17 or higher (for Browser4 server)
- **Google Chrome**: Latest version (for browser automation)

### Check Current Versions

```bash
node --version    # Should be >= 16.0.0
npm --version     # Should be >= 7.0.0
java --version    # Should be >= 17
```

### Update Node.js and npm

If you need to update Node.js:

**Using nvm (recommended):**
```bash
# Install nvm (if not already installed)
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash

# Install latest LTS version
nvm install --lts
nvm use --lts
```

**Using official installer:**
- Download from [nodejs.org](https://nodejs.org/)

## Installation Methods

### Method 1: npm (Recommended)

Install the latest version:

```bash
npm install @platonai/browser4-sdk
```

Install a specific version:

```bash
npm install @platonai/browser4-sdk@0.1.0
```

Install as a development dependency:

```bash
npm install --save-dev @platonai/browser4-sdk
```

### Method 2: yarn

```bash
yarn add @platonai/browser4-sdk
```

### Method 3: pnpm

```bash
pnpm add @platonai/browser4-sdk
```

### Method 4: From Source (Development)

For contributors or advanced users:

```bash
# Clone the repository
git clone https://github.com/platonai/Browser4.git
cd Browser4/sdks/browser4-nodejs

# Install dependencies
npm install

# Build the SDK
npm run build

# Link for local development
npm link

# In your project
npm link @platonai/browser4-sdk
```

## Verification

After installation, verify everything works correctly.

### Method 1: Using Built-in Verification Script

If you installed from a published package that includes the verification script:

```bash
npx @platonai/browser4-sdk verify
```

Or if installed locally:

```bash
npm run verify
```

### Method 2: Manual Verification

Create a test file `test-install.js`:

```javascript
const sdk = require('@platonai/browser4-sdk');

console.log('Testing @platonai/browser4-sdk installation...\n');

// Check main exports
const exports = [
  'PulsarClient',
  'Browser4Driver',
  'PulsarSession',
  'AgenticSession',
  'WebDriver'
];

exports.forEach(name => {
  if (sdk[name]) {
    console.log(`✅ ${name} is available`);
  } else {
    console.log(`❌ ${name} is missing`);
  }
});

console.log('\n✅ Installation successful!');
```

Run it:

```bash
node test-install.js
```

### Method 3: TypeScript Verification

For TypeScript projects, create `test-install.ts`:

```typescript
import {
  PulsarClient,
  Browser4Driver,
  PulsarSession,
  AgenticSession,
  WebDriver
} from '@platonai/browser4-sdk';

console.log('✅ TypeScript imports working!');
console.log('✅ Type definitions available!');
```

Compile and run:

```bash
npx tsc test-install.ts
node test-install.js
```

## Project Setup Examples

### JavaScript Project

```bash
mkdir my-browser4-project
cd my-browser4-project
npm init -y
npm install @platonai/browser4-sdk
```

Create `index.js`:

```javascript
const { Browser4Driver, PulsarClient, AgenticSession } = require('@platonai/browser4-sdk');

async function main() {
  const driver = new Browser4Driver();

  await driver.use(async (d) => {
    const client = new PulsarClient({ baseUrl: d.baseUrl });
    await client.createSession();
    const session = new AgenticSession(client);

    await session.open('https://example.com');
    console.log('✅ Successfully opened page!');

    await session.close();
  });
}

main().catch(console.error);
```

Run:

```bash
node index.js
```

### TypeScript Project

```bash
mkdir my-browser4-ts-project
cd my-browser4-ts-project
npm init -y
npm install @platonai/browser4-sdk
npm install --save-dev typescript @types/node
npx tsc --init
```

Create `index.ts`:

```typescript
import { Browser4Driver, PulsarClient, AgenticSession } from '@platonai/browser4-sdk';

async function main(): Promise<void> {
  const driver = new Browser4Driver();

  await driver.use(async (d) => {
    const client = new PulsarClient({ baseUrl: d.baseUrl });
    await client.createSession();
    const session = new AgenticSession(client);

    const page = await session.open('https://example.com');
    console.log(`✅ Opened: ${page.url}`);

    await session.close();
  });
}

main().catch(console.error);
```

Update `tsconfig.json`:

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "module": "commonjs",
    "esModuleInterop": true,
    "strict": true
  }
}
```

Build and run:

```bash
npx tsc
node index.js
```

## Common Issues

### Issue: "Cannot find module '@platonai/browser4-sdk'"

**Solutions:**

1. Verify installation:
   ```bash
   npm list @platonai/browser4-sdk
   ```

2. Reinstall:
   ```bash
   npm install @platonai/browser4-sdk --force
   ```

3. Clear npm cache:
   ```bash
   npm cache clean --force
   npm install
   ```

### Issue: "Module not found: Error: Can't resolve 'axios'"

This means peer dependencies aren't installed.

**Solution:**
```bash
npm install
```

### Issue: TypeScript errors about missing types

**Solutions:**

1. Install type definitions:
   ```bash
   npm install --save-dev @types/node
   ```

2. Check `tsconfig.json` includes:
   ```json
   {
     "compilerOptions": {
       "esModuleInterop": true,
       "skipLibCheck": true
     }
   }
   ```

### Issue: "Browser4.jar not found"

The SDK will automatically download Browser4.jar on first use. If this fails:

**Solutions:**

1. Check internet connection
2. Manually download from GitHub releases
3. Specify custom jar location:
   ```javascript
   const driver = new Browser4Driver({
     homeDir: '/path/to/browser4',
     autoDownload: true
   });
   ```

### Issue: "Java not found"

**Solutions:**

1. Install Java 17+:
   - Ubuntu/Debian: `sudo apt install openjdk-17-jdk`
   - macOS: `brew install openjdk@17`
   - Windows: Download from [adoptium.net](https://adoptium.net/)

2. Verify installation:
   ```bash
   java --version
   ```

3. Specify Java path:
   ```javascript
   const driver = new Browser4Driver({
     javaPath: '/path/to/java'
   });
   ```

### Issue: Port 8182 already in use

**Solution:**

Use a different port:

```javascript
const driver = new Browser4Driver({
  port: 8183
});
```

### Issue: Build errors during installation

If you see errors about TypeScript or build issues:

**Solutions:**

1. Clear build artifacts:
   ```bash
   rm -rf node_modules package-lock.json
   npm install
   ```

2. Use a specific Node version:
   ```bash
   nvm use 18
   npm install
   ```

## Updating

### Check for Updates

```bash
npm outdated @platonai/browser4-sdk
```

### Update to Latest Version

```bash
npm update @platonai/browser4-sdk
```

### Update to Specific Version

```bash
npm install @platonai/browser4-sdk@0.2.0
```

## Uninstallation

### Remove Package

```bash
npm uninstall @platonai/browser4-sdk
```

### Remove Package and Clean Up

```bash
npm uninstall @platonai/browser4-sdk
rm -rf node_modules package-lock.json
npm install
```

### Remove Browser4 Server Data

Browser4Driver stores data in `~/.browser4` by default:

```bash
rm -rf ~/.browser4
```

## Getting Help

If you encounter issues:

1. Check the [GitHub Issues](https://github.com/platonai/Browser4/issues)
2. Read the [README](./README.md)
3. Review [examples](./examples/)
4. Open a new issue with:
   - Your Node.js version (`node --version`)
   - Your npm version (`npm --version`)
   - Error messages
   - Steps to reproduce

## Next Steps

After successful installation:

1. Read the [README.md](./README.md) for API overview
2. Check [examples/](./examples/) for code samples
3. Review [PUBLISHING.md](./PUBLISHING.md) for publishing guidelines (contributors)

## Additional Resources

- [Browser4 Documentation](https://github.com/platonai/Browser4/tree/main/docs)
- [npm Documentation](https://docs.npmjs.com/)
- [Node.js Best Practices](https://github.com/goldbergyoni/nodebestpractices)
