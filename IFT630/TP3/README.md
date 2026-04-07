# TP3 - Systeme de Transport Distribue

**IFT630 - Processus Concurrents et Parallelisme**  
Universite de Sherbrooke



## Installation des Outils

### macOS
```bash
# Installer Homebrew si nécessaire
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Installer les outils
brew install openjdk@21
brew install maven
```

### Ubuntu/Debian
```bash
sudo apt update
sudo apt install openjdk-21-jdk maven
```

### Windows
- Télécharger Java 21 : https://www.oracle.com/ca-fr/java/technologies/downloads/
- Télécharger Maven : https://maven.apache.org/download.cgi

### Vérification
```bash
java -version    # Java 21
mvn -version     # Maven 3.9+
```

---

## Démarrage Rapide
```bash
# 1. Obtenir le projet 
# Par un clone sur le GitLab du cours
git clone git@depot.dinf.usherbrooke.ca:dinf/cours/H2026/IFT630/
cd tp3-sts-transport-sherbrooke
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

## Description

Ce projet implemente un systeme de traitement distribue d'evenements de transport urbain avec deux modes d'execution :
- **Mode threads** : Tous les workers dans la meme JVM, communication via `BlockingQueue`
- **Mode processus** : Chaque worker dans sa propre JVM, communication via `sockets TCP localhost`

L'objectif pedagogique est de **mesurer et comparer les couts reels du parallelisme** (overhead IPC, speedup, efficacite).

## Architecture

```
+---------------------------------------------------------+
|                  Broker MQTT                            |
|             (RabbitMQ/Mosquitto)                        |
|                                                         |
|  Topics:  realtime  |  batch  |  interval              |
+---------------------------+-----------------------------+
                            |
                            | Messages JSON
                            v
+---------------------------------------------------------+
|                  MQTTClient                             |
|                                                         |
+---------------------------+-----------------------------+
                            |
                      +-----+-----+
                      |           |
                      v           v
                 MODE THREADS   MODE PROCESSUS
                      |               |
                      v               v
                BlockingQueue    Socket IPC
                      |               |
                      v               v
                ThreadWorker    ProcessWorker
                 (1 thread)     (1 processus)
                      |               |
                      +-------+-------+
                              |
                              v
                  Calculs distribues:
                  - Agregations simples
                  - Monte Carlo
                  - Detection anomalies
```

## Calculs Implementes

### Agregations Simples (peu de calcul)
- Revenus totaux par ligne
- Nombre de passagers par ligne/arret
- Kilometres parcourus estimes
- Methodes de paiement
- Cout par passager-km

### Calculs Intensifs (calcul lourd)
- **Simulation Monte Carlo** : 10 000 iterations pour optimiser les horaires
  - Temps d'attente moyen
  - Taux de satisfaction
  - Utilisation des bus

## Installation et Compilation

### Prerequis
- Java 21+
- Maven 3.8+
- Acces au broker MQTT (configure dans `config.json`)

### Workflow complet

```bash
# Installation des dependances
make install

# Nettoyage
make clean

# Compilation
make compile

# Package JAR (optionnel)
make package

# Execution mode threads
make run-threads 

# Execution mode processus
make run-processes

# Comparaison complete
make compare 

# EVALUATION + NOTE
make eval

# Tests seuls
make test         

# Aide complete
make help
```


## Utilisation

### Commandes principales

```bash
# Execution mode threads (4 workers, 30 secondes)
make run-threads

# Execution mode processus (4 workers, 30 secondes)
make run-processes

# Comparaison automatique threads vs processus
make compare

# Evaluation automatique avec calcul de la note (faire d'abord make scenarios)
make eval
```

### Personnalisation des parametres

```bash
# Mode threads avec 8 workers pendant 60 secondes
make run-threads WORKERS=8 DURATION=60

# Mode processus avec Monte Carlo intensif
make run-processes WORKERS=4 MONTE_CARLO=20000

