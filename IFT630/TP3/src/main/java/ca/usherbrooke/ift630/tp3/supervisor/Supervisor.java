package ca.usherbrooke.ift630.tp3.supervisor;

import ca.usherbrooke.ift630.tp3.compute.MonteCarloSimulator;
import ca.usherbrooke.ift630.tp3.compute.Statistics;

/**
 * Interface pour les superviseurs (threads ou processus).
 * Le superviseur gère les workers et agrège les résultats.
 */
public interface Supervisor {
    
    /**
     * Démarre le superviseur et ses workers
     */
    void start() throws Exception;
    
    /**
     * Arrête proprement le superviseur et ses workers
     */
    void shutdown();
    
    /**
     * Attend que le traitement soit terminé (pour durée fixe)
     * @param durationSeconds Durée d'exécution en secondes
     */
    void awaitCompletion(int durationSeconds) throws InterruptedException;
    
    /**
     * Récupère les statistiques agrégées de tous les workers
     */
    Statistics getAggregatedStatistics();
    
    /**
     * Exécute la simulation Monte Carlo sur tous les événements traités
     */
    MonteCarloSimulator.MonteCarloResult runMonteCarloSimulation(int iterations);
    
    /**
     * Affiche les rapports de performance et statistiques
     */
    void printReports();
}
