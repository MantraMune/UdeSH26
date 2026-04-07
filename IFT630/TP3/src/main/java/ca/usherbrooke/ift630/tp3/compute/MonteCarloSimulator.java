package ca.usherbrooke.ift630.tp3.compute;

import ca.usherbrooke.ift630.tp3.mqtt.TransportEvent;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Simulateur Monte Carlo pour évaluer différents scénarios d'horaires.
 * Objectif pédagogique: calcul INTENSIF pour montrer le gain du parallélisme.
 * 
 * Simule N scénarios d'horaires et calcule:
 * - Temps d'attente moyen
 * - Taux de satisfaction
 * - Utilisation optimale des bus
 */
public class MonteCarloSimulator {
    private static final int DEFAULT_ITERATIONS = 10000;
    private static final int BUS_CAPACITY = 50;
    private static final double TARGET_WAIT_TIME = 15.0; // minutes
    
    private final int iterations;
    private final Random random;
    private List<SimulationResult> results;

    public MonteCarloSimulator() {
        this(DEFAULT_ITERATIONS);
    }

    public MonteCarloSimulator(int iterations) {
        this.iterations = iterations;
        this.random = ThreadLocalRandom.current();
        this.results = new ArrayList<>();
    }

    /**
     * Exécute la simulation Monte Carlo
     * @param events Événements observés pour calibrer la simulation
     * @return Résultat agrégé de toutes les simulations
     */
    public MonteCarloResult simulate(List<TransportEvent> events) {
        System.out.println("\n Lancement simulation Monte Carlo (" + iterations + " itérations)...");
        long startTime = System.nanoTime();
        
        // Analyser les événements pour extraire des paramètres
        SimulationParameters params = extractParameters(events);
        
        // Exécuter N simulations
        results.clear();
        for (int i = 0; i < iterations; i++) {
            results.add(simulateScenario(params));
            
            // Afficher progression tous les 1000 itérations
            if ((i + 1) % 1000 == 0) {
                System.out.println("   Progression: " + (i + 1) + "/" + iterations);
            }
        }
        
        long endTime = System.nanoTime();
        double durationSeconds = (endTime - startTime) / 1_000_000_000.0;
        
        // Agréger les résultats
        MonteCarloResult finalResult = aggregateResults(results);
        finalResult.setComputationTime(durationSeconds);
        
        System.out.println("   Simulation terminée en " + String.format("%.2f s", durationSeconds));
        
        return finalResult;
    }

    /**
     * Extrait les paramètres de simulation depuis les événements observés
     */
    private SimulationParameters extractParameters(List<TransportEvent> events) {
        Map<String, Integer> passengersPerLine = new HashMap<>();
        Map<String, Set<String>> stopsPerLine = new HashMap<>();
        
        for (TransportEvent event : events) {
            String ligne = event.getLigne();
            
            if ("embarquement".equals(event.getType())) {
                passengersPerLine.merge(ligne, 1, Integer::sum);
            }
            
            stopsPerLine.computeIfAbsent(ligne, k -> new HashSet<>()).add(event.getArret());
        }
        
        return new SimulationParameters(passengersPerLine, stopsPerLine);
    }

    /**
     * Simule un scénario d'horaires aléatoire
     */
    private SimulationResult simulateScenario(SimulationParameters params) {
        double totalWaitTime = 0;
        int satisfiedPassengers = 0;
        int totalPassengers = 0;
        double busUtilization = 0;
        
        for (Map.Entry<String, Integer> entry : params.passengersPerLine.entrySet()) {
            String ligne = entry.getKey();
            int passengers = entry.getValue();
            int stops = params.stopsPerLine.getOrDefault(ligne, Collections.emptySet()).size();
            
            // Fréquence aléatoire de passage (5-20 minutes)
            double frequency = 5 + random.nextDouble() * 15;
            
            // Simuler l'arrivée des passagers
            for (int p = 0; p < passengers; p++) {
                // Temps d'attente = distribution exponentielle basée sur la fréquence
                double waitTime = -Math.log(1 - random.nextDouble()) * frequency;
                totalWaitTime += waitTime;
                
                // Passager satisfait si temps d'attente < cible
                if (waitTime <= TARGET_WAIT_TIME) {
                    satisfiedPassengers++;
                }
                
                totalPassengers++;
            }
            
            // Utilisation du bus (simple heuristique)
            int trips = (int) Math.ceil(passengers / (double) BUS_CAPACITY);
            busUtilization += (passengers / (double) (trips * BUS_CAPACITY));
        }
        
        double avgWaitTime = totalPassengers > 0 ? totalWaitTime / totalPassengers : 0;
        double satisfactionRate = totalPassengers > 0 ? 
            (satisfiedPassengers / (double) totalPassengers) * 100 : 0;
        busUtilization = params.passengersPerLine.size() > 0 ? 
            (busUtilization / params.passengersPerLine.size()) * 100 : 0;
        
        return new SimulationResult(avgWaitTime, satisfactionRate, busUtilization);
    }

