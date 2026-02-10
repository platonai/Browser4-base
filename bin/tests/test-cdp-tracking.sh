#!/bin/bash
# Test script to verify CDP URL tracking and browser reuse

echo "Testing CDP URL tracking and browser reuse..."
echo ""

# Run a simple test that launches Chrome and verifies CDP URL file creation
cd /home/runner/work/Browser4/Browser4

echo "Step 1: Compiling the test..."
./mvnw -q -pl pulsar-core/pulsar-browser -am test-compile

echo ""
echo "Step 2: Running CDP URL tracking test..."
./mvnw -q -pl pulsar-core/pulsar-browser test \
  -Dtest=ChromeImplLauncherTest#testCdpUrlTracking \
  -D"surefire.failIfNoSpecifiedTests=false" \
  2>&1 | grep -E "(CDP|PASSED|FAILED|ERROR)"

echo ""
echo "Step 3: Checking for CDP URL files in test context directories..."
find /tmp -name "cdp-url" -type f 2>/dev/null | head -5

echo ""
echo "Step 4: Showing sample CDP URL content (if exists)..."
CDP_FILE=$(find /tmp -name "cdp-url" -type f 2>/dev/null | head -1)
if [ -n "$CDP_FILE" ]; then
  echo "Found CDP URL file: $CDP_FILE"
  echo "Content:"
  cat "$CDP_FILE"
else
  echo "No CDP URL files found (test may not have run with Chrome)"
fi

echo ""
echo "Test verification complete!"
