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
package ai.platon.pulsar.sdk

/**
 * PerceptiveAgent interface matching Browser4's native PerceptiveAgent API.
 *
 * This interface provides AI-powered browser automation capabilities through
 * the Browser4 REST API. It is implemented by [AgenticSession] on the client side.
 *
 * Key capabilities:
 * - **observe**: Analyze the page and return potential actions
 * - **act**: Execute a single action described in natural language
 * - **run**: Execute multi-step autonomous tasks
 * - **extract**: AI-powered structured data extraction
 * - **summarize**: Generate natural language summaries of page content
 *
 * Example usage:
 * ```kotlin
 * val session = AgenticSession.getOrCreate()
 * val agent: PerceptiveAgent = session.companionAgent
 *
 * // Single action
 * val actResult = agent.act("click the search button")
 *
 * // Multi-step autonomous task
 * val runResult = agent.run("search for 'kotlin' and extract the first 5 results")
 *
 * // Observe page and get suggestions
 * val observations = agent.observe("What actions can I take?")
 *
 * // Extract structured data
 * val extractResult = agent.extract("Extract all product names and prices")
 *
 * // Summarize content
 * val summary = agent.summarize("Summarize the main content")
 * ```
 *
 * @see AgenticSession
 */
interface PerceptiveAgent {

    /**
     * The agent state history tracking executed actions and their results.
     *
     * The state history provides memory of what has been done, helping the
     * agent make better decisions and avoid repeating failed actions.
     */
    val stateHistory: AgentHistory

    /**
     * The process trace listing all actions taken in order.
     *
     * Unlike stateHistory which contains full state information, processTrace
     * is a simple list of action descriptions useful for logging and debugging.
     */
    val processTrace: List<String>

    /**
     * Observes the page and returns potential actions.
     *
     * This method analyzes the current page state and suggests possible
     * actions that can be performed. It's useful for understanding what
     * elements are interactive and what operations are possible.
     *
     * @param instruction Optional instruction for what to observe
     * @param modelName Optional LLM model name to use
     * @param domSettleTimeoutMs Timeout for DOM to become stable
     * @param returnAction Whether to include actionable tool calls in results
     * @param drawOverlay Whether to highlight interactive elements
     * @return [AgentObservation] containing observation results
     */
    suspend fun observe(
        instruction: String? = null,
        modelName: String? = null,
        domSettleTimeoutMs: Long? = null,
        returnAction: Boolean? = null,
        drawOverlay: Boolean = true
    ): AgentObservation

    /**
     * Executes a single action described in natural language.
     *
     * This method converts natural language instructions into browser
     * operations and executes them. Examples:
     * - "click the search button"
     * - "type 'hello world' into the search box"
     * - "scroll to the bottom of the page"
     * - "navigate to https://example.com"
     *
     * @param action Natural language description of the action to perform
     * @param multiAct Whether each act forms a new chained context
     * @param modelName Optional LLM model name
     * @param variables Extra variables for prompt/tool
     * @param domSettleTimeoutMs Timeout for DOM settling
     * @param timeoutMs Overall timeout for the action
     * @return [AgentActResult] with the execution result
     */
    suspend fun act(
        action: String,
        multiAct: Boolean = false,
        modelName: String? = null,
        variables: Map<String, String>? = null,
        domSettleTimeoutMs: Long? = null,
        timeoutMs: Long? = null
    ): AgentActResult

    /**
     * Runs an autonomous multi-step task.
     *
     * This method executes an observe-act loop, repeatedly analyzing the
     * page and taking actions until the task is complete or a limit is reached.
     *
     * Example tasks:
     * - "search for 'kotlin sdk' and click the first result"
     * - "fill out the form with test data and submit it"
     * - "find all products priced under $50 and add them to cart"
     *
     * @param task Natural language description of the task to accomplish
     * @param multiAct Whether each act forms a new chained context
     * @param modelName Optional LLM model name
     * @param variables Extra variables for prompt/tool
     * @param domSettleTimeoutMs Timeout for DOM settling
     * @param timeoutMs Overall timeout for the entire task
     * @return [AgentRunResult] with the task execution result
     */
    suspend fun run(
        task: String,
        multiAct: Boolean = false,
        modelName: String? = null,
        variables: Map<String, String>? = null,
        domSettleTimeoutMs: Long? = null,
        timeoutMs: Long? = null
    ): AgentRunResult

    /**
     * Extracts structured data from the page using AI.
     *
     * This method uses an LLM to understand the page content and extract
     * requested information in a structured format.
     *
     * @param instruction Description of what data to extract
     * @param schema Optional JSON schema constraining the result structure
     * @param selector Optional CSS selector to scope the extraction
     * @param modelName Optional LLM model name
     * @param domSettleTimeoutMs Timeout for DOM settling
     * @return [ExtractionResult] with the extracted data
     */
    suspend fun extract(
        instruction: String,
        schema: Map<String, Any?>? = null,
        selector: String? = null,
        modelName: String? = null,
        domSettleTimeoutMs: Long? = null
    ): ExtractionResult

    /**
     * Summarizes the current page content.
     *
     * This method generates a natural language summary of the page or a
     * specific element within it.
     *
     * @param instruction Optional guidance for the summarization tone/focus
     * @param selector Optional CSS selector to limit the scope
     * @return Summary text
     */
    suspend fun summarize(instruction: String? = null, selector: String? = null): String

    /**
     * Clears the agent's history.
     *
     * Call this method to reset the agent's memory, ensuring that new tasks
     * are not affected by previous actions or their results.
     *
     * @return True if the history was cleared successfully
     */
    suspend fun clearHistory(): Boolean
}
