#!/bin/bash

# Build the debug image
echo "Building Linux check image..."
docker build -t quickoutline-linux-check -f Dockerfile.linux-check .

# Create dummy libs directory to satisfy Tauri config
mkdir -p build/libs

# Run cargo check
echo "Running cargo check in Linux container..."
docker run --rm -v "$(pwd)":/app quickoutline-linux-check
