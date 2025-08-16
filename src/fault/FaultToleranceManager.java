package fault;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fault Tolerance Manager - Monitors agent health and handles recovery
 * Implements heartbeat monitoring, automatic restart, and state persistence
 * Master IA - Systèmes Multi-Agents Project
 */
public class FaultToleranceManager extends Agent {
    
    private Map<AID, AgentHealthStatus> agentHealthMap;
    private Map<String, AgentState> persistedStates;
    private final long HEARTBEAT_INTERVAL = 5000; // 5 seconds
    private final long FAILURE_THRESHOLD = 15000; // 15 seconds without heartbeat = failure
    private final String STATE_FILE = "agent_states.ser";
    
    @Override
    protected void setup() {
        System.out.println("Fault Tolerance Manager started");
        
        agentHealthMap = new ConcurrentHashMap<>();
        persistedStates = new ConcurrentHashMap<>();
        
        // Load persisted states if available
        loadPersistedStates();
        
        // Register as fault tolerance service
        registerService();
        
        // Add behaviors
        addBehaviour(new HeartbeatMonitor());
        addBehaviour(new FailureDetector(this, HEARTBEAT_INTERVAL));
        addBehaviour(new StateBackupManager(this, 30000));
        addBehaviour(new RecoveryManager());
    }
    
