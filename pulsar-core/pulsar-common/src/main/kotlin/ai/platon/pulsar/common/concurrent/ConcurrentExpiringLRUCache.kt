package ai.platon.pulsar.common.concurrent

import java.time.Duration
import java.time.Instant

/**
 * A cached value with the time it was created/inserted.
 */
class ExpiringItem<T>(
        val datum: T,
        val timestamp: Long = System.currentTimeMillis()
) {
    constructor(datum: T, instant: Instant): this(datum, instant.toEpochMilli())

    fun isExpired(expires: Duration, now: Instant = Instant.now()): Boolean {
        // Expire at the boundary to avoid "lives forever" when checked at exact ttl.
        return timestamp + expires.toMillis() <= now.toEpochMilli()
    }

    fun isExpired(expireMillis: Long, now: Long = System.currentTimeMillis()): Boolean {
        // Expire at the boundary to avoid "lives forever" when checked at exact ttl.
        return timestamp + expireMillis <= now
    }
}

/**
 * A small concurrent LRU cache with two layers of expiration:
 * - coarse TTL bucketing enforced by [ConcurrentLRUCache] (seconds precision)
 * - per-entry expiry checks using [ExpiringItem.timestamp]
 *
 * Notes:
 * - [ttl] should be >= 0. When set to 0, the underlying cache doesn't apply TTL bucketing.
 * - This cache doesn't proactively purge expired items; callers should use [getDatum] overloads
 *   (or [computeDatumIfAbsent]) when expiry correctness matters.
 */
class ConcurrentExpiringLRUCache<K, T>(
    val ttl: Duration = CACHE_TTL,
    val capacity: Int = CACHE_CAPACITY,
) {
    companion object {
        val CACHE_TTL = Duration.ofMinutes(5)
        const val CACHE_CAPACITY = 200
    }

    init {
        require(!ttl.isNegative) { "ttl must be >= 0" }
        require(capacity > 0) { "capacity must be > 0" }
    }

    private val cache = ConcurrentLRUCache<K, ExpiringItem<T>>(ttl.seconds, capacity)

    val size get() = cache.size

    fun put(key: K, item: ExpiringItem<T>) {
        cache.put(key, item)
    }

    fun putDatum(key: K, datum: T, timestamp: Long = System.currentTimeMillis()) {
        put(key, ExpiringItem(datum, timestamp))
    }

    fun get(key: K): ExpiringItem<T>? {
        return cache[key]
    }

    fun getDatum(key: K): T? {
        return cache[key]?.datum
    }

    fun getDatum(key: K, expires: Duration, now: Instant = Instant.now()): T? {
        return get(key)?.takeUnless { it.isExpired(expires, now) }?.datum
    }

    fun contains(key: K): Boolean {
        return cache[key] != null
    }

    /**
     * Computes the value if absent, without checking per-entry expiry.
     *
     * Prefer [computeDatumIfAbsent] when you also want to treat expired values as absent.
     */
    fun computeIfAbsent(key: K, mappingFunction: (K) -> T): T {
        return cache.computeIfAbsent(key) { ExpiringItem(mappingFunction(key)) }.datum
    }

    /**
     * Computes the value if absent or expired according to [expires].
     */
    fun computeDatumIfAbsent(
        key: K,
        expires: Duration,
        now: Instant = Instant.now(),
        mappingFunction: (K) -> T,
    ): T {
        val existing = get(key)
        if (existing != null && !existing.isExpired(expires, now)) {
            return existing.datum
        }

        // Underlying cache is synchronized, so computeIfAbsent is safe; we remove first to avoid
        // returning a stale/expired datum when another thread races.
        cache.remove(key)
        return cache.computeIfAbsent(key) { ExpiringItem(mappingFunction(key), now) }.datum
    }

    fun remove(): ExpiringItem<T>? = cache.remove()

    fun remove(key: K): ExpiringItem<T>? = cache.remove(key)

    fun removeAll(keys: Iterable<K>) = keys.forEach { cache.remove(it) }

    fun clear() = cache.clear()
}
