# Browser4 Rust SDK

Rust SDK for Browser4 based on OpenAPI specification.

This SDK provides a Rust interface to the Browser4 browser automation platform,
enabling web scraping, data extraction, and AI-powered browser interaction.

## Features

- **Session Management**: Create, manage, and delete browser sessions
- **Navigation**: Navigate to URLs, go back/forward, reload pages
- **Element Interaction**: Click, fill, type, press keys, hover, focus
- **Scrolling**: Scroll down/up, scroll to elements, scroll to top/bottom
- **Content Extraction**: Extract text, attributes, and HTML content
- **Screenshots**: Capture screenshots of pages or elements
- **Script Execution**: Execute JavaScript in the browser
- **AI-Powered Automation**: Natural language commands for browser interaction

## Installation

Add the dependency to your `Cargo.toml`:

```toml
[dependencies]
browser4-sdk-rust = "4.6.0-SNAPSHOT"
tokio = { version = "1.36", features = ["full"] }
```

## Quick Start

### Basic Usage with PulsarSession

```rust
use browser4::prelude::*;
use std::sync::Arc;
use std::collections::HashMap;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    // Create client and session
    let client = Arc::new(PulsarClient::new());
    client.create_session().await?;

    let mut session = PulsarSession::new(client.clone());

    // Load a page
    let page = session.load("https://example.com", Some("-expire 1d")).await?;
    println!("Loaded page: {}", page.url);

    // Parse and extract data
    if let Some(document) = session.parse(&page) {
        let mut selectors = HashMap::new();
        selectors.insert("title".to_string(), "h1".to_string());
        let fields = session.extract(&document, &selectors);
        println!("Title: {:?}", fields.get("title"));
    }

    // Navigate and interact using WebDriver
    let mut driver = session.get_or_create_driver();
    driver.navigate_to("https://example.com").await?;
    println!("Current URL: {}", driver.current_url().await?);

    // Extract data
    if let Some(title) = driver.select_first_text("h1").await? {
        println!("Title: {}", title);
    }

    // Clean up
    client.delete_session().await?;
    Ok(())
}
```

### AI-Powered Automation with AgenticSession

```rust
use browser4::prelude::*;
use std::sync::Arc;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let client = Arc::new(PulsarClient::new());
    client.create_session().await?;

    let mut session = AgenticSession::new(client);

    // Navigate to a page
    session.session_mut()
        .get_or_create_driver()
        .navigate_to("https://example.com")
        .await?;

    // Use natural language to interact
    let act_result = session.act(
        "click the search button",
        false,
        None,
        None,
        None,
        None
    ).await?;
    println!("Action success: {}", act_result.success);

    // Run autonomous task
    let run_result = session.run(
        "search for 'rust' and click first result",
        false,
        None,
        None,
        None,
        None
    ).await?;
    println!("Task success: {}", run_result.success);

    // Observe page state
    let observation = session.observe(
        Some("find all interactive elements"),
        None,
        None,
        None,
        true
    ).await?;
    for obs in observation.observations {
        if let Some(desc) = obs.description {
            println!("Found: {}", desc);
        }
    }

    // AI-powered extraction
    let extraction = session.extract(
        "extract the main heading and first paragraph",
        None,
        None,
        None,
        None
    ).await?;
    println!("Extracted data: {:?}", extraction.data);

    // Summarize page content
    let summary = session.summarize(None, None).await?;
    println!("Summary: {}", summary);

    Ok(())
}
```

### WebDriver Usage

