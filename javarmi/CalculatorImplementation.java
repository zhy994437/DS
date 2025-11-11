import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

/**
 * Calculator Remote Object Implementation
 * Provides thread-safe stack-based calculator operations for RMI clients
 * All clients share the same stack on the server
 */
public class CalculatorImplementation extends UnicastRemoteObject implements Calculator {
    
    // Constants for supported operations
    private static final String MIN_OPERATION = "min";
    private static final String MAX_OPERATION = "max";
    private static final String LCM_OPERATION = "lcm";
    private static final String GCD_OPERATION = "gcd";
    
    /** Shared calculation stack for all clients */
    private final Stack<Integer> stack;
    
    /**
     * Constructor - initializes the shared calculation stack
     * @throws RemoteException If RMI initialization fails
     */
    protected CalculatorImplementation() throws RemoteException {
        super();
        stack = new Stack<>();
        System.out.println("Calculator implementation created with shared stack");
    }
    
    /**
     * Push an integer value onto the top of the stack
     * Thread-safe operation using synchronization
     * @param val The integer value to push
     * @throws RemoteException If RMI communication fails
     */
    @Override
    public synchronized void pushValue(int val) throws RemoteException {
        stack.push(val);
        System.out.println("[" + Thread.currentThread().getName() + "] Pushed value: " + val + 
                          " (Stack size: " + stack.size() + ")");
    }
    
    /**
     * Perform a mathematical operation on all stack values
     * Pops all values from stack, performs operation, pushes result back
     * Special case: Returns immediately if stack is empty
     * @param operator The operation: "min", "max", "lcm", "gcd"
     * @throws RemoteException If operation is invalid or RMI communication fails
     */
    @Override
    public synchronized void pushOperation(String operator) throws RemoteException {
        if (stack.isEmpty()) {
            throw new RemoteException("Cannot perform operation '" + operator + "' on empty stack");
        }
        
        // Validate operation before proceeding
        if (!isValidOperation(operator)) {
            throw new RemoteException("Invalid operation: " + operator + 
                                    ". Valid operations are: min, max, lcm, gcd");
        }
        
        // Collect all values from the stack
        List<Integer> values = new ArrayList<>();
        while (!stack.isEmpty()) {
            values.add(stack.pop());
        }
        
        int result = calculateOperation(operator, values);
        stack.push(result);
        
        System.out.println("[" + Thread.currentThread().getName() + "] Operation '" + operator + 
                          "' completed. Result: " + result + " (from " + values.size() + " values)");
    }
    
    /**
     * Pop the top element from the stack
     * @return The integer value at the top of the stack
     * @throws RemoteException If stack is empty or RMI communication fails
     */
    @Override
    public synchronized int pop() throws RemoteException {
        if (stack.isEmpty()) {
            throw new RemoteException("Cannot pop from empty stack");
        }
        
        int value = stack.pop();
        System.out.println("[" + Thread.currentThread().getName() + "] Popped value: " + value + 
                          " (Stack size: " + stack.size() + ")");
        return value;
    }
    
    /**
     * Check if the stack is empty
     * @return true if stack is empty, false otherwise
     * @throws RemoteException If RMI communication fails
     */
    @Override
    public synchronized boolean isEmpty() throws RemoteException {
        boolean empty = stack.isEmpty();
        System.out.println("[" + Thread.currentThread().getName() + "] Stack isEmpty check: " + empty + 
                          " (Stack size: " + stack.size() + ")");
        return empty;
    }
    
    /**
     * Delayed pop operation - waits specified milliseconds before popping
     * @param millis Delay time in milliseconds (must be non-negative)
     * @return The value popped from the stack after delay
     * @throws RemoteException If stack is empty, delay interrupted, invalid millis, or RMI fails
     */
    @Override
    public synchronized int delayPop(int millis) throws RemoteException {
        if (millis < 0) {
            throw new RemoteException("Delay milliseconds cannot be negative: " + millis);
        }
        
        if (stack.isEmpty()) {
            throw new RemoteException("Cannot delayPop from empty stack");
        }
        
        System.out.println("[" + Thread.currentThread().getName() + "] Starting delayPop with " + 
                          millis + "ms delay (Stack size: " + stack.size() + ")");
        
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RemoteException("DelayPop interrupted during wait", e);
        }
        
        return pop();
    }
    
    /**
     * Validate if the operation string is supported
     * @param operator The operation string to validate
     * @return true if operation is valid, false otherwise
     */
    private boolean isValidOperation(String operator) {
        return MIN_OPERATION.equals(operator) || 
               MAX_OPERATION.equals(operator) || 
               LCM_OPERATION.equals(operator) || 
               GCD_OPERATION.equals(operator);
    }
    
    /**
     * Calculate the result of the specified operation on the given values
     * @param operator The operation to perform
     * @param values List of values to operate on
     * @return The calculated result
     * @throws RemoteException If operation fails
     */
    private int calculateOperation(String operator, List<Integer> values) throws RemoteException {
        if (values.isEmpty()) {
            throw new RemoteException("Cannot perform operation on empty value list");
        }
        
        int result = values.get(0);
        
        switch (operator) {
            case MIN_OPERATION:
                for (int value : values) {
                    result = Math.min(result, value);
                }
                break;
                
            case MAX_OPERATION:
                for (int value : values) {
                    result = Math.max(result, value);
                }
                break;
                
            case LCM_OPERATION:
                for (int i = 1; i < values.size(); i++) {
                    result = calculateLcm(result, values.get(i));
                }
                break;
                
            case GCD_OPERATION:
                for (int i = 1; i < values.size(); i++) {
                    result = calculateGcd(result, values.get(i));
                }
                break;
                
            default:
                throw new RemoteException("Unsupported operation: " + operator);
        }
        
        return result;
    }
    
    /**
     * Calculate the greatest common divisor of two numbers using Euclidean algorithm
     * @param a First number
     * @param b Second number
     * @return The greatest common divisor
     */
    private int calculateGcd(int a, int b) {
        a = Math.abs(a);
        b = Math.abs(b);
        
        while (b != 0) {
            int temp = b;
            b = a % b;
            a = temp;
        }
        return a;
    }
    
    /**
     * Calculate the least common multiple of two numbers
     * @param a First number
     * @param b Second number
     * @return The least common multiple
     */
    private int calculateLcm(int a, int b) {
        if (a == 0 || b == 0) {
            return 0;
        }
        return Math.abs(a * b) / calculateGcd(a, b);
    }
}