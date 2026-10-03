#!/bin/bash

echo "Building Biblioteki frontend..."

# Check if npm is available
if ! command -v npm &> /dev/null; then
    echo "Error: npm is not installed. Please install Node.js and npm."
    exit 1
fi

# Install dependencies
npm install

# Run Vite build
npm run build

echo "Build complete. Files are in: $(pwd)/dist/"
echo "Copy the contents of dist/ to the static directory if needed."