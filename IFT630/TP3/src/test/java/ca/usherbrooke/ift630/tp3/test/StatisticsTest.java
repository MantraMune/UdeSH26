package ca.usherbrooke.ift630.tp3.test;

import ca.usherbrooke.ift630.tp3.compute.Statistics;
import ca.usherbrooke.ift630.tp3.mqtt.TransportEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests Statistics et agregations")
public class StatisticsTest {

    private Statistics stats;

    @BeforeEach
    public void setUp() {
        stats = new Statistics(0);
    }

    @Test
    @DisplayName("Comptage evenements embarquement")
    public void testEmbarquementCounting() {
        TransportEvent event = createEvent("embarquement", "1", "BUS_001", 3.50, "carte");
        
        stats.processEvent(event);
        
        assertEquals(1, stats.getTotalEvents());
        assertEquals(1, stats.getTotalPassengers());
        assertEquals(3.50, stats.getTotalRevenue(), 0.01);
    }

    @Test
    @DisplayName("Comptage evenements debarquement")
    public void testDebarquementCounting() {
        TransportEvent event = createEvent("debarquement", "1", "BUS_001", 0.0, "");
        
        stats.processEvent(event);
        
        assertEquals(1, stats.getTotalEvents());
        assertEquals(0, stats.getTotalPassengers());
        assertEquals(0.0, stats.getTotalRevenue(), 0.01);
    }

    @Test
    @DisplayName("Agregation multiple evenements")
    public void testMultipleEvents() {
        stats.processEvent(createEvent("embarquement", "1", "BUS_001", 3.50, "carte"));
        stats.processEvent(createEvent("embarquement", "1", "BUS_002", 3.00, "cash"));
        stats.processEvent(createEvent("debarquement", "1", "BUS_001", 0.0, ""));
        stats.processEvent(createEvent("trajet", "2", "BUS_003", 0.0, ""));
        
        assertEquals(4, stats.getTotalEvents());
        assertEquals(2, stats.getTotalPassengers());
        assertEquals(6.50, stats.getTotalRevenue(), 0.01);
    }

    @Test
    @DisplayName("Calcul kilometres pour trajets")
    public void testKilometerCalculation() {
        stats.processEvent(createEvent("trajet", "1", "BUS_001", 0.0, ""));
        stats.processEvent(createEvent("trajet", "2", "BUS_002", 0.0, ""));
        
        // Chaque trajet = 3.5 km (constante dans Statistics)
        assertEquals(7.0, stats.getTotalKilometers(), 0.1);
    }

    @Test
    @DisplayName("Fusion de deux Statistics")
    public void testMergeStatistics() {
        Statistics stats1 = new Statistics(1);
        Statistics stats2 = new Statistics(2);
        
        stats1.processEvent(createEvent("embarquement", "1", "BUS_001", 3.50, "carte"));
        stats1.processEvent(createEvent("embarquement", "2", "BUS_002", 3.00, "cash"));
        
        stats2.processEvent(createEvent("embarquement", "3", "BUS_003", 2.50, "mobile"));
        stats2.processEvent(createEvent("trajet", "1", "BUS_001", 0.0, ""));
        
        Statistics merged = new Statistics(-1);
        merged.merge(stats1);
        merged.merge(stats2);
        
        assertEquals(4, merged.getTotalEvents());
        assertEquals(3, merged.getTotalPassengers());
        assertEquals(9.00, merged.getTotalRevenue(), 0.01);
    }

    @Test
    @DisplayName("Thread-safety test concurrent")
    public void testThreadSafety() throws InterruptedException {
        int numThreads = 10;
        int eventsPerThread = 100;
        Thread[] threads = new Thread[numThreads];
        
        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < eventsPerThread; j++) {
                    stats.processEvent(createEvent("embarquement", "1", "BUS_001", 3.50, "carte"));
                }
            });
            threads[i].start();
        }
        
        for (Thread t : threads) {
            t.join();
        }
        
        assertEquals(numThreads * eventsPerThread, stats.getTotalEvents());
        assertEquals(numThreads * eventsPerThread, stats.getTotalPassengers());
        assertEquals(numThreads * eventsPerThread * 3.50, stats.getTotalRevenue(), 0.01);
    }

    private TransportEvent createEvent(String type, String ligne, String busId, double montant, String methode) {
        TransportEvent event = new TransportEvent();
        event.setType(type);
        event.setLigne(ligne);
        event.setBusId(busId);
        event.setMontant(montant);
        event.setMethode(methode);
        event.setArret("Test-Arret");
        event.setPassagerId("PASS_TEST");
        event.setOrigine("Origine");
        event.setDestination("Destination");
        event.setTimestamp("2025-01-15T10:00:00.000-05:00");
        return event;
    }
}