```rust
use browser4::prelude::*;
use std::sync::Arc;
use std::collections::HashMap;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let client = Arc::new(PulsarClient::new());
    client.create_session().await?;
    let mut driver = WebDriver::new(client.clone());

    // Navigation
    driver.navigate_to("https://example.com").await?;
    println!("URL: {}", driver.current_url().await?);
    println!("Title: {}", driver.title().await?);

    // Element interaction
    driver.click("button.submit").await?;
    driver.fill("input[name='search']", "rust").await?;
    driver.press("input[name='search']", "Enter").await?;

    // Wait for elements
    driver.wait_for_selector(".results", None).await?;

    // Scrolling
    driver.scroll_to_bottom().await?;
    driver.scroll_to(".footer").await?;

    // Content extraction
    let texts = driver.select_text_all(".result-item").await?;
    for text in texts {
        println!("{}", text);
    }

    // Extract multiple fields
    let mut fields = HashMap::new();
    fields.insert("title".to_string(), "h1".to_string());
    fields.insert("description".to_string(), ".description".to_string());
    fields.insert("price".to_string(), ".price".to_string());
    let extracted = driver.extract(&fields).await?;
    println!("Fields: {:?}", extracted);

    // Screenshots
    let screenshot = driver.screenshot(None).await?;
    println!("Screenshot captured: {}", screenshot.is_some());

    // Execute JavaScript
    let result = driver.execute_script(
        "return document.querySelectorAll('a').length",
        None
    ).await?;
    println!("Links count: {:?}", result);

    client.delete_session().await?;
    Ok(())
}
```

## API Reference

### PulsarClient

Low-level HTTP client for API communication.

| Method | Description |
|--------|-------------|
| `create_session()` | Create a new browser session |
| `delete_session()` | Delete the current session |
| `post(path, body)` | Make a POST request |
| `get(path)` | Make a GET request |
| `delete(path)` | Make a DELETE request |

### PulsarSession

Session management for page loading and extraction.

| Method | Description |
|--------|-------------|
| `normalize(url, args, to_item_option)` | Normalize a URL with load arguments |
| `open(url, args)` | Open a URL immediately (bypass cache) |
| `load(url, args)` | Load from cache or fetch from internet |
| `submit(url, args)` | Submit URL to crawl pool |
| `extract(document, selectors)` | Extract fields using CSS selectors |
| `scrape(url, args, selectors)` | Load, parse, and extract in one operation |

### AgenticSession

AI-powered browser automation.

| Method | Description |
|--------|-------------|
| `act(action, ...)` | Execute a single action in natural language |
| `run(task, ...)` | Run an autonomous agent task |
| `observe(instruction, ...)` | Observe page and suggest actions |
| `extract(instruction, schema, ...)` | AI-powered data extraction |
| `summarize(instruction, ...)` | Generate page summary |
| `clear_history()` | Clear agent history |

### WebDriver

Browser control and element interaction.

**Navigation:**
- `navigate_to(url)`, `current_url()`, `reload()`, `go_back()`, `go_forward()`

**Element Interaction:**
- `click(selector)`, `fill(selector, text)`, `type_text(selector, text)`
- `press(selector, key)`, `hover(selector)`, `focus(selector)`
- `check(selector)`, `uncheck(selector)`

**Waiting:**
- `wait_for_selector(selector, timeout)`, `wait_for_navigation()`
- `exists(selector)`, `is_visible(selector)`, `is_hidden(selector)`

**Scrolling:**
- `scroll_down(count)`, `scroll_up(count)`, `scroll_to(selector)`
- `scroll_to_top()`, `scroll_to_bottom()`, `scroll_to_middle(ratio)`

**Content:**
- `select_first_text(selector)`, `select_text_all(selector)`
- `select_first_attribute(selector, attr)`, `select_attribute_all(selector, attr)`
- `outer_html(selector)`, `text_content(selector)`
- `extract(fields)` - Extract multiple fields at once

**Screenshots:**
- `capture_screenshot(selector, full_page)`, `screenshot(selector)`

**Scripts:**
- `execute_script(script, args)`, `execute_async_script(script, args, timeout)`
- `evaluate(expression)`

**Control:**
- `delay(millis)`, `pause()`, `stop()`

## Server Requirements

The SDK requires a running Browser4 server. By default, it connects to `http://localhost:8182`.

To connect to a different server:

```rust
let client = PulsarClient::with_base_url("http://your-server:8182");
```

## Building

```bash
cargo build
```

## Testing

```bash
cargo test
```

## License

Apache License, Version 2.0

See [LICENSE](../../LICENSE) for details.
