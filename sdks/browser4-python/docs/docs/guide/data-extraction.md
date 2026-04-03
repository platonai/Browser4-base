# Data Extraction

Extract structured data from web pages using CSS selectors, WebDriver methods, and parsing capabilities. Browser4 provides multiple approaches for efficient data extraction.

## Extraction Methods

Browser4 offers several ways to extract data:

1. **CSS Selectors** - Simple, efficient extraction with `extract()`
2. **WebDriver Methods** - Low-level text and attribute extraction
3. **Scrape Method** - Combined load, parse, and extract in one call
4. **AI Extraction** - Intelligent extraction with natural language

## Basic Extraction

### Extract with CSS Selectors

```python
from browser4 import PulsarClient, PulsarSession

client = PulsarClient(base_url="http://localhost:8182")
session_id = client.create_session()
session = PulsarSession(client)

# Load and parse page
page = session.open("https://example.com")
document = session.parse(page)

# Extract single field
fields = session.extract(document, {
    "title": "h1"
})
print(f"Title: {fields['title']}")

# Extract multiple fields
fields = session.extract(document, {
    "title": "h1",
    "description": "p.description",
    "author": ".author-name",
    "date": "time.published"
})

for key, value in fields.items():
    print(f"{key}: {value}")
```

### Extract with WebDriver

```python
from browser4 import PulsarClient, WebDriver

client = PulsarClient(base_url="http://localhost:8182")
session_id = client.create_session()
driver = WebDriver(client)

# Navigate to page
driver.navigate_to("https://example.com")

# Extract single text
title = driver.select_first_text_or_null("h1")
print(f"Title: {title}")

# Extract multiple texts
paragraphs = driver.select_text_all("p")
print(f"Found {len(paragraphs)} paragraphs")
for i, p in enumerate(paragraphs, 1):
    print(f"  {i}. {p[:100]}...")

# Extract attribute
link = driver.select_first_attribute_or_null("a.main-link", "href")
print(f"Link: {link}")

# Extract multiple attributes
links = driver.select_attribute_all("a", "href")
print(f"Found {len(links)} links")
```

## WebDriver Extraction Methods

### Text Extraction

```python
driver = WebDriver(client)
driver.navigate_to("https://example.com")

# Select first text (returns None if not found)
title = driver.select_first_text_or_null("h1.title")
if title:
    print(f"Title: {title}")

# Select all matching texts
items = driver.select_text_all("li.item")
print(f"Items: {len(items)}")
for item in items:
    print(f"  - {item}")

# Get text from first element only
first_paragraph = driver.select_first_text_or_null("p")
print(f"First paragraph: {first_paragraph}")
```

### Attribute Extraction

```python
# Extract single attribute
logo_src = driver.select_first_attribute_or_null("img.logo", "src")
print(f"Logo: {logo_src}")

link_href = driver.select_first_attribute_or_null("a.download", "href")
print(f"Download link: {link_href}")

# Extract multiple attributes
all_images = driver.select_attribute_all("img", "src")
print(f"Found {len(all_images)} images")
for img in all_images:
    print(f"  - {img}")

# Extract data attributes
product_id = driver.select_first_attribute_or_null(".product", "data-id")
product_price = driver.select_first_attribute_or_null(".product", "data-price")
print(f"Product: {product_id}, Price: {product_price}")
```

### Multi-Field Extraction

```python
# Extract multiple fields at once
fields = driver.extract({
    "title": "h1.product-title",
    "price": ".price",
    "description": ".product-description",
    "rating": ".rating-value",
    "reviews": ".review-count"
})

print("Product Information:")
for key, value in fields.items():
    print(f"  {key}: {value}")
```

## Scrape Method

### One-Call Extraction

The `scrape()` method combines loading, parsing, and extraction:

```python
session = PulsarSession(client)

# Load, parse, and extract in one call
data = session.scrape(
    "https://example.com",
    "-expire 1d",
    {
        "title": "h1",
        "content": ".main-content",
        "author": ".author"
    }
)

print("Scraped data:")
for key, value in data.items():
    print(f"  {key}: {value[:100] if value else 'None'}...")
```

### Batch Scraping

