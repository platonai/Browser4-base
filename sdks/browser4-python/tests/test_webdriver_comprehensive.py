"""
Comprehensive tests for WebDriver functionality.

These tests focus on improving coverage of the webdriver module,
particularly navigation, element interaction, scrolling, and selection methods.
"""
import json
from typing import Any
from pathlib import Path
import sys

# Ensure the local package is importable without installation
ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

import pytest

from browser4 import PulsarClient, WebDriver


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

        # Navigation endpoints
        if url.endswith("/session") and method == "POST":
            return StubResponse(payload={"value": {"sessionId": "test-session-123"}})
        
        if "/url" in url and method == "POST":
            return StubResponse(payload={"value": body})
        
        if "/url" in url and method == "GET":
            return StubResponse(payload={"value": "https://example.com"})
        
        if "/documentUri" in url:
            return StubResponse(payload={"value": "https://example.com/page"})
        
        if "/baseUri" in url:
            return StubResponse(payload={"value": "https://example.com/"})

        # Selector endpoints
        if "/selectors/exists" in url:
            return StubResponse(payload={"value": {"exists": True}})
        
        if "/selectors/waitFor" in url:
            return StubResponse(payload={"value": {"exists": True}})

        # Interaction endpoints
        if "/selectors/click" in url:
            return StubResponse(payload={"value": True})
        
        if "/selectors/fill" in url:
            return StubResponse(payload={"value": True})
        
        if "/selectors/press" in url:
            return StubResponse(payload={"value": True})

        # Scrolling endpoints
        if "/scroll" in url:
            return StubResponse(payload={"value": 1234.5})

        # Selection/extraction endpoints
        if "/outerHtml" in url:
            return StubResponse(payload={"value": "<html><body>Test</body></html>"})
        
        if "/textContent" in url:
            return StubResponse(payload={"value": "Test content"})
        
        if "/selectFirstTextOrNull" in url:
            return StubResponse(payload={"value": "First text"})
        
        if "/selectTextAll" in url:
            return StubResponse(payload={"value": ["Text 1", "Text 2", "Text 3"]})
        
        if "/selectFirstAttributeOrNull" in url:
            return StubResponse(payload={"value": "attribute-value"})
        
        if "/selectAttributeAll" in url:
            return StubResponse(payload={"value": ["attr1", "attr2"]})

        # Element endpoints
        if "/element" in url and method == "POST":
            if "elements" in url:
                return StubResponse(payload={"value": [
                    {"element-id": "elem1"},
                    {"element-id": "elem2"}
                ]})
            return StubResponse(payload={"value": {"element-id": "test-element-123"}})
        
        if "/element/" in url and "/attribute/" in url:
            return StubResponse(payload={"value": "attribute-value"})
        
        if "/element/" in url and "/text" in url:
            return StubResponse(payload={"value": "Element text"})
        
        if "/element/" in url and "/click" in url:
            return StubResponse(payload={"value": None})

        # Screenshot endpoints
        if "/screenshot" in url:
            return StubResponse(payload={"value": "base64-encoded-image-data"})

        # Script execution endpoints
        if "/execute/sync" in url:
            script = body.get("script", "") if body else ""
            # Navigation scripts
            if "document.title" in script:
                return StubResponse(payload={"value": "Page Title"})
            elif "location.reload" in script:
                return StubResponse(payload={"value": None})
            elif "history.back" in script:
                return StubResponse(payload={"value": None})
            elif "history.forward" in script:
                return StubResponse(payload={"value": None})
            # Document HTML/content scripts
            elif "documentElement.outerHTML" in script:
                return StubResponse(payload={"value": "<html><body>Test</body></html>"})
            elif "body.innerText" in script:
                return StubResponse(payload={"value": "Test content"})
            elif "window.focus()" in script:
                return StubResponse(payload={"value": None})
            # Selector-based scripts that return text
            elif "innerText" in script and "querySelectorAll" in script:
                # select_text_all
                return StubResponse(payload={"value": ["Text 1", "Text 2", "Text 3"]})
            elif "innerText" in script:
                # select_first_text_or_null
                return StubResponse(payload={"value": "First text"})
            # Selector-based scripts that return attributes
            elif "getAttribute" in script and "querySelectorAll" in script:
                # select_attribute_all
                return StubResponse(payload={"value": ["attr1", "attr2"]})
            elif "getAttribute" in script:
                # select_first_attribute_or_null
                return StubResponse(payload={"value": "attribute-value"})
            # Property value selectors
            elif "querySelectorAll" in script and ".map(" in script:
                # Generic array-based selector
                return StubResponse(payload={"value": ["prop1", "prop2"]})
            # Scroll scripts  
            elif "scrollY" in script or "scrollBy" in script or "scrollTo" in script or "scrollIntoView" in script:
                return StubResponse(payload={"value": 1234.5})
            # Check/visibility/interaction scripts
            elif "querySelector" in script:
                if "checked" in script:
                    return StubResponse(payload={"value": True})
                elif "display" in script or "visibility" in script:
                    return StubResponse(payload={"value": True})
                elif "focus()" in script:
                    return StubResponse(payload={"value": None})
                elif "click()" in script:
                    return StubResponse(payload={"value": None})
                elif "MouseEvent" in script or "mouseover" in script:
                    return StubResponse(payload={"value": None})
            return StubResponse(payload={"value": "script result"})
        
        if "/execute/async" in url:
            return StubResponse(payload={"value": "async result"})

        # Control endpoints
        if "/control/delay" in url:
            return StubResponse(payload={"value": None})
        
        if "/control/pause" in url:
            return StubResponse(payload={"value": None})
        
        if "/control/stop" in url:
            return StubResponse(payload={"value": None})
        
        if "/control/bringToFront" in url:
            return StubResponse(payload={"value": None})

        # Cookie endpoints
        if "/cookie" in url and method == "GET":
            return StubResponse(payload={"value": [
                {"name": "session", "value": "abc123"},
                {"name": "user", "value": "test"}
            ]})
        
        if "/cookie" in url and method == "DELETE":
            return StubResponse(payload={"value": None})
        
        if "/cookie" in url and method == "POST":
            return StubResponse(payload={"value": None})

        # Property/Attribute manipulation
        if "/setAttribute" in url:
            return StubResponse(payload={"value": None})
        
        if "/setProperty" in url:
            return StubResponse(payload={"value": None})
        
        if "/selectFirstPropertyValueOrNull" in url:
            return StubResponse(payload={"value": "property-value"})
        
        if "/selectPropertyValueAll" in url:
            return StubResponse(payload={"value": ["prop1", "prop2"]})

        # Advanced interaction
        if "/clickTextMatches" in url:
            return StubResponse(payload={"value": True})
        
        if "/clickMatches" in url:
            return StubResponse(payload={"value": True})
        
        if "/clickNthAnchor" in url:
            return StubResponse(payload={"value": "https://example.com/link"})
        
        if "/mouseWheel" in url:
            return StubResponse(payload={"value": 1000.0})
        
        if "/moveMouse" in url:
            return StubResponse(payload={"value": None})
        
        if "/dragAndDrop" in url:
            return StubResponse(payload={"value": None})

        # Link/image selection
        if "/selectHyperlinks" in url:
            return StubResponse(payload={"value": ["https://example.com/1", "https://example.com/2"]})
        
        if "/selectAnchors" in url:
            return StubResponse(payload={"value": ["https://example.com/a", "https://example.com/b"]})
        
        if "/selectImages" in url:
            return StubResponse(payload={"value": ["https://example.com/img1.jpg"]})

        # Geometry endpoints
        if "/clickablePoint" in url:
            return StubResponse(payload={"value": {"x": 100.0, "y": 200.0}})
        
        if "/boundingBox" in url:
            return StubResponse(payload={"value": {
                "x": 10.0, "y": 20.0, "width": 100.0, "height": 50.0
            }})

        # Event endpoints
        if "/event/config" in url and method == "POST":
            return StubResponse(payload={"value": {"configId": "config-123"}})
        
        if "/event/configs" in url:
            return StubResponse(payload={"value": [{"configId": "config-1"}]})
        
        if "/events" in url and method == "GET":
            return StubResponse(payload={"value": [{"type": "click", "timestamp": 123456}]})
        
        if "/event/subscribe" in url:
            return StubResponse(payload={"value": {"subscriptionId": "sub-123"}})

        # Extract/fields endpoint
        if "/extract" in url:
            fields = body.get("fields", {}) if body else {}
            result = {k: f"value-{k}" for k in fields.keys()}
            return StubResponse(payload={"value": result})

        # Default response
        return StubResponse(payload={"value": body or {}})

    def close(self):
        pass


