package ai.platon.pulsar.common.concurrent

import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ConcurrentExpiringLRUCacheTest {

    @Test
    fun `getDatum should return null at expiration boundary`() {
        val cache = ConcurrentExpiringLRUCache<String, String>(ttl = Duration.ZERO, capacity = 10)

        val base = Instant.parse("2026-01-01T00:00:00Z")
        cache.put("k", ExpiringItem("v", base))

        val expires = Duration.ofSeconds(1)
        // exactly at boundary => expired
        assertNull(cache.getDatum("k", expires, base.plusSeconds(1)))
        // just before boundary => not expired
        assertEquals("v", cache.getDatum("k", expires, base.plusMillis(999)))
    }

    @Test
    fun `computeDatumIfAbsent should recompute when expired`() {
        val cache = ConcurrentExpiringLRUCache<String, Int>(ttl = Duration.ZERO, capacity = 10)

        val base = Instant.parse("2026-01-01T00:00:00Z")
        cache.put("k", ExpiringItem(1, base))

        val expires = Duration.ofSeconds(1)
        val v = cache.computeDatumIfAbsent("k", expires, base.plusSeconds(1)) { 2 }
        assertEquals(2, v)

        // once refreshed, it should be present and fresh for the new timestamp (base+1s)
        val stored = cache.get("k")
        assertNotNull(stored)
        assertEquals(2, stored.datum)
        assertEquals(base.plusSeconds(1).toEpochMilli(), stored.timestamp)
    }
}