```python
def scrape_multiple_pages(session, urls):
    """Scrape data from multiple pages."""
    
    results = []
    fields_to_extract = {
        "title": "h1",
        "description": "p.description",
        "author": ".author-name"
    }
    
    for url in urls:
        print(f"Scraping: {url}")
        data = session.scrape(url, "-expire 1d", fields_to_extract)
        results.append({
            "url": url,
            "data": data
        })
    
    return results

# Usage
urls = [
    "https://example.com/page1",
    "https://example.com/page2",
    "https://example.com/page3"
]
results = scrape_multiple_pages(session, urls)

# Print results
for result in results:
    print(f"\n{result['url']}:")
    for key, value in result['data'].items():
        print(f"  {key}: {value}")
```

## CSS Selectors

### Basic Selectors

```python
# Element selector
data = session.extract(document, {"content": "p"})

# Class selector
data = session.extract(document, {"price": ".price"})

# ID selector
data = session.extract(document, {"header": "#header"})

# Attribute selector
data = session.extract(document, {"links": "a[href]"})
```

### Advanced Selectors

```python
# Descendant selector
data = session.extract(document, {
    "nav_links": "nav ul li a"
})

# Child selector
data = session.extract(document, {
    "direct_children": "div.container > p"
})

# Pseudo-class selectors
data = session.extract(document, {
    "first_item": "li:first-child",
    "last_item": "li:last-child",
    "even_rows": "tr:nth-child(even)"
})

# Multiple conditions
data = session.extract(document, {
    "external_links": "a[href^='http']:not([href*='example.com'])"
})
```

### Complex Extraction

```python
# Extract nested data
fields = {
    "product_name": "h1.product-title",
    "product_price": ".price-box .current-price",
    "old_price": ".price-box .old-price",
    "availability": ".stock-status",
    "description": ".product-description p",
    "images": "img.product-image",
    "reviews_count": ".reviews-summary .count",
    "rating": ".rating-stars[data-rating]"
}

data = session.scrape("https://example.com/product/123", "-expire 1h", fields)
```

## Parsing and BeautifulSoup

### Local Parsing

```python
from bs4 import BeautifulSoup

session = PulsarSession(client)

# Load page
page = session.open("https://example.com")

# Parse with Browser4
document = session.parse(page)

# Or parse HTML directly with BeautifulSoup
if hasattr(page, 'content'):
    soup = BeautifulSoup(page.content, 'html.parser')
    
    # Use BeautifulSoup methods
    title = soup.find('h1').text if soup.find('h1') else None
    paragraphs = [p.text for p in soup.find_all('p')]
    
    print(f"Title: {title}")
    print(f"Paragraphs: {len(paragraphs)}")
```

### Combined Approach

```python
def extract_with_fallback(session, url):
    """Extract data with BeautifulSoup fallback."""
    
    # Load page
    page = session.open(url)
    
    # Try Browser4 extraction first
    try:
        document = session.parse(page)
        data = session.extract(document, {
            "title": "h1",
            "content": ".main-content"
        })
        return data
    except Exception as e:
        print(f"Browser4 extraction failed: {e}")
        
        # Fallback to BeautifulSoup
        if hasattr(page, 'content'):
            soup = BeautifulSoup(page.content, 'html.parser')
            return {
                "title": soup.find('h1').text if soup.find('h1') else None,
                "content": soup.find(class_='main-content').text if soup.find(class_='main-content') else None
            }
    
    return {}

# Usage
data = extract_with_fallback(session, "https://example.com")
```

## Advanced Extraction Patterns

### Structured Data Extraction

```python
def extract_product_data(session, url):
    """Extract structured product information."""
    
    page = session.open(url)
    document = session.parse(page)
    
    # Define extraction schema
    product_fields = {
        "name": "h1.product-name",
        "sku": "[data-sku]",
        "price": ".price-current",
        "currency": ".price-currency",
        "availability": ".stock-status",
        "brand": ".brand-name",
        "category": ".breadcrumb li:last-child",
        "rating": "[itemprop='ratingValue']",
        "review_count": "[itemprop='reviewCount']",
        "description": ".product-description",
        "features": ".features li",
        "image": "img.main-image"
    }
    
    product = session.extract(document, product_fields)
    
    # Post-process data
    if product.get('price'):
        product['price'] = float(product['price'].replace('$', '').replace(',', ''))
    
    if product.get('rating'):
        product['rating'] = float(product['rating'])
    
    return product

# Usage
product = extract_product_data(session, "https://example.com/product/123")
print(f"Product: {product['name']}")
print(f"Price: ${product['price']}")
```

