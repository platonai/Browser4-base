# Complex Workflows Examples

This guide demonstrates production-ready implementations of complex, real-world web automation workflows using the Pulsar Python SDK. These examples show end-to-end solutions for challenging scenarios.

## Overview

Complex workflows involve:
- Multi-page navigation and data aggregation
- Authentication and session management
- Pagination and infinite scroll handling
- Rate limiting and error recovery
- Data validation and persistence
- Parallel processing and optimization

## Prerequisites

```python
from pulsar_sdk import PulsarSession, AgenticSession
import json
import csv
from pathlib import Path
from datetime import datetime
from typing import List, Dict, Optional
import time
import logging
from concurrent.futures import ThreadPoolExecutor, as_completed
```

---

## Example 1: E-commerce Product Catalog Scraper

### Problem Statement
Build a comprehensive e-commerce scraper that navigates category pages, handles pagination, extracts all product details, manages rate limits, and saves data incrementally.

### Complete Code

```python
from pulsar_sdk import PulsarSession
import json
import csv
from pathlib import Path
from datetime import datetime
from typing import List, Dict, Optional
import time
import logging

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

class EcommerceCatalogScraper:
    """
    Comprehensive e-commerce catalog scraper with advanced features.
    """
    
    def __init__(self, base_url: str, output_dir: str = "catalog_data"):
        self.base_url = base_url
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(exist_ok=True)
        self.products = []
        self.errors = []
        self.session = None
        
    def scrape_catalog(
        self, 
        category: str, 
        max_pages: Optional[int] = None,
        delay_between_pages: float = 2.0
    ) -> Dict:
        """
        Scrape entire product catalog from a category.
        
        Args:
            category: Category path (e.g., 'electronics/headphones')
            max_pages: Maximum pages to scrape (None for all)
            delay_between_pages: Delay between page requests (seconds)
        """
        logger.info(f"Starting catalog scrape: {category}")
        start_time = time.time()
        
        with PulsarSession() as session:
            self.session = session
            
            # Navigate to category
            category_url = f"{self.base_url}/{category}"
            logger.info(f"Loading category: {category_url}")
            
            page = session.load(category_url)
            
            # Scrape all pages
            page_num = 1
            has_next_page = True
            
            while has_next_page and (max_pages is None or page_num <= max_pages):
                logger.info(f"Scraping page {page_num}...")
                
                try:
                    # Extract products from current page
                    products = self.extract_products_from_page(page)
                    logger.info(f"  Found {len(products)} products on page {page_num}")
                    
                    # Get detailed information for each product
                    for idx, product in enumerate(products, 1):
                        try:
                            detailed = self.get_product_details(product['url'])
                            self.products.append(detailed)
                            logger.info(f"  [{idx}/{len(products)}] Scraped: {detailed['name'][:50]}")
                        except Exception as e:
                            logger.error(f"  Error scraping product {product['url']}: {e}")
                            self.errors.append({
                                "url": product['url'],
                                "error": str(e),
                                "page": page_num
                            })
                    
                    # Save progress after each page
                    self.save_progress(page_num)
                    
                    # Check for next page
                    next_page = self.find_next_page(page)
                    
                    if next_page:
                        time.sleep(delay_between_pages)  # Rate limiting
                        page = session.load(next_page)
                        page_num += 1
                    else:
                        has_next_page = False
                        logger.info("No more pages found")
                        
                except Exception as e:
                    logger.error(f"Error on page {page_num}: {e}")
                    self.errors.append({
                        "page": page_num,
                        "error": str(e)
                    })
                    has_next_page = False
            
            # Generate final report
            elapsed = time.time() - start_time
            report = self.generate_report(category, page_num - 1, elapsed)
            
            return report
    
    def extract_products_from_page(self, page) -> List[Dict]:
        """Extract product listings from a category page."""
        products = []
        
        # Try multiple selectors for product cards
        product_selectors = [
            ".product-card",
            ".product-item",
            "[data-product-id]",
            "article.product",
            ".product-grid-item"
        ]
        
        product_elements = []
        for selector in product_selectors:
            product_elements = page.select(selector)
            if product_elements:
                break
        
        for elem in product_elements:
            product = {
                "url": self.extract_product_url(elem),
                "name": self.extract_text(elem, "h2, h3, .product-name, .product-title"),
                "price": self.extract_text(elem, ".price, .product-price, [data-price]"),
                "image": self.extract_image(elem),
                "rating": self.extract_rating(elem)
            }
            
            if product['url']:
                products.append(product)
        
        return products
    
    def extract_product_url(self, element) -> Optional[str]:
        """Extract product URL from element."""
        link = element.select_first("a[href]")
        if link:
            href = link.attr("href")
            if href.startswith("http"):
                return href
            else:
                return f"{self.base_url}{href}"
        return None
    
    def extract_text(self, element, selectors: str) -> Optional[str]:
        """Extract text using multiple selectors."""
        for selector in selectors.split(","):
            elem = element.select_first(selector.strip())
            if elem:
                return elem.text.strip()
        return None
    
    def extract_image(self, element) -> Optional[str]:
        """Extract product image URL."""
        img = element.select_first("img")
        if img:
            return img.attr("src") or img.attr("data-src")
        return None
    
    def extract_rating(self, element) -> Optional[float]:
        """Extract product rating."""
        rating_elem = element.select_first("[data-rating], .rating, .star-rating")
        if rating_elem:
            rating_text = rating_elem.attr("data-rating") or rating_elem.text
            try:
                # Extract number from text like "4.5 stars" or "4.5/5"
                import re
                match = re.search(r'(\d+\.?\d*)', rating_text)
                if match:
                    return float(match.group(1))
            except:
                pass
        return None
    
    def get_product_details(self, product_url: str) -> Dict:
        """Get detailed information from product page."""
        page = self.session.load(product_url)
        
        return {
            "url": product_url,
            "scraped_at": datetime.now().isoformat(),
            "name": self.extract_text(page, "h1, .product-title, #productTitle"),
            "brand": self.extract_text(page, ".brand, [itemprop='brand'], .product-brand"),
            "price": self.extract_price_details(page),
            "description": self.extract_description(page),
            "specifications": self.extract_specifications(page),
            "images": self.extract_all_images(page),
            "availability": self.extract_availability(page),
            "reviews": self.extract_review_summary(page),
            "sku": self.extract_text(page, ".sku, [itemprop='sku'], .product-code")
        }
    
    def extract_price_details(self, page) -> Dict:
        """Extract comprehensive price information."""
        current_price = self.extract_text(
            page,
            ".price, .current-price, [itemprop='price'], .product-price"
        )
        original_price = self.extract_text(
            page,
            ".original-price, .was-price, .regular-price"
        )
        
        return {
            "current": self.parse_price(current_price),
            "original": self.parse_price(original_price),
            "currency": self.extract_currency(current_price),
            "on_sale": original_price is not None
        }
    
    def parse_price(self, price_text: Optional[str]) -> Optional[float]:
        """Parse price string to float."""
        if not price_text:
            return None
        
        import re
        cleaned = re.sub(r'[^\d.]', '', price_text)
        try:
            return float(cleaned)
        except:
            return None
    
    def extract_currency(self, price_text: Optional[str]) -> str:
        """Extract currency from price text."""
        if not price_text:
            return "USD"
        
        currency_map = {
            "$": "USD",
            "€": "EUR",
            "£": "GBP",
            "¥": "JPY"
        }
        
        for symbol, code in currency_map.items():
            if symbol in price_text:
                return code
        
        return "USD"
    
    def extract_description(self, page) -> str:
        """Extract product description."""
        desc_elem = page.select_first(
            ".product-description, #productDescription, [itemprop='description']"
        )
        return desc_elem.text.strip() if desc_elem else ""
    
    def extract_specifications(self, page) -> Dict:
        """Extract product specifications."""
        specs = {}
        
        # Try table format
        spec_rows = page.select(".specifications tr, .specs-table tr")
        for row in spec_rows:
            key = row.select_first("th, td:first-child")
            value = row.select_first("td:last-child")
            if key and value:
                specs[key.text.strip()] = value.text.strip()
        
        # Try list format
        if not specs:
            spec_items = page.select(".spec-item")
            for item in spec_items:
                text = item.text.strip()
                if ":" in text:
                    key, value = text.split(":", 1)
                    specs[key.strip()] = value.strip()
        
        return specs
    
    def extract_all_images(self, page) -> List[str]:
        """Extract all product images."""
        images = []
        
        # Main image
        main_img = page.select_first(".main-image img, #mainImage")
        if main_img:
            images.append(main_img.attr("src") or main_img.attr("data-src"))
        
        # Gallery images
        gallery_imgs = page.select(".product-gallery img, .thumbnail img")
        for img in gallery_imgs:
            src = img.attr("src") or img.attr("data-src")
            if src and src not in images:
                images.append(src)
        
        return images
    
    def extract_availability(self, page) -> Dict:
        """Extract availability information."""
        stock_elem = page.select_first(".stock-status, .availability, #availability")
        stock_text = stock_elem.text.strip().lower() if stock_elem else ""
        
        in_stock = any(keyword in stock_text for keyword in ["in stock", "available"])
        
        return {
            "in_stock": in_stock,
            "status_text": stock_text
        }
    
    def extract_review_summary(self, page) -> Dict:
        """Extract review summary."""
        rating_elem = page.select_first(".rating, [itemprop='ratingValue']")
        count_elem = page.select_first(".review-count, [itemprop='reviewCount']")
        
        rating = None
        if rating_elem:
            rating_text = rating_elem.text.strip()
            try:
                import re
                match = re.search(r'(\d+\.?\d*)', rating_text)
                if match:
                    rating = float(match.group(1))
            except:
                pass
        
        count = 0
        if count_elem:
            count_text = count_elem.text.strip()
            try:
                import re
                match = re.search(r'(\d+)', count_text.replace(',', ''))
                if match:
                    count = int(match.group(1))
            except:
                pass
        
        return {
            "average_rating": rating,
            "total_reviews": count
        }
    
    def find_next_page(self, page) -> Optional[str]:
        """Find next pagination page URL."""
        # Try common pagination patterns
        next_selectors = [
            "a.next-page",
            ".pagination a.next",
            "a[aria-label='Next']",
            ".pagination li.active + li a",
            "a:contains('Next')"
        ]
        
        for selector in next_selectors:
            next_link = page.select_first(selector)
            if next_link:
                href = next_link.attr("href")
                if href:
                    if href.startswith("http"):
                        return href
                    else:
                        return f"{self.base_url}{href}"
        
        return None
    
    def save_progress(self, page_num: int):
        """Save progress after each page."""
        # Save to JSON
        json_file = self.output_dir / f"products_page_{page_num}.json"
        with open(json_file, "w", encoding="utf-8") as f:
            json.dump(self.products, f, indent=2, ensure_ascii=False)
        
        # Save to CSV (append mode)
        csv_file = self.output_dir / "products_all.csv"
        
        if self.products:
            file_exists = csv_file.exists()
            
            with open(csv_file, "a", newline="", encoding="utf-8") as f:
                writer = csv.DictWriter(
                    f,
                    fieldnames=self.get_csv_fieldnames(),
                    extrasaction='ignore'
                )
                
                if not file_exists:
                    writer.writeheader()
                
                for product in self.products[-10:]:  # Save last 10 products
                    writer.writerow(self.flatten_product(product))
        
        logger.info(f"Progress saved: {len(self.products)} products")
    
    def get_csv_fieldnames(self) -> List[str]:
        """Get CSV column names."""
        return [
            "url", "scraped_at", "name", "brand", "sku",
            "current_price", "original_price", "currency", "on_sale",
            "in_stock", "average_rating", "total_reviews",
            "description"
        ]
    
    def flatten_product(self, product: Dict) -> Dict:
        """Flatten nested product data for CSV."""
        return {
            "url": product.get("url"),
            "scraped_at": product.get("scraped_at"),
            "name": product.get("name"),
            "brand": product.get("brand"),
            "sku": product.get("sku"),
            "current_price": product.get("price", {}).get("current"),
            "original_price": product.get("price", {}).get("original"),
            "currency": product.get("price", {}).get("currency"),
            "on_sale": product.get("price", {}).get("on_sale"),
            "in_stock": product.get("availability", {}).get("in_stock"),
            "average_rating": product.get("reviews", {}).get("average_rating"),
            "total_reviews": product.get("reviews", {}).get("total_reviews"),
            "description": product.get("description", "")[:200]  # Truncate
        }
    
    def generate_report(self, category: str, pages_scraped: int, elapsed: float) -> Dict:
        """Generate final scraping report."""
        report = {
            "category": category,
            "started_at": datetime.now().isoformat(),
            "pages_scraped": pages_scraped,
            "products_found": len(self.products),
            "errors": len(self.errors),
            "elapsed_seconds": round(elapsed, 2),
            "products_per_second": round(len(self.products) / elapsed, 2) if elapsed > 0 else 0,
            "output_directory": str(self.output_dir)
        }
        
        # Save report
        report_file = self.output_dir / "scraping_report.json"
        with open(report_file, "w", encoding="utf-8") as f:
            json.dump(report, f, indent=2)
        
        # Save errors
        if self.errors:
            errors_file = self.output_dir / "errors.json"
            with open(errors_file, "w", encoding="utf-8") as f:
                json.dump(self.errors, f, indent=2)
        
        # Print summary
        logger.info(f"\n{'='*60}")
        logger.info(f"Scraping Complete!")
        logger.info(f"  Category: {category}")
        logger.info(f"  Pages scraped: {pages_scraped}")
        logger.info(f"  Products found: {len(self.products)}")
        logger.info(f"  Errors: {len(self.errors)}")
        logger.info(f"  Time elapsed: {elapsed:.2f}s")
        logger.info(f"  Rate: {report['products_per_second']:.2f} products/second")
        logger.info(f"  Output: {self.output_dir}")
        logger.info(f"{'='*60}")
        
        return report

# Example usage
if __name__ == "__main__":
    scraper = EcommerceCatalogScraper(
        base_url="https://example-shop.com",
        output_dir="electronics_catalog"
    )
    
    report = scraper.scrape_catalog(
        category="electronics/headphones",
        max_pages=10,
        delay_between_pages=2.0
    )
    
    print(f"\nFinal Report:")
    print(json.dumps(report, indent=2))
```