# Comparaison avec parametres personnalises
make compare WORKERS=4 DURATION=30 MONTE_CARLO=10000
```

### Aide

```bash
# Afficher toutes les commandes disponibles
make help
```

## Evaluation Automatique

Le projet inclut un systeme d'evaluation automatique complet :

```bash
make eval
```

Cette commande :
1. Compile le projet
2. Execute tous les tests JUnit
3. Calcule la note sur 100 points selon le bareme
4. Affiche un rapport detaille avec interpretation

### Bareme (100 points)

- **Tests fonctionnels de base** (30 points)
  - TransportEventTest : 5 points
  - ThreadWorkerTest : 10 points
  - ProcessWorkerTest : 10 points
  - IPCTest : 5 points

- **Calculs corrects** (40 points)
  - StatisticsTest : 15 points
  - MonteCarloTest : 15 points
  - MergeTest : 10 points

- **Performance et parallelisme** (20 points)
  - PerformanceMonitorTest : 10 points
  - ScalabilityTest : 10 points

- **Qualite du code** (10 points)
  - CodeQualityTest : 10 points

## SCENARIOS
il existe 3 scenarios pour mesurer les performances du syteme.

### SCENARIO 1 : Overhead IPC Evident

**Commande :**
```bash
# Montrer overhead IPC
make scenario1
```

**Configuration :**
- 4 workers
- 15 secondes
- Monte Carlo DESACTIVE (--monte-carlo=0)
- Agregations simples uniquement

**Objectif :**
Montrer que l'overhead IPC > gain du parallelisme pour des calculs simples.

**Calculs effectues :**
- Revenus par ligne (simple addition)
- Comptage passagers (increments atomiques)
- Kilometres parcourus (calcul trivial)
- Methodes de paiement (compteurs)

### SCENARIO 2 : Parallelisme Benefique

**Commande :**
```bash
# Montrer benefice parallelisme
make scenario2
```

**Configuration :**
- 4 workers
- 30 secondes
- Monte Carlo ACTIVE (20 000 iterations)
- Agregations + simulation intensive

**Objectif :**
Montrer que le gain du parallelisme peut compenser l'overhead IPC.

**Calculs effectues :**
- Agregations simples (comme scenario 1)
- **Simulation Monte Carlo** (20 000 iterations) :
  - Generation aleatoire de scenarios d'horaires
  - Calcul temps d'attente moyen
  - Taux de satisfaction
  - Utilisation des bus
  - Statistiques (moyenne, ecart-type)

### SCENARIO 3 : Scalabilite

**Commande :**
```bash
# Montrer scalabilite
make scenario3
```

**Configuration :**
- 1, 2, 4, 8 workers (teste sequentiellement)
- 20 secondes chacun
- Monte Carlo modere (5 000 iterations)
- Mode threads uniquement

**Objectif :**
Demontrer la relation entre nombre de workers, speedup et efficacite.

**Calculs effectues :**
- Agregations
- Monte Carlo 5 000 iterations
- Distribution round-robin des evenements

## Execution de tous les scenarios

**Commande unique :**
```bash
make scenarios
```

Execute les 3 scenarios en sequence et sauvegarde les logs dans `/tmp/`.


## Personnalisation

Les scenarios peuvent etre personnalises :

```bash
# Scenario 1 plus long
make run-threads WORKERS=4 DURATION=30 MONTE_CARLO=0
make run-processes WORKERS=4 DURATION=30 MONTE_CARLO=0

# Scenario 2 encore plus intensif
make run-threads WORKERS=4 DURATION=60 MONTE_CARLO=50000
make run-processes WORKERS=4 DURATION=60 MONTE_CARLO=50000

# Scenario 3 jusqu'a 16 workers
make run-threads WORKERS=1 DURATION=20 MONTE_CARLO=5000
make run-threads WORKERS=2 DURATION=20 MONTE_CARLO=5000
# ... jusqu'a 16
```

---

## Structure du Projet

```
tp3-sts-transport-sherbrooke/
├── Makefile                         # Automatisation build/run/eval
├── pom.xml                          # Configuration Maven
├── config.json                      # Configuration MQTT
├── README.md                        # Ce fichier
├── src/
│   ├── main/java/ca/usherbrooke/ift630/tp3/
│   │   ├── Main.java                # Point d'entree
│   │   ├── mqtt/
│   │   │   ├── MQTTClient.java      # Client MQTT (FOURNI)
│   │   │   └── TransportEvent.java  # DTO evenements
│   │   ├── worker/
│   │   │   ├── TransportWorker.java # Interface commune
│   │   │   ├── ThreadWorker.java    # Implementation threads
│   │   │   └── ProcessWorker.java   # Implementation processus
│   │   ├── supervisor/
│   │   │   ├── Supervisor.java      # Interface superviseur
│   │   │   ├── ThreadSupervisor.java# Mode threads
│   │   │   └── ProcessSupervisor.java# Mode processus
│   │   ├── compute/
│   │   │   ├── Statistics.java      # Agregations
│   │   │   └── MonteCarloSimulator.java # Monte Carlo
│   │   └── utils/
│   │       └── PerformanceMonitor.java # Instrumentation
│   └── test/java/ca/usherbrooke/ift630/tp3/test/
│       ├── EvaluationRunner.java    # Systeme evaluation
│       ├── TransportEventTest.java
│       ├── StatisticsTest.java
│       ├── MonteCarloTest.java
│       ├── ThreadWorkerTest.java
│       ├── ProcessWorkerTest.java
│       ├── IPCTest.java
│       ├── PerformanceMonitorTest.java
│       ├── MergeTest.java
│       ├── ScalabilityTest.java
│       └── CodeQualityTest.java
```

## Depannage

### Le broker MQTT est inaccessible
```
Erreur de connexion au broker
```
Verifier `config.json`, tester la connexion reseau et votre VPN ou que vous êtes sur eduroam

### Les processus workers ne se connectent pas
```
Timeout connexion workers
```
Verifier le firewall, ports disponibles

### Compilation echoue
```
Erreur Maven
```
Verifier Java 21+ installe : `java -version`  
Reinstaller dependances : `make install`

### Tests echouent
```bash
make clean
make compile
make eval
```

## Objectifs Pedagogiques

Ce TP permet d'apprendre :

1. **Communication Inter-Processus (IPC)**
   - Sockets TCP localhost
   - Serialisation JSON
   - Protocoles de communication

2. **Mesure de Performance**
   - Instrumentation fine
   - Calcul du speedup
   - Analyse de l'efficacite

3. **Comparaison Threads vs Processus**
   - Quand utiliser threads vs processus ?
   - Impact de l'overhead IPC
   - Trade-offs isolation vs performance

4. **Calculs Distribues**
   - Agregations paralleles
   - Simulations Monte Carlo
   - Repartition de charge

## Ressources

- **Eclipse Paho MQTT** : https://www.eclipse.org/paho/
- **Java Concurrency** : https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/package-summary.html
- **JUnit 5** : https://junit.org/junit5/
- **Cours IFT630** : Semaines 9-12 (IPC, parallelisme, performance)


**Bonne chance avec le TP3!**
