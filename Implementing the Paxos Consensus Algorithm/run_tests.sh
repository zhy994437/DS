#!/bin/bash

# run_tests.sh
# Automated test script for Paxos implementation
# Compiles the code and runs all test scenarios

echo "======================================"
echo "Paxos Implementation Test Suite"
echo "======================================"
echo ""

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if Java is installed
if ! command -v javac &> /dev/null; then
    echo -e "${RED}ERROR: javac not found. Please install Java JDK${NC}"
    exit 1
fi

if ! command -v java &> /dev/null; then
    echo -e "${RED}ERROR: java not found. Please install Java JRE${NC}"
    exit 1
fi

echo -e "${YELLOW}Step 1: Cleaning previous builds...${NC}"
rm -rf bin
mkdir -p bin
echo "Done."
echo ""

echo -e "${YELLOW}Step 2: Compiling Java source files...${NC}"
javac -d bin paxos/*.java

if [ $? -ne 0 ]; then
    echo -e "${RED}ERROR: Compilation failed${NC}"
    exit 1
fi

echo -e "${GREEN}Compilation successful${NC}"
echo ""

echo -e "${YELLOW}Step 3: Running test suite...${NC}"
echo "This will take approximately 2-3 minutes"
echo ""

# Run the test suite and capture output
java -cp bin paxos.PaxosTest 2>&1 | tee test_output.log

if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}======================================"
    echo "Test suite completed successfully"
    echo "======================================${NC}"
    echo ""
    echo "Test output has been saved to: test_output.log"
else
    echo ""
    echo -e "${RED}======================================"
    echo "Test suite encountered errors"
    echo "======================================${NC}"
    exit 1
fi

echo ""
echo "To view detailed logs: cat test_output.log"
echo ""
