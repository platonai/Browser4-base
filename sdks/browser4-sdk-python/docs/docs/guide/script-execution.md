# Script Execution

Execute JavaScript code in the browser context using Browser4's script execution capabilities. Run custom scripts, manipulate the DOM, extract data, and interact with page elements programmatically.

## Basic Script Execution

### execute_script()

Execute JavaScript code and get the return value:

```python
from browser4 import PulsarClient, WebDriver

client = PulsarClient(base_url="http://localhost:8182")
session_id = client.create_session()
driver = WebDriver(client)

# Navigate to page
driver.navigate_to("https://example.com")

# Execute simple script
result = driver.execute_script("return document.title")
print(f"Page title: {result}")

# Execute with multiple statements
result = driver.execute_script("""
    const title = document.title;
    const url = document.URL;
    return {title: title, url: url};
""")
print(f"Result: {result}")
```

### evaluate()

Evaluate JavaScript expressions:

```python
driver = WebDriver(client)
driver.navigate_to("https://example.com")

# Evaluate simple expression
scroll_y = driver.evaluate("window.scrollY")
print(f"Scroll position: {scroll_y}")

# Evaluate complex expression
page_info = driver.evaluate("""
    ({
        width: window.innerWidth,
        height: window.innerHeight,
        scrollY: window.scrollY,
        scrollHeight: document.documentElement.scrollHeight
    })
""")
print(f"Page dimensions: {page_info}")
```

## Differences Between Methods

| Method | Purpose | Use Case |
|--------|---------|----------|
| `execute_script()` | Execute multi-line scripts | Complex logic, DOM manipulation |
| `evaluate()` | Evaluate expressions | Quick property access, calculations |

```python
# execute_script - for statements and logic
result = driver.execute_script("""
    if (document.querySelector('h1')) {
        return document.querySelector('h1').textContent;
    }
    return null;
""")

# evaluate - for expressions
result = driver.evaluate("document.querySelector('h1')?.textContent")
```

## Common Script Patterns

### DOM Queries

```python
driver = WebDriver(client)
driver.navigate_to("https://example.com")

# Count elements
link_count = driver.execute_script("""
    return document.querySelectorAll('a').length;
""")
print(f"Links on page: {link_count}")

# Check element existence
has_header = driver.execute_script("""
    return document.querySelector('header') !== null;
""")
print(f"Has header: {has_header}")

# Get element properties
element_info = driver.execute_script("""
    const el = document.querySelector('.main-content');
    if (!el) return null;
    
    return {
        tagName: el.tagName,
        className: el.className,
        textLength: el.textContent.length,
        childCount: el.children.length
    };
""")
print(f"Element info: {element_info}")
```

### Page Information

```python
# Get page metadata
page_data = driver.execute_script("""
    return {
        title: document.title,
        url: document.URL,
        domain: document.domain,
        referrer: document.referrer,
        lastModified: document.lastModified,
        readyState: document.readyState
    };
""")

print(f"Page metadata: {page_data}")

# Get viewport information
viewport = driver.execute_script("""
    return {
        width: window.innerWidth,
        height: window.innerHeight,
        scrollX: window.scrollX,
        scrollY: window.scrollY,
        devicePixelRatio: window.devicePixelRatio
    };
""")

print(f"Viewport: {viewport}")

# Get document dimensions
doc_size = driver.execute_script("""
    return {
        scrollWidth: document.documentElement.scrollWidth,
        scrollHeight: document.documentElement.scrollHeight,
        clientWidth: document.documentElement.clientWidth,
        clientHeight: document.documentElement.clientHeight
    };
""")

print(f"Document size: {doc_size}")
```

### Scroll Control

```python
# Scroll to position
driver.execute_script("window.scrollTo(0, 500);")

# Scroll by amount
driver.execute_script("window.scrollBy(0, 300);")

# Scroll element into view
driver.execute_script("""
    document.querySelector('.element').scrollIntoView({
        behavior: 'smooth',
        block: 'center'
    });
""")

# Get scroll position
scroll_pos = driver.execute_script("""
    return {
        x: window.scrollX,
        y: window.scrollY,
        maxY: document.documentElement.scrollHeight - window.innerHeight
    };
""")
print(f"Scroll position: {scroll_pos}")
```

### Element Manipulation

