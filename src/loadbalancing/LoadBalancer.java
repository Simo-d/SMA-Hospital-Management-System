package loadbalancing;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import models.Patient;

/**
 * Load Balancer - Distributes workload dynamically across resources
 * Implements multiple load balancing algorithms
 * Master IA - Systèmes Multi-Agents Project
 */
public class LoadBalancer extends Agent {
    
    private Map<AID, ResourceLoad> resourceLoads;
    private LoadBalancingStrategy strategy;
    private Queue<Patient> pendingPatients;
    private Map<String, List<AID>> resourcesByType;
    
    // Metrics
    private Map<AID, ResourceMetrics> resourceMetrics;
    private long totalRequests = 0;
    private long successfulAllocations = 0;
    
    @Override
    protected void setup() {
        System.out.println("Load Balancer started");
        
        resourceLoads = new ConcurrentHashMap<>();
        resourceMetrics = new ConcurrentHashMap<>();
        pendingPatients = new LinkedList<>();
        resourcesByType = new HashMap<>();
        
        // Get load balancing strategy from arguments
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            String strategyName = (String) args[0];
            strategy = createStrategy(strategyName);
        } else {
            strategy = new RoundRobinStrategy(); // Default
        }
        
        // Register service
        registerService();
        
        // Add behaviors
        addBehaviour(new ResourceDiscovery(this, 10000));
        addBehaviour(new LoadMonitor());
        addBehaviour(new RequestDistributor());
        addBehaviour(new LoadRebalancer(this, 30000));
        addBehaviour(new MetricsCollector(this, 60000));
    }
    
    private void registerService() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("load-balancer-service");
        sd.setName("LoadBalancer");
        dfd.addServices(sd);
        
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }
    
    private LoadBalancingStrategy createStrategy(String name) {
        switch (name.toLowerCase()) {
            case "roundrobin":
                return new RoundRobinStrategy();
            case "leastconnections":
                return new LeastConnectionsStrategy();
            case "weighted":
                return new WeightedRoundRobinStrategy();
            case "adaptive":
                return new AdaptiveLoadBalancingStrategy();
            default:
                return new RoundRobinStrategy();
        }
    }
    
    /**
     * Discover available resources
     */
    private class ResourceDiscovery extends TickerBehaviour {
        public ResourceDiscovery(Agent agent, long period) {
            super(agent, period);
        }
        
        @Override
        protected void onTick() {
            resourcesByType.clear();
            
            // Discover doctors
            discoverResourceType("doctor-service", "DOCTOR");
            // Discover rooms
            discoverResourceType("room-service", "ROOM");
            // Discover equipment
            discoverResourceType("equipment-service", "EQUIPMENT");
        }
        
        private void discoverResourceType(String serviceType, String resourceType) {
            try {
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType(serviceType);
                template.addServices(sd);
                
                DFAgentDescription[] results = DFService.search(myAgent, template);
                List<AID> resources = new ArrayList<>();
                
                for (DFAgentDescription dfd : results) {
                    AID agent = dfd.getName();
                    resources.add(agent);
                    
                    // Initialize load tracking if new
                    if (!resourceLoads.containsKey(agent)) {
                        resourceLoads.put(agent, new ResourceLoad(agent, resourceType));
                        resourceMetrics.put(agent, new ResourceMetrics(agent));
                    }
                }
                
                resourcesByType.put(resourceType, resources);
                System.out.println("Discovered " + resources.size() + " " + resourceType + " resources");
                
            } catch (FIPAException fe) {
                fe.printStackTrace();
            }
        }
    }
    
    /**
     * Monitor resource loads
     */
    private class LoadMonitor extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = myAgent.receive();
            if (msg != null) {
                if (msg.getContent() != null) {
                    if (msg.getContent().startsWith("LOAD_UPDATE:")) {
                        handleLoadUpdate(msg);
                    } else if (msg.getContent().startsWith("RESOURCE_FREE:")) {
                        handleResourceFree(msg);
                    }
                }
            } else {
                block();
            }
        }
        
        private void handleLoadUpdate(ACLMessage msg) {
            String[] parts = msg.getContent().split(":");
            if (parts.length >= 2) {
                try {
                    int load = Integer.parseInt(parts[1]);
                    ResourceLoad resourceLoad = resourceLoads.get(msg.getSender());
                    if (resourceLoad != null) {
                        resourceLoad.setCurrentLoad(load);
                        resourceLoad.updateAverageLoad(load);
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }
        
        private void handleResourceFree(ACLMessage msg) {
            ResourceLoad resourceLoad = resourceLoads.get(msg.getSender());
            if (resourceLoad != null) {
                resourceLoad.decrementLoad();
            }
        }
    }
    
    /**
     * Distribute requests based on load balancing strategy
     */
    private class RequestDistributor extends CyclicBehaviour {
        @Override
        public void action() {
            if (!pendingPatients.isEmpty()) {
                Patient patient = pendingPatients.poll();
                AID selectedResource = selectResource(patient);
                
                if (selectedResource != null) {
                    allocateToResource(patient, selectedResource);
                    successfulAllocations++;
                } else {
                    // No suitable resource, re-queue
                    pendingPatients.offer(patient);
                }
            } else {
                block(1000); // Wait 1 second
            }
        }
        
        private AID selectResource(Patient patient) {
            String resourceType = determineResourceType(patient);
            List<AID> availableResources = resourcesByType.get(resourceType);
            
            if (availableResources == null || availableResources.isEmpty()) {
                return null;
            }
            
            // Filter overloaded resources
            List<AID> viableResources = new ArrayList<>();
            for (AID resource : availableResources) {
                ResourceLoad load = resourceLoads.get(resource);
                if (load != null && !load.isOverloaded()) {
                    viableResources.add(resource);
                }
            }
            
            if (viableResources.isEmpty()) {
                return null;
            }
            
            // Apply load balancing strategy
            return strategy.selectResource(viableResources, resourceLoads, patient);
        }
        
        private String determineResourceType(Patient patient) {
            // Simple logic - in real system would be more complex
            return "DOCTOR";
        }
        
        private void allocateToResource(Patient patient, AID resource) {
            ACLMessage allocation = new ACLMessage(ACLMessage.REQUEST);
            allocation.addReceiver(resource);
            allocation.setContent("ALLOCATE_PATIENT:" + patient.getId());
            myAgent.send(allocation);
            
            // Update load
            ResourceLoad load = resourceLoads.get(resource);
            if (load != null) {
                load.incrementLoad();
                load.addPatient(patient.getId());
            }
            
            // Update metrics
            ResourceMetrics metrics = resourceMetrics.get(resource);
            if (metrics != null) {
                metrics.incrementAllocations();
            }
            
            totalRequests++;
            
            System.out.println("Allocated patient " + patient.getName() + " to " + 
                             resource.getLocalName() + " (Load: " + load.getCurrentLoad() + ")");
        }
    }
    
    /**
     * Rebalance loads periodically
     */
    private class LoadRebalancer extends TickerBehaviour {
        public LoadRebalancer(Agent agent, long period) {
            super(agent, period);
        }
        
        @Override
        protected void onTick() {
            performLoadRebalancing();
        }
        
        private void performLoadRebalancing() {
            // Find overloaded and underloaded resources
            List<AID> overloaded = new ArrayList<>();
            List<AID> underloaded = new ArrayList<>();
            
            double averageLoad = calculateAverageLoad();
            
            for (Map.Entry<AID, ResourceLoad> entry : resourceLoads.entrySet()) {
                ResourceLoad load = entry.getValue();
                if (load.getCurrentLoad() > averageLoad * 1.5) {
                    overloaded.add(entry.getKey());
                } else if (load.getCurrentLoad() < averageLoad * 0.5) {
                    underloaded.add(entry.getKey());
                }
            }
            
            // Attempt to rebalance
            if (!overloaded.isEmpty() && !underloaded.isEmpty()) {
                System.out.println("Rebalancing: " + overloaded.size() + " overloaded, " + 
                                 underloaded.size() + " underloaded resources");
                
                for (AID overloadedResource : overloaded) {
                    if (underloaded.isEmpty()) break;
                    
                    AID target = underloaded.get(0);
                    requestMigration(overloadedResource, target);
                    
                    // Update expected loads
                    resourceLoads.get(overloadedResource).decrementLoad();
                    resourceLoads.get(target).incrementLoad();
                    
                    // Remove target if it's no longer underloaded
                    if (resourceLoads.get(target).getCurrentLoad() >= averageLoad) {
                        underloaded.remove(0);
                    }
                }
            }
        }
        
        private double calculateAverageLoad() {
            if (resourceLoads.isEmpty()) return 0;
            
            int totalLoad = resourceLoads.values().stream()
                .mapToInt(ResourceLoad::getCurrentLoad)
                .sum();
            
            return (double) totalLoad / resourceLoads.size();
        }
        
        private void requestMigration(AID source, AID target) {
            ACLMessage migrate = new ACLMessage(ACLMessage.REQUEST);
            migrate.addReceiver(source);
            migrate.setContent("MIGRATE_PATIENT:" + target.getName());
            myAgent.send(migrate);
            
            System.out.println("Requested migration from " + source.getLocalName() + 
                             " to " + target.getLocalName());
        }
    }
    
    /**
     * Collect and report metrics
     */
    private class MetricsCollector extends TickerBehaviour {
        public MetricsCollector(Agent agent, long period) {
            super(agent, period);
        }
        
        @Override
        protected void onTick() {
            System.out.println("\n=== Load Balancer Metrics ===");
            System.out.println("Strategy: " + strategy.getName());
            System.out.println("Total Requests: " + totalRequests);
            System.out.println("Successful Allocations: " + successfulAllocations);
            System.out.println("Success Rate: " + 
                (totalRequests > 0 ? (successfulAllocations * 100.0 / totalRequests) : 0) + "%");
            
            System.out.println("\nResource Loads:");
            for (Map.Entry<AID, ResourceLoad> entry : resourceLoads.entrySet()) {
                ResourceLoad load = entry.getValue();
                System.out.println("  " + entry.getKey().getLocalName() + 
                    ": Current=" + load.getCurrentLoad() + 
                    ", Average=" + String.format("%.2f", load.getAverageLoad()) +
                    ", Peak=" + load.getPeakLoad());
            }
            
            // Calculate load variance (measure of balance)
            double variance = calculateLoadVariance();
            System.out.println("\nLoad Variance: " + String.format("%.2f", variance) + 
                             " (lower is better)");
            System.out.println("============================\n");
        }
        
        private double calculateLoadVariance() {
            if (resourceLoads.isEmpty()) return 0;
            
            double mean = resourceLoads.values().stream()
                .mapToInt(ResourceLoad::getCurrentLoad)
                .average()
                .orElse(0);
            
            return resourceLoads.values().stream()
                .mapToDouble(load -> Math.pow(load.getCurrentLoad() - mean, 2))
                .average()
                .orElse(0);
        }
    }
    
    /**
     * Resource load tracking
     */
    private static class ResourceLoad {
        private AID resource;
        private String type;
        private int currentLoad;
        private double averageLoad;
        private int peakLoad;
        private List<String> assignedPatients;
        private long lastUpdateTime;
        
        public ResourceLoad(AID resource, String type) {
            this.resource = resource;
            this.type = type;
            this.currentLoad = 0;
            this.averageLoad = 0;
            this.peakLoad = 0;
            this.assignedPatients = new ArrayList<>();
            this.lastUpdateTime = System.currentTimeMillis();
        }
        
        public void incrementLoad() { 
            currentLoad++;
            if (currentLoad > peakLoad) peakLoad = currentLoad;
        }
        
        public void decrementLoad() { 
            if (currentLoad > 0) currentLoad--;
        }
        
        public void setCurrentLoad(int load) { 
            this.currentLoad = load;
            if (load > peakLoad) peakLoad = load;
        }
        
        public void updateAverageLoad(int load) {
            averageLoad = averageLoad * 0.9 + load * 0.1; // Exponential moving average
        }
        
        public boolean isOverloaded() {
            return currentLoad > 5; // Threshold
        }
        
        public void addPatient(String patientId) {
            assignedPatients.add(patientId);
        }
        
        // Getters
        public int getCurrentLoad() { return currentLoad; }
        public double getAverageLoad() { return averageLoad; }
        public int getPeakLoad() { return peakLoad; }
    }
    
    /**
     * Resource metrics tracking
     */
    private static class ResourceMetrics {
        private AID resource;
        private long totalAllocations;
        private long totalProcessingTime;
        private long lastAllocationTime;
        
        public ResourceMetrics(AID resource) {
            this.resource = resource;
            this.totalAllocations = 0;
            this.totalProcessingTime = 0;
        }
        
        public void incrementAllocations() {
            totalAllocations++;
            lastAllocationTime = System.currentTimeMillis();
        }
    }
    
    /**
     * Load balancing strategy interface
     */
    private interface LoadBalancingStrategy {
        AID selectResource(List<AID> resources, Map<AID, ResourceLoad> loads, Patient patient);
        String getName();
    }
    
    /**
     * Round Robin strategy
     */
    private static class RoundRobinStrategy implements LoadBalancingStrategy {
        private int currentIndex = 0;
        
        @Override
        public AID selectResource(List<AID> resources, Map<AID, ResourceLoad> loads, Patient patient) {
            if (resources.isEmpty()) return null;
            
            AID selected = resources.get(currentIndex % resources.size());
            currentIndex++;
            return selected;
        }
        
        @Override
        public String getName() { return "Round Robin"; }
    }
    
    /**
     * Least Connections strategy
     */
    private static class LeastConnectionsStrategy implements LoadBalancingStrategy {
        @Override
        public AID selectResource(List<AID> resources, Map<AID, ResourceLoad> loads, Patient patient) {
            return resources.stream()
                .min(Comparator.comparing(r -> loads.get(r).getCurrentLoad()))
                .orElse(null);
        }
        
        @Override
        public String getName() { return "Least Connections"; }
    }
    
    /**
     * Weighted Round Robin strategy
     */
    private static class WeightedRoundRobinStrategy implements LoadBalancingStrategy {
        private Map<AID, Integer> weights = new HashMap<>();
        private Map<AID, Integer> currentWeights = new HashMap<>();
        
        @Override
        public AID selectResource(List<AID> resources, Map<AID, ResourceLoad> loads, Patient patient) {
            // Initialize weights based on capacity
            for (AID resource : resources) {
                if (!weights.containsKey(resource)) {
                    // Higher weight for resources with lower average load
                    int weight = Math.max(1, 10 - (int)loads.get(resource).getAverageLoad());
                    weights.put(resource, weight);
                    currentWeights.put(resource, weight);
                }
            }
            
            // Select resource with highest current weight
            AID selected = resources.stream()
                .max(Comparator.comparing(r -> currentWeights.get(r)))
                .orElse(null);
            
            if (selected != null) {
                // Decrease weight and reset if needed
                int weight = currentWeights.get(selected) - 1;
                if (weight <= 0) {
                    weight = weights.get(selected);
                }
                currentWeights.put(selected, weight);
            }
            
            return selected;
        }
        
        @Override
        public String getName() { return "Weighted Round Robin"; }
    }
    
    /**
     * Adaptive load balancing strategy
     */
    private static class AdaptiveLoadBalancingStrategy implements LoadBalancingStrategy {
        @Override
        public AID selectResource(List<AID> resources, Map<AID, ResourceLoad> loads, Patient patient) {
            // Complex selection based on multiple factors
            return resources.stream()
                .min(Comparator.comparing(r -> calculateScore(r, loads.get(r), patient)))
                .orElse(null);
        }
        
        private double calculateScore(AID resource, ResourceLoad load, Patient patient) {
            double score = load.getCurrentLoad() * 1.0;
            score += load.getAverageLoad() * 0.5;
            
            // Urgency factor
            if (patient.getUrgencyLevel() >= 4) {
                score *= 0.8; // Prefer less loaded for urgent patients
            }
            
            return score;
        }
        
        @Override
        public String getName() { return "Adaptive"; }
    }
}
