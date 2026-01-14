package ai.platon.pulsar.test.mcp

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

/**
 * Comprehensive tests demonstrating how TestMCPServer can be used for MCP plugin testing.
 * 
 * This test class serves as both validation of TestMCPServer functionality and documentation
 * for how the server can be used to test MCP plugin implementations in pulsar-agentic.
 * 
 * ## Purpose
 * 
 * TestMCPServer provides a minimal HTTP-based MCP implementation that can be used to:
 * 1. Test MCP plugin discovery and registration without external dependencies
 * 2. Validate tool schema parsing and validation
 * 3. Test tool execution and result handling
 * 4. Test error scenarios and edge cases
 * 5. Provide a stable test environment for MCP client development
 * 
 * ## MCP Protocol Coverage
 * 
 * The tests validate:
 * - Server initialization and lifecycle management
 * - Tool listing with complete schemas
 * - Tool execution with various argument types
 * - Error handling for invalid requests
 * - Response format compliance with MCP protocol
 * 
 * ## Integration with Pulsar-Agentic
 * 
 * When the MCP plugin feature in pulsar-agentic is ready, TestMCPServer can be:
 * 1. Started in Spring Boot test context (see MockSiteApplication pattern)
 * 2. Used as HTTP backend for MCPClientManager testing
 * 3. Used to validate tool discovery via MCPPluginRegistry
 * 4. Used to test tool execution via MCPToolExecutor
 * 
 * Example integration (when pulsar-agentic MCP SDK dependency is resolved):
 * ```kotlin
 * // Start TestMCPServer in Spring Boot test
 * @SpringBootTest(webEnvironment = RANDOM_PORT)
 * class MCPPluginIntegrationTest {
 *     @LocalServerPort
 *     private var port: Int = 0
 *     
 *     @Test
 *     fun `test MCP plugin with TestMCPServer`() = runBlocking {
 *         // Configure MCP client to use test server
 *         val config = MCPConfig(
 *             serverName = "test-server",
 *             transportType = MCPTransportType.SSE,  // or custom HTTP transport
 *             url = "http://localhost:$port/mcp"
 *         )
 *         
 *         // Register and test
 *         MCPPluginRegistry.instance.registerMCPServer(config)
 *         val executor = MCPPluginRegistry.instance.getToolExecutor("test-server")
 *         
 *         // Execute tools discovered from TestMCPServer
 *         val result = executor.execute(ToolCall("test-server", "echo", mapOf("message" to "test")))
 *         assertEquals("test", result.value)
 *     }
 * }
 * ```
 */
@Tag("mcp")
@Tag("unit")
class TestMCPServerForPluginTest {
    
    private lateinit var server: TestMCPServer
    private val objectMapper = jacksonObjectMapper()
    
    @BeforeEach
    fun setUp() {
        server = TestMCPServer(
            serverName = "plugin-test-server",
            serverVersion = "1.0.0"
        )
    }
    
    @AfterEach
    fun tearDown() {
        server.close()
    }
    
    // ========== Server Lifecycle Tests ==========
    
    @Test
    fun `server initializes and is running`() {
        assertTrue(server.isRunning(), "Server should be running after initialization")
    }
    
    @Test
    fun `server can be closed and stopped`() {
        assertTrue(server.isRunning())
        server.close()
        assertFalse(server.isRunning(), "Server should be stopped after close()")
    }
    
    @Test
    fun `server info provides required MCP metadata`() {
        val info = server.getInfo()
        
        Assertions.assertNotNull(info)
        assertEquals("plugin-test-server", info["name"], "Server name should match")
        assertEquals("1.0.0", info["version"], "Server version should match")
        assertTrue(info.containsKey("capabilities"), "Should include capabilities")
        
        @Suppress("UNCHECKED_CAST")
        val capabilities = info["capabilities"] as Map<String, Any>
        assertTrue(capabilities.containsKey("tools"), "Capabilities should declare tool support")
    }
    
    // ========== Tool Discovery Tests ==========
    
