package paxos;

import java.util.*;

/**
 * Council member implementing all three Paxos roles through delegation.
 * This class coordinates between Proposer, Acceptor, and Learner roles.
 * 
 * The member can act as:
 * - Proposer: Initiates proposals for candidates
 * - Acceptor: Votes on proposals from proposers
 * - Learner: Learns the final consensus value
 */
public class CouncilMember implements MessageSender {
    
    private static final int BASE_PORT = 9000;
    private static final int CAFE_CONNECTION_PROBABILITY = 20;
    private static final int CAFE_MAX_DELAY_MS = 100;
    
    private final String id;
    private final NetworkManager network;
    private final Random random = new Random();
    
    private NetworkProfile profile;
    private volatile boolean hasCrashed = false;
    
    private final ProposerRole proposer;
    private final AcceptorRole acceptor;
    private final LearnerRole learner;
    
    /**
     * Network behavior profiles for simulating different network conditions.
     */
    public enum NetworkProfile {
        RELIABLE(0, 50, 0.0, 0.0),
        LATENT(1000, 3000, 0.15, 0.05),
        FAILURE(500, 1500, 0.30, 0.20),
        STANDARD(100, 500, 0.05, 0.01);
        
        private final int minDelayMs;
        private final int maxDelayMs;
        private final double messageDropRate;
        private final double crashRate;
        
        NetworkProfile(int minDelayMs, int maxDelayMs, double messageDropRate, double crashRate) {
            this.minDelayMs = minDelayMs;
            this.maxDelayMs = maxDelayMs;
            this.messageDropRate = messageDropRate;
            this.crashRate = crashRate;
        }
        
        public int getMinDelay() { return minDelayMs; }
        public int getMaxDelay() { return maxDelayMs; }
        public double getMessageDropRate() { return messageDropRate; }
        public double getCrashRate() { return crashRate; }
    }
    
    /**
     * Constructor with default profile.
     * 
     * @param id Member ID (e.g., "M1")
     * @param port Port number for this member
     */
    public CouncilMember(String id, int port) {
        this(id, port, NetworkProfile.STANDARD);
    }
    
    /**
     * Constructor with custom profile.
     * 
     * @param id Member ID (e.g., "M1")
     * @param port Port number for this member
     * @param profile Network behavior profile
     */
    public CouncilMember(String id, int port, NetworkProfile profile) {
        this.id = id;
        this.network = new NetworkManager(id, port, this::handleMessage);
        this.profile = profile;
        
        this.proposer = new ProposerRole(id, network);
        this.acceptor = new AcceptorRole(id);
        this.learner = new LearnerRole(id);
        
        System.out.println(id + " initialized with " + profile + " profile");
    }
    
    /**
     * Start the member and load network configuration.
     * 
     * @param configFile Path to network configuration file
     * @throws Exception if startup fails
     */
    public void start(String configFile) throws Exception {
        network.loadConfig(configFile);
        network.start();
        System.out.println(id + " started");
    }
    
    /**
     * Stop the member and cleanup resources.
     */
    public void stop() {
        network.stop();
        System.out.println(id + " stopped");
    }
    
    /**
     * Change network profile at runtime.
     * 
     * @param newProfile New network profile to use
     */
    public void setProfile(NetworkProfile newProfile) {
        this.profile = newProfile;
        System.out.println(id + " switched to " + newProfile + " profile");
    }
    
    /**
     * Simulate node crash/failure.
     */
    public void simulateCrash() {
        hasCrashed = true;
        System.out.println(id + " has CRASHED!");
    }
    
    /**
     * Recover from crash.
     */
    public void recover() {
        hasCrashed = false;
        System.out.println(id + " has RECOVERED");
    }
    
    /**
     * Propose a candidate for council president.
     * Delegates to ProposerRole.
     * 
     * @param candidate Candidate name to propose
     * @return true if proposal initiated successfully
     */
    public boolean propose(String candidate) {
        return proposer.propose(candidate, hasCrashed, learner.getLearned(), this);
    }
    
