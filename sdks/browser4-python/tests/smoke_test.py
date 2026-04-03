#!/usr/bin/env python3
"""
Quick smoke test for Python SDK.

This script runs a subset of unit tests to quickly verify the SDK is working.
Does not require running servers or building the project.

Run: python smoke_test.py
"""
import sys
import subprocess
from pathlib import Path


def main():
    print("Browser4 Python SDK - Quick Smoke Test")
    print("=" * 60)
    
    # Ensure we're in the right directory
    sdk_root = Path(__file__).resolve().parent.parent
    
    # Run a few key unit tests
    test_cases = [
        "tests/test_client.py::test_create_session_sets_id",
        "tests/test_client.py::test_pulsar_session_properties",
        "tests/test_client.py::test_webdriver_navigate",
    ]
    
    print("\nRunning unit tests (no server required)...\n")
    
    try:
        result = subprocess.run(
            ["python", "-m", "pytest"] + test_cases + ["-v", "--tb=short"],
            cwd=sdk_root,
            capture_output=True,
            text=True,
            timeout=30
        )
        
        print(result.stdout)
        if result.stderr:
            print(result.stderr)
        
        if result.returncode == 0:
            print("\n" + "=" * 60)
            print("✓ Smoke tests PASSED")
            print("\nThe SDK basic functionality is working correctly.")
            print("\nTo run full test suite:")
            print("  - Unit tests: pytest -m 'not integration'")
            print("  - Integration tests: pytest -m integration -v -s")
            return 0
        else:
            print("\n" + "=" * 60)
            print("✗ Smoke tests FAILED")
            print("\nSome basic functionality is not working.")
            print("Check the output above for details.")
            return 1
            
    except FileNotFoundError:
        print("\n✗ ERROR: pytest not found")
        print("Install with: pip install pytest")
        return 1
    except subprocess.TimeoutExpired:
        print("\n✗ ERROR: Tests timed out")
        return 1
    except Exception as e:
        print(f"\n✗ ERROR: {e}")
        return 1


if __name__ == "__main__":
    sys.exit(main())
