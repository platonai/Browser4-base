/**
 * Web scraping example demonstrating data extraction.
 * 
 * This example shows how to:
 * - Load pages with options
 * - Parse HTML content
 * - Extract data using CSS selectors
 * - Scrape multiple fields in one operation
 */

import { Browser4Driver, PulsarClient, PulsarSession } from '../src';

async function main() {
  console.log('Starting Browser4 server...');
  
  const driver = new Browser4Driver();
  
  await driver.use(async (d) => {
    console.log(`Server running at: ${d.baseUrl}`);
    
    // Create client and session
    const client = new PulsarClient({ baseUrl: d.baseUrl });
    await client.createSession();
    const session = new PulsarSession(client);
    
    // Load a page with caching options
    console.log('\n=== Loading page with cache ===');
    const page = await session.load('https://example.com', '-expire 1d');
    console.log(`Loaded: ${page.url}`);
    console.log(`Protocol status: ${page.protocolStatus}`);
    
    // Parse the page
    console.log('\n=== Parsing page ===');
    const document = await session.parse(page);
    console.log('Page parsed successfully');
    
    // Extract fields using CSS selectors
    console.log('\n=== Extracting fields ===');
    const fields = await session.extract(document, {
      title: 'h1',
      description: 'p',
      links: 'a'
    });
    
    console.log('Extracted fields:');
    console.log(JSON.stringify(fields.fields, null, 2));
    
    // Scrape in one operation (load + parse + extract)
    console.log('\n=== Scraping in one call ===');
    const scrapedData = await session.scrape(
      'https://example.com',
      {
        heading: 'h1',
        paragraph: 'p:first-of-type',
        linkCount: 'a'
      },
      '-expire 1h'
    );
    
    console.log('Scraped data:');
    console.log(JSON.stringify(scrapedData.fields, null, 2));
    
    // Normalize URL with args
    console.log('\n=== Normalizing URL ===');
    const normUrl = await session.normalize(
      'https://example.com/path?query=test',
      '-expire 12h -ignoreFailure'
    );
    console.log(`Spec: ${normUrl.spec}`);
    console.log(`URL: ${normUrl.url}`);
    console.log(`Args: ${normUrl.args}`);
    
    // Submit URL to crawl pool for async processing
    console.log('\n=== Submitting to crawl pool ===');
    const submitted = await session.submit(
      'https://example.com/page2',
      '-expire 1d'
    );
    console.log(`Submitted: ${submitted}`);
    
    // Clean up
    await session.close();
    client.close();
    console.log('\nSession closed');
  });
  
  console.log('Server stopped');
}

// Run the example
main().catch(error => {
  console.error('Error:', error);
  process.exit(1);
});
