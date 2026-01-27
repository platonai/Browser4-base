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
package ai.platon.pulsar.sdk.v0

import ai.platon.pulsar.sdk.v0.detail.OpenApiEvent

/**
 * Reference to a DOM element, matching WebDriver element identifier.
 */
data class ElementRef(
    val elementId: String
)

/**
 * Represents a web page result from load/open operations.
 * Corresponds to the OpenAPI WebPageResult schema.
 */
data class WebPage(
    val url: String,
    val location: String? = null,
    val contentType: String? = null,
    val contentLength: Int = 0,
    val protocolStatus: String? = null,
    val isNil: Boolean = false,
    val html: String? = null
) {
    companion object {
        /**
         * Creates a WebPage from an API response map.
         */
        fun fromMap(data: Map<String, Any?>): WebPage {
            return WebPage(
                url = data["url"] as? String ?: "",
                location = data["location"] as? String,
                contentType = data["contentType"] as? String,
                contentLength = (data["contentLength"] as? Number)?.toInt() ?: 0,
                protocolStatus = data["protocolStatus"] as? String,
                isNil = data["isNil"] as? Boolean ?: false,
                html = data["html"] as? String
            )
        }
    }
}

/**
 * Normalized URL result.
 * Corresponds to the OpenAPI NormalizeResponse schema.
 */
data class NormURL(
    val spec: String,
    val url: String,
    val args: String? = null,
    val isNil: Boolean = false
) {
    companion object {
        /**
         * Creates a NormURL from an API response map.
         */
        fun fromMap(data: Map<String, Any?>): NormURL {
            return NormURL(
                spec = data["spec"] as? String ?: "",
                url = data["url"] as? String ?: "",
                args = data["args"] as? String,
                isNil = data["isNil"] as? Boolean ?: false
            )
        }
    }
}

/**
 * Result from agent run operation.
 * Corresponds to the OpenAPI AgentRunResponse schema.
 */
data class AgentRunResult(
    val success: Boolean = false,
    val message: String = "",
    val historySize: Int = 0,
    val processTraceSize: Int = 0,
    val finalResult: Any? = null,
    val trace: List<String>? = null
) {
    companion object {
        /**
         * Creates an AgentRunResult from an API response map.
         */
        @Suppress("UNCHECKED_CAST")
        fun fromMap(data: Map<String, Any?>): AgentRunResult {
            return AgentRunResult(
                success = data["success"] as? Boolean ?: false,
                message = data["message"] as? String ?: "",
                historySize = (data["historySize"] as? Number)?.toInt() ?: 0,
                processTraceSize = (data["processTraceSize"] as? Number)?.toInt() ?: 0,
                finalResult = data["finalResult"],
                trace = data["trace"] as? List<String>
            )
        }
    }
}

/**
 * Result from agent act operation.
 * Corresponds to the OpenAPI AgentActResponse schema.
 */
data class AgentActResult(
    val success: Boolean = false,
    val message: String = "",
    val action: String? = null,
    val isComplete: Boolean = false,
    val expression: String? = null,
    val result: Any? = null,
    val trace: List<String>? = null
) {
    companion object {
        /**
         * Creates an AgentActResult from an API response map.
         */
        @Suppress("UNCHECKED_CAST")
        fun fromMap(data: Map<String, Any?>): AgentActResult {
            return AgentActResult(
                success = data["success"] as? Boolean ?: false,
                message = data["message"] as? String ?: "",
                action = data["action"] as? String,
                isComplete = data["isComplete"] as? Boolean ?: false,
                expression = data["expression"] as? String,
                result = data["result"],
                trace = data["trace"] as? List<String>
            )
        }
    }
}

/**
 * Single observation result from agent observe operation.
 * Corresponds to the OpenAPI ObserveResult schema.
 */
