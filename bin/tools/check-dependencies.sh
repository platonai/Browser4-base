#!/bin/bash
# Dependency Health Check Script for Browser4
# 依赖健康检查脚本
# Usage: ./check-dependencies.sh [--full]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
REPORT_DIR="$PROJECT_ROOT/target/dependency-reports"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Create report directory
mkdir -p "$REPORT_DIR"

echo -e "${BLUE}==================================${NC}"
echo -e "${BLUE}Browser4 Dependency Health Check${NC}"
echo -e "${BLUE}==================================${NC}"
echo ""

# Function to print section header
print_header() {
    echo ""
    echo -e "${BLUE}>>> $1${NC}"
    echo "-----------------------------------"
}

# Function to check command availability
check_command() {
    if ! command -v "$1" &> /dev/null; then
        echo -e "${RED}Error: $1 is not installed${NC}"
        return 1
    fi
    return 0
}

# Change to project root
cd "$PROJECT_ROOT"

# Check prerequisites
print_header "Checking Prerequisites"
check_command "java" || exit 1
check_command "mvn" || check_command "mvnw" || exit 1

# Use Maven Wrapper if available
MVN_CMD="./mvnw"
if [ ! -f "$MVN_CMD" ]; then
    MVN_CMD="mvn"
fi

echo -e "${GREEN}✓ Java version: $(java -version 2>&1 | head -n 1)${NC}"
echo -e "${GREEN}✓ Maven command: $MVN_CMD${NC}"

# 1. Check for dependency updates
print_header "1. Checking for Dependency Updates"
echo "Generating dependency update report..."
$MVN_CMD versions:display-dependency-updates -q > "$REPORT_DIR/dependency-updates.txt" 2>&1 || true

if grep -q "No dependencies in Dependencies" "$REPORT_DIR/dependency-updates.txt"; then
    echo -e "${GREEN}✓ All dependencies are up to date${NC}"
else
    echo -e "${YELLOW}⚠ Updates available. See: $REPORT_DIR/dependency-updates.txt${NC}"
    echo ""
    echo "Summary of updates:"
    grep -A 2 "The following dependencies in Dependencies have newer versions:" "$REPORT_DIR/dependency-updates.txt" | head -20 || echo "Check full report for details"
fi

# 2. Check for plugin updates
print_header "2. Checking for Plugin Updates"
echo "Generating plugin update report..."
$MVN_CMD versions:display-plugin-updates -q > "$REPORT_DIR/plugin-updates.txt" 2>&1 || true

if grep -q "All plugins have a version specified" "$REPORT_DIR/plugin-updates.txt" || \
   grep -q "All plugins with a version specified are using the latest versions" "$REPORT_DIR/plugin-updates.txt"; then
    echo -e "${GREEN}✓ All plugins are up to date${NC}"
else
    echo -e "${YELLOW}⚠ Plugin updates available. See: $REPORT_DIR/plugin-updates.txt${NC}"
fi

# 3. Check for property updates
print_header "3. Checking for Property Updates"
echo "Generating property update report..."
$MVN_CMD versions:display-property-updates -q > "$REPORT_DIR/property-updates.txt" 2>&1 || true
echo -e "${GREEN}✓ Report generated: $REPORT_DIR/property-updates.txt${NC}"

# 4. Analyze dependencies
print_header "4. Analyzing Dependency Usage"
echo "Analyzing declared dependencies..."
$MVN_CMD dependency:analyze -q > "$REPORT_DIR/dependency-analysis.txt" 2>&1 || true

if grep -q "Used undeclared dependencies" "$REPORT_DIR/dependency-analysis.txt"; then
    echo -e "${YELLOW}⚠ Found used undeclared dependencies${NC}"
fi

if grep -q "Unused declared dependencies" "$REPORT_DIR/dependency-analysis.txt"; then
    echo -e "${YELLOW}⚠ Found unused declared dependencies${NC}"
