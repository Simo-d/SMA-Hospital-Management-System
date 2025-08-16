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

/**
 * Patient Agent - Requests treatment and waits for resource allocation
 * Master IA - Systèmes Multi-Agents Project
 */
public class PatientAgent extends Agent {
    private Patient patientData;
    private AID schedulerAgent;
    private boolean treatmentCompleted = false;
    
    @Override
    protected void setup() {
        // Get arguments: name, urgency, treatment type
        Object[] args = getArguments();
        if (args != null && args.length >= 3) {
            String name = (String) args[0];
            int urgency = Integer.parseInt(args[1].toString());
            String treatment = (String) args[2];
            
            patientData = new Patient(name, urgency, treatment);
            System.out.println("Patient Agent " + getLocalName() + " initialized: " + patientData);
            
            // Notify monitoring agent
            notifyMonitor("PATIENT_REGISTERED:" + patientData.getId());
            
            // Register in Yellow Pages
            registerInYellowPages();
            
            // Add behaviors
            addBehaviour(new RequestTreatmentBehaviour());
            addBehaviour(new WaitForAllocationBehaviour());
            addBehaviour(new UpdateStatusBehaviour(this, 5000)); // Update every 5 seconds
        } else {
            System.err.println("Patient Agent requires arguments: name, urgency, treatment");
            doDelete();
        }
    }
    
