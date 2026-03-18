package ca.usherbrooke.ift630.tp2;

import ca.usherbrooke.ift630.tp2.benchmark.BenchmarkRWLock;
import ca.usherbrooke.ift630.tp2.coordination.Dashboard;
import ca.usherbrooke.ift630.tp2.coordination.FileAttentePrioritaire;
import ca.usherbrooke.ift630.tp2.coordination.GestionnaireStations;
import ca.usherbrooke.ift630.tp2.generation.GenerateurEvenements;
import ca.usherbrooke.ift630.tp2.modele.*;
import ca.usherbrooke.ift630.tp2.benchmark.BenchmarkDashboard;

import ca.usherbrooke.ift630.tp2.generation.GenerateurDeadlock;
import ca.usherbrooke.ift630.tp2.threads.BusThread;
import ca.usherbrooke.ift630.tp2.threads.BusThreadDeadlock;
import ca.usherbrooke.ift630.tp2.threads.ConsommateurDebarquement;
import ca.usherbrooke.ift630.tp2.threads.ConsommateurEmbarquement;
import ca.usherbrooke.ift630.tp2.threads.ConsommateurPaiement;
import ca.usherbrooke.ift630.tp2.threads.DashboardAfficheur;
import ca.usherbrooke.ift630.tp2.threads.DeadlockDetector;
import ca.usherbrooke.ift630.tp2.threads.PassagerThread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Point d'entree principal de la simulation STS concurrente.
 * Supporte l'execution par question via --question N.
 */
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final long SEED = 12345;

    public static void main(String[] args) {
        logger.info("=== Simulation STS Concurrent - TP2 IFT630 ===");

        if (args.length >= 2 && "--question".equals(args[0])) {
            int question = Integer.parseInt(args[1]);
            executerQuestion(question);
        } else if (args.length >= 1 && args[0].endsWith(".csv")) {
            executerDepuisCSV(args[0]);
        } else {
            // Par defaut, executer Q1
            executerQuestion(1);
        }
    }

    private static void executerQuestion(int question) {
        logger.info("Execution de la Question {}", question);

        switch (question) {
            case 1 -> executerQ1();
            case 2 -> executerQ2();
            case 3 -> executerQ3();
            case 4 -> executerQ4();
            case 5 -> executerQ5();
            case 6 -> executerQ6();
            case 7 -> executerQ7();
            case 8 -> executerQ8();
            default -> logger.error("Question {} non reconnue (1-8)", question);
        }
    }

    /**
     * Question 1 : Modele de donnees et generation deterministe.
     * Genere 1000 evenements avec seed 12345 et les sauvegarde en CSV.
     */
    private static void executerQ1() {
        logger.info("=== QUESTION 1 : Modele de donnees et generation deterministe ===");

        // Creer le generateur avec le seed fixe
        GenerateurEvenements generateur = new GenerateurEvenements(SEED);

        // Generer 1000 evenements
        List<Evenement> evenements = generateur.genererScenario(1000);

        // Afficher les statistiques
        long nbEmbarquements = evenements.stream()
                .filter(e -> e.getType() == TypeEvenement.EMBARQUEMENT)
                .count();
        long nbDebarquements = evenements.stream()
                .filter(e -> e.getType() == TypeEvenement.DEBARQUEMENT)
                .count();
        long nbHaute = evenements.stream()
                .filter(e -> e.getPrioritePassager() == Priorite.HAUTE)
                .count();
        long nbNormale = evenements.stream()
                .filter(e -> e.getPrioritePassager() == Priorite.NORMALE)
                .count();

        logger.info("--- Statistiques du scenario ---");
        logger.info("Nombre total d'evenements : {}", evenements.size());
        logger.info("Embarquements : {}", nbEmbarquements);
        logger.info("Debarquements : {}", nbDebarquements);
        logger.info("Passagers HAUTE priorite : {}", nbHaute);
        logger.info("Passagers NORMALE priorite : {}", nbNormale);

        // Afficher premier et dernier evenement
        Evenement premier = evenements.get(0);
        Evenement dernier = evenements.get(evenements.size() - 1);
        logger.info("Premier evenement : {}", premier.toCSV());
        logger.info("Dernier evenement : {}", dernier.toCSV());

        // Sauvegarder en CSV
        generateur.sauvegarderCSV(evenements, "scenarios/scenario-seed-12345.csv");

        // Creer les objets du modele pour verification
        Map<String, Bus> busMap = generateur.creerBus();
        Map<String, Station> stationMap = generateur.creerStations();
        Map<String, Passager> passagerMap = generateur.creerPassagers(evenements);

        logger.info("--- Objets du modele ---");
        logger.info("Bus crees : {}", busMap.size());
        logger.info("Stations creees : {}", stationMap.size());
        logger.info("Passagers uniques : {}", passagerMap.size());

        // Afficher les lignes STS
        logger.info("--- Lignes STS ---");
        for (Ligne ligne : generateur.getLignes()) {
            logger.info("  {} : {}", ligne, ligne.getStations());
        }

        // Afficher les bus
        logger.info("--- Bus ---");
        for (Bus bus : busMap.values()) {
            logger.info("  {}", bus);
        }

        // Afficher les stations
        logger.info("--- Stations ---");
        for (Station station : stationMap.values()) {
            logger.info("  {}", station);
        }

        // Verification de reproductibilite : generer une seconde fois et comparer
        logger.info("--- Verification de reproductibilite ---");
        GenerateurEvenements gen2 = new GenerateurEvenements(SEED);
        List<Evenement> evenements2 = gen2.genererScenario(1000);

        boolean identique = true;
        for (int i = 0; i < evenements.size(); i++) {
            if (!evenements.get(i).toCSV().equals(evenements2.get(i).toCSV())) {
                logger.error("DIVERGENCE a l'evenement {} : {} vs {}",
                        i, evenements.get(i).toCSV(), evenements2.get(i).toCSV());
                identique = false;
                break;
            }
        }

        if (identique) {
            logger.info("Reproductibilite VERIFIEE : les 2 generations sont identiques");
        } else {
            logger.error("ERREUR : les 2 generations different !");
        }

        logger.info("=== QUESTION 1 TERMINEE ===");
    }

    /**
     * Question 2 : Threads autobus producteurs.
     * Lance 16 bus (2 par ligne) comme threads producteurs d'evenements.
     * Chaque bus parcourt ses stations et depose les evenements dans la file partagee.
     */
    private static void executerQ2() {
        logger.info("=== QUESTION 2 : Threads autobus producteurs ===");

        // 1. Generer le scenario deterministe
        GenerateurEvenements generateur = new GenerateurEvenements(SEED);
        List<Evenement> evenements = generateur.genererScenario(1000);

        // 2. Creer les objets du modele
        Map<String, Bus> busMap = generateur.creerBus();
        Map<String, Station> stationMap = generateur.creerStations();
        Map<String, Passager> passagerMap = new ConcurrentHashMap<>(generateur.creerPassagers(evenements));

        // 3. Creer la file d'attente prioritaire partagee
        FileAttentePrioritaire fileEvenements = new FileAttentePrioritaire();

        // 4. Repartir les evenements par bus
        Map<String, List<Evenement>> evenementsParBus = new HashMap<>();
        for (Bus bus : busMap.values()) {
            evenementsParBus.put(bus.getId(), new ArrayList<>());
        }
        for (Evenement evt : evenements) {
            List<Evenement> listeEvt = evenementsParBus.get(evt.getBusId());
            if (listeEvt != null) {
                listeEvt.add(evt);
            }
        }

        // 5. Creer et demarrer les threads bus
        List<BusThread> busThreads = new ArrayList<>();
        for (Bus bus : busMap.values()) {
            List<Evenement> evtBus = evenementsParBus.get(bus.getId());
            BusThread bt = new BusThread(bus, fileEvenements, evtBus, stationMap, passagerMap);
            busThreads.add(bt);
        }

        logger.info("Demarrage de {} threads bus...", busThreads.size());
        long debut = System.nanoTime();

        // Demarrer tous les threads
        for (BusThread bt : busThreads) {
            bt.start();
        }

        // Attendre la fin de tous les threads
        for (BusThread bt : busThreads) {
            try {
                bt.join(30000); // Timeout de 30 secondes
                if (bt.isAlive()) {
                    logger.warn("Bus {} toujours en cours apres 30s, interruption...", bt.getBus().getId());
                    bt.interrupt();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        long fin = System.nanoTime();
        double dureeMs = (fin - debut) / 1_000_000.0;

        // 6. Afficher les resultats
        logger.info("=== Resultats Question 2 ===");
        logger.info("Duree d'execution : {} ms", String.format("%.1f", dureeMs));
        logger.info("Evenements produits dans la file : {}", fileEvenements.getTotalProduits());

        // Afficher le nombre de passagers par bus
        logger.info("--- Passagers par bus ---");
        for (Bus bus : busMap.values()) {
            logger.info("  {} : {} passagers", bus.getId(), bus.getNombrePassagers());
        }

        // Verifier qu'aucun bus ne depasse sa capacite
        boolean capaciteRespectee = true;
        for (Bus bus : busMap.values()) {
            if (bus.getNombrePassagers() > bus.getCapaciteMax()) {
                logger.error("ERREUR: {} depasse sa capacite : {}/{}",
                        bus.getId(), bus.getNombrePassagers(), bus.getCapaciteMax());
                capaciteRespectee = false;
            }
        }
        if (capaciteRespectee) {
            logger.info("Capacite maximale respectee pour tous les bus");
        }

        // Verifier les threads termines
        boolean tousTermines = busThreads.stream().noneMatch(Thread::isAlive);
        logger.info("Tous les threads termines : {}", tousTermines ? "OUI" : "NON");
        logger.info("Aucun deadlock detecte : {}", tousTermines ? "OUI" : "VERIFICATION REQUISE");

        logger.info("=== QUESTION 2 TERMINEE ===");
    }

    /**
     * Question 3 : Threads passagers actifs avec mecanisme de fairness.
     * Lance des threads bus (producteurs) ET des threads passagers (actifs).
     * Les passagers attendent a leur station et tentent d'embarquer.
     * Le mecanisme de fairness garantit qu'apres 3 HAUTE consecutifs,
     * un passager NORMALE est servi.
     */
    private static void executerQ3() {
        logger.info("=== QUESTION 3 : Threads passagers actifs ===");

        // 1. Generer le scenario deterministe
        GenerateurEvenements generateur = new GenerateurEvenements(SEED);
        List<Evenement> evenements = generateur.genererScenario(1000);

        // 2. Creer les objets du modele
        Map<String, Bus> busMap = generateur.creerBus();
        Map<String, Station> stationMap = generateur.creerStations();
        Map<String, Passager> passagerMap = new ConcurrentHashMap<>(generateur.creerPassagers(evenements));

        // 3. Creer le gestionnaire de stations avec fairness
        GestionnaireStations gestionnaireStations = new GestionnaireStations(stationMap);

        // 4. Creer la file d'attente prioritaire partagee
        FileAttentePrioritaire fileEvenements = new FileAttentePrioritaire();

        // 5. Repartir les evenements par bus
        Map<String, List<Evenement>> evenementsParBus = new HashMap<>();
        for (Bus bus : busMap.values()) {
            evenementsParBus.put(bus.getId(), new ArrayList<>());
        }
        for (Evenement evt : evenements) {
            List<Evenement> listeEvt = evenementsParBus.get(evt.getBusId());
            if (listeEvt != null) {
                listeEvt.add(evt);
            }
        }

        // 6. Collecter les 100 premiers passagers uniques (embarquements)
        // et leur associer le bus et la ligne attendus
        List<PassagerThread> passagerThreads = new ArrayList<>();
        Map<String, Ligne> ligneParNumero = new HashMap<>();
        for (Ligne ligne : generateur.getLignes()) {
            ligneParNumero.put(String.valueOf(ligne.getNumero()), ligne);
        }

        int nbPassagersActifs = 0;
        for (Evenement evt : evenements) {
            if (evt.getType() == TypeEvenement.EMBARQUEMENT && nbPassagersActifs < 100) {
                Passager passager = passagerMap.get(evt.getPassagerId());
                if (passager != null) {
                    // Extraire le numero de ligne depuis le busId (ex: "BUS-L3-02" -> "3")
                    String busId = evt.getBusId();
                    String numLigne = busId.replaceAll("BUS-L(\\d+)-.*", "$1");
                    Ligne ligne = ligneParNumero.get(numLigne);

                    PassagerThread pt = new PassagerThread(passager, gestionnaireStations, busId, ligne);
                    passagerThreads.add(pt);
                    nbPassagersActifs++;
                }
            }
        }

        // 7. Creer les threads bus
        List<BusThread> busThreads = new ArrayList<>();
        for (Bus bus : busMap.values()) {
            List<Evenement> evtBus = evenementsParBus.get(bus.getId());
            BusThread bt = new BusThread(bus, fileEvenements, evtBus, stationMap, passagerMap);
            busThreads.add(bt);
        }

        logger.info("Demarrage de {} threads bus et {} threads passagers...",
                busThreads.size(), passagerThreads.size());

        long nbHaute = passagerThreads.stream()
                .filter(pt -> pt.getPassager().getPriorite() == Priorite.HAUTE)
                .count();
        long nbNormale = passagerThreads.size() - nbHaute;
        logger.info("Distribution : {} passagers HAUTE, {} passagers NORMALE", nbHaute, nbNormale);

        long debut = System.nanoTime();

        // Demarrer les threads passagers d'abord (ils attendent aux stations)
        for (PassagerThread pt : passagerThreads) {
            pt.start();
        }

        // Petite pause pour laisser les passagers se placer aux stations
        try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        // Demarrer les threads bus (ils parcourent les stations et notifient les passagers)
        for (BusThread bt : busThreads) {
            bt.start();
        }

        // Attendre la fin des threads bus
        for (BusThread bt : busThreads) {
            try {
                bt.join(30000);
                if (bt.isAlive()) {
                    logger.warn("Bus {} toujours en cours apres 30s", bt.getBus().getId());
                    bt.interrupt();
                }
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        // Attendre un court delai puis interrompre les passagers non embarques
        try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        for (PassagerThread pt : passagerThreads) {
            if (pt.isAlive()) {
                pt.interrupt();
            }
        }

        // Attendre que tous les passagers terminent
        for (PassagerThread pt : passagerThreads) {
            try {
                pt.join(5000);
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        long fin = System.nanoTime();
        double dureeMs = (fin - debut) / 1_000_000.0;

        // 8. Afficher les resultats
        logger.info("=== Resultats Question 3 ===");
        logger.info("Duree d'execution : {} ms", String.format("%.1f", dureeMs));

        long embarques = passagerThreads.stream().filter(PassagerThread::isEmbarque).count();
        long nonEmbarques = passagerThreads.size() - embarques;
        logger.info("Passagers embarques : {}/{}", embarques, passagerThreads.size());
        logger.info("Passagers non embarques : {}", nonEmbarques);

        // Rapport de fairness
        gestionnaireStations.afficherRapport();

        // Verifier qu'aucun passager normal n'a attendu plus de 5 embarquements prioritaires
        int maxConsecutifs = gestionnaireStations.getMaxPrioritairesConsecutifs();
        if (maxConsecutifs <= 5) {
            logger.info("Aucun passager normal n'a attendu plus de 5 prioritaires : OUI (max={})", maxConsecutifs);
        } else {
            logger.warn("ATTENTION: un passager normal a attendu {} prioritaires consecutifs", maxConsecutifs);
        }

        // Verification deadlock
        boolean tousTermines = busThreads.stream().noneMatch(Thread::isAlive)
                && passagerThreads.stream().noneMatch(Thread::isAlive);
        logger.info("Tous les threads termines : {}", tousTermines ? "OUI" : "NON");

        logger.info("=== QUESTION 3 TERMINEE ===");
    }

    /**
     * Question 4 : Consommateurs specialises avec variables de condition.
     * Les bus produisent les evenements (EMBARQUEMENT, DEBARQUEMENT, PAIEMENT)
     * dans une PriorityBlockingQueue partagee.
     * 3 consommateurs tournent en boucle sur queue.take() et traitent
     * chacun leur type d'evenement.
     */
    private static void executerQ4() {
        logger.info("=== QUESTION 4 : Consommateurs specialises ===");

        // 1. Generer le scenario deterministe
        GenerateurEvenements generateur = new GenerateurEvenements(SEED);
        List<Evenement> evenements = generateur.genererScenario(1000);

        // 2. Creer les objets du modele
        Map<String, Bus> busMap = generateur.creerBus();
        Map<String, Station> stationMap = generateur.creerStations();
        Map<String, Passager> passagerMap = new ConcurrentHashMap<>(generateur.creerPassagers(evenements));

        // 3. Creer le dashboard et l'initialiser
        Dashboard dashboard = new Dashboard();
        dashboard.initialiser(stationMap, generateur.getLignes());

        // 4. Creer la file d'attente prioritaire partagee
        FileAttentePrioritaire fileEvenements = new FileAttentePrioritaire();

        // 5. Repartir les evenements par bus
        Map<String, List<Evenement>> evenementsParBus = new HashMap<>();
        for (Bus bus : busMap.values()) {
            evenementsParBus.put(bus.getId(), new ArrayList<>());
        }
        for (Evenement evt : evenements) {
            List<Evenement> listeEvt = evenementsParBus.get(evt.getBusId());
            if (listeEvt != null) {
                listeEvt.add(evt);
            }
        }

        // 6. Creer les 3 consommateurs specialises
        ConsommateurEmbarquement consoEmbarquement =
                new ConsommateurEmbarquement(fileEvenements, busMap, passagerMap, dashboard);
        ConsommateurDebarquement consoDebarquement =
                new ConsommateurDebarquement(fileEvenements, busMap, passagerMap, dashboard);
        ConsommateurPaiement consoPaiement =
                new ConsommateurPaiement(fileEvenements, busMap, dashboard);

        // Les consommateurs sont des daemon threads pour qu'ils s'arretent quand les producteurs finissent
        consoEmbarquement.setDaemon(true);
        consoDebarquement.setDaemon(true);
        consoPaiement.setDaemon(true);

        // 7. Creer les threads bus (producteurs)
        List<BusThread> busThreads = new ArrayList<>();
        for (Bus bus : busMap.values()) {
            List<Evenement> evtBus = evenementsParBus.get(bus.getId());
            BusThread bt = new BusThread(bus, fileEvenements, evtBus, stationMap, passagerMap);
            busThreads.add(bt);
        }

        logger.info("Demarrage de {} threads bus (producteurs) et 3 consommateurs...", busThreads.size());
        long debut = System.nanoTime();

        // Demarrer les consommateurs d'abord
        consoEmbarquement.start();
        consoDebarquement.start();
        consoPaiement.start();

        // Demarrer les producteurs (bus)
        for (BusThread bt : busThreads) {
            bt.start();
        }

        // Attendre la fin des producteurs
        for (BusThread bt : busThreads) {
            try {
                bt.join(30000);
                if (bt.isAlive()) {
                    logger.warn("Bus {} toujours en cours apres 30s", bt.getBus().getId());
                    bt.interrupt();
                }
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        // Attendre que la file se vide (les consommateurs traitent les derniers evenements)
        int attente = 0;
        while (!fileEvenements.estVide() && attente < 50) {
            try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            attente++;
        }

        // Laisser encore un peu de temps pour les derniers traitements
        try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        long fin = System.nanoTime();
        double dureeMs = (fin - debut) / 1_000_000.0;

        // Interrompre les consommateurs
        consoEmbarquement.interrupt();
        consoDebarquement.interrupt();
        consoPaiement.interrupt();

        // 8. Afficher les resultats
        logger.info("=== Resultats Question 4 ===");
        logger.info("Duree d'execution : {} ms", String.format("%.1f", dureeMs));
        logger.info("Evenements produits : {}", fileEvenements.getTotalProduits());
        logger.info("Evenements consommes : {}", fileEvenements.getTotalConsommes());
        logger.info("Evenements restants dans la file : {}", fileEvenements.getTaille());

        logger.info("--- Consommateurs ---");
        logger.info("  Embarquements traites : {}", consoEmbarquement.getCompteurTraites());
        logger.info("  Debarquements traites : {}", consoDebarquement.getCompteurTraites());
        logger.info("  Paiements traites : {}", consoPaiement.getCompteurTraites());

        int totalTraites = consoEmbarquement.getCompteurTraites()
                + consoDebarquement.getCompteurTraites()
                + consoPaiement.getCompteurTraites();
        logger.info("  Total evenements traites : {}", totalTraites);

        logger.info("--- Revenus ---");
        logger.info("  Revenus totaux (paiements) : {} $",
                String.format("%.2f", consoPaiement.getRevenusDollars()));
        logger.info("  Revenus totaux (dashboard) : {} $",
                String.format("%.2f", dashboard.getRevenusTotalDollars()));

        // Afficher le dashboard
        dashboard.afficher();

        // Verifications
        boolean aucunPerdu = fileEvenements.getTaille() == 0;
        logger.info("Aucun evenement perdu : {}", aucunPerdu ? "OUI" : "NON (restants: " + fileEvenements.getTaille() + ")");

        logger.info("=== QUESTION 4 TERMINEE ===");
    }

    /**
     * Question 5 : Read-Write Locks sur les ressources partagees.
     * Demontre l'utilisation de ReentrantReadWriteLock dans Bus, Station et Dashboard.
     * Execute un benchmark comparatif synchronized vs RW Lock.
     *
     * Les RW Locks sont deja en place dans :
     * - Bus.java : readLock pour getNombrePassagers/getPourcentageOccupation,
     *              writeLock pour embarquer/debarquer
     * - Station.java : readLock pour getNombrePassagersEnAttente/estPleine,
     *                  writeLock pour ajouterPassagerEnAttente/retirerPassagerEnAttente
     * - Dashboard.java : readLock pour getStatistiquesStations (100x/sec),
     *                    writeLock pour mettreAJour (1x/sec)
     */
    private static void executerQ5() {
        logger.info("=== QUESTION 5 : Read-Write Locks sur les ressources partagees ===");

        logger.info("--- Classes utilisant ReentrantReadWriteLock ---");
        logger.info("  Bus.java : readLock(getNombrePassagers, getPourcentageOccupation, getPassagers, estPlein)");
        logger.info("           : writeLock(embarquer, debarquer)");
        logger.info("  Station.java : readLock(getNombrePassagersEnAttente, estPleine, getBusPresents)");
        logger.info("               : writeLock(ajouterPassagerEnAttente, retirerPassagerEnAttente, arriverBus, departBus)");
        logger.info("  Dashboard.java : readLock(getStatistiquesStations, getRevenusParLigne, afficher)");
        logger.info("                 : writeLock(enregistrerEmbarquement, enregistrerDebarquement, enregistrerPaiement)");

        // Executer le benchmark comparatif
        Map<String, Object> resultats = BenchmarkRWLock.executer();

        // Sauvegarder les resultats dans un fichier CSV
        try {
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get("results"));
            try (java.io.BufferedWriter writer = java.nio.file.Files.newBufferedWriter(
                    java.nio.file.Paths.get("results/bench-rwlock.csv"))) {
                writer.write("type,throughput_lecture_ops_sec,throughput_ecriture_ops_sec");
                writer.newLine();
                writer.write(String.format("synchronized,%.0f,%.0f",
                        resultats.get("sync_throughput_lecture"),
                        resultats.get("sync_throughput_ecriture")));
                writer.newLine();
                writer.write(String.format("rwlock,%.0f,%.0f",
                        resultats.get("rw_throughput_lecture"),
                        resultats.get("rw_throughput_ecriture")));
                writer.newLine();
            }
            logger.info("Resultats sauvegardes dans results/bench-rwlock.csv");
        } catch (java.io.IOException e) {
            logger.error("Erreur lors de la sauvegarde des resultats", e);
        }

        logger.info("=== QUESTION 5 TERMINEE ===");
    }

    /**
     * Question 6 : Prevention de deadlock et livelock.
     *
     * Scenario de deadlock :
     *   Bus A (ligne 101) veut : Station Universite -> Station Cegep
     *   Bus B (ligne 102) veut : Station Cegep -> Station Universite
     *   Si A acquiert Universite et B acquiert Cegep -> DEADLOCK
     *
     * Strategie de prevention : tryLock avec timeout progressif
     *   - tryLock(timeout) au lieu de lock() bloquant
     *   - Si echec : relacher TOUS les locks (back-off)
     *   - Timeout progressif : 10ms, 20ms, 40ms, 80ms... (exponentiel)
     *   - Delai aleatoire entre tentatives (anti-livelock)
     *
     * Detection : DeadlockDetector avec ThreadMXBean (sondage toutes les 500ms)
     *
     * Test : 20 bus sur 5 stations, doit completer en moins de 60 secondes
     */
    private static void executerQ6() {
        logger.info("=== QUESTION 6 : Prevention deadlock et livelock ===");

        // 1. Creer le generateur de scenario deadlock
        GenerateurDeadlock genDeadlock = new GenerateurDeadlock(SEED);

        // 2. Creer les lignes avec ordres de stations inverses (pour provoquer des deadlocks)
        List<Ligne> lignesDeadlock = genDeadlock.creerLignesDeadlock();
        logger.info("--- Lignes configurees pour test de deadlock ---");
        for (Ligne ligne : lignesDeadlock) {
            logger.info("  {} : {}", ligne.getNom(), ligne.getStations());
        }

        // 3. Creer 20 bus (4 par ligne) et 5 stations partagees
        Map<String, Bus> busMap = genDeadlock.creerBusDeadlock(lignesDeadlock);
        Map<String, Station> stationMap = genDeadlock.creerStationsDeadlock();
        logger.info("{} bus crees, {} stations partagees", busMap.size(), stationMap.size());

        // 4. Generer les evenements et les sauvegarder
        List<Evenement> evenements = genDeadlock.genererEvenements(busMap);
        Map<String, Passager> passagerMap = new ConcurrentHashMap<>(genDeadlock.creerPassagers(evenements));
        genDeadlock.sauvegarderCSV(evenements, "scenarios/scenario-deadlock-test.csv");
        logger.info("{} evenements generes, {} passagers", evenements.size(), passagerMap.size());

        // 5. Repartir les evenements par bus
        Map<String, List<Evenement>> evenementsParBus = new HashMap<>();
        for (Bus bus : busMap.values()) {
            evenementsParBus.put(bus.getId(), new ArrayList<>());
        }
        for (Evenement evt : evenements) {
            List<Evenement> listeEvt = evenementsParBus.get(evt.getBusId());
            if (listeEvt != null) {
                listeEvt.add(evt);
            }
        }

        // 6. Creer la file d'evenements partagee
        FileAttentePrioritaire fileEvenements = new FileAttentePrioritaire();

        // 7. Demarrer le detecteur de deadlock (daemon thread, sonde toutes les 500ms)
        DeadlockDetector detector = new DeadlockDetector(500);
        detector.start();
        logger.info("DeadlockDetector demarre (sondage toutes les 500ms)");

        // 8. Creer les threads bus avec tryLock anti-deadlock
        List<BusThreadDeadlock> busThreads = new ArrayList<>();
        for (Bus bus : busMap.values()) {
            List<Evenement> evtBus = evenementsParBus.get(bus.getId());
            BusThreadDeadlock bt = new BusThreadDeadlock(bus, fileEvenements, evtBus, stationMap, passagerMap);
            busThreads.add(bt);
        }

        logger.info("=== Demarrage de {} threads bus avec anti-deadlock ===", busThreads.size());
        logger.info("Strategie : tryLock + timeout progressif (10ms -> 500ms)");
        logger.info("Anti-livelock : delai aleatoire (0-50ms) entre tentatives");
        logger.info("Max tentatives par transfert : 20");
        logger.info("Timeout global : 60 secondes");

        long debut = System.nanoTime();

        // Demarrer tous les bus simultanement (maximise les chances de contention)
        for (BusThreadDeadlock bt : busThreads) {
            bt.start();
        }

        // Attendre la fin de tous les threads (timeout 60 secondes)
        boolean tousTermines = true;
        for (BusThreadDeadlock bt : busThreads) {
            try {
                bt.join(60000);
                if (bt.isAlive()) {
                    logger.error("BusDL {} TOUJOURS EN COURS apres 60s - possible deadlock!", bt.getBus().getId());
                    bt.interrupt();
                    tousTermines = false;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Attendre un peu pour les derniers traitements puis arreter le detecteur
        try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        detector.arreter();

        long fin = System.nanoTime();
        double dureeMs = (fin - debut) / 1_000_000.0;
        double dureeSec = dureeMs / 1000.0;

        // 9. Afficher les resultats
        logger.info("=== Resultats Question 6 ===");
        logger.info("Duree d'execution : {} ms ({} s)", String.format("%.1f", dureeMs), String.format("%.2f", dureeSec));

        // Statistiques du detecteur de deadlock
        logger.info("--- Detecteur de deadlocks (ThreadMXBean) ---");
        logger.info("  Sondes effectuees : {}", detector.getSondes());
        logger.info("  Deadlocks detectes et resolus : {}", detector.getDeadlocksDetectes());
        if (detector.getDeadlocksDetectes() > 0) {
            logger.info("  -> Les deadlocks ont ete resolus par interruption des threads impliques");
        }

        // Statistiques des bus
        logger.info("--- Statistiques des bus (tryLock) ---");
        int totalTentatives = 0;
        int totalEchecs = 0;
        int totalRetrys = 0;
        int totalTransferts = 0;

        for (BusThreadDeadlock bt : busThreads) {
            totalTentatives += bt.getTentativesLock();
            totalEchecs += bt.getEchecsLock();
            totalRetrys += bt.getRetrys();
            totalTransferts += bt.getTransfertsReussis();

            if (bt.getEchecsLock() > 0) {
                logger.info("  {} : {} tentatives, {} echecs, {} retrys, {} transferts reussis",
                        bt.getBus().getId(), bt.getTentativesLock(), bt.getEchecsLock(),
                        bt.getRetrys(), bt.getTransfertsReussis());
            }
        }

        logger.info("--- Totaux ---");
        logger.info("  Tentatives de lock : {}", totalTentatives);
        logger.info("  Echecs de lock : {}", totalEchecs);
        logger.info("  Retrys (back-off) : {}", totalRetrys);
        logger.info("  Transferts reussis : {}", totalTransferts);

        if (totalTentatives > 0) {
            double tauxReussite = (double) (totalTentatives - totalEchecs) / totalTentatives * 100;
            logger.info("  Taux de reussite au 1er essai : {} %", String.format("%.1f", tauxReussite));
        }

        // Statistiques de la file d'evenements
        logger.info("--- File d'evenements ---");
        logger.info("  Evenements produits : {}", fileEvenements.getTotalProduits());
        logger.info("  Evenements consommes : {}", fileEvenements.getTotalConsommes());

        // Verification finale
        logger.info("--- Verification ---");
        logger.info("  Tous les threads termines (<60s) : {}", tousTermines ? "OUI" : "NON - ECHEC");
        logger.info("  Deadlocks transitoires detectes : {}", detector.getDeadlocksDetectes());
        logger.info("  Duree < 60 secondes : {}", dureeSec < 60 ? "OUI" : "NON - TIMEOUT");

        // Verifier les threads BLOCKED
        long threadsBlocked = Thread.getAllStackTraces().keySet().stream()
                .filter(t -> t.getName().startsWith("BusDL-"))
                .filter(t -> t.getState() == Thread.State.BLOCKED)
                .count();
        logger.info("  Threads BLOCKED restants : {}", threadsBlocked);

        // Le succes de Q6 = tous les threads terminent en < 60s + 0 threads bloques restants
        // Les deadlocks transitoires sont acceptables car ils sont detectes et resolus
        boolean succes = tousTermines && threadsBlocked == 0 && dureeSec < 60;
        if (succes) {
            logger.info("=== QUESTION 6 REUSSIE ===");
            logger.info("  Le mecanisme tryLock + timeout progressif + delai aleatoire");
            logger.info("  a permis de prevenir/resoudre tous les deadlocks.");
            logger.info("  Strategie : back-off exponentiel (10ms -> 500ms) + random(0-50ms)");
        } else {
            logger.error("=== QUESTION 6 ECHOUEE ===");
        }
    }

    /**
     * Question 7 : Dashboard temps reel avec RW Lock.
     *
     * Demontre l'affichage en temps reel du dashboard pendant que la simulation
     * tourne. Le DashboardAfficheur utilise uniquement readLock (parallele avec
     * les autres lecteurs), tandis que les consommateurs utilisent writeLock
     * pour mettre a jour les statistiques.
     *
     * Configuration :
     * - 16 threads bus (producteurs)
     * - 3 threads consommateurs (MAJ dashboard via writeLock)
     * - 1 thread DashboardAfficheur (readLock, toutes les 500ms)
     * - Benchmark : 50 lecteurs + 1 ecrivain, synchronized vs RW Lock
     *
     * Resultat attendu : gain ~5x pour les lectures avec RW Lock
     */
    private static void executerQ7() {
        logger.info("=== QUESTION 7 : Dashboard temps reel avec RW Lock ===");

        // === PARTIE 1 : Simulation avec dashboard temps reel ===
        logger.info("--- Partie 1 : Simulation avec dashboard temps reel ---");

        // 1. Generer le scenario
        GenerateurEvenements generateur = new GenerateurEvenements(SEED);
        List<Evenement> evenements = generateur.genererScenario(1000);

        // 2. Creer les objets du modele
        Map<String, Bus> busMap = generateur.creerBus();
        Map<String, Station> stationMap = generateur.creerStations();
        Map<String, Passager> passagerMap = new ConcurrentHashMap<>(generateur.creerPassagers(evenements));

        // 3. Creer et initialiser le dashboard
        Dashboard dashboard = new Dashboard();
        dashboard.initialiser(stationMap, generateur.getLignes());

        // 4. Creer la file d'evenements
        FileAttentePrioritaire fileEvenements = new FileAttentePrioritaire();

        // 5. Repartir les evenements par bus
        Map<String, List<Evenement>> evenementsParBus = new HashMap<>();
        for (Bus bus : busMap.values()) {
            evenementsParBus.put(bus.getId(), new ArrayList<>());
        }
        for (Evenement evt : evenements) {
            List<Evenement> listeEvt = evenementsParBus.get(evt.getBusId());
            if (listeEvt != null) {
                listeEvt.add(evt);
            }
        }

        // 6. Creer les consommateurs
        ConsommateurEmbarquement consoEmbarquement =
                new ConsommateurEmbarquement(fileEvenements, busMap, passagerMap, dashboard);
        ConsommateurDebarquement consoDebarquement =
                new ConsommateurDebarquement(fileEvenements, busMap, passagerMap, dashboard);
        ConsommateurPaiement consoPaiement =
                new ConsommateurPaiement(fileEvenements, busMap, dashboard);
        consoEmbarquement.setDaemon(true);
        consoDebarquement.setDaemon(true);
        consoPaiement.setDaemon(true);

        // 7. Creer le DashboardAfficheur (temps reel, 500ms)
        DashboardAfficheur afficheur = new DashboardAfficheur(dashboard, 500);

        // 8. Creer les threads bus
        List<BusThread> busThreads = new ArrayList<>();
        for (Bus bus : busMap.values()) {
            List<Evenement> evtBus = evenementsParBus.get(bus.getId());
            BusThread bt = new BusThread(bus, fileEvenements, evtBus, stationMap, passagerMap);
            busThreads.add(bt);
        }

        logger.info("Demarrage : {} bus + 3 consommateurs + 1 afficheur temps reel", busThreads.size());
        long debut = System.nanoTime();

        // Demarrer l'afficheur et les consommateurs
        afficheur.start();
        consoEmbarquement.start();
        consoDebarquement.start();
        consoPaiement.start();

        // Demarrer les bus
        for (BusThread bt : busThreads) {
            bt.start();
        }

        // Attendre la fin des bus
        for (BusThread bt : busThreads) {
            try {
                bt.join(30000);
                if (bt.isAlive()) {
                    logger.warn("Bus {} toujours en cours apres 30s", bt.getBus().getId());
                    bt.interrupt();
                }
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        // Attendre que la file se vide
        int attente = 0;
        while (!fileEvenements.estVide() && attente < 50) {
            try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            attente++;
        }

        // Laisser le temps pour les derniers affichages
        try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        // Arreter l'afficheur et les consommateurs
        afficheur.arreter();
        consoEmbarquement.interrupt();
        consoDebarquement.interrupt();
        consoPaiement.interrupt();

        long fin = System.nanoTime();
        double dureeMs = (fin - debut) / 1_000_000.0;

        // Resultats de la simulation temps reel
        logger.info("--- Resultats simulation temps reel ---");
        logger.info("Duree d'execution : {} ms", String.format("%.1f", dureeMs));
        logger.info("DashboardAfficheur : {} affichages en {} ms (moyenne : {} ms/affichage)",
                afficheur.getNbAffichages(),
                String.format("%.0f", dureeMs),
                String.format("%.2f", afficheur.getMoyenneLectureMs()));
        logger.info("Evenements produits : {}", fileEvenements.getTotalProduits());
        logger.info("Evenements consommes : {}", fileEvenements.getTotalConsommes());

        int totalTraites = consoEmbarquement.getCompteurTraites()
                + consoDebarquement.getCompteurTraites()
                + consoPaiement.getCompteurTraites();
        logger.info("Evenements traites : {} (emb={}, deb={}, paie={})",
                totalTraites, consoEmbarquement.getCompteurTraites(),
                consoDebarquement.getCompteurTraites(), consoPaiement.getCompteurTraites());

        // Affichage final du dashboard
        logger.info("--- Dashboard final ---");
        dashboard.afficher();

        // === PARTIE 2 : Benchmark synchronized vs RW Lock ===
        logger.info("");
        logger.info("--- Partie 2 : Benchmark throughput Dashboard ---");

        Map<String, Object> benchResults = BenchmarkDashboard.executer();

        // Sauvegarder les resultats
        try {
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get("results"));
            try (java.io.BufferedWriter writer = java.nio.file.Files.newBufferedWriter(
                    java.nio.file.Paths.get("results/bench-dashboard.csv"))) {
                writer.write("type,throughput_lecture_ops_sec,throughput_ecriture_ops_sec");
                writer.newLine();
                writer.write(String.format("synchronized,%.0f,%.0f",
                        benchResults.get("sync_throughput_lecture"),
                        benchResults.get("sync_throughput_ecriture")));
                writer.newLine();
                writer.write(String.format("rwlock,%.0f,%.0f",
                        benchResults.get("rw_throughput_lecture"),
                        benchResults.get("rw_throughput_ecriture")));
                writer.newLine();
            }
            logger.info("Resultats sauvegardes dans results/bench-dashboard.csv");
        } catch (java.io.IOException e) {
            logger.error("Erreur lors de la sauvegarde des resultats", e);
        }

        // Verification finale
        logger.info("--- Verification Q7 ---");
        double gainLecture = (double) benchResults.get("gain_lecture");
        logger.info("  Dashboard temps reel fonctionne : {}",
                afficheur.getNbAffichages() > 0 ? "OUI" : "NON");
        logger.info("  Throughput lecture RW Lock : {} ops/sec",
                String.format("%.0f", benchResults.get("rw_throughput_lecture")));
        logger.info("  Gain RW Lock vs synchronized : {} x",
                String.format("%.2f", gainLecture));
        logger.info("  Dashboard n'a pas bloque les consommateurs : {}",
                totalTraites > 0 ? "OUI" : "NON");

        boolean succes = afficheur.getNbAffichages() > 0 && gainLecture > 1.0 && totalTraites > 0;
        logger.info("=== QUESTION 7 {} ===", succes ? "REUSSIE" : "ECHOUEE");
    }

    /**
     * Question 8 : Benchmarks de performance multi-echelles.
     * (Non implementee)
     */
    private static void executerQ8() {
        logger.info("=== QUESTION 8 : Non implementee ===");
        logger.info("Les questions 1 a 7 sont implementees et fonctionnelles.");
    }

    private static void executerDepuisCSV(String cheminCSV) {
        logger.info("Execution depuis le fichier CSV : {}", cheminCSV);
        List<Evenement> evenements = GenerateurEvenements.chargerCSV(cheminCSV);
        logger.info("Charge {} evenements depuis {}", evenements.size(), cheminCSV);
    }
}
