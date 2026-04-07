package ca.usherbrooke.ift630.tp3.supervisor;

import ca.usherbrooke.ift630.tp3.compute.MonteCarloSimulator;
import ca.usherbrooke.ift630.tp3.compute.Statistics;
import ca.usherbrooke.ift630.tp3.mqtt.MQTTClient;
import ca.usherbrooke.ift630.tp3.mqtt.TransportEvent;
import ca.usherbrooke.ift630.tp3.utils.PerformanceMonitor;
import ca.usherbrooke.ift630.tp3.worker.ThreadWorker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Superviseur en mode threads.
 * Crée N threads workers qui lisent depuis une BlockingQueue partagée.
 */
public class ThreadSupervisor implements Supervisor {
    private final int numWorkers;
    private final String configFile;
    
    private MQTTClient mqttClient;
    private final BlockingQueue<TransportEvent> eventQueue;
    private final List<ThreadWorker> workers;
    private final PerformanceMonitor perfMonitor;

    public ThreadSupervisor(int numWorkers, String configFile) {
        this.numWorkers = numWorkers;
        this.configFile = configFile;
        this.eventQueue = new LinkedBlockingQueue<>();
        this.workers = new ArrayList<>();
        this.perfMonitor = new PerformanceMonitor("threads", numWorkers);
    }

    @Override
    public void start() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println(" DÉMARRAGE MODE THREADS");
        System.out.println("=".repeat(80));
        System.out.println("   Workers: " + numWorkers);
        System.out.println("   Queue:   BlockingQueue (mémoire partagée)");
        System.out.println("=".repeat(80));
        
        perfMonitor.start();
        
        // Phase 1: Créer les workers
        perfMonitor.startPhase("creation_workers");
        System.out.println("\n Création des workers...");
        for (int i = 0; i < numWorkers; i++) {
            ThreadWorker worker = new ThreadWorker(i, eventQueue);
            workers.add(worker);
            worker.start();
        }
        perfMonitor.stopPhase("creation_workers");
        System.out.println(" " + numWorkers + " workers créés et démarrés");
        
        // Phase 2: Connexion MQTT
        perfMonitor.startPhase("mqtt_connection");
        System.out.println("\n Connexion au broker MQTT...");
        mqttClient = new MQTTClient(configFile, eventQueue);
        mqttClient.connect();
        perfMonitor.stopPhase("mqtt_connection");
        
        System.out.println("\n Système threads prêt - réception en cours...\n");
    }

    @Override
    public void awaitCompletion(int durationSeconds) throws InterruptedException {
        System.out.println(" Traitement pendant " + durationSeconds + " secondes...\n");
        
        perfMonitor.startPhase("processing");
        
        // Attendre la durée spécifiée
        Thread.sleep(durationSeconds * 1000L);
        
        perfMonitor.stopPhase("processing");
    }

    @Override
    public void shutdown() {
        System.out.println("\n Arrêt du système threads...");
        
        perfMonitor.startPhase("shutdown");
        
        // Arrêter la réception MQTT
        if (mqttClient != null) {
            mqttClient.disconnect();
        }

        // Arrêter tous les workers
        System.out.println("   Arrêt des workers...");
        for (ThreadWorker worker : workers) {
            worker.shutdown();
        }
        
        // Attendre que tous les workers terminent
        for (ThreadWorker worker : workers) {
            try {
                worker.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        perfMonitor.stopPhase("shutdown");
        
        // Calculer les statistiques finales
        long totalEvents = workers.stream()
            .mapToLong(w -> w.getStatistics().getTotalEvents())
            .sum();
        
        perfMonitor.stop(totalEvents);
        
        System.out.println(" Arrêt terminé");
    }

    @Override
    public Statistics getAggregatedStatistics() {
        Statistics aggregated = new Statistics(-1); // -1 = agrégé
        
        for (ThreadWorker worker : workers) {
            aggregated.merge(worker.getStatistics());
        }
        
        aggregated.markEnd();
        return aggregated;
    }

    @Override
    public MonteCarloSimulator.MonteCarloResult runMonteCarloSimulation(int iterations) {
        perfMonitor.startPhase("monte_carlo");
        
        // Collecter tous les événements traités
        List<TransportEvent> allEvents = new ArrayList<>();
        for (ThreadWorker worker : workers) {
            allEvents.addAll(worker.getProcessedEvents());
        }
        
        System.out.println("\n Préparation simulation Monte Carlo...");
        System.out.println("   Événements collectés: " + allEvents.size());
        
        MonteCarloSimulator simulator = new MonteCarloSimulator(iterations);
        MonteCarloSimulator.MonteCarloResult result = simulator.simulate(allEvents);
        
        perfMonitor.stopPhase("monte_carlo");
        
        return result;
    }

    @Override
    public void printReports() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println(" RAPPORTS FINAUX - MODE THREADS");
        System.out.println("=".repeat(80));
        
        // Rapport MQTT
        if (mqttClient != null) {
            mqttClient.printStats();
        }
        
        // Statistiques agrégées
        Statistics aggregated = getAggregatedStatistics();
        aggregated.printReport();
        
        // Performance
        perfMonitor.printReport();
        
        System.out.println("\n Statistiques par worker:");
        for (ThreadWorker worker : workers) {
            Statistics stats = worker.getStatistics();
            System.out.println("   Worker #" + worker.getWorkerId() + ": " + 
                stats.getTotalEvents() + " événements, " +
                String.format("%.2f $", stats.getTotalRevenue()));
        }
    }

    public PerformanceMonitor getPerformanceMonitor() {
        return perfMonitor;
    }

    public List<ThreadWorker> getWorkers() {
        return workers;
    }
}
