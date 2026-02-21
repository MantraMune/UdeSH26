package udes.fsci.info.ift630.transport;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import udes.fsci.info.ift630.transport.evenements.BusArrivee;
import udes.fsci.info.ift630.transport.evenements.BusDepart;
import udes.fsci.info.ift630.transport.evenements.EntreeSysteme;
import udes.fsci.info.ift630.transport.evenements.Evenement;
import udes.fsci.info.ift630.transport.evenements.Paiement;
import udes.fsci.info.ift630.transport.evenements.PassagerDescend;
import udes.fsci.info.ift630.transport.evenements.PassagerMonte;
import udes.fsci.info.ift630.transport.util.LecteurCSV;

/**
 * Système de traitement d'événements de transport en temps réel.
 * 
 * TP1 - IFT630 - Hiver 2026
 * @author Luc Nathan Ramasamy (raml2101)
 */
public class SystemeTransport {
    
    private static final Logger logger = LoggerFactory.getLogger(SystemeTransport.class);
    
    // ====================================================================
    // STRUCTURES FOURNIES
    // ====================================================================
    
    private final BlockingQueue<Evenement> fileEvenements;
    private final List<Thread> threadsTraitement;
    private volatile boolean enExecution;
    private final AtomicInteger evenementsEnAttente;
    
    // ====================================================================
    // STRUCTURES DE DONNÉES THREAD-SAFE
    // ====================================================================
    
    // Occupation par bus (thread-safe)
    private final ConcurrentHashMap<String, AtomicInteger> passagersParBus;
    
    // Capacités maximales par bus (immuable après init)
    private final ConcurrentHashMap<String, Integer> capacitesMaxBus;
    
    // Locks par bus pour opérations atomiques check-then-act
    private final ReentrantLock[] locksStripedBus;
    private final int NB_LOCKS = 16; // Nombre de verrous 
    
    // Compteurs globaux (thread-safe)
    private final AtomicInteger totalPassagersTransportes;
    private final AtomicInteger totalMontees;
    private final AtomicInteger totalDescentes;
    
    // Revenus (double nécessite synchronisation)
    private final AtomicInteger revenusTotal; // Change le type double en AtomicInteger pour éviter une synchronisation explicite (verrous)
    private final Object lockRevenus = new Object(); // Verrou (optionnel avec AtomicInteger)

    // Refus d'embarquement par bus
    private final ConcurrentHashMap<String, AtomicInteger> refusParBus;
    
    // Alertes 90% par bus
    private final ConcurrentHashMap<String, Boolean> alertes90ParBus;
    
    // Suivi des passagers pour voyages complets
    private final Set<String> passagersMontes;
    private final Set<String> passagersDescendus;
    private final Object lockVoyages = new Object();
    
    // ====================================================================
    // CALCULS ADDITIONNELS DE L'ÉNONCÉ
    // ====================================================================
    
    // Revenus par ligne
    private final ConcurrentHashMap<String, AtomicInteger> revenusParLigne;
    
    // Revenus par méthode de paiement
    private final ConcurrentHashMap<String, AtomicInteger> revenusParMethode;
    
    // Occupation maximale atteinte par bus
    private final ConcurrentHashMap<String, AtomicInteger> occupationMaxAtteinte;
    
    // Embarquements par arrêt
    private final ConcurrentHashMap<String, AtomicInteger> embarqParArret;
    
    // Débarquements par arrêt
    private final ConcurrentHashMap<String, AtomicInteger> debarqParArret;

    // Ajout d'un compteur de threads actifs (pour debug/perf)
    private final AtomicInteger threadsActifs = new AtomicInteger(0);
    
