package agents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.wrapper.ContainerController;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import gui.HospitalGUI;
import models.Patient;
import utils.MessageProtocol;

import java.util.*;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.IOException;

/**
 * Monitoring Agent - Manages the GUI and monitors the hospital system
 * Master IA - Systèmes Multi-Agents Project
 */
public class MonitoringAgent extends Agent {
    private HospitalGUI gui;
    private List<Patient> patients;
    private List<Map<String, Object>> doctors;
    private List<Map<String, Object>> rooms;
    private List<Map<String, Object>> equipment;
    
    // Statistics
    private int totalPatients;
    private double avgWaitTime;
    private double successRate;
    private double resourceUtilization;
    
    @Override
    protected void setup() {
        System.out.println("Monitoring Agent " + getLocalName() + " started.");
        
        // Initialize data structures
        patients = new ArrayList<>();
        doctors = new ArrayList<>();
        rooms = new ArrayList<>();
        equipment = new ArrayList<>();
        
        // Initialize statistics
        totalPatients = 0;
        avgWaitTime = 0.0;
        successRate = 0.0;
        resourceUtilization = 0.0;
        
        // Create and show GUI
        gui = new HospitalGUI(this);
        
        // Add behaviors
        addBehaviour(new ResourceMonitorBehaviour(this, 2000)); // Update every 2 seconds
        addBehaviour(new StatisticsCollectorBehaviour(this, 5000)); // Collect stats every 5 seconds
        
        // Add behavior to handle patient status responses
         addBehaviour(new CyclicBehaviour() {
             @Override
             public void action() {
                 MessageTemplate mt = MessageTemplate.and(
                     MessageTemplate.MatchConversationId(MessageProtocol.STATUS_UPDATE),
                     MessageTemplate.MatchPerformative(ACLMessage.INFORM)
                 );
                 
                 ACLMessage msg = myAgent.receive(mt);
                 if (msg != null) {
                     try {
                         // Deserialize the patient data
                         ByteArrayInputStream bais = new ByteArrayInputStream(msg.getByteSequenceContent());
                         ObjectInputStream ois = new ObjectInputStream(bais);
                         Patient patient = (Patient) ois.readObject();
                         
                         // Update or add the patient to our list
                         boolean found = false;
                         for (int i = 0; i < patients.size(); i++) {
                             if (patients.get(i).getName().equals(patient.getName())) {
                                 patients.set(i, patient);
                                 found = true;
                                 break;
                             }
                         }
                         
                         if (!found) {
                             patients.add(patient);
                         }
                         
                         // Update the GUI immediately
                         gui.updatePatientTable(patients);
                         
                     } catch (IOException | ClassNotFoundException e) {
                         e.printStackTrace();
                     }
                 } else {
                     block();
                 }
             }
         });
    }
    
