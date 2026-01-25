package ai.platon.pulsar.common.event

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * A simple event bus for registering and emitting general events in a non-blocking manner.
 *
 * This event bus allows registration of event handlers for specific event types and
 * emits events to the registered handlers asynchronously using coroutines.
 */
object DangerousEventBus {

    private val generalEventHandlers = ConcurrentHashMap<String, GeneralEventHandler>()

    /**
     * Background coroutine scope for non-blocking event emission.
     * Uses Dispatchers.Default for CPU-bound work and SupervisorJob to isolate failures.
     */
    private val eventScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun emit(name: String, payload: Any) {
        generalEventHandlers[name]?.let { handlers ->
            eventScope.launch {
                handlers.invoke(payload)
            }
        }
    }

    fun register(name: String, handler: (Any) -> Any?): GeneralEventHandler {
        val handler = object : GeneralEventHandler(name) {
            override fun invoke(payload: Any): Any? {
                return handler.invoke(payload)
            }
        }

        return register(name, handler)
    }

    fun register(name: String, handler: GeneralEventHandler): GeneralEventHandler {
        generalEventHandlers[name] = handler
        return handler
    }

    fun unregister(eventType: String) {
        generalEventHandlers.remove(eventType)
    }
}
