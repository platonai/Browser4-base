# Simple Web Scraping Examples

This guide provides complete, production-ready examples for common web scraping tasks using the Pulsar Python SDK. Each example is self-contained and can be run as-is.

## Overview

Simple web scraping involves extracting data from web pages and saving it in structured formats. This guide covers:

- Single page scraping
- Multiple page scraping
- Data extraction (text, links, images)
- Data persistence (JSON, CSV, files)

## Prerequisites

```python
from pulsar_sdk import PulsarSession
import json
import csv
from pathlib import Path
from datetime import datetime
```

---

## Example 1: Single Page News Article Scraping

### Problem Statement
Extract article metadata and content from a news website, including title, author, publication date, content, and tags.

### Complete Code

```python
from pulsar_sdk import PulsarSession
import json
from datetime import datetime

def scrape_news_article(url: str, output_file: str = "article.json"):
    """
    Scrape a single news article and save to JSON.
    
    Args:
        url: The article URL to scrape
        output_file: Path to save the JSON output
    """
    with PulsarSession() as session:
        # Load the page with extraction hints
        page = session.load(url)
        
        # Extract article data using CSS selectors
        article_data = {
            "url": url,
            "scraped_at": datetime.now().isoformat(),
            "title": page.select_first("h1.article-title, h1[itemprop='headline']")?.text?.strip(),
            "author": page.select_first(".author-name, [rel='author'], [itemprop='author']")?.text?.strip(),
            "published_date": page.select_first("time[datetime], .publish-date")?.attr("datetime") or 
                            page.select_first("time[datetime], .publish-date")?.text?.strip(),
            "content": extract_article_content(page),
            "tags": [tag.text.strip() for tag in page.select(".article-tags a, .post-tags a")],
            "images": [img.attr("src") for img in page.select("article img, .article-content img")],
            "word_count": len(page.select_first("article, .article-content")?.text?.split() or [])
        }
        
        # Save to JSON file
        with open(output_file, "w", encoding="utf-8") as f:
            json.dump(article_data, f, indent=2, ensure_ascii=False)
        
        print(f"✓ Article scraped successfully: {article_data['title']}")
        print(f"✓ Saved to: {output_file}")
        
        return article_data

def extract_article_content(page):
    """Extract main article content, handling various HTML structures."""
    # Try common content selectors
    content_selectors = [
        "article .article-body",
        ".article-content",
        "[itemprop='articleBody']",
        "article p",
        ".post-content"
    ]
    
    for selector in content_selectors:
        elements = page.select(selector)
        if elements:
            return "\n\n".join([elem.text.strip() for elem in elements if elem.text.strip()])
    
    return ""

# Example usage
if __name__ == "__main__":
    # Scrape a news article
    article = scrape_news_article(
        "https://example-news.com/technology/ai-breakthrough-2024",
        "ai_article.json"
    )
    
    print(f"\nExtracted data:")
    print(f"  Title: {article['title']}")
    print(f"  Author: {article['author']}")
    print(f"  Word count: {article['word_count']}")
    print(f"  Tags: {', '.join(article['tags'])}")
```

### Expected Output

```json
{
  "url": "https://example-news.com/technology/ai-breakthrough-2024",
  "scraped_at": "2024-01-30T10:30:45.123456",
  "title": "Major AI Breakthrough Announced in 2024",
  "author": "Jane Smith",
  "published_date": "2024-01-29T14:00:00",
  "content": "Researchers have announced a significant breakthrough...",
  "tags": ["AI", "Technology", "Research"],
  "images": [
    "https://example-news.com/images/ai-lab.jpg",
    "https://example-news.com/images/chart.png"
  ],
  "word_count": 1250
}
```

### Key Concepts

1. **Optional Chaining**: The `?.` operator safely handles missing elements
2. **Multiple Selectors**: Try various CSS selectors to handle different site structures
3. **Data Validation**: Strip whitespace and handle None values
4. **Timestamp**: Always record when data was scraped
5. **UTF-8 Encoding**: Handle international characters properly

### Variations

