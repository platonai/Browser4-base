# Browser4 Python SDK Examples

This directory contains example scripts demonstrating various features of the Browser4 Python SDK.

## Prerequisites

Before running these examples, ensure you have:

1. **Installed the SDK**:
   ```bash
   # From the python-sdk directory
   uv sync --extra dev
   ```

2. **Running Browser4 Server**:
   - Download Browser4.jar from the releases page
   - Start the server: `java -jar Browser4.jar`
   - Default URL: `http://localhost:8182`

3. **API Key (for AI features)**:
   ```bash
   # bash/zsh
   export OPENROUTER_API_KEY="your-api-key-here"

   # PowerShell
   $env:OPENROUTER_API_KEY = "your-api-key-here"
   ```

## Examples

### 1. basic_usage.py

Demonstrates fundamental SDK operations:
- Creating a client and session
- Loading and parsing pages
- Extracting data with CSS selectors
- Using WebDriver for navigation
- Basic AI-powered actions

**Run:**
```bash
python examples/basic_usage.py
```

### 2. agentic_session_example.py

Showcases advanced AI-powered features:
- Single actions with `act()`
- Multi-step autonomous tasks with `run()`
- Page observation and analysis
- AI-powered data extraction
- Content summarization
- Agent state history tracking

**Run:**
```bash
python examples/agentic_session_example.py
```

**Note:** Requires AI capabilities enabled on the server.

### 3. webdriver_example.py

Demonstrates low-level browser control:
- Navigation and page control
- Element interaction (click, fill, type)
- Scrolling operations
- Content extraction
- Screenshots
- JavaScript execution

**Run:**
```bash
python examples/webdriver_example.py
```

### 4. driver_usage.py

Demonstrates Browser4Driver lifecycle management:
- Automatic download and startup using context manager
- Manual control with custom configuration
- Custom port and Java options

**Run:**
```bash
python examples/driver_usage.py
```

### 5. fused_acts_style_example.py

Mirrors the Kotlin FusedActsStyleExample to demonstrate API consistency across languages:
- Three-layer architecture: PulsarSession, WebDriver, Agent
- Page loading, parsing, and extraction
- AI-powered actions and multi-step tasks
- Process trace tracking

**Run:**
```bash
python examples/fused_acts_style_example.py
```

**Note:** Requires a running Browser4 server and `OPENROUTER_API_KEY` environment variable.

## Example Output

Each example includes detailed console output showing:
- Step-by-step operations
- Results and extracted data
- Success/failure indicators
- Helpful error messages

## Troubleshooting

### Connection Refused
If you see connection errors, ensure:
- Browser4 server is running on `http://localhost:8182`
- No firewall blocking the connection
- Check server logs for errors

### AI Features Not Working
For AI-powered features:
- Verify `OPENROUTER_API_KEY` is set
- Ensure server has AI capabilities enabled
- Check server logs for AI model access issues

### Import Errors
If imports fail:
```bash
# Install in development mode
cd /path/to/python-sdk
uv sync --extra dev

# Or run examples without activating a venv
uv run python examples/basic_usage.py
```

## Further Reading

- [Python SDK README](../README.md) - Full API documentation
- [Kotlin SDK Examples](../../kotlin-sdk-examples/src/main/kotlin/ai/platon/pulsar/sdk/examples/) - Kotlin equivalents
- [Browser4 Documentation](../../../docs/) - Complete platform documentation

## Contributing

Feel free to contribute additional examples! Please ensure:
- Examples are well-documented
- Include error handling
- Follow the existing style
- Test before submitting
