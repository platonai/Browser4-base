# Data Extraction Guide

This comprehensive guide covers web scraping and data extraction patterns using Browser4 Python SDK. Learn advanced CSS selector strategies, handle complex page structures, process data at scale, and optimize performance.

## Table of Contents

- [CSS Selector Strategies](#css-selector-strategies)
- [Single vs Multiple Elements](#single-vs-multiple-elements)
- [Text vs Attributes](#text-vs-attributes)
- [Complex Page Structures](#complex-page-structures)
- [Handling Pagination](#handling-pagination)
- [Dynamic Content](#dynamic-content)
- [Batch Processing](#batch-processing)
- [Performance Optimization](#performance-optimization)
- [Export Formats](#export-formats)

---

## CSS Selector Strategies

Effective CSS selectors are the foundation of reliable web scraping. Here's how to write robust selectors.

### Basic Selectors

```python
from pulsar_sdk import PulsarClient, PulsarSession

client = PulsarClient()
client.create_session()
session = PulsarSession(client)
driver = session.driver

# Navigate to page
driver.navigate_to("https://example.com")

# Element selectors
title = driver.select_first_text_or_null("h1")              # Tag
title = driver.select_first_text_or_null(".page-title")    # Class
title = driver.select_first_text_or_null("#main-title")    # ID

# Attribute selectors
email = driver.select_first_text_or_null('input[type="email"]')
required = driver.select_first_text_or_null('input[required]')
data_id = driver.select_first_text_or_null('[data-id="123"]')

session.close()
client.close()
```

### Combinators

```python
# Descendant selector (space)
text = driver.select_first_text_or_null("article p")  # Any <p> inside <article>

# Child selector (>)
text = driver.select_first_text_or_null("article > p")  # Direct <p> child of <article>

# Adjacent sibling (+)
text = driver.select_first_text_or_null("h2 + p")  # <p> immediately after <h2>

# General sibling (~)
text = driver.select_first_text_or_null("h2 ~ p")  # Any <p> after <h2>
```

### Pseudo-Classes

```python
# First/last child
first_item = driver.select_first_text_or_null("li:first-child")
last_item = driver.select_first_text_or_null("li:last-child")

# Nth child
second_item = driver.select_first_text_or_null("li:nth-child(2)")
even_items = driver.select_text_all("li:nth-child(even)")
odd_items = driver.select_text_all("li:nth-child(odd)")

# Not selector
non_hidden = driver.select_text_all("div:not(.hidden)")
```

### Robust Selector Strategies

```python
def get_robust_selector(driver, fallback_selectors):
    """Try multiple selectors until one works."""
    for selector in fallback_selectors:
        result = driver.select_first_text_or_null(selector)
        if result:
            return result
    return None

# Usage
title = get_robust_selector(driver, [
    "h1.article-title",           # Most specific
    "h1[itemprop='headline']",    # Semantic
    "article h1",                  # Structural
    "h1"                          # Fallback
])
```

### Advanced Pattern: Data Attributes

```python
# Modern sites use data attributes
product_id = driver.select_first_attribute_or_null(
    "[data-product-id]", 
    "data-product-id"
)

# Extract all tracking IDs
tracking_ids = driver.select_attribute_all(
    "[data-track-id]",
    "data-track-id"
)

# Combine with other selectors
premium_products = driver.select_text_all(
    ".product[data-tier='premium'] .product-name"
)
```

---

## Single vs Multiple Elements

Understanding when to extract single vs multiple elements is crucial for effective scraping.

### Single Element Extraction

```python
from pulsar_sdk import PulsarClient, PulsarSession

client = PulsarClient()
client.create_session()
session = PulsarSession(client)
driver = session.driver

driver.navigate_to("https://blog.example.com/post/123")

# Get first match
title = driver.select_first_text_or_null("h1")
author = driver.select_first_text_or_null(".author-name")
date = driver.select_first_text_or_null("time.published")

# Null-safe: returns None if not found
description = driver.select_first_text_or_null(".description")
if description:
    print(f"Description: {description}")
else:
    print("No description found")

session.close()
client.close()
```

### Multiple Element Extraction

```python
# Get all matches
headings = driver.select_text_all("h2")
links = driver.select_text_all("a.nav-link")
paragraphs = driver.select_text_all("article p")

print(f"Found {len(headings)} headings")
for i, heading in enumerate(headings, 1):
    print(f"  {i}. {heading}")

# Get all with attributes
all_links = driver.select_attribute_all("a", "href")
all_images = driver.select_attribute_all("img", "src")
```

### Combined Extraction Pattern

```python
def extract_article_with_sections(driver, url):
    """Extract article with multiple sections."""
    driver.navigate_to(url)
    
    # Single elements
    article = {
        "title": driver.select_first_text_or_null("h1.article-title"),
        "author": driver.select_first_text_or_null(".author-name"),
        "date": driver.select_first_text_or_null("time[datetime]"),
        "summary": driver.select_first_text_or_null(".article-summary"),
    }
    
    # Multiple elements
    article["sections"] = driver.select_text_all("article h2")
    article["paragraphs"] = driver.select_text_all("article p")
    article["images"] = driver.select_attribute_all("article img", "src")
    article["links"] = driver.select_attribute_all("article a", "href")
    
    return article

# Use it
article = extract_article_with_sections(driver, "https://blog.example.com/post/123")
print(f"Title: {article['title']}")
print(f"Sections: {len(article['sections'])}")
print(f"Paragraphs: {len(article['paragraphs'])}")
```

---

## Text vs Attributes

Different extraction methods for different data types.

### Extracting Text Content

```python
from pulsar_sdk import PulsarClient, PulsarSession

client = PulsarClient()
client.create_session()
session = PulsarSession(client)
driver = session.driver

driver.navigate_to("https://example.com")

# Single text extraction
title = driver.select_first_text_or_null("h1")
description = driver.select_first_text_or_null(".description")

# Multiple text extraction
items = driver.select_text_all(".item-title")
prices = driver.select_text_all(".item-price")

# Text from specific element
article_text = driver.select_first_text_or_null("article.content")

session.close()
client.close()
```

### Extracting Attributes

```python
# Single attribute
first_link = driver.select_first_attribute_or_null("a.main-link", "href")
image_src = driver.select_first_attribute_or_null("img.hero", "src")
alt_text = driver.select_first_attribute_or_null("img.hero", "alt")

# Multiple attributes
all_links = driver.select_attribute_all("a", "href")
all_images = driver.select_attribute_all("img", "src")
all_titles = driver.select_attribute_all("a", "title")

# Data attributes
product_ids = driver.select_attribute_all(".product", "data-product-id")
prices = driver.select_attribute_all(".product", "data-price")
```

### Mixed Extraction

```python
def extract_product_cards(driver):
    """Extract product cards with mixed data."""
    driver.navigate_to("https://shop.example.com/products")
    
    # Get counts first
    titles = driver.select_text_all(".product-card .title")
    count = len(titles)
    
    # Extract all data
    products = []
    for i in range(count):
        # Create selector for each card
        card_selector = f".product-card:nth-child({i+1})"
        
        product = {
            # Text content
            "title": driver.select_first_text_or_null(f"{card_selector} .title"),
            "price": driver.select_first_text_or_null(f"{card_selector} .price"),
            "rating": driver.select_first_text_or_null(f"{card_selector} .rating"),
            
            # Attributes
            "url": driver.select_first_attribute_or_null(f"{card_selector} a", "href"),
            "image": driver.select_first_attribute_or_null(f"{card_selector} img", "src"),
            "product_id": driver.select_first_attribute_or_null(card_selector, "data-product-id"),
        }
        
        products.append(product)
    
    return products

# Use it
products = extract_product_cards(driver)
for product in products:
    print(f"{product['title']}: {product['price']}")
```

### Using extract() for Multi-Field

```python
# Extract multiple fields at once
data = driver.extract({
    # Text fields
    "title": "h1.page-title",
    "author": ".author-name",
    "date": "time.published",
    
    # Will get text by default
    "price": ".product-price",
    "rating": ".star-rating",
    
    # For attributes, need different approach
    "description": ".product-description"
})

print(f"Title: {data['title']}")
print(f"Price: {data['price']}")

# For attributes, still use select_attribute methods
image_url = driver.select_first_attribute_or_null("img.product-image", "src")
link_url = driver.select_first_attribute_or_null("a.product-link", "href")
```

---

## Complex Page Structures

Handling nested elements, tables, lists, and dynamic layouts.

### Nested Elements

```python
def extract_nested_comments(driver, url):
    """Extract nested comment threads."""
    driver.navigate_to(url)
    
    comments = []
    
    # Get top-level comments
    top_level = driver.select_text_all(".comment.level-0 .comment-text")
    authors = driver.select_text_all(".comment.level-0 .comment-author")
    
    for i, (text, author) in enumerate(zip(top_level, authors), 1):
        comment = {
            "author": author,
            "text": text,
            "replies": []
        }
        
        # Get replies for this comment
        reply_selector = f".comment.level-0:nth-child({i}) + .replies .comment"
        replies_text = driver.select_text_all(f"{reply_selector} .comment-text")
        replies_author = driver.select_text_all(f"{reply_selector} .comment-author")
        
        for reply_text, reply_author in zip(replies_text, replies_author):
            comment["replies"].append({
                "author": reply_author,
                "text": reply_text
            })
        
        comments.append(comment)
    
    return comments

# Use it
comments = extract_nested_comments(driver, "https://blog.example.com/post/123")
for comment in comments:
    print(f"{comment['author']}: {comment['text']}")
    for reply in comment['replies']:
        print(f"  └─ {reply['author']}: {reply['text']}")
```

### Table Extraction

```python
def extract_table_data(driver, table_selector):
    """Extract data from HTML table."""
    # Get headers
    headers = driver.select_text_all(f"{table_selector} thead th")
    
    # Get all row cells
    rows_data = []
    
    # Count rows
    row_count = len(driver.select_text_all(f"{table_selector} tbody tr"))
    
    for i in range(1, row_count + 1):
        row_selector = f"{table_selector} tbody tr:nth-child({i})"
        cells = driver.select_text_all(f"{row_selector} td")
        
        # Create dict from headers and cells
        row_dict = {}
        for header, cell in zip(headers, cells):
            row_dict[header] = cell
        
        rows_data.append(row_dict)
    
    return rows_data

# Use it
driver.navigate_to("https://example.com/data-table")
data = extract_table_data(driver, "table.data-table")

for row in data:
    print(row)
```

### Card/Grid Layouts

```python
def extract_grid_items(driver, url):
    """Extract items from grid layout."""
    driver.navigate_to(url)
    
    items = []
    
    # Count items
    item_count = len(driver.select_text_all(".grid-item"))
    
    for i in range(1, item_count + 1):
        item_selector = f".grid-item:nth-child({i})"
        
        item = {
            "title": driver.select_first_text_or_null(f"{item_selector} .item-title"),
            "subtitle": driver.select_first_text_or_null(f"{item_selector} .item-subtitle"),
            "image": driver.select_first_attribute_or_null(f"{item_selector} img", "src"),
            "link": driver.select_first_attribute_or_null(f"{item_selector} a", "href"),
            "tags": driver.select_text_all(f"{item_selector} .tag"),
        }
        
        items.append(item)
    
    return items

# Use it
items = extract_grid_items(driver, "https://example.com/gallery")
print(f"Extracted {len(items)} items")
```

### Lists with Metadata

```python
def extract_product_list_detailed(driver, url):
    """Extract detailed product list with all metadata."""
    driver.navigate_to(url)
    
    products = []
    
    # Get count
    product_count = len(driver.select_text_all(".product-item"))
    
    for i in range(1, product_count + 1):
        selector = f".product-item:nth-child({i})"
        
        product = {
            # Basic info
            "name": driver.select_first_text_or_null(f"{selector} .product-name"),
            "price": driver.select_first_text_or_null(f"{selector} .product-price"),
            "original_price": driver.select_first_text_or_null(f"{selector} .original-price"),
            
            # Rating
            "rating": driver.select_first_text_or_null(f"{selector} .rating-value"),
            "review_count": driver.select_first_text_or_null(f"{selector} .review-count"),
            
            # Status
            "in_stock": driver.select_first_text_or_null(f"{selector} .stock-status"),
            "badge": driver.select_first_text_or_null(f"{selector} .badge"),
            
            # Links and images
            "url": driver.select_first_attribute_or_null(f"{selector} a", "href"),
            "image": driver.select_first_attribute_or_null(f"{selector} img", "src"),
            
            # Metadata
            "product_id": driver.select_first_attribute_or_null(selector, "data-product-id"),
            "category": driver.select_first_attribute_or_null(selector, "data-category"),
            
            # Multiple items
            "features": driver.select_text_all(f"{selector} .feature-item"),
            "color_options": driver.select_attribute_all(f"{selector} .color-option", "data-color"),
        }
        
        products.append(product)
    
    return products

# Use it
products = extract_product_list_detailed(driver, "https://shop.example.com/products")
for product in products:
    print(f"{product['name']}: {product['price']} ({product['review_count']} reviews)")
```

---

## Handling Pagination

Extract data across multiple pages efficiently.

### Pattern 1: Click-Based Pagination

```python
def scrape_paginated_content(driver, start_url, max_pages=10):
    """Scrape content across paginated pages."""
    driver.navigate_to(start_url)
    
    all_items = []
    page = 1
    
    while page <= max_pages:
        print(f"Scraping page {page}...")
        
        # Extract items from current page
        titles = driver.select_text_all(".item-title")
        descriptions = driver.select_text_all(".item-description")
        
        for title, desc in zip(titles, descriptions):
            all_items.append({
                "title": title,
                "description": desc,
                "page": page
            })
        
        # Check if next button exists
        next_button_exists = driver.exists("a.next-page")
        if not next_button_exists:
            print("No more pages")
            break
        
        # Click next page
        driver.click("a.next-page")
        driver.delay(2000)  # Wait for page load
        
        page += 1
    
    return all_items

# Use it
client = PulsarClient()
client.create_session()
session = PulsarSession(client)
driver = session.driver

items = scrape_paginated_content(driver, "https://example.com/articles", max_pages=5)
print(f"Scraped {len(items)} items from {max(item['page'] for item in items)} pages")

session.close()
client.close()
```

### Pattern 2: URL-Based Pagination

```python
def scrape_url_pagination(session, base_url, max_pages=10):
    """Scrape pages with URL-based pagination."""
    all_items = []
    driver = session.driver
    
    for page_num in range(1, max_pages + 1):
        url = f"{base_url}?page={page_num}"
        print(f"Scraping: {url}")
        
        # Load page
        page = session.load(url, args="-expire 1d")
        driver.navigate_to(page.url)
        
        # Extract items
        titles = driver.select_text_all(".item-title")
        
        # Break if no items found
        if not titles:
            print(f"No items on page {page_num}")
            break
        
        links = driver.select_attribute_all(".item-title", "href")
        
        for title, link in zip(titles, links):
            all_items.append({
                "title": title,
                "link": link,
                "page": page_num
            })
    
    return all_items

# Use it
items = scrape_url_pagination(
    session, 
    "https://example.com/articles",
    max_pages=5
)
print(f"Total items: {len(items)}")
```

### Pattern 3: Infinite Scroll

```python
def scrape_infinite_scroll(driver, url, max_scrolls=10):
    """Scrape infinite scroll pages."""
    driver.navigate_to(url)
    
    all_items = []
    previous_count = 0
    scrolls = 0
    
    while scrolls < max_scrolls:
        # Extract current items
        current_items = driver.select_text_all(".item-title")
        current_count = len(current_items)
        
        print(f"Scroll {scrolls + 1}: {current_count} items")
        
        # Check if new items loaded
        if current_count == previous_count:
            print("No new items, stopping")
            break
        
        previous_count = current_count
        
        # Scroll down
        driver.scroll_down(3)
        driver.delay(2000)  # Wait for items to load
        
        scrolls += 1
    
    # Extract all items at the end
    titles = driver.select_text_all(".item-title")
    links = driver.select_attribute_all(".item-link", "href")
    
    for title, link in zip(titles, links):
        all_items.append({"title": title, "link": link})
    
    return all_items

# Use it
items = scrape_infinite_scroll(driver, "https://example.com/feed", max_scrolls=10)
print(f"Scraped {len(items)} items")
```

### Pattern 4: Load More Button

```python
def scrape_load_more(driver, url, max_clicks=5):
    """Scrape pages with 'Load More' button."""
    driver.navigate_to(url)
    
    clicks = 0
    
    while clicks < max_clicks:
        # Check if load more button exists
        if not driver.exists("button.load-more"):
            print("No more 'Load More' button")
            break
        
        # Click load more
        driver.click("button.load-more")
        driver.delay(2000)  # Wait for new items
        
        clicks += 1
        print(f"Clicked 'Load More' {clicks} times")
    
    # Extract all items
    titles = driver.select_text_all(".item-title")
    prices = driver.select_text_all(".item-price")
    
    items = []
    for title, price in zip(titles, prices):
        items.append({"title": title, "price": price})
    
    return items

# Use it
items = scrape_load_more(driver, "https://shop.example.com/products", max_clicks=5)
print(f"Loaded {len(items)} items")
```

---

## Dynamic Content

Handle JavaScript-rendered content and dynamic updates.

### Waiting for Elements

```python
def scrape_dynamic_page(driver, url):
    """Scrape page with dynamic content."""
    driver.navigate_to(url)
    
    # Wait for key element to appear
    driver.wait_for_selector(".main-content", timeout=10000)
    
    # Additional delay for JS to fully execute
    driver.delay(2000)
    
    # Now extract
    data = driver.extract({
        "title": "h1.page-title",
        "content": ".main-content",
        "items": ".dynamic-item"
    })
    
    return data

# Use it
data = scrape_dynamic_page(driver, "https://spa.example.com")
```

### Handling AJAX Loading

```python
def scrape_ajax_content(driver, url):
    """Handle AJAX-loaded content."""
    driver.navigate_to(url)
    
    # Wait for initial page
    driver.delay(1000)
    
    # Trigger AJAX load (e.g., click tab)
    driver.click(".tab[data-tab='products']")
    
    # Wait for AJAX content
    driver.wait_for_selector(".product-list .product", timeout=10000)
    driver.delay(1000)  # Extra buffer
    
    # Extract AJAX content
    products = []
    titles = driver.select_text_all(".product .title")
    prices = driver.select_text_all(".product .price")
    
    for title, price in zip(titles, prices):
        products.append({"title": title, "price": price})
    
    return products

# Use it
products = scrape_ajax_content(driver, "https://example.com/shop")
```

### Multiple Dynamic Sections

```python
def scrape_spa_page(driver, url):
    """Scrape single-page application with multiple sections."""
    driver.navigate_to(url)
    
    results = {}
    
    # Section 1: Wait and extract
    driver.wait_for_selector(".section-1 .content", timeout=10000)
    results["section1"] = driver.select_first_text_or_null(".section-1 .content")
    
    # Navigate to section 2
    driver.click("[data-section='section-2']")
    driver.wait_for_selector(".section-2 .content", timeout=10000)
    driver.delay(1000)
    results["section2"] = driver.select_first_text_or_null(".section-2 .content")
    
    # Navigate to section 3
    driver.click("[data-section='section-3']")
    driver.wait_for_selector(".section-3 .content", timeout=10000)
    driver.delay(1000)
    results["section3"] = driver.select_first_text_or_null(".section-3 .content")
    
    return results

# Use it
data = scrape_spa_page(driver, "https://spa.example.com")
for section, content in data.items():
    print(f"{section}: {content[:100]}...")
```

---

## Batch Processing

Process multiple URLs efficiently at scale.

### Basic Batch Processing

```python
def batch_scrape_urls(urls, cache_duration="1d"):
    """Scrape multiple URLs efficiently."""
    client = PulsarClient()
    client.create_session()
    session = PulsarSession(client)
    driver = session.driver
    
    results = []
    
    try:
        for i, url in enumerate(urls, 1):
            print(f"[{i}/{len(urls)}] Processing: {url}")
            
            try:
                # Load with caching
                page = session.load(url, args=f"-expire {cache_duration}")
                driver.navigate_to(page.url)
                
                # Extract data
                data = driver.extract({
                    "title": "h1",
                    "description": ".description",
                    "author": ".author"
                })
                
                data["url"] = url
                data["status"] = "success"
                results.append(data)
                
            except Exception as e:
                print(f"  Error: {e}")
                results.append({
                    "url": url,
                    "status": "error",
                    "error": str(e)
                })
        
        return results
        
    finally:
        session.close()
        client.close()

# Use it
urls = [
    "https://example.com/article1",
    "https://example.com/article2",
    "https://example.com/article3",
    # ... more URLs
]

results = batch_scrape_urls(urls, cache_duration="1h")
successful = [r for r in results if r["status"] == "success"]
print(f"Successfully scraped: {len(successful)}/{len(urls)}")
```

### Parallel Processing (Conceptual)

```python
from concurrent.futures import ThreadPoolExecutor, as_completed

def scrape_single_url(url):
    """Scrape a single URL (needs separate session)."""
    client = PulsarClient()
    
    try:
        client.create_session()
        session = PulsarSession(client)
        driver = session.driver
        
        page = session.load(url, args="-expire 1d")
        driver.navigate_to(page.url)
        
        data = driver.extract({
            "title": "h1",
            "content": ".main-content"
        })
        
        data["url"] = url
        return data
        
    except Exception as e:
        return {"url": url, "error": str(e)}
    
    finally:
        session.close()
        client.close()

def batch_scrape_parallel(urls, max_workers=5):
    """Scrape URLs in parallel (use with caution)."""
    results = []
    
    with ThreadPoolExecutor(max_workers=max_workers) as executor:
        # Submit all tasks
        future_to_url = {
            executor.submit(scrape_single_url, url): url 
            for url in urls
        }
        
        # Collect results
        for future in as_completed(future_to_url):
            url = future_to_url[future]
            try:
                result = future.result()
                results.append(result)
                print(f"✓ {url}")
            except Exception as e:
                print(f"✗ {url}: {e}")
                results.append({"url": url, "error": str(e)})
    
    return results

# Use with caution - may overload server
# results = batch_scrape_parallel(urls, max_workers=3)
```

### Rate-Limited Batch Processing

```python
import time

def batch_scrape_rate_limited(urls, delay_seconds=2):
    """Scrape with rate limiting."""
    client = PulsarClient()
    client.create_session()
    session = PulsarSession(client)
    driver = session.driver
    
    results = []
    
    try:
        for i, url in enumerate(urls):
            print(f"[{i+1}/{len(urls)}] Scraping: {url}")
            
            try:
                page = session.load(url, args="-expire 1d")
                driver.navigate_to(page.url)
                
                data = driver.extract({
                    "title": "h1",
                    "description": ".description"
                })
                
                data["url"] = url
                results.append(data)
                
            except Exception as e:
                print(f"  Error: {e}")
                results.append({"url": url, "error": str(e)})
            
            # Rate limiting
            if i < len(urls) - 1:  # Don't delay after last URL
                time.sleep(delay_seconds)
        
        return results
        
    finally:
        session.close()
        client.close()

# Use it
results = batch_scrape_rate_limited(urls, delay_seconds=3)
```

---

## Performance Optimization

Optimize scraping speed and resource usage.

### Caching Strategy

```python
def scrape_with_smart_caching(session, urls):
    """Use appropriate caching for different content types."""
    driver = session.driver
    results = []
    
    for url in urls:
        # Determine cache duration based on URL pattern
        if "/news/" in url or "/live/" in url:
            # News: short cache
            cache_args = "-expire 10m"
        elif "/product/" in url:
            # Products: medium cache
            cache_args = "-expire 1h"
        elif "/about/" in url or "/terms/" in url:
            # Static pages: long cache
            cache_args = "-expire 7d"
        else:
            # Default: 1 day
            cache_args = "-expire 1d"
        
        page = session.load(url, args=cache_args)
        driver.navigate_to(page.url)
        
        data = driver.extract({"title": "h1"})
        data["url"] = url
        results.append(data)
    
    return results
```

### Minimal Extraction

```python
def extract_minimal_data(driver, url):
    """Extract only what you need."""
    driver.navigate_to(url)
    
    # Don't extract everything - just what you need
    data = {
        "title": driver.select_first_text_or_null("h1"),
        "price": driver.select_first_text_or_null(".price"),
    }
    
    # Skip expensive operations if not needed
    # data["full_text"] = driver.select_first_text_or_null("body")  # Heavy!
    
    return data
```

### Reuse Sessions

```python
class ScraperPool:
    """Reusable scraper with session pooling."""
    
    def __init__(self):
        self.client = PulsarClient()
        self.session = None
        self.driver = None
    
    def setup(self):
        """Setup session (call once)."""
        self.client.create_session()
        self.session = PulsarSession(self.client)
        self.driver = self.session.driver
    
    def scrape_many(self, urls):
        """Scrape multiple URLs with same session."""
        results = []
        
        for url in urls:
            page = self.session.load(url, args="-expire 1d")
            self.driver.navigate_to(page.url)
            
            data = self.driver.extract({
                "title": "h1",
                "content": ".content"
            })
            
            data["url"] = url
            results.append(data)
        
        return results
    
    def cleanup(self):
        """Cleanup session (call once)."""
        if self.session:
            self.session.close()
        if self.client:
            self.client.close()

# Use it
scraper = ScraperPool()
scraper.setup()

try:
    results = scraper.scrape_many(urls)
    print(f"Scraped {len(results)} URLs")
finally:
    scraper.cleanup()
```

---

## Export Formats

Export extracted data in various formats.

### JSON Export

```python
import json

def export_to_json(data, filename):
    """Export data to JSON file."""
    with open(filename, 'w', encoding='utf-8') as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
    
    print(f"Exported to {filename}")

# Use it
articles = batch_scrape_urls(urls)
export_to_json(articles, "articles.json")
```

### CSV Export

```python
import csv

def export_to_csv(data, filename):
    """Export data to CSV file."""
    if not data:
        return
    
    # Get all keys from all dicts
    keys = set()
    for item in data:
        keys.update(item.keys())
    
    keys = sorted(keys)
    
    with open(filename, 'w', newline='', encoding='utf-8') as f:
        writer = csv.DictWriter(f, fieldnames=keys)
        writer.writeheader()
        writer.writerows(data)
    
    print(f"Exported to {filename}")

# Use it
products = scrape_product_list_detailed(driver, "https://shop.example.com")
export_to_csv(products, "products.csv")
```

### Structured Output

```python
def export_structured(data, filename):
    """Export with structure preservation."""
    import json
    
    output = {
        "metadata": {
            "extracted_at": str(datetime.now()),
            "total_items": len(data),
            "source": "Browser4 Python SDK"
        },
        "data": data
    }
    
    with open(filename, 'w', encoding='utf-8') as f:
        json.dump(output, f, indent=2, ensure_ascii=False)
    
    print(f"Exported to {filename}")

# Use it
from datetime import datetime
export_structured(articles, "articles_structured.json")
```

### Database Export (Example)

```python
import sqlite3

def export_to_database(data, db_path, table_name):
    """Export data to SQLite database."""
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()
    
    # Create table (simplified example)
    cursor.execute(f"""
        CREATE TABLE IF NOT EXISTS {table_name} (
            url TEXT PRIMARY KEY,
            title TEXT,
            content TEXT,
            extracted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    """)
    
    # Insert data
    for item in data:
        cursor.execute(f"""
            INSERT OR REPLACE INTO {table_name} (url, title, content)
            VALUES (?, ?, ?)
        """, (item.get('url'), item.get('title'), item.get('content')))
    
    conn.commit()
    conn.close()
    
    print(f"Exported {len(data)} items to {db_path}")

# Use it
# export_to_database(articles, "scraping.db", "articles")
```

---

## Complete Examples

### Example 1: E-commerce Product Scraper

```python
from pulsar_sdk import PulsarClient, PulsarSession
import json

def scrape_ecommerce_site(base_url, max_pages=5):
    """Complete e-commerce scraping example."""
    client = PulsarClient()
    client.create_session()
    session = PulsarSession(client)
    driver = session.driver
    
    all_products = []
    
    try:
        for page_num in range(1, max_pages + 1):
            url = f"{base_url}?page={page_num}"
            print(f"\n📄 Page {page_num}: {url}")
            
            # Load with caching
            page = session.load(url, args="-expire 1h")
            driver.navigate_to(page.url)
            
            # Wait for products to load
            driver.wait_for_selector(".product-card", timeout=10000)
            driver.delay(1000)
            
            # Extract products
            product_count = len(driver.select_text_all(".product-card"))
            print(f"   Found {product_count} products")
            
            if product_count == 0:
                break
            
            for i in range(1, product_count + 1):
                selector = f".product-card:nth-child({i})"
                
                product = {
                    "name": driver.select_first_text_or_null(f"{selector} .product-name"),
                    "price": driver.select_first_text_or_null(f"{selector} .price"),
                    "rating": driver.select_first_text_or_null(f"{selector} .rating"),
                    "reviews": driver.select_first_text_or_null(f"{selector} .review-count"),
                    "url": driver.select_first_attribute_or_null(f"{selector} a", "href"),
                    "image": driver.select_first_attribute_or_null(f"{selector} img", "src"),
                    "page": page_num
                }
                
                all_products.append(product)
        
        # Export results
        export_to_json(all_products, "products.json")
        export_to_csv(all_products, "products.csv")
        
        print(f"\n✅ Scraped {len(all_products)} products from {page_num} pages")
        return all_products
        
    finally:
        session.close()
        client.close()

# Run it
products = scrape_ecommerce_site("https://shop.example.com/products", max_pages=3)
```

### Example 2: News Article Aggregator

```python
def scrape_news_aggregator(news_urls):
    """Scrape multiple news sites and aggregate articles."""
    client = PulsarClient()
    client.create_session()
    session = PulsarSession(client)
    driver = session.driver
    
    articles = []
    
    try:
        for url in news_urls:
            print(f"\n📰 Scraping: {url}")
            
            try:
                # Fresh content for news
                page = session.load(url, args="-expire 10m")
                driver.navigate_to(page.url)
                
                article = {
                    "url": url,
                    "title": driver.select_first_text_or_null("h1"),
                    "author": driver.select_first_text_or_null(".author"),
                    "date": driver.select_first_text_or_null("time"),
                    "content": driver.select_first_text_or_null("article .content"),
                    "tags": driver.select_text_all(".tag"),
                    "image": driver.select_first_attribute_or_null("article img", "src"),
                }
                
                articles.append(article)
                print(f"   ✓ {article['title']}")
                
            except Exception as e:
                print(f"   ✗ Error: {e}")
        
        # Export
        export_to_json(articles, f"news_{datetime.now().strftime('%Y%m%d')}.json")
        
        return articles
        
    finally:
        session.close()
        client.close()

# Run it
news_sites = [
    "https://news.example.com/article1",
    "https://news.example.com/article2",
    "https://news.example.com/article3",
]

articles = scrape_news_aggregator(news_sites)
```

---

## Summary

Key takeaways:

1. **CSS Selectors**: Master selectors for reliable extraction
2. **Single vs Multiple**: Choose the right extraction method
3. **Text vs Attributes**: Know when to extract text or attributes
4. **Complex Structures**: Handle nested, tabular, and grid layouts
5. **Pagination**: Extract data across multiple pages
6. **Dynamic Content**: Wait for JavaScript-rendered elements
7. **Batch Processing**: Scale your scraping efficiently
8. **Performance**: Use caching and session reuse
9. **Export**: Save data in appropriate formats

Happy data extraction! 📊
