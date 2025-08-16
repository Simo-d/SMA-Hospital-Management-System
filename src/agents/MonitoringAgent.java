package agents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import analytics.AnalyticsDashboard;
import ml.WaitTimePredictor;

import javax.swing.SwingUtilities;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.io.*;

/**
 * Enhanced Monitoring Agent - Collects real-time metrics from all agents
 * Master IA - Systèmes Multi-Agents Project
 */
public class MonitoringAgent extends Agent {
    
    private AnalyticsDashboard dashboard;
    private WaitTimePredictor predictor;
    
    // Metrics storage
    private Map<String, Double> systemMetrics;
    private Map<AID, AgentStatus> agentStatuses;
    
    // Counters
    private int totalPatients = 0;
    private int treatedPatients = 0;
    private int waitingPatients = 0;
    private double totalWaitTime = 0;
    private long lastUpdateTime;
    
    // Resource tracking
    private Map<String, ResourceStatus> doctorStatus;
    private Map<String, ResourceStatus> roomStatus;
    private Map<String, ResourceStatus> equipmentStatus;
    
    // Store reference to dashboard for updates
    private static AnalyticsDashboard dashboardReference;
    
    @Override
    protected void setup() {
        System.out.println("Monitoring Agent " + getLocalName() + " started");
        
        // Initialize data structures
        systemMetrics = new ConcurrentHashMap<>();
        agentStatuses = new ConcurrentHashMap<>();
        doctorStatus = new ConcurrentHashMap<>();
        roomStatus = new ConcurrentHashMap<>();
        equipmentStatus = new ConcurrentHashMap<>();
        predictor = new WaitTimePredictor();
        lastUpdateTime = System.currentTimeMillis();
        
        // Initialize metrics
        initializeMetrics();
        
        // Register in Yellow Pages
        registerInYellowPages();
        
        // Create and show dashboard
        SwingUtilities.invokeLater(() -> {
            dashboard = new AnalyticsDashboard();
            dashboard.setMainContainer(getContainerController());
            dashboard.setVisible(true);
            dashboardReference = dashboard; // Store reference
        });
        
        // Add behaviors
        addBehaviour(new MetricsCollectorBehaviour());
        addBehaviour(new ResourceMonitorBehaviour(this, 2000)); // Every 2 seconds
        addBehaviour(new DashboardUpdaterBehaviour(this, 1000)); // Every second
        addBehaviour(new StatisticsCalculatorBehaviour(this, 5000)); // Every 5 seconds
    }
    
    private void initializeMetrics() {
        systemMetrics.put("total_patients", 0.0);
        systemMetrics.put("treated_patients", 0.0);
        systemMetrics.put("waiting_patients", 0.0);
        systemMetrics.put("avg_wait_time", 0.0);
        systemMetrics.put("success_rate", 100.0);
        systemMetrics.put("doctor_utilization", 0.0);
        systemMetrics.put("room_utilization", 0.0);
        systemMetrics.put("equipment_utilization", 0.0);
        systemMetrics.put("throughput", 0.0);
        systemMetrics.put("queue_length", 0.0);
    }
    
    private void registerInYellowPages() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("monitoring-service");
        sd.setName("System-Monitor");
        dfd.addServices(sd);
        
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }
    
    /**
     * Collect metrics from all agents
     */
    private class MetricsCollectorBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            ACLMessage msg = myAgent.receive(mt);
            
            if (msg != null) {
                String content = msg.getContent();
                
                if (content != null) {
                    // Parse different types of metric updates
                    if (content.startsWith("PATIENT_REGISTERED:")) {
                        handlePatientRegistration(content);
                    } else if (content.startsWith("PATIENT_WAITING:")) {
                        handlePatientWaiting(content);
                    } else if (content.startsWith("PATIENT_TREATED:")) {
                        handlePatientTreated(content);
                    } else if (content.startsWith("RESOURCE_STATUS:")) {
                        handleResourceStatus(msg.getSender(), content);
                    } else if (content.startsWith("ALLOCATION_SUCCESS:")) {
                        handleAllocationSuccess(content);
                    } else if (content.startsWith("ALLOCATION_FAILED:")) {
                        handleAllocationFailed(content);
                    } else if (content.startsWith("WAIT_TIME:")) {
                        handleWaitTimeUpdate(content);
                    }
                }
            } else {
                block();
            }
        }
        
        private void handlePatientRegistration(String content) {
            totalPatients++;
            waitingPatients++;
            systemMetrics.put("total_patients", (double) totalPatients);
            systemMetrics.put("waiting_patients", (double) waitingPatients);
            updateQueueLength();
            
            // Update dashboard immediately
            if (dashboardReference != null) {
                SwingUtilities.invokeLater(() -> {
                    dashboardReference.updateMetric("total_patients", (double) totalPatients);
                    dashboardReference.updateMetric("waiting_patients", (double) waitingPatients);
                    dashboardReference.updateMetric("queue_length", (double) waitingPatients);
                });
            }
            
            System.out.println("Monitor: Patient registered. Total: " + totalPatients);
        }
        
        private void handlePatientWaiting(String content) {
            String[] parts = content.split(":");
            if (parts.length >= 2) {
                try {
                    double waitTime = Double.parseDouble(parts[1]);
                    updateWaitTime(waitTime);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }
        
        private void handlePatientTreated(String content) {
            treatedPatients++;
            if (waitingPatients > 0) waitingPatients--;
            
            systemMetrics.put("treated_patients", (double) treatedPatients);
            systemMetrics.put("waiting_patients", (double) waitingPatients);
            
            // Update success rate
            if (totalPatients > 0) {
                double successRate = (treatedPatients * 100.0) / totalPatients;
                systemMetrics.put("success_rate", successRate);
            }
            
            updateQueueLength();
            
            // Update dashboard immediately
            if (dashboardReference != null) {
                SwingUtilities.invokeLater(() -> {
                    dashboardReference.updateMetric("treated_patients", (double) treatedPatients);
                    dashboardReference.updateMetric("waiting_patients", (double) waitingPatients);
                    dashboardReference.updateMetric("success_rate", systemMetrics.get("success_rate"));
                    dashboardReference.updateMetric("queue_length", (double) waitingPatients);
                });
            }
            
            System.out.println("Monitor: Patient treated. Total treated: " + treatedPatients);
        }
        
        private void handleResourceStatus(AID sender, String content) {
            String[] parts = content.split(":");
            if (parts.length >= 3) {
                String resourceType = parts[1];
                String status = parts[2];
                
                ResourceStatus rs = new ResourceStatus(sender.getLocalName(), status.equals("BUSY"));
                
                if (resourceType.equals("DOCTOR")) {
                    doctorStatus.put(sender.getLocalName(), rs);
                } else if (resourceType.equals("ROOM")) {
                    roomStatus.put(sender.getLocalName(), rs);
                } else if (resourceType.equals("EQUIPMENT")) {
                    equipmentStatus.put(sender.getLocalName(), rs);
                }
                
                updateUtilization();
            }
        }
        
        private void handleAllocationSuccess(String content) {
            // Track successful allocations
            double currentSuccess = systemMetrics.getOrDefault("allocation_success", 0.0);
            systemMetrics.put("allocation_success", currentSuccess + 1);
        }
        
        private void handleAllocationFailed(String content) {
            // Track failed allocations
            double currentFailed = systemMetrics.getOrDefault("allocation_failed", 0.0);
            systemMetrics.put("allocation_failed", currentFailed + 1);
        }
        
        private void handleWaitTimeUpdate(String content) {
            String[] parts = content.split(":");
            if (parts.length >= 2) {
                try {
                    double waitTime = Double.parseDouble(parts[1]);
                    updateWaitTime(waitTime);
                    
                    // Update ML model
                    if (parts.length >= 4) {
                        int queueSize = Integer.parseInt(parts[2]);
                        double resourceAvail = Double.parseDouble(parts[3]);
                        // Note: We'd need patient info for full ML update
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }
        
        private void updateWaitTime(double waitTime) {
            totalWaitTime += waitTime;
            if (treatedPatients > 0) {
                double avgWaitTime = totalWaitTime / treatedPatients;
                systemMetrics.put("avg_wait_time", avgWaitTime / 60000.0); // Convert to minutes
                
                // Update dashboard immediately
                if (dashboardReference != null) {
                    SwingUtilities.invokeLater(() -> {
                        dashboardReference.updateMetric("avg_wait_time", avgWaitTime / 60000.0);
                    });
                }
            }
        }
        
        private void updateQueueLength() {
            systemMetrics.put("queue_length", (double) waitingPatients);
        }
        
        private void updateUtilization() {
            // Calculate doctor utilization
            if (!doctorStatus.isEmpty()) {
                long busyDoctors = doctorStatus.values().stream()
                    .filter(ResourceStatus::isBusy)
                    .count();
                double doctorUtil = (busyDoctors * 100.0) / doctorStatus.size();
                systemMetrics.put("doctor_utilization", doctorUtil);
            }
            
            // Calculate room utilization
            if (!roomStatus.isEmpty()) {
                long busyRooms = roomStatus.values().stream()
                    .filter(ResourceStatus::isBusy)
                    .count();
                double roomUtil = (busyRooms * 100.0) / roomStatus.size();
                systemMetrics.put("room_utilization", roomUtil);
            }
            
            // Calculate equipment utilization
            if (!equipmentStatus.isEmpty()) {
                long busyEquipment = equipmentStatus.values().stream()
                    .filter(ResourceStatus::isBusy)
                    .count();
                double equipUtil = (busyEquipment * 100.0) / equipmentStatus.size();
                systemMetrics.put("equipment_utilization", equipUtil);
            }
            
            // Update dashboard immediately
            if (dashboardReference != null) {
                SwingUtilities.invokeLater(() -> {
                    dashboardReference.updateMetric("doctor_utilization", 
                        systemMetrics.getOrDefault("doctor_utilization", 0.0));
                    dashboardReference.updateMetric("room_utilization", 
                        systemMetrics.getOrDefault("room_utilization", 0.0));
                    dashboardReference.updateMetric("equipment_utilization", 
                        systemMetrics.getOrDefault("equipment_utilization", 0.0));
                });
            }
        }
    }
    
    /**
     * Monitor resource agents
     */
    private class ResourceMonitorBehaviour extends TickerBehaviour {
        public ResourceMonitorBehaviour(Agent agent, long period) {
            super(agent, period);
        }
        
        @Override
        protected void onTick() {
            // Query all registered agents for their status
            queryAgentStatuses();
        }
        
        private void queryAgentStatuses() {
            // Query doctors
            queryResourceType("doctor-service", "DOCTOR");
            // Query rooms
            queryResourceType("room-service", "ROOM");
            // Query equipment
            queryResourceType("equipment-service", "EQUIPMENT");
            // Query patients
            queryResourceType("patient-service", "PATIENT");
        }
        
        private void queryResourceType(String serviceType, String resourceType) {
            try {
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType(serviceType);
                template.addServices(sd);
                
                DFAgentDescription[] results = DFService.search(myAgent, template);
                
                for (DFAgentDescription dfd : results) {
                    // Send status query
                    ACLMessage query = new ACLMessage(ACLMessage.REQUEST);
                    query.addReceiver(dfd.getName());
                    query.setContent("GET_STATUS");
                    query.setConversationId("status-query");
                    myAgent.send(query);
                }
            } catch (FIPAException fe) {
                fe.printStackTrace();
            }
        }
    }
    
    /**
     * Update dashboard with latest metrics
     */
    private class DashboardUpdaterBehaviour extends TickerBehaviour {
        public DashboardUpdaterBehaviour(Agent agent, long period) {
            super(agent, period);
        }
        
        @Override
        protected void onTick() {
            if (dashboardReference != null) {
                // Send all current metrics to dashboard
                SwingUtilities.invokeLater(() -> {
                    // Update all metrics
                    dashboardReference.updateMetric("total_patients", (double) totalPatients);
                    dashboardReference.updateMetric("treated_patients", (double) treatedPatients);
                    dashboardReference.updateMetric("waiting_patients", (double) waitingPatients);
                    dashboardReference.updateMetric("queue_length", (double) waitingPatients);
                    
                    for (Map.Entry<String, Double> entry : systemMetrics.entrySet()) {
                        dashboardReference.updateMetric(entry.getKey(), entry.getValue());
                    }
                    
                    // Update time series data
                    dashboardReference.updateTimeSeries("wait_time", 
                        systemMetrics.getOrDefault("avg_wait_time", 0.0));
                    dashboardReference.updateTimeSeries("throughput", 
                        calculateThroughput());
                    dashboardReference.updateTimeSeries("utilization", 
                        calculateAverageUtilization());
                    dashboardReference.updateTimeSeries("queue_length", 
                        (double) waitingPatients);
                });
            }
        }
        
        private double calculateThroughput() {
            long currentTime = System.currentTimeMillis();
            double elapsedHours = (currentTime - lastUpdateTime) / 3600000.0;
            if (elapsedHours > 0) {
                return treatedPatients / elapsedHours;
            }
            return 0;
        }
        
        private double calculateAverageUtilization() {
            double doctorUtil = systemMetrics.getOrDefault("doctor_utilization", 0.0);
            double roomUtil = systemMetrics.getOrDefault("room_utilization", 0.0);
            double equipUtil = systemMetrics.getOrDefault("equipment_utilization", 0.0);
            return (doctorUtil + roomUtil + equipUtil) / 3.0;
        }
    }
    
    /**
     * Calculate advanced statistics
     */
    private class StatisticsCalculatorBehaviour extends TickerBehaviour {
        public StatisticsCalculatorBehaviour(Agent agent, long period) {
            super(agent, period);
        }
        
        @Override
        protected void onTick() {
            // Calculate additional statistics
            calculateEfficiencyMetrics();
            
            // Print summary to console
            printMetricsSummary();
        }
        
        private void calculateEfficiencyMetrics() {
            // System efficiency score
            double efficiency = 0;
            
            // Factor 1: Low wait time (40% weight)
            double avgWait = systemMetrics.getOrDefault("avg_wait_time", 0.0);
            double waitScore = Math.max(0, 100 - avgWait * 5); // Penalty for wait time
            efficiency += waitScore * 0.4;
            
            // Factor 2: High utilization (30% weight)
            double avgUtil = calculateAverageUtilization();
            efficiency += avgUtil * 0.3;
            
            // Factor 3: High success rate (30% weight)
            double successRate = systemMetrics.getOrDefault("success_rate", 100.0);
            efficiency += successRate * 0.3;
            
            systemMetrics.put("efficiency_score", efficiency);
        }
        
        private double calculateAverageUtilization() {
            double doctorUtil = systemMetrics.getOrDefault("doctor_utilization", 0.0);
            double roomUtil = systemMetrics.getOrDefault("room_utilization", 0.0);
            double equipUtil = systemMetrics.getOrDefault("equipment_utilization", 0.0);
            return (doctorUtil + roomUtil + equipUtil) / 3.0;
        }
        
        private void printMetricsSummary() {
            System.out.println("\n=== System Metrics Summary ===");
            System.out.println("Total Patients: " + totalPatients);
            System.out.println("Treated: " + treatedPatients);
            System.out.println("Waiting: " + waitingPatients);
            System.out.println("Avg Wait Time: " + 
                String.format("%.2f", systemMetrics.getOrDefault("avg_wait_time", 0.0)) + " min");
            System.out.println("Success Rate: " + 
                String.format("%.1f%%", systemMetrics.getOrDefault("success_rate", 0.0)));
            System.out.println("Doctor Utilization: " + 
                String.format("%.1f%%", systemMetrics.getOrDefault("doctor_utilization", 0.0)));
            System.out.println("Room Utilization: " + 
                String.format("%.1f%%", systemMetrics.getOrDefault("room_utilization", 0.0)));
            System.out.println("Equipment Utilization: " + 
                String.format("%.1f%%", systemMetrics.getOrDefault("equipment_utilization", 0.0)));
            System.out.println("Efficiency Score: " + 
                String.format("%.1f", systemMetrics.getOrDefault("efficiency_score", 0.0)));
            System.out.println("==============================\n");
        }
    }
    
    /**
     * Helper class for resource status
     */
    private static class ResourceStatus {
        String name;
        boolean busy;
        long lastUpdate;
        
        ResourceStatus(String name, boolean busy) {
            this.name = name;
            this.busy = busy;
            this.lastUpdate = System.currentTimeMillis();
        }
        
        boolean isBusy() { return busy; }
    }
    
    /**
     * Helper class for agent status
     */
    private static class AgentStatus {
        AID agent;
        String type;
        String status;
        long lastHeartbeat;
        
        AgentStatus(AID agent, String type, String status) {
            this.agent = agent;
            this.type = type;
            this.status = status;
            this.lastHeartbeat = System.currentTimeMillis();
        }
    }
    
    // Public methods for dashboard to get metrics
    public Map<String, Double> getSystemMetrics() {
        return new HashMap<>(systemMetrics);
    }
    
    public void resetMetrics() {
        totalPatients = 0;
        treatedPatients = 0;
        waitingPatients = 0;
        totalWaitTime = 0;
        initializeMetrics();
        System.out.println("Metrics reset");
    }
    
    @Override
    protected void takeDown() {
        // Close dashboard
        if (dashboard != null) {
            dashboard.dispose();
        }
        
        // Deregister from Yellow Pages
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        
        System.out.println("Monitoring Agent " + getLocalName() + " terminating");
    }
}
