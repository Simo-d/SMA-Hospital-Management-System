import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

/**
 * Enhanced Hospital Resource Allocation System
 * With ML, Negotiation, Fault Tolerance, Load Balancing and Analytics
 * Master IA - Systèmes Multi-Agents Project
 */
public class HospitalMain {
    
    private static AgentContainer mainContainer;
    
    public static void main(String[] args) {
        try {
            // Get JADE runtime
            Runtime rt = Runtime.instance();
            
            // Create a default profile
            Profile profile = new ProfileImpl(true);
            profile.setParameter(Profile.GUI, "true"); // Start JADE GUI
            
            // Create main container
            mainContainer = rt.createMainContainer(profile);
            
            System.out.println("===============================================");
            System.out.println("  Hospital Resource Allocation System v2.0");
            System.out.println("  Enhanced with ML, Negotiation & Analytics");
            System.out.println("===============================================\n");
            
            // Create core infrastructure agents
            createInfrastructureAgents();
            
            // Small delay to ensure monitoring agent is ready
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
            
            // Create resource agents
            createResourceAgents();
            
            // Create initial patients
            createInitialPatients();
            
            // Note: Dashboard is created by MonitoringAgent
            
            System.out.println("\n✅ System started successfully!");
            System.out.println("📊 Analytics Dashboard is available");
            System.out.println("🤖 All agents are operational\n");
            
            // Print usage instructions
            printInstructions();
            
        } catch (StaleProxyException e) {
            System.err.println("❌ Error starting system: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void createInfrastructureAgents() throws StaleProxyException {
        System.out.println("Creating Infrastructure Agents...");
        
        // 1. Monitoring Agent (MUST BE FIRST - creates the dashboard)
        AgentController monitoringAgent = mainContainer.createNewAgent(
            "Monitor", "agents.MonitoringAgent", new Object[]{});
        monitoringAgent.start();
        System.out.println("  ✓ Monitoring Agent created");
        
        // Small delay to ensure monitoring agent and dashboard are ready
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // 2. Scheduler Agent (Core)
        AgentController schedulerAgent = mainContainer.createNewAgent(
            "Scheduler", "agents.SchedulerAgent", new Object[]{});
        schedulerAgent.start();
        System.out.println("  ✓ Scheduler Agent created");
        
        // 3. Fault Tolerance Manager (NEW)
        AgentController faultManager = mainContainer.createNewAgent(
            "FaultManager", "fault.FaultToleranceManager", new Object[]{});
        faultManager.start();
        System.out.println("  ✓ Fault Tolerance Manager created");
        
        // 4. Load Balancer (NEW)
        AgentController loadBalancer = mainContainer.createNewAgent(
            "LoadBalancer", "loadbalancing.LoadBalancer", 
            new Object[]{"adaptive"}); // Using adaptive strategy
        loadBalancer.start();
        System.out.println("  ✓ Load Balancer created (Strategy: Adaptive)");
        
        // Add small delay to ensure infrastructure is ready
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    private static void createResourceAgents() throws StaleProxyException {
        System.out.println("\nCreating Resource Agents...");
        
        // Create Enhanced Doctor Agents with Negotiation capability
        String[] doctorNames = {"Dr. Smith", "Dr. Johnson", "Dr. Williams", "Dr. Brown", "Dr. Davis"};
        String[] specializations = {"General", "Surgery", "Cardiology", "Neurology", "Pediatrics"};
        
        for (int i = 0; i < doctorNames.length; i++) {
            AgentController doctorAgent = mainContainer.createNewAgent(
                "Doctor" + i, 
                "agents.DoctorAgent", 
                new Object[]{"D" + i, doctorNames[i], specializations[i]}
            );
            doctorAgent.start();
        }
        System.out.println("  ✓ " + doctorNames.length + " Doctor Agents created");
        
        // Create Room Agents
        String[] roomTypes = {"CONSULTATION", "SURGERY", "EMERGENCY", "ICU", "CONSULTATION", "EXAMINATION"};
        int[] roomCapacities = {1, 2, 3, 2, 1, 2};
        
        for (int i = 0; i < roomTypes.length; i++) {
            AgentController roomAgent = mainContainer.createNewAgent(
                "Room" + i, 
                "agents.RoomAgent", 
                new Object[]{"R" + i, roomTypes[i], roomCapacities[i]}
            );
            roomAgent.start();
        }
        System.out.println("  ✓ " + roomTypes.length + " Room Agents created");
        
        // Create Equipment Agents
        String[] equipmentTypes = {"MRI", "CT_SCAN", "XRAY", "VENTILATOR", "ECG", "ULTRASOUND"};
        
        for (int i = 0; i < equipmentTypes.length; i++) {
            AgentController equipmentAgent = mainContainer.createNewAgent(
                "Equipment" + i, 
                "agents.EquipmentAgent", 
                new Object[]{"E" + i, equipmentTypes[i]}
            );
            equipmentAgent.start();
        }
        System.out.println("  ✓ " + equipmentTypes.length + " Equipment Agents created");
    }
    
    private static void createInitialPatients() throws StaleProxyException {
        System.out.println("\nCreating Initial Patients...");
        
        // Create diverse set of initial patients
        PatientProfile[] initialPatients = {
            new PatientProfile("Alice", 2, "CONSULTATION"),
            new PatientProfile("Bob", 5, "EMERGENCY"),
            new PatientProfile("Charlie", 3, "SURGERY"),
            new PatientProfile("Diana", 1, "CHECKUP"),
            new PatientProfile("Edward", 4, "XRAY"),
            new PatientProfile("Fiona", 3, "MRI"),
            new PatientProfile("George", 2, "CONSULTATION"),
            new PatientProfile("Hannah", 5, "EMERGENCY")
        };
        
        for (PatientProfile patient : initialPatients) {
            AgentController patientAgent = mainContainer.createNewAgent(
                "Patient_" + patient.name, 
                "agents.PatientAgent", 
                new Object[]{patient.name, patient.urgency, patient.treatment}
            );
            patientAgent.start();
        }
        System.out.println("  ✓ " + initialPatients.length + " Initial Patients created");
    }
    
    private static void printInstructions() {
        System.out.println("\n╔══════════════════════════════════════════════════════╗");
        System.out.println("║             SYSTEM USAGE INSTRUCTIONS                 ║");
        System.out.println("╠══════════════════════════════════════════════════════╣");
        System.out.println("║ 1. Analytics Dashboard:                              ║");
        System.out.println("║    • Click 'Add Random Patient' to add patients      ║");
        System.out.println("║    • Click 'Add Emergency' for urgent cases          ║");
        System.out.println("║    • View real-time metrics in Overview tab          ║");
        System.out.println("║    • Check ML predictions in Predictions tab         ║");
        System.out.println("║                                                       ║");
        System.out.println("║ 2. JADE GUI:                                         ║");
        System.out.println("║    • View all agents in the container                ║");
        System.out.println("║    • Monitor agent communications                    ║");
        System.out.println("║    • Use Sniffer to trace messages                   ║");
        System.out.println("║                                                       ║");
        System.out.println("║ 3. Features:                                         ║");
        System.out.println("║    • ML-based wait time prediction                   ║");
        System.out.println("║    • Contract Net negotiation protocol               ║");
        System.out.println("║    • Automatic agent recovery on failure             ║");
        System.out.println("║    • Dynamic load balancing                          ║");
        System.out.println("║    • Real-time analytics and visualization           ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
    }
    
    /**
     * Helper class for patient profiles
     */
    private static class PatientProfile {
        String name;
        int urgency;
        String treatment;
        
        PatientProfile(String name, int urgency, String treatment) {
            this.name = name;
            this.urgency = urgency;
            this.treatment = treatment;
        }
    }
    
    /**
     * Method to dynamically add a patient (can be called from Dashboard)
     */
    public static void addPatient(String name, int urgency, String treatment) {
        try {
            if (mainContainer != null) {
                AgentController patientAgent = mainContainer.createNewAgent(
                    "Patient_" + name + "_" + System.currentTimeMillis(), 
                    "agents.PatientAgent", 
                    new Object[]{name, urgency, treatment}
                );
                patientAgent.start();
                System.out.println("New patient added: " + name);
            }
        } catch (StaleProxyException e) {
            System.err.println("Error adding patient: " + e.getMessage());
        }
    }
}
