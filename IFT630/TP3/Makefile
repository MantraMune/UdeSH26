# Makefile pour TP3 - Systeme de Transport Distribue
# IFT630 - Universite de Sherbrooke

# Variables
JAR_FILE = target/tp3-transport-distribue-1.0-SNAPSHOT.jar
CONFIG_FILE = config.json
WORKERS ?= 4
DURATION ?= 30
MONTE_CARLO ?= 10000
MAIN_CLASS = ca.usherbrooke.ift630.tp3.Main
EVAL_CLASS = ca.usherbrooke.ift630.tp3.test.EvaluationRunner

.PHONY: all install clean compile run-threads run-processes compare eval help test scenario1 scenario2 scenario3 scenarios

# Cible par defaut
all: compile

# Installation des dependances Maven
install:
	@echo "============================================================"
	@echo "Installation des dependances..."
	@echo "============================================================"
	@mvn dependency:resolve
	@echo ""
	@echo "Installation terminee"
	@echo ""

# Nettoyage complet
clean:
	@echo "============================================================"
	@echo "Nettoyage du projet..."
	@echo "============================================================"
	@mvn clean -q
	@rm -rf logs/scenarios
	@echo ""
	@echo "Nettoyage termine"
	@echo ""

# Compilation du projet
compile:
	@echo "============================================================"
	@echo "Compilation du projet..."
	@echo "============================================================"
	@mvn compile -q
	@echo ""
	@echo "Compilation terminee"
	@echo ""

# Package JAR
package: compile
	@echo "============================================================"
	@echo "Creation du JAR..."
	@echo "============================================================"
	@mvn package -q -DskipTests
	@echo ""
	@echo "JAR cree: $(JAR_FILE)"
	@echo ""

# Execution en mode threads
run-threads: package
	@echo ""
	@echo "============================================================"
	@echo "EXECUTION MODE THREADS"
	@echo "============================================================"
	@echo "Workers: $(WORKERS), Duree: $(DURATION)s, Monte Carlo: $(MONTE_CARLO)"
	@echo ""
	@java -jar $(JAR_FILE) --mode=threads --workers=$(WORKERS) --duration=$(DURATION) --monte-carlo=$(MONTE_CARLO)

# Execution en mode processus
run-processes: package
	@echo ""
	@echo "============================================================"
	@echo "EXECUTION MODE PROCESSUS"
	@echo "============================================================"
	@echo "Workers: $(WORKERS), Duree: $(DURATION)s, Monte Carlo: $(MONTE_CARLO)"
	@echo ""
	@java -jar $(JAR_FILE) --mode=processes --workers=$(WORKERS) --duration=$(DURATION) --monte-carlo=$(MONTE_CARLO)

# Comparaison threads vs processus
compare: package
	@echo ""
	@echo "============================================================"
	@echo "COMPARAISON THREADS vs PROCESSUS"
	@echo "============================================================"
	@echo ""
	@echo ">>> Execution mode THREADS..."
	@java -jar $(JAR_FILE) --mode=threads --workers=$(WORKERS) --duration=$(DURATION) --monte-carlo=$(MONTE_CARLO) > /logs/tp3-threads.log 2>&1
	@echo "Mode threads termine"
	@echo ""
	@echo ">>> Execution mode PROCESSUS..."
	@java -jar $(JAR_FILE) --mode=processes --workers=$(WORKERS) --duration=$(DURATION) --monte-carlo=$(MONTE_CARLO) > /logs/tp3-processes.log 2>&1
	@echo "Mode processus termine"
	@echo ""
	@echo "============================================================"
	@echo "RESULTATS"
	@echo "============================================================"
	@echo ""
	@echo "=== MODE THREADS ==="
	@grep -A 20 "RAPPORT DE PERFORMANCE" /tmp/tp3-threads.log | head -25 || true
	@echo ""
	@echo "=== MODE PROCESSUS ==="
	@grep -A 20 "RAPPORT DE PERFORMANCE" /tmp/tp3-processes.log | head -25 || true
	@echo ""
	@echo "Logs complets: /tmp/tp3-threads.log et /tmp/tp3-processes.log"
	@echo ""

# Evaluation automatique avec calcul de la note et tests unitaires
eval: compile scenarios
	@echo ""
	@echo "============================================================"
	@echo "EVALUATION AUTOMATIQUE - TP3"
	@echo "============================================================"
	@echo ""
	@mvn test
	@echo ""
	@echo "============================================================"
	@echo "CALCUL DE LA NOTE"
	@echo "============================================================"
	@mvn exec:java -Dexec.mainClass="$(EVAL_CLASS)" -Dexec.classpathScope=test -q
	@echo ""

# Tests unitaires uniquement
test: compile
	@echo ""
	@echo "============================================================"
	@echo "EXECUTION DES TESTS UNITAIRES"
	@echo "============================================================"
	@echo ""
	@mvn test

# Verification de la configuration
check-config:
	@echo ""
	@echo "============================================================"
	@echo "Verification configuration MQTT"
	@echo "============================================================"
	@if [ -f $(CONFIG_FILE) ]; then \
		echo "Fichier $(CONFIG_FILE) trouve:"; \
		echo ""; \
		cat $(CONFIG_FILE); \
	else \
		echo "ERREUR: Fichier $(CONFIG_FILE) introuvable"; \
		exit 1; \
	fi
	@echo ""

# Scenarios pedagogiques

