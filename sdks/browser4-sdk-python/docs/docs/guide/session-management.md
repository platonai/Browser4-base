# Session Management

Sessions are the foundation of Browser4 automation. This guide covers everything you need to know about creating, managing, and using sessions effectively.

## Session Types

Browser4 provides two session types:

### PulsarSession
Basic session for page loading and data extraction.

**Use when:**
- You need basic web scraping
- CSS selector extraction is sufficient
- You don't need AI capabilities

```python
from browser4 import PulsarClient, PulsarSession

client = PulsarClient(base_url="http://localhost:8182")
session_id = client.create_session()
session = PulsarSession(client)

page = session.load("https://example.com")
document = session.parse(page)
data = session.extract(document, {"title": "h1"})

session.close()
```

### AgenticSession
AI-powered session extending PulsarSession.

**Use when:**
- You need AI-powered actions
- Complex automation workflows
- Natural language commands
- All PulsarSession features plus AI

```python
from browser4 import PulsarClient, AgenticSession

client = PulsarClient(base_url="http://localhost:8182")
session_id = client.create_session()
session = AgenticSession(client)

# PulsarSession methods available
page = session.load("https://example.com")

# Plus AI capabilities
session.act("click the login button")
session.run("complete the signup form")

session.close()
```

## Creating Sessions

### With Browser4Driver (Recommended)

Browser4Driver handles server lifecycle automatically:

```python
from browser4 import Browser4Driver, PulsarClient, AgenticSession

with Browser4Driver() as driver:
    client = PulsarClient(base_url=driver.base_url)
    session_id = client.create_session()
    session = AgenticSession(client)
    
    # Use session
    page = session.open("https://example.com")
    
    session.close()
    client.close()
```

### With Existing Server

Connect to a running Browser4 server:

```python
from browser4 import PulsarClient, AgenticSession

# Connect to existing server
client = PulsarClient(base_url="http://localhost:8182")
session_id = client.create_session()
session = AgenticSession(client)

# Use session
page = session.open("https://example.com")

session.close()
client.close()
```

### With Custom Capabilities

Specify browser capabilities when creating sessions:

```python
session_id = client.create_session(capabilities={
    "browserName": "chrome",
    "platform": "ANY",
    "enableAI": True
})
```

## Session Properties

Access session information through properties:

```python
session = AgenticSession(client)

# Session identifiers
print(f"ID: {session.id}")           # Session ID
print(f"UUID: {session.uuid}")       # UUID format
print(f"Display: {session.display}") # Human-readable

# State
print(f"Active: {session.is_active}")  # Is session active?

# WebDriver access
driver = session.driver               # Get WebDriver
bound_driver = session.bound_driver   # Get bound driver
```

## Session Lifecycle

### Creation
```python
client = PulsarClient(base_url=driver.base_url)
session_id = client.create_session()
session = AgenticSession(client)
```

### Usage
```python
# Load pages
page = session.open("https://example.com")

# Extract data
document = session.parse(page)
data = session.extract(document, {"title": "h1"})

# AI actions
session.act("click button")
```

### Cleanup
```python
# Close session
session.close()

# Close client (if done with all sessions)
client.close()
```

### Full Lifecycle Example

```python
from browser4 import Browser4Driver, PulsarClient, AgenticSession

try:
    # Start server
    with Browser4Driver() as driver:
        # Create client
        client = PulsarClient(base_url=driver.base_url)
        
        try:
            # Create session
            session_id = client.create_session()
            session = AgenticSession(client)
            
            try:
                # Use session
                page = session.open("https://example.com")
                result = session.act("scroll down")
                
            finally:
                # Always close session
                session.close()
                
        finally:
            # Close client
            client.close()
            
except Exception as e:
    print(f"Error: {e}")
```

## Multiple Sessions

You can create multiple sessions from one client:

```python
client = PulsarClient(base_url=driver.base_url)

# Session 1
session1_id = client.create_session()
session1 = AgenticSession(client)
page1 = session1.open("https://example.com")
session1.close()

# Session 2
session2_id = client.create_session()
session2 = AgenticSession(client)
page2 = session2.open("https://another-site.com")
session2.close()

client.close()
```

