# Screenshots

Capture visual snapshots of web pages and elements using Browser4's screenshot capabilities. Screenshots are useful for debugging, documentation, visual testing, and archiving page states.

## Basic Screenshots

### Full Page Screenshot

Capture the entire visible viewport:

```python
from browser4 import PulsarClient, WebDriver

client = PulsarClient(base_url="http://localhost:8182")
session_id = client.create_session()
driver = WebDriver(client)

# Navigate to page
driver.navigate_to("https://example.com")

# Capture screenshot (returns base64 encoded PNG)
screenshot = driver.capture_screenshot()

if screenshot:
    print(f"Screenshot captured: {len(screenshot)} bytes (base64)")
else:
    print("Screenshot failed")
```

### Element Screenshot

Capture a specific element:

```python
driver = WebDriver(client)
driver.navigate_to("https://example.com")

# Capture specific element
screenshot = driver.capture_screenshot(selector=".main-content")

if screenshot:
    print(f"Element screenshot: {len(screenshot)} bytes")
else:
    print("Element not found or screenshot failed")

# Capture different elements
header_shot = driver.capture_screenshot(selector="header")
footer_shot = driver.capture_screenshot(selector="footer")
article_shot = driver.capture_screenshot(selector="article.main")
```

## Saving Screenshots

### Save to File

```python
import base64
from pathlib import Path

def save_screenshot(driver, filename, selector=None):
    """Capture and save screenshot to file."""
    
    # Capture screenshot
    screenshot_b64 = driver.capture_screenshot(selector=selector)
    
    if not screenshot_b64:
        print(f"Failed to capture screenshot")
        return False
    
    # Decode base64 to bytes
    screenshot_bytes = base64.b64decode(screenshot_b64)
    
    # Save to file
    output_path = Path(filename)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_bytes(screenshot_bytes)
    
    print(f"Screenshot saved to {filename} ({len(screenshot_bytes)} bytes)")
    return True

# Usage
driver.navigate_to("https://example.com")

# Save full page
save_screenshot(driver, "screenshots/full_page.png")

# Save specific element
save_screenshot(driver, "screenshots/header.png", selector="header")
```

### Save Multiple Screenshots

```python
def save_multiple_screenshots(driver, elements):
    """Save screenshots of multiple elements."""
    
    for name, selector in elements.items():
        filename = f"screenshots/{name}.png"
        
        # Check if element exists
        if driver.exists(selector):
            success = save_screenshot(driver, filename, selector)
            if success:
                print(f"✓ Saved {name}")
        else:
            print(f"✗ Element not found: {selector}")

# Usage
driver.navigate_to("https://example.com")

elements_to_capture = {
    "header": "header",
    "navigation": "nav.main-nav",
    "content": "article.main",
    "sidebar": "aside.sidebar",
    "footer": "footer"
}

save_multiple_screenshots(driver, elements_to_capture)
```

## Screenshot Patterns

### Progress Screenshots

Capture screenshots at different stages:

```python
def workflow_with_screenshots(driver, output_dir="screenshots"):
    """Execute workflow and capture screenshots at each step."""
    
    Path(output_dir).mkdir(exist_ok=True)
    
    # Step 1: Initial page
    driver.navigate_to("https://example.com")
    save_screenshot(driver, f"{output_dir}/01_initial.png")
    
    # Step 2: After scrolling
    driver.scroll_down(count=3)
    save_screenshot(driver, f"{output_dir}/02_scrolled.png")
    
    # Step 3: After interaction
    if driver.exists("button.show-more"):
        driver.click("button.show-more")
        driver.wait_for_selector(".expanded-content", timeout=5000)
        save_screenshot(driver, f"{output_dir}/03_expanded.png")
    
    # Step 4: Final state
    driver.scroll_to_bottom()
    save_screenshot(driver, f"{output_dir}/04_bottom.png")
    
    print(f"Screenshots saved to {output_dir}/")

# Usage
workflow_with_screenshots(driver)
```

### Before/After Comparison

```python
def capture_before_after(driver, action_description, action_func):
    """Capture screenshots before and after an action."""
    
    # Before screenshot
    before = driver.capture_screenshot()
    save_screenshot_bytes(before, "before.png")
    print("Before screenshot captured")
    
    # Perform action
    print(f"Performing action: {action_description}")
    action_func()
    
    # Wait for changes
    driver.delay(1000)
    
    # After screenshot
    after = driver.capture_screenshot()
    save_screenshot_bytes(after, "after.png")
    print("After screenshot captured")
    
    return before, after

def save_screenshot_bytes(screenshot_b64, filename):
    """Helper to save base64 screenshot."""
    import base64
    screenshot_bytes = base64.b64decode(screenshot_b64)
    Path(filename).write_bytes(screenshot_bytes)

# Usage
def my_action():
    driver.click("button.toggle")
    driver.wait_for_selector(".modal", timeout=5000)

capture_before_after(
    driver,
    "Open modal dialog",
    my_action
)
```

