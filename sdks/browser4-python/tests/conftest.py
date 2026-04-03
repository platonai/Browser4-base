"""
Pytest configuration and shared fixtures for Browser4 Python SDK tests.

This module provides fixtures for starting the Browser4 REST server and
Mock server, similar to the Kotlin SDK integration test setup.
"""
import os
import subprocess
import sys
import time
import urllib.request
import urllib.error
from pathlib import Path

import pytest


# Constants
MOCK_SERVER_PORT = 18080
BROWSER4_SERVER_PORT = 8182
MOCK_SERVER_BASE = f"http://localhost:{MOCK_SERVER_PORT}"
BROWSER4_SERVER_BASE = f"http://localhost:{BROWSER4_SERVER_PORT}"


def _is_wsl() -> bool:
    """Return True when running under WSL."""
    return bool(os.environ.get("WSL_INTEROP") or os.environ.get("WSL_DISTRO_NAME"))


def _find_browser4_repo_root(start: Path) -> Path:
    """Find the Browser4 mono-repo root by walking up from `start`.

    We key off the presence of Maven wrapper + .mvn directory.

    Env overrides (useful for CI/IDEs):
      - BROWSER4_REPO_ROOT
      - BROWSER4_PROJECT_ROOT (legacy/alt)
    """
    override = os.environ.get("BROWSER4_REPO_ROOT") or os.environ.get("BROWSER4_PROJECT_ROOT")
    if override:
        root = Path(override).expanduser().resolve()
        return root

    start = start.resolve()
    for candidate in [start, *start.parents]:
        if (candidate / ".mvn").exists() and ((candidate / "mvnw").exists() or (candidate / "mvnw.cmd").exists()):
            return candidate

    # Fallback to the historical assumption (3 parents up) if markers weren't found.
    return Path(__file__).resolve().parents[3]


def _maven_wrapper_path(project_root: Path) -> Path:
    """Return the Maven wrapper path appropriate for the running OS."""
    # If we're on Windows-native Python, mvnw.cmd is the wrapper.
    if os.name == "nt" and not _is_wsl():
        return project_root / "mvnw.cmd"

    # POSIX / WSL
    return project_root / "mvnw"


# Project paths (discovered dynamically so IDEs/WSL don't break)
PROJECT_ROOT = _find_browser4_repo_root(Path(__file__))
MAVEN_WRAPPER = _maven_wrapper_path(PROJECT_ROOT)


def is_port_in_use(port: int) -> bool:
    """Check if a port is already in use."""
    try:
        url = f"http://localhost:{port}"
        with urllib.request.urlopen(url, timeout=1):
            return True
    except urllib.error.HTTPError:
        # The port is in use even if the endpoint returns a non-2xx response.
        return True
    except Exception:
        return False


def wait_for_server(base_url: str, timeout_seconds: int = 60) -> bool:
    """Wait for server to become ready."""
    candidates = [
        f"{base_url}/health",
        f"{base_url}/actuator/health",
        f"{base_url}/",
    ]
    end_time = time.time() + timeout_seconds

    while time.time() < end_time:
        for url in candidates:
            try:
                with urllib.request.urlopen(url, timeout=2) as response:
                    if 200 <= response.status < 300:
                        return True
            except urllib.error.HTTPError as e:
                # Any HTTP response implies the server is up.
                if 200 <= e.code < 600:
                    return True
            except Exception:
                pass
        time.sleep(0.5)

    return False


@pytest.fixture(scope="session")
def maven_build():
    """Build the Browser4 project if needed (session-scoped)."""
    if not MAVEN_WRAPPER.exists():
        pytest.skip(
            f"Maven wrapper not found at {MAVEN_WRAPPER}. "
            "Set BROWSER4_REPO_ROOT to the Browser4 repo root containing mvnw/mvnw.cmd."
        )

    # Check if already built by looking for a key class file
    pulsar_rest_classes = PROJECT_ROOT / "pulsar-rest" / "target" / "classes"

    if not pulsar_rest_classes.exists():
        print("Building Browser4 project (this may take a few minutes)...")

        # Build command
        cmd = [str(MAVEN_WRAPPER), "-q", "-DskipTests", "install"]

        try:
            result = subprocess.run(
                cmd,
                cwd=PROJECT_ROOT,
                capture_output=True,
                text=True,
                timeout=600  # 10 minutes max
            )

            if result.returncode != 0:
                print(f"Build failed:\nSTDOUT: {result.stdout}\nSTDERR: {result.stderr}")
                pytest.skip("Failed to build Browser4 project")

            print("Build completed successfully")
        except subprocess.TimeoutExpired:
            pytest.skip("Build timed out")
        except Exception as e:
            pytest.skip(f"Build error: {e}")

    return True


