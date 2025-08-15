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
import models.Patient;
import utils.MessageProtocol;
import utils.SchedulingAlgorithm;

import java.io.*;
import java.util.*;

/**
 * Scheduler Agent - Coordinates resource allocation in the hospital
 * Master IA - Systèmes Multi-Agents Project
 */
public class SchedulerAgent extends Agent {
    private SchedulingAlgorithm scheduler;
    private Map<String, Patient> patients;
    private List<AID> doctorAgents;
    private List<AID> roomAgents;
    private List<AID> equipmentAgents;
    
    // Statistics
    private int totalPatientsProcessed;
    private long totalWaitTime;
    private int successfulAllocations;
    private int failedAllocations;
    
    @Override
    protected void setup() {
        System.out.println("Scheduler Agent " + getLocalName() + " started.");
        
        // Initialize data structures
        scheduler = new SchedulingAlgorithm();
        patients = new HashMap<>();
        doctorAgents = new ArrayList<>();
        roomAgents = new ArrayList<>();
        equipmentAgents = new ArrayList<>();
        
        // Register in Yellow Pages
        registerInYellowPages();
        
        // Add behaviors
        addBehaviour(new PatientRequestReceiver());
        addBehaviour(new ResourceDiscoveryBehaviour(this, 10000)); // Discover resources every 10 seconds
        addBehaviour(new ResourceAllocationBehaviour(this, 5000)); // Try allocation every 5 seconds
        addBehaviour(new StatisticsReportBehaviour(this, 30000)); // Report stats every 30 seconds
    }
    
