package paxos;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Manages network communication using TCP sockets for Paxos protocol.
 * 
 * Responsibilities:
 * - Load network configuration (member IDs, hosts, ports)
 * - Accept incoming connections from other members
 * - Send messages to specific members or broadcast to all
 * - Queue and process incoming messages asynchronously
 * 
 * Thread Safety:
 * - Uses thread-safe collections and executor services
 * - Message processing is serialized through a blocking queue
 */
public class NetworkManager {
    
    private final String id;
    private final int port;
    
    // Thread-safe maps for member configuration
    private final Map<String, String> hosts = new ConcurrentHashMap<>();
    private final Map<String, Integer> ports = new ConcurrentHashMap<>();
    
    // Thread pool for handling concurrent connections
    private final ExecutorService executor = Executors.newFixedThreadPool(5);
    
    // Queue for incoming messages to ensure ordered processing
    private final BlockingQueue<PaxosMessage> queue = new LinkedBlockingQueue<>();
    
    // Callback to handle processed messages
    private final Consumer<PaxosMessage> handler;
    
    // Server socket for accepting connections
    private ServerSocket server;
    
    // Flag to control running state
    private volatile boolean running = false;
    
    public NetworkManager(String id, int port, Consumer<PaxosMessage> handler) {
        this.id = id;
        this.port = port;
        this.handler = handler;
    }
    
    /**
     * Load network configuration from file.
     * Format: memberId,host,port (one per line)
     * 
     * @param file Configuration file path
     * @throws IOException if file cannot be read
     */
    public void loadConfig(String file) throws IOException {
        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                
                // Skip empty lines and comments
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                String[] parts = line.split(",");
                if (parts.length == 3) {
                    String memberId = parts[0].trim();
                    String host = parts[1].trim();
                    int memberPort = Integer.parseInt(parts[2].trim());
                    
                    hosts.put(memberId, host);
                    ports.put(memberId, memberPort);
                }
            }
        }
        System.out.println("Loaded config for " + hosts.size() + " members");
    }
    
    /**
     * Start the network manager.
     * Begins accepting connections and processing messages.
     * 
     * @throws IOException if server socket cannot be created
     */
    public void start() throws IOException {
        server = new ServerSocket(port);
        running = true;
        
        System.out.println(id + " listening on port " + port);
        
        // Start connection acceptor thread
        new Thread(this::acceptLoop, "AcceptLoop-" + id).start();
        
        // Start message processor thread
        new Thread(this::processLoop, "ProcessLoop-" + id).start();
    }
    
    /**
     * Stop the network manager gracefully.
     * Closes server socket and shuts down executor.
     */
    public void stop() {
        running = false;
        
        // Close server socket
        try {
            if (server != null && !server.isClosed()) {
                server.close();
            }
        } catch (IOException e) {
            // Ignore exceptions during shutdown
        }
        
        // Shutdown executor gracefully
        executor.shutdown();
        try {
            if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Send a message to a specific target member.
     * Uses TCP socket connection for reliable delivery.
     * 
     * @param target Target member ID
     * @param msg Message to send
     * @return true if sent successfully, false otherwise
     */
    public boolean send(String target, PaxosMessage msg) {
        // Validate target exists in configuration
        if (!hosts.containsKey(target)) {
            System.err.println("Unknown target: " + target);
            return false;
        }
        
        String host = hosts.get(target);
        int targetPort = ports.get(target);
        
        // Create connection and send message
        try (Socket s = new Socket(host, targetPort);
             PrintWriter out = new PrintWriter(s.getOutputStream(), true)) {
            
            out.println(msg.serialize());
            return true;
            
        } catch (IOException e) {
            // Silently fail - network issues are expected in distributed systems
            // In production, might want to log or retry
            return false;
        }
    }
    
    /**
     * Broadcast a message to all other members.
     * 
     * @param msg Message to broadcast
     * @return Number of members successfully reached
     */
    public int broadcast(PaxosMessage msg) {
        int count = 0;
        
        // Send to all members except self
        for (String target : hosts.keySet()) {
            if (!target.equals(id) && send(target, msg)) {
                count++;
            }
        }
        
        return count;
    }
    
    /**
     * Get total number of members in the network.
     * Used for calculating quorum/majority.
     * 
     * @return Total member count
     */
    public int getMemberCount() {
        return hosts.size();
    }
    
    /**
     * Get all member IDs in the network.
     * Used for broadcasting and determining all participants.
     * 
     * @return Set of all member IDs
     */
    public Set<String> getMemberIds() {
        return new HashSet<>(hosts.keySet());
    }
    
    /**
     * Accept incoming connections in a loop.
     * Each connection is handled in a separate thread from the pool.
     */
    private void acceptLoop() {
        while (running && server != null && !server.isClosed()) {
            try {
                Socket client = server.accept();
                
                // Handle each connection asynchronously
                executor.execute(() -> handleConnection(client));
                
            } catch (SocketException e) {
                // Expected when server socket is closed
                if (running) {
                    System.err.println("Socket error: " + e.getMessage());
                }
            } catch (IOException e) {
                if (running) {
                    System.err.println("Accept error: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Handle a single client connection.
     * Read message, deserialize it, and add to processing queue.
     * 
     * @param s Client socket
     */
    private void handleConnection(Socket s) {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(s.getInputStream()))) {
            
            String msg = in.readLine();
            if (msg != null && !msg.isEmpty()) {
                try {
                    PaxosMessage message = PaxosMessage.deserialize(msg);
                    
                    // Add to queue for ordered processing
                    if (!queue.offer(message)) {
                        System.err.println("Message queue full, dropping message");
                    }
                } catch (IllegalArgumentException e) {
                    System.err.println("Invalid message format: " + msg);
                }
            }
            
        } catch (IOException e) {
            // Ignore connection errors - common in distributed systems
        } finally {
            // Ensure socket is closed
            try {
                if (s != null && !s.isClosed()) {
                    s.close();
                }
            } catch (IOException e) {
                // Ignore
            }
        }
    }
    
    /**
     * Process messages from the queue in order.
     * Ensures messages are handled sequentially to avoid race conditions.
     */
    private void processLoop() {
        while (running) {
            try {
                // Poll with timeout to allow checking running flag
                PaxosMessage msg = queue.poll(1, TimeUnit.SECONDS);
                
                if (msg != null && handler != null) {
                    // Invoke the message handler callback
                    handler.accept(msg);
                }
                
            } catch (InterruptedException e) {
                // Thread interrupted, exit loop
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                // Log errors but continue processing
                System.err.println("Message processing error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
