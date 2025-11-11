Testing
The project includes three levels of testing:
1. CalculatorClient.java

Basic demonstration of all calculator features
Shows expected usage patterns
Good for manual verification

2. CalculatorClientTest.java

Comprehensive automated test suite
Tests all operations with various inputs
Includes edge case testing
Validates error handling

3. MultiClientTest.java

Concurrent client testing
Stress testing with rapid operations
Thread safety validation
Configurable client count



Quick Start Guide
1. Compilation
Compile all Java source files:
bashjavac *.java
2. Start RMI Registry
Start the RMI registry on default port 1099:
bash# On Linux/macOS
rmiregistry 1099 &

# On Windows
start rmiregistry 1099

# Alternative: let server create registry automatically
3. Start Calculator Server
Launch the calculator server:
bashjava CalculatorServer
Expected output:
Starting Calculator RMI Server...
Calculator implementation created successfully
Calculator Server ready!
Service bound to: CalculatorService
Registry port: 1099
Server is running... Press Ctrl+C to stop
4. Run Client Tests
In separate terminal windows, run the various clients:
Basic Client Demo
bashjava CalculatorClient
Comprehensive Test Suite
bashjava CalculatorClientTest
Multi-Client Concurrency Test
bash# Default 5 clients
java MultiClientTest

# Specify number of clients
java MultiClientTest 10