    @Test
    fun `list_tools returns all available tools`() {
        val result = server.listTools()
        
        Assertions.assertNotNull(result)
        assertTrue(result.containsKey("tools"), "Response should contain tools array")
        
        @Suppress("UNCHECKED_CAST")
        val tools = result["tools"] as List<Map<String, Any>>
        assertEquals(3, tools.size, "Should have 3 default tools")
        
        val toolNames = tools.map { it["name"] as String }.toSet()
        assertTrue(toolNames.contains("echo"), "Should have echo tool")
        assertTrue(toolNames.contains("add"), "Should have add tool")
        assertTrue(toolNames.contains("multiply"), "Should have multiply tool")
    }
    
    @Test
    fun `tool schemas are MCP-compliant`() {
        val result = server.listTools()
        
        @Suppress("UNCHECKED_CAST")
        val tools = result["tools"] as List<Map<String, Any>>
        
        // Each tool must have required MCP fields
        tools.forEach { tool ->
            assertTrue(tool.containsKey("name"), "Tool must have name")
            assertTrue(tool.containsKey("description"), "Tool must have description")
            assertTrue(tool.containsKey("inputSchema"), "Tool must have inputSchema")
            
            // Schema must be valid JSON Schema object
            @Suppress("UNCHECKED_CAST")
            val schema = tool["inputSchema"] as Map<String, Any>
            assertEquals("object", schema["type"], "Schema type must be 'object'")
            assertTrue(schema.containsKey("properties"), "Schema must have properties")
            assertTrue(schema.containsKey("required"), "Schema must list required fields")
        }
    }
    
    @Test
    fun `tool schemas describe arguments correctly`() {
        val result = server.listTools()
        
        @Suppress("UNCHECKED_CAST")
        val tools = result["tools"] as List<Map<String, Any>>
        
        // Validate echo tool schema
        @Suppress("UNCHECKED_CAST")
        val echoTool = tools.first { (it["name"] as String) == "echo" }
        @Suppress("UNCHECKED_CAST")
        val echoSchema = echoTool["inputSchema"] as Map<String, Any>
        @Suppress("UNCHECKED_CAST")
        val echoProps = echoSchema["properties"] as Map<String, Any>
        @Suppress("UNCHECKED_CAST")
        val echoRequired = echoSchema["required"] as List<String>
        
        assertTrue(echoProps.containsKey("message"), "Echo should have message property")
        assertEquals("string", (echoProps["message"] as Map<*, *>)["type"], "Message should be string type")
        assertTrue(echoRequired.contains("message"), "Message should be required")
        
        // Validate add tool schema
        @Suppress("UNCHECKED_CAST")
        val addTool = tools.first { (it["name"] as String) == "add" }
        @Suppress("UNCHECKED_CAST")
        val addSchema = addTool["inputSchema"] as Map<String, Any>
        @Suppress("UNCHECKED_CAST")
        val addProps = addSchema["properties"] as Map<String, Any>
        @Suppress("UNCHECKED_CAST")
        val addRequired = addSchema["required"] as List<String>
        
        assertTrue(addProps.containsKey("a"), "Add should have parameter 'a'")
        assertTrue(addProps.containsKey("b"), "Add should have parameter 'b'")
        assertEquals("number", (addProps["a"] as Map<*, *>)["type"], "Parameter 'a' should be number")
        assertEquals("number", (addProps["b"] as Map<*, *>)["type"], "Parameter 'b' should be number")
        assertTrue(addRequired.containsAll(listOf("a", "b")), "Both parameters should be required")
    }
    
    // ========== Tool Execution Tests ==========
    
    @Test
    fun `echo tool executes correctly`() {
        val testMessage = "Hello from MCP plugin test!"
        val request = objectMapper.createObjectNode().apply {
            put("name", "echo")
            set<ObjectNode>("arguments", objectMapper.createObjectNode().apply {
                put("message", testMessage)
            })
        }
        
        val result = server.callTool(request)
        
        Assertions.assertNotNull(result)
        assertTrue(result.containsKey("content"), "Result should contain content")
        assertFalse(result.containsKey("isError"), "Should not have error flag on success")
        
        @Suppress("UNCHECKED_CAST")
        val content = result["content"] as List<Map<String, Any>>
        assertEquals(1, content.size, "Should have one content item")
        assertEquals("text", content[0]["type"], "Content type should be 'text'")
        assertEquals(testMessage, content[0]["text"], "Echo should return exact input")
    }
    
