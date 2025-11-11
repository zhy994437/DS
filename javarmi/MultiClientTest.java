import java.rmi.Naming;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Multi-Client Calculator Test
 * Tests the calculator service with multiple concurrent clients
 * Demonstrates thread safety and concurrent access patterns
 */
public class MultiClientTest {
    
    private static final String SERVER_URL = "//localhost/CalculatorService";
    private static final int DEFAULT_CLIENT_COUNT = 5;
    private static final AtomicInteger successfulClients = new AtomicInteger(0);
    private static final AtomicInteger failedClients = new AtomicInteger(0);
    
    /**
     * Main method to run multi-client tests
     * @param args Command line arguments - can specify number of clients
     */
    public static void main(String[] args) {
        int clientCount = DEFAULT_CLIENT_COUNT;
        
        // Parse client count from command line if provided
        if (args.length > 0) {
            try {
                clientCount = Integer.parseInt(args[0]);
                if (clientCount <= 0) {
                    System.err.println("Client count must be positive. Using default: " + DEFAULT_CLIENT_COUNT);
                    clientCount = DEFAULT_CLIENT_COUNT;
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid client count format. Using default: " + DEFAULT_CLIENT_COUNT);
                clientCount = DEFAULT_CLIENT_COUNT;
            }
        }
        
        System.out.println("=== Multi-Client Calculator Test ===");
        System.out.println("Number of concurrent clients: " + clientCount);
        System.out.println("Server URL: " + SERVER_URL);
        System.out.println();
        
        // Run different test scenarios
        runBasicConcurrencyTest(clientCount);
        runStressTest();
        runSequentialTest();
        
        printFinalResults();
    }
    
    /**
     * Test basic concurrent operations with multiple clients
     * @param clientCount Number of concurrent clients to create
     */
    private static void runBasicConcurrencyTest(int clientCount) {
        System.out.println("--- Basic Concurrency Test ---");
        
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(clientCount);
        Thread[] clients = new Thread[clientCount];
        
        // Create client threads
        for (int i = 0; i < clientCount; i++) {
            final int clientId = i + 1;
            clients[i] = new Thread(new BasicTestClient(clientId, startLatch, finishLatch));
            clients[i].setName("Client-" + clientId);
            clients[i].start();
        }
        
        // Start all clients simultaneously
        System.out.println("Starting " + clientCount + " clients simultaneously...");
        startLatch.countDown();
        
        // Wait for all clients to complete
        try {
            finishLatch.await();
            System.out.println("All clients completed basic concurrency test");
        } catch (InterruptedException e) {
            System.err.println("Test interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
        
        System.out.println();
    }
    
    /**
     * Run stress test with rapid operations
     */
    private static void runStressTest() {
        System.out.println("--- Stress Test ---");
        System.out.println("Running rapid operations stress test...");
        
        CountDownLatch stressLatch = new CountDownLatch(3);
        
        // Create stress test clients
        for (int i = 0; i < 3; i++) {
            final int clientId = i + 1;
            Thread stressClient = new Thread(new StressTestClient(clientId, stressLatch));
            stressClient.setName("StressClient-" + clientId);
            stressClient.start();
        }
        
        try {
            stressLatch.await();
            System.out.println("Stress test completed");
        } catch (InterruptedException e) {
            System.err.println("Stress test interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
        
        System.out.println();
    }
    
    /**
     * Run sequential test to verify stack state consistency
     */
    private static void runSequentialTest() {
        System.out.println("--- Sequential Consistency Test ---");
        
        try {
            Calculator calc = (Calculator) Naming.lookup(SERVER_URL);
            
            // Clear any existing values
            while (!calc.isEmpty()) {
                calc.pop();
            }
            
            System.out.println("Testing sequential operations for consistency...");
            
            // Push known values
            calc.pushValue(100);
            calc.pushValue(200);
            calc.pushValue(300);
            
            calc.pushOperation("max");
            int result = calc.pop();
            
            if (result == 300) {
                System.out.println("✓ Sequential test passed: max(100,200,300) = " + result);
                successfulClients.incrementAndGet();
            } else {
                System.out.println("✗ Sequential test failed: expected 300, got " + result);
                failedClients.incrementAndGet();
            }
            
        } catch (Exception e) {
            System.err.println("Sequential test failed with exception: " + e.getMessage());
            failedClients.incrementAndGet();
        }
        
        System.out.println();
    }
    
    /**
     * Print final test results summary
     */
    private static void printFinalResults() {
        System.out.println("=== Final Test Results ===");
        
        int successful = successfulClients.get();
        int failed = failedClients.get();
        int total = successful + failed;
        
        System.out.println("Successful operations: " + successful);
        System.out.println("Failed operations: " + failed);
        System.out.println("Total operations: " + total);
        
        if (total > 0) {
            double successRate = (double) successful / total * 100;
            System.out.printf("Success rate: %.1f%%\n", successRate);
            
            if (successRate == 100.0) {
                System.out.println("🎉 All multi-client tests passed!");
            } else if (successRate >= 80.0) {
                System.out.println("⚠️  Most tests passed, but some issues detected");
            } else {
                System.out.println("❌ Multiple failures detected - check server implementation");
            }
        }
        
        System.out.println("Multi-client testing completed.");
    }
    
    /**
     * Basic test client implementation
     */
    private static class BasicTestClient implements Runnable {
        private final int clientId;
        private final CountDownLatch startLatch;
        private final CountDownLatch finishLatch;
        
        public BasicTestClient(int clientId, CountDownLatch startLatch, CountDownLatch finishLatch) {
            this.clientId = clientId;
            this.startLatch = startLatch;
            this.finishLatch = finishLatch;
        }
        
        @Override
        public void run() {
            try {
                // Wait for start signal
                startLatch.await();
                
                Calculator calc = (Calculator) Naming.lookup(SERVER_URL);
                System.out.println("Client " + clientId + " connected");
                
                // Perform client-specific operations
                performClientOperations(calc);
                
                successfulClients.incrementAndGet();
                System.out.println("Client " + clientId + " completed successfully");
                
            } catch (Exception e) {
                System.err.println("Client " + clientId + " failed: " + e.getMessage());
                failedClients.incrementAndGet();
            } finally {
                finishLatch.countDown();
            }
        }
        
        /**
         * Perform operations specific to this client
         * @param calc Calculator interface
         * @throws Exception If operations fail
         */
        private void performClientOperations(Calculator calc) throws Exception {
            // Each client pushes unique values and performs operations
            int baseValue = clientId * 10;
            
            calc.pushValue(baseValue);
            calc.pushValue(baseValue + 5);
            calc.pushValue(baseValue + 10);
            
            // Alternate between different operations
            String operation = (clientId % 4 == 0) ? "min" : 
                              (clientId % 3 == 0) ? "max" : 
                              (clientId % 2 == 0) ? "gcd" : "lcm";
            
            calc.pushOperation(operation);
            int result = calc.pop();
            
            System.out.println("Client " + clientId + " operation '" + operation + 
                             "' result: " + result);
            
            // Test isEmpty
            boolean empty = calc.isEmpty();
            System.out.println("Client " + clientId + " sees stack empty: " + empty);
            
            // Test delayPop
            calc.pushValue(clientId * 100);
            int delayedResult = calc.delayPop(500); // 0.5 second delay
            System.out.println("Client " + clientId + " delayPop result: " + delayedResult);
        }
    }
    
    /**
     * Stress test client for rapid operations
     */
    private static class StressTestClient implements Runnable {
        private final int clientId;
        private final CountDownLatch finishLatch;
        private static final int OPERATIONS_COUNT = 20;
        
        public StressTestClient(int clientId, CountDownLatch finishLatch) {
            this.clientId = clientId;
            this.finishLatch = finishLatch;
        }
        
        @Override
        public void run() {
            try {
                Calculator calc = (Calculator) Naming.lookup(SERVER_URL);
                
                System.out.println("StressClient " + clientId + " starting rapid operations...");
                
                // Perform rapid operations
                for (int i = 0; i < OPERATIONS_COUNT; i++) {
                    try {
                        // Rapid push/pop cycles
                        calc.pushValue(clientId * 1000 + i);
                        
                        if (i % 5 == 0 && !calc.isEmpty()) {
                            // Occasionally pop values
                            calc.pop();
                        }
                        
                        if (i % 10 == 0) {
                            // Occasionally perform operations
                            calc.pushValue(i);
                            calc.pushValue(i + 1);
                            calc.pushOperation("max");
                            if (!calc.isEmpty()) {
                                calc.pop();
                            }
                        }
                        
                        // Small delay to avoid overwhelming server
                        Thread.sleep(10);
                        
                    } catch (Exception e) {
                        // Continue with other operations even if one fails
                        System.err.println("StressClient " + clientId + " operation " + i + " failed: " + e.getMessage());
                    }
                }
                
                successfulClients.incrementAndGet();
                System.out.println("StressClient " + clientId + " completed " + OPERATIONS_COUNT + " operations");
                
            } catch (Exception e) {
                System.err.println("StressClient " + clientId + " failed: " + e.getMessage());
                failedClients.incrementAndGet();
            } finally {
                finishLatch.countDown();
            }
        }
    }
}