package ca.usherbrooke.ift630.tp3.utils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Moniteur de performance pour mesurer et comparer threads vs processus.
 * Calcule: temps total, speedup, efficacité, overhead IPC.
 */
public class PerformanceMonitor {
    private final String mode; // "threads" ou "processes"
    private final int numWorkers;
    
    private long startTime;
    private long endTime;
    private long totalEvents;
    
    // Temps par phase
    private final Map<String, Long> phaseTimes = new ConcurrentHashMap<>();
    private final Map<String, Long> phaseStartTimes = new ConcurrentHashMap<>();
    
    // Résultats précédents pour comparaison
    private static PerformanceResult lastThreadsResult;
    private static PerformanceResult lastProcessesResult;

    public PerformanceMonitor(String mode, int numWorkers) {
        this.mode = mode;
        this.numWorkers = numWorkers;
    }

    /**
     * Démarre le chronomètre global
     */
    public void start() {
        this.startTime = System.nanoTime();
    }

    /**
     * Arrête le chronomètre global
     */
    public void stop(long totalEvents) {
        this.endTime = System.nanoTime();
        this.totalEvents = totalEvents;
    }

    /**
     * Démarre le chronomètre d'une phase
     */
    public void startPhase(String phaseName) {
        phaseStartTimes.put(phaseName, System.nanoTime());
    }

    /**
     * Arrête le chronomètre d'une phase
     */
    public void stopPhase(String phaseName) {
        Long startTime = phaseStartTimes.get(phaseName);
        if (startTime != null) {
            long duration = System.nanoTime() - startTime;
            phaseTimes.put(phaseName, duration);
        }
    }

    /**
     * Calcule le temps d'une phase en secondes
     */
    public double getPhaseTimeSeconds(String phaseName) {
        Long nanos = phaseTimes.get(phaseName);
        return nanos != null ? nanos / 1_000_000_000.0 : 0.0;
    }

    /**
     * Génère le rapport de performance
     */
    public PerformanceResult getResult() {
        double totalSeconds = (endTime - startTime) / 1_000_000_000.0;
        double throughput = totalEvents / totalSeconds;
        
        PerformanceResult result = new PerformanceResult(
            mode,
            numWorkers,
            totalSeconds,
            totalEvents,
            throughput,
            new HashMap<>(phaseTimes)
        );
        
        // Sauvegarder pour comparaison future
        if ("threads".equals(mode)) {
            lastThreadsResult = result;
        } else if ("processes".equals(mode)) {
            lastProcessesResult = result;
        }
        
        return result;
    }

    /**
     * Affiche le rapport de performance
     */
    public void printReport() {
        PerformanceResult result = getResult();
        result.printReport();
        
        // Si on a les deux résultats, afficher la comparaison
        if (lastThreadsResult != null && lastProcessesResult != null) {
            printComparison(lastThreadsResult, lastProcessesResult);
        }
    }

    /**
     * Affiche la comparaison threads vs processus
     */
    private static void printComparison(PerformanceResult threads, PerformanceResult processes) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println(" COMPARAISON THREADS vs PROCESSUS");
        System.out.println("=".repeat(80));
        
        double speedupThreads = 1.0; // Baseline
        double speedupProcesses = threads.totalSeconds / processes.totalSeconds;
        
        double efficiencyThreads = speedupThreads / threads.numWorkers * 100;
        double efficiencyProcesses = speedupProcesses / processes.numWorkers * 100;
        
        double overhead = ((processes.totalSeconds - threads.totalSeconds) / threads.totalSeconds) * 100;
        
        System.out.println("\n  TEMPS D'EXÉCUTION:");
        System.out.println("   Threads (" + threads.numWorkers + " workers):   " + 
            String.format("%.2f s", threads.totalSeconds));
        System.out.println("   Processus (" + processes.numWorkers + " workers): " + 
            String.format("%.2f s", processes.totalSeconds));
        System.out.println("   Overhead IPC:          " + String.format("%+.1f %%", overhead));
        
        System.out.println("\n SPEEDUP:");
        System.out.println("   Threads:               " + String.format("%.2fx", speedupThreads));
        System.out.println("   Processus:             " + String.format("%.2fx", speedupProcesses));
        
        System.out.println("\n EFFICACITÉ:");
        System.out.println("   Threads:               " + String.format("%.1f %%", efficiencyThreads));
        System.out.println("   Processus:             " + String.format("%.1f %%", efficiencyProcesses));
        
        System.out.println("\n DÉBIT:");
        System.out.println("   Threads:               " + 
            String.format("%.0f événements/s", threads.throughput));
        System.out.println("   Processus:             " + 
            String.format("%.0f événements/s", processes.throughput));
        
        // Analyse
        System.out.println("\n ANALYSE:");
        if (overhead > 20) {
            System.out.println("    Overhead IPC significatif (>" + String.format("%.0f%%", overhead) + ")");
            System.out.println("    Les calculs ne sont pas assez intensifs pour compenser l'IPC");
        } else if (overhead > 0) {
            System.out.println("    Overhead IPC modéré (" + String.format("%.1f%%", overhead) + ")");
            System.out.println("    Le parallélisme compense partiellement l'overhead");
        } else {
            System.out.println("    Processus plus rapides que threads!");
            System.out.println("    Gain du parallélisme > overhead IPC");
        }
        
        System.out.println("=".repeat(80));
    }

    /**
     * Résultat de performance
     */
    public static class PerformanceResult {
        final String mode;
        final int numWorkers;
        final double totalSeconds;
        final long totalEvents;
        final double throughput;
        final Map<String, Long> phaseTimes;

        public PerformanceResult(String mode, int numWorkers, double totalSeconds, 
                                long totalEvents, double throughput, 
                                Map<String, Long> phaseTimes) {
            this.mode = mode;
            this.numWorkers = numWorkers;
            this.totalSeconds = totalSeconds;
            this.totalEvents = totalEvents;
            this.throughput = throughput;
            this.phaseTimes = phaseTimes;
        }

        public void printReport() {
            System.out.println("\n" + "=".repeat(80));
            System.out.println(" RAPPORT DE PERFORMANCE - MODE: " + mode.toUpperCase());
            System.out.println("=".repeat(80));
            System.out.println("   Workers:               " + numWorkers);
            System.out.println("   Temps total:           " + String.format("%.2f s", totalSeconds));
            System.out.println("   Événements traités:    " + totalEvents);
            System.out.println("   Débit:                 " + String.format("%.0f événements/s", throughput));
            
            if (!phaseTimes.isEmpty()) {
                System.out.println("\n  TEMPS PAR PHASE:");
                phaseTimes.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        double seconds = entry.getValue() / 1_000_000_000.0;
                        double percentage = (seconds / totalSeconds) * 100;
                        System.out.println("   " + entry.getKey() + ": " + 
                            String.format("%.2f s (%.1f%%)", seconds, percentage));
                    });
            }
            
            System.out.println("=".repeat(80));
        }

        public double getTotalSeconds() { return totalSeconds; }
        public double getThroughput() { return throughput; }
        public String getMode() { return mode; }
        public long getTotalEvents() { return totalEvents; }
    }

    /**
     * Réinitialise les résultats sauvegardés
     */
    public static void reset() {
        lastThreadsResult = null;
        lastProcessesResult = null;
    }
}
