/**
 * Basic example demonstrating Browser4 NodeJS SDK usage.
 * 
 * This example shows how to:
 * - Start Browser4 server automatically
 * - Create a session
 * - Navigate and interact with pages
 * - Use AI-powered actions
 */

import { Browser4Driver, PulsarClient, AgenticSession } from '../src';

async function main() {
  console.log('Starting Browser4 server...');
  
  // Browser4Driver automatically downloads and starts the server
  const driver = new Browser4Driver();
  
  await driver.use(async (d) => {
    console.log(`Server running at: ${d.baseUrl}`);
    
    // Create client and session
    const client = new PulsarClient({ baseUrl: d.baseUrl });
    const sessionId = await client.createSession();
    console.log(`Session created: ${sessionId}`);
    
    const session = new AgenticSession(client);
    
    // Navigate to a page
    console.log('\n=== Opening page ===');
    const page = await session.open('https://example.com');
    console.log(`Opened: ${page.url}`);
    console.log(`Content type: ${page.contentType}`);
    console.log(`Content length: ${page.contentLength} bytes`);
    
    // Use WebDriver for element interaction
    console.log('\n=== Using WebDriver ===');
    const webDriver = session.driver;
    
    // Get current URL
    const currentUrl = await webDriver.currentUrl();
    console.log(`Current URL: ${currentUrl}`);
    
    // Get page title
    const title = await webDriver.title();
    console.log(`Page title: ${title}`);
    
    // Check if an element exists
    const h1Exists = await webDriver.exists('h1');
    console.log(`H1 element exists: ${h1Exists}`);
    
    // Get text from an element
    if (h1Exists) {
      const h1Text = await webDriver.getText('h1');
      console.log(`H1 text: ${h1Text}`);
    }
    
    // Use AI-powered actions
    console.log('\n=== Using AI-powered actions ===');
    
    // Observe the page
    const observations = await session.observe('What can I interact with?');
    console.log(`Found ${observations.observations.length} observations`);
    observations.observations.slice(0, 3).forEach((obs, i) => {
      console.log(`  ${i + 1}. ${obs.method}: ${obs.description}`);
    });
    
    // Summarize the page
    const summary = await session.summarize('Summarize this page in one sentence');
    console.log(`Summary: ${summary}`);
    
    // Execute a single action
    const actResult = await session.act('scroll down 200 pixels');
    console.log(`Action result: ${actResult.success ? 'Success' : 'Failed'}`);
    console.log(`Message: ${actResult.message}`);
    
    // Check state history
    const history = session.stateHistory;
    console.log(`\nExecuted ${history.states.length} actions`);
    history.states.forEach((state, i) => {
      console.log(`  ${i + 1}. ${state.action}: ${state.success ? 'Success' : 'Failed'}`);
    });
    
    // Clean up
    console.log('\n=== Cleaning up ===');
    await session.close();
    client.close();
    console.log('Session closed');
  });
  
  console.log('Server stopped');
}

// Run the example
main().catch(error => {
  console.error('Error:', error);
  process.exit(1);
});
