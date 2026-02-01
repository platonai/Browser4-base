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
package ai.platon.pulsar.sdk.integration

import ai.platon.pulsar.sdk.v0.AgenticContexts
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

/**
 * AgenticContexts integration tests.
 *
 * Tests factory methods for creating agentic sessions.
 */
@Tag("IntegrationTest")
class AgenticContextsTest : KotlinSdkIntegrationTestBase() {

    @Test
    @DisplayName("should create session using AgenticContexts factory")
    suspend fun testShouldCreateSessionUsingAgenticContextsFactory() {
        // Note: This test uses the server already started by the test base class
        val session = AgenticContexts.getOrCreateSession(baseUrl = baseUrl, useLocalDriver = false)

        assertNotNull(session, "Session should not be null")
        assertNotNull(session.uuid, "Session UUID should not be null")
        assertNotNull(session.client, "Session client should not be null")
    }
}