    public SystemeTransport() {
        this.fileEvenements = new LinkedBlockingQueue<>();
        this.threadsTraitement = new ArrayList<>();
        this.enExecution = true;
        this.evenementsEnAttente = new AtomicInteger(0);
        
        // Initialisation des structures
        this.passagersParBus = new ConcurrentHashMap<>();
        this.capacitesMaxBus = new ConcurrentHashMap<>();
        this.locksStripedBus = new ReentrantLock[NB_LOCKS];
        for (int i = 0; i < NB_LOCKS; i++) {
            this.locksStripedBus[i] = new ReentrantLock();
        }
        this.totalPassagersTransportes = new AtomicInteger(0);
        this.totalMontees = new AtomicInteger(0);
        this.totalDescentes = new AtomicInteger(0);
        this.revenusTotal = new AtomicInteger(0);
        this.refusParBus = new ConcurrentHashMap<>();
        this.alertes90ParBus = new ConcurrentHashMap<>();
        this.passagersMontes = ConcurrentHashMap.newKeySet();
        this.passagersDescendus = ConcurrentHashMap.newKeySet();
        
        // Initialisation calculs énoncé
        this.revenusParLigne = new ConcurrentHashMap<>();
        this.revenusParMethode = new ConcurrentHashMap<>();
        this.occupationMaxAtteinte = new ConcurrentHashMap<>();
        this.embarqParArret = new ConcurrentHashMap<>();
        this.debarqParArret = new ConcurrentHashMap<>();
        
        int nombreThreads = Runtime.getRuntime().availableProcessors();
        for (int i = 0; i < nombreThreads; i++) {
            Thread t = new Thread(this::boucleTraitement, "Thread-Traitement-" + i);
            t.start();
            threadsTraitement.add(t);
        }
        
        logger.info("Système démarré avec {} threads", nombreThreads);
    }
    
    public void traiterFichier(String nomFichier) {
        reinitialiser(); // Réinitialiser l'état avant traitement car plusieurs tests (csv) peuvent être lancés
        try {
            List<Evenement> evenements = LecteurCSV.lireFichier(nomFichier);
            evenements.sort(Comparator.comparingLong(Evenement::getTimestamp));
            logger.info("Fichier {} chargé : {} événements", nomFichier, evenements.size());

            // Initialiser les bus avant de traiter les événements ---
            for (Evenement evt : evenements) {
                switch (evt) {
                    case PassagerMonte passagerMonte -> {
                        String busId = passagerMonte.getBusId();
                        // Initialise le bus si non existant; valeur par défaut 50
                        setCapaciteBus(busId, 50);
                    }
                    case PassagerDescend passagerDescend -> {
                        String busId = passagerDescend.getBusId();
                        setCapaciteBus(busId, 50);
                    }
                    case Paiement paiement -> {
                        // Si tu as besoin d'initialiser revenusParMethode par défaut
                        String methode = paiement.getMethodePaiement();
                        revenusParMethode.putIfAbsent(methode, new AtomicInteger(0));
                    }
                    default -> {
                    }
                }
            }
            
            for (Evenement evt : evenements) {
                fileEvenements.put(evt);
                evenementsEnAttente.incrementAndGet();
            }
            
        } catch (IOException e) {
            logger.error("Erreur lecture fichier {}", nomFichier, e);
        } catch (InterruptedException e) {
            logger.error("Interruption", e);
            Thread.currentThread().interrupt();
        }
    }
    
    public void attendreFinTraitement() throws InterruptedException {
        while (evenementsEnAttente.get() > 0 || threadsActifs.get() > 0) {
            Thread.sleep(50);
        }
        // Force memory barrier - s'assurer que tous les changements sont visibles
        synchronized (this) {
            Thread.sleep(500); // Petite attente pour s'assurer que les threads terminent
        }
        logger.info("Traitement des événements terminé");
    }
    
