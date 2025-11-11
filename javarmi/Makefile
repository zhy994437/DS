JAVAC = javac
JAVA = java
RMIREGISTRY = rmiregistry

SRC = Calculator.java CalculatorClient.java CalculatorClientTest.java CalculatorImplementation.java CalculatorServer.java MultiClientTest.java
CLASSES = $(SRC:.java=.class)

.PHONY: all clean start-rmiregistry start-server test-single test-multi

all: $(CLASSES)

%.class: %.java
	$(JAVAC) $<

clean:
	rm -f *.class

start-rmiregistry:
	@echo "Starting RMI registry on port 1099..."
	@$(RMIREGISTRY) 1099 & \
	sleep 2

start-server: start-rmiregistry
	@echo "Starting Calculator server..."
	@$(JAVA) CalculatorServer & \
	sleep 2

test-single: start-server
	@echo "Running single client test..."
	@$(JAVA) CalculatorClient

test-multi: start-server
	@echo "Running multi-client test..."
	@$(JAVA) MultiClientTest

stop:
	@echo "Stopping all java processes (use with caution)..."
	@pkill -f CalculatorServer || true
	@pkill -f rmiregistry || true
