package ai.platon.pulsar.test.mcp

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests for the TestMCPServer.
 */
class TestMCPServerTest {
    
    private lateinit var server: TestMCPServer
    private val objectMapper = jacksonObjectMapper()
    
    @BeforeEach
    fun setUp() {
        server = TestMCPServer(serverName = "test-server", serverVersion = "1.0.0-test")
    }
    
    @AfterEach
    fun tearDown() {
        server.close()
    }
    
    @Test
    fun `server starts and is running`() {
        assertTrue(server.isRunning(), "Server should be running after initialization")
    }
    
    @Test
    fun `server provides info`() {
        val info = server.getInfo()
        
        assertNotNull(info)
        assertEquals("test-server", info["name"])
        assertEquals("1.0.0-test", info["version"])
        assertTrue(info.containsKey("capabilities"))
    }
    
    @Test
    fun `server lists available tools`() {
        val result = server.listTools()
        
        assertNotNull(result)
        assertTrue(result.containsKey("tools"))
        
        @Suppress("UNCHECKED_CAST")
        val tools = result["tools"] as List<Map<String, Any>>
        assertEquals(3, tools.size, "Should have 3 default tools")
        
        val toolNames = tools.map { it["name"] as String }.toSet()
        assertTrue(toolNames.contains("echo"), "Should have echo tool")
        assertTrue(toolNames.contains("add"), "Should have add tool")
        assertTrue(toolNames.contains("multiply"), "Should have multiply tool")
    }
    
    @Test
    fun `echo tool returns input message`() {
        val request = objectMapper.createObjectNode().apply {
            put("name", "echo")
            set<ObjectNode>("arguments", objectMapper.createObjectNode().apply {
                put("message", "Hello, MCP!")
            })
        }
        
        val result = server.callTool(request)
        
        assertNotNull(result)
        assertTrue(result.containsKey("content"))
        
        @Suppress("UNCHECKED_CAST")
        val content = result["content"] as List<Map<String, Any>>
        assertEquals(1, content.size)
        assertEquals("text", content[0]["type"])
        assertEquals("Hello, MCP!", content[0]["text"])
    }
    
    @Test
    fun `add tool adds two numbers`() {
        val request = objectMapper.createObjectNode().apply {
            put("name", "add")
            set<ObjectNode>("arguments", objectMapper.createObjectNode().apply {
                put("a", 5)
                put("b", 3)
            })
        }
        
        val result = server.callTool(request)
        
        assertNotNull(result)
        assertTrue(result.containsKey("content"))
        
        @Suppress("UNCHECKED_CAST")
        val content = result["content"] as List<Map<String, Any>>
        assertEquals(1, content.size)
        assertEquals("text", content[0]["type"])
        assertEquals("8.0", content[0]["text"])
    }
    
    @Test
    fun `multiply tool multiplies two numbers`() {
        val request = objectMapper.createObjectNode().apply {
            put("name", "multiply")
            set<ObjectNode>("arguments", objectMapper.createObjectNode().apply {
                put("a", 4)
                put("b", 7)
            })
        }
        
        val result = server.callTool(request)
        
        assertNotNull(result)
        assertTrue(result.containsKey("content"))
        
        @Suppress("UNCHECKED_CAST")
        val content = result["content"] as List<Map<String, Any>>
        assertEquals(1, content.size)
        assertEquals("text", content[0]["type"])
        assertEquals("28.0", content[0]["text"])
    }
    
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
    fun `calling tool without required argument returns error`() {
        val request = objectMapper.createObjectNode().apply {
            put("name", "echo")
            set<ObjectNode>("arguments", objectMapper.createObjectNode()) // Missing 'message' argument
        }
        
        val result = server.callTool(request)
        
        assertNotNull(result)
        assertTrue(result.containsKey("isError"))
        assertTrue(result["isError"] as Boolean)
        assertTrue(result.containsKey("content"))
    }
    
    @Test
    fun `server can be closed`() {
        assertTrue(server.isRunning())
        server.close()
        assertFalse(server.isRunning())
    }
}
