# Frequently Asked Questions

Common questions and answers about Browser4 Python SDK.

## General Questions

### What is Browser4?

Browser4 is a lightning-fast, coroutine-safe browser engine designed for AI agents. It provides high-performance browser automation with features like:

- AI-powered browser agents that can reason and act autonomously
- Hybrid data extraction combining LLM, ML, and CSS selectors
- Extreme performance (100k-200k complex page visits per day per machine)
- WebDriver-compatible API for browser control

### What is the Browser4 Python SDK?

The Browser4 Python SDK is a Python client library that provides a Pythonic interface to the Browser4 browser automation platform. It includes:

- `Browser4Driver` - Automatic server management (download & start Browser4.jar)
- `PulsarClient` - Low-level HTTP client for API communication
- `PulsarSession` - Page loading and data extraction
- `AgenticSession` - AI-powered browser automation
- `WebDriver` - Browser control and element interaction

### Do I need Java installed?

Yes, Browser4 runs as a Java application (Browser4.jar). You need Java 17 or higher installed on your system.

Check your Java version:
```bash
java -version
```

Install Java if needed:
- **Ubuntu/Debian**: `sudo apt install openjdk-17-jdk`
- **macOS**: `brew install openjdk@17`
- **Windows**: Download from [Adoptium](https://adoptium.net/)

### Do I need to download Browser4.jar manually?

No! The `Browser4Driver` class automatically downloads Browser4.jar from GitHub releases on first use. The JAR is cached locally at `~/.browser4/lib/Browser4.jar` for subsequent runs.

However, you can download it manually if needed and specify the path:
```python
driver = Browser4Driver(jar_path="/path/to/Browser4.jar")
```

## Installation

### How do I install the SDK?

Install via pip:
```bash
pip install browser4
```

Or using uv (recommended):
```bash
uv pip install browser4
```

### What Python versions are supported?

Browser4 Python SDK requires Python 3.9 or higher. We recommend Python 3.11 for best performance.

### Can I install from source?

Yes, clone the repository and install:
```bash
git clone https://github.com/platonai/Browser4.git
cd Browser4/sdks/browser4-sdk-python
pip install -e .
```

## Server Management

### How do I start the Browser4 server?

The easiest way is using `Browser4Driver`:

```python
from browser4 import Browser4Driver

# Automatic - downloads and starts server
with Browser4Driver() as driver:
    # Use driver.base_url with PulsarClient
    pass
# Server stops automatically
```

### Can I use an existing Browser4 server?

Yes! Connect directly with `PulsarClient`:

```python
from browser4 import PulsarClient, AgenticSession

client = PulsarClient(base_url="http://localhost:8182")
client.create_session()
session = AgenticSession(client)
```

### How do I stop the Browser4 server?

If using `Browser4Driver`:

```python
# With context manager (automatic)
with Browser4Driver() as driver:
    # Use server...
    pass  # Stops automatically

# Or manually
driver = Browser4Driver()
driver.start()
# ... use server ...
driver.stop()
```

### The server won't start. What should I check?

1. **Java installation**: `java -version` should show Java 17+
2. **Port availability**: Default port 8182 might be in use
   ```python
   driver = Browser4Driver(port=8183)  # Try different port
   ```
3. **Firewall**: Ensure localhost connections are allowed
4. **Disk space**: Ensure sufficient space for Browser4.jar (~200MB)
5. **Check logs**: Browser4 outputs logs to stdout/stderr

### How long does the server take to start?

Typically 10-30 seconds on first start, 5-15 seconds on subsequent starts. The SDK waits up to 120 seconds by default.

For slower systems, you can wait manually:
```python
driver = Browser4Driver()
driver.start(wait_for_ready=False)
driver.wait_for_server_ready(timeout_seconds=180)  # 3 minutes
```

## AI Features

### Do I need an API key for AI features?

Yes, AI features (`act()`, `run()`, `observe()`, `agent_extract()`, `summarize()`) require an OpenRouter API key:

```bash
export OPENROUTER_API_KEY="sk-or-v1-your-key"
```

Get a key at [openrouter.ai](https://openrouter.ai/)

### Can I use Browser4 without AI features?

Absolutely! The core functionality (page loading, data extraction with CSS selectors, WebDriver control) works without AI:

```python
from browser4 import Browser4Driver, PulsarClient, PulsarSession

with Browser4Driver() as driver:
    client = PulsarClient(base_url=driver.base_url)
    client.create_session()
    session = PulsarSession(client)  # Use PulsarSession, not AgenticSession
    
    page = session.open("https://example.com")
    document = session.parse(page)
    fields = session.extract(document, {"title": "h1"})
```

### How much do AI features cost?

AI features use OpenRouter which charges based on model usage. Costs vary by model:
- Simple actions: ~$0.001-0.01 per request
- Complex tasks: ~$0.01-0.10 per request
- Extraction: ~$0.005-0.05 per request

Check [OpenRouter pricing](https://openrouter.ai/docs#pricing) for current rates.

### Can I use a different AI provider?

Currently, Browser4's AI features are integrated with OpenRouter. Support for other providers may be added in future releases. You can make a feature request on GitHub.

## Data Extraction

### What's the difference between extract() and agent_extract()?

**`extract()`** - Traditional CSS selector-based extraction:
```python
fields = session.extract(document, {
    "title": "h1",
    "price": ".price"
})
```
- Fast and deterministic
- No AI required
- Works on parsed HTML

**`agent_extract()`** - AI-powered extraction:
```python
extraction = session.agent_extract(
    instruction="Extract product name and price",
    schema={"type": "object"}
)
```
- Uses AI to understand page structure
- More flexible, handles variations
- Requires OpenRouter API key
- Slower and costs per request

### How do I extract data from multiple pages?

Use pagination:
```python
all_data = []
page_num = 1

while page_num <= max_pages:
    # Extract current page
    data = session.scrape(f"https://example.com/page{page_num}", ...)
    all_data.append(data)
    
    # Navigate to next page
    driver.click(".next-page")
    driver.delay(2000)
    page_num += 1
```

See [Advanced Usage - Pagination](examples/advanced-usage.md#pagination-handling) for complete examples.

### Can I extract JavaScript-rendered content?

Yes! Browser4 renders JavaScript. Just wait for content to load:

```python
page = session.open("https://dynamic-site.com")
driver = session.driver

# Wait for content
driver.wait_for_selector(".content", timeout=10000)

# Extract data
data = driver.select_text_all(".item")
```

### How do I handle dynamic content that loads on scroll?

```python
driver = session.driver

# Scroll to trigger loading
driver.scroll_to_bottom()
driver.delay(2000)  # Wait for content

# Extract data
items = driver.select_text_all(".item")
```

## Performance

### How fast is Browser4?

Browser4 is designed for extreme performance:
- 100,000-200,000 complex page visits per machine per day
- Fully coroutine-safe for concurrent operations
- Efficient memory usage with intelligent caching

### Can I scrape multiple pages concurrently?

Yes! Create multiple sessions:

```python
from concurrent.futures import ThreadPoolExecutor

def scrape_url(url):
    client = PulsarClient(base_url="http://localhost:8182")
    client.create_session()
    session = AgenticSession(client)
    page = session.open(url)
    # ... extract data ...
    session.close()
    client.close()
    return data

with ThreadPoolExecutor(max_workers=5) as executor:
    results = executor.map(scrape_url, urls)
```

See [Advanced Usage - Parallel Processing](examples/advanced-usage.md#parallel-processing) for details.

### How can I improve scraping performance?

1. **Use batch submission**: Submit multiple URLs asynchronously
   ```python
   for url in urls:
       session.submit(url, args="-expire 1d")
   ```

2. **Enable caching**: Use expire arguments
   ```python
   page = session.load(url, args="-expire 1d")
   ```

3. **Reuse sessions**: Create session pool
4. **Minimize AI calls**: Use traditional extraction where possible
5. **Parallel processing**: Use multiple workers

### My scraper is slow. How can I debug it?

1. **Profile operations**:
   ```python
   import time
   start = time.time()
   page = session.open(url)
   print(f"Load time: {time.time() - start}s")
   ```

2. **Check network**: Slow pages affect performance
3. **Reduce waits**: Minimize `driver.delay()` calls
4. **Monitor resources**: Check CPU/memory usage
5. **Check logs**: Enable DEBUG logging

## Error Handling

### What errors should I handle?

Common errors:
- `ConnectionError`: Server not available
- `TimeoutError`: Request timeout
- `ValueError`: Invalid parameters
- `RuntimeError`: Server startup failed

Always use try-finally for cleanup:
```python
driver = Browser4Driver()
try:
    driver.start()
    # ... use server ...
finally:
    driver.stop()
```

### How do I handle "connection refused" errors?

Check:
1. Server is running: `driver.is_running`
2. Server is healthy: `driver.is_server_healthy()`
3. Correct URL: `driver.base_url`
4. Firewall allows connections
5. Port is correct

### How do I retry failed operations?

Implement retry logic:
```python
def retry_operation(func, max_retries=3):
    for attempt in range(max_retries):
        try:
            return func()
        except Exception as e:
            if attempt == max_retries - 1:
                raise
            time.sleep(2 ** attempt)  # Exponential backoff
```

See [Advanced Usage - Retry Logic](examples/advanced-usage.md#retry-logic) for complete examples.

## Sessions

### What's the difference between PulsarSession and AgenticSession?

**`PulsarSession`** - Basic session for web scraping:
- Page loading and caching
- CSS selector extraction
- WebDriver access
- No AI features

**`AgenticSession`** - Extends PulsarSession with AI:
- All PulsarSession features
- AI-powered actions (`act()`, `run()`)
- Page observation and analysis
- AI extraction and summarization

### How many sessions can I create?

You can create many sessions, limited by:
- Available memory
- Server configuration
- Browser resources

For most use cases, 5-20 concurrent sessions work well.

### Do I need to close sessions?

Yes! Always close sessions to free resources:

```python
session.close()  # Close session
client.close()   # Close client
```

Or use context managers to ensure cleanup.

### Can I reuse sessions?

Yes, but clear history between tasks:

```python
# Task 1
session.run("complete task 1")

# Clear for task 2
session.clear_history()

# Task 2
session.run("complete task 2")
```

## Troubleshooting

### "Browser4.jar not found" error

The SDK should download it automatically. If it fails:

1. **Check internet connection**
2. **Check proxy settings**: Set `HTTP_PROXY`/`HTTPS_PROXY`
3. **Download manually**: Get from [GitHub releases](https://github.com/platonai/Browser4/releases)
4. **Specify path**:
   ```python
   driver = Browser4Driver(jar_path="/path/to/Browser4.jar")
   ```

### "Port already in use" error

Change the port:
```python
driver = Browser4Driver(port=8183)
```

Or find used port and kill it:
```bash
# Linux/macOS
lsof -ti :8182 | xargs kill

# Windows
netstat -ano | findstr :8182
taskkill /PID <PID> /F
```

### SSL certificate errors

For development with self-signed certificates:
1. Add certificate to system trust store
2. Use HTTP instead of HTTPS (local only)
3. Configure reverse proxy with valid certificates

### Browser won't load pages

Check:
1. **Internet connection**: Can you access the URL in browser?
2. **Firewall**: Is Browser4 allowed to make connections?
3. **Chrome installation**: Browser4 needs Chrome installed
4. **Headless mode**: Try with headless=false for debugging
   ```python
   driver = Browser4Driver(java_options={"browser4.driver.headless": "false"})
   ```

## Best Practices

### Should I use Browser4Driver or connect to remote server?

- **Local development**: Use `Browser4Driver` for convenience
- **Production**: Use remote server for better resource management
- **CI/CD**: Use `Browser4Driver` for isolated tests
- **Large scale**: Use centralized server infrastructure

### How should I structure my scrapers?

Use classes for organization:
```python
class MyScraper:
    def __init__(self, session):
        self.session = session
    
    def scrape_page(self, url):
        # Scraping logic
        pass
    
    def extract_data(self, page):
        # Extraction logic
        pass
```

See [Complete Workflow Examples](examples/complete-workflow.md) for full examples.

### How do I handle different page structures?

Use graceful degradation:
```python
def safe_extract(document, selectors, defaults):
    fields = session.extract(document, selectors)
    return {k: fields.get(k, defaults.get(k)) for k in selectors}
```

### Should I use AI features for everything?

No. Use AI strategically:
- **Traditional extraction**: Known, stable selectors
- **AI extraction**: Dynamic or complex pages
- **Traditional navigation**: Known workflows
- **AI navigation**: Exploratory or adaptive tasks

## Getting Help

### Where can I get help?

- **Documentation**: Read the guides in `docs/`
- **Examples**: Check `examples/` directory
- **GitHub Issues**: Report bugs or request features
- **Stack Overflow**: Tag questions with `browser4`

### How do I report a bug?

1. Check if it's already reported on [GitHub Issues](https://github.com/platonai/Browser4/issues)
2. Create a minimal reproduction example
3. Include:
   - Python version
   - SDK version
   - Operating system
   - Error messages
   - Steps to reproduce

### How do I request a feature?

Open a feature request on [GitHub Issues](https://github.com/platonai/Browser4/issues) with:
- Use case description
- Expected behavior
- Example code (if applicable)

### Where is the source code?

- **Main repository**: https://github.com/platonai/Browser4
- **Python SDK**: https://github.com/platonai/Browser4/tree/master/sdks/browser4-sdk-python

## Next Steps

- [Getting Started](getting-started/installation.md) - Installation and setup
- [Basic Usage](examples/basic-usage.md) - Learn the fundamentals
- [Advanced Usage](examples/advanced-usage.md) - Advanced patterns
- [Complete Workflows](examples/complete-workflow.md) - Real-world examples