### Expected Output

```
2024-01-30 10:00:00 - INFO - Starting catalog scrape: electronics/headphones
2024-01-30 10:00:00 - INFO - Loading category: https://example-shop.com/electronics/headphones
2024-01-30 10:00:05 - INFO - Scraping page 1...
2024-01-30 10:00:05 - INFO -   Found 24 products on page 1
2024-01-30 10:00:06 - INFO -   [1/24] Scraped: Sony WH-1000XM5 Wireless Noise Canceling Headphones
2024-01-30 10:00:08 - INFO -   [2/24] Scraped: Bose QuietComfort 45 Bluetooth Wireless Noise Can
...
2024-01-30 10:02:15 - INFO - Progress saved: 24 products
2024-01-30 10:02:17 - INFO - Scraping page 2...
...
2024-01-30 10:25:30 - INFO - No more pages found

============================================================
Scraping Complete!
  Category: electronics/headphones
  Pages scraped: 10
  Products found: 237
  Errors: 3
  Time elapsed: 1530.45s
  Rate: 0.15 products/second
  Output: electronics_catalog
============================================================
```

### Key Concepts

1. **Progressive Saving**: Save data after each page
2. **Error Recovery**: Continue despite individual failures
3. **Rate Limiting**: Respect server resources
4. **Structured Logging**: Track progress and issues
5. **Multiple Output Formats**: JSON and CSV for different uses

