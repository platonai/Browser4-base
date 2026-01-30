# AI Automation Examples

This guide demonstrates how to use Pulsar's AI-powered automation capabilities for intelligent web tasks. These examples showcase autonomous navigation, form filling, content discovery, and complex multi-step workflows.

## Overview

AI automation enables your scripts to:
- Understand page context and content
- Fill forms intelligently
- Navigate websites autonomously
- Extract relevant information
- Handle dynamic interactions
- Adapt to different page layouts

## Prerequisites

```python
from pulsar_sdk import PulsarSession, AgenticSession
import json
from datetime import datetime
from typing import Dict, List, Optional
```

---

## Example 1: Intelligent Form Filling and Submission

### Problem Statement
Automatically fill out complex web forms using AI to understand field requirements, validate inputs, and handle various form types (contact forms, registration, search).

### Complete Code

```python
from pulsar_sdk import AgenticSession
import json
from datetime import datetime

def fill_contact_form(url: str, form_data: Dict, output_file: str = "form_submission.json"):
    """
    Intelligently fill and submit a contact form using AI.
    
    Args:
        url: URL of the page containing the form
        form_data: Dictionary with form field data
        output_file: Path to save submission results
    """
    with AgenticSession() as session:
        print(f"Navigating to: {url}")
        
        # Navigate to the form page
        page = session.load(url)
        
        print("Analyzing form structure...")
        
        # Use AI to identify and fill form fields
        result = session.execute_task(
            f"""
            Fill out the contact form on this page with the following information:
            - Name: {form_data.get('name', '')}
            - Email: {form_data.get('email', '')}
            - Subject: {form_data.get('subject', '')}
            - Message: {form_data.get('message', '')}
            
            Find the appropriate fields, fill them correctly, and submit the form.
            Wait for confirmation and report the result.
            """
        )
        
        # Capture submission result
        submission_data = {
            "url": url,
            "submitted_at": datetime.now().isoformat(),
            "form_data": form_data,
            "result": result,
            "success": "success" in result.lower() or "thank" in result.lower(),
            "confirmation_message": extract_confirmation_message(session)
        }
        
        # Save results
        with open(output_file, "w", encoding="utf-8") as f:
            json.dump(submission_data, f, indent=2, ensure_ascii=False)
        
        print(f"\n{'='*60}")
        print(f"Form Submission {'Successful' if submission_data['success'] else 'Failed'}")
        print(f"Confirmation: {submission_data['confirmation_message']}")
        print(f"Results saved to: {output_file}")
        print(f"{'='*60}")
        
        return submission_data

def extract_confirmation_message(session) -> str:
    """Extract confirmation or success message from the page."""
    current_page = session.current_page()
    
    # Look for common confirmation selectors
    selectors = [
        ".success-message",
        ".confirmation",
        ".alert-success",
        "[role='alert']",
        ".thank-you"
    ]
    
    for selector in selectors:
        element = current_page.select_first(selector)
        if element:
            return element.text.strip()
    
    # Use AI to find confirmation message
    try:
        result = session.execute_task(
            "Find and extract the confirmation or success message on this page."
        )
        return result
    except:
        return "Could not extract confirmation message"

def fill_registration_form(url: str, user_data: Dict):
    """
    Fill a user registration form with validation handling.
    
    Args:
        url: Registration page URL
        user_data: User information dictionary
    """
    with AgenticSession() as session:
        print(f"Starting registration at: {url}")
        
        page = session.load(url)
        
        # Fill registration form with AI assistance
        result = session.execute_task(
            f"""
            Complete the user registration form with this information:
            - Username: {user_data['username']}
            - Email: {user_data['email']}
            - Password: {user_data['password']}
            - Confirm Password: {user_data['password']}
            - First Name: {user_data.get('first_name', '')}
            - Last Name: {user_data.get('last_name', '')}
            - Phone: {user_data.get('phone', '')}
            - Date of Birth: {user_data.get('dob', '')}
            
            Handle any checkboxes for terms of service or newsletter subscription.
            If there are captchas or verification steps, report them.
            Submit the form and report the outcome.
            """
        )
        
        print(f"\nRegistration result: {result}")
        return result

def fill_search_form(url: str, search_query: str, filters: Optional[Dict] = None):
    """
    Fill a search form with filters and execute the search.
    
    Args:
        url: Search page URL
        search_query: Main search query
        filters: Optional dictionary of filter criteria
    """
    with AgenticSession() as session:
        print(f"Navigating to search page: {url}")
        
        page = session.load(url)
        
        # Build filter instructions
        filter_instructions = ""
        if filters:
            filter_instructions = "Apply these filters:\n"
            for key, value in filters.items():
                filter_instructions += f"- {key}: {value}\n"
        
        # Execute search with AI
        result = session.execute_task(
            f"""
            Perform a search with the following criteria:
            
            Search query: {search_query}
            {filter_instructions}
            
            Fill out the search form, apply all filters, and execute the search.
            Wait for results to load and report how many results were found.
            """
        )
        
        print(f"\nSearch executed: {result}")
        
        # Extract search results
        results_page = session.current_page()
        results = extract_search_results(results_page)
        
        return {
            "query": search_query,
            "filters": filters,
            "execution_result": result,
            "results": results
        }

def extract_search_results(page) -> List[Dict]:
    """Extract search results from the results page."""
    results = []
    
    # Common result item selectors
    result_items = page.select(".search-result, .result-item, article.result, .product-card")
    
    for item in result_items[:10]:  # Limit to first 10 results
        result = {
            "title": item.select_first("h2, h3, .title")?.text?.strip(),
            "link": item.select_first("a")?.attr("href"),
            "description": item.select_first(".description, .excerpt, p")?.text?.strip(),
            "price": item.select_first(".price")?.text?.strip()
        }
        results.append(result)
    
    return results

# Example usage
if __name__ == "__main__":
    # Example 1: Contact form
    contact_data = {
        "name": "John Doe",
        "email": "john.doe@example.com",
        "subject": "Product Inquiry",
        "message": "I'm interested in learning more about your enterprise solutions."
    }
    
    submission = fill_contact_form(
        "https://example.com/contact",
        contact_data,
        "contact_submission.json"
    )
    
    # Example 2: Registration form
    user_data = {
        "username": "johndoe123",
        "email": "john.doe@example.com",
        "password": "SecurePass123!",
        "first_name": "John",
        "last_name": "Doe",
        "phone": "+1-555-0123",
        "dob": "1990-01-15"
    }
    
    registration_result = fill_registration_form(
        "https://example.com/register",
        user_data
    )
    
    # Example 3: Search with filters
    search_results = fill_search_form(
        "https://example-shop.com/search",
        "wireless headphones",
        filters={
            "price_range": "$100-$300",
            "brand": "Sony",
            "rating": "4 stars and above",
            "shipping": "Free shipping"
        }
    )
    
    print(f"\nFound {len(search_results['results'])} products")
```

