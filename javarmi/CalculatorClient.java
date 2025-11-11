import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.net.MalformedURLException;

/**
 * Calculator RMI Client
 * Demonstrates basic usage of the remote calculator service
 * Connects to server and performs simple calculator operations
 */
public class CalculatorClient {
    
    /** Default server location */
    private static final String SERVER_URL = "//localhost/CalculatorService";
    
    /**
     * Main method demonstrating calculator client usage
     * Performs a sequence of calculator operations to test functionality
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        Calculator calc = null;
        
        try {
            System.out.println("Calculator Client starting...");
            System.out.println("Connecting to server at: " + SERVER_URL);
            
            // Connect to the remote calculator service
            calc = (Calculator) Naming.lookup(SERVER_URL);
            System.out.println("Successfully connected to Calculator service");
            System.out.println();
            
            // Demonstrate basic calculator operations
            performBasicDemo(calc);
            
            System.out.println("Calculator Client demo completed successfully");
            
        } catch (NotBoundException e) {
            System.err.println("Calculator service not found on server");
            System.err.println("Make sure the CalculatorServer is running");
            handleError(e);
            
        } catch (MalformedURLException e) {
            System.err.println("Invalid server URL: " + SERVER_URL);
            handleError(e);
            
        } catch (RemoteException e) {
            System.err.println("RMI communication error:");
            System.err.println("Error: " + e.getMessage());
            handleError(e);
            
        } catch (Exception e) {
            System.err.println("Unexpected error occurred:");
            handleError(e);
        }
    }
    
    /**
     * Perform basic calculator operations demonstration
     * Shows usage of all calculator methods
     * @param calc The remote calculator interface
     * @throws RemoteException If any RMI operation fails
     */
    private static void performBasicDemo(Calculator calc) throws RemoteException {
        System.out.println("=== Basic Calculator Demo ===");
        
        // Test 1: Push values and perform max operation
        System.out.println("\n1. Testing pushValue and max operation:");
        calc.pushValue(10);
        System.out.println("   Pushed value: 10");
        
        calc.pushValue(20);
        System.out.println("   Pushed value: 20");
        
        calc.pushValue(15);
        System.out.println("   Pushed value: 15");
        
        calc.pushOperation("max");
        System.out.println("   Executed max operation");
        
        int maxResult = calc.pop();
        System.out.println("   Max result: " + maxResult + " (expected: 20)");
        
        // Test 2: Check if stack is empty
        System.out.println("\n2. Testing isEmpty:");
        boolean empty = calc.isEmpty();
        System.out.println("   Stack empty: " + empty + " (expected: true)");
        
        // Test 3: Test min operation
        System.out.println("\n3. Testing min operation:");
        calc.pushValue(5);
        calc.pushValue(25);
        calc.pushValue(15);
        System.out.println("   Pushed values: 5, 25, 15");
        
        calc.pushOperation("min");
        int minResult = calc.pop();
        System.out.println("   Min result: " + minResult + " (expected: 5)");
        
        // Test 4: Test delayPop
        System.out.println("\n4. Testing delayPop:");
        calc.pushValue(42);
        System.out.println("   Pushed value: 42");
        System.out.println("   Starting delayPop (2 seconds)...");
        
        long startTime = System.currentTimeMillis();
        int delayedResult = calc.delayPop(2000);
        long duration = System.currentTimeMillis() - startTime;
        
        System.out.println("   DelayPop result: " + delayedResult + 
                          " (delay: " + duration + "ms, expected ~2000ms)");
        
        // Test 5: Test GCD operation
        System.out.println("\n5. Testing GCD operation:");
        calc.pushValue(12);
        calc.pushValue(18);
        calc.pushValue(24);
        System.out.println("   Pushed values: 12, 18, 24");
        
        calc.pushOperation("gcd");
        int gcdResult = calc.pop();
        System.out.println("   GCD result: " + gcdResult + " (expected: 6)");
        
        // Test 6: Test LCM operation
        System.out.println("\n6. Testing LCM operation:");
        calc.pushValue(4);
        calc.pushValue(6);
        System.out.println("   Pushed values: 4, 6");
        
        calc.pushOperation("lcm");
        int lcmResult = calc.pop();
        System.out.println("   LCM result: " + lcmResult + " (expected: 12)");
        
        System.out.println("\n=== Demo completed ===");
    }
    
    /**
     * Handle errors with appropriate logging and cleanup
     * @param e The exception that occurred
     */
    private static void handleError(Exception e) {
        System.err.println("Full error details:");
        e.printStackTrace();
        System.err.println();
        System.err.println("Troubleshooting tips:");
        System.err.println("1. Ensure CalculatorServer is running");
        System.err.println("2. Check if RMI registry is started on port 1099");
        System.err.println("3. Verify no firewall is blocking the connection");
        System.exit(1);
    }
}