    /**
     * Agrège les résultats de toutes les simulations
     */
    private MonteCarloResult aggregateResults(List<SimulationResult> results) {
        DoubleSummaryStatistics waitTimeStats = results.stream()
            .mapToDouble(SimulationResult::avgWaitTime)
            .summaryStatistics();
        
        DoubleSummaryStatistics satisfactionStats = results.stream()
            .mapToDouble(SimulationResult::satisfactionRate)
            .summaryStatistics();
        
        DoubleSummaryStatistics utilizationStats = results.stream()
            .mapToDouble(SimulationResult::busUtilization)
            .summaryStatistics();
        
        return new MonteCarloResult(
            waitTimeStats.getAverage(),
            calculateStdDev(results, waitTimeStats.getAverage(), SimulationResult::avgWaitTime),
            satisfactionStats.getAverage(),
            utilizationStats.getAverage(),
            iterations
        );
    }

    /**
     * Calcule l'écart-type
     */
    private double calculateStdDev(List<SimulationResult> results, double mean, 
                                   java.util.function.ToDoubleFunction<SimulationResult> extractor) {
        double variance = results.stream()
            .mapToDouble(extractor)
            .map(value -> Math.pow(value - mean, 2))
            .average()
            .orElse(0);
        
        return Math.sqrt(variance);
    }

    /**
     * Paramètres de simulation
     */
    private static class SimulationParameters {
        final Map<String, Integer> passengersPerLine;
        final Map<String, Set<String>> stopsPerLine;

        SimulationParameters(Map<String, Integer> passengersPerLine, 
                           Map<String, Set<String>> stopsPerLine) {
            this.passengersPerLine = passengersPerLine;
            this.stopsPerLine = stopsPerLine;
        }
    }

    /**
     * Résultat d'une simulation
     */
    private record SimulationResult(double avgWaitTime, double satisfactionRate, 
                                    double busUtilization) {}

    /**
     * Résultat agrégé Monte Carlo
     */
    public static class MonteCarloResult {
        private final double avgWaitTime;
        private final double stdDevWaitTime;
        private final double satisfactionRate;
        private final double busUtilization;
        private final int iterations;
        private double computationTime;

        public MonteCarloResult(double avgWaitTime, double stdDevWaitTime, 
                               double satisfactionRate, double busUtilization, 
                               int iterations) {
            this.avgWaitTime = avgWaitTime;
            this.stdDevWaitTime = stdDevWaitTime;
            this.satisfactionRate = satisfactionRate;
            this.busUtilization = busUtilization;
            this.iterations = iterations;
        }

        public void setComputationTime(double seconds) {
            this.computationTime = seconds;
        }

        public void printReport() {
            System.out.println("\n" + "=".repeat(80));
            System.out.println(" RÉSULTATS SIMULATION MONTE CARLO");
            System.out.println("=".repeat(80));
            System.out.println("   Itérations:              " + iterations);
            System.out.println("   Temps de calcul:         " + String.format("%.2f s", computationTime));
            System.out.println("\n   Temps d'attente moyen:   " + String.format("%.2f min", avgWaitTime));
            System.out.println("   Écart-type:              " + String.format("%.2f min", stdDevWaitTime));
            System.out.println("   Taux de satisfaction:    " + String.format("%.1f %%", satisfactionRate));
            System.out.println("   Utilisation des bus:     " + String.format("%.1f %%", busUtilization));
            System.out.println("=".repeat(80));
        }

        public double getComputationTime() { return computationTime; }
        public double getAvgWaitTime() { return avgWaitTime; }
        public double getSatisfactionRate() { return satisfactionRate; }
    }
}
