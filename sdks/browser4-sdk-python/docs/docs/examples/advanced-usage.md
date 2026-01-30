# Advanced Usage

This guide covers advanced Browser4 Python SDK features including multiple sessions, complex data extraction, error handling, and performance optimization.

## Multiple Concurrent Sessions

### Managing Multiple Sessions

Create and manage multiple browser sessions:

```python
from browser4 import Browser4Driver, PulsarClient, AgenticSession

with Browser4Driver() as driver:
    client = PulsarClient(base_url=driver.base_url)
    
    # Create multiple sessions
    sessions = []
    for i in range(3):
        session_id = client.create_session()
        session = AgenticSession(client)
        sessions.append((session_id, session))
        print(f"Created session {i+1}: {session_id}")
    
    # Use sessions independently
    for idx, (session_id, session) in enumerate(sessions):
        url = f"https://example.com/page{idx+1}"
        page = session.open(url)
        print(f"Session {idx+1} loaded: {page.url}")
    
    # Clean up all sessions
    for session_id, session in sessions:
        session.close()
        print(f"Closed session: {session_id}")
    
    client.close()
```

### Session Pooling Pattern

Implement a simple session pool for reusability:

```python
from typing import List, Optional
from browser4 import PulsarClient, AgenticSession

class SessionPool:
    """Simple session pool for reusing browser sessions."""
    
    def __init__(self, client: PulsarClient, size: int = 5):
        self.client = client
        self.size = size
        self.available: List[AgenticSession] = []
        self.in_use: List[AgenticSession] = []
        self._initialize_pool()
    
    def _initialize_pool(self):
        """Create initial pool of sessions."""
        for _ in range(self.size):
            session_id = self.client.create_session()
            session = AgenticSession(self.client)
            self.available.append(session)
    
    def acquire(self) -> Optional[AgenticSession]:
        """Get a session from the pool."""
        if not self.available:
            # Optionally create a new session if pool is exhausted
            session_id = self.client.create_session()
            session = AgenticSession(self.client)
            self.in_use.append(session)
            return session
        
        session = self.available.pop()
        self.in_use.append(session)
        return session
    
    def release(self, session: AgenticSession):
        """Return a session to the pool."""
        if session in self.in_use:
            self.in_use.remove(session)
            # Clear session state before reuse
            session.clear_history()
            self.available.append(session)
    
    def close_all(self):
        """Close all sessions in the pool."""
        for session in self.available + self.in_use:
            session.close()
        self.available.clear()
        self.in_use.clear()

# Usage
client = PulsarClient(base_url="http://localhost:8182")
pool = SessionPool(client, size=3)

try:
    # Acquire session
    session = pool.acquire()
    page = session.open("https://example.com")
    # ... use session ...
    
    # Release back to pool
    pool.release(session)
    
finally:
    pool.close_all()
    client.close()
```

## Complex Data Extraction

### Nested Data Extraction

Extract complex nested structures:

```python
from browser4 import AgenticSession

def extract_product_listings(session: AgenticSession, url: str):
    """Extract product listings with nested data."""
    
    page = session.open(url)
    driver = session.driver
    
    # Find all product containers
    product_elements = driver.select_text_all(".product-card")
    
    products = []
    for idx in range(len(product_elements)):
        # Extract data for each product
        product = {
            "name": driver.select_first_text_or_null(
                f".product-card:nth-child({idx+1}) .product-name"
            ),
            "price": driver.select_first_text_or_null(
                f".product-card:nth-child({idx+1}) .price"
            ),
            "rating": driver.select_first_attribute_or_null(
                f".product-card:nth-child({idx+1}) .rating", "data-score"
            ),
            "image": driver.select_first_attribute_or_null(
                f".product-card:nth-child({idx+1}) img", "src"
            ),
            "availability": driver.select_first_text_or_null(
                f".product-card:nth-child({idx+1}) .stock-status"
            )
        }
        
        if product["name"]:
            products.append(product)
    
    return products

# Usage
session = AgenticSession(client)
products = extract_product_listings(session, "https://shop.example.com")
for product in products:
    print(f"Product: {product['name']} - ${product['price']}")
```