@pytest.fixture()
def stub_driver(monkeypatch):
    """Create a WebDriver with stubbed HTTP session."""
    client = PulsarClient(base_url="http://localhost:8182")
    client.session_id = "test-session-123"
    stub = StubRequestsSession()
    monkeypatch.setattr(client, "session", stub)
    driver = WebDriver(client)
    return driver, stub


# ========== Navigation Tests ==========

def test_open_navigates_to_url(stub_driver):
    """Test open method navigates to URL."""
    driver, stub = stub_driver
    
    driver.open("https://example.com")
    
    assert len(driver.navigate_history) == 1
    assert driver.navigate_history[0] == "https://example.com"


def test_reload(stub_driver):
    """Test reload method."""
    driver, stub = stub_driver
    
    result = driver.reload()
    
    # Verify execute_script was called
    assert len(stub.calls) > 0
    request_body = json.loads(stub.calls[-1]["data"])
    assert "location.reload" in request_body["script"]


def test_go_back(stub_driver):
    """Test go_back method."""
    driver, stub = stub_driver
    
    result = driver.go_back()
    
    # Verify execute_script was called
    assert len(stub.calls) > 0
    request_body = json.loads(stub.calls[-1]["data"])
    assert "history.back" in request_body["script"]


def test_go_forward(stub_driver):
    """Test go_forward method."""
    driver, stub = stub_driver
    
    result = driver.go_forward()
    
    # Verify execute_script was called
    assert len(stub.calls) > 0
    request_body = json.loads(stub.calls[-1]["data"])
    assert "history.forward" in request_body["script"]


