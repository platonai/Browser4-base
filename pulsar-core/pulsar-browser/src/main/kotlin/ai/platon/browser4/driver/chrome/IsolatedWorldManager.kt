package ai.platon.browser4.driver.chrome

import ai.platon.pulsar.common.getLogger

/**
 * Manages isolated worlds for Browser4 runtime injection.
 *
 * The IsolatedWorldManager implements the dual-world architecture:
 * - Page World: Contains minimal stealth patches and fingerprint patches
 * - Isolated World: Contains the full Browser4 runtime that is invisible to page JavaScript
 *
 * Design Goals:
 * 1. Page JS cannot directly access Agent Runtime
 * 2. Page JS cannot reliably detect Agent Runtime
 * 3. CDP / Agent can stably access Runtime
 * 4. Runtime is versionable and hot-updatable
 * 5. Runtime doesn't break page behavior
 * 6. Runtime is observable, extensible, and evolvable
 */
class IsolatedWorldManager(
    private val devTools: RemoteDevTools
) {
    companion object {
        private val logger = getLogger(this)
        
        /**
         * Isolated world name for Browser4 runtime.
         * This name is used to identify and access the isolated world via CDP.
         */
        const val RUNTIME_WORLD_NAME = "__browser4_runtime__"
        
        /**
         * Version of the Browser4 runtime.
         * Increment this when the runtime API changes.
         */
        const val RUNTIME_VERSION = "1.0.0"
    }
    
    private val pageAPI get() = devTools.page
    private val runtimeAPI get() = devTools.runtime
    
    /**
     * Stores the frame ID to isolated world context ID mapping.
     * Key: frameId, Value: executionContextId
     */
    private val isolatedWorldContexts = mutableMapOf<String, Int>()
    
    /**
     * Creates an isolated world for the given frame.
     * 
     * @param frameId The frame ID to create the isolated world in. 
     *                If null, creates in the main frame.
     * @return The execution context ID of the created isolated world
     */
    suspend fun createIsolatedWorld(frameId: String? = null): Int {
        logger.info("Creating isolated world '{}' for frame: {}", RUNTIME_WORLD_NAME, frameId ?: "main")
        
        // Create isolated world using CDP Page.createIsolatedWorld
        val params = mutableMapOf<String, Any?>(
            "worldName" to RUNTIME_WORLD_NAME,
            "grantUniversalAccess" to true // Allow access to cross-origin frames
        )
        
        if (frameId != null) {
            params["frameId"] = frameId
        }
        
        val result = devTools.invoke<Map<String, Any>>(
            "Page.createIsolatedWorld",
            params,
            null
        )
        
        val executionContextId = (result?.get("executionContextId") as? Number)?.toInt()
            ?: throw IllegalStateException("Failed to create isolated world: no executionContextId returned")
        
        if (frameId != null) {
            isolatedWorldContexts[frameId] = executionContextId
        }
        
        logger.info("Created isolated world with execution context ID: {}", executionContextId)
        return executionContextId
    }
    
    /**
     * Evaluates JavaScript in the isolated world.
     * 
     * @param script The JavaScript code to evaluate
     * @param contextId The execution context ID of the isolated world.
     *                  If null, evaluates in the default isolated world.
     * @return The result of the evaluation
     */
    suspend fun evaluateInIsolatedWorld(
        script: String,
        contextId: Int? = null
    ): Any? {
        val params = mutableMapOf<String, Any?>(
            "expression" to script,
            "returnByValue" to true,
            "awaitPromise" to true
        )
        
        if (contextId != null) {
            params["contextId"] = contextId
        }
        
        val result = devTools.invoke<Map<String, Any>>(
            "Runtime.evaluate",
            params,
            null
        )
        
        return result?.get("result")
    }
    
    /**
     * Injects the Browser4 runtime into the isolated world.
     * 
     * @param runtimeScript The compiled runtime JavaScript code
     * @param contextId The execution context ID of the isolated world
     */
    suspend fun injectRuntime(runtimeScript: String, contextId: Int) {
        logger.info("Injecting Browser4 runtime (v{}) into isolated world context {}", 
            RUNTIME_VERSION, contextId)
        
        // Wrap the runtime with version info
        val versionedScript = """
            // Browser4 Runtime v$RUNTIME_VERSION
            (function() {
                'use strict';
                
                const __browser4_runtime__ = {
                    version: '$RUNTIME_VERSION',
                    worldName: '$RUNTIME_WORLD_NAME',
                };
                
                $runtimeScript
                
                // Expose runtime API (but only in isolated world)
                if (typeof window !== 'undefined') {
                    Object.defineProperty(window, '__browser4_runtime__', {
                        value: __browser4_runtime__,
                        writable: false,
                        enumerable: false,
                        configurable: false
                    });
                }
                
                return __browser4_runtime__;
            })();
        """.trimIndent()
        
        evaluateInIsolatedWorld(versionedScript, contextId)
        
        logger.info("Browser4 runtime injection completed")
    }
    
    /**
     * Gets the execution context ID for a frame's isolated world.
     * 
     * @param frameId The frame ID
     * @return The execution context ID, or null if not found
     */
    fun getContextId(frameId: String): Int? {
        return isolatedWorldContexts[frameId]
    }
    
    /**
     * Clears all isolated world contexts.
     * Called when navigating to a new page or closing the tab.
     */
    fun clearContexts() {
        isolatedWorldContexts.clear()
        logger.debug("Cleared all isolated world contexts")
    }
}