### Pagination Handling

Navigate through paginated results:

```python
from browser4 import AgenticSession
from typing import List, Dict

def scrape_all_pages(session: AgenticSession, start_url: str, max_pages: int = 10) -> List[Dict]:
    """Scrape data from all pages in a paginated list."""
    
    all_data = []
    current_page = 1
    
    page = session.open(start_url)
    
    while current_page <= max_pages:
        print(f"Scraping page {current_page}...")
        
        # Extract data from current page
        document = session.parse(page)
        if document:
            items = session.extract(document, {
                "titles": ".item-title",
                "descriptions": ".item-desc",
                "links": ".item-link@href"
            })
            all_data.append(items)
        
        # Check for next page
        driver = session.driver
        next_button = driver.exists(".pagination .next")
        
        if not next_button:
            print("No more pages")
            break
        
        # Click next page
        driver.click(".pagination .next")
        driver.delay(2000)  # Wait for page load
        
        # Capture current page
        page = session.capture(driver)
        current_page += 1
    
    return all_data

# Usage
session = AgenticSession(client)
all_results = scrape_all_pages(session, "https://example.com/search?q=python")
print(f"Scraped {len(all_results)} pages")
```

### Dynamic Content Extraction

Handle dynamically loaded content:

```python
from browser4 import AgenticSession

def extract_dynamic_content(session: AgenticSession, url: str):
    """Extract content that loads dynamically."""
    
    page = session.open(url)
    driver = session.driver
    
    # Wait for initial content
    driver.wait_for_selector(".content-container", timeout=10000)
    
    # Scroll to trigger lazy loading
    driver.scroll_to_bottom()
    driver.delay(2000)  # Wait for content to load
    
    # Scroll back up for more content
    driver.scroll_to_top()
    driver.delay(1000)
    
    # Extract all loaded content
    titles = driver.select_text_all(".article-title")
    summaries = driver.select_text_all(".article-summary")
    
    articles = []
    for title, summary in zip(titles, summaries):
        articles.append({
            "title": title,
            "summary": summary
        })
    
    return articles

# Usage
session = AgenticSession(client)
articles = extract_dynamic_content(session, "https://news.example.com")
print(f"Extracted {len(articles)} articles")
```

## Advanced Error Handling

### Retry Logic

Implement retry logic for unreliable operations:

```python
import time
from typing import Optional, Callable, TypeVar

T = TypeVar('T')

def retry_operation(
    operation: Callable[[], T],
    max_retries: int = 3,
    delay: float = 1.0,
    backoff: float = 2.0
) -> Optional[T]:
    """Retry an operation with exponential backoff."""
    
    for attempt in range(max_retries):
        try:
            return operation()
        except Exception as e:
            if attempt == max_retries - 1:
                print(f"Failed after {max_retries} attempts: {e}")
                raise
            
            wait_time = delay * (backoff ** attempt)
            print(f"Attempt {attempt + 1} failed: {e}. Retrying in {wait_time}s...")
            time.sleep(wait_time)
    
    return None

# Usage
def load_page():
    return session.open("https://example.com")

page = retry_operation(load_page, max_retries=3, delay=2.0)
```

### Graceful Degradation

Handle missing elements gracefully:

```python
from browser4 import AgenticSession
from typing import Dict, Any, Optional

def safe_extract(
    session: AgenticSession,
    url: str,
    selectors: Dict[str, str],
    defaults: Optional[Dict[str, Any]] = None
) -> Dict[str, Any]:
    """Extract data with fallback defaults."""
    
    defaults = defaults or {}
    result = {}
    
    try:
        page = session.open(url)
        document = session.parse(page)
        
        if not document:
            print("Failed to parse page, using defaults")
            return defaults.copy()
        
        fields = session.extract(document, selectors)
        
        # Apply defaults for missing fields
        for key in selectors:
            result[key] = fields.get(key, defaults.get(key))
        
        return result
        
    except Exception as e:
        print(f"Extraction failed: {e}, using defaults")
        return defaults.copy()

# Usage
data = safe_extract(
    session,
    "https://example.com",
    selectors={
        "title": "h1",
        "price": ".price",
        "rating": ".rating"
    },
    defaults={
        "title": "Unknown",
        "price": "N/A",
        "rating": 0
    }
)
```