---

## Example 2: News Aggregation from Multiple Sources

### Problem Statement
Aggregate news articles from multiple sources, deduplicate, categorize, and create a unified feed with sentiment analysis.

### Complete Code

```python
from pulsar_sdk import PulsarSession, AgenticSession
import json
from datetime import datetime, timedelta
from typing import List, Dict
from collections import defaultdict
import hashlib

class NewsAggregator:
    """
    Multi-source news aggregator with deduplication and analysis.
    """
    
    def __init__(self, output_dir: str = "news_data"):
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(exist_ok=True)
        self.articles = []
        self.sources = []
        
    def aggregate_news(
        self,
        sources: List[Dict[str, str]],
        topics: List[str],
        hours_back: int = 24
    ) -> Dict:
        """
        Aggregate news from multiple sources.
        
        Args:
            sources: List of source configs [{"name": "...", "url": "..."}]
            topics: List of topics to filter
            hours_back: How far back to look for articles
        """
        logger.info(f"Starting news aggregation from {len(sources)} sources")
        logger.info(f"Topics: {', '.join(topics)}")
        
        cutoff_time = datetime.now() - timedelta(hours=hours_back)
        
        with PulsarSession() as session:
            for source in sources:
                logger.info(f"\nScraping source: {source['name']}")
                
                try:
                    articles = self.scrape_news_source(
                        session,
                        source,
                        topics,
                        cutoff_time
                    )
                    
                    self.articles.extend(articles)
                    logger.info(f"  Found {len(articles)} articles")
                    
                except Exception as e:
                    logger.error(f"  Error scraping {source['name']}: {e}")
        
        # Deduplicate articles
        logger.info("\nDeduplicating articles...")
        unique_articles = self.deduplicate_articles(self.articles)
        logger.info(f"  Unique articles: {len(unique_articles)}")
        
        # Categorize and analyze
        logger.info("\nAnalyzing articles...")
        analyzed = self.analyze_articles(unique_articles)
        
        # Generate feed
        feed = self.generate_feed(analyzed, topics)
        
        # Save results
        self.save_feed(feed)
        
        return feed
    
    def scrape_news_source(
        self,
        session,
        source: Dict,
        topics: List[str],
        cutoff_time: datetime
    ) -> List[Dict]:
        """Scrape articles from a single news source."""
        articles = []
        
        page = session.load(source['url'])
        
        # Extract article listings
        article_elements = page.select(
            "article, .article-item, .news-item, .post"
        )
        
        for elem in article_elements[:50]:  # Limit per source
            article = self.extract_article_preview(elem, source)
            
            if article and self.is_relevant(article, topics):
                # Get full article content
                try:
                    full_article = self.get_full_article(session, article['url'])
                    article.update(full_article)
                    
                    # Check publication date
                    if self.is_recent(article, cutoff_time):
                        articles.append(article)
                        
                except Exception as e:
                    logger.warning(f"    Error getting full article: {e}")
        
        return articles
    
    def extract_article_preview(self, element, source: Dict) -> Dict:
        """Extract article preview from listing."""
        link = element.select_first("a[href]")
        title_elem = element.select_first("h1, h2, h3, .title")
        
        if not link or not title_elem:
            return None
        
        url = link.attr("href")
        if not url.startswith("http"):
            url = f"{source['url']}{url}"
        
        return {
            "url": url,
            "title": title_elem.text.strip(),
            "source": source['name'],
            "source_url": source['url'],
            "extracted_at": datetime.now().isoformat()
        }
    
    def get_full_article(self, session, url: str) -> Dict:
        """Get full article content."""
        page = session.load(url)
        
        return {
            "author": self.extract_author(page),
            "published_date": self.extract_published_date(page),
            "content": self.extract_content(page),
            "summary": self.extract_summary(page),
            "tags": self.extract_tags(page),
            "image": self.extract_main_image(page)
        }
    
    def extract_author(self, page) -> str:
        """Extract article author."""
        author_elem = page.select_first(
            ".author, [rel='author'], [itemprop='author'], .byline"
        )
        return author_elem.text.strip() if author_elem else "Unknown"
    
    def extract_published_date(self, page) -> Optional[str]:
        """Extract publication date."""
        date_elem = page.select_first(
            "time[datetime], .publish-date, .post-date, [itemprop='datePublished']"
        )
        
        if date_elem:
            datetime_attr = date_elem.attr("datetime")
            if datetime_attr:
                return datetime_attr
            return date_elem.text.strip()
        
        return None
    
    def extract_content(self, page) -> str:
        """Extract main article content."""
        content_selectors = [
            "article .article-body",
            ".article-content",
            "[itemprop='articleBody']",
            ".post-content",
            "article p"
        ]
        
        for selector in content_selectors:
            elements = page.select(selector)
            if elements:
                return "\n\n".join([e.text.strip() for e in elements])
        
        return ""
    
    def extract_summary(self, page) -> str:
        """Extract article summary or excerpt."""
        summary_elem = page.select_first(
            ".summary, .excerpt, [itemprop='description'], meta[name='description']"
        )
        
        if summary_elem:
            if summary_elem.name == "meta":
                return summary_elem.attr("content") or ""
            return summary_elem.text.strip()
        
        # Generate summary from content (first paragraph)
        first_para = page.select_first("article p, .article-content p")
        return first_para.text.strip() if first_para else ""
    
    def extract_tags(self, page) -> List[str]:
        """Extract article tags."""
        tags = []
        tag_elements = page.select(".tags a, .categories a, [rel='tag']")
        
        for elem in tag_elements:
            tags.append(elem.text.strip())
        
        return tags
    
    def extract_main_image(self, page) -> Optional[str]:
        """Extract main article image."""
        img = page.select_first(
            "article img, .featured-image img, .article-image img"
        )
        
        if img:
            return img.attr("src") or img.attr("data-src")
        
        return None
    
    def is_relevant(self, article: Dict, topics: List[str]) -> bool:
        """Check if article is relevant to topics."""
        title_lower = article['title'].lower()
        return any(topic.lower() in title_lower for topic in topics)
    
    def is_recent(self, article: Dict, cutoff_time: datetime) -> bool:
        """Check if article is recent enough."""
        pub_date_str = article.get('published_date')
        
        if not pub_date_str:
            return True  # Include if no date
        
        try:
            # Simple ISO format parsing
            pub_date = datetime.fromisoformat(pub_date_str.replace('Z', '+00:00'))
            return pub_date >= cutoff_time
        except:
            return True  # Include if can't parse
    
    def deduplicate_articles(self, articles: List[Dict]) -> List[Dict]:
        """Remove duplicate articles based on content similarity."""
        seen_hashes = set()
        unique = []
        
        for article in articles:
            # Create hash from title and content
            content = f"{article['title']}{article.get('content', '')[:500]}"
            content_hash = hashlib.md5(content.encode()).hexdigest()
            
            if content_hash not in seen_hashes:
                seen_hashes.add(content_hash)
                article['content_hash'] = content_hash
                unique.append(article)
        
        return unique
    
    def analyze_articles(self, articles: List[Dict]) -> List[Dict]:
        """Analyze articles using AI."""
        logger.info("Performing AI analysis on articles...")
        
        with AgenticSession() as session:
            for idx, article in enumerate(articles[:20], 1):  # Analyze first 20
                logger.info(f"  [{idx}/20] Analyzing: {article['title'][:50]}...")
                
                try:
                    analysis = session.execute_task(
                        f"""
                        Analyze this news article:
                        
                        Title: {article['title']}
                        Content: {article.get('content', '')[:1000]}
                        
                        Provide:
                        1. Main topic/category
                        2. Sentiment (positive/negative/neutral)
                        3. Key entities mentioned (people, organizations, places)
                        4. Importance score (1-10)
                        """
                    )
                    
                    article['ai_analysis'] = analysis
                    article['analyzed'] = True
                    
                except Exception as e:
                    logger.warning(f"    Analysis failed: {e}")
                    article['analyzed'] = False
        
        return articles
    
    def generate_feed(self, articles: List[Dict], topics: List[str]) -> Dict:
        """Generate structured news feed."""
        # Sort by date (most recent first)
        sorted_articles = sorted(
            articles,
            key=lambda x: x.get('published_date', ''),
            reverse=True
        )
        
        # Group by source
        by_source = defaultdict(list)
        for article in sorted_articles:
            by_source[article['source']].append(article)
        
        # Group by topic
        by_topic = defaultdict(list)
        for article in sorted_articles:
            for topic in topics:
                if topic.lower() in article['title'].lower():
                    by_topic[topic].append(article)
        
        return {
            "generated_at": datetime.now().isoformat(),
            "total_articles": len(sorted_articles),
            "sources_count": len(by_source),
            "topics": topics,
            "articles": sorted_articles[:100],  # Top 100
            "by_source": dict(by_source),
            "by_topic": dict(by_topic)
        }
    
    def save_feed(self, feed: Dict):
        """Save feed in multiple formats."""
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        
        # Full JSON feed
        json_file = self.output_dir / f"news_feed_{timestamp}.json"
        with open(json_file, "w", encoding="utf-8") as f:
            json.dump(feed, f, indent=2, ensure_ascii=False)
        
        logger.info(f"\n✓ Feed saved: {json_file}")
        
        # HTML summary
        html_file = self.output_dir / f"news_feed_{timestamp}.html"
        self.generate_html_feed(feed, html_file)
        
        logger.info(f"✓ HTML feed: {html_file}")
        
        # RSS feed
        rss_file = self.output_dir / f"news_feed_{timestamp}.xml"
        self.generate_rss_feed(feed, rss_file)
        
        logger.info(f"✓ RSS feed: {rss_file}")
    
    def generate_html_feed(self, feed: Dict, output_file: Path):
        """Generate HTML version of feed."""
        html = f"""
        <!DOCTYPE html>
        <html>
        <head>
            <title>News Feed - {feed['generated_at']}</title>
            <style>
                body {{ font-family: Arial, sans-serif; max-width: 1200px; margin: 0 auto; padding: 20px; }}
                h1 {{ color: #333; }}
                .article {{ border: 1px solid #ddd; margin: 20px 0; padding: 15px; border-radius: 5px; }}
                .article h2 {{ margin-top: 0; }}
                .meta {{ color: #666; font-size: 0.9em; }}
                .source {{ background: #007bff; color: white; padding: 2px 8px; border-radius: 3px; }}
            </style>
        </head>
        <body>
            <h1>News Feed</h1>
            <p>Generated: {feed['generated_at']}</p>
            <p>Total Articles: {feed['total_articles']} | Sources: {feed['sources_count']}</p>
            <hr>
        """
        
        for article in feed['articles'][:50]:
            html += f"""
            <div class="article">
                <h2><a href="{article['url']}">{article['title']}</a></h2>
                <div class="meta">
                    <span class="source">{article['source']}</span>
                    | By {article.get('author', 'Unknown')}
                    | {article.get('published_date', 'Date unknown')}
                </div>
                <p>{article.get('summary', '')[:300]}...</p>
            </div>
            """
        
        html += "</body></html>"
        
        with open(output_file, "w", encoding="utf-8") as f:
            f.write(html)
    
    def generate_rss_feed(self, feed: Dict, output_file: Path):
        """Generate RSS XML feed."""
        from xml.etree.ElementTree import Element, SubElement, tostring
        from xml.dom import minidom
        
        rss = Element("rss", version="2.0")
        channel = SubElement(rss, "channel")
        
        SubElement(channel, "title").text = "Aggregated News Feed"
        SubElement(channel, "description").text = "Multi-source news aggregation"
        SubElement(channel, "pubDate").text = feed['generated_at']
        
        for article in feed['articles'][:50]:
            item = SubElement(channel, "item")
            SubElement(item, "title").text = article['title']
            SubElement(item, "link").text = article['url']
            SubElement(item, "description").text = article.get('summary', '')
            SubElement(item, "pubDate").text = article.get('published_date', '')
            SubElement(item, "source").text = article['source']
        
        # Pretty print
        xml_str = minidom.parseString(tostring(rss)).toprettyxml(indent="  ")
        
        with open(output_file, "w", encoding="utf-8") as f:
            f.write(xml_str)

# Example usage
if __name__ == "__main__":
    aggregator = NewsAggregator(output_dir="aggregated_news")
    
    sources = [
        {"name": "TechCrunch", "url": "https://techcrunch.com"},
        {"name": "The Verge", "url": "https://www.theverge.com"},
        {"name": "Ars Technica", "url": "https://arstechnica.com"},
        {"name": "Wired", "url": "https://www.wired.com"}
    ]
    
    topics = ["artificial intelligence", "machine learning", "robotics", "automation"]
    
    feed = aggregator.aggregate_news(
        sources=sources,
        topics=topics,
        hours_back=24
    )
    
    print(f"\nAggregation complete!")
    print(f"Total articles: {feed['total_articles']}")
    print(f"Sources: {feed['sources_count']}")
```