    private void registerInYellowPages() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType(MessageProtocol.PATIENT_SERVICE);
        sd.setName("Patient-" + getLocalName());
        dfd.addServices(sd);
        
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }
    
    /**
     * Behavior to request treatment from scheduler
     */
    private class RequestTreatmentBehaviour extends OneShotBehaviour {
        @Override
        public void action() {
            // Find scheduler agent
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType(MessageProtocol.SCHEDULER_SERVICE);
            template.addServices(sd);
            
            try {
                DFAgentDescription[] result = DFService.search(myAgent, template);
                if (result.length > 0) {
                    schedulerAgent = result[0].getName();
                    
                    // Send treatment request
                    ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
                    request.addReceiver(schedulerAgent);
                    request.setConversationId(MessageProtocol.TREATMENT_REQUEST);
                    request.setOntology(MessageProtocol.HOSPITAL_ONTOLOGY);
                    
                    // Serialize patient data
                    try {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ObjectOutputStream oos = new ObjectOutputStream(baos);
                        oos.writeObject(patientData);
                        request.setByteSequenceContent(baos.toByteArray());
                        
                        myAgent.send(request);
                        System.out.println(getLocalName() + " sent treatment request to scheduler");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    System.err.println(getLocalName() + " could not find scheduler agent");
                    // Retry after delay
                    myAgent.addBehaviour(new WakerBehaviour(myAgent, 3000) {
                        @Override
                        protected void onWake() {
                            myAgent.addBehaviour(new RequestTreatmentBehaviour());
                        }
                    });
                }
            } catch (FIPAException fe) {
                fe.printStackTrace();
            }
        }
    }
    
    /**
     * Behavior to wait for resource allocation from scheduler and handle status requests
     */
    private class WaitForAllocationBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            // Template for allocation messages
            MessageTemplate allocationTemplate = MessageTemplate.and(
                MessageTemplate.MatchConversationId(MessageProtocol.STATUS_UPDATE),
                MessageTemplate.MatchPerformative(ACLMessage.INFORM)
            );
            
            // Template for status request messages
            MessageTemplate statusRequestTemplate = MessageTemplate.and(
                MessageTemplate.MatchConversationId(MessageProtocol.STATUS_UPDATE),
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST)
            );
            
            // Combined template
            MessageTemplate mt = MessageTemplate.or(allocationTemplate, statusRequestTemplate);
            
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                if (msg.getPerformative() == ACLMessage.REQUEST) {
                    // Handle status request from monitoring agent
                    if (msg.getContent().equals("REQUEST_STATUS")) {
                        // Send patient data back to requester
                        ACLMessage reply = msg.createReply();
                        reply.setPerformative(ACLMessage.INFORM);
                        
                        try {
                            // Serialize patient data
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            ObjectOutputStream oos = new ObjectOutputStream(baos);
                            oos.writeObject(patientData);
                            reply.setByteSequenceContent(baos.toByteArray());
                            
                            myAgent.send(reply);
                            System.out.println(getLocalName() + " sent status update");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else if (msg.getPerformative() == ACLMessage.INFORM) {
                    // Handle allocation messages from scheduler
                    String content = msg.getContent();
                    if (content.startsWith("RESOURCES_ALLOCATED:")) {
                        String[] parts = content.split(":");
                        if (parts.length >= 3) {
                            String doctorId = parts[1];
                            String roomId = parts[2];
                            String equipmentId = parts.length > 3 ? parts[3] : "NONE";
                            
                            patientData.setAssignedDoctor(doctorId);
                            patientData.setAssignedRoom(roomId);
                            patientData.setRequiredEquipment(equipmentId);
                            patientData.setStatus("IN_TREATMENT");
                            
                            System.out.println(getLocalName() + " allocated resources - Doctor: " + 
                                doctorId + ", Room: " + roomId + ", Equipment: " + equipmentId);
                            
                            // Simulate treatment duration
                            long duration = SchedulingAlgorithm.estimateTreatmentDuration(patientData.getTreatmentType());
                            myAgent.addBehaviour(new WakerBehaviour(myAgent, duration) {
                                @Override
                                protected void onWake() {
                                    completeTreatment();
                                }
                            });
                        }
                    } else if (content.startsWith("ALLOCATION_FAILED:")) {
                        System.out.println(getLocalName() + " allocation failed, will retry later");
                        // Scheduler will handle retrying
                    }
                }
            } else {
                block();
            }
        }
    }
    
    /**
     * Complete treatment and notify scheduler
     */
    private void completeTreatment() {
        patientData.setStatus("COMPLETED");
        treatmentCompleted = true;
        
        // Notify scheduler
        ACLMessage complete = new ACLMessage(ACLMessage.INFORM);
        complete.addReceiver(schedulerAgent);
        complete.setConversationId(MessageProtocol.STATUS_UPDATE);
        complete.setOntology(MessageProtocol.HOSPITAL_ONTOLOGY);
        complete.setContent(MessageProtocol.TREATMENT_COMPLETE + ":" + patientData.getId());
        send(complete);
        
        // Notify monitoring agent
        notifyMonitor("PATIENT_TREATED:" + patientData.getId());
        notifyMonitor("WAIT_TIME:" + patientData.getWaitingTime() + ":" + 
                     patientData.getUrgencyLevel() + ":0.7");
        
        System.out.println(getLocalName() + " completed treatment. Total waiting time: " + 
            (patientData.getWaitingTime() / 1000) + " seconds");
        
        // Agent can terminate after treatment
        doDelete();
    }
    
    /**
     * Periodic behavior to update waiting time
     */
    private class UpdateStatusBehaviour extends TickerBehaviour {
        public UpdateStatusBehaviour(Agent a, long period) {
            super(a, period);
        }
        
        @Override
        protected void onTick() {
            if (!treatmentCompleted && patientData.getStatus().equals("WAITING")) {
                patientData.updateWaitingTime();
                System.out.println(getLocalName() + " waiting time: " + 
                    (patientData.getWaitingTime() / 1000) + " seconds");
                // Notify monitor about waiting status
                notifyMonitor("PATIENT_WAITING:" + (patientData.getWaitingTime() / 1000));
            }
        }
    }
    
    /**
     * Helper method to notify monitoring agent
     */
    private void notifyMonitor(String message) {
        // Find monitoring agent
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("monitoring-service");
        template.addServices(sd);
        
        try {
            DFAgentDescription[] result = DFService.search(this, template);
            if (result.length > 0) {
                ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
                inform.addReceiver(result[0].getName());
                inform.setContent(message);
                send(inform);
            }
        } catch (FIPAException fe) {
            // Monitoring agent might not be available yet
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
        System.out.println("Patient Agent " + getLocalName() + " terminating");
    }
}
