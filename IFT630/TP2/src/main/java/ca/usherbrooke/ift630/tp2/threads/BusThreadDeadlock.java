package ca.usherbrooke.ift630.tp2.threads;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.usherbrooke.ift630.tp2.coordination.FileAttentePrioritaire;
import ca.usherbrooke.ift630.tp2.modele.Bus;
import ca.usherbrooke.ift630.tp2.modele.Evenement;
import ca.usherbrooke.ift630.tp2.modele.Passager;
import ca.usherbrooke.ift630.tp2.modele.Station;
import ca.usherbrooke.ift630.tp2.modele.TypeEvenement;

/**
 * Version du BusThread specialisee pour la prevention des deadlocks.
 *
 * Scenario de deadlock potentiel :
 *   Bus A veut : Station Universite -> Station Cegep (lock Uni, puis lock Cegep)
 *   Bus B veut : Station Cegep -> Station Universite (lock Cegep, puis lock Uni)
 *   => DEADLOCK si acquisition simultanee
 *
 */
public class BusThreadDeadlock extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(BusThreadDeadlock.class);

    private static final long TIMEOUT_INITIAL_MS = 10;
    private static final double FACTEUR_TIMEOUT = 2.0;
    private static final long TIMEOUT_MAX_MS = 500;
    private static final int MAX_TENTATIVES = 20;
    private static final long DELAI_ALEATOIRE_MAX_MS = 50;

    private final Bus bus;
    private final FileAttentePrioritaire fileEvenements;
    private final List<Evenement> evenementsAssignes;
    private final Map<String, Station> stations;
    private final Map<String, Passager> passagers;
    private final Random random;

    private final AtomicInteger tentativesLock = new AtomicInteger(0);
    private final AtomicInteger echecsLock = new AtomicInteger(0);
    private final AtomicInteger retrys = new AtomicInteger(0);
    private final AtomicInteger transfertsReussis = new AtomicInteger(0);

    public BusThreadDeadlock(Bus bus, FileAttentePrioritaire fileEvenements,
                              List<Evenement> evenementsAssignes,
                              Map<String, Station> stations,
                              Map<String, Passager> passagers) {
        super("BusDL-" + bus.getId());
        this.bus = bus;
        this.fileEvenements = fileEvenements;
        this.evenementsAssignes = evenementsAssignes;
        this.stations = stations;
        this.passagers = passagers;
        this.random = new Random(bus.getId().hashCode());
    }

    @Override
    public void run() {
        logger.info("BusDL {} demarre sur la ligne {} ({} evenements)",
                bus.getId(), bus.getLigne().getNom(), evenementsAssignes.size());

        List<String> stationsLigne = bus.getLigne().getStations();

        for (int i = 0; i < stationsLigne.size(); i++) {
            if (Thread.interrupted()) {
                logger.info("BusDL {} interrompu", bus.getId());
                return;
            }

            String nomStationCourante = stationsLigne.get(i);
            Station stationCourante = stations.get(nomStationCourante);
            if (stationCourante == null) continue;

            if (i + 1 < stationsLigne.size()) {
                String nomStationSuivante = stationsLigne.get(i + 1);
                Station stationSuivante = stations.get(nomStationSuivante);
                if (stationSuivante != null) {
                    transfertEntreStations(stationCourante, stationSuivante);
                }
            }

            traiterEvenementsStation(stationCourante);
        }

        logger.info("BusDL {} termine. Transferts: {}, Tentatives: {}, Echecs: {}, Retrys: {}",
                bus.getId(), transfertsReussis.get(), tentativesLock.get(),
                echecsLock.get(), retrys.get());
    }

    /**
     * Effectue un transfert entre deux stations en acquirant les deux locks.
     */
    private void transfertEntreStations(Station stationA, Station stationB) {

        long timeout = TIMEOUT_INITIAL_MS;

        // Boucles de tentatives
        for (int i = 0; i < MAX_TENTATIVES; i++) {

            tentativesLock.incrementAndGet();
         
            boolean lockA = false;
            boolean lockB = false;

            try {
                lockA = stationA.getRwLock().writeLock().tryLock(timeout, TimeUnit.MILLISECONDS);

                if (lockA){
                    lockB = stationB.getRwLock().writeLock().tryLock(timeout, TimeUnit.MILLISECONDS);
                
                    if (lockB) // Succès
                    {
                        effectuerTransfert(stationA, stationB);
                        transfertsReussis.incrementAndGet();
                        return;
                    }
                }

                // Échec
                echecsLock.incrementAndGet();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } finally {
                // Rêlacher les locks après modification
                if (lockB) stationB.getRwLock().writeLock().unlock();
                if (lockA) stationA.getRwLock().writeLock().unlock();
            }

            // Retry
            retrys.incrementAndGet();

            // Anti-livelock
            try {
                Thread.sleep(random.nextInt((int) DELAI_ALEATOIRE_MAX_MS));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            //Augmenter le timeout (progressif)
            timeout = Math.min((long)(timeout * FACTEUR_TIMEOUT), TIMEOUT_MAX_MS);

        }

        logger.warn("BusDL {} : abandon de transfert {} -> {} après {} tentatives",
                bus.getId(), stationA.getNom(), stationB.getNom(), MAX_TENTATIVES);
    }

    /**
     * Effectue le transfert effectif entre deux stations.
     * Appele SEULEMENT quand les deux locks sont detenus.
     */
    private void effectuerTransfert(Station stationA, Station stationB) {
        logger.debug("BusDL {} : en transit {} -> {}", bus.getId(), stationA.getNom(), stationB.getNom());
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Traite les evenements pour la station donnee.
     */
    private void traiterEvenementsStation(Station station) {
        for (Evenement evt : evenementsAssignes) {
            if (evt.getType() == TypeEvenement.DEBARQUEMENT
                    && evt.getStationNom().equals(station.getNom())) {
                evt.setBus(bus);
                Passager passager = passagers.get(evt.getPassagerId());
                evt.setPassager(passager);
                if (passager != null) {
                    bus.debarquer(passager);
                }
                fileEvenements.produire(evt);
            }
        }

        for (Evenement evt : evenementsAssignes) {
            if (evt.getType() == TypeEvenement.EMBARQUEMENT
                    && evt.getStationNom().equals(station.getNom())) {
                evt.setBus(bus);
                Passager passager = passagers.get(evt.getPassagerId());
                if (passager == null) {
                    passager = new Passager(evt.getPassagerId(), evt.getPrioritePassager(),
                            evt.getStationNom(), station.getNom());
                    passagers.put(evt.getPassagerId(), passager);
                }
                evt.setPassager(passager);
                if (!bus.estPlein()) {
                    bus.embarquer(passager);
                    Evenement evtPaiement = new Evenement(
                            evt.getTimestamp(), TypeEvenement.PAIEMENT,
                            evt.getBusId(), evt.getPassagerId(),
                            evt.getStationNom(), evt.getPrioritePassager());
                    evtPaiement.setBus(bus);
                    evtPaiement.setPassager(passager);
                    fileEvenements.produire(evtPaiement);
                }
                fileEvenements.produire(evt);
            }
        }
    }

    public Bus getBus() { return bus; }
    public int getTentativesLock() { return tentativesLock.get(); }
    public int getEchecsLock() { return echecsLock.get(); }
    public int getRetrys() { return retrys.get(); }
    public int getTransfertsReussis() { return transfertsReussis.get(); }
}