### Expected Output

```
Navigating to: https://example.com/contact
Analyzing form structure...

============================================================
Form Submission Successful
Confirmation: Thank you! Your message has been received and we'll respond within 24 hours.
Results saved to: contact_submission.json
============================================================

Starting registration at: https://example.com/register

Registration result: Registration successful. Welcome email sent to john.doe@example.com

Navigating to search page: https://example-shop.com/search

Search executed: Search completed. Found 24 products matching your criteria.

Found 10 products
```

### Key Concepts

1. **AI Task Execution**: Let AI understand and interact with forms
2. **Context-Aware Filling**: AI adapts to different form structures
3. **Validation Handling**: AI can handle form validation errors
4. **Result Verification**: Extract and confirm submission results
5. **Multi-step Forms**: Handle forms that span multiple pages

---

## Example 2: Autonomous Navigation and Data Discovery

### Problem Statement
Navigate through a website autonomously to discover and extract specific information without knowing the exact page structure.

### Complete Code

```python
from pulsar_sdk import AgenticSession
import json
from datetime import datetime
from typing import List, Dict

def discover_product_catalog(base_url: str, category: str, max_products: int = 20):
    """
    Autonomously navigate to a product category and extract product information.
    
    Args:
        base_url: Website base URL
        category: Product category to find
        max_products: Maximum number of products to extract
    """
    with AgenticSession() as session:
        print(f"Starting product discovery on: {base_url}")
        print(f"Looking for category: {category}\n")
        
        # Navigate to homepage
        session.load(base_url)
        
        # Use AI to find and navigate to the category
        navigation_result = session.execute_task(
            f"""
            Find and navigate to the "{category}" category or section.
            Look for navigation menus, category links, or search functionality.
            Once you reach the category page, confirm you're on the correct page.
            """
        )
        
        print(f"Navigation: {navigation_result}\n")
        
        # Extract products from the category page
        extraction_result = session.execute_task(
            f"""
            Extract information for up to {max_products} products on this page:
            - Product name
            - Price
            - Rating
            - Description or brief summary
            - Link to product detail page
            
            Return the information in a structured format.
            """
        )
        
        # Parse extracted data
        products = parse_ai_extraction(extraction_result)
        
        # Enhance data by visiting product pages
        detailed_products = []
        for idx, product in enumerate(products[:5], 1):  # Visit first 5 products
            print(f"[{idx}/5] Enhancing product data: {product.get('name', 'Unknown')}")
            
            if product.get('link'):
                detailed = get_product_details(session, product['link'])
                detailed_products.append({**product, **detailed})
        
        # Save results
        results = {
            "base_url": base_url,
            "category": category,
            "discovered_at": datetime.now().isoformat(),
            "total_products": len(products),
            "detailed_products": detailed_products,
            "all_products": products
        }
        
        output_file = f"products_{category.replace(' ', '_')}.json"
        with open(output_file, "w", encoding="utf-8") as f:
            json.dump(results, f, indent=2, ensure_ascii=False)
        
        print(f"\n{'='*60}")
        print(f"Product Discovery Complete!")
        print(f"  Category: {category}")
        print(f"  Products found: {len(products)}")
        print(f"  Detailed: {len(detailed_products)}")
        print(f"  Saved to: {output_file}")
        print(f"{'='*60}")
        
        return results

def get_product_details(session, product_url: str) -> Dict:
    """Get detailed information from a product page."""
    try:
        session.load(product_url)
        
        details = session.execute_task(
            """
            Extract detailed product information:
            - Full specifications
            - Available sizes/colors/variants
            - Shipping information
            - Customer review count
            - Product images (URLs)
            """
        )
        
        return parse_ai_extraction(details)
    except Exception as e:
        print(f"  Error getting details: {e}")
        return {}

def navigate_and_extract_blog_posts(blog_url: str, topic: str, max_posts: int = 10):
    """
    Find and extract blog posts about a specific topic.
    
    Args:
        blog_url: Blog homepage URL
        topic: Topic to search for
        max_posts: Maximum posts to extract
    """
    with AgenticSession() as session:
        print(f"Searching blog for topic: {topic}")
        
        session.load(blog_url)
        
        # Search or navigate to relevant posts
        search_result = session.execute_task(
            f"""
            Find blog posts about "{topic}". You can:
            1. Use the search functionality if available
            2. Browse categories or tags
            3. Scroll through recent posts
            
            Navigate to find posts related to this topic.
            """
        )
        
        print(f"Search: {search_result}\n")
        
        # Extract blog post information
        posts_result = session.execute_task(
            f"""
            Extract information for up to {max_posts} blog posts about {topic}:
            - Post title
            - Author
            - Publication date
            - Summary or excerpt
            - Link to full post
            - Tags or categories
            
            Return structured data.
            """
        )
        
        posts = parse_ai_extraction(posts_result)
        
        # Get full content for top posts
        detailed_posts = []
        for idx, post in enumerate(posts[:3], 1):
            print(f"[{idx}/3] Reading full post: {post.get('title', 'Unknown')}")
            
            if post.get('link'):
                session.load(post['link'])
                
                content = session.execute_task(
                    "Extract the full article content, including all text and any code examples."
                )
                
                post['full_content'] = content
                detailed_posts.append(post)
        
        return {
            "topic": topic,
            "total_posts_found": len(posts),
            "posts": detailed_posts,
            "discovered_at": datetime.now().isoformat()
        }

def explore_documentation(docs_url: str, query: str):
    """
    Navigate documentation to find information about a specific query.
    
    Args:
        docs_url: Documentation site URL
        query: What to search for in the documentation
    """
    with AgenticSession() as session:
        print(f"Exploring documentation: {docs_url}")
        print(f"Query: {query}\n")
        
        session.load(docs_url)
        
        # Navigate to relevant documentation
        result = session.execute_task(
            f"""
            Find documentation about: {query}
            
            Steps:
            1. Use search if available
            2. Browse navigation/table of contents
            3. Navigate to the most relevant page
            4. Extract the key information
            5. If there are related pages, note them
            
            Provide a comprehensive summary of what you found.
            """
        )
        
        print(f"Documentation found:\n{result}")
        
        return {
            "query": query,
            "findings": result,
            "url": session.current_url(),
            "timestamp": datetime.now().isoformat()
        }

def parse_ai_extraction(ai_response: str) -> List[Dict]:
    """
    Parse AI extraction response into structured data.
    Handles various response formats.
    """
    # Try to find JSON in the response
    import re
    
    # Look for JSON array or object
    json_pattern = r'\[.*\]|\{.*\}'
    match = re.search(json_pattern, ai_response, re.DOTALL)
    
    if match:
        try:
            data = json.loads(match.group())
            if isinstance(data, list):
                return data
            elif isinstance(data, dict):
                return [data]
        except json.JSONDecodeError:
            pass
    
    # Fallback: parse structured text
    return parse_structured_text(ai_response)

def parse_structured_text(text: str) -> List[Dict]:
    """Parse structured text response into list of dictionaries."""
    items = []
    current_item = {}
    
    for line in text.split('\n'):
        line = line.strip()
        if not line:
            if current_item:
                items.append(current_item)
                current_item = {}
            continue
        
        if ':' in line:
            key, value = line.split(':', 1)
            current_item[key.strip().lower().replace(' ', '_')] = value.strip()
    
    if current_item:
        items.append(current_item)
    
    return items if items else [{"raw_response": text}]

# Example usage
if __name__ == "__main__":
    # Example 1: Discover products
    products = discover_product_catalog(
        "https://example-electronics.com",
        "Wireless Headphones",
        max_products=20
    )
    
    # Example 2: Find blog posts
    posts = navigate_and_extract_blog_posts(
        "https://tech-blog.example.com",
        "machine learning",
        max_posts=10
    )
    
    print(f"\nFound {len(posts['posts'])} detailed blog posts")
    
    # Example 3: Search documentation
    docs = explore_documentation(
        "https://docs.example.com",
        "authentication and API keys"
    )
```