### Tiled Screenshots

Capture multiple elements in organized files:

```python
import base64
from pathlib import Path

def capture_page_sections(driver, url, output_dir="screenshots/sections"):
    """Capture all major sections of a page."""
    
    driver.navigate_to(url)
    driver.wait_for_selector("body", timeout=10000)
    
    # Create output directory
    Path(output_dir).mkdir(parents=True, exist_ok=True)
    
    # Define sections to capture
    sections = {
        "full_page": None,  # Full viewport
        "header": "header",
        "navigation": "nav",
        "main_content": "main",
        "sidebar": "aside",
        "footer": "footer"
    }
    
    results = {}
    
    for section_name, selector in sections.items():
        filename = f"{output_dir}/{section_name}.png"
        
        try:
            # Capture screenshot
            screenshot = driver.capture_screenshot(selector=selector)
            
            if screenshot:
                # Save to file
                screenshot_bytes = base64.b64decode(screenshot)
                Path(filename).write_bytes(screenshot_bytes)
                
                results[section_name] = {
                    "success": True,
                    "file": filename,
                    "size": len(screenshot_bytes)
                }
                print(f"✓ {section_name}: {len(screenshot_bytes)} bytes")
            else:
                results[section_name] = {"success": False, "error": "Capture failed"}
                print(f"✗ {section_name}: Capture failed")
                
        except Exception as e:
            results[section_name] = {"success": False, "error": str(e)}
            print(f"✗ {section_name}: {e}")
    
    return results

# Usage
results = capture_page_sections(driver, "https://example.com")
print(f"\nCaptured {sum(1 for r in results.values() if r.get('success'))} sections")
```

## Screenshot with Context

### Annotated Screenshots

Capture with metadata:

```python
import json
from datetime import datetime

def capture_with_metadata(driver, filename, selector=None, notes=""):
    """Capture screenshot with metadata."""
    
    # Capture screenshot
    screenshot = driver.capture_screenshot(selector=selector)
    
    if not screenshot:
        return None
    
    # Collect metadata
    metadata = {
        "url": driver.current_url(),
        "title": driver.title(),
        "timestamp": datetime.now().isoformat(),
        "selector": selector,
        "notes": notes,
        "viewport": {
            "scroll_y": driver.evaluate("window.scrollY"),
            "inner_height": driver.evaluate("window.innerHeight")
        }
    }
    
    # Save screenshot
    screenshot_bytes = base64.b64decode(screenshot)
    Path(filename).write_bytes(screenshot_bytes)
    
    # Save metadata
    metadata_file = filename.replace('.png', '_metadata.json')
    Path(metadata_file).write_text(json.dumps(metadata, indent=2))
    
    print(f"Screenshot saved: {filename}")
    print(f"Metadata saved: {metadata_file}")
    
    return metadata

# Usage
driver.navigate_to("https://example.com")
metadata = capture_with_metadata(
    driver,
    "screenshots/page_with_context.png",
    notes="Homepage after login"
)
```

### Visual Testing

Compare screenshots for visual regression:

```python
def visual_regression_test(driver, url, baseline_dir="baseline", current_dir="current"):
    """Capture screenshots for visual regression testing."""
    
    Path(baseline_dir).mkdir(exist_ok=True)
    Path(current_dir).mkdir(exist_ok=True)
    
    # Navigate to page
    driver.navigate_to(url)
    driver.wait_for_selector("body", timeout=10000)
    
    # Capture current screenshot
    screenshot = driver.capture_screenshot()
    
    if not screenshot:
        print("Failed to capture screenshot")
        return False
    
    # Save current
    current_file = f"{current_dir}/screenshot.png"
    screenshot_bytes = base64.b64decode(screenshot)
    Path(current_file).write_bytes(screenshot_bytes)
    
    # Check if baseline exists
    baseline_file = f"{baseline_dir}/screenshot.png"
    
    if not Path(baseline_file).exists():
        # Create baseline
        Path(baseline_file).write_bytes(screenshot_bytes)
        print(f"Baseline created: {baseline_file}")
        return True
    
    # Compare sizes (simple check)
    baseline_size = Path(baseline_file).stat().st_size
    current_size = len(screenshot_bytes)
    size_diff = abs(baseline_size - current_size) / baseline_size * 100
    
    print(f"Baseline: {baseline_size} bytes")
    print(f"Current: {current_size} bytes")
    print(f"Difference: {size_diff:.2f}%")
    
    if size_diff > 5:  # 5% threshold
        print("⚠️  Visual difference detected!")
        return False
    
    print("✓ Visual test passed")
    return True

# Usage
passed = visual_regression_test(driver, "https://example.com")
```