    /**
     * Main message dispatcher. Routes messages to appropriate role handlers.
     * 
     * @param msg Incoming message
     */
    private void handleMessage(PaxosMessage msg) {
        if (hasCrashed) {
            return;
        }
        
        switch (msg.getType()) {
            case PREPARE:
                acceptor.handlePrepare(msg, hasCrashed, this);
                break;
            case PROMISE:
                proposer.handlePromise(msg, this);
                break;
            case ACCEPT_REQUEST:
                acceptor.handleAcceptRequest(msg, hasCrashed, this);
                break;
            case ACCEPTED:
                proposer.handleAccepted(msg, this);
                break;
            case LEARN:
                learner.handleLearn(msg);
                break;
        }
    }
    
    /**
     * Send message to a specific target with network simulation.
     * Implementation of MessageSender interface.
     * 
     * @param target Target member ID
     * @param msg Message to send
     * @return true if sent successfully
     */
    @Override
    public boolean sendTo(String target, PaxosMessage msg) {
        return sendWithSimulation(target, msg);
    }
    
    /**
     * Broadcast message to all members with network simulation.
     * Implementation of MessageSender interface.
     * 
     * @param msg Message to broadcast
     * @return Number of successful sends
     */
    @Override
    public int broadcast(PaxosMessage msg) {
        return broadcastWithSimulation(msg);
    }
    
