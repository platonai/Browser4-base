#!/bin/bash

# Browser4 Test Runner Script
# Comprehensive test execution with multiple options

# Find the project root directory (contains VERSION file)
APP_HOME=$(cd "$(dirname "$0")"/.. >/dev/null || exit 1; pwd)
cd "$APP_HOME" || exit 1

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

function print_usage {
  echo "Usage: test.sh [OPTIONS]"
  echo ""
  echo "Options:"
  echo "  -h, --help          Show this help message"
  echo "  -clean              Clean before testing"
  echo "  -integration        Include integration tests"
  echo "  -e2e                Include E2E tests (implies -integration)"
  echo "  -module MODULE      Test specific module (e.g., pulsar-core)"
  echo "  -test CLASS         Test specific class or pattern"
  echo "  -coverage           Generate coverage report"
  echo "  -parallel           Enable parallel test execution"
  echo "  -verbose            Enable verbose output"
  echo "  -groups TAGS        Include only specific test groups (comma-separated)"
  echo "  -exclude-groups TAGS Exclude specific test groups (comma-separated)"
  echo ""
  echo "Examples:"
  echo "  test.sh                           # Run unit tests only"
  echo "  test.sh -integration              # Run unit + integration tests"
  echo "  test.sh -e2e                      # Run all tests including E2E"
  echo "  test.sh -module pulsar-core       # Test specific module"
  echo "  test.sh -test MyTest              # Run specific test class"
  echo "  test.sh -coverage                 # Generate coverage report"
  echo "  test.sh -clean -integration       # Clean and run integration tests"
  echo "  test.sh -parallel                 # Run tests in parallel"
  echo "  test.sh -groups SmokeTest         # Run only smoke tests"
  echo ""
  exit 1
}

# Validate Maven wrapper exists
if [[ ! -x "$APP_HOME/mvnw" ]]; then
    echo -e "${RED}Error: Maven wrapper not found or not executable at $APP_HOME/mvnw${NC}"
    exit 1
fi

MvnCmd="./mvnw"

# Initialize variables
PerformClean=false
IncludeIntegration=false
IncludeE2E=false
SpecificModule=""
SpecificTest=""
GenerateCoverage=false
EnableParallel=false
VerboseOutput=false
IncludeGroups=""
ExcludeGroups="TimeConsumingTest,ExternalServiceTest"

# Parse command-line arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    -h|--help)
      print_usage
      ;;
    -clean)
      PerformClean=true
      shift
      ;;
    -integration)
      IncludeIntegration=true
      shift
      ;;
    -e2e)
      IncludeE2E=true
      IncludeIntegration=true
      shift
      ;;
    -module)
      SpecificModule="$2"
      shift 2
      ;;
    -test)
      SpecificTest="$2"
      shift 2
      ;;
    -coverage)
      GenerateCoverage=true
      shift
      ;;
    -parallel)
      EnableParallel=true
      shift
      ;;
    -verbose)
      VerboseOutput=true
      shift
      ;;
    -groups)
      IncludeGroups="$2"
      shift 2
      ;;
    -exclude-groups)
      ExcludeGroups="$2"
      shift 2
      ;;
    *)
      echo -e "${RED}Unknown option: $1${NC}"
      print_usage
      ;;
  esac
done

# Build Maven command
MvnArgs=()

# Add clean if requested
if $PerformClean; then
  MvnArgs+=("clean")
fi

# Add test goal
MvnArgs+=("test")

# Add module specification
if [[ -n "$SpecificModule" ]]; then
  MvnArgs+=("-pl" "$SpecificModule" "-am")
  # When using -am, we need this flag to avoid "No tests were executed" error
  MvnArgs+=("-Dsurefire.failIfNoSpecifiedTests=false")
fi

# Add specific test class
if [[ -n "$SpecificTest" ]]; then
  MvnArgs+=("-Dtest=$SpecificTest")
fi

# Handle test groups
if $IncludeE2E; then
  # Include all tests
  ExcludeGroups=""
elif $IncludeIntegration; then
  # Exclude only E2E and time-consuming
  ExcludeGroups="E2ETest,TimeConsumingTest"
fi

# Set excluded groups
if [[ -n "$ExcludeGroups" ]]; then
  MvnArgs+=("-DexcludedGroups=$ExcludeGroups")
else
  # Empty string to include all
  MvnArgs+=("-DexcludedGroups=")
fi

# Set included groups if specified
if [[ -n "$IncludeGroups" ]]; then
  MvnArgs+=("-Dgroups=$IncludeGroups")
fi

# Add parallel execution
if $EnableParallel; then
  MvnArgs+=("-Dsurefire.parallel=methods")
  MvnArgs+=("-Dsurefire.threadCount=4")
  MvnArgs+=("-Dsurefire.perCoreThreadCount=true")
fi

# Add coverage if requested
if $GenerateCoverage; then
  MvnArgs+=("jacoco:report")
fi

# Add verbose output
if $VerboseOutput; then
  MvnArgs+=("-X")
else
  MvnArgs+=("-B")  # Batch mode
fi

# Print configuration
echo -e "${BLUE}╔═══════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║          Browser4 Test Execution                    ║${NC}"
echo -e "${BLUE}╚═══════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${YELLOW}Configuration:${NC}"
echo "  Clean: $PerformClean"
echo "  Integration: $IncludeIntegration"
echo "  E2E: $IncludeE2E"
[[ -n "$SpecificModule" ]] && echo "  Module: $SpecificModule"
[[ -n "$SpecificTest" ]] && echo "  Test: $SpecificTest"
echo "  Coverage: $GenerateCoverage"
echo "  Parallel: $EnableParallel"
[[ -n "$ExcludeGroups" ]] && echo "  Excluded Groups: $ExcludeGroups"
[[ -n "$IncludeGroups" ]] && echo "  Included Groups: $IncludeGroups"
echo ""
echo -e "${YELLOW}Executing:${NC} $MvnCmd ${MvnArgs[*]}"
echo ""

# Record start time
StartTime=$(date +%s)

# Execute Maven test command
$MvnCmd "${MvnArgs[@]}"
ExitCode=$?

# Record end time
EndTime=$(date +%s)
Duration=$((EndTime - StartTime))

# Print summary
echo ""
echo -e "${BLUE}╔═══════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║          Test Execution Summary                      ║${NC}"
echo -e "${BLUE}╚═══════════════════════════════════════════════════════╝${NC}"
echo ""

if [[ $ExitCode -eq 0 ]]; then
  echo -e "${GREEN}✓ Tests passed successfully!${NC}"
else
  echo -e "${RED}✗ Tests failed!${NC}"
fi

echo ""
echo "Duration: ${Duration}s"

# Show coverage report location if generated
if $GenerateCoverage; then
  echo ""
  echo -e "${YELLOW}Coverage reports:${NC}"
  if [[ -n "$SpecificModule" ]]; then
    echo "  - $SpecificModule/target/site/jacoco/index.html"
  else
    echo "  - target/site/jacoco-aggregate/index.html"
  fi
fi

# Show test reports location
echo ""
echo -e "${YELLOW}Test reports:${NC}"
if [[ -n "$SpecificModule" ]]; then
  echo "  - $SpecificModule/target/surefire-reports/"
else
  echo "  - */target/surefire-reports/"
fi

echo ""

exit $ExitCode