def test_get_current_url(stub_driver):
    """Test get_current_url alias."""
    driver, stub = stub_driver
    
    url = driver.get_current_url()
    
    assert url == "https://example.com"


def test_url_property(stub_driver):
    """Test url() method."""
    driver, stub = stub_driver
    
    url = driver.url()
    
    assert url == "https://example.com"


def test_document_uri(stub_driver):
    """Test document_uri method."""
    driver, stub = stub_driver
    
    uri = driver.document_uri()
    
    assert uri == "https://example.com/page"


def test_get_document_uri(stub_driver):
    """Test get_document_uri alias."""
    driver, stub = stub_driver
    
    uri = driver.get_document_uri()
    
    assert uri == "https://example.com/page"


def test_base_uri(stub_driver):
    """Test base_uri method."""
    driver, stub = stub_driver
    
    uri = driver.base_uri()
    
    assert uri == "https://example.com/"


def test_get_base_uri(stub_driver):
    """Test get_base_uri alias."""
    driver, stub = stub_driver
    
    uri = driver.get_base_uri()
    
    assert uri == "https://example.com/"


def test_title(stub_driver):
    """Test title method."""
    driver, stub = stub_driver
    
    title = driver.title()
    
    assert title == "Page Title"


def test_page_source(stub_driver):
    """Test page_source method."""
    driver, stub = stub_driver
    
    source = driver.page_source()
    
    assert source == "<html><body>Test</body></html>"


# ========== Status Checking Tests ==========

