# AI Automation

Leverage AI-powered browser automation with natural language instructions. AgenticSession provides intelligent automation capabilities including autonomous task execution, page observation, smart extraction, and content summarization.

## AI Session Setup

### Prerequisites

AI capabilities require:
- A running Browser4 server with AI features enabled
- `OPENROUTER_API_KEY` environment variable set
- Internet connection for AI model access

```python
import os

# Set API key
os.environ['OPENROUTER_API_KEY'] = 'your-api-key-here'

# Or configure in server properties
# spring.profiles.active=rest,private
```

### Creating an AI Session

```python
from browser4 import Browser4Driver, PulsarClient, AgenticSession

with Browser4Driver() as driver:
    client = PulsarClient(base_url=driver.base_url)
    session_id = client.create_session(capabilities={
        "browserName": "chrome",
        "enableAI": True
    })
    session = AgenticSession(client)
    
    # Now you can use AI features
    session.open("https://example.com")
```

## AI Methods Overview

AgenticSession provides these AI-powered methods:

| Method | Purpose | Returns |
|--------|---------|---------|
| `act()` | Execute single action | `AgentActResult` |
| `run()` | Execute multi-step task | `AgentRunResult` |
| `observe()` | Analyze page state | `ObserveResult` |
| `agent_extract()` | Extract with AI | `ExtractionResult` |
| `summarize()` | Summarize content | `str` |

## act() - Single Actions

Execute a single action with natural language:

```python
session = AgenticSession(client)
session.open("https://example.com")

# Click actions
result = session.act("click the login button")
print(f"Success: {result.success}")
print(f"Message: {result.message}")

# Form filling
result = session.act("fill in the email field with user@example.com")
print(f"Action: {result.action}")

# Navigation
result = session.act("scroll to the bottom of the page")
print(f"Complete: {result.is_complete}")

# Search operations
result = session.act("type 'python programming' in the search box")
print(f"Result: {result.message}")
```

### Act Result

```python
result = session.act("click the submit button")

# Check result properties
print(f"Success: {result.success}")        # bool: action succeeded
print(f"Action: {result.action}")          # str: action performed
print(f"Message: {result.message}")        # str: result message
print(f"Complete: {result.is_complete}")   # bool: task complete
print(f"Observation: {result.observation}") # str: page observation
```

### Act Examples

```python
session.open("https://example.com/search")

# Simple clicks
session.act("click the first result")
session.act("click on the menu icon")
session.act("press the enter key")

# Form interactions
session.act("fill the username field with 'john_doe'")
session.act("type 'secure123' in the password field")
session.act("check the remember me checkbox")
session.act("select 'United States' from the country dropdown")

# Navigation
session.act("scroll down to see more content")
session.act("go back to the previous page")
session.act("refresh the page")

# Complex actions
session.act("click the 3rd link in the sidebar")
session.act("hover over the profile menu and click settings")
session.act("find and click the download button")
```

## run() - Multi-Step Tasks

Execute autonomous multi-step tasks:

```python
session = AgenticSession(client)
session.open("https://example.com")

# Run a complete task
result = session.run("""
    1. Find the search box
    2. Type 'web scraping' 
    3. Press Enter
    4. Wait for results to load
    5. Click on the first result
""")

print(f"Success: {result.success}")
print(f"Message: {result.message}")
print(f"History size: {result.history_size}")
print(f"Final result: {result.final_result}")
```

### Run Result

```python
result = session.run("complete the signup form with test data")

# Check result properties
print(f"Success: {result.success}")           # bool: task succeeded
print(f"Message: {result.message}")           # str: completion message
print(f"History size: {result.history_size}") # int: steps taken
print(f"Final result: {result.final_result}") # str: final outcome
print(f"Error: {result.error}")               # str: error if failed
```

### Run Examples

```python
session.open("https://example.com")

# Complete workflows
result = session.run("""
    Navigate to the login page, 
    fill in credentials with username 'test' and password 'pass123',
    and submit the form
""")

# Search and filter
result = session.run("""
    Search for 'python books',
    filter by 'newest first',
    and open the top result
""")

# Data collection
result = session.run("""
    Go through the first 5 product listings,
    note the titles and prices,
    and return to the main page
""")

# Complex navigation
result = session.run("""
    1. Click on the Products menu
    2. Select the Electronics category
    3. Filter by price range $100-$500
    4. Sort by customer rating
    5. Open the highest-rated item
""")
```

## observe() - Page Analysis

Analyze page state and get suggestions:

```python
session = AgenticSession(client)
session.open("https://example.com")

# Observe page
observations = session.observe("What interactive elements are on this page?")

print(f"Found {len(observations.observations)} observations")
for obs in observations.observations:
    if obs.description:
        print(f"- {obs.description}")
    if obs.selector:
        print(f"  Selector: {obs.selector}")
    if obs.action:
        print(f"  Suggested action: {obs.action}")
```

### Observe Examples

```python
# General page analysis
observations = session.observe("What can I do on this page?")

# Specific queries
observations = session.observe("What forms are available?")
observations = session.observe("What links lead to other pages?")
observations = session.observe("Are there any buttons I can click?")
observations = session.observe("What data can I extract from this page?")

# Troubleshooting
observations = session.observe("Why can't I see the login button?")
observations = session.observe("What elements are currently visible?")
```

### Using Observations

```python
# Get observations
observations = session.observe("What actions are available?")

# Act on observations
for obs in observations.observations:
    if obs.action and "submit" in obs.action.lower():
        print(f"Found submit action: {obs.description}")
        if obs.selector:
            driver = session.driver
            driver.click(obs.selector)
            break
```

## agent_extract() - AI Extraction

Extract structured data with AI:

```python
session = AgenticSession(client)
session.open("https://example.com/products")

# Define extraction schema
extraction = session.agent_extract(
    instruction="Extract all product information including name, price, and rating",
    schema={
        "type": "array",
        "items": {
            "type": "object",
            "properties": {
                "name": {"type": "string"},
                "price": {"type": "number"},
                "rating": {"type": "number"},
                "available": {"type": "boolean"}
            }
        }
    }
)

if extraction.success:
    products = extraction.data
    print(f"Extracted {len(products)} products")
    for product in products:
        print(f"- {product['name']}: ${product['price']}")
else:
    print(f"Extraction failed: {extraction.error}")
```

### Extraction Schemas

```python
# Simple object extraction
schema = {
    "type": "object",
    "properties": {
        "title": {"type": "string"},
        "author": {"type": "string"},
        "date": {"type": "string"}
    }
}

# Array of items
schema = {
    "type": "array",
    "items": {
        "type": "object",
        "properties": {
            "name": {"type": "string"},
            "value": {"type": "number"}
        }
    }
}

# Nested structure
schema = {
    "type": "object",
    "properties": {
        "article": {
            "type": "object",
            "properties": {
                "title": {"type": "string"},
                "content": {"type": "string"}
            }
        },
        "author": {
            "type": "object",
            "properties": {
                "name": {"type": "string"},
                "bio": {"type": "string"}
            }
        },
        "comments": {
            "type": "array",
            "items": {
                "type": "object",
                "properties": {
                    "user": {"type": "string"},
                    "text": {"type": "string"}
                }
            }
        }
    }
}
```

### Extraction Examples

```python
# Extract article information
extraction = session.agent_extract(
    instruction="Extract the article title, author, publication date, and main content",
    schema={
        "type": "object",
        "properties": {
            "title": {"type": "string"},
            "author": {"type": "string"},
            "published": {"type": "string"},
            "content": {"type": "string"}
        }
    }
)

# Extract table data
extraction = session.agent_extract(
    instruction="Extract all rows from the comparison table",
    schema={
        "type": "array",
        "items": {
            "type": "object",
            "properties": {
                "feature": {"type": "string"},
                "plan_basic": {"type": "string"},
                "plan_pro": {"type": "string"},
                "plan_enterprise": {"type": "string"}
            }
        }
    }
)

# Extract contact information
extraction = session.agent_extract(
    instruction="Find all contact details on the page",
    schema={
        "type": "object",
        "properties": {
            "email": {"type": "string"},
            "phone": {"type": "string"},
            "address": {"type": "string"},
            "social_media": {
                "type": "object",
                "properties": {
                    "twitter": {"type": "string"},
                    "linkedin": {"type": "string"}
                }
            }
        }
    }
)
```

## summarize() - Content Summary

Generate natural language summaries:

```python
session = AgenticSession(client)
session.open("https://example.com/article")

# Summarize page content
summary = session.summarize("Provide a brief summary of this article")
print(f"Summary: {summary}")

# Specific summaries
summary = session.summarize("What are the main points of this article?")
summary = session.summarize("Summarize the pricing information")
summary = session.summarize("What are the key features mentioned?")
summary = session.summarize("Explain what this product does in simple terms")
```

### Summarize Examples

