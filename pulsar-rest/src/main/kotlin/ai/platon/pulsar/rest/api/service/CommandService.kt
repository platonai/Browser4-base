package ai.platon.pulsar.rest.api.service

import ai.platon.pulsar.agentic.AgenticSession
import ai.platon.pulsar.common.ResourceStatus
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.rest.api.entities.*
import ai.platon.pulsar.skeleton.crawl.PageEventHandlers
import ai.platon.pulsar.skeleton.crawl.event.impl.PageEventHandlersFactory
import ai.platon.pulsar.tools.crawl.AgenticPageVisitor
import ai.platon.pulsar.tools.crawl.PGInstructResult
import ai.platon.pulsar.tools.crawl.PageVisitRequest
import ai.platon.pulsar.tools.crawl.PageVisitStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.springframework.http.codec.ServerSentEvent
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import java.time.Instant
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.Executors

@Service
class CommandService(
    val session: AgenticSession,
    val conversationService: ConversationService,
) {
    companion object {
        const val FLOW_POLLING_INTERVAL = 1000L
    }

    // TODO: use ehcache
    private val commandStatusCache = ConcurrentSkipListMap<String, CommandStatus>()

    // Create a dedicated dispatcher for long-running command operations
    private val scrapingExecutor = Executors.newFixedThreadPool(10)
    private val commandDispatcher = scrapingExecutor.asCoroutineDispatcher()

    private val commanderScope: CoroutineScope = CoroutineScope(
        commandDispatcher + SupervisorJob() + CoroutineName("commander")
    )

    private val logger = getLogger(CommandService::class)

    private val pageVisitor = AgenticPageVisitor(session)

    suspend fun executePageVisitCommandSync(request: CommandRequest, eventHandlers: PageEventHandlers): CommandStatus {
        val status = createCachedCommandStatus(request)
        executePageVisitCommand(request, status, eventHandlers)
        return status
    }

    fun submitPageVisitCommandAsync(request: CommandRequest, eventHandlers: PageEventHandlers): String {
        val status = createCachedCommandStatus(request)
        commanderScope.launch { executePageVisitCommand(request, status, eventHandlers) }
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
            val status = createCachedCommandStatus()
            status.failed(ResourceStatus.SC_BAD_REQUEST)
            return status
        }

        val request = conversationService.normalizePlainCommand(plainCommand)
        return if (request != null) {
            // Page visit execution
            val status = createCachedCommandStatus(request)
            val eventHandlers = PageEventHandlersFactory.create()
            executePageVisitCommand(request, status, eventHandlers)
        } else {
            // Open task execution
            executeAgentCommand(plainCommand)
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
        if (plainCommand.isBlank()) {
            val status = createCachedCommandStatus()
            status.failed(ResourceStatus.SC_BAD_REQUEST)
            return status.id
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
     * This method creates a cached status, executes the agent's run method, and updates
     * the status with the result.
     *
     * @param plainCommand The plain text command for the agent to execute.
     * @return CommandStatus containing the execution result.
     */
    suspend fun executeAgentCommand(plainCommand: String): CommandStatus {
        val status = createCachedCommandStatus()
        executeAgentTaskInternal(plainCommand, status)
        return status
    }

    /**
     * Submit a plain command for asynchronous agent execution.
     *
     * @param plainCommand The plain text command for the agent to execute.
     * @return The command status ID for tracking execution progress.
     */
    fun submitAgentTaskAsync(plainCommand: String): String {
        val status = createCachedCommandStatus()
        commanderScope.launch { executeAgentTaskInternal(plainCommand, status) }
        return status.id
    }

    /**
     * Internal method to execute agent command with a pre-created status.
     *
     * The status is updated with the agent's state history reference, allowing callers
     * to access the latest agent state via [CommandStatus.currentAgentState] during execution.
     */
    private suspend fun executeAgentTaskInternal(plainCommand: String, status: CommandStatus) {
        try {
            status.refresh(ResourceStatus.SC_PROCESSING)
            val agent = session.companionAgent

            // Set agent history reference to allow real-time state tracking
            status.agentHistory = agent.stateHistory

            val history = agent.run(plainCommand)
            val finalState = history.finalResult

            // AgentState has 'summary' for the final result message
            val resultSummary = finalState?.summary ?: finalState?.description ?: ""
            status.message = resultSummary
            status.ensureCommandResult().summary = resultSummary
            status.refresh(ResourceStatus.SC_OK)
        } catch (e: Exception) {
            logger.error("Failed to execute agent command: {}", plainCommand, e)
            status.failed(ResourceStatus.SC_EXPECTATION_FAILED)
            status.message = e.message
        } finally {
            status.done()
        }
    }

    fun getStatus(id: String) = commandStatusCache[id]

    fun getResult(id: String) = commandStatusCache[id]?.commandResult

    fun streamEvents(id: String): Flux<ServerSentEvent<CommandStatus>> {
        val handleFluxSink = { sink: FluxSink<CommandStatus> ->
            val job = commandStatusFlow(id)
                .onEach { sink.next(it) }
                .onCompletion { sink.complete() }
                .catch {
                    logger.error("Error in command status flow", it)
                    sink.error(it)
                }
                .launchIn(commanderScope)

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

            val status = commandStatusCache[id] ?: CommandStatus.notFound(id)
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
    suspend fun executePageVisitCommand(request: String): CommandStatus {
        if (request.isBlank()) {
            return CommandStatus.failed(ResourceStatus.SC_BAD_REQUEST)
        }

        val request2 = conversationService.normalizePlainCommand(request)
        val status = createCachedCommandStatus(request2)
        if (request2 == null) {
            status.failed(ResourceStatus.SC_EXPECTATION_FAILED)
            return status
        }

        val eventHandlers = PageEventHandlersFactory.create()
        return executePageVisitCommand(request2, status, eventHandlers)
    }

    suspend fun executePageVisitCommand(request: CommandRequest): CommandStatus {
        val status = createCachedCommandStatus(request)

        val eventHandlers = PageEventHandlersFactory.create()
        executePageVisitCommand(request, status, eventHandlers)
        return status
    }

    suspend fun executePageVisitCommand(
        request: CommandRequest,
        status: CommandStatus,
        eventHandlers: PageEventHandlers
    ): CommandStatus {
        try {
            status.refresh(ResourceStatus.SC_PROCESSING)

            // Delegate the heavy lifting to AgenticPageVisitor so CommandService stays thin.
            val visitStatus = pageVisitor.visit(request.toPageVisitRequest(), eventHandlers)
            status.applyVisitStatus(visitStatus)
        } catch (e: Exception) {
            status.message = e.message
            status.failed(ResourceStatus.SC_EXPECTATION_FAILED)
        } finally {
            status.done()
        }

        return status
    }

    // =====================
    // Internals
    // =====================

    private fun createCachedCommandStatus(request: CommandRequest? = null): CommandStatus {
        val status = CommandStatus()
        // status.request = request
        commandStatusCache[status.id] = status
        status.refresh("created")
        return status
    }

    private fun CommandRequest.toPageVisitRequest(): PageVisitRequest {
        return PageVisitRequest(
            url = url,
            args = args,
            onBrowserLaunchedActions = onBrowserLaunchedActions,
            onPageReadyActions = onPageReadyActions,
            actions = actions,
            pageSummaryPrompt = pageSummaryPrompt,
            dataExtractionRules = dataExtractionRules,
            uriExtractionRules = uriExtractionRules,
            xsql = xsql,
            richText = richText,
            async = async,
            id = null,
        )
    }

    /**
     * Map a visitor execution result back onto REST [CommandStatus]/[CommandResult] types.
     */
    private fun CommandStatus.applyVisitStatus(visitStatus: PageVisitStatus) {
        // high-level status
        statusCode = visitStatus.statusCode
        pageStatusCode = visitStatus.pageStatusCode
        pageContentBytes = visitStatus.pageContentBytes
        message = visitStatus.message

        // instruct results -> REST instruct results
        @Suppress("UNCHECKED_CAST")
        val restResults = visitStatus.instructResults.map { it.toRestInstructResult() }
        restResults.forEach { addInstructResult(it) }

        // best-effort summary mapping
        val visitResult = visitStatus.pageVisitResult
        if (visitResult != null) {
            val commandResult = ensureCommandResult()
            commandResult.pageSummary = visitResult.pageSummary
            commandResult.fields = visitResult.fields
            commandResult.xsqlResultSet = visitResult.xsqlResultSet
        }

        // Align internal state string with HTTP status.
        if (visitStatus.statusCode == ResourceStatus.SC_OK) {
            refresh(ResourceStatus.SC_OK)
        } else if (visitStatus.statusCode >= 400) {
            failed(visitStatus.statusCode)
        }
    }

    private fun PGInstructResult.toRestInstructResult(): InstructResult {
        // Keep naming aligned to REST API conventions.
        val t = resultType ?: "string"
        return InstructResult.ok(name, result ?: "", t)
    }
}
