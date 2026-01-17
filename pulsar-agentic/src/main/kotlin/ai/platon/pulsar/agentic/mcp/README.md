# MCP Plugin Support for Pulsar Agentic

This module provides MCP (Model Context Protocol) plugin support for the Pulsar agentic framework, enabling seamless integration with external MCP servers.

## Overview

The MCP plugin support allows Pulsar to:
- Connect to MCP servers using various transport protocols (STDIO, SSE, WebSocket)
- Automatically discover and register tools from MCP servers
- Execute MCP tools through Pulsar's standard tool execution interface
- Manage multiple MCP server connections concurrently

## Features

- **Multiple Transport Types**: Support for STDIO, Server-Sent Events (SSE), and WebSocket transports
- **Automatic Tool Discovery**: Tools are automatically discovered and registered when connecting to an MCP server
- **Unified Interface**: MCP tools integrate seamlessly with Pulsar's existing ToolExecutor framework
- **Concurrent Connections**: Support for connecting to multiple MCP servers simultaneously
- **Error Handling**: Robust error handling with detailed logging
- **Resource Management**: Automatic cleanup of connections and resources

## Quick Start

### 1. Add Dependencies

The dependencies are already configured in the parent POM:

```xml
<dependency>
    <groupId>ai.platon.pulsar</groupId>
    <artifactId>pulsar-agentic</artifactId>
    <version>${pulsar.version}</version>
</dependency>
```

### 2. Configure and Connect

```kotlin
import ai.platon.pulsar.agentic.mcp.*

// Create configuration
val config = MCPConfig(
    serverName = "my-mcp-server",
    transportType = MCPTransportType.STDIO,
    command = "node",
    args = listOf("server.js")
)

// Register the server
MCPPluginRegistry.instance.registerMCPServer(config)
```

### 3. Use MCP Tools

```kotlin
// Get the tool executor
val executor = MCPPluginRegistry.instance.getToolExecutor("my-mcp-server")

// Execute a tool
val toolCall = ToolCall(
    domain = "mcp.my-mcp-server",
    method = "my_tool",
    arguments = mutableMapOf("param" to "value")
)
val result = executor?.execute(toolCall)
```

## Architecture

### Core Components

- **MCPConfig**: Configuration data class for MCP server connections
- **MCPClientManager**: Manages the lifecycle of an MCP client connection
- **MCPToolExecutor**: Implements ToolExecutor to execute MCP tools
- **MCPPluginRegistry**: Central registry for managing multiple MCP servers

### Integration Points

MCP tools integrate with:
- **CustomToolRegistry**: MCP tools are automatically registered for discovery
- **ToolExecutor Interface**: Provides standard execution interface
- **Tool Specifications**: MCP schemas are converted to Pulsar tool specs

## Configuration

### STDIO Transport (Local Process)

```kotlin
MCPConfig(
    serverName = "local-server",
    transportType = MCPTransportType.STDIO,
    command = "python",
    args = listOf("server.py")
)
```

### SSE Transport (HTTP Streaming)

```kotlin
MCPConfig(
    serverName = "remote-server",
    transportType = MCPTransportType.SSE,
    url = "http://localhost:8080/sse"
)
```

### WebSocket Transport

```kotlin
MCPConfig(
    serverName = "ws-server",
    transportType = MCPTransportType.WEBSOCKET,
    url = "ws://localhost:8080/ws"
)
```

## Testing

Run the tests with:

```bash
./mvnw test -pl pulsar-core/pulsar-agentic -Dtest=MCPConfigTest
```

See example usage in:
- `src/test/kotlin/ai/platon/pulsar/agentic/mcp/MCPConfigTest.kt`
- `src/test/kotlin/ai/platon/pulsar/agentic/mcp/examples/MCPPluginExample.kt`

## Documentation

Full documentation is available at: [docs/mcp-plugin-support.md](../../../docs/mcp-plugin-support.md)

## Dependencies

The MCP plugin requires:
- **kotlin-sdk**: `io.modelcontextprotocol:kotlin-sdk` (latest compatible version)
- **ktor-client-cio**: `io.ktor:ktor-client-cio` (Ktor client engine)

Specific versions are managed through the parent POM dependency management.

## License

Apache License 2.0 - See LICENSE file for details.
