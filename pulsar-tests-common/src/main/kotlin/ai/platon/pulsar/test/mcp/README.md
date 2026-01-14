# Test MCP Server

A minimal Model Context Protocol (MCP) server implementation for testing purposes.

## Overview

The `TestMCPServer` provides a simplified MCP implementation that can be used for testing MCP clients and integration scenarios. It implements core MCP protocol features using HTTP/JSON endpoints, making it easy to set up and test without complex dependencies.

## Features

- **Tool Listing**: List all available tools via the `/mcp/list_tools` endpoint
- **Tool Execution**: Execute tools via the `/mcp/call_tool` endpoint
- **Server Info**: Get server information via the `/mcp/info` endpoint

## Built-in Tools

The server comes with three pre-configured tools:

### 1. echo
Echoes back the input message.

**Parameters:**
- `message` (string, required): The message to echo back

**Example:**
```json
{
  "name": "echo",
  "arguments": {
    "message": "Hello, MCP!"
  }
}
```

### 2. add
Adds two numbers together.

**Parameters:**
- `a` (number, required): First number
- `b` (number, required): Second number

**Example:**
```json
{
  "name": "add",
  "arguments": {
    "a": 5,
    "b": 3
  }
}
```

### 3. multiply
Multiplies two numbers together.

**Parameters:**
- `a` (number, required): First number
- `b` (number, required): Second number

**Example:**
```json
{
  "name": "multiply",
  "arguments": {
    "a": 4,
    "b": 7
  }
}
```

## Usage

### As a Spring Bean

The TestMCPServer is a Spring `@RestController` and can be automatically registered in a Spring Boot application:

```kotlin
import ai.platon.pulsar.test.mcp.TestMCPServer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TestMCPConfig {
    @Bean
    fun testMCPServer(): TestMCPServer {
        return TestMCPServer(
            serverName = "my-test-server",
            serverVersion = "1.0.0"
        )
    }
}
```

### Standalone Usage

You can also use it directly in your tests:

```kotlin
import ai.platon.pulsar.test.mcp.TestMCPServer
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

class MyTest {
    private val server = TestMCPServer()
    private val objectMapper = jacksonObjectMapper()
    
    @Test
    fun testEchoTool() {
        val request = objectMapper.createObjectNode().apply {
            put("name", "echo")
            set<ObjectNode>("arguments", objectMapper.createObjectNode().apply {
                put("message", "Test message")
            })
        }
        
        val result = server.callTool(request)
        // Assert result...
    }
}
```

## API Endpoints

When used in a Spring Boot application, the following endpoints are available:

### GET /mcp/info
Returns server information.

**Response:**
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
Lists all available tools.

**Response:**
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
    },
    // ... other tools
  ]
}
```

### POST /mcp/call_tool
Executes a tool with the given arguments.

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

## Integration with MockSiteApplication

The TestMCPServer can be automatically included in the `MockSiteApplication` by adding it as a bean. It will then be available at `http://localhost:{port}/mcp/*` when the mock site is running.

## Testing

The server includes comprehensive tests in `TestMCPServerTest` that validate:
- Server initialization and lifecycle
- Tool listing
- Tool execution (echo, add, multiply)
- Error handling for missing tools and invalid arguments

Run tests with:
```bash
./mvnw -pl pulsar-tests-common test -Dtest=TestMCPServerTest
```

## Implementation Notes

- The server uses Spring's `@RestController` for HTTP endpoint handling
- JSON processing is done via Jackson with Kotlin module support
- Tool definitions are stored in a `ConcurrentHashMap` for thread-safe access
- All tools return results as text content in MCP format
- Error handling returns properly formatted error responses with `isError: true`

## Limitations

This is a minimal implementation designed for testing:
- Only implements HTTP/JSON transport (not STDIO, SSE, or WebSocket transports)
- No authentication or authorization
- No resource or prompt capabilities (MCP features not related to tools)
- Simplified protocol - not a full MCP SDK server implementation

For production use or more advanced features, consider using the full MCP Kotlin SDK server implementation.
