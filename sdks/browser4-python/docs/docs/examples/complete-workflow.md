# Complete Workflow Examples

This guide provides end-to-end workflow examples demonstrating how to build complete automation pipelines with Browser4 Python SDK.

## Example 1: E-Commerce Product Scraper

A complete scraper for extracting product information from an e-commerce site.

```python
"""
E-Commerce Product Scraper
Extracts product data from multiple pages with images and reviews.
"""

from browser4 import Browser4Driver, PulsarClient, AgenticSession
from typing import List, Dict
import json
from pathlib import Path

class ECommerceScra per:
    """Scrape product data from e-commerce sites."""
    
    def __init__(self, base_url: str):
        self.driver = None
        self.client = None
        self.session = None
        self.base_url = base_url
        self.products = []
    
    def __enter__(self):
        """Start browser and create session."""
        self.driver = Browser4Driver()
        self.driver.start()
        
        self.client = PulsarClient(base_url=self.driver.base_url)
        self.client.create_session()
        self.session = AgenticSession(self.client)
        
        return self
    
    def __exit__(self, exc_type, exc_val, exc_tb):
        """Clean up resources."""
        if self.session:
            self.session.close()
        if self.client:
            self.client.close()
        if self.driver:
            self.driver.stop()
    
    def scrape_product_page(self, url: str) -> Dict:
        """Scrape a single product page."""
        print(f"Scraping: {url}")
        
        page = self.session.open(url)
        driver = self.session.driver
        
        # Wait for product details to load
        driver.wait_for_selector(".product-details", timeout=10000)
        
        # Extract product data
        product = {
            "url": url,
            "name": driver.select_first_text_or_null("h1.product-name"),
            "price": driver.select_first_text_or_null(".price-current"),
            "original_price": driver.select_first_text_or_null(".price-original"),
            "description": driver.select_first_text_or_null(".product-description"),
            "rating": driver.select_first_attribute_or_null(".rating", "data-score"),
            "review_count": driver.select_first_text_or_null(".review-count"),
            "availability": driver.select_first_text_or_null(".availability-status"),
            "brand": driver.select_first_text_or_null(".brand-name"),
            "sku": driver.select_first_attribute_or_null("[data-sku]", "data-sku"),
            "images": driver.select_attribute_all(".product-image img", "src"),
            "features": driver.select_text_all(".feature-item"),
        }
        
        # Extract specifications
        spec_keys = driver.select_text_all(".spec-name")
        spec_values = driver.select_text_all(".spec-value")
        product["specifications"] = dict(zip(spec_keys, spec_values))
        
        return product
    
    def scrape_category_page(self, url: str, max_products: int = 50) -> List[str]:
        """Extract product URLs from category page."""
        print(f"Scraping category: {url}")
        
        page = self.session.open(url)
        driver = self.session.driver
        
        product_urls = []
        page_num = 1
        
        while len(product_urls) < max_products:
            # Wait for products to load
            driver.wait_for_selector(".product-card", timeout=10000)
            
            # Extract product links
            links = driver.select_attribute_all(".product-card a.product-link", "href")
            
            for link in links:
                if len(product_urls) >= max_products:
                    break
                
                # Convert relative to absolute URL
                if link.startswith("/"):
                    link = f"{self.base_url}{link}"
                
                if link not in product_urls:
                    product_urls.append(link)
            
            # Check for next page
            next_button = driver.exists(".pagination .next:not(.disabled)")
            if not next_button or len(product_urls) >= max_products:
                break
            
            # Navigate to next page
            print(f"Loading page {page_num + 1}...")
            driver.click(".pagination .next")
            driver.delay(2000)
            page_num += 1
        
        return product_urls[:max_products]
    
    def scrape_category(self, category_url: str, max_products: int = 50):
        """Scrape all products in a category."""
        # Get product URLs
        product_urls = self.scrape_category_page(category_url, max_products)
        print(f"Found {len(product_urls)} products")
        
        # Scrape each product
        for i, url in enumerate(product_urls, 1):
            try:
                print(f"[{i}/{len(product_urls)}] Scraping product...")
                product = self.scrape_product_page(url)
                self.products.append(product)
                
                # Delay between requests
                self.session.driver.delay(1000)
                
            except Exception as e:
                print(f"Error scraping {url}: {e}")
                continue
    
    def save_results(self, output_file: str = "products.json"):
        """Save scraped products to JSON file."""
        output_path = Path(output_file)
        
        with open(output_path, 'w', encoding='utf-8') as f:
            json.dump(self.products, f, indent=2, ensure_ascii=False)
        
        print(f"Saved {len(self.products)} products to {output_file}")
    
    def get_statistics(self) -> Dict:
        """Get scraping statistics."""
        if not self.products:
            return {}
        
        total = len(self.products)
        with_images = sum(1 for p in self.products if p.get("images"))
        with_reviews = sum(1 for p in self.products if p.get("review_count"))
        
        return {
            "total_products": total,
            "with_images": with_images,
            "with_reviews": with_reviews,
            "avg_features": sum(len(p.get("features", [])) for p in self.products) / total
        }

def main():
    """Main scraping workflow."""
    
    # Configuration
    BASE_URL = "https://shop.example.com"
    CATEGORY_URL = f"{BASE_URL}/electronics/laptops"
    MAX_PRODUCTS = 20
    
    # Run scraper
    with ECommerceScraper(BASE_URL) as scraper:
        # Scrape category
        scraper.scrape_category(CATEGORY_URL, max_products=MAX_PRODUCTS)
        
        # Save results
        scraper.save_results("laptop_products.json")
        
        # Print statistics
        stats = scraper.get_statistics()
        print("\nScraping Statistics:")
        print(f"  Total products: {stats['total_products']}")
        print(f"  With images: {stats['with_images']}")
        print(f"  With reviews: {stats['with_reviews']}")
        print(f"  Avg features per product: {stats['avg_features']:.1f}")

if __name__ == "__main__":
    main()
```

