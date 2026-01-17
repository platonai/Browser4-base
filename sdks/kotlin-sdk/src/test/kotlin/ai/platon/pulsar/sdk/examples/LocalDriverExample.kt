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

import ai.platon.pulsar.sdk.AgenticSession

/**
 * Example demonstrating the automatic local driver mode.
 *
 * This example shows how the SDK automatically downloads and starts
 * Browser4.jar when no server URL is specified.
 *
 * To run this example:
 * 1. Set your OPENROUTER_API_KEY environment variable (optional)
 * 2. Run the main function
 * 3. The SDK will automatically download Browser4.jar to ~/.browser4/
 * 4. The local server will start on port 8182
 * 5. The session will connect to the local server
 */
fun main() {
    println("=== Browser4 SDK - Local Driver Mode Example ===\n")

    // Automatically downloads and starts Browser4.jar
    println("Creating session with local driver...")
    val session = AgenticSession.getOrCreate()
    
    println("Session created successfully!")
    println("Session ID: ${session.uuid}")
    println("Session is active: ${session.isActive}")
    
    // Example: Load a page
    println("\nAttempting to load example.com...")
    
    try {
        val page = session.open("https://example.com")
        println("Page loaded: ${page.url}")
        
        // Parse the page
        val document = session.parse(page)
        
        // Extract data using CSS selectors (only if document is not null)
        if (document != null) {
            val fields = session.extract(document, mapOf(
                "title" to "h1",
                "description" to "p"
            ))
            
            println("\nExtracted data:")
            fields.forEach { (key, value) ->
                println("  $key: $value")
            }
        }
        
    } catch (e: Exception) {
        println("Error: ${e.message}")
        println("Note: This example requires a working internet connection and API key.")
    }
    
    // Clean up (automatically stops the local driver)
    println("\nClosing session...")
    session.close()
    println("Session closed. Local driver stopped.")
    
    println("\n=== Example completed ===")
}