data class ObserveResult(
    val locator: String? = null,
    val domain: String? = null,
    val method: String? = null,
    val arguments: Map<String, Any?>? = null,
    val description: String? = null,
    val screenshotContentSummary: String? = null,
    val currentPageContentSummary: String? = null,
    val nextGoal: String? = null,
    val thinking: String? = null,
    val summary: String? = null,
    val keyFindings: String? = null,
    val nextSuggestions: List<String>? = null
) {
    companion object {
        /**
         * Creates an ObserveResult from an API response map.
         */
        @Suppress("UNCHECKED_CAST")
        fun fromMap(data: Map<String, Any?>): ObserveResult {
            return ObserveResult(
                locator = data["locator"] as? String,
                domain = data["domain"] as? String,
                method = data["method"] as? String,
                arguments = data["arguments"] as? Map<String, Any?>,
                description = data["description"] as? String,
                screenshotContentSummary = data["screenshotContentSummary"] as? String,
                currentPageContentSummary = data["currentPageContentSummary"] as? String,
                nextGoal = data["nextGoal"] as? String,
                thinking = data["thinking"] as? String,
                summary = data["summary"] as? String,
                keyFindings = data["keyFindings"] as? String,
                nextSuggestions = data["nextSuggestions"] as? List<String>
            )
        }
    }
}

/**
 * Result from agent observe operation containing multiple observations.
 */
data class AgentObservation(
    val observations: List<ObserveResult> = emptyList()
) {
    companion object {
        /**
         * Creates an AgentObservation from an API response.
         */
        @Suppress("UNCHECKED_CAST")
        fun fromAny(data: Any?): AgentObservation {
            if (data is List<*>) {
                val observations = data.mapNotNull { item ->
                    when (item) {
                        is Map<*, *> -> ObserveResult.fromMap(item as Map<String, Any?>)
                        else -> null
                    }
                }
                return AgentObservation(observations)
            }
            return AgentObservation()
        }
    }
}

/**
 * Result from agent extract operation.
 * Corresponds to the OpenAPI AgentExtractResponse schema.
 */
data class ExtractionResult(
    val success: Boolean = false,
    val message: String = "",
    val data: Any? = null
) {
    companion object {
        /**
         * Creates an ExtractionResult from an API response map.
         */
        fun fromMap(data: Map<String, Any?>): ExtractionResult {
            return ExtractionResult(
                success = data["success"] as? Boolean ?: false,
                message = data["message"] as? String ?: "",
                data = data["data"]
            )
        }
    }
}

/**
 * Snapshot of a web page, used for capture operations.
 */
data class PageSnapshot(
    val url: String,
    val html: String? = null
)

/**
 * Result of field extraction with CSS selectors.
 */
data class FieldsExtraction(
    val fields: Map<String, Any?> = emptyMap()
)

/**
 * Result of a tool call execution.
 */
data class ToolCallResult(
    val success: Boolean = false,
    val message: String = "",
    val data: Any? = null
) {
    companion object {
        /**
         * Creates a ToolCallResult from an API response map.
         */
        fun fromMap(data: Map<String, Any?>): ToolCallResult {
            return ToolCallResult(
                success = data["success"] as? Boolean ?: false,
                message = data["message"] as? String ?: "",
                data = data["data"]
            )
        }
    }
}

/**
 * Description of an action to be performed.
 */
data class ActionDescription(
    val description: String,
    val parameters: Map<String, Any?>? = null
)

/**
 * SDK-side page event handlers.
 *
 * This is a lightweight, callback-based API intended for REST/OpenAPI mode.
 * Events are received from `/session/{sessionId}/events/stream` (SSE) and dispatched
 * locally by the SDK.
 *
 * Notes:
 * - This is intentionally minimal and string-based.
 * - The server currently emits events with an `eventType` string (e.g. "onLoaded").
 * - Handlers are grouped by phase for API compatibility with Browser4 core.
 */
class PageEventHandlers {
    /**
     * A group of event handlers keyed by `eventType`.
     */
    class Group {
        private val handlers: MutableMap<String, MutableList<(OpenApiEvent) -> Unit>> = mutableMapOf()

