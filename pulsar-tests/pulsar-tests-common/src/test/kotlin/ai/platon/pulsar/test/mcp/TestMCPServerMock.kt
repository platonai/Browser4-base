package ai.platon.pulsar.test.mcp

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

/**
 * Tests for the TestMCPServer.
 */
class TestMCPServerMock {

    private lateinit var server: MockMCPServer
    private val objectMapper = jacksonObjectMapper()

    @BeforeEach
    fun setUp() {
        server = MockMCPServer(serverName = "test-server", serverVersion = "1.0.0-test")
    }

    @AfterEach
    fun tearDown() {
        server.close()
    }

    @Test
        @DisplayName("server starts and is running")
    fun serverStartsAndIsRunning() {
        assertTrue(server.isRunning(), "Server should be running after initialization")
    }

    @Test
        @DisplayName("server provides info")
    fun serverProvidesInfo() {
        val info = server.getInfo()

        assertNotNull(info)
        assertEquals("test-server", info["name"])
        assertEquals("1.0.0-test", info["version"])
        assertTrue(info.containsKey("capabilities"))
    }

    @Test
        @DisplayName("server lists available tools")
    fun serverListsAvailableTools() {
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
        @DisplayName("echo tool returns input message")
    fun echoToolReturnsInputMessage() {
        val request = objectMapper.createObjectNode().apply {
            put("name", "echo")
            set<ObjectNode>("arguments", objectMapper.createObjectNode().apply {
                put("message", "Hello, MCP!")
            })
        }

        val result = server.callTool(objectMapper.writeValueAsString(request))

        assertNotNull(result)
        assertTrue(result.containsKey("content"))

        @Suppress("UNCHECKED_CAST")
        val content = result["content"] as List<Map<String, Any>>
        assertEquals(1, content.size)
        assertEquals("text", content[0]["type"])
        assertEquals("Hello, MCP!", content[0]["text"])
    }

    @Test
        @DisplayName("add tool adds two numbers")
    fun addToolAddsTwoNumbers() {
        val request = objectMapper.createObjectNode().apply {
            put("name", "add")
            set<ObjectNode>("arguments", objectMapper.createObjectNode().apply {
                put("a", 5)
                put("b", 3)
            })
        }

        val result = server.callTool(objectMapper.writeValueAsString(request))

        assertNotNull(result)
        assertTrue(result.containsKey("content"))

        @Suppress("UNCHECKED_CAST")
        val content = result["content"] as List<Map<String, Any>>
        assertEquals(1, content.size)
        assertEquals("text", content[0]["type"])
        assertEquals("8.0", content[0]["text"])
    }

    @Test
        @DisplayName("multiply tool multiplies two numbers")
    fun multiplyToolMultipliesTwoNumbers() {
        val request = objectMapper.createObjectNode().apply {
            put("name", "multiply")
            set<ObjectNode>("arguments", objectMapper.createObjectNode().apply {
                put("a", 4)
                put("b", 7)
            })
        }

        val result = server.callTool(objectMapper.writeValueAsString(request))

        assertNotNull(result)
        assertTrue(result.containsKey("content"))

        @Suppress("UNCHECKED_CAST")
        val content = result["content"] as List<Map<String, Any>>
        assertEquals(1, content.size)
        assertEquals("text", content[0]["type"])
        assertEquals("28.0", content[0]["text"])
    }

    @Test
        @DisplayName("calling non-existent tool throws exception")
    fun callingNonExistentToolThrowsException() {
        val request = objectMapper.createObjectNode().apply {
            put("name", "non_existent_tool")
            set<ObjectNode>("arguments", objectMapper.createObjectNode())
        }

        assertThrows(IllegalArgumentException::class.java) {
            server.callTool(objectMapper.writeValueAsString(request))
        }
    }

    @Test
        @DisplayName("calling tool without required argument returns error")
    fun callingToolWithoutRequiredArgumentReturnsError() {
        val request = objectMapper.createObjectNode().apply {
            put("name", "echo")
            set<ObjectNode>("arguments", objectMapper.createObjectNode()) // Missing 'message' argument
        }

        val result = server.callTool(objectMapper.writeValueAsString(request))

        assertNotNull(result)
        assertTrue(result.containsKey("isError"))
        assertTrue(result["isError"] as Boolean)
        assertTrue(result.containsKey("content"))
    }

    @Test
        @DisplayName("server can be closed")
    fun serverCanBeClosed() {
        assertTrue(server.isRunning())
        server.close()
        assertFalse(server.isRunning())
    }
}