### Expected Output

```
2024-01-30 10:00:00 - INFO - Starting news aggregation from 4 sources
2024-01-30 10:00:00 - INFO - Topics: artificial intelligence, machine learning, robotics, automation

2024-01-30 10:00:00 - INFO - Scraping source: TechCrunch
2024-01-30 10:00:15 - INFO -   Found 12 articles

2024-01-30 10:00:20 - INFO - Scraping source: The Verge
2024-01-30 10:00:35 - INFO -   Found 8 articles

2024-01-30 10:00:40 - INFO - Scraping source: Ars Technica
2024-01-30 10:00:52 - INFO -   Found 15 articles

2024-01-30 10:00:57 - INFO - Scraping source: Wired
2024-01-30 10:01:10 - INFO -   Found 10 articles

2024-01-30 10:01:10 - INFO - Deduplicating articles...
2024-01-30 10:01:11 - INFO -   Unique articles: 42

2024-01-30 10:01:11 - INFO - Analyzing articles...
2024-01-30 10:01:11 - INFO - Performing AI analysis on articles...
  [1/20] Analyzing: OpenAI Announces GPT-5 with Revolutionary Capabili...
  [2/20] Analyzing: Tesla's New Humanoid Robot Performs Complex Tasks...
...

✓ Feed saved: aggregated_news/news_feed_20240130_100245.json
✓ HTML feed: aggregated_news/news_feed_20240130_100245.html
✓ RSS feed: aggregated_news/news_feed_20240130_100245.xml

Aggregation complete!
Total articles: 42
Sources: 4
```