        /**
         * Registers a handler for an event type.
         *
         * @param eventType The event type emitted by the server, e.g. "onLoaded".
         * @param handler Callback invoked with the raw [OpenApiEvent].
         */
        fun on(eventType: String, handler: (OpenApiEvent) -> Unit): Group {
            val key = eventType.trim()
            require(key.isNotEmpty()) { "eventType must not be blank" }
            handlers.getOrPut(key) { mutableListOf() }.add(handler)
            return this
        }

        internal fun dispatch(event: OpenApiEvent) {
            handlers[event.eventType]?.forEach { h ->
                try {
                    h(event)
                } catch (_: Exception) {
                    // Best-effort: user callback errors must not break SSE processing.
                }
            }
        }

        internal fun isEmpty(): Boolean = handlers.isEmpty()
        internal fun keys(): Set<String> = handlers.keys
    }

    /** Load phase handlers (fetch/parse). */
    val load = Group()

    /** Browse phase handlers (navigation/interaction). */
    val browse = Group()

    /** Crawl phase handlers (iteration-level). */
    val crawl = Group()

    /**
     * Registers a handler for all phases.
     */
    fun onAny(eventType: String, handler: (OpenApiEvent) -> Unit): PageEventHandlers {
        load.on(eventType, handler)
        browse.on(eventType, handler)
        crawl.on(eventType, handler)
        return this
    }

    internal fun dispatch(event: OpenApiEvent) {
        load.dispatch(event)
        browse.dispatch(event)
        crawl.dispatch(event)
    }

    /**
     * Returns the set of event types registered in all groups.
     *
     * This is used by the SDK to decide what to subscribe to on the server.
     * If no event handlers are registered, this returns an empty set.
     */
    fun registeredEventTypes(): Set<String> {
        return (load.keys() + browse.keys() + crawl.keys()).toSet()
    }

    internal fun subscribedEventTypesOrNull(): List<String>? {
        val types = registeredEventTypes().toList()
        return types.ifEmpty { null }
    }
}

/**
 * Represents a single state in agent history.
 * Contains information about a step in the agent's execution.
 */
data class AgentState(
    val step: Int = 0,
    val action: String? = null,
    val result: Any? = null,
    val success: Boolean = false,
    val message: String = ""
)

/**
 * Agent history tracking execution states.
 * Provides memory of what actions have been performed.
 */
data class AgentHistory(
    val states: MutableList<AgentState> = mutableListOf(),
    val hasErrors: Boolean = false,
    val finalResult: Any? = null
) {
    val size: Int get() = states.size

    companion object {
        /**
         * Creates an AgentHistory from an API response map.
         */
        @Suppress("UNCHECKED_CAST")
        fun fromMap(data: Map<String, Any?>): AgentHistory {
            val statesList = data["states"] as? List<Map<String, Any?>> ?: emptyList()
            val states = statesList.map { stateMap ->
                AgentState(
                    step = (stateMap["step"] as? Number)?.toInt() ?: 0,
                    action = stateMap["action"] as? String,
                    result = stateMap["result"],
                    success = stateMap["success"] as? Boolean ?: false,
                    message = stateMap["message"] as? String ?: ""
                )
            }.toMutableList()

            return AgentHistory(
                states = states,
                hasErrors = data["hasErrors"] as? Boolean ?: false,
                finalResult = data["finalResult"]
            )
        }
    }
}

/**
 * Chat response from the LLM.
 */
data class ChatResponse(
    val content: String = "",
    val role: String = "assistant",
    val model: String? = null
) {
    companion object {
        /**
         * Creates a ChatResponse from an API response.
         */
        @Suppress("UNCHECKED_CAST")
        fun fromAny(data: Any?): ChatResponse {
            return when (data) {
                is Map<*, *> -> {
                    val map = data as Map<String, Any?>
                    ChatResponse(
                        content = map["content"] as? String ?: "",
                        role = map["role"] as? String ?: "assistant",
                        model = map["model"] as? String
                    )
                }
                is String -> ChatResponse(content = data)
                else -> ChatResponse()
            }
        }
    }
}

