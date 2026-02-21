# TP1 - Système de Transport Concurrent
## IFT630 - Processus Concurrents et Parallélisme

### Objectif pédagogique

Ce TP vous guide dans l'implémentation d'un système de transport concurrent en Java. Vous partirez d'une base fonctionnelle et apprendrez progressivement les concepts de programmation concurrente.

**Important**: Le code fourni compile et fonctionne. Votre mission est de comprendre pourquoi chaque partie fonctionne et comment les problèmes de concurrence sont résolus.

---

## ÉTAPE 1: Comprendre la base fournie

Le code fourni contient déjà une implémentation fonctionnelle. Étudiez-le attentivement avant de modifier quoi que ce soit.

```java
package udes.fsci.info.ift630.transport;

import udes.fsci.info.ift630.transport.evenements.*;
import udes.fsci.info.ift630.transport.util.LecteurCSV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Système de traitement d'événements de transport en temps réel.
 *
 * TP1 - IFT630 - Hiver 2026
 * @author Votre nom
 */
public class SystemeTransport {

    private static final Logger logger = LoggerFactory.getLogger(SystemeTransport.class);

    // ====================================================================
    // STRUCTURES FOURNIES - NE PAS MODIFIER
    // ====================================================================

    // File d'attente thread-safe pour les événements
    private final BlockingQueue<Evenement> fileEvenements;

    // Pool de threads pour le traitement parallèle
    private final List<Thread> threadsTraitement;

    // Flag volatile pour arrêter les threads proprement
    private volatile boolean enExecution;

    // Compteur atomique pour suivre les événements en attente
    private final AtomicInteger evenementsEnAttente;

    public SystemeTransport() {
        this.fileEvenements = new LinkedBlockingQueue<>();
        this.threadsTraitement = new ArrayList<>();
        this.enExecution = true;
        this.evenementsEnAttente = new AtomicInteger(0);

        // Création automatique du pool de threads
        int nombreThreads = Runtime.getRuntime().availableProcessors();
        for (int i = 0; i < nombreThreads; i++) {
            Thread t = new Thread(this::boucleTraitement, "Thread-Traitement-" + i);
            t.start();
            threadsTraitement.add(t);
        }

        logger.info("Système démarré avec {} threads", nombreThreads);
    }
```

---

## ÉTAPE 2: Analyser les structures de données thread-safe

Le code fourni utilise plusieurs structures thread-safe. Comprenez pourquoi chacune est nécessaire :

```java
    // ====================================================================
    // STRUCTURES DE DONNÉES THREAD-SAFE
    // ====================================================================

    // Occupation par bus (thread-safe)
    // PROBLÈME: Plusieurs threads lisent/écrivent l'occupation simultanément
    // SOLUTION: ConcurrentHashMap + AtomicInteger pour thread-safety
    private final ConcurrentHashMap<String, AtomicInteger> passagersParBus;

    // Capacités maximales par bus (immuable après init)
    // ConcurrentHashMap permet lectures concurrentes sans lock
    private final ConcurrentHashMap<String, Integer> capacitesMaxBus;

    // Locks par bus pour opérations atomiques check-then-act
    // PROBLÈME: Vérifier capacité PUIS monter = 2 opérations non atomiques
    // SOLUTION: ReentrantLock par bus
    private final ConcurrentHashMap<String, ReentrantLock> locksBus;

    // Compteurs globaux (thread-safe)
    private final AtomicInteger totalPassagersTransportes;
    private final AtomicInteger totalMontees;
    private final AtomicInteger totalDescentes;

    // Revenus (double nécessite synchronisation)
    private double revenusTotal;
    private final Object lockRevenus = new Object();

    // Refus d'embarquement par bus
    private final ConcurrentHashMap<String, AtomicInteger> refusParBus;

    // Alertes 90% par bus
    private final ConcurrentHashMap<String, Boolean> alertes90ParBus;

    // Suivi des passagers pour voyages complets
    private final Set<String> passagersMontes;
    private final Set<String> passagersDescendus;
    private final Object lockVoyages = new Object();
```

**Questions d'analyse:**
1. Pourquoi `AtomicInteger` au lieu de `int` pour les compteurs ?
2. Pourquoi `ConcurrentHashMap` au lieu de `HashMap` ?
3. Pourquoi `synchronized` pour les revenus mais pas pour les compteurs AtomicInteger ?
4. Pourquoi un lock par bus plutôt qu'un lock global ?

---

## ÉTAPE 3: Étudier l'initialisation

