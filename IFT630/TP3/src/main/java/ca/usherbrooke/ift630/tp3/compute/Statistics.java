package ca.usherbrooke.ift630.tp3.compute;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;

import ca.usherbrooke.ift630.tp3.mqtt.TransportEvent;

/**
 * Calcule les statistiques agrégées du système de transport.
 * Thread-safe pour être utilisé en mode threads ou processus.
 */
public class Statistics {
    private final int workerId;
    
    // Compteurs globaux (thread-safe)
    private final AtomicLong totalEvents = new AtomicLong(0);
    private final DoubleAdder totalRevenue = new DoubleAdder();
    private final AtomicInteger totalPassengers = new AtomicInteger(0);
    
    // Statistiques par ligne
    private final Map<String, DoubleAdder> revenuePerLine = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> passengersPerLine = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> eventsPerLine = new ConcurrentHashMap<>();
    
    // Statistiques par arrêt
    private final Map<String, AtomicInteger> passengersPerStop = new ConcurrentHashMap<>();
    
    // Occupation des bus
    private final Map<String, AtomicInteger> busOccupancy = new ConcurrentHashMap<>();
    
    // Méthodes de paiement
    private final Map<String, AtomicInteger> paymentMethods = new ConcurrentHashMap<>();
    
    // Distance estimée
    private final DoubleAdder totalKilometers = new DoubleAdder();
    private static final double AVG_DISTANCE_PER_TRIP = 3.5; // km
    
    private long startTime;
    private long endTime;

    public Statistics(int workerId) {
        this.workerId = workerId;
        this.startTime = System.currentTimeMillis();
    }

    /**
     * Traite un événement de transport
     */
    public void processEvent(TransportEvent event) {

        // Extraire les données de l'évènement
       String numero_ligne = event.getLigne();
       String arret = event.getArret();
       String bus_id = event.getBusId();
       String type = event.getType();   // "embarquement", "debarquement", ou "trajet"
       double montant = event.getMontant(); // montant payé (0 si débarquement/trajet
       String methode = event.getMethode(); // "carte", "cash", "mobile" (vide si pas paiement)

       // Agrégations

       // 1. Compteurs globaux
       totalEvents.incrementAndGet();
       if (montant > 0) {
            totalRevenue.add(montant);
       }

       // 2. Passagers
       if (type.equals("embarquement")) {
            totalPassengers.incrementAndGet();
            passengersPerLine.computeIfAbsent(numero_ligne, k -> new AtomicInteger()).incrementAndGet();
            passengersPerStop.computeIfAbsent(arret, k -> new AtomicInteger()).incrementAndGet();
            busOccupancy.computeIfAbsent(bus_id, k -> new AtomicInteger()).incrementAndGet();
       }
       if (type.equals("debarquement")) {
            busOccupancy.computeIfAbsent(bus_id, k -> new AtomicInteger()).updateAndGet(v -> Math.max(0, v - 1));
       }

       // 3. Revenus par ligne
       if (montant > 0) {
            revenuePerLine.computeIfAbsent(numero_ligne, k -> new DoubleAdder()).add(montant);
       }

       // 4. Événements par ligne
       eventsPerLine.computeIfAbsent(numero_ligne, k -> new AtomicLong()).incrementAndGet();


       // 5. Méthodes de paiement
       if (methode != null && !methode.equals("vide")) {
            paymentMethods.computeIfAbsent(methode, k -> new AtomicInteger()).incrementAndGet();
       }

       // 6. Distance en kilomètres
       if (type.equals("trajet")) {
            totalKilometers.add(AVG_DISTANCE_PER_TRIP);
       }

        // Tests à passer: StatisticsTest (10 points)
        // - testEmbarquementCounting: comptage embarquements
        // - testMultipleEvents: agrégation correcte
        // - testThreadSafety: accès concurrent sécurisé
    }

    public void markEnd() {
        this.endTime = System.currentTimeMillis();
    }

