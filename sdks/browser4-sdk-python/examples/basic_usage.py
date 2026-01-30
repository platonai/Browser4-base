"""
Basic usage example for Browser4 Python SDK.

This example demonstrates fundamental Browser4 SDK operations:
- Using Browser4Driver to automatically start the server (recommended)
- Creating a client and session
- Loading pages
- Navigating with WebDriver
- Extracting data with CSS selectors
- AI-powered actions

Note: This example uses Browser4Driver which automatically downloads
      and starts the Browser4 server. No manual setup required!
"""

from browser4 import Browser4Driver, PulsarClient, AgenticSession

def main():
    print("=== Browser4 Python SDK - Basic Usage Example ===\n")

    # Use Browser4Driver to automatically start the server
    print("Starting Browser4 server...")
    driver = Browser4Driver()

    try:
        driver.start()
        print(f"Server started at: {driver.base_url}\n")

        # Create client and session
        print("Creating session...")
        client = PulsarClient(base_url=driver.base_url)
    except:
        print("Failed to start Browser4 server.")
        return

    try:
        session_id = client.create_session()
        print(f"Session created: {session_id}\n")

        session = AgenticSession(client)

        # Load a page
        print("Loading example.com...")
        page = session.open("https://example.com")
        print(f"Page loaded: {page.url}")
        print(f"Content type: {page.content_type}")
        print(f"Content length: {page.content_length} bytes\n")

        # Parse the page
        document = session.parse(page)

        # Extract data using CSS selectors
        if document:
            print("Extracting data with CSS selectors...")
            fields = session.extract(document, {
                "title": "h1",
                "description": "p"
            })

            print("Extracted data:")
            for key, value in fields.items():
                print(f"  {key}: {value[:100] if value else 'None'}...")
            print()

        # Use WebDriver for navigation
        driver = session.driver
        print("Getting current URL from WebDriver...")
        current_url = driver.current_url()
        print(f"Current URL: {current_url}")
        print(f"Page title: {driver.title()}\n")

        # AI-powered action (requires AI capabilities)
        try:
            print("Attempting AI-powered action...")
            result = session.act("scroll to the bottom of the page")
            print(f"Action success: {result.success}")
            print(f"Action message: {result.message}\n")
        except Exception as e:
            print(f"AI action skipped: {e}\n")

        # Clean up
        print("Closing session...")
        session.close()
        print("Session closed successfully!")

    except Exception as e:
        print(f"Error: {e}")
    finally:
        client.close()
        driver.stop()

    print("\n=== Example completed ===")
    print("\nNote: On first run, Browser4.jar is downloaded automatically.")
    print("      This is a one-time operation and may take a few minutes.")


if __name__ == "__main__":
    main()