### Expected Output

```
Starting product discovery on: https://example-electronics.com
Looking for category: Wireless Headphones

Navigation: Successfully navigated to Wireless Headphones category. Found 48 products.

[1/5] Enhancing product data: Sony WH-1000XM5
[2/5] Enhancing product data: Bose QuietComfort 45
[3/5] Enhancing product data: Apple AirPods Max
[4/5] Enhancing product data: Sennheiser Momentum 4
[5/5] Enhancing product data: Beats Studio Pro

============================================================
Product Discovery Complete!
  Category: Wireless Headphones
  Products found: 48
  Detailed: 5
  Saved to: products_Wireless_Headphones.json
============================================================

Searching blog for topic: machine learning
Search: Found search box and searched for "machine learning". Results page loaded with 15 articles.

[1/3] Reading full post: Introduction to Neural Networks
[2/3] Reading full post: Machine Learning Best Practices
[3/3] Reading full post: Deep Learning Frameworks Comparison

Found 3 detailed blog posts

Exploring documentation: https://docs.example.com
Query: authentication and API keys

Documentation found:
Found comprehensive documentation on authentication. Key points:
1. API keys are generated in the dashboard under Settings > API
2. Two types: Public keys (client-side) and Secret keys (server-side)
3. Authentication uses Bearer token in headers: Authorization: Bearer YOUR_API_KEY
4. Rate limits: 1000 requests/hour for free tier, 10000 for pro tier
5. Related pages: OAuth integration, Webhooks, Security best practices
```

