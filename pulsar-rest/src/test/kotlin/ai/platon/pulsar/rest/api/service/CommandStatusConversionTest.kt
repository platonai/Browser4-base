package ai.platon.pulsar.rest.api.service

import ai.platon.pulsar.agentic.tools.agent.AgentTaskStatus
import ai.platon.pulsar.agentic.tools.crawl.PageVisitStatus
import ai.platon.pulsar.common.ResourceStatus
import ai.platon.pulsar.persist.metadata.ProtocolStatusCodes
import ai.platon.pulsar.rest.api.entities.CommandStatus
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Test status conversion logic to ensure proper field transfer between different status types.
 * 
 * This test verifies:
 * 1. PageVisitStatus -> CommandStatus conversion
 * 2. AgentTaskStatus -> CommandStatus conversion
 * 3. All status fields are properly transferred
 */
class CommandStatusConversionTest {

    @Test
    fun `test PageVisitStatus to CommandStatus conversion with all fields`() {
        // Create a PageVisitStatus with all fields set
        val now = Instant.now()
        val pageVisitStatus = PageVisitStatus(
            id = "test-id-123",
            statusCode = ResourceStatus.SC_OK,
            event = "test-event",
            processState = "in_progress",
            pageStatusCode = ProtocolStatusCodes.SC_OK,
            pageContentBytes = 1024,
            message = "test message"
        )
        pageVisitStatus.lastModifiedTime = now
        pageVisitStatus.finishTime = now.plusSeconds(10)
        
        // Convert using reflection to call the private method
        val commandService = createDummyCommandService()
        val method = CommandService::class.java.getDeclaredMethod(
            "toCommandStatus",
            PageVisitStatus::class.java
        )
        method.isAccessible = true
        val commandStatus = method.invoke(commandService, pageVisitStatus) as CommandStatus
        
        // Verify all fields are transferred
        assertEquals("test-id-123", commandStatus.id)
        assertEquals(ResourceStatus.SC_OK, commandStatus.statusCode)
        assertEquals("test-event", commandStatus.event)
        assertEquals("in_progress", commandStatus.processState)
        assertEquals(ProtocolStatusCodes.SC_OK, commandStatus.pageStatusCode)
        assertEquals(1024, commandStatus.pageContentBytes)
        assertEquals("test message", commandStatus.message)
        assertEquals(now, commandStatus.lastModifiedTime)
        assertEquals(now.plusSeconds(10), commandStatus.finishTime)
    }

    @Test
    fun `test PageVisitStatus to CommandStatus conversion preserves processState done`() {
        val pageVisitStatus = PageVisitStatus(
            id = "test-done",
            statusCode = ResourceStatus.SC_OK,
            processState = "done"
        )
        
        val commandService = createDummyCommandService()
        val method = CommandService::class.java.getDeclaredMethod(
            "toCommandStatus",
            PageVisitStatus::class.java
        )
        method.isAccessible = true
        val commandStatus = method.invoke(commandService, pageVisitStatus) as CommandStatus
        
        // Verify processState is preserved
        assertEquals("done", commandStatus.processState)
        assertEquals(true, commandStatus.isDone)
    }

    @Test
    fun `test AgentTaskStatus to CommandStatus conversion with all fields`() {
        // Create an AgentTaskStatus with all fields set
        val now = Instant.now()
        val agentTaskStatus = AgentTaskStatus(
            id = "agent-id-456",
            statusCode = ResourceStatus.SC_OK,
            event = "agent-event",
            processState = "in_progress",
            message = "agent message"
        )
        agentTaskStatus.lastModifiedTime = now
        agentTaskStatus.finishTime = now.plusSeconds(5)
        
        // Convert using reflection to call the private method
        val commandService = createDummyCommandService()
        val method = CommandService::class.java.getDeclaredMethod(
            "toCommandStatus",
            AgentTaskStatus::class.java
        )
        method.isAccessible = true
        val commandStatus = method.invoke(commandService, agentTaskStatus) as CommandStatus
        
        // Verify all fields are transferred
        assertEquals("agent-id-456", commandStatus.id)
        assertEquals(ResourceStatus.SC_OK, commandStatus.statusCode)
        assertEquals("agent-event", commandStatus.event)
        assertEquals("in_progress", commandStatus.processState)
        assertEquals("agent message", commandStatus.message)
        assertEquals(now, commandStatus.lastModifiedTime)
        assertEquals(now.plusSeconds(5), commandStatus.finishTime)
        
        // Verify that pageStatusCode and pageContentBytes are at default values
        // since AgentTaskStatus doesn't have these fields
        assertEquals(ProtocolStatusCodes.SC_CREATED, commandStatus.pageStatusCode)
        assertEquals(0, commandStatus.pageContentBytes)
    }

    @Test
    fun `test AgentTaskStatus to CommandStatus conversion does not always mark as done`() {
        val agentTaskStatus = AgentTaskStatus(
            id = "agent-created",
            statusCode = ResourceStatus.SC_CREATED,
            processState = "created"
        )
        
        val commandService = createDummyCommandService()
        val method = CommandService::class.java.getDeclaredMethod(
            "toCommandStatus",
            AgentTaskStatus::class.java
        )
        method.isAccessible = true
        val commandStatus = method.invoke(commandService, agentTaskStatus) as CommandStatus
        
        // Verify processState is preserved and not forced to "done"
        assertEquals("created", commandStatus.processState)
        assertEquals(false, commandStatus.isDone)
    }

    @Test
    fun `test AgentTaskStatus to CommandStatus preserves done state`() {
        val agentTaskStatus = AgentTaskStatus(
            id = "agent-done",
            statusCode = ResourceStatus.SC_OK,
            processState = "done"
        )
        
        val commandService = createDummyCommandService()
        val method = CommandService::class.java.getDeclaredMethod(
            "toCommandStatus",
            AgentTaskStatus::class.java
        )
        method.isAccessible = true
        val commandStatus = method.invoke(commandService, agentTaskStatus) as CommandStatus
        
        // Verify processState is preserved
        assertEquals("done", commandStatus.processState)
        assertEquals(true, commandStatus.isDone)
    }

    // Helper method to create a dummy CommandService instance for testing
    private fun createDummyCommandService(): CommandService {
        // We don't need to actually initialize the service for these tests
        // since we're testing the conversion methods directly via reflection
        return null as CommandService
    }
}