    private void registerService() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("fault-tolerance-service");
        sd.setName("FaultToleranceManager");
        dfd.addServices(sd);
        
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }
    
    /**
     * Heartbeat monitoring behavior
     */
    private class HeartbeatMonitor extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            ACLMessage msg = myAgent.receive(mt);
            
            if (msg != null && msg.getContent() != null) {
                if (msg.getContent().startsWith("HEARTBEAT:")) {
                    handleHeartbeat(msg.getSender());
                } else if (msg.getContent().startsWith("REGISTER:")) {
                    registerAgent(msg.getSender(), msg.getContent());
                } else if (msg.getContent().startsWith("STATE_UPDATE:")) {
                    handleStateUpdate(msg);
                }
            } else {
                block();
            }
        }
        
        private void handleHeartbeat(AID sender) {
            AgentHealthStatus status = agentHealthMap.get(sender);
            if (status != null) {
                status.updateLastHeartbeat();
                status.setStatus(HealthStatus.HEALTHY);
            } else {
                // New agent registration
                agentHealthMap.put(sender, new AgentHealthStatus(sender));
            }
        }
        
        private void registerAgent(AID agent, String content) {
            String[] parts = content.split(":");
            if (parts.length >= 2) {
                String agentType = parts[1];
                AgentHealthStatus status = new AgentHealthStatus(agent);
                status.setAgentType(agentType);
                agentHealthMap.put(agent, status);
                System.out.println("Registered agent " + agent.getLocalName() + " for monitoring");
            }
        }
        
        private void handleStateUpdate(ACLMessage msg) {
            try {
                // Extract serialized state from message
                byte[] stateData = msg.getByteSequenceContent();
                ByteArrayInputStream bais = new ByteArrayInputStream(stateData);
                ObjectInputStream ois = new ObjectInputStream(bais);
                AgentState state = (AgentState) ois.readObject();
                
                persistedStates.put(msg.getSender().getLocalName(), state);
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Failure detection behavior
     */
    private class FailureDetector extends TickerBehaviour {
        public FailureDetector(Agent agent, long period) {
            super(agent, period);
        }
        
        @Override
        protected void onTick() {
            long currentTime = System.currentTimeMillis();
            List<AID> failedAgents = new ArrayList<>();
            
            for (Map.Entry<AID, AgentHealthStatus> entry : agentHealthMap.entrySet()) {
                AgentHealthStatus status = entry.getValue();
                long timeSinceLastHeartbeat = currentTime - status.getLastHeartbeat();
                
                if (timeSinceLastHeartbeat > FAILURE_THRESHOLD) {
                    if (status.getStatus() != HealthStatus.FAILED) {
                        status.setStatus(HealthStatus.FAILED);
                        failedAgents.add(entry.getKey());
                        System.out.println("FAILURE DETECTED: " + entry.getKey().getLocalName());
                    }
                } else if (timeSinceLastHeartbeat > HEARTBEAT_INTERVAL * 2) {
                    status.setStatus(HealthStatus.SUSPECTED);
                }
            }
            
            // Trigger recovery for failed agents
            for (AID failedAgent : failedAgents) {
                myAgent.addBehaviour(new RecoverAgent(failedAgent));
            }
        }
    }
    
    /**
     * Agent recovery behavior
     */
    private class RecoverAgent extends OneShotBehaviour {
        private AID failedAgent;
        
        public RecoverAgent(AID failedAgent) {
            this.failedAgent = failedAgent;
        }
        
        @Override
        public void action() {
            System.out.println("Attempting to recover " + failedAgent.getLocalName());
            
            AgentHealthStatus healthStatus = agentHealthMap.get(failedAgent);
            if (healthStatus == null) return;
            
            try {
                // Get the agent's container
                AgentContainer container = myAgent.getContainerController();
                
                // Prepare agent arguments based on persisted state
                Object[] args = prepareAgentArguments(failedAgent.getLocalName(), healthStatus.getAgentType());
                
                // Create new agent instance
                String newAgentName = failedAgent.getLocalName() + "_recovered_" + System.currentTimeMillis();
                AgentController newAgent = container.createNewAgent(
                    newAgentName,
                    getAgentClassName(healthStatus.getAgentType()),
                    args
                );
                
                // Start the recovered agent
                newAgent.start();
                
                // Update health status
                healthStatus.setStatus(HealthStatus.RECOVERED);
                healthStatus.incrementRecoveryCount();
                
                System.out.println("Successfully recovered " + failedAgent.getLocalName() + 
                                 " as " + newAgentName);
                
                // Notify other agents about the recovery
                notifyRecovery(failedAgent, new AID(newAgentName, AID.ISLOCALNAME));
                
            } catch (Exception e) {
                System.err.println("Failed to recover " + failedAgent.getLocalName() + ": " + e.getMessage());
                healthStatus.incrementFailedRecoveryCount();
                
                // If recovery fails multiple times, escalate
                if (healthStatus.getFailedRecoveryCount() > 3) {
                    escalateFailure(failedAgent);
                }
            }
        }
        
        private Object[] prepareAgentArguments(String agentName, String agentType) {
            // Restore from persisted state if available
            AgentState state = persistedStates.get(agentName);
            if (state != null) {
                return state.getArguments();
            }
            
            // Default arguments based on agent type
            switch (agentType) {
                case "DoctorAgent":
                    return new Object[]{"D_REC", "Recovered Doctor", "General"};
                case "RoomAgent":
                    return new Object[]{"R_REC", "CONSULTATION", 1};
                case "EquipmentAgent":
                    return new Object[]{"E_REC", "XRAY"};
                default:
                    return new Object[]{};
            }
        }
        
        private String getAgentClassName(String agentType) {
            return "agents." + agentType;
        }
        
        private void notifyRecovery(AID oldAgent, AID newAgent) {
            ACLMessage notification = new ACLMessage(ACLMessage.INFORM);
            notification.setContent("AGENT_RECOVERED:" + oldAgent.getLocalName() + ":" + newAgent.getLocalName());
            
            // Notify scheduler and monitoring agents
            for (AID agent : agentHealthMap.keySet()) {
                if (agent.getLocalName().contains("Scheduler") || 
                    agent.getLocalName().contains("Monitor")) {
                    notification.addReceiver(agent);
                }
            }
            
            myAgent.send(notification);
        }
        
        private void escalateFailure(AID agent) {
            System.err.println("CRITICAL: Multiple recovery attempts failed for " + agent.getLocalName());
            // Could trigger alerts, notifications, or manual intervention
        }
    }
    
    /**
     * State backup manager
     */
    private class StateBackupManager extends TickerBehaviour {
        public StateBackupManager(Agent agent, long period) {
            super(agent, period);
        }
        
        @Override
        protected void onTick() {
            savePersistedStates();
        }
    }
    
    /**
     * Recovery manager for system-wide failures
     */
    private class RecoveryManager extends OneShotBehaviour {
        @Override
        public void action() {
            // Check for system-wide failures
            int failedCount = 0;
            int totalCount = agentHealthMap.size();
            
            for (AgentHealthStatus status : agentHealthMap.values()) {
                if (status.getStatus() == HealthStatus.FAILED) {
                    failedCount++;
                }
            }
            
            // If more than 50% of agents have failed, trigger system recovery
            if (totalCount > 0 && failedCount > totalCount / 2) {
                System.err.println("SYSTEM FAILURE: More than 50% of agents have failed");
                initiateSystemRecovery();
            }
        }
        
        private void initiateSystemRecovery() {
            System.out.println("Initiating system-wide recovery...");
            // Implement system recovery logic
            // Could include restarting all agents, restoring from checkpoint, etc.
        }
    }
    
    private void loadPersistedStates() {
        try {
            File file = new File(STATE_FILE);
            if (file.exists()) {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
                persistedStates = (Map<String, AgentState>) ois.readObject();
                ois.close();
                System.out.println("Loaded " + persistedStates.size() + " persisted agent states");
            }
        } catch (Exception e) {
            System.err.println("Could not load persisted states: " + e.getMessage());
        }
    }
    
    private void savePersistedStates() {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(STATE_FILE));
            oos.writeObject(persistedStates);
            oos.close();
        } catch (Exception e) {
            System.err.println("Could not save persisted states: " + e.getMessage());
        }
    }
    
    /**
     * Agent health status tracking
     */
    private static class AgentHealthStatus {
        private AID agent;
        private String agentType;
        private long lastHeartbeat;
        private HealthStatus status;
        private int recoveryCount;
        private int failedRecoveryCount;
        
        public AgentHealthStatus(AID agent) {
            this.agent = agent;
            this.lastHeartbeat = System.currentTimeMillis();
            this.status = HealthStatus.HEALTHY;
            this.recoveryCount = 0;
            this.failedRecoveryCount = 0;
        }
        
        public void updateLastHeartbeat() {
            this.lastHeartbeat = System.currentTimeMillis();
        }
        
        // Getters and setters
        public String getAgentType() { return agentType; }
        public void setAgentType(String type) { this.agentType = type; }
        public long getLastHeartbeat() { return lastHeartbeat; }
        public HealthStatus getStatus() { return status; }
        public void setStatus(HealthStatus status) { this.status = status; }
        public void incrementRecoveryCount() { this.recoveryCount++; }
        public void incrementFailedRecoveryCount() { this.failedRecoveryCount++; }
        public int getFailedRecoveryCount() { return failedRecoveryCount; }
    }
    
    /**
     * Agent state for persistence
     */
    public static class AgentState implements Serializable {
        private String agentName;
        private String agentType;
        private Object[] arguments;
        private Map<String, Object> stateData;
        private long timestamp;
        
        public AgentState(String agentName, String agentType, Object[] arguments) {
            this.agentName = agentName;
            this.agentType = agentType;
            this.arguments = arguments;
            this.stateData = new HashMap<>();
            this.timestamp = System.currentTimeMillis();
        }
        
        public Object[] getArguments() { return arguments; }
        public Map<String, Object> getStateData() { return stateData; }
        public void addStateData(String key, Object value) { stateData.put(key, value); }
    }
    
    private enum HealthStatus {
        HEALTHY, SUSPECTED, FAILED, RECOVERED
    }
}
