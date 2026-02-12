"""
Unit tests for Browser4Driver.

These tests verify the driver's functionality without requiring actual
file downloads or server processes.
"""
import json
import os
import subprocess
import tempfile
import time
from pathlib import Path
from unittest.mock import Mock, MagicMock, patch, call
import sys

# Ensure the local package is importable
ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

import pytest

from browser4.driver import Browser4Driver


class TestBrowser4Driver:
    """Tests for Browser4Driver class."""

    def test_default_jar_path(self):
        """Test that default jar path is in a user-specific directory."""
        path = Browser4Driver._default_jar_path()

        # Windows defaults to %LOCALAPPDATA%\Browser4\lib\Browser4.jar.
        # Other OSes default to ~/.browser4/lib/Browser4.jar.
        assert (".browser4" in path) or ("Browser4" in path)
        assert "lib" in path
        assert "Browser4.jar" in path
        assert Path(path).is_absolute()

    def test_init_with_defaults(self):
        """Test initialization with default parameters."""
        driver = Browser4Driver()

        assert driver.port == Browser4Driver.DEFAULT_PORT
        assert driver.download_url == Browser4Driver.DEFAULT_DOWNLOAD_URL
        assert driver.java_options == {}
        assert not driver.is_running

    def test_init_with_custom_params(self):
        """Test initialization with custom parameters."""
        custom_jar = "/tmp/custom.jar"
        custom_url = "https://example.com/custom.jar"
        custom_port = 9999
        custom_options = {"key": "value"}

        driver = Browser4Driver(
            jar_path=custom_jar,
            download_url=custom_url,
            port=custom_port,
            java_options=custom_options,
        )

        assert driver.jar_path == custom_jar
        assert driver.download_url == custom_url
        assert driver.port == custom_port
        assert driver.java_options == custom_options

    def test_base_url(self):
        """Test base_url property."""
        driver = Browser4Driver(port=8182)
        assert driver.base_url == "http://localhost:8182"

        driver2 = Browser4Driver(port=9999)
        assert driver2.base_url == "http://localhost:9999"

    def test_is_jar_present_when_file_exists(self, tmp_path):
        """Test is_jar_present returns True when jar exists."""
        jar_file = tmp_path / "test.jar"
        jar_file.write_text("fake jar content")

        driver = Browser4Driver(jar_path=str(jar_file))
        assert driver.is_jar_present

    def test_is_jar_present_when_file_missing(self, tmp_path):
        """Test is_jar_present returns False when jar doesn't exist."""
        jar_file = tmp_path / "missing.jar"

        driver = Browser4Driver(jar_path=str(jar_file))
        assert not driver.is_jar_present

    def test_is_running_when_process_alive(self):
        """Test is_running returns True when process is alive."""
        driver = Browser4Driver()
        mock_process = Mock()
        mock_process.poll.return_value = None  # Process is running
        driver._process = mock_process

        assert driver.is_running

    def test_is_running_when_process_dead(self):
        """Test is_running returns False when process is dead."""
        driver = Browser4Driver()
        mock_process = Mock()
        mock_process.poll.return_value = 1  # Process has exited
        driver._process = mock_process

        assert not driver.is_running

    def test_is_running_when_no_process(self):
        """Test is_running returns False when no process."""
        driver = Browser4Driver()
        assert not driver.is_running

    def test_parse_latest_browser_download_url(self):
        """Test parsing download URL from GitHub API response."""
        json_response = """
        {
            "tag_name": "v4.5.0",
            "assets": [
                {
                    "name": "Browser4.jar",
                    "browser_download_url": "https://github.com/platonai/Browser4/releases/download/v4.5.0/Browser4.jar"
                }
            ]
        }
        """

        url = Browser4Driver._parse_latest_browser_download_url(json_response)

        assert url == "https://github.com/platonai/Browser4/releases/download/v4.5.0/Browser4.jar"

    def test_parse_latest_browser_download_url_not_found(self):
        """Test parsing returns None when URL not found."""
        json_response = '{"tag_name": "v1.0.0", "assets": []}'

        url = Browser4Driver._parse_latest_browser_download_url(json_response)

        assert url is None

    def test_download_if_needed_skips_when_jar_exists(self, tmp_path):
        """Test download_if_needed skips download when jar already exists."""
        jar_file = tmp_path / "existing.jar"
        jar_file.write_text("existing content")

        driver = Browser4Driver(jar_path=str(jar_file))

        # Should not raise any exception
        driver.download_if_needed()

        # Content should be unchanged
        assert jar_file.read_text() == "existing content"

    @patch('browser4.driver.Browser4Driver._download_file')
    @patch('browser4.driver.Browser4Driver._resolve_download_url')
    def test_download_if_needed_downloads_when_missing(
        self, mock_resolve, mock_download, tmp_path
    ):
        """Test download_if_needed downloads jar when missing."""
        jar_file = tmp_path / "new.jar"
        mock_resolve.return_value = "https://example.com/Browser4.jar"

        driver = Browser4Driver(jar_path=str(jar_file))
        driver.download_if_needed()

        mock_resolve.assert_called_once()
        mock_download.assert_called_once()

    @patch('browser4.driver.Browser4Driver._download_file')
    @patch('browser4.driver.Browser4Driver._resolve_download_url')
    def test_download_if_needed_retries_on_failure(
        self, mock_resolve, mock_download, tmp_path
    ):
        """Test download retries on failure."""
        jar_file = tmp_path / "retry.jar"
        mock_resolve.return_value = "https://example.com/Browser4.jar"
        mock_download.side_effect = [
            Exception("Network error"),
            Exception("Network error"),
            None  # Success on third try
        ]

        driver = Browser4Driver(jar_path=str(jar_file))
        driver.download_if_needed()

        assert mock_download.call_count == 3

    @patch('browser4.driver.Browser4Driver._download_file')
    @patch('browser4.driver.Browser4Driver._resolve_download_url')
    def test_download_if_needed_raises_after_max_retries(
        self, mock_resolve, mock_download, tmp_path
    ):
        """Test download raises exception after max retries."""
        jar_file = tmp_path / "fail.jar"
        mock_resolve.return_value = "https://example.com/Browser4.jar"
        mock_download.side_effect = Exception("Network error")

        driver = Browser4Driver(jar_path=str(jar_file))

        with pytest.raises(RuntimeError) as exc_info:
            driver.download_if_needed()

        assert "Failed to download" in str(exc_info.value)
        assert mock_download.call_count == 3

    def test_resolve_download_url_returns_custom_url(self):
        """Test _resolve_download_url returns custom URL when set."""
        custom_url = "https://custom.example.com/Browser4.jar"
        driver = Browser4Driver(download_url=custom_url)

        url = driver._resolve_download_url()

        assert url == custom_url

    @patch('browser4.driver.Browser4Driver._fetch_latest_release_download_url')
    def test_resolve_download_url_fetches_latest(self, mock_fetch):
        """Test _resolve_download_url fetches latest URL when using default."""
        mock_fetch.return_value = "https://github.com/platonai/Browser4/releases/download/v4.5.0/Browser4.jar"
        driver = Browser4Driver()

        url = driver._resolve_download_url()

        assert url == "https://github.com/platonai/Browser4/releases/download/v4.5.0/Browser4.jar"
        mock_fetch.assert_called_once()

    @patch('browser4.driver.Browser4Driver._fetch_latest_release_download_url')
    def test_resolve_download_url_fallback_on_fetch_failure(self, mock_fetch):
        """Test _resolve_download_url falls back to default on fetch failure."""
        mock_fetch.return_value = None
        driver = Browser4Driver()

        url = driver._resolve_download_url()

        assert url == Browser4Driver.DEFAULT_DOWNLOAD_URL

    @patch('subprocess.Popen')
    @patch('browser4.driver.Browser4Driver.wait_for_server_ready')
    @patch('browser4.driver.Browser4Driver.download_if_needed')
    def test_start_downloads_and_launches_server(
        self, mock_download, mock_wait, mock_popen, tmp_path
    ):
        """Test start method downloads jar and launches server."""
        jar_file = tmp_path / "test.jar"
        jar_file.write_text("fake jar")

        mock_process = Mock()
        mock_process.poll.return_value = None
        mock_popen.return_value = mock_process

        driver = Browser4Driver(jar_path=str(jar_file))
        driver.start()

        mock_download.assert_called_once()
        mock_popen.assert_called_once()
        mock_wait.assert_called_once()
        assert driver.is_running

    @patch('subprocess.Popen')
    @patch('browser4.driver.Browser4Driver.download_if_needed')
    def test_start_with_wait_for_ready_false(
        self, mock_download, mock_popen, tmp_path
    ):
        """Test start with wait_for_ready=False doesn't wait."""
        jar_file = tmp_path / "test.jar"
        jar_file.write_text("fake jar")

        mock_process = Mock()
        mock_process.poll.return_value = None
        mock_popen.return_value = mock_process

        driver = Browser4Driver(jar_path=str(jar_file))
        driver.start(wait_for_ready=False)

        # Process should be started but not waited for
        mock_popen.assert_called_once()
        assert driver.is_running

    @patch('subprocess.Popen')
    @patch('browser4.driver.Browser4Driver.download_if_needed')
    def test_start_raises_when_already_running(
        self, mock_download, mock_popen, tmp_path
    ):
        """Test start raises exception when already running."""
        jar_file = tmp_path / "test.jar"
        jar_file.write_text("fake jar")

        mock_process = Mock()
        mock_process.poll.return_value = None
        mock_popen.return_value = mock_process

        driver = Browser4Driver(jar_path=str(jar_file))
        driver.start(wait_for_ready=False)

        with pytest.raises(RuntimeError) as exc_info:
            driver.start(wait_for_ready=False)

        assert "already running" in str(exc_info.value)

    @patch('subprocess.Popen')
    @patch('browser4.driver.Browser4Driver.download_if_needed')
    def test_start_includes_default_profiles(
        self, mock_download, mock_popen, tmp_path
    ):
        """Test start includes default Spring profiles."""
        jar_file = tmp_path / "test.jar"
        jar_file.write_text("fake jar")

        mock_process = Mock()
        mock_process.poll.return_value = None
        mock_popen.return_value = mock_process

        driver = Browser4Driver(jar_path=str(jar_file))
        driver.start(wait_for_ready=False)

        # Check that default profiles were included
        args = mock_popen.call_args[0][0]
        assert any("spring.profiles.active=rest,private,advanced" in arg for arg in args)

    @patch('subprocess.Popen')
    @patch('browser4.driver.Browser4Driver.download_if_needed')
    def test_start_respects_custom_port(
        self, mock_download, mock_popen, tmp_path
    ):
        """Test start respects custom port setting."""
        jar_file = tmp_path / "test.jar"
        jar_file.write_text("fake jar")

        mock_process = Mock()
        mock_process.poll.return_value = None
        mock_popen.return_value = mock_process

        driver = Browser4Driver(jar_path=str(jar_file), port=9999)
        driver.start(wait_for_ready=False)

        # Check that custom port was set
        args = mock_popen.call_args[0][0]
        assert any("server.port=9999" in arg for arg in args)

    @patch('subprocess.Popen')
    @patch('browser4.driver.Browser4Driver.download_if_needed')
    def test_start_includes_java_options(
        self, mock_download, mock_popen, tmp_path
    ):
        """Test start includes custom Java options."""
        jar_file = tmp_path / "test.jar"
        jar_file.write_text("fake jar")

        mock_process = Mock()
        mock_process.poll.return_value = None
        mock_popen.return_value = mock_process

        driver = Browser4Driver(
            jar_path=str(jar_file),
            java_options={"custom.key": "custom.value"}
        )
        driver.start(wait_for_ready=False)

        # Check that custom option was included
        args = mock_popen.call_args[0][0]
        assert any("custom.key=custom.value" in arg for arg in args)

    @patch('browser4.driver.Browser4Driver.is_server_healthy')
    @patch('subprocess.Popen')
    @patch('browser4.driver.Browser4Driver.download_if_needed')
    def test_wait_for_server_ready_success(
        self, mock_download, mock_popen, mock_healthy, tmp_path
    ):
        """Test wait_for_server_ready succeeds when server becomes healthy."""
        jar_file = tmp_path / "test.jar"
        jar_file.write_text("fake jar")

        mock_process = Mock()
        mock_process.poll.return_value = None
        mock_popen.return_value = mock_process
        mock_healthy.return_value = True

        driver = Browser4Driver(jar_path=str(jar_file))
        driver._process = mock_process

        # Should not raise exception
        driver.wait_for_server_ready(timeout_seconds=1)

        mock_healthy.assert_called()

    @patch('browser4.driver.Browser4Driver.is_server_healthy')
    @patch('subprocess.Popen')
    @patch('browser4.driver.Browser4Driver.download_if_needed')
    def test_wait_for_server_ready_timeout(
        self, mock_download, mock_popen, mock_healthy, tmp_path
    ):
        """Test wait_for_server_ready raises timeout exception."""
        jar_file = tmp_path / "test.jar"
        jar_file.write_text("fake jar")

        mock_process = Mock()
        mock_process.poll.return_value = None
        mock_popen.return_value = mock_process
        mock_healthy.return_value = False  # Server never becomes healthy

        driver = Browser4Driver(jar_path=str(jar_file))
        driver._process = mock_process

        with pytest.raises(RuntimeError) as exc_info:
            driver.wait_for_server_ready(timeout_seconds=0.5)

        assert "failed to start within" in str(exc_info.value)

    @patch('browser4.driver.Browser4Driver.is_server_healthy')
    def test_wait_for_server_ready_process_died(self, mock_healthy):
        """Test wait_for_server_ready raises when process dies."""
        driver = Browser4Driver()
        mock_process = Mock()
        mock_process.poll.return_value = 1  # Process has exited
        driver._process = mock_process

        with pytest.raises(RuntimeError) as exc_info:
            driver.wait_for_server_ready(timeout_seconds=1)

        assert "terminated unexpectedly" in str(exc_info.value)

    @patch('browser4.driver.Browser4Driver._create_opener')
    def test_is_server_healthy_returns_true_on_success(self, mock_opener):
        """Test is_server_healthy returns True when server responds."""
        mock_response = Mock()
        mock_response.status = 200
        mock_response.__enter__ = Mock(return_value=mock_response)
        mock_response.__exit__ = Mock(return_value=False)

        mock_opener_instance = Mock()
        mock_opener_instance.open.return_value = mock_response
        mock_opener.return_value = mock_opener_instance

        driver = Browser4Driver()

        assert driver.is_server_healthy()

    @patch('browser4.driver.Browser4Driver._create_opener')
    def test_is_server_healthy_returns_false_on_error(self, mock_opener):
        """Test is_server_healthy returns False on connection error."""
        mock_opener_instance = Mock()
        mock_opener_instance.open.side_effect = Exception("Connection failed")
        mock_opener.return_value = mock_opener_instance

        driver = Browser4Driver()

        assert not driver.is_server_healthy()

    def test_stop_terminates_process(self):
        """Test stop terminates the process."""
        driver = Browser4Driver()
        mock_process = Mock()
        mock_process.poll.return_value = None
        driver._process = mock_process

        driver.stop()

        mock_process.terminate.assert_called_once()
        assert driver._process is None

    def test_stop_force_kills_process(self):
        """Test stop with force=True kills the process."""
        driver = Browser4Driver()
        mock_process = Mock()
        mock_process.poll.return_value = None
        driver._process = mock_process

        driver.stop(force=True)

        mock_process.kill.assert_called_once()
        mock_process.terminate.assert_not_called()

    def test_stop_force_kills_if_graceful_fails(self):
        """Test stop force kills if graceful shutdown times out."""
        driver = Browser4Driver()
        mock_process = Mock()
        mock_process.poll.return_value = None
        mock_process.wait.side_effect = subprocess.TimeoutExpired("cmd", 10)
        driver._process = mock_process

        driver.stop()

        mock_process.terminate.assert_called_once()
        mock_process.kill.assert_called_once()

    def test_stop_does_nothing_when_not_running(self):
        """Test stop does nothing when no process."""
        driver = Browser4Driver()

        # Should not raise exception
        driver.stop()

    def test_close_calls_stop(self):
        """Test close method calls stop."""
        driver = Browser4Driver()
        mock_process = Mock()
        mock_process.poll.return_value = None
        driver._process = mock_process

        driver.close()

        assert driver._process is None

    @patch('subprocess.Popen')
    @patch('browser4.driver.Browser4Driver.wait_for_server_ready')
    @patch('browser4.driver.Browser4Driver.download_if_needed')
    def test_context_manager(self, mock_download, mock_wait, mock_popen, tmp_path):
        """Test Browser4Driver as context manager."""
        jar_file = tmp_path / "test.jar"
        jar_file.write_text("fake jar")

        mock_process = Mock()
        mock_process.poll.return_value = None
        mock_popen.return_value = mock_process

        driver = Browser4Driver(jar_path=str(jar_file))

        with driver as d:
            assert d is driver
            assert d.is_running

        # Should be stopped after exiting context
        mock_process.terminate.assert_called()

    @patch('browser4.driver.Browser4Driver._create_opener')
    def test_fetch_latest_release_download_url_success(self, mock_opener):
        """Test fetching latest release URL succeeds."""
        json_response = """
        {
            "browser_download_url": "https://github.com/platonai/Browser4/releases/download/v4.5.0/Browser4.jar"
        }
        """

        mock_response = Mock()
        mock_response.read.return_value = json_response.encode('utf-8')
        mock_response.__enter__ = Mock(return_value=mock_response)
        mock_response.__exit__ = Mock(return_value=False)

        mock_opener_instance = Mock()
        mock_opener_instance.open.return_value = mock_response
        mock_opener.return_value = mock_opener_instance

        driver = Browser4Driver()
        url = driver._fetch_latest_release_download_url()

        assert url == "https://github.com/platonai/Browser4/releases/download/v4.5.0/Browser4.jar"

    @patch('browser4.driver.Browser4Driver._create_opener')
    def test_fetch_latest_release_download_url_failure(self, mock_opener):
        """Test fetching latest release URL returns None on failure."""
        mock_opener_instance = Mock()
        mock_opener_instance.open.side_effect = Exception("Network error")
        mock_opener.return_value = mock_opener_instance

        driver = Browser4Driver()
        url = driver._fetch_latest_release_download_url()

        assert url is None


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