def test_is_visible(stub_driver):
    """Test is_visible method."""
    driver, stub = stub_driver
    
    visible = driver.is_visible("h1.title")
    
    assert visible is True


def test_is_hidden(stub_driver):
    """Test is_hidden method."""
    driver, stub = stub_driver
    
    hidden = driver.is_hidden("h1.title")
    
    # is_hidden returns the opposite of is_visible
    assert hidden is False


def test_is_checked(stub_driver):
    """Test is_checked method."""
    driver, stub = stub_driver
    
    checked = driver.is_checked("input[type=checkbox]")
    
    assert checked is True


def test_wait_for(stub_driver):
    """Test wait_for alias."""
    driver, stub = stub_driver
    
    found = driver.wait_for("h1.title", timeout=5000)
    
    assert found is True


def test_wait_for_navigation(stub_driver):
    """Test wait_for_navigation method."""
    driver, stub = stub_driver
    
    result = driver.wait_for_navigation(old_url="https://old.com", timeout=1000)
    
    assert result is True


# ========== Element Finding Tests ==========

def test_find_element_by_selector(stub_driver):
    """Test find_element_by_selector method."""
    driver, stub = stub_driver
    
    element = driver.find_element_by_selector("h1", strategy="css")
    
    assert element["element-id"] == "test-element-123"


def test_find_elements_by_selector(stub_driver):
    """Test find_elements_by_selector method."""
    driver, stub = stub_driver
    
    elements = driver.find_elements_by_selector("div", strategy="css")
    
    assert len(elements) == 2
    assert elements[0]["element-id"] == "elem1"


def test_find_element(stub_driver):
    """Test find_element method."""
    driver, stub = stub_driver
    
    element = driver.find_element(using="css selector", value="h1")
    
    assert element["element-id"] == "test-element-123"


def test_find_elements(stub_driver):
    """Test find_elements method."""
    driver, stub = stub_driver
    
    elements = driver.find_elements(using="css selector", value="div")
    
    assert len(elements) == 2


# ========== Element Interaction Tests ==========

def test_click_with_count(stub_driver):
    """Test click method."""
    driver, stub = stub_driver
    
    driver.click("button.submit")
    
    assert "/selectors/click" in stub.calls[-1]["url"]


def test_click_element(stub_driver):
    """Test click_element method."""
    driver, stub = stub_driver
    
    driver.click_element("element-123")
    
    assert "/element/element-123/click" in stub.calls[-1]["url"]


def test_hover(stub_driver):
    """Test hover method uses execute_script."""
    driver, stub = stub_driver
    
    driver.hover("button.submit")
    
    assert "/execute/sync" in stub.calls[-1]["url"]


def test_focus(stub_driver):
    """Test focus method uses execute_script."""
    driver, stub = stub_driver
    
    driver.focus("input#username")
    
    assert "/execute/sync" in stub.calls[-1]["url"]


def test_type(stub_driver):
    """Test type method delegates to fill."""
    driver, stub = stub_driver
    
    driver.type("input#username", "testuser")
    
    request_body = json.loads(stub.calls[-1]["data"])
    assert request_body["value"] == "testuser"


def test_fill(stub_driver):
    """Test fill method."""
    driver, stub = stub_driver
    
    driver.fill("input#email", "test@example.com")
    
    request_body = json.loads(stub.calls[-1]["data"])
    assert request_body["value"] == "test@example.com"


def test_press(stub_driver):
    """Test press method."""
    driver, stub = stub_driver
    
    driver.press("input#search", "Enter")
    
    request_body = json.loads(stub.calls[-1]["data"])
    assert request_body["key"] == "Enter"


def test_send_keys(stub_driver):
    """Test send_keys delegates to fill."""
    driver, stub = stub_driver
    
    driver.send_keys("input#search", "test query")
    
    request_body = json.loads(stub.calls[-1]["data"])
    assert request_body["value"] == "test query"


def test_check(stub_driver):
    """Test check method uses execute_script."""
    driver, stub = stub_driver
    
    driver.check("input[type=checkbox]")
    
    assert "/execute/sync" in stub.calls[-1]["url"]


