# PulsarSession

::: browser4.agentic_session.PulsarSession

## Overview

`PulsarSession` provides methods for loading pages from storage or the internet, parsing them, and extracting data. It mirrors the Kotlin PulsarSession interface, providing a consistent API across languages for web scraping and data extraction tasks.

## Key Features

- **Page Loading**: Load pages with intelligent caching and expiration policies
- **URL Normalization**: Parse and normalize URLs with load arguments
- **HTML Parsing**: Parse pages using BeautifulSoup
- **Data Extraction**: Extract data using CSS selectors
- **Batch Operations**: Load and submit multiple URLs
- **WebDriver Integration**: Access low-level browser control via bound driver
- **LLM Chat**: Direct access to chat/LLM capabilities

## Class Definition

```python
class PulsarSession:
    """
    PulsarSession provides methods for loading pages from storage or internet,
    parsing them, and extracting data.

    Args:
        client: PulsarClient instance for API communication
    """
```

## Constructor

### `__init__`

```python
def __init__(self, client: PulsarClient)
```

**Parameters:**

- **client** (`PulsarClient`): PulsarClient instance for server communication

**Example:**

```python
from browser4 import PulsarClient, PulsarSession

client = PulsarClient()
client.create_session()
session = PulsarSession(client)
```

## Properties

### `id`

```python
@property
def id(self) -> int
```

Get the numeric session ID.

**Returns:** Session ID as integer

### `uuid`

```python
@property
def uuid(self) -> str
```

Get the session UUID string.

**Returns:** Session UUID

**Example:**

```python
session = PulsarSession(client)
print(f"Session UUID: {session.uuid}")
```

### `display`

```python
@property
def display(self) -> str
```

Get a short descriptive display text for the session.

**Returns:** Display string (e.g., `"PulsarSession(abc12345...)"`)

### `is_active`

```python
@property
def is_active(self) -> bool
```

Check if the session is active.

**Returns:** `True` if session has a valid session_id

### `driver`

```python
@property
def driver(self) -> WebDriver
```

Get the bound WebDriver instance (creates one if not exists).

**Returns:** WebDriver instance

**Example:**

```python
session = PulsarSession(client)
driver = session.driver
driver.navigate_to("https://example.com")
```

### `bound_driver`

```python
@property
def bound_driver(self) -> Optional[WebDriver]
```

Get the bound driver or `None` if not bound yet.

**Returns:** WebDriver or `None`

## URL Normalization

### `normalize`

```python
def normalize(
    self,
    url: str,
    args: Optional[str] = None,
    to_item_option: bool = False
) -> NormURL
```

Normalize a URL with optional load arguments.

**Parameters:**

- **url** (`str`): URL to normalize
- **args** (`Optional[str]`): Load arguments (e.g., `"-expire 1d -refresh"`)
- **to_item_option** (`bool`): Convert to item load options

**Returns:** `NormURL` object with normalized URL and parsed arguments

**Example:**

```python
session = PulsarSession(client)

# Simple normalization
norm = session.normalize("https://example.com")
print(norm.url)

# With load arguments
norm = session.normalize(
    "https://example.com",
    args="-expire 1d -refresh"
)
print(f"URL: {norm.url}, Args: {norm.args}")
```

### `normalize_or_null`

```python
def normalize_or_null(
    self,
    url: Optional[str],
    args: Optional[str] = None,
    to_item_option: bool = False
) -> Optional[NormURL]
```

Normalize a URL, returning `None` if invalid.

**Parameters:**

- **url** (`Optional[str]`): URL to normalize (can be None)
- **args** (`Optional[str]`): Load arguments
- **to_item_option** (`bool`): Convert to item options

**Returns:** `NormURL` or `None` if invalid

**Example:**

```python
norm = session.normalize_or_null("https://example.com")
if norm:
    print(f"Valid URL: {norm.url}")
else:
    print("Invalid URL")
```

## Page Loading

### `open`

```python
def open(self, url: str, args: Optional[str] = None) -> WebPage
```

Open a URL immediately, bypassing local cache. This method always fetches fresh content from the web.

**Parameters:**

- **url** (`str`): URL to open
- **args** (`Optional[str]`): Load arguments (e.g., `"-parse"`, `"-expires 1d"`)

**Returns:** `WebPage` with loaded page information

**Example:**

```python
session = PulsarSession(client)

# Simple open
page = session.open("https://example.com")
print(f"Loaded: {page.url}")

# With arguments
page = session.open(
    "https://example.com",
    args="-parse -expires 1d"
)
print(f"HTML length: {len(page.html)}")
```

### `load`

```python
def load(self, url: str, args: Optional[str] = None) -> WebPage
```

Load a URL from local storage or fetch from internet. Checks cache first based on expiration policy.

**Parameters:**

- **url** (`str`): URL to load
- **args** (`Optional[str]`): Load arguments
  - `"-expire 1d"` - Page expires after 1 day
  - `"-refresh"` - Force refresh
  - `"-parse"` - Activate parsing subsystem
  - `"-outLink a[href]"` - Extract links

**Returns:** `WebPage` with loaded page information

**Example:**

