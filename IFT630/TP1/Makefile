# Makefile pour TP1 - Système de Transport Sherbrooke
# IFT630 - Université de Sherbrooke

.PHONY: help clean compile run eval test-all test-simple test-compteurs test-capacite test-deadlock test-voyage test-stress generate-csv results-only stability build install

# Configuration
MAIN_CLASS = udes.fsci.info.ift630.transport.Main
VENV = .venv
PYTHON = $(VENV)/bin/python3

# Par défaut : afficher l'aide
help:
	@echo "======================================================================"
	@echo "  TP1 - Système de Transport Concurrent"
	@echo "  IFT630 - Processus Concurrents et Parallélisme"
	@echo "======================================================================"
	@echo ""
	@echo "Commandes principales:"
	@echo "  make compile        - Compiler le code"
	@echo "  make run            - Exécuter et afficher les résultats"
	@echo "  make eval           - Évaluation automatique (Python)"
	@echo "  make clean          - Nettoyer le projet"
	@echo ""
	@echo "Tests individuels:"
	@echo "  make test-simple    - Test basique (validation)"
	@echo "  make test-compteurs - Test race conditions (10x)"
	@echo "  make test-capacite  - Test gestion capacité"
	@echo "  make test-deadlock  - Test absence deadlock (10x)"
	@echo "  make test-voyage    - Test logique métier"
	@echo "  make test-stress    - Test robustesse (50x)"
	@echo "  make test-all       - Exécuter tous les tests"
	@echo ""
	@echo "Autres commandes:"
	@echo "  make generate-csv   - Générer les fichiers CSV de test"
	@echo "  make build          - Build complet (clean + compile + test-all)"
	@echo "  make install        - Installation et validation du projet"
	@echo ""

