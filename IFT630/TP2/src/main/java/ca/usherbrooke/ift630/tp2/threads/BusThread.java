package ca.usherbrooke.ift630.tp2.threads;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.usherbrooke.ift630.tp2.coordination.FileAttentePrioritaire;
import ca.usherbrooke.ift630.tp2.modele.Bus;
import ca.usherbrooke.ift630.tp2.modele.Evenement;
import ca.usherbrooke.ift630.tp2.modele.Passager;
import ca.usherbrooke.ift630.tp2.modele.Station;
import ca.usherbrooke.ift630.tp2.modele.TypeEvenement;

/**
 * Thread representant un autobus du reseau STS.
 * Agit comme PRODUCTEUR d'evenements :
 * - Parcourt les stations de sa ligne
 * - Pour chaque station, genere des evenements d'embarquement/debarquement
 * - Depose les evenements dans la PriorityBlockingQueue partagee
 *
 * Synchronisation requise :
 * - Respecte la capacite max du bus (50 passagers)
 * - Si bus plein : attendre jusqu'a ce qu'un debarquement libere une place
 * - Apres debarquement : reveiller les threads en attente d'une place
 * - Coordonner l'arrivee aux stations avec les autres bus
 */
public class BusThread extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(BusThread.class);

    private final Bus bus;
    private final FileAttentePrioritaire fileEvenements;
    private final List<Evenement> evenementsAssignes;
    private final Map<String, Station> stations;
    private final Map<String, Passager> passagers;

    public BusThread(Bus bus, FileAttentePrioritaire fileEvenements,
                     List<Evenement> evenementsAssignes,
                     Map<String, Station> stations,
                     Map<String, Passager> passagers) {
        super("Bus-" + bus.getId());
        this.bus = bus;
        this.fileEvenements = fileEvenements;
        this.evenementsAssignes = evenementsAssignes;
        this.stations = stations;
        this.passagers = passagers;
    }

    @Override
    public void run() {
        logger.info("Bus {} demarre sur la ligne {} ({} evenements a traiter)",
                bus.getId(), bus.getLigne().getNom(), evenementsAssignes.size());

        List<String> stationsLigne = bus.getLigne().getStations();

        for (String nomStation : stationsLigne) {
            if (Thread.interrupted()) {
                logger.info("Bus {} interrompu", bus.getId());
                return;
            }

            Station station = stations.get(nomStation);
            if (station == null) {
                logger.warn("Bus {} : station '{}' non trouvee, on passe", bus.getId(), nomStation);
                continue;
            }

            arriverAStation(station);
            traiterDebarquements(station);
            traiterEmbarquements(station);

            synchronized (station) { // Dès qu'un bus part de la station, tous les bus sont notifiés
                station.departBus(bus);
                station.notifyAll();
            }
            logger.debug("Bus {} quitte la station {}", bus.getId(), nomStation);
        }

        logger.info("Bus {} a termine son trajet. Passagers a bord : {}/{}",
                bus.getId(), bus.getNombrePassagers(), bus.getCapaciteMax());
    }

    /**
     * Arrivee du bus a une station.
     * Le bus doit attendre si la station est pleine (pas de place pour un bus supplementaire).
     */
    private void arriverAStation(Station station) {
        synchronized (station) { // On sychnronise tous les threads bus à l'objet station
            while(station.estPleine()){
                try {                   // Essaye de faire attendre les autres threads bus,
                                        // jusqu'à ce qu'une place se libère
                    station.wait();
                } catch (InterruptedException e) { // Si le try échoue, affiche une erreur
                    BusThread.currentThread().interrupt();
                    return;
                } 
            } 

            station.arriverBus(bus); // Si la station est pas pleine, un bus arrive à la station
        }

        logger.debug("Bus {} arrive a la station {}", bus.getId(), station.getNom());
    }

    /**
     * Traite les evenements de debarquement pour la station courante.
     * Apres chaque debarquement, les threads en attente d'une place doivent etre reveilles.
     */
    private void traiterDebarquements(Station station) {
        for (Evenement evt : evenementsAssignes) {
            if (evt.getType() == TypeEvenement.DEBARQUEMENT
                    && evt.getStationNom().equals(station.getNom())) {

                evt.setBus(bus);
                Passager passager = passagers.get(evt.getPassagerId());
                evt.setPassager(passager);

                // Si un débarquement a lieu, on notifie les threads passagers d'une place libérée
                boolean debarque = bus.debarquer(passager);
                if (debarque) {
                    synchronized (bus) {
                        bus.notifyAll();
                    }

                    logger.info("DEBARQUEMENT: {} descend de {} a {}",
                            evt.getPassagerId(), bus.getId(), station.getNom());
                }

                fileEvenements.produire(evt);
            }
        }
    }

    /**
     * Traite les evenements d'embarquement pour la station courante.
     * Si le bus est plein, le thread doit attendre qu'une place se libere.
     */
    private void traiterEmbarquements(Station station) {
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

                // Faire attendre les threads passagers qui veulent une place dans le bus
                synchronized (bus) {
                    while(bus.estPlein()){ // Tant que le bus est plein, les threads passagers attendent
                        try {
                            bus.wait();
                        } catch (InterruptedException e) { // Si le try échoue
                            PassagerThread.currentThread().interrupt();
                            return;
                        }
                    }

                    boolean embarque = bus.embarquer(passager);
                    if (embarque) {
                        logger.info("EMBARQUEMENT: {} monte dans {} a {} (priorite: {})",
                                evt.getPassagerId(), bus.getId(), station.getNom(), evt.getPrioritePassager());

                        // Notifier les autres threads bus en attente de la situation de la station.
                        synchronized (station) {
                            station.retirerPassagerEnAttente();
                            station.notifyAll();
                        }

                        // Notifier les threads passagers qui attendent une nouvelle place dans le bus.
                        bus.notifyAll();
                    }
                }

                Evenement evtPaiement = new Evenement(
                        evt.getTimestamp(), TypeEvenement.PAIEMENT,
                        evt.getBusId(), evt.getPassagerId(),
                        evt.getStationNom(), evt.getPrioritePassager());
                evtPaiement.setBus(bus);
                evtPaiement.setPassager(passager);
                fileEvenements.produire(evtPaiement);
                
                fileEvenements.produire(evt);
            }
        }
    }

    public Bus getBus() {
        return bus;
    }
}
