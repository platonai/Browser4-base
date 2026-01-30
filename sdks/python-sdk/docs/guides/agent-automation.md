# Agent Automation Guide

This guide covers AI-powered browser automation using the Browser4 AgenticSession. Learn how to use natural language instructions to control browsers, extract data, and complete complex multi-step tasks autonomously.

## Table of Contents

- [Introduction to Agentic Capabilities](#introduction-to-agentic-capabilities)
- [Getting Started with AgenticSession](#getting-started-with-agenticsession)
- [Single Actions with act()](#single-actions-with-act)
- [Multi-Step Tasks with run()](#multi-step-tasks-with-run)
- [Page Analysis with observe()](#page-analysis-with-observe)
- [AI-Powered Extraction](#ai-powered-extraction)
- [Content Summarization](#content-summarization)
- [Managing Agent History](#managing-agent-history)
- [When to Use AI vs WebDriver](#when-to-use-ai-vs-webdriver)
- [Best Practices for AI Instructions](#best-practices-for-ai-instructions)
- [Debugging Agent Tasks](#debugging-agent-tasks)

---

## Introduction to Agentic Capabilities

The `AgenticSession` extends traditional web scraping with AI-powered capabilities:

- **Natural Language Control**: Describe actions in plain English instead of writing code
- **Intelligent Element Location**: AI finds the right elements even without perfect selectors
- **Multi-Step Reasoning**: Complete complex tasks that require decision-making
- **Adaptive Extraction**: Extract data based on intent rather than rigid rules
- **Context Awareness**: Understands page state and previous actions

**Key Use Cases:**

- Interactive forms with complex validation
- Dynamic single-page applications (SPAs)
- Sites with frequently changing HTML structure
- Tasks requiring human-like decision-making
- Exploratory data gathering

---

## Getting Started with AgenticSession

### Basic Setup

```python
from pulsar_sdk import PulsarClient, AgenticSession

# Create client and session
client = PulsarClient(base_url="http://localhost:8182")
session_id = client.create_session()

# Create AgenticSession
session = AgenticSession(client)

# AgenticSession has all PulsarSession methods PLUS agent methods
print(f"Session: {session.display}")
print(f"Active: {session.is_active}")
```

### Quick Example

```python
from pulsar_sdk import PulsarClient, AgenticSession

def quick_agent_example():
    """Quick example of agent automation."""
    client = PulsarClient()
    client.create_session()
    session = AgenticSession(client)
    
    try:
        # Open a page
        session.open("https://example.com")
        
        # Use AI to perform actions
        result = session.act("click the contact link")
        print(f"Action result: {result.message}")
        
        # Check if successful
        if result.success:
            print("✓ Successfully clicked contact link")
        
    finally:
        session.close()
        client.close()

quick_agent_example()
```

---

## Single Actions with act()

The `act()` method executes a single action described in natural language.

### Basic Usage

```python
from pulsar_sdk import PulsarClient, AgenticSession

client = PulsarClient()
client.create_session()
session = AgenticSession(client)

# Open a page
session.open("https://example.com")

# Single action: click a button
result = session.act("click the search button")

print(f"Success: {result.success}")
print(f"Message: {result.message}")
print(f"Action taken: {result.action}")

session.close()
client.close()
```

### Common Single Actions

```python
# Click elements
session.act("click the login button")
session.act("click the first product in the list")
session.act("click the 'Learn More' link")

# Fill forms
session.act("type 'john@example.com' in the email field")
session.act("fill the search box with 'python tutorials'")
session.act("enter 'password123' in the password input")

# Check/uncheck
session.act("check the 'Remember me' checkbox")
session.act("uncheck the newsletter subscription")

# Select options
session.act("select 'United States' from the country dropdown")
session.act("choose the 'Premium' plan")

# Scroll
session.act("scroll down to the footer")
session.act("scroll to the pricing section")

# Navigate
session.act("go back to the previous page")
session.act("refresh the page")
```

### Handling Action Results

```python
def perform_action(session, action_description):
    """Perform an action and check result."""
    result = session.act(action_description)
    
    if result.success:
        print(f"✓ {action_description}: Success")
        print(f"  Message: {result.message}")
        
        if result.tool_calls:
            print(f"  Tool calls: {len(result.tool_calls)}")
            for tool_call in result.tool_calls:
                print(f"    - {tool_call}")
    else:
        print(f"✗ {action_description}: Failed")
        print(f"  Error: {result.message}")
    
    return result

# Use it
session.open("https://github.com/login")
perform_action(session, "fill username with 'testuser'")
perform_action(session, "fill password with 'testpass'")
perform_action(session, "click the sign in button")
```

### Multi-Act Mode

For chained actions, use `multi_act=True` to maintain context:

```python
# First action sets context
result1 = session.act("focus on the search section", multi_act=True)

# Subsequent actions use that context
result2 = session.act("type 'machine learning'", multi_act=True)
result3 = session.act("click the search button", multi_act=True)

print("Chained actions completed")
```

### Advanced Options

```python
# Custom model
result = session.act(
    "click the submit button",
    model_name="gpt-4"
)

# Custom timeout
result = session.act(
    "wait for the results to load",
    timeout_ms=30000  # 30 seconds
)

# With variables
result = session.act(
    "fill the email field with {email}",
    variables={"email": "user@example.com"}
)

# DOM settle timeout
result = session.act(
    "click the load more button",
    dom_settle_timeout_ms=5000  # Wait 5s for DOM to stabilize
)
```

---

## Multi-Step Tasks with run()

The `run()` method executes autonomous multi-step tasks using an observe-act loop.

### Basic Multi-Step Tasks

```python
from pulsar_sdk import PulsarClient, AgenticSession

client = PulsarClient()
client.create_session()
session = AgenticSession(client)

# Navigate to starting page
session.open("https://example.com")

# Run multi-step task
result = session.run(
    "find the search box, type 'browser automation', and submit the form"
)

print(f"Task completed: {result.success}")
print(f"Final result: {result.final_result}")
print(f"Steps taken: {result.history_size}")

session.close()
client.close()
```

### Complex Task Examples

```python
# E-commerce workflow
result = session.run("""
    1. Search for 'wireless headphones'
    2. Click on the first product with rating above 4 stars
    3. Add to cart
    4. View the cart
""")

# Research workflow
result = session.run("""
    Go to the documentation page, find the API reference section,
    and locate the information about authentication methods
""")

# Data collection workflow
result = session.run("""
    Navigate to the products page, scroll through all products,
    and note the names and prices of items on sale
""")
```

### Handling Task Results

```python
def run_task_safely(session, task_description):
    """Run a task and provide detailed feedback."""
    print(f"Starting task: {task_description}")
    
    result = session.run(task_description)
    
    if result.success:
        print(f"✓ Task completed successfully")
        print(f"  Final result: {result.final_result}")
        print(f"  Steps: {result.history_size}")
        
        # Access history
        if hasattr(result, 'history') and result.history:
            print("  Action history:")
            for i, action in enumerate(result.history, 1):
                print(f"    {i}. {action}")
    else:
        print(f"✗ Task failed")
        print(f"  Message: {result.message}")
        print(f"  Completed steps: {result.history_size}")
    
    return result

# Use it
session.open("https://example.com")
run_task_safely(session, "find and click the pricing link")
```

### Task with Extraction

```python
# Run task and extract resulting data
session.open("https://shop.example.com")

result = session.run("""
    Search for 'laptop', apply the filter for 'Gaming',
    sort by price descending, and show the top 3 results
""")

if result.success:
    # Now extract the data
    driver = session.driver
    products = []
    
    titles = driver.select_text_all(".product-card h3")
    prices = driver.select_text_all(".product-card .price")
    
    for title, price in zip(titles[:3], prices[:3]):
        products.append({"title": title, "price": price})
    
    print("Top 3 gaming laptops:")
    for p in products:
        print(f"  - {p['title']}: {p['price']}")
```

### Conditional Tasks

```python
def conditional_automation(session, url):
    """Run different tasks based on page content."""
    session.open(url)
    
    # Check what's on the page
    observation = session.observe("what type of page is this?")
    
    # Run different tasks based on observation
    if "login" in observation.description.lower():
        result = session.run("""
            Fill in username with 'testuser',
            fill in password,
            and click sign in
        """)
    elif "product" in observation.description.lower():
        result = session.run("""
            Find the product price,
            check if there's a discount,
            and add to cart if price is under $100
        """)
    else:
        result = session.run("""
            Explore the page and find the main call-to-action
        """)
    
    return result
```

---

## Page Analysis with observe()

The `observe()` method analyzes the current page and suggests possible actions.

### Basic Observation

```python
from pulsar_sdk import PulsarClient, AgenticSession

client = PulsarClient()
client.create_session()
session = AgenticSession(client)

# Navigate to page
session.open("https://example.com")

# Observe the page
observation = session.observe()

print(f"Page analysis: {observation.description}")
print(f"Suggested actions: {len(observation.observations)}")

for obs in observation.observations:
    print(f"  - {obs}")

session.close()
client.close()
```

### Observation with Instructions

```python
# Specific observation goal
session.open("https://github.com/explore")

observation = session.observe("what are the trending repositories?")
print(f"Analysis: {observation.description}")

# Look for specific elements
observation = session.observe("where is the search functionality?")
print(f"Search location: {observation.description}")

# Analyze form
session.open("https://example.com/signup")
observation = session.observe("what fields are required in this form?")
print(f"Form analysis: {observation.description}")
```

### Using Observations to Guide Actions

```python
def intelligent_navigation(session, url, goal):
    """Navigate intelligently using observations."""
    session.open(url)
    
    # Observe the page
    observation = session.observe(f"How can I {goal}?")
    
    print(f"Page understanding: {observation.description}")
    
    # Get suggested actions
    if observation.suggested_actions:
        print("Suggested approach:")
        for action in observation.suggested_actions:
            print(f"  - {action}")
        
        # Execute the first suggested action
        if len(observation.suggested_actions) > 0:
            result = session.act(observation.suggested_actions[0])
            return result
    
    return None

# Use intelligent navigation
result = intelligent_navigation(
    session,
    "https://example.com",
    "contact the support team"
)
```

### Observation Options

```python
# Without visual overlay
observation = session.observe(
    "analyze this page",
    draw_overlay=False
)

# Return actionable tool calls
observation = session.observe(
    "what can I click?",
    return_action=True
)

# Custom model
observation = session.observe(
    "describe this page layout",
    model_name="gpt-4"
)

# With DOM settle timeout
observation = session.observe(
    "analyze after animations complete",
    dom_settle_timeout_ms=3000
)
```

---

## AI-Powered Extraction

The `agent_extract()` method uses AI to extract structured data based on natural language instructions.

### Basic AI Extraction

```python
from pulsar_sdk import PulsarClient, AgenticSession

client = PulsarClient()
client.create_session()
session = AgenticSession(client)

# Navigate to page
session.open("https://blog.example.com/post/123")

# Extract with natural language
extraction = session.agent_extract(
    instruction="Extract the article title, author, publication date, and main content"
)

if extraction.success:
    print("Extracted data:")
    print(extraction.data)

session.close()
client.close()
```

### Structured Extraction with Schema

```python
# Define the expected structure
schema = {
    "type": "object",
    "properties": {
        "title": {"type": "string"},
        "author": {"type": "string"},
        "date": {"type": "string"},
        "tags": {
            "type": "array",
            "items": {"type": "string"}
        },
        "content": {"type": "string"}
    },
    "required": ["title", "author"]
}

# Extract with schema
extraction = session.agent_extract(
    instruction="Extract article information",
    schema=schema
)

if extraction.success:
    data = extraction.data
    print(f"Title: {data.get('title')}")
    print(f"Author: {data.get('author')}")
    print(f"Tags: {data.get('tags')}")
```

### Extract Lists

```python
# Extract list of products
session.open("https://shop.example.com/category/electronics")

schema = {
    "type": "array",
    "items": {
        "type": "object",
        "properties": {
            "name": {"type": "string"},
            "price": {"type": "number"},
            "rating": {"type": "number"},
            "in_stock": {"type": "boolean"}
        }
    }
}

extraction = session.agent_extract(
    instruction="Extract all products with their name, price, rating, and stock status",
    schema=schema
)

if extraction.success:
    products = extraction.data
    for product in products:
        print(f"- {product['name']}: ${product['price']}")
```

### Scoped Extraction

```python
# Extract from specific section
session.open("https://example.com/article")

extraction = session.agent_extract(
    instruction="Extract all links and their descriptions",
    selector=".sidebar .related-articles"  # Only look in sidebar
)

print("Related articles:")
for item in extraction.data:
    print(f"  - {item}")
```

### Complex Extraction Example

```python
def extract_structured_data(session, url):
    """Extract complex structured data from a page."""
    session.open(url)
    
    # Define comprehensive schema
    schema = {
        "type": "object",
        "properties": {
            "metadata": {
                "type": "object",
                "properties": {
                    "title": {"type": "string"},
                    "author": {"type": "string"},
                    "published": {"type": "string"}
                }
            },
            "products": {
                "type": "array",
                "items": {
                    "type": "object",
                    "properties": {
                        "name": {"type": "string"},
                        "price": {"type": "number"},
                        "features": {
                            "type": "array",
                            "items": {"type": "string"}
                        }
                    }
                }
            },
            "reviews": {
                "type": "array",
                "items": {
                    "type": "object",
                    "properties": {
                        "author": {"type": "string"},
                        "rating": {"type": "number"},
                        "comment": {"type": "string"}
                    }
                }
            }
        }
    }
    
    extraction = session.agent_extract(
        instruction="Extract page metadata, all products with features, and user reviews",
        schema=schema
    )
    
    return extraction.data if extraction.success else None

# Use it
data = extract_structured_data(session, "https://example.com/product/123")
if data:
    print(f"Title: {data['metadata']['title']}")
    print(f"Products: {len(data['products'])}")
    print(f"Reviews: {len(data['reviews'])}")
```

---

## Content Summarization

The `summarize()` method generates concise summaries of page content.

### Basic Summarization

```python
from pulsar_sdk import PulsarClient, AgenticSession

client = PulsarClient()
client.create_session()
session = AgenticSession(client)

# Navigate to article
session.open("https://blog.example.com/long-article")

# Summarize the entire page
summary = session.summarize()
print(f"Summary: {summary}")

session.close()
client.close()
```

### Guided Summarization

```python
# Summarize with specific focus
summary = session.summarize(
    instruction="Summarize the main technical concepts discussed"
)
print(f"Technical summary: {summary}")

# Summarize key points
summary = session.summarize(
    instruction="List the 3 most important takeaways"
)
print(f"Key points: {summary}")

# Summarize for specific audience
summary = session.summarize(
    instruction="Explain this in simple terms for beginners"
)
print(f"Beginner summary: {summary}")
```

### Scoped Summarization

```python
# Summarize specific section
session.open("https://docs.example.com/api")

summary = session.summarize(
    instruction="Summarize the authentication section",
    selector=".authentication-docs"
)
print(f"Auth summary: {summary}")

# Summarize comments
summary = session.summarize(
    instruction="What are users saying in the comments?",
    selector=".comments-section"
)
print(f"User feedback: {summary}")
```

### Practical Summarization Examples

```python
def summarize_news_articles(session, urls):
    """Summarize multiple news articles."""
    summaries = []
    
    for url in urls:
        session.open(url)
        
        summary = session.summarize(
            instruction="Provide a 2-sentence summary of the main news"
        )
        
        summaries.append({
            "url": url,
            "summary": summary
        })
        
        print(f"✓ Summarized: {url}")
    
    return summaries

# Use it
news_urls = [
    "https://news.example.com/article1",
    "https://news.example.com/article2",
    "https://news.example.com/article3"
]

summaries = summarize_news_articles(session, news_urls)
for item in summaries:
    print(f"\n{item['url']}")
    print(f"  {item['summary']}")
```

```python
def extract_and_summarize(session, url):
    """Extract data and provide summary."""
    session.open(url)
    
    # Extract structured data
    extraction = session.agent_extract(
        instruction="Extract all product names and prices"
    )
    
    # Summarize the offerings
    summary = session.summarize(
        instruction="What are the main product categories and price ranges?"
    )
    
    return {
        "products": extraction.data if extraction.success else [],
        "summary": summary
    }

# Use it
result = extract_and_summarize(session, "https://shop.example.com")
print(f"Summary: {result['summary']}")
print(f"Products extracted: {len(result['products'])}")
```

---

## Managing Agent History

The agent maintains a history of actions and observations. Managing this history is important for context and performance.

### Viewing History

```python
from pulsar_sdk import PulsarClient, AgenticSession

client = PulsarClient()
client.create_session()
session = AgenticSession(client)

# Perform some actions
session.open("https://example.com")
session.act("click the about link")
session.act("scroll to bottom")

# View the trace
print("Process trace:")
for entry in session.process_trace:
    print(f"  - {entry}")

# Also available as processTrace (Kotlin-style)
print(f"Total entries: {len(session.processTrace)}")

session.close()
client.close()
```

### Clearing History

```python
# Perform first task
session.open("https://example.com")
result1 = session.run("complete task 1")

print(f"History size: {len(session.process_trace)}")

# Clear history before next task
session.clear_history()
print(f"History after clear: {len(session.process_trace)}")

# Perform second task with clean slate
result2 = session.run("complete task 2")
print(f"New history size: {len(session.process_trace)}")
```

### History Management Pattern

```python
def run_independent_tasks(session, tasks):
    """Run multiple independent tasks with clean history."""
    results = []
    
    for i, task in enumerate(tasks, 1):
        # Clear history for fresh context
        if i > 1:
            session.clear_history()
        
        print(f"\nTask {i}: {task}")
        result = session.run(task)
        
        results.append({
            "task": task,
            "success": result.success,
            "trace": list(session.process_trace)
        })
    
    return results

# Use it
tasks = [
    "Find the pricing page and note the basic plan cost",
    "Navigate to documentation and find the API section",
    "Go to blog and find the latest post title"
]

results = run_independent_tasks(session, tasks)

for result in results:
    print(f"\nTask: {result['task']}")
    print(f"Success: {result['success']}")
    print(f"Steps: {len(result['trace'])}")
```

---

## When to Use AI vs WebDriver

Choosing between AI-powered methods and traditional WebDriver depends on your use case.

### Use AI (AgenticSession) When:

✅ **HTML structure changes frequently**
```python
# AI adapts to changes
result = session.act("click the sign up button")
# Works even if button class/ID changes
```

✅ **Complex multi-step workflows**
```python
# AI handles the logic
result = session.run("""
    Search for 'laptops',
    filter by price under $1000,
    sort by rating,
    and click the first result
""")
```

✅ **Ambiguous or fuzzy requirements**
```python
# AI interprets intent
result = session.act("find and click the contact option")
# Could be button, link, or form
```

✅ **Exploratory tasks**
```python
# AI explores and decides
observation = session.observe("what can I do on this page?")
```

### Use WebDriver When:

✅ **Fixed, known page structure**
```python
# Faster and more reliable
driver = session.driver
title = driver.select_first_text_or_null("h1.page-title")
```

✅ **High-volume, repetitive tasks**
```python
# Lower latency
for url in urls:
    driver.navigate_to(url)
    price = driver.select_first_text_or_null(".price")
```

✅ **Precise control needed**
```python
# Exact element targeting
driver.click("#checkout-button")
driver.fill("input[name='email']", "test@example.com")
```

✅ **No AI/LLM access**
```python
# Works without LLM backend
driver.click("button.submit")
```

### Hybrid Approach (Best of Both)

```python
def hybrid_scraping(session, url):
    """Combine AI and WebDriver for optimal results."""
    
    # Use AI for navigation
    session.open(url)
    result = session.run("navigate to the products page and find laptops")
    
    if result.success:
        # Use WebDriver for extraction (faster, more reliable)
        driver = session.driver
        
        titles = driver.select_text_all(".product-title")
        prices = driver.select_text_all(".product-price")
        ratings = driver.select_text_all(".product-rating")
        
        products = []
        for title, price, rating in zip(titles, prices, ratings):
            products.append({
                "title": title,
                "price": price,
                "rating": rating
            })
        
        return products
    
    return []

# Best of both worlds
products = hybrid_scraping(session, "https://shop.example.com")
```

---

## Best Practices for AI Instructions

Writing effective instructions improves AI accuracy and reliability.

### Be Specific and Clear

❌ **Vague:**
```python
session.act("click something")
```

✅ **Specific:**
```python
session.act("click the blue 'Sign Up' button in the header")
```

### Use Action Verbs

✅ **Good action verbs:**
```python
session.act("click the login button")
session.act("fill the email field with 'user@example.com'")
session.act("select 'Premium' from the plan dropdown")
session.act("scroll to the testimonials section")
session.act("hover over the profile menu")
```

### Break Down Complex Tasks

❌ **Too complex:**
```python
result = session.run("find products, filter, sort, compare prices, and buy the cheapest")
```

✅ **Broken down:**
```python
result = session.run("""
    1. Navigate to products page
    2. Apply filter for 'laptops'
    3. Sort by price ascending
    4. Note the price of the first item
""")
```

### Provide Context

❌ **No context:**
```python
session.act("click the button")
```

✅ **With context:**
```python
session.act("in the navigation menu, click the 'Products' button")
```

### Use Descriptive Extraction Instructions

❌ **Vague:**
```python
extraction = session.agent_extract("get the data")
```

✅ **Descriptive:**
```python
extraction = session.agent_extract(
    "Extract the product name, price, description, and availability status"
)
```

### Include Expected Format

✅ **With format guidance:**
```python
summary = session.summarize(
    "Summarize this article in 3 bullet points highlighting the key findings"
)
```

---

## Debugging Agent Tasks

Debugging AI-powered automation requires different techniques than traditional code.

### Check Action Results

```python
def debug_action(session, action):
    """Debug a single action."""
    print(f"\n🔍 Debugging: {action}")
    
    result = session.act(action)
    
    print(f"  Success: {result.success}")
    print(f"  Message: {result.message}")
    print(f"  Action taken: {result.action}")
    print(f"  Complete: {result.is_complete}")
    
    if result.tool_calls:
        print(f"  Tool calls: {len(result.tool_calls)}")
        for call in result.tool_calls:
            print(f"    - {call}")
    
    return result

# Use it
session.open("https://example.com")
debug_action(session, "click the search button")
```

### Examine Process Trace

```python
def run_with_trace(session, task):
    """Run task and show detailed trace."""
    print(f"Task: {task}\n")
    
    result = session.run(task)
    
    print(f"\nResult: {'✓ Success' if result.success else '✗ Failed'}")
    print(f"Message: {result.message}")
    
    print(f"\nProcess Trace ({len(session.process_trace)} entries):")
    for i, entry in enumerate(session.process_trace, 1):
        print(f"  {i}. {entry}")
    
    return result

# Use it
session.open("https://example.com")
run_with_trace(session, "find and click the pricing link")
```

### Observe Before Acting

```python
def observe_then_act(session, goal):
    """Observe page before taking action."""
    print(f"Goal: {goal}\n")
    
    # First, observe
    print("Observing page...")
    observation = session.observe(f"How can I {goal}?")
    print(f"Analysis: {observation.description}\n")
    
    # Then act based on observation
    print("Taking action...")
    result = session.act(goal)
    print(f"Result: {result.message}")
    
    return result

# Use it
session.open("https://example.com")
observe_then_act(session, "contact support")
```

### Step-by-Step Execution

```python
def step_by_step_task(session, steps):
    """Execute task step by step with feedback."""
    for i, step in enumerate(steps, 1):
        print(f"\n📍 Step {i}: {step}")
        
        result = session.act(step)
        
        if result.success:
            print(f"  ✓ Success: {result.message}")
        else:
            print(f"  ✗ Failed: {result.message}")
            print(f"  Stopping at step {i}")
            return False
        
        # Small delay between steps
        import time
        time.sleep(1)
    
    print(f"\n✓ All {len(steps)} steps completed")
    return True

# Use it
session.open("https://example.com/signup")

steps = [
    "fill the username field with 'testuser'",
    "fill the email field with 'test@example.com'",
    "fill the password field with 'securepass123'",
    "check the terms and conditions checkbox",
    "click the sign up button"
]

success = step_by_step_task(session, steps)
```

### Compare AI vs WebDriver

```python
def compare_methods(session, url):
    """Compare AI and WebDriver approaches."""
    session.open(url)
    
    print("Method 1: AI")
    import time
    start = time.time()
    result_ai = session.act("get the page title")
    time_ai = time.time() - start
    print(f"  Result: {result_ai.message}")
    print(f"  Time: {time_ai:.2f}s")
    
    print("\nMethod 2: WebDriver")
    start = time.time()
    driver = session.driver
    title = driver.select_first_text_or_null("h1")
    time_wd = time.time() - start
    print(f"  Result: {title}")
    print(f"  Time: {time_wd:.2f}s")
    
    print(f"\nSpeed difference: {time_ai/time_wd:.1f}x")

# Use it
compare_methods(session, "https://example.com")
```

---

## Complete Examples

### Example 1: AI-Powered Form Submission

```python
from pulsar_sdk import PulsarClient, AgenticSession

def submit_contact_form(name, email, message):
    """Submit a contact form using AI."""
    client = PulsarClient()
    client.create_session()
    session = AgenticSession(client)
    
    try:
        # Navigate to contact page
        session.open("https://example.com")
        
        result = session.run(f"""
            1. Navigate to the contact page
            2. Fill in name field with '{name}'
            3. Fill in email field with '{email}'
            4. Fill in message field with '{message}'
            5. Click the submit button
        """)
        
        if result.success:
            print("✓ Form submitted successfully")
            return True
        else:
            print(f"✗ Form submission failed: {result.message}")
            return False
            
    finally:
        session.close()
        client.close()

# Use it
submit_contact_form(
    name="John Doe",
    email="john@example.com",
    message="I'm interested in your services"
)
```

### Example 2: Intelligent Data Gathering

```python
def gather_product_info(session, product_name):
    """Intelligently gather product information."""
    # Search for product
    session.open("https://shop.example.com")
    
    search_result = session.run(f"""
        Search for '{product_name}' and click the first result
    """)
    
    if not search_result.success:
        return None
    
    # Extract product details using AI
    extraction = session.agent_extract(
        instruction="Extract product name, price, rating, description, and availability",
        schema={
            "type": "object",
            "properties": {
                "name": {"type": "string"},
                "price": {"type": "number"},
                "rating": {"type": "number"},
                "description": {"type": "string"},
                "available": {"type": "boolean"}
            }
        }
    )
    
    if extraction.success:
        return extraction.data
    
    return None

# Use it
product = gather_product_info(session, "wireless mouse")
if product:
    print(f"Found: {product['name']}")
    print(f"Price: ${product['price']}")
    print(f"Rating: {product['rating']}/5")
```

---

## Next Steps

- **[Data Extraction Guide](data-extraction.md)** - Advanced extraction techniques
- **[Advanced Guide](advanced.md)** - Complex workflows and production deployment
- **[Basic Usage Guide](basic-usage.md)** - Traditional WebDriver patterns

## Summary

Key takeaways:

1. **act()**: Single actions in natural language
2. **run()**: Multi-step autonomous tasks
3. **observe()**: Analyze pages and get suggestions
4. **agent_extract()**: AI-powered structured extraction
5. **summarize()**: Generate content summaries
6. **History**: Manage context with clear_history()
7. **Hybrid**: Combine AI and WebDriver for best results
8. **Instructions**: Be specific, clear, and action-oriented

AI-powered automation excels at adaptability and intelligence—use it wisely! 🤖