    /**
     * Simulate network delay based on profile.
     */
    private void simulateNetworkDelay() {
        if (profile == NetworkProfile.RELIABLE) {
            return;
        }
        
        int delay = profile.getMinDelay() + 
                   random.nextInt(profile.getMaxDelay() - profile.getMinDelay() + 1);
        
        if (profile == NetworkProfile.LATENT && random.nextInt(100) < CAFE_CONNECTION_PROBABILITY) {
            delay = random.nextInt(CAFE_MAX_DELAY_MS);
            System.out.println(id + " found good connection at Sheoak Cafe!");
        }
        
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Check if message should be dropped (lost in network).
     * 
     * @return true if message should be dropped
     */
    private boolean shouldDropMessage() {
        if (random.nextDouble() < profile.getMessageDropRate()) {
            System.out.println(id + " dropping message due to network issues");
            return true;
        }
        return false;
    }
    
    /**
     * Check if node should crash randomly.
     */
    private void checkRandomCrash() {
        if (!hasCrashed && random.nextDouble() < profile.getCrashRate()) {
            simulateCrash();
        }
    }
    
    /**
     * Send message with network simulation (internal implementation).
     * 
     * @param target Target member ID
     * @param msg Message to send
     * @return true if sent successfully
     */
    private boolean sendWithSimulation(String target, PaxosMessage msg) {
        if (hasCrashed) {
            System.out.println(id + " cannot send (crashed)");
            return false;
        }
        
        checkRandomCrash();
        if (hasCrashed) {
            return false;
        }
        
        simulateNetworkDelay();
        
        if (shouldDropMessage()) {
            return false;
        }
        
        return network.send(target, msg);
    }
    
    /**
     * Broadcast message with network simulation (internal implementation).
     * 
     * @param msg Message to broadcast
     * @return Number of successful sends
     */
    private int broadcastWithSimulation(PaxosMessage msg) {
        if (hasCrashed) {
            System.out.println(id + " cannot broadcast (crashed)");
            return 0;
        }
        
        checkRandomCrash();
        if (hasCrashed) {
            return 0;
        }
        
        int successCount = 0;
        for (String target : network.getMemberIds()) {
            if (!target.equals(id) && sendWithSimulation(target, msg)) {
                successCount++;
            }
        }
        return successCount;
    }
    
    /**
     * Get member ID.
     * 
     * @return Member ID
     */
    public String getId() { 
        return id; 
    }
    
    /**
     * Get learned consensus value.
     * 
     * @return Consensus value or null if not learned
     */
    public String getLearned() { 
        return learner.getLearned(); 
    }
    
    /**
     * Check if consensus has been learned.
     * 
     * @return true if consensus learned
     */
    public boolean hasLearned() { 
        return learner.hasLearned(); 
    }
    
    /**
     * Check if member has crashed.
     * 
     * @return true if crashed
     */
    public boolean hasCrashed() {
        return hasCrashed;
    }
    
    /**
     * Reset all state to initial values.
     * Used for testing multiple scenarios.
     */
    public void reset() {
        synchronized (this) {
            proposer.reset();
            acceptor.reset();
            learner.reset();
            hasCrashed = false;
        }
    }
    
    /**
     * Get current state as string.
     * Useful for debugging and monitoring.
     * 
     * @return State description
     */
    public String getState() {
        synchronized (this) {
            return String.format("%s [Profile:%s, Crashed:%s, %s, %s, %s]",
                id, 
                profile,
                hasCrashed,
                proposer.getStateInfo(),
                acceptor.getStateInfo(),
                learner.getStateInfo());
        }
    }
    
    /**
     * Parse command line arguments and return profile.
     * 
     * @param args Command line arguments
     * @return NetworkProfile parsed from args
     */
    private static NetworkProfile parseProfile(String[] args) {
        for (int i = 1; i < args.length; i++) {
            if ("--profile".equals(args[i]) && i + 1 < args.length) {
                try {
                    return NetworkProfile.valueOf(args[i + 1].toUpperCase());
                } catch (IllegalArgumentException e) {
                    System.err.println("Unknown profile: " + args[i + 1]);
                    System.err.println("Valid profiles: reliable, latent, failure, standard");
                    System.exit(1);
                }
            }
        }
        return NetworkProfile.STANDARD;
    }
    
    /**
     * Parse config file path from command line arguments.
     * 
     * @param args Command line arguments
     * @return Config file path
     */
    private static String parseConfig(String[] args) {
        for (int i = 1; i < args.length; i++) {
            if ("--config".equals(args[i]) && i + 1 < args.length) {
                return args[i + 1];
            }
        }
        return "network.config";
    }
    
    /**
     * Main entry point for running a council member.
     * 
     * @param args Command line arguments
     * @throws Exception if startup fails
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: java CouncilMember <memberId> [--profile <profile>] [--config <configFile>]");
            System.err.println("Profiles: reliable, latent, failure, standard (default)");
            System.exit(1);
        }
        
        String memberId = args[0];
        NetworkProfile profile = parseProfile(args);
        String config = parseConfig(args);
        
        int port = BASE_PORT + Integer.parseInt(memberId.substring(1));
        CouncilMember member = new CouncilMember(memberId, port, profile);
        
        member.start(config);
        
        System.out.println(memberId + " ready. Commands: <candidate>, profile <name>, crash, recover, status, quit");
        
        try (Scanner sc = new Scanner(System.in)) {
            while (true) {
                String input = sc.nextLine().trim();
                
                if ("quit".equalsIgnoreCase(input)) {
                    break;
                } else if ("status".equalsIgnoreCase(input)) {
                    System.out.println(member.getState());
                } else if ("crash".equalsIgnoreCase(input)) {
                    member.simulateCrash();
                } else if ("recover".equalsIgnoreCase(input)) {
                    member.recover();
                } else if (input.startsWith("profile ")) {
                    String profileName = input.substring(8).trim();
                    try {
                        NetworkProfile newProfile = NetworkProfile.valueOf(profileName.toUpperCase());
                        member.setProfile(newProfile);
                    } catch (IllegalArgumentException e) {
                        System.err.println("Unknown profile: " + profileName);
                    }
                } else if (!input.isEmpty()) {
                    member.propose(input);
                }
            }
        } finally {
            member.stop();
        }
    }
}