**Add image downloads:**
```python
import requests
from pathlib import Path

def download_images(article_data, output_dir="images"):
    Path(output_dir).mkdir(exist_ok=True)
    downloaded = []
    
    for idx, img_url in enumerate(article_data['images']):
        try:
            response = requests.get(img_url, timeout=10)
            if response.status_code == 200:
                filename = f"{output_dir}/image_{idx}.jpg"
                with open(filename, "wb") as f:
                    f.write(response.content)
                downloaded.append(filename)
        except Exception as e:
            print(f"Failed to download {img_url}: {e}")
    
    return downloaded
```

**Add error handling:**
```python
def scrape_news_article_safe(url: str, output_file: str = "article.json"):
    try:
        return scrape_news_article(url, output_file)
    except Exception as e:
        error_data = {
            "url": url,
            "error": str(e),
            "scraped_at": datetime.now().isoformat(),
            "success": False
        }
        with open(output_file, "w") as f:
            json.dump(error_data, f, indent=2)
        print(f"✗ Error scraping {url}: {e}")
        return error_data
```

---

## Example 2: E-commerce Product Page Scraping

### Problem Statement
Extract comprehensive product information from an e-commerce product page, including price, specifications, reviews, and availability.

### Complete Code

```python
from pulsar_sdk import PulsarSession
import json
from decimal import Decimal
import re

def scrape_product_page(url: str, output_file: str = "product.json"):
    """
    Scrape detailed product information from an e-commerce site.
    
    Args:
        url: Product page URL
        output_file: Output JSON file path
    """
    with PulsarSession() as session:
        page = session.load(url)
        
        product_data = {
            "url": url,
            "scraped_at": datetime.now().isoformat(),
            "basic_info": extract_basic_info(page),
            "pricing": extract_pricing(page),
            "specifications": extract_specifications(page),
            "reviews": extract_review_summary(page),
            "availability": extract_availability(page),
            "images": extract_product_images(page)
        }
        
        # Save to JSON
        with open(output_file, "w", encoding="utf-8") as f:
            json.dump(product_data, f, indent=2, ensure_ascii=False)
        
        print(f"✓ Product scraped: {product_data['basic_info']['name']}")
        print(f"✓ Price: {product_data['pricing']['current_price']}")
        print(f"✓ Rating: {product_data['reviews']['average_rating']}/5")
        
        return product_data

def extract_basic_info(page):
    """Extract basic product information."""
    return {
        "name": page.select_first("h1.product-title, #productTitle")?.text?.strip(),
        "brand": page.select_first(".brand, [itemprop='brand']")?.text?.strip(),
        "model": page.select_first(".model-number, .product-model")?.text?.strip(),
        "sku": page.select_first(".sku, [itemprop='sku']")?.text?.strip(),
        "description": page.select_first(".product-description, #productDescription")?.text?.strip()
    }

def extract_pricing(page):
    """Extract pricing information."""
    current_price_text = page.select_first(".price, .current-price, [itemprop='price']")?.text?.strip()
    original_price_text = page.select_first(".original-price, .was-price")?.text?.strip()
    
    return {
        "current_price": parse_price(current_price_text),
        "original_price": parse_price(original_price_text),
        "currency": extract_currency(current_price_text),
        "discount_percentage": calculate_discount(
            parse_price(original_price_text),
            parse_price(current_price_text)
        ),
        "on_sale": original_price_text is not None and original_price_text != current_price_text
    }

def extract_specifications(page):
    """Extract product specifications."""
    specs = {}
    
    # Try table format
    spec_rows = page.select(".specifications tr, .product-specs tr")
    for row in spec_rows:
        key_elem = row.select_first("th, .spec-label")
        value_elem = row.select_first("td, .spec-value")
        if key_elem and value_elem:
            specs[key_elem.text.strip()] = value_elem.text.strip()
    
    # Try list format
    if not specs:
        spec_items = page.select(".spec-item, .product-detail")
        for item in spec_items:
            text = item.text.strip()
            if ":" in text:
                key, value = text.split(":", 1)
                specs[key.strip()] = value.strip()
    
    return specs

def extract_review_summary(page):
    """Extract review summary information."""
    rating_text = page.select_first(".rating, [itemprop='ratingValue']")?.text?.strip()
    count_text = page.select_first(".review-count, [itemprop='reviewCount']")?.text?.strip()
    
    return {
        "average_rating": float(rating_text) if rating_text else None,
        "total_reviews": int(re.sub(r'[^\d]', '', count_text)) if count_text else 0,
        "rating_distribution": extract_rating_distribution(page)
    }

def extract_rating_distribution(page):
    """Extract rating distribution (5 star, 4 star, etc.)."""
    distribution = {}
    rating_bars = page.select(".rating-bar, .star-distribution")
    
    for bar in rating_bars:
        star_text = bar.select_first(".star-label")?.text?.strip()
        count_text = bar.select_first(".count")?.text?.strip()
        
        if star_text and count_text:
            stars = re.search(r'(\d+)', star_text)
            count = re.search(r'(\d+)', count_text)
            if stars and count:
                distribution[f"{stars.group(1)}_star"] = int(count.group(1))
    
    return distribution

def extract_availability(page):
    """Extract availability information."""
    stock_text = page.select_first(".stock-status, .availability")?.text?.strip().lower()
    
    in_stock = any(keyword in stock_text for keyword in ["in stock", "available", "ships"])
    
    return {
        "in_stock": in_stock,
        "stock_text": stock_text,
        "shipping_info": page.select_first(".shipping-info, .delivery-info")?.text?.strip()
    }

def extract_product_images(page):
    """Extract product images."""
    images = []
    
    # Main image
    main_img = page.select_first(".main-product-image img, #mainImage")
    if main_img:
        images.append({
            "url": main_img.attr("src") or main_img.attr("data-src"),
            "type": "main"
        })
    
    # Thumbnail images
    thumb_imgs = page.select(".thumbnail img, .product-images img")
    for img in thumb_imgs:
        img_url = img.attr("src") or img.attr("data-src")
        if img_url and img_url not in [i["url"] for i in images]:
            images.append({
                "url": img_url,
                "type": "thumbnail"
            })
    
    return images

def parse_price(price_text):
    """Parse price string to float."""
    if not price_text:
        return None
    
    # Remove currency symbols and extract number
    cleaned = re.sub(r'[^\d.,]', '', price_text)
    cleaned = cleaned.replace(',', '')
    
    try:
        return float(cleaned)
    except (ValueError, AttributeError):
        return None

def extract_currency(price_text):
    """Extract currency symbol from price text."""
    if not price_text:
        return None
    
    currency_symbols = {
        '$': 'USD',
        '€': 'EUR',
        '£': 'GBP',
        '¥': 'JPY',
        '₹': 'INR'
    }
    
    for symbol, code in currency_symbols.items():
        if symbol in price_text:
            return code
    
    return None

def calculate_discount(original, current):
    """Calculate discount percentage."""
    if not original or not current or original <= current:
        return 0
    
    return round(((original - current) / original) * 100, 2)

# Example usage
if __name__ == "__main__":
    product = scrape_product_page(
        "https://example-shop.com/products/wireless-headphones-pro",
        "product_headphones.json"
    )
    
    print(f"\nProduct Details:")
    print(f"  Name: {product['basic_info']['name']}")
    print(f"  Brand: {product['basic_info']['brand']}")
    print(f"  Current Price: {product['pricing']['currency']} {product['pricing']['current_price']}")
    if product['pricing']['on_sale']:
        print(f"  Discount: {product['pricing']['discount_percentage']}% OFF")
    print(f"  Rating: {product['reviews']['average_rating']}/5 ({product['reviews']['total_reviews']} reviews)")
    print(f"  In Stock: {'Yes' if product['availability']['in_stock'] else 'No'}")
```

