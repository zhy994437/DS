 Paxos Consensus Algorithm Implementation

Overview
This project implements the Paxos consensus algorithm for the Adelaide Suburbs Council election simulation. Nine council members (M1-M9) use distributed consensus to elect a single council president despite network failures, high latency, and node crashes.

Project Structure

paxos/
├── CouncilMember.java    - Main implementation (Proposer, Acceptor, Learner roles)
├── NetworkManager.java   - TCP/IP socket communication manager
├── PaxosMessage.java     - Message protocol definition
└── PaxosTest.java        - Automated test suite
run_tests.sh              - Automated test execution script
README.md                 - This file
```

 Compilation

 Option 1: Using the test script (recommended)
```bash
chmod +x run_tests.sh
./run_tests.sh


### Option 2: Manual compilation
```bash
# Create bin directory
mkdir -p bin

# Compile all Java files
javac -d bin paxos/*.java

# Run tests
java -cp bin paxos.PaxosTest
```

## Running the Program

### Running Automated Tests
The easiest way to test all scenarios:
```bash
./run_tests.sh


This will:
1. Compile all source files
2. Run all test scenarios (1, 2, 3a, 3b, 3c)
3. Save output to `test_output.log`
4. Display pass/fail results

### Running Individual Members
To manually run council members:

1. Create a network configuration file `network.config`:
```
M1,localhost,9001
M2,localhost,9002
M3,localhost,9003
M4,localhost,9004
M5,localhost,9005
M6,localhost,9006
M7,localhost,9007
M8,localhost,9008
M9,localhost,9009
```

2. Launch members in separate terminals:
```bash
# Terminal 1 - M1 with reliable profile
java -cp bin paxos.CouncilMember M1 --profile reliable --config network.config

# Terminal 2 - M2 with latent profile
java -cp bin paxos.CouncilMember M2 --profile latent --config network.config

# Terminal 3 - M3 with failure profile
java -cp bin paxos.CouncilMember M3 --profile failure --config network.config

