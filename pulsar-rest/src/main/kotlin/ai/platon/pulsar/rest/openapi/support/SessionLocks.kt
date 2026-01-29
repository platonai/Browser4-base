package ai.platon.pulsar.rest.openapi.support

import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.ConcurrentHashMap

/**
 * A lightweight lock registry keyed by sessionId.
 *
 * Used to serialize non-reentrant operations (like navigation) per session.
 */
object SessionLocks {
    private val locks = ConcurrentHashMap<String, Mutex>()

    /**
     * Gets the [Mutex] for a session.
     */
    fun forSession(sessionId: String): Mutex = locks.computeIfAbsent(sessionId) { Mutex() }

    /**
     * Removes the lock for a sessionId.
     *
     * Safe to call multiple times.
     */
    fun remove(sessionId: String) {
        locks.remove(sessionId)
    }
}