scenario1: package
	@echo ""
	@echo "============================================================"
	@echo "SCENARIO 1 : OVERHEAD IPC EVIDENT"
	@echo "============================================================"
	@echo "Calculs simples uniquement (pas de Monte Carlo)"
	@echo "Objectif : montrer que overhead IPC > gain parallelisme"
	@echo ""
	@echo ">>> Mode THREADS..."
	@java -jar $(JAR_FILE) --mode=threads --workers=4 --duration=15 --monte-carlo=0 > logs/scenarios/scenario1-threads.log 2>&1
	@echo ">>> Mode PROCESSUS..."
	@java -jar $(JAR_FILE) --mode=processes --workers=4 --duration=15 --monte-carlo=0 > logs/scenarios/scenario1-processes.log 2>&1
	@echo ""
	@echo "RESULTATS SCENARIO 1:"
	@echo ""
	@grep "Temps total" logs/scenario1-threads.log | head -1 || true
	@grep "Temps total" logs/scenario1-processes.log | head -1 || true
	@echo ""
	@echo "Attendu : Processus plus lent (overhead IPC significatif)"
	@echo ""

scenario2: package
	@echo ""
	@echo "============================================================"
	@echo "SCENARIO 2 : PARALLELISME BENEFIQUE"
	@echo "============================================================"
	@echo "Monte Carlo intensif (20000 iterations)"
	@echo "Objectif : montrer que gain parallelisme > overhead IPC"
	@echo ""
	@echo ">>> Mode THREADS..."
	@java -jar $(JAR_FILE) --mode=threads --workers=4 --duration=30 --monte-carlo=20000 2>&1 | tee logs/scenarios/scenario2-threads.log > /tmp/scenario2-threads.log
	@echo ">>> Mode PROCESSUS..."
	@java -jar $(JAR_FILE) --mode=processes --workers=4 --duration=30 --monte-carlo=20000 2>&1 | tee logs/scenarios/scenario2-processes.log > /tmp/scenario2-processes.log
	@echo ""
	@echo "RESULTATS SCENARIO 2:"
	@echo ""
	@grep "Temps total" /tmp/scenario2-threads.log | head -1 || true
	@grep "Temps total" /tmp/scenario2-processes.log | head -1 || true
	@echo ""
	@echo "Attendu : Overhead IPC compense par calcul intensif"
	@echo ""

scenario3: package
	@echo ""
	@echo "============================================================"
	@echo "SCENARIO 3 : SCALABILITE"
	@echo "============================================================"
	@echo "Test avec 1, 2, 4, 8 workers (mode threads)"
	@echo "Objectif : montrer speedup et efficacite"
	@echo ""
	@for workers in 1 2 4 8; do \
		echo ">>> Test avec $$workers worker(s)..."; \
		java -jar $(JAR_FILE) --mode=threads --workers=$$workers --duration=20 --monte-carlo=5000 2>&1 | tee logs/scenarios/scenario3-$$workers.log > /tmp/scenario3-$$workers.log; \
		echo "Workers: $$workers"; \
		grep "Temps total" /tmp/scenario3-$$workers.log | head -1 || true; \
		echo ""; \
	done
	@echo ""
	@echo "ANALYSE SCALABILITE:"
	@echo "Comparer les temps : speedup ideal = temps(1) / temps(N)"
	@echo "Efficacite = speedup / N workers"
	@echo ""

# Tous les scenarios en sequence
scenarios:
	@mkdir -p logs/scenarios
	@$(MAKE) scenario1 scenario2 scenario3
	@echo ""
	@echo "============================================================"
	@echo "TOUS LES SCENARIOS TERMINES"
	@echo "============================================================"
	@echo "Logs disponibles dans logs/scenarios/ et /tmp/scenario*.log"
	@echo ""

# Afficher l'aide
help:
	@echo ""
	@echo "============================================================"
	@echo "TP3 - Systeme de Transport Distribue - Aide"
	@echo "============================================================"
	@echo ""
	@echo "WORKFLOW PRINCIPAL:"
	@echo "  make install        - Installe les dependances Maven"
	@echo "  make clean          - Nettoie les fichiers compiles"
	@echo "  make compile        - Compile le code source"
	@echo "  make run-threads    - Execute en mode threads"
	@echo "  make run-processes  - Execute en mode processus"
	@echo "  make compare        - Compare threads vs processus"
	@echo "  make eval           - Lance les tests et calcule la note"
	@echo ""
	@echo "SCENARIOS PEDAGOGIQUES:"
	@echo "  make scenario1      - Overhead IPC evident (calculs simples)"
	@echo "  make scenario2      - Parallelisme benefique (Monte Carlo)"
	@echo "  make scenario3      - Scalabilite (1, 2, 4, 8 workers)"
	@echo "  make scenarios      - Execute les 3 scenarios en sequence"
	@echo ""
	@echo "AUTRES CIBLES:"
	@echo "  make test           - Execute les tests unitaires"
	@echo "  make package        - Cree le JAR executable"
	@echo "  make check-config   - Verifie la configuration MQTT"
	@echo "  make help           - Affiche cette aide"
	@echo ""
	@echo "VARIABLES PERSONNALISABLES:"
	@echo "  WORKERS       - Nombre de workers (defaut: 4)"
	@echo "  DURATION      - Duree en secondes (defaut: 30)"
	@echo "  MONTE_CARLO   - Iterations Monte Carlo (defaut: 10000)"
	@echo ""
	@echo "EXEMPLES:"
	@echo "  make run-threads WORKERS=8 DURATION=60"
	@echo "  make run-processes WORKERS=4 MONTE_CARLO=20000"
	@echo "  make compare WORKERS=4 DURATION=30"
	@echo "  make scenario1"
	@echo "  make scenarios"
	@echo "  make eval"
	@echo ""
	@echo "============================================================"
	@echo ""