    public void arreter() {
        enExecution = false;
        for (Thread t : threadsTraitement) {
            t.interrupt();
        }
        for (Thread t : threadsTraitement) {
            try {
                t.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        logger.info("Système arrêté");
    }
    
    public boolean estActif() {
        return enExecution;
    }
    
    // ====================================================================
    // BOUCLE DE TRAITEMENT
    // ====================================================================
    
    private void boucleTraitement() {
        logger.debug("Thread {} démarré", Thread.currentThread().getName());
        
        while (enExecution) {
            try {
                Evenement evt = fileEvenements.poll(100, TimeUnit.MILLISECONDS);
                
                if (evt != null) {
                    threadsActifs.incrementAndGet();
                    try {
                        traiterEvenement(evt);
                    } finally {
                        threadsActifs.decrementAndGet();
                        evenementsEnAttente.decrementAndGet();
                    }
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        logger.debug("Thread {} terminé", Thread.currentThread().getName());
    }
    
    private void traiterEvenement(Evenement evt) {
        logger.debug("Traitement: {}", evt);
        
        switch (evt.getType()) {
            case ENTREE_SYSTEME:
                traiterEntreeSysteme((EntreeSysteme) evt);
                break;
            case PASSAGER_MONTE:
                traiterPassagerMonte((PassagerMonte) evt);
                break;
            case PASSAGER_DESCEND:
                traiterPassagerDescend((PassagerDescend) evt);
                break;
            case PAIEMENT:
                traiterPaiement((Paiement) evt);
                break;
            case BUS_ARRIVEE:
                traiterBusArrivee((BusArrivee) evt);
                break;
            case BUS_DEPART:
                traiterBusDepart((BusDepart) evt);
                break;
        }
    }
    
    // ====================================================================
    // MÉTHODES DE TRAITEMENT
    // ====================================================================
    
    private void traiterEntreeSysteme(EntreeSysteme evt) {
        logger.debug("Passager {} entre dans le système", evt.getPassagerId());
    }
    
    private void traiterPassagerMonte(PassagerMonte evt) {
        String busId = evt.getBusId();
        String passagerId = evt.getPassagerId();
        String ligne = evt.getLigne();
        String arret = evt.getArret();

        // Avant de commencer, acquérir le lock pour ce bus afin d'assurer l'atomicité
        ReentrantLock lock = getLock(busId);
        lock.lock();
        try {
            // Capacité maximale du bus
            int capacite = capacitesMaxBus.get(busId);
        
            // Occupation actuelle du bus
            AtomicInteger occupation = passagersParBus.get(busId); 

            // Vérifier si le bus est plein ou non
            if(occupation.get() < capacite) {

                occupation.incrementAndGet(); // Incrémenter l'occupation du bus
                totalMontees.incrementAndGet(); // Incrémenter le total des montées
                
                embarqParArret.computeIfAbsent(arret, k -> new AtomicInteger(0)).incrementAndGet(); // Incrémenter embarqParArret pour cet arrêt

                // Mettre à jour occupationMaxAtteinte si nécessaire
                AtomicInteger occupationMax = occupationMaxAtteinte.get(busId);
                occupationMax.updateAndGet(max -> Math.max(max, occupation.get()));

                // Synchroniser les voyages complets
                synchronized (lockVoyages) {
                    passagersMontes.add(passagerId); // Ajouter le passager à l'ensemble des passagers montés
                }
            
            } else {
                // Bus plein - refuser l'embarquement donc incrémenter refus[busId]
                AtomicInteger refus = refusParBus.get(busId);
                refus.incrementAndGet();
                logger.debug("Bus {} plein - refus embarquement passager {}", busId, passagerId);
            }
            if (occupation.get() >= 0.9 * capacite) {

                alertes90ParBus.put(busId, true); // Alerte si occupation >= 90% de la capacité
                logger.warn("Le bus {} a atteint 90% de sa capacité", busId);
            }
        } finally {
            lock.unlock();
        }
        logger.debug("Passager {} monte dans bus {}", passagerId, busId);
    }
    
    private void traiterPassagerDescend(PassagerDescend evt) {
        String busId = evt.getBusId();
        String passagerId = evt.getPassagerId();
        String arret = evt.getArret();

        // Acquérir le lock pour ce bus afin d'assurer l'atomicité
        // comme avec traiterPassagerMonte
        ReentrantLock lock = getLock(busId);
        lock.lock();
        try {
            // Décrémenter l'occupation du bus
            AtomicInteger occupation = passagersParBus.get(busId);
            occupation.decrementAndGet();

            // Incrémenter totalDescentes
            totalDescentes.incrementAndGet();

            // Incrémenter debarqParArret pour cet arrêt
            embarqParArret.computeIfAbsent(arret, k -> new AtomicInteger(0)).incrementAndGet();

            // Synchroniser les voyages complets
            synchronized (lockVoyages) {
                passagersDescendus.add(passagerId); // Ajouter le passager à l'ensemble des passagers montés

                // Vérifier si le même passager est monté 
                if (passagersMontes.contains(passagerId)) {
                    totalPassagersTransportes.incrementAndGet(); // Incrémenter le total des passagers transportés
                }
            }
        } finally {
            lock.unlock();
        }
        logger.debug("Passager {} descend du bus {}", passagerId, busId);
    }
    
    private void traiterPaiement(Paiement evt) {
        double montant = evt.getMontant();
        String methode = evt.getMethodePaiement();
    
        // Stocker en centimes = montant × 100 (pour utiliser AtomicInteger)
        int centimes = (int) Math.round(montant * 100);

        // Maintenant que revenusTotal est un AtomicInteger, on peut l'incrémenter directement (thread-safe)
        // Ajouter montant aux revenus totaux (revenusTotal)
        revenusTotal.addAndGet(centimes);

        // Mise à jour du HashMap des revenus par méthode
        // Ajouter montant aux revenus par méthode (revenusParMethode)
        revenusParMethode.computeIfAbsent(methode, k -> new AtomicInteger(0)).addAndGet(centimes);

        logger.debug("Paiement de {} $ reçu (passager {}, méthode: {})", montant, evt.getPassagerId(), methode);
    }
    
    private void traiterBusArrivee(BusArrivee evt) {
        logger.debug("Bus {} arrive à {}", evt.getBusId(), evt.getArret());
    }
    
    private void traiterBusDepart(BusDepart evt) {
        String ligne = evt.getLigne();
        logger.debug("Bus {} part de {} (ligne: {})", evt.getBusId(), evt.getArret(), ligne);
    }
    
    // ====================================================================
    // GETTERS THREAD-SAFE
    // ====================================================================
    
    public void setCapaciteBus(String busId, int capacite) {
        capacitesMaxBus.put(busId, capacite);
        passagersParBus.putIfAbsent(busId, new AtomicInteger(0));
        refusParBus.putIfAbsent(busId, new AtomicInteger(0));
        alertes90ParBus.putIfAbsent(busId, false);
        occupationMaxAtteinte.putIfAbsent(busId, new AtomicInteger(0));
        logger.info("Capacité bus {} fixée à {}", busId, capacite);
    }
    
    public int getNombrePassagers(String busId) {
        AtomicInteger occupation = passagersParBus.get(busId);
        if (occupation != null) {
            return occupation.get();
        }
        return 0;
    }
    
    public int getTotalPassagersTransportes() {
        return totalPassagersTransportes.get();
    }
    
    public int getNombreEvenementsTraites() {
        return totalMontees.get() + totalDescentes.get();
    }
    
    public double getRevenusTotal() {
        return revenusTotal.get() / 100.0;
    }
    
    public int getTotalPassagersLigne(String ligne) {
        // Optionnel pour TP1
        return 0;
    }
    
    public int getNombreRefus(String busId) {
        AtomicInteger refus = refusParBus.get(busId);
        return refus != null ? refus.get() : 0;
    }
    
    public boolean aAlerte90Pourcent(String busId) {
        return alertes90ParBus.getOrDefault(busId, false);
    }
    
    public Set<String> getPassagersMontes() {
        return new HashSet<>(passagersMontes);
    }
    
    public Set<String> getPassagersDescendus() {
        return new HashSet<>(passagersDescendus);
    }
    
    public int getTotalMontees() {
        return totalMontees.get();
    }
    
    public int getTotalDescentes() {
        return totalDescentes.get();
    }
    
    // ====================================================================
    // GETTERS CALCULS ÉNONCÉ
    // ====================================================================
    
    public double getRevenusLigne(String ligne) {
        AtomicInteger centimes = revenusParLigne.get(ligne);
        return centimes != null ? centimes.get() / 100.0 : 0.0;
    }
    
    public double getRevenusMethode(String methode) {
        AtomicInteger centimes = revenusParMethode.get(methode);
        return centimes != null ? centimes.get() / 100.0 : 0.0;
    }
    
    public int getOccupationMaxAtteinte(String busId) {
        AtomicInteger max = occupationMaxAtteinte.get(busId);
        return max != null ? max.get() : 0;
    }
    
    public int getEmbarqParArret(String arret) {
        AtomicInteger count = embarqParArret.get(arret);
        return count != null ? count.get() : 0;
    }
    
    public int getDebarqParArret(String arret) {
        AtomicInteger count = debarqParArret.get(arret);
        return count != null ? count.get() : 0;
    }
    
    public Map<String, Integer> getTousEmbarqParArret() {
        Map<String, Integer> result = new HashMap<>();
        embarqParArret.forEach((arret, count) -> result.put(arret, count.get()));
        return result;
    }
    
    public Map<String, Integer> getTousDebarqParArret() {
        Map<String, Integer> result = new HashMap<>();
        debarqParArret.forEach((arret, count) -> result.put(arret, count.get()));
        return result;
    }

    private ReentrantLock getLock(String busId) {
        int index = Math.abs(busId.hashCode() % NB_LOCKS);
        return locksStripedBus[index];
    }
    
    // ====================================================================
    // AFFICHAGE DES RÉSULTATS
    // ====================================================================
    
    public void afficherResultats(String nomTest) throws IOException {
        Map<String, Object> resultats = new HashMap<>();
        
        System.out.println("\n" + "=".repeat(70));
        System.out.println("RESULTATS - " + nomTest);
        System.out.println("=".repeat(70));
        
        // Question 1: Revenus total
        System.out.println("\nQuestion 1: Revenus total");
        double revenus = getRevenusTotal();
        System.out.printf("  Reponse: %.2f\n", revenus);
        resultats.put("revenus_total", revenus);
        
        // Question 2: Passagers transportés
        System.out.println("\nQuestion 2: Passagers transportes (voyages complets)");
        int transportes = getTotalPassagersTransportes();
        System.out.printf("  Reponse: %d\n", transportes);
        resultats.put("passagers_transportes", transportes);
        
        // Question 3: Total montées
        System.out.println("\nQuestion 3: Total montees");
        int montees = getTotalMontees();
        System.out.printf("  Reponse: %d\n", montees);
        resultats.put("total_montees", montees);
        
        // Question 4: Total descentes
        System.out.println("\nQuestion 4: Total descentes");
        int descentes = getTotalDescentes();
        System.out.printf("  Reponse: %d\n", descentes);
        resultats.put("total_descentes", descentes);
        
        // Question 5: Occupation des bus
        System.out.println("\nQuestion 5: Occupation des bus");
        List<String> busList = Arrays.asList(
            "B011", "B012", "B021", "B022", "B023", "B024", "B025", "B031",
            "B041", "B042", "B051", "B052",
            "B061", "B062", "B063", "B064", "B065", "B066", "B067", "B068", "B069", "B070"
        );
        Map<String, Integer> occupationBus = new HashMap<>();
        for (String busId : busList) {
            int occupation = getNombrePassagers(busId);
            if (occupation >= 0) {
                System.out.printf("  %s: %d passagers\n", busId, occupation);
                occupationBus.put(busId, occupation);
            }
        }
        resultats.put("occupation_bus", occupationBus);
        
        // Question 6: Refus
        System.out.println("\nQuestion 6: Refus d'embarquement");
        Map<String, Integer> refus = new HashMap<>();
        boolean hasRefus = false;
        for (String busId : busList) {
            int refusCount = getNombreRefus(busId);
            if (refusCount > 0) {
                System.out.printf("  %s: %d refus\n", busId, refusCount);
                refus.put(busId, refusCount);
                hasRefus = true;
            }
        }
        if (!hasRefus) {
            System.out.println("  (aucun refus)");
        } else {
            resultats.put("refus", refus);
        }
        
        // Question 7: Alertes 90%
        System.out.println("\nQuestion 7: Alertes capacite (90%)");
        Map<String, Boolean> alertes90 = new HashMap<>();
        boolean hasAlerte = false;
        for (String busId : busList) {
            boolean alerte = aAlerte90Pourcent(busId);
            if (alerte) {
                System.out.printf("  %s: OUI\n", busId);
                alertes90.put(busId, true);
                hasAlerte = true;
            }
        }
        if (!hasAlerte) {
            System.out.println("  (aucune alerte)");
        } else {
            resultats.put("alertes_90", alertes90);
        }
        
        // Question 8: Invariants
        System.out.println("\nQuestion 8: Verification invariants");
        int monteesVal = getTotalMontees();
        int descentesVal = getTotalDescentes();
        int occupationTotale = 0;
        for (String busId : busList) {
            occupationTotale += getNombrePassagers(busId);
        }
        boolean invariantOK = (monteesVal == descentesVal + occupationTotale);
        System.out.printf("  Conservation (montees = descentes + occupation): %s\n", 
                          invariantOK ? "RESPECTE" : "VIOLE");
        System.out.printf("    Montees: %d, Descentes: %d, Occupation: %d\n", 
                          monteesVal, descentesVal, occupationTotale);
        resultats.put("invariant_conservation", invariantOK);
        
        // Question 9: Revenus par méthode
        System.out.println("\nQuestion 9: Revenus par methode de paiement");
        Map<String, Double> revenusMethode = new HashMap<>();
        for (String methode : Arrays.asList("CARTE", "ESPECES", "MOBILE")) {
            double revenusMeth = getRevenusMethode(methode);
            if (revenusMeth > 0) {
                System.out.printf(Locale.US, "  %s: %.2f\n", methode, revenusMeth);
                revenusMethode.put(methode, revenusMeth);
            }
        }
        resultats.put("revenus_methode", revenusMethode);
        
        // Question 11: Embarquements par arrêt (top 5)
        System.out.println("\nQuestion 11: Embarquements par arret (top 5)");
        Map<String, Integer> embarqArret = new HashMap<>();
        List<Map.Entry<String, Integer>> embarqList = new ArrayList<>(getTousEmbarqParArret().entrySet());
        embarqList.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));
        for (int i = 0; i < Math.min(5, embarqList.size()); i++) {
            Map.Entry<String, Integer> e = embarqList.get(i);
            System.out.printf("  %s: %d embarquements\n", e.getKey(), e.getValue());
            embarqArret.put(e.getKey(), e.getValue());
        }
        resultats.put("embarq_arret", embarqArret);
        
        // Question 12: Débarquements par arrêt (top 5)
        System.out.println("\nQuestion 12: Debarquements par arret (top 5)");
        Map<String, Integer> debarqArret = new HashMap<>();
        List<Map.Entry<String, Integer>> debarqList = new ArrayList<>(getTousDebarqParArret().entrySet());
        debarqList.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));
        for (int i = 0; i < Math.min(5, debarqList.size()); i++) {
            Map.Entry<String, Integer> e = debarqList.get(i);
            System.out.printf("  %s: %d debarquements\n", e.getKey(), e.getValue());
            debarqArret.put(e.getKey(), e.getValue());
        }
        resultats.put("debarq_arret", debarqArret);
        
        // Question 10: Occupation max atteinte
        System.out.println("\nQuestion 10: Occupation maximale atteinte par bus");
        Map<String, Integer> occupationMax = new HashMap<>();
        for (String busId : busList) {
            int maxOccup = getOccupationMaxAtteinte(busId);
            if (maxOccup > 0) {
                System.out.printf("  Max %s: %d passagers\n", busId, maxOccup);
                occupationMax.put(busId, maxOccup);
            }
        }
        resultats.put("occupation_max", occupationMax);
        
        System.out.println("\n" + "=".repeat(70));
        
        // Écrire les résultats en JSON
        try {
            Files.createDirectories(Paths.get("logs"));
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (FileWriter writer = new FileWriter("logs/resultats_" + nomTest + ".json")) {
                gson.toJson(resultats, writer);
            }
            System.out.println("Résultats écrits dans logs/resultats_" + nomTest + ".json");
        } catch (IOException e) {
            logger.error("Erreur lors de l'écriture du fichier JSON", e);
        }
    }

    public void reinitialiser() {
        passagersParBus.clear();
        totalPassagersTransportes.set(0);
        totalMontees.set(0);
        totalDescentes.set(0);
        revenusTotal.set(0);
        refusParBus.clear();
        alertes90ParBus.clear();
        passagersMontes.clear();
        passagersDescendus.clear();
        revenusParMethode.clear();
        occupationMaxAtteinte.clear();
        embarqParArret.clear();
        debarqParArret.clear();
    }
}