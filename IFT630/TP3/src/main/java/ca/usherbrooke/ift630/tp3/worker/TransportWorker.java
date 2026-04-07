package ca.usherbrooke.ift630.tp3.worker;

import ca.usherbrooke.ift630.tp3.compute.Statistics;
import ca.usherbrooke.ift630.tp3.mqtt.TransportEvent;

import java.util.List;

/**
 * Interface commune pour les workers (threads ou processus).
 * Définit le contrat de traitement des événements de transport.
 */
public interface TransportWorker {
    
    /**
     * Démarre le worker
     */
    void start();
    
    /**
     * Arrête proprement le worker
     */
    void shutdown();
    
    /**
     * Récupère les statistiques calculées par ce worker
     */
    Statistics getStatistics();
    
    /**
     * Vérifie si le worker est encore actif
     */
    boolean isRunning();
    
    /**
     * Traite un événement de transport (méthode commune)
     * Cette méthode est partagée entre ThreadWorker et ProcessWorker
     */
    default void processEvent(TransportEvent event, Statistics stats) {
        if (event != null) {
            stats.processEvent(event);
        }
    }
    
    /**
     * Récupère tous les événements traités (pour Monte Carlo)
     */
    List<TransportEvent> getProcessedEvents();
}
