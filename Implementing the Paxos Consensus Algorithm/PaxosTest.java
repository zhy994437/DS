package paxos;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Test suite for Paxos implementation covering all assignment scenarios.
 * Scenario 1: Ideal Network
 * Scenario 2: Concurrent Proposals
 * Scenario 3: Fault-Tolerance (3a, 3b, 3c)
 */
public class PaxosTest {
    
    private static final int MEMBERS = 9;
    private static final String CONFIG = "test.config";
    private static final int CONSENSUS_TIMEOUT_SHORT = 10000;
    private static final int CONSENSUS_TIMEOUT_MEDIUM = 20000;
    private static final int CONSENSUS_TIMEOUT_LONG = 30000;
    private static final int SETUP_DELAY_MS = 2000;
    private static final int CRASH_DELAY_MS = 2000;
    private static final int RECOVERY_DELAY_MS = 1000;
    private static final int RESET_DELAY_MS = 1000;
    
    private final List<CouncilMember> members = new ArrayList<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(12);
    
    /**
     * Setup with custom profiles for each member.
     * Allows testing different network conditions.
     * 
     * @param profiles Network profiles for each member
     * @throws Exception if setup fails
     */
    public void setup(CouncilMember.NetworkProfile... profiles) throws Exception {
        System.out.println("Setting up test environment...");
        
        // Create config file
        try (PrintWriter w = new PrintWriter(new FileWriter(CONFIG))) {
            for (int i = 1; i <= MEMBERS; i++) {
                w.println("M" + i + ",localhost," + (9000 + i));
            }
        }
        
        // Create members with specified profiles
        for (int i = 1; i <= MEMBERS; i++) {
            String id = "M" + i;
            int port = 9000 + i;
            
            // Use provided profile or default to RELIABLE
            CouncilMember.NetworkProfile profile = CouncilMember.NetworkProfile.RELIABLE;
            if (profiles.length > 0) {
                // If multiple profiles provided, use corresponding one
                // Otherwise use first profile for all
                profile = profiles.length == 1 ? profiles[0] : 
                         (i - 1 < profiles.length ? profiles[i - 1] : profile);
            }
            
            CouncilMember m = new CouncilMember(id, port, profile);
            members.add(m);
            m.start(CONFIG);
        }
        
        Thread.sleep(SETUP_DELAY_MS);
        System.out.println("Environment ready with " + members.size() + " members\n");
    }
    
    /**
     * Setup with all reliable connections (for Scenario 1 & 2).
     * 
     * @throws Exception if setup fails
     */
    public void setup() throws Exception {
        setup(CouncilMember.NetworkProfile.RELIABLE);
    }
    
    /**
     * Teardown and cleanup resources.
     */
    public void teardown() {
        System.out.println("\nTearing down...");
        members.forEach(CouncilMember::stop);
        members.clear();
        executor.shutdown();
        new File(CONFIG).delete();
    }
    
    // ========== SCENARIO 1: IDEAL NETWORK ==========
    
    /**
     * Test Scenario 1: All members with reliable connections.
     * 
     * @return true if test passes
     */
    public boolean testScenario1_IdealNetwork() {
        System.out.println("=== SCENARIO 1: The Ideal Network ===");
        System.out.println("All 9 members with RELIABLE profile");
        
        reset();
        
        // M4 proposes M5
        members.get(3).propose("M5");
        
        String result = waitConsensus(CONSENSUS_TIMEOUT_SHORT);
        boolean pass = "M5".equals(result);
        
        System.out.println(pass ? "PASS: Consensus on M5" : "FAIL: " + result);
        System.out.println("Expected: Quick consensus, all members agree");
        printMemberStates();
        return pass;
    }
    
    // ========== SCENARIO 2: CONCURRENT PROPOSALS ==========
    
    /**
     * Test Scenario 2: Concurrent proposals from multiple members.
     * 
     * @return true if test passes
     */
    public boolean testScenario2_ConcurrentProposals() {
        System.out.println("\n=== SCENARIO 2: Concurrent Proposals ===");
        System.out.println("M1 and M8 propose simultaneously");
        
        reset();
        
        // M1 proposes "M1" and M8 proposes "M8" concurrently
        executor.submit(() -> members.get(0).propose("M1"));
        executor.submit(() -> members.get(7).propose("M8"));
        
        String result = waitConsensus(CONSENSUS_TIMEOUT_SHORT + 5000);
        boolean pass = result != null && (result.equals("M1") || result.equals("M8"));
        
        System.out.println(pass ? "PASS: Consensus on " + result : "FAIL: " + result);
        System.out.println("Expected: Single winner (M1 or M8), all agree");
        printMemberStates();
        return pass;
    }
    