```python
# Modify element attributes
driver.execute_script("""
    const el = document.querySelector('input#search');
    if (el) {
        el.value = 'search query';
        el.setAttribute('data-filled', 'true');
    }
""")

# Add CSS class
driver.execute_script("""
    document.querySelectorAll('.item').forEach(el => {
        el.classList.add('highlighted');
    });
""")

# Change styles
driver.execute_script("""
    const el = document.querySelector('.modal');
    if (el) {
        el.style.display = 'none';
        el.style.opacity = '0';
    }
""")

# Insert content
driver.execute_script("""
    const div = document.createElement('div');
    div.textContent = 'Injected content';
    div.className = 'injected';
    document.body.appendChild(div);
""")
```

## Data Extraction with Scripts

### Extract Text Content

```python
# Extract all paragraph text
paragraphs = driver.execute_script("""
    return Array.from(document.querySelectorAll('p'))
        .map(p => p.textContent.trim())
        .filter(text => text.length > 0);
""")

print(f"Found {len(paragraphs)} paragraphs")
for i, p in enumerate(paragraphs[:3], 1):
    print(f"{i}. {p[:100]}...")

# Extract links
links = driver.execute_script("""
    return Array.from(document.querySelectorAll('a[href]'))
        .map(a => ({
            text: a.textContent.trim(),
            href: a.href,
            title: a.title
        }))
        .filter(link => link.text.length > 0);
""")

print(f"Found {len(links)} links")
```

### Extract Structured Data

```python
# Extract table data
table_data = driver.execute_script("""
    const table = document.querySelector('table');
    if (!table) return null;
    
    const headers = Array.from(table.querySelectorAll('th'))
        .map(th => th.textContent.trim());
    
    const rows = Array.from(table.querySelectorAll('tbody tr'))
        .map(tr => {
            const cells = Array.from(tr.querySelectorAll('td'))
                .map(td => td.textContent.trim());
            
            const rowData = {};
            headers.forEach((header, i) => {
                rowData[header] = cells[i] || '';
            });
            return rowData;
        });
    
    return {headers, rows};
""")

if table_data:
    print(f"Headers: {table_data['headers']}")
    print(f"Rows: {len(table_data['rows'])}")

# Extract list data
list_items = driver.execute_script("""
    return Array.from(document.querySelectorAll('.product-list .product'))
        .map(product => ({
            name: product.querySelector('.name')?.textContent.trim(),
            price: product.querySelector('.price')?.textContent.trim(),
            rating: product.querySelector('.rating')?.getAttribute('data-rating'),
            available: product.querySelector('.in-stock') !== null
        }));
""")

print(f"Products: {len(list_items)}")
```

### Extract Metadata

```python
# Extract meta tags
meta_data = driver.execute_script("""
    const metas = {};
    
    // Get all meta tags
    document.querySelectorAll('meta').forEach(meta => {
        const name = meta.getAttribute('name') || meta.getAttribute('property');
        const content = meta.getAttribute('content');
        if (name && content) {
            metas[name] = content;
        }
    });
    
    // Add other metadata
    metas.canonical = document.querySelector('link[rel="canonical"]')?.href;
    metas.description = document.querySelector('meta[name="description"]')?.content;
    metas.keywords = document.querySelector('meta[name="keywords"]')?.content;
    
    return metas;
""")

print("Page metadata:")
for key, value in meta_data.items():
    print(f"  {key}: {value[:100] if value else 'None'}...")
```

## Event Handling

### Trigger Events

```python
# Click event
driver.execute_script("""
    const button = document.querySelector('button.submit');
    if (button) {
        button.click();
    }
""")

# Input event
driver.execute_script("""
    const input = document.querySelector('input#search');
    if (input) {
        input.value = 'search query';
        input.dispatchEvent(new Event('input', {bubbles: true}));
        input.dispatchEvent(new Event('change', {bubbles: true}));
    }
""")

# Custom event
driver.execute_script("""
    const event = new CustomEvent('myCustomEvent', {
        detail: {message: 'Hello from Python'},
        bubbles: true
    });
    document.dispatchEvent(event);
""")

# Form submission
driver.execute_script("""
    const form = document.querySelector('form#login');
    if (form) {
        form.dispatchEvent(new Event('submit', {
            bubbles: true,
            cancelable: true
        }));
    }
""")
```

### Listen to Events

```python
# Add event listener and capture data
driver.execute_script("""
    window._eventData = [];
    
    document.addEventListener('click', function(e) {
        window._eventData.push({
            type: 'click',
            target: e.target.tagName,
            x: e.clientX,
            y: e.clientY,
            timestamp: Date.now()
        });
    });
""")

# Trigger some clicks
driver.click("button.action")

# Retrieve captured data
event_data = driver.execute_script("return window._eventData;")
print(f"Captured {len(event_data)} events")
```

