import java.rmi.Naming;

/**
 * Comprehensive Calculator Client Test Suite
 * Performs systematic testing of all calculator operations
 * Includes both positive and edge case testing
 */
public class CalculatorClientTest {
    
    private static final String SERVER_URL = "//localhost/CalculatorService";
    private static Calculator calc;
    private static int testsPassed = 0;
    private static int totalTests = 0;
    
    /**
     * Main method to run all calculator tests
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        try {
            System.out.println("=== Calculator Comprehensive Test Suite ===");
            System.out.println("Connecting to calculator service...");
            
            calc = (Calculator) Naming.lookup(SERVER_URL);
            System.out.println("Connected successfully to: " + SERVER_URL);
            System.out.println();
            
            // Run all test suites
            testBasicOperations();
            testMathematicalOperations();
            testEdgeCases();
            testDelayedOperations();
            
            // Print final results
            printTestSummary();
            
        } catch (Exception e) {
            System.err.println("Failed to connect to calculator service:");
            System.err.println("Error: " + e.getMessage());
            System.err.println("Make sure CalculatorServer is running");
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Test basic stack operations (push, pop, isEmpty)
     */
    private static void testBasicOperations() {
        System.out.println("--- Testing Basic Operations ---");
        
        // Test 1: isEmpty on fresh start
        testOperation("isEmpty() on fresh start", () -> {
            boolean empty = calc.isEmpty();
            return empty; // Should be true initially
        }, true);
        
        // Test 2: Single pushValue and pop
        testOperation("pushValue(10) and pop()", () -> {
            calc.pushValue(10);
            int result = calc.pop();
            return result == 10;
        }, true);
        
        // Test 3: Multiple pushValue operations
        testOperation("Multiple pushValue operations", () -> {
            calc.pushValue(10);
            calc.pushValue(20);
            calc.pushValue(30);
            
            int third = calc.pop();  // Should be 30
            int second = calc.pop(); // Should be 20
            int first = calc.pop();  // Should be 10
            
            return third == 30 && second == 20 && first == 10;
        }, true);
        
        // Test 4: isEmpty after clearing stack
        testOperation("isEmpty() after clearing stack", () -> {
            return calc.isEmpty();
        }, true);
        
        System.out.println();
    }
    
    /**
     * Test mathematical operations (min, max, gcd, lcm)
     */
    private static void testMathematicalOperations() {
        System.out.println("--- Testing Mathematical Operations ---");
        
        // Test MAX operation
        testOperation("MAX operation", () -> {
            calc.pushValue(5);
            calc.pushValue(15);
            calc.pushValue(10);
            calc.pushOperation("max");
            int result = calc.pop();
            return result == 15;
        }, true);
        
        // Test MIN operation
        testOperation("MIN operation", () -> {
            calc.pushValue(25);
            calc.pushValue(5);
            calc.pushValue(15);
            calc.pushOperation("min");
            int result = calc.pop();
            return result == 5;
        }, true);
        
        // Test GCD operation
        testOperation("GCD operation (12, 18, 24)", () -> {
            calc.pushValue(12);
            calc.pushValue(18);
            calc.pushValue(24);
            calc.pushOperation("gcd");
            int result = calc.pop();
            return result == 6; // GCD of 12, 18, 24 is 6
        }, true);
        
        // Test LCM operation
        testOperation("LCM operation (4, 6)", () -> {
            calc.pushValue(4);
            calc.pushValue(6);
            calc.pushOperation("lcm");
            int result = calc.pop();
            return result == 12; // LCM of 4, 6 is 12
        }, true);
        
        // Test GCD with single value
        testOperation("GCD operation with single value", () -> {
            calc.pushValue(42);
            calc.pushOperation("gcd");
            int result = calc.pop();
            return result == 42;
        }, true);
        
        // Test with negative numbers
        testOperation("MAX operation with negative numbers", () -> {
            calc.pushValue(-10);
            calc.pushValue(-5);
            calc.pushValue(-20);
            calc.pushOperation("max");
            int result = calc.pop();
            return result == -5;
        }, true);
        
        System.out.println();
    }
    
