import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Calculator Remote Interface
 * Defines the remote operations for a stack-based calculator service
 * All methods can throw RemoteException for RMI communication errors
 */
public interface Calculator extends Remote {
    
    /**
     * Push an integer value onto the top of the stack
     * @param val The integer value to push
     * @throws RemoteException If RMI communication fails
     */
    void pushValue(int val) throws RemoteException;    
    /**
     * Push an operation onto the stack and execute it
     * Pops all values from stack, applies operation, pushes result back
     * @param operator The operation to perform: "min", "max", "lcm", "gcd"
     * @throws RemoteException If RMI communication fails or invalid operation
     */
    void pushOperation(String operator) throws RemoteException;    
    /**
     * Pop and return the top value from the stack
     * @return The integer value at the top of the stack
     * @throws RemoteException If stack is empty or RMI communication fails
     */
    int pop() throws RemoteException;    
    /**
     * Check if the stack is empty
     * @return true if stack is empty, false otherwise
     * @throws RemoteException If RMI communication fails
     */
    boolean isEmpty() throws RemoteException;    
    /**
     * Pop the top value after waiting for specified milliseconds
     * @param millis Delay time in milliseconds before popping
     * @return The integer value at the top of the stack after delay
     * @throws RemoteException If stack is empty, interrupted, or RMI communication fails
     */
    int delayPop(int millis) throws RemoteException;
}