### Key Concepts

1. **Autonomous Navigation**: AI finds its way through websites
2. **Context Understanding**: AI understands page context and content
3. **Adaptive Extraction**: Works with different layouts and structures
4. **Multi-page Workflows**: Navigate through multiple pages automatically
5. **Intelligent Parsing**: Extract structured data from AI responses

---

## Example 3: Content Analysis and Comparison

### Problem Statement
Analyze and compare content from multiple sources, identifying key differences, trends, and insights.

### Complete Code

```python
from pulsar_sdk import AgenticSession
import json
from datetime import datetime
from typing import List, Dict, Tuple

def compare_products(product_urls: List[str], output_file: str = "comparison.json"):
    """
    Compare multiple products across different websites.
    
    Args:
        product_urls: List of product page URLs to compare
        output_file: Output file for comparison results
    """
    with AgenticSession() as session:
        print(f"Comparing {len(product_urls)} products...\n")
        
        products_data = []
        
        # Extract data from each product
        for idx, url in enumerate(product_urls, 1):
            print(f"[{idx}/{len(product_urls)}] Analyzing: {url}")
            
            session.load(url)
            
            product_info = session.execute_task(
                """
                Extract comprehensive product information:
                - Product name and model
                - Brand
                - Price and any discounts
                - Key specifications
                - Features and benefits
                - Customer rating and review count
                - Pros and cons if mentioned
                - Warranty and return policy
                """
            )
            
            products_data.append({
                "url": url,
                "data": parse_ai_extraction(product_info)
            })
        
        # Compare products using AI
        print("\nPerforming comparison analysis...")
        
        comparison_prompt = "Compare these products:\n\n"
        for idx, product in enumerate(products_data, 1):
            comparison_prompt += f"Product {idx}:\n{json.dumps(product['data'], indent=2)}\n\n"
        
        comparison_prompt += """
        Provide:
        1. Side-by-side comparison of key specifications
        2. Price comparison and value analysis
        3. Strengths and weaknesses of each
        4. Best use cases for each product
        5. Overall recommendation
        """
        
        comparison = session.execute_task(comparison_prompt)
        
        # Save results
        results = {
            "compared_at": datetime.now().isoformat(),
            "products": products_data,
            "comparison_analysis": comparison,
            "summary": extract_comparison_summary(comparison)
        }
        
        with open(output_file, "w", encoding="utf-8") as f:
            json.dump(results, f, indent=2, ensure_ascii=False)
        
        print(f"\n{'='*60}")
        print(f"Product Comparison Complete!")
        print(f"Products analyzed: {len(product_urls)}")
        print(f"Results saved to: {output_file}")
        print(f"{'='*60}")
        print(f"\nComparison Summary:")
        print(comparison)
        
        return results

def extract_comparison_summary(comparison_text: str) -> Dict:
    """Extract key points from comparison analysis."""
    return {
        "best_value": extract_section(comparison_text, "best value", "recommendation"),
        "best_features": extract_section(comparison_text, "strengths", "features"),
        "recommendation": extract_section(comparison_text, "recommendation", "conclusion")
    }

def extract_section(text: str, *keywords: str) -> str:
    """Extract section containing specific keywords."""
    lines = text.split('\n')
    relevant_lines = []
    
    for line in lines:
        if any(keyword in line.lower() for keyword in keywords):
            relevant_lines.append(line.strip())
    
    return ' '.join(relevant_lines) if relevant_lines else ""

def analyze_competitor_pricing(competitor_urls: List[str], product_name: str):
    """
    Analyze pricing across competitors for a specific product.
    
    Args:
        competitor_urls: List of competitor website URLs
        product_name: Product to search for
    """
    with AgenticSession() as session:
        print(f"Analyzing pricing for: {product_name}\n")
        
        pricing_data = []
        
        for idx, url in enumerate(competitor_urls, 1):
            print(f"[{idx}/{len(competitor_urls)}] Checking: {url}")
            
            session.load(url)
            
            # Search for the product
            search_result = session.execute_task(
                f"""
                Search for "{product_name}" on this website.
                Find the product and extract:
                - Exact product name
                - Current price
                - Any discounts or promotions
                - Availability status
                - Shipping cost if shown
                """
            )
            
            pricing_data.append({
                "competitor": url,
                "data": parse_ai_extraction(search_result)
            })
        
        # Analyze pricing
        prices = []
        for item in pricing_data:
            data = item['data']
            if isinstance(data, list) and data:
                data = data[0]
            if isinstance(data, dict) and 'price' in str(data).lower():
                prices.append(item)
        
        if prices:
            analysis = {
                "product": product_name,
                "analyzed_at": datetime.now().isoformat(),
                "competitor_count": len(prices),
                "pricing": prices,
                "lowest_price": find_lowest_price(prices),
                "highest_price": find_highest_price(prices),
                "average_price": calculate_average_price(prices)
            }
            
            print(f"\nPricing Analysis:")
            print(f"  Competitors checked: {len(prices)}")
            print(f"  Price range: {analysis['lowest_price']} - {analysis['highest_price']}")
            print(f"  Average: {analysis['average_price']}")
            
            return analysis
        
        return {"error": "No pricing data found"}

def find_lowest_price(pricing_data: List[Dict]) -> str:
    """Find the lowest price from pricing data."""
    # Simplified implementation
    return "Analysis required"

def find_highest_price(pricing_data: List[Dict]) -> str:
    """Find the highest price from pricing data."""
    # Simplified implementation
    return "Analysis required"

def calculate_average_price(pricing_data: List[Dict]) -> str:
    """Calculate average price."""
    # Simplified implementation
    return "Analysis required"

def track_content_changes(url: str, check_interval_minutes: int = 60, checks: int = 3):
    """
    Monitor a page for content changes over time.
    
    Args:
        url: URL to monitor
        check_interval_minutes: Minutes between checks
        checks: Number of checks to perform
    """
    import time
    
    with AgenticSession() as session:
        print(f"Monitoring content changes at: {url}")
        print(f"Checks: {checks}, Interval: {check_interval_minutes} minutes\n")
        
        snapshots = []
        
        for check_num in range(checks):
            print(f"Check {check_num + 1}/{checks} at {datetime.now().strftime('%H:%M:%S')}")
            
            session.load(url)
            
            # Extract key content
            content = session.execute_task(
                """
                Extract the main content of this page:
                - Headlines
                - Key text content
                - Important links
                - Any dynamic elements (prices, stock status, etc.)
                """
            )
            
            snapshot = {
                "check_number": check_num + 1,
                "timestamp": datetime.now().isoformat(),
                "content": content
            }
            
            snapshots.append(snapshot)
            
            if check_num < checks - 1:
                print(f"Waiting {check_interval_minutes} minutes...\n")
                time.sleep(check_interval_minutes * 60)
        
        # Analyze changes
        print("Analyzing changes...")
        
        changes_detected = []
        for i in range(1, len(snapshots)):
            if snapshots[i]['content'] != snapshots[i-1]['content']:
                changes_detected.append({
                    "between_checks": f"{i} and {i+1}",
                    "timestamp": snapshots[i]['timestamp'],
                    "change_description": "Content changed"
                })
        
        results = {
            "url": url,
            "monitoring_period": {
                "start": snapshots[0]['timestamp'],
                "end": snapshots[-1]['timestamp']
            },
            "total_checks": checks,
            "changes_detected": len(changes_detected),
            "snapshots": snapshots,
            "changes": changes_detected
        }
        
        print(f"\nMonitoring Complete!")
        print(f"Changes detected: {len(changes_detected)}")
        
        return results

# Example usage
if __name__ == "__main__":
    # Example 1: Compare products
    product_comparison = compare_products([
        "https://amazon.com/product/sony-wh1000xm5",
        "https://bestbuy.com/product/sony-wh1000xm5",
        "https://target.com/product/sony-wh1000xm5"
    ], "sony_headphones_comparison.json")
    
    # Example 2: Analyze competitor pricing
    pricing = analyze_competitor_pricing([
        "https://amazon.com",
        "https://bestbuy.com",
        "https://target.com",
        "https://walmart.com"
    ], "Sony WH-1000XM5 Headphones")
    
    # Example 3: Monitor content changes (simplified for demo)
    changes = track_content_changes(
        "https://example.com/products/hot-deals",
        check_interval_minutes=60,
        checks=3
    )
```

