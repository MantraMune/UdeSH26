package ca.usherbrooke.ift630.tp2.threads;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.usherbrooke.ift630.tp2.coordination.GestionnaireStations;
import ca.usherbrooke.ift630.tp2.modele.Bus;
import ca.usherbrooke.ift630.tp2.modele.Ligne;
import ca.usherbrooke.ift630.tp2.modele.Passager;
import ca.usherbrooke.ift630.tp2.modele.Station;

/**
 * Thread representant un passager actif du reseau STS.
 * Chaque passager :
 * 1. Attend a sa station de depart
 * 2. Verifie periodiquement si un bus de sa ligne est present avec de la place
 * 3. Si aucun bus disponible : se met en attente
 * 4. Si bus present : verifie la fairness puis tente l'embarquement
 * 5. Apres embarquement : notifie les autres passagers
 */
public class PassagerThread extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(PassagerThread.class);

    private static final long TIMEOUT_ATTENTE_MS = 500;
    private static final int MAX_TENTATIVES = 100;

    private final Passager passager;
    private final GestionnaireStations gestionnaireStations;
    private final String busIdAttendu;
    private final Ligne ligneAttendee;

    private volatile boolean embarque = false;
    private volatile boolean debarque = false;

    public PassagerThread(Passager passager, GestionnaireStations gestionnaireStations,
                          String busIdAttendu, Ligne ligneAttendee) {
        super("Passager-" + passager.getId());
        this.passager = passager;
        this.gestionnaireStations = gestionnaireStations;
        this.busIdAttendu = busIdAttendu;
        this.ligneAttendee = ligneAttendee;
    }

    @Override
    public void run() {
        logger.debug("Passager {} ({}) arrive a la station {} et attend un bus vers {}",
                passager.getId(), passager.getPriorite(),
                passager.getStationDepart(), passager.getStationDestination());

        Station stationDepart = gestionnaireStations.getStation(passager.getStationDepart());
        if (stationDepart == null) {
            logger.warn("Passager {} : station de depart '{}' non trouvee",
                    passager.getId(), passager.getStationDepart());
            return;
        }

        stationDepart.ajouterPassagerEnAttente();

        int tentatives = 0;
        while (!embarque && !Thread.interrupted() && tentatives < MAX_TENTATIVES) {
            tentatives++;

            Bus busCompatible = trouverBusCompatible(stationDepart);

            if (busCompatible != null) {
                // Vérification de fairness pour embarquement
                boolean autorise = gestionnaireStations.peutEmbarquer(passager); 

                if (autorise) {
                    boolean succes = busCompatible.embarquer(passager);
                    synchronized (busCompatible) {
                        busCompatible.notifyAll();
                    }
                    if (succes) {
                        embarque = true;
                        // Enregistrer l'embarquement dans le gestionnaire
                        // pour mettre a jour les compteurs de fairness.
                        gestionnaireStations.enregistrerEmbarquement(passager);
                        // Notifier les autres passagers en attente à cette station.
                        synchronized (stationDepart) {
                            stationDepart.retirerPassagerEnAttente();
                            stationDepart.notifyAll();
                        }

                        logger.info("Passager {} ({}) embarque dans {} a {} (tentative {})",
                                passager.getId(), passager.getPriorite(),
                                busCompatible.getId(), stationDepart.getNom(), tentatives);

                        break;
                    }
                    logger.debug("Passager {} : bus {} plein, nouvelle tentative",
                            passager.getId(), busCompatible.getId());
                } else {
                    synchronized (busCompatible){
                        while(!gestionnaireStations.peutEmbarquer(passager)) {
                            try {
                                busCompatible.wait(); // Les threads passagers attendent qu'une place se libère
                            } catch (InterruptedException e) {
                                PassagerThread.currentThread().interrupt();
                                return;
                            }
                        }
                    }

                    logger.debug("Passager {} ({}) attend (fairness) a {}",
                            passager.getId(), passager.getPriorite(), stationDepart.getNom());
                }
            }

            // Thread en attente à la station (timeout)
            if (!embarque) {
                synchronized (stationDepart) {
                    try {
                        stationDepart.wait(TIMEOUT_ATTENTE_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }

        if (!embarque) {
            stationDepart.retirerPassagerEnAttente();
            logger.warn("Passager {} n'a pas pu embarquer apres {} tentatives",
                    passager.getId(), tentatives);
        }
    }

    /**
     * Cherche un bus compatible a la station (meme ligne, avec de la place).
     */
    private Bus trouverBusCompatible(Station station) {
        List<Bus> busPresents = station.getBusPresents();

        for (Bus bus : busPresents) {
            if (busIdAttendu != null && bus.getId().equals(busIdAttendu)) {
                if (!bus.estPlein()) {
                    return bus;
                }
            } else if (ligneAttendee != null && bus.getLigne().getNumero() == ligneAttendee.getNumero()) {
                if (!bus.estPlein()) {
                    return bus;
                }
            }
        }
        return null;
    }

    public Passager getPassager() { return passager; }
    public boolean isEmbarque() { return embarque; }
    public boolean isDebarque() { return debarque; }
}
