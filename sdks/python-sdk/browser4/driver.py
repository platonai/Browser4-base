"""
Browser4Driver - Manages the lifecycle of a local Browser4.jar process.

This module provides automatic download, startup, and lifecycle management
of the Browser4 server backend. It mirrors the functionality of the Kotlin
Browser4Driver class.

Licensed to the Apache Software Foundation (ASF) under one or more contributor
license agreements. See the NOTICE file distributed with this work for additional
information regarding copyright ownership. The ASF licenses this file to
you under the Apache License, Version 2.0 (the "License"); you may not use
this file except in compliance with the License. You may obtain a copy of
the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required
by applicable law or agreed to in writing, software distributed under the
License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
OF ANY KIND, either express or implied. See the License for the specific
language governing permissions and limitations under the License.
"""

import atexit
import json
import os
import re
import subprocess
import time
import urllib.request
from pathlib import Path
from typing import Dict, Optional, List
from urllib.error import URLError
from urllib.request import Request, ProxyHandler, build_opener


class Browser4Driver:
    """
    Browser4Driver manages the lifecycle of a local Browser4.jar process.

    This class handles:
    - Downloading Browser4.jar from GitHub releases if not present
    - Starting and stopping the Browser4 server process
    - Health checking the server

    Example usage:
        >>> driver = Browser4Driver()
        >>> driver.start()
        >>> # Use driver.base_url with PulsarClient
        >>> from browser4 import PulsarClient
        >>> client = PulsarClient(base_url=driver.base_url)
        >>> # ... use client ...
        >>> driver.stop()

    Or use as context manager:
        >>> with Browser4Driver() as driver:
        ...     client = PulsarClient(base_url=driver.base_url)
        ...     # ... use client ...

    Args:
        jar_path: Path where Browser4.jar should be stored 
                  (default: ~/.browser4/lib/Browser4.jar)
        download_url: URL to download Browser4.jar from 
                      (default: GitHub release v4.4.0)
        port: Port for the Browser4 server (default: 8182)
        java_options: Additional Java options for the process
    """

    DEFAULT_DOWNLOAD_URL = "https://github.com/platonai/Browser4/releases/download/v4.4.0/Browser4.jar"
    LATEST_RELEASE_API = "https://api.github.com/repos/platonai/Browser4/releases/latest"
    JAR_FILENAME = "Browser4.jar"
    DEFAULT_PORT = 8182
    STARTUP_TIMEOUT_SECONDS = 120
    HEALTH_CHECK_INTERVAL_MS = 500
    MEGABYTE = 1024 * 1024

    def __init__(
        self,
        jar_path: Optional[str] = None,
        download_url: Optional[str] = None,
        port: int = DEFAULT_PORT,
        java_options: Optional[Dict[str, str]] = None,
    ):
        """Initialize the Browser4Driver with configuration options."""
        self.jar_path = jar_path or self._default_jar_path()
        self.download_url = download_url or self.DEFAULT_DOWNLOAD_URL
        self.port = port
        self.java_options = java_options or {}
        self._process: Optional[subprocess.Popen] = None
        self._shutdown_hook_registered = False

    @staticmethod
    def _default_jar_path() -> str:
        """Returns the default jar path in the user's home directory."""
        home = Path.home()
        browser4_dir = home / ".browser4" / "lib"
        browser4_dir.mkdir(parents=True, exist_ok=True)
        return str(browser4_dir / Browser4Driver.JAR_FILENAME)

    @property
    def base_url(self) -> str:
        """The base URL where the Browser4 server is accessible."""
        return f"http://localhost:{self.port}"

    @property
    def is_jar_present(self) -> bool:
        """Checks if the Browser4.jar file exists."""
        return Path(self.jar_path).exists()

    @property
    def is_running(self) -> bool:
        """Checks if the Browser4 server process is running."""
        return self._process is not None and self._process.poll() is None

    def download_if_needed(self) -> None:
        """
        Downloads Browser4.jar from the configured URL if it doesn't exist.

        Raises:
            RuntimeError: If download fails after retries.
        """
        if self.is_jar_present:
            return

        print(f"Browser4.jar not found at {self.jar_path}")
        resolved_url = self._resolve_download_url()
        print(f"Downloading from {resolved_url}...")

        jar_file = Path(self.jar_path)
        jar_file.parent.mkdir(parents=True, exist_ok=True)

        attempts = 3
        backoff_ms = 3000
        last_error: Optional[Exception] = None

        for attempt in range(attempts):
            try:
                self._download_file(resolved_url, jar_file)
                print(f"Browser4.jar downloaded successfully to {self.jar_path}")
                return
            except Exception as e:
                last_error = e
                is_last_attempt = attempt == attempts - 1
                msg = f"Failed to download Browser4.jar (attempt {attempt + 1}/{attempts}): {e}"
                
                if not is_last_attempt:
                    print(f"{msg}; retrying in {backoff_ms / 1000}s...")
                    time.sleep(backoff_ms / 1000)
                else:
                    raise RuntimeError(
                        f"{msg}. Please check network/proxy or override downloadUrl."
                    ) from e

    def _resolve_download_url(self) -> str:
        """Resolves the download URL to the latest release asset when using default URL."""
        if self.download_url != self.DEFAULT_DOWNLOAD_URL:
            return self.download_url

        latest_url = self._fetch_latest_release_download_url()
        if latest_url:
            print(f"Resolved latest Browser4.jar download URL: {latest_url}")
            return latest_url

        print(f"Falling back to default Browser4.jar URL: {self.download_url}")
        return self.download_url

    def _fetch_latest_release_download_url(self) -> Optional[str]:
        """Fetches the latest Browser4.jar download URL from GitHub API."""
        try:
            request = Request(self.LATEST_RELEASE_API)
            request.add_header("Accept", "application/vnd.github+json")
            request.add_header("User-Agent", "Browser4Driver")

            opener = self._create_opener()
            with opener.open(request, timeout=10) as response:
                data = response.read().decode("utf-8")
                return self._parse_latest_browser_download_url(data)
        except Exception as e:
            print(f"Failed to resolve latest Browser4.jar URL: {e}")
            return None

    @staticmethod
    def _parse_latest_browser_download_url(json_text: str) -> Optional[str]:
        """Parses the Browser4.jar download URL from GitHub API JSON response."""
        pattern = rf'"browser_download_url"\s*:\s*"([^"]*{Browser4Driver.JAR_FILENAME})"'
        match = re.search(pattern, json_text)
        return match.group(1) if match else None

    def _create_opener(self):
        """Creates a URL opener that respects system proxy settings."""
        # Use system proxy settings
        proxy_handler = ProxyHandler()
        return build_opener(proxy_handler)

    def _download_file(self, url: str, target_path: Path) -> None:
        """Downloads a file from URL to target path with progress tracking."""
        request = Request(url)
        opener = self._create_opener()
        
        with opener.open(request, timeout=120) as response:
            total_size = int(response.headers.get("Content-Length", 0))
            downloaded = 0
            last_percent = -1
            
            with open(target_path, "wb") as f:
                while True:
                    chunk = response.read(8192)
                    if not chunk:
                        break
                    f.write(chunk)
                    downloaded += len(chunk)
                    
                    if total_size > 0:
                        percent = int((downloaded * 100) / total_size)
                        if percent != last_percent and percent % 5 == 0:
                            print(f"Downloading Browser4.jar... {percent}%")
                            last_percent = percent
                    elif downloaded % Browser4Driver.MEGABYTE == 0:
                        print(f"Downloading Browser4.jar... {downloaded // Browser4Driver.MEGABYTE} MB")
            
            if total_size > 0 and last_percent != 100:
                print("Downloading Browser4.jar... 100%")

    def start(self, wait_for_ready: bool = True) -> None:
        """
        Starts the Browser4 server process.

        This method:
        1. Downloads Browser4.jar if needed
        2. Starts the Java process
        3. Waits for the server to be ready (if wait_for_ready=True)

        Args:
            wait_for_ready: If True, waits for server to be ready before returning.

        Raises:
            RuntimeError: If already running or if startup fails.
        """
        if self.is_running:
            raise RuntimeError("Browser4 server is already running")

        self.download_if_needed()

        print(f"Starting Browser4 server on port {self.port}...")

        commands = ["java"]

        # Ensure REST API profiles are active
        profile_property = "spring.profiles.active"
        has_profile = any(
            k.lower() == profile_property.lower() for k in self.java_options.keys()
        )
        env_profile = os.environ.get("SPRING_PROFILES_ACTIVE")
        
        if not has_profile and not env_profile:
            commands.append(f"-D{profile_property}=rest,private,advanced")

        # Add Java system properties
        for key, value in self.java_options.items():
            commands.append(f"-D{key}={value}")

        # Add environment variable properties
        openrouter_api_key = os.environ.get("OPENROUTER_API_KEY")
        if openrouter_api_key and "OPENROUTER_API_KEY" not in self.java_options:
            commands.append(f"-DOPENROUTER_API_KEY={openrouter_api_key}")

        # Configure server port if not default
        if self.port != self.DEFAULT_PORT:
            commands.append(f"-Dserver.port={self.port}")

        commands.extend(["-jar", self.jar_path])

        self._process = subprocess.Popen(
            commands,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            universal_newlines=True,
        )
        
        self._install_shutdown_hook()

        if wait_for_ready:
            self.wait_for_server_ready()

        print("Browser4 server started successfully")

    def wait_for_server_ready(
        self, timeout_seconds: float = STARTUP_TIMEOUT_SECONDS
    ) -> None:
        """
        Waits for the Browser4 server to be ready by checking the health endpoint.

        Args:
            timeout_seconds: Maximum time to wait in seconds.

        Raises:
            RuntimeError: If server doesn't become ready within timeout.
        """
        start_time = time.time()
        timeout_ms = timeout_seconds * 1000

        while (time.time() - start_time) * 1000 < timeout_ms:
            if not self.is_running:
                raise RuntimeError("Browser4 process terminated unexpectedly")

            if self.is_server_healthy():
                return

            time.sleep(self.HEALTH_CHECK_INTERVAL_MS / 1000)

        raise RuntimeError(
            f"Browser4 server failed to start within {timeout_seconds} seconds"
        )

    def is_server_healthy(self) -> bool:
        """
        Checks if the Browser4 server is healthy by attempting to connect to it.

        Returns:
            True if server responds to health check, False otherwise.
        """
        candidates = [
            f"{self.base_url}/health",
            f"{self.base_url}/actuator/health",
        ]

        for endpoint in candidates:
            try:
                request = Request(endpoint)
                opener = self._create_opener()
                with opener.open(request, timeout=1) as response:
                    return 200 <= response.status < 300
            except Exception:
                continue

        return False

    def stop(self, force: bool = False) -> None:
        """
        Stops the Browser4 server process.

        Args:
            force: If True, forcibly kills the process; otherwise attempts graceful shutdown.
        """
        self._remove_shutdown_hook()
        
        if self._process is not None:
            if self.is_running:
                print("Stopping Browser4 server...")

                if force:
                    self._process.kill()
                else:
                    self._process.terminate()
                    try:
                        self._process.wait(timeout=10)
                    except subprocess.TimeoutExpired:
                        print("Browser4 server did not stop gracefully, forcing...")
                        self._process.kill()

                print("Browser4 server stopped")

            self._process = None

    def _install_shutdown_hook(self) -> None:
        """Installs a shutdown hook to stop the server on program exit."""
        if not self._shutdown_hook_registered:
            atexit.register(self._shutdown_hook)
            self._shutdown_hook_registered = True

    def _remove_shutdown_hook(self) -> None:
        """Removes the shutdown hook."""
        if self._shutdown_hook_registered:
            try:
                atexit.unregister(self._shutdown_hook)
            except ValueError:
                # Hook was already removed or never registered
                pass
            self._shutdown_hook_registered = False

    def _shutdown_hook(self) -> None:
        """Shutdown hook that forcibly stops the server."""
        try:
            self.stop(force=True)
        except Exception:
            pass

    def close(self) -> None:
        """Closes the driver and stops the server process."""
        self.stop()

    def __enter__(self):
        """Context manager entry."""
        self.start()
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        """Context manager exit."""
        self.close()


__all__ = ["Browser4Driver"]
