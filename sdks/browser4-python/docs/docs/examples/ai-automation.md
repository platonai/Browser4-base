# AI-Powered Automation

This guide demonstrates AI-powered browser automation using Browser4's AgenticSession, which enables natural language control of browsers and intelligent data extraction.

## Prerequisites

AI-powered features require:

1. A running Browser4 server with AI capabilities
2. `OPENROUTER_API_KEY` environment variable set
3. Internet connection for AI model access

```bash
export OPENROUTER_API_KEY="your-api-key"
```

## AI Automation Overview

AgenticSession provides these AI-powered methods:

- `act()` - Execute single actions using natural language
- `run()` - Run autonomous multi-step tasks
- `observe()` - Analyze page state and get suggestions
- `agent_extract()` - AI-powered data extraction
- `summarize()` - Generate content summaries

## Single Actions with act()

### Basic Actions

Execute single browser actions using natural language:

```python
from browser4 import PulsarClient, AgenticSession

client = PulsarClient(base_url="http://localhost:8182")
client.create_session()
session = AgenticSession(client)

# Open a page
page = session.open("https://example.com")

# Execute AI-powered actions
result = session.act("click the login button")
print(f"Success: {result.success}")
print(f"Message: {result.message}")
print(f"Action: {result.action}")

# More examples
session.act("scroll to the bottom of the page")
session.act("click the first product in the list")
session.act("hover over the navigation menu")
session.act("fill in the search box with 'python'")

session.close()
client.close()
```

### Checking Action Results

Inspect action results in detail:

```python
result = session.act("click the 'Learn More' button")

# Check success
if result.success:
    print(f"Action completed: {result.action}")
    print(f"Message: {result.message}")
    print(f"Is task complete: {result.is_complete}")
else:
    print(f"Action failed: {result.message}")

# Check action metadata
print(f"Instruction: {result.instruction}")
print(f"Number: {result.number}")
```

### Conditional Actions

Use act() results to make decisions:

```python
# Try to click a button
result = session.act("click the 'Accept Cookies' button")

if result.success:
    print("Cookies accepted, continuing...")
    session.act("click the search icon")
else:
    print("No cookie prompt, proceeding directly...")
    session.act("click the search icon")
```

## Autonomous Tasks with run()

### Multi-Step Workflows

Execute complex multi-step tasks autonomously:

```python
from browser4 import PulsarClient, AgenticSession

client = PulsarClient(base_url="http://localhost:8182")
client.create_session()
session = AgenticSession(client)

# Open starting page
page = session.open("https://www.wikipedia.org")

# Run autonomous task
history = session.run(
    "search for 'artificial intelligence' and click on the first result"
)

print(f"Task success: {history.success}")
print(f"Final result: {history.final_result}")
print(f"Total actions: {history.history_size}")
print(f"Message: {history.message}")

session.close()
client.close()
```

### Complex Task Examples

```python
# E-commerce navigation
history = session.run("""
    1. Click on the 'Electronics' category
    2. Filter by laptops
    3. Sort by price (low to high)
    4. Click on the third product
""")

# Form completion
history = session.run("""
    find the contact form,
    fill in name as 'John Smith',
    fill in email as 'john@example.com',
    fill in message as 'Interested in your product',
    and submit the form
""")

# Research task
history = session.run("""
    search for 'python web scraping',
    open the first three results in new tabs,
    and return to the main page
""")
```

### Inspecting Task History

Review what the agent did:

```python
history = session.run("navigate to the pricing page")

# Check overall result
print(f"Success: {history.success}")
print(f"Message: {history.message}")

# Get state history
state_history = session.state_history

print(f"Total actions: {state_history.size}")
print(f"Has errors: {state_history.has_errors}")

# Review each step
for state in state_history.states:
    print(f"Step {state.step}:")
    print(f"  Action: {state.action}")
    print(f"  Success: {state.success}")
    print(f"  Message: {state.message}")
    if state.error:
        print(f"  Error: {state.error}")
```

## Page Observation

### Analyzing Page State

Get AI insights about the current page:

```python
from browser4 import PulsarClient, AgenticSession

client = PulsarClient(base_url="http://localhost:8182")
client.create_session()
session = AgenticSession(client)

page = session.open("https://example.com")

# Observe page
observations = session.observe("What interactive elements are available on this page?")

print(f"Found {len(observations.observations)} observations")

for obs in observations.observations:
    if obs.description:
        print(f"- {obs.description}")
    if obs.suggestion:
        print(f"  Suggestion: {obs.suggestion}")

session.close()
client.close()
```

### Context-Aware Observations

Ask specific questions about the page:

```python
# Check for specific functionality
obs1 = session.observe("Is there a login form on this page?")

# Get navigation suggestions
obs2 = session.observe("What are the main navigation options?")

# Identify data extraction opportunities
obs3 = session.observe("What data can I extract from this page?")

# Check form requirements
obs4 = session.observe("What fields are required in this form?")
```