### Key Concepts

1. **Multi-source Aggregation**: Collect from multiple sites
2. **Deduplication**: Remove duplicate content
3. **AI Analysis**: Categorize and analyze articles
4. **Multiple Output Formats**: JSON, HTML, RSS
5. **Recency Filtering**: Only recent articles

---

## Example 3: Authentication and Session Management

### Problem Statement
Handle authenticated sessions, manage cookies, login flows, and scrape user-specific data.

### Complete Code

```python
from pulsar_sdk import PulsarSession
import json
from pathlib import Path
from datetime import datetime
import pickle

class AuthenticatedScraper:
    """
    Scraper with authentication and session management.
    """
    
    def __init__(self, site_url: str, cookies_file: str = "cookies.pkl"):
        self.site_url = site_url
        self.cookies_file = Path(cookies_file)
        self.session = None
        self.authenticated = False
    
    def login(self, username: str, password: str, save_cookies: bool = True):
        """
        Perform login and save session cookies.
        
        Args:
            username: Login username
            password: Login password
            save_cookies: Whether to save cookies for future use
        """
        logger.info(f"Logging in to {self.site_url}")
        
        with PulsarSession() as session:
            self.session = session
            
            # Navigate to login page
            login_url = f"{self.site_url}/login"
            page = session.load(login_url)
            
            # Find and fill login form
            username_field = page.select_first(
                "input[name='username'], input[type='email'], #username, #email"
            )
            password_field = page.select_first(
                "input[name='password'], input[type='password'], #password"
            )
            submit_button = page.select_first(
                "button[type='submit'], input[type='submit'], button.login"
            )
            
            if not username_field or not password_field:
                raise Exception("Could not find login form fields")
            
            # Fill credentials
            logger.info("Filling login credentials...")
            username_field.fill(username)
            password_field.fill(password)
            
            # Submit form
            logger.info("Submitting login form...")
            submit_button.click()
            
            # Wait for redirect/login to complete
            session.wait_for_navigation()
            
            # Verify login success
            current_page = session.current_page()
            if self.verify_login(current_page):
                self.authenticated = True
                logger.info("✓ Login successful")
                
                # Save cookies
                if save_cookies:
                    self.save_cookies(session)
                
                return True
            else:
                logger.error("✗ Login failed")
                return False
    
    def verify_login(self, page) -> bool:
        """Verify that login was successful."""
        # Check for common indicators of successful login
        indicators = [
            ".user-profile",
            ".logged-in",
            "#dashboard",
            "a[href*='logout']",
            ".account-menu"
        ]
        
        for indicator in indicators:
            if page.select_first(indicator):
                return True
        
        # Check for login error messages
        error_selectors = [
            ".error-message",
            ".login-error",
            ".alert-danger"
        ]
        
        for selector in error_selectors:
            if page.select_first(selector):
                return False
        
        # If URL changed from login page, assume success
        current_url = page.url
        if "login" not in current_url:
            return True
        
        return False
    
    def save_cookies(self, session):
        """Save session cookies to file."""
        cookies = session.get_cookies()
        with open(self.cookies_file, "wb") as f:
            pickle.dump(cookies, f)
        logger.info(f"✓ Cookies saved to {self.cookies_file}")
    
    def load_cookies(self, session) -> bool:
        """Load cookies from file and set in session."""
        if not self.cookies_file.exists():
            return False
        
        try:
            with open(self.cookies_file, "rb") as f:
                cookies = pickle.load(f)
            
            session.set_cookies(cookies)
            logger.info(f"✓ Cookies loaded from {self.cookies_file}")
            return True
        except Exception as e:
            logger.error(f"Failed to load cookies: {e}")
            return False
    
    def scrape_user_dashboard(self, output_file: str = "dashboard_data.json"):
        """
        Scrape user-specific data from dashboard.
        
        Requires authentication first.
        """
        if not self.authenticated:
            raise Exception("Not authenticated. Call login() first.")
        
        logger.info("Scraping user dashboard...")
        
        dashboard_url = f"{self.site_url}/dashboard"
        page = self.session.load(dashboard_url)
        
        # Extract user-specific data
        data = {
            "scraped_at": datetime.now().isoformat(),
            "profile": self.extract_profile_info(page),
            "statistics": self.extract_statistics(page),
            "recent_activity": self.extract_recent_activity(page),
            "notifications": self.extract_notifications(page)
        }
        
        # Save data
        with open(output_file, "w", encoding="utf-8") as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
        
        logger.info(f"✓ Dashboard data saved to {output_file}")
        
        return data
    
    def extract_profile_info(self, page) -> Dict:
        """Extract user profile information."""
        return {
            "username": page.select_first(".username, .user-name")?.text?.strip(),
            "email": page.select_first(".email, .user-email")?.text?.strip(),
            "member_since": page.select_first(".member-since, .join-date")?.text?.strip()
        }
    
    def extract_statistics(self, page) -> Dict:
        """Extract user statistics."""
        stats = {}
        
        stat_elements = page.select(".stat-item, .statistic, .metric")
        for elem in stat_elements:
            label = elem.select_first(".label, .stat-label")
            value = elem.select_first(".value, .stat-value")
            
            if label and value:
                stats[label.text.strip()] = value.text.strip()
        
        return stats
    
    def extract_recent_activity(self, page) -> List[Dict]:
        """Extract recent activity items."""
        activities = []
        
        activity_elements = page.select(".activity-item, .recent-item")
        for elem in activity_elements[:10]:  # Last 10
            activity = {
                "title": elem.select_first(".title, h3")?.text?.strip(),
                "date": elem.select_first(".date, time")?.text?.strip(),
                "type": elem.select_first(".type, .category")?.text?.strip()
            }
            activities.append(activity)
        
        return activities
    
    def extract_notifications(self, page) -> List[str]:
        """Extract user notifications."""
        notifications = []
        
        notif_elements = page.select(".notification, .alert, .message")
        for elem in notif_elements:
            notifications.append(elem.text.strip())
        
        return notifications

# Example usage
if __name__ == "__main__":
    scraper = AuthenticatedScraper("https://example-app.com")
    
    # Login
    success = scraper.login(
        username="user@example.com",
        password="secure_password_123",
        save_cookies=True
    )
    
    if success:
        # Scrape authenticated data
        dashboard_data = scraper.scrape_user_dashboard("my_dashboard.json")
        
        print(f"\nDashboard Data:")
        print(f"  Username: {dashboard_data['profile']['username']}")
        print(f"  Recent activities: {len(dashboard_data['recent_activity'])}")
        print(f"  Notifications: {len(dashboard_data['notifications'])}")
```