### Expected Output

```json
{
  "url": "https://example-shop.com/products/wireless-headphones-pro",
  "scraped_at": "2024-01-30T10:45:30.456789",
  "basic_info": {
    "name": "Wireless Headphones Pro",
    "brand": "AudioTech",
    "model": "WHP-3000",
    "sku": "AT-WHP3000-BLK",
    "description": "Premium wireless headphones with active noise cancellation..."
  },
  "pricing": {
    "current_price": 199.99,
    "original_price": 299.99,
    "currency": "USD",
    "discount_percentage": 33.33,
    "on_sale": true
  },
  "specifications": {
    "Battery Life": "30 hours",
    "Bluetooth Version": "5.2",
    "Weight": "250g",
    "Drivers": "40mm"
  },
  "reviews": {
    "average_rating": 4.5,
    "total_reviews": 1234,
    "rating_distribution": {
      "5_star": 800,
      "4_star": 300,
      "3_star": 100,
      "2_star": 24,
      "1_star": 10
    }
  },
  "availability": {
    "in_stock": true,
    "stock_text": "in stock",
    "shipping_info": "Free shipping on orders over $50"
  },
  "images": [
    {"url": "https://example-shop.com/images/whp3000-main.jpg", "type": "main"},
    {"url": "https://example-shop.com/images/whp3000-side.jpg", "type": "thumbnail"}
  ]
}
```

