package ai.platon.pulsar.agentic.event

/**
 * Centralized event type definitions for the agentic module.
 * 
 * This object provides a unified location for all event types used with DangerousEventBus,
 * making it easier to discover, maintain, and document the events in the system.
 */
object AgenticEvents {
    
    /**
     * Events emitted by PerceptiveAgent implementations.
     */
    object PerceptiveAgent {
        /**
         * Emitted before executing the run method.
         * Payload: Map containing "action" (ActionOptions), "uuid" (UUID)
         */
        const val RUN_WILL_EXECUTE = "PerceptiveAgent.run.willExecute"
        
        /**
         * Emitted after executing the run method.
         * Payload: Map containing "action" (ActionOptions), "uuid" (UUID), 
         *          "result" (ActResult), "stateHistory" (AgentHistory)
         */
        const val RUN_DID_EXECUTE = "PerceptiveAgent.run.didExecute"
        
        /**
         * Emitted before executing the observe method.
         * Payload: Map containing "options" (ObserveOptions), "uuid" (UUID)
         */
        const val OBSERVE_WILL_EXECUTE = "PerceptiveAgent.observe.willExecute"
        
        /**
         * Emitted after executing the observe method.
         * Payload: Map containing "options" (ObserveOptions), "uuid" (UUID),
         *          "observeResults" (List<ObserveResult>), "actionDescription" (ActionDescription)
         */
        const val OBSERVE_DID_EXECUTE = "PerceptiveAgent.observe.didExecute"
        
        /**
         * Emitted before executing the act method.
         * Payload: Map containing "action" (ActionOptions), "uuid" (UUID)
         */
        const val ACT_WILL_EXECUTE = "PerceptiveAgent.act.willExecute"
        
        /**
         * Emitted after executing the act method.
         * Payload: Map containing "action" (ActionOptions), "uuid" (UUID), "result" (ActResult)
         */
        const val ACT_DID_EXECUTE = "PerceptiveAgent.act.didExecute"
        
        /**
         * Emitted before executing the extract method.
         * Payload: Map containing "options" (ExtractOptions), "uuid" (UUID)
         */
        const val EXTRACT_WILL_EXECUTE = "PerceptiveAgent.extract.willExecute"
        
        /**
         * Emitted after executing the extract method.
         * Payload: Map containing "options" (ExtractOptions), "uuid" (UUID), "result" (ExtractResult)
         */
        const val EXTRACT_DID_EXECUTE = "PerceptiveAgent.extract.didExecute"
        
        /**
         * Emitted before executing the summarize method.
         * Payload: Map containing "instruction" (String?), "selector" (String?), "uuid" (UUID)
         */
        const val SUMMARIZE_WILL_EXECUTE = "PerceptiveAgent.summarize.willExecute"
        
        /**
         * Emitted after executing the summarize method.
         * Payload: Map containing "instruction" (String?), "selector" (String?), 
         *          "uuid" (UUID), "result" (String)
         */
        const val SUMMARIZE_DID_EXECUTE = "PerceptiveAgent.summarize.didExecute"
    }
    
    /**
     * Events emitted by InferenceEngine.
     */
    object InferenceEngine {
        /**
         * Emitted before observe inference in BasicBrowserAgent init block.
         * Payload: Map containing "messages" (AgentMessageList)
         */
        const val OBSERVE_WILL_EXECUTE = "InferenceEngine.observe.willExecute"
        
        /**
         * Emitted after observe inference in BasicBrowserAgent init block.
         * Payload: Map containing "actionDescription" (ActionDescription)
         */
        const val OBSERVE_DID_EXECUTE = "InferenceEngine.observe.didExecute"
        
        /**
         * Emitted before extract inference.
         * Payload: Map containing "params" (ExtractParams)
         */
        const val EXTRACT_WILL_EXECUTE = "InferenceEngine.extract.willExecute"
        
        /**
         * Emitted after extract inference.
         * Payload: Map containing "params" (ExtractParams), "result" (ObjectNode),
         *          "extractedNode" (ObjectNode), "metaNode" (ObjectNode)
         */
        const val EXTRACT_DID_EXECUTE = "InferenceEngine.extract.didExecute"
        
        /**
         * Emitted before summarize inference.
         * Payload: Map containing "instruction" (String?), "messages" (AgentMessageList), 
         *          "textContent" (String)
         */
        const val SUMMARIZE_WILL_EXECUTE = "InferenceEngine.summarize.willExecute"
        
        /**
         * Emitted after summarize inference.
         * Payload: Map containing "instruction" (String?), "textContentLength" (Int),
         *          "result" (String), "tokenUsage" (TokenUsage)
         */
        const val SUMMARIZE_DID_EXECUTE = "InferenceEngine.summarize.didExecute"
    }
    
    /**
     * Events emitted by ContextToAction during action generation.
     */
    object ContextToAction {
        /**
         * Emitted before generating action from context.
         * Payload: Map containing "context" (ExecutionContext), "messages" (AgentMessageList)
         */
        const val GENERATE_WILL_EXECUTE = "ContextToAction.generate.willExecute"
        
        /**
         * Emitted after generating action from context.
         * Payload: Map containing "context" (ExecutionContext), "messages" (AgentMessageList),
         *          "actionDescription" (ActionDescription)
         */
        const val GENERATE_DID_EXECUTE = "ContextToAction.generate.didExecute"
    }
    
    /**
     * Returns all event types as a list for easy iteration.
     */
    fun getAllEventTypes(): List<String> = listOf(
        // PerceptiveAgent events
        PerceptiveAgent.RUN_WILL_EXECUTE,
        PerceptiveAgent.RUN_DID_EXECUTE,
        PerceptiveAgent.OBSERVE_WILL_EXECUTE,
        PerceptiveAgent.OBSERVE_DID_EXECUTE,
        PerceptiveAgent.ACT_WILL_EXECUTE,
        PerceptiveAgent.ACT_DID_EXECUTE,
        PerceptiveAgent.EXTRACT_WILL_EXECUTE,
        PerceptiveAgent.EXTRACT_DID_EXECUTE,
        PerceptiveAgent.SUMMARIZE_WILL_EXECUTE,
        PerceptiveAgent.SUMMARIZE_DID_EXECUTE,
        // InferenceEngine events
        InferenceEngine.OBSERVE_WILL_EXECUTE,
        InferenceEngine.OBSERVE_DID_EXECUTE,
        InferenceEngine.EXTRACT_WILL_EXECUTE,
        InferenceEngine.EXTRACT_DID_EXECUTE,
        InferenceEngine.SUMMARIZE_WILL_EXECUTE,
        InferenceEngine.SUMMARIZE_DID_EXECUTE,
        // ContextToAction events
        ContextToAction.GENERATE_WILL_EXECUTE,
        ContextToAction.GENERATE_DID_EXECUTE
    )
}
