"""
WebDriver example for Browser4 Python SDK.

This example demonstrates WebDriver capabilities:
- Navigation and page control
- Element interaction (click, fill, type, press)
- Scrolling operations
- Content extraction with selectors
- Screenshots
- JavaScript execution

Prerequisites:
- A running Browser4 server at http://localhost:8182
"""

from browser4 import PulsarClient, WebDriver


def main():
    print("=== Browser4 Python SDK - WebDriver Example ===\n")

    # Create client and session
    client = PulsarClient(base_url="http://localhost:8182")

    try:
        session_id = client.create_session()
        print(f"Session created: {session_id}\n")

        driver = WebDriver(client)

        # Navigation
        print("1. Navigation")
        driver.navigate_to("https://example.com")
        print(f"   Current URL: {driver.current_url()}")
        print(f"   Page title: {driver.title()}\n")

        # Check element existence
        print("2. Element checking")
        h1_exists = driver.exists("h1")
        print(f"   H1 element exists: {h1_exists}")

        if h1_exists:
            driver.wait_for_selector("h1", timeout=5000)
            print(f"   H1 element found and visible\n")

        # Content extraction
        print("3. Content extraction")
        title_text = driver.select_first_text_or_null("h1")
        print(f"   Title: {title_text}")

        paragraphs = driver.select_text_all("p")
        print(f"   Found {len(paragraphs)} paragraphs")
        if paragraphs:
            print(f"   First paragraph: {paragraphs[0][:100]}...\n")

        # Extract multiple fields
        print("4. Multi-field extraction")
        fields = driver.extract({
            "heading": "h1",
            "description": "p"
        })
        for name, value in fields.items():
            print(f"   {name}: {value[:80] if value else 'None'}...")
        print()

        # Scrolling
        print("5. Scrolling operations")
        scroll_pos = driver.scroll_down(count=3)
        print(f"   Scrolled down, position: {scroll_pos}")

        scroll_pos = driver.scroll_to_top()
        print(f"   Scrolled to top, position: {scroll_pos}")

        scroll_pos = driver.scroll_to_bottom()
        print(f"   Scrolled to bottom, position: {scroll_pos}\n")

        # JavaScript execution
        print("6. JavaScript execution")
        links_count = driver.execute_script(
            "return document.querySelectorAll('a').length"
        )
        print(f"   Number of links on page: {links_count}")

        scroll_y = driver.evaluate("window.scrollY")
        print(f"   Current scroll position: {scroll_y}\n")

        # Take screenshot (optional)
        print("7. Screenshot")
        try:
            screenshot = driver.capture_screenshot(selector="body")
            if screenshot:
                print(f"   Screenshot captured: {len(screenshot)} bytes (base64)")
            else:
                print("   Screenshot not available")
        except Exception as e:
            print(f"   Screenshot skipped: {e}")
        print()

        # Form interaction example (if forms exist)
        print("8. Element interaction (demo)")
        print("   Demonstrating interaction methods:")
        print("   - driver.click(selector)")
        print("   - driver.fill(selector, text)")
        print("   - driver.press(selector, key)")
        print("   - driver.hover(selector)")
        print("   - driver.check(selector)")
        print("   Note: Actual interaction requires appropriate elements\n")

        # Navigation history
        print("9. Navigation history")
        history = driver.navigate_history
        print(f"   Visited {len(history)} pages:")
        for i, url in enumerate(history, 1):
            print(f"   {i}. {url}")
        print()

        # Clean up
        print("Closing session...")
        client.delete_session()
        print("Session closed successfully!")

    except Exception as e:
        print(f"Error: {e}")
        print("\nNote: This example requires a running Browser4 server.")
    finally:
        client.close()

    print("\n=== Example completed ===")


if __name__ == "__main__":
    main()