```java
    public SystemeTransport() {
        // ... code fourni ...

        // Initialisation des structures
        this.passagersParBus = new ConcurrentHashMap<>();
        this.capacitesMaxBus = new ConcurrentHashMap<>();
        this.locksBus = new ConcurrentHashMap<>();
        this.totalPassagersTransportes = new AtomicInteger(0);
        this.totalMontees = new AtomicInteger(0);
        this.totalDescentes = new AtomicInteger(0);
        this.revenusTotal = 0.0;
        this.refusParBus = new ConcurrentHashMap<>();
        this.alertes90ParBus = new ConcurrentHashMap<>();
        this.passagersMontes = ConcurrentHashMap.newKeySet();  // Set thread-safe
        this.passagersDescendus = ConcurrentHashMap.newKeySet();

        // ... reste du constructeur ...
    }
```

**À retenir:**
- Tous les champs `final` sont initialisés dans le constructeur
- `ConcurrentHashMap.newKeySet()` crée un Set thread-safe
- Chaque structure est initialisée avec sa valeur par défaut appropriée

---

## ÉTAPE 4: Comprendre le traitement des événements

Le système traite différents types d'événements. Analysez chaque méthode :

### 4.1 Montée d'un passager (opération critique)

```java
    private void traiterPassagerMonte(PassagerMonte evt) {
        String busId = evt.getBusId();
        String passagerId = evt.getPassagerId();

        // Initialiser structures si nécessaire (lazy initialization)
        locksBus.putIfAbsent(busId, new ReentrantLock());
        refusParBus.putIfAbsent(busId, new AtomicInteger(0));
        alertes90ParBus.putIfAbsent(busId, false);

        ReentrantLock lock = locksBus.get(busId);
        lock.lock();
        try {
            // Initialiser occupation si nouveau bus
            passagersParBus.putIfAbsent(busId, new AtomicInteger(0));
            int occupationActuelle = passagersParBus.get(busId).get();
            Integer capaciteMax = capacitesMaxBus.get(busId);

            // VÉRIFICATION CAPACITÉ (sous lock)
            if (capaciteMax != null && occupationActuelle >= capaciteMax) {
                refusParBus.get(busId).incrementAndGet();
                logger.debug("Refus passager {} sur bus {} (plein: {}/{})",
                            passagerId, busId, occupationActuelle, capaciteMax);
                return;
            }

            // ACCEPTATION (sous lock)
            int nouvelleOccupation = passagersParBus.get(busId).incrementAndGet();
            totalMontees.incrementAndGet();

            // Marquer comme monté (sous lock séparé pour voyages)
            synchronized (lockVoyages) {
                passagersMontes.add(passagerId);
            }

            logger.debug("Passager {} monte dans bus {} (occupation: {})",
                        passagerId, busId, nouvelleOccupation);

        } finally {
            lock.unlock();  // TOUJOURS libérer le lock
        }
    }
```

**Points critiques à analyser:**
1. **Lazy initialization**: Pourquoi initialiser les structures dans la méthode plutôt qu'au constructeur ?
2. **Lock par bus**: Pourquoi pas un lock global ?
3. **Double synchronisation**: Pourquoi `lock` ET `synchronized(lockVoyages)` ?
4. **Ordre des opérations**: Lock → Check → Act → Unlock

### 4.2 Descente d'un passager

```java
    private void traiterPassagerDescend(PassagerDescend evt) {
        String busId = evt.getBusId();
        String passagerId = evt.getPassagerId();

        ReentrantLock lock = locksBus.computeIfAbsent(busId, k -> new ReentrantLock());
        lock.lock();
        try {
            passagersParBus.putIfAbsent(busId, new AtomicInteger(0));
            passagersParBus.get(busId).decrementAndGet();
            totalDescentes.incrementAndGet();

            // Vérifier voyage complet (monté ET descendu)
            synchronized (lockVoyages) {
                passagersDescendus.add(passagerId);

                if (passagersMontes.contains(passagerId) && passagersDescendus.contains(passagerId)) {
                    // Voyage complet détecté
                    logger.debug("Voyage complet pour passager {}", passagerId);
                }
            }

            logger.debug("Passager {} descend du bus {}", passagerId, busId);

        } finally {
            lock.unlock();
        }
    }
```

**Questions:**
1. Pourquoi `computeIfAbsent` plutôt que `putIfAbsent` ?
2. Comment éviter les voyages "fantômes" (descente sans montée) ?

### 4.3 Traitement des paiements

```java
    private void traiterPaiement(Paiement evt) {
        double montant = evt.getMontant();
        String methode = evt.getMethodePaiement();

        // ATTENTION: double n'est pas atomique!
        synchronized (lockRevenus) {
            revenusTotal += montant;
        }

        logger.debug("Paiement de {} $ reçu (passager {}, méthode: {})",
                    montant, evt.getPassagerId(), methode);
    }
```

