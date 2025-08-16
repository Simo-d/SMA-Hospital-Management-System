#!/bin/bash

echo "================================================"
echo "  Starting Enhanced Hospital System v2.0"
echo "================================================"
echo ""

# Check if bin directory exists
if [ ! -d "bin" ]; then
    echo "❌ Error: bin directory not found. Please run ./build.sh first"
    exit 1
fi

# Check if the main class exists
if [ ! -f "bin/HospitalMain.class" ]; then
    echo "❌ Error: HospitalMain.class not found. Please run ./build.sh first"
    exit 1
fi

# Run the enhanced system
echo "🚀 Launching Hospital Resource Allocation System..."
echo "📊 Analytics Dashboard will open automatically"
echo ""

# Run with sufficient memory for analytics
java -Xmx1024m -cp "bin:lib/jade.jar" HospitalMain

# Check exit status
if [ $? -ne 0 ]; then
    echo ""
    echo "❌ System exited with errors"
    exit 1
fi