### Expected Output

```
Comparing 3 products...

[1/3] Analyzing: https://amazon.com/product/sony-wh1000xm5
[2/3] Analyzing: https://bestbuy.com/product/sony-wh1000xm5
[3/3] Analyzing: https://target.com/product/sony-wh1000xm5

Performing comparison analysis...

============================================================
Product Comparison Complete!
Products analyzed: 3
Results saved to: sony_headphones_comparison.json
============================================================

Comparison Summary:
Price Comparison:
- Amazon: $398.00 (Regular price)
- Best Buy: $379.99 (Sale price - Best Value)
- Target: $399.99 (Regular price, includes RedCard discount)

Key Differences:
- Amazon offers fastest shipping (Prime)
- Best Buy has in-store pickup option
- Target provides 5% off with RedCard

Recommendation: Best Buy offers the best price currently. If you have Amazon Prime and need it quickly, Amazon is a good alternative despite higher price.
```

### Key Concepts

1. **Multi-source Analysis**: Compare data from multiple sources
2. **Intelligent Comparison**: AI understands and compares products
3. **Change Detection**: Monitor pages for content changes
4. **Competitive Analysis**: Analyze competitor offerings
5. **Structured Reporting**: Generate comprehensive comparison reports

---

## Example 4: Multi-Step Interactive Workflows

