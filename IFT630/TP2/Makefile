.PHONY: compile run clean test install

compile:
	mvn clean compile

run:
	mvn exec:java -Dexec.mainClass="ca.usherbrooke.ift630.tp2.Main" -Dexec.args="scenarios/scenario-seed-12345.csv"

run-q1:
	mvn exec:java -Dexec.mainClass="ca.usherbrooke.ift630.tp2.Main" -Dexec.args="--question 1"

run-q2:
	mvn exec:java -Dexec.mainClass="ca.usherbrooke.ift630.tp2.Main" -Dexec.args="--question 2"

run-q3:
	mvn exec:java -Dexec.mainClass="ca.usherbrooke.ift630.tp2.Main" -Dexec.args="--question 3"

run-q4:
	mvn exec:java -Dexec.mainClass="ca.usherbrooke.ift630.tp2.Main" -Dexec.args="--question 4"

run-q5:
	mvn exec:java -Dexec.mainClass="ca.usherbrooke.ift630.tp2.Main" -Dexec.args="--question 5"

run-q6:
	mvn exec:java -Dexec.mainClass="ca.usherbrooke.ift630.tp2.Main" -Dexec.args="--question 6"

run-q7:
	mvn exec:java -Dexec.mainClass="ca.usherbrooke.ift630.tp2.Main" -Dexec.args="--question 7"

test:
	mvn test

test-q2:
	mvn test -Dtest="Q2BusThreadTest"

test-q3:
	mvn test -Dtest="Q3FairnessTest"

test-q4:
	mvn test -Dtest="Q4ConsommateursTest"

test-q5:
	mvn test -Dtest="Q5RWLockTest"

test-q6:
	mvn test -Dtest="Q6DeadlockTest"

test-q7:
	mvn test -Dtest="Q7DashboardTest"

clean:
	mvn clean
	rm -rf logs/*.log results/*.csv

install:
	chmod +x install.sh
	./install.sh
