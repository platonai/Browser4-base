# PulsarSession API Reference

The `PulsarSession` class provides session management for page loading, parsing, and data extraction. It offers high-level methods for working with web pages.

## Class: PulsarSession

```python
from pulsar_sdk import PulsarSession
```

### Constructor

```python
PulsarSession(client: PulsarClient)
```

**Parameters:**

- `client` (PulsarClient): An initialized PulsarClient instance with an active session

**Example:**

```python
from pulsar_sdk import PulsarClient, PulsarSession

client = PulsarClient()
client.create_session()
session = PulsarSession(client)
```

## Page Loading

### open()

Open a URL immediately, bypassing cache.

```python
open(url: str) -> WebPage
```

**Parameters:**

- `url` (str): The URL to open

**Returns:**

- `WebPage`: The loaded page object

**Example:**

```python
page = session.open("https://example.com")
print(f"Opened: {page.url}")
print(f"Content type: {page.content_type}")
```

### load()

Load a URL, using cache if available.

```python
load(url: str, args: str = "") -> WebPage
```

**Parameters:**

- `url` (str): The URL to load
- `args` (str): Load arguments (e.g., `"-expire 1d"`). Default: `""`

**Returns:**

- `WebPage`: The loaded page object

**Load Arguments:**

- `-expire <duration>`: Cache expiration (e.g., `1h`, `1d`, `7d`)
- `-refresh`: Force refresh, ignore cache
- `-persist`: Persist the page in storage
- `-ignoreFailure`: Continue even if the page fails to load

**Example:**

```python
# Load with 1-hour cache
page = session.load("https://example.com", args="-expire 1h")

# Force refresh
page = session.load("https://example.com", args="-refresh")

# Combine multiple arguments
page = session.load("https://example.com", args="-expire 1d -persist")
```

### submit()

Submit a URL for asynchronous background processing.

```python
submit(url: str, args: str = "") -> None
```

**Parameters:**

- `url` (str): The URL to submit
- `args` (str): Load arguments. Default: `""`

**Example:**

```python
# Submit multiple URLs for batch processing
urls = [
    "https://example.com/page1",
    "https://example.com/page2",
    "https://example.com/page3"
]

for url in urls:
    session.submit(url, args="-expire 1d")

print("URLs submitted for processing")
```

### normalize()

Normalize a URL with load arguments.

```python
normalize(url: str, args: str = "") -> NormURL
```

**Parameters:**

- `url` (str): The URL to normalize
- `args` (str): Load arguments. Default: `""`

**Returns:**

- `NormURL`: Normalized URL object

**Example:**

```python
norm = session.normalize("https://example.com", args="-expire 1d")
print(f"Normalized spec: {norm.spec}")
print(f"URL: {norm.url}")
print(f"Args: {norm.args}")
```

## Data Parsing and Extraction

### parse()

Parse an HTML page into a document structure.

```python
parse(page: WebPage) -> PageSnapshot
```

**Parameters:**

- `page` (WebPage): The page to parse

**Returns:**

- `PageSnapshot`: Parsed document structure

**Example:**

```python
page = session.open("https://example.com")
document = session.parse(page)
print(f"Document parsed: {document.url}")
```

### extract()

Extract fields from a page using CSS selectors.

```python
extract(
    page_or_doc: Union[WebPage, PageSnapshot],
    fields: Dict[str, str]
) -> FieldsExtraction
```

**Parameters:**

- `page_or_doc` (Union[WebPage, PageSnapshot]): Page or document to extract from
- `fields` (Dict[str, str]): Mapping of field names to CSS selectors

**Returns:**

- `FieldsExtraction`: Extracted field values

**Example:**

```python
page = session.open("https://example.com")

# Extract single values
fields = session.extract(page, {
    "title": "h1",
    "description": "meta[name='description']",
    "author": ".author-name"
})

print(f"Title: {fields['title']}")
print(f"Author: {fields['author']}")

# Extract multiple values (returns lists)
fields = session.extract(page, {
    "headings": "h2",
    "links": "a[href]",
    "images": "img[src]"
})

print(f"Found {len(fields['headings'])} headings")
print(f"Found {len(fields['links'])} links")
```

### scrape()

Load a page and extract data in one operation.

```python
scrape(
    url: str,
    args: str = "",
    fields: Optional[Dict[str, str]] = None
) -> FieldsExtraction
```

