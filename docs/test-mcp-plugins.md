# Testing MCP Plugins with TestMCPServer

This document explains how to use the `TestMCPServer` from `pulsar-tests-common` to test MCP plugin functionality in the `pulsar-agentic` module.

## Overview

`TestMCPServer` is a minimal HTTP-based MCP (Model Context Protocol) server implementation designed specifically for testing. It provides:

- Standard MCP protocol endpoints (`/mcp/info`, `/mcp/list_tools`, `/mcp/call_tool`)
- Three built-in example tools (echo, add, multiply)
- MCP-compliant tool schemas and response formats
- Proper error handling for invalid requests
- No external dependencies - runs entirely in-process

## Quick Start

### 1. Running Tests

The comprehensive test suite demonstrates all TestMCPServer capabilities:

```bash
# Run the MCP plugin test suite
./mvnw -pl pulsar-tests-common test -Dtest=TestMCPServerForPluginTest
```

### 2. Using TestMCPServer in Your Tests

```kotlin
import ai.platon.pulsar.test.mcp.TestMCPServer
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

class MyMCPTest {
    private lateinit var server: TestMCPServer
    private val objectMapper = jacksonObjectMapper()
    
    @BeforeEach
    fun setUp() {
        server = TestMCPServer(
            serverName = "my-test-server",
            serverVersion = "1.0.0"
        )
    }
    
    @AfterEach
    fun tearDown() {
        server.close()
    }
    
    @Test
    fun testToolExecution() {
        // List available tools
        val tools = server.listTools()
        
        // Execute a tool
        val request = objectMapper.createObjectNode().apply {
            put("name", "echo")
            set<ObjectNode>("arguments", objectMapper.createObjectNode().apply {
                put("message", "Hello MCP!")
            })
        }
        val result = server.callTool(request)
        
        // Validate result
        val content = result["content"] as List<Map<String, Any>>
        assertEquals("Hello MCP!", content[0]["text"])
    }
}
```

## Test Coverage

The `TestMCPServerForPluginTest` suite includes 18 comprehensive tests covering:

### Server Lifecycle
- Server initialization and running state
- Proper closure and cleanup
- Server info endpoint (name, version, capabilities)

### Tool Discovery
- `list_tools` endpoint returns all available tools
- Tool schemas are MCP-compliant (type, properties, required fields)
- Tool descriptions and argument schemas are correctly defined

### Tool Execution
- **echo tool**: Returns input message unchanged
- **add tool**: Correctly adds two numbers (integers and decimals)
- **multiply tool**: Correctly multiplies two numbers

### Error Handling
- Missing tool names throw appropriate exceptions
- Non-existent tools throw `IllegalArgumentException`
- Missing required arguments return error responses with `isError: true`
- Error messages are informative

### Multiple Operations
- Sequential tool calls maintain server state
- Mixed successful and failed calls don't affect server stability
- Server remains operational after errors

### MCP Protocol Compliance
- Successful responses have `content` array with type and text
- Error responses have `isError` flag and descriptive content
- Response formats match MCP specification

## Integration with Pulsar-Agentic (Future)

When the MCP SDK dependency issues are resolved in pulsar-agentic, TestMCPServer can be used for integration testing:

### Option 1: HTTP Transport Adapter

Create an HTTP-based transport adapter for TestMCPServer:

```kotlin
class HttpMCPTransport(val baseUrl: String) : MCPTransport {
    private val httpClient = HttpClient(CIO)
    
    override suspend fun send(request: MCPRequest): MCPResponse {
        // Implement HTTP-based MCP protocol
        // POST to $baseUrl/mcp/list_tools or $baseUrl/mcp/call_tool
    }
}
```

### Option 2: Spring Boot Integration Test

Start TestMCPServer in a Spring Boot test context:

```kotlin
@SpringBootTest(webEnvironment = RANDOM_PORT)
class MCPPluginIntegrationTest {
    @LocalServerPort
    private var port: Int = 0
    
    @Test
    fun `test MCP plugin with TestMCPServer`() = runBlocking {
        val config = MCPConfig(
            serverName = "test-server",
            transportType = MCPTransportType.SSE,
            url = "http://localhost:$port/mcp"
        )
        
        MCPPluginRegistry.instance.registerMCPServer(config)
        val executor = MCPPluginRegistry.instance.getToolExecutor("test-server")
        
        // Test tool execution through the plugin framework
        val result = executor.execute(
            ToolCall("test-server", "echo", mapOf("message" to "test"))
        )
        assertEquals("test", result.value)
    }
}
```

### Option 3: Mock Site Integration

TestMCPServer is designed to work with MockSiteApplication:

```kotlin
@SpringBootApplication(scanBasePackages = [
    "ai.platon.pulsar.test.server",
    "ai.platon.pulsar.test.mcp"  // Add MCP server package
])
class MockSiteApplication
```

This makes it available at `http://localhost:{port}/mcp/*` alongside other mock endpoints.

## MCP Protocol Endpoints

### GET /mcp/info

Returns server metadata:

```json
{
  "name": "test-mcp-server",
  "version": "1.0.0",
  "capabilities": {
    "tools": {}
  }
}
```

### POST /mcp/list_tools

Lists available tools with their schemas:

```json
{
  "tools": [
    {
      "name": "echo",
      "description": "Echoes back the input message",
      "inputSchema": {
        "type": "object",
        "properties": {
          "message": {
            "type": "string",
            "description": "The message to echo back"
          }
        },
        "required": ["message"]
      }
    }
  ]
}
```

### POST /mcp/call_tool

Executes a tool with given arguments:

**Request:**
```json
{
  "name": "add",
  "arguments": {
    "a": 5,
    "b": 3
  }
}
```

**Success Response:**
```json
{
  "content": [
    {
      "type": "text",
      "text": "8.0"
    }
  ]
}
```

**Error Response:**
```json
{
  "isError": true,
  "content": [
    {
      "type": "text",
      "text": "Error: message argument is required"
    }
  ]
}
```

## Built-in Tools

### echo
- **Description**: Echoes back the input message
- **Parameters**: `message` (string, required)
- **Example**: `{"message": "Hello"}` → `"Hello"`

### add
- **Description**: Adds two numbers together
- **Parameters**: 
  - `a` (number, required)
  - `b` (number, required)
- **Example**: `{"a": 5, "b": 3}` → `"8.0"`

### multiply
- **Description**: Multiplies two numbers together
- **Parameters**:
  - `a` (number, required)
  - `b` (number, required)
- **Example**: `{"a": 4, "b": 7}` → `"28.0"`

## Benefits for Testing

1. **No External Dependencies**: Runs entirely in-process, no need for external MCP servers
2. **Fast**: Minimal overhead, tests run quickly
3. **Reliable**: Consistent behavior, no network issues
4. **Controllable**: Full control over server state and responses
5. **MCP-Compliant**: Follows MCP protocol specifications
6. **Well-Tested**: Comprehensive test suite ensures reliability

## See Also

- [TestMCPServer.kt](../pulsar-tests-common/src/main/kotlin/ai/platon/pulsar/test/mcp/TestMCPServer.kt) - Server implementation
- [TestMCPServerTest.kt](../pulsar-tests-common/src/test/kotlin/ai/platon/pulsar/test/mcp/TestMCPServerTest.kt) - Basic functionality tests
- [TestMCPServerForPluginTest.kt](../pulsar-tests-common/src/test/kotlin/ai/platon/pulsar/test/mcp/TestMCPServerForPluginTest.kt) - Comprehensive MCP plugin testing
- [MCP Plugin README](../pulsar-core/pulsar-agentic/src/main/kotlin/ai/platon/pulsar/agentic/mcp/README.md) - MCP plugin feature documentation