/**
 * Server-side agent event received via SSE.
 *
 * This represents events emitted by the server's AgentEventBus, including:
 * - Agent lifecycle events (onWillRun, onDidRun, onWillObserve, etc.)
 * - Inference events (onWillInfer, onDidInfer)
 * - Tool events (onWillExecuteTool, onDidExecuteTool)
 * - MCP events (onWillCallMCP, onDidCallMCP)
 * - Skill events (onWillRunSkill, onDidRunSkill)
 *
 * @property eventType The type of the event (e.g., "onWillObserve", "onDidAct").
 * @property eventPhase The phase of the event (e.g., "agent", "inference", "tool", "mcp", "skill").
 * @property agentId The unique identifier of the agent, if applicable.
 * @property message Optional message describing the event.
 * @property timestamp The timestamp when the event was created (epoch millis).
 * @property metadata Additional metadata associated with the event.
 */
data class AgentEvent(
    val eventType: String,
    val eventPhase: String,
    val agentId: String? = null,
    val message: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: Map<String, Any?> = emptyMap()
) {
    companion object {
        /**
         * Creates an AgentEvent from an OpenApiEvent.
         */
        fun fromOpenApiEvent(event: OpenApiEvent): AgentEvent {
            val data = event.data ?: emptyMap()
            return AgentEvent(
                eventType = event.eventType,
                eventPhase = data["eventPhase"] as? String ?: "agent",
                agentId = data["agentId"] as? String,
                message = data["message"] as? String,
                timestamp = event.timestamp,
                metadata = data
            )
        }

        /**
         * Creates an AgentEvent from a raw map.
         */
        @Suppress("UNCHECKED_CAST")
        fun fromMap(data: Map<String, Any?>): AgentEvent {
            return AgentEvent(
                eventType = data["eventType"] as? String ?: "",
                eventPhase = data["eventPhase"] as? String ?: "agent",
                agentId = data["agentId"] as? String,
                message = data["message"] as? String,
                timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                metadata = data["metadata"] as? Map<String, Any?> ?: emptyMap()
            )
        }
    }
}

/**
 * SDK-side agent event handlers.
 *
 * This is a lightweight, callback-based API for receiving agent lifecycle events
 * from the server via SSE. Events are dispatched to registered handlers based on
 * their event phase (agent, inference, tool, mcp, skill).
 *
 * Example usage:
 * ```kotlin
 * val handlers = AgentEventHandlers()
 * handlers.agent.on("onWillObserve") { event ->
 *     println("Agent starting observation: ${event.message}")
 * }
 * handlers.inference.on("onDidInfer") { event ->
 *     println("Inference completed: ${event.metadata}")
 * }
 * ```
 *
 * @see AgentEvent for the event data structure
 */
class AgentEventHandlers {
    /**
     * A group of event handlers keyed by `eventType`.
     */
    class Group(val phase: String) {
        private val handlers: MutableMap<String, MutableList<(AgentEvent) -> Unit>> = mutableMapOf()

        /**
         * Registers a handler for an event type.
         *
         * @param eventType The event type emitted by the server, e.g. "onWillObserve".
         * @param handler Callback invoked with the [AgentEvent].
         */
        fun on(eventType: String, handler: (AgentEvent) -> Unit): Group {
            val key = eventType.trim()
            require(key.isNotEmpty()) { "eventType must not be blank" }
            handlers.getOrPut(key) { mutableListOf() }.add(handler)
            return this
        }

        /**
         * Dispatches an event to registered handlers.
         */
        internal fun dispatch(event: AgentEvent) {
            handlers[event.eventType]?.forEach { h ->
                try {
                    h(event)
                } catch (_: Exception) {
                    // Best-effort: user callback errors must not break SSE processing.
                }
            }
        }

        internal fun isEmpty(): Boolean = handlers.isEmpty()
        internal fun keys(): Set<String> = handlers.keys
    }

    /** Agent lifecycle event handlers (run, observe, act, extract, summarize). */
    val agent = Group("agent")

    /** Inference phase event handlers (LLM calls). */
    val inference = Group("inference")

    /** Tool execution event handlers. */
    val tool = Group("tool")

    /** MCP (Model Context Protocol) event handlers. */
    val mcp = Group("mcp")

    /** Skill execution event handlers. */
    val skill = Group("skill")

    /**
     * Registers a handler for all phases.
     */
    fun onAny(eventType: String, handler: (AgentEvent) -> Unit): AgentEventHandlers {
        agent.on(eventType, handler)
        inference.on(eventType, handler)
        tool.on(eventType, handler)
        mcp.on(eventType, handler)
        skill.on(eventType, handler)
        return this
    }

    /**
     * Dispatches an event to the appropriate phase handler.
     */
    internal fun dispatch(event: AgentEvent) {
        when (event.eventPhase) {
            "agent" -> agent.dispatch(event)
            "inference" -> inference.dispatch(event)
            "tool" -> tool.dispatch(event)
            "mcp" -> mcp.dispatch(event)
            "skill" -> skill.dispatch(event)
            else -> {
                // Dispatch to all groups for unknown phases
                agent.dispatch(event)
                inference.dispatch(event)
                tool.dispatch(event)
                mcp.dispatch(event)
                skill.dispatch(event)
            }
        }
    }

    /**
     * Dispatches an OpenApiEvent by converting it to an AgentEvent first.
     */
    internal fun dispatchFromOpenApi(event: OpenApiEvent) {
        dispatch(AgentEvent.fromOpenApiEvent(event))
    }

    /**
     * Returns the set of event types registered in all groups.
     */
    fun registeredEventTypes(): Set<String> {
        return (agent.keys() + inference.keys() + tool.keys() + mcp.keys() + skill.keys()).toSet()
    }

    /**
     * Returns registered event types as a list, or null if none registered.
     */
    internal fun subscribedEventTypesOrNull(): List<String>? {
        val types = registeredEventTypes().toList()
        return types.ifEmpty { null }
    }

    /**
     * Common agent event type constants for convenience.
     */
    object EventTypes {
        // Agent lifecycle events
        const val ON_WILL_RUN = "onWillRun"
        const val ON_DID_RUN = "onDidRun"
        const val ON_WILL_OBSERVE = "onWillObserve"
        const val ON_DID_OBSERVE = "onDidObserve"
        const val ON_WILL_ACT = "onWillAct"
        const val ON_DID_ACT = "onDidAct"
        const val ON_WILL_EXTRACT = "onWillExtract"
        const val ON_DID_EXTRACT = "onDidExtract"
        const val ON_WILL_SUMMARIZE = "onWillSummarize"
        const val ON_DID_SUMMARIZE = "onDidSummarize"

        // Inference events
        const val ON_WILL_INFER = "onWillInfer"
        const val ON_DID_INFER = "onDidInfer"

        // Tool events
        const val ON_WILL_EXECUTE_TOOL = "onWillExecuteTool"
        const val ON_DID_EXECUTE_TOOL = "onDidExecuteTool"
        const val ON_TOOL_ERROR = "onToolError"

        // MCP events
        const val ON_WILL_CALL_MCP = "onWillCallMCP"
        const val ON_DID_CALL_MCP = "onDidCallMCP"
        const val ON_MCP_ERROR = "onMCPError"

        // Skill events
        const val ON_WILL_RUN_SKILL = "onWillRunSkill"
        const val ON_DID_RUN_SKILL = "onDidRunSkill"
        const val ON_SKILL_ERROR = "onSkillError"
    }
}
