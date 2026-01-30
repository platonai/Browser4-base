# Advanced Patterns Guide

This guide covers advanced usage patterns, production deployment strategies, complex workflows, and expert techniques for the Browser4 Python SDK. Learn how to build robust, scalable, and maintainable scraping applications.

## Table of Contents

- [Session Reuse and Reconnection](#session-reuse-and-reconnection)
- [Custom Headers and Authentication](#custom-headers-and-authentication)
- [Timeout Configuration](#timeout-configuration)
- [Combining WebDriver and Agent](#combining-webdriver-and-agent)
- [Complex Workflows](#complex-workflows)
- [Error Recovery Strategies](#error-recovery-strategies)
- [Testing and Debugging](#testing-and-debugging)
- [Performance Tuning](#performance-tuning)
- [Production Deployment](#production-deployment)

---

## Session Reuse and Reconnection

Efficient session management is critical for production applications.

### Basic Session Reuse

```python
from pulsar_sdk import PulsarClient, PulsarSession

class SessionManager:
    """Manage session lifecycle efficiently."""
    
    def __init__(self, base_url="http://localhost:8182"):
        self.client = PulsarClient(base_url=base_url)
        self.session = None
        self.session_id = None
    
    def create_session(self):
        """Create a new session."""
        if self.session:
            self.close_session()
        
        self.session_id = self.client.create_session()
        self.session = PulsarSession(self.client)
        print(f"Created session: {self.session_id}")
        return self.session
    
    def get_session(self):
        """Get current session or create new one."""
        if not self.session or not self.session.is_active:
            return self.create_session()
        return self.session
    
    def close_session(self):
        """Close current session."""
        if self.session:
            try:
                self.session.close()
            except Exception as e:
                print(f"Error closing session: {e}")
            finally:
                self.session = None
                self.session_id = None
    
    def __enter__(self):
        """Context manager entry."""
        self.create_session()
        return self
    
    def __exit__(self, exc_type, exc_val, exc_tb):
        """Context manager exit."""
        self.close_session()
        self.client.close()

# Usage
with SessionManager() as manager:
    session = manager.get_session()
    
    # Do multiple operations with same session
    page1 = session.load("https://example.com/page1")
    page2 = session.load("https://example.com/page2")
    page3 = session.load("https://example.com/page3")
```

### Session Pool Pattern

```python
from contextlib import contextmanager
from typing import List
import queue

class SessionPool:
    """Pool of reusable sessions."""
    
    def __init__(self, size=3, base_url="http://localhost:8182"):
        self.base_url = base_url
        self.size = size
        self.pool = queue.Queue(maxsize=size)
        self._initialize_pool()
    
    def _initialize_pool(self):
        """Create initial sessions."""
        for _ in range(self.size):
            client = PulsarClient(base_url=self.base_url)
            client.create_session()
            session = PulsarSession(client)
            self.pool.put((client, session))
    
    @contextmanager
    def get_session(self):
        """Get a session from pool."""
        client, session = self.pool.get()
        try:
            yield session
        finally:
            # Return to pool
            self.pool.put((client, session))
    
    def cleanup(self):
        """Close all sessions in pool."""
        while not self.pool.empty():
            client, session = self.pool.get()
            try:
                session.close()
                client.close()
            except:
                pass

# Usage
pool = SessionPool(size=3)

try:
    # Multiple tasks using pool
    with pool.get_session() as session:
        page = session.load("https://example.com/page1")
    
    with pool.get_session() as session:
        page = session.load("https://example.com/page2")
    
    with pool.get_session() as session:
        page = session.load("https://example.com/page3")

finally:
    pool.cleanup()
```

### Reconnection Strategy

```python
import time

class ResilientSession:
    """Session with automatic reconnection."""
    
    def __init__(self, base_url="http://localhost:8182", max_retries=3):
        self.base_url = base_url
        self.max_retries = max_retries
        self.client = None
        self.session = None
        self._connect()
    
    def _connect(self):
        """Establish connection."""
        self.client = PulsarClient(base_url=self.base_url)
        self.client.create_session()
        self.session = PulsarSession(self.client)
    
    def _reconnect(self):
        """Reconnect after failure."""
        print("Reconnecting...")
        try:
            if self.session:
                self.session.close()
            if self.client:
                self.client.close()
        except:
            pass
        
        self._connect()
        print("Reconnected successfully")
    
    def load_with_retry(self, url, args=None):
        """Load page with automatic retry."""
        for attempt in range(self.max_retries):
            try:
                return self.session.load(url, args)
            except Exception as e:
                print(f"Attempt {attempt + 1} failed: {e}")
                
                if attempt < self.max_retries - 1:
                    self._reconnect()
                    time.sleep(2 ** attempt)  # Exponential backoff
                else:
                    raise
    
    def close(self):
        """Close session."""
        if self.session:
            self.session.close()
        if self.client:
            self.client.close()

# Usage
resilient = ResilientSession()

try:
    page1 = resilient.load_with_retry("https://example.com/page1")
    page2 = resilient.load_with_retry("https://example.com/page2")
    page3 = resilient.load_with_retry("https://example.com/page3")
finally:
    resilient.close()
```

---

## Custom Headers and Authentication

Handle authentication and custom headers for protected resources.

### Basic Authentication

```python
from pulsar_sdk import PulsarClient, PulsarSession

# Client-level headers (applied to all requests)
client = PulsarClient(
    base_url="http://localhost:8182",
    default_headers={
        "Authorization": "Bearer your-api-token-here",
        "X-API-Key": "your-api-key",
        "User-Agent": "MyBot/1.0"
    }
)

client.create_session()
session = PulsarSession(client)

# Now all requests include these headers
page = session.load("https://api.example.com/protected-resource")
```

### Dynamic Authentication

```python
class AuthenticatedSession:
    """Session with token refresh capability."""
    
    def __init__(self, base_url, auth_url, credentials):
        self.base_url = base_url
        self.auth_url = auth_url
        self.credentials = credentials
        self.token = None
        self.client = None
        self.session = None
        
        self._authenticate()
    
    def _authenticate(self):
        """Obtain authentication token."""
        import requests
        
        # Get token from auth endpoint
        response = requests.post(
            self.auth_url,
            json=self.credentials
        )
        response.raise_for_status()
        
        self.token = response.json()["access_token"]
        print("Authenticated successfully")
        
        # Create client with token
        self._create_client()
    
    def _create_client(self):
        """Create client with current token."""
        if self.client:
            self.client.close()
        
        self.client = PulsarClient(
            base_url=self.base_url,
            default_headers={
                "Authorization": f"Bearer {self.token}"
            }
        )
        
        self.client.create_session()
        self.session = PulsarSession(self.client)
    
    def refresh_token(self):
        """Refresh authentication token."""
        print("Refreshing token...")
        self._authenticate()
    
    def load_authenticated(self, url, args=None):
        """Load with automatic token refresh on 401."""
        try:
            return self.session.load(url, args)
        except Exception as e:
            if "401" in str(e) or "Unauthorized" in str(e):
                print("Token expired, refreshing...")
                self.refresh_token()
                return self.session.load(url, args)
            raise
    
    def close(self):
        """Close session."""
        if self.session:
            self.session.close()
        if self.client:
            self.client.close()

# Usage
auth_session = AuthenticatedSession(
    base_url="http://localhost:8182",
    auth_url="https://auth.example.com/token",
    credentials={
        "username": "user@example.com",
        "password": "secure-password"
    }
)

try:
    page = auth_session.load_authenticated("https://api.example.com/data")
    print(f"Loaded: {page.url}")
finally:
    auth_session.close()
```

### Custom User-Agent Rotation

```python
import random

class RotatingUserAgentClient:
    """Client with rotating user agents."""
    
    USER_AGENTS = [
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36",
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36",
    ]
    
    def __init__(self, base_url="http://localhost:8182"):
        self.base_url = base_url
        self.current_client = None
        self.current_session = None
    
    def _get_random_user_agent(self):
        """Get random user agent."""
        return random.choice(self.USER_AGENTS)
    
    def create_session(self):
        """Create session with random user agent."""
        user_agent = self._get_random_user_agent()
        print(f"Using User-Agent: {user_agent[:50]}...")
        
        self.current_client = PulsarClient(
            base_url=self.base_url,
            default_headers={
                "User-Agent": user_agent
            }
        )
        
        self.current_client.create_session()
        self.current_session = PulsarSession(self.current_client)
        
        return self.current_session
    
    def close(self):
        """Close current session."""
        if self.current_session:
            self.current_session.close()
        if self.current_client:
            self.current_client.close()

# Usage
rotator = RotatingUserAgentClient()

try:
    session = rotator.create_session()
    page = session.load("https://example.com")
finally:
    rotator.close()
```

---

## Timeout Configuration

Configure timeouts for different operations.

### Client-Level Timeout

```python
# Set default timeout for all HTTP requests
client = PulsarClient(
    base_url="http://localhost:8182",
    timeout=60.0  # 60 seconds
)

client.create_session()
session = PulsarSession(client)

# All requests use this timeout
page = session.load("https://slow-site.example.com")
```

### Operation-Specific Timeouts

```python
from pulsar_sdk import PulsarClient, AgenticSession

client = PulsarClient()
client.create_session()
session = AgenticSession(client)
driver = session.driver

# Navigate to page
driver.navigate_to("https://example.com")

# Wait for element with timeout
driver.wait_for_selector(".dynamic-content", timeout=15000)  # 15 seconds

# Agent operation with timeout
result = session.act(
    "click the submit button",
    timeout_ms=30000  # 30 seconds
)

# Agent with DOM settle timeout
result = session.act(
    "click load more",
    dom_settle_timeout_ms=5000  # Wait 5s for DOM to stabilize
)

session.close()
client.close()
```

### Timeout Wrapper

```python
import signal
from contextlib import contextmanager

class TimeoutError(Exception):
    pass

@contextmanager
def timeout(seconds):
    """Context manager for operation timeout."""
    def timeout_handler(signum, frame):
        raise TimeoutError(f"Operation timed out after {seconds} seconds")
    
    # Set the signal handler
    old_handler = signal.signal(signal.SIGALRM, timeout_handler)
    signal.alarm(seconds)
    
    try:
        yield
    finally:
        signal.alarm(0)
        signal.signal(signal.SIGALRM, old_handler)

# Usage (Unix-like systems only)
client = PulsarClient()
client.create_session()
session = PulsarSession(client)

try:
    with timeout(30):
        page = session.load("https://very-slow-site.example.com")
        print("Loaded successfully")
except TimeoutError as e:
    print(f"Operation timed out: {e}")
finally:
    session.close()
    client.close()
```

---

## Combining WebDriver and Agent

Leverage both traditional and AI-powered approaches for optimal results.

### Pattern 1: AI Navigation + WebDriver Extraction

```python
from pulsar_sdk import PulsarClient, AgenticSession

def hybrid_scraping_pattern(url):
    """Use AI for navigation, WebDriver for extraction."""
    client = PulsarClient()
    client.create_session()
    session = AgenticSession(client)
    
    try:
        # Step 1: AI handles complex navigation
        session.open(url)
        
        result = session.run("""
            Find the products section,
            apply the 'Electronics' filter,
            and sort by price ascending
        """)
        
        if not result.success:
            print(f"Navigation failed: {result.message}")
            return None
        
        # Step 2: WebDriver for fast, reliable extraction
        driver = session.driver
        
        products = []
        titles = driver.select_text_all(".product-title")
        prices = driver.select_text_all(".product-price")
        ratings = driver.select_text_all(".product-rating")
        
        for title, price, rating in zip(titles, prices, ratings):
            products.append({
                "title": title,
                "price": price,
                "rating": rating
            })
        
        print(f"Extracted {len(products)} products")
        return products
        
    finally:
        session.close()
        client.close()

# Use it
products = hybrid_scraping_pattern("https://shop.example.com")
```

### Pattern 2: WebDriver Setup + AI Interaction

```python
def setup_then_interact(url, form_data):
    """Use WebDriver for setup, AI for interaction."""
    client = PulsarClient()
    client.create_session()
    session = AgenticSession(client)
    
    try:
        # WebDriver for precise navigation
        driver = session.driver
        driver.navigate_to(url)
        
        # Wait for page to be ready
        driver.wait_for_selector("form.contact-form", timeout=10000)
        
        # AI for form filling (handles validation, errors)
        result = session.run(f"""
            Fill in the contact form:
            - Name: {form_data['name']}
            - Email: {form_data['email']}
            - Message: {form_data['message']}
            Then submit the form
        """)
        
        if result.success:
            # WebDriver to extract confirmation
            confirmation = driver.select_first_text_or_null(".success-message")
            return {"success": True, "message": confirmation}
        else:
            return {"success": False, "error": result.message}
        
    finally:
        session.close()
        client.close()

# Use it
result = setup_then_interact(
    "https://example.com/contact",
    {
        "name": "John Doe",
        "email": "john@example.com",
        "message": "I have a question"
    }
)
```

### Pattern 3: Conditional Strategy Selection

```python
def smart_extraction(url, use_ai_threshold=0.5):
    """Intelligently choose between AI and WebDriver."""
    client = PulsarClient()
    client.create_session()
    session = AgenticSession(client)
    
    try:
        session.open(url)
        
        # Observe page complexity
        observation = session.observe("analyze page structure")
        
        # Decide strategy based on observation
        if "dynamic" in observation.description.lower() or \
           "complex" in observation.description.lower():
            
            print("Using AI approach for complex page")
            extraction = session.agent_extract(
                "Extract product information including name, price, and availability"
            )
            return extraction.data if extraction.success else None
        else:
            print("Using WebDriver approach for simple page")
            driver = session.driver
            
            return driver.extract({
                "name": ".product-name",
                "price": ".product-price",
                "availability": ".stock-status"
            })
        
    finally:
        session.close()
        client.close()

# Use it
data = smart_extraction("https://shop.example.com/product/123")
```

---

## Complex Workflows

Build sophisticated multi-step scraping workflows.

### Multi-Site Aggregation

```python
from typing import List, Dict, Any
import json

class MultiSiteAggregator:
    """Aggregate data from multiple sites."""
    
    def __init__(self, sites_config: List[Dict[str, Any]]):
        self.sites_config = sites_config
        self.client = PulsarClient()
        self.session = None
    
    def setup(self):
        """Setup session."""
        self.client.create_session()
        self.session = AgenticSession(self.client)
    
    def scrape_site(self, site_config):
        """Scrape a single site."""
        url = site_config["url"]
        selectors = site_config["selectors"]
        site_name = site_config["name"]
        
        print(f"\n📍 Scraping {site_name}...")
        
        try:
            # Load page
            page = self.session.load(url, args="-expire 1h")
            driver = self.session.driver
            driver.navigate_to(page.url)
            
            # Extract data
            data = driver.extract(selectors)
            data["source"] = site_name
            data["url"] = url
            
            print(f"  ✓ Success")
            return data
            
        except Exception as e:
            print(f"  ✗ Error: {e}")
            return {
                "source": site_name,
                "url": url,
                "error": str(e)
            }
    
    def aggregate_all(self):
        """Scrape all sites and aggregate."""
        results = []
        
        for site_config in self.sites_config:
            result = self.scrape_site(site_config)
            results.append(result)
        
        return results
    
    def cleanup(self):
        """Cleanup resources."""
        if self.session:
            self.session.close()
        if self.client:
            self.client.close()

# Usage
sites = [
    {
        "name": "Site A",
        "url": "https://site-a.example.com/products",
        "selectors": {
            "title": "h1.product-title",
            "price": ".price",
            "rating": ".rating"
        }
    },
    {
        "name": "Site B",
        "url": "https://site-b.example.com/items",
        "selectors": {
            "title": ".item-name",
            "price": ".item-price",
            "rating": ".star-rating"
        }
    }
]

aggregator = MultiSiteAggregator(sites)
aggregator.setup()

try:
    results = aggregator.aggregate_all()
    
    # Export results
    with open("aggregated_data.json", "w") as f:
        json.dump(results, f, indent=2)
    
    print(f"\n✅ Aggregated data from {len(sites)} sites")
    
finally:
    aggregator.cleanup()
```

### Sequential Workflow with State

```python
from enum import Enum
from dataclasses import dataclass, field
from typing import Optional, List, Dict, Any

class WorkflowState(Enum):
    INITIALIZED = "initialized"
    SEARCHING = "searching"
    FILTERING = "filtering"
    EXTRACTING = "extracting"
    COMPLETED = "completed"
    FAILED = "failed"

@dataclass
class WorkflowContext:
    """Context for workflow execution."""
    state: WorkflowState = WorkflowState.INITIALIZED
    current_url: Optional[str] = None
    search_query: Optional[str] = None
    extracted_data: List[Dict[str, Any]] = field(default_factory=list)
    errors: List[str] = field(default_factory=list)

class StatefulWorkflow:
    """Stateful scraping workflow."""
    
    def __init__(self):
        self.context = WorkflowContext()
        self.client = PulsarClient()
        self.session = None
    
    def setup(self):
        """Setup session."""
        self.client.create_session()
        self.session = AgenticSession(self.client)
    
    def execute_search(self, base_url, query):
        """Step 1: Execute search."""
        print(f"🔍 Searching for: {query}")
        self.context.state = WorkflowState.SEARCHING
        self.context.search_query = query
        
        try:
            self.session.open(base_url)
            
            result = self.session.run(f"""
                Find the search box,
                type '{query}',
                and submit the search
            """)
            
            if not result.success:
                raise Exception(f"Search failed: {result.message}")
            
            self.context.current_url = self.session.driver.current_url()
            print("  ✓ Search completed")
            return True
            
        except Exception as e:
            self.context.state = WorkflowState.FAILED
            self.context.errors.append(str(e))
            print(f"  ✗ Search failed: {e}")
            return False
    
    def apply_filters(self, filters):
        """Step 2: Apply filters."""
        print(f"🔧 Applying filters: {filters}")
        self.context.state = WorkflowState.FILTERING
        
        try:
            filter_instructions = ", ".join([
                f"apply filter '{k}': '{v}'"
                for k, v in filters.items()
            ])
            
            result = self.session.run(filter_instructions)
            
            if not result.success:
                raise Exception(f"Filtering failed: {result.message}")
            
            print("  ✓ Filters applied")
            return True
            
        except Exception as e:
            self.context.errors.append(str(e))
            print(f"  ✗ Filtering failed: {e}")
            return False
    
    def extract_results(self):
        """Step 3: Extract results."""
        print("📊 Extracting results...")
        self.context.state = WorkflowState.EXTRACTING
        
        try:
            driver = self.session.driver
            
            # Extract all results
            titles = driver.select_text_all(".result-title")
            prices = driver.select_text_all(".result-price")
            ratings = driver.select_text_all(".result-rating")
            
            for title, price, rating in zip(titles, prices, ratings):
                self.context.extracted_data.append({
                    "title": title,
                    "price": price,
                    "rating": rating
                })
            
            print(f"  ✓ Extracted {len(self.context.extracted_data)} results")
            return True
            
        except Exception as e:
            self.context.errors.append(str(e))
            print(f"  ✗ Extraction failed: {e}")
            return False
    
    def execute(self, base_url, query, filters=None):
        """Execute complete workflow."""
        print("🚀 Starting workflow\n")
        
        # Step 1: Search
        if not self.execute_search(base_url, query):
            return self.context
        
        # Step 2: Filter (if provided)
        if filters and not self.apply_filters(filters):
            return self.context
        
        # Step 3: Extract
        if not self.extract_results():
            return self.context
        
        # Complete
        self.context.state = WorkflowState.COMPLETED
        print("\n✅ Workflow completed")
        
        return self.context
    
    def cleanup(self):
        """Cleanup resources."""
        if self.session:
            self.session.close()
        if self.client:
            self.client.close()

# Usage
workflow = StatefulWorkflow()
workflow.setup()

try:
    context = workflow.execute(
        base_url="https://shop.example.com",
        query="laptop",
        filters={
            "category": "Electronics",
            "price": "under $1000"
        }
    )
    
    print(f"\nFinal State: {context.state.value}")
    print(f"Results: {len(context.extracted_data)}")
    
    if context.errors:
        print(f"Errors: {context.errors}")
    
finally:
    workflow.cleanup()
```

---

## Error Recovery Strategies

Build resilient scrapers that handle failures gracefully.

### Retry with Exponential Backoff

```python
import time
import random

def retry_with_backoff(func, max_retries=5, base_delay=1):
    """Retry function with exponential backoff."""
    for attempt in range(max_retries):
        try:
            return func()
        except Exception as e:
            if attempt == max_retries - 1:
                raise
            
            # Exponential backoff with jitter
            delay = base_delay * (2 ** attempt) + random.uniform(0, 1)
            print(f"Attempt {attempt + 1} failed: {e}")
            print(f"Retrying in {delay:.1f} seconds...")
            time.sleep(delay)

# Usage
def scrape_page():
    client = PulsarClient()
    client.create_session()
    session = PulsarSession(client)
    
    try:
        page = session.load("https://unreliable-site.example.com")
        driver = session.driver
        driver.navigate_to(page.url)
        return driver.select_first_text_or_null("h1")
    finally:
        session.close()
        client.close()

# Retry with backoff
try:
    result = retry_with_backoff(scrape_page, max_retries=5)
    print(f"Success: {result}")
except Exception as e:
    print(f"All retries failed: {e}")
```

### Circuit Breaker Pattern

```python
from datetime import datetime, timedelta

class CircuitBreaker:
    """Circuit breaker for failing operations."""
    
    def __init__(self, failure_threshold=5, timeout_duration=60):
        self.failure_threshold = failure_threshold
        self.timeout_duration = timeout_duration
        self.failure_count = 0
        self.last_failure_time = None
        self.state = "CLOSED"  # CLOSED, OPEN, HALF_OPEN
    
    def call(self, func, *args, **kwargs):
        """Call function with circuit breaker."""
        if self.state == "OPEN":
            if self._should_attempt_reset():
                self.state = "HALF_OPEN"
            else:
                raise Exception("Circuit breaker is OPEN")
        
        try:
            result = func(*args, **kwargs)
            self._on_success()
            return result
        except Exception as e:
            self._on_failure()
            raise
    
    def _should_attempt_reset(self):
        """Check if should attempt reset."""
        return (
            self.last_failure_time and
            datetime.now() - self.last_failure_time > timedelta(seconds=self.timeout_duration)
        )
    
    def _on_success(self):
        """Handle successful call."""
        self.failure_count = 0
        self.state = "CLOSED"
    
    def _on_failure(self):
        """Handle failed call."""
        self.failure_count += 1
        self.last_failure_time = datetime.now()
        
        if self.failure_count >= self.failure_threshold:
            self.state = "OPEN"
            print(f"Circuit breaker opened after {self.failure_count} failures")

# Usage
breaker = CircuitBreaker(failure_threshold=3, timeout_duration=30)

def scrape_with_breaker(url):
    """Scrape with circuit breaker protection."""
    def _scrape():
        client = PulsarClient()
        client.create_session()
        session = PulsarSession(client)
        
        try:
            page = session.load(url)
            driver = session.driver
            driver.navigate_to(page.url)
            return driver.select_first_text_or_null("h1")
        finally:
            session.close()
            client.close()
    
    return breaker.call(_scrape)

# Try scraping multiple URLs
urls = ["https://example.com/page1", "https://example.com/page2"]

for url in urls:
    try:
        result = scrape_with_breaker(url)
        print(f"✓ {url}: {result}")
    except Exception as e:
        print(f"✗ {url}: {e}")
```

### Fallback Strategy

```python
def scrape_with_fallback(url, strategies):
    """Try multiple strategies until one succeeds."""
    errors = []
    
    for strategy_name, strategy_func in strategies:
        print(f"Trying {strategy_name}...")
        
        try:
            result = strategy_func(url)
            print(f"✓ {strategy_name} succeeded")
            return result
        except Exception as e:
            print(f"✗ {strategy_name} failed: {e}")
            errors.append((strategy_name, str(e)))
    
    # All strategies failed
    raise Exception(f"All strategies failed: {errors}")

# Define strategies
def strategy_cached(url):
    """Try with caching."""
    client = PulsarClient()
    client.create_session()
    session = PulsarSession(client)
    
    try:
        page = session.load(url, args="-expire 1d")
        driver = session.driver
        driver.navigate_to(page.url)
        return driver.select_first_text_or_null("h1")
    finally:
        session.close()
        client.close()

def strategy_fresh(url):
    """Try with fresh fetch."""
    client = PulsarClient()
    client.create_session()
    session = PulsarSession(client)
    
    try:
        page = session.open(url)  # Always fresh
        driver = session.driver
        driver.navigate_to(page.url)
        return driver.select_first_text_or_null("h1")
    finally:
        session.close()
        client.close()

def strategy_ai(url):
    """Try with AI extraction."""
    client = PulsarClient()
    client.create_session()
    session = AgenticSession(client)
    
    try:
        session.open(url)
        extraction = session.agent_extract("Extract the page title")
        return extraction.data if extraction.success else None
    finally:
        session.close()
        client.close()

# Use fallback
strategies = [
    ("Cached", strategy_cached),
    ("Fresh", strategy_fresh),
    ("AI", strategy_ai)
]

result = scrape_with_fallback("https://example.com", strategies)
print(f"Final result: {result}")
```

---

## Testing and Debugging

Write tests and debug scraping code effectively.

### Unit Testing

```python
import unittest
from unittest.mock import Mock, patch

class TestScrapingFunctions(unittest.TestCase):
    """Unit tests for scraping functions."""
    
    def setUp(self):
        """Setup test fixtures."""
        self.client = PulsarClient()
        self.client.create_session()
        self.session = PulsarSession(self.client)
    
    def tearDown(self):
        """Cleanup after tests."""
        self.session.close()
        self.client.close()
    
    def test_load_page(self):
        """Test page loading."""
        page = self.session.load("https://example.com")
        
        self.assertIsNotNone(page)
        self.assertEqual(page.url, "https://example.com")
        self.assertFalse(page.is_nil)
    
    def test_extract_title(self):
        """Test title extraction."""
        driver = self.session.driver
        driver.navigate_to("https://example.com")
        
        title = driver.select_first_text_or_null("h1")
        
        self.assertIsNotNone(title)
        self.assertIsInstance(title, str)
    
    @patch('pulsar_sdk.PulsarClient')
    def test_with_mock_client(self, mock_client):
        """Test with mocked client."""
        # Configure mock
        mock_instance = Mock()
        mock_client.return_value = mock_instance
        
        # Test code
        client = PulsarClient()
        # ... assertions ...

if __name__ == '__main__':
    unittest.main()
```

### Integration Testing

```python
import pytest

@pytest.fixture
def session():
    """Pytest fixture for session."""
    client = PulsarClient()
    client.create_session()
    sess = PulsarSession(client)
    
    yield sess
    
    sess.close()
    client.close()

def test_scrape_product(session):
    """Test product scraping."""
    driver = session.driver
    driver.navigate_to("https://shop.example.com/product/123")
    
    product = driver.extract({
        "name": ".product-name",
        "price": ".product-price"
    })
    
    assert product["name"] is not None
    assert product["price"] is not None

def test_pagination(session):
    """Test pagination handling."""
    driver = session.driver
    driver.navigate_to("https://example.com/articles")
    
    # First page
    page1_items = driver.select_text_all(".article-title")
    assert len(page1_items) > 0
    
    # Navigate to page 2
    driver.click(".next-page")
    driver.delay(2000)
    
    page2_items = driver.select_text_all(".article-title")
    assert len(page2_items) > 0
```

### Debugging Utilities

```python
class DebugSession:
    """Session wrapper with debugging utilities."""
    
    def __init__(self):
        self.client = PulsarClient()
        self.session = None
        self.operation_log = []
    
    def setup(self):
        """Setup session."""
        self.client.create_session()
        self.session = PulsarSession(self.client)
        self._log("Session created")
    
    def _log(self, message):
        """Log operation."""
        import datetime
        timestamp = datetime.datetime.now().isoformat()
        entry = f"[{timestamp}] {message}"
        self.operation_log.append(entry)
        print(entry)
    
    def load(self, url, args=None):
        """Load with logging."""
        self._log(f"Loading: {url} (args: {args})")
        try:
            page = self.session.load(url, args)
            self._log(f"  ✓ Loaded: {page.url}")
            return page
        except Exception as e:
            self._log(f"  ✗ Error: {e}")
            raise
    
    def extract(self, selectors):
        """Extract with logging."""
        self._log(f"Extracting: {list(selectors.keys())}")
        driver = self.session.driver
        
        try:
            data = driver.extract(selectors)
            self._log(f"  ✓ Extracted {len(data)} fields")
            return data
        except Exception as e:
            self._log(f"  ✗ Error: {e}")
            raise
    
    def print_log(self):
        """Print complete log."""
        print("\n=== Operation Log ===")
        for entry in self.operation_log:
            print(entry)
    
    def save_log(self, filename):
        """Save log to file."""
        with open(filename, 'w') as f:
            for entry in self.operation_log:
                f.write(entry + '\n')
        self._log(f"Log saved to {filename}")
    
    def cleanup(self):
        """Cleanup resources."""
        self._log("Closing session")
        if self.session:
            self.session.close()
        if self.client:
            self.client.close()

# Usage
debug_session = DebugSession()
debug_session.setup()

try:
    page = debug_session.load("https://example.com")
    data = debug_session.extract({"title": "h1"})
    
    debug_session.print_log()
    debug_session.save_log("scraping_debug.log")
    
finally:
    debug_session.cleanup()
```

---

## Performance Tuning

Optimize scraping performance for production workloads.

### Connection Pooling

```python
import threading
from queue import Queue

class ConnectionPool:
    """Pool of client connections."""
    
    def __init__(self, pool_size=5, base_url="http://localhost:8182"):
        self.pool_size = pool_size
        self.base_url = base_url
        self.connections = Queue(maxsize=pool_size)
        self.lock = threading.Lock()
        
        self._initialize_pool()
    
    def _initialize_pool(self):
        """Initialize connection pool."""
        for _ in range(self.pool_size):
            client = PulsarClient(base_url=self.base_url)
            self.connections.put(client)
    
    def get_connection(self):
        """Get connection from pool."""
        return self.connections.get()
    
    def return_connection(self, client):
        """Return connection to pool."""
        self.connections.put(client)
    
    def cleanup(self):
        """Cleanup all connections."""
        while not self.connections.empty():
            client = self.connections.get()
            try:
                client.close()
            except:
                pass

# Usage
pool = ConnectionPool(pool_size=3)

def scrape_with_pool(url):
    """Scrape using connection pool."""
    client = pool.get_connection()
    
    try:
        client.create_session()
        session = PulsarSession(client)
        
        page = session.load(url)
        driver = session.driver
        driver.navigate_to(page.url)
        
        return driver.select_first_text_or_null("h1")
        
    finally:
        try:
            session.close()
        except:
            pass
        pool.return_connection(client)

# Scrape multiple URLs
urls = ["https://example.com/page1", "https://example.com/page2"]

for url in urls:
    result = scrape_with_pool(url)
    print(f"{url}: {result}")

pool.cleanup()
```

### Caching Layer

```python
import pickle
import hashlib
from pathlib import Path

class ScrapingCache:
    """Simple file-based cache for scraping results."""
    
    def __init__(self, cache_dir=".cache"):
        self.cache_dir = Path(cache_dir)
        self.cache_dir.mkdir(exist_ok=True)
    
    def _get_cache_key(self, url, selectors):
        """Generate cache key."""
        key_str = f"{url}:{sorted(selectors.items())}"
        return hashlib.md5(key_str.encode()).hexdigest()
    
    def get(self, url, selectors):
        """Get cached result."""
        key = self._get_cache_key(url, selectors)
        cache_file = self.cache_dir / f"{key}.pkl"
        
        if cache_file.exists():
            with open(cache_file, 'rb') as f:
                return pickle.load(f)
        
        return None
    
    def set(self, url, selectors, data):
        """Set cache entry."""
        key = self._get_cache_key(url, selectors)
        cache_file = self.cache_dir / f"{key}.pkl"
        
        with open(cache_file, 'wb') as f:
            pickle.dump(data, f)
    
    def clear(self):
        """Clear all cache."""
        for cache_file in self.cache_dir.glob("*.pkl"):
            cache_file.unlink()

# Usage
cache = ScrapingCache()

def scrape_with_cache(url, selectors):
    """Scrape with caching."""
    # Check cache first
    cached = cache.get(url, selectors)
    if cached:
        print(f"✓ Cache hit: {url}")
        return cached
    
    print(f"⚡ Cache miss: {url}")
    
    # Scrape
    client = PulsarClient()
    client.create_session()
    session = PulsarSession(client)
    driver = session.driver
    
    try:
        page = session.load(url)
        driver.navigate_to(page.url)
        
        data = driver.extract(selectors)
        
        # Cache result
        cache.set(url, selectors, data)
        
        return data
        
    finally:
        session.close()
        client.close()

# Scrape with caching
selectors = {"title": "h1", "description": ".description"}

# First call - cache miss
result1 = scrape_with_cache("https://example.com", selectors)

# Second call - cache hit
result2 = scrape_with_cache("https://example.com", selectors)
```

---

## Production Deployment

Deploy Browser4 scrapers in production environments.

### Configuration Management

```python
import os
from dataclasses import dataclass

@dataclass
class ScraperConfig:
    """Production scraper configuration."""
    browser_url: str
    timeout: float
    max_retries: int
    cache_duration: str
    rate_limit_delay: float
    log_level: str
    
    @classmethod
    def from_env(cls):
        """Load configuration from environment."""
        return cls(
            browser_url=os.getenv("BROWSER4_URL", "http://localhost:8182"),
            timeout=float(os.getenv("TIMEOUT", "60.0")),
            max_retries=int(os.getenv("MAX_RETRIES", "3")),
            cache_duration=os.getenv("CACHE_DURATION", "1h"),
            rate_limit_delay=float(os.getenv("RATE_LIMIT_DELAY", "1.0")),
            log_level=os.getenv("LOG_LEVEL", "INFO")
        )

# Usage
config = ScraperConfig.from_env()

client = PulsarClient(
    base_url=config.browser_url,
    timeout=config.timeout
)
```

### Logging and Monitoring

```python
import logging
from datetime import datetime

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s [%(levelname)s] %(message)s',
    handlers=[
        logging.FileHandler('scraper.log'),
        logging.StreamHandler()
    ]
)

logger = logging.getLogger(__name__)

class MonitoredScraper:
    """Scraper with monitoring."""
    
    def __init__(self):
        self.stats = {
            "total_requests": 0,
            "successful_requests": 0,
            "failed_requests": 0,
            "total_time": 0.0
        }
    
    def scrape(self, url):
        """Scrape with monitoring."""
        start_time = datetime.now()
        self.stats["total_requests"] += 1
        
        logger.info(f"Starting scrape: {url}")
        
        try:
            client = PulsarClient()
            client.create_session()
            session = PulsarSession(client)
            
            try:
                page = session.load(url)
                driver = session.driver
                driver.navigate_to(page.url)
                
                data = driver.extract({"title": "h1"})
                
                # Success
                self.stats["successful_requests"] += 1
                elapsed = (datetime.now() - start_time).total_seconds()
                self.stats["total_time"] += elapsed
                
                logger.info(f"Completed scrape: {url} ({elapsed:.2f}s)")
                
                return data
                
            finally:
                session.close()
                client.close()
                
        except Exception as e:
            self.stats["failed_requests"] += 1
            logger.error(f"Failed scrape: {url} - {e}")
            raise
    
    def get_stats(self):
        """Get scraping statistics."""
        avg_time = (
            self.stats["total_time"] / self.stats["successful_requests"]
            if self.stats["successful_requests"] > 0 else 0
        )
        
        return {
            **self.stats,
            "success_rate": (
                self.stats["successful_requests"] / self.stats["total_requests"]
                if self.stats["total_requests"] > 0 else 0
            ),
            "avg_time": avg_time
        }

# Usage
scraper = MonitoredScraper()

urls = ["https://example.com/page1", "https://example.com/page2"]

for url in urls:
    try:
        scraper.scrape(url)
    except:
        pass

# Print statistics
stats = scraper.get_stats()
logger.info(f"Statistics: {stats}")
```

### Deployment Checklist

```python
"""
Production Deployment Checklist:

1. Configuration
   ☐ Environment variables set
   ☐ Connection pooling configured
   ☐ Timeout values appropriate
   ☐ Rate limiting in place

2. Error Handling
   ☐ Retry logic implemented
   ☐ Circuit breaker configured
   ☐ Fallback strategies defined
   ☐ Error logging comprehensive

3. Monitoring
   ☐ Logging configured
   ☐ Metrics collection in place
   ☐ Alerts configured
   ☐ Health checks implemented

4. Performance
   ☐ Caching strategy defined
   ☐ Session reuse optimized
   ☐ Resource limits set
   ☐ Load testing completed

5. Security
   ☐ Credentials secured
   ☐ API keys in environment
   ☐ HTTPS enforced
   ☐ Input validation in place

6. Maintenance
   ☐ Graceful shutdown implemented
   ☐ Backup strategy defined
   ☐ Update procedure documented
   ☐ Rollback plan ready
"""
```

---

## Summary

Advanced patterns covered:

1. **Session Management**: Reuse, pooling, reconnection
2. **Authentication**: Custom headers, token refresh
3. **Timeouts**: Per-operation and global configuration
4. **Hybrid Approach**: Combine WebDriver and AI effectively
5. **Workflows**: Multi-step, stateful processes
6. **Error Recovery**: Retry, circuit breaker, fallback
7. **Testing**: Unit and integration tests
8. **Performance**: Pooling, caching, optimization
9. **Production**: Configuration, monitoring, deployment

You're now ready to build production-grade scrapers! 🚀