fi

echo -e "${GREEN}✓ Analysis complete. See: $REPORT_DIR/dependency-analysis.txt${NC}"

# 5. Check for duplicate dependencies
print_header "5. Checking for Duplicate Dependencies"
echo "Generating dependency tree..."
$MVN_CMD dependency:tree -q > "$REPORT_DIR/dependency-tree.txt" 2>&1 || true

# Look for version conflicts
if grep -q "omitted for conflict" "$REPORT_DIR/dependency-tree.txt"; then
    echo -e "${YELLOW}⚠ Version conflicts detected in dependency tree${NC}"
    echo "Conflicts found:"
    grep "omitted for conflict" "$REPORT_DIR/dependency-tree.txt" | head -10
    echo ""
    echo "See full report: $REPORT_DIR/dependency-tree.txt"
else
    echo -e "${GREEN}✓ No obvious version conflicts detected${NC}"
fi

# 6. Security vulnerability check (if enabled)
if [ "$1" == "--full" ]; then
    print_header "6. Security Vulnerability Check"
    echo "Running OWASP Dependency Check (this may take several minutes)..."
    echo -e "${YELLOW}Note: First run will download CVE database${NC}"
    
    if $MVN_CMD dependency-check:check -q > "$REPORT_DIR/security-check.log" 2>&1; then
        echo -e "${GREEN}✓ No high severity vulnerabilities found${NC}"
    else
        echo -e "${RED}✗ Security vulnerabilities detected!${NC}"
        echo "See report: target/dependency-check-report.html"
    fi
else
    echo ""
    echo -e "${YELLOW}Tip: Run with --full flag to include security vulnerability check${NC}"
fi

# 7. Generate summary report
print_header "7. Summary Report"

SUMMARY_FILE="$REPORT_DIR/summary.txt"
{
    echo "Browser4 Dependency Health Report"
    echo "Generated: $(date)"
    echo ""
    echo "Report Location: $REPORT_DIR"
    echo ""
    echo "Files Generated:"
    echo "  - dependency-updates.txt: Available dependency updates"
    echo "  - plugin-updates.txt: Available plugin updates"
    echo "  - property-updates.txt: Available property updates"
    echo "  - dependency-analysis.txt: Dependency usage analysis"
    echo "  - dependency-tree.txt: Full dependency tree"
    if [ "$1" == "--full" ]; then
        echo "  - security-check.log: Security vulnerability scan log"
        echo "  - ../dependency-check-report.html: Detailed security report"
    fi
    echo ""
    echo "Quick Stats:"
    echo "  Total POM files: $(find . -name pom.xml | wc -l)"
    echo "  Direct dependencies: $(grep -h "<dependency>" */pom.xml 2>/dev/null | wc -l)"
} > "$SUMMARY_FILE"

cat "$SUMMARY_FILE"

# 8. Recommendations
print_header "8. Recommendations"

echo ""
echo "Next Steps:"
echo "  1. Review dependency updates in $REPORT_DIR/dependency-updates.txt"
echo "  2. Prioritize security updates (run with --full to check)"
echo "  3. Create upgrade plan based on priority (see docs/dependency-upgrade-plan.md)"
echo "  4. Test upgrades in feature branch before merging"
echo ""
echo "Useful Commands:"
echo "  # Update specific dependency:"
echo "  $MVN_CMD versions:use-dep-version -Dincludes=groupId:artifactId -DdepVersion=x.y.z"
echo ""
echo "  # Update all properties to latest:"
echo "  $MVN_CMD versions:update-properties"
echo ""
echo "  # Run full security scan:"
echo "  $MVN_CMD dependency-check:check"
echo ""

echo -e "${GREEN}==================================${NC}"
echo -e "${GREEN}Health check completed!${NC}"
echo -e "${GREEN}==================================${NC}"
echo ""
echo -e "Reports saved to: ${BLUE}$REPORT_DIR${NC}"