### Expected Output

```
2024-01-30 10:00:00 - INFO - Logging in to https://example-app.com
2024-01-30 10:00:05 - INFO - Filling login credentials...
2024-01-30 10:00:06 - INFO - Submitting login form...
2024-01-30 10:00:08 - INFO - ✓ Login successful
2024-01-30 10:00:08 - INFO - ✓ Cookies saved to cookies.pkl
2024-01-30 10:00:08 - INFO - Scraping user dashboard...
2024-01-30 10:00:12 - INFO - ✓ Dashboard data saved to my_dashboard.json

Dashboard Data:
  Username: john_doe
  Recent activities: 10
  Notifications: 3
```

### Key Concepts

1. **Form Interaction**: Find and fill login forms
2. **Session Persistence**: Save and reuse cookies
3. **Login Verification**: Confirm successful authentication
4. **Protected Content**: Access user-specific data
5. **Cookie Management**: Store sessions for reuse

---

## Best Practices for Complex Workflows

### 1. Implement Robust Error Handling

```python
def scrape_with_retry(url: str, max_retries: int = 3):
    for attempt in range(max_retries):
        try:
            return scrape_page(url)
        except Exception as e:
            if attempt < max_retries - 1:
                logger.warning(f"Attempt {attempt + 1} failed: {e}")
                time.sleep(2 ** attempt)  # Exponential backoff
            else:
                logger.error(f"All retries failed for {url}")
                raise
```