**Pourquoi cette synchronisation ?**
- Les variables `double` et `long` ne sont pas atomiques en Java
- Une écriture `revenusTotal += montant` peut être interrompue par un autre thread
- Résultat: perte de données ou valeurs corrompues

---

## ÉTAPE 5: Analyser les getters thread-safe

### 5.1 Getter avec lock

```java
    public int getNombrePassagers(String busId) {
        ReentrantLock lock = locksBus.get(busId);
        if (lock != null) {
            lock.lock();
            try {
                AtomicInteger occupation = passagersParBus.get(busId);
                return occupation != null ? occupation.get() : 0;
            } finally {
                lock.unlock();
            }
        } else {
            // Pas de lock = bus pas encore utilisé
            AtomicInteger occupation = passagersParBus.get(busId);
            return occupation != null ? occupation.get() : 0;
        }
    }
```

**Pourquoi cette complexité ?**
- Si le bus n'existe pas encore, pas besoin de lock
- Si le bus existe, il faut synchroniser l'accès

### 5.2 Getter avec synchronized

```java
    public double getRevenusTotal() {
        synchronized (lockRevenus) {
            return revenusTotal;
        }
    }

    public int getTotalPassagersTransportes() {
        synchronized (lockVoyages) {
            int count = 0;
            for (String passagerId : passagersDescendus) {
                if (passagersMontes.contains(passagerId)) {
                    count++;
                }
            }
            return count;
        }
    }
```

**Différences importantes:**
- `getRevenusTotal()`: synchronisation simple pour protéger le double
- `getTotalPassagersTransportes()`: itération sur les collections, nécessite synchronisation complète

---

## ÉTAPE 6: Tester votre compréhension

Avant de modifier le code, vérifiez que vous comprenez :

### Test 1: Race conditions
```java
// Que se passe-t-il si on enlève le lock dans traiterPassagerMonte ?
// Simulation: Thread A vérifie capacité (9/10), Thread B vérifie capacité (9/10)
// Thread A monte (10/10), Thread B monte aussi (11/10) → dépassement!
```

### Test 2: Atomicité des doubles
```java
// Que se passe-t-il si on enlève synchronized dans traiterPaiement ?
// Thread A: lit 10.0, Thread B: lit 10.0
// Thread A: écrit 15.0, Thread B: écrit 13.0 → perte de 2.0$
```

### Test 3: Visibilité des changements
```java
// Pourquoi volatile sur enExecution mais pas sur revenusTotal ?
// volatile = visibilité garantie, synchronized = atomicité + visibilité
```

---

## ÉTAPE 7: Exercices d'amélioration

Maintenant que vous comprenez le code, améliorez-le :

### 7.1 Optimisation des locks
**Problème**: Le lock par bus est correct mais peut créer contention.

**Exercice**: Implémentez un système de "stripe locking" où plusieurs bus partagent le même lock pour réduire la contention.

### 7.2 Statistiques temps réel
**Exercice**: Ajoutez des compteurs de performance :
- Temps moyen de traitement par événement
- Nombre de conflits de lock par bus
- Utilisation CPU par thread

### 7.3 Validation d'intégrité
**Exercice**: Ajoutez des assertions pour vérifier :
- Aucun passager ne descend sans être monté
- Les totaux sont cohérents
- Les capacités ne sont pas dépassées

### 7.4 Gestion des erreurs
**Exercice**: Améliorez la robustesse :
- Gestion des interruptions de threads
- Recovery automatique en cas de deadlock
- Logging détaillé des états inconsistants

---

## ÉTAPE 8: Questions de réflexion

1. **Performance vs Correctness**: Où est le compromis entre performance et sécurité des threads ?

2. **Évolutivité**: Comment ce système se comporterait-il avec 1000 bus et 100 threads ?

3. **Alternatives**: Pourriez-vous utiliser `LongAdder` au lieu d'`AtomicInteger` pour les compteurs ? Pourquoi ?

4. **Testabilité**: Comment tester qu'une race condition a été éliminée ?

5. **Maintenance**: Ce code est-il facile à maintenir ? Pourquoi ?

---

## Commandes de test

```bash
# Compiler
make compile

# Tester un scénario simple
make run TEST=scenario_simple

# Tester avec stabilité (recommandé)
make run TEST=scenario_stress

# Évaluer automatiquement
make eval
```

---

## Concepts clés maîtrisés

✅ **Thread-safety**: Structures concurrentes, atomicité, visibilité
✅ **Synchronisation**: Locks, synchronized, volatile
✅ **Race conditions**: Détection et prévention
✅ **Performance**: Contention, scalabilité
✅ **Débogage**: Logging, invariants, assertions

**Félicitations !** Vous maîtrisez maintenant les bases de la programmation concurrente en Java. 🚀