    @Test
    fun `add tool calculates sum correctly`() {
        val request = objectMapper.createObjectNode().apply {
            put("name", "add")
            set<ObjectNode>("arguments", objectMapper.createObjectNode().apply {
                put("a", 15)
                put("b", 27)
            })
        }
        
        val result = server.callTool(request)
        
        @Suppress("UNCHECKED_CAST")
        val content = result["content"] as List<Map<String, Any>>
        assertEquals("42.0", content[0]["text"], "Add should calculate correct sum")
    }
    
    @Test
    fun `add tool handles decimal numbers`() {
        val request = objectMapper.createObjectNode().apply {
            put("name", "add")
            set<ObjectNode>("arguments", objectMapper.createObjectNode().apply {
                put("a", 3.14)
                put("b", 2.86)
            })
        }
        
        val result = server.callTool(request)
        
        @Suppress("UNCHECKED_CAST")
        val content = result["content"] as List<Map<String, Any>>
        val resultValue = (content[0]["text"] as String).toDouble()
        assertEquals(6.0, resultValue, 0.01, "Add should handle decimal numbers")
    }
    
    @Test
    fun `multiply tool calculates product correctly`() {
        val request = objectMapper.createObjectNode().apply {
            put("name", "multiply")
            set<ObjectNode>("arguments", objectMapper.createObjectNode().apply {
                put("a", 6)
                put("b", 7)
            })
        }
        
        val result = server.callTool(request)
        
        @Suppress("UNCHECKED_CAST")
        val content = result["content"] as List<Map<String, Any>>
        assertEquals("42.0", content[0]["text"], "Multiply should calculate correct product")
    }
    
    // ========== Error Handling Tests ==========
    
    @Test
    fun `calling non-existent tool throws exception`() {
        val request = objectMapper.createObjectNode().apply {
            put("name", "non_existent_tool")
            set<ObjectNode>("arguments", objectMapper.createObjectNode())
        }
        
        assertThrows(IllegalArgumentException::class.java) {
            server.callTool(request)
        }
    }
    
    @Test
    fun `calling tool without name throws exception`() {
        val request = objectMapper.createObjectNode().apply {
            set<ObjectNode>("arguments", objectMapper.createObjectNode())
            // Missing "name" field
        }
        
        assertThrows(IllegalArgumentException::class.java) {
            server.callTool(request)
        }
    }
    
    @Test
    fun `calling tool without required argument returns error response`() {
        val request = objectMapper.createObjectNode().apply {
            put("name", "echo")
            set<ObjectNode>("arguments", objectMapper.createObjectNode()) // Missing 'message'
        }
        
        val result = server.callTool(request)
        
        Assertions.assertNotNull(result)
        assertTrue(result.containsKey("isError"), "Response should indicate error")
        assertEquals(true, result["isError"], "isError flag should be true")
        assertTrue(result.containsKey("content"), "Error response should have content")
        
        @Suppress("UNCHECKED_CAST")
        val content = result["content"] as List<Map<String, Any>>
        val errorText = content[0]["text"] as String
        assertTrue(
            errorText.contains("required", ignoreCase = true) ||
            errorText.contains("argument", ignoreCase = true),
            "Error message should mention required argument"
        )
    }
    
    @Test
    fun `calling add without required argument returns error`() {
        val request = objectMapper.createObjectNode().apply {
            put("name", "add")
            set<ObjectNode>("arguments", objectMapper.createObjectNode().apply {
                put("a", 5)
                // Missing 'b' argument
            })
        }
        
        val result = server.callTool(request)
        
        assertTrue(result.containsKey("isError"))
        assertEquals(true, result["isError"])
    }
    
    // ========== Multiple Operations Tests ==========
    