# Terminal 4-9 - M4-M9 with standard profile
java -cp bin paxos.CouncilMember M4 --profile standard --config network.config
# ... repeat for M5-M9
```

3. Propose candidates by typing into any member's terminal:
```
M5          # Propose M5 as president
status      # Check current state
crash       # Simulate crash
recover     # Recover from crash
profile latent  # Change network profile
quit        # Exit
```

## Message Protocol Design

### Message Format
Messages use a colon-delimited text format:
```
TYPE:SENDER:PROPOSAL_NUM:VALUE[:ACCEPTED_NUM:ACCEPTED_VAL]
```

### Message Types
- **PREPARE**: Phase 1a - Proposer requests permission to propose
- **PROMISE**: Phase 1b - Acceptor promises not to accept lower proposals
- **ACCEPT_REQUEST**: Phase 2a - Proposer requests acceptance of value
- **ACCEPTED**: Phase 2b - Acceptor confirms acceptance
- **LEARN**: Final phase - Broadcast consensus decision

### Proposal Number Format
Proposals use format `counter.memberId` (e.g., `3.1` for M1's 3rd proposal):
- Ensures total ordering of proposals
- Counter provides primary ordering
- Member ID breaks ties

### Example Message Flow
```
1. M4 → All: PREPARE:M4:1.4:
2. M1 → M4: PROMISE:M1:1.4::
3. M2 → M4: PROMISE:M2:1.4::
4. ... (majority of promises received)
5. M4 → All: ACCEPT_REQUEST:M4:1.4:M5
6. M1 → M4: ACCEPTED:M1:1.4:M5
7. ... (majority of accepts received)
8. M4 → All: LEARN:M4:1.4:M5
```

## Network Profiles

### RELIABLE (M1 behavior)
- Delay: 0-50ms
- Message drop rate: 0%
- Crash rate: 0%
- Ultra-responsive, instant communication

### LATENT (M2 behavior)
- Delay: 1000-3000ms
- Message drop rate: 15%
- Crash rate: 5%
- Poor connection from Adelaide Hills
- 20% chance of finding good connection at Sheoak Café (reduced to 0-100ms)

### FAILURE (M3 behavior)
- Delay: 500-1500ms
- Message drop rate: 30%
- Crash rate: 20%
- Intermittent connection, goes camping in Coorong

### STANDARD (M4-M9 behavior)
- Delay: 100-500ms
- Message drop rate: 5%
- Crash rate: 1%
- Normal business network conditions

## Test Scenarios

### Scenario 1: Ideal Network (10 points)
- **Setup**: All 9 members with RELIABLE profile
- **Test**: M4 proposes M5
- **Expected**: Quick consensus on M5

### Scenario 2: Concurrent Proposals (20 points)
- **Setup**: All 9 members with RELIABLE profile
- **Test**: M1 and M8 propose simultaneously
- **Expected**: Single winner chosen (M1 or M8)

### Scenario 3a: Standard Member Proposal (part of 20 points)
- **Setup**: M1(reliable), M2(latent), M3(failure), M4-M9(standard)
- **Test**: M4 proposes M6
- **Expected**: Consensus despite latency and failures

### Scenario 3b: Latent Member Proposal (part of 20 points)
- **Setup**: Same as 3a
- **Test**: M2 proposes M7
- **Expected**: Slower but successful consensus

### Scenario 3c: Failure and Recovery (part of 20 points)
- **Setup**: Same as 3a
- **Test**: M3 proposes M9, crashes, then M5 takes over
- **Expected**: System recovers, backup proposer succeeds

## Design Decisions

### 1. TCP/IP Socket Communication
- **Why**: Reliable, ordered delivery suitable for Paxos
- **Implementation**: Each member listens on unique port (9001-9009)
- **Thread-safe**: Uses ExecutorService and blocking queues

### 2. Network Simulation
- **Delay**: Simulated with `Thread.sleep()` before sending
- **Message loss**: Random drops based on profile
- **Crashes**: Random or manual crash simulation

### 3. State Management
- **Proposer state**: Tracks current proposal, promises, accepts
- **Acceptor state**: Tracks highest promised and accepted proposals
- **Learner state**: Stores final consensus value
- **Synchronization**: All state access is synchronized

### 4. Quorum Calculation
- **Majority**: `total_members / 2 + 1`
- **Example**: For 9 members, quorum = 5

### 5. Handling Failures
- Crashed nodes don't respond to messages
- Remaining nodes can reach consensus with majority
- Failed proposals can be retried by other members

## Key Implementation Details

### Thread Safety
- All state access protected by `synchronized` blocks
- Thread-safe collections (`ConcurrentHashMap`, `LinkedBlockingQueue`)
- Atomic counters for proposal numbers

### Message Ordering
- Messages processed sequentially through blocking queue
- Prevents race conditions in state updates
- Ensures proper Paxos semantics

### Error Handling
- Network failures handled gracefully (silent drops)
- Invalid messages logged but don't crash system
- Timeouts prevent indefinite waiting

## Expected Output

### Successful Consensus
```
M4 proposing M5 with 1.4
M1 promising 1.4
M2 promising 1.4
...
M4 got majority promises (5/9)
M1 accepting 1.4 = M5
...
M4 achieved consensus! M5
CONSENSUS: M5 elected as Council President!
```

### Concurrent Proposals
```
M1 proposing M1 with 1.1
M8 proposing M8 with 1.8
M2 promising 1.8
...
CONSENSUS: M8 elected as Council President!
```

## Troubleshooting

### Port Already in Use
```bash
# Find and kill process using port 9001
lsof -i :9001
kill <PID>
```

### Compilation Errors
```bash
# Ensure Java version 8 or higher
java -version
javac -version

# Clean and recompile
rm -rf bin
mkdir bin
javac -d bin paxos/*.java
```

### Tests Timeout
- Increase timeout values in `PaxosTest.java`
- Check network configuration file format
- Verify all ports are available




1. Manual Steps
1. cd to the src directory
2. mkdir -p bin
3. javac -d bin paxos/*.java
4. java -cp bin paxos.PaxosTest > output.log

2. Automatic Runtime
1. cd to src
2. ./run_tests.sh

## Requirements
- Java JDK 8 or higher
- Unix-like environment (Linux, macOS) or Windows with Bash
- Ports 9001-9009 available

## Assignment Compliance

This implementation fulfills all assignment requirements:
 All three Paxos roles implemented (Proposer, Acceptor, Learner)
TCP/IP socket communication
 Network simulation with configurable profiles
 All test scenarios (1, 2, 3a, 3b, 3c)
Automated test script (`run_tests.sh`)
 Comprehensive documentation
 Clean, modular, well-documented code

## Authors
Adelaide Suburbs Council Paxos Implementation

## License
Educational project for distributed systems course