### Context Manager for Sessions

Create a custom context manager:

```python
from contextlib import contextmanager
from browser4 import Browser4Driver, PulsarClient, AgenticSession

@contextmanager
def browser_session(port: int = 8182):
    """Context manager for automatic session cleanup."""
    
    driver = Browser4Driver(port=port)
    client = None
    session = None
    
    try:
        driver.start()
        client = PulsarClient(base_url=driver.base_url)
        session_id = client.create_session()
        session = AgenticSession(client)
        
        yield session
        
    finally:
        if session:
            session.close()
        if client:
            client.close()
        driver.stop()

# Usage
with browser_session() as session:
    page = session.open("https://example.com")
    print(f"Loaded: {page.url}")
# Everything cleaned up automatically
```

## Performance Optimization

### Batch URL Submission

Submit multiple URLs efficiently:

```python
from browser4 import AgenticSession
from typing import List

def batch_submit_urls(session: AgenticSession, urls: List[str], batch_size: int = 50):
    """Submit URLs in batches for async processing."""
    
    for i in range(0, len(urls), batch_size):
        batch = urls[i:i + batch_size]
        
        print(f"Submitting batch {i // batch_size + 1} ({len(batch)} URLs)...")
        
        for url in batch:
            session.submit(url, args="-expire 1d")
        
        print(f"Batch {i // batch_size + 1} submitted")

# Usage
urls = [f"https://example.com/page{i}" for i in range(1, 501)]
batch_submit_urls(session, urls, batch_size=100)
```

### Parallel Processing

Process multiple pages in parallel:

```python
from concurrent.futures import ThreadPoolExecutor, as_completed
from browser4 import Browser4Driver, PulsarClient, AgenticSession
from typing import List, Dict

def process_url(base_url: str, url: str) -> Dict:
    """Process a single URL in its own session."""
    
    client = PulsarClient(base_url=base_url)
    session_id = client.create_session()
    session = AgenticSession(client)
    
    try:
        page = session.open(url)
        document = session.parse(page)
        
        if document:
            data = session.extract(document, {
                "title": "h1",
                "content": ".content"
            })
            return {"url": url, "data": data, "success": True}
        else:
            return {"url": url, "data": None, "success": False}
    
    finally:
        session.close()
        client.close()

def parallel_scrape(urls: List[str], max_workers: int = 5) -> List[Dict]:
    """Scrape multiple URLs in parallel."""
    
    with Browser4Driver() as driver:
        results = []
        
        with ThreadPoolExecutor(max_workers=max_workers) as executor:
            # Submit all tasks
            future_to_url = {
                executor.submit(process_url, driver.base_url, url): url
                for url in urls
            }
            
            # Process results as they complete
            for future in as_completed(future_to_url):
                url = future_to_url[future]
                try:
                    result = future.result()
                    results.append(result)
                    print(f"Completed: {url}")
                except Exception as e:
                    print(f"Failed {url}: {e}")
                    results.append({"url": url, "data": None, "success": False})
        
        return results

# Usage
urls = [
    "https://example.com/page1",
    "https://example.com/page2",
    "https://example.com/page3",
]
results = parallel_scrape(urls, max_workers=3)
print(f"Processed {len(results)} URLs")
```

### Caching Strategy

Implement intelligent caching:

```python
from browser4 import AgenticSession
import hashlib
import json
from pathlib import Path
from typing import Dict, Optional

class CachedScraper:
    """Scraper with local caching."""
    
    def __init__(self, session: AgenticSession, cache_dir: str = ".cache"):
        self.session = session
        self.cache_dir = Path(cache_dir)
        self.cache_dir.mkdir(exist_ok=True)
    
    def _cache_key(self, url: str, selectors: Dict) -> str:
        """Generate cache key from URL and selectors."""
        key_data = f"{url}:{json.dumps(selectors, sort_keys=True)}"
        return hashlib.md5(key_data.encode()).hexdigest()
    
    def _get_cached(self, key: str) -> Optional[Dict]:
        """Get cached data if available."""
        cache_file = self.cache_dir / f"{key}.json"
        if cache_file.exists():
            with open(cache_file, 'r') as f:
                return json.load(f)
        return None
    
    def _save_cache(self, key: str, data: Dict):
        """Save data to cache."""
        cache_file = self.cache_dir / f"{key}.json"
        with open(cache_file, 'w') as f:
            json.dump(data, f)
    
    def scrape(self, url: str, selectors: Dict, use_cache: bool = True) -> Dict:
        """Scrape with caching support."""
        
        cache_key = self._cache_key(url, selectors)
        
        # Check cache
        if use_cache:
            cached = self._get_cached(cache_key)
            if cached:
                print(f"Using cached data for {url}")
                return cached
        
        # Scrape fresh data
        print(f"Scraping {url}...")
        data = self.session.scrape(url, args="-expire 1h", fields=selectors)
        
        # Save to cache
        self._save_cache(cache_key, data)
        
        return data

# Usage
session = AgenticSession(client)
scraper = CachedScraper(session)

# First call: scrapes and caches
data1 = scraper.scrape("https://example.com", {"title": "h1"})

# Second call: uses cache
data2 = scraper.scrape("https://example.com", {"title": "h1"})

# Force fresh scrape
data3 = scraper.scrape("https://example.com", {"title": "h1"}, use_cache=False)
```

## Advanced WebDriver Usage

### Complex Interactions

Chain multiple WebDriver operations:

```python
from browser4 import AgenticSession

def complete_form_workflow(session: AgenticSession, url: str):
    """Complete a multi-step form workflow."""
    
    page = session.open(url)
    driver = session.driver
    
    # Wait for form to load
    driver.wait_for_selector("form#registration", timeout=10000)
    
    # Fill form fields
    driver.fill("input[name='username']", "john_doe")
    driver.fill("input[name='email']", "john@example.com")
    driver.fill("input[name='password']", "SecurePass123")
    
    # Check terms checkbox
    driver.check("input#terms")
    
    # Select from dropdown
    driver.click("select#country")
    driver.delay(500)
    driver.click("option[value='US']")
    
    # Submit form
    driver.click("button[type='submit']")
    
    # Wait for success message
    driver.wait_for_selector(".success-message", timeout=10000)
    
    # Verify success
    success_text = driver.select_first_text_or_null(".success-message")
    return success_text

# Usage
session = AgenticSession(client)
result = complete_form_workflow(session, "https://example.com/register")
print(f"Registration result: {result}")
```

### Screenshot Capture

Capture screenshots at different points:

```python
from browser4 import AgenticSession
from pathlib import Path

def capture_workflow_screenshots(session: AgenticSession, url: str, output_dir: str = "screenshots"):
    """Capture screenshots during a workflow."""
    
    output = Path(output_dir)
    output.mkdir(exist_ok=True)
    
    driver = session.driver
    page = session.open(url)
    
    # Capture full page
    screenshot1 = driver.capture_screenshot()
    (output / "01_initial.png").write_bytes(screenshot1)
    
    # Interact and capture
    driver.click("button.show-more")
    driver.delay(1000)
    screenshot2 = driver.capture_screenshot()
    (output / "02_after_click.png").write_bytes(screenshot2)
    
    # Capture specific element
    screenshot3 = driver.capture_screenshot(selector=".main-content")
    (output / "03_main_content.png").write_bytes(screenshot3)
    
    print(f"Screenshots saved to {output_dir}/")

# Usage
session = AgenticSession(client)
capture_workflow_screenshots(session, "https://example.com")
```

## Next Steps

- [AI Automation](ai-automation.md) - AI-powered browser automation
- [Complete Workflow](complete-workflow.md) - End-to-end scraping pipelines
- [Configuration](../configuration/browser4-driver.md) - Advanced configuration options
- [API Reference](../api/agentic-session.md) - Complete API documentation
