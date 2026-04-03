"""
Comprehensive tests for AgenticSession edge cases and missing coverage.

These tests cover the missing parts of agentic_session.py to increase coverage.
"""
import json
from typing import Any
from pathlib import Path
import sys

# Ensure the local package is importable without installation
ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

import pytest

from browser4 import (
    PulsarClient,
    PulsarSession,
    AgenticSession,
    WebDriver,
    WebPage,
)


class StubResponse:
    """Mock HTTP response for testing."""

    def __init__(self, status_code: int = 200, payload: Any = None):
        self.status_code = status_code
        self._payload = payload
        self.content = json.dumps(payload).encode("utf-8") if payload is not None else b""

    def raise_for_status(self):
        if 400 <= self.status_code:
            raise Exception(f"HTTP {self.status_code}")

    def json(self):
        return self._payload


class StubRequestsSession:
    """Mock requests session for testing."""

    def __init__(self):
        self.calls = []

    def request(self, method, url, headers=None, data=None, timeout=None):
        self.calls.append({
            "method": method,
            "url": url,
            "headers": headers,
            "data": data,
            "timeout": timeout
        })
        body = json.loads(data) if data else None

        # Session endpoints
        if url.endswith("/session") and method == "POST":
            return StubResponse(payload={"value": {"sessionId": "test-session-123"}})
        
        if "/session/" in url and method == "DELETE":
            return StubResponse(payload={"value": None})

        # Normalize endpoint
        if "/normalize" in url:
            url_value = body.get("url", "") if body else ""
            return StubResponse(payload={"value": {
                "spec": url_value,
                "url": url_value,
                "args": body.get("args") if body else None,
                "isNil": False if url_value else True
            }})

        # Load/open endpoints
        if "/open" in url or "/load" in url:
            return StubResponse(payload={"value": {
                "url": body.get("url", "") if body else "",
                "location": body.get("url", "") if body else "",
                "contentType": "text/html",
                "contentLength": 1024,
                "protocolStatus": "200 OK",
                "isNil": False,
                "html": "<html><body>Test</body></html>"
            }})

        # Submit endpoint
        if "/submit" in url:
            return StubResponse(payload={"value": True})

        # Default response
        return StubResponse(payload={"value": body or {}})

    def close(self):
        pass


@pytest.fixture()
def stub_session(monkeypatch):
    """Create a PulsarSession with stubbed HTTP session."""
    client = PulsarClient(base_url="http://localhost:8182")
    client.session_id = "test-session-123"
    stub = StubRequestsSession()
    monkeypatch.setattr(client, "session", stub)
    session = PulsarSession(client)
    return session, stub


@pytest.fixture()
def stub_agentic_session(monkeypatch):
    """Create an AgenticSession with stubbed HTTP session."""
    client = PulsarClient(base_url="http://localhost:8182")
    client.session_id = "test-session-123"
    stub = StubRequestsSession()
    monkeypatch.setattr(client, "session", stub)
    session = AgenticSession(client)
    return session, stub


# ========== PulsarSession Additional Tests ==========

def test_pulsar_session_id_property(stub_session):
    """Test PulsarSession id property."""
    session, _ = stub_session
    
    assert session.id == 0


def test_pulsar_session_display_no_session(stub_session):
    """Test display property when no session."""
    session, _ = stub_session
    session.client.session_id = None
    
    display = session.display
    
    assert "no-session" in display


def test_pulsar_session_bound_driver(stub_session):
    """Test bound_driver property."""
    session, _ = stub_session
    
    # Initially None
    assert session.bound_driver is None
    
    # After accessing driver, should be set
    driver = session.driver
    assert session.bound_driver is driver


def test_pulsar_session_normalize_or_null_with_none(stub_session):
    """Test normalize_or_null with None URL."""
    session, _ = stub_session
    
    result = session.normalize_or_null(None)
    
    assert result is None


def test_pulsar_session_normalize_or_null_with_valid_url(stub_session):
    """Test normalize_or_null with valid URL."""
    session, _ = stub_session
    
    result = session.normalize_or_null("https://example.com")
    
    assert result is not None
    assert result.url == "https://example.com"


def test_pulsar_session_normalize_or_null_with_nil_result(stub_session):
    """Test normalize_or_null when result is nil."""
    session, stub = stub_session
    
    # Empty URL returns isNil=True from our stub
    result = session.normalize_or_null("")
    
    assert result is None


def test_pulsar_session_load_all(stub_session):
    """Test load_all method."""
    session, _ = stub_session
    
    urls = ["https://example.com/1", "https://example.com/2"]
    pages = session.load_all(urls, args="-expire 1d")
    
    assert len(pages) == 2
    assert all(isinstance(p, WebPage) for p in pages)


def test_pulsar_session_submit_all(stub_session):
    """Test submit_all method."""
    session, _ = stub_session
    
    urls = ["https://example.com/1", "https://example.com/2"]
    result = session.submit_all(urls)
    
    assert result is True


def test_pulsar_session_parse_with_no_html(stub_session):
    """Test parse with page that has no HTML."""
    session, _ = stub_session
    
    page = WebPage(url="https://example.com")  # No html attribute
    result = session.parse(page)
    
    assert result is None