    /**
     * Test edge cases and error conditions
     */
    private static void testEdgeCases() {
        System.out.println("--- Testing Edge Cases ---");
        
        // Test invalid operation
        testOperation("Invalid operation handling", () -> {
            try {
                calc.pushValue(10);
                calc.pushOperation("invalid");
                return false; // Should not reach here
            } catch (Exception e) {
                // Should throw exception for invalid operation
                return e.getMessage().contains("Invalid operation") || 
                       e.getMessage().contains("Unknown operation");
            }
        }, true);
        
        // Test pop from empty stack
        testOperation("Pop from empty stack", () -> {
            try {
                // Make sure stack is empty first
                while (!calc.isEmpty()) {
                    calc.pop();
                }
                calc.pop(); // This should fail
                return false;
            } catch (Exception e) {
                return e.getMessage().contains("empty stack");
            }
        }, true);
        
        // Test operation on empty stack
        testOperation("Operation on empty stack", () -> {
            try {
                // Make sure stack is empty
                while (!calc.isEmpty()) {
                    calc.pop();
                }
                calc.pushOperation("max");
                return false;
            } catch (Exception e) {
                return e.getMessage().contains("empty stack");
            }
        }, true);
        
        // Test LCM with zero
        testOperation("LCM with zero", () -> {
            calc.pushValue(0);
            calc.pushValue(5);
            calc.pushOperation("lcm");
            int result = calc.pop();
            return result == 0;
        }, true);
        
        System.out.println();
    }
    
    /**
     * Test delayed operations
     */
    private static void testDelayedOperations() {
        System.out.println("--- Testing Delayed Operations ---");
        
        // Test delayPop functionality
        testOperation("delayPop with 1 second delay", () -> {
            calc.pushValue(99);
            
            long startTime = System.currentTimeMillis();
            int result = calc.delayPop(1000); // 1 second delay
            long duration = System.currentTimeMillis() - startTime;
            
            // Check both result and timing
            return result == 99 && duration >= 1000 && duration < 1500;
        }, true);
        
        // Test delayPop with zero delay
        testOperation("delayPop with zero delay", () -> {
            calc.pushValue(77);
            
            long startTime = System.currentTimeMillis();
            int result = calc.delayPop(0);
            long duration = System.currentTimeMillis() - startTime;
            
            return result == 77 && duration < 100; // Should be very quick
        }, true);        
        // Test delayPop on empty stack
        testOperation("delayPop on empty stack", () -> {
            try {
                // Ensure stack is empty
                while (!calc.isEmpty()) {
                    calc.pop();
                }
                calc.delayPop(500);
                return false;
            } catch (Exception e) {
                return e.getMessage().contains("empty stack");
            }
        }, true);        
        System.out.println();
    }
    
    /**
     * Generic test operation helper
     * @param testName Description of the test
     * @param testLogic Test logic to execute
     * @param expectedResult Expected test result
     */
    private static void testOperation(String testName, TestOperation testLogic, boolean expectedResult) {
        totalTests++;
        try {
            boolean result = testLogic.execute();
            if (result == expectedResult) {
                System.out.println("✓ PASS: " + testName);
                testsPassed++;
            } else {
                System.out.println("✗ FAIL: " + testName + " (expected: " + expectedResult + ", got: " + result + ")");
            }
        } catch (Exception e) {
            if (!expectedResult) {
                // We expected this test to fail
                System.out.println("✓ PASS: " + testName + " (correctly threw exception)");
                testsPassed++;
            } else {
                System.out.println("✗ FAIL: " + testName + " (unexpected exception: " + e.getMessage() + ")");
            }
        }
    }    
    /**
     * Print test summary results
     */
    private static void printTestSummary() {
        System.out.println("=== Test Summary ===");
        System.out.println("Tests passed: " + testsPassed + "/" + totalTests);
        
        double passRate = (double) testsPassed / totalTests * 100;
        System.out.printf("Pass rate: %.1f%%\n", passRate);
        
        if (testsPassed == totalTests) {
            System.out.println("🎉 All tests passed! Calculator implementation is working correctly.");
        } else {
            System.out.println("⚠️  Some tests failed. Check the implementation for issues.");
        }
        System.out.println();
    }    
    /**
     * Functional interface for test operations
     */
    @FunctionalInterface
    private interface TestOperation {
        boolean execute() throws Exception;
    }
}