# Nettoyer le projet
clean:
	@echo "Nettoyage du projet..."
	@mvn clean -q
	@rm -rf $(VENV) logs/* evaluation/*
	@find . -type d -name __pycache__ -exec rm -rf {} + 2>/dev/null || true
	@echo "Projet nettoyé"

# Compiler le code
compile:
	@echo "Compilation du code..."
	@mvn compile -q
	@echo "Compilation réussie"

# Exécuter et afficher les résultats
run:
	@echo "Exécution de tous les scénarios..."
	@mvn exec:java -Dexec.mainClass="$(MAIN_CLASS)" \
	               -Dexec.args="src/main/resources/scenario_simple.csv"
	@mvn exec:java -Dexec.mainClass="$(MAIN_CLASS)" \
	               -Dexec.args="src/main/resources/scenario_compteurs.csv"
	@mvn exec:java -Dexec.mainClass="$(MAIN_CLASS)" \
	               -Dexec.args="src/main/resources/scenario_capacite.csv"
	@mvn exec:java -Dexec.mainClass="$(MAIN_CLASS)" \
	               -Dexec.args="src/main/resources/scenario_deadlock.csv"
	@mvn exec:java -Dexec.mainClass="$(MAIN_CLASS)" \
	               -Dexec.args="src/main/resources/scenario_voyage.csv"
	@mvn exec:java -Dexec.mainClass="$(MAIN_CLASS)" \
	               -Dexec.args="src/main/resources/scenario_stress.csv"
	@echo "Exécution terminée. Fichiers de résultats générés dans logs/"

# Évaluation automatique avec environnement Python
eval: $(VENV)
	@echo "======================================================================"
	@echo "ÉVALUATION AUTOMATIQUE"
	@echo "======================================================================"
	@$(PYTHON) corriger_tp1.py .
	@echo ""
	@if [ -f evaluation/rapport_correction.json ]; then \
		echo "Rapport sauvegardé: evaluation/rapport_correction.json"; \
	fi

# Créer l'environnement virtuel Python
$(VENV):
	@echo "Création environnement Python..."
	@python3 -m venv $(VENV)
	@$(PYTHON) -m pip install --upgrade pip -q
	@echo "Environnement Python créé"

# Exécuter tous les tests
test-all: test-simple test-compteurs test-capacite test-deadlock test-voyage test-stress
	@echo ""
	@echo "======================================================================"
	@echo "Tous les tests terminés"
	@echo "======================================================================"

# Test 1: Simple (validation basique)
test-simple:
	@echo ""
	@echo "======================================================================"
	@echo "TEST 1: scenario_simple.csv - Validation basique"
	@echo "======================================================================"
	@mvn exec:java -Dexec.mainClass="$(MAIN_CLASS)" \
	               -Dexec.args="scenario_simple.csv" 2>&1 | \
	               grep -A 100 "RESULTATS"

# Test 2: Compteurs (race conditions) - 10 répétitions
test-compteurs:
	@echo ""
	@echo "======================================================================"
	@echo "TEST 2: scenario_compteurs.csv - Race conditions (10 répétitions)"
	@echo "======================================================================"
	@echo "Test de stabilité (résultats doivent être identiques):"
	@echo ""
	@SUCCESS=0; \
	FIRST_RESULT=""; \
	for i in 1 2 3 4 5 6 7 8 9 10; do \
		echo "--- Exécution $$i/10 ---"; \
		RESULT=$$(mvn exec:java -Dexec.mainClass="$(MAIN_CLASS)" \
		              -Dexec.args="scenario_compteurs.csv" -q 2>&1 | \
		              grep "Reponse:" | head -4 | \
		              sed '1s/Reponse:/Revenus:/' | \
		              sed '2s/Reponse:/Transportés:/' | \
		              sed '3s/Reponse:/Montées:/' | \
		              sed '4s/Reponse:/Descentes:/'); \
		echo "$$RESULT"; \
		if [ $$i -eq 1 ]; then \
			FIRST_RESULT="$$RESULT"; \
		fi; \
		if echo "$$RESULT" | grep -q "Revenus: 125.00"; then \
			SUCCESS=$$((SUCCESS+1)); \
		fi; \
	done; \
	echo ""; \
	echo "Résultat: $$SUCCESS/10 exécutions correctes"; \
	if [ $$SUCCESS -eq 10 ]; then \
		echo "✓ STABLE - Bonus stabilité: +2 points"; \
	else \
		echo "✗ INSTABLE - Pénalité: -5 points"; \
	fi

# Test 3: Capacité (gestion capacité)
test-capacite:
	@echo ""
	@echo "======================================================================"
	@echo "TEST 3: scenario_capacite.csv - Gestion capacité"
	@echo "======================================================================"
	@mvn exec:java -Dexec.mainClass="$(MAIN_CLASS)" \
	               -Dexec.args="scenario_capacite.csv" 2>&1 | \
	               grep -A 100 "RESULTATS"

# Test 4: Deadlock (absence deadlock) - 10 répétitions
test-deadlock:
	@echo ""
	@echo "======================================================================"
	@echo "TEST 4: scenario_deadlock.csv - Absence deadlock (10 répétitions)"
	@echo "======================================================================"
	@echo "Test de timeout (max 30s par exécution):"
	@echo ""
	@SUCCESS=0; \
	for i in 1 2 3 4 5 6 7 8 9 10; do \
		echo -n "Exécution $$i/10... "; \
		if timeout 30 mvn exec:java -Dexec.mainClass="$(MAIN_CLASS)" \
		                         -Dexec.args="scenario_deadlock.csv" -q > /dev/null 2>&1; then \
			echo "OK (pas de timeout)"; \
			SUCCESS=$$((SUCCESS+1)); \
		else \
			echo "TIMEOUT - Deadlock détecté!"; \
		fi; \
	done; \
	echo ""; \
	echo "Résultat: $$SUCCESS/10 exécutions sans timeout"; \
	if [ $$SUCCESS -eq 10 ]; then \
		echo "✓ STABLE - Bonus stabilité: +2 points"; \
	else \
		echo "✗ INSTABLE - Deadlock présent"; \
	fi; \
	echo ""; \
	echo "Voir résultats détaillés: make run TEST=scenario_deadlock.csv"

# Test 5: Voyage (logique métier)
test-voyage:
	@echo ""
	@echo "======================================================================"
	@echo "TEST 5: scenario_voyage.csv - Logique métier"
	@echo "======================================================================"
	@mvn exec:java -Dexec.mainClass="$(MAIN_CLASS)" \
	               -Dexec.args="scenario_voyage.csv" 2>&1 | \
	               grep -A 100 "RESULTATS"

# Test 6: Stress (robustesse) - 50 répétitions
test-stress:
	@echo ""
	@echo "======================================================================"
	@echo "TEST 6: scenario_stress.csv - Robustesse (50 répétitions)"
	@echo "======================================================================"
	@echo "Test de stabilité sur 50 exécutions (peut prendre 1-2 minutes)..."
	@echo ""
	@SUCCESS=0; \
	TOTAL=50; \
	for i in $$(seq 1 $$TOTAL); do \
		mvn exec:java -Dexec.mainClass="$(MAIN_CLASS)" \
		              -Dexec.args="scenario_stress.csv" -q 2>&1 | \
		              grep -q "Reponse: 500.00" && SUCCESS=$$((SUCCESS+1)) || true; \
		if [ $$((i % 10)) -eq 0 ]; then \
			echo "  Progression: $$i/$$TOTAL ($$SUCCESS réussites)"; \
		fi; \
	done; \
	echo ""; \
	echo "Résultat: $$SUCCESS/$$TOTAL exécutions correctes"; \
	if [ $$SUCCESS -ge 45 ]; then \
		echo "✓ STABLE - Bonus stabilité: +2 points"; \
	else \
		echo "✗ INSTABLE - Pénalité: -5 points"; \
	fi; \
	echo ""; \
	echo "Voir résultats détaillés: make run TEST=scenario_stress.csv"

# Générer les fichiers CSV
generate-csv:
	@echo "Génération des fichiers CSV de test..."
	@if [ -f generer_csv_tests.py ]; then \
		python3 generer_csv_tests.py src/main/resources/; \
	else \
		echo "ERREUR: generer_csv_tests.py non trouvé"; \
	fi

# Afficher juste les résultats (sans logs Maven)
results-only:
	@mvn exec:java -Dexec.mainClass="$(MAIN_CLASS)" \
	               -Dexec.args="$(TEST)" 2>&1 | \
	               grep -E "(Question|Reponse|Conservation|passagers|refus)"

# Vérifier la stabilité d'un test spécifique
stability:
	@if [ -z "$(TEST)" ]; then \
		echo "Usage: make stability TEST=scenario_compteurs.csv RUNS=10"; \
		exit 1; \
	fi; \
	RUNS=$${RUNS:-10}; \
	SUCCESS=0; \
	echo "Test de stabilité: $(TEST) ($$RUNS exécutions)"; \
	for i in $$(seq 1 $$RUNS); do \
		mvn exec:java -Dexec.mainClass="$(MAIN_CLASS)" \
		              -Dexec.args="$(TEST)" > /tmp/exec$$i.txt 2>&1 && \
		SUCCESS=$$((SUCCESS+1)) || true; \
	done; \
	echo "Succès: $$SUCCESS/$$RUNS"; \
	if [ $$SUCCESS -eq $$RUNS ]; then \
		echo "STABLE"; \
	else \
		echo "INSTABLE"; \
	fi

# Build complet: clean + compile + test
build: clean compile test-all
	@echo ""
	@echo "Build complet terminé"

# Installation rapide du projet
install:
	@echo "Installation du projet TP1..."
	@echo ""
	@echo "1. Compilation..."
	@mvn clean compile -q
	@echo ""
	@echo "2. Test de validation..."
	@make test-simple > /dev/null 2>&1 && echo "✓ Test simple OK" || echo "✗ Test simple ÉCHEC"
	@echo ""
	@echo "Installation terminée!"
	@echo "Utiliser 'make help' pour voir les commandes disponibles"