```python
session = PulsarSession(client)

# Load with caching
page = session.load("https://example.com", "-expire 1d")

# Force refresh
page = session.load("https://example.com", "-refresh")

# Load and parse
page = session.load("https://example.com", "-expire 1d -parse")
```

### `load_all`

```python
def load_all(
    self,
    urls: Iterable[str],
    args: Optional[str] = None
) -> List[WebPage]
```

Load multiple URLs with the same arguments.

**Parameters:**

- **urls** (`Iterable[str]`): URLs to load
- **args** (`Optional[str]`): Load arguments applied to all URLs

**Returns:** List of `WebPage` objects

**Example:**

```python
session = PulsarSession(client)

urls = [
    "https://example.com/page1",
    "https://example.com/page2",
    "https://example.com/page3"
]

pages = session.load_all(urls, args="-expire 1d -parse")
for page in pages:
    print(f"Loaded: {page.url}")
```

### `submit`

```python
def submit(self, url: str, args: Optional[str] = None) -> bool
```

Submit a URL to the crawl pool for asynchronous processing. This is non-blocking.

**Parameters:**

- **url** (`str`): URL to submit
- **args** (`Optional[str]`): Load arguments

**Returns:** `True` if submitted successfully

**Example:**

```python
session = PulsarSession(client)

# Submit single URL
success = session.submit("https://example.com", "-expire 1d")

# Submit for background processing
session.submit("https://example.com/large-page", "-parse")
```

### `submit_all`

```python
def submit_all(self, urls: Iterable[str], args: Optional[str] = None) -> bool
```

Submit multiple URLs to the crawl pool.

**Parameters:**

- **urls** (`Iterable[str]`): URLs to submit
- **args** (`Optional[str]`): Load arguments applied to all

**Returns:** `True` if all URLs were submitted successfully

**Example:**

```python
session = PulsarSession(client)

urls = ["https://example.com/page1", "https://example.com/page2"]
success = session.submit_all(urls, args="-expire 1d")
```

## Parsing and Extraction

### `parse`

```python
def parse(self, page: WebPage) -> Any
```

Parse a WebPage into a BeautifulSoup document for DOM querying.

**Parameters:**

- **page** (`WebPage`): Page to parse

**Returns:** BeautifulSoup Document object, or raw HTML if BeautifulSoup not available, or `None` if no HTML

**Example:**

```python
session = PulsarSession(client)

page = session.load("https://example.com", "-parse")
document = session.parse(page)

if document:
    # Use BeautifulSoup API
    title = document.select_one("h1")
    print(f"Title: {title.text}")
```

### `extract`

```python
def extract(
    self,
    document: Any,
    field_selectors: Union[Mapping[str, str], Iterable[str]]
) -> Dict[str, Optional[str]]
```

Extract fields from a document using CSS selectors.

**Parameters:**

- **document** (`Any`): BeautifulSoup document (from `parse()`) or WebPage
- **field_selectors** (`Union[Mapping[str, str], Iterable[str]]`):
  - Dict mapping field names to CSS selectors
  - Or iterable of selectors (selector becomes field name)

**Returns:** Dictionary mapping field names to extracted values

**Example:**

```python
session = PulsarSession(client)

page = session.load("https://example.com", "-parse")
document = session.parse(page)

# Extract with field mapping
fields = session.extract(document, {
    "title": "h1",
    "description": "p.description",
    "price": "span.price"
})
print(fields)  # {"title": "...", "description": "...", "price": "..."}

# Extract with selector list
fields = session.extract(document, ["h1", "p.description"])
print(fields)  # {"h1": "...", "p.description": "..."}
```

### `scrape`

```python
def scrape(
    self,
    url: str,
    args: str,
    field_selectors: Union[Mapping[str, str], Iterable[str]]
) -> Dict[str, Optional[str]]
```

Load a page, parse it, and extract fields in one operation.

**Parameters:**

- **url** (`str`): URL to scrape
- **args** (`str`): Load arguments
- **field_selectors** (`Union[Mapping[str, str], Iterable[str]]`): Field selectors

**Returns:** Dictionary mapping field names to extracted values

**Example:**

```python
session = PulsarSession(client)

# One-step scraping
data = session.scrape(
    "https://example.com/product",
    "-expire 1d -parse",
    {
        "title": "h1.product-title",
        "price": "span.price",
        "description": "div.description"
    }
)
print(data)
```

## Chat/LLM Operations

### `chat`

```python
def chat(self, prompt: str, system_message: Optional[str] = None) -> ChatResponse
```

Send a prompt to the LLM and get a response.

**Parameters:**

- **prompt** (`str`): User prompt (or userMessage if system_message provided)
- **system_message** (`Optional[str]`): System instructions for the LLM

**Returns:** `ChatResponse` with the LLM's response

**Example:**

```python
session = PulsarSession(client)

# Simple chat
response = session.chat("What is web scraping?")
print(response.content)

# With system message
response = session.chat(
    prompt="Explain this in simple terms",
    system_message="You are a helpful teacher for beginners"
)
print(response.content)
```

## Driver Management

### `get_or_create_bound_driver`