### Key Concepts

1. **Structured Data Extraction**: Organize data into logical categories
2. **Price Parsing**: Handle various price formats and currencies
3. **Null Safety**: Always check for None before processing
4. **Regular Expressions**: Clean and extract numbers from text
5. **Discount Calculation**: Compute percentages from prices

---

## Example 3: Scraping Multiple Pages from a List

### Problem Statement
Scrape multiple pages from a list of URLs, handle errors gracefully, and save results in both JSON and CSV formats.

### Complete Code

```python
from pulsar_sdk import PulsarSession
import json
import csv
from pathlib import Path
from datetime import datetime
import time
from typing import List, Dict

def scrape_multiple_pages(urls: List[str], output_dir: str = "output"):
    """
    Scrape multiple pages and save results in various formats.
    
    Args:
        urls: List of URLs to scrape
        output_dir: Directory to save output files
    """
    # Create output directory
    Path(output_dir).mkdir(exist_ok=True)
    
    results = []
    errors = []
    
    print(f"Starting to scrape {len(urls)} pages...")
    
    with PulsarSession() as session:
        for idx, url in enumerate(urls, 1):
            print(f"\n[{idx}/{len(urls)}] Scraping: {url}")
            
            try:
                page_data = scrape_single_page(session, url)
                results.append(page_data)
                print(f"  ✓ Success: {page_data['title'][:50]}...")
                
                # Be polite: add delay between requests
                if idx < len(urls):
                    time.sleep(1)
                    
            except Exception as e:
                error_info = {
                    "url": url,
                    "error": str(e),
                    "timestamp": datetime.now().isoformat()
                }
                errors.append(error_info)
                print(f"  ✗ Error: {e}")
    
    # Save results in multiple formats
    save_results(results, errors, output_dir)
    
    # Print summary
    print(f"\n{'='*60}")
    print(f"Scraping Complete!")
    print(f"  Successful: {len(results)}")
    print(f"  Failed: {len(errors)}")
    print(f"  Output directory: {output_dir}")
    print(f"{'='*60}")
    
    return results, errors

def scrape_single_page(session, url: str) -> Dict:
    """Scrape a single page and return structured data."""
    page = session.load(url)
    
    return {
        "url": url,
        "scraped_at": datetime.now().isoformat(),
        "title": page.select_first("h1, title")?.text?.strip(),
        "meta_description": page.select_first("meta[name='description']")?.attr("content"),
        "links_count": len(page.select("a[href]")),
        "images_count": len(page.select("img[src]")),
        "word_count": len(page.text.split()),
        "external_links": extract_external_links(page, url),
        "headings": extract_headings(page)
    }

def extract_external_links(page, base_url: str) -> List[str]:
    """Extract external links from the page."""
    from urllib.parse import urlparse
    
    base_domain = urlparse(base_url).netloc
    external = []
    
    for link in page.select("a[href]"):
        href = link.attr("href")
        if href and href.startswith("http"):
            link_domain = urlparse(href).netloc
            if link_domain != base_domain:
                external.append(href)
    
    return list(set(external))[:10]  # Limit to 10 unique external links

def extract_headings(page) -> Dict[str, List[str]]:
    """Extract all headings organized by level."""
    headings = {}
    
    for level in range(1, 7):
        h_elements = page.select(f"h{level}")
        if h_elements:
            headings[f"h{level}"] = [h.text.strip() for h in h_elements]
    
    return headings

def save_results(results: List[Dict], errors: List[Dict], output_dir: str):
    """Save results in multiple formats."""
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    
    # Save complete JSON
    json_file = f"{output_dir}/results_{timestamp}.json"
    with open(json_file, "w", encoding="utf-8") as f:
        json.dump({
            "results": results,
            "errors": errors,
            "summary": {
                "total": len(results) + len(errors),
                "successful": len(results),
                "failed": len(errors),
                "timestamp": datetime.now().isoformat()
            }
        }, f, indent=2, ensure_ascii=False)
    print(f"\n✓ JSON saved: {json_file}")
    
    # Save CSV (flattened data)
    if results:
        csv_file = f"{output_dir}/results_{timestamp}.csv"
        save_to_csv(results, csv_file)
        print(f"✓ CSV saved: {csv_file}")
    
    # Save errors separately
    if errors:
        errors_file = f"{output_dir}/errors_{timestamp}.json"
        with open(errors_file, "w", encoding="utf-8") as f:
            json.dump(errors, f, indent=2, ensure_ascii=False)
        print(f"✓ Errors saved: {errors_file}")

def save_to_csv(results: List[Dict], csv_file: str):
    """Save results to CSV format."""
    if not results:
        return
    
    # Flatten nested data for CSV
    flattened = []
    for item in results:
        flat_item = {
            "url": item["url"],
            "scraped_at": item["scraped_at"],
            "title": item["title"],
            "meta_description": item["meta_description"],
            "word_count": item["word_count"],
            "links_count": item["links_count"],
            "images_count": item["images_count"],
            "external_links_count": len(item["external_links"]),
            "h1_count": len(item["headings"].get("h1", [])),
            "h2_count": len(item["headings"].get("h2", []))
        }
        flattened.append(flat_item)
    
    # Write CSV
    with open(csv_file, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=flattened[0].keys())
        writer.writeheader()
        writer.writerows(flattened)

# Example usage
if __name__ == "__main__":
    # List of URLs to scrape
    urls_to_scrape = [
        "https://example.com/blog/post-1",
        "https://example.com/blog/post-2",
        "https://example.com/blog/post-3",
        "https://example.com/products/item-1",
        "https://example.com/products/item-2",
    ]
    
    results, errors = scrape_multiple_pages(urls_to_scrape, "scraped_data")
    
    # Analyze results
    if results:
        avg_word_count = sum(r["word_count"] for r in results) / len(results)
        print(f"\nAnalysis:")
        print(f"  Average word count: {avg_word_count:.0f}")
        print(f"  Total links found: {sum(r['links_count'] for r in results)}")
        print(f"  Total images found: {sum(r['images_count'] for r in results)}")
```

