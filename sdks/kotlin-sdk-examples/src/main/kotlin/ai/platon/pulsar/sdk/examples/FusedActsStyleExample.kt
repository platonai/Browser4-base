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
package ai.platon.pulsar.sdk.examples

import ai.platon.pulsar.sdk.v0.AgenticContexts

/**
 * Example demonstrating FusedActs-style usage of the Kotlin SDK.
 *
 * This example shows how to use the SDK API in the same way as the
 * internal FusedActs example, making it consistent with internal code patterns.
 */
class FusedActsStyleExample {

    private var step = 0

    // Create session using the convenient factory method
    private val session = AgenticContexts.getOrCreateSession()

    suspend fun run() {
        // Use local mock site instead of external site so actions are deterministic.
        val url = "https://news.ycombinator.com/news"

        // Get the companion agent and driver (just like FusedActs)
        val agent = session.companionAgent
        val driver = session.getOrCreateBoundDriver()

        println("[STEP ${++step}] Open URL: $url")
        var page = session.open(url)
        println("Opened page: ${page.url}")

        println("[STEP ${++step}] Parse the page into a Jsoup document")
        var document = session.parse(page)
        println("Parsed document title: ${document?.title() ?: "N/A"}")

        println("[STEP ${++step}] Extract fields (title) with CSS selector")
        var fields = if (document != null) {
            session.extract(document, mapOf("title" to "#title"))
        } else {
            emptyMap()
        }
        println("Extracted fields: $fields")

        // Natural language actions
        println("[STEP ${++step}] Action: search for 'browser'")
        var result = agent.act("search for 'browser'")
        println("Action result: ${result.message}")

        println("[STEP ${++step}] Capture body text in the live DOM after search")
        var content = driver.selectFirstTextOrNull("body")
        println("Body snippet: ${content?.take(160)}")

        println("[STEP ${++step}] Action: click the 3rd link")
        result = agent.act("click the 3rd link")
        println("Action result: ${result.message}")

        println("[STEP ${++step}] Capture body text after clicking")
        content = driver.selectFirstTextOrNull("body")
        println("Body snippet: ${content?.take(160)}")

        println("[STEP ${++step}] Action: go back")
        result = agent.act("go back")
        println("Action result: ${result.message}")

        println("[STEP ${++step}] Action: open the 4th link in new tab")
        result = agent.act("open the 4th link in new tab")
        println("Action result: ${result.message}")

        println("[STEP ${++step}] Run autonomous task: find search box and submit")
        agent.clearHistory()
        var history = agent.run("find the search box, type 'web scraping' and submit the form")
        println("Task result: ${history.finalResult}")

        println("[STEP ${++step}] Capture and parse the live page")
        page = session.capture(driver)
        document = session.parse(page)
        fields = if (document != null) {
            session.extract(document, mapOf("title" to "#title"))
        } else {
            emptyMap()
        }
        println("Extracted after search: $fields")

        println("[STEP ${++step}] Action: scroll to bottom")
        agent.clearHistory()
        history = agent.run("scroll to the bottom of the page and wait for new content to load")
        println("Task result: ${history.finalResult}")

        println("[STEP ${++step}] Re-open the original URL")
        page = session.open(url)
        document = session.parse(page)
        fields = if (document != null) {
            session.extract(document, mapOf("title" to "#title"))
        } else {
            emptyMap()
        }
        println("Final fields: $fields")

        println("[STEP ${++step}] Print process trace")
        agent.processTrace.forEach { println("🚩 $it") }

        // Close using context.close() like FusedActs
        session.context.close()
    }
}

/**
 * Main function to run the example.
 * Note: Requires a running Browser4 server at http://localhost:8182
 */
suspend fun main() = FusedActsStyleExample().run()
