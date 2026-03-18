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
 * Consommateur specialise pour les evenements de DEBARQUEMENT.
 * Thread daemon qui retire les evenements DEBARQUEMENT de la file,
 * debarque le passager du bus, et notifie les threads en attente
 * qu'une place s'est liberee.
 */
public class ConsommateurDebarquement extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(ConsommateurDebarquement.class);

    private final FileAttentePrioritaire fileEvenements;
    private final Map<String, Bus> busMap;
    private final Map<String, Passager> passagerMap;
    private final Dashboard dashboard;
    private final AtomicInteger compteurTraites = new AtomicInteger(0);

    public ConsommateurDebarquement(FileAttentePrioritaire fileEvenements,
                                     Map<String, Bus> busMap,
                                     Map<String, Passager> passagerMap,
                                     Dashboard dashboard) {
        super("Conso-Debarquement");
        this.fileEvenements = fileEvenements;
        this.busMap = busMap;
        this.passagerMap = passagerMap;
        this.dashboard = dashboard;
    }

    @Override
    public void run() {
        logger.info("Consommateur DEBARQUEMENT demarre");

        while(!Thread.interrupted()){
            try {
                // Consommer les évènements de débarquement dans la file débarquement
                Evenement evt = fileEvenements.consommerParType(TypeEvenement.DEBARQUEMENT); 
                
                if (evt == null) // Si aucun évènement
                {
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
                else if (passager == null)
                {
                    logger.warn("Passager non trouvé pour l'évènement {}", evt);
                    continue;
                }

                // Débarquement thread-safe
                synchronized (bus) {
                    boolean debarque = bus.debarquer(passager);

                    if(debarque){
                        compteurTraites.incrementAndGet();
                        logger.info("Passager {} a été débarqué de {} à {}", passager.getId(), bus.getId(), evt.getStationNom());
                        // Notifier les threads passagers attendant une place
                        bus.notifyAll();
                    } else {
                        logger.warn("Débarquement échoué pour passager {} dans bus {}", passager.getId(), bus.getId());
                    }
                }

                if (dashboard != null){
                    dashboard.enregistrerDebarquement(evt);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) { 
                logger.error("Erreur dans le consommateur de débarquement", e);
            }

        }

        logger.info("Consommateur DEBARQUEMENT arrete ({} evenements traites)", compteurTraites.get());
    }

    public int getCompteurTraites() {
        return compteurTraites.get();
    }
}