### Expected Output

Console output:
```
Starting to scrape 5 pages...

[1/5] Scraping: https://example.com/blog/post-1
  ✓ Success: How to Build Web Scrapers in Python...

[2/5] Scraping: https://example.com/blog/post-2
  ✓ Success: Advanced Web Scraping Techniques...

[3/5] Scraping: https://example.com/blog/post-3
  ✗ Error: Connection timeout

[4/5] Scraping: https://example.com/products/item-1
  ✓ Success: Premium Wireless Mouse - TechGear...

[5/5] Scraping: https://example.com/products/item-2
  ✓ Success: Mechanical Keyboard RGB - GamePro...

✓ JSON saved: scraped_data/results_20240130_104530.json
✓ CSV saved: scraped_data/results_20240130_104530.csv
✓ Errors saved: scraped_data/errors_20240130_104530.json

============================================================
Scraping Complete!
  Successful: 4
  Failed: 1
  Output directory: scraped_data
============================================================

Analysis:
  Average word count: 1250
  Total links found: 143
  Total images found: 28
```

### Key Concepts

1. **Batch Processing**: Handle multiple URLs efficiently
2. **Error Handling**: Continue processing even when some pages fail
3. **Multiple Output Formats**: Save as JSON (complete) and CSV (tabular)
4. **Polite Scraping**: Add delays between requests
5. **Progress Tracking**: Show progress and summary statistics

### Variations

**Load URLs from file:**
```python
def load_urls_from_file(filename: str) -> List[str]:
    """Load URLs from a text file (one URL per line)."""
    with open(filename, "r") as f:
        return [line.strip() for line in f if line.strip()]

urls = load_urls_from_file("urls.txt")
scrape_multiple_pages(urls)
```

**Add retry logic:**
```python
def scrape_with_retry(session, url: str, max_retries: int = 3) -> Dict:
    """Scrape with automatic retry on failure."""
    for attempt in range(max_retries):
        try:
            return scrape_single_page(session, url)
        except Exception as e:
            if attempt < max_retries - 1:
                wait_time = 2 ** attempt  # Exponential backoff
                print(f"  Retry {attempt + 1}/{max_retries} after {wait_time}s...")
                time.sleep(wait_time)
            else:
                raise e
```

