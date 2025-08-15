package agents;

import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import models.Room;
import utils.MessageProtocol;

/**
 * Room Agent - Manages room availability and allocation
 * Master IA - Systèmes Multi-Agents Project
 */
public class RoomAgent extends Agent {
    private Room roomData;
    
    @Override
    protected void setup() {
        // Get arguments: id, type, capacity
        Object[] args = getArguments();
        if (args != null && args.length >= 3) {
            String id = (String) args[0];
            String type = (String) args[1];
            int capacity = Integer.parseInt(args[2].toString());
            
            roomData = new Room(id, type, capacity);
            System.out.println("Room Agent " + getLocalName() + " initialized: " + roomData);
            
            // Register in Yellow Pages
            registerInYellowPages();
            
            // Add behaviors
            addBehaviour(new HandleRoomRequestBehaviour());
            addBehaviour(new MonitorRoomStatusBehaviour());
        } else {
            System.err.println("Room Agent requires arguments: id, type, capacity");
            doDelete();
        }
    }
    
    private void registerInYellowPages() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType(MessageProtocol.ROOM_SERVICE);
        sd.setName("Room-" + roomData.getType());
        sd.addProperties(new jade.domain.FIPAAgentManagement.Property("roomType", roomData.getType()));
        sd.addProperties(new jade.domain.FIPAAgentManagement.Property("roomId", roomData.getId()));
        sd.addProperties(new jade.domain.FIPAAgentManagement.Property("capacity", String.valueOf(roomData.getCapacity())));
        dfd.addServices(sd);
        
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }
    
    /**
     * Behavior to handle room allocation requests
     */
    private class HandleRoomRequestBehaviour extends CyclicBehaviour {
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
                    // Check room availability
                    if (roomData.isAvailable()) {
                        reply.setPerformative(ACLMessage.CONFIRM);
                        reply.setContent(MessageProtocol.RESOURCE_AVAILABLE + ":" + 
                            roomData.getId() + ":" + roomData.getType());
                        System.out.println(getLocalName() + " is available");
                    } else {
                        reply.setPerformative(ACLMessage.REFUSE);
                        reply.setContent(MessageProtocol.RESOURCE_BUSY + ":" + roomData.getId());
                        System.out.println(getLocalName() + " is occupied");
                    }
                } else if (content.startsWith(MessageProtocol.ALLOCATE_RESOURCE)) {
                    // Allocate room
                    String[] parts = content.split(":");
                    if (parts.length >= 3) {
                        String patientId = parts[1];
                        String doctorId = parts[2];
                        
                        if (roomData.isAvailable()) {
                            roomData.occupy(patientId, doctorId);
                            reply.setPerformative(ACLMessage.CONFIRM);
                            reply.setContent("ALLOCATED:" + roomData.getId());
                            System.out.println(getLocalName() + " allocated to patient " + 
                                patientId + " with doctor " + doctorId);
                        } else {
                            reply.setPerformative(ACLMessage.REFUSE);
                            reply.setContent("NOT_AVAILABLE:" + roomData.getId());
                        }
                    }
                } else if (content.startsWith(MessageProtocol.RELEASE_RESOURCE)) {
                    // Release room
                    roomData.release();
                    reply.setPerformative(ACLMessage.CONFIRM);
                    reply.setContent("RELEASED:" + roomData.getId());
                    System.out.println(getLocalName() + " released and now available");
                }
                
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }
    
    /**
     * Behavior to monitor and handle room status updates
     */
    private class MonitorRoomStatusBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                MessageTemplate.MatchConversationId(MessageProtocol.STATUS_UPDATE)
            );
            
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String content = msg.getContent();
                
                if (content.startsWith("RELEASE_ROOM")) {
                    String[] parts = content.split(":");
                    if (parts.length >= 2 && parts[1].equals(roomData.getId())) {
                        roomData.release();
                        System.out.println(getLocalName() + " released after treatment completion");
                        
                        // Notify scheduler that room is available
                        notifySchedulerAvailable();
                    }
                }
            } else {
                block();
            }
        }
    }
    
    private void notifySchedulerAvailable() {
        // Find scheduler and notify availability
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType(MessageProtocol.SCHEDULER_SERVICE);
        template.addServices(sd);
        
        try {
            DFAgentDescription[] result = DFService.search(this, template);
            if (result.length > 0) {
                ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
                inform.addReceiver(result[0].getName());
                inform.setConversationId(MessageProtocol.STATUS_UPDATE);
                inform.setContent("ROOM_AVAILABLE:" + roomData.getId() + ":" + roomData.getType());
                send(inform);
            }
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
        System.out.println("Room Agent " + getLocalName() + " terminating");
    }
}