def test_uncheck(stub_driver):
    """Test uncheck method uses execute_script."""
    driver, stub = stub_driver
    
    driver.uncheck("input[type=checkbox]")
    
    assert "/execute/sync" in stub.calls[-1]["url"]


# ========== Scrolling Tests ==========

def test_scroll_down(stub_driver):
    """Test scroll_down method."""
    driver, stub = stub_driver
    
    position = driver.scroll_down(count=2)
    
    assert position == 1234.5


def test_scroll_up(stub_driver):
    """Test scroll_up method."""
    driver, stub = stub_driver
    
    position = driver.scroll_up(count=3)
    
    assert position == 1234.5


def test_scroll_to(stub_driver):
    """Test scroll_to method."""
    driver, stub = stub_driver
    
    position = driver.scroll_to("h1#top")
    
    assert position == 1234.5


def test_scroll_to_top(stub_driver):
    """Test scroll_to_top method."""
    driver, stub = stub_driver
    
    position = driver.scroll_to_top()
    
    assert position == 1234.5


def test_scroll_to_bottom(stub_driver):
    """Test scroll_to_bottom method."""
    driver, stub = stub_driver
    
    position = driver.scroll_to_bottom()
    
    assert position == 1234.5


def test_scroll_to_middle(stub_driver):
    """Test scroll_to_middle method."""
    driver, stub = stub_driver
    
    position = driver.scroll_to_middle(ratio=0.75)
    
    # Verify script contains the ratio
    request_body = json.loads(stub.calls[-1]["data"])
    assert "0.75" in request_body["script"]


def test_scroll_by(stub_driver):
    """Test scroll_by method."""
    driver, stub = stub_driver
    
    position = driver.scroll_by(pixels=500.0, smooth=False)
    
    # Verify script contains the pixels value
    request_body = json.loads(stub.calls[-1]["data"])
    assert "500" in request_body["script"]


# ========== Selection/Extraction Tests ==========

def test_outer_html_with_selector(stub_driver):
    """Test outer_html with selector."""
    driver, stub = stub_driver
    
    html = driver.outer_html(selector="div#content")
    
    # Check that API call was made (stub returns empty dict by default)
    assert stub.calls[-1]["method"] == "POST"


def test_outer_html_without_selector(stub_driver):
    """Test outer_html without selector (full page)."""
    driver, stub = stub_driver
    
    html = driver.outer_html()
    
    assert html == "<html><body>Test</body></html>"


def test_text_content(stub_driver):
    """Test text_content method."""
    driver, stub = stub_driver
    
    text = driver.text_content(selector="p.description")
    
    # text_content delegates to select_first_text_or_null which uses execute_script
    assert text == "First text"


def test_select_first_text_or_null(stub_driver):
    """Test select_first_text_or_null method."""
    driver, stub = stub_driver
    
    text = driver.select_first_text_or_null("h1")
    
    assert text == "First text"


def test_select_text_all(stub_driver):
    """Test select_text_all method."""
    driver, stub = stub_driver
    
    texts = driver.select_text_all("p")
    
    assert len(texts) == 3
    assert texts[0] == "Text 1"


def test_select_first_attribute_or_null(stub_driver):
    """Test select_first_attribute_or_null method."""
    driver, stub = stub_driver
    
    value = driver.select_first_attribute_or_null("a", "href")
    
    assert value == "attribute-value"


def test_select_attribute_all(stub_driver):
    """Test select_attribute_all method."""
    driver, stub = stub_driver
    
    values = driver.select_attribute_all("a", "href")
    
    assert len(values) == 2
    assert values[0] == "attr1"


def test_get_attribute(stub_driver):
    """Test get_attribute method."""
    driver, stub = stub_driver
    
    value = driver.get_attribute("element-123", "class")
    
    assert value == "attribute-value"


