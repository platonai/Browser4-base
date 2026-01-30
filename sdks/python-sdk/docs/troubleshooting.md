# Troubleshooting Guide

This guide helps you diagnose and fix common issues with the Browser4 Python SDK.

## Table of Contents

- [Installation Issues](#installation-issues)
- [Connection Problems](#connection-problems)
- [Session Management Issues](#session-management-issues)
- [Browser Automation Problems](#browser-automation-problems)
- [Data Extraction Issues](#data-extraction-issues)
- [Agent/AI Issues](#agentai-issues)
- [Performance Problems](#performance-problems)
- [Error Messages](#error-messages)

---

## Installation Issues

### ModuleNotFoundError: No module named 'pulsar_sdk'

**Problem:** Python cannot find the installed SDK.

**Solutions:**

```bash
# 1. Verify installation
pip list | grep pulsar

# 2. Reinstall in editable mode
cd Browser4/sdks/python-sdk
pip install -e .[dev]

# 3. Check Python path
python -c "import pulsar_sdk; print(pulsar_sdk.__file__)"

# 4. Use virtual environment
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
pip install -e .[dev]
```

### Dependency Conflicts

**Problem:** Conflicting package versions.

**Solutions:**

```bash
# Create clean environment
python -m venv fresh_env
source fresh_env/bin/activate
pip install -e .[dev]

# Or specify versions explicitly
pip install requests>=2.31.0 beautifulsoup4>=4.12.0 pytest>=7.4.0
```

### ImportError on Windows

**Problem:** Import fails on Windows due to path issues.

**Solutions:**

```bash
# Use PowerShell or cmd with proper paths
cd C:\path\to\Browser4\sdks\python-sdk
pip install -e .[dev]

# Or add to PYTHONPATH
set PYTHONPATH=C:\path\to\Browser4\sdks\python-sdk;%PYTHONPATH%
```

---

## Connection Problems

### ConnectionError: Cannot connect to server

**Problem:** Cannot reach the Browser4 server.

**Diagnosis:**

```python
import requests

# Test server connectivity
try:
    response = requests.get("http://localhost:8182/api/v1/status", timeout=5)
    print(f"Server status: {response.status_code}")
    print(f"Response: {response.json()}")
except requests.exceptions.ConnectionError:
    print("❌ Cannot connect to server")
except requests.exceptions.Timeout:
    print("❌ Server not responding (timeout)")
```

**Solutions:**

1. **Verify server is running:**
   ```bash
   # Check if process is running
   ps aux | grep java
   
   # Or on Windows
   tasklist | findstr java
   ```

2. **Start the server:**
   ```bash
   # Using Docker
   docker run -p 8182:8182 platonai/browser4:latest
   
   # Or from source
   java -jar pulsar-rest/target/pulsar-rest-*.jar
   ```

3. **Check firewall:**
   ```bash
   # Test port is open
   telnet localhost 8182
   
   # Or use curl
   curl http://localhost:8182/api/v1/status
   ```

4. **Use correct URL:**
   ```python
   # If server is on different host/port
   client = PulsarClient(base_url="http://192.168.1.100:8182")
   ```

### Connection Timeout

**Problem:** Requests time out.

**Solutions:**

```python
# Increase timeout
client = PulsarClient(
    base_url="http://localhost:8182",
    timeout=60.0  # 60 seconds instead of default 30
)

# Or set environment variable
import os
os.environ['BROWSER4_TIMEOUT'] = '120'
```

### SSL/HTTPS Issues

**Problem:** SSL certificate errors when using HTTPS.

**Solutions:**

```python
# For development, you can disable SSL verification (NOT for production!)
import requests
from pulsar_sdk import PulsarClient

# Note: This is a workaround; proper solution is to fix certificates
client = PulsarClient(base_url="https://your-server:8182")
# Configure requests session if needed
```

---

## Session Management Issues

### ValueError: session_id is required

**Problem:** Trying to use SDK without creating a session.

**Solution:**

```python
from pulsar_sdk import PulsarClient, AgenticSession

client = PulsarClient()
# ❌ Missing this line:
client.create_session()
# ✅ Create session before using SDK

session = AgenticSession(client)
```

### Session Lost/Expired

**Problem:** Session no longer valid.

**Solutions:**

```python
# 1. Check session status
try:
    status = client.get_session_status()
    print(f"Session valid: {status.get('ready')}")
except Exception as e:
    print(f"Session invalid: {e}")
    # Recreate session
    client.create_session()

# 2. Implement session recovery
def ensure_session(client):
    """Ensure session is valid, create if needed."""
    try:
        client.get_session_status()
    except:
        print("Recreating session...")
        client.create_session()

# Use before operations
ensure_session(client)
page = session.open("https://example.com")
```

### Multiple Sessions Conflict

**Problem:** Multiple sessions interfering with each other.

**Solutions:**

```python
# Create separate client instances
client1 = PulsarClient()
client1.create_session()
session1 = AgenticSession(client1)

client2 = PulsarClient()
client2.create_session()
session2 = AgenticSession(client2)

# Or use explicit session IDs
sid1 = client1.create_session()
sid2 = client2.create_session()

# Pass session_id to operations
client1.open_url("https://site1.com", session_id=sid1)
client2.open_url("https://site2.com", session_id=sid2)
```

---

## Browser Automation Problems

### Element Not Found

**Problem:** Cannot find element with selector.

**Diagnosis:**

```python
# Check if element exists
exists = driver.exists("button.submit")
print(f"Element exists: {exists}")

# Check if visible
visible = driver.is_visible("button.submit")
print(f"Element visible: {visible}")

# Wait for element
found = driver.wait_for_selector("button.submit", timeout=10000)
print(f"Element appeared: {found}")
```

**Solutions:**

```python
# 1. Wait for element to appear
driver.wait_for_selector(".dynamic-content", timeout=5000)
driver.click(".dynamic-content button")

# 2. Try different selectors
selectors = [
    "button#submit",
    "button.submit-button",
    "input[type='submit']",
    "button[type='submit']"
]

for selector in selectors:
    if driver.exists(selector):
        driver.click(selector)
        break

# 3. Use more specific selector
# ❌ Too generic
driver.click("button")

# ✅ More specific
driver.click("form#login button[type='submit']")

# 4. Check if element is in iframe
# (Note: iframe support may vary, check documentation)
```

### Click Not Working

**Problem:** Click action doesn't trigger expected behavior.

**Solutions:**

```python
# 1. Wait for element to be clickable
driver.wait_for_selector("button.submit", timeout=5000)
time.sleep(0.5)  # Brief pause
driver.click("button.submit")

# 2. Scroll element into view first
driver.scroll_to("button.submit")
driver.click("button.submit")

# 3. Try hover before click
driver.hover("button.submit")
time.sleep(0.2)
driver.click("button.submit")

# 4. Use JavaScript click as fallback
driver.execute_script(
    "document.querySelector('button.submit').click()"
)

# 5. Check if element is covered/disabled
visible = driver.is_visible("button.submit")
if not visible:
    print("Element not visible, cannot click")
```

### Form Submission Issues

**Problem:** Form doesn't submit or submits incorrectly.

**Solutions:**

```python
# 1. Fill fields and press Enter
driver.fill("input[name='search']", "query")
driver.press("input[name='search']", "Enter")

# 2. Fill fields and click submit
driver.fill("input[name='email']", "user@example.com")
driver.fill("input[name='password']", "secret")
driver.click("button[type='submit']")

# 3. Wait for form to process
driver.click("button[type='submit']")
driver.wait_for_selector(".success-message", timeout=5000)

# 4. Use type() instead of fill() for special cases
driver.type("input[name='code']", "123456")  # One character at a time
```

### Navigation Not Working

**Problem:** Browser doesn't navigate or navigation fails.

**Solutions:**

```python
# 1. Check URL format
# ❌ Missing protocol
driver.navigate_to("example.com")

# ✅ Full URL
driver.navigate_to("https://example.com")

# 2. Wait after navigation
driver.navigate_to("https://example.com")
time.sleep(2)  # Wait for page load

# 3. Check navigation result
try:
    driver.navigate_to("https://example.com")
    url = driver.current_url()
    print(f"Current URL: {url}")
except Exception as e:
    print(f"Navigation failed: {e}")

# 4. Use page load timeout
# Configure if possible, or implement retry logic
```

---

## Data Extraction Issues

### Empty or None Results

**Problem:** Extraction returns empty or None values.

**Diagnosis:**

```python
# Check page content
html = driver.page_source()
print(f"Page HTML length: {len(html)}")
print(html[:500])  # First 500 chars

# Check if page loaded
url = driver.current_url()
title = driver.title()
print(f"URL: {url}")
print(f"Title: {title}")

# Test selector directly
text = driver.select_first_text_or_null("h1")
print(f"H1 text: {text}")
```

**Solutions:**

```python
# 1. Wait for content to load
driver.navigate_to("https://example.com")
driver.wait_for_selector("h1", timeout=5000)
title = driver.select_first_text_or_null("h1")

# 2. Check selector syntax
# ❌ Invalid selector
data = driver.select_first_text_or_null("h1.title")  # No space after h1

# ✅ Correct selector
data = driver.select_first_text_or_null("h1 .title")  # Space indicates descendant

# 3. Use broader selector
# ❌ Too specific
data = driver.select_first_text_or_null("div.container > article.post > h1.title")

# ✅ Simpler
data = driver.select_first_text_or_null("h1.title")

# 4. Handle None results
text = driver.select_first_text_or_null("h1")
if text:
    print(f"Title: {text}")
else:
    print("No title found")
```

### Incorrect Data Extracted

**Problem:** Wrong data is extracted.

**Solutions:**

```python
# 1. Be more specific with selectors
# ❌ Selects all paragraphs
text = driver.select_first_text_or_null("p")

# ✅ Selects specific paragraph
text = driver.select_first_text_or_null("article.main-content p.intro")

# 2. Extract attribute instead of text
# ❌ Gets text content
link = driver.select_first_text_or_null("a")

# ✅ Gets href attribute
link = driver.select_first_attribute_or_null("a", "href")

# 3. Use extract() for multiple fields
fields = driver.extract({
    "title": "h1.title",  # First match
    "author": ".author-name",
    "date": ".publish-date"
})

# 4. Get all matches with select_text_all
all_prices = driver.select_text_all(".product .price")
print(f"Found {len(all_prices)} prices")
```

### Special Characters Corrupted

**Problem:** Special characters (unicode, emojis) are corrupted.

**Solutions:**

```python
# Ensure proper encoding
text = driver.select_first_text_or_null("h1")

# Save with UTF-8 encoding
with open("output.txt", "w", encoding="utf-8") as f:
    f.write(text)

# For JSON
import json
data = {"title": text}
with open("output.json", "w", encoding="utf-8") as f:
    json.dump(data, f, ensure_ascii=False, indent=2)
```

---

## Agent/AI Issues

### Agent Actions Not Working

**Problem:** AI agent doesn't perform expected actions.

**Diagnosis:**

```python
# Check agent result
result = session.act("click the login button")
print(f"Success: {result.success}")
print(f"Message: {result.message}")
print(f"Action taken: {result.action}")
print(f"Reasoning: {result.reasoning}")
```

**Solutions:**

```python
# 1. Be more specific in instructions
# ❌ Too vague
result = session.act("click button")

# ✅ More specific
result = session.act("click the blue 'Login' button in the top right")

# 2. Check page state first
observations = session.observe("what buttons are visible?")
for obs in observations.observations:
    print(f"- {obs.description}")

# Then act based on observations
result = session.act("click the login button")

# 3. Break down complex tasks
# ❌ Too complex for single action
result = session.act("login with email and password then navigate to profile")

# ✅ Use run() for multi-step
history = session.run("""
1. Fill email field with 'user@example.com'
2. Fill password field
3. Click login button
4. Wait for page to load
5. Click profile link
""")

# 4. Clear history if context is wrong
session.clear_history()
result = session.act("click search button")  # Fresh context
```

### Agent Gets Stuck or Loops

**Problem:** Agent repeats same action or doesn't complete task.

**Solutions:**

```python
# 1. Set max history size (if supported)
# Check if there's a way to limit iterations

# 2. Use observe() to check state
observations = session.observe("is the task complete?")
for obs in observations.observations:
    if "complete" in obs.description.lower():
        break

# 3. Use act() with clear success criteria
result = session.act("click 'Next' button")
if result.success:
    print("Clicked successfully")
else:
    print(f"Failed: {result.message}")

# 4. Clear history and retry
session.clear_history()
history = session.run("complete the form")

# 5. Use traditional WebDriver for problematic steps
# Use agent for discovery, WebDriver for execution
observations = session.observe("find the search button selector")
# Then use driver.click() directly
driver.click("button.search")
```

### Agent Extraction Inaccurate

**Problem:** agent_extract() returns incorrect or incomplete data.

**Solutions:**

```python
# 1. Provide clear schema
schema = {
    "type": "object",
    "properties": {
        "title": {"type": "string", "description": "Product title"},
        "price": {"type": "number", "description": "Price in dollars"},
        "inStock": {"type": "boolean", "description": "Is product in stock"}
    },
    "required": ["title", "price"]
}

result = session.agent_extract(
    instruction="Extract product information",
    schema=schema
)

# 2. Use clear instructions
result = session.agent_extract(
    instruction="""
    Extract product details from the main product area:
    - title: The product name/title
    - price: The current selling price (number only, no $ symbol)
    - rating: The average rating (number out of 5)
    """,
    schema=schema
)

# 3. Fall back to CSS selectors
try:
    data = session.agent_extract(instruction="Extract product info")
except Exception:
    # Fallback to traditional extraction
    data = session.extract(page, {
        "title": "h1.product-title",
        "price": ".price"
    })
```

---

## Performance Problems

### Slow Page Loading

**Problem:** Pages take too long to load.

**Solutions:**

```python
# 1. Use cache effectively
# First load (slow)
page = session.load("https://example.com", args="-expire 1h")

# Subsequent loads (fast, from cache)
page = session.load("https://example.com", args="-expire 1h")

# 2. Submit URLs for background processing
for url in urls:
    session.submit(url, args="-expire 1d")
# Do other work...
# Later, load from cache
for url in urls:
    page = session.load(url)  # Fast from cache

# 3. Increase timeout for slow sites
client = PulsarClient(timeout=120.0)

# 4. Check if site is actually slow
import time
start = time.time()
page = session.open("https://example.com")
elapsed = time.time() - start
print(f"Load time: {elapsed:.2f}s")
```

### High Memory Usage

**Problem:** Script uses too much memory.

**Solutions:**

```python
# 1. Close sessions when done
try:
    session = AgenticSession(client)
    # ... do work ...
finally:
    session.close()

# 2. Process in batches
urls = [...]  # Large list
batch_size = 10

for i in range(0, len(urls), batch_size):
    batch = urls[i:i+batch_size]
    
    for url in batch:
        page = session.load(url)
        data = session.extract(page, fields)
        # Process immediately
        save_data(data)
    
    # Brief pause between batches
    time.sleep(1)

# 3. Don't store large HTML in memory
# ❌ Stores all HTML
pages = []
for url in urls:
    page = session.open(url)
    pages.append(page)  # Memory builds up

# ✅ Process immediately
for url in urls:
    page = session.open(url)
    data = session.extract(page, fields)
    save_data(data)  # No memory buildup
```

### Rate Limiting/Blocked

**Problem:** Getting rate limited or blocked by target site.

**Solutions:**

```python
import time
import random

# 1. Add delays between requests
for url in urls:
    page = session.open(url)
    # ... extract data ...
    time.sleep(random.uniform(1, 3))  # 1-3 second random delay

# 2. Use caching to reduce requests
# Only fetches once per day
page = session.load(url, args="-expire 1d")

# 3. Rotate user agents (if supported via headers)
client = PulsarClient(default_headers={
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
})

# 4. Implement exponential backoff on errors
def fetch_with_backoff(url, max_retries=3):
    for attempt in range(max_retries):
        try:
            return session.open(url)
        except Exception as e:
            if attempt < max_retries - 1:
                wait = 2 ** attempt  # 1s, 2s, 4s
                print(f"Retry {attempt+1} after {wait}s")
                time.sleep(wait)
            else:
                raise
```

---

## Error Messages

### Common HTTP Errors

#### 400 Bad Request
**Meaning:** Invalid request parameters.

**Fix:** Check URL format and parameters:
```python
# ❌ Invalid URL
page = session.open("not-a-valid-url")

# ✅ Valid URL
page = session.open("https://example.com")
```

#### 404 Not Found
**Meaning:** Endpoint or resource doesn't exist.

**Fix:**
```python
# Check server version and API endpoints
status = client.get_session_status()
print(status)

# Verify server is running correct version
```

#### 500 Internal Server Error
**Meaning:** Server error.

**Fix:**
- Check server logs
- Verify server has enough resources (memory, disk)
- Try simpler operations to isolate issue
- Report bug if reproducible

#### 503 Service Unavailable
**Meaning:** Server is overloaded or down.

**Fix:**
- Wait and retry
- Check server health
- Scale server resources if needed

### Common Python Exceptions

#### TypeError: 'NoneType' object...
**Meaning:** Trying to use None value.

**Fix:**
```python
# ❌ No None check
text = driver.select_first_text_or_null("h1")
print(text.upper())  # Crashes if None

# ✅ With None check
text = driver.select_first_text_or_null("h1")
if text:
    print(text.upper())
else:
    print("No text found")

# ✅ With default
text = driver.select_first_text_or_null("h1") or "Unknown"
print(text.upper())
```

#### KeyError: 'field_name'
**Meaning:** Expected key missing from dict.

**Fix:**
```python
# ❌ Direct access
fields = session.extract(page, {"title": "h1"})
print(fields["title"])  # Crashes if not found

# ✅ Safe access
print(fields.get("title", "No title"))

# ✅ With check
if "title" in fields:
    print(fields["title"])
```

---

## Getting More Help

### Enable Debug Logging

```python
import logging

# Enable debug logging
logging.basicConfig(level=logging.DEBUG)

# Or for specific module
logging.getLogger('pulsar_sdk').setLevel(logging.DEBUG)
```

### Capture Network Traffic

```python
# Monitor requests (if using requests library)
import requests
from requests.adapters import HTTPAdapter

# Enable detailed logging
import http.client
http.client.HTTPConnection.debuglevel = 1
```

### Check Server Logs

The Browser4 server logs often contain helpful error messages:

```bash
# If running from source
tail -f pulsar-rest/logs/application.log

# If using Docker
docker logs -f <container-id>
```

### Report Issues

When reporting issues, include:

1. Python version: `python --version`
2. SDK version: Check pyproject.toml
3. Server version
4. Minimal reproducible example
5. Error message and stack trace
6. Expected vs actual behavior

**Report at:** https://github.com/platonai/Browser4/issues

---

## Quick Checklist

When troubleshooting, check these first:

- [ ] Is Browser4 server running?
- [ ] Can you connect to server URL?
- [ ] Did you create a session?
- [ ] Are selectors correct?
- [ ] Did you wait for elements to load?
- [ ] Are there any errors in console/logs?
- [ ] Is the page actually loaded?
- [ ] Did you handle None/empty results?
- [ ] Are you closing sessions properly?
- [ ] Is network/firewall blocking requests?

---

Need more help? Check the [API Reference](api-reference/client.md) or [open an issue](https://github.com/platonai/Browser4/issues).
