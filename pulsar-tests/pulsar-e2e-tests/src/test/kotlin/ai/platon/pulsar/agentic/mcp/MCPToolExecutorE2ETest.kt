package ai.platon.pulsar.agentic.mcp

import ai.platon.pulsar.test.mcp.MockMCPServer
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.client.RestTestClient
import org.springframework.test.web.servlet.client.expectBody
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.DisplayName

/**
 * End-to-end tests for MCP functionality.
 *
 * This test class validates the complete MCP integration by:
 * 1. Automatically starting TestMCPServer in a Spring Boot context
 * 2. Creating MCPClientManager to connect to the test server
 * 3. Testing MCPToolExecutor with real network communication
 * 4. Validating tool discovery, execution, and error handling
 *
 * Test coverage:
 * - Server auto-start and connectivity
 * - Tool discovery and registration via MCPClientManager
 * - Tool execution through MCPToolExecutor
 * - Error scenarios (invalid tools, missing arguments)
 * - Connection lifecycle management
 * - Cross-network communication (HTTP-based)
 */
@SpringBootTest(
    classes = [MCPTestApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Tag("E2ETest")
@Tag("mcp")
class MCPToolExecutorE2ETest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var mockMCPServer: MockMCPServer

    private lateinit var client: RestTestClient

    @BeforeEach
    fun setUp() {
        // Verify server is running
        assertTrue(mockMCPServer.isRunning(), "TestMCPServer should be running")

        client = RestTestClient.bindToServer()
            .baseUrl("http://127.0.0.1:$port")
            .build()
    }

    @AfterEach
    fun tearDown() {
    }

    private fun getInfo(): Map<String, Any> =
        client.get().uri("/mcp/info")
            .exchange()
            .expectStatus().is2xxSuccessful
            .expectBody<Map<String, Any>>()
            .returnResult()
            .responseBody!!

    private fun listTools(): Map<String, Any> =
        client.post().uri("/mcp/list_tools")
            .exchange()
            .expectStatus().is2xxSuccessful
            .expectBody<Map<String, Any>>()
            .returnResult()
            .responseBody!!

    private fun callTool(request: ObjectNode): Map<String, Any> =
        client.post().uri("/mcp/call_tool")
            .contentType(MediaType.APPLICATION_JSON)
            .body(jacksonObjectMapper().convertValue(request, Map::class.java))
            .exchange()
            .expectStatus().is2xxSuccessful
            .expectBody<Map<String, Any>>()
            .returnResult()
            .responseBody!!

    // ========== Server Connectivity Tests ==========

    @Test
        @DisplayName("test server is accessible via HTTP")
    fun testServerIsAccessibleViaHttp() {
        val info = getInfo()
        assertNotNull(info)
        assertEquals("test-mcp-server", info["name"])
        assertEquals("1.0.0", info["version"])
    }

    @Test
        @DisplayName("test server lists available tools")
    fun testServerListsAvailableTools() {
        val result = listTools()
        assertNotNull(result)

        @Suppress("UNCHECKED_CAST")
        val tools = result["tools"] as List<Map<String, Any>>
        assertEquals(3, tools.size, "Should have 3 default tools")

        val toolNames = tools.map { it["name"] as String }.toSet()
        assertTrue(toolNames.containsAll(listOf("echo", "add", "multiply")))
    }

    // ========== Tool Discovery Tests ==========

    @Test
        @DisplayName("test tool discovery returns all available tools")
    fun testToolDiscoveryReturnsAllAvailableTools() {
        val result = listTools()

        @Suppress("UNCHECKED_CAST")
        val tools = result["tools"] as List<Map<String, Any>>

        // Verify each tool has required MCP fields
        tools.forEach { tool ->
            assertTrue(tool.containsKey("name"))
            assertTrue(tool.containsKey("description"))
            assertTrue(tool.containsKey("inputSchema"))

            @Suppress("UNCHECKED_CAST")
            val schema = tool["inputSchema"] as Map<String, Any>
            assertEquals("object", schema["type"])
            assertTrue(schema.containsKey("properties"))
            assertTrue(schema.containsKey("required"))
        }
    }

    @Test
        @DisplayName("test echo tool schema is correct")
    fun testEchoToolSchemaIsCorrect() {
        val result = listTools()

        @Suppress("UNCHECKED_CAST")
        val tools = result["tools"] as List<Map<String, Any>>
        val echoTool = tools.first { it["name"] == "echo" }

        assertEquals("Echoes back the input message", echoTool["description"])

        @Suppress("UNCHECKED_CAST")
        val schema = echoTool["inputSchema"] as Map<String, Any>
        @Suppress("UNCHECKED_CAST")
        val properties = schema["properties"] as Map<String, Any>
        @Suppress("UNCHECKED_CAST")
        val required = schema["required"] as List<String>

        assertTrue(properties.containsKey("message"))
        assertTrue(required.contains("message"))
    }

    // ========== Tool Execution Tests ==========

    @Test
        @DisplayName("test echo tool execution via direct server call")
    fun testEchoToolExecutionViaDirectServerCall() {
        val request = jacksonObjectMapper().createObjectNode().apply {
            put("name", "echo")
            set<ObjectNode>(
                "arguments",
                jacksonObjectMapper().createObjectNode().apply {
                    put("message", "Hello from E2E test")
                }
            )
        }

        val result = callTool(request)
        assertNotNull(result)
        assertFalse(result.containsKey("isError"))

        @Suppress("UNCHECKED_CAST")
        val content = result["content"] as List<Map<String, Any>>
        assertEquals(1, content.size)
        assertEquals("text", content[0]["type"])
        assertEquals("Hello from E2E test", content[0]["text"])
    }

    @Test
        @DisplayName("test add tool execution returns correct sum")
    fun testAddToolExecutionReturnsCorrectSum() {
        val request = jacksonObjectMapper().createObjectNode().apply {
            put("name", "add")
            set<ObjectNode>(
                "arguments",
                jacksonObjectMapper().createObjectNode().apply {
                    put("a", 42)
                    put("b", 58)
                }
            )
        }

        val result = callTool(request)
        assertNotNull(result)

        @Suppress("UNCHECKED_CAST")
        val content = result["content"] as List<Map<String, Any>>
        assertEquals("100.0", content[0]["text"])
    }

    @Test
        @DisplayName("test multiply tool execution returns correct product")
    fun testMultiplyToolExecutionReturnsCorrectProduct() {
        val request = jacksonObjectMapper().createObjectNode().apply {
            put("name", "multiply")
            set<ObjectNode>(
                "arguments",
                jacksonObjectMapper().createObjectNode().apply {
                    put("a", 12)
                    put("b", 8)
                }
            )
        }

        val result = callTool(request)
        assertNotNull(result)

        @Suppress("UNCHECKED_CAST")
        val content = result["content"] as List<Map<String, Any>>
        assertEquals("96.0", content[0]["text"])
    }

    // ========== Error Handling Tests ==========

    @Test
        @DisplayName("test calling non-existent tool throws exception")
    fun testCallingNonExistentToolThrowsException() {
        val request = jacksonObjectMapper().createObjectNode().apply {
            put("name", "non_existent_tool")
            set<ObjectNode>(
                "arguments",
                jacksonObjectMapper().createObjectNode()
            )
        }

        client.post().uri("/mcp/call_tool")
            .contentType(MediaType.APPLICATION_JSON)
            .body(jacksonObjectMapper().convertValue(request, Map::class.java))
            .exchange()
            .expectStatus().is5xxServerError
    }

    @Test
        @DisplayName("test calling tool with missing required argument returns error")
    fun testCallingToolWithMissingRequiredArgumentReturnsError() {
        val request = jacksonObjectMapper().createObjectNode().apply {
            put("name", "echo")
            set<ObjectNode>(
                "arguments",
                jacksonObjectMapper().createObjectNode()
                // Missing 'message' argument
            )
        }

        val result = callTool(request)
        assertNotNull(result)
        assertTrue(result.containsKey("isError"))
        assertEquals(true, result["isError"])
    }

    @Test
        @DisplayName("test add tool with missing argument returns error")
    fun testAddToolWithMissingArgumentReturnsError() {
        val request = jacksonObjectMapper().createObjectNode().apply {
            put("name", "add")
            set<ObjectNode>(
                "arguments",
                jacksonObjectMapper().createObjectNode().apply {
                    put("a", 10)
                    // Missing 'b' argument
                }
            )
        }

        val result = callTool(request)
        assertNotNull(result)
        assertTrue(result.containsKey("isError"))
        assertEquals(true, result["isError"])
    }

    // ========== Sequential Operations Tests ==========

    @Test
        @DisplayName("test multiple sequential tool calls maintain server state")
    fun testMultipleSequentialToolCallsMaintainServerState() {
        // First call: echo
        var request = jacksonObjectMapper().createObjectNode().apply {
            put("name", "echo")
            set<ObjectNode>(
                "arguments",
                jacksonObjectMapper().createObjectNode().apply {
                    put("message", "First")
                }
            )
        }
        var result = callTool(request)
        assertFalse(result.containsKey("isError"))

        // Second call: add
        request = jacksonObjectMapper().createObjectNode().apply {
            put("name", "add")
            set<ObjectNode>(
                "arguments",
                jacksonObjectMapper().createObjectNode().apply {
                    put("a", 5)
                    put("b", 10)
                }
            )
        }
        result = callTool(request)
        assertFalse(result.containsKey("isError"))
        @Suppress("UNCHECKED_CAST")
        var content = result["content"] as List<Map<String, Any>>
        assertEquals("15.0", content[0]["text"])

        // Third call: multiply
        request = jacksonObjectMapper().createObjectNode().apply {
            put("name", "multiply")
            set<ObjectNode>(
                "arguments",
                jacksonObjectMapper().createObjectNode().apply {
                    put("a", 3)
                    put("b", 7)
                }
            )
        }
        result = callTool(request)
        assertFalse(result.containsKey("isError"))
        @Suppress("UNCHECKED_CAST")
        content = result["content"] as List<Map<String, Any>>
        assertEquals("21.0", content[0]["text"])

        // Verify server is still running
        assertTrue(mockMCPServer.isRunning())
    }

    @Test
        @DisplayName("test mixed successful and failed calls maintain server stability")
    fun testMixedSuccessfulAndFailedCallsMaintainServerStability() {
        // Successful call
        var request = jacksonObjectMapper().createObjectNode().apply {
            put("name", "echo")
            set<ObjectNode>(
                "arguments",
                jacksonObjectMapper().createObjectNode().apply {
                    put("message", "Success")
                }
            )
        }
        var result = callTool(request)
        assertFalse(result.containsKey("isError"))

        // Failed call
        request = jacksonObjectMapper().createObjectNode().apply {
            put("name", "add")
            set<ObjectNode>(
                "arguments",
                jacksonObjectMapper().createObjectNode().apply {
                    put("a", 5)
                    // Missing 'b'
                }
            )
        }
        result = callTool(request)
        assertTrue(result.containsKey("isError"))

        // Another successful call
        request = jacksonObjectMapper().createObjectNode().apply {
            put("name", "multiply")
            set<ObjectNode>(
                "arguments",
                jacksonObjectMapper().createObjectNode().apply {
                    put("a", 4)
                    put("b", 5)
                }
            )
        }
        result = callTool(request)
        assertFalse(result.containsKey("isError"))
        @Suppress("UNCHECKED_CAST")
        val content = result["content"] as List<Map<String, Any>>
        assertEquals("20.0", content[0]["text"])

        // Verify server is still running
        assertTrue(mockMCPServer.isRunning())
    }

    // ========== Network Communication Tests ==========

    @Test
        @DisplayName("test cross-network communication via HTTP endpoints")
    fun testCrossNetworkCommunicationViaHttpEndpoints() {
        // This test validates that the TestMCPServer is accessible over HTTP
        // and can handle requests across network boundaries

        // Get server info via HTTP
        val info = getInfo()
        assertEquals("test-mcp-server", info["name"])

        // List tools via HTTP
        val toolsList = listTools()
        @Suppress("UNCHECKED_CAST")
        val tools = toolsList["tools"] as List<Map<String, Any>>
        assertTrue(tools.isNotEmpty())

        // Execute tool via HTTP
        val request = jacksonObjectMapper().createObjectNode().apply {
            put("name", "echo")
            set<ObjectNode>(
                "arguments",
                jacksonObjectMapper().createObjectNode().apply {
                    put("message", "Network test")
                }
            )
        }

        val result = callTool(request)
        assertNotNull(result)
        @Suppress("UNCHECKED_CAST")
        val content = result["content"] as List<Map<String, Any>>
        assertEquals("Network test", content[0]["text"])
    }
}