### 2. Use Progressive Saving

```python
def save_checkpoint(data, checkpoint_file):
    """Save progress periodically."""
    with open(checkpoint_file, "w") as f:
        json.dump(data, f)
```

### 3. Implement Rate Limiting

```python
import time
from datetime import datetime, timedelta

class RateLimiter:
    def __init__(self, requests_per_minute: int):
        self.requests_per_minute = requests_per_minute
        self.requests = []
    
    def wait_if_needed(self):
        now = datetime.now()
        # Remove requests older than 1 minute
        self.requests = [r for r in self.requests if now - r < timedelta(minutes=1)]
        
        if len(self.requests) >= self.requests_per_minute:
            sleep_time = 60 - (now - self.requests[0]).seconds
            time.sleep(sleep_time)
        
        self.requests.append(now)
```

### 4. Monitor and Log Progress

```python
import logging

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('scraping.log'),
        logging.StreamHandler()
    ]
)
```

### 5. Validate Data Quality

```python
def validate_product(product: Dict) -> bool:
    """Validate required fields are present."""
    required_fields = ['name', 'price', 'url']
    return all(product.get(field) for field in required_fields)
```

---

## Performance Optimization

### Parallel Processing

```python
from concurrent.futures import ThreadPoolExecutor, as_completed

def scrape_urls_parallel(urls: List[str], max_workers: int = 5):
    results = []
    
    with ThreadPoolExecutor(max_workers=max_workers) as executor:
        future_to_url = {executor.submit(scrape_url, url): url for url in urls}
        
        for future in as_completed(future_to_url):
            url = future_to_url[future]
            try:
                result = future.result()
                results.append(result)
            except Exception as e:
                logger.error(f"Error scraping {url}: {e}")
    
    return results
```

---

## Next Steps

- Review [Simple Scraping](simple-scraping.md) for basic techniques
- Explore [AI Automation](ai-automation.md) for intelligent workflows
- Check the [API Reference](../api-reference/client.md) for complete documentation
- Read [Best Practices Guide](../guides/advanced.md) for production tips
