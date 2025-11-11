package paxos;

/**
 * Interface for sending messages with network simulation.
 * Allows dependency injection for testing and modularity.
 */
public interface MessageSender {
    
    /**
     * Send message to a specific target.
     * 
     * @param target Target member ID
     * @param msg Message to send
     * @return true if sent successfully
     */
    boolean sendTo(String target, PaxosMessage msg);
    
    /**
     * Broadcast message to all members.
     * 
     * @param msg Message to broadcast
     * @return Number of successful sends
     */
    int broadcast(PaxosMessage msg);
}