### List Extraction Pattern

```python
def extract_list_items(driver, container_selector, item_fields):
    """Extract data from a list of items."""
    
    # Get number of items
    driver.wait_for_selector(container_selector, timeout=10000)
    items_count = driver.execute_script(f"""
        return document.querySelectorAll('{container_selector}').length;
    """)
    
    print(f"Found {items_count} items")
    
    results = []
    for i in range(items_count):
        item_data = {}
        for field_name, field_selector in item_fields.items():
            # Build selector for specific item
            full_selector = f"{container_selector}:nth-child({i+1}) {field_selector}"
            value = driver.select_first_text_or_null(full_selector)
            item_data[field_name] = value
        
        results.append(item_data)
    
    return results

# Usage
search_results = extract_list_items(
    driver,
    ".search-result",
    {
        "title": ".result-title",
        "description": ".result-description",
        "link": ".result-link"
    }
)

for i, result in enumerate(search_results, 1):
    print(f"{i}. {result['title']}")
```

### Pagination Extraction

```python
def extract_all_pages(driver, max_pages=10):
    """Extract data from paginated results."""
    
    all_data = []
    
    for page_num in range(1, max_pages + 1):
        print(f"Extracting page {page_num}...")
        
        # Extract data from current page
        items = driver.select_text_all(".item-title")
        all_data.extend(items)
        
        # Check for next page button
        if not driver.exists("button.next-page"):
            print("No more pages")
            break
        
        # Click next page
        driver.click("button.next-page")
        driver.wait_for_selector(".item-title", timeout=10000)
        driver.delay(1000)  # Wait for page to stabilize
    
    print(f"Extracted {len(all_data)} total items")
    return all_data

# Usage
driver.navigate_to("https://example.com/search?q=python")
all_items = extract_all_pages(driver, max_pages=5)
```

## Complete Extraction Example

```python
from browser4 import Browser4Driver, PulsarClient, AgenticSession

def comprehensive_extraction_demo():
    """Comprehensive data extraction demonstration."""
    
    with Browser4Driver() as driver_mgr:
        client = PulsarClient(base_url=driver_mgr.base_url)
        session_id = client.create_session()
        session = AgenticSession(client)
        
        try:
            # 1. Simple scrape
            print("1. Simple scrape:")
            data = session.scrape(
                "https://example.com",
                "-expire 1d",
                {"title": "h1", "content": "p"}
            )
            print(f"   Title: {data.get('title')}")
            
            # 2. Load and extract separately
            print("\n2. Load and extract separately:")
            page = session.open("https://example.com")
            document = session.parse(page)
            fields = session.extract(document, {
                "heading": "h1",
                "paragraphs": "p",
                "links": "a[href]"
            })
            print(f"   Found {len(fields)} fields")
            
            # 3. WebDriver extraction
            print("\n3. WebDriver extraction:")
            driver = session.driver
            driver.navigate_to("https://example.com")
            
            # Text extraction
            title = driver.select_first_text_or_null("h1")
            paragraphs = driver.select_text_all("p")
            print(f"   Title: {title}")
            print(f"   Paragraphs: {len(paragraphs)}")
            
            # Attribute extraction
            links = driver.select_attribute_all("a", "href")
            print(f"   Links: {len(links)}")
            
            # Multi-field extraction
            fields = driver.extract({
                "title": "h1",
                "description": "p:first-of-type",
                "links_count": "a"
            })
            print(f"   Extracted fields: {fields.keys()}")
            
            # 4. Structured extraction
            print("\n4. Structured extraction:")
            structured_data = {
                "header": {
                    "title": driver.select_first_text_or_null("h1"),
                    "subtitle": driver.select_first_text_or_null("h2")
                },
                "content": {
                    "paragraphs": driver.select_text_all("p"),
                    "images": driver.select_attribute_all("img", "src")
                },
                "metadata": {
                    "url": driver.current_url(),
                    "title": driver.title()
                }
            }
            print(f"   Structured data keys: {structured_data.keys()}")
            
        finally:
            session.close()
            client.close()

if __name__ == "__main__":
    comprehensive_extraction_demo()
```

## AI-Powered Extraction

### Natural Language Extraction