    // ========== SCENARIO 3: FAULT-TOLERANCE ==========
    
    /**
     * Setup mixed network profiles for fault-tolerance testing.
     * 
     * @throws Exception if setup fails
     */
    private void setupMixedProfiles() throws Exception {
        teardown();
        setup(
            CouncilMember.NetworkProfile.RELIABLE,  // M1
            CouncilMember.NetworkProfile.LATENT,    // M2
            CouncilMember.NetworkProfile.FAILURE,   // M3
            CouncilMember.NetworkProfile.STANDARD,  // M4
            CouncilMember.NetworkProfile.STANDARD,  // M5
            CouncilMember.NetworkProfile.STANDARD,  // M6
            CouncilMember.NetworkProfile.STANDARD,  // M7
            CouncilMember.NetworkProfile.STANDARD,  // M8
            CouncilMember.NetworkProfile.STANDARD   // M9
        );
    }
    
    /**
     * Scenario 3a: Standard member (M4) initiates proposal.
     * Setup: M1=RELIABLE, M2=LATENT, M3=FAILURE, M4-M9=STANDARD
     * 
     * @return true if test passes
     */
    public boolean testScenario3a_StandardMemberProposal() {
        System.out.println("\n=== SCENARIO 3a: Standard Member Proposal ===");
        System.out.println("M4 (standard) proposes with mixed network conditions");
        
        try {
            setupMixedProfiles();
            reset();
            
            // M4 proposes M6
            members.get(3).propose("M6");
            
            String result = waitConsensus(CONSENSUS_TIMEOUT_MEDIUM);
            boolean pass = "M6".equals(result);
            
            System.out.println(pass ? "PASS: Consensus on M6" : "FAIL: " + result);
            System.out.println("Expected: Consensus despite M2 latency and M3 failures");
            printMemberStates();
            return pass;
            
        } catch (Exception e) {
            System.err.println("FAIL: Exception - " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Scenario 3b: Latent member (M2) initiates proposal.
     * M2 has poor connection but should eventually succeed.
     * 
     * @return true if test passes
     */
    public boolean testScenario3b_LatentMemberProposal() {
        System.out.println("\n=== SCENARIO 3b: Latent Member Proposal ===");
        System.out.println("M2 (latent) proposes despite high latency");
        
        try {
            // Ensure fresh setup with correct profiles
            setupMixedProfiles();
            reset();
            
            // M2 proposes M7
            members.get(1).propose("M7");
            
            // Longer timeout for latent member
            String result = waitConsensus(CONSENSUS_TIMEOUT_LONG);
            boolean pass = "M7".equals(result);
            
            System.out.println(pass ? "PASS: Consensus on M7" : "FAIL: " + result);
            System.out.println("Expected: Slow but successful consensus from latent proposer");
            printMemberStates();
            return pass;
            
        } catch (Exception e) {
            System.err.println("FAIL: Exception - " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Scenario 3c: Failing member (M3) initiates proposal then crashes.
     * System must not stall - another member should take over.
     * 
     * @return true if test passes
     */
    public boolean testScenario3c_FailureMemberCrash() {
        System.out.println("\n=== SCENARIO 3c: Failure Member Crash ===");
        System.out.println("M3 (failure) proposes, crashes, then M5 takes over");
        
        try {
            // Ensure fresh setup with correct profiles
            setupMixedProfiles();
            reset();
            
            // M3 proposes M9
            members.get(2).propose("M9");
            
            // Wait a bit, then simulate M3 crash
            Thread.sleep(CRASH_DELAY_MS);
            members.get(2).simulateCrash();
            System.out.println("M3 crashed after sending PREPARE");
            
            // Wait another moment
            Thread.sleep(RECOVERY_DELAY_MS);
            
            // M5 takes over with same proposal
            System.out.println("M5 initiating backup proposal");
            members.get(4).propose("M9");
            
            // Wait for consensus
            String result = waitConsensus(CONSENSUS_TIMEOUT_MEDIUM + 5000);
            boolean pass = "M9".equals(result);
            
            System.out.println(pass ? "PASS: Consensus on M9" : "FAIL: " + result);
            System.out.println("Expected: System recovers, backup proposer succeeds");
            printMemberStates();
            return pass;
            
        } catch (Exception e) {
            System.err.println("FAIL: Exception - " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    // ========== TEST RUNNER ==========
    
    /**
     * Run all test scenarios sequentially.
     */
    public void runAllScenarios() {
        int pass = 0, fail = 0;
        boolean[] results = new boolean[5];
        
        try {
            // Scenario 1: Ideal Network
            System.out.println("Starting Scenario 1...\n");
            setup();
            results[0] = testScenario1_IdealNetwork();
            if (results[0]) pass++; else fail++;
            
            // Scenario 2: Concurrent Proposals
            System.out.println("\nStarting Scenario 2...\n");
            reset();
            results[1] = testScenario2_ConcurrentProposals();
            if (results[1]) pass++; else fail++;
            
            teardown();
            Thread.sleep(1000); // Brief pause between major phases
            
            // Scenario 3a: Standard member proposal
            System.out.println("\nStarting Scenario 3a...\n");
            results[2] = testScenario3a_StandardMemberProposal();
            if (results[2]) pass++; else fail++;
            
            // Scenario 3b: Latent member proposal
            System.out.println("\nStarting Scenario 3b...\n");
            results[3] = testScenario3b_LatentMemberProposal();
            if (results[3]) pass++; else fail++;
            
            // Scenario 3c: Failure member crash and recovery
            System.out.println("\nStarting Scenario 3c...\n");
            results[4] = testScenario3c_FailureMemberCrash();
            if (results[4]) pass++; else fail++;
            
        } catch (Exception e) {
            System.err.println("Test execution error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            teardown();
        }
        
        // Print summary
        System.out.println("\n================");
        System.out.println("TEST SUMMARY");
        System.out.println("================");
        System.out.println("Scenario 1 (Ideal Network):          " + (results[0] ? "PASS" : "FAIL"));
        System.out.println("Scenario 2 (Concurrent Proposals):   " + (results[1] ? "PASS" : "FAIL"));
        System.out.println("Scenario 3a (Standard Member):       " + (results[2] ? "PASS" : "FAIL"));
        System.out.println("Scenario 3b (Latent Member):         " + (results[3] ? "PASS" : "FAIL"));
        System.out.println("Scenario 3c (Failure Member):        " + (results[4] ? "PASS" : "FAIL"));
        System.out.println("================");
        System.out.println("Total: " + pass + " passed, " + fail + " failed");
        System.out.println("Success Rate: " + String.format("%.1f", 100.0 * pass / (pass + fail)) + "%");
    }
    
    // ========== HELPER METHODS ==========
    
    /**
     * Reset all members to initial state.
     */
    private void reset() {
        members.forEach(CouncilMember::reset);
        try {
            Thread.sleep(RESET_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Wait for consensus to be reached.
     * 
     * @param timeout Maximum time to wait in milliseconds
     * @return Consensus value or null if timeout
     */
    private String waitConsensus(long timeout) {
        long start = System.currentTimeMillis();
        
        while (System.currentTimeMillis() - start < timeout) {
            for (CouncilMember m : members) {
                if (m.hasLearned()) {
                    return m.getLearned();
                }
            }
            
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        return null;
    }
    
    /**
     * Print current state of all members.
     */
    private void printMemberStates() {
        System.out.println("\nMember States:");
        for (CouncilMember m : members) {
            System.out.println("  " + m.getState());
        }
    }
    
    // ========== MAIN ==========
    
    /**
     * Main entry point for running tests.
     * 
     * @param args Command line arguments
     * @throws Exception if test execution fails
     */
    public static void main(String[] args) throws Exception {
        System.out.println("Paxos Test Suite");
        System.out.println("================\n");
        System.out.println("Testing Adelaide Suburbs Council Election");
        System.out.println("9 council members (M1-M9) reaching consensus\n");
        
        PaxosTest test = new PaxosTest();
        test.runAllScenarios();
        
        System.exit(0);
    }
}
