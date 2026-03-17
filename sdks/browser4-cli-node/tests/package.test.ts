import * as fs from 'fs';
import * as path from 'path';

describe('package entrypoints', () => {
    it('points the CLI bin and main entry to program.js', () => {
        const packageJsonPath = path.resolve(__dirname, '..', 'package.json');
        const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf-8')) as {
            bin?: Record<string, string>;
            main?: string;
        };

        expect(packageJson.bin?.['b4-playwright-cli']).toBe('dist/program.js');
        expect(packageJson.main).toBe('dist/program.js');
    });
});
