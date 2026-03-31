package ai.platon.pulsar.rest.api.service

import ai.platon.pulsar.agentic.AgenticSession
import ai.platon.pulsar.agentic.tools.agent.StatefulAgentRunner
import ai.platon.pulsar.agentic.tools.crawl.PageVisitRequest
import ai.platon.pulsar.agentic.tools.crawl.PageVisitStatus
import ai.platon.pulsar.agentic.tools.crawl.StatefulPageVisitor
import ai.platon.pulsar.agentic.tools.crawl.failed
import ai.platon.pulsar.common.ResourceStatus
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.rest.api.entities.*
import ai.platon.pulsar.skeleton.crawl.PageEventHandlers
import ai.platon.pulsar.skeleton.crawl.event.impl.PageEventHandlersFactory
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.springframework.http.codec.ServerSentEvent
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import java.time.Instant

@Service
class CommandService(
    val session: AgenticSession,
    val conversationService: ConversationService,
    val loadService: LoadService,
) {
    companion object {
        const val FLOW_POLLING_INTERVAL = 1000L
    }

    private val logger = getLogger(CommandService::class)

    // Create a dedicated dispatcher for long-running command operations
    private val commandDispatcher = Dispatchers.IO.limitedParallelism(10)

    private val commanderScope: CoroutineScope = CoroutineScope(
        commandDispatcher + SupervisorJob() + CoroutineName("commander")
    )

    private val statefulPageVisitor = StatefulPageVisitor(session)
    private val statefulAgentRunner = StatefulAgentRunner(session)

    suspend fun executePageVisitCommandSync(
        request: CommandRequest, eventHandlers: PageEventHandlers
    ): CommandStatus {
        return statefulPageVisitor.visit(request, eventHandlers).toCommandStatus()
    }

    fun submitPageVisitCommandAsync(request: CommandRequest, eventHandlers: PageEventHandlers): String {
        val status = statefulPageVisitor.create()
        commanderScope.launch { statefulPageVisitor.visit(request, status, eventHandlers) }
        return status.id
    }

    /**
     * Execute a plain command synchronously.
     *
     * If `conversationService.normalizePlainCommand(plainCommand)` returns a valid CommandRequest,
     * it executes the command using the standard command execution flow.
     * If it returns null (meaning the command cannot be normalized to a URL-based command),
     * it executes the command using the agent's run method.
     *
     * @param plainCommand The plain text command to execute.
     * @return CommandStatus containing the execution result.
     */
    suspend fun executePlainCommandSync(plainCommand: String): CommandStatus {
        if (plainCommand.isBlank()) {
            return CommandStatus.failed(ResourceStatus.SC_BAD_REQUEST)
        }

        val request = conversationService.normalizePlainCommand(plainCommand)
        return if (request != null) {
            // Page visit execution
            val status = statefulPageVisitor.create()
            val eventHandlers = PageEventHandlersFactory.create()
            statefulPageVisitor.visit(request, status, eventHandlers)
            status.toCommandStatus()
        } else {
            // Open task execution
            val agentStatus = statefulAgentRunner.execute(plainCommand)
            agentStatus.toCommandStatus()
        }
    }

    /**
     * Submit a plain command for asynchronous execution.
     *
     * If `conversationService.normalizePlainCommand(plainCommand)` returns a valid CommandRequest,
     * it submits the command using the standard async command execution flow.
     * If it returns null (meaning the command cannot be normalized to a URL-based command),
     * it submits the command for agent-based execution.
     *
     * @param plainCommand The plain text command to execute.
     * @return The command status ID for tracking execution progress.
     */
    suspend fun submitPlainCommandAsync(plainCommand: String): String {
        val plainCommand = plainCommand.trim()

        if (plainCommand.isBlank()) {
            val status = statefulPageVisitor.create()
            status.failed(ResourceStatus.SC_BAD_REQUEST)
            return status.id
        }

        if (plainCommand.startsWith("http") && Strings.isSingleLine(plainCommand)) {
            return loadService.load(plainCommand).contentAsString
        }

        val request = conversationService.normalizePlainCommand(plainCommand)
        return if (request != null) {
            // Standard URL-based async command execution
            val eventHandlers = PageEventHandlersFactory.create()
            submitPageVisitCommandAsync(request, eventHandlers)
        } else {
            // Agent-based async command execution
            submitAgentTaskAsync(plainCommand)
        }
    }

    /**
     * Execute a plain command using the agent's run method.
     *
     * This delegates to [StatefulAgentRunner] so the execution logic (history tracking,
     * error handling, status transitions) is shared across modules.
     *
     * @param plainCommand The plain text command for the agent to execute.
     * @return CommandStatus containing the execution result.
     */
    suspend fun executeAgentCommand(plainCommand: String): CommandStatus {
        val status = statefulAgentRunner.execute(plainCommand)
        return status.toCommandStatus()
    }

    fun submitAgentTaskAsync(plainCommand: String): String {
        val status = statefulAgentRunner.create()
        commanderScope.launch { statefulAgentRunner.execute(plainCommand, status) }
        return status.id
    }

    fun getStatus(id: String): CommandStatus? {
        return statefulPageVisitor.getStatus(id)?.toCommandStatus() ?: statefulAgentRunner.getStatus(id)
            ?.toCommandStatus()
    }

    fun getResult(id: String): CommandResult? = getStatus(id)?.commandResult

    fun streamEvents(id: String): Flux<ServerSentEvent<CommandStatus>> {
        val handleFluxSink = { sink: FluxSink<CommandStatus> ->
            val job = commandStatusFlow(id).onEach { sink.next(it) }.onCompletion { sink.complete() }.catch {
                logger.error("Error in command status flow", it)
                sink.error(it)
            }.launchIn(commanderScope)

            sink.onDispose { job.cancel() }
        }

        return Flux.create { sink -> handleFluxSink(sink) }.map {
            // ServerSentEvent.builder(it).id(it.id).event(it.event).build()
            // NOTE: [2025/5/20] JavaScript client-side code expects only JSON data, not the event ID nor event name.
            ServerSentEvent.builder(it).build()
        }
    }

    fun commandStatusFlow(id: String): Flow<CommandStatus> = flow {
        var lastModifiedTime = Instant.EPOCH
        do {
            delay(FLOW_POLLING_INTERVAL)

            val status = getStatus(id) ?: CommandStatus.notFound(id)
            if (status.refreshed(lastModifiedTime)) {
                emit(status)
                lastModifiedTime = status.lastModifiedTime
            }

            if (status.isDone) {
                // emit a final event, it's OK to emit a duplicate event
                emit(status)
            }
        } while (!status.isDone)
    }

    /**
     * Executes a command based on the provided request string.
     *
     * This method first attempts to convert the request string into a PromptRequestL2 object.
     * If successful, it calls the command method with the PromptRequestL2 object.
     * If not, it returns a failed status with a status code indicating a bad request.
     *
     * @param request The request string containing a URL and other parameters.
     * @return A PromptResponseL2 object containing the result of the command execution.
     * */
    suspend fun executePageVisitCommand(request: String): PageVisitStatus {
        if (request.isBlank()) {
            return PageVisitStatus.failed(ResourceStatus.SC_BAD_REQUEST)
        }

        val request2 = conversationService.normalizePlainCommand(request) ?: return PageVisitStatus.failed(
            ResourceStatus.SC_EXPECTATION_FAILED
        )

        val eventHandlers = PageEventHandlersFactory.create()
        return statefulPageVisitor.visit(request2, eventHandlers)
    }

    suspend fun executePageVisitCommand(request: PageVisitRequest): PageVisitStatus {
        return statefulPageVisitor.visit(request)
    }

    @PreDestroy
    fun destroy() {
        commanderScope.cancel()
    }
}
