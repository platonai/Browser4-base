#!/usr/bin/env bash

# 🔍 Find the first parent directory containing the VERSION file
APP_HOME=$(cd "$(dirname "$0")">/dev/null || exit; pwd)
while [[ ! -f "$repoRoot/VERSION" && "$repoRoot" != "/" ]]; do
  APP_HOME=$(dirname "$repoRoot")
done
[[ -f "$repoRoot/VERSION" ]] && cd "$repoRoot" || exit

echo "🔄 Updating Browser4 documentation..."
echo "📅 Current Date: $(date -u '+%Y-%m-%d %H:%M:%S UTC')"
echo "👤 User: $USER"

# Check if VERSION file exists
if [ ! -f "$repoRoot/VERSION" ]; then
  echo "❌ Error: VERSION file not found in $repoRoot"
  exit 1
fi

SNAPSHOT_VERSION=$(head -n 1 "$repoRoot/VERSION" | tr -d '\r\n')
VERSION=${SNAPSHOT_VERSION/-SNAPSHOT/}
PREFIX=$(echo "$VERSION" | cut -d'.' -f1,2)

echo "📦 Version Info:"
echo "   Snapshot: $SNAPSHOT_VERSION"
echo "   Release:  $VERSION"
echo "   Prefix:   $PREFIX"

# Files containing the version number to upgrade
VERSION_AWARE_FILES=(
  "$repoRoot/README.md"
  "$repoRoot/README.zh.md"  # Removed .delete extension
)

echo "🔍 Processing files..."
UPDATED_FILES=()

for F in "${VERSION_AWARE_FILES[@]}"; do
  if [ -e "$F" ]; then
    echo "  📄 Processing: $(basename "$F")"

    # Backup original file
    cp "$F" "$F.backup"

    # Replace SNAPSHOT versions - only exact matches
    sed -i "s/\b$SNAPSHOT_VERSION\b/$VERSION/g" "$F"

    # Find old versions with same prefix but different patch number
    OLD_VERSIONS=$(grep -oE "v?$PREFIX\.[0-9]+" "$F" | sort -u | uniq)

    for OLD_VERSION in $OLD_VERSIONS; do
      if [[ "$OLD_VERSION" != "$VERSION" && "$OLD_VERSION" != "v$VERSION" ]]; then
        echo "    🔄 Replacing $OLD_VERSION → v$VERSION"
        sed -i "s/\b$OLD_VERSION\b/v$VERSION/g" "$F"
      fi
    done

    # Check if file was actually modified
    if ! cmp -s "$F" "$F.backup"; then
      UPDATED_FILES+=("$F")
    fi

    # Remove backup
    rm "$F.backup"
  else
    echo "  ⚠️  File not found: $F"
  fi
done

if [ ${#UPDATED_FILES[@]} -eq 0 ]; then
  echo "ℹ️  No files were updated."
  exit 0
fi

echo "✅ Documentation updated with version v$VERSION"
echo "📝 Modified files:"
for file in "${UPDATED_FILES[@]}"; do
  echo "   - $(basename "$file")"
done

echo ""
echo "🔍 Please review the changes before committing:"
echo "   git diff"
echo ""
echo "📤 To commit and push changes:"
echo "   git add ${UPDATED_FILES[*]}"
echo "   git commit -m 'docs: update documentation for version v$VERSION'"
echo "   git push origin master"

git add -- "${UPDATED_FILES[@]}"
git commit -m "docs: update documentation for version v$VERSION"
git push
