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
                DFAgentDescription[] result = DFService.search(myAgent, template);
                for (DFAgentDescription agent : result) {
                    // For now, we'll create dummy patient data
                    // In a real implementation, we would query each patient agent for its state
                    String name = agent.getName().getLocalName();
                    if (name.startsWith("Patient_")) {
                        String[] parts = name.split("_");
                        if (parts.length >= 2) {
                            Patient patient = new Patient(parts[1], (int)(Math.random() * 5) + 1, 
                                                      getRandomTreatment());
                            patient.setStatus(getRandomStatus());
                            patient.setWaitingTime((long)(Math.random() * 60000));
                            
                            if (!patient.getStatus().equals("WAITING")) {
                                patient.setAssignedDoctor("Doctor_" + (int)(Math.random() * 5));
                                patient.setAssignedRoom("Room_" + (int)(Math.random() * 5));
                            }
                            
                            patients.add(patient);
                        }
                    }
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