## Complete Screenshot Example

```python
from browser4 import Browser4Driver, PulsarClient, WebDriver
import base64
from pathlib import Path
from datetime import datetime

def comprehensive_screenshot_demo():
    """Comprehensive screenshot capture demonstration."""
    
    with Browser4Driver() as driver_mgr:
        client = PulsarClient(base_url=driver_mgr.base_url)
        session_id = client.create_session()
        driver = WebDriver(client)
        
        try:
            # Create output directory
            output_dir = Path("screenshots") / datetime.now().strftime("%Y%m%d_%H%M%S")
            output_dir.mkdir(parents=True, exist_ok=True)
            
            print(f"Saving screenshots to: {output_dir}")
            
            # Navigate to page
            driver.navigate_to("https://example.com")
            driver.wait_for_selector("body", timeout=10000)
            
            # 1. Full page screenshot
            print("\n1. Capturing full page...")
            full_screenshot = driver.capture_screenshot()
            if full_screenshot:
                screenshot_bytes = base64.b64decode(full_screenshot)
                (output_dir / "full_page.png").write_bytes(screenshot_bytes)
                print(f"   Saved: {len(screenshot_bytes)} bytes")
            
            # 2. Element screenshots
            print("\n2. Capturing page elements...")
            elements = {
                "header": "header, h1",
                "content": "body > div:first-of-type",
                "links": "a"
            }
            
            for name, selector in elements.items():
                if driver.exists(selector):
                    screenshot = driver.capture_screenshot(selector=selector)
                    if screenshot:
                        screenshot_bytes = base64.b64decode(screenshot)
                        (output_dir / f"{name}.png").write_bytes(screenshot_bytes)
                        print(f"   ✓ {name}: {len(screenshot_bytes)} bytes")
                else:
                    print(f"   ✗ {name}: Element not found")
            
            # 3. Scroll and capture
            print("\n3. Capturing at different scroll positions...")
            positions = ["top", "middle", "bottom"]
            
            for position in positions:
                if position == "top":
                    driver.scroll_to_top()
                elif position == "middle":
                    driver.scroll_to_middle(0.5)
                else:
                    driver.scroll_to_bottom()
                
                driver.delay(500)
                screenshot = driver.capture_screenshot()
                
                if screenshot:
                    screenshot_bytes = base64.b64decode(screenshot)
                    (output_dir / f"scroll_{position}.png").write_bytes(screenshot_bytes)
                    print(f"   ✓ {position}: {len(screenshot_bytes)} bytes")
            
            # 4. Interactive state screenshots
            print("\n4. Capturing interactive states...")
            
            # Before interaction
            screenshot = driver.capture_screenshot()
            if screenshot:
                screenshot_bytes = base64.b64decode(screenshot)
                (output_dir / "state_before.png").write_bytes(screenshot_bytes)
                print(f"   ✓ Before state: {len(screenshot_bytes)} bytes")
            
            # After scroll (simulated interaction)
            driver.scroll_down(count=2)
            driver.delay(500)
            
            screenshot = driver.capture_screenshot()
            if screenshot:
                screenshot_bytes = base64.b64decode(screenshot)
                (output_dir / "state_after.png").write_bytes(screenshot_bytes)
                print(f"   ✓ After state: {len(screenshot_bytes)} bytes")
            
            # 5. Summary
            print(f"\n5. Summary:")
            screenshots = list(output_dir.glob("*.png"))
            total_size = sum(f.stat().st_size for f in screenshots)
            print(f"   Total screenshots: {len(screenshots)}")
            print(f"   Total size: {total_size:,} bytes")
            print(f"   Output directory: {output_dir}")
            
        finally:
            driver.close()
            client.close()

if __name__ == "__main__":
    comprehensive_screenshot_demo()
```

## Screenshot Utilities

### Screenshot Helper Class

