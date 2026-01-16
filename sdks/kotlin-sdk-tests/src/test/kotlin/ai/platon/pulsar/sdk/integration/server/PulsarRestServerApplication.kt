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
package ai.platon.pulsar.sdk.integration.server

import ai.platon.pulsar.boot.autoconfigure.test.PulsarTestContextInitializer
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.ImportResource
import org.springframework.test.context.ContextConfiguration

/**
 * Test server application for Kotlin SDK integration tests.
 *
 * Starts a complete Browser4 REST API server for SDK testing.
 */
@SpringBootApplication
@ContextConfiguration(initializers = [PulsarTestContextInitializer::class])
@ComponentScan(
    basePackages = [
        "ai.platon.pulsar.boot.autoconfigure",
        "ai.platon.pulsar.rest.api",
        "ai.platon.pulsar.rest.openapi",  // OpenAPI controllers including SessionController
        "ai.platon.pulsar.test.server"  // Mock site server
    ]
)
@ImportResource("rest-beans/app-context.xml")
class PulsarRestServerApplication
