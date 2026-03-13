package ai.platon.pulsar.rest.mcp.controller

import ai.platon.pulsar.agentic.AgenticSession
import ai.platon.pulsar.agentic.agents.BasicBrowserAgent
import ai.platon.pulsar.agentic.model.ToolCall
import ai.platon.pulsar.agentic.tools.AgentToolExecutor
import ai.platon.pulsar.agentic.model.TcEvaluate
import ai.platon.pulsar.agentic.model.ToolCallResult
import ai.platon.pulsar.agentic.model.ToolSpec
import ai.platon.pulsar.rest.mcp.service.SessionManager
import ai.platon.pulsar.rest.mcp.service.SessionManager.ManagedSession
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.springframework.http.HttpStatus
import jakarta.servlet.http.HttpServletResponse
import java.util.UUID

class MCPToolControllerTest {
    private val objectMapper = jacksonObjectMapper()

    @Mock
    private lateinit var sessionManager: SessionManager

    @Mock
    private lateinit var response: HttpServletResponse

    @Mock
    private lateinit var managedSession: ManagedSession

    @Mock
    private lateinit var agenticSession: AgenticSession

    @Mock
    private lateinit var basicBrowserAgent: BasicBrowserAgent

    @Mock
    private lateinit var agentToolExecutor: AgentToolExecutor

    private lateinit var controller: MCPToolController

    private val sessionId = UUID.randomUUID().toString()

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        controller = MCPToolController(sessionManager)