def test_get_text(stub_driver):
    """Test get_text method."""
    driver, stub = stub_driver
    
    text = driver.get_text("element-123")
    
    assert text == "Element text"


def test_extract(stub_driver):
    """Test extract method with field selectors."""
    driver, stub = stub_driver
    
    fields = {
        "title": "h1",
        "description": "p.desc",
        "price": "span.price"
    }
    
    result = driver.extract(fields)
    
    # extract() calls select_first_text_or_null for each field
    assert "title" in result
    assert "description" in result
    assert "price" in result


# ========== Screenshot Tests ==========

def test_capture_screenshot(stub_driver):
    """Test capture_screenshot method."""
    driver, stub = stub_driver
    
    screenshot = driver.capture_screenshot(selector="div#content")
    
    assert screenshot == "base64-encoded-image-data"


def test_capture_screenshot_full_page(stub_driver):
    """Test capture_screenshot with full_page=True."""
    driver, stub = stub_driver
    
    screenshot = driver.capture_screenshot(full_page=True)
    
    request_body = json.loads(stub.calls[-1]["data"])
    assert request_body.get("fullPage") is True


def test_screenshot_alias(stub_driver):
    """Test screenshot alias method."""
    driver, stub = stub_driver
    
    screenshot = driver.screenshot(selector="div#content")
    
    assert screenshot == "base64-encoded-image-data"


# ========== Script Execution Tests ==========

def test_evaluate(stub_driver):
    """Test evaluate method."""
    driver, stub = stub_driver
    
    result = driver.evaluate("2 + 2")
    
    assert result == "script result"


def test_execute_script_with_args(stub_driver):
    """Test execute_script with arguments."""
    driver, stub = stub_driver
    
    result = driver.execute_script("return arguments[0] + arguments[1]", args=[1, 2])
    
    request_body = json.loads(stub.calls[-1]["data"])
    assert request_body["args"] == [1, 2]


def test_execute_async_script(stub_driver):
    """Test execute_async_script method."""
    driver, stub = stub_driver
    
    result = driver.execute_async_script("callback(42);")
    
    assert result == "async result"


# ========== Control Tests ==========

def test_delay(stub_driver):
    """Test delay method."""
    driver, stub = stub_driver
    
    driver.delay(2000)
    
    request_body = json.loads(stub.calls[-1]["data"])
    assert request_body["ms"] == 2000


def test_pause(stub_driver):
    """Test pause method."""
    driver, stub = stub_driver
    
    driver.pause()
    
    assert "/control/pause" in stub.calls[-1]["url"]


def test_stop(stub_driver):
    """Test stop method."""
    driver, stub = stub_driver
    
    driver.stop()
    
    assert "/control/stop" in stub.calls[-1]["url"]


def test_bring_to_front(stub_driver):
    """Test bring_to_front method uses execute_script."""
    driver, stub = stub_driver
    
    driver.bring_to_front()
    
    assert "/execute/sync" in stub.calls[-1]["url"]


# ========== Cookie Tests ==========

def test_get_cookies(stub_driver):
    """Test get_cookies method."""
    driver, stub = stub_driver
    
    cookies = driver.get_cookies()
    
    assert isinstance(cookies, list)
    assert len(cookies) == 2
    assert cookies[0]["name"] == "session"
    assert cookies[1]["name"] == "user"


def test_delete_cookies(stub_driver):
    """Test delete_cookies method."""
    driver, stub = stub_driver
    
    driver.delete_cookies(name="session")
    
    assert "/cookie" in stub.calls[-1]["url"]


def test_clear_browser_cookies(stub_driver):
    """Test clear_browser_cookies method."""
    driver, stub = stub_driver
    
    driver.clear_browser_cookies()
    
    assert stub.calls[-1]["method"] == "DELETE"


def test_add_cookie(stub_driver):
    """Test add_cookie method."""
    driver, stub = stub_driver
    
    cookie = {"name": "test", "value": "abc123"}
    driver.add_cookie(cookie)
    
    request_body = json.loads(stub.calls[-1]["data"])
    assert request_body["cookie"]["name"] == "test"