    /**
     * Create a new patient agent
     */
    public void createPatientAgent(String name, int urgency, String treatment) {
        try {
            // Get container controller
            ContainerController container = getContainerController();
            
            // Create unique agent name
            String agentName = "Patient_" + name + "_" + System.currentTimeMillis();
            
            // Start patient agent with arguments
            Object[] args = new Object[] {name, urgency, treatment};
            AgentController patientController = container.createNewAgent(agentName, "agents.PatientAgent", args);
            patientController.start();
            
            System.out.println("Created new patient agent: " + agentName);
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Behavior to monitor resources and update GUI
     */
    private class ResourceMonitorBehaviour extends TickerBehaviour {
        public ResourceMonitorBehaviour(Agent a, long period) {
            super(a, period);
        }
        
        @Override
        protected void onTick() {
            // Clear current data
            patients.clear();
            doctors.clear();
            rooms.clear();
            equipment.clear();
            
            // Collect patient data
            collectPatientData();
            
            // Collect doctor data
            collectDoctorData();
            
            // Collect room data
            collectRoomData();
            
            // Collect equipment data
            collectEquipmentData();
            
            // Update GUI
            gui.updatePatientTable(patients);
            gui.updateDoctorTable(doctors);
            gui.updateRoomTable(rooms);
            gui.updateEquipmentTable(equipment);
        }
        
        private void collectPatientData() {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType(MessageProtocol.PATIENT_SERVICE);
            template.addServices(sd);
            
            try {
                // Search for patient agents in the DF
                DFAgentDescription[] result = DFService.search(myAgent, template);
                System.out.println("Found " + result.length + " patient agents");
                
                // Request status from each patient agent
                for (DFAgentDescription agent : result) {
                    AID patientAID = agent.getName();
                    
                    // Send request for patient data
                    ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
                    request.addReceiver(patientAID);
                    request.setConversationId(MessageProtocol.STATUS_UPDATE);
                    request.setContent("REQUEST_STATUS");
                    myAgent.send(request);
                }
            } catch (FIPAException fe) {
                fe.printStackTrace();
            }
        }
        
        private void collectDoctorData() {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType(MessageProtocol.DOCTOR_SERVICE);
            template.addServices(sd);
            
            try {
                DFAgentDescription[] result = DFService.search(myAgent, template);
                for (DFAgentDescription agent : result) {
                    // For now, we'll create dummy doctor data
                    Map<String, Object> doctor = new HashMap<>();
                    doctor.put("id", agent.getName().getLocalName());
                    doctor.put("name", agent.getName().getLocalName().replace("Doctor", "Dr."));
                    doctor.put("specialization", getRandomSpecialization());
                    doctor.put("available", Math.random() > 0.5);
                    doctor.put("currentPatient", (Boolean)doctor.get("available") ? null : "Patient_" + (int)(Math.random() * 10));
                    doctor.put("patientsServed", (int)(Math.random() * 20));
                    
                    doctors.add(doctor);
                }
            } catch (FIPAException fe) {
                fe.printStackTrace();
            }
        }
        
        private void collectRoomData() {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType(MessageProtocol.ROOM_SERVICE);
            template.addServices(sd);
            
            try {
                DFAgentDescription[] result = DFService.search(myAgent, template);
                for (DFAgentDescription agent : result) {
                    // For now, we'll create dummy room data
                    Map<String, Object> room = new HashMap<>();
                    room.put("id", agent.getName().getLocalName());
                    room.put("type", getRandomRoomType());
                    room.put("available", Math.random() > 0.5);
                    
                    if (!(Boolean)room.get("available")) {
                        room.put("currentPatient", "Patient_" + (int)(Math.random() * 10));
                        room.put("currentDoctor", "Doctor_" + (int)(Math.random() * 5));
                    } else {
                        room.put("currentPatient", null);
                        room.put("currentDoctor", null);
                    }
                    
                    rooms.add(room);
                }
            } catch (FIPAException fe) {
                fe.printStackTrace();
            }
        }
        
        private void collectEquipmentData() {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType(MessageProtocol.EQUIPMENT_SERVICE);
            template.addServices(sd);
            
            try {
                DFAgentDescription[] result = DFService.search(myAgent, template);
                for (DFAgentDescription agent : result) {
                    // For now, we'll create dummy equipment data
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", agent.getName().getLocalName());
                    item.put("type", getRandomEquipmentType());
                    item.put("available", Math.random() > 0.3);
                    item.put("currentPatient", (Boolean)item.get("available") ? null : "Patient_" + (int)(Math.random() * 10));
                    item.put("usageCount", (int)(Math.random() * 50));
                    
                    equipment.add(item);
                }
            } catch (FIPAException fe) {
                fe.printStackTrace();
            }
        }
        
        private String getRandomTreatment() {
            String[] treatments = {"CONSULTATION", "EMERGENCY", "SURGERY", "CHECKUP", "XRAY", "MRI", "CT_SCAN"};
            return treatments[(int)(Math.random() * treatments.length)];
        }
        
        private String getRandomStatus() {
            String[] statuses = {"WAITING", "IN_TREATMENT", "COMPLETED"};
            return statuses[(int)(Math.random() * statuses.length)];
        }
        
        private String getRandomSpecialization() {
            String[] specializations = {"General", "Surgery", "Cardiology", "Neurology", "Pediatrics", "Radiology"};
            return specializations[(int)(Math.random() * specializations.length)];
        }
        
        private String getRandomRoomType() {
            String[] types = {"CONSULTATION", "SURGERY", "EMERGENCY", "ICU"};
            return types[(int)(Math.random() * types.length)];
        }
        
        private String getRandomEquipmentType() {
            String[] types = {"MRI", "CT_SCAN", "XRAY", "VENTILATOR", "ECG"};
            return types[(int)(Math.random() * types.length)];
        }
    }
    
    /**
     * Behavior to collect system statistics
     */
    private class StatisticsCollectorBehaviour extends TickerBehaviour {
        public StatisticsCollectorBehaviour(Agent a, long period) {
            super(a, period);
        }
        
        @Override
        protected void onTick() {
            // For now, we'll generate random statistics
            // In a real implementation, we would query the scheduler agent for actual statistics
            totalPatients = patients.size();
            avgWaitTime = Math.random() * 120; // Random wait time up to 2 minutes
            successRate = 70 + Math.random() * 30; // Random success rate between 70-100%
            resourceUtilization = 50 + Math.random() * 50; // Random utilization between 50-100%
            
            // Update GUI with statistics
            gui.updateStatistics(totalPatients, avgWaitTime, successRate, resourceUtilization);
        }
    }
    

    
    @Override
    protected void takeDown() {
        // Close GUI
        if (gui != null) {
            gui.dispose();
        }
        System.out.println("Monitoring Agent " + getLocalName() + " terminating.");
    }
}