### Using Observations for Decision Making

```python
def smart_navigation(session: AgenticSession, url: str):
    """Navigate intelligently based on page observations."""
    
    page = session.open(url)
    
    # Analyze the page
    observations = session.observe(
        "What are the key actions I can take on this page?"
    )
    
    # Find relevant action
    for obs in observations.observations:
        if obs.description and "login" in obs.description.lower():
            print("Found login option")
            session.act("click the login button")
            break
        elif obs.description and "search" in obs.description.lower():
            print("Found search option")
            session.act("fill in the search box with 'browser automation'")
            break

# Usage
session = AgenticSession(client)
smart_navigation(session, "https://example.com")
```

## AI-Powered Data Extraction

### Schema-Based Extraction

Extract data with AI understanding:

```python
from browser4 import PulsarClient, AgenticSession

client = PulsarClient(base_url="http://localhost:8182")
client.create_session()
session = AgenticSession(client)

page = session.open("https://example.com/products/laptop")

# Extract with AI
extraction = session.agent_extract(
    instruction="Extract product information",
    schema={
        "type": "object",
        "properties": {
            "name": {"type": "string"},
            "price": {"type": "number"},
            "description": {"type": "string"},
            "features": {
                "type": "array",
                "items": {"type": "string"}
            },
            "rating": {"type": "number"},
            "availability": {"type": "string"}
        }
    }
)

if extraction.success:
    print("Extracted data:")
    print(extraction.data)
else:
    print(f"Extraction failed: {extraction.message}")

session.close()
client.close()
```

### List Extraction

Extract lists of items:

```python
# Extract article list
extraction = session.agent_extract(
    instruction="Extract all articles with their titles, authors, and dates",
    schema={
        "type": "array",
        "items": {
            "type": "object",
            "properties": {
                "title": {"type": "string"},
                "author": {"type": "string"},
                "date": {"type": "string"},
                "summary": {"type": "string"}
            }
        }
    }
)

if extraction.success:
    articles = extraction.data
    print(f"Found {len(articles)} articles")
    for article in articles:
        print(f"- {article['title']} by {article['author']}")
```

### Complex Nested Extraction

Extract complex hierarchical data:

```python
# Extract product catalog
extraction = session.agent_extract(
    instruction="Extract product catalog with categories",
    schema={
        "type": "object",
        "properties": {
            "categories": {
                "type": "array",
                "items": {
                    "type": "object",
                    "properties": {
                        "name": {"type": "string"},
                        "products": {
                            "type": "array",
                            "items": {
                                "type": "object",
                                "properties": {
                                    "name": {"type": "string"},
                                    "price": {"type": "number"},
                                    "inStock": {"type": "boolean"}
                                }
                            }
                        }
                    }
                }
            }
        }
    }
)

if extraction.success:
    catalog = extraction.data
    for category in catalog.get("categories", []):
        print(f"Category: {category['name']}")
        for product in category.get("products", []):
            print(f"  - {product['name']}: ${product['price']}")
```

## Content Summarization

### Basic Summarization

Generate content summaries:

```python
from browser4 import PulsarClient, AgenticSession

client = PulsarClient(base_url="http://localhost:8182")
client.create_session()
session = AgenticSession(client)

page = session.open("https://en.wikipedia.org/wiki/Artificial_intelligence")

# Get summary
summary = session.summarize("Provide a brief summary of the main article")

print("Summary:")
print(summary)

session.close()
client.close()
```

### Targeted Summarization

Summarize specific aspects:

```python
# Summarize with specific focus
summary1 = session.summarize(
    "Summarize the key benefits mentioned on this product page"
)

summary2 = session.summarize(
    "What are the main features highlighted in the first three sections?"
)

summary3 = session.summarize(
    "Summarize the pricing information and available plans"
)

summary4 = session.summarize(
    "Extract the main arguments from this article"
)
```

## Complete AI Automation Example

Here's a comprehensive example combining multiple AI features:

```python
from browser4 import Browser4Driver, PulsarClient, AgenticSession

def ai_research_workflow(search_query: str):
    """Complete AI-powered research workflow."""
    
    with Browser4Driver() as driver:
        client = PulsarClient(base_url=driver.base_url)
        client.create_session()
        session = AgenticSession(client)
        
        try:
            # 1. Navigate to search engine
            print("Step 1: Opening search engine...")
            page = session.open("https://www.google.com")
            
            # 2. Observe the page
            print("\nStep 2: Analyzing page...")
            observations = session.observe("What can I do on this page?")
            for obs in observations.observations[:3]:
                if obs.description:
                    print(f"  - {obs.description}")
            
            # 3. Perform search
            print(f"\nStep 3: Searching for '{search_query}'...")
            history = session.run(f"search for '{search_query}' and press enter")
            print(f"  Search result: {history.final_result}")
            
            # 4. Extract search results
            print("\nStep 4: Extracting search results...")
            extraction = session.agent_extract(
                instruction="Extract the top 5 search results with titles and descriptions",
                schema={
                    "type": "array",
                    "items": {
                        "type": "object",
                        "properties": {
                            "title": {"type": "string"},
                            "description": {"type": "string"},
                            "url": {"type": "string"}
                        }
                    }
                }
            )
            
            if extraction.success:
                results = extraction.data
                print(f"  Found {len(results)} results")
                for i, result in enumerate(results[:3], 1):
                    print(f"    {i}. {result.get('title', 'N/A')}")
            
            # 5. Click first result
            print("\nStep 5: Opening first result...")
            session.act("click on the first search result")
            session.driver.delay(2000)
            
            # 6. Summarize the page
            print("\nStep 6: Summarizing content...")
            summary = session.summarize(
                "Provide a 2-3 sentence summary of the main content"
            )
            print(f"  Summary: {summary}")
            
            # 7. Review process trace
            print("\nStep 7: Process trace:")
            for entry in session.process_trace[-5:]:
                print(f"  🚩 {entry[:80]}...")
            
            # 8. Check history
            state_history = session.state_history
            print(f"\nTotal actions performed: {state_history.size}")
            print(f"Errors encountered: {state_history.has_errors}")
            
        finally:
            session.close()
            client.close()

if __name__ == "__main__":
    ai_research_workflow("Browser4 web automation")
```

## Managing Agent State

### Clearing History

Clear agent history between tasks:

```python
# Task 1
session.run("complete first task")
print(f"History size: {session.state_history.size}")

# Clear for new task
session.clear_history()
print(f"History size after clear: {session.state_history.size}")

# Task 2
session.run("complete second task")
```

### Accessing Process Trace

Review the agent's process trace:

```python
# Perform actions
session.act("click the button")
session.run("complete the form")

# Check trace
trace = session.process_trace
print(f"Total trace entries: {len(trace)}")

# Print recent entries
print("Recent trace:")
for entry in trace[-10:]:
    print(f"  {entry}")
```

## Error Handling for AI Operations

### Handling AI Failures

```python
from browser4 import AgenticSession

def safe_ai_operation(session: AgenticSession, instruction: str):
    """Safely execute AI operations with fallback."""
    
    try:
        result = session.act(instruction)
        
        if result.success:
            return result
        else:
            print(f"AI action failed: {result.message}")
            # Fallback to manual approach
            return None
            
    except Exception as e:
        print(f"AI operation error: {e}")
        # Handle error
        return None

# Usage
session = AgenticSession(client)
result = safe_ai_operation(session, "click the submit button")

if result is None:
    # Use WebDriver fallback
    session.driver.click("button[type='submit']")
```

### Retry Logic for AI Tasks

```python
def retry_ai_task(session: AgenticSession, instruction: str, max_retries: int = 3):
    """Retry AI task with multiple attempts."""
    
    for attempt in range(max_retries):
        try:
            result = session.run(instruction)
            
            if result.success:
                return result
            
            print(f"Attempt {attempt + 1} failed: {result.message}")
            
            # Clear history before retry
            session.clear_history()
            
        except Exception as e:
            print(f"Attempt {attempt + 1} error: {e}")
    
    print(f"All {max_retries} attempts failed")
    return None

# Usage
result = retry_ai_task(session, "navigate to checkout and enter payment info")
```

## Best Practices

### 1. Clear Instructions

Provide clear, specific instructions:

```python
# ✅ Good: Specific and clear
session.act("click the blue 'Submit' button in the bottom right")

# ❌ Bad: Vague
session.act("click something")
```

### 2. Break Down Complex Tasks

Split complex tasks into steps:

```python
# ✅ Good: Broken down
session.act("fill in the email field with 'user@example.com'")
session.act("fill in the password field with 'password123'")
session.act("click the login button")

# ❌ Bad: Too complex for single act()
session.act("fill in email and password then login")
# Better: Use run() for multi-step
session.run("fill in email and password then login")
```

### 3. Verify AI Results

Always check AI operation results:

```python
result = session.act("click the 'Add to Cart' button")

if result.success:
    print("Item added to cart")
    # Verify with WebDriver
    cart_count = session.driver.select_first_text_or_null(".cart-count")
    print(f"Cart now has {cart_count} items")
else:
    print("Failed to add item")
```

### 4. Combine AI and Traditional Methods

Use AI where it excels, WebDriver for precision:

```python
# Use AI for navigation
session.run("navigate to the product catalog")

# Use WebDriver for precise extraction
driver = session.driver
products = driver.select_text_all(".product-name")
prices = driver.select_text_all(".product-price")
```

## Next Steps

- [Complete Workflow](complete-workflow.md) - End-to-end AI-powered pipelines
- [AgenticSession API Reference](../api/agentic-session.md) - Full AI API documentation
- [Configuration](../configuration/environment-variables.md) - AI configuration options
