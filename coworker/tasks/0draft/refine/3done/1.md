# Start Browser4 as an MCP Server

To start Browser4 in MCP server mode, run:

```shell
java -jar browser4.jar --mcp-server
```

This command starts Browser4 as an MCP server and allows MCP clients to connect.

## Claude Desktop configuration (`claude_desktop_config.json`)

Add the following entry to your Claude Desktop configuration:

```json
{
  "mcpServers": {
    "browser4": {
      "command": "java",
      "args": ["-jar", "/path/to/browser4.jar", "--mcp-server"]
    }
  }
}
```

## References

- [Browser4AgentsApplication.kt](../../../browser4/browser4-agents/src/main/kotlin/ai/platon/pulsar/app/Browser4AgentsApplication.kt)
- [Browser4MCPServer.kt](../../../pulsar-agentic/src/main/kotlin/ai/platon/pulsar/agentic/mcp/server/Browser4MCPServer.kt)
- [Browser4MCPServerRunner.kt](../../../pulsar-agentic/src/main/kotlin/ai/platon/pulsar/agentic/mcp/server/Browser4MCPServerRunner.kt)
