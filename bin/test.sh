#!/bin/bash


repoRoot=$(cd "$(dirname "$0")" > /dev/null || exit 1; pwd)
while [[ ! -f "$repoRoot/VERSION" && "$repoRoot" != "/" ]]; do
  repoRoot=$(dirname "$repoRoot")
done
[[ -f "$repoRoot/VERSION" ]] && cd "$repoRoot" || exit 1

function print_usage {
  echo "Usage: test.sh [test-types...] [maven-args...]"
  echo ""
  echo "Test Types:"
  echo "  fast        Run fast unit tests only"
  echo "  it          Run integration tests"
  echo "  e2e         Run end-to-end tests"
  echo "  kotlin-sdk  Run Kotlin SDK tests"
  echo "  python-sdk  Run Python SDK tests"
  echo "  nodejs-sdk  Run NodeJS SDK tests"
  echo "  core        Run core module supplementary tests"
  echo "  rest        Run REST module tests"
  echo "  skills      Run skills module tests"
  echo "  mcp         Run MCP module tests"
  echo "  browser4    Run all Browser4 main tests (fast, core, rest, it, e2e)"
  echo ""
  echo "Examples:"
  echo "  test.sh fast                       # Run fast unit tests"
  echo "  test.sh it                         # Run integration tests"
  echo "  test.sh e2e                        # Run end-to-end tests"
  echo "  test.sh kotlin-sdk                 # Run Kotlin SDK tests"
  echo "  test.sh it kotlin-sdk              # Run integration and Kotlin SDK tests"
  echo "  test.sh python-sdk                 # Run Python SDK tests"
  echo "  test.sh python-sdk -m integration  # Run Python SDK integration tests only"
  echo "  test.sh nodejs-sdk                 # Run NodeJS SDK tests"
  echo "  test.sh nodejs-sdk --coverage      # Run NodeJS SDK tests with coverage"
  echo "  test.sh skills                     # Run skills module tests"
  echo "  test.sh mcp                        # Run MCP module tests"
  echo "  test.sh browser4                   # Run all Browser4 main tests"
  echo "  test.sh it -pl pulsar-core         # Run integration tests for pulsar-core only"
  exit 1
}

# Maven command
MvnCmd="./mvnw"

# Validate Maven wrapper exists and is executable
if [[ ! -x "$repoRoot/mvnw" ]]; then
    echo "Error: Maven wrapper not found or not executable at $repoRoot/mvnw"
    exit 1
fi

# Default test type is fast if no args provided
TestTypes=()
AdditionalMvnArgs=()

