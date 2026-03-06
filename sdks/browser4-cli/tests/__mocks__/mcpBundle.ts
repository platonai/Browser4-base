/**
 * Mock for mcpBundle — provides zod directly from the zod package
 * instead of going through the bundled mcpBundleImpl.
 */
import * as zod from 'zod';
export const z = zod;
