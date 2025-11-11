package paxos;

/**
 * Learner role implementation for Paxos consensus algorithm.
 * Learns and stores the final consensus value.
 */
public class LearnerRole {
    
    private final String memberId;
    
    private volatile String learned;
    private volatile String learnedProposal;
    
    /**
     * Constructor for LearnerRole.
     * 
     * @param memberId ID of the council member
     */
    public LearnerRole(String memberId) {
        this.memberId = memberId;
    }
    
    /**
     * Handle LEARN message with final consensus value.
     * 
     * @param msg LEARN message
     */
    public void handleLearn(PaxosMessage msg) {
        synchronized (this) {
            if (learned == null) {
                learned = msg.getValue();
                learnedProposal = msg.getProposalNum();
                System.out.println("CONSENSUS: " + learned + " elected as Council President!");
                System.out.println(memberId + " learned consensus from " + msg.getSender());
            }
        }
    }
    
    /**
     * Get learned consensus value.
     * 
     * @return Consensus value or null if not learned
     */
    public String getLearned() {
        return learned;
    }
    
    /**
     * Check if consensus has been learned.
     * 
     * @return true if consensus learned
     */
    public boolean hasLearned() {
        return learned != null;
    }
    
    /**
     * Reset learner state.
     */
    public void reset() {
        synchronized (this) {
            learned = null;
            learnedProposal = null;
        }
    }
    
    /**
     * Get current state information.
     * 
     * @return State description
     */
    public String getStateInfo() {
        synchronized (this) {
            return String.format("Learned:%s", 
                learned != null ? learned : "None");
        }
    }
}