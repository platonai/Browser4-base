# Publishing Guide for @platonai/browser4-sdk

This guide explains how to publish the Browser4 NodeJS SDK to npm.

## Prerequisites

Before publishing, ensure you have:

1. **npm Account**: Create an account at [npmjs.com](https://www.npmjs.com/)
2. **npm Login**: Run `npm login` to authenticate
3. **Organization Access**: Request access to the `@platonai` organization (if applicable)
4. **Node.js**: Version 16 or higher
5. **npm**: Version 7 or higher

## Pre-Publishing Checklist

Before each release, verify:

- [ ] All tests pass: `npm test`
- [ ] Code is linted: `npm run lint`
- [ ] Build succeeds: `npm run build`
- [ ] README.md is up to date
- [ ] CHANGELOG is updated (if maintained)
- [ ] Version number is correct in package.json
- [ ] LICENSE file is present

## Publishing Steps

### 1. Prepare the Package

Install dependencies and build:

```bash
cd /path/to/Browser4/sdks/browser4-nodejs
npm install
npm run build
```

### 2. Run Tests

Ensure all tests pass:

```bash
npm test
npm run lint
```

### 3. Check Package Contents

Preview what will be published:

```bash
npm run pack:check
```

This shows which files will be included in the package. Verify:
- `dist/` directory is included
- `README.md` is included
- `LICENSE` is included
- Source files (`src/`, `tests/`) are excluded
- `node_modules/` is excluded

### 4. Update Version

Choose the appropriate version bump:

```bash
# For bug fixes (0.1.0 -> 0.1.1)
npm run version:patch

# For new features (0.1.0 -> 0.2.0)
npm run version:minor

# For breaking changes (0.1.0 -> 1.0.0)
npm run version:major
```

Or manually update `package.json` version.

### 5. Test Publish (Dry Run)

Test the publishing process without actually publishing:

```bash
npm run publish:test
```

This performs all pre-publish checks without uploading to npm.

### 6. Publish to npm

Publish the package:

```bash
npm run publish:public
```

Or directly:

```bash
npm publish --access public
```

The `--access public` flag is required for scoped packages (@platonai/browser4-sdk).

### 7. Verify Publication

After publishing:

1. Check the package on npm: https://www.npmjs.com/package/@platonai/browser4-sdk
2. Verify installation works:
   ```bash
   npm install @platonai/browser4-sdk
   ```
3. Test in a new project to ensure it works correctly

## Version Management

### Semantic Versioning

Follow [semver](https://semver.org/):

- **MAJOR** (x.0.0): Breaking changes
- **MINOR** (0.x.0): New features, backward compatible
- **PATCH** (0.0.x): Bug fixes, backward compatible

### Pre-release Versions

For beta/alpha releases:

```bash
# Create beta version
npm version prerelease --preid=beta
# Example: 0.1.0 -> 0.1.1-beta.0

# Publish with beta tag
npm publish --tag beta
```

Install pre-release versions:

```bash
npm install @platonai/browser4-sdk@beta
```

## Automated Publishing with CI/CD

The repository includes GitHub Actions workflows for automated publishing:

### Workflow: `publish-nodejs-sdk.yml`

Automatically publishes the SDK when a tag is pushed or manually triggered.

**Trigger by Git Tag:**

```bash
# Create and push a version tag
git tag sdk-nodejs-v0.1.0
git push origin sdk-nodejs-v0.1.0
```

**Manual Trigger:**

1. Go to GitHub Actions tab
2. Select "Publish NodeJS SDK" workflow
3. Click "Run workflow"
4. Enter version and npm tag (latest/beta/rc/next)

**What the workflow does:**

1. ✅ Runs linting and tests
2. ✅ Builds the package
3. ✅ Verifies build output
4. ✅ Publishes to npm
5. ✅ Creates GitHub release

### Workflow: `nodejs-sdk-test.yml`

Automatically tests the SDK on pull requests and pushes.

**Tests across:**
- Operating Systems: Ubuntu, macOS, Windows
- Node.js versions: 16, 18, 20

**What it tests:**
1. Linting
2. Unit tests
3. Build verification
4. Package installation

### Setting Up CI/CD

1. Create an npm access token:
   - Go to npmjs.com → Account → Access Tokens
   - Create an "Automation" token

2. Add token to GitHub secrets:
   - Repository Settings → Secrets → New repository secret
   - Name: `NPM_TOKEN`
   - Value: Your npm token

3. Create and push a version tag:
   ```bash
   git tag sdk-nodejs-v0.1.0
   git push origin sdk-nodejs-v0.1.0
   ```

The workflow will automatically:
- Run tests
- Build the package
- Publish to npm
- Create a GitHub release

## Unpublishing

⚠️ **Warning**: Unpublishing is generally discouraged. Once published, prefer deprecating instead.

### Deprecate a Version

```bash
npm deprecate @platonai/browser4-sdk@0.1.0 "This version has critical bugs, use 0.1.1 instead"
```

### Unpublish (within 72 hours)

Only possible within 72 hours of publishing:

```bash
npm unpublish @platonai/browser4-sdk@0.1.0
```

## Troubleshooting

### "You do not have permission to publish"

- Ensure you're logged in: `npm whoami`
- Verify organization access: `npm org ls @platonai`
- Check package.json has correct name: `@platonai/browser4-sdk`

### "Version already published"

- Update version in package.json
- Or use: `npm version patch`

### "Package not found after publishing"

- Wait a few minutes for npm registry to update
- Clear npm cache: `npm cache clean --force`
- Try with full URL: `npm install @platonai/browser4-sdk@latest`

### Build Errors

```bash
# Clean and rebuild
npm run clean
npm install
npm run build
```

## Package Maintenance

### Regular Tasks

1. **Update Dependencies**:
   ```bash
   npm outdated
   npm update
   ```

2. **Security Audits**:
   ```bash
   npm audit
   npm audit fix
   ```

3. **Monitor Usage**:
   - Check download stats: https://npm-stat.com/charts.html?package=@platonai/browser4-sdk

## Additional Resources

- [npm Publishing Documentation](https://docs.npmjs.com/packages-and-modules/contributing-packages-to-the-registry)
- [Semantic Versioning](https://semver.org/)
- [npm CLI Commands](https://docs.npmjs.com/cli/v9/commands)
- [Package.json Reference](https://docs.npmjs.com/cli/v9/configuring-npm/package-json)

## Support

For issues with publishing:
- Open an issue: https://github.com/platonai/Browser4/issues
- Contact the Browser4 team
