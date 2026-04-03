#!/usr/bin/env python3
"""
Version bumping script for browser4-python.

This script updates the version number in all relevant files:
- pyproject.toml
- setup.cfg
- browser4/__init__.py

Usage:
    python scripts/bump-version.py 0.2.0
    python scripts/bump-version.py 0.1.1
    python scripts/bump-version.py 1.0.0-rc.1
"""

import re
import sys
from pathlib import Path


def validate_version(version: str) -> bool:
    """Validate version string against semantic versioning."""
    pattern = r'^\d+\.\d+\.\d+(-[a-zA-Z0-9.]+)?$'
    return bool(re.match(pattern, version))


def update_pyproject_toml(sdk_dir: Path, new_version: str) -> bool:
    """Update version in pyproject.toml."""
    filepath = sdk_dir / "pyproject.toml"
    
    if not filepath.exists():
        print(f"❌ File not found: {filepath}")
        return False
    
    content = filepath.read_text()
    pattern = r'^version = "[^"]+"'
    replacement = f'version = "{new_version}"'
    
    new_content = re.sub(pattern, replacement, content, flags=re.MULTILINE)
    
    if new_content == content:
        print(f"⚠️  No changes in {filepath.name}")
        return False
    
    filepath.write_text(new_content)
    print(f"✓ Updated {filepath.name}: version = \"{new_version}\"")
    return True


def update_setup_cfg(sdk_dir: Path, new_version: str) -> bool:
    """Update version in setup.cfg."""
    filepath = sdk_dir / "setup.cfg"
    
    if not filepath.exists():
        print(f"❌ File not found: {filepath}")
        return False
    
    content = filepath.read_text()
    pattern = r'^version = .+'
    replacement = f'version = {new_version}'
    
    new_content = re.sub(pattern, replacement, content, flags=re.MULTILINE)
    
    if new_content == content:
        print(f"⚠️  No changes in {filepath.name}")
        return False
    
    filepath.write_text(new_content)
    print(f"✓ Updated {filepath.name}: version = {new_version}")
    return True


def update_init_py(sdk_dir: Path, new_version: str) -> bool:
    """Update version in browser4/__init__.py."""
    filepath = sdk_dir / "browser4" / "__init__.py"
    
    if not filepath.exists():
        print(f"❌ File not found: {filepath}")
        return False
    
    content = filepath.read_text()
    pattern = r'^__version__ = "[^"]+"'
    replacement = f'__version__ = "{new_version}"'
    
    new_content = re.sub(pattern, replacement, content, flags=re.MULTILINE)
    
    if new_content == content:
        print(f"⚠️  No changes in {filepath.name}")
        return False
    
    filepath.write_text(new_content)
    print(f"✓ Updated {filepath.name}: __version__ = \"{new_version}\"")
    return True


def get_current_version(sdk_dir: Path) -> str:
    """Get current version from pyproject.toml."""
    filepath = sdk_dir / "pyproject.toml"
    
    if not filepath.exists():
        return "unknown"
    
    content = filepath.read_text()
    match = re.search(r'^version = "([^"]+)"', content, re.MULTILINE)
    
    if match:
        return match.group(1)
    
    return "unknown"


def main():
    if len(sys.argv) != 2:
        print("Usage: python bump-version.py <new_version>")
        print("Example: python bump-version.py 0.2.0")
        sys.exit(1)
    
    new_version = sys.argv[1]
    
    # Validate version format
    if not validate_version(new_version):
        print(f"❌ Invalid version format: {new_version}")
        print("Version must follow semantic versioning: MAJOR.MINOR.PATCH[-SUFFIX]")
        print("Examples: 0.1.0, 0.2.0, 1.0.0-rc.1")
        sys.exit(1)
    
    # Determine SDK directory (script is in sdks/browser4-python/scripts)
    script_dir = Path(__file__).parent
    sdk_dir = script_dir.parent
    
    # Get current version
    current_version = get_current_version(sdk_dir)
    
    print(f"Bumping version: {current_version} → {new_version}")
    print()
    
    # Update all files
    success_count = 0
    
    if update_pyproject_toml(sdk_dir, new_version):
        success_count += 1
    
    if update_setup_cfg(sdk_dir, new_version):
        success_count += 1
    
    if update_init_py(sdk_dir, new_version):
        success_count += 1
    
    print()
    
    if success_count == 0:
        print("❌ No files were updated")
        sys.exit(1)
    elif success_count < 3:
        print(f"⚠️  Only {success_count}/3 files were updated")
        sys.exit(1)
    else:
        print(f"✓ Successfully updated version to {new_version} in all files")
        print()
        print("Next steps:")
        print(f"  1. Update CHANGELOG.md with changes for version {new_version}")
        print(f"  2. Commit changes: git add . && git commit -m 'chore(python-sdk): Bump version to {new_version}'")
        print(f"  3. Create tag: git tag python-sdk-v{new_version}")
        print(f"  4. Push: git push origin main python-sdk-v{new_version}")


if __name__ == "__main__":
    main()
