package negotiation;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.io.Serializable;
import java.util.*;

/**
 * Contract Net Protocol implementation for resource negotiation
 * Agents can bid for resources based on their utility functions
 * Master IA - Systèmes Multi-Agents Project
 */
public class NegotiationProtocol {
    
    /**
     * Initiate a Contract Net Protocol negotiation
     */
    public static class ContractNetInitiator extends Behaviour {
        private Agent myAgent;
        private ResourceRequest request;
        private List<AID> participants;
        private Map<AID, Bid> receivedBids;
        private AID winner;
        private int step = 0;
        private long deadline;
        private MessageTemplate template;
        private String conversationId;
        
        public ContractNetInitiator(Agent agent, ResourceRequest request, List<AID> participants) {
            this.myAgent = agent;
            this.request = request;
            this.participants = new ArrayList<>(participants);
            this.receivedBids = new HashMap<>();
            this.deadline = System.currentTimeMillis() + 5000; // 5 second deadline
        }
        
        @Override
        public void action() {
            switch (step) {
                case 0:
                    // Send CFP (Call For Proposals) to all participants
                    sendCFP();
                    step = 1;
                    break;
                    
                case 1:
                    // Collect proposals/refusals
                    collectProposals();
                    if (allResponsesReceived() || deadlineReached()) {
                        step = 2;
                    }
                    break;
                    
                case 2:
                    // Evaluate proposals and select winner
                    evaluateProposals();
                    step = 3;
                    break;
                    
                case 3:
                    // Send accept/reject notifications
                    notifyParticipants();
                    step = 4;
                    break;
                    
                case 4:
                    // Wait for confirmation from winner
                    waitForConfirmation();
                    step = 5;
                    break;
            }
        }
        
        private void sendCFP() {
            ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
            conversationId = "resource-negotiation-" + System.currentTimeMillis();
            cfp.setConversationId(conversationId);
            cfp.setReplyWith("cfp" + System.currentTimeMillis());
            
            for (AID participant : participants) {
                cfp.addReceiver(participant);
            }
            
            try {
                cfp.setContentObject(request);
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            myAgent.send(cfp);
            template = MessageTemplate.and(
                MessageTemplate.MatchConversationId(conversationId),
                MessageTemplate.MatchInReplyTo(cfp.getReplyWith())
            );
            
            System.out.println(myAgent.getLocalName() + ": Sent CFP for " + request.getResourceType());
        }
        
        private void collectProposals() {
            ACLMessage reply = myAgent.receive(template);
            if (reply != null) {
                if (reply.getPerformative() == ACLMessage.PROPOSE) {
                    try {
                        Bid bid = (Bid) reply.getContentObject();
                        receivedBids.put(reply.getSender(), bid);
                        System.out.println(myAgent.getLocalName() + ": Received bid from " + 
                                         reply.getSender().getLocalName() + " - Utility: " + bid.getUtility());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (reply.getPerformative() == ACLMessage.REFUSE) {
                    System.out.println(myAgent.getLocalName() + ": " + 
                                     reply.getSender().getLocalName() + " refused to bid");
                }
            }
        }
        
        private void evaluateProposals() {
            if (receivedBids.isEmpty()) {
                System.out.println(myAgent.getLocalName() + ": No bids received");
                return;
            }
            
            // Select the bid with highest utility
            Map.Entry<AID, Bid> bestBid = receivedBids.entrySet().stream()
                .max(Map.Entry.comparingByValue(Comparator.comparing(Bid::getUtility)))
                .orElse(null);
            
            if (bestBid != null) {
                winner = bestBid.getKey();
                System.out.println(myAgent.getLocalName() + ": Winner is " + 
                                 winner.getLocalName() + " with utility " + bestBid.getValue().getUtility());
            }
        }
        
        private void notifyParticipants() {
            for (Map.Entry<AID, Bid> entry : receivedBids.entrySet()) {
                ACLMessage reply = new ACLMessage(
                    entry.getKey().equals(winner) ? ACLMessage.ACCEPT_PROPOSAL : ACLMessage.REJECT_PROPOSAL
                );
                reply.addReceiver(entry.getKey());
                reply.setConversationId(conversationId);
                myAgent.send(reply);
            }
        }
        
        private void waitForConfirmation() {
            ACLMessage confirm = myAgent.receive(
                MessageTemplate.and(
                    MessageTemplate.MatchSender(winner),
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM)
                )
            );
            
            if (confirm != null) {
                System.out.println(myAgent.getLocalName() + ": Resource allocated to " + 
                                 winner.getLocalName());
            }
        }
        
        private boolean allResponsesReceived() {
            return receivedBids.size() >= participants.size();
        }
        
        private boolean deadlineReached() {
            return System.currentTimeMillis() > deadline;
        }
        
        @Override
        public boolean done() {
            return step >= 5;
        }
        
        public AID getWinner() {
            return winner;
        }
    }
    
    /**
     * Contract Net Protocol responder behavior
     */
    public static class ContractNetResponder extends CyclicBehaviour {
        private Agent myAgent;
        private UtilityFunction utilityFunction;
        
        public ContractNetResponder(Agent agent, UtilityFunction utilityFunction) {
            this.myAgent = agent;
            this.utilityFunction = utilityFunction;
        }
        
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage cfp = myAgent.receive(mt);
            
            if (cfp != null) {
                try {
                    ResourceRequest request = (ResourceRequest) cfp.getContentObject();
                    
                    // Calculate utility for this request
                    double utility = utilityFunction.calculateUtility(request);
                    
                    ACLMessage reply = cfp.createReply();
                    
                    if (utility > 0) {
                        // Send proposal with bid
                        reply.setPerformative(ACLMessage.PROPOSE);
                        Bid bid = new Bid(myAgent.getAID(), utility, request);
                        reply.setContentObject(bid);
                        System.out.println(myAgent.getLocalName() + ": Bidding with utility " + utility);
                    } else {
                        // Refuse to bid
                        reply.setPerformative(ACLMessage.REFUSE);
                        System.out.println(myAgent.getLocalName() + ": Refusing to bid");
                    }
                    
                    myAgent.send(reply);
                    
                    // Wait for accept/reject
                    MessageTemplate resultTemplate = MessageTemplate.and(
                        MessageTemplate.MatchConversationId(cfp.getConversationId()),
                        MessageTemplate.or(
                            MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL),
                            MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL)
                        )
                    );
                    
                    ACLMessage result = myAgent.blockingReceive(resultTemplate, 5000);
                    
                    if (result != null && result.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                        // Won the negotiation
                        System.out.println(myAgent.getLocalName() + ": Won the negotiation!");
                        
                        // Send confirmation
                        ACLMessage confirm = result.createReply();
                        confirm.setPerformative(ACLMessage.INFORM);
                        confirm.setContent("Resource allocated");
                        myAgent.send(confirm);
                        
                        // Update agent state
                        handleWonNegotiation(request);
                    }
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                block();
            }
        }
        