def test_pulsar_session_parse_without_beautifulsoup(stub_session, monkeypatch):
    """Test parse fallback when BeautifulSoup not available."""
    session, _ = stub_session
    
    # Mock BeautifulSoup import to fail
    import builtins
    real_import = builtins.__import__

    def mock_import(name, *args, **kwargs):
        if name == "bs4":
            raise ImportError("BeautifulSoup not installed")
        return real_import(name, *args, **kwargs)

    monkeypatch.setattr("builtins.__import__", mock_import)
    
    page = WebPage(url="https://example.com", html="<html></html>")
    result = session.parse(page)
    
    # Should return raw HTML when BeautifulSoup unavailable
    assert result == "<html></html>"


def test_pulsar_session_extract_with_iterable(stub_session):
    """Test extract with iterable of selectors."""
    session, _ = stub_session
    
    # Create a mock BeautifulSoup document
    from bs4 import BeautifulSoup
    doc = BeautifulSoup("<html><h1>Title</h1><p>Para</p></html>", 'html.parser')
    
    selectors = ["h1", "p"]
    result = session.extract(doc, selectors)
    
    assert "h1" in result
    assert "p" in result


def test_pulsar_session_scrape(stub_session):
    """Test scrape method."""
    session, _ = stub_session
    
    result = session.scrape(
        "https://example.com",
        "-expire 1d",
        {"title": "h1"}
    )
    
    assert isinstance(result, dict)
    assert "title" in result


def test_pulsar_session_exists(stub_session):
    """Test exists method (placeholder)."""
    session, _ = stub_session
    
    # Currently returns False (placeholder implementation)
    result = session.exists("https://example.com")
    
    assert result is False


def test_pulsar_session_flush(stub_session):
    """Test flush method (placeholder)."""
    session, _ = stub_session
    
    # Should not raise exception
    session.flush()


def test_pulsar_session_create_bound_driver(stub_session):
    """Test create_bound_driver method."""
    session, _ = stub_session
    
    driver = session.create_bound_driver()
    
    assert isinstance(driver, WebDriver)
    assert session._driver is driver


def test_pulsar_session_bind_driver(stub_session):
    """Test bind_driver method."""
    session, _ = stub_session
    client = session.client
    
    new_driver = WebDriver(client)
    session.bind_driver(new_driver)
    
    assert session._driver is new_driver


def test_pulsar_session_unbind_driver(stub_session):
    """Test unbind_driver method."""
    session, _ = stub_session
    
    # Bind a driver first
    driver = session.driver
    
    # Unbind it
    session.unbind_driver(driver)
    
    assert session._driver is None


def test_pulsar_session_unbind_wrong_driver(stub_session):
    """Test unbind_driver with different driver."""
    session, _ = stub_session
    client = session.client
    
    # Bind one driver
    driver1 = session.driver
    
    # Try to unbind a different driver
    driver2 = WebDriver(client)
    session.unbind_driver(driver2)
    
    # Should still have driver1
    assert session._driver is driver1


# ========== AgenticSession Additional Tests ==========

def test_agentic_session_context_property(stub_agentic_session):
    """Test context property returns self."""
    session, _ = stub_agentic_session
    
    assert session.context is session


def test_agentic_session_capture(stub_agentic_session):
    """Test capture method."""
    session, _ = stub_agentic_session
    
    page = session.capture()
    
    assert isinstance(page, WebPage)


def test_agentic_session_capture_with_url(stub_agentic_session):
    """Test capture with explicit URL."""
    session, _ = stub_agentic_session
    
    page = session.capture(url="https://example.com/page")
    
    assert isinstance(page, WebPage)


def test_agentic_session_register_closable(stub_agentic_session):
    """Test register_closable placeholder."""
    session, _ = stub_agentic_session
    
    # Mock closable object
    class MockClosable:
        def close(self):
            pass
    
    closable = MockClosable()
    
    # Should not raise exception
    session.register_closable(closable)


def test_agentic_session_data(stub_agentic_session):
    """Test data placeholder."""
    session, _ = stub_agentic_session
    
    # Get returns None
    value = session.data("key")
    assert value is None
    
    # Set also returns None
    result = session.data("key", "value")
    assert result is None


def test_agentic_session_property(stub_agentic_session):
    """Test property placeholder."""
    session, _ = stub_agentic_session
    
    # Get returns None
    value = session.property("key")
    assert value is None
    
    # Set also returns None
    result = session.property("key", "value")
    assert result is None


def test_agentic_session_options(stub_agentic_session):
    """Test options method."""
    session, _ = stub_agentic_session
    
    from browser4 import PageEventHandlers
    handlers = PageEventHandlers()
    
    options = session.options("-expire 1d", event_handlers=handlers)
    
    assert isinstance(options, dict)
    assert options["args"] == "-expire 1d"
    assert options["eventHandlers"] is handlers


def test_agentic_session_close(stub_agentic_session):
    """Test close clears history."""
    session, _ = stub_agentic_session
    
    # Add some state
    session._process_trace.append("trace1")
    session._state_history.append(None)
    
    # Close
    session.close()
    
    # Should be cleared
    assert len(session._process_trace) == 0
    assert len(session._state_history) == 0


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
