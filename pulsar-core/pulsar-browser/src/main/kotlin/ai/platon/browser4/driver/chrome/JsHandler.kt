package ai.platon.browser4.driver.chrome

import ai.platon.browser4.driver.chrome.util.ChromeDriverException
import ai.platon.cdt.kt.protocol.types.runtime.CallFunctionOn
import ai.platon.cdt.kt.protocol.types.runtime.Evaluate
import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.js.JsUtils
import ai.platon.pulsar.common.serialize.json.prettyPulsarObjectMapper
import ai.platon.pulsar.common.serialize.json.pulsarObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue

class JsHandler(
    private val devTools: RemoteDevTools,
    private val pageHandler: PageHandler,
    private val isolatedWorldManager: IsolatedWorldManager,
) {
    private val logger = getLogger(this)

    private val isActive get() = AppContext.isActive && devTools.isOpen
    private val pageAPI get() = devTools.page.takeIf { isActive }
    private val domAPI get() = devTools.dom.takeIf { isActive }
    private val cssAPI get() = devTools.css.takeIf { isActive }
    private val runtimeAPI get() = devTools.runtime.takeIf { isActive }

    private val confuser get() = isolatedWorldManager.settings.confuser

    /**
     * Evaluates expression on global object.
     *
     * @param expression Javascript expression to evaluate
     * @return Remote object value in case of primitive values or JSON values (if it was requested).
     * */
    @Throws(ChromeDriverException::class)
    suspend fun evaluateDetail(script: String): Evaluate? {
        val expression: String
        val lines = script.split('\n').map { it.trim() }.filter { it.isNotBlank() }
        // Check if this script is a IIFE
        if (lines.size > 1) {
            val firstLine = lines[0]
            expression = if (!firstLine.startsWith("(")) {
                JsUtils.toIIFE(script)
            } else {
                script
            }
        } else {
            expression = script
        }

        val confusedExpr = confuser.confuse(expression)

//        val isolatedContextId = isolatedWorldManager
//            .getContextId(runCatching { pageAPI?.getFrameTree()?.frame?.id }.getOrNull())
//        if (isolatedContextId != null && isolatedContextId > 0) {
//            val isolatedResult = runCatching { evaluateInContext(confusedExpr, isolatedContextId, returnByValue = false) }
//                .getOrNull()
//            if (isolatedResult != null) {
//                return isolatedResult
//            }
//        }

        return try {
            runtimeAPI?.evaluate(confusedExpr)
        } catch (e: Exception) {
            logger.warn("Failed to evaluate $expression", e)
            null
        }
    }

    /**
     * Evaluates expression on global object.
     *
     * @param expression Javascript expression to evaluate
     * @return Remote object value in case of primitive values or JSON values (if it was requested).
     * */
    @Throws(ChromeDriverException::class)
    suspend fun evaluate(script: String): Any? {
        val evaluate = if (script.contains("__pulsar_utils__")) {
            // Just for debugging purpose
            evaluateDetail(script)
        } else {
            evaluateDetail(script.trim())
        }

        val exception = evaluate?.exceptionDetails?.exception
        if (exception != null) {
            logger.warn(exception.description + "\n>>>$script<<<")
            throw RuntimeException(prettyPulsarObjectMapper().writeValueAsString(exception))
        }

        val result = evaluate?.result
        return result?.value
    }

    @Throws(ChromeDriverException::class)
    suspend fun evaluateValueDetail(script: String): Evaluate? {
        val expression: String
        val lines = script.split('\n').map { it.trim() }.filter { it.isNotBlank() }
        // Check if this script is a IIFE
        if (lines.size > 1) {
            val firstLine = lines[0]
            expression = if (!firstLine.startsWith("(")) {
                JsUtils.toIIFE(confuser.confuse(script))
            } else {
                script
            }
        } else {
            expression = script
        }

        val confusedExpr = confuser.confuse(expression)

        val isolatedContextId = isolatedWorldManager
            ?.getContextId(runCatching { pageAPI?.getFrameTree()?.frame?.id }.getOrNull())
        if (isolatedContextId != null && isolatedContextId > 0) {
            val isolatedResult = runCatching {
                evaluateInContext(confusedExpr, isolatedContextId, returnByValue = true) }.getOrNull()
            if (isolatedResult != null) {
                return isolatedResult
            }
        }

        return try {
            runtimeAPI?.evaluate(confusedExpr, returnByValue = true)
        } catch (e: Exception) {
            logger.warn("Failed to evaluate $script", e)
            null
        }
    }

    /**
     * Evaluates expression on global object.
     *
     * @param expression Javascript expression to evaluate
     * @return Remote object value in case of primitive values or JSON values (if it was requested).
     * */
    @Throws(ChromeDriverException::class)
    suspend fun evaluateValue(script: String): Any? {
        val evaluate = evaluateValueDetail(script)

        val exception = evaluate?.exceptionDetails?.exception
        if (exception != null) {
            logger.info(exception.description + "\n>>>$script<<<")
        }

        return evaluate?.result?.value
    }

    @Throws(ChromeDriverException::class)
    suspend fun evaluateValueDetail(selector: String, functionDeclaration: String): CallFunctionOn? {
        val node = pageHandler.resolveSelector(selector) ?: return null
        // Resolve a fresh objectId and ensure it's released after the call
        val resolved = try {
            when {
                node.nodeId > 0 -> domAPI?.resolveNode(node.nodeId, null, null, null)
                node.backendNodeId > 0 -> domAPI?.resolveNode(null, node.backendNodeId, null, null)
                else -> null
            }
        } catch (e: Exception) {
            null
        } ?: return null

        val oid = resolved.objectId ?: return null
        return try {
            runtimeAPI?.callFunctionOn(functionDeclaration, objectId = oid, returnByValue = true)
        } finally {
            try {
                runtimeAPI?.releaseObject(oid)
            } catch (_: Exception) {
            }
        }
    }

    @Throws(ChromeDriverException::class)
    suspend fun evaluateValue(selector: String, functionDeclaration: String): Any? {
        val reslut = evaluateValueDetail(selector, functionDeclaration)

        val exception = reslut?.exceptionDetails?.exception
        if (exception != null) {
            logger.info(exception.description + "\n>>>$functionDeclaration<<<")
        }

        return reslut?.result?.value
    }

    private suspend fun evaluateInContext(expression: String, contextId: Int, returnByValue: Boolean): Evaluate? {
        val params = mutableMapOf<String, Any?>(
            "expression" to expression,
            "contextId" to contextId,
            "returnByValue" to returnByValue,
            "awaitPromise" to true,
        )

        // runtimeAPI.evaluate()
        val raw = devTools.invoke<Map<String, Any?>>("Runtime.evaluate", params, null) ?: return null
        return runCatching { pulsarObjectMapper().convertValue(raw) as Evaluate }.getOrNull()
    }
}
