#!/usr/bin/env node

const path = require('node:path');
const {spawnSync} = require('node:child_process');

const jestBinary = path.join(
  __dirname,
  '..',
  'node_modules',
  'jest',
  'bin',
  'jest.js',
);

const result = spawnSync(process.execPath, [jestBinary, '--runInBand', 'tests/e2e.test.ts'], {
  stdio: 'inherit',
  env: {
    ...process.env,
    BROWSER4_CLI_E2E: 'true',
  },
});

if (result.error) {
  throw result.error;
}

process.exit(result.status ?? 1);
