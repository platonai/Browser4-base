@file:Suppress("UNUSED")
package ai.platon.pulsar.sdk.v0

import ai.platon.pulsar.sdk.v0.detail.PulsarClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor
 * license agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership. The ASF licenses this file to
 * you under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required
 * by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

/**
 * AgenticSession extends PulsarSession with AI-powered browser automation.
 *
 * This class provides methods for intelligent browser interaction using
 * natural language instructions. It combines the data extraction capabilities
 * of [PulsarSession] with AI-powered agent functionality.
 *
 * Key capabilities:
 * - All [PulsarSession] methods (open, load, submit, extract, etc.)
 * - Agent act: Execute single actions described in natural language
 * - Agent run: Execute multi-step tasks autonomously
 * - Agent observe: Analyze page and suggest actions
 * - Agent extract: AI-powered data extraction
 * - Agent summarize: Generate page summaries
 *
 * Example usage (FusedActs-style):
 * ```kotlin
 * val session = AgenticSession.getOrCreate()
 * val driver = session.getOrCreateBoundDriver()
 * val agent = session.companionAgent
 *
 * val page = session.open("https://example.com")
 * val document = session.parse(page)
 * val fields = session.extract(document, mapOf("title" to "h1"))
 *
 * val result = agent.act("click the search button")
 * val history = agent.run("search for 'kotlin' and extract results")
 * session.close()
 * ```
 *
 * Example usage (explicit client):
 * ```kotlin
 * val client = PulsarClient()
 * client.createSession()
 * val session = AgenticSession(client)
 * session.open("https://example.com")
 * val result = session.act("click the search button")
 * session.close()
 * ```
 *
 * @param client PulsarClient instance for API communication
 */