## Example 2: News Article Aggregator

Collect and process news articles from multiple sources.

```python
"""
News Article Aggregator
Scrapes articles from multiple news sites and generates summaries.
"""

from browser4 import Browser4Driver, PulsarClient, AgenticSession
from typing import List, Dict
from datetime import datetime
import json

class NewsAggregator:
    """Aggregate news articles from multiple sources."""
    
    def __init__(self, sources: List[Dict[str, str]]):
        self.sources = sources
        self.articles = []
        self.driver = None
        self.client = None
        self.session = None
    
    def __enter__(self):
        self.driver = Browser4Driver()
        self.driver.start()
        self.client = PulsarClient(base_url=self.driver.base_url)
        self.client.create_session()
        self.session = AgenticSession(self.client)
        return self
    
    def __exit__(self, exc_type, exc_val, exc_tb):
        if self.session:
            self.session.close()
        if self.client:
            self.client.close()
        if self.driver:
            self.driver.stop()
    
    def scrape_article(self, url: str, source_name: str) -> Dict:
        """Scrape a single article."""
        print(f"Scraping article from {source_name}: {url}")
        
        try:
            page = self.session.open(url)
            driver = self.session.driver
            
            # Extract article data
            article = {
                "url": url,
                "source": source_name,
                "scraped_at": datetime.now().isoformat(),
                "title": driver.select_first_text_or_null("h1, .article-title"),
                "author": driver.select_first_text_or_null(".author-name, .byline"),
                "date": driver.select_first_text_or_null("time, .publish-date"),
                "category": driver.select_first_text_or_null(".category, .section"),
                "content": driver.select_text_all("article p, .article-content p"),
                "tags": driver.select_text_all(".tag, .topic"),
            }
            
            # Generate AI summary
            try:
                summary = self.session.summarize(
                    "Provide a 2-3 sentence summary of this article"
                )
                article["ai_summary"] = summary
            except Exception as e:
                print(f"Could not generate summary: {e}")
                article["ai_summary"] = None
            
            return article
            
        except Exception as e:
            print(f"Error scraping article {url}: {e}")
            return None
    
    def scrape_source(self, source: Dict[str, str], max_articles: int = 10):
        """Scrape articles from a single source."""
        source_name = source["name"]
        source_url = source["url"]
        article_selector = source["article_selector"]
        
        print(f"\n=== Scraping {source_name} ===")
        
        try:
            page = self.session.open(source_url)
            driver = self.session.driver
            
            # Wait for articles to load
            driver.wait_for_selector(article_selector, timeout=10000)
            
            # Extract article URLs
            article_links = driver.select_attribute_all(
                f"{article_selector} a",
                "href"
            )
            
            print(f"Found {len(article_links)} article links")
            
            # Scrape each article
            for i, link in enumerate(article_links[:max_articles], 1):
                # Make absolute URL
                if link.startswith("/"):
                    link = f"{source['base_url']}{link}"
                
                article = self.scrape_article(link, source_name)
                if article:
                    self.articles.append(article)
                    print(f"[{i}/{min(len(article_links), max_articles)}] Scraped: {article['title'][:50]}...")
                
                # Delay between requests
                driver.delay(1000)
                
        except Exception as e:
            print(f"Error scraping source {source_name}: {e}")
    
    def scrape_all(self, max_per_source: int = 10):
        """Scrape articles from all sources."""
        for source in self.sources:
            self.scrape_source(source, max_articles=max_per_source)
    
    def filter_by_keyword(self, keyword: str) -> List[Dict]:
        """Filter articles by keyword in title or content."""
        keyword_lower = keyword.lower()
        
        filtered = []
        for article in self.articles:
            title = (article.get("title") or "").lower()
            content = " ".join(article.get("content", [])).lower()
            
            if keyword_lower in title or keyword_lower in content:
                filtered.append(article)
        
        return filtered
    
    def save_results(self, output_file: str = "articles.json"):
        """Save articles to JSON file."""
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(self.articles, f, indent=2, ensure_ascii=False)
        
        print(f"\nSaved {len(self.articles)} articles to {output_file}")
    
    def generate_report(self) -> str:
        """Generate a summary report."""
        if not self.articles:
            return "No articles scraped."
        
        sources = {}
        for article in self.articles:
            source = article.get("source", "Unknown")
            sources[source] = sources.get(source, 0) + 1
        
        report = f"News Aggregation Report\n"
        report += f"{'=' * 50}\n"
        report += f"Total articles: {len(self.articles)}\n"
        report += f"Sources:\n"
        for source, count in sources.items():
            report += f"  - {source}: {count} articles\n"
        
        return report

def main():
    """Main news aggregation workflow."""
    
    # Configure news sources
    sources = [
        {
            "name": "Tech News",
            "base_url": "https://technews.example.com",
            "url": "https://technews.example.com/latest",
            "article_selector": ".article-card"
        },
        {
            "name": "Science Daily",
            "base_url": "https://sciencedaily.example.com",
            "url": "https://sciencedaily.example.com/news",
            "article_selector": ".news-item"
        },
    ]
    
    # Run aggregator
    with NewsAggregator(sources) as aggregator:
        # Scrape all sources
        aggregator.scrape_all(max_per_source=5)
        
        # Filter by keyword
        ai_articles = aggregator.filter_by_keyword("artificial intelligence")
        print(f"\nFound {len(ai_articles)} articles about AI")
        
        # Save results
        aggregator.save_results("tech_news.json")
        
        # Print report
        print("\n" + aggregator.generate_report())

if __name__ == "__main__":
    main()
```

