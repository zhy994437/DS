package paxos;

/**
 * Immutable Paxos protocol message.
 * 
 * Message Types:
 * - PREPARE: Phase 1a - Proposer asks for permission to propose
 * - PROMISE: Phase 1b - Acceptor promises not to accept lower proposals
 * - ACCEPT_REQUEST: Phase 2a - Proposer asks acceptors to accept value
 * - ACCEPTED: Phase 2b - Acceptor confirms acceptance
 * - LEARN: Learning phase - Broadcast final consensus value
 * 
 * Wire Format: TYPE:SENDER:PROPOSAL_NUM:VALUE[:ACCEPTED_NUM:ACCEPTED_VAL]
 * 
 * Proposal Number Format: counter.memberId
 * - This ensures total ordering of proposals
 * - Ties are broken by member ID
 */
public class PaxosMessage {
    
    public enum Type {
        PREPARE,        // Phase 1a: Request to prepare a proposal
        PROMISE,        // Phase 1b: Promise not to accept lower proposals
        ACCEPT_REQUEST, // Phase 2a: Request to accept a specific value
        ACCEPTED,       // Phase 2b: Confirmation of acceptance
        LEARN           // Learning: Broadcast of consensus value
    }
    
    // Message type
    private final Type type;
    
    // ID of the sender
    private final String sender;
    
    // Proposal number (format: counter.memberId)
    private final String proposalNum;
    
    // Value being proposed or accepted
    private final String value;
    
    // For PROMISE messages: previously accepted proposal number
    private final String acceptedNum;
    
    // For PROMISE messages: previously accepted value
    private final String acceptedVal;
    
    /**
     * Construct a basic Paxos message.
     * Used for PREPARE, ACCEPT_REQUEST, ACCEPTED, and LEARN messages.
     */
    public PaxosMessage(Type type, String sender, String proposalNum, String value) {
        this(type, sender, proposalNum, value, null, null);
    }
    
    /**
     * Construct a Paxos message with accepted value information.
     * Used for PROMISE messages that include previously accepted values.
     */
    public PaxosMessage(Type type, String sender, String proposalNum, String value,
                       String acceptedNum, String acceptedVal) {
        this.type = type;
        this.sender = sender;
        this.proposalNum = proposalNum;
        this.value = value;
        this.acceptedNum = acceptedNum;
        this.acceptedVal = acceptedVal;
    }
    
    /**
     * Serialize message to string format for network transmission.
     * Format: TYPE:SENDER:PROPOSAL_NUM:VALUE[:ACCEPTED_NUM:ACCEPTED_VAL]
     * 
     * @return Serialized message string
     */
    public String serialize() {
        StringBuilder sb = new StringBuilder()
            .append(type).append(":")
            .append(sender).append(":")
            .append(proposalNum != null ? proposalNum : "").append(":")
            .append(value != null ? value : "");
        
        // Include accepted value info if present (for PROMISE messages)
        if (acceptedNum != null && acceptedVal != null) {
            sb.append(":").append(acceptedNum).append(":").append(acceptedVal);
        }
        
        return sb.toString();
    }
    
    /**
     * Deserialize message from string format.
     * 
     * @param msg Serialized message string
     * @return Deserialized PaxosMessage object
     * @throws IllegalArgumentException if message format is invalid
     */
    public static PaxosMessage deserialize(String msg) {
        // Use -1 limit to preserve empty trailing fields
        String[] parts = msg.split(":", -1);
        
        if (parts.length < 4) {
            throw new IllegalArgumentException("Invalid message format: " + msg);
        }
        
        Type type = Type.valueOf(parts[0]);
        String sender = parts[1];
        String proposalNum = parts[2].isEmpty() ? null : parts[2];
        String value = parts[3].isEmpty() ? null : parts[3];
        
        // Check if message includes accepted value info
        if (parts.length >= 6) {
            String acceptedNum = parts[4].isEmpty() ? null : parts[4];
            String acceptedVal = parts[5].isEmpty() ? null : parts[5];
            return new PaxosMessage(type, sender, proposalNum, value, 
                acceptedNum, acceptedVal);
        }
        
        return new PaxosMessage(type, sender, proposalNum, value);
    }
    
    /**
     * Compare two proposal numbers to determine ordering.
     * Proposal format: counter.memberId
     * 
     * Comparison rules:
     * 1. Higher counter wins
     * 2. If counters equal, higher member ID wins
     * 3. null is treated as lowest value
     * 
     * @param p1 First proposal number
     * @param p2 Second proposal number
     * @return negative if p1 < p2, zero if p1 == p2, positive if p1 > p2
     */
    public static int compareProposals(String p1, String p2) {
        // Handle null cases
        if (p1 == null && p2 == null) return 0;
        if (p1 == null) return -1;
        if (p2 == null) return 1;
        
        try {
            // Parse proposal numbers: counter.memberId
            String[] parts1 = p1.split("\\.");
            String[] parts2 = p2.split("\\.");
            
            int counter1 = Integer.parseInt(parts1[0]);
            int counter2 = Integer.parseInt(parts2[0]);
            
            // Compare by counter first
            if (counter1 != counter2) {
                return Integer.compare(counter1, counter2);
            }
            
            // If counters are equal, compare by member ID (tie-breaker)
            int id1 = Integer.parseInt(parts1[1]);
            int id2 = Integer.parseInt(parts2[1]);
            return Integer.compare(id1, id2);
            
        } catch (Exception e) {
            // Fallback to string comparison if parsing fails
            return p1.compareTo(p2);
        }
    }
    
    // ========== Getters ==========
    
    public Type getType() { 
        return type; 
    }
    
    public String getSender() { 
        return sender; 
    }
    
    public String getProposalNum() { 
        return proposalNum; 
    }
    
    public String getValue() { 
        return value; 
    }
    
    public String getAcceptedNum() { 
        return acceptedNum; 
    }
    
    public String getAcceptedVal() { 
        return acceptedVal; 
    }
    
    /**
     * Check if this message contains accepted value information.
     * Only PROMISE messages should have this.
     * 
     * @return true if message has accepted value info
     */
    public boolean hasAccepted() { 
        return acceptedNum != null && acceptedVal != null; 
    }
    
    @Override
    public String toString() {
        return String.format("PaxosMessage[type=%s, sender=%s, proposal=%s, value=%s]",
            type, sender, proposalNum, value);
    }
}
