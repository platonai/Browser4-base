#!/usr/bin/env node

/**
 * Installation verification script for @platonai/browser4-sdk
 * 
 * This script verifies that the package is correctly installed and all
 * main exports are accessible.
 */

console.log('🔍 Verifying @platonai/browser4-sdk installation...\n');

try {
  // Import the package
  const sdk = require('@platonai/browser4-sdk');
  
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
  const packageJson = require('@platonai/browser4-sdk/package.json');
  console.log(`   Name: ${packageJson.name}`);
  console.log(`   Version: ${packageJson.version}`);
  console.log(`   Description: ${packageJson.description}`);
  console.log(`   License: ${packageJson.license}`);
  
  // Check TypeScript declarations
  const fs = require('fs');
  const path = require('path');
  const packageDir = path.dirname(require.resolve('@platonai/browser4-sdk/package.json'));
  const declarationFile = path.join(packageDir, 'dist', 'index.d.ts');
  
  if (fs.existsSync(declarationFile)) {
    console.log('\n✅ TypeScript declarations found');
  } else {
    console.log('\n⚠️  TypeScript declarations not found');
  }
  
  // Summary
  console.log('\n' + '='.repeat(50));
  if (missingExports.length === 0) {
    console.log('✅ Installation verified successfully!');
    console.log(`   All ${requiredExports.length} main exports are available.`);
    process.exit(0);
  } else {
    console.log('⚠️  Installation incomplete!');
    console.log(`   ${missingExports.length} exports are missing: ${missingExports.join(', ')}`);
    process.exit(1);
  }
  
} catch (error) {
  console.error('\n❌ Installation verification failed!');
  console.error('   Error:', error.message);
  console.error('\n💡 Suggestions:');
  console.error('   1. Make sure the package is installed: npm install @platonai/browser4-sdk');
  console.error('   2. Check if you\'re in the correct directory');
  console.error('   3. Try clearing npm cache: npm cache clean --force');
  console.error('   4. Reinstall: npm install @platonai/browser4-sdk --force');
  process.exit(1);
}