**Parameters:**

- `url` (str): The URL to scrape
- `args` (str): Load arguments. Default: `""`
- `fields` (Optional[Dict[str, str]]): CSS selectors for extraction. Default: `None`

**Returns:**

- `FieldsExtraction`: Extracted field values

**Example:**

```python
# One-line scraping
data = session.scrape(
    url="https://example.com",
    args="-expire 1h",
    fields={
        "title": "h1",
        "price": ".price",
        "description": ".description"
    }
)

print(f"Product: {data['title']}")
print(f"Price: {data['price']}")
```

## Session Management

### close()

Close the session and clean up resources.

```python
close() -> None
```

**Example:**

```python
session = PulsarSession(client)
try:
    page = session.open("https://example.com")
    # ... do work ...
finally:
    session.close()
```

## Properties

### client

Access the underlying PulsarClient.

```python
session.client  # PulsarClient instance
```

### driver

Get a WebDriver instance for this session.

```python
driver = session.driver
driver.navigate_to("https://example.com")
```

## Working with WebPage

The `WebPage` object returned by `open()` and `load()` contains:

```python
page = session.open("https://example.com")

# Properties
page.url           # Original URL
page.location      # Final URL after redirects
page.content_type  # MIME type (e.g., "text/html")
page.content_length  # Content length in bytes
page.protocol_status  # HTTP status
page.is_nil        # True if page is invalid/not found
page.html          # Raw HTML content (if available)
```

## Working with NormURL

The `NormURL` object contains normalized URL information:

```python
norm = session.normalize("https://example.com", args="-expire 1d")

norm.spec    # Full normalized specification
norm.url     # Normalized URL
norm.args    # Parsed arguments dict
norm.is_nil  # True if URL is invalid
```

## CSS Selector Tips

The `extract()` and `scrape()` methods use CSS selectors:

```python
fields = {
    # Element text
    "title": "h1",
    
    # Attribute values
    "link": "a[href]",  # Gets href attribute
    "image": "img[src]",  # Gets src attribute
    
    # Complex selectors
    "price": ".product .price",
    "author": "div.author > span.name",
    
    # Multiple elements (returns list)
    "items": "li.item",
    "links": "a.external-link"
}
```

## Complete Example

```python
from pulsar_sdk import PulsarClient, PulsarSession

# Setup
client = PulsarClient(base_url="http://localhost:8182")
client.create_session()
session = PulsarSession(client)

try:
    # Method 1: Open and extract separately
    page = session.open("https://news.ycombinator.com")
    document = session.parse(page)
    
    articles = session.extract(document, {
        "title": ".titleline > a",
        "score": ".score"
    })
    
    print(f"Found {len(articles['title'])} articles")
    
    # Method 2: Scrape in one call
    data = session.scrape(
        url="https://example.com/products",
        args="-expire 1h",
        fields={
            "name": ".product-name",
            "price": ".product-price",
            "rating": ".product-rating"
        }
    )
    
    print(f"Products: {data}")
    
    # Method 3: Load with caching
    page = session.load("https://example.com", args="-expire 1d")
    if not page.is_nil:
        print(f"Loaded from cache: {page.url}")
    
finally:
    session.close()
```

## Batch Processing

Submit multiple URLs for background processing:

```python
# Submit URLs
urls = [f"https://example.com/page{i}" for i in range(1, 101)]
for url in urls:
    session.submit(url, args="-expire 1d")

print(f"Submitted {len(urls)} URLs for processing")

# Later, load the processed pages from cache
for url in urls:
    page = session.load(url)
    if not page.is_nil:
        data = session.extract(page, {"title": "h1"})
        print(f"Title: {data['title']}")
```

## Error Handling

```python
try:
    page = session.open("https://invalid-url.example")
    if page.is_nil:
        print("Page not found or failed to load")
    else:
        data = session.extract(page, {"title": "h1"})
except Exception as e:
    print(f"Error: {e}")
```

## See Also

- [PulsarClient API](client.md) - Low-level HTTP client
- [AgenticSession API](agentic-session.md) - AI-powered features
- [WebDriver API](webdriver.md) - Browser control
- [Data Models](models.md) - WebPage, NormURL, etc.

---

[← Back to PulsarClient](client.md) | [Next: AgenticSession →](agentic-session.md)
