package ca.usherbrooke.ift630.tp2.coordination;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.usherbrooke.ift630.tp2.modele.Passager;
import ca.usherbrooke.ift630.tp2.modele.Priorite;
import ca.usherbrooke.ift630.tp2.modele.Station;

/**
 * Gestionnaire centralise des stations du reseau STS.
 * Responsable du mecanisme de fairness :
 * apres SEUIL_FAIRNESS embarquements HAUTE priorite consecutifs,
 * le systeme DOIT embarquer au moins 1 passager NORMALE priorite.
 */
public class GestionnaireStations {

    private static final Logger logger = LoggerFactory.getLogger(GestionnaireStations.class);
    private static final int SEUIL_FAIRNESS = 3;

    private final Map<String, Station> stations;

    

    private final AtomicInteger totalEmbarquements;
    private final AtomicInteger totalEmbarquementsHaute; 
    private final AtomicInteger totalEmbarquementsNormale;
    private final AtomicInteger maxPrioritairesConsecutifsObserve;
    private final AtomicInteger consecutifsHaute;

    public GestionnaireStations(Map<String, Station> stations) {
        this.stations = stations;
        this.totalEmbarquements = new AtomicInteger(0);
        this.totalEmbarquementsHaute = new AtomicInteger(0);
        this.totalEmbarquementsNormale = new AtomicInteger(0);
        this.maxPrioritairesConsecutifsObserve = new AtomicInteger(0);
        this.consecutifsHaute = new AtomicInteger(0);
    }

    /**
     * Determine si un passager peut embarquer selon la fairness.
     * Doit etre thread-safe (appelee par plusieurs PassagerThread).
     *
     * @param passager le passager qui souhaite embarquer
     * @return true si le passager peut embarquer maintenant
     */
    public synchronized boolean peutEmbarquer(Passager passager) {
        // Cas 1 : Haute priorité
        if (consecutifsHaute.get() < SEUIL_FAIRNESS){ // Si le nombre de passagers consécutifs de priorité haute < 3
            return true; // peut embarquer
        }
        // Cas 2 : Normale priorité
        else if (passager.getPriorite() == Priorite.NORMALE){
            // Toujours autorisé pour les passagers de priorité normale
            return true;
        } 

        // Cas 3 : Bloquer les hautes priorité si consecutifsHaute >= SEUIL_FAIRNESS
        return false;
    }

    /**
     * Enregistre un embarquement reussi et met a jour les compteurs de fairness.
     */
    public synchronized void enregistrerEmbarquement(Passager passager) {
        totalEmbarquements.incrementAndGet();

        if (passager.getPriorite() == Priorite.HAUTE) {
            totalEmbarquementsHaute.incrementAndGet();
            int consecutifs = consecutifsHaute.incrementAndGet(); // Incrémente le compteur consécutifsHaute.
            maxPrioritairesConsecutifsObserve.updateAndGet(x -> Math.max(x, consecutifs)); // Mettre à jour le max observé (compare entre la valeur actuelle de max obseervé et les consécutifs HAUTE)
        } else {
            totalEmbarquementsNormale.incrementAndGet();
            consecutifsHaute.set(0); // Réinitialise le compteur consecutifsHaute si un embarquement de priorité normale a lieu
        }
    }

    public Station getStation(String nom) { return stations.get(nom); }
    public Map<String, Station> getStations() { return stations; }
    public int getTotalEmbarquements() { return totalEmbarquements.get(); }
    public int getTotalEmbarquementsHaute() { return totalEmbarquementsHaute.get(); }
    public int getTotalEmbarquementsNormale() { return totalEmbarquementsNormale.get(); }
    public int getMaxPrioritairesConsecutifs() { return maxPrioritairesConsecutifsObserve.get(); }

    public boolean isFairnessRespectee() {
        return maxPrioritairesConsecutifsObserve.get() <= SEUIL_FAIRNESS + 1;
    }

    public void afficherRapport() {
        logger.info("--- Rapport de fairness ---");
        logger.info("Total embarquements : {}", totalEmbarquements.get());
        logger.info("Embarquements HAUTE : {}", totalEmbarquementsHaute.get());
        logger.info("Embarquements NORMALE : {}", totalEmbarquementsNormale.get());
        logger.info("Max prioritaires consecutifs observe : {}", maxPrioritairesConsecutifsObserve.get());
        logger.info("Seuil de fairness : {}", SEUIL_FAIRNESS);
        logger.info("Fairness respectee : {}", isFairnessRespectee() ? "OUI" : "NON - FAMINE DETECTEE");
    }
}
