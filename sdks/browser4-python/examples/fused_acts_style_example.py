"""
Example demonstrating FusedActs-style usage of the Python SDK.

This example shows how to use the SDK API in the same way as the
internal FusedActs example, making it consistent with internal code patterns.

This mirrors the Kotlin FusedActsStyleExample to demonstrate API consistency
across languages.

Prerequisites:
- A running Browser4 server at http://localhost:8182
- OPENROUTER_API_KEY environment variable set (for AI actions)
"""
from browser4 import PulsarClient, AgenticSession


class FusedActsStyleExample:
    """
    FusedActs-style example for Browser4 Python SDK.
    
    Demonstrates the three-layer architecture:
    - PulsarSession: Page loading, parsing, extraction
    - WebDriver: Browser control and automation
    - Agent: AI-powered decision making and task execution
    """

    def __init__(self):
        self.step = 0

    def run(self):
        """Run the FusedActs-style example."""
        # Create client and session
        client = PulsarClient(base_url="http://localhost:8182")
        client.create_session()
        session = AgenticSession(client)
        
        url = "https://news.ycombinator.com/news"
        
        # Get the companion agent and driver (just like FusedActs)
        agent = session.companion_agent
        driver = session.get_or_create_bound_driver()
        
        self.step += 1
        print(f"[STEP {self.step}] Open URL: {url}")
        page = session.open(url)
        print(f"Opened page: {page.url}")
        
        self.step += 1
        print(f"[STEP {self.step}] Parse the page into a BeautifulSoup document")
        document = session.parse(page)
        title_tag = document.select_one("title") if document else None
        title = title_tag.get_text() if title_tag else "N/A"
        print(f"Parsed document title: {title}")
        
        self.step += 1
        print(f"[STEP {self.step}] Extract fields (title) with CSS selector")
        if document:
            fields = session.extract(document, {"title": "title"})
        else:
            fields = {}
        print(f"Extracted fields: {fields}")
        
        # Natural language actions
        self.step += 1
        print(f"[STEP {self.step}] Action: search for 'browser'")
        result = agent.act("search for 'browser'")
        print(f"Action result: {result.message}")
        
        self.step += 1
        print(f"[STEP {self.step}] Capture body text in the live DOM after search")
        content = driver.select_first_text_or_null("body")
        if content:
            print(f"Body snippet: {content[:160]}")
        
        self.step += 1
        print(f"[STEP {self.step}] Action: click the 3rd link")
        result = agent.act("click the 3rd link")
        print(f"Action result: {result.message}")
        
        self.step += 1
        print(f"[STEP {self.step}] Capture body text after clicking")
        content = driver.select_first_text_or_null("body")
        if content:
            print(f"Body snippet: {content[:160]}")
        
        self.step += 1
        print(f"[STEP {self.step}] Action: go back")
        result = agent.act("go back")
        print(f"Action result: {result.message}")
        
        self.step += 1
        print(f"[STEP {self.step}] Action: open the 4th link in new tab")
        result = agent.act("open the 4th link in new tab")
        print(f"Action result: {result.message}")
        
        self.step += 1
        print(f"[STEP {self.step}] Run autonomous task: find search box and submit")
        agent.clear_history()
        run_result = agent.run("find the search box, type 'web scraping' and submit the form")
        print(f"Task result: {run_result.final_result}")
        
        self.step += 1
        print(f"[STEP {self.step}] Capture and parse the live page")
        page = session.capture(driver)
        document = session.parse(page)
        if document:
            fields = session.extract(document, {"title": "title"})
        else:
            fields = {}
        print(f"Extracted after search: {fields}")
        
        self.step += 1
        print(f"[STEP {self.step}] Action: scroll to bottom")
        agent.clear_history()
        run_result = agent.run("scroll to the bottom of the page and wait for new content to load")
        print(f"Task result: {run_result.final_result}")
        
        self.step += 1
        print(f"[STEP {self.step}] Re-open the original URL")
        page = session.open(url)
        document = session.parse(page)
        if document:
            fields = session.extract(document, {"title": "title"})
        else:
            fields = {}
        print(f"Final fields: {fields}")
        
        self.step += 1
        print(f"[STEP {self.step}] Print process trace")
        process_trace = agent.process_trace
        for trace_item in process_trace:
            print(f"🚩 {trace_item}")
        
        # Close using context.close() like FusedActs
        session.context.close()


def main():
    """Main function to run the example."""
    example = FusedActsStyleExample()
    example.run()


if __name__ == "__main__":
    main()
