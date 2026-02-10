#!/bin/bash

# Find the first parent directory that contains a VERSION file
APP_HOME=$(cd "$(dirname "$0")">/dev/null || exit 1; pwd)
while [[ ! -f "$APP_HOME/VERSION" && "$APP_HOME" != "/" ]]; do
  APP_HOME=$(dirname "$APP_HOME")
done
[[ -f "$APP_HOME/VERSION" ]] && cd "$APP_HOME" || exit 1

function print_usage {
  echo "Usage: test.sh [test-type] [maven-args...]"
  echo ""
  echo "Test Types:"
  echo "  fast        Run fast unit tests only"
  echo "  it          Run integration tests"
  echo "  e2e         Run end-to-end tests"
  echo "  sdk         Run SDK tests"
  echo "  core        Run core module supplementary tests"
  echo "  rest        Run REST module tests"
  echo "  all         Run all tests (integration, e2e, sdk)"
  echo ""
  echo "Examples:"
  echo "  test.sh fast               # Run fast unit tests"
  echo "  test.sh it                 # Run integration tests"
  echo "  test.sh e2e                # Run end-to-end tests"
  echo "  test.sh sdk                # Run SDK tests"
  echo "  test.sh all                # Run all tests"
  echo "  test.sh it -pl pulsar-core # Run integration tests for pulsar-core only"
  exit 1
}

# Maven command
MvnCmd="./mvnw"

# Validate Maven wrapper exists and is executable
if [[ ! -x "$APP_HOME/mvnw" ]]; then
    echo "Error: Maven wrapper not found or not executable at $APP_HOME/mvnw"
    exit 1
fi

# Default test type is fast
TestType="fast"
AdditionalMvnArgs=()

# Parse command-line arguments
if [[ $# -eq 0 ]]; then
  print_usage
fi

if [[ $# -gt 0 ]]; then
  case $1 in
    fast|it|e2e|sdk|core|rest|all)
      TestType=$1
      shift
      ;;
    -h|-help|--help)
      print_usage
      ;;
    *)
      echo "Error: Invalid argument '$1'"
      echo ""
      print_usage
      ;;
  esac
fi

# Collect remaining arguments
AdditionalMvnArgs=("$@")

# Execute tests based on type
echo "=========================================="
echo "Running $TestType tests..."
echo "=========================================="

case $TestType in
  fast)
    echo "Running fast unit tests (default behavior)..."
    $MvnCmd test "${AdditionalMvnArgs[@]}"
    ;;
  it)
    echo "Running integration tests..."
    $MvnCmd test -DrunITs=true "${AdditionalMvnArgs[@]}"
    ;;
  e2e)
    echo "Running end-to-end tests..."
    $MvnCmd test -DrunE2ETests=true "${AdditionalMvnArgs[@]}"
    ;;
  sdk)
    echo "Running SDK tests..."
    $MvnCmd test -DrunSDKTests=true "${AdditionalMvnArgs[@]}"
    ;;
  core)
    echo "Running core module supplementary tests..."
    $MvnCmd test -DrunCoreTests=true "${AdditionalMvnArgs[@]}"
    ;;
  rest)
    echo "Running REST module tests..."
    $MvnCmd test -DrunRestTests=true "${AdditionalMvnArgs[@]}"
    ;;
  all)
    echo "Running all tests (integration, e2e, sdk)..."
    $MvnCmd test -DrunITs=true -DrunE2ETests=true -DrunSDKTests=true "${AdditionalMvnArgs[@]}"
    ;;
  *)
    echo "Error: Unknown test type '$TestType'"
    print_usage
    ;;
esac

ExitCode=$?

if [[ $ExitCode -eq 0 ]]; then
  echo ""
  echo "=========================================="
  echo "✅ $TestType tests completed successfully"
  echo "=========================================="
else
  echo ""
  echo "=========================================="
  echo "❌ $TestType tests failed with exit code $ExitCode"
  echo "=========================================="
fi

exit $ExitCode