## Session State Management

### Clearing History

Clear agent history for new tasks:

```python
session = AgenticSession(client)

# First task
session.run("search for python")
print(f"History: {len(session.process_trace)}")

# Clear for new task
session.clear_history()

# Second task (fresh context)
session.run("search for javascript")
```

### Process Trace

Track operations performed in the session:

```python
session = AgenticSession(client)

# Perform operations
session.open("https://example.com")
session.act("click button")
session.run("complete form")

# View trace
for trace in session.process_trace:
    print(f"🚩 {trace}")
```

## Session Configuration

### Client Timeouts

Set request timeouts:

```python
client = PulsarClient(
    base_url="http://localhost:8182",
    timeout=60.0  # 60 seconds
)
```

### Custom Headers

Add authentication or custom headers:

```python
client = PulsarClient(
    base_url="http://localhost:8182",
    default_headers={
        "Authorization": "Bearer your-token",
        "X-Custom-Header": "value"
    }
)
```

### Server Configuration

Configure Browser4Driver:

```python
driver = Browser4Driver(
    port=8183,  # Custom port
    jar_path="/custom/path/Browser4.jar",  # Custom jar location
    java_options={
        "spring.profiles.active": "rest,private",
        "server.address": "0.0.0.0"
    }
)
```

## Session Patterns

### Single Session Pattern

Simple scripts with one session:

```python
with Browser4Driver() as driver:
    client = PulsarClient(base_url=driver.base_url)
    session_id = client.create_session()
    session = AgenticSession(client)
    
    # Do work
    page = session.open("https://example.com")
    
    session.close()
    client.close()
```

### Session Pool Pattern

Reuse client for multiple sessions:

```python
def process_url(client, url):
    session_id = client.create_session()
    session = AgenticSession(client)
    try:
        page = session.open(url)
        # Process page
        return page
    finally:
        session.close()

with Browser4Driver() as driver:
    client = PulsarClient(base_url=driver.base_url)
    
    urls = ["https://example.com", "https://another.com"]
    for url in urls:
        result = process_url(client, url)
    
    client.close()
```

### Long-Running Session Pattern

Keep session alive for interactive work:

```python
driver = Browser4Driver()
driver.start()

client = PulsarClient(base_url=driver.base_url)
session_id = client.create_session()
session = AgenticSession(client)

# Long-running operations
while True:
    command = input("Command: ")
    if command == "quit":
        break
    
    result = session.run(command)
    print(result.message)

# Cleanup
session.close()
client.close()
driver.stop()
```

## Troubleshooting

### Session Creation Fails

Check server is running:
```python
import requests
try:
    response = requests.get("http://localhost:8182/status")
    print(f"Server status: {response.status_code}")
except:
    print("Server not reachable")
```

### Session Times Out

Increase timeout:
```python
client = PulsarClient(
    base_url="http://localhost:8182",
    timeout=120.0  # 2 minutes
)
```

### Memory Issues

Close sessions properly:
```python
# Always close when done
try:
    session = AgenticSession(client)
    # ... use session
finally:
    session.close()
```

### Session ID Conflicts

Each create_session() creates a new session:
```python
# Wrong - creates new session but uses old reference
session_id1 = client.create_session()
session_id2 = client.create_session()
session = AgenticSession(client)  # Uses latest session

# Right - create client/session pairs
session1_id = client.create_session()
session1 = AgenticSession(client)
# Use session1, then close

session2_id = client.create_session()
session2 = AgenticSession(client)
# Use session2, then close
```

## Best Practices

1. **Use context managers** for automatic cleanup
2. **Close sessions** when done to free resources
3. **Reuse clients** when creating multiple sessions
4. **Set appropriate timeouts** for your use case
5. **Clear history** between unrelated tasks
6. **Handle errors** gracefully with try/finally
7. **Monitor session state** with is_active property

## Next Steps

- **[Navigation](navigation.md)** - Navigate and control pages
- **[Element Interaction](element-interaction.md)** - Interact with page elements
- **[Data Extraction](data-extraction.md)** - Extract data from pages
- **[AI Automation](ai-automation.md)** - Use AI-powered features
