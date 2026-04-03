"""
Browser4Driver usage example for Browser4 Python SDK.

This example demonstrates how to use Browser4Driver to automatically:
- Download Browser4.jar if not present
- Start the Browser4 server
- Use the SDK with the running server
- Clean up when done

The Browser4Driver eliminates the need to manually download and start
the Browser4 server, making it much easier to get started.
"""

from browser4 import Browser4Driver, PulsarClient, AgenticSession


def main():
    print("=== Browser4Driver Usage Example ===\n")

    # Method 1: Using context manager (recommended)
    print("Method 1: Using context manager")
    print("-" * 40)
    
    try:
        with Browser4Driver() as driver:
            print(f"Server started at: {driver.base_url}")
            
            # Create client and session
            client = PulsarClient(base_url=driver.base_url)
            session_id = client.create_session()
            print(f"Session created: {session_id}\n")
            
            session = AgenticSession(client)
            
            # Load a page
            print("Loading example.com...")
            page = session.open("https://example.com")
            print(f"Page loaded: {page.url}")
            print(f"Status: {page.protocol_status}\n")
            
            # Use WebDriver
            current_url = session.driver.current_url()
            print(f"Current URL: {current_url}")
            print(f"Page title: {session.driver.title()}\n")
            
            # Clean up
            session.close()
            client.close()
            print("Session closed")
            
        print("Server stopped automatically\n")
        
    except Exception as e:
        print(f"Error: {e}\n")


    # Method 2: Manual control
    print("\nMethod 2: Manual control")
    print("-" * 40)
    
    driver = None
    try:
        # Create driver with custom configuration
        driver = Browser4Driver(
            port=8183,  # Custom port
            java_options={
                # Add other Java options as needed
            }
        )
        
        # Start the server
        driver.start()
        print(f"Server started at: {driver.base_url}")
        print(f"Server healthy: {driver.is_server_healthy()}\n")
        
        # Use the server...
        client = PulsarClient(base_url=driver.base_url)
        session_id = client.create_session()
        print(f"Session created: {session_id}\n")
        
        # Clean up
        client.delete_session()
        client.close()
        print("Session deleted")
        
        # Stop the server
        driver.stop()
        print("Server stopped\n")
        
    except Exception as e:
        print(f"Error: {e}\n")
        # Make sure to stop on error
        if driver and driver.is_running:
            driver.stop()


    # Method 3: Start without waiting (advanced)
    print("\nMethod 3: Start without waiting for ready")
    print("-" * 40)
    
    driver = None
    try:
        driver = Browser4Driver()
        
        # Start server without waiting for it to be ready
        driver.start(wait_for_ready=False)
        print("Server starting in background...")
        
        # Wait manually if needed
        print("Waiting for server to be ready...")
        driver.wait_for_server_ready(timeout_seconds=30)
        print(f"Server ready at: {driver.base_url}\n")
        
        # Use the server...
        # ...
        
        # Clean up
        driver.stop()
        print("Server stopped\n")
        
    except Exception as e:
        print(f"Error: {e}\n")
        if driver and driver.is_running:
            driver.stop()


    print("\n=== Example completed ===")
    print("\nNote: On first run, Browser4.jar will be downloaded automatically.")
    print("This may take a few minutes depending on your connection speed.")
    print(f"The jar is cached in: {Browser4Driver._default_jar_path()}")


if __name__ == "__main__":
    main()
