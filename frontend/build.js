/**
 * Build script for frontend
 * Run with: node build.js
 * 
 * This script:
 * 1. Builds TypeScript files using Vite
 * 2. Copies only the JS bundle to src/main/resources/static/js/
 * 3. Does NOT delete any existing files
 */

import { execSync } from 'child_process';
import { copyFileSync, mkdirSync, existsSync } from 'fs';
import { resolve, dirname } from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

console.log('Building Biblioteki frontend...');

try {
    // Step 1: Build with Vite
    console.log('Running Vite build...');
    execSync('npx vite build', { 
        cwd: __dirname,
        stdio: 'inherit'
    });

    // Step 2: Copy built files to Spring Boot static directory
    const staticDir = resolve(__dirname, '../src/main/resources/static');
    const distDir = resolve(__dirname, 'dist');
    const staticSourceDir = resolve(__dirname, 'static');

    // Create static/js directory if it doesn't exist
    if (!existsSync(staticDir)) {
        mkdirSync(staticDir, { recursive: true });
    }

    // Copy all JS files from dist to static/js
    const { readdirSync } = await import('fs');

    readdirSync(distDir).forEach(file => {
        const srcPath = resolve(distDir, file);
        const destPath = resolve(staticDir, file);
        copyFileSync(srcPath, destPath);
        console.log(`Copied: ${file} -> ${destPath}`);
    });

    readdirSync(staticSourceDir).forEach(file => {
        const srcPath = resolve(staticSourceDir, file);
        const destPath = resolve(staticDir, file);
        copyFileSync(srcPath, destPath);
        console.log(`Copied: ${file} -> ${destPath}`);
    });

    console.log('Build complete!');
    console.log('Files copied to: ' + staticDir);

} catch (error) {
    console.error('Build failed:', error);
    process.exit(1);
}