### Problem Statement
Execute complex workflows that require multiple steps, decisions, and interactions.

### Complete Code

```python
from pulsar_sdk import AgenticSession
import json
from datetime import datetime
from typing import Dict, List

def complete_online_purchase_workflow(
    product_name: str,
    store_url: str,
    user_info: Dict,
    max_price: float
):
    """
    Complete multi-step purchase workflow with AI automation.
    
    Args:
        product_name: Product to purchase
        store_url: Online store URL
        user_info: User information for checkout
        max_price: Maximum acceptable price
    """
    workflow_log = []
    
    with AgenticSession() as session:
        print(f"Starting purchase workflow for: {product_name}")
        print(f"Store: {store_url}")
        print(f"Max price: ${max_price}\n")
        
        # Step 1: Navigate to store and search
        print("[Step 1] Searching for product...")
        session.load(store_url)
        
        search_result = session.execute_task(
            f'Search for "{product_name}" and navigate to the product page.'
        )
        workflow_log.append({"step": 1, "action": "search", "result": search_result})
        print(f"  ✓ {search_result}\n")
        
        # Step 2: Check price and availability
        print("[Step 2] Checking price and availability...")
        
        price_check = session.execute_task(
            f"""
            Extract:
            - Current price
            - Availability status
            - Shipping options
            
            If price is above ${max_price}, report that it exceeds budget.
            """
        )
        workflow_log.append({"step": 2, "action": "price_check", "result": price_check})
        
        # Parse price from result
        if "exceeds budget" in price_check.lower():
            print(f"  ✗ Price exceeds budget\n")
            return {"success": False, "reason": "price_too_high", "log": workflow_log}
        
        print(f"  ✓ {price_check}\n")
        
        # Step 3: Add to cart
        print("[Step 3] Adding to cart...")
        
        add_cart = session.execute_task(
            "Add this product to the shopping cart. Confirm it was added successfully."
        )
        workflow_log.append({"step": 3, "action": "add_to_cart", "result": add_cart})
        print(f"  ✓ {add_cart}\n")
        
        # Step 4: Proceed to checkout
        print("[Step 4] Proceeding to checkout...")
        
        checkout = session.execute_task(
            "Navigate to the checkout page."
        )
        workflow_log.append({"step": 4, "action": "checkout", "result": checkout})
        print(f"  ✓ {checkout}\n")
        
        # Step 5: Fill shipping information
        print("[Step 5] Filling shipping information...")
        
        shipping = session.execute_task(
            f"""
            Fill in the shipping information:
            - Name: {user_info['name']}
            - Address: {user_info['address']}
            - City: {user_info['city']}
            - State: {user_info['state']}
            - ZIP: {user_info['zip']}
            - Phone: {user_info['phone']}
            
            Continue to next step.
            """
        )
        workflow_log.append({"step": 5, "action": "shipping_info", "result": shipping})
        print(f"  ✓ {shipping}\n")
        
        # Step 6: Review order (but don't complete payment)
        print("[Step 6] Reviewing order...")
        
        review = session.execute_task(
            """
            Extract the order summary:
            - Items
            - Subtotal
            - Shipping cost
            - Tax
            - Total
            
            DO NOT complete the purchase. Just extract and report the information.
            """
        )
        workflow_log.append({"step": 6, "action": "order_review", "result": review})
        print(f"  ✓ Order review complete\n")
        
        # Save workflow results
        results = {
            "success": True,
            "product": product_name,
            "store": store_url,
            "completed_at": datetime.now().isoformat(),
            "workflow_log": workflow_log,
            "order_summary": parse_ai_extraction(review)
        }
        
        print(f"{'='*60}")
        print(f"Workflow Complete (Payment NOT processed)")
        print(f"Order ready for review")
        print(f"{'='*60}")
        
        return results

def research_and_book_travel(destination: str, dates: Dict, preferences: Dict):
    """
    Research and prepare travel booking workflow.
    
    Args:
        destination: Travel destination
        dates: Check-in and check-out dates
        preferences: Travel preferences (hotel type, budget, etc.)
    """
    with AgenticSession() as session:
        print(f"Planning trip to: {destination}")
        print(f"Dates: {dates['check_in']} to {dates['check_out']}\n")
        
        workflow_results = {}
        
        # Step 1: Research hotels
        print("[Step 1] Researching hotels...")
        session.load("https://www.booking.com")
        
        hotel_search = session.execute_task(
            f"""
            Search for hotels in {destination}:
            - Check-in: {dates['check_in']}
            - Check-out: {dates['check_out']}
            - Budget: ${preferences['budget_per_night']} per night
            - Rating: {preferences['min_rating']} stars or higher
            
            Find and list top 5 options with prices and ratings.
            """
        )
        workflow_results['hotel_options'] = parse_ai_extraction(hotel_search)
        print(f"  ✓ Found {len(workflow_results['hotel_options'])} hotel options\n")
        
        # Step 2: Research flights
        print("[Step 2] Researching flights...")
        session.load("https://www.google.com/flights")
        
        flight_search = session.execute_task(
            f"""
            Search for flights to {destination}:
            - Departure: {dates['departure_date']}
            - Return: {dates['return_date']}
            - From: {preferences['departure_city']}
            
            Find best options considering price and duration.
            """
        )
        workflow_results['flight_options'] = parse_ai_extraction(flight_search)
        print(f"  ✓ Found flight options\n")
        
        # Step 3: Research activities
        print("[Step 3] Researching activities...")
        session.load(f"https://www.tripadvisor.com")
        
        activities = session.execute_task(
            f"""
            Find top activities and attractions in {destination}:
            - Focus on: {', '.join(preferences.get('interests', []))}
            - List top 10 with ratings and descriptions
            """
        )
        workflow_results['activities'] = parse_ai_extraction(activities)
        print(f"  ✓ Found activity recommendations\n")
        
        # Create comprehensive travel plan
        travel_plan = {
            "destination": destination,
            "dates": dates,
            "preferences": preferences,
            "research_date": datetime.now().isoformat(),
            "recommendations": workflow_results
        }
        
        # Save travel plan
        output_file = f"travel_plan_{destination.replace(' ', '_')}.json"
        with open(output_file, "w", encoding="utf-8") as f:
            json.dump(travel_plan, f, indent=2, ensure_ascii=False)
        
        print(f"{'='*60}")
        print(f"Travel Research Complete!")
        print(f"Plan saved to: {output_file}")
        print(f"{'='*60}")
        
        return travel_plan

def automated_job_application(job_url: str, resume_data: Dict):
    """
    Automate job application process.
    
    Args:
        job_url: Job posting URL
        resume_data: Structured resume information
    """
    with AgenticSession() as session:
        print(f"Applying to job: {job_url}\n")
        
        # Navigate to job posting
        session.load(job_url)
        
        # Extract job details
        print("[Step 1] Analyzing job posting...")
        job_details = session.execute_task(
            """
            Extract job information:
            - Job title
            - Company name
            - Required qualifications
            - Job description
            - Application requirements
            """
        )
        print(f"  ✓ Job analyzed\n")
        
        # Fill application form
        print("[Step 2] Filling application form...")
        application = session.execute_task(
            f"""
            Fill out the job application with this information:
            
            Personal Information:
            - Name: {resume_data['name']}
            - Email: {resume_data['email']}
            - Phone: {resume_data['phone']}
            - Location: {resume_data['location']}
            
            Work Experience:
            {json.dumps(resume_data['experience'], indent=2)}
            
            Education:
            {json.dumps(resume_data['education'], indent=2)}
            
            Skills:
            {', '.join(resume_data['skills'])}
            
            Fill all required fields. If there's a cover letter field, generate an appropriate cover letter based on the job requirements.
            
            DO NOT submit the application. Stop after filling all fields.
            """
        )
        print(f"  ✓ Application filled\n")
        
        return {
            "job_url": job_url,
            "job_details": parse_ai_extraction(job_details),
            "application_status": "ready_for_review",
            "completed_at": datetime.now().isoformat()
        }

# Example usage
if __name__ == "__main__":
    # Example 1: Purchase workflow
    purchase_result = complete_online_purchase_workflow(
        product_name="Sony WH-1000XM5 Wireless Headphones",
        store_url="https://www.bestbuy.com",
        user_info={
            "name": "John Doe",
            "address": "123 Main St",
            "city": "San Francisco",
            "state": "CA",
            "zip": "94102",
            "phone": "+1-555-0123"
        },
        max_price=450.00
    )
    
    print(f"\nPurchase workflow: {'Success' if purchase_result['success'] else 'Failed'}")
    
    # Example 2: Travel research
    travel_plan = research_and_book_travel(
        destination="Paris, France",
        dates={
            "check_in": "2024-06-15",
            "check_out": "2024-06-22",
            "departure_date": "2024-06-15",
            "return_date": "2024-06-22"
        },
        preferences={
            "budget_per_night": 150,
            "min_rating": 4,
            "departure_city": "New York",
            "interests": ["museums", "food", "history"]
        }
    )
    
    # Example 3: Job application
    job_app = automated_job_application(
        "https://example-jobs.com/posting/senior-developer",
        resume_data={
            "name": "Jane Smith",
            "email": "jane.smith@example.com",
            "phone": "+1-555-0199",
            "location": "Seattle, WA",
            "experience": [
                {"title": "Senior Developer", "company": "Tech Corp", "years": "2020-2024"},
                {"title": "Developer", "company": "StartupX", "years": "2018-2020"}
            ],
            "education": [
                {"degree": "BS Computer Science", "school": "State University", "year": "2018"}
            ],
            "skills": ["Python", "JavaScript", "React", "AWS", "Docker"]
        }
    )
```

