package ca.usherbrooke.ift630.tp2.coordination;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.usherbrooke.ift630.tp2.modele.Evenement;
import ca.usherbrooke.ift630.tp2.modele.Ligne;
import ca.usherbrooke.ift630.tp2.modele.Priorite;
import ca.usherbrooke.ift630.tp2.modele.Station;

/**
 * Dashboard temps reel de la simulation STS.
 */
public class Dashboard {

    private static final Logger logger = LoggerFactory.getLogger(Dashboard.class);

    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(true);

    private final Map<String, AtomicInteger> passagersParStation = new ConcurrentHashMap<>();
    private final Map<Integer, AtomicLong> revenusParLigne = new ConcurrentHashMap<>();
    private final AtomicInteger totalPassagersPrioritaires = new AtomicInteger(0);
    private final AtomicLong revenusTotal = new AtomicLong(0);
    private final AtomicInteger totalEmbarquements = new AtomicInteger(0);
    private final AtomicInteger totalDebarquements = new AtomicInteger(0);
    private final AtomicInteger totalPaiements = new AtomicInteger(0);
    private final AtomicInteger totalEvenementsTraites = new AtomicInteger(0);

    public void initialiser(Map<String, Station> stations, java.util.List<Ligne> lignes) {
        rwLock.writeLock().lock();
        try {
            for (String nomStation : stations.keySet()) {
                passagersParStation.put(nomStation, new AtomicInteger(0));
            }
            for (Ligne ligne : lignes) {
                revenusParLigne.put(ligne.getNumero(), new AtomicLong(0));
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    // --- Methodes d'ecriture ---

    public void enregistrerEmbarquement(Evenement evt) {
        rwLock.writeLock().lock();
        try {
            totalEmbarquements.incrementAndGet();
            totalEvenementsTraites.incrementAndGet();
            AtomicInteger compteur = passagersParStation.get(evt.getStationNom());
            if (compteur != null) compteur.incrementAndGet();
            if (evt.getPrioritePassager() == Priorite.HAUTE) totalPassagersPrioritaires.incrementAndGet();
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public void enregistrerDebarquement(Evenement evt) {
        rwLock.writeLock().lock();
        try {
            totalDebarquements.incrementAndGet();
            totalEvenementsTraites.incrementAndGet();
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public void enregistrerPaiement(Evenement evt, long montantCentimes, int numeroLigne) {
        rwLock.writeLock().lock();
        try {
            totalPaiements.incrementAndGet();
            totalEvenementsTraites.incrementAndGet();
            revenusTotal.addAndGet(montantCentimes);
            AtomicLong revenuLigne = revenusParLigne.get(numeroLigne);
            if (revenuLigne != null) revenuLigne.addAndGet(montantCentimes);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    // --- Methodes de lecture ---

    public Map<String, Integer> getStatistiquesStations() {
        rwLock.readLock().lock();
        try {
            return passagersParStation.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public Map<Integer, Long> getRevenusParLigne() {
        rwLock.readLock().lock();
        try {
            return revenusParLigne.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public double getRevenusTotalDollars() { rwLock.readLock().lock(); try { return revenusTotal.get() / 100.0; } finally { rwLock.readLock().unlock(); } }
    public int getTotalPassagersPrioritaires() { rwLock.readLock().lock(); try { return totalPassagersPrioritaires.get(); } finally { rwLock.readLock().unlock(); } }
    public int getTotalEvenementsTraites() {  rwLock.readLock().lock(); try { return totalEvenementsTraites.get(); } finally { rwLock.readLock().unlock(); } }
    public int getTotalEmbarquements() { rwLock.readLock().lock(); try { return totalEmbarquements.get(); } finally { rwLock.readLock().unlock(); } }
    public int getTotalDebarquements() { rwLock.readLock().lock(); try { return totalDebarquements.get(); } finally { rwLock.readLock().unlock(); } }
    public int getTotalPaiements() { rwLock.readLock().lock(); try { return totalPaiements.get(); } finally { rwLock.readLock().unlock(); } }

    public ReentrantReadWriteLock getRwLock() {
        return rwLock;
    }

    public void afficher() {
        rwLock.readLock().lock();
        try {
            logger.info("========== DASHBOARD STS ==========");
            logger.info("Evenements traites : {}", totalEvenementsTraites.get());
            logger.info("  Embarquements : {}", totalEmbarquements.get());
            logger.info("  Debarquements : {}", totalDebarquements.get());
            logger.info("  Paiements : {}", totalPaiements.get());
            logger.info("Revenus totaux : {} $", String.format("%.2f", revenusTotal.get() / 100.0));
            logger.info("Passagers prioritaires : {}", totalPassagersPrioritaires.get());
            logger.info("--- Passagers par station ---");
            for (Map.Entry<String, AtomicInteger> entry : passagersParStation.entrySet()) {
                if (entry.getValue().get() > 0) logger.info("  {} : {}", entry.getKey(), entry.getValue().get());
            }
            logger.info("--- Revenus par ligne ---");
            for (Map.Entry<Integer, AtomicLong> entry : revenusParLigne.entrySet()) {
                if (entry.getValue().get() > 0) logger.info("  Ligne {} : {} $", entry.getKey(), String.format("%.2f", entry.getValue().get() / 100.0));
            }
            logger.info("====================================");
        } finally {
            rwLock.readLock().unlock();
        }
    }
}