```python
from browser4 import AgenticSession

session = AgenticSession(client)
session.open("https://example.com/products")

# Use AI to extract data
extraction = session.agent_extract(
    instruction="Extract all product names and their prices",
    schema={
        "type": "array",
        "items": {
            "type": "object",
            "properties": {
                "name": {"type": "string"},
                "price": {"type": "number"}
            }
        }
    }
)

print(f"Extraction success: {extraction.success}")
print(f"Extracted data: {extraction.data}")
```

### Intelligent Extraction

```python
# Extract complex data with AI
session.open("https://example.com/article")

extraction = session.agent_extract(
    instruction="Extract the article's main points, author info, and publication date",
    schema={
        "type": "object",
        "properties": {
            "main_points": {
                "type": "array",
                "items": {"type": "string"}
            },
            "author": {
                "type": "object",
                "properties": {
                    "name": {"type": "string"},
                    "bio": {"type": "string"}
                }
            },
            "published": {"type": "string"}
        }
    }
)

if extraction.success:
    data = extraction.data
    print(f"Main points: {len(data['main_points'])}")
    print(f"Author: {data['author']['name']}")
    print(f"Published: {data['published']}")
```

## Troubleshooting

### Element Not Found

```python
# Check if element exists first
if driver.exists("h1.title"):
    title = driver.select_first_text_or_null("h1.title")
else:
    print("Title element not found")
    title = None

# Use wait_for_selector
driver.wait_for_selector("h1.title", timeout=10000)
title = driver.select_first_text_or_null("h1.title")
```

### Empty Extraction Results

```python
# Verify page loaded
page = session.open("https://example.com")
if page.is_nil:
    print("Page failed to load")
else:
    document = session.parse(page)
    if document:
        data = session.extract(document, {"title": "h1"})
        if not data.get('title'):
            print("Title not found in page")
```

### Dynamic Content Not Loaded

```python
# Wait for dynamic content
driver.navigate_to("https://example.com")
driver.wait_for_selector(".dynamic-content", timeout=15000)

# Verify content loaded before extracting
if driver.exists(".dynamic-content"):
    content = driver.select_first_text_or_null(".dynamic-content")
else:
    print("Dynamic content did not load")
```

### Selector Too Generic

```python
# Instead of generic selector
# paragraphs = driver.select_text_all("p")  # Gets all <p> tags

# Use specific selector
paragraphs = driver.select_text_all("article.main p")  # Only article paragraphs
paragraphs = driver.select_text_all("div.content > p")  # Direct children only
```

## Best Practices

1. **Use specific selectors** - Target exactly what you need
2. **Validate extraction results** - Check for None/empty values
3. **Handle dynamic content** - Wait for elements to load
4. **Structure your data** - Organize extracted data logically
5. **Cache when possible** - Use load arguments for caching
6. **Test selectors** - Verify selectors work on target pages
7. **Handle errors gracefully** - Use try/except and fallbacks
8. **Post-process data** - Clean and normalize extracted values
9. **Use appropriate method** - Choose scrape() vs extract() vs WebDriver methods
10. **Document extraction schema** - Comment complex selector structures

## Performance Tips

### Batch Extraction

```python
# Extract all fields at once (efficient)
fields = driver.extract({
    "title": "h1",
    "price": ".price",
    "description": ".desc",
    "rating": ".rating"
})

# Instead of multiple separate calls (inefficient)
# title = driver.select_first_text_or_null("h1")
# price = driver.select_first_text_or_null(".price")
# description = driver.select_first_text_or_null(".desc")
# rating = driver.select_first_text_or_null(".rating")
```

### Minimize Page Loads

```python
# Use cache for multiple extractions
page = session.load("https://example.com", args="-expire 1h")
document = session.parse(page)

# Multiple extractions from same page
data1 = session.extract(document, {"title": "h1"})
data2 = session.extract(document, {"links": "a"})
data3 = session.extract(document, {"images": "img"})
```

### Selective Extraction

```python
# Extract only what you need
data = session.scrape(
    "https://example.com",
    "-expire 1d",
    {
        "title": "h1",  # Only 2 fields
        "price": ".price"
    }
)

# Avoid extracting everything
# data = driver.page_source()  # Downloads entire HTML
```

## Next Steps

- **[Element Interaction](element-interaction.md)** - Interact with elements before extraction
- **[AI Automation](ai-automation.md)** - Use AI for intelligent extraction
- **[Navigation](navigation.md)** - Navigate to pages for extraction
- **[Screenshots](screenshots.md)** - Visual verification of extraction
