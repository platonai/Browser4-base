"""
Advanced AgenticSession example for Browser4 Python SDK.

This example demonstrates advanced features:
- AI-powered actions with act()
- Autonomous multi-step tasks with run()
- Page observation and suggestions
- AI-powered data extraction
- Page summarization
- Agent state history tracking

Prerequisites:
- A running Browser4 server with AI capabilities
- OPENROUTER_API_KEY environment variable set
"""

from browser4 import PulsarClient, AgenticSession


def main():
    print("=== Browser4 Python SDK - Agentic Session Example ===\n")

    # Create client and session
    client = PulsarClient(base_url="http://localhost:8182")

    try:
        session_id = client.create_session()
        print(f"Session created: {session_id}\n")

        session = AgenticSession(client)

        # Open a page
        print("Opening example.com...")
        page = session.open("https://example.com")
        print(f"Page loaded: {page.url}\n")

        # AI-powered single action
        print("1. Testing act() - Single AI action")
        print("   Executing: 'click the first link'")
        act_result = session.act("click the first link")
        print(f"   Success: {act_result.success}")
        print(f"   Message: {act_result.message}")
        print(f"   Is complete: {act_result.is_complete}\n")

        # Autonomous multi-step task
        print("2. Testing run() - Autonomous task")
        print("   Task: 'scroll to the bottom and back to top'")
        run_result = session.run("scroll to the bottom and back to top")
        print(f"   Success: {run_result.success}")
        print(f"   Message: {run_result.message}")
        print(f"   History size: {run_result.history_size}")
        print(f"   Final result: {run_result.final_result}\n")

        # Page observation
        print("3. Testing observe() - Page analysis")
        observations = session.observe("What interactive elements are on this page?")
        print(f"   Found {len(observations.observations)} observations")
        for i, obs in enumerate(observations.observations[:3], 1):
            if obs.description:
                print(f"   {i}. {obs.description}")
        print()

        # AI-powered extraction
        print("4. Testing agent_extract() - AI data extraction")
        extraction = session.agent_extract(
            instruction="Extract the main heading and first paragraph",
            schema={"type": "object", "properties": {
                "heading": {"type": "string"},
                "paragraph": {"type": "string"}
            }}
        )
        print(f"   Success: {extraction.success}")
        print(f"   Extracted data: {extraction.data}\n")

        # Page summarization
        print("5. Testing summarize() - Content summary")
        summary = session.summarize("Provide a brief summary of this page")
        print(f"   Summary: {summary[:200]}...\n")

        # Check agent state history
        print("6. Agent state history")
        history = session.state_history
        print(f"   Total actions in history: {history.size}")
        print(f"   Has errors: {history.has_errors}")
        for i, state in enumerate(history.states[:3], 1):
            print(f"   Step {state.step}: {state.action[:50]}... "
                  f"(success: {state.success})")
        print()

        # Check process trace
        print("7. Process trace")
        trace = session.process_trace
        print(f"   Total trace entries: {len(trace)}")
        if trace:
            print(f"   Recent entries:")
            for entry in trace[-3:]:
                print(f"   - {entry[:80]}...")
        print()

        # Clear history for new task
        print("8. Clearing history")
        session.clear_history()
        print(f"   History size after clear: {session.state_history.size}")
        print(f"   Trace size after clear: {len(session.process_trace)}\n")

        # Clean up
        print("Closing session...")
        session.close()
        print("Session closed successfully!")

    except Exception as e:
        print(f"Error: {e}")
        print("\nNote: This example requires:")
        print("1. A running Browser4 server with AI capabilities")
        print("2. OPENROUTER_API_KEY environment variable set")
        print("3. Internet connection for AI model access")
    finally:
        client.close()

    print("\n=== Example completed ===")


if __name__ == "__main__":
    main()
