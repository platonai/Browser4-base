# Release Scripts

This directory contains helper scripts for releasing browser4-sdk-python.

## Scripts

### bump-version.py

Updates version numbers across all required files.

**Usage:**
```bash
python scripts/bump-version.py <version>
```

**Example:**
```bash
# Bump to patch version
python scripts/bump-version.py 0.1.1

# Bump to minor version
python scripts/bump-version.py 0.2.0

# Pre-release version
python scripts/bump-version.py 0.2.0-rc.1
```

**What it does:**
- Updates `pyproject.toml` - `version = "X.Y.Z"`
- Updates `setup.cfg` - `version = X.Y.Z`
- Updates `browser4/__init__.py` - `__version__ = "X.Y.Z"`
- Validates semantic versioning format
- Provides next-steps guidance

### release.sh

Manual release script with safety checks.

**Usage:**
```bash
# Dry run (TestPyPI only)
./scripts/release.sh <version> true

# Production release (PyPI)
./scripts/release.sh <version>
```

**Example:**
```bash
# Test release to TestPyPI
./scripts/release.sh 0.1.0 true

# Production release to PyPI
./scripts/release.sh 0.1.0
```

**What it does:**
1. Updates version numbers (via bump-version.py)
2. Runs unit tests
3. Builds wheel and sdist packages
4. Checks package with twine
5. Tests local installation
6. Uploads to TestPyPI (dry-run) or PyPI (production)
7. Creates and pushes git tag (production only)

**Safety features:**
- Confirmation prompt before PyPI upload
- Local installation testing
- Version validation
- Automatic cleanup

## Quick Release Guide

### For Patch Releases (bug fixes)

```bash
# 1. Fix the bug and test
pytest

# 2. Update version
python scripts/bump-version.py 0.1.1

# 3. Update CHANGELOG.md
vi CHANGELOG.md

# 4. Commit changes
git add .
git commit -m "chore(python-sdk): Bump version to 0.1.1"

# 5. Test release (optional)
./scripts/release.sh 0.1.1 true

# 6. Production release
./scripts/release.sh 0.1.1
```

### For Automated Release (Recommended)

```bash
# 1. Update version
python scripts/bump-version.py 0.1.0

# 2. Update CHANGELOG.md
vi CHANGELOG.md

# 3. Commit and push
git add .
git commit -m "chore(python-sdk): Prepare release 0.1.0"
git push

# 4. Create and push tag
git tag python-sdk-v0.1.0
git push origin python-sdk-v0.1.0

# GitHub Actions will automatically:
# - Build packages
# - Run tests on multiple platforms and Python versions
# - Publish to PyPI
# - Create GitHub Release
```

## Manual Release Checklist

Before running release scripts:

- [ ] All tests pass
- [ ] Documentation updated
- [ ] CHANGELOG.md updated
- [ ] Version numbers consistent
- [ ] No uncommitted changes

After release:

- [ ] Verify PyPI page
- [ ] Test installation: `pip install browser4-sdk==X.Y.Z`
- [ ] Verify GitHub Release
- [ ] Monitor for issues

## Troubleshooting

### "Version already exists" error

PyPI doesn't allow re-uploading the same version. You must increment the version number.

```bash
# If you accidentally released 0.1.0 with issues:
python scripts/bump-version.py 0.1.1
./scripts/release.sh 0.1.1
```

### Build fails

```bash
# Clean build artifacts
rm -rf dist/ build/ *.egg-info

# Try building again
python -m build
```

### Import error after installation

Check that package name and import name match:
- Package name: `browser4-sdk` (on PyPI)
- Import name: `browser4` (in Python)

```bash
pip install browser4-sdk
python -c "import browser4"
```

## Additional Resources

- [Release Plan (Chinese)](../RELEASE_PLAN.md)
- [Release Plan (English)](../RELEASE_PLAN.en.md)
- [Quick Reference](../RELEASE_QUICKREF.md)
- [GitHub Actions Workflow](/.github/workflows/python-sdk-release.yml)
