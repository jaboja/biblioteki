#!/bin/bash

# This script copies the built frontend files to Spring Boot's static directory
# Usage: ./copy-to-static.sh

FRONTEND_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
STATIC_DIR="$FRONTEND_DIR/../src/main/resources/static"

echo "Copying frontend files to: $STATIC_DIR"

# Create static directory if it doesn't exist
mkdir -p "$STATIC_DIR"

# Copy all files from frontend to static
cp -r "$FRONTEND_DIR/index.html" "$STATIC_DIR/"
cp -r "$FRONTEND_DIR/css" "$STATIC_DIR/"
cp -r "$FRONTEND_DIR/dist/*" "$STATIC_DIR/" 2>/dev/null || echo "No dist directory yet"

echo "Files copied successfully!"
echo "You can now run: mvn spring-boot:run"