package agents;

import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import models.Equipment;
import utils.MessageProtocol;

/**
 * Equipment Agent - Manages medical equipment availability
 * Master IA - Systèmes Multi-Agents Project
 */
public class EquipmentAgent extends Agent {
    private Equipment equipmentData;
    
    @Override
    protected void setup() {
        // Get arguments: id, type
        Object[] args = getArguments();
        if (args != null && args.length >= 2) {
            String id = (String) args[0];
            String type = (String) args[1];
            
            equipmentData = new Equipment(id, type);
            System.out.println("Equipment Agent " + getLocalName() + " initialized: " + equipmentData);
            
            // Register in Yellow Pages
            registerInYellowPages();
            
            // Add behaviors
            addBehaviour(new HandleEquipmentRequestBehaviour());
            addBehaviour(new MaintenanceCheckBehaviour());
        } else {
            System.err.println("Equipment Agent requires arguments: id, type");
            doDelete();
        }
    }
    
    private void registerInYellowPages() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType(MessageProtocol.EQUIPMENT_SERVICE);
        sd.setName("Equipment-" + equipmentData.getType());
        sd.addProperties(new jade.domain.FIPAAgentManagement.Property("equipmentType", equipmentData.getType()));
        sd.addProperties(new jade.domain.FIPAAgentManagement.Property("equipmentId", equipmentData.getId()));
        dfd.addServices(sd);
        
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }
    
    /**
     * Behavior to handle equipment allocation requests
     */
    private class HandleEquipmentRequestBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                MessageTemplate.MatchConversationId(MessageProtocol.RESOURCE_ALLOCATION)
            );
            
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                ACLMessage reply = msg.createReply();
                String content = msg.getContent();
                
                if (content.startsWith(MessageProtocol.CHECK_AVAILABILITY)) {
                    // Check equipment availability
                    String[] parts = content.split(":");
                    boolean typeMatch = true;
                    
                    if (parts.length >= 2) {
                        String requestedType = parts[1];
                        typeMatch = requestedType.equals(equipmentData.getType());
                    }
                    
                    if (equipmentData.isAvailable() && typeMatch) {
                        reply.setPerformative(ACLMessage.CONFIRM);
                        reply.setContent(MessageProtocol.RESOURCE_AVAILABLE + ":" + 
                            equipmentData.getId() + ":" + equipmentData.getType());
                        System.out.println(getLocalName() + " is available");
                    } else {
                        reply.setPerformative(ACLMessage.REFUSE);
                        reply.setContent(MessageProtocol.RESOURCE_BUSY + ":" + equipmentData.getId());
                        if (!typeMatch) {
                            System.out.println(getLocalName() + " type mismatch");
                        } else {
                            System.out.println(getLocalName() + " is in use");
                        }
                    }
                } else if (content.startsWith(MessageProtocol.ALLOCATE_RESOURCE)) {
                    // Allocate equipment
                    String[] parts = content.split(":");
                    if (parts.length >= 2) {
                        String patientId = parts[1];
                        
                        if (equipmentData.isAvailable()) {
                            equipmentData.allocate(patientId);
                            reply.setPerformative(ACLMessage.CONFIRM);
                            reply.setContent("ALLOCATED:" + equipmentData.getId());
                            System.out.println(getLocalName() + " allocated to patient " + patientId + 
                                ". Total usage count: " + equipmentData.getUsageCount());
                        } else {
                            reply.setPerformative(ACLMessage.REFUSE);
                            reply.setContent("NOT_AVAILABLE:" + equipmentData.getId());
                        }
                    }
                } else if (content.startsWith(MessageProtocol.RELEASE_RESOURCE)) {
                    // Release equipment
                    equipmentData.release();
                    reply.setPerformative(ACLMessage.CONFIRM);
                    reply.setContent("RELEASED:" + equipmentData.getId());
                    System.out.println(getLocalName() + " released and now available");
                    
                    // Check if maintenance needed after certain usage
                    if (equipmentData.getUsageCount() % 10 == 0) {
                        System.out.println(getLocalName() + " requires maintenance check after " + 
                            equipmentData.getUsageCount() + " uses");
                    }
                }
                
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }
    
    /**
     * Periodic behavior to check maintenance requirements
     */
    private class MaintenanceCheckBehaviour extends TickerBehaviour {
        public MaintenanceCheckBehaviour() {
            super(myAgent, 60000); // Check every minute
        }
        
        @Override
        protected void onTick() {
            // Simulate maintenance requirements
            if (equipmentData.getUsageCount() > 0 && equipmentData.getUsageCount() % 20 == 0) {
                System.out.println("WARNING: " + getLocalName() + 
                    " requires scheduled maintenance after " + 
                    equipmentData.getUsageCount() + " uses");
                
                // Could temporarily make equipment unavailable for maintenance
                // For now, just log the warning
            }
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
        System.out.println("Equipment Agent " + getLocalName() + " terminating. Total usage: " + 
            equipmentData.getUsageCount());
    }
}
