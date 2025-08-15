import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

/**
 * Main class to start the Hospital Resource Allocation System
 * Master IA - Systèmes Multi-Agents Project
 */
public class HospitalMain {
    public static void main(String[] args) {
        try {
            // Get JADE runtime
            Runtime rt = Runtime.instance();
            
            // Create a default profile
            Profile profile = new ProfileImpl(true);
            profile.setParameter(Profile.GUI, "true"); // Start JADE GUI
            
            // Create main container
            AgentContainer mainContainer = rt.createMainContainer(profile);
            
            System.out.println("Starting Hospital Resource Allocation System...");
            
            // Create Scheduler Agent
            AgentController schedulerAgent = mainContainer.createNewAgent(
                "Scheduler", "agents.SchedulerAgent", new Object[]{});
            schedulerAgent.start();
            
            // Create Monitoring Agent
            AgentController monitoringAgent = mainContainer.createNewAgent(
                "Monitor", "agents.MonitoringAgent", new Object[]{});
            monitoringAgent.start();
            
            // Create Doctor Agents
            String[] doctorSpecializations = {"General", "Surgery", "Cardiology", "Neurology", "Pediatrics"};
            for (int i = 0; i < doctorSpecializations.length; i++) {
                AgentController doctorAgent = mainContainer.createNewAgent(
                    "Doctor" + i, "agents.DoctorAgent", 
                    new Object[]{"D" + i, "Doctor " + i, doctorSpecializations[i]});
                doctorAgent.start();
            }
            
            // Create Room Agents
            String[] roomTypes = {"CONSULTATION", "SURGERY", "EMERGENCY", "ICU", "CONSULTATION"};
            int[] roomCapacities = {1, 2, 3, 2, 1};
            for (int i = 0; i < roomTypes.length; i++) {
                AgentController roomAgent = mainContainer.createNewAgent(
                    "Room" + i, "agents.RoomAgent", 
                    new Object[]{"R" + i, roomTypes[i], roomCapacities[i]});
                roomAgent.start();
            }
            
            // Create Equipment Agents
            String[] equipmentTypes = {"MRI", "CT_SCAN", "XRAY", "VENTILATOR", "ECG"};
            for (int i = 0; i < equipmentTypes.length; i++) {
                AgentController equipmentAgent = mainContainer.createNewAgent(
                    "Equipment" + i, "agents.EquipmentAgent", 
                    new Object[]{"E" + i, equipmentTypes[i]});
                equipmentAgent.start();
            }
            
            // Create some initial patients
            String[] patientNames = {"Alice", "Bob", "Charlie", "Diana", "Edward"};
            String[] treatments = {"CONSULTATION", "EMERGENCY", "SURGERY", "CHECKUP", "XRAY"};
            int[] urgencyLevels = {2, 5, 3, 1, 4};
            
            for (int i = 0; i < patientNames.length; i++) {
                AgentController patientAgent = mainContainer.createNewAgent(
                    "Patient_" + patientNames[i], "agents.PatientAgent", 
                    new Object[]{patientNames[i], urgencyLevels[i], treatments[i]});
                patientAgent.start();
            }
            
            System.out.println("Hospital Resource Allocation System started successfully.");
            System.out.println("Created " + doctorSpecializations.length + " doctors, " + 
                             roomTypes.length + " rooms, " + 
                             equipmentTypes.length + " equipment, and " + 
                             patientNames.length + " initial patients.");
            
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }
}