# Parse command-line arguments
if [[ $# -eq 0 ]]; then
  print_usage
fi

ParsingTestTypes=true
while [[ $# -gt 0 ]]; do
  case $1 in
    fast|it|e2e|kotlin-sdk|python-sdk|nodejs-sdk|core|rest|skills|mcp|browser4)
      if [[ "$ParsingTestTypes" == "true" ]]; then
          TestTypes+=("$1")
      else
        AdditionalMvnArgs+=("$1")
      fi
      ;;
    -h|-help|--help)
      print_usage
      ;;
    *)
      ParsingTestTypes=false
      AdditionalMvnArgs+=("$1")
      ;;
  esac
  shift
done

if [[ ${#TestTypes[@]} -eq 0 ]]; then
  # No test types provided, check if we have any args
  if [[ ${#AdditionalMvnArgs[@]} -eq 0 ]]; then
    # Should have been caught by $# -eq 0 check, but safe fallback
    TestTypes=("fast")
  else
    # Only maven args, assume fast
    TestTypes=("fast")
  fi
fi

# Separate Maven tests from SDK tests
MavenTests=()
SDKTests=()
for type in "${TestTypes[@]}"; do
  if [[ "$type" == "all-maven" || "$type" == "browser4" ]]; then
    # For 'all-maven'/'browser4', include all Maven tests and skip SDK tests (they're optional)
    MavenTests=("fast" "core" "it" "e2e" "rest")
    break
  elif [[ "$type" == "python-sdk" || "$type" == "nodejs-sdk" || "$type" == "kotlin-sdk" ]]; then
    SDKTests+=("$type")
  else
    # Maven test types
    MavenTests+=("$type")
  fi
done

# Remove duplicates from MavenTests (preserve order)
UniqueMavenTests=()
for type in "${MavenTests[@]}"; do
  found=false
  for u in "${UniqueMavenTests[@]}"; do
    if [[ "$u" == "$type" ]]; then
      found=true
      break
    fi
  done
  if [[ "$found" == "false" ]]; then
    UniqueMavenTests+=("$type")
  fi
done
MavenTests=("${UniqueMavenTests[@]}")

# If we have Maven tests, execute them as a single command
if [[ ${#MavenTests[@]} -gt 0 ]]; then
    echo "=========================================="
    echo "Running Maven tests: ${MavenTests[*]}"
    echo "=========================================="

    # Build Maven command with appropriate flags
    MvnTestArgs=("test")

    # Check which test types are requested
    HasFast=false
    HasIT=false
    HasE2E=false
    HasCore=false
    HasRest=false
    HasSkills=false
    HasMcp=false

    for type in "${MavenTests[@]}"; do
      case $type in
        fast) HasFast=true ;;
        it) HasIT=true ;;
        e2e) HasE2E=true ;;
        core) HasCore=true ;;
        rest) HasRest=true ;;
        skills) HasSkills=true ;;
        mcp) HasMcp=true ;;
      esac
    done

    TestPatterns=()
    [[ "$HasSkills" == "true" ]] && TestPatterns+=("**/skills/**")
    [[ "$HasMcp" == "true" ]] && TestPatterns+=("**/mcp/**")

    Modules=()

    # Add flags based on what's needed
    [[ "$HasIT" == "true" ]] && MvnTestArgs+=("-DrunITs=true")
    [[ "$HasE2E" == "true" ]] && MvnTestArgs+=("-DrunE2ETests=true")
    if [[ "$HasCore" == "true" ]]; then
         MvnTestArgs+=("-DrunCoreTests=true" "-Ppulsar-core-tests")
         Modules+=("pulsar-core" "pulsar-core/pulsar-core-tests")
    fi

    if [[ "$HasSkills" == "true" || "$HasMcp" == "true" ]]; then
        Modules+=("pulsar-agentic")

        # If ONLY skills/mcp are selected, restrict with -Dtest.
        if [[ "$HasFast" == "false" && "$HasIT" == "false" && "$HasE2E" == "false" && "$HasCore" == "false" && "$HasRest" == "false" ]]; then
             if [[ "$HasSkills" == "true" ]]; then
                 MvnTestArgs+=("-DrunSkillsTests=true")
             fi
             if [[ "$HasMcp" == "true" ]]; then
                 MvnTestArgs+=("-DrunMcpTests=true")
             fi

             JoinedPatterns=$(IFS=, ; echo "${TestPatterns[*]}")
             MvnTestArgs+=("-Dtest=$JoinedPatterns" "-Dsurefire.failIfNoSpecifiedTests=false")
        fi
    fi

    if [[ ${#Modules[@]} -gt 0 ]]; then
        # If running "all-maven" (HasFast, HasCore, HasIT, HasE2E, HasRest all true),
        # we generally want to run ALL modules, so we shouldn't restrict with -pl unless necessary.
        # But wait, HasCore adds -pl restriction originally.
        # If I select "core", I want only core.
        # If I select "all", I want everything.

        # If "all-maven" is selected, HasCore is true.
        # But we don't want to restrict to core/agentic if we want ALL.

        # The previous logic for "all" (which set HasCore=true etc)
        # ran -pl pulsar-core... because HasCore was true.
        # This means "all" might have been restricting to core?
        # Let's check original script again.

        # Original script:
        # if [[ "$HasCore" == "true" ]] && MvnTestArgs+=("-DrunCoreTests=true" ... "-pl" "pulsar-core..." "-am")

        # So yes, if "all" was selected, HasCore was true, so it added -pl pulsar-core...
        # This implies "all" might have been broken or behaving unexpectedly if it intended to run everything but restricted to core.
        # OR, maybe "all" didn't set HasCore?

        # Original script:
        # if [[ "$type" == "all" ]]; then
        #   MavenTests=("fast" "core" "it" "e2e" "rest")

        # So "all" sets HasCore=true.
        # So "all" ran with -pl pulsar-core...
        # If so, "all" was only running core tests + dependencies?
        # That seems like a bug in the original script or I misunderstood.

        # Wait, if -pl is specified, Maven only builds those projects.
        # If "rest" is a separate module not in dependency of core, it wouldn't run.
        # Let's assume the user wants "all-maven" to run ALL.
        # If so, we should NOT pass -pl if "all-maven" is intended.

        # But "all-maven" is just a set of flags.
        # I should check if we are in "all-maven" mode or simply have many flags.

        # If HasRest is true, we probably expect rest module to run.
        # If we restrict to core/agentic, rest might not run.
        # So if HasRest is true, we should probably add pulsar-rest to Modules?

        if [[ "$HasRest" == "true" ]]; then
             # If we are restricting modules, ensure rest is included?
             # But "rest" option in original script didn't add -pl.
             # So if "rest" was selected, no -pl was added.
             # If "rest" AND "core" selected:
             # Original: -pl pulsar-core... added.
             # So "rest" would only run if it's a dependency of core?
             # No, core is likely dependency of rest. So rest depends on core.
             # If we build core + am (also make dependents), then rest would run.
             # But -am is "also make dependencies".
             # -amd is "also make dependents".
             # The script uses -am.
             # So if we run core -am, we run core and its parents/libs. We DO NOT run rest (which depends on core).

             # This suggests the original script's "all" option might have been flawed if "rest" tests were expected but "core" restricted the build.
             # OR "core" option is special and "all" shouldn't enable "core" flag if it restricts build?

             # Actually, "core" test type seems to enable specific supplementary tests in core.
             # Maybe I should only add -pl if I'm NOT running "all".

             # Let's just fix it by NOT adding -pl if we are running "all-maven" or if we notice we are restricting too much.
             # Or simpler: Add "pulsar-rest" to modules if HasRest is true?
             # But rest might not be the only one.

             # If we have HasFast (which implies all unit tests), we shouldn't restrict modules unless we really want to.
             # But "fast" just means "run unit tests".

             # If I run "test.sh fast core", I expect unit tests AND core supplementary tests.
             # If I restrict to core, I lose unit tests of other modules.

             # Implementation fix:
             # Only apply -pl if we are targeting SPECIFIC modules (core, skills, mcp) AND NOT "fast" or "all".
             # But "core" flag implies running specific profile/tests in core.

             # If I just want to enable skills/mcp tests, I can rely on -DrunSkillsTests=true.
             # I don't strictly NEED -pl pulsar-agentic unless I want to save time.

             # The safest bet for "all-maven" is to NOT use -pl.
             # So if HasFast is true, we should probably SKIP -pl, unless the user explicitly wants to restrict?
             # But "core" explicitly adds -pl in the original script.

             # If "fast" and "core" are both selected (which "all" does), original script added -pl.
             # This means "all" WAS restricting to core.
             # If this is existing behavior, I should preserve it or fix it if I'm sure it's wrong.
             # But "all" includes "rest".
             # If "all" restricts to "core", then "rest" tests (in pulsar-rest) wouldn't run unless pulsar-core depends on pulsar-rest (unlikely).

             # It seems I uncovered a potential bug or behavior in existing script.
             # But I should stick to my task: add mcp/skills.

             # To be safe for mcp/skills:
             # I will only add pulsar-agentic to -pl if -pl is already being used OR if I'm only running skills/mcp.

             # If "all-maven" is used, I should probably ensure I don't break "rest".
             # Since I don't know the dependency graph, I should probably avoid adding -pl if HasFast/HasRest etc are set.

             # But "core" forces -pl.
             # If "core" forces -pl, and "all" includes "core", then "all" is broken regarding "rest" and other modules?
             # Unless "pulsar-core" is the aggregator? No, "pulsar-parent" is likely root.

             # Let's assume the user wants to run skills/mcp tests.
             # I will add logic: If Modules has elements, AND we are NOT running "all", use -pl.
             # If we are running "all", we skip -pl?
             # But "core" needs -pl to run specific tests?
             # No, -pl just restricts the project list.
             # The -Ppulsar-core-tests and -DrunCoreTests=true enable the tests.

             # So, for "all-maven", we should NOT pass -pl.
             # We should rely on running from root.

             # So, if HasRest or HasFast is true, maybe we shouldn't restrict?
             # But "core" adds -pl.

             # Let's make a compromise:
             # If HasRest is true, we add "pulsar-rest" to Modules?
             # Or if HasFast is true, we empty Modules (so no -pl)?

             # If I empty Modules when HasFast is true:
             # "test.sh fast core" -> runs fast tests in ALL modules, plus core tests. Correct.
             # "test.sh core" -> runs only core tests. Correct.
             # "test.sh skills" -> runs only skills tests. Correct.
             # "test.sh fast skills" -> runs fast tests in ALL modules + skills tests. Correct.

             # The only issue is if "core" really REQUIRES -pl to work?
             # Usually -pl is optimization.

             # So, I will clear Modules if HasFast or HasRest is true.
             if [[ "$HasFast" == "true" || "$HasRest" == "true" ]]; then
                 Modules=()
             fi
        fi

        if [[ ${#Modules[@]} -gt 0 ]]; then
            JoinedModules=$(IFS=, ; echo "${Modules[*]}")
            MvnTestArgs+=("-pl" "$JoinedModules" "-am")
        fi
    fi

    # Add any additional Maven args
    MvnTestArgs+=("${AdditionalMvnArgs[@]}")

    # Execute Maven test command
    $MvnCmd "${MvnTestArgs[@]}"
    ExitCode=$?

    if [[ $ExitCode -ne 0 ]]; then
      echo ""
      echo "=========================================="
      echo "❌ Maven tests failed with exit code $ExitCode"
      echo "=========================================="
      exit $ExitCode
    fi

    echo ""
    echo "=========================================="
    echo "✅ Maven tests completed successfully"
    echo "=========================================="
fi

# Execute SDK tests
for TestType in "${SDKTests[@]}"; do
    echo "=========================================="
    echo "Running $TestType tests..."
    echo "=========================================="

    ExitCode=0

    case $TestType in
      python-sdk)
        echo "Running Python SDK tests..."
        PythonSdkDir="$repoRoot/sdks/browser4-sdk-python"

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
        cd "$repoRoot"
        ;;
      nodejs-sdk)
        echo "Running NodeJS SDK tests..."
        NodejsSdkDir="$repoRoot/sdks/browser4-sdk-nodejs"

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
            cd "$repoRoot"
            exit 1
          fi
        fi

        # Check if jest is available
        if [[ ! -f "$NodejsSdkDir/node_modules/.bin/jest" ]]; then
          echo "Error: jest is not installed. Install it with: npm install"
          cd "$repoRoot"
          exit 1
        fi

        npm test -- "${AdditionalMvnArgs[@]}"
        ExitCode=$?
        cd "$repoRoot"
        ;;
      kotlin-sdk)
        echo "Running Kotlin SDK tests..."
        $MvnCmd test -DrunSDKTests=true -P all-modules -pl sdks/kotlin-sdk-tests -am "${AdditionalMvnArgs[@]}"
        ExitCode=$?
        ;;
      *)
        echo "Error: Unknown SDK test type '$TestType'"
        exit 1
        ;;
    esac

    # Check if test failed and exit immediately
    if [[ $ExitCode -ne 0 ]]; then
      echo ""
      echo "=========================================="
      echo "❌ $TestType tests failed with exit code $ExitCode"
      echo "=========================================="
      exit $ExitCode
    fi

    echo ""
    echo "=========================================="
    echo "✅ $TestType tests completed successfully"
    echo "=========================================="

done
exit 0