```python
# Article summary
summary = session.summarize(
    "Summarize this article in 3-4 sentences covering the main ideas"
)

# Product summary  
summary = session.summarize(
    "What is this product and who is it for?"
)

# Comparison summary
summary = session.summarize(
    "Compare the different pricing plans and their key differences"
)

# Technical summary
summary = session.summarize(
    "Explain the technical requirements and installation process"
)
```

## Complete AI Workflow

```python
from browser4 import Browser4Driver, PulsarClient, AgenticSession

def ai_automation_demo():
    """Comprehensive AI automation demonstration."""
    
    with Browser4Driver() as driver:
        client = PulsarClient(base_url=driver.base_url)
        session_id = client.create_session()
        session = AgenticSession(client)
        
        try:
            # 1. Open and observe
            print("1. Opening page and observing...")
            session.open("https://example.com")
            
            observations = session.observe("What can I do on this page?")
            print(f"   Found {len(observations.observations)} possible actions")
            
            # 2. Execute single action
            print("\n2. Executing single action...")
            result = session.act("scroll down to see more content")
            print(f"   Action success: {result.success}")
            print(f"   Message: {result.message}")
            
            # 3. Run multi-step task
            print("\n3. Running multi-step task...")
            result = session.run("""
                Find the search functionality,
                search for 'automation',
                and open the first result
            """)
            print(f"   Task success: {result.success}")
            print(f"   Steps taken: {result.history_size}")
            
            # 4. Extract structured data
            print("\n4. Extracting structured data...")
            extraction = session.agent_extract(
                instruction="Extract the page title and main content",
                schema={
                    "type": "object",
                    "properties": {
                        "title": {"type": "string"},
                        "content": {"type": "string"}
                    }
                }
            )
            
            if extraction.success:
                print(f"   Title: {extraction.data.get('title', 'N/A')}")
                content = extraction.data.get('content', '')
                print(f"   Content: {content[:100]}...")
            
            # 5. Summarize content
            print("\n5. Summarizing content...")
            summary = session.summarize("Summarize the main topic of this page")
            print(f"   Summary: {summary[:200]}...")
            
            # 6. Check process trace
            print("\n6. Process trace:")
            for i, trace in enumerate(session.process_trace[-5:], 1):
                print(f"   {i}. {trace}")
            
            # 7. Check agent state
            print("\n7. Agent state:")
            history = session.state_history
            print(f"   Total actions: {history.size}")
            print(f"   Has errors: {history.has_errors}")
            
        finally:
            session.close()
            client.close()

if __name__ == "__main__":
    ai_automation_demo()
```

## Agent State Management

### State History

Track agent actions and state:

```python
session = AgenticSession(client)

# Perform actions
session.act("click button")
session.run("complete task")

# Access state history
history = session.state_history
print(f"Total actions: {history.size}")
print(f"Has errors: {history.has_errors}")

# View individual states
for state in history.states:
    print(f"Step {state.step}: {state.action}")
    print(f"  Success: {state.success}")
    print(f"  Result: {state.result}")
```

### Process Trace

Track all operations:

```python
session = AgenticSession(client)

# Perform operations
session.open("https://example.com")
session.act("scroll down")
session.run("search for python")

# View process trace
trace = session.process_trace
print(f"Total operations: {len(trace)}")

for entry in trace:
    print(f"🚩 {entry}")
```

### Clear History

Start fresh for new tasks:

```python
session = AgenticSession(client)

# First task
session.run("complete registration form")
print(f"History size: {len(session.process_trace)}")

# Clear for new independent task
session.clear_history()
print(f"After clear: {len(session.process_trace)}")

# Second task with fresh context
session.run("search for products")
```

## AI Automation Patterns

### Exploration Pattern

```python
def explore_page(session, url):
    """Explore a page with AI."""
    
    session.open(url)
    
    # Observe page
    observations = session.observe("What are all the interactive elements?")
    
    # Summarize purpose
    summary = session.summarize("What is this page about?")
    print(f"Page purpose: {summary}")
    
    # Extract key data
    extraction = session.agent_extract(
        instruction="Extract important information from this page",
        schema={
            "type": "object",
            "properties": {
                "main_topic": {"type": "string"},
                "key_points": {"type": "array", "items": {"type": "string"}}
            }
        }
    )
    
    return {
        "summary": summary,
        "observations": observations.observations,
        "data": extraction.data if extraction.success else None
    }

# Usage
info = explore_page(session, "https://example.com")
```

### Task Automation Pattern

