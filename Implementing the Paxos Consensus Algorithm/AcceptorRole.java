package paxos;

/**
 * Acceptor role implementation for Paxos consensus algorithm.
 * Handles promise and accept requests according to Paxos rules.
 */
public class AcceptorRole {
    
    private final String memberId;
    
    private volatile String highestPromised;
    private volatile String highestAccepted;
    private volatile String acceptedValue;
    
    /**
     * Constructor for AcceptorRole.
     * 
     * @param memberId ID of the council member
     */
    public AcceptorRole(String memberId) {
        this.memberId = memberId;
    }
    
    /**
     * Handle PREPARE request from proposer.
     * Simplified logic: respond with promise if proposal number is higher.
     * 
     * @param msg PREPARE message
     * @param hasCrashed Whether the node is crashed
     * @param sender Message sender interface
     */
    public void handlePrepare(PaxosMessage msg, boolean hasCrashed, 
                             MessageSender sender) {
        if (hasCrashed) {
            return;
        }
        
        String proposalNum = msg.getProposalNum();
        
        synchronized (this) {
            if (highestPromised == null || 
                PaxosMessage.compareProposals(proposalNum, highestPromised) > 0) {
                
                highestPromised = proposalNum;
                
                PaxosMessage response;
                if (highestAccepted != null) {
                    response = new PaxosMessage(
                        PaxosMessage.Type.PROMISE, memberId, proposalNum, null,
                        highestAccepted, acceptedValue);
                } else {
                    response = new PaxosMessage(
                        PaxosMessage.Type.PROMISE, memberId, proposalNum, null);
                }
                
                System.out.println(memberId + " promising " + proposalNum);
                sender.sendTo(msg.getSender(), response);
            } else {
                System.out.println(memberId + " ignoring lower prepare " + proposalNum + 
                    " (promised " + highestPromised + ")");
            }
        }
    }
    
    /**
     * Handle ACCEPT_REQUEST from proposer.
     * 
     * @param msg ACCEPT_REQUEST message
     * @param hasCrashed Whether the node is crashed
     * @param sender Message sender interface
     */
    public void handleAcceptRequest(PaxosMessage msg, boolean hasCrashed,
                                   MessageSender sender) {
        if (hasCrashed) {
            return;
        }
        
        String proposalNum = msg.getProposalNum();
        
        synchronized (this) {
            if (highestPromised == null || 
                PaxosMessage.compareProposals(proposalNum, highestPromised) >= 0) {
                
                highestPromised = proposalNum;
                highestAccepted = proposalNum;
                acceptedValue = msg.getValue();
                
                PaxosMessage response = new PaxosMessage(
                    PaxosMessage.Type.ACCEPTED, memberId, proposalNum, acceptedValue);
                
                System.out.println(memberId + " accepting " + proposalNum + " = " + acceptedValue);
                sender.sendTo(msg.getSender(), response);
            } else {
                System.out.println(memberId + " rejecting accept " + proposalNum + 
                    " (promised " + highestPromised + ")");
            }
        }
    }
    
    /**
     * Reset acceptor state.
     */
    public void reset() {
        synchronized (this) {
            highestPromised = null;
            highestAccepted = null;
            acceptedValue = null;
        }
    }
    
    /**
     * Get current state information.
     * 
     * @return State description
     */
    public String getStateInfo() {
        synchronized (this) {
            return String.format("Promised:%s, Accepted:%s->%s",
                highestPromised != null ? highestPromised : "None",
                highestAccepted != null ? highestAccepted : "None",
                acceptedValue != null ? acceptedValue : "None");
        }
    }
}