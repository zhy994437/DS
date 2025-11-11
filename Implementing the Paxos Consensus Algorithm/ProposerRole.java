package paxos;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Proposer role implementation for Paxos consensus algorithm.
 * Handles proposal initiation and coordination of the two-phase protocol.
 */
public class ProposerRole {
    
    private final String memberId;
    private final NetworkManager network;
    private final AtomicInteger proposalCounter = new AtomicInteger(0);
    
    private volatile String currentProposal;
    private volatile String currentValue;
    private volatile String highestAcceptedInPromises;
    private volatile boolean proposing = false;
    
    private final Set<String> promises = new HashSet<>();
    private final Set<String> accepts = new HashSet<>();
    
    /**
     * Constructor for ProposerRole.
     * 
     * @param memberId ID of the council member
     * @param network Network manager for member count
     */
    public ProposerRole(String memberId, NetworkManager network) {
        this.memberId = memberId;
        this.network = network;
    }
    
    /**
     * Initiate a new proposal for a candidate.
     * 
     * @param candidate Candidate name to propose
     * @param hasCrashed Whether the node is crashed
     * @param learned Current learned value
     * @param sender Message sender interface
     * @return true if proposal initiated successfully
     */
    public boolean propose(String candidate, boolean hasCrashed, String learned,
                          MessageSender sender) {
        if (hasCrashed) {
            System.out.println(memberId + " cannot propose (crashed)");
            return false;
        }
        
        if (learned != null) {
            System.out.println(memberId + " - consensus already reached: " + learned);
            return false;
        }
        
        synchronized (this) {
            if (proposing) {
                System.out.println(memberId + " - already proposing");
                return false;
            }
            
            int counter = proposalCounter.incrementAndGet();
            int memberNum = Integer.parseInt(memberId.substring(1));
            currentProposal = counter + "." + memberNum;
            currentValue = candidate;
            proposing = true;
            highestAcceptedInPromises = null;
            
            promises.clear();
            accepts.clear();
            
            System.out.println(memberId + " proposing " + candidate + " with " + currentProposal);
        }
        
        PaxosMessage prepareMsg = new PaxosMessage(
            PaxosMessage.Type.PREPARE, memberId, currentProposal, null);
        sender.broadcast(prepareMsg);
        
        return true;
    }
    
    /**
     * Handle PROMISE response from acceptors.
     * 
     * @param msg PROMISE message
     * @param sender Message sender interface
     */
    public void handlePromise(PaxosMessage msg, MessageSender sender) {
        synchronized (this) {
            if (!proposing || !currentProposal.equals(msg.getProposalNum())) {
                return;
            }
            
            promises.add(msg.getSender());
            
            if (msg.hasAccepted()) {
                if (highestAcceptedInPromises == null || 
                    PaxosMessage.compareProposals(msg.getAcceptedNum(), highestAcceptedInPromises) > 0) {
                    highestAcceptedInPromises = msg.getAcceptedNum();
                    currentValue = msg.getAcceptedVal();
                    System.out.println(memberId + " adopting value from higher proposal " + 
                        msg.getAcceptedNum() + ": " + currentValue);
                }
            }
            
            int total = network.getMemberCount();
            int majority = total / 2 + 1;
            
            if (promises.size() == majority) {
                System.out.println(memberId + " got majority promises (" + 
                    promises.size() + "/" + total + ")");
                sendAcceptRequest(sender);
            }
        }
    }
    
    /**
     * Send ACCEPT_REQUEST to all acceptors.
     * 
     * @param sender Message sender interface
     */
    private void sendAcceptRequest(MessageSender sender) {
        PaxosMessage acceptMsg = new PaxosMessage(
            PaxosMessage.Type.ACCEPT_REQUEST, memberId, currentProposal, currentValue);
        sender.broadcast(acceptMsg);
    }
    
    /**
     * Handle ACCEPTED response from acceptors.
     * 
     * @param msg ACCEPTED message
     * @param sender Message sender interface
     */
    public void handleAccepted(PaxosMessage msg, MessageSender sender) {
        synchronized (this) {
            if (!proposing || !currentProposal.equals(msg.getProposalNum())) {
                return;
            }
            
            accepts.add(msg.getSender());
            
            int total = network.getMemberCount();
            int majority = total / 2 + 1;
            
            if (accepts.size() == majority) {
                System.out.println(memberId + " achieved consensus! " + currentValue);
                
                PaxosMessage learnMsg = new PaxosMessage(
                    PaxosMessage.Type.LEARN, memberId, currentProposal, currentValue);
                sender.broadcast(learnMsg);
                
                proposing = false;
            }
        }
    }
    
    /**
     * Reset proposer state.
     */
    public void reset() {
        synchronized (this) {
            proposing = false;
            currentProposal = null;
            currentValue = null;
            highestAcceptedInPromises = null;
            promises.clear();
            accepts.clear();
        }
    }
    
    /**
     * Get current state information.
     * 
     * @return State description
     */
    public String getStateInfo() {
        synchronized (this) {
            return String.format("Proposing:%s", 
                proposing ? currentProposal : "No");
        }
    }
}