```python
def automate_workflow(session, instructions):
    """Execute a complete workflow with AI."""
    
    # Execute main task
    result = session.run(instructions)
    
    if not result.success:
        print(f"Task failed: {result.error}")
        
        # Observe to diagnose
        observations = session.observe("Why did the task fail?")
        
        # Retry with adjustments
        result = session.run(instructions + " (retry with alternative approach)")
    
    return result

# Usage
result = automate_workflow(session, """
    Navigate to products page,
    filter by category 'Electronics',
    and find items under $100
""")
```

### Intelligent Extraction Pattern

```python
def smart_extract(session, url, description):
    """Intelligently extract data from a page."""
    
    session.open(url)
    
    # First observe to understand page structure
    observations = session.observe(
        f"Where can I find {description}?"
    )
    
    # Then extract based on observations
    extraction = session.agent_extract(
        instruction=f"Extract {description}",
        schema={"type": "object"}  # Let AI determine structure
    )
    
    return extraction.data if extraction.success else None

# Usage
data = smart_extract(
    session,
    "https://example.com/product",
    "product specifications and pricing"
)
```

## Troubleshooting

### AI Not Responding

```python
# Check API key configured
import os
if not os.getenv('OPENROUTER_API_KEY'):
    print("OPENROUTER_API_KEY not set")

# Check server has AI enabled
# Ensure server started with AI capabilities

# Verify session created with AI support
session_id = client.create_session(capabilities={"enableAI": True})
```

### Actions Failing

```python
# Add observation before action
observations = session.observe("Can I click the submit button?")
for obs in observations.observations:
    print(f"Observation: {obs.description}")

# Try simpler action
result = session.act("click button")  # Instead of complex instruction

# Check process trace for errors
for trace in session.process_trace:
    if "error" in trace.lower():
        print(f"Error in trace: {trace}")
```

### Extraction Issues

```python
# Simplify schema
schema = {
    "type": "object",
    "properties": {
        "title": {"type": "string"}
    }
}

# Make instruction more specific
extraction = session.agent_extract(
    instruction="Extract the main heading at the top of the page",
    schema=schema
)

# Fallback to traditional extraction
if not extraction.success:
    document = session.parse(session.capture(session.driver))
    data = session.extract(document, {"title": "h1"})
```

### Timeout Issues

```python
# Increase client timeout
client = PulsarClient(
    base_url=driver.base_url,
    timeout=120.0  # 2 minutes for AI operations
)

# Break complex tasks into smaller steps
session.act("navigate to products")
session.act("filter by category")
session.act("select first item")

# Instead of one large run()
# session.run("navigate to products, filter, and select first item")
```

## Best Practices

1. **Start with observation** - Use `observe()` to understand page state
2. **Be specific in instructions** - Clear instructions get better results
3. **Use appropriate method** - `act()` for single actions, `run()` for workflows
4. **Define clear schemas** - Structured schemas improve extraction accuracy
5. **Monitor state history** - Track actions and results
6. **Clear history between tasks** - Start fresh for independent tasks
7. **Handle failures gracefully** - Check success and provide fallbacks
8. **Use process trace** - Debug issues by reviewing trace
9. **Combine AI with traditional methods** - Use AI where it adds value
10. **Test with simple cases first** - Validate AI features before complex tasks

## Performance Tips

### Optimize Instructions

```python
# Good: Specific and clear
session.act("click the blue submit button in the form")

# Less optimal: Vague
session.act("submit")
```

### Batch Related Actions

```python
# Efficient: One run() call
result = session.run("""
    Fill username with 'user',
    fill password with 'pass',
    click submit
""")

# Less efficient: Multiple act() calls
# session.act("fill username")
# session.act("fill password")
# session.act("click submit")
```

### Cache AI Results

```python
# Store extraction results
extraction_cache = {}

def cached_extract(session, url, instruction, schema):
    """Extract with caching."""
    
    cache_key = f"{url}:{instruction}"
    
    if cache_key in extraction_cache:
        return extraction_cache[cache_key]
    
    session.open(url)
    extraction = session.agent_extract(instruction, schema)
    
    if extraction.success:
        extraction_cache[cache_key] = extraction.data
    
    return extraction.data

# Usage
data = cached_extract(session, url, "extract products", schema)
```

## Next Steps

- **[Element Interaction](element-interaction.md)** - Manual interaction methods
- **[Data Extraction](data-extraction.md)** - Traditional extraction techniques
- **[Session Management](session-management.md)** - Managing AI sessions
- **[Navigation](navigation.md)** - Page navigation basics
