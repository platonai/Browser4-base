#!/usr/bin/env python3
"""
Simple script to verify the test infrastructure can start servers.

This script checks:
1. Maven wrapper exists
2. Can locate project structure
3. Can detect if ports are in use

Run: python verify_test_setup.py
"""
import os
import sys
from pathlib import Path

# Add tests directory to path
tests_dir = Path(__file__).resolve().parent
sys.path.insert(0, str(tests_dir))

from conftest import (
    is_port_in_use,
    MOCK_SERVER_PORT,
    BROWSER4_SERVER_PORT,
    PROJECT_ROOT,
    MAVEN_WRAPPER,
)


def main():
    print("Browser4 Python SDK Test Infrastructure Verification")
    print("=" * 60)
    
    # Check project root
    print(f"\n✓ Project root: {PROJECT_ROOT}")
    if not PROJECT_ROOT.exists():
        print(f"  ✗ ERROR: Project root does not exist!")
        return 1
    
    # Check Maven wrapper
    print(f"\n✓ Maven wrapper: {MAVEN_WRAPPER}")
    if not MAVEN_WRAPPER.exists():
        print(f"  ✗ ERROR: Maven wrapper not found!")
        print(f"  Expected at: {MAVEN_WRAPPER}")
        return 1
    
    # Check if Maven wrapper is executable (on Unix)
    if os.name != "nt" and not os.access(MAVEN_WRAPPER, os.X_OK):
        print(f"  ⚠ Warning: Maven wrapper is not executable")
        print(f"  Run: chmod +x {MAVEN_WRAPPER}")
    
    # Check ports
    print(f"\n✓ Port checks:")
    mock_in_use = is_port_in_use(MOCK_SERVER_PORT)
    browser4_in_use = is_port_in_use(BROWSER4_SERVER_PORT)
    
    print(f"  - Mock server port {MOCK_SERVER_PORT}: {'IN USE' if mock_in_use else 'AVAILABLE'}")
    print(f"  - Browser4 port {BROWSER4_SERVER_PORT}: {'IN USE' if browser4_in_use else 'AVAILABLE'}")
    
    if mock_in_use or browser4_in_use:
        print("\n  ⚠ Warning: Some ports are in use. Tests will try to use existing servers.")
    
    # Check if pulsar-rest module exists
    pulsar_rest = PROJECT_ROOT / "pulsar-rest"
    print(f"\n✓ Pulsar REST module: {pulsar_rest}")
    if not pulsar_rest.exists():
        print(f"  ✗ ERROR: pulsar-rest module not found!")
        return 1
    
    # Check if pulsar-tests-common module exists
    pulsar_tests_common = PROJECT_ROOT / "pulsar-tests" / "pulsar-tests-common"
    print(f"\n✓ Pulsar tests common: {pulsar_tests_common}")
    if not pulsar_tests_common.exists():
        print(f"  ✗ ERROR: pulsar-tests-common module not found!")
        return 1
    
    # Check if Java is available
    import subprocess
    try:
        result = subprocess.run(
            ["java", "-version"],
            capture_output=True,
            text=True,
            timeout=5
        )
        java_version = result.stderr.split('\n')[0] if result.stderr else "unknown"
        print(f"\n✓ Java: {java_version}")
    except FileNotFoundError:
        print("\n  ✗ ERROR: Java not found in PATH!")
        print("  Java 17+ is required to run Browser4 servers")
        return 1
    except Exception as e:
        print(f"\n  ⚠ Warning: Could not check Java version: {e}")
    
    # Summary
    print("\n" + "=" * 60)
    print("✓ Test infrastructure setup looks good!")
    print("\nTo run integration tests:")
    print("  pytest -m integration -v -s")
    print("\nTo run unit tests only:")
    print("  pytest -m 'not integration'")
    
    return 0


if __name__ == "__main__":
    sys.exit(main())