@pytest.fixture(scope="session")
def mock_server(maven_build):
    """
    Start the Mock server on port 18080 (session-scoped).

    This is similar to MockServerConfiguration in Kotlin tests.
    Provides test pages at http://localhost:18080.
    """
    # Check if already running
    if is_port_in_use(MOCK_SERVER_PORT):
        print(f"Mock server already running on port {MOCK_SERVER_PORT}")
        yield MOCK_SERVER_BASE
        return

    print(f"Starting Mock server on port {MOCK_SERVER_PORT}...")

    # Start Mock server using Spring Boot plugin
    # We'll run the MockSiteApplication from pulsar-tests-common
    cmd = [
        str(MAVEN_WRAPPER),
        "-q",
        "-pl", "pulsar-tests/pulsar-tests-common",
        # Use fully-qualified goal so Maven doesn't need prefix/pluginGroup mapping.
        "org.springframework.boot:spring-boot-maven-plugin:run",
        f"-Dspring-boot.run.arguments=--server.port={MOCK_SERVER_PORT}",
        "-Dspring-boot.run.main-class=ai.platon.pulsar.test.server.MockSiteApplicationKt",
    ]

    process = None
    try:
        # Do not pipe stdout/stderr: Maven can output a lot and block on full pipes.
        process = subprocess.Popen(
            cmd,
            cwd=PROJECT_ROOT,
        )

        # Wait for server to start
        if wait_for_server(MOCK_SERVER_BASE, timeout_seconds=60):
            print(f"Mock server started successfully on port {MOCK_SERVER_PORT}")
            yield MOCK_SERVER_BASE
        else:
            pytest.skip("Mock server failed to start within timeout")

    finally:
        # Clean up
        if process:
            print("Stopping Mock server...")
            process.terminate()
            try:
                process.wait(timeout=10)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait()


@pytest.fixture(scope="session")
def browser4_server(maven_build, mock_server):
    """
    Start the Browser4 REST server (session-scoped).

    This is similar to PulsarRestServerApplication in Kotlin tests.
    Starts a complete Browser4 server on port 8182.

    The server depends on mock_server being available.
    """
    # Check if already running
    if is_port_in_use(BROWSER4_SERVER_PORT):
        print(f"Browser4 server already running on port {BROWSER4_SERVER_PORT}")
        yield BROWSER4_SERVER_BASE
        return

    print(f"Starting Browser4 server on port {BROWSER4_SERVER_PORT}...")

    # Start Browser4 REST server using Spring Boot plugin
    cmd = [
        str(MAVEN_WRAPPER),
        "-q",
        "-pl", "pulsar-rest",
        # Use fully-qualified goal so Maven doesn't need prefix/pluginGroup mapping.
        "org.springframework.boot:spring-boot-maven-plugin:run",
        f"-Dspring-boot.run.arguments=--server.port={BROWSER4_SERVER_PORT}",
        "-Dspring-boot.run.main-class=ai.platon.pulsar.rest.ApiApplicationKt",
        "-Dspring-boot.run.profiles=test",
    ]

    process = None
    try:
        # Do not pipe stdout/stderr: Maven can output a lot and block on full pipes.
        process = subprocess.Popen(
            cmd,
            cwd=PROJECT_ROOT,
        )

        # Wait for server to start (longer timeout as it needs to initialize browser)
        if wait_for_server(BROWSER4_SERVER_BASE, timeout_seconds=120):
            print(f"Browser4 server started successfully on port {BROWSER4_SERVER_PORT}")
            # Additional wait to ensure browser subsystem is ready
            time.sleep(5)
            yield BROWSER4_SERVER_BASE
        else:
            pytest.skip("Browser4 server failed to start within timeout")

    finally:
        # Clean up
        if process:
            print("Stopping Browser4 server...")
            process.terminate()
            try:
                process.wait(timeout=15)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait()


@pytest.fixture(scope="function")
def integration_client(browser4_server):
    """
    Create a PulsarClient configured for integration tests.

    This fixture depends on the browser4_server fixture to ensure
    the server is running. It's function-scoped to provide a clean
    client for each test.
    """
    # Add the SDK to path
    sdk_path = Path(__file__).resolve().parents[1]
    if str(sdk_path) not in sys.path:
        sys.path.insert(0, str(sdk_path))

    from browser4 import PulsarClient

    client = PulsarClient(base_url=browser4_server)

    # Create a session for the test
    try:
        session_id = client.create_session()
        client.session_id = session_id
    except Exception as e:
        pytest.skip(f"Failed to create session: {e}")

    yield client

    # Cleanup
    try:
        if client.session_id:
            client.delete_session()
    except Exception:
        pass
    finally:
        client.close()