## Complete Script Execution Example

```python
from browser4 import Browser4Driver, PulsarClient, WebDriver

def comprehensive_script_demo():
    """Comprehensive script execution demonstration."""
    
    with Browser4Driver() as driver_mgr:
        client = PulsarClient(base_url=driver_mgr.base_url)
        session_id = client.create_session()
        driver = WebDriver(client)
        
        try:
            # Navigate to page
            driver.navigate_to("https://example.com")
            driver.wait_for_selector("body", timeout=10000)
            
            # 1. Page information
            print("1. Page Information:")
            page_info = driver.execute_script("""
                return {
                    title: document.title,
                    url: document.URL,
                    readyState: document.readyState,
                    linkCount: document.querySelectorAll('a').length,
                    imageCount: document.querySelectorAll('img').length,
                    scriptCount: document.querySelectorAll('script').length
                };
            """)
            
            for key, value in page_info.items():
                print(f"   {key}: {value}")
            
            # 2. Viewport and scroll info
            print("\n2. Viewport Information:")
            viewport = driver.execute_script("""
                return {
                    width: window.innerWidth,
                    height: window.innerHeight,
                    scrollY: window.scrollY,
                    scrollHeight: document.documentElement.scrollHeight,
                    devicePixelRatio: window.devicePixelRatio
                };
            """)
            
            print(f"   Size: {viewport['width']}x{viewport['height']}")
            print(f"   Scroll: {viewport['scrollY']} / {viewport['scrollHeight']}")
            print(f"   DPR: {viewport['devicePixelRatio']}")
            
            # 3. DOM queries
            print("\n3. DOM Queries:")
            heading = driver.execute_script("""
                const h1 = document.querySelector('h1');
                return h1 ? h1.textContent.trim() : null;
            """)
            print(f"   Main heading: {heading}")
            
            # 4. Extract data
            print("\n4. Data Extraction:")
            data = driver.execute_script("""
                return {
                    paragraphs: Array.from(document.querySelectorAll('p'))
                        .map(p => p.textContent.trim())
                        .filter(t => t.length > 0)
                        .slice(0, 3),
                    links: Array.from(document.querySelectorAll('a[href]'))
                        .map(a => ({text: a.textContent.trim(), href: a.href}))
                        .filter(l => l.text.length > 0)
                        .slice(0, 3)
                };
            """)
            
            print(f"   Paragraphs: {len(data['paragraphs'])}")
            print(f"   Links: {len(data['links'])}")
            
            # 5. Manipulate page
            print("\n5. Page Manipulation:")
            driver.execute_script("""
                // Add custom style
                const style = document.createElement('style');
                style.textContent = '.highlight { background: yellow; }';
                document.head.appendChild(style);
                
                // Highlight first paragraph
                const p = document.querySelector('p');
                if (p) p.classList.add('highlight');
            """)
            print("   Added highlight style and marked first paragraph")
            
            # 6. Scroll operations
            print("\n6. Scroll Operations:")
            
            # Scroll to bottom
            driver.execute_script("window.scrollTo(0, document.documentElement.scrollHeight);")
            bottom_pos = driver.evaluate("window.scrollY")
            print(f"   Scrolled to bottom: {bottom_pos}px")
            
            # Scroll to top
            driver.execute_script("window.scrollTo(0, 0);")
            top_pos = driver.evaluate("window.scrollY")
            print(f"   Scrolled to top: {top_pos}px")
            
            # 7. Form interaction
            print("\n7. Form Interaction:")
            has_form = driver.execute_script("""
                const forms = document.querySelectorAll('form');
                return forms.length > 0;
            """)
            print(f"   Has forms: {has_form}")
            
        finally:
            driver.close()
            client.close()

if __name__ == "__main__":
    comprehensive_script_demo()
```

## Advanced Patterns

### Polling with Scripts

```python
def wait_for_condition(driver, condition_script, timeout=10000, interval=500):
    """Wait for a JavaScript condition to be true."""
    
    import time
    start_time = time.time()
    
    while (time.time() - start_time) * 1000 < timeout:
        result = driver.execute_script(f"return {condition_script};")
        if result:
            return True
        time.sleep(interval / 1000)
    
    return False

# Usage
driver.navigate_to("https://example.com")

# Wait for element to appear
success = wait_for_condition(
    driver,
    "document.querySelector('.dynamic-content') !== null",
    timeout=10000
)

if success:
    print("Element appeared")
```

