import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Calculator RMI Server
 * Bootstraps and hosts the remote calculator service
 * Handles RMI registry management and service binding
 */
public class CalculatorServer {
    
    /** RMI service name for client lookups */
    private static final String SERVICE_NAME = "CalculatorService";
    
    /** Default RMI registry port */
    private static final int REGISTRY_PORT = 1099;
    
    /**
     * Main method to start the calculator server
     * Creates calculator implementation, manages RMI registry, and binds service
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        try {
            System.out.println("Starting Calculator RMI Server...");
            
            // Create the calculator implementation object
            CalculatorImplementation calc = new CalculatorImplementation();
            System.out.println("Calculator implementation created successfully");
            
            // Handle RMI registry - try to use existing, create if needed
            Registry registry = setupRmiRegistry();
            
            // Bind the service to the registry
            String serviceUrl = "rmi://localhost:" + REGISTRY_PORT + "/" + SERVICE_NAME;
            Naming.rebind(serviceUrl, calc);
            
            System.out.println("Calculator Server ready!");
            System.out.println("Service bound to: " + SERVICE_NAME);
            System.out.println("Registry port: " + REGISTRY_PORT);
            System.out.println("Service URL: " + serviceUrl);
            System.out.println();
            System.out.println("Server is running... Press Ctrl+C to stop");
            
            // Add shutdown hook for graceful termination
            addShutdownHook();
            
            // Keep the server running
            keepServerRunning();
            
        } catch (Exception e) {
            System.err.println("Failed to start Calculator Server:");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Setup RMI registry - try to use existing registry or create new one
     * @return The RMI registry instance
     * @throws Exception If registry setup fails
     */
    private static Registry setupRmiRegistry() throws Exception {
        Registry registry;
        
        try {
            // Try to get existing registry
            registry = LocateRegistry.getRegistry(REGISTRY_PORT);
            registry.list(); // Test if registry is accessible
            System.out.println("Using existing RMI registry on port " + REGISTRY_PORT);
            
        } catch (Exception e) {
            // Create new registry if none exists
            System.out.println("No existing registry found, creating new RMI registry on port " + REGISTRY_PORT);
            registry = LocateRegistry.createRegistry(REGISTRY_PORT);
            
            // Give registry time to initialize
            Thread.sleep(500);
        }
        
        return registry;
    }
    
    /**
     * Add shutdown hook for graceful server termination
     * Provides cleanup when server is stopped
     */
    private static void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down Calculator Server...");
            try {
                // Unbind service
                Naming.unbind("rmi://localhost:" + REGISTRY_PORT + "/" + SERVICE_NAME);
                System.out.println("Service unbound successfully");
            } catch (Exception e) {
                System.err.println("Error during shutdown: " + e.getMessage());
            }
            System.out.println("Calculator Server stopped");
        }));
    }
    
    /**
     * Keep server running until interrupted
     * Provides a clean way to maintain server lifecycle
     */
    private static void keepServerRunning() {
        try {
            // Keep main thread alive
            while (true) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            System.out.println("Server interrupted, shutting down...");
            Thread.currentThread().interrupt();
        }
    }
}