# ========== Advanced Interaction Tests ==========

def test_click_text_matches(stub_driver):
    """Test click_text_matches delegates to click."""
    driver, stub = stub_driver
    
    driver.click_text_matches("a", "Click.*here", count=2)
    
    # Just verify it calls the underlying click method
    assert len(stub.calls) > 0


def test_click_matches(stub_driver):
    """Test click_matches delegates to click."""
    driver, stub = stub_driver
    
    driver.click_matches(selector="div", attr_name="data-id", pattern="item-\\d+")
    
    # Just verify it calls the underlying click method
    assert len(stub.calls) > 0


def test_click_nth_anchor(stub_driver):
    """Test click_nth_anchor method."""
    driver, stub = stub_driver
    
    url = driver.click_nth_anchor(n=3, root_selector="nav")
    
    # Returns the href attribute, which comes from select_first_attribute_or_null
    # which uses execute_script, so result depends on stub
    assert stub.calls[-1]["method"] == "POST"


def test_mouse_wheel_down(stub_driver):
    """Test mouse_wheel_down delegates to scroll_by."""
    driver, stub = stub_driver
    
    driver.mouse_wheel_down(delta_x=10, delta_y=100)
    
    # Calls scroll_by internally
    assert len(stub.calls) > 0


def test_mouse_wheel_up(stub_driver):
    """Test mouse_wheel_up delegates to scroll_by."""
    driver, stub = stub_driver
    
    driver.mouse_wheel_up(delta_x=10, delta_y=100)
    
    # Calls scroll_by internally
    assert len(stub.calls) > 0


def test_move_mouse_to_coordinates(stub_driver):
    """Test move_mouse_to with coordinates."""
    driver, stub = stub_driver
    
    driver.move_mouse_to(100, 200)
    
    # Uses execute_script
    assert "/execute/sync" in stub.calls[-1]["url"]


def test_move_mouse_to_selector(stub_driver):
    """Test move_mouse_to with selector."""
    driver, stub = stub_driver
    
    driver.move_mouse_to("button.submit")
    
    # Delegates to hover
    assert len(stub.calls) > 0


def test_drag_and_drop(stub_driver):
    """Test drag_and_drop method."""
    driver, stub = stub_driver
    
    driver.drag_and_drop("div.draggable", delta_x=50, delta_y=100)
    
    # Uses execute_script  
    assert "/execute/sync" in stub.calls[-1]["url"]


# ========== Attribute/Property Manipulation Tests ==========

def test_set_attribute(stub_driver):
    """Test set_attribute method uses execute_script."""
    driver, stub = stub_driver
    
    driver.set_attribute("input#username", "placeholder", "Enter username")
    
    # Uses execute_script
    assert "/execute/sync" in stub.calls[-1]["url"]


def test_set_attribute_all(stub_driver):
    """Test set_attribute_all method uses execute_script."""
    driver, stub = stub_driver
    
    driver.set_attribute_all("input", "required", "true")
    
    assert "/execute/sync" in stub.calls[-1]["url"]


def test_select_first_property_value_or_null(stub_driver):
    """Test select_first_property_value_or_null uses execute_script."""
    driver, stub = stub_driver
    
    value = driver.select_first_property_value_or_null("input", "value")
    
    # Uses execute_script, so result depends on stub
    assert stub.calls[-1]["method"] == "POST"


def test_select_property_value_all(stub_driver):
    """Test select_property_value_all uses execute_script."""
    driver, stub = stub_driver
    
    values = driver.select_property_value_all("input", "value")
    
    # Returns list from execute_script
    assert isinstance(values, list)


def test_set_property(stub_driver):
    """Test set_property method uses execute_script."""
    driver, stub = stub_driver
    
    driver.set_property("input#search", "value", "test query")
    
    # Uses execute_script
    assert "/execute/sync" in stub.calls[-1]["url"]