---

## Example 4: Extract and Save Images

### Problem Statement
Download all images from a web page, organize them by type, and save metadata.

### Complete Code

```python
from pulsar_sdk import PulsarSession
import requests
import json
from pathlib import Path
from urllib.parse import urljoin, urlparse
import hashlib
from typing import List, Dict
import mimetypes

def scrape_and_download_images(url: str, output_dir: str = "images"):
    """
    Extract all images from a page and download them.
    
    Args:
        url: Page URL to scrape
        output_dir: Directory to save images and metadata
    """
    # Create output directory
    Path(output_dir).mkdir(exist_ok=True)
    
    print(f"Scraping images from: {url}\n")
    
    with PulsarSession() as session:
        page = session.load(url)
        
        # Extract all images with metadata
        images = extract_all_images(page, url)
        print(f"Found {len(images)} images")
        
        # Download images
        downloaded = download_images(images, output_dir)
        
        # Save metadata
        metadata = {
            "source_url": url,
            "scraped_at": datetime.now().isoformat(),
            "total_images": len(images),
            "downloaded": len(downloaded),
            "failed": len(images) - len(downloaded),
            "images": downloaded
        }
        
        metadata_file = f"{output_dir}/metadata.json"
        with open(metadata_file, "w", encoding="utf-8") as f:
            json.dump(metadata, f, indent=2, ensure_ascii=False)
        
        # Create summary
        print(f"\n{'='*60}")
        print(f"Download Complete!")
        print(f"  Total found: {len(images)}")
        print(f"  Successfully downloaded: {len(downloaded)}")
        print(f"  Failed: {len(images) - len(downloaded)}")
        print(f"  Saved to: {output_dir}")
        print(f"  Metadata: {metadata_file}")
        print(f"{'='*60}")
        
        return metadata

def extract_all_images(page, base_url: str) -> List[Dict]:
    """Extract all images with comprehensive metadata."""
    images = []
    
    for img in page.select("img"):
        src = img.attr("src") or img.attr("data-src") or img.attr("data-lazy")
        
        if not src:
            continue
        
        # Convert relative URLs to absolute
        absolute_url = urljoin(base_url, src)
        
        image_info = {
            "url": absolute_url,
            "alt": img.attr("alt") or "",
            "title": img.attr("title") or "",
            "width": img.attr("width"),
            "height": img.attr("height"),
            "class": img.attr("class") or "",
            "loading": img.attr("loading") or "eager",
            "context": extract_image_context(img)
        }
        
        images.append(image_info)
    
    return images

def extract_image_context(img_element):
    """Extract context information about where the image appears."""
    # Check parent elements for context
    parent = img_element.parent
    
    contexts = []
    if parent:
        if parent.name == "figure":
            contexts.append("figure")
        if "gallery" in parent.attr("class", ""):
            contexts.append("gallery")
        if "hero" in parent.attr("class", ""):
            contexts.append("hero")
        if "thumbnail" in parent.attr("class", ""):
            contexts.append("thumbnail")
    
    return contexts

def download_images(images: List[Dict], output_dir: str) -> List[Dict]:
    """Download images and return metadata for successful downloads."""
    downloaded = []
    
    for idx, image in enumerate(images, 1):
        print(f"[{idx}/{len(images)}] Downloading: {image['url'][:60]}...")
        
        try:
            # Download image
            response = requests.get(image['url'], timeout=15, stream=True)
            response.raise_for_status()
            
            # Determine file extension
            content_type = response.headers.get('content-type', '')
            extension = mimetypes.guess_extension(content_type) or '.jpg'
            
            # Generate filename
            url_hash = hashlib.md5(image['url'].encode()).hexdigest()[:8]
            filename = f"image_{idx:03d}_{url_hash}{extension}"
            filepath = Path(output_dir) / filename
            
            # Save image
            with open(filepath, 'wb') as f:
                for chunk in response.iter_content(chunk_size=8192):
                    f.write(chunk)
            
            # Get file size
            file_size = filepath.stat().st_size
            
            # Add download info
            image['local_path'] = str(filepath)
            image['filename'] = filename
            image['file_size_bytes'] = file_size
            image['file_size_kb'] = round(file_size / 1024, 2)
            image['content_type'] = content_type
            image['download_success'] = True
            
            downloaded.append(image)
            print(f"  ✓ Saved: {filename} ({image['file_size_kb']} KB)")
            
        except Exception as e:
            print(f"  ✗ Failed: {e}")
            image['download_success'] = False
            image['error'] = str(e)
    
    return downloaded

# Example usage
if __name__ == "__main__":
    metadata = scrape_and_download_images(
        "https://example.com/gallery/nature-photos",
        "nature_images"
    )
    
    # Analyze downloaded images
    if metadata['downloaded'] > 0:
        total_size = sum(img['file_size_kb'] for img in metadata['images'])
        print(f"\nStatistics:")
        print(f"  Total size: {total_size:.2f} KB")
        print(f"  Average size: {total_size / len(metadata['images']):.2f} KB")
        
        # Group by content type
        by_type = {}
        for img in metadata['images']:
            content_type = img['content_type']
            by_type[content_type] = by_type.get(content_type, 0) + 1
        
        print(f"\nBy type:")
        for content_type, count in by_type.items():
            print(f"  {content_type}: {count}")
```