### Expected Output

```
Starting purchase workflow for: Sony WH-1000XM5 Wireless Headphones
Store: https://www.bestbuy.com
Max price: $450.0

[Step 1] Searching for product...
  ✓ Found product: Sony WH-1000XM5 - Black. Navigated to product page.

[Step 2] Checking price and availability...
  ✓ Price: $399.99. In stock. Free shipping available.

[Step 3] Adding to cart...
  ✓ Product added to cart successfully.

[Step 4] Proceeding to checkout...
  ✓ Now on checkout page.

[Step 5] Filling shipping information...
  ✓ Shipping information entered. Proceeding to payment.

[Step 6] Reviewing order...
  ✓ Order review complete

============================================================
Workflow Complete (Payment NOT processed)
Order ready for review
============================================================

Purchase workflow: Success
```

### Key Concepts

1. **Multi-Step Automation**: Chain multiple actions together
2. **Decision Making**: AI makes decisions based on conditions
3. **State Management**: Track workflow progress
4. **Error Recovery**: Handle failures at any step
5. **Safety**: Review before final actions (payments, submissions)

---

## Best Practices for AI Automation

### 1. Clear Task Instructions
```python
# Good: Specific and clear
task = "Click the 'Add to Cart' button and wait for confirmation"

# Bad: Vague
task = "Buy it"
```

### 2. Break Down Complex Tasks
```python
# Break into steps
step1 = session.execute_task("Find the search box and enter 'headphones'")
step2 = session.execute_task("Click the search button")
step3 = session.execute_task("Select the first result")
```

### 3. Validate Results
```python
result = session.execute_task("Fill the form")
if "success" not in result.lower():
    print("Form filling may have failed")
```

### 4. Handle Errors Gracefully
```python
try:
    session.execute_task("Complex task")
except Exception as e:
    print(f"Task failed: {e}")
    # Implement fallback or retry
```

### 5. Provide Context
```python
# Good: Provide context
task = f"""
Current page: Product listing
Goal: Find and click on the product named "{product_name}"
After clicking, wait for the product detail page to load
"""
```

---

## Next Steps

- Explore [Complex Workflows](complex-workflows.md) for enterprise-scale automation
- Review [Simple Scraping](simple-scraping.md) for data extraction basics
- Check the [API Reference](../api-reference/client.md) for complete documentation