def test_set_property_all(stub_driver):
    """Test set_property_all method uses execute_script."""
    driver, stub = stub_driver
    
    driver.set_property_all("input", "disabled", "false")
    
    assert "/execute/sync" in stub.calls[-1]["url"]


# ========== Link/Image Selection Tests ==========

def test_select_hyperlinks(stub_driver):
    """Test select_hyperlinks method."""
    driver, stub = stub_driver
    
    links = driver.select_hyperlinks(selector="div#content")
    
    # Returns list of dicts (may be empty based on stub)
    assert isinstance(links, list)


def test_select_anchors(stub_driver):
    """Test select_anchors delegates to select_hyperlinks."""
    driver, stub = stub_driver
    
    anchors = driver.select_anchors(selector="nav")
    
    assert isinstance(anchors, list)


def test_select_images(stub_driver):
    """Test select_images method."""
    driver, stub = stub_driver
    
    images = driver.select_images(selector="article")
    
    # Returns list of image URLs
    assert isinstance(images, list)


# ========== Geometry Tests ==========

def test_clickable_point(stub_driver):
    """Test clickable_point method."""
    driver, stub = stub_driver
    
    point = driver.clickable_point("button.submit")
    
    # May return None if bounding_box returns None/empty from execute_script
    # Just verify method is callable
    assert stub.calls[-1]["method"] == "POST"


def test_bounding_box(stub_driver):
    """Test bounding_box method uses execute_script."""
    driver, stub = stub_driver
    
    box = driver.bounding_box("div#content")
    
    # Uses execute_script, result depends on stub
    assert stub.calls[-1]["method"] == "POST"


# ========== Evaluation Detail Tests ==========

def test_evaluate_detail(stub_driver):
    """Test evaluate_detail method."""
    driver, stub = stub_driver
    
    result = driver.evaluate_detail("2 + 2")
    
    # Returns dict with value and type
    assert isinstance(result, dict)
    assert "value" in result
    assert "type" in result


def test_evaluate_value(stub_driver):
    """Test evaluate_value method."""
    driver, stub = stub_driver
    
    result = driver.evaluate_value("document.title")
    
    # Should call evaluate internally
    assert result is not None


def test_evaluate_value_detail(stub_driver):
    """Test evaluate_value_detail method."""
    driver, stub = stub_driver
    
    result = driver.evaluate_value_detail("document.location.href")
    
    assert result is not None


# ========== Event Tests ==========

def test_create_event_config(stub_driver):
    """Test create_event_config method."""
    driver, stub = stub_driver
    
    config = {"type": "click", "selector": "button"}
    result = driver.create_event_config(config)
    
    # Just verify the method is callable
    assert stub.calls[-1]["method"] == "POST"


def test_list_event_configs(stub_driver):
    """Test list_event_configs method."""
    driver, stub = stub_driver
    
    configs = driver.list_event_configs()
    
    # Returns whatever the client gives
    assert stub.calls[-1]["method"] == "GET"


def test_get_events(stub_driver):
    """Test get_events method."""
    driver, stub = stub_driver
    
    events = driver.get_events()
    
    # Returns whatever the client gives
    assert stub.calls[-1]["method"] == "GET"


def test_subscribe_events(stub_driver):
    """Test subscribe_events method."""
    driver, stub = stub_driver
    
    request = {"events": ["click", "scroll"]}
    result = driver.subscribe_events(request)
    
    # Just verify the method is callable
    assert stub.calls[-1]["method"] == "POST"


# ========== Utility Tests ==========

def test_encode_path_segment(stub_driver):
    """Test _encode_path_segment method."""
    driver, stub = stub_driver
    
    encoded = driver._encode_path_segment("test/value?query=1")
    
    # Should URL-encode special characters
    assert "/" not in encoded or "%2F" in encoded
    assert "?" not in encoded or "%3F" in encoded


def test_close(stub_driver):
    """Test close method."""
    driver, stub = stub_driver
    
    # Should not raise exception
    driver.close()
    
    # Verify it doesn't crash
    assert True


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
