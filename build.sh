#!/bin/bash

echo "================================================"
echo "  Building Enhanced Hospital System v2.0"
echo "================================================"

# Create bin directory if it doesn't exist
mkdir -p bin

# Clean previous build
echo "Cleaning previous build..."
rm -rf bin/*

# Compile all Java source files including new packages
echo "Compiling Java sources..."
javac -cp lib/jade.jar -d bin \
    src/models/*.java \
    src/utils/*.java \
    src/ml/*.java \
    src/negotiation/*.java \
    src/fault/*.java \
    src/loadbalancing/*.java \
    src/agents/*.java \
    src/gui/*.java \
    src/analytics/*.java \
    src/test/*.java \
    src/*.java 2>&1

if [ $? -eq 0 ]; then
    echo "✅ Build successful!"
    echo ""
    echo "To run the system, use: ./run.sh"
else
    echo "❌ Build failed!"
    echo "Check the error messages above for details."
    exit 1
fi
