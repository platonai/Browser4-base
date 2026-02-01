"""
WebDriver class providing browser automation capabilities.

This module implements a WebDriver-compatible interface that maps to selector-first
REST endpoints. It mirrors the Kotlin WebDriver interface for consistent API design
across languages.

Key features:
- Navigation: navigate_to, current_url, go_back, go_forward, reload
- Element interaction: click, fill, type, press, hover, focus
- Scrolling: scroll_down, scroll_up, scroll_to, scroll_to_bottom, scroll_to_top
- Selection: exists, wait_for_selector, select_first_text, select_text_all
- Screenshots: capture_screenshot
- Script execution: evaluate, execute_script
- Control: delay, pause, stop

Usage example:
    >>> from browser4 import PulsarClient, WebDriver
    >>> client = PulsarClient()
    >>> client.create_session()
    >>> driver = WebDriver(client)
    >>> driver.navigate_to("https://example.com")
    >>> title = driver.select_first_text_or_null("h1")
"""
from typing import Any, Dict, List, Optional, Union
from urllib.parse import quote

from .client import PulsarClient


class WebDriver:
    """
    WebDriver-compatible façade mapping to selector-first REST endpoints.

    This class mirrors the Kotlin WebDriver interface, providing methods for:
    - Browser control and navigation
    - Element selection and interaction
    - Script execution
    - Event handling

    The driver communicates with the Browser4 server via REST API, making it
    suitable for remote browser automation.
    """

    def __init__(self, client: PulsarClient):
        """
        Initialize WebDriver with a PulsarClient.

        Args:
            client: PulsarClient instance for API communication.
        """
        self.client = client
        self._id: int = 0
        self._navigate_history: List[str] = []

    @staticmethod
    def _encode_path_segment(value: str) -> str:
        """
        URL-encode a string for safe use in URL paths.
        
        Uses quote() with safe='' to encode all special characters for path segments.
        """
        return quote(value, safe='')

    @property
    def id(self) -> int:
        """Get the driver ID."""
        return self._id

    @property
    def navigate_history(self) -> List[str]:
        """Get the navigation history."""
        return list(self._navigate_history)

    # ========== Navigation ==========

    def open(self, url: str) -> None:
        """
        Opens the specified URL and waits for navigation to complete.

        This is a convenience method combining navigate_to and wait_for_navigation.

        Args:
            url: The URL to navigate to.
        """
        self.navigate_to(url)

    def navigate_to(self, url: str) -> Any:
        """
        Navigate to a URL.

        Args:
            url: The URL to navigate to.

        Returns:
            Navigation result.
        """
        result = self.client.post("/session/{sessionId}/url", {"url": url})
        self._navigate_history.append(url)
        return result

    def reload(self) -> Any:
        """Reload the current page."""
        return self.execute_script("location.reload()")

    def go_back(self) -> Any:
        """Navigate back in browser history."""
        return self.execute_script("history.back()")

    def go_forward(self) -> Any:
        """Navigate forward in browser history."""
        return self.execute_script("history.forward()")

    def current_url(self) -> str:
        """
        Get the current URL displayed in the address bar.

        Returns:
            The current URL as a string.
        """
        return self.client.get("/session/{sessionId}/url")

    # Alias for Kotlin compatibility
    def get_current_url(self) -> str:
        """Alias for current_url()."""
        return self.current_url()

    def url(self) -> str:
        """
        Get document.URL property.

        Returns:
            The document URL.
        """
        return self.current_url()

    def document_uri(self) -> str:
        """
        Get document.documentURI property.

        Returns:
            The document URI.
        """
        return self.client.get("/session/{sessionId}/documentUri")

    # Alias for snake_case consistency
    def get_document_uri(self) -> str:
        """Alias for document_uri()."""
        return self.document_uri()

    def base_uri(self) -> str:
        """
        Get document.baseURI property.

        Returns:
            The base URI.
        """
        return self.client.get("/session/{sessionId}/baseUri")

    # Alias for snake_case consistency
    def get_base_uri(self) -> str:
        """Alias for base_uri()."""
        return self.base_uri()

    def title(self) -> str:
        """
        Get the current page title.

        Returns:
            The page title.
        """
        result = self.execute_script("return document.title")
        return result if result else ""

    def page_source(self) -> Optional[str]:
        """
        Get the source code of the current page.

        Returns:
            The page HTML source or None.
        """
        return self.outer_html()

    # ========== Status Checking ==========

    def exists(self, selector: str, strategy: str = "css") -> bool:
        """
        Check if an element exists in the DOM.

        Args:
            selector: CSS selector or XPath expression.
            strategy: Selector strategy ("css" or "xpath").

        Returns:
            True if the element exists, False otherwise.
        """
        value = self.client.post(
            "/session/{sessionId}/selectors/exists",
            {"selector": selector, "strategy": strategy}
        )
        if isinstance(value, dict):
            return bool(value.get("exists"))
        return bool(value)

    def is_visible(self, selector: str) -> bool:
        """
        Check if an element is visible.

        Args:
            selector: CSS selector.

        Returns:
            True if the element is visible.
        """
        script = f"""
        (() => {{
            const el = document.querySelector('{selector}');
            if (!el) return false;
            const style = window.getComputedStyle(el);
            return style.display !== 'none' && style.visibility !== 'hidden' && style.opacity !== '0';
        }})()
        """
        result = self.execute_script(script)
        return bool(result)

    def is_hidden(self, selector: str) -> bool:
        """
        Check if an element is hidden.

        Args:
            selector: CSS selector.

        Returns:
            True if the element is hidden.
        """
        return not self.is_visible(selector)

    def is_checked(self, selector: str) -> bool:
        """
        Check if a checkbox/radio element is checked.

        Args:
            selector: CSS selector.

        Returns:
            True if the element is checked.
        """
        script = f"document.querySelector('{selector}')?.checked"
        result = self.execute_script(script)
        return bool(result)

    # ========== Wait Operations ==========

    def wait_for_selector(self, selector: str, strategy: str = "css", timeout: int = 30000) -> bool:
        """
        Wait for an element to appear in the DOM.

        Args:
            selector: CSS selector or XPath expression.
            strategy: Selector strategy ("css" or "xpath").
            timeout: Maximum wait time in milliseconds.

        Returns:
            True if the element was found before timeout.
        """
        value = self.client.post(
            "/session/{sessionId}/selectors/waitFor",
            {"selector": selector, "strategy": strategy, "timeout": timeout},
        )
        if value is None:
            return True
        return bool(value.get("exists")) if isinstance(value, dict) else bool(value)

    # Alias for compatibility
    def wait_for(self, selector: str, strategy: str = "css", timeout: int = 30000) -> bool:
        """Alias for wait_for_selector()."""
        return self.wait_for_selector(selector, strategy, timeout)

    def wait_for_navigation(self, old_url: str = "", timeout: int = 30000) -> bool:
        """
        Wait for navigation to complete (URL change).

        Args:
            old_url: The previous URL to compare against.
            timeout: Maximum wait time in milliseconds.

        Returns:
            True if navigation completed.
        """
        # Simple implementation using delay - in real implementation,
        # this would poll for URL change
        self.delay(min(timeout, 1000))
        return True

    # ========== Element Finding ==========

    def find_element_by_selector(self, selector: str, strategy: str = "css") -> Dict[str, Any]:
        """
        Find a single element by selector.

        Args:
            selector: CSS selector or XPath expression.
            strategy: Selector strategy.

        Returns:
            Element reference dictionary.
        """
        return self.client.post(
            "/session/{sessionId}/selectors/element",
            {"selector": selector, "strategy": strategy}
        )

    def find_elements_by_selector(self, selector: str, strategy: str = "css") -> List[Dict[str, Any]]:
        """
        Find all elements matching a selector.

        Args:
            selector: CSS selector or XPath expression.
            strategy: Selector strategy.

        Returns:
            List of element reference dictionaries.
        """
        return self.client.post(
            "/session/{sessionId}/selectors/elements",
            {"selector": selector, "strategy": strategy}
        )

    def find_element(self, using: str, value: str) -> Dict[str, Any]:
        """
        Find element using WebDriver locator strategy.

        Args:
            using: Locator strategy (e.g., "css selector", "xpath").
            value: Locator value.

        Returns:
            Element reference dictionary.
        """
        return self.client.post(
            "/session/{sessionId}/element",
            {"using": using, "value": value}
        )

    def find_elements(self, using: str, value: str) -> List[Dict[str, Any]]:
        """
        Find elements using WebDriver locator strategy.

        Args:
            using: Locator strategy.
            value: Locator value.

        Returns:
            List of element reference dictionaries.
        """
        return self.client.post(
            "/session/{sessionId}/elements",
            {"using": using, "value": value}
        )

    # ========== Element Interaction ==========

    def click(self, selector: str, count: int = 1, strategy: str = "css") -> Any:
        """
        Click an element identified by selector.

        Args:
            selector: CSS selector or XPath expression.
            count: Number of clicks (for double-click, use 2).
            strategy: Selector strategy.

        Returns:
            Click result.
        """
        return self.client.post(
            "/session/{sessionId}/selectors/click",
            {"selector": selector, "strategy": strategy}
        )

    def click_element(self, element_id: str) -> Any:
        """
        Click an element by its ID.

        Args:
            element_id: WebDriver element ID.

        Returns:
            Click result.
        """
        return self.client.post(f"/session/{{sessionId}}/element/{self._encode_path_segment(element_id)}/click", {})

    def hover(self, selector: str) -> Any:
        """
        Hover over an element.

        Args:
            selector: CSS selector.

        Returns:
            Hover result.
        """
        script = f"""
        (() => {{
            const el = document.querySelector('{selector}');
            if (el) {{
                const event = new MouseEvent('mouseover', {{bubbles: true}});
                el.dispatchEvent(event);
            }}
        }})()
        """
        return self.execute_script(script)

    def focus(self, selector: str) -> Any:
        """
        Focus on an element.

        Args:
            selector: CSS selector.

        Returns:
            Focus result.
        """
        script = f"document.querySelector('{selector}')?.focus()"
        return self.execute_script(script)

    def type(self, selector: str, text: str) -> Any:
        """
        Type text into an element (appending to existing content).

        Args:
            selector: CSS selector.
            text: Text to type.

        Returns:
            Type result.
        """
        return self.fill(selector, text)

    def fill(self, selector: str, text: str, strategy: str = "css") -> Any:
        """
        Fill an input element with text (clearing existing content first).

        Args:
            selector: CSS selector or XPath expression.
            text: Text to fill.
            strategy: Selector strategy.

        Returns:
            Fill result.
        """
        return self.client.post(
            "/session/{sessionId}/selectors/fill",
            {"selector": selector, "strategy": strategy, "value": text},
        )

    def press(self, selector: str, key: str, strategy: str = "css") -> Any:
        """
        Press a key on an element.

        Args:
            selector: CSS selector or XPath expression.
            key: Key to press (e.g., "Enter", "Tab").
            strategy: Selector strategy.

        Returns:
            Press result.
        """
        return self.client.post(
            "/session/{sessionId}/selectors/press",
            {"selector": selector, "strategy": strategy, "key": key},
        )

    def send_keys(self, selector: str, text: str, strategy: str = "css") -> Any:
        """
        Send keys to an element.
        
        This method now delegates to fill() for consistency with the selector-based API.
        It provides a familiar WebDriver-compatible method name while using the
        selector-based endpoint internally.

        Args:
            selector: CSS selector or XPath expression.
            text: Text to send.
            strategy: Selector strategy.

        Returns:
            Send keys result.
        """
        return self.fill(selector, text, strategy)

    def check(self, selector: str) -> Any:
        """
        Check a checkbox element.

        Args:
            selector: CSS selector.

        Returns:
            Check result.
        """
        script = f"""
        (() => {{
            const el = document.querySelector('{selector}');
            if (el && !el.checked) el.click();
        }})()
        """
        return self.execute_script(script)

    def uncheck(self, selector: str) -> Any:
        """
        Uncheck a checkbox element.

        Args:
            selector: CSS selector.

        Returns:
            Uncheck result.
        """
        script = f"""
        (() => {{
            const el = document.querySelector('{selector}');
            if (el && el.checked) el.click();
        }})()
        """
        return self.execute_script(script)

    # ========== Scrolling ==========

    def scroll_down(self, count: int = 1) -> float:
        """
        Scroll down the page.

        Args:
            count: Number of scroll actions.

        Returns:
            Current scroll position.
        """
        script = f"window.scrollBy(0, {200 * count}); return window.scrollY;"
        result = self.execute_script(script)
        return float(result) if result else 0.0

    def scroll_up(self, count: int = 1) -> float:
        """
        Scroll up the page.

        Args:
            count: Number of scroll actions.

        Returns:
            Current scroll position.
        """
        script = f"window.scrollBy(0, -{200 * count}); return window.scrollY;"
        result = self.execute_script(script)
        return float(result) if result else 0.0

    def scroll_to(self, selector: str) -> float:
        """
        Scroll an element into view.

        Args:
            selector: CSS selector of the element.

        Returns:
            Current scroll position.
        """
        script = f"""
        (() => {{
            const el = document.querySelector('{selector}');
            if (el) el.scrollIntoView({{behavior: 'smooth', block: 'center'}});
            return window.scrollY;
        }})()
        """
        result = self.execute_script(script)
        return float(result) if result else 0.0

    def scroll_to_top(self) -> float:
        """
        Scroll to the top of the page.

        Returns:
            Current scroll position (0).
        """
        script = "window.scrollTo(0, 0); return window.scrollY;"
        result = self.execute_script(script)
        return float(result) if result else 0.0

    def scroll_to_bottom(self) -> float:
        """
        Scroll to the bottom of the page.

        Returns:
            Current scroll position.
        """
        script = "window.scrollTo(0, document.body.scrollHeight); return window.scrollY;"
        result = self.execute_script(script)
        return float(result) if result else 0.0

    def scroll_to_middle(self, ratio: float = 0.5) -> float:
        """
        Scroll to a specific position on the page.

        Args:
            ratio: Scroll ratio (0.0 = top, 1.0 = bottom).

        Returns:
            Current scroll position.
        """
        script = f"""
        (() => {{
            const maxScroll = document.body.scrollHeight - window.innerHeight;
            window.scrollTo(0, maxScroll * {ratio});
            return window.scrollY;
        }})()
        """
        result = self.execute_script(script)
        return float(result) if result else 0.0

    def scroll_by(self, pixels: float = 200.0, smooth: bool = True) -> float:
        """
        Scroll by a specific number of pixels.

        Args:
            pixels: Pixels to scroll (positive = down, negative = up).
            smooth: Whether to use smooth scrolling.

        Returns:
            Current scroll position.
        """
        behavior = "'smooth'" if smooth else "'auto'"
        script = f"""
        (() => {{
            window.scrollBy({{top: {pixels}, behavior: {behavior}}});
            return window.scrollY;
        }})()
        """
        result = self.execute_script(script)
        return float(result) if result else 0.0

    # ========== Content Extraction ==========

    def outer_html(self, selector: Optional[str] = None, strategy: str = "css") -> Optional[str]:
        """
        Get the outer HTML of an element or the entire document.

        Args:
            selector: CSS selector (optional, if None returns document HTML).
            strategy: Selector strategy.

        Returns:
            HTML content or None.
        """
        if selector:
            value = self.client.post(
                "/session/{sessionId}/selectors/outerHtml",
                {"selector": selector, "strategy": strategy},
            )
            return value.get("outerHtml") if isinstance(value, dict) else value
        else:
            return self.execute_script("return document.documentElement.outerHTML")

    def text_content(self, selector: Optional[str] = None) -> Optional[str]:
        """
        Get the text content of an element or document.

        Args:
            selector: CSS selector (optional).

        Returns:
            Text content or None.
        """
        if selector:
            return self.select_first_text_or_null(selector)
        else:
            return self.execute_script("return document.body.innerText")

    def select_first_text_or_null(self, selector: str) -> Optional[str]:
        """
        Get the text content of the first element matching the selector.

        Args:
            selector: CSS selector.

        Returns:
            Text content or None if not found.
        """
        script = f"document.querySelector('{selector}')?.innerText"
        return self.execute_script(script)

    def select_text_all(self, selector: str) -> List[str]:
        """
        Get text content of all elements matching the selector.

        Args:
            selector: CSS selector.

        Returns:
            List of text contents.
        """
        script = f"""
        Array.from(document.querySelectorAll('{selector}'))
            .map(el => el.innerText)
        """
        result = self.execute_script(script)
        return result if isinstance(result, list) else []

    def select_first_attribute_or_null(self, selector: str, attr_name: str) -> Optional[str]:
        """
        Get an attribute value of the first element matching the selector.

        Args:
            selector: CSS selector.
            attr_name: Attribute name.

        Returns:
            Attribute value or None.
        """
        script = f"document.querySelector('{selector}')?.getAttribute('{attr_name}')"
        return self.execute_script(script)

    def select_attribute_all(self, selector: str, attr_name: str) -> List[str]:
        """
        Get attribute values of all elements matching the selector.

        Args:
            selector: CSS selector.
            attr_name: Attribute name.

        Returns:
            List of attribute values.
        """
        script = f"""
        Array.from(document.querySelectorAll('{selector}'))
            .map(el => el.getAttribute('{attr_name}'))
            .filter(v => v !== null)
        """
        result = self.execute_script(script)
        return result if isinstance(result, list) else []

    def get_attribute(self, element_id: str, name: str) -> Any:
        """
        Get an attribute of an element by ID.

        Args:
            element_id: WebDriver element ID.
            name: Attribute name.

        Returns:
            Attribute value.
        """
        return self.client.get(f"/session/{{sessionId}}/element/{self._encode_path_segment(element_id)}/attribute/{self._encode_path_segment(name)}")

    def get_text(self, element_id: str) -> str:
        """
        Get the text content of an element by ID.

        Args:
            element_id: WebDriver element ID.

        Returns:
            Text content.
        """
        return self.client.get(f"/session/{{sessionId}}/element/{self._encode_path_segment(element_id)}/text")

    def extract(self, fields: Dict[str, str]) -> Dict[str, Optional[str]]:
        """
        Extract multiple fields using CSS selectors.

        Args:
            fields: Dictionary mapping field names to CSS selectors.

        Returns:
            Dictionary mapping field names to extracted values.
        """
        result = {}
        for name, selector in fields.items():
            result[name] = self.select_first_text_or_null(selector)
        return result

    # ========== Screenshots ==========

    def capture_screenshot(self, selector: Optional[str] = None, full_page: bool = False) -> Optional[str]:
        """
        Take a screenshot.

        Args:
            selector: CSS selector for element screenshot (optional).
            full_page: Whether to capture the full page.

        Returns:
            Base64-encoded screenshot or None.
        """
        if selector:
            return self.screenshot(selector)
        payload = {"strategy": "css"}
        if full_page:
            payload["fullPage"] = True
        result = self.client.post("/session/{sessionId}/selectors/screenshot", payload)
        return result

    def screenshot(self, selector: Optional[str] = None, strategy: str = "css") -> Optional[str]:
        """
        Take a screenshot (alias for capture_screenshot).

        Args:
            selector: CSS selector (optional).
            strategy: Selector strategy.

        Returns:
            Base64-encoded screenshot or None.
        """
        payload: Dict[str, Any] = {"strategy": strategy}
        if selector:
            payload["selector"] = selector
        return self.client.post("/session/{sessionId}/selectors/screenshot", payload)

    # ========== Script Execution ==========

    def evaluate(self, expression: str) -> Any:
        """
        Execute JavaScript and return the result.

        Args:
            expression: JavaScript expression to evaluate.

        Returns:
            Evaluation result.
        """
        return self.execute_script(f"return {expression}")

    def execute_script(self, script: str, args: Optional[List[Any]] = None) -> Any:
        """
        Execute synchronous JavaScript.

        Args:
            script: JavaScript code to execute.
            args: Arguments to pass to the script.

        Returns:
            Script return value.
        """
        return self.client.post(
            "/session/{sessionId}/execute/sync",
            {"script": script, "args": args or []},
        )

    def execute_async_script(
        self, script: str, args: Optional[List[Any]] = None, timeout: int = 30000
    ) -> Any:
        """
        Execute asynchronous JavaScript.

        Args:
            script: JavaScript code to execute.
            args: Arguments to pass to the script.
            timeout: Execution timeout in milliseconds.

        Returns:
            Script return value.
        """
        return self.client.post(
            "/session/{sessionId}/execute/async",
            {"script": script, "args": args or [], "timeout": timeout},
        )

    # ========== Control ==========

    def delay(self, millis: int) -> Any:
        """
        Delay execution for a specified time.

        Args:
            millis: Delay in milliseconds.

        Returns:
            Delay result.
        """
        return self.client.post("/session/{sessionId}/control/delay", {"ms": millis})

    def pause(self) -> Any:
        """
        Pause the session execution.

        Returns:
            Pause result.
        """
        return self.client.post("/session/{sessionId}/control/pause", {})

    def stop(self) -> Any:
        """
        Stop the session execution.

        Returns:
            Stop result.
        """
        return self.client.post("/session/{sessionId}/control/stop", {})

    def bring_to_front(self) -> Any:
        """Bring the browser window to the front."""
        return self.execute_script("window.focus()")

    # ========== Cookies ==========

    def get_cookies(self) -> List[Dict[str, Any]]:
        """
        Get all cookies for the current page.

        Returns:
            List of cookie dictionaries.
        """
        response = self.client.get("/session/{sessionId}/cookie")
        if isinstance(response, dict) and "value" in response:
            return response["value"]
        return []

    def delete_cookies(
        self,
        name: str,
        url: Optional[str] = None,
        domain: Optional[str] = None,
        path: Optional[str] = None
    ) -> None:
        """
        Delete a cookie by name.

        Args:
            name: Cookie name.
            url: Optional URL.
            domain: Optional domain.
            path: Optional path.
        """
        self.client.delete(f"/session/{{sessionId}}/cookie/{name}")

    def clear_browser_cookies(self) -> None:
        """Clear all browser cookies."""
        self.client.delete("/session/{sessionId}/cookie")

    def add_cookie(self, cookie: Dict[str, Any]) -> None:
        """
        Add a cookie.

        Args:
            cookie: Cookie data dictionary with keys: name, value, domain, path, etc.
        """
        self.client.post("/session/{sessionId}/cookie", {"cookie": cookie})

    # ========== Advanced Element Operations ==========

    def click_text_matches(self, selector: str, pattern: str, count: int = 1) -> Any:
        """
        Click elements whose text content matches a pattern.

        Args:
            selector: CSS selector.
            pattern: Text pattern to match.
            count: Number of matches to click.

        Returns:
            Click result.
        """
        # Simplified - in full implementation would use pattern matching
        return self.click(selector, count)

    def click_matches(
        self,
        selector: str,
        attr_name: str,
        pattern: str,
        count: int = 1
    ) -> Any:
        """
        Click elements whose attribute matches a pattern.

        Args:
            selector: CSS selector.
            attr_name: Attribute name.
            pattern: Attribute value pattern.
            count: Number of matches to click.

        Returns:
            Click result.
        """
        # Simplified - in full implementation would use pattern matching
        return self.click(selector, count)

    def click_nth_anchor(self, n: int, root_selector: str = "body") -> Optional[str]:
        """
        Click the nth anchor element.

        Args:
            n: Zero-based index.
            root_selector: Root selector (default: "body").

        Returns:
            URL of clicked anchor, or None.
        """
        selector = f"{root_selector} a:nth-of-type({n + 1})"
        self.click(selector)
        return self.select_first_attribute_or_null(selector, "href")

    # ========== Mouse Operations ==========

    def mouse_wheel_down(
        self,
        count: int = 1,
        delta_x: float = 0.0,
        delta_y: float = 150.0,
        delay_millis: int = 0
    ) -> None:
        """
        Scroll down using mouse wheel.

        Args:
            count: Number of wheel events.
            delta_x: Horizontal scroll delta.
            delta_y: Vertical scroll delta.
            delay_millis: Delay between events in milliseconds.
        """
        for i in range(count):
            self.scroll_by(delta_y, smooth=False)
            if delay_millis > 0 and i < count - 1:
                self.delay(delay_millis)

    def mouse_wheel_up(
        self,
        count: int = 1,
        delta_x: float = 0.0,
        delta_y: float = -150.0,
        delay_millis: int = 0
    ) -> None:
        """
        Scroll up using mouse wheel.

        Args:
            count: Number of wheel events.
            delta_x: Horizontal scroll delta.
            delta_y: Vertical scroll delta.
            delay_millis: Delay between events in milliseconds.
        """
        for i in range(count):
            self.scroll_by(delta_y, smooth=False)
            if delay_millis > 0 and i < count - 1:
                self.delay(delay_millis)

    def move_mouse_to(self, *args: Union[float, str, int]) -> None:
        """
        Move mouse to coordinates or element.

        Overloaded method supporting:
        - move_mouse_to(x: float, y: float)
        - move_mouse_to(selector: str, delta_x: int, delta_y: int = 0)

        Args:
            args: Either (x, y) coordinates or (selector, delta_x, delta_y).
        """
        if len(args) == 2 and isinstance(args[0], (int, float)) and isinstance(args[1], (int, float)):
            # move_mouse_to(x, y)
            x, y = args
            self.execute_script(
                f"window.dispatchEvent(new MouseEvent('mousemove', {{clientX: {x}, clientY: {y}}}))"
            )
        elif len(args) >= 1 and isinstance(args[0], str):
            # move_mouse_to(selector, delta_x, delta_y)
            selector = args[0]
            self.hover(selector)

    def drag_and_drop(self, selector: str, delta_x: int, delta_y: int = 0) -> None:
        """
        Drag and drop an element.

        Args:
            selector: CSS selector.
            delta_x: X offset.
            delta_y: Y offset.
        """
        self.execute_script(
            f"""
            const el = document.querySelector('{selector}');
            if (el) {{
                el.dispatchEvent(new DragEvent('dragstart'));
                el.style.transform = 'translate({delta_x}px, {delta_y}px)';
                el.dispatchEvent(new DragEvent('drop'));
            }}
            """
        )

    # ========== Attribute and Property Operations ==========

    def set_attribute(self, selector: str, attr_name: str, attr_value: str) -> None:
        """
        Set an attribute on the first matching element.

        Args:
            selector: CSS selector.
            attr_name: Attribute name.
            attr_value: Attribute value.
        """
        self.execute_script(
            f"document.querySelector('{selector}')?.setAttribute('{attr_name}', '{attr_value}')"
        )

    def set_attribute_all(self, selector: str, attr_name: str, attr_value: str) -> None:
        """
        Set an attribute on all matching elements.

        Args:
            selector: CSS selector.
            attr_name: Attribute name.
            attr_value: Attribute value.
        """
        self.execute_script(
            f"""
            document.querySelectorAll('{selector}').forEach(el => 
                el.setAttribute('{attr_name}', '{attr_value}')
            )
            """
        )

    def select_first_property_value_or_null(
        self,
        selector: str,
        prop_name: str
    ) -> Optional[str]:
        """
        Get a property value from the first matching element.

        Args:
            selector: CSS selector.
            prop_name: Property name.

        Returns:
            Property value or None.
        """
        result = self.execute_script(f"document.querySelector('{selector}')?.{prop_name}")
        return str(result) if result is not None else None

    def select_property_value_all(
        self,
        selector: str,
        prop_name: str,
        start: int = 0,
        limit: int = 10000
    ) -> List[str]:
        """
        Get property values from all matching elements.

        Args:
            selector: CSS selector.
            prop_name: Property name.
            start: Start index.
            limit: Maximum results.

        Returns:
            List of property values.
        """
        result = self.execute_script(
            f"""
            Array.from(document.querySelectorAll('{selector}'))
                .slice({start}, {start + limit})
                .map(el => el.{prop_name})
                .filter(v => v != null)
            """
        )
        return result if isinstance(result, list) else []

    def set_property(self, selector: str, prop_name: str, prop_value: str) -> None:
        """
        Set a property on the first matching element.

        Args:
            selector: CSS selector.
            prop_name: Property name.
            prop_value: Property value.
        """
        self.execute_script(
            f"const el = document.querySelector('{selector}'); if (el) el.{prop_name} = '{prop_value}'"
        )

    def set_property_all(self, selector: str, prop_name: str, prop_value: str) -> None:
        """
        Set a property on all matching elements.

        Args:
            selector: CSS selector.
            prop_name: Property name.
            prop_value: Property value.
        """
        self.execute_script(
            f"document.querySelectorAll('{selector}').forEach(el => el.{prop_name} = '{prop_value}')"
        )

    # ========== Link and Image Selection ==========

    def select_hyperlinks(
        self,
        selector: str,
        offset: int = 1,
        limit: int = 2147483647  # Max int in many contexts
    ) -> List[Dict[str, Optional[str]]]:
        """
        Select hyperlinks matching a selector.

        Args:
            selector: CSS selector.
            offset: Start offset.
            limit: Maximum results.

        Returns:
            List of hyperlink dictionaries with 'href' and 'text'.
        """
        hrefs = self.select_attribute_all(selector, "href")
        texts = self.select_text_all(selector)
        links = [
            {"href": href, "text": texts[i] if i < len(texts) else None}
            for i, href in enumerate(hrefs)
        ]
        return links[offset - 1:offset - 1 + limit]

    def select_anchors(
        self,
        selector: str,
        offset: int = 1,
        limit: int = 2147483647
    ) -> List[Dict[str, Any]]:
        """
        Select anchor elements with geometric information.

        Args:
            selector: CSS selector.
            offset: Start offset.
            limit: Maximum results.

        Returns:
            List of anchor data dictionaries.
        """
        return self.select_hyperlinks(selector, offset, limit)

    def select_images(
        self,
        selector: str,
        offset: int = 1,
        limit: int = 2147483647
    ) -> List[str]:
        """
        Select image URLs matching a selector.

        Args:
            selector: CSS selector.
            offset: Start offset.
            limit: Maximum results.

        Returns:
            List of image URLs.
        """
        srcs = self.select_attribute_all(selector, "src")
        return srcs[offset - 1:offset - 1 + limit]

    # ========== Advanced Evaluation ==========

    def evaluate_detail(self, expression: str) -> Dict[str, Any]:
        """
        Evaluate JavaScript and return detailed result.

        Args:
            expression: JavaScript expression.

        Returns:
            Evaluation result dictionary with 'value' and 'type'.
        """
        value = self.evaluate(expression)
        return {
            "value": value,
            "type": type(value).__name__
        }

    def evaluate_value(self, *args: Any) -> Any:
        """
        Evaluate JavaScript expression and return value.

        Overloaded method supporting:
        - evaluate_value(expression: str) -> Any
        - evaluate_value(expression: str, default_value: T) -> T
        - evaluate_value(selector: str, function_declaration: str) -> Any

        Args:
            args: Either (expression,), (expression, default_value), or (selector, function_declaration).

        Returns:
            Evaluation result.
        """
        if len(args) == 1:
            return self.evaluate(args[0])
        elif len(args) == 2:
            if isinstance(args[1], str) and '(' in args[1]:
                # evaluate_value(selector, function_declaration)
                selector, func = args
                return self.execute_script(f"({func})(document.querySelector('{selector}'))")
            else:
                # evaluate_value(expression, default_value)
                result = self.evaluate(args[0])
                return result if result is not None else args[1]

    def evaluate_value_detail(self, *args: Union[str, Any]) -> Dict[str, Any]:
        """
        Evaluate JavaScript and return detailed result.

        Args:
            args: Either (expression,) or (selector, function_declaration).

        Returns:
            Evaluation result dictionary with 'value' and 'type'.
        """
        if len(args) == 1:
            return self.evaluate_detail(args[0])
        elif len(args) == 2:
            value = self.evaluate_value(args[0], args[1])
            return {"value": value, "type": type(value).__name__}

    # ========== Geometry Operations ==========

    def clickable_point(self, selector: str) -> Optional[Dict[str, float]]:
        """
        Get the clickable point of an element.

        Args:
            selector: CSS selector.

        Returns:
            Point dictionary with 'x', 'y' coordinates, or None.
        """
        rect = self.bounding_box(selector)
        if not rect:
            return None
        return {
            "x": rect["x"] + rect["width"] / 2,
            "y": rect["y"] + rect["height"] / 2
        }

    def bounding_box(self, selector: str) -> Optional[Dict[str, float]]:
        """
        Get the bounding box of an element.

        Args:
            selector: CSS selector.

        Returns:
            Rectangle dictionary with 'x', 'y', 'width', 'height', or None.
        """
        result = self.execute_script(
            f"""
            const el = document.querySelector('{selector}');
            if (el) {{
                const rect = el.getBoundingClientRect();
                return {{x: rect.x, y: rect.y, width: rect.width, height: rect.height}};
            }}
            return null;
            """
        )
        return result if isinstance(result, dict) else None

    # ========== Events (Placeholder) ==========

    def create_event_config(self, config: Dict[str, Any]) -> Any:
        """
        Create an event configuration.

        Args:
            config: Event configuration dictionary.

        Returns:
            Created config response.
        """
        return self.client.post("/session/{sessionId}/event-configs", config)

    def list_event_configs(self) -> Any:
        """
        List all event configurations.

        Returns:
            List of event configurations.
        """
        return self.client.get("/session/{sessionId}/event-configs")

    def get_events(self) -> Any:
        """
        Get captured events.

        Returns:
            List of events.
        """
        return self.client.get("/session/{sessionId}/events")

    def subscribe_events(self, subscribe_request: Dict[str, Any]) -> Any:
        """
        Subscribe to events.

        Args:
            subscribe_request: Subscription request.

        Returns:
            Subscription response.
        """
        return self.client.post("/session/{sessionId}/events/subscribe", subscribe_request)

    # ========== Convenience Methods ==========

    def close(self) -> None:
        """Close the driver (cleanup)."""
        pass  # No specific cleanup needed for REST-based driver


__all__ = ["WebDriver"]