    /**
     * Fusionne les statistiques d'un autre worker
     */
    public void merge(Statistics other) {
        totalEvents.addAndGet(other.totalEvents.get());
        totalRevenue.add(other.totalRevenue.sum());
        totalPassengers.addAndGet(other.totalPassengers.get());
        totalKilometers.add(other.totalKilometers.sum());
        
        other.revenuePerLine.forEach((ligne, revenue) -> 
            revenuePerLine.computeIfAbsent(ligne, k -> new DoubleAdder()).add(revenue.sum())
        );
        
        other.passengersPerLine.forEach((ligne, count) ->
            passengersPerLine.computeIfAbsent(ligne, k -> new AtomicInteger()).addAndGet(count.get())
        );
        
        other.eventsPerLine.forEach((ligne, count) ->
            eventsPerLine.computeIfAbsent(ligne, k -> new AtomicLong()).addAndGet(count.get())
        );
        
        other.passengersPerStop.forEach((arret, count) ->
            passengersPerStop.computeIfAbsent(arret, k -> new AtomicInteger()).addAndGet(count.get())
        );
        
        other.paymentMethods.forEach((methode, count) ->
            paymentMethods.computeIfAbsent(methode, k -> new AtomicInteger()).addAndGet(count.get())
        );
    }

    public void printReport() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println(" RAPPORT STATISTIQUES - Worker #" + workerId);
        System.out.println("=".repeat(80));
        
        System.out.println("\n STATISTIQUES GLOBALES:");
        System.out.println("   Total événements:     " + totalEvents.get());
        System.out.println("   Revenus totaux:       " + String.format("%.2f $", totalRevenue.sum()));
        System.out.println("   Passagers totaux:     " + totalPassengers.get());
        System.out.println("   Kilomètres estimés:   " + String.format("%.1f km", totalKilometers.sum()));
        
        if (totalPassengers.get() > 0 && totalKilometers.sum() > 0) {
            double costPerPassengerKm = totalRevenue.sum() / (totalPassengers.get() * totalKilometers.sum());
            System.out.println("   Coût/passager-km:     " + String.format("%.4f $", costPerPassengerKm));
        }
        
        System.out.println("\n REVENUS PAR LIGNE:");
        revenuePerLine.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> 
                System.out.println("   Ligne " + entry.getKey() + ": " + 
                    String.format("%.2f $", entry.getValue().sum()))
            );
        
        System.out.println("\n PASSAGERS PAR LIGNE:");
        passengersPerLine.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry ->
                System.out.println("   Ligne " + entry.getKey() + ": " + entry.getValue().get())
            );

        System.out.println("\n TOP 5 ARRÊTS LES PLUS FRÉQUENTÉS:");
        passengersPerStop.entrySet().stream()
            .sorted(Map.Entry.<String, AtomicInteger>comparingByValue(
                Comparator.comparingInt(AtomicInteger::get)).reversed())
            .limit(5)
            .forEach(entry ->
                System.out.println("   " + entry.getKey() + ": " + entry.getValue().get())
            );
        
        System.out.println("\n MÉTHODES DE PAIEMENT:");
        paymentMethods.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry ->
                System.out.println("   " + entry.getKey() + ": " + entry.getValue().get())
            );
        
        if (endTime > startTime) {
            double durationSeconds = (endTime - startTime) / 1000.0;
            System.out.println("\n PERFORMANCE:");
            System.out.println("   Durée totale:         " + String.format("%.2f s", durationSeconds));
            System.out.println("   Débit moyen:          " + 
                String.format("%.1f événements/s", totalEvents.get() / durationSeconds));
        }
        
        System.out.println("=".repeat(80));
    }

    // Getters
    public int getWorkerId() { return workerId; }
    public long getTotalEvents() { return totalEvents.get(); }
    public double getTotalRevenue() { return totalRevenue.sum(); }
    public int getTotalPassengers() { return totalPassengers.get(); }
    public double getTotalKilometers() { return totalKilometers.sum(); }
    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }
    public double getDurationSeconds() { return (endTime - startTime) / 1000.0; }
}