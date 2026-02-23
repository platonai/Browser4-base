#!/usr/bin/env node

/**
 * Local installation verification script for development
 * Tests the SDK from the dist directory without requiring npm install
 */

console.log('🔍 Verifying local build of @platonai/browser4-sdk...\n');

try {
  // Import from local dist directory
  const sdk = require('../dist/index.js');
  
  // Check main exports
  const requiredExports = [
    'PulsarClient',
    'Browser4Driver',
    'PulsarSession',
    'AgenticSession',
    'WebDriver'
  ];
  
  const missingExports = [];
  const availableExports = [];
  
  for (const exportName of requiredExports) {
    if (sdk[exportName]) {
      availableExports.push(exportName);
      console.log(`✅ ${exportName} - Available`);
    } else {
      missingExports.push(exportName);
      console.log(`❌ ${exportName} - Missing`);
    }
  }
  
  console.log('\n📦 Package Information:');
  
  // Get package.json
  const packageJson = require('../package.json');
  console.log(`   Name: ${packageJson.name}`);
  console.log(`   Version: ${packageJson.version}`);
  console.log(`   Description: ${packageJson.description}`);
  console.log(`   License: ${packageJson.license}`);
  
  // Check TypeScript declarations
  const fs = require('fs');
  const path = require('path');
  const declarationFile = path.join(__dirname, '..', 'dist', 'index.d.ts');
  
  if (fs.existsSync(declarationFile)) {
    console.log('\n✅ TypeScript declarations found');
    
    // Count declaration files
    const distDir = path.join(__dirname, '..', 'dist');
    const dtsFiles = fs.readdirSync(distDir).filter(f => f.endsWith('.d.ts'));
    console.log(`   ${dtsFiles.length} .d.ts files generated`);
  } else {
    console.log('\n⚠️  TypeScript declarations not found');
  }
  
  // Check build output
  const distDir = path.join(__dirname, '..', 'dist');
  if (fs.existsSync(distDir)) {
    const jsFiles = fs.readdirSync(distDir).filter(f => f.endsWith('.js'));
    console.log(`   ${jsFiles.length} .js files generated`);
  }
  
  // Summary
  console.log('\n' + '='.repeat(50));
  if (missingExports.length === 0) {
    console.log('✅ Build verified successfully!');
    console.log(`   All ${requiredExports.length} main exports are available.`);
    console.log('\n💡 To test installation:');
    console.log('   1. Run: npm pack');
    console.log('   2. In another directory: npm install /path/to/platonai-browser4-sdk-0.1.0.tgz');
    console.log('   3. Run: npm run verify (from installed location)');
    process.exit(0);
  } else {
    console.log('⚠️  Build incomplete!');
    console.log(`   ${missingExports.length} exports are missing: ${missingExports.join(', ')}`);
    process.exit(1);
  }
  
} catch (error) {
  console.error('\n❌ Build verification failed!');
  console.error('   Error:', error.message);
  console.error('\n💡 Suggestions:');
  console.error('   1. Run: npm run build');
  console.error('   2. Check for TypeScript errors');
  console.error('   3. Ensure all source files are present');
  process.exit(1);
}