### Script with Arguments

```python
# Pass arguments to script (if supported by implementation)
def set_element_value(driver, selector, value):
    """Set element value using script."""
    
    driver.execute_script(f"""
        const el = document.querySelector('{selector}');
        if (el) {{
            el.value = '{value}';
            el.dispatchEvent(new Event('input', {{bubbles: true}}));
        }}
    """)

# Usage
set_element_value(driver, "input#search", "search query")
```

### Batch Operations

```python
# Perform multiple operations in one script
results = driver.execute_script("""
    const operations = {};
    
    // Operation 1: Count elements
    operations.linkCount = document.querySelectorAll('a').length;
    operations.imageCount = document.querySelectorAll('img').length;
    
    // Operation 2: Extract data
    operations.title = document.title;
    operations.url = document.URL;
    
    // Operation 3: Check conditions
    operations.hasHeader = document.querySelector('header') !== null;
    operations.hasFooter = document.querySelector('footer') !== null;
    
    // Operation 4: Measurements
    operations.scrollHeight = document.documentElement.scrollHeight;
    operations.clientHeight = document.documentElement.clientHeight;
    
    return operations;
""")

print("Batch results:", results)
```

## Troubleshooting

### Script Errors

```python
# Wrap script in try-catch
result = driver.execute_script("""
    try {
        const el = document.querySelector('.might-not-exist');
        return el.textContent;
    } catch (e) {
        return {error: e.message};
    }
""")

if isinstance(result, dict) and 'error' in result:
    print(f"Script error: {result['error']}")
```

### Undefined Results

```python
# Check for null/undefined
result = driver.execute_script("""
    const el = document.querySelector('.element');
    return el ? el.textContent : null;
""")

if result is None:
    print("Element not found or has no content")
```

### Timing Issues

```python
# Wait for page to be ready
driver.execute_script("""
    if (document.readyState !== 'complete') {
        throw new Error('Page not ready');
    }
""")

# Or check ready state
ready = driver.execute_script("return document.readyState === 'complete';")
if not ready:
    print("Page not fully loaded")
```

### Large Data Returns

```python
# Limit data size
limited_data = driver.execute_script("""
    const links = Array.from(document.querySelectorAll('a'));
    
    // Return only first 100 links
    return links.slice(0, 100).map(a => ({
        text: a.textContent.trim().substring(0, 100),
        href: a.href
    }));
""")

print(f"Retrieved {len(limited_data)} links (limited)")
```

## Best Practices

1. **Use return statements** - Always return values explicitly
2. **Handle null values** - Check for null/undefined in scripts
3. **Keep scripts focused** - One script, one purpose
4. **Minimize script size** - Break large scripts into smaller ones
5. **Use try-catch** - Handle errors within scripts
6. **Validate inputs** - Check parameters before executing
7. **Limit data returns** - Don't return huge datasets
8. **Use const/let** - Modern JavaScript best practices
9. **Test scripts in console** - Verify in browser DevTools first
10. **Document complex scripts** - Add comments for clarity

## Performance Tips

### Efficient Queries

```python
# Efficient: Single query with processing
data = driver.execute_script("""
    return Array.from(document.querySelectorAll('a'))
        .map(a => ({text: a.textContent, href: a.href}));
""")

# Less efficient: Multiple separate queries
# links = driver.execute_script("return document.querySelectorAll('a').length;")
# for i in range(links):
#     link = driver.execute_script(f"return document.querySelectorAll('a')[{i}].href;")
```

### Cache Queries

```python
# Store queries in window object
driver.execute_script("""
    if (!window._cachedData) {
        window._cachedData = {
            links: Array.from(document.querySelectorAll('a')),
            images: Array.from(document.querySelectorAll('img'))
        };
    }
""")

# Reuse cached data
link_count = driver.execute_script("return window._cachedData.links.length;")
```

### Minimize Script Calls

```python
# Bad: Multiple script calls
# title = driver.execute_script("return document.title;")
# url = driver.execute_script("return document.URL;")
# links = driver.execute_script("return document.querySelectorAll('a').length;")

# Good: Single script call
page_data = driver.execute_script("""
    return {
        title: document.title,
        url: document.URL,
        linkCount: document.querySelectorAll('a').length
    };
""")
```

## Next Steps

- **[Element Interaction](element-interaction.md)** - Interact with elements
- **[Data Extraction](data-extraction.md)** - Extract data from pages
- **[Screenshots](screenshots.md)** - Capture visual state
- **[Navigation](navigation.md)** - Navigate between pages