```python
def get_or_create_bound_driver(self) -> WebDriver
```

Get or create a bound WebDriver instance.

**Returns:** WebDriver instance

**Example:**

```python
session = PulsarSession(client)
driver = session.get_or_create_bound_driver()
driver.navigate_to("https://example.com")
```

### `create_bound_driver`

```python
def create_bound_driver(self) -> WebDriver
```

Create a new bound WebDriver.

**Returns:** New WebDriver instance

### `bind_driver`

```python
def bind_driver(self, driver: WebDriver) -> None
```

Bind a WebDriver to this session.

**Parameters:**

- **driver** (`WebDriver`): WebDriver to bind

**Example:**

```python
from browser4 import WebDriver

session = PulsarSession(client)
driver = WebDriver(client)
session.bind_driver(driver)
```

### `unbind_driver`

```python
def unbind_driver(self, driver: WebDriver) -> None
```

Unbind a WebDriver from this session.

**Parameters:**

- **driver** (`WebDriver`): WebDriver to unbind

## Utility Methods

### `exists`

```python
def exists(self, url: str) -> bool
```

Check if a page exists in storage.

**Parameters:**

- **url** (`str`): URL to check

**Returns:** `True` if page exists (currently returns `False` as placeholder)

### `flush`

```python
def flush(self) -> None
```

Flush pending changes to storage.

### `close`

```python
def close(self) -> None
```

Close the session and release resources.

**Example:**

```python
session = PulsarSession(client)
try:
    page = session.open("https://example.com")
    # Use session...
finally:
    session.close()
```

## Complete Examples

### Basic Page Loading

```python
from browser4 import PulsarClient, PulsarSession

client = PulsarClient()
client.create_session()
session = PulsarSession(client)

# Load a page
page = session.load("https://example.com", "-expire 1d -parse")
print(f"Loaded: {page.url}")
print(f"Content-Type: {page.content_type}")
print(f"HTML length: {len(page.html)}")

session.close()
```

### Scraping Workflow

```python
from browser4 import PulsarClient, PulsarSession

client = PulsarClient()
client.create_session()
session = PulsarSession(client)

# Load page
page = session.load("https://example.com/product", "-expire 1d -parse")

# Parse HTML
document = session.parse(page)

# Extract fields
data = session.extract(document, {
    "title": "h1.product-name",
    "price": "span.price",
    "rating": "div.rating",
    "description": "p.description"
})

print(data)
session.close()
```

### One-Step Scraping

```python
from browser4 import PulsarClient, PulsarSession

client = PulsarClient()
client.create_session()
session = PulsarSession(client)

# Load, parse, and extract in one call
product_data = session.scrape(
    "https://example.com/product",
    "-expire 1d -parse",
    {
        "name": "h1",
        "price": ".price",
        "stock": ".stock-status"
    }
)

print(f"Product: {product_data['name']}")
print(f"Price: {product_data['price']}")
session.close()
```

### Batch Loading

```python
from browser4 import PulsarClient, PulsarSession

client = PulsarClient()
client.create_session()
session = PulsarSession(client)

urls = [
    "https://example.com/page1",
    "https://example.com/page2",
    "https://example.com/page3"
]

# Load all pages
pages = session.load_all(urls, args="-expire 1d -parse")

# Extract from each
for page in pages:
    doc = session.parse(page)
    title = session.extract(doc, {"title": "h1"})
    print(f"{page.url}: {title['title']}")

session.close()
```

### WebDriver Integration

```python
from browser4 import PulsarClient, PulsarSession

client = PulsarClient()
client.create_session()
session = PulsarSession(client)

# Access bound driver
driver = session.driver
driver.navigate_to("https://example.com")
driver.click("button.submit")

# Capture and parse
page = session.load(driver.current_url())
document = session.parse(page)
data = session.extract(document, {"result": "div.result"})

print(data)
session.close()
```

## Load Arguments Reference

Common load arguments you can use with `load()`, `open()`, and `scrape()`:

| Argument | Description | Example |
|----------|-------------|---------|
| `-expire <duration>` | Page expiration time | `-expire 1d`, `-expire 3h` |
| `-refresh` | Force page refresh | `-refresh` |
| `-parse` | Activate parsing subsystem | `-parse` |
| `-outLink <selector>` | Extract outgoing links | `-outLink a[href]` |
| `-requireSize <size>` | Minimum content size | `-requireSize 10k` |
| `-ignoreFailure` | Don't throw on load failure | `-ignoreFailure` |

**Example combinations:**

```python
# Cache for 1 day and parse
page = session.load(url, "-expire 1d -parse")

# Always fetch fresh and extract links
page = session.load(url, "-refresh -outLink a[href]")

# Long expiration with minimum size
page = session.load(url, "-expire 7d -requireSize 10k")
```

## See Also

- [AgenticSession](agentic-session.md) - AI-powered session (extends PulsarSession)
- [WebDriver](webdriver.md) - Low-level browser control
- [WebPage Model](models.md#webpage) - Page data structure
- [NormURL Model](models.md#normUrl) - Normalized URL structure
- [API Overview](overview.md) - Complete API reference
