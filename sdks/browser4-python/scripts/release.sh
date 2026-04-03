#!/bin/bash
# Manual release script for browser4-python
# This script automates the manual release process as documented in RELEASE_PLAN.md

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

VERSION=$1
DRY_RUN=${2:-false}

# Script directory and SDK directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SDK_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

# Helper functions
info() {
    echo -e "${BLUE}ℹ ${NC}$1"
}

success() {
    echo -e "${GREEN}✓${NC} $1"
}

warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

error() {
    echo -e "${RED}✗${NC} $1"
}

# Check if version is provided
if [ -z "$VERSION" ]; then
    error "Version is required"
    echo "Usage: ./release.sh <version> [dry_run]"
    echo "Example: ./release.sh 0.1.0"
    echo "         ./release.sh 0.1.0 true  # TestPyPI only"
    exit 1
fi

# Validate version format
if ! echo "$VERSION" | grep -qE '^[0-9]+\.[0-9]+\.[0-9]+(-[a-zA-Z0-9.]+)?$'; then
    error "Invalid version format: $VERSION"
    echo "Version must follow semantic versioning: MAJOR.MINOR.PATCH[-SUFFIX]"
    echo "Examples: 0.1.0, 0.2.0, 1.0.0-rc.1"
    exit 1
fi

echo ""
info "🚀 Starting release process for version $VERSION"
if [ "$DRY_RUN" = "true" ]; then
    warning "DRY RUN MODE: Will only upload to TestPyPI"
fi
echo ""

# Change to SDK directory
cd "$SDK_DIR"

# Step 1: Update version numbers
info "📝 Updating version numbers..."
python3 scripts/bump-version.py "$VERSION"
echo ""

# Step 2: Run tests
info "🧪 Running tests..."
if command -v uv &> /dev/null; then
    uv run pytest -m "not integration" -v
else
    pytest -m "not integration" -v
fi
success "Tests passed"
echo ""

# Step 3: Clean and build
info "📦 Building package..."
rm -rf dist/ build/ *.egg-info browser4_python.egg-info

if command -v uv &> /dev/null; then
    uv run python -m build
else
    python -m build
fi
success "Package built"
echo ""

# Step 4: Check package
info "🔍 Checking package..."
twine check dist/*
success "Package check passed"
echo ""

# Step 5: Test local installation
info "🧪 Testing local installation..."
TEMP_VENV="/tmp/test-install-$$"
python3 -m venv "$TEMP_VENV"
source "$TEMP_VENV/bin/activate"

pip install dist/*.whl > /dev/null 2>&1
INSTALLED_VERSION=$(python -c "import browser4; print(browser4.__version__)")

if [ "$INSTALLED_VERSION" = "$VERSION" ]; then
    success "Local installation test passed (version: $INSTALLED_VERSION)"
else
    error "Version mismatch: expected $VERSION, got $INSTALLED_VERSION"
    deactivate
    rm -rf "$TEMP_VENV"
    exit 1
fi

deactivate
rm -rf "$TEMP_VENV"
echo ""

# Step 6: Upload
if [ "$DRY_RUN" = "true" ]; then
    info "🧪 Uploading to TestPyPI..."
    twine upload --repository testpypi dist/*
    success "Test upload complete!"
    echo ""
    info "📦 Install with:"
    echo "    pip install --index-url https://test.pypi.org/simple/ --extra-index-url https://pypi.org/simple/ browser4-python==$VERSION"
else
    echo ""
    warning "⚠️  You are about to upload to PyPI. This action cannot be undone!"
    read -p "   Upload to PyPI? (yes/no): " confirm
    echo ""
    
    if [ "$confirm" = "yes" ]; then
        info "📤 Uploading to PyPI..."
        twine upload dist/*
        success "Upload to PyPI complete!"
        echo ""
        
        info "🏷️  Creating git tag..."
        git add pyproject.toml setup.cfg browser4/__init__.py CHANGELOG.md
        git commit -m "chore(python-sdk): Release v$VERSION"
        git tag "python-sdk-v$VERSION"
        git push origin main "python-sdk-v$VERSION"
        success "Git tag created and pushed"
        echo ""
        
        success "✅ Release complete!"
        echo ""
        info "📦 Install with:"
        echo "    pip install browser4-python==$VERSION"
        echo ""
        info "🔗 Links:"
        echo "    PyPI: https://pypi.org/project/browser4-python/$VERSION/"
        echo "    GitHub: https://github.com/platonai/Browser4/releases/tag/python-sdk-v$VERSION"
    else
        warning "❌ Upload cancelled"
        exit 1
    fi
fi