## Example 3: Competitive Price Monitor

Monitor competitor prices and send alerts.

```python
"""
Competitive Price Monitor
Tracks product prices across multiple competitors and detects changes.
"""

from browser4 import Browser4Driver, PulsarClient, AgenticSession
from typing import List, Dict
import json
from datetime import datetime
from pathlib import Path

class PriceMonitor:
    """Monitor product prices across competitors."""
    
    def __init__(self, products: List[Dict]):
        self.products = products
        self.price_history = self.load_history()
        self.driver = None
        self.client = None
        self.session = None
    
    def __enter__(self):
        self.driver = Browser4Driver()
        self.driver.start()
        self.client = PulsarClient(base_url=self.driver.base_url)
        self.client.create_session()
        self.session = AgenticSession(self.client)
        return self
    
    def __exit__(self, exc_type, exc_val, exc_tb):
        if self.session:
            self.session.close()
        if self.client:
            self.client.close()
        if self.driver:
            self.driver.stop()
        self.save_history()
    
    def load_history(self) -> Dict:
        """Load price history from file."""
        history_file = Path("price_history.json")
        if history_file.exists():
            with open(history_file, 'r') as f:
                return json.load(f)
        return {}
    
    def save_history(self):
        """Save price history to file."""
        with open("price_history.json", 'w') as f:
            json.dump(self.price_history, f, indent=2)
    
    def extract_price(self, text: str) -> float:
        """Extract numeric price from text."""
        import re
        if not text:
            return 0.0
        
        # Remove currency symbols and extract number
        match = re.search(r'[\d,]+\.?\d*', text.replace(',', ''))
        if match:
            return float(match.group())
        return 0.0
    
    def check_price(self, product: Dict) -> Dict:
        """Check current price for a product."""
        url = product["url"]
        name = product["name"]
        price_selector = product["price_selector"]
        
        print(f"Checking price for: {name}")
        
        try:
            page = self.session.open(url)
            driver = self.session.driver
            
            # Wait for price element
            driver.wait_for_selector(price_selector, timeout=10000)
            
            # Extract price
            price_text = driver.select_first_text_or_null(price_selector)
            price = self.extract_price(price_text)
            
            # Check availability
            in_stock = not driver.exists(".out-of-stock")
            
            result = {
                "product": name,
                "url": url,
                "price": price,
                "price_text": price_text,
                "in_stock": in_stock,
                "checked_at": datetime.now().isoformat(),
            }
            
            # Compare with history
            product_key = url
            if product_key in self.price_history:
                last_price = self.price_history[product_key]["price"]
                result["price_change"] = price - last_price
                result["price_change_pct"] = ((price - last_price) / last_price * 100) if last_price > 0 else 0
            else:
                result["price_change"] = 0
                result["price_change_pct"] = 0
            
            # Update history
            self.price_history[product_key] = result
            
            return result
            
        except Exception as e:
            print(f"Error checking price for {name}: {e}")
            return None
    
    def check_all_prices(self) -> List[Dict]:
        """Check prices for all products."""
        results = []
        
        for product in self.products:
            result = self.check_price(product)
            if result:
                results.append(result)
            
            # Delay between checks
            self.session.driver.delay(2000)
        
        return results
    
    def generate_alerts(self, results: List[Dict], threshold_pct: float = 5.0) -> List[str]:
        """Generate price change alerts."""
        alerts = []
        
        for result in results:
            change_pct = result.get("price_change_pct", 0)
            
            if abs(change_pct) >= threshold_pct:
                direction = "increased" if change_pct > 0 else "decreased"
                alert = (
                    f"ALERT: {result['product']} price {direction} by "
                    f"{abs(change_pct):.1f}% to ${result['price']:.2f}"
                )
                alerts.append(alert)
            
            if not result.get("in_stock"):
                alerts.append(f"ALERT: {result['product']} is out of stock!")
        
        return alerts
    
    def generate_report(self, results: List[Dict]) -> str:
        """Generate price monitoring report."""
        report = f"Price Monitoring Report\n"
        report += f"Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n"
        report += f"{'=' * 70}\n\n"
        
        for result in results:
            report += f"Product: {result['product']}\n"
            report += f"Price: ${result['price']:.2f}\n"
            
            if result['price_change'] != 0:
                change_pct = result['price_change_pct']
                direction = "↑" if change_pct > 0 else "↓"
                report += f"Change: {direction} ${abs(result['price_change']):.2f} ({change_pct:+.1f}%)\n"
            
            report += f"In Stock: {'Yes' if result['in_stock'] else 'No'}\n"
            report += f"URL: {result['url']}\n"
            report += f"{'-' * 70}\n"
        
        return report

def main():
    """Main price monitoring workflow."""
    
    # Configure products to monitor
    products = [
        {
            "name": "Laptop Model X",
            "url": "https://shop.example.com/laptop-x",
            "price_selector": ".price-current"
        },
        {
            "name": "Smartphone Y",
            "url": "https://shop.example.com/phone-y",
            "price_selector": ".product-price"
        },
        {
            "name": "Headphones Z",
            "url": "https://competitor.example.com/headphones-z",
            "price_selector": ".price"
        },
    ]
    
    # Run price monitor
    with PriceMonitor(products) as monitor:
        # Check all prices
        results = monitor.check_all_prices()
        
        # Generate alerts
        alerts = monitor.generate_alerts(results, threshold_pct=5.0)
        
        if alerts:
            print("\n=== PRICE ALERTS ===")
            for alert in alerts:
                print(f"  {alert}")
        else:
            print("\n No significant price changes detected.")
        
        # Generate report
        report = monitor.generate_report(results)
        print("\n" + report)
        
        # Save report
        with open("price_report.txt", 'w') as f:
            f.write(report)
        print("Report saved to price_report.txt")

if __name__ == "__main__":
    main()
```

