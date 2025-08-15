package agents;

import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import models.Doctor;
import utils.MessageProtocol;
import utils.SchedulingAlgorithm;

/**
 * Doctor Agent - Manages doctor availability and treats patients
 * Master IA - Systèmes Multi-Agents Project
 */
public class DoctorAgent extends Agent {
    private Doctor doctorData;
    private long treatmentStartTime;
    
    @Override
    protected void setup() {
        // Get arguments: id, name, specialization
        Object[] args = getArguments();
        if (args != null && args.length >= 3) {
            String id = (String) args[0];
            String name = (String) args[1];
            String specialization = (String) args[2];
            
            doctorData = new Doctor(id, name, specialization);
            System.out.println("Doctor Agent " + getLocalName() + " initialized: " + doctorData);
            
            // Register in Yellow Pages
            registerInYellowPages();
            
            // Add behaviors
            addBehaviour(new HandlePatientAssignmentBehaviour());
            addBehaviour(new UpdateAvailabilityBehaviour(this));
        } else {
            System.err.println("Doctor Agent requires arguments: id, name, specialization");
            doDelete();
        }
    }
    
    private void registerInYellowPages() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType(MessageProtocol.DOCTOR_SERVICE);
        sd.setName("Doctor-" + doctorData.getSpecialization());
        sd.addProperties(new jade.domain.FIPAAgentManagement.Property("specialization", doctorData.getSpecialization()));
        sd.addProperties(new jade.domain.FIPAAgentManagement.Property("doctorId", doctorData.getId()));
        dfd.addServices(sd);
        
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }
    
    /**
     * Behavior to handle patient assignment requests from scheduler
     */
    private class HandlePatientAssignmentBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                MessageTemplate.MatchConversationId(MessageProtocol.RESOURCE_ALLOCATION)
            );
            
            ACLMessage msg = DoctorAgent.this.receive(mt);
            if (msg != null) {
                ACLMessage reply = msg.createReply();
                
                if (msg.getContent().startsWith(MessageProtocol.CHECK_AVAILABILITY)) {
                    // Check availability
                    if (doctorData.isAvailable()) {
                        reply.setPerformative(ACLMessage.CONFIRM);
                        reply.setContent(MessageProtocol.RESOURCE_AVAILABLE + ":" + doctorData.getId());
                        System.out.println(getLocalName() + " is available");
                    } else {
                        reply.setPerformative(ACLMessage.REFUSE);
                        reply.setContent(MessageProtocol.RESOURCE_BUSY + ":" + doctorData.getId());
                        System.out.println(getLocalName() + " is busy");
                    }
                } else if (msg.getContent().startsWith(MessageProtocol.ASSIGN_PATIENT)) {
                    // Assign patient to this doctor
                    String[] parts = msg.getContent().split(":");
                    if (parts.length >= 3) {
                        String patientId = parts[1];
                        String treatmentType = parts[2];
                        
                        if (doctorData.isAvailable() && 
                            SchedulingAlgorithm.isDoctorSuitable(doctorData.getSpecialization(), treatmentType)) {
                            
                            doctorData.setAvailable(false);
                            doctorData.setCurrentPatientId(patientId);
                            treatmentStartTime = System.currentTimeMillis();
                            
                            reply.setPerformative(ACLMessage.CONFIRM);
                            reply.setContent("ASSIGNED:" + doctorData.getId());
                            
                            System.out.println(getLocalName() + " assigned to patient " + patientId + 
                                " for " + treatmentType);
                            
                            // Schedule treatment completion
                            long duration = SchedulingAlgorithm.estimateTreatmentDuration(treatmentType);
                            myAgent.addBehaviour(new WakerBehaviour(myAgent, duration) {
                                @Override
                                protected void onWake() {
                                    completeTreatment();
                                }
                            });
                        } else {
                            reply.setPerformative(ACLMessage.REFUSE);
                            reply.setContent("NOT_AVAILABLE:" + doctorData.getId());
                        }
                    }
                }
                
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }
    
    /**
     * Complete treatment and update statistics
     */
    private void completeTreatment() {
        if (doctorData.getCurrentPatientId() != null) {
            long treatmentDuration = System.currentTimeMillis() - treatmentStartTime;
            doctorData.addServiceTime(treatmentDuration);
            doctorData.incrementPatientsServed();
            
            System.out.println(getLocalName() + " completed treatment for patient " + 
                doctorData.getCurrentPatientId() + ". Duration: " + (treatmentDuration / 1000) + " seconds");
            
            // Notify scheduler
            ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
            // Find scheduler
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType(MessageProtocol.SCHEDULER_SERVICE);
            template.addServices(sd);
            
            try {
                DFAgentDescription[] result = DFService.search(DoctorAgent.this, template);
                if (result.length > 0) {
                    inform.addReceiver(result[0].getName());
                    inform.setConversationId(MessageProtocol.STATUS_UPDATE);
                    inform.setContent("DOCTOR_AVAILABLE:" + doctorData.getId());
                    send(inform);
                }
            } catch (FIPAException fe) {
                fe.printStackTrace();
            }
            
            // Make doctor available again
            doctorData.setAvailable(true);
            doctorData.setCurrentPatientId(null);
            
            System.out.println(getLocalName() + " stats - Patients served: " + 
                doctorData.getPatientsServed() + ", Avg service time: " + 
                (doctorData.getAverageServiceTime() / 1000) + " seconds");
        }
    }
    
    /**
     * Periodic behavior to update availability status
     */
    private class UpdateAvailabilityBehaviour extends TickerBehaviour {
        public UpdateAvailabilityBehaviour(Agent a) {
            super(a, 10000); // Update every 10 seconds
        }
        
        @Override
        protected void onTick() {
            // Could implement more complex availability logic here
            // For example, scheduled breaks, shift changes, etc.
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
        System.out.println("Doctor Agent " + getLocalName() + " terminating. Final stats: " + doctorData);
    }
}
