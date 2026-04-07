package ca.usherbrooke.ift630.tp3.test;

import ca.usherbrooke.ift630.tp3.compute.Statistics;
import ca.usherbrooke.ift630.tp3.mqtt.TransportEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests fusion statistiques")
public class MergeTest {

    @Test
    @DisplayName("Fusion deux workers simples")
    public void testSimpleMerge() {
        Statistics stats1 = new Statistics(1);
        Statistics stats2 = new Statistics(2);
        
        stats1.processEvent(createEvent("embarquement", "1", 3.50));
        stats2.processEvent(createEvent("embarquement", "2", 3.00));
        
        Statistics merged = new Statistics(-1);
        merged.merge(stats1);
        merged.merge(stats2);
        
        assertEquals(2, merged.getTotalEvents());
        assertEquals(2, merged.getTotalPassengers());
        assertEquals(6.50, merged.getTotalRevenue(), 0.01);
    }

    @Test
    @DisplayName("Fusion multiple workers")
    public void testMultipleWorkersMerge() {
        int numWorkers = 5;
        List<Statistics> workerStats = new ArrayList<>();
        
        for (int i = 0; i < numWorkers; i++) {
            Statistics stats = new Statistics(i);
            for (int j = 0; j < 10; j++) {
                stats.processEvent(createEvent("embarquement", "1", 3.50));
            }
            workerStats.add(stats);
        }
        
        Statistics merged = new Statistics(-1);
        for (Statistics stats : workerStats) {
            merged.merge(stats);
        }
        
        assertEquals(50, merged.getTotalEvents());
        assertEquals(50, merged.getTotalPassengers());
        assertEquals(175.0, merged.getTotalRevenue(), 0.01);
    }

    @Test
    @DisplayName("Fusion preserv donnees par ligne")
    public void testMergePreservesLineData() {
        Statistics stats1 = new Statistics(1);
        Statistics stats2 = new Statistics(2);
        
        stats1.processEvent(createEvent("embarquement", "1", 3.50));
        stats1.processEvent(createEvent("embarquement", "1", 3.50));
        
        stats2.processEvent(createEvent("embarquement", "2", 3.00));
        stats2.processEvent(createEvent("embarquement", "2", 3.00));
        
        Statistics merged = new Statistics(-1);
        merged.merge(stats1);
        merged.merge(stats2);
        
        assertEquals(4, merged.getTotalEvents());
        assertEquals(13.0, merged.getTotalRevenue(), 0.01);
    }

    @Test
    @DisplayName("Fusion avec worker vide")
    public void testMergeWithEmptyWorker() {
        Statistics stats1 = new Statistics(1);
        Statistics stats2 = new Statistics(2);
        
        stats1.processEvent(createEvent("embarquement", "1", 3.50));
        stats1.processEvent(createEvent("embarquement", "1", 3.50));
        
        // stats2 reste vide
        
        Statistics merged = new Statistics(-1);
        merged.merge(stats1);
        merged.merge(stats2);
        
        assertEquals(2, merged.getTotalEvents());
        assertEquals(7.0, merged.getTotalRevenue(), 0.01);
    }

    @Test
    @DisplayName("Fusion correcte kilometres")
    public void testMergeKilometers() {
        Statistics stats1 = new Statistics(1);
        Statistics stats2 = new Statistics(2);
        
        stats1.processEvent(createEvent("trajet", "1", 0.0));
        stats1.processEvent(createEvent("trajet", "1", 0.0));
        
        stats2.processEvent(createEvent("trajet", "2", 0.0));
        
        Statistics merged = new Statistics(-1);
        merged.merge(stats1);
        merged.merge(stats2);
        
        // 3 trajets * 3.5 km = 10.5 km
        assertEquals(10.5, merged.getTotalKilometers(), 0.1);
    }

    @Test
    @DisplayName("Fusion idempotente")
    public void testMergeIdempotent() {
        Statistics stats1 = new Statistics(1);
        stats1.processEvent(createEvent("embarquement", "1", 3.50));
        
        Statistics merged1 = new Statistics(-1);
        merged1.merge(stats1);
        
        Statistics merged2 = new Statistics(-1);
        merged2.merge(stats1);
        
        assertEquals(merged1.getTotalEvents(), merged2.getTotalEvents());
        assertEquals(merged1.getTotalRevenue(), merged2.getTotalRevenue(), 0.01);
    }

    private TransportEvent createEvent(String type, String ligne, double montant) {
        TransportEvent event = new TransportEvent();
        event.setType(type);
        event.setLigne(ligne);
        event.setBusId("BUS_001");
        event.setMontant(montant);
        event.setMethode("carte");
        event.setArret("Test");
        event.setPassagerId("PASS_TEST");
        event.setOrigine("A");
        event.setDestination("B");
        event.setTimestamp("2025-01-15T10:00:00.000-05:00");
        return event;
    }
}