        // Setup session structure
        `when`(sessionManager.getSession(sessionId)).thenReturn(managedSession)
        `when`(managedSession.agenticSession).thenReturn(agenticSession)
        `when`(agenticSession.companionAgent).thenReturn(basicBrowserAgent)
        `when`(basicBrowserAgent.toolExtractor).thenReturn(agentToolExecutor)
    }

    private fun <T> any(): T {
        Mockito.any<T>()
        return null as T
    }

    private fun anyToolCall(): ToolCall {
        Mockito.any(ToolCall::class.java)
        return ToolCall("dummy", "dummy")
    }

    private fun capture(captor: ArgumentCaptor<ToolCall>): ToolCall {
        captor.capture()
        return ToolCall("dummy", "dummy")
    }

    @Test
    fun `test open session`() = runBlocking {
        val request = MCPToolCallRequest(
            tool = "open_session",
            arguments = mapOf("url" to "https://example.com")
        )

        val newSessionId = "new-session-id"
        val newManagedSession = Mockito.mock(ManagedSession::class.java)
        `when`(newManagedSession.sessionId).thenReturn(newSessionId)
        `when`(sessionManager.createSession(any())).thenReturn(newManagedSession)

        val result = controller.callTool(request, response)

        assertEquals(HttpStatus.OK, result.statusCode)
        assertTrue(result.body!!.content[0].text.contains(newSessionId))
        Mockito.verify(sessionManager).createSession(null)
        Unit
    }

    @Test
    fun `test response deserializes null isError as false`() {
        val json = """{"content":[{"type":"text","text":"ok"}],"isError":null}"""

        val result = objectMapper.readValue(json, MCPToolCallResponse::class.java)

        assertEquals(false, result.isError)
        assertEquals("ok", result.content.single().text)
    }

    @Test
    fun `test close session`() = runBlocking {
        val request = MCPToolCallRequest(
            tool = "close_session",
            arguments = mapOf("sessionId" to sessionId)
        )
        `when`(sessionManager.deleteSession(sessionId)).thenReturn(true)

        val result = controller.callTool(request, response)

        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals("Session closed", result.body!!.content[0].text)
        Mockito.verify(sessionManager).deleteSession(sessionId)
        Unit
    }

    @Test
    fun `test list sessions`() = runBlocking {
        val request = MCPToolCallRequest(tool = "list_sessions")
        `when`(sessionManager.getAllSessions()).thenReturn(listOf(managedSession))
        `when`(managedSession.sessionId).thenReturn(sessionId)
        `when`(managedSession.url).thenReturn("https://example.com")
        `when`(managedSession.status).thenReturn("active")

        val result = controller.callTool(request, response)

        assertEquals(HttpStatus.OK, result.statusCode)
        assertTrue(result.body!!.content[0].text.contains(sessionId))
        assertTrue(result.body!!.content[0].text.contains("https://example.com"))
    }

    @Test
    fun `test close all sessions`() = runBlocking {
        val request = MCPToolCallRequest(tool = "close_all_sessions")
        `when`(sessionManager.deleteAllSessions()).thenReturn(5)

        val result = controller.callTool(request, response)

        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals("Closed 5 session(s)", result.body!!.content[0].text)
    }

    @Test
    fun `test kill all sessions`() = runBlocking {
        val request = MCPToolCallRequest(tool = "kill_all_sessions")
        `when`(sessionManager.deleteAllSessions()).thenReturn(3)

        val result = controller.callTool(request, response)

        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals("Killed 3 session(s)", result.body!!.content[0].text)
    }

    @Test
    fun `test goto command maps to navigate`() = runBlocking {
        mockTool("tab", "navigate")

        val request = MCPToolCallRequest(
            tool = "navigate",
            arguments = mapOf("sessionId" to sessionId, "url" to "https://example.com")
        )

        val result = controller.callTool(request, response)

        assertEquals(HttpStatus.OK, result.statusCode)

        val captor = ArgumentCaptor.forClass(ToolCall::class.java)
        Mockito.verify(agentToolExecutor).execute(capture(captor))
        val toolCall = captor.value

        assertEquals("tab", toolCall.domain)
        assertEquals("navigate", toolCall.method)
        assertEquals("https://example.com", toolCall.arguments["url"])
    }

    @Test
    fun `test click command`() = runBlocking {
        mockTool("tab", "click")

        val request = MCPToolCallRequest(
            tool = "click",
            arguments = mapOf("sessionId" to sessionId, "selector" to "#btn")
        )

        val result = controller.callTool(request, response)

        assertEquals(HttpStatus.OK, result.statusCode)

        val captor = ArgumentCaptor.forClass(ToolCall::class.java)
        Mockito.verify(agentToolExecutor).execute(capture(captor))
        val toolCall = captor.value

        assertEquals("tab", toolCall.domain)
        assertEquals("click", toolCall.method)
        assertEquals("#btn", toolCall.arguments["selector"])
    }

    @Test
    fun `test fill command`() = runBlocking {
        mockTool("tab", "fill")

        val request = MCPToolCallRequest(
            tool = "fill",
            arguments = mapOf("sessionId" to sessionId, "selector" to "#input", "text" to "text")
        )

        val result = controller.callTool(request, response)

        assertEquals(HttpStatus.OK, result.statusCode)

        val captor = ArgumentCaptor.forClass(ToolCall::class.java)
        Mockito.verify(agentToolExecutor).execute(capture(captor))
        val toolCall = captor.value

        assertEquals("tab", toolCall.domain)
        assertEquals("fill", toolCall.method)
        assertEquals("text", toolCall.arguments["text"])
    }

    @Test
    fun `test explicit mapping page_title`() = runBlocking {
        // page_title maps to driver.title explicitly in resolveToolCall
        val request = MCPToolCallRequest(
            tool = "page_title",
            arguments = mapOf("sessionId" to sessionId)
        )

        // Mock execute to return a value
        `when`(agentToolExecutor.execute(anyToolCall())).thenReturn(toolCallResult("My Page Title"))

        val result = controller.callTool(request, response)

        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals("My Page Title", result.body!!.content[0].text)

        val captor = ArgumentCaptor.forClass(ToolCall::class.java)
        Mockito.verify(agentToolExecutor).execute(capture(captor))
        val toolCall = captor.value

        assertEquals("tab", toolCall.domain)
        assertEquals("title", toolCall.method)
    }

    @Test
    fun `test explicit mapping switch_tab`() = runBlocking {
        // switch_tab maps to browser.switchTab explicitly
        val request = MCPToolCallRequest(
            tool = "switch_tab",
            arguments = mapOf("sessionId" to sessionId, "index" to 1)
        )

        `when`(agentToolExecutor.execute(anyToolCall())).thenReturn(toolCallResult("ok"))

        val result = controller.callTool(request, response)
        assertEquals(HttpStatus.OK, result.statusCode)

        val captor = ArgumentCaptor.forClass(ToolCall::class.java)
        Mockito.verify(agentToolExecutor).execute(capture(captor))
        val toolCall = captor.value

        assertEquals("browser", toolCall.domain)
        assertEquals("switchTab", toolCall.method)
        assertEquals("1", toolCall.arguments["tabId"])
    }

    @Test
    fun `test snake case arguments normalize for drag`() = runBlocking {
        mockTool("tab", "drag")

        val request = MCPToolCallRequest(
            tool = "drag",
            arguments = mapOf("sessionId" to sessionId, "source_selector" to "#a", "target_selector" to "#b")
        )

        val result = controller.callTool(request, response)
        assertEquals(HttpStatus.OK, result.statusCode)

        val captor = ArgumentCaptor.forClass(ToolCall::class.java)
        Mockito.verify(agentToolExecutor).execute(capture(captor))
        val toolCall = captor.value

        assertEquals("tab", toolCall.domain)
        assertEquals("drag", toolCall.method)
        assertEquals("#a", toolCall.arguments["sourceSelector"])
        assertEquals("#b", toolCall.arguments["targetSelector"])
    }

    @Test
    fun `test generic commands`() = runBlocking {
        val commands = listOf(
            Triple("go_back", "tab", "go_back"),
            Triple("reload", "tab", "reload"),
            Triple("press", "tab", "press"),
            Triple("hover", "tab", "hover"),
            Triple("screenshot", "tab", "screenshot"),
            Triple("dblclick", "tab", "dblclick"),
            Triple("drag", "tab", "drag"),
            Triple("select_option", "tab", "select_option"),
            Triple("upload", "tab", "upload"),
            Triple("check", "tab", "check"),
            Triple("uncheck", "tab", "uncheck"),
            Triple("type", "tab", "type"),
            Triple("evaluate", "tab", "evaluate"),
            Triple("evaluate_value", "tab", "evaluateValue"),
            Triple("dialog_accept", "tab", "dialog_accept"),
            Triple("dialog_dismiss", "tab", "dialog_dismiss"),
            Triple("resize", "tab", "resize"),
            Triple("keydown", "tab", "keyDown"),
            Triple("keyup", "tab", "keyUp"),
            Triple("mousemove", "tab", "mouseMove"),
            Triple("mousedown", "tab", "mouseDown"),
            Triple("mouseup", "tab", "mouseUp"),
            Triple("mousewheel", "tab", "mouseWheel")
        )

        for ((tool, domain, method) in commands) {
            mockTool(domain, method)

            val request = MCPToolCallRequest(
                tool = tool,
                arguments = mapOf("sessionId" to sessionId, "arg" to "val")
            )

            // Reset mocks for each iteration to avoid interference or strict stubbing issues
            Mockito.reset(agentToolExecutor)
            mockTool(domain, method) // Re-apply stubbing

            val result = controller.callTool(request, response)

            assertEquals(HttpStatus.OK, result.statusCode, "Failed for tool: $tool")

            val captor = ArgumentCaptor.forClass(ToolCall::class.java)
            Mockito.verify(agentToolExecutor).execute(capture(captor))
            val toolCall = captor.value

            assertEquals(domain, toolCall.domain)
            assertEquals(method, toolCall.method)
        }
    }

    @Test
    fun `test browser4 cli input aliases`() = runBlocking {
        val commands = listOf(
            "browser_keydown" to "keyDown",
            "browser_keyup" to "keyUp",
            "browser_mouse_move_xy" to "mouseMove",
            "browser_mouse_down" to "mouseDown",
            "browser_mouse_up" to "mouseUp",
            "browser_mouse_wheel" to "mouseWheel"
        )

        for ((tool, method) in commands) {
            mockTool("tab", method)

            val request = MCPToolCallRequest(
                tool = tool,
                arguments = mapOf("sessionId" to sessionId, "arg" to "val")
            )

            Mockito.reset(agentToolExecutor)
            mockTool("tab", method)

            val result = controller.callTool(request, response)

            assertEquals(HttpStatus.OK, result.statusCode, "Failed for tool: $tool")

            val captor = ArgumentCaptor.forClass(ToolCall::class.java)
            Mockito.verify(agentToolExecutor).execute(capture(captor))
            val toolCall = captor.value

            assertEquals("tab", toolCall.domain)
            assertEquals(method, toolCall.method)
        }
    }

    @Test
    fun `test explicit mapping tab_new`() = runBlocking {
        // tab_new maps to browser.newTab
        val request = MCPToolCallRequest(
            tool = "tab_new",
            arguments = mapOf("sessionId" to sessionId, "url" to "about:blank")
        )

        `when`(agentToolExecutor.execute(anyToolCall())).thenReturn(toolCallResult("ok"))

        val result = controller.callTool(request, response)
        assertEquals(HttpStatus.OK, result.statusCode)

        val captor = ArgumentCaptor.forClass(ToolCall::class.java)
        Mockito.verify(agentToolExecutor).execute(capture(captor))
        val toolCall = captor.value

        assertEquals("browser", toolCall.domain)
        assertEquals("newTab", toolCall.method)
    }

    @Test
    fun `test explicit mapping tab_list`() = runBlocking {
        // tab_list maps to browser.listTabs
        val request = MCPToolCallRequest(
            tool = "tab_list",
            arguments = mapOf("sessionId" to sessionId)
        )

        `when`(agentToolExecutor.execute(anyToolCall())).thenReturn(toolCallResult("[]"))

        val result = controller.callTool(request, response)
        assertEquals(HttpStatus.OK, result.statusCode)

        val captor = ArgumentCaptor.forClass(ToolCall::class.java)
        Mockito.verify(agentToolExecutor).execute(capture(captor))
        val toolCall = captor.value

        assertEquals("browser", toolCall.domain)
        assertEquals("listTabs", toolCall.method)
    }

    @Test
    fun `test explicit mapping tab_close`() = runBlocking {
        // tab_close maps to browser.closeTab
        val request = MCPToolCallRequest(
            tool = "tab_close",
            arguments = mapOf("sessionId" to sessionId)
        )

        `when`(agentToolExecutor.execute(anyToolCall())).thenReturn(toolCallResult("ok"))

        val result = controller.callTool(request, response)
        assertEquals(HttpStatus.OK, result.statusCode)

        val captor = ArgumentCaptor.forClass(ToolCall::class.java)
        Mockito.verify(agentToolExecutor).execute(capture(captor))
        val toolCall = captor.value

        assertEquals("browser", toolCall.domain)
        assertEquals("closeTab", toolCall.method)
    }

    @Test
    fun `test delete session data`() = runBlocking {
        val request = MCPToolCallRequest(
            tool = "delete_session_data",
            arguments = mapOf("sessionId" to sessionId)
        )

        val mockMutex = Mockito.mock(Mutex::class.java)
        `when`(managedSession.mutex).thenReturn(mockMutex)

        // Mock lock/unlock
        `when`(mockMutex.lock(any())).thenReturn(Unit)

        val mockDriver = Mockito.mock(WebDriver::class.java)
        `when`(managedSession.driver).thenReturn(mockDriver)

        val result = controller.callTool(request, response)

        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals("User data deleted for session", result.body!!.content[0].text)

        Mockito.verify(mockDriver).clearBrowserCookies()
        Mockito.verify(mockDriver).evaluate("localStorage.clear(); sessionStorage.clear()")
        Unit
    }

    @Test
    fun `test unknown tool returns error`() = runBlocking {
        val request = MCPToolCallRequest(
            tool = "unknown_tool",
            arguments = mapOf("sessionId" to sessionId)
        )

        // Return empty specs so it's not found
        `when`(agentToolExecutor.getAllToolSpecs()).thenReturn(emptyMap())

        val result = controller.callTool(request, response)

        assertEquals(HttpStatus.OK, result.statusCode)
        assertTrue(result.body!!.isError)
        assertTrue(result.body!!.content[0].text.contains("Unknown tool: unknown_tool"))
    }

    private fun mockTool(domain: String, method: String) {
        val toolSpecs = mapOf(
            domain to mapOf(method to ToolSpec(domain = domain, method = method, description = "desc"))
        )
        `when`(agentToolExecutor.getAllToolSpecs()).thenReturn(toolSpecs)

        // Ensure execute returns success
        runBlocking {
            `when`(agentToolExecutor.execute(anyToolCall())).thenReturn(toolCallResult("ok"))
        }
    }

    private fun toolCallResult(value: Any? = null, evaluate: TcEvaluate? = null): ToolCallResult {
        val resolvedEvaluate = evaluate ?: TcEvaluate(value = value)
        return ToolCallResult(
            evaluate = resolvedEvaluate,
            message = resolvedEvaluate.exception?.message,
        )
    }
}