    private void registerInYellowPages() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType(MessageProtocol.SCHEDULER_SERVICE);
        sd.setName("Hospital-Scheduler");
        dfd.addServices(sd);
        
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }
    
    @Override
    protected void takeDown() {
        // Deregister from Yellow Pages
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        
        System.out.println("Scheduler Agent " + getLocalName() + " terminating.");
    }
    
    /**
     * Behavior to receive treatment requests from patients
     */
    private class PatientRequestReceiver extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                MessageTemplate.MatchConversationId(MessageProtocol.TREATMENT_REQUEST)
            );
            
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                try {
                    // Deserialize patient data
                    ByteArrayInputStream bais = new ByteArrayInputStream(msg.getByteSequenceContent());
                    ObjectInputStream ois = new ObjectInputStream(bais);
                    Patient patient = (Patient) ois.readObject();
                    
                    // Add to scheduler queue and tracking map
                    scheduler.addPatient(patient);
                    patients.put(patient.getId(), patient);
                    
                    System.out.println("Scheduler received treatment request from " + patient.getName() + 
                                       " (Urgency: " + patient.getUrgencyLevel() + ", Treatment: " + 
                                       patient.getTreatmentType() + ")");
                    
                    // Send acknowledgment
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent("Request received. You are #" + scheduler.getQueueSize() + " in queue.");
                    myAgent.send(reply);
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                block();
            }
        }
    }
    
    /**
     * Behavior to discover available resources (doctors, rooms, equipment)
     */
    private class ResourceDiscoveryBehaviour extends TickerBehaviour {
        public ResourceDiscoveryBehaviour(Agent a, long period) {
            super(a, period);
        }
        
        @Override
        protected void onTick() {
            // Clear previous lists
            doctorAgents.clear();
            roomAgents.clear();
            equipmentAgents.clear();
            
            // Search for doctor agents
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType(MessageProtocol.DOCTOR_SERVICE);
            template.addServices(sd);
            
            try {
                DFAgentDescription[] result = DFService.search(myAgent, template);
                for (DFAgentDescription agent : result) {
                    doctorAgents.add(agent.getName());
                }
                System.out.println("Discovered " + doctorAgents.size() + " doctor agents");
                
                // Search for room agents
                sd.setType(MessageProtocol.ROOM_SERVICE);
                template.addServices(sd);
                result = DFService.search(myAgent, template);
                for (DFAgentDescription agent : result) {
                    roomAgents.add(agent.getName());
                }
                System.out.println("Discovered " + roomAgents.size() + " room agents");
                
                // Search for equipment agents
                sd.setType(MessageProtocol.EQUIPMENT_SERVICE);
                template.addServices(sd);
                result = DFService.search(myAgent, template);
                for (DFAgentDescription agent : result) {
                    equipmentAgents.add(agent.getName());
                }
                System.out.println("Discovered " + equipmentAgents.size() + " equipment agents");
                
            } catch (FIPAException fe) {
                fe.printStackTrace();
            }
        }
    }
    
    /**
     * Behavior to allocate resources to patients
     */
    private class ResourceAllocationBehaviour extends TickerBehaviour {
        public ResourceAllocationBehaviour(Agent a, long period) {
            super(a, period);
        }
        
        @Override
        protected void onTick() {
            if (!scheduler.hasWaitingPatients() || doctorAgents.isEmpty() || roomAgents.isEmpty()) {
                return; // No patients or resources available
            }
            
            // Get next patient from priority queue
            Patient patient = scheduler.getNextPatient();
            if (patient == null) return;
            
            System.out.println("Attempting to allocate resources for patient: " + patient.getName());
            
            // Start a sequential behavior for resource allocation
            SequentialBehaviour allocationSequence = new SequentialBehaviour();
            
            // 1. Find suitable doctor
            allocationSequence.addSubBehaviour(new FindDoctorBehaviour(patient));
            
            // 2. Find suitable room
            allocationSequence.addSubBehaviour(new FindRoomBehaviour(patient));
            
            // 3. Find required equipment if needed
            String requiredEquipment = SchedulingAlgorithm.getRequiredEquipment(patient.getTreatmentType());
            if (requiredEquipment != null) {
                allocationSequence.addSubBehaviour(new FindEquipmentBehaviour(patient, requiredEquipment));
            }
            
            // 4. Finalize allocation
            allocationSequence.addSubBehaviour(new FinalizeAllocationBehaviour(patient));
            
            myAgent.addBehaviour(allocationSequence);
        }
    }
    
    /**
     * Behavior to find an available doctor for a patient
     */
    private class FindDoctorBehaviour extends Behaviour {
        private Patient patient;
        private int step = 0;
        private int repliesCount = 0;
        private AID selectedDoctor = null;
        private boolean done = false;
        
        public FindDoctorBehaviour(Patient patient) {
            this.patient = patient;
        }
        
        @Override
        public void action() {
            switch (step) {
                case 0: // Send availability requests to all doctors
                    ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                    for (AID doctor : doctorAgents) {
                        msg.addReceiver(doctor);
                    }
                    msg.setConversationId(MessageProtocol.RESOURCE_ALLOCATION);
                    msg.setContent(MessageProtocol.CHECK_AVAILABILITY + ":" + patient.getTreatmentType());
                    myAgent.send(msg);
                    step = 1;
                    break;
                    
                case 1: // Collect responses
                    MessageTemplate mt = MessageTemplate.and(
                        MessageTemplate.MatchConversationId(MessageProtocol.RESOURCE_ALLOCATION),
                        MessageTemplate.or(
                            MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
                            MessageTemplate.MatchPerformative(ACLMessage.REFUSE)
                        )
                    );
                    
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.CONFIRM && selectedDoctor == null) {
                            // First available doctor is selected
                            selectedDoctor = reply.getSender();
                            String[] parts = reply.getContent().split(":");
                            if (parts.length >= 2) {
                                patient.setAssignedDoctor(parts[1]); // Store doctor ID
                            }
                        }
                        repliesCount++;
                        if (repliesCount >= doctorAgents.size() || selectedDoctor != null) {
                            step = 2;
                        }
                    } else {
                        block();
                    }
                    break;
                    
                case 2: // Assign patient to selected doctor
                    if (selectedDoctor != null) {
                        ACLMessage assignMsg = new ACLMessage(ACLMessage.REQUEST);
                        assignMsg.addReceiver(selectedDoctor);
                        assignMsg.setConversationId(MessageProtocol.RESOURCE_ALLOCATION);
                        assignMsg.setContent(MessageProtocol.ASSIGN_PATIENT + ":" + 
                                           patient.getId() + ":" + patient.getTreatmentType());
                        myAgent.send(assignMsg);
                        System.out.println("Doctor assigned to patient " + patient.getName() + ": " + selectedDoctor.getLocalName());
                    } else {
                        // No doctor available, put patient back in queue
                        scheduler.addPatient(patient);
                        System.out.println("No doctor available for patient " + patient.getName() + ", returning to queue");
                    }
                    done = true;
                    break;
            }
        }
        
        @Override
        public boolean done() {
            return done;
        }
    }
    
    /**
     * Behavior to find an available room for a patient
     */
    private class FindRoomBehaviour extends Behaviour {
        private Patient patient;
        private int step = 0;
        private int repliesCount = 0;
        private AID selectedRoom = null;
        private boolean done = false;
        
        public FindRoomBehaviour(Patient patient) {
            this.patient = patient;
        }
        
        @Override
        public void action() {
            switch (step) {
                case 0: // Send availability requests to all rooms
                    if (patient.getAssignedDoctor() == null) {
                        // If no doctor was assigned, skip room allocation
                        done = true;
                        break;
                    }
                    
                    ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                    for (AID room : roomAgents) {
                        msg.addReceiver(room);
                    }
                    msg.setConversationId(MessageProtocol.RESOURCE_ALLOCATION);
                    msg.setContent(MessageProtocol.CHECK_AVAILABILITY + ":" + patient.getTreatmentType());
                    myAgent.send(msg);
                    step = 1;
                    break;
                    
                case 1: // Collect responses
                    MessageTemplate mt = MessageTemplate.and(
                        MessageTemplate.MatchConversationId(MessageProtocol.RESOURCE_ALLOCATION),
                        MessageTemplate.or(
                            MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
                            MessageTemplate.MatchPerformative(ACLMessage.REFUSE)
                        )
                    );
                    
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.CONFIRM && selectedRoom == null) {
                            // First available room is selected
                            selectedRoom = reply.getSender();
                            String[] parts = reply.getContent().split(":");
                            if (parts.length >= 2) {
                                patient.setAssignedRoom(parts[1]); // Store room ID
                            }
                        }
                        repliesCount++;
                        if (repliesCount >= roomAgents.size() || selectedRoom != null) {
                            step = 2;
                        }
                    } else {
                        block();
                    }
                    break;
                    
                case 2: // Allocate room
                    if (selectedRoom != null) {
                        ACLMessage allocateMsg = new ACLMessage(ACLMessage.REQUEST);
                        allocateMsg.addReceiver(selectedRoom);
                        allocateMsg.setConversationId(MessageProtocol.RESOURCE_ALLOCATION);
                        allocateMsg.setContent(MessageProtocol.ALLOCATE_RESOURCE + ":" + 
                                             patient.getId() + ":" + patient.getAssignedDoctor());
                        myAgent.send(allocateMsg);
                        System.out.println("Room assigned to patient " + patient.getName() + ": " + selectedRoom.getLocalName());
                    } else {
                        // No room available, put patient back in queue
                        scheduler.addPatient(patient);
                        System.out.println("No room available for patient " + patient.getName() + ", returning to queue");
                    }
                    done = true;
                    break;
            }
        }
        
        @Override
        public boolean done() {
            return done;
        }
    }
    
    /**
     * Behavior to find required equipment for a patient
     */
    private class FindEquipmentBehaviour extends Behaviour {
        private Patient patient;
        private String equipmentType;
        private int step = 0;
        private int repliesCount = 0;
        private AID selectedEquipment = null;
        private boolean done = false;
        
        public FindEquipmentBehaviour(Patient patient, String equipmentType) {
            this.patient = patient;
            this.equipmentType = equipmentType;
        }
        
        @Override
        public void action() {
            switch (step) {
                case 0: // Send availability requests to all equipment
                    if (patient.getAssignedDoctor() == null || patient.getAssignedRoom() == null) {
                        // If no doctor or room was assigned, skip equipment allocation
                        done = true;
                        break;
                    }
                    
                    ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                    for (AID equipment : equipmentAgents) {
                        msg.addReceiver(equipment);
                    }
                    msg.setConversationId(MessageProtocol.RESOURCE_ALLOCATION);
                    msg.setContent(MessageProtocol.CHECK_AVAILABILITY + ":" + equipmentType);
                    myAgent.send(msg);
                    step = 1;
                    break;
                    
                case 1: // Collect responses
                    MessageTemplate mt = MessageTemplate.and(
                        MessageTemplate.MatchConversationId(MessageProtocol.RESOURCE_ALLOCATION),
                        MessageTemplate.or(
                            MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
                            MessageTemplate.MatchPerformative(ACLMessage.REFUSE)
                        )
                    );
                    
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.CONFIRM && selectedEquipment == null) {
                            // First available equipment is selected
                            selectedEquipment = reply.getSender();
                            String[] parts = reply.getContent().split(":");
                            if (parts.length >= 2) {
                                patient.setRequiredEquipment(parts[1]); // Store equipment ID
                            }
                        }
                        repliesCount++;
                        if (repliesCount >= equipmentAgents.size() || selectedEquipment != null) {
                            step = 2;
                        }
                    } else {
                        block();
                    }
                    break;
                    
                case 2: // Allocate equipment
                    if (selectedEquipment != null) {
                        ACLMessage allocateMsg = new ACLMessage(ACLMessage.REQUEST);
                        allocateMsg.addReceiver(selectedEquipment);
                        allocateMsg.setConversationId(MessageProtocol.RESOURCE_ALLOCATION);
                        allocateMsg.setContent(MessageProtocol.ALLOCATE_RESOURCE + ":" + patient.getId());
                        myAgent.send(allocateMsg);
                        System.out.println("Equipment assigned to patient " + patient.getName() + ": " + selectedEquipment.getLocalName());
                    } else {
                        // No equipment available, put patient back in queue
                        scheduler.addPatient(patient);
                        System.out.println("No " + equipmentType + " available for patient " + patient.getName() + ", returning to queue");
                    }
                    done = true;
                    break;
            }
        }
        
        @Override
        public boolean done() {
            return done;
        }
    }
    
    /**
     * Behavior to finalize resource allocation and notify patient
     */
    private class FinalizeAllocationBehaviour extends OneShotBehaviour {
        private Patient patient;
        
        public FinalizeAllocationBehaviour(Patient patient) {
            this.patient = patient;
        }
        
        @Override
        public void action() {
            if (patient.getAssignedDoctor() != null && patient.getAssignedRoom() != null) {
                // All required resources allocated successfully
                patient.setStatus("IN_TREATMENT");
                patient.updateWaitingTime();
                
                // Update statistics
                totalPatientsProcessed++;
                totalWaitTime += patient.getWaitingTime();
                successfulAllocations++;
                
                // Notify patient about allocation
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType(MessageProtocol.PATIENT_SERVICE);
                template.addServices(sd);
                
                try {
                    DFAgentDescription[] result = DFService.search(myAgent, template);
                    for (DFAgentDescription agent : result) {
                        // Find the patient agent
                        if (agent.getName().getLocalName().contains(patient.getName())) {
                            ACLMessage notification = new ACLMessage(ACLMessage.INFORM);
                            notification.addReceiver(agent.getName());
                            notification.setConversationId(MessageProtocol.STATUS_UPDATE);
                            
                            StringBuilder content = new StringBuilder("RESOURCES_ALLOCATED:");
                            content.append(patient.getAssignedDoctor()).append(":");
                            content.append(patient.getAssignedRoom());
                            if (patient.getRequiredEquipment() != null) {
                                content.append(":").append(patient.getRequiredEquipment());
                            }
                            
                            notification.setContent(content.toString());
                            myAgent.send(notification);
                            
                            System.out.println("Patient " + patient.getName() + " has been allocated all resources");
                            break;
                        }
                    }
                } catch (FIPAException fe) {
                    fe.printStackTrace();
                }
            } else {
                // Resource allocation failed
                failedAllocations++;
                
                // Put patient back in queue with higher priority
                if (!patients.containsKey(patient.getId())) {
                    // Only if patient is not already in queue
                    scheduler.addPatient(patient);
                }
            }
        }
    }
    
    /**
     * Behavior to report system statistics periodically
     */
    private class StatisticsReportBehaviour extends TickerBehaviour {
        public StatisticsReportBehaviour(Agent a, long period) {
            super(a, period);
        }
        
        @Override
        protected void onTick() {
            System.out.println("\n=== HOSPITAL STATISTICS ====");
            System.out.println("Total patients processed: " + totalPatientsProcessed);
            System.out.println("Patients in queue: " + scheduler.getQueueSize());
            System.out.println("Successful allocations: " + successfulAllocations);
            System.out.println("Failed allocations: " + failedAllocations);
            
            if (totalPatientsProcessed > 0) {
                double avgWaitTime = totalWaitTime / (double) totalPatientsProcessed / 1000.0;
                System.out.println("Average wait time: " + String.format("%.2f", avgWaitTime) + " seconds");
            }
            
            System.out.println("Available resources: " + doctorAgents.size() + " doctors, " + 
                             roomAgents.size() + " rooms, " + equipmentAgents.size() + " equipment");
            System.out.println("==============================\n");
        }
    }
}