class AgenticSession(
    client: PulsarClient
) : PulsarSession(client), PerceptiveAgent {

    companion object {
        private var defaultClient: PulsarClient? = null
        private var defaultSession: AgenticSession? = null

        /**
         * Checks if the default session needs to be recreated.
         */
        private fun needsNewDefaultSession(): Boolean {
            return defaultSession == null || defaultClient == null || defaultClient?.sessionId == null
        }

        /**
         * Gets or creates a default AgenticSession instance.
         *
         * This convenience method creates a session with automatic local driver
         * when no explicit URL is provided. The session is reused across
         * multiple calls.
         *
         * When called without parameters, it will automatically download and start
         * a local Browser4.jar instance if needed.
         *
         * Usage pattern similar to AgenticContexts.getOrCreateSession():
         * ```kotlin
         * val session = AgenticSession.getOrCreate()
         * val agent = session.companionAgent
         * val driver = session.getOrCreateBoundDriver()
         * ```
         *
         * @param baseUrl Optional explicit base URL. If provided, connects to remote server.
         *                If null, uses local driver mode.
         * @param useLocalDriver If true, automatically starts local Browser4 driver (default when baseUrl is null)
         * @return The default AgenticSession instance
         */
        fun getOrCreate(
            baseUrl: String? = null,
            useLocalDriver: Boolean = baseUrl == null
        ): AgenticSession {
            if (needsNewDefaultSession()) {
                val client = PulsarClient(
                    baseUrl = baseUrl,
                    useLocalDriver = useLocalDriver
                )

                runBlocking { client.createSession() }
                defaultClient = client
                defaultSession = AgenticSession(client)
            }
            return defaultSession!!
        }

        /**
         * Creates a new AgenticSession instance.
         *
         * Unlike [getOrCreate], this always creates a fresh session.
         *
         * @param baseUrl Optional explicit base URL. If provided, connects to remote server.
         *                If null, uses local driver mode.
         * @param useLocalDriver If true, automatically starts local Browser4 driver (default when baseUrl is null)
         * @return A new AgenticSession instance
         */
        suspend fun create(
            baseUrl: String? = null,
            useLocalDriver: Boolean = baseUrl == null
        ): AgenticSession {
            val client = PulsarClient(
                baseUrl = baseUrl,
                useLocalDriver = useLocalDriver
            )
            client.createSession()
            return AgenticSession(client)
        }

        /**
         * Closes and resets the default session.
         *
         * Call this to clean up the default session created by [getOrCreate].
         */
        fun resetDefault() {
            try {
                defaultSession?.close()
            } catch (e: Exception) {
                // Ignore errors during cleanup
            } finally {
                defaultSession = null
                defaultClient?.close()
                defaultClient = null
            }
        }
    }

    private val _stateHistory: MutableList<AgentState> = mutableListOf()
    private val _processTrace: MutableList<String> = mutableListOf()

    /**
     * Agent event handlers for receiving server-side agent events.
     *
     * Register handlers to receive agent lifecycle events (run, observe, act),
     * inference events, tool events, MCP events, and skill events.
     *
     * Example:
     * ```kotlin
     * session.agentEventHandlers.agent.on("onWillObserve") { event ->
     *     println("Observation starting: ${event.message}")
     * }
     * ```
     */
    val agentEventHandlers = AgentEventHandlers()

    /**
     * Gets the companion agent (self, for API compatibility).
     *
     * In this implementation, AgenticSession itself provides the agent functionality.
     */
    val companionAgent: PerceptiveAgent get() = this

    /**
     * Gets the agent state history.
     *
     * The state history tracks executed actions and their results,
     * providing memory for the agent's decision-making process.
     */
    override val stateHistory: AgentHistory
        get() = AgentHistory(
            states = _stateHistory.toMutableList(),
            hasErrors = _stateHistory.any { !it.success }
        )

    /**
     * Gets the process trace (list of actions taken).
     */
    override val processTrace: List<String> get() = _processTrace.toList()

    /**
     * Gets the context (self, for API compatibility).
     */
    val context: AgenticSession get() = this

    // ========== Agentic Operations ==========

    /**
     * Executes a single action described in natural language.
     *
     * This method converts the action description into browser operations
     * and executes them.
     *
     * @param action Natural language description of the action to perform
     * @param multiAct Whether each act forms a new chained context
     * @param modelName Optional LLM model name
     * @param variables Extra variables for prompt/tool
     * @param domSettleTimeoutMs Timeout for DOM settling
     * @param timeoutMs Overall timeout
     * @return [AgentActResult] with the action result
     */
    @Suppress("UNCHECKED_CAST")
    override suspend fun act(
        action: String,
        multiAct: Boolean,
        modelName: String?,
        variables: Map<String, String>?,
        domSettleTimeoutMs: Long?,
        timeoutMs: Long?
    ): AgentActResult {
        return agentAct(action, multiAct, modelName, variables, domSettleTimeoutMs, timeoutMs)
    }

    /**
     * Executes a single action described in natural language.
     *
     * @param action Natural language description of the action
     * @param multiAct Whether each act forms a new chained context
     * @param modelName Optional LLM model name
     * @param variables Extra variables for prompt/tool
     * @param domSettleTimeoutMs Timeout for DOM settling
     * @param timeoutMs Overall timeout
     * @return [AgentActResult] with the action result
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun agentAct(
        action: String,
        multiAct: Boolean = false,
        modelName: String? = null,
        variables: Map<String, String>? = null,
        domSettleTimeoutMs: Long? = null,
        timeoutMs: Long? = null
    ): AgentActResult {
        val payload = mutableMapOf<String, Any?>("action" to action)
        if (multiAct) payload["multiAct"] = multiAct
        if (modelName != null) payload["modelName"] = modelName
        if (variables != null) payload["variables"] = variables
        if (domSettleTimeoutMs != null) payload["domSettleTimeoutMs"] = domSettleTimeoutMs
        if (timeoutMs != null) payload["timeoutMs"] = timeoutMs

        val value = client.post("/session/{sessionId}/agent/act", payload)

        val result = if (value is Map<*, *>) {
            val map = value as Map<String, Any?>
            val trace = map["trace"] as? List<String>
            trace?.let { _processTrace.addAll(it) }

            // Add to state history
            val step = _stateHistory.size + 1
            _stateHistory.add(
                AgentState(
                    step = step,
                    action = action,
                    result = map["result"],
                    success = map["success"] as? Boolean ?: false,
                    message = map["message"] as? String ?: ""
                )
            )

            AgentActResult.fromMap(map)
        } else {
            AgentActResult()
        }

        return result
    }

    /**
     * Runs an autonomous agent task.
     *
     * This method runs an observe-act loop attempting to fulfill the
     * task described in natural language.
     *
     * @param task Natural language description of the task to accomplish
     * @param multiAct Whether each act forms a new chained context
     * @param modelName Optional LLM model name
     * @param variables Extra variables for prompt/tool
     * @param domSettleTimeoutMs Timeout for DOM settling
     * @param timeoutMs Overall timeout
     * @return [AgentRunResult] with the task result
     */
    override suspend fun run(
        task: String,
        multiAct: Boolean,
        modelName: String?,
        variables: Map<String, String>?,
        domSettleTimeoutMs: Long?,
        timeoutMs: Long?
    ): AgentRunResult {
        return agentRun(task, multiAct, modelName, variables, domSettleTimeoutMs, timeoutMs)
    }

    /**
     * Runs an autonomous agent task.
     *
     * @param task Natural language description of the task
     * @param multiAct Whether each act forms a new chained context
     * @param modelName Optional LLM model name
     * @param variables Extra variables for prompt/tool
     * @param domSettleTimeoutMs Timeout for DOM settling
     * @param timeoutMs Overall timeout
     * @return [AgentRunResult] with the task result
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun agentRun(
        task: String,
        multiAct: Boolean = false,
        modelName: String? = null,
        variables: Map<String, String>? = null,
        domSettleTimeoutMs: Long? = null,
        timeoutMs: Long? = null
    ): AgentRunResult {
        val payload = mutableMapOf<String, Any?>("task" to task)
        if (multiAct) payload["multiAct"] = multiAct
        if (modelName != null) payload["modelName"] = modelName
        if (variables != null) payload["variables"] = variables
        if (domSettleTimeoutMs != null) payload["domSettleTimeoutMs"] = domSettleTimeoutMs
        if (timeoutMs != null) payload["timeoutMs"] = timeoutMs

        val value = client.post("/session/{sessionId}/agent/run", payload)

        val result = if (value is Map<*, *>) {
            val map = value as Map<String, Any?>
            val trace = map["trace"] as? List<String>
            trace?.let { _processTrace.addAll(it) }

            // The run operation typically involves multiple steps
            // We track this as a high-level task in the state history
            val step = _stateHistory.size + 1
            _stateHistory.add(
                AgentState(
                    step = step,
                    action = "run: $task",
                    result = map["finalResult"],
                    success = map["success"] as? Boolean ?: false,
                    message = map["message"] as? String ?: ""
                )
            )

            AgentRunResult.fromMap(map)
        } else {
            AgentRunResult()
        }

        return result
    }

    /**
     * Observes the page and returns potential actions.
     *
     * @param instruction Optional observation instruction
     * @param modelName Optional LLM model name
     * @param domSettleTimeoutMs Timeout for DOM settling
     * @param returnAction Whether to return actionable tool calls
     * @param drawOverlay Whether to highlight interactive elements
     * @return [AgentObservation] with observation results
     */
    override suspend fun observe(
        instruction: String?,
        modelName: String?,
        domSettleTimeoutMs: Long?,
        returnAction: Boolean?,
        drawOverlay: Boolean
    ): AgentObservation {
        return agentObserve(instruction, modelName, domSettleTimeoutMs, returnAction, drawOverlay)
    }

    /**
     * Observes the page and returns potential actions.
     *
     * @param instruction Optional observation instruction
     * @param modelName Optional LLM model name
     * @param domSettleTimeoutMs Timeout for DOM settling
     * @param returnAction Whether to return actionable tool calls
     * @param drawOverlay Whether to highlight interactive elements
     * @return [AgentObservation] with observation results
     */
    suspend fun agentObserve(
        instruction: String? = null,
        modelName: String? = null,
        domSettleTimeoutMs: Long? = null,
        returnAction: Boolean? = null,
        drawOverlay: Boolean = true
    ): AgentObservation {
        val payload = mutableMapOf<String, Any?>()
        if (instruction != null) payload["instruction"] = instruction
        if (modelName != null) payload["modelName"] = modelName
        if (domSettleTimeoutMs != null) payload["domSettleTimeoutMs"] = domSettleTimeoutMs
        if (returnAction != null) payload["returnAction"] = returnAction
        payload["drawOverlay"] = drawOverlay

        val value = client.post("/session/{sessionId}/agent/observe", payload)
        return AgentObservation.fromAny(value)
    }

    /**
     * Extracts structured data from the page using AI.
     *
     * @param instruction Extraction instruction describing what to extract
     * @param schema Optional JSON schema for the extraction result
     * @param selector Optional CSS selector to scope extraction
     * @param modelName Optional LLM model name
     * @param domSettleTimeoutMs Timeout for DOM settling
     * @return [ExtractionResult] with extracted data
     */
    @Suppress("UNCHECKED_CAST")
    override suspend fun extract(
        instruction: String,
        schema: Map<String, Any?>?,
        selector: String?,
        modelName: String?,
        domSettleTimeoutMs: Long?
    ): ExtractionResult {
        return agentExtract(instruction, schema, selector, modelName, domSettleTimeoutMs)
    }

    /**
     * Extracts structured data from the page using AI (internal implementation).
     *
     * @param instruction Extraction instruction describing what to extract
     * @param schema Optional JSON schema for the extraction result
     * @param selector Optional CSS selector to scope extraction
     * @param modelName Optional LLM model name
     * @param domSettleTimeoutMs Timeout for DOM settling
     * @return [ExtractionResult] with extracted data
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun agentExtract(
        instruction: String,
        schema: Map<String, Any?>? = null,
        selector: String? = null,
        modelName: String? = null,
        domSettleTimeoutMs: Long? = null
    ): ExtractionResult {
        val payload = mutableMapOf<String, Any?>("instruction" to instruction)
        if (schema != null) payload["schema"] = schema
        if (selector != null) payload["selector"] = selector
        if (modelName != null) payload["modelName"] = modelName
        if (domSettleTimeoutMs != null) payload["domSettleTimeoutMs"] = domSettleTimeoutMs

        val value = client.post("/session/{sessionId}/agent/extract", payload)
        return if (value is Map<*, *>) {
            ExtractionResult.fromMap(value as Map<String, Any?>)
        } else {
            ExtractionResult()
        }
    }

    /**
     * Summarizes page content.
     *
     * @param instruction Optional guidance for summarization
     * @param selector Optional CSS selector to limit summarization scope
     * @return Summary text
     */
    override suspend fun summarize(instruction: String?, selector: String?): String {
        return agentSummarize(instruction, selector)
    }

    /**
     * Summarizes page content.
     *
     * @param instruction Optional guidance for summarization
     * @param selector Optional CSS selector to limit summarization scope
     * @return Summary text
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun agentSummarize(instruction: String? = null, selector: String? = null): String {
        val payload = mutableMapOf<String, Any?>()
        if (instruction != null) payload["instruction"] = instruction
        if (selector != null) payload["selector"] = selector

        val value = client.post("/session/{sessionId}/agent/summarize", payload)
        return when (value) {
            is Map<*, *> -> {
                val map = value as Map<String, Any?>
                map["summary"] as? String ?: map["value"] as? String ?: ""
            }
            is String -> value
            else -> ""
        }
    }

    /**
     * Clears the agent's history.
     *
     * This clears the history so new tasks remain unaffected by previous ones.
     *
     * @return True if history was cleared successfully
     */
    override suspend fun clearHistory(): Boolean {
        return agentClearHistory()
    }

    /**
     * Clears the agent's history.
     *
     * @return True if history was cleared successfully
     */
    suspend fun agentClearHistory(): Boolean {
        val value = client.post("/session/{sessionId}/agent/clearHistory", emptyMap())
        _processTrace.clear()
        _stateHistory.clear()
        return if (value != null) value as? Boolean ?: true else true
    }

    /**
     * Runs an agent task with event streaming.
     *
     * This method runs the task and simultaneously streams agent events to the
     * registered [agentEventHandlers]. Events are delivered as they occur during
     * the agent's observe-act loop.
     *
     * Example:
     * ```kotlin
     * session.agentEventHandlers.agent.on("onWillObserve") { event ->
     *     println("Observation starting: ${event.message}")
     * }
     * session.agentEventHandlers.agent.on("onDidAct") { event ->
     *     println("Action completed: ${event.metadata}")
     * }
     * val result = session.runWithEvents("search for 'kotlin' and click the first result")
     * ```
     *
     * @param task Natural language description of the task
     * @param multiAct Whether each act forms a new chained context
     * @param modelName Optional LLM model name
     * @param variables Extra variables for prompt/tool
     * @param domSettleTimeoutMs Timeout for DOM settling
     * @param timeoutMs Overall timeout
     * @return [AgentRunResult] with the task result
     */
    suspend fun runWithEvents(
        task: String,
        multiAct: Boolean = false,
        modelName: String? = null,
        variables: Map<String, String>? = null,
        domSettleTimeoutMs: Long? = null,
        timeoutMs: Long? = null
    ): AgentRunResult {
        return withAgentEventStreaming {
            agentRun(task, multiAct, modelName, variables, domSettleTimeoutMs, timeoutMs)
        }
    }

    /**
     * Executes an agent action with event streaming.
     *
     * @param action Natural language description of the action
     * @param multiAct Whether each act forms a new chained context
     * @param modelName Optional LLM model name
     * @param variables Extra variables for prompt/tool
     * @param domSettleTimeoutMs Timeout for DOM settling
     * @param timeoutMs Overall timeout
     * @return [AgentActResult] with the action result
     */
    suspend fun actWithEvents(
        action: String,
        multiAct: Boolean = false,
        modelName: String? = null,
        variables: Map<String, String>? = null,
        domSettleTimeoutMs: Long? = null,
        timeoutMs: Long? = null
    ): AgentActResult {
        return withAgentEventStreaming {
            agentAct(action, multiAct, modelName, variables, domSettleTimeoutMs, timeoutMs)
        }
    }

    /**
     * Observes the page with event streaming.
     *
     * @param instruction Optional observation instruction
     * @param modelName Optional LLM model name
     * @param domSettleTimeoutMs Timeout for DOM settling
     * @param returnAction Whether to return actionable tool calls
     * @param drawOverlay Whether to highlight interactive elements
     * @return [AgentObservation] with observation results
     */
    suspend fun observeWithEvents(
        instruction: String? = null,
        modelName: String? = null,
        domSettleTimeoutMs: Long? = null,
        returnAction: Boolean? = null,
        drawOverlay: Boolean = true
    ): AgentObservation {
        return withAgentEventStreaming {
            agentObserve(instruction, modelName, domSettleTimeoutMs, returnAction, drawOverlay)
        }
    }

    /**
     * Executes a block while streaming agent events to [agentEventHandlers].
     *
     * This method starts an SSE listener before executing the block and stops it
     * after the block completes. Events received during the block execution are
     * dispatched to the registered handlers.
     *
     * @param block The suspend function to execute with event streaming
     * @return The result of the block
     */
    private suspend fun <T> withAgentEventStreaming(block: suspend () -> T): T {
        val stopFlag = java.util.concurrent.atomic.AtomicBoolean(false)

        // Subscribe to agent events
        val subscription = try {
            val requestedEventTypes = agentEventHandlers.subscribedEventTypesOrNull()
                ?: listOf("onWillRun", "onDidRun", "onWillObserve", "onDidObserve",
                    "onWillAct", "onDidAct", "onWillInfer", "onDidInfer")
            val value = driver.subscribeEvents(mapOf("eventTypes" to requestedEventTypes))

            val map = value as? Map<String, Any?>
            val valueObj = map?.get("value")
            when (valueObj) {
                is Map<*, *> -> (valueObj as Map<String, Any?>)["subscriptionId"] as? String
                else -> map?.get("subscriptionId") as? String
            }
        } catch (_: Exception) {
            null
        }

        // Start SSE listener
        val listenerJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val base = client.resolvedBaseUrl.trimEnd('/')
                val sessionId = client.sessionId ?: return@launch
                val query = if (!subscription.isNullOrBlank()) {
                    "?subscriptionId=$subscription"
                } else {
                    ""
                }
                val streamUrl = "$base/session/$sessionId/events/stream$query"

                val sse = ai.platon.pulsar.sdk.v0.detail.SseClient(client.rawHttpClient)
                sse.connect(
                    url = streamUrl,
                    headers = client.resolvedDefaultHeaders.filterKeys { it.lowercase() != "content-type" },
                    shouldStop = { stopFlag.get() }
                ) { evt ->
                    val oe = ai.platon.pulsar.sdk.v0.detail.OpenApiEvent.fromJson(evt.data)
                    if (oe != null) {
                        agentEventHandlers.dispatchFromOpenApi(oe)
                    }
                }
            } catch (_: Exception) {
                // Swallow listener errors
            }
        }

        return try {
            block()
        } finally {
            stopFlag.set(true)
            listenerJob.cancel()
        }
    }

    /**
     * Closes the session and releases resources.
     */
    override fun close() {
        _processTrace.clear()
        _stateHistory.clear()
        super.close()
    }
}
