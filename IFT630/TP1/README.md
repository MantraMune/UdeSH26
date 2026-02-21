# TP1 - Système de Transport STS Sherbrooke

**IFT630 - Processus Concurrents et Parallélisme**  
**Université de Sherbrooke - Hiver 2026**

---

## Installation des Outils

### macOS
```bash
# Installer Homebrew si nécessaire
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Installer les outils
brew install openjdk@21
brew install maven
brew install python@3.11
```

### Ubuntu/Debian
```bash
sudo apt update
sudo apt install openjdk-21-jdk maven python3 python3-venv
```

### Windows
- Télécharger Java 21 : https://www.oracle.com/ca-fr/java/technologies/downloads/
- Télécharger Maven : https://maven.apache.org/download.cgi
- Python : https://www.python.org/downloads/

### Vérification
```bash
java -version    # Java 21
mvn -version     # Maven 3.9+
python3 --version # Python 3.8+
```

---

## Démarrage Rapide
```bash
# 1. Obtenir le projet 
# Par un clone sur le GitLab du cours
git clone git@depot.dinf.usherbrooke.ca:dinf/cours/H2026/IFT630/
cd tp1-sts-transport-sherbrooke
git switch --create main
touch README.md
git add README.md
git commit -m "add README"
git push --set-upstream origin main

#Par Turnin en téléchargeant le code sur la plateforme du cours
[Plateforme Turnin](https://turnin.dinf.usherbrooke.ca/)

#Si par téléchargement il faut décompresser l'archive
unzip tp1-sts-transport-sherbrooke.zip
cd tp1-sts-transport-sherbrooke

# 2. Compiler
make compile

# 3. Exécuter
make run

# 4. Évaluation automatique
make eval
```

---

## Développement

### Fichier à Compléter
`src/main/java/udes/fsci/info/ift630/transport/SystemeTransport.java`

**Les TODO à compléter**
- traiterPassagerMonte() - 4 opérations (capacité, refus, compteurs, alerte 90%)
- traiterPassagerDescend() - 4 opérations (décrément, descentes, arrêt, ensemble)
- traiterPaiement() - 2 opérations (revenus total, par méthode avec centimes)
- getTotalPassagersTransportes() - intersection des ensembles
- getNombrePassagers() - accès passagersParBus
- getRevenusTotal() - accès revenusTotal
- getPassagersMontes() - copie ensemble
- getPassagersDescendus() - copie ensemble


**TODO à implémenter:**
- Structures de données thread-safe
- Méthodes de traitement (8 méthodes)
- Getters thread-safe (12 méthodes)

### Commandes de Test
```bash
# Tests individuels
make test-simple      # Validation basique
make test-compteurs   # Race conditions (10 répétitions)
make test-capacite    # Gestion capacité
make test-deadlock    # Absence deadlock (10 répétitions)
make test-voyage      # Logique métier
make test-stress      # Robustesse (50 répétitions)

# Tous les tests
make test-all

# Évaluation finale
make eval
```

### Workflow Itératif
```bash
# 1. Coder un TODO dans SystemeTransport.java

# 2. Compiler
make compile

# 3. Tester le scenario simple
make test-simple

# 4. Corriger si nécessaire

# 5. Répéter pour tous les tests des autres scénarios
```

---

## Ordre d'éxécution des commandes
```bash
make clean && make compile && make run && make eval
```

## Barème de Notation

| Test | Points | Critères |
|------|--------|----------|
| Compilation | Go/No-go | Échec = note 0 |
| scenario_simple | 10 | Validation basique |
| scenario_compteurs | 20 + 2* | Race conditions + stabilité |
| scenario_capacite | 20 | Gestion capacité, refus, alertes |
| scenario_deadlock | 15 + 2* | Absence deadlock + stabilité |
| scenario_voyage | 15 | Logique métier |
| scenario_stress | 20 + 2* | Robustesse + stabilité |
| **TOTAL** | **100 + 6** | *Bonus stabilité |

### Pénalités
- Code ne compile pas: **note = 0**
- Deadlock: **-2 points**
- Invariants violés: **-2 points/test**
- Instabilité: **-2 points/test**

---

## Résultats Attendus
```bash
scenario_simple     : 7.50$, 3 transportés
scenario_compteurs  : 125.00$, 50 transportés (stable 10/10)
scenario_capacite   : 150.00$, 50 transportés, 10 refus
scenario_deadlock   : 25.00$, 10 transportés (pas timeout)
scenario_voyage     : 7.50$, 3 transportés
scenario_stress     : 500.00$, 150 transportés (stable 45+/50)
```

---

## Débogage

### Voir les logs
```bash
# Temps réel
tail -f logs/sts-tp1.log

# Nettoyer
rm -rf logs/*
```

### Changer niveau de log
Dans `src/main/resources/logback.xml`:
```xml
<root level="DEBUG">  <!-- INFO → DEBUG -->
```

### Rapport de correction
```bash
cat evaluation/rapport_correction.json
cat evaluation/rapport_correction.json | grep "note"
```

---

## Structure du Projet
```
tp1-sts-transport-sherbrooke/
├── src/main/
│   ├── java/.../transport/
│   │   ├── SystemeTransport.java    # À COMPLÉTER
│   │   ├── Main.java
│   │   ├── evenements/              # FOURNI
│   │   └── util/LecteurCSV.java     # FOURNI
│   └── resources/
│       ├── logback.xml
│       └── *.csv                     # Fichiers de données
├── Makefile
├── pom.xml
├── generer_csv_tests.py
├── corriger_tp1.py
└── expected_results.json
```

---

## Aide
```bash
make help    # Liste des commandes
```

**Contact:** Voir Moodle - Teams - Courriel pour support