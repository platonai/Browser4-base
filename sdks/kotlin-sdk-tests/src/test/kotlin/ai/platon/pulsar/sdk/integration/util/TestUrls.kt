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

import java.net.URL

/**
 * Test URL constants for integration tests.
 */
object TestUrls {
    /**
     * Mock server base URL
     */
    const val MOCK_SERVER_BASE = "http://localhost:18080"

    /**
     * Simple static page (for basic navigation tests)
     */
    const val SIMPLE_PAGE = "$MOCK_SERVER_BASE/ec/"

    /**
     * Product list page
     */
    const val PRODUCT_LIST = "$MOCK_SERVER_BASE/ec/b?node=1292115012"

    /**
     * Product detail page
     */
    const val PRODUCT_DETAIL = "$MOCK_SERVER_BASE/ec/dp/B0E000001"

    /**
     * Verify if mock server is running
     */
    fun isMockServerRunning(): Boolean {
        return try {
            URL(MOCK_SERVER_BASE).openConnection().apply {
                connectTimeout = 5000
                readTimeout = 5000
            }.getInputStream().close()
            true
        } catch (e: Exception) {
            false
        }
    }
}
