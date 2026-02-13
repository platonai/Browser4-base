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
  echo "  sdk         Run Kotlin SDK tests"
  echo "  python-sdk  Run Python SDK tests"
  echo "  nodejs-sdk  Run NodeJS SDK tests"
  echo "  core        Run core module supplementary tests"
  echo "  rest        Run REST module tests"
  echo "  all         Run all tests (integration, e2e, sdk)"
  echo ""
  echo "Examples:"
  echo "  test.sh fast                       # Run fast unit tests"
  echo "  test.sh it                         # Run integration tests"
  echo "  test.sh e2e                        # Run end-to-end tests"
  echo "  test.sh sdk                        # Run Kotlin SDK tests"
  echo "  test.sh python-sdk                 # Run Python SDK tests"
  echo "  test.sh python-sdk -m integration  # Run Python SDK integration tests only"
  echo "  test.sh nodejs-sdk                 # Run NodeJS SDK tests"
  echo "  test.sh nodejs-sdk --coverage      # Run NodeJS SDK tests with coverage"
  echo "  test.sh all                        # Run all tests"
  echo "  test.sh it -pl pulsar-core         # Run integration tests for pulsar-core only"
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
    fast|it|e2e|sdk|python-sdk|nodejs-sdk|core|rest|all)
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
    $MvnCmd test -DrunE2ETests=true -P all-modules -pl pulsar-tests,pulsar-tests/pulsar-e2e-tests -am "${AdditionalMvnArgs[@]}"
    ;;
  sdk)
    echo "Running SDK tests..."
    $MvnCmd test -DrunSDKTests=true -P all-modules -pl sdks/kotlin-sdk-tests -am "${AdditionalMvnArgs[@]}"
    ;;
  python-sdk)
    echo "Running Python SDK tests..."
    PythonSdkDir="$APP_HOME/sdks/browser4-sdk-python"

    if [[ ! -d "$PythonSdkDir" ]]; then
      echo "Error: Python SDK directory not found at $PythonSdkDir"
      exit 1
    fi

    # Check if Python is available
    if ! command -v python3 &> /dev/null; then
      echo "Error: python3 is not installed or not in PATH"
      exit 1
    fi

    cd "$PythonSdkDir"
    echo "Working directory: $(pwd)"

    # Check if venv exists and activate it
    if [[ -d "$PythonSdkDir/venv" ]]; then
      echo "Activating virtual environment..."
      source "$PythonSdkDir/venv/bin/activate"
    fi

    # Check if pytest is available
    if ! python3 -m pytest --version &> /dev/null; then
      echo "Error: pytest is not installed. Install it with: pip install pytest"
      echo "Or install all dev dependencies with: pip install -e \".[dev]\" in $PythonSdkDir"
      exit 1
    fi

    python3 -m pytest "${AdditionalMvnArgs[@]}"
    ExitCode=$?
    cd "$APP_HOME"
    exit $ExitCode
    ;;
  nodejs-sdk)
    echo "Running NodeJS SDK tests..."
    NodejsSdkDir="$APP_HOME/sdks/browser4-sdk-nodejs"

    if [[ ! -d "$NodejsSdkDir" ]]; then
      echo "Error: NodeJS SDK directory not found at $NodejsSdkDir"
      exit 1
    fi

    # Check if Node.js is available
    if ! command -v node &> /dev/null; then
      echo "Error: node is not installed or not in PATH"
      exit 1
    fi

    cd "$NodejsSdkDir"
    echo "Working directory: $(pwd)"

    # Check if node_modules exists
    if [[ ! -d "$NodejsSdkDir/node_modules" ]]; then
      echo "Installing dependencies..."
      npm install
      if [[ $? -ne 0 ]]; then
        echo "Error: Failed to install dependencies"
        cd "$APP_HOME"
        exit 1
      fi
    fi

    # Check if jest is available
    if [[ ! -f "$NodejsSdkDir/node_modules/.bin/jest" ]]; then
      echo "Error: jest is not installed. Install it with: npm install"
      cd "$APP_HOME"
      exit 1
    fi

    npm test -- "${AdditionalMvnArgs[@]}"
    ExitCode=$?
    cd "$APP_HOME"
    exit $ExitCode
    ;;
  core)
    echo "Running core module supplementary tests..."
    $MvnCmd test -DrunCoreTests=true -Ppulsar-core-tests -pl pulsar-core,pulsar-core/pulsar-core-tests -am "${AdditionalMvnArgs[@]}"
    ;;
  rest)
    echo "Running REST module tests..."
    $MvnCmd test -DrunRestTests=true "${AdditionalMvnArgs[@]}"
    ;;
  all)
    echo "Running all tests (integration, e2e, sdk)..."
    $MvnCmd test -Pall-modules -DrunITs=true -DrunE2ETests=true -DrunSDKTests=true "${AdditionalMvnArgs[@]}"
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
