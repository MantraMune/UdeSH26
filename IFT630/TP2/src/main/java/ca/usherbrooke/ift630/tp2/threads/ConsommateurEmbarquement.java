package ca.usherbrooke.ift630.tp2.threads;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.usherbrooke.ift630.tp2.coordination.Dashboard;
import ca.usherbrooke.ift630.tp2.coordination.FileAttentePrioritaire;
import ca.usherbrooke.ift630.tp2.modele.Bus;
import ca.usherbrooke.ift630.tp2.modele.Evenement;
import ca.usherbrooke.ift630.tp2.modele.Passager;
import ca.usherbrooke.ift630.tp2.modele.TypeEvenement;

/**
 * Consommateur specialise pour les evenements d'EMBARQUEMENT.
 * Thread daemon qui consomme les evenements de la file dediee EMBARQUEMENT.
 * L'embarquement physique est deja fait par le BusThread (producteur).
 * Le consommateur enregistre l'evenement dans le dashboard.
 */
public class ConsommateurEmbarquement extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(ConsommateurEmbarquement.class);

    private final FileAttentePrioritaire fileEvenements;
    private final Map<String, Bus> busMap;
    private final Map<String, Passager> passagerMap;
    private final Dashboard dashboard;
    private final AtomicInteger compteurTraites = new AtomicInteger(0);

    public ConsommateurEmbarquement(FileAttentePrioritaire fileEvenements,
                                     Map<String, Bus> busMap,
                                     Map<String, Passager> passagerMap,
                                     Dashboard dashboard) {
        super("Conso-Embarquement");
        this.fileEvenements = fileEvenements;
        this.busMap = busMap;
        this.passagerMap = passagerMap;
        this.dashboard = dashboard;
    }

    @Override
    public void run() {
        logger.info("Consommateur EMBARQUEMENT demarre");

        while(!Thread.interrupted()){
            try {
                // Consommer les évènements d'embarquement dans la file embarquement
                Evenement evt = fileEvenements.consommerParType(TypeEvenement.EMBARQUEMENT);

                if (evt == null){ // Si aucun évènement
                    Thread.sleep(50);
                    continue;
                }

                // Récupérer le bus et le passager de l'évènement
                Bus bus = busMap.get(evt.getBusId());
                Passager passager = passagerMap.get(evt.getPassagerId());

                if (bus == null) {
                    logger.warn("Bus non trouvé pour l'évènement {}", evt);
                    continue;
                }
                else if (passager == null) {
                    logger.warn("Passager non trouvé pour l'évènement {}", evt);
                    continue;
                }

                boolean embarque; // Par défaut

                // Embarquement thread-safe
                synchronized (bus) {
                    while(bus.estPlein()){
                        try {
                            bus.wait(); // Les threads passagers attendent pour une place
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                    
                    embarque = bus.embarquer(passager);

                    if(embarque){
                        compteurTraites.incrementAndGet();
                        logger.info("Passager {} a embarqué dans {} à {}", passager.getId(), bus.getId(), evt.getStationNom());
                        bus.notifyAll(); // Notifier les threads passagers de l'embarquement
                    } else {
                        logger.warn("Embarquement échoué pour passager {} dans bus {}", passager.getId(), bus.getId());
                    }
                }

                if (dashboard != null){
                    dashboard.enregistrerEmbarquement(evt);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Erreur dans le consommateur d'embarquement", e);
            }
        }

        logger.info("Consommateur EMBARQUEMENT arrete ({} evenements traites)", compteurTraites.get());
    }

    public int getCompteurTraites() {
        return compteurTraites.get();
    }
}
