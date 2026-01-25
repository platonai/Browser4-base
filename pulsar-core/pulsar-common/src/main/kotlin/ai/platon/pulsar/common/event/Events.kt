@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE", "unused")

package ai.platon.pulsar.common.event

import ai.platon.pulsar.common.lang.AbstractChainedFunction1
import kotlin.reflect.KClass

open class GeneralEventHandler(
    override val name: String = "GeneralEventHandler"
) : AbstractChainedFunction1<Any, Any?>() {
    override fun invoke(payload: Any): Any? {
        return super.invoke(param = payload)
    }
}

data class TargetMethodPayload(
    val target: Any,
    val method: String,
    val args: Map<String, Any?> = emptyMap()
)

open class TargetMethodEventHandler(
    val target: KClass<out Any>? = null,
    val method: String? = null,
    name: String = "TargetMethodEventHandler",
) : GeneralEventHandler("TargetMethodEventHandler") {

    override fun invoke(payload: Any): Any? {
        val p = payload as? TargetMethodPayload ?: return null
        return super.invoke(p)
    }
}

interface TargetMethodHandlers {
    val onWillExecute: TargetMethodEventHandler

    val onDidExecute: TargetMethodEventHandler

    /**
     * Chains another general event handler to the tail of this one.
     *
     * @param other The general event handlers to chain
     * @return This handler instance for fluent chaining
     */
    fun chain(other: TargetMethodHandlers): TargetMethodHandlers
}

abstract class AbstractGeneralEventHandlers : TargetMethodHandlers {

    override val onDidExecute: TargetMethodEventHandler = TargetMethodEventHandler()

    override val onWillExecute: TargetMethodEventHandler = TargetMethodEventHandler()

    override fun chain(other: TargetMethodHandlers): TargetMethodHandlers {
        onWillExecute.addLast(other.onWillExecute)
        onDidExecute.addLast(other.onDidExecute)
        return this
    }
}

/**
 * The default crawl event handler.
 */
open class DefaultGeneralEventHandlers : AbstractGeneralEventHandlers()