        private void handleWonNegotiation(ResourceRequest request) {
            // Implementation specific to each agent type
            System.out.println(myAgent.getLocalName() + ": Handling won negotiation for " + 
                             request.getResourceType());
        }
    }
    
    /**
     * Resource request for negotiation
     */
    public static class ResourceRequest implements Serializable {
        private String resourceType;
        private int urgencyLevel;
        private long duration;
        private Map<String, Object> attributes;
        
        public ResourceRequest(String resourceType, int urgencyLevel, long duration) {
            this.resourceType = resourceType;
            this.urgencyLevel = urgencyLevel;
            this.duration = duration;
            this.attributes = new HashMap<>();
        }
        
        // Getters and setters
        public String getResourceType() { return resourceType; }
        public int getUrgencyLevel() { return urgencyLevel; }
        public long getDuration() { return duration; }
        public Map<String, Object> getAttributes() { return attributes; }
        
        public void addAttribute(String key, Object value) {
            attributes.put(key, value);
        }
    }
    
    /**
     * Bid in the negotiation
     */
    public static class Bid implements Serializable, Comparable<Bid> {
        private AID bidder;
        private double utility;
        private ResourceRequest request;
        private long timestamp;
        
        public Bid(AID bidder, double utility, ResourceRequest request) {
            this.bidder = bidder;
            this.utility = utility;
            this.request = request;
            this.timestamp = System.currentTimeMillis();
        }
        
        public AID getBidder() { return bidder; }
        public double getUtility() { return utility; }
        public ResourceRequest getRequest() { return request; }
        public long getTimestamp() { return timestamp; }
        
        @Override
        public int compareTo(Bid other) {
            return Double.compare(this.utility, other.utility);
        }
    }
    
    /**
     * Utility function interface for calculating bid values
     */
    public interface UtilityFunction {
        double calculateUtility(ResourceRequest request);
    }
    
    /**
     * Example utility function for doctors
     */
    public static class DoctorUtilityFunction implements UtilityFunction {
        private String specialization;
        private boolean available;
        private int currentWorkload;
        
        public DoctorUtilityFunction(String specialization, boolean available, int currentWorkload) {
            this.specialization = specialization;
            this.available = available;
            this.currentWorkload = currentWorkload;
        }
        
        @Override
        public double calculateUtility(ResourceRequest request) {
            if (!available) return 0;
            
            double utility = 100.0;
            
            // Specialization match bonus
            String requiredSpec = (String) request.getAttributes().get("specialization");
            if (requiredSpec != null && requiredSpec.equals(specialization)) {
                utility += 50;
            }
            
            // Urgency consideration
            utility += request.getUrgencyLevel() * 10;
            
            // Workload penalty
            utility -= currentWorkload * 5;
            
            // Duration consideration (prefer shorter treatments when busy)
            if (currentWorkload > 3) {
                utility -= request.getDuration() / 60000; // Penalty per minute
            }
            
            return Math.max(0, utility);
        }
    }
}
