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
package ai.platon.pulsar.sdk.integration.util

import java.time.Duration
import java.util.concurrent.TimeoutException

/**
 * Test helper utilities.
 */
object TestHelpers {
    
    /**
     * Retry execution until success or timeout
     */
    fun <T> retry(
        maxAttempts: Int = 3,
        delay: Duration = Duration.ofSeconds(1),
        block: () -> T
    ): T {
        var lastException: Exception? = null
        
        repeat(maxAttempts) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxAttempts - 1) {
                    Thread.sleep(delay.toMillis())
                }
            }
        }
        
        throw lastException ?: IllegalStateException("Retry failed")
    }

    /**
     * Wait for condition to be met
     */
    fun waitFor(
        timeout: Duration = Duration.ofSeconds(10),
        pollInterval: Duration = Duration.ofMillis(500),
        condition: () -> Boolean
    ) {
        val endTime = System.currentTimeMillis() + timeout.toMillis()
        
        while (System.currentTimeMillis() < endTime) {
            if (condition()) {
                return
            }
            Thread.sleep(pollInterval.toMillis())
        }
        
        throw TimeoutException("Condition not met within ${timeout.seconds} seconds")
    }

    /**
     * Generate unique test session ID
     */
    fun generateTestSessionId(): String {
        return "test-session-${System.currentTimeMillis()}-${(0..9999).random()}"
    }
}