```python
import base64
from pathlib import Path
from datetime import datetime

class ScreenshotHelper:
    """Helper class for managing screenshots."""
    
    def __init__(self, driver, output_dir="screenshots"):
        self.driver = driver
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(parents=True, exist_ok=True)
        self.counter = 0
    
    def capture(self, name=None, selector=None):
        """Capture and save screenshot."""
        
        # Auto-generate name if not provided
        if name is None:
            self.counter += 1
            timestamp = datetime.now().strftime("%H%M%S")
            name = f"{self.counter:03d}_{timestamp}"
        
        # Capture screenshot
        screenshot = self.driver.capture_screenshot(selector=selector)
        
        if not screenshot:
            print(f"Failed to capture: {name}")
            return None
        
        # Save to file
        filename = self.output_dir / f"{name}.png"
        screenshot_bytes = base64.b64decode(screenshot)
        filename.write_bytes(screenshot_bytes)
        
        print(f"✓ Captured: {filename} ({len(screenshot_bytes)} bytes)")
        return filename
    
    def capture_sequence(self, action_func, prefix="step"):
        """Capture before and after an action."""
        
        # Before
        before_file = self.capture(f"{prefix}_before")
        
        # Execute action
        action_func()
        self.driver.delay(500)
        
        # After
        after_file = self.capture(f"{prefix}_after")
        
        return before_file, after_file
    
    def capture_all_elements(self, selectors):
        """Capture screenshots of multiple elements."""
        
        results = {}
        for name, selector in selectors.items():
            if self.driver.exists(selector):
                file = self.capture(name, selector)
                results[name] = file
            else:
                print(f"✗ Element not found: {name} ({selector})")
                results[name] = None
        
        return results

# Usage
driver = WebDriver(client)
driver.navigate_to("https://example.com")

helper = ScreenshotHelper(driver, output_dir="my_screenshots")

# Single screenshot
helper.capture("homepage")

# Element screenshot
helper.capture("header", selector="header")

# Before/after sequence
helper.capture_sequence(
    lambda: driver.scroll_down(count=3),
    prefix="scroll"
)

# Multiple elements
helper.capture_all_elements({
    "header": "header",
    "nav": "nav",
    "footer": "footer"
})
```

## Troubleshooting

### Screenshot Returns None

```python
# Check page loaded
driver.navigate_to("https://example.com")
driver.wait_for_selector("body", timeout=10000)

# Verify driver is active
url = driver.current_url()
print(f"Current URL: {url}")

# Retry screenshot
screenshot = driver.capture_screenshot()
if not screenshot:
    print("Screenshot failed, retrying...")
    driver.delay(2000)
    screenshot = driver.capture_screenshot()
```

### Element Not in Screenshot

```python
# Scroll element into view first
driver.scroll_to("selector")
driver.delay(500)  # Wait for scroll

# Then capture
screenshot = driver.capture_screenshot(selector="selector")
```

### Screenshot Size Issues

```python
# Check if element exists and is visible
if not driver.exists("selector"):
    print("Element not found")
elif not driver.is_visible("selector"):
    print("Element not visible")
else:
    screenshot = driver.capture_screenshot(selector="selector")
```

### Large Screenshot Files

```python
# Screenshots are PNG format by default
# For large pages, consider capturing specific sections

# Instead of full page
# screenshot = driver.capture_screenshot()

# Capture important sections
header = driver.capture_screenshot(selector="header")
content = driver.capture_screenshot(selector="main")
footer = driver.capture_screenshot(selector="footer")
```

## Best Practices

1. **Create organized directories** - Use dated folders for screenshots
2. **Use meaningful names** - Name files based on content or purpose
3. **Check element existence** - Verify elements exist before capturing
4. **Wait for page stability** - Add delays after navigation/interaction
5. **Handle failures gracefully** - Check for None returns
6. **Clean up old screenshots** - Remove outdated captures periodically
7. **Save metadata** - Include context information with screenshots
8. **Use appropriate selectors** - Capture specific elements when possible
9. **Consider file sizes** - Balance quality with storage needs
10. **Version control exclusion** - Don't commit screenshots to repos

## Performance Tips

### Minimize Screenshot Calls

```python
# Avoid excessive screenshots
# for i in range(100):
#     driver.scroll_down()
#     driver.capture_screenshot()  # Too many captures

# Better: Capture at key points
driver.scroll_to_top()
screenshot1 = driver.capture_screenshot()

driver.scroll_to_middle(0.5)
screenshot2 = driver.capture_screenshot()

driver.scroll_to_bottom()
screenshot3 = driver.capture_screenshot()
```

### Async Screenshot Capture

```python
import asyncio

async def capture_multiple_async(driver, selectors):
    """Capture multiple screenshots (simulated async)."""
    
    # In actual async implementation, these would be parallel
    screenshots = {}
    
    for name, selector in selectors.items():
        screenshot = driver.capture_screenshot(selector=selector)
        if screenshot:
            screenshots[name] = screenshot
    
    return screenshots

# Usage would require async driver implementation
```

### Selective Capturing

```python
# Only capture when needed
def capture_if_changed(driver, previous_url):
    """Capture only if page changed."""
    
    current_url = driver.current_url()
    
    if current_url != previous_url:
        screenshot = driver.capture_screenshot()
        return screenshot, current_url
    
    return None, current_url

# Usage
prev_url = None
for url in urls:
    driver.navigate_to(url)
    screenshot, prev_url = capture_if_changed(driver, prev_url)
```

## Next Steps

- **[Element Interaction](element-interaction.md)** - Interact before capturing
- **[Navigation](navigation.md)** - Navigate to capture different pages
- **[Script Execution](script-execution.md)** - Execute scripts for custom captures
- **[Data Extraction](data-extraction.md)** - Extract data along with screenshots