### Expected Output

```
Scraping images from: https://example.com/gallery/nature-photos

Found 15 images
[1/15] Downloading: https://example.com/images/mountain.jpg...
  ✓ Saved: image_001_a3f2b1c8.jpg (245.32 KB)
[2/15] Downloading: https://example.com/images/lake.jpg...
  ✓ Saved: image_002_9d7e4b2a.jpg (312.18 KB)
...

============================================================
Download Complete!
  Total found: 15
  Successfully downloaded: 14
  Failed: 1
  Saved to: nature_images
  Metadata: nature_images/metadata.json
============================================================

Statistics:
  Total size: 3458.92 KB
  Average size: 247.07 KB

By type:
  image/jpeg: 12
  image/png: 2
```

### Key Concepts

1. **URL Normalization**: Convert relative URLs to absolute
2. **Streaming Downloads**: Handle large files efficiently
3. **Content Type Detection**: Determine file extensions
4. **Unique Filenames**: Use hashing to avoid collisions
5. **Rich Metadata**: Capture comprehensive image information

---

## Best Practices

### 1. Always Handle Errors Gracefully
```python
try:
    result = scrape_page(url)
except Exception as e:
    logger.error(f"Failed to scrape {url}: {e}")
    # Continue with other pages
```

### 2. Respect Rate Limits
```python
import time
time.sleep(1)  # 1 second between requests
```

### 3. Use Appropriate Timeouts
```python
page = session.load(url, timeout=30)
```

### 4. Validate Data
```python
def validate_price(price):
    if price is None or price < 0:
        raise ValueError("Invalid price")
    return price
```

### 5. Log Your Progress
```python
import logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

logger.info(f"Scraping {url}")
logger.info(f"Found {len(items)} items")
```

---

## Common Patterns

### Pattern 1: Try Multiple Selectors
```python
def find_element(page, selectors):
    for selector in selectors:
        element = page.select_first(selector)
        if element:
            return element
    return None
```

### Pattern 2: Clean Extracted Text
```python
def clean_text(text):
    if not text:
        return ""
    return " ".join(text.split()).strip()
```

### Pattern 3: Parse Structured Data
```python
def parse_json_ld(page):
    script = page.select_first('script[type="application/ld+json"]')
    if script:
        return json.loads(script.text)
    return None
```

---

## Troubleshooting

### Issue: Missing Elements
**Solution**: Use multiple selectors and check for None
```python
title = (page.select_first("h1.title")?.text or 
         page.select_first("h1")?.text or 
         "No title found")
```

### Issue: Encoding Problems
**Solution**: Always specify UTF-8 encoding
```python
with open(file, "w", encoding="utf-8") as f:
    json.dump(data, f, ensure_ascii=False)
```

### Issue: Rate Limiting
**Solution**: Add delays and implement exponential backoff
```python
time.sleep(random.uniform(1, 3))
```

---

## Next Steps

- Explore [AI Automation Examples](ai-automation.md) for intelligent scraping
- Learn about [Complex Workflows](complex-workflows.md) for advanced scenarios
- Check the [API Reference](../api-reference/client.md) for complete documentation