    @Test
    fun `server handles multiple sequential tool calls`() {
        // Call 1: Echo
        var request = objectMapper.createObjectNode().apply {
            put("name", "echo")
            set<ObjectNode>("arguments", objectMapper.createObjectNode().apply {
                put("message", "First call")
            })
        }
        var result = server.callTool(request)
        assertFalse(result.containsKey("isError"), "First call should succeed")
        
        // Call 2: Add
        request = objectMapper.createObjectNode().apply {
            put("name", "add")
            set<ObjectNode>("arguments", objectMapper.createObjectNode().apply {
                put("a", 10)
                put("b", 20)
            })
        }
        result = server.callTool(request)
        assertFalse(result.containsKey("isError"), "Second call should succeed")
        
        @Suppress("UNCHECKED_CAST")
        var content = result["content"] as List<Map<String, Any>>
        assertEquals("30.0", content[0]["text"])
        
        // Call 3: Multiply
        request = objectMapper.createObjectNode().apply {
            put("name", "multiply")
            set<ObjectNode>("arguments", objectMapper.createObjectNode().apply {
                put("a", 5)
                put("b", 8)
            })
        }
        result = server.callTool(request)
        assertFalse(result.containsKey("isError"), "Third call should succeed")
        
        @Suppress("UNCHECKED_CAST")
        content = result["content"] as List<Map<String, Any>>
        assertEquals("40.0", content[0]["text"])
        
        // Verify server is still running
        assertTrue(server.isRunning(), "Server should still be running after multiple calls")
    }
    
    @Test
    fun `server maintains state across mixed successful and failed calls`() {
        // Successful call
        var request = objectMapper.createObjectNode().apply {
            put("name", "echo")
            set<ObjectNode>("arguments", objectMapper.createObjectNode().apply {
                put("message", "Success")
            })
        }
        var result = server.callTool(request)
        assertFalse(result.containsKey("isError"))
        
        // Failed call (missing argument)
        request = objectMapper.createObjectNode().apply {
            put("name", "add")
            set<ObjectNode>("arguments", objectMapper.createObjectNode().apply {
                put("a", 5)
                // Missing 'b'
            })
        }
        result = server.callTool(request)
        assertTrue(result.containsKey("isError"))
        
        // Another successful call
        request = objectMapper.createObjectNode().apply {
            put("name", "multiply")
            set<ObjectNode>("arguments", objectMapper.createObjectNode().apply {
                put("a", 3)
                put("b", 4)
            })
        }
        result = server.callTool(request)
        assertFalse(result.containsKey("isError"))
        
        @Suppress("UNCHECKED_CAST")
        val content = result["content"] as List<Map<String, Any>>
        assertEquals("12.0", content[0]["text"])
        
        // Verify server is still functioning
        assertTrue(server.isRunning())
    }
    
    // ========== MCP Protocol Compliance Tests ==========
    
    @Test
    fun `response format is MCP-compliant for successful tool execution`() {
        val request = objectMapper.createObjectNode().apply {
            put("name", "echo")
            set<ObjectNode>("arguments", objectMapper.createObjectNode().apply {
                put("message", "test")
            })
        }
        
        val result = server.callTool(request)
        
        // MCP spec: successful responses have 'content' array
        assertTrue(result.containsKey("content"))
        @Suppress("UNCHECKED_CAST")
        val content = result["content"] as List<Map<String, Any>>
        
        // Each content item must have 'type' and content
        content.forEach { item ->
            assertTrue(item.containsKey("type"))
            assertTrue(item.containsKey("text") || item.containsKey("data"))
        }
    }
    
    @Test
    fun `response format is MCP-compliant for error cases`() {
        val request = objectMapper.createObjectNode().apply {
            put("name", "echo")
            set<ObjectNode>("arguments", objectMapper.createObjectNode())
        }
        
        val result = server.callTool(request)
        
        // MCP spec: error responses have 'isError' flag and 'content' with error details
        assertTrue(result.containsKey("isError"))
        assertEquals(true, result["isError"])
        assertTrue(result.containsKey("content"))
        
        @Suppress("UNCHECKED_CAST")
        val content = result["content"] as List<Map<String, Any>>
        assertTrue(content.isNotEmpty())
        assertEquals("text", content[0]["type"])
        assertTrue((content[0]["text"] as String).isNotEmpty())
    }
}