## Example 4: Social Media Content Collector

Collect and analyze social media content.

```python
"""
Social Media Content Collector
Collects posts, metrics, and engagement data from social media platforms.
"""

from browser4 import Browser4Driver, PulsarClient, AgenticSession
from typing import List, Dict
import json
from datetime import datetime

class SocialMediaCollector:
    """Collect content from social media platforms."""
    
    def __init__(self):
        self.posts = []
        self.driver = None
        self.client = None
        self.session = None
    
    def __enter__(self):
        self.driver = Browser4Driver()
        self.driver.start()
        self.client = PulsarClient(base_url=self.driver.base_url)
        self.client.create_session()
        self.session = AgenticSession(self.client)
        return self
    
    def __exit__(self, exc_type, exc_val, exc_tb):
        if self.session:
            self.session.close()
        if self.client:
            self.client.close()
        if self.driver:
            self.driver.stop()
    
    def collect_posts(self, profile_url: str, max_posts: int = 20):
        """Collect posts from a social media profile."""
        print(f"Collecting posts from: {profile_url}")
        
        page = self.session.open(profile_url)
        driver = self.session.driver
        
        # Wait for posts to load
        driver.wait_for_selector(".post", timeout=10000)
        
        posts_collected = 0
        scroll_attempts = 0
        max_scrolls = 10
        
        while posts_collected < max_posts and scroll_attempts < max_scrolls:
            # Extract visible posts
            post_elements = driver.select_text_all(".post")
            current_count = len(post_elements)
            
            if current_count > posts_collected:
                # Extract new posts
                for i in range(posts_collected, min(current_count, max_posts)):
                    post = self.extract_post(i + 1)
                    if post:
                        self.posts.append(post)
                        posts_collected += 1
                
                print(f"Collected {posts_collected} posts...")
            
            # Scroll to load more
            if posts_collected < max_posts:
                driver.scroll_down(3)
                driver.delay(2000)
                scroll_attempts += 1
        
        print(f"Total posts collected: {len(self.posts)}")
    
    def extract_post(self, post_index: int) -> Dict:
        """Extract data from a single post."""
        driver = self.session.driver
        base_selector = f".post:nth-child({post_index})"
        
        try:
            post = {
                "collected_at": datetime.now().isoformat(),
                "text": driver.select_first_text_or_null(f"{base_selector} .post-text"),
                "author": driver.select_first_text_or_null(f"{base_selector} .author-name"),
                "timestamp": driver.select_first_text_or_null(f"{base_selector} .timestamp"),
                "likes": driver.select_first_text_or_null(f"{base_selector} .like-count"),
                "comments": driver.select_first_text_or_null(f"{base_selector} .comment-count"),
                "shares": driver.select_first_text_or_null(f"{base_selector} .share-count"),
                "image_url": driver.select_first_attribute_or_null(f"{base_selector} img", "src"),
                "post_url": driver.select_first_attribute_or_null(f"{base_selector} a.permalink", "href"),
            }
            
            return post
        except Exception as e:
            print(f"Error extracting post {post_index}: {e}")
            return None
    
    def analyze_sentiment(self, text: str) -> str:
        """Analyze sentiment using AI."""
        # Use AI for sentiment analysis
        try:
            extraction = self.session.agent_extract(
                instruction=f"Analyze the sentiment of this text: '{text}'. Return 'positive', 'negative', or 'neutral'",
                schema={"type": "string", "enum": ["positive", "negative", "neutral"]}
            )
            if extraction.success:
                return extraction.data
        except:
            pass
        return "unknown"
    
    def save_results(self, output_file: str = "social_posts.json"):
        """Save collected posts."""
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(self.posts, f, indent=2, ensure_ascii=False)
        print(f"Saved {len(self.posts)} posts to {output_file}")

def main():
    """Main social media collection workflow."""
    
    with SocialMediaCollector() as collector:
        # Collect posts
        collector.collect_posts(
            "https://socialmedia.example.com/user/techcompany",
            max_posts=50
        )
        
        # Save results
        collector.save_results("tech_company_posts.json")
        
        # Print statistics
        total = len(collector.posts)
        with_images = sum(1 for p in collector.posts if p.get("image_url"))
        
        print(f"\nStatistics:")
        print(f"  Total posts: {total}")
        print(f"  With images: {with_images}")

if __name__ == "__main__":
    main()
```

## Running the Examples

To run any of these examples:

1. Ensure Browser4 SDK is installed:
```bash
pip install browser4
# or
uv pip install browser4
```

2. Set up environment variables (for AI features):
```bash
export OPENROUTER_API_KEY="your-api-key"
```

3. Run the script:
```bash
python ecommerce_scraper.py
python news_aggregator.py
python price_monitor.py
python social_media_collector.py
```

## Next Steps

- [Basic Usage](basic-usage.md) - Learn fundamental operations
- [Advanced Usage](advanced-usage.md) - Complex patterns and optimization
- [AI Automation](ai-automation.md) - AI-powered browser control
- [Configuration](../configuration/browser4-driver.md) - Configure Browser4Driver
