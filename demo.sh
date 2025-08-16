#!/bin/bash

# Hospital Resource Allocation System - Demo Script
# Master IA - Systèmes Multi-Agents

echo "╔════════════════════════════════════════════════════════════╗"
echo "║     Hospital Resource Allocation System v2.0 Demo          ║"
echo "║          Master IA - Systèmes Multi-Agents                 ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

# Function to pause and wait for user
pause() {
    echo ""
    echo "Press Enter to continue..."
    read
}

# Function to print section header
section() {
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "  $1"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
}

# Check prerequisites
section "📋 CHECKING PREREQUISITES"
echo "Checking Java installation..."
java -version
echo ""
echo "Checking JADE library..."
if [ -f "lib/jade.jar" ]; then
    echo "✅ JADE library found"
else
    echo "❌ JADE library not found! Please add jade.jar to lib/ directory"
    exit 1
fi

pause

# Build the system
section "🔨 BUILDING THE SYSTEM"
echo "Compiling all components..."
./build.sh
if [ $? -ne 0 ]; then
    echo "❌ Build failed! Please check for errors."
    exit 1
fi
echo "✅ Build successful!"

pause

# Explain the architecture
section "🏗️ SYSTEM ARCHITECTURE"
cat << EOF
The system consists of multiple layers:

1. INFRASTRUCTURE LAYER
   - Scheduler Agent: Coordinates resource allocation
   - Load Balancer: Distributes workload (Adaptive strategy)
   - Fault Manager: Monitors health & recovers failed agents
   - Monitoring Agent: Collects system metrics

2. RESOURCE LAYER
   - 5 Doctor Agents (Different specializations)
   - 6 Room Agents (Various types)
   - 6 Equipment Agents (Medical devices)

3. PATIENT LAYER
   - Dynamic patient agents with urgency levels

4. ANALYTICS LAYER
   - Real-time dashboard with charts
   - ML predictions
   - Performance metrics
EOF

pause

# Start the system
section "🚀 STARTING THE SYSTEM"
echo "Launching Hospital Resource Allocation System..."
echo "The following windows will open:"
echo "  1. JADE Agent GUI"
echo "  2. Analytics Dashboard"
echo ""
echo "Starting in 3 seconds..."
sleep 3

# Run the system in background
java -cp bin:lib/jade.jar HospitalMain &
SYSTEM_PID=$!

echo "✅ System started with PID: $SYSTEM_PID"
echo ""
echo "Please arrange the windows to see both JADE GUI and Analytics Dashboard"

pause

# Demo scenarios
section "📊 DEMO SCENARIO 1: NORMAL OPERATIONS"
cat << EOF
In the Analytics Dashboard:
1. Click "Add Random Patient" button 5 times
2. Observe in Overview tab:
   - Total Patients counter increasing
   - Real-time activity chart updating
3. Check JADE GUI:
   - New PatientAgent instances
   - Message exchanges between agents
EOF

pause

section "🔬 DEMO SCENARIO 2: MACHINE LEARNING"
cat << EOF
In the Analytics Dashboard:
1. Navigate to "Predictions" tab
2. Observe:
   - Wait time predictions graph
   - ML model accuracy metrics (MAE, RMSE, R²)
   - System recommendations
3. The model learns from each patient interaction
EOF

pause

section "🤝 DEMO SCENARIO 3: NEGOTIATION PROTOCOL"
cat << EOF
In the Analytics Dashboard:
1. Click "Add Emergency" button
2. Check console output for:
   - "Sent CFP for..." (Call for Proposals)
   - "Received bid from..." (Agent bidding)
   - "Winner is..." (Negotiation result)
3. Contract Net Protocol in action!
EOF

pause

section "🛡️ DEMO SCENARIO 4: FAULT TOLERANCE"
cat << EOF
In JADE GUI:
1. Right-click on any Doctor agent
2. Select "Kill" to simulate failure
3. Watch console for:
   - "FAILURE DETECTED: ..."
   - "Attempting to recover..."
   - "Successfully recovered..."
4. New recovered agent appears in JADE GUI
EOF

pause

section "⚖️ DEMO SCENARIO 5: LOAD BALANCING"
cat << EOF
In the Analytics Dashboard:
1. Click "Add 5 Patients" button 3 times (15 patients)
2. Navigate to "Resources" tab
3. Observe utilization gauges:
   - Balanced distribution across resources
   - No single resource overloaded
4. Check console for load rebalancing messages
EOF

pause

section "📈 DEMO SCENARIO 6: ANALYTICS & REPORTING"
cat << EOF
In the Analytics Dashboard:
1. Tour all tabs:
   - Overview: Key metrics cards
   - Performance: Charts and graphs
   - Resources: Utilization gauges
   - Predictions: ML insights
2. Click "Export Report" button
3. Check generated report file
EOF

pause

section "🎯 KEY ACHIEVEMENTS"
cat << EOF
✅ Multi-agent architecture with 20+ agents
✅ FIPA ACL communication protocols
✅ Machine Learning with online training
✅ Contract Net negotiation protocol
✅ Automatic fault recovery
✅ Dynamic load balancing (4 strategies)
✅ Real-time analytics dashboard
✅ 8+ visualization types
✅ Performance metrics tracking
✅ Export functionality
EOF

pause

section "🏁 DEMO COMPLETE"
echo "The system demonstrates:"
echo "  • Distributed AI coordination"
echo "  • Self-healing capabilities"
echo "  • Intelligent resource optimization"
echo "  • Predictive analytics"
echo "  • Real-time monitoring"
echo ""
echo "Thank you for watching!"
echo ""
echo "To stop the system, press Ctrl+C or close the windows"
echo ""

# Keep script running